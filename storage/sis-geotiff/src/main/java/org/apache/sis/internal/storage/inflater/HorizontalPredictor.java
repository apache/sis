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
package org.apache.sis.internal.storage.inflater;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.sis.image.DataType;
import org.apache.sis.internal.jdk9.JDK9;


/**
 * Implementation of {@link org.apache.sis.internal.geotiff.Predictor#HORIZONTAL}.
 * Current implementation works only on 8, 16, 32 or 64-bits samples.
 * Values packed on 4, 2 or 1 bits are not yet supported.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
abstract class HorizontalPredictor extends PredictorChannel {
    /**
     * Number of bytes minus one in a sample value.
     */
    private final int sampleSizeM1;

    /**
     * Number of <em>bytes</em> between a sample value of a pixel and the same sample value of the next pixel.
     * Contrarily to similar fields in other classes, the value in this class is expressed in <em>bytes</em>
     * rather than a count of sample values because this stride we be applied to {@link ByteBuffer} no matter
     * the data type.
     */
    protected final int pixelStride;

    /**
     * Number of <em>bytes</em> between a column in a row and the same column in the next row.
     * Contrarily to similar fields in other classes, the value in this class is expressed in <em>bytes</em>
     * rather than a count of sample values because this stride we be applied to {@link ByteBuffer} no matter
     * the data type.
     */
    private final int scanlineStride;

    /**
     * Column index (as a count of <em>bytes</em>, not a count of sample values or pixels).
     * Used for detecting when the decoding process starts a new row.
     */
    private int column;

    /**
     * Creates a new predictor.
     * The {@link #setInput(long, long)} method must be invoked after construction
     * before a reading process can start.
     *
     * @param  input        the channel that decompress data.
     * @param  pixelStride  number of sample values per pixel in the source image.
     * @param  width        number of pixels in the source image.
     * @param  sampleSize   number of bytes in a sample value.
     */
    HorizontalPredictor(final CompressionChannel input, final int pixelStride, final int width, final int sampleSize) {
        super(input);
        this.sampleSizeM1   = sampleSize - Byte.BYTES;
        this.pixelStride    = pixelStride * sampleSize;
        this.scanlineStride = Math.multiplyExact(width, this.pixelStride);
    }

    /**
     * Creates a new predictor. The {@link #setInput(long, long)} method must
     * be invoked after construction before a reading process can start.
     *
     * @param  input        the channel that decompress data.
     * @param  dataType     primitive type used for storing data elements in the bank.
     * @param  pixelStride  number of sample values per pixel in the source image.
     * @param  width        number of pixels in the source image.
     * @param  sampleSize   number of bytes in a sample value.
     * @return the predictor, or {@code null} if the given type is unsupported.
     */
    static HorizontalPredictor create(final CompressionChannel input, final DataType dataType,
            final int pixelStride, final int width)
    {
        switch (dataType) {
            case USHORT:
            case SHORT:  return new Shorts  (input, pixelStride, width);
            case BYTE:   return new Bytes   (input, pixelStride, width);
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
     * @param  byteCount  number of byte to read from the input.
     * @throws IOException if the stream can not be seek to the given start position.
     */
    @Override
    public void setInput(final long start, final long byteCount) throws IOException {
        super.setInput(start, byteCount);
        column = 0;
    }

    /**
     * Applies the predictor on data in the given buffer,
     * from the given start position until current buffer position.
     *
     * @param  buffer  the buffer on which to apply the predictor.
     * @param  start   position of first byte to process.
     * @return position after the same sample value processed. Should be {@code limit},
     *         unless the predictor needs more data for processing the last bytes.
     */
    @Override
    protected int uncompress(final ByteBuffer buffer, final int start) {
        final int limit = buffer.position() - sampleSizeM1;
        int position = start;
        while (position < limit) {
            /*
             * This loop body should be executed on a row-by-row basis. But the `startOfRow` and `endOfRow` indices
             * may not be the real start/end of row if the previous call to this method finished before end of row,
             * or if current call to this method also finishes before end of row (because of buffer limit).
             */
            final int startOfRow    = position;
            final int endOfRow      = Math.min(position + (scanlineStride - column), limit);
            final int endOfDeferred = Math.min(position + pixelStride, endOfRow);
            if (column < pixelStride) {
                // Pixels in the first column are left unchanged.
                position += Math.min(pixelStride - column, endOfRow - position);
            }
            position = applyOnRow(buffer, startOfRow, position, endOfDeferred, endOfRow);
            column += position - startOfRow;
            if (column >= scanlineStride) {
                column = 0;
            }
        }
        /*
         * Save the last bytes for next invocation of this method.
         */
        final int capacity = position - start;
        if (capacity >= pixelStride) {
            saveLastPixel(buffer, position - pixelStride);
        } else {
            saveLastPixel(buffer, pixelStride - capacity, start, capacity);
        }
        return position;
    }

    /**
     * Applies the predictor on the specified region of the given buffer.
     * The region to process is divided in two parts:
     *
     * <ul>
     *   <li>From {@code position} to {@code deferred}: values that need to be added with values
     *       from a previous invocation of {@link #uncompress(ByteBuffer, int)}.</li>
     *   <li>From {@code deferred} to {@code endOfRow}: values to be added with values available
     *       at a previous position in the current buffer.</li>
     * </ul>
     *
     * All integer arguments given to this method are in bytes, with increasing values from left to right.
     *
     * @param  buffer         the buffer on which to apply the predictor.
     * @param  startOfRow     position of the start of the (possibly truncated) row.
     * @param  position       position of the first value to modify.
     * @param  endOfDeferred  position after the last value combined with values saved from previous batch.
     * @param  endOfRow       position after the last value to process in this {@code apply(…)} call.
     * @return value of {@code position} after the last sample values processed by this method.
     *         Should be equal to {@code endOfRow}, unless this method needs more byte for processing
     *         the last sample value.
     */
    abstract int applyOnRow(ByteBuffer buffer, int startOfRow, int position, int endOfDeferred, int endOfRow);

    /**
     * Saves the sample values of the last pixel, starting from given buffer position.
     * Those values will be needed for processing the first pixel in the next invocation
     * of {@link #uncompress(ByteBuffer, int)}.
     *
     * @param  buffer    buffer from which to save sample values.
     * @param  position  position in the buffer of the first byte to save.
     */
    abstract void saveLastPixel(ByteBuffer buffer, int position);

    /**
     * Saves some sample values of the last pixel, starting from given position.
     * This method is invoked when there is not enough space in the buffer for saving a complete pixel.
     *
     * @param  buffer    buffer from which to save sample values.
     * @param  keep      number of bytes to keep in the currently saved values.
     * @param  position  position in the buffer of the first byte to save.
     * @param  length    number of bytes to save.
     */
    abstract void saveLastPixel(ByteBuffer buffer, int keep, int position, int length);



    /**
     * A horizontal predictor working on byte values.
     */
    private static final class Bytes extends HorizontalPredictor {
        /**
         * Data in the previous column. The length of this array is the pixel stride.
         */
        private final byte[] previousColumns;

        /**
         * Creates a new predictor.
         */
        Bytes(final CompressionChannel input, final int pixelStride, final int width) {
            super(input, pixelStride, width, Byte.BYTES);
            previousColumns = new byte[pixelStride];
        }

        /**
         * Applies the predictor on a row of bytes.
         */
        @Override
        int applyOnRow(final ByteBuffer buffer, final int startOfRow, int position, final int endOfDeferred, final int endOfRow) {
            while (position < endOfDeferred) {
                buffer.put(position, (byte) (buffer.get(position) + previousColumns[position - startOfRow]));
                position++;
            }
            while (position < endOfRow) {
                buffer.put(position, (byte) (buffer.get(position) + buffer.get(position - pixelStride)));
                position++;
            }
            return position;
        }

        /**
         * Saves the sample values of the last pixel, starting from given buffer position.
         * Needed for processing the first pixel in next {@code uncompress(…)} invocation.
         */
        @Override
        void saveLastPixel(final ByteBuffer buffer, final int position) {
            JDK9.get(buffer, position, previousColumns);
        }

        /**
         * Saves some sample values of the last pixel, starting from given buffer position.
         * Invoked when there is not enough space in the buffer for saving a complete pixel.
         */
        @Override
        void saveLastPixel(final ByteBuffer buffer, final int keep, final int position, final int length) {
            System.arraycopy(previousColumns, keep, previousColumns, 0, length);
            JDK9.get(buffer, position, previousColumns, keep, length);
        }
    }



    /**
     * A horizontal predictor working on short integer values.
     */
    private static final class Shorts extends HorizontalPredictor {
        /**
         * Data in the previous column. The length of this array is the pixel stride.
         */
        private final short[] previousColumns;

        /**
         * Creates a new predictor.
         */
        Shorts(final CompressionChannel input, final int pixelStride, final int width) {
            super(input, pixelStride, width, Short.BYTES);
            previousColumns = new short[pixelStride];
        }

        /**
         * Applies the predictor on a row of short integers.
         */
        @Override
        int applyOnRow(final ByteBuffer buffer, final int startOfRow, int position, final int endOfDeferred, final int endOfRow) {
            while (position < endOfDeferred) {
                buffer.putShort(position, (short) (buffer.getShort(position) + previousColumns[position - startOfRow]));
                position += Short.BYTES;
            }
            while (position < endOfRow) {
                buffer.putShort(position, (short) (buffer.getShort(position) + buffer.getShort(position - pixelStride)));
                position += Short.BYTES;
            }
            return position;
        }

        /**
         * Saves the sample values of the last pixel, starting from given buffer position.
         * Needed for processing the first pixel in next {@code uncompress(…)} invocation.
         */
        @Override
        void saveLastPixel(final ByteBuffer buffer, int position) {
            for (int i=0; i<previousColumns.length; i++) {
                previousColumns[i] = buffer.getShort(position);
                position += Short.BYTES;
            }
        }

        /**
         * Saves some sample values of the last pixel, starting from given buffer position.
         * Invoked when there is not enough space in the buffer for saving a complete pixel.
         */
        @Override
        void saveLastPixel(final ByteBuffer buffer, int keep, int position, int length) {
            keep   /= Short.BYTES;
            length /= Short.BYTES;
            System.arraycopy(previousColumns, keep, previousColumns, 0, length);
            length += keep;
            while (keep < length) {
                previousColumns[keep++] = buffer.get(position);
                position += Short.BYTES;
            }
        }
    }



    /**
     * A horizontal predictor working on 32 bits integer values.
     */
    private static final class Integers extends HorizontalPredictor {
        /**
         * Data in the previous column. The length of this array is the pixel stride.
         */
        private final int[] previousColumns;

        /**
         * Creates a new predictor.
         */
        Integers(final CompressionChannel input, final int pixelStride, final int width) {
            super(input, pixelStride, width, Integer.BYTES);
            previousColumns = new int[pixelStride];
        }

        /**
         * Applies the predictor on a row of integers.
         */
        @Override
        int applyOnRow(final ByteBuffer buffer, final int startOfRow, int position, final int endOfDeferred, final int endOfRow) {
            while (position < endOfDeferred) {
                buffer.putInt(position, buffer.getInt(position) + previousColumns[position - startOfRow]);
                position += Integer.BYTES;
            }
            while (position < endOfRow) {
                buffer.putInt(position, buffer.getInt(position) + buffer.getInt(position - pixelStride));
                position += Integer.BYTES;
            }
            return position;
        }

        /**
         * Saves the sample values of the last pixel, starting from given buffer position.
         * Needed for processing the first pixel in next {@code uncompress(…)} invocation.
         */
        @Override
        void saveLastPixel(final ByteBuffer buffer, int position) {
            for (int i=0; i<previousColumns.length; i++) {
                previousColumns[i] = buffer.getInt(position);
                position += Integer.BYTES;
            }
        }

        /**
         * Saves some sample values of the last pixel, starting from given buffer position.
         * Invoked when there is not enough space in the buffer for saving a complete pixel.
         */
        @Override
        void saveLastPixel(final ByteBuffer buffer, int keep, int position, int length) {
            keep   /= Integer.BYTES;
            length /= Integer.BYTES;
            System.arraycopy(previousColumns, keep, previousColumns, 0, length);
            length += keep;
            while (keep < length) {
                previousColumns[keep++] = buffer.get(position);
                position += Integer.BYTES;
            }
        }
    }



    /**
     * A horizontal predictor working on single-precision floating point values.
     */
    private static final class Floats extends HorizontalPredictor {
        /**
         * Data in the previous column. The length of this array is the pixel stride.
         */
        private final float[] previousColumns;

        /**
         * Creates a new predictor.
         */
        Floats(final CompressionChannel input, final int pixelStride, final int width) {
            super(input, pixelStride, width, Float.BYTES);
            previousColumns = new float[pixelStride];
        }

        /**
         * Applies the predictor on a row of floating point values.
         */
        @Override
        int applyOnRow(final ByteBuffer buffer, final int startOfRow, int position, final int endOfDeferred, final int endOfRow) {
            while (position < endOfDeferred) {
                buffer.putFloat(position, buffer.getFloat(position) + previousColumns[position - startOfRow]);
                position += Float.BYTES;
            }
            while (position < endOfRow) {
                buffer.putFloat(position, buffer.getFloat(position) + buffer.getFloat(position - pixelStride));
                position += Float.BYTES;
            }
            return position;
        }

        /**
         * Saves the sample values of the last pixel, starting from given buffer position.
         * Needed for processing the first pixel in next {@code uncompress(…)} invocation.
         */
        @Override
        void saveLastPixel(final ByteBuffer buffer, int position) {
            for (int i=0; i<previousColumns.length; i++) {
                previousColumns[i] = buffer.getFloat(position);
                position += Float.BYTES;
            }
        }

        /**
         * Saves some sample values of the last pixel, starting from given buffer position.
         * Invoked when there is not enough space in the buffer for saving a complete pixel.
         */
        @Override
        void saveLastPixel(final ByteBuffer buffer, int keep, int position, int length) {
            keep   /= Float.BYTES;
            length /= Float.BYTES;
            System.arraycopy(previousColumns, keep, previousColumns, 0, length);
            length += keep;
            while (keep < length) {
                previousColumns[keep++] = buffer.get(position);
                position += Float.BYTES;
            }
        }
    }



    /**
     * A horizontal predictor working on double-precision floating point values.
     */
    private static final class Doubles extends HorizontalPredictor {
        /**
         * Data in the previous column. The length of this array is the pixel stride.
         */
        private final double[] previousColumns;

        /**
         * Creates a new predictor.
         */
        Doubles(final CompressionChannel input, final int pixelStride, final int width) {
            super(input, pixelStride, width, Double.BYTES);
            previousColumns = new double[pixelStride];
        }

        /**
         * Applies the predictor on a row of floating point values.
         */
        @Override
        int applyOnRow(final ByteBuffer buffer, final int startOfRow, int position, final int endOfDeferred, final int endOfRow) {
            while (position < endOfDeferred) {
                buffer.putDouble(position, buffer.getDouble(position) + previousColumns[position - startOfRow]);
                position += Double.BYTES;
            }
            while (position < endOfRow) {
                buffer.putDouble(position, buffer.getDouble(position) + buffer.getDouble(position - pixelStride));
                position += Double.BYTES;
            }
            return position;
        }

        /**
         * Saves the sample values of the last pixel, starting from given buffer position.
         * Needed for processing the first pixel in next {@code uncompress(…)} invocation.
         */
        @Override
        void saveLastPixel(final ByteBuffer buffer, int position) {
            for (int i=0; i<previousColumns.length; i++) {
                previousColumns[i] = buffer.getDouble(position);
                position += Double.BYTES;
            }
        }

        /**
         * Saves some sample values of the last pixel, starting from given buffer position.
         * Invoked when there is not enough space in the buffer for saving a complete pixel.
         */
        @Override
        void saveLastPixel(final ByteBuffer buffer, int keep, int position, int length) {
            keep   /= Double.BYTES;
            length /= Double.BYTES;
            System.arraycopy(previousColumns, keep, previousColumns, 0, length);
            length += keep;
            while (keep < length) {
                previousColumns[keep++] = buffer.get(position);
                position += Double.BYTES;
            }
        }
    }
}
