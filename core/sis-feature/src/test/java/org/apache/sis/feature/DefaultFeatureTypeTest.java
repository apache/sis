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
import java.util.Iterator;
import java.util.Collection;
import org.opengis.util.NameFactory;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import static org.apache.sis.test.TestUtilities.getSingleton;

// Branch-dependent imports
import org.opengis.feature.PropertyType;
import org.opengis.feature.AttributeType;


/**
 * Tests {@link DefaultFeatureType}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.5
 */
@DependsOn(DefaultAttributeTypeTest.class)
public final class DefaultFeatureTypeTest extends TestCase {
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
        final Map<String,Object> identification = new HashMap<>();
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
        final Map<String,Object> identification = new HashMap<>(4);
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
        if (!superTypes.isEmpty()) try {
            superTypes.clear();
            fail("Super-types collection shall not be modifiable.");
        } catch (UnsupportedOperationException e) {
            assertFalse(superTypes.isEmpty());
        }
        if (!declaredProperties.isEmpty()) try {
            declaredProperties.clear();
            fail("Properties collection shall not be modifiable.");
        } catch (UnsupportedOperationException e) {
            assertFalse(declaredProperties.isEmpty());
        }
        if (!allProperties.isEmpty()) try {
            allProperties.clear();
            fail("Properties collection shall not be modifiable.");
        } catch (UnsupportedOperationException e) {
            assertFalse(allProperties.isEmpty());
        }
        // Opportunist check.
        assertTrue("'properties(true)' shall contain all 'properties(false)' elements.",
                allProperties.containsAll(declaredProperties));
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
        for (final PropertyType property : feature.getProperties(includeSuperTypes)) {
            assertTrue("Found more properties than expected.", index < expected.length);
            final String name = expected[index++];
            assertNotNull(name, property);
            assertEquals (name, property.getName().toString());
            assertSame   (name, property, feature.getProperty(name));
        }
        assertEquals("Unexpected number of properties.", expected.length, index);
        try {
            feature.getProperty("apple");
            fail("Shall not found a non-existent property.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("apple"));
            assertTrue(message, message.contains(feature.getName().toString()));
        }
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
        assertEquals("name", "City",     simple.getName().toString());
        assertTrue  ("superTypes",       simple.getSuperTypes().isEmpty());
        assertFalse ("isAbstract",       simple.isAbstract());
        assertFalse ("isSparse",         simple.isSparse());
        assertTrue  ("isSimple",         simple.isSimple());
        assertTrue  ("isAssignableFrom", simple.isAssignableFrom(simple));
        assertEquals("instanceSize", 2,  simple.indices().size());
        assertPropertiesEquals(simple, false, "city", "population");
    }

    /**
     * Tests the construction of a "complex" feature without super-types.
     * A feature is said "complex" if it contains at least one attribute
     * with a multiplicity different than [0 … 0] and [1 … 1].
     */
    @Test
    @DependsOnMethod("testSimple")
    public void testComplex() {
        final Map<String,Object> identification = new HashMap<>();
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
        final Collection<PropertyType> properties = complex.getProperties(false);
        final Iterator<PropertyType> it = properties.iterator();

        assertEquals("name",            "Festival",                     complex.getName().toString());
        assertTrue  ("superTypes",                                      complex.getSuperTypes().isEmpty());
        assertTrue  ("isAssignableFrom",                                complex.isAssignableFrom(complex));
        assertFalse ("isAbstract",                                      complex.isAbstract());
        assertFalse ("isSparse",                                        complex.isSparse());
        assertEquals("isSimple",        maximumOccurs == minimumOccurs, complex.isSimple());
        assertEquals("instanceSize",    maximumOccurs == 0 ? 2 : 3,     complex.indices().size());
        assertEquals("minimumOccurs",   minimumOccurs,                  festival.getMinimumOccurs());
        assertEquals("maximumOccurs",   maximumOccurs,                  festival.getMaximumOccurs());
        assertEquals("properties.size", 3,                              properties.size());
        assertSame  ("properties[0]",   city,                           it.next());
        assertSame  ("properties[1]",   population,                     it.next());
        assertSame  ("properties[2]",   festival,                       it.next());
        assertFalse (it.hasNext());
    }

