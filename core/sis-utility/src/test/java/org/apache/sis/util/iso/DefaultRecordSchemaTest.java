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
import org.apache.sis.internal.simple.SimpleAttributeType;

// Test imports
import org.junit.Test;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;

import static org.junit.Assert.*;


/**
 * Tests the {@link DefaultRecordSchema} implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn({
    DefaultRecordTypeTest.class,
    NamesTest.class
})
public final strictfp class DefaultRecordSchemaTest extends TestCase {
    /**
     * Tests {@link DefaultRecordSchema#createRecordType(CharSequence, Map)}.
     */
    @Test
    public void testCreateRecordType() {
        final DefaultRecordSchema schema = new DefaultRecordSchema(null, null, "MySchema");
        final Map<CharSequence,Class<?>> members = new LinkedHashMap<CharSequence,Class<?>>(8);
        assertNull(members.put("city",       String.class));
        assertNull(members.put("latitude",   Double.class));
        assertNull(members.put("longitude",  Double.class));
        assertNull(members.put("population", Integer.class));
        final RecordType recordType = schema.createRecordType("MyRecordType", members);
        /*
         * Inspect properties.
         */
        assertSame("container", schema, recordType.getContainer());
        assertEquals("typeName", Names.createTypeName("MySchema", ":", "MyRecordType"), recordType.getTypeName());
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
        final DefaultRecordType copy = new DefaultRecordType(
                recordType.getTypeName(),
                recordType.getContainer(),
                recordType.getMemberTypes());
        assertEquals(recordType, copy);
    }
}
