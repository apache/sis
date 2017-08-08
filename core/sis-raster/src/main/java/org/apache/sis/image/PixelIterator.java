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

import java.nio.Buffer;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.util.NoSuchElementException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.internal.jdk8.JDK8;

import static org.apache.sis.internal.jdk8.JDK8.floorDiv;


/**
 * An iterator over sample values in a raster or an image.  This iterator makes easier to read and write efficiently
 * pixel or sample values. The iterator {@linkplain RenderedImage#getTile(int,int) acquires tiles} and releases them
 * automatically. Unless otherwise specified, iterators are free to use an iteration order
 * that minimize the "acquire / release tile" operations (in other words, iterations are not necessarily from
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
 * @version 0.8
 * @since   0.8
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
     * The lower values are inclusive and the upper values exclusive.
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
     * This may be smaller than the image or raster tile grid bounds, but not greater.
     * The lower values are inclusive and the upper values exclusive.
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
        upperX          = JDK8.addExact(lowerX, bounds.width);
        upperY          = JDK8.addExact(lowerY, bounds.height);
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
        upperX          = JDK8.addExact(lowerX, bounds.width);
        upperY          = JDK8.addExact(lowerY, bounds.height);
        tileLowerX      = floorDiv(JDK8.subtractExact(lowerX, tileGridXOffset), tileWidth);
        tileLowerY      = floorDiv(JDK8.subtractExact(lowerY, tileGridYOffset), tileHeight);
        tileUpperX      =  ceilDiv(JDK8.subtractExact(upperX, tileGridXOffset), tileWidth);
        tileUpperY      =  ceilDiv(JDK8.subtractExact(upperY, tileGridYOffset), tileHeight);
        windowWidth     = (window != null) ? window.width  : 0;
        windowHeight    = (window != null) ? window.height : 0;
    }

    /**
     * Returns {@code numerator / denominator} rounded toward positive infinity.
     */
    private static int ceilDiv(final int numerator, final int denominator) {
        return -floorDiv(-numerator, denominator);
    }

    /**
     * Computes the intersection between the given bounds and and {@code subArea} if {@code subArea} is non-null.
     * If the result is empty, then the width and/or height are set to zero (not negative).
     */
    private static Rectangle intersection(int x, int y, int width, int height, Rectangle subArea, Dimension window) {
        if (window != null) {
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
         * Creates a read-only iterator for the given raster.
         *
         * @param  data  the raster which contains the sample values on which to iterate.
         * @return a new iterator traversing pixels in the given raster.
         */
        public PixelIterator create(final Raster data) {
            ArgumentChecks.ensureNonNull("data", data);
            // TODO: check here for cases that we can optimize (after we ported corresponding implementations).
            return new DefaultIterator(data, null, subArea, window);
        }

        /**
         * Creates a read-only iterator for the given image.
         *
         * @param  data  the image which contains the sample values on which to iterate.
         * @return a new iterator traversing pixels in the given image.
         */
        public PixelIterator create(final RenderedImage data) {
            ArgumentChecks.ensureNonNull("data", data);
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
        public WritablePixelIterator createWritable(final RenderedImage input, final WritableRenderedImage output) {
            ArgumentChecks.ensureNonNull("input",  input);
            ArgumentChecks.ensureNonNull("output", output);
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
     * Returns a moving window over the sample values in a rectangular region starting at iterator position.
     * The <cite>window size</cite> must have been specified at {@code PixelIterator} construction time.
     * Sample values are stored in a sequence of length
     * <var>(number of bands)</var> × <var>(window width)</var> × <var>(window height)</var>.
     * Values are always stored with band index varying fastest, then column index, then row index.
     * Columns are traversed from left to right and rows are traversed from top to bottom
     * (linear iteration order).
     * That order is the same regardless the iteration order of this iterator.
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
     * <div class="note"><b>Usage example:</b>
     * following code creates an iterator over the full area of given image, then a window of 5×5 pixels.
     * The window is moved over all the image area in iteration order. Inside the window, data are copied
     * in linear order regardless the iteration order.
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
     * @version 0.8
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
         * (linear iteration order).
         * That order is the same regardless the iteration order
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
