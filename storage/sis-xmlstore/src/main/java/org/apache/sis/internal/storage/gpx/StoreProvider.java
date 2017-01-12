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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.internal.storage.xml.stream.StaxDataStoreProvider;


/**
 * The provider of {@link Store} instances. Given a {@link StorageConnector} input,
 * this class tries to instantiate a GPX {@code Store}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public final class StoreProvider extends StaxDataStoreProvider {
    /**
     * Creates a new GPX store provider.
     */
    public StoreProvider() {
        super(4);
        types.put(Tags.NAMESPACE_V10, "application/gpx+xml");
        types.put(Tags.NAMESPACE_V11, "application/gpx+xml");
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
