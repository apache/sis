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
package org.apache.sis.measure;

import javax.measure.Unit;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link ConventionalUnit} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
@DependsOn({SystemUnitTest.class, LinearConverterTest.class})
public final strictfp class ConventionalUnitTest extends TestCase {
    /**
     * Verifies the properties if the given unit.
     *
     * @param  system  the expected system unit.
     * @param  unit    the conventional unit to verify.
     * @param  symbol  the expected symbol.
     * @param  scale   the expected scale factor.
     */
    static void verify(final Unit<?> system, final Unit<?> unit, final String symbol, final double scale) {
        assertSame  ("getSystemUnit()", system, unit.getSystemUnit());
        assertEquals("getSymbol()",     symbol, unit.getSymbol());
        assertEquals("UnitConverter", scale, Units.toStandardUnit(unit), STRICT);
    }

    /**
     * Verifies some of the hard-coded constants defined in the {@link Units} class.
     */
    @Test
    public void verifyConstants() {
        verify(Units.METRE,             Units.NANOMETRE,             "nm",  1E-9);
        verify(Units.METRE,             Units.MILLIMETRE,            "mm",  1E-3);
        verify(Units.METRE,             Units.CENTIMETRE,            "cm",  1E-2);
        verify(Units.METRE,             Units.METRE,                  "m",  1E+0);
        verify(Units.METRE,             Units.KILOMETRE,             "km",  1E+3);
        verify(Units.METRE,             Units.NAUTICAL_MILE,          "M",  1852);
        verify(Units.SECOND,            Units.SECOND,                 "s",     1);
        verify(Units.SECOND,            Units.MINUTE,               "min",    60);
        verify(Units.SECOND,            Units.HOUR,                   "h",  3600);
        verify(Units.PASCAL,            Units.PASCAL,                "Pa",     1);
        verify(Units.PASCAL,            Units.HECTOPASCAL,          "hPa",   100);
        verify(Units.METRES_PER_SECOND, Units.KILOMETRES_PER_HOUR, "km∕h",  0.06);
        verify(Units.KILOGRAM,          Units.KILOGRAM,              "kg",     1);
        verify(Units.UNITY,             Units.UNITY,                   "",     1);
        verify(Units.UNITY,             Units.PERCENT,                "%",  1E-2);
        verify(Units.UNITY,             Units.PPM,                  "ppm",  1E-6);
    }
}
