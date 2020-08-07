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

import java.util.Arrays;
import java.util.Optional;
import java.nio.Buffer;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.awt.image.SampleModel;
import java.util.NoSuchElementException;
import org.opengis.coverage.grid.SequenceType;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.feature.Resources;
import org.apache.sis.internal.coverage.j2d.ImageUtilities;

import static java.lang.Math.floorDiv;
import static org.apache.sis.internal.util.Numerics.ceilDiv;


/**
 * An iterator over sample values in a raster or an image.  This iterator makes easier to read and write efficiently
 * pixel or sample values. The iterator {@linkplain RenderedImage#getTile(int,int) acquires tiles} and releases them
 * automatically. Unless otherwise specified, iterators are free to use an {@linkplain #getIterationOrder() iteration
 * order} that minimize the "acquire / release tile" operations (in other words, iterations are not necessarily from
 * left to right). Iteration can be performed on a complete image or only a sub-region of it. Some optimized iterator
 * implementations exist for a few commonly used {@linkplain java.awt.image.SampleModel sample models}.
 *
 * <div class="note"><b>Example:</b>
 * {@preformat java
 *     PixelIterator it = PixelIterator.create(image);
 *     double[] samples = null;
 *     while (it.next()) {
 *         samples = it.getPixel(samples);      // Get values in all bands.
 *         // Perform computation here...
 *     }
 * }
 * </div>
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
public abstract class PixelIterator {
    /**
     * The image in which iteration is occurring, or {@code null} if none.
     * If {@code null}, then {@link #currentRaster} must be non-null.
     */
    final RenderedImage image;

    /**
     * The current raster in which iteration is occurring. This may change when the iterator
     * reaches a new {@link #image} tile. May be {@code null} if not yet determined.
     *
     * @see RenderedImage#getTile(int, int)
     */
    Raster currentRaster;

    /**
     * Number of bands in all tiles in the {@linkplain #image}.
     * The {@link #currentRaster} shall always have this number of bands.
     */
    final int numBands;

    /**
     * The domain, in pixel coordinates, of the region traversed by this pixel iterator.
     * This may be smaller than the image or raster bounds, but not greater.
     * The lower values are inclusive and the upper values are exclusive.
     *
     * @see #getDomain()
     */
    final int lowerX, lowerY, upperX, upperY;

    /**
     * Size of all tiles in the {@link #image}.
     */
    final int tileWidth, tileHeight;

    /**
     * The X and Y coordinate of the upper-left pixel of tile (0,0).
     * Note that tile (0,0) may not actually exist.
     */
    final int tileGridXOffset, tileGridYOffset;

    /**
     * The domain, in tile coordinates, of the region traversed by this pixel iterator.
     * The tile index ranges may be smaller than the ranges of valid indices in image,
     * but not greater. The lower values are inclusive and the upper values exclusive.
     */
    final int tileLowerX, tileLowerY, tileUpperX, tileUpperY;

    /**
     * Size of the window to use in {@link #createWindow(TransferType)} method, or {@code 0} if none.
     */
    final int windowWidth, windowHeight;

    /**
     * Creates an iterator for the given region in the given raster.
     *
     * @param  data     the raster which contains the sample values on which to iterate.
     * @param  subArea  the raster region where to perform the iteration, or {@code null}
     *                  for iterating over all the raster domain.
     * @param  window   size of the window to use in {@link #createWindow(TransferType)} method, or {@code null} if none.
     */
    PixelIterator(final Raster data, final Rectangle subArea, final Dimension window) {
        final Rectangle bounds;
        image           = null;
        currentRaster   = data;
        numBands        = data.getNumBands();
        tileWidth       = data.getWidth();
        tileHeight      = data.getHeight();
        tileGridXOffset = data.getMinX();
        tileGridYOffset = data.getMinY();
        tileLowerX      = 0;                    // In this case only one raster: tile index is fixed to 0.
        tileLowerY      = 0;
        tileUpperX      = 1;
        tileUpperY      = 1;
        bounds          = intersection(tileGridXOffset, tileGridYOffset, tileWidth, tileHeight, subArea, window);
        lowerX          = bounds.x;
        lowerY          = bounds.y;
        upperX          = Math.addExact(lowerX, bounds.width);
        upperY          = Math.addExact(lowerY, bounds.height);
        windowWidth     = (window != null) ? window.width  : 0;
        windowHeight    = (window != null) ? window.height : 0;
    }

    /**
     * Creates an iterator for the given region in the given image.
     *
     * @param  data     the image which contains the sample values on which to iterate.
     * @param  subArea  the image region where to perform the iteration, or {@code null}
     *                  for iterating over all the image domain.
     * @param  window   size of the window to use in {@link #createWindow(TransferType)} method, or {@code null} if none.
     */
    PixelIterator(final RenderedImage data, final Rectangle subArea, final Dimension window) {
        final Rectangle bounds;
        image           = data;
        numBands        = data.getSampleModel().getNumBands();
        tileWidth       = data.getTileWidth();
        tileHeight      = data.getTileHeight();
        tileGridXOffset = data.getTileGridXOffset();
        tileGridYOffset = data.getTileGridYOffset();
        bounds          = intersection(data.getMinX(), data.getMinY(), data.getWidth(), data.getHeight(), subArea, window);
        lowerX          = bounds.x;
        lowerY          = bounds.y;
        upperX          = Math.addExact(lowerX, bounds.width);
        upperY          = Math.addExact(lowerY, bounds.height);
        tileLowerX      = floorDiv(Math.subtractExact(lowerX, tileGridXOffset), tileWidth);
        tileLowerY      = floorDiv(Math.subtractExact(lowerY, tileGridYOffset), tileHeight);
        tileUpperX      =  ceilDiv(Math.subtractExact(upperX, tileGridXOffset), tileWidth);
        tileUpperY      =  ceilDiv(Math.subtractExact(upperY, tileGridYOffset), tileHeight);
        windowWidth     = (window != null) ? window.width  : 0;
        windowHeight    = (window != null) ? window.height : 0;
    }

    /**
     * Computes the intersection between the given bounds and {@code subArea} if {@code subArea} is non-null.
     * If the result is empty, then the width and/or height are set to zero (not negative).
     */
    private static Rectangle intersection(int x, int y, int width, int height, Rectangle subArea, Dimension window) {
        if (window != null) {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException(Resources.format(Resources.Keys.EmptyImage));
            }
            ArgumentChecks.ensureBetween("window.width",  1, width,  window.width);
            ArgumentChecks.ensureBetween("window.height", 1, height, window.height);
            width  -= (window.width  - 1);
            height -= (window.height - 1);
        }
        Rectangle bounds = new Rectangle(x, y, width, height);
        if (subArea != null) {
            bounds = bounds.intersection(subArea);
            if (bounds.width  < 0) bounds.width  = 0;
            if (bounds.height < 0) bounds.height = 0;
        }
        return bounds;
    }

    /**
     * Builds pixel iterators for specified region of interest, window size or iteration order.
     * By default, the builder creates iterators for all pixels in the given raster or image,
     * with unspecified iteration order. Users can invoke setter methods for specifying
     * desired behavior for the iterators to create.
     *
     * <div class="note"><b>Example:</b>
     * {@preformat java
     *     PixelIterator iterator = new PixelIterator.Builder().setRegionOfInterest(new Rectangle(10, 10, 5, 5).create(image);
     * }
     * </div>
     */
    public static class Builder {
        /**
         * The region where to perform the iteration, or {@code null} for iterating over all the domain.
         */
        private Rectangle subArea;

        /**
         * Size of the window to use in {@link PixelIterator#createWindow(TransferType)} method,
         * or {@code null} if none.
         */
        private Dimension window;

        /**
         * The desired iteration order, or {@code null} for a default order.
         */
        private SequenceType order;

        /**
         * Creates a new iterator builder with no region of interest, no window size and default iterator order.
         */
        public Builder() {
        }

        /**
         * Sets the region (in pixel coordinates) where to perform the iteration.
         * By default, iterators will traverse all pixels in the given image or raster.
         *
         * @param  subArea  region where to iterator, or {@code null} for iterating over all image domain.
         * @return {@code this} for method call chaining.
         */
        public Builder setRegionOfInterest(final Rectangle subArea) {
            this.subArea = subArea;
            return this;
        }

        /**
         * Sets the size of the window to use in {@link PixelIterator#createWindow(TransferType)} method.
         * By default, iterators do not create windows.
         *
         * @param  window  the window size, or {@code null} if no window will be created.
         * @return {@code this} for method call chaining.
         */
        public Builder setWindowSize(final Dimension window) {
            this.window = window;
            return this;
        }

        /**
         * Sets the desired iteration order.
         * The {@code order} argument can have the following values:
         *
         * <table class="sis">
         *   <caption>Supported iteration order</caption>
         *   <tr><th>Value</th>                         <th>Iteration order</th>                                <th>Supported on</th></tr>
         *   <tr><td>{@code null}</td>                  <td>Most efficient iteration order.</td>                <td>Image and raster</td></tr>
         *   <tr><td>{@link SequenceType#LINEAR}</td>   <td>From left to right, then from top to bottom.</td>   <td>Raster only</td></tr>
         * </table>
         *
         * Any other {@code order} value will cause an {@link IllegalArgumentException} to be thrown.
         * More iteration orders may be supported in future Apache SIS versions.
         *
         * @param  order  the desired iteration order, or {@code null} for a default order.
         * @return {@code this} for method call chaining.
         */
        public Builder setIteratorOrder(final SequenceType order) {
            if (order == null || order.equals(SequenceType.LINEAR)) {
                this.order = order;
            } else {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedType_1, order));
            }
            return this;
        }

        /**
         * If the given image is a wrapper doing nothing else than computing a property value,
         * unwraps it since the iterators are not interested in properties. This unwrapping is
         * also necessary for allowing the builder to recognize the {@link BufferedImage} case.
         */
        private static RenderedImage unwrap(RenderedImage image) {
            while (image instanceof ImageAdapter) {
                image = ((ImageAdapter) image).source;
            }
            return image;
        }

        /**
         * Creates a read-only iterator for the given raster.
         *
         * @param  data  the raster which contains the sample values on which to iterate.
         * @return a new iterator traversing pixels in the given raster.
         */
        public PixelIterator create(final Raster data) {
            ArgumentChecks.ensureNonNull("data", data);
            if (order == SequenceType.LINEAR) {
                return new LinearIterator(data, null, subArea, window);
            } else if (order != null) {
                throw new IllegalStateException(Errors.format(Errors.Keys.UnsupportedType_1, order));
            }
            // TODO: check here for cases that we can optimize (after we ported corresponding implementations).
            return new DefaultIterator(data, null, subArea, window);
        }

        /**
         * Creates a read-only iterator for the given image.
         *
         * @param  data  the image which contains the sample values on which to iterate.
         * @return a new iterator traversing pixels in the given image.
         */
        public PixelIterator create(RenderedImage data) {
            ArgumentChecks.ensureNonNull("data", data);
            data = unwrap(data);
            if (data instanceof BufferedImage) {
                return create(((BufferedImage) data).getRaster());
            }
            /*
             * Note: As of Java 14, `BufferedImage.getTileGridXOffset()` and `getTileGridYOffset()` have a bug.
             * They should return `BufferedImage.getMinX()` (which is always 0) because the image contains only
             * one tile at index (0,0).  But they return `raster.getSampleModelTranslateX()` instead, which may
             * be non-zero if the image is a sub-region of another image.  Delegating to `create(Raster)` avoid
             * this problem in addition of being a slight optimization.
             *
             * Issue tracker: https://bugs.openjdk.java.net/browse/JDK-8166038
             */
            if (order == SequenceType.LINEAR) {
                return new LinearIterator(data, null, subArea, window);
            } else if (order != null) {
                throw new IllegalStateException(Errors.format(Errors.Keys.UnsupportedType_1, order));
            }
            // TODO: check here for cases that we can optimize (after we ported corresponding implementations).
            return new DefaultIterator(data, null, subArea, window);
        }

        /**
         * Creates a read/write iterator for the given raster.
         *
         * @param  data  the raster which contains the sample values on which to iterate.
         * @return a new iterator traversing pixels in the given raster.
         */
        public WritablePixelIterator createWritable(final WritableRaster data) {
            ArgumentChecks.ensureNonNull("data", data);
            return createWritable(data, data);
        }

        /**
         * Creates a read/write iterator for the given image.
         *
         * @param  data  the image which contains the sample values on which to iterate.
         * @return a new iterator traversing pixels in the given image.
         */
        public WritablePixelIterator createWritable(final WritableRenderedImage data) {
            ArgumentChecks.ensureNonNull("data", data);
            if (data instanceof BufferedImage) {
                return createWritable(((BufferedImage) data).getRaster());
            }
            return createWritable(data, data);
        }

        /**
         * Creates an iterator which will read and write in two different rasters.
         *
         * @param  input    the raster which contains the sample values to read.
         * @param  output   the raster where to write the sample values. Can be the same than {@code input}.
         * @return a new writable iterator.
         */
        public WritablePixelIterator createWritable(final Raster input, final WritableRaster output) {
            ArgumentChecks.ensureNonNull("input",  input);
            ArgumentChecks.ensureNonNull("output", output);
            if (order == SequenceType.LINEAR) {
                return new LinearIterator(input, output, subArea, window);
            } else if (order != null) {
                throw new IllegalStateException(Errors.format(Errors.Keys.UnsupportedType_1, order));
            }
            // TODO: check here for cases that we can optimize (after we ported corresponding implementations).
            return new DefaultIterator(input, output, subArea, window);
        }

        /**
         * Creates an iterator which will read and write in two different images.
         *
         * @param  input    the image which contains the sample values to read.
         * @param  output   the image where to write the sample values. Can be the same than {@code input}.
         * @return a new writable iterator.
         */
        public WritablePixelIterator createWritable(RenderedImage input, final WritableRenderedImage output) {
            ArgumentChecks.ensureNonNull("input",  input);
            ArgumentChecks.ensureNonNull("output", output);
            input = unwrap(input);
            if (order == SequenceType.LINEAR) {
                return new LinearIterator(input, output, subArea, window);
            } else if (order != null) {
                throw new IllegalStateException(Errors.format(Errors.Keys.UnsupportedType_1, order));
            }
            // TODO: check here for cases that we can optimize (after we ported corresponding implementations).
            return new DefaultIterator(input, output, subArea, window);
        }
    }

    /**
     * Creates an iterator for all pixels in the given image.
     * This is a convenience method for {@code new Builder().create(data)}.
     *
     * @param  data  the image which contains the sample values on which to iterate.
     * @return a new iterator traversing all pixels in the given image, in arbitrary order.
     */
    public static PixelIterator create(final RenderedImage data) {
        return new Builder().create(data);
    }

    /**
     * Returns {@code true} if this iterator can write pixel values (after cast to {@code WritablePixelIterator}).
     * This method should be used instead than {@code instanceof} check because, for some implementations, being
     * an instance of {@code WritablePixelIterator} is not a sufficient condition.
     *
     * @return {@code true} if this iterator can safely be casted to {@link WritablePixelIterator} and used for
     *         writing pixel values.
     */
    public boolean isWritable() {
        return false;
    }

    /**
     * Returns the most efficient type ({@code int}, {@code float} or {@code double}) for transferring data between the
     * underlying rasters and this iterator. The transfer type is not necessarily the storage type used by the rasters.
     * For example {@code int} values will be used for transferring data even if the underlying rasters store all sample
     * values as {@code byte}s.
     *
     * <p>The transfer type is only a hint since all iterator methods work for any type (conversions are applied as needed).
     * However if this method returns {@link TransferType#INT}, then {@link #getSample(int)} and {@link #getPixel(int[])}
     * will be slightly more efficient than equivalent methods for other types. Conversely if this method returns
     * {@link TransferType#DOUBLE}, then {@link #getSampleDouble(int)} will be both more efficient and avoid accuracy lost.</p>
     *
     * @return the most efficient data type for transferring data.
     */
    public TransferType<?> getTransferType() {
        return TransferType.valueOf(image != null ? image.getSampleModel().getTransferType() : currentRaster.getTransferType());
    }

    /**
     * Returns the range of sample values that can be stored in each band of the rendered image or raster.
     * The ranges depend on the data type (byte, integer, <i>etc.</i>) and the number of bits per sample.
     * If the samples are stored as floating point values, then the ranges are infinite (unbounded).
     *
     * <p>Usually, the range is the same for all bands. A situation where the ranges may differ is when an
     * image uses {@link java.awt.image.SinglePixelPackedSampleModel}, in which case the number of bits per
     * pixel may vary for different bands.</p>
     *
     * @return the ranges of valid sample values for each band. Ranges may be {@linkplain NumberRange#isBounded() unbounded}.
     */
    public NumberRange<?>[] getSampleRanges() {
        /*
         * Take the sample model of current tile if possible. This details should be irrelevant (so we do not mention
         * it in above javadoc) because the sample model shall be the same for all tiles. But if inconsistency happens,
         * stay consistent at least with the fact that all getter methods in PixelIterator return information relative
         * to current iterator position.
         */
        final SampleModel model = (currentRaster != null) ? currentRaster.getSampleModel() : image.getSampleModel();
        final NumberRange<?>[] ranges = new NumberRange<?>[model.getNumBands()];
        if (ranges.length != 0) {
            final int dataType = model.getDataType();
            if (ImageUtilities.isIntegerType(dataType)) {
                int bandToDefine = 0, lastDefinedBand;
                do {
                    final int size = model.getSampleSize(bandToDefine);
                    long minimum = 0;
                    long maximum = Numerics.bitmask(size) - 1;
                    if (!ImageUtilities.isUnsignedType(model)) {
                        maximum >>>= 1;                                 // Convert unsigned range to signed range.
                        minimum = ~maximum;
                    }
                    final NumberRange<?> range = (dataType == DataBuffer.TYPE_BYTE || dataType == DataBuffer.TYPE_SHORT)
                                               ? NumberRange.create((short) minimum, true, (short) maximum, true)
                                               : NumberRange.create((int)   minimum, true, (int)   maximum, true);
                    ranges[bandToDefine] = range;
                    /*
                     * Usually all bands have the same number of bits, and consequently the same range of values.
                     * For handling this common case, loop below shares the same `NumberRange` instance with all
                     * bands having same characteristic. If at least one band has a different number of bits, the
                     * `bandToDefine` index will point to the first occurrence and the computation is repeated.
                     */
                    lastDefinedBand = bandToDefine;
                    for (int band = ranges.length; --band > lastDefinedBand;) {
                        if (ranges[band] == null) {
                            if (model.getSampleSize(band) == size) {
                                ranges[band] = range;
                            } else {
                                bandToDefine = band;
                            }
                        }
                    }
                } while (bandToDefine > lastDefinedBand);
            } else {
                Arrays.fill(ranges, (dataType == DataBuffer.TYPE_FLOAT)
                        ? NumberRange.create(Float. NEGATIVE_INFINITY, false, Float. POSITIVE_INFINITY, false)
                        : NumberRange.create(Double.NEGATIVE_INFINITY, false, Double.POSITIVE_INFINITY, false));
            }
        }
        return ranges;
    }

    /**
     * Returns the order in which pixels are traversed. {@link SequenceType#LINEAR} means that pixels on the first
     * row are traversed from left to right, then pixels on the second row from left to right, <i>etc.</i>
     * An empty value means that the iteration order is unspecified.
     *
     * @return order in which pixels are traversed.
     */
    public abstract Optional<SequenceType> getIterationOrder();

    /**
     * Returns the number of bands (samples per pixel) in the image or raster.
     *
     * @return number of bands.
     */
    public int getNumBands() {
        return numBands;
    }

    /**
     * Returns the pixel coordinates of the region where this iterator is doing the iteration.
     * If no region was specified at construction time, then this method returns the image or raster bounds.
     *
     * @return pixel coordinates of the iteration region.
     */
    public Rectangle getDomain() {
        return new Rectangle(lowerX, lowerY, upperX - lowerX, upperY - lowerY);
    }

    /**
     * Returns the column (x) and row (y) indices of the current pixel.
     * The {@link #next()} or {@link #moveTo(int,int)} method must have been invoked before this method.
     * Indices of the first pixel are not necessarily zero; they can even be negative.
     *
     * @return column and row indices of current iterator position.
     * @throws IllegalStateException if this method is invoked before the first call to {@link #next()}
     *         or {@link #moveTo(int,int)}, or after {@code next()} returned {@code false}.
     */
    public abstract Point getPosition();

    /**
     * Returns {@code true} if current iterator position is inside the given shape.
     * Current version does not require implementations to check if iteration started or finished
     * (this method is non-public for that reason).
     *
     * @param  domain  the shape for which to test inclusion.
     * @return whether current iterator position is inside the given shape.
     */
    boolean isInside(final Shape domain) {
        return domain.contains(getPosition());
    }

    /**
     * Moves the pixel iterator to the given column (x) and row (y) indices. After this method invocation,
     * the iterator state is as if the {@link #next()} method has been invoked just before to reach the
     * specified position.
     *
     * <div class="note"><b>Usage example:</b>
     * {@preformat java
     *     iterator.moveTo(x, y);
     *     do {
     *         int sample = iterator.getSample(band);
     *         // Use sample value here...
     *     } while (iterator.next());
     * }
     * </div>
     *
     * @param  x  the column index of the pixel to make current.
     * @param  y  the row index of the pixel to make current.
     * @throws IndexOutOfBoundsException if the given indices are outside the iteration domain.
     */
    public abstract void moveTo(int x, int y);

    /**
     * Moves the iterator to the next pixel. A pixel iterator is initially positioned before the first pixel.
     * The first call to {@code next()} makes the first pixel the current one; the second call makes the second
     * pixel the current one, <i>etc.</i> The second pixel is not necessarily on the same row than the first one;
     * iteration order is implementation dependent.
     *
     * <p>When a call to {@code next()} returns {@code false}, the iterator is positioned after the last pixel.
     * Any invocation of a {@code getSample(int)} method will result in a {@link NoSuchElementException} to be
     * thrown.</p>
     *
     * @return {@code true} if the current pixel is valid, or {@code false} if there is no more pixels.
     * @throws IllegalStateException if this iterator already reached end of iteration in a previous call
     *         to {@code next()}, and {@link #rewind()} or {@link #moveTo(int,int)} have not been invoked.
     */
    public abstract boolean next();

    /**
     * Returns the sample value in the specified band of current pixel, rounded toward zero.
     * The {@link #next()} method must have returned {@code true}, or the {@link #moveTo(int,int)} method must have
     * been invoked successfully, before this {@code getSample(int)} method is invoked. If above condition is not met,
     * then this method behavior is undefined: it may throw any runtime exception or return a meaningless value
     * (there is no explicit bounds check for performance reasons).
     *
     * @param  band  the band for which to get the sample value.
     * @return sample value in specified band of current pixel.
     *
     * @see Raster#getSample(int, int, int)
     */
    public abstract int getSample(int band);

    /**
     * Returns the sample value in the specified band of current pixel as a single-precision floating point number.
     * The {@link #next()} method must have returned {@code true}, or the {@link #moveTo(int,int)} method must have
     * been invoked successfully, before this {@code getSampleFloat(int)} method is invoked. If above condition is
     * not met, then this method behavior is undefined: it may throw any runtime exception or return a meaningless
     * value (there is no explicit bounds check for performance reasons).
     *
     * @param  band  the band for which to get the sample value.
     * @return sample value in specified band of current pixel.
     *
     * @see Raster#getSampleFloat(int, int, int)
     */
    public abstract float getSampleFloat(int band);

    /**
     * Returns the sample value in the specified band of current pixel, without precision lost.
     * The {@link #next()} method must have returned {@code true}, or the {@link #moveTo(int,int)} method must have
     * been invoked successfully, before this {@code getSampleDouble(int)} method is invoked. If above condition is
     * not met, then this method behavior is undefined: it may throw any runtime exception or return a meaningless
     * value (there is no explicit bounds check for performance reasons).
     *
     * @param  band  the band for which to get the sample value.
     * @return sample value in specified band of current pixel.
     *
     * @see Raster#getSampleDouble(int, int, int)
     */
    public abstract double getSampleDouble(int band);

    /**
     * Returns the sample values of current pixel for all bands.
     * The {@link #next()} method must have returned {@code true}, or the {@link #moveTo(int,int)} method must have
     * been invoked successfully, before this {@code getPixel(…)} method is invoked. If above condition is not met,
     * then this method behavior is undefined: it may throw any runtime exception or return a meaningless value
     * (there is no explicit bounds check for performance reasons).
     *
     * @param  dest  a pre-allocated array where to store the sample values, or {@code null} if none.
     * @return the sample values for current pixel.
     *
     * @see Raster#getPixel(int, int, int[])
     */
    public abstract int[] getPixel​(int[] dest);

    /**
     * Returns the sample values of current pixel for all bands.
     * The {@link #next()} method must have returned {@code true}, or the {@link #moveTo(int,int)} method must have
     * been invoked successfully, before this {@code getPixel(…)} method is invoked. If above condition is not met,
     * then this method behavior is undefined: it may throw any runtime exception or return a meaningless value
     * (there is no explicit bounds check for performance reasons).
     *
     * @param  dest  a pre-allocated array where to store the sample values, or {@code null} if none.
     * @return the sample values for current pixel.
     *
     * @see Raster#getPixel(int, int, float[])
     */
    public abstract float[] getPixel​(float[] dest);

    /**
     * Returns the sample values of current pixel for all bands.
     * The {@link #next()} method must have returned {@code true}, or the {@link #moveTo(int,int)} method must have
     * been invoked successfully, before this {@code getPixel(…)} method is invoked. If above condition is not met,
     * then this method behavior is undefined: it may throw any runtime exception or return a meaningless value
     * (there is no explicit bounds check for performance reasons).
     *
     * @param  dest  a pre-allocated array where to store the sample values, or {@code null} if none.
     * @return the sample values for current pixel.
     *
     * @see Raster#getPixel(int, int, double[])
     */
    public abstract double[] getPixel​(double[] dest);

    /**
     * Returns the data elements (not necessarily band values) of current pixel.
     * The {@code Object} argument and return value is a relatively opaque format (it may be {@code int[]},
     * {@code byte[]}, <i>etc.</i>): it is used for transferring values in a packed format between compatible
     * Java2D sample or color models. That {@code Object} should generally not be used directly by the caller.
     *
     * <div class="note"><b>Example:</b>
     * if an image has Red, Green, Blue and Alpha bands, then the {@link #getPixel(int[])} methods will return
     * arrays of length 4 containing the individual values for each band, no matter how those bands are stored
     * in the image. By contrast this {@code getDataElements​(…)} method may return an array of length 1 with
     * all sample values packed as a single ARGB value.</div>
     *
     * Data elements are useful for copying efficiently values in another image using the same sample model,
     * or for getting colors with a call to {@link java.awt.image.ColorModel#getRGB(Object)}.
     *
     * @param  dest  a pre-allocated array where to store the data elements, or {@code null} if none.
     * @return the data elements for current pixel.
     *
     * @see Raster#getDataElements​(int, int, Object)
     *
     * @since 1.1
     */
    public abstract Object getDataElements(Object dest);

    /**
     * Returns a moving window over the sample values in a rectangular region starting at iterator position.
     * The <cite>window size</cite> must have been specified at {@code PixelIterator} construction time.
     * The current iterator position is the window corner having the smallest <var>x</var> and <var>y</var> coordinates.
     * This is typically, but not necessarily (depending on axis orientations) the window upper-left corner.
     * Sample values are stored in a sequence of length
     * <var>(number of bands)</var> × <var>(window width)</var> × <var>(window height)</var>.
     * Values are always stored with band index varying fastest, then column index, then row index.
     * Columns are traversed from left to right and rows are traversed from top to bottom
     * ({@link SequenceType#LINEAR} iteration order).
     * That order is the same regardless the {@linkplain #getIterationOrder() iteration order} of this iterator.
     *
     * <div class="note"><b>Example:</b>
     * for an RGB image, the 3 first values are the red, green and blue components of the pixel at
     * {@linkplain #getPosition() current iterator position}. The 3 next values are the red, green
     * and blue components of the pixel at the right of current iterator position (not necessarily
     * the position where a call to {@link #next()} would have go), <i>etc.</i></div>
     *
     * Calls to {@link #next()} or {@link #moveTo(int,int)} followed by {@link Window#update()}
     * replaces the window content with values starting at the new iterator position.
     * Before the first {@link Window#update()} invocation, the window is filled with zero values.
     *
     * <p>If this iterator is used for
     * {@linkplain WritablePixelIterator#setPixel(int[]) writing pixel values at current position},
     * those write operations may change the content of windows at {@linkplain #next() next positions}
     * unless the iteration order of this iterator is {@link SequenceType#LINEAR}.</p>
     *
     * <div class="note"><b>Usage example:</b>
     * following code creates an iterator over the full area of given image, then a window of 5×5 pixels.
     * The window is moved over all the image area in iteration order. Inside the window, data are copied
     * in {@linkplain SequenceType#LINEAR linear order} regardless the iteration order.
     *
     * {@preformat java
     *     PixelIterator it = create(image, null, new Dimension(5, 5), null);     // Windows size will be 5×5 pixels.
     *     PixelIterator<FloatBuffer> window = it.createWindow(TransferType.FLOAT);
     *     FloatBuffer values = window.values;
     *     while (it.next()) {
     *         window.update();
     *         while (buffer.hasRemaining()) {
     *             float sample = buffer.get();
     *             // use the sample value here.
     *         }
     *     }
     * }
     * </div>
     *
     * @param  <T>   the type of the data buffer to use for transferring data.
     * @param  type  the desired type of values ({@code int}, {@code float} or {@code double}).
     *               Use {@link #getTransferType()} if the most efficient type is desired.
     * @return a window over the sample values in the underlying image or raster.
     *
     * @see Raster#getPixels(int, int, int, int, double[])
     */
    public abstract <T extends Buffer> Window<T> createWindow(TransferType<T> type);

    /**
     * Contains the sample values in a moving window over the image. Windows are created by calls to
     * {@link PixelIterator#createWindow(TransferType)} and sample values are stored in {@link Buffer}s.
     * The buffer content is replaced ever time {@link #update()} is invoked.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.1
     *
     * @param  <T>  the type of buffer which can be used for transferring data.
     *
     * @since 0.8
     * @module
     */
    public abstract static class Window<T extends Buffer> {
        /**
         * A buffer containing all sample values fetched by the last call to {@link #update()}. The buffer
         * capacity is <var>(number of bands)</var> × <var>(window width)</var> × <var>(window height)</var>.
         * Values are always stored with band index varying fastest, then column index, then row index.
         * Columns are traversed from left to right and rows are traversed from top to bottom
         * ({@link SequenceType#LINEAR} iteration order).
         * That order is the same regardless the {@linkplain PixelIterator#getIterationOrder() iteration order}
         * of enclosing iterator.
         *
         * <p>Every time that {@link #update()} is invoked, the buffer content is replaced by sample values
         * starting at the {@linkplain PixelIterator#getPosition() current iterator position}.
         * Before the first {@code update()} invocation, the buffer is filled with zero values.</p>
         */
        public final T values;

        /**
         * Creates a new window which will store the sample values in the given buffer.
         */
        Window(final T buffer) {
            values = buffer;
        }

        /**
         * Returns the width and height of this window in pixels.
         *
         * @return the window size in pixels.
         *
         * @since 1.1
         */
        public abstract Dimension getSize();

        /**
         * Updates this window with the sample values in the region starting at current iterator position.
         * The buffer position, limit and mark are {@linkplain Buffer#clear() cleared}.
         *
         * <p>The {@link #next()} method must have returned {@code true}, or the {@link #moveTo(int,int)} method must have
         * been invoked successfully, before this {@code update()} method is invoked. If above condition is not met,
         * then this method behavior is undefined: it may throw any runtime exception or return meaningless values
         * (there is no explicit bounds check for performance reasons).</p>
         */
        public abstract void update();
    }

    /**
     * Restores the iterator to the start position. After this method has been invoked,
     * the iterator is in the same state than after construction.
     */
    public abstract void rewind();
}
