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

import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;
import javax.measure.converter.ConversionException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.cs.VerticalCS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.measure.Units;
import org.apache.sis.measure.Angle;
import org.apache.sis.measure.ElevationAngle;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.lang.Double.NaN;
import static java.util.Collections.singletonMap;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import static org.apache.sis.referencing.IdentifiedObjects.getProperties;
import static org.apache.sis.referencing.cs.CoordinateSystems.*;
import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link CoordinateSystems} class.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.4
 * @version 0.6
 * @module
 */
@DependsOn({
    DirectionAlongMeridianTest.class,
    NormalizerTest.class
})
public final strictfp class CoordinateSystemsTest extends TestCase {
    /**
     * Tests {@link CoordinateSystems#parseAxisDirection(String)}.
     */
    @Test
    public void testParseAxisDirection() {
        assertEquals("NORTH",            AxisDirection.NORTH,            parseAxisDirection("NORTH"));
        assertEquals("north",            AxisDirection.NORTH,            parseAxisDirection("north"));
        assertEquals("  north ",         AxisDirection.NORTH,            parseAxisDirection("  north "));
        assertEquals("east",             AxisDirection.EAST,             parseAxisDirection("east"));
        assertEquals("NORTH_EAST",       AxisDirection.NORTH_EAST,       parseAxisDirection("NORTH_EAST"));
        assertEquals("north-east",       AxisDirection.NORTH_EAST,       parseAxisDirection("north-east"));
        assertEquals("north east",       AxisDirection.NORTH_EAST,       parseAxisDirection("north east"));
        assertEquals("south-south-east", AxisDirection.SOUTH_SOUTH_EAST, parseAxisDirection("south-south-east"));
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
    @DependsOnMethod({"testParseAxisDirection", "testAngle"})
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
     * Asserts that the angle between the parsed directions is equals to the given value.
     * This method tests also the angle by interchanging the axis directions.
     */
    private static void assertAngleEquals(final boolean isElevation, final double expected,
            final String source, final String target)
    {
        final AxisDirection dir1 = parseAxisDirection(source);
        final AxisDirection dir2 = parseAxisDirection(target);
        assertNotNull(source, dir1);
        assertNotNull(target, dir2);
        assertAngleEquals(isElevation, expected, dir1, dir2);
    }

    /**
     * Asserts that the angle between the given directions is equals to the given value.
     * This method tests also the angle by interchanging the given directions.
     */
    private static void assertAngleEquals(final boolean isElevation, final double expected,
            final AxisDirection source, final AxisDirection target)
    {
        final Angle forward = angle(source, target);
        final Angle inverse = angle(target, source);
        assertEquals(isElevation, forward instanceof ElevationAngle);
        assertEquals(isElevation, inverse instanceof ElevationAngle);
        assertEquals(+expected, (forward != null) ? forward.degrees() : Double.NaN, STRICT);
        assertEquals(-expected, (inverse != null) ? inverse.degrees() : Double.NaN, STRICT);
    }

    /**
     * Tests {@link CoordinateSystems#swapAndScaleAxes(CoordinateSystem, CoordinateSystem)} for (λ,φ) ↔ (φ,λ).
     * This very common conversion is of critical importance to Apache SIS.
     *
     * @throws ConversionException Should not happen.
     */
    @Test
    public void testSwapAndScaleAxes2D() throws ConversionException {
        final CoordinateSystem λφ = new DefaultEllipsoidalCS(singletonMap(NAME_KEY, "(λ,φ)"),
                HardCodedAxes.GEODETIC_LONGITUDE,
                HardCodedAxes.GEODETIC_LATITUDE);
        final CoordinateSystem φλ = new DefaultEllipsoidalCS(singletonMap(NAME_KEY, "(φ,λ)"),
                HardCodedAxes.GEODETIC_LATITUDE,
                HardCodedAxes.GEODETIC_LONGITUDE);
        final Matrix expected = Matrices.create(3, 3, new double[] {
                0, 1, 0,
                1, 0, 0,
                0, 0, 1});
        assertTrue(swapAndScaleAxes(λφ, λφ).isIdentity());
        assertTrue(swapAndScaleAxes(φλ, φλ).isIdentity());
        assertMatrixEquals("(λ,φ) → (φ,λ)", expected, swapAndScaleAxes(λφ, φλ), STRICT);
        assertMatrixEquals("(φ,λ) → (λ,φ)", expected, swapAndScaleAxes(φλ, λφ), STRICT);
    }

    /**
     * Tests {@link CoordinateSystems#swapAndScaleAxes(CoordinateSystem, CoordinateSystem)} for (λ,φ,h) ↔ (φ,λ,h).
     * This very common conversion is of critical importance to Apache SIS.
     *
     * @throws ConversionException Should not happen.
     */
    @Test
    @DependsOnMethod("testSwapAndScaleAxes2D")
    public void testSwapAndScaleAxes3D() throws ConversionException {
        final CoordinateSystem λφh = new DefaultEllipsoidalCS(singletonMap(NAME_KEY, "(λ,φ,h)"),
                HardCodedAxes.GEODETIC_LONGITUDE,
                HardCodedAxes.GEODETIC_LATITUDE,
                HardCodedAxes.ELLIPSOIDAL_HEIGHT);
        final CoordinateSystem φλh = new DefaultEllipsoidalCS(singletonMap(NAME_KEY, "(φ,λ,h)"),
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
        assertMatrixEquals("(λ,φ,h) → (φ,λ,h)", expected, swapAndScaleAxes(λφh, φλh), STRICT);
        assertMatrixEquals("(φ,λ,h) → (λ,φ,h)", expected, swapAndScaleAxes(φλh, λφh), STRICT);
    }

    /**
     * Tests {@link CoordinateSystems#swapAndScaleAxes(CoordinateSystem, CoordinateSystem)}
     * with a more arbitrary case, which include unit conversions.
     *
     * @throws ConversionException Should not happen.
     */
    @Test
    @DependsOnMethod("testSwapAndScaleAxes3D")
    public void testSwapAndScaleAxes() throws ConversionException {
        final CoordinateSystem hxy = new DefaultCartesianCS(singletonMap(NAME_KEY, "(h,x,y)"),
                HardCodedAxes.HEIGHT_cm,
                HardCodedAxes.EASTING,
                HardCodedAxes.NORTHING);
        final CoordinateSystem yxh = new DefaultCartesianCS(singletonMap(NAME_KEY, "(y,x,h)"),
                HardCodedAxes.SOUTHING,
                HardCodedAxes.EASTING,
                HardCodedAxes.DEPTH);
        assertTrue(swapAndScaleAxes(hxy, hxy).isIdentity());
        assertTrue(swapAndScaleAxes(yxh, yxh).isIdentity());
        assertMatrixEquals("(h,x,y) → (y,x,h)", Matrices.create(4, 4, new double[] {
                0,    0,   -1,    0,
                0,    1,    0,    0,
               -0.01, 0,    0,    0,
                0,    0,    0,    1}), swapAndScaleAxes(hxy, yxh), STRICT);

        assertMatrixEquals("(y,x,h) → (h,x,y)", Matrices.create(4, 4, new double[] {
                0,    0, -100,    0,
                0,    1,    0,    0,
               -1,    0,    0,    0,
                0,    0,    0,    1}), swapAndScaleAxes(yxh, hxy), STRICT);
    }

    /**
     * Tests {@link CoordinateSystems#swapAndScaleAxes(CoordinateSystem, CoordinateSystem)} with a non-square matrix.
     *
     * @throws ConversionException Should not happen.
     */
    @Test
    @DependsOnMethod("testSwapAndScaleAxes")
    public void testScaleAndSwapAxesNonSquare() throws ConversionException {
        final DefaultCartesianCS cs = new DefaultCartesianCS(singletonMap(NAME_KEY, "Test"),
                new DefaultCoordinateSystemAxis(getProperties(HardCodedAxes.SOUTHING), "y", AxisDirection.SOUTH, SI.CENTIMETRE),
                new DefaultCoordinateSystemAxis(getProperties(HardCodedAxes.EASTING),  "x", AxisDirection.EAST,  SI.MILLIMETRE));

        Matrix matrix = swapAndScaleAxes(HardCodedCS.CARTESIAN_2D, cs);
        assertMatrixEquals("(x,y) → (y,x)", Matrices.create(3, 3, new double[] {
                0,  -100,    0,
                1000,  0,    0,
                0,     0,    1
        }), matrix, STRICT);

        matrix = swapAndScaleAxes(HardCodedCS.CARTESIAN_3D, cs);
        assertMatrixEquals("(x,y,z) → (y,x)", Matrices.create(3, 4, new double[] {
                0,  -100,   0,   0,
                1000,  0,   0,   0,
                0,     0,   0,   1
        }), matrix, STRICT);
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
            public Unit<?> getUnitReplacement(Unit<?> unit) {
                if (Units.isAngular(unit)) {
                    unit = NonSI.GRADE;
                }
                return unit;
            }

            @Override
            public Unit<?> getUnitReplacement(CoordinateSystemAxis axis, Unit<?> unit) {
                if (Units.isAngular(unit)) {
                    unit = NonSI.GRADE;
                }
                return unit;
            }

            @Override
            public AxisDirection getDirectionReplacement(CoordinateSystemAxis axis, final AxisDirection direction) {
                return direction;
            }

            @Override
            public AxisDirection getDirectionReplacement(final AxisDirection direction) {
                return direction;
            }
        });
        assertEqualsIgnoreMetadata(targetCS, actualCS);
    }

    /**
     * Tests {@link CoordinateSystems#replaceAxes(CoordinateSystem, AxisFilter)}
     * with a change of coordinate system type.
     */
    @Test
    @DependsOnMethod("testReplaceAxes")
    public void testReplaceAxesWithTypeChange() {
        final EllipsoidalCS    sourceCS = HardCodedCS.GEODETIC_3D;
        final VerticalCS       targetCS = HardCodedCS.ELLIPSOIDAL_HEIGHT;   // What we want to get.
        final CoordinateSystem actualCS = CoordinateSystems.replaceAxes(sourceCS, new AxisFilter() {
            @Override
            public boolean accept(final CoordinateSystemAxis axis) {
                return Units.isLinear(axis.getUnit());
            }

            @Override
            public Unit<?> getUnitReplacement(final Unit<?> unit) {
                return unit;
            }

            @Override
            public AxisDirection getDirectionReplacement(final AxisDirection direction) {
                return direction;
            }

            @Override
            public Unit<?> getUnitReplacement(CoordinateSystemAxis axis, final Unit<?> unit) {
                return unit;
            }

            @Override
            public AxisDirection getDirectionReplacement(CoordinateSystemAxis axis, final AxisDirection direction) {
                return direction;
            }
        });
        assertEqualsIgnoreMetadata(targetCS, actualCS);
    }
}
