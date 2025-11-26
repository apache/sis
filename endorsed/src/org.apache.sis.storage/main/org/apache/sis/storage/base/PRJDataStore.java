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
package org.apache.sis.storage.base;

import java.net.URISyntaxException;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.NoSuchFileException;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Objects;
import java.util.Optional;
import java.util.ArrayList;
import jakarta.xml.bind.JAXBException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.storage.wkt.StoreFormat;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.Classes;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.xml.internal.shared.ExceptionSimplifier;


/**
 * A data store for a file or URI accompanied by an auxiliary file of the same name with {@code .prj} extension.
 * If the auxiliary file is absent, {@link OptionKey#DEFAULT_CRS} is used as a fallback.
 * The default WKT 1 variant used for parsing the {@code "*.prj"} file is the variant used by "World Files" and GDAL.
 * This is not the standard specified by OGC 01-009 (they differ in there interpretation of units of measurement).
 * This implementation accepts also WKT 2 (in which case the WKT 1 convention is ignored) and GML.
 *
 * <p>The URI can be null if the only available storage is a {@link java.nio.channels.ReadableByteChannel},
 * {@link java.io.InputStream} or {@link java.io.Reader}. In such case, the CRS should be specified by the
 * {@link OptionKey#DEFAULT_CRS}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class PRJDataStore extends URIDataStore {
    /**
     * The filename extension of {@code "*.prj"} files.
     *
     * @see #getFileSet()
     */
    protected static final String PRJ = "prj";

    /**
     * The coordinate reference system. This is initialized on the value provided by {@link OptionKey#DEFAULT_CRS}
     * at construction time, and is modified later if a {@code "*.prj"} file is found.
     */
    protected CoordinateReferenceSystem crs;

    /**
     * Creates a new data store. This constructor does not open the file,
     * so subclass constructors can decide whether to open in read-only or read/write mode.
     *
     * <p>The following options are recognized:</p>
     * <ul>
     *   <li>{@link OptionKey#DEFAULT_CRS}: default CRS if no auxiliary {@code "*.prj"} file is found.</li>
     *   <li>{@link OptionKey#ENCODING}: encoding of the {@code "*.prj"} file. Default is the JVM default.</li>
     *   <li>{@link OptionKey#TIMEZONE}: timezone of dates in the {@code "*.prj"} file. Default is UTC.</li>
     *   <li>{@link OptionKey#LOCALE}: locale for texts in the {@code "*.prj"} file. Default is English.</li>
     * </ul>
     *
     * @param  provider   the factory that created this {@code PRJDataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, reader instance, <i>etc</i>).
     * @throws DataStoreException if an error occurred while creating the data store for the given storage.
     */
    protected PRJDataStore(final DataStoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(provider, connector);
        crs = connector.getOption(OptionKey.DEFAULT_CRS);
    }

    /**
     * Returns the convention to use for parsing the <abbr>PRJ</abbr> file if Well-Known Text 1 is used.
     * Unfortunately, many formats use the ambiguous conventions from the very first specification,
     * and ignore the clarifications done by OGC 01-009. In such case, we have to tell the WKT parser
     * that the ambiguous conventions are used. This method can be overridden if the subclass has a way
     * to know which WKT 1 conventions are used.
     *
     * @return convention to use for parsing the <abbr>PRJ</abbr> file.
     */
    protected Convention getConvention() {
        return Convention.WKT1_COMMON_UNITS;
    }

    /**
     * Reads the {@code "*.prj"} auxiliary file. If the file is not found, then this method does nothing
     * and {@link #crs} keeps its current value (usually the default value found at construction time).
     *
     * <p>This method does not verify if it has been invoked multiple time.
     * Caller should track whether the data store has been initialized.</p>
     *
     * @param  caller  the class to report as the warning source if a log record is created.
     * @param  method  the method to report as the warning source if a log record is created.
     * @throws DataStoreException if an error occurred while reading the file.
     */
    protected final void readPRJ(final Class<? extends DataStore> caller, final String method) throws DataStoreException {
        readWKT(caller, method, CoordinateReferenceSystem.class, PRJ).ifPresent((result) -> crs = result);
    }

    /**
     * Reads an auxiliary file in WKT or GML format. Standard PRJ files use WKT only,
     * but the GML format is also accepted by this method as an extension specific to Apache SIS.
     *
     * @param  caller     the class to report as the warning source if a log record is created.
     * @param  method     the method to report as the warning source if a log record is created.
     * @param  type       base class or interface of the object to read.
     * @param  extension  extension of the file to read (usually {@link #PRJ}), or {@code null} for the main file.
     * @return the parsed object, or an empty value if the file does not exist.
     * @throws DataStoreException if an error occurred while reading the file.
     */
    protected final <T> Optional<T> readWKT(final Class<? extends DataStore> caller, final String method,
            final Class<T> type, final String extension) throws DataStoreException
    {
        Exception cause = null, suppressed = null;
        try {
            final AuxiliaryContent content = readAuxiliaryFile(extension, true);
            if (content == null) {
                cannotReadAuxiliaryFile(caller, method, extension, null, true);
                return Optional.empty();
            }
            if (content.source != null) try {
                // ClassCastException handled by `catch` statement below.
                return Optional.of(type.cast(readXML(content.source)));
            } catch (JAXBException e) {
                var s = new ExceptionSimplifier(content.getFilename(), e);
                throw new DataStoreException(s.getMessage(getLocale()), s.exception);
            }
            final String wkt = content.toString();
            final var format = new StoreFormat(dataLocale, timezone, null, listeners);
            format.setConvention(getConvention());          // Ignored if the format is WKT 2.
            try {
                format.setSourceFile(content.getURI());
            } catch (URISyntaxException e) {
                suppressed = e;
            }
            final var pos = new ParsePosition(0);
            // ClassCastException handled by `catch` statement below.
            final T result = type.cast(format.parse(wkt, pos));
            if (result != null) {
                /*
                 * Some characters may exist after the WKT definition. For example, we sometimes see the CRS
                 * defined twice: as a WKT on the first line, followed by key-value pairs on next lines.
                 * Current Apache SIS implementation ignores all characters after the WKT.
                 */
                format.validate(null, caller, method, result);
                return Optional.of(result);
            }
        } catch (NoSuchFileException | FileNotFoundException e) {
            cannotReadAuxiliaryFile(caller, method, extension, e, true);
            return Optional.empty();
        } catch (URISyntaxException | IOException | ParseException | ClassCastException e) {
            cause = e;
        }
        final var e = new DataStoreReferencingException(cannotReadAuxiliaryFile(extension), cause);
        if (suppressed != null) e.addSuppressed(suppressed);
        throw e;
    }

    /**
     * Writes the {@code "*.prj"} auxiliary file if {@link #crs} is non-null.
     * If {@link #crs} is null and the auxiliary file exists, it is deleted.
     *
     * <h4>WKT version used</h4>
     * Current version writes the CRS in WKT 2 format. This is not the common practice, which uses WKT 1.
     * But the WKT 1 variant used by the common practice is not the standard format defined by OGC 01-009.
     * It is more like {@link Convention#WKT1_IGNORE_AXES}, which has many ambiguity problems. The WKT 2
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
                final var format = new StoreFormat(dataLocale, timezone, null, listeners);
                format.setConvention(Convention.WKT2_2015);     // TODO: upgrade to newer version.
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
     * Returns the {@linkplain #location} as a {@code Path} component together with auxiliary files.
     * The default implementation does the same computation as the super-class, then adds the sibling
     * file with {@code ".prj"} extension if it exists.
     *
     * @return the main file and auxiliary files as paths, or an empty value if unknown.
     * @throws DataStoreException if the URI cannot be converted to a {@link Path}.
     */
    @Override
    public Optional<FileSet> getFileSet() throws DataStoreException {
        return listComponentFiles(PRJ);
    }

    /**
     * Returns the {@linkplain #location} as a {@code Path} component together with auxiliary files.
     * This method computes the path to the main file as {@link URIDataStore#getFileSet()},
     * then adds the sibling files with all extensions specified in the {@code auxiliaries} argument.
     * Each auxiliary file is tested for existence. Paths that are not regular files are omitted.
     * This is a helper method for {@link #getFileSet()} implementations.
     *
     * @param  auxiliaries  filename extension (without leading dot) of all auxiliary files.
     *         Null elements are silently ignored.
     * @return the URI as a path, followed by all auxiliary files that exist.
     * @throws DataStoreException if the URI cannot be converted to a {@link Path}.
     */
    protected final Optional<FileSet> listComponentFiles(final String... auxiliaries) throws DataStoreException {
        return super.getFileSet().map((fileset) -> {
            final var paths = new ArrayList<Path>(fileset.getPaths());
            final Path path = paths.get(0);  // This list is ever empty.
            final String base = getBaseFilename(path);
            boolean modified = false;
            for (final String extension : auxiliaries) {
                if (extension != null) {
                    final Path p = path.resolveSibling(base.concat(extension));
                    if (Files.isRegularFile(p)) {
                        modified |= paths.add(p);
                    }
                }
            }
            return modified ? new FileSet(paths) : fileset;
        });
    }

    /**
     * Returns the parameters used to open this data store.
     *
     * @return parameters used for opening this {@code DataStore}.
     */
    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        final Optional<ParameterValueGroup> op = super.getOpenParameters();
        op.ifPresent((pg) -> pg.parameter(Provider.CRS_NAME).setValue(crs));
        return op;
    }

    /**
     * Provider for {@link PRJDataStore} instances.
     *
     * @author  Martin Desruisseaux (Geomatys)
     */
    public abstract static class Provider extends URIDataStoreProvider {
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
            DEFAULT_CRS = builder.addName(CRS_NAME)
                    .setDescription(Vocabulary.formatInternational(Vocabulary.Keys.CoordinateRefSys))
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
            return builder.createGroup(LOCATION_PARAM, METADATA_PARAM, DEFAULT_CRS);
        }

        /**
         * Returns a data store implementation from the given parameters.
         *
         * @return a data store implementation associated with this provider for the given parameters.
         * @throws DataStoreException if an error occurred while creating the data store instance.
         */
        @Override
        public DataStore open(final ParameterValueGroup parameters) throws DataStoreException {
            final StorageConnector connector = connector(this, Objects.requireNonNull(parameters));
            final Parameters pg = Parameters.castOrWrap(parameters);
            connector.setOption(OptionKey.DEFAULT_CRS, pg.getValue(DEFAULT_CRS));
            return open(connector);
        }
    }
}
