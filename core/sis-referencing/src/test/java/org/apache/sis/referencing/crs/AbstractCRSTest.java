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
package org.apache.sis.referencing.crs;

import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.referencing.cs.HardCodedAxes;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.util.Collections.singletonMap;
import static org.opengis.referencing.cs.CoordinateSystem.NAME_KEY;
import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link AbstractCRS} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.5
 * @module
 */
@DependsOn(org.apache.sis.referencing.cs.AbstractCSTest.class)
public final strictfp class AbstractCRSTest extends TestCase {
    /**
     * Tests {@link AbstractCRS#forConvention(AxesConvention)} with {@link AxesConvention#RIGHT_HANDED}.
     */
    @Test
    public void testForRightHandedConvention() {
        final AbstractCRS toTest, expected, actual;
        toTest   =  new AbstractCRS(singletonMap(NAME_KEY, "My CRS"),
                    new AbstractCS (singletonMap(NAME_KEY, "My strange CS"),
                    HardCodedAxes.TIME, HardCodedAxes.ALTITUDE, HardCodedAxes.GEODETIC_LATITUDE, HardCodedAxes.GEODETIC_LONGITUDE));
        expected =  new AbstractCRS(singletonMap(NAME_KEY, "My CRS"),
                    new AbstractCS (singletonMap(NAME_KEY, "Coordinate system: East (°), North (°), Up (m), Future (d)."),
                    HardCodedAxes.GEODETIC_LONGITUDE, HardCodedAxes.GEODETIC_LATITUDE, HardCodedAxes.ALTITUDE, HardCodedAxes.TIME));
        actual   =  toTest.forConvention(AxesConvention.RIGHT_HANDED);

        assertEquals("forConvention(RIGHT_HANDED)", expected, actual);
        assertSame(actual,   toTest  .forConvention(AxesConvention.RIGHT_HANDED));
        assertSame(actual,   toTest  .forConvention(AxesConvention.CONVENTIONALLY_ORIENTED));
        assertSame(actual,   toTest  .forConvention(AxesConvention.NORMALIZED));
        assertSame(expected, expected.forConvention(AxesConvention.RIGHT_HANDED));
        assertSame(expected, expected.forConvention(AxesConvention.CONVENTIONALLY_ORIENTED));
        assertSame(expected, expected.forConvention(AxesConvention.NORMALIZED));
    }
}
