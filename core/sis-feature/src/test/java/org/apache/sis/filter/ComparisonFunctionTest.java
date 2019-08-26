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

import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.BinaryComparisonOperator;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


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
    private final FilterFactory factory;

    /**
     * Expressions used as constant for the tests.
     */
    private final Literal c05, c10, c20;

    /**
     * Expected name of the filter to be evaluated. The {@link #evaluate(BinaryComparisonOperator)} method
     * will compare {@link ComparisonFunction#getName()} against this value.
     */
    private String expectedName;

    /**
     * The filter tested by last call to {@link #evaluate(BinaryComparisonOperator)}.
     */
    private BinaryComparisonOperator filter;

    /**
     * Creates a new test case.
     */
    public ComparisonFunctionTest() {
        factory = new DefaultFilterFactory();
        c05 = factory.literal(5);
        c10 = factory.literal(10);
        c20 = factory.literal(20);
    }

    /**
     * Evaluates the given filter. The {@link #expectedName} field must be set before this method is invoked.
     * This method assumes that the first expression of all filters is {@link #c10}.
     */
    private boolean evaluate(final BinaryComparisonOperator filter) {
        this.filter = filter;
        assertInstanceOf("Expected SIS implementation.", ComparisonFunction.class, filter);
        assertEquals("name", expectedName, ((ComparisonFunction) filter).getName());
        assertSame("expression1", c10, filter.getExpression1());
        return filter.evaluate(null);
    }

    /**
     * Tests "LessThan" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testLess() {
        expectedName = "LessThan";
        assertTrue (evaluate(factory.less(c10, c20)));
        assertFalse(evaluate(factory.less(c10, c10)));
        assertFalse(evaluate(factory.less(c10, c05)));
        assertSerializedEquals(filter);
    }

    /**
     * Tests "LessThanOrEqualTo" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testLessOrEqual() {
        expectedName = "LessThanOrEqualTo";
        assertTrue (evaluate(factory.lessOrEqual(c10, c20)));
        assertTrue (evaluate(factory.lessOrEqual(c10, c10)));
        assertFalse(evaluate(factory.lessOrEqual(c10, c05)));
        assertSerializedEquals(filter);
    }

    /**
     * Tests "GreaterThan" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testGreater() {
        expectedName = "GreaterThan";
        assertFalse(evaluate(factory.greater(c10, c20)));
        assertFalse(evaluate(factory.greater(c10, c10)));
        assertTrue (evaluate(factory.greater(c10, c05)));
        assertSerializedEquals(filter);
    }

    /**
     * Tests "GreaterThanOrEqualTo" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testGreaterOrEqual() {
        expectedName = "GreaterThanOrEqualTo";
        assertFalse(evaluate(factory.greaterOrEqual(c10, c20)));
        assertTrue (evaluate(factory.greaterOrEqual(c10, c10)));
        assertTrue (evaluate(factory.greaterOrEqual(c10, c05)));
        assertSerializedEquals(filter);
    }

    /**
     * Tests "EqualTo" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testEqual() {
        expectedName = "EqualTo";
        assertFalse(evaluate(factory.equals(c10, c20)));
        assertTrue (evaluate(factory.equals(c10, c10)));
        assertFalse(evaluate(factory.equals(c10, c05)));
        assertSerializedEquals(filter);
    }

    /**
     * Tests "NotEqualTo" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testNotEqual() {
        expectedName = "NotEqualTo";
        assertTrue (evaluate(factory.notEqual(c10, c20)));
        assertFalse(evaluate(factory.notEqual(c10, c10)));
        assertTrue (evaluate(factory.notEqual(c10, c05)));
        assertSerializedEquals(filter);
    }
}
