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
package org.apache.sis.internal.storage.folder;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Collection;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.ReadOnlyStorageException;
import org.apache.sis.storage.Resource;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;

/**
 * A Folder store acts as an aggregate of multiple files in a single store.
 * each file will be tested and possibly opened by another store.
 * This approach allows to discover the content of a folder or archive without
 * testing each file one by one.
 *
 * @author Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public class FolderStore extends DataStore implements Aggregate {

    private final ParameterValueGroup params;
    private final FolderAggregate root;

    /**
     * Construct a new FolderStore from an URI.
     *
     * @param uri root folder path
     */
    public FolderStore(URI uri) {
        this(toParameters(uri));
    }

    /**
     * Construct a new FolderStore from parameters.
     *
     * @param params opening parameters, those must comply to the provider parameter
     *        definition {@link FolderProvider#PARAMETERS_DESCRIPTOR}
     */
    public FolderStore(ParameterValueGroup params) {
        this.params = params;
        final URI uri = URI.class.cast(params.parameter(DataStoreProvider.LOCATION).getValue());
        root = new FolderAggregate(this, null, Paths.get(uri));
    }

    private static ParameterValueGroup toParameters(final URI uri) {
        final Parameters params = Parameters.castOrWrap(FolderProvider.PARAMETERS_DESCRIPTOR.createValue());
        params.parameter(DataStoreProvider.LOCATION).setValue(uri);
        return params;
    }

    /**
     * {@inheritDoc }
     *
     * @return the factory that created this {@code DataStore} instance
     */
    @Override
    public DataStoreProvider getProvider() {
        return DataStores.providers().stream().filter(
                (DataStoreProvider t) -> t.getShortName().equals(FolderProvider.NAME)).findFirst().get();
    }

    /**
     * {@inheritDoc }
     *
     * @return parameters used for opening this {@code DataStore}, not null.
     */
    @Override
    public ParameterValueGroup getOpenParameters() {
        return params;
    }

    /**
     * @see FolderAggregate#getMetadata()
     *
     * @throws org.apache.sis.storage.DataStoreException
     */
    @Override
    public Metadata getMetadata() throws DataStoreException {
        return root.getMetadata();
    }

    /**
     * @see FolderAggregate#components()
     *
     * @throws org.apache.sis.storage.DataStoreException
     */
    @Override
    public Collection<Resource> components() throws DataStoreException {
        return root.components();
    }

    /**
     * @see FolderAggregate#add(org.apache.sis.storage.Resource)
     *
     * @throws org.apache.sis.storage.DataStoreException
     */
    @Override
    public org.apache.sis.storage.Resource add(org.apache.sis.storage.Resource resource) throws DataStoreException, ReadOnlyStorageException {
        return root.add(resource);
    }

    /**
     * @see FolderAggregate#remove(org.apache.sis.storage.Resource)
     *
     * @throws org.apache.sis.storage.DataStoreException
     */
    @Override
    public void remove(org.apache.sis.storage.Resource resource) throws DataStoreException, ReadOnlyStorageException {
        root.remove(resource);
    }

    /**
     * @see FolderAggregate#close()
     *
     * @throws org.apache.sis.storage.DataStoreException
     */
    @Override
    public void close() throws DataStoreException {
        root.close();
    }

}
