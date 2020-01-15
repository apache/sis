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
 * Filters features according their properties.
 * A <cite>filter expression</cite> is a construct used to constraint a feature set to a subset.
 *
 * All operations in this package try to follow rules of both following standards:
 * <ul>
 *     <li><a href="https://www.iso.org/fr/standard/53698.html">ISO/IEC 13249-3:2011 - SQLMM</a></li>
 *     <li><a href="http://docs.opengeospatial.org/is/09-026r2/09-026r2.html">OGC® Filter Encoding 2.0 Encoding Standard</a></li>
 * </ul>
 *
 * <h2>General considerations:</h2>
 * <h3>Coordinate reference system handling:</h3>
 * As stated by Filter encoding 2.0.2, section 7.8.4, heterogeneous coordinate reference systems must be handled by
 * libraries, one way or another. The standard does not define any strategy. As Apache-SIS contains a powerful
 * transform system, we try to handle differences in the following way:
 * <ul>
 *   <li>
 *     If all evaluated geometries define a {@code srid}, but they are not the same, we try to project them in a common space.
 *     The strategy is guided by {@linkplain org.apache.sis.referencing.CRS#suggestCommonTarget Referencing utility method}.
 *     If it cannot provide a common space, we fail any ongoing operation.
 *   </li><li>
 *     Missing information:
 *     <ul>
 *       <li>If no geometry contains any {@code srid}, consider they are defined in the same space, and proceed.</li>
 *       <li>If one geometry define a CRS but the other do not, consider that an ambiguity resides: fail.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Optimizations</h2>
 * For now, few to no optimization is done in the operators.
 * Most important ones would require one of the two following things:
 * <ul>
 *   <li>
 *     Context information: Filters does not know in advance the feature type they are operating upon,
 *     which is essential to define some calculus parameters, as property value conversion strategy,
 *     spatial system changes, <i>etc.</i>
 *     Such information would allow operators to prepare data at initialization time.
 *   </li><li>
 *     User hints: some operations could be set faster at the cost of precision. To activate such things, it would
 *     require user consent. Most naive example is spatial reference system conversion, which could be de-activated
 *     for systems with nearly equal parameters (see {@link org.apache.sis.util.Utilities#equalsApproximately(Object, Object)}.
 *   </li>
 * </ul>
 *
 * <h2>Thread-safety</h2>
 * All filter and expression implementations provided by Apache SIS are thread-safe.
 * They are not necessarily stateless however; for example a filter may remember which
 * warnings have been reported in order to avoid to report the same warning twice.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @since 1.1
 * @module
 */
package org.apache.sis.filter;
