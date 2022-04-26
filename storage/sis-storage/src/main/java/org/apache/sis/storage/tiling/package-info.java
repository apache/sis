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
 * Base types for retrieving and saving tiles in resources.
 * A {@link TiledResource} if a resource capable to describe its tiling schemes as {@link TileMatrixSet} instances.
 * A {@link TileMatrixSet} is a collection of {@link TileMatrix} instances in the same CRS but at different scale levels.
 * A {@link TileMatrix} is a collection of {@link Tile} instances with the same size and properties placed on a regular grid with no overlapping.
 * The "tile" word is used because of its wide usage with two-dimensional data, but actually this package has no restriction
 * on the number of dimensions and can work with multi-dimensional "tiles" as well.
 *
 * <h2>References</h2>
 * <ul>
 *   <li><a href="https://www.ogc.org/standards/tms">OGC Two Dimensional Tile Matrix Set</a> — the core standard used in this package.</li>
 *   <li><a href="https://www.ogc.org/standards/wmts">OGC Web Map Tile Service (WMTS)</a> — a common use of above standard.</li>
 *   <li><a href="https://docs.opengeospatial.org/is/17-066r1/17-066r1.html">OGC Geopackage: Extension for Tiled Gridded Coverage Data</a> — another common use.</li>
 *   <li><a href="https://docs.ogc.org/per/18-074.html">OGC Geopackage: Extension for vector tiles</a> — experimental work for tiled geometries.</li>
 * </ul>
 *
 * The concepts developed in above references are also used, often with different names, by other projects such as
 * <a href="https://wiki.osgeo.org/wiki/Tile_Map_Service_Specification">OSGeo Tile Map Service Specification</a>,
 * <a href="https://github.com/CesiumGS/quantized-mesh">Cesium QuantizedMesh</a>,
 * <a href="https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames">Open Street Map: Slippy Map</a>,
 * <a href="https://developers.google.com/maps/documentation/javascript/coordinates">Google Map</a> and
 * <a href="https://docs.mapbox.com/mapbox-tiling-service/guides/">MapBox Tiling service</a>.
 *
 *
 * <h2>Relationship with OGC specifications</h2>
 * The {@code TileMatrix} and {@code TileMatrixSet} class names are reused as defined by OGC.
 * The "2D" suffix in class names is omitted because this package is fully multi-dimensional.
 * The concept of "tiling scheme" is encapsulated in a {@link org.apache.sis.coverage.grid.GridGeometry}.
 *
 * <h3>Departures with OGC specifications</h3>
 * The OGC {@code TileMatrixLimits} class is replaced by {@link org.apache.sis.coverage.grid.GridExtent}.
 * The OGC restriction against negative numbers is removed (Apache SIS accepts negative tile indices).
 * The <var>tile span</var> and <var>tile matrix min/max</var> coefficients are replaced by a more generic
 * "grid to CRS" {@link org.opengis.referencing.operation.MathTransform}, usually affine but not necessarily.
 * Users may need to enforce above OGC restrictions themselves if compatibility with OGC specification is desired).
 *
 *
 * <h2>Relationship with Java2D rendered image</h2>
 * OGC tiles can be mapped to {@linkplain java.awt.image.RenderedImage#getTile(int, int) Java2D tiles}
 * with the following restrictions:
 *
 * <ul>
 *   <li>Java2D tile indices and pixel indices are 32 bits integer instead of 64 bits.
 *       See {@linkplain org.apache.sis.coverage.grid.GridCoverage#render grid coverage render}
 *       (in particular the relative pixel coordinates) for the workaround applied in Apache SIS.</li>
 *   <li>Java2D does not support coalescence coefficient (used in OGC tiles for compensating distortions near poles).
 *       Tiles in Java2D are expanded as needed as if the {@code TileMatrix} had no coalescence.</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
package org.apache.sis.storage.tiling;
