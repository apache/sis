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
 * Implementation of the {@link org.apache.sis.geometries.topology} interfaces with primitives stored by index.
 *
 * <ul>
 *   <li>{@link org.apache.sis.geometries.internal.shared.topology.ComplexBuilder} builds a complex from points,
 *       lines, polygons, polyhedra and meshes, and verifies that the interiors of its primitives are disjoint,
 *       except for the nodes and edges isolated in a face or a solid.</li>
 *   <li>{@link org.apache.sis.geometries.internal.shared.topology.IndexedComplex} stores the nodes, edges, faces
 *       and solids in arrays, and returns lightweight views implementing the topology interfaces.</li>
 *   <li>{@link org.apache.sis.geometries.internal.shared.topology.NodePoints} holds the positions and the other
 *       attributes of the nodes, such as normals or colors, which are kept by the geometric realizations.</li>
 *   <li>{@link org.apache.sis.geometries.internal.shared.topology.ComplexEditor} adds, splits, merges and removes
 *       primitives, modeled after the topology editing functions of ISO/IEC 13249-3 (SQL/MM Spatial).</li>
 *   <li>{@link org.apache.sis.geometries.internal.shared.topology.DefaultExpression} implements the polynomials
 *       of oriented primitives on which boundaries and co-boundaries are computed.</li>
 * </ul>
 *
 * @author Johann Sorel (Geomatys)
 */
package org.apache.sis.geometries.internal.shared.topology;
