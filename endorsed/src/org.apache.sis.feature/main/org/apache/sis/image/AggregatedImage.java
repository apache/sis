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
package org.apache.sis.image;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.BitSet;
import java.util.Vector;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.RasterFormatException;
import java.awt.image.RenderedImage;
import java.awt.image.ImagingOpException;
import org.apache.sis.geometry.Shapes2D;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.image.internal.shared.WritableTiledImage;
import org.apache.sis.image.internal.shared.ImageUtilities;
import org.apache.sis.image.internal.shared.FillValues;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.pending.jdk.JDK18;
import org.apache.sis.system.CommonExecutor;
import org.apache.sis.util.Disposable;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;

// Specific to the GeoAPI 3.1 and 4.0 branches:
import org.opengis.coverage.grid.SequenceType;


/**
 * An image which is the result of aggregating the pixel values of other images.
 * Aggregating is the action of computing the pixel values of this image from the
 * values of overlapping pixels in source images.
 * The aggregation can be performed by statistic methods such as weighted average.
 * Each source image is associated with a {@link MathTransform} which converts pixel corner
 * coordinates from the <em>source</em> image to pixel corner coordinates in <em>this</em> image.
 *
 * <p>This image class can be used as an alternative to {@link ResampledImage}
 * when an aggregation method is desired instead of an interpolation method.
 * For example, weighted averages are preferred to bilinear interpolations when
 * pixel values are proportional to physical energy and the total energy needs to be preserved.
 * {@code AggregatedImage} can also be used as a fallback
 * when {@code ResampledImage} fails because of a {@link TransformException}.
 * Since {@code ResampledImage} and {@code AggregatedImage} use {@link MathTransform} in opposite directions,
 * an exception thrown in one direction may be absent in the opposite direction.</p>
 *
 * <p>Contrary to {@link ComputedImage}, this image computes all tiles immediately when one tile is requested.
 * For very large images, {@link ResampledImage} is preferred because it defers computation on a tile-by-tile
 * basis instead of computing the whole image.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.8
 *
 * @since SIS 1.8
 */
public class AggregatedImage extends MonolithicImage implements Disposable {
    /**
     * An image to aggregate together with its transforms from <em>source</em> to <em>aggregated</em> image.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.8
     *
     * @since SIS 1.8
     */
    public static class Source {
        /**
         * A source image providing values for the aggregated image to create.
         */
        public final RenderedImage source;

        /**
         * Transform of pixel coordinates from <em>source</em> image to <em>aggregated</em> image.
         * The transform shall map {@linkplain PixelInCell#CELL_CORNER pixel corners}.
         */
        public final MathTransform fromSource;

        /**
         * Creates a new (source, transform) pair.
         *
         * @param source     a source image providing values for the aggregated image to create.
         * @param fromSource transform of pixel coordinates from <em>source</em> image to <em>aggregated</em> image.
         */
        public Source(final RenderedImage source, final MathTransform fromSource) {
            this.source     = Objects.requireNonNull(source);
            this.fromSource = Objects.requireNonNull(fromSource);
            ArgumentChecks.ensureDimensionsMatch("fromSource", BIDIMENSIONAL, BIDIMENSIONAL, fromSource);
        }

        /*
         * A future version may add a global weight for the whole image. It may be useful
         * for aggregating images at different times or heights, with some times or heights
         * closer than others to the targeted time or height.
         */
    }

    /**
     * Images containing the pixels to aggregate in this image, together with their conversions
     * of pixel coordinates from <em>source</em> images to <em>this</em> image.
     *
     * The array is also used as a lock for things related to change detection in sources.
     */
    private final Source[] sources;

    /**
     * Domain of pixel coordinates in this image.
     */
    private final int minX, minY, width, height;

    /**
     * Index of the first tile. This is usually 0.
     */
    private final int minTileX, minTileY;

    /**
     * Number of tiles along the <var>x</var> and <var>y</var> directions.
     * Stored in order to guarantee that those values are stable.
     */
    private final int numXTiles, numYTiles;

    /**
     * The sample model declared at construction time.
     */
    protected final SampleModel sampleModel;

    /**
     * The fill values in a helper class for filling new tiles to that value.
     */
    private final FillValues filler;

    /**
     * Number of bands in which some values will be aggregated.
     * This is usually the number of bands given by {@link SampleModel#getNumBands()},
     * but may be smaller if all source images have a smaller number of bands.
     */
    private final int effectiveNumBands;

    /**
     * Number of values per sample needed by the accumulator during the computation phase.
     * For example we need two values for computing averages: the sum and the number of values in the sum.
     */
    private static final int NUM_ACCUMULATORS = 2;

    /**
     * Width and height of each chunk, in number of cells. Each cell contains many bands.
     * The size of accumulator chunks does not need to be related to the size of image tiles.
     * We prefer a relatively small chunk size in order to create small temporary arrays
     * during {@link SourceScanner} execution.
     */
    private static final int CHUNK_SIZE = 32;

