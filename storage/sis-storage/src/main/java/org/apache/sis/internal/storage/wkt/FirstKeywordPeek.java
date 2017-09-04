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
package org.apache.sis.internal.storage.wkt;

import java.io.Reader;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.sis.internal.storage.io.IOUtilities;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.util.Characters;


/**
 * Inspects the type of a text file based on the first keyword.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public abstract class FirstKeywordPeek {
    /**
     * Return values for the {@link #isKeywordChar(int)} method.
     */
    protected static final int REJECT = 0, ACCEPT = 1, IGNORE = 2;

    /**
     * The read-ahead limit when reading a text from a {@link Reader}.
     * Should be no more than {@code StorageConnector.DEFAULT_BUFFER_SIZE / 2}.
     */
    static final int READ_AHEAD_LIMIT = 2048;

    /**
     * The comment character to ignore.
     */
    protected static final char COMMENT = '#';

    /**
     * Length of the longest keyword.
     */
    protected final int maxLength;

    /**
     * Creates a new provider.
     *
     * @param  maxLength  length of the longest keyword.
     */
    public FirstKeywordPeek(final int maxLength) {
        this.maxLength = maxLength;
    }

    /**
     * Returns the next character which is not a white space, or -1 if the end of stream is reached.
     * Exactly one of {@code buffer} and {@code reader} shall be non-null.
     */
    private static int nextAfterSpaces(final ByteBuffer buffer, final Reader reader) throws IOException {
        if (buffer != null) {
            while (buffer.hasRemaining()) {
                final char c = (char) buffer.get();
                if (!Character.isWhitespace(c)) {
                    return c;
                }
            }
            return -1;
        }
        int c;
        while ((c = IOUtilities.readCodePoint(reader)) >= 0) {
            if (!Character.isWhitespace(c)) break;
        }
        return c;
    }

    /**
     * Moves the buffer or stream until the character after the next end of line character.
     */
    private static void toEndOfLine(final ByteBuffer buffer, final Reader reader) throws IOException {
        int c;
        do {
            if (buffer != null) {
                if (!buffer.hasRemaining()) break;
                c = (char) buffer.get();
            } else {
                c = IOUtilities.readCodePoint(reader);
                if (c < 0) break;
            }
        } while (!Characters.isLineOrParagraphSeparator(c));
    }

    /**
     * Returns {@code true} if the given character is valid for a keyword.
     *
     * @param  c  the character to test.
     * @return {@link #ACCEPT} if the given character should be accepted,
     *         {@link #REJECT} if the character is not valid for the keyword, or
     *         {@link #IGNORE} if the character should be accepted but not stored.
     */
    protected int isKeywordChar(final int c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c == '_') ? ACCEPT : REJECT;
    }

    /**
     * Returns {@link ProbeResult#SUPPORTED} if the given storage appears to begin with an expected keyword
     * Returning {@code SUPPORTED} from this method does not guarantee that reading or writing will succeed,
     * only that there appears to be a reasonable chance of success based on a brief inspection of the storage
     * header.
     *
     * @param  connector information about the storage (URL, stream, JDBC connection, <i>etc</i>).
     * @return {@link ProbeResult#SUPPORTED} if the given storage seems to be readable.
     * @throws DataStoreException if an I/O or SQL error occurred.
     */
    @SuppressWarnings("null")
    public final ProbeResult probeContent(final StorageConnector connector) throws DataStoreException {
        char[] keyword = null;
        int pos = 0;
        try {
            final ByteBuffer buffer = connector.getStorageAs(ByteBuffer.class);
            final Reader reader;
            if (buffer != null) {
                buffer.mark();
                reader = null;
            } else {
                // User gave us explicitely a Reader (e.g. a StringReader wrapping a String instance).
                reader = connector.getStorageAs(Reader.class);
                if (reader == null) {
                    return ProbeResult.UNSUPPORTED_STORAGE;
                }
                reader.mark(READ_AHEAD_LIMIT);
            }
            /*
             * Ignore leading spaces and comments if any, then get a keyword no longer than 'maxLength'.
             * That keyword shall be followed by [ or (, ignoring whitespaces.
             */
            int c;
            while ((c = nextAfterSpaces(buffer, reader)) == COMMENT) {
                toEndOfLine(buffer, reader);
            }
            int s;
            if ((s = isKeywordChar(c)) >= ACCEPT) {
                keyword = new char[maxLength];
                do {
                    if (s == ACCEPT) {
                        if (pos >= keyword.length) {
                            pos = 0;                // Keyword too long.
                            break;
                        }
                        keyword[pos++] = (char) c;
                    }
                    c = (buffer == null) ? IOUtilities.readCodePoint(reader) : buffer.hasRemaining() ? (char) buffer.get() : -1;
                } while ((s = isKeywordChar(c)) >= ACCEPT);
                /*
                 * At this point we finished to read and store the keyword.
                 * Verify if the keyword is followed by a character that indicate a keyword end.
                 */
                if (Character.isWhitespace(c)) {
                    c = nextAfterSpaces(buffer, reader);
                }
                if (!isPostKeyword(c)) {
                    pos = 0;
                }
            }
            if (buffer != null) {
                buffer.reset();
            } else {
                reader.reset();
            }
            if (c < 0) {
                return ProbeResult.INSUFFICIENT_BYTES;
            }
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
        return forKeyword(keyword, pos);
    }

    /**
     * Returns {@code true} if the given first non-white character after the keyword is one of the expected characters.
     *
     * @param  c  the first non-white character after the keyword, or -1 if we reached the end of stream.
     * @return {@code true} if the given character is one of the expected post-keyword characters.
     */
    protected abstract boolean isPostKeyword(int c);

    /**
     * Returns the value to be returned by {@link #probeContent(StorageConnector)} for the given keyword.
     *
     * @param  keyword  the first keyword found in the input. May be {@code null} if {@code length} is zero.
     * @param  length   number of valid characters in {@code keyword}.
     * @return {@link ProbeResult#SUPPORTED} if the given storage seems to be readable.
     */
    protected abstract ProbeResult forKeyword(final char[] keyword, final int length);
}
