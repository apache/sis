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

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSerializedEquals;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.filter.Literal;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.ComparisonOperator;
import org.opengis.filter.ComparisonOperatorName;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.BetweenComparisonOperator;
import org.apache.sis.filter.visitor.FunctionNames;


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
    private final Literal<Feature,Integer> c05, c10, c20;

    /**
     * Expected name of the filter to be evaluated. The {@code evaluate(…)} methods
     * will compare {@link ComparisonFilter#getFunctionName()} against this value.
     */
    private ComparisonOperatorName expectedName;

    /**
     * The filter tested by last call to {@code evaluate(…)} methods.
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
     * Evaluates the given filter. The {@link #expectedName} field must be set before this method is invoked.
     * This method assumes that the first expression of all filters is {@link #c10}.
     */
    private boolean evaluate(final BinaryComparisonOperator<Feature> filter) {
        this.filter = filter;
        assertInstanceOf(ComparisonFilter.class, filter);
        assertEquals(expectedName, filter.getOperatorType());
        assertSame(c10, filter.getExpressions().get(0));
        return filter.test(null);
    }

    /**
     * Evaluates the given "Property is between" filter.
     */
    private boolean evaluate(final BetweenComparisonOperator<Feature> filter) {
        this.filter = filter;
        assertInstanceOf(ComparisonFilter.Between.class, filter);
        assertEquals(expectedName, filter.getOperatorType());
        return filter.test(null);
    }

    /**
     * Tests "LessThan" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testLess() {
        expectedName = ComparisonOperatorName.PROPERTY_IS_LESS_THAN;
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
        expectedName = ComparisonOperatorName.PROPERTY_IS_LESS_THAN_OR_EQUAL_TO;
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
        expectedName = ComparisonOperatorName.PROPERTY_IS_GREATER_THAN;
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
        expectedName = ComparisonOperatorName.PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO;
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
        expectedName = ComparisonOperatorName.PROPERTY_IS_EQUAL_TO;
        assertFalse(evaluate(factory.equal(c10, c20)));
        assertTrue (evaluate(factory.equal(c10, c10)));
        assertFalse(evaluate(factory.equal(c10, c05)));
        assertSerializedEquals(filter);
    }

    /**
     * Tests "NotEqualTo" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testNotEqual() {
        expectedName = ComparisonOperatorName.PROPERTY_IS_NOT_EQUAL_TO;
        assertTrue (evaluate(factory.notEqual(c10, c20)));
        assertFalse(evaluate(factory.notEqual(c10, c10)));
        assertTrue (evaluate(factory.notEqual(c10, c05)));
        assertSerializedEquals(filter);
    }

    /**
     * Tests "Between" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testBetween() {
        expectedName = ComparisonOperatorName.valueOf(FunctionNames.PROPERTY_IS_BETWEEN);
        assertTrue (evaluate(factory.between(c10, c05, c20)));
        assertFalse(evaluate(factory.between(c20, c05, c10)));
        assertFalse(evaluate(factory.between(c05, c10, c20)));
        assertSerializedEquals(filter);
    }
}
