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
package org.apache.sis.storage.wkt;

import java.util.Set;
import java.util.logging.Logger;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.StoreMetadata;
import org.apache.sis.storage.base.URIDataStoreOption;
import org.apache.sis.storage.base.URIDataStoreProvider;
import org.apache.sis.referencing.internal.shared.WKTKeywords;
import org.apache.sis.util.Version;


/**
 * The provider of <abbr>WKT</abbr> {@link Store} instances.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@StoreMetadata(formatName   = StoreProvider.NAME,
               fileSuffixes = "prj",
               capabilities = Capability.READ)
public final class StoreProvider extends URIDataStoreProvider {
    /**
     * The format name.
     */
    public static final String NAME = "WKT";

    /**
     * The logger used by WKT stores.
     *
     * @see #getLogger()
     */
    private static final Logger LOGGER = Logger.getLogger("org.apache.sis.storage.wkt");

    /**
     * The object to use for verifying if the first keyword is a WKT one.
     * This object contains the set of recognized WKT keywords.
     */
    static final class Peek extends FirstKeywordPeek {
        /**
         * The unique instance.
         */
        static final Peek INSTANCE = new Peek();

        /**
         * Length of the shortest keyword.
         */
        static final int MIN_LENGTH = 6;

        /**
         * The set of <abbr>WKT</abbr> keywords for <abbr>CRS</abbr> definitions.
         * This set does not include the <abbr>WKT</abbr> keywords for coordinate operations,
         * because the <abbr>WKT</abbr> store can only return metadata and metadata can only
         * store the <abbr>CRS</abbr>s.
         */
        final Set<String> keywords;

        /**
         * Creates the unique instance.
         */
        private Peek() {
            super(14);
            keywords = Set.of(
                    WKTKeywords.GeodeticCRS,
                    WKTKeywords.GeodCRS,
                    WKTKeywords.GeogCS,
                    WKTKeywords.GeocCS,
                    WKTKeywords.VerticalCRS,
                    WKTKeywords.VertCRS,
                    WKTKeywords.Vert_CS,
                    WKTKeywords.TimeCRS,
                    WKTKeywords.ImageCRS,
                    WKTKeywords.EngineeringCRS,
                    WKTKeywords.EngCRS,
                    WKTKeywords.Local_CS,
                    WKTKeywords.CompoundCRS,
                    WKTKeywords.Compd_CS,
                    WKTKeywords.ProjectedCRS,
                    WKTKeywords.ProjCRS,
                    WKTKeywords.ProjCS,
                    WKTKeywords.Fitted_CS,
                    WKTKeywords.BoundCRS);
        }

        /**
         * Returns {@code true} if the given first non-white character after the keyword
         * is one of the expected characters.
         */
        @Override
        protected boolean isPostKeyword(final int c) {
            return c == '[' || c == '(';
        }

        /**
         * Returns the value to be returned by {@link StoreProvider#probeContent(StorageConnector)}
         * for the given WKT keyword. This method changes the case to match the one used in the keywords map,
         * then verify if the keyword that we found is one of the known WKT keywords. Keywords with the "CRS"
         * suffix are WKT 2 while keywords with the "CS" suffix are WKT 1.
         */
        @Override
        protected ProbeResult forKeyword(final char[] keyword, final int length) {
            if (length >= MIN_LENGTH) {
                int pos = length;
                int version = 1;
                keyword[    0] &= (char) ~0x20;         // Make upper-case (valid only for characters in the a-z range).
                keyword[--pos] &= (char) ~0x20;
                if ((keyword[--pos] &= (char) ~0x20) == 'R') {
                    keyword [--pos] &= (char) ~0x20;    // Make "CRS" suffix in upper case (otherwise, was "CS" suffix)
                    version = 2;
                }
                while (--pos != 0) {
                    if (keyword[pos] != '_') {
                        keyword[pos] |= 0x20;    // Make lower-case.
                    }
                }
                if (keywords.contains(String.valueOf(keyword, 0, length))) {
                    return new ProbeResult(true, null, Version.valueOf(version));
                }
            }
            return ProbeResult.UNSUPPORTED_STORAGE;
        }
    }

    /**
     * Creates a new provider.
     */
    public StoreProvider() {
        supportedOptions.add(URIDataStoreOption.METADATA);
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
     * Returns WKT version if the given storage appears to be supported by WKT {@link Store}.
     * A {@linkplain ProbeResult#isSupported() supported} status does not guarantee that reading
     * or writing will succeed, only that there appears to be a reasonable chance of success
     * based on a brief inspection of the file header.
     *
     * @return a {@linkplain ProbeResult#isSupported() supported} status with the WKT version
     *         if the given storage seems to be readable as a WKT string.
     * @throws DataStoreException if an I/O error occurred.
     */
    @Override
    public ProbeResult probeContent(final StorageConnector connector) throws DataStoreException {
        return Peek.INSTANCE.probeContent(this, connector);
    }

    /**
     * Returns a {@link Store} implementation associated with this provider.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @return a data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final StorageConnector connector) throws DataStoreException {
        return new Store(this, connector);
    }

    /**
     * Returns the logger used by <abbr>WKT</abbr> stores.
     */
    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
