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
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.provider.Affine;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.util.privy.Constants;
import static org.apache.sis.util.privy.Constants.NUM_ROW;
import static org.apache.sis.util.privy.Constants.NUM_COL;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.opengis.test.Validators.validate;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import static org.apache.sis.referencing.Assertions.assertWktEquals;


/**
 * Tests the {@link TensorValues} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TensorValuesTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public TensorValuesTest() {
    }

    /**
     * The name of the parameter group created in this test class.
     */
    private static final String GROUP_NAME = "Group test";

    /**
     * Creates an instance for a matrix using the WKT 1 conventions.
     */
    private static ParameterValueGroup createWKT1() {
        return TensorParameters.WKT1.createValueGroup(Map.of(TensorValues.NAME_KEY, GROUP_NAME));
    }

    /**
     * Creates an instance for a matrix using the alphanumeric (EPSG) conventions.
     */
    private static ParameterValueGroup createAlphaNumeric() {
        return TensorParameters.ALPHANUM.createValueGroup(Map.of(TensorValues.NAME_KEY, GROUP_NAME));
    }

    /**
     * Asserts that the given descriptor has the given name and default value.
     * Aliases and identifiers are ignored - testing them is the purpose of {@link MatrixParametersTest}.
     *
     * @param  name          the expected parameter name.
     * @param  defaultValue  the expected parameter default value.
     * @param  actual        the actual parameter to verify.
     */
    private static void assertDescriptorEquals(final String name, final Number defaultValue,
            final GeneralParameterDescriptor actual)
    {
        assertEquals(actual.getName().getCode(), name);
        assertEquals(defaultValue, ((ParameterDescriptor<?>) actual).getDefaultValue(), name);
    }

    /**
     * Asserts that the given parameter has the given name and value.
     *
     * @param  name    the expected parameter name.
     * @param  value   the expected parameter value.
     * @param  actual  the actual parameter to verify.
     */
    private static void assertValueEquals(final String name, final Number value, final GeneralParameterValue actual) {
        assertEquals(actual.getDescriptor().getName().getCode(), name);
        assertEquals(value, ((ParameterValue<?>) actual).getValue(), name);
    }

    /**
     * Tests {@link TensorValues#descriptors()} using WKT1 contentions.
     */
    @Test
    public void testDescriptors() {
        final Double  N0 = 0.0;
        final Double  N1 = 1.0;
        final Integer N3 = 3;
        final ParameterValueGroup group = createWKT1();

        group.parameter(NUM_ROW).setValue(1);
        group.parameter(NUM_COL).setValue(1);
        List<GeneralParameterDescriptor> descriptors = group.getDescriptor().descriptors();
        assertDescriptorEquals( NUM_ROW,  N3, descriptors.get(0));
        assertDescriptorEquals( NUM_COL,  N3, descriptors.get(1));
        assertDescriptorEquals("elt_0_0", N1, descriptors.get(2));
        assertEquals(3, descriptors.size());

        group.parameter(NUM_ROW).setValue(2);
        group.parameter(NUM_COL).setValue(3);
        descriptors = group.getDescriptor().descriptors();
        assertDescriptorEquals( NUM_ROW,  N3, descriptors.get(0));
        assertDescriptorEquals( NUM_COL,  N3, descriptors.get(1));
        assertDescriptorEquals("elt_0_0", N1, descriptors.get(2));
        assertDescriptorEquals("elt_0_1", N0, descriptors.get(3));
        assertDescriptorEquals("elt_0_2", N0, descriptors.get(4));
        assertDescriptorEquals("elt_1_0", N0, descriptors.get(5));
        assertDescriptorEquals("elt_1_1", N1, descriptors.get(6));
        assertDescriptorEquals("elt_1_2", N0, descriptors.get(7));
        assertEquals(8, descriptors.size());
    }

    /**
     * Tests {@link TensorValues#descriptors()} using alphanumeric (EPSG) contentions.
     */
    @Test
    public void testAlphaNumericDescriptors() {
        final Double  N0 = 0.0;
        final Double  N1 = 1.0;
        final Integer N3 = 3;
        final ParameterValueGroup group = createAlphaNumeric();
        final List<GeneralParameterDescriptor> descriptors = group.getDescriptor().descriptors();
        assertDescriptorEquals(NUM_ROW, N3, descriptors.get(0));
        assertDescriptorEquals(NUM_COL, N3, descriptors.get(1));
        assertDescriptorEquals("A0",    N1, descriptors.get( 2));
        assertDescriptorEquals("A1",    N0, descriptors.get( 3));
        assertDescriptorEquals("A2",    N0, descriptors.get( 4));
        assertDescriptorEquals("B0",    N0, descriptors.get( 5));
        assertDescriptorEquals("B1",    N1, descriptors.get( 6));
        assertDescriptorEquals("B2",    N0, descriptors.get( 7));
        assertDescriptorEquals("C0",    N0, descriptors.get( 8));
        assertDescriptorEquals("C1",    N0, descriptors.get( 9));
        assertDescriptorEquals("C2",    N1, descriptors.get(10));
        assertEquals(11, descriptors.size());
    }

    /**
     * Tests {@link TensorValues#values()}.
     */
    @Test
    public void testValues() {
        final ParameterValueGroup group = createWKT1();
        group.parameter(NUM_ROW).setValue(2);
        group.parameter(NUM_COL).setValue(3);
        List<GeneralParameterValue> values = group.values();
        assertValueEquals(NUM_ROW, 2, values.get(0));
        assertValueEquals(NUM_COL, 3, values.get(1));
        assertEquals(2, values.size());
        /*
         * Above list had no explicit parameters, since all of them had their default values.
         * Now set some parameters to different values. Those parameters should now appear in
         * the list.
         */
        group.parameter("elt_0_1").setValue(8);
        group.parameter("elt_1_1").setValue(7);
        group.parameter("elt_1_2").setValue(6);
        values = group.values();
        assertValueEquals( NUM_ROW,  2,   values.get(0));
        assertValueEquals( NUM_COL,  3,   values.get(1));
        assertValueEquals("elt_0_1", 8.0, values.get(2));
        assertValueEquals("elt_1_1", 7.0, values.get(3));
        assertValueEquals("elt_1_2", 6.0, values.get(4));
        assertEquals(5, values.size());
    }

    /**
     * Tests {@link TensorValues#descriptor(String)}.
     */
    @Test
    public void testDescriptor() {
        final Double  N0 = 0.0;
        final Double  N1 = 1.0;
        final Integer N3 = 3;
        final ParameterValueGroup group = createWKT1();
        final ParameterDescriptorGroup d = group.getDescriptor();
        assertDescriptorEquals( NUM_ROW,  N3, d.descriptor( NUM_ROW ));
        assertDescriptorEquals( NUM_COL,  N3, d.descriptor( NUM_COL ));
        assertDescriptorEquals("elt_0_0", N1, d.descriptor("elt_0_0"));
        assertDescriptorEquals("elt_0_1", N0, d.descriptor("elt_0_1"));
        assertDescriptorEquals("elt_0_2", N0, d.descriptor("elt_0_2"));
        assertDescriptorEquals("elt_1_0", N0, d.descriptor("elt_1_0"));
        assertDescriptorEquals("elt_1_1", N1, d.descriptor("elt_1_1"));
        assertDescriptorEquals("elt_1_2", N0, d.descriptor("elt_1_2"));
        assertDescriptorEquals("elt_2_0", N0, d.descriptor("elt_2_0"));
        assertDescriptorEquals("elt_2_1", N0, d.descriptor("elt_2_1"));
        assertDescriptorEquals("elt_2_2", N1, d.descriptor("elt_2_2"));
        /*
         * Same test as above, but using the EPSG or pseudo-EPSG names.
         */
        assertDescriptorEquals("elt_0_0", N1, d.descriptor("A0"));
        assertDescriptorEquals("elt_0_1", N0, d.descriptor("A1"));
        assertDescriptorEquals("elt_0_2", N0, d.descriptor("A2"));
        assertDescriptorEquals("elt_1_0", N0, d.descriptor("B0"));
        assertDescriptorEquals("elt_1_1", N1, d.descriptor("B1"));
        assertDescriptorEquals("elt_1_2", N0, d.descriptor("B2"));
        assertDescriptorEquals("elt_2_0", N0, d.descriptor("C0"));
        assertDescriptorEquals("elt_2_1", N0, d.descriptor("C1"));
        assertDescriptorEquals("elt_2_2", N1, d.descriptor("C2"));
        /*
         * If we reduce the matrix size, than it shall not be possible
         * anymore to get the descriptor in the row that we removed.
         */
        group.parameter(NUM_COL).setValue(2);
        var e = assertThrows(ParameterNotFoundException.class, () -> d.descriptor("elt_2_2"));
        assertMessageContains(e, "elt_2_2", GROUP_NAME);
    }

    /**
     * Tests {@link TensorValues#parameter(String)}.
     */
    @Test
    public void testParameter() {
        final Double  N0 = 0.0;
        final Double  N1 = 1.0;
        final Integer N3 = 3;
        final ParameterValueGroup group = createWKT1();
        assertValueEquals( NUM_ROW,  N3, group.parameter( NUM_ROW ));
        assertValueEquals( NUM_COL,  N3, group.parameter( NUM_COL ));
        assertValueEquals("elt_0_0", N1, group.parameter("elt_0_0"));
        assertValueEquals("elt_0_1", N0, group.parameter("elt_0_1"));
        assertValueEquals("elt_2_2", N1, group.parameter("elt_2_2"));
        assertValueEquals("elt_0_0", N1, group.parameter("A0"));
        assertValueEquals("elt_0_1", N0, group.parameter("A1"));
        assertValueEquals("elt_2_2", N1, group.parameter("C2"));
        /*
         * Change some values and test again.
         */
        group.parameter("elt_2_2").setValue(8);
        group.parameter("elt_0_1").setValue(6);
        assertValueEquals("elt_2_2", 8.0, group.parameter("elt_2_2"));
        assertValueEquals("elt_0_1", 6.0, group.parameter("elt_0_1"));
        assertValueEquals("elt_0_0", N1,  group.parameter("elt_0_0"));
        assertValueEquals("elt_2_2", 8.0, group.parameter("C2"));
        assertValueEquals("elt_0_1", 6.0, group.parameter("A1"));
        assertValueEquals("elt_0_0", N1,  group.parameter("A0"));
        /*
         * If we reduce the matrix size, than it shall not be possible
         * anymore to get the descriptor in the row that we removed.
         */
        group.parameter(NUM_COL).setValue(2);
        var e = assertThrows(ParameterNotFoundException.class, () -> group.parameter("elt_2_2"));
        assertMessageContains(e, "elt_2_2", GROUP_NAME);
    }

    /**
     * Tests {@link TensorValues#clone()}.
     */
    @Test
    public void testClone() {
        final ParameterValueGroup group = createWKT1();
        group.parameter( NUM_ROW ).setValue(2);
        group.parameter("elt_0_1").setValue(4);
        group.parameter("elt_1_0").setValue(2);
        /*
         * Creates a clone, modify some values, keep other values.
         */
        final ParameterValueGroup clone = group.clone();
        clone.parameter( NUM_ROW ).setValue(4);
        clone.parameter("elt_0_1").setValue(3);
        /*
         * Verify that changes in cloned values did not affected
         * values in the original object.
         */
        assertEquals(2, group.parameter( NUM_ROW ).intValue());
        assertEquals(4, clone.parameter( NUM_ROW ).intValue());
        assertEquals(4, group.parameter("elt_0_1").intValue());
        assertEquals(3, clone.parameter("elt_0_1").intValue());
        assertEquals(2, group.parameter("elt_1_0").intValue());
        assertEquals(2, clone.parameter("elt_1_0").intValue());
    }

    /**
     * Tests {@link TensorParameters#WKT1} formatting.
     * <ul>
     *   <li>Group name shall be {@code "Affine"}.</li>
     *   <li>Parameters {@code "num_row"} and {@code "num_col"} are mandatory.</li>
     *   <li>Parameter names shall be of the form {@code "elt_0_0"}.</li>
     *   <li>No identifier.</li>
     * </ul>
     */
    @Test
    public void testWKT1() {
        final Matrix matrix = Matrices.createIdentity(3);
        matrix.setElement(0,2,  4);
        matrix.setElement(1,0, -2);
        matrix.setElement(2,2,  7);
        final ParameterValueGroup group = TensorParameters.WKT1.createValueGroup(
                Map.of(TensorValues.NAME_KEY, Constants.AFFINE), matrix);
        validate(group);
        assertWktEquals(Convention.WKT2,
                "PARAMETERGROUP[“Affine”,\n"      +
                "  PARAMETER[“num_row”, 3],\n"    +   // Shall be shown even if equals to the default value.
                "  PARAMETER[“num_col”, 3],\n"    +
                "  PARAMETER[“elt_0_2”, 4.0],\n"  +
                "  PARAMETER[“elt_1_0”, -2.0],\n" +
                "  PARAMETER[“elt_2_2”, 7.0]]", group);
    }

    /**
     * Tests {@link TensorParameters#ALPHANUM} formatting.
     * <ul>
     *   <li>Group name shall be {@code "Affine parametric transformation"}.</li>
     *   <li>No {@code "num_row"} or {@code "num_col"} parameters if their value is equal to 3.</li>
     *   <li>Parameter names shall be of the form {@code "A0"}.</li>
     *   <li>Identifiers present, but only for A0-A2 and B0-B2.</li>
     * </ul>
     */
    @Test
    public void testWKT2() {
        final Matrix matrix = Matrices.createIdentity(3);
        matrix.setElement(0,2,  4);
        matrix.setElement(1,0, -2);
        matrix.setElement(2,2,  7);
        final ParameterValueGroup group = TensorParameters.ALPHANUM.createValueGroup(
                Map.of(TensorValues.NAME_KEY, Affine.NAME), matrix);
        validate(group);
        assertWktEquals(Convention.WKT2,
                "PARAMETERGROUP[“Affine parametric transformation”,\n" +
                "  PARAMETER[“A2”, 4.0, ID[“EPSG”, 8625]],\n"  +
                "  PARAMETER[“B0”, -2.0, ID[“EPSG”, 8639]],\n" +
                "  PARAMETER[“C2”, 7.0]]", group);
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        assertSerializedEquals(createWKT1());
    }
}
