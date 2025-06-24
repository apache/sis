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
 *
 * Based on specifications :
 * <ul>
 * <li>ISO 19107 version 2019</li>
 * <li>OGC Simple Feature Access https://www.ogc.org/standards/sfa</li>
 * <li>Khronos GLTF-2 https://github.com/KhronosGroup/glTF/tree/main/specification/2.0</li>
 * </ul>
 *
 * This API being a merge of different geometry standard some elements differ from the original specifications.
 * OGC SFA is a simplified version on ISO 19107.
 * Khronos GLTF-2 is a set of highly specialized implementations.
 *
 * <h2>API Differences</h2>
 *
 * <h3>Key differences with OGC SFA</h3>
 * <ul>
 *   <li>SRID : replaced by getCoordinateReferenceSystem</li>
 *   <li>is3D : look at geometry CoordinateReferenceSystem instead</li>
 *   <li>isMeasured() : replaced by Attributive interface which holds more informations.</li>
 *   <li>Relation and query function have been moved to {@link  org.apache.sis.geometries.operation.GeometryOperations  GeometryOperations}</li>
 *   <li>Query3D interface is moved to {@link  org.apache.sis.geometries.operation.GeometryOperations#SpatialAnalysis3D  SpatialAnalysis3D}
 *       and {@link  org.apache.sis.geometries.operation.GeometryOperations#SpatialRelations3D  SpatialRelations3D}</li>
 * </ul>
 *
 * <h3>Key differences with ISO 19107</h3>
 * <ul>
 *   <li>Encoding interface is fused in Geometry interface</li>
 *   <li>Query2D interface is moved to {@link  org.apache.sis.geometries.operation.GeometryOperations#SpatialAnalysis2D  SpatialAnalysis2D}
 *       and {@link  org.apache.sis.geometries.operation.GeometryOperations#SpatialRelations2D  SpatialRelations2D}</li>
 *   <li>Query3D interface is moved to {@link  org.apache.sis.geometries.operation.GeometryOperations#SpatialAnalysis3D  SpatialAnalysis3D}
 *       and {@link  org.apache.sis.geometries.operation.GeometryOperations#SpatialRelations3D  SpatialRelations3D}</li>
 *   <li>Encoding.asGML has been removed since it is a large task to implement and multiple versions
 *       exists. GML support should be located in a different module</li>
 * </ul>
 *
 *
 * <h2>Remaining work to be done</h2>
 *
 * <h3>TODO : ISO 19107 CRS</h3>
 * No interface or implementation yet, check SIS CRS have everything needed.
 *
 * <h3>TODO : ISO 19107 Geometry has an mbResult ?</h3>
 * This attribute is only on the UML but not in the document, recheck this.
 *
 * <h3>TODO : ISO 19107 Complex (Section 6.4.34)</h3>
 * No interface or implementation yet.
 * This is special sub class of Collection with advance analysis capabilities.
 * Add methode getMaximalComplex on Geometry, see section 6.4.4.17
 *
 * <h3>TODO : ISO 19107 Topology (Section 10)</h3>
 * No interface or implementation yet.
 * This work would be interesting to formalize delaunay classes.
 *
 * <h3>TODO : ISO 19107 Simplex (Section 11.1)</h3>
 * No interface or implementation yet.
 *
 * <h3>TODO : ISO 19107 PointCloud (Section 11.2.2)</h3>
 * No interface or implementation yet.
 *
 * <h3>TODO : ISO 19107 PointCloud (Section 6.4.4.27)</h3>
 * Geometry has an equal method with a surface parameter.
 *
 *
 */
package org.apache.sis.geometries;