    /**
     * Ensures that we cannot use two properties with the same name.
     */
    @Test
    @DependsOnMethod("testSimple")
    public void testNameCollision() {
        final DefaultAttributeType<String>  city       = new DefaultAttributeType<>(name("name"),       String.class,  1, 1, null);
        final DefaultAttributeType<Integer> cityId     = new DefaultAttributeType<>(name("name"),       Integer.class, 1, 1, null);
        final DefaultAttributeType<Integer> population = new DefaultAttributeType<>(name("population"), Integer.class, 1, 1, null);

        try {
            final Object t = new DefaultFeatureType(name("City"), false, null, city, population, cityId);
            fail("Duplicated attribute names shall not be allowed:\n" + t);
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("name"));      // Property name.
            assertTrue(message, message.contains("City"));      // Feature name.
        }
    }

    /**
     * Same than {@link #testNameCollision()}, but resolving collisions with usage of names
     * of the form {@code "head:tip"}.
     *
     * @since 0.6
     */
    @Test
    @DependsOnMethod("testNameCollision")
    public void testQualifiedNames() {
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        final DefaultAttributeType<String> city = new DefaultAttributeType<>(
                name(factory.createGenericName(null, "ns1", "name")),
                String.class, 1, 1, null);
        final DefaultAttributeType<Integer> cityId = new DefaultAttributeType<>(
                name(factory.createGenericName(null, "ns2", "name")),
                Integer.class, 1, 1, null);
        final DefaultAttributeType<Integer> population = new DefaultAttributeType<>(
                name(factory.createGenericName(null, "ns1", "population")),
                Integer.class, 1, 1, null);
        final DefaultFeatureType feature = new DefaultFeatureType(
                name("City"), false, null, city, cityId, population);

        final Iterator<PropertyType> it = feature.getProperties(false).iterator();
        assertSame ("properties[0]", city,       it.next());
        assertSame ("properties[1]", cityId,     it.next());
        assertSame ("properties[2]", population, it.next());
        assertFalse(it.hasNext());

        assertSame("Shall get from fully qualified name.", city,       feature.getProperty("ns1:name"));
        assertSame("Shall get from fully qualified name.", cityId,     feature.getProperty("ns2:name"));
        assertSame("Shall get from fully qualified name.", population, feature.getProperty("ns1:population"));
        assertSame("Shall get from short alias.",          population, feature.getProperty(    "population"));
        try {
            feature.getProperty("name");
            fail("Expected no alias because of ambiguity.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("name"));      // Property name.
            assertTrue(message, message.contains("ns1:name"));  // Ambiguity 1.
            assertTrue(message, message.contains("ns2:name"));  // Ambiguity 2.
        }
        try {
            feature.getProperty("other");
            fail("Expected no property.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("other"));     // Property name.
            assertTrue(message, message.contains("City"));      // Feature name.
        }
    }

    /**
     * Tests two names having the same tip, but where only one of the two names have a namespace.
     *
     * @since 0.8
     */
    @Test
    @DependsOnMethod("testQualifiedNames")
    public void testQualifiedAndUnqualifiedNames() {
        final NameFactory factory = DefaultFactories.forBuildin(NameFactory.class);
        final DefaultAttributeType<String> a1 = new DefaultAttributeType<>(
                name(factory.createGenericName(null, "sis", "identifier")),
                String.class, 1, 1, null);
        final DefaultAttributeType<String> a2 = new DefaultAttributeType<>(
                name(factory.createGenericName(null, "identifier")),
                String.class, 1, 1, null);
        final DefaultFeatureType feature = new DefaultFeatureType(
                name("City"), false, null, a1, a2);

        assertSame("sis:identifier", a1, feature.getProperty("sis:identifier"));
        assertSame(    "identifier", a2, feature.getProperty(    "identifier"));
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
        final PropertyType parliament = new LinkOperation(identifierName, DefaultAttributeTypeTest.parliament());
        try {
            final DefaultFeatureType illegal = new DefaultFeatureType(featureName, false, parent, parliament);
            fail("Should not have been allowed to create this feature:\n" + illegal);
        } catch (IllegalArgumentException e) {
            final String message = e.getLocalizedMessage();
            assertTrue(message, message.contains("identifier"));
            assertTrue(message, message.contains("parliament"));
            assertTrue(message, message.contains("Identified city"));
        }
    }

    /**
     * Tests a feature type which inherit from another feature type, but without property overriding.
     *
     * <p>Current implementation performs its tests on the {@link #capital()} feature.</p>
     */
    @Test
    @DependsOnMethod({"testComplex", "testEquals"})
    public void testInheritance() {
        final DefaultFeatureType city    = city();      // Tested by 'testSimple()'.
        final DefaultFeatureType capital = capital();
        assertUnmodifiable(capital);
        assertEquals("name", "Capital", capital.getName().toString());
        assertEquals("superTypes",      city, getSingleton(capital.getSuperTypes()));
        assertFalse ("isAbstract",      capital.isAbstract());
        assertFalse ("isSparse",        capital.isSparse());
        assertTrue  ("isSimple",        capital.isSimple());
        assertEquals("instanceSize", 3, capital.indices().size());

        assertPropertiesEquals(city,    false, "city", "population");
        assertPropertiesEquals(capital, false, "parliament");
        assertPropertiesEquals(capital, true,  "city", "population", "parliament");

        // Check based only on name.
        assertTrue ("maybeAssignableFrom", DefaultFeatureType.maybeAssignableFrom(city, capital));
        assertFalse("maybeAssignableFrom", DefaultFeatureType.maybeAssignableFrom(capital, city));

        // Public API.
        assertTrue ("isAssignableFrom", city.isAssignableFrom(capital));
        assertFalse("isAssignableFrom", capital.isAssignableFrom(city));
    }

    /**
     * Tests the inheritance of 2 types having the same common parent.
     */
    @Test
    @DependsOnMethod("testInheritance")
    public void testMultiInheritance() {
        final DefaultFeatureType metropolis   = metropolis();
        final DefaultFeatureType capital      = capital();      // Tested by 'testComplex()'.
        final DefaultFeatureType metroCapital = new DefaultFeatureType(
                name("Metropolis and capital"), false,
                new DefaultFeatureType[] {metropolis, capital},
                new DefaultAttributeType<>(name("country"),
                        String.class, 1, 1, null));

        assertUnmodifiable(metroCapital);
        assertEquals     ("name", "Metropolis and capital", metroCapital.getName().toString());
        assertArrayEquals("superTypes", new Object[] {metropolis, capital}, metroCapital.getSuperTypes().toArray());
        assertFalse      ("isAbstract",      metroCapital.isAbstract());
        assertFalse      ("isSparse",        metroCapital.isSparse());
        assertTrue       ("isSimple",        metroCapital.isSimple());
        assertEquals     ("instanceSize", 6, metroCapital.indices().size());

        assertPropertiesEquals(metroCapital, false, "country");
        assertPropertiesEquals(metroCapital, true, "city", "population", "region", "isGlobal", "parliament", "country");
        assertEquals("property(“region”).valueClass", CharSequence.class,
                ((AttributeType<?>) metroCapital.getProperty("region")).getValueClass());

        // Check based only on name.
        assertTrue ("maybeAssignableFrom", DefaultFeatureType.maybeAssignableFrom(capital, metroCapital));
        assertFalse("maybeAssignableFrom", DefaultFeatureType.maybeAssignableFrom(metroCapital, capital));
        assertTrue ("maybeAssignableFrom", DefaultFeatureType.maybeAssignableFrom(metropolis, metroCapital));
        assertFalse("maybeAssignableFrom", DefaultFeatureType.maybeAssignableFrom(metroCapital, metropolis));

        // Public API.
        assertTrue ("isAssignableFrom", capital.isAssignableFrom(metroCapital));
        assertFalse("isAssignableFrom", metroCapital.isAssignableFrom(capital));
        assertTrue ("isAssignableFrom", metropolis.isAssignableFrom(metroCapital));
        assertFalse("isAssignableFrom", metroCapital.isAssignableFrom(metropolis));
    }

    /**
     * Tests inheritance with a property that override another property with a more specific type.
     */
    @Test
    @DependsOnMethod({"testMultiInheritance", "testNameCollision"})
    public void testPropertyOverride() {
        final DefaultFeatureType metropolis     = metropolis();
        final DefaultFeatureType universityCity = universityCity();
        final DefaultAttributeType<?> temperature = CharacteristicTypeMapTest.temperature();
        try {
            worldMetropolis(metropolis, universityCity, temperature, Integer.class);
            fail("Shall not be allowed to override a 'CharSequence' attribute with an 'Integer' one.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("region"));
            assertTrue(message, message.contains("Metropolis"));
        }
        final DefaultFeatureType worldMetropolis = worldMetropolis(metropolis, universityCity, temperature, InternationalString.class);
        assertUnmodifiable(worldMetropolis);
        assertEquals     ("name", "World metropolis", worldMetropolis.getName().toString());
        assertArrayEquals("superTypes", new Object[] {metropolis, universityCity}, worldMetropolis.getSuperTypes().toArray());
        assertFalse      ("isAbstract",      worldMetropolis.isAbstract());
        assertFalse      ("isSparse",        worldMetropolis.isSparse());
        assertFalse      ("isSimple",        worldMetropolis.isSimple());           // Because of the arbitrary number of universities.
        assertEquals     ("instanceSize", 6, worldMetropolis.indices().size());

        assertPropertiesEquals(worldMetropolis, false, "region", "temperature");
        assertPropertiesEquals(worldMetropolis, true, "city", "population", "region", "isGlobal", "universities", "temperature");
        assertEquals("property(“region”).valueClass", InternationalString.class,
                ((AttributeType<?>) worldMetropolis.getProperty("region")).getValueClass());

        // Check based only on name.
        assertTrue ("maybeAssignableFrom", DefaultFeatureType.maybeAssignableFrom(metropolis, worldMetropolis));
        assertFalse("maybeAssignableFrom", DefaultFeatureType.maybeAssignableFrom(worldMetropolis, metropolis));

        // Public API.
        assertTrue ("isAssignableFrom", metropolis.isAssignableFrom(worldMetropolis));
        assertFalse("isAssignableFrom", worldMetropolis.isAssignableFrom(metropolis));
    }

    /**
     * Tests the ommission of a property that duplicate a property already declared in the parent.
     * This is a little bit different than {@link #testPropertyOverride()} since the duplicated property
     * should be completely omitted.
     */
    @Test
    @DependsOnMethod("testPropertyOverride")
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
    @DependsOnMethod("testSimple")
    public void testEquals() {
        final DefaultFeatureType city = city();
        assertTrue (city.equals(city()));
        assertFalse(city.equals(capital()));
    }

    /**
     * Tests serialization.
     */
    @Test
    @DependsOnMethod({"testInheritance", "testEquals"})
    public void testSerialization() {
        assertPropertiesEquals(assertSerializedEquals(capital()), true, "city", "population", "parliament");
    }
}
