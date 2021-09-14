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

import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.BandedSampleModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import org.opengis.coverage.grid.SequenceType;


/**
 * A pixel iterator reading values directly from a {@link DataBuffer} instead of using {@link Raster} API.
 * This iterator has the same behavior than the default implementation and is provided only for performance reasons.
 * It can bring performance benefits when reading values as {@code float} or {@code double} values, but the benefits
 * are more dubious for {@code int} values because Java2D has optimizations for that specific type.
 *
 * <p>This class assumes a {@link BandedSampleModel} or other models having an equivalently simple mapping from pixel
 * coordinates to indices in banks. For other kinds of sample model, the default implementation should be used.
 * More specifically assumptions are!</p>
 *
 * <ul>
 *   <li>One sample value per band, or (equivalently) only one band.</li>
 *   <li>{@linkplain ComponentSampleModel#getPixelStride() Pixel stride} equals to 1.</li>
 *   <li>{@linkplain ComponentSampleModel#getBankIndices() Bank indices} are the 0, 1, 2, … sequence.</li>
 *   <li>{@linkplain ComponentSampleModel#getBandOffsets() Band offsets} are all zero.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
final class BandedIterator extends WritablePixelIterator {
    /**
     * The buffer from where to read data. This is the buffer backing {@link #currentRaster}.
     * It is the lowest-level object we can use before the plain Java array. We do not fetch
     * the Java array because doing so may cause Java2D to disable GPU accelerations.
     */
    private DataBuffer buffer;

    /**
     * The buffer where to write data, or {@code null} if none.
     * May be the same instance than {@link #buffer}.
     */
    private DataBuffer destBuffer;

    /**
     * The translation from {@link SampleModel} coordinates to {@link Raster} coordinates.
     */
    private int sampleModelTranslateX, sampleModelTranslateY;

    /**
     * The translation from {@link Raster} <var>x</var> coordinates to {@link #buffer} indices.
     * This is constant for a row and needs to be updated only when {@link #y} changed.
     * Given buffer index computed by the following formula:
     *
     * <pre>index = (y - sampleModelTranslateY) * scanlineStride + (x - sampleModelTranslateX)</pre>
     *
     * Then {@code xToBuffer} is above value with <var>x</var> = 0. This value may be negative.
     *
     * @see #changedRowOrTile()
     */
    private int xToBuffer;

    /**
     * Number of {@linkplain #buffer} elements between a given sample and the corresponding sample
     * in the same column of the next row. This value shall be the same for all tiles.
     */
    private final int scanlineStride;

    /**
     * Creates an iterator for the given region in the given raster.
     *
     * @param  input    the raster which contains the sample values to read.
     * @param  output   the raster where to write the sample values, or {@code null} for read-only iterator.
     * @param  subArea  the raster region where to perform the iteration, or {@code null} for iterating over all the raster domain.
     * @param  window   size of the window to use in {@link #createWindow(TransferType)} method, or {@code null} if none.
     * @param  order    {@code null} or {@link SequenceType#LINEAR}. Other values may be added in future versions.
     * @param  scanlineStride  value of {@code getScanlineStride(input.getSampleModel()}. Shall be greater than zero.
     */
    BandedIterator(final Raster input, final WritableRaster output, final Rectangle subArea,
                   final Dimension window, final SequenceType order, final int scanlineStride)
    {
        super(input, output, subArea, window, order);
        this.scanlineStride = scanlineStride;
        acquiredTile(input);
        changedRowOrTile();
    }

    /**
     * Creates an iterator for the given region in the given image.
     *
     * @param  input    the image which contains the sample values to read.
     * @param  output   the image where to write the sample values, or {@code null} for read-only iterator.
     * @param  subArea  the image region where to perform the iteration, or {@code null} for iterating over all the image domain.
     * @param  window   size of the window to use in {@link #createWindow(TransferType)} method, or {@code null} if none.
     * @param  order    {@code null} or {@link SequenceType#LINEAR}. Other values may be added in future versions.
     * @param  scanlineStride  value of {@code getScanlineStride(input.getSampleModel()}. Shall be greater than zero.
     */
    BandedIterator(final RenderedImage input, final WritableRenderedImage output, final Rectangle subArea,
                   final Dimension window, final SequenceType order, final int scanlineStride)
    {
        super(input, output, subArea, window, order);
        this.scanlineStride = scanlineStride;
        // `acquiredTile(…)` will be invoked later by `fetchTile()`.
    }

    /**
     * Recomputes {@link #xToBuffer} for the new {@link #y} value. This method shall be invoked
     * when the iterator moved to a new row, or when the iterator fetched a new tile but only
     * after the (x,y) coordinates have been updated.
     */
    @Override
    final void changedRowOrTile() {
        xToBuffer = (y - sampleModelTranslateY) * scanlineStride - sampleModelTranslateX;
    }

    /**
     * Moves the pixel iterator to the given column (x) and row (y) indices.
     *
     * @throws IndexOutOfBoundsException if the given indices are outside the iteration domain.
     */
    @Override
    public void moveTo(final int px, final int py) {
        if (isSameRowAndTile(px, py)) {
            x = px;
        } else {
            super.moveTo(px, py);
            changedRowOrTile();
        }
    }

    /**
     * Invoked when the iterator fetched a new tile. This method updates {@code BandedIterator} fields
     * with raster properties, except {@link #xToBuffer} which is not updated here because {@link #y}
     * is not yet updated to its new value. {@code BandedIterator} must override all methods invoking
     * {@link #fetchTile()} ane ensure that {@link #changedRowOrTile()} is invoked after (x,y) have
     * been updated.
     */
    @Override
    final void acquiredTile(Raster tile) {
        assert PixelIterator.Builder.getScanlineStride(tile.getSampleModel()) == scanlineStride;
        sampleModelTranslateY = tile.getSampleModelTranslateY();
        sampleModelTranslateX = tile.getSampleModelTranslateX();
        buffer                = tile.getDataBuffer();
        tile = destination();
        if (tile != null) {
            destBuffer = tile.getDataBuffer();
            assert PixelIterator.Builder.getScanlineStride(tile.getSampleModel()) == scanlineStride &&
                   tile.getSampleModelTranslateX() == sampleModelTranslateX &&
                   tile.getSampleModelTranslateY() == sampleModelTranslateY;
        }
    }

    /**
     * Releases the buffer acquired by this iterator, if any.
     * This is safety for avoiding accidental usage of wrong buffer.
     * Also avoid to retain large array if the tile is garbage collected.
     */
    @Override
    final void releaseTile() {
        if (image != null) {
            buffer = null;
            destBuffer = null;
        }
        super.releaseTile();
    }

    /** Returns the sample value in the specified band of current pixel. */
    @Override public int    getSample      (final int band) {return buffer.getElem      (band, x + xToBuffer);}
    @Override public float  getSampleFloat (final int band) {return buffer.getElemFloat (band, x + xToBuffer);}
    @Override public double getSampleDouble(final int band) {return buffer.getElemDouble(band, x + xToBuffer);}
    @Override public void   setSample(int band, int    value)  {destBuffer.setElem      (band, x + xToBuffer, value);}
    @Override public void   setSample(int band, float  value)  {destBuffer.setElemFloat (band, x + xToBuffer, value);}
    @Override public void   setSample(int band, double value)  {destBuffer.setElemDouble(band, x + xToBuffer, value);}

    /**
     * Returns the sample values of current pixel for all bands. If the iterator is not in a valid position
     * as documented in parent class, then this method behavior is undetermined: It may either throw an
     * {@link ArrayIndexOutOfBoundsException} or return a random value.
     */
    @Override
    public int[] getPixel​(int[] dest) {
        if (dest == null) {
            dest = new int[numBands];
        }
        /*
         * `getElement(index)` is synonymous to `getElement(0, index)` but possibly slightly faster
         * since it is implemented with a single array access instead of 3. After that, the loop is
         * not executed at all in the common case of an image with a single band.
         */
        final int index = x + xToBuffer;
        dest[0] = buffer.getElem(index);
        for (int i=1; i<numBands; i++) {
            dest[i] = buffer.getElem(i, index);
        }
        return dest;
    }

    /**
     * Returns the sample values of current pixel for all bands. If the iterator is not in a valid position
     * as documented in parent class, then this method behavior is undetermined: It may either throw an
     * {@link ArrayIndexOutOfBoundsException} or return a random value.
     */
    @Override
    public float[] getPixel​(float[] dest) {
        if (dest == null) {
            dest = new float[numBands];
        }
        final int index = x + xToBuffer;
        dest[0] = buffer.getElemFloat(index);           // See comment in `getPixel(int[])`.
        for (int i=1; i<numBands; i++) {
            dest[i] = buffer.getElemFloat(i, index);
        }
        return dest;
    }

    /**
     * Returns the sample values of current pixel for all bands. If the iterator is not in a valid position
     * as documented in parent class, then this method behavior is undetermined: It may either throw an
     * {@link ArrayIndexOutOfBoundsException} or return a random value.
     */
    @Override
    public double[] getPixel​(double[] dest) {
        if (dest == null) {
            dest = new double[numBands];
        }
        final int index = x + xToBuffer;
        dest[0] = buffer.getElemDouble(index);          // See comment in `getPixel(int[])`.
        for (int i=1; i<numBands; i++) {
            dest[i] = buffer.getElemDouble(i, index);
        }
        return dest;
    }

    /**
     * Sets the sample values of current pixel for all bands. If the iterator is not in a valid position
     * as documented in parent class, then this method behavior is undetermined: It may either throw an
     * {@link ArrayIndexOutOfBoundsException} or return a random value.
     */
    @Override
    public void setPixel​(final int[] values) {
        final int index = x + xToBuffer;
        destBuffer.setElem(index, values[0]);           // See comment in `getPixel(int[])`.
        for (int i=1; i<numBands; i++) {
            destBuffer.setElem(i, index, values[i]);
        }
    }

    /**
     * Sets the sample values of current pixel for all bands. If the iterator is not in a valid position
     * as documented in parent class, then this method behavior is undetermined: It may either throw an
     * {@link ArrayIndexOutOfBoundsException} or return a random value.
     */
    @Override
    public void setPixel​(final float[] values) {
        final int index = x + xToBuffer;
        destBuffer.setElemFloat(index, values[0]);      // See comment in `getPixel(int[])`.
        for (int i=1; i<numBands; i++) {
            destBuffer.setElemFloat(i, index, values[i]);
        }
    }

    /**
     * Sets the sample values of current pixel for all bands. If the iterator is not in a valid position
     * as documented in parent class, then this method behavior is undetermined: It may either throw an
     * {@link ArrayIndexOutOfBoundsException} or return a random value.
     */
    @Override
    public void setPixel​(final double[] values) {
        final int index = x + xToBuffer;
        destBuffer.setElemDouble(index, values[0]);     // See comment in `getPixel(int[])`.
        for (int i=1; i<numBands; i++) {
            destBuffer.setElemDouble(i, index, values[i]);
        }
    }

    /**
     * Creates a window for floating point values using the given arrays.
     */
    @Override Window<FloatBuffer>  createWindow( float[] data,  float[] transfer) {return new  FloatWindow(data, transfer);}
    @Override Window<DoubleBuffer> createWindow(double[] data, double[] transfer) {return new DoubleWindow(data, transfer);}

    /**
     * {@link Window} implementation backed by an array of {@code float[]}.
     * This is a copy of {@link org.apache.sis.image.PixelIterator.FloatWindow}
     * except in {@code getPixels(…)} implementation.
     */
    private final class FloatWindow extends Window<FloatBuffer> {
        /**
         * Sample values in the window ({@code data}) and a temporary array ({@code transfer}).
         * Those arrays are overwritten when {@link #update()} is invoked.
         */
        private final float[] data, transfer;

        /**
         * Creates a new window which will store the sample values in the given {@code data} array.
         */
        FloatWindow(final float[] data, final float[] transfer) {
            super(FloatBuffer.wrap(data).asReadOnlyBuffer());
            this.data = data;
            this.transfer = transfer;
        }

        /**
         * Returns the iterator that created this window.
         */
        @Override
        final PixelIterator owner() {
            return BandedIterator.this;
        }

        /**
         * Performs the transfer between the underlying raster and this window.
         */
        @Override
        Object getPixels(final Raster raster, int subX, int subY, final int subWidth, int subHeight, final int mode) {
            final float[] target = (mode == DIRECT) ? data : transfer;
            if (mode != TRANSFER_FROM_OTHER && subY == y) {
                final DataBuffer source = buffer;
                final int        toNext = scanlineStride - subWidth;
                final int        numBds = numBands;
                int srcOff = subX + xToBuffer;
                int tgtOff = 0;
                do {
                    int c = subWidth;
                    do {
                        target[tgtOff++] = source.getElemFloat(srcOff);
                        for (int b=1; b<numBds; b++) {
                            target[tgtOff++] = source.getElemFloat(b, srcOff);
                        }
                        srcOff++;
                    } while (--c != 0);
                    srcOff += toNext;
                } while (--subHeight != 0);
                return target;
            }
            // Fallback for all cases that we can not handle with above loop.
            return raster.getPixels(subX, subY, subWidth, subHeight, target);
        }

        /**
         * Updates this window with the sample values in the region starting at current iterator position.
         * This method assumes that {@link #next()} or {@link #moveTo(int,int)} has been invoked.
         */
        @Override
        public void update() {
            values.clear();
            fetchValues(this, data);
        }
    }

    /**
     * {@link Window} implementation backed by an array of {@code double[]}.
     * This is a copy of {@link org.apache.sis.image.PixelIterator.DoubleWindow}
     * except in {@code getPixels(…)} implementation.
     */
    private final class DoubleWindow extends Window<DoubleBuffer> {
        /**
         * Sample values in the window ({@code data}) and a temporary array ({@code transfer}).
         * Those arrays are overwritten when {@link #update()} is invoked.
         */
        private final double[] data, transfer;

        /**
         * Creates a new window which will store the sample values in the given {@code data} array.
         */
        DoubleWindow(final double[] data, final double[] transfer) {
            super(DoubleBuffer.wrap(data).asReadOnlyBuffer());
            this.data = data;
            this.transfer = transfer;
        }

        /**
         * Returns the iterator that created this window.
         */
        @Override
        final PixelIterator owner() {
            return BandedIterator.this;
        }

        /**
         * Performs the transfer between the underlying raster and this window.
         */
        @Override
        Object getPixels(final Raster raster, int subX, int subY, final int subWidth, int subHeight, final int mode) {
            final double[] target = (mode == DIRECT) ? data : transfer;
            if (mode != TRANSFER_FROM_OTHER && subY == y) {
                final DataBuffer source = buffer;
                final int        toNext = scanlineStride - subWidth;
                final int        numBds = numBands;
                int srcOff = subX + xToBuffer;
                int tgtOff = 0;
                do {
                    int c = subWidth;
                    do {
                        target[tgtOff++] = source.getElemDouble(srcOff);
                        for (int b=1; b<numBds; b++) {
                            target[tgtOff++] = source.getElemDouble(b, srcOff);
                        }
                        srcOff++;
                    } while (--c != 0);
                    srcOff += toNext;
                } while (--subHeight != 0);
                return target;
            }
            // Fallback for all cases that we can not handle with above loop.
            return raster.getPixels(subX, subY, subWidth, subHeight, target);
        }

        /**
         * Updates this window with the sample values in the region starting at current iterator position.
         * This method assumes that {@link #next()} or {@link #moveTo(int,int)} has been invoked.
         */
        @Override
        public void update() {
            values.clear();
            fetchValues(this, data);
        }
    }
}
