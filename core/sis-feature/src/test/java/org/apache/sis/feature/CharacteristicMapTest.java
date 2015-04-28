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
import java.util.Collection;
import java.util.AbstractMap.SimpleEntry;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


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
        return new SingletonAttribute<Float>(CharacteristicTypeMapTest.temperature(), 20f);
    }

    /**
     * Creates an attribute for the given characteristic.
     *
     * @param  temperature The attribute created by {@link #temperature()}.
     * @param  name Either {@code "accuracy"} or {@code "units"}.
     * @return An attribute for the given name.
     */
    @SuppressWarnings({"unchecked","rawtypes"})    // Not needed on JDK7 branch.
    private static AbstractAttribute<?> create(final AbstractAttribute<?> temperature, final String name) {
        return new SingletonAttribute(((DefaultAttributeType<?>) temperature.getType()).characteristics().get(name));
    }

    /**
     * Tests adding explicitely a characteristic with {@link CharacteristicMap#put(String, Attribute)}.
     */
    @Test
    public void testPut() {
        final AbstractAttribute<?> temperature = temperature();
        final AbstractAttribute<?> units       = create(temperature, "units");
        final AbstractAttribute<?> accuracy    = create(temperature, "accuracy");
        final Map<String,AbstractAttribute<?>> characteristics = temperature.characteristics();
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
        assertEntriesEqual(units, accuracy, characteristics);
        assertSame(characteristics, temperature.characteristics());
    }

    /**
     * Tests adding a characteristic indirectly with {@link CharacteristicMap#addValue(Attribute)}.
     */
    @Test
    @DependsOnMethod("testPut")
    public void testAddValue() {
        final AbstractAttribute<?> temperature = temperature();
        final AbstractAttribute<?> units       = create(temperature, "units");
        final AbstractAttribute<?> accuracy    = create(temperature, "accuracy");
        final Map<String,AbstractAttribute<?>> characteristics = temperature.characteristics();
        final Collection<AbstractAttribute<?>> values          = characteristics.values();
        /*
         * Verify that the collection is initially empty.
         */
        assertTrue  ("isEmpty",  values.isEmpty());
        assertEquals("size", 0,  values.size());
        assertFalse ("contains", values.contains(units));
        assertFalse ("contains", values.contains(accuracy));
        assertFalse ("contains", values.contains(temperature));
        /*
         * Store "units" characteristic and verify.
         */
        assertTrue  ("add",      values.add(units));
        assertFalse ("isEmpty",  values.isEmpty());
        assertEquals("size", 1,  values.size());
        assertTrue  ("contains", values.contains(units));
        assertFalse ("contains", values.contains(accuracy));
        assertFalse ("contains", values.contains(temperature));
        /*
         * Store "accuracy" characteristic and verify.
         */
        assertTrue  ("add",      values.add(accuracy));
        assertFalse ("isEmpty",  values.isEmpty());
        assertEquals("size", 2,  values.size());
        assertTrue  ("contains", values.contains(units));
        assertTrue  ("contains", values.contains(accuracy));
        assertFalse ("contains", values.contains(temperature));
        /*
         * Overwrite values. Map shall stay unchanged.
         */
        assertFalse ("add",     values.add(accuracy));
        assertFalse ("add",     values.add(units));
        assertEquals("size", 2, values.size());
        /*
         * Try adding an attribute of the wrong type.
         * Map shall stay unchanged.
         */
        try {
            values.add(temperature);
            fail("Operation shall not have been allowed.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("temperature"));
        }
        assertEntriesEqual(units, accuracy, characteristics);
        assertSame(characteristics, temperature.characteristics());
    }

    /**
     * Tests adding a characteristic indirectly with {@link CharacteristicMap#addKey(String)}.
     */
    @Test
    @DependsOnMethod("testPut")
    public void testAddKey() {
        final AbstractAttribute<?> units, accuracy;
        final AbstractAttribute<?> temperature = temperature();
        final Map<String,AbstractAttribute<?>> characteristics = temperature.characteristics();
        final Collection<String> keys = characteristics.keySet();
        /*
         * Verify that the collection is initially empty.
         */
        assertTrue  ("isEmpty",  keys.isEmpty());
        assertEquals("size", 0,  keys.size());
        assertFalse ("contains", keys.contains("units"));
        assertFalse ("contains", keys.contains("accuracy"));
        assertFalse ("contains", keys.contains("temperature"));
        /*
         * Store "units" characteristic and verify.
         */
        assertTrue   ("add",      keys.add("units"));
        assertFalse  ("isEmpty",  keys.isEmpty());
        assertEquals ("size", 1,  keys.size());
        assertTrue   ("contains", keys.contains("units"));
        assertFalse  ("contains", keys.contains("accuracy"));
        assertFalse  ("contains", keys.contains("temperature"));
        assertNotNull("get",      units = characteristics.get("units"));
        /*
         * Store "accuracy" characteristic and verify.
         */
        assertTrue  ("add",       keys.add("accuracy"));
        assertFalse ("isEmpty",   keys.isEmpty());
        assertEquals("size", 2,   keys.size());
        assertTrue  ("contains",  keys.contains("units"));
        assertTrue  ("contains",  keys.contains("accuracy"));
        assertFalse ("contains",  keys.contains("temperature"));
        assertNotNull("get",      accuracy = characteristics.get("accuracy"));
        /*
         * Overwrite values. Map shall stay unchanged.
         */
        assertFalse ("add",     keys.add("accuracy"));
        assertFalse ("add",     keys.add("units"));
        assertEquals("size", 2, keys.size());
        /*
         * Try adding an attribute of the wrong type.
         * Map shall stay unchanged.
         */
        try {
            keys.add("dummy");
            fail("Operation shall not have been allowed.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("temperature"));
            assertTrue(message, message.contains("dummy"));
        }
        assertEntriesEqual(units, accuracy, characteristics);
        assertSame(characteristics, temperature.characteristics());
    }

    /**
     * Verifies that the given characteristics map contains entries for the given attributes.
     *
     * @param units           The first expected value in iteration order.
     * @param accuracy        The second expected value in iteration order.
     * @param characteristics The map to verify.
     */
    private static void assertEntriesEqual(final AbstractAttribute<?> units, final AbstractAttribute<?> accuracy,
            final Map<String,AbstractAttribute<?>> characteristics)
    {
        assertArrayEquals("keySet", new String[] {"accuracy", "units"}, characteristics.keySet().toArray());
        assertArrayEquals("values", new Object[] { accuracy ,  units }, characteristics.values().toArray());
        assertArrayEquals("entrySet", new Object[] {
                new SimpleEntry<String,AbstractAttribute<?>>("accuracy", accuracy),
                new SimpleEntry<String,AbstractAttribute<?>>("units",    units)
            }, characteristics.entrySet().toArray());
    }

    /**
     * Sets the accuracy characteristic in the given attribute.
     *
     * @param temperature The attribute where to set the accuracy.
     * @param isFirstTime {@code true} if the accuracy value is set for the first time.
     * @param value       The new accuracy value.
     */
    private static void setAccuracy(final AbstractAttribute<Float> temperature, final boolean isFirstTime, final float value) {
        assertEquals("keySet.add", isFirstTime, temperature.characteristics().keySet().add("accuracy"));
        final AbstractAttribute<Float> accuracy = Features.cast(temperature.characteristics().get("accuracy"), Float.class);
        accuracy.setValue(value);
    }

    /**
     * Tests {@link CharacteristicMap#equals(Object)} and (opportunistically) {@link CharacteristicMap#clone()}
     */
    @Test
    @DependsOnMethod("testAddKey")
    public void testEquals() {
        final AbstractAttribute<Float> t1 = temperature();
        final AbstractAttribute<Float> t2 = temperature();
        assertEquals("equals",   t1, t2);
        assertEquals("hashCode", t1.hashCode(), t2.hashCode());
        setAccuracy(t1, true, 0.2f);
        assertFalse("equals",   t1.equals(t2));
        assertFalse("equals",   t2.equals(t1));
        assertFalse("hashCode", t1.hashCode() == t2.hashCode());
        setAccuracy(t2, true, 0.3f);
        assertFalse("equals",   t1.equals(t2));
        assertFalse("equals",   t2.equals(t1));
        assertFalse("hashCode", t1.hashCode() == t2.hashCode());
        setAccuracy(t1, false, 0.3f);
        assertEquals("equals",   t1, t2);
        assertEquals("hashCode", t1.hashCode(), t2.hashCode());
    }

    /**
     * Tests the reconstruction of {@link CharacteristicTypeMap} after serialization.
     */
    @Test
    @DependsOnMethod({"testEquals", "testAddValue"}) // Implementation of readObject use values().addAll(...).
    public void testSerialization() {
        final AbstractAttribute<Float> temperature = temperature();
        setAccuracy(temperature, true, 0.2f);

        final AbstractAttribute<Float> unserialized = assertSerializedEquals(temperature);
        assertNotSame(temperature, unserialized);
        assertNotSame(temperature.characteristics(), unserialized.characteristics());
        assertSame(((DefaultAttributeType<?>) temperature .getType()).characteristics(),
                   ((DefaultAttributeType<?>) unserialized.getType()).characteristics());
    }
}
