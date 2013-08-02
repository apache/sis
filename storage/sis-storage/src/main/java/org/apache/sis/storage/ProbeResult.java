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
 *   <li>{@link #UNDETERMINED} indicates that the provider does not have enough information for telling
 *       whether the storage can be opened. SIS will try to use such provider last, if no better suited
 *       provider is found.</li>
 *   <li>All other values indicate that the storage can not be opened. The actual enumeration value gives
 *       the reason (e.g. {@linkplain #UNKNOWN_FORMAT unknown format}, or
 *       {@linkplain #UNSUPPORTED_VERSION unsupported version}).</li>
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
     * The open capability can not be determined.
     * This value may be returned in two kinds of situation:
     *
     * <ul>
     *   <li>The method can not look ahead far enough in the file header,
     *       for example because the buffer has not fetched enough bytes.</li>
     *   <li>The {@code DataStore} could potentially open anything.
     *       This is the case for example of the RAW image format.</li>
     * </ul>
     */
    UNDETERMINED,

    /**
     * The {@code DataStoreProvider} does not recognize the given storage object.
     * For example the storage may be a file while the provider expected a database connection, or conversely.
     */
    UNKNOWN_STORAGE,

    /**
     * The {@code DataStoreProvider} does not recognize the file format or schema.
     * For example the file does not contains the expected magic number,
     * or the database schema does not contain the expected tables.
     */
    UNKNOWN_FORMAT,

    /**
     * The {@code DataStoreProvider} recognizes the given storage, but the data are structured
     * according a file or schema version not yet supported by the current implementation.
     */
    UNSUPPORTED_VERSION
}
