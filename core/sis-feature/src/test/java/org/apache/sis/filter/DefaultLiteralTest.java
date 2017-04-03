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

import java.util.Date;
import org.apache.sis.test.TestCase;
import org.junit.Test;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Literal;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link DefaultLiteral}.
 *
 * @author Johann Sorel (Geomatys)
 * @version 0.7
 * @since   0.7
 * @module
 */
public class DefaultLiteralTest extends TestCase {
    /**
     * Test factory.
     */
    @Test
    public void testConstructor() {
        final FilterFactory2 factory = new DefaultFilterFactory();
        assertNotNull(factory.literal(true));
        assertNotNull(factory.literal("a text string"));
        assertNotNull(factory.literal('x'));
        assertNotNull(factory.literal(122));
        assertNotNull(factory.literal(45.56d));
    }

    /**
     * Tests value and evaluation.
     */
    @Test
    public void testEvaluate() {
        final Literal literal = new DefaultLiteral<>(12.45);
        assertEquals(12.45, (Double)literal.getValue(), STRICT);
        assertEquals(12.45, (Double)literal.evaluate(null), STRICT);
        assertEquals(12.45, literal.evaluate(null, Double.class), STRICT);
        assertEquals("12.45", literal.evaluate(null, String.class));
        assertEquals(null, literal.evaluate(null, Date.class));
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialize() {
        assertSerializedEquals(new DefaultLiteral<>(true));
        assertSerializedEquals(new DefaultLiteral<>("a text string"));
        assertSerializedEquals(new DefaultLiteral<>('x'));
        assertSerializedEquals(new DefaultLiteral<>(122));
        assertSerializedEquals(new DefaultLiteral<>(45.56d));
    }
}
