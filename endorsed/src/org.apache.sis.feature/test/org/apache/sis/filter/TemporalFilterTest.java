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

import java.time.Instant;
import org.apache.sis.geometry.WraparoundMethod;
import static org.apache.sis.util.internal.shared.Constants.MILLISECONDS_PER_DAY;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSerializedEquals;

// Specific to the main branch:
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.pending.geoapi.temporal.Period;
import org.apache.sis.pending.geoapi.temporal.TemporalOperatorName;


/**
 * Tests {@link TemporalFilter} implementations.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TemporalFilterTest extends TestCase {
    /**
     * The factory to use for creating the objects to test.
     */
    private DefaultFilterFactory<AbstractFeature, Object, ? super Period> factory;

    /**
     * The filter to test. This field shall be assigned by each {@code testFoo()} method by invoking
     * a {@link #factory} method with {@link #expression1} and {@link #expression2} in arguments.
     */
    private Filter<AbstractFeature> filter;

    /**
     * The expression to test. They are the arguments to be given to {@link #factory} method.
     * Each expression will return a period made of {@link PeriodLiteral#begin} and {@link PeriodLiteral#end}.
     */
    private final PeriodLiteral expression1, expression2;

    /**
     * Creates a new test case.
     */
    public TemporalFilterTest() {
        factory = DefaultFilterFactory.forFeatures();
        expression1 = new PeriodLiteral();
        expression2 = new PeriodLiteral();
        expression1.begin = expression2.begin = Instant.parse("2000-01-01T09:00:00Z").toEpochMilli();
        expression1.end   = expression2.end   = Instant.parse("2000-01-05T10:00:00Z").toEpochMilli();
    }

    /**
     * Performs some validation on newly created filter.
     * The {@link #filter} field must be initialized before this method is invoked.
     *
     * @param  name  expected filter name.
     */
    private void validate(final TemporalOperatorName name) {
        assertInstanceOf(TemporalFilter.class, filter);
        assertEquals(name, filter.getOperatorType());
        final var operands = filter.getExpressions();
        assertEquals(2, operands.size());
        assertSame(expression1, operands.get(0));
        assertSame(expression2, operands.get(1));
        assertSerializedEquals(filter);
    }

    /**
     * Evaluates the filter with a null resource.
     * This is okay if all filter operands are literal.
     */
    private boolean evaluate() {
        return filter.test(null);
    }

    /**
     * Tests "TEquals" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testEquals() {
        filter = factory.tequals(expression1, expression2);
        validate(TemporalOperatorName.EQUALS);
        assertTrue(evaluate());

        // Break the "self.end = other.end" condition.
        expression1.end++;
        assertFalse(evaluate());
    }

    /**
     * Tests "Before" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testBefore() {
        filter = factory.before(expression1, expression2);
        validate(TemporalOperatorName.BEFORE);
        assertFalse(evaluate());

        // Move before expression 2.
        expression1.begin -= 10 * MILLISECONDS_PER_DAY;
        expression1.end   -= 10 * MILLISECONDS_PER_DAY;
        assertTrue(evaluate());

        // Break the "self.end < other.begin" condition.
        expression1.end = expression2.begin;
        assertFalse(evaluate());
    }

    /**
     * Tests "After" (construction, evaluation, serialization, equality).
     *
     * @see #testOnPeriods()
     */
    @Test
    public void testAfter() {
        filter = factory.after(expression1, expression2);
        validate(TemporalOperatorName.AFTER);
        assertFalse(evaluate());

        // Move after expression 2.
        expression1.begin += 10 * MILLISECONDS_PER_DAY;
        expression1.end   += 10 * MILLISECONDS_PER_DAY;
        assertTrue(evaluate());

        // Break the "self.begin > other.end" condition.
        expression1.begin = expression2.end;
        assertFalse(evaluate());
    }

    /**
     * Tests "Begins" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testBegins() {
        filter = factory.begins(expression1, expression2);
        validate(TemporalOperatorName.BEGINS);
        assertFalse(evaluate());

        // End before ending of expression 2.
        expression1.end--;
        assertTrue(evaluate());

        // Break the "self.begin = other.begin" condition.
        expression1.begin++;
        assertFalse(evaluate());
    }

    /**
     * Tests "Ends" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testEnds() {
        filter = factory.ends(expression1, expression2);
        validate(TemporalOperatorName.ENDS);
        assertFalse(evaluate());

        // Begin after beginning of expression 2.
        expression1.begin++;
        assertTrue(evaluate());

        // Break the "self.end = other.end" condition.
        expression1.end--;
        assertFalse(evaluate());
    }

    /**
     * Tests "BegunBy" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testBegunBy() {
        filter = factory.begunBy(expression1, expression2);
        validate(TemporalOperatorName.BEGUN_BY);
        assertFalse(evaluate());

        // End after ending of expression 2.
        expression1.end++;
        assertTrue(evaluate());

        // Break the "self.begin = other.begin" condition.
        expression1.begin--;
        assertFalse(evaluate());
    }

    /**
     * Tests "EndedBy" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testEndedBy() {
        filter = factory.endedBy(expression1, expression2);
        validate(TemporalOperatorName.ENDED_BY);
        assertFalse(evaluate());

        // Begin before beginning of expression 2.
        expression1.begin--;
        assertTrue(evaluate());

        // Break the "self.end = other.end" condition.
        expression1.end++;
        assertFalse(evaluate());
    }

    /**
     * Tests "Meets" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testMeets() {
        filter = factory.meets(expression1, expression2);
        validate(TemporalOperatorName.MEETS);
        assertFalse(evaluate());

        // Move before expression 2.
        expression1.begin -= 10 * MILLISECONDS_PER_DAY;
        expression1.end   -= 10 * MILLISECONDS_PER_DAY;
        assertFalse(evaluate());

        // Met the "self.end = other.begin" condition.
        expression1.end = expression2.begin;
        assertTrue(evaluate());
    }

    /**
     * Tests "MetBy" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testMetBy() {
        filter = factory.metBy(expression1, expression2);
        validate(TemporalOperatorName.MET_BY);
        assertFalse(evaluate());

        // Move after expression 2.
        expression1.begin += 10 * MILLISECONDS_PER_DAY;
        expression1.end   += 10 * MILLISECONDS_PER_DAY;
        assertFalse(evaluate());

        // Met the "self.begin = other.end" condition.
        expression1.begin = expression2.end;
        assertTrue(evaluate());
    }

    /**
     * Tests "During" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testDuring() {
        filter = factory.during(expression1, expression2);
        validate(TemporalOperatorName.DURING);
        assertFalse(evaluate());

        // Shrink inside expression 2.
        expression1.begin++;
        expression1.end--;
        assertTrue(evaluate());

        // Break the "self.end < other.end" condition.
        expression1.end += 2;
        assertFalse(evaluate());
    }

    /**
     * Tests "TContains" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testContains() {
        filter = factory.tcontains(expression1, expression2);
        validate(TemporalOperatorName.CONTAINS);
        assertFalse(evaluate());

        // Expand to encompass expression 2.
        expression1.begin--;
        expression1.end++;
        assertTrue(evaluate());

        // Break the "self.end > other.end" condition.
        expression1.end -= 2;
        assertFalse(evaluate());
    }

    /**
     * Tests "TOverlaps" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testOverlaps() {
        filter = factory.toverlaps(expression1, expression2);
        validate(TemporalOperatorName.OVERLAPS);
        assertFalse(evaluate());

        // Translate to overlap left part of expression 2.
        expression1.begin--;
        expression1.end--;
        assertTrue(evaluate());

        // Break the "self.end < other.end" condition.
        expression1.end += 2;
        assertFalse(evaluate());
    }

    /**
     * Tests "OverlappedBy" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testOverlappedBy() {
        filter = factory.overlappedBy(expression1, expression2);
        validate(TemporalOperatorName.OVERLAPPED_BY);
        assertFalse(evaluate());

        // Translate to overlap right part of expression 2.
        expression1.begin++;
        expression1.end++;
        assertTrue(evaluate());

        // Break the "self.end > other.end" condition.
        expression1.end -= 2;
        assertFalse(evaluate());
    }

    /**
     * Tests "AnyInteracts" (construction, evaluation, serialization, equality).
     */
    @Test
    public void testAnyInteracts() {
        filter = factory.anyInteracts(expression1, expression2);
        validate(TemporalOperatorName.ANY_INTERACTS);
        assertTrue(evaluate());
        expression1.begin++;
        assertTrue(evaluate());
        expression1.begin -= 2;
        assertTrue(evaluate());
        expression1.end += 2;
        assertTrue(evaluate());
    }

    /**
     * Re-test an arbitrary operation, but using a different factory.
     * This test uses a factory specialized for the {@link Period} type
     * in order to test a different code path.
     */
    @Test
    public void testOnPeriods() {
        factory = new DefaultFilterFactory.Features<>(Object.class, Period.class, WraparoundMethod.NONE);
        testAfter();
    }
}
