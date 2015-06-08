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
package org.apache.sis.referencing;

import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.RangeMeaning;
import org.opengis.referencing.crs.GeodeticCRS;
import org.apache.sis.internal.metadata.AxisNames;

import static org.apache.sis.test.Assert.*;


/**
 * Verifies the values of some geodetic objects. Methods in this class ignore most textual properties like remarks,
 * because IOGP allows implementations to modify non-essential properties.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 *
 * @todo Move this class to GeoAPI.
 */
public final strictfp class GeodeticObjectVerifier {
    /**
     * The tolerance threshold for strict comparisons of floating point values.
     */
    private static final double STRICT = 0;

    /**
     * Creates a new test case.
     */
    private GeodeticObjectVerifier() {
    }

    /**
     * Asserts that all {@link GeographicBoundingBox}, if any,
     * {@linkplain #assertIsWorld(GeographicBoundingBox) encompasses the world}.
     *
     * <p><b>Note:</b> a future version of this method may accept other kinds of extent,
     * for example a polygon encompassing the world.</p>
     *
     * @param extent The extent to verify, or {@code null} if none.
     * @param isMandatory {@code true} if an absence of world extent is a failure.
     */
    private static void assertIsWorld(final Extent extent, boolean isMandatory) {
        if (extent != null) {
            for (final GeographicExtent element : extent.getGeographicElements()) {
                if (element instanceof GeographicBoundingBox) {
                    assertIsWorld((GeographicBoundingBox) element);
                    isMandatory = false;
                }
            }
        }
        if (isMandatory) {
            fail("Expected a world extent element.");
        }
    }

    /**
     * Asserts that the given geographic bounding box encompasses the world.
     * This method verifies the following properties:
     *
     * <table class="sis">
     * <caption>Verified properties</caption>
     * <tr><th>Property</th> <th>Expected value</th></tr>
     * <tr><td>{@linkplain GeographicBoundingBox#getInclusion() Inclusion}</td><td>Absent or {@link Boolean#TRUE}</td></tr>
     * <tr><td>{@linkplain GeographicBoundingBox#getWestBoundLongitude() West bound longitude}</td><td>-180</td></tr>
     * <tr><td>{@linkplain GeographicBoundingBox#getEastBoundLongitude() East bound longitude}</td><td>+180</td></tr>
     * <tr><td>{@linkplain GeographicBoundingBox#getSouthBoundLatitude() South bound latitude}</td><td> -90</td></tr>
     * <tr><td>{@linkplain GeographicBoundingBox#getNorthBoundLatitude() North bound latitude}</td><td> +90</td></tr>
     * </table>
     *
     * @param box The geographic bounding box to verify.
     */
    public static void assertIsWorld(final GeographicBoundingBox box) {
        final Boolean inclusion = box.getInclusion();
        if (inclusion != null) {
            assertEquals("inclusion", Boolean.TRUE, inclusion);
        }
        assertEquals("westBoundLongitude", -180, box.getWestBoundLongitude(), STRICT);
        assertEquals("eastBoundLongitude", +180, box.getEastBoundLongitude(), STRICT);
        assertEquals("southBoundLatitude",  -90, box.getSouthBoundLatitude(), STRICT);
        assertEquals("northBoundLatitude",  +90, box.getNorthBoundLatitude(), STRICT);
    }

    /**
     * Asserts that the given prime meridian is the Greenwich one.
     * This method verifies the following properties:
     *
     * <table class="sis">
     * <caption>Verified properties</caption>
     * <tr><th>Property</th> <th>Expected value</th></tr>
     * <tr><td>{@linkplain Identifier#getCode() Code} of the {@linkplain PrimeMeridian#getName() name}</td>
     *     <td>{@code "Greenwich"}</td></tr>
     * <tr><td>{@linkplain PrimeMeridian#getGreenwichLongitude() Greenwich longitude}</td>
     *     <td>0</td></tr>
     * <tr><td>{@linkplain PrimeMeridian#getAngularUnit() Angular unit}</td>
     *     <td>{@link NonSI#DEGREE_ANGLE}</td></tr>
     * </table>
     *
     * @param meridian The prime meridian to verify.
     */
    public static void assertIsGreenwich(final PrimeMeridian meridian) {
        assertEquals("name",               "Greenwich",        meridian.getName().getCode());
        assertEquals("greenwichLongitude", 0,                  meridian.getGreenwichLongitude(), STRICT);
        assertEquals("angularUnit",        NonSI.DEGREE_ANGLE, meridian.getAngularUnit());
    }

    /**
     * Asserts that the given prime meridian is the Paris one.
     * This method verifies the following properties:
     *
     * <table class="sis">
     * <caption>Verified properties</caption>
     * <tr><th>Property</th> <th>Expected value</th></tr>
     * <tr><td>{@linkplain Identifier#getCode() Code} of the {@linkplain PrimeMeridian#getName() name}</td>
     *     <td>{@code "Paris"}</td></tr>
     * <tr><td>{@linkplain PrimeMeridian#getGreenwichLongitude() Greenwich longitude}</td>
     *     <td>2.5969213</td></tr>
     * <tr><td>{@linkplain PrimeMeridian#getAngularUnit() Angular unit}</td>
     *     <td>{@link NonSI#GRADE}</td></tr>
     * </table>
     *
     * @param meridian The prime meridian to verify.
     */
    public static void assertIsParis(final PrimeMeridian meridian) {
        assertEquals("name",               "Paris",     meridian.getName().getCode());
        assertEquals("greenwichLongitude", 2.5969213,   meridian.getGreenwichLongitude(), STRICT);
        assertEquals("angularUnit",        NonSI.GRADE, meridian.getAngularUnit());
    }

    /**
     * Asserts that the given ellipsoid is the WGS 84 one.
     * This method verifies the following properties:
     *
     * <table class="sis">
     * <caption>Verified properties</caption>
     * <tr><th>Property</th> <th>Expected value</th></tr>
     * <tr><td>{@linkplain Identifier#getCode() Code} of the {@linkplain Ellipsoid#getName() name}</td>
     *     <td>{@code "WGS 84"}</td></tr>
     * <tr><td>{@linkplain Ellipsoid#getAxisUnit() Axis unit}</td>
     *     <td>{@link SI#METRE}</td></tr>
     * <tr><td>{@linkplain Ellipsoid#getSemiMajorAxis() Semi-major axis}</td>
     *     <td>6378137</td></tr>
     * <tr><td>{@linkplain Ellipsoid#getSemiMinorAxis() Semi-minor axis}</td>
     *     <td>6356752.314245179 ± 0.001</td></tr>
     * <tr><td>{@linkplain Ellipsoid#getInverseFlattening() Inverse flattening}</td>
     *     <td>298.257223563</td></tr>
     * <tr><td>{@linkplain Ellipsoid#isIvfDefinitive() is IVF definitive}</td>
     *     <td>{@code true}</td></tr>
     * </table>
     *
     * @param ellipsoid The ellipsoid to verify.
     */
    public static void assertIsWGS84(final Ellipsoid ellipsoid) {
        assertEquals("name",              "WGS 84",          ellipsoid.getName().getCode());
        assertEquals("axisUnit",          SI.METRE,          ellipsoid.getAxisUnit());
        assertEquals("semiMajorAxis",     6378137,           ellipsoid.getSemiMajorAxis(),     STRICT);
        assertEquals("semiMinorAxis",     6356752.314245179, ellipsoid.getSemiMinorAxis(),     0.001);
        assertEquals("inverseFlattening", 298.257223563,     ellipsoid.getInverseFlattening(), STRICT);
        assertTrue  ("isIvfDefinitive",                      ellipsoid.isIvfDefinitive());
    }

    /**
     * Asserts that the given datum is the WGS 84 one.
     * This method verifies the following properties:
     *
     * <table class="sis">
     * <caption>Verified properties</caption>
     * <tr><th>Property</th> <th>Expected value</th></tr>
     * <tr><td>{@linkplain Identifier#getCode() Code} of the {@linkplain GeodeticDatum#getName() name}</td>
     *     <td>{@code "World Geodetic System 1984"}</td></tr>
     * <tr><td>{@linkplain GeodeticDatum#getDomainOfValidity() Domain of validity}</td>
     *     <td>{@linkplain #assertIsWorld(GeographicBoundingBox) Is world} or absent</td></tr>
     * <tr><td>{@linkplain GeodeticDatum#getPrimeMeridian() Prime meridian}</td>
     *     <td>{@linkplain #assertIsGreenwich(PrimeMeridian) Is Greenwich}</td></tr>
     * <tr><td>{@linkplain GeodeticDatum#getEllipsoid() Ellipsoid}</td>
     *     <td>{@linkplain #assertIsWGS84(Ellipsoid) Is WGS84}</td></tr>
     * </table>
     *
     * @param datum The datum to verify.
     * @param isExtentMandatory {@code true} if the domain of validity is required to contain an {@code Extent} element
     *        for the world, or {@code false} if optional.
     */
    public static void assertIsWGS84(final GeodeticDatum datum, final boolean isExtentMandatory) {
        assertEquals("name", "World Geodetic System 1984", datum.getName().getCode());
        assertIsWorld    (datum.getDomainOfValidity(), isExtentMandatory);
        assertIsGreenwich(datum.getPrimeMeridian());
        assertIsWGS84    (datum.getEllipsoid());
    }

    /**
     * Asserts that the given CRS is the WGS 84 one.
     * This method verifies the following properties:
     *
     * <table class="sis">
     * <caption>Verified properties</caption>
     * <tr><th>Property</th> <th>Expected value</th></tr>
     * <tr><td>{@linkplain Identifier#getCode() Code} of the {@linkplain GeodeticCRS#getName() name}</td>
     *     <td>{@code "WGS 84"}</td></tr>
     * <tr><td>{@linkplain GeodeticCRS#getDomainOfValidity() Domain of validity}</td>
     *     <td>{@linkplain #assertIsWorld(GeographicBoundingBox) Is world} or absent</td></tr>
     * <tr><td>{@linkplain GeodeticCRS#getDatum() Datum}</td>
     *     <td>{@linkplain #assertIsWGS84(GeodeticDatum, boolean) Is WGS84}</td></tr>
     * <tr><td>{@linkplain GeodeticCRS#getCoordinateSystem() Coordinate system}</td>
     *     <td>{@linkplain #assertIsGeodetic2D(EllipsoidalCS, boolean) Is for a 2D geodetic CRS}</td></tr>
     * </table>
     *
     * @param crs The coordinate reference system to verify.
     * @param isExtentMandatory {@code true} if the CRS and datum domains of validity are required to contain an
     *        {@code Extent} element for the world, or {@code false} if optional.
     * @param isRangeMandatory {@code true} if the coordinate system axes range and range meaning properties
     *        shall be defined, or {@code false} if they are optional.
     */
    public static void assertIsWGS84(final GeodeticCRS crs, final boolean isExtentMandatory, final boolean isRangeMandatory) {
        assertEquals("name", "WGS 84", crs.getName().getCode());
        assertIsWorld(crs.getDomainOfValidity(), isExtentMandatory);
        assertIsWGS84(crs.getDatum(), isExtentMandatory);
        final CoordinateSystem cs = crs.getCoordinateSystem();
        assertInstanceOf("coordinateSystem", EllipsoidalCS.class, cs);
        assertIsGeodetic2D((EllipsoidalCS) cs, isRangeMandatory);
    }

    /**
     * Asserts that the given datum is the Mean Sea Level one.
     * This method verifies the following properties:
     *
     * <table class="sis">
     * <caption>Verified properties</caption>
     * <tr><th>Property</th> <th>Expected value</th></tr>
     * <tr><td>{@linkplain Identifier#getCode() Code} of the {@linkplain GeodeticDatum#getName() name}</td>
     *     <td>{@code "Mean Sea Level"}</td></tr>
     * <tr><td>{@linkplain GeodeticDatum#getDomainOfValidity() Domain of validity}</td>
     *     <td>{@linkplain #assertIsWorld(GeographicBoundingBox) Is world} or absent</td></tr>
     * </table>
     *
     * @param datum The datum to verify.
     * @param isExtentMandatory {@code true} if the domain of validity is required to contain an {@code Extent} element
     *        for the world, or {@code false} if optional.
     */
    public static void assertIsMeanSeaLevel(final VerticalDatum datum, final boolean isExtentMandatory) {
        assertEquals("name", "Mean Sea Level", datum.getName().getCode());
        assertIsWorld(datum.getDomainOfValidity(), isExtentMandatory);
    }

    /**
     * Asserts that the given coordinate system contains the (easting, northing) axes in metres.
     * This method verifies the following properties:
     *
     * <table class="sis">
     * <caption>Verified properties</caption>
     * <tr><th>Property</th> <th colspan="2">Expected value</th></tr>
     * <tr><td>{@linkplain CartesianCS#getDimension() Dimension}</td>
     *     <td colspan="2">2</td></tr>
     * <tr><td>Axes {@linkplain Identifier#getCode() Code} of the {@linkplain GeodeticDatum#getName() name}</td>
     *     <td>{@code "Easting"}</td>
     *     <td>{@code "Northing"}</td></tr>
     * <tr><td>Axes {@linkplain CoordinateSystemAxis#getAbbreviation() abbreviation}</td>
     *     <td>{@code "E"}</td>
     *     <td>{@code "N"}</td></tr>
     * <tr><td>Axes {@linkplain CoordinateSystemAxis#getDirection() direction}</td>
     *     <td>{@link AxisDirection#EAST EAST}</td>
     *     <td>{@link AxisDirection#NORTH NORTH}</td></tr>
     * <tr><td>Axes {@linkplain CoordinateSystemAxis#getUnit() units}</td>
     *     <td>{@link SI#METRE}</td>
     *     <td>{@link SI#METRE}</td></tr>
     * <tr><td>Axes range</td>
     *     <td>[-∞ … ∞]</td>
     *     <td>[-∞ … ∞]</td></tr>
     * <tr><td>Axes range meaning</td>
     *     <td>{@code null}</td>
     *     <td>{@code null}</td>
     * </table>
     *
     * @param cs The coordinate system to verify.
     */
    public static void assertIsProjected2D(final CartesianCS cs) {
        assertEquals("dimension", 2, cs.getDimension());
        final CoordinateSystemAxis E = cs.getAxis(0);
        final CoordinateSystemAxis N = cs.getAxis(1);
        assertNotNull("axis", E);
        assertNotNull("axis", N);
        assertEquals("axis[0].name",         AxisNames.EASTING,   E.getName().getCode());
        assertEquals("axis[1].name",         AxisNames.NORTHING,  N.getName().getCode());
        assertEquals("axis[0].abbreviation", "E",                 E.getAbbreviation());
        assertEquals("axis[1].abbreviation", "N",                 N.getAbbreviation());
        assertEquals("axis[0].direction",    AxisDirection.EAST,  E.getDirection());
        assertEquals("axis[1].direction",    AxisDirection.NORTH, N.getDirection());
        assertEquals("axis[0].unit",         SI.METRE,            E.getUnit());
        assertEquals("axis[1].unit",         SI.METRE,            N.getUnit());
        verifyRange(E, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null, true);
        verifyRange(N, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null, true);
    }

    /**
     * Asserts that the given coordinate system contains the geodetic (latitude, longitude) axes in degrees.
     * This method verifies the following properties:
     *
     * <table class="sis">
     * <caption>Verified properties</caption>
     * <tr><th>Property</th> <th colspan="2">Expected value</th></tr>
     * <tr><td>{@linkplain EllipsoidalCS#getDimension() Dimension}</td>
     *     <td colspan="2">2</td></tr>
     * <tr><td>Axes {@linkplain Identifier#getCode() Code} of the {@linkplain GeodeticDatum#getName() name}</td>
     *     <td>{@code "Geodetic latitude"}</td>
     *     <td>{@code "Geodetic longitude"}</td></tr>
     * <tr><td>Axes {@linkplain CoordinateSystemAxis#getDirection() direction}</td>
     *     <td>{@link AxisDirection#NORTH NORTH}</td>
     *     <td>{@link AxisDirection#EAST EAST}</td></tr>
     * <tr><td>Axes {@linkplain CoordinateSystemAxis#getUnit() units}</td>
     *     <td>{@link NonSI#DEGREE_ANGLE}</td>
     *     <td>{@link NonSI#DEGREE_ANGLE}</td></tr>
     * <tr><td>Axes range</td>
     *     <td>[-90 … 90] (see below)</td>
     *     <td>[-180 … 180] (see below)</td></tr>
     * <tr><td>Axes range meaning</td>
     *     <td>{@link RangeMeaning#EXACT} or missing</td>
     *     <td>{@link RangeMeaning#WRAPAROUND} or missing</td></tr>
     * </table>
     *
     * <b>Notes:</b>
     * <ul>
     *   <li>The axes range may be missing if and only if the range meaning is also missing.</li>
     *   <li>This method does not verify {@linkplain CoordinateSystemAxis#getAbbreviation() abbreviations}
     *       because the classical symbols (φ,λ) are often replaced by (lat,long).</li>
     * </ul>
     *
     * @param cs The coordinate system to verify.
     * @param isRangeMandatory {@code true} if the axes range and range meaning properties shall be defined,
     *        or {@code false} if they are optional.
     */
    public static void assertIsGeodetic2D(final EllipsoidalCS cs, final boolean isRangeMandatory) {
        assertEquals("dimension", 2, cs.getDimension());
        final CoordinateSystemAxis latitude  = cs.getAxis(0);
        final CoordinateSystemAxis longitude = cs.getAxis(1);
        assertNotNull("axis", latitude);
        assertNotNull("axis", longitude);
        assertEquals("axis[0].name",       AxisNames.GEODETIC_LATITUDE,  latitude .getName().getCode());
        assertEquals("axis[1].name",       AxisNames.GEODETIC_LONGITUDE, longitude.getName().getCode());
        assertEquals("axis[0].direction",  AxisDirection.NORTH,          latitude .getDirection());
        assertEquals("axis[1].direction",  AxisDirection.EAST,           longitude.getDirection());
        assertEquals("axis[0].unit",       NonSI.DEGREE_ANGLE,           latitude .getUnit());
        assertEquals("axis[1].unit",       NonSI.DEGREE_ANGLE,           longitude.getUnit());
        verifyRange(latitude,   -90,  +90, RangeMeaning.EXACT,           isRangeMandatory);
        verifyRange(longitude, -180, +180, RangeMeaning.WRAPAROUND,      isRangeMandatory);
    }

    /**
     * Asserts that the axis range is either fully missing, or defined to exactly the given properties.
     */
    private static void verifyRange(final CoordinateSystemAxis axis,
            final double min, final double max, final RangeMeaning expected, final boolean isMandatory)
    {
        final double       minimumValue = axis.getMinimumValue();
        final double       maximumValue = axis.getMaximumValue();
        final RangeMeaning rangeMeaning = axis.getRangeMeaning();
        if (isMandatory || rangeMeaning != null ||
                minimumValue != Double.NEGATIVE_INFINITY ||
                maximumValue != Double.POSITIVE_INFINITY)
        {
            assertEquals("axis.minimumValue", min, minimumValue, STRICT);
            assertEquals("axis.maximumValue", max, maximumValue, STRICT);
            assertEquals("axis.rangeMeaning", expected, rangeMeaning);
        }
    }
}
