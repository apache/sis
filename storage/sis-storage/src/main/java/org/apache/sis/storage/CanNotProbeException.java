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
 * Thrown when an unrecoverable error occurred during the probing of a file.
 * This exception contains a reference to the provider that failed.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 *
 * @see DataStoreProvider#probeContent(StorageConnector)
 *
 * @since 1.2
 * @module
 */
public class CanNotProbeException extends DataStoreException {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7183214487330030125L;

    /**
     * The data store provider that failed to probe a file.
     */
    private final DataStoreProvider provider;

    /**
     * Creates an exception with the specified details message and cause.
     *
     * @param provider  the data store provider that failed to probe a file.
     * @param message   the detail message in the default locale.
     * @param cause     the cause for this exception.
     */
    public CanNotProbeException(final DataStoreProvider provider, final String message, final Throwable cause) {
        super(message, cause);
        this.provider = provider;
    }

    /**
     * Creates a localized exception with a message saying that the given store can not be processed.
     *
     * @param provider   the data store provider that failed to probe a file.
     * @param connector  the stream, file or other kind of resource that the store provider tried to probe.
     * @param cause      the reason why the data store can not be probed.
     */
    public CanNotProbeException(final DataStoreProvider provider, final StorageConnector connector, final Throwable cause) {
        super(null, provider.getShortName(), connector.getStorageName(), connector.storage);
        this.provider = provider;
        super.initCause(cause);
    }

    /**
     * Returns the data store provider that failed to probe a file.
     *
     * @return the data store provider that failed to probe a file.
     *
     * @see DataStore#getProvider()
     */
    public DataStoreProvider getProvider() {
        return provider;
    }
}
