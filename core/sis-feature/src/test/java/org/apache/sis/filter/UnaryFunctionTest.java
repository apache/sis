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

import java.util.Map;
import java.util.Collections;
import org.apache.sis.test.TestCase;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Literal;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link UnaryFunction}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class UnaryFunctionTest extends TestCase {
    /**
     * The factory to use for creating the objects to test.
     */
    private final FilterFactory2 factory = new DefaultFilterFactory();

    /**
     * Test factory with the "Not" expression.
     */
    @Test
    public void testNotConstructor() {
        final Literal literal = factory.literal("text");
        final Filter  filter  = factory.isNull(literal);
        assertNotNull(factory.not(filter));
    }

    /**
     * Tests evaluation of "Not" expression.
     */
    @Test
    public void testNotEvaluate() {
        final Filter filterTrue  = factory.isNull(factory.property("attNull"));
        final Filter filterFalse = factory.isNull(factory.property("attNotNull"));
        final Map<String,String> feature = Collections.singletonMap("attNotNull", "text");

        assertFalse(factory.not(filterTrue ).evaluate(feature));
        assertTrue (factory.not(filterFalse).evaluate(feature));
    }

    /**
     * Tests serialization of "Not" expression.
     */
    @Test
    public void testNotSerialize() {
        final Literal literal = factory.literal("text");
        final Filter  filter  = factory.isNull(literal);
        assertSerializedEquals(factory.not(filter));
    }
}
