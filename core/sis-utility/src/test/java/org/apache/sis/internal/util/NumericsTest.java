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
 * @version 0.4
 * @module
 */
public final strictfp class NumericsTest extends TestCase {
    /**
     * Tests the {@link Numerics#epsilonEqual(double, double)} method.
     */
    @Test
    public void testEpsilonEqual() {
        assertTrue (epsilonEqual(POSITIVE_INFINITY, POSITIVE_INFINITY));
        assertTrue (epsilonEqual(NEGATIVE_INFINITY, NEGATIVE_INFINITY));
        assertFalse(epsilonEqual(POSITIVE_INFINITY, NEGATIVE_INFINITY));
        assertFalse(epsilonEqual(POSITIVE_INFINITY, NaN));
        assertTrue (epsilonEqual(NaN,               NaN));
        assertFalse(epsilonEqual(   0,        COMPARISON_THRESHOLD / 2));
        assertTrue (epsilonEqual(   1,    1 + COMPARISON_THRESHOLD / 2));
        assertFalse(epsilonEqual(   1,    1 + COMPARISON_THRESHOLD * 2));
        assertTrue (epsilonEqual(-100, -100 + COMPARISON_THRESHOLD * 50));
        assertFalse(epsilonEqual( 100,  100 + COMPARISON_THRESHOLD * 150));
    }

    /**
     * Tests the {@link Numerics#toExp10(int)} method over the full [-2620 … 2620] range of values.
     * This is the range documented as valid.
     */
    @Test
    public void testToExp10() {
        for (int i=-2620; i<=2620; i++) {
            assertEquals(Math.floor(i * MathFunctions.LOG10_2), toExp10(i), 0);
        }
    }

    /**
     * Tests the {@link Numerics#getSignificand(double)} method.
     */
    @Test
    public void testGetSignificand() {
        assertEquals(0x00000000000000L, getSignificand(0d));
        assertEquals(0x10000000000000L, getSignificand(1d));
        assertEquals(0x1F400000000000L, getSignificand(1000d));
        assertEquals(0x1FFFFFFFFFFFFFL, getSignificand(Double.MAX_VALUE));
        assertEquals(0x10000000000000L, getSignificand(Double.MIN_NORMAL));
        assertEquals(0x00000000000002L, getSignificand(Double.MIN_VALUE));
        final Random random = TestUtilities.createRandomNumberGenerator();
        for (int i=0; i<100; i++) {
            final double value       = random.nextGaussian();
            final double significand = getSignificand(value);
            final double recomposed  = StrictMath.scalb(significand, StrictMath.getExponent(value) - SIGNIFICAND_SIZE);
            assertEquals(value, StrictMath.copySign(recomposed, value), 0);
        }
    }

    /**
     * Tests the {@link Numerics#getSignificand(float)} method.
     */
    @Test
    public void testGetSignificandOfFloat() {
        assertEquals(0x000000, getSignificand(0f));
        assertEquals(0x800000, getSignificand(1f));
        assertEquals(0xFA0000, getSignificand(1000f));
        assertEquals(0xFFFFFF, getSignificand(Float.MAX_VALUE));
        assertEquals(0x800000, getSignificand(Float.MIN_NORMAL));
        assertEquals(0x000002, getSignificand(Float.MIN_VALUE));
        final Random random = TestUtilities.createRandomNumberGenerator();
        for (int i=0; i<100; i++) {
            final float value       = (float) random.nextGaussian();
            final float significand = getSignificand(value);
            final float recomposed  = StrictMath.scalb(significand, StrictMath.getExponent(value) - SIGNIFICAND_SIZE_OF_FLOAT);
            assertEquals(value, StrictMath.copySign(recomposed, value), 0);
        }
    }
}
