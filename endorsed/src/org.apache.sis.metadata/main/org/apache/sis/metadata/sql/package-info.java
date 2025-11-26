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
 * An implementation of {@linkplain org.opengis.metadata.Metadata Metadata} interfaces
 * fetching the data from an SQL database. Each metadata class is mapped to a table,
 * and each metadata property is mapped to a column in the table corresponding to the class.
 * Tables and columns are created only when first needed.
 *
 * <p>This package is not a replacement for more sophisticated metadata applications.
 * This package provides only a direct mapping from metadata <i>interfaces</i> and <i>methods</i>
 * to database <i>tables</i> and <i>columns</i> with limited capability.
 * This is suitable only for applications wanting a simple metadata schema.
 * The restrictions are:</p>
 *
 * <ul>
 *   <li>Interfaces and methods must have {@link org.opengis.annotation.UML} annotations.</li>
 *   <li>Collections are not currently supported (only the first element is stored).</li>
 *   <li>{@link org.opengis.util.InternationalString} are stored only for the default locale.</li>
 *   <li>Cyclic graph (<var>A</var> references <var>B</var> which reference <var>A</var>) are not supported,
 *       unless foreigner key constraints are manually disabled for the columns which contain the cyclic references.</li>
 * </ul>
 *
 * If the database supports <i>table inheritance</i> (like <a href="http://www.postgresql.org">PostgreSQL</a>),
 * then this package will leverage that feature for the storage of metadata that are sub-interface of other metadata
 * (for example {@link org.opengis.metadata.extent.GeographicDescription} which extends
 * {@link org.opengis.metadata.extent.GeographicExtent}).
 *
 * @author  Touraïvane (IRD)
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.6
 *
 * @see org.apache.sis.referencing.factory.sql
 *
 * @since 0.8
 */
package org.apache.sis.metadata.sql;
