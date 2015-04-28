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
package org.apache.sis.internal.storage;

import java.util.Arrays;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link ChannelImageInputStream}. A buffer is filled with random data
 * and a standard {@link ImageInputStream} is used for comparison purpose.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
@DependsOn(ChannelDataInputTest.class)
public final strictfp class ChannelImageInputStreamTest extends ChannelDataTestCase {
    /**
     * The implementation to test.
     */
    private ChannelImageInputStream testedStream;

    /**
     * A stream to use as a reference implementation.
     */
    private ImageInputStream referenceStream;

    /**
     * Fills a buffer with random data and compares the result with a standard image input stream.
     * We will allocate a small buffer for the {@code ChannelImageInputStream} in order to force
     * frequent interactions between the buffer and the channel.
     *
     * @throws IOException Should never happen.
     */
    @Test
    public void testWithRandomData() throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_MAX_CAPACITY);
        final ByteOrder byteOrder = random.nextBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        final byte[] data = createRandomArray(STREAM_LENGTH);
        referenceStream = ImageIO.createImageInputStream(new ByteArrayInputStream(data));
        referenceStream.setByteOrder(byteOrder);
        testedStream = new ChannelImageInputStream("testWithRandomData",
                Channels.newChannel(new ByteArrayInputStream(data)), buffer, false);
        testedStream.setByteOrder(byteOrder);
        transferRandomData(testedStream, data.length - ARRAY_MAX_LENGTH, 24);
    }

    /**
     * Reads a random unit of data using a method selected randomly.
     * This method is invoked (indirectly) by {@link #testWithRandomData()}.
     */
    @Override
    final void transferRandomData(final int operation) throws IOException {
        final ChannelImageInputStream t = testedStream;
        final ImageInputStream r = referenceStream;
        switch (operation) {
            default: throw new AssertionError(operation);
            case  0: assertEquals("read()",              r.read(),              t.read());              break;
            case  1: assertEquals("readBoolean()",       r.readBoolean(),       t.readBoolean());       break;
            case  2: assertEquals("readChar()",          r.readChar(),          t.readChar());          break;
            case  3: assertEquals("readByte()",          r.readByte(),          t.readByte());          break;
            case  4: assertEquals("readShort()",         r.readShort(),         t.readShort());         break;
            case  5: assertEquals("readUnsignedShort()", r.readUnsignedShort(), t.readUnsignedShort()); break;
            case  6: assertEquals("readInt()",           r.readInt(),           t.readInt());           break;
            case  7: assertEquals("readUnsignedInt()",   r.readUnsignedInt(),   t.readUnsignedInt());   break;
            case  8: assertEquals("readLong()",          r.readLong(),          t.readLong());          break;
            case  9: assertEquals("readFloat()",         r.readFloat(),         t.readFloat(),  0f);    break;
            case 10: assertEquals("readDouble()",        r.readDouble(),        t.readDouble(), 0d);    break;
            case 11: assertEquals("readBit()",           r.readBit(),           t.readBit());           break;
            case 12: {
                final int n = random.nextInt(Long.SIZE + 1);
                assertEquals("readBits(" + n + ')', r.readBits(n), t.readBits(n));
                break;
            }
            case 13: {
                final int length = random.nextInt(ARRAY_MAX_LENGTH);
                final byte[] actual = new byte[length];
                final int n = t.read(actual);
                assertFalse("Reached EOF", n < 0);
                final byte[] expected = new byte[n];
                r.readFully(expected);
                assertArrayEquals("read(byte[])", expected, actual);
                break;
            }
            case 14: {
                final int length = random.nextInt(ARRAY_MAX_LENGTH);
                final byte[] expected = new byte[length]; r.readFully(expected);
                final byte[] actual   = new byte[length]; t.readFully(actual);
                assertArrayEquals("readFully(byte[])", expected, actual);
                break;
            }
            case 15: {
                final int length = random.nextInt(ARRAY_MAX_LENGTH / (Character.SIZE / Byte.SIZE));
                final char[] expected = new char[length]; r.readFully(expected, 0, length);
                final char[] actual   = new char[length]; t.readFully(actual,   0, length);
                assertArrayEquals("readFully(char[])", expected, actual);
                break;
            }
            case 16: {
                final int length = random.nextInt(ARRAY_MAX_LENGTH / (Short.SIZE / Byte.SIZE));
                final short[] expected = new short[length]; r.readFully(expected, 0, length);
                final short[] actual   = new short[length]; t.readFully(actual,   0, length);
                assertArrayEquals("readFully(short[])", expected, actual);
                break;
            }
            case 17: {
                final int length = random.nextInt(ARRAY_MAX_LENGTH / (Integer.SIZE / Byte.SIZE));
                final int[] expected = new int[length]; r.readFully(expected, 0, length);
                final int[] actual   = new int[length]; t.readFully(actual,   0, length);
                assertArrayEquals("readFully(int[])", expected, actual);
                break;
            }
            case 18: {
                final int length = random.nextInt(ARRAY_MAX_LENGTH / (Long.SIZE / Byte.SIZE));
                final long[] expected = new long[length]; r.readFully(expected, 0, length);
                final long[] actual   = new long[length]; t.readFully(actual,   0, length);
                assertArrayEquals("readFully(long[])", expected, actual);
                break;
            }
            case 19: {
                final int length = random.nextInt(ARRAY_MAX_LENGTH / (Float.SIZE / Byte.SIZE));
                final float[] expected = new float[length]; r.readFully(expected, 0, length);
                final float[] actual   = new float[length]; t.readFully(actual,   0, length);
                assertTrue("readFully(float[])", Arrays.equals(expected, actual));
                break;
            }
            case 20: {
                final int length = random.nextInt(ARRAY_MAX_LENGTH / (Double.SIZE / Byte.SIZE));
                final double[] expected = new double[length]; r.readFully(expected, 0, length);
                final double[] actual   = new double[length]; t.readFully(actual,   0, length);
                assertTrue("readFully(double[])", Arrays.equals(expected, actual));
                break;
            }
            case 21: {
                final long length = random.nextInt(ARRAY_MAX_LENGTH);
                final long n = t.skipBytes(length);
                assertFalse("Reached EOF", n < 0);
                r.readFully(new byte[(int) n]);
                assertEquals("skipBytes(int)", r.getStreamPosition(), t.getStreamPosition());
                break;
            }
            case 22: {
                long flushedPosition = StrictMath.max(r.getFlushedPosition(), t.getFlushedPosition());
                flushedPosition += random.nextInt(1 + (int) (r.getStreamPosition() - flushedPosition));
                r.flushBefore(flushedPosition);
                t.flushBefore(flushedPosition);
                break;
            }
            case 23: {
                r.flush();
                t.flush();
                break;
            }
        }
        assertEquals("getStreamPosition()", r.getStreamPosition(), t.getStreamPosition());
        assertEquals("getBitOffset()",      r.getBitOffset(),      t.getBitOffset());
    }
}
