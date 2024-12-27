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

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRenderedImage;
import java.util.Objects;
import java.util.function.Consumer;
import javax.measure.Quantity;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.image.privy.ImageUtilities;
import org.apache.sis.image.privy.TileOpExecutor;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.measure.Units;


/**
 * Combines an arbitrary number of images into a single one.
 * The combined images may use different coordinate systems if a resampling operation is specified.
 * The workflow is as below:
 *
 * <ol>
 *   <li>Creates an {@code ImageCombiner} with the destination image where to write.</li>
 *   <li>Configure with methods such as {@link #setInterpolation setInterpolation(…)}.</li>
 *   <li>Invoke {@link #accept accept(…)} or {@link #resample resample(…)}
 *       methods for each image to combine.</li>
 *   <li>Get the combined image with {@link #result()}.</li>
 * </ol>
 *
 * Images are combined in the order they are specified.
 * If the same pixel is written by many images, then the final value is the pixel of the last image specified.
 * The last pixel values win even if those pixels are transparent, i.e. the alpha band is handled like ordinary bands.
 *
 * <p>For each image, bands are written in the destination image in the order they appear, regardless the color model.
 * This class does not try to match colors. For example, it does not verify if band order needs to be reversed because
 * an image is <abbr>RGB</abbr> and the other image is <abbr>BVR</abbr>.
 * It is caller responsibility to ensure that bands are in the same order.</p>
 *
 * <h2>Alternative</h2>
 * A single call to the {@link ImageProcessor#overlay ImageProcessor.overlay(…)} method can produce the same result
 * as multiple calls to the {@link #accept(RenderedImage)} method of this class. The main difference between the two
 * methods is that this class writes immediately in the given destination image, while {@code ImageProcessor} creates
 * an image which will compute the requested tiles on the fly.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @see ImageProcessor#overlay(RenderedImage[], Rectangle)
 *
 * @since 1.1
 */
public class ImageCombiner implements Consumer<RenderedImage> {
    /**
     * The image processor for resampling operation.
     */
    private final ImageProcessor processor;

    /**
     * The destination image where to write the images given to this {@code ImageCombiner}.
     */
    private final WritableRenderedImage destination;

    /**
     * Creates an image combiner which will write in the given image. That image is not cleared;
     * pixels that are not overwritten by calls to the {@code accept(…)} or {@code resample(…)}
     * methods will be left unchanged.
     *
     * @param  destination  the image where to combine images.
     */
    public ImageCombiner(final WritableRenderedImage destination) {
        this(destination,  new ImageProcessor());
    }

    /**
     * Creates an image combiner which will use the given processor for resampling operations.
     * The given destination image is not cleared; pixels that are not overwritten by calls to
     * the {@code accept(…)} or {@code resample(…)} methods will be left unchanged.
     *
     * @param  destination  the image where to combine images.
     * @param  processor    the processor to use for resampling operations.
     *
     * @since 1.2
     */
    public ImageCombiner(final WritableRenderedImage destination, final ImageProcessor processor) {
        this.destination = Objects.requireNonNull(destination);
        this.processor   = Objects.requireNonNull(processor);
    }

    /**
     * Returns the interpolation method to use during resample operations.
     *
     * @return interpolation method to use during resample operations.
     *
     * @see #resample(RenderedImage, Rectangle, MathTransform)
     */
    public Interpolation getInterpolation() {
        return processor.getInterpolation();
    }

    /**
     * Sets the interpolation method to use during resample operations.
     *
     * @param  method  interpolation method to use during resample operations.
     *
     * @see #resample(RenderedImage, Rectangle, MathTransform)
     */
    public void setInterpolation(final Interpolation method) {
        processor.setInterpolation(method);
    }

    /**
     * Returns hints about the desired positional accuracy, in "real world" units or in pixel units.
     * If the returned array is non-empty and contains accuracies large enough,
     * {@code ImageCombiner} may use some slightly faster algorithms at the expense of accuracy.
     *
     * @return desired accuracy in no particular order, or an empty array if none.
     *
     * @see ImageProcessor#getPositionalAccuracyHints()
     */
    public Quantity<?>[] getPositionalAccuracyHints() {
        return processor.getPositionalAccuracyHints();
    }

    /**
     * Sets hints about desired positional accuracy, in "real world" units or in pixel units.
     * Accuracy can be specified in real world units such as {@linkplain Units#METRE metres}
     * or in {@linkplain Units#PIXEL pixel units}, which are converted to real world units depending
     * on image resolution. If more than one value is applicable to a dimension
     * (after unit conversion if needed), the smallest value is taken.
     *
     * @param  hints  desired accuracy in no particular order, or a {@code null} array if none.
     *                Null elements in the array are ignored.
     *
     * @see ImageProcessor#setPositionalAccuracyHints(Quantity...)
     */
    public void setPositionalAccuracyHints(final Quantity<?>... hints) {
        processor.setPositionalAccuracyHints(hints);
    }

    /**
     * Returns the type of number used for representing the values of each band.
     *
     * @return the type of number capable to hold sample values of each band.
     *
     * @since 1.4
     */
    public DataType getBandType() {
        return DataType.forBands(destination);
    }

