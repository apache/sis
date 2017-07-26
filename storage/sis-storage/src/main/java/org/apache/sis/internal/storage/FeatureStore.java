/*
 * Copyright 2016 desruisseaux.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.internal.storage;

import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.IllegalNameException;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.Stream;
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;


/**
 * Base class of data store that produce feature instances.
 *
 * <b>This is not yet a committed API!</b>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
public abstract class FeatureStore extends DataStore {
    /**
     * Creates a new instance for the given storage (typically file or database).
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, reader instance, <i>etc</i>).
     * @throws DataStoreException if an error occurred while creating the data store for the given storage.
     */
    protected FeatureStore(final DataStoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(provider, connector);
    }

    /**
     * Returns the feature type for the given name. The {@code name} argument should be the result of calling
     * {@link org.opengis.util.GenericName#toString()} on the name of one of the feature types in this data store.
     * The list of feature type names can be obtained from the {@linkplain #getMetadata() metadata} like below:
     *
     * {@preformat java
     *     for (ContentInformation c : metadata.getContentInfo()) {
     *         if (c instanceof FeatureCatalogueDescription) {
     *             for (FeatureTypeInfo info : ((FeatureCatalogueDescription) c).getFeatureTypeInfo()) {
     *                 GenericName name = info.getFeatureTypeName();
     *                 // ... add the name to some list ...
     *             }
     *         }
     *     }
     * }
     *
     * Implementation may also accept aliases for convenience. For example if the full name of a feature type
     * is {@code "foo:bar"}, then this method may accept {@code "bar"} as a synonymous of {@code "foo:bar"}
     * provided that it does not introduce ambiguity.
     *
     * @param  name  the name or alias of the feature type to get.
     * @return the feature type of the given name or alias (never {@code null}).
     * @throws IllegalNameException if the given name was not found or is ambiguous.
     * @throws DataStoreException if another kind of error occurred while searching for feature types.
     */
    public abstract DefaultFeatureType getFeatureType(String name) throws DataStoreException;

    /**
     * Returns the stream of all features found in the data store.
     * If a checked exception occurs during consumption of the returned stream, that exception will
     * be wrapped in a unchecked {@link org.apache.sis.util.collection.BackingStoreException}.
     *
     * @param  parallel  {@code true} for a parallel stream, or {@code false} for a sequential stream.
     * @return a stream over all features in the data store.
     * @throws DataStoreException if an error occurred while creating the feature stream.
     *
     * @todo a future version of this method will take some kind of {@code Query} or {@code Filter} argument.
     */
    public abstract Stream<AbstractFeature> features(boolean parallel) throws DataStoreException;
}