    /**
     * Number of chunks needed for covering all the surface of this image.
     * The chunks are created only during the time when aggregation is performed.
     */
    private final int numChunks;

    /**
     * All tiles in a row-major fashion. The array elements are initially {@code null}.
     * All tiles will be created together the first time that a tile is requested.
     *
     * @see #getTile(int, int)
     */
    private final WritableRaster[] tiles;

    /**
     * Indexes of updated tiles for each source, or {@code null} if there are no change in the sources.
     * If the array is non-null, some elements may still be {@code null} if the source didn't changed.
     * Indexes are the same as {@link #sources}, i.e. {@code updatedSourceTiles[i]} lists which tiles
     * of {@code sources[i].source} need to be scanned again for updating this aggregated image.
     * Accesses to this flag should be synchronized on the {@link #sources} lock.
     *
     * @see #sourceTileChanged(RenderedImage, int, int)
     */
    private BitSet[] updatedSourceTiles;

    /**
     * Whether parallel execution is allowed.
     * If {@code true}, may threads can be used for aggregating the pixel values.
     */
    protected boolean parallel;

    /**
     * Creates a new image which will aggregate the given images. The operation requires transforms
     * (which can be non-linear) from the specified <em>source</em> images to <em>this</em> image.
     * The transforms shall map {@linkplain PixelInCell#CELL_CORNER pixel corners}.
     *
     * @param  sources      the images to aggregate with their transforms from <em>source</em> to <em>this</em>.
     * @param  layout       preferences about how to derive a sample model for this aggregated image.
     * @param  bounds       domain of pixel coordinates of this aggregated image, or {@code null} for automatic.
     * @param  aggregation  the method to use for performing aggregations.
     * @param  fillValues   the values to use for sample values not covered by a source.
     * @throws TransformException if {@code bounds} is null and an error occurred while computing automatic bounds.
     */
    public AggregatedImage(Source[] sources,
                     final ImageLayout layout,
                           Rectangle bounds,
                     final Aggregation aggregation,
                     final Number[] fillValues)
            throws TransformException
    {
        ArgumentChecks.ensureNonEmpty("sources",     sources);
        ArgumentChecks.ensureNonNull ("layout",      layout);
        ArgumentChecks.ensureNonNull ("aggregation", aggregation);   // Anticipation for a future version.
        this.sources = sources = sources.clone();
        RenderedImage template = null;
        /*
         * Store sources and transforms, checking their validity in same time.
         * Also compute the image bounds if it was not explicitly specified.
         */
        final boolean computeBounds = (bounds == null);
        for (int i=0; i<sources.length; i++) {
            final Source entry = sources[i];
            ArgumentChecks.ensureNonNullElement("sources", i, entry);
            final int n = ImageUtilities.getNumBands(entry.source);
            if (n > ImageUtilities.getNumBands(template)) {
                template = entry.source;
            }
            if (computeBounds) {
                final Rectangle boundsOfSource = ImageUtilities.getBounds(entry.source);
                Shapes2D.transform(MathTransforms.bidimensional(entry.fromSource), boundsOfSource, boundsOfSource);
                if (bounds == null) {
                    bounds = boundsOfSource;
                } else {
                    bounds.add(boundsOfSource);
                }
            }
        }
        /*
         * Store the specified or inferred image bounds.
         * Use the bounds for inferring a sample model.
         */
        ArgumentChecks.ensureStrictlyPositive("width",  width  = bounds.width);
        ArgumentChecks.ensureStrictlyPositive("height", height = bounds.height);
        numChunks = Math.multiplyExact(numChunks(width), numChunks(height));
        minX              = bounds.x;
        minY              = bounds.y;
        minTileX          = layout.preferredMinTileX;
        minTileY          = layout.preferredMinTileY;
        sampleModel       = layout.createCompatibleSampleModel(template, bounds);
        numXTiles         = JDK18.ceilDiv(width,  sampleModel.getWidth());
        numYTiles         = JDK18.ceilDiv(height, sampleModel.getHeight());
        tiles             = new WritableRaster[Math.multiplyExact(numXTiles, numYTiles)];
        effectiveNumBands = Math.min(sampleModel.getNumBands(), ImageUtilities.getNumBands(template));
        if (effectiveNumBands >= Integer.MAX_VALUE / (CHUNK_SIZE * CHUNK_SIZE * NUM_ACCUMULATORS)) {
            throw new RasterFormatException(Errors.format(Errors.Keys.IllegalPropertyValue_2, "numBands", effectiveNumBands));
        }
        filler = new FillValues(sampleModel, fillValues, true);
    }

    /**
     * Returns the number of chunks for the given size.
     *
     * @param  size  number of cells.
     * @return number of chunks needed for the given number of cells.
     */
    private static int numChunks(final int size) {
        return JDK18.ceilDiv(size, CHUNK_SIZE);
    }

    /**
     * Returns all source images.
     */
    @Override
    final RenderedImage[] getSourceImages() {
        final var images = new RenderedImage[sources.length];
        Arrays.setAll(images, (i) -> sources[i].source);
        return images;
    }

