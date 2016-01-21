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
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.NoninvertibleMatrixException;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.date;
import static org.apache.sis.internal.referencing.Formulas.JULIAN_YEAR_LENGTH;


/**
 * Tests the {@link TimeDependentBWP} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 */
@DependsOn(BursaWolfParametersTest.class)
public final strictfp class TimeDependentBWPTest extends TestCase {
    /**
     * Creates a {@code TimeDependentBWP} using the example given in the EPSG database for operation method EPSG:1053.
     * The target datum given by the EPG example is actually GDA94, but it is coincident with WGS84 to within 1 metre.
     * For the purpose of this test, the target datum does not matter anyway.
     */
    private static TimeDependentBWP create() {
        final TimeDependentBWP p = new TimeDependentBWP(GeodeticDatumMock.WGS84, null, date("1994-01-01 00:00:00"));
        p.tX = -0.08468;    p.dtX = +1.42;
        p.tY = -0.01942;    p.dtY = +1.34;
        p.tZ = +0.03201;    p.dtZ = +0.90;
        p.rX = +0.0004254;  p.drX = -1.5461;
        p.rY = -0.0022578;  p.drY = -1.1820;
        p.rZ = -0.0024015;  p.drZ = -1.1551;
        p.dS = +0.00971;    p.ddS = +0.000109;
        return p;
    }

    /**
     * Tests {@link TimeDependentBWP#invert()}. This will indirectly tests {@link TimeDependentBWP#getValues()}
     * followed by {@link TimeDependentBWP#setValues(double[])} because of the way the {@code invert()} method
     * is implemented.
     */
    @Test
    public void testInvert() {
        /*
         * Opportunistically test getValue() first because it is used by TimeDependentBWP.invert().
         */
        final double[] expected = {
            -0.08468, -0.01942, +0.03201, +0.0004254, -0.0022578, -0.0024015, +0.00971,
            +1.42,    +1.34,    +0.90,    -1.5461,    -1.1820,    -1.1551,    +0.000109
        };
        final TimeDependentBWP p = create();
        assertArrayEquals(expected, p.getValues(), STRICT);
        /*
         * Now perform the actual TimeDependentBWP.invert() test.
         */
        for (int i=0; i<expected.length; i++) {
            expected[i] = -expected[i];
        }
        p.invert();
        assertArrayEquals(expected, p.getValues(), STRICT);
    }

    /**
     * Tests the {@link TimeDependentBWP#setPositionVectorTransformation(Matrix, double)} method
     * using the example given in the EPSG database for operation method EPSG:1053.
     *
     * @throws NoninvertibleMatrixException Should not happen.
     */
    @Test
    @DependsOnMethod("testEpsgCalculation")
    public void testSetPositionVectorTransformation() throws NoninvertibleMatrixException {
        /*
         * The transformation that we are going to test use as input
         * geocentric coordinates on ITRF2008 at epoch 2013.9.
         */
        final TimeDependentBWP p = create();
        final Date time = p.getTimeReference();
        time.setTime(time.getTime() + StrictMath.round((2013.9 - 1994) * JULIAN_YEAR_LENGTH));
        assertEquals(date("2013-11-25 11:24:00"), time);
        /*
         * Transform the point given in the EPSG example and compare with the coordinate
         * that we obtain if we do the calculation ourself using the intermediate values
         * given in EPSG example.
         */
        final MatrixSIS toGDA94    = MatrixSIS.castOrCopy(p.getPositionVectorTransformation(time));
        final MatrixSIS toITRF2008 = toGDA94.inverse();
        final MatrixSIS source     = Matrices.create(4, 1, new double[] {-3789470.702, 4841770.411, -1690893.950, 1});
        final MatrixSIS target     = Matrices.create(4, 1, new double[] {-3789470.008, 4841770.685, -1690895.103, 1});
        final MatrixSIS actual     = toGDA94.multiply(source);
        compareWithExplicitCalculation(actual);
        /*
         * Now compare with the expected value given in the EPSG example. The 0.013 tolerance threshold
         * is for the X ordinate and has been determined empirically in testEpsgCalculation().
         */
        assertMatrixEquals("toGDA94",    target, actual, 0.013);
        assertMatrixEquals("toITRF2008", source, toITRF2008.multiply(target), 0.013);
    }

    /**
     * Compares the coordinates calculated with the {@link TimeDependentBWP#getPositionVectorTransformation(Date)}
     * matrix with the coordinates calculated ourselves from the numbers given in the EPSG examples. Note that the
     * EPSG documentation truncates the numerical values given in their example, so it is normal that we have a
     * slight difference.
     *
     * @param actual The coordinates calculated by the matrix, or {@code null} for comparing against
     *        the EPSG expected values.
     */
    private void compareWithExplicitCalculation(final Matrix actual) {
        /*
         * Following are Bursa-Wolf parameters corrected for the rate of changes. The numerical values here
         * are different than the numerical values in testSetPositionVectorTransformation() because of this
         * correction. Units are metres and radians.
         */
        final double tX = -0.056;
        final double tY = +0.007;
        final double tZ = +0.050;
        final double rX = -1.471021E-07;
        final double rY = -1.249830E-07;
        final double rZ = -1.230844E-07;
        final double dS = +0.01188;
        /*
         * The source coordinates. This is the same coordinates than testSetPositionVectorTransformation().
         */
        final double Xs = -3789470.702;
        final double Ys =  4841770.411;
        final double Zs = -1690893.950;
        /*
         * Application of the 7 parameter Position Vector transformation.
         * (the multiplications by 1 are like no-op, but kept for visualizing the matrix terms).
         */
        final double M  = 1 + (dS * 1E-6);
        final double Xt = M * (  1 * Xs   +  -rZ * Ys   +   rY * Zs)   +   tX;
        final double Yt = M * (+rZ * Xs   +    1 * Ys   +  -rX * Zs)   +   tY;
        final double Zt = M * (-rY * Xs   +   rX * Ys   +    1 * Zs)   +   tZ;
        /*
         * Compares with the coordinates calculated by the TimeDependentBWP matrix (first case), or
         * with the expected coordinates provided by EPSG (second case). See testEpsgCalculation()
         * for a discussion about the second case.
         */
        if (actual != null) {
            assertEquals("X", Xt, actual.getElement(0, 0), 0.0005);
            assertEquals("Y", Yt, actual.getElement(1, 0), 0.0005);
            assertEquals("Z", Zt, actual.getElement(2, 0), 0.0005);
        } else {
            assertEquals("X", -3789470.008, Xt, 0.013); // Smallest tolerance value such as the test do not fail.
            assertEquals("Y",  4841770.685, Yt, 0.009);
            assertEquals("Z", -1690895.103, Zt, 0.003);
        }
    }

    /**
     * Tries to apply ourselves the example given for operation method EPSG:1053 in EPSG database 8.2.7.
     * For a unknown reason, we do not get exactly the values that EPSG said that we should get when applying
     * the 7 parameter Position Vector transformation from the parameter values as corrected by EPSG themselves.
     * We find a difference of 13, 9 and 3 millimetres along the X, Y and Z axis respectively.
     *
     * <p>The purpose of this test is to ensure that we get the same errors when calculating from the corrected values
     * provided by EPSG, rather than as an error in {@link TimeDependentBWP#getPositionVectorTransformation(Date)}</p>
     */
    @Test
    public void testEpsgCalculation() {
        compareWithExplicitCalculation(null);
    }
}
