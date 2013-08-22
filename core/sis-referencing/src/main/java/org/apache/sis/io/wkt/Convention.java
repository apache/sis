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
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.crs.GeographicCRS;
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
 * This enumeration exists as an attempt to address some of the problems documented in the Frank Warmerdam's
 * <a href="http://home.gdal.org/projects/opengis/wktproblems.html">OGC WKT Coordinate System Issues</a> page.
 *
 * <p>The various conventions differ mostly in parameter names, and sometime in WKT syntax.
 * For example the {@linkplain org.apache.sis.referencing.operation.projection.Mercator projection}
 * has a parameter named "<cite>Longitude of natural origin</cite>" by {@linkplain #EPSG},
 * "{@code central_meridian}" by {@linkplain #OGC} and "{@code NatOriginLong}" by {@linkplain #GEOTIFF}.
 * In addition the unit of the prime meridian shall be the angular unit of the enclosing
 * {@linkplain GeographicCRS geographic CRS} according the {@linkplain #OGC} standard,
 * but is restricted to decimal degrees by {@linkplain #ESRI}.
 * Other differences are documented in the javadoc of each enumeration value.</p>
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
     *
     * {@section Spacial case}
     * For {@link GeocentricCRS}, this convention uses the legacy set of Cartesian axes.
     * Those axes were defined in OGC 01-009 as <var>Other</var>,
     * <var>{@linkplain DefaultCoordinateSystemAxis#EASTING Easting}</var> and
     * <var>{@linkplain DefaultCoordinateSystemAxis#NORTHING Northing}</var>
     * in metres, where the "<var>Other</var>" axis is toward prime meridian.
     *
     * @see Citations#OGC
     * @see #toConformCS(CoordinateSystem)
     */
    OGC(Citations.OGC, null, false),

    /**
     * The <a href="http://www.epsg.org">European Petroleum Survey Group</a> convention.
     * This convention uses the most descriptive parameter and projection names.
     *
     * {@section Spacial case}
     * For {@link GeocentricCRS}, this convention uses the new set of Cartesian axes.
     * Those axes are defined in ISO 19111 as
     * <var>{@linkplain DefaultCoordinateSystemAxis#GEOCENTRIC_X Geocentric X}</var>,
     * <var>{@linkplain DefaultCoordinateSystemAxis#GEOCENTRIC_Y Geocentric Y}</var> and
     * <var>{@linkplain DefaultCoordinateSystemAxis#GEOCENTRIC_Z Geocentric Z}</var> in metres.
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
     * This convention is similar to the {@link #OGC} convention except for parameter and projection names.
     *
     * @see Citations#NETCDF
     */
    NETCDF(Citations.NETCDF, null, false),

    /**
     * The <a href="http://www.remotesensing.org/geotiff/geotiff.html">GeoTIFF</a> convention.
     * This convention is similar to the {@link #OGC} convention except for parameter and projection names.
     *
     * @see Citations#GEOTIFF
     */
    GEOTIFF(Citations.GEOTIFF, null, false),

    /**
     * The <a href="http://trac.osgeo.org/proj/">Proj.4</a> convention.
     * This convention uses very short parameter and projection names.
     * Other differences are:
     *
     * <ul>
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
     *
     * @see Formatter#isInternalWKT()
     */
    @Debug
    INTERNAL(Citations.SIS, null, false) {
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
     * @see ReferencingParser#getForcedAngularUnit()
     */
    final Unit<Angle> forcedAngularUnit;

    /**
     * {@code true} if the convention uses US unit names instead of the international names.
     * For example Americans said [@code "meter"} instead of {@code "metre"}.
     */
    final boolean unitUS;

    /**
     * The citation for this enumeration.
     */
    private final Citation citation;

    /**
     * Creates a new enumeration value.
     */
    private Convention(final Citation citation, final Unit<Angle> angularUnit, final boolean unitUS) {
        this.citation = citation;
        this.forcedAngularUnit = angularUnit;
        this.unitUS = unitUS;
    }

    /**
     * Returns the citation for this enumeration.
     * This is one of the constants defined in the {@link Citations} class.
     *
     * @return The citation for this enum.
     *
     * @see WKTFormat#getAuthority()
     */
    public Citation getCitation() {
        return citation;
    }

    /**
     * Returns the convention for the given citation.
     *
     * @param  citation The citation for which to get the convention, or {@code null}.
     * @param  defaultConvention The default convention to return if none where found for
     *         the given citation. May be {@code null}.
     * @return The convention, or {@code null} if no matching convention were found and the
     *         {@code defaultConvention} argument is {@code null}.
     */
    public static Convention forCitation(final Citation citation, final Convention defaultConvention) {
        if (citation != null) {
            for (final Convention candidate : values()) {
                if (Citations.identifierMatches(candidate.getCitation(), citation)) {
                    return candidate;
                }
            }
        }
        return defaultConvention;
    }

    /**
     * Returns the convention for the given identifier.
     *
     * @param  identifier The identifier for which to get the convention, or {@code null}.
     * @param  defaultConvention The default convention to return if none where found for
     *         the given identifier. May be {@code null}.
     * @return The convention, or {@code null} if no matching convention were found and the
     *         {@code defaultConvention} argument is {@code null}.
     */
    public static Convention forIdentifier(final String identifier, final Convention defaultConvention) {
        if (identifier != null) {
            for (final Convention candidate : values()) {
                if (Citations.identifierMatches(candidate.getCitation(), identifier)) {
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
     * <ul>
     *   <li>OGC 01-009: Other,
     *     {@linkplain DefaultCoordinateSystemAxis#EASTING Easting},
     *     {@linkplain DefaultCoordinateSystemAxis#NORTHING Northing}.
     *   </li>
     *   <li>ISO 19111:
     *     {@linkplain DefaultCoordinateSystemAxis#GEOCENTRIC_X Geocentric X},
     *     {@linkplain DefaultCoordinateSystemAxis#GEOCENTRIC_Y Geocentric Y},
     *     {@linkplain DefaultCoordinateSystemAxis#GEOCENTRIC_Z Geocentric Z}.
     *   </li>
     * </ul>
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
