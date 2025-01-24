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
package org.apache.sis.referencing.cs;

import java.util.Map;
import java.util.Locale;
import static java.lang.Double.NaN;
import javax.measure.Unit;
import javax.measure.IncommensurableException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.VerticalCS;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.measure.Units;
import org.apache.sis.measure.Angle;
import org.apache.sis.measure.ElevationAngle;
import static org.apache.sis.referencing.IdentifiedObjects.getProperties;
import static org.apache.sis.referencing.cs.CoordinateSystems.*;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertEqualsIgnoreMetadata;

// Specific to the main branch:
import static org.apache.sis.test.GeoapiAssert.assertMatrixEquals;


/**
 * Tests the {@link CoordinateSystems} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public final class CoordinateSystemsTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public CoordinateSystemsTest() {
    }

    /**
     * Tests {@link CoordinateSystems#parseAxisDirection(String)}.
     */
    @Test
    public void testParseAxisDirection() {
        assertEquals(AxisDirection.NORTH,            parseAxisDirection("NORTH"));
        assertEquals(AxisDirection.NORTH,            parseAxisDirection("north"));
        assertEquals(AxisDirection.NORTH,            parseAxisDirection("  north "));
        assertEquals(AxisDirection.EAST,             parseAxisDirection("east"));
        assertEquals(AxisDirection.NORTH_EAST,       parseAxisDirection("NORTH_EAST"));
        assertEquals(AxisDirection.NORTH_EAST,       parseAxisDirection("north-east"));
        assertEquals(AxisDirection.NORTH_EAST,       parseAxisDirection("north east"));
        assertEquals(AxisDirection.SOUTH_SOUTH_EAST, parseAxisDirection("south-south-east"));
        assertEquals("South along 180°", parseAxisDirection("South along 180 deg").name());
        assertEquals("South along 180°", parseAxisDirection("South along 180°").name());
        assertEquals("South along 180°", parseAxisDirection(" SOUTH  along  180 ° ").name());
        assertEquals("South along 90°E", parseAxisDirection("south along 90 deg east").name());
        assertEquals("South along 90°E", parseAxisDirection("south along 90°e").name());
        assertEquals("North along 45°E", parseAxisDirection("north along 45 deg e").name());
        assertEquals("North along 45°W", parseAxisDirection("north along 45 deg west").name());
    }

    /**
     * Tests {@link CoordinateSystems#angle(AxisDirection, AxisDirection)}.
     */
    @Test
    public void testAngle() {
        assertAngleEquals(false,   0,   AxisDirection.EAST,             AxisDirection.EAST);
        assertAngleEquals(false,  90,   AxisDirection.EAST,             AxisDirection.NORTH);
        assertAngleEquals(false,  90,   AxisDirection.WEST,             AxisDirection.SOUTH);
        assertAngleEquals(false, 180,   AxisDirection.SOUTH,            AxisDirection.NORTH);
        assertAngleEquals(false, 180,   AxisDirection.WEST,             AxisDirection.EAST);
        assertAngleEquals(false,  45,   AxisDirection.NORTH_EAST,       AxisDirection.NORTH);
        assertAngleEquals(false,  22.5, AxisDirection.NORTH_NORTH_EAST, AxisDirection.NORTH);
        assertAngleEquals(false,  45,   AxisDirection.SOUTH,            AxisDirection.SOUTH_EAST);
        assertAngleEquals(false, NaN,   AxisDirection.NORTH,            AxisDirection.FUTURE);
        assertAngleEquals(true,   90,   AxisDirection.SOUTH,            AxisDirection.UP);
        assertAngleEquals(true,  -90,   AxisDirection.SOUTH,            AxisDirection.DOWN);
        assertAngleEquals(false,   0,   AxisDirection.UP,               AxisDirection.UP);
        assertAngleEquals(false,   0,   AxisDirection.DOWN,             AxisDirection.DOWN);
        assertAngleEquals(false, 180,   AxisDirection.DOWN,             AxisDirection.UP);
        assertAngleEquals(false, NaN,   AxisDirection.DOWN,             AxisDirection.FUTURE);
        assertAngleEquals(false, 180,   AxisDirection.DISPLAY_DOWN,     AxisDirection.DISPLAY_UP);
        assertAngleEquals(false, -90,   AxisDirection.DISPLAY_RIGHT,    AxisDirection.DISPLAY_DOWN);
        assertAngleEquals(false, NaN,   AxisDirection.DISPLAY_UP,       AxisDirection.DOWN);
        assertAngleEquals(false, NaN,   AxisDirection.PAST,             AxisDirection.FUTURE); // Not spatial directions.
        assertAngleEquals(false,  90,   AxisDirection.GEOCENTRIC_X,     AxisDirection.GEOCENTRIC_Y);
        assertAngleEquals(false,  90,   AxisDirection.GEOCENTRIC_Y,     AxisDirection.GEOCENTRIC_Z);
        assertAngleEquals(false,   0,   AxisDirection.GEOCENTRIC_Y,     AxisDirection.GEOCENTRIC_Y);
        assertAngleEquals(false, NaN,   AxisDirection.GEOCENTRIC_Z,     AxisDirection.UP);
    }

    /**
     * Tests {@link CoordinateSystems#angle(AxisDirection, AxisDirection)} using directions parsed from text.
     */
    @Test
    public void testAngleAlongMeridians() {
        assertAngleEquals(false,   90.0, "West",                    "South");
        assertAngleEquals(false,  -90.0, "South",                   "West");
        assertAngleEquals(false,   45.0, "South",                   "South-East");
        assertAngleEquals(true,    90.0, "West",                    "Up");
        assertAngleEquals(true,   -90.0, "West",                    "Down");
        assertAngleEquals(false,  -22.5, "North-North-West",        "North");
        assertAngleEquals(false,  -22.5, "North_North_West",        "North");
        assertAngleEquals(false,  -22.5, "North North West",        "North");
        assertAngleEquals(false,   90.0, "North along 90 deg East", "North along 0 deg");
        assertAngleEquals(false,   90.0, "South along 180 deg",     "South along 90 deg West");
        assertAngleEquals(false,   90.0, "North along 90°E",        "North along 0°");
        assertAngleEquals(false,  135.0, "North along 90°E",        "North along 45°W");
        assertAngleEquals(false, -135.0, "North along 45°W",        "North along 90°E");
        assertAngleEquals(true,    90.0, "North along 45°W",        "Up");
        assertAngleEquals(true,   -90.0, "North along 45°W",        "Down");
    }

    /**
     * Asserts that the angle between the parsed directions is equal to the given value.
     * This method tests also the angle by interchanging the axis directions.
     */
    private static void assertAngleEquals(final boolean isElevation, final double expected,
            final String source, final String target)
    {
        final AxisDirection dir1 = parseAxisDirection(source);
        final AxisDirection dir2 = parseAxisDirection(target);
        assertNotNull(dir1, source);
        assertNotNull(dir2, target);
        assertAngleEquals(isElevation, expected, dir1, dir2);
    }

    /**
     * Asserts that the angle between the given directions is equal to the given value.
     * This method tests also the angle by interchanging the given directions.
     */
    private static void assertAngleEquals(final boolean isElevation, final double expected,
            final AxisDirection source, final AxisDirection target)
    {
        final Angle forward = angle(source, target);
        final Angle inverse = angle(target, source);
        assertEquals(isElevation, forward instanceof ElevationAngle);
        assertEquals(isElevation, inverse instanceof ElevationAngle);
        assertEquals(+expected, (forward != null) ? forward.degrees() : Double.NaN);
        assertEquals(-expected, (inverse != null) ? inverse.degrees() : Double.NaN, 0);     // Δ=0 for ignoring the sign of ±0.
    }

    /**
     * Tests {@link CoordinateSystems#getSimpleAxisDirections(CoordinateSystem)}.
     */
    @Test
    public void testGetSimpleAxisDirections() {
        final AxisDirection n90 = parseAxisDirection("North along 90°E");
        final AxisDirection n00 = parseAxisDirection("North along 0°E");
        final var cs = new AbstractCS(Map.of(NAME_KEY, "Polar"),
                new DefaultCoordinateSystemAxis(Map.of(NAME_KEY, "Easting"),  "E", n90, Units.METRE),
                new DefaultCoordinateSystemAxis(Map.of(NAME_KEY, "Northing"), "N", n00, Units.METRE),
                HardCodedAxes.DEPTH);

        final var expected = new AxisDirection[] {n90, n00, AxisDirection.DOWN};
        assertArrayEquals(expected, getAxisDirections(cs));

        expected[0] = AxisDirection.EAST;
        expected[1] = AxisDirection.NORTH;
        assertArrayEquals(expected, getSimpleAxisDirections(cs));
    }

    /**
     * Tests {@link CoordinateSystems#hasAllTargetTypes(CoordinateSystem, CoordinateSystem)}.
     */
    @Test
    public void testHasAllTargetTypes() {
        final var cs = new DefaultCompoundCS(HardCodedCS.GEODETIC_2D, HardCodedCS.GRAVITY_RELATED_HEIGHT);
        assertTrue (CoordinateSystems.hasAllTargetTypes(cs, HardCodedCS.GRAVITY_RELATED_HEIGHT));
        assertFalse(CoordinateSystems.hasAllTargetTypes(cs, HardCodedCS.DAYS));
        assertTrue (CoordinateSystems.hasAllTargetTypes(cs, HardCodedCS.GEODETIC_2D));
        assertFalse(CoordinateSystems.hasAllTargetTypes(cs, HardCodedCS.CARTESIAN_2D));
        assertTrue (CoordinateSystems.hasAllTargetTypes(cs, cs));
    }

    /**
     * Tests {@link CoordinateSystems#swapAndScaleAxes(CoordinateSystem, CoordinateSystem)} for (λ,φ) ↔ (φ,λ).
     * This very common conversion is of critical importance to Apache SIS.
     *
     * @throws IncommensurableException if a conversion between incompatible units was attempted.
     */
    @Test
    public void testSwapAndScaleAxes2D() throws IncommensurableException {
        final CoordinateSystem λφ = new DefaultEllipsoidalCS(Map.of(NAME_KEY, "(λ,φ)"),
                HardCodedAxes.GEODETIC_LONGITUDE,
                HardCodedAxes.GEODETIC_LATITUDE);
        final CoordinateSystem φλ = new DefaultEllipsoidalCS(Map.of(NAME_KEY, "(φ,λ)"),
                HardCodedAxes.GEODETIC_LATITUDE,
                HardCodedAxes.GEODETIC_LONGITUDE);
        final Matrix expected = Matrices.create(3, 3, new double[] {
                0, 1, 0,
                1, 0, 0,
                0, 0, 1});
        assertTrue(swapAndScaleAxes(λφ, λφ).isIdentity());
        assertTrue(swapAndScaleAxes(φλ, φλ).isIdentity());
        assertMatrixEquals(expected, swapAndScaleAxes(λφ, φλ), STRICT, "(λ,φ) → (φ,λ)");
        assertMatrixEquals(expected, swapAndScaleAxes(φλ, λφ), STRICT, "(φ,λ) → (λ,φ)");
    }

    /**
     * Tests {@link CoordinateSystems#swapAndScaleAxes(CoordinateSystem, CoordinateSystem)} for (λ,φ,h) ↔ (φ,λ,h).
     * This very common conversion is of critical importance to Apache SIS.
     *
     * @throws IncommensurableException if a conversion between incompatible units was attempted.
     */
    @Test
    public void testSwapAndScaleAxes3D() throws IncommensurableException {
        final CoordinateSystem λφh = new DefaultEllipsoidalCS(Map.of(NAME_KEY, "(λ,φ,h)"),
                HardCodedAxes.GEODETIC_LONGITUDE,
                HardCodedAxes.GEODETIC_LATITUDE,
                HardCodedAxes.ELLIPSOIDAL_HEIGHT);
        final CoordinateSystem φλh = new DefaultEllipsoidalCS(Map.of(NAME_KEY, "(φ,λ,h)"),
                HardCodedAxes.GEODETIC_LATITUDE,
                HardCodedAxes.GEODETIC_LONGITUDE,
                HardCodedAxes.ELLIPSOIDAL_HEIGHT);
        final Matrix expected = Matrices.create(4, 4, new double[] {
                0, 1, 0, 0,
                1, 0, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1});
        assertTrue(swapAndScaleAxes(λφh, λφh).isIdentity());
        assertTrue(swapAndScaleAxes(φλh, φλh).isIdentity());
        assertMatrixEquals(expected, swapAndScaleAxes(λφh, φλh), STRICT, "(λ,φ,h) → (φ,λ,h)");
        assertMatrixEquals(expected, swapAndScaleAxes(φλh, λφh), STRICT, "(φ,λ,h) → (λ,φ,h)");
    }

    /**
     * Tests {@link CoordinateSystems#swapAndScaleAxes(CoordinateSystem, CoordinateSystem)}
     * with a more arbitrary case, which include unit conversions.
     *
     * @throws IncommensurableException if a conversion between incompatible units was attempted.
     */
    @Test
    public void testSwapAndScaleAxes() throws IncommensurableException {
        final CoordinateSystem hxy = new DefaultCartesianCS(Map.of(NAME_KEY, "(h,x,y)"),
                HardCodedAxes.HEIGHT_cm,
                HardCodedAxes.EASTING,
                HardCodedAxes.NORTHING);
        final CoordinateSystem yxh = new DefaultCartesianCS(Map.of(NAME_KEY, "(y,x,h)"),
                HardCodedAxes.SOUTHING,
                HardCodedAxes.EASTING,
                HardCodedAxes.DEPTH);
        assertTrue(swapAndScaleAxes(hxy, hxy).isIdentity());
        assertTrue(swapAndScaleAxes(yxh, yxh).isIdentity());
        assertMatrixEquals(Matrices.create(4, 4, new double[] {
                    0,    0,   -1,    0,
                    0,    1,    0,    0,
                   -0.01, 0,    0,    0,
                    0,    0,    0,    1
                }), swapAndScaleAxes(hxy, yxh), STRICT, "(h,x,y) → (y,x,h)");

        assertMatrixEquals(Matrices.create(4, 4, new double[] {
                    0,    0, -100,    0,
                    0,    1,    0,    0,
                   -1,    0,    0,    0,
                    0,    0,    0,    1
                }), swapAndScaleAxes(yxh, hxy), STRICT, "(y,x,h) → (h,x,y)");
    }

    /**
     * Tests {@link CoordinateSystems#swapAndScaleAxes(CoordinateSystem, CoordinateSystem)} with a non-square matrix.
     *
     * @throws IncommensurableException if a conversion between incompatible units was attempted.
     */
    @Test
    public void testScaleAndSwapAxesNonSquare() throws IncommensurableException {
        final var cs = new DefaultCartesianCS(Map.of(NAME_KEY, "Test"),
                new DefaultCoordinateSystemAxis(getProperties(HardCodedAxes.SOUTHING), "y", AxisDirection.SOUTH, Units.CENTIMETRE),
                new DefaultCoordinateSystemAxis(getProperties(HardCodedAxes.EASTING),  "x", AxisDirection.EAST,  Units.MILLIMETRE));

        Matrix matrix = swapAndScaleAxes(HardCodedCS.CARTESIAN_2D, cs);
        assertMatrixEquals(Matrices.create(3, 3, new double[] {
                    0,  -100,    0,
                    1000,  0,    0,
                    0,     0,    1
                }), matrix, STRICT, "(x,y) → (y,x)");

        matrix = swapAndScaleAxes(HardCodedCS.CARTESIAN_3D, cs);
        assertMatrixEquals(Matrices.create(3, 4, new double[] {
                    0,  -100,   0,   0,
                    1000,  0,   0,   0,
                    0,     0,   0,   1
                }), matrix, STRICT, "(x,y,z) → (y,x)");
    }

    /**
     * Tests {@link CoordinateSystems#replaceAxes(CoordinateSystem, AxisFilter)}
     * without change of coordinate system type.
     */
    @Test
    public void testReplaceAxes() {
        final EllipsoidalCS    sourceCS = HardCodedCS.GEODETIC_3D;
        final EllipsoidalCS    targetCS = HardCodedCS.ELLIPSOIDAL_gon;  // What we want to get.
        final CoordinateSystem actualCS = CoordinateSystems.replaceAxes(sourceCS, new AxisFilter() {
            @Override
            public boolean accept(final CoordinateSystemAxis axis) {
                return Units.isAngular(axis.getUnit());
            }

            @Override
            public Unit<?> getUnitReplacement(CoordinateSystemAxis axis, Unit<?> unit) {
                if (Units.isAngular(unit)) {
                    unit = Units.GRAD;
                }
                return unit;
            }
        });
        assertEqualsIgnoreMetadata(targetCS, actualCS);
    }

    /**
     * Tests {@link CoordinateSystems#replaceAxes(CoordinateSystem, AxisFilter)}
     * with a change of coordinate system type.
     */
    @Test
    public void testReplaceAxesWithTypeChange() {
        final EllipsoidalCS    sourceCS = HardCodedCS.GEODETIC_3D;
        final VerticalCS       targetCS = HardCodedCS.ELLIPSOIDAL_HEIGHT;   // What we want to get.
        final CoordinateSystem actualCS = CoordinateSystems.replaceAxes(sourceCS, new AxisFilter() {
            @Override
            public boolean accept(final CoordinateSystemAxis axis) {
                return Units.isLinear(axis.getUnit());
            }
        });
        assertEqualsIgnoreMetadata(targetCS, actualCS);
    }

    /**
     * Tests {@link CoordinateSystems#getShortName(CoordinateSystemAxis, Locale)}
     */
    @Test
    public void testGetShortName() {
        assertEquals("Latitude", CoordinateSystems.getShortName(HardCodedAxes.GEODETIC_LATITUDE,  Locale.ENGLISH));
        assertEquals("Height",   CoordinateSystems.getShortName(HardCodedAxes.ELLIPSOIDAL_HEIGHT, Locale.ENGLISH));
        assertEquals("Hauteur",  CoordinateSystems.getShortName(HardCodedAxes.ELLIPSOIDAL_HEIGHT, Locale.FRENCH));
    }

    /**
     * Tests {@link CoordinateSystems#getEpsgCode(Class, CoordinateSystemAxis...)}
     * with an ellipsoidal coordinate system.
     */
    @Test
    public void testGetEpsgCodeForEllipsoidalCS() {
        final Class<EllipsoidalCS> type = EllipsoidalCS.class;
        final CoordinateSystemAxis φ = HardCodedAxes.GEODETIC_LATITUDE;
        final CoordinateSystemAxis λ = HardCodedAxes.GEODETIC_LONGITUDE;
        final CoordinateSystemAxis h = HardCodedAxes.ELLIPSOIDAL_HEIGHT;
        assertEquals(Integer.valueOf(6422), CoordinateSystems.getEpsgCode(type, φ, λ));
        assertEquals(Integer.valueOf(6423), CoordinateSystems.getEpsgCode(type, φ, λ, h));
        assertEquals(Integer.valueOf(6424), CoordinateSystems.getEpsgCode(type, λ, φ));
        assertEquals(Integer.valueOf(6426), CoordinateSystems.getEpsgCode(type, λ, φ, h));
        assertNull(CoordinateSystems.getEpsgCode(type, HardCodedAxes.EASTING, HardCodedAxes.NORTHING));
    }

    /**
     * Tests {@link CoordinateSystems#getEpsgCode(Class, CoordinateSystemAxis...)}
     * with an ellipsoidal coordinate system.
     */
    @Test
    public void testGetEpsgCodeForCartesianCS() {
        final Class<CartesianCS> type = CartesianCS.class;
        final CoordinateSystemAxis E = HardCodedAxes.EASTING;
        final CoordinateSystemAxis W = HardCodedAxes.WESTING;
        final CoordinateSystemAxis N = HardCodedAxes.NORTHING;
        assertEquals(Integer.valueOf(4400), CoordinateSystems.getEpsgCode(type, E, N));
        assertEquals(Integer.valueOf(4500), CoordinateSystems.getEpsgCode(type, N, E));
        assertEquals(Integer.valueOf(4501), CoordinateSystems.getEpsgCode(type, N, W));
        assertNull(CoordinateSystems.getEpsgCode(type, HardCodedAxes.GEODETIC_LATITUDE, HardCodedAxes.GEODETIC_LONGITUDE));
    }
}
