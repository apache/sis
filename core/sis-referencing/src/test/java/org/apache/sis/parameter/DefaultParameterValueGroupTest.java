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

import java.util.List;
import java.util.Arrays;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Validators.*;
import static org.apache.sis.parameter.DefaultParameterDescriptorGroupTest.name;


/**
 * Tests the {@link DefaultParameterValueGroup} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.1)
 * @version 0.4
 * @module
 */
@DependsOn({
    DefaultParameterDescriptorGroupTest.class,
    DefaultParameterValueTest.class
})
public final strictfp class DefaultParameterValueGroupTest extends TestCase {
    /**
     * Creates values for all parameters defines by the given descriptor,
     * and assigns to them an integer value in sequence with the given step.
     */
    private static DefaultParameterValue<?>[] createValues(
            final List<GeneralParameterDescriptor> descriptors, final int step)
    {
        final DefaultParameterValue<?>[] parameters = new DefaultParameterValue<?>[descriptors.size()];
        for (int i=0; i<parameters.length;) {
            parameters[i] = new DefaultParameterValue<>((ParameterDescriptor<?>) descriptors.get(i));
            parameters[i].setValue(++i * step);
        }
        return parameters;
    }

    /**
     * Validates the test parameter values created by {@link #createValues(List, int)}.
     */
    @Test
    public void validateTestObjects() {
        final DefaultParameterDescriptorGroup group = DefaultParameterDescriptorGroupTest.createGroup_2M_2O();
        for (final DefaultParameterValue<?> param : createValues(group.descriptors(), 10)) {
            AssertionError error = null;
            try {
                validate(param);
            } catch (AssertionError e) {
                error = e;
            }
            if (param.getDescriptor() instanceof MultiOccurrenceDescriptor) {
                assertNotNull("Validation methods should have detected that the descriptor is invalid.", error);
            } else if (error != null) {
                throw error;
            }
        }
    }

    /**
     * Tests {@code DefaultParameterValueGroup.values().addAll(…)}.
     */
    @Test
    public void testAddAll() {
        final DefaultParameterDescriptorGroup descriptor = DefaultParameterDescriptorGroupTest.createGroup_2M_2O();
        final DefaultParameterValueGroup group = new DefaultParameterValueGroup(descriptor);
        final List<GeneralParameterValue> values = group.values();
        assertEquals("Initial size", 2, values.size());

        final DefaultParameterValue<?>[] external = createValues(descriptor.descriptors(), 10);
        assertTrue(values.addAll(Arrays.asList(external)));
        assertEquals("Final size", external.length, values.size());
        for (int i=0; i<external.length; i++) {
            assertSame(external[i], values.get(i));
        }
    }

    /**
     * Tests the {@link DefaultParameterValueGroup#addGroup(String)} method.
     * Ensures the descriptor is found and the new value correctly inserted.
     */
    @Test
    public void testAddGroup() {
        final ParameterDescriptorGroup groupDesc = new DefaultParameterDescriptorGroup(name("theGroup"), 1, 1,
                new DefaultParameterDescriptorGroup(name("theSubGroup"), 0, 10)
        );
        validate(groupDesc);

        final ParameterValueGroup groupValues = groupDesc.createValue();
        assertEquals("Size before add.", 0, groupValues.values().size());
        final ParameterValueGroup subGroupValues = groupValues.addGroup("theSubGroup");
        assertEquals("Size after add.", 1, groupValues.values().size());
        assertSame(subGroupValues, groupValues.values().get(0));
    }
}
