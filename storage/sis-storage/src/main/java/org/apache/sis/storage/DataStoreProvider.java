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

import org.opengis.metadata.distribution.Format;
import org.apache.sis.internal.simple.SimpleFormat;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.distribution.DefaultFormat;


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
 * @version 0.8
 * @module
 */
public abstract class DataStoreProvider {
    /**
     * Creates a new provider.
     */
    protected DataStoreProvider() {
    }

    /**
     * Returns a short name or abbreviation for the data format.
     * This name is used in some warnings or exception messages.
     * It may contain any characters, including white spaces
     * (i.e. this short name is <strong>not</strong> a format identifier).
     *
     * <div class="note"><b>Examples:</b>
     * {@code "CSV"}, {@code "GeoTIFF"}, {@code "GML"}, {@code "GPX"}, {@code "JPEG"}, {@code "JPEG 2000"},
     * {@code "NetCDF"}, {@code "PNG"}, {@code "Shapefile"}.
     * </div>
     *
     * For a more comprehensive format name, see {@link #getFormat()}.
     *
     * @return a short name or abbreviation for the data format.
     *
     * @see #getFormat()
     *
     * @since 0.8
     */
    public abstract String getShortName();

    /**
     * Returns a description of the data format. The description should contain (if available):
     *
     * <ul>
     *   <li>A reference to the {@linkplain DefaultFormat#getFormatSpecificationCitation()
     *       format specification citation}, including:
     *     <ul>
     *       <li>a format specification {@linkplain DefaultCitation#getTitle() title}
     *           (example: <cite>“PNG (Portable Network Graphics) Specification”</cite>),</li>
     *       <li>the format {@linkplain #getShortName() short name} as a citation
     *           {@linkplain DefaultCitation#getAlternateTitles() alternate title}
     *           (example: <cite>“PNG”</cite>),</li>
     *       <li>the format version as the citation {@linkplain DefaultCitation#getEdition() edition},</li>
     *       <li>link to an {@linkplain DefaultCitation#getOnlineResources() online} version of the specification.</li>
     *     </ul>
     *   </li>
     *   <li>The title of the {@linkplain DefaultFormat#getFileDecompressionTechnique() file decompression technique}
     *       used for reading the data.</li>
     * </ul>
     *
     * The default implementation returns a format containing only the value returned by {@link #getShortName()}.
     * Subclasses are encouraged to override this method for providing a more complete description, if available.
     *
     * @return a description of the data format.
     *
     * @see #getShortName()
     * @see DefaultFormat
     *
     * @since 0.8
     */
    public Format getFormat() {
        return new SimpleFormat(getShortName());
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
     * @param  connector information about the storage (URL, stream, JDBC connection, <i>etc</i>).
     * @return {@link ProbeResult#SUPPORTED} if the given storage seems to be readable by the {@code DataStore}
     *         instances created by this provider.
     * @throws DataStoreException if an I/O or SQL error occurred. The error shall be unrelated to the logical
     *         structure of the storage.
     */
    public abstract ProbeResult probeContent(StorageConnector connector) throws DataStoreException;

    /**
     * Returns a data store implementation associated with this provider.
     *
     * <div class="section">Implementation note</div>
     * Implementors shall invoke {@link StorageConnector#closeAllExcept(Object)} after {@code DataStore}
     * creation, keeping open only the needed resource.
     *
     * @param  connector  information about the storage (URL, stream, JDBC connection, <i>etc</i>).
     * @return a data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     *
     * @see DataStores#open(Object)
     */
    public abstract DataStore open(StorageConnector connector) throws DataStoreException;
}
