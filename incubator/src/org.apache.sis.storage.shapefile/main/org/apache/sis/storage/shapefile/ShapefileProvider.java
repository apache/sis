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
package org.apache.sis.storage.shapefile;

import java.nio.file.Path;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.base.URIDataStoreOption;
import org.apache.sis.storage.base.URIDataStoreProvider;


/**
 * Shapefile format datastore provider.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="http://www.esri.com/library/whitepapers/pdfs/shapefile.pdf">ESRI Shapefile Specification</a>
 */
public final class ShapefileProvider extends URIDataStoreProvider {
    /**
     * Format name.
     */
    public static final String NAME = "esri shapefile";

    /**
     * Format mime type.
     */
    public static final String MIME_TYPE = "application/x-shapefile";

    private static ShapefileProvider INSTANCE;

    /**
     * Get singleton instance of shapefile provider.
     *
     * <p>
     * Note : this method is named after Java 9 service loader provider method.
     * {@link https://docs.oracle.com/javase/9/docs/api/java/util/ServiceLoader.html}
     * </p>
     *
     * @return singleton instance of ShapefileProvider
     */
    public static synchronized ShapefileProvider provider() {
        if (INSTANCE == null) INSTANCE = new ShapefileProvider();
        return INSTANCE;
    }

    /**
     * Default constructor.
     */
    public ShapefileProvider() {
        supportedOptions.add(URIDataStoreOption.TIMEZONE);
        supportedOptions.add(URIDataStoreOption.ENCODING);
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
    public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
        final Path path = connector.getStorageAs(Path.class);
        if (path != null && path.getFileName().toString().toLowerCase().endsWith(".shp")) {
            return new ProbeResult(true, MIME_TYPE, null);
        }
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public DataStore open(StorageConnector connector) throws DataStoreException {
        return new ShapefileStore(this, connector);
    }
}
