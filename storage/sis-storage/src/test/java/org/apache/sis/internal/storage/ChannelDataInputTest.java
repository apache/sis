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

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link ChannelDataInput}. First, a buffer is filled with random data. Then, a view over a portion
 * of that buffer is used for the tests, while the original full buffer is used for comparison purpose.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
public final strictfp class ChannelDataInputTest extends ChannelDataTestCase {
    /**
     * The implementation to test.
     */
    private ChannelDataInput testedStream;

    /**
     * A stream to use as a reference implementation.
     */
    private DataInput referenceStream;

    /**
     * Fills a buffer with random data and compare the result with a standard image input stream.
     * We allocate a small buffer for the {@code ChannelDataInput} in order to force frequent
     * interactions between the buffer and the channel.
     *
     * @throws IOException Should never happen.
     */
    @Test
    public void testAllReadMethods() throws IOException {
        final byte[] array = createRandomArray(STREAM_LENGTH);
        referenceStream = new DataInputStream(new ByteArrayInputStream(array));
        testedStream = new ChannelDataInput("testAllReadMethods", new DripByteChannel(array, random, 1, 1024),
                ByteBuffer.allocate(random.nextInt(BUFFER_MAX_CAPACITY) + (Double.SIZE / Byte.SIZE)), false);
        transferRandomData(testedStream, array.length - ARRAY_MAX_LENGTH, 16);
    }

    /**
     * Reads a random unit of data using a method selected randomly.
     * This method is invoked (indirectly) by {@link #testAllReadMethods()}.
     */
    @Override
    final void transferRandomData(final int operation) throws IOException {
        final ChannelDataInput t = testedStream;
        final DataInput r = referenceStream;
        switch (operation) {
            default: throw new AssertionError(operation);
            case  0: assertEquals("readByte()",          r.readByte(),              t.readByte());          break;
            case  1: assertEquals("readShort()",         r.readShort(),             t.readShort());         break;
            case  2: assertEquals("readUnsignedShort()", r.readUnsignedShort(),     t.readUnsignedShort()); break;
            case  3: assertEquals("readChar()",          r.readChar(),              t.readChar());          break;
            case  4: assertEquals("readInt()",           r.readInt(),               t.readInt());           break;
            case  5: assertEquals("readUnsignedInt()",   r.readInt() & 0xFFFFFFFFL, t.readUnsignedInt());   break;
            case  6: assertEquals("readLong()",          r.readLong(),              t.readLong());          break;
            case  7: assertEquals("readFloat()",         r.readFloat(),             t.readFloat(),  0f);    break;
            case  8: assertEquals("readDouble()",        r.readDouble(),            t.readDouble(), 0d);    break;
            case  9: {
                final int n = random.nextInt(ARRAY_MAX_LENGTH);
                final byte[] tmp = new byte[n];
                r.readFully(tmp);
                assertArrayEquals("readBytes(int)", tmp, t.readBytes(n));
                break;
            }
            case 10: {
                final int n = random.nextInt(ARRAY_MAX_LENGTH / (Character.SIZE / Byte.SIZE));
                final char[] tmp = new char[n];
                for (int i=0; i<n; i++) tmp[i] = r.readChar();
                assertArrayEquals("readChars(int)", tmp, t.readChars(n));
                break;
            }
            case 11: {
                final int n = random.nextInt(ARRAY_MAX_LENGTH / (Short.SIZE / Byte.SIZE));
                final short[] tmp = new short[n];
                for (int i=0; i<n; i++) tmp[i] = r.readShort();
                assertArrayEquals("readShorts(int)", tmp, t.readShorts(n));
                break;
            }
            case 12: {
                final int n = random.nextInt(ARRAY_MAX_LENGTH / (Integer.SIZE / Byte.SIZE));
                final int[] tmp = new int[n];
                for (int i=0; i<n; i++) tmp[i] = r.readInt();
                assertArrayEquals("readInts(int)", tmp, t.readInts(n));
                break;
            }
            case 13: {
                final int n = random.nextInt(ARRAY_MAX_LENGTH / (Long.SIZE / Byte.SIZE));
                final long[] tmp = new long[n];
                for (int i=0; i<n; i++) tmp[i] = r.readLong();
                assertArrayEquals("readLongs(int)", tmp, t.readLongs(n));
                break;
            }
            case 14: {
                final int n = random.nextInt(ARRAY_MAX_LENGTH / (Float.SIZE / Byte.SIZE));
                final float[] tmp = new float[n];
                for (int i=0; i<n; i++) tmp[i] = r.readFloat();
                assertArrayEquals("readFloats(int)", tmp, t.readFloats(n), 0);
                break;
            }
            case 15: {
                final int n = random.nextInt(ARRAY_MAX_LENGTH / (Double.SIZE / Byte.SIZE));
                final double[] tmp = new double[n];
                for (int i=0; i<n; i++) tmp[i] = r.readDouble();
                assertArrayEquals("readDoubles(int)", tmp, t.readDoubles(n), 0);
                break;
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
        final byte[] array    = expected.getBytes("UTF-8");
        assertEquals(expected.length()*3, array.length); // Sanity check.
        final ChannelDataInput input = new ChannelDataInput("testReadString",
                new DripByteChannel(array, random, 1, 32),
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
        int length = random.nextInt(2048) + 1024;
        final byte[] array = createRandomArray(length);
        length -= (Long.SIZE / Byte.SIZE); // Safety against buffer underflow.
        final ByteBuffer buffer = ByteBuffer.wrap(array);
        final ChannelDataInput input = new ChannelDataInput("testSeekOnForwardOnlyChannel",
                new DripByteChannel(array, random, 1, 2048),
                ByteBuffer.allocate(random.nextInt(64) + 16), false);
        int position = 0;
        while (position < length) {
            input.seek(position);
            assertEquals("getStreamPosition()", position, input.getStreamPosition());
            assertEquals(buffer.getLong(position), input.readLong());
            position += random.nextInt(128);
        }
    }

    /**
     * Tests {@link ChannelDataInput#prefetch()}.
     *
     * @throws IOException Should never happen.
     */
    @Test
    public void testPrefetch() throws IOException {
        final int        length = random.nextInt(256) + 128;
        final byte[]     array  = createRandomArray(length);
        final ByteBuffer buffer = ByteBuffer.allocate(random.nextInt(64) + 16);
        final ChannelDataInput input = new ChannelDataInput("testPrefetch",
                new DripByteChannel(array, random, 1, 64), buffer, false);
        int position = 0;
        while (position != length) {
            if (random.nextBoolean()) {
                assertEquals(array[position++], input.readByte());
            }
            /*
             * Prefetch a random amount of bytes and verifies the buffer status.
             */
            final int p = buffer.position();
            final int m = buffer.limit();
            final int n = input.prefetch();
            assertEquals("Position shall be unchanged.", p, buffer.position());
            final int limit = buffer.limit();
            if (n >= 0) {
                // Usual case.
                assertTrue("Limit shall be increased.", limit > m);
            } else {
                // Buffer is full or channel reached the end of stream.
                assertEquals("Limit shall be unchanged", m, limit);
            }
            /*
             * Compare the buffer content with the original data array. The comparison starts
             * from the buffer begining, in order to ensure that previous data are unchanged.
             */
            final int offset = position - buffer.position();
            for (int i=0; i<limit; i++) {
                assertEquals(array[offset + i], buffer.get(i));
            }
        }
    }
}
