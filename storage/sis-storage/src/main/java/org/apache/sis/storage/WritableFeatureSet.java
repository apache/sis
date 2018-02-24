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

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;


/**
 * Subtype of {@linkplain FeatureSet} with writing capabilities.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public interface WritableFeatureSet extends FeatureSet {
    /**
     * Declares or redefines the type of all feature instances in this feature set.
     * In the case of a newly created feature set, this method can be used for defining the type of features
     * to be stored (this is not a required step however). In the case of a feature set which already contains
     * feature instances, this operation may take an undefined amount of time to execute since all features in
     * the set may need to be transformed.
     *
     * <p>Feature sets may restrict the kind of changes that are allowed. An {@link IllegalFeatureTypeException}
     * will be thrown if the given type contains incompatible property changes.</p>
     *
     * @param  newType  new feature type definition (not {@code null}).
     * @throws IllegalFeatureTypeException if the given type is not compatible with the types supported by the store.
     * @throws DataStoreException if another error occurred while changing the feature type.
     */
    void updateType(FeatureType newType) throws IllegalFeatureTypeException, DataStoreException;

    /**
     * Inserts new features in this {@code FeatureSet}.
     * Any feature already present in the {@link FeatureSet} will remain unmodified.
     *
     * <div class="note"><b>API note:</b>
     * this method expects an {@link Iterator} rather then a {@link Stream} for easing
     * inter-operability with various API. Implementing a custom {@link Iterator} requires less effort
     * than implementing a {@link Stream}. On the other side if the user has a {@link Stream},
     * obtaining an {@link Iterator} can be done by a call to {@link Stream#iterator()}.</div>
     *
     * @param  features features to insert in this {@code FeatureSet}.
     * @throws DataStoreException if another error occurred while storing new features.
     */
    void add(Iterator<? extends Feature> features) throws DataStoreException;

    /**
     * Removes all features from this {@code FeatureSet} which matches the given predicate.
     *
     * @param  filter  a predicate which returns true for resources to be removed.
     * @return {@code true} if any elements were removed.
     * @throws DataStoreException if another error occurred while removing features.
     */
    boolean removeIf(Predicate<? super Feature> filter) throws DataStoreException;

    /**
     * Updates all features from this {@code FeatureSet} which matches the given predicate.
     * For each {@link Feature} instance matching the given {@link Predicate},
     * the <code>{@linkplain UnaryOperator#apply UnaryOperator.apply(Feature)}</code> method will be invoked.
     * {@code UnaryOperator}s are free to modify the given {@code Feature} <i>in-place</i> or to return a
     * different feature instance. Two behaviors are possible:
     * <ul>
     *   <li>If the operator returns a non-null {@link Feature}, then the modified feature is stored
     *       in replacement of the previous feature (not necessarily at the same location).</li>
     *   <li>If the operator returns {@code null}, then the feature will be removed from the {@code FeatureSet}.</li>
     * </ul>
     *
     * @param  filter   a predicate which returns true for resources to be updated.
     * @param  updater  operation called for each matching {@link Feature}.
     * @throws DataStoreException if another error occurred while replacing features.
     */
    void replaceIf(Predicate<? super Feature> filter, UnaryOperator<Feature> updater)
            throws DataStoreException;
}
