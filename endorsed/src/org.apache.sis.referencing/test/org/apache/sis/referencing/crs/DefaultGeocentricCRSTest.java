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

import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.internal.Legacy;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.opengis.test.Validators;
import org.apache.sis.test.TestCase;
import static org.apache.sis.referencing.Assertions.assertWktEquals;


/**
 * Tests the {@link DefaultGeocentricCRS} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DefaultGeocentricCRSTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultGeocentricCRSTest() {
    }

    /**
     * Tests the {@link DefaultGeocentricCRS#forConvention(AxesConvention)} method
     * for {@link AxesConvention#RIGHT_HANDED}.
     */
    @Test
    public void testRightHanded() {
        final DefaultGeocentricCRS crs = DefaultGeocentricCRS.castOrCopy(HardCodedCRS.SPHERICAL);
        final DefaultGeocentricCRS normalized = crs.forConvention(AxesConvention.RIGHT_HANDED);
        assertNotSame(crs, normalized);
        final CoordinateSystem cs = normalized.getCoordinateSystem();
        final CoordinateSystem ref = crs.getCoordinateSystem();
        assertSame(ref.getAxis(1), cs.getAxis(0));
        assertSame(ref.getAxis(0), cs.getAxis(1));
        assertSame(ref.getAxis(2), cs.getAxis(2));
    }

    /**
     * Tests the {@link DefaultGeocentricCRS#forConvention(AxesConvention)} method
     * for {@link AxesConvention#POSITIVE_RANGE}.
     */
    @Test
    public void testShiftLongitudeRange() {
        final DefaultGeocentricCRS crs = HardCodedCRS.SPHERICAL;
        CoordinateSystemAxis axis = crs.getCoordinateSystem().getAxis(1);
        assertEquals(-180.0, axis.getMinimumValue());
        assertEquals(+180.0, axis.getMaximumValue());

        final DefaultGeocentricCRS shifted =  crs.forConvention(AxesConvention.POSITIVE_RANGE);
        assertNotSame(crs, shifted, "Expected a new CRS.");
        Validators.validate(shifted);

        axis = shifted.getCoordinateSystem().getAxis(1);
        assertEquals(  0.0, axis.getMinimumValue());
        assertEquals(360.0, axis.getMaximumValue());
        assertSame(shifted, shifted.forConvention(AxesConvention.POSITIVE_RANGE), "Expected a no-op.");
        assertSame(shifted,     crs.forConvention(AxesConvention.POSITIVE_RANGE), "Expected cached instance.");
    }

    /**
     * Tests WKT 1 formatting.
     * Axis directions Geocentric X, Y and Z shall be replaced be Other, East and North respectively,
     * for conformance with legacy WKT 1 practice.
     */
    @Test
    public void testWKT1() {
        assertWktEquals(Convention.WKT1,
                "GEOCCS[“Geocentric”,\n" +
                "  DATUM[“World Geodetic System 1984”,\n" +
                "    SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
                "    PRIMEM[“Greenwich”, 0.0],\n" +
                "  UNIT[“metre”, 1],\n" +
                "  AXIS[“X”, OTHER],\n" +
                "  AXIS[“Y”, EAST],\n" +
                "  AXIS[“Z”, NORTH]]",
                HardCodedCRS.GEOCENTRIC);
    }

    /**
     * Tests WKT 1 formatting using axes in kilometres. The intent of this test is to verify that
     * the coordinate system replacement documented in {@link #testWKT1()} preserves the axis units.
     */
    @Test
    public void testWKT1_kilometres() {
        DefaultGeocentricCRS crs = HardCodedCRS.GEOCENTRIC;
        crs = new DefaultGeocentricCRS(IdentifiedObjects.getProperties(crs), crs.getDatum(), null,
                Legacy.replaceUnit((CartesianCS) crs.getCoordinateSystem(), Units.KILOMETRE));
        assertWktEquals(Convention.WKT1,
                "GEOCCS[“Geocentric”,\n" +
                "  DATUM[“World Geodetic System 1984”,\n" +
                "    SPHEROID[“WGS84”, 6378137.0, 298.257223563]],\n" +
                "    PRIMEM[“Greenwich”, 0.0],\n" +
                "  UNIT[“kilometre”, 1000],\n" +
                "  AXIS[“X”, OTHER],\n" +
                "  AXIS[“Y”, EAST],\n" +
                "  AXIS[“Z”, NORTH]]",
                crs);
    }

    /**
     * Tests WKT 2 formatting.
     *
     * <h4>Note on axis names</h4>
     * ISO 19162 said: “For geodetic CRSs having a geocentric Cartesian coordinate system,
     * the axis name should be omitted as it is given through the mandatory axis direction,
     * but the axis abbreviation, respectively ‘X’, 'Y' and ‘Z’, shall be given.”
     */
    @Test
    public void testWKT2() {
        assertWktEquals(Convention.WKT2_2015,
                "GEODCRS[“Geocentric”,\n" +
                "  DATUM[“World Geodetic System 1984”,\n" +
                "    ELLIPSOID[“WGS84”, 6378137.0, 298.257223563, LENGTHUNIT[“metre”, 1]]],\n" +
                "    PRIMEM[“Greenwich”, 0.0, ANGLEUNIT[“degree”, 0.017453292519943295]],\n" +
                "  CS[Cartesian, 3],\n" +
                "    AXIS[“(X)”, geocentricX, ORDER[1]],\n" +
                "    AXIS[“(Y)”, geocentricY, ORDER[2]],\n" +
                "    AXIS[“(Z)”, geocentricZ, ORDER[3]],\n" +
                "    LENGTHUNIT[“metre”, 1]]",
                HardCodedCRS.GEOCENTRIC);
    }

    /**
     * Tests WKT 2 simplified formatting.
     */
    @Test
    public void testWKT2_Simplified() {
        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "GeodeticCRS[“Geocentric”,\n" +
                "  Datum[“World Geodetic System 1984”,\n" +
                "    Ellipsoid[“WGS84”, 6378137.0, 298.257223563]],\n" +
                "  CS[Cartesian, 3],\n" +
                "    Axis[“(X)”, geocentricX],\n" +
                "    Axis[“(Y)”, geocentricY],\n" +
                "    Axis[“(Z)”, geocentricZ],\n" +
                "    Unit[“metre”, 1]]",
                HardCodedCRS.GEOCENTRIC);
    }

    /**
     * Tests WKT 2 internal formatting.
     */
    @Test
    public void testWKT2_Internal() {
        assertWktEquals(Convention.INTERNAL,
                "GeodeticCRS[“Geocentric”,\n" +
                "  Datum[“World Geodetic System 1984”,\n" +
                "    Ellipsoid[“WGS84”, 6378137.0, 298.257223563],\n" +
                "    Scope[“Satellite navigation.”],\n" +
                "    Id[“EPSG”, 6326]],\n" +
                "    PrimeMeridian[“Greenwich”, 0.0, Id[“EPSG”, 8901]],\n" +
                "  CS[Cartesian, 3],\n" +
                "    Axis[“Geocentric X (X)”, geocentricX],\n" +
                "    Axis[“Geocentric Y (Y)”, geocentricY],\n" +
                "    Axis[“Geocentric Z (Z)”, geocentricZ],\n" +
                "    Unit[“metre”, 1, Id[“EPSG”, 9001]]]",
                HardCodedCRS.GEOCENTRIC);
    }
}
