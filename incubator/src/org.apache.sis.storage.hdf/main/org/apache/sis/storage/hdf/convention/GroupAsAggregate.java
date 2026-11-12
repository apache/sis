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
package org.apache.sis.storage.hdf.convention;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.sis.storage.base.StoreResource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.storage.hdf.HDF5Store;
import org.apache.sis.storage.hdf.internal.GroupNode;
import org.opengis.metadata.Metadata;
import org.opengis.util.GenericName;

/**
 * Decorate an HDF Group as an Aggregate.
 * The components are the resources decorating the group children, added by the store.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class GroupAsAggregate implements Aggregate, StoreResource{

    private final HDF5Store store;
    private final GroupNode group;
    private final List<Resource> components = new ArrayList<>();

    /**
     * Creates an aggregate view of the given group, initially without components.
     *
     * @param store store owning the group.
     * @param group group to decorate.
     */
    public GroupAsAggregate(HDF5Store store, GroupNode group) {
        this.store = store;
        this.group = group;
    }

    /**
     * Returns the modifiable list of components of this aggregate.
     *
     * @return the modifiable list of components of this aggregate
     */
    public List<Resource> resources() {
        return components;
    }

    @Override
    public Collection<? extends Resource> components() throws DataStoreException {
        return Collections.unmodifiableList(components);
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return group.getIdentifier();
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        return group.getMetadata();
    }

    @Override
    public <T extends StoreEvent> void addListener(Class<T> type, StoreListener<? super T> sl) {
    }

    @Override
    public <T extends StoreEvent> void removeListener(Class<T> type, StoreListener<? super T> sl) {
    }

    @Override
    public DataStore getOriginator() {
        return store;
    }

    @Override
    public Optional<FileSet> getFileSet() throws DataStoreException {
        return store.getFileSet();
    }
}
