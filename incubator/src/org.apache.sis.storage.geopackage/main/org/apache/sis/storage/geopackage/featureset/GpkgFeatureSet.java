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
package org.apache.sis.storage.geopackage.featureset;

import java.util.Optional;
import java.util.stream.Stream;
import org.apache.sis.storage.AbstractResource;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.storage.geopackage.GpkgContentHandler;
import org.apache.sis.storage.geopackage.GpkgContentResource;
import org.apache.sis.storage.geopackage.GpkgStore;
import org.apache.sis.storage.geopackage.privy.Record;
import org.apache.sis.storage.sql.ResourceDefinition;
import org.apache.sis.storage.sql.SQLStore;
import org.apache.sis.storage.sql.SQLStoreProvider;
import org.apache.sis.util.iso.Names;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.util.GenericName;

/**
 * TODO, this class in incomplete and rely on Apache SIS metatable analyze.
 *
 * @author Johann Sorel (Geomatys)
 */
final class GpkgFeatureSet extends AbstractResource implements FeatureSet, GpkgContentResource {

    private final GpkgFeatureSetHandler handler;
    private final GpkgStore store;
    private final Record.Content row;
    private final NamedIdentifier identifier;

    private FeatureSet sqlSet;

    GpkgFeatureSet(GpkgFeatureSetHandler handler, GpkgStore store, Record.Content row) {
        super(null, false);
        this.handler = handler;
        this.store = store;
        this.row = row;
        this.identifier = NamedIdentifier.castOrCopy(Names.createLocalName(null, null, row.identifier));
    }

    @Override
    public StoreListeners listeners() {
        return listeners;
    }

    @Override
    public GpkgStore getStore() {
        return store;
    }

    @Override
    public Record.Content getRecord() {
        return row;
    }

    @Override
    public GpkgContentHandler getHandler() {
        return handler;
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.of(identifier);
    }

    private synchronized FeatureSet getSQLStore() throws DataStoreException {

        final StorageConnector connector = new StorageConnector(store.getDataSource());
        final ResourceDefinition table = ResourceDefinition.table(null, null, row.tableName);

        final SQLStore sqlStore = new SQLStore(new SQLStoreProvider(), connector, table);
        sqlSet = sqlStore.components().iterator().next();
        return sqlSet;
    }

    @Override
    public FeatureType getType() throws DataStoreException {
        return getSQLStore().getType();
    }

    @Override
    public Stream<Feature> features(boolean parallel) throws DataStoreException {
        return getSQLStore().features(parallel);
    }

}
