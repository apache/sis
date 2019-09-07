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

import java.util.stream.Stream;
import org.apache.sis.internal.storage.AbstractFeatureSet;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.event.StoreListeners;

// Branch-dependent imports
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;


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
    private DefaultFeatureType resultType;

    /**
     * Creates a new set of features by filtering the given set using the given query.
     */
    FeatureSubset(final FeatureSet source, final SimpleQuery query) {
        super(source instanceof StoreListeners ? (StoreListeners) source : null);
        this.source = source;
        this.query = query;
    }

    /**
     * Returns a description of properties that are common to all features in this dataset.
     */
    @Override
    public synchronized DefaultFeatureType getType() throws DataStoreException {
        if (resultType == null) {
            final DefaultFeatureType type = source.getType();
            try {
                resultType = query.expectedType(type);
            } catch (IllegalArgumentException e) {
                throw new DataStoreContentException(Resources.forLocale(getLocale())
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
        return stream;
    }
}
