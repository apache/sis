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
 * Each relation defined by a foreigner key is represented by an {@link org.opengis.feature.FeatureAssociationRole}
 * to another feature (with transitive dependencies automatically resolved), and the other columns are represented
 * by {@link org.opengis.feature.AttributeType}.
 *
 * <div class="section">Limitations</div>
 * <ul>
 *   <li>Current implementation does not yet map geometric objects (e.g. PostGIS types).</li>
 *   <li>If a parent feature contains association to other features, those other features are created
 *       in same time than the parent feature (no lazy instantiation yet).</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
package org.apache.sis.storage.sql;
