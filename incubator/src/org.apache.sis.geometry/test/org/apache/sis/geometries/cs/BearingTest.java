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
package org.apache.sis.geometries.cs;

import java.util.Arrays;
import javax.measure.Quantity;
import javax.measure.quantity.Angle;
import org.apache.sis.maths.Vector;
import org.apache.sis.maths.Vectors;
import org.apache.sis.measure.Quantities;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CommonCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link Bearing}.
 *
 * @author Johann Sorel (Geomatys)
 */
public class BearingTest {
    /**
     * Tolerance for the comparison of angles in degrees and of vector coordinates.
     */
    private static final double TOLERANCE = 1E-9;

    /**
     * Creates a new test case.
     */
    public BearingTest() {
    }

    /**
     * Returns the given value as an angle in degrees.
     */
    private static Quantity<Angle> deg(final double value) {
        return Quantities.create(value, Units.DEGREE);
    }

    /**
     * Creates a vector with the given coordinates, in no particular coordinate reference system.
     */
    private static Vector<?> vector(final double... values) {
        final Vector<?> v = Vectors.createDouble(values.length);
        for (int i=0; i<values.length; i++) {
            v.set(i, values[i]);
        }
        return v;
    }

    /**
     * Creates a vector with the given coordinates in the given coordinate reference system.
     */
    private static Vector<?> vector(final CoordinateReferenceSystem crs, final double... values) {
        final Vector<?> v = Vectors.createDouble(crs);
        for (int i=0; i<values.length; i++) {
            v.set(i, values[i]);
        }
        return v;
    }

    /**
     * Returns the azimuth of the given bearing in degrees.
     */
    private static double azimuth(final Bearing bearing) {
        final Quantity<Angle> a = bearing.getAzimuth();
        assertNotNull(a, "azimuth");
        assertEquals(Units.DEGREE, a.getUnit(), "Angles shall be returned in degrees.");
        return a.getValue().doubleValue();
    }

    /**
     * Returns the altitude of the given bearing in degrees.
     */
    private static double altitude(final Bearing bearing) {
        final Quantity<Angle> a = bearing.getAltitude();
        assertNotNull(a, "altitude");
        assertEquals(Units.DEGREE, a.getUnit(), "Angles shall be returned in degrees.");
        return a.getValue().doubleValue();
    }

    /**
     * A bearing whose angular form is given, together with the direction vector expected from it.
     *
     * @param  azimuth   angle from the reference direction, in degrees.
     * @param  altitude  angle above the horizontal plane in degrees, or {@code null} if none.
     * @param  rotation  sense in which the azimuth is measured.
     * @param  expected  expected coordinates of the direction vector.
     */
    private record TestCase(double azimuth, Double altitude, Rotation rotation, double[] expected) {
    }

    /**
     * The bearings tested by {@link #testAnglesToDirection()} and {@link #testDirectionToAngles()}.
     * The coordinates are given in the east, north and up order.
     */
    private static final TestCase[] ENTRIES = {
        new TestCase(  0, null, Rotation.CLOCKWISE,         new double[] { 0,  1}),
        new TestCase( 90, null, Rotation.CLOCKWISE,         new double[] { 1,  0}),
        new TestCase(180, null, Rotation.CLOCKWISE,         new double[] { 0, -1}),
        new TestCase(270, null, Rotation.CLOCKWISE,         new double[] {-1,  0}),
        new TestCase( 90, null, Rotation.COUNTER_CLOCKWISE, new double[] {-1,  0}),
        new TestCase(270, null, Rotation.COUNTER_CLOCKWISE, new double[] { 1,  0}),
        new TestCase(  0, 0.0,  Rotation.CLOCKWISE,         new double[] { 0,  1, 0}),
        new TestCase( 90, 0.0,  Rotation.CLOCKWISE,         new double[] { 1,  0, 0}),
        new TestCase(  0, 45.0, Rotation.CLOCKWISE,         new double[] { 0,  Math.sqrt(0.5), Math.sqrt(0.5)}),
        new TestCase( 45, 0.0,  Rotation.CLOCKWISE,         new double[] { Math.sqrt(0.5), Math.sqrt(0.5), 0})
    };

    /**
     * Test of {@code ofAzimuth(Quantity)} default values.
     */
    @Test
    public void testDefaults() {
        final Bearing bearing = Bearing.ofAzimuth(deg(45));
        assertEquals(45, azimuth(bearing), TOLERANCE);
        assertNull(bearing.getAltitude(), "A bearing created without altitude shall have none.");
        assertEquals(FixedDirection.TRUE_NORTH, bearing.getReference());
        assertEquals(Rotation.CLOCKWISE, bearing.getRotation());
        assertEquals(2, bearing.getDirection().getDimension());
    }

