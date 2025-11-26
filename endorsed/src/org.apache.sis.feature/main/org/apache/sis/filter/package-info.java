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
 * A <dfn>filter expression</dfn> is a construct used to constraint a feature set to a subset.
 * Operations in this package follow the rules of
 * <a href="http://docs.opengeospatial.org/is/09-026r2/09-026r2.html">OGC® Filter Encoding</a> and
 * <a href="https://www.iso.org/standard/60343.html">ISO 13249-3 - SQLMM</a> standards.
 *
 * <h2>Coordinate reference system handling</h2>
 * Filters and expressions may contain heterogeneous coordinate reference systems.
 * Apache SIS tries to handle differences in the following way:
 * <ul>
 *   <li>If at least one geometry does not has a CRS, then SIS assumes that the geometries are in the same space.</li>
 *   <li>If all geometries are in the same CRS, no coordinate operation is applied.</li>
 *   <li>If geometries have non-null but different CRS, then SIS tries to project the geometries in a common space:
 *     <ul>
 *       <li>For SQLMM operations, the CRS of the first operand is used (as required by the specification).</li>
 *       <li>For other operations, the common CRS is chosen by
 *         {@linkplain org.apache.sis.referencing.CRS#suggestCommonTarget referencing utility method}.
 *         If that method cannot provide a common space,
 *         then an {@link java.lang.IllegalArgumentException} is thrown.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Performance tips</h2>
 * <p>In expressions like {@code ST_Intersects(A,B)} where the <var>A</var> and <var>B</var> parameters are two
 * sub-expressions evaluating to geometry values, if one of those expressions is a literal, then that literal
 * should be <var>B</var>. The reason is because SQLMM specification requires us to project <var>B</var>
 * in the Coordinate Reference System of <var>A</var>. If <var>B</var> is a literal, Apache SIS can do
 * this transformation only once before to start any evaluation instead of every time that the expression
 * needs to be evaluated.</p>
 *
 * <p>Data store implementations should apply {@link org.apache.sis.filter.Optimization} on the filters
 * before to evaluate them.</p>
 *
 * <h2>Thread-safety</h2>
 * All filter and expression implementations provided by Apache SIS are thread-safe.
 * They are not necessarily stateless however; for example a filter may remember which
 * warnings have been reported in order to avoid to report the same warning twice.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 *
 * @since 1.1
 */
package org.apache.sis.filter;
