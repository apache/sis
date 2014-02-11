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
package org.apache.sis.io.wkt;

import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.crs.GeocentricCRS;
import org.apache.sis.util.Debug;
import org.apache.sis.metadata.iso.citation.Citations;


/**
 * The convention to use for WKT formatting.
 * This enumeration specifies whether to use the <cite>Well Known Text</cite> format defined by ISO 19162
 * (also known as “WKT 2”), or whether to use the format previously defined in OGC 01-009 (referenced as “WKT 1”).
 *
 * {@section WKT 1 variants}
 * The WKT 2 format should be parsed and formatted consistently by all softwares.
 * But the WKT 1 format has been interpreted differently by various implementors.
 * Apache SIS can adapt itself to different WKT variants, sometime automatically. But some aspects can not be guessed.
 * One noticeable source of confusion is the unit of measurement of {@code PRIMEM[…]} and {@code PARAMETER[…]} elements:
 *
 * <ul>
 *   <li>The unit of the Prime Meridian shall be the angular unit of the enclosing Geographic CRS
 *       according the OGC 01-009 (<cite>Coordinate transformation services</cite>) specification.</li>
 *   <li>An older specification — <cite>Simple Features</cite> — was unclear on this matter and has been
 *       interpreted by many softwares as fixing the unit to decimal degrees.</li>
 * </ul>
 *
 * Despite the first interpretation being specified by both OGC 01-009 and ISO 19162 standards, the second
 * interpretation appears to be in wide use for WKT 1. Apache SIS uses the standard interpretation by default,
 * but the {@link #WKT1_COMMON_UNITS} enumeration allows parsing and formatting using the older interpretation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-3.20)
 * @version 0.4
 * @module
 *
 * @see WKTFormat#getConvention()
 * @see WKTFormat#setConvention(Convention)
 */
public enum Convention {
    /**
     * The ISO 19162 format, also known as “WKT 2”.
     *
     * <p>Unless otherwise specified by {@link WKTFormat#setNameAuthority(Citation)}, projections
     * and parameters formatted with this convention will use the {@linkplain Citations#EPSG EPSG}
     * names when available.</p>
     *
     * <p>This is the default convention used by {@link FormattableObject#toWKT()}
     * and for new {@link WKTFormat} instances.</p>
     */
    WKT2(false),

    /**
     * The ISO 19162 format with omission of some optional elements. This convention is identical
     * to the {@link #WKT2} convention except for the following aspects:
     *
     * <ul>
     *   <li>{@code ANGLEUNIT}, {@code LENGTHUNIT}, {@code SCALEUNIT}, {@code PARAMETRICUNIT} and
     *       {@code TIMEUNIT} are formatted as plain {@code UNIT} elements.</li>
     *   <li>In {@code AXIS} elements, the {@code ORDER} sub-element is omitted.</li>
     *   <li>In {@code VERTICALEXTENT} elements, the {@code UNIT} sub-element is omitted
     *       if the unit is {@link javax.measure.unit.SI#METRE}.</li>
     *   <li>{@code ID[…]} elements are formatted for child elements in addition to the root one.
     *       This <em>addition</em> apparently goes against the simplification goal, but is often
     *       a helpful information when using Apache SIS.</li>
     * </ul>
     *
     * Those modifications are allowed by the ISO 19162 standard and do not cause any information lost.
     * The omitted elements are not needed by Apache SIS WKT parser and often distractive for the human reader.
     *
     * <p>This is the default convention used by {@link FormattableObject#toString()}.</p>
     */
    WKT2_SIMPLIFIED(false),

    /**
     * The OGC 01-009 format, also known as “WKT 1”.
     * A definition for this format is shown in Extended Backus Naur Form (EBNF)
     * <a href="http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html">on GeoAPI</a>.
     *
     * <p>Unless otherwise specified by {@link WKTFormat#setNameAuthority(Citation)}, projections
     * and parameters formatted with this convention will use the {@linkplain Citations#OGC OGC}
     * names when available.</p>
     *
     * {@section Differences compared to WKT 2}
     * WKT 1 and WKT 2 differ in their keywords and syntax, but also in more subtle ways regarding parameter
     * and code list values. For {@link GeocentricCRS}, WKT 1 uses a legacy set of Cartesian axes which were
     * defined in OGC 01-009. Those axes use the <var>Other</var>, <var>Easting</var> and <var>Northing</var>
     * {@linkplain org.opengis.referencing.cs.AxisDirection axis directions} instead than the geocentric ones,
     * as shown in the following table:
     *
     * <table class="sis">
     *   <tr><th>ISO 19111</th>    <th>OGC 01-009</th> <th>Description</th></tr>
     *   <tr><td>Geocentric X</td> <td>Other</td>      <td>Toward prime meridian</td></tr>
     *   <tr><td>Geocentric Y</td> <td>Easting</td>    <td>Toward 90°E longitude</td></tr>
     *   <tr><td>Geocentric Z</td> <td>Northing</td>   <td>Toward north pole</td></tr>
     * </table>
     */
    WKT1(true),

