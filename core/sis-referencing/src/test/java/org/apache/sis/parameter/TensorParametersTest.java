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
import java.util.Random;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import static java.util.Collections.singletonMap;
import static org.opengis.test.Validators.validate;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import static org.apache.sis.test.ReferencingAssert.*;


/**
 * Tests the {@link TensorParameters} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 */
@DependsOn(ParametersTest.class)
public final strictfp class TensorParametersTest extends TestCase {
    /**
     * The parameters to use for testing purpose. Mostly identical to {@link TensorParameters#WKT1},
     * except that it is not an instance of the {@link MatrixParameters} subclass. Those parameters
     * do not contain EPSG aliases and identifiers.
     */
    private static TensorParameters<Double> WKT1;

    /**
     * The expected parameter names according the WKT 1 convention for the matrix size.
     */
    private static final String[][] SIZE_NAMES = {
        {"num_row"},
        {"num_col"}
    };

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
     * Creates the parameters to be used by the tests in this class.
     */
    @BeforeClass
    @SuppressWarnings("unchecked")
    public static void createTensorParameters() {
        WKT1 = new TensorParameters<>(Double.class, "elt_", "_",
                TensorParameters.WKT1.getDimensionDescriptor(0),
                TensorParameters.WKT1.getDimensionDescriptor(1));
    }

    /**
     * Discards the parameters used by the tests in this class.
     */
    @AfterClass
    public static void clearTensorParameters() {
        WKT1 = null;
    }

    /**
     * Asserts that the given descriptor has the given name, alias, identifier and default value.
     *
     * @param names        The expected parameter names for all matrix elements.
     * @param aliases      The expected parameter aliases for all matrix elements, or {@code null} for no alias.
     * @param identifiers  The expected parameter identifiers for all matrix elements, or {@code null} for no identifier.
     * @param defaultValue The expected parameter default value.
     * @param actual       The actual parameter to verify.
     * @param row          Row index of the matrix element to test.
     * @param column       Column index of the matrix element to test.
     */
    private static void verifyDescriptor(final String[][] names, final String[][] aliases, final short[][] identifiers,
            final Number defaultValue, final GeneralParameterDescriptor actual, final int row, final int column)
    {
        assertEquals("name", names[row][column], actual.getName().getCode());
        assertAliasTipEquals((aliases != null) ? aliases[row][column] : null, actual);
        assertIdentifierEqualsEPSG((identifiers != null) ? identifiers[row][column] : 0, actual);
        assertEquals("defaultValue", defaultValue, ((ParameterDescriptor<?>) actual).getDefaultValue());
    }

    /**
     * Tests {@link TensorParameters#getDimensionDescriptor(int)}.
     */
    @Test
    public void testGetDimensionDescriptor() {
        testGetDimensionDescriptor(WKT1);
    }

    /** Implementation of {@link #testGetDimensionDescriptor()} with user-supplied parameters. */
    static void testGetDimensionDescriptor(final TensorParameters<Double> WKT1) {
        final Integer N3 = 3;
        verifyDescriptor(SIZE_NAMES, null, null, N3, WKT1.getDimensionDescriptor(0), 0, 0);
        verifyDescriptor(SIZE_NAMES, null, null, N3, WKT1.getDimensionDescriptor(1), 1, 0);
    }

    /**
     * Tests {@link TensorParameters#getElementDescriptor(int[])}.
     */
    @Test
    @DependsOnMethod("testIndicesToName")
    public void testGetElementDescriptor() {
        testGetElementDescriptor(WKT1, ELEMENT_NAMES, null, null);
    }

    /** Implementation of {@link #testGetElementDescriptor()} with user-supplied parameters. */
    static void testGetElementDescriptor(final TensorParameters<Double> WKT1,
            final String[][] names, final String[][] aliases, final short[][] identifiers)
    {
        final Double N0 = 0.0;
        final Double N1 = 1.0;
        final ParameterDescriptor<Double> e00 = WKT1.getElementDescriptor(0, 0);
        final ParameterDescriptor<Double> e01 = WKT1.getElementDescriptor(0, 1);
        final ParameterDescriptor<Double> e10 = WKT1.getElementDescriptor(1, 0);
        final ParameterDescriptor<Double> e11 = WKT1.getElementDescriptor(1, 1);
        verifyDescriptor(names, aliases, identifiers, N1, e00, 0, 0);
        verifyDescriptor(names, aliases, identifiers, N0, e01, 0, 1);
        verifyDescriptor(names, aliases, identifiers, N0, e10, 1, 0);
        verifyDescriptor(names, aliases, identifiers, N1, e11, 1, 1);
        assertSame(e00, WKT1.getElementDescriptor(0, 0)); // Test caching.
        assertSame(e01, WKT1.getElementDescriptor(0, 1));
        assertSame(e10, WKT1.getElementDescriptor(1, 0));
        assertSame(e11, WKT1.getElementDescriptor(1, 1));
    }

    /**
     * Tests {@link TensorParameters#getElementDescriptor(int[])} with a value outside the cache capacity.
     */
    @DependsOnMethod("testGetElementDescriptor")
    public void testGetElementDescriptorOutsideCache() {
        final int row = TensorParameters.CACHE_SIZE + 1;
        final int col = TensorParameters.CACHE_SIZE + 2;
        verifyDescriptor(new String[][] {{"elt_" + row + "_" + col}}, null, null, 0.0,
                WKT1.getElementDescriptor(row, col), 0, 0);
    }

    /**
     * Tests {@link TensorParameters#indicesToName(int[])}.
     */
    @Test
    public void testIndicesToName() {
        testIndicesToName(WKT1);
    }

    /** Implementation of {@link #testIndicesToName()} with user-supplied parameters. */
    static void testIndicesToName(final TensorParameters<Double> WKT1) {
        assertEquals("elt_4_8", WKT1.indicesToName(new int[] {4, 8}));
        assertEquals("elt_7_2", WKT1.indicesToName(new int[] {7, 2}));
    }

    /**
     * Tests {@link TensorParameters#nameToIndices(String)}.
     */
    @Test
    public void testNameToIndices() {
        testNameToIndices(WKT1);
    }

    /** Implementation of {@link #testNameToIndices()} with user-supplied parameters. */
    static void testNameToIndices(final TensorParameters<Double> WKT1) {
        assertArrayEquals(new int[] {4, 8}, WKT1.nameToIndices("elt_4_8"));
        assertArrayEquals(new int[] {7, 2}, WKT1.nameToIndices("elt_7_2"));
        assertNull(WKT1.nameToIndices("other_7_2"));
        assertNull(WKT1.nameToIndices("elt_7"));
        try {
            WKT1.nameToIndices("elt_7_2_3");
            fail("Should not have parsed a name with too many indices.");
        } catch (IllegalArgumentException e) {
            // This is the expected exception.
        }
    }

    /**
     * Tests {@link TensorParameters#descriptors(int[])} for a 1×1, 2×3 and 3×3 matrices.
     */
    @Test
    @DependsOnMethod("testGetElementDescriptor")
    public void testDescriptors() {
        testDescriptors(WKT1, false, ELEMENT_NAMES, null, null);
    }

    /** Implementation of {@link #testDescriptors()} with user-supplied parameters. */
    static void testDescriptors(final TensorParameters<Double> WKT1, final boolean isEPSG,
            final String[][] names, final String[][] aliases, final short[][] identifiers)
    {
        final Double  N0 = 0.0;
        final Double  N1 = 1.0;
        final Integer N3 = 3;
        List<GeneralParameterDescriptor> descriptors = WKT1.descriptors(new int[] {1, 1});
        verifyDescriptor(SIZE_NAMES, null, null,      N3, descriptors.get(0), 0, 0);
        verifyDescriptor(SIZE_NAMES, null, null,      N3, descriptors.get(1), 1, 0);
        verifyDescriptor(names, aliases, identifiers, N1, descriptors.get(2), 0, 0);
        assertEquals("size", 3, descriptors.size());

        descriptors = WKT1.descriptors(new int[] {2, 3});
        verifyDescriptor(SIZE_NAMES, null, null,      N3, descriptors.get(0), 0, 0);
        verifyDescriptor(SIZE_NAMES, null, null,      N3, descriptors.get(1), 1, 0);
        verifyDescriptor(names, aliases, identifiers, N1, descriptors.get(2), 0, 0);
        verifyDescriptor(names, aliases, identifiers, N0, descriptors.get(3), 0, 1);
        verifyDescriptor(names, aliases, identifiers, N0, descriptors.get(4), 0, 2);
        verifyDescriptor(names, aliases, identifiers, N0, descriptors.get(5), 1, 0);
        verifyDescriptor(names, aliases, identifiers, N1, descriptors.get(6), 1, 1);
        verifyDescriptor(names, aliases, identifiers, N0, descriptors.get(7), 1, 2);
        assertEquals("size", 8, descriptors.size());

        descriptors = WKT1.descriptors(new int[] {3, 3});
        int i = 0;
        if (!isEPSG) {
            verifyDescriptor(SIZE_NAMES, null, null,  N3, descriptors.get(i++), 0, 0);
            verifyDescriptor(SIZE_NAMES, null, null,  N3, descriptors.get(i++), 1, 0);
        }
        verifyDescriptor(names, aliases, identifiers, N1, descriptors.get(i++), 0, 0);
        verifyDescriptor(names, aliases, identifiers, N0, descriptors.get(i++), 0, 1);
        verifyDescriptor(names, aliases, identifiers, N0, descriptors.get(i++), 0, 2);
        verifyDescriptor(names, aliases, identifiers, N0, descriptors.get(i++), 1, 0);
        verifyDescriptor(names, aliases, identifiers, N1, descriptors.get(i++), 1, 1);
        verifyDescriptor(names, aliases, identifiers, N0, descriptors.get(i++), 1, 2);
        if (!isEPSG) {
            verifyDescriptor(names, aliases, identifiers, N0, descriptors.get(i++), 2, 0);
            verifyDescriptor(names, aliases, identifiers, N0, descriptors.get(i++), 2, 1);
            verifyDescriptor(names, aliases, identifiers, N1, descriptors.get(i++), 2, 2);
        }
        assertEquals("size", i, descriptors.size());

        descriptors = WKT1.descriptors(new int[] {4, 4});
        verifyDescriptor(SIZE_NAMES, null, null,      N3, descriptors.get( 0), 0, 0);
        verifyDescriptor(SIZE_NAMES, null, null,      N3, descriptors.get( 1), 1, 0);
        verifyDescriptor(names, aliases, identifiers, N1, descriptors.get( 2), 0, 0);
        verifyDescriptor(names, aliases, identifiers, N0, descriptors.get( 3), 0, 1);
        verifyDescriptor(names, aliases, identifiers, N0, descriptors.get( 4), 0, 2);
        verifyDescriptor(names, aliases, identifiers, N0, descriptors.get( 5), 0, 3);
        verifyDescriptor(names, aliases, identifiers, N0, descriptors.get( 6), 1, 0);
        verifyDescriptor(names, aliases, identifiers, N1, descriptors.get( 7), 1, 1);
        verifyDescriptor(names, aliases, identifiers, N0, descriptors.get( 8), 1, 2);
        verifyDescriptor(names, aliases, identifiers, N0, descriptors.get( 9), 1, 3);
        verifyDescriptor(names, aliases, identifiers, N0, descriptors.get(10), 2, 0);
        verifyDescriptor(names, aliases, identifiers, N0, descriptors.get(11), 2, 1);
        verifyDescriptor(names, aliases, identifiers, N1, descriptors.get(12), 2, 2);
        verifyDescriptor(names, aliases, identifiers, N0, descriptors.get(13), 2, 3);
        verifyDescriptor(names, aliases, identifiers, N0, descriptors.get(14), 3, 0);
        verifyDescriptor(names, aliases, identifiers, N0, descriptors.get(15), 3, 1);
        verifyDescriptor(names, aliases, identifiers, N0, descriptors.get(16), 3, 2);
        verifyDescriptor(names, aliases, identifiers, N1, descriptors.get(17), 3, 3);
        assertEquals("size", 18, descriptors.size());
    }

    /**
     * Tests {@link TensorParameters#createValueGroup(Map, Matrix)} and its converse
     * {@link TensorParameters#toMatrix(ParameterValueGroup)}.
     */
    @Test
    @DependsOnMethod("testDescriptors")
    public void testMatrixConversion() {
        final int size = 8;
        final Random random = TestUtilities.createRandomNumberGenerator();
        for (int numRow = 2; numRow <= size; numRow++) {
            for (int numCol = 2; numCol <= size; numCol++) {
                final Matrix matrix = Matrices.createZero(numRow, numCol);
                for (int j=0; j<numRow; j++) {
                    for (int i=0; i<numCol; i++) {
                        matrix.setElement(j, i, 200*random.nextDouble() - 100);
                    }
                }
                final ParameterValueGroup group = WKT1.createValueGroup(singletonMap(NAME_KEY, "Test"), matrix);
                validate(group);
                assertEquals("num_row",  numRow, group.parameter("num_row").intValue());
                assertEquals("num_col",  numCol, group.parameter("num_col").intValue());
                assertEquals("elements", matrix, WKT1.toMatrix(group));
                assertEquals("elements", matrix, WKT1.toMatrix(new ParameterValueGroupWrapper(group)));
            }
        }
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        assertSerializedEquals(WKT1);
    }
}
