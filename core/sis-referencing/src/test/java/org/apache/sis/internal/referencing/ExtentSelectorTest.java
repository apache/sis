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
package org.apache.sis.internal.referencing;

import org.opengis.metadata.extent.Extent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link ExtentSelector}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class ExtentSelectorTest extends TestCase {
    /**
     * Tests the selector when intersection with AOI is a sufficient criterion.
     */
    @Test
    public void testInsideAreaCriterion() {
        assertBestEquals(extent(-20,  30, -10, 5), 2,
                         extent(-30, -17,  -5, 3),
                         extent(-32, -16,  -5, 3),      // Largest area inside.
                         extent( 28,  32,  -5, 3));
    }

    /**
     * Tests the selector where the "area outside" criterion must be used.
     */
    @Test
    public void testOutsideAreaCriterion() {
        assertBestEquals(extent(-20,  30, -10, 5), 2,
                         extent(-25, -15,  -5, 3),
                         extent(-24, -15,  -5, 3),      // Same area inside, smaller area outside.
                         extent(-21, -16,  -5, 3));     // Smallest area outside, but smaller area inside too.
    }

    /**
     * Tests the selector where the "closer to AOI center" criterion must be used.
     */
    @Test
    public void testCentreCriterion() {
        assertBestEquals(extent(-20,  30, -10, 5), 3,
                         extent(-20, -10,  -5, 3),
                         extent(-15,  -5,  -5, 3),      // Closer to centre.
                         extent(  0,  10,  -5, 3));     // Yet closer to centre.
    }

    /**
     * Creates an extent for a geographic bounding box having the given boundaries.
     */
    private static Extent extent(final double westBoundLongitude,
                                 final double eastBoundLongitude,
                                 final double southBoundLatitude,
                                 final double northBoundLatitude)
    {
        return new DefaultExtent(null, new DefaultGeographicBoundingBox(
                westBoundLongitude, eastBoundLongitude,
                southBoundLatitude, northBoundLatitude),
                null, null);
    }

    /**
     * Tests evaluating the <var>a</var>, <var>b</var> and <var>c</var> elements in various order.
     *
     * @param aoi       area of interest to give to {@link ExtentSelector} constructor.
     * @param expected  expected best result: 1 for <var>a</var>, 2 for <var>b</var> or 3 for <var>c</var>.
     */
    private static void assertBestEquals(final Extent aoi, final Integer expected,
                                         final Extent a, final Extent b, final Extent c)
    {
        ExtentSelector<Integer> selector = new ExtentSelector<>(aoi);
        selector.evaluate(a, 1);
        selector.evaluate(b, 2);
        selector.evaluate(c, 3);
        assertEquals(expected, selector.best());

        selector = new ExtentSelector<>(aoi);
        selector.evaluate(b, 2);
        selector.evaluate(c, 3);
        selector.evaluate(a, 1);
        assertEquals(expected, selector.best());

        selector = new ExtentSelector<>(aoi);
        selector.evaluate(c, 3);
        selector.evaluate(a, 1);
        selector.evaluate(b, 2);
        assertEquals(expected, selector.best());

        selector = new ExtentSelector<>(aoi);
        selector.evaluate(a, 1);
        selector.evaluate(c, 3);
        selector.evaluate(b, 2);
        assertEquals(expected, selector.best());

        selector = new ExtentSelector<>(aoi);
        selector.evaluate(b, 2);
        selector.evaluate(a, 1);
        selector.evaluate(c, 3);
        assertEquals(expected, selector.best());
    }
}
