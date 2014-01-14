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

import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.util.Collections.singletonMap;
import static org.opengis.referencing.cs.CoordinateSystem.NAME_KEY;
import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link AbstractCS}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn({
    org.apache.sis.referencing.AbstractIdentifiedObjectTest.class,
    DefaultCoordinateSystemAxisTest.class,
    NormalizerTest.class
})
public final strictfp class AbstractCSTest extends TestCase {
    /**
     * Gets a coordinate system for the given axes convention and compare against the expected values.
     *
     * @param convention The convention to use.
     * @param cs The coordinate system to test.
     * @param expected The expected axes, in order.
     */
    private static void verifyAxesConvention(final AxesConvention convention, final AbstractCS cs,
            final CoordinateSystemAxis... expected)
    {
        final AbstractCS derived = cs.forConvention(convention);
        assertNotSame("Expected a new instance.", cs, derived);
        assertSame("No change expected.", derived, derived.forConvention(convention));
        assertSame("Shall be cached.", derived, cs.forConvention(convention));
        assertEquals("dimension", expected.length, cs.getDimension());
        for (int i=0; i<expected.length; i++) {
            assertEquals(expected[i], derived.getAxis(i));
        }
    }

    /**
     * Tests {@link AbstractCS#forConvention(AxesConvention)}
     * with a {@link AxesConvention#RIGHT_HANDED} argument.
     */
    @Test
    public void testForRightHandedConvention() {
        verifyAxesConvention(AxesConvention.RIGHT_HANDED, new AbstractCS(singletonMap(NAME_KEY, "Test"),
            CommonAxes.LATITUDE, CommonAxes.TIME, CommonAxes.ALTITUDE, CommonAxes.LONGITUDE),
            CommonAxes.LONGITUDE, CommonAxes.LATITUDE, CommonAxes.ALTITUDE, CommonAxes.TIME);
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        final AbstractCS cs = new AbstractCS(singletonMap(NAME_KEY, "Test"), CommonAxes.X, CommonAxes.Y);
        assertNotSame(cs, assertSerializedEquals(cs));
    }
}
