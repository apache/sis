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
package org.apache.sis.referencing.datum;

import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.Transformation;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.EPSG;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link BursaWolfParameters}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4 (derived from geotk-2.2)
 * @version 0.4
 * @module
 */
public final strictfp class BursaWolfParametersTest extends TestCase {
    /**
     * Tests {@link BursaWolfParameters#getPositionVectorTransformation(boolean)}.
     * This test transform a point from WGS72 to WGS84, and conversely,
     * as documented in the example section of EPSG operation method 9606.
     */
    @Test
    @EPSG(type = OperationMethod.class, code = 9606)
    public void testGetPositionVectorTransformation() {
        final BursaWolfParameters bursaWolf = new BursaWolfParameters(0, 0, 4.5, 0, 0, 0.554, 0.219, null);
        final MatrixSIS toWGS84 = MatrixSIS.castOrCopy(bursaWolf.getPositionVectorTransformation(false));
        final MatrixSIS toWGS72 = MatrixSIS.castOrCopy(bursaWolf.getPositionVectorTransformation(true));
        final MatrixSIS source  = Matrices.create(4, 1, new double[] {3657660.66, 255768.55, 5201382.11, 1});
        final MatrixSIS target  = Matrices.create(4, 1, new double[] {3657660.78, 255778.43, 5201387.75, 1});
        assertMatrixEquals("toWGS84", target, toWGS84.multiply(source), 0.01);
        assertMatrixEquals("toWGS72", source, toWGS72.multiply(target), 0.01);
    }

    /**
     * Tests serialization of <cite>ED87 to WGS 84</cite> parameters (EPSG:1146).
     */
    @Test
    @EPSG(type = Transformation.class, code = 1146)
    public void testToString() {
        final BursaWolfParameters bursaWolf = new BursaWolfParameters(
                -82.981, -99.719, -110.709, -0.5076, 0.1503, 0.3898, -0.3143, null);
        assertEquals("TOWGS84[-82.981, -99.719, -110.709, -0.5076, 0.1503, 0.3898, -0.3143]", bursaWolf.toString());
    }

    /**
     * Tests {@link BursaWolfParameters} serialization.
     */
    @Test
    public void testSerialization() {
        final BursaWolfParameters bursaWolf = new BursaWolfParameters(
                -82.981, -99.719, -110.709, -0.5076, 0.1503, 0.3898, -0.3143, null);
        assertSerializedEquals(bursaWolf);
    }
}
