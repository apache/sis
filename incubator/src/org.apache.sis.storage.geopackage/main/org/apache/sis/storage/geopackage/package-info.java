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
 * While the OGC Geopackage specification mandate the use of SQLite as the database software,
 * the Apache <abbr>SIS</abbr> implementation of this data store accepts any <abbr>JDBC</abbr>
 * {@link javax.sql.DataSource} providing connections to a database having the same tables or
 * views as defined by the Geopackage standard.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see <a href="https://www.opengeospatial.org/standards/geopackage">OGC® GeoPackage Encoding Standard</a>
 *
 * @since 1.5
 */
package org.apache.sis.storage.geopackage;
