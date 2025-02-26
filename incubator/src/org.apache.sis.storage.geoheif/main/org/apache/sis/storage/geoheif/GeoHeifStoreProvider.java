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
package org.apache.sis.storage.geoheif;

import java.nio.ByteBuffer;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.StoreMetadata;
import org.apache.sis.storage.base.URIDataStoreProvider;
import org.apache.sis.storage.tiling.TiledResource;
import org.apache.sis.storage.isobmff.base.FileType;


/**
 * The provider of {@code GeoHeifStore} instances.
 * Given a {@link StorageConnector} input, this class tries to instantiate a {@link GeoHeifStore}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
@StoreMetadata(formatName    = GeoHeifStoreProvider.NAME,
               fileSuffixes  = {"heif", "heij", "heic", "avif"},
               capabilities  = {Capability.READ},
               resourceTypes = {Aggregate.class, GridCoverageResource.class, TiledResource.class})
public class GeoHeifStoreProvider extends DataStoreProvider {
    /**
     * Format name.
     */
    static final String NAME = "GeoHEIF";

    /**
     * The MIME type for GeoHEIF files.
     */
    private static final String MIME_TYPE = "image/heif";

    /**
     * The parameter descriptor to be returned by {@link #getOpenParameters()}.
     */
    private static final ParameterDescriptorGroup OPEN_DESCRIPTOR;
    static {
        final var builder = new ParameterBuilder();
        OPEN_DESCRIPTOR   = builder.addName(NAME).createGroup(URIDataStoreProvider.LOCATION_PARAM);
    }

    /**
     * Creates a new provider.
     */
    public GeoHeifStoreProvider() {
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
     * Returns a description of all parameters accepted by this provider for opening a GeoHEIF file.
     *
     * @return description of available parameters for opening a GeoHEIF file.
     */
    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return OPEN_DESCRIPTOR;
    }

    /**
     * Returns the MIME type if the given storage appears to be supported by {@code GeoHeifStore}.
     * A {@linkplain ProbeResult#isSupported() supported} status does not guarantee that reading
     * or writing will succeed, only that there appears to be a reasonable chance of success
     * based on a brief inspection of the file header.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @return a {@linkplain ProbeResult#isSupported() supported} status with the MIME type
     *         if the given storage seems to be readable by {@link GeoHeifStore} instances.
     * @throws DataStoreException if an I/O error occurred.
     */
    @Override
    public ProbeResult probeContent(final StorageConnector connector) throws DataStoreException {
        return probeContent(connector, ByteBuffer.class, (buffer) -> {
            if (buffer.remaining() < 2 * Integer.BYTES) {
                return ProbeResult.INSUFFICIENT_BYTES;
            }
            // Default buffer order is big endian.
            final int size = buffer.getInt();
            if (size == 0 || size == 1 || size >= 4*Integer.BYTES) {
                switch (buffer.getInt()) {      // Box identifier.
                    case FileType.BOXTYPE: {
                        return new ProbeResult(true, MIME_TYPE, null);
                    }
                }
            }
            return ProbeResult.UNSUPPORTED_STORAGE;
        });
    }

    /**
     * Creates a {@code GeoHeifStore} instance associated with this provider.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @return a data store instance associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final StorageConnector connector) throws DataStoreException {
        return new GeoHeifStore(this, connector);
    }
}
