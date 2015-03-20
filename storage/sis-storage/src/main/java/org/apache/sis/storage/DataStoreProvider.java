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
 * Provides information about a specific {@link DataStore} implementation.
 * There is typically one {@code DataStoreProvider} instance for each format supported by a library.
 * Each {@code DataStoreProvider} instances provides the following services:
 *
 * <ul>
 *   <li>Provide generic information about the storage (name, <i>etc.</i>).</li>
 *   <li>Create instances of the {@link DataStore} implementation described by this provider.</li>
 *   <li>Test if a {@code DataStore} instance created by this provider would have reasonable chances
 *       to open a given {@link StorageConnector}.</li>
 * </ul>
 *
 * <div class="section">Packaging data stores</div>
 * JAR files that provide implementations of this class shall contain an entry with exactly the following path:
 *
 * {@preformat text
 *     META-INF/services/org.apache.sis.storage.DataStoreProvider
 * }
 *
 * The above entry shall contain one line for each {@code DataStoreProvider} implementation provided in the JAR file,
 * where each line is the fully qualified name of the implementation class.
 * See {@link java.util.ServiceLoader} for more general discussion about this lookup mechanism.
 *
 * <div class="section">Thread safety</div>
 * All {@code DataStoreProvider} implementations shall be thread-safe.
 * However the {@code DataStore} instances created by the providers do not need to be thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
public abstract class DataStoreProvider {
    /**
     * Creates a new provider.
     */
    protected DataStoreProvider() {
    }

    /**
     * Indicates if the given storage appears to be supported by the {@code DataStore}s created by this provider.
     * The most typical return values are:
     *
     * <ul>
     *   <li>{@link ProbeResult#SUPPORTED} if the {@code DataStore}s created by this provider
     *       can open the given storage.</li>
     *   <li>{@link ProbeResult#UNSUPPORTED_STORAGE} if the given storage does not appear to be in a format
     *       supported by this {@code DataStoreProvider}.</li>
     * </ul>
     *
     * Note that the {@code SUPPORTED} value does not guarantee that reading or writing will succeed,
     * only that there appears to be a reasonable chance of success based on a brief inspection of the
     * {@linkplain StorageConnector#getStorage() storage object} or contents.
     *
     * <p>Implementors are responsible for restoring the input to its original stream position on return of this method.
     * Implementors can use a mark/reset pair for this purpose. Marks are available as
     * {@link java.nio.ByteBuffer#mark()}, {@link java.io.InputStream#mark(int)} and
     * {@link javax.imageio.stream.ImageInputStream#mark()}.</p>
     *
     * <div class="note"><b>Implementation example</b><br>
     * Implementations will typically check the first bytes of the stream for a "magic number" associated
     * with the format, as in the following example:
     *
     * {@preformat java
     *     public ProbeResult probeContent(StorageConnector storage) throws DataStoreException {
     *         final ByteBuffer buffer = storage.getStorageAs(ByteBuffer.class);
     *         if (buffer == null) {
     *             // If StorageConnector can not provide a ByteBuffer, then the storage is
     *             // probably not a File, URL, URI, InputStream neither a ReadableChannel.
     *             return ProbeResult.UNSUPPORTED_STORAGE;
     *         }
     *         if (buffer.remaining() < Integer.BYTES) {
     *             // If the buffer does not contain enough bytes for the integer type, this is not
     *             // necessarily because the file is truncated. It may be because the data were not
     *             // yet available at the time this method has been invoked.
     *             return ProbeResult.INSUFFICIENT_BYTES;
     *         }
     *         if (buffer.getInt(buffer.position()) != MAGIC_NUMBER) {
     *             // We used ByteBuffer.getInt(int) instead than ByteBuffer.getInt() above
     *             // in order to keep the buffer position unchanged after this method call.
     *             return ProbeResult.UNSUPPORTED_STORAGE;
     *         }
     *         return ProbeResult.SUPPORTED;
     *     }
     * }
     * </div>
     *
     * @param  storage Information about the storage (URL, stream, JDBC connection, <i>etc</i>).
     * @return {@link ProbeResult#SUPPORTED} if the given storage seems to be readable by the {@code DataStore}
     *         instances created by this provider.
     * @throws DataStoreException if an I/O or SQL error occurred. The error shall be unrelated to the logical
     *         structure of the storage.
     */
    public abstract ProbeResult probeContent(StorageConnector storage) throws DataStoreException;

    /**
     * Returns a data store implementation associated with this provider.
     *
     * <div class="section">Implementation note</div>
     * Implementors shall invoke {@link StorageConnector#closeAllExcept(Object)} after {@code DataStore}
     * creation, keeping open only the needed resource.
     *
     * @param  storage Information about the storage (URL, stream, JDBC connection, <i>etc</i>).
     * @return A data store implementation associated with this provider for the given storage.
     * @throws DataStoreException If an error occurred while creating the data store instance.
     *
     * @see DataStores#open(Object)
     */
    public abstract DataStore open(StorageConnector storage) throws DataStoreException;
}
