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

import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.referencing.operation.matrix.Matrices;
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
     * Ensures that the {@link Affine#EPSG_INDEX} value is consistent with
     * {@link Affine#cacheIndex(int, int)} computation.
     */
    @Test
    public void testEpsgIndex() {
        assertEquals("EPSG_INDEX", Affine.cacheIndex(Affine.EPSG_DIMENSION, Affine.EPSG_DIMENSION), Affine.EPSG_INDEX);
    }

    /**
     * Tests WKT formatting, and in particular the adjustment according
     * whether we comply with EPSG:9624 definition or not.
     */
    public void testWKT() {
        final ParameterValueGroup matrix = Affine.parameters(Matrices.createDiagonal(3, 3));
        assertWktEquals(
                "ParameterGroup[“Affine general parametric transformation”," +
                " Id[“EPSG”, 9624, Citation[“OGP”]]]", matrix);
        /*
         * Try arbitrary values.
         */
        matrix.parameter("A1").setValue( 2);
        matrix.parameter("B1").setValue( 0);
        matrix.parameter("B2").setValue(-1);
        assertWktEquals(
                "ParameterGroup[“Affine general parametric transformation”,\n" +
                "  Parameter[“A1”, 2.0, Id[“EPSG”, 8624]],\n"  +
                "  Parameter[“B1”, 0.0, Id[“EPSG”, 8640]],\n" +
                "  Parameter[“B2”, -1.0, Id[“EPSG”, 8641]],\n" +
                "  Id[“EPSG”, 9624, Citation[“OGP”]]]", matrix);
        /*
         * Setting a value on the last row make the matrix non-affine.
         * So it should not ne anymore EPSG:9624.
         *
         * TODO: The parameter should also be renamed as "elt_0_1", "elt_1_1", etc.
         */
        matrix.parameter("C0").setValue(3);
        assertWktEquals(
                "ParameterGroup[“Affine”,\n" +
                "  Parameter[“A1”, 2.0, Id[“EPSG”, 8624]],\n"  +
                "  Parameter[“B1”, 0.0, Id[“EPSG”, 8640]],\n" +
                "  Parameter[“B2”, -1.0, Id[“EPSG”, 8641]],\n" +
                "  Parameter[“C0”, 3.0]]", matrix);
    }
}
