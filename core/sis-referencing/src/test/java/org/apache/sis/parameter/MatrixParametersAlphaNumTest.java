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
 * Tests the {@link MatrixParametersAlphaNum} class using the {@link TensorParameters#ALPHANUM} constant.
 * This class inherits all the tests from {@link TensorParametersTest}, but applies them on a different instance.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn(MatrixParametersTest.class)
public final strictfp class MatrixParametersAlphaNumTest extends MatrixParametersTest {
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
     * Creates a new test case for {@link MatrixParameters}.
     */
    public MatrixParametersAlphaNumTest() {
        super(TensorParameters.ALPHANUM, ALPHANUM_NAMES, ELEMENT_NAMES, IDENTIFIERS);
    }

    /**
     * Tests {@link MatrixParameters#indicesToAlias(int[])}.
     */
    @Test
    @Override
    public void testIndicesToAlias() {
        assertEquals("A0", MatrixParameters.indicesToAlias(new int[] {0, 0}));
        assertEquals("A1", MatrixParameters.indicesToAlias(new int[] {0, 1}));
        assertEquals("A2", MatrixParameters.indicesToAlias(new int[] {0, 2}));
        assertEquals("B0", MatrixParameters.indicesToAlias(new int[] {1, 0}));
        assertEquals("B1", MatrixParameters.indicesToAlias(new int[] {1, 1}));
        assertEquals("B2", MatrixParameters.indicesToAlias(new int[] {1, 2}));
    }

    /**
     * Tests {@link MatrixParameters#aliasToIndices(String)}.
     */
    @Test
    @Override
    public void testAliasToIndices() {
        assertArrayEquals(new int[] {0, 0}, MatrixParameters.aliasToIndices("A0"));
        assertArrayEquals(new int[] {0, 1}, MatrixParameters.aliasToIndices("A1"));
        assertArrayEquals(new int[] {0, 2}, MatrixParameters.aliasToIndices("A2"));
        assertArrayEquals(new int[] {1, 0}, MatrixParameters.aliasToIndices("B0"));
        assertArrayEquals(new int[] {1, 1}, MatrixParameters.aliasToIndices("B1"));
        assertArrayEquals(new int[] {1, 2}, MatrixParameters.aliasToIndices("B2"));
    }

    /**
     * Tests {@link MatrixParametersAlphaNum#indicesToName(int[])}.
     */
    @Test
    @Override
    public void testIndicesToName() {
        assertEquals("E8", param.indicesToName(new int[] {4, 8}));
        assertEquals("H2", param.indicesToName(new int[] {7, 2}));
    }
}
