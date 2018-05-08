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

import java.util.List;
import java.util.stream.Stream;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.apache.sis.internal.feature.FeatureUtilities;
import org.apache.sis.internal.storage.AbstractFeatureSet;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.expression.Expression;


/**
 * The result of {@link SimpleQuery#execute(FeatureSet)} executed using Java {@link Stream} methods.
 * Queries executed by this class do not benefit from accelerations provided for example by databases.
 * This class should be used only as a fallback when the query can not be executed natively by
 * {@link FeatureSet#subset(Query)}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class FeatureSubset extends AbstractFeatureSet {
    /**
     * The set of feature instances to filter, sort or process.
     */
    private final FeatureSet source;

    /**
     * The query for filtering the source set of features.
     */
    private final SimpleQuery query;

    /**
     * The type of features in this set. May or may not be the same as {@link #source}.
     * This is computed when first needed.
     */
    private FeatureType resultType;

    /**
     * A description of this set of features, computed when first needed.
     */
    private DefaultMetadata metadata;

    /**
     * Creates a new set of features by filtering the given set using the given query.
     */
    FeatureSubset(final FeatureSet source, final SimpleQuery query) {
        super(source);
        this.source = source;
        this.query = query;
    }

    /**
     * Returns {@code null} since computing the envelope would be costly.
     */
    @Override
    public Envelope getEnvelope() {
        return null;
    }

    /**
     * Computes information about this resource.
     * Current implementation sets only the resource name.
     */
    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            final DefaultCitation citation = new DefaultCitation(getType().getName().toInternationalString());
            final DefaultDataIdentification identification = new DefaultDataIdentification();
            identification.setCitation(citation);

            final DefaultMetadata metadata = new DefaultMetadata();
            metadata.getIdentificationInfo().add(identification);
            metadata.freeze();

            this.metadata = metadata;
        }
        return metadata;
    }

    /**
     * Returns a description of properties that are common to all features in this dataset.
     */
    @Override
    public synchronized FeatureType getType() throws DataStoreException {
        if (resultType == null) {
            resultType = query.expectedType(source.getType());
        }
        return resultType;
    }

    /**
     * Returns a stream of all features contained in this dataset.
     */
    @Override
    public Stream<Feature> features(final boolean parallel) throws DataStoreException {
        Stream<Feature> stream = source.features(parallel);
        /*
         * Apply filter.
         */
        final Filter filter = query.getFilter();
        if (!Filter.INCLUDE.equals(filter)) {
            stream = stream.filter(filter::evaluate);
        }
        /*
         * Apply sorting.
         */
        final SortBy[] sortBy = query.getSortBy();
        if (sortBy.length > 0) {
            stream = stream.sorted(new SortByComparator(sortBy));
        }
        /*
         * Apply offset.
         */
        final long offset = query.getOffset();
        if (offset > 0) {
            stream = stream.skip(offset);
        }
        /*
         * Apply limit.
         */
        final long limit = query.getLimit();
        if (limit >= 0) {
            stream = stream.limit(limit);
        }
        /*
         * Transform feature instances.
         */
        final List<SimpleQuery.Column> columns = query.getColumns();
        if (columns != null) {
            final Expression[] expressions = new Expression[columns.size()];
            for (int i=0; i<expressions.length; i++) {
                expressions[i] = columns.get(i).expression;
            }
            final FeatureType type = getType();
            final String[] names = FeatureUtilities.getNames(type.getProperties(false));
            stream = stream.map(t -> {
                final Feature f = type.newInstance();
                for (int i=0; i < expressions.length; i++) {
                    f.setPropertyValue(names[i], expressions[i].evaluate(t));
                }
                return f;
            });
        }
        return stream;
    }
}
