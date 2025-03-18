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
 * Data store for features in a <abbr>SQL</abbr> spatial database.
 * This module expects a spatial schema conforms to the conventions described in the
 * <a href="https://www.ogc.org/standards/sfs">OGC Simple feature access - Part 2: SQL option</a>
 * international standard, also known as <abbr>ISO</abbr> 19125-2.
 *
 * <h2>Difference with Geopackage</h2>
 * Compared to the <abbr>OGC</abbr> Geopackage standard,
 * this <abbr>SQL</abbr> module has the following differences:
 *
 * <ul>
 *   <li>There is no discovery mechanism (e.g., no {@code "gpkg_contents"} table).
 *       The tables to use as {@linkplain org.apache.sis.storage.sql.ResourceDefinition resource definitions}
 *       must be specified explicitly.</li>
 *   <li>Each feature table can contain an arbitrary number of geometry columns, including zero.
 *       By contrast, Geopackage requires each feature table to have exactly one geometry column.</li>
 *   <li>As a consequence of the above, this module makes no distinction between "features" table and "attributes" table.</li>
 *   <li>This module supports <dfn>complex features</dfn>, i.e. features having associations to other features.
 *       The associations are discovered automatically by following the foreigner keys.</li>
 *   <li>Primary keys are optional. If present, they can be of any type (not necessarily integers) and can be composite
 *       (made of many columns). By contrast, Geopackage mandates primary keys made of exactly one column of integers.</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @version 1.5
 * @since   1.0
 */
module org.apache.sis.storage.sql {
    requires java.sql;
    requires transitive org.apache.sis.storage;
    requires static org.postgresql.jdbc;

    exports org.apache.sis.storage.sql;
}
