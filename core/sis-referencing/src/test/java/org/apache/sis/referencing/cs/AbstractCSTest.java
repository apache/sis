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
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.util.Collections.singletonMap;
import static org.opengis.referencing.cs.CoordinateSystem.NAME_KEY;
import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link AbstractCS} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.5
 * @module
 */
@DependsOn({
    org.apache.sis.referencing.AbstractIdentifiedObjectTest.class,
    DefaultCoordinateSystemAxisTest.class,
    NormalizerTest.class
})
public final strictfp class AbstractCSTest extends TestCase {
    /**
     * Gets a coordinate system for the given axes convention and compare against the expected values.
     *
     * @param convention The convention to use.
     * @param cs The coordinate system to test.
     * @param expected The expected axes, in order.
     */
    private static void verifyAxesConvention(final AxesConvention convention, final AbstractCS cs,
            final CoordinateSystemAxis... expected)
    {
        final AbstractCS derived = cs.forConvention(convention);
        assertNotSame("cs.forConvention(…)", cs, derived);
        assertSame("derived.forConvention(…)", derived, derived.forConvention(convention));
        assertSame("cs.forConvention(…)", derived, cs.forConvention(convention));
        assertEquals("dimension", expected.length, cs.getDimension());
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
        final AbstractCS cs = new AbstractCS(singletonMap(NAME_KEY, "Test"),
                HardCodedAxes.GEODETIC_LATITUDE,
                HardCodedAxes.TIME,
                HardCodedAxes.ALTITUDE,
                HardCodedAxes.GEODETIC_LONGITUDE);
        verifyAxesConvention(AxesConvention.RIGHT_HANDED, cs,
                HardCodedAxes.GEODETIC_LONGITUDE,
                HardCodedAxes.GEODETIC_LATITUDE,
                HardCodedAxes.ALTITUDE,
                HardCodedAxes.TIME);
        assertSame("Right-handed CS shall be same as conventionally oriented for this test.",
                cs.forConvention(AxesConvention.RIGHT_HANDED),
                cs.forConvention(AxesConvention.CONVENTIONALLY_ORIENTED));
        assertSame("Right-handed CS shall be same as normalized for this test.",
                cs.forConvention(AxesConvention.RIGHT_HANDED),
                cs.forConvention(AxesConvention.NORMALIZED));
    }

    /**
     * Tests {@link AbstractCS#forConvention(AxesConvention)}
     * with a {@link AxesConvention#NORMALIZED} argument.
     */
    @Test
    @DependsOnMethod("testForRightHandedConvention")
    public void testForNormalizedConvention() {
        /*
         * Some expected axes, identical to the ones in HardCodedAxes except for name or units.
         * We verify the properties inferred by the constructor as a matter of principle, even
         * if it is not really the purpose of this test.
         */
        final DefaultCoordinateSystemAxis EASTING = new DefaultCoordinateSystemAxis(
                singletonMap(NAME_KEY, Vocabulary.format(Vocabulary.Keys.Unnamed)), "E", AxisDirection.EAST, SI.METRE);
        final DefaultCoordinateSystemAxis HEIGHT = new DefaultCoordinateSystemAxis(
                singletonMap(NAME_KEY, "Height"), "h", AxisDirection.UP, SI.METRE);
        assertEquals("minimumValue", Double.NEGATIVE_INFINITY, EASTING.getMinimumValue(), STRICT);
        assertEquals("maximumValue", Double.POSITIVE_INFINITY, EASTING.getMaximumValue(), STRICT);
        assertNull  ("rangeMeaning", EASTING.getRangeMeaning());
        assertEquals("minimumValue", Double.NEGATIVE_INFINITY, HEIGHT.getMinimumValue(), STRICT);
        assertEquals("maximumValue", Double.POSITIVE_INFINITY, HEIGHT.getMaximumValue(), STRICT);
        assertNull  ("rangeMeaning", HEIGHT.getRangeMeaning());
        /*
         * Now the actual test. First we opportunistically test RIGHT_HANDED and CONVENTIONALLY_ORIENTED
         * before to test NORMALIZED, in order to test in increasing complexity.
         */
        final AbstractCS cs = new AbstractCS(singletonMap(NAME_KEY, "Test"),
                HardCodedAxes.TIME,
                HardCodedAxes.NORTHING,
                HardCodedAxes.WESTING,
                HardCodedAxes.HEIGHT_cm);
        verifyAxesConvention(AxesConvention.RIGHT_HANDED, cs,
                HardCodedAxes.NORTHING,
                HardCodedAxes.WESTING,
                HardCodedAxes.HEIGHT_cm,
                HardCodedAxes.TIME);
        verifyAxesConvention(AxesConvention.CONVENTIONALLY_ORIENTED, cs,
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
        final AbstractCS cs = new AbstractCS(singletonMap(NAME_KEY, "Test"),
                HardCodedAxes.GEODETIC_LONGITUDE, HardCodedAxes.GEODETIC_LATITUDE);
        verifyAxesConvention(AxesConvention.POSITIVE_RANGE, cs,
                HardCodedAxes.SHIFTED_LONGITUDE, HardCodedAxes.GEODETIC_LATITUDE);
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        final AbstractCS cs = new AbstractCS(singletonMap(NAME_KEY, "Test"), HardCodedAxes.X, HardCodedAxes.Y);
        assertNotSame(cs, assertSerializedEquals(cs));
    }
}
