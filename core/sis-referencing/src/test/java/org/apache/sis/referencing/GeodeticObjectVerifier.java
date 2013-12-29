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
import org.opengis.metadata.extent.Extent;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.datum.VerticalDatum;

import static org.apache.sis.test.Assert.*;


/**
 * Verifies the values of some geodetic objects. Methods in this class ignore most textual properties like remarks,
 * because OGP allows implementations to modify non-essential properties.
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
     * The tolerance factor for strict comparisons of floating point values.
     */
    private static final double STRICT = 0.0;

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
     */
    private static void assertIsWorld(final Extent extent) {
        if (extent != null) {
            for (final GeographicExtent element : extent.getGeographicElements()) {
                if (element instanceof GeographicBoundingBox) {
                    assertIsWorld((GeographicBoundingBox) element);
                }
            }
        }
    }

    /**
     * Asserts that the given geographic bounding box encompasses the world.
     * This method verifies the following properties:
     *
     * <table class="sis">
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
     * <tr><th>Property</th> <th>Expected value</th></tr>
     * <tr><td>{@linkplain ReferenceIdentifier#getCode() Code} of the {@linkplain PrimeMeridian#getName() name}</td>
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
     * <tr><th>Property</th> <th>Expected value</th></tr>
     * <tr><td>{@linkplain ReferenceIdentifier#getCode() Code} of the {@linkplain PrimeMeridian#getName() name}</td>
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
     * <tr><th>Property</th> <th>Expected value</th></tr>
     * <tr><td>{@linkplain ReferenceIdentifier#getCode() Code} of the {@linkplain Ellipsoid#getName() name}</td>
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
     * <tr><th>Property</th> <th>Expected value</th></tr>
     * <tr><td>{@linkplain ReferenceIdentifier#getCode() Code} of the {@linkplain GeodeticDatum#getName() name}</td>
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
     */
    public static void assertIsWGS84(final GeodeticDatum datum) {
        assertEquals("name", "World Geodetic System 1984", datum.getName().getCode());
        assertIsWorld    (datum.getDomainOfValidity());
        assertIsGreenwich(datum.getPrimeMeridian());
        assertIsWGS84    (datum.getEllipsoid());
    }

    /**
     * Asserts that the given datum is the Mean Sea Level one.
     * This method verifies the following properties:
     *
     * <table class="sis">
     * <tr><th>Property</th> <th>Expected value</th></tr>
     * <tr><td>{@linkplain ReferenceIdentifier#getCode() Code} of the {@linkplain GeodeticDatum#getName() name}</td>
     *     <td>{@code "Mean Sea Level"}</td></tr>
     * <tr><td>{@linkplain GeodeticDatum#getDomainOfValidity() Domain of validity}</td>
     *     <td>{@linkplain #assertIsWorld(GeographicBoundingBox) Is world} or absent</td></tr>
     * </table>
     *
     * @param datum The datum to verify.
     */
    public static void assertIsMeanSeaLevel(final VerticalDatum datum) {
        assertEquals("name", "Mean Sea Level", datum.getName().getCode());
        assertIsWorld(datum.getDomainOfValidity());
    }
}
