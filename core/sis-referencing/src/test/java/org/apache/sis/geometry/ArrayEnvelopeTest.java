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
package org.apache.sis.geometry;

import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.referencing.Assertions.assertWktEquals;
import static org.apache.sis.referencing.crs.HardCodedCRS.WGS84;


/**
 * Unit tests for class {@link ArrayEnvelope}.
 * This is the base class of {@link GeneralEnvelope} and {@link ImmutableEnvelope}.
 *
 * @author  Michael Hausegger
 * @version 1.0
 * @since   0.8
 */
@DependsOn(AbstractEnvelopeTest.class)
public final class ArrayEnvelopeTest extends TestCase {
    /**
     * Tests {@link ArrayEnvelope#isEmpty()}.
     */
    @Test
    public void testIsEmpty() {
        ArrayEnvelope envelope = new ArrayEnvelope(new double[] {0, -356.683168});
        assertTrue(envelope.isEmpty());
        envelope.coordinates[0] = -360;
        assertFalse(envelope.isEmpty());
    }

    /**
     * Tests {@link ArrayEnvelope#getMinimum(int)} and {@link ArrayEnvelope#getMaximum(int)}.
     */
    @Test
    public void testGetExtremums() {
        ArrayEnvelope envelope = new ArrayEnvelope(2);
        envelope.coordinates[2] = -1728;
        assertTrue(Double.isNaN(envelope.getMinimum(0)));
        assertTrue(Double.isNaN(envelope.getMaximum(0)));
        assertEquals(0, envelope.getMinimum(1), STRICT);
        assertEquals(0, envelope.getMaximum(1), STRICT);

        // Make the range valid and test again.
        envelope.coordinates[0] = -1800;
        assertEquals(-1800, envelope.getMinimum(0), STRICT);
        assertEquals(-1728, envelope.getMaximum(0), STRICT);
    }

    /**
     * Tests the {@link ArrayEnvelope#formatTo(Formatter)} method.
     * Contrarily to {@code toString()}, the precision depends on the CRS.
     */
    @Test
    public void testFormatWKT() {
        ArrayEnvelope envelope = new ArrayEnvelope(new double[] {4, -10, 50, 2});
        assertWktEquals("BOX[ 4 -10,\n" +
                        "    50   2]", envelope);
        envelope.crs = WGS84;
        assertWktEquals("BOX[ 4.00000000 -10.00000000,\n" +
                        "    50.00000000   2.00000000]", envelope);

    }

    /**
     * Tests envelope construction from a the pseudo-Well Known Text (WKT) representation of a Bounding Box (BBOX).
     */
    @Test
    public void testCreatesFromWKT() {
        ArrayEnvelope envelope = new ArrayEnvelope(
                "BOX6D(-5610.14928 -3642.5148 1957.4432 -170.0175 -77.9698 -Infinity,"
                   + " -5610.14920 -3642.5140 1957.4440 -170.0170 -77.9690 -Infinity)");
        assertEquals(6, envelope.getDimension());
        assertArrayEquals(new double[] {
            -5610.14928, -3642.5148, 1957.4432, -170.0175, -77.9698, Double.NEGATIVE_INFINITY,
            -5610.14920, -3642.5140, 1957.4440, -170.0170, -77.9690, Double.NEGATIVE_INFINITY
        }, envelope.coordinates, STRICT);
    }

    /**
     * Verifies that attempt to create an envelope from an invalid WKT results in an exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreatesFromInvalidWKT() {
        assertNotNull(new ArrayEnvelope("BBOX[\"invalid\"]").coordinates);
    }

    /**
     * Verifies the creation of dimensionless envelopes (envelopes having zero dimension).
     */
    @Test
    public void testDimensionlessEnvelope() {
        ArrayEnvelope envelope = new ArrayEnvelope(0);
        assertTrue(envelope.isAllNaN());
    }

    /**
     * Tests {@link ArrayEnvelope#equals(Object)}.
     */
    @Test
    @SuppressWarnings("ObjectEqualsNull")
    public void testEquals() {
        ArrayEnvelope env1 = new ArrayEnvelope(new double[] {1, 2, 4, 5});
        ArrayEnvelope env2 = new ArrayEnvelope(new double[] {3, 2, 4, 5});
        assertFalse(env1.equals(null));
        assertFalse(env1.equals(env2));
        assertFalse(env2.equals(env1));
        assertTrue (env1.equals(env1));
        assertTrue (env2.equals(env2));

        env2.coordinates[0] = 1;
        assertTrue(env1.equals(env2));
        assertTrue(env2.equals(env1));
    }
}
