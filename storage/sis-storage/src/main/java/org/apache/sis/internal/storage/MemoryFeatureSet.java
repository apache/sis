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

import java.util.Collection;
import java.util.stream.Stream;
import org.apache.sis.storage.DataStore;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.WarningListeners;

// Branch-dependent imports
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;


/**
 * Set of features stored in memory. Features are specified at construction time.
 * Metadata can be specified by overriding {@link #createMetadata(MetadataBuilder)}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class MemoryFeatureSet extends AbstractFeatureSet {
    /**
     * The type specified at construction time and returned by {@link #getType()}.
     */
    private final DefaultFeatureType type;

    /**
     * The features specified at construction time, potentially as a modifiable collection.
     * For all features in this collection, {@link AbstractFeature#getType()} shall be {@link #type}.
     */
    private final Collection<AbstractFeature> features;

    /**
     * Creates a new set of features stored in memory. It is caller responsibility to ensure that
     * <code>{@linkplain AbstractFeature#getType()} == type</code> for all elements in the given collection
     * (this is not verified).
     *
     * @param listeners  the set of registered warning listeners for the data store, or {@code null} if none.
     * @param type       the type of all features in the given collection.
     * @param features   collection of stored features. This collection will not be copied.
     */
    public MemoryFeatureSet(final WarningListeners<DataStore> listeners,
                            final DefaultFeatureType type, final Collection<AbstractFeature> features)
    {
        super(listeners);
        ArgumentChecks.ensureNonNull("type",     type);
        ArgumentChecks.ensureNonNull("features", features);
        this.type     = type;
        this.features = features;
    }

    /**
     * Returns the type common to all feature instances in this set.
     *
     * @return a description of properties that are common to all features in this dataset.
     */
    @Override
    public DefaultFeatureType getType() {
        return type;
    }

    /**
     * Returns the number of features in this set.
     *
     * @return the number of features.
     */
    @Override
    protected Integer getFeatureCount() {
        return features.size();
    }

    /**
     * Returns a stream of all features contained in this dataset.
     *
     * @param  parallel  {@code true} for a parallel stream (if supported), or {@code false} for a sequential stream.
     * @return all features contained in this dataset.
     */
    @Override
    public Stream<AbstractFeature> features(final boolean parallel) {
        return parallel ? features.parallelStream() : features.stream();
    }
}
