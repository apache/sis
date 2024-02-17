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

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertSerializedEquals;


/**
 * Tests {@link CharacteristicMap} indirectly, through {@link AbstractAttribute} construction.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class CharacteristicMapTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public CharacteristicMapTest() {
    }

    /**
     * Creates an attribute for a temperature measurement of 20°C with a ±0.1°C accuracy.
     *
     * @return a {@code "temperature"} attribute with two characteristics: {@code "accuracy"} and {@code "units"}.
     */
    public static AbstractAttribute<Float> temperature() {
        return new SingletonAttribute<>(CharacteristicTypeMapTest.temperature(), 20f);
    }

    /**
     * Creates an attribute for the given characteristic.
     *
     * @param  temperature  the attribute created by {@link #temperature()}.
     * @param  name         either {@code "accuracy"} or {@code "units"}.
     * @return an attribute for the given name.
     */
    private static AbstractAttribute<?> create(final AbstractAttribute<?> temperature, final String name) {
        return new SingletonAttribute<>(((DefaultAttributeType<?>) temperature.getType()).characteristics().get(name));
    }

    /**
     * Tests adding explicitly a characteristic with {@code CharacteristicMap.put(String, AbstractAttribute)}.
     */
    @Test
    public void testPut() {
        final AbstractAttribute<?> temperature = temperature();
        final AbstractAttribute<?> units       = create(temperature, "units");
        final AbstractAttribute<?> accuracy    = create(temperature, "accuracy");
        final var characteristics = temperature.characteristics();
        /*
         * Verify that the map is initially empty.
         */
        assertTrue  (   characteristics.isEmpty());
        assertEquals(0, characteristics.size());
        assertFalse (   characteristics.containsKey("units"));
        assertFalse (   characteristics.containsKey("accuracy"));
        assertFalse (   characteristics.containsKey("temperature"));
        assertNull  (   characteristics.get("units"));
        assertNull  (   characteristics.get("accuracy"));
        assertNull  (   characteristics.get("temperature"));
        assertFalse (   characteristics.containsValue(units));
        assertFalse (   characteristics.containsValue(accuracy));
        assertFalse (   characteristics.containsValue(temperature));
        /*
         * Store "units" characteristic and verify.
         */
        assertNull  (       characteristics.put("units", units));
        assertFalse (       characteristics.isEmpty());
        assertEquals(1,     characteristics.size());
        assertTrue  (       characteristics.containsKey("units"));
        assertFalse (       characteristics.containsKey("accuracy"));
        assertFalse (       characteristics.containsKey("temperature"));
        assertSame  (units, characteristics.get("units"));
        assertNull  (       characteristics.get("accuracy"));
        assertNull  (       characteristics.get("temperature"));
        assertTrue  (       characteristics.containsValue(units));
        assertFalse (       characteristics.containsValue(accuracy));
        assertFalse (       characteristics.containsValue(temperature));
        /*
         * Store "accuracy" characteristic and verify.
         */
        assertNull  (          characteristics.put("accuracy", accuracy));
        assertFalse (          characteristics.isEmpty());
        assertEquals(2,        characteristics.size());
        assertTrue  (          characteristics.containsKey("units"));
        assertTrue  (          characteristics.containsKey("accuracy"));
        assertFalse (          characteristics.containsKey("temperature"));
        assertSame  (units,    characteristics.get("units"));
        assertSame  (accuracy, characteristics.get("accuracy"));
        assertNull  (          characteristics.get("temperature"));
        assertTrue  (          characteristics.containsValue(units));
        assertTrue  (          characteristics.containsValue(accuracy));
        assertFalse (          characteristics.containsValue(temperature));
        /*
         * Overwrite values. Map shall stay unchanged.
         */
        assertSame  (accuracy, characteristics.put("accuracy", accuracy));
        assertSame  (units,    characteristics.put("units",    units));
        assertEquals(2,        characteristics.size());
        /*
         * Try putting an attribute for a non-existent name.
         * Map shall stay unchanged.
         */
        IllegalArgumentException e;
        e = assertThrows(IllegalArgumentException.class, () -> characteristics.put("dummy", units));
        assertMessageContains(e, "dummy", "temperature");
        /*
         * Try putting an attribute of the wrong type.
         * Map shall stay unchanged.
         */
        e = assertThrows(IllegalArgumentException.class, () -> characteristics.put("units", accuracy));
        assertMessageContains(e, "accuracy", "temperature:units");
        assertEntriesEqual(units, accuracy, characteristics);
        assertSame(characteristics, temperature.characteristics());
    }

    /**
     * Tests adding a characteristic indirectly with {@code CharacteristicMap.addValue(AbstractAttribute)}.
     */
    @Test
    public void testAddValue() {
        final AbstractAttribute<?> temperature = temperature();
        final AbstractAttribute<?> units       = create(temperature, "units");
        final AbstractAttribute<?> accuracy    = create(temperature, "accuracy");
        final var characteristics = temperature.characteristics();
        final var values = characteristics.values();
        /*
         * Verify that the collection is initially empty.
         */
        assertTrue  (   values.isEmpty());
        assertEquals(0, values.size());
        assertFalse (   values.contains(units));
        assertFalse (   values.contains(accuracy));
        assertFalse (   values.contains(temperature));
        /*
         * Store "units" characteristic and verify.
         */
        assertTrue  (   values.add(units));
        assertFalse (   values.isEmpty());
        assertEquals(1, values.size());
        assertTrue  (   values.contains(units));
        assertFalse (   values.contains(accuracy));
        assertFalse (   values.contains(temperature));
        /*
         * Store "accuracy" characteristic and verify.
         */
        assertTrue  (   values.add(accuracy));
        assertFalse (   values.isEmpty());
        assertEquals(2, values.size());
        assertTrue  (   values.contains(units));
        assertTrue  (   values.contains(accuracy));
        assertFalse (   values.contains(temperature));
        /*
         * Overwrite values. Map shall stay unchanged.
         */
        assertFalse (   values.add(accuracy));
        assertFalse (   values.add(units));
        assertEquals(2, values.size());
        /*
         * Try adding an attribute of the wrong type.
         * Map shall stay unchanged.
         */
        var e = assertThrows(IllegalArgumentException.class, () -> values.add(temperature));
        assertMessageContains(e, "temperature");
        assertEntriesEqual(units, accuracy, characteristics);
        assertSame(characteristics, temperature.characteristics());
    }

    /**
     * Tests adding a characteristic indirectly with {@link CharacteristicMap#addKey(String)}.
     */
    @Test
    public void testAddKey() {
        final AbstractAttribute<?> units, accuracy;
        final AbstractAttribute<?> temperature = temperature();
        final var characteristics = temperature.characteristics();
        final Collection<String> keys = characteristics.keySet();
        /*
         * Verify that the collection is initially empty.
         */
        assertTrue  (   keys.isEmpty());
        assertEquals(0, keys.size());
        assertFalse (   keys.contains("units"));
        assertFalse (   keys.contains("accuracy"));
        assertFalse (   keys.contains("temperature"));
        /*
         * Store "units" characteristic and verify.
         */
        assertTrue   (   keys.add("units"));
        assertFalse  (   keys.isEmpty());
        assertEquals (1, keys.size());
        assertTrue   (   keys.contains("units"));
        assertFalse  (   keys.contains("accuracy"));
        assertFalse  (   keys.contains("temperature"));
        assertNotNull(units = characteristics.get("units"));
        /*
         * Store "accuracy" characteristic and verify.
         */
        assertTrue  (   keys.add("accuracy"));
        assertFalse (   keys.isEmpty());
        assertEquals(2, keys.size());
        assertTrue  (   keys.contains("units"));
        assertTrue  (   keys.contains("accuracy"));
        assertFalse (   keys.contains("temperature"));
        assertNotNull(accuracy = characteristics.get("accuracy"));
        /*
         * Overwrite values. Map shall stay unchanged.
         */
        assertFalse (   keys.add("accuracy"));
        assertFalse (   keys.add("units"));
        assertEquals(2, keys.size());
        /*
         * Try adding an attribute of the wrong type.
         * Map shall stay unchanged.
         */
        var e = assertThrows(IllegalArgumentException.class, () -> keys.add("dummy"));
        assertMessageContains(e, "temperature", "dummy");
        assertEntriesEqual(units, accuracy, characteristics);
        assertSame(characteristics, temperature.characteristics());
    }

    /**
     * Verifies that the given characteristics map contains entries for the given attributes.
     *
     * @param  units            the first expected value in iteration order.
     * @param  accuracy         the second expected value in iteration order.
     * @param  characteristics  the map to verify.
     */
    private static void assertEntriesEqual(final AbstractAttribute<?> units, final AbstractAttribute<?> accuracy,
            final Map<String,AbstractAttribute<?>> characteristics)
    {
        assertArrayEquals(new String[] {"accuracy", "units"}, characteristics.keySet().toArray());
        assertArrayEquals(new Object[] { accuracy ,  units }, characteristics.values().toArray());
        assertArrayEquals(new Object[] {
                new SimpleEntry<>("accuracy", accuracy),
                new SimpleEntry<>("units",    units)
            }, characteristics.entrySet().toArray());
    }

    /**
     * Sets the accuracy characteristic in the given attribute.
     *
     * @param  temperature  the attribute where to set the accuracy.
     * @param  isFirstTime  {@code true} if the accuracy value is set for the first time.
     * @param  value        the new accuracy value.
     */
    private static void setAccuracy(final AbstractAttribute<Float> temperature, final boolean isFirstTime, final float value) {
        assertEquals(isFirstTime, temperature.characteristics().keySet().add("accuracy"));
        final var accuracy = Features.cast(temperature.characteristics().get("accuracy"), Float.class);
        accuracy.setValue(value);
    }

    /**
     * Tests {@link CharacteristicMap#equals(Object)} and (opportunistically) {@link CharacteristicMap#clone()}
     */
    @Test
    public void testEquals() {
        final AbstractAttribute<Float> t1 = temperature();
        final AbstractAttribute<Float> t2 = temperature();
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());

        setAccuracy(t1, true, 0.2f);
        assertNotEquals(t1, t2);
        assertNotEquals(t2, t1);
        assertNotEquals(t1.hashCode(), t2.hashCode());

        setAccuracy(t2, true, 0.3f);
        assertNotEquals(t1, t2);
        assertNotEquals(t2, t1);
        assertNotEquals(t1.hashCode(), t2.hashCode());

        setAccuracy(t1, false, 0.3f);
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    /**
     * Tests the reconstruction of {@link CharacteristicTypeMap} after serialization.
     */
    @Test
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
