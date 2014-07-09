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
package org.apache.sis.util.iso;


import java.util.LinkedHashMap;
import java.util.Map;
import org.opengis.util.MemberName;
import org.opengis.util.RecordType;

// Test imports
import org.junit.Test;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link DefaultRecord} implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn(DefaultRecordSchemaTest.class)
public final strictfp class DefaultRecordTest extends TestCase {
    /**
     * The record type to be shared by all tests.
     */
    private static RecordType recordType;

    /**
     * Creates the {@link DefaultRecordType} to be used by all tests in this class.
     */
    @BeforeClass
    public static void createRecordType() {
        final DefaultRecordSchema schema = new SerializableRecordSchema("MySchema");
        final Map<CharSequence,Class<?>> members = new LinkedHashMap<>(8);
        assertNull(members.put("city",       String.class));
        assertNull(members.put("latitude",   Double.class));
        assertNull(members.put("longitude",  Double.class));
        assertNull(members.put("population", Integer.class));
        recordType = schema.createRecordType("MyRecordType", members);
        SerializableRecordSchema.INSTANCE = schema;
    }

    /**
     * Clears the {@link DefaultRecordType} used by the tests.
     */
    @AfterClass
    public static void clearRecordType() {
        SerializableRecordSchema.INSTANCE = null;
        recordType = null;
    }

    /**
     * Tests {@link DefaultRecord#setValues(Object[])}.
     */
    @Test
    public void testSetValues() {
        final DefaultRecord record = new DefaultRecord(recordType);
        try {
            record.setValues("Machu Picchu", -13.1639, -72.5468);
            fail("Shall not accept array of illegal length.");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
        try {
            record.setValues("Machu Picchu", -13.1639, -72.5468, "Unknown");
            fail("Shall not accept 'population' value of class String.");
        } catch (ClassCastException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("population"));
            assertTrue(message, message.contains("String"));
        }
        final Object[] values = {"Machu Picchu", -13.1639, -72.5468, null};
        record.setValues(values);
        assertArrayEquals(values, record.getAttributes().values().toArray());
    }

    /**
     * Tests iteration over the {@link DefaultRecord#getAttributes()} elements.
     */
    @Test
    public void testAttributes() {
        final DefaultRecord record = new DefaultRecord(recordType);
        assertSame("recordType", recordType, record.getRecordType());
        int index = 0;
        for (final Map.Entry<MemberName,Object> entry : record.getAttributes().entrySet()) {
            final String name;
            final Object value;
            switch (index) {
                case 0: name = "city";       value = "Machu Picchu"; break;
                case 1: name = "latitude";   value = -13.1639;       break;
                case 2: name = "longitude";  value = -72.5468;       break;
                case 3: name = "population"; value = null;           break;
                default: throw new AssertionError(index);
            }
            assertEquals(name, entry.getKey().toString());
            assertNull  (name, entry.getValue());
            assertNull  (name, entry.setValue(value));
            assertEquals(name, value, entry.getValue());
            index++;
        }
        assertEquals(4, index);
    }

    /**
     * Tests {@link DefaultRecord#toString()}.
     */
    @Test
    @DependsOnMethod("testSetValues")
    public void testToString() {
        final DefaultRecord record = new DefaultRecord(recordType);
        record.setValues("Machu Picchu", -13.1639, -72.5468, null);
        assertMultilinesEquals(
                "Record[“MyRecordType”] {\n" +
                "    city       : Machu Picchu\n" +
                "    latitude   : -13.1639\n" +
                "    longitude  : -72.5468\n" +
                "    population\n" +
                "}\n",
                record.toString());
        /*
         * Opportunist test of RecordType.toString(),
         * which share the same implementation code.
         */
        assertMultilinesEquals(
                "RecordType[“MyRecordType”] {\n" +
                "    city       : CharacterString\n" +
                "    latitude   : Real\n" +
                "    longitude  : Real\n" +
                "    population : Integer\n" +
                "}\n",
                record.getRecordType().toString());
    }

    /**
     * Tests serialization of a {@link DefaultRecord}.
     */
    @Test
    @DependsOnMethod("testSetValues")
    public void testSerialization() {
        final DefaultRecord record = new DefaultRecord(recordType);
        record.setValues("Machu Picchu", -13.1639, -72.5468, null);
        assertNotSame(record, assertSerializedEquals(record));
    }
}
