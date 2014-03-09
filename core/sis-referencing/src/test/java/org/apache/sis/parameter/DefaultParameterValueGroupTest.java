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
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.InvalidParameterNameException;
import org.opengis.parameter.InvalidParameterCardinalityException;
import org.apache.sis.test.DependsOnMethod;
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
     * The descriptors of parameters to be tested by this class.
     * The default descriptors are:
     * <ul>
     *   <li>One mandatory parameter (cardinality [1…1]).</li>
     *   <li>One mandatory parameter (cardinality [1…1]).</li>
     *   <li>One optional  parameter (cardinality [0…1]).</li>
     *   <li>One optional  parameter (cardinality [0…2]) — invalid according ISO 19111, but supported by SIS.</li>
     * </ul>
     *
     * Some test methods may replace the default descriptor by an other one.
     */
    private ParameterDescriptorGroup descriptor = DefaultParameterDescriptorGroupTest.M1_M1_O1_O2;

    /**
     * Creates values for all parameters defined by the {@linkplain #descriptor} (regardless their cardinality),
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
        group.values().addAll(Arrays.asList(createValues(step)));
        return group;
    }

    /**
     * Validates the test parameter values created by {@link #createValues(List, int)}.
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
            if (param.getDescriptor() instanceof MultiOccurrenceDescriptor) {
                assertNotNull("Validation methods should have detected that the descriptor is invalid.", error);
            } else if (error != null) {
                throw error;
            }
        }
    }

    /**
     * Tests {@link DefaultParameterValueGroup#parameter(String)}.
     */
    @Test
    @DependsOnMethod("testValuesAddAll")
    public void testParameter() {
        final DefaultParameterValueGroup  group  = createGroup(10);
        final List<GeneralParameterValue> values = group.values();
        assertSame  ("parameter(“Mandatory 1”)", values.get(0), group.parameter("Mandatory 1"));
        assertSame  ("parameter(“Mandatory 2”)", values.get(1), group.parameter("Mandatory 2"));
        assertSame  ("parameter(“Optional 3”)",  values.get(2), group.parameter("Optional 3"));
        assertSame  ("parameter(“Optional 4”)",  values.get(3), group.parameter("Optional 4"));
        assertEquals("parameter(“Mandatory 1”)", 10, group.parameter("Mandatory 1").intValue());
        assertEquals("parameter(“Mandatory 2”)", 20, group.parameter("Mandatory 2").intValue());
        assertEquals("parameter(“Optional 3”)",  30, group.parameter("Optional 3") .intValue());
        assertEquals("parameter(“Optional 4”)",  40, group.parameter("Optional 4") .intValue());
    }

    /**
     * Tests {@code DefaultParameterValueGroup.values().get(…)} on a group expected to be pre-filled
     * with mandatory parameters.
     */
    @Test
    public void testValuesGet() {
        final DefaultParameterValueGroup  group  = new DefaultParameterValueGroup(descriptor);
        final List<GeneralParameterValue> values = group.values();
        assertEquals("Initial size", 2, values.size());
        assertEquals(descriptor.descriptors().get(0).createValue(), values.get(0));
        assertEquals(descriptor.descriptors().get(1).createValue(), values.get(1));
        try {
            values.get(2);
            fail("Index 2 shall be out of bounds.");
        } catch (IndexOutOfBoundsException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Tests {@code DefaultParameterValueGroup.values().contains(…)}.
     */
    @Test
    @DependsOnMethod("testValuesAddAll")
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
    @DependsOnMethod("testValuesAdd")
    public void testValuesAddAll() {
        final DefaultParameterValueGroup  group  = new DefaultParameterValueGroup(descriptor);
        final List<GeneralParameterValue> values = group.values();
        assertEquals("Initial size", 2, values.size());

        final DefaultParameterValue<?>[] parameters = createValues(10);
        assertTrue(values.addAll(Arrays.asList(parameters)));
        assertEquals("Final size", parameters.length, values.size());
        for (int i=0; i<parameters.length; i++) {
            assertSame(parameters[i], values.get(i));
        }
    }

    /**
     * Tests that attempts to add an invalid parameter cause an {@link InvalidParameterNameException} to be thrown.
     */
    @Test
    @DependsOnMethod("testValuesAdd")
    public void testValuesAddWrongParameter() {
        final DefaultParameterValueGroup    group = createGroup(10);
        final List<GeneralParameterValue>  values = group.values();
        final ParameterValue<Integer> nonExistent = new DefaultParameterDescriptor<>(
                name("Optional 5"), Integer.class, null, null, null, false).createValue();
        try {
            values.add(nonExistent);
            fail("“Optional 5” is not a parameter for this group.");
        } catch (InvalidParameterNameException e) {
            assertEquals("Optional 5", e.getParameterName());
            final String message = e.getMessage();
            assertTrue(message, message.contains("Optional 5"));
            assertTrue(message, message.contains("The group"));
        }
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
        assertEquals("size", 2, values.size()); // Because the descriptor declares 2 parameters as mandatory.
        assertTrue  ("add(“Mandatory 1”)", values.add(toAdd[0])); assertEquals("size", 2, values.size());
        assertTrue  ("add(“Mandatory 2”)", values.add(toAdd[1])); assertEquals("size", 2, values.size());
        assertTrue  ("add(“Optional 3”)",  values.add(toAdd[2])); assertEquals("size", 3, values.size());
        assertTrue  ("add(“Optional 4”)",  values.add(toAdd[3])); assertEquals("size", 4, values.size());
        /*
         * Test [1…1] cardinality.
         */
        try {
            values.add(toAdd[1]);
            fail("“Mandatory 2” is already present in this group.");
        } catch (InvalidParameterCardinalityException e) {
            assertEquals("Mandatory 2", e.getParameterName());
            final String message = e.getMessage();
            assertTrue(message, message.contains("Mandatory 2"));
        }
        assertEquals("size", 4, values.size()); // Size shall be unchanged.
        /*
         * Test [0…1] cardinality.
         */
        try {
            values.add(toAdd[2]);
            fail("“Optional 3” is already present in this group.");
        } catch (InvalidParameterCardinalityException e) {
            assertEquals("Optional 3", e.getParameterName());
            final String message = e.getMessage();
            assertTrue(message, message.contains("Optional 3"));
        }
        assertEquals("size", 4, values.size()); // Size shall be unchanged.
        /*
         * Test [0…2] cardinality.
         */
        assertTrue("add(“Optional 4”)",values.add(toAdd[3]));
        assertEquals("size", 5, values.size());
        /*
         * Verifies parameter values.
         */
        assertEquals("parameter(“Mandatory 1”)", -10, group.parameter("Mandatory 1").intValue());
        assertEquals("parameter(“Mandatory 2”)", -20, group.parameter("Mandatory 2").intValue());
        assertEquals("parameter(“Optional 3”)",  -30, group.parameter( "Optional 3").intValue());
    }

    /**
     * Tests {@code DefaultParameterValueGroup.values().remove(…)}. In particular, tests that attempt to
     * remove a mandatory parameter causes an {@link InvalidParameterCardinalityException} to be thrown.
     */
    @Test
    @DependsOnMethod("testValuesAddAll")
    public void testValuesRemove() {
        final GeneralParameterValue[]  negatives = createValues(-10);
        final DefaultParameterValueGroup   group = createGroup(10);
        final List<GeneralParameterValue> values = group.values();
        assertFalse(values.remove(negatives[0])); // Non-existant parameter.
        try {
            values.remove(values.get(0));
            fail("“Mandatory 1” is a mandatory parameter; it should not be removeable.");
        } catch (InvalidParameterCardinalityException e) {
            assertEquals("Mandatory 1", e.getParameterName());
            final String message = e.getMessage();
            assertTrue(message, message.contains("Mandatory 1"));
        }
    }

    /**
     * Tests the {@link DefaultParameterValueGroup#addGroup(String)} method.
     * Ensures the descriptor is found and the new value correctly inserted.
     */
    @Test
    public void testAddGroup() {
        descriptor = new DefaultParameterDescriptorGroup(name("theGroup"), 1, 1,
                new DefaultParameterDescriptorGroup(name("theSubGroup"), 0, 10)
        );
        validate(descriptor);

        final ParameterValueGroup groupValues = descriptor.createValue();
        assertEquals("Size before add.", 0, groupValues.values().size());
        final ParameterValueGroup subGroupValues = groupValues.addGroup("theSubGroup");
        assertEquals("Size after add.", 1, groupValues.values().size());
        assertSame(subGroupValues, groupValues.values().get(0));
    }

    /**
     * Tests {@link #equals(Object)} and {@link #hashCode()} methods.
     */
    @Test
    @DependsOnMethod("testValuesAddAll")
    public void testEqualsAndHashCode() {
        final DefaultParameterValueGroup g1 = createGroup( 10);
        final DefaultParameterValueGroup g2 = createGroup(-10);
        final DefaultParameterValueGroup g3 = createGroup( 10);
        assertTrue ("equals", g1.equals(g1));
        assertFalse("equals", g1.equals(g2));
        assertTrue ("equals", g1.equals(g3));
        assertEquals("hashCode", g1.hashCode(), g3.hashCode());
    }
}
