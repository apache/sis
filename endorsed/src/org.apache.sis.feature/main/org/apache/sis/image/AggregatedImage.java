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
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.image.internal.shared.WritableTiledImage;
import org.apache.sis.image.internal.shared.ImageUtilities;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.pending.jdk.JDK18;
import org.apache.sis.system.CommonExecutor;
import org.apache.sis.util.Disposable;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.ArgumentChecks;
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
     * The sample model declared at construction time.
     */
    protected final SampleModel sampleModel;

    /**
     * The values to use if the source images provide no value.
     */
    private final double[] fillValues;

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
     * All tiles in a row-major fashion, created when first requested.
     *
     * @see #getTile(int, int)
     */
    private WritableRaster[] tiles;

    /**
     * Whether the tile content have been computed.
     */
    private boolean isComputed;

    /**
     * Whether listeners to writable images have been registered.
     * This is done only when first needed and use weak references.
     */
    private boolean hasAddedListeners;

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
     * @param  fillValues   the values to use if the source images have missing bands, or {@code null} for NaNs.
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
        effectiveNumBands = Math.min(sampleModel.getNumBands(), ImageUtilities.getNumBands(template));
        if (effectiveNumBands >= Integer.MAX_VALUE / (CHUNK_SIZE * CHUNK_SIZE * NUM_ACCUMULATORS)) {
            throw new RasterFormatException(Errors.format(Errors.Keys.IllegalPropertyValue_2, "numBands", effectiveNumBands));
        }
        final var values = new double[sampleModel.getNumBands()];
        Arrays.fill(values, Double.NaN);
        if (fillValues != null) {
            for (int i = Math.min(values.length, fillValues.length); --i >= 0;) {
                final Number value = fillValues[i];
                if (value != null) {
                    values[i] = value.doubleValue();
                }
            }
        }
        this.fillValues = values;
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
    public final int getWidth() {
        return width;
    }

    /**
     * Returns the number of rows in this image.
     * This is the {@link Rectangle#height} value of the {@code bounds} specified or inferred at construction time.
     *
     * @return number of rows in this image.
     */
    @Override
    public final int getHeight() {
        return height;
    }

    /**
     * Returns the minimum <var>x</var> coordinate (inclusive) of this image.
     * This is the {@link Rectangle#x} value of the {@code bounds} specified
     * or inferred at construction time.
     */
    @Override
    public final int getMinX() {
        return minX;
    }

    /**
     * Returns the minimum <var>y</var> coordinate (inclusive) of this image.
     * This is the {@link Rectangle#y} value of the {@code bounds} specified
     * or inferred at construction time.
     */
    @Override
    public final int getMinY() {
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
    public final int getMinTileY() {
        return minTileY;
    }

    /**
     * Returns the width of tiles in this image.
     *
     * @return the width (in pixels) of all tiles in this image.
     */
    @Override
    public int getTileWidth() {
        return getSampleModel().getWidth();
    }

    /**
     * Returns the height of tiles in this image.
     *
     * @return the height (in pixels) of all tiles in this image.
     */
    @Override
    public int getTileHeight() {
        return getSampleModel().getHeight();
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
    @SuppressWarnings("UseSpecificCatch")
    public synchronized Raster getTile(final int tileX, final int tileY) {
        final int numXTiles = getNumXTiles();
        final int numYTiles = getNumYTiles();
        checkTileIndex("tileX", minTileX, numXTiles, tileX);
        checkTileIndex("tileY", minTileY, numYTiles, tileY);
        final int chunkCellStride = effectiveNumBands * NUM_ACCUMULATORS;
        if (!isComputed) try {
            if (tiles == null) {
                tiles = new WritableRaster[Math.multiplyExact(numXTiles, numYTiles)];
            }
            if (!hasAddedListeners) {
                hasAddedListeners = true;
                registerSourceListeners();
            }
            // Execute the aggregation now, possibly using many threads.
            final double[][] data = new SourceScanner().call();
            /*
             * At this point, the calculatuon of all aggregated values is completed.
             * Dispatch the results in the tiles of this image.
             */
            final int chunksPerImageRow = numChunks(width);
            final var bounds = new Rectangle(CHUNK_SIZE, CHUNK_SIZE);
            final double[] samples = fillValues.clone();
            final var writable = new WritableTiledImage(
                    null,           // Properties,  not needed for dispatching values.
                    null,           // Color model, not needed for dispatching values.
                    getSampleModel(),
                    new Rectangle(minX, minY, width, height),
                    new Point(minTileX, minTileY),
                    tiles);

            for (int i=0; i<data.length; i++) {
                final double[] tileData = data[i];
                if (tileData != null) {
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
                            for (int j=0; j < effectiveNumBands; j++) {
                                // The two values counted by `NUM_ACCUMULATORS`.
                                final double sum = tileData[valueIndex++];
                                final double num = tileData[valueIndex++];
                                samples[j] = (num != 0) ? sum/num : fillValues[j];
                            }
                            it.setPixel(samples);
                        }
                    }
                }
            }
            isComputed = true;
        } catch (ImagingOpException e) {
            throw e;
        } catch (Exception cause) {
            final var e = new ImagingOpException(cause.getLocalizedMessage());
            e.initCause(Exceptions.unwrap(cause));
            throw e;
        }
        return tiles[(tileX - minTileX) + (tileY - minTileY) * numXTiles];
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
        private long srcTileIndex;

        /**
         * Index after the last source image tile to aggregate, or -1 for aggregating all remaining tiles.
         * This is {@code numTileX} × {@code numTileY}, unless the work has been divided between many threads.
         * This value is updated every time that {@link #call()} iterates over a new image.
         */
        private long upperTileIndex;

        /**
         * Number of forks that are allowed. This is 0 if parallel execution is not enabled.
         * The number of tasks will be 2ⁿ. Therefore, a value of 3 will create a maximum of 8 tasks.
         * We avoid creating too much tasks because each of them brings an additional cost in arrays
         * creation and merges.
         */
        private byte numAllowedForks;

        /**
         * Creates a new task initialized for aggregating all tiles of all images.
         */
        SourceScanner() {
            upperImageIndex = sources.length;
            upperTileIndex  = -1;
            numAllowedForks = parallel ? (byte) 3 : 0;
        }

        /**
         * Creates a new task for aggregating half of the tiles handled by the given task.
         * This new task will work on a prefix of the tile collection while the given task
         * will work on the suffix.
         */
        private SourceScanner(final SourceScanner suffix, final int upperImageIndex, final long upperTileIndex) {
            this.imageIndex      = suffix.imageIndex;
            this.upperImageIndex = upperImageIndex;
            this.srcTileIndex    = suffix.srcTileIndex;
            this.upperTileIndex  = upperTileIndex;
            this.numAllowedForks = --suffix.numAllowedForks;
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
         * @return result of the aggregation for each tile in the enclosing target image.
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
            final double[] imageOffset = {getMinX(), getMinY()};
            final double[][] data = new double[numChunks][];
            final double[] sourcePixelCorners = new double[BIDIMENSIONAL * 2];  // Bounds of a single source pixel in target coordinates.
            final int chunkValueStride = effectiveNumBands * NUM_ACCUMULATORS;
            final int chunksPerImageRow = numChunks(getWidth());
            while (imageIndex < upperImageIndex) {
                final MathTransform fromSource = sources[imageIndex].fromSource;
                final RenderedImage source = sources[imageIndex].source;
                final double[][] coordinates = new double[2][(source.getTileWidth() + 1) * BIDIMENSIONAL];
                final int srcMinTileX = source.getMinTileX();
                final int srcMinTileY = source.getMinTileY();
                final int srcNumTileX = source.getNumXTiles();
                if (upperTileIndex < 0) {
                    upperTileIndex = Math.multiplyFull(srcNumTileX, source.getNumYTiles());
                }
                while (numAllowedForks > 0) {
                    // Try to delegate half of the tiles to a background thread.
                    final long half = (upperTileIndex - srcTileIndex) / 2 + srcTileIndex;
                    if (half <= srcTileIndex) break;
                    parallelTasks.add(CommonExecutor.instance().submit(new SourceScanner(this, imageIndex + 1, half)));
                    srcTileIndex = half;
                }
                while (srcTileIndex < upperTileIndex) {
                    /*
                     * We will iterate over all pixels of a single tile of a source image.
                     * For each pixel, the coordinates of the 4 corners will be transformed
                     * to coordinates of a bounding box in the target image (conceptually,
                     * actual implementation tries to be more efficient).
                     */
                    final Raster tile = source.getTile(srcMinTileX + (int) (srcTileIndex % srcNumTileX),
                                                       srcMinTileY + (int) (srcTileIndex / srcNumTileX));
                    final int numBands = Math.min(tile.getNumBands(), effectiveNumBands);
                    final int scanline = tile.getWidth() + 1;   // +1 become xmax will be inclusive.
                    final int xmin     = tile.getMinX();
                    final int ymin     = tile.getMinY();
                    final int xmax     = Math.addExact(xmin, scanline - 1);
                    final int ymax     = Math.addExact(ymin, tile.getHeight());
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
                                        double[] tileData = data[chunkIndex];
                                        if (tileData == null) {
                                            data[chunkIndex] = tileData = new double[accumulatorValuesPerChunk()];
                                        }
                                        int valueIndex = cellIndex * chunkValueStride;
                                        for (int j=0; j<numBands; j++) {
                                            tileData[valueIndex++] += weight * sampleValues[j];
                                            tileData[valueIndex++] += weight;
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
             * Merges the data computed by other threads with the data computed by this thread.
             * The loop is executed only if parallel computation was enabled.
             */
            for (final Future<double[][]> fork : parallelTasks) {
                final double[][] other = fork.get();
                for (int i=0; i<other.length; i++) {
                    final double[] otherTile = other[i];
                    if (otherTile != null) {
                        other[i] = null;
                        final double[] tileData = data[i];
                        if (tileData == null) {
                            data[i] = otherTile;
                        } else {
                            for (int j = otherTile.length; --j >= 0;) {
                                tileData[j] += otherTile[j];
                            }
                        }
                    }
                }
            }
            return data;
        }
    }

    /**
     * Discards all tiles and marks this image as in need to be recomputed.
     * This method is invoked automatically if any source image is an instance of
     * {@link java.awt.image.WritableRenderedImage} and the content of that image changed.
     */
    @Override
    protected synchronized void recompute() {
        isComputed = false;
        /*
         * Existing tiles will be overwritten. No need to clear the tile content because
         * every pixels will be overwritten, since they stay at the exact same locations.
         */
    }

    /**
     * Advises this image that its tiles will no longer be requested.
     * This method removes all tiles from the cache and stops observation
     * of {@link java.awt.image.WritableRenderedImage} sources.
     * This image should not be used anymore after this method call.
     */
    @Override
    public synchronized void dispose() {
        tiles = null;
        isComputed = false;
        unregisterSourceListeners();
    }
}
