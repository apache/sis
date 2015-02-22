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

import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link MatrixParameters} class using the {@link TensorParameters#WKT1} constant.
 * This class inherits all the tests from {@link TensorParametersTest}, but applies them on a
 * different instance.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn(TensorParametersTest.class)
public strictfp class MatrixParametersTest extends TensorParametersTest {
    /**
     * The expected parameter names according the EPSG convention for the matrix elements.
     *
     * @see TensorParametersTest#ELEMENT_NAMES
     */
    static final String[][] ALPHANUM_NAMES = {
        {"A0", "A1", "A2", "A3"},
        {"B0", "B1", "B2", "B3"},
        {"C0", "C1", "C2", "C3"},
        {"D0", "D1", "D2", "D3"}
    };

    /**
     * Creates a new test case for {@link MatrixParameters}.
     */
    public MatrixParametersTest() {
        super(TensorParameters.WKT1, ELEMENT_NAMES, ALPHANUM_NAMES, null);
    }

    /**
     * Creates a new test case for a {@link MatrixParameters} defined by the subclass.
     *
     * @param param       The instance tested by this class.
     * @param names       The expected parameter names for all matrix elements.
     * @param aliases     The expected parameter aliases for all matrix elements, or {@code null} for no alias.
     * @param identifiers The expected parameter identifiers for all matrix elements, or {@code null} for no identifier.
     */
    MatrixParametersTest(TensorParameters<Double> param, String[][] names, String[][] aliases, short[][] identifiers) {
        super(param, names, aliases, identifiers);
    }

    /**
     * Tests {@link MatrixParameters#indicesToAlias(int[])}.
     */
    @Test
    public void testIndicesToAlias() {
        assertEquals("K0", MatrixParameters.indicesToAlias(new int[] {10, 0}));
        assertEquals("A6", MatrixParameters.indicesToAlias(new int[] { 0, 6}));
        assertEquals("G4", MatrixParameters.indicesToAlias(new int[] { 6, 4}));
        assertNull(MatrixParameters.indicesToAlias(new int[] {27, 2}));
        assertNull(MatrixParameters.indicesToAlias(new int[] {2, 10}));
    }

    /**
     * Tests {@link MatrixParameters#aliasToIndices(String)}.
     */
    @Test
    public void testAliasToIndices() {
        assertArrayEquals(new int[] {10, 0}, MatrixParameters.aliasToIndices("K0"));
        assertArrayEquals(new int[] { 0, 6}, MatrixParameters.aliasToIndices("A6"));
        assertArrayEquals(new int[] { 6, 4}, MatrixParameters.aliasToIndices("G4"));
        assertNull(MatrixParameters.aliasToIndices("2B"));
        assertNull(MatrixParameters.aliasToIndices("elt_1_2"));
    }

    /**
     * Tests serialization.
     */
    @Test
    @Override
    public void testSerialization() {
        assertSame(param, assertSerializedEquals(param));
    }
}
