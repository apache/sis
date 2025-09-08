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
import static org.opengis.referencing.IdentifiedObject.*;
import org.apache.sis.util.privy.Constants;
import org.apache.sis.io.wkt.Convention;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.opengis.test.Validators.validate;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import static org.apache.sis.referencing.Assertions.assertWktEquals;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.parameter.ParameterDirection;


/**
 * Tests the {@link DefaultParameterDescriptorGroup} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 */
public final class DefaultParameterDescriptorGroupTest extends TestCase {
    /**
     * The default value used by the parameters in the {@link #M1_M1_O1_O2} descriptor.
     */
    static final Integer DEFAULT_VALUE = 10;

    /**
     * A group of 4 parameters of type {@link Integer} with default value 10.
     * The two first parameters are mandatory, while the two last parameters are optional.
     * The very last parameter has a maximum number of occurrence of 2, which is illegal
     * according ISO 19111 but nevertheless supported by Apache SIS.
     */
    public static final DefaultParameterDescriptorGroup M1_M1_O1_O2;
    static {
        final Class<Integer> type = Integer.class;
        final Map<String,Object> properties = new HashMap<>(4);
        M1_M1_O1_O2 = new DefaultParameterDescriptorGroup(Map.of(NAME_KEY, "Test group"), 0, 1,
            new DefaultParameterDescriptor<>(name(properties, "Mandatory 1", "Ambiguity"), 1, 1, type, null, null, DEFAULT_VALUE),
            new DefaultParameterDescriptor<>(name(properties, "Mandatory 2", "Alias 2"),   1, 1, type, null, null, DEFAULT_VALUE),
            new DefaultParameterDescriptor<>(name(properties, "Optional 3",  "Alias 3"),   0, 1, type, null, null, DEFAULT_VALUE),
            new DefaultParameterDescriptor<>(name(properties, "Optional 4",  "Ambiguity"), 0, 2, type, null, null, DEFAULT_VALUE)
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
     * Creates a new test case.
     */
    public DefaultParameterDescriptorGroupTest() {
    }

    /**
     * Ensures that the constructor detects duplicated names.
     */
    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstruction() {
        final Class<Integer> type = Integer.class;
        final Map<String,Object> properties = new HashMap<>(4);
        final DefaultParameterDescriptor<Integer> p1, p2;
        p1 = new DefaultParameterDescriptor<>(name(properties,   "Name",  null), 1, 1, type, null, null, null);
        p2 = new DefaultParameterDescriptor<>(name(properties, "  NAME ", null), 1, 1, type, null, null, null);

        var e = assertThrows(IllegalArgumentException.class,
                () -> new DefaultParameterDescriptorGroup(Map.of(NAME_KEY, "Test group"), 0, 1, p1, p2));
        assertMessageContains(e, "Name", "NAME");
    }

    /**
     * Validates the test parameter descriptors given by {@link #M1_M1_O1_O2}.
     */
    @Test
    public void validateTestObjects() {
        assertEquals(ParameterDirection.IN, M1_M1_O1_O2.getDirection());
        for (final GeneralParameterDescriptor descriptor : M1_M1_O1_O2.descriptors()) {
            AssertionError error = null;
            try {
                validate(descriptor);
            } catch (AssertionError e) {
                error = e;
            }
            if (descriptor.getMaximumOccurs() > 1) {
                assertNotNull(error, "Validation methods should have detected that the descriptor is invalid.");
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
        assertEquals("Test group", group.getName().getCode());
        assertEquals(4, descriptors.size());
        assertSame(descriptors.get(0), group.descriptor("Mandatory 1"));
        assertSame(descriptors.get(2), group.descriptor("Optional 3"));
        assertSame(descriptors.get(3), group.descriptor("Optional 4"));
        assertSame(descriptors.get(1), group.descriptor("Mandatory 2"));
        assertSame(descriptors.get(1), group.descriptor("Alias 2"));
        assertSame(descriptors.get(2), group.descriptor("Alias 3"));

        var e = assertThrows(ParameterNotFoundException.class, () -> group.descriptor("Alias 1"));
        assertEquals("Alias 1", e.getParameterName());
        assertMessageContains(e, "Alias 1", "Test group");
    }

    /**
     * Verifies that {@link DefaultParameterDescriptorGroup#descriptor(String)} can detect ambiguities.
     */
    @Test
    public void testAmbiguityDetection() {
        var e = assertThrows(ParameterNotFoundException.class, () -> M1_M1_O1_O2.descriptor("Ambiguity"),
                             "“Ambiguity” shall not be accepted because 2 parameters have this alias.");
        assertEquals("Ambiguity", e.getParameterName());
        assertMessageContains(e, "Ambiguity", "Mandatory 1", "Optional 4");
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
     * Tests the WKT representation.
     */
    @Test
    public void testWKT() {
        assertWktEquals(Convention.WKT2,
                "PARAMETERGROUP[“Test group”,\n" +
                "  PARAMETER[“Mandatory 1”, 10],\n" +
                "  PARAMETER[“Mandatory 2”, 10],\n" +
                "  PARAMETER[“Optional 3”, 10],\n" +
                "  PARAMETER[“Optional 4”, 10]]", M1_M1_O1_O2);

        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "ParameterGroup[“Test group”,\n" +
                "  Parameter[“Mandatory 1”, 10],\n" +
                "  Parameter[“Mandatory 2”, 10],\n" +
                "  Parameter[“Optional 3”, 10],\n" +
                "  Parameter[“Optional 4”, 10]]", M1_M1_O1_O2);
    }

    /**
     * Tests WKT formatting of a group with a parameter having an identifier.
     *
     * @see DefaultParameterDescriptorTest#testIdentifiedParameterWKT()
     */
    @Test
    public void testIdentifiedParameterWKT() {
        /*
         * Test below is identical to DefaultParameterDescriptorTest.testIdentifiedParameterWKT(),
         * but is reproduced here for easier comparison with the test following it.
         */
        final DefaultParameterDescriptor<Double> descriptor = DefaultParameterDescriptorTest.createEPSG("A0", Constants.EPSG_A0);
        assertWktEquals(Convention.WKT2, "PARAMETER[“A0”, ID[“EPSG”, 8623, URI[“urn:ogc:def:parameter:EPSG::8623”]]]", descriptor);
        /*
         * When the parameter is part of a larger element, we expect a simplification.
         * Here, the URI should be omitted because it is a long value which does not
         * bring new information, since it is computed from other values.
         */
        final var group = new DefaultParameterDescriptorGroup(Map.of(NAME_KEY, "Affine"), 1, 1, descriptor);
        assertWktEquals(Convention.WKT2,
                "PARAMETERGROUP[“Affine”,\n" +
                "  PARAMETER[“A0”, ID[“EPSG”, 8623]]]", group);
    }

    /**
     * Tests {@link DefaultParameterDescriptorGroup} serialization.
     */
    @Test
    public void testSerialization() {
        assertSerializedEquals(M1_M1_O1_O2);
    }
}
