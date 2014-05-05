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
import java.util.Locale;
import org.opengis.util.LocalName;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link DefaultAttributeType}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final strictfp class DefaultAttributeTypeTest extends TestCase {
    /**
     * Creates an attribute type for city name.
     *
     * @param properties An empty temporary map (provided only for recycling existing instances).
     */
    static DefaultAttributeType<String> city(final Map<String,Object> properties) {
        assertNull(properties.put(DefaultAttributeType.NAME_KEY, "city"));
        assertNull(properties.put(DefaultAttributeType.DESIGNATION_KEY + "_en", "City"));
        assertNull(properties.put(DefaultAttributeType.DESIGNATION_KEY + "_fr", "Ville"));
        assertNull(properties.put(DefaultAttributeType.DESIGNATION_KEY + "_ja", "都市"));
        assertNull(properties.put(DefaultAttributeType.DEFINITION_KEY  + "_en", "The name of the city."));
        assertNull(properties.put(DefaultAttributeType.DEFINITION_KEY  + "_fr", "Le nom de la ville."));
        assertNull(properties.put(DefaultAttributeType.DEFINITION_KEY  + "_ja", "都市の名前。"));
        assertNull(properties.put(DefaultAttributeType.DESCRIPTION_KEY, "Some verbose description."));
        final DefaultAttributeType<String> city = new DefaultAttributeType<>(properties,
                String.class, "Utopia", NumberRange.create(1, true, 1, true));
        properties.clear();
        return city;
    }

    /**
     * Creates an attribute type for city population.
     *
     * @param properties An empty temporary map (provided only for recycling existing instances).
     */
    static DefaultAttributeType<Integer> population(final Map<String,Object> properties) {
        assertNull(properties.put(DefaultAttributeType.NAME_KEY, "population"));
        final DefaultAttributeType<Integer> population = new DefaultAttributeType<>(
                properties, Integer.class, null, NumberRange.create(1, true, 1, true));
        properties.clear();
        return population;
    }

    /**
     * Tests the creation of a simple {@link DefaultAttributeType} instance for a mandatory singleton.
     */
    @Test
    public void testMandatorySingleton() {
        final DefaultAttributeType<String> city = city(new HashMap<>());
        final GenericName name = city.getName();
        assertInstanceOf("city.name", LocalName.class, name);
        assertEquals("city.name", "city", name.toString());

        InternationalString p = city.getDesignation();
        assertNotNull("designation", p);
        assertEquals("designation", "City",  p.toString(Locale.ENGLISH));
        assertEquals("designation", "Ville", p.toString(Locale.FRENCH));
        assertEquals("designation", "都市",   p.toString(Locale.JAPANESE));

        p = city.getDefinition();
        assertEquals("definition",  "The name of the city.", p.toString(Locale.ENGLISH));
        assertEquals("definition",  "Le nom de la ville.", p.toString(Locale.FRENCH));
        assertEquals("definition",  "都市の名前。", p.toString(Locale.JAPANESE));

        p = city.getDescription();
        assertEquals("description",  "Some verbose description.", p.toString(Locale.ENGLISH));
        assertEquals("valueClass",   String.class, city.getValueClass());
        assertEquals("defaultValue", "Utopia",     city.getDefaultValue());

        final NumberRange<Integer> cardinality = city.getCardinality();
        assertNotNull("cardinality", cardinality);
        assertEquals("cardinality.minValue", Integer.valueOf(1), cardinality.getMinValue());
        assertEquals("cardinality.maxValue", Integer.valueOf(1), cardinality.getMaxValue());
    }

    /**
     * Tests attribute comparison.
     */
    @Test
    public void testEquals() {
        final Map<String,Object> properties = new HashMap<>(4);
        final DefaultAttributeType<Integer> a1 = population(properties);
        final DefaultAttributeType<Integer> a2 = population(properties);
        assertFalse ("equals",   a1.equals(null));
        assertTrue  ("equals",   a1.equals(a2));
        assertEquals("hashCode", a1.hashCode(), a2.hashCode());
    }

    /**
     * Tests serialization.
     */
    @Test
    @DependsOnMethod("testEquals")
    public void testSerialization() {
        final DefaultAttributeType<String> attribute = city(new HashMap<>(4));
        assertSerializedEquals(attribute);
    }
}
