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

import org.opengis.metadata.Metadata;


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
     * Closes this data store and releases any underlying resources.
     *
     * @throws DataStoreException If an error occurred while closing this data store.
     */
    @Override
    void close() throws DataStoreException;
}
