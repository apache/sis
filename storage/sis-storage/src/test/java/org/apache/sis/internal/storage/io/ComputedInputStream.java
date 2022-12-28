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
import java.io.InputStream;

import static org.junit.Assert.*;


/**
 * An input stream where each value is computed from the stream position.
 * The byte values are the 1, 2, 3, …, 100, -1, -2, -3, … -100 series repeated indefinitely.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.4
 */
final class ComputedInputStream extends InputStream {
    /**
     * Number of bytes in this stream.
     */
    private final int length;

    /**
     * The current stream position.
     */
    private int position;

    /**
     * Random value to be returned by {@link #available()}.
     */
    private int available;

    /**
     * Whether this input stream has been closed.
     */
    private boolean closed;

    /**
     * Generator of random numbers for controlling the behavior of this stream.
     */
    private final Random random;

    /**
     * Creates a new input stream of the given length.
     *
     * @param  start  position of the first byte to read.
     * @param  end    position after the last byte to read.
     * @param  rg     generator of random numbers for controlling the behavior of this stream.
     */
    ComputedInputStream(final int start, final int end, final Random rg) {
        assertTrue(start >= 0);
        assertTrue(start <= end);
        position = start;
        length   = end;
        random   = rg;
    }

    /**
     * Returns the value at the given position.
     *
     * @param  position  the stream position where to get a value.
     * @return value at the specified stream position.
     */
    static byte valueAt(final int position) {
        int i = (position % 200) + 1;
        if (i > 100) i = 100 - i;
        return (byte) i;
    }

    /**
     * Reads the next byte of data from the input stream.
     */
    @Override
    public int read() {
        assertFalse("closed", closed);
        if (available != 0) available--;
        return (position < length) ? Byte.toUnsignedInt(valueAt(position++)) : -1;
    }

    /**
     * Reads up to {@code length} bytes of data from the input stream into an array of bytes.
     * This method randomly read a smaller number of bytes.
     *
     * @param  bytes    the buffer into which the data is read.
     * @param  offseet  the start offset at which the data is written.
     * @param  count    the maximum number of bytes to read.
     * @return the total number of bytes read into the buffer, or {@code -1} on EOF.
     */
    @Override
    public int read(final byte[] bytes, int offset, int count) {
        assertFalse("closed", closed);
        assertNotNull(bytes);
        assertTrue("Negative count",  count  >= 0);
        assertTrue("Nagative offset", offset >= 0);
        assertTrue("Out of bounds", offset + count <= bytes.length);
        if (position >= length) {
            return -1;
        }
        if (count != 0) {
            final int end = Math.min(offset + random.nextInt(count) + 1, length);
            count = end - offset;
            while (offset < end) {
                bytes[offset++] = valueAt(position++);
            }
        }
        if (available >= count) {
            available -= count;
        } else {
            available = random.nextInt(100);
        }
        return count;
    }

    /**
     * Returns an estimate of the number of bytes that can be read without blocking.
     *
     * @return an estimate of the number of bytes available.
     */
    @Override
    public int available() {
        assertFalse("closed", closed);
        return available;
    }

    /**
     * Marks this input stream as closed.
     */
    @Override
    public void close() {
        closed = true;
    }
}
