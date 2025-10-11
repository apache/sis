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
package org.apache.sis.feature;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import org.opengis.util.InternationalString;
import org.apache.sis.util.iso.DefaultNameFactory;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import static org.apache.sis.test.Assertions.assertSingleton;


/**
 * Tests {@link DefaultFeatureType}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class DefaultFeatureTypeTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultFeatureTypeTest() {
    }

    /**
     * Convenience method returning the given name in a a property map
     * to be given to {@link AbstractIdentifiedType} constructor.
     */
    private static Map<String,?> name(final Object name) {
        return Map.of(AbstractIdentifiedType.NAME_KEY, name);
    }

    /**
     * Creates a simple feature type without super-types.
     * The feature contains the following attributes:
     *
     * <ul>
     *   <li>{@code city}       as a  {@link String}  (mandatory)</li>
     *   <li>{@code population} as an {@link Integer} (mandatory)</li>
     * </ul>
     *
     * @return the feature for a city.
     */
    public static DefaultFeatureType city() {
        final var identification = new HashMap<String,Object>();
        final DefaultAttributeType<String>  city       = DefaultAttributeTypeTest.city(identification);
        final DefaultAttributeType<Integer> population = DefaultAttributeTypeTest.population(identification);
        return new DefaultFeatureType(name("City"), false, null, city, population);
    }

    /**
     * Creates a sub-type of the "city" type with only one additional property, an arbitrary number of strings.
     * The feature contains the following attribute:
     *
     * <ul>
     *   <li>{@code city}         as a  {@link String}  (mandatory)</li>
     *   <li>{@code population}   as an {@link Integer} (mandatory)</li>
     *   <li>{@code universities} as an arbitrary number of {@link String}</li>
     * </ul>
     *
     * @return the feature for an university city.
     */
    public static DefaultFeatureType universityCity() {
        return new DefaultFeatureType(name("University city"), false,
                new DefaultFeatureType[] {city()}, DefaultAttributeTypeTest.universities());
    }

    /**
     * Creates a sub-type of the "city" type with only one additional property, a string giving the parliament name.
     * The feature contains the following attribute:
     *
     * <ul>
     *   <li>{@code city}       as a  {@link String}  (mandatory)</li>
     *   <li>{@code population} as an {@link Integer} (mandatory)</li>
     *   <li>{@code parliament} as a  {@link String}  (mandatory)</li>
     * </ul>
     *
     * @return the feature for a capital.
     */
    public static DefaultFeatureType capital() {
        return new DefaultFeatureType(name("Capital"), false,
                new DefaultFeatureType[] {city()}, DefaultAttributeTypeTest.parliament());
    }

    /**
     * Creates a sub-type of the "city" type with two additional properties.
     * The feature contains the following attribute:
     *
     * <ul>
     *   <li>{@code city}       as a  {@link String}       (mandatory)</li>
     *   <li>{@code population} as an {@link Integer}      (mandatory)</li>
     *   <li>{@code region}     as a  {@link CharSequence} (mandatory) — the region for which the city is a metropolis.</li>
     *   <li>{@code isGlobal}   as a  {@link Boolean}      (mandatory) — whether the city has an effect on global affairs.</li>
     * </ul>
     *
     * @return the feature for a metropolis.
     */
    public static DefaultFeatureType metropolis() {
        final var identification = new HashMap<String,Object>(4);
        assertNull(identification.put(DefaultFeatureType.NAME_KEY,         "Metropolis"));
        assertNull(identification.put(DefaultFeatureType.NAME_KEY + "_fr", "Métropole"));
        return new DefaultFeatureType(identification, false,
                new DefaultFeatureType[] {city()},
                new DefaultAttributeType<>(name("region"), CharSequence.class, 1, 1, null),
                new DefaultAttributeType<>(name("isGlobal"),    Boolean.class, 1, 1, null));
    }

    /**
     * Creates a sub-type of the "metropolis" type with the "region" attribute overridden to
     * {@link InternationalString} and an arbitrary number of universities.
     */
    static DefaultFeatureType worldMetropolis() {
        return worldMetropolis(metropolis(), universityCity(), CharacteristicTypeMapTest.temperature(), InternationalString.class);
    }

    /**
     * Creates a sub-type of the "metropolis" type with the "region" attribute overridden to the given type.
     * The given type should be {@link InternationalString}, but we allow other types for testing argument checks.
     */
    private static DefaultFeatureType worldMetropolis(final DefaultFeatureType metropolis,
            final DefaultFeatureType universityCity, final DefaultAttributeType<?> temperature, final Class<?> regionType)
    {
        return new DefaultFeatureType(name("World metropolis"), false,
                new DefaultFeatureType[] {                                                  // Super types
                    metropolis,
                    universityCity
                },
                new DefaultAttributeType<?>[] {                                             // Properties
                    new DefaultAttributeType<>(name("region"), regionType, 1, 1, null),
                    temperature
                });
    }

    /**
     * Verifies that {@code DefaultFeatureType} methods returns unmodifiable collections.
     * This method does <strong>not</strong> check recursively the properties.
     */
    private static void assertUnmodifiable(final DefaultFeatureType feature) {
        final Collection<?> superTypes         = feature.getSuperTypes();
        final Collection<?> declaredProperties = feature.getProperties(false);
        final Collection<?> allProperties      = feature.getProperties(true);
        if (!superTypes.isEmpty()) {
            var e = assertThrows(UnsupportedOperationException.class, () -> superTypes.clear());
            assertFalse(superTypes.isEmpty());
            assertNotNull(e);
        }
        if (!declaredProperties.isEmpty()) {
            var e = assertThrows(UnsupportedOperationException.class, () -> declaredProperties.clear());
            assertFalse(declaredProperties.isEmpty());
            assertNotNull(e);
        }
        if (!allProperties.isEmpty()) {
            var e = assertThrows(UnsupportedOperationException.class, () -> allProperties.clear());
            assertFalse(allProperties.isEmpty());
            assertNotNull(e);
        }
        /*
         * Opportunist check: `properties(true)` shall contain all `properties(false)` elements.
         */
        assertTrue(allProperties.containsAll(declaredProperties));
    }

    /**
     * Asserts that the given feature contains the given properties, in the same order.
     * This method tests the following {@code FeatureType} methods:
     *
     * <ul>
     *   <li>{@link DefaultFeatureType#getProperties(boolean)}</li>
     *   <li>{@link DefaultFeatureType#getProperty(String)}</li>
     * </ul>
     *
     * @param  feature            the feature to verify.
     * @param  includeSuperTypes  {@code true} for including the properties inherited from the super-types, or
     *                            {@code false} for returning only the properties defined explicitly in the feature type.
     * @param  expected           names of the expected properties.
     */
    private static void assertPropertiesEquals(final DefaultFeatureType feature, final boolean includeSuperTypes,
            final String... expected)
    {
        int index = 0;
        for (final AbstractIdentifiedType property : feature.getProperties(includeSuperTypes)) {
            assertTrue(index < expected.length, "Found more properties than expected.");
            final String name = expected[index++];
            assertNotNull(property, name);
            assertEquals (property.getName().toString(), name);
            assertSame   (property, feature.getProperty(name), name);
        }
        assertEquals(expected.length, index, "Unexpected number of properties.");
        var e = assertThrows(IllegalArgumentException.class, () -> feature.getProperty("apple"));
        assertMessageContains(e, "apple", feature.getName().toString());
    }

    /**
     * Tests the construction of a simple feature without super-types.
     * A feature is said "simple" if the multiplicity of all attributes is [1 … 1].
     *
     * <p>Current implementation performs its tests on the {@link #city()} feature.</p>
     */
    @Test
    public void testSimple() {
        final DefaultFeatureType simple = city();
        assertUnmodifiable(simple);
        assertEquals("City", simple.getName().toString());
        assertTrue  (        simple.getSuperTypes().isEmpty());
        assertFalse (        simple.isAbstract());
        assertFalse (        simple.isSparse());
        assertTrue  (        simple.isSimple());
        assertTrue  (        simple.isAssignableFrom(simple));
        assertEquals(2,      simple.indices().size());
        assertPropertiesEquals(simple, false, "city", "population");
    }

    /**
     * Tests the construction of a "complex" feature without super-types.
     * A feature is said "complex" if it contains at least one attribute
     * with a multiplicity different than [0 … 0] and [1 … 1].
     */
    @Test
    public void testComplex() {
        final var identification = new HashMap<String,Object>();
        final DefaultAttributeType<String>  city       = DefaultAttributeTypeTest.city(identification);
        final DefaultAttributeType<Integer> population = DefaultAttributeTypeTest.population(identification);
        testComplex(city, population, 0, 0);    // Simple
        testComplex(city, population, 0, 1);
        testComplex(city, population, 0, 2);
        testComplex(city, population, 1, 2);
        testComplex(city, population, 1, 1);    // Simple
    }

    /**
     * Implementation of {@link #testComplex()} for the given minimum and maximum occurrences.
     */
    private static void testComplex(
            final DefaultAttributeType<String>  city,
            final DefaultAttributeType<Integer> population,
            final int minimumOccurs, final int maximumOccurs)
    {
        final DefaultAttributeType<String> festival = new DefaultAttributeType<>(
                name("festival"), String.class, minimumOccurs, maximumOccurs, null);

        final DefaultFeatureType complex = new DefaultFeatureType(
                name("Festival"), false, null, city, population, festival);

        assertUnmodifiable(complex);
        final var properties = complex.getProperties(false);
        final var it = properties.iterator();

        assertEquals("Festival",                     complex.getName().toString());
        assertTrue  (                                complex.getSuperTypes().isEmpty());
        assertTrue  (                                complex.isAssignableFrom(complex));
        assertFalse (                                complex.isAbstract());
        assertFalse (                                complex.isSparse());
        assertEquals(maximumOccurs == minimumOccurs, complex.isSimple());
        assertEquals(maximumOccurs == 0 ? 2 : 3,     complex.indices().size());
        assertEquals(minimumOccurs,                  festival.getMinimumOccurs());
        assertEquals(maximumOccurs,                  festival.getMaximumOccurs());
        assertEquals(3,                              properties.size());
        assertSame  (city,                           it.next());
        assertSame  (population,                     it.next());
        assertSame  (festival,                       it.next());
        assertFalse (it.hasNext());
    }

    /**
     * Ensures that we cannot use two properties with the same name.
     */
    @Test
    public void testNameCollision() {
        final var city       = new DefaultAttributeType<>(name("name"),       String.class,  1, 1, null);
        final var cityId     = new DefaultAttributeType<>(name("name"),       Integer.class, 1, 1, null);
        final var population = new DefaultAttributeType<>(name("population"), Integer.class, 1, 1, null);
        var e = assertThrows(IllegalArgumentException.class,
                () -> new DefaultFeatureType(name("City"), false, null, city, population, cityId),
                "Duplicated attribute names shall not be allowed");
        assertMessageContains(e, "name", "City");
    }

    /**
     * Same as {@link #testNameCollision()}, but resolving collisions with usage of names
     * of the form {@code "head:tip"}.
     */
    @Test
    public void testQualifiedNames() {
        final var factory = DefaultNameFactory.provider();
        final var city = new DefaultAttributeType<>(
                name(factory.createGenericName(null, "ns1", "name")),
                String.class, 1, 1, null);
        final var cityId = new DefaultAttributeType<>(
                name(factory.createGenericName(null, "ns2", "name")),
                Integer.class, 1, 1, null);
        final var population = new DefaultAttributeType<>(
                name(factory.createGenericName(null, "ns1", "population")),
                Integer.class, 1, 1, null);
        final var feature = new DefaultFeatureType(
                name("City"), false, null, city, cityId, population);

        final var it = feature.getProperties(false).iterator();
        assertSame (city,       it.next());
        assertSame (cityId,     it.next());
        assertSame (population, it.next());
        assertFalse(it.hasNext());

        assertSame(city,       feature.getProperty("ns1:name"));
        assertSame(cityId,     feature.getProperty("ns2:name"));
        assertSame(population, feature.getProperty("ns1:population"));
        assertSame(population, feature.getProperty(    "population"));

        IllegalArgumentException e;
        e = assertThrows(IllegalArgumentException.class, () -> feature.getProperty("name"), "Expected no alias because of ambiguity.");
        assertMessageContains(e, "name", "ns1:name", "ns2:name");

        e = assertThrows(IllegalArgumentException.class, () -> feature.getProperty("other"));
        assertMessageContains(e, "other", "City");
    }

    /**
     * Tests two names having the same tip, but where only one of the two names have a namespace.
     */
    @Test
    public void testQualifiedAndUnqualifiedNames() {
        final var factory = DefaultNameFactory.provider();
        final var a1 = new DefaultAttributeType<>(
                name(factory.createGenericName(null, "sis", "identifier")),
                String.class, 1, 1, null);
        final var a2 = new DefaultAttributeType<>(
                name(factory.createGenericName(null, "identifier")),
                String.class, 1, 1, null);
        final var feature = new DefaultFeatureType(
                name("City"), false, null, a1, a2);

        assertSame(a1, feature.getProperty("sis:identifier"));
        assertSame(a2, feature.getProperty(    "identifier"));
    }

    /**
     * Tests inclusion of a property of kind operation.
     */
    @Test
    public void testOperationProperty() {
        final Map<String,?> featureName    = name("Identified city");
        final Map<String,?> identifierName = name("identifier");
        final DefaultFeatureType[] parent  = {city()};
        final DefaultFeatureType city = new DefaultFeatureType(featureName, false,
                parent, new LinkOperation(identifierName, parent[0].getProperty("city")));
        assertPropertiesEquals(city, true, "city", "population", "identifier");
        /*
         * Try to add an operation that depends on a non-existent property.
         * Such construction shall not be allowed.
         */
        final var parliament = new LinkOperation(identifierName, DefaultAttributeTypeTest.parliament());
        var e = assertThrows(IllegalArgumentException.class,
                () -> new DefaultFeatureType(featureName, false, parent, parliament));
        assertMessageContains(e, "identifier", "parliament", "Identified city");
    }

    /**
     * Tests a feature type which inherit from another feature type, but without property overriding.
     *
     * <p>Current implementation performs its tests on the {@link #capital()} feature.</p>
     */
    @Test
    public void testInheritance() {
        final DefaultFeatureType city    = city();      // Tested by 'testSimple()'.
        final DefaultFeatureType capital = capital();
        assertUnmodifiable(capital);
        assertEquals("Capital", capital.getName().toString());
        assertEquals(city, assertSingleton(capital.getSuperTypes()));
        assertFalse (   capital.isAbstract());
        assertFalse (   capital.isSparse());
        assertTrue  (   capital.isSimple());
        assertEquals(3, capital.indices().size());

        assertPropertiesEquals(city,    false, "city", "population");
        assertPropertiesEquals(capital, false, "parliament");
        assertPropertiesEquals(capital, true,  "city", "population", "parliament");

        // Check based only on name.
        assertTrue (DefaultFeatureType.maybeAssignableFrom(city, capital));
        assertFalse(DefaultFeatureType.maybeAssignableFrom(capital, city));

        // Public API.
        assertTrue (city.isAssignableFrom(capital));
        assertFalse(capital.isAssignableFrom(city));
    }

    /**
     * Tests the inheritance of 2 types having the same common parent.
     */
    @Test
    public void testMultiInheritance() {
        final DefaultFeatureType metropolis   = metropolis();
        final DefaultFeatureType capital      = capital();      // Tested by 'testComplex()'.
        final DefaultFeatureType metroCapital = new DefaultFeatureType(
                name("Metropolis and capital"), false,
                new DefaultFeatureType[] {metropolis, capital},
                new DefaultAttributeType<>(name("country"),
                        String.class, 1, 1, null));

        assertUnmodifiable(metroCapital);
        assertEquals     ("Metropolis and capital", metroCapital.getName().toString());
        assertArrayEquals(new Object[] {metropolis, capital}, metroCapital.getSuperTypes().toArray());
        assertFalse      (   metroCapital.isAbstract());
        assertFalse      (   metroCapital.isSparse());
        assertTrue       (   metroCapital.isSimple());
        assertEquals     (6, metroCapital.indices().size());

        assertPropertiesEquals(metroCapital, false, "country");
        assertPropertiesEquals(metroCapital, true, "city", "population", "region", "isGlobal", "parliament", "country");
        assertEquals(CharSequence.class,
                assertInstanceOf(DefaultAttributeType.class, metroCapital.getProperty("region")).getValueClass());

        // Check based only on name.
        assertTrue (DefaultFeatureType.maybeAssignableFrom(capital, metroCapital));
        assertFalse(DefaultFeatureType.maybeAssignableFrom(metroCapital, capital));
        assertTrue (DefaultFeatureType.maybeAssignableFrom(metropolis, metroCapital));
        assertFalse(DefaultFeatureType.maybeAssignableFrom(metroCapital, metropolis));

        // Public API.
        assertTrue (capital.isAssignableFrom(metroCapital));
        assertFalse(metroCapital.isAssignableFrom(capital));
        assertTrue (metropolis.isAssignableFrom(metroCapital));
        assertFalse(metroCapital.isAssignableFrom(metropolis));
    }

    /**
     * Tests inheritance with a property that override another property with a more specific type.
     */
    @Test
    public void testPropertyOverride() {
        final DefaultFeatureType metropolis     = metropolis();
        final DefaultFeatureType universityCity = universityCity();
        final DefaultAttributeType<?> temperature = CharacteristicTypeMapTest.temperature();

        var e = assertThrows(IllegalArgumentException.class,
                () -> worldMetropolis(metropolis, universityCity, temperature, Integer.class),
                "Shall not be allowed to override a 'CharSequence' attribute with an 'Integer' one.");
        assertMessageContains(e, "region" ,"Metropolis");

        final DefaultFeatureType worldMetropolis = worldMetropolis(metropolis, universityCity, temperature, InternationalString.class);
        assertUnmodifiable(worldMetropolis);
        assertEquals     ("World metropolis", worldMetropolis.getName().toString());
        assertArrayEquals(new Object[] {metropolis, universityCity}, worldMetropolis.getSuperTypes().toArray());
        assertFalse      (   worldMetropolis.isAbstract());
        assertFalse      (   worldMetropolis.isSparse());
        assertFalse      (   worldMetropolis.isSimple());           // Because of the arbitrary number of universities.
        assertEquals     (6, worldMetropolis.indices().size());

        assertPropertiesEquals(worldMetropolis, false, "region", "temperature");
        assertPropertiesEquals(worldMetropolis, true, "city", "population", "region", "isGlobal", "universities", "temperature");
        assertEquals(InternationalString.class,
                assertInstanceOf(DefaultAttributeType.class, worldMetropolis.getProperty("region")).getValueClass());

        // Check based only on name.
        assertTrue (DefaultFeatureType.maybeAssignableFrom(metropolis, worldMetropolis));
        assertFalse(DefaultFeatureType.maybeAssignableFrom(worldMetropolis, metropolis));

        // Public API.
        assertTrue (metropolis.isAssignableFrom(worldMetropolis));
        assertFalse(worldMetropolis.isAssignableFrom(metropolis));
    }

    /**
     * Tests the ommission of a property that duplicate a property already declared in the parent.
     * This is a little bit different than {@link #testPropertyOverride()} since the duplicated property
     * should be completely omitted.
     */
    @Test
    public void testPropertyDuplication() {
        DefaultFeatureType city = city();
        city = new DefaultFeatureType(name("New-City"),
                false, new DefaultFeatureType[] {city()}, city.getProperty("city"));

        assertPropertiesEquals(city, false);
        assertPropertiesEquals(city, true, "city", "population");
    }

    /**
     * Tests {@link DefaultFeatureType#equals(Object)}.
     */
    @Test
    public void testEquals() {
        final DefaultFeatureType city = city();
        assertTrue (city.equals(city()));
        assertFalse(city.equals(capital()));
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        assertPropertiesEquals(assertSerializedEquals(capital()), true, "city", "population", "parliament");
    }
}
