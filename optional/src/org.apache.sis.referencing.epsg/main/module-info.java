/*
 * Copyright 2023 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * SQL scripts for EPSG geodetic data set installation.
 * This module provides the <abbr>EPSG</abbr> data of the {@code org.apache.sis.referencing.database} module,
 * but in a form that allows installation on <abbr>SQL</abbr> databases other than Apache Derby.
 *
 * <h2>Licensing</h2>
 * EPSG is maintained by the <a href="https://www.iogp.org/">International Association of Oil and Gas Producers</a>
 * (IOGP) Surveying &amp; Positioning Committee and is subject to <a href="https://epsg.org/terms-of-use.html">EPSG
 * terms of use</a>. This module is not included in the Apache <abbr>SIS</abbr> distribution of convenience binaries,
 * and the source code contains only the Java classes without the <abbr>EPSG</abbr> data. For use in an application,
 * see <a href="https://sis.apache.org/epsg.html">How to use EPSG geodetic dataset</a> on the <abbr>SIS</abbr> web site.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   0.7
 */
module org.apache.sis.referencing.epsg {
    requires transitive org.apache.sis.referencing;

    exports org.apache.sis.referencing.factory.sql.epsg;

    uses     org.apache.sis.setup.InstallationResources;
    provides org.apache.sis.setup.InstallationResources
        with org.apache.sis.referencing.factory.sql.epsg.ScriptProvider;
}
