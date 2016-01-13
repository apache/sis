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
 * Tests {@link TransverseMercator} static methods.
 * This class is about projection parameters only. For test about the Transverse Mercator calculation,
 * see {@link org.apache.sis.referencing.operation.projection.TransverseMercatorTest} instead.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final strictfp class TransverseMercatorTest extends TestCase {
    /**
     * Tests {@link TransverseMercator#zone(double)}.
     */
    @Test
    public void testZone() {
        assertEquals(1,  TransverseMercator.zone(-180));
        assertEquals(10, TransverseMercator.zone(-123));
        assertEquals(60, TransverseMercator.zone(179.9));
    }

    /**
     * Tests {@link TransverseMercator#centralMeridian(int)}.
     */
    @Test
    public void testCentralMeridian() {
        assertEquals(-177, TransverseMercator.centralMeridian( 1), STRICT);
        assertEquals(-123, TransverseMercator.centralMeridian(10), STRICT);
        assertEquals( 177, TransverseMercator.centralMeridian(60), STRICT);
    }

    /**
     * Tests {@link TransverseMercator#setParameters(ParameterValueGroup, double, boolean, boolean)}.
     */
    @Test
    @DependsOnMethod({
        "testZone",
        "testCentralMeridian"
    })
    public void testCreate() {
        final ParameterValueGroup p = TransverseMercator.PARAMETERS.createValue();
        assertEquals("UTM zone 10N", TransverseMercator.setParameters(p, true, 0, -122));
        assertEquals(Constants.CENTRAL_MERIDIAN, -123, p.parameter(Constants.CENTRAL_MERIDIAN).doubleValue(), STRICT);
        assertEquals(Constants.FALSE_NORTHING, 0, p.parameter(Constants.FALSE_NORTHING).doubleValue(), STRICT);

        assertEquals("Transverse Mercator", TransverseMercator.setParameters(p, false, 0, -122));
        assertEquals(Constants.CENTRAL_MERIDIAN, -122, p.parameter(Constants.CENTRAL_MERIDIAN).doubleValue(), STRICT);
        assertEquals(Constants.FALSE_NORTHING, 0, p.parameter(Constants.FALSE_NORTHING).doubleValue(), STRICT);

        assertEquals("UTM zone 10S", TransverseMercator.setParameters(p, false, -0.0, -123));
        assertEquals(Constants.CENTRAL_MERIDIAN, -123, p.parameter(Constants.CENTRAL_MERIDIAN).doubleValue(), STRICT);
        assertEquals(Constants.FALSE_NORTHING, 10000000, p.parameter(Constants.FALSE_NORTHING).doubleValue(), STRICT);
    }
}
