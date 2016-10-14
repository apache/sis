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

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link Fraction} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
public final strictfp class FractionTest extends TestCase {
    /**
     * Tests the {@link Fraction#floor()} method.
     */
    @Test
    public void testFloor() {
        final int[] numerators   = { 0,  1,  2,  3,  9, 10, 11, 12};
        final int[] denominators = { 3,  3,  3,  3,  3,  3,  3,  3};
        final int[] positives    = { 0,  0,  0,  1,  3,  3,  3,  4};
        final int[] negatives    = {-0, -1, -1, -1, -3, -4, -4, -4};
        for (int i=0; i<numerators.length; i++) {
            for (int s=0; s<4; s++) {
                int numerator   = numerators  [i];
                int denominator = denominators[i];
                if ((s & 1) != 0) numerator   = -numerator;
                if ((s & 2) != 0) denominator = -denominator;
                final int[] expected = (numerator * denominator >= 0) ? positives : negatives;
                final String label = "floor(" + numerator + '/' + denominator + ')';
                assertEquals(label, expected[i], new Fraction(numerator, denominator).floor());
            }
        }
    }

    /**
     * Tests the {@link Fraction#ceil()} method.
     */
    @Test
    public void testCeil() {
        final int[] numerators   = { 0,  1,  2,  3,  9, 10, 11, 12};
        final int[] denominators = { 3,  3,  3,  3,  3,  3,  3,  3};
        final int[] positives    = { 0,  1,  1,  1,  3,  4,  4,  4};
        final int[] negatives    = {-0, -0, -0, -1, -3, -3, -3, -4};
        for (int i=0; i<numerators.length; i++) {
            for (int s=0; s<4; s++) {
                int numerator   = numerators  [i];
                int denominator = denominators[i];
                if ((s & 1) != 0) numerator   = -numerator;
                if ((s & 2) != 0) denominator = -denominator;
                final int[] expected = (numerator * denominator >= 0) ? positives : negatives;
                final String label = "ceil(" + numerator + '/' + denominator + ')';
                assertEquals(label, expected[i], new Fraction(numerator, denominator).ceil());
            }
        }
    }

    /**
     * Tests the {@link Fraction#round()} method.
     */
    @Test
    public void testRoundFraction() {
        final int[] numerators   = { 0,  1,  2,  3,  9, 10, 11, 12, 12, 13, 14, 15, 16, 17, 18, 19};
        final int[] denominators = { 3,  3,  3,  3,  3,  3,  3,  3,  4,  4,  4,  4,  4,  4,  4,  4};
        final int[] results      = { 0,  0,  1,  1,  3,  3,  4,  4,  3,  3,  4,  4,  4,  4,  4,  5};
        for (int i=10; i<numerators.length; i++) {
            for (int s=0; s<4; s++) {
                int numerator   = numerators  [i];
                int denominator = denominators[i];
                int expected    = results     [i];
                if ((s & 1) != 0) numerator   = -numerator;
                if ((s & 2) != 0) denominator = -denominator;
                if (numerator * denominator < 0) expected = -expected;
                final String label = "even(" + numerator + '/' + denominator + ')';
                assertEquals(label, expected, new Fraction(numerator, denominator).round());
            }
        }
    }

    /**
     * Tests the {@link Fraction#simplify()} method.
     */
    @Test
    public void testSimplify() {
        Fraction fraction = new Fraction(4, 7).simplify();
        assertEquals(4, fraction.numerator);
        assertEquals(7, fraction.denominator);

        fraction = new Fraction(4, 8).simplify();
        assertEquals(1, fraction.numerator);
        assertEquals(2, fraction.denominator);

        fraction = new Fraction(48, 18).simplify();
        assertEquals(8, fraction.numerator);
        assertEquals(3, fraction.denominator);

        fraction = new Fraction(17*21, 31*21).simplify();
        assertEquals(17, fraction.numerator);
        assertEquals(31, fraction.denominator);
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        final Fraction local = new Fraction(5, 7);
        assertNotSame(local, assertSerializedEquals(local));
    }
}
