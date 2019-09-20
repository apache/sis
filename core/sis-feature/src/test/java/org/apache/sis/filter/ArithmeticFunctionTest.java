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
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link ArithmeticFunction} implementations.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class ArithmeticFunctionTest extends TestCase {
    /**
     * The factory to use for creating the objects to test.
     */
    private final FilterFactory2 factory = new DefaultFilterFactory();

    /**
     * Tests "Add" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testAdd() {
        Expression op = factory.add(factory.literal(10.0), factory.literal(20.0));
        assertEquals(30.0, op.evaluate(null));
        assertSerializedEquals(op);
    }

    /**
     * Tests "Subtract" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testSubtract() {
        Expression op = factory.subtract(factory.literal(10.0), factory.literal(20.0));
        assertEquals(-10.0, op.evaluate(null));
        assertSerializedEquals(op);
    }

    /**
     * Tests "Multiply" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testMultiply() {
        Expression op = factory.multiply(factory.literal(10.0), factory.literal(20.0));
        assertEquals(200.0, op.evaluate(null));
        assertSerializedEquals(op);
    }

    /**
     * Tests "Divide" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testDivide() {
        Expression op = factory.divide(factory.literal(10.0), factory.literal(20.0));
        assertEquals(0.5, op.evaluate(null));
        assertSerializedEquals(op);
    }
}
