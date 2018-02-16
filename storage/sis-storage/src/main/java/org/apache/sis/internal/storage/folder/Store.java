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
package org.apache.sis.internal.storage.folder;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.TimeZone;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryIteratorException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.apache.sis.internal.storage.FileSystemResource;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.maintenance.ScopeCode;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.UnsupportedStorageException;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.internal.storage.URIDataStore;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.ReadOnlyStorageException;
import org.apache.sis.storage.WritableAggregate;
import org.apache.sis.storage.WritableFeatureSet;
import org.opengis.feature.Feature;
import org.opengis.metadata.identification.Identification;


/**
 * A folder store acts as an aggregate of multiple files in a single store.
 * Only visible files are considered; all hidden files are excluded.
 * Each visible file will be tested and eventually opened by another store.
 * This approach allows to discover the content of a folder or archive without
 * testing each file one by one.
 *
 * <p><b>Limitations:</b></p>
 * <ul>
 *   <li>Current version is read-only.</li>
 *   <li>Current version does not watch for external modifications in directory content.</li>
 *   <li>Current version open all files in the directory and keep those files open.
 *       If the directory is large, it will be a problem.</li>
 *   <li>We could open data stores concurrently. This is not yet done.</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
class Store extends DataStore implements Aggregate, DirectoryStream.Filter<Path> {

    /**
     * File walker to delete file and folder recursively.
     */
    private static final SimpleFileVisitor<Path> FILE_DELETE = new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };

    /**
     * The {@link FolderStoreProvider#LOCATION} parameter value, or {@code null} if none.
     */
    protected final Path location;

    /**
     * Formating conventions of dates and numbers, or {@code null} if unspecified.
     */
    protected final Locale locale;

    /**
     * Timezone of dates in the data store, or {@code null} if unspecified.
     */
    protected final TimeZone timezone;

    /**
     * Character encoding used by the data store, or {@code null} if unspecified.
     */
    protected final Charset encoding;

    /**
     * Single provider to use in searches and creation operations, or {@code null} if unspecified.
     */
    protected final String providerName;

    /**
     * All data stores (including sub-folders) found in the directory structure, including the root directory.
     * This is used for avoiding never-ending loop with symbolic links.
     */
    protected final Map<Path,DataStore> children;

    /**
     * Information about the data store as a whole, created when first needed.
     *
     * @see #getMetadata()
     */
    private transient Metadata metadata;

    /**
     * Resources in the folder given at construction time, created when first needed.
     *
     * @see #components()
     */
    protected transient Collection<Resource> components;

    /**
     * {@code true} if {@link #sharedRepository(Path)} has already been invoked for {@link #location} path.
     * This is used for avoiding to report the same message many times.
     */
    private transient boolean sharedRepositoryReported;

    /**
     * Cached search and create provider to use.
     */
    private transient DataStoreProvider searchProvider;

    /**
     * Creates a new folder store from the given file, path or URI.
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the stream.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")    // Okay because 'folders' does not escape.
    Store(final DataStoreProvider provider, final StorageConnector connector) throws DataStoreException, IOException {
        super(provider, connector);
        location = connector.getStorageAs(Path.class);
        locale   = connector.getOption(OptionKey.LOCALE);
        timezone = connector.getOption(OptionKey.TIMEZONE);
        encoding = connector.getOption(OptionKey.ENCODING);
        children = new ConcurrentHashMap<>();
        children.put(location.toRealPath(), this);
        providerName = null;
    }

    Store(final DataStoreProvider provider, final Parameters params) throws DataStoreException, IOException {
        super(provider, new StorageConnector(params));
        location     = params.getValue(FolderStoreProvider.LOCATION);
        locale       = params.getValue(FolderStoreProvider.LOCALE);
        timezone     = params.getValue(FolderStoreProvider.TIMEZONE);
        encoding     = params.getValue(FolderStoreProvider.ENCODING);
        providerName = params.getValue(FolderStoreProvider.PROVIDER);
        children     = new ConcurrentHashMap<>();
        children.put(location.toRealPath(), this);
    }

    /**
     * Creates a new sub-folder store as a child of the given folder store.
     *
     * @param  parent     the parent folder store.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the stream.
     */
    private Store(final Store parent, final StorageConnector connector) throws DataStoreException {
        super(parent, connector);
        location       = connector.getStorageAs(Path.class);
        locale         = connector.getOption(OptionKey.LOCALE);
        timezone       = connector.getOption(OptionKey.TIMEZONE);
        encoding       = connector.getOption(OptionKey.ENCODING);
        children       = parent.children;
        providerName   = parent.providerName;
        searchProvider = parent.searchProvider;
    }

    /**
     * Returns the parameters used to open this data store.
     */
    @Override
    public ParameterValueGroup getOpenParameters() {
        final ParameterValueGroup pg = (provider != null ? provider.getOpenParameters() : FolderStoreProvider.PARAMETERS).createValue();
        pg.parameter(DataStoreProvider.LOCATION).setValue(location);
        if (locale       != null) pg.parameter("locale"  ).setValue(locale  );
        if (timezone     != null) pg.parameter("timezone").setValue(timezone);
        if (encoding     != null) pg.parameter("encoding").setValue(encoding);
        if (providerName != null) pg.parameter("provider").setValue(providerName);
        return pg;
    }

    /**
     * Invoked during iteration for omitting hidden files.
     */
    @Override
    public boolean accept(final Path entry) throws IOException {
        return !Files.isHidden(entry);
    }

    /**
     * Returns information about the data store as a whole.
     * Those metadata contains the directory name in the resource title.
     *
     * @return information about resources in the data store.
     */
    @Override
    public synchronized Metadata getMetadata() {
        if (metadata == null) {
            final MetadataBuilder mb = new MetadataBuilder();
            final String name = getDisplayName();
            mb.addResourceScope(ScopeCode.COLLECTION, Resources.formatInternational(Resources.Keys.DirectoryContent_1, name));
            mb.addLanguage(locale,   MetadataBuilder.Scope.RESOURCE);
            mb.addEncoding(encoding, MetadataBuilder.Scope.RESOURCE);
            mb.addTitleOrIdentifier(name, MetadataBuilder.Scope.ALL);
            metadata = mb.build(true);
        }
        return metadata;
    }

    /**
     * Returns all resources found in the folder given at construction time.
     * Only the resources recognized by a {@link DataStore} will be included.
     * This includes sub-folders. Resources are in no particular order.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public synchronized Collection<Resource> components() throws DataStoreException {
        if (components == null) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(location, this)) {
                final List<DataStore> resources = new ArrayList<>();
                for (final Path candidate : stream) {
                    /*
                     * The candidate path may be a symbolic link to a file that we have previously read.
                     * In such case, use the existing data store.   A use case is a directory containing
                     * hundred of GeoTIFF files all accompanied by ".prj" files having identical content.
                     * (Note: those ".prj" files should be invisible since they should be identified as
                     * GeoTIFF auxiliary files, but current Store implementation does not know that).
                     */
                    final Path real = candidate.toRealPath();
                    DataStore next = children.get(real);
                    if (next instanceof Store) {
                        ((Store) next).sharedRepository(real);          // Warn about directories only.
                    }
                    if (next == null) {
                        /*
                         * The candidate file has never been read before. Try to read it now.
                         * If the file format is unknown (UnsupportedStorageException), we will
                         * check if we can open it as a child folder store before to skip it.
                         */
                        final StorageConnector connector = new StorageConnector(candidate);
                        connector.setOption(OptionKey.LOCALE,   locale);
                        connector.setOption(OptionKey.TIMEZONE, timezone);
                        connector.setOption(OptionKey.ENCODING, encoding);

                        final DataStoreProvider provider = getSearchAndCreateProvider();
                        try {
                            if (provider != null) {
                                final ProbeResult result = provider.probeContent(connector);
                                if (result.isSupported()) {
                                    next = provider.open(connector);
                                } else {
                                    throw new UnsupportedStorageException();
                                }
                            } else {
                                next = DataStores.open(connector);
                            }

                        } catch (UnsupportedStorageException ex) {
                            if (!Files.isDirectory(candidate)) {
                                connector.closeAllExcept(null);
                                listeners.warning(Level.FINE, null, ex);
                                continue;
                            }
                            next = new Store(this, connector);
                        } catch (DataStoreException ex) {
                            try {
                                connector.closeAllExcept(null);
                            } catch (DataStoreException s) {
                                ex.addSuppressed(s);
                            }
                            throw ex;
                        }
                        /*
                         * At this point we got the data store. It could happen that a store for
                         * the same file has been added concurrently, so we need to check again.
                         */
                        final DataStore existing = children.putIfAbsent(real, next);
                        if (existing != null) {
                            next.close();
                            next = existing;
                            if (next instanceof Store) {
                                ((Store) next).sharedRepository(real);      // Warn about directories only.
                            }
                        }
                    }
                    resources.add(next);
                }
                components = UnmodifiableArrayList.wrap(resources.toArray(new Resource[resources.size()]));
            } catch (DirectoryIteratorException | UncheckedIOException ex) {
                // The cause is an IOException (no other type allowed).
                throw new DataStoreException(canNotRead(), ex.getCause());
            } catch (IOException ex) {
                throw new DataStoreException(canNotRead(), ex);
            } catch (BackingStoreException ex) {
                throw ex.unwrapOrRethrow(DataStoreException.class);
            }
        }
        return components;              // Safe because unmodifiable list.
    }

    /**
     *
     * @return search and create provider, can be null
     * @throws DataStoreException
     */
    protected DataStoreProvider getSearchAndCreateProvider() throws DataStoreException {
        if (searchProvider == null && providerName != null) {
            for (DataStoreProvider provider : DataStores.providers()) {
                if (providerName.equals(provider.getShortName())) {
                    searchProvider = provider;
                    break;
                }
            }
            if (searchProvider == null) {
                throw new DataStoreException(message(Resources.Keys.FolderStoreProviderUnknown_1, providerName));
            }
        }
        return searchProvider;
    }
    /**
     * Builds an error message for an error occurring while reading files in the directory.
     */
    private String canNotRead() {
        return message(Resources.Keys.CanNotReadDirectory_1, getDisplayName());
    }

    /**
     * Logs a warning about a file that could be read, but happen to be a directory that we have read previously.
     * We could add the existing {@link Aggregate} instance in the parent {@code Aggregate} that we are building,
     * but doing so may create a cycle. Current version logs a warning instead because users may not be prepared
     * to handle cycles. Not that we have no guarantee that a cycle really exists at this stage, only that it may
     * exist.
     */
    private void sharedRepository(final Path candidate) {
        if (!sharedRepositoryReported) {
            sharedRepositoryReported = true;
            listeners.warning(message(Resources.Keys.SharedDirectory_1, candidate), null);
        }
    }

    /**
     * Returns a localized string for the given key and value.
     *
     * @param  key  one of the {@link Resources.Keys} constants ending with {@code _1} suffix.
     */
    private String message(final short key, final Object value) {
        return Resources.forLocale(getLocale()).getString(key, value);
    }

    /**
     * Closes all children resources.
     */
    @Override
    public synchronized void close() throws DataStoreException {
        final Collection<Resource> resources = components;
        if (resources != null) {
            components = null;                                      // Clear first in case of failure.
            DataStoreException failure = null;
            for (final Resource r : resources) {
                if (r instanceof DataStore) try {
                    ((DataStore) r).close();
                } catch (DataStoreException ex) {
                    if (failure == null) {
                        failure = ex;
                    } else {
                        failure.addSuppressed(ex);
                    }
                }
            }
            if (failure != null) {
                throw failure;
            }
        }
    }

    /**
     * Writable version of the store which rely on given datastore provider to create new types.
     *
     * Note 1 : this implementation is experimental.
     * Note 2 : it has not been tested since we do not have writable feature sets yet.
     */
    static class Writable extends Store implements WritableAggregate {

        public Writable(DataStoreProvider provider, Parameters params) throws DataStoreException, IOException {
            super(provider, params);
        }

        /**
         * Create a new resource.
         * This implementation uses the provider given in store creation parameters.
         *
         * @param resource
         * @return
         * @throws DataStoreException
         */
        @Override
        public synchronized Resource add(Resource resource) throws DataStoreException {
            if (!(resource instanceof FeatureSet)) {
                throw new DataStoreException("Only FeatureSet resources can be imported in this store.");
            }

            if (components().contains(resource)) {
                throw new DataStoreException("Resource is already in this aggregate.");
            }

            //we know it is not null in this instance
            final DataStoreProvider provider = getSearchAndCreateProvider();
            if (!(provider instanceof URIDataStore.Provider)) {
                throw new DataStoreException("Resource creation is possible only with URIProviders");
            }

            final URIDataStore.Provider p = (URIDataStore.Provider) provider;

            //build location
            String fileName = null;
            for (Identification id : resource.getMetadata().getIdentificationInfo()) {
                fileName = Citations.getIdentifier(id.getCitation());
                if (fileName!=null && !fileName.isEmpty()) break;
            }
            if (fileName == null || fileName.isEmpty()) {
                throw new DataStoreException("Resource does not have an identifier.");
            }

            //some format may have no suffix at all
            if (!p.getSuffix().isEmpty()) {
                fileName += "."+ p.getSuffix().get(0);
            }

            //create new store/resource
            final Path location = this.location.resolve(fileName);
            final StorageConnector connector = new StorageConnector(location);
            connector.setOption(OptionKey.LOCALE,   locale);
            connector.setOption(OptionKey.TIMEZONE, timezone);
            connector.setOption(OptionKey.ENCODING, encoding);
            final DataStore store = p.open(connector);

            //check we can write datas
            if (!(store instanceof WritableFeatureSet)) {
                try {
                    //remove any created file
                    if (resource instanceof FileSystemResource) {
                        //delete resource files
                        final Path[] resourcePaths = ((FileSystemResource) resource).getResourcePaths();
                        for (Path path : resourcePaths) {
                            Files.walkFileTree(path, FILE_DELETE);
                        }
                    }
                    Files.deleteIfExists(location);
                } catch (IOException ex) {
                    //do nothing
                } finally {
                    store.close();
                }
                throw new DataStoreException("Created resource is not a WritableFeatureSet.");
            }

            //copy datas between resources
            children.put(location, store);
            final FeatureSet source = (FeatureSet) resource;
            final WritableFeatureSet target = (WritableFeatureSet) store;
            target.updateType(source.getType());
            try (Stream<Feature> stream = source.features(false)) {
                target.add(stream.iterator());
            }


            //clear cache
            components = null;

            return store;
        }

        /**
         * Note : in this implementation we clear the cache after closing the stores and before deleting the files.
         * This ensure in the worse case scenario a new store will be created on the possible remaining files.
         *
         * @param resource
         * @throws ReadOnlyStorageException
         * @throws DataStoreException
         */
        @Override
        public synchronized void remove(Resource resource) throws ReadOnlyStorageException, DataStoreException {
            if (!(components().contains(resource))) {
                throw new DataStoreException("Unknown resource, verify it is part of this aggregate.");
            }

            //clear cache
            components = null;

            if (resource instanceof Store) {
                final Store store = (Store) resource;
                store.close();
                //clear cache
                children.remove(store.location);

                try {
                    Files.walkFileTree(store.location, FILE_DELETE);
                } catch (IOException ex) {
                    throw new DataStoreException(ex.getMessage(), ex);
                }
            } else {
                //resource is a datastore, we are sure of it
                final DataStore store = (DataStore) resource;
                store.close();

                //clear cache, we need to do this loop in case the resource is
                //not a FileSystemResource or wrongly declares the used files
                for (Entry<Path,DataStore> entry : children.entrySet()) {
                    if (entry.getValue() == store) {
                        children.remove(entry.getKey());
                        break;
                    }
                }

                if (resource instanceof FileSystemResource) {
                    //delete resource files
                    final Path[] resourcePaths = ((FileSystemResource) resource).getResourcePaths();
                    for (Path path : resourcePaths) {
                        try {
                            Files.walkFileTree(path, FILE_DELETE);
                        } catch (IOException ex) {
                            throw new DataStoreException(ex.getMessage(), ex);
                        }
                    }
                }
            }
        }

    }

}
