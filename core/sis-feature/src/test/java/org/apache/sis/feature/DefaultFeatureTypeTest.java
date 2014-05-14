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
import java.util.List;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
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
     */
    static DefaultFeatureType cityPopulation() {
        final Map<String,Object> identification = new HashMap<>();
        final DefaultAttributeType<String>  city       = DefaultAttributeTypeTest.city(identification);
        final DefaultAttributeType<Integer> population = DefaultAttributeTypeTest.population(identification);

        identification.clear();
        assertNull(identification.put(DefaultFeatureType.NAME_KEY, "City population"));
        return new DefaultFeatureType(identification, false, null, city, population);
    }

    /**
     * Creates a sub-type of the "city" type with only one additional property,
     * a string giving the date since the city is a capital.
     *
     * <p>We do not specify the country, since this will be the purpose of an other test class.</p>
     */
    static DefaultFeatureType capital() {
        return new DefaultFeatureType(singletonMap(DefaultFeatureType.NAME_KEY, "capital"), false,
                new DefaultFeatureType[] {cityPopulation()},
                new DefaultAttributeType<>(singletonMap(DefaultAttributeType.NAME_KEY, "since"),
                        String.class, 1, 1, null));
    }

    /**
     * Tests the construction of a simple feature without super-types.
     * A feature is said "simple" if the cardinality of all attributes is [1 … 1].
     */
    @Test
    public void testSimple() {
        final DefaultFeatureType simple = cityPopulation();
        assertEquals("name", "City population", simple.getName().toString());
        assertEquals("instanceSize", 2, simple.getInstanceSize());
        assertFalse ("isAbstract",      simple.isAbstract());
        assertTrue  ("isSimple",        simple.isSimple());
        /*
         * Verify content.
         */
        final List<AbstractIdentifiedType> properties = simple.properties();
        assertEquals("properties.size", 2,            properties.size());
        assertEquals("properties[0]",   "city",       properties.get(0).getName().toString());
        assertEquals("properties[1]",   "population", properties.get(1).getName().toString());
        /*
         * Verify search by name.
         */
        assertSame(properties.get(0), simple.getProperty("city"));
        assertSame(properties.get(1), simple.getProperty("population"));
        assertNull(                        simple.getProperty("apple"));
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
        identification.clear();
        for (int i=0; i<=4; i++) {
            final int minimumOccurs, maximumOccurs;
            switch (i) {
                case 0: minimumOccurs = 0; maximumOccurs = 0; break; // Simple
                case 1: minimumOccurs = 0; maximumOccurs = 1; break;
                case 2: minimumOccurs = 0; maximumOccurs = 2; break;
                case 3: minimumOccurs = 1; maximumOccurs = 2; break;
                case 4: minimumOccurs = 1; maximumOccurs = 1; break; // Simple
                default: throw new AssertionError(i);
            }
            identification.put(DefaultAttributeType.NAME_KEY, "festival");
            final DefaultAttributeType<String> festival = new DefaultAttributeType<>(
                    identification, String.class, minimumOccurs, maximumOccurs, null);
            /*
             * Build the feature.
             */
            identification.put(DefaultAttributeType.NAME_KEY, "City festival");
            final DefaultFeatureType complex = new DefaultFeatureType(identification, false, null, city, population, festival);
            final List<AbstractIdentifiedType> properties = complex.properties();
            /*
             * Verify content.
             */
            assertEquals("name",            "City festival",                complex.getName().toString());
            assertFalse ("isAbstract",                                      complex.isAbstract());
            assertEquals("isSimple",        maximumOccurs == minimumOccurs, complex.isSimple());
            assertEquals("instanceSize",    maximumOccurs == 0 ? 2 : 3,     complex.getInstanceSize());
            assertEquals("properties.size", 3,                              properties.size());
            assertSame  ("properties[0]",   city,                           properties.get(0));
            assertSame  ("properties[1]",   population,                     properties.get(1));
            assertSame  ("properties[3]",   festival,                       properties.get(2));
            assertEquals("minimumOccurs",   minimumOccurs,                  festival.getMinimumOccurs());
            assertEquals("maximumOccurs",   maximumOccurs,                  festival.getMaximumOccurs());
        }
    }

    /**
     * Ensures that we can not use two properties with the same name.
     */
    @Test
    @DependsOnMethod("testSimple")
    public void testNameCollision() {
        final DefaultAttributeType<String> city = new DefaultAttributeType<>(
                singletonMap(DefaultAttributeType.NAME_KEY, "city"), String.class, 1, 1, null);
        final DefaultAttributeType<Integer> cityId = new DefaultAttributeType<>(
                singletonMap(DefaultAttributeType.NAME_KEY, "city"), Integer.class, 1, 1, null);
        final DefaultAttributeType<Integer> population = new DefaultAttributeType<>(
                singletonMap(DefaultAttributeType.NAME_KEY, "population"), Integer.class, 1, 1, null);

        final Map<String,String> identification = singletonMap(DefaultAttributeType.NAME_KEY, "City population");
        try {
            new DefaultFeatureType(identification, false, null, city, population, cityId);
            fail("Duplicated attribute names shall not be allowed.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("city"));
        }
    }

    /**
     * Tests a feature type which inherit from an other feature type.
     */
    @Test
    @DependsOnMethod("testSimple")
    public void testInheritance() {
        final DefaultFeatureType capital = capital();
        final DefaultFeatureType city = cityPopulation();

        // Check based only on name.
        assertTrue ("maybeAssignableFrom", city.maybeAssignableFrom(capital));
        assertFalse("maybeAssignableFrom", capital.maybeAssignableFrom(city));

        // Public API.
        assertTrue ("isAssignableFrom", city.isAssignableFrom(capital));
        assertFalse("isAssignableFrom", capital.isAssignableFrom(city));
        /*
         * Verify content.
         */
        List<AbstractIdentifiedType> properties = city.properties();
        assertEquals("properties.size", 2,            properties.size());
        assertEquals("properties[0]",   "city",       properties.get(0).getName().toString());
        assertEquals("properties[1]",   "population", properties.get(1).getName().toString());

        properties = capital.properties();
        assertEquals("properties.size", 1,            properties.size());
        assertEquals("properties[0]",   "since",      properties.get(0).getName().toString());
        /*
         * Verify search by name.
         */
        assertEquals("city",       capital.getProperty("city")      .getName().toString());
        assertEquals("population", capital.getProperty("population").getName().toString());
        assertEquals("since",      capital.getProperty("since")     .getName().toString());
        assertNull  (              capital.getProperty("apple"));
    }
}
