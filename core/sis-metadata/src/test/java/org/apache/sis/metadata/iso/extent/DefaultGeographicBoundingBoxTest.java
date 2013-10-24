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
     */
    private static void assertBoxEquals(final double westBoundLongitude,
                                        final double eastBoundLongitude,
                                        final double southBoundLatitude,
                                        final double northBoundLatitude,
                                        final GeographicBoundingBox box)
    {
        assertEquals("inclusion", Boolean.TRUE, box.getInclusion());
        assertEquals("westBoundLongitude", westBoundLongitude, box.getWestBoundLongitude(), 0);
        assertEquals("eastBoundLongitude", eastBoundLongitude, box.getEastBoundLongitude(), 0);
        assertEquals("southBoundLatitude", southBoundLatitude, box.getSouthBoundLatitude(), 0);
        assertEquals("northBoundLatitude", northBoundLatitude, box.getNorthBoundLatitude(), 0);
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
        box.setBounds  (-180,  +20, -8, 2); assertBoxEquals(-180,  +20, -8, 2, box); // Already normalized.
        box.setBounds  ( +20, +180, -8, 2); assertBoxEquals( +20, +180, -8, 2, box); // Already normalized.
        box.setBounds  (+180,  +20, -8, 2); assertBoxEquals(-180,  +20, -8, 2, box); // Normalize west bound so we don't need to span anti-meridian.
        box.setBounds  ( +20, -180, -8, 2); assertBoxEquals( +20, +180, -8, 2, box); // Normalize east bound so we don't need to span anti-meridian.
        box.setBounds  (-180, -180, -8, 2); assertBoxEquals(-180, -180, -8, 2, box); // Empty box shall stay empty.
        box.setBounds  (+180, +180, -8, 2); assertBoxEquals(-180, -180, -8, 2, box); // Empty box shall stay empty.
        /*
         * Special care for the ±0° longitude bounds.
         */
        box.setBounds  (+0.0, +0.0, -8, 2); assertBoxEquals(+0.0, +0.0, -8, 2, box);
        box.setBounds  (-0.0, +0.0, -8, 2); assertBoxEquals(-0.0, +0.0, -8, 2, box);
        box.setBounds  (-0.0, -0.0, -8, 2); assertBoxEquals(-0.0, -0.0, -8, 2, box);
        box.setBounds  (+0.0, -0.0, -8, 2); assertBoxEquals(-180, +180, -8, 2, box);
    }


}
