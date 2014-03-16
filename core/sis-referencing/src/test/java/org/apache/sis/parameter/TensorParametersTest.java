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
import org.junit.Test;

import static java.util.Collections.singletonMap;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import static org.apache.sis.parameter.TensorParameters.WKT1;
import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link TensorParameters} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn(ParametersTest.class)
public final strictfp class TensorParametersTest extends TestCase {
    /**
     * Asserts that the given descriptor has the given name and default value.
     */
    static void assertDescriptorEquals(final String name, final Number defaultValue,
            final GeneralParameterDescriptor actual)
    {
        assertEquals(name, actual.getName().getCode());
        assertEquals(name, defaultValue, ((ParameterDescriptor<?>) actual).getDefaultValue());
    }

    /**
     * Tests {@link TensorParameters#getDimensionDescriptor(int[])}.
     */
    @Test
    public void testGetDimensionDescriptor() {
        final Integer THREE = 3;
        assertDescriptorEquals("num_row", THREE, WKT1.getDimensionDescriptor(0));
        assertDescriptorEquals("num_col", THREE, WKT1.getDimensionDescriptor(1));
    }

    /**
     * Tests {@link TensorParameters#getElementDescriptor(int[])}.
     */
    @Test
    @DependsOnMethod("testIndicesToName")
    public void testGetElementDescriptor() {
        final Double  ZERO  = 0.0;
        final Double  ONE   = 1.0;
        final ParameterDescriptor<Double> e00 = WKT1.getElementDescriptor(0, 0);
        final ParameterDescriptor<Double> e01 = WKT1.getElementDescriptor(0, 1);
        final ParameterDescriptor<Double> e10 = WKT1.getElementDescriptor(1, 0);
        final ParameterDescriptor<Double> e11 = WKT1.getElementDescriptor(1, 1);
        assertDescriptorEquals("elt_0_0", ONE,  e00);
        assertDescriptorEquals("elt_0_1", ZERO, e01);
        assertDescriptorEquals("elt_1_0", ZERO, e10);
        assertDescriptorEquals("elt_1_1", ONE,  e11);
        assertSame(e00, WKT1.getElementDescriptor(0, 0)); // Test caching.
        assertSame(e01, WKT1.getElementDescriptor(0, 1));
        assertSame(e10, WKT1.getElementDescriptor(1, 0));
        assertSame(e11, WKT1.getElementDescriptor(1, 1));
        /*
         * Tests a value outside the cache capacity.
         */
        final int row = TensorParameters.CACHE_SIZE + 1;
        final int col = TensorParameters.CACHE_SIZE + 2;
        assertDescriptorEquals("elt_" + row + "_" + col, ZERO, WKT1.getElementDescriptor(row, col));
    }

    /**
     * Tests {@link TensorParameters#indicesToName(int[])}.
     */
    @Test
    public void testIndicesToName() {
        assertEquals("elt_4_8", WKT1.indicesToName(new int[] {4, 8}));
        assertEquals("elt_7_2", WKT1.indicesToName(new int[] {7, 2}));
    }

    /**
     * Tests {@link TensorParameters#nameToIndices(String)}.
     */
    @Test
    public void testNameToIndices() {
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
        final Double  ZERO  = 0.0;
        final Double  ONE   = 1.0;
        final Integer THREE = 3;
        List<GeneralParameterDescriptor> descriptors = WKT1.descriptors(new int[] {1, 1});
        assertDescriptorEquals("num_row", THREE, descriptors.get(0));
        assertDescriptorEquals("num_col", THREE, descriptors.get(1));
        assertDescriptorEquals("elt_0_0", ONE,   descriptors.get(2));
        assertEquals("size", 3, descriptors.size());

        descriptors = WKT1.descriptors(new int[] {2, 3});
        assertDescriptorEquals("num_row", THREE, descriptors.get(0));
        assertDescriptorEquals("num_col", THREE, descriptors.get(1));
        assertDescriptorEquals("elt_0_0", ONE,   descriptors.get(2));
        assertDescriptorEquals("elt_0_1", ZERO,  descriptors.get(3));
        assertDescriptorEquals("elt_0_2", ZERO,  descriptors.get(4));
        assertDescriptorEquals("elt_1_0", ZERO,  descriptors.get(5));
        assertDescriptorEquals("elt_1_1", ONE,   descriptors.get(6));
        assertDescriptorEquals("elt_1_2", ZERO,  descriptors.get(7));
        assertEquals("size", 8, descriptors.size());

        descriptors = WKT1.descriptors(new int[] {3, 3});
        assertDescriptorEquals("num_row", THREE, descriptors.get( 0));
        assertDescriptorEquals("num_col", THREE, descriptors.get( 1));
        assertDescriptorEquals("elt_0_0", ONE,   descriptors.get( 2));
        assertDescriptorEquals("elt_0_1", ZERO,  descriptors.get( 3));
        assertDescriptorEquals("elt_0_2", ZERO,  descriptors.get( 4));
        assertDescriptorEquals("elt_1_0", ZERO,  descriptors.get( 5));
        assertDescriptorEquals("elt_1_1", ONE,   descriptors.get( 6));
        assertDescriptorEquals("elt_1_2", ZERO,  descriptors.get( 7));
        assertDescriptorEquals("elt_2_0", ZERO,  descriptors.get( 8));
        assertDescriptorEquals("elt_2_1", ZERO,  descriptors.get( 9));
        assertDescriptorEquals("elt_2_2", ONE,   descriptors.get(10));
        assertEquals("size", 11, descriptors.size());
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
        assertSerializedEquals(TensorParameters.WKT1);
    }
}
