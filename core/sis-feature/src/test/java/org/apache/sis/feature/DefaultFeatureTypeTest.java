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
import org.apache.sis.measure.NumberRange;
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
    static DefaultFeatureType simple() {
        final Map<String,Object> properties = new HashMap<>();
        final DefaultAttributeType<String> city = DefaultAttributeTypeTest.city(properties);

        properties.clear();
        assertNull(properties.put(DefaultAttributeType.NAME_KEY, "population"));
        final DefaultAttributeType<Integer> population = new DefaultAttributeType<>(
                properties, Integer.class, null, NumberRange.create(1, true, 1, true));

        properties.clear();
        assertNull(properties.put(DefaultAttributeType.NAME_KEY, "City population"));
        return new DefaultFeatureType(properties, false, null, city, population);
    }

    /**
     * Tests the construction of a simple feature without super-types.
     */
    @Test
    public void testSimple() {
        final DefaultFeatureType simple = simple();
        assertEquals("name", "City population", simple.getName().toString());
        /*
         * Verify content.
         */
        final List<DefaultAttributeType<?>> characteristics = simple.getCharacteristics();
        assertEquals("characteristics.size", 2, characteristics.size());
        assertEquals("characteristics[0]", "city",       characteristics.get(0).getName().toString());
        assertEquals("characteristics[1]", "population", characteristics.get(1).getName().toString());
        /*
         * Verify search by name.
         */
        assertSame(characteristics.get(0), simple.getProperty("city"));
        assertSame(characteristics.get(1), simple.getProperty("population"));
        assertNull(simple.getProperty("apple"));
    }

    /**
     * Ensures that we can not use two property with the same name.
     */
    @Test
    @DependsOnMethod("testSimple")
    public void testNameCollision() {
        final NumberRange<Integer> cardinality = NumberRange.create(1, true, 1, true);
        final DefaultAttributeType<String> city = new DefaultAttributeType<>(
                singletonMap(DefaultAttributeType.NAME_KEY, "city"), String.class, null, cardinality);
        final DefaultAttributeType<Integer> cityId = new DefaultAttributeType<>(
                singletonMap(DefaultAttributeType.NAME_KEY, "city"), Integer.class, null, cardinality);
        final DefaultAttributeType<Integer> population = new DefaultAttributeType<>(
                singletonMap(DefaultAttributeType.NAME_KEY, "population"), Integer.class, null, cardinality);

        final Map<String,String> properties = singletonMap(DefaultAttributeType.NAME_KEY, "City population");
        try {
            new DefaultFeatureType(properties, false, null, city, population, cityId);
            fail("Duplicated attribute names shall not be allowed.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("city"));
        }
    }
}
