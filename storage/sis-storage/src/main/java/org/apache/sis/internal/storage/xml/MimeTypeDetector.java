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
package org.apache.sis.internal.storage.xml;

import java.util.Map;
import java.util.Arrays;
import java.io.IOException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.ProbeResult;


/**
 * Detects the MIME type of a XML document from the namespace of the root element.
 * This class does not support encoding: it will search only for US-ASCII characters.
 * It does not prevent usage with encodings like ISO-LATIN-1 or UTF-8, provided that
 * the characters in the [32 … 122] range (from space to 'z') are the same and can not
 * be used as part of a multi-byte character.
 *
 * <p>This class tries to implement a lightweight detection mechanism. We can not for instance
 * unmarshall the whole document with JAXB and look at the class of unmarshalled object, since
 * it would be way too heavy.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.4
 * @module
 */
abstract class MimeTypeDetector {
    /**
     * The mapping from XML namespace to MIME type.
     * This map shall be read-only, since we do not synchronize it.
     */
    private final Map<String,String> types;

    /**
     * The {@code "xmlns"} string as a sequence of bytes.
     */
    private static final byte[] XMLNS = {'x','m','l','n','s'};

    /**
     * The maximal US-ASCII value, inclusive.
     */
    private static final int MAX_ASCII = 126;

    /**
     * A buffer for reading a word from the XML document, assumed using US-ASCII characters.
     */
    private byte[] buffer = new byte[32];

    /**
     * Number of valid characters in {@link #buffer} string.
     */
    private int length;

    /**
     * Sets to {@code true} when {@link #read()} implementations reached the {@link java.nio.ByteBuffer} limit,
     * but the buffer has enough capacity for more bytes. In such case the {@link #probeContent()} method will
     * return {@link ProbeResult#INSUFFICIENT_BYTES}, which means that the method requests more bytes for
     * detecting the MIME type.
     *
     * @see ProbeResult#INSUFFICIENT_BYTES
     */
    boolean insufficientBytes;

    /**
     * Creates a new instance.
     *
     * @param  types  the mapping from XML namespaces to MIME type.
     */
    MimeTypeDetector(final Map<String,String> types) {
        this.types = types;
    }

    /**
     * Adds the given byte in the {@link #buffer}, increasing its capacity if needed.
     */
    private void remember(final int c) {
        if (length == buffer.length) {
            buffer = Arrays.copyOf(buffer, length*2);
        }
        buffer[length++] = (byte) c;
    }

    /**
     * Reads a single byte or character, or -1 if we reached the end of the stream portion that we are allowed
     * to read. We are typically not allowed to read the full stream because only a limited amount of bytes is
     * cached.
     *
     * @return the character, or -1 on EOF.
     * @throws IOException if an error occurred while reading the byte or character.
     */
    abstract int read() throws IOException;

    /**
     * Skips all bytes or characters up to {@code search}, then returns the character after it.
     * Characters inside quotes will be ignored.
     *
     * @param  search  the byte or character to skip.
     * @return the byte or character after {@code search}, or -1 on EOF.
     * @throws IOException if an error occurred while reading the bytes or characters.
     */
    private int readAfter(final int search) throws IOException {
        int c;
        boolean isQuote = false;
        while ((c = read()) >= 0) {
            if (c == '"') {
                isQuote = !isQuote;
            } else if (c == search && !isQuote) {
                return read();
            }
        }
        return -1;
    }

    /**
     * If the given character is a space, skip it and all following spaces.
     * Returns the first non-space character.
     *
     * <p>For the purpose of this method, a "space" is considered to be the {@code ' '} character
     * and all control characters (character below 32, which include tabulations and line feeds).
     * This is the same criterion than {@link String#trim()}, but does not include Unicode spaces.</p>
     *
     * @return the first non-space character, or -1 on EOF.
     * @throws IOException if an error occurred while reading the bytes or characters.
     */
    private int afterSpaces(int c) throws IOException {
        while (c <= ' ' && c >= 0) {
            c = read();
        }
        return c;
    }

