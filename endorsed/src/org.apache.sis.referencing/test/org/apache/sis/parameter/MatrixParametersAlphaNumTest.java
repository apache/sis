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

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests the {@link MatrixParameters} class using the {@link MatrixParameters#ALPHANUM} constant.
 * This class inherits all the tests from {@link MatrixParametersTest},
 * but applies them on a different instance.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class MatrixParametersAlphaNumTest extends MatrixParametersTest {
    /**
     * The expected parameter names according the alphanumeric convention for the matrix elements.
     *
     * @see MatrixParametersTest#NAMES
     */
    private static final String[][] NAMES = {
        {"A0", "A1", "A2", "A3"},
        {"B0", "B1", "B2", "B3"},
        {"C0", "C1", "C2", "C3"},
        {"D0", "D1", "D2", "D3"}
    };

    /**
     * Creates a new test case for {@link MatrixParameters}.
     */
    public MatrixParametersAlphaNumTest() {
        param = MatrixParameters.ALPHANUM;
        aliases = names;
        names = NAMES;
    }

    /**
     * Tests {@link MatrixParameters#indicesToName(int[])}.
     */
    @Test
    @Override
    public void testIndicesToName() {
        assertEquals("K0", param.indicesToName(new int[] {10, 0}));
        assertEquals("A6", param.indicesToName(new int[] { 0, 6}));
        assertEquals("G4", param.indicesToName(new int[] { 6, 4}));
        assertNull(param.indicesToName(new int[] {27, 2}));
        assertNull(param.indicesToName(new int[] {2, 10}));
    }

    /**
     * Tests {@link MatrixParameters#nameToIndices(String)}.
     */
    @Test
    @Override
    public void testNameToIndices() {
        assertArrayEquals(new int[] {10, 0}, param.nameToIndices("K0"));
        assertArrayEquals(new int[] { 0, 6}, param.nameToIndices("A6"));
        assertArrayEquals(new int[] { 6, 4}, param.nameToIndices("G4"));
        assertArrayEquals(new int[] { 1, 2}, param.nameToIndices("elt_1_2"));
        assertNull(param.nameToIndices("2B"));
        super.testNameToIndices();  // Test aliases.
    }

    /**
     * Tests {@link MatrixParameters#getElementDescriptor(int[])} with a value outside the cache capacity.
     */
    @Test
    @Override
    public void testGetElementDescriptorOutsideCache() {
        verifyDescriptor("G7", 0.0, param.getElementDescriptor(6, 7));
    }
}
