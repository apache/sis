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
import java.math.BigInteger;
import static java.lang.Double.NaN;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Double.NEGATIVE_INFINITY;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.util.ComparisonMode;
import static org.apache.sis.util.internal.shared.Numerics.*;
import static org.apache.sis.pending.jdk.JDK19.FLOAT_PRECISION;
import static org.apache.sis.pending.jdk.JDK19.DOUBLE_PRECISION;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link Numerics} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class NumericsTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public NumericsTest() {
    }

    /**
     * Verifies the value of {@link Numerics#LONG_SHIFT} and {@link Numerics#INT_SHIFT}.
     */
    @Test
    public void verifyMaxDimension() {
        assertEquals(Integer.SIZE, 1 << Numerics.INT_SHIFT);
        assertEquals(Long.SIZE, 1 << Numerics.LONG_SHIFT);
        for (int i=350; i<400; i += 17) {
            assertEquals(i / Long.SIZE, i >> Numerics.LONG_SHIFT);
            assertEquals(i * Long.SIZE, i << Numerics.LONG_SHIFT);
        }
    }

    /**
     * Tests {@link Numerics#bitmask(int)}.
     */
    @Test
    public void testBitmask() {
        assertEquals( 1L, Numerics.bitmask(0));
        assertEquals( 2L, Numerics.bitmask(1));
        assertEquals(32L, Numerics.bitmask(5));
        assertEquals(Long.SIZE, Numerics.bitmask(Numerics.LONG_SHIFT));
        assertEquals(Long.MIN_VALUE, Numerics.bitmask(Long.SIZE - 1));
        assertEquals( 0L, Numerics.bitmask(Long.SIZE));
        assertEquals( 0L, Numerics.bitmask(100));
        assertEquals( 0L, Numerics.bitmask(-1));
        assertEquals( 0L, Numerics.bitmask(-256));
    }

    /**
     * Tests {@link Numerics#snapToCeil(int, int)}.
     */
    public void testSnapToCeil() {
        boolean negative = false;
        do {    // Executed exactly twice.
            final int divisor = negative ? -3 : 3;
            assertEquals(  9, Numerics.snapToCeil(  9, divisor));
            assertEquals( 12, Numerics.snapToCeil( 10, divisor));
            assertEquals( 12, Numerics.snapToCeil( 11, divisor));
            assertEquals( 12, Numerics.snapToCeil( 12, divisor));
            assertEquals( -9, Numerics.snapToCeil( -9, divisor));
            assertEquals( -9, Numerics.snapToCeil(-10, divisor));
            assertEquals( -9, Numerics.snapToCeil(-11, divisor));
            assertEquals(-12, Numerics.snapToCeil(-12, divisor));
        } while (negative = !negative);
    }

    /**
     * Tests {@link Numerics#multiplyDivide(long, long, long)}.
     * This method uses {@link BigInteger} as a reference.
     */
    @Test
    public void testMultiplyDivide() {
        final Random random = TestUtilities.createRandomNumberGenerator();
        for (int i=0; i<10; i++) {
            final long value      = random.nextLong();
            final long multiplier = random.nextLong();
            final long divisor    = random.nextLong();              // Accepted also 0.
            final long expected;
            try {
                // ArithmeticException may occur because of overflow or division by 0.
                expected = BigInteger.valueOf(value).multiply(BigInteger.valueOf(multiplier))
                                     .divide(BigInteger.valueOf(divisor)).longValueExact();
            } catch (ArithmeticException e) {
                // If BigInteger fails, then `multiplyDivide(…)` shall also fail.
                try {
                    Numerics.multiplyDivide(value, multiplier, divisor);
                    fail("Expected " + e);
                } catch (ArithmeticException ex) {
                    // Expected exception.
                    continue;
                }
                throw e;
            }
            final long actual = Numerics.multiplyDivide(value, multiplier, divisor);
            assertEquals(expected, actual);
        }
    }

    /**
     * Tests {@link Numerics#saturatingAdd(long, int)}.
     */
    @Test
    public void testSaturatingAdd() {
        assertEquals(1234 + 56, Numerics.saturatingAdd(1234,  56));
        assertEquals(1234 - 56, Numerics.saturatingAdd(1234, -56));
        assertEquals(Long.MAX_VALUE, Numerics.saturatingAdd(Long.MAX_VALUE - 10,  56));
        assertEquals(Long.MIN_VALUE, Numerics.saturatingAdd(Long.MIN_VALUE + 10, -56));
    }

    /**
     * Tests {@link Numerics#saturatingSubtract(long, int)}.
     */
    @Test
    public void testSaturatingSubtract() {
        assertEquals(1234 - 56, Numerics.saturatingSubtract(1234,  56));
        assertEquals(1234 + 56, Numerics.saturatingSubtract(1234, -56));
        assertEquals(Long.MAX_VALUE, Numerics.saturatingSubtract(Long.MAX_VALUE - 10, -56));
        assertEquals(Long.MIN_VALUE, Numerics.saturatingSubtract(Long.MIN_VALUE + 10, +56));
    }

    /**
     * Tests the {@link Numerics#epsilonEqual(double, double, ComparisonMode)} method.
     */
    @Test
    public void testEpsilonEqual() {
        assertTrue (epsilonEqual(POSITIVE_INFINITY, POSITIVE_INFINITY,    ComparisonMode.APPROXIMATE));
        assertTrue (epsilonEqual(NEGATIVE_INFINITY, NEGATIVE_INFINITY,    ComparisonMode.APPROXIMATE));
        assertFalse(epsilonEqual(POSITIVE_INFINITY, NEGATIVE_INFINITY,    ComparisonMode.APPROXIMATE));
        assertFalse(epsilonEqual(POSITIVE_INFINITY, NaN,                  ComparisonMode.APPROXIMATE));
        assertTrue (epsilonEqual(NaN,               NaN,                  ComparisonMode.APPROXIMATE));
        assertFalse(epsilonEqual(   0,        COMPARISON_THRESHOLD /   2, ComparisonMode.APPROXIMATE));
        assertTrue (epsilonEqual(   1,    1 + COMPARISON_THRESHOLD /   2, ComparisonMode.APPROXIMATE));
        assertFalse(epsilonEqual(   1,    1 + COMPARISON_THRESHOLD *   2, ComparisonMode.APPROXIMATE));
        assertTrue (epsilonEqual(-100, -100 + COMPARISON_THRESHOLD *  50, ComparisonMode.APPROXIMATE));
        assertFalse(epsilonEqual( 100,  100 + COMPARISON_THRESHOLD * 150, ComparisonMode.APPROXIMATE));
    }

    /**
     * Tests the {@link Numerics#toExp10(int)} method over the full [-2620 … 2620] range of values
     * (the validity range documented by method javadoc). Also verifies our javadoc claim that
     * {@code toExp10(getExponent(10ⁿ))} returns {@code n-1} except for {@code n == 0}.
     */
    @Test
    public void testToExp10() {
        for (int i=-2620; i<=2620; i++) {
            assertEquals(StrictMath.floor(i * MathFunctions.LOG10_2), toExp10(i));
        }
        for (int i=-307; i<=308; i++) {
            final String value = "1E" + i;
            assertEquals((i == 0) ? i : i-1, toExp10(StrictMath.getExponent(Double.parseDouble(value))), value);
        }
    }

    /**
     * Tests the {@link Numerics#getSignificand(double)} method.
     */
    @Test
    public void testGetSignificand() {
        assertSignificandEquals(0x00000000000000L, 0d);
        assertSignificandEquals(0x10000000000000L, 1d);
        assertSignificandEquals(0x1F400000000000L, 1000d);
        assertSignificandEquals(0x1FFFFFFFFFFFFFL, Double.MAX_VALUE);
        assertSignificandEquals(0x10000000000000L, Double.MIN_NORMAL);
        assertSignificandEquals(0x00000000000002L, Double.MIN_VALUE);
        assertSignificandEquals(0x10000000000000L, Double.POSITIVE_INFINITY);
        assertSignificandEquals(0x10000000000000L, Double.NEGATIVE_INFINITY);
        final Random random = TestUtilities.createRandomNumberGenerator();
        for (int i=0; i<100; i++) {
            final double value = random.nextGaussian();
            assertSignificandEquals(getSignificand(value), -value);
        }
    }

    /**
     * Tests the {@link Numerics#getSignificand(float)} method.
     */
    @Test
    public void testGetSignificandOfFloat() {
        assertSignificandEquals(0x000000, 0f);
        assertSignificandEquals(0x800000, 1f);
        assertSignificandEquals(0xFA0000, 1000f);
        assertSignificandEquals(0xFFFFFF, Float.MAX_VALUE);
        assertSignificandEquals(0x800000, Float.MIN_NORMAL);
        assertSignificandEquals(0x000002, Float.MIN_VALUE);
        assertSignificandEquals(0x800000, Float.POSITIVE_INFINITY);
        assertSignificandEquals(0x800000, Float.NEGATIVE_INFINITY);
        final Random random = TestUtilities.createRandomNumberGenerator();
        for (int i=0; i<100; i++) {
            final float value = (float) random.nextGaussian();
            assertSignificandEquals(getSignificand(value), -value);
        }
    }

    /**
     * Asserts that {@link Numerics#getSignificand(double)} returns the expected value,
     * then verify the {@link StrictMath#scalb(double, int)} identity.
     */
    private static void assertSignificandEquals(final long expected, final double value) {
        assertEquals(expected, getSignificand(value));
        final int e = StrictMath.getExponent(value) - (DOUBLE_PRECISION - 1);
        final double recomposed = StrictMath.scalb((double) expected, e);
        assertEquals(value, StrictMath.copySign(recomposed, value));
    }

    /**
     * Asserts that {@link Numerics#getSignificand(float)} returns the expected value.
     * Also verifies the {@link StrictMath#scalb(float, int)} identity.
     */
    private static void assertSignificandEquals(final int expected, final float value) {
        assertEquals(expected, getSignificand(value));
        final int e = StrictMath.getExponent(value) - (FLOAT_PRECISION - 1);
        final float recomposed = StrictMath.scalb((float) expected, e);
        assertEquals(value, StrictMath.copySign(recomposed, value));
    }
}
