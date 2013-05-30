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

import java.util.NoSuchElementException;
import org.opengis.metadata.Metadata;
import org.apache.sis.util.logging.WarningListener;


/**
 * A storage object which manage a series of features, coverages or sensor data.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public interface DataStore extends AutoCloseable {
    /**
     * Returns information about the dataset as a whole. The returned metadata object, if any, can contain
     * information such as the spatiotemporal extent of the dataset, contact information about the creator
     * or distributor, data quality, update frequency, usage constraints and more.
     *
     * @return Information about the dataset, or {@code null} if none.
     * @throws DataStoreException If an error occurred while reading the data.
     */
    Metadata getMetadata() throws DataStoreException;

    /**
     * Adds a listener to be notified when a warning occurred while reading from or writing to the storage.
     * When a warning occurs, there is a choice:
     *
     * <ul>
     *   <li>If this data store has no warning listener, then the warning is logged at
     *       {@link java.util.logging.Level#WARNING}.</li>
     *   <li>If this data store has at least one warning listener, then all listeners are notified
     *       and the warning is <strong>not</strong> logged by this data store instance.</li>
     * </ul>
     *
     * Consider invoking this method in a {@code try} … {@code finally} block if the {@code DataStore}
     * lifetime is longer than the listener lifetime, as below:
     *
     * {@preformat java
     *     datastore.addWarningListener(listener);
     *     try {
     *         // Do some work...
     *     } finally {
     *         datastore.removeWarningListener(listener);
     *     }
     * }
     *
     * @param  listener The listener to add.
     * @throws IllegalArgumentException If the given listener is already registered in this data store.
     */
    void addWarningListener(WarningListener<? super DataStore> listener) throws IllegalArgumentException;

    /**
     * Removes a previously registered listener.
     *
     * @param  listener The listener to remove.
     * @throws NoSuchElementException If the given listener is not registered in this data store.
     */
    void removeWarningListener(WarningListener<? super DataStore> listener) throws NoSuchElementException;

    /**
     * Closes this data store and releases any underlying resources.
     *
     * @throws DataStoreException If an error occurred while closing this data store.
     */
    @Override
    void close() throws DataStoreException;
}