    /**
     * Test of the direction derived from the angles of a bearing.
     */
    @Test
    public void testAnglesToDirection() {
        for (final TestCase entry : ENTRIES) {
            final Bearing bearing = Bearing.ofAzimuth(deg(entry.azimuth()),
                    (entry.altitude() != null) ? deg(entry.altitude()) : null,
                    FixedDirection.TRUE_NORTH, entry.rotation());
            assertArrayEquals(entry.expected(), bearing.getDirection().toArrayDouble(), TOLERANCE,
                    () -> "Direction of azimuth " + entry.azimuth() + "° " + entry.rotation());
        }
    }

    /**
     * Test of the angles derived from the direction of a bearing.
     */
    @Test
    public void testDirectionToAngles() {
        for (final TestCase entry : ENTRIES) {
            final Bearing bearing = Bearing.ofDirection(vector(entry.expected()),
                    FixedDirection.TRUE_NORTH, entry.rotation());
            assertEquals(entry.azimuth(), azimuth(bearing), TOLERANCE,
                    () -> "Azimuth of " + Arrays.toString(entry.expected()));
            if (entry.altitude() == null) {
                assertNull(bearing.getAltitude());
            } else {
                assertEquals(entry.altitude(), altitude(bearing), TOLERANCE);
            }
        }
    }

    /**
     * Test that the angular and the vectorial forms convert into each other without loss.
     */
    @Test
    public void testRoundTrip() {
        for (final double a : new double[] {0, 1, 45, 90, 180, 270, 359}) {
            for (final Double h : new Double[] {null, -89.0, -45.0, 0.0, 45.0, 89.0}) {
                final Bearing source = Bearing.ofAzimuth(deg(a), (h != null) ? deg(h) : null);
                final Bearing target = Bearing.ofDirection(source.getDirection());
                assertEquals(a, azimuth(target), TOLERANCE, () -> "Azimuth " + a + "°, altitude " + h);
                if (h == null) {
                    assertNull(target.getAltitude());
                } else {
                    assertEquals(h, altitude(target), TOLERANCE);
                }
            }
        }
    }

    /**
     * Test that the rotation changes the azimuth but not the altitude.
     */
    @Test
    public void testRotation() {
        final Bearing cw  = Bearing.ofAzimuth(deg(90), deg(30), FixedDirection.TRUE_NORTH, Rotation.CLOCKWISE);
        final Bearing ccw = Bearing.ofAzimuth(deg(90), deg(30), FixedDirection.TRUE_NORTH, Rotation.COUNTER_CLOCKWISE);
        final double[] d1 = cw .getDirection().toArrayDouble();
        final double[] d2 = ccw.getDirection().toArrayDouble();
        assertEquals(-d1[0], d2[0], TOLERANCE, "The east coordinate shall be mirrored.");
        assertEquals( d1[1], d2[1], TOLERANCE, "The north coordinate shall be unchanged.");
        assertEquals( d1[2], d2[2], TOLERANCE, "The rotation shall not change the altitude.");
        assertEquals(30, altitude(ccw), TOLERANCE);
        /*
         * The same direction read in the two senses gives supplementary azimuths.
         */
        final Vector<?> east = vector(1, 0);
        assertEquals( 90, azimuth(Bearing.ofDirection(east, FixedDirection.TRUE_NORTH, Rotation.CLOCKWISE)),         TOLERANCE);
        assertEquals(270, azimuth(Bearing.ofDirection(east, FixedDirection.TRUE_NORTH, Rotation.COUNTER_CLOCKWISE)), TOLERANCE);
    }

    /**
     * Test that the azimuth is reduced to the range of 0 inclusive to 360 exclusive.
     */
    @Test
    public void testAzimuthNormalization() {
        assertEquals(270, azimuth(Bearing.ofAzimuth(deg(-90))),  TOLERANCE);
        assertEquals( 90, azimuth(Bearing.ofAzimuth(deg(450))),  TOLERANCE);
        assertEquals(  0, azimuth(Bearing.ofAzimuth(deg(720))),  TOLERANCE);
        assertEquals(359, azimuth(Bearing.ofAzimuth(deg(-1))),   TOLERANCE);
        /*
         * A negative zero shall be turned into a positive one, because the two do not have the same
         * bit pattern and would therefore compare as different values. `assertEquals(double,double)`
         * compares the bit patterns, so it does detect a negative zero here.
         */
        assertEquals(0.0, azimuth(Bearing.ofAzimuth(deg(-0.0))), "The azimuth shall not be a negative zero.");
    }

