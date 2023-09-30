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
package org.apache.sis.style.se1;

import java.awt.Color;
import java.util.Optional;
import org.apache.sis.util.SimpleInternationalString;

// Test dependencies
import org.apache.sis.test.TestCase;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.filter.Literal;
import org.opengis.filter.Expression;


/**
 * Base class of all tests of style elements.
 *
 * @author  Johann Sorel (Geomatys)
 */
abstract class StyleTestCase extends TestCase {
    /**
     * The factory to use for creating style elements.
     */
    final StyleFactory<Feature> factory;

    /**
     * Creates a new test case.
     */
    StyleTestCase() {
        factory = FeatureTypeStyle.FACTORY;
    }

    /**
     * Returns a literal for the given value.
     *
     * @param  <E>    type of value.
     * @param  value  the value for which to return a literal.
     * @return literal for the given value.
     */
    final <E> Literal<Feature,E> literal(final E value) {
        return factory.filterFactory.literal(value);
    }

    /**
     * Creates a dummy description with arbitrary title and abstract.
     */
    final Description<Feature> anyDescription() {
        final var value = factory.createDescription();
        value.setTitle(new SimpleInternationalString("A random title"));
        value.setAbstract(new SimpleInternationalString("A random abstract"));
        return value;
    }

    /**
     * Returns an expression with a random color.
     * The color is {@link #ANY_COLOR}.
     */
    final Expression<Feature,Color> anyColor() {
        return literal(ANY_COLOR);
    }

    /**
     * The color used by {@link #anyColor()}.
     * Should be different than all default colors.
     * Provided for verification with {@link #assertLiteralEquals(Object, Expression)}.
     */
    static final Color ANY_COLOR = Color.YELLOW;

    /**
     * Asserts that the given optional is empty.
     *
     * @param  opt  the optional to check.
     */
    static void assertEmpty(final Optional<?> opt) {
        assertTrue(opt.isEmpty());
    }

    /**
     * Asserts that the value of the given optional is equal to the expected value.
     *
     * @param <E>       type of object to compare.
     * @param expected  the expected value.
     * @param opt       the actual value in an optional.
     */
    static <E> void assertOptionalEquals(final E expected, final Optional<E> opt) {
        assertEquals(expected, opt.orElseThrow());
    }

    /**
     * Asserts that the given expression is a literal with the given value.
     *
     * @param  <E>       the expression value type.
     * @param  expected  the expected expression value.
     * @param  actual    the expression from which to test the value.
     */
    static <E> void assertLiteralEquals(final E expected, final Expression<?, ? extends E> actual) {
        assertInstanceOf("expression", Literal.class, actual);
        assertEquals(expected, actual.apply(null));
    }
}