    /**
     * Writes the given image on top of destination image. The given source image shall use the same pixel
     * coordinate system than the destination image (but not necessarily the same tile indices).
     * For every (<var>x</var>,<var>y</var>) pixel coordinates in the destination image:
     *
     * <ul>
     *   <li>If (<var>x</var>,<var>y</var>) are valid {@code source} pixel coordinates,
     *       then the source pixel values overwrite the destination pixel values.</li>
     *   <li>Otherwise the destination pixel is left unchanged.</li>
     * </ul>
     *
     * Note that source pixels overwrite destination pixels even if they are transparent.
     * In order words, the alpha channel is treated as an ordinary band.
     * Bands are written in the order they appear regardless their color model.
     *
     * @param  source  the image to write on top of destination image.
     *
     * @see ImageProcessor#overlay(RenderedImage[], Rectangle)
     */
    @Override
    public void accept(final RenderedImage source) {
        ArgumentChecks.ensureNonNull("source", source);
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final WritableRenderedImage destination = this.destination;
        final Rectangle bounds = ImageUtilities.getBounds(source);
        ImageUtilities.clipBounds(destination, bounds);
        if (!bounds.isEmpty()) {
            final TileOpExecutor executor = new TileOpExecutor(source, bounds) {
                @Override protected void readFrom(final Raster tile) {
                    destination.setData(tile);
                }
            };
            executor.readFrom(processor.prefetch(source, bounds));
        }
    }

    /**
     * Combines the result of resampling the given image. The resampling operation is defined by a potentially
     * non-linear transform from the <em>destination</em> image to the specified <em>source</em> image.
     * That transform should map {@linkplain org.apache.sis.coverage.grid.PixelInCell#CELL_CENTER pixel centers}.
     *
     * <h4>Properties used</h4>
     * This operation uses the following properties in addition to method parameters:
     * <ul>
     *   <li>{@linkplain #getInterpolation() Interpolation method} (nearest neighbor, bilinear, <i>etc</i>).</li>
     *   <li>{@linkplain #getPositionalAccuracyHints() Positional accuracy hints}
     *       for enabling faster resampling at the cost of lower precision.</li>
     * </ul>
     *
     * Contrarily to {@link ImageProcessor}, this method does not use {@linkplain ImageProcessor#getFillValues() fill values}.
     * Destination pixels that cannot be mapped to source pixels are left unchanged.
     *
     * @param  source    the image to be resampled.
     * @param  bounds    domain of pixel coordinates in the destination image, or {@code null} for the whole image.
     * @param  toSource  conversion of pixel coordinates from destination image to {@code source} image.
     *
     * @see ImageProcessor#resample(RenderedImage, Rectangle, MathTransform)
     */
    public void resample(final RenderedImage source, Rectangle bounds, final MathTransform toSource) {
        ArgumentChecks.ensureNonNull("source",   source);
        ArgumentChecks.ensureNonNull("toSource", toSource);
        if (bounds == null) {
            bounds = ImageUtilities.getBounds(destination);
        }
        final int  tileWidth       = destination.getTileWidth();
        final int  tileHeight      = destination.getTileHeight();
        final long tileGridXOffset = destination.getTileGridXOffset();
        final long tileGridYOffset = destination.getTileGridYOffset();
        final int  minTileX        = Math.toIntExact(Math.floorDiv((bounds.x - tileGridXOffset), tileWidth));
        final int  minTileY        = Math.toIntExact(Math.floorDiv((bounds.y - tileGridYOffset), tileHeight));
        final int  minX            = Math.toIntExact(Math.multiplyFull(minTileX, tileWidth)  + tileGridXOffset);
        final int  minY            = Math.toIntExact(Math.multiplyFull(minTileY, tileHeight) + tileGridYOffset);
        /*
         * Expand the target bounds until it contains an integer number of tiles, computed using the size
         * of destination tiles. We have to do that because the resample operation below is not free to
         * choose a tile size suiting the given bounds.
         */
        long maxX = (bounds.x + (long) bounds.width)  - 1;                                  // Inclusive.
        long maxY = (bounds.y + (long) bounds.height) - 1;
        maxX = ((maxX - tileGridXOffset) / tileWidth  + 1) * tileWidth  + tileGridXOffset;  // Exclusive.
        maxY = ((maxY - tileGridYOffset) / tileHeight + 1) * tileHeight + tileGridYOffset;
        bounds = new Rectangle(minX, minY,
                Math.toIntExact(maxX - minX),
                Math.toIntExact(maxY - minY));
        /*
         * Values of (minTileX, minTileY) computed above will cause `ResampledImage.getTileGridOffset()`
         * to return the exact same value as `destination.getTileGridOffset()`. This is a requirement
         * of `setDestination(…)` method.
         */
        final RenderedImage result;
        synchronized (processor) {
            final ImageLayout layout = ImageLayout.forDestination(destination, minTileX, minTileY);
            final ImageLayout previous = processor.getImageLayout();
            try {
                processor.setImageLayout(layout);
                result = processor.resample(source, bounds, toSource);
            } finally {
                processor.setImageLayout(previous);
            }
        }
        /*
         * Check if the result is writing directly in the destination image.
         */
        if (result instanceof ComputedImage && ((ComputedImage) result).getDestination() == destination) {
            processor.prefetch(result, ImageUtilities.getBounds(destination));
        } else {
            accept(result);
        }
    }

    /**
     * Returns the combination of destination image with all images specified to {@code ImageCombiner} methods.
     * This may be the destination image specified at construction time, but may also be a larger image if the
     * destination has been dynamically expanded for accommodating larger sources.
     *
     * <p><b>Note:</b> dynamic expansion is not yet implemented in current version.
     * If a future version implements it, we shall guarantee that the coordinate of each pixel is unchanged
     * (i.e. the image {@code minX} and {@code minY} may become negative, but the pixel identified by
     * coordinates (0,0) for instance will stay the same pixel.)</p>
     *
     * @return the combination of destination image with all source images.
     */
    public RenderedImage result() {
        return destination;
    }
}
