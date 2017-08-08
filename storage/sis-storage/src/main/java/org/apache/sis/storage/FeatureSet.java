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

// Branch-dependent imports
import org.apache.sis.internal.jdk8.Stream;
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;


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
    DefaultFeatureType getType() throws DataStoreException;

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
    Stream<AbstractFeature> features(boolean parallel) throws DataStoreException;
}
