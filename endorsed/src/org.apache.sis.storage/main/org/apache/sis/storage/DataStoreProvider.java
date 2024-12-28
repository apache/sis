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

import java.io.Reader;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.imageio.stream.ImageInputStream;
import java.util.logging.Logger;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.metadata.distribution.Format;
import org.apache.sis.metadata.simple.SimpleFormat;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.distribution.DefaultFormat;
import org.apache.sis.io.stream.Markable;
import org.apache.sis.storage.base.URIDataStoreProvider;
import org.apache.sis.storage.internal.RewindableLineReader;
import org.apache.sis.measure.Range;
import org.apache.sis.util.Version;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;


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
 * <h2>Packaging data stores</h2>
 * JAR files that provide implementations of this class shall declare the implementation class names in
 * {@code module-info.java} as providers of the {@code org.apache.sis.storage.DataStoreProvider} service.
 * See {@link java.util.ServiceLoader} for more general discussion about this lookup mechanism.
 *
 * <h2>Thread safety</h2>
 * All {@code DataStoreProvider} implementations shall be thread-safe.
 * However, the {@code DataStore} instances created by the providers do not need to be thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.4
 * @since   0.3
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
     * Consequently, {@link java.io.InputStream} and {@link java.nio.channels.Channel} should be avoided.</p>
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
     * <h4>Relationship with standard file open options</h4>
     * For data stores on file systems, a <code>{@value} = true</code> parameter value is equivalent to opening a file
     * with {@link java.nio.file.StandardOpenOption#CREATE} and {@link java.nio.file.StandardOpenOption#APPEND APPEND}.
     * The other file standard options like {@link java.nio.file.StandardOpenOption#CREATE_NEW CREATE_NEW} and
     * {@link java.nio.file.StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING} should not be accessible through
     * this {@value} parameter. The reason is that {@link ParameterValueGroup} may be used for storing parameters
     * permanently (for example in a configuration file or in a database) for reopening the same {@link DataStore}
     * many times. File options designed for being used only once like {@code CREATE_NEW} and {@code TRUNCATE_EXISTING}
     * are incompatible with this usage.
     *
     * @see #LOCATION
     * @see #getOpenParameters()
     */
    public static final String CREATE = "create";

    /**
     * Creates a new provider.
     */
    protected DataStoreProvider() {
    }

    /**
     * Returns a short name or abbreviation for the data format.
     * This name is used in some warnings or exception messages.
     * It may contain any characters, including white spaces, and is not guaranteed to be unique.
     * For a more comprehensive format name, see {@link #getFormat()}.
     *
     * <h4>Examples</h4>
     * {@code "CSV"}, {@code "GeoTIFF"}, {@code "GML"}, {@code "GPX"}, {@code "JPEG"}, {@code "JPEG 2000"},
     * {@code "NetCDF"}, {@code "PNG"}, {@code "Shapefile"}.
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
     *           (example: <q>PNG (Portable Network Graphics) Specification</q>),</li>
     *       <li>the format {@linkplain #getShortName() short name} as a citation
     *           {@linkplain DefaultCitation#getAlternateTitles() alternate title}
     *           (example: <q>PNG</q>),</li>
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
     * Those parameters will be recognized by the default {@code DataStoreProvider} methods and used whenever a
     * {@link StorageConnector} is required.</p>
     *
     * <h4>Alternative</h4>
     * The main differences between the use of {@code StorageConnector} and parameters are:
     * <ul class="verbose">
     *   <li>{@code StorageConnector} is designed for use with file or stream of unknown format;
     *       the format is automatically detected. By contrast, the use of parameters require to
     *       determine the format first (i.e. select a {@code DataStoreProvider}).</li>
     *   <li>Parameters can be used to dynamically generate user configuration interfaces
     *       and provide fine grain control over the store general behavior such as caching,
     *       time-outs, encoding, <i>etc</i>.</li>
     *   <li>Parameters can more easily be serialized in XML or configuration files.</li>
     * </ul>
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
     * Implementations will typically check the first bytes of the input stream for a "magic number" associated
     * with the format. The most typical return values are:
     *
     * <ul>
     *   <li>{@link ProbeResult#SUPPORTED} or another instance with {@linkplain ProbeResult#isSupported() supported}
     *       status if the {@code DataStore}s created by this provider can open the given storage.</li>
     *   <li>{@link ProbeResult#UNSUPPORTED_STORAGE} if the given storage does not appear to be in a format
     *       supported by this {@code DataStoreProvider}.</li>
     * </ul>
     *
     * Note that the {@code SUPPORTED} value does not guarantee that reading or writing will succeed,
     * only that there appears to be a reasonable chance of success based on a brief inspection of the
     * {@linkplain StorageConnector#getStorage() storage object} or contents.
     *
     * <h4>Note for implementers</h4>
     * Implementations are responsible for restoring the storage object to its original position
     * on return of this method. Implementers can use the mark/reset mechanism for this purpose.
     * Marks are available as {@link java.nio.ByteBuffer#mark()}, {@link java.io.InputStream#mark(int)}
     * and {@link javax.imageio.stream.ImageInputStream#mark()}.
     * Alternatively, the {@link #probeContent(StorageConnector, Class, Prober)}
     * helper method manages automatically the marks for a set of known types.
     *
     * @param  connector  information about the storage (URL, stream, JDBC connection, <i>etc</i>).
     * @return a {@linkplain ProbeResult#isSupported() supported} status if the given storage
     *         seems to be readable by the {@code DataStore} instances created by this provider.
     * @throws DataStoreException if an I/O or SQL error occurred. The error shall be unrelated to the logical
     *         structure of the storage.
     */
    public abstract ProbeResult probeContent(StorageConnector connector) throws DataStoreException;

    /**
     * Applies the specified test on the storage content without modifying buffer or input stream position.
     * This is a helper method for {@link #probeContent(StorageConnector)} implementations,
     * providing an alternative safer than {@link StorageConnector#getStorageAs(Class)}
     * for performing an arbitrary number of independent tests on the same {@code StorageConnector}.
     * Current implementation accepts the following types (this list may be expanded in future versions):
     *
     * <blockquote>
     * {@link java.nio.ByteBuffer} (default byte order fixed to {@link java.nio.ByteOrder#BIG_ENDIAN BIG_ENDIAN}),
     * {@link java.io.InputStream},
     * {@link java.io.DataInput},
     * {@link javax.imageio.stream.ImageInputStream} and
     * {@link java.io.Reader}.
     * </blockquote>
     *
     * The following types are also accepted for completeness but provide no additional safety
     * compared to direct use of {@link StorageConnector#getStorageAs(Class)}:
     *
     * <blockquote>
     * {@link java.net.URI},
     * {@link java.net.URL},
     * {@link java.io.File},
     * {@link java.nio.file.Path} and
     * {@link String} (interpreted as a file path).
     * </blockquote>
     *
     * This method {@linkplain InputStream#mark(int) marks} and {@linkplain InputStream#reset() resets}
     * streams automatically with an arbitrary read-ahead limit (typically okay for the first 8 kilobytes).
     *
     * <h4>Usage example</h4>
     * {@link #probeContent(StorageConnector)} implementations often check the first bytes of the
     * input stream for a "magic number" associated with the format, as in the following example:
     *
     * {@snippet lang="java" :
     *     @Override
     *     public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
     *         return probeContent(connector, ByteBuffer.class, (buffer) -> {
     *             if (buffer.remaining() >= Integer.BYTES) {
     *                 if (buffer.getInt() == MAGIC_NUMBER) {
     *                     return ProbeResult.SUPPORTED;
     *                 }
     *                 return ProbeResult.UNSUPPORTED_STORAGE;
     *             }
     *             // If the buffer does not contain enough bytes for the integer type, this is not
     *             // necessarily because the file is truncated. It may be because the data were not
     *             // yet available at the time this method has been invoked.
     *             return ProbeResult.INSUFFICIENT_BYTES;
     *         });
     *     }
     * }
     *
     * @param  <S>        the compile-time type of the {@code type} argument (the source or storage type).
     * @param  connector  information about the storage (URL, stream, JDBC connection, <i>etc</i>).
     * @param  type       the desired type as one of {@code ByteBuffer}, {@code DataInput}, {@code Connection}
     *                    class or other {@linkplain StorageConnector#getStorageAs(Class) documented types}.
     * @param  prober     the test to apply on the source of the given type.
     * @return the result of executing the probe action with a source of the given type,
     *         or {@link ProbeResult#UNSUPPORTED_STORAGE} if the given type is supported
     *         but no view can be created for the source given at construction time.
     * @throws IllegalArgumentException if the given {@code type} argument is not one of the supported types.
     * @throws IllegalStateException if this {@code StorageConnector} has been {@linkplain StorageConnector#closeAllExcept closed}.
     * @throws DataStoreException if an error occurred while opening a stream or database connection,
     *         or during the execution of the probe action.
     *
     * @see #probeContent(StorageConnector)
     * @see StorageConnector#getStorageAs(Class)
     *
     * @since 1.2
     */
    protected <S> ProbeResult probeContent(final StorageConnector connector,
            final Class<S> type, final Prober<? super S> prober) throws DataStoreException
    {
        ArgumentChecks.ensureNonNull("prober", prober);
        boolean undetermined;
        /*
         * Synchronization is not a documented feature for now because the policy may change in future version.
         * Current version uses the storage source as the synchronization lock because using `StorageConnector`
         * as the lock is not sufficient; the stream may be in use outside the connector. We have no way to know
         * which lock (if any) is used by the source. But `InputStream` for example uses `this`.
         */
        synchronized (connector.storage) {
            ProbeResult result = tryProber(connector, type, prober);
            undetermined = (result == ProbeResult.UNDETERMINED);
            if (result != null && !undetermined) {
                return result;
            }
            /*
             * If the storage connector cannot provide the type of source required by the specified prober,
             * verify if there is any other probers specified by `Prober.orElse(…)`.
             */
            Prober<?> next = prober;
            while (next instanceof ProberList<?,?>) {
                final var list = (ProberList<?,?>) next;
                result = tryNextProber(connector, list);
                if (result != null && result != ProbeResult.UNDETERMINED) {
                    return result;
                }
                undetermined |= (result == ProbeResult.UNDETERMINED);
                next = list.next;
            }
        }
        return undetermined ? ProbeResult.UNDETERMINED : ProbeResult.UNSUPPORTED_STORAGE;
    }

    /**
     * Tries the {@link ProberList#next} probe. This method is defined for type parameterization
     * (the caller has only {@code <?>} and we need a specific type {@code <N>}).
     *
     * @param  <N>        type of input requested by the next probe.
     * @param  connector  information about the storage (URL, stream, JDBC connection, <i>etc</i>).
     * @param  list       root of the chained list of next probes.
     */
    private <N> ProbeResult tryNextProber(final StorageConnector connector, final ProberList<?,N> list) throws DataStoreException {
        return tryProber(connector, list.type, list.next);
    }

    /**
     * Implementation of {@link #probeContent(StorageConnector, Class, Prober)}
     * for a single element in a list of probe.
     *
     * @param  <S>        the compile-time type of the {@code type} argument (the source or storage type).
     * @param  connector  information about the storage (URL, stream, JDBC connection, <i>etc</i>).
     * @param  type       the desired type as one of {@code ByteBuffer}, {@code DataInput}, <i>etc</i>.
     * @param  prober     the test to apply on the source of the given type.
     * @return the result of executing the probe action with a source of the given type,
     *         or {@code null} if the given type is supported but no view can be created.
     * @throws IllegalArgumentException if the given {@code type} argument is not one of the supported types.
     * @throws IllegalStateException if this {@code StorageConnector} has been {@linkplain #closeAllExcept closed}.
     * @throws DataStoreException if another kind of error occurred.
     */
    private <S> ProbeResult tryProber(final StorageConnector connector,
            final Class<S> type, final Prober<? super S> prober) throws DataStoreException
    {
        final S input = connector.getStorageAs(type);
        if (input == null) {
            /*
             * Means one of the following:
             *   - The storage is a file that do not exist yet but can be created by this provider.
             *   - The given type is valid but not applicable with the `StorageConnector` content.
             */
            if (connector.probing != null) {
                return connector.probing.probe;
            }
            return null;
        }
        if (input == connector.storage && !StorageConnector.isSupportedType(type)) {
            /*
             * The given type is not one of the types known to `StorageConnector` (the list of supported types
             * is hard-coded). We could give the input as-is to the prober, but we have no idea how to fulfill
             * the method contract saying that the use of the input is safe. We throw an exception for telling
             * to the users that they should manage the input themselves.
             */
            throw new IllegalArgumentException(Errors.format(Errors.Keys.UnsupportedType_1, type));
        }
        ProbeResult result = null;
        try {
            if (input instanceof ByteBuffer) {
                /*
                 * No need to save buffer position because `asReadOnlyBuffer()` creates an independent buffer
                 * with its own mark and position. Byte order of the view is intentionally fixed to BIG_ENDIAN
                 * (the default) regardless the byte order of the original buffer.
                 */
                final var buffer = (ByteBuffer) input;
                result = prober.test(type.cast(buffer.asReadOnlyBuffer()));
            } else if (input instanceof Markable) {
                /*
                 * `Markable` stream can nest an arbitrary number of marks. So we allow users to create
                 * their own marks. In principle a single call to `reset()` is enough, but we check the
                 * position in case the user has done some marks without resets.
                 */
                final var stream = (Markable) input;
                final long position = stream.getStreamPosition();
                stream.mark();
                result = prober.test(input);
                stream.reset(position);
            } else if (input instanceof ImageInputStream) {
                /*
                 * `ImageInputStream` supports an arbitrary number of marks as well,
                 * but we use absolute positioning for simplicity.
                 */
                final var stream = (ImageInputStream) input;
                final long position = stream.getStreamPosition();
                result = prober.test(input);
                stream.seek(position);
            } else if (input instanceof InputStream) {
                /*
                 * `InputStream` supports at most one mark. So we keep it for ourselves
                 * and wrap the stream in an object that prevent users from using marks.
                 */
                final var stream = new ProbeInputStream(connector, (InputStream) input);
                result = prober.test(type.cast(stream));
                stream.close();     // No "try with resource". See `ProbeInputStream.close()` contract.
            } else if (input instanceof RewindableLineReader) {
                /*
                 * `Reader` supports at most one mark. So we keep it for ourselves and prevent users
                 * from using marks, but without wrapper if we can safely expose a `BufferedReader`
                 * (because users may want to use the `BufferedReader.readLine()` method).
                 */
                final var r = (RewindableLineReader) input;
                r.protectedMark();
                result = prober.test(input);
                r.protectedReset();
            } else if (input instanceof Reader) {
                final var stream = new ProbeReader(connector, (Reader) input);
                result = prober.test(type.cast(stream));
                stream.close();     // No "try with resource". See `ProbeReader.close()` contract.
            } else {
                /*
                 * All other cases are objects like File, URL, etc. which can be used without mark/reset.
                 * Note that if the type was not known to be safe, an exception would have been thrown at
                 * the beginning of this method.
                 */
                result = prober.test(input);
            }
        } catch (DataStoreException e) {
            throw e;
        } catch (Exception e) {
            final String message = Errors.format(Errors.Keys.CanNotRead_1, connector.getStorageName());
            if (result != null) {
                throw new ForwardOnlyStorageException(message, e);
            }
            throw new CanNotProbeException(this, connector, e);
        }
        return result;
    }

    /**
     * An action to execute for testing if a {@link StorageConnector} input can be read.
     * This action is invoked by {@link #probeContent(StorageConnector, Class, Prober)}
     * with an input of the type {@code <S>} specified to the {@code probe(…)} method.
     * The {@code DataStoreProvider} is responsible for restoring the input to its initial position
     * after the probe action completed.
     *
     * @param  <S>  the source type as one of {@code ByteBuffer}, {@code DataInput} or other classes
     *              documented in {@link StorageConnector#getStorageAs(Class)}.
     *
     * @version 1.2
     *
     * @see #probeContent(StorageConnector, Class, Prober)
     *
     * @since 1.2
     */
    @FunctionalInterface
    protected interface Prober<S> {
        /**
         * Probes the given input and returns an indication about whether that input is supported.
         * This method may return {@code SUPPORTED} if there is reasonable chance of success based
         * on a brief inspection of the given input;
         * the supported status does not need to be guaranteed.
         *
         * @param  input  the input to probe. This is for example a {@code ByteBuffer} or a {@code DataInput}.
         * @return the result of executing the probe action with the given source. Should not be null.
         * @throws Exception if an error occurred during the execution of the probe action.
         */
        ProbeResult test(S input) throws Exception;

        /**
         * Returns a composed probe that attempts, in sequence, this probe followed by the alternative probe
         * if the first probe cannot be executed. The alternative probe is tried if and only if one of the
         * following conditions is true:
         *
         * <ul>
         *   <li>The storage connector cannot provide an input of the type requested by this probe.</li>
         *   <li>This probe {@link #test(S)} method returned {@link ProbeResult#UNDETERMINED}.</li>
         * </ul>
         *
         * If any probe throws an exception, the exception is propagated
         * (the alternative probe is not a fallback executed if this probe threw an exception).
         *
         * @param  <A>          the compile-time type of the {@code type} argument (the source or storage type).
         * @param  type         the desired type as one of {@code ByteBuffer}, {@code DataInput}, <i>etc</i>.
         * @param  alternative  the test to apply on the source of the given type.
         * @return a composed probe that attempts the given probe if this probe cannot be executed.
         */
        default <A> Prober<S> orElse(final Class<A> type, final Prober<? super A> alternative) {
            return new ProberList<>(this, type, alternative);
        }
    }

    /**
     * Implementation of the composed probe returned by {@link Prober#orElse(Class, Prober)}.
     * Instances of this class a nodes in a linked list.
     *
     * @param <S>  the source type of the original probe.
     * @param <N>  the source type of the next probe to try as an alternative.
     */
    private static final class ProberList<S,N> implements Prober<S> {
        /** The main probe to try first. */
        private final Prober<S> first;

        /** The probe to try next if the {@linkplain #first} probe cannot be executed. */
        Prober<? super N> next;

        /** Type of input expected by the {@linkplain #next} probe. */
        final Class<N> type;

        /** Creates a new composed probe as a root node of a linked list. */
        ProberList(final Prober<S> first, final Class<N> type, final Prober<? super N> next) {
            this.first = first;
            this.type  = type;
            this.next  = next;
        }

        /** Forward to the primary probe. */
        @Override public ProbeResult test(final S input) throws Exception {
            return first.test(input);
        }

        /** Appends a new probe alternative at the end of this linked list. */
        @Override public <A> Prober<S> orElse(final Class<A> type, final Prober<? super A> prober) {
            next = next.orElse(type, prober);
            return this;
        }
    }

    /**
     * Creates a data store instance associated with this provider.
     * This method is typically invoked when the format is not known in advance
     * (the {@link #probeContent(StorageConnector)} method can be tested on many providers)
     * or when the input is not a type accepted by {@link #open(ParameterValueGroup)}
     * (for example an {@link java.io.InputStream}).
     *
     * <h4>Implementation note</h4>
     * Implementers shall invoke {@link StorageConnector#closeAllExcept(Object)} after {@code DataStore}
     * creation, keeping open only the needed resource.
     *
     * @param  connector  information about the storage (URL, stream, JDBC connection, <i>etc</i>).
     * @return a data store instance associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     *
     * @see DataStores#open(Object)
     */
    public abstract DataStore open(StorageConnector connector) throws DataStoreException;

    /**
     * Creates a data store instance associated with this provider for the given parameters.
     * The {@code DataStoreProvider} instance needs to be known before parameters are initialized,
     * since the parameters are implementation-dependent. Example:
     *
     * {@snippet lang="java" :
     *     DataStoreProvider provider = ...;
     *     ParameterValueGroup pg = provider.getOpenParameters().createValue();
     *     pg.parameter(DataStoreProvider.LOCATION, myURL);
     *     // Set any other parameters if desired.
     *     try (DataStore ds = provider.open(pg)) {
     *         // Use the data store.
     *     }
     *     }
     *
     * <h4>Implementation note</h4>
     * The default implementation gets the value of a parameter named {@value #LOCATION}.
     * That value (typically a path or URL) is given to {@link StorageConnector} constructor,
     * which is then passed to {@link #open(StorageConnector)}.
     *
     * @param  parameters  opening parameters as defined by {@link #getOpenParameters()}.
     * @return a data store instance associated with this provider for the given parameters.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     *
     * @see #LOCATION
     * @see #CREATE
     * @see #getOpenParameters()
     *
     * @since 0.8
     */
    public DataStore open(final ParameterValueGroup parameters) throws DataStoreException {
        // IllegalOpenParameterException thrown if `parameters` is null.
        return open(URIDataStoreProvider.connector(this, parameters));
    }

    /**
     * Returns the logger where to report warnings or loading operations.
     * This logger is used only if no {@link org.apache.sis.storage.event.StoreListener}
     * has been registered for {@link org.apache.sis.storage.event.WarningEvent}.
     *
     * <p>The default implementation returns a logger with the same name as the package name
     * of the subclass of this {@code DataStoreProvider} instance. Subclasses should override
     * this method if they can provide a more specific logger.</p>
     *
     * @return the logger to use as a fallback (when there are no listeners) for warning messages.
     *
     * @see org.apache.sis.storage.event.StoreListeners#getLogger()
     *
     * @since 1.0
     */
    public Logger getLogger() {
        return Logging.getLogger(getClass());
    }
}
