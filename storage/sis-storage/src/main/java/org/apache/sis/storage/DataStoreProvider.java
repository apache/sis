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
 * Creates {@link DataStore} instances for a specific format from a given {@link DataStoreConnection} input.
 * There is typically a different {@code DataStoreProvider} instance for each format provided by a library.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public abstract class DataStoreProvider {
    /**
     * Creates a new provider.
     */
    protected DataStoreProvider() {
    }

    /**
     * Returns {@code TRUE} if the given storage appears to be supported by the {@code DataStore}.
     * Returning {@code TRUE} from this method does not guarantee that reading or writing will succeed,
     * only that there appears to be a reasonable chance of success based on a brief inspection of the
     * {@linkplain DataStoreConnection#getStorage() storage object} or contents.
     *
     * <p>Implementations will typically check the first bytes of the stream for a "magic number"
     * associated with the format, as in the following example:</p>
     *
     * {@preformat java
     *     final ByteBuffer buffer = storage.getStorageAs(ByteBuffer.class);
     *     if (buffer == null) {
     *         // If DataStoreConnection can not provide a ByteBuffer, then the storage is probably
     *         // not a File, URL, URI, InputStream neither a ReadableChannel. In this example, our
     *         // provider can not handle such unknown source.
     *         return Boolean.FALSE;
     *     }
     *     if (buffer.remaining() < Integer.SIZE / Byte.SIZE) {
     *         // If the buffer does not contain enough bytes for the 'int' type, this is not necessarily
     *         // because the file is truncated. It may be because the data were not yet available at the
     *         // time this method has been invoked. Returning 'null' means "don't know".
     *         return null;
     *     }
     *     // Use ByteBuffer.getInt(int) instead than ByteBuffer.getInt() in order to keep buffer position
     *     // unchanged after this method call.
     *     return buffer.getInt(buffer.position()) == MAGIC_NUMBER;
     * }
     *
     * Implementors are responsible for restoring the input to its original stream position on return of this method.
     * Implementors can use a mark/reset pair for this purpose. Marks are available as
     * {@link java.nio.ByteBuffer#mark()}, {@link java.io.InputStream#mark(int)} and
     * {@link javax.imageio.stream.ImageInputStream#mark()}.
     *
     * @param  storage Information about the storage (URL, stream, JDBC connection, <i>etc</i>).
     * @return {@link Boolean#TRUE} if the given storage seems to be usable by the {@code DataStore} instances
     *         create by this provider, {@link Boolean#FALSE} if the {@code DataStore} will not be able to use
     *         the given storage, or {@code null} if this method does not have enough information.
     * @throws DataStoreException if an I/O or SQL error occurred. The error shall be unrelated to the logical
     *         structure of the storage.
     */
    public abstract Boolean canOpen(DataStoreConnection storage) throws DataStoreException;

    /**
     * Returns a data store implementation associated with this provider.
     *
     * <p><b>Implementation note:</b>
     * Implementors shall invoke {@link DataStoreConnection#closeAllExcept(Object)} after {@code DataStore}
     * creation, keeping open only the needed resource.</p>
     *
     * @param  storage Information about the storage (URL, stream, JDBC connection, <i>etc</i>).
     * @return A data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    public abstract DataStore open(DataStoreConnection storage) throws DataStoreException;
}
