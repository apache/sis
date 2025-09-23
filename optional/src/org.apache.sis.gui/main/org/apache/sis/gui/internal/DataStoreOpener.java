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
package org.apache.sis.gui.internal;

import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.FileSystemNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.function.UnaryOperator;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.application.Platform;
import javafx.scene.Node;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.Identification;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.DataStore;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.io.stream.ChannelFactory;
import org.apache.sis.io.stream.InternalOptionKey;
import org.apache.sis.storage.folder.ConcurrentCloser;
import org.apache.sis.gui.DataViewer;


/**
 * Task in charge of opening a {@link DataStore} from a path or URL.
 * No action is registered by default;
 * caller should invoke {@link #setOnSucceeded(EventHandler)} for defining such action.
 * Example:
 *
 * {@snippet lang="java" :
 *     public void loadResource(Object source) {
 *         var opener = new DataStoreOpener(source);
 *         opener.setOnSucceeded((event) -> addResource((DataStore) event.getSource().getValue()));
 *         opener.setOnFailed(ExceptionReporter::show);
 *         BackgroundThreads.execute(opener);
 *     }
 * }
 *
 * This class maintains a cache. If the same {@code source} argument is given to the constructor
 * and the associated resource is still in memory, it will be returned directly.
 *
 * @todo Set title. Add progress listener and cancellation capability.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see BackgroundThreads#execute(Runnable)
 */
public final class DataStoreOpener extends Task<DataStore> {
    /**
     * The cache of previously loaded resources.
     * Used for avoiding to load the same resource twice.
     * Can be used from any thread.
     */
    private static final Cache<Object,DataStore> CACHE = new Cache<>();

    /**
     * Provider of wrappers around channels used for reading data.
     * Those wrappers can be used for listening to file accesses.
     *
     * <p><b>Note:</b> it should be a package-private field of {@link DataViewer}, but we put it here because
     * we have no public access to {@code DataViewer} non-public members. Using a static field is okay if only
     * one {@link DataViewer} application is running in the same JVM.</p>
     *
     * @see #setFactoryWrapper(UnaryOperator)
     * @see DataViewer#getCurrentStage()
     */
    private static volatile UnaryOperator<ChannelFactory> factoryWrapper;

    /**
     * The {@link DataStore} input.
     * This is usually a {@link File} or {@link Path}.
     */
    private final Object source;

    /**
     * Key to use in the {@link CACHE}, or {@code null} if the resource should not be cached.
     * If possible, this is a {@link Path} containing the real path. If we cannot perform
     * such conversion, then it is either {@code null} or the same object as {@link #source}.
     */
    private final Object key;

    /**
     * Creates a new task for opening the given input.
     *
     * @param  source  the source of the resource to load.
     *         This is usually a {@link File} or a {@link Path}.
     */
    public DataStoreOpener(Object source) {
        this.source = source;
        try {
            if (source instanceof StorageConnector) {
                source = ((StorageConnector) source).getStorage();
            }
            if (source instanceof AutoCloseable) {
                source = null;                                      // Do not cache InputStream, Channel, etc.
            } else {
                if (source instanceof File) {
                    source = ((File) source).getAbsoluteFile();     // First in case the next line fail.
                    source = ((File) source).toPath();              // May throw InvalidPathException.
                } else if (source instanceof URL) {
                    source = ((URL) source).toURI();                // May throw URISyntaxException.
                }
                if (source instanceof URI) {                        // May be the result of above URL.toURI().
                    source = Path.of((URI) source);                 // May throw FileSystemNotFoundException.
                }
                if (source instanceof Path) {                       // May be the result of a previous block.
                    source = ((Path) source).toRealPath();          // May throw IOException.
                }
            }
        } catch (URISyntaxException | FileSystemNotFoundException | IllegalArgumentException e) {
            // Ignore — keep `source` as is (File, URI, URI or non-absolute Path).
        } catch (DataStoreException | IOException | RuntimeException e) {
            source = null;
        }
        key = source;
    }

