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
package org.apache.sis.storage.geotiff.writer;

import java.util.Arrays;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;
import org.apache.sis.image.DataType;
import org.apache.sis.io.stream.ChannelDataOutput;
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
     * Number of elements (not necessarily bytes) between a row and the next row.
     * This is usually the tile scanlineStride.
     */
    protected final int scanlineStride;

    /**
     * The column index of the next sample values to write.
     * This is reset to 0 for each new row, and increased by 1 for each sample value.
     */
    private int column;

    /**
     * Creates a new predictor which will write uncompressed data to the given channel.
     *
     * @param  output          the channel that compress data.
     * @param  scanlineStride  number of elements (not necessarily bytes) between a row and the next row.
     */
    HorizontalPredictor(final PixelChannel output, final int scanlineStride) {
        super(output);
        this.scanlineStride = scanlineStride;
    }

    /**
     * Creates a new predictor.
     *
     * @param  output          the channel that decompress data.
     * @param  dataType        primitive type used for storing data elements in the bank.
     * @param  pixelStride     number of elements (not necessarily bytes) between a pixel and the next pixel.
     * @param  scanlineStride  number of elements (not necessarily bytes) between a row and the next row.
     * @return the predictor, or {@code null} if the given type is unsupported.
     */
    static HorizontalPredictor create(final PixelChannel output, final DataType dataType,
            final int pixelStride, final int scanlineStride)
    {
        switch (dataType) {
            case BYTE:   return new Bytes   (output, pixelStride, scanlineStride);
            case USHORT: // Fall through
            case SHORT:  return new Shorts  (output, pixelStride, scanlineStride);
            case UINT:   // Fall through
            case INT:    return new Integers(output, pixelStride, scanlineStride);
            case FLOAT:  return new Floats  (output, pixelStride, scanlineStride);
            case DOUBLE: return new Doubles (output, pixelStride, scanlineStride);
            default:     return null;
        }
    }

    /**
     * {@return the size of sample values in number of bytes}.
     */
    abstract int sampleSize();

    /**
     * Applies the predictor on data in the given buffer,
     * from the buffer position until the buffer limit.
     * This method modifies in-place the content of the given buffer.
     * That buffer should contain only temporary data, typically copied from a raster data buffer.
     *
     * @param  buffer  the buffer on which to apply the predictor. Content will be modified in-place.
     * @return number of bytes written.
     * @throws IOException if an error occurred while writing the data to the channel.
     */
    @Override
    public final int write(final ByteBuffer buffer) throws IOException {
        final int start = buffer.position();
        final int count = apply(buffer, column);
        column = (column + count) % scanlineStride;
        final int limit = buffer.limit();
        buffer.limit(buffer.position() + count * sampleSize());
        while (buffer.hasRemaining()) {
            output.write(buffer);
        }
        buffer.limit(limit);
        return buffer.position() - start;
    }

    /**
     * Applies the differential predictor on the given buffer, from current position to limit.
     * Implementation shall not modify the buffer position or limit.
     *
     * @param  buffer  the buffer on which to apply the predictor.
     * @param  start   index of the column of the first value in the buffer.
     */
    abstract int apply(ByteBuffer output, int start);


    /**
     * A horizontal predictor working on byte values.
     */
    private static final class Bytes extends HorizontalPredictor {
        /** Sample values of the previous pixel. */
        private final byte[] previous;

        /** Creates a new predictor. */
        Bytes(final PixelChannel output, final int pixelStride, final int scanlineStride) {
            super(output, scanlineStride);
            previous = new byte[pixelStride];
        }

        /** The number of bytes in each sample value. */
        @Override int sampleSize() {
            return Byte.BYTES;
        }

        /** Applies the differential predictor. */
        @Override int apply(final ByteBuffer buffer, final int start) {
            final ByteBuffer view = buffer.slice();
            final int pixelStride = previous.length;
            final int bankShift   = start % pixelStride;
            for (int bank=0; bank < pixelStride; bank++) {
                final int pi = (bank + bankShift) % pixelStride;
                byte p = previous[pi];
                int endOfRow = scanlineStride - start;
                for (int i=bank;;) {
                    final int endOfPass = Math.min(endOfRow, view.limit());
                    while (i < endOfPass) {
                        final byte v = view.get(i);
                        view.put(i, (byte) (v - p));
                        p = v;
                        i += pixelStride;
                    }
                    if (i < endOfRow) break;
                    endOfRow += scanlineStride;
                    p = 0;
                }
                previous[pi] = p;
            }
            return view.limit();
        }

        /** Writes pending data and resets the predictor for the next tile to write. */
        @Override public void finish(final ChannelDataOutput owner) throws IOException {
            super.finish(owner);
            Arrays.fill(previous, (byte) 0);
        }
    }



    /**
     * A horizontal predictor working on short integer values.
     * The code of this class is a copy of {@link Bytes} adapted for short integers.
     */
    private static final class Shorts extends HorizontalPredictor {
        /** Sample values of the previous pixel. */
        private final short[] previous;

        /** Creates a new predictor. */
        Shorts(final PixelChannel output, final int pixelStride, final int scanlineStride) {
            super(output, scanlineStride);
            previous = new short[pixelStride];
        }

        /** The number of bytes in each sample value. */
        @Override int sampleSize() {
            return Short.BYTES;
        }

        /** Applies the differential predictor. */
        @Override int apply(final ByteBuffer buffer, final int start) {
            final ShortBuffer view = buffer.asShortBuffer();
            final int pixelStride = previous.length;
            final int bankShift   = start % pixelStride;
            for (int bank=0; bank < pixelStride; bank++) {
                final int pi = (bank + bankShift) % pixelStride;
                short p = previous[pi];
                int endOfRow = scanlineStride - start;
                for (int i=bank;;) {
                    final int endOfPass = Math.min(endOfRow, view.limit());
                    while (i < endOfPass) {
                        final short v = view.get(i);
                        view.put(i, (short) (v - p));
                        p = v;
                        i += pixelStride;
                    }
                    if (i < endOfRow) break;
                    endOfRow += scanlineStride;
                    p = 0;
                }
                previous[pi] = p;
            }
            return view.limit();
        }

        /** Writes pending data and resets the predictor for the next tile to write. */
        @Override public void finish(final ChannelDataOutput owner) throws IOException {
            super.finish(owner);
            Arrays.fill(previous, (short) 0);
        }
    }



    /**
     * A horizontal predictor working on 32 bits integer values.
     * The code of this class is a copy of {@link Bytes} adapted for integers.
     */
    private static final class Integers extends HorizontalPredictor {
        /** Sample values of the previous pixel. */
        private final int[] previous;

        /** Creates a new predictor. */
        Integers(final PixelChannel output, final int pixelStride, final int scanlineStride) {
            super(output, scanlineStride);
            previous = new int[pixelStride];
        }

        /** The number of bytes in each sample value. */
        @Override int sampleSize() {
            return Integer.BYTES;
        }

        /** Applies the differential predictor. */
        @Override int apply(final ByteBuffer buffer, final int start) {
            final IntBuffer view = buffer.asIntBuffer();
            final int pixelStride = previous.length;
            final int bankShift   = start % pixelStride;
            for (int bank=0; bank < pixelStride; bank++) {
                final int pi = (bank + bankShift) % pixelStride;
                int p = previous[pi];
                int endOfRow = scanlineStride - start;
                for (int i=bank;;) {
                    final int endOfPass = Math.min(endOfRow, view.limit());
                    while (i < endOfPass) {
                        final int v = view.get(i);
                        view.put(i, v - p);
                        p = v;
                        i += pixelStride;
                    }
                    if (i < endOfRow) break;
                    endOfRow += scanlineStride;
                    p = 0;
                }
                previous[pi] = p;
            }
            return view.limit();
        }

        /** Writes pending data and resets the predictor for the next tile to write. */
        @Override public void finish(final ChannelDataOutput owner) throws IOException {
            super.finish(owner);
            Arrays.fill(previous, 0);
        }
    }



    /**
     * A horizontal predictor working on single-precision floating point values.
     * The code of this class is a copy of {@link Bytes} adapted for floating point values.
     */
    private static final class Floats extends HorizontalPredictor {
        /** Sample values of the previous pixel. */
        private final float[] previous;

        /** Creates a new predictor. */
        Floats(final PixelChannel output, final int pixelStride, final int scanlineStride) {
            super(output, scanlineStride);
            previous = new float[pixelStride];
        }

        /** The number of bytes in each sample value. */
        @Override int sampleSize() {
            return Float.BYTES;
        }

        /** Applies the differential predictor. */
        @Override int apply(final ByteBuffer buffer, final int start) {
            final FloatBuffer view = buffer.asFloatBuffer();
            final int pixelStride = previous.length;
            final int bankShift   = start % pixelStride;
            for (int bank=0; bank < pixelStride; bank++) {
                final int pi = (bank + bankShift) % pixelStride;
                float p = previous[pi];
                int endOfRow = scanlineStride - start;
                for (int i=bank;;) {
                    final int endOfPass = Math.min(endOfRow, view.limit());
                    while (i < endOfPass) {
                        final float v = view.get(i);
                        view.put(i, v - p);
                        p = v;
                        i += pixelStride;
                    }
                    if (i < endOfRow) break;
                    endOfRow += scanlineStride;
                    p = 0;
                }
                previous[pi] = p;
            }
            return view.limit();
        }

        /** Writes pending data and resets the predictor for the next tile to write. */
        @Override public void finish(final ChannelDataOutput owner) throws IOException {
            super.finish(owner);
            Arrays.fill(previous, 0);
        }
    }



    /**
     * A horizontal predictor working on double-precision floating point values.
     * The code of this class is a copy of {@link Bytes} adapted for floating point values.
     */
    private static final class Doubles extends HorizontalPredictor {
        /** Sample values of the previous pixel. */
        private final double[] previous;

        /** Creates a new predictor. */
        Doubles(final PixelChannel output, final int pixelStride, final int scanlineStride) {
            super(output, scanlineStride);
            previous = new double[pixelStride];
        }

        /** The number of bytes in each sample value. */
        @Override int sampleSize() {
            return Double.BYTES;
        }

        /** Applies the differential predictor. */
        @Override int apply(final ByteBuffer buffer, final int start) {
            final DoubleBuffer view = buffer.asDoubleBuffer();
            final int pixelStride = previous.length;
            final int bankShift   = start % pixelStride;
            for (int bank=0; bank < pixelStride; bank++) {
                final int pi = (bank + bankShift) % pixelStride;
                double p = previous[pi];
                int endOfRow = scanlineStride - start;
                for (int i=bank;;) {
                    final int endOfPass = Math.min(endOfRow, view.limit());
                    while (i < endOfPass) {
                        final double v = view.get(i);
                        view.put(i, v - p);
                        p = v;
                        i += pixelStride;
                    }
                    if (i < endOfRow) break;
                    endOfRow += scanlineStride;
                    p = 0;
                }
                previous[pi] = p;
            }
            return view.limit();
        }

        /** Writes pending data and resets the predictor for the next tile to write. */
        @Override public void finish(final ChannelDataOutput owner) throws IOException {
            super.finish(owner);
            Arrays.fill(previous, 0);
        }
    }
}
