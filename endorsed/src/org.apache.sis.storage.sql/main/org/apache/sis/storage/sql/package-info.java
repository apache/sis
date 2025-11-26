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
 * Data store capable to read and write features using a JDBC connection to a database.
 * {@link org.apache.sis.storage.sql.SimpleFeatureStore} takes one or more tables at construction time.
 * Each enumerated table is represented by a {@code FeatureType}.
 * Each row in those table represents a {@code Feature} instance.
 * Each relation defined by a foreigner key is represented by an {@code FeatureAssociationRole}
 * to another feature (with transitive dependencies automatically resolved), and the other columns are represented
 * by {@code AttributeType}.
 *
 * <p>The storage of spatial features in <abbr>SQL</abbr> databases is described by the
 * <a href="https://www.ogc.org/standards/sfs">OGC Simple feature access - Part 2: SQL option</a>
 * international standard, also known as ISO 19125-2.
 * The implementation of geometric objects and their operations must be provided by the database.
 * This is sometimes provided by an extension that needs to be installed explicitly.
 * For example, when using PostgreSQL, the PostGIS extension is recommended.</p>
 *
 * <p>The tables to use as {@linkplain org.apache.sis.storage.sql.ResourceDefinition resource definitions}
 * must be specified at construction time. There is no automatic discovery mechanism. Note that discovery
 * may be done by other modules. For example, Geopackage module uses the {@code "gpkg_contents"} table.</p>
 *
 * <h2>Performance tips</h2>
 * <p>A subset of features can be obtained by applying filters on the stream returned by
 * {@link org.apache.sis.storage.FeatureSet#features(boolean)}.
 * While the filter can be any {@link java.util.function.Predicate},
 * performances will be much better if they are instances of {@link org.apache.sis.filter.Filter}
 * because Apache SIS will know how to translate some of them to <abbr>SQL</abbr> statements.</p>
 *
 * <p>In filter expressions like {@code ST_Intersects(A,B)} where the <var>A</var> and <var>B</var> parameters are
 * two sub-expressions evaluating to geometry values, if one of those expressions is a literal, then that literal
 * should be <var>B</var>. The reason is because the SQLMM standard requires us to project <var>B</var> in the
 * Coordinate Reference System of <var>A</var>. If <var>B</var> is a literal, Apache SIS can do this transformation
 * only once before to start the filtering process instead of every time that the filter needs to be evaluated.</p>
 *
 * <p><b>Limitation:</b> if a parent feature contains association to other features (defined by foreigner keys),
 * those other features are created at the same time as the parent feature. There is no lazy instantiation yet.
 * Performances should be okay if each parent feature references only a small amount of children.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.6
 * @since   1.0
 */
package org.apache.sis.storage.sql;
