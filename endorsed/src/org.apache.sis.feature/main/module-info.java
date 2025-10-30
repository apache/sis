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
 * @since   0.5
 */
module org.apache.sis.feature {
    requires java.sql;
    requires transitive org.apache.sis.referencing;

    // Optional dependencies to be provided by user.
    requires static esri.geometry.api;
    requires static org.locationtech.jts;

    uses org.apache.sis.filter.FunctionRegister;

    provides org.apache.sis.filter.FunctionRegister
        with org.apache.sis.filter.math.Registry;

    exports org.apache.sis.image;
    exports org.apache.sis.coverage;
    exports org.apache.sis.coverage.grid;
    exports org.apache.sis.feature;
    exports org.apache.sis.feature.builder;
    exports org.apache.sis.filter;
    exports org.apache.sis.index.tree;

    exports org.apache.sis.filter.base to
            org.apache.sis.storage,
            org.apache.sis.storage.sql,
            org.apache.sis.storage.shapefile,       // In the "incubator" sub-project.
            org.apache.sis.portrayal.map;           // In the "incubator" sub-project.

    exports org.apache.sis.filter.visitor to
            org.apache.sis.storage,
            org.apache.sis.storage.sql,
            org.apache.sis.storage.shapefile,       // In the "incubator" sub-project.
            org.apache.sis.cql;                     // In the "incubator" sub-project.

    exports org.apache.sis.filter.sqlmm to
            org.apache.sis.geometry;                // In the "incubator" sub-project.

    exports org.apache.sis.feature.internal.shared to
            org.apache.sis.storage,
            org.apache.sis.storage.xml,
            org.apache.sis.storage.sql,
            org.apache.sis.storage.netcdf,
            org.apache.sis.storage.shapefile,       // In the "incubator" sub-project.
            org.apache.sis.storage.gdal,            // In the "optional" sub-project.
            org.apache.sis.portrayal,
            org.apache.sis.portrayal.map,           // In the "incubator" sub-project.
            org.apache.sis.gui;                     // In the "optional" sub-project.

    exports org.apache.sis.geometry.wrapper to
            org.apache.sis.geometry,                // In the "incubator" sub-project.
            org.apache.sis.storage,
            org.apache.sis.storage.xml,
            org.apache.sis.storage.sql,
            org.apache.sis.storage.netcdf,
            org.apache.sis.storage.gdal,            // In the "optional" sub-project.
            org.apache.sis.storage.shapefile,       // In the "incubator" sub-project.
            org.apache.sis.portrayal.map,           // In the "incubator" sub-project.
            org.apache.sis.cql;                     // In the "incubator" sub-project.

    exports org.apache.sis.geometry.wrapper.j2d to
            org.apache.sis.gui;                     // In the "optional" sub-project.

    exports org.apache.sis.geometry.wrapper.jts to
            org.apache.sis.geometry,                // In the "incubator" sub-project.
            org.apache.sis.portrayal.map,           // In the "incubator" sub-project.
            org.apache.sis.cql;                     // In the "incubator" sub-project.

    exports org.apache.sis.coverage.internal.shared to
            org.apache.sis.storage,
            org.apache.sis.storage.netcdf,
            org.apache.sis.portrayal;

    exports org.apache.sis.image.internal.shared to
            org.apache.sis.storage,
            org.apache.sis.storage.sql,
            org.apache.sis.storage.netcdf,
            org.apache.sis.storage.geotiff,
            org.apache.sis.storage.geoheif,             // In the "incubator" sub-project.
            org.apache.sis.storage.gdal,                // In the "optional" sub-project.
            org.apache.sis.portrayal,
            org.apache.sis.gui;                         // In the "optional" sub-project.

    exports org.apache.sis.image.processing.isoline to
            org.apache.sis.gui;                         // In the "optional" sub-project.
}
