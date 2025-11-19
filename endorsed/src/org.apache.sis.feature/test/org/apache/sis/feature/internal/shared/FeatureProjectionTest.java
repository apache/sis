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
package org.apache.sis.feature.internal.shared;

import java.util.Set;
import java.util.HashSet;
import org.apache.sis.util.iso.Names;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.filter.DefaultFilterFactory;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Operation;
import org.opengis.filter.FilterFactory;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertSetEquals;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link FeatureProjection} and {@link FeatureProjectionBuilder}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class FeatureProjectionTest extends TestCase {
    /**
     * The feature type to use as a source, before projection.
     */
    private final FeatureType source;

    /**
     * The factory to use for building expressions.
     */
    private final FilterFactory<Feature, ?, ?> ff;

    /**
     * Creates a new test case.
     */
    public FeatureProjectionTest() {
        ff = DefaultFilterFactory.forFeatures();
        var builder = new FeatureTypeBuilder().setName("Country");
        builder.addAttribute(String .class).setName("name");
        builder.addAttribute(String .class).setName("capital");
        builder.addAttribute(Integer.class).setName("population");
        builder.addAttribute(Double .class).setName("area");
        source = builder.build();
    }

    /**
     * Tests the creation of a subset on a simple feature type.
     * The type of values are not changed.
     */
    @Test
    public void testSubsetWithSameValueClasses() {
        final var builder = new FeatureProjectionBuilder(source, null);
        addCountry(builder);
        addPopulation(builder, false);
        final FeatureProjection projection = assertContryAndPopulation(builder);
        assertValueClassEquals(Integer.class, projection, "population");
        final Feature result = applyOnFeatureInstance(projection);
        assertEquals("Canada", result.getPropertyValue("country"));
        assertEquals(40769890, result.getPropertyValue("population"));
    }

    /**
     * Tests the creation of a subset on a simple feature type with a change of property type.
     * The type of the population value is changed from {@code Integer} to {@code Long}.
     */
    @Test
    public void testSubsetWithModifiedValueClasses() {
        final var builder = new FeatureProjectionBuilder(source, null);
        addCountry(builder);
        addPopulation(builder, true);
        final FeatureProjection projection = assertContryAndPopulation(builder);
        assertValueClassEquals(Long.class, projection, "population");
        final Feature result = applyOnFeatureInstance(projection);
        assertEquals("Canada",  result.getPropertyValue("country"));
        assertEquals(40769890L, result.getPropertyValue("population"));
    }

    /**
     * Creates a feature instance, applies the projection and returns the result.
     *
     * @param  projection  the projection to apply.
     * @return the projected feature instance.
     */
    private Feature applyOnFeatureInstance(final FeatureProjection projection) {
        final Feature instance = source.newInstance();
        instance.setPropertyValue("name",       "Canada");
        instance.setPropertyValue("area",       9984670d);
        instance.setPropertyValue("capital",    "Ottawa");
        instance.setPropertyValue("population", 40769890);  // In 2024.
        final Feature result = projection.apply(instance);
        assertNotEquals(instance, result);
        return result;
    }

    /**
     * Builds the projection and asserts that it contains a country name and a population.
     *
     * @param  builder  the builder.
     * @return the projection built by the given builder.
     */
    private FeatureProjection assertContryAndPopulation(final FeatureProjectionBuilder builder) {
        assertSame(source, builder.source());
        builder.setName("Country population");
        final FeatureProjection projection = builder.project().orElseThrow();
        assertSame(projection.typeRequested, projection.typeWithDependencies);
        assertPropertyNamesEqual(projection.typeRequested, "country", "population");
        assertSetEquals(Set.of("name", "population"), projection.dependencies());
        assertValueClassEquals(String.class, projection, "country");
        return projection;
    }

    /**
     * Asserts that the given feature has properties of the given names in the same order.
     */
    private static void assertPropertyNamesEqual(final FeatureType type, final String... expected) {
        assertArrayEquals(expected, type.getProperties(true).stream().map((p) -> p.getName().toString()).toArray());
    }

    /**
     * Asserts that the given property is an attribute with the specified value type.
     *
     * @param expected    expected value class.
     * @param projection  the projection to verify.
     * @param property    the property to verify in the given projection.
     */
    private static void assertValueClassEquals(final Class<?> expected, final FeatureProjection projection, final String property) {
        assertEquals(expected, assertInstanceOf(AttributeType.class, projection.typeRequested.getProperty(property)).getValueClass());
    }

    /**
     * Adds a projection item for the country name.
     * The property is renamed from "name" to "country".
     *
     * @param builder  the builder in which to add the item.
     */
    private void addCountry(final FeatureProjectionBuilder builder) {
        final FeatureProjectionBuilder.Item item = builder.addSourceProperty(source.getProperty("name"), true);
        assertEquals("name", item.sourceName.toString());
        assertEquals("name", item.getPreferredName());
        item.setPreferredName(Names.createLocalName(null, null, "country"));
        assertEquals("country", item.getPreferredName());
        assertNull(item.attributeValueGetter());
        item.setValueGetter(ff.property("name"), true);
    }

    /**
     * Adds a projection item for the population, optionally with a change of the value type.
     *
     * @param builder     the builder in which to add the item.
     * @param changeType  whether to change the population type from {@code Integer} to {@code Long}.
     */
    private void addPopulation(final FeatureProjectionBuilder builder, final boolean changeType) {
        final FeatureProjectionBuilder.Item item = builder.addSourceProperty(source.getProperty("population"), true);
        assertEquals("population", item.sourceName.toString());
        assertEquals("population", item.getPreferredName());
        assertNull(item.attributeValueGetter());
        item.setValueGetter(ff.property("population"), true);
        assertTrue(item.replaceValueClass((type) -> {
            assertEquals(Integer.class, type);
            return changeType ? Long.class : type;
        }));
    }

    /**
     * Adds an expression which computes the population density from two other properties.
     *
     * @param builder  the builder in which to add the item.
     * @param stored   {@code true} for evaluating the expression immediately after feature instances
     *                 are known, or {@code false} for wrapping the expression in a feature operation.
     */
    private void addPopulationDensity(final FeatureProjectionBuilder builder, final boolean stored) {
        final FeatureProjectionBuilder.Item item = builder.addComputedProperty(
                builder.addAttribute(Number.class).setName("density"), true);
        assertEquals("density", item.sourceName.toString());
        assertEquals("density", item.getPreferredName());
        assertNull(item.attributeValueGetter());
        item.setValueGetter(ff.divide(ff.property("population").toValueType(Number.class),
                                      ff.property("area").toValueType(Number.class)), stored);
    }

    /**
     * Tests the creation of a feature with an additional property computed early.
     * The operation is computed immediately from the source feature type.
     */
    @Test
    public void testSubsetWithStoredOperation() {
        final var builder = new FeatureProjectionBuilder(source, null);
        addCountry(builder);
        addPopulation(builder, false);
        addPopulationDensity(builder, true);
        builder.setName("Population density");
        final FeatureProjection projection = builder.project().orElseThrow();
        assertSame(projection.typeRequested, projection.typeWithDependencies);
        assertSetEquals(Set.of("name", "population", "area"), projection.dependencies());
        assertPropertyNamesEqual(projection.typeRequested, "country", "population", "density");

        // Property is an attribute because we requested the "stored" mode.
        assertInstanceOf(AttributeType.class, projection.typeWithDependencies.getProperty("density"));
        assertValueClassEquals(Number.class,  projection, "density");
        verifyDensityOnFeatureInstance(projection);
    }

    /**
     * Tests the creation of a feature with an additional property computed when first requested.
     * The operation forces the projection to add dependencies that were not part of the request.
     */
    @Test
    public void testSubsetWithDeferredOperation() {
        final var builder = new FeatureProjectionBuilder(source, null);
        addCountry(builder);
        addPopulation(builder, false);
        addPopulationDensity(builder, false);
        builder.setName("Population density");
        final FeatureProjection projection = builder.project().orElseThrow();
        assertNotEquals(projection.typeRequested, projection.typeWithDependencies);
        assertSetEquals(Set.of("name", "population", "area"), projection.dependencies());
        assertPropertyNamesEqual(projection.typeRequested, "country", "population", "density");
        assertPropertyNamesEqual(projection.typeWithDependencies, "country", "population", "density", "area");

        // Property is an operation because we requested the "deferred" mode.
        assertInstanceOf(Operation.class, projection.typeWithDependencies.getProperty("density"));

        // Operation should have been replaced by a view because of missing dependencies.
        assertInstanceOf(OperationView.class, projection.typeRequested.getProperty("density"));
        verifyDensityOnFeatureInstance(projection);
    }

    /**
     * Tests the creation of a feature where an operation has been replaced by a simpler one.
     * This case happen when a pure-Java operation has been replaced by a <abbr>SQL</abbr> expression,
     * in which case the expression is simpler from <abbr>SIS</abbr> perspective. A consequence of this
     * simplification is that it may remove the need for some dependencies.
     */
    @Test
    public void testSubsetWithReplacedOperation() {
        final var builder = new FeatureProjectionBuilder(source, null);
        addCountry(builder);
        addPopulation(builder, false);
        addPopulationDensity(builder, true);
        builder.setName("Population density");
        FeatureProjection projection = builder.project().orElseThrow();
        final Set<String> expected = new HashSet<>(Set.of("country", "population", "density"));
        projection = new FeatureProjection(projection, (name, expression) -> {
            assertTrue(expected.remove(name), name);
            if (name.equals("density")) {
                return ff.literal(4.08);
            }
            return expression;
        });
        assertTrue(expected.isEmpty(), expected.toString());
        assertSame(projection.typeRequested, projection.typeWithDependencies);  // Because no more extra dependency.
        assertSetEquals(Set.of("name", "population"), projection.dependencies());
        assertPropertyNamesEqual(projection.typeRequested, "country", "population", "density");
        verifyDensityOnFeatureInstance(projection);
    }

    /**
     * Creates a feature instance, applies the projection and verifies the result.
     *
     * @param  projection  the projection to apply.
     */
    private void verifyDensityOnFeatureInstance(final FeatureProjection projection) {
        assertValueClassEquals(String.class,  projection, "country");
        assertValueClassEquals(Integer.class, projection, "population");
        final Feature result = applyOnFeatureInstance(projection);
        assertEquals("Canada", result.getPropertyValue("country"));
        assertEquals(40769890, result.getPropertyValue("population"));
        assertEquals(4.08, assertInstanceOf(Double.class, result.getPropertyValue("density")), 1.01);
    }
}
