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
import org.apache.sis.metadata.simple.SimpleAttributeType;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import static org.apache.sis.test.Assertions.assertSingleton;


/**
 * Tests the {@link DefaultRecordType} implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class DefaultRecordTypeTest extends TestCase {
    /** Value of {@link DefaultRecordType#getContainer()}. */
    private final DefaultRecordSchema container;

    /** Value of {@link DefaultRecordType#getTypeName()}.   */ private DefaultTypeName   recordTypeName;
    /** Value of {@link DefaultRecordType#getMembers()}.    */ private DefaultMemberName fieldName;
    /** Value of {@link DefaultRecordType#getFieldTypes()}. */ private DefaultTypeName   fieldTypeName;

    /**
     * Creates a new test case.
     */
    public DefaultRecordTypeTest() {
        final DefaultNameSpace recordNamespace;
        final DefaultNameSpace fieldNamespace;

        recordNamespace = new DefaultNameSpace (null, "MyNameSpace", ":", ":");
        recordTypeName  = new DefaultTypeName  (recordNamespace, "MyRecordType");
        fieldNamespace  = new DefaultNameSpace (recordNamespace, "MyRecordType", ":", ":");
        fieldTypeName   = new DefaultTypeName  (new DefaultNameSpace(null, "gco", ":", ":"), "Integer");
        fieldName       = new DefaultMemberName(fieldNamespace, "aMember", fieldTypeName);
        container       = new DefaultRecordSchema("MyNameSpace");
        assertEquals("MyNameSpace:MyRecordType:aMember", fieldName.toFullyQualifiedName().toString());
    }

    /**
     * Creates a new record type from the current values of private fields.
     */
    @SuppressWarnings("deprecation")
    private DefaultRecordType create() throws IllegalArgumentException {
        final Type fieldType = new SimpleAttributeType<>(fieldTypeName, Integer.class);
        return new DefaultRecordType(recordTypeName, container, Map.of(fieldName, fieldType));
    }

    /**
     * Tests the construction of {@link DefaultRecordType}, and opportunistically tests
     * {@link DefaultRecordType#locate(MemberName)}.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testConstructor() {
        final DefaultRecordType type = create();
        assertEquals(1, type.size());
        assertEquals(Integer.TYPE, type.baseValueClass());

        // Public properties
        assertSame(container,      type.getContainer());
        assertSame(recordTypeName, type.getTypeName());
        assertSame(fieldName,      assertSingleton(type.getMembers()));
        assertSame(fieldName,      assertSingleton(type.getFieldTypes().keySet()));
        assertSame(fieldTypeName,  assertSingleton(type.getFieldTypes().values()).getTypeName());
        assertSame(fieldTypeName,  type.locate(fieldName));
        assertNull(                type.locate(new DefaultMemberName(null, "otherMember", fieldTypeName)));
    }

    /**
     * Ensures that constructions of {@link DefaultRecordType} with
     * inconsistent arguments throw an exception.
     */
    @Test
    public void testArgumentChecks() {
        final DefaultTypeName  correctRecordName      = recordTypeName;
        final NameSpace        correctMemberNamespace = fieldName.scope();
        final DefaultNameSpace wrongNamespace         = new DefaultNameSpace(null, "WrongNameSpace", ":", ":");
        /*
         * RecordType namespace validation.
         * Constructor shall require "MyNameSpace:MyRecordType".
         */
        recordTypeName = new DefaultTypeName(wrongNamespace, "MyRecordType");
        var e = assertThrows(IllegalArgumentException.class, () -> create(),
                             "Should have detected namespace mismatch.");
        assertMessageContains(e, "MyNameSpace", "WrongNameSpace:MyRecordType");
        /*
         * MemberName namespace validation.
         * Constructor shall require "MyNameSpace:MyRecordType:aMember".
         */
        recordTypeName = correctRecordName;
        fieldName = new DefaultMemberName(wrongNamespace, "aMember", fieldTypeName);
        e = assertThrows(IllegalArgumentException.class, () -> create(),
                         "Should have detected namespace mismatch.");
        assertMessageContains(e, "MyNameSpace:MyRecordType", "WrongNameSpace:aMember");
        /*
         * MemberName type validation.
         */
        final var otherType = new DefaultTypeName(fieldTypeName.scope(), "Real");
        fieldName = new DefaultMemberName(correctMemberNamespace, "aMember", otherType);
        e = assertThrows(IllegalArgumentException.class, () -> create(),
                         "Should have detected type mismatch.");
        assertMessageContains(e, "aMember", "gco:Integer");
    }

    /**
     * Tests serialization of a {@code RecordType}.
     */
    @Test
    public void testSerialization() {
        assertSerializedEquals(create());
    }
}
