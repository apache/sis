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
 * <a href="http://www.geoapi.org/snapshot/javadoc/org/opengis/referencing/doc-files/WKT.html"><cite>Well
 * Known Text</cite> (WKT)</a> parsing and formatting. This package provides the internal mechanism used
 * by SIS implementation. Most users do not need to know about it, except if they want to customize the
 * WKT output. For example it is possible to:
 *
 * <ul>
 *   <li>{@linkplain org.apache.sis.io.wkt.WKTFormat#setConvention Format the parameters using the names
 *       of an other authority than OGC}. For example a user may want to format map projections using the
 *       {@linkplain org.apache.sis.io.wkt.Convention#GEOTIFF GeoTIFF} parameter names.</li>
 *   <li>{@linkplain org.apache.sis.io.wkt.WKTFormat#setSymbols Use curly brackets instead than square ones},
 *       for example {@code DATUM("WGS84")} instead than {@code DATUM["WGS84"]}.
 *       The former is legal WKT, while less frequently used than the later one.</li>
 *   <li>{@linkplain org.apache.sis.io.wkt.WKTFormat#setColors Apply syntactic coloring} for output
 *       on terminal supporting <cite>ANSI escape codes</cite> (a.k.a. ECMA-48, ISO/IEC 6429 and X3.64).</li>
 *   <li>{@linkplain org.apache.sis.io.wkt.WKTFormat#setIndentation Use a different indentation}, or
 *       format the whole WKT on a {@linkplain org.apache.sis.io.wkt.WKTFormat#SINGLE_LINE single line}.</li>
 * </ul>
 *
 * Current implementation is primarily designed for parsing and formatting referencing objects.
 * However other objects (especially the one for geometric objects) are expected to be provided
 * here in future versions.
 *
 * {@section Referencing WKT}
 * Parsing of {@linkplain org.apache.sis.referencing.crs.AbstractCoordinateReferenceSystem Coordinate Reference System}
 * and {@linkplain org.apache.sis.referencing.operation.transform.AbstractMathTransform Math Transform} objects
 * are performed by the {@link org.apache.sis.io.wkt.ReferencingParser} class. The parser provides methods for:
 *
 * <ul>
 *   <li>Specifying whatever the default axis names shall be ISO identifiers or the
 *       legacy identifiers specified in the WKT specification.</li>
 *   <li>Ignoring the {@code AXIS[...]} elements. This approach can be used as a way to force
 *       the (<var>longitude</var>, <var>latitude</var>) axes order.</li>
 * </ul>
 *
 * {@section Geometry WKT}
 * The {@link org.apache.sis.geometry.GeneralEnvelope} and
 * {@link org.apache.sis.geometry.GeneralDirectPosition} classes provide their own, limited,
 * WKT parsing and formatting services for the {@code BOX} and {@code POINT} elements.
 *
 * {@section References}
 * <ul>
 *   <li><a href="http://www.geoapi.org/snapshot/javadoc/org/opengis/referencing/doc-files/WKT.html">Well Known Text specification</a></li>
 *   <li><a href="http://home.gdal.org/projects/opengis/wktproblems.html">OGC WKT Coordinate System Issues</a></li>
 *   <li><a href="http://en.wikipedia.org/wiki/Well-known_text">Well Known Text in Wikipedia</a></li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Rémi Eve (IRD)
 * @author  Rueben Schulz (UBC)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
package org.apache.sis.io.wkt;