    /**
     * Returns the resource if it has already been loaded before, or {@code null} otherwise.
     * If loading is in progress in another thread, this method returns {@code null} without waiting.
     * This method can be invoked from any thread.
     *
     * @return the resource, or {@code null} if not yet available.
     */
    public DataStore fromCache() {
        return (key != null) ? CACHE.peek(key) : null;
    }

    /**
     * Invoked for loading the resource in a background thread.
     * If the source is a directory, the directory content will
     * be parsed as a set of sub-resources.
     *
     * @return the resource.
     * @throws Exception if an exception occurred while loading the resource.
     */
    @Override
    protected DataStore call() throws Exception {
        return (key != null) ? CACHE.getOrCreate(key, this::load) : load();
    }

    /**
     * Loads the resource after we verified that it is not in the cache.
     * This method is invoked from a background thread.
     */
    private DataStore load() throws DataStoreException {
        Object input = source;
        final UnaryOperator<ChannelFactory> wrapper = factoryWrapper;
        if (wrapper != null) {
            final StorageConnector connector;
            if (input instanceof StorageConnector) {
                connector = (StorageConnector) input;
            } else {
                connector = new StorageConnector(input);
            }
            connector.setOption(InternalOptionKey.CHANNEL_FACTORY_WRAPPER, wrapper);
            input = connector;
        }
        return DataStores.open(input);
    }

    /**
     * Returns the input filename, or "unknown" if we cannot infer the filename.
     * This is used for reporting errors.
     *
     * @return the input file name for message purpose.
     */
    final String getFileName() {
        if (source instanceof StorageConnector) {
            return ((StorageConnector) source).getStorageName();
        }
        String name = Strings.trimOrNull(IOUtilities.filename(source));
        if (name == null) {
            name = Strings.trimOrNull(IOUtilities.toString(source));
            if (name == null) {
                name = Vocabulary.format(Vocabulary.Keys.NotKnown);
            }
        }
        return name;
    }

    /**
     * Set the provider of wrappers around channels used for reading data.
     * Those wrappers can be used for listening to file accesses.
     *
     * @param  wrapper  the wrapper, or {@code null} if none.
     */
    public static void setFactoryWrapper(final UnaryOperator<ChannelFactory> wrapper) {
        factoryWrapper = wrapper;
    }

    /**
     * Returns a label for a resource. Current implementation returns the
     * {@linkplain DataStore#getDisplayName() data store display name} if available,
     * or the title found in {@linkplain Resource#getMetadata() metadata} otherwise.
     * If no label can be found, then this method returns the localized "Unnamed" string.
     *
     * <p>Identifiers can be very short, for example "1" or "2" meaning first or second image in a TIFF file.
     * If {@code qualified} is {@code true}, then this method tries to return a label such as "filename:id".
     * Generally {@code qualified} should be {@code false} if the label will be a node in a tree having the
     * filename as parent, and {@code true} if the label will be used outside the context of a tree.</p>
     *
     * <p>This operation may be costly. For example, the call to {@link Resource#getMetadata()}
     * may cause the resource to open a connection to the EPSG database.
     * Consequently, this method should be invoked in a background thread.</p>
     *
     * @param  resource   the resource for which to get a label, or {@code null}.
     * @param  locale     the locale to use for localizing international strings.
     * @param  qualified  whether to use fully-qualified path of generic names.
     * @return the resource display name or the citation title, never null.
     * @throws DataStoreException if an error occurred while fetching the resource identifier or metadata.
     */
    public static String findLabel(final Resource resource, final Locale locale, final boolean qualified)
            throws DataStoreException
    {
        if (resource != null) {
            final Long logID = LogHandler.loadingStart(resource);
            try {
                /*
                 * The data store display name is typically the file name. We give precedence to that name
                 * instead of the citation title because the citation may be the same for many files of
                 * the same product, while the display name have better chances to be distinct for each file.
                 */
                if (resource instanceof DataStore) {
                    final String name = Strings.trimOrNull(((DataStore) resource).getDisplayName());
                    if (name != null) return name;
                }
                /*
                 * Search for a title in metadata first because it has better chances to be human-readable
                 * compared to the resource identifier. If the title is the same text as the identifier,
                 * then execute the code path for identifier (i.e. try to find a more informative text).
                 */
                GenericName name = resource.getIdentifier().orElse(null);
                Collection<? extends Identification> identifications = null;
                final Metadata metadata = resource.getMetadata();
                if (metadata != null) {
                    identifications = metadata.getIdentificationInfo();
                    if (identifications != null) {
                        for (final Identification identification : identifications) {
                            final Citation citation = identification.getCitation();
                            if (citation != null) {
                                final String t = string(citation.getTitle(), locale);
                                if (t != null && (name == null || !t.equals(name.tip().toString()))) {
                                    return t;
                                }
                            }
                        }
                    }
                }
                /*
                 * If we find no title in the metadata, use the resource identifier.
                 * We search for explicitly declared identifier first before to fallback on
                 * metadata identifier, because the latter is more subject to interpretation.
                 */
                if (name != null) {
                    name = qualified ? name.toFullyQualifiedName() : name.tip();
                    final String t = string(name.toInternationalString(), locale);
                    if (t != null) return t;
                }
                if (identifications != null) {
                    for (final Identification identification : identifications) {
                        final String t = Citations.getIdentifier(identification.getCitation());
                        if (t != null) return t;
                    }
                }
            } finally {
                LogHandler.loadingStop(logID);
            }
        }
        return Vocabulary.forLocale(locale).getString(Vocabulary.Keys.Unnamed);
    }

