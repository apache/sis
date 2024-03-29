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
import java.util.ArrayList;
import java.util.Collections;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.InvalidParameterNameException;
import org.opengis.parameter.InvalidParameterCardinalityException;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import org.apache.sis.util.ComparisonMode;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.opengis.test.Validators.validate;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSetEquals;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import static org.apache.sis.test.Assertions.assertMessageContains;


/**
 * Tests the {@link DefaultParameterValueGroup} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public final class DefaultParameterValueGroupTest extends TestCase {
    /**
     * The descriptors of parameters to be tested by this class.
     * The default descriptors are:
     * <ul>
     *   <li>One mandatory parameter (multiplicity [1…1]).</li>
     *   <li>One mandatory parameter (multiplicity [1…1]).</li>
     *   <li>One optional  parameter (multiplicity [0…1]).</li>
     *   <li>One optional  parameter (multiplicity [0…2]) — invalid according ISO 19111, but supported by SIS.</li>
     * </ul>
     *
     * Some test methods may replace the default descriptor by another one.
     */
    private ParameterDescriptorGroup descriptor;

    /**
     * Creates a new test case.
     */
    public DefaultParameterValueGroupTest() {
        descriptor = DefaultParameterDescriptorGroupTest.M1_M1_O1_O2;
    }

    /**
     * Creates values for all parameters defined by the {@linkplain #descriptor} (regardless their multiplicity),
     * and assigns to them an integer value in sequence with the given step. For example if {@code step} is 10,
     * then this method will create parameters with values 10, 20, 30 and 40.
     */
    private DefaultParameterValue<?>[] createValues(final int step) {
        final List<GeneralParameterDescriptor> descriptors = descriptor.descriptors();
        final DefaultParameterValue<?>[] parameters = new DefaultParameterValue<?>[descriptors.size()];
        for (int i=0; i<parameters.length;) {
            parameters[i] = new DefaultParameterValue<>((ParameterDescriptor<?>) descriptors.get(i));
            parameters[i].setValue(++i * step);
        }
        return parameters;
    }

    /**
     * Same as {@link #createValues(int)}, but put the parameters in a {@link DefaultParameterValueGroup}.
     *
     * @see #testValuesAddAll()
     */
    private DefaultParameterValueGroup createGroup(final int step) {
        final DefaultParameterValueGroup group = new DefaultParameterValueGroup(descriptor);
        Collections.addAll(group.values(), createValues(step));
        return group;
    }

    /**
     * Validates the test parameter values created by {@link #createValues(int)}.
     */
    @Test
    public void validateTestObjects() {
        for (final DefaultParameterValue<?> param : createValues(10)) {
            AssertionError error = null;
            try {
                validate(param);
            } catch (AssertionError e) {
                error = e;
            }
            if (param.getDescriptor().getMaximumOccurs() > 1) {
                assertNotNull(error, "Validation methods should have detected that the descriptor is invalid.");
            } else if (error != null) {
                throw error;
            }
        }
    }

    /**
     * Tests {@link DefaultParameterValueGroup#parameter(String)}.
     */
    @Test
    public void testParameter() {
        final List<GeneralParameterDescriptor> descriptors = descriptor.descriptors();
        final GeneralParameterValue[] expected = {
            descriptors.get(0).createValue(),
            descriptors.get(1).createValue(),
            descriptors.get(2).createValue(),
            descriptors.get(3).createValue()
        };
        final DefaultParameterValueGroup  group  = new DefaultParameterValueGroup(descriptor);
        assertEquals(expected[0], group.parameter("Mandatory 1"));
        assertEquals(expected[1], group.parameter("Mandatory 2"));
        assertEquals(expected[2], group.parameter("Optional 3"));
        assertEquals(expected[3], group.parameter("Optional 4"));
        assertEquals(expected[1], group.parameter("Alias 2"));
        assertEquals(expected[2], group.parameter("Alias 3"));
    }

    /**
     * Tests {@code DefaultParameterValueGroup.values().clear()}.
     */
    @Test
    public void testValuesClear() {
        final DefaultParameterValueGroup  group  = createGroup(10);
        final List<GeneralParameterValue> values = group.values();
        assertEquals(4, values.size());
        assertEquals(20, group.parameter("Mandatory 2").intValue());
        values.clear();
        assertEquals(2, values.size());
        assertEquals(DefaultParameterDescriptorGroupTest.DEFAULT_VALUE, group.parameter("Mandatory 2").getValue());
    }

    /**
     * Tests {@code DefaultParameterValueGroup.values().get(…)} on a group expected to be pre-filled
     * with mandatory parameters.
     */
    @Test
    public void testValuesGet() {
        final DefaultParameterValueGroup  group  = new DefaultParameterValueGroup(descriptor);
        final List<GeneralParameterValue> values = group.values();
        assertEquals(2, values.size());
        assertEquals(descriptor.descriptors().get(0).createValue(), values.get(0));
        assertEquals(descriptor.descriptors().get(1).createValue(), values.get(1));
        var e = assertThrows(IndexOutOfBoundsException.class, () -> values.get(2));
        assertMessageContains(e);
        assertEquals(DefaultParameterDescriptorGroupTest.DEFAULT_VALUE, ((ParameterValue<?>) values.get(0)).getValue());
        assertEquals(DefaultParameterDescriptorGroupTest.DEFAULT_VALUE, ((ParameterValue<?>) values.get(1)).getValue());
    }

    /**
     * Tests {@code DefaultParameterValueGroup.values().set(…)}.
     */
    @Test
    public void testValuesSet() {
        final DefaultParameterValueGroup  group  = new DefaultParameterValueGroup(descriptor);
        final List<GeneralParameterValue> values = group.values();
        assertEquals(2, values.size());
        final ParameterValue<?> p0 = (ParameterValue<?>) descriptor.descriptors().get(0).createValue();
        final ParameterValue<?> p1 = (ParameterValue<?>) descriptor.descriptors().get(1).createValue();
        p0.setValue(4);
        p1.setValue(5);
        assertEquals("Mandatory 1", values.set(0, p0).getDescriptor().getName().toString());
        assertEquals("Mandatory 2", values.set(1, p1).getDescriptor().getName().toString());
        var e = assertThrows(IndexOutOfBoundsException.class, () -> values.set(2, p1));
        assertMessageContains(e);
        assertEquals(2, values.size()); // Size should be unchanged.
        assertEquals(4, ((ParameterValue<?>) values.get(0)).intValue());
        assertEquals(5, ((ParameterValue<?>) values.get(1)).intValue());
    }

    /**
     * Tests {@code DefaultParameterValueGroup.values().contains(…)}.
     */
    @Test
    public void testValuesContains() {
        final GeneralParameterValue[] positives = createValues(+10);
        final GeneralParameterValue[] negatives = createValues(-10);
        final List<GeneralParameterValue> values = createGroup(+10).values();
        for (int i=0; i<positives.length; i++) {
            assertTrue (values.contains(positives[i]));
            assertFalse(values.contains(negatives[i]));
        }
    }

    /**
     * Tests {@code DefaultParameterValueGroup.values().addAll(…)}.
     */
    @Test
    public void testValuesAddAll() {
        final DefaultParameterValueGroup  group  = new DefaultParameterValueGroup(descriptor);
        final List<GeneralParameterValue> values = group.values();
        assertEquals(2, values.size());

        final DefaultParameterValue<?>[] parameters = createValues(10);
        assertTrue(Collections.addAll(values, parameters));
        assertEquals(parameters.length, values.size());
        for (int i=0; i<parameters.length; i++) {
            assertSame(parameters[i], values.get(i));
        }
    }

    /**
     * Tests {@code DefaultParameterValueGroup.values().addAll(…)} with subgroups.
     */
    @Test
    public void testValuesAddAllWithSubgroups() {
        final DefaultParameterDescriptorGroup group, subGroup;
        final List<GeneralParameterDescriptor> descriptors = new ArrayList<>(descriptor.descriptors());
        subGroup = new DefaultParameterDescriptorGroup(Map.of(NAME_KEY, "theSubGroup"),
                2, 4, descriptors.toArray(GeneralParameterDescriptor[]::new));
        descriptors.add(subGroup);
        group = new DefaultParameterDescriptorGroup(Map.of(NAME_KEY, "theGroup"),
                descriptors.toArray(GeneralParameterDescriptor[]::new));
        /*
         * Prepare the GeneralParameterValue instances that we are going to add in the above group.
         * We assign arbitrary integer values to each instance if order to be able to differentiate
         * them, but the purpose of this test is not to verify those integer values.
         *
         * We intentionally:
         *   - Omit the creation of a mandatory parameter value
         *   - Create more sub-groups than the minimum required.
         */
        final ParameterValue<?>   v2 = (ParameterValue<?>) descriptor.descriptor("Mandatory 2").createValue();
        final ParameterValue<?>   v3 = (ParameterValue<?>) descriptor.descriptor( "Optional 3").createValue();
        final ParameterValueGroup g1 = subGroup.createValue();
        final ParameterValueGroup g2 = subGroup.createValue();
        final ParameterValueGroup g3 = subGroup.createValue();
        v2.setValue(4);
        v3.setValue(8);
        g1.parameter("Mandatory 1").setValue(3);
        g2.parameter( "Optional 4").setValue(7);
        g3.parameter("Mandatory 2").setValue(5);
        final List<GeneralParameterValue> expected = new ArrayList<>(6);
        assertTrue(expected.add(v2));
        assertTrue(expected.add(v3));
        assertTrue(expected.add(g1));
        assertTrue(expected.add(g2));
        assertTrue(expected.add(g3));
        /*
         * A newly created group should be initialized with 4 GeneralParameterValue instances because of
         * the mandatory ones. After we added our own instances created above, the group should contain
         * 6 instances (one more than what we added) because of the "Mandatory 1" parameter that we did
         * not provided. Note that the element order in the 'values' collection does not need to be the
         * order in which we provided our GeneralParameterValue instances.
         */
        final List<GeneralParameterValue> values = group.createValue().values();
        assertEquals(4, values.size());
        assertTrue  (values.addAll(expected));
        assertEquals(6, values.size());
        final ParameterValue<?> v1 = (ParameterValue<?>) values.get(0);
        assertEquals(DefaultParameterDescriptorGroupTest.DEFAULT_VALUE, v1.getValue());
        assertTrue(expected.add(v1));
        assertSetEquals(expected, values);
    }

    /**
     * Tests that attempts to add an invalid parameter cause an {@link InvalidParameterNameException} to be thrown.
     */
    @Test
    public void testValuesAddWrongParameter() {
        final DefaultParameterValueGroup    group = createGroup(10);
        final List<GeneralParameterValue>  values = group.values();
        final ParameterValue<Integer> nonExistent = new DefaultParameterDescriptor<>(
                Map.of(NAME_KEY, "Optional 5"), 0, 1, Integer.class, null, null, null).createValue();

        var e = assertThrows(InvalidParameterNameException.class,
                () -> values.add(nonExistent),
                "“Optional 5” is not a parameter for this group.");
        assertEquals("Optional 5", e.getParameterName());
        assertMessageContains(e, "Optional 5", "Test group");
    }

    /**
     * Tests {@code DefaultParameterValueGroup.values().add(…)}. In particular, tests that attempt to add
     * a parameter already present causes an {@link InvalidParameterCardinalityException} to be thrown.
     */
    @Test
    public void testValuesAdd() {
        final GeneralParameterValue[]      toAdd = createValues(-10);
        final DefaultParameterValueGroup   group = new DefaultParameterValueGroup(descriptor);
        final List<GeneralParameterValue> values = group.values();
        assertEquals(2, values.size());     // Because the descriptor declares 2 parameters as mandatory.
        assertTrue  (values.add(toAdd[0])); assertEquals(2, values.size());
        assertTrue  (values.add(toAdd[1])); assertEquals(2, values.size());
        assertTrue  (values.add(toAdd[2])); assertEquals(3, values.size());
        assertTrue  (values.add(toAdd[3])); assertEquals(4, values.size());
        /*
         * Test [1…1] multiplicity.
         */
        InvalidParameterCardinalityException e;
        e = assertThrows(InvalidParameterCardinalityException.class,
                () -> values.add(toAdd[1]),
                "“Mandatory 2” is already present in this group.");
        assertEquals("Mandatory 2", e.getParameterName());
        assertMessageContains(e, "Mandatory 2");
        assertEquals(4, values.size());             // Size shall be unchanged.
        /*
         * Test [0…1] multiplicity.
         */
        e = assertThrows(InvalidParameterCardinalityException.class,
                () -> values.add(toAdd[2]),
                "“Optional 3” is already present in this group.");
        assertEquals("Optional 3", e.getParameterName());
        assertMessageContains(e, "Optional 3");
        assertEquals(4, values.size());             // Size shall be unchanged.
        /*
         * Test [0…2] multiplicity.
         */
        assertTrue(values.add(toAdd[3]));
        assertEquals(5, values.size());
        /*
         * Verifies parameter values.
         */
        assertEquals(-10, group.parameter("Mandatory 1").intValue());
        assertEquals(-20, group.parameter("Mandatory 2").intValue());
        assertEquals(-30, group.parameter( "Optional 3").intValue());
    }

    /**
     * Tests {@code DefaultParameterValueGroup.values().remove(…)}. In particular, tests that attempt to
     * remove a mandatory parameter causes an {@link InvalidParameterCardinalityException} to be thrown.
     */
    @Test
    public void testValuesRemove() {
        final GeneralParameterValue[]  negatives = createValues(-10);
        final DefaultParameterValueGroup   group = createGroup(10);
        final List<GeneralParameterValue> values = group.values();
        assertFalse(values.remove(negatives[0]));                       // Non-existant parameter.

        var e = assertThrows(InvalidParameterCardinalityException.class,
                () -> values.remove(values.get(0)),
                "“Mandatory 1” is a mandatory parameter; it should not be removeable.");
        assertEquals("Mandatory 1", e.getParameterName());
        assertMessageContains(e, "Mandatory 1");
    }

    /**
     * Tests the {@link DefaultParameterValueGroup#addGroup(String)} method.
     * Ensures the descriptor is found and the new value correctly inserted.
     */
    @Test
    public void testAddGroup() {
        descriptor = new DefaultParameterDescriptorGroup(Map.of(NAME_KEY, "theGroup"), 1, 1,
                new DefaultParameterDescriptorGroup(Map.of(NAME_KEY, "theSubGroup"), 0, 10)
        );
        validate(descriptor);

        final ParameterValueGroup groupValues = descriptor.createValue();
        assertEquals(0, groupValues.values().size(), "Size before add.");
        final ParameterValueGroup subGroupValues = groupValues.addGroup("theSubGroup");
        assertEquals(1, groupValues.values().size(), "Size after add.");
        assertSame(subGroupValues, groupValues.values().get(0));
        assertArrayEquals(new Object[] {subGroupValues}, groupValues.groups("theSubGroup").toArray());
    }

    /**
     * Tests {@link #equals(Object)} and {@link #hashCode()} methods.
     */
    @Test
    public void testEqualsAndHashCode() {
        final DefaultParameterValueGroup g1 = createGroup( 10);
        final DefaultParameterValueGroup g2 = createGroup(-10);
        final DefaultParameterValueGroup g3 = createGroup( 10);
        assertTrue  (g1.equals(g1));
        assertFalse (g1.equals(g2));
        assertTrue  (g1.equals(g3));
        assertEquals(g1.hashCode(), g3.hashCode());
    }

    /**
     * Tests {@link DefaultParameterValueGroup#equals(Object, ComparisonMode)}.
     */
    @Test
    public void testEqualsIgnoreMetadata() {
        final DefaultParameterValueGroup g1 = createGroup(10);
        final DefaultParameterValueGroup g2 = new DefaultParameterValueGroup(g1.getDescriptor());
        final List<GeneralParameterValue> values = new ArrayList<>(g1.values());
        Collections.swap(values, 2, 3);
        g2.values().addAll(values);

        assertFalse(g1.equals(g2, ComparisonMode.STRICT));
        assertFalse(g1.equals(g2, ComparisonMode.BY_CONTRACT));
        assertTrue (g1.equals(g2, ComparisonMode.IGNORE_METADATA));
        assertTrue (g1.equals(g2, ComparisonMode.APPROXIMATE));
    }

    /**
     * Tests {@link DefaultParameterValueGroup} serialization.
     */
    @Test
    public void testSerialization() {
        assertSerializedEquals(createGroup(10));
    }
}
