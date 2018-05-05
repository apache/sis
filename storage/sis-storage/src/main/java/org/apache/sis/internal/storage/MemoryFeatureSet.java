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
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.storage.DataStore;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.WarningListeners;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.metadata.Metadata;


/**
 * Set of features stored in memory.
 * Metadata and features are specified at construction time.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class MemoryFeatureSet extends AbstractFeatureSet {
    /**
     * The metadata to be returned by {@link #getMetadata()}.
     */
    private final Metadata metadata;

    /**
     * The type specified at construction time and returned by {@link #getType()}.
     */
    private final FeatureType type;

    /**
     * The features specified at construction time, potentially as a modifiable collection.
     * For all features in this collection, {@link Feature#getType()} shall be {@link #type}.
     */
    private final Collection<Feature> features;

    /**
     * Creates a new set of features stored in memory. It is caller responsibility to ensure that
     * <code>{@linkplain Feature#getType()} == type</code> for all elements in the given collection
     * (this is not verified).
     *
     * @param listeners  the set of registered warning listeners for the data store, or {@code null} if none.
     * @param metadata   information about this resource, or {@code null} for inferring default metadata.
     * @param type       the type of all features in the given collection.
     * @param features   collection of stored features. This collection will not be copied.
     */
    public MemoryFeatureSet(final WarningListeners<DataStore> listeners, Metadata metadata,
                            final FeatureType type, final Collection<Feature> features)
    {
        super(listeners);
        ArgumentChecks.ensureNonNull("type",     type);
        ArgumentChecks.ensureNonNull("features", features);
        this.type     = type;
        this.features = features;
        if (metadata == null) {
            final DefaultDataIdentification identification = new DefaultDataIdentification();
            final DefaultCitation citation = new DefaultCitation(type.getName().toString());
            citation.getIdentifiers().add(new NamedIdentifier(type.getName()));
            identification.setCitation(citation);

            final DefaultMetadata md = new DefaultMetadata(null, null, identification);
            md.freeze();
            metadata = md;
        }
        this.metadata = metadata;
    }

    /**
     * Returns the metadata given or inferred at construction time.
     *
     * @return information about this resource.
     */
    @Override
    public Metadata getMetadata() {
        return metadata;
    }

    /**
     * Returns the type common to all feature instances in this set.
     *
     * @return a description of properties that are common to all features in this dataset.
     */
    @Override
    public FeatureType getType() {
        return type;
    }

    /**
     * Returns a stream of all features contained in this dataset.
     *
     * @param  parallel  {@code true} for a parallel stream (if supported), or {@code false} for a sequential stream.
     * @return all features contained in this dataset.
     */
    @Override
    public Stream<Feature> features(final boolean parallel) {
        return parallel ? features.parallelStream() : features.stream();
    }
}
