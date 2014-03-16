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
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static java.util.Collections.singletonMap;
import static org.opengis.test.Validators.validate;
import static org.apache.sis.parameter.TensorParameters.WKT1;
import static org.apache.sis.parameter.TensorParametersTest.assertDescriptorEquals;
import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests the {@link TensorValues} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn(TensorParametersTest.class)
public final strictfp class TensorValuesTest extends TestCase {
    /**
     * The name of the parameter group created in this test class.
     */
    private static final String GROUP_NAME = "Group test";

    /**
     * Creates an instance for a matrix.
     */
    private static ParameterValueGroup create() {
        return WKT1.createValueGroup(singletonMap(TensorValues.NAME_KEY, GROUP_NAME));
    }

    /**
     * Asserts that the given parameter has the given name and value.
     */
    private static void assertValueEquals(final String name, final Number value, final GeneralParameterValue actual) {
        assertEquals(name, actual.getDescriptor().getName().getCode());
        assertEquals(name, value, ((ParameterValue<?>) actual).getValue());
    }

    /**
     * Tests {@link TensorValues#descriptors()}.
     */
    @Test
    public void testDescriptors() {
        final Double  ZERO  = 0.0;
        final Double  ONE   = 1.0;
        final Integer THREE = 3;
        final ParameterValueGroup group = create();

        group.parameter("num_row").setValue(1);
        group.parameter("num_col").setValue(1);
        List<GeneralParameterDescriptor> descriptors = group.getDescriptor().descriptors();
        assertDescriptorEquals("num_row", THREE, descriptors.get(0));
        assertDescriptorEquals("num_col", THREE, descriptors.get(1));
        assertDescriptorEquals("elt_0_0", ONE,   descriptors.get(2));
        assertEquals("size", 3, descriptors.size());

        group.parameter("num_row").setValue(2);
        group.parameter("num_col").setValue(3);
        descriptors = group.getDescriptor().descriptors();
        assertDescriptorEquals("num_row", THREE, descriptors.get(0));
        assertDescriptorEquals("num_col", THREE, descriptors.get(1));
        assertDescriptorEquals("elt_0_0", ONE,   descriptors.get(2));
        assertDescriptorEquals("elt_0_1", ZERO,  descriptors.get(3));
        assertDescriptorEquals("elt_0_2", ZERO,  descriptors.get(4));
        assertDescriptorEquals("elt_1_0", ZERO,  descriptors.get(5));
        assertDescriptorEquals("elt_1_1", ONE,   descriptors.get(6));
        assertDescriptorEquals("elt_1_2", ZERO,  descriptors.get(7));
        assertEquals("size", 8, descriptors.size());
    }

    /**
     * Tests {@link TensorValues#values()}.
     */
    @Test
    @DependsOnMethod("testParameter")
    public void testValues() {
        final ParameterValueGroup group = create();
        group.parameter("num_row").setValue(2);
        group.parameter("num_col").setValue(3);
        List<GeneralParameterValue> values = group.values();
        assertValueEquals("num_row", 2, values.get(0));
        assertValueEquals("num_col", 3, values.get(1));
        assertEquals("size", 2, values.size());

        group.parameter("elt_0_1").setValue(8);
        group.parameter("elt_1_1").setValue(7);
        group.parameter("elt_1_2").setValue(6);
        values = group.values();
        assertValueEquals("num_row", 2,   values.get(0));
        assertValueEquals("num_col", 3,   values.get(1));
        assertValueEquals("elt_0_1", 8.0, values.get(2));
        assertValueEquals("elt_1_1", 7.0, values.get(3));
        assertValueEquals("elt_1_2", 6.0, values.get(4));
        assertEquals("size", 5, values.size());
    }

    /**
     * Tests {@link TensorValues#descriptor(String)}.
     */
    @Test
    public void testDescriptor() {
        final ParameterValueGroup group = create();
        final ParameterDescriptorGroup d = group.getDescriptor();
        assertDescriptorEquals("num_row", 3,   d.descriptor("num_row"));
        assertDescriptorEquals("num_col", 3,   d.descriptor("num_col"));
        assertDescriptorEquals("elt_0_0", 1.0, d.descriptor("elt_0_0"));
        assertDescriptorEquals("elt_2_2", 1.0, d.descriptor("elt_2_2"));
        /*
         * If we reduce the matrix size, than it shall not be possible
         * anymore to get the descriptor in the row that we removed.
         */
        group.parameter("num_col").setValue(2);
        try {
            d.descriptor("elt_2_2");
            fail("elt_2_2 should not exist.");
        } catch (ParameterNotFoundException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("elt_2_2"));
            assertTrue(message, message.contains(GROUP_NAME));
        }
    }

    /**
     * Tests {@link TensorValues#parameter(String)}.
     */
    @Test
    public void testParameter() {
        final ParameterValueGroup group = create();
        assertValueEquals("num_row", 3,   group.parameter("num_row"));
        assertValueEquals("num_col", 3,   group.parameter("num_col"));
        assertValueEquals("elt_0_0", 1.0, group.parameter("elt_0_0"));
        assertValueEquals("elt_2_2", 1.0, group.parameter("elt_2_2"));

        group.parameter("elt_2_2").setValue(8);
        group.parameter("elt_0_1").setValue(6);
        assertValueEquals("elt_2_2", 8.0, group.parameter("elt_2_2"));
        assertValueEquals("elt_0_1", 6.0, group.parameter("elt_0_1"));
        /*
         * If we reduce the matrix size, than it shall not be possible
         * anymore to get the descriptor in the row that we removed.
         */
        group.parameter("num_col").setValue(2);
        try {
            group.parameter("elt_2_2");
            fail("elt_2_2 should not exist.");
        } catch (ParameterNotFoundException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains("elt_2_2"));
            assertTrue(message, message.contains(GROUP_NAME));
        }
    }

    /**
     * Tests {@link TensorValues#clone()}.
     */
    @Test
    @DependsOnMethod("testParameter")
    public void testClone() {
        final ParameterValueGroup group = create();
        group.parameter("num_row").setValue(2);
        group.parameter("elt_0_1").setValue(4);
        group.parameter("elt_1_0").setValue(2);
        /*
         * Creates a clone, modify some values, keep other values.
         */
        final ParameterValueGroup clone = group.clone();
        clone.parameter("num_row").setValue(4);
        clone.parameter("elt_0_1").setValue(3);
        /*
         * Verify that changes in cloned values did not affected
         * values in the original object.
         */
        assertEquals(2, group.parameter("num_row").intValue());
        assertEquals(4, clone.parameter("num_row").intValue());
        assertEquals(4, group.parameter("elt_0_1").intValue());
        assertEquals(3, clone.parameter("elt_0_1").intValue());
        assertEquals(2, group.parameter("elt_1_0").intValue());
        assertEquals(2, clone.parameter("elt_1_0").intValue());
    }

    /**
     * Tests WKT formatting.
     */
    @Test
    public void testWKT() {
        final Matrix matrix = Matrices.createIdentity(4);
        matrix.setElement(0,2,  4);
        matrix.setElement(1,0, -2);
        matrix.setElement(2,3,  7);
        final ParameterValueGroup group = WKT1.createValueGroup(singletonMap(TensorValues.NAME_KEY, "Affine"), matrix);
        validate(group);
        assertWktEquals(
                "ParameterGroup[“Affine”,\n"      +
                "  Parameter[“num_row”, 4],\n"    +
                "  Parameter[“num_col”, 4],\n"    +
                "  Parameter[“elt_0_2”, 4.0],\n"  +
                "  Parameter[“elt_1_0”, -2.0],\n" +
                "  Parameter[“elt_2_3”, 7.0]]", group);
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        assertSerializedEquals(create());
    }
}