    /**
     * Test that an altitude outside the range of −90 to +90 degrees is rejected.
     */
    @Test
    public void testAltitudeOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> Bearing.ofAzimuth(deg(0), deg( 91)));
        assertThrows(IllegalArgumentException.class, () -> Bearing.ofAzimuth(deg(0), deg(-91)));
        assertThrows(IllegalArgumentException.class, () -> Bearing.ofAzimuth(deg(0), deg(Double.NaN)));
        assertThrows(IllegalArgumentException.class, () -> Bearing.ofAzimuth(deg(Double.POSITIVE_INFINITY)));
    }

    /**
     * Test that a direction which gives no usable direction is rejected.
     */
    @Test
    public void testInvalidDirection() {
        assertThrows(IllegalArgumentException.class, () -> Bearing.ofDirection(vector(0, 0)),
                     "A vector of length zero gives no direction.");
        assertThrows(IllegalArgumentException.class, () -> Bearing.ofDirection(vector(0, 0, 0)));
        assertThrows(IllegalArgumentException.class, () -> Bearing.ofDirection(vector(1, Double.NaN)));
        assertThrows(IllegalArgumentException.class, () -> Bearing.ofDirection(vector(Double.POSITIVE_INFINITY, 1)));
    }

    /**
     * Test that only the direction of the given vector is retained, not its length.
     */
    @Test
    public void testDirectionIsNormalized() {
        final Bearing bearing = Bearing.ofDirection(vector(0, 5));
        assertEquals(0, azimuth(bearing), TOLERANCE);
        assertArrayEquals(new double[] {0, 1}, bearing.getDirection().toArrayDouble(), TOLERANCE,
                          "The direction shall be a unit vector.");
    }

    /**
     * Test of a direction whose dimension allows no angle to be derived.
     */
    @Test
    public void testNonDerivableDimension() {
        for (final Vector<?> v : new Vector<?>[] {vector(3), vector(1, 2, 3, 4)}) {
            final Bearing bearing = Bearing.ofDirection(v);
            assertNull(bearing.getAzimuth(),  "No azimuth can be derived in this dimension.");
            assertNull(bearing.getAltitude(), "No altitude can be derived in this dimension.");
            assertNotNull(bearing.getDirection());
            assertEquals(1, bearing.getDirection().length(), TOLERANCE);
        }
    }

    /**
     * Test that the angles are converted to degrees when the bearing is created.
     */
    @Test
    public void testUnitConversion() {
        final Bearing inRadians = Bearing.ofAzimuth(Quantities.create(Math.PI/2, Units.RADIAN));
        assertEquals(Units.DEGREE, inRadians.getAzimuth().getUnit(),
                     "Angles shall be canonicalized to degrees when the bearing is created.");
        assertEquals(90, azimuth(inRadians), TOLERANCE);
        /*
         * Equality is exact, and the conversion from radians to degrees is not, so two bearings
         * given in different units are not required to be equal. Only the unit of the result and
         * its value are pinned here.
         */
        assertEquals(Units.DEGREE, Bearing.ofAzimuth(deg(90)).getAzimuth().getUnit());
    }

    /**
     * Test that a bearing whose reference direction leads back to itself is rejected.
     */
    @Test
    public void testCyclicReference() {
        assertThrows(IllegalArgumentException.class,
                () -> Bearing.ofAzimuth(deg(0), null, new Stub(null, true), Rotation.CLOCKWISE),
                "A reference direction referring to itself shall be rejected.");

        final Stub a = new Stub(null, false);
        final Stub b = new Stub(a, false);
        a.reference = b;
        assertThrows(IllegalArgumentException.class,
                () -> Bearing.ofAzimuth(deg(0), null, a, Rotation.CLOCKWISE),
                "A cycle between two reference directions shall be rejected.");

        assertThrows(IllegalArgumentException.class,
                () -> Bearing.ofAzimuth(deg(0), null, new Endless(), Rotation.CLOCKWISE),
                "An endless chain of reference directions shall be rejected, not walked forever.");
    }

    /**
     * Test that a finite chain of reference directions is accepted.
     */
    @Test
    public void testNestedReference() {
        final Bearing inner  = Bearing.ofAzimuth(deg(20));
        final Bearing outer  = Bearing.ofAzimuth(deg(10), null, inner, Rotation.CLOCKWISE);
        final Bearing nested = Bearing.ofAzimuth(deg(5),  null, outer, Rotation.CLOCKWISE);
        assertSame(inner, outer.getReference());
        assertSame(outer, nested.getReference());
    }

    /**
     * Test of {@code equals(Object)} and {@code hashCode()}.
     */
    @Test
    public void testEqualsAndHashCode() {
        final Bearing bearing = Bearing.ofAzimuth(deg(45), deg(10));
        assertEquals(bearing, bearing);
        assertNotEquals(bearing, null);

        final Bearing same = Bearing.ofAzimuth(deg(45), deg(10));
        assertEquals(bearing, same);
        assertEquals(bearing.hashCode(), same.hashCode());

        assertNotEquals(bearing, Bearing.ofAzimuth(deg(46), deg(10)));
        assertNotEquals(bearing, Bearing.ofAzimuth(deg(45), deg(11)));
        assertNotEquals(bearing, Bearing.ofAzimuth(deg(45), deg(10), FixedDirection.MAGNETIC_NORTH, Rotation.CLOCKWISE));
        assertNotEquals(bearing, Bearing.ofAzimuth(deg(45), deg(10), FixedDirection.TRUE_NORTH, Rotation.COUNTER_CLOCKWISE));
        /*
         * An implementation other than the one of this module is never equal, so that equality
         * stays symmetric.
         */
        assertNotEquals(bearing, new Stub(FixedDirection.TRUE_NORTH, false));
    }

    /**
     * Test that a bearing shares no mutable state with its argument or with its result.
     */
    @Test
    public void testDefensiveCopy() {
        final Vector<?> source = vector(0, 1);
        final Bearing bearing = Bearing.ofDirection(source);
        source.set(0, 100);
        assertArrayEquals(new double[] {0, 1}, bearing.getDirection().toArrayDouble(), TOLERANCE,
                          "Modifying the given vector shall not modify the bearing.");

        final Vector<?> result = bearing.getDirection();
        result.set(0, 100);
        assertArrayEquals(new double[] {0, 1}, bearing.getDirection().toArrayDouble(), TOLERANCE,
                          "Modifying the returned vector shall not modify the bearing.");
    }

    /**
     * Test that the azimuth does not depend on the order in which the axes are given.
     */
    @Test
    public void testAxisOrder() {
        /*
         * `normalizedGeographic()` gives the coordinates in the (longitude, latitude) order, which
         * is (east, north), while `geographic()` gives them in the (latitude, longitude) order,
         * which is (north, east). The same physical direction shall give the same azimuth.
         */
        final CoordinateReferenceSystem eastNorth = CommonCRS.WGS84.normalizedGeographic();
        final CoordinateReferenceSystem northEast = CommonCRS.WGS84.geographic();
        assertEquals(90, azimuth(Bearing.ofDirection(vector(eastNorth, 1, 0))), TOLERANCE, "Toward east.");
        assertEquals(90, azimuth(Bearing.ofDirection(vector(northEast, 0, 1))), TOLERANCE, "Toward east.");
        assertEquals( 0, azimuth(Bearing.ofDirection(vector(eastNorth, 0, 1))), TOLERANCE, "Toward north.");
        assertEquals( 0, azimuth(Bearing.ofDirection(vector(northEast, 1, 0))), TOLERANCE, "Toward north.");
    }

    /**
     * Test of {@code toString()}.
     */
    @Test
    public void testToString() {
        final String text = Bearing.ofAzimuth(deg(45), deg(10)).toString();
        assertTrue(text.contains("45"),        text);
        assertTrue(text.contains("10"),        text);
        assertTrue(text.contains("TRUE_NORTH"), text);
        assertTrue(text.contains("CLOCKWISE"), text);
    }

    /**
     * A bearing which gives an arbitrary reference direction, for testing the constraint that
     * the reference direction of a bearing shall not refer to that bearing transitively.
     */
    private static final class Stub implements Bearing {
        /** The reference direction to return, or {@code null} for {@code this}. */
        ReferenceDirection reference;

        /** Whether {@link #getReference()} shall return {@code this}. */
        private final boolean self;

        /** Creates a new stub returning the given reference direction. */
        Stub(final ReferenceDirection reference, final boolean self) {
            this.reference = reference;
            this.self = self;
        }

        @Override public Quantity<Angle>    getAzimuth()   {return null;}
        @Override public Quantity<Angle>    getAltitude()  {return null;}
        @Override public Vector<?>          getDirection() {return null;}
        @Override public Rotation           getRotation()  {return Rotation.CLOCKWISE;}
        @Override public ReferenceDirection getReference() {return self ? this : reference;}
    }

    /**
     * A bearing returning a new reference direction on each call, so that the chain of reference
     * directions is endless without ever containing twice the same instance.
     */
    private static final class Endless implements Bearing {
        /** Creates a new stub. */
        Endless() {
        }

        @Override public Quantity<Angle>    getAzimuth()   {return null;}
        @Override public Quantity<Angle>    getAltitude()  {return null;}
        @Override public Vector<?>          getDirection() {return null;}
        @Override public Rotation           getRotation()  {return Rotation.CLOCKWISE;}
        @Override public ReferenceDirection getReference() {return new Endless();}
    }
}
