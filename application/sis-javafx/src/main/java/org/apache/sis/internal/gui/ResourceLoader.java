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
package org.apache.sis.internal.gui;

import java.net.URI;
import java.net.URL;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.application.Platform;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.internal.storage.folder.FolderStoreProvider;
import org.apache.sis.internal.storage.io.IOUtilities;
import org.apache.sis.storage.DataStore;


/**
 * Task in charge of loading a {@link org.apache.sis.storage.Resource} from a data store.
 * No action is registered by default;
 * caller should invoke {@link #setOnSucceeded(EventHandler)} for defining such action.
 * Example:
 *
 * {@preformat java
 *     public void loadResource(final Object source) {
 *         final ResourceLoader loader = new ResourceLoader(source);
 *         loader.setOnSucceeded((event) -> addResource((Resource) event.getSource().getValue()));
 *         loader.setOnFailed(ExceptionReporter::show);
 *         BackgroundThreads.execute(loader);
 *     }
 * }
 *
 * This class maintains a cache. If the same {@code source} argument is given to the constructor
 * and the associated resource is still in memory, it will be returned directly.
 *
 * @todo Set title. Add progress listener and cancellation capability.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @see BackgroundThreads#execute(Runnable)
 *
 * @since 1.1
 * @module
 */
public final class ResourceLoader extends Task<Resource> {
    /**
     * The cache of previously loaded resources.
     * Used for avoiding to load the same resource twice.
     * Can be used from any thread.
     */
    private static final Cache<Object,DataStore> CACHE = new Cache<>();

    /**
     * The {@link Resource} input.
     * This is usually a {@link File} or {@link Path}.
     */
    private final Object source;

    /**
     * Key to use in the {@link CACHE}, or {@code null} if the resource should not be cached.
     * If possible, this is a {@link Path} containing the real path. If we can not perform
     * such conversion, then it is either {@code null} or the same object than {@link #source}.
     */
    private final Object key;

    /**
     * Creates a new task for opening the given input.
     *
     * @param  source  the source of the resource to load.
     *         This is usually a {@link File} or {@link Path}.
     */
    public ResourceLoader(Object source) {
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
                    source = Paths.get((URI) source);               // May throw FileSystemNotFoundException.
                }
                if (source instanceof Path) {                       // May be the result of a previous block.
                    source = ((Path) source).toRealPath();          // May throw IOException.
                }
            }
        } catch (URISyntaxException | IOException | IllegalArgumentException e) {
            // Ignore — keep `source` as is (File, URI, URI or non-absolute Path).
        } catch (DataStoreException | RuntimeException e) {
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
     */
    private DataStore load() throws DataStoreException {
        Object input = source;
        if (input instanceof StorageConnector) {
            input = ((StorageConnector) input).getStorage();
        }
        if ((input instanceof File && ((File) input).isDirectory()) ||
            (input instanceof Path && Files.isDirectory((Path) input)))
        {
            return FolderStoreProvider.INSTANCE.open((source instanceof StorageConnector)
                              ? (StorageConnector) source : new StorageConnector(input));
        }
        return DataStores.open(source);
    }

    /**
     * Returns the input filename, or "unknown" if we can not infer the filename.
     * This is used for reporting errors.
     */
    final String getFileName() {
        if (source instanceof StorageConnector) {
            return ((StorageConnector) source).getStorageName();
        }
        String name = IOUtilities.filename(source);
        if (name == null) {
            name = Vocabulary.format(Vocabulary.Keys.Unknown);
        }
        return name;
    }

    /**
     * Removes the given data store from cache and closes it. It is caller's responsibility
     * to ensure that the given data store is not used anymore before to invoke this method.
     * This method should be invoked from JavaFX thread for making sure there is no new usage
     * of the given data store starting while we are closing it. However after the data store
     * has been removed from the cache, the close action is performed in a background thread.
     *
     * @param  toClose  the data store to remove from the cache and to close.
     * @return {@code true} if the value has been removed from the cache, or {@code false}
     *         if it has not been found. Note that the data store is closed in all cases.
     */
    public static boolean removeAndClose(final DataStore toClose) {
        /*
         * A simpler code would be as below, but can not be used at this time because our
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
                    ExceptionReporter.canNotCloseFile(toClose.getDisplayName(), e);
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
     * @throws Exception if an error occurred while closing at least one data store.
     */
    static void closeAll() throws Exception {
        final Closer closer = new Closer();
        do {
            Stream.of(CACHE.keySet().toArray()).parallel().forEach(closer);
        } while (!CACHE.isEmpty());
        closer.rethrow();
    }

    /**
     * The handler in charge of closing the data store and record the failures if some errors happen.
     * The same handler instance may be used concurrently while closing many data stores in parallel.
     */
    private static final class Closer implements Consumer<Object> {
        /** The error that occurred while closing a data store. */
        private Exception error;

        /** Closes the given data store. */
        @Override public void accept(final Object source) {
            final DataStore toClose = CACHE.remove(source);
            if (source != null) try {
                toClose.close();
            } catch (Exception e) {
                synchronized (this) {
                    if (error == null) error = e;
                    else error.addSuppressed(e);
                }
            }
        }

        /** If an error occurred, re-throws that error. */
        synchronized void rethrow() throws Exception {
            if (error != null) {
                throw error;
            }
        }
    }
}
