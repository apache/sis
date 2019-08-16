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
package org.apache.sis.filter;

import static org.apache.sis.test.Assert.*;
import org.apache.sis.test.TestCase;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;


/**
 * Tests {@link ComparisonFunction} implementations.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class ComparisonFunctionTest extends TestCase {
    /**
     * The factory to use for creating the objects to test.
     */
    private final FilterFactory2 factory = new DefaultFilterFactory();

    /**
     * Tests "LessThan" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testLess() {
        assertFilter(true,  factory.less(factory.literal(10), factory.literal(20)));
        assertFilter(false, factory.less(factory.literal(10), factory.literal(10)));
        assertFilter(false, factory.less(factory.literal(10), factory.literal(5)));
    }

    /**
     * Tests "LessThanOrEqualTo" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testLessOrEqual() {
        assertFilter(true,  factory.lessOrEqual(factory.literal(10), factory.literal(20)));
        assertFilter(true,  factory.lessOrEqual(factory.literal(10), factory.literal(10)));
        assertFilter(false, factory.lessOrEqual(factory.literal(10), factory.literal(5)));
    }

    /**
     * Tests "GreaterThan" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testGreater() {
        assertFilter(false, factory.greater(factory.literal(10), factory.literal(20)));
        assertFilter(false, factory.greater(factory.literal(10), factory.literal(10)));
        assertFilter(true,  factory.greater(factory.literal(10), factory.literal(5)));
    }

    /**
     * Tests "GreaterThanOrEqualTo" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testGreaterOrEqual() {
        assertFilter(false, factory.greaterOrEqual(factory.literal(10), factory.literal(20)));
        assertFilter(true,  factory.greaterOrEqual(factory.literal(10), factory.literal(10)));
        assertFilter(true,  factory.greaterOrEqual(factory.literal(10), factory.literal(5)));
    }

    /**
     * Tests "EqualTo" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testEqual() {
        assertFilter(false, factory.equals(factory.literal(10), factory.literal(20)));
        assertFilter(true,  factory.equals(factory.literal(10), factory.literal(10)));
        assertFilter(false, factory.equals(factory.literal(10), factory.literal(5)));
    }

    /**
     * Tests "NotEqualTo" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testNotEqual() {
        assertFilter(true,  factory.notEqual(factory.literal(10), factory.literal(20)));
        assertFilter(false, factory.notEqual(factory.literal(10), factory.literal(10)));
        assertFilter(true,  factory.notEqual(factory.literal(10), factory.literal(5)));
    }

    private static void assertFilter(boolean expected, Filter op) {
        assertEquals(expected, op.evaluate(null));
        assertSerializedEquals(op);
    }
}
