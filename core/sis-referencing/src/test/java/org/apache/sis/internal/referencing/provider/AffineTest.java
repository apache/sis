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
package org.apache.sis.internal.referencing.provider;

import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests the {@link Affine} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
@DependsOn(org.apache.sis.parameter.TensorValuesTest.class)
public final strictfp class AffineTest extends TestCase {
    /**
     * Tests {@link Affine#getParameters()} on a standard EPSG:9624 instance.
     */
    @Test
    public void testParameters() {
        verifyParameters(Affine.getProvider(Affine.EPSG_DIMENSION, Affine.EPSG_DIMENSION, true),
                "A0", "A1", "A2",
                "B0", "B1", "B2");
    }

    /**
     * Tests {@link Affine#getParameters()} on an instance that do not comply with EPSG definition.
     * The {@link Affine} provider should fallback on OGC parameters in such cases.
     */
    @Test
    public void testOGCParameters() {
        verifyParameters(Affine.getProvider(3, 2, true),
                "num_row",
                "num_col",
                "elt_0_0", "elt_0_1", "elt_0_2", "elt_0_3",
                "elt_1_0", "elt_1_1", "elt_1_2", "elt_1_3",
                "elt_2_0", "elt_2_1", "elt_2_2", "elt_2_3");
    }

    /**
     * Verifies that the given providers contain parameters of the given names.
     */
    private static void verifyParameters(final Affine provider, final String... expectedNames) {
        int index = 0;
        for (final GeneralParameterDescriptor p : provider.getParameters().descriptors()) {
            final String expectedName = expectedNames[index++];
            assertEquals(expectedName, p.getName().getCode());
        }
        assertEquals("Number of parameters", expectedNames.length, index);
    }

    /**
     * Tests WKT formatting, and in particular the adjustment according
     * whether we comply with EPSG:9624 definition or not.
     */
    @Test
    @DependsOnMethod("testParameters")
    public void testWKT() {
        final Matrix matrix = Matrices.createDiagonal(3, 3);
        assertWktEquals(
                "ParameterGroup[“Affine parametric transformation”," +
                " Id[“EPSG”, 9624, Citation[“OGP”]]]", Affine.parameters(matrix));
        /*
         * Try arbitrary values.
         */
        matrix.setElement(0, 1,  2);  // A1
        matrix.setElement(1, 1,  0);  // B1
        matrix.setElement(1, 2, -1);  // B2
        assertWktEquals(
                "ParameterGroup[“Affine parametric transformation”,\n" +
                "  Parameter[“A1”, 2.0, Id[“EPSG”, 8624]],\n"  +
                "  Parameter[“B1”, 0.0, Id[“EPSG”, 8640]],\n" +
                "  Parameter[“B2”, -1.0, Id[“EPSG”, 8641]],\n" +
                "  Id[“EPSG”, 9624, Citation[“OGP”]]]", Affine.parameters(matrix));
        /*
         * Setting a value on the last row make the matrix non-affine.
         * So it should not be anymore EPSG:9624.
         */
        matrix.setElement(2, 0, 3);  // C0
        assertWktEquals(
                "ParameterGroup[“Affine”,\n" +
                "  Parameter[“num_row”, 3],\n"  +
                "  Parameter[“num_col”, 3],\n"  +
                "  Parameter[“elt_0_1”, 2.0],\n"  +
                "  Parameter[“elt_1_1”, 0.0],\n" +
                "  Parameter[“elt_1_2”, -1.0],\n" +
                "  Parameter[“elt_2_0”, 3.0]]", Affine.parameters(matrix));
    }
}
