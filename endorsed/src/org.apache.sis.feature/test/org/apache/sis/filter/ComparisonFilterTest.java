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

import org.apache.sis.filter.visitor.FunctionNames;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSerializedEquals;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import org.opengis.filter.Literal;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.ComparisonOperator;
import org.opengis.filter.ComparisonOperatorName;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.BetweenComparisonOperator;


/**
 * Tests {@link ComparisonFilter} implementations.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class ComparisonFilterTest extends TestCase {
    /**
     * The factory to use for creating the objects to test.
     */
    private final FilterFactory<Feature, ?, ?> factory;

    /**
     * Expressions used as constant for the tests.
     */
    private final Literal<Feature, Integer> c05, c10, c20;

    /**
     * Expected name of the filter to be evaluated. The {@code assertTestEqual(…)} methods
     * will compare {@link ComparisonFilter#getFunctionName()} against this value.
     */
    private ComparisonOperatorName expectedName;

    /**
     * The filter tested by last call to {@code assertTestEqual(…)} methods.
     */
    private ComparisonOperator<Feature> filter;

    /**
     * Creates a new test case.
     */
    public ComparisonFilterTest() {
        factory = DefaultFilterFactory.forFeatures();
        c05 = factory.literal(5);
        c10 = factory.literal(10);
        c20 = factory.literal(20);
    }

    /**
     * Performs various assertions on the given filter, including serialization.
     * This method assumes that the first expression of all filters is {@link #c10}.
     *
     * @param expected  the expected result of evaluating the given filter.
     * @param filter    the filter to test.
     */
    private void verify(final boolean expected, final BinaryComparisonOperator<Feature> filter) {
        assertTestEquals(expected, filter);
        assertSame(c10, filter.getExpressions().get(0));
        assertSerializedEquals(filter);
    }

    /**
     * Evaluates the given filter and asserts that the returned value is equal to the expected value.
     * The {@link #expectedName} field must be set before this method is invoked.
     */
    private void assertTestEquals(final boolean expected, final BinaryComparisonOperator<Feature> filter) {
        this.filter = filter;
        assertInstanceOf(ComparisonFilter.class, filter);
        assertEquals(expectedName, filter.getOperatorType());
        assertEquals(expected, filter.test(null));

        // Optimization of an expression with literals should result in a literal.
        assertSame(expected ? Filter.include() : Filter.exclude(), new Optimization().apply(filter));
    }

    /**
     * Evaluates the given "Property is between" filter and asserts that the returned value
     * is equal to the expected value.
     */
    private void assertTestEquals(final boolean expected, final BetweenComparisonOperator<Feature> filter) {
        this.filter = filter;
        assertInstanceOf(ComparisonFilter.Between.class, filter);
        assertEquals(expectedName, filter.getOperatorType());
        assertEquals(expected, filter.test(null));

        // Optimization of an expression with literals should result in a literal.
        assertSame(expected ? Filter.include() : Filter.exclude(), new Optimization().apply(filter));
    }

    /**
     * Tests "LessThan" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testLess() {
        expectedName = ComparisonOperatorName.PROPERTY_IS_LESS_THAN;
        verify(true,  factory.less(c10, c20));
        verify(false, factory.less(c10, c10));
        verify(false, factory.less(c10, c05));
    }

    /**
     * Tests "LessThanOrEqualTo" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testLessOrEqual() {
        expectedName = ComparisonOperatorName.PROPERTY_IS_LESS_THAN_OR_EQUAL_TO;
        verify(true,  factory.lessOrEqual(c10, c20));
        verify(true,  factory.lessOrEqual(c10, c10));
        verify(false, factory.lessOrEqual(c10, c05));
    }

    /**
     * Tests "GreaterThan" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testGreater() {
        expectedName = ComparisonOperatorName.PROPERTY_IS_GREATER_THAN;
        verify(false, factory.greater(c10, c20));
        verify(false, factory.greater(c10, c10));
        verify(true,  factory.greater(c10, c05));
    }

    /**
     * Tests "GreaterThanOrEqualTo" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testGreaterOrEqual() {
        expectedName = ComparisonOperatorName.PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO;
        verify(false, factory.greaterOrEqual(c10, c20));
        verify(true,  factory.greaterOrEqual(c10, c10));
        verify(true,  factory.greaterOrEqual(c10, c05));
    }

    /**
     * Tests "EqualTo" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testEqual() {
        expectedName = ComparisonOperatorName.PROPERTY_IS_EQUAL_TO;
        verify(false, factory.equal(c10, c20));
        verify(true,  factory.equal(c10, c10));
        verify(false, factory.equal(c10, c05));
    }

    /**
     * Tests "NotEqualTo" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testNotEqual() {
        expectedName = ComparisonOperatorName.PROPERTY_IS_NOT_EQUAL_TO;
        verify(true,  factory.notEqual(c10, c20));
        verify(false, factory.notEqual(c10, c10));
        verify(true,  factory.notEqual(c10, c05));
    }

    /**
     * Tests "Between" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testBetween() {
        expectedName = ComparisonOperatorName.valueOf(FunctionNames.PROPERTY_IS_BETWEEN);
        assertTestEquals(true,  factory.between(c10, c05, c20));
        assertTestEquals(false, factory.between(c20, c05, c10));
        assertTestEquals(false, factory.between(c05, c10, c20));
        assertSerializedEquals(filter);
    }

    /**
     * Tests "Equal" between Boolean values.
     */
    @Test
    public void testBooleans() {
        expectedName = ComparisonOperatorName.PROPERTY_IS_EQUAL_TO;
        assertTestEquals(true, factory.equal(factory.function("isNaN", c10), factory.literal(Boolean.FALSE)));
        assertSerializedEquals(filter);
    }
}
