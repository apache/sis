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

import java.util.stream.Stream;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;


/**
 * A dataset providing access to a stream of features.
 * All features share a common set of properties described by {@link #getType()}.
 * Each {@linkplain org.apache.sis.feature.AbstractFeature feature instance} can be associated to a geometry,
 * but not necessarily. The geometries (if any) may or may not be parts of a coverage.
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
     * If an error happen during stream execution, an unchecked {@link org.apache.sis.util.collection.BackingStoreException}
     * will be thrown. The following code shows how this stream can be used:
     *
     * {@preformat java
     *     void myReadOperation() throws DataStoreException {
     *         try (Stream<Feature> features = myDataStore.features()) {
     *             // Use the stream here.
     *         } catch (BackingStoreException e) {
     *             throw e.unwrapOrRethrow(DataStoreException.class);
     *         }
     *     }
     * }
     *
     * For performance reasons, some {@code Feature} instances may be recycled during stream execution.
     * Consequently if the caller needs to keep property values, (s)he should copy the data in her own structure.
     *
     * @return all features contained in this dataset.
     * @throws DataStoreException if an error occurred while creating the stream.
     */
    Stream<Feature> features() throws DataStoreException;
}
