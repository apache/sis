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
import org.opengis.filter.temporal.BinaryTemporalOperator;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link TemporalFunction} implementations.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class TemporalFunctionTest extends TestCase {
    /**
     * The factory to use for creating the objects to test.
     */
    private final FilterFactory factory;

    /**
     * The filter to test. This field shall be assigned by each {@code testFoo()} method by invoking
     * a {@link #factory} method with {@link #expression1} and {@link #expression2} in arguments.
     */
    private BinaryTemporalOperator filter;

    /**
     * The expression to test. They are the arguments to be given to {@link #factory} method.
     * Each expression will return a period made of {@link PeriodLiteral#begin} and {@link PeriodLiteral#end}.
     * Date pattern is {@code "yyyy-MM-dd HH:mm:ss"} in UTC timezone.
     */
    private final PeriodLiteral expression1, expression2;

    /**
     * Creates a new test case.
     */
    public TemporalFunctionTest() {
        factory = new DefaultFilterFactory();
        expression1 = new PeriodLiteral();
        expression2 = new PeriodLiteral();
        expression1.begin = expression2.begin = "2010-04-20 10:00:00";
        expression1.end   = expression2.end   = "2010-04-25 15:00:00";
        expression1.begin = expression2.begin = "2010-04-20 10:00:00";
        expression1.end   = expression2.end   = "2010-04-25 15:00:00";
    }

    /**
     * Performs some validation on newly created filter.
     *
     * @param  name  expected filter name.
     */
    private void validate(final String name) {
        assertInstanceOf("Expected SIS implementation.", TemporalFunction.class, filter);
        final TemporalFunction f = ((TemporalFunction) filter);
        assertEquals("name", name, f.getName());
        assertSame("expression1", expression1, f.expression1);
        assertSame("expression2", expression2, f.expression2);
        assertSerializedEquals(filter);
    }

    /**
     * Evaluates the filter.
     */
    private boolean evaluate() {
        return filter.evaluate(null);
    }

    /**
     * Tests "TEquals" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testEquals() {
        filter = factory.tequals(expression1, expression2);
        validate("TEquals");
        assertTrue(evaluate());
    }

    /**
     * Tests "Before" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testBefore() {
        filter = factory.before(expression1, expression2);
        validate("Before");
        assertFalse(evaluate());
    }

    /**
     * Tests "After" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testAfter() {
        filter = factory.after(expression1, expression2);
        validate("After");
        assertFalse(evaluate());
    }

    /**
     * Tests "Begins" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testBegins() {
        filter = factory.begins(expression1, expression2);
        validate("Begins");
        assertFalse(evaluate());
    }

    /**
     * Tests "Ends" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testEnds() {
        filter = factory.ends(expression1, expression2);
        validate("Ends");
        assertFalse(evaluate());
    }

    /**
     * Tests "BegunBy" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testBegunBy() {
        filter = factory.begunBy(expression1, expression2);
        validate("BegunBy");
        assertFalse(evaluate());
    }

    /**
     * Tests "EndedBy" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testEndedBy() {
        filter = factory.endedBy(expression1, expression2);
        validate("EndedBy");
        assertFalse(evaluate());
    }

    /**
     * Tests "Meets" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testMeets() {
        filter = factory.meets(expression1, expression2);
        validate("Meets");
        assertFalse(evaluate());
    }

    /**
     * Tests "MetBy" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testMetBy() {
        filter = factory.metBy(expression1, expression2);
        validate("MetBy");
        assertFalse(evaluate());
    }

    /**
     * Tests "During" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testDuring() {
        filter = factory.during(expression1, expression2);
        validate("During");
        assertFalse(evaluate());
    }

    /**
     * Tests "TContains" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testContains() {
        filter = factory.tcontains(expression1, expression2);
        validate("TContains");
        assertFalse(evaluate());
    }

    /**
     * Tests "TOverlaps" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testOverlaps() {
        filter = factory.toverlaps(expression1, expression2);
        validate("TOverlaps");
        assertFalse(evaluate());
    }

    /**
     * Tests "OverlappedBy" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testOverlappedBy() {
        filter = factory.overlappedBy(expression1, expression2);
        validate("OverlappedBy");
        assertFalse(evaluate());
    }

    /**
     * Tests "AnyInteracts" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testAnyInteracts() {
        filter = factory.anyInteracts(expression1, expression2);
        validate("AnyInteracts");
        assertTrue(evaluate());
    }
}
