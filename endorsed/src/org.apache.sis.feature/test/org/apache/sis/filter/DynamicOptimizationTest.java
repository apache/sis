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
import java.util.HashSet;
import java.util.Set;
import org.apache.sis.feature.AbstractFeature;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.filter.base.Node;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCaseWithLogs;


/**
 * Tests {@link DynamicOptimization} implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class DynamicOptimizationTest extends TestCaseWithLogs {
    /**
     * A feature for a city with "name" and "population" attributes.
     */
    private final AbstractFeature city;

    /**
     * A feature for a city which is a capital.
     */
    private final AbstractFeature capital;

    /**
     * The factory to use for creating the objects to test.
     */
    private final DefaultFilterFactory<AbstractFeature, ?, ?> factory;

    /**
     * Creates a new test case.
     */
    public DynamicOptimizationTest() {
        super(Node.LOGGER);
        factory = DefaultFilterFactory.forFeatures();
        final var builder = new FeatureTypeBuilder();
        builder.addAttribute(String.class).setName("name");
        builder.addAttribute(Integer.class).setName("population");
        city = builder.setName("City").build().newInstance();
        city.setPropertyValue("name",       "Osaka");
        city.setPropertyValue("population", 2752024);  // In 2020 (Wikipedia).

        builder.clear().setSuperTypes(city.getType());
        builder.addAttribute(String.class).setName("country");
        capital = builder.setName("Capital").build().newInstance();
        capital.setPropertyValue("country",    "Japan");
        capital.setPropertyValue("name",       "Tokyo");
        capital.setPropertyValue("population", 14192184);  // In 2024 (Wikipedia).
    }

    /**
     * Tests an optimization which can be resolved in advance.
     * We use the {@code "population"} property which exists in all feature types.
     * No {@link DynamicOptimization} should be needed for this case.
     */
    @Test
    public void testWithPropertyResolvedInAdvance() {
        final var filter = factory.greater(factory.property("population", Integer.class), factory.literal(10000000));
        assertFalse(filter.test(city));
        assertTrue(filter.test(capital));
        loggings.assertNoUnexpectedLog();

        final var optimization = new Optimization();
        optimization.setFinalFeatureTypes(Set.of(city.getType(), capital.getType()));
        final var optimized = optimization.apply(filter);
        assertFalse(optimized instanceof DynamicOptimization<?>);
        assertNotSame(filter, optimized);   // Optimized but not dynamic.
        assertFalse(optimized.test(city));
        assertTrue(optimized.test(capital));
        loggings.assertNoUnexpectedLog();

        var checked = new CheckedInstance(city, "population");
        assertFalse(optimized.test(checked));
        checked.assertAllPropertiesHaveBeenRead();

        checked = new CheckedInstance(capital, "population");
        assertTrue(optimized.test(checked));
        checked.assertAllPropertiesHaveBeenRead();
    }

    /**
     * Tests an optimization which cannot be resolved in advance.
     * A {@link DynamicOptimization} is needed for this case.
     */
    @Test
    public void testDynamic() {
        final var filter = factory.equal(factory.property("country", String.class), factory.literal("Japan"));
        assertFalse(filter.test(city));
        loggings.assertNextLogContains("country", "City");
        assertTrue(filter.test(capital));
        loggings.assertNoUnexpectedLog();

        final var optimization = new Optimization();
        optimization.setFinalFeatureTypes(Set.of(city.getType(), capital.getType()));
        final var optimized = optimization.apply(filter);
        assertInstanceOf(DynamicOptimization.class, optimized);
        assertFalse(optimized.test(city));
        assertTrue(optimized.test(capital));
        loggings.assertNoUnexpectedLog();

        var checked = new CheckedInstance(city);
        assertFalse(optimized.test(checked));
        checked.assertAllPropertiesHaveBeenRead();
        loggings.assertNoUnexpectedLog();

        checked = new CheckedInstance(capital, "country");
        assertTrue(optimized.test(checked));
        checked.assertAllPropertiesHaveBeenRead();
        loggings.assertNoUnexpectedLog();
    }

    /**
     * A feature instance which ensure that no unexpected method is invoked.
     */
    @SuppressWarnings("serial")
    private static final class CheckedInstance extends AbstractFeature {
        /** The original feature to wrap. */
        private final AbstractFeature original;

        /** Names of the properties that are expected to be queried. */
        private final Set<String> expectedQueries;

        /** Creates a wrapper for the given feature instance. */
        CheckedInstance(final AbstractFeature original, final String... expected) {
            super(original.getType());
            this.original = original;
            expectedQueries = new HashSet<>(Arrays.asList(expected));
        }

        /** Returns the value of the property of the given name. */
        @Override public Object getPropertyValue(final String name) {
            assertTrue(expectedQueries.remove(name), name);
            return original.getPropertyValue(name);
        }

        /** Should not be invoked. */
        @Override public void setPropertyValue(String name, Object value) {
            fail();
        }

        /** Verifies that all properties that we expected to be read have been read. */
        void assertAllPropertiesHaveBeenRead() {
            assertTrue(expectedQueries.isEmpty(), expectedQueries.toString());
        }
    }
}
