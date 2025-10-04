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

import java.util.Map;
import java.util.LinkedHashMap;
import org.opengis.util.Type;
import org.opengis.util.RecordType;
import org.opengis.util.MemberName;
import org.apache.sis.metadata.simple.SimpleAttributeType;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link DefaultRecordSchema} implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class DefaultRecordSchemaTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultRecordSchemaTest() {
    }

    /**
     * Creates a record type in a temporary schema.
     *
     * @param  schemaName  name of the schema.
     * @param  recordName  name of the record.
     * @param  fields      record fields.
     * @return the record in a temporary schema.
     */
    public static RecordType createRecordType(CharSequence schemaName, CharSequence recordName, Map<CharSequence, Class<?>> fields) {
        final var schema = new DefaultRecordSchema(schemaName);
        return schema.createRecordType(recordName, fields);
    }

    /**
     * Tests {@link DefaultRecordSchema#createRecordType(CharSequence, Map)}.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testCreateRecordType() {
        // Do not use `Map.of(…)` because we need to preserve order.
        final var fields = new LinkedHashMap<CharSequence,Class<?>>(8);
        assertNull(fields.put("city",       String.class));
        assertNull(fields.put("latitude",   Double.class));
        assertNull(fields.put("longitude",  Double.class));
        assertNull(fields.put("population", Integer.class));
        final RecordType recordType = createRecordType("MySchema", "MyRecordType", fields);
        /*
         * Inspect properties.
         */
        assertEquals(Names.createTypeName("MySchema", ":", "MyRecordType"), recordType.getTypeName());
        int count = 0;
        for (final Map.Entry<MemberName,Type> entry : recordType.getMemberTypes().entrySet()) {
            final String   expectedName;
            final String   expectedType;
            final Class<?> expectedClass;
            switch (count) {
                case 0: {
                    expectedName  = "city";
                    expectedType  = "OGC:CharacterString";
                    expectedClass = String.class;
                    break;
                }
                case 1: {
                    expectedName  = "latitude";
                    expectedType  = "OGC:Real";
                    expectedClass = Double.class;
                    break;
                }
                case 2: {
                    expectedName  = "longitude";
                    expectedType  = "OGC:Real";
                    expectedClass = Double.class;
                    break;
                }
                case 3: {
                    expectedName  = "population";
                    expectedType  = "OGC:Integer";
                    expectedClass = Integer.class;
                    break;
                }
                default: {
                    throw new AssertionError(count);
                }
            }
            final Type type = entry.getValue();
            assertEquals(expectedName,  entry.getKey().toString());
            assertEquals(expectedType,  type.getTypeName().toFullyQualifiedName().toString());
            assertEquals(expectedClass, ((SimpleAttributeType) type).getValueClass());
            count++;
        }
        /*
         * The DefaultRecordType(TypeName, RecordSchema, Map) constructor performs many argument checks, so
         * we use that constructor as a way to perform a final validation, especially regarding namespaces.
         */
        final var copy = new DefaultRecordType(
                recordType.getTypeName(),
                recordType.getContainer(),
                recordType.getMemberTypes());
        assertEquals(recordType, copy);
    }
}
