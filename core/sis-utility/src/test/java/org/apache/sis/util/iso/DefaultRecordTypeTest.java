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

import java.util.Collections;
import org.opengis.util.Type;
import org.opengis.util.MemberName;
import org.opengis.util.NameSpace;
import org.apache.sis.internal.simple.SimpleAttributeType;

// Test imports
import org.junit.Test;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests the {@link DefaultRecordType} implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@DependsOn(TypeNamesTest.class)
public final strictfp class DefaultRecordTypeTest extends TestCase {
    /** Value of {@link DefaultRecordType#getContainer()}.   */ private DefaultRecordSchema container;
    /** Value of {@link DefaultRecordType#getTypeName()}.    */ private DefaultTypeName     recordTypeName;
    /** Value of {@link DefaultRecordType#getMembers()}.     */ private DefaultMemberName   memberName;
    /** Value of {@link DefaultRecordType#getMemberTypes()}. */ private DefaultTypeName     memberTypeName;

    /**
     * Initializes the private fields.
     * This method shall be invoked only once per test.
     */
    private void init() {
        final DefaultNameSpace recordNamespace;
        final DefaultNameSpace memberNamespace;

        recordNamespace = new DefaultNameSpace (null, "MyNameSpace", ":", ":");
        recordTypeName  = new DefaultTypeName  (recordNamespace, "MyRecordType");
        memberNamespace = new DefaultNameSpace (recordNamespace, "MyRecordType", ":", ":");
        memberTypeName  = new DefaultTypeName  (new DefaultNameSpace(null, "gco", ":", ":"), "Integer");
        memberName      = new DefaultMemberName(memberNamespace, "aMember", memberTypeName);
        container       = new SerializableRecordSchema("MyNameSpace");
        assertEquals("MyNameSpace:MyRecordType:aMember", memberName.toFullyQualifiedName().toString());
    }

    /**
     * Creates a new record type from the current values of private fields.
     */
    private DefaultRecordType create() throws IllegalArgumentException {
        final Type memberType = new SimpleAttributeType<Integer>(memberTypeName, Integer.class);
        return new DefaultRecordType(recordTypeName, container, Collections.singletonMap(memberName, memberType));
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
        assertSame("container",   container,      type.getContainer());
        assertSame("typeName",    recordTypeName, type.getTypeName());
        assertSame("members",     memberName,     getSingleton(type.getMembers()));
        assertSame("memberTypes", memberName,     getSingleton(type.getMemberTypes().keySet()));
        assertSame("memberTypes", memberTypeName, getSingleton(type.getMemberTypes().values()).getTypeName());
        assertSame("locate",      memberTypeName, type.locate(memberName));
        assertNull("locate",                      type.locate(new DefaultMemberName(null, "otherMember", memberTypeName)));
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
        final NameSpace        correctMemberNamespace = memberName.scope();
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
        memberName = new DefaultMemberName(wrongNamespace, "aMember", memberTypeName);
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
        final DefaultTypeName otherType = new DefaultTypeName(memberTypeName.scope(), "Real");
        memberName = new DefaultMemberName(correctMemberNamespace, "aMember", otherType);
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
