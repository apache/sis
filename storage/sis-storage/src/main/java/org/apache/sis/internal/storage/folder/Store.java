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
import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Optional;
import java.util.logging.Level;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryIteratorException;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.NameSpace;
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
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.storage.MetadataBuilder;
import org.apache.sis.internal.storage.StoreUtilities;
import org.apache.sis.internal.storage.StoreResource;
import org.apache.sis.internal.storage.Resources;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.storage.event.WarningEvent;


/**
 * A folder store acts as an aggregate of multiple files in a single store.
 * Only visible files are considered; all hidden files are excluded.
 * Each visible file will be tested and eventually opened by another store.
 * This approach allows to discover the content of a folder or archive without
 * testing each file one by one.
 *
 * <h2>Limitations</h2>
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
 * @version 1.3
 * @since   0.8
 * @module
 */
class Store extends DataStore implements StoreResource, Aggregate, DirectoryStream.Filter<Path> {
    /**
     * The data store for the root directory specified by the user.
     * May be {@code this} if this store instance is for the root directory.
     */
    private final Store originator;

    /**
     * The {@link StoreProvider#LOCATION} parameter value.
     */
    protected final Path location;

    /**
     * An identifier for this folder, or {@code null} if not yet created. Only the root folder specified by
     * user has an initially null identifier. All sub-folders shall have a non-null identifier determined
     * at construction time.
     *
     * @see #identifier(NameFactory)
     */
    private GenericName identifier;

    /**
     * Formatting conventions of dates and numbers, or {@code null} if unspecified.
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
     * All data stores (including sub-folders) found in the directory structure, including the root directory.
     * This is used for avoiding never-ending loop with symbolic links.
     */
    final Map<Path,DataStore> children;

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
    transient Collection<Resource> components;

    /**
     * The provider to use for probing the directory content, opening files and creating new files.
     * The provider is determined by the format name specified at construction time.
     * This field is {@code null} if that format name is null.
     */
    protected final DataStoreProvider componentProvider;

    /**
     * {@code true} if {@link #sharedRepository(Path)} has already been invoked for {@link #location} path.
     * This is used for avoiding to report the same message many times.
     */
    private transient boolean sharedRepositoryReported;

    /**
     * Creates a new folder store from the given file, path or URI.
     * The folder store will attempt to open only the files of the given format, if non-null.
     * If a null format is specified, then the folder store will attempt to open any file
     * found in the directory (this may produce confusing results).
     *
     * @param  provider   the factory that created this {@code DataStore} instance, or {@code null} if unspecified.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @param  path       the value of {@code connector.getStorageAs(Path.class)}.
     * @param  format     format to use for reading or writing the directory content, or {@code null}.
     * @throws UnsupportedStorageException if the given format name is unknown.
     * @throws DataStoreException if an error occurred while fetching the directory {@link Path}.
     * @throws IOException if an error occurred while using the directory {@code Path}.
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction")    // Okay because 'children' does not escape.
    Store(final DataStoreProvider provider, final StorageConnector connector, final Path path, final DataStoreProvider format)
            throws DataStoreException, IOException
    {
        super(provider, connector);
        originator = this;
        location   = path;
        locale     = connector.getOption(OptionKey.LOCALE);
        timezone   = connector.getOption(OptionKey.TIMEZONE);
        encoding   = connector.getOption(OptionKey.ENCODING);
        children   = new ConcurrentHashMap<>();
        children.put(path.toRealPath(), this);
        componentProvider = format;
        listeners.useReadOnlyEvents();
    }

    /**
     * Creates a new sub-folder store as a child of the given folder store.
     *
     * @param  parent     the parent folder store.
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @throws DataStoreException if an error occurred while opening the stream.
     */
    private Store(final Store parent, final StorageConnector connector, final NameFactory nameFactory) throws DataStoreException {
        super(parent, parent.getProvider(), connector, false);
        originator        = parent;
        locale            = connector.getOption(OptionKey.LOCALE);
        timezone          = connector.getOption(OptionKey.TIMEZONE);
        encoding          = connector.getOption(OptionKey.ENCODING);
        location          = connector.commit(Path.class, StoreProvider.NAME);
        children          = parent.children;
        componentProvider = parent.componentProvider;
        identifier        = nameFactory.createLocalName(parent.identifier(nameFactory).scope(), super.getDisplayName());
    }

    /**
     * Returns the data store for the root directory specified by the user.
     */
    @Override
    public DataStore getOriginator() {
        return originator;
    }

