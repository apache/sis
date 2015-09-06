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
import java.util.Random;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.AfterClass;
import org.junit.Test;

import static java.util.Collections.singletonMap;
import static org.apache.sis.test.ReferencingAssert.*;
import static org.apache.sis.internal.util.Constants.NUM_ROW;
import static org.apache.sis.internal.util.Constants.NUM_COL;


/**
 * Tests the {@link TensorParameters} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 */
@DependsOn({
    DefaultParameterDescriptorTest.class,
    DefaultParameterValueTest.class,
    ParametersTest.class
})
public strictfp class TensorParametersTest extends TestCase {
    /**
     * The parameters to use for testing purpose. Mostly identical to {@link TensorParameters#WKT1},
     * except that it is not an instance of the {@link MatrixParameters} subclass. Those parameters
     * do not contain EPSG aliases and identifiers.
     */
    private static TensorParameters<Double> WKT1;

    /**
     * The expected parameter names according the WKT 1 convention for the matrix elements.
     *
     * @see MatrixParametersTest#NAMES
     */
    static final String[][] ELEMENT_NAMES = {
        {"elt_0_0", "elt_0_1", "elt_0_2", "elt_0_3"},
        {"elt_1_0", "elt_1_1", "elt_1_2", "elt_1_3"},
        {"elt_2_0", "elt_2_1", "elt_2_2", "elt_2_3"},
        {"elt_3_0", "elt_3_1", "elt_3_2", "elt_3_3"}
    };

    /**
     * The instance tested by this class.
     */
    final TensorParameters<Double> param;

    /**
     * The expected parameter names for all matrix elements.
     * Example: {@link #ELEMENT_NAMES}.
     */
    private final String[][] names;

    /**
     * The expected parameter aliases for all matrix elements, or {@code null} for no alias.
     * Example: {@link MatrixParametersTest#ALPHANUM_NAMES}.
     */
    private final String[][] aliases;

    /**
     * The expected parameter identifiers for all matrix elements, or {@code null} for no identifier.
     * Example: {@link MatrixParametersAlphaNum#IDENTIFIERS}.
     */
    private final short[][] identifiers;

    /**
     * Creates a new test case for {@link TensorParameters}.
     */
    @SuppressWarnings("unchecked")
    public TensorParametersTest() {
        if (WKT1 == null) {
            WKT1 = new TensorParameters<Double>(Double.class, "elt_", "_",
                    TensorParameters.WKT1.getDimensionDescriptor(0),
                    TensorParameters.WKT1.getDimensionDescriptor(1));
        }
        param       = WKT1;
        names       = ELEMENT_NAMES;
        aliases     = null;
        identifiers = null;
    }

    /**
     * Creates a new test case for a {@link MatrixParameters} defined by the subclass.
     *
     * @param param       The instance tested by this class.
     * @param names       The expected parameter names for all matrix elements.
     * @param aliases     The expected parameter aliases for all matrix elements, or {@code null} for no alias.
     * @param identifiers The expected parameter identifiers for all matrix elements, or {@code null} for no identifier.
     */
    TensorParametersTest(final TensorParameters<Double> param, final String[][] names, final String[][] aliases,
            final short[][] identifiers)
    {
        this.param       = param;
        this.names       = names;
        this.aliases     = aliases;
        this.identifiers = identifiers;
    }

    /**
     * Discards the parameters used by the tests in this class.
     * This method is invoked by JUnit only after all tests completed.
     */
    @AfterClass
    public static void clearTensorParameters() {
        WKT1 = null;
    }

    /**
     * Asserts that the given descriptor has the given name.
     *
     * @param names        The expected parameter name.
     * @param defaultValue The expected parameter default value.
     * @param actual       The actual parameter to verify.
     */
    private static void verifyDescriptor(final String name, final Number defaultValue,
            final ParameterDescriptor<?> actual)
    {
        assertEquals("name", name, actual.getName().getCode());
        assertEquals("defaultValue", defaultValue, actual.getDefaultValue());
    }

    /**
     * Asserts that the given descriptor has the given name, alias, identifier and default value.
     *
     * @param defaultValue The expected parameter default value.
     * @param actual       The actual parameter to verify.
     * @param row          Row index of the matrix element to test.
     * @param column       Column index of the matrix element to test.
     */
    private void verifyDescriptor(final Number defaultValue, final ParameterDescriptor<?> actual,
            final int row, final int column)
    {
        assertEquals("name", names[row][column], actual.getName().getCode());
        assertAliasTipEquals((aliases != null) ? aliases[row][column] : null, actual);
        assertEquals("defaultValue", defaultValue, actual.getDefaultValue());
        if (identifiers != null) {
            final short expected = identifiers[row][column];
            if (expected != 0) {
                assertEpsgIdentifierEquals(String.valueOf(expected), TestUtilities.getSingleton(actual.getIdentifiers()));
                return;
            }
        }
        assertTrue(actual.getIdentifiers().isEmpty());
    }

    /**
     * Tests {@link TensorParameters#getDimensionDescriptor(int)}.
     */
    @Test
    public void testGetDimensionDescriptor() {
        final Integer N3 = 3;
        verifyDescriptor(NUM_ROW, N3, param.getDimensionDescriptor(0));
        verifyDescriptor(NUM_COL, N3, param.getDimensionDescriptor(1));
    }

    /**
     * Tests {@link TensorParameters#getElementDescriptor(int[])}.
     */
    @Test
    @DependsOnMethod("testIndicesToName")
    public void testGetElementDescriptor() {
        final Double N0 = 0.0;
        final Double N1 = 1.0;
        final ParameterDescriptor<Double> e00 = param.getElementDescriptor(0, 0);
        final ParameterDescriptor<Double> e01 = param.getElementDescriptor(0, 1);
        final ParameterDescriptor<Double> e10 = param.getElementDescriptor(1, 0);
        final ParameterDescriptor<Double> e11 = param.getElementDescriptor(1, 1);
        verifyDescriptor(N1, e00, 0, 0);
        verifyDescriptor(N0, e01, 0, 1);
        verifyDescriptor(N0, e10, 1, 0);
        verifyDescriptor(N1, e11, 1, 1);
        assertSame(e00, param.getElementDescriptor(0, 0)); // Test caching.
        assertSame(e01, param.getElementDescriptor(0, 1));
        assertSame(e10, param.getElementDescriptor(1, 0));
        assertSame(e11, param.getElementDescriptor(1, 1));
    }

    /**
     * Tests {@link TensorParameters#getElementDescriptor(int[])} with a value outside the cache capacity.
     */
    @DependsOnMethod("testGetElementDescriptor")
    public void testGetElementDescriptorOutsideCache() {
        final int row = TensorParameters.CACHE_SIZE + 1;
        final int col = TensorParameters.CACHE_SIZE + 2;
        verifyDescriptor("elt_" + row + "_" + col, 0.0, param.getElementDescriptor(row, col));
    }

    /**
     * Tests {@link TensorParameters#indicesToName(int[])}.
     */
    @Test
    public void testIndicesToName() {
        assertEquals("elt_4_8", param.indicesToName(new int[] {4, 8}));
        assertEquals("elt_7_2", param.indicesToName(new int[] {7, 2}));
    }

    /**
     * Tests {@link TensorParameters#nameToIndices(String)}.
     */
    @Test
    public void testNameToIndices() {
        assertArrayEquals(new int[] {4, 8}, param.nameToIndices("elt_4_8"));
        assertArrayEquals(new int[] {7, 2}, param.nameToIndices("elt_7_2"));
        assertNull(param.nameToIndices("other_7_2"));
        assertNull(param.nameToIndices("elt_7"));
        try {
            param.nameToIndices("elt_7_2_3");
            fail("Should not have parsed a name with too many indices.");
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
        }
    }

    /**
     * Tests {@link TensorParameters#getAllDescriptors(int[])} for a 1×1, 2×3 and 3×3 matrices.
     */
    @Test
    @DependsOnMethod("testGetElementDescriptor")
    public void testGetAllDescriptors() {
        final Double  N0 = 0.0;
        final Double  N1 = 1.0;
        final Integer N3 = 3;
        ParameterDescriptor<?>[] descriptors = param.getAllDescriptors(1, 1);
        verifyDescriptor(NUM_ROW, N3, descriptors[0]);
        verifyDescriptor(NUM_COL, N3, descriptors[1]);
        verifyDescriptor(N1, descriptors[2], 0, 0);
        assertEquals("size", 3, descriptors.length);

        descriptors = param.getAllDescriptors(2, 3);
        verifyDescriptor(NUM_ROW, N3, descriptors[0]);
        verifyDescriptor(NUM_COL, N3, descriptors[1]);
        verifyDescriptor(N1, descriptors[2], 0, 0);
        verifyDescriptor(N0, descriptors[3], 0, 1);
        verifyDescriptor(N0, descriptors[4], 0, 2);
        verifyDescriptor(N0, descriptors[5], 1, 0);
        verifyDescriptor(N1, descriptors[6], 1, 1);
        verifyDescriptor(N0, descriptors[7], 1, 2);
        assertEquals("size", 8, descriptors.length);

        descriptors = param.getAllDescriptors(3, 3);
        verifyDescriptor(NUM_ROW, N3, descriptors[0]);
        verifyDescriptor(NUM_COL, N3, descriptors[1]);
        verifyDescriptor(N1, descriptors[ 2], 0, 0);
        verifyDescriptor(N0, descriptors[ 3], 0, 1);
        verifyDescriptor(N0, descriptors[ 4], 0, 2);
        verifyDescriptor(N0, descriptors[ 5], 1, 0);
        verifyDescriptor(N1, descriptors[ 6], 1, 1);
        verifyDescriptor(N0, descriptors[ 7], 1, 2);
        verifyDescriptor(N0, descriptors[ 8], 2, 0);
        verifyDescriptor(N0, descriptors[ 9], 2, 1);
        verifyDescriptor(N1, descriptors[10], 2, 2);
        assertEquals("size", 11, descriptors.length);

        descriptors = param.getAllDescriptors(4, 4);
        verifyDescriptor(NUM_ROW, N3, descriptors[0]);
        verifyDescriptor(NUM_COL, N3, descriptors[1]);
        verifyDescriptor(N1, descriptors[ 2], 0, 0);
        verifyDescriptor(N0, descriptors[ 3], 0, 1);
        verifyDescriptor(N0, descriptors[ 4], 0, 2);
        verifyDescriptor(N0, descriptors[ 5], 0, 3);
        verifyDescriptor(N0, descriptors[ 6], 1, 0);
        verifyDescriptor(N1, descriptors[ 7], 1, 1);
        verifyDescriptor(N0, descriptors[ 8], 1, 2);
        verifyDescriptor(N0, descriptors[ 9], 1, 3);
        verifyDescriptor(N0, descriptors[10], 2, 0);
        verifyDescriptor(N0, descriptors[11], 2, 1);
        verifyDescriptor(N1, descriptors[12], 2, 2);
        verifyDescriptor(N0, descriptors[13], 2, 3);
        verifyDescriptor(N0, descriptors[14], 3, 0);
        verifyDescriptor(N0, descriptors[15], 3, 1);
        verifyDescriptor(N0, descriptors[16], 3, 2);
        verifyDescriptor(N1, descriptors[17], 3, 3);
        assertEquals("size", 18, descriptors.length);
    }

    /**
     * Tests {@link TensorParameters#createValueGroup(Map, Matrix)} and its converse
     * {@link TensorParameters#toMatrix(ParameterValueGroup)}.
     */
    @Test
    @DependsOnMethod("testGetAllDescriptors")
    public void testMatrixConversion() {
        final int size = StrictMath.min(6, TensorParameters.CACHE_SIZE);
        final Random random = TestUtilities.createRandomNumberGenerator();
        for (int numRow = 2; numRow <= size; numRow++) {
            for (int numCol = 2; numCol <= size; numCol++) {
                final Matrix matrix = Matrices.createZero(numRow, numCol);
                for (int j=0; j<numRow; j++) {
                    for (int i=0; i<numCol; i++) {
                        matrix.setElement(j, i, 200*random.nextDouble() - 100);
                    }
                }
                final ParameterValueGroup group = param.createValueGroup(
                        singletonMap(ParameterDescriptor.NAME_KEY, "Test"), matrix);
                assertEquals(NUM_ROW,    numRow, group.parameter(NUM_ROW).intValue());
                assertEquals(NUM_COL,    numCol, group.parameter(NUM_COL).intValue());
                assertEquals("elements", matrix, param.toMatrix(group));
                assertEquals("elements", matrix, param.toMatrix(new ParameterValueGroupWrapper(group)));
            }
        }
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        assertSerializedEquals(param);
    }
}