    /**
     * Returns the immediate sources of image data for this image.
     * This method returns all sources specified at construction time.
     *
     * @return the immediate sources.
     */
    @Override
    @SuppressWarnings("UseOfObsoleteCollectionType")
    public Vector<RenderedImage> getSources() {
        return new Vector<>(Arrays.asList(getSourceImages()));
    }

    /**
     * Returns the color model of this aggregated image.
     * The default implementation returns the color model of the first compatible source.
     *
     * @return the color model, or {@code null} if unspecified.
     */
    @Override
    public ColorModel getColorModel() {
        for (final Source entry : sources) {
            final ColorModel cm = entry.source.getColorModel();
            if (cm != null && cm.isCompatibleSampleModel(getSampleModel())) {
                return cm;
            }
        }
        return null;
    }

    /**
     * Returns the sample model associated with this image.
     * All rasters returned from this image will have this sample model.
     *
     * @return the sample model of this image.
     */
    @Override
    public SampleModel getSampleModel() {
        return sampleModel;
    }

    /**
     * Returns the number of columns in this image.
     * This is the {@link Rectangle#width} value of the {@code bounds} specified or inferred at construction time.
     *
     * @return number of columns in this image.
     */
    @Override
    public int getWidth() {
        return width;
    }

    /**
     * Returns the number of rows in this image.
     * This is the {@link Rectangle#height} value of the {@code bounds} specified or inferred at construction time.
     *
     * @return number of rows in this image.
     */
    @Override
    public int getHeight() {
        return height;
    }

    /**
     * Returns the minimum <var>x</var> coordinate (inclusive) of this image.
     * This is the {@link Rectangle#x} value of the {@code bounds} specified
     * or inferred at construction time.
     */
    @Override
    public int getMinX() {
        return minX;
    }

    /**
     * Returns the minimum <var>y</var> coordinate (inclusive) of this image.
     * This is the {@link Rectangle#y} value of the {@code bounds} specified
     * or inferred at construction time.
     */
    @Override
    public int getMinY() {
        return minY;
    }

    /**
     * Returns the minimum tile index in the <var>x</var> direction. This is often 0.
     */
    @Override
    public int getMinTileX() {
        return minTileX;
    }

    /**
     * Returns the minimum tile index in the <var>y</var> direction. This is often 0.
     */
    @Override
    public int getMinTileY() {
        return minTileY;
    }

    /**
     * Returns the number of tiles in the <var>x</var> direction.
     */
    @Override
    public int getNumXTiles() {
        return numXTiles;
    }

    /**
     * Returns the number of tiles in the <var>y</var> direction.
     */
    @Override
    public int getNumYTiles() {
        return numYTiles;
    }

    /**
     * Returns a tile of this image, computing it when needed.
     *
     * @param  tileX  the column index of the tile to get.
     * @param  tileY  the row index of the tile to get.
     * @return the tile at the given index (never null).
     * @throws IndexOutOfBoundsException if a given tile index is out of bounds.
     * @throws ImagingOpException if an error occurred while computing the image.
     */
    @Override
    @SuppressWarnings({"UseSpecificCatch", "NestedSynchronizedStatement"})
    public synchronized Raster getTile(final int tileX, final int tileY) {
        checkTileIndex("tileX", minTileX, numXTiles, tileX);
        checkTileIndex("tileY", minTileY, numYTiles, tileY);
        final int tileIndex = (tileY - minTileY) * numXTiles + (tileX - minTileX);
        Raster tile = tiles[tileIndex];
        BitSet[] sourcesToScan;
        synchronized (sources) {
            sourcesToScan = updatedSourceTiles;
            updatedSourceTiles = null;
        }
        if (tile == null || sourcesToScan != null) {
            registerSourceListeners();      // Does nothing if already registered.
            BitSet dirtyTiles = null;
            if (sourcesToScan != null) try {
                dirtyTiles = findDirtyTiles(sourcesToScan);
            } catch (TransformException e) {
                sourcesToScan = null;   // Mean to recompute everything.
                Logging.unexpectedException(ImageUtilities.LOGGER, AggregatedImage.class, "getTile", e);
            }
            try {
                // Execute the aggregation now, possibly using many threads.
                createTiles(new SourceScanner(sourcesToScan).call(), dirtyTiles);
            } catch (ImagingOpException e) {
                throw e;
            } catch (Exception cause) {
                final var e = new ImagingOpException(cause.getLocalizedMessage());
                e.initCause(Exceptions.unwrap(cause));
                throw e;
            }
            tile = tiles[tileIndex];
        }
        return tile;
    }

