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
package org.apache.sis.storage.landsat;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import org.opengis.util.NameSpace;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.FactoryException;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreClosedException;
import org.apache.sis.storage.DataStoreReferencingException;
import org.apache.sis.storage.UnsupportedStorageException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.storage.event.WarningEvent;
import org.apache.sis.storage.base.URIDataStore;
import org.apache.sis.storage.folder.ConcurrentCloser;
import org.apache.sis.util.iso.DefaultNameFactory;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.setup.OptionKey;


/**
 * Reads Landsat metadata and bands.
 * Landsat data are distributed as a collection of TIFF files,
 * together with a single text file like below:
 *
 * <pre class="text">
 * GROUP = L1_METADATA_FILE
 *   GROUP = METADATA_FILE_INFO
 *     ORIGIN = "Image courtesy of the U.S. Geological Survey"
 *     REQUEST_ID = "0501403126384_00011"
 *     LANDSAT_SCENE_ID = "LC81230522014071LGN00"
 *     FILE_DATE = 2014-03-12T06:06:35Z
 *     STATION_ID = "LGN"
 *     PROCESSING_SOFTWARE_VERSION = "LPGS_2.3.0"
 *   END_GROUP = METADATA_FILE_INFO
 *   GROUP = PRODUCT_METADATA
 *     DATA_TYPE = "L1T"
 *     ELEVATION_SOURCE = "GLS2000"
 *     OUTPUT_FORMAT = "GEOTIFF"
 *     SPACECRAFT_ID = "LANDSAT_8"
 *     SENSOR_ID = "OLI_TIRS"
 *     etc...</pre>
 *
 * This class reads the content from the given input until the first occurrence of the {@code END} keyword.
 * Lines beginning with the {@code #} character (ignoring spaces) are treated as comment lines and ignored.
 *
 * @author  Thi Phuong Hao Nguyen (VNSC)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 * @since   1.1
 */
public class LandsatStore extends DataStore implements Aggregate {
    /**
     * The reader, or {@code null} if closed.
     */
    private Reader source;

    /**
     * The file from which {@link #source} has been read, or {@code null} if unknown.
     */
    private final Path sourceFile;

    /**
     * The root directory where this file is located, or {@code null} if unknown.
     */
    final Path directory;

    /**
     * The {@link LandsatStoreProvider#LOCATION} parameter value, or {@code null} if none.
     */
    private final URI location;

    /**
     * The object returned by {@link #getMetadata()}, created when first needed and cached.
     */
    private Metadata metadata;

    /**
     * The identifier, cached when first requested.
     */
    private GenericName identifier;

    /**
     * The array of aggregates for each Landsat band group, or {@code null} if not yet created.
     * This array is created together with {@linkplain #metadata} and is unmodifiable.
     */
    @SuppressWarnings("VolatileArrayField")     // Array elements are not modified after creation.
    private volatile BandGroup[] components;

    /**
     * Creates a new Landsat store from the given file, URL, stream or character reader.
     * This constructor invokes {@link StorageConnector#closeAllExcept(Object)},
     * keeping open only the needed resource.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, reader instance, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the Landsat file.
     *
     * @since 1.5
     */
    @SuppressWarnings("this-escape")    // The invoked method does not store `this` and is not overrideable.
    public LandsatStore(final DataStoreProvider provider, final StorageConnector connector) throws DataStoreException {
        super(provider, connector);
        Path path = connector.getStorageAs(Path.class);
        location  = connector.getStorageAs(URI.class);
        source    = connector.getStorageAs(Reader.class);
        connector.closeAllExcept(source);
        Path file = null;
        if (path != null) {
            if (source != null) {
                file = path;
                path = path.getParent();        // If the source has been opened, then the path is a file.
            } else try {
                file = LandsatStoreProvider.getMetadataFile(path);
                if (file != null) {
                    final Charset encoding = connector.getOption(OptionKey.ENCODING);
                    if (encoding != null) {
                        source = Files.newBufferedReader(file, encoding);
                    } else {
                        source = Files.newBufferedReader(file);
                    }
                }
            } catch (IOException e) {
                throw new DataStoreException(e);
            }
        }
        sourceFile = file;
        directory  = path;
        if (source == null) {
            throw new UnsupportedStorageException(super.getLocale(), LandsatStoreProvider.NAME,
                    connector.getStorage(), connector.getOption(OptionKey.OPEN_OPTIONS));
        }
        if (getClass() == LandsatStore.class) {
            listeners.useReadOnlyEvents();
        }
    }

    /**
     * Returns the name of the directory that contains this data set.
     * The directory may not exist, for example if the data are read from a ZIP file.
     * The returned name can be used in user interfaces or in error messages.
     */
    @Override
    public final String getDisplayName() {
        return (directory != null) ? directory.getFileName().toString() : super.getDisplayName();
    }

