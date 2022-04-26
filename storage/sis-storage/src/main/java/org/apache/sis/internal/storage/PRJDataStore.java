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
package org.apache.sis.internal.storage;

import java.net.URL;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.net.UnknownServiceException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.NoSuchFileException;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.storage.DataOptionKey;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.internal.storage.io.IOUtilities;
import org.apache.sis.internal.storage.wkt.StoreFormat;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Classes;


/**
 * A data store for a file or URI accompanied by an auxiliary file of the same name with {@code .prj} extension.
 * If the auxiliary file is absent, {@link DataOptionKey#DEFAULT_CRS} is used as a fallback.
 * The WKT 1 variant used for parsing the {@code "*.prj"} file is the variant used by "World Files" and GDAL;
 * this is not the standard specified by OGC 01-009 (they differ in there interpretation of units of measurement).
 *
 * <p>It is still possible to create a data store with a {@link java.nio.channels.ReadableByteChannel},
 * {@link java.io.InputStream} or {@link java.io.Reader}, in which case the {@linkplain #location} will
 * be null and the CRS defined by the {@code DataOptionKey} will be used.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public abstract class PRJDataStore extends URIDataStore {
    /**
     * Maximal length (in bytes) of auxiliary files. This is an arbitrary restriction, we could let
     * the buffer growth indefinitely instead. But a large auxiliary file is probably an error and
     * we do not want an {@link OutOfMemoryError} because of that.
     */
    private static final int MAXIMAL_LENGTH = 64 * 1024;

    /**
     * The filename extension of {@code "*.prj"} files.
     *
     * @see #getComponentFiles()
     */
    protected static final String PRJ = "prj";

    /**
     * Character encoding in {@code *.prj} or other auxiliary files,
     * or {@code null} for the JVM default (usually UTF-8).
     */
    protected final Charset encoding;

    /**
     * The locale for texts in {@code *.prj} or other auxiliary files,
     * or {@code null} for {@link Locale#ROOT} (usually English).
     * This locale is <strong>not</strong> used for parsing numbers or dates.
     */
    private final Locale locale;

    /**
     * Timezone for dates in {@code *.prj} or other auxiliary files,
     * or {@code null} for UTC.
     */
    private final TimeZone timezone;

    /**
     * The coordinate reference system. This is initialized on the value provided by {@link DataOptionKey#DEFAULT_CRS}
     * at construction time, and is modified later if a {@code "*.prj"} file is found.
     */
    protected CoordinateReferenceSystem crs;

    /**
     * Creates a new data store. This constructor does not open the file,
     * so subclass constructors can decide whether to open in read-only or read/write mode.
     *
     * <p>The following options are recognized:</p>
     * <ul>
     *   <li>{@link DataOptionKey#DEFAULT_CRS}: default CRS if no auxiliary {@code "*.prj"} file is found.</li>
     *   <li>{@link DataOptionKey#ENCODING}: encoding of the {@code "*.prj"} file. Default is the JVM default.</li>
     *   <li>{@link DataOptionKey#TIMEZONE}: timezone of dates in the {@code "*.prj"} file. Default is UTC.</li>
     *   <li>{@link DataOptionKey#LOCALE}: locale for texts in the {@code "*.prj"} file. Default is English.</li>
     * </ul>
     *
     * @param  provider   the factory that created this {@code PRJDataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, reader instance, <i>etc</i>).
     * @throws DataStoreException if an error occurred while creating the data store for the given storage.
     */
    protected PRJDataStore(final DataStoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(provider, connector);
        crs      = connector.getOption(DataOptionKey.DEFAULT_CRS);
        encoding = connector.getOption(DataOptionKey.ENCODING);
        locale   = connector.getOption(DataOptionKey.LOCALE);       // For `InternationalString`, not for numbers.
        timezone = connector.getOption(DataOptionKey.TIMEZONE);
    }

    /**
     * Reads the {@code "*.prj"} auxiliary file. If the file is not found, then this method does nothing
     * and {@link #crs} keeps its current value (usually the default value found at construction time).
     *
     * <p>This method does not verify if it has been invoked multiple time.
     * Caller should track whether the data store has been initialized.</p>
     *
     * @throws DataStoreException if an error occurred while reading the file.
     */
    protected final void readPRJ() throws DataStoreException {
        Exception cause = null;
        try {
            final String wkt = readAuxiliaryFile(PRJ).toString();
            final StoreFormat format = new StoreFormat(locale, timezone, null, listeners);
            format.setConvention(Convention.WKT1_COMMON_UNITS);         // Ignored if the format is WKT 2.
            final ParsePosition pos = new ParsePosition(0);
            crs = (CoordinateReferenceSystem) format.parse(wkt, pos);
            if (crs != null) {
                /*
                 * Some characters may exist after the WKT definition. For example we sometime see the CRS
                 * defined twice: as a WKT on the first line, followed by key-value pairs on next lines.
                 * Current Apache SIS implementation ignores the characters after WKT.
                 */
                format.validate(crs);
                return;
            }
        } catch (NoSuchFileException | FileNotFoundException e) {
            listeners.warning(Resources.format(Resources.Keys.CanNotReadAuxiliaryFile_1, PRJ), e);
            return;
        } catch (IOException | ParseException | ClassCastException e) {
            cause = e;
        }
        throw new DataStoreReferencingException(Resources.format(Resources.Keys.CanNotReadAuxiliaryFile_1, PRJ), cause);
    }

    /**
     * Reads the content of the auxiliary file with the specified extension.
     * This method uses the same URI than {@link #location},
     * except for the extension which is replaced by the given value.
     * This method is suitable for reasonably small files.
     * An arbitrary size limit is applied for safety.
     *
     * @param  extension    the filename extension of the auxiliary file to open.
     * @return the file content together with the source. Should be short-lived.
     * @throws NoSuchFileException if the auxiliary file has not been found (when opened from path).
     * @throws FileNotFoundException if the auxiliary file has not been found (when opened from URL).
     * @throws IOException if another error occurred while opening the stream.
     * @throws DataStoreException if the auxiliary file content seems too large.
     */
    protected final AuxiliaryContent readAuxiliaryFile(final String extension) throws IOException, DataStoreException {
        /*
         * Try to open the stream using the storage type (Path or URL) closest to the type
         * given at construction time. We do that because those two types can not open the
         * same streams. For example Path does not open HTTP or FTP connections by default,
         * and URL does not open S3 files in current implementation.
         */
        final InputStream stream;
        Path path = getSpecifiedPath();
        final Object source;                    // In case an error message is produced.
        if (path != null) {
            final String base = getBaseFilename(path);
            path   = path.resolveSibling(base.concat(extension));
            stream = Files.newInputStream(path);
            source = path;
        } else {
            final URL url = IOUtilities.toAuxiliaryURL(location, extension);
            if (url == null) {
                return null;
            }
            stream = url.openStream();
            source = url;
        }
        /*
         * Reads the auxiliary file fully, with an arbitrary size limit.
         */
        try (InputStreamReader reader = (encoding != null)
                ? new InputStreamReader(stream, encoding)
                : new InputStreamReader(stream))
        {
            char[] buffer = new char[1024];
            int offset = 0, count;
            while ((count = reader.read(buffer, offset, buffer.length - offset)) >= 0) {
                offset += count;
                if (offset >= buffer.length) {
                    if (offset >= MAXIMAL_LENGTH) {
                        throw new DataStoreContentException(Resources.forLocale(listeners.getLocale())
                                .getString(Resources.Keys.AuxiliaryFileTooLarge_1, IOUtilities.filename(source)));
                    }
                    buffer = Arrays.copyOf(buffer, offset*2);
                }
            }
            return new AuxiliaryContent(source, buffer, 0, offset);
        }
    }

    /**
     * Content of a file read by {@link #readAuxiliaryFile(String)}.
     * This is used as a workaround for not being able to return multiple values from a single method.
     * Instances of this class should be short lived, because they hold larger arrays than necessary.
     */
    protected static final class AuxiliaryContent implements CharSequence {
        /** {@link Path} or {@link URL} that have been read. */
        private final Object source;

        /** The textual content of the auxiliary file. */
        private final char[] buffer;

        /** Index of the first valid character in {@link #buffer}. */
        private final int offset;

        /** Number of valid characters in {@link #buffer}. */
        private final int length;

        /** Wraps (without copying) the given array as the content of an auxiliary file. */
        private AuxiliaryContent(final Object source, final char[] buffer, final int offset, final int length) {
            this.source = source;
            this.buffer = buffer;
            this.offset = offset;
            this.length = length;
        }

        /**
         * Returns the filename (without path) of the auxiliary file.
         * This information is mainly for producing error messages.
         *
         * @return name of the auxiliary file that have been read.
         */
        public String getFilename() {
            return IOUtilities.filename(source);
        }

        /**
         * Returns the number of valid characters in this sequence.
         */
        @Override
        public int length() {
            return length;
        }

        /**
         * Returns the character at the given index. For performance reasons this method does not check index bounds.
         * The behavior of this method is undefined if the given index is not smaller than {@link #length()}.
         * We skip bounds check because this class should be used for Apache SIS internal purposes only.
         */
        @Override
        public char charAt(final int index) {
            return buffer[offset + index];
        }

        /**
         * Returns a sub-sequence of this auxiliary file content. For performance reasons this method does not
         * perform bound checks. The behavior of this method is undefined if arguments are out of bounds.
         * We skip bounds check because this class should be used for Apache SIS internal purposes only.
         */
        @Override
        public CharSequence subSequence(final int start, final int end) {
            return new AuxiliaryContent(source, buffer, offset + start, end - start);
        }

        /**
         * Copies this auxiliary file content in a {@link String}.
         * This method does not cache the result; caller should invoke at most once.
         */
        @Override
        public String toString() {
            return new String(buffer, offset, length);
        }
    }

    /**
     * Writes the {@code "*.prj"} auxiliary file if {@link #crs} is non-null.
     * If {@link #crs} is null and the auxiliary file exists, it is deleted.
     *
     * <h4>WKT version used</h4>
     * Current version writes the CRS in WKT 2 format. This is not the common practice, which uses WKT 1.
     * But the WKT 1 variant used by the common practice is not the standard format defined by OGC 01-009.
     * It is more like  {@link Convention#WKT1_IGNORE_AXES}, which has many ambiguity problems. The WKT 2
     * format fixes those ambiguities. We hope that major software have updated their referencing engine
     * and can now parse WKT 2 as well as WKT 1.
     *
     * @throws DataStoreException if an error occurred while writing the file.
     */
    protected final void writePRJ() throws DataStoreException {
        try {
            if (crs == null) {
                deleteAuxiliaryFile(PRJ);
            } else try (BufferedWriter out = writeAuxiliaryFile(PRJ)) {
                final StoreFormat format = new StoreFormat(locale, timezone, null, listeners);
                // Keep the default "WKT 2" format (see method javadoc).
                format.format(crs, out);
                out.newLine();
            }
        } catch (IOException e) {
            Object identifier = getIdentifier().orElse(null);
            if (identifier == null) identifier = Classes.getShortClassName(this);
            throw new DataStoreException(Resources.format(Resources.Keys.CanNotWriteResource_1, identifier), e);
        }
    }

    /**
     * Creates a writer for an auxiliary file with the specified extension.
     * This method uses the same path than {@link #location},
     * except for the extension which is replaced by the given value.
     *
     * @param  extension  the filename extension of the auxiliary file to write.
     * @return a stream opened on the specified file.
     * @throws UnknownServiceException if no {@link Path} or {@link java.net.URI} is available.
     * @throws DataStoreException if the auxiliary file can not be created.
     * @throws IOException if another error occurred while opening the stream.
     */
    protected final BufferedWriter writeAuxiliaryFile(final String extension)
            throws IOException, DataStoreException
    {
        final Path[] paths = super.getComponentFiles();
        if (paths.length == 0) {
            throw new UnknownServiceException();
        }
        Path path = paths[0];
        final String base = getBaseFilename(path);
        path = path.resolveSibling(base.concat(extension));
        return (encoding != null)
                ? Files.newBufferedWriter(path, encoding)
                : Files.newBufferedWriter(path);
    }

    /**
     * Deletes the auxiliary file with the given extension if it exists.
     * If the auxiliary file does not exist, then this method does nothing.
     *
     * @param  extension  the filename extension of the auxiliary file to delete.
     * @throws DataStoreException if the auxiliary file is not on a supported file system.
     * @throws IOException if an error occurred while deleting the file.
     */
    protected final void deleteAuxiliaryFile(final String extension) throws DataStoreException, IOException {
        for (Path path : super.getComponentFiles()) {
            final String base = getBaseFilename(path);
            path = path.resolveSibling(base.concat(extension));
            Files.deleteIfExists(path);
        }
    }

    /**
     * Returns the {@linkplain #location} as a {@code Path} component together with auxiliary files.
     * The default implementation does the same computation as the super-class, then adds the sibling
     * file with {@code ".prj"} extension if it exists.
     *
     * @return the main file and auxiliary files as paths, or an empty array if unknown.
     * @throws DataStoreException if the URI can not be converted to a {@link Path}.
     */
    @Override
    public Path[] getComponentFiles() throws DataStoreException {
        return listComponentFiles(PRJ);
    }

    /**
     * Returns the {@linkplain #location} as a {@code Path} component together with auxiliary files.
     * This method computes the path to the main file as {@link URIDataStore#getComponentFiles()},
     * then add the sibling files with all extensions specified in the {@code auxiliaries} argument.
     * Each auxiliary file is tested for existence. Paths that are not regular files are omitted.
     * This is a helper method for {@link #getComponentFiles()} implementation.
     *
     * @param  auxiliaries  filename extension (without leading dot) of all auxiliary files.
     *         Null elements are silently ignored.
     * @return the URI as a path, followed by all auxiliary files that exist.
     * @throws DataStoreException if the URI can not be converted to a {@link Path}.
     */
    protected final Path[] listComponentFiles(final String... auxiliaries) throws DataStoreException {
        Path[] paths = super.getComponentFiles();
        int count = paths.length;
        if (count != 0) {
            final Path path = paths[0];
            final String base = getBaseFilename(path);
            for (final String extension : auxiliaries) {
                if (extension != null) {
                    final Path p = path.resolveSibling(base.concat(extension));
                    if (Files.isRegularFile(p)) {
                        if (count >= paths.length) {
                            paths = Arrays.copyOf(paths, count + auxiliaries.length);
                        }
                        paths[count++] = p;
                    }
                }
            }
            paths = ArraysExt.resize(paths, count);
        }
        return paths;
    }

    /**
     * Returns the filename of the given path without the file suffix.
     * The returned string always ends in {@code '.'}, making it ready
     * for concatenation of a new suffix.
     */
    private static String getBaseFilename(final Path path) {
        final String base = path.getFileName().toString();
        final int s = base.lastIndexOf('.');
        return (s >= 0) ? base.substring(0, s+1) : base + '.';
    }

    /**
     * Returns the parameters used to open this data store.
     *
     * @return parameters used for opening this {@code DataStore}.
     */
    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        final ParameterValueGroup pg = parameters(provider, location);
        if (pg != null) {
            pg.parameter(Provider.CRS_NAME).setValue(crs);
            return Optional.of(pg);
        }
        return Optional.empty();
    }

    /**
     * Provider for {@link PRJDataStore} instances.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.2
     * @since   1.2
     * @module
     */
    public abstract static class Provider extends URIDataStore.Provider {
        /**
         * Name of the {@link #DEFAULT_CRS} parameter.
         */
        static final String CRS_NAME = "defaultCRS";

        /**
         * Description of the optional parameter for the default coordinate reference system.
         */
        public static final ParameterDescriptor<CoordinateReferenceSystem> DEFAULT_CRS;
        static {
            final ParameterBuilder builder = new ParameterBuilder();
            DEFAULT_CRS = builder.addName(CRS_NAME).setDescription(
                    Vocabulary.formatInternational(Vocabulary.Keys.CoordinateRefSys))
                    .create(CoordinateReferenceSystem.class, null);
        }

        /**
         * Creates a new provider.
         */
        protected Provider() {
        }

        /**
         * Invoked by {@link #getOpenParameters()} the first time that a parameter descriptor needs to be created.
         * When invoked, the parameter group name is set to a name derived from the {@link #getShortName()} value.
         * The default implementation creates a group containing {@link #LOCATION_PARAM} and {@link #DEFAULT_CRS}.
         * Subclasses can override if they need to create a group with more parameters.
         *
         * @param  builder  the builder to use for creating parameter descriptor. The group name is already set.
         * @return the parameters descriptor created from the given builder.
         */
        @Override
        protected ParameterDescriptorGroup build(final ParameterBuilder builder) {
            return builder.createGroup(LOCATION_PARAM, DEFAULT_CRS);
        }

        /**
         * Returns a data store implementation from the given parameters.
         *
         * @return a data store implementation associated with this provider for the given parameters.
         * @throws DataStoreException if an error occurred while creating the data store instance.
         */
        @Override
        public DataStore open(final ParameterValueGroup parameters) throws DataStoreException {
            ArgumentChecks.ensureNonNull("parameter", parameters);
            final StorageConnector connector = connector(this, parameters);
            final Parameters pg = Parameters.castOrWrap(parameters);
            connector.setOption(DataOptionKey.DEFAULT_CRS, pg.getValue(DEFAULT_CRS));
            return open(connector);
        }
    }
}
