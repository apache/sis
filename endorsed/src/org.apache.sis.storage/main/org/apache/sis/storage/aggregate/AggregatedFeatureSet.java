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
package org.apache.sis.storage.aggregate;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.AbstractFeatureSet;
import org.apache.sis.storage.base.MetadataBuilder;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
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
 */
abstract class AggregatedFeatureSet extends AbstractFeatureSet {
    /**
     * The envelope, computed when first needed and cached for reuse.
     *
     * @see #getEnvelope()
     */
    private ImmutableEnvelope envelope;

    /**
     * Whether {@link #envelope} has been computed. The result may still be null.
     */
    private boolean isEnvelopeComputed;

    /**
     * Creates a new aggregated feature set.
     *
     * @param  parent  the parent resource, or {@code null} if none.
     *         This is usually the {@link org.apache.sis.storage.DataStore} that created this resource.
     */
    protected AggregatedFeatureSet(final Resource parent) {
        super(parent);
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
     *
     * <p>If any source has an absent value, then this method stops the collect immediately and returns {@code false}.
     * The rational is that if at least one source has unknown location, providing a location based on other sources
     * may be misleading since they may be very far from the missing resource location.</p>
     *
     * @return {@code false} if the collect has been interrupted because an envelope is absent.
     */
    private boolean getEnvelopes(final List<Envelope> addTo) throws DataStoreException {
        for (final FeatureSet fs : dependencies()) {
            if (fs instanceof AggregatedFeatureSet) {
                if (!((AggregatedFeatureSet) fs).getEnvelopes(addTo)) {
                    return false;
                }
            } else {
                final Optional<Envelope> e = fs.getEnvelope();
                if (e.isEmpty()) return false;
                addTo.add(e.get());
            }
        }
        return true;
    }

    /**
     * Returns the union of the envelopes in all aggregated feature sets.
     * This method tries to find a CRS common to all feature sets.
     * If no common CRS can be found, then the envelope is absent.
     *
     * @return union of envelopes from all dependencies.
     * @throws DataStoreException if an error occurred while computing the envelope.
     */
    @Override
    public final Optional<Envelope> getEnvelope() throws DataStoreException {
        /*
         * This method is final because overriding it would invalidate the unwrapping
         * of other `AggregatedFeatureSet` instances. If we wish to allow overrides
         * in a future version, we would need to revisit `getEnvelopes(List)` first.
         */
        synchronized (getSynchronizationLock()) {
            if (!isEnvelopeComputed) {
                final List<Envelope> envelopes = new ArrayList<>();
                if (getEnvelopes(envelopes)) try {
                    envelope = ImmutableEnvelope.castOrCopy(Envelopes.union(envelopes.toArray(Envelope[]::new)));
                } catch (TransformException e) {
                    listeners.warning(e);
                }
                isEnvelopeComputed = true;
            }
            return Optional.ofNullable(envelope);
        }
    }

    /**
     * Invoked in a synchronized block the first time that {@code getMetadata()} is invoked.
     * The default implementation adds the information documented in the
     * {@linkplain AbstractFeatureSet#createMetadata() parent class},
     * then adds the dependencies as lineages.
     *
     * @return the newly created metadata, or {@code null} if unknown.
     * @throws DataStoreException if an error occurred while reading metadata from the data stores.
     */
    @Override
    protected Metadata createMetadata() throws DataStoreException {
        final MetadataBuilder metadata = new MetadataBuilder();
        metadata.addDefaultMetadata(this, listeners);
        for (final FeatureSet fs : dependencies()) {
            final FeatureType type = fs.getType();
            metadata.addSource(fs.getMetadata(), ScopeCode.FEATURE_TYPE,
                    (type == null) ? null : new CharSequence[] {type.getName().toInternationalString()});
        }
        return metadata.build();
    }

    /**
     * Clears any cache in this resource, forcing the data to be recomputed when needed again.
     * This method should be invoked if the data in underlying data store changed.
     */
    @Override
    protected void clearCache() {
        synchronized (getSynchronizationLock()) {
            isEnvelopeComputed = false;
            envelope = null;
            super.clearCache();
        }
    }
}
