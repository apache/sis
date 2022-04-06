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
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.NoSuchFileException;
import java.text.ParseException;
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
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.internal.storage.io.IOUtilities;
import org.apache.sis.internal.storage.wkt.StoreFormat;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;


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
        try {
            final String wkt = readAuxiliaryFile(PRJ, encoding);
            if (wkt != null) {
                final StoreFormat format = new StoreFormat(locale, timezone, null, listeners);
                format.setConvention(Convention.WKT1_COMMON_UNITS);
                crs = (CoordinateReferenceSystem) format.parseObject(wkt);
                format.validate(crs);
            }
        } catch (NoSuchFileException | FileNotFoundException e) {
            listeners.warning(Resources.format(Resources.Keys.CanNotReadAuxiliaryFile_1, PRJ), e);
        } catch (IOException | ParseException | ClassCastException e) {
            throw new DataStoreException(Resources.format(Resources.Keys.CanNotReadAuxiliaryFile_1, PRJ), e);
        }
    }

    /**
     * Reads the content of the auxiliary file with the specified extension.
     * This method uses the same URI than {@link #location},
     * except for the extension which is replaced by the given value.
     * This method is suitable for reasonably small files.
     *
     * @param  extension  the filename extension of the auxiliary file to open.
     * @param  encoding   the encoding to use for reading the file content, or {@code null} for default.
     * @return a stream opened on the specified file, or {@code null} if the file is not found.
     * @throws NoSuchFileException if the auxiliary file has not been found (when opened from path).
     * @throws FileNotFoundException if the auxiliary file has not been found (when opened from URL).
     * @throws IOException if another error occurred while opening the stream.
     */
    protected final String readAuxiliaryFile(final String extension, Charset encoding) throws IOException {
        if (encoding == null) {
            encoding = Charset.defaultCharset();
        }
        /*
         * Try to open the stream using the storage type (Path or URL) closest to the type
         * given at construction time. We do that because those two types can not open the
         * same streams. For example Path does not open HTTP or FTP connections by default,
         * and URL does not open S3 files in current implementation.
         */
        final InputStream stream;
        Path path = getSpecifiedPath();
        if (path != null) {
            final String base = getBaseFilename(path);
            path = path.resolveSibling(base.concat(PRJ));
            stream = Files.newInputStream(path);
        } else {
            final URL url = IOUtilities.toAuxiliaryURL(location, extension);
            if (url == null) {
                return null;
            }
            stream = url.openStream();
        }
        /*
         * Reads the auxiliary file fully.
         */
        try (InputStreamReader reader = new InputStreamReader(stream, encoding)) {
            char[] buffer = new char[1024];
            int offset = 0, count;
            while ((count = reader.read(buffer, offset, buffer.length - offset)) >= 0) {
                offset += count;
                if (offset >= buffer.length) {
                    buffer = Arrays.copyOf(buffer, offset*2);
                }
            }
            return new String(buffer, 0, offset);
        }
    }

    /**
     * Returns the {@linkplain #location} as a {@code Path} component together with auxiliary files.
     * The default implementation does the same computation as the super-class, then adds the sibling
     * file with {@code ".prj"} extension if it exists.
     *
     * @return the URI as a path, or an empty array if the URI is null.
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
                final Path p = path.resolveSibling(base.concat(extension));
                if (Files.isRegularFile(p)) {
                    if (count >= paths.length) {
                        paths = Arrays.copyOf(paths, count + auxiliaries.length);
                    }
                    paths[count++] = p;
                }
            }
            paths = ArraysExt.resize(paths, count);
        }
        return paths;
    }

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
