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

import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Quantity;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.crs.GeocentricCRS;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.CoordinateOperation;
import org.apache.sis.util.Debug;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.cs.DefaultCartesianCS;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;

import static javax.measure.unit.NonSI.DEGREE_ANGLE;


/**
 * The convention to use for WKT formatting.
 * This enumeration attempts to address some of the variability documented in the Frank Warmerdam's
 * <a href="http://home.gdal.org/projects/opengis/wktproblems.html">OGC WKT Coordinate System Issues</a> page.
 * The various conventions enumerated in this class differ mostly in:
 *
 * <ul>
 *   <li><em>Parameter names</em> - for example a parameter named "<cite>Longitude of natural origin</cite>"
 *       according {@linkplain #EPSG} may be named "{@code central_meridian}" according {@linkplain #OGC}
 *       and "{@code NatOriginLong}" according {@linkplain #GEOTIFF GeoTIFF}.</li>
 *   <li><em>WKT syntax</em> - for example {@linkplain #ORACLE Oracle} does not enclose Bursa-Wolf parameters in a
 *       {@code TOWGS84[…]} element.</li>
 *   <li><em>Unit of measurement</em> - for example the unit of the Prime Meridian shall be the angular unit of the
 *       enclosing Geographic CRS according the {@linkplain #OGC} standard, but is restricted to decimal degrees by
 *       {@linkplain #ESRI}.</li>
 * </ul>
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
     * The <a href="http://www.opengeospatial.org">Open Geospatial consortium</a> convention.
     * This is the default convention for all WKT formatting in the Apache SIS library.
     * Some worthy aspects to note:
     *
     * <ul>
     *   <li>For {@link GeocentricCRS}, this convention uses the legacy set of Cartesian axes.
     *     Those axes were defined in OGC 01-009 as <var>Other</var>,
     *     <var>{@linkplain DefaultCoordinateSystemAxis#EASTING Easting}</var> and
     *     <var>{@linkplain DefaultCoordinateSystemAxis#NORTHING Northing}</var>
     *     in metres, where the "<var>Other</var>" axis is toward prime meridian.</li>
     * </ul>
     *
     * @see Citations#OGC
     * @see #toConformCS(CoordinateSystem)
     */
    OGC(Citations.OGC, null, false),

    /**
     * The <a href="http://www.epsg.org">European Petroleum Survey Group</a> convention.
     * This convention uses the most descriptive parameter and projection names.
     * Some worthy aspects to note:
     *
     * <ul>
     *   <li>For {@link GeocentricCRS}, this convention uses the new set of Cartesian axes.
     *     Those axes are defined in ISO 19111 as
     *     <var>{@linkplain DefaultCoordinateSystemAxis#GEOCENTRIC_X Geocentric X}</var>,
     *     <var>{@linkplain DefaultCoordinateSystemAxis#GEOCENTRIC_Y Geocentric Y}</var> and
     *     <var>{@linkplain DefaultCoordinateSystemAxis#GEOCENTRIC_Z Geocentric Z}</var> in metres.</li>
     * </ul>
     *
     * @see Citations#EPSG
     * @see #toConformCS(CoordinateSystem)
     */
    EPSG(Citations.EPSG, null, false) {
        @Override
        public CoordinateSystem toConformCS(CoordinateSystem cs) {
            if (cs instanceof CartesianCS) {
                cs = replace((CartesianCS) cs, false);
            }
            return cs;
        }
    },

    /**
     * The <a href="http://www.esri.com">ESRI</a> convention.
     * This convention is similar to the {@link #OGC} convention except in the following aspects:
     *
     * <ul>
     *   <li>The angular units of {@code PRIMEM} and {@code PARAMETER} elements are always degrees,
     *       no matter the units of the enclosing {@code GEOGCS} element.</li>
     *   <li>The {@code AXIS} elements are ignored at parsing time.</li>
     *   <li>Unit names use American spelling instead than the international ones
     *       (e.g. "<cite>meter</cite>" instead than "<cite>metre</cite>").</li>
     *   <li>At parsing time, the {@code AXIS} elements are ignored.</li>
     * </ul>
     *
     * @see Citations#ESRI
     */
    ESRI(Citations.ESRI, DEGREE_ANGLE, true),

    /**
     * The <a href="http://www.oracle.com">Oracle</a> convention.
     * This convention is similar to the {@link #OGC} convention except in the following aspects:
     *
     * <ul>
     *   <li>The Bursa-Wolf parameters are inserted straight into the {@code DATUM} element,
     *       without enclosing them in a {@code TOWGS84} element.</li>
     *   <li>The {@code PROJECTION} names are {@linkplain CoordinateOperation Coordinate Operation}
     *       names instead than {@linkplain OperationMethod Operation Method} names.</li>
     *   <li>Unit names use American spelling instead than the international ones
     *       (e.g. "<cite>meter</cite>" instead than "<cite>metre</cite>").</li>
     * </ul>
     *
     * @see Citations#ORACLE
     */
    ORACLE(Citations.ORACLE, null, true),

    /**
     * The <a href="http://www.unidata.ucar.edu/software/netcdf-java">NetCDF</a> convention.
     * This convention is similar to the {@link #OGC} convention except in the following aspects:
     *
     * <ul>
     *   <li>Parameter and projection names.</li>
     * </ul>
     *
     * @see Citations#NETCDF
     */
    NETCDF(Citations.NETCDF, null, false),

    /**
     * The <a href="http://www.remotesensing.org/geotiff/geotiff.html">GeoTIFF</a> convention.
     * This convention is similar to the {@link #OGC} convention except in the following aspects:
     *
     * <ul>
     *   <li>Parameter and projection names.</li>
     * </ul>
     *
     * @see Citations#GEOTIFF
     */
    GEOTIFF(Citations.GEOTIFF, null, false),

    /**
     * The <a href="http://trac.osgeo.org/proj/">Proj.4</a> convention.
     * This convention is similar to the {@link #OGC} convention except in the following aspects:
     *
     * <ul>
     *   <li>Very short parameter and projection names.</li>
     *   <li>The angular units of {@code PRIMEM} and {@code PARAMETER} elements are always degrees,
     *       no matter the units of the enclosing {@code GEOGCS} element.</li>
     * </ul>
     *
     * @see Citations#PROJ4
     */
    PROJ4(Citations.PROJ4, DEGREE_ANGLE, false),

    /**
     * A special convention for formatting objects as stored internally by Apache SIS.
     * In the majority of cases, the result will be identical to the one we would get using the {@link #OGC} convention.
     * However in the particular case of map projections, the result may be quite different because of the way
     * SIS separates the linear from the non-linear parameters.
     *
     * <p>This convention is used only for debugging purpose.</p>
     */
    @Debug
    INTERNAL(Citations.OGC, null, false) {
        /**
         * Declares publicly that this convention is defined by Apache SIS, despite the
         * package-private {@link #authority} field being set to OGC for {@link Formatter} needs.
         */
        @Override
        public Citation getAuthority() {
            return Citations.SIS;
        }

        @Override
        public CoordinateSystem toConformCS(final CoordinateSystem cs) {
            return cs; // Prevent any modification on the internal CS.
        }
    };

    /**
     * A three-dimensional Cartesian CS with the legacy set of geocentric axes.
     * Those axes were defined in the OGC 01-009 specification as <var>Other</var>,
     * <var>{@linkplain DefaultCoordinateSystemAxis#EASTING Easting}</var>,
     * <var>{@linkplain DefaultCoordinateSystemAxis#NORTHING Northing}</var>
     * in metres, where the "Other" axis is toward prime meridian.
     */
    private static final DefaultCartesianCS LEGACY = new DefaultCartesianCS("Legacy",
            new DefaultCoordinateSystemAxis("X", AxisDirection.OTHER, SI.METRE),
            new DefaultCoordinateSystemAxis("Y", AxisDirection.EAST,  SI.METRE),
            new DefaultCoordinateSystemAxis("Z", AxisDirection.NORTH, SI.METRE));

    /**
     * If non-null, forces {@code PRIMEM} and {@code PARAMETER} angular units to this field
     * value instead than inferring it from the context. The standard value is {@code null},
     * which means that the angular units are inferred from the context as required by the
     * <a href="http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html#PRIMEM">WKT specification</a>.
     *
     * @see #getForcedUnit(Class)
     */
    final Unit<Angle> forcedAngularUnit;

    /**
     * {@code true} if the convention uses US unit names instead of the international names.
     * For example Americans said [@code "meter"} instead of {@code "metre"}.
     */
    final boolean unitUS;

    /**
     * The organization, standard or project to use for fetching Map Projection parameter names.
     * Shall be one of the authorities known to {@link org.apache.sis.referencing.operation.provider}.
     */
    final Citation authority;

    /**
     * Creates a new enumeration value.
     */
    private Convention(final Citation authority, final Unit<Angle> angularUnit, final boolean unitUS) {
        this.authority         = authority;
        this.forcedAngularUnit = angularUnit;
        this.unitUS            = unitUS;
    }

    /**
     * Returns the citation for the organization, standard of project that defines this convention.
     *
     * @return The organization, standard or project that defines this convention.
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
        if (quantity == Angle.class) {
            return (Unit) forcedAngularUnit;
        }
        return null;
    }

    /**
     * Returns the convention for the organization, standard or project specified by the given citation.
     *
     * @param  authority The organization, standard or project for which to get the convention, or {@code null}.
     * @param  defaultConvention The default convention to return if none where found for the given citation.
     * @return The convention, or {@code null} if no matching convention were found and the
     *         {@code defaultConvention} argument is {@code null}.
     */
    public static Convention forCitation(final Citation authority, final Convention defaultConvention) {
        if (authority != null) {
            for (final Convention candidate : values()) {
                if (Citations.identifierMatches(candidate.getAuthority(), authority)) {
                    return candidate;
                }
            }
        }
        return defaultConvention;
    }

    /**
     * Returns the convention for the organization, standard or project specified by the given identifier.
     *
     * @param  authority The organization, standard or project for which to get the convention, or {@code null}.
     * @param  defaultConvention The default convention to return if none where found for the given identifier.
     * @return The convention, or {@code null} if no matching convention were found and the
     *         {@code defaultConvention} argument is {@code null}.
     */
    public static Convention forIdentifier(final String authority, final Convention defaultConvention) {
        if (authority != null) {
            for (final Convention candidate : values()) {
                if (Citations.identifierMatches(candidate.getAuthority(), authority)) {
                    return candidate;
                }
            }
        }
        return defaultConvention;
    }

    /**
     * Makes the given coordinate system conform to this convention. This method is used mostly
     * for converting between the legacy (OGC 01-009) {@link GeocentricCRS} axis directions,
     * and the new (ISO 19111) directions. Those directions are:
     *
     * <table class="sis">
     * <tr>
     *   <th>ISO 19111</th>
     *   <th>OGC 01-009</th>
     * </tr><tr>
     *   <td>{@linkplain DefaultCoordinateSystemAxis#GEOCENTRIC_X Geocentric X}</td>
     *   <td>Other</td>
     * </tr><tr>
     *   <td>{@linkplain DefaultCoordinateSystemAxis#GEOCENTRIC_Y Geocentric Y}</td>
     *   <td>{@linkplain DefaultCoordinateSystemAxis#EASTING Easting}</td>
     * </tr><tr>
     *   <td>{@linkplain DefaultCoordinateSystemAxis#GEOCENTRIC_Z Geocentric Z}</td>
     *   <td>{@linkplain DefaultCoordinateSystemAxis#NORTHING Northing}</td>
     * </tr></table>
     *
     * @param  cs The coordinate system.
     * @return A coordinate system equivalent to the given one but with conform axis names,
     *         or the given {@code cs} if no change apply to the given coordinate system.
     *
     * @see #OGC
     * @see #EPSG
     */
    public CoordinateSystem toConformCS(CoordinateSystem cs) {
        if (cs instanceof CartesianCS) {
            cs = replace((CartesianCS) cs, true);
        }
        return cs;
    }

    /**
     * Returns the axes to use instead of the ones in the given coordinate system.
     * If the coordinate system axes should be used as-is, returns {@code cs}.
     *
     * @param  cs The coordinate system for which to compare the axis directions.
     * @param  legacy {@code true} for replacing ISO directions by the legacy ones,
     *         or {@code false} for the other way around.
     * @return The axes to use instead of the ones in the given CS,
     *         or {@code cs} if the CS axes should be used as-is.
     */
    static CartesianCS replace(final CartesianCS cs, final boolean legacy) {
        final CartesianCS check = legacy ? DefaultCartesianCS.GEOCENTRIC : LEGACY;
        final int dimension = check.getDimension();
        if (cs.getDimension() != dimension) {
            return cs;
        }
        for (int i=0; i<dimension; i++) {
            if (!cs.getAxis(i).getDirection().equals(check.getAxis(i).getDirection())) {
                return cs;
            }
        }
        return legacy ? LEGACY : DefaultCartesianCS.GEOCENTRIC;
    }
}
