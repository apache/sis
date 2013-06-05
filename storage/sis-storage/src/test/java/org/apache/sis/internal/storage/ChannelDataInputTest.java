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

import java.util.Random;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link ChannelDataInput}. First, a buffer is filled with random data. Then, a view over a portion
 * of that buffer is used for the tests, while the original full buffer is used for comparison purpose.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.07)
 * @version 0.3
 * @module
 */
public final strictfp class ChannelDataInputTest extends TestCase {
    /**
     * The maximal size of the arrays to be used for the tests, in bytes.
     */
    private static final int ARRAY_MAX_SIZE = 256;

    /**
     * Fills a buffer with random data and compare the result with a standard image input stream.
     * We allocate a small buffer for the {@code ChannelDataInput} in order to force frequent
     * interactions between the buffer and the channel.
     *
     * @throws IOException Should never happen.
     */
    @Test
    public void testAllReadMethods() throws IOException {
        final Random random = TestUtilities.createRandomNumberGenerator("testAllReadMethods");
        final byte[] array = createRandomArray(ARRAY_MAX_SIZE * 1024, random);
        compareStreamToBuffer(random, array.length,
                new DataInputStream(new ByteArrayInputStream(array)),
                new ChannelDataInput("testAllReadMethods",
                    Channels.newChannel(new ByteArrayInputStream(array)),
                    ByteBuffer.allocate(random.nextInt(ARRAY_MAX_SIZE / 4) + (Double.SIZE / Byte.SIZE)), false));
    }

    /**
     * Compares the data returned by the given input to the data returned by the given buffer.
     *
     * @param  random A random number generator for executing the test.
     * @param  length Number of bytes in the {@code r} stream.
     * @param  data   A stream over all expected data.
     * @param  input  The instance to test.
     * @throws IOException Should never happen.
     */
    private static void compareStreamToBuffer(final Random random, int length,
            final DataInput data, final ChannelDataInput input) throws IOException
    {
        length -= ARRAY_MAX_SIZE; // Margin against buffer underflow.
        while (input.getStreamPosition() < length) {
            final int operation = random.nextInt(16);
            switch (operation) {
                default: throw new AssertionError(operation);
                case  0: assertEquals("readByte()",          data.readByte(),              input.readByte());          break;
                case  1: assertEquals("readShort()",         data.readShort(),             input.readShort());         break;
                case  2: assertEquals("readUnsignedShort()", data.readUnsignedShort(),     input.readUnsignedShort()); break;
                case  3: assertEquals("readChar()",          data.readChar(),              input.readChar());          break;
                case  4: assertEquals("readInt()",           data.readInt(),               input.readInt());           break;
                case  5: assertEquals("readUnsignedInt()",   data.readInt() & 0xFFFFFFFFL, input.readUnsignedInt());   break;
                case  6: assertEquals("readLong()",          data.readLong(),              input.readLong());          break;
                case  7: assertEquals("readFloat()",         data.readFloat(),             input.readFloat(),  0f);    break;
                case  8: assertEquals("readDouble()",        data.readDouble(),            input.readDouble(), 0d);    break;
                case  9: {
                    final int n = random.nextInt(ARRAY_MAX_SIZE);
                    final byte[] tmp = new byte[n];
                    data.readFully(tmp);
                    assertArrayEquals("readBytes(int)", tmp, input.readBytes(n));
                    break;
                }
                case 10: {
                    final int n = random.nextInt(ARRAY_MAX_SIZE / (Character.SIZE / Byte.SIZE));
                    final char[] tmp = new char[n];
                    for (int i=0; i<n; i++) tmp[i] = data.readChar();
                    assertArrayEquals("readChars(int)", tmp, input.readChars(n));
                    break;
                }
                case 11: {
                    final int n = random.nextInt(ARRAY_MAX_SIZE / (Short.SIZE / Byte.SIZE));
                    final short[] tmp = new short[n];
                    for (int i=0; i<n; i++) tmp[i] = data.readShort();
                    assertArrayEquals("readShorts(int)", tmp, input.readShorts(n));
                    break;
                }
                case 12: {
                    final int n = random.nextInt(ARRAY_MAX_SIZE / (Integer.SIZE / Byte.SIZE));
                    final int[] tmp = new int[n];
                    for (int i=0; i<n; i++) tmp[i] = data.readInt();
                    assertArrayEquals("readInts(int)", tmp, input.readInts(n));
                    break;
                }
                case 13: {
                    final int n = random.nextInt(ARRAY_MAX_SIZE / (Long.SIZE / Byte.SIZE));
                    final long[] tmp = new long[n];
                    for (int i=0; i<n; i++) tmp[i] = data.readLong();
                    assertArrayEquals("readLongs(int)", tmp, input.readLongs(n));
                    break;
                }
                case 14: {
                    final int n = random.nextInt(ARRAY_MAX_SIZE / (Float.SIZE / Byte.SIZE));
                    final float[] tmp = new float[n];
                    for (int i=0; i<n; i++) tmp[i] = data.readFloat();
                    assertArrayEquals("readFloats(int)", tmp, input.readFloats(n), 0);
                    break;
                }
                case 15: {
                    final int n = random.nextInt(ARRAY_MAX_SIZE / (Double.SIZE / Byte.SIZE));
                    final double[] tmp = new double[n];
                    for (int i=0; i<n; i++) tmp[i] = data.readDouble();
                    assertArrayEquals("readDoubles(int)", tmp, input.readDoubles(n), 0);
                    break;
                }
            }
        }
    }

    /**
     * Tests the {@link ChannelDataInput#readString(int, String)} method.
     *
     * @throws IOException Should never happen.
     */
    @Test
    public void testReadString() throws IOException {
        final String expected = "お元気ですか";
        final byte[] array = expected.getBytes("UTF-8");
        assertEquals(expected.length()*3, array.length); // Sanity check.
        final ChannelDataInput input = new ChannelDataInput("testReadString",
                Channels.newChannel(new ByteArrayInputStream(array)),
                ByteBuffer.allocate(array.length + 4), false);
        assertEquals(expected, input.readString(array.length, "UTF-8"));
        assertFalse(input.buffer.hasRemaining());
    }

    /**
     * Tests {@link ChannelDataInput#seek(long)} on a channel that do not implement
     * {@link java.nio.channels.SeekableByteChannel}.
     *
     * @throws IOException Should never happen.
     */
    @Test
    public void testSeekOnForwardOnlyChannel() throws IOException {
        final Random random = TestUtilities.createRandomNumberGenerator("testSeekOnForwardOnlyChannel");
        int length = random.nextInt(2048) + 1024;
        final byte[] array = createRandomArray(length, random);
        length -= (Long.SIZE / Byte.SIZE) - 1; // Safety against buffer underflow.
        final ByteBuffer buffer = ByteBuffer.wrap(array);
        final ChannelDataInput input = new ChannelDataInput("testSeekOnForwardOnlyChannel",
                Channels.newChannel(new ByteArrayInputStream(array)),
                ByteBuffer.allocate(random.nextInt(64) + 16), false);
        int position = 0;
        while (position <= length) {
            input.seek(position);
            assertEquals("getStreamPosition()", position, input.getStreamPosition());
            assertEquals(buffer.getLong(position), input.readLong());
            position += random.nextInt(128);
        }
    }

    /**
     * Creates an array filled with random values.
     *
     * @param length The length of the array to create.
     * @param random The random number generator to use.
     */
    static byte[] createRandomArray(final int length, final Random random) {
        final byte[] array = new byte[length];
        for (int i=0; i<length; i++) {
            array[i] = (byte) random.nextInt(256);
        }
        return array;
    }
}