    /**
     * Creates the tiles from the data computed by {@code SourceScanner}.
     * The data layout is described in {@link SourceScanner#call()}.
     *
     * @param  data        row-major results of the aggregation of source images, separated in chunks.
     * @param  dirtyTiles  tiles in this image to recreate, or {@code null} for all tiles.
     */
    private void createTiles(final double[][] data, final BitSet dirtyTiles) {
        final int chunksPerImageRow = numChunks(width);
        final int chunkCellStride = effectiveNumBands * NUM_ACCUMULATORS;
        final var bounds = new Rectangle(CHUNK_SIZE, CHUNK_SIZE);
        final double[] fillValues = filler.asDoubles();
        final double[] samples = fillValues.clone();
        final var writable = new WriteAdapter(
                sampleModel,
                new Rectangle(minX, minY, width, height),
                new Point(minTileX, minTileY),
                tiles, dirtyTiles);

        for (int i=0; i<data.length; i++) {
            final double[] dataChunk = data[i];
            if (dataChunk != null) {
                data[i] = null;
                bounds.y = (i / chunksPerImageRow) * CHUNK_SIZE + minY;
                bounds.x = (i % chunksPerImageRow) * CHUNK_SIZE + minX;
                try (WritablePixelIterator it = new PixelIterator.Builder()
                            .setIteratorOrder(SequenceType.LINEAR)
                            .setRegionOfInterest(bounds)
                            .createWritable(writable))
                {
                    // Should always be zero because the first chunck starts at the image corner, but check anyway.
                    int valueIndex = ((it.lowerY - bounds.y)  * CHUNK_SIZE
                                   +  (it.lowerX - bounds.x)) * chunkCellStride;
                    final int toNextRow = (bounds.x + CHUNK_SIZE - it.upperX) * chunkCellStride;
                    while (it.next()) {
                        if (it.isBeginningOfNewRow()) {
                            valueIndex += toNextRow;
                        }
                        if (writable.dirtyTile) {
                            for (int j=0; j < effectiveNumBands; j++) {
                                // The two values counted by `NUM_ACCUMULATORS`.
                                final double sum = dataChunk[valueIndex++];
                                final double num = dataChunk[valueIndex++];
                                samples[j] = (num != 0) ? sum/num : fillValues[j];
                            }
                            it.setPixel(samples);
                        } else {
                            valueIndex += chunkCellStride;      // Skip the pixel without writing it.
                        }
                    }
                } finally {
                    writable.fireTilesUpdated();
                }
            }
        }
    }

    /**
     * An adapter for allowing {@link WritablePixelIterator} to write in this image.
     * This adapter also maintains a flag about whether the currently traversed tile
     * should be written. Non-dirty tiles should not be modified because all needed
     * source tiles may not have been scanned.
     */
    private final class WriteAdapter extends WritableTiledImage {
        /**
         * Whether the current tile should be updated.
         * If {@code false}, {@link AggregatedImage} shall not update the current tile.
         * Skipping such tiles is necessary otherwise their content would be corrupted because
         * of incomplete aggregated data when only a subset of the sources have been scanned.
         * This flag is updated when the pixel iterator moves from one tile to another.
         */
        boolean dirtyTile;

        /**
         * Row-major indexes of dirty tiles, or {@code null} if all tiles need to be computed.
         */
        private final BitSet dirtyTiles;

        /**
         * Indexes of tiles which are updated, together with whether to notify the listener.
         * The values are {@code false} when a tile is created for the first time.
         */
        private final Map<Point, Boolean> updatedTiles;

        /**
         * Creates a new adapter for writing the tiles of the enclosing aggregated image.
         * This is an adapter for allowing the use of {@link WritablePixelIterator}.
         */
        WriteAdapter(final SampleModel sampleModel,
                     final Rectangle   bounds,
                     final Point       minTile,
                     final WritableRaster[] tiles,
                     final BitSet dirtyTiles)
        {
            super(null, null, sampleModel, bounds, minTile, tiles);
            this.dirtyTiles = dirtyTiles;
            updatedTiles = new LinkedHashMap<>();
        }

        /**
         * Creates an initially empty tile filled with the fill value.
         */
        @Override
        protected WritableRaster createTile(final Point location) {
            final WritableRaster tile = super.createTile(location);
            if (!filler.isFullyZero) filler.fill(tile);
            return tile;
        }

        /**
         * Gets a writable tile and set the flag saying whether this tile should be updated.
         * If {@link #dirtyTile} is {@code false} after the invocation of this method, then
         * the caller shall not update the current tile.
         */
        @Override
        public WritableRaster getWritableTile(final int tileX, final int tileY) {
            final int tileIndex = (tileY - minTileY) * numXTiles + (tileX - minTileX);
            dirtyTile = (dirtyTiles == null) || dirtyTiles.get(tileIndex);
            if (dirtyTile) {
                final boolean tileExists = (tiles[tileIndex] != null);
                if (updatedTiles.putIfAbsent(new Point(tileX, tileY), tileExists) == null && tileExists) {
                    AggregatedImage.this.fireTileUpdate(tileX, tileY, true);
                }
            }
            return super.getWritableTile(tileX, tileY);
        }

        /**
         * Notifies listeners after we are sure that the tile updates are finished.
         * We wait until the iteration finished because the same tiles may be acquired, released,
         * then acquired again many times, and we want to notify the listeners only once.
         */
        final void fireTilesUpdated() {
            for (final Map.Entry<Point, Boolean> entry : updatedTiles.entrySet()) {
                if (entry.getValue()) {
                    final Point tile = entry.getKey();
                    AggregatedImage.this.fireTileUpdate(tile.x, tile.y, false);
                }
            }
            updatedTiles.clear();
        }
    }

