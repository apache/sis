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
package org.apache.sis.storage.esri;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.StoreMetadata;
import org.apache.sis.storage.base.PRJDataStore;
import org.apache.sis.io.stream.IOUtilities;


/**
 * The provider of {@link RawRasterStore} instances.
 * Given a {@link StorageConnector} input, this class tries to instantiate a {@code RawRasterStore}.
 *
 * <h2>Thread safety</h2>
 * The same {@code RawRasterStoreProvider} instance can be safely used by many threads without synchronization on
 * the part of the caller. However, the {@link RawRasterStore} instances created by this factory are not thread-safe.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
@StoreMetadata(formatName    = RawRasterStoreProvider.NAME,
               fileSuffixes  = {"bil", "bip", "bsq"},
               capabilities  = Capability.READ,
               resourceTypes = GridCoverageResource.class)
public final class RawRasterStoreProvider extends PRJDataStore.Provider {
    /**
     * The format names for raw binary raster files.
     */
    static final String NAME = "BIL/BIP/BSQ";

    /**
     * The filename extension of {@code "*.hdr"} files.
     */
    static final String HDR = "hdr";

    /**
     * The logger used by RAW stores.
     *
     * @see #getLogger()
     */
    private static final Logger LOGGER = Logger.getLogger("org.apache.sis.storage.esri");

    /**
     * Creates a new provider.
     */
    public RawRasterStoreProvider() {
    }

    /**
     * Returns a generic name for this data store, used mostly in warnings or error messages.
     *
     * @return a short name or abbreviation for the data format.
     */
    @Override
    public String getShortName() {
        return NAME;
    }

    /**
     * Returns {@link ProbeResult#SUPPORTED} if the given storage appears to be supported by {@link RawRasterStore}.
     * A {@linkplain ProbeResult#isSupported() supported} status does not guarantee that reading will succeed,
     * only that there appears to be a reasonable chance of success based on a brief inspection of the file header.
     *
     * @return {@link ProbeResult#SUPPORTED} if the given storage seems to be readable as a RAW file.
     * @throws DataStoreException if an I/O error occurred.
     */
    @Override
    public ProbeResult probeContent(final StorageConnector connector) throws DataStoreException {
        Path path = connector.getStorageAs(Path.class);
        if (path != null) {
            String filename = path.getFileName().toString();
            final int s = filename.lastIndexOf(IOUtilities.EXTENSION_SEPARATOR);
            filename = ((s >= 0) ? filename.substring(0, s+1) : filename + IOUtilities.EXTENSION_SEPARATOR) + HDR;
            path = path.resolveSibling(filename);
            if (Files.isRegularFile(path)) {
                // TODO: maybe we should do more tests here (open the file?)
                return ProbeResult.SUPPORTED;
            }
        } else if (connector.getStorageAs(URL.class) != null) {
            // Do not test auxiliary file existence because establishing a connection may be costly.
            return ProbeResult.UNDETERMINED;
        }
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

    /**
     * Returns an {@link RawRasterStore} implementation associated with this provider.
     *
     * @param  connector  information about the storage (URL, file, <i>etc</i>).
     * @return a data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final StorageConnector connector) throws DataStoreException {
        return new RawRasterStore(this, connector);
    }

    /**
     * Returns the logger used by RAW stores.
     */
    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
