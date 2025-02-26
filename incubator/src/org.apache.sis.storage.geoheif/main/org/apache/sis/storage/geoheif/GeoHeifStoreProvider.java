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

import java.net.URI;
import java.nio.file.Path;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.GridCoverageResource;
import static org.apache.sis.storage.DataStoreProvider.LOCATION;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.StoreMetadata;
import org.apache.sis.storage.tiling.TiledResource;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
@StoreMetadata(formatName    = GeoHeifStoreProvider.NAME,
               capabilities  = {Capability.READ},
               fileSuffixes  = {"heij", "heif", "heic", "avif"},
               resourceTypes = {Aggregate.class, GridCoverageResource.class, TiledResource.class},
               yieldPriority = false)
public final class GeoHeifStoreProvider extends DataStoreProvider {

    /**
     * Format name.
     */
    public static final String NAME = "GIMI";

    /**
     * Format mime type.
     */
    public static final String MIME_TYPE = "application/x-gimi";

    /**
     * URI to the gimi file.
     */
    public static final ParameterDescriptor<URI> PATH = new ParameterBuilder()
            .addName(LOCATION)
            .setRequired(true)
            .create(URI.class, null);

    /**
     * Shapefile store creation parameters.
     */
    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR =
            new ParameterBuilder().addName(NAME).addName("GimiParameters").createGroup(
                PATH);

    /**
     * Default constructor.
     */
    public GeoHeifStoreProvider() {
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String getShortName() {
        return NAME;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS_DESCRIPTOR;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
        final Path path = connector.getStorageAs(Path.class);
        if (path != null) {
            final String name = path.getFileName().toString().toLowerCase();
            if (name.endsWith(".heij") || name.endsWith(".heif") || name.endsWith(".heic") || name.endsWith(".avif")) {
                return new ProbeResult(true, MIME_TYPE, null);
            }
        }
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public DataStore open(StorageConnector connector) throws DataStoreException {
        return new GeoHeifStore(connector);
    }

}
