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
import org.apache.sis.test.DependsOnMethod;

import static org.junit.Assert.*;
import static java.lang.Double.*;
import static org.apache.sis.math.MathFunctions.*;
import static org.apache.sis.util.ArraysExt.isSorted;
import static org.apache.sis.internal.util.Numerics.SIGNIFICAND_SIZE;

// Related to JDK8
import org.apache.sis.internal.jdk8.JDK8;


/**
 * Tests the {@link MathFunctions} static methods.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
@DependsOn({
    org.apache.sis.util.ArraysExtTest.class,
    org.apache.sis.internal.util.NumericsTest.class
})
public final strictfp class MathFunctionsTest extends TestCase {
    /**
     * Small number for floating point comparisons.
     */
    private static final double EPS = 1E-12;

    /**
     * Highest prime number representable as a signed {@code short}.
     */
    private static final int HIGHEST_SHORT_PRIME = 32749;

    /**
     * The lowest prime number for which unsigned {@code short} is necessary.
     */
    private static final int LOWEST_USHORT_PRIME = 32771;

    /**
     * Verifies the values of {@link MathFunctions#SQRT_2} and {@link MathFunctions#LOG10_2}.
     */
    @Test
    public void testConstants() {
        assertEquals(StrictMath.sqrt (2), SQRT_2,  STRICT);
        assertEquals(StrictMath.log10(2), LOG10_2, STRICT);
    }

    /**
     * Tests {@link MathFunctions#truncate(double)}.
     */
    @Test
    @DependsOnMethod({"testIsPositiveZero", "testIsNegativeZero"})
    public void testTruncate() {
        assertEquals(+4.0, truncate(+4.9), STRICT);
        assertEquals(-4.0, truncate(-4.9), STRICT);
        assertEquals(+0.0, truncate(+0.1), STRICT);
        assertEquals(-0.0, truncate(-0.1), STRICT);
        assertTrue("Positive zero", isPositiveZero(truncate(+0.5)));
        assertTrue("Negative zero", isNegativeZero(truncate(-0.5)));
        assertTrue("Positive zero", isPositiveZero(truncate(+0.0)));
        assertTrue("Negative zero", isNegativeZero(truncate(-0.0)));
    }

    /**
     * Tests the {@link MathFunctions#magnitude(double[])} method.
     */
    @Test
    public void testMagnitude() {
        assertEquals(0, magnitude(), EPS);
        assertEquals(4, magnitude(0, -4, 0), EPS);
        assertEquals(5, magnitude(0, -4, 0, 3, 0), EPS);
        assertEquals(5, magnitude(3, 1, -2, 1, -3, -1), EPS);
    }

    /**
     * Tests the {@link MathFunctions#getExponent(double)} method.
     * This method performs two tests:
     *
     * <ul>
     *   <li>First, tests with a few normal (non-subnormal) numbers.
     *       The result shall be identical to {@link StrictMath#getExponent(double)}.</li>
     *   <li>Then, test with a few sub-normal numbers.</li>
     * </ul>
     */
    @Test
    public void testGetExponent() {
        final double[] normalValues = {
            1E+300, 1E+200, 1E+100, 1E+10, 50, 20, 1, 1E-10, 1E-100, 1E-200, 1E-300,
            POSITIVE_INFINITY,
            NEGATIVE_INFINITY,
            NaN,
            MAX_VALUE,
            MIN_NORMAL
        };
        for (final double value : normalValues) {
            assertEquals(StrictMath.getExponent(value), getExponent(value));
        }
        /*
         * Tests sub-normal values. We expect:
         *
         *   getExponent(MIN_NORMAL    )  ==  MIN_EXPONENT
         *   getExponent(MIN_NORMAL / 2)  ==  MIN_EXPONENT - 1
         *   getExponent(MIN_NORMAL / 4)  ==  MIN_EXPONENT - 2
         *   getExponent(MIN_NORMAL / 8)  ==  MIN_EXPONENT - 3
         *   etc.
         */
        for (int i=0; i<=SIGNIFICAND_SIZE; i++) {
            assertEquals(MIN_EXPONENT - i, getExponent(MIN_NORMAL / (1L << i)));
        }
        assertEquals(MIN_EXPONENT - 1,                    getExponent(JDK8.nextDown(MIN_NORMAL)));
        assertEquals(MIN_EXPONENT - SIGNIFICAND_SIZE,     getExponent(MIN_VALUE));
        assertEquals(MIN_EXPONENT - SIGNIFICAND_SIZE - 1, getExponent(0));
        /*
         * Tests consistency with scalb, as documented in MathFunctions.getExponent(double) javadoc.
         */
        for (int i = MIN_EXPONENT - SIGNIFICAND_SIZE - 1; i <= MAX_EXPONENT + 1; i++) {
            assertEquals(i, getExponent(StrictMath.scalb(1.0, i)));
        }
        /*
         * Tests consistency with log10, as documented in MathFunctions.getExponent(double) javadoc.
         */
        for (int i = MIN_EXPONENT - SIGNIFICAND_SIZE; i <= MAX_EXPONENT; i++) {
            assertEquals(StrictMath.floor(StrictMath.log10(StrictMath.scalb(1.0, i))),
                         StrictMath.floor(LOG10_2 * i /* i = getExponent(value) */), STRICT);
        }
    }

    /**
     * Tests the {@link MathFunctions#asinh(double)} method in the [-10 … +10] range.
     */
    @Test
    public void testAsinh() {
        for (int i=-100; i<=100; i++) {
            final double x = 0.1 * i;
            final double y = asinh(x);
            assertEquals(x, StrictMath.sinh(y), EPS);
        }
    }

    /**
     * Tests the {@link MathFunctions#acosh(double)} method in the [1 … +10] range.
     */
    @Test
    public void testAcosh() {
        for (int i=10; i<=100; i++) {
            final double x = 0.1 * i;
            final double y = acosh(x);
            assertEquals(x, StrictMath.cosh(y), EPS);
        }
    }

    /**
     * Tests the {@link MathFunctions#atanh(double)} method in the [-1 … +1] range.
     */
    @Test
    public void testAtanh() {
        for (int i=-10; i<=10; i++) {
            final double x = 0.1 * i;
            final double y = atanh(x);
            switch (i) {
                case -10: assertEquals(NEGATIVE_INFINITY, y,  EPS); break;
                default:  assertEquals(x, StrictMath.tanh(y), EPS); break;
                case +10: assertEquals(POSITIVE_INFINITY, y,  EPS); break;
            }
        }
    }

    /**
     * Tests {@link MathFunctions#isPositiveZero(double)}.
     */
    @Test
    public void testIsPositiveZero() {
        assertTrue (isPositiveZero(+0.0));
        assertFalse(isPositiveZero(-0.0));
        assertFalse(isPositiveZero( NaN));
    }

    /**
     * Tests {@link MathFunctions#isNegativeZero(double)}.
     */
    @Test
    public void testIsNegativeZero() {
        assertTrue (isNegativeZero(-0.0));
        assertFalse(isNegativeZero(+0.0));
        assertFalse(isNegativeZero( NaN));
    }

    /**
     * Tests the {@link MathFunctions#xorSign(double, double)} method.
     */
    @Test
    public void testXorSign() {
        assertEquals( 10, xorSign( 10,  0.5), STRICT);
        assertEquals(-10, xorSign(-10,  0.5), STRICT);
        assertEquals( 10, xorSign(-10, -0.5), STRICT);
        assertEquals(-10, xorSign( 10, -0.5), STRICT);
    }

    /**
     * Tests the {@link MathFunctions#epsilonEqual(float, float, float)} and
     * {@link MathFunctions#epsilonEqual(double, double, double)} methods.
     */
    @Test
    public void testEpsilonEqual() {
        assertTrue (epsilonEqual(10.0, 12.0, 2.0));
        assertFalse(epsilonEqual(10.0, 12.0, 1.0));
        assertTrue (epsilonEqual(Double.NaN, Double.NaN, 1.0));
        assertTrue (epsilonEqual(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 1.0));
        assertTrue (epsilonEqual(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, 1.0));
        assertFalse(epsilonEqual(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1.0));

        // Same tests using the 'float' version.
        assertTrue (epsilonEqual(10f, 12f, 2f));
        assertFalse(epsilonEqual(10f, 12f, 1f));
        assertTrue (epsilonEqual(Float.NaN, Float.NaN, 1f));
        assertTrue (epsilonEqual(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 1f));
        assertTrue (epsilonEqual(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, 1f));
        assertFalse(epsilonEqual(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 1f));
    }

    /**
     * Tests the {@link MathFunctions#toNanFloat(int)} method. This will indirectly test the
     * converse {@link MathFunctions#toNanOrdinal(float)} method through Java assertions.
     */
    public void testToNanFloat() {
        final int standardNaN = Float.floatToRawIntBits(Float.NaN);
        for (int ordinal = 0; ordinal < MathFunctions.MAX_NAN_ORDINAL; ordinal += 256) {
            final float vp = toNanFloat(+ordinal);
            final float vn = toNanFloat(-ordinal);
            final int   bp = Float.floatToRawIntBits(vp);
            final int   bn = Float.floatToRawIntBits(vn);
            assertEquals(ordinal == 0, standardNaN == bp);
            assertEquals(ordinal == 0, standardNaN == bn);
            assertEquals(ordinal == 0, bp == bn);
        }
    }

    /**
     * Tests the {@link MathFunctions#quadrupleToDouble(long, long)} method. Values used in this test are taken from
     * <a href="https://en.wikipedia.org/wiki/Quadruple-precision_floating-point_format">Quadruple-precision
     * floating-point format</a> on Wikipedia.
     */
    @Test
    public void testQuadrupleToDouble(){
        long l0, l1;

        // 1.0
        l0 = 0x3FFF000000000000L;
        l1 = 0x0000000000000000L;
        assertEquals(doubleToLongBits(1.0),
                     doubleToLongBits(quadrupleToDouble(l0, l1)));

        // -2.0
        l0 = 0xC000000000000000L;
        l1 = 0x0000000000000000L;
        assertEquals(doubleToLongBits(-2.0),
                     doubleToLongBits(quadrupleToDouble(l0, l1)));

        // 3.1415926535897932384626433832795028
        l0 = 0x4000921FB54442D1L;
        l1 = 0x8469898CC51701B8L;
        assertEquals(doubleToLongBits(3.1415926535897932384626433832795028),
                     doubleToLongBits(quadrupleToDouble(l0, l1)));

        // ~1/3
        l0 = 0x3FFD555555555555L;
        l1 = 0x5555555555555555L;
        assertEquals(doubleToLongBits(1.0/3.0),
                     doubleToLongBits(quadrupleToDouble(l0, l1)));

        // positive zero
        l0 = 0x0000000000000000L;
        l1 = 0x0000000000000000L;
        assertEquals(doubleToLongBits(+0.0),
                     doubleToLongBits(quadrupleToDouble(l0, l1)));

        // negative zero
        l0 = 0x8000000000000000L;
        l1 = 0x0000000000000000L;
        assertEquals(doubleToLongBits(-0.0),
                     doubleToLongBits(quadrupleToDouble(l0, l1)));

        // positive infinite
        l0 = 0x7FFF000000000000L;
        l1 = 0x0000000000000000L;
        assertEquals(doubleToLongBits(Double.POSITIVE_INFINITY),
                     doubleToLongBits(quadrupleToDouble(l0, l1)));

        // negative infinite
        l0 = 0xFFFF000000000000L;
        l1 = 0x0000000000000000L;
        assertEquals(doubleToLongBits(Double.NEGATIVE_INFINITY),
                     doubleToLongBits(quadrupleToDouble(l0, l1)));

        // a random NaN
        l0 = 0x7FFF000100040000L;
        l1 = 0x0001005000080000L;
        assertEquals(doubleToLongBits(Double.NaN),
                     doubleToLongBits(quadrupleToDouble(l0, l1)));
    }

    /**
     * Tests the {@link MathFunctions#primeNumberAt(int)} method.
     */
    @Test
    public void testPrimeNumberAt() {
        final int[] primes = {
              2,   3,   5,   7,  11,  13,  17,  19,  23,  29,  31,  37,  41,  43,  47,  53 , 59,
             61,  67,  71,  73,  79,  83,  89,  97, 101, 103, 107, 109, 113, 127, 131, 137, 139,
            149, 151, 157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 211, 223, 227, 229, 233,
            239, 241, 251, 257, 263, 269, 271, 277, 281, 283, 293, 307, 311, 313, 317, 331, 337,
            347, 349, 353, 359, 367, 373, 379, 383, 389, 397, 401, 409, 419, 421, 431, 433, 439,
            443, 449, 457, 461, 463, 467, 479, 487, 491, 499, 503, 509, 521, 523, 541, 547, 557,
            563, 569, 571, 577, 587, 593, 599, 601, 607, 613, 617, 619, 631, 641, 643, 647, 653,
            659, 661, 673, 677, 683, 691, 701, 709, 719, 727, 733, 739, 743, 751, 757, 761, 769,
            773, 787, 797, 809, 811, 821, 823, 827, 829, 839, 853, 857, 859, 863, 877, 881, 883,
            887, 907, 911, 919, 929, 937, 941, 947, 953, 967, 971, 977, 983, 991, 997
        };
        for (int i=0; i<primes.length; i++) {
            assertEquals(primes[i], primeNumberAt(i));
        }
        assertEquals(HIGHEST_SHORT_PRIME,            primeNumberAt(PRIMES_LENGTH_15_BITS - 1));
        assertEquals(HIGHEST_SUPPORTED_PRIME_NUMBER, primeNumberAt(PRIMES_LENGTH_16_BITS - 1));
    }

    /**
     * Tests the {@link MathFunctions#nextPrimeNumber(int)} method.
     */
    @Test
    @DependsOnMethod("testPrimeNumberAt")
    public void testNextPrimeNumber() {
        assertEquals(151, nextPrimeNumber(151));
        assertEquals(157, nextPrimeNumber(152));
        assertEquals(997, nextPrimeNumber(996));
        assertEquals(HIGHEST_SHORT_PRIME,            nextPrimeNumber(HIGHEST_SHORT_PRIME - 1));
        assertEquals(HIGHEST_SHORT_PRIME,            nextPrimeNumber(HIGHEST_SHORT_PRIME));
        assertEquals(LOWEST_USHORT_PRIME,            nextPrimeNumber(HIGHEST_SHORT_PRIME + 1));
        assertEquals(32779,                          nextPrimeNumber(LOWEST_USHORT_PRIME + 1));
        assertEquals(HIGHEST_SUPPORTED_PRIME_NUMBER, nextPrimeNumber(HIGHEST_SUPPORTED_PRIME_NUMBER - 1));
        assertEquals(HIGHEST_SUPPORTED_PRIME_NUMBER, nextPrimeNumber(HIGHEST_SUPPORTED_PRIME_NUMBER));
    }

    /**
     * Tests the {@link MathFunctions#divisors(int)} method.
     */
    @Test
    @DependsOnMethod("testPrimeNumberAt")
    public void testDivisors() {
        for (int i=0; i<10000; i++) {
            final int[] divisors = divisors(i);
            assertTrue(isSorted(divisors, true));
            for (int j=0; j<divisors.length; j++) {
                assertEquals(0, i % divisors[j]);
            }
            if (i == 0){
                assertEquals(0, divisors.length);
            } else {
                assertEquals(1, divisors[0]);
                assertEquals(i, divisors[divisors.length - 1]);
            }
        }
        assertArrayEquals(new int[] {
            1, 2, 4, 5, 8, 10, 16, 20, 25, 40, 50, 80, 100, 125, 200, 250, 400, 500, 1000, 2000
        }, divisors(2000));

        assertArrayEquals(new int[] {
            1, 61, 71, 4331
        }, divisors(4331));

        assertArrayEquals(new int[] {
            1, 2, 3, 4, 5, 6, 8, 10, 12, 13, 15, 20, 24, 25, 26, 30, 39, 40, 50, 52, 60, 65, 75,
            78, 100, 104, 120, 130, 150, 156, 195, 200, 260, 300, 312, 325, 390, 520, 600, 650,
            780, 975, 1300, 1560, 1950, 2600, 3900, 7800
        }, divisors(7800));
    }

    /**
     * Tests the {@link MathFunctions#commonDivisors(int[])} method.
     */
    @Test
    @DependsOnMethod("testDivisors")
    public void testCommonDivisors() {
        assertArrayEquals(new int[] {
            1, 5
        }, commonDivisors(2000, 15));
    }
}
