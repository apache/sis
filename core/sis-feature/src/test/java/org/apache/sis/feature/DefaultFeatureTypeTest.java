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
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static java.util.Collections.singletonMap;


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
     *   <li>{@code city}       as a  {@link String}  (mandatory)</li>
     *   <li>{@code population} as an {@link Integer} (mandatory)</li>
     *   <li>{@code region}     as a  {@link String}  (mandatory) — the region for which the city is a metropolis.</li>
     *   <li>{@code isGlobal}   as a  {@link Boolean} (mandatory) — whether the city has an effect on global affairs.</li>
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
                        String.class, 1, 1, null),
                new DefaultAttributeType<>(singletonMap(DefaultAttributeType.NAME_KEY, "isGlobal"),
                        Boolean.class, 1, 1, null));
    }

    /**
     * Returns the string representation of the names of all properties in the given collection.
     * This method is used with {@code assertArrayEquals(…)} for verifying the collection of feature properties.
     */
    private static String[] getNames(final Collection<? extends AbstractIdentifiedType> properties) {
        final String[] names = new String[properties.size()];
        int index = 0;
        for (final AbstractIdentifiedType property : properties) {
            assertNotNull(properties);
            names[index++] = property.getName().toString();
        }
        assertEquals(names.length, index);
        return names;
    }

    /**
     * Performs some basic validations on the given feature.
     * This method does <strong>not</strong> validate recursively the properties.
     */
    private static void validate(final DefaultFeatureType feature) {
        final Collection<?> explicitProperties = feature.properties(false);
        final Collection<?> allProperties = feature.properties(true);
        assertTrue("'properties(true)' shall contain all 'properties(false)' elements.",
                allProperties.containsAll(explicitProperties));
        try {
            explicitProperties.clear();
            fail("Properties collection shall not be modifiable.");
        } catch (UnsupportedOperationException e) {
            assertFalse(explicitProperties.isEmpty());
        }
        try {
            allProperties.clear();
            fail("Properties collection shall not be modifiable.");
        } catch (UnsupportedOperationException e) {
            assertFalse(allProperties.isEmpty());
        }
    }

    /**
     * Tests the construction of a simple feature without super-types.
     * A feature is said "simple" if the cardinality of all attributes is [1 … 1].
     */
    @Test
    public void testSimple() {
        final DefaultFeatureType simple = city();
        assertEquals("name", "City",    simple.getName().toString());
        assertEquals("instanceSize", 2, simple.getInstanceSize());
        assertFalse ("isAbstract",      simple.isAbstract());
        assertTrue  ("isSimple",        simple.isSimple());
        validate(simple);
        /*
         * Verify content.
         */
        assertArrayEquals("properties",
                new String[] {"city", "population"},
                getNames(simple.properties(false)));
        /*
         * Verify search by name.
         */
        final Iterator<AbstractIdentifiedType> it = simple.properties(false).iterator();
        assertSame(it.next(), simple.getProperty("city"));
        assertSame(it.next(), simple.getProperty("population"));
        assertNull(           simple.getProperty("apple"));
        assertFalse(it.hasNext());
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

        validate(complex);
        final Collection<AbstractIdentifiedType> properties = complex.properties(false);
        final Iterator<AbstractIdentifiedType> it = properties.iterator();

        assertEquals("name",            "Festival",                     complex.getName().toString());
        assertFalse ("isAbstract",                                      complex.isAbstract());
        assertEquals("isSimple",        maximumOccurs == minimumOccurs, complex.isSimple());
        assertEquals("instanceSize",    maximumOccurs == 0 ? 2 : 3,     complex.getInstanceSize());
        assertEquals("minimumOccurs",   minimumOccurs,                  festival.getMinimumOccurs());
        assertEquals("maximumOccurs",   maximumOccurs,                  festival.getMaximumOccurs());
        assertEquals("properties.size", 3,                              properties.size());
        assertSame  ("properties[0]",   city,                           it.next());
        assertSame  ("properties[1]",   population,                     it.next());
        assertSame  ("properties[3]",   festival,                       it.next());
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
     */
    @Test
    @DependsOnMethod("testComplex")
    public void testInheritance() {
        final DefaultFeatureType capital = capital();
        final DefaultFeatureType city    = city();
        validate(capital);

        // Check based only on name.
        assertTrue ("maybeAssignableFrom", city.maybeAssignableFrom(capital));
        assertFalse("maybeAssignableFrom", capital.maybeAssignableFrom(city));

        // Public API.
        assertTrue ("isAssignableFrom", city.isAssignableFrom(capital));
        assertFalse("isAssignableFrom", capital.isAssignableFrom(city));

        assertArrayEquals("properties",
                new String[] {"city", "population"},
                getNames(city.properties(false)));

        assertArrayEquals("properties",
                new String[] {"parliament"},
                getNames(capital.properties(false)));

        assertArrayEquals("properties",
                new String[] {"city", "population", "parliament"},
                getNames(capital.properties(true)));

        testGetPropertiesOfCapital(capital);
    }

    /**
     * Verifies the content of a feature created by {@link #capital()}.
     * This is a partial implementation of {@link #testInheritance()},
     * also shared by {@link #testSerialization()}.
     */
    private static void testGetPropertiesOfCapital(final DefaultFeatureType capital) {
        assertEquals("city",       capital.getProperty("city")      .getName().toString());
        assertEquals("population", capital.getProperty("population").getName().toString());
        assertEquals("parliament", capital.getProperty("parliament").getName().toString());
        assertNull  (              capital.getProperty("apple"));
    }

    /**
     * Tests the inheritance of 2 types having the same common parent.
     */
    @Test
    @DependsOnMethod("testInheritance")
    public void testMultiInheritance() {
        final DefaultFeatureType capital = new DefaultFeatureType(
                singletonMap(DefaultFeatureType.NAME_KEY, "Metropolis and capital"), false,
                new DefaultFeatureType[] {metropolis(), capital()},
                new DefaultAttributeType<>(singletonMap(DefaultAttributeType.NAME_KEY, "country"),
                        String.class, 1, 1, null));

        validate(capital);
        assertArrayEquals("properties",
                new String[] {"country"},
                getNames(capital.properties(false)));
        assertArrayEquals("properties",
                new String[] {"city", "population", "region", "isGlobal", "parliament", "country"},
                getNames(capital.properties(true)));

        testGetPropertiesOfCapital(capital);
        assertEquals("country",  capital.getProperty("country") .getName().toString());
        assertEquals("region",   capital.getProperty("region")  .getName().toString());
        assertEquals("isGlobal", capital.getProperty("isGlobal").getName().toString());
    }

    /**
     * Tests serialization.
     */
    @Test
    @DependsOnMethod("testInheritance")
    public void testSerialization() {
        testGetPropertiesOfCapital(assertSerializedEquals(capital()));
    }
}
