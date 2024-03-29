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
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import static org.opengis.referencing.cs.CoordinateSystem.NAME_KEY;
import org.apache.sis.measure.Units;
import org.apache.sis.util.resources.Vocabulary;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests the {@link AbstractCS} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class AbstractCSTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public AbstractCSTest() {
    }

    /**
     * Gets a coordinate system for the given axes convention and compare against the expected values.
     *
     * @param  convention  the convention to use.
     * @param  cs          the coordinate system to test.
     * @param  expected    the expected axes, in order.
     */
    private static void verifyAxesConvention(final AxesConvention convention, final AbstractCS cs,
            final CoordinateSystemAxis... expected)
    {
        final AbstractCS derived = cs.forConvention(convention);
        assertNotSame(cs, derived);
        assertSame(derived, derived.forConvention(convention));
        assertSame(derived, cs.forConvention(convention));
        assertEquals(expected.length, cs.getDimension());
        for (int i=0; i<expected.length; i++) {
            assertEquals(expected[i], derived.getAxis(i));
        }
    }

    /**
     * Tests {@link AbstractCS#forConvention(AxesConvention)}
     * with a {@link AxesConvention#RIGHT_HANDED} argument.
     */
    @Test
    public void testForRightHandedConvention() {
        final var cs = new AbstractCS(Map.of(NAME_KEY, "Test"),
                HardCodedAxes.GEODETIC_LATITUDE,
                HardCodedAxes.TIME,
                HardCodedAxes.ALTITUDE,
                HardCodedAxes.GEODETIC_LONGITUDE);
        verifyAxesConvention(AxesConvention.RIGHT_HANDED, cs,
                HardCodedAxes.GEODETIC_LONGITUDE,
                HardCodedAxes.GEODETIC_LATITUDE,
                HardCodedAxes.ALTITUDE,
                HardCodedAxes.TIME);
        assertSame(cs.forConvention(AxesConvention.RIGHT_HANDED),
                   cs.forConvention(AxesConvention.DISPLAY_ORIENTED),
                   "Right-handed CS shall be same as display-oriented for this test.");
        assertSame(cs.forConvention(AxesConvention.RIGHT_HANDED),
                   cs.forConvention(AxesConvention.NORMALIZED),
                   "Right-handed CS shall be same as normalized for this test.");
    }

    /**
     * Tests {@link AbstractCS#forConvention(AxesConvention)}
     * with a {@link AxesConvention#NORMALIZED} argument.
     */
    @Test
    public void testForNormalizedConvention() {
        /*
         * Some expected axes, identical to the ones in HardCodedAxes except for name or units.
         * We verify the properties inferred by the constructor as a matter of principle, even
         * if it is not really the purpose of this test.
         */
        final var EASTING = new DefaultCoordinateSystemAxis(
                Map.of(NAME_KEY, Vocabulary.format(Vocabulary.Keys.Unnamed)), "E", AxisDirection.EAST, Units.METRE);
        final var HEIGHT = new DefaultCoordinateSystemAxis(
                Map.of(NAME_KEY, "Height"), "h", AxisDirection.UP, Units.METRE);
        assertEquals(Double.NEGATIVE_INFINITY, EASTING.getMinimumValue());
        assertEquals(Double.POSITIVE_INFINITY, EASTING.getMaximumValue());
        assertNull  (EASTING.getRangeMeaning());
        assertEquals(Double.NEGATIVE_INFINITY, HEIGHT.getMinimumValue());
        assertEquals(Double.POSITIVE_INFINITY, HEIGHT.getMaximumValue());
        assertNull  (HEIGHT.getRangeMeaning());
        /*
         * Now the actual test. First we opportunistically test RIGHT_HANDED and DISPLAY_ORIENTED
         * before to test NORMALIZED, in order to test in increasing complexity.
         */
        final var cs = new AbstractCS(Map.of(NAME_KEY, "Test"),
                HardCodedAxes.TIME,
                HardCodedAxes.NORTHING,
                HardCodedAxes.WESTING,
                HardCodedAxes.HEIGHT_cm);
        verifyAxesConvention(AxesConvention.RIGHT_HANDED, cs,
                HardCodedAxes.NORTHING,
                HardCodedAxes.WESTING,
                HardCodedAxes.HEIGHT_cm,
                HardCodedAxes.TIME);
        verifyAxesConvention(AxesConvention.DISPLAY_ORIENTED, cs,
                EASTING,
                HardCodedAxes.NORTHING,
                HardCodedAxes.HEIGHT_cm,
                HardCodedAxes.TIME);
        verifyAxesConvention(AxesConvention.NORMALIZED, cs,
                EASTING,
                HardCodedAxes.NORTHING,
                HEIGHT,
                HardCodedAxes.TIME);
    }

    /**
     * Tests {@link AbstractCS#forConvention(AxesConvention)}
     * with a {@link AxesConvention#POSITIVE_RANGE} argument.
     */
    @Test
    public void testForPositiveRangeConvention() {
        final var cs = new AbstractCS(Map.of(NAME_KEY, "Test"),
                HardCodedAxes.GEODETIC_LONGITUDE, HardCodedAxes.GEODETIC_LATITUDE);
        verifyAxesConvention(AxesConvention.POSITIVE_RANGE, cs,
                HardCodedAxes.SHIFTED_LONGITUDE, HardCodedAxes.GEODETIC_LATITUDE);
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        final var cs = new AbstractCS(Map.of(NAME_KEY, "Test"), HardCodedAxes.X, HardCodedAxes.Y);
        assertNotSame(cs, assertSerializedEquals(cs));
    }
}