    /**
     * A task for aggregating the values of source images. This class is not thread safe.
     * However, each {@code SourceScanner} instance can be subdivided into other instances
     * working on a subset of the images and tiles. Each sub-task can be executed
     * in a different thread and their results combined after task completion.
     *
     * <p><b>Design note:</b>
     * We do not use {@link java.util.concurrent.RecursiveTask} because the latter is designed
     * for a lot of small tasks, while we use a few large and potentially I/O bounded tasks.
     * Each task may create a lot of temporary arrays which need to be merged afterward,
     * therefore it is not necessarily advantageous to create a lot of tasks.</p>
     */
    private final class SourceScanner implements Callable<double[][]> {
        /**
         * Index of the next source image to aggregate.
         * This is initially 0, unless the work has been divided between many threads.
         */
        private int imageIndex;

        /**
         * Index after the last source image to aggregate.
         * This is {@code sources.length}, unless the work has been divided between many threads.
         */
        private final int upperImageIndex;

        /**
         * Index of the next tile to read in the current image.
         * This is initially 0, unless the work has been divided between many threads.
         */
        private int srcTileIndex;

        /**
         * Index after the last source image tile to aggregate, or -1 for aggregating all remaining tiles.
         * This is {@code numXTiles} × {@code numYTiles}, unless the work has been divided between many threads.
         * This value is updated every time that {@link #call()} iterates over a new image.
         */
        private int upperTileIndex;

        /**
         * For each source image, the row-major indexes of the tiles to scan.
         * If the array is {@code null}, there is no filtering to apply (all tiles are scanned).
         * If the array is non-null but an array element is {@code null}, the source is skipped.
         */
        private final BitSet[] sourcesToScan;

        /**
         * Number of forks that are allowed. This is 0 if parallel execution is not enabled.
         * The number of tasks will be 2ⁿ. Therefore, a value of 3 will create a maximum of 8 tasks.
         * We avoid creating too much tasks because each of them brings an additional cost in arrays
         * creation and merges.
         */
        private byte numAllowedForks;

        /**
         * Creates a new task initialized for aggregating all tiles of all images.
         *
         * @param  updatedSourceTiles  row-major indexes of tiles to scan in each image, or {@code null} for all.
         */
        SourceScanner(final BitSet[] updatedSourceTiles) {
            upperImageIndex = sources.length;
            upperTileIndex  = -1;
            numAllowedForks = parallel ? (byte) 3 : 0;
            sourcesToScan   = updatedSourceTiles;
        }

        /**
         * Creates a new task for aggregating half of the tiles handled by the given task.
         * This new task will work on a prefix of the tile collection while the given task
         * will work on the suffix.
         */
        private SourceScanner(final SourceScanner suffix, final int upperImageIndex, final int upperTileIndex) {
            this.imageIndex      = suffix.imageIndex;
            this.upperImageIndex = upperImageIndex;
            this.srcTileIndex    = suffix.srcTileIndex;
            this.upperTileIndex  = upperTileIndex;
            this.numAllowedForks = --suffix.numAllowedForks;
            this.sourcesToScan   = suffix.sourcesToScan;
        }

        /**
         * Number of values in each chunk. This is the length of the arrays to create
         * in the second dimension of {@link SourceScanner#compute()} return values.
         */
        private int accumulatorValuesPerChunk() {
            return CHUNK_SIZE * CHUNK_SIZE * NUM_ACCUMULATORS * effectiveNumBands;
        }

