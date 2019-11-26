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
import org.apache.sis.internal.filter.Node;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsBetween;


/**
 * Tests {@link ComparisonFunction.Between} implementations.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
public final strictfp class BetweenFunctionTest extends TestCase {
    /**
     * The factory to use for creating the objects to test.
     */
    private final FilterFactory factory;

    /**
     * Expressions used as constant for the tests.
     */
    private final Literal c05, c10, c20;

    /**
     * Expected name of the filter to be evaluated. The {@link #evaluate(Filter)} method
     * will compare {@link Node#getName()} against this value.
     */
    private String expectedName;

    /**
     * The filter tested by last call to {@link #evaluate(Filter)}.
     */
    private Filter filter;

    /**
     * Creates a new test case.
     */
    public BetweenFunctionTest() {
        factory = new DefaultFilterFactory();
        c05 = factory.literal(5);
        c10 = factory.literal(10);
        c20 = factory.literal(20);
    }

    /**
     * Evaluates the given filter. The {@link #expectedName} field must be set before this method is invoked.
     */
    private boolean evaluate(final Filter filter) {
        this.filter = filter;
        assertInstanceOf("Expected SIS implementation.", PropertyIsBetween.class, filter);
        assertEquals("name", expectedName, ((Node) filter).getName());
        return filter.evaluate(null);
    }

    /**
     * Tests "Between" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testBetween() {
        expectedName = "Between";
        assertTrue (evaluate(factory.between(c10, c05, c20)));
        assertFalse(evaluate(factory.between(c20, c05, c10)));
        assertFalse(evaluate(factory.between(c05, c10, c20)));
        assertSerializedEquals(filter);
    }
}
