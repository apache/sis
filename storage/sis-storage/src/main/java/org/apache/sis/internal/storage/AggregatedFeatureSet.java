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
package org.apache.sis.internal.storage;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.util.logging.WarningListeners;

// Branch-dependent imports
import org.opengis.feature.FeatureType;


/**
 * A feature set made from the aggregation of other feature sets. The features may be aggregated in different ways,
 * depending on the subclass. The aggregation may be all features from one set followed by all features from another set,
 * or it may be features of the two sets merged together in a way similar to SQL JOIN statement.
 *
 * <p>This class provides default implementations of {@link #getEnvelope()} and {@link #getMetadata()}.
 * Subclasses need to implement {@link #dependencies()}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
abstract class AggregatedFeatureSet extends AbstractFeatureSet {
    /**
     * Creates a new aggregated feature set.
     *
     * @param  listeners  the set of registered warning listeners for the data store, or {@code null} if none.
     */
    protected AggregatedFeatureSet(final WarningListeners<DataStore> listeners) {
        super(listeners);
    }

    /**
     * Returns all feature set used by this aggregation. This method is invoked for implementation of
     * {@link #getEnvelope()} and {@link #createMetadata(MetadataBuilder)}.
     *
     * @return all feature sets in this aggregation.
     */
    abstract Collection<FeatureSet> dependencies();

    /**
     * Adds the envelopes of the aggregated feature sets in the given list. If some of the feature sets
     * are themselves aggregated feature sets, then this method traverses them recursively. We compute
     * the union of all envelopes at once after we got all envelopes.
     */
    private void getEnvelopes(final List<Envelope> addTo) throws DataStoreException {
        for (final FeatureSet fs : dependencies()) {
            if (fs instanceof AggregatedFeatureSet) {
                ((AggregatedFeatureSet) fs).getEnvelopes(addTo);
            } else {
                // No need to filter null envelopes since Envelopes.union(…) will ignore them.
                addTo.add(fs.getEnvelope());
            }
        }
    }

    /**
     * Returns the union of the envelope in all aggregated feature set, or {@code null} if none.
     * This method tries to find a CRS common to all feature sets. If no common CRS can be found,
     * an exception is thrown.
     *
     * <div class="note"><b>Implementation note:</b>
     * the envelope is recomputed every time this method is invoked. The result is not cached because
     * the envelope of {@code FeatureSet} sources may change between invocations of this method.
     * The cost should not be excessive if the sources cache themselves their envelopes.</div>
     *
     * @return union of envelopes of both sides, or {@code null}.
     * @throws DataStoreException if an error occurred while computing the envelope.
     */
    @Override
    public Envelope getEnvelope() throws DataStoreException {
        final List<Envelope> envelopes = new ArrayList<>();
        getEnvelopes(envelopes);
        try {
            return Envelopes.union(envelopes.toArray(new Envelope[envelopes.size()]));
        } catch (TransformException e) {
            /*
             * Since Envelopes.union(…) tried to find a common CRS,
             * failure to transform an envelope may be important.
             */
            throw new DataStoreReferencingException(e);
        }
    }

    /**
     * Invoked the first time that {@link #getMetadata()} is invoked. The default implementation adds
     * the information documented in {@link AbstractFeatureSet#createMetadata(MetadataBuilder)}, then
     * adds the dependencies as lineages.
     *
     * @param  metadata  the builder where to set metadata properties.
     * @throws DataStoreException if an error occurred while reading metadata from the data stores.
     */
    @Override
    protected void createMetadata(final MetadataBuilder metadata) throws DataStoreException {
        super.createMetadata(metadata);
        for (final FeatureSet fs : dependencies()) {
            final FeatureType type = fs.getType();
            metadata.addSource(fs.getMetadata(), ScopeCode.FEATURE_TYPE,
                    (type == null) ? null : new CharSequence[] {type.getName().toInternationalString()});
        }
    }
}