    /**
     * Returns the parameters used to open this data store.
     */
    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        final String format = StoreUtilities.getFormatName(componentProvider);
        final ParameterValueGroup pg = (provider != null ? provider.getOpenParameters() : StoreProvider.PARAMETERS).createValue();
        pg.parameter(DataStoreProvider.LOCATION).setValue(location);
        if (locale   != null) pg.parameter("locale"  ).setValue(locale  );
        if (timezone != null) pg.parameter("timezone").setValue(timezone);
        if (encoding != null) pg.parameter("encoding").setValue(encoding);
        if (format   != null) pg.parameter("format"  ).setValue(format);
        return Optional.of(pg);
    }

    /**
     * Invoked during iteration for omitting hidden files.
     */
    @Override
    public boolean accept(final Path entry) throws IOException {
        return !Files.isHidden(entry);
    }

    /**
     * Returns the name of this folder.
     */
    @Override
    public Optional<GenericName> getIdentifier() {
        return Optional.of(identifier(null));
    }

    /**
     * Returns the name of this folder, creating it if needed.
     * Only the root folder may have its creation delayed.
     *
     * @param  nameFactory  the factory to use for creating the root folder, or {@code null} for the default.
     */
    private synchronized GenericName identifier(NameFactory nameFactory) {
        if (identifier == null) {
            if (nameFactory == null) {
                nameFactory = DefaultFactories.forBuildin(NameFactory.class);
            }
            GenericName name = nameFactory.createLocalName(null, super.getDisplayName());
            NameSpace   ns   = nameFactory.createNameSpace(name, Collections.singletonMap("separator", "/"));
            identifier       = nameFactory.createLocalName(ns, ".");
        }
        return identifier;
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
            mb.addResourceScope(ScopeCode.COLLECTION, Resources.formatInternational(Resources.Keys.DirectoryContent_1, getDisplayName()));
            mb.addLanguage(locale,   MetadataBuilder.Scope.RESOURCE);
            mb.addEncoding(encoding, MetadataBuilder.Scope.RESOURCE);
            String name = null;
            if (identifier != null) {
                name = identifier.toString();
                if (".".equals(name)) name = null;
            }
            if (name == null) {
                name = super.getDisplayName();              // User-specified folder (root of this resource).
            }
            mb.addTitleOrIdentifier(name, MetadataBuilder.Scope.RESOURCE);
            metadata = mb.buildAndFreeze();
        }
        return metadata;
    }

    /**
     * Returns all resources found in the folder given at construction time.
     * Only the resources recognized by a {@link DataStore} will be included.
     * Sub-folders are represented by other folder {@code Store} instances;
     * their resources are available by invoking {@link Aggregate#components()}
     * on them (this method does not traverse sub-folders recursively by itself).
     * Resources are in no particular order.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public synchronized Collection<Resource> components() throws DataStoreException {
        if (components == null) {
            final List<DataStore> resources = new ArrayList<>();
            final NameFactory nameFactory = DefaultFactories.forBuildin(NameFactory.class);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(location, this)) {
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
                        try {
                            if (componentProvider == null) {
                                next = DataStores.open(connector);          // May throw UnsupportedStorageException.
                            } else if (componentProvider.probeContent(connector).isSupported()) {
                                next = componentProvider.open(connector);   // Open a file of specified format.
                            } else if (Files.isDirectory(candidate)) {
                                next = new Store(this, connector, nameFactory);        // Open a sub-directory.
                            } else {
                                connector.closeAllExcept(null);             // Not the format specified at construction time.
                                continue;
                            }
                        } catch (UnsupportedStorageException ex) {
                            if (!Files.isDirectory(candidate)) {
                                connector.closeAllExcept(null);
                                listeners.warning(Level.FINE, null, ex);
                                continue;
                            }
                            next = new Store(this, connector, nameFactory);
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
            } catch (DirectoryIteratorException | UncheckedIOException ex) {
                // The cause is an IOException (no other type allowed).
                throw new DataStoreException(canNotRead(), ex.getCause());
            } catch (IOException ex) {
                throw new DataStoreException(canNotRead(), ex);
            } catch (BackingStoreException ex) {
                throw ex.unwrapOrRethrow(DataStoreException.class);
            }
            components = UnmodifiableArrayList.wrap(resources.toArray(new Resource[resources.size()]));
        }
        return components;              // Safe because unmodifiable list.
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
     * to handle cycles. Note that we have no guarantee that a cycle really exists at this stage, only that it may
     * exist.
     */
    private void sharedRepository(final Path candidate) {
        if (!sharedRepositoryReported) {
            sharedRepositoryReported = true;
            listeners.warning(message(Resources.Keys.SharedDirectory_1, candidate));
        }
    }

    /**
     * Returns the resource bundle to use for error message in exceptions.
     */
    final Resources messages() {
        return Resources.forLocale(getLocale());
    }

    /**
     * Returns a localized string for the given key and value.
     *
     * @param  key  one of the {@link Resources.Keys} constants ending with {@code _1} suffix.
     */
    final String message(final short key, final Object value) {
        return messages().getString(key, value);
    }

    /**
     * Registers a listener to notify when the specified kind of event occurs in this data store.
     * The current implementation of this data store can emit only {@link WarningEvent}s;
     * any listener specified for another kind of events will be ignored.
     */
    @Override
    public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
        // If an argument is null, we let the parent class throws (indirectly) NullArgumentException.
        if (listener == null || eventType == null || eventType.isAssignableFrom(WarningEvent.class)) {
            super.addListener(eventType, listener);
        }
    }

    /**
     * Closes all children resources.
     */
    @Override
    public synchronized void close() throws DataStoreException {
        listeners.close();                                          // Should never fail.
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
}
