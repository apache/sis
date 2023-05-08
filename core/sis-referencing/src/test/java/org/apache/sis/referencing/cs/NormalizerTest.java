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
import java.util.HashMap;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.referencing.ImmutableIdentifier;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.measure.Units;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.referencing.cs.CoordinateSystem.NAME_KEY;
import static org.junit.Assert.*;
import static org.apache.sis.test.Assertions.assertEqualsIgnoreMetadata;
import static org.apache.sis.referencing.Assertions.assertAxisEquals;


/**
 * Tests the {@link Normalizer} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   0.4
 */
@DependsOn({
    DirectionAlongMeridianTest.class,
    DefaultCoordinateSystemAxisTest.class
})
public final class NormalizerTest extends TestCase {
    /**
     * Tests {@link Normalizer#sort(CoordinateSystemAxis[], int)}
     * with axes of an ellipsoidal coordinate system.
     */
    @Test
    public void testSortEllipsoidalAxes() {
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
    }

    /**
     * Tests {@link Normalizer#sort(CoordinateSystemAxis[], int)}
     * with axes of a Cartesian coordinate system.
     */
    @Test
    public void testSortCartesianAxes() {
        assertOrdered(new AxisDirection[] {
            AxisDirection.EAST,                 // Right handed-rule
            AxisDirection.NORTH,                // Right handed-rule
            AxisDirection.UP
        }, new AxisDirection[] {
            AxisDirection.NORTH,
            AxisDirection.UP,
            AxisDirection.EAST
        });
        assertOrdered(new AxisDirection[] {
            AxisDirection.WEST,                 // Right handed-rule
            AxisDirection.SOUTH,                // Right handed-rule
            AxisDirection.DOWN
        }, new AxisDirection[] {
            AxisDirection.SOUTH,
            AxisDirection.DOWN,
            AxisDirection.WEST
        });
        assertOrdered(new AxisDirection[] {
            AxisDirection.SOUTH,                // Right handed-rule
            AxisDirection.EAST,                 // Right handed-rule
            AxisDirection.DOWN
        }, new AxisDirection[] {
            AxisDirection.SOUTH,
            AxisDirection.DOWN,
            AxisDirection.EAST
        });
    }

