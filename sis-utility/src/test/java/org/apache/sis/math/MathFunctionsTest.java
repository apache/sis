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
import static org.apache.sis.math.MathFunctions.*;
import static org.apache.sis.util.ArraysExt.isSorted;


/**
 * Tests the {@link MathFunctions} static methods.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.3
 * @module
 */
@DependsOn(org.apache.sis.util.ArraysTest.class)
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
     * Tests {@link MathFunctions#truncate(double)}.
     */
    @Test
    public void testTruncate() {
        assertEquals(+4, truncate(+4.9), 0);
        assertEquals(-4, truncate(-4.9), 0);
        assertEquals("Positive zero",
                Double.doubleToLongBits(+0.0),
                Double.doubleToLongBits(truncate(+0.5)));
        assertEquals("Negative zero",
                Double.doubleToLongBits(-0.0),
                Double.doubleToLongBits(truncate(-0.5)));
        assertEquals("Positive zero",
                Double.doubleToLongBits(+0.0),
                Double.doubleToLongBits(truncate(+0.0)));
        assertEquals("Negative zero",
                Double.doubleToLongBits(-0.0),
                Double.doubleToLongBits(truncate(-0.0)));
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
     * Tests {@link MathFunctions#fractionDigitsForDelta(double, boolean)}.
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

        // Special cases.
        assertEquals(0,  fractionDigitsForDelta(0, true));
        assertEquals(0,  fractionDigitsForDelta(Double.NaN, true));
        assertEquals(0,  fractionDigitsForDelta(Double.POSITIVE_INFINITY, true));
        assertEquals(0,  fractionDigitsForDelta(Double.NEGATIVE_INFINITY, true));
    }

    /**
     * Tests the {@link MathFunctions#pow10(double)} method.
     * This will indirectly test {@link MathFunctions#pow10(int)}
     * since the former will delegate to the later in this test.
     */
    @Test
    public void testPow10() {
        for (int i=-304; i<=304; i++) { // Range of allowed exponents in base 10.
            assertEquals(Double.parseDouble("1E"+i), pow10((double) i), 0);
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
                case -10: assertEquals(Double.NEGATIVE_INFINITY, y, EPS); break;
                default:  assertEquals(x, StrictMath.tanh(y),       EPS); break;
                case +10: assertEquals(Double.POSITIVE_INFINITY, y, EPS); break;
            }
        }
    }

    /**
     * Tests the {@link MathFunctions#xorSign(double, double)} method.
     */
    @Test
    public void testXorSign() {
        assertEquals( 10, xorSign( 10,  0.5), 0);
        assertEquals(-10, xorSign(-10,  0.5), 0);
        assertEquals( 10, xorSign(-10, -0.5), 0);
        assertEquals(-10, xorSign( 10, -0.5), 0);
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
