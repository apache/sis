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

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests {@link DefaultAttributeType}.
 * This class does not test {@link DefaultAttributeType#characteristics()}.
 * Characteristics are tested by {@link CharacteristicTypeMapTest} instead.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DefaultAttributeTypeTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultAttributeTypeTest() {
    }

    /**
     * Creates an attribute type for city name.
     *
     * @return an attribute type for a city name.
     */
    public static DefaultAttributeType<String> city() {
        return city(new HashMap<>());
    }

    /**
     * Implementation of {@link #city()} using the given map (for reusing existing objects).
     *
     * @param  identification  an empty temporary map (provided only for recycling existing instances).
     */
    static DefaultAttributeType<String> city(final Map<String,Object> identification) {
        assertNull(identification.put(DefaultAttributeType.NAME_KEY, "city"));
        assertNull(identification.put(DefaultAttributeType.DESIGNATION_KEY + "_en", "City"));
        assertNull(identification.put(DefaultAttributeType.DESIGNATION_KEY + "_fr", "Ville"));
        assertNull(identification.put(DefaultAttributeType.DESIGNATION_KEY + "_ja", "都市"));
        assertNull(identification.put(DefaultAttributeType.DEFINITION_KEY  + "_en", "The name of the city."));
        assertNull(identification.put(DefaultAttributeType.DEFINITION_KEY  + "_fr", "Le nom de la ville."));
        assertNull(identification.put(DefaultAttributeType.DEFINITION_KEY  + "_ja", "都市の名前。"));
        assertNull(identification.put(DefaultAttributeType.DESCRIPTION_KEY, "Some verbose description."));
        final var city = new DefaultAttributeType<String>(identification, String.class, 1, 1, "Utopia");
        identification.clear();
        return city;
    }

    /**
     * Creates an attribute type for city population.
     *
     * @param  identification  an empty temporary map (provided only for recycling existing instances).
     */
    static DefaultAttributeType<Integer> population(final Map<String,Object> identification) {
        assertNull(identification.put(DefaultAttributeType.NAME_KEY, "population"));
        // We may add more properties here in a future version.

        final var population = new DefaultAttributeType<Integer>(identification, Integer.class, 1, 1, null);
        identification.clear();
        return population;
    }

    /**
     * Creates an attribute type for a parliament name.
     * This applies only to features of type "Capital".
     * This is used for testing feature type inheritance.
     *
     * @return an attribute type for the name of the parliament in a capital.
     */
    public static DefaultAttributeType<String> parliament() {
        return attribute("parliament");
    }

    /**
     * Creates an attribute type for a list of universities.
     * The multiplicity is [0 … ∞].
     *
     * @return an attribute type for university names.
     */
    public static DefaultAttributeType<String> universities() {
        return new DefaultAttributeType<>(
                Map.of(DefaultAttributeType.NAME_KEY, "universities"),
                String.class, 0, Integer.MAX_VALUE, null);
    }

    /**
     * Creates a mandatory attribute type of type {@link String}.
     * This is the kind of attribute created by the ShapeFile reader.
     */
    static DefaultAttributeType<String> attribute(final String name) {
        return new DefaultAttributeType<>(Map.of(DefaultAttributeType.NAME_KEY, name), String.class, 1, 1, null);
    }

    /**
     * Tests the creation of a simple {@link DefaultAttributeType} instance for a mandatory singleton.
     */
    @Test
    public void testMandatorySingleton() {
        final DefaultAttributeType<String> city = city();
        final GenericName name = city.getName();
        assertInstanceOf(LocalName.class, name);
        assertEquals("city", name.toString());

        InternationalString p = city.getDesignation().orElseThrow();
        assertEquals("City",  p.toString(Locale.ENGLISH));
        assertEquals("Ville", p.toString(Locale.FRENCH));
        assertEquals("都市",   p.toString(Locale.JAPANESE));

        p = city.getDefinition();
        assertEquals("The name of the city.", p.toString(Locale.ENGLISH));
        assertEquals("Le nom de la ville.", p.toString(Locale.FRENCH));
        assertEquals("都市の名前。", p.toString(Locale.JAPANESE));

        p = city.getDescription().orElseThrow();
        assertEquals("Some verbose description.", p.toString(Locale.ENGLISH));
        assertEquals(String.class, city.getValueClass());
        assertEquals("Utopia",     city.getDefaultValue());

        assertEquals(1, city.getMinimumOccurs());
        assertEquals(1, city.getMaximumOccurs());
    }

    /**
     * Tests attribute comparison.
     */
    @Test
    @SuppressWarnings("ObjectEqualsNull")
    public void testEquals() {
        final var identification = new HashMap<String,Object>(4);
        final DefaultAttributeType<Integer> a1 = population(identification);
        final DefaultAttributeType<Integer> a2 = population(identification);
        assertFalse (a1.equals(null));
        assertTrue  (a1.equals(a2));
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        final DefaultAttributeType<String> attribute = city();
        assertSerializedEquals(attribute);
    }

    /**
     * Tests {@link DefaultAttributeType#toString()}.
     */
    @Test
    public void testToString() {
        final DefaultAttributeType<String> city = city();
        assertEquals("AttributeType[“city” : String]", city.toString());
    }
}
