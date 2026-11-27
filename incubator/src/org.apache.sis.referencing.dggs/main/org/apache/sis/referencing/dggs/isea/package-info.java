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
 * <h2>Origin</h2>
 * ISEA package takes it's origin from the Geogrid project.
 * https://github.com/mocnik-science/geogrid
 *
 * Provides methods to generate and handle the ISEA Aperture 3 Hexagon Discrete Global Grid System
 * (ISEA3H DGGS) using the Inverse Snyder Equal-Area Projection (ISEA) in dual license MIT and Apache 2.
 *
 * <h2>Scientific Publications</h2>
 *
 * <p>An overview of the ISEA3H DGGS and the identifier scheme used in this library can be found in:</p>
 * <ul>
 *   <li>F-B Mocnik: <a href="http://doi.org/10.1080/15230406.2018.1455157"><b>A Novel Identifier Scheme for the
 *       ISEA Aperture 3 Hexagon Discrete Global Grid System.</b></a> Cartography and Geographic Information
 *       Science 46(3), 2019, 227–291</li>
 * </ul>
 *
 * <p>The ISEA3H has originally been proposed by:</p>
 * <ul>
 *   <li>K Sahr, D White, and AJ Kimerling: <a href="http://doi.org/10.1559/152304003100011090"><b>Geodesic
 *       Discrete Global Grid Systems</b></a>. Cartography and Geographic Information Science, 30(2), 2003,
 *       121–134. doi:10.1559/152304003100011090</li>
 * </ul>
 *
 * <p>The projection has been proposed and has been described in detail by:</p>
 * <ul>
 *   <li>JP Snyder: <a href="http://doi.org/10.3138/27H7-8K88-4882-1752"><b>An equal-area map projection for
 *       polyhedral globes</b></a>. Cartographica, 29(1), 1992, 10–21.
 *       doi:10.3138/27H7-8K88-4882-1752</li>
 * </ul>
 *
 * <h2>Short Overview</h2>
 *
 * <h3>ISEA Projection</h3>
 *
 * <p>The ISEA projection maps the regular icosahedron to the sphere. A hexagonal grid introduced on the
 * icosahedron can thus be mapped to the sphere as well. As the ISEA projection and the ISEA3H grid are thus
 * strongly interlinked, this library offers an implementation of both, the ISEA projection and the ISEA3H
 * grid.</p>
 *
 * <p>A location on the icosahedron can be mapped to the sphere, as in the following example of the coordinate
 * {@code (20.3, 12.5)} on face {@code 2}:</p>
 *
 * <pre>{@code
 * ISEAProjection projection = new ISEAProjection();
 * GeoCoordinates c = projection.icosahedronToSphere(new FaceCoordinates(2, 20.3, 12.5));
 * c.getLat(); // 52.739063
 * c.getLon(); // 0.315051
 * }</pre>
 *
 * <p>The resulting geographic coordinates can be projected back to the icosahedron:</p>
 *
 * <pre>{@code
 * FaceCoordinates c2 = projection.sphereToIcosahedron(c);
 * c2.getFace(); // 2
 * c2.getX(); // 20.3
 * c2.getY(); // 12.5
 * }</pre>
 *
 * <h3>ISEA3H DGGS: Determining the Cell for a Given Location</h3>
 *
 * <p>When the grid cell containing a given location should be determined, the method
 * {@code cellForLocation} can be used, either for a pair of coordinates, or an instance of
 * {@code GeoCoordinates}:</p>
 *
 * <pre>{@code
 * GridCell gc1 = grid.cellForLocation(41.5, 6.5);
 * GridCell gc2 = grid.cellForLocation(new GeoCoordinates(41.5, 6.5));
 * }</pre>
 *
 * <p>The cell containing the centroid of a {@code Geometry} (from the
 * <a href="https://github.com/locationtech/jts">JTS Topology Suite</a>) can be determined accordingly:</p>
 *
 * <pre>{@code
 * grid.cellForCentroid(g);
 * }</pre>
 *
 * <h3>ISEA3H DGGS: Computing the Cells</h3>
 *
 * <p>The grid cells of the ISEA3H for a given boundary box can easily be computed as follows:</p>
 *
 * <pre>{@code
 * ISEA3H grid = new ISEA3H(10);
 * Collection<GridCell> cells = grid.cellsForBound(41, 42, 6, 7);
 * }</pre>
 *
 * <p>Observe that the constructor of the class {@code ISEA3H} accepts the grid resolution as a parameter. The
 * method {@code cellsForBound} computes, in this case, all cells that are (about) contained in the area between
 * {@code 41} and {@code 42} degree latitude and {@code 6} and {@code 7} degree longitude. If all cells should be
 * computed, the method {@code cells} can be used instead:</p>
 *
 * <pre>{@code
 * Collection<GridCell> cellsAll = grid.cells();
 * }</pre>
 *
 * <p>The resulting collection of cells contains instances of {@code GridCell}. Such an instance contains
 * information about the centroid of the grid cell, the grid resolution, whether it is a hexagon (or a pentagon)
 * cell, and an identifier (ID):</p>
 *
 * <pre>{@code
 * for (GridCell gc : cells) {
 *     gc.getLat();
 *     gc.getLon();
 *     gc.getResolution();
 *     gc.isPentagon();
 *     gc.getID();
 * }
 * }</pre>
 *
 * <p>Different types of cell IDs can be used:</p>
 *
 * <table>
 *   <caption>Types of cell IDs</caption>
 *   <tr><th>Type of ID</th><th>Description</th><th>Typical use</th></tr>
 *   <tr><td>{@code NON_ADAPTIVE}</td>
 *       <td><i>Non-adaptive IDs</i> include the geometry of the cell in a high precision.</td>
 *       <td>Perfect rendering of the grid cells</td></tr>
 *   <tr><td>{@code ADAPTIVE_1_PERCENT}</td>
 *       <td><i>Adaptive 1% IDs</i> include the geometry in a scale dependent but yet reasonable precision.</td>
 *       <td>Rendering of the grid cells on a website</td></tr>
 *   <tr><td>{@code ADAPTIVE_UNIQUE}</td>
 *       <td><i>Adaptive unique IDs</i> aim at being as short as possible while preserving uniqueness.</td>
 *       <td>Comparison of whether two cells are identical</td></tr>
 * </table>
 *
 * <p>Further information can be found in:</p>
 * <ul>
 *   <li>F-B Mocnik: <a href="http://doi.org/10.1080/15230406.2018.1455157"><b>A Novel Identifier Scheme for the
 *       ISEA Aperture 3 Hexagon Discrete Global Grid System.</b></a> Cartography and Geographic Information
 *       Science 46(3), 2019, 227–291</li>
 * </ul>
 *
 * <p>By default, non-adaptive IDs are returned. The type of ID can though be inserted explicitly as follows:</p>
 *
 * <pre>{@code
 * gc.getID(NON_ADAPTIVE);
 * gc.getID(ADAPTIVE_1_PERCENT);
 * gc.getID(ADAPTIVE_UNIQUE);
 * }</pre>
 *
 * <h3>ISEA3H DGGS: Computing Cell IDs Only</h3>
 *
 * <p>In case that just the identifiers (IDs) of the cells should be computed, two more efficient methods can be
 * used:</p>
 *
 * <pre>{@code
 * ISEA3H grid = new ISEA3H(10);
 * Collection<Long> cells = grid.cellIDsForBound(41, 42, 6, 7);
 * Collection<Long> cells2 = grid.cellIDsForBound(41, 42, 6, 7, ADAPTIVE_UNIQUE);
 * Collection<Long> cellsAll = grid.cellIDs();
 * Collection<Long> cellsAll2 = grid.cellIDs(ADAPTIVE_UNIQUE);
 * }</pre>
 *
 * <p>The IDs can even be saved to the hard disc:</p>
 *
 * <pre>{@code
 * ISEA3H grid = new ISEA3H(10);
 * grid.cellIDs("path/filename");
 * grid.cellIDs("path/filename2", ADAPTIVE_UNIQUE);
 * }</pre>
 *
 * <p>Each of the created files contains only IDs that refer to one face of the icosahedron, and the data is
 * divided into a number of chunks. The same ID can occur multiple times (for different faces) when it refers to
 * a cell that is partially contained in different faces. The files are named as {@code filename.face.chunk_number}.
 * The first part of this name refers to the filename provided in the method {@code cellIDs}; the second part, to
 * the face to which the IDs contained in the file belong to; and the third part consists of consecutive numbers
 * to distinguish the chunks.</p>
 *
 * <h2>ISEA Projection</h2>
 *
 * <h3>Properties</h3>
 *
 * <p>Many properties of the ISEA projection are provided by an instance of the {@code ISEAProjection}. The
 * following table provides an overview over some of them:</p>
 *
 * <table>
 *   <caption>Properties of the ISEA projection</caption>
 *   <tr><th>Function</th><th>Description</th></tr>
 *   <tr><td>{@code numberOfFaces()}</td><td>Number of faces of the icosahedron</td></tr>
 *   <tr><td>{@code maximumAngularDistortion()}</td><td>Maximum angular distortion</td></tr>
 *   <tr><td>{@code maximumScaleVariation()}</td><td>Maximum scale variation</td></tr>
 *   <tr><td>{@code miniumScaleVariation()}</td><td>Minimum scale variation</td></tr>
 *   <tr><td>{@code lengthOfTriangleBase()}</td><td>Length of the bases of the triangles of the icosahedron</td></tr>
 *   <tr><td>{@code sphericalDistanceFromCenterToVerticesOnSphere()}</td>
 *       <td>Spherical distance from center of a face to any of its vertices on the sphere; in degrees</td></tr>
 * </table>
 *
 * <h3>Orientation</h3>
 *
 * <p>The ISEA projection maps from the icosahedron to the sphere. By default, two of the vertices of the faces of
 * the icosahedron are mapped to the poles. Another orientation might, however, be useful in many cases, as, for
 * example, in the case of the ISEA3H DGGS: distortions are especially large at the vertices of the faces, and one
 * aims at mapping the vertices to places that are within the oceans. Such an ‘ideal’ choice of an orientation can
 * be reached by:</p>
 *
 * <pre>{@code
 * ISEAProjection projection = new ISEAProjection();
 * projection.setOrientationSymmetricEquator();
 * }</pre>
 *
 * <p>If the orientation is to be set manually by shifting the orientation relative to the standard orientation
 * (two of the vertices are mapped to the poles), the following method shifts every location by the angle
 * {@code orientationLon} in the direction of positive longitude, and thereafter by the angle {@code orientationLat}
 * in direction of positive latitude:</p>
 *
 * <pre>{@code
 * projection.setOrientation(orientationLat, orientationLon);
 * }</pre>
 *
 * <h2>ISEA3H DGGS</h2>
 *
 * <h3>Properties</h3>
 *
 * <p>Many properties of the ISEA3H DGGS are provided by an instance of the {@code ISEA3H}. The following table
 * provides an overview over some of them:</p>
 *
 * <table>
 *   <caption>Properties of the ISEA3H DGGS</caption>
 *   <tr><th>Function</th><th>Description</th></tr>
 *   <tr><td>{@code diameterOfHexagonalCellOnIcosahedron()}</td>
 *       <td>Diameter of a hexagonal cell on the icosahedron, in kilometres</td></tr>
 *   <tr><td>{@code lengthOfASideOfHexagonalCellOnIcosahedron()}</td>
 *       <td>Length of a side of a hexagonal cell on the icosahedron, in kilometres</td></tr>
 *   <tr><td>{@code lowerBoundForLengthOfASideOfHexagonalCellOnSphere()}</td>
 *       <td>Lower bound for the length of a side of a hexagonal cell on the sphere, in kilometres</td></tr>
 *   <tr><td>{@code areaOfAHexagonalCell()}</td>
 *       <td>Area of a hexagonal cell; the cells have all the same area by construction, because the ISEA
 *       projection is equal-area, in square kilometres</td></tr>
 *   <tr><td>{@code areaOfAPentagonalCell()}</td>
 *       <td>Area of a pentagonal cell; the cells have all the same area by construction, because the ISEA
 *       projection is equal-area, in square kilometres</td></tr>
 *   <tr><td>{@code numberOfHexagonalCells()}</td><td>Number of hexagonal cells</td></tr>
 *   <tr><td>{@code numberOfPentagonalCells()}</td><td>Number of pentagonal cells</td></tr>
 * </table>
 *
 * <h3>Threads</h3>
 *
 * <p>Some of the computations can be executed in parallel. The number of threads can be set as follows:</p>
 *
 * <pre>{@code
 * ISEA3H grid = new ISEA3H(10);
 * grid.setNumberOfThreads(8);
 * }</pre>
 *
 * <p>By default, 8 threads are used.</p>
 *
 *
 * <h2>License</h2>
 *
 * <p>In 2017–2019, this software has been supported by the DFG project <i>A framework for measuring the fitness
 * for purpose of OpenStreetMap data based on intrinsic quality indicators</i> (FA 1189/3-1).</p>
 *
 * <p>All commits after 9/2019 (c) by Franz-Benjamin Mocnik, 2019.<br>
 * All commits before 8/2019 (c) by Heidelberg University, 2017–2019.</p>
 *
 * <p>The code is licensed both under the <a href="https://github.com/mocnik-science/geogrid/blob/master/LICENSE">MIT
 * license</a> and under the <a href="https://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 license</a>.</p>
 *
 * @author Franz-Benjamin Mocnik, <a href="mailto:mail@mocnik-science.net">mail@mocnik-science.net</a>
 */
package org.apache.sis.referencing.dggs.isea;
