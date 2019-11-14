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
package org.apache.sis.internal.sql.feature;

import java.util.Optional;
import java.util.stream.Stream;

import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.sort.SortBy;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.util.GenericName;

import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;

/**
 * A {@link Table} feature set on which a query has been applied.
 * TODO: Override {@link #subset(Query)} method to allow stacking of SQL filter and sorting.
 */
public class TableSubset implements FeatureSet {

    final Table parent;
    final SortBy[] sorting;
    final CharSequence where;

    public TableSubset(Table parent, SortBy[] sorting, CharSequence where) {
        this.parent = parent;
        this.sorting = sorting;
        this.where = where;
    }

    @Override
    public FeatureType getType() throws DataStoreException {
        return parent.getType();
    }

    @Override
    public Stream<Feature> features(boolean parallel) throws DataStoreException {
        final Features.Builder builder = new Features.Builder(parent)
                .where(where)
                .sortBy(sorting);
        return new StreamSQL(builder, parent.source, parallel);
    }

    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        return parent.getEnvelope();
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.empty();
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        return parent.getMetadata();
    }

    @Override
    public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
        parent.addListener(eventType, listener);
    }

    @Override
    public <T extends StoreEvent> void removeListener(Class<T> eventType, StoreListener<? super T> listener) {
        parent.removeListener(eventType, listener);
    }
}
