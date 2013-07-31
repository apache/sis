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

import org.apache.sis.util.Static;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.system.SystemListener;


/**
 * Creates {@link DataStore} instances from a given storage object.
 * Storage objects are typically {@link java.io.File} or {@link javax.sql.DataSource} instances,
 * but can also be any other objects documented in the {@link StorageConnector} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final class DataStores extends Static {
    /**
     * The registry to use for searching for {@link DataStoreProvider} implementations.
     *
     * {@section Class loader}
     * In current implementation, this registry is instantiated when first needed using the
     * {@linkplain Thread#getContextClassLoader() context class loader}. This means that the set of
     * available formats may depend on the first thread that invoked a {@code DataStores} method.
     */
    private static volatile DataStoreRegistry registry;

    /*
     * Forces a reload of all providers when the classpath changes. Note that invoking
     * ServiceLoader.reload() is not sufficient because the ClassLoader may also change
     * in OSGi context.
     */
    static {
        SystemListener.add(new SystemListener(Modules.STORAGE) {
            @Override protected void classpathChanged() {
                registry = null;
            }
        });
    }

    /**
     * Do not allow instantiation of this class.
     */
    private DataStores() {
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
    public static DataStore open(final Object storage) throws DataStoreException {
        DataStoreRegistry r = registry;
        if (r == null) {
            synchronized (DataStores.class) {
                r = registry;
                if (r == null) {
                    registry = r = new DataStoreRegistry();
                }
            }
        }
        return r.open(storage);
    }
}
