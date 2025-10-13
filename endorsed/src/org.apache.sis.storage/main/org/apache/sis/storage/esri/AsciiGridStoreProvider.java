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

import java.util.Map;
import java.util.logging.Logger;
import java.nio.ByteBuffer;
import java.io.EOFException;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.StoreMetadata;
import org.apache.sis.storage.base.PRJDataStore;


/**
 * The provider of {@link AsciiGridStore} instances.
 * Given a {@link StorageConnector} input, this class tries to instantiate an {@code AsciiGridStore}.
 *
 * <h2>Thread safety</h2>
 * The same {@code AsciiGridStoreProvider} instance can be safely used by many threads without synchronization on
 * the part of the caller. However, the {@link AsciiGridStore} instances created by this factory are not thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@StoreMetadata(formatName    = AsciiGridStoreProvider.NAME,
               fileSuffixes  = {"asc", "grd", "agr", "aig"},
               capabilities  = {Capability.READ, Capability.WRITE, Capability.CREATE},
               resourceTypes = GridCoverageResource.class)
public final class AsciiGridStoreProvider extends PRJDataStore.Provider {
    /**
     * The format names for ESRI ASCII grid files.
     */
    static final String NAME = "ASCII Grid";

    /**
     * The logger used by ASCII grid stores.
     *
     * @see #getLogger()
     */
    private static final Logger LOGGER = Logger.getLogger("org.apache.sis.storage.esri");

    /**
     * Creates a new provider.
     */
    public AsciiGridStoreProvider() {
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
     * Returns the MIME type if the given storage appears to be supported by {@link AsciiGridStore}.
     * A {@linkplain ProbeResult#isSupported() supported} status does not guarantee that reading
     * or writing will succeed, only that there appears to be a reasonable chance of success
     * based on a brief inspection of the file header.
     *
     * @return a {@linkplain ProbeResult#isSupported() supported} status with the MIME type
     *         if the given storage seems to be readable as an ASCII Grid file.
     * @throws DataStoreException if an I/O error occurred.
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
                if (header.containsKey(AsciiGridStore.NROWS)     && header.containsKey(AsciiGridStore.NCOLS) &&
                   (header.containsKey(AsciiGridStore.XLLCORNER) || header.containsKey(AsciiGridStore.XLLCENTER)) &&
                   (header.containsKey(AsciiGridStore.YLLCORNER) || header.containsKey(AsciiGridStore.YLLCENTER)))
                {
cellsize:           if (!header.containsKey(AsciiGridStore.CELLSIZE)) {
                        int def = 0;
                        for (int i=0; i < AsciiGridStore.CELLSIZES.length;) {
                            if (header.containsKey(AsciiGridStore.CELLSIZES[i++])) def |= 1;
                            if (header.containsKey(AsciiGridStore.CELLSIZES[i++])) def |= 2;
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
     * Returns an {@link AsciiGridStore} implementation associated with this provider.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @return a data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final StorageConnector connector) throws DataStoreException {
        if (isWritable(connector, false)) {
            return new WritableStore(this, connector);
        } else {
            return new AsciiGridStore(this, connector, true);
        }
    }

    /**
     * Returns the logger used by ASCII grid stores.
     */
    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
