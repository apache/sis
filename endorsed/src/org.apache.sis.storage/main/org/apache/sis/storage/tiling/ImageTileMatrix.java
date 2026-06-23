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
import static java.lang.Math.addExact;
import static java.lang.Math.subtractExact;
import static java.lang.Math.multiplyExact;
import static java.lang.Math.incrementExact;
import static java.lang.Math.decrementExact;
import static java.lang.Math.floorDiv;
import static java.lang.Math.toIntExact;
import static java.lang.Math.min;
import static java.lang.Math.max;
import org.opengis.util.GenericName;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.MemoryGridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.NoSuchDataException;
import org.apache.sis.storage.UnsupportedQueryException;
import org.apache.sis.storage.Resource;
import org.apache.sis.coverage.SubspaceNotSpecifiedException;
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
 * If the coverage has more than two dimensions, the current implementation requires
 * a tile size of 1 in all dimensions other than <var>X</var> and <var>Y</var>.
 *
 * <p>This class is needed only when the application needs details about the tiling scheme,
 * for example in order to implement a Web Map Tile Service (<abbr>WMTS</abbr>).</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
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
     *
     * @see TiledGridCoverageResource#getTileSize()
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
     *
     * <p>If the coverage has more than two dimensions, this image contains the tiles of only one slice.
     * The current implementation does not cache more than one two-dimensional slice at a time, in order
     * to avoid too large memory consumption.</p>
     */
    private RenderedImage image;

    /**
     * Indices in extra-dimensions (above two) of the tiles cached by {@link #image}.
     * The length of this array shall be the number of dimensions of the {@linkplain #coverage}.
     * The values at the <var>X</var> and <var>Y</var> dimensions will be ignored and <em>shall</em> be zero.
     * The values in all other dimensions shall be the index of the slice rendered by {@linkplain #image}.
     * The zeros in <var>X</var> and <var>Y</var> dimensions are important for making array comparisons simpler.
     *
     * <p>The content of this array shall not be modified.
     * If new indices are needed, a new array shall be created.</p>
     */
    private long[] imageSliceIndices;

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
            pattern.append("%0").append(DecimalFunctions.floorLog10(max(tileCount[i] - 1, 1)) + 1).append('d');
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
     * Returns the extent of the resource.
     *
     * @return the grid extent.
     * @throws DataStoreException if an error occurred while fecthing the grid geometry.
     */
    final GridExtent getResourceExtent() throws DataStoreException {
        return resource.getGridGeometry().getExtent();
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
                final long tileX = subtractExact(indices[coverage.xDimension], imageToTileX);
                final long x0 = image.getMinTileX();
                if (tileX >= x0 && tileX < x0 + image.getNumXTiles()) {
                    final long tileY = subtractExact(indices[coverage.yDimension], imageToTileY);
                    final long y0 = image.getMinTileY();
                    if (tileY >= y0 && tileY < y0 + image.getNumYTiles()) {
                        return getTileStatus(image, toIntExact(tileX), toIntExact(tileY));
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
            final int dimension = indices.length;
            final long[] cellLow  = new long[dimension];
            final long[] cellHigh = new long[dimension];
            for (int i=0; i < dimension; i++) {
                cellLow [i] = tileToCell(indices[i], i);
                cellHigh[i] = addExact(cellLow[i], tileSize[i] - 1);    // Inclusive.
            }
            final Tile tile = builder(indices, indices, cellLow, cellHigh).createFirstTile();
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
     * If the requested tile ranges exceed the capacity of the integer numbers used by this implementation,
     * an {@link ArithmeticException} will be thrown either at the execution of this method,
     * or during the execution of the returned stream.
     *
     * <p>Another limitation is that the tile size in all dimensions other than <var>X</var> and <var>Y</var>
     * must be one, otherwise an {@link SubspaceNotSpecifiedException} will be thrown.</p>
     *
     * @param  tileRanges  ranges of tile indices in all dimensions, or {@code null} for all tiles.
     * @param  parallel  {@code true} for a parallel stream (if supported), or {@code false} for a sequential stream.
     * @return stream of tiles, excluding missing tiles.
     * @throws DataStoreException if the tiles cannot be fetched in the given ranges of tile indexes.
     * @throws SubspaceNotSpecifiedException if tile size in dimensions other than X and Y is greater than 1.
     * @throws ArithmeticException if some coordinates are too large.
     *         This exception may also happen latter, during the execution of the returned stream.
     */
    @Override
    public Stream<Tile> getTiles(GridExtent tileRanges, final boolean parallel) throws DataStoreException {
        /*
         * Argument check: "indiceRanges" is the name used in public API, but we use `tileRanges` in this
         * implementation for avoiding confusion with `cellRanges`. No need to expose this change to user.
         */
        ArgumentChecks.ensureDimensionMatches("indiceRanges", tilingScheme.getDimension(), tileRanges);
        if (tileRanges == null) {
            tileRanges = tilingScheme.getExtent();
        }
        /*
         * Gets the bounds of the images to read. If deferred reading is supported,
         * we can expand to the bounds of the whole coverage in order to perform a
         * read operation (deferred) only once. The bounds are intersected with the
         * coverage extent.
         */
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final var coverage = coverage();
        final int xDimension = coverage.xDimension;
        final int yDimension = coverage.yDimension;
        final GridExtent cellRanges = coverage.getGridGeometry().getExtent();
        final int dimension = cellRanges.getDimension();
        final var cellLow  = new long[dimension];
        final var tileLow  = new long[dimension];
        final var cellHigh = new long[dimension];
        final var tileHigh = new long[dimension];
        boolean is2D = true;
        for (int i=0; i<dimension; i++) {
            // Intersection between request and available data, in cell coordinates.
            cellLow [i] = max(cellRanges.getLow (i), tileToCell(tileRanges.getLow(i), i));
            cellHigh[i] = min(cellRanges.getHigh(i), decrementExact(tileToCell(incrementExact(tileRanges.getHigh(i)), i)));
            final long span = cellHigh[i] - cellLow[i] + 1;
            if (span < 0 || span > Integer.MAX_VALUE) {
                throw new ArithmeticException(resource.errors().getString(Errors.Keys.IntegerOverflow_1, Integer.SIZE));
            }
            // Convert back to tile indices after intersection.
            tileLow [i] = cellToTile(cellLow [i], i);
            tileHigh[i] = cellToTile(cellHigh[i], i);
            /*
             * Special cases for (X,Y) dimensions: if deferred loading is enabled, expand the request
             * to the maximum size that we can handle with 32-bits integers. This is okay because the
             * actual loading will not happen at `read(…)` invocation time. Note that we do not expand
             * `tileLow` and `tileHigh` because they should stay bounded to user's request.
             */
            if (i != xDimension && i != yDimension) {
                is2D &= (cellLow[i] == cellHigh[i]);
            } else if (coverage.deferredTileReading) {
                final long remain = min(cellRanges.getSize(i), Integer.MAX_VALUE) - span;
                final long after  = min(remain / 2, cellRanges.getHigh(i) - cellHigh[i]);
                final long before = min(remain - after, cellLow[i] - cellRanges.getLow(i));
                cellLow [i] -= before;
                cellHigh[i] += after;
            }
        }
        if (is2D) {
            // This code should only be reached if the requested extent is 2D.
            assert tileRanges.getDegreesOfFreedom() <= TiledGridCoverageResource.BIDIMENSIONAL;
            return StreamSupport.stream(builder(tileLow, tileHigh, cellLow, cellHigh).iterator(), parallel);
        }
        /*
         * Splits the given extent in a lazy sequence of 2D extents.
         * For each 2D slice, we query all tiles contained in the user requested tile ranges.
         *
         * Note: the iteration over slices is executed in sequential order even if `parallel` is true.
         * Allowing parallel iteration over slices could hurt performance, because it would cause the
         * loading of rendered images from different slices in parallel. As this class `image` caching
         * strategy is based on 2D extents, we instead push parallelism down on tile loading level directly
         * (see `flatMap` block).
         */
        final long[] tileHighInExtraDimensions = tileHigh.clone();
        tileHighInExtraDimensions[xDimension] = tileLow[xDimension];
        tileHighInExtraDimensions[yDimension] = tileLow[yDimension];
        return tileRanges.reshape(tileLow, tileHighInExtraDimensions, true).latticePointStream(false)
                .flatMap((final long[] sliceLow) -> {
                    final long[] sliceHigh = sliceLow.clone();
                    sliceHigh[xDimension] = tileHigh[xDimension];
                    sliceHigh[yDimension] = tileHigh[yDimension];
                    for (int i=0; i<sliceLow.length; i++) {
                        if (i != xDimension && i != yDimension) {
                            cellLow [i] = tileToCell(sliceLow[i], i);
                            cellHigh[i] = addExact(cellLow[i], tileSize[i] - 1);    // Inclusive.
                        }
                    }
                    final IteratorBuilder builder;
                    try {
                        builder = builder(sliceLow, sliceHigh, cellLow, cellHigh);
                    } catch (DataStoreException e) {
                        throw new BackingStoreException("Cannot load tiles for 2D extent", e);
                    }
                    return StreamSupport.stream(builder.iterator(), parallel);
                });
    }

    /**
     * Creates an object which can be used for retrieving a single tile or a stream of tiles.
     * All arguments in this method are in the coordinate system of the {@linkplain #coverage}.
     * Note that a translation may exist between the coverage and the {@linkplain #image}.
     *
     * <p>The cell indices may describe a region larger to the tile indices if deferred loading
     * of tiles is enabled. In such case, we will prepare more tiles than what user requested,
     * but will iterate over only the requested tiles.</p>
     *
     * @param  tileLow   tile index (inclusive) of the first valid tile requested by the user.
     * @param  tileHigh  tile index (inclusive) of the last valid tile requested by the user.
     * @param  cellLow   cell index (inclusive) of the first cell to load if necessary.
     * @param  cellHigh  cell index (inclusive) of the last cell to load if necessary.
     * @return a request which can be used for getting a tile or a stream of tiles in the given region.
     * @throws DataStoreException if the tiles cannot be fetched in the given ranges of tile indexes.
     * @throws ArithmeticException if coordinate computation exceeds the capacity of 64-bits integers.
     * @throws SubspaceNotSpecifiedException if tile size in dimensions other than X and Y is greater than 1.
     */
    private synchronized IteratorBuilder builder(final long[] tileLow, final long[] tileHigh,
                                                 final long[] cellLow, final long[] cellHigh)
            throws DataStoreException
    {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final TiledGridCoverage coverage = coverage();
        final int xDimension = coverage.xDimension;
        final int yDimension = coverage.yDimension;
        final long[] slice = cellLow.clone();
        slice[xDimension] = 0;
        slice[yDimension] = 0;
        if (image != null) {
            final long tx0, ty0;
            if (!Arrays.equals(imageSliceIndices, slice)
                    || tileLow [xDimension] < (tx0 = image.getMinTileX() + imageToTileX) || tileHigh[xDimension] >= tx0 + image.getNumXTiles()
                    || tileLow [yDimension] < (ty0 = image.getMinTileY() + imageToTileY) || tileHigh[yDimension] >= ty0 + image.getNumYTiles())
            {
                image = null;
            }
        }
        if (image == null) {
            image = coverage.render(coverage.getGridGeometry().getExtent().reshape(cellLow, cellHigh, true));
            imageToTileX = cellToImageTile(xDimension, cellLow, image.getTileGridXOffset(), image.getTileWidth());
            imageToTileY = cellToImageTile(yDimension, cellLow, image.getTileGridYOffset(), image.getTileHeight());
            imageSliceIndices = slice;
        }
        return new IteratorBuilder(toIntExact(subtractExact(tileLow [xDimension], imageToTileX)),
                                   toIntExact(subtractExact(tileLow [yDimension], imageToTileY)),
                                   toIntExact(subtractExact(tileHigh[xDimension], imageToTileX)),
                                   toIntExact(subtractExact(tileHigh[yDimension], imageToTileY)));
    }

    /**
     * Converts the give tile coordinate in the given dimension to cell coordinates.
     * The cell and tile coordinates are in the {@linkplain #coverage} space, which
     * may be translated compared to the coordinates of the rendered image.
     *
     * @param  coordinate  the tile coordinate to convert.
     * @param  dimension   the dimension of the coordinate to convert.
     * @return the cell coordinate in the grid coverage.
     * @throws ArithmeticException if the result overflows the capacity of 64-bits integers.
     */
    private long tileToCell(long coordinate, final int dimension) {
        if (dimension < tileSize.length) {
            coordinate = multiplyExact(coordinate, tileSize[dimension]);
        }
        return addExact(tileToCell[dimension], coordinate);
    }

    /**
     * Converts the give cell coordinate in the given dimension to tile coordinates.
     * The cell and tile coordinates are in the {@linkplain #coverage} space, which
     * may be translated compared to the coordinates of the rendered image.
     *
     * @param  coordinate  the cell coordinate to convert.
     * @param  dimension   the dimension of the coordinate to convert.
     * @return the tile coordinate in the grid coverage.
     * @throws ArithmeticException if the result overflows the capacity of 64-bits integers.
     */
    private long cellToTile(long coordinate, final int dimension) {
        coordinate = subtractExact(coordinate, tileToCell[dimension]);
        if (dimension < tileSize.length) {
            coordinate = floorDiv(coordinate, tileSize[dimension]);
        }
        return coordinate;
    }

    /**
     * Converts a coverage cell coordinates to the index of a tile in the rendered image.
     * This is used for computing {@link #imageToTileX} and {@link #imageToTileY}.
     *
     * @param  dimension  the dimension of the coordinate to convert.
     * @param  origin     lower values of the grid coordinates of the extent which has been requested for rendering.
     * @param  offset     {@link RenderedImage#getTileGridXOffset()} or {@link RenderedImage#getTileGridYOffset()}.
     * @param  size       {@link RenderedImage#getTileWidth()} or {@link RenderedImage#getTileHeight()}.
     * @return difference between index of the tile in the coverage and index of the tile in the rendered image.
     * @throws ArithmeticException if the result overflows the capacity of 64-bits integers.
     */
    private long cellToImageTile(final int dimension, final long[] origin, final int offset, final int size) {
        /*
         * `cellToTile` is the tile coordinate in the grid coverage, assuming that tile indices start at (0,0).
         * The corresponding tile coordinate in the image can be anything. Since `origin` gave the coordinates
         * of pixel (0,0) in the rendered image, `cellToTile` corresponds to the rendered image tile which
         * contains pixel (0,0). This is the tile at index `-tileGridoffset / tileSize` (note the minus sign).
         */
        return addExact(cellToTile(origin[dimension], dimension), floorDiv(offset, size));
    }

    /**
     * Factory for an iterator over tiles in ranges of user-specified tile indices.
     */
    private final class IteratorBuilder extends IterationDomain<Tile> {
        /**
         * Snapshot of {@link ImageTileMatrix#image}.
         */
        private final RenderedImage tiles;

        /**
         * Snapshot of {@link ImageTileMatrix#imageToTileX} and {@link ImageTileMatrix#imageToTileY}.
         */
        private final long offsetX, offsetY;

        /**
         * Dimension indices for the <var>X</var> and <var>Y</var> axes in the tile matrix coordinate system.
         */
        private final int xDimension, yDimension;

        /**
         * Tile coordinates of the slice on which to iterate, except for values in X and Y dimensions.
         * Used when returning {@linkplain Tile#getIndices() tile indices}.
         * Tiles will use these coordinates, replacing {@link #xDimension X} and {@linkplain #yDimension Y} dimensions.
         */
        private final long[] sliceIndices;

        /**
         * Creates a new request for tile iterators.
         *
         * <h4>Iteration in a slice of a <var>n</var>-dimensional cube.</h4>
         * The {@link #imageSliceIndices} field gives the tile coordinates of the slice on which to iterate.
         * It can be any valid coordinates of the tiles managed by this iterator.
         * It serves as base to build correct tile coordinates for each returned tile.
         * Each tile will clone this array and replace its X and Y indices by its own.
         * Therefore, it is important that it represents properly the coordinates of extra-dimensions
         * this iterator operates on.
         *
         * @param minTileX   first column index of tiles, inclusive.
         * @param minTileY   first row index of tiles, inclusive.
         * @param maxTileX   last column index of tiles, inclusive.
         * @param maxTileY   last row index of tiles, inclusive.
         */
        IteratorBuilder(final int minTileX, final int minTileY, final int maxTileX, final int maxTileY) {
            super(minTileX, minTileY, maxTileX, maxTileY);
            tiles        = image;
            offsetX      = imageToTileX;
            offsetY      = imageToTileY;
            xDimension   = coverage.xDimension;
            yDimension   = coverage.yDimension;
            sliceIndices = imageSliceIndices;
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
                    final long[] indices = sliceIndices.clone();
                    indices[xDimension] = offsetX + tileX;
                    indices[yDimension] = offsetY + tileY;
                    return indices;
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
            low [i] = addExact(tileToCell[i], multiplyExact(indices[i], size));
            high[i] = addExact(low[i], size - 1);
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
