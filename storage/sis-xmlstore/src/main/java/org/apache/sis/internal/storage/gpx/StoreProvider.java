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
package org.apache.sis.internal.storage.gpx;

import java.net.URI;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.internal.storage.Capability;
import org.apache.sis.internal.storage.Capabilities;
import org.apache.sis.internal.storage.xml.stream.StaxDataStoreProvider;
import org.apache.sis.measure.Range;
import org.apache.sis.parameter.ParameterBuilder;
import static org.apache.sis.storage.DataStoreProvider.LOCATION;
import org.apache.sis.util.Version;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;


/**
 * The provider of {@link Store} instances. Given a {@link StorageConnector} input,
 * this class tries to instantiate a GPX {@code Store}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
@Capabilities({Capability.READ, Capability.WRITE})
public final class StoreProvider extends StaxDataStoreProvider {
    /**
     * XML source.
     */
    static final ParameterDescriptor<URI> PARAM_LOCATION = new ParameterBuilder()
            .addName(LOCATION)
            .setRequired(true)
            .create(URI.class, null);

    static final ParameterDescriptorGroup OPEN_DESCRIPTOR =
            new ParameterBuilder().addName("GPX").createGroup(PARAM_LOCATION);
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
     * Creates a new GPX store provider.
     */
    public StoreProvider() {
        super("GPX", 4);
        types.put(Tags.NAMESPACE_V10, "application/gpx+xml");
        types.put(Tags.NAMESPACE_V11, "application/gpx+xml");
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
     * {@inheritDoc }
     */
    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return OPEN_DESCRIPTOR;
    }

    /**
     * Returns a GPX {@link Store} implementation associated with this provider.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @return a data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(StorageConnector connector) throws DataStoreException {
        return new Store(this, connector);
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
}
