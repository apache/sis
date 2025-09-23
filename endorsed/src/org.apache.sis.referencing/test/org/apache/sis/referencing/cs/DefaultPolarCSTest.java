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
import org.opengis.referencing.cs.RangeMeaning;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;

// Specific to the main branch:
import org.apache.sis.referencing.internal.shared.AxisDirections;
import static org.apache.sis.test.GeoapiAssert.assertAxisDirectionsEqual;


/**
 * Tests {@link DefaultPolarCS}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DefaultPolarCSTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultPolarCSTest() {
    }

    /**
     * Tests {@link DefaultPolarCS#forConvention(AxesConvention)}
     * with a change from clockwise to counterclockwise axis orientation.
     */
    @Test
    public void testChangeClockwiseOrientation() {
        final DefaultPolarCS cs = HardCodedCS.POLAR;
        final DefaultPolarCS normalized = cs.forConvention(AxesConvention.DISPLAY_ORIENTED);
        assertNotSame(cs, normalized);
        assertAxisDirectionsEqual(normalized,
                AxisDirections.AWAY_FROM,
                AxisDirections.COUNTER_CLOCKWISE);
    }

    /**
     * Tests {@link DefaultPolarCS#forConvention(AxesConvention)} with a change of axis order.
     * This test uses a (r) axis oriented toward South instead of "awayFrom".
     */
    @Test
    public void testChangeAxisOrder() {
        final DefaultCoordinateSystemAxis radius = HardCodedAxes.create("Radius", "r",
                AxisDirection.SOUTH, Units.METRE, 0, Double.POSITIVE_INFINITY, RangeMeaning.EXACT);

        final DefaultPolarCS cs = new DefaultPolarCS(
                Map.of(DefaultPolarCS.NAME_KEY, "Polar"),
                HardCodedAxes.BEARING,
                radius);

        DefaultPolarCS normalized = cs.forConvention(AxesConvention.RIGHT_HANDED);
        assertAxisDirectionsEqual(normalized,
                AxisDirections.CLOCKWISE,
                AxisDirection.SOUTH);
        assertSame(cs, normalized);

        normalized = cs.forConvention(AxesConvention.NORMALIZED);
        assertNotSame(cs, normalized);
        assertAxisDirectionsEqual(normalized,
                AxisDirection.SOUTH,                            // Not modified to North because radius cannot be negative.
                AxisDirections.COUNTER_CLOCKWISE);
    }
}
