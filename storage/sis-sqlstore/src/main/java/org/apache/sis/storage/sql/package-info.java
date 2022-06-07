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
 * Data store capable to read and create features from a JDBC connection to a database.
 * {@link org.apache.sis.storage.sql.SQLStore} takes a one or more tables at construction time.
 * Each enumerated table is represented by a {@link org.opengis.feature.FeatureType}.
 * Each row in those table represents a {@link org.opengis.feature.Feature} instance.
 * Each relation defined by a foreigner key is represented by an {@link org.opengis.feature.FeatureAssociationRole}
 * to another feature (with transitive dependencies automatically resolved), and the other columns are represented
 * by {@link org.opengis.feature.AttributeType}.
 *
 * <p>The storage of spatial features in SQL databases is described by the
 * <a href="https://www.ogc.org/standards/sfs">OGC Simple feature access - Part 2: SQL option</a>
 * international standard, also known as ISO 19125-2. Implementation of geometric types and operations must
 * be provided by the database (sometime through an extension, for example PostGIS on PostgreSQL databases).
 * This Java package uses those provided types and operations.</p>
 *
 * <h2>Performance tips</h2>
 * <p>A subset of features can be obtained by applying filters on the stream returned by
 * {@link org.apache.sis.storage.FeatureSet#features(boolean)}.
 * While the filter can be any {@link java.util.function.Predicate},
 * performances will be much better if they are instances of {@link org.opengis.filter.Filter}
 * because Apache SIS will know how to translate some of them to SQL statements.</p>
 *
 * <p>In filter expressions like {@code ST_Intersects(A,B)} where the <var>A</var> and <var>B</var> parameters are
 * two sub-expressions evaluating to geometry values, if one of those expressions is a literal, then that literal
 * should be <var>B</var>. The reason is because the SQLMM standard requires us to project <var>B</var> in the
 * Coordinate Reference System of <var>A</var>. If <var>B</var> is a literal, Apache SIS can do this transformation
 * only once before to start the filtering process instead of every time that the filter needs to be evaluated.</p>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Current implementation does not scan the {@code "GEOMETRY_COLUMNS"} (from Simple Feature Access)
 *       or {@code "gpkg_content"} (from GeoPackage) tables for a default list of feature tables.</li>
 *   <li>Current implementation does not yet map geometric objects (e.g. PostGIS types).</li>
 *   <li>If a parent feature contains association to other features, those other features are created
 *       at the same time than the parent feature (no lazy instantiation yet).</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.3
 * @since   1.0
 * @module
 */
package org.apache.sis.storage.sql;
