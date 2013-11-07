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

import java.util.Random;
import java.math.BigDecimal;
import org.junit.Test;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestUtilities;

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
     * Tolerance threshold for strict comparisons of floating point values.
     */
    private static final double STRICT = 0;

    /**
     * Verifies the values of {@link DecimalFunctions#EXPONENT_FOR_ZERO}.
     */
    @Test
    public void testConstants() {
        assertEquals(0,  parseDouble("1E" +  EXPONENT_FOR_ZERO), STRICT);
        assertTrue  (0 < parseDouble("1E" + (EXPONENT_FOR_ZERO + 1)));
        assertTrue  (POSITIVE_INFINITY > parseDouble("1E" +  EXPONENT_FOR_MAX));
        assertEquals(POSITIVE_INFINITY,  parseDouble("1E" + (EXPONENT_FOR_MAX + 1)), STRICT);
    }

    /**
     * Tests the {@link MathFunctions#pow10(double)} method.
     * This will indirectly test {@link MathFunctions#pow10(int)}
     * since the former will delegate to the later in this test.
     */
    @Test
    public void testPow10() {
        for (int i=EXPONENT_FOR_ZERO; i<=EXPONENT_FOR_MAX; i++) { // Range of allowed exponents in base 10.
            assertEquals(parseDouble("1E"+i), pow10(i), STRICT);
        }
    }

    /**
     * Tests {@link DecimalFunctions#floatToDouble(float)}.
     */
    @Test
    @DependsOnMethod("testPow10")
    public void testFloatToDouble() {
        assertEquals(0.0,    floatToDouble(0.0f),    0);
        assertEquals(-0.0,   floatToDouble(-0.0f),   0);
        assertEquals(10,     floatToDouble(10f),     0);
        assertEquals(0.1,    floatToDouble(0.1f),    0);
        assertEquals(0.01,   floatToDouble(0.01f),   0);
        assertEquals(0.001,  floatToDouble(0.001f),  0);
        assertEquals(0.0001, floatToDouble(0.0001f), 0);
        assertEquals(3.7E-8, floatToDouble(3.7E-8f), 0);
        assertEquals(3.7E-9, floatToDouble(3.7E-9f), 0);
        final Random random = TestUtilities.createRandomNumberGenerator();
        for (int i=0; i<100; i++) {
            final float value = StrictMath.scalb(random.nextFloat(), random.nextInt(20) - 10);
            assertEquals(String.valueOf(value), String.valueOf(floatToDouble(value)));
        }
        assertEquals(POSITIVE_INFINITY, floatToDouble(Float.POSITIVE_INFINITY), STRICT);
        assertEquals(NEGATIVE_INFINITY, floatToDouble(Float.NEGATIVE_INFINITY), STRICT);
        assertEquals(NaN,               floatToDouble(Float.NaN),               STRICT);
    }

    /**
     * Tests {@link DecimalFunctions#deltaForDoubleToDecimal(double)}.
     */
    @Test
    @DependsOnMethod("testPow10")
    public void testDeltaForDoubleToDecimal() {
        assertEquals(0, deltaForDoubleToDecimal(0),                 STRICT);
        assertEquals(0, deltaForDoubleToDecimal(1),                 STRICT);
        assertEquals(0, deltaForDoubleToDecimal(NaN),               STRICT);
        assertEquals(0, deltaForDoubleToDecimal(POSITIVE_INFINITY), STRICT);
        assertEquals(0, deltaForDoubleToDecimal(NEGATIVE_INFINITY), STRICT);

        assertEquals(-2.2204460492503132E-17, deltaForDoubleToDecimal(0.9),      STRICT);
        assertEquals(-5.5511151231257827E-18, deltaForDoubleToDecimal(0.1),      STRICT);
        assertEquals(-2.0816681711721684E-19, deltaForDoubleToDecimal(0.01),     STRICT);
        assertEquals(-2.0816681711721686E-20, deltaForDoubleToDecimal(0.001),    STRICT);
        assertEquals(-4.7921736023859296E-21, deltaForDoubleToDecimal(0.0001),   STRICT);
        assertEquals(-8.1803053914031310E-22, deltaForDoubleToDecimal(0.00001),  STRICT);
        assertEquals( 4.5251888174113741E-23, deltaForDoubleToDecimal(0.000001), STRICT);
        assertEquals(-1.3471890270011499E-17, deltaForDoubleToDecimal(0.201168), STRICT); // Link to metres
        assertEquals(-1.5365486660812166E-17, deltaForDoubleToDecimal(0.3048),   STRICT); // Feet to metres
        assertEquals( 9.4146912488213275E-18, deltaForDoubleToDecimal(0.9144),   STRICT); // Yard to metres
        assertEquals( 1.8829382497642655E-17, deltaForDoubleToDecimal(1.8288),   STRICT); // Fathom to metres
        assertEquals(-3.5527136788005009E-17, deltaForDoubleToDecimal(2.54),     STRICT); // Inch to centimetres
        assertEquals(-1.3471890270011499E-15, deltaForDoubleToDecimal(20.1168),  STRICT); // Chain to metres
        /*
         * Tests random value that do not use the full 'double' accuracy.
         */
        final Random random = TestUtilities.createRandomNumberGenerator();
        for (int fractionDigits=0; fractionDigits<=9; fractionDigits++) {
            for (int i=0; i<10; i++) {
                final BigDecimal value = BigDecimal.valueOf(random.nextInt(1000000000)).movePointLeft(fractionDigits);
                final double     ieee  = value.doubleValue(); // Inexact approximation of value.
                final BigDecimal delta = value.subtract(new BigDecimal(ieee));
                assertEquals(delta.doubleValue(), deltaForDoubleToDecimal(ieee), STRICT);
            }
        }
        /*
         * Tests random values that do use the full 'double' accuracy.
         */
        for (int i=0; i<0; i++) { // TODO: disabled for now
            final double     ieee  = random.nextDouble();
            final String     text  = String.valueOf(ieee);
            final BigDecimal value = new BigDecimal(text);
            final BigDecimal delta = value.subtract(new BigDecimal(ieee));
            assertEquals(text, delta.doubleValue(), deltaForDoubleToDecimal(ieee), STRICT);
        }
    }

    /**
     * Tests {@link DecimalFunctions#fractionDigitsForDelta(double, boolean)}.
     */
    @Test
    @DependsOnMethod("testPow10")
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
}
