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
import java.util.Map;
import java.util.HashMap;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.apache.sis.util.iso.Names;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link LeafExpression}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class LeafExpressionTest extends TestCase {
    /**
     * The factory to use for creating the objects to test.
     */
    private final FilterFactory2 factory = new DefaultFilterFactory();

    /**
     * Test creation of "PropertyName".
     */
    @Test
    public void testPropertyConstructor() {
        assertNotNull(factory.property(Names.parseGenericName(null, null, "type")));
        assertNotNull(factory.property("type"));
    }

    /**
     * Test creation of "Literal".
     */
    @Test
    public void testLiteralConstructor() {
        assertNotNull(factory.literal(true));
        assertNotNull(factory.literal("a text string"));
        assertNotNull(factory.literal('x'));
        assertNotNull(factory.literal(122));
        assertNotNull(factory.literal(45.56d));
    }

    /**
     * Tests evaluation of "PropertyName".
     */
    @Test
    public void testPropertyEvaluate() {
        final Map<String,String> candidate = new HashMap<>();

        final PropertyName prop = factory.property("type");
        assertEquals("type", prop.getPropertyName());

        assertNull(prop.evaluate(candidate));
        assertNull(prop.evaluate(null));

        candidate.put("type", "road");
        assertEquals("road", prop.evaluate(candidate));
        assertEquals("road", prop.evaluate(candidate, String.class));

        candidate.put("type", "45.1");
        assertEquals("45.1", prop.evaluate(candidate));
        assertEquals("45.1", prop.evaluate(candidate, Object.class));
        assertEquals("45.1", prop.evaluate(candidate, String.class));
        assertEquals( 45.1,  prop.evaluate(candidate, Double.class), STRICT);
    }

    /**
     * Tests evaluation of "Literal".
     */
    @Test
    public void testLiteralEvaluate() {
        final Literal literal = factory.literal(12.45);
        assertEquals(12.45,   literal.getValue());
        assertEquals(12.45,   literal.evaluate(null));
        assertEquals(12.45,   literal.evaluate(null, Double.class), STRICT);
        assertEquals("12.45", literal.evaluate(null, String.class));
        assertNull  (         literal.evaluate(null, Date.class));
    }

    /**
     * Tests serialization of "PropertyName".
     */
    @Test
    public void testPropertySerialize() {
        assertSerializedEquals(factory.property("type"));
    }

    /**
     * Tests serialization of "Literal".
     */
    @Test
    public void testLiteralSerialize() {
        assertSerializedEquals(factory.literal(true));
        assertSerializedEquals(factory.literal("a text string"));
        assertSerializedEquals(factory.literal('x'));
        assertSerializedEquals(factory.literal(122));
        assertSerializedEquals(factory.literal(45.56d));
    }
}
