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
import java.util.Vector;
import java.util.Arrays;
import java.util.Objects;
import java.util.ConcurrentModificationException;
import java.util.concurrent.RecursiveTask;
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
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.BackingStoreException;

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
 * {@code AggregatedImage} can also be used as a fallback when {@code ResampledImage} fails to
 * resample an image because of a {@link TransformException}.
 * Since these two image classes use {@link MathTransform} in opposite directions,
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
public class AggregatedImage extends PlanarImage {
    /**
     * The images containing the pixels to aggregate in this image.
     */
    private final RenderedImage[] sources;

    /**
     * Conversions from pixel coordinates of <em>source</em> images to <em>this</em> image.
     * The transforms map {@linkplain PixelInCell#CELL_CORNER pixel corners}.
     */
    private final MathTransform[] fromSources;

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
     * Creates a new image which will aggregate the given images. The operation requires transforms
     * (which can be non-linear) from the specified <em>source</em> images to <em>this</em> image.
     * The transforms shall map {@linkplain PixelInCell#CELL_CORNER pixel corners}.
     *
     * <p>The given {@code sampleModel} determines the tile size and the target data type.
     * This is often the same sample model as the ones used by the source images,
     * but may also be different for forcing a different tile size or a different data type
     * (for example, casting {@code float} values to {@code byte}s).
     * If the specified sample model is not the same as the one used by the source images,
     * then subclass should override {@link #getColorModel()} for returning a color model which is
     * {@linkplain ColorModel#isCompatibleSampleModel(SampleModel) compatible with the sample model}.</p>
     *
     * @param  sources      the images to aggregate with their transforms from <em>source</em> to <em>this</em>.
     * @param  sampleModel  the sample model for this aggregated image.
     * @param  minTile      indices of the first tile ({@code minTileX}, {@code minTileY}), or {@code null} for (0,0).
     * @param  bounds       domain of pixel coordinates of this aggregated image, or {@code null} for automatic.
     * @param  fillValues   the values to use if the source images provide no value, or {@code null} for zeros.
     * @throws TransformException if {@code bounds} is null and an error occurred while computing automatic bounds.
     */
    public AggregatedImage(final Map<RenderedImage, MathTransform> sources,
                           final SampleModel sampleModel,
                           final Point minTile,
                           Rectangle bounds,
                           double[] fillValues)
            throws TransformException
    {
        final int numSources = sources.size();
        this.sources     = new RenderedImage[numSources];
        this.fromSources = new MathTransform[numSources];
        this.sampleModel = Objects.requireNonNull(sampleModel);
        final boolean computeBounds = (bounds == null);
        int i = 0, numBands = 0;
        for (final var entry : sources.entrySet()) {
            final RenderedImage source = entry.getKey();
            final MathTransform fromSource = entry.getValue();
            ArgumentChecks.ensureNonNullElement("sources[#].key",   i, source);
            ArgumentChecks.ensureNonNullElement("sources[#].value", i, fromSource);
            ArgumentChecks.ensureDimensionsMatch("sources", BIDIMENSIONAL, BIDIMENSIONAL, fromSource);
            this.fromSources[i] = fromSource;
            this.sources[i++] = source;
            numBands = Math.max(numBands, ImageUtilities.getNumBands(source));
            if (computeBounds) {
                final Rectangle boundsOfSource = ImageUtilities.getBounds(source);
                Shapes2D.transform(MathTransforms.bidimensional(fromSource), boundsOfSource, boundsOfSource);
                if (bounds == null) {
                    bounds = boundsOfSource;
                } else {
                    bounds.add(boundsOfSource);
                }
            }
        }
        if (i != numSources) {
            throw new ConcurrentModificationException();    // New entries added while we were iterating.
        }
        minX = bounds.x;
        minY = bounds.y;
        ArgumentChecks.ensureStrictlyPositive("width",  width  = bounds.width);
        ArgumentChecks.ensureStrictlyPositive("height", height = bounds.height);
        effectiveNumBands = Math.min(sampleModel.getNumBands(), numBands);
        if (effectiveNumBands >= Integer.MAX_VALUE / (CHUNK_SIZE * CHUNK_SIZE * NUM_ACCUMULATORS)) {
            throw new RasterFormatException(Errors.format(Errors.Keys.IllegalPropertyValue_2, "numBands", effectiveNumBands));
        }
        if (fillValues == null) {
            fillValues = new double[effectiveNumBands];
        } else {
            fillValues = Arrays.copyOf(fillValues, effectiveNumBands);
        }
        this.fillValues = fillValues;
        numChunks = Math.multiplyExact(numChunks(width), numChunks(height));
        if (minTile != null) {
            minTileX = minTile.x;
            minTileY = minTile.y;
        } else {
            minTileX = 0;
            minTileY = 0;
        }
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
     * Returns the immediate sources of image data for this image.
     * This method returns all sources specified at construction time.
     *
     * @return the immediate sources.
     */
    @Override
    @SuppressWarnings("UseOfObsoleteCollectionType")
    public Vector<RenderedImage> getSources() {
        return new Vector<>(Arrays.asList(sources));
    }

    /**
     * Returns the color model of this aggregated image.
     * The default implementation returns the color model of the first compatible source.
     *
     * @return the color model, or {@code null} if unspecified.
     */
    @Override
    public ColorModel getColorModel() {
        for (final RenderedImage source : sources) {
            final ColorModel cm = source.getColorModel();
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
    public synchronized Raster getTile(final int tileX, final int tileY) {
        final int numXTiles = getNumXTiles();
        final int numYTiles = getNumYTiles();
        checkTileIndex("tileX", minTileX, numXTiles, tileX);
        checkTileIndex("tileY", minTileY, numYTiles, tileY);
        final int chunkCellStride = effectiveNumBands * NUM_ACCUMULATORS;
        if (tiles == null) try {
            // Execute the aggregation now, possibly using many threads.
            final double[][] data = new SourceScanner().compute();
            /*
             * At this point, the calculatuon of all aggregated values is completed.
             * Dispatch the results in the tiles of this image.
             */
            final int chunksPerImageRow = numChunks(width);
            final var bounds = new Rectangle(CHUNK_SIZE, CHUNK_SIZE);
            final var samples = new double[effectiveNumBands];
            final var writable = new WritableTiledImage(
                    null,           // Properties,  not needed for dispatching values.
                    null,           // Color model, not needed for dispatching values.
                    getSampleModel(),
                    new Rectangle(minX, minY, width, height),
                    new Point(minTileX, minTileY),
                    tiles = new WritableRaster[numXTiles * numYTiles]);

            for (int i=0; i<data.length; i++) {
                final double[] tileData = data[i];
                if (tileData != null) {
                    bounds.y = (i / chunksPerImageRow) * CHUNK_SIZE + minX;
                    bounds.x = (i % chunksPerImageRow) * CHUNK_SIZE + minY;
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
                            for (int j=0; j < samples.length; j++) {
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
     */
    @SuppressWarnings("serial")     // Not intended to be serialized.
    private final class SourceScanner extends RecursiveTask<double[][]> {
        /**
         * Index of the next source image to aggregate.
         */
        private int imageIndex;

        /**
         * Index after the last source image to aggregate.
         */
        private final int upperImageIndex;

        /**
         * Creates a new task initialized for aggregating all tiles of all images.
         */
        SourceScanner() {
            upperImageIndex = sources.length;
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
         * @return Result of the aggregation for each tile in the enclosing target image.
         */
        @Override
        protected double[][] compute() {
            final double[] imageOffset = {getMinX(), getMinY()};
            final double[][] data = new double[numChunks][];
            final double[] sourcePixelCorners = new double[BIDIMENSIONAL * 2];  // Bounds of a single source pixel in target coordinates.
            final int chunkValueStride = effectiveNumBands * NUM_ACCUMULATORS;
            final int chunksPerImageRow = numChunks(getWidth());
            while (imageIndex < upperImageIndex) {
                final MathTransform fromSource = fromSources[imageIndex];
                final RenderedImage source = sources[imageIndex];
                final double[][] coordinates = new double[2][(source.getTileWidth() + 1) * BIDIMENSIONAL];
                final int srcMinTileX = source.getMinTileX();
                final int srcMinTileY = source.getMinTileY();
                final int srcNumTileX = source.getNumXTiles();
                final int srcNumTiles = Math.multiplyExact(srcNumTileX, source.getNumYTiles());
                for (int srcTileIndex = 0; srcTileIndex < srcNumTiles; srcTileIndex++) try {
                    /*
                     * We will iterate over all pixels of a single tile of a source image.
                     * For each pixel, the coordinates of the 4 corners will be transformed
                     * to coordinates of a bounding box in the target image (conceptually,
                     * actual implementation tries to be more efficient).
                     */
                    final Raster tile = source.getTile(srcMinTileX + (srcTileIndex % srcNumTileX),
                                                       srcMinTileY + (srcTileIndex / srcNumTileX));
                    final int numBands = Math.min(tile.getNumBands(), effectiveNumBands);
                    final int scanline = tile.getWidth();
                    final int xmin     = tile.getMinX();
                    final int ymin     = tile.getMinY();
                    final int xmax     = Math.addExact(xmin, scanline);
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
                } catch (TransformException e) {
                    throw new BackingStoreException(e);
                }
                imageIndex++;
            }
            return data;
        }
    }
}
