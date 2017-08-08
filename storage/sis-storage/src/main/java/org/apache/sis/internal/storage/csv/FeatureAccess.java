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
package org.apache.sis.internal.storage.csv;

import org.opengis.metadata.Metadata;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.internal.storage.AbstractFeatureSet;
import org.apache.sis.util.logging.WarningListeners;

// Branch-dependent imports
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.internal.jdk8.Stream;


/**
 * Provides access to feature instances.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class FeatureAccess extends AbstractFeatureSet {
    /**
     * Creates a {@code FeatureSet} for the given CSV store.
     */
    FeatureAccess(final Store store, final WarningListeners<DataStore> listeners) {
        super(store, listeners);
    }

    /**
     * Returns information about the dataset as a whole.
     *
     * @return information about the dataset, or {@code null} if none.
     * @throws DataStoreException if an error occurred while reading the metadata.
     */
    @Override
    public Metadata getMetadata() throws DataStoreException {
        return store.getMetadata();
    }

    /**
     * Returns the type of features in the CSV file. The feature type name will be the value
     * specified at the following path (only one such value exists for a CSV data store):
     *
     * <blockquote>
     * {@link #getMetadata()} /
     * {@link org.apache.sis.metadata.iso.DefaultMetadata#getContentInfo() contentInfo} /
     * {@link org.apache.sis.metadata.iso.content.DefaultFeatureCatalogueDescription#getFeatureTypeInfo() featureTypes} /
     * {@link org.apache.sis.metadata.iso.content.DefaultFeatureTypeInfo#getFeatureTypeName() featureTypeName}
     * </blockquote>
     *
     * @return type of features in the CSV file.
     */
    @Override
    public DefaultFeatureType getType() {
        return ((Store) store).featureType;
    }

    /**
     * Returns the stream of features.
     *
     * @param  parallel  {@code true} for a parallel stream, or {@code false} for a sequential stream.
     * @return a stream over all features in the CSV file.
     * @throws DataStoreException if an error occurred while creating the feature stream.
     */
    @Override
    public Stream<AbstractFeature> features(final boolean parallel) throws DataStoreException {
        return ((Store) store).features(parallel);
    }
}
