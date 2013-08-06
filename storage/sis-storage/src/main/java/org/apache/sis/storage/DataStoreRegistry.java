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

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.ServiceLoader;
import org.apache.sis.util.ThreadSafe;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * Creates {@link DataStore} instances for a given storage object by scanning all providers on the classpath.
 * Storage objects are typically {@link java.io.File} or {@link javax.sql.DataSource} instances, but can also
 * be any other objects documented in the {@link StorageConnector} class.
 *
 * {@note This class is package-private for now in order to get more experience about what could be a good API.
 *        This class may become public in a future SIS version.}
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@ThreadSafe
final class DataStoreRegistry {
    /**
     * The loader to use for searching for {@link DataStoreProvider} implementations.
     * Note that {@code ServiceLoader} are not thread-safe - usage of this field must
     * be protected in a synchronized block.
     */
    private final ServiceLoader<DataStoreProvider> loader;

    /**
     * Creates a new registry which will use the current thread
     * {@linkplain Thread#getContextClassLoader() context class loader}.
     */
    public DataStoreRegistry() {
        loader = ServiceLoader.load(DataStoreProvider.class);
    }

    /**
     * Creates a new registry which will use the given class loader.
     *
     * @param loader The class loader to use for loading {@link DataStoreProvider} implementations.
     */
    public DataStoreRegistry(final ClassLoader loader) {
        ArgumentChecks.ensureNonNull("loader", loader);
        this.loader = ServiceLoader.load(DataStoreProvider.class, loader);
    }

    /**
     * Creates a {@link DataStore} for reading the given storage.
     * The {@code storage} argument can be any of the following types:
     *
     * <ul>
     *   <li>A {@link java.nio.file.Path} or a {@link java.io.File} for a file or a directory.</li>
     *   <li>A {@link java.net.URI} or a {@link java.net.URL} to a distant resource.</li>
     *   <li>A {@link java.lang.CharSequence} interpreted as a filename or a URL.</li>
     *   <li>A {@link java.nio.channels.Channel} or a {@link java.io.DataInput}.</li>
     *   <li>A {@link javax.sql.DataSource} or a {@link java.sql.Connection} to a JDBC database.</li>
     *   <li>Any other {@code DataStore}-specific object, for example {@link ucar.nc2.NetcdfFile}.</li>
     *   <li>An existing {@link StorageConnector} instance.</li>
     * </ul>
     *
     * @param  storage The input/output object as a URL, file, image input stream, <i>etc.</i>.
     * @return The object to use for reading geospatial data from the given storage.
     * @throws UnsupportedStorageException if no {@link DataStoreProvider} is found for a given storage object.
     * @throws DataStoreException If an error occurred while opening the storage.
     */
    public DataStore open(final Object storage) throws DataStoreException {
        ArgumentChecks.ensureNonNull("storage", storage);
        StorageConnector connector;
        if (storage instanceof StorageConnector) {
            connector = (StorageConnector) storage;
        } else {
            connector = new StorageConnector(storage);
        }
        DataStoreProvider provider = null;
        List<DataStoreProvider> deferred = null;
        try {
            /*
             * All usages of 'loader' and its 'providers' iterator must be protected in a synchronized block,
             * because ServiceLoader is not thread-safe. We try to keep the synhronization block as small as
             * possible for less contention. In particular, the canOpen(connector) method call may be costly.
             */
            final Iterator<DataStoreProvider> providers;
            DataStoreProvider candidate;
            synchronized (loader) {
                providers = loader.iterator();
                candidate = providers.hasNext() ? providers.next() : null;
            }
search:     while (candidate != null) {
                switch (candidate.canOpen(connector)) {
                    /*
                     * Stop at the first provider claiming to be able to read the storage.
                     * Do not iterate over the list of deferred providers (if any).
                     */
                    case SUPPORTED: {
                        provider = candidate;
                        deferred = null;
                        break search;
                    }
                    /*
                     * If a provider doesn't have enough bytes for answering the question,
                     * try again after this loop with more bytes in the buffer, unless we
                     * found an other provider.
                     */
                    case INSUFFICIENT_BYTES: {
                        if (deferred == null) {
                            deferred = new LinkedList<>();
                        }
                        deferred.add(candidate);
                        break;
                    }
                    /*
                     * If a provider doesn't know whether it can open the given storage,
                     * we will try it only if we find no provider retuning SUPPORTED.
                     *
                     * TODO: What to do if we find more than one provider here? We can not invoke
                     *       provider.open(connector) in a try … catch block because it may leave
                     *       the StorageConnector in an invalid state in case of failure.
                     */
                    case UNDETERMINED: {
                        provider = candidate;
                        break;
                    }
                }
                synchronized (loader) {
                    candidate = providers.hasNext() ? providers.next() : null;
                }
            }
            /*
             * If any provider did not had enough bytes for answering the 'canOpen(…)' question,
             * get more bytes and try again. We try to prefetch more bytes only if we have no choice
             * in order to avoid latency on network connection.
             */
            if (deferred != null) {
search:         while (!deferred.isEmpty() && connector.prefetch()) {
                    for (final Iterator<DataStoreProvider> it=deferred.iterator(); it.hasNext();) {
                        candidate = it.next();
                        switch (candidate.canOpen(connector)) {
                            case SUPPORTED:          provider = candidate; break search;
                            case UNDETERMINED:       provider = candidate; break;
                            case INSUFFICIENT_BYTES: continue; // Will try again in next iteration.
                        }
                        it.remove(); // UNSUPPORTED_* or UNDETERMINED: do not try again those providers.
                    }
                }
            }
            /*
             * If a provider has been found, or if a provider returned UNDETERMINED, use that one
             * for opening a DataStore. Note that if more than one provider returned UNDETERMINED,
             * the selected one is arbitrary and may change in different execution. Implementors
             * shall avoid the UNDETERMINED value as much as possible (this value should be used
             * only for RAW image format).
             */
            if (provider != null) {
                final DataStore data = provider.open(connector);
                connector = null; // For preventing it to be closed.
                return data;
            }
        } finally {
            if (connector != null && connector != storage) {
                connector.closeAllExcept(null);
            }
        }
        throw new UnsupportedStorageException(Errors.format(Errors.Keys.UnknownFormatFor_1, connector.getStorageName()));
    }
}
