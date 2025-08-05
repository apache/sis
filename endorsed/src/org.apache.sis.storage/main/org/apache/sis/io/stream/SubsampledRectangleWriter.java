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


/**
 * Helper methods for writing a rectangular area with subsampling applied on-the-fly.
 * This class is thread-safe if writing in different output channels.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class SubsampledRectangleWriter extends HyperRectangleWriter {
    /**
     * The indices of the sample value to take in each pixel.
     */
    private final int[] bandOffsets;

    /**
     * Number of sample values (usually bands) between a pixel and the next pixel in the source arrays.
     * By comparison, {@code super.strides[0]} is the scanline stride.
     */
    private final int pixelStride;

    /**
     * Creates a new writer for data of a shape specified by the given region.
     * The region also specifies the subset to write.
     *
     * @param  output       where to write data.
     * @param  region       size of the source hyper-rectangle and region to write.
     * @param  bandOffsets  indices of bands to write. This array is not cloned.
     * @param  pixelStride  number of bands in a pixel.
     * @throws ArithmeticException if the region is too large.
     */
    SubsampledRectangleWriter(final Region region, final int[] bandOffsets, final int pixelStride) {
        super(region);
        this.bandOffsets = bandOffsets;
        this.pixelStride = pixelStride;
    }

    /**
     * Returns {@code false} since direct mode is never supported when sub-sampling is applied.
     */
    @Override
    public boolean suggestDirect(final ChannelDataOutput output) {
        return false;
    }

    /**
     * Writes an hyper-rectangle with the shape and subsampling described at construction time.
     *
     * @param  output      where to write data.
     * @param  offset      index of the first data element to write.
     * @param  sampleSize  number of bytes in a sample value.
     * @param  data        wrapper over the data of the hyper-rectangle to write.
     * @throws IOException if an error occurred while writing the data.
     */
    private void write(final ChannelDataOutput output, int offset, final int sampleSize, final Data data) throws IOException {
        final ByteBuffer target = output.buffer;
        final int numBands  = bandOffsets.length;
        final int pixelSize = numBands * sampleSize;        // Pixel stride in target buffer and in bytes.
        final int[] count = count();
        offset = startAt(offset);
        do {
            int index = offset;
            final int end = index + contiguousDataLength;
            do {
                output.ensureBufferAccepts(pixelSize);      // At least one pixel, but will usually free more space.
                final int numPixels = Math.min((end - index) / pixelStride,
                        (target.capacity() - target.position()) / pixelSize);
                target.limit(target.position() + numPixels * pixelSize);
                if (numBands == 1) {
                    index = data.fill(target, index + bandOffsets[0], pixelStride);
                } else {
                    index = data.fill(target, index, bandOffsets, pixelStride);
                }
            } while (index < end);
        } while ((offset = next(offset, count)) >= 0);
    }

    /**
     * A wrapper of an array of arbitrary primitive type to be sub-sampled in a {@link ByteBuffer}.
     * An instance is created for each array to write. The subclass depends on the primitive type.
     */
    private static abstract class Data {
        /**
         * Creates a new adapter.
         */
        Data() {
        }

        /**
         * Fills the given buffer with pixels of one sample value each.
         * Caller must ensure that the remaining space in the buffer is an integer number of pixels.
         *
         * @param  target  the buffer to fill.
         * @param  index   index of the first array element to put in the target buffer.
         * @param  stride  value to add to the index for moving to the next pixel in the source array.
         * @return value of {@code index} after the buffer has been filled.
         */
        abstract int fill(final ByteBuffer target, int index, int stride);

        /**
         * Fills the given buffer with pixels of made of multiple sample values each.
         * Caller must ensure that the remaining space in the buffer is an integer number of pixels.
         *
         * @param  target  the buffer to fill.
         * @param  index   index of the first pixel to put in the target buffer.
         * @param  bands   indices of the bands to put in the buffer, in order.
         * @param  stride  value to add to the index for moving to the next pixel in the source array.
         * @return value of {@code index} after the buffer has been filled.
         */
        abstract int fill(final ByteBuffer target, int index, int[] bands, int stride);
    }

    /**
     * Writes an hyper-rectangle with the shape and subsampling described at construction time.
     *
     * @param  output  where to write data.
     * @param  data    data of the hyper-rectangle.
     * @param  offset  index of the first data element to write.
     * @param  direct  Must be {@code false}. The transfer will never be direct.
     * @throws IOException if an error occurred while writing the data.
     */
    @Override
    public void write(final ChannelDataOutput output, final byte[] data, int offset, final boolean direct) throws IOException {
        if (direct) throw new UnsupportedOperationException();
        write(output, offset, Byte.BYTES, new Data() {
            /** Fills the buffer with pixels made of a single sample value. */
            @Override int fill(final ByteBuffer target, int index, final int stride) {
                while (target.hasRemaining()) {
                    target.put(data[index]);
                    index += stride;
                }
                return index;
            }

            /** Fills the buffer with pixels made of multiple sample values. */
            @Override int fill(final ByteBuffer target, int index, final int[] bands, final int stride) {
                while (target.hasRemaining()) {
                    for (int b : bands) {
                        target.put(data[index + b]);
                    }
                    index += stride;
                }
                return index;
            }
        });
    }

    /**
     * Writes an hyper-rectangle with the shape and subsampling described at construction time.
     *
     * @param  output  where to write data.
     * @param  data    data of the hyper-rectangle.
     * @param  offset  index of the first data element to write.
     * @throws IOException if an error occurred while writing the data.
     */
    @Override
    public void write(final ChannelDataOutput output, final short[] data, int offset) throws IOException {
        write(output, offset, Short.BYTES, new Data() {
            /** Fills the buffer with pixels made of a single sample value. */
            @Override int fill(final ByteBuffer target, int index, final int stride) {
                while (target.hasRemaining()) {
                    target.putShort(data[index]);
                    index += stride;
                }
                return index;
            }

            /** Fills the buffer with pixels made of multiple sample values. */
            @Override int fill(final ByteBuffer target, int index, final int[] bands, final int stride) {
                while (target.hasRemaining()) {
                    for (int b : bands) {
                        target.putShort(data[index + b]);
                    }
                    index += stride;
                }
                return index;
            }
        });
    }

    /**
     * Writes an hyper-rectangle with the shape and subsampling described at construction time.
     *
     * @param  output  where to write data.
     * @param  data    data of the hyper-rectangle.
     * @param  offset  index of the first data element to write.
     * @throws IOException if an error occurred while writing the data.
     */
    @Override
    public void write(final ChannelDataOutput output, final int[] data, int offset) throws IOException {
        write(output, offset, Integer.BYTES, new Data() {
            /** Fills the buffer with pixels made of a single sample value. */
            @Override int fill(final ByteBuffer target, int index, final int stride) {
                while (target.hasRemaining()) {
                    target.putInt(data[index]);
                    index += stride;
                }
                return index;
            }

            /** Fills the buffer with pixels made of multiple sample values. */
            @Override int fill(final ByteBuffer target, int index, final int[] bands, final int stride) {
                while (target.hasRemaining()) {
                    for (int b : bands) {
                        target.putInt(data[index + b]);
                    }
                    index += stride;
                }
                return index;
            }
        });
    }

    /**
     * Writes an hyper-rectangle with the shape and subsampling described at construction time.
     *
     * @param  output  where to write data.
     * @param  data    data of the hyper-rectangle.
     * @param  offset  index of the first data element to write.
     * @throws IOException if an error occurred while writing the data.
     */
    @Override
    public void write(final ChannelDataOutput output, final long[] data, int offset) throws IOException {
        write(output, offset, Long.BYTES, new Data() {
            /** Fills the buffer with pixels made of a single sample value. */
            @Override int fill(final ByteBuffer target, int index, final int stride) {
                while (target.hasRemaining()) {
                    target.putLong(data[index]);
                    index += stride;
                }
                return index;
            }

            /** Fills the buffer with pixels made of multiple sample values. */
            @Override int fill(final ByteBuffer target, int index, final int[] bands, final int stride) {
                while (target.hasRemaining()) {
                    for (int b : bands) {
                        target.putLong(data[index + b]);
                    }
                    index += stride;
                }
                return index;
            }
        });
    }

    /**
     * Writes an hyper-rectangle with the shape and subsampling described at construction time.
     *
     * @param  output  where to write data.
     * @param  data    data of the hyper-rectangle.
     * @param  offset  index of the first data element to write.
     * @throws IOException if an error occurred while writing the data.
     */
    @Override
    public void write(final ChannelDataOutput output, final float[] data, int offset) throws IOException {
        write(output, offset, Float.BYTES, new Data() {
            /** Fills the buffer with pixels made of a single sample value. */
            @Override int fill(final ByteBuffer target, int index, final int stride) {
                while (target.hasRemaining()) {
                    target.putFloat(data[index]);
                    index += stride;
                }
                return index;
            }

            /** Fills the buffer with pixels made of multiple sample values. */
            @Override int fill(final ByteBuffer target, int index, final int[] bands, final int stride) {
                while (target.hasRemaining()) {
                    for (int b : bands) {
                        target.putFloat(data[index + b]);
                    }
                    index += stride;
                }
                return index;
            }
        });
    }

    /**
     * Writes an hyper-rectangle with the shape and subsampling described at construction time.
     *
     * @param  output  where to write data.
     * @param  data    data of the hyper-rectangle.
     * @param  offset  index of the first data element to write.
     * @throws IOException if an error occurred while writing the data.
     */
    @Override
    public void write(final ChannelDataOutput output, final double[] data, int offset) throws IOException {
        write(output, offset, Double.BYTES, new Data() {
            /** Fills the buffer with pixels made of a single sample value. */
            @Override int fill(final ByteBuffer target, int index, final int stride) {
                while (target.hasRemaining()) {
                    target.putDouble(data[index]);
                    index += stride;
                }
                return index;
            }

            /** Fills the buffer with pixels made of multiple sample values. */
            @Override int fill(final ByteBuffer target, int index, final int[] bands, final int stride) {
                while (target.hasRemaining()) {
                    for (int b : bands) {
                        target.putDouble(data[index + b]);
                    }
                    index += stride;
                }
                return index;
            }
        });
    }
}
