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
package org.apache.sis.storage.geopackage;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.StoreMetadata;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Geopackage datastore.
 * Specification : <a href="https://www.opengeospatial.org/standards/geopackage">https://www.opengeospatial.org/standards/geopackage</a>
 * version : 1.2.1
 *
 * @author Johann Sorel (Geomatys)
 */
@StoreMetadata(
        formatName = GpkgProvider.NAME,
        capabilities = {Capability.READ,Capability.WRITE},
        fileSuffixes = {"gpkg"},
        resourceTypes = {FeatureSet.class, GridCoverageResource.class})
public final class GpkgProvider extends DataStoreProvider {

    /**
     * Provider identifier.
     */
    public static final String NAME = "gpkg";
    /**
     * Custom pragma to enforce use of an Hikari connection pool.
     */
    public static final String PRAGMA_HIKARICP = "HIKARICP";

    /**
     * URI to the dafift folder.
     */
    public static final ParameterDescriptor<URI> PATH = new ParameterBuilder()
            .addName(LOCATION)
            .setRequired(true)
            .create(URI.class, null);
    /**
     * Pragma parameters encoded in the form 'name=value;name=value'
     * List of pragma can be found at : <a href="https://www.sqlite.org/pragma.html">https://www.sqlite.org/pragma.html</a>
     */
    public static final ParameterDescriptor<String> PRAGMAS = new ParameterBuilder()
            .addName("pragmas")
            .setDescription("Pragma parameters encoded in the form 'name=value;name=value'")
            .setRemarks("List of pragamas can be found at : https://www.sqlite.org/pragma.html")
            .setRequired(false)
            .create(String.class, null);
    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR = new ParameterBuilder()
            .addName(NAME)
            .createGroup(PATH, PRAGMAS);


    private static GpkgProvider INSTANCE;

    /**
     * Get singleton instance of Geopackage provider.
     *
     * <p>
     * Note : this method is named after Java 9 service loader provider method.
     * {@link https://docs.oracle.com/javase/9/docs/api/java/util/ServiceLoader.html}
     * </p>
     *
     * @return singleton instance of GpkgProvider
     */
    public static synchronized GpkgProvider provider() {
        if (INSTANCE == null) INSTANCE = new GpkgProvider();
        return INSTANCE;
    }

    public GpkgProvider() {
    }

    @Override
    public String getShortName() {
        return NAME;
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS_DESCRIPTOR;
    }

    @Override
    public DataStore open(ParameterValueGroup parameters) throws DataStoreException {
        return new GpkgStore(parameters);
    }

    @Override
    public DataStore open(StorageConnector connector) throws DataStoreException {
        final Path path = connector.getStorageAs(Path.class);
        connector.closeAllExcept(path);
        return new GpkgStore(path, Collections.EMPTY_MAP);
    }

    @Override
    public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
        Path path = connector.getStorageAs(Path.class);
        if (path == null || Files.isDirectory(path)) return ProbeResult.UNSUPPORTED_STORAGE;

        //check file extension
        final boolean validExt = path.getFileName().toString().toLowerCase().endsWith("gpkg");
        if (!validExt) {
            return ProbeResult.UNSUPPORTED_STORAGE;
        }

        //check signature and application id
        final ByteBuffer buffer = connector.getStorageAs(ByteBuffer.class);
        if (buffer != null) {
            try {
                buffer.mark();
                if (buffer.remaining() >= Gpkg.SIGNATURE.length) {
                    final byte[] candidate = new byte[Gpkg.SIGNATURE.length];
                    buffer.get(candidate);

                    //compare signatures
                    if (Arrays.equals(Gpkg.SIGNATURE, candidate)) {
                        return new ProbeResult(true, Gpkg.MIME_TYPE, null);
                    }
                }

                buffer.rewind();
                //check application id
                buffer.position(68);
                int appId = buffer.getInt();
                if (appId != 0x47504B47) { //"GPKG" in ASCII
                    return ProbeResult.UNSUPPORTED_STORAGE;
                }

            } finally {
                buffer.rewind();
            }
        }

        return ProbeResult.UNSUPPORTED_STORAGE;
    }

}
