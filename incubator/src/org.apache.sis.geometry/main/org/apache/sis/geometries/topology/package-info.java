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
 * Topology interfaces of ISO 19107:2019 section 10.
 *
 * <p>A topological complex describes how primitives are connected, independently of their coordinates,
 * which turns geometric computations into combinatorial ones. Each primitive usually has a geometric
 * realization of the same dimension in the {@link org.apache.sis.geometries} package.</p>
 *
 * <table class="sis">
 *   <caption>Topological types</caption>
 *   <tr><th>Dimension</th> <th>Primitive</th>                  <th>Oriented primitive</th>                 <th>Usual realization</th></tr>
 *   <tr><td>0</td>         <td>{@link org.apache.sis.geometries.topology.Node}</td>  <td>{@link org.apache.sis.geometries.topology.DirectedNode}</td>  <td>{@link org.apache.sis.geometries.Point}</td></tr>
 *   <tr><td>1</td>         <td>{@link org.apache.sis.geometries.topology.Edge}</td>  <td>{@link org.apache.sis.geometries.topology.DirectedEdge}</td>  <td>{@link org.apache.sis.geometries.Curve}</td></tr>
 *   <tr><td>2</td>         <td>{@link org.apache.sis.geometries.topology.Face}</td>  <td>{@link org.apache.sis.geometries.topology.DirectedFace}</td>  <td>{@link org.apache.sis.geometries.Surface}</td></tr>
 *   <tr><td>3</td>         <td>{@link org.apache.sis.geometries.topology.Solid}</td> <td>{@link org.apache.sis.geometries.topology.DirectedSolid}</td> <td>{@link org.apache.sis.geometries.Solid}</td></tr>
 * </table>
 *
 * <p>As in the standard, each primitive extends its oriented type, so a primitive is its own positive
 * orientation. Oriented primitives are the variables of {@link org.apache.sis.geometries.topology.Expression},
 * the polynomials on which boundaries and co-boundaries are computed.</p>
 *
 * <h2>Differences with ISO 19107</h2>
 * <ul>
 *   <li>Attributes are getter methods, such as {@code getBoundary()} for {@code boundary}.</li>
 *   <li>The orientation reuses {@link org.apache.sis.geometries.Orientable.Sign}, and is read by
 *       {@code getOrientationSign()} as for geometric orientable primitives.</li>
 *   <li>The two proxies of a primitive are listed with the positive one first.</li>
 *   <li>The rings of a face and the shells of a solid are read by {@code getExteriorRing()}, {@code getInteriorRings()},
 *       {@code getExteriorShell()} and {@code getInteriorShells()}, instead of {@code FaceBoundary} and
 *       {@code SolidBoundary} types.</li>
 *   <li>The co-boundary of a solid is an empty list instead of {@code NULL}.</li>
 *   <li>The geometric complex realizing a topological complex, and the {@code GeometryData} of the construction
 *       data types, are a {@link org.apache.sis.geometries.Geometry}, since neither type exists in this module yet.</li>
 *   <li>The construction of a complex from a geometric collection is left to implementations,
 *       since Java interfaces have no constructor.</li>
 *   <li>{@code Expression} gives access to its terms, and tells whether it is zero.</li>
 * </ul>
 *
 * <h2>Not covered</h2>
 * <ul>
 *   <li>The derived topological relations of section 10.8: aggregate boundaries, Boolean, Egenhofer
 *       and full topological operators.</li>
 *   <li>The simplicial complexes of section 11.1.</li>
 *   <li>Editing operations, such as splitting an edge or a face, which the standard does not define.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
package org.apache.sis.geometries.topology;
