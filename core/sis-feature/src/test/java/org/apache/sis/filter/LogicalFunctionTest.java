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
import java.util.Arrays;
import java.util.Collections;
import org.apache.sis.test.TestCase;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Literal;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link LogicalFunction} implementations.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class LogicalFunctionTest extends TestCase {
    /**
     * The factory to use for creating the objects to test.
     */
    private final FilterFactory2 factory = new DefaultFilterFactory();

    /**
     * Tests creation of "And" expression from the factory.
     */
    @Test
    public void testAndConstructor() {
        final Filter filter = factory.isNull(factory.literal("text"));
        assertNotNull(factory.and(filter, filter));
        assertNotNull(factory.and(Arrays.asList(filter, filter, filter)));
        try {
            factory.and(null, null);
            fail("Creation of an AND with a null child filter must raise an exception");
        } catch (NullPointerException ex) {}
        try {
            factory.and(filter, null);
            fail("Creation of an AND with a null child filter must raise an exception");
        } catch (NullPointerException ex) {}
        try {
            factory.and(null, filter);
            fail("Creation of an AND with a null child filter must raise an exception");
        } catch (NullPointerException ex) {}
        try {
            factory.and(Arrays.asList(filter));
            fail("Creation of an AND with less then two children filters must raise an exception");
        } catch (IllegalArgumentException ex) {}
    }

    /**
     * Tests creation of "Or" expression from the factory.
     */
    @Test
    public void testOrConstructor() {
        final Filter filter = factory.isNull(factory.literal("text"));
        assertNotNull(factory.or(filter, filter));
        assertNotNull(factory.or(Arrays.asList(filter, filter, filter)));
        try {
            factory.or(null, null);
            fail("Creation of an OR with a null child filter must raise an exception");
        } catch (NullPointerException ex) {}
        try {
            factory.or(filter, null);
            fail("Creation of an OR with a null child filter must raise an exception");
        } catch (NullPointerException ex) {}
        try {
            factory.or(null, filter);
            fail("Creation of an OR with a null child filter must raise an exception");
        } catch (NullPointerException ex) {}
        try {
            factory.or(Arrays.asList(filter));
            fail("Creation of an OR with less then two children filters must raise an exception");
        } catch (IllegalArgumentException ex) {}
    }

    /**
     * Tests evaluation of "And" expression.
     */
    @Test
    public void testAndEvaluate() {
        final Filter filterTrue  = factory.isNull(factory.property("attNull"));
        final Filter filterFalse = factory.isNull(factory.property("attNotNull"));
        final Map<String,String> feature = Collections.singletonMap("attNotNull", "text");

        assertTrue (factory.and(filterTrue,  filterTrue ).evaluate(feature));
        assertFalse(factory.and(filterFalse, filterTrue ).evaluate(feature));
        assertFalse(factory.and(filterTrue,  filterFalse).evaluate(feature));
        assertFalse(factory.and(filterFalse, filterFalse).evaluate(feature));
    }

    /**
     * Tests evaluation of "Or" expression.
     */
    @Test
    public void testOrEvaluate() {
        final Filter filterTrue  = factory.isNull(factory.property("attNull"));
        final Filter filterFalse = factory.isNull(factory.property("attNotNull"));
        final Map<String,String> feature = Collections.singletonMap("attNotNull", "text");

        assertTrue (factory.or(filterTrue,  filterTrue ).evaluate(feature));
        assertTrue (factory.or(filterFalse, filterTrue ).evaluate(feature));
        assertTrue (factory.or(filterTrue,  filterFalse).evaluate(feature));
        assertFalse(factory.or(filterFalse, filterFalse).evaluate(feature));
    }

    /**
     * Tests serialization of "And" expression.
     */
    @Test
    public void testAndSerialize() {
        final Literal literal = factory.literal("text");
        final Filter  filter  = factory.isNull(literal);
        assertSerializedEquals(factory.and(filter, filter));
    }

    /**
     * Tests serialization of "Or" expression.
     */
    @Test
    public void testOrSerialize() {
        final Literal literal = factory.literal("text");
        final Filter  filter  = factory.isNull(literal);
        assertSerializedEquals(factory.or(filter, filter));
    }
}
