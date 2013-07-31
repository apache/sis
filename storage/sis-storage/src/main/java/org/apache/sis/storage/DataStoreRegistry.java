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
        StorageConnector connector;
        if (storage instanceof StorageConnector) {
            connector = (StorageConnector) storage;
        } else {
            connector = new StorageConnector(storage);
        }
        try {
            DataStoreProvider provider = null;
            synchronized (loader) {
                for (final DataStoreProvider candidate : loader) {
                    final Boolean canOpen = candidate.canOpen(connector);
                    if (canOpen == null) {
                        // TODO: not enough information.
                    } else if (canOpen) {
                        provider = candidate;
                        break;
                    }
                }
            }
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
