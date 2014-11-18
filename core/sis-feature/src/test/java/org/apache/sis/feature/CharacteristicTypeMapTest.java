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

import static java.util.Collections.singletonMap;
import static org.apache.sis.feature.DefaultAssociationRole.NAME_KEY;
import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link CharacteristicTypeMap} indirectly, through {@link DefaultAttributeType} construction.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn(DefaultAttributeTypeTest.class)
public final strictfp class CharacteristicTypeMapTest extends TestCase {
    /**
     * Creates an attribute type for a temperature measurement in °C with a ±0.1°C accuracy.
     *
     * @return A {@code "temperature"} type attribute with two characteristics: {@code "accuracy"} and {@code "units"}.
     */
    public static DefaultAttributeType<Float> temperature() {
        final DefaultAttributeType<?> units, accuracy;
        units    = new DefaultAttributeType<String>(singletonMap(NAME_KEY, "units"),      String.class, 1, 1, "°C", (DefaultAttributeType[]) null);
        accuracy = new DefaultAttributeType<Float> (singletonMap(NAME_KEY, "accuracy"),    Float.class, 1, 1, 0.1f, (DefaultAttributeType[]) null);
        return     new DefaultAttributeType<Float> (singletonMap(NAME_KEY, "temperature"), Float.class, 1, 1, null, accuracy, units);
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
        assertTrue  ("equals",   t1.equals(t2));
        assertTrue  ("equals",   t2.equals(t1));
        assertEquals("hashCode", t1.hashCode(), t2.hashCode());
        assertSame  ("shared",   t1.characteristics(), t2.characteristics());
    }

    /**
     * Tests various methods from the {@link Map} interface.
     */
    @Test
    public void testMapMethods() {
        final DefaultAttributeType<?> units, accuracy;
        final DefaultAttributeType<Float> temperature = temperature();
        final Map<String, DefaultAttributeType<?>> characteristics = temperature.characteristics();

        assertFalse  ("isEmpty",        characteristics.isEmpty());
        assertEquals ("size", 2,        characteristics.size());
        assertTrue   ("containsKey",    characteristics.containsKey("units"));
        assertTrue   ("containsKey",    characteristics.containsKey("accuracy"));
        assertFalse  ("containsKey",    characteristics.containsKey("temperature"));
        assertNotNull("get", units    = characteristics.get("units"));
        assertNotNull("get", accuracy = characteristics.get("accuracy"));
        assertNull   ("get",            characteristics.get("temperature"));
        assertSame   ("get", units,     characteristics.get("units"));
        assertSame   ("get", accuracy,  characteristics.get("accuracy"));
        assertTrue   ("containsValue",  characteristics.containsValue(units));
        assertTrue   ("containsValue",  characteristics.containsValue(accuracy));
        assertFalse  ("containsValue",  characteristics.containsValue(temperature));
        assertArrayEquals("keySet", new String[] {"accuracy", "units"}, characteristics.keySet().toArray());
        assertArrayEquals("values", new Object[] { accuracy ,  units }, characteristics.values().toArray());
        assertArrayEquals("entrySet", new Object[] {
                new SimpleEntry<String,DefaultAttributeType<?>>("accuracy", accuracy),
                new SimpleEntry<String,DefaultAttributeType<?>>("units",    units)
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
}
