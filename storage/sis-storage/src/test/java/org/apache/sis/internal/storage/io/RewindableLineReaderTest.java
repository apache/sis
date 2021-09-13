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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.imageio.stream.ImageInputStreamImpl;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link RewindableLineReader}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public final strictfp class RewindableLineReaderTest extends TestCase {
    /**
     * Number of bytes to transfer from the {@code ImageInputStream} to {@code BufferedReader}.
     * We use a small size in order to invalidate the {@code BufferedReader} mark without having
     * to fill all the buffer.
     */
    private static final int TRANSFERT_SIZE = 100;

    /**
     * Tests {@link RewindableLineReader#rewind()}.
     *
     * @throws IOException if an error occurred while reading characters.
     */
    @Test
    public void testRewind() throws IOException {
        RewindableLineReader reader = reader();
        reader.mark(TRANSFERT_SIZE);                    // Use a smaller limit for testing sooner mark invalidation.
        assertEquals("charAt(0)", 'A', reader.read());
        assertEquals("charAt(1)", 'B', reader.read());
        assertEquals("charAt(2)", 'C', reader.read());
        /*
         * Since we have read less than 100 characters, RewindableLineReader.rewind()
         * should be able to successfully delegate the work to BufferedReader.reset().
         */
        assertSame("BufferedReader.reset() should have succeeded.", reader, reader.rewind());
        assertEquals("charAt(0)", 'A', reader.read());
        assertEquals("charAt(1)", 'B', reader.read());
        assertEquals("charAt(2)", 'C', reader.read());
        assertEquals("charAt(3)", 'D', reader.read());
        /*
         * Skip a number of characters greater than the current buffer content. It should cause BufferedReader to
         * invalidate the mark. As a result of failure to execute BufferedReader.reset(), the 'reader' variable
         * should get a new value. However while we expect the value to change, we do not require it since whether
         * BufferedReader.reset() succeeded or not depends on BufferedReader implementation.
         */
        reader.skip(2 * TRANSFERT_SIZE);
        assertEquals("charAt(…)", 'W', reader.read());
        final RewindableLineReader old = reader;
        reader = reader.rewind();                       // Should be a new instance, but this is not mandatory.
        if (reader != old) try {
            old.read();
            fail("Old reader should be closed.");
        } catch (IOException e) {
            // This is the expected exception.
        }
        assertEquals("charAt(0)", 'A', reader.read());
        assertEquals("charAt(1)", 'B', reader.read());
        assertEquals("charAt(2)", 'C', reader.read());
        assertEquals("charAt(3)", 'D', reader.read());
        reader.close();
    }

    /**
     * Returns a reader over a dummy sequence of characters. That reader returns letters
     * from A to Z, than restart that sequence an infinite number of times.
     */
    private static RewindableLineReader reader() throws IOException {
        return new RewindableLineReader(new InputStreamAdapter(new ImageInputStreamImpl() {
            /** Next byte to return, in A … Z range. */
            private char next = 'A';

            /** Returns the next byte in A … Z range. */
            @Override public int read() {
                final char c = next;
                if (++next > 'Z') {
                    next = 'A';
                }
                return c;
            }

            /**
             * Transfers at most {@value #TRANSFERT_SIZE} bytes. We put a limit in the number of bytes
             * to be transferred in order to cause {@link BufferedReader} to invalidate the mark sooner
             * than waiting that we have filled the buffer.
             */
            @Override public int read(final byte[] buffer, int offset, int length) {
                if (length > TRANSFERT_SIZE) {
                    length = TRANSFERT_SIZE;
                }
                for (int i=0; i<length; i++) {
                    buffer[offset++] = (byte) read();
                }
                return length;
            }

            /** The only seek allowed for this test should be at the beginning of stream. */
            @Override public void seek(final long pos) throws IOException {
                assertEquals("Should seek at origin.", 0, pos);
                next = 'A';
            }
        }), StandardCharsets.US_ASCII);
    }
}
