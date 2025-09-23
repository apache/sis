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
package org.apache.sis.referencing.internal.shared;

import java.time.Instant;
import java.time.Duration;
import org.opengis.metadata.extent.Extent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.metadata.iso.extent.DefaultTemporalExtent;
import org.apache.sis.metadata.iso.extent.DefaultExtent;

// Test dependencies
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link ExtentSelector}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ExtentSelectorTest extends TestCase {
    /**
     * Whether to test an alternate ordering where distance to TOI is tested last.
     *
     * @see ExtentSelector#alternateOrdering
     */
    private boolean alternateOrdering;

    /**
     * The temporal granularity of the Time Of Interest (TOI), or {@code null} if none.
     */
    private Duration granularity;

    /**
     * Creates a new test case.
     */
    public ExtentSelectorTest() {
    }

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
     * Tests using temporal ranges.
     */
    @Test
    @Disabled("Require temporal module, not yet available in SIS.")
    public void testTemporal() {
        assertBestEquals(time(1000, 2000, true), 2,
                         time(1500, 2000, true),
                         time(1300, 1800, false),       // Same duration as above, but better centered.
                         time(1400, 1600, true));       // Well centered but intersection is small.
    }

    /**
     * Tests using temporal ranges with {@link ExtentSelector#alternateOrdering} set to {@code true}.
     * The criterion should give precedence to larger geographic area instead of better centered.
     */
    @Test
    @Disabled("Require temporal module, not yet available in SIS.")
    public void testAlternateOrdering() {
        alternateOrdering = true;
        assertBestEquals(time(1000, 2000, true), 1,
                         time(1500, 2000, true),        // Not well centered by has larger geographic area.
                         time(1300, 1800, false),       // Better centered but smaller geographic area.
                         time(1400, 1600, true));
    }

    /**
     * Tests using temporal ranges with {@link ExtentSelector#setTimeGranularity(Duration)} defined.
     */
    @Test
    @Disabled("Require temporal module, not yet available in SIS.")
    public void testTimeGranularity() {
        granularity = Duration.ofSeconds(20);
        assertBestEquals(time(10000, 70000, true), 3,
                         time(14000, 47000, false),     // 2 units of temporal resolution.
                         time(15000, 46000, true),      // Same size if counted in units of temporal resolution.
                         time(25000, 55000, true));     // Same size in units of resolution, but better centered.
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
     * Creates an extent for a temporal range having the given boundaries.
     * A geographic extent is also added, but may be ignored because temporal extent has precedence.
     *
     * @param startTime  arbitrary start time in milliseconds.
     * @param endTime    arbitrary end time in milliseconds.
     * @param largeArea  {@code false} for associating a small geographic area,
     *                   {@code true} for associating a larger geographic area.
     */
    private static Extent time(final long startTime, final long endTime, final boolean largeArea) {
        final var bbox = new DefaultGeographicBoundingBox(
                largeArea ? -20 : -10, 10,
                largeArea ?  10 :  20, 30);
        final var range = new DefaultTemporalExtent(
                Instant.ofEpochMilli(startTime),
                Instant.ofEpochMilli(endTime));
        return new DefaultExtent(null, bbox, null, range);
    }

    /**
     * Creates the selector to use for testing purpose.
     */
    private ExtentSelector<Integer> create(final Extent aoi) {
        final var selector = new ExtentSelector<Integer>(aoi);
        selector.alternateOrdering = alternateOrdering;
        selector.setTimeGranularity(granularity);
        return selector;
    }

    /**
     * Tests evaluating the <var>a</var>, <var>b</var> and <var>c</var> elements in various order.
     *
     * @param aoi       area of interest to give to {@link ExtentSelector} constructor.
     * @param expected  expected best result: 1 for <var>a</var>, 2 for <var>b</var> or 3 for <var>c</var>.
     */
    private void assertBestEquals(final Extent aoi, final Integer expected,
                                  final Extent a, final Extent b, final Extent c)
    {
        ExtentSelector<Integer> selector = create(aoi);
        selector.evaluate(a, 1);
        selector.evaluate(b, 2);
        selector.evaluate(c, 3);
        assertEquals(expected, selector.best());

        selector = create(aoi);
        selector.evaluate(b, 2);
        selector.evaluate(c, 3);
        selector.evaluate(a, 1);
        assertEquals(expected, selector.best());

        selector = create(aoi);
        selector.evaluate(c, 3);
        selector.evaluate(a, 1);
        selector.evaluate(b, 2);
        assertEquals(expected, selector.best());

        selector = create(aoi);
        selector.evaluate(a, 1);
        selector.evaluate(c, 3);
        selector.evaluate(b, 2);
        assertEquals(expected, selector.best());

        selector = create(aoi);
        selector.evaluate(b, 2);
        selector.evaluate(a, 1);
        selector.evaluate(c, 3);
        assertEquals(expected, selector.best());
    }
}
