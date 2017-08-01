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
 * Specialized type of DataSet which manage features.
 *
 * @author Johann Sorel (Geomatys)
 */
public interface FeatureSet extends DataSet {

    /**
     * Get dataset feature type.
     * The feature type contains the definition of all fields, including but not only :
     * <ul>
     * <li>description</li>
     * <li>primitive type</li>
     * <li>cardinality</li>
     * <li>{@link CoordinateReferenceSystem}</li>
     * </ul>
     *
     * @return the feature type, never null.
     * @throws DataStoreException if an I/O or decoding error occurs.
     */
    FeatureType getType() throws DataStoreException;

    /**
     * Reads features from the dataset.
     *
     * @return stream of features.
     * @throws DataStoreException if an I/O or decoding error occurs.
     */
    Stream<Feature> features() throws DataStoreException;

}
