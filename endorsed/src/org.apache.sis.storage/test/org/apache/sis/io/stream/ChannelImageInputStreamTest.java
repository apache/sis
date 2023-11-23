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

import java.util.Arrays;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import javax.imageio.ImageIO;
import javax.imageio.stream.IIOByteBuffer;
import javax.imageio.stream.ImageInputStream;

// Test dependencies
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.DependsOn;


/**
 * Tests {@link ChannelImageInputStream}. A buffer is filled with random data
 * and a standard {@link ImageInputStream} is used for comparison purpose.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@DependsOn(ChannelDataInputTest.class)
public final class ChannelImageInputStreamTest extends ChannelDataTestCase {
    /**
     * The implementation to test.
     */
    private ChannelImageInputStream testedStream;

    /**
     * A stream to use as a reference implementation.
     */
    private ImageInputStream referenceStream;

    /**
     * Creates a new test case.
     */
    public ChannelImageInputStreamTest() {
    }

    /**
     * Fills a buffer with random data and compares the result with a standard image input stream.
     * We will allocate a small buffer for the {@code ChannelImageInputStream} in order to force
     * frequent interactions between the buffer and the channel.
     *
     * @throws IOException should never happen since we read and write in memory only.
     */
    @Test
    public void testWithRandomData() throws IOException {
        test(ByteBuffer.allocate(BUFFER_MAX_CAPACITY));
    }

    /**
     * Same test, but using a direct buffer.
     * Some code paths are different.
     *
     * @throws IOException should never happen since we read and write in memory only.
     */
    @Test
    public void testWithDirectBuffer() throws IOException {
        test(ByteBuffer.allocateDirect(BUFFER_MAX_CAPACITY));
    }

    /**
     * Runs the tests with the specified buffer.
     */
    private void test(final ByteBuffer buffer) throws IOException {
        final ByteOrder byteOrder = random.nextBoolean() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        final byte[] data = createRandomArray(STREAM_LENGTH);
        referenceStream = ImageIO.createImageInputStream(new ByteArrayInputStream(data));
        referenceStream.setByteOrder(byteOrder);
        testedStream = new ChannelImageInputStream("testWithRandomData",
                Channels.newChannel(new ByteArrayInputStream(data)), buffer, false);
        testedStream.setByteOrder(byteOrder);
        transferRandomData(testedStream, data.length - ARRAY_MAX_LENGTH, 25);
        testedStream.close();
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
            case  0: assertEquals(r.read(),              t.read(),               "read()");              break;
            case  1: assertEquals(r.readBoolean(),       t.readBoolean(),        "readBoolean()");       break;
            case  2: assertEquals(r.readChar(),          t.readChar(),           "readChar()");          break;
            case  3: assertEquals(r.readByte(),          t.readByte(),           "readByte()");          break;
            case  4: assertEquals(r.readShort(),         t.readShort(),          "readShort()");         break;
            case  5: assertEquals(r.readUnsignedShort(), t.readUnsignedShort(),  "readUnsignedShort()"); break;
            case  6: assertEquals(r.readInt(),           t.readInt(),            "readInt()");           break;
            case  7: assertEquals(r.readUnsignedInt(),   t.readUnsignedInt(),    "readUnsignedInt()");   break;
            case  8: assertEquals(r.readLong(),          t.readLong(),           "readLong()");          break;
            case  9: assertEquals(r.readFloat(),         t.readFloat(),          "readFloat()");         break;
            case 10: assertEquals(r.readDouble(),        t.readDouble(),         "readDouble()");        break;
            case 11: assertEquals(r.readBit(),           t.readBit(),            "readBit()");           break;
            case 12: {
                final int n = random.nextInt(Long.SIZE + 1);
                assertEquals(r.readBits(n), t.readBits(n), () -> "readBits(" + n + ')');
                break;
            }
            case 13: {
                final int length = random.nextInt(ARRAY_MAX_LENGTH);
                final byte[] actual = new byte[length];
                final int n = t.read(actual);
                assertFalse(n < 0, "Reached EOF");
                final byte[] expected = new byte[n];
                r.readFully(expected);
                assertArrayEquals(expected, actual, "read(byte[])");
                break;
            }
            case 14: {
                final int length = random.nextInt(ARRAY_MAX_LENGTH);
                final byte[] expected = new byte[length]; r.readFully(expected);
                final byte[] actual   = new byte[length]; t.readFully(actual);
                assertArrayEquals(expected, actual, "readFully(byte[])");
                break;
            }
            case 15: {
                final int length = random.nextInt(ARRAY_MAX_LENGTH / Character.BYTES);
                final char[] expected = new char[length]; r.readFully(expected, 0, length);
                final char[] actual   = new char[length]; t.readFully(actual,   0, length);
                assertArrayEquals(expected, actual, "readFully(char[])");
                break;
            }
            case 16: {
                final int length = random.nextInt(ARRAY_MAX_LENGTH / Short.BYTES);
                final short[] expected = new short[length]; r.readFully(expected, 0, length);
                final short[] actual   = new short[length]; t.readFully(actual,   0, length);
                assertArrayEquals(expected, actual, "readFully(short[])");
                break;
            }
            case 17: {
                final int length = random.nextInt(ARRAY_MAX_LENGTH / Integer.BYTES);
                final int[] expected = new int[length]; r.readFully(expected, 0, length);
                final int[] actual   = new int[length]; t.readFully(actual,   0, length);
                assertArrayEquals(expected, actual, "readFully(int[])");
                break;
            }
            case 18: {
                final int length = random.nextInt(ARRAY_MAX_LENGTH / Long.BYTES);
                final long[] expected = new long[length]; r.readFully(expected, 0, length);
                final long[] actual   = new long[length]; t.readFully(actual,   0, length);
                assertArrayEquals(expected, actual, "readFully(long[])");
                break;
            }
            case 19: {
                final int length = random.nextInt(ARRAY_MAX_LENGTH / Float.BYTES);
                final float[] expected = new float[length]; r.readFully(expected, 0, length);
                final float[] actual   = new float[length]; t.readFully(actual,   0, length);
                assertArrayEquals(expected, actual, "readFully(float[])");
                break;
            }
            case 20: {
                final int length = random.nextInt(ARRAY_MAX_LENGTH / Double.BYTES);
                final double[] expected = new double[length]; r.readFully(expected, 0, length);
                final double[] actual   = new double[length]; t.readFully(actual,   0, length);
                assertArrayEquals(expected, actual, "readFully(double[])");
                break;
            }
            case 21: {
                final IIOByteBuffer buffer = new IIOByteBuffer(null, 0, 0);
                t.readBytes(buffer, random.nextInt(ARRAY_MAX_LENGTH));
                final byte[] actual = data(buffer);
                r.readBytes(buffer, actual.length);
                final byte[] expected = data(buffer);
                assertArrayEquals(expected, actual, "readBytes(IIOByteBuffer)");
                break;
            }
            case 22: {
                final long length = random.nextInt(ARRAY_MAX_LENGTH);
                final long n = t.skipBytes(length);
                assertFalse(n < 0, "Reached EOF");
                r.readFully(new byte[(int) n]);
                assertEquals(r.getStreamPosition(), t.getStreamPosition(), "skipBytes(int)");
                break;
            }
            case 23: {
                long flushedPosition = StrictMath.max(r.getFlushedPosition(), t.getFlushedPosition());
                flushedPosition += random.nextInt(1 + (int) (r.getStreamPosition() - flushedPosition));
                r.flushBefore(flushedPosition);
                t.flushBefore(flushedPosition);
                break;
            }
            case 24: {
                r.flush();
                t.flush();
                break;
            }
        }
        assertEquals(r.getStreamPosition(), t.getStreamPosition(), "getStreamPosition()");
        assertEquals(r.getBitOffset(),      t.getBitOffset(),      "getBitOffset()");
    }

    /**
     * Returns a copy of the data in the given buffer.
     */
    private static byte[] data(final IIOByteBuffer buffer) {
        final int offset = buffer.getOffset();
        return Arrays.copyOfRange(buffer.getData(), offset, offset + buffer.getLength());
    }
}
