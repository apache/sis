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
package org.apache.sis.util.internal.shared;

import java.util.Random;
import java.math.BigDecimal;
import java.math.MathContext;
import java.lang.reflect.Field;
import static java.lang.StrictMath.*;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.util.ArraysExt;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;


/**
 * Tests {@link DoubleDouble} using {@link BigDecimal} as the references.
 * Those tests need {@link DoubleDouble#DISABLED} to be set to {@code false}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DoubleDoubleTest extends TestCase {
    /**
     * Number of time to repeat arithmetic tests.
     */
    private static final int NUMBER_OF_REPETITIONS = 1000;

    /**
     * The tolerance factor (as a multiplicand) for the addition and subtraction operations.
     * This is a tolerance factor in units of {@link DoubleDouble#value} ULP. Results smaller
     * than 1 ULP of {@link DoubleDouble#error} will be clamped.
     */
    private static final double ADD_TOLERANCE_FACTOR = 1E-17;

    /**
     * The tolerance factor (as a multiplicand) for the multiplication and division operations.
     * This is a tolerance factor in units of {@link DoubleDouble#value} ULP. Results smaller
     * than 1 ULP of {@link DoubleDouble#error} will be clamped.
     */
    private static final double PRODUCT_TOLERANCE_FACTOR = 1E-15;

    /**
     * The random number generator to use for the test.
     */
    private final Random random;

    /**
     * Creates a new test case.
     */
    public DoubleDoubleTest() {
        random = TestUtilities.createRandomNumberGenerator();
    }

    /**
     * Returns the next {@code double} random value. The scale factor is a power of two
     * in order to change only the exponent part of the IEEE representation.
     */
    private double nextRandom() {
        return fma(random.nextDouble(), 2048, -1024);
    }

    /**
     * Fetches the next {@code DoubleDouble} random value.
     */
    private DoubleDouble nextRandomDD() {
        return DoubleDouble.sum(nextRandom(), random.nextDouble());
    }

    /**
     * Returns a {@code BigDecimal} representation of the given {@code DoubleDouble} value.
     */
    private static BigDecimal toBigDecimal(final DoubleDouble dd) {
        return new BigDecimal(dd.value).add(new BigDecimal(dd.error));
    }

    /**
     * Asserts that the given {@code DoubleDouble} is normalized and has a value equals to the expected one.
     * More specifically:
     *
     * <ul>
     *   <li>the {@link DoubleDouble#value} is strictly equals to the expected value, and</li>
     *   <li>the {@link DoubleDouble#error} is not greater than 1 ULP of the above value.</li>
     * </ul>
     */
    private static void assertNormalizedAndEquals(final double expected, final DoubleDouble actual) {
        assertTrue(abs(actual.error) <= ulp(actual.value), "DoubleDouble is not normalized.");
        assertEquals(expected, actual.value, "Unexpected arithmetic result.");
    }

    /**
     * Asserts that the result of some operation is equal to the expected value,
     * up to a tolerance value determined by the extended arithmetic precision.
     *
     * @param expected  the expected value, computed using {@code BigInteger} arithmetic.
     * @param actual    the actual value.
     * @param ef        multiplication factor for the tolerance threshold.
     */
    private static void assertExtendedEquals(final BigDecimal expected, final DoubleDouble actual, final double ef) {
        final BigDecimal value = toBigDecimal(actual);
        final double delta = abs(expected.subtract(value).doubleValue());
        final double threshold = max(ulp(actual.error), ulp(actual.value) * ef);
        if (!(delta <= threshold)) {                                                // Use ! for catching NaN values.
            fail("Arithmetic error:\n" +
                 "  Expected:   " + expected  + '\n' +
                 "  Actual:     " + value     + '\n' +
                 "  Difference: " + delta     + '\n' +
                 "  Threshold:  " + threshold + '\n' +
                 "  Value ULP:  " + ulp(actual.value) + '\n');
        }
    }

    /**
     * Tests {@link DoubleDouble#quickSum(double, double)}.
     */
    @Test
    public void testQuickSum() {
        for (int i=0; i<NUMBER_OF_REPETITIONS; i++) {
            double a = nextRandom();
            double b = nextRandom();
            if (abs(a) < abs(b)) {
                final double t = a;
                a = b;
                b = t;
            }
            var dd = DoubleDouble.quickSum(a, b);
            assertNormalizedAndEquals(a + b, dd);
            assertExtendedEquals(new BigDecimal(a).add(new BigDecimal(b)), dd, ADD_TOLERANCE_FACTOR);
        }
    }

    /**
     * Tests {@link DoubleDouble#sum(double, double)}.
     */
    @Test
    public void testSum() {
        for (int i=0; i<NUMBER_OF_REPETITIONS; i++) {
            final double a = nextRandom();
            final double b = nextRandom();
            var dd = DoubleDouble.sum(a, b);
            assertNormalizedAndEquals(a + b, dd);
            assertExtendedEquals(new BigDecimal(a).add(new BigDecimal(b)), dd, ADD_TOLERANCE_FACTOR);
        }
    }

    /**
     * Tests {@link DoubleDouble#product(double, double)}.
     */
    @Test
    public void testProduct() {
        for (int i=0; i<NUMBER_OF_REPETITIONS; i++) {
            final double a = nextRandom();
            final double b = nextRandom();
            var dd = DoubleDouble.product(a, b);
            assertNormalizedAndEquals(a * b, dd);
            assertExtendedEquals(new BigDecimal(a).multiply(new BigDecimal(b)), dd, ADD_TOLERANCE_FACTOR);
        }
    }

    /**
     * Tests {@link DoubleDouble#add(DoubleDouble)}.
     */
    @Test
    public void testAdd() {
        for (int i=0; i<NUMBER_OF_REPETITIONS; i++) {
            final DoubleDouble dd = nextRandomDD();
            final DoubleDouble op = nextRandomDD();
            final BigDecimal expected = toBigDecimal(dd).add(toBigDecimal(op));
            assertExtendedEquals(expected, dd.add(op), ADD_TOLERANCE_FACTOR);
        }
    }

    /**
     * Tests {@link DoubleDouble#multiply(DoubleDouble)}.
     */
    @Test
    public void testMultiply() {
        for (int i=0; i<NUMBER_OF_REPETITIONS; i++) {
            final DoubleDouble dd = nextRandomDD();
            final DoubleDouble op = nextRandomDD();
            final BigDecimal expected = toBigDecimal(dd).multiply(toBigDecimal(op), MathContext.DECIMAL128);
            assertExtendedEquals(expected, dd.multiply(op), PRODUCT_TOLERANCE_FACTOR);
        }
    }

    /**
     * Tests {@link DoubleDouble#divide(DoubleDouble)}.
     */
    @Test
    public void testDivide() {
        for (int i=0; i<NUMBER_OF_REPETITIONS; i++) {
            final DoubleDouble dd = nextRandomDD();
            final DoubleDouble op = nextRandomDD();
            final BigDecimal expected = toBigDecimal(dd).divide(toBigDecimal(op), MathContext.DECIMAL128);
            assertExtendedEquals(expected, dd.divide(op), PRODUCT_TOLERANCE_FACTOR);
        }
    }

    /**
     * Tests {@link DoubleDouble#ratio_1m_1p()}.
     */
    @Test
    public void testRatio_1m_1p() {
        final DoubleDouble t = DoubleDouble.of(0.25, false).ratio_1m_1p();
        assertEquals((1 - 0.25) / (1 + 0.25), t.doubleValue());
    }

    /**
     * Tests {@link DoubleDouble#sqrt()} first with the square root of 2, then with random values.
     * In the {@code sqrt(2)} case:
     *
     * <ul>
     *   <li>The error using {@code double} arithmetic is approximately 1E-16.</li>
     *   <li>The error using double-double arithmetic is expected to be slightly less that 1E-32.</li>
     * </ul>
     */
    @Test
    public void testSqrt() {
        final BigDecimal SQRT2 = new BigDecimal("1.414213562373095048801688724209698");
        DoubleDouble dd = DoubleDouble.of(2).sqrt();
        assertNormalizedAndEquals(sqrt(2), dd);
        assertEquals(0, SQRT2.subtract(toBigDecimal(dd)).doubleValue(), 1E-32);
        /*
         * If we have been able to compute √2, now test with random values.
         * Since the range of values is approximately [-1000 … 1000], use
         * a tolerance value 1000 time the one that we used for √2.
         */
        for (int i=0; i<NUMBER_OF_REPETITIONS; i++) {
            dd = nextRandomDD();
            if (dd.value < 0) {
                dd = dd.negate();
            }
            final DoubleDouble original = dd;
            dd = dd.square();
            dd = dd.sqrt();
            dd = dd.subtract(original);
            assertEquals(0, dd.doubleValue(), 1E-29);
        }
        dd = DoubleDouble.ZERO.sqrt();
        assertTrue(dd.isZero());
    }

    /**
     * Tests the {@link DoubleDouble#series(double...)} method.
     */
    @Test
    public void testSeries() {
        DoubleDouble t = DoubleDouble.of(2).series(1, 1./3, 1./9, 1./7, 1./13);     // Random coefficients.
        assertEquals(1 + 2./3 + 4./9 + 8./7 + 16./13, t.doubleValue());
    }

    /**
     * List of all {@link DoubleDouble#VALUES} as string decimal representations,
     * together with some additional values to test.
     */
    private static final String[] PREDEFINED_VALUES = {
         "0.1",
         "0.01",
         "0.001",
         "0.0001",
         "0.00001",
         "0.000001",
         "0.3048",                                      // Feet to metres
         "0.201168",                                    // Link to metres
         "0.9144",                                      // Yard to metres
         "1.8288",                                      // Fathom to metres
        "20.1168",                                      // Chain to metres
         "2.54",                                        // Inch to centimetres
         "0.9",                                         // Degree to grads
         "1.111111111111111111111111111111111",         // Grad to degrees
         "0.002777777777777777777777777777777778",      // 1/360°
         "0.0002777777777777777777777777777777778",     // Second to degrees
         "0.01666666666666666666666666666666667",       // Minute to degrees
         "0.000004848136811095359935899141023579480",   // Arc-second to radians
         "0.01745329251994329576923690768488613",       // Degrees to radians
        "57.2957795130823208767981548141052",           // Radians to degrees
         "3.14159265358979323846264338327950",          //  π
         "6.28318530717958647692528676655901",          // 2π
         "1.570796326794896619231321691639751",         //  π/2
         "0.785398163397448309615660845819876",         //  π/4
         "2.356194490192344928846982537459627",         // 3π/4
         "1.414213562373095048801688724209698"          // √2
    };

    /**
     * Verifies the {@link DoubleDouble#VALUES} and {@link DoubleDouble#ERRORS} arrays.
     *
     * <ul>
     *   <li>Elements in the {@code VALUES} array shall be sorted in increasing order.</li>
     *   <li>{@code VALUES} and {@code ERRORS} arrays shall have the same length.</li>
     *   <li>The arrays do not contains an entry for a value that could be omitted.</li>
     * </ul>
     *
     * @throws ReflectiveOperationException if this test uses wrong field names.
     */
    @Test
    public void testArraysConsistency() throws ReflectiveOperationException {
        Field field = DoubleDouble.class.getDeclaredField("VALUES");
        field.setAccessible(true);
        final double[] values = (double[]) field.get(null);
        assertTrue(ArraysExt.isSorted(values, true));

        field = DoubleDouble.class.getDeclaredField("ERRORS");
        field.setAccessible(true);
        final double[] errors = (double[]) field.get(null);
        assertEquals(values.length, errors.length);

        for (int i=0; i<values.length; i++) {
            final double value = values[i];
            final double delta = DecimalFunctions.deltaForDoubleToDecimal(value);
            if (delta == errors[i]) {
                fail("(value,entry) pair for value " + value + " can be omitted.");
            }
        }
    }

    /**
     * Tests {@link DoubleDouble#errorForWellKnownValue(double)}.
     */
    @Test
    public void testErrorForWellKnownValue() {
        for (final String text : PREDEFINED_VALUES) {
            final double     value         = Double.parseDouble(text);
            final BigDecimal accurate      = new BigDecimal(text);
            final BigDecimal approximation = new BigDecimal(value);
            final double     expected      = accurate.subtract(approximation).doubleValue();
            assertEquals( expected, DoubleDouble.errorForWellKnownValue( value), text);
            assertEquals(-expected, DoubleDouble.errorForWellKnownValue(-value), text);
            assertNotEquals(expected, 0, "There is no point to define an entry for values having no error.");
        }
    }

    /**
     * Tests π values using the {@link StrictMath#PI} constant.
     * This test method serves two purposes:
     *
     * <ul>
     *   <li>Ensure that the results of small arithmetic operations on {@link StrictMath#PI} produce
     *       numbers that {@link DoubleDouble#errorForWellKnownValue(double)} can find.</li>
     *   <li>Compare with the values computed by the {@code qd-2.3.14} package (a C/C++ library),
     *       which is taken as the reference implementation.</li>
     * </ul>
     */
    @Test
    public void testPI() {
        assertEquals(1.224646799147353207E-16, DoubleDouble.errorForWellKnownValue(PI    ));
        assertEquals(2.449293598294706414E-16, DoubleDouble.errorForWellKnownValue(PI * 2));
        assertEquals(6.123233995736766036E-17, DoubleDouble.errorForWellKnownValue(PI / 2));
        assertEquals(3.061616997868383018E-17, DoubleDouble.errorForWellKnownValue(PI / 4));
        assertEquals(9.184850993605148436E-17, DoubleDouble.errorForWellKnownValue(PI * (3./4)));
        assertEquals(9.320078015422868E-23,    DoubleDouble.errorForWellKnownValue(PI / (180 * 60 * 60)));

        // Following is actually an anti-regression test.
        assertEquals(-1.9878495670576283E-15, DoubleDouble.errorForWellKnownValue(180 / PI));
        assertEquals(-1.9878495670576283E-15, DoubleDouble.errorForWellKnownValue(toDegrees(1)));
        assertEquals( 2.9486522708701687E-19, DoubleDouble.errorForWellKnownValue(PI / 180));
        assertEquals( 2.9486522708701687E-19, DoubleDouble.errorForWellKnownValue(toRadians(1)));
    }

    /**
     * Tests the {@code DoubleDouble} constants.
     */
    @Test
    public void testConstants() {
        for (int i=0; ; i++) {
            final DoubleDouble dd;
            switch (i) {
                case 0:  dd = DoubleDouble.PI;                 break;
                case 1:  dd = DoubleDouble.RADIANS_TO_DEGREES; break;
                case 2:  dd = DoubleDouble.DEGREES_TO_RADIANS; break;
                case 3:  dd = DoubleDouble.SECONDS_TO_RADIANS; break;
                default: return;                                             // Test done.
            }
            assertEquals(DoubleDouble.errorForWellKnownValue(dd.value), dd.error);
        }
    }

    /**
     * Tests initialization with a long value.
     */
    @Test
    public void testLong() {
        long value = Long.MAX_VALUE - 10;
        DoubleDouble t = DoubleDouble.of(value);
        assertEquals(-10, t.error);
        assertEquals(value, t.longValue());

        value = Long.MIN_VALUE + 10;
        t = DoubleDouble.of(value);
        assertEquals(10, t.error);
        assertEquals(value, t.longValue());
    }
}
