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
package org.apache.sis.storage.earthobservation;

import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.internal.storage.Capability;
import org.apache.sis.internal.storage.StoreMetadata;
import org.apache.sis.internal.storage.URIDataStore;
import org.apache.sis.internal.storage.wkt.FirstKeywordPeek;


/**
 * The provider of {@link LandsatStore} instances. Given a {@link StorageConnector} input,
 * this class tries to instantiate a {@code LandsatStore}.
 *
 * <div class="section">Thread safety</div>
 * The same {@code LandsatStoreProvider} instance can be safely used by many threads without synchronization on
 * the part of the caller. However the {@link LandsatStore} instances created by this factory are not thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
@StoreMetadata(formatName   = LandsatStoreProvider.NAME,
               fileSuffixes = "txt",
               capabilities = Capability.READ)
public class LandsatStoreProvider extends DataStoreProvider {
    /**
     * The format name.
     */
    static final String NAME = "Landsat";

    /**
     * The parameter descriptor to be returned by {@link #getOpenParameters()}.
     */
    private static final ParameterDescriptorGroup OPEN_DESCRIPTOR = URIDataStore.Provider.descriptor(NAME);

    /**
     * The object to use for verifying if the first keyword is the expected one.
     */
    private static final class Peek extends FirstKeywordPeek {
        /**
         * The expected keyword after spaces removal.
         */
        private static final String KEYWORD = "GROUP=L1_METADATA_FILE";

        /**
         * The part in process of being parsed:
         * <ul>
         *   <li>{@code KEY} (0) for the {@code "GROUP"} keyword,</li>
         *   <li>{@code SEPARATOR} (1) for the {@code '='} symbol,</li>
         *   <li>{@code VALUE} (2) for the {@code "L1_METADATA_FILE"} value.</li>
         * </ul>
         */
        private static final int KEY = 0, SEPARATOR = 1, VALUE = 2;

        /**
         * {@value #SEPARATOR} if parsing the characters after the {@code =} symbol,
         * {@value #VALUE} if parsing the non-white characters after {@code =}.
         */
        private int part = KEY;

        /**
         * Creates a new instance.
         */
        Peek() {
            super(KEYWORD.length());
        }

        /**
         * Returns {@code ACCEPT} if the given character is valid for a keyword.
         */
        @Override
        protected int isKeywordChar(final int c) {
            final int s = super.isKeywordChar(c);
            if (s == REJECT) {
                if (c >= '0' && c <= '9' && part == VALUE) {
                    return ACCEPT;
                }
                switch (c) {
                    case '\t':
                    case ' ' : if (part < VALUE) {                  return IGNORE;} else break;
                    case '=' : if (part == KEY)  {part = SEPARATOR; return ACCEPT;} else break;
                }
            } else {
                if (part == SEPARATOR) {
                    part = VALUE;
                }
            }
            return s;
        }

        /**
         * Returns {@code true} if the given first non-white character after the keyword is one of the expected
         * characters. This implementation expects the first letter of the {@code "GROUP"} keyword for the first
         * sub-group (unless there is a comment line between them).
         */
        @Override
        protected boolean isPostKeyword(final int c) {
            return c == 'G' || c == 'g' || c == COMMENT;
        }

        /**
         * Returns the value to be returned by {@link LandsatStoreProvider#probeContent(StorageConnector)}
         * for the given keyword.
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
    public LandsatStoreProvider() {
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
     * Returns a description of all parameters accepted by this provider for opening a Landsat file.
     *
     * @return description of available parameters for opening a Landsat file.
     *
     * @since 0.8
     */
    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return OPEN_DESCRIPTOR;
    }

    /**
     * Returns {@link ProbeResult#SUPPORTED} if the given storage appears to be supported by {@link LandsatStore}.
     * Returning {@code SUPPORTED} from this method does not guarantee that reading or writing will succeed, only
     * that there appears to be a reasonable chance of success based on a brief inspection of the storage header.
     *
     * @return {@link ProbeResult#SUPPORTED} if the given storage seems to be readable as a Landsat file.
     * @throws DataStoreException if an I/O or SQL error occurred.
     */
    @Override
    public ProbeResult probeContent(final StorageConnector connector) throws DataStoreException {
        return new Peek().probeContent(connector);
    }

    /**
     * Returns a {@link LandsatStore} implementation associated with this provider.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @return a data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final StorageConnector connector) throws DataStoreException {
        return new LandsatStore(this, connector);
    }
}
