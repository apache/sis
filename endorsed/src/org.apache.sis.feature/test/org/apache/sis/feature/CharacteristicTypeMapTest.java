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
import java.util.AbstractMap.SimpleEntry;
import org.apache.sis.util.iso.Names;
import static org.apache.sis.feature.DefaultAssociationRole.NAME_KEY;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSerializedEquals;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.feature.AttributeType;


/**
 * Tests {@link CharacteristicTypeMap} indirectly, through {@link DefaultAttributeType} construction.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@DependsOn(DefaultAttributeTypeTest.class)
public final class CharacteristicTypeMapTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public CharacteristicTypeMapTest() {
    }

    /**
     * Creates an attribute type for a temperature measurement in °C with a ±0.1°C accuracy.
     *
     * @return a {@code "temperature"} type attribute with two characteristics: {@code "accuracy"} and {@code "units"}.
     */
    public static DefaultAttributeType<Float> temperature() {
        final DefaultAttributeType<?> units, accuracy;
        units    = new DefaultAttributeType<>(Map.of(NAME_KEY, "units"),      String.class, 1, 1, "°C", (DefaultAttributeType[]) null);
        accuracy = new DefaultAttributeType<>(Map.of(NAME_KEY, "accuracy"),    Float.class, 1, 1, 0.1f, (DefaultAttributeType[]) null);
        return     new DefaultAttributeType<>(Map.of(NAME_KEY, "temperature"), Float.class, 1, 1, null, accuracy, units);
    }

    /**
     * Tests the creation of two attributes with the same characteristics.
     * We expect those attributes to share the same {@link CharacteristicTypeMap} instance.
     * We opportunistically test {@code equals} and {@code hashCode} first, because sharing
     * is unlikely to work if those methods did not worked properly.
     */
    @Test
    public void testTwoAttributes() {
        final DefaultAttributeType<Float> t1 = temperature();
        final DefaultAttributeType<Float> t2 = temperature();
        assertNotSame(t1, t2); // Remaining of this test is useless if instances are the same.
        assertTrue  (t1.equals(t2));
        assertTrue  (t2.equals(t1));
        assertEquals(t1.hashCode(), t2.hashCode());
        assertSame  (t1.characteristics(), t2.characteristics());
    }

    /**
     * Tests various methods from the {@link Map} interface.
     */
    @Test
    public void testMapMethods() {
        final AttributeType<?> units, accuracy;
        final DefaultAttributeType<Float> temperature = temperature();
        final var characteristics = temperature.characteristics();

        assertFalse  (           characteristics.isEmpty());
        assertEquals (2,         characteristics.size());
        assertTrue   (           characteristics.containsKey("units"));
        assertTrue   (           characteristics.containsKey("accuracy"));
        assertFalse  (           characteristics.containsKey("temperature"));
        assertNotNull(units    = characteristics.get("units"));
        assertNotNull(accuracy = characteristics.get("accuracy"));
        assertNull   (           characteristics.get("temperature"));
        assertSame   (units,     characteristics.get("units"));
        assertSame   (accuracy,  characteristics.get("accuracy"));
        assertTrue   (           characteristics.containsValue(units));
        assertTrue   (           characteristics.containsValue(accuracy));
        assertFalse  (           characteristics.containsValue(temperature));
        assertArrayEquals(new String[] {"accuracy", "units"}, characteristics.keySet().toArray());
        assertArrayEquals(new Object[] { accuracy ,  units }, characteristics.values().toArray());
        assertArrayEquals(new Object[] {
                new SimpleEntry<>("accuracy", accuracy),
                new SimpleEntry<>("units",    units)
            }, characteristics.entrySet().toArray());
    }

    /**
     * Tests the reconstruction of {@link CharacteristicTypeMap} after serialization.
     */
    @Test
    public void testSerialization() {
        final DefaultAttributeType<Float> temperature  = temperature();
        final DefaultAttributeType<Float> unserialized = assertSerializedEquals(temperature);
        assertNotSame(temperature, unserialized);
        assertSame(temperature.characteristics(), unserialized.characteristics());
    }

    /**
     * Tests usage of names in namespaces.
     */
    @Test
    public void testQualifiedNames() {
        final DefaultAttributeType<?> a1, a2, a3, tp;
        a1 = new DefaultAttributeType<>(Map.of(NAME_KEY, Names.parseGenericName(null, null, "ns1:accuracy")), Float.class, 1, 1, 0.1f);
        a2 = new DefaultAttributeType<>(Map.of(NAME_KEY, Names.parseGenericName(null, null, "ns2:accuracy")), Float.class, 1, 1, 0.1f);
        a3 = new DefaultAttributeType<>(Map.of(NAME_KEY, Names.parseGenericName(null, null, "ns2:s3:units")), String.class, 1, 1, "°C");
        tp = new DefaultAttributeType<>(Map.of(NAME_KEY, "temperature"), Float.class, 1, 1, null, a1, a2, a3);

        final var characteristics = tp.characteristics();
        assertSame(a1, characteristics.get("ns1:accuracy"));
        assertSame(a2, characteristics.get("ns2:accuracy"));
        assertSame(a3, characteristics.get("ns2:s3:units"));
        assertSame(a3, characteristics.get(    "s3:units"));
        assertSame(a3, characteristics.get(       "units"));
        assertNull(    characteristics.get("    accuracy"));        // Because ambiguous.
    }
}
