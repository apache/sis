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

import java.util.Date;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.metadata.iso.extent.Extents;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.matrix.NoninvertibleMatrixException;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the {@link BursaWolfParameters} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
public final strictfp class BursaWolfParametersTest extends TestCase {
    /**
     * The conversion factor from arc-seconds to radians.
     */
    private static final double TO_RADIANS = StrictMath.PI / (180 * 60 * 60);

    /**
     * Returns the parameters for the <cite>WGS 72 to WGS 84 (2)</cite> transformation (EPSG:1238).
     * Area of validity is the World.
     */
    static BursaWolfParameters createWGS72_to_WGS84() {
        final BursaWolfParameters bursaWolf = new BursaWolfParameters(GeodeticDatumMock.WGS84, Extents.WORLD);
        bursaWolf.tZ = 4.5;
        bursaWolf.rZ = 0.554;
        bursaWolf.dS = 0.219;
        bursaWolf.verify(PrimeMeridianMock.GREENWICH);
        assertFalse("isIdentity",    bursaWolf.isIdentity());
        assertFalse("isTranslation", bursaWolf.isTranslation());
        return bursaWolf;
    }

    /**
     * Returns the parameters for the <cite>ED87 to WGS 84 (1)</cite> transformation (EPSG:1146).
     * Area of validity is the North Sea: 5.05°W to 11.13°E in longitude and 51.04°N to 62.0°N in latitude.
     */
    static BursaWolfParameters createED87_to_WGS84() {
        final BursaWolfParameters bursaWolf = new BursaWolfParameters(GeodeticDatumMock.WGS84, new DefaultExtent("Europe - North Sea",
                new DefaultGeographicBoundingBox(-5.05, 11.13, 51.04, 62.0), null, null));
        bursaWolf.tX =  -82.981;
        bursaWolf.tY =  -99.719;
        bursaWolf.tZ = -110.709;
        bursaWolf.rX =   -0.5076;
        bursaWolf.rY =    0.1503;
        bursaWolf.rZ =    0.3898;
        bursaWolf.dS =   -0.3143;
        bursaWolf.verify(PrimeMeridianMock.GREENWICH);
        assertFalse("isIdentity",    bursaWolf.isIdentity());
        assertFalse("isTranslation", bursaWolf.isTranslation());
        return bursaWolf;
    }

    /**
     * Returns the parameters for the <cite>NTF to WGS 84 (1)</cite> transformation (EPSG:1193).
     * Area of validity is France - onshore - mainland and Corsica.
     * This transformation uses only translation parameters.
     */
    static BursaWolfParameters createNTF_to_WGS84() {
        final BursaWolfParameters bursaWolf = new BursaWolfParameters(GeodeticDatumMock.WGS84, Extents.WORLD);
        bursaWolf.tX = -168;
        bursaWolf.tY =  -60;
        bursaWolf.tZ =  320;
        bursaWolf.verify(PrimeMeridianMock.GREENWICH);
        assertFalse("isIdentity",    bursaWolf.isIdentity());
        assertTrue ("isTranslation", bursaWolf.isTranslation());
        return bursaWolf;
    }

    /**
     * Invokes {@link BursaWolfParameters#getPositionVectorTransformation(Date)}
     * and compares with our own matrix calculated using double arithmetic.
     */
    private static MatrixSIS getPositionVectorTransformation(final BursaWolfParameters p) {
        final double   S = 1 + p.dS / BursaWolfParameters.PPM;
        final double  RS = TO_RADIANS * S;
        final Matrix4 expected = new Matrix4(
                   S,  -p.rZ*RS,  +p.rY*RS,  p.tX,
            +p.rZ*RS,         S,  -p.rX*RS,  p.tY,
            -p.rY*RS,  +p.rX*RS,         S,  p.tZ,
                   0,         0,         0,  1);

        final MatrixSIS matrix = MatrixSIS.castOrCopy(p.getPositionVectorTransformation(null));
        assertMatrixEquals("getPositionVectorTransformation", expected, matrix, p.isTranslation() ? 0 : 1E-14);
        return matrix;
    }

    /**
     * Tests {@link BursaWolfParameters#getValues()}.
     */
    @Test
    public void testGetValues() {
        assertArrayEquals("Translation only", new double[] {-168, -60, 320},
                createNTF_to_WGS84().getValues(), STRICT);
        assertArrayEquals("All 7 params", new double[] {-82.981, -99.719, -110.709, -0.5076, 0.1503, 0.3898, -0.3143},
                createED87_to_WGS84().getValues(), STRICT);
        assertArrayEquals("Mixed", new double[] {0, 0, 4.5, 0, 0, 0.554, 0.219},
                createWGS72_to_WGS84().getValues(), STRICT);
    }

    /**
     * Tests {@link BursaWolfParameters#setValues(double[])}.
     */
    @Test
    @DependsOnMethod("testGetValues")
    public void testSetValues() {
        final BursaWolfParameters actual =  createWGS72_to_WGS84();
        final BursaWolfParameters expected = createED87_to_WGS84();
        final double[] values = expected.getValues();
        assertFalse("equals(Object) before to set the values.", actual.equals(expected));
        actual.setValues(values);
        assertArrayEquals("getValues() after setting the values.", values, actual.getValues(), STRICT);
        // Can not test assertEquals(expected, actual) because of different geographic extent.
    }

    /**
     * Tests {@link BursaWolfParameters#getPositionVectorTransformation(Date)}.
     * This test transform a point from WGS72 to WGS84, and conversely,
     * as documented in the example section of EPSG operation method 9606.
     *
     * @throws NoninvertibleMatrixException Should never happen.
     */
    @Test
    public void testGetPositionVectorTransformation() throws NoninvertibleMatrixException {
        final BursaWolfParameters bursaWolf = createWGS72_to_WGS84();
        final MatrixSIS toWGS84 = getPositionVectorTransformation(bursaWolf);
        final MatrixSIS toWGS72 = toWGS84.inverse();
        final MatrixSIS source  = Matrices.create(4, 1, new double[] {3657660.66, 255768.55, 5201382.11, 1});
        final MatrixSIS target  = Matrices.create(4, 1, new double[] {3657660.78, 255778.43, 5201387.75, 1});
        assertMatrixEquals("toWGS84", target, toWGS84.multiply(source), 0.01);
        assertMatrixEquals("toWGS72", source, toWGS72.multiply(target), 0.01);
        /*
         * Tests the optimized path for translation-only parameters.
         * Matrices having only translation terms are much easier to predict.
         */
        assertMatrixEquals("Translation only", new Matrix4(
                1, 0, 0, -168,
                0, 1, 0,  -60,
                0, 0, 1,  320,
                0, 0, 0,    1), getPositionVectorTransformation(createNTF_to_WGS84()), 0);
    }

    /**
     * Tests the {@link BursaWolfParameters#setPositionVectorTransformation(Matrix, double)} method.
     * This is an internal consistency test.
     */
    @Test
    @DependsOnMethod("testGetPositionVectorTransformation")
    public void testSetPositionVectorTransformation() {
        final BursaWolfParameters bursaWolf = createED87_to_WGS84();
        final Matrix matrix = bursaWolf.getPositionVectorTransformation(null);
        final BursaWolfParameters actual = new BursaWolfParameters(
                bursaWolf.getTargetDatum(), bursaWolf.getDomainOfValidity());
        actual.setPositionVectorTransformation(matrix, 1E-10);
        assertEquals(bursaWolf, actual);
    }

    /**
     * Multiplies the <cite>ED87 to WGS 84</cite> parameters (EPSG:1146) transformation by its inverse and
     * verifies that the result is very close to the identity matrix, thanks to the double-double arithmetic.
     * This is an internal consistency test.
     *
     * @throws NoninvertibleMatrixException Should never happen.
     */
    @Test
    @DependsOnMethod("testGetPositionVectorTransformation")
    public void testProductOfInverse() throws NoninvertibleMatrixException {
        final BursaWolfParameters bursaWolf = createED87_to_WGS84();
        final MatrixSIS toWGS84 = getPositionVectorTransformation(bursaWolf);
        final MatrixSIS toED87  = getPositionVectorTransformation(bursaWolf).inverse();
        final MatrixSIS product = toWGS84.multiply(toED87);
        assertTrue(product.isIdentity());
    }

    /**
     * Tests {@link BursaWolfParameters#invert()}.
     *
     * @throws NoninvertibleMatrixException Should never happen.
     */
    @Test
    @DependsOnMethod("testProductOfInverse")
    public void testInvert() throws NoninvertibleMatrixException {
        final BursaWolfParameters bursaWolf = createED87_to_WGS84();
        final Matrix original = getPositionVectorTransformation(bursaWolf).inverse();
        bursaWolf.invert();
        final Matrix inverse = getPositionVectorTransformation(bursaWolf);
        assertMatrixEquals("invert", original, inverse, 0.001);
    }

    /**
     * Tests the string representation of <cite>ED87 to WGS 84</cite> parameters (EPSG:1146).
     */
    @Test
    @DependsOnMethod("testGetValues")
    public void testToString() {
        assertEquals("ToWGS84[-82.981, -99.719, -110.709, -0.5076, 0.1503, 0.3898, -0.3143]", createED87_to_WGS84().toString());
        assertEquals("ToWGS84[-168.0, -60.0, 320.0]", createNTF_to_WGS84().toString());
    }

    /**
     * Tests {@link BursaWolfParameters} serialization.
     */
    @Test
    public void testSerialization() {
        final BursaWolfParameters bursaWolf = createED87_to_WGS84();
        assertSerializedEquals(bursaWolf);
    }
}
