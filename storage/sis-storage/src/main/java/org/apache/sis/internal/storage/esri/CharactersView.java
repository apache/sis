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
package org.apache.sis.internal.storage.esri;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.io.IOException;
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.apache.sis.internal.jdk9.JDK9;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.internal.storage.io.ChannelDataInput;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.util.resources.Errors;


/**
 * Character sequences as a view over a buffer of bytes interpreted as US-ASCII characters.
 * The character sequence starts always at zero and its length is the buffer limit.
 * The intent is to allow the use of {@link Integer#parseInt(CharSequence, int, int, int)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class CharactersView implements CharSequence {
    /**
     * The space character used as sample value separator.
     */
    private static final char SPACE = ' ';

    /**
     * The object to use for reading data, or {@code null} if unavailable.
     * This is null during {@linkplain AsciiGridStoreProvider#probeContent probe} operation.
     * Shall never be null when this instance is the {@link AsciiGridStore#input} instance.
     */
    final ChannelDataInput input;

    /**
     * The buffer of bytes to wrap. This is the same reference as {@link ChannelDataInput#buffer},
     * copied here because frequently used.
     */
    private final ByteBuffer buffer;

    /**
     * The buffer array if the buffer is allocated on the heap,
     * or a temporary array of arbitrary length otherwise.
     */
    private final byte[] array;

    /**
     * Whether {@link #array} is the array backing the {@link #buffer}.
     * If {@code true}, then we do not need some copy operations.
     */
    private final boolean direct;

    /**
     * Creates a new sequence of characters.
     *
     * @param  input   the source of bytes, or {@code null} if unavailable.
     * @param  buffer  {@code input.buffer} or a standalone buffer if {@code input} is null.
     */
    CharactersView(final ChannelDataInput input, final ByteBuffer buffer) {
        this.input  = input;
        this.buffer = buffer;
        this.direct = buffer.hasArray();
        this.array  = direct ? buffer.array() : new byte[80];
    }

    /**
     * Returns the number of bytes in the buffer.
     */
    @Override
    public int length() {
        return buffer.limit();
    }

    /**
     * Returns the bytes at the given index, converted to a character.
     */
    @Override
    public char charAt(final int index) {
        return (char) Byte.toUnsignedInt(buffer.get(index));
    }

    /**
     * Read all (key, value) pairs from the header. All keys are converted to upper-case letters.
     * The map may contain null values if a key was declared with no value.
     *
     * @return the (key, value) pairs, with keys in upper-case letters.
     * @throws IOException if an error occurred while reading the header.
     * @throws DataStoreContentException if a duplicated key is found.
     */
    final Map<String,String> readHeader() throws IOException, DataStoreContentException {
        final Map<String,String> header = new HashMap<>();
        for (;;) {
            String key = readToken();           // Never empty.
            final char c = key.charAt(0);
            if (c != '#') {
                if (!Character.isJavaIdentifierStart(c)) {
                    buffer.position(buffer.position() - key.length() - 1);
                    return header;
                }
                String value = null;
                while (!skipLine(true)) {
                    buffer.position(buffer.position() - 1);
                    final String next = readToken();
                    if (value == null) value = next;
                    else value = value + SPACE + next;
                }
                key = key.toUpperCase(Locale.US);
                final String old = header.put(key, value);
                if (old != null && !old.equals(value)) {
                    if (value == null) header.put(key, old);
                    else throw new DataStoreContentException(Errors.format(Errors.Keys.DuplicatedElement_1, key));
                }
            }
            skipLine(false);
        }
    }

    /**
     * Reads the next byte as an unsigned value.
     */
    private int readByte() throws IOException {
        if (!buffer.hasRemaining()) {
            if (input == null) {
                throw new EOFException();
            }
            input.ensureBufferContains(Byte.BYTES);
        }
        return Byte.toUnsignedInt(buffer.get());
    }

    /**
     * Skips all character until the end of line.
     * This is used for skipping a comment line in the header.
     * This method can be invoked after {@link #readToken()}.
     *
     * @param  stopAtToken  whether to stop at the first non-white character.
     * @return whether end of line has been reached.
     * @throws EOFException if the channel has reached the end of stream.
     * @throws IOException if an other kind of error occurred while reading.
     */
    private boolean skipLine(final boolean stopAtToken) throws IOException {
        buffer.position(buffer.position() - 1);     // For checking if the space that we skipped was CR/LF.
        boolean eol = false;
        int c;
        do {
            c = readByte();
            eol = (c == '\r' || c == '\n');
        }
        while (!(eol || (stopAtToken && c > SPACE)));
        return eol;
    }

    /**
     * Skips leading white spaces, carriage returns or control characters, then reads and returns the next
     * sequence of non-white characters. After this method call, the buffer position is on the first white
     * character after the token.
     *
     * @return the next token, never empty and without leading or trailing white spaces.
     * @throws EOFException if the channel has reached the end of stream.
     * @throws IOException if an other kind of error occurred while reading.
     * @throws DataStoreContentException if the content does not seem to comply with ASCII Grid format.
     */
    @SuppressWarnings("empty-statement")
    final String readToken() throws IOException, DataStoreContentException {
        while (readByte() <= SPACE);
        int start = buffer.position() - 1;
        int c;
        do {
            if (!buffer.hasRemaining()) {
                if (input == null) {
                    throw new EOFException();
                }
                buffer.position(start);
                final int current = buffer.limit() - start;
                if (current >= buffer.capacity()) {
                    throw new DataStoreContentException(Resources.format(Resources.Keys.ExcessiveHeaderSize_1, input.filename));
                }
                input.ensureBufferContains(current + 1);
                buffer.position(current);
                start = 0;
            }
            c = Byte.toUnsignedInt(buffer.get());
        } while (c > SPACE);
        return subSequence(start, buffer.position() - 1);
    }

    /**
     * Returns a copy of the buffer content over the given range of bytes.
     * This method should be invoked only for small ranges (e.g. less than 80 characters).
     * We use it for parsing floating point numbers.
     *
     * @param  start  the start index, inclusive.
     * @param  end    the end index, exclusive.
     * @return the specified subsequence
     */
    @Override
    public String subSequence(final int start, final int end) {
        final int length = end - start;
        if (direct) {
            return new String(array, start, length, StandardCharsets.US_ASCII);
        } else if (length <= array.length) {
            JDK9.get(buffer, start, array, 0, length);
            return new String(array, 0, length, StandardCharsets.US_ASCII);
        } else {
            final byte[] data = new byte[length];
            JDK9.get(buffer, start, data);
            return new String(data, StandardCharsets.US_ASCII);
        }
    }

    /**
     * Returns a string representation of the buffer content.
     * Note that it represents only a truncated view of the file content.
     */
    public String toString() {
        return subSequence(0, length());
    }
}
