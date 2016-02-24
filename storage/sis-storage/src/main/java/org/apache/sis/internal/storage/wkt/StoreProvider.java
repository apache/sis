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

import java.util.HashSet;
import java.util.Set;
import java.io.Reader;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.util.Version;


/**
 * The provider of WKT {@link Store} instances.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public class StoreProvider extends DataStoreProvider {
    /**
     * The {@value} MIME type.
     */
    public static final String MIME_TYPE = "application/wkt";

    /**
     * The read-ahead limit when reading the WKT from a {@link Reader}.
     */
    static final int READ_AHEAD_LIMIT = 2048;

    /**
     * Length of the shortest WKT keyword.
     */
    static final int MIN_LENGTH = 6;

    /**
     * Length of the longest WKT keyword.
     */
    static final int MAX_LENGTH = 14;

    /**
     * The set of WKT keywords.
     */
    private static final Set<String> KEYWORDS = keywords();
    static Set<String> keywords() {
        final Set<String> s = new HashSet<String>(22);
        s.add(WKTKeywords.GeodeticCRS);
        s.add(WKTKeywords.GeodCRS);
        s.add(WKTKeywords.GeogCS);
        s.add(WKTKeywords.GeocCS);
        s.add(WKTKeywords.VerticalCRS);
        s.add(WKTKeywords.VertCRS);
        s.add(WKTKeywords.Vert_CS);
        s.add(WKTKeywords.TimeCRS);
        s.add(WKTKeywords.ImageCRS);
        s.add(WKTKeywords.EngineeringCRS);
        s.add(WKTKeywords.EngCRS);
        s.add(WKTKeywords.Local_CS);
        s.add(WKTKeywords.CompoundCRS);
        s.add(WKTKeywords.Compd_CS);
        s.add(WKTKeywords.ProjectedCRS);
        s.add(WKTKeywords.ProjCRS);
        s.add(WKTKeywords.ProjCS);
        s.add(WKTKeywords.Fitted_CS);
        s.add(WKTKeywords.BoundCRS);
        return s;
    }

    /**
     * Creates a new provider.
     */
    public StoreProvider() {
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
        while ((c = reader.read()) >= 0) {
            if (!Character.isWhitespace(c)) break;
        }
        return c;
    }

    /**
     * Returns {@code true} if the given character is valid for a WKT keyword.
     */
    private static boolean isValidChar(final int c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c == '_');
    }

    /**
     * Returns {@link ProbeResult#SUPPORTED} if the given storage appears to be supported by WKT {@link Store}.
     * Returning {@code SUPPORTED} from this method does not guarantee that reading or writing will succeed,
     * only that there appears to be a reasonable chance of success based on a brief inspection of the storage
     * header.
     *
     * @return {@link ProbeResult#SUPPORTED} if the given storage seems to be readable as a WKT file.
     * @throws DataStoreException if an I/O or SQL error occurred.
     */
    @Override
    @SuppressWarnings("null")
    public ProbeResult probeContent(final StorageConnector storage) throws DataStoreException {
        char[] keyword = null;
        int pos = 0;
        try {
            final ByteBuffer buffer = storage.getStorageAs(ByteBuffer.class);
            final Reader reader;
            if (buffer != null) {
                buffer.mark();
                reader = null;
            } else {
                // User gave us explicitely a Reader (e.g. a StringReader wrapping a String instance).
                reader = storage.getStorageAs(Reader.class);
                if (reader == null) {
                    return ProbeResult.UNSUPPORTED_STORAGE;
                }
                reader.mark(READ_AHEAD_LIMIT);
            }
            /*
             * Ignore leading spaces if any, then get a keyword no longer than LENGTH.
             * That keyword shall be followed by [ or (, ignoring whitespaces.
             */
            int c = nextAfterSpaces(buffer, reader);
            if (isValidChar(c)) {
                keyword = new char[MAX_LENGTH];
                do {
                    if (pos >= MAX_LENGTH) {
                        keyword = null;             // Keyword too long.
                        break;
                    }
                    keyword[pos++] = (char) c;
                    c = (buffer == null) ? reader.read() : buffer.hasRemaining() ? (char) buffer.get() : -1;
                } while (isValidChar(c));
                if (Character.isWhitespace(c)) {
                    c = nextAfterSpaces(buffer, reader);
                }
                if (c != '[' && c != '(') {
                    keyword = null;
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
        /*
         * At this point we got the first keyword. Change the case to match the one used in the KEYWORDS map,
         * then verify if the keyword that we found is one of the known WKT keywords. Keywords with the "CRS"
         * suffix are WKT 2 while keywords with the "CS" suffix are WKT 1.
         */
        final int length = pos;
        if (pos >= MIN_LENGTH) {
            int version = 1;
            keyword[    0] &= ~0x20;         // Make upper-case (valid only for characters in the a-z range).
            keyword[--pos] &= ~0x20;
            if ((keyword[--pos] &= ~0x20) == 'R') {
                keyword[--pos] &= ~0x20;     // Make "CRS" suffix in upper case (otherwise, was "CS" suffix)
                version = 2;
            }
            while (--pos != 0) {
                if (keyword[pos] != '_') {
                    keyword[pos] |= 0x20;    // Make lower-case.
                }
            }
            if (KEYWORDS.contains(String.valueOf(keyword, 0, length))) {
                return new ProbeResult(true, MIME_TYPE, Version.valueOf(version));
            }
        }
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

    /**
     * Returns a {@link Store} implementation associated with this provider.
     *
     * @param  storage Information about the storage (URL, stream, <i>etc</i>).
     * @return A data store implementation associated with this provider for the given storage.
     * @throws DataStoreException If an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final StorageConnector storage) throws DataStoreException {
        return new Store(storage);
    }
}