    /**
     * The <cite>Simple Feature</cite> format, also known as “WKT 1”.
     * <cite>Simple Feature</cite> is anterior to OGC 01-009 and defines the same format,
     * but was unclear about the unit of measurement for prime meridians and projection parameters.
     * Consequently many implementations interpreted those angular units as fixed to degrees instead
     * than being context-dependent.
     *
     * <p>This convention is identical to {@link #WKT1} except for the following aspects:</p>
     * <ul>
     *   <li>The angular units of {@code PRIMEM} and {@code PARAMETER} elements are always degrees,
     *       no matter the units of the enclosing {@code GEOGCS} element.</li>
     *   <li>Unit names use American spelling instead than the international ones
     *       (e.g. "<cite>meter</cite>" instead than "<cite>metre</cite>").</li>
     * </ul>
     */
    WKT1_COMMON_UNITS(true),

    /**
     * A special convention for formatting objects as stored internally by Apache SIS.
     * The result is similar to the one produced using the {@link #WKT2_SIMPLIFIED} convention,
     * with the following differences:
     *
     * <ul>
     *   <li>In {@code ID[…]} elements, the {@code URI[…]} sub-element is omitted if the sub-element
     *       was derived by Apache SIS from the other {@code ID[…]} properties.</li>
     *   <li>Map projections are shown as SIS stores them internally, i.e. with the separation between
     *       linear and non-linear steps, rather than as a single operation.</li>
     * </ul>
     *
     * This convention is used only for debugging purpose.
     */
    @Debug
    INTERNAL(false);

    /**
     * The default conventions.
     *
     * @todo Make final after we completed the migration from Geotk.
     */
    static Convention DEFAULT = WKT2, DEFAULT_SIMPLIFIED = WKT2_SIMPLIFIED;

    /**
     * {@code true} for using WKT 1 syntax, or {@code false} for using WKT 2 syntax.
     */
    private final boolean isWKT1;

    /**
     * Creates a new enumeration value.
     */
    private Convention(final boolean isWKT1) {
        this.isWKT1 = isWKT1;
    }

    /**
     * Returns {@code true} if this convention is one of the WKT 1 variants.
     *
     * @return {@code true} if this convention is one of the WKT 1 variants.
     */
    public boolean isWKT1() {
        return isWKT1;
    }

    /**
     * {@code true} for a simplified WKT. The simplifications are documented in the {@link #WKT2_SIMPLIFIED} javadoc.
     */
    final boolean isSimple() {
        return this != WKT2;
    }

    /**
     * {@code true} if the identifiers should be formatted for all elements instead of only the last one.
     */
    final boolean showIdentifiers() {
        return this == WKT2_SIMPLIFIED || this == INTERNAL;
    }

    /**
     * {@code true} for a frequently-used convention about units instead than the standard one.
     * <ul>
     *   <li>If {@code true}, forces {@code PRIMEM} and {@code PARAMETER} angular units to degrees
     *       instead than inferring the unit from the context. The standard value is {@code false},
     *       which means that the angular units are inferred from the context as required by the
     *       WKT 1 specification.</li>
     *   <li>If {@code true}, uses US unit names instead of the international names.
     *       For example Americans said {@code "meter"} instead of {@code "metre"}.</li>
     * </ul>
     */
    final boolean usesCommonUnits() {
        return this == WKT1_COMMON_UNITS;
    }

    /**
     * Returns the default authority to look for when fetching Map Projection parameter names.
     * The value returned by this method can be overwritten by {@link WKTFormat#setNameAuthority(Citation)}.
     *
     * {@example The following table shows the names given by various organizations or projects for the same projection:
     *
     * <table class="sis">
     *   <tr><th>Authority</th> <th>Projection name</th></tr>
     *   <tr><td>EPSG</td>      <td>Mercator (variant A)</td></tr>
     *   <tr><td>OGC</td>       <td>Mercator_1SP</td></tr>
     *   <tr><td>GEOTIFF</td>   <td>CT_Mercator</td></tr>
     * </table>}
     *
     * @return The organization, standard or project to look for when fetching Map Projection parameter names.
     *
     * @see WKTFormat#getNameAuthority()
     * @see Citations#EPSG
     * @see Citations#OGC
     */
    public Citation getNameAuthority() {
        return isWKT1 ? Citations.OGC : Citations.EPSG;
    }
}
