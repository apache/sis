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
package org.apache.sis.internal.storage.io;

import java.util.Random;
import java.util.OptionalLong;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link FileCacheByteChannel}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.4
 * @module
 */
public final strictfp class FileCacheByteChannelTest extends TestCase {
    /**
     * The implementation used for testing purpose.
     */
    private static final class Implementation extends FileCacheByteChannel {
        /**
         * Name of the test method. Used for error messages only.
         */
        private final String name;

        /**
         * Number of bytes in the input stream to use for testing purpose.
         * It should be large enough for forcing {@link #position(long, long)}
         * to skip bytes instead of reading them.
         */
        private final int length;

        /**
         * Generator of random numbers for controlling the behavior of this channel.
         */
        private final Random random;

        /**
         * Creates a new test channel.
         *
         * @param  test  a name to use for identifying the test in error messages.
         * @param  rg    generator of random numbers for controlling the behavior of this channel.
         * @throws IOException if the temporary file can not be created.
         */
        Implementation(final String test, final Random rg) throws IOException {
            super("Test-");
            name   = test;
            length = SKIP_THRESHOLD * 10 + rg.nextInt(SKIP_THRESHOLD * 10);
            random = rg;
        }

        /**
         * Returns a name to use in error messages.
         */
        @Override
        protected String filename() {
            return name;
        }

        /**
         * Creates an input stream which provides the bytes to read starting at the specified position.
         *
         * @param  start  position of the first byte to read (inclusive).
         * @param  end    position of the last byte to read with the returned stream (inclusive),
         *                or {@link Long#MAX_VALUE} for end of stream.
         * @return contains the input stream providing the bytes to read.
         */
        @Override
        protected Connection openConnection(long start, long end) {
            assertTrue(end >= 0);
            if (end >= length) end = length - 1;
            start = Math.max(start - random.nextInt(40), 0);
            end = Math.min(end + random.nextInt(40), length - 1);       // Inclusive.
            var input = new ComputedInputStream(Math.toIntExact(start), Math.toIntExact(end), random);
            return new Connection(input, start, end, length, true);
        }

        /**
         * Marks the given input stream as closed and notify that a new one can be created.
         */
        @Override
        protected boolean abort(final InputStream input) throws IOException {
            input.close();
            return true;
        }

        /**
         * Reads the next bytes from the channel and stores them in a random region of the given buffer.
         * On return, the buffer position is set on the first byte read and the buffer limit is set after
         * the last byte read.
         *
         * @param  dst  the buffer where to store the bytes that are read.
         * @return {@code true} if bytes have been read, or {@code false} on EOF.
         * @throws IOException if an error occurred when reading or writing to the temporary file.
         */
        final boolean readInRandomRegion(final ByteBuffer dst) throws IOException {
            int start = random.nextInt(dst.capacity());
            int end   = random.nextInt(dst.capacity());
            if (start > end) {
                int t = start;
                start = end;
                end   = t;
            }
            final int n = read(dst.limit(end).position(start));
            assertEquals("Number of bytes", Math.min(end - start, n), n);
            assertEquals("Buffer position", start + Math.max(n, 0), dst.flip().position(start).limit());
            return n >= 0;
        }
    }

    /**
     * Tests random operations on a stream of computed values.
     * The bytes values are determined by their position, which allows easy verifications.
     *
     * @throws IOException if an error occurred when reading or writing to the temporary file.
     */
    @Test
    public void testRandomOperations() throws IOException {
        final Random random = TestUtilities.createRandomNumberGenerator();
        final Implementation channel = new Implementation("test", random);
        final ByteBuffer buffer = ByteBuffer.allocate(random.nextInt(1000) + 1000);
        int position = 0;
        for (int i=0; i<10000; i++) {
            assertTrue(channel.isOpen());
            assertEquals(position, channel.position());
            if (random.nextInt(4) == 0) {
                position = random.nextInt(channel.length - 1);
                int end  = random.nextInt(channel.length - 1);
                if (position > end) {
                    int t = position;
                    position = end;
                    end = t;
                }
                channel.position(position, end - position + 1);
            }
            channel.readInRandomRegion(buffer);
            while (buffer.hasRemaining()) {
                assertEquals(ComputedInputStream.valueAt(position++), buffer.get());
            }
        }
        assertEquals(position, channel.position());
        channel.close();                                // Intentionally no "try with resource".
        assertFalse(channel.isOpen());
    }

    /**
     * Tests the constructor that parse HTTP ranges.
     *
     * @see FileCacheByteChannel.Connection#Connection(InputStream, String, Iterable, OptionalLong)
     */
    @Test
    public void testParseRange() {
        FileCacheByteChannel.Connection c;
        c = new FileCacheByteChannel.Connection(null, "bytes 25000-75000/100000", List.of("bytes"), OptionalLong.empty());
        assertEquals( 25000, c.start);
        assertEquals( 75000, c.end);
        assertEquals(100000, c.length);

        c = new FileCacheByteChannel.Connection(null, "bytes 25000-75000", List.of("bytes"), OptionalLong.empty());
        assertEquals( 25000, c.start);
        assertEquals( 75000, c.end);
        assertEquals(    -1, c.length);

        c = new FileCacheByteChannel.Connection(null, "bytes 25000/100000", List.of("bytes"), OptionalLong.empty());
        assertEquals( 25000, c.start);
        assertEquals(100000, c.end);
        assertEquals(100000, c.length);

        // Not legal, but we test robustness.
        c = new FileCacheByteChannel.Connection(null, "25000", List.of("bytes"), OptionalLong.empty());
        assertEquals( 25000, c.start);
        assertEquals(    -1, c.end);
        assertEquals(    -1, c.length);
    }
}