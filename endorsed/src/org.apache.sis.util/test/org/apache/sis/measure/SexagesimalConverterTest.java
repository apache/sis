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
import javax.measure.Quantity;
import javax.measure.UnitConverter;
import static org.apache.sis.measure.SexagesimalConverter.*;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.TestCase;


/**
 * Test the {@link SexagesimalConverter} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class SexagesimalConverterTest extends TestCase {
    /**
     * Tolerance value for the comparisons of floating point numbers.
     */
    private static final double TOLERANCE = 1E-12;

    /**
     * Creates a new test case.
     */
    public SexagesimalConverterTest() {
    }

    /**
     * Converts the given value to another unit, compares with the expected value, and verify
     * the inverse conversion. Then tries again with the negative of the given values.
     */
    private static <Q extends Quantity<Q>> void checkConversion(
            final double expected, final Unit<Q> unitExpected,
            final double actual,   final Unit<Q> unitActual)
    {
        final UnitConverter converter = unitActual.getConverterTo(unitExpected);
        final UnitConverter inverse   = converter.inverse();
        assertEquals( expected, converter.convert( actual), TOLERANCE);
        assertEquals( actual,   inverse.convert( expected), TOLERANCE);
        assertEquals(-expected, converter.convert(-actual), TOLERANCE);
        assertEquals(-actual,   inverse.convert(-expected), TOLERANCE);
    }

    /**
     * Checks the conversions using {@link SexagesimalConverter#DM}.
     */
    @Test
    public void testDM() {
        checkConversion(10.00,              Units.DEGREE, 10.0000,    DM);
        checkConversion(10.006,             Units.DEGREE, 10.0036,    DM);
        checkConversion(10.50,              Units.DEGREE, 10.3000,    DM);
        checkConversion(10.987333333333333, Units.DEGREE, 10.5924,    DM);
        checkConversion(44.503354166666666, Units.DEGREE, 44.3020125, DM);
    }

    /**
     * Checks the conversions using {@link SexagesimalConverter#DMS}.
     */
    @Test
    public void testDMS() {
        checkConversion(10.00,              Units.DEGREE, 10.0000,    DMS);
        checkConversion(10.01,              Units.DEGREE, 10.0036,    DMS);
        checkConversion(10.50,              Units.DEGREE, 10.3000,    DMS);
        checkConversion(10.99,              Units.DEGREE, 10.5924,    DMS);
        checkConversion(44.505590277777777, Units.DEGREE, 44.3020125, DMS);
    }

    /**
     * Checks the conversions using {@link SexagesimalConverter#DMS_SCALED}.
     */
    @Test
    public void testDMS_Scaled() {
        checkConversion(10.00,              Units.DEGREE, 100000,     DMS_SCALED);
        checkConversion(10.01,              Units.DEGREE, 100036,     DMS_SCALED);
        checkConversion(10.50,              Units.DEGREE, 103000,     DMS_SCALED);
        checkConversion(10.99,              Units.DEGREE, 105924,     DMS_SCALED);
        checkConversion(44.505590277777777, Units.DEGREE, 443020.125, DMS_SCALED);
    }

    /**
     * Tests the error message on attempt to convert an illegal value.
     */
    @Test
    public void testErrorMessage() {
        final UnitConverter converter = DMS.getConverterTo(Units.DEGREE);
        assertEquals(10.5, converter.convert(10.3));
        var e = assertThrows(IllegalArgumentException.class, () -> converter.convert(10.7),
                             "Conversion of illegal value should not be allowed.");
        assertMessageContains(e);     // Cannot check message content because it is locale-sensitive.
    }

    /**
     * Tests the fix for rounding error in conversion of 46°57'8.66".
     * This fix is necessary for avoiding a 4 cm error with Apache SIS
     * construction of EPSG:2056 projected CRS.
     */
    @Test
    public void testRoundingErrorFix() {
        final UnitConverter c = DMS.getConverterTo(Units.DEGREE);
        assertEquals(46.95240555555556, c.convert(46.570866));
    }

    /**
     * Verifies the unit symbols.
     */
    @Test
    public void testToString() {
        assertEquals("D.M",  DM.toString());
        assertEquals("D.MS", DMS.toString());
        assertEquals("DMS",  DMS_SCALED.toString());
    }
}
