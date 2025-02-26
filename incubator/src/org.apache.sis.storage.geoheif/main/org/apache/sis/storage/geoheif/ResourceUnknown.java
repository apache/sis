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

import java.util.Optional;
import org.opengis.util.GenericName;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.base.StoreResource;
import org.apache.sis.util.iso.Names;


/**
 * Unknown item type.
 * We display those items as Resource even if we have only minimal support.
 *
 * @author Johann Sorel (Geomatys)
 */
final class ResourceUnknown extends AbstractResource implements StoreResource {

    private final GeoHeifStore store;
    private final Image item;
    private final GenericName identifier;

    public ResourceUnknown(GeoHeifStore store, Image item) throws DataStoreException {
        super(store);
        this.store = store;
        this.item = item;

        if (item.entry.itemName == null || item.entry.itemName.isBlank()) {
            this.identifier = Names.createLocalName(null, null, Integer.toString(item.entry.itemId));
        } else {
            this.identifier = Names.createLocalName(null, null, item.entry.itemName);
        }
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.of(identifier);
    }

    @Override
    public DataStore getOriginator() {
        return store;
    }

}
