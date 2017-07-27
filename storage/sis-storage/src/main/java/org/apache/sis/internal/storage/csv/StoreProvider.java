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
package org.apache.sis.internal.storage.csv;

import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.internal.storage.Capability;
import org.apache.sis.internal.storage.Capabilities;
import org.apache.sis.internal.storage.wkt.FirstKeywordPeek;


/**
 * The provider of {@link Store} instances. Given a {@link StorageConnector} input,
 * this class tries to instantiate a CSV {@code Store}.
 *
 * <div class="section">Thread safety</div>
 * The same {@code StoreProvider} instance can be safely used by many threads without synchronization on
 * the part of the caller. However the {@link Store} instances created by this factory are not thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
@Capabilities(Capability.READ)
public final class StoreProvider extends DataStoreProvider {
    /**
     * The format names for static features and moving features.
     */
    static final String NAME = "CSV", MOVING = "CSV-MF";

    /**
     * The object to use for verifying if the first keyword is the expected one.
     */
    private static final class Peek extends FirstKeywordPeek {
        /**
         * The unique instance.
         */
        static final Peek INSTANCE = new Peek();

        /**
         * The expected keyword after spaces removal.
         */
        private static final String KEYWORD = "@stboundedby";

        /**
         * Creates a new instance.
         */
        private Peek() {
            super(KEYWORD.length());
        }

        /**
         * Returns whether the given character is valid for the keyword. This implementation accepts
         * {@code '@'} in addition of the alphanumeric characters accepted by the parent class.
         */
        @Override
        protected int isKeywordChar(final int c) {
            return (c == Store.METADATA) ? ACCEPT : super.isKeywordChar(c);
        }

        /**
         * Returns {@code true} if the given first non-white character after the keyword
         * is one of the expected characters.
         */
        @Override
        protected boolean isPostKeyword(final int c) {
            return c == Store.SEPARATOR;
        }

        /**
         * Returns the value to be returned by {@link StoreProvider#probeContent(StorageConnector)}
         * for the given WKT keyword. This method changes the case to match the one used in the keywords map,
         * then verify if the keyword that we found is one of the known WKT keywords. Keywords with the "CRS"
         * suffix are WKT 2 while keywords with the "CS" suffix are WKT 1.
         */
        @Override
        protected ProbeResult forKeyword(final char[] keyword, final int length) {
            if (length == maxLength && KEYWORD.equalsIgnoreCase(new String(keyword))) {
                return ProbeResult.SUPPORTED;
            }
            return ProbeResult.UNSUPPORTED_STORAGE;
        }
    }

    /**
     * Creates a new provider.
     */
    public StoreProvider() {
    }

    /**
     * Returns a generic name for this data store, used mostly in warnings or error messages.
     *
     * @return a short name or abbreviation for the data format.
     */
    @Override
    public String getShortName() {
        return NAME;
    }

    /**
     * Returns {@link ProbeResult#SUPPORTED} if the given storage appears to be supported by CSV {@link Store}.
     * Returning {@code SUPPORTED} from this method does not guarantee that reading or writing will succeed, only
     * that there appears to be a reasonable chance of success based on a brief inspection of the storage header.
     *
     * @return {@link ProbeResult#SUPPORTED} if the given storage seems to be readable as a CSV file.
     * @throws DataStoreException if an I/O or SQL error occurred.
     */
    @Override
    public ProbeResult probeContent(final StorageConnector connector) throws DataStoreException {
        return new Peek().probeContent(connector);
    }

    /**
     * Returns a CSV {@link Store} implementation associated with this provider.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @return a data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final StorageConnector connector) throws DataStoreException {
        return new Store(this, connector, false);
    }
}
