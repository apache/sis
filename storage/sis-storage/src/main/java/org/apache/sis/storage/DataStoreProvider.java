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

import java.util.logging.Logger;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.metadata.distribution.Format;
import org.apache.sis.internal.simple.SimpleFormat;
import org.apache.sis.internal.storage.URIDataStore;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.distribution.DefaultFormat;
import org.apache.sis.measure.Range;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.Version;


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
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
public abstract class DataStoreProvider {
    /**
     * Name of the parameter that specifies the data store location.
     * A parameter named {@value} should be included in the group of parameters returned by {@link #getOpenParameters()}.
     * The parameter value is often a {@link java.net.URI} or a {@link java.nio.file.Path}, but other types are allowed.
     *
     * <p>Implementers are encouraged to define a parameter with this name
     * to ensure a common and consistent definition among providers.
     * The parameter should be defined as mandatory and typed with a well-known Java class such as
     * {@link java.net.URI}, {@link java.nio.file.Path}, JDBC {@linkplain javax.sql.DataSource}, <i>etc</i>.
     * The type should have a compact textual representation, for serialization in XML or configuration files.
     * Consequently {@link java.io.InputStream} and {@link java.nio.channels.Channel} should be avoided.</p>
     *
     * @see #CREATE
     * @see #getOpenParameters()
     */
    public static final String LOCATION = "location";

    /**
     * Name of the parameter that specifies whether to allow creation of a new {@code DataStore} if none exist
     * at the given location. A parameter named {@value} may be included in the group of parameters returned by
     * {@link #getOpenParameters()} if the data store supports write operations. The parameter value is often a
     * {@link Boolean} and the default value should be {@link Boolean#FALSE} or equivalent.
     *
     * <p>Implementers are encouraged to define an <em>optional</em> parameter with this name in complement to the
     * {@value #LOCATION} parameter <em>only if</em> write operations are supported. If this parameter value is not
     * set or is set to {@code false}, then the {@link #open(ParameterValueGroup)} method should fail if no file or
     * database exists at the URL or path given by the {@value #LOCATION} parameter. Otherwise if this parameter is
     * set to {@code true}, then the {@code open(…)} method may create files, a directory or a database at the given
     * location.</p>
     *
     * <div class="note"><b>Relationship with standard file open options</b>
     * <p>For data stores on file systems, a <code>{@value} = true</code> parameter value is equivalent to opening a file
     * with {@link java.nio.file.StandardOpenOption#CREATE} and {@link java.nio.file.StandardOpenOption#APPEND APPEND}.
     * The other file standard options like {@link java.nio.file.StandardOpenOption#CREATE_NEW CREATE_NEW} and
     * {@link java.nio.file.StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING} should not be accessible through
     * this {@value} parameter. The reason is that {@link ParameterValueGroup} may be used for storing parameters
     * permanently (for example in a configuration file or in a database) for reopening the same {@link DataStore}
     * many times. File options designed for being used only once like {@code CREATE_NEW} and {@code TRUNCATE_EXISTING}
     * are incompatible with this usage.</p></div>
     *
     * @see #LOCATION
     * @see #getOpenParameters()
     */
    public static final String CREATE = "create";

    /**
     * The logger where to reports warnings or change events. Created when first needed and kept
     * by strong reference for avoiding configuration lost if the logger if garbage collected.
     * This strategy assumes that {@code URIDataStore.Provider} instances are kept alive for
     * the duration of JVM lifetime, which is the case with {@link DataStoreRegistry}.
     *
     * @see #getLogger()
     */
    private volatile Logger logger;

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
     * Returns the range of versions supported by the data store, or {@code null} if unspecified.
     *
     * @return the range of supported versions, or {@code null} if unspecified.
     *
     * @since 0.8
     */
    public Range<Version> getSupportedVersions() {
        return null;
    }

