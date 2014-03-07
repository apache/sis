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

import java.util.Map;
import java.util.List;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Validators.*;
import static java.util.Collections.singletonMap;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;


/**
 * Tests the {@link DefaultParameterDescriptorGroup} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.4 (derived from geotk-2.1)
 * @version 0.4
 * @module
 */
@DependsOn(DefaultParameterDescriptorTest.class)
public final strictfp class DefaultParameterDescriptorGroupTest extends TestCase {
    /**
     * Returns a group of 4 parameters of type {@link Integer} with default value 10.
     * The two first parameters are mandatory, while the two last parameters are optional.
     * The very last parameter has a maximum number of occurrence of 2, which is illegal
     * according ISO 19111 but nevertheless supported by Apache SIS.
     */
    static final DefaultParameterDescriptorGroup createGroup_2M_2O() {
        final Integer DEFAULT_VALUE = 10;
        final Class<Integer> type = Integer.class;
        return new DefaultParameterDescriptorGroup(name("The group"), 0, 1,
            new DefaultParameterDescriptor<>(name("Mandatory 1"), type, null, null, DEFAULT_VALUE, true),
            new DefaultParameterDescriptor<>(name("Mandatory 2"), type, null, null, DEFAULT_VALUE, true),
            new DefaultParameterDescriptor<>(name( "Optional 3"), type, null, null, DEFAULT_VALUE, false),
            new MultiOccurrenceDescriptor <>(name( "Optional 4"), type, null, null, DEFAULT_VALUE, false)
        );
    }

    /**
     * Returns a map with only one entry, which is {@code "name"}=<var>name</var>.
     */
    static Map<String,String> name(final String name) {
        return singletonMap(NAME_KEY, name);
    }

    /**
     * Validates the test parameter descriptors created by {@link #createGroup_2M_2O()}.
     */
    @Test
    public void validateTestObjects() {
        for (final GeneralParameterDescriptor descriptor : createGroup_2M_2O().descriptors()) {
            AssertionError error = null;
            try {
                validate(descriptor);
            } catch (AssertionError e) {
                error = e;
            }
            if (descriptor instanceof MultiOccurrenceDescriptor) {
                assertNotNull("Validation methods should have detected that the descriptor is invalid.", error);
            } else if (error != null) {
                throw error;
            }
        }
    }

    /**
     * Tests {@link DefaultParameterDescriptorGroup#descriptor(String)}.
     */
    @Test
    public void testDescriptor() {
        final DefaultParameterDescriptorGroup group = createGroup_2M_2O();
        final List<GeneralParameterDescriptor> descriptors = group.descriptors();
        assertEquals("name", "The group", group.getName().getCode());
        assertEquals("size", 4, descriptors.size());
        assertSame("descriptor(“Mandatory 1”)",  descriptors.get(0), group.descriptor("Mandatory 1"));
        assertSame("descriptor(“Optional 3”)",   descriptors.get(2), group.descriptor("Optional 3"));
        assertSame("descriptor(“Optional 4”)",   descriptors.get(3), group.descriptor("Optional 4"));
        assertSame("descriptor(“Mandatory 2”)",  descriptors.get(1), group.descriptor("Mandatory 2"));
    }

    /**
     * Tests {@code DefaultParameterDescriptorGroup.descriptors().contains(Object)}.
     * The list returned by {@code descriptors()} provides a fast implementation based on {@code HashSet},
     * because this operation is requested everytime a new parameter is added or modified.
     */
    @Test
    public void testContains() {
        final List<GeneralParameterDescriptor> descriptors = createGroup_2M_2O().descriptors();
        for (final GeneralParameterDescriptor p : descriptors) {
            assertTrue(descriptors.contains(p));
        }
    }
}
