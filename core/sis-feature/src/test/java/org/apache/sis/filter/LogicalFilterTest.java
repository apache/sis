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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.function.BiFunction;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;

// Branch-dependent imports
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.DefaultFeatureType;
import org.apache.sis.internal.geoapi.filter.LogicalOperator;


/**
 * Tests {@link LogicalFilter} implementations.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class LogicalFilterTest extends TestCase {
    /**
     * The factory to use for creating the objects to test.
     */
    private final DefaultFilterFactory<AbstractFeature,Object,?> factory;

    /**
     * Creates a new test case.
     */
    public LogicalFilterTest() {
        factory = DefaultFilterFactory.forFeatures();
    }

    /**
     * Tests creation of "And" expression from the factory.
     * Also tests an evaluation from literal values and serialization.
     */
    @Test
    public void testAnd() {
        create(factory::and, factory::and, false);
    }

    /**
     * Tests creation of "Or" expression from the factory.
     * Also tests an evaluation from literal values and serialization.
     */
    @Test
    public void testOr() {
        create(factory::or, factory::or, true);
    }

    /**
     * Tests creation of "Not" expression from the factory.
     * Also tests an evaluation from literal values and serialization.
     */
    @Test
    public void testNot() {
        final Expression<AbstractFeature,String> literal = factory.literal("text");
        final Filter<AbstractFeature> operand = factory.isNull(literal);
        final LogicalOperator<AbstractFeature> filter  = (LogicalOperator<AbstractFeature> ) factory.not(operand);
        assertArrayEquals(new Filter<?>[] {operand}, filter.getOperands().toArray());
        assertTrue(filter.test(null));
        assertSerializedEquals(filter);
    }

    /**
     * Verifies that {@link Filter#negate()} is overridden.
     */
    @Test
    public void testNegate() {
        final Expression<AbstractFeature,String> literal = factory.literal("text");
        final Filter<AbstractFeature> operand = factory.isNull(literal);
        assertInstanceOf("Predicate.negate()", LogicalFilter.Not.class, operand.negate());
    }

    /**
     * Implementation of {@link #testAnd()} and {@link #testOr()}.
     *
     * @param  binary    the function creating a logical operator from two operands.
     * @param  anyArity  the function creating a logical operator from an arbitrary number of operands.
     * @param  expected  expected evaluation result.
     */
    private void create(final BiFunction<Filter<? super AbstractFeature>, Filter<? super AbstractFeature>, Filter<AbstractFeature>> binary,
                        final Function<Collection<Filter<? super AbstractFeature>>, Filter<AbstractFeature>> anyArity,
                        final boolean expected)
    {
        final Filter<AbstractFeature> f1 = factory.isNull(factory.literal("text"));
        final Filter<AbstractFeature> f2 = factory.isNull(factory.literal(null));
        try {
            binary.apply(null, null);
            fail("Creation with a null operand shall raise an exception.");
        } catch (NullPointerException ex) {
        }
        try {
            binary.apply(f1, null);
            fail("Creation with a null operand shall raise an exception.");
        } catch (NullPointerException ex) {
        }
        try {
            binary.apply(null, f2);
            fail("Creation with a null operand shall raise an exception.");
        } catch (NullPointerException ex) {
        }
        try {
            anyArity.apply(Collections.singleton(f1));
            fail("Creation with less then two operands shall raise an exception.");
        } catch (IllegalArgumentException ex) {
            assertNotNull(ex.getMessage());
        }
        /*
         * Test construction, evaluation and serialization.
         */
        LogicalOperator<AbstractFeature> filter = (LogicalOperator<AbstractFeature>) binary.apply(f1, f2);
        assertArrayEquals(new Filter<?>[] {f1, f2}, filter.getOperands().toArray());
        assertEquals(expected, filter.test(null));
        assertSerializedEquals(filter);

        filter = (LogicalOperator<AbstractFeature>) anyArity.apply(Arrays.asList(f1, f2, f1));
        assertArrayEquals(new Filter<?>[] {f1, f2, f1}, filter.getOperands().toArray());
        assertEquals(expected, filter.test(null));
        assertSerializedEquals(filter);
    }

    /**
     * Tests evaluation using a feature instance.
     */
    @Test
    public void testEvaluateOnFeature() {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.addAttribute(String.class).setName("attNull");
        ftb.addAttribute(String.class).setName("attNotNull");
        final AbstractFeature feature = ftb.setName("Test").build().newInstance();
        feature.setPropertyValue("attNotNull", "a value");

        final Filter<AbstractFeature> filterTrue  = factory.isNull(factory.property("attNull",    String.class));
        final Filter<AbstractFeature> filterFalse = factory.isNull(factory.property("attNotNull", String.class));

        assertTrue (factory.and(filterTrue,  filterTrue ).test(feature));
        assertFalse(factory.and(filterFalse, filterTrue ).test(feature));
        assertFalse(factory.and(filterTrue,  filterFalse).test(feature));
        assertFalse(factory.and(filterFalse, filterFalse).test(feature));

        assertTrue (factory.or (filterTrue,  filterTrue ).test(feature));
        assertTrue (factory.or (filterFalse, filterTrue ).test(feature));
        assertTrue (factory.or (filterTrue,  filterFalse).test(feature));
        assertFalse(factory.or (filterFalse, filterFalse).test(feature));

        assertFalse(factory.not(filterTrue ).test(feature));
        assertTrue (factory.not(filterFalse).test(feature));
    }

    /**
     * Tests {@link Optimization} applied on logical filters.
     */
    @Test
    public void testOptimization() {
        final Filter<AbstractFeature> f1 = factory.isNull(factory.literal("text"));
        final Filter<AbstractFeature> f2 = factory.isNull(factory.literal(null));
        optimize(factory.and(f1, f2), Filter.exclude());
        optimize(factory.or (f1, f2), Filter.include());
    }

    /**
     * Verifies an optimization which is expected to evaluate immediately.
     */
    private static void optimize(final Filter<AbstractFeature> original, final Filter<AbstractFeature> expected) {
        final Filter<? super AbstractFeature> optimized = new Optimization().apply(original);
        assertNotSame("Expected a new optimized filter.", original, optimized);
        assertSame("Second optimization should have no effect.", optimized, new Optimization().apply(optimized));
        assertSame("Expression should have been evaluated now.", expected, optimized);
    }

    /**
     * Tests {@link Optimization} applied on logical filters when the {@link DefaultFeatureType} is known.
     */
    @Test
    public void testFeatureOptimization() {
        final String attribute = "population";
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.addAttribute(String.class).setName(attribute);
        final DefaultFeatureType type = ftb.setName("Test").build();
        final AbstractFeature instance = type.newInstance();
        instance.setPropertyValue("population", "1000");
        /*
         * Prepare an expression which divide the population value by 5.
         */
        final Expression<AbstractFeature,Number> e = factory.divide(factory.property(attribute, Integer.class), factory.literal(5));
        final Optimization optimization = new Optimization();
        assertSame(e, optimization.apply(e));                       // No optimization.
        assertEquals(200, e.apply(instance).intValue());
        /*
         * Notify the optimizer that property values will be of `String` type.
         * The optimizer should compute an `ObjectConverter` in advance.
         */
        optimization.setFeatureType(type);
        final Expression<? super AbstractFeature, ? extends Number> opt = optimization.apply(e);
        assertEquals(200, e.apply(instance).intValue());
        assertNotSame(e, opt);

        final PropertyValue<?> p = (PropertyValue<?>) opt.getParameters().get(0);
        assertEquals(String.class,  p.getSourceClass());
        assertEquals(Integer.class, p.getValueClass());
    }
}
