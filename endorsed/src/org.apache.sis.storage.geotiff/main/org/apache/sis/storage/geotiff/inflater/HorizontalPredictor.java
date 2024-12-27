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
package org.apache.sis.storage.geotiff.inflater;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.sis.image.DataType;
import org.apache.sis.pending.jdk.JDK13;
import org.apache.sis.storage.geotiff.base.Predictor;


/**
 * Implementation of {@link Predictor#HORIZONTAL_DIFFERENCING}.
 * Current implementation works only on 8, 16, 32 or 64-bits samples.
 * Values packed on 4, 2 or 1 bits are not yet supported.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class HorizontalPredictor extends PredictorChannel {
    /**
     * Number of bytes minus one in a sample value.
     */
    private final int sampleSizeM1;

    /**
     * Number of <em>bytes</em> between a sample value of a pixel and the same sample value of the next pixel.
     * Contrarily to similar fields in other classes, the value in this class is expressed in <em>bytes</em>
     * rather than a count of sample values because this stride will be applied to {@link ByteBuffer} no matter
     * the data type.
     */
    protected final int pixelStride;

    /**
     * Number of <em>bytes</em> between a column in a row and the same column in the next row.
     * Contrarily to similar fields in other classes, the value in this class is expressed in <em>bytes</em>
     * rather than a count of sample values because this stride will be applied to {@link ByteBuffer} no matter
     * the data type.
     *
     * <p>Invariants:</p>
     * <ul>
     *   <li>This is a multiple of {@link #pixelStride}.</li>
     *   <li>Must be strictly greater than {@link #pixelStride}
     *       (i.e. image width must be at least 2 pixels).</li>
     * </ul>
     */
    private final int scanlineStride;

    /**
     * Column index (as a count of <em>bytes</em>, not a count of sample values or pixels).
     * Used for detecting when the decoding process starts a new row. It shall always be a
     * multiple of the data size (in bytes) and between 0 to {@link #scanlineStride}.
     */
    private int column;

    /**
     * The mask to apply for truncating a position to a multiple of data type size.
     * For example if the data type is unsigned short, then the mask shall truncate
     * positions to a multiple of {@value Short#BYTES}.
     */
    private final int truncationMask;

    /**
     * Creates a new predictor which will read uncompressed data from the given channel.
     * The {@link #setInputRegion(long, long)} method must be invoked after construction
     * before a reading process can start.
     *
     * @param  input            the channel that decompress data.
     * @param  samplesPerPixel  number of sample values per pixel in the source image.
     * @param  width            number of pixels in the source image.
     * @param  sampleSize       number of bytes in a sample value.
     */
    HorizontalPredictor(final CompressionChannel input, final int samplesPerPixel, final int width, final int sampleSize) {
        super(input);
        sampleSizeM1   = sampleSize - Byte.BYTES;
        truncationMask = ~sampleSizeM1;
        pixelStride    = samplesPerPixel * sampleSize;
        scanlineStride = Math.multiplyExact(width, pixelStride);
    }

    /**
     * Creates a new predictor. The {@link #setInputRegion(long, long)} method
     * must be invoked after construction before a reading process can start.
     *
     * @param  input        the channel that decompress data.
     * @param  dataType     primitive type used for storing data elements in the bank.
     * @param  pixelStride  number of sample values per pixel in the source image.
     * @param  width        number of pixels in the source image.
     * @return the predictor, or {@code null} if the given type is unsupported.
     */
    static HorizontalPredictor create(final CompressionChannel input, final DataType dataType,
            final int pixelStride, final int width)
    {
        switch (dataType) {
            case BYTE:   return new Bytes   (input, pixelStride, width);
            case USHORT: // Fall through
            case SHORT:  return new Shorts  (input, pixelStride, width);
            case UINT:   // Fall through
            case INT:    return new Integers(input, pixelStride, width);
            case FLOAT:  return new Floats  (input, pixelStride, width);
            case DOUBLE: return new Doubles (input, pixelStride, width);
            default:     return null;
        }
    }

    /**
     * Prepares this predictor for reading a new tile or a new band of a tile.
     *
     * @param  start      stream position where to start reading.
     * @param  byteCount  number of bytes to read from the input.
     * @throws IOException if the stream cannot be seek to the given start position.
     */
    @Override
    public final void setInputRegion(final long start, final long byteCount) throws IOException {
        super.setInputRegion(start, byteCount);
        column = 0;
    }

    /**
     * Applies the predictor on data in the given buffer,
     * from the given start position until current buffer position.
     *
     * @param  buffer  the buffer on which to apply the predictor.
     * @param  start   position of first byte to process.
     * @return position after the last sample value processed. Should be {@code limit},
     *         unless the predictor needs more data for processing the last bytes.
     */
    @Override
    protected final int apply(final ByteBuffer buffer, final int start) {
        final int limit   = buffer.position();
        final int limitMS = limit - sampleSizeM1;       // Limit with enough space for last sample value.
        /*
         * Pixels in the first column are left unchanged. The column index is not necessarily zero
         * because during the previous invocation of this method, the buffer may have stopped in the
         * middle of the first column (this method does not have control about where the buffer stops).
         */
        int position = start;
        if (column < pixelStride) {
            position += Math.min(pixelStride - column, (limit - position) & truncationMask);
        }
        /*
         * For the first pixel in the buffer, we cannot combine with previous values from the buffer
         * because the buffer does not contain those values anymore.  We have to use the values saved
         * at the end of the last invocation of this method. Note that this will perform no operation
         * if the block above skipped fully the pixel in the first column.
         */
        final int endOfFirstPixel = Math.min(Math.min(start + pixelStride, scanlineStride), limitMS);
        position = applyOnFirst(buffer, position, endOfFirstPixel, position - start);
        if ((column += position - start) >= scanlineStride) {
            column = 0;
        }
        /*
         * This loop body should be executed on a row-by-row basis. But the `startOfRow` and `endOfRow` indices
         * may not be the real start/end of row if the previous call to this method finished before end of row,
         * or if current call to this method also finishes before end of row (because of buffer limit).
         */
        while (position < limitMS) {
            assert (column & ~truncationMask) == 0 : column;
            if (column == 0) {
                // Pixels in the first column are left unchanged.
                column = Math.min(pixelStride, (limit - position) & truncationMask);
                position += column;
            }
            final int startOfRow = position;
            position = applyOnRow(buffer, position, Math.min(position + (scanlineStride - column), limitMS));
            if ((column += position - startOfRow) >= scanlineStride) {
                column = 0;
            }
        }
        /*
         * Save the last bytes for next invocation of this method. There are two cases:
         *
         *   - In the usual case where the above call to `applyOnFirst(…)` used all `savedValues` elements
         *     (this is true when at least `pixelStride` bytes have been used), the `keep` value below will
         *     be zero and the call to `saveLastPixel(…)` will store the last `pixelStride` bytes.
         *
         *   - If the above call to `applyOnFirst(…)` had to stop prematurely before the `limit` position,
         *     the `while` loop is never executed and the `savedValues` array has some residual elements.
         *     The number of residual bytes is given by `keep`.
         */
        int from = position - pixelStride;
        int keep = Math.max(start - from, 0);
        assert (keep & ~truncationMask) == 0 : keep;
        saveLastPixel(buffer, keep, from + keep);
        return position;
    }

    /**
     * Applies the predictor on the specified region of the given buffer, but using {@code savedValues}
     * array as the source of previous values. This is used only for the first pixel in a new invocation
     * of {@link #apply(ByteBuffer, int)}.
     *
     * @param  buffer    the buffer on which to apply the predictor.
     * @param  position  position of the first value to modify in the given buffer.
     * @param  end       position after the last value to process in this {@code apply(…)} call.
     * @param  offset    offset (in bytes) of the first saved value to use.
     * @return value of {@code position} after the last sample values processed by this method.
     */
    abstract int applyOnFirst(ByteBuffer buffer, int position, int end, int offset);

    /**
     * Applies the predictor on the specified region of the given buffer.
     * All integer arguments given to this method are in bytes, with increasing values from left to right.
     * This method shall increment the position by a multiple of data type size (e.g. 2 for short integers).
     *
     * @param  buffer    the buffer on which to apply the predictor.
     * @param  position  position of the first value to modify in the given buffer.
     * @param  end       position after the last value to process in this {@code apply(…)} call.
     * @return value of {@code position} after the last sample values processed by this method.
     */
    abstract int applyOnRow(ByteBuffer buffer, int position, int end);

    /**
     * Saves {@link #pixelStride} bytes making the sample values of the last pixel.
     * The first sample value to read from the buffer is given by {@code position}.
     * In rare occasions, some previously saved values may need to be reused.
     *
     * @param  buffer    buffer from which to save sample values.
     * @param  keep      number of bytes to keep in the currently saved values.
     * @param  position  position in the buffer of the first byte to save, after the values to keep.
     */
    abstract void saveLastPixel(ByteBuffer buffer, int keep, int position);



    /**
     * A horizontal predictor working on byte values.
     */
    private static final class Bytes extends HorizontalPredictor {
        /**
         * The trailing values of previous invocation of {@link #apply(ByteBuffer, int)}.
         * After each call to {@code apply(…)}, the last values in the buffer are saved
         * for use by the next invocation. The buffer capacity is exactly one pixel.
         */
        private final byte[] savedValues;

        /**
         * Creates a new predictor.
         */
        Bytes(final CompressionChannel input, final int samplesPerPixel, final int width) {
            super(input, samplesPerPixel, width, Byte.BYTES);
            savedValues = new byte[samplesPerPixel];
        }

        /**
         * Saves {@link #pixelStride} bytes making the sample values of the last pixel.
         * The first sample value to read from the buffer is given by {@code position}.
         */
        @Override
        void saveLastPixel(final ByteBuffer buffer, int offset, int position) {
            System.arraycopy(savedValues, savedValues.length - offset, savedValues, 0, offset);
            JDK13.get(buffer, position, savedValues, offset, savedValues.length - offset);
        }

        /**
         * Applies the predictor, using {@link #savedValues} as the source of previous values.
         * Used only for the first pixel in a new invocation of {@link #apply(ByteBuffer, int)}.
         */
        @Override
        int applyOnFirst(final ByteBuffer buffer, int position, final int end, int offset) {
            while (position < end) {
                buffer.put(position, (byte) (buffer.get(position) + savedValues[offset++]));
                position++;
            }
            return position;
        }

        /**
         * Applies the predictor on a row of bytes.
         */
        @Override
        int applyOnRow(final ByteBuffer buffer, int position, final int end) {
            while (position < end) {
                buffer.put(position, (byte) (buffer.get(position) + buffer.get(position - pixelStride)));
                position++;
            }
            return position;
        }
    }



    /**
     * A horizontal predictor working on short integer values.
     */
    private static final class Shorts extends HorizontalPredictor {
        /**
         * The trailing values of previous invocation of {@link #apply(ByteBuffer, int)}.
         * After each call to {@code apply(…)}, the last values in the buffer are saved
         * for use by the next invocation. The buffer capacity is exactly one pixel.
         */
        private final short[] savedValues;

        /**
         * Creates a new predictor.
         */
        Shorts(final CompressionChannel input, final int samplesPerPixel, final int width) {
            super(input, samplesPerPixel, width, Short.BYTES);
            savedValues = new short[samplesPerPixel];
        }

        /**
         * Saves {@link #pixelStride} bytes making the sample values of the last pixel.
         * The first sample value to read from the buffer is given by {@code position}.
         */
        @Override
        void saveLastPixel(final ByteBuffer buffer, int offset, int position) {
            assert (offset % Short.BYTES) == 0 : offset;
            offset /= Short.BYTES;
            System.arraycopy(savedValues, savedValues.length - offset, savedValues, 0, offset);
            while (offset < savedValues.length) {
                savedValues[offset++] = buffer.getShort(position);
                position += Short.BYTES;
            }
        }

        /**
         * Applies the predictor, using {@link #savedValues} as the source of previous values.
         * Used only for the first pixel in a new invocation of {@link #apply(ByteBuffer, int)}.
         */
        @Override
        int applyOnFirst(final ByteBuffer buffer, int position, final int end, int offset) {
            assert (offset % Short.BYTES) == 0 : offset;
            offset /= Short.BYTES;
            while (position < end) {
                buffer.putShort(position, (short) (buffer.getShort(position) + savedValues[offset++]));
                position += Short.BYTES;
            }
            return position;
        }

        /**
         * Applies the predictor on a row of short integers.
         */
        @Override
        int applyOnRow(final ByteBuffer buffer, int position, final int end) {
            while (position < end) {
                buffer.putShort(position, (short) (buffer.getShort(position) + buffer.getShort(position - pixelStride)));
                position += Short.BYTES;
            }
            return position;
        }
    }



    /**
     * A horizontal predictor working on 32 bits integer values.
     */
    private static final class Integers extends HorizontalPredictor {
        /**
         * The trailing values of previous invocation of {@link #apply(ByteBuffer, int)}.
         * After each call to {@code apply(…)}, the last values in the buffer are saved
         * for use by the next invocation. The buffer capacity is exactly one pixel.
         */
        private final int[] savedValues;

        /**
         * Creates a new predictor.
         */
        Integers(final CompressionChannel input, final int samplesPerPixel, final int width) {
            super(input, samplesPerPixel, width, Integer.BYTES);
            savedValues = new int[samplesPerPixel];
        }

        /**
         * Saves {@link #pixelStride} bytes making the sample values of the last pixel.
         * The first sample value to read from the buffer is given by {@code position}.
         */
        @Override
        void saveLastPixel(final ByteBuffer buffer, int offset, int position) {
            assert (offset % Integer.BYTES) == 0 : offset;
            offset /= Integer.BYTES;
            System.arraycopy(savedValues, savedValues.length - offset, savedValues, 0, offset);
            while (offset < savedValues.length) {
                savedValues[offset++] = buffer.getInt(position);
                position += Integer.BYTES;
            }
        }

        /**
         * Applies the predictor, using {@link #savedValues} as the source of previous values.
         * Used only for the first pixel in a new invocation of {@link #apply(ByteBuffer, int)}.
         */
        @Override
        int applyOnFirst(final ByteBuffer buffer, int position, final int end, int offset) {
            assert (offset % Integer.BYTES) == 0 : offset;
            offset /= Integer.BYTES;
            while (position < end) {
                buffer.putInt(position, buffer.getInt(position) + savedValues[offset++]);
                position += Integer.BYTES;
            }
            return position;
        }

        /**
         * Applies the predictor on a row of integers.
         */
        @Override
        int applyOnRow(final ByteBuffer buffer, int position, final int end) {
            while (position < end) {
                buffer.putInt(position, buffer.getInt(position) + buffer.getInt(position - pixelStride));
                position += Integer.BYTES;
            }
            return position;
        }
    }



    /**
     * A horizontal predictor working on single-precision floating point values.
     */
    private static final class Floats extends HorizontalPredictor {
        /**
         * The trailing values of previous invocation of {@link #apply(ByteBuffer, int)}.
         * After each call to {@code apply(…)}, the last values in the buffer are saved
         * for use by the next invocation. The buffer capacity is exactly one pixel.
         */
        private final float[] savedValues;

        /**
         * Creates a new predictor.
         */
        Floats(final CompressionChannel input, final int samplesPerPixel, final int width) {
            super(input, samplesPerPixel, width, Float.BYTES);
            savedValues = new float[samplesPerPixel];
        }

        /**
         * Saves {@link #pixelStride} bytes making the sample values of the last pixel.
         * The first sample value to read from the buffer is given by {@code position}.
         */
        @Override
        void saveLastPixel(final ByteBuffer buffer, int offset, int position) {
            assert (offset % Float.BYTES) == 0 : offset;
            offset /= Float.BYTES;
            System.arraycopy(savedValues, savedValues.length - offset, savedValues, 0, offset);
            while (offset < savedValues.length) {
                savedValues[offset++] = buffer.getFloat(position);
                position += Float.BYTES;
            }
        }

        /**
         * Applies the predictor, using {@link #savedValues} as the source of previous values.
         * Used only for the first pixel in a new invocation of {@link #apply(ByteBuffer, int)}.
         */
        @Override
        int applyOnFirst(final ByteBuffer buffer, int position, final int end, int offset) {
            assert (offset % Float.BYTES) == 0 : offset;
            offset /= Float.BYTES;
            while (position < end) {
                buffer.putFloat(position, buffer.getFloat(position) + savedValues[offset++]);
                position += Float.BYTES;
            }
            return position;
        }

        /**
         * Applies the predictor on a row of floating point values.
         */
        @Override
        int applyOnRow(final ByteBuffer buffer, int position, final int end) {
            while (position < end) {
                buffer.putFloat(position, buffer.getFloat(position) + buffer.getFloat(position - pixelStride));
                position += Float.BYTES;
            }
            return position;
        }
    }



    /**
     * A horizontal predictor working on double-precision floating point values.
     */
    private static final class Doubles extends HorizontalPredictor {
        /**
         * The trailing values of previous invocation of {@link #apply(ByteBuffer, int)}.
         * After each call to {@code apply(…)}, the last values in the buffer are saved
         * for use by the next invocation. The buffer capacity is exactly one pixel.
         */
        private final double[] savedValues;

        /**
         * Creates a new predictor.
         */
        Doubles(final CompressionChannel input, final int samplesPerPixel, final int width) {
            super(input, samplesPerPixel, width, Double.BYTES);
            savedValues = new double[samplesPerPixel];
        }

        /**
         * Saves {@link #pixelStride} bytes making the sample values of the last pixel.
         * The first sample value to read from the buffer is given by {@code position}.
         */
        @Override
        void saveLastPixel(final ByteBuffer buffer, int offset, int position) {
            assert (offset % Double.BYTES) == 0 : offset;
            offset /= Double.BYTES;
            System.arraycopy(savedValues, savedValues.length - offset, savedValues, 0, offset);
            while (offset < savedValues.length) {
                savedValues[offset++] = buffer.getDouble(position);
                position += Double.BYTES;
            }
        }

        /**
         * Applies the predictor, using {@link #savedValues} as the source of previous values.
         * Used only for the first pixel in a new invocation of {@link #apply(ByteBuffer, int)}.
         */
        @Override
        int applyOnFirst(final ByteBuffer buffer, int position, final int end, int offset) {
            assert (offset % Double.BYTES) == 0 : offset;
            offset /= Double.BYTES;
            while (position < end) {
                buffer.putDouble(position, buffer.getDouble(position) + savedValues[offset++]);
                position += Double.BYTES;
            }
            return position;
        }

        /**
         * Applies the predictor on a row of floating point values.
         */
        @Override
        int applyOnRow(final ByteBuffer buffer, int position, final int end) {
            while (position < end) {
                buffer.putDouble(position, buffer.getDouble(position) + buffer.getDouble(position - pixelStride));
                position += Double.BYTES;
            }
            return position;
        }
    }
}
