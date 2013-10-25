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

import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link DefaultGeographicBoundingBox}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class DefaultGeographicBoundingBoxTest extends TestCase {
    /**
     * Asserts that the given geographic bounding box is strictly equals to the given values.
     * The {@link GeographicBoundingBox#getInclusion()} is expected to be {@code true}.
     */
    private static void assertBoxEquals(final double λbgn, final double λend,
                                        final double φmin, final double φmax,
                                        final GeographicBoundingBox box)
    {
        assertEquals("inclusion", Boolean.TRUE, box.getInclusion());
        assertEquals("westBoundLongitude", λbgn, box.getWestBoundLongitude(), 0);
        assertEquals("eastBoundLongitude", λend, box.getEastBoundLongitude(), 0);
        assertEquals("southBoundLatitude", φmin, box.getSouthBoundLatitude(), 0);
        assertEquals("northBoundLatitude", φmax, box.getNorthBoundLatitude(), 0);
    }

    /**
     * Tests construction with an invalid range of latitudes.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidLatitudeRange() {
        new DefaultGeographicBoundingBox(-1, +1, 12, 10);
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
        /*
         * Span more than the whole Earth.
         */
        box.setBounds  (-200, +200, -100, +100);
        assertBoxEquals(-180, +180,  -90,  +90, box);
        /*
         * Values in a shifted range, but without anti-meridian spanning.
         */
        box.setBounds  (380, 420, -8, 2);
        assertBoxEquals( 20,  60, -8, 2, box);
        /*
         * Anti-meridian spanning, without change needed.
         */
        box.setBounds  ( 160, -170, -8, 2);
        assertBoxEquals( 160, -170, -8, 2, box);
        /*
         * Anti-meridian spanning in the [0 … 360]° range.
         */
        box.setBounds  ( 160,  190, -8, 2);
        assertBoxEquals( 160, -170, -8, 2, box);
        /*
         * Random anti-meridian spanning outside of range.
         */
        box.setBounds  (-200, +20, -8, 2);
        assertBoxEquals( 160, +20, -8, 2, box);
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
     * Sets the given box to the given values, and verifies if the box spans or not the anti-meridian as expected.
     * This is a convenience method for the {@link #testAdd()} and {@link #testIntersect()} methods for checking
     * that we are really testing the case that we intended to test.
     *
     * @param isSpanningAntiMeridian {@code true} if the box shall spans the anti-meridian.
     * @param box The box to set. Previous values will be overwritten.
     */
    private static void setBounds(final boolean isSpanningAntiMeridian,
                                  final double λbgn, final double λend,
                                  final double φmin, final double φmax,
                                  final DefaultGeographicBoundingBox box)
    {
        box.setBounds(λbgn, λend, φmin, φmax);
        assertEquals("isSpanningAntiMeridian", isSpanningAntiMeridian,
                box.getWestBoundLongitude() > box.getEastBoundLongitude());
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
     * Tests {@link DefaultGeographicBoundingBox#add(GeographicBoundingBox)}.
     */
    @Test
    @DependsOnMethod("testNormalize")
    public void testAdd() {
        /*
         * Simple cases: no anti-meridian spanning.
         *    ┌─────────────┐           ┌──────────┐
         *    │  ┌───────┐  │    and    │  ┌───────┼──┐
         *    │  └───────┘  │           └──┼───────┘  │
         *    └─────────────┘              └──────────┘
         *  -40             30         -40            50
         */
        final DefaultGeographicBoundingBox b1 = new DefaultGeographicBoundingBox(-40, 30, -38,  20);
        final DefaultGeographicBoundingBox b2 = new DefaultGeographicBoundingBox(-20, 10, -30, -25);
        assertAddEquals( -40, 30, -38,  20, b1, b2);
        setBounds(false, -30, 50, -42, -20, b2);
        assertAddEquals( -40, 50, -42,  20, b1, b2);
        /*
         * Anti-meridian spanning for only one box.
         *   ──────────┐  ┌─────           ─────┐      ┌─────
         *     ┌────┐  │  │         and       ┌─┼────┐ │
         *     └────┘  │  │                   └─┼────┘ │
         *   ──────────┘  └─────           ─────┘      └─────
         */
        setBounds(true,    80, -100, -2, 2, b1);
        setBounds(false, -140, -120, -1, 1, b2);
        assertAddEquals(   80, -100, -2, 2, b1, b2);
        setBounds(false, -120,   50, -1, 1, b2);
        assertAddEquals(   80,   50, -2, 2, b1, b2);
        /*
         * To be replaced by the whole world:
         *    ────┐  ┌────
         *     ┌──┼──┼─┐
         *     └──┼──┼─┘
         *    ────┘  └────
         */
        setBounds(true,    80, -100, -2, 2, b1);
        setBounds(false, -120,   90, -1, 1, b2);
        assertAddEquals( -180,  180, -2, 2, b1, b2);
        /*
         * Anti-meridian spanning in both boxes.
         * The second case shall result in a box spanning the whole world.
         *
         *    ────┐  ┌────           ────┐  ┌────
         *    ──┐ │  │ ┌──    and    ────┼──┼─┐┌─
         *    ──┘ │  │ └──           ────┼──┼─┘└─
         *    ────┘  └────           ────┘  └────
         */
        setBounds(true,   80, -100, -1, 1, b1);
        setBounds(true,   90, -120, -2, 2, b2);
        assertAddEquals(  80, -100, -2, 2, b1, b2);
        setBounds(true,  100,   90, -1, 1, b2);
        assertAddEquals(-180,  180, -2, 2, b1, b2);
    }

    /**
     * Asserts that the call to {@code b1.add(b2)} and {@code b2.add(b1)} is strictly equals to the given values.
     * This method tests also with horizontally flipped boxes, and tests with interchanged boxes ({@code b1.add(b2)}).
     * After this method call, the two boxes are equal to the given values.
     */
    private static void assertAddEquals(final double λbgn, final double λend,
                                        final double φmin, final double φmax,
                                        final DefaultGeographicBoundingBox b1,
                                        final DefaultGeographicBoundingBox b2)
    {
        final double westBoundLongitude = b1.getWestBoundLongitude();
        final double eastBoundLongitude = b1.getEastBoundLongitude();
        final double southBoundLatitude = b1.getSouthBoundLatitude();
        final double northBoundLatitude = b1.getNorthBoundLatitude();
        b1.add(b2);
        assertBoxEquals(λbgn, λend, φmin, φmax, b1);
        /*
         * The above tested the boxes as given in argument to this method. Now test again,
         * but with horizontally flipped boxes - so this is a mirror of above test. A test
         * failure here would mean that there is some asymmetry in the code.
         */
        flipHorizontally(b2);
        b1.setBounds(-eastBoundLongitude, -westBoundLongitude, southBoundLatitude, northBoundLatitude);
        b1.add(b2);
        assertBoxEquals(-λend, -λbgn, φmin, φmax, b1);
        /*
         * Reset the boxes to there initial state, then test again with the two boxes interchanged.
         * The consequence for the implementation is not as smetric than the above test, so there
         * is more risk of failure here.
         */
        flipHorizontally(b2);
        b1.setBounds(westBoundLongitude, eastBoundLongitude, southBoundLatitude, northBoundLatitude);
        b2.add(b1);
        assertBoxEquals(λbgn, λend, φmin, φmax, b2);
        /*
         * Following should be equivalent to b1.setBounds(b2), tested opportunistically.
         */
        b1.add(b2);
        assertBoxEquals(λbgn, λend, φmin, φmax, b1);
        assertEquals(b1, b2);
    }
}
