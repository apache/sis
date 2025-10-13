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
package org.apache.sis.storage.gpx;

import java.util.Map;
import java.util.logging.Logger;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.StoreMetadata;
import org.apache.sis.storage.xml.stream.StaxDataStoreProvider;
import org.apache.sis.measure.Range;
import org.apache.sis.util.Version;


/**
 * The provider of {@link Store} instances. Given a {@link StorageConnector} input,
 * this class tries to instantiate a GPX {@code Store}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
@StoreMetadata(formatName    = StoreProvider.NAME,
               fileSuffixes  = "xml",
               capabilities  = {Capability.READ, Capability.WRITE, Capability.CREATE},
               resourceTypes = {FeatureSet.class})
public final class StoreProvider extends StaxDataStoreProvider {
    /**
     * The format name.
     */
    public static final String NAME = "GPX";

    /**
     * The logger used by GPX stores.
     *
     * @see #getLogger()
     */
    private static final Logger LOGGER = Logger.getLogger("org.apache.sis.storage.gpx");

    /**
     * The "1.0" version.
     */
    static final Version V1_0 = Version.valueOf(1,0);

    /**
     * The "1.1" version.
     */
    static final Version V1_1 = Version.valueOf(1,1);

    /**
     * The range of versions returned by {@link #getSupportedVersions()}.
     */
    private static final Range<Version> VERSIONS = new Range<>(Version.class, V1_0, true, V1_1, true);

    /**
     * The default factory instance.
     */
    private static final StoreProvider INSTANCE = new StoreProvider();

    /**
     * Returns the default provider instance.
     * This method is invoked by Java service loader.
     */
    public static StoreProvider provider() {
        return INSTANCE;
    }

    /**
     * Creates a new GPX store provider.
     */
    public StoreProvider() {
        super("GPX",
              Map.of(Tags.NAMESPACE_V10, "application/gpx+xml",
                     Tags.NAMESPACE_V11, "application/gpx+xml"),
              Map.of("gpx",              "application/gpx+xml"));
    }

    /**
     * Returns the range of versions supported by the GPX data store.
     *
     * @return the range of supported versions.
     */
    @Override
    public Range<Version> getSupportedVersions() {
        return VERSIONS;
    }

    /**
     * Returns a GPX {@link Store} implementation associated with this provider.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @return a data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final StorageConnector connector) throws DataStoreException {
        if (isWritable(connector, false)) {
            return new WritableStore(this, connector);
        } else {
            return new Store(this, connector);
        }
    }

    /**
     * Returns the JAXB context for the data store. This method is invoked at most once.
     *
     * @return the JAXB context.
     * @throws JAXBException if an error occurred while creating the JAXB context.
     */
    @Override
    protected JAXBContext getJAXBContext() throws JAXBException {
        return JAXBContext.newInstance(Metadata.class);
    }

    /**
     * Returns the logger used by <abbr>GPX</abbr> stores.
     */
    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
