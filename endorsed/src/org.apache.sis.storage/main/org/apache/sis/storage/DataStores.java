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

import java.util.Collection;
import java.util.function.Predicate;
import org.apache.sis.util.Static;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.image.DataStoreFilter;


/**
 * Static convenience methods creating {@link DataStore} instances from a given storage object.
 * Storage objects are typically {@link java.io.File} or {@link javax.sql.DataSource} instances,
 * but can also be any other objects documented in the {@link StorageConnector} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.4
 * @since   0.4
 */
public final class DataStores extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private DataStores() {
    }

    /**
     * Returns the set of available data store providers.
     * The returned collection is live: its content may change
     * if new modules are added on the module path at run-time.
     *
     * @return descriptions of available data stores.
     *
     * @since 0.8
     */
    public static Collection<DataStoreProvider> providers() {
        return DataStoreRegistry.INSTANCE;
    }

    /**
     * Returns the MIME type of the storage file format, or {@code null} if unknown or not applicable.
     *
     * @param  storage  the input/output object as a URL, file, image input stream, <i>etc.</i>.
     * @return the storage MIME type, or {@code null} if unknown or not applicable.
     * @throws DataStoreException if an error occurred while opening the storage.
     */
    public static String probeContentType(final Object storage) throws DataStoreException {
        return DataStoreRegistry.INSTANCE.probeContentType(storage);
    }

    /**
     * Creates a {@link DataStore} capable to read the given storage.
     * The {@code storage} argument can be any of the following types:
     *
     * <ul>
     *   <li>A {@link java.nio.file.Path} or a {@link java.io.File} for a file or a directory.</li>
     *   <li>A {@link java.net.URI} or a {@link java.net.URL} to a distant resource.</li>
     *   <li>A {@link java.lang.CharSequence} interpreted as a filename or a URL.</li>
     *   <li>A {@link java.nio.channels.Channel}, {@link java.io.DataInput}, {@link java.io.InputStream} or {@link java.io.Reader}.</li>
     *   <li>A {@link javax.sql.DataSource} or a {@link java.sql.Connection} to a JDBC database.</li>
     *   <li>Any other {@code DataStore}-specific object, for example {@link ucar.nc2.NetcdfFile}.</li>
     *   <li>An existing {@link StorageConnector} instance.</li>
     * </ul>
     *
     * @param  storage  the input object as a URL, file, image input stream, <i>etc.</i>.
     * @return the object to use for reading geospatial data from the given storage.
     * @throws UnsupportedStorageException if no {@link DataStoreProvider} is found for the given storage object.
     * @throws DataStoreException if an error occurred while opening the storage in read mode.
     */
    public static DataStore open(final Object storage) throws UnsupportedStorageException, DataStoreException {
        return DataStoreRegistry.INSTANCE.open(storage, Capability.READ, null);
    }

    /**
     * Creates a {@link DataStore} capable to write or update the given storage.
     * The {@code storage} argument can be any of the types documented in {@link #open(Object)}.
     * If the storage is a file and that file does not exist, then a new file will be created.
     * If the storage exists, then it will be opened in read/write mode for updates.
     * The returned data store should implement the {@link WritableGridCoverageResource},
     * {@link WritableFeatureSet} or {@link WritableAggregate} interface.
     *
     * <h4>Format selection</h4>
     * The {@code preferredFormat} argument can be a {@linkplain DataStoreProvider#getShortName() data store name}
     * (examples: {@code "CSV"}, {@code "GPX"}) or an {@linkplain javax.imageio.ImageIO Image I/O} name
     * (examples: {@code "TIFF"}, {@code "PNG"}). In the latter case, the WorldFile convention is used.
     *
     * <p>If the given storage exists (for example, an existing file), then the {@link DataStoreProvider} is determined
     * by probing the existing content and the {@code preferredFormat} argument may be ignored (it can be {@code null}).
     * Otherwise the {@link DataStoreProvider} is selected by a combination of {@code preferredFormat} (if non-null) and
     * file suffix (if the storage is a file path or URI).</p>
     *
     * @param  storage         the input/output object as a URL, file, image input stream, <i>etc.</i>.
     * @param  preferredFormat the format to use if not determined by the existing content, or {@code null}.
     * @return the object to use for writing geospatial data in the given storage.
     * @throws UnsupportedStorageException if no {@link DataStoreProvider} is found for the given storage object.
     * @throws DataStoreException if an error occurred while opening the storage in write mode.
     *
     * @since 1.4
     */
    public static DataStore openWritable(final Object storage, final String preferredFormat)
            throws UnsupportedStorageException, DataStoreException
    {
        Predicate<DataStoreProvider> preferred = null;
        if (preferredFormat != null) {
            preferred = new DataStoreFilter(preferredFormat, true);
        }
        return DataStoreRegistry.INSTANCE.open(storage, Capability.WRITE, preferred);
    }
}
