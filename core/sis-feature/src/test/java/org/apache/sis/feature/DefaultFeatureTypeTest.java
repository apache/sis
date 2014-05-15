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
import org.opengis.util.InternationalString;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static java.util.Collections.singletonMap;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests {@link DefaultFeatureType}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn(DefaultAttributeTypeTest.class)
public final strictfp class DefaultFeatureTypeTest extends TestCase {
    /**
     * Creates a simple feature type without super-types.
     * The feature contains the following attribute:
     *
     * <ul>
     *   <li>{@code city}       as a  {@link String}  (mandatory)</li>
     *   <li>{@code population} as an {@link Integer} (mandatory)</li>
     * </ul>
     *
     * @return The feature for a city.
     */
    public static DefaultFeatureType city() {
        final Map<String,Object> identification = new HashMap<>();
        final DefaultAttributeType<String>  city       = DefaultAttributeTypeTest.city(identification);
        final DefaultAttributeType<Integer> population = DefaultAttributeTypeTest.population(identification);

        identification.clear();
        assertNull(identification.put(DefaultFeatureType.NAME_KEY, "City"));
        return new DefaultFeatureType(identification, false, null, city, population);
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
     * @return The feature for a capital.
     */
    public static DefaultFeatureType capital() {
        return new DefaultFeatureType(singletonMap(DefaultFeatureType.NAME_KEY, "Capital"), false,
                new DefaultFeatureType[] {city()},
                new DefaultAttributeType<>(singletonMap(DefaultAttributeType.NAME_KEY, "parliament"),
                        String.class, 1, 1, null));
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
     * @return The feature for a metropolis.
     */
    public static DefaultFeatureType metropolis() {
        final Map<String,Object> identification = new HashMap<>(4);
        assertNull(identification.put(DefaultFeatureType.NAME_KEY,         "Metropolis"));
        assertNull(identification.put(DefaultFeatureType.NAME_KEY + "_fr", "Métropole"));
        return new DefaultFeatureType(identification, false,
                new DefaultFeatureType[] {city()},
                new DefaultAttributeType<>(singletonMap(DefaultAttributeType.NAME_KEY, "region"),
                        CharSequence.class, 1, 1, null),
                new DefaultAttributeType<>(singletonMap(DefaultAttributeType.NAME_KEY, "isGlobal"),
                        Boolean.class, 1, 1, null));
    }

    /**
     * Creates a sub-type of the "metropolis" type with the "region" attribute overridden to the given type.
     * The given type should be {@link InternationalString}, but we allow other type for testing argument checks.
     */
    private static DefaultFeatureType worldMetropolis(final DefaultFeatureType metropolis, final Class<?> regionType) {
        return new DefaultFeatureType(singletonMap(DefaultFeatureType.NAME_KEY, "World metropolis"), false,
                new DefaultFeatureType[] {metropolis},
                new DefaultAttributeType<>(singletonMap(DefaultAttributeType.NAME_KEY, "region"),
                        regionType, 1, 1, null));

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
     * @param feature The feature to verify.
     * @param includeSuperTypes {@code true} for including the properties inherited from the super-types,
     *        or {@code false} for returning only the properties defined explicitely in the feature type.
     * @param expected Names of the expected properties.
     */
    private static void assertPropertiesEquals(final DefaultFeatureType feature, final boolean includeSuperTypes,
            final String... expected)
    {
        int index = 0;
        for (final AbstractIdentifiedType property : feature.getProperties(includeSuperTypes)) {
            assertTrue("Found more properties than expected.", index < expected.length);
            final String name = expected[index++];
            assertNotNull(name, property);
            assertEquals (name, property.getName().toString());
            assertSame   (name, property, feature.getProperty(name));
        }
        assertEquals("Unexpected number of properties.", expected.length, index);
        assertNull("Shall not found a non-existent property.", feature.getProperty("apple"));
    }

    /**
     * Tests the construction of a simple feature without super-types.
     * A feature is said "simple" if the cardinality of all attributes is [1 … 1].
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
        assertTrue  ("isSimple",         simple.isSimple());
        assertTrue  ("isAssignableFrom", simple.isAssignableFrom(simple));
        assertEquals("instanceSize", 2,  simple.getInstanceSize());
        assertPropertiesEquals(simple, false, "city", "population");
    }

    /**
     * Tests the construction of a "complex" feature without super-types.
     * A feature is said "complex" if it contains at least one attribute
     * with a cardinality different than [0 … 0] and [1 … 1].
     */
    @Test
    @DependsOnMethod("testSimple")
    public void testComplex() {
        final Map<String,Object> identification = new HashMap<>();
        final DefaultAttributeType<String>  city       = DefaultAttributeTypeTest.city(identification);
        final DefaultAttributeType<Integer> population = DefaultAttributeTypeTest.population(identification);
        testComplex(city, population, 0, 0); // Simple
        testComplex(city, population, 0, 1);
        testComplex(city, population, 0, 2);
        testComplex(city, population, 1, 2);
        testComplex(city, population, 1, 1); // Simple
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
                singletonMap(DefaultAttributeType.NAME_KEY, "festival"),
                String.class, minimumOccurs, maximumOccurs, null);

        final DefaultFeatureType complex = new DefaultFeatureType(
                singletonMap(DefaultAttributeType.NAME_KEY, "Festival"),
                false, null, city, population, festival);

        assertUnmodifiable(complex);
        final Collection<AbstractIdentifiedType> properties = complex.getProperties(false);
        final Iterator<AbstractIdentifiedType> it = properties.iterator();

        assertEquals("name",            "Festival",                     complex.getName().toString());
        assertTrue  ("superTypes",                                      complex.getSuperTypes().isEmpty());
        assertTrue  ("isAssignableFrom",                                complex.isAssignableFrom(complex));
        assertFalse ("isAbstract",                                      complex.isAbstract());
        assertEquals("isSimple",        maximumOccurs == minimumOccurs, complex.isSimple());
        assertEquals("instanceSize",    maximumOccurs == 0 ? 2 : 3,     complex.getInstanceSize());
        assertEquals("minimumOccurs",   minimumOccurs,                  festival.getMinimumOccurs());
        assertEquals("maximumOccurs",   maximumOccurs,                  festival.getMaximumOccurs());
        assertEquals("properties.size", 3,                              properties.size());
        assertSame  ("properties[0]",   city,                           it.next());
        assertSame  ("properties[1]",   population,                     it.next());
        assertSame  ("properties[2]",   festival,                       it.next());
        assertFalse (it.hasNext());
    }

    /**
     * Ensures that we can not use two properties with the same name.
     */
    @Test
    @DependsOnMethod("testSimple")
    public void testNameCollision() {
        final DefaultAttributeType<String> city = new DefaultAttributeType<>(
                singletonMap(DefaultAttributeType.NAME_KEY, "name"), String.class, 1, 1, null);
        final DefaultAttributeType<Integer> cityId = new DefaultAttributeType<>(
                singletonMap(DefaultAttributeType.NAME_KEY, "name"), Integer.class, 1, 1, null);
        final DefaultAttributeType<Integer> population = new DefaultAttributeType<>(
                singletonMap(DefaultAttributeType.NAME_KEY, "population"), Integer.class, 1, 1, null);

        final Map<String,String> identification = singletonMap(DefaultAttributeType.NAME_KEY, "City");
        try {
            new DefaultFeatureType(identification, false, null, city, population, cityId);
            fail("Duplicated attribute names shall not be allowed.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("name")); // Property name.
            assertTrue(message, message.contains("City")); // Feature name.
        }
    }

    /**
     * Tests a feature type which inherit from an other feature type, but without property overriding.
     *
     * <p>Current implementation performs its tests on the {@link #capital()} feature.</p>
     */
    @Test
    @DependsOnMethod({"testComplex", "testEquals"})
    public void testInheritance() {
        final DefaultFeatureType city    = city(); // Tested by 'testSimple()'.
        final DefaultFeatureType capital = capital();
        assertUnmodifiable(capital);
        assertEquals("name", "Capital", capital.getName().toString());
        assertEquals("superTypes",      city, getSingleton(capital.getSuperTypes()));
        assertFalse ("isAbstract",      capital.isAbstract());
        assertTrue  ("isSimple",        capital.isSimple());
        assertEquals("instanceSize", 3, capital.getInstanceSize());

        assertPropertiesEquals(city,    false, "city", "population");
        assertPropertiesEquals(capital, false, "parliament");
        assertPropertiesEquals(capital, true,  "city", "population", "parliament");

        // Check based only on name.
        assertTrue ("maybeAssignableFrom", city.maybeAssignableFrom(capital));
        assertFalse("maybeAssignableFrom", capital.maybeAssignableFrom(city));

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
        final DefaultFeatureType capital      = capital(); // Tested by 'testComplex()'.
        final DefaultFeatureType metroCapital = new DefaultFeatureType(
                singletonMap(DefaultFeatureType.NAME_KEY, "Metropolis and capital"), false,
                new DefaultFeatureType[] {metropolis, capital},
                new DefaultAttributeType<>(singletonMap(DefaultAttributeType.NAME_KEY, "country"),
                        String.class, 1, 1, null));

        assertUnmodifiable(metroCapital);
        assertEquals     ("name", "Metropolis and capital", metroCapital.getName().toString());
        assertArrayEquals("superTypes", new Object[] {metropolis, capital}, metroCapital.getSuperTypes().toArray());
        assertFalse      ("isAbstract",      metroCapital.isAbstract());
        assertTrue       ("isSimple",        metroCapital.isSimple());
        assertEquals     ("instanceSize", 6, metroCapital.getInstanceSize());

        assertPropertiesEquals(metroCapital, false, "country");
        assertPropertiesEquals(metroCapital, true, "city", "population", "region", "isGlobal", "parliament", "country");
        assertEquals("property(“region”).valueClass", CharSequence.class,
                ((DefaultAttributeType) metroCapital.getProperty("region")).getValueClass());

        // Check based only on name.
        assertTrue ("maybeAssignableFrom", capital.maybeAssignableFrom(metroCapital));
        assertFalse("maybeAssignableFrom", metroCapital.maybeAssignableFrom(capital));
        assertTrue ("maybeAssignableFrom", metropolis.maybeAssignableFrom(metroCapital));
        assertFalse("maybeAssignableFrom", metroCapital.maybeAssignableFrom(metropolis));

        // Public API.
        assertTrue ("isAssignableFrom", capital.isAssignableFrom(metroCapital));
        assertFalse("isAssignableFrom", metroCapital.isAssignableFrom(capital));
        assertTrue ("isAssignableFrom", metropolis.isAssignableFrom(metroCapital));
        assertFalse("isAssignableFrom", metroCapital.isAssignableFrom(metropolis));
    }

    /**
     * Tests inheritance with a property that override an other property with a more specific type.
     */
    @Test
    @DependsOnMethod({"testMultiInheritance", "testNameCollision"})
    public void testPropertyOverride() {
        final DefaultFeatureType metropolis = metropolis();
        try {
            worldMetropolis(metropolis, Integer.class);
            fail("Shall not be allowed to override a 'CharSequence' attribute with an 'Integer' one.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("region"));
            assertTrue(message, message.contains("Metropolis"));
        }
        final DefaultFeatureType worldMetropolis = worldMetropolis(metropolis, InternationalString.class);
        assertUnmodifiable(worldMetropolis);
        assertEquals     ("name", "World metropolis", worldMetropolis.getName().toString());
        assertArrayEquals("superTypes", new Object[] {metropolis}, worldMetropolis.getSuperTypes().toArray());
        assertFalse      ("isAbstract",      worldMetropolis.isAbstract());
        assertTrue       ("isSimple",        worldMetropolis.isSimple());
        assertEquals     ("instanceSize", 4, worldMetropolis.getInstanceSize());

        assertPropertiesEquals(worldMetropolis, false, "region");
        assertPropertiesEquals(worldMetropolis, true, "city", "population", "region", "isGlobal");
        assertEquals("property(“region”).valueClass", InternationalString.class,
                ((DefaultAttributeType) worldMetropolis.getProperty("region")).getValueClass());

        // Check based only on name.
        assertTrue ("maybeAssignableFrom", metropolis.maybeAssignableFrom(worldMetropolis));
        assertFalse("maybeAssignableFrom", worldMetropolis.maybeAssignableFrom(metropolis));

        // Public API.
        assertTrue ("isAssignableFrom", metropolis.isAssignableFrom(worldMetropolis));
        assertFalse("isAssignableFrom", worldMetropolis.isAssignableFrom(metropolis));
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
