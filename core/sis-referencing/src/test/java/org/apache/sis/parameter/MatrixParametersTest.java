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

import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link MatrixParameters} class using the {@link TensorParameters#WKT1} and
 * {@link TensorParameters#EPSG} constants.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn(TensorParametersTest.class)
public final strictfp class MatrixParametersTest extends TestCase {
    /**
     * The expected parameter names according the EPSG convention for the matrix elements.
     *
     * @see TensorParametersTest#ELEMENT_NAMES
     */
    private static final String[][] NAMES = {
        {"A0", "A1", "A2", "A3"},
        {"B0", "B1", "B2", "B3"},
        {"C0", "C1", "C2", "C3"},
        {"D0", "D1", "D2", "D3"}
    };

    /**
     * The expected parameter identifiers for the matrix elements, or 0 if none.
     * Note that the EPSG database contains A3 and B3 parameters, but they are
     * for polynomial transformation, not affine transformation.
     */
    private static final short[][] IDENTIFIERS = {
        {8623, 8624, 8625, 0},
        {8639, 8640, 8641, 0},
        {   0,    0,    0, 0},
        {   0,    0,    0, 0}
    };

    /**
     * Tests {@link MatrixParameters#isEPSG()}.
     */
    @Test
    public void testIsEPSG() {
        assertTrue (((MatrixParameters) TensorParameters.EPSG).isEPSG());
        assertFalse(((MatrixParameters) TensorParameters.WKT1).isEPSG());
    }

    /**
     * Tests {@link MatrixParameters#indicesToAlias(int[])}.
     */
    @Test
    public void testIndicesToAlias() {
        assertEquals("A0", MatrixParameters.indicesToAlias(new int[] {0, 0}));
        assertEquals("A1", MatrixParameters.indicesToAlias(new int[] {0, 1}));
        assertEquals("A2", MatrixParameters.indicesToAlias(new int[] {0, 2}));
        assertEquals("B0", MatrixParameters.indicesToAlias(new int[] {1, 0}));
        assertEquals("B1", MatrixParameters.indicesToAlias(new int[] {1, 1}));
        assertEquals("B2", MatrixParameters.indicesToAlias(new int[] {1, 2}));
        assertNull(MatrixParameters.indicesToAlias(new int[] {27, 2}));
        assertNull(MatrixParameters.indicesToAlias(new int[] {2, 10}));
    }

    /**
     * Tests {@link MatrixParameters#aliasToIndices(String)}.
     */
    @Test
    public void testAliasToIndices() {
        assertArrayEquals(new int[] {0, 0}, MatrixParameters.aliasToIndices("A0"));
        assertArrayEquals(new int[] {0, 1}, MatrixParameters.aliasToIndices("A1"));
        assertArrayEquals(new int[] {0, 2}, MatrixParameters.aliasToIndices("A2"));
        assertArrayEquals(new int[] {1, 0}, MatrixParameters.aliasToIndices("B0"));
        assertArrayEquals(new int[] {1, 1}, MatrixParameters.aliasToIndices("B1"));
        assertArrayEquals(new int[] {1, 2}, MatrixParameters.aliasToIndices("B2"));
        assertNull(MatrixParameters.aliasToIndices("2B"));
        assertNull(MatrixParameters.aliasToIndices("elt_1_2"));
    }

    /**
     * Tests {@link MatrixParameters#indicesToName(int[])}.
     */
    @Test
    @DependsOnMethod({"testIsEPSG", "testIndicesToAlias"})
    public void testIndicesToName() {
        TensorParametersTest.testIndicesToName(TensorParameters.WKT1);
        assertEquals("E8", TensorParameters.EPSG.indicesToName(new int[] {4, 8}));
        assertEquals("H2", TensorParameters.EPSG.indicesToName(new int[] {7, 2}));
    }

    /**
     * Tests {@link MatrixParameters#nameToIndices(String)}.
     */
    @Test
    @DependsOnMethod({"testIsEPSG", "testAliasToIndices"})
    public void testNameToIndices() {
        TensorParametersTest.testNameToIndices(TensorParameters.WKT1);
        assertArrayEquals(new int[] {4, 8}, TensorParameters.EPSG.nameToIndices("E8"));
        assertArrayEquals(new int[] {7, 2}, TensorParameters.EPSG.nameToIndices("H2"));
        assertNull(TensorParameters.EPSG.nameToIndices("other_7_2"));
        assertNull(TensorParameters.EPSG.nameToIndices("elt_7"));
    }

    /**
     * Tests {@link MatrixParameters#getDimensionDescriptor(int)}.
     */
    @Test
    public void testGetDimensionDescriptor() {
        TensorParametersTest.testGetDimensionDescriptor(TensorParameters.WKT1);
        TensorParametersTest.testGetDimensionDescriptor(TensorParameters.EPSG);
    }

    /**
     * Tests {@link TensorParameters#getElementDescriptor(int[])}.
     */
    @Test
    @DependsOnMethod("testIndicesToName")
    public void testGetElementDescriptor() {
        TensorParametersTest.testGetElementDescriptor(TensorParameters.WKT1, TensorParametersTest.ELEMENT_NAMES, NAMES, IDENTIFIERS);
        TensorParametersTest.testGetElementDescriptor(TensorParameters.EPSG, NAMES, TensorParametersTest.ELEMENT_NAMES, IDENTIFIERS);
    }

    /**
     * Tests {@link TensorParameters#descriptors(int[])} for a 1×1, 2×3 and 3×3 matrices.
     */
    @Test
    @DependsOnMethod("testGetElementDescriptor")
    public void testDescriptors() {
        TensorParametersTest.testDescriptors(TensorParameters.WKT1, TensorParametersTest.ELEMENT_NAMES, NAMES, IDENTIFIERS);
        TensorParametersTest.testDescriptors(TensorParameters.EPSG, NAMES, TensorParametersTest.ELEMENT_NAMES, IDENTIFIERS);
    }

    /**
     * Tests serialization.
     */
    @Test
    @DependsOnMethod("testIsEPSG")
    public void testSerialization() {
        assertSame(TensorParameters.EPSG, assertSerializedEquals(TensorParameters.EPSG));
        assertSame(TensorParameters.WKT1, assertSerializedEquals(TensorParameters.WKT1));
    }
}
