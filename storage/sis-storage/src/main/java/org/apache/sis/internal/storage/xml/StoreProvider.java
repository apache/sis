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

import org.apache.sis.xml.Namespaces;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.internal.jaxb.LegacyNamespaces;
import org.apache.sis.internal.storage.StoreMetadata;
import org.apache.sis.internal.storage.Capability;


/**
 * The provider of {@link Store} instances.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.4
 * @module
 */
@StoreMetadata(formatName   = StoreProvider.NAME,
               fileSuffixes = "xml",
               capabilities = Capability.READ)
public final class StoreProvider extends AbstractProvider {
    /**
     * The format name.
     */
    static final String NAME = "XML";

    /**
     * Creates a new provider.
     */
    public StoreProvider() {
        super(null);
        mimeForNameSpaces.put(      Namespaces.GML,       "application/gml+xml");
        mimeForNameSpaces.put(      Namespaces.CSW,       "application/vnd.ogc.csw_xml");
        mimeForNameSpaces.put(LegacyNamespaces.GMD,       "application/vnd.iso.19139+xml");
        mimeForNameSpaces.put(LegacyNamespaces.GMI,       "application/vnd.iso.19139+xml");
        mimeForNameSpaces.put(LegacyNamespaces.GMI_ALIAS, "application/vnd.iso.19139+xml");
        mimeForRootElements.put("MD_Metadata",            "application/vnd.iso.19139+xml");
        // More types to be added in future versions.
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
}
