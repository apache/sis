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
package org.apache.sis.storage.geotiff;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.util.Version;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.internal.storage.Capabilities;
import org.apache.sis.internal.storage.Capability;
import org.apache.sis.internal.storage.URIDataStore;
import org.apache.sis.internal.util.Constants;


/**
 * The provider of {@link GeoTiffStore} instances. Given a {@link StorageConnector} input,
 * this class tries to instantiate a {@code GeoTiffStore}.
 *
 * <div class="section">Thread safety</div>
 * The same {@code GeoTiffStoreProvider} instance can be safely used by many threads without synchronization on
 * the part of the caller. However the {@link GeoTiffStore} instances created by this factory are not thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @see GeoTiffStore
 *
 * @since 0.8
 * @module
 */
@Capabilities(Capability.READ)
public class GeoTiffStoreProvider extends DataStoreProvider {
    /**
     * The MIME type for GeoTIFF files.
     */
    private static final String MIME_TYPE = "image/tiff";

    /**
     * The TIFF version.
     */
    private static final Version VERSION = new Version("6.0");

    /**
     * The parameter descriptor to be returned by {@link #getOpenParameters()}.
     */
    private static final ParameterDescriptorGroup OPEN_DESCRIPTOR = URIDataStore.Provider.descriptor(Constants.GEOTIFF);

    /**
     * Creates a new provider.
     */
    public GeoTiffStoreProvider() {
    }

    /**
     * Returns a generic name for this data store, used mostly in warnings or error messages.
     *
     * @return a short name or abbreviation for the data format.
     */
    @Override
    public String getShortName() {
        return Constants.GEOTIFF;
    }

    /**
     * Returns a description of all parameters accepted by this provider for opening a GeoTIFF file.
     *
     * @return description of available parameters for opening a GeoTIFF file.
     *
     * @since 0.8
     */
    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return OPEN_DESCRIPTOR;
    }

    /**
     * Returns {@link ProbeResult#SUPPORTED} if the given storage appears to be supported by {@link GeoTiffStore}.
     * Returning {@code SUPPORTED} from this method does not guarantee that reading or writing will succeed,
     * only that there appears to be a reasonable chance of success based on a brief inspection of the
     * {@linkplain StorageConnector#getStorage() storage object} or contents.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @return {@code SUPPORTED} if the given storage seems to be usable by {@code GeoTiffStore} instances.
     * @throws DataStoreException if an I/O error occurred.
     */
    @Override
    public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
        final ByteBuffer buffer = connector.getStorageAs(ByteBuffer.class);
        if (buffer != null) {
            if (buffer.remaining() < 2 * Short.BYTES) {
                return ProbeResult.INSUFFICIENT_BYTES;
            }
            final int p = buffer.position();
            final short order = buffer.getShort(p);
            final boolean isBigEndian = (order == GeoTIFF.BIG_ENDIAN);
            if (isBigEndian || order == GeoTIFF.LITTLE_ENDIAN) {
                final ByteOrder old = buffer.order();
                try {
                    buffer.order(isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
                    switch (buffer.getShort(p + Short.BYTES)) {
                        case GeoTIFF.CLASSIC:
                        case GeoTIFF.BIG_TIFF: return new ProbeResult(true, MIME_TYPE, VERSION);
                    }
                } finally {
                    buffer.order(old);
                }
            }
        }
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

    /**
     * Returns a {@link GeoTiffStore} implementation associated with this provider.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @return a data store implementation associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final StorageConnector connector) throws DataStoreException {
        return new GeoTiffStore(this, connector);
    }
}
