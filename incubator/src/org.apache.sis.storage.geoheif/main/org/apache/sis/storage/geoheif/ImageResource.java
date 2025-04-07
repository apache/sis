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
package org.apache.sis.storage.geoheif;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.IdentityHashMap;
import java.io.IOException;
import static java.lang.Math.addExact;
import static java.lang.Math.multiplyExact;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RasterFormatException;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import org.opengis.metadata.Metadata;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.base.StoreResource;
import org.apache.sis.storage.base.TiledGridCoverage;
import org.apache.sis.storage.base.TiledGridResource;


/**
 * A single image as a grid coverage resource.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
final class ImageResource extends TiledGridResource implements StoreResource {
    /**
     * The data store that produced this resource.
     *
     * @see #getOriginator()
     */
    private final GeoHeifStore store;

    /**
     * Identifier of this resource.
     *
     * @see #getIdentifier()
     */
    private final GenericName identifier;

    /**
     * The metadata, initially in a mutable state.
     * Modifications may still happen before the metadata is returned to the user.
     * These modifications may happen indirectly, for example through the {@link CoverageBuilder}
     * that created this metadata if that builder is still used for creating more resources.
     *
     * @see #createMetadata()
     */
    private final DefaultMetadata metadata;

    /**
     * The grid geometry of this grid coverage resource.
     *
     * @see #getGridGeometry()
     */
    private final GridGeometry gridGeometry;

    /**
     * Description of the bands.
     *
     * @see #getSampleDimensions()
     */
    private final List<SampleDimension> sampleDimensions;

    /**
     * Number of columns in the tile matrix. This is the offset to add to the index of a tile
     * in the {@link #tiles} array for moving to the tile in the same column of the next row.
     */
    private final int tileMatrixRowStride;

    /**
     * The sample model of each tile. The model size is the tile size.
     *
     * @see #getSampleModel(int[])
     */
    private final SampleModel sampleModel;

    /**
     * The color model of the image.
     *
     * @see #getColorModel(int[])
     */
    private final ColorModel colorModel;

    /**
     * The images that constitute the tiles of this grid. Shall contain at least 1 element.
     * All tiles shall have the same size.
     */
    private final Image[] tiles;

    /**
     * Creates a new grid coverage resource for an image.
     * Exactly one of {@code tiles} and {@code reader} argument shall be non-null.
     *
     * @param  builder  helper class for building the grid geometry and sample dimensions.
     * @param  tiles    the images that constitute the tiles, or {@code null} if {@code reader} is provided.
     * @param  image    the single tile for the wole image, or {@code null} if {@code tiles} is provided.
     * @throws RasterFormatException if the sample dimensions or sample model cannot be created.
     * @throws DataStoreException if the "grid to <abbr>CRS</abbr>" transform cannot be created.
     */
    ImageResource(final CoverageBuilder builder, Image[] tiles, final Image.Supplier image) throws DataStoreException {
        super(builder.store);
        this.store       = builder.store;
        identifier       = builder.name();
        sampleDimensions = builder.sampleDimensions();
        gridGeometry     = builder.gridGeometry();      // Should be after `sampleDimensions()`.
        if (tiles == null) {
            // Shall be after the call to `sampleDimensions()`.
            tiles = new Image[] {
                image.get()         // Actual I/O operations are deferred (not in this call).
            };
        }
        this.tiles  = tiles;
        sampleModel = builder.sampleModel();
        colorModel  = builder.colorModel();
        metadata    = builder.metadata().build();     // Not `buildAndFreeze()` as the metadata may still be modified.
        tileMatrixRowStride = builder.numTiles(0);
    }

    /**
     * Returns the data store that produced this resource.
     */
    @Override
    public final DataStore getOriginator() {
        return store;
    }

    /**
     * Returns the resource persistent identifier if available.
     */
    @Override
    public final Optional<GenericName> getIdentifier() {
        return Optional.of(identifier);
    }

    /**
     * Invoked the first time that the user requested a description of this resource.
     *
     * <h4>Implementation note</h4>
     * The {@link #getMetadata()} method, which is invoking this method, will freeze this metadata.
     * Therefore, if the metadata was still modifiable, it will usually not be modifiable anymore
     * after this method call.
     */
    @Override
    protected Metadata createMetadata() {
        return metadata;
    }

    /**
     * Returns the valid extent of grid coordinates together with the conversion from those grid
     * coordinates to real world coordinates.
     */
    @Override
    public final GridGeometry getGridGeometry() {
        return gridGeometry;
    }

    /**
     * Returns the ranges of sample values together with the conversion from samples to real values.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public final List<SampleDimension> getSampleDimensions() {
        return sampleDimensions;
    }

    /**
     * Returns the size of tiles in this resource.
     * The length of the returned array is the number of dimensions.
     */
    @Override
    protected final int[] getTileSize() {
        return new int[] {
            sampleModel.getWidth(),
            sampleModel.getHeight()
        };
    }

    /**
     * Returns the Java2D sample model describing pixel type and layout for the specified bands.
     * The raster size shall be the two first dimensions of the {@linkplain #getTileSize() tile size}.
     *
     * <p>If the {@code bands} argument is non-null, then this method returns {@code null} for telling
     * the caller to derive the sample model from {@code getSampleModel(null)}.</p>
     *
     * @param  bands  indices of desired bands, or {@code null} for all bands.
     * @return the sample model for tiles at full resolution, or {@code null}.
     */
    @Override
    protected final SampleModel getSampleModel(final int[] bands) {
        return (bands == null) ? sampleModel : null;
    }

    /**
     * Returns the Java2D color model for rendering images, or {@code null} if none.
     * The color model shall be compatible with the sample model returned by {@link #getSampleModel(int[])}.
     *
     * @param  bands  indices (not necessarily in increasing order) of desired bands, or {@code null} for all bands.
     * @return a color model compatible with {@link #getSampleModel(int[])}, or {@code null} if none.
     * @throws DataStoreException if an error occurred during color model construction.
     */
    @Override
    protected final ColorModel getColorModel(final int[] bands) throws DataStoreException {
        return (bands == null) ? colorModel : super.getColorModel(bands);
    }

    /**
     * Returns the object on which to perform synchronization.
     */
    @Override
    protected Object getSynchronizationLock() {
        return store;
    }

    /**
     * Loads a subset of the grid coverage represented by this resource.
     * While this method name suggests an immediate reading, the actual reading may be deferred.
     *
     * @param  domain  desired grid extent and resolution, or {@code null} for reading the whole domain.
     * @param  ranges  0-based indices of sample dimensions to read, or {@code null} or an empty sequence for reading them all.
     * @return the grid coverage for the specified domain and ranges.
     * @throws DataStoreException if an error occurred while reading the grid coverage data.
     */
    @Override
    public final GridCoverage read(final GridGeometry domain, final int ... range) throws DataStoreException {
        try {
            synchronized (getSynchronizationLock()) {
                return preload(new Coverage(new Subset(domain, range)));
            }
        } catch (RuntimeException e) {
            throw new DataStoreException(e);
        }
    }

    /**
     * Grid coverage read from the enclosing resource.
     * Tiles are read from the storage only when first needed.
     *
     * <h2>Cell coordinates</h2>
     * When there is no subsampling, this coverage uses the same cell coordinates as the originating resource.
     * When there is a subsampling, cell coordinates in this coverage are divided by the subsampling factors.
     * Conversions are done by {@link #coverageToResourceCoordinate(long, int)}.
     *
     * <h2>Tile matrix coordinate (<abbr>TMC</abbr>)</h2>
     * Indices of tiles starts at (0, 0, …).
     * Each {@code Coverage} instance uses its own, independent, Tile Matrix Coordinates (<abbr>TMC</abbr>).
     */
    final class Coverage extends TiledGridCoverage {
        /**
         * Creates a new tiled grid coverage.
         *
         * @param  subset  description of the {@link TiledGridResource} subset to cover.
         * @throws ArithmeticException if the number of tiles overflows 32 bits integer arithmetic.
         */
        Coverage(TiledGridResource.Subset subset) {
            super(subset);
        }

        /**
         * Returns a human-readable identification of this coverage.
         */
        @Override
        protected GenericName getIdentifier() {
            return identifier;
        }

        /**
         * Returns all tiles in the given area of interest. Tile indices are relative to this {@code Coverage}:
         * (0,0) is the tile in the upper-left corner of this {@code Coverage} (not necessarily the upper-left
         * corner of the image in the {@link TiledGridResource}). This method must be thread-safe.
         *
         * @param  iterator  an iterator over the tiles that intersect the Area Of Interest specified by user.
         * @return tiles decoded from the enclosing resource.
         */
        @Override
        protected Raster[] readTiles(final TileIterator iterator) throws IOException, DataStoreException {
            final Raster[] result = new Raster[iterator.tileCountInQuery];
            try (final var context = new ReadContext(iterator)) {
                synchronized (getSynchronizationLock()) {
                    do {
                        Raster tile = iterator.getCachedTile();
                        if (tile == null) {
                            long[] tileCoord = iterator.getTileCoordinatesInResource();
                            tile = readTile(tileCoord[0], tileCoord[1], context);
                            tile = iterator.cache(iterator.moveRaster(tile));
                        }
                        result[iterator.getTileIndexInResultArray()] = tile;
                    } while (iterator.next());
                }
            }
            return result;
        }

        /**
         * Context about a {@code readTile(…)} operation. Contains the tile to create, or
         * the image reader to use in the case of read operations delegated to Image I/O.
         */
        static final class ReadContext implements AutoCloseable {
            /**
             * Iterator over the tiles to read.
             */
            private final AOI iterator;

            /**
             * The image reader created for reading the tiles,
             * or {@code null} if image readers are not used.
             */
            private Map<ImageReaderSpi, ImageReader> readers;

            /**
             * Creates a new read context.
             *
             * @param  iterator  iterator over the tiles to read.
             */
            private ReadContext(final AOI iterator) {
                this.iterator = iterator;
            }

            /**
             * Returns the destination raster. This method shall be invoked at most once per tile.
             * It may be invoked not at all if the read operation is delegated to Image I/O,
             * in which case the image reader will create the raster itself.
             *
             * @return a newly created, initially empty raster.
             */
            public WritableRaster createRaster() {
                return iterator.createRaster();
            }

            /**
             * Returns an image reader created by the given provider.
             * This method caches the first reader created by this method,
             * then returns the cached value in subsequent calls.
             *
             * @param  spi  the provider of image reader.
             * @return an image reader instance created by the given provider.
             * @throws IOException if an error occurred while creating the image reader.
             */
            public ImageReader getReader(final ImageReaderSpi spi) throws IOException {
                if (readers == null) {
                    readers = new IdentityHashMap<>();
                }
                ImageReader reader = readers.get(spi);
                if (reader == null) {
                    reader = spi.createReaderInstance();
                    readers.put(spi, reader);
                }
                return reader;
            }

            /**
             * Invoked after a sequence of tiles have been read.
             * This method disposes the image readers.
             */
            @Override
            public void close() {
                if (readers != null) {
                    readers.values().forEach(ImageReader::dispose);
                }
            }
        }
    }

    /**
     * Reads a single tile.
     *
     * @param  tileX    0-based column index of the tile to read, starting from image left.
     * @param  tileY    0-based column index of the tile to read, starting from image top.
     * @param  context  contains the target raster or the image reader to use.
     * @return tile filled with the pixel values read by this method.
     */
    private Raster readTile(long tileX, long tileY, final Coverage.ReadContext context)
            throws IOException, DataStoreException
    {
        Image tile = tiles[0];
        final long tx = tileX / tile.numXTiles;
        final long ty = tileY / tile.numYTiles;
        tile = tiles[Math.toIntExact(addExact(tx, multiplyExact(ty, tileMatrixRowStride)))];
        tileX %= tile.numXTiles;
        tileY %= tile.numYTiles;
        return tile.readTile(store, tileX, tileY, context);
    }
}