    /**
     * Skips the spaces if any, then the given characters, then the spaces, then the given separator.
     * After this method class, the stream position is on the first character after the separator if
     * a match has been found, or after the first unknown character otherwise.
     *
     * @param  word       the word to search, as US-ASCII characters.
     * @param  n          number of valid characters in {@code word}.
     * @param  c          value of {@code afterSpaces(read())}.
     * @param  separator  the {@code ':'} or {@code '='} character.
     * @return 1 if a match is found, 0 if no match, or -1 on EOF.
     * @throws IOException if an error occurred while reading the bytes or characters.
     */
    private int matches(final byte[] word, final int n, int c, final char separator) throws IOException {
        if (c >= 0) {
            for (int i=0; i<n; i++) {
                if (c != word[i]) {
                    return (c >= 0) ? 0 : -1;
                }
                c = read();
            }
            c = afterSpaces(c);
            if (c >= 0) {
                return (c == separator) ? 1 : 0;
            }
        }
        return -1;
    }

    /**
     * Returns the MIME type, or {@code null} if unknown.
     *
     * @throws IOException if an error occurred while reading the bytes or characters.
     */
    final String getMimeType() throws IOException {
        if (readAfter('?') != '>') {
            return null;
        }
        /*
         * At this point, we skipped the "<?xml ...?>" header. Skip the "<!-- -->" comments (if any),
         * then find the first '<' character. For example:
         *
         *                          <?xml version="1.0" encoding="UTF-8"?>
         * (we are here)        →   <!-- Licensed to the Apache Software Foundation (ASF) -->
         *                          <gmd:MD_Metadata xmlns:gmd = "http://www.isotc211.org/2005/gmd"/>
         * (we want to go there)     ↑
         */
        int c;
        while ((c = readAfter('<')) == '!') {
            do {
                c = readAfter('-');
                while (c == '-') {
                    c = read();
                }
                if (c < 0) {
                    return null;
                }
            } while (c != '>');
        }
        /*
         * At this point, we are after the opening bracket of root element.
         * Skip spaces and read the prefix, which may be absent (we will detect that later).
         * For example if the line is "<gmd:MD_Metadata … >", we will remember "gmd" here.
         */
        c = afterSpaces(c);
        while (c > ' ' && c != ':') {
            if (c == '>' || c > MAX_ASCII) {
                return null;
            }
            remember(c);
            c = read();
        }
        /*
         * At this point, we got what we think is the prefix of the root element. If we find the ':' character,
         * we consider that this was indeed the prefix (e.g. "gmd" in "<gmd:MD_Metadata … >"). But if we do not
         * find the ':' character, then we will consider that the element is in the default namespace, in which
         * case there is no prefix (length == 0). Exemple: "<MD_Metadata xmlns = … >"
         */
        c = afterSpaces(c);
        if (c != ':') {
            length = 0;         // XML element in the default namespace: it has no prefix.
        } else {
            c = afterSpaces(read());
        }
        /*
         * Search for "xmlns" keyword, ignoring anything before it. If we find a prefix in the previous step,
         * we will require that "xmlns" is followed by ":prefix" where "prefix" is the prefix that we found.
         */
        while (true) {
            int m = matches(XMLNS, XMLNS.length, c, (length == 0) ? '=' : ':');
            if (m != 0) {
                if (m < 0) {
                    return null;                            // End of file.
                }
                if (length == 0) break;                     // Found match for default namespace.
                m = matches(buffer, length, afterSpaces(read()), '=');
                if (m != 0) {
                    if (m < 0) {
                        return null;                        // End of file.
                    }
                    break;                                  // Found match for prefix.
                }
            }
            // Skip everything up to the next space, and check again.
            c = afterSpaces(read());
            if (c < 0) return null;
        }
        /*
         * At this point, we found the "xmlns" attribute for the prefix of the root element.
         * Get the attribute value (i.e. the namespace).
         */
        length = 0;
        c = afterSpaces(read());
        if (c != '"') {
            return null;
        }
        c = afterSpaces(read());
        do {
            if (c < 0 || c > MAX_ASCII) {
                return null;
            }
            remember(c);
            c = read();
        } while (c != '"');
        /*
         * Done reading the "xmlns" attribute value.
         */
        return types.get(new String(buffer, 0, length, "US-ASCII"));
    }

    /**
     * Wraps the call to {@link #getMimeType()} for catching {@link IOException} and for
     * instantiating the {@link ProbeResult}.
     */
    final ProbeResult probeContent() throws DataStoreException {
        boolean isSupported = true;
        String mimeType;
        try {
            mimeType = getMimeType();
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
        if (mimeType == null) {
            if (insufficientBytes) {
                return ProbeResult.INSUFFICIENT_BYTES;
            }
            isSupported = false;
            mimeType = StoreProvider.MIME_TYPE;
        }
        return new ProbeResult(isSupported, mimeType, null);
    }
}
