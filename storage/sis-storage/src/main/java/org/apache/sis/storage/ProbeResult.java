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
 * Tells whether a storage (file, database) appears to be supported by a {@code DataStore}.
 * There is three categories of values in this enumeration:
 *
 * <ul>
 *   <li>{@link #SUPPORTED} indicates that the storage can be read and eventually written.</li>
 *   <li>{@code UNSUPPORTED_*} indicate that the storage can not be opened. The actual enumeration value gives
 *       the reason (e.g. unsupported format or {@linkplain #UNSUPPORTED_VERSION unsupported version}).</li>
 *   <li>{@link #INSUFFICIENT_BYTES} or {@link #UNDETERMINED} indicate that the provider does not have enough
 *       information for telling whether the storage can be opened. SIS will try to use such provider last,
 *       if no better suited provider is found.</li>
 * </ul>
 *
 * When a {@link DataStores#open DataStores.open(…)} method is invoked, SIS will iterate over the list of known
 * providers and invoke the {@link DataStoreProvider#canOpen(StorageConnector)} method for each of them.
 * The {@code ProbeResult} value returned by {@code canOpen(…)} tells to SIS whether a particular
 * {@code DataStoreProvider} instance has reasonable chances to be able to handle the given storage.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 *
 * @see DataStoreProvider#canOpen(StorageConnector)
 */
public enum ProbeResult {
    /**
     * The {@code DataStoreProvider} recognizes the given storage.
     * {@code DataStore} instances created by that provider are likely (but not guaranteed)
     * to be able to read from - and eventually write to - the given storage.
     */
    SUPPORTED,

    /**
     * The {@code DataStoreProvider} does not recognize the given storage object, file format or database schema.
     * Examples:
     *
     * <ul>
     *   <li>The storage is a file while the provider expected a database connection (or conversely).</li>
     *   <li>The file does not contains the expected magic number.</li>
     *   <li>The database schema does not contain the expected tables.</li>
     * </ul>
     */
    UNSUPPORTED_STORAGE,

    /**
     * The {@code DataStoreProvider} recognizes the given storage, but the data are structured
     * according a file or schema version not yet supported by the current implementation.
     */
    UNSUPPORTED_VERSION,

    /**
     * The open capability can not be determined because the {@link ByteBuffer} contains an insufficient
     * amount of bytes. This value may be returned by {@link DataStoreProvider#canOpen(StorageConnector)}
     * implementations similar to the following:
     *
     * {@preformat java
     *     public ProbeResult canOpen(StorageConnector storage) throws DataStoreException {
     *         final ByteBuffer buffer = storage.getStorageAs(ByteBuffer.class);
     *         if (buffer == null) {
     *             return ProbeResult.UNSUPPORTED_STORAGE;
     *         }
     *         if (buffer.remaining() < Integer.SIZE / Byte.SIZE) {
     *             return ProbeResult.INSUFFICIENT_BYTES;
     *         }
     *         // Other verifications here.
     *     }
     * }
     *
     * When some {@code DataStoreProvider} return this value, SIS will first continue the search for a provider that
     * can answer the {@code canOpen(…)} question using only the available bytes. Only if no provider is found,
     * then SIS will fetch more bytes and try again the providers that returned {@code INSUFFICIENT_BYTES}.
     * SIS tries to work with available bytes before to ask more in order to reduce latencies on network connections.
     */
    INSUFFICIENT_BYTES,

    /**
     * The open capability can not be determined.
     * This value may be returned by {@code DataStore} implementations that could potentially open anything,
     * as for example of the RAW image format.
     *
     * <p><strong>This is a last resort value!</strong> {@code canOpen(…)} implementations are strongly encouraged
     * to return a more accurate enumeration value for allowing {@link DataStores#open(Object)} to perform a better
     * choice. Generally, this value should be used only by the RAW image format.</p>
     */
    UNDETERMINED
}
