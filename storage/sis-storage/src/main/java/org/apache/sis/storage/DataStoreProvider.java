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

import java.util.Set;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import org.apache.sis.util.ThreadSafe;


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
 * {@section Packaging data stores}
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
 * {@section Thread safety policy}
 * All {@code DataStoreProvider} implementations shall be thread-safe.
 * However the {@code DataStore} instances created by the providers do not need to be thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
@ThreadSafe
public abstract class DataStoreProvider {
    /**
     * Creates a new provider.
     */
    protected DataStoreProvider() {
    }

    /**
     * Returns a non-empty set if the given storage appears to be supported by the {@code DataStore}.
     * Returning a non-empty set from this method does not guarantee that reading or writing will succeed,
     * only that there appears to be a reasonable chance of success based on a brief inspection of the
     * {@linkplain StorageConnector#getStorage() storage object} or contents.
     *
     * <p>If the given storage is supported, then the returned set shall contain at least one of the
     * following values:</p>
     *
     * <table class="sis">
     *   <tr><th>Value</th>                                 <th>Meaning</th></tr>
     *   <tr><td>{@link StandardOpenOption#READ}</td>       <td>Can read data from the given storage.</td></tr>
     *   <tr><td>{@link StandardOpenOption#WRITE}</td>      <td>Can overwrite existing data.</td></tr>
     *   <tr><td>{@link StandardOpenOption#APPEND}</td>     <td>Can write new data.</td></tr>
     *   <tr><td>{@link StandardOpenOption#CREATE_NEW}</td> <td>Can create a new storage at the given location.</td></tr>
     * </table>
     *
     * Other values may be present at implementation choice.
     *
     * {@section Implementation note}
     * Implementations will typically check the first bytes of the stream for a "magic number" associated
     * with the format, as in the following example:
     *
     * {@preformat java
     *     public Set<OpenOption> getOpenCapabilities(StorageConnector storage) throws DataStoreException {
     *         final ByteBuffer buffer = storage.getStorageAs(ByteBuffer.class);
     *         if (buffer != null) {
     *             if (buffer.remaining() < Integer.SIZE / Byte.SIZE) {
     *                 return null; // See notes below.
     *             }
     *             if (buffer.getInt(buffer.position()) == MAGIC_NUMBER) {
     *                 return EnumSet.of(StandardOpenOption.READ);
     *             }
     *         }
     *         return Collections.emptySet();
     *     }
     * }
     *
     * {@note <ul>
     *   <li>If <code>StorageConnector</code> can not provide a <code>ByteBuffer</code>, then the storage is
     *       probably not a <code>File</code>, <code>URL</code>, <code>URI</code>, <code>InputStream</code>
     *       neither a <code>ReadableChannel</code>. In the above example, our provider can not handle such
     *       unknown source.</li>
     *   <li>Above example uses <code>ByteBuffer.getInt(int)</code> instead than <code>ByteBuffer.getInt()</code>
     *       in order to keep the buffer position unchanged after this method call.</li>
     *   <li>If the buffer does not contain enough bytes for the <code>int</code> type, this is not necessarily
     *       because the file is truncated. It may be because the data were not yet available at the time this
     *       method has been invoked. Returning <code>null</code> means "don't know".</li>
     * </ul>}
     *
     * Implementors are responsible for restoring the input to its original stream position on return of this method.
     * Implementors can use a mark/reset pair for this purpose. Marks are available as
     * {@link java.nio.ByteBuffer#mark()}, {@link java.io.InputStream#mark(int)} and
     * {@link javax.imageio.stream.ImageInputStream#mark()}.
     *
     * @param  storage Information about the storage (URL, stream, JDBC connection, <i>etc</i>).
     * @return A non-empty set if the given storage seems to be usable by the {@code DataStore} instances
     *         create by this provider, an empty set if the {@code DataStore} will not be able to use
     *         the given storage, or {@code null} if this method does not have enough information.
     * @throws DataStoreException if an I/O or SQL error occurred. The error shall be unrelated to the logical
     *         structure of the storage.
     *
     * @since 0.4
     */
    public abstract Set<OpenOption> getOpenCapabilities(StorageConnector storage) throws DataStoreException;

    /**
     * @deprecated Replaced by {@link #getOpenCapabilities(StorageConnector)}.
     */
    @Deprecated
    public Boolean canOpen(StorageConnector storage) throws DataStoreException {
        final Set<OpenOption> options = getOpenCapabilities(storage);
        return (options == null) ? null : options.contains(StandardOpenOption.READ);
    }

    /**
     * Returns a data store implementation associated with this provider.
     *
     * <p><b>Implementation note:</b>
     * Implementors shall invoke {@link StorageConnector#closeAllExcept(Object)} after {@code DataStore}
     * creation, keeping open only the needed resource.</p>
     *
     * @param  storage Information about the storage (URL, stream, JDBC connection, <i>etc</i>).
     * @return A data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    public abstract DataStore open(StorageConnector storage) throws DataStoreException;
}
