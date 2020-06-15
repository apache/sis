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
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;

/**
 * Tests {@code ComparisonFunction.Like} implementations.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 2.0
 * @since   2.0
 * @module
 */
public final strictfp class LikeFunctionTest extends TestCase {
    /**
     * The factory to use for creating the objects to test.
     */
    private final FilterFactory factory;

    /**
     * Creates a new test case.
     */
    public LikeFunctionTest() {
        factory = new DefaultFilterFactory();
    }


    /**
     * Tests "Like" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testLike() {

        assertTrue(factory.like(factory.literal("Apache SIS"), "Apache*", "*", ".", "\\", true).evaluate(null));
        assertFalse(factory.like(factory.literal("Apache SIS"), "Oracle*", "*", ".", "\\", true).evaluate(null));

        // a character is missing, should not match
        assertFalse(factory.like(factory.literal("Apache SIS"), "Apache*IS.*", "*", ".", "\\", true).evaluate(null));
        assertTrue(factory.like(factory.literal("Apache SIS"), "Apache*I.", "*", ".", "\\", true).evaluate(null));

        // test case insensitive
        assertTrue(factory.like(factory.literal("Apache SIS"), "apache sis", "*", ".", "\\", false).evaluate(null));

        // test escape
        assertTrue(factory.like(factory.literal("*Apache* SIS"), "!*Apache!* SIS", "*", ".", "!", true).evaluate(null));
    }
}
