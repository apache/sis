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
package org.apache.sis.storage.geoheif;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.opengis.util.GenericName;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.isobmff.base.EntityToGroup;
import org.apache.sis.util.iso.Names;


/**
 * An unidentified group of entities.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class Group extends AbstractResource implements Aggregate {

    private final GeoHeifStore store;
    final EntityToGroup group;

    //cache linked resources
    private List<Resource> components;

    public Group(GeoHeifStore store, EntityToGroup group) {
        super(null);
        this.store = store;
        this.group = group;
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.of(Names.createLocalName(null, null, "EntityGroup " + group.groupId));
    }

    @Override
    public synchronized Collection<? extends Resource> components() throws DataStoreException {
        if (components != null) return components;

        components = new ArrayList<>(group.entitiesId.length);
        for (int entityId : group.entitiesId) {
            components.add(store.getComponent(entityId));
        }
        return components;
    }

}
