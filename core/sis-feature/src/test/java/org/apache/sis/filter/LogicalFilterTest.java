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

import java.util.Set;
import java.util.List;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;
import static org.apache.sis.test.Assertions.assertSerializedEquals;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.Expression;
import org.opengis.filter.Filter;
import org.opengis.filter.Literal;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.LogicalOperator;


/**
 * Tests {@link LogicalFilter} implementations.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.1
 */
public final class LogicalFilterTest extends TestCase {
    /**
     * The factory to use for creating the objects to test.
     */
    private final FilterFactory<Feature,Object,?> factory;

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
        final Literal<Feature,String>  literal = factory.literal("text");
        final Filter<Feature>          operand = factory.isNull(literal);
        final LogicalOperator<Feature> filter  = factory.not(operand);
        assertArrayEquals(new Filter<?>[] {operand}, filter.getOperands().toArray());
        assertTrue(filter.test(null));
        assertSerializedEquals(filter);
        assertFalse(isVolatile(filter));
    }

    /**
     * Verifies that {@link Filter#negate()} is overridden.
     */
    @Test
    public void testNegate() {
        final Literal<Feature,String>  literal = factory.literal("text");
        final Filter<Feature>          operand = factory.isNull(literal);
        assertInstanceOf("Predicate.negate()", LogicalFilter.Not.class, operand.negate());
    }

    /**
     * Tests a filter having a volatile expression.
     */
    @Test
    public void testVolatile() {
        final var literal = new LeafExpression.Literal<Feature,Object>("test") {
            @Override public Set<FunctionProperty> properties() {
                return Set.of(FunctionProperty.VOLATILE);
            }
        };
        final Filter<Feature>          operand = factory.isNull(literal);
        final LogicalOperator<Feature> filter  = factory.not(operand);
        assertTrue(isVolatile(filter));
    }

    /**
     * Implementation of {@link #testAnd()} and {@link #testOr()}.
     *
     * @param  binary    the function creating a logical operator from two operands.
     * @param  anyArity  the function creating a logical operator from an arbitrary number of operands.
     * @param  expected  expected evaluation result.
     */
    private void create(final BiFunction<Filter<Feature>, Filter<Feature>, LogicalOperator<Feature>> binary,
                        final Function<Collection<Filter<Feature>>, LogicalOperator<Feature>> anyArity,
                        final boolean expected)
    {
        final Filter<Feature> f1 = factory.isNull(factory.literal("text"));
        final Filter<Feature> f2 = factory.isNull(factory.literal(null));
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
            anyArity.apply(Set.of(f1));
            fail("Creation with less then two operands shall raise an exception.");
        } catch (IllegalArgumentException ex) {
            assertNotNull(ex.getMessage());
        }
        /*
         * Test construction, evaluation and serialization.
         */
        LogicalOperator<Feature> filter = binary.apply(f1, f2);
        assertArrayEquals(new Filter<?>[] {f1, f2}, filter.getOperands().toArray());
        assertEquals(expected, filter.test(null));
        assertSerializedEquals(filter);
        assertFalse(isVolatile(filter));
        /*
         * Same test, using the constructor accepting any number of operands.
         */
        filter = anyArity.apply(List.of(f1, f2, f1));
        assertArrayEquals(new Filter<?>[] {f1, f2, f1}, filter.getOperands().toArray());
        assertEquals(expected, filter.test(null));
        assertSerializedEquals(filter);
        assertFalse(isVolatile(filter));
        /*
         * Test the `Predicate` methods, which should be overridden by `Optimization.OnFilter`.
         */
        assertInstanceOf("Predicate.and(…)",   Optimization.OnFilter.class, f1.and(f2));
        assertInstanceOf("Predicate.or(…)",    Optimization.OnFilter.class, f1.or(f2));
        assertInstanceOf("Predicate.negate()", Optimization.OnFilter.class, f1.negate());
    }

    /**
     * Tests evaluation using a feature instance.
     */
    @Test
    public void testEvaluateOnFeature() {
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.addAttribute(String.class).setName("attNull");
        ftb.addAttribute(String.class).setName("attNotNull");
        final Feature feature = ftb.setName("Test").build().newInstance();
        feature.setPropertyValue("attNotNull", "a value");

        final Filter<Feature> filterTrue  = factory.isNull(factory.property("attNull",    String.class));
        final Filter<Feature> filterFalse = factory.isNull(factory.property("attNotNull", String.class));

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
        /*
         * Test the `Predicate` methods, which should be overridden by `Optimization.OnFilter`.
         */
        Predicate<Feature> predicate = filterTrue.and(filterFalse);
        assertInstanceOf("Predicate.and(…)", Optimization.OnFilter.class, predicate);
        assertFalse(predicate.test(feature));

        predicate = filterTrue.or(filterFalse);
        assertInstanceOf("Predicate.or(…)", Optimization.OnFilter.class, predicate);
        assertTrue(predicate.test(feature));
    }

    /**
     * Tests {@link Optimization} applied on logical filters.
     */
    @Test
    public void testOptimization() {
        final Filter<Feature> f1 = factory.isNull(factory.literal("text"));     // False
        final Filter<Feature> f2 = factory.isNull(factory.literal(null));       // True
        final Filter<Feature> f3 = factory.isNull(factory.property("*"));       // Indeterminate
        optimize(factory.and(f1, f2), Filter.exclude());
        optimize(factory.or (f1, f2), Filter.include());
        optimize(factory.and(f3, factory.not(f3)), Filter.exclude());
        optimize(factory.or (f3, factory.not(f3)), Filter.include());
    }

    /**
     * Verifies an optimization which is expected to evaluate immediately.
     */
    private static void optimize(final Filter<Feature> original, final Filter<Feature> expected) {
        final Filter<? super Feature> optimized = new Optimization().apply(original);
        assertNotSame("Expected a new optimized filter.", original, optimized);
        assertSame("Second optimization should have no effect.", optimized, new Optimization().apply(optimized));
        assertSame("Expression should have been evaluated now.", expected, optimized);
    }

    /**
     * Tests {@link Optimization} applied on logical filters when the {@link FeatureType} is known.
     */
    @Test
    public void testFeatureOptimization() {
        final String attribute = "population";
        final FeatureTypeBuilder ftb = new FeatureTypeBuilder();
        ftb.addAttribute(String.class).setName(attribute);
        final FeatureType type = ftb.setName("Test").build();
        final Feature instance = type.newInstance();
        instance.setPropertyValue("population", "1000");
        /*
         * Prepare an expression which divide the population value by 5.
         */
        final Expression<Feature,Number> e = factory.divide(factory.property(attribute, Integer.class), factory.literal(5));
        final Optimization optimization = new Optimization();
        assertSame(e, optimization.apply(e));                       // No optimization.
        assertEquals(200, e.apply(instance).intValue());
        /*
         * Notify the optimizer that property values will be of `String` type.
         * The optimizer should compute an `ObjectConverter` in advance.
         */
        optimization.setFeatureType(type);
        final Expression<? super Feature, ? extends Number> opt = optimization.apply(e);
        assertEquals(200, e.apply(instance).intValue());
        assertNotSame(e, opt);

        final PropertyValue<?> p = (PropertyValue<?>) opt.getParameters().get(0);
        assertEquals(String.class, p.getSourceClass());
        assertEquals(Number.class, p.getValueClass());
    }

    /**
     * Returns {@code true} if the given filter is declared volatile.
     */
    private static boolean isVolatile(final Filter<?> filter) {
        return Optimization.properties(filter).equals(Set.of(FunctionProperty.VOLATILE));
    }
}
