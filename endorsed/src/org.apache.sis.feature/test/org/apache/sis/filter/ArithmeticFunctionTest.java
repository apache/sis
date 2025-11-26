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
import org.opengis.filter.FilterFactory;


/**
 * Tests {@link ArithmeticFunction} implementations.
 *
 * @author  Johann Sorel (Geomatys)
 */
@SuppressWarnings("exports")
public final class ArithmeticFunctionTest extends TestCase {
    /**
     * The factory to use for creating the objects to test.
     */
    private final FilterFactory<Feature, ?, ?> factory;

    /**
     * Creates a new test case.
     */
    public ArithmeticFunctionTest() {
        factory = DefaultFilterFactory.forFeatures();
    }

    /**
     * Tests "Add" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testAdd() {
        var expression = factory.add(factory.literal(10.0), factory.literal(20.0));
        assertEquals(30.0, expression.apply(null));
        assertSerializedEquals(expression);
    }

    /**
     * Tests "Subtract" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testSubtract() {
        var expression = factory.subtract(factory.literal(10.0), factory.literal(20.0));
        assertEquals(-10.0, expression.apply(null));
        assertSerializedEquals(expression);
    }

    /**
     * Tests "Multiply" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testMultiply() {
        var expression = factory.multiply(factory.literal(10.0), factory.literal(20.0));
        assertEquals(200.0, expression.apply(null));
        assertSerializedEquals(expression);
    }

    /**
     * Tests "Divide" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testDivide() {
        var expression = factory.divide(factory.literal(10.0), factory.literal(20.0));
        assertEquals(0.5, expression.apply(null));
        assertSerializedEquals(expression);
    }
}
