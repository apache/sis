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
@DependsOn(org.apache.sis.internal.util.NumericsTest.class)
public final strictfp class DecimalFunctionsTest extends TestCase {
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
    @DependsOnMethod("testConstants")
    public void testPow10() {
        for (int i=EXPONENT_FOR_ZERO; i<=EXPONENT_FOR_MAX; i++) { // Range of allowed exponents in base 10.
            assertEquals(parseDouble("1E"+i), pow10(i), STRICT);
        }
        assertEquals(1000000000000000000L, StrictMath.round(pow10(18))); // Highest value having an exact representation.
    }

    /**
     * Tests {@link DecimalFunctions#floatToDouble(float)}.
     */
    @Test
    @DependsOnMethod("testPow10")
    public void testFloatToDouble() {
        assertEquals(NaN,               floatToDouble(Float.NaN),               STRICT);
        assertEquals(POSITIVE_INFINITY, floatToDouble(Float.POSITIVE_INFINITY), STRICT);
        assertEquals(NEGATIVE_INFINITY, floatToDouble(Float.NEGATIVE_INFINITY), STRICT);

        assertEquals( 0.0,    floatToDouble( 0.0f),    STRICT);
        assertEquals(-0.0,    floatToDouble(-0.0f),    STRICT);
        assertEquals( 10,     floatToDouble( 10f),     STRICT);
        assertEquals(-10,     floatToDouble(-10f),     STRICT);
        assertEquals( 0.1,    floatToDouble( 0.1f),    STRICT);
        assertEquals( 0.01,   floatToDouble( 0.01f),   STRICT);
        assertEquals(-0.01,   floatToDouble(-0.01f),   STRICT);
        assertEquals( 0.001,  floatToDouble( 0.001f),  STRICT);
        assertEquals( 0.0001, floatToDouble( 0.0001f), STRICT);
        assertEquals( 3.7E-8, floatToDouble( 3.7E-8f), STRICT);
        assertEquals( 3.7E-9, floatToDouble( 3.7E-9f), STRICT);

        final Random random = TestUtilities.createRandomNumberGenerator();
        for (int i=0; i<100; i++) {
            float value = StrictMath.scalb(random.nextFloat(), random.nextInt(20) - 10);
            if (random.nextBoolean()) {
                value = -value;
            }
            assertEquals(String.valueOf(value), String.valueOf(floatToDouble(value)));
        }
    }

    /**
     * Tests {@link DecimalFunctions#deltaForDoubleToDecimal(double)}.
     * This method uses {@link BigDecimal} as the reference implementation.
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
         * This is a simpler case than the next one after this one, because the
         * final adjustment at the end of deltaForDoubleToDecimal is not needed.
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
         * Tests random values that do use the full 'double' accuracy. First, tests a few values which
         * were known to fail in an earlier version of deltaForDoubleToDecimal, then uses random values.
         * The expected values were computed with BigDecimal. The tolerance thresholds were determined
         * empirically. Comments on the right side give the tolerance thresholds in ULP of the delta.
         * The later are sometime hight, but it does not really matter. What matter is the tolerance
         * relative to the given value, not to the returned delta.
         */
        assertEquals(-1.9216378778219224E-23, deltaForDoubleToDecimal(3.3446045755169960E-7), STRICT);
        assertEquals(-4.1861088853329420E-24, deltaForDoubleToDecimal(3.5496578465465944E-7), 3E-39); //        4 ULP
        assertEquals(-4.1997787803848041E-17, deltaForDoubleToDecimal(0.7714013208272988),    2E-32); //        3 ULP
        assertEquals( 4.0373325589462183E-18, deltaForDoubleToDecimal(0.37197394704138476),   4E-33); //        4 ULP
        assertEquals(-2.3295945035351907E-18, deltaForDoubleToDecimal(0.25380700796141886),   4E-33); //        9 ULP
        assertEquals(-4.1729149110324215E-18, deltaForDoubleToDecimal(0.6546245266605436),    4E-32); //       43 ULP
        assertEquals( 4.8633955884724856E-23, deltaForDoubleToDecimal(0.8234936921177336),    4E-32); //  5666840 ULP
        assertEquals(-2.1507730707526207E-25, deltaForDoubleToDecimal(0.19920566694813302),   2E-33); // 36267774 ULP
        for (int i=0; i<500; i++) {
            double ieee = StrictMath.scalb(random.nextDouble(), 20 - random.nextInt(48));
            if (random.nextBoolean()) {
                ieee = -ieee;
            }
            final String     text  = String.valueOf(ieee);
            final BigDecimal value = new BigDecimal(text);
            final double     delta = value.subtract(new BigDecimal(ieee)).doubleValue();
            final double    actual = deltaForDoubleToDecimal(ieee);
            if (!Double.isNaN(actual)) {
                assertEquals(text, delta, actual, StrictMath.ulp(ieee) * 1E-12);
            }
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
        assertEquals(16, fractionDigitsForValue(1));
        assertEquals(15, fractionDigitsForValue(45));
        assertEquals(15, fractionDigitsForValue(39.666666666666667));
        assertEquals(15, fractionDigitsForValue(-8.131906111111111));
        assertEquals(16, fractionDigitsForValue(0.6149999999999993));
        assertEquals(17, fractionDigitsForValue(-0.198));
        assertEquals(14, fractionDigitsForValue(179.12499999999824));
        assertEquals( 0, fractionDigitsForValue(NaN));
        assertEquals( 0, fractionDigitsForValue(POSITIVE_INFINITY));
        assertEquals( 0, fractionDigitsForValue(NEGATIVE_INFINITY));
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
     * Tests {@link DecimalFunctions#fractionDigitsForValue(double, int)}.
     */
    @Test
    @DependsOnMethod("testFractionDigitsForValue")
    public void testFractionDigitsForValue2() {
        assertEquals(-EXPONENT_FOR_ZERO, fractionDigitsForValue(0, 2));
        assertEquals(0, fractionDigitsForValue(POSITIVE_INFINITY,  2));

        assertEquals("Expected no rounding", 15, fractionDigitsForValue(39.666666666666667,  2));
        assertEquals("Expected no rounding", 15, fractionDigitsForValue(-8.131906111111111,  2));
        assertEquals("Expected no rounding", 16, fractionDigitsForValue( 0.6149999999999993, 1));
        assertEquals("Expected rounding",    14, fractionDigitsForValue( 0.6149999999999993, 2));
        assertEquals("Expected rounding",    14, fractionDigitsForValue(-0.6149999999999993, 2));
        assertEquals("Expected rounding",    15, fractionDigitsForValue(-0.1979999999999998, 2));
        assertEquals("Expected rounding",    11, fractionDigitsForValue( 179.12499999999824, 3));
        assertEquals("Expected no rounding", 14, fractionDigitsForValue( 179.12499999999824, 2));
        assertEquals("Expected no rounding", 14, fractionDigitsForValue( 179.12499997999999, 3));
    }
}