    /**
     * Returns the parameters used to open this Landsat data store.
     * The parameters are described by {@link LandsatStoreProvider#getOpenParameters()} and contains at least
     * a parameter named {@value org.apache.sis.storage.DataStoreProvider#LOCATION} with a {@link URI} value.
     * The return value may be empty if the storage input cannot be described by a URI
     * (for example a Landsat file reading directly from a {@link java.nio.channels.ReadableByteChannel}).
     *
     * @return parameters used for opening this data store.
     *
     * @since 0.8
     */
    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        return Optional.ofNullable(URIDataStore.parameters(provider, location));
    }

    /**
     * Returns the value associated to {@code LANDSAT_SCENE_ID} in the Landsat metadata file.
     * This value is fetched from
     * <code>{@linkplain #getMetadata()}/identificationInfo/citation/identifier</code>.
     *
     * @return the identifier fetched from metadata, or {@code null} if none.
     * @throws DataStoreException if an error occurred while reading the metadata.
     *
     * @since 1.0
     */
    @Override
    public synchronized Optional<GenericName> getIdentifier() throws DataStoreException {
        if (identifier == null) {
            identifier = super.getIdentifier().orElse(null);
        }
        return Optional.ofNullable(identifier);
    }

    /**
     * Parses the main Landsat text file.
     * Also creates the array of components, but without loading GeoTIFF data yet.
     */
    private BandGroup[] loadMetadata() throws DataStoreException {
        if (source == null) {
            throw new DataStoreClosedException(getLocale(), LandsatStoreProvider.NAME, StandardOpenOption.READ);
        }
        final String      name    = getDisplayName();
        final NameFactory factory = DefaultNameFactory.provider();
        final NameSpace   scope   = (name != null) ? factory.createNameSpace(factory.createLocalName(null, name), null) : null;
        final Band[] resources;
        int count = 0;
        try (BufferedReader reader = (source instanceof BufferedReader) ? (BufferedReader) source : new LineNumberReader(source)) {
            source = null;      // Will be closed at the end of this try-finally block.
            final var parser = new MetadataReader(this, getDisplayName(), listeners);
            parser.read(reader);
            parser.addFormatReaderSIS(LandsatStoreProvider.NAME);
            metadata = parser.getMetadata();
            /*
             * Create the array of components. The resource identifier is the band name.
             * The namespace of each identifier is the name of the data set directory.
             */
            resources = new Band[parser.bands.size()];
            for (final Map.Entry<BandName,Band> entry : parser.bands.entrySet()) {
                final Band component = entry.getValue();
                if (component.filename != null) {
                    component.identifier = factory.createLocalName(scope, entry.getKey().name());
                    resources[count++] = component;
                }
            }
        } catch (IOException e) {
            throw new DataStoreException(e);
        } catch (FactoryException e) {
            throw new DataStoreReferencingException(e.getMessage(), e);
        }
        final BandGroup[] bands = BandGroup.group(this, resources, count);
        for (final BandGroup c : bands) {
            c.identifier = factory.createLocalName(scope, c.group.name());
        }
        components = bands;
        return bands;
    }

    /**
     * Returns information about the dataset as a whole. The returned metadata object can contain information
     * such as the spatiotemporal extent of the dataset, contact information about the creator or distributor,
     * data quality, usage constraints and more.
     *
     * @return information about the dataset.
     * @throws DataStoreException if an error occurred while reading the metadata.
     *
     * @see <a href="doc-files/MetadataMapping.html">Mapping from Landsat to ISO 19115 metadata</a>
     */
    @Override
    public synchronized Metadata getMetadata() throws DataStoreException {
        if (metadata == null) {
            loadMetadata();
        }
        return metadata;
    }

    /**
     * Returns the resources for each group of Landsat bands.
     *
     * @return all group of bands that are components of this aggregate. Never {@code null}.
     * @throws DataStoreException if an error occurred while fetching the components.
     *
     * @since 1.1
     */
    @Override
    public synchronized List<Aggregate> components() throws DataStoreException {
        BandGroup[] bands = components;
        if (bands == null) {
            bands = loadMetadata();
        }
        return Containers.viewAsUnmodifiableList(bands);
    }

    /**
     * Registers a listener to notify when the specified kind of event occurs in this data store.
     * The current implementation of this data store can emit only {@link WarningEvent}s;
     * any listener specified for another kind of events will be ignored.
     */
    @Override
    public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
        // If an argument is null, we let the parent class throws (indirectly) NullPointerException.
        if (listener == null || eventType == null || eventType.isAssignableFrom(WarningEvent.class)) {
            super.addListener(eventType, listener);
        }
    }

    /**
     * Returns the list of band files and the {@code *_MTL.txt} file.
     */
    @Override
    public Optional<FileSet> getFileSet() throws DataStoreException {
        final var paths = new LinkedHashSet<Path>();    // Order matter, as `sourceFile` should be first.
        paths.add(sourceFile);                          // May be null (all nulls are removed at the end).
        for (Aggregate b : components()) {
            b.getFileSet().map(FileSet::getPaths).ifPresent(paths::addAll);
        }
        paths.remove(null);
        return Optional.of(new FileSet(paths));
    }

    /**
     * Closes this Landsat store and releases any underlying resources.
     * This method can be invoked asynchronously for interrupting a long reading process.
     *
     * @throws DataStoreException if an error occurred while closing the Landsat file.
     */
    @Override
    public void close() throws DataStoreException {
        listeners.close();                  // Should never fail.
        try {
            CLOSER.closeAll(BandGroup.bands(components));
        } finally {
            synchronized (this) {
                metadata = null;
                components = null;
            }
        }
    }

    /**
     * Helper for closing concurrently the images for each band.
     */
    private static final ConcurrentCloser<Band> CLOSER = new ConcurrentCloser<>() {
        @Override protected Callable<?> closer(final Band r) {
            return () -> {
                r.closeDataStore();
                return null;
            };
        }
    };
}
