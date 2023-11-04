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
package org.apache.sis.io.stream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import org.apache.sis.util.ArraysExt;


/**
 * Helper methods for writing a rectangular area, a cube or a hyper-cube in a channel.
 * A rectangular area is usually a tile, and consequently should be relatively small.
 * The same instance can be reused when the shape of source arrays and the shape of
 * the region to write do not change, which is typically the case when writing tiles.
 * This class is thread-safe if writing in different output channels.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class HyperRectangleWriter {
    /**
     * Index of the first value to use in the array given to write methods.
     */
    private final int startAt;

    /**
     * Number of elements that can be written in a single I/O operation.
     */
    private final int contiguousDataLength;

    /**
     * Number of elements to write in each dimension after the contiguous dimensions, in reverse order.
     * For an image, it may be an array of length 2 with the height and width of the destination region,
     * in that order. However it may be an array of length 1 with only the height, or an empty array
     * if some chunks of data can be written in a single I/O operation.
     *
     * <p>Values in this array are decremented by 1.</p>
     */
    private final int[] remaining;

    /**
     * Value by which to increment the flat array index for moving to the next value.
     * The array length and the element order is the same as in {@link #remaining}.
     */
    private final int[] strides;

    /**
     * Creates a new writer for data of a shape specified by the given region.
     * The region also specifies the subset to write.
     *
     * @param  output  where to write data.
     * @param  region  size of the source hyper-rectangle and region to write.
     * @throws ArithmeticException if the region is too large.
     */
    public HyperRectangleWriter(final Region region) {
        startAt              = Math.toIntExact(region.startAt);
        int cdd              = region.contiguousDataDimension();
        contiguousDataLength = region.targetLength(cdd);
        final int d          = region.getDimension() - cdd;
        remaining            = new int[d];
        strides              = new int[d];
        for (int i=d; --i>=0; cdd++) {
            if ((remaining[i] = region.getTargetSize(cdd) - 1) < 0 ||
                  (strides[i] = Math.toIntExact(region.getSkip(cdd) + contiguousDataLength)) == 0)
            {
                /*
                 * Should have been verified as of Region constructor contract.
                 * Check again as a safety against never-ending loops.
                 */
                throw new AssertionError(region);
            }
        }
    }

    /**
     * A builder for {@code HyperRectangleWriter} created from a {@code SampleModel}.
     *
     * @author  Martin Desruisseaux (Geomatys)
     */
    public static final class Builder {
        /**
         * Number of elements (not necessarily bytes) contained in this hyper-rectangle.
         * The number of bytes to write will be this length multiplied by element size.
         *
         * @see #length()
         */
        private long length;

        /**
         * Number of elements (not necessarily bytes) between a pixel and the next pixel.
         *
         * @see #pixelStride()
         */
        private int pixelStride;

        /**
         * Number of elements (not necessarily bytes) between a row and the next row.
         *
         * @see #scanlineStride()
         */
        private int scanlineStride;

        /**
         * The indices of all banks to write with {@code HyperRectangleWriter}.
         * A length greater than one means that the {@link HyperRectangleWriter} instance
         * created by this builder will need to be invoked repetitively for each bank.
         *
         * @see bankIndices()
         */
        private int[] bankIndices;

        /**
         * Subregion to write, or {@code null} for writing the whole raster.
         */
        private Rectangle region;

        /**
         * Creates a new builder.
         */
        public Builder() {
        }

        /**
         * Specifies the region to write.
         * This method retains the given rectangle by reference, it is not copied.
         *
         * @param  aoi  the region to write, or {@code null} for writing the whole raster.
         * @return {@code this} for chained call.
         */
        public Builder region(final Rectangle aoi) {
            region = aoi;
            return this;
        }

        /**
         * Creates a new writer for raster data described by the given sample model and strides.
         * If the {@link #region} is non-null, it specifies a subset of the data to write.
         */
        private HyperRectangleWriter create(final SampleModel sm, final int subX) {
            final int[]  subsampling = {subX, 1};
            final long[] sourceSize  = {scanlineStride, sm.getHeight()};
            final long[] regionLower = new long[2];
            final long[] regionUpper = new long[2];
            if (region != null) {
                regionUpper[0] = (regionLower[0] = region.x) + region.width;
                regionUpper[1] = (regionLower[1] = region.y) + region.height;
            } else {
                regionUpper[0] = sm.getWidth();
                regionUpper[1] = sm.getHeight();
            }
            regionLower[0] = Math.multiplyExact(regionLower[0], pixelStride);
            regionUpper[0] = Math.multiplyExact(regionUpper[0], pixelStride);
            var subset = new Region(sourceSize, regionLower, regionUpper, subsampling);
            length = subset.length;
            return new HyperRectangleWriter(subset);
        }

        /**
         * Creates a new writer for raster data described by the given sample model.
         * This method supports only the writing of either a single band, or all bands
         * in the order they appear in the array.
         *
         * <p>The returned writer will need to be applied repetitively for each bank
         * if {@link #bankIndices()} returns an array with a length greater than one.</p>
         *
         * @param  sm  the sample model of the rasters to write.
         * @return writer, or {@code null} if the given sample model is not supported.
         */
        public HyperRectangleWriter create(final ComponentSampleModel sm) {
            pixelStride    = sm.getPixelStride();
            scanlineStride = sm.getScanlineStride();
            bankIndices    = sm.getBankIndices();
            final int[] d  = sm.getBandOffsets();
            final int subX;
            if (ArraysExt.allEquals(bankIndices, bankIndices[0])) {
                /*
                 * PixelInterleavedSampleModel (at least conceptually, no matter the actual type).
                 * The returned `HyperRectangleWriter` instance will write all sample values in a
                 * single call to a `write(…)` method, no matter the actual number of bands.
                 */
                bankIndices = ArraysExt.resize(bankIndices, 1);
                if (d.length == pixelStride && ArraysExt.isRange(0, d)) {
                    subX = 1;
                } else if (d.length == 1) {
                    subX = pixelStride;
                } else {
                    return null;
                }
            } else {
                /*
                 * BandedSampleModel (at least conceptually, no matter the actual type).
                 * The returned `HyperRectangleWriter` instance will need to be used
                 * repetitively by the caller.
                 */
                if (ArraysExt.allEquals(d, 0)) {
                    subX = 1;
                } else {
                    return null;
                }
            }
            return create(sm, subX);
        }

        /**
         * Creates a new writer for raster data described by the given sample model.
         * This method supports only the writing of a single band using all bits.
         *
         * @param  sm  the sample model of the rasters to write.
         * @return writer, or {@code null} if the given sample model is not supported.
         */
        public HyperRectangleWriter create(final SinglePixelPackedSampleModel sm) {
            bankIndices    = new int[1];   // Length is NOT the number of bands.
            pixelStride    = 1;
            scanlineStride = sm.getScanlineStride();
            final int[] d  = sm.getBitMasks();
            if (d.length == 1) {
                final long mask = (1L << DataBuffer.getDataTypeSize(sm.getDataType())) - 1;
                if ((d[0] & mask) == mask) {
                    return create(sm, 1);
                }
            }
            return null;
        }

        /**
         * Creates a new writer for raster data described by the given sample model.
         * This method supports only the writing of a single band using all bits.
         *
         * @param  sm  the sample model of the rasters to write.
         * @return writer, or {@code null} if the given sample model is not supported.
         */
        public HyperRectangleWriter create(final MultiPixelPackedSampleModel sm) {
            bankIndices    = new int[1];   // Length is NOT the number of bands.
            pixelStride    = 1;
            scanlineStride = sm.getScanlineStride();
            final int[] d  = sm.getSampleSize();
            if (d.length == 1) {
                final int size = DataBuffer.getDataTypeSize(sm.getDataType());
                if (d[0] == size && sm.getPixelBitStride() == size) {
                    return create(sm, 1);
                }
            }
            return null;
        }

        /**
         * Creates a new writer for raster data described by the given sample model.
         * The returned writer will need to be applied repetitively for each bank
         * if {@link #bankIndices()} returns an array with a length greater than one.
         *
         * @param  sm  the sample model of the rasters to write.
         * @return writer, or {@code null} if the given sample model is not supported.
         */
        public HyperRectangleWriter create(final SampleModel sm) {
            if (sm instanceof ComponentSampleModel)         return create((ComponentSampleModel)         sm);
            if (sm instanceof SinglePixelPackedSampleModel) return create((SinglePixelPackedSampleModel) sm);
            if (sm instanceof MultiPixelPackedSampleModel)  return create((MultiPixelPackedSampleModel)  sm);
            return null;
        }

        /**
         * {@return the total number of elements contained in the hyper-rectangle}.
         * The number of bytes to write will be this length multiplied by element size.
         * This information is valid only after a {@code create(…)} method has been invoked.
         */
        public long length() {
            return length;
        }

        /**
         * {@return the number of elements (not necessarily bytes) between a pixel and the next pixel}.
         * This information is valid only after a {@code create(…)} method has been invoked.
         */
        public int pixelStride() {
            return pixelStride;
        }

        /**
         * {@return the number of elements (not necessarily bytes) between a row and the next row}.
         * This information is valid only after a {@code create(…)} method has been invoked.
         */
        public int scanlineStride() {
            return scanlineStride;
        }

        /**
         * Returns the indices of all banks to write with {@code HyperRectangleWriter}.
         * This is not necessarily the bank indices of all bands, because the writer may be
         * able to write all bands contiguously in a single call to a {@code write(…)} method.
         * This information is valid only after a {@code create(…)} method has been invoked.
         *
         * @return indices of all banks to write with {@code HyperRectangleWriter}.
         */
        @SuppressWarnings("ReturnOfCollectionOrArrayField")
        public int[] bankIndices() {
            return bankIndices;
        }
    }

    /**
     * Returns the offset of the first element to use in user supplied array.
     *
     * @param  offset  offset supplied by the useR.
     * @return offset to use.
     */
    private int startAt(final int offset) {
        return Math.addExact(startAt, offset);
    }

    /**
     * Returns an array which will be decremented for counting the number of contiguous write operations to apply.
     *
     * @return number of I/O operations to apply.
     */
    private int[] count() {
        return remaining.clone();
    }

    /**
     * Updates the array index to the next value to use after a contiguous write operation.
     *
     * @param  count  array of counters to update in-place.
     * @return next offset, or -1 if the iteration is finished.
     */
    private int next(int offset, final int[] count) {
        for (int i = count.length; --i >= 0;) {
            if (--count[i] >= 0) {
                return offset + strides[i];
            }
            count[i] = remaining[i];
        }
        return -1;
    }

    /**
     * Returns a suggested value for the {@code direct} argument of {@code write(…, byte[], …)}.
     * The suggestion is based on whether the output buffer is direct or not.
     *
     * @param  output  the output which will be given in argument to the {@code write(…)} methods.
     * @return suggested value for the {@code direct} boolean argument.
     */
    public boolean suggestDirect(final ChannelDataOutput output) {
        // Really ! because we want to use the direct buffer if it exists.
        return !output.buffer.isDirect() && output.buffer.capacity() <= contiguousDataLength;
    }

    /**
     * Writes an hyper-rectangle with the shape described at construction time.
     * If the {@code direct} argument is {@code true}, then this method writes
     * directly in the {@link ChannelDataOutput#channel} without copying bytes
     * to the {@link ChannelDataOutput#buffer}.
     *
     * <p>Note that the direct mode is not necessarily faster.
     * If the destination is a NIO channel, Java may perform internally a copy to a direct buffer.
     * If the {@code output} buffer is already {@linkplain ByteBuffer#isDirect() direct}, it may be
     * more efficient to set the {@code direct} argument to {@code false} for using that buffer.</p>
     *
     * @param  output  where to write data.
     * @param  data    data of the hyper-rectangle.
     * @param  offset  offset to add to array index.
     * @throws IOException if an error occurred while writing the data.
     */
    public void write(final ChannelDataOutput output, final byte[] data, int offset, final boolean direct) throws IOException {
        offset = startAt(offset);
        final int[] count = count();
        if (direct) {
            final ByteBuffer buffer = ByteBuffer.wrap(data, offset, contiguousDataLength);
            offset = 0;
            output.flush();
            do {
                buffer.limit(offset + contiguousDataLength).position(offset);
                do {
                    final int n = output.channel.write(buffer);
                    output.moveBufferForward(n);
                    if (n == 0) {
                        output.onEmptyTransfer();
                    }
                } while (buffer.hasRemaining());
            } while ((offset = next(offset, count)) >= 0);
        } else {
            do output.write(data, offset, contiguousDataLength);
            while ((offset = next(offset, count)) >= 0);
        }
    }

    /**
     * Writes an hyper-rectangle with the shape described at construction time.
     *
     * @param  output  where to write data.
     * @param  data    data of the hyper-rectangle.
     * @param  offset  offset to add to array index.
     * @throws IOException if an error occurred while writing the data.
     */
    public void write(final ChannelDataOutput output, final short[] data, int offset) throws IOException {
        offset = startAt(offset);
        final int[] count = count();
        do output.writeShorts(data, offset, contiguousDataLength);
        while ((offset = next(offset, count)) >= 0);
    }

    /**
     * Writes an hyper-rectangle with the shape described at construction time.
     *
     * @param  output  where to write data.
     * @param  data    data of the hyper-rectangle.
     * @param  offset  offset to add to array index.
     * @throws IOException if an error occurred while writing the data.
     */
    public void write(final ChannelDataOutput output, final int[] data, int offset) throws IOException {
        offset = startAt(offset);
        final int[] count = count();
        do output.writeInts(data, offset, contiguousDataLength);
        while ((offset = next(offset, count)) >= 0);
    }

    /**
     * Writes an hyper-rectangle with the shape described at construction time.
     *
     * @param  output  where to write data.
     * @param  data    data of the hyper-rectangle.
     * @param  offset  offset to add to array index.
     * @throws IOException if an error occurred while writing the data.
     */
    public void write(final ChannelDataOutput output, final long[] data, int offset) throws IOException {
        offset = startAt(offset);
        final int[] count = count();
        do output.writeLongs(data, offset, contiguousDataLength);
        while ((offset = next(offset, count)) >= 0);
    }

    /**
     * Writes an hyper-rectangle with the shape described at construction time.
     *
     * @param  output  where to write data.
     * @param  data    data of the hyper-rectangle.
     * @param  offset  offset to add to array index.
     * @throws IOException if an error occurred while writing the data.
     */
    public void write(final ChannelDataOutput output, final float[] data, int offset) throws IOException {
        offset = startAt(offset);
        final int[] count = count();
        do output.writeFloats(data, offset, contiguousDataLength);
        while ((offset = next(offset, count)) >= 0);
    }

    /**
     * Writes an hyper-rectangle with the shape described at construction time.
     *
     * @param  output  where to write data.
     * @param  data    data of the hyper-rectangle.
     * @param  offset  offset to add to array index.
     * @throws IOException if an error occurred while writing the data.
     */
    public void write(final ChannelDataOutput output, final double[] data, int offset) throws IOException {
        offset = startAt(offset);
        final int[] count = count();
        do output.writeDoubles(data, offset, contiguousDataLength);
        while ((offset = next(offset, count)) >= 0);
    }
}
