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
package org.apache.sis.internal.referencing.provider;

import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link TransverseMercator} {@code Zoner} enumeration.
 * This class is about projection parameters only. For test about the Transverse Mercator calculation,
 * see {@link org.apache.sis.referencing.operation.projection.TransverseMercatorTest} instead.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.7
 * @module
 */
public final strictfp class TransverseMercatorTest extends TestCase {
    /**
     * Tests {@link TransverseMercator.Zoner#zone(double, double)},
     * including the special cases for Norway and Svalbard.
     */
    @Test
    public void testZone() {
        assertEquals("180°E",        1, TransverseMercator.Zoner.UTM.zone( 0, -180  ));
        assertEquals("123°E",       10, TransverseMercator.Zoner.UTM.zone( 0, -123  ));
        assertEquals("179.9°W",     60, TransverseMercator.Zoner.UTM.zone( 0,  179.9));
        assertEquals( "4°E band T", 31, TransverseMercator.Zoner.UTM.zone(45,    4  ));
        assertEquals( "4°E band V", 32, TransverseMercator.Zoner.UTM.zone(56,    4  ));
        assertEquals("20°E band W", 34, TransverseMercator.Zoner.UTM.zone(71,   20  ));
        assertEquals("20°E band X", 33, TransverseMercator.Zoner.UTM.zone(72,   20  ));
    }

    /**
     * Tests {@link TransverseMercator.Zoner#centralMeridian(int)}.
     */
    @Test
    public void testCentralMeridian() {
        assertEquals(-177, TransverseMercator.Zoner.UTM.centralMeridian( 1), STRICT);
        assertEquals(-123, TransverseMercator.Zoner.UTM.centralMeridian(10), STRICT);
        assertEquals( 177, TransverseMercator.Zoner.UTM.centralMeridian(60), STRICT);
    }

    /**
     * Tests {@link TransverseMercator.Zoner#setParameters(ParameterValueGroup, double, double)}
     * followed by {@link TransverseMercator.Zoner#zone(ParameterValueGroup)}.
     */
    @Test
    @DependsOnMethod({
        "testZone",
        "testCentralMeridian"
    })
    public void testSetParameters() {
        final ParameterValueGroup p = TransverseMercator.PARAMETERS.createValue();
        assertEquals("UTM zone 10N", TransverseMercator.Zoner.UTM.setParameters(p, 0, -122));
        assertEquals(Constants.CENTRAL_MERIDIAN, -123, p.parameter(Constants.CENTRAL_MERIDIAN).doubleValue(), STRICT);
        assertEquals(Constants.FALSE_NORTHING, 0, p.parameter(Constants.FALSE_NORTHING).doubleValue(), STRICT);
        assertEquals("UTM.zone(parameters)", 10, TransverseMercator.Zoner.UTM.zone(p));

        assertEquals("Transverse Mercator", TransverseMercator.Zoner.ANY.setParameters(p, 0, -122));
        assertEquals(Constants.CENTRAL_MERIDIAN, -122, p.parameter(Constants.CENTRAL_MERIDIAN).doubleValue(), STRICT);
        assertEquals(Constants.FALSE_NORTHING, 0, p.parameter(Constants.FALSE_NORTHING).doubleValue(), STRICT);
        assertEquals("UTM.zone(parameters)", 0, TransverseMercator.Zoner.UTM.zone(p));

        assertEquals("UTM zone 10S", TransverseMercator.Zoner.ANY.setParameters(p, -0.0, -123));
        assertEquals(Constants.CENTRAL_MERIDIAN, -123, p.parameter(Constants.CENTRAL_MERIDIAN).doubleValue(), STRICT);
        assertEquals(Constants.FALSE_NORTHING, 10000000, p.parameter(Constants.FALSE_NORTHING).doubleValue(), STRICT);
        assertEquals("UTM.zone(parameters)", -10, TransverseMercator.Zoner.UTM.zone(p));
    }
}
