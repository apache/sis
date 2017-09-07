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
import org.apache.sis.internal.storage.Resources;

// Branch-dependent imports
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;


/**
 * A dataset providing access to a stream of features.
 * All features share a common set of properties described by {@link #getType()}.
 * The common set of properties does not need to enumerate all possible properties since additional properties
 * can be defined in subtypes. In many cases at least one property is a geometry, but features without geometry
 * are also allowed.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public interface FeatureSet extends DataSet {
    /**
     * Returns a description of properties that are common to all features in this dataset.
     * The feature type contains the definition of all properties, including but not only:
     * <ul>
     *   <li>Name to use for accessing the property</li>
     *   <li>Human-readable description</li>
     *   <li>Type of values</li>
     *   <li>Cardinality (minimum and maximum number of occurrences)</li>
     *   <li>{@linkplain org.opengis.referencing.crs.CoordinateReferenceSystem Coordinate Reference System}.</li>
     * </ul>
     *
     * All features returned by {@link #features(boolean)} will be either of that type, or a sub-type of it.
     *
     * <div class="note"><b>Relationship with metadata:</b>
     * if subtypes exist, their list may be obtained from the {@linkplain #getMetadata() metadata} like below
     * (if the {@code FeatureSet} implementation provides that information):
     *
     * {@preformat java
     *     for (ContentInformation content : metadata.getContentInfo()) {
     *         if (content instanceof FeatureCatalogueDescription) {
     *             for (FeatureTypeInfo info : ((FeatureCatalogueDescription) content).getFeatureTypeInfo()) {
     *                 GenericName name = info.getFeatureTypeName();
     *                 // ... add the name to some list ...
     *             }
     *         }
     *     }
     * }
     * </div>
     *
     * @return description of common properties (never {@code null}).
     * @throws DataStoreException if an error occurred while reading definitions from the underlying data store.
     */
    FeatureType getType() throws DataStoreException;

    /**
     * Returns a stream of all features contained in this dataset.
     * For all features, the following condition shall be true:
     *
     * <blockquote><code>{@linkplain #getType()}.{@linkplain org.apache.sis.feature.DefaultFeatureType#isAssignableFrom
     * isAssignableFrom}(feature.{@linkplain org.apache.sis.feature.AbstractFeature#getType() getType()})</code></blockquote>
     *
     * Most implementations will create {@code Feature} instances on-the-fly when the stream terminal operation is executed.
     * A {@code try} … {@code finally} block should be used for releasing {@link DataStore} resources used by the operation.
     * If a checked exception happens during stream execution, that exception will be wrapped in an unchecked
     * {@link org.apache.sis.util.collection.BackingStoreException}.
     * The following code shows how this stream can be used:
     *
     * {@preformat java
     *     void myReadOperation() throws DataStoreException {
     *         try (Stream<Feature> features = myDataStore.features(false)) {
     *             // Use the stream here.
     *         } catch (BackingStoreException e) {
     *             throw e.unwrapOrRethrow(DataStoreException.class);
     *         }
     *     }
     * }
     *
     * The {@code parallel} argument specifies whether a parallelized stream is desired. If {@code false}, the stream
     * is guaranteed to be sequential. If {@code true}, the stream may or may not be parallel; implementations are free
     * to ignore this argument if they do not support parallelism.
     *
     * @param  parallel  {@code true} for a parallel stream (if supported), or {@code false} for a sequential stream.
     * @return all features contained in this dataset.
     * @throws DataStoreException if an error occurred while creating the stream.
     */
    Stream<Feature> features(boolean parallel) throws DataStoreException;

    /**
     * Inserts new features in this {@code FeatureSet}.
     * Any feature already present in the {@link FeatureSet} will remain unmodified.
     *
     * <div class="note"><b>API note:</b>
     * this method expects an {@link Iterator} rather then a {@link java.util.stream.Stream} for easing
     * inter-operability with various API. Implementing a custom {@link Iterator} requires less effort
     * than implementing a {@link Stream}. On the other side if the user has a {@link Stream},
     * obtaining an {@link Iterator} can be done by a call to {@link Stream#iterator()}.</div>
     *
     * <p>The default implementation throws {@link ReadOnlyStorageException}.</p>
     *
     * @param  features features to insert in this {@code FeatureSet}.
     * @throws ReadOnlyStorageException if this instance does not support write operations.
     * @throws DataStoreException if another error occurred while storing new features.
     */
    default void add(Iterator<? extends Feature> features) throws ReadOnlyStorageException, DataStoreException {
        throw new ReadOnlyStorageException(this, Resources.Keys.StoreIsReadOnly);
    }

    /**
     * Removes all features from this {@code FeatureSet} which matches the given predicate.
     *
     * <p>The default implementation throws {@link ReadOnlyStorageException}.</p>
     *
     * @param  filter  a predicate which returns true for resources to be removed.
     * @return {@code true} if any elements were removed.
     * @throws ReadOnlyStorageException if this instance does not support write operations.
     * @throws DataStoreException if another error occurred while removing features.
     */
    default boolean removeIf(Predicate<? super Feature> filter) throws ReadOnlyStorageException, DataStoreException {
        throw new ReadOnlyStorageException(this, Resources.Keys.StoreIsReadOnly);
    }

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
     * <p>The default implementation throws {@link ReadOnlyStorageException}.</p>
     *
     * @param  filter   a predicate which returns true for resources to be updated.
     * @param  updater  operation called for each matching {@link Feature}.
     * @throws ReadOnlyStorageException if this instance does not support write operations.
     * @throws DataStoreException if another error occurred while replacing features.
     */
    default void replaceIf(Predicate<? super Feature> filter, UnaryOperator<Feature> updater)
            throws ReadOnlyStorageException, DataStoreException
    {
        throw new ReadOnlyStorageException(this, Resources.Keys.StoreIsReadOnly);
    }
}
