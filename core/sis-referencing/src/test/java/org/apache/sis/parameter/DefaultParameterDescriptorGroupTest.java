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
import java.util.HashMap;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.opengis.test.Validators.*;
import static java.util.Collections.singletonMap;
import static org.opengis.referencing.IdentifiedObject.*;


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
     * A group of 4 parameters of type {@link Integer} with default value 10.
     * The two first parameters are mandatory, while the two last parameters are optional.
     * The very last parameter has a maximum number of occurrence of 2, which is illegal
     * according ISO 19111 but nevertheless supported by Apache SIS.
     */
    static final DefaultParameterDescriptorGroup M1_M1_O1_O2;
    static {
        final Integer DEFAULT_VALUE = 10;
        final Class<Integer> type = Integer.class;
        final Map<String,Object> properties = new HashMap<>(4);
        M1_M1_O1_O2 = new DefaultParameterDescriptorGroup(singletonMap(NAME_KEY, "Test group"), 0, 1,
            new DefaultParameterDescriptor<>(name(properties, "Mandatory 1", "Ambiguity"), type, null, null, DEFAULT_VALUE, true),
            new DefaultParameterDescriptor<>(name(properties, "Mandatory 2", "Alias 2"),   type, null, null, DEFAULT_VALUE, true),
            new DefaultParameterDescriptor<>(name(properties,  "Optional 3", "Alias 3"),   type, null, null, DEFAULT_VALUE, false),
            new MultiOccurrenceDescriptor <>(name(properties,  "Optional 4", "Ambiguity"), type, null, null, DEFAULT_VALUE, false)
        );
    }

    /**
     * Returns a map with {@code "name"}=<var>name</var> and {@code "alias"}=<var>alias</var> entries.
     */
    private static Map<String,Object> name(final Map<String,Object> properties, final String name, final String alias) {
        properties.put(NAME_KEY, name);
        properties.put(ALIAS_KEY, alias);
        return properties;
    }

    /**
     * Ensures that the constructor detects duplicated names.
     */
    @Test
    public void testConstruction() {
        final Class<Integer> type = Integer.class;
        final Map<String,Object> properties = new HashMap<>(4);
        final DefaultParameterDescriptor<Integer> p1, p2;
        p1 = new DefaultParameterDescriptor<>(name(properties, "Name", null), type, null, null, null, true);
        p2 = new DefaultParameterDescriptor<>(name(properties, "  NAME ", null), type, null, null, null, true);
        try {
            new DefaultParameterDescriptorGroup(singletonMap(NAME_KEY, "Test group"), 0, 1, p1, p2);
            fail("Constructor should have detected the duplicated names.");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("Name"));
            assertTrue(message, message.contains("NAME"));
        }
    }

    /**
     * Validates the test parameter descriptors created by {@link #createGroup_2M_2O()}.
     */
    @Test
    public void validateTestObjects() {
        for (final GeneralParameterDescriptor descriptor : M1_M1_O1_O2.descriptors()) {
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
        final DefaultParameterDescriptorGroup group = M1_M1_O1_O2;
        final List<GeneralParameterDescriptor> descriptors = group.descriptors();
        assertEquals("name", "Test group", group.getName().getCode());
        assertEquals("size", 4, descriptors.size());
        assertSame("descriptor(“Mandatory 1”)",  descriptors.get(0), group.descriptor("Mandatory 1"));
        assertSame("descriptor(“Optional 3”)",   descriptors.get(2), group.descriptor("Optional 3"));
        assertSame("descriptor(“Optional 4”)",   descriptors.get(3), group.descriptor("Optional 4"));
        assertSame("descriptor(“Mandatory 2”)",  descriptors.get(1), group.descriptor("Mandatory 2"));
        assertSame("descriptor(“Mandatory 2”)",  descriptors.get(1), group.descriptor("Alias 2"));
        assertSame("descriptor(“Optional 3”)",   descriptors.get(2), group.descriptor("Alias 3"));
        try {
            group.descriptor("Alias 1");
            fail("“Alias 1” is not a parameter for this group.");
        } catch (ParameterNotFoundException e) {
            final String message = e.getMessage();
            assertEquals(message, "Alias 1", e.getParameterName());
            assertTrue  (message, message.contains("Alias 1"));
            assertTrue  (message, message.contains("Test group"));
        }
    }

    /**
     * Verifies that {@link DefaultParameterDescriptorGroup#descriptor(String)} can detect ambiguities.
     */
    @Test
    public void testAmbiguityDetection() {
        try {
            M1_M1_O1_O2.descriptor("Ambiguity");
            fail("“Ambiguity” shall not be accepted since 2 parameters have this alias.");
        } catch (ParameterNotFoundException e) {
            final String message = e.getMessage();
            assertEquals(message, "Ambiguity", e.getParameterName());
            assertTrue  (message, message.contains("Ambiguity"));
            assertTrue  (message, message.contains("Mandatory 1"));
            assertTrue  (message, message.contains("Optional 4"));
        }
    }

    /**
     * Tests {@code DefaultParameterDescriptorGroup.descriptors().contains(Object)}.
     * The list returned by {@code descriptors()} provides a fast implementation based on {@code HashSet},
     * because this operation is requested everytime a new parameter is added or modified.
     */
    @Test
    public void testContains() {
        final List<GeneralParameterDescriptor> descriptors = M1_M1_O1_O2.descriptors();
        for (final GeneralParameterDescriptor p : descriptors) {
            assertTrue(descriptors.contains(p));
        }
    }

    /**
     * Tests {@link DefaultParameterDescriptorGroup} serialization.
     */
    @Test
    public void testSerialization() {
        assertSerializedEquals(M1_M1_O1_O2);
    }
}
