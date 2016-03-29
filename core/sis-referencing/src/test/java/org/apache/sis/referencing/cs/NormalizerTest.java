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
import java.util.Arrays;
import javax.measure.unit.SI;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.util.Collections.singletonMap;
import static org.opengis.referencing.cs.CoordinateSystem.NAME_KEY;
import static org.apache.sis.test.ReferencingAssert.*;


/**
 * Tests the {@link Normalizer} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
@DependsOn({
    DirectionAlongMeridianTest.class,
    DefaultCoordinateSystemAxisTest.class
})
public final strictfp class NormalizerTest extends TestCase {
    /**
     * Tests {@link Normalizer#sort(CoordinateSystemAxis[])}.
     */
    @Test
    public void testSort() {
        assertOrdered(new CoordinateSystemAxis[] {
            HardCodedAxes.GEODETIC_LONGITUDE,
            HardCodedAxes.GEODETIC_LATITUDE,
            HardCodedAxes.ELLIPSOIDAL_HEIGHT
        }, new CoordinateSystemAxis[] {
            HardCodedAxes.GEODETIC_LONGITUDE,
            HardCodedAxes.GEODETIC_LATITUDE,
            HardCodedAxes.ELLIPSOIDAL_HEIGHT
        });
        assertOrdered(new CoordinateSystemAxis[] {
            HardCodedAxes.GEODETIC_LONGITUDE,
            HardCodedAxes.GEODETIC_LATITUDE,
            HardCodedAxes.ELLIPSOIDAL_HEIGHT
        }, new CoordinateSystemAxis[] {
            HardCodedAxes.GEODETIC_LATITUDE,
            HardCodedAxes.ELLIPSOIDAL_HEIGHT,
            HardCodedAxes.GEODETIC_LONGITUDE
        });

        // A plausible CS.
        assertOrdered(new AxisDirection[] {
            AxisDirection.EAST,                 // Right handed-rule
            AxisDirection.NORTH,                // Right handed-rule
            AxisDirection.UP
        }, new AxisDirection[] {
            AxisDirection.NORTH,
            AxisDirection.UP,
            AxisDirection.EAST
        });

        // A very dummy CS just for testing. The order of
        // any non-compass direction should be unchanged.
        assertOrdered(new AxisDirection[] {
            AxisDirection.NORTH_EAST,           // Right handed-rule
            AxisDirection.NORTH_NORTH_WEST,     // Right handed-rule
            AxisDirection.GEOCENTRIC_X,
            AxisDirection.GEOCENTRIC_Y,
            AxisDirection.PAST
        }, new AxisDirection[] {
            AxisDirection.GEOCENTRIC_Y,
            AxisDirection.NORTH_NORTH_WEST,
            AxisDirection.GEOCENTRIC_X,
            AxisDirection.NORTH_EAST,
            AxisDirection.PAST
        });

        // An other plausible CS.
        assertOrdered(new AxisDirection[] {
            AxisDirection.WEST,                 // Right handed-rule
            AxisDirection.SOUTH,                // Right handed-rule
            AxisDirection.DOWN
        }, new AxisDirection[] {
            AxisDirection.SOUTH,
            AxisDirection.DOWN,
            AxisDirection.WEST
        });

        // An other plausible CS.
        assertOrdered(new AxisDirection[] {
            AxisDirection.SOUTH,                // Right handed-rule
            AxisDirection.EAST,                 // Right handed-rule
            AxisDirection.DOWN
        }, new AxisDirection[] {
            AxisDirection.SOUTH,
            AxisDirection.DOWN,
            AxisDirection.EAST
        });

        // Legacy (WKT 1) geocentric axes.
        assertOrdered(new AxisDirection[] {
            AxisDirection.OTHER,
            AxisDirection.EAST,
            AxisDirection.NORTH
        }, new AxisDirection[] {
            AxisDirection.NORTH,
            AxisDirection.OTHER,
            AxisDirection.EAST
        });
    }

    /**
     * Sorts the specified axis and compares against the expected result.
     */
    private static void assertOrdered(final CoordinateSystemAxis[] expected,
                                      final CoordinateSystemAxis[] actual)
    {
        final boolean changeExpected = !Arrays.equals(actual, expected);
        assertEquals(changeExpected, Normalizer.sort(actual, 0));
        assertArrayEquals(expected, actual);
    }

    /**
     * Sorts the specified directions and compares against the expected result.
     */
    private static void assertOrdered(final AxisDirection[] expected,
                                      final AxisDirection[] actual)
    {
        assertOrdered(toAxes(expected), toAxes(actual));
    }

    /**
     * Creates axis from the specified directions.
     */
    private static CoordinateSystemAxis[] toAxes(final AxisDirection[] directions) {
        final Map<String,?> properties = singletonMap(NAME_KEY, "Temporary axis");
        final CoordinateSystemAxis[] axis = new CoordinateSystemAxis[directions.length];
        for (int i=0; i<directions.length; i++) {
            axis[i] = new DefaultCoordinateSystemAxis(properties, "none", directions[i], SI.METRE);
        }
        return axis;
    }

    /**
     * Asserts that a collection of pre-defined axes is not modified by the given {@link AxesConvention}.
     */
    private static void assertSameAfterNormalization(final AxisFilter changes) {
        assertSame(HardCodedAxes.GEODETIC_LATITUDE,  Normalizer.normalize(HardCodedAxes.GEODETIC_LATITUDE, changes));
        assertSame(HardCodedAxes.GEODETIC_LONGITUDE, Normalizer.normalize(HardCodedAxes.GEODETIC_LONGITUDE, changes));
        assertSame(HardCodedAxes.EASTING,            Normalizer.normalize(HardCodedAxes.EASTING, changes));
        assertSame(HardCodedAxes.NORTHING,           Normalizer.normalize(HardCodedAxes.NORTHING, changes));
        assertSame(HardCodedAxes.ALTITUDE,           Normalizer.normalize(HardCodedAxes.ALTITUDE, changes));
        assertSame(HardCodedAxes.TIME,               Normalizer.normalize(HardCodedAxes.TIME, changes));
        assertSame(HardCodedAxes.DISTANCE,           Normalizer.normalize(HardCodedAxes.DISTANCE, changes));
    }

    /**
     * Tests {@link Normalizer#normalize(CoordinateSystemAxis, AxisFilter)}.
     */
    @Test
    public void testNormalizeAxis() {
        // Execute twice, first without units normalization, then with units normalization.
        assertSameAfterNormalization(AxesConvention.CONVENTIONALLY_ORIENTED);
        assertSameAfterNormalization(AxesConvention.NORMALIZED);
        /*
         * Test a change of unit from centimetre to metre.
         */
        assertSame(HardCodedAxes.HEIGHT_cm, Normalizer.normalize(HardCodedAxes.HEIGHT_cm,
                AxesConvention.CONVENTIONALLY_ORIENTED));                                   // Do not change unit.
        assertAxisEquals("Height", "h", AxisDirection.UP,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, SI.METRE, null,
                Normalizer.normalize(HardCodedAxes.HEIGHT_cm, AxesConvention.NORMALIZED));
        /*
         * Test a change of direction from West to East.
         */
        assertAxisEquals(Vocabulary.format(Vocabulary.Keys.Unnamed), "E",
                AxisDirection.EAST, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, SI.METRE, null,
                Normalizer.normalize(HardCodedAxes.WESTING, AxesConvention.NORMALIZED));
    }
}
