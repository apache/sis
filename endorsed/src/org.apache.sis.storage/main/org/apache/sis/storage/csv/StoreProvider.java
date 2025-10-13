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
package org.apache.sis.storage.csv;

import java.util.logging.Logger;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataOptionKey;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.feature.FoliationRepresentation;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.StoreMetadata;
import org.apache.sis.storage.base.URIDataStoreProvider;
import org.apache.sis.storage.wkt.FirstKeywordPeek;


/**
 * The provider of {@link Store} instances. Given a {@link StorageConnector} input,
 * this class tries to instantiate a CSV {@code Store}.
 *
 * <h2>Thread safety</h2>
 * The same {@code StoreProvider} instance can be safely used by many threads without synchronization on
 * the part of the caller. However, the {@link Store} instances created by this factory are not thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@StoreMetadata(formatName    = StoreProvider.NAME,
               fileSuffixes  = "csv",
               capabilities  = Capability.READ,
               resourceTypes = FeatureSet.class)
public final class StoreProvider extends URIDataStoreProvider {
    /**
     * The format names for static features and moving features.
     */
    static final String NAME = "CSV", MOVING = "CSV-MF";

    /**
     * The logger used by CSV stores.
     *
     * @see #getLogger()
     */
    private static final Logger LOGGER = Logger.getLogger("org.apache.sis.storage.csv");

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
         * for the given keyword. This method changes the case to match the one used in the keywords map,
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
     * Description of the optional parameter for specifying whether the reader should assemble distinct CSV lines
     * into a single {@code Feature} instance forming a foliation. This is ignored if the CSV file does not seem
     * to contain moving features.
     */
    private static final ParameterDescriptor<FoliationRepresentation> FOLIATION;
    static {
        final ParameterBuilder builder = new ParameterBuilder();
        FOLIATION = builder.addName("foliation")
                .setDescription(Resources.formatInternational(Resources.Keys.FoliationRepresentation))
                .create(FoliationRepresentation.class, FoliationRepresentation.ASSEMBLED);
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
     * Returns the MIME type if the given storage appears to be supported by CSV {@link Store}.
     * A {@linkplain ProbeResult#isSupported() supported} status does not guarantee that reading
     * or writing will succeed, only that there appears to be a reasonable chance of success
     * based on a brief inspection of the file header.
     *
     * @return a {@linkplain ProbeResult#isSupported() supported} status with the MIME type
     *         if the given storage seems to be readable as a CSV file.
     * @throws DataStoreException if an I/O error occurred.
     */
    @Override
    public ProbeResult probeContent(final StorageConnector connector) throws DataStoreException {
        return Peek.INSTANCE.probeContent(this, connector);
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
        return new Store(this, connector);
    }

    /**
     * Returns a CSV {@link Store} implementation from the given parameters.
     *
     * @return a data store implementation associated with this provider for the given parameters.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final ParameterValueGroup parameters) throws DataStoreException {
        final StorageConnector connector = connector(this, parameters);
        final Parameters pg = Parameters.castOrWrap(parameters);
        connector.setOption(DataOptionKey.ENCODING, pg.getValue(ENCODING));
        connector.setOption(DataOptionKey.FOLIATION_REPRESENTATION, pg.getValue(FOLIATION));
        return new Store(this, connector);
    }

    /**
     * Invoked by {@link #getOpenParameters()} the first time that a parameter descriptor needs to be created.
     *
     * @return the parameters descriptor for CSV files.
     */
    @Override
    protected ParameterDescriptorGroup build(final ParameterBuilder builder) {
        return builder.createGroup(LOCATION_PARAM, METADATA_PARAM, ENCODING, FOLIATION);
    }

    /**
     * Returns the logger used by <abbr>CSV</abbr> stores.
     */
    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
