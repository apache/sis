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

import javax.measure.unit.Unit;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Quantity;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.crs.GeocentricCRS;
import org.apache.sis.util.Debug;
import org.apache.sis.metadata.iso.citation.Citations;

import static javax.measure.unit.NonSI.DEGREE_ANGLE;


/**
 * The convention to use for WKT formatting.
 * This enumeration specifies whether to use the <cite>Well Known Text</cite> format defined by ISO 19162
 * (also known as “WKT 2”), or whether to use the format previously defined in OGC 01-009 (referenced as “WKT 1”).
 *
 * {@section Apache SIS extensions to WKT 2}
 * The WKT 2 format does not define any syntax for {@link org.opengis.referencing.operation.MathTransform} instances,
 * and consequently does not provide {@link org.opengis.referencing.crs.DerivedCRS} representations. Apache SIS uses
 * the WKT 1 format for {@code MathTransform} and extends the WKT 2 format with a {@code DerivedCRS} representation
 * that contains those math transforms.
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
     * This is the default convention for all WKT formatting in the Apache SIS library.
     *
     * <p>Unless otherwise specified by {@link WKTFormat#setNameAuthority(Citation)}, when using
     * this convention SIS will favor {@linkplain Citations#EPSG EPSG} definitions of projection
     * and parameter names.</p>
     */
    WKT2(Citations.EPSG, false, false),

    /**
     * The OGC 01-009 format, also known as “WKT 1”.
     * A definition for this format is shown in Extended Backus Naur Form (EBNF)
     * <a href="http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html">on GeoAPI</a>.
     *
     * <p>Unless otherwise specified by {@link WKTFormat#setNameAuthority(Citation)}, when using
     * this convention SIS will favor {@linkplain Citations#OGC OGC} definitions of projection
     * and parameter names.</p>
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
    WKT1(Citations.OGC, true, false),

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
    WKT1_COMMON_UNITS(Citations.OGC, true, true),

    /**
     * A special convention for formatting objects as stored internally by Apache SIS.
     * In the majority of cases, the result will be identical to the one we would get using the {@link #WKT2} convention.
     * However in the particular case of map projections, the result may be quite different because of the way
     * SIS separates the linear from the non-linear parameters.
     *
     * <p>This convention is used only for debugging purpose.</p>
     */
    @Debug
    INTERNAL(Citations.OGC, false, false);

    /**
     * The default conventions.
     *
     * @todo Make final after we completed the migration from Geotk.
     */
    static Convention DEFAULT = WKT2;

    /**
     * {@code true} for using WKT 1 syntax, or {@code false} for using WKT 2 syntax.
     */
    final boolean isWKT1;

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
     *
     * @see #getForcedUnit(Class)
     */
    final boolean commonUnits;

    /**
     * The organization, standard or project to look for when fetching Map Projection parameter names.
     * Should be one of the authorities known to {@link org.apache.sis.referencing.operation.provider}.
     */
    private final Citation authority;

    /**
     * Creates a new enumeration value.
     */
    private Convention(final Citation authority, final boolean isWKT1, final boolean commonUnits) {
        this.authority   = authority;
        this.isWKT1      = isWKT1;
        this.commonUnits = commonUnits;
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
        return authority;
    }

    /**
     * If non-null, {@code PRIMEM} and {@code PARAMETER} values shall unconditionally use the returned units.
     * The standard value is {@code null}, which means that units are inferred from the context as required by the
     * <a href="http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html#PRIMEM">WKT specification</a>.
     * However some conventions ignore the above WKT specification and use hard-coded units instead.
     *
     * @param  <T>       The compile-time type specified by the {@code quantity} argument.
     * @param  quantity  The kind of quantity for which to get the unit.
     *                   The most typical value for this argument is <code>{@linkplain Angle}.class</code>.
     * @return The unit to use for the given kind of quantity, or {@code null} for inferring the unit in the standard way.
     */
    @SuppressWarnings("unchecked")
    public <T extends Quantity> Unit<T> getForcedUnit(final Class<T> quantity) {
        if (commonUnits) {
            if (quantity == Angle.class) {
                return (Unit<T>) DEGREE_ANGLE;
            }
        }
        return null;
    }
}
