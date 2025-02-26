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
import java.util.logging.Logger;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.util.Version;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.base.StoreMetadata;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.URIDataStoreProvider;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;


/**
 * The provider of {@code GeoTiffStore} instances.
 * Given a {@link StorageConnector} input, this class tries to instantiate a {@link GeoTiffStore}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see GeoTiffStore
 *
 * @since 0.8
 */
@StoreMetadata(formatName    = Constants.GEOTIFF,
               fileSuffixes  = {"tiff", "tif"},
               capabilities  = {Capability.READ, Capability.WRITE, Capability.CREATE},
               resourceTypes = {Aggregate.class, GridCoverageResource.class})
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
     * The logger used by GeoTIFF stores.
     *
     * @see #getLogger()
     */
    private static final Logger LOGGER = Logger.getLogger("org.apache.sis.storage.geotiff");

    /**
     * Name of the parameter for specifying the format modifiers (BigTIFF, COG…).
     */
    static final String MODIFIERS = "modifiers";

    /**
     * Name of the parameter for specifying the compression.
     */
    static final String COMPRESSION = "compression";

    /**
     * The parameter descriptor for {@link #MODIFIERS}.
     */
    private static final ParameterDescriptor<FormatModifier[]> MODIFIERS_PARAM;

    /**
     * The parameter descriptor for {@link #COMPRESSION}.
     */
    private static final ParameterDescriptor<Compression> COMPRESSION_PARAM;

    /**
     * The parameter descriptor to be returned by {@link #getOpenParameters()}.
     */
    private static final ParameterDescriptorGroup OPEN_DESCRIPTOR;
    static {
        final var builder = new ParameterBuilder();
        MODIFIERS_PARAM   = builder.addName(MODIFIERS).setDescription(Vocabulary.formatInternational(Vocabulary.Keys.Options)).create(FormatModifier[].class, null);
        COMPRESSION_PARAM = builder.addName(COMPRESSION).setDescription(Vocabulary.formatInternational(Vocabulary.Keys.Compression)).create(Compression.class, null);
        OPEN_DESCRIPTOR   = builder.addName(Constants.GEOTIFF).createGroup(URIDataStoreProvider.LOCATION_PARAM, MODIFIERS_PARAM, COMPRESSION_PARAM);
    }

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
     * Returns the MIME type if the given storage appears to be supported by {@code GeoTiffStore}.
     * A {@linkplain ProbeResult#isSupported() supported} status does not guarantee that reading
     * or writing will succeed, only that there appears to be a reasonable chance of success
     * based on a brief inspection of the file header.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @return a {@linkplain ProbeResult#isSupported() supported} status with the MIME type
     *         if the given storage seems to be readable by {@link GeoTiffStore} instances.
     * @throws DataStoreException if an I/O error occurred.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
        return probeContent(connector, ByteBuffer.class, (buffer) -> {
            if (buffer.remaining() < 2 * Short.BYTES) {
                return ProbeResult.INSUFFICIENT_BYTES;
            }
            switch (buffer.getShort()) {
                case IOBase.LITTLE_ENDIAN: {
                    buffer.order(ByteOrder.LITTLE_ENDIAN);
                    // Fall through
                }
                case IOBase.BIG_ENDIAN: {   // Default buffer order is big endian.
                    switch (buffer.getShort()) {
                        case IOBase.CLASSIC:
                        case IOBase.BIG_TIFF: {
                            return new ProbeResult(true, MIME_TYPE, VERSION);
                        }
                    }
                }
            }
            return ProbeResult.UNSUPPORTED_STORAGE;
        });
    }

    /**
     * Creates a {@code GeoTiffStore} instance associated with this provider.
     *
     * @param  connector  information about the storage (URL, stream, <i>etc</i>).
     * @return a data store instance associated with this provider for the given storage.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     */
    @Override
    public DataStore open(final StorageConnector connector) throws DataStoreException {
        if (URIDataStoreProvider.isWritable(connector, false)) {
            return new WritableStore(this, connector);
        }
        return new GeoTiffStore(this, connector);
    }

    /**
     * Creates a {@code GeoTiffStore} instance from the given parameters.
     *
     * @param  parameters  opening parameters as defined by {@link #getOpenParameters()}.
     * @return a data store instance associated with this provider for the given parameters.
     * @throws DataStoreException if an error occurred while creating the data store instance.
     *
     * @since 1.5
     */
    @Override
    public DataStore open(final ParameterValueGroup parameters) throws DataStoreException {
        final var p = Parameters.castOrWrap(parameters);
        final var connector = new StorageConnector(p.getValue(URIDataStoreProvider.LOCATION_PARAM));
        final FormatModifier[] modifiers = p.getValue(MODIFIERS_PARAM);
        if (modifiers != null) {
            connector.setOption(FormatModifier.OPTION_KEY, modifiers);
        }
        final Compression compression = p.getValue(COMPRESSION_PARAM);
        if (compression != null) {
            connector.setOption(Compression.OPTION_KEY, compression);
        }
        return open(connector);
    }

    /**
     * Returns the logger used by GeoTIFF stores.
     */
    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
