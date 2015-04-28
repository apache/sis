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


/**
 * A pair of {@link ProbeResult} and {@link DataStoreProvider},
 * for internal usage by {@link DataStoreRegistry} only.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
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
     */
    DataStore store;

    /**
     * Creates a new pair.
     */
    ProbeProviderPair(final DataStoreProvider provider, final ProbeResult probe) {
        this.provider = provider;
        this.probe    = probe;
    }
}
