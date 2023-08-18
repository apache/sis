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

import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.Assertions.assertSerializedEquals;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.FilterFactory;


/**
 * Tests {@link ArithmeticFunction} implementations.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 */
public final class ArithmeticFunctionTest extends TestCase {
    /**
     * The factory to use for creating the objects to test.
     */
    private final FilterFactory<Feature,Object,?> factory;

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
        Expression<Feature,?> op = factory.add(factory.literal(10.0), factory.literal(20.0));
        assertEquals(30.0, op.apply(null));
        assertSerializedEquals(op);
    }

    /**
     * Tests "Subtract" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testSubtract() {
        Expression<Feature,?> op = factory.subtract(factory.literal(10.0), factory.literal(20.0));
        assertEquals(-10.0, op.apply(null));
        assertSerializedEquals(op);
    }

    /**
     * Tests "Multiply" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testMultiply() {
        Expression<Feature,?> op = factory.multiply(factory.literal(10.0), factory.literal(20.0));
        assertEquals(200.0, op.apply(null));
        assertSerializedEquals(op);
    }

    /**
     * Tests "Divide" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testDivide() {
        Expression<Feature,?> op = factory.divide(factory.literal(10.0), factory.literal(20.0));
        assertEquals(0.5, op.apply(null));
        assertSerializedEquals(op);
    }
}
