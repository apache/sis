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
package org.apache.sis.referencing;

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.*;


/**
 * Tests the {@link GeodeticObjects} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.2)
 * @version 0.4
 * @module
 */
public final strictfp class GeodeticObjectsTest extends TestCase {
    /**
     * Verifies the epoch values of temporal enumeration compared to the Julian epoch.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Julian_day">Wikipedia: Julian day</a>
     */
    @Test
    public void testTemporal() {
        final double epoch = epoch(GeodeticObjects.Temporal.JULIAN);
        assertTrue(epoch < 0);

        assertEquals(2400000.5, epoch(GeodeticObjects.Temporal.MODIFIED_JULIAN) - epoch, 0);
        assertEquals("1858-11-17 00:00:00", epochString(GeodeticObjects.Temporal.MODIFIED_JULIAN));

        assertEquals(2440000.5, epoch(GeodeticObjects.Temporal.TRUNCATED_JULIAN) - epoch, 0);
        assertEquals("1968-05-24 00:00:00", epochString(GeodeticObjects.Temporal.TRUNCATED_JULIAN));

        assertEquals(2415020.0, epoch(GeodeticObjects.Temporal.DUBLIN_JULIAN) - epoch, 0);
        assertEquals("1899-12-31 12:00:00", epochString(GeodeticObjects.Temporal.DUBLIN_JULIAN));
    }

    /**
     * Returns the epoch of the given datum, in day units relative to Java epoch.
     */
    private static double epoch(final GeodeticObjects.Temporal def) {
        return def.datum().getOrigin().getTime() / (24*60*60*1000.0);
    }

    /**
     * Returns the epoch of the given datum formatted as a string.
     */
    private static String epochString(final GeodeticObjects.Temporal def) {
        return format(def.datum().getOrigin());
    }
}
