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
 * <cite>Well Known Text</cite> (WKT) parsing and formatting.
 * This package implements the services provided by the {@link org.apache.sis.referencing.CRS#parseWKT(String)}
 * and {@link org.opengis.referencing.IdentifiedObject#toWKT()} convenience methods, with more control.
 * For example this package allows to:
 *
 * <ul>
 *   <li>Format projection and parameters using the names of a chosen authority. For example the
 *       "<cite>Mercator (variant A)</cite>" projection is named {@code "Mercator_1SP"} by OGC 01-009
 *       and {@code "CT_Mercator"} by GeoTIFF.</li>
 *   <li>Format the elements with curly brackets instead than square ones.
 *       For example both {@code ID["EPSG",4326]} and {@code ID("EPSG",4326)} are legal WKT.</li>
 *   <li>Format with a different indentation or format the whole WKT on a single line.</li>
 *   <li>Apply syntactic coloring on terminal supporting <cite>ANSI escape codes</cite>
 *       (a.k.a. ECMA-48, ISO/IEC 6429 and X3.64).</li>
 *   <li>Ignore the {@code AXIS[…]} elements at parsing time. This approach can be used as a way to force
 *       the (<var>longitude</var>, <var>latitude</var>) axes order.</li>
 * </ul>
 *
 * {@section Referencing WKT}
 * Referencing WKT is defined using Extended Backus Naur Form (EBNF) in two versions:
 * <ul>
 *   <li>ISO 19162 defines the current format, also known as “WKT 2”.</li>
 *   <li>The previous format — “WKT 1” — was defined in the <a href="http://www.opengeospatial.org/standards/ct">OGC
 *       document 01-009</a>. This definition is
 *       <a href="http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html">shown on GeoAPI</a>.</li>
 * </ul>
 *
 * The WKT 1 format has been interpreted differently by various implementors.
 * One noticeable difference is the unit of measurement of prime meridians and projection parameters.
 * The WKT 2 format aims to solve the inter-operability problem caused by such mismatches,
 * but not all softwares support this new format. Consequently importing or exporting data from/to a software with
 * the WKT syntax require knowledge of the WKT variant used by that software. This variant can be specified by the
 * {@link org.apache.sis.io.wkt.Convention} enumeration.
 *
 * {@section Geometry WKT}
 * The {@link org.apache.sis.geometry.GeneralEnvelope} and {@link org.apache.sis.geometry.GeneralDirectPosition} classes
 * provide their own, limited, WKT parsing and formatting services for the {@code BOX} and {@code POINT} elements.
 * A description for this WKT format can be found on <a href="http://en.wikipedia.org/wiki/Well-known_text">Wikipedia</a>.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Rémi Eve (IRD)
 * @author  Rueben Schulz (UBC)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
package org.apache.sis.io.wkt;
