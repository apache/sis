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

import java.nio.file.StandardOpenOption;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.StoreMetadata;


/**
 * A pair of {@link ProbeResult} and {@link DataStoreProvider}, for internal usage by {@link DataStoreRegistry} only.
 * Provides also a {@link DataStore} created by the provider if this class is used for an {@code open(…)} operation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ProbeProviderPair {
    /**
     * The provider to use for probing a file.
     */
    final DataStoreProvider provider;

    /**
     * The result of the call to {@link DataStoreProvider#probeContent(StorageConnector)}.
     */
    ProbeResult probe;

    /**
     * A data store created by the provider.
     * This is non-null only if this class is used for an {@code open} operation.
     */
    DataStore store;

    /**
     * Creates a new pair with a result not yet known.
     */
    ProbeProviderPair(final DataStoreProvider provider) {
        this.provider = provider;
    }

    /**
     * Sets the {@linkplain #probe} result for a file that does not exist yet.
     * The result will be {@link ProbeResult#CREATE_NEW} or {@code UNSUPPORTED_STORAGE},
     * depending on whether the {@linkplain #provider} supports the creation of new storage.
     * In both cases, {@link StorageConnector#wasProbingAbsentFile()} will return {@code true}.
     *
     * <p>This method is invoked for example if the storage is a file, the file does not exist
     * but {@link StandardOpenOption#CREATE} or {@link StandardOpenOption#CREATE_NEW CREATE_NEW}
     * option was provided and the data store has write capability. Note however that declaring
     * {@code CREATE_NEW} is not a guarantee that the data store will successfully create the resource.
     * For example we do not verify if the file system grants write permission to the application.</p>
     *
     * @see StorageConnector#wasProbingAbsentFile()
     */
    final void setProbingAbsentFile() {
        final StoreMetadata md = provider.getClass().getAnnotation(StoreMetadata.class);
        if (md == null || ArraysExt.contains(md.capabilities(), Capability.CREATE)) {
            probe = ProbeResult.CREATE_NEW;
        } else {
            probe = ProbeResult.UNSUPPORTED_STORAGE;
        }
    }
}
