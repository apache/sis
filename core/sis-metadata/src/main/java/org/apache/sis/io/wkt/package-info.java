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
 * This package implements the services provided by various convenience methods:
 *
 * <ul>
 *   <li>{@link org.apache.sis.referencing.CRS#fromWKT(String)} (SIS parsing static method)</li>
 *   <li>{@link org.opengis.referencing.crs.CRSFactory#createFromWKT(String)} (GeoAPI parsing method)</li>
 *   <li>{@link org.opengis.referencing.operation.MathTransformFactory#createFromWKT(String)} (GeoAPI parsing method)</li>
 *   <li>{@link org.opengis.referencing.IdentifiedObject#toWKT()} (GeoAPI formatting method)</li>
 * </ul>
 *
 * However the {@link org.apache.sis.io.wkt.WKTFormat} class provided in this package gives more control.
 * For example this package allows to:
 *
 * <ul>
 *   <li>Format projection and parameters using the names of a chosen authority.
 *       For example the <cite>"Mercator (variant A)"</cite> projection is named
 *       {@code "Mercator_1SP"} by OGC 01-009 and {@code "CT_Mercator"} by GeoTIFF.</li>
 *   <li>Format the elements with different quote characters or brackets style.
 *       For example both {@code ID["EPSG",4326]} and {@code ID("EPSG",4326)} are legal WKT.</li>
 *   <li>Format with a different indentation or format the whole WKT on a single line.</li>
 *   <li>Apply syntactic coloring on terminal supporting <cite>ANSI escape codes</cite>
 *       (a.k.a. ECMA-48, ISO/IEC 6429 and X3.64).</li>
 *   <li>Alter the parsing in a way compatible with non-standard (but commonly used) WKT.
 *       For example some others softwares ignore the {@code AXIS[…]} elements at parsing time.</li>
 *   <li>Report warnings that occurred during parsing or formatting.</li>
 * </ul>
 *
 * <div class="section">Referencing WKT</div>
 * Referencing WKT is defined using Extended Backus Naur Form (EBNF) in two versions:
 * <ul>
 *   <li>ISO 19162 defines the current format, also known as “WKT 2”. The specification is also made
 *       <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html">available online</a> by OGC.</li>
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
 * <div class="section">Geometry WKT</div>
 * The {@link org.apache.sis.geometry.GeneralEnvelope} and {@link org.apache.sis.geometry.GeneralDirectPosition} classes
 * provide their own, limited, WKT parsing and formatting services for the {@code BOX} and {@code POINT} elements.
 * A description for this WKT format can be found on <a href="http://en.wikipedia.org/wiki/Well-known_text">Wikipedia</a>.
 *
 * <div class="section">Where to find WKT examples</div>
 * An excellent source of well-formed WKT is the online <cite>EPSG Geodetic Parameter Registry</cite>.
 * The WKT of many Coordinate Reference System object can be viewed using the pattern below
 * (replace {@code 3395} by the EPSG code of the desired CRS):
 *
 * <blockquote><b>Example</b>: <cite>"WGS 84 / World Mercator"</cite>:
 * <a href="http://epsg-registry.org/export.htm?wkt=urn:ogc:def:crs:EPSG::3395">http://epsg-registry.org/export.htm?wkt=urn:ogc:def:crs:EPSG::3395</a>
 * </blockquote>
 *
 * Readers should be aware that some popular other sources of WKT are actually invalid,
 * since many of them do not comply with EPSG definitions (especially on axis order).
 * The above-cited EPSG registry is <strong>the</strong> authoritative source
 * of CRS definitions in the EPSG namespace.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Rémi Eve (IRD)
 * @author  Rueben Schulz (UBC)
 * @since   0.4
 * @version 0.6
 * @module
 *
 * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html">WKT 2 specification</a>
 * @see <a href="http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html">Legacy WKT 1</a>
 */
package org.apache.sis.io.wkt;
