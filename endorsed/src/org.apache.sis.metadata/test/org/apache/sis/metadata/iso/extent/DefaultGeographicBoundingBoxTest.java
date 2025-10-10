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
package org.apache.sis.metadata.iso.extent;

import java.util.Map;
import static java.lang.Double.NaN;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertMultilinesEquals;
import static org.apache.sis.test.Assertions.assertSingleton;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link DefaultGeographicBoundingBox}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class DefaultGeographicBoundingBoxTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultGeographicBoundingBoxTest() {
    }

    /**
     * Asserts that the given geographic bounding box is strictly equals to the given values.
     * The {@link GeographicBoundingBox#getInclusion()} is expected to be {@code true}.
     */
    private static void assertBoxEquals(final double λbgn, final double λend,
                                        final double φmin, final double φmax,
                                        final GeographicBoundingBox box)
    {
        assertEquals(Boolean.TRUE, box.getInclusion(),  "inclusion");
        assertEquals(λbgn, box.getWestBoundLongitude(), "westBoundLongitude");
        assertEquals(λend, box.getEastBoundLongitude(), "eastBoundLongitude");
        assertEquals(φmin, box.getSouthBoundLatitude(), "southBoundLatitude");
        assertEquals(φmax, box.getNorthBoundLatitude(), "northBoundLatitude");
    }

    /**
     * Returns {@code true} if the given box is crossing over the anti-meridian.
     */
    static boolean isCrossingAntiMeridian(final GeographicBoundingBox box) {
        return box.getWestBoundLongitude() > box.getEastBoundLongitude();
    }

    /**
     * Tests construction with an invalid range of latitudes.
     */
    @Test
    public void testInvalidLatitudeRange() {
        var e = assertThrows(IllegalArgumentException.class, () -> new DefaultGeographicBoundingBox(-1, +1, 12, 10));
        assertMessageContains(e);
    }

    /**
     * Tests {@link DefaultGeographicBoundingBox#normalize()}. This is also an indirect test of
     * {@link DefaultGeographicBoundingBox#DefaultGeographicBoundingBox(double, double, double, double)} and
     * {@link DefaultGeographicBoundingBox#setBounds(double, double, double, double)}, but those later methods
     * are quite trivial except for the call to {@code normalize()}.
     */
    @Test
    public void testNormalize() {
        final DefaultGeographicBoundingBox box = new DefaultGeographicBoundingBox(-180, +180, -90, +90);
        assertBoxEquals(-180, +180, -90, +90, box);
        assertFalse(isCrossingAntiMeridian(box));
        /*
         * Span more than the whole Earth.
         */
        box.setBounds  (-200, +200, -100, +100);
        assertBoxEquals(-180, +180,  -90,  +90, box);
        assertFalse(isCrossingAntiMeridian(box));
        /*
         * Values in a shifted range, but without anti-meridian crossing.
         */
        box.setBounds  (380, 420, -8, 2);
        assertBoxEquals( 20,  60, -8, 2, box);
        assertFalse(isCrossingAntiMeridian(box));
        /*
         * Anti-meridian crossing, without change needed.
         */
        box.setBounds  ( 160, -170, -8, 2);
        assertBoxEquals( 160, -170, -8, 2, box);
        assertTrue(isCrossingAntiMeridian(box));
        /*
         * Anti-meridian crossing in the [0 … 360]° range.
         */
        box.setBounds  ( 160,  190, -8, 2);
        assertBoxEquals( 160, -170, -8, 2, box);
        assertTrue(isCrossingAntiMeridian(box));
        /*
         * Random anti-meridian crossing outside of range.
         */
        box.setBounds  (-200, +20, -8, 2);
        assertBoxEquals( 160, +20, -8, 2, box);
        assertTrue(isCrossingAntiMeridian(box));
        /*
         * Special care for the ±180° longitude bounds.
         */
        box.setBounds(-180,  +20, -8, 2); assertBoxEquals(-180,  +20, -8, 2, box); // Already normalized.
        box.setBounds( +20, +180, -8, 2); assertBoxEquals( +20, +180, -8, 2, box); // Already normalized.
        box.setBounds(+180,  +20, -8, 2); assertBoxEquals(-180,  +20, -8, 2, box); // Normalize west bound so we don't need to span anti-meridian.
        box.setBounds( +20, -180, -8, 2); assertBoxEquals( +20, +180, -8, 2, box); // Normalize east bound so we don't need to span anti-meridian.
        box.setBounds(-180, -180, -8, 2); assertBoxEquals(-180, -180, -8, 2, box); // Empty box shall stay empty.
        box.setBounds(+180, +180, -8, 2); assertBoxEquals(-180, -180, -8, 2, box); // Empty box shall stay empty.
        /*
         * Special care for the ±0° longitude bounds.
         */
        box.setBounds(+0.0, +0.0, -8, 2); assertBoxEquals(+0.0, +0.0, -8, 2, box);
        box.setBounds(-0.0, +0.0, -8, 2); assertBoxEquals(-0.0, +0.0, -8, 2, box);
        box.setBounds(-0.0, -0.0, -8, 2); assertBoxEquals(-0.0, -0.0, -8, 2, box);
        box.setBounds(+0.0, -0.0, -8, 2); assertBoxEquals(-180, +180, -8, 2, box);
    }

    /**
     * Tests {@link DefaultGeographicBoundingBox#add(GeographicBoundingBox)}.
     */
    @Test
    public void testAdd() {
        testOperation(true);
    }

    /**
     * Tests {@link DefaultGeographicBoundingBox#intersect(GeographicBoundingBox)}.
     */
    @Test
    public void testIntersect() {
        testOperation(false);
    }

    /**
     * Implementation of {@link #testAdd()} and {@link #testIntersect()}.
     *
     * @param union {@code true} for {@code b1.add(b2)}, or {@code false} for {@code b1.intersect(b2)}.
     */
    private void testOperation(final boolean union) {
        final DefaultGeographicBoundingBox b1 = new DefaultGeographicBoundingBox(NaN, 20, -20, NaN);
        final DefaultGeographicBoundingBox b2 = new DefaultGeographicBoundingBox(-20, 20, NaN,  20);
        assertFalse(isCrossingAntiMeridian(b1));
        assertFalse(isCrossingAntiMeridian(b2));
        assertOperationEquals(union, NaN, 20, NaN, NaN, b1, b2);
        /*
         *    ┌─────────────┐
         *    │  ┌───────┐  │
         *    │  └───────┘  │
         *    └─────────────┘
         */
        double λbgn, λend, φmin, φmax;
        b1.setBounds(-40, 30, -38,  20);
        b2.setBounds(-20, 10, -30, -25);
        assertFalse(isCrossingAntiMeridian(b1));
        assertFalse(isCrossingAntiMeridian(b2));
        if (union) {
            λbgn = -40; φmin = -38;
            λend =  30; φmax =  20;
        } else {
            λbgn = -20; φmin = -30;
            λend =  10; φmax = -25;
        }
        assertOperationEquals(union, λbgn, λend, φmin,  φmax, b1, b2);
        /*
         *    ┌──────────┐
         *    │  ┌───────┼──┐
         *    └──┼───────┘  │
         *       └──────────┘
         */
        b1.setBounds(-40, 30, -38,  20);
        b2.setBounds(-30, 50, -42, -20);
        assertFalse(isCrossingAntiMeridian(b1));
        assertFalse(isCrossingAntiMeridian(b2));
        if (union) {
            λbgn = -40; φmin = -42;
            λend =  50; φmax =  20;
        } else {
            λbgn = -30; φmin = -38;
            λend =  30; φmax = -20;
        }
        assertOperationEquals(union, λbgn, λend, φmin,  φmax, b1, b2);
        /*
         *   ┌─────────┐
         *   │         │  ┌─────┐
         *   │         │  └─────┘
         *   └─────────┘
         */
        b1.setBounds(-40, 30, -80, 40);
        b2.setBounds( 50, 80, -30, 20);
        assertFalse(isCrossingAntiMeridian(b1));
        assertFalse(isCrossingAntiMeridian(b2));
        if (union) {
            λbgn = -40; φmin = -80;
            λend =  80; φmax =  40;
        } else {
            λbgn = NaN; φmin = -30;
            λend = NaN; φmax =  20;
        }
        assertOperationEquals(union, λbgn, λend, φmin,  φmax, b1, b2);
        /*
         *   ──────────┐  ┌─────
         *     ┌────┐  │  │
         *     └────┘  │  │
         *   ──────────┘  └─────
         */
        b1.setBounds(  80, -100, -2, 2);
        b2.setBounds(-140, -120, -1, 1);
        assertTrue (isCrossingAntiMeridian(b1));
        assertFalse(isCrossingAntiMeridian(b2));
        if (union) {
            λbgn =   80; φmin = -2;
            λend = -100; φmax =  2;
        } else {
            λbgn = -140; φmin = -1;
            λend = -120; φmax =  1;
        }
        assertOperationEquals(union, λbgn, λend, φmin,  φmax, b1, b2);
        /*
         *    ─────┐      ┌─────
         *       ┌─┼────┐ │
         *       └─┼────┘ │
         *    ─────┘      └─────
         */
        b1.setBounds(  80, -100, -2, 2);
        b2.setBounds(-120,   50, -1, 1);
        assertTrue (isCrossingAntiMeridian(b1));
        assertFalse(isCrossingAntiMeridian(b2));
        if (union) {
            λbgn =   80;
            λend =   50;
        } else {
            λbgn = -120;
            λend = -100;
        }
        assertOperationEquals(union, λbgn, λend, φmin,  φmax, b1, b2);
        /*
         *    ────┐  ┌────
         *     ┌──┼──┼─┐
         *     └──┼──┼─┘
         *    ────┘  └────
         */
        b1.setBounds(  80, -100, -2, 2);
        b2.setBounds(-120,   90, -1, 1);
        assertTrue (isCrossingAntiMeridian(b1));
        assertFalse(isCrossingAntiMeridian(b2));
        if (union) {
            λbgn = -180;
            λend =  180;
        } else {
            // Intersection unchanged.
        }
        assertOperationEquals(union, λbgn, λend, φmin,  φmax, b1, b2);
        /*
         *    ────┐  ┌────
         *    ──┐ │  │ ┌──
         *    ──┘ │  │ └──
         *    ────┘  └────
         */
        b1.setBounds(80, -100, -1, 1);
        b2.setBounds(90, -120, -2, 2);
        assertTrue(isCrossingAntiMeridian(b1));
        assertTrue(isCrossingAntiMeridian(b2));
        if (union) {
            λbgn =   80;
            λend = -100;
        } else {
            λbgn =   90;
            λend = -120;
        }
        assertOperationEquals(union, λbgn, λend, φmin,  φmax, b1, b2);
        /*
         *    ────┐  ┌────
         *    ────┼──┼─┐┌─
         *    ────┼──┼─┘└─
         *    ────┘  └────
         */
        b1.setBounds( 80, -100, -2, 2);
        b2.setBounds(100,   90, -1, 1);
        assertTrue(isCrossingAntiMeridian(b1));
        assertTrue(isCrossingAntiMeridian(b2));
        if (union) {
            λbgn =  -180;
            λend =   180;
        } else {
            λbgn =  100;
            λend = -100;
        }
        assertOperationEquals(union, λbgn, λend, φmin,  φmax, b1, b2);
        /*
         *    ────┐          ┌────
         *        │  ┌────┐  │
         *        │  └────┘  │
         *    ────┘          └────
         */
        b1.setBounds(120, -110, -1, 1);
        b2.setBounds(100,  112, -2, 2);
        assertTrue (isCrossingAntiMeridian(b1));
        assertFalse(isCrossingAntiMeridian(b2));
        if (union) {
            λbgn =  100;
            λend = -110;
        } else {
            λbgn =  NaN;
            λend =  NaN;
        }
        assertOperationEquals(union, λbgn, λend, φmin,  φmax, b1, b2);
    }

    /**
     * Asserts that the result of applying the {@code add} or {@code intersect} operation on {@code b1}
     * is equal to the given values. This method tests also with horizontally flipped boxes, and tests
     * with interchanged boxes.
     *
     * @param union {@code true} for {@code b1.add(b2)}, or {@code false} for {@code b1.intersect(b2)}.
     */
    private static void assertOperationEquals(final boolean union,
            final double λbgn, final double λend,
            final double φmin, final double φmax,
            final DefaultGeographicBoundingBox b1,
            final DefaultGeographicBoundingBox b2)
    {
        final double westBoundLongitude = b1.getWestBoundLongitude();
        final double eastBoundLongitude = b1.getEastBoundLongitude();
        final double southBoundLatitude = b1.getSouthBoundLatitude();
        final double northBoundLatitude = b1.getNorthBoundLatitude();
        applyOperation(union, b1, b2);
        assertBoxEquals(λbgn, λend, φmin, φmax, b1);
        /*
         * The above tested the boxes as given in argument to this method. Now test again,
         * but with horizontally flipped boxes - so this is a mirror of above test. A test
         * failure here would mean that there is some asymmetry in the code.
         */
        flipHorizontally(b2);
        b1.setBounds(-eastBoundLongitude, -westBoundLongitude, southBoundLatitude, northBoundLatitude);
        applyOperation(union, b1, b2);
        assertBoxEquals(-λend, -λbgn, φmin, φmax, b1);
        /*
         * Reset the boxes to there initial state, then test again with the two boxes interchanged.
         * The consequence for the implementation is not as symmetric than the above test, so there
         * is more risk of failure here.
         */
        flipHorizontally(b2);
        b1.setBounds(westBoundLongitude, eastBoundLongitude, southBoundLatitude, northBoundLatitude);
        applyOperation(union, b2, b1);
        assertBoxEquals(λbgn, λend, φmin, φmax, b2);
        /*
         * Following should be equivalent to b1.setBounds(b2), tested opportunistically.
         */
        applyOperation(union, b1, b2);
        assertBoxEquals(λbgn, λend, φmin, φmax, b1);
        assertEquals(b1, b2);
    }

    /**
     * Flips the given box horizontally. Longitudes are interchanged and their sign reversed.
     * Union and intersection tests what worked with the given boxes shall work as well with flipped boxes.
     */
    private static void flipHorizontally(final DefaultGeographicBoundingBox box) {
        box.setBounds(-box.getEastBoundLongitude(),
                      -box.getWestBoundLongitude(),
                       box.getSouthBoundLatitude(),
                       box.getNorthBoundLatitude());
    }

    /**
     * Applies the given operation on the given bounding boxes.
     * The operation is invoked on {@code b1}.
     *
     * @param union {@code true} for {@code b1.add(b2)}, or {@code false} for {@code b1.intersect(b2)}.
     */
    private static void applyOperation(final boolean union,
            final DefaultGeographicBoundingBox b1,
            final DefaultGeographicBoundingBox b2)
    {
        assertNotSame(b1, b2);
        if (union) {
            b1.add(b2);
        } else {
            b1.intersect(b2);
        }
    }

    /**
     * Tests {@link DefaultGeographicBoundingBox#asMap()}. The longitude and latitude values are
     * expected to be represented by {@link Longitude} and {@link Latitude} objects respectively.
     */
    @Test
    public void testAsMap() {
        final Map<String, Object> map = new DefaultGeographicBoundingBox(-40, 50, -20, 45).asMap();
        assertArrayEquals(new String[] {
            "westBoundLongitude",
            "eastBoundLongitude",
            "southBoundLatitude",
            "northBoundLatitude",
            "inclusion"
        }, map.keySet().toArray());
        assertArrayEquals(new Object[] {
            new Longitude(-40),
            new Longitude(+50),
            new Latitude (-20),
            new Latitude (+45),
            Boolean.TRUE
        }, map.values().toArray());
    }

    /**
     * Tests the {@code toString()} implementation of a custom geographic bounding box inside a {@link DefaultExtent}.
     * In a previous Apache SIS version, those properties were not properly sorted.
     */
    @Test
    public void testToString() {
        final GeographicBoundingBox bbox = new GeographicBoundingBox() {
            @Override public double getWestBoundLongitude() {return -40;}
            @Override public double getEastBoundLongitude() {return  50;}
            @Override public double getSouthBoundLatitude() {return -20;}
            @Override public double getNorthBoundLatitude() {return  45;}
            @Override public Boolean getInclusion() {return Boolean.TRUE;}
        };
        final var extent = new DefaultExtent(null, bbox, null, null);
        assertSame(bbox, assertSingleton(extent.getGeographicElements()));
        assertMultilinesEquals(
                "Extent\n" +
                "  └─Geographic element\n" +
                "      ├─West bound longitude…… 40°W\n" +
                "      ├─East bound longitude…… 50°E\n" +
                "      ├─South bound latitude…… 20°S\n" +
                "      ├─North bound latitude…… 45°N\n" +
                "      └─Extent type code……………… True\n",
                TestUtilities.formatMetadata(extent.asTreeTable()));
    }
}
