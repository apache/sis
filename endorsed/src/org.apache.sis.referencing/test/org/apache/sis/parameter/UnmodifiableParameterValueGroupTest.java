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
package org.apache.sis.parameter;

import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

// Test dependencies
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;


/**
 * Tests the {@link UnmodifiableParameterValueGroup} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@DependsOn(UnmodifiableParameterValueTest.class)
public final class UnmodifiableParameterValueGroupTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public UnmodifiableParameterValueGroupTest() {
    }

    /**
     * Tests creation of an {@link UnmodifiableParameterValueGroup} and verify the values.
     */
    @Test
    public void testCreate() {
        ParameterValueGroup group = DefaultParameterDescriptorGroupTest.M1_M1_O1_O2.createValue();
        group.parameter("Mandatory 1").setValue(5);
        group.parameter("Optional 3") .setValue(8);
        group = UnmodifiableParameterValueGroup.create(group);

        assertEquals(           3,  group.values().size());
        assertEquals("Mandatory 1", group.values().get(0).getDescriptor().getName().toString());
        assertEquals("Mandatory 2", group.values().get(1).getDescriptor().getName().toString());
        assertEquals("Optional 3",  group.values().get(2).getDescriptor().getName().toString());
        assertEquals(           5,  group.parameter("Mandatory 1").getValue());
        assertEquals(          10,  group.parameter("Mandatory 2").getValue());
        assertEquals(           8,  group.parameter("Optional 3") .getValue());

        var g = group;      // Because lambda expressions want final variable.
        var e = assertThrows(ParameterNotFoundException.class, () -> g.groups("dummy"),
                             "Shall not return non-existent groups.");
        assertMessageContains(e, "Test group", "dummy");
    }

    /**
     * Ensures that {@link UnmodifiableParameterValueGroup} is unmodifiable.
     */
    @Test
    public void ensureUnmodifiable() {
        ParameterValueGroup group = DefaultParameterDescriptorGroupTest.M1_M1_O1_O2.createValue();
        group.parameter("Mandatory 1").setValue(5);
        group.parameter("Optional 3") .setValue(8);
        group = UnmodifiableParameterValueGroup.create(group);
        ParameterValue<?> param = group.parameter("Mandatory 1");

        UnsupportedOperationException e;
        e = assertThrows(UnsupportedOperationException.class, () -> param.setValue(5));
        assertMessageContains(e, "ParameterValue");

        var g = group;      // Because lambda expressions want final variable.
        e = assertThrows(UnsupportedOperationException.class, () -> g.addGroup("dummy"));
        assertMessageContains(e, "ParameterValueGroup");
    }
}
