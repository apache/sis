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
package org.apache.sis.internal.storage.query;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Query;
import org.apache.sis.storage.UnsupportedQueryException;
import org.apache.sis.storage.event.ChangeEvent;
import org.apache.sis.storage.event.ChangeListener;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.util.GenericName;

/**
 *
 * @author Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class SimpleQueryFeatureSet implements FeatureSet {

    private final FeatureSet source;
    private final SimpleQuery query;
    private FeatureType resultType;
    private DefaultMetadata metadata;

    public SimpleQueryFeatureSet(FeatureSet source, SimpleQuery query) {
        this.source = source;
        this.query = query;
    }

    @Override
    public Envelope getEnvelope() throws DataStoreException {
        return null;
    }

    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            final FeatureType type = getType();

            final DefaultDataIdentification identification = new DefaultDataIdentification();
            final NamedIdentifier identifier = new NamedIdentifier(type.getName());
            final DefaultCitation citation = new DefaultCitation(type.getName().toString());
            citation.setIdentifiers(Collections.singleton(identifier));
            identification.setCitation(citation);

            final DefaultMetadata metadata = new DefaultMetadata();
            metadata.setIdentificationInfo(Collections.singleton(identification));
            metadata.freeze();

            this.metadata = metadata;
        }
        return metadata;
    }

    @Override
    public synchronized FeatureType getType() throws DataStoreException {
        if (resultType == null) {
            resultType = SimpleQuery.expectedType(source.getType(), query);
        }
        return resultType;
    }

    @Override
    public FeatureSet subset(Query query) throws UnsupportedQueryException, DataStoreException {
        if (query instanceof SimpleQuery) {
            return SimpleQuery.executeOnCPU(this, (SimpleQuery) query);
        }
        return FeatureSet.super.subset(query);
    }

    @Override
    public Stream<Feature> features(boolean parallel) throws DataStoreException {

        Stream<Feature> stream = source.features(parallel);

        //apply filter
        final Filter filter = query.getFilter();
        if (!Filter.INCLUDE.equals(filter)) {
            stream.filter(filter::evaluate);
        }

        //apply sort by
        final SortBy[] sortBy = query.getSortBy();
        if (sortBy.length > 0) {
            stream = stream.sorted(new SortByComparator(sortBy));
        }

        //apply offset
        final long offset = query.getOffset();
        if (offset > 0) {
            stream = stream.skip(offset);
        }

        //apply limit
        final long limit = query.getLimit();
        if (limit >= 0) {
            stream = stream.limit(limit);
        }

        //transform feature
        final List<SimpleQuery.Column> columns = query.getColumns();
        if (columns != null) {
            final SimpleQuery.Column[] cols = columns.toArray(new SimpleQuery.Column[0]);
            final FeatureType type = getType();
            final String[] names = type.getProperties(false).stream()
                    .map(PropertyType::getName)
                    .map(GenericName::tip)
                    .map(Object::toString)
                    .collect(Collectors.toList())
                    .toArray(new String[0]);

            stream.map(new Function<Feature, Feature>() {
                @Override
                public Feature apply(Feature t) {
                    final Feature f = type.newInstance();
                    for (int i=0;i<cols.length;i++) {
                        f.setPropertyValue(names[i], cols[i].expression.evaluate(t));
                    }
                    return f;
                }
            });
        }

        return stream;
    }

    @Override
    public <T extends ChangeEvent> void addListener(ChangeListener<? super T> listener, Class<T> eventType) {
    }

    @Override
    public <T extends ChangeEvent> void removeListener(ChangeListener<? super T> listener, Class<T> eventType) {
    }

}