    /**
     * Returns the given international string as a non-empty localized string, or {@code null} if none.
     */
    private static String string(final InternationalString i18n, final Locale locale) {
        return (i18n != null) ? Strings.trimOrNull(i18n.toString(locale)) : null;
    }

    /**
     * Removes the given data store from cache and closes it. It is caller's responsibility
     * to ensure that the given data store is not used anymore before to invoke this method.
     * This method should be invoked from JavaFX thread for making sure there is no new usage
     * of the given data store starting while we are closing it. However, after the data store
     * has been removed from the cache, the close action is performed in a background thread.
     *
     * @param  toClose  the data store to remove from the cache and to close.
     * @param  owner    the node invoking this method, used if a dialog must be shown.
     * @return {@code true} if the value has been removed from the cache, or {@code false}
     *         if it has not been found. Note that the data store is closed in all cases.
     */
    public static boolean removeAndClose(final DataStore toClose, final Node owner) {
        /*
         * A simpler code would be as below, but cannot be used at this time because our
         * Cache.entrySet() implementation does not support the Iterator.remove() operation.
         *
         * CACHE.values().removeIf((v) -> v == toClose);
         */
        boolean removed = false;
        for (final Cache.Entry<Object,DataStore> entries : CACHE.entrySet()) {
            if (entries.getValue() == toClose) {
                removed |= CACHE.remove(entries.getKey(), toClose);
            }
        }
        BackgroundThreads.execute(() -> {
            try {
                toClose.close();
            } catch (final Throwable e) {
                Platform.runLater(() -> {
                    ExceptionReporter.canNotCloseFile(owner, toClose.getDisplayName(), e);
                });
            }
        });
        return removed;
    }

    /**
     * Closes all data stores. This method should be invoked only after all background threads
     * terminated in case some of them were using a data store. The data stores will be closed
     * in parallel.
     *
     * @throws DataStoreException if an error occurred while closing at least one data store.
     */
    static void closeAll() throws DataStoreException {
        do CLOSER.closeAll(List.copyOf(CACHE.keySet()));
        while (!CACHE.isEmpty());
    }

    /**
     * Helper for closing concurrently the stores.
     */
    public static final ConcurrentCloser<Object> CLOSER = new ConcurrentCloser<>() {
        @Override protected Callable<?> closer(final Object key) {
            final DataStore store = CACHE.remove(key);
            if (store != null) return () -> {
                store.close();
                return null;
            };
            return null;
        }
    };
}
