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

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Optional;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.time.ZoneId;
import java.io.BufferedWriter;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.charset.Charset;
import javax.xml.transform.Source;
import jakarta.xml.bind.JAXBException;
import org.opengis.util.GenericName;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataOptionKey;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreContentException;
import org.apache.sis.storage.ReadOnlyStorageException;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.iso.Names;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.privy.URISource;
import org.apache.sis.xml.privy.ExceptionSimplifier;


/**
 * A data store for a storage that may be represented by a {@link URI}.
 * The URI is stored in {@link #location} field and is used for populating some default metadata.
 * It is also use for resolving the path to auxiliary files, for example the CRS definition in PRJ file.
 * The URI can be null if the only available storage is a {@link java.nio.channels.ReadableByteChannel},
 * {@link InputStream} or {@link java.io.Reader}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class URIDataStore extends DataStore implements StoreResource {
    /**
     * The {@link DataStoreProvider#LOCATION} parameter value, or {@code null} if none.
     */
    protected final URI location;

    /**
     * The {@link #location} as a path, or {@code null} if none or if the URI cannot be converted to a path.
     *
     * @see #getFileSet()
     */
    protected final Path locationAsPath;

    /**
     * Path to an auxiliary file providing metadata as path, or {@code null} if none or not applicable.
     * Unless absolute, this path is relative to the {@link #location} or to the {@link #locationAsPath}.
     * The path may contain the {@code '*'} character, which need to be replaced by the main file name
     * without suffix at reading time.
     */
    private final Path metadataPath;

    /**
     * User-specified character encoding, or {@code null} for the JVM default (usually UTF-8).
     * Subclasses may replace this value by a value read from the data file.
     */
    protected Charset encoding;

    /**
     * User-specified locale for textual content, or {@code null} for {@link Locale#ROOT} (usually English).
     * This locale is usually for {@link org.opengis.util.InternationalString} localization rather than for
     * parsing numbers or dates, but the exact interpretation is at subclasses choice.
     * Subclasses may replace this value by a value read from the data file.
     */
    protected Locale dataLocale;

    /**
     * User-specified timezone for dates, or {@code null} for UTC.
     * Subclasses may replace this value by a value read from the data file.
     */
    protected ZoneId timezone;

    /**
     * Creates a new data store. This constructor does not open the file,
     * so subclass constructors can decide whether to open in read-only or read/write mode.
     * It is caller's responsibility to ensure that the {@link java.nio.file.OpenOption}
     * are compatible with the capabilities (read-only or read/write) of this data store.
     *
     * @param  provider   the factory that created this {@code URIDataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, reader instance, <i>etc</i>).
     * @throws DataStoreException if an error occurred while creating the data store for the given storage.
     */
    protected URIDataStore(final DataStoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(provider, connector);
        location       = connector.getStorageAs(URI.class);
        locationAsPath = connector.getStorageAs(Path.class);
        if (locationAsPath != null || location != null) {
            metadataPath = connector.getOption(DataOptionKey.METADATA_PATH);
        } else {
            metadataPath = null;
        }
        encoding   = connector.getOption(OptionKey.ENCODING);
        dataLocale = connector.getOption(OptionKey.LOCALE);
        timezone   = connector.getOption(OptionKey.TIMEZONE);
    }

    /**
     * Returns the originator of this resource, which is this data store itself.
     *
     * @return {@code this}.
     */
    @Override
    public final DataStore getOriginator() {
        return this;
    }

    /**
     * Returns an identifier for the root resource of this data store, or an empty value if none.
     * The default implementation returns the filename without path and without file extension.
     *
     * @return an identifier for the root resource of this data store.
     * @throws DataStoreException if an error occurred while fetching the identifier.
     */
    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        final String filename = getFilename();
        return (filename != null) ? Optional.of(Names.createLocalName(null, null, filename)) : super.getIdentifier();
    }

    /**
     * {@return the filename without path and without file extension, or null if none}.
     * This method can be used for building metadata like below (note that {@link #getIdentifier()}
     * should not be invoked during metadata construction time, for avoiding recursive method calls):
     *
     * {@snippet lang="java" :
     *     builder.addTitleOrIdentifier(getFilename(), MetadataBuilder.Scope.ALL);
     *     }
     *
     * Above snippet should not be applied before this data store did its best effort for providing a title.
     * The use of identifier as a title is a fallback for making valid metadata, because the title is mandatory
     * in ISO 19111 metadata.
     */
    public final String getFilename() {
        if (location == null) {
            return null;
        }
        return IOUtilities.filenameWithoutExtension(location.isOpaque()
                ? location.getSchemeSpecificPart() : location.getPath());
    }

    /**
     * Returns the main and metadata locations as {@code Path} components, or an empty value if none.
     * The default implementation returns the storage specified at construction time converted to a
     * {@link Path} if such conversion was possible, or an empty value otherwise. The set may also
     * contains the path to the {@linkplain DataOptionKey#METADATA_PATH auxiliary metadata file}.
     *
     * @return the URI to component files as paths, or an empty value if unknown.
     * @throws DataStoreException if an error occurred while getting the paths.
     */
    @Override
    public Optional<FileSet> getFileSet() throws DataStoreException {
        final Path[] paths;
        try {
            paths = new Path[] {locationAsPath, getMetadataPath()};
        } catch (IOException e) {
            throw new DataStoreException(e);
        }
        final int count = ArraysExt.removeDuplicated(paths, ArraysExt.removeNulls(paths));
        if (count != 0) {
            return Optional.of(new FileSet(ArraysExt.resize(paths, count)));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns the parameters used to open this data store.
     *
     * @return parameters used for opening this {@code DataStore}.
     */
    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        return Optional.ofNullable(parameters(provider, location));
    }

    /**
     * Creates parameter value group for the current location, if non-null.
     * This convenience method is used for {@link DataStore#getOpenParameters()} implementations in public
     * {@code DataStore} that cannot extend {@code URIDataStore} directly, because this class is internal.
     *
     * @param  provider  the provider of the data store for which to get open parameters.
     * @param  location  file opened by the data store.
     * @return parameters to be returned by {@link DataStore#getOpenParameters()}, or {@code null} if unknown.
     */
    public static ParameterValueGroup parameters(final DataStoreProvider provider, final URI location) {
        if (location == null || provider == null) return null;
        final ParameterValueGroup pg = provider.getOpenParameters().createValue();
        pg.parameter(DataStoreProvider.LOCATION).setValue(location);
        return pg;
    }




    /*
     ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
     ┃                                                                                ┃
     ┃                                AUXILIARY FILES                                 ┃
     ┃                                                                                ┃
     ┃      The following are helper methods for data stores made of many files       ┃
     ┃      at some location relative to the main file (e.g. in the same folder).     ┃
     ┃                                                                                ┃
     ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
     */

    /**
     * Returns the given path with the wildcard character replaced by the name of the main file.
     *
     * @param  path  path in which to replace wildcard character, or {@code null}.
     * @return path with wildcard character replaced, or {@code path} if no replacement was done,
     *         or {@code null} if a replacement was required but couldn't be done.
     */
    private Path replaceWildcard(Path path) {
        if (path != null) {
            boolean changed = false;
            String filename = null;     // Determined when first needed.
            final var names = new String[path.getNameCount()];
            int count = 0;
            for (final Path p : path) {
                String name = p.toString();
                if (name.indexOf('*') >= 0) {
                    if (filename == null) {
                        filename = IOUtilities.filename(locationAsPath != null ? locationAsPath : location);
                        if (filename == null) {
                            return null;
                        }
                        final int s = filename.lastIndexOf(IOUtilities.EXTENSION_SEPARATOR);
                        if (s >= 0) {
                            filename = filename.substring(0, s);
                        }
                    }
                    name = name.replace("*", filename);
                    changed = true;
                }
                names[count++] = name;
            }
            if (changed) {
                path = path.getFileSystem().getPath(names[0], Arrays.copyOfRange(names, 1, count));
            }
        }
        return path;
    }

    /**
     * {@return the path to the auxiliary metadata file, or null if none}.
     * This is a path built from the {@link DataOptionKey#METADATA_PATH} value if present.
     * Note that the metadata may be unavailable as a {@link Path} but available as an {@link URI}.
     */
    private Path getMetadataPath() throws IOException {
        Path path = replaceWildcard(metadataPath);
        if (path != null) {
            Path parent = locationAsPath;
            if (parent != null) {
                parent = parent.getParent();
                if (parent != null) {
                    path = parent.resolve(path);
                }
            }
            if (Files.isSameFile(path, locationAsPath)) {
                return null;
            }
        }
        return path;
    }

    /**
     * {@return the URI to the auxiliary metadata file, or null if none}.
     * This is a path built from the {@link DataOptionKey#METADATA_PATH} value if present.
     * Note that the metadata may be unavailable as an {@link URI} but available as a {@link Path}.
     */
    private URI getMetadataURI() throws URISyntaxException {
        URI uri = location;
        if (uri != null) {
            final Path path = replaceWildcard(metadataPath);
            if (path != null) {
                uri = IOUtilities.toAuxiliaryURI(uri, path.toString(), false);
                if (!uri.equals(location)) {
                    return uri;
                }
            }
        }
        return null;
    }

    /**
     * Opens a buffered input stream for the given path.
     *
     * @param  path  the path to the file to open.
     * @return a new buffered input stream.
     * @throws IOException in an error occurred while opening the file.
     */
    private static InputStream open(final Path path) throws IOException {
        return new BufferedInputStream(Files.newInputStream(path, StandardOpenOption.READ), AuxiliaryContent.BUFFER_SIZE);
    }

    /**
     * If an auxiliary metadata file has been specified, merges that file to the given metadata.
     * This step should be done only after the data store added its own metadata.
     * Failure to load auxiliary metadata are only a warning.
     * If a warning is logged, declared source will be the {@code getMetadata()} method of the given class.
     *
     * @param  caller   the source class to declare if a warning is logged.
     * @param  builder  where to merge the metadata.
     */
    protected final void mergeAuxiliaryMetadata(final Class<? extends DataStore> caller, final MetadataBuilder builder) {
        Object spec = null;         // Used only for formatting error message.
        Object metadata = null;
        try {
            final URI source;
            final InputStream input;
            final Path path = getMetadataPath();
            if (path != null) {
                spec   = path;
                source = path.toUri();
                input  = open(path);
            } else {
                source = getMetadataURI();
                if (source == null) return;
                spec  = source;
                input = source.toURL().openStream();
            }
            metadata = readXML(input, source);
        } catch (URISyntaxException | IOException e) {
            cannotReadAuxiliaryFile(caller, "getMetadata", "xml", e, true);
        } catch (JAXBException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                cannotReadAuxiliaryFile(caller, "getMetadata", "xml", (Exception) cause, true);
            } else {
                listeners.warning(new ExceptionSimplifier(spec, e).record(URIDataStore.class, "mergeAuxiliaryMetadata"));
            }
        }
        if (metadata != null) {
            builder.mergeMetadata(metadata, getLocale());
        }
    }

    /**
     * Reads a XML document from an input stream using JAXB. The {@link #dataLocale} and {@link #timezone}
     * are specified to the unmarshaller. Warnings are redirected to the listeners.
     *
     * @param  input   the input stream of the XML document. Will be closed by this method.
     * @param  source  the source of the XML document to read.
     * @return the unmarshalled object.
     * @throws URISyntaxException if an error occurred while normalizing the URI.
     * @throws JAXBException if an error occurred while parsing the XML document.
     * @throws IOException if an error occurred while closing the input stream.
     */
    protected final Object readXML(final InputStream input, final URI source)
            throws URISyntaxException, IOException, JAXBException
    {
        try (input) {
            return readXML(URISource.create(input, source));
        }
    }

    /**
     * Reads a XML document from a source using JAXB. The {@link #dataLocale} and {@link #timezone}
     * are specified to the unmarshaller. Warnings are redirected to the listeners.
     *
     * @param  source  the source of the XML document to read.
     * @return the unmarshalled object.
     * @throws JAXBException if an error occurred while parsing the XML document.
     */
    protected final Object readXML(final Source source) throws JAXBException {
        java.util.logging.Filter handler = (record) -> {
            record.setLoggerName(null);        // For allowing `listeners` to use the provider's logger name.
            listeners.warning(record);
            return false;
        };
        // Cannot use Map.of(…) because it does not accept null values.
        Map<String,Object> properties = new HashMap<>(8);
        properties.put(XML.LOCALE, dataLocale);
        properties.put(XML.TIMEZONE, timezone);
        properties.put(XML.WARNING_FILTER, handler);
        return XML.unmarshal(source, properties);
    }

    /**
     * Reads the content of the auxiliary file with the specified extension.
     * This method uses the {@link #location} URI with the extension replaced by the given value.
     * The file content is read and stored as a character sequence decoded according the store
     * {@linkplain #encoding}, unless {@code acceptXML} is {@code true} and the file has been
     * identified as an XML file. In the latter case, the character sequence is empty and the
     * source must be read with {@link AuxiliaryContent#source}.
     *
     * <h4>Limitations</h4>
     * This method is suitable for reasonably small files. An arbitrary size limit is applied for safety,
     * unless {@code acceptXML} is {@code true} and the file has been detected as an XML file.
     *
     * @param  extension  the filename extension (without leading dot) of the auxiliary file to open,
     *                    or {@code null} for using the main file without changing its extension.
     * @param  acceptXML  whether to check if the source is a XML file.
     * @return the file content together with the source, or {@code null} if none. Should be short-lived.
     * @throws NoSuchFileException if the auxiliary file has not been found (when opened from path).
     * @throws FileNotFoundException if the auxiliary file has not been found (when opened from URL).
     * @throws URISyntaxException if an error occurred while normalizing the URI.
     * @throws IOException if another error occurred while opening the stream.
     * @throws DataStoreException if the auxiliary file content seems too large.
     */
    protected final AuxiliaryContent readAuxiliaryFile(final String extension, final boolean acceptXML)
            throws URISyntaxException, IOException, DataStoreException
    {
        /*
         * Try to open the stream using the storage type (Path or URL) closest to the type
         * given at construction time. We do that because those two types cannot open the
         * same streams. For example, Path does not open HTTP or FTP connections by default,
         * and URL does not open S3 files in current implementation.
         */
        final InputStream stream;
        Path path = locationAsPath;
        final Object source;                    // In case an error message is produced.
        final URI sourceURI;                    // The source as an URI, or null.
        if (path != null) {
            if (extension != null) {
                path = path.resolveSibling(getBaseFilename(path).concat(extension));
            }
            stream    = open(path);
            source    = path;
            sourceURI = null;
        } else try {
            sourceURI = (extension != null) ? IOUtilities.toAuxiliaryURI(location, extension, true) : location;
            if (sourceURI == null) {
                return null;
            }
            final URL url = sourceURI.toURL();
            stream = url.openStream();
            source = url;
        } catch (URISyntaxException e) {
            throw new DataStoreException(cannotReadAuxiliaryFile(extension), e);
        }
        /*
         * If enabled, tests if the file is an XML file. If this is the case, we need to use `URISource`
         * for giving a chance of `org.apache.sis.xml.XML.unmarshal(Source)` to resolve relative links.
         */
        if (acceptXML && stream.markSupported() && org.apache.sis.storage.xml.AbstractProvider.isXML(stream)) {
            return new AuxiliaryContent(source, URISource.create(stream, (path != null) ? path.toUri() : sourceURI));
        }
        /*
         * If the auxiliary file is not an XML file, reads it fully as a text file with an arbitrary size limit.
         */
        var content = AuxiliaryContent.read(source, stream, encoding);
        if (content != null) {
            return content;
        }
        throw new DataStoreContentException(Resources.forLocale(getLocale())
                .getString(Resources.Keys.AuxiliaryFileTooLarge_1, IOUtilities.filename(source)));
    }

    /**
     * Creates a writer for an auxiliary file with the specified extension.
     * This method uses the same path as {@link #location},
     * except for the extension which is replaced by the given value.
     *
     * @param  extension  the filename extension of the auxiliary file to write.
     * @return a stream opened on the specified file.
     * @throws DataStoreException if the auxiliary file cannot be created.
     * @throws IOException if another error occurred while opening the stream.
     */
    protected final BufferedWriter writeAuxiliaryFile(final String extension) throws IOException, DataStoreException {
        Path path = locationAsPath;
        if (path == null) {
            throw new ReadOnlyStorageException(Resources.forLocale(getLocale())
                    .getString(Resources.Keys.CanNotWriteResource_1, getDisplayName()));
        }
        path = path.resolveSibling(getBaseFilename(path).concat(extension));
        return (encoding != null) ? Files.newBufferedWriter(path, encoding)
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
        String previous = null;
        final Optional<FileSet> files = getFileSet();
        if (files.isPresent()) {
            for (Path path : files.get().getPaths()) {
                final String base = getBaseFilename(path);
                if (!base.equals(previous)) {
                    previous = base;
                    path = path.resolveSibling(base.concat(extension));
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    /**
     * Returns the filename of the given path without the file suffix.
     * The returned string always ends in {@code '.'}, making it ready
     * for concatenation of a new suffix.
     */
    static String getBaseFilename(final Path path) {
        final String base = path.getFileName().toString();
        final int s = base.lastIndexOf(IOUtilities.EXTENSION_SEPARATOR);
        return (s >= 0) ? base.substring(0, s+1) : base + IOUtilities.EXTENSION_SEPARATOR;
    }

    /**
     * {@return the error message for saying that an auxiliary file cannot be read}.
     *
     * @param  extension  file extension (without leading dot) of the auxiliary file, or null for the main file.
     */
    protected final String cannotReadAuxiliaryFile(final String extension) {
        if (extension == null) {
            return Errors.forLocale(getLocale()).getString(Errors.Keys.CanNotRead_1, location);
        }
        return Resources.forLocale(getLocale()).getString(Resources.Keys.CanNotReadAuxiliaryFile_1, extension);
    }

    /**
     * Logs an error message saying that an auxiliary file cannot be read.
     *
     * @param  classe     the class to report as the source of the warning to log.
     * @param  method     the method to report as the source of the warning to log.
     * @param  extension  file extension (without leading dot) of the auxiliary file.
     * @param  cause      the reason why the auxiliary file cannot be read.
     * @param  warning    {@code true} for logging at warning level, or {@code false} for fine level.
     */
    protected final void cannotReadAuxiliaryFile(final Class<? extends DataStore> classe, final String method,
            final String extension, final Exception cause, final boolean warning)
    {
        final LogRecord record = Resources.forLocale(getLocale())
                .getLogRecord(warning ? Level.WARNING : Level.FINE, Resources.Keys.CanNotReadAuxiliaryFile_1, extension);
        record.setSourceClassName(classe.getCanonicalName());
        record.setSourceMethodName(method);
        record.setThrown(cause);
        listeners.warning(record);      // Logger name will be inferred by this method.
    }
}
