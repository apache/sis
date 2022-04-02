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
package org.apache.sis.internal.storage.ascii;

import java.util.Map;
import java.nio.ByteBuffer;
import java.io.EOFException;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.internal.storage.Capability;
import org.apache.sis.internal.storage.StoreMetadata;
import org.apache.sis.internal.storage.PRJDataStore;


/**
 * The provider of {@link Store} instances.
 * Given a {@link StorageConnector} input, this class tries to instantiate an ESRI ASCII Grid {@code Store}.
 *
 * <h2>Thread safety</h2>
 * The same {@code StoreProvider} instance can be safely used by many threads without synchronization on
 * the part of the caller. However the {@link Store} instances created by this factory are not thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
@StoreMetadata(formatName    = StoreProvider.NAME,
               fileSuffixes  = {"asc", "grd", "agr", "aig"},
               capabilities  = Capability.READ,
               resourceTypes = GridCoverageResource.class)
public final class StoreProvider extends PRJDataStore.Provider {
    /**
     * The format names for ESRI ASCII grid files.
     */
    static final String NAME = "ASCII Grid";

    /**
     * Creates a new provider.
     */
    public StoreProvider() {
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
     * Returns {@link ProbeResult#SUPPORTED} if the given storage appears to be supported by ASCII Grid {@link Store}.
     * Returning {@code SUPPORTED} from this method does not guarantee that reading or writing will succeed, only
     * that there appears to be a reasonable chance of success based on a brief inspection of the storage header.
     *
     * @return {@link ProbeResult#SUPPORTED} if the given storage seems to be readable as an ASCII Grid file.
     * @throws DataStoreException if an I/O or SQL error occurred.
     */
    @Override
    public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
        return probeContent(connector, ByteBuffer.class, (buffer) -> {
            /*
             * Quick check if all characters are US-ASCII.
             */
            buffer.mark();
            while (buffer.hasRemaining()) {
                if (buffer.get() < 0) {
                    return ProbeResult.UNSUPPORTED_STORAGE;
                }
            }
            buffer.reset();
            /*
             * Try to parse the header and check if we can find the expected keywords.
             */
            final CharactersView view = new CharactersView(null, buffer);
            try {
                final Map<String, String> header = view.readHeader();
                if (header.containsKey(Store.NROWS)     && header.containsKey(Store.NCOLS) &&
                   (header.containsKey(Store.XLLCORNER) || header.containsKey(Store.XLLCENTER)) &&
                   (header.containsKey(Store.YLLCORNER) || header.containsKey(Store.YLLCENTER)))
                {
cellsize:           if (!header.containsKey(Store.CELLSIZE)) {
                        int def = 0;
                        for (int i=0; i < Store.CELLSIZES.length;) {
                            if (header.containsKey(Store.CELLSIZES[i++])) def |= 1;
                            if (header.containsKey(Store.CELLSIZES[i++])) def |= 2;
                            if (def == 3) break cellsize;
                        }
                        return ProbeResult.UNSUPPORTED_STORAGE;
                    }
                    return new ProbeResult(true, "text/plain", null);
                }
            } catch (EOFException e) {
                return ProbeResult.INSUFFICIENT_BYTES;
            } catch (DataStoreException e) {
                // Ignore and return `UNSUPPORTED_STORAGE`.
            }
            return ProbeResult.UNSUPPORTED_STORAGE;
        });
    }

    /**
     * Returns a CSV {@link Store} implementation associated with this provider.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @return a data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final StorageConnector connector) throws DataStoreException {
        return new Store(this, connector);
    }
}
