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
package org.apache.sis.storage.hdf.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Input stream returning a byte pattern repeated up to a given size.
 * Used for dataset regions which are not stored in the file and must be
 * filled with the fill value.
 *
 * @author Johann Sorel (Geomatys)
 */
public class ConstantInputStream extends InputStream {

    private static final byte[] ZEROS = new byte[4096];

    /** Total number of bytes of the stream. */
    protected final long size;
    /** Bytes repeated over the whole stream. */
    protected final byte[] pattern;
    /** Number of bytes already read or skipped. */
    protected long position = 0;

    private ConstantInputStream(long size, byte[] pattern) {
        this.size = size;
        this.pattern = pattern;
    }

    @Override
    public int read() throws IOException {
        if (position >= size) return -1;
        final byte b = pattern[(int) (position % pattern.length)];
        position++;
        return b;
    }

    @Override
    public void skipNBytes(long n) throws IOException {
        if (n <= 0) return;
        final long remaining = size - position;
        final long skipped = Math.min(remaining, n);
        position += skipped;
        if (skipped != n) {
            throw new EOFException();
        }
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) return 0;
        final long remaining = size - position;
        final long skipped = Math.min(remaining, n);
        position += skipped;
        return skipped;
    }

    @Override
    public int available() throws IOException {
        return Math.toIntExact(size - position);
    }

    /**
     * Returns a stream repeating the given pattern.
     * Patterns made of a single repeated byte use a faster implementation.
     *
     * @param size total number of bytes of the stream
     * @param pattern bytes to repeat, not empty
     * @return a stream repeating the given pattern
     */
    public static ConstantInputStream create(long size, byte[] pattern) {
        reduce:
        if (pattern.length > 1) {
            //simplify to a single byte pattern if possible
            byte r = pattern[0];
            for (int i = 1; i < pattern.length; i++) {
                if (pattern[i] != r) break reduce;
            }
            pattern = new byte[]{r};
        }

        if (pattern.length == 1) {
            return new SingleByte(size, pattern[0] == 0 ? ZEROS : pattern);
        } else {
            return new ConstantInputStream(size, pattern);
        }
    }

    private static final class SingleByte extends ConstantInputStream {

        private SingleByte(long size, byte[] pattern) {
            super(size, pattern);
        }

        @Override
        public int read() throws IOException {
            if (position >= size) return -1;
            position++;
            return pattern[0];
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            final long remaining = size - position;
            if (remaining <= 0) return -1;
            if (pattern == ZEROS) {
                final int r = (int) Math.min(len, ZEROS.length);
                System.arraycopy(ZEROS, 0, b, off, r);
                position += r;
                return r;
            } else {
                final int r = (int) Math.min(len, remaining);
                arrayFill(b, off, r, pattern[0]);
                position += r;
                return r;
            }
        }

        //https://stackoverflow.com/questions/9128737/fastest-way-to-set-all-values-of-an-array/25508988#25508988
        private static void arrayFill(byte[] array, int off, int len, byte value) {
            if (len > 0) {
                array[off] = value;
            }
            //Value of i will be [1, 2, 4, 8, 16, 32, ..., len]
            for (int i = 1; i < len; i += i) {
                final int range = len - i;
                System.arraycopy(array, off, array, off+i, (range < i) ? range : i);
            }
        }
    }
}