    /**
     * Tests {@link Normalizer#sort(CoordinateSystemAxis[], int)}
     * with axes of legacy (WKT 1) axes.
     */
    @Test
    public void testSortWKT1() {
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
     * Tests {@link Normalizer#sort(CoordinateSystemAxis[], int)} with axes of a dummy CS just for testing.
     */
    @Test
    public void testSortMixedAxes() {
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
     * Creates axes from the specified directions.
     */
    private static CoordinateSystemAxis[] toAxes(final AxisDirection[] directions) {
        final Map<String,?> properties = Map.of(NAME_KEY, "Temporary axis");
        final CoordinateSystemAxis[] axis = new CoordinateSystemAxis[directions.length];
        for (int i=0; i<directions.length; i++) {
            axis[i] = new DefaultCoordinateSystemAxis(properties, "none", directions[i], Units.METRE);
        }
        return axis;
    }

    /**
     * Asserts that a collection of predefined axes is not modified by the given {@link AxesConvention}.
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
     * Tests {@link Normalizer#normalize(CoordinateSystemAxis, AxisFilter)} for axis directions.
     * Units are left unchanged.
     */
    @Test
    public void testNormalizeAxisDirection() {
        assertSameAfterNormalization(AxesConvention.DISPLAY_ORIENTED);
        /*
         * Test a change of direction from West to East.
         */
        assertAxisEquals(Vocabulary.format(Vocabulary.Keys.Unnamed), "E",
                AxisDirection.EAST, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Units.METRE, null,
                Normalizer.normalize(HardCodedAxes.WESTING, AxesConvention.NORMALIZED));
    }

    /**
     * Tests {@link Normalizer#normalize(CoordinateSystemAxis, AxisFilter)} for axis units and directions.
     */
    @Test
    public void testNormalizeAxisUnitAndDirection() {
        assertSameAfterNormalization(AxesConvention.NORMALIZED);
        /*
         * Test a change of unit from centimetre to metre.
         */
        assertSame(HardCodedAxes.HEIGHT_cm, Normalizer.normalize(HardCodedAxes.HEIGHT_cm,
                AxesConvention.DISPLAY_ORIENTED));                                          // Do not change unit.
        assertAxisEquals("Height", "h", AxisDirection.UP,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Units.METRE, null,
                Normalizer.normalize(HardCodedAxes.HEIGHT_cm, AxesConvention.NORMALIZED));
    }

    /**
     * Tests normalization of an ellipsoidal CS. The axes used in this test do not contain any EPSG code.
     * Consequently, the {@link Normalizer#normalize(CoordinateSystem, AxisFilter, boolean)} method should
     * be able to reuse them as-is even if axis order changed.
     */
    @Test
    public void testNormalize() {
        final DefaultEllipsoidalCS cs = new DefaultEllipsoidalCS(
                Map.of(DefaultEllipsoidalCS.NAME_KEY, "lat lon height"),
                HardCodedAxes.GEODETIC_LATITUDE,
                HardCodedAxes.GEODETIC_LONGITUDE,
                HardCodedAxes.ELLIPSOIDAL_HEIGHT);
        final AbstractCS normalized = Normalizer.forConvention(cs, AxesConvention.RIGHT_HANDED);
        assertEquals("name", "Ellipsoidal CS: East (°), North (°), Up (m).", String.valueOf(normalized.getName()));
        /*
         * Longitude and latitude axes shall be interchanged. Since they have no EPSG code, there
         * is no need to create new CoordinateSystemAxis instances; the same ones can be reused.
         */
        assertSame("Latitude",  cs.getAxis(0), normalized.getAxis(1));
        assertSame("Longitude", cs.getAxis(1), normalized.getAxis(0));
        assertSame("Height",    cs.getAxis(2), normalized.getAxis(2));
    }

    /**
     * Tests normalization of an ellipsoidal CS with EPSG codes. This test first creates the axes
     * of EPSG::6423 coordinate system, then reorder axes. Since axis EPSG codes differ depending
     * on axis order, this test verifies that axis EPSG codes has been removed.
     */
    @Test
    public void testIdentifierRemoval() {
        final DefaultEllipsoidalCS cs = new DefaultEllipsoidalCS(           // EPSG::6423
                Map.of(DefaultEllipsoidalCS.NAME_KEY, "lat lon height"),
                addIdentifier(HardCodedAxes.GEODETIC_LATITUDE,  (short) 108),
                addIdentifier(HardCodedAxes.GEODETIC_LONGITUDE, (short) 109),
                addIdentifier(HardCodedAxes.ELLIPSOIDAL_HEIGHT, (short) 110));
        final AbstractCS normalized = Normalizer.forConvention(cs, AxesConvention.RIGHT_HANDED);
        assertEquals("name", "Ellipsoidal CS: East (°), North (°), Up (m).", String.valueOf(normalized.getName()));
        /*
         * Longitude and latitude axes shall be interchanged. In addition of that, since the EPSG codes
         * need to be removed, new CoordinateSystemAxis instances shall have been created except for
         * ellipsoidal height, because its position did not changed.
         */
        assertIdentifierRemoved(cs.getAxis(1), normalized.getAxis(0));
        assertIdentifierRemoved(cs.getAxis(0), normalized.getAxis(1));
        assertSame("Height",    cs.getAxis(2), normalized.getAxis(2));
        /*
         * The HardCodedAxes constants have no EPSG identifiers, so we can compare the normalized axes
         * with those constants for equality.
         */
        assertEquals("Longitude", HardCodedAxes.GEODETIC_LONGITUDE, normalized.getAxis(0));
        assertEquals("Latitude",  HardCodedAxes.GEODETIC_LATITUDE,  normalized.getAxis(1));
    }

    /**
     * Creates an axis identical to the given one with an EPSG code added.
     * This is a helper method for {@link #testIdentifierRemoval()}.
     */
    private static CoordinateSystemAxis addIdentifier(final CoordinateSystemAxis axis, final short epsg) {
        final Map<String,Object> properties = new HashMap<>(8);
        properties.putAll(IdentifiedObjects.getProperties(axis));
        properties.put(DefaultCoordinateSystemAxis.IDENTIFIERS_KEY,   new ImmutableIdentifier(null, "EPSG", String.valueOf(epsg)));
        properties.put(DefaultCoordinateSystemAxis.MINIMUM_VALUE_KEY, axis.getMinimumValue());
        properties.put(DefaultCoordinateSystemAxis.MAXIMUM_VALUE_KEY, axis.getMaximumValue());
        properties.put(DefaultCoordinateSystemAxis.RANGE_MEANING_KEY, axis.getRangeMeaning());
        return new DefaultCoordinateSystemAxis(properties, axis.getAbbreviation(), axis.getDirection(), axis.getUnit());
    }

    /**
     * Verifies that an EPSG identifier added by {@link #addIdentifier(CoordinateSystemAxis, short)} has been removed
     * after the axes have been reordered.
     *
     * @param  original    the axis with EPSG identifier before normalization.
     * @param  normalized  the axis after normalization, in which we expect the EPSG identifier to have been removed.
     */
    private static void assertIdentifierRemoved(final CoordinateSystemAxis original, final CoordinateSystemAxis normalized) {
        assertNotSame  (original, normalized);
        assertNotEquals(original, normalized);
        assertFalse("identifiers.isEmpty()",   original.getIdentifiers().isEmpty());
        assertTrue ("identifiers.isEmpty()", normalized.getIdentifiers().isEmpty());
        assertEqualsIgnoreMetadata(original, normalized);
    }
}
