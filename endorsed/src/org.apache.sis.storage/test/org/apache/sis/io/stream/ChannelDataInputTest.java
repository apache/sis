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

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link ChannelDataInput}. First, a buffer is filled with random data. Then, a view over a portion
 * of that buffer is used for the tests, while the original full buffer is used for comparison purpose.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ChannelDataInputTest extends ChannelDataTestCase {
    /**
     * The implementation to test.
     */
    private ChannelDataInput testedStream;

    /**
     * A stream to use as a reference implementation.
     */
    private DataInput referenceStream;

    /**
     * Creates a new test case.
     */
    public ChannelDataInputTest() {
    }

    /**
     * Fills a buffer with random data and compare the result with a standard image input stream.
     * We allocate a small buffer for the {@code ChannelDataInput} in order to force frequent
     * interactions between the buffer and the channel.
     *
     * @throws IOException should never happen since we read and write in memory only.
     */
    @Test
    public void testAllReadMethods() throws IOException {
        final byte[] array = createRandomArray(STREAM_LENGTH);
        referenceStream = new DataInputStream(new ByteArrayInputStream(array));
        testedStream = new ChannelDataInput("testAllReadMethods",
                new DripByteChannel(array, random, 1, 1024),
                ByteBuffer.allocate(randomBufferCapacity()), false);
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
            case  0: assertEquals(r.readByte(),              t.readByte(),          "readByte()");          break;
            case  1: assertEquals(r.readShort(),             t.readShort(),         "readShort()");         break;
            case  2: assertEquals(r.readUnsignedShort(),     t.readUnsignedShort(), "readUnsignedShort()"); break;
            case  3: assertEquals(r.readChar(),              t.readChar(),          "readChar()");          break;
            case  4: assertEquals(r.readInt(),               t.readInt(),           "readInt()");           break;
            case  5: assertEquals(r.readInt() & 0xFFFFFFFFL, t.readUnsignedInt(),   "readUnsignedInt()");   break;
            case  6: assertEquals(r.readLong(),              t.readLong(),          "readLong()");          break;
            case  7: assertEquals(r.readFloat(),             t.readFloat(),         "readFloat()");         break;
            case  8: assertEquals(r.readDouble(),            t.readDouble(),        "readDouble()");        break;
            case  9: {
                final int n = random.nextInt(ARRAY_MAX_LENGTH);
                final byte[] tmp = new byte[n];
                r.readFully(tmp);
                assertArrayEquals(tmp, t.readBytes(n), "readBytes(int)");
                break;
            }
            case 10: {
                final int n = random.nextInt(ARRAY_MAX_LENGTH / Character.BYTES);
                final char[] tmp = new char[n];
                for (int i=0; i<n; i++) tmp[i] = r.readChar();
                assertArrayEquals(tmp, t.readChars(n), "readChars(int)");
                break;
            }
            case 11: {
                final int n = random.nextInt(ARRAY_MAX_LENGTH / Short.BYTES);
                final short[] tmp = new short[n];
                for (int i=0; i<n; i++) tmp[i] = r.readShort();
                assertArrayEquals(tmp, t.readShorts(n), "readShorts(int)");
                break;
            }
            case 12: {
                final int n = random.nextInt(ARRAY_MAX_LENGTH / Integer.BYTES);
                final int[] tmp = new int[n];
                for (int i=0; i<n; i++) tmp[i] = r.readInt();
                assertArrayEquals(tmp, t.readInts(n), "readInts(int)");
                break;
            }
            case 13: {
                final int n = random.nextInt(ARRAY_MAX_LENGTH / Long.BYTES);
                final long[] tmp = new long[n];
                for (int i=0; i<n; i++) tmp[i] = r.readLong();
                assertArrayEquals(tmp, t.readLongs(n), "readLongs(int)");
                break;
            }
            case 14: {
                final int n = random.nextInt(ARRAY_MAX_LENGTH / Float.BYTES);
                final float[] tmp = new float[n];
                for (int i=0; i<n; i++) tmp[i] = r.readFloat();
                assertArrayEquals(tmp, t.readFloats(n), "readFloats(int)");
                break;
            }
            case 15: {
                final int n = random.nextInt(ARRAY_MAX_LENGTH / Double.BYTES);
                final double[] tmp = new double[n];
                for (int i=0; i<n; i++) tmp[i] = r.readDouble();
                assertArrayEquals(tmp, t.readDoubles(n), "readDoubles(int)");
                break;
            }
        }
    }

    /**
     * Tests the {@link ChannelDataInput#readString(int, Charset)} method.
     *
     * @throws IOException should never happen since we read and write in memory only.
     */
    @Test
    public void testReadString() throws IOException {
        final String expected = "お元気ですか";
        final byte[] array = expected.getBytes(StandardCharsets.UTF_8);
        assertEquals(expected.length()*3, array.length);    // Sanity check.
        final var input = new ChannelDataInput("testReadString",
                              new DripByteChannel(array, random, 1, 32),
                              ByteBuffer.allocate(array.length + 4), false);
        assertEquals(expected, input.readString(array.length, StandardCharsets.UTF_8));
        assertFalse(input.buffer.hasRemaining());
    }

    /**
     * Tests the {@link ChannelDataInput#readNullTerminatedString(Charset)} method.
     *
     * @throws IOException should never happen since we read and write in memory only.
     */
    public void testReadNullTerminatedString() throws IOException {
        for (int i=0; i<=8; i++) {
            String  expected = "theatre théâtre 劇場";
            Charset encoding;
            int     charSize;
            char    bom = 0;
            switch (i) {
                case 0: {
                    encoding = StandardCharsets.US_ASCII;
                    expected = expected.substring(0, expected.indexOf(' '));
                    charSize = Byte.BYTES;
                    break;
                }
                case 1: {
                    encoding = StandardCharsets.ISO_8859_1;
                    expected = expected.substring(0, expected.lastIndexOf(' '));
                    charSize = Byte.BYTES;
                    break;
                }
                case 2: {
                    encoding = StandardCharsets.UTF_8;
                    charSize = Byte.BYTES;
                    break;
                }
                case 3: {
                    encoding = StandardCharsets.UTF_16;
                    charSize = Short.BYTES;
                    break;
                }
                case 4: {
                    encoding = StandardCharsets.UTF_16BE;
                    charSize = Short.BYTES;
                    break;
                }
                case 5: {
                    encoding = StandardCharsets.UTF_16LE;
                    charSize = Short.BYTES;
                    break;
                }
                case 6: {
                    encoding = StandardCharsets.UTF_8;
                    charSize = Byte.BYTES;
                    bom      = ' ';             // Arbitrary value for meaning "do not write BOM".
                    break;
                }
                case 7: {
                    encoding = StandardCharsets.UTF_16BE;
                    charSize = Short.BYTES;
                    bom      = '\uFEFF';
                    break;
                }
                case 8: {
                    encoding = StandardCharsets.UTF_16LE;
                    charSize = Short.BYTES;
                    bom      = '\uFFFE';        // BOM with swapped bytes.
                    break;
                }
                default: throw new AssertionError(i);
            }
            int base = random.nextInt(5) + 3;
            final byte[] bytes = expected.getBytes(encoding);
            final byte[] array = new byte[base + bytes.length + charSize];
            System.arraycopy(bytes, 0, array, base, bytes.length);
            if (bom > ' ') {
                array[--base] = (byte) (bom & 0xFF);
                array[--base] = (byte) (bom >>> Byte.SIZE);
            }
            final var input = new ChannelDataInput("testReadNullTerminatedString",
                                  new DripByteChannel(array, random, charSize, 24),
                                  ByteBuffer.allocate(array.length + 4), false);
            input.seek(base);
            assertEquals(expected, input.readNullTerminatedString(bom == 0 ? encoding : null));
            assertFalse(input.buffer.hasRemaining());
        }
    }

    /**
     * Tests {@link ChannelDataInput#seek(long)} on a channel that do not implement
     * {@link java.nio.channels.SeekableByteChannel}.
     *
     * @throws IOException should never happen since we read and write in memory only.
     */
    @Test
    public void testSeekOnForwardOnlyChannel() throws IOException {
        int length = random.nextInt(2048) + 1024;
        final byte[] array = createRandomArray(length);
        length -= Long.BYTES;   // Safety against buffer underflow.
        final ByteBuffer buffer = ByteBuffer.wrap(array);
        final var input = new ChannelDataInput("testSeekOnForwardOnlyChannel",
                              new DripByteChannel(array, random, 1, 2048),
                              ByteBuffer.allocate(random.nextInt(64) + 16), false);
        int position = 0;
        while (position < length) {
            input.seek(position);
            assertEquals(position, input.getStreamPosition(), "getStreamPosition()");
            assertEquals(buffer.getLong(position), input.readLong());
            position += random.nextInt(128);
        }
    }

    /**
     * Tests {@link ChannelDataInput#prefetch()}.
     *
     * @throws IOException should never happen since we read and write in memory only.
     */
    @Test
    public void testPrefetch() throws IOException {
        final int    length = random.nextInt(256) + 128;
        final byte[] array  = createRandomArray(length);
        final var    buffer = ByteBuffer.allocate(random.nextInt(64) + 16);
        final var    input  = new ChannelDataInput("testPrefetch",
                                  new DripByteChannel(array, random, 1, 64),
                                  buffer, false);
        int position = 0;
        while (position != length) {
            if (random.nextBoolean()) {
                assertEquals(array[position++], input.readByte());
            }
            /*
             * Prefetch a random number of bytes and verifies the buffer status.
             */
            final int p = buffer.position();
            final int m = buffer.limit();
            final int n = input.prefetch();
            assertEquals(p, buffer.position(), "Position shall be unchanged.");
            final int limit = buffer.limit();
            if (n >= 0) {
                // Usual case.
                assertTrue(limit > m, "Limit shall be increased.");
            } else {
                // Buffer is full or channel reached the end of stream.
                assertEquals(m, limit, "Limit shall be unchanged");
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
