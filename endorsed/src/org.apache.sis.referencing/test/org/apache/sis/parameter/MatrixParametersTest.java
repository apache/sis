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
import static org.apache.sis.util.internal.shared.Constants.NUM_ROW;
import static org.apache.sis.util.internal.shared.Constants.NUM_COL;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import static org.apache.sis.referencing.Assertions.assertEpsgIdentifierEquals;
import static org.apache.sis.referencing.Assertions.assertAliasTipEquals;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import static org.opengis.test.Validators.validate;


/**
 * Tests the {@link MatrixParameters} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public class MatrixParametersTest extends TestCase {
    /**
     * The expected parameter names according the <abbr>WKT</abbr> 1 convention for the matrix elements.
     *
     * @see MatrixParametersAlphaNumTest#NAMES
     */
    private static final String[][] NAMES = {
        {"elt_0_0", "elt_0_1", "elt_0_2", "elt_0_3"},
        {"elt_1_0", "elt_1_1", "elt_1_2", "elt_1_3"},
        {"elt_2_0", "elt_2_1", "elt_2_2", "elt_2_3"},
        {"elt_3_0", "elt_3_1", "elt_3_2", "elt_3_3"}
    };

    /**
     * The instance tested by this class.
     */
    MatrixParameters<Double> param;

    /**
     * The expected parameter names for all matrix elements.
     * Example: {@link #NAMES}.
     */
    String[][] names;

    /**
     * The expected parameter aliases for all matrix elements, or {@code null} for no alias.
     * Example: {@link MatrixParametersAlphaNumTest#NAMES}.
     */
    String[][] aliases;

    /**
     * The expected parameter identifiers for all matrix elements, or {@code null} for no identifier.
     * Example: {@link MatrixParametersEPSGTest#IDENTIFIERS}.
     */
    short[][] identifiers;

    /**
     * Creates a new test case for {@link MatrixParameters}.
     */
    @SuppressWarnings("unchecked")
    public MatrixParametersTest() {
        param = MatrixParameters.WKT1;
        names = NAMES;
    }

    /**
     * Asserts that the given descriptor has the given name.
     *
     * @param  name          the expected parameter name.
     * @param  defaultValue  the expected parameter default value.
     * @param  actual        the actual parameter to verify.
     */
    static void verifyDescriptor(String name, Number defaultValue, ParameterDescriptor<?> actual) {
        assertEquals(name, actual.getName().getCode());
        assertEquals(defaultValue, actual.getDefaultValue());
    }

    /**
     * Asserts that the given descriptor has the given name, alias, identifier and default value.
     *
     * @param  defaultValue  the expected parameter default value.
     * @param  actual        the actual parameter to verify.
     * @param  row           row index of the matrix element to test.
     * @param  column        column index of the matrix element to test.
     */
    private void verifyDescriptor(Number defaultValue, ParameterDescriptor<?> actual, int row, int column) {
        assertEquals(names[row][column], actual.getName().getCode());
        assertAliasTipEquals((aliases != null) ? aliases[row][column] : null, actual);
        assertEquals(defaultValue, actual.getDefaultValue());
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
     * Tests {@link MatrixParameters#getDimensionDescriptor(int)}.
     */
    @Test
    public void testGetDimensionDescriptor() {
        final Integer N3 = 3;
        verifyDescriptor(NUM_ROW, N3, param.getDimensionDescriptor(0));
        verifyDescriptor(NUM_COL, N3, param.getDimensionDescriptor(1));
    }

    /**
     * Tests {@link MatrixParameters#getElementDescriptor(int[])}.
     */
    @Test
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
        assertSame(e00, param.getElementDescriptor(0, 0));      // Test caching.
        assertSame(e01, param.getElementDescriptor(0, 1));
        assertSame(e10, param.getElementDescriptor(1, 0));
        assertSame(e11, param.getElementDescriptor(1, 1));
    }

    /**
     * Tests {@link MatrixParameters#getElementDescriptor(int[])} with a value outside the cache capacity.
     */
    public void testGetElementDescriptorOutsideCache() {
        final int row = MatrixParameters.MAX_CACHE_SIZE + 1;
        final int col = MatrixParameters.MAX_CACHE_SIZE + 2;
        verifyDescriptor("elt_" + row + "_" + col, 0.0, param.getElementDescriptor(row, col));
    }

    /**
     * Tests {@link MatrixParameters#indicesToName(int[])}.
     */
    @Test
    public void testIndicesToName() {
        assertEquals("elt_4_8", param.indicesToName(new int[] {4, 8}));
        assertEquals("elt_7_2", param.indicesToName(new int[] {7, 2}));
    }

    /**
     * Tests {@link MatrixParameters#nameToIndices(String)}.
     */
    @Test
    public void testNameToIndices() {
        assertArrayEquals(new int[] {4, 8}, param.nameToIndices("elt_4_8"));
        assertArrayEquals(new int[] {7, 2}, param.nameToIndices("elt_7_2"));
        assertNull(param.nameToIndices("other_7_2"));
        assertNull(param.nameToIndices("elt_7"));
        var e = assertThrows(IllegalArgumentException.class, () -> param.nameToIndices("elt_7_2_3"));
        assertFalse(e.getMessage().isEmpty());
    }

    /**
     * Tests {@link MatrixParameters#getAllDescriptors(int[])} for a 1×1, 2×3 and 3×3 matrices.
     */
    @Test
    public void testGetAllDescriptors() {
        final Double  N0 = 0.0;
        final Double  N1 = 1.0;
        final Integer N3 = 3;
        ParameterDescriptor<?>[] descriptors = param.getAllDescriptors(1, 1);
        verifyDescriptor(NUM_ROW, N3, descriptors[0]);
        verifyDescriptor(NUM_COL, N3, descriptors[1]);
        verifyDescriptor(N1, descriptors[2], 0, 0);
        assertEquals(3, descriptors.length);

        descriptors = param.getAllDescriptors(2, 3);
        verifyDescriptor(NUM_ROW, N3, descriptors[0]);
        verifyDescriptor(NUM_COL, N3, descriptors[1]);
        verifyDescriptor(N1, descriptors[2], 0, 0);
        verifyDescriptor(N0, descriptors[3], 0, 1);
        verifyDescriptor(N0, descriptors[4], 0, 2);
        verifyDescriptor(N0, descriptors[5], 1, 0);
        verifyDescriptor(N1, descriptors[6], 1, 1);
        verifyDescriptor(N0, descriptors[7], 1, 2);
        assertEquals(8, descriptors.length);

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
        assertEquals(11, descriptors.length);

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
        assertEquals(18, descriptors.length);
    }

    /**
     * Tests {@link MatrixParameters#createValueGroup(Map, Matrix)} and its converse
     * {@link MatrixParameters#toMatrix(ParameterValueGroup)}.
     */
    @Test
    public void testMatrixConversion() {
        final int size = StrictMath.min(6, MatrixParameters.MAX_CACHE_SIZE);
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
                        Map.of(ParameterDescriptor.NAME_KEY, "Test"), matrix);
                validate(group);
                assertEquals(numRow, group.parameter(NUM_ROW).intValue());
                assertEquals(numCol, group.parameter(NUM_COL).intValue());
                assertEquals(matrix, param.toMatrix(group));
                assertEquals(matrix, param.toMatrix(new ParameterValueGroupWrapper(group)));
            }
        }
    }

    /**
     * Tests serialization.
     */
    @Test
    public void testSerialization() {
        assertSame(param, assertSerializedEquals(param));
    }
}
