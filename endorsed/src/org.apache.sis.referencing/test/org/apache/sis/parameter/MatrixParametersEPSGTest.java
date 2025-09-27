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
 * Tests the {@link MatrixParameters} class using the {@link MatrixParameters#EPSG} constant.
 * This class inherits all the tests from {@link MatrixParametersTest},
 * but applies them on a different instance.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class MatrixParametersEPSGTest extends MatrixParametersTest {
    /**
     * The expected parameter names according the <abbr>EPSG</abbr> convention for the matrix elements.
     */
    private static final String[][] NAMES = {
        {"A1",      "A2",      "A0",      "elt_0_3"},
        {"B1",      "B2",      "B0",      "elt_1_3"},
        {"elt_2_0", "elt_2_1", "elt_2_2", "elt_2_3"},
        {"elt_3_0", "elt_3_1", "elt_3_2", "elt_3_3"}
    };

    /**
     * The expected parameter aliases.
     */
    private static final String[][] ALIASES = {
        {"elt_0_0", "elt_0_1", "elt_0_2", null},
        {"elt_1_0", "elt_1_1", "elt_1_2", null},
        {null,      null,      null, null},
        {null,      null,      null, null}
    };

    /**
     * The expected parameter identifiers for the matrix elements, or 0 if none.
     * Note that the EPSG database contains A3 and B3 parameters, but they are
     * for polynomial transformation, not affine transformation.
     */
    private static final short[][] IDENTIFIERS = {
        {8624, 8625, 8623, 0},
        {8640, 8641, 8639, 0},
        {   0,    0,    0, 0},
        {   0,    0,    0, 0}
    };

    /**
     * Creates a new test case for {@link MatrixParameters#EPSG}.
     */
    public MatrixParametersEPSGTest() {
        param       = MatrixParameters.EPSG;
        names       = NAMES;
        aliases     = ALIASES;
        identifiers = IDENTIFIERS;
    }

    /**
     * Tests {@link MatrixParameters#indicesToName(int[])}.
     */
    @Test
    @Override
    public void testIndicesToName() {
        assertEquals("A1", param.indicesToName(new int[] {0, 0}));
        assertEquals("A2", param.indicesToName(new int[] {0, 1}));
        assertEquals("A0", param.indicesToName(new int[] {0, 2}));
        assertEquals("B1", param.indicesToName(new int[] {1, 0}));
        assertEquals("B2", param.indicesToName(new int[] {1, 1}));
        assertEquals("B0", param.indicesToName(new int[] {1, 2}));
    }

    /**
     * Tests {@link MatrixParameters#nameToIndices(String)}.
     */
    @Test
    @Override
    public void testNameToIndices() {
        assertArrayEquals(new int[] {0, 0}, param.nameToIndices("A1"));
        assertArrayEquals(new int[] {0, 1}, param.nameToIndices("A2"));
        assertArrayEquals(new int[] {0, 2}, param.nameToIndices("A0"));
        assertArrayEquals(new int[] {1, 0}, param.nameToIndices("B1"));
        assertArrayEquals(new int[] {1, 1}, param.nameToIndices("B2"));
        assertArrayEquals(new int[] {1, 2}, param.nameToIndices("B0"));
    }
}