    /**
     * Returns a description of all parameters accepted by this provider for opening a data store.
     * Those parameters provide an alternative to {@link StorageConnector} for opening a {@link DataStore}
     * from a path or URL, together with additional information like character encoding.
     *
     * <p>Implementers are responsible for declaring all parameters and whether they are mandatory or optional.
     * It is recommended to define at least a parameter named {@value #LOCATION}, completed by {@value #CREATE}
     * if the data store supports write operations.
     * That parameter will be recognized by the default {@code DataStoreProvider} methods and used whenever a
     * {@link StorageConnector} is required.</p>
     *
     * <div class="note"><b>Alternative:</b>
     * the main differences between the use of {@code StorageConnector} and parameters are:
     * <ul class="verbose">
     *   <li>{@code StorageConnector} is designed for use with file or stream of unknown format;
     *       the format is automatically detected. By contrast, the use of parameters require to
     *       determine the format first (i.e. select a {@code DataStoreProvider}).</li>
     *   <li>Parameters can be used to dynamically generate user configuration interfaces
     *       and provide fine grain control over the store general behavior such as caching,
     *       time-outs, encoding, <i>etc</i>.</li>
     *   <li>Parameters can more easily be serialized in XML or configuration files.</li>
     * </ul></div>
     *
     * @return description of the parameters required or accepted for opening a {@link DataStore}.
     *
     * @see #LOCATION
     * @see #CREATE
     * @see #open(ParameterValueGroup)
     * @see DataStore#getOpenParameters()
     *
     * @since 0.8
     */
    public abstract ParameterDescriptorGroup getOpenParameters();

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
     * <p>Implementers are responsible for restoring the input to its original stream position on return of this method.
     * Implementers can use a mark/reset pair for this purpose. Marks are available as
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
     * This method is typically invoked when the format is not known in advance
     * (the {@link #probeContent(StorageConnector)} method can be tested on many providers)
     * or when the input is not a type accepted by {@link #open(ParameterValueGroup)}
     * (for example an {@link java.io.InputStream}).
     *
     * <div class="section">Implementation note</div>
     * Implementers shall invoke {@link StorageConnector#closeAllExcept(Object)} after {@code DataStore}
     * creation, keeping open only the needed resource.
     *
     * @param  connector  information about the storage (URL, stream, JDBC connection, <i>etc</i>).
     * @return a data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     *
     * @see DataStores#open(Object)
     */
    public abstract DataStore open(StorageConnector connector) throws DataStoreException;

    /**
     * Returns a data store implementation associated with this provider for the given parameters.
     * The {@code DataStoreProvider} instance needs to be known before parameters are initialized,
     * since the parameters are implementation-dependent. Example:
     *
     * {@preformat java
     *     DataStoreProvider provider = ...;
     *     ParameterValueGroup pg = provider.getOpenParameters().createValue();
     *     pg.parameter(DataStoreProvider.LOCATION, myURL);
     *     // Set any other parameters if desired.
     *     try (DataStore ds = provider.open(pg)) {
     *         // Use the data store.
     *     }
     * }
     *
     * <div class="section">Implementation note</div>
     * The default implementation gets the value of a parameter named {@value #LOCATION}.
     * That value (typically a path or URL) is given to {@link StorageConnector} constructor,
     * which is then passed to {@link #open(StorageConnector)}.
     *
     * @param  parameters  opening parameters as defined by {@link #getOpenParameters()}.
     * @return a data store implementation associated with this provider for the given parameters.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     *
     * @see #LOCATION
     * @see #CREATE
     * @see #getOpenParameters()
     *
     * @since 0.8
     */
    public DataStore open(final ParameterValueGroup parameters) throws DataStoreException {
        ArgumentChecks.ensureNonNull("parameter", parameters);
        return open(URIDataStore.Provider.connector(this, parameters));
    }

    /**
     * Returns the logger where to report warnings. This logger is used only if no
     * {@link org.apache.sis.storage.event.StoreListener} has been registered for
     * {@link org.apache.sis.storage.event.WarningEvent}.
     *
     * <p>The default implementation returns a logger with the same name as the package name
     * of the subclass of this {@code DataStoreProvider} instance. Subclasses should override
     * this method if they can provide a more specific logger.</p>
     *
     * @return the logger to use as a fallback (when there is no listeners) for warning messages.
     *
     * @since 1.0
     */
    public Logger getLogger() {
        Logger lg = logger;
        if (lg == null) {
            logger = lg = Logging.getLogger(getClass());
        }
        return lg;
    }
}
