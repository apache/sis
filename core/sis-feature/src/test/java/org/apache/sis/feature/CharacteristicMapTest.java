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
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;

// Branch-dependent imports
import org.opengis.feature.Attribute;


/**
 * Tests {@link CharacteristicMap} indirectly, through {@link AbstractAttribute} construction.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn(CharacteristicTypeMapTest.class)
public final strictfp class CharacteristicMapTest extends TestCase {
    /**
     * Creates an attribute for a temperature measurement of 20°C with a ±0.1°C accuracy.
     *
     * @return A {@code "temperature"} attribute with two characteristics: {@code "accuracy"} and {@code "units"}.
     */
    public static AbstractAttribute<Float> temperature() {
        return new SingletonAttribute<>(CharacteristicTypeMapTest.temperature(), 20f);
    }

    /**
     * Creates an attribute for the given characteristic.
     *
     * @param  temperature The attribute created by {@link #temperature()}.
     * @param  name Either {@code "accuracy"} or {@code "units"}.
     * @return An attribute for the given name.
     */
    private static AbstractAttribute<?> create(final AbstractAttribute<?> temperature, final String name) {
        return new SingletonAttribute<>(((DefaultAttributeType<?>) temperature.getType()).characteristics().get(name));
    }

    /**
     * Tests adding explicitely a characteristic with {@link CharacteristicMap#put(String, Attribute)}.
     */
    @Test
    public void testPut() {
        final AbstractAttribute<?>     temperature     = temperature();
        final AbstractAttribute<?>     units           = create(temperature, "units");
        final AbstractAttribute<?>     accuracy        = create(temperature, "accuracy");
        final Map<String,Attribute<?>> characteristics = temperature.characteristics();
        /*
         * Verify that the map is initially empty.
         */
        assertTrue  ("isEmpty",       characteristics.isEmpty());
        assertEquals("size", 0,       characteristics.size());
        assertFalse ("containsKey",   characteristics.containsKey("units"));
        assertFalse ("containsKey",   characteristics.containsKey("accuracy"));
        assertFalse ("containsKey",   characteristics.containsKey("temperature"));
        assertNull  ("get",           characteristics.get("units"));
        assertNull  ("get",           characteristics.get("accuracy"));
        assertNull  ("get",           characteristics.get("temperature"));
        assertFalse ("containsValue", characteristics.containsValue(units));
        assertFalse ("containsValue", characteristics.containsValue(accuracy));
        assertFalse ("containsValue", characteristics.containsValue(temperature));
        /*
         * Store "units" characteristic and verify.
         */
        assertNull  ("put",           characteristics.put("units", units));
        assertFalse ("isEmpty",       characteristics.isEmpty());
        assertEquals("size", 1,       characteristics.size());
        assertTrue  ("containsKey",   characteristics.containsKey("units"));
        assertFalse ("containsKey",   characteristics.containsKey("accuracy"));
        assertFalse ("containsKey",   characteristics.containsKey("temperature"));
        assertSame  ("get", units,    characteristics.get("units"));
        assertNull  ("get",           characteristics.get("accuracy"));
        assertNull  ("get",           characteristics.get("temperature"));
        assertTrue  ("containsValue", characteristics.containsValue(units));
        assertFalse ("containsValue", characteristics.containsValue(accuracy));
        assertFalse ("containsValue", characteristics.containsValue(temperature));
        /*
         * Store "accuracy" characteristic and verify.
         */
        assertNull  ("put",           characteristics.put("accuracy", accuracy));
        assertFalse ("isEmpty",       characteristics.isEmpty());
        assertEquals("size", 2,       characteristics.size());
        assertTrue  ("containsKey",   characteristics.containsKey("units"));
        assertTrue  ("containsKey",   characteristics.containsKey("accuracy"));
        assertFalse ("containsKey",   characteristics.containsKey("temperature"));
        assertSame  ("get", units,    characteristics.get("units"));
        assertSame  ("get", accuracy, characteristics.get("accuracy"));
        assertNull  ("get",           characteristics.get("temperature"));
        assertTrue  ("containsValue", characteristics.containsValue(units));
        assertTrue  ("containsValue", characteristics.containsValue(accuracy));
        assertFalse ("containsValue", characteristics.containsValue(temperature));
        /*
         * Overwrite values. Map shall stay unchanged.
         */
        assertSame  ("put",  accuracy, characteristics.put("accuracy", accuracy));
        assertSame  ("put",  units,    characteristics.put("units",    units));
        assertEquals("size", 2,        characteristics.size());
        /*
         * Try putting an attribute for a non-existent name.
         * Map shall stay unchanged.
         */
        try {
            characteristics.put("dummy", units);
            fail("Operation shall not have been allowed.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("dummy"));       // Message shall contain the wrong name.
            assertTrue(message, message.contains("temperature")); // Message shall contain the enclosing attribute name.
        }
        /*
         * Try putting an attribute of the wrong type.
         * Map shall stay unchanged.
         */
        try {
            characteristics.put("units", accuracy);
            fail("Operation shall not have been allowed.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("accuracy"));
            assertTrue(message, message.contains("temperature:units"));
        }
        assertArrayEquals("keySet", new String[] {"accuracy", "units"}, characteristics.keySet().toArray());
        assertArrayEquals("values", new Object[] { accuracy ,  units }, characteristics.values().toArray());
        assertArrayEquals("entrySet", new Object[] {
                new SimpleEntry<>("accuracy", accuracy),
                new SimpleEntry<>("units",    units)
            }, characteristics.entrySet().toArray());
    }
}
