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
import org.opengis.util.Type;
import org.opengis.util.MemberName;
import org.opengis.util.NameSpace;
import org.apache.sis.internal.simple.SimpleAttributeType;

// Test imports
import org.junit.Test;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;

import static org.junit.Assert.*;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests the {@link DefaultRecordType} implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.5
 */
@DependsOn(TypeNamesTest.class)
public final class DefaultRecordTypeTest extends TestCase {
    /** Value of {@link DefaultRecordType#getContainer()}.  */ private DefaultRecordSchema container;
    /** Value of {@link DefaultRecordType#getTypeName()}.   */ private DefaultTypeName     recordTypeName;
    /** Value of {@link DefaultRecordType#getMembers()}.    */ private DefaultMemberName   fieldName;
    /** Value of {@link DefaultRecordType#getFieldTypes()}. */ private DefaultTypeName     fieldTypeName;

    /**
     * Initializes the private fields.
     * This method shall be invoked only once per test.
     */
    private void init() {
        final DefaultNameSpace recordNamespace;
        final DefaultNameSpace fieldNamespace;

        recordNamespace = new DefaultNameSpace (null, "MyNameSpace", ":", ":");
        recordTypeName  = new DefaultTypeName  (recordNamespace, "MyRecordType");
        fieldNamespace  = new DefaultNameSpace (recordNamespace, "MyRecordType", ":", ":");
        fieldTypeName   = new DefaultTypeName  (new DefaultNameSpace(null, "gco", ":", ":"), "Integer");
        fieldName       = new DefaultMemberName(fieldNamespace, "aMember", fieldTypeName);
        container       = new SerializableRecordSchema("MyNameSpace");
        assertEquals("MyNameSpace:MyRecordType:aMember", fieldName.toFullyQualifiedName().toString());
    }

    /**
     * Creates a new record type from the current values of private fields.
     */
    private DefaultRecordType create() throws IllegalArgumentException {
        final Type fieldType = new SimpleAttributeType<>(fieldTypeName, Integer.class);
        return new DefaultRecordType(recordTypeName, container, Map.of(fieldName, fieldType));
    }

    /**
     * Tests the construction of {@link DefaultRecordType}, and opportunistically tests
     * {@link DefaultRecordType#locate(MemberName)}.
     */
    @Test
    public void testConstructor() {
        init();
        final DefaultRecordType type = create();
        assertEquals("size", 1, type.size());
        assertEquals("baseValueClass", Integer.TYPE, type.baseValueClass());

        // Public properties
        assertSame("container",  container,      type.getContainer());
        assertSame("typeName",   recordTypeName, type.getTypeName());
        assertSame("fields",     fieldName,      getSingleton(type.getMembers()));
        assertSame("fieldTypes", fieldName,      getSingleton(type.getFieldTypes().keySet()));
        assertSame("fieldTypes", fieldTypeName,  getSingleton(type.getFieldTypes().values()).getTypeName());
        assertSame("locate",     fieldTypeName,  type.locate(fieldName));
        assertNull("locate",                     type.locate(new DefaultMemberName(null, "otherMember", fieldTypeName)));
    }

    /**
     * Ensures that constructions of {@link DefaultRecordType} with
     * inconsistent arguments throw an exception.
     */
    @Test
    @DependsOnMethod("testConstructor")
    public void testArgumentChecks() {
        init();
        final DefaultTypeName  correctRecordName      = recordTypeName;
        final NameSpace        correctMemberNamespace = fieldName.scope();
        final DefaultNameSpace wrongNamespace         = new DefaultNameSpace(null, "WrongNameSpace", ":", ":");
        /*
         * RecordType namespace validation.
         * Constructor shall require "MyNameSpace:MyRecordType".
         */
        recordTypeName = new DefaultTypeName(wrongNamespace, "MyRecordType");
        try {
            create();
            fail("Should have detected namespace mismatch.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("MyNameSpace"));                 // Expected name.
            assertTrue(message, message.contains("WrongNameSpace:MyRecordType")); // Actual namespace.
        }
        /*
         * MemberName namespace validation.
         * Constructor shall require "MyNameSpace:MyRecordType:aMember".
         */
        recordTypeName = correctRecordName;
        fieldName = new DefaultMemberName(wrongNamespace, "aMember", fieldTypeName);
        try {
            create();
            fail("Should have detected namespace mismatch.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("MyNameSpace:MyRecordType"));  // Expected name.
            assertTrue(message, message.contains("WrongNameSpace:aMember"));    // Actual namespace.
        }
        /*
         * MemberName type validation.
         */
        final DefaultTypeName otherType = new DefaultTypeName(fieldTypeName.scope(), "Real");
        fieldName = new DefaultMemberName(correctMemberNamespace, "aMember", otherType);
        try {
            create();
            fail("Should have detected type mismatch.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("aMember"));
            assertTrue(message, message.contains("gco:Integer"));
        }
    }

    /**
     * Tests serialization of a {@code RecordType}.
     */
    @Test
    @DependsOnMethod("testConstructor")
    public void testSerialization() {
        init();
        synchronized (SerializableRecordSchema.class) {
            try {
                SerializableRecordSchema.INSTANCE = container;
                assertSerializedEquals(create());
            } finally {
                SerializableRecordSchema.INSTANCE = null;
            }
        }
    }
}
