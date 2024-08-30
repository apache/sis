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
 * A data store for reading and writing Geopackage files.
 * This module is based on the {@link org.apache.sis.storage.sql} module,
 * which is extended for supporting the {@code "gpkg_contents"} table.
 *
 * <h2>Generalization</h2>
 * At reading time, this module is more flexible than required by the Geopackage standard:
 *
 * <ul>
 *   <li>The database can be any <abbr>JDBC</abbr> compliant database, not necessarily SQLite.</li>
 *   <li>Feature and attribute tables can have any number of geometry columns, including zero.
 *       By contrast, the Geopackage standard restricts feature tables to one geometry column,
 *       and attribute tables to zero geometry column.</li>
 *   <li><dfn>Complex features</dfn> (i.e., features having associations to other features) are supported.
 *       The associations are discovered automatically by following the foreigner keys.</li>
 *   <li>Primary keys are optional, can be composite (made of many columns) and are not restricted to integer type.</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see <a href="https://www.opengeospatial.org/standards/geopackage">OGC® GeoPackage Encoding Standard</a>
 *
 * @since 1.5
 */
module org.apache.sis.storage.geopackage {
    requires transitive org.apache.sis.referencing;
    requires transitive org.apache.sis.feature;
    requires transitive org.apache.sis.storage;
    requires transitive org.apache.sis.storage.sql;
    requires org.xerial.sqlitejdbc;

    exports org.apache.sis.storage.geopackage;

    uses org.apache.sis.storage.geopackage.ContentHandler;

    provides org.apache.sis.storage.DataStoreProvider
            with org.apache.sis.storage.geopackage.GpkgStoreProvider;
}
