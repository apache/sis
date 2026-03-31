/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.storage.tiling;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.opengis.util.GenericName;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.MemoryGridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.NoSuchDataException;
import org.apache.sis.storage.UnsupportedQueryException;
import org.apache.sis.storage.InternalDataStoreException;
import org.apache.sis.storage.Resource;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverage2D;
import org.apache.sis.coverage.grid.GridCoverageProcessor;
import org.apache.sis.coverage.grid.IncompleteGridGeometryException;
import org.apache.sis.image.ComputedImage;
import org.apache.sis.image.internal.shared.ReshapedImage;
import org.apache.sis.pending.jdk.JDK18;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.storage.internal.Resources;


/**
 * Default implementation of {@code TileMatrix} as a wrapper for a {@code GridCoverage}.
 * The tile size must be specified at construction time and must be equal to the size of
 * the tiles of the rendered image.
 *
 * <p>This class is needed only when the application needs details about the tiling scheme,
 * for example in order to implement a Web Map Tile Service (<abbr>WMTS</abbr>).</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ImageTileMatrix implements TileMatrix {
    /**
     * Logger for the tiling package.
     */
    static final Logger LOGGER = Logger.getLogger("org.apache.sis.storage.tiling");

    /**
     * An alphanumeric identifier which is unique in the {@code TileMatrixSet} that contains this {@code TileMatrix}.
     * The identifier contains the zoom level as a number encoded in <abbr>ASCII</abbr>.
     */
    private final GenericName identifier;

    /**
     * The resource for reading the tiles of this tile matrix.
     */
    private final TiledGridCoverageResource resource;

    /**
     * The coverage from which to get the rendered image which contains the tiles.
     * This coverage uses deferred reading of tiles. Created when first requested.
     *
     * @see #coverage()
     */
    private TiledGridCoverage coverage;

    /**
     * Pattern for formatting tile indices using {@link java.util.Formatter}.
     * The number of digits and the presence of a sign depend on the {@linkplain #tilingScheme tiling scheme}.
     *
     * @see #getTileIdentifier(long[])
     */
    private final String tileIndicesPattern;

    /**
     * Extent of valid tile indices and their relationship with "real world" coordinates.
     * The (0, 0) tile indices should map to the tile in the upper-left corner. The last
     * row and last column of tiles may contain partial tiles if the coverage size is not
     * a divisor of the tile size.
     */
    private final GridGeometry tilingScheme;

    /**
     * Size of tiles, in number of {@linkplain #coverage} cells.
     * The length of this array may be shorter than the number of dimensions of the grid coverage,
     * because some {@link TiledGridCoverage} implementations may use some metadata (typically the
     * image date) as a third grid dimension, but still manage all tiles as two-dimensional.
     * In such case, all extra dimensions are assumed to have a size of 1.
     */
    private final int[] tileSize;

    /**
     * Values to add to tile coordinates, after multiplication by {@link #tileSize}, for getting cell coordinates.
     *
     * @see #tileToCell(long, int)
     */
    private final long[] tileToCell;

    /**
     * Values to add to tile coordinates in {@link #image} for getting a tile coordinates in {@link #tilingScheme}.
     * Those values are updated when a new image is rendered.
     */
    private long imageToTileX, imageToTileY;

    /**
     * The image containing the tiles of the tile matrix. Computed when first needed, and may be
     * recomputed multiple times with different offsets if the tile indices are larger than the
     * capacity of 32-bits integers.
     */
    private RenderedImage image;

    /**
     * The grid coverage processor to use when tiles use a subset of the bands.
     *
     * @see #createResourceView(long[], RenderedImage)
     */
    private final GridCoverageProcessor processor;

    /**
     * Creates a new tile matrix for the given coverage.
     *
     * @param  identifier  identifier unique in the {@code TileMatrixSet} that contains this {@code TileMatrix}.
     * @param  resource    the resource for reading the tiles of this tile matrix.
     * @param  processor   the grid coverage processor to use when tiles use a subset of the bands.
     * @throws TransformException if the "tile indices to CRS" transform cannot be computed.
     */
    ImageTileMatrix(final GenericName identifier,
                    final TiledGridCoverageResource resource,
                    final GridCoverageProcessor processor)
            throws DataStoreException, TransformException
    {
        this.identifier = identifier;
        this.processor  = processor;
        this.resource   = resource;
        this.tileSize   = resource.getTileSize();
        final GridGeometry cellGrid = resource.getGridGeometry();
        final GridExtent extent     = cellGrid.getExtent();
        final int        dimension  = extent.getDimension();
        final long[]     tileCount  = new long[dimension];
        final MatrixSIS  toCells    = Matrices.createIdentity(dimension + 1);
        this.tileToCell = new long[dimension];
        final var pattern = new StringBuilder(6 * dimension);
        for (int i=0; i<dimension; i++) {
            long size = extent.getSize(i);
            final long offset = extent.getLow(i);
            if (i < tileSize.length) {
                final int scale = tileSize[i];
                toCells.setNumber(i, i, scale);
                size = JDK18.ceilDiv(size, scale);
            }
            toCells.setNumber(i, dimension, offset);
            tileToCell[i] = offset;
            tileCount [i] = size;
            /*
             * Prepare a pattern for formatting the tile indices.
             * Indices are formatted with fixed number of digits,
             * using the minimum number needed for the largest index.
             */
            if (i != 0) pattern.append(',');
            pattern.append("%0").append(DecimalFunctions.floorLog10(Math.max(tileCount[i] - 1, 1)) + 1).append('d');
        }
        tilingScheme = new GridGeometry(cellGrid, extent.reshape(null, tileCount, false), MathTransforms.linear(toCells));
        tileIndicesPattern = pattern.toString();
    }

    /**
     * Returns an identifier which is unique in the {@code TileMatrixSet} that contains this {@code TileMatrix}.
     * The identifier contains the zoom level as a number encoded in <abbr>ASCII</abbr>.
     */
    @Override
    public GenericName getIdentifier() {
        return identifier;
    }

    /**
     * Returns the resolution (in units of CRS axes) at which tiles in this matrix should be used.
     * The array length is the number of <abbr>CRS</abbr> dimensions, and value at index <var>i</var>
     * is the resolution along CRS dimension <var>i</var> in units of the CRS axis <var>i</var>.
     *
     * @throws IncompleteGridGeometryException if the tiling scheme has no resolution.
     *         Tile matrices with such tiling scheme should not have been constructed.
     */
    @Override
    public double[] getResolution() {
        try {
            return resource.getGridGeometry().getResolution(false);
        } catch (DataStoreException e) {
            throw new BackingStoreException(e);
        }
    }

    /**
     * Returns the tile size of the given matrix if known, or {@code null} otherwise.
     * This method returns a direct reference to the internal array, caller shall not modify.
     */
    static int[] getTileSize(final TileMatrix matrix) {
        return (matrix instanceof ImageTileMatrix) ? ((ImageTileMatrix) matrix).tileSize : null;
    }

    /**
     * Returns a description about how space is partitioned into individual tiled units.
     * The description contains the extent of valid tile indices, the spatial reference system,
     * and the conversion from tile indices to the spatial reference system coordinates.
     *
     * @return extent of valid tile indices and their relationship with "real world" coordinates.
     */
    @Override
    public GridGeometry getTilingScheme() {
        return tilingScheme;
    }

    /**
     * Returns the coverage, which is read when first needed.
     * This coverage uses deferred reading of tiles.
     *
     * @return the coverage from which to get the rendered image which contains the tiles.
     * @throws DataStoreException if an error occurred during the construction of the coverage.
     */
    private synchronized TiledGridCoverage coverage() throws DataStoreException {
        if (coverage == null) {
            coverage = resource.readAtGetTileTime();
        }
        return coverage;
    }

    /**
     * Fetches information about whether a tile exists, is missing or failed to load.
     *
     * @param  indices  indices of the requested tile (may be outside the tile matrix extent).
     * @return information about the availability of the specified tile.
     * @throws DataStoreException if fetching the tile status failed.
     */
    @Override
    @SuppressWarnings("LocalVariableHidesMemberVariable")
    public TileStatus getTileStatus(final long... indices) throws DataStoreException {
        if (tilingScheme.getExtent().contains(indices)) try {
            final TiledGridCoverage coverage;
            final RenderedImage image;
            final long imageToTileX;
            final long imageToTileY;
            synchronized (this) {
                coverage     = this.coverage;   // Never null if `image` is non-null.
                image        = this.image;
                imageToTileX = this.imageToTileX;
                imageToTileY = this.imageToTileY;
            }
            if (image != null) {
                final long tileX = Math.subtractExact(indices[coverage.xDimension], imageToTileX);
                final long x0 = image.getMinTileX();
                if (tileX >= x0 && tileX < x0 + image.getNumXTiles()) {
                    final long tileY = Math.subtractExact(indices[coverage.yDimension], imageToTileY);
                    final long y0 = image.getMinTileY();
                    if (tileY >= y0 && tileY < y0 + image.getNumYTiles()) {
                        return getTileStatus(image, Math.toIntExact(tileX), Math.toIntExact(tileY));
                    }
                }
            }
            return TileStatus.UNKNOWN;
        } catch (ArithmeticException e) {
            Logging.ignorableException(LOGGER, ImageTileMatrix.class, "getTileStatus", e);
        }
        return TileStatus.OUTSIDE_EXTENT;
    }

    /**
     * Returns the status of the tile at the given index.
     * If the image is an instance of {@link ComputedImage},
     * then this method checks whether the tile is in error.
     *
     * This method does not check whether the tile indexes are outside the image domain
     * ({@link TileStatus#OUTSIDE_EXTENT}). This verification must be done by the caller.
     *
     * @param  image  image from which to get a tile status.
     * @param  tileX  row index of the tile for which to get the status.
     * @param  tileY  column index of the tile for which to get the status.
     * @return status of the tile at the specified indexes.
     */
    private static TileStatus getTileStatus(final RenderedImage image, final int tileX, final int tileY) {
        if (image instanceof ComputedImage) {
            final var computed = (ComputedImage) image;
            final var tiles = new Rectangle(tileX, tileY, 1, 1);
            if (computed.hasErrorFlag(tiles)) {
                return TileStatus.IN_ERROR;
            }
        }
        return TileStatus.EXISTS;
    }

    /**
     * Gets a tile at the given indices if not missing.
     *
     * @param  indices  indices of the tile to fetch, as coordinates inside the matrix extent.
     * @return the tile if it exists, or an empty value if the tile is missing.
     * @throws NoSuchDataException if the given indices are outside the matrix extent.
     * @throws DataStoreException if fetching the tile failed for another reason.
     */
    @Override
    public Optional<Tile> getTile(final long... indices) throws DataStoreException {
        final GridExtent extent = tilingScheme.getExtent();
        if (extent.contains(indices)) try {
            final Tile tile = iterator(extent.reshape(indices, indices, true)).createFirstTile();
            return (tile.getStatus() == TileStatus.MISSING) ? Optional.empty() : Optional.of(tile);
        } catch (ArithmeticException e) {
            throw new UnsupportedQueryException(e);
        }
        throw new NoSuchDataException(Resources.format(Resources.Keys.TileIndexesOutOfBounds));
    }

    /**
     * Retrieves a stream of existing tiles in the specified region.
     * The stream contains the existing tiles that are inside the given region and excludes missing tiles.
     * If a tile is {@linkplain TileStatus#IN_ERROR in error}, then the stream nevertheless return a tile
     * but its {@link Tile#getResource()} method should throw the exception.
     *
     * <h4>Limitations</h4>
     * The current implementation limits the size of the given extent to {@link Integer#MAX_VALUE}
     * in each dimension. Note that this is a maximum size in tile indices, not in pixel coordinates.
     *
     * @param  indiceRanges  ranges of tile indices in all dimensions, or {@code null} for all tiles.
     * @param  parallel  {@code true} for a parallel stream (if supported), or {@code false} for a sequential stream.
     * @return stream of tiles, excluding missing tiles.
     * @throws DataStoreException if the tiles can not be fetched in the given ranges of tile indexes.
     */
    @Override
    public Stream<Tile> getTiles(GridExtent indiceRanges, final boolean parallel) throws DataStoreException {
        if (indiceRanges == null) {
            indiceRanges = tilingScheme.getExtent();
        }
        try {
            return StreamSupport.stream(iterator(indiceRanges).iterator(), parallel);
        } catch (ArithmeticException e) {
            throw new UnsupportedQueryException(e);
        }
    }

    /**
     * Creates an object which can be used for retrieving a single tile or a stream tiles.
     *
     * @param  indiceRanges  ranges of tile indices in all dimensions, or {@code null} for all tiles.
     * @return a request which can be used for getting a tile or a stream of tiles in the given region.
     * @throws DataStoreException if the tiles can not be fetched in the given ranges of tile indexes.
     * @throws ArithmeticException if coordinate computation exceeds the capacity of 64-bits integers.
     */
    private synchronized IterationDomain<Tile> iterator(final GridExtent indiceRanges) throws DataStoreException {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final TiledGridCoverage coverage = coverage();
        boolean retry = false;
        do {    // This loop will be executed only 1 or 2 times.
            if (image != null) {
                final long xmin, ymin, xmax, ymax;
                xmin = Math.subtractExact(indiceRanges.getLow (coverage.xDimension), imageToTileX);
                xmax = Math.subtractExact(indiceRanges.getHigh(coverage.xDimension), imageToTileX);
                final long x0 = image.getMinTileX();
                if (xmin >= x0 && xmax < x0 + image.getNumXTiles()) {
                    ymin = Math.subtractExact(indiceRanges.getLow (coverage.yDimension), imageToTileY);
                    ymax = Math.subtractExact(indiceRanges.getHigh(coverage.yDimension), imageToTileY);
                    final long y0 = image.getMinTileY();
                    if (ymin >= y0 && ymax < y0 + image.getNumYTiles()) {
                        return new Iterator(Math.toIntExact(xmin),
                                            Math.toIntExact(ymin),
                                            Math.toIntExact(xmax),
                                            Math.toIntExact(ymax));
                    }
                }
            }
            /*
             * Gets the bounds of the image to read. If deferred reading is supported,
             * we can expand to the bounds of the whole coverage in order to perform a
             * read operation (deferred) only once.
             */
            final GridExtent extent = coverage.getGridGeometry().getExtent();
            final int dimension = extent.getDimension();
            final var low  = new long[dimension];
            final var high = new long[dimension];
            for (int i=0; i<dimension; i++) {
                final long limit = Math.incrementExact(extent.getHigh(i));
                high[i] = Math.min(limit, tileToCell(Math.incrementExact(indiceRanges.getHigh(i)), i));
                low [i] = Math.max(extent.getLow(i), tileToCell(indiceRanges.getLow(i), i));
                final long span = high[i] - low[i];
                if (span < 0 || span > Integer.MAX_VALUE) {
                    throw new ArithmeticException(resource.errors().getString(Errors.Keys.IntegerOverflow_1, Integer.SIZE));
                }
                if (coverage.deferredTileReading) {
                    final long remain = Math.min(extent.getSize(i), Integer.MAX_VALUE) - span;
                    final long after  = Math.min(remain >> 1, limit - high[i]);
                    final long before = Math.min(remain - after, low[i] - extent.getLow(i));
                    low [i] -= before;
                    high[i] += after;
                }
            }
            image = coverage.render(extent.reshape(low, high, false));
            imageToTileX = low[coverage.xDimension];
            imageToTileY = low[coverage.yDimension];
        } while ((retry = !retry) == true);
        throw new InternalDataStoreException();     // Should never happen.
    }

    /**
     * Converts the give tile coordinate in the given dimension to cell coordinates.
     *
     * @param  coordinate  the tile coordinate to convert.
     * @param  dimension   the dimension of the coordinate to convert.
     * @return the cell coordinate.
     * @throws ArithmeticException if the result overflows the capacity of 64-bits integers.
     */
    private long tileToCell(long coordinate, final int dimension) {
        if (dimension < tileSize.length) {
            coordinate = Math.multiplyExact(coordinate, tileSize[dimension]);
        }
        return Math.addExact(tileToCell[dimension], coordinate);
    }

    /**
     * Factory for an iterator over tiles in ranges of user-specified tile indices.
     */
    private final class Iterator extends IterationDomain<Tile> {
        /**
         * Snapshot of {@link ImageTileMatrix#image}.
         */
        private final RenderedImage tiles;

        /**
         * Snapshot of {@link ImageTileMatrix#imageToTileX} and {@link ImageTileMatrix#imageToTileY}.
         */
        private final long offsetX, offsetY;

        /**
         * Creates a new request for tile iterators.
         *
         * @param xmin  first column index of tiles, inclusive.
         * @param xmin  first row index of tiles, inclusive.
         * @param xmax  last column index of tiles, inclusive.
         * @param ymax  last row index of tiles, inclusive.
         */
        Iterator(final int xmin, final int ymin, final int xmax, final int ymax) {
            super(xmin, ymin, xmax, ymax);
            tiles   = image;
            offsetX = imageToTileX;
            offsetY = imageToTileY;
        }

        /**
         * Creates the tile at the given indexes.
         * The caller must ensure that the arguments are valid image tile indexes.
         * This condition is not verified by this method.
         */
        @Override
        protected Tile createTile(final int tileX, final int tileY) {
            return new Tile() {
                /** This tile viewed as a resource, created when first requested. */
                private Resource resourceView;

                /** Returns the path to content of the tile if known. */
                @Override public Optional<Path> getContentPath() throws DataStoreException {
                    return Optional.ofNullable(coverage().getContentPath(getIndices()));
                }

                /** Returns the indices of this tile in the {@code TileMatrix}. */
                @Override public long[] getIndices() {
                    return new long[] {offsetX + tileX, offsetY + tileY};
                }

                /** Returns information about whether the tile failed to load. */
                @Override public TileStatus getStatus() {
                    return getTileStatus(tiles, tileX, tileY);
                }

                /** Returns the tile content as a resource. */
                @Override public synchronized Resource getResource() throws DataStoreException {
                    if (resourceView == null) {
                        resourceView = createResourceView(getIndices(), ReshapedImage.singleTile(tiles, tileX, tileY));
                    }
                    return resourceView;
                }
            };
        }
    }

    /**
     * Creates a resource for the tile at the given indices.
     * The resource wraps a grid coverage, which is itself wrapping the given image.
     * The given image should contains only the desired tile. The caller currently sets
     * the tile indexes and image coordinates to (0,0), but this is not mandatory.
     *
     * @param  indices  indices of the tile, as coordinates inside the matrix extent.
     * @param  tile     a rendered image which contains only the tile.
     * @return resource for the specified tile.
     */
    private Resource createResourceView(final long[] indices, final RenderedImage tile) throws DataStoreException {
        final Object[] args = new Object[indices.length];
        Arrays.setAll(args, (i) -> indices[i]);
        final GenericName id = Names.createScopedName(identifier, null, String.format(tileIndicesPattern, args));
        final long[] low  = new long[indices.length];
        final long[] high = new long[indices.length];
        for (int i=0; i<indices.length; i++) {
            final long size = (i < tileSize.length) ? tileSize[i] : 1;
            low [i] = Math.addExact(tileToCell[i], Math.multiplyExact(indices[i], size));
            high[i] = Math.addExact(low[i], size - 1);
        }
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final TiledGridCoverage coverage = coverage();
        GridGeometry cellGrid = coverage.getGridGeometry();
        final GridExtent extent = cellGrid.getExtent().reshape(low, high, true);
        cellGrid = cellGrid.derive().subgrid(extent, null).build();
        final var subset = new GridCoverage2D(cellGrid, coverage.getSampleDimensions(), tile);
        return new MemoryGridCoverageResource(resource, id, subset, processor);
    }
}
