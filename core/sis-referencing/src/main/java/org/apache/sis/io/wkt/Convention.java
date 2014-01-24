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
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.GeocentricCRS;
import org.apache.sis.util.Debug;
import org.apache.sis.metadata.iso.citation.Citations;

import static javax.measure.unit.NonSI.DEGREE_ANGLE;


/**
 * The convention to use for WKT formatting.
 * This enumeration specifies whether to use the <cite>Well Known Text</cite> format defined by ISO 19162
 * (also known as “WKT 2”), or whether to use the format previously defined in OGC 01-009 (referenced as “WKT 1”).
 *
 * {@section WKT 1 variants}
 * The WKT 2 format should be parsed and formatted consistently by all softwares.
 * But the WKT 1 format has been interpreted differently by various implementors. Some of those differences
 * are <a href="http://home.gdal.org/projects/opengis/wktproblems.html">documented by the GDAL project</a>.
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
 * interpretation appears to be in wide use for WKT 1. Apache SIS uses the standard interpretation by default.
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
     * Unless otherwise specified by {@link WKTFormat#setAuthority(Citation)}, when using
     * this convention SIS will favor <a href="http://www.epsg.org">EPSG</a> definitions
     * of projection and parameter names.
     *
     * @see Citations#ISO
     * @see Citations#EPSG
     */
    WKT2(Citations.EPSG, true, false),

    /**
     * The OGC 01-009 format, also known as “WKT 1”.
     * A definition for this format is shown in Extended Backus Naur Form (EBNF)
     * <a href="http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html">on GeoAPI</a>.
     * Unless otherwise specified by {@link WKTFormat#setAuthority(Citation)}, when using
     * this convention SIS will favor OGC definitions of projection and parameter names.
     *
     * <p>Some worthy aspects to note:</p>
     * <ul>
     *   <li>For {@link GeocentricCRS}, this convention uses the legacy set of Cartesian axes.
     *     Those axes were defined in OGC 01-009 as <var>Other</var>, <var>Easting</var> and <var>Northing</var>
     *     in metres, where the "<var>Other</var>" axis is toward prime meridian.</li>
     * </ul>
     *
     * @see Citations#OGC
     */
    WKT1(Citations.OGC, false, false),

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
    WKT1_COMMON_UNITS(Citations.OGC, false, true),

    /**
     * A special convention for formatting objects as stored internally by Apache SIS.
     * In the majority of cases, the result will be identical to the one we would get using the {@link #WKT1} convention.
     * However in the particular case of map projections, the result may be quite different because of the way
     * SIS separates the linear from the non-linear parameters.
     *
     * <p>This convention is used only for debugging purpose.</p>
     */
    @Debug
    INTERNAL(Citations.OGC, true, false) {
        @Override
        public CoordinateSystem toConformCS(final CoordinateSystem cs) {
            return cs; // Prevent any modification on the internal CS.
        }
    };

    /**
     * The default conventions.
     *
     * @todo Make final after we completed the migration from Geotk.
     */
    static Convention DEFAULT = WKT2;

    /**
     * {@code true} for using WKT 2 syntax, or {@code false} for using WKT 1 syntax.
     */
    final boolean isISO;

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
    private Convention(final Citation authority, final boolean isISO, final boolean commonUnits) {
        this.authority   = authority;
        this.isISO       = isISO;
        this.commonUnits = commonUnits;
    }

    /**
     * Returns the default authority to look for when fetching Map Projection parameter names.
     * The value returned by this method can be overwritten by {@link WKTFormat#setAuthority(Citation)}.
     *
     * {@example The following table shows the names given by various organizations or projects for the same projection:
     *
     * <table class="sis">
     *   <tr><th>Authority</th>                 <th>Projection name</th></tr>
     *   <tr><td>{@link Citations#EPSG}</td>    <td>Mercator (variant A)</td></tr>
     *   <tr><td>{@link Citations#OGC}</td>     <td>Mercator_1SP</td></tr>
     *   <tr><td>{@link Citations#GEOTIFF}</td> <td>CT_Mercator</td></tr>
     * </table>}
     *
     * @return The organization, standard or project to look for when fetching Map Projection parameter names.
     *
     * @see WKTFormat#getAuthority()
     */
    public Citation getAuthority() {
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

    /**
     * Makes the given coordinate system conform to this convention. This method is used mostly
     * for converting between the legacy (OGC 01-009) {@link GeocentricCRS} axis directions,
     * and the new (ISO 19111) directions. Those directions are:
     *
     * <table class="sis">
     *   <tr><th>ISO 19111</th>    <th>OGC 01-009</th></tr>
     *   <tr><td>Geocentric X</td> <td>Other</td></tr>
     *   <tr><td>Geocentric Y</td> <td>Easting</td></tr>
     *   <tr><td>Geocentric Z</td> <td>Northing</td></tr>
     * </table>
     *
     * @param  cs The coordinate system.
     * @return A coordinate system equivalent to the given one but with conform axis names,
     *         or the given {@code cs} if no change apply to the given coordinate system.
     */
    public CoordinateSystem toConformCS(CoordinateSystem cs) {
        if (cs instanceof CartesianCS) {
            cs = Legacy.replace((CartesianCS) cs, true);
        }
        return cs;
    }
}
