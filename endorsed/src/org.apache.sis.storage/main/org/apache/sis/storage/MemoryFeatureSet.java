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

import java.util.Set;
import java.util.HashSet;
import java.util.Objects;
import java.util.Collection;
import java.util.OptionalLong;
import java.util.stream.Stream;
import org.apache.sis.feature.Features;
import org.apache.sis.filter.Optimization;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.util.resources.Errors;

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
 * <h2>Mutability</h2>
 * By default, the feature collection given at construction time is assumed stable.
 * If the content of that collection is modified, then the {@link #refresh()} method
 * should be invoked for rebuilding the {@linkplain #allTypes set of feature types}.
 *
 * <h2>Thread-safety</h2>
 * This class is thread-safe if the collection given at construction time is thread-safe.
 * Synchronizations use the lock returned by {@link #getSynchronizationLock()}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @since 1.6
 */
public class MemoryFeatureSet extends AbstractFeatureSet {
    /**
     * The base type of all feature instances that the collection can contain.
     * All elements of {@link #allTypes} must be assignable to this base type.
     *
     * @see #getType()
     */
    protected final FeatureType baseType;

    /**
     * The types, including sub-types, of all feature instances found in the collection.
     * This is often a singleton containing only {@link #baseType}, but it may also be a
     * set without {@code baseType} if all features are instances of various subtypes.
     *
     * <p>This set is modifiable and is updated when {@link #refresh()} is invoked.
     * This set should always contains at least one element.</p>
     *
     * @see #refresh()
     * @see #prepareQueryOptimization(FeatureQuery, Optimization)
     */
    protected final Set<FeatureType> allTypes;

    /**
     * The features specified at construction time, potentially as a modifiable collection.
     * For all feature instances in this collection, the value returned by {@link Feature#getType()}
     * shall be {@link #baseType} or a subtype of {@code baseType}.
     *
     * @see #features(boolean)
     */
    protected final Collection<Feature> features;

    /**
     * Creates a new set of feature instances stored in memory.
     * The base feature type is determined automatically from the given collection of features.
     * The collection shall contain at least one element.
     *
     * @param  features  collection of stored features. This collection will not be copied.
     * @throws IllegalArgumentException if the given collection is empty or does not contain
     *         feature instances having a common parent type.
     */
    public MemoryFeatureSet(Collection<Feature> features) {
        this(null, null, features);
    }

    /**
     * Creates a new set of feature instances stored in memory with specified parent resource and base type.
     * This constructor verifies that all feature instances are assignable to a base type.
     * That base type can be either specified explicitly or inferred automatically.
     *
     * @param  parent    the parent resource, or {@code null} if none.
     * @param  baseType  the base type of all features in the given collection, or {@code null} for automatic.
     * @param  features  collection of stored features. This collection will not be copied.
     * @throws IllegalArgumentException if {@code baseType} is null and cannot be determined from the feature instances,
     *         or if some feature instances are not assignable to {@code baseType}.
     */
    public MemoryFeatureSet(final Resource parent, final FeatureType baseType, final Collection<Feature> features) {
        super(parent);
        this.features = Objects.requireNonNull(features);
        allTypes = new HashSet<>();
        if (baseType != null) {
            verifyFeatureInstances(baseType, true);
        } else {
            features.forEach((instance) -> allTypes.add(instance.getType()));
            if (allTypes.isEmpty()) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "features"));
            }
        }
        if ((this.baseType = Features.findCommonParent(allTypes)) == null) {
            throw new IllegalArgumentException(Resources.format(Resources.Keys.NoCommonFeatureType));
        }
    }

    /**
     * Scans all feature instances for building the set of feature types.
     * This method opportunistically verifies that all instances are assignable to {@code baseType}.
     *
     * @param baseType  tentative value of {@link #baseType}, may be provided before {@link #baseType} is set.
     * @param creating  whether this method is invoked from the constructor.
     */
    private void verifyFeatureInstances(final FeatureType baseType, final boolean creating) {
        for (final Feature feature : features) {
            final FeatureType type = feature.getType();
            if (allTypes.add(type) && !baseType.isAssignableFrom(type)) {
                allTypes.clear();
                String message = Resources.format(Resources.Keys.FeatureNotAssignableToBaseType_2, baseType.getName(), type.getName());
                throw creating ? new IllegalArgumentException(message) : new IllegalStateException(message);
            }
        }
        if (allTypes.isEmpty()) {
            allTypes.add(baseType);
        }
    }

    /**
     * Notifies this {@code FeatureSet} that the elements in the collection of features have changed.
     * This method re-verifies that all feature instances are assignable to the {@link #baseType} and
     * rebuilds the set of all feature types.
     *
     * @throws IllegalStateException if some features are not instances of {@link #baseType}.
     */
    public void refresh() {
        synchronized (getSynchronizationLock()) {
            allTypes.clear();
            verifyFeatureInstances(baseType, false);
        }
    }

    /**
     * Returns the type common to all feature instances in this set.
     * By default, this type is determined at construction time and does
     * not change even if the content of the feature collection is updated.
     *
     * @return a description of properties that are common to all features in this dataset.
     */
    @Override
    public FeatureType getType() {
        return baseType;
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
     * Configures the optimization of a query with information about the expected types of all feature instances.
     * This method is invoked indirectly when a {@linkplain #subset feature subset} is created from a query.
     * The optimization depends on the set of {@linkplain #allTypes all feature types} found in the collection.
     *
     * @since 1.6
     */
    @Override
    protected void prepareQueryOptimization(FeatureQuery query, Optimization optimizer) throws DataStoreException {
        synchronized (getSynchronizationLock()) {
            optimizer.setFinalFeatureTypes(allTypes);
        }
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
            return baseType.equals(other.baseType) &&
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
        return baseType.hashCode() + 31 * features.hashCode() + 37 * listeners.hashCode();
    }
}