        /**
         * Iterates over the tiles of source images and aggregates their pixel values in a temporary array.
         * The aggregation result is returned in separate arrays of aggregated values for each target tiles.
         * The index in the first {@code double[][]} array dimension is the tile index (in the target image)
         * with tiles arranged in a row-major fashion. The index in the second array dimension is the sample
         * value index with <var>y</var> pixel coordinate varying slowest, then <var>x</var> pixel coordinate,
         * then band index, then accumulator index for a total length of {@link #accumulatorValuesPerChunk()}.
         *
         * @return row-major results of the aggregation of source images, separated in chunks.
         * @throws TransformException if an error occurred while transforming coordinates.
         * @throws InterruptedException if the current thread was interrupted while waiting for other threads.
         */
        @Override
        public double[][] call() throws TransformException, InterruptedException, ExecutionException {
            /*
             * If there are many images and we are allowed to fork,
             * delegates half of the images to a background thread.
             */
            final var parallelTasks = new ArrayList<Future<double[][]>>();
            while (numAllowedForks > 0) {
                final int half = (upperImageIndex - imageIndex) / 2 + imageIndex;
                if (half <= imageIndex) break;
                parallelTasks.add(CommonExecutor.instance().submit(new SourceScanner(this, half, -1)));
                imageIndex = half;
            }
            /*
             * Prepare pixel aggregation on the remaining suffix of the tile collection.
             * The code may decide again, inside the loop, to delegate half of the tiles
             * to a background thread.
             */
            final double[] imageOffset = {minX, minY};
            final double[][] data = new double[numChunks][];
            final double[] sourcePixelCorners = new double[BIDIMENSIONAL * 2];  // Bounds of a single source pixel in target coordinates.
            final int chunkValueStride = effectiveNumBands * NUM_ACCUMULATORS;
            final int chunksPerImageRow = numChunks(width);
images:     while (imageIndex < upperImageIndex) {
                BitSet tilesToScan = null;
                if (sourcesToScan != null) {
                    // Will compute only a subset of the sources and the tiles.
                    while ((tilesToScan = sourcesToScan[imageIndex]) == null) {
                        if (++imageIndex >= upperImageIndex) {
                            break images;
                        }
                    }
                }
                final MathTransform fromSource = sources[imageIndex].fromSource;
                final RenderedImage source = sources[imageIndex].source;
                final double[][] coordinates = new double[2][(source.getTileWidth() + 1) * BIDIMENSIONAL];
                final int srcMinX     = source.getMinX();
                final int srcMinY     = source.getMinY();
                final int srcMaxX     = Math.addExact(srcMinX, source.getWidth());
                final int srcMaxY     = Math.addExact(srcMinY, source.getHeight());
                final int srcMinTileX = source.getMinTileX();
                final int srcMinTileY = source.getMinTileY();
                final int srcNumTileX = source.getNumXTiles();
                if (upperTileIndex < 0) {
                    upperTileIndex = Math.multiplyExact(srcNumTileX, source.getNumYTiles());
                }
                while (numAllowedForks > 0) {
                    // Try to delegate half of the tiles to a background thread.
                    final int half = (upperTileIndex - srcTileIndex) / 2 + srcTileIndex;
                    if (half <= srcTileIndex) break;
                    if (tilesToScan != null && tilesToScan.previousSetBit(half - 1) < 0) break;  // No tile before.
                    parallelTasks.add(CommonExecutor.instance().submit(new SourceScanner(this, imageIndex + 1, half)));
                    srcTileIndex = half;
                }
                /*
                 * About to iterate over the tiles of a single image, or a subset of those tiles.
                 * Subset can be applied twice: we may be iterating over half, or a quarter, etc.
                 * of the tiles because of task division for parallelism (done in above block).
                 * In addition, we may have only a subset of the tiles which have been modified.
                 */
                while (srcTileIndex < upperTileIndex) {
                    if (tilesToScan != null) {
                        srcTileIndex = tilesToScan.nextSetBit(srcTileIndex);
                        if (srcTileIndex < 0 || srcTileIndex >= upperTileIndex) {
                            break;  // No other source tiles flagged as updated.
                        }
                    }
                    /*
                     * We will iterate over all pixels of a single tile of a source image,
                     * clipped to the image bounds because tiles in the last row and column
                     * may extend beyond the image. For each pixel, coordinates of 4 corners
                     * will be transformed to coordinates of a bounding box in target image
                     * (conceptually, actual implementation tries to be more efficient).
                     */
                    final Raster tile = source.getTile(srcMinTileX + (srcTileIndex % srcNumTileX),
                                                       srcMinTileY + (srcTileIndex / srcNumTileX));
                    final int numBands = Math.min(tile.getNumBands(), effectiveNumBands);
                    final int xmin     = Math.max(srcMinX, tile.getMinX());
                    final int ymin     = Math.max(srcMinY, tile.getMinY());
                    final int xmax     = Math.min(srcMaxX, Math.addExact(tile.getMinX(), tile.getWidth()));
                    final int ymax     = Math.min(srcMaxY, Math.addExact(tile.getMinY(), tile.getHeight()));
                    final int scanline = xmax - xmin + 1;       // +1 because xmax will be inclusive.
                    double[]  xyBottom = coordinates[ymax & 1];
                    for (int i=0, x=xmin; x <= xmax; x++) {     // Really the <= comparator.
                        xyBottom[i++] = x;
                        xyBottom[i++] = ymax;       // Bottom side of the last row of pixels.
                    }
                    fromSource.transform(xyBottom, 0, xyBottom, 0, scanline);
                    double[] sampleValues = null;
                    for (int y = ymax; --y >= ymin;) {
                        final double[] xyTop = coordinates[y & 1];
                        for (int i=0, x=xmin; x <= xmax; x++) {     // Really the <= comparator.
                            xyTop[i++] = x;
                            xyTop[i++] = y;
                        }
                        fromSource.transform(xyTop, 0, xyTop, 0, scanline);
                        for (int i=0, x=xmin; x<xmax; x++) {        // Now the < comparator (exclusive bound).
                            /*
                             * At this point, we can have values in all bands of one pixel of the source image.
                             * Find which pixels of that target image are overlapped by the source image pixel.
                             * There are potentially many pixels.
                             */
                            for (int j = 0; j < sourcePixelCorners.length;) {
                                final double v00 = xyTop   [i];
                                final double v10 = xyTop   [i + BIDIMENSIONAL];
                                final double v11 = xyBottom[i + BIDIMENSIONAL];
                                final double v01 = xyBottom[i];
                                final double off = imageOffset[i & 1];  // Even indexes are x, odd indexes are y.
                                sourcePixelCorners[j++] = Math.min(Math.min(v00, v10), Math.min(v01, v11)) - off;
                                sourcePixelCorners[j++] = Math.max(Math.max(v00, v10), Math.max(v01, v11)) - off;
                                i++;    // After we have done x, move to y.
                            }
                            final int lowerX = Math.max(0,      (int) (Math.floor(sourcePixelCorners[0])));
                            final int lowerY = Math.max(0,      (int) (Math.floor(sourcePixelCorners[2])));
                            final int upperX = Math.min(width,  (int) (Math.ceil (sourcePixelCorners[1])));
                            final int upperY = Math.min(height, (int) (Math.ceil (sourcePixelCorners[3])));
                            if (lowerX < upperX && lowerY < upperY) {
                                sampleValues = tile.getPixel(x, y, sampleValues);
                                for (int py=lowerY; py<upperY; py++) {
                                    final double weightY = Math.min(sourcePixelCorners[3], py + 1)
                                                         - Math.max(sourcePixelCorners[2], py);
                                    assert weightY > 0 && weightY <= 1: weightY;
                                    final int chunkIndexBase = (py / CHUNK_SIZE) * chunksPerImageRow;
                                    final int cellIndexBase  = (py % CHUNK_SIZE) * CHUNK_SIZE;
                                    for (int px=lowerX; px<upperX; px++) {
                                        final double weightX = Math.min(sourcePixelCorners[1], px + 1)
                                                             - Math.max(sourcePixelCorners[0], px);
                                        assert weightX > 0 && weightX <= 1: weightX;
                                        final double weight = weightX * weightY;
                                        final int cellIndex  = (px % CHUNK_SIZE) + cellIndexBase;
                                        final int chunkIndex = (px / CHUNK_SIZE) + chunkIndexBase;
                                        double[] dataChunk = data[chunkIndex];
                                        if (dataChunk == null) {
                                            data[chunkIndex] = dataChunk = new double[accumulatorValuesPerChunk()];
                                        }
                                        int valueIndex = cellIndex * chunkValueStride;
                                        for (int j=0; j<numBands; j++) {
                                            final double value = sampleValues[j];
                                            if (!Double.isNaN(value)) {
                                                dataChunk[valueIndex++] += weight * value;
                                                dataChunk[valueIndex++] += weight;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        xyBottom = xyTop;
                    }
                    // Finished pixel aggregation of one tile of source image. Move to next tile.
                    srcTileIndex++;
                }
                // Finished pixel aggregation of all tiles of one source image. Move to next image.
                srcTileIndex = 0;
                upperTileIndex = -1;
                imageIndex++;
            }
            /*
             * At this point, we finished to compute aggregations for all source images and tiles assigned
             * to this thread. More aggregations may have been computed in parallel by background threads.
             * Merges the data computed by other threads with the data computed by this thread.
             * The loop is executed only if parallel computation was enabled.
             */
            for (final Future<double[][]> fork : parallelTasks) {
                final double[][] other = fork.get();
                for (int i=0; i<other.length; i++) {
                    final double[] otherTile = other[i];
                    if (otherTile != null) {
                        other[i] = null;
                        final double[] dataChunk = data[i];
                        if (dataChunk == null) {
                            data[i] = otherTile;
                        } else {
                            for (int j = otherTile.length; --j >= 0;) {
                                dataChunk[j] += otherTile[j];
                            }
                        }
                    }
                }
            }
            return data;
        }
    }

    /**
     * Invoked when a tile of a source image has been updated.
     * The tile identified by the arguments may be scanned again the next time that {@link #getTile(int, int)} is
     * invoked, together with other source images and tiles involved in the same aggregation as the updated tile.
     *
     * @param source  the image that own the tile which has been updated.
     * @param tileX   the <var>x</var> index of the tile that has been updated.
     * @param tileY   the <var>y</var> index of the tile that has been updated.
     *
     * @see ComputedImage#sourceTileChanged(RenderedImage, int, int)
     */
    @Override
    protected void sourceTileChanged(final RenderedImage source, int tileX, int tileY) {
        tileX -= source.getMinTileX();
        tileY -= source.getMinTileY();
        if ((tileX | tileY) >= 0) {     // Paranoiac check.
            for (int imageIndex = 0; imageIndex < sources.length; imageIndex++) {
                if (sources[imageIndex].source == source) {
                    synchronized (sources) {
                        BitSet[] sourcesToScan = updatedSourceTiles;
                        if (sourcesToScan == null) {
                            updatedSourceTiles = sourcesToScan = new BitSet[sources.length];
                        }
                        BitSet tilesToScan = sourcesToScan[imageIndex];
                        if (tilesToScan == null) {
                            sourcesToScan[imageIndex] = tilesToScan = new BitSet();
                        }
                        tilesToScan.set(tileY * source.getNumXTiles() + tileX);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Finds which tiles of this images need to be computed after the specified changes in source images.
     * In addition, updates the given {@code sourcesToScan} for scanning all sources overlapping the dirty tiles.
     *
     * <p>Note: the dirty tiles will not necessarily have all their pixel overwritten with new values,
     * because the tiles of source images are not necessarily aligned with the tiles of this images.
     * This is okay because the pixel that were not overwritten are still valid is the source used
     * for computing their values did not changed.</p>
     *
     * @param  sourcesToScan  for each source image, the row-major indexes of the tiles to scan. May be updated.
     * @return row-major indexes of the dirty tiles in this image.
     * @throws TransformException if the bounds of a tile cannot be computed.
     */
    private BitSet findDirtyTiles(final BitSet[] sourcesToScan) throws TransformException {
        final var dirtyTiles = new BitSet();
        final var oneTile = new Rectangle(1, 1);
        final var limit = new Rectangle(minTileX, minTileY, numXTiles, numYTiles);
        for (int imageIndex = 0; imageIndex < sourcesToScan.length; imageIndex++) {
            final BitSet tilesToScan = sourcesToScan[imageIndex];
            if (tilesToScan != null) {
                final MathTransform2D fromSource = MathTransforms.bidimensional(sources[imageIndex].fromSource);
                final RenderedImage source = sources[imageIndex].source;
                final int srcMinTileX = source.getMinTileX();
                final int srcMinTileY = source.getMinTileY();
                final int srcNumTileX = source.getNumXTiles();
                for (int srcTileIndex = 0; (srcTileIndex = tilesToScan.nextSetBit(srcTileIndex)) >= 0; srcTileIndex++) {
                    oneTile.x = srcMinTileX + (srcTileIndex % srcNumTileX);
                    oneTile.y = srcMinTileY + (srcTileIndex / srcNumTileX);
                    Rectangle bounds = ImageUtilities.tilesToPixels(source, oneTile);
                    Shapes2D.transform(fromSource, bounds, bounds);
                    bounds = ImageUtilities.pixelsToTiles(this, bounds);
                    bounds = bounds.intersection(limit);
                    bounds.x -= minTileX;
                    bounds.y -= minTileY;
                    setInRect(dirtyTiles, bounds, numXTiles);
                }
            }
        }
        /*
         * Found all dirty tiles in this image. Now perform the reverse operation:
         * flags all source tiles that overlap with the dirty tiles.
         */
        for (int imageIndex = 0; imageIndex < sourcesToScan.length; imageIndex++) {
            final MathTransform2D toSource = MathTransforms.bidimensional(sources[imageIndex].fromSource.inverse());
            final RenderedImage source = sources[imageIndex].source;
            final int srcMinTileX = source.getMinTileX();
            final int srcMinTileY = source.getMinTileY();
            final int srcNumTileX = source.getNumXTiles();
            limit.setBounds(srcMinTileX, srcMinTileY, srcNumTileX, source.getNumYTiles());
            BitSet tilesToScan = sourcesToScan[imageIndex];
            for (int tileIndex = 0; (tileIndex = dirtyTiles.nextSetBit(tileIndex)) >= 0; tileIndex++) {
                oneTile.x = minTileX + (tileIndex % numXTiles);
                oneTile.y = minTileY + (tileIndex / numXTiles);
                Rectangle bounds = ImageUtilities.tilesToPixels(this, oneTile);
                Shapes2D.transform(toSource, bounds, bounds);
                bounds = ImageUtilities.pixelsToTiles(source, bounds);
                bounds = bounds.intersection(limit);
                bounds.x -= srcMinTileX;
                bounds.y -= srcMinTileY;
                if (!bounds.isEmpty()) {
                    if (tilesToScan == null) {
                        sourcesToScan[imageIndex] = tilesToScan = new BitSet();
                    }
                    setInRect(tilesToScan, bounds, srcNumTileX);
                }
            }
        }
        return dirtyTiles;
    }

    /**
     * Sets the bits in {@code dirtyTiles} for all tiles in the given rectangle.
     *
     * @param dirtyTiles  the bit set where to set some bits in a row-major fashion.
     * @param bounds      bounds of the bits to set.
     * @param scanline    the row length.
     */
    private static void setInRect(final BitSet dirtyTiles, final Rectangle bounds, final int scanline) {
        for (int y = bounds.y + bounds.height; --y >= bounds.y;) {
            final int base = y * scanline + bounds.x;
            dirtyTiles.set(base, base + bounds.width);
        }
    }

    /**
     * Advises this image that its tiles will no longer be requested.
     * This method removes all tiles from the cache and stops observation
     * of {@link java.awt.image.WritableRenderedImage} sources.
     * This image should not be used anymore after this method call.
     */
    @Override
    public synchronized void dispose() {
        Arrays.fill(tiles, null);
        updatedSourceTiles = null;
        unregisterSourceListeners();
    }
}
