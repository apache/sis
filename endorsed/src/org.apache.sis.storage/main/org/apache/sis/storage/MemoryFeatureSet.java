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

import java.util.Objects;
import java.util.Collection;
import java.util.OptionalLong;
import java.util.stream.Stream;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;


/**
 * Set of feature instances stored in memory.
 * Features are specified at construction time.
 * Metadata can be specified by overriding the {@link #createMetadata()} method.
 *
 * <h2>When to use</h2>
 * This class is useful for small sets of features, or for testing purposes,
 * or when the features are in memory anyway (for example, a computation result).
 * It should generally not be used for large data sets read from files or databases.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @since 1.6
 */
public class MemoryFeatureSet extends AbstractFeatureSet {
    /**
     * The type specified at construction time.
     *
     * @see #getType()
     */
    protected final FeatureType type;

    /**
     * The features specified at construction time, potentially as a modifiable collection.
     * For all features in this collection, {@link Feature#getType()} shall be {@link #type}.
     *
     * @see #features(boolean)
     */
    protected final Collection<Feature> features;

    /**
     * Creates a new set of features stored in memory. It is caller responsibility to ensure that
     * <code>{@linkplain Feature#getType()} == type</code> for all elements in the given collection
     * (this is not verified by this constructor).
     *
     * @param parent    the parent resource, or {@code null} if none.
     * @param type      the type of all features in the given collection.
     * @param features  collection of stored features. This collection will not be copied.
     */
    public MemoryFeatureSet(final Resource parent, final FeatureType type, final Collection<Feature> features) {
        super(parent);
        this.type = Objects.requireNonNull(type);
        this.features = Objects.requireNonNull(features);
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
     * Returns the number of features in this set.
     *
     * @return the number of features.
     */
    @Override
    public OptionalLong getFeatureCount() {
        return OptionalLong.of(features.size());
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

    /**
     * Tests whether this memory feature set is wrapping the same feature instances as the given object.
     * This method checks also that the listeners are equal.
     *
     * @param  obj  the object to compare.
     * @return whether the two objects are memory resources wrapping the same features.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj != null && obj.getClass() == getClass()) {
            final var other = (MemoryFeatureSet) obj;
            return type.equals(other.type) &&
                   features.equals(other.features) &&
                   listeners.equals(other.listeners);
        }
        return false;
    }

    /**
     * Returns a hash code value for consistency with {@code equals(Object)}.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return type.hashCode() + 31 * features.hashCode() + 37 * listeners.hashCode();
    }
}
