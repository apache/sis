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
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the {@link UnmodifiableParameterValueGroup} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn(UnmodifiableParameterValueTest.class)
public final strictfp class UnmodifiableParameterValueGroupTest extends TestCase {
    /**
     * Tests creation of an {@link UnmodifiableParameterValueGroup} and verify the values.
     */
    @Test
    public void testCreate() {
        ParameterValueGroup group = DefaultParameterDescriptorGroupTest.M1_M1_O1_O2.createValue();
        group.parameter("Mandatory 1").setValue(5);
        group.parameter("Optional 3") .setValue(8);
        group = UnmodifiableParameterValueGroup.create(group);

        assertEquals("values.size()",             3,  group.values().size());
        assertEquals("values[0].name", "Mandatory 1", group.values().get(0).getDescriptor().getName().toString());
        assertEquals("values[1].name", "Mandatory 2", group.values().get(1).getDescriptor().getName().toString());
        assertEquals("values[2].name", "Optional 3",  group.values().get(2).getDescriptor().getName().toString());
        assertEquals("values[0].value",           5,  group.parameter("Mandatory 1").getValue());
        assertEquals("values[1].value",          10,  group.parameter("Mandatory 2").getValue());
        assertEquals("values[2].value",           8,  group.parameter("Optional 3") .getValue());

        try {
            group.groups("dummy");
            fail("Shall not return non-existent groups.");
        } catch (ParameterNotFoundException e) {
            assertTrue(e.getMessage().contains("Test group"));
            assertTrue(e.getMessage().contains("dummy"));
        }
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
        try {
            param.setValue(5);
            fail("Shall not allow modification.");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("ParameterValue"));
        }
        try {
            group.addGroup("dummy");
            fail("Shall not allow modification.");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("ParameterValueGroup"));
        }
    }
}
