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

import static org.apache.sis.test.Assert.*;
import org.apache.sis.test.TestCase;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;


/**
 * Tests {@link TemporalFunction} implementations.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class TemporalFunctionTest extends TestCase {
    /**
     * The factory to use for creating the objects to test.
     */
    private final FilterFactory2 factory = new DefaultFilterFactory();

    /**
     * Tests "After" (construction, evaluation, serialization, equality).
     */
    @Ignore
    @Test
    public void testAfter() {
    }

    /**
     * Tests "AnyInteracts" (construction, evaluation, serialization, equality).
     */
    @Ignore
    @Test
    public void testAnyInteracts() {
    }

    /**
     * Tests "Before" (construction, evaluation, serialization, equality).
     */
    @Ignore
    @Test
    public void testBefore() {
    }

    /**
     * Tests "Begins" (construction, evaluation, serialization, equality).
     */
    @Ignore
    @Test
    public void testBegins() {
    }

    /**
     * Tests "BegunBy" (construction, evaluation, serialization, equality).
     */
    @Ignore
    @Test
    public void testBegunBy() {
    }

    /**
     * Tests "During" (construction, evaluation, serialization, equality).
     */
    @Ignore
    @Test
    public void testDuring() {
    }

    /**
     * Tests "EndedBy" (construction, evaluation, serialization, equality).
     */
    @Ignore
    @Test
    public void testEndedBy() {
    }

    /**
     * Tests "Ends" (construction, evaluation, serialization, equality).
     */
    @Ignore
    @Test
    public void testEnds() {
    }

    /**
     * Tests "Meets" (construction, evaluation, serialization, equality).
     */
    @Ignore
    @Test
    public void testMeets() {
    }

    /**
     * Tests "MetBy" (construction, evaluation, serialization, equality).
     */
    @Ignore
    @Test
    public void testMetBy() {
    }

    /**
     * Tests "OverlappedBy" (construction, evaluation, serialization, equality).
     */
    @Ignore
    @Test
    public void testOverlappedBy() {
    }

    /**
     * Tests "TContains" (construction, evaluation, serialization, equality).
     */
    @Ignore
    @Test
    public void testTContains() {
    }

    /**
     * Tests "TEquals" (construction, evaluation, serialization, equality).
     */
    @Ignore
    @Test
    public void testTEquals() {
    }

    /**
     * Tests "TOverlaps" (construction, evaluation, serialization, equality).
     */
    @Ignore
    @Test
    public void testTOverlaps() {
    }

    private static void assertFilter(boolean expected, Filter op) {
        assertEquals(expected, op.evaluate(null));
        assertSerializedEquals(op);
    }
}
