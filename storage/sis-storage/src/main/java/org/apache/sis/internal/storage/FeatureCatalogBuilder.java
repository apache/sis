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

import org.opengis.util.GenericName;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.FeatureNaming;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.metadata.iso.DefaultMetadata;

// Branch-dependent imports
import org.apache.sis.feature.DefaultFeatureType;


/**
 * Helper methods for the feature metadata created by {@code DataStore} implementations.
 * This is a convenience class for chaining {@code addFeatureType(FeatureType, Integer)}
 * method calls with {@link FeatureNaming#add(DataStore, GenericName, Object)}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @see org.apache.sis.metadata.iso.content.DefaultFeatureCatalogueDescription
 *
 * @since 0.8
 * @module
 */
public class FeatureCatalogBuilder extends MetadataBuilder {
    /**
     * The data store for which the metadata will be created, or {@code null} if unknown.
     * This is used for producing error message if an exception is thrown.
     */
    private final DataStore store;

    /**
     * The feature types created by the {@code FeatureCatalogBuilder}.
     * {@code DataStore} implementations can keep the reference to this {@code FeatureNaming}
     * after the {@link #build(boolean)} method has been invoked.
     */
    public final FeatureNaming<DefaultFeatureType> features;

    /**
     * Creates a new builder for the given data store.
     *
     * @param  store  the data store for which the metadata will be created, or {@code null} if unknown.
     */
    public FeatureCatalogBuilder(final DataStore store) {
        this.store = store;
        features = new FeatureNaming<>();
    }

    /**
     * Adds descriptions for the given feature in both the {@link DefaultMetadata} and {@link FeatureNaming} instances.
     * Invoking this method is equivalent to executing the following code (omitting {@code null} checks for brevity):
     *
     * {@preformat java
     *     features.add(store, add(type, null), type);
     * }
     *
     * @param  type  the feature type to add, or {@code null}.
     * @throws IllegalNameException if a feature of the same name has already been added.
     *
     * @see #addFeatureType(DefaultFeatureType, Integer)
     */
    public final void define(final DefaultFeatureType type) throws IllegalNameException {
        final GenericName name = addFeatureType(type, null);
        if (name != null) {
            features.add(store, name, type);
        }
    }
}
