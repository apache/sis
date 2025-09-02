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

/**
 * Raster imagery and geometry features.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   0.3
 */
module org.apache.sis.storage {
    requires java.sql;
    requires java.net.http;
    requires jakarta.xml.bind;
    requires transitive org.apache.sis.feature;

    provides java.nio.file.spi.FileTypeDetector
        with org.apache.sis.storage.services.StoreTypeDetector;

    uses     org.apache.sis.storage.DataStoreProvider;
    provides org.apache.sis.storage.DataStoreProvider
        with org.apache.sis.storage.image.WorldFileStoreProvider,
             org.apache.sis.storage.esri.AsciiGridStoreProvider,
             org.apache.sis.storage.esri.RawRasterStoreProvider,
             org.apache.sis.storage.csv.StoreProvider,
             org.apache.sis.storage.xml.StoreProvider,
             org.apache.sis.storage.wkt.StoreProvider,
             org.apache.sis.storage.folder.StoreProvider;

    exports org.apache.sis.storage;
    exports org.apache.sis.storage.event;
    exports org.apache.sis.storage.tiling;
    exports org.apache.sis.storage.aggregate;
    exports org.apache.sis.storage.modifier;

    exports org.apache.sis.storage.base to
            org.apache.sis.storage.xml,
            org.apache.sis.storage.sql,
            org.apache.sis.storage.netcdf,
            org.apache.sis.storage.geotiff,
            org.apache.sis.storage.earthobservation,
            org.apache.sis.storage.gdal,                // In the "optional" sub-project.
            org.apache.sis.util,                        // For the "About" command.
            org.apache.sis.console,
            org.apache.sis.openoffice,
            org.apache.sis.gui;                         // In the "optional" sub-project.

    exports org.apache.sis.io.stream to
            org.apache.sis.storage.xml,
            org.apache.sis.storage.sql,
            org.apache.sis.storage.netcdf,
            org.apache.sis.storage.geotiff,
            org.apache.sis.storage.gdal,                // In the "optional" sub-project.
            org.apache.sis.cloud.aws,
            org.apache.sis.console,
            org.apache.sis.gui;                         // In the "optional" sub-project.

    exports org.apache.sis.storage.xml to
            org.apache.sis.storage.xml,
            org.apache.sis.gui;                         // In the "optional" sub-project.

    exports org.apache.sis.storage.wkt to
            org.apache.sis.storage.netcdf,
            org.apache.sis.storage.earthobservation,
            org.apache.sis.gui;                         // In the "optional" sub-project.

    exports org.apache.sis.storage.folder to
            org.apache.sis.storage.earthobservation,
            org.apache.sis.gui;                         // In the "optional" sub-project.
}
