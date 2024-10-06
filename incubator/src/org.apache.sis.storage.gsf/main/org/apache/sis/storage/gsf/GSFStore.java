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
package org.apache.sis.storage.gsf;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.base.URIDataStore;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GSFStore extends DataStore {
    /**
     * The {@link GSFStoreProvider#LOCATION} parameter value, or {@code null} if none.
     *
     * @see #getOpenParameters()
     */
    private final URI location;
    /**
     * Path of the file opened by this data store, or {@code null} if none.
     *
     * @see #getFileSet()
     */
    private final Path path;
    /**
     * A description of this resource as an unmodifiable metadata, or {@code null} if not yet computed.
     *
     * @see #getMetadata()
     */
    private Metadata metadata;

    GSFStore(GSFStoreProvider provider, StorageConnector connector) throws DataStoreException {
        super(provider, connector);
        path     = connector.getStorageAs(Path.class);
        location = connector.commit(URI.class, GSFStoreProvider.NAME);
    }

    /**
     * Returns the factory that created this {@code DataStore} instance.
     * The provider determines which <abbr>GSF</abbr> library is used.
     *
     * @return the factory that created this {@code DataStore} instance.
     */
    @Override
    public final GSFStoreProvider getProvider() {
        return (GSFStoreProvider) super.getProvider();
    }

    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        final Parameters param = Parameters.castOrWrap(URIDataStore.parameters(provider, location));
        return Optional.ofNullable(param);
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            final var builder = new MetadataBuilder();
            builder.addIdentifier(getIdentifier().orElse(null), MetadataBuilder.Scope.RESOURCE);
            // TODO: add more information.
            return builder.buildAndFreeze();
        }
        return metadata;
    }

    public GSFRecordReader openReader() throws DataStoreException {
        return new GSFRecordReader(this);
    }

    @Override
    public Optional<FileSet> getFileSet() throws DataStoreException {
        return Optional.of(new FileSet(path));
    }

    @Override
    public void close() throws DataStoreException {
    }

}
