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
package org.apache.sis.referencing.operation.provider;

import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.util.internal.shared.Constants;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link TransverseMercator} {@code Zoner} enumeration.
 * This class is about projection parameters only. For test about the Transverse Mercator calculation,
 * see {@link org.apache.sis.referencing.operation.projection.TransverseMercatorTest} instead.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TransverseMercatorTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public TransverseMercatorTest() {
    }

    /**
     * Tests {@link TransverseMercator.Zoner#zone(double, double)},
     * including the special cases for Norway and Svalbard.
     */
    @Test
    public void testZone() {
        assertEquals( 1, TransverseMercator.Zoner.UTM.zone( 0, -180  ), "180°E");
        assertEquals(10, TransverseMercator.Zoner.UTM.zone( 0, -123  ), "123°E");
        assertEquals(60, TransverseMercator.Zoner.UTM.zone( 0,  179.9), "179.9°W");
        assertEquals(31, TransverseMercator.Zoner.UTM.zone(45,    4  ),  "4°E band T");
        assertEquals(32, TransverseMercator.Zoner.UTM.zone(56,    4  ),  "4°E band V");
        assertEquals(34, TransverseMercator.Zoner.UTM.zone(71,   20  ), "20°E band W");
        assertEquals(33, TransverseMercator.Zoner.UTM.zone(72,   20  ), "20°E band X");
    }

    /**
     * Tests {@link TransverseMercator.Zoner#centralMeridian(int)}.
     */
    @Test
    public void testCentralMeridian() {
        assertEquals(-177, TransverseMercator.Zoner.UTM.centralMeridian( 1));
        assertEquals(-123, TransverseMercator.Zoner.UTM.centralMeridian(10));
        assertEquals( 177, TransverseMercator.Zoner.UTM.centralMeridian(60));
    }

    /**
     * Tests {@link TransverseMercator.Zoner#setParameters(ParameterValueGroup, double, double)}
     * followed by {@link TransverseMercator.Zoner#zone(ParameterValueGroup)}.
     */
    @Test
    public void testSetParameters() {
        final ParameterValueGroup p = TransverseMercator.PARAMETERS.createValue();
        assertEquals("UTM zone 10N", TransverseMercator.Zoner.UTM.setParameters(p, 0, -122));
        assertEquals(-123, p.parameter(Constants.CENTRAL_MERIDIAN).doubleValue());
        assertEquals(   0, p.parameter(Constants.FALSE_NORTHING).doubleValue());
        assertEquals(  10, TransverseMercator.Zoner.UTM.zone(p));

        assertEquals("Transverse Mercator", TransverseMercator.Zoner.ANY.setParameters(p, 0, -122));
        assertEquals(-122, p.parameter(Constants.CENTRAL_MERIDIAN).doubleValue());
        assertEquals(   0, p.parameter(Constants.FALSE_NORTHING).doubleValue());
        assertEquals(   0, TransverseMercator.Zoner.UTM.zone(p));

        assertEquals("UTM zone 10S", TransverseMercator.Zoner.ANY.setParameters(p, -0.0, -123));
        assertEquals(-123, p.parameter(Constants.CENTRAL_MERIDIAN).doubleValue());
        assertEquals(10000000, p.parameter(Constants.FALSE_NORTHING).doubleValue());
        assertEquals(-10, TransverseMercator.Zoner.UTM.zone(p));
    }
}
