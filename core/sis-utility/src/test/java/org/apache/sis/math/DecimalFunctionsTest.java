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
package org.apache.sis.math;

import org.junit.Test;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;

import static org.junit.Assert.*;
import static java.lang.Double.*;
import static org.apache.sis.math.DecimalFunctions.*;


/**
 * Tests the {@link DecimalFunctions} static methods.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn({
    org.apache.sis.internal.util.NumericsTest.class
})
public final strictfp class DecimalFunctionsTest extends TestCase {
    /**
     * The maximal exponent value such as {@code parseDouble("1E+308")} still a finite number.
     *
     * @see Double#MAX_VALUE
     */
    private static final int EXPONENT_FOR_MAX = 308;

    /**
     * Verifies the values of {@link DecimalFunctions#EXPONENT_FOR_ZERO}.
     */
    @Test
    public void testConstants() {
        assertEquals(0,  parseDouble("1E" +  EXPONENT_FOR_ZERO), 0);
        assertTrue  (0 < parseDouble("1E" + (EXPONENT_FOR_ZERO + 1)));
        assertTrue  (POSITIVE_INFINITY > parseDouble("1E" +  EXPONENT_FOR_MAX));
        assertEquals(POSITIVE_INFINITY,  parseDouble("1E" + (EXPONENT_FOR_MAX + 1)), 0);
    }

    /**
     * Tests {@link DecimalFunctions#fractionDigitsForDelta(double, boolean)}.
     */
    @Test
    public void testFractionDigitsForDelta() {
        assertEquals(3, fractionDigitsForDelta(0.001, true));
        assertEquals(3, fractionDigitsForDelta(0.009, true));
        assertEquals(2, fractionDigitsForDelta(0.010, true));
        assertEquals(2, fractionDigitsForDelta(0.015, true));
        assertEquals(1, fractionDigitsForDelta(0.100, true));
        assertEquals(1, fractionDigitsForDelta(0.125, true));
        assertEquals(1, fractionDigitsForDelta(0.949, true));
        assertEquals(2, fractionDigitsForDelta(0.994, true)); // Special case
        assertEquals(3, fractionDigitsForDelta(0.999, true)); // Special case

        assertEquals( 0, fractionDigitsForDelta(  1.0, true));
        assertEquals( 0, fractionDigitsForDelta(  1.9, true));
        assertEquals( 0, fractionDigitsForDelta(  9.1, true));
        assertEquals(-1, fractionDigitsForDelta( 10.0, true));
        assertEquals(-1, fractionDigitsForDelta( 19.9, true));
        assertEquals(-1, fractionDigitsForDelta( 94.9, true));
        assertEquals( 0, fractionDigitsForDelta( 99.0, true)); // Special case
        assertEquals(-2, fractionDigitsForDelta(100.0, true));
        assertEquals(-2, fractionDigitsForDelta(100.1, true));
        assertEquals(-1, fractionDigitsForDelta(994.9, true)); // Special case
        assertEquals(+1, fractionDigitsForDelta(999.9, true)); // Special case
        assertEquals(-3, fractionDigitsForDelta(1000,  true));

        // Tests values out of the POW10 array range.
        assertEquals(23,  fractionDigitsForDelta(1.0E-23,  true));
        assertEquals(23,  fractionDigitsForDelta(1.9E-23,  true));
        assertEquals(23,  fractionDigitsForDelta(9.1E-23,  true));
        assertEquals(24,  fractionDigitsForDelta(9.6E-23,  true)); // Special case
        assertEquals(300, fractionDigitsForDelta(1.1E-300, true));

        assertEquals(-23,  fractionDigitsForDelta(1.0E+23,  true));
        assertEquals(-23,  fractionDigitsForDelta(1.9E+23,  true));
        assertEquals(-23,  fractionDigitsForDelta(9.1E+23,  true));
        assertEquals(-22,  fractionDigitsForDelta(9.6E+23,  true)); // Special case
        assertEquals(-300, fractionDigitsForDelta(1.1E+300, true));

        // Other cases.
        assertEquals(-EXPONENT_FOR_ZERO, fractionDigitsForDelta(0,                    false));
        assertEquals(-EXPONENT_FOR_ZERO, fractionDigitsForDelta(MIN_VALUE,            false));
        assertEquals(                16, fractionDigitsForDelta(StrictMath.ulp(1.0),  false));
        assertEquals(                15, fractionDigitsForDelta(StrictMath.ulp(45.0), false));
        assertEquals(                 0, fractionDigitsForDelta(NaN,                  false));
        assertEquals(                 0, fractionDigitsForDelta(POSITIVE_INFINITY,    false));
        assertEquals(                 0, fractionDigitsForDelta(NEGATIVE_INFINITY,    false));
    }

    /**
     * Tests {@link DecimalFunctions#fractionDigitsForValue(double)}.
     */
    @Test
    public void testFractionDigitsForValue() {
        assertEquals(-EXPONENT_FOR_ZERO, fractionDigitsForValue(0));
        assertEquals(-EXPONENT_FOR_ZERO, fractionDigitsForValue(MIN_VALUE));
        assertEquals(                16, fractionDigitsForValue(1));
        assertEquals(                15, fractionDigitsForValue(45));
        assertEquals(                 0, fractionDigitsForValue(NaN));
        assertEquals(                 0, fractionDigitsForValue(POSITIVE_INFINITY));
        assertEquals(                 0, fractionDigitsForValue(NEGATIVE_INFINITY));
        for (int i=EXPONENT_FOR_ZERO; i<=EXPONENT_FOR_MAX; i++) {
            final double value = pow10(i);
            final double accuracy = pow10(-fractionDigitsForValue(value));
            assertEquals("Shall not be greater than ULP", 0, accuracy, StrictMath.ulp(value));
        }
        for (int i=MIN_EXPONENT; i<=MAX_EXPONENT; i++) {
            final double value = StrictMath.scalb(1, i);
            final double accuracy = pow10(-fractionDigitsForValue(value));
            assertEquals("Shall not be greater than ULP", 0, accuracy, StrictMath.ulp(value));
        }
    }

    /**
     * Tests the {@link MathFunctions#pow10(double)} method.
     * This will indirectly test {@link MathFunctions#pow10(int)}
     * since the former will delegate to the later in this test.
     */
    @Test
    public void testPow10() {
        for (int i=EXPONENT_FOR_ZERO; i<=EXPONENT_FOR_MAX; i++) { // Range of allowed exponents in base 10.
            assertEquals(parseDouble("1E"+i), pow10((double) i), 0);
        }
    }
}
