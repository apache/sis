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
package org.apache.sis.internal.util;

import java.util.Random;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.ComparisonMode;
import org.junit.Test;

import static java.lang.Double.NaN;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Double.NEGATIVE_INFINITY;
import static org.apache.sis.internal.util.Numerics.*;
import static org.junit.Assert.*;


/**
 * Tests the {@link Numerics} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.6
 * @module
 */
@SuppressWarnings("UnnecessaryBoxing")
public final strictfp class NumericsTest extends TestCase {
    /**
     * Tests the {@link Numerics#cached(Object)} method.
     */
    @Test
    public void testCached() {
        Double value;
        assertEquals(value = Double.valueOf(   0), Numerics.cached(value));
        assertEquals(value = Double.valueOf(   1), Numerics.cached(value));
        assertEquals(value = Double.valueOf(  -1), Numerics.cached(value));
        assertEquals(value = Double.valueOf(  10), Numerics.cached(value));
        assertEquals(value = Double.valueOf(-150), Numerics.cached(value));
        assertEquals(value = Double.valueOf( NaN), Numerics.cached(value));
    }

    /**
     * Tests the {@link Numerics#valueOf(double)} method.
     */
    @Test
    public void testValueOf() {
        double value;
        assertEquals(Double.valueOf(value =    0), Numerics.valueOf(value));
        assertEquals(Double.valueOf(value =    1), Numerics.valueOf(value));
        assertEquals(Double.valueOf(value =   -1), Numerics.valueOf(value));
        assertEquals(Double.valueOf(value =   10), Numerics.valueOf(value));
        assertEquals(Double.valueOf(value = -150), Numerics.valueOf(value));
        assertEquals(Double.valueOf(value =  NaN), Numerics.valueOf(value));
    }

    /**
     * Tests the {@link Numerics#epsilonEqual(double, double, ComparisonMode)} method.
     */
    @Test
    public void testEpsilonEqual() {
        assertTrue (epsilonEqual(POSITIVE_INFINITY, POSITIVE_INFINITY,    ComparisonMode.APPROXIMATIVE));
        assertTrue (epsilonEqual(NEGATIVE_INFINITY, NEGATIVE_INFINITY,    ComparisonMode.APPROXIMATIVE));
        assertFalse(epsilonEqual(POSITIVE_INFINITY, NEGATIVE_INFINITY,    ComparisonMode.APPROXIMATIVE));
        assertFalse(epsilonEqual(POSITIVE_INFINITY, NaN,                  ComparisonMode.APPROXIMATIVE));
        assertTrue (epsilonEqual(NaN,               NaN,                  ComparisonMode.APPROXIMATIVE));
        assertFalse(epsilonEqual(   0,        COMPARISON_THRESHOLD /   2, ComparisonMode.APPROXIMATIVE));
        assertTrue (epsilonEqual(   1,    1 + COMPARISON_THRESHOLD /   2, ComparisonMode.APPROXIMATIVE));
        assertFalse(epsilonEqual(   1,    1 + COMPARISON_THRESHOLD *   2, ComparisonMode.APPROXIMATIVE));
        assertTrue (epsilonEqual(-100, -100 + COMPARISON_THRESHOLD *  50, ComparisonMode.APPROXIMATIVE));
        assertFalse(epsilonEqual( 100,  100 + COMPARISON_THRESHOLD * 150, ComparisonMode.APPROXIMATIVE));
    }

    /**
     * Tests the {@link Numerics#toExp10(int)} method over the full [-2620 … 2620] range of values
     * (the validity range documented by method javadoc). Also verifies our javadoc claim that
     * {@code toExp10(getExponent(10ⁿ))} returns {@code n-1} except for {@code n == 0}.
     */
    @Test
    public void testToExp10() {
        for (int i=-2620; i<=2620; i++) {
            assertEquals(StrictMath.floor(i * MathFunctions.LOG10_2), toExp10(i), 0);
        }
        for (int i=-307; i<=308; i++) {
            final String value = "1E" + i;
            assertEquals(value, (i == 0) ? i : i-1, toExp10(StrictMath.getExponent(Double.parseDouble(value))));
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
        final int e = StrictMath.getExponent(value) - SIGNIFICAND_SIZE;
        final double recomposed = StrictMath.scalb((double) expected, e);
        assertEquals(value, StrictMath.copySign(recomposed, value), 0);
    }

    /**
     * Asserts that {@link Numerics#getSignificand(float)} returns the expected value,
     * then verify the {@link StrictMath#scalb(float, int)} identity.
     */
    private static void assertSignificandEquals(final int expected, final float value) {
        assertEquals(expected, getSignificand(value));
        final int e = StrictMath.getExponent(value) - SIGNIFICAND_SIZE_OF_FLOAT;
        final float recomposed = StrictMath.scalb((float) expected, e);
        assertEquals(value, StrictMath.copySign(recomposed, value), 0f);
    }
}
