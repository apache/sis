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

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;

// Specific to the main branch:
import org.apache.sis.referencing.internal.shared.AxisDirections;
import static org.apache.sis.test.GeoapiAssert.assertAxisDirectionsEqual;


/**
 * Tests {@link DefaultSphericalCS}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DefaultSphericalCSTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultSphericalCSTest() {
    }

    /**
     * Tests a spherical CRS conforms to EPSG:8.9:6404 definition.
     * Expected axes are:
     *
     * <ol>
     *   <li>Spherical latitude (Ω)</li>
     *   <li>Spherical longitude (θ)</li>
     *   <li>Geocentric radius (R)</li>
     * </ol>
     */
    @Test
    public void testGeodetic() {
        final DefaultSphericalCS cs = HardCodedCS.SPHERICAL;
        assertEquals("r", cs.getAxis(2).getAbbreviation());     // EPSG abbreviation for geocentric radius should be lower-case

        final DefaultSphericalCS normalized = cs.forConvention(AxesConvention.DISPLAY_ORIENTED);
        assertNotSame(cs, normalized, "Should create a new CoordinateSystem.");
        assertAxisDirectionsEqual(normalized,
                AxisDirection.EAST,
                AxisDirection.NORTH,
                AxisDirection.UP);

        assertEquals(new DefaultSphericalCS(
                Map.of(AbstractCS.NAME_KEY, "Spherical CS: East (°), North (°), Up (m)."),
                HardCodedAxes.SPHERICAL_LONGITUDE,
                HardCodedAxes.SPHERICAL_LATITUDE,
                HardCodedAxes.GEOCENTRIC_RADIUS), normalized);
    }

    /**
     * Tests a spherical CRS conforms to the example given in ISO 19162.
     * Expected axes are:
     *
     * <ol>
     *   <li>Distance (r)</li>
     *   <li>Longitude</li>
     *   <li>Elevation</li>
     * </ol>
     *
     * This order is not exactly the usual engineering axis order.
     * But this is the order expected by the {@code SphericalToCartesian} transform.
     */
    @Test
    public void testEngineering() {
        final DefaultSphericalCS cs = HardCodedCS.SPHERICAL_ENGINEERING;
        assertEquals("r", cs.getAxis(0).getAbbreviation());     // Abbreviation for distance should be lower-case

        final DefaultSphericalCS normalized = cs.forConvention(AxesConvention.NORMALIZED);
        assertNotSame(cs, normalized);          // Should create a new CoordinateSystem.
        assertAxisDirectionsEqual(normalized,
                AxisDirections.COUNTER_CLOCKWISE,
                AxisDirection.UP,
                AxisDirections.AWAY_FROM);
    }
}
