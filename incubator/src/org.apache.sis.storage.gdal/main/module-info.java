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
 * Bridge to the <abbr>GDAL</abbr> library for reading rasters.
 * This module assumes that <abbr>GDAL</abbr> 3.0 or later is preinstalled.
 * The <abbr>GDAL</abbr> C/C++ functions are invoked by using the {@link java.lang.foreign} package.
 * <em>Accesses to native functions are restricted by default in Java and requires user's authorization.</em>
 * This authorization can be granted by the following option on the command-line:
 *
 * {@snippet lang="java" :
 *     java --enable-native-access org.apache.sis.storage.gdal <other options>
 *     }
 *
 * <h2>When to use</h2>
 * <abbr>GDAL</abbr> supports a wider range of formats and compression methods than Apache <abbr>SIS</abbr>.
 * Therefore, <abbr>GDAL</abbr> is a useful fallback when a format is not supported directly by <abbr>SIS</abbr>.
 * However, <abbr>GDAL</abbr> is not necessarily faster or more capable than pure Java implementations.
 * When a pure Java implementation exists, we recommend to test it before to fallback on native code.
 * The performances of Apache <abbr>SIS</abbr> data stores are often comparable to <abbr>GDAL</abbr>,
 * and the <abbr>SIS</abbr> modules dedicated to a format (GeoTIFF, netCDF, Earth Observation, …)
 * have more extensive support than this module for some features such as <abbr>ISO</abbr> 19115 metadata
 * and non-linear transforms.
 *
 * <h2>Memory consumption</h2>
 * <abbr>GDAL</abbr> and Apache <abbr>SIS</abbr> both have their own cache mechanism.
 * For reducing the amount of memory consumed by the same data cached twice,
 * it may be useful to set the {@code GDAL_CACHEMAX} environment variable to a lower value.
 * The value can be specified in megabytes or as a percentage (e.g. "5%").
 *
 * @author  Quentin Bialota (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
module org.apache.sis.storage.gdal {
    // Dependencies used in public API.
    requires transitive org.apache.sis.referencing;
    requires transitive org.apache.sis.storage;

    exports org.apache.sis.storage.gdal;

    exports org.apache.sis.storage.panama to
            org.apache.sis.storage.gsf;                 // In the "incubator" sub-project.

    provides org.apache.sis.storage.DataStoreProvider
            with org.apache.sis.storage.gdal.GDALStoreProvider;
}
