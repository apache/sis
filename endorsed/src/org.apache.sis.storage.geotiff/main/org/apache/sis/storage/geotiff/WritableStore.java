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
package org.apache.sis.storage.geotiff;

import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.WritableAggregate;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.base.WritableAggregateSupport;


/**
 * A GeoTIFF data store with write capabilities.
 *
 * @author  Erwan Roussel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class WritableStore extends GeoTiffStore implements WritableAggregate {
    /**
     * Creates a new GeoTIFF store from the given file, URL or stream object.
     * This constructor invokes {@link StorageConnector#closeAllExcept(Object)},
     * keeping open only the needed resource.
     *
     * @param  provider   the factory that created this {@code WritableStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the GeoTIFF file.
     */
    public WritableStore(final GeoTiffStoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(provider, connector);
    }

    /**
     * Adds a new {@code GridCoverageResource} in this {@code Aggregate}.
     * The given {@link Resource} will be copied, and the <i>effectively added</i> resource returned.
     *
     * @param  resource  the resource to copy in this {@code Aggregate}.
     * @return the effectively added resource. Using this resource may cause data to be reloaded.
     * @throws DataStoreException if the given resource cannot be stored in this {@code Aggregate}.
     */
    @Override
    public Resource add(final Resource resource) throws DataStoreException {
        final var helper = new WritableAggregateSupport(this);
        if (resource instanceof Aggregate) {
            return helper.writeComponents((Aggregate) resource);
        }
        final GridCoverageResource gr = helper.asGridCoverage(resource);
        return append(gr.read(null, null), gr.getMetadata());
    }

    /**
     * Removes a {@code Resource} from this {@code Aggregate}.
     * The given resource should be one of the instances returned by {@link #components()}.
     * This operation is destructive in two aspects:
     *
     * <ul>
     *   <li>The {@link Resource} and it's data will be deleted from the {@link DataStore}.</li>
     *   <li>The given resource may become invalid and should not be used anymore after this method call.</li>
     * </ul>
     *
     * @param  resource  child resource to remove from this {@code Aggregate}.
     * @throws DataStoreException if the given resource could not be removed.
     */
    @Override
    public void remove(Resource resource) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
