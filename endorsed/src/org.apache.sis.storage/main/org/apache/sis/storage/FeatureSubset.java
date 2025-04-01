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
package org.apache.sis.storage;

import java.util.OptionalLong;
import java.util.stream.Stream;
import org.opengis.metadata.Metadata;
import org.apache.sis.storage.base.FeatureProjection;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.base.StoreUtilities;
import org.apache.sis.storage.internal.Resources;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.filter.Filter;
import org.apache.sis.pending.geoapi.filter.SortBy;


/**
 * The result of {@link FeatureQuery#execute(FeatureSet)} executed using Java {@link Stream} methods.
 * Queries executed by this class do not benefit from accelerations provided for example by databases.
 * This class should be used only as a fallback when the query cannot be executed natively by
 * {@link FeatureSet#subset(Query)}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class FeatureSubset extends AbstractFeatureSet {
    /**
     * The set of feature instances to filter, sort or process.
     */
    private final FeatureSet source;

    /**
     * The query for filtering the source set of features.
     */
    private final FeatureQuery query;

    /**
     * A function applying projections (with "projected" in the <abbr>SQL</abbr> database sense) of features.
     * This is computed together with {@link #resultType}. May stay {@code null} if there is no projection.
     */
    private FeatureProjection projection;

    /**
     * The type of features in this set. May or may not be the same as {@link #source}.
     * This is computed when first needed.
     */
    private DefaultFeatureType resultType;

    /**
     * Creates a new set of features by filtering the given set using the given query.
     * This given query is stored as-is (it is not cloned neither optimized).
     */
    FeatureSubset(final FeatureSet source, final FeatureQuery query) {
        super(source);
        this.source = source;
        this.query = query;
    }

    /**
     * Creates metadata about this subset.
     * It includes information about the complete feature set.
     */
    @Override
    protected Metadata createMetadata() throws DataStoreException {
        final var builder = new MetadataBuilder();
        builder.addDefaultMetadata(this, listeners);
        builder.addLineage(Resources.formatInternational(Resources.Keys.UnfilteredData));
        builder.addProcessDescription(Resources.formatInternational(Resources.Keys.SubsetQuery_1, StoreUtilities.getLabel(source)));
        builder.addSource(source.getMetadata());
        return builder.build();
    }

    /**
     * Returns a description of properties that are common to all features in this dataset.
     */
    @Override
    public synchronized DefaultFeatureType getType() throws DataStoreException {
        if (resultType == null) {
            final DefaultFeatureType type = source.getType();
            try {
                projection = FeatureProjection.create(type, query.getProjection());
                resultType = (projection != null) ? projection.featureType : type;
            } catch (IllegalArgumentException e) {
                throw new DataStoreContentException(Resources.forLocale(listeners.getLocale())
                        .getString(Resources.Keys.CanNotDeriveTypeFromFeature_1, type.getName()), e);
            }
        }
        return resultType;
    }

    /**
     * Returns a stream of all features contained in this dataset.
     */
    @Override
    public Stream<AbstractFeature> features(final boolean parallel) throws DataStoreException {
        Stream<AbstractFeature> stream = source.features(parallel);
        /*
         * Apply filter.
         */
        final Filter<? super AbstractFeature> selection = query.getSelection();
        if (selection != null && !selection.equals(Filter.include())) {
            stream = stream.filter(selection);
        }
        /*
         * Apply sorting.
         */
        final SortBy<AbstractFeature> sortBy = query.getSortBy();
        if (sortBy != null) {
            stream = stream.sorted(sortBy);
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
        final OptionalLong limit = query.getLimit();
        if (limit.isPresent()) {
            stream = stream.limit(limit.getAsLong());
        }
        /*
         * Transform feature instances, usually for keeping only a subset of the properties.
         * Note: "projection" here is in relational database sense (SQL), not map projection.
         * This operation should be last, because the filter applied above may need properties
         * that are excluded by this projection.
         */
        getType();      // Force the computation of `projection` if not already done.
        if (projection != null) {
            stream = stream.map(projection);
        }
        return stream;
    }
}
