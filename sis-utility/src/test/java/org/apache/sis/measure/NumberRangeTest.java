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
package org.apache.sis.measure;

import org.junit.Test;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;

import static org.junit.Assert.*;


/**
 * Tests the {@link NumberRange} class.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
@DependsOn(RangeTest.class)
public final strictfp class NumberRangeTest extends TestCase {
    /**
     * Tests the bounds values of a range of integers.
     */
    @Test
    public void testIntegerBounds() {
        final NumberRange<Integer> range = NumberRange.create(10, 20);
        assertEquals(10, range.getMinDouble(     ), 0);
        assertEquals(10, range.getMinDouble(true ), 0);
        assertEquals( 9, range.getMinDouble(false), 0);
        assertEquals(20, range.getMaxDouble(     ), 0);
        assertEquals(20, range.getMaxDouble(true ), 0);
        assertEquals(21, range.getMaxDouble(false), 0);
    }

    /**
     * Tests union and intersection with {@link Integer} values.
     */
    @Test
    public void testWithIntegers() {
        NumberRange<Integer> r1 = NumberRange.create(10, 20);
        NumberRange<Integer> r2 = NumberRange.create(15, 30);
        assertTrue (r1.equals(r1));
        assertTrue (r2.equals(r2));
        assertFalse(r1.equals(r2));
        assertEquals(Integer.class, r1.getElementType());
        assertEquals(Integer.class, r2.getElementType());
        assertEquals(NumberRange.create(10, 30), r1.union(r2));
        assertEquals(NumberRange.create(15, 20), r1.intersect(r2));
    }

    /**
     * Tests union and intersection with {@link Double} values.
     */
    @Test
    public void testWithDoubles() {
        NumberRange<Double> r1 = NumberRange.create(10.0, 20.0);
        NumberRange<Double> r2 = NumberRange.create(15.0, 30.0);
        assertEquals(Double.class, r1.getElementType());
        assertEquals(Double.class, r2.getElementType());
        assertEquals(NumberRange.create(10.0, 30.0), r1.union(r2));
        assertEquals(NumberRange.create(15.0, 20.0), r1.intersect(r2));
    }

    /**
     * Tests union and intersection involving a cast from integer to double values.
     */
    @Test
    public void testIntegerWithDoubleArguments() {
        NumberRange<Integer> r1 = NumberRange.create(10, 20);
        NumberRange<Double>  r2 = NumberRange.create(15.0, 30.0);
        assertEquals(Integer.class, r1.getElementType());
        assertEquals(Double .class, r2.getElementType());
        assertEquals(NumberRange.create(10.0, 30.0), r1.unionAny(r2));
        assertEquals(NumberRange.create(15, 20), r1.intersectAny(r2));

        r2 = NumberRange.create(15.5, 30.0);
        assertEquals(NumberRange.create(15.5f, 20.0f), r1.intersectAny(r2));
    }

    /**
     * Tests union and intersection involving a cast from integer to double values.
     */
    @Test
    public void testDoubleWithIntegerArguments() {
        NumberRange<Double>  r1 = NumberRange.create(10.0, 20.0);
        NumberRange<Integer> r2 = NumberRange.create(15, 30);
        assertEquals(Double .class, r1.getElementType());
        assertEquals(Integer.class, r2.getElementType());
        assertEquals(NumberRange.create(10.0, 30.0), r1.unionAny(r2));
        assertEquals(NumberRange.create(15, 20), r1.intersectAny(r2));

        r1 = NumberRange.create(10.0, 20.5);
        assertEquals(NumberRange.create(15.0f, 20.5f), r1.intersectAny(r2));
    }

    /**
     * Tests the {@link NumberRange#createBestFit(Number, boolean, Number, boolean)} method.
     */
    @Test
    public void testCreateBestFit() {
        assertEquals(NumberRange.create((short) 2, (short) 200),
                NumberRange.createBestFit(2, true, 200.0, true));
    }
}
