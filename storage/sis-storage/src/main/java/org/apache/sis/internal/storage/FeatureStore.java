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
import org.apache.sis.storage.DataStoreException;

// Branch-dependent imports
import java.util.stream.Stream;
import org.opengis.feature.Feature;


/**
 * Base class of data store that produce feature instances.
 *
 * <b>This is not yet a committed API!</b>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public abstract class FeatureStore extends DataStore {
    /**
     * Creates a new instance for the given storage (typically file or database).
     *
     * @param connector information about the storage (URL, stream, reader instance, <i>etc</i>).
     * @throws DataStoreException if an error occurred while creating the data store for the given storage.
     */
    protected FeatureStore(final StorageConnector connector) throws DataStoreException {
        super(connector);
    }

    /**
     * Returns the stream of features.
     *
     * @return a stream over all features in the file.
     * @throws DataStoreException if an error occurred while creating the feature stream.
     */
    public abstract Stream<Feature> getFeatures() throws DataStoreException;
}
