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
package org.apache.sis.referencing.crs;

import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.crs.GeographicCRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.io.wkt.Convention;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.opengis.test.Validators;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertEqualsIgnoreMetadata;
import static org.apache.sis.referencing.Assertions.assertWktEquals;
import static org.apache.sis.test.TestUtilities.getSingleton;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.Identifier;


/**
 * Tests the {@link DefaultGeographicCRS} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DefaultGeographicCRSTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultGeographicCRSTest() {
    }

    /**
     * Tests the {@link DefaultGeographicCRS#forConvention(AxesConvention)} method
     * for {@link AxesConvention#POSITIVE_RANGE}.
     */
    @Test
    public void testShiftLongitudeRange() {
        final DefaultGeographicCRS crs = HardCodedCRS.WGS84_3D;
        CoordinateSystemAxis axis = crs.getCoordinateSystem().getAxis(0);
        assertEquals(-180.0, axis.getMinimumValue());
        assertEquals(+180.0, axis.getMaximumValue());

        assertSame(crs, crs.forConvention(AxesConvention.RIGHT_HANDED), "Expected a no-op.");
        final DefaultGeographicCRS shifted =  crs.forConvention(AxesConvention.POSITIVE_RANGE);
        assertNotSame(crs, shifted, "Expected a new CRS.");
        Validators.validate(shifted);

        axis = shifted.getCoordinateSystem().getAxis(0);
        assertEquals(  0.0, axis.getMinimumValue());
        assertEquals(360.0, axis.getMaximumValue());
        assertSame(shifted, shifted.forConvention(AxesConvention.POSITIVE_RANGE), "Expected a no-op.");
        assertSame(shifted,     crs.forConvention(AxesConvention.POSITIVE_RANGE), "Expected cached instance.");
    }

    /**
     * Tests the {@link DefaultGeographicCRS#forConvention(AxesConvention)} method
     * for {@link AxesConvention#DISPLAY_ORIENTED}.
     */
    @Test
    public void testConventionalOrientation() {
        final DefaultGeographicCRS crs = DefaultGeographicCRS.castOrCopy(CommonCRS.WGS84.geographic3D());
        final DefaultGeographicCRS normalized = crs.forConvention(AxesConvention.DISPLAY_ORIENTED);
        assertNotSame(crs, normalized);
        final EllipsoidalCS cs = normalized.getCoordinateSystem();
        final EllipsoidalCS ref = crs.getCoordinateSystem();
        assertEqualsIgnoreMetadata(ref.getAxis(1), cs.getAxis(0));      // EPSG codes differ because of different axis order.
        assertEqualsIgnoreMetadata(ref.getAxis(0), cs.getAxis(1));
        assertEqualsIgnoreMetadata(ref.getAxis(2), cs.getAxis(2));
    }

    /**
     * Verifies the {@link CommonCRS#WGS84} identifiers in both normalized and unnormalized CRS.
     * The intent is actually to test the replacement of {@code "EPSG:4326"} by {@code "CRS:84"}.
     */
    @Test
    public void testIdentifiers() {
        GeographicCRS crs = CommonCRS.WGS72.geographic();
        Identifier identifier = getSingleton(crs.getIdentifiers());
        assertEquals("EPSG", identifier.getCodeSpace());
        assertEquals("4322", identifier.getCode());

        crs = CommonCRS.WGS72.normalizedGeographic();
        assertTrue(crs.getIdentifiers().isEmpty());

        crs = CommonCRS.WGS84.geographic();
        identifier = getSingleton(crs.getIdentifiers());
        assertEquals("EPSG", identifier.getCodeSpace());
        assertEquals("4326", identifier.getCode());

        crs = CommonCRS.WGS84.normalizedGeographic();
        identifier = getSingleton(crs.getIdentifiers());
        assertEquals("CRS", identifier.getCodeSpace());
        assertEquals("84",  identifier.getCode());

        crs = CommonCRS.NAD83.geographic();
        identifier = getSingleton(crs.getIdentifiers());
        assertEquals("EPSG", identifier.getCodeSpace());
        assertEquals("4269", identifier.getCode());

        crs = CommonCRS.NAD83.normalizedGeographic();
        identifier = getSingleton(crs.getIdentifiers());
        assertEquals("CRS", identifier.getCodeSpace());
        assertEquals("83",  identifier.getCode());

        crs = CommonCRS.NAD27.geographic();
        identifier = getSingleton(crs.getIdentifiers());
        assertEquals("EPSG", identifier.getCodeSpace());
        assertEquals("4267", identifier.getCode());

        crs = CommonCRS.NAD27.normalizedGeographic();
        identifier = getSingleton(crs.getIdentifiers());
        assertEquals("CRS", identifier.getCodeSpace());
        assertEquals("27",  identifier.getCode());
    }

    /**
     * Tests WKT 1 formatting.
     */
    @Test
    public void testWKT1() {
        assertWktEquals(Convention.WKT1,
                "GEOGCS[“WGS 84”,\n" +
                "  DATUM[“World Geodetic System 1984”,\n" +
                "    SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
                "    PRIMEM[“Greenwich”, 0.0],\n" +
                "  UNIT[“degree”, 0.017453292519943295],\n" +
                "  AXIS[“Longitude”, EAST],\n" +
                "  AXIS[“Latitude”, NORTH]]",
                HardCodedCRS.WGS84);
    }

    /**
     * Tests WKT 2 formatting.
     */
    @Test
    public void testWKT2() {
        assertWktEquals(Convention.WKT2_2015,
                "GEODCRS[“WGS 84”,\n" +
                "  DATUM[“World Geodetic System 1984”,\n" +
                "    ELLIPSOID[“WGS84”, 6378137.0, 298.257223563, LENGTHUNIT[“metre”, 1]]],\n" +
                "    PRIMEM[“Greenwich”, 0.0, ANGLEUNIT[“degree”, 0.017453292519943295]],\n" +
                "  CS[ellipsoidal, 2],\n" +
                "    AXIS[“Longitude (L)”, east, ORDER[1]],\n" +
                "    AXIS[“Latitude (B)”, north, ORDER[2]],\n" +
                "    ANGLEUNIT[“degree”, 0.017453292519943295],\n" +
                "  AREA[“World”],\n" +
                "  BBOX[-90.00, -180.00, 90.00, 180.00]]",
                HardCodedCRS.WGS84);
    }

    /**
     * Tests WKT 2 formatting of a three-dimensional CRS.
     *
     * <p>This CRS used in this test is equivalent to {@code EPSG:4979} except for axis order,
     * since EPSG puts latitude before longitude.</p>
     *
     * @see #testWKT1_For3D()
     */
    @Test
    public void testWKT2_For3D() {
        assertWktEquals(Convention.WKT2_2015,
                "GEODCRS[“WGS 84 (3D)”,\n" +
                "  DATUM[“World Geodetic System 1984”,\n" +
                "    ELLIPSOID[“WGS84”, 6378137.0, 298.257223563, LENGTHUNIT[“metre”, 1]]],\n" +
                "    PRIMEM[“Greenwich”, 0.0, ANGLEUNIT[“degree”, 0.017453292519943295]],\n" +
                "  CS[ellipsoidal, 3],\n" +
                "    AXIS[“Longitude (L)”, east, ORDER[1], ANGLEUNIT[“degree”, 0.017453292519943295]],\n" +
                "    AXIS[“Latitude (B)”, north, ORDER[2], ANGLEUNIT[“degree”, 0.017453292519943295]],\n" +
                "    AXIS[“Ellipsoidal height (h)”, up, ORDER[3], LENGTHUNIT[“metre”, 1]],\n" +
                "  AREA[“World”],\n" +
                "  BBOX[-90.00, -180.00, 90.00, 180.00]]",
                HardCodedCRS.WGS84_3D);
    }

    /**
     * Tests WKT 2 simplified formatting.
     */
    @Test
    public void testWKT2_Simplified() {
        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "GeographicCRS[“WGS 84”,\n" +
                "  Datum[“World Geodetic System 1984”,\n" +
                "    Ellipsoid[“WGS84”, 6378137.0, 298.257223563]],\n" +
                "  CS[ellipsoidal, 2],\n" +
                "    Axis[“Longitude (L)”, east],\n" +
                "    Axis[“Latitude (B)”, north],\n" +
                "    Unit[“degree”, 0.017453292519943295],\n" +
                "  Usage[\n" +
                "    Area[“World”],\n" +
                "    BBox[-90.00, -180.00, 90.00, 180.00]]]",
                HardCodedCRS.WGS84);
    }

    /**
     * Tests WKT 2 internal formatting.
     */
    @Test
    public void testWKT2_Internal() {
        assertWktEquals(Convention.INTERNAL,
                "GeographicCRS[“WGS 84”,\n" +
                "  Datum[“World Geodetic System 1984”,\n" +
                "    Ellipsoid[“WGS84”, 6378137.0, 298.257223563],\n" +
                "    Usage[\n" +
                "      Scope[“Satellite navigation.”]],\n" +
                "    Id[“EPSG”, 6326]],\n" +
                "    PrimeMeridian[“Greenwich”, 0.0, Id[“EPSG”, 8901]],\n" +
                "  CS[ellipsoidal, 2],\n" +
                "    Axis[“Geodetic longitude (λ)”, east],\n" +
                "    Axis[“Geodetic latitude (φ)”, north],\n" +
                "    Unit[“degree”, 0.017453292519943295, Id[“EPSG”, 9102]],\n" +
                "  Usage[\n" +
                "    Area[“World”],\n" +
                "    BBox[-90.00, -180.00, 90.00, 180.00]]]",
                HardCodedCRS.WGS84);
    }

    /**
     * Tests WKT 2 formatting of a CRS using a prime meridian other than Greenwich.
     *
     * <p>This CRS used in this test is equivalent to {@code EPSG:4807} except for axis order,
     * since EPSG defines (<var>latitude</var>, <var>longitude</var>) in grads.</p>
     */
    @Test
    public void testWKT2_ForNonGreenwich() {
        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "GeographicCRS[“NTF (Paris)”,\n" +
                "  Datum[“Nouvelle Triangulation Francaise”,\n" +           // Formatter should replace "ç" by "c".
                "    Ellipsoid[“NTF”, 6378249.2, 293.4660212936269]],\n" +
                "    PrimeMeridian[“Paris”, 2.5969213, Unit[“grad”, 0.015707963267948967]],\n" +
                "  CS[ellipsoidal, 2],\n" +
                "    Axis[“Longitude (L)”, east, AxisMinValue[-200.0], AxisMaxValue[200.0], RangeMeaning[wraparound]],\n" +
                "    Axis[“Latitude (B)”, north, AxisMinValue[-100.0], AxisMaxValue[100.0], RangeMeaning[exact]],\n" +
                "    Unit[“grad”, 0.015707963267948967]]",
                HardCodedCRS.NTF);
    }

    /**
     * Tests WKT 1 formatting on a CRS using a prime meridian other than Greenwich.
     *
     * <p>This CRS used in this test is equivalent to {@code EPSG:4807} except for axis order,
     * since EPSG defines (<var>latitude</var>, <var>longitude</var>) in grads.</p>
     */
    @Test
    public void testWKT1_ForNonGreenwich() {
        assertWktEquals(Convention.WKT1,
                "GEOGCS[“NTF (Paris)”,\n" +
                "  DATUM[“Nouvelle Triangulation Francaise”,\n" +   // Formatter should replace "ç" by "c".
                "    SPHEROID[“NTF”, 6378249.2, 293.4660212936269]],\n" +
                "    PRIMEM[“Paris”, 2.5969213],\n" +
                "  UNIT[“grad”, 0.015707963267948967],\n" +
                "  AXIS[“Longitude”, EAST],\n" +
                "  AXIS[“Latitude”, NORTH]]",
                HardCodedCRS.NTF);
    }

    /**
     * Tests WKT 1 formatting using {@link Convention#WKT1_COMMON_UNITS}. That convention ignores the unit of
     * measurement in {@code PRIMEM} element, and rather unconditionally interpret the angle unit as degrees.
     * This is a violation of OGC 01-009 and ISO 19162 standards, but is required for compatibility with GDAL.
     */
    @Test
    public void testWKT1_WithCommonUnits() {
        assertWktEquals(Convention.WKT1_COMMON_UNITS,
                "GEOGCS[“NTF (Paris)”,\n" +
                "  DATUM[“Nouvelle Triangulation Francaise”,\n" +   // Formatter should replace "ç" by "c".
                "    SPHEROID[“NTF”, 6378249.2, 293.4660212936269]],\n" +
                "    PRIMEM[“Paris”, 2.33722917],\n" +              // Would be 2.5969213 in standard-compliant WKT.
                "  UNIT[“grad”, 0.015707963267948967],\n" +
                "  AXIS[“Longitude”, EAST],\n" +
                "  AXIS[“Latitude”, NORTH]]",
                HardCodedCRS.NTF);
    }

    /**
     * Tests WKT 1 formatting of a three-dimensional CRS. Such CRS cannot be represented directly in WKT 1 format.
     * Consequently, the formatter will need to split the three-dimensional geographic CRS into a two-dimensional
     * geographic CRS followed by an ellipsoidal height. Such construction is illegal according ISO 19111, so this
     * split shall be done on-the-fly only for formatting purpose.
     *
     * @see #testWKT2_For3D()
     * @see <a href="https://issues.apache.org/jira/browse/SIS-317">SIS-317</a>
     */
    @Test
    public void testWKT1_For3D() {
        assertWktEquals(Convention.WKT1,
                "COMPD_CS[“WGS 84 (3D)”,\n" +
                "  GEOGCS[“WGS 84”,\n" +
                "    DATUM[“World Geodetic System 1984”,\n" +
                "      SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
                "      PRIMEM[“Greenwich”, 0.0],\n" +
                "    UNIT[“degree”, 0.017453292519943295],\n" +
                "    AXIS[“Longitude”, EAST],\n" +
                "    AXIS[“Latitude”, NORTH]],\n" +
                "  VERT_CS[“Ellipsoidal height”,\n" +
                "    VERT_DATUM[“Ellipsoid”, 2002],\n" +
                "    UNIT[“metre”, 1],\n" +
                "    AXIS[“Ellipsoidal height”, UP]]]",
                HardCodedCRS.WGS84_3D);
    }
}
