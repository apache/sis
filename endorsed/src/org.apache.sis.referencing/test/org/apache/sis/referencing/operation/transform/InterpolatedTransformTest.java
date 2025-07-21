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
package org.apache.sis.referencing.operation.transform;

import java.net.URL;
import java.util.Arrays;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.provider.NADCON;
import org.apache.sis.referencing.operation.provider.NTv2;
import org.apache.sis.referencing.privy.Formulas;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.apache.sis.referencing.operation.provider.FranceGeocentricInterpolationTest;
import org.apache.sis.referencing.operation.provider.NADCONTest;
import org.apache.sis.referencing.operation.provider.NTv2Test;


/**
 * Tests {@link InterpolatedTransform}.
 * Tested transformations are:
 *
 * <ul>
 *   <li>Simple case based on sinusoidal calculations (easier to debug).</li>
 *   <li>From NTF to RGF93 using a NTv2 grid.</li>
 *   <li>From NAD27 to NAD83 using a NADCON grid.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class InterpolatedTransformTest extends MathTransformTestCase {
    /**
     * Creates a new test case.
     */
    public InterpolatedTransformTest() {
    }

    /**
     * Creates an {@link InterpolatedTransform} derived from a sinusoidal formula.
     * We do not really need {@code InterpolatedTransform} for sinusoidal formulas,
     * but we use them for testing purpose since they are easier to debug.
     *
     * @param  rotation  rotation angle, in degrees. Use 0 for debugging a simple case.
     * @return suggested points to use for testing purposes as an array of length 2,
     *         with source coordinates in the first array and target coordinates in the second array.
     */
    private double[][] createSinusoidal(final double rotation) throws TransformException {
        final var grid = new SinusoidalShiftGrid(rotation);
        transform = new InterpolatedTransform(grid);
        return grid.samplePoints();
    }

    /**
     * Creates the same transformation as <q>France geocentric interpolation</q> transform
     * (approximately), but using shifts in geographic domain instead of in geocentric domain.
     *
     * @throws FactoryException if an error occurred while loading the grid.
     */
    private void createRGF93() throws FactoryException {
        final URL file = NTv2Test.getResourceAsConvertibleURL(NTv2Test.TEST_FILE);
        final NTv2 provider = new NTv2();
        final ParameterValueGroup values = provider.getParameters().createValue();
        values.parameter("Latitude and longitude difference file").setValue(file);    // Automatic conversion from URL to Path.
        transform = provider.createMathTransform(DefaultMathTransformFactory.provider(), values);
        tolerance = Formulas.ANGULAR_TOLERANCE;
        validate();
    }

    /**
     * Creates a transformation from NAD27 to NAD93.
     *
     * @throws FactoryException if an error occurred while loading the grid.
     */
    private void createNADCON() throws FactoryException {
        final URL latitudeShifts  = NADCONTest.getResourceAsConvertibleURL(NADCONTest.TEST_FILE + ".laa");
        final URL longitudeShifts = NADCONTest.getResourceAsConvertibleURL(NADCONTest.TEST_FILE + ".loa");
        final NADCON provider = new NADCON();
        final ParameterValueGroup values = provider.getParameters().createValue();
        values.parameter("Latitude difference file").setValue(latitudeShifts);
        values.parameter("Longitude difference file").setValue(longitudeShifts);
        transform = provider.createMathTransform(DefaultMathTransformFactory.provider(), values);
        tolerance = NADCONTest.ANGULAR_TOLERANCE;
        validate();
    }

    /**
     * Tests forward transformation of sample points.
     *
     * @throws TransformException if an error occurred while transforming a coordinate.
     */
    @Test
    public void testForwardTransform() throws TransformException {
        isInverseTransformSupported = false;                                            // For focusing on a single aspect.
        final double[][] samplePoints = createSinusoidal(-15);
        tolerance = 1E-10;
        verifyTransform(Arrays.copyOf(samplePoints[0], SinusoidalShiftGrid.FIRST_FRACTIONAL_COORDINATE),
                        Arrays.copyOf(samplePoints[1], SinusoidalShiftGrid.FIRST_FRACTIONAL_COORDINATE));
        /*
         * For non-integer coordinates, we need to relax the tolerance threshold because the linear interpolations
         * computed by InterpolatedTransform do not give the same results as the calculation done with cosine by
         * SinudoisalShiftGrid. The result of tested point is about (81.96 22.89).
         */
        tolerance = 0.01;
        verifyTransform(samplePoints[0], samplePoints[1]);
    }

    /**
     * Tests inverse transformation of sample points.
     * Inverse transform requires derivative.
     *
     * @throws TransformException if an error occurred while transforming a coordinate.
     */
    @Test
    public void testInverseTransform() throws TransformException {
        isInverseTransformSupported = false;                                            // For focusing on a single aspect.
        final double[][] samplePoints = createSinusoidal(-20);
        transform = transform.inverse();
        tolerance = SinusoidalShiftGrid.PRECISION;
        verifyTransform(Arrays.copyOf(samplePoints[1], SinusoidalShiftGrid.FIRST_FRACTIONAL_COORDINATE),
                        Arrays.copyOf(samplePoints[0], SinusoidalShiftGrid.FIRST_FRACTIONAL_COORDINATE));
        /*
         * For non-integer coordinates, we need to relax the tolerance threshold because the linear interpolations
         * computed by InterpolatedTransform do not give the same results as the calculation done with cosine by
         * SinudoisalShiftGrid.
         */
        tolerance = 0.01;
        verifyTransform(samplePoints[1], samplePoints[0]);
    }

    /**
     * Tests the derivatives at the sample points. This method compares the derivatives computed by
     * the transform with an estimation of derivatives computed by the finite differences method.
     *
     * @throws TransformException if an error occurred while transforming the coordinate.
     */
    @Test
    public void testDerivative() throws TransformException {
        final double[][] samplePoints = createSinusoidal(-40);
        final double[] point = new double[SinusoidalShiftGrid.DIMENSION];                   // A single point from 'samplePoints'
        derivativeDeltas = new double[] {0.002, 0.002};
        isInverseTransformSupported = false;                                                // For focusing on a single aspect.
        for (int i=0; i < samplePoints[0].length; i += SinusoidalShiftGrid.DIMENSION) {
            /*
             * The tolerance threshold must be relaxed for derivative at a position having factional digits
             * for the same reason as in `testForwardTransform()`. The matrix values are close to ±1.
             */
            System.arraycopy(samplePoints[0], i, point, 0, SinusoidalShiftGrid.DIMENSION);
            tolerance = (i < SinusoidalShiftGrid.FIRST_FRACTIONAL_COORDINATE) ? 1E-10 : 0.01;
            verifyDerivative(point);
            /*
             * Verify derivative at the same point but using inverse transform,
             * done in same loop for easier to comparisons during debugging.
             */
            if (isInverseTransformSupported) {
                transform = transform.inverse();
                System.arraycopy(samplePoints[1], i, point, 0, SinusoidalShiftGrid.DIMENSION);
                verifyDerivative(point);
                transform = transform.inverse();            // Back to forward transform.
            }
        }
    }

    /**
     * Tests the derivatives at sample points outside the grid. Those derivatives must be consistent
     * in order to allow inverse transformation to work when the initial point is outside the grid.
     *
     * @throws TransformException if an error occurred while transforming the coordinate.
     */
    @Test
    public void testExtrapolations() throws TransformException {
        createSinusoidal(-50);
        final double[] point = new double[SinusoidalShiftGrid.DIMENSION];
        derivativeDeltas = new double[] {0.002, 0.002};
        isInverseTransformSupported = false;                                                // For focusing on a single aspect.
        tolerance = 1E-10;
        for (int i=0; i<=4; i++) {
            switch (i) {
                default: throw new AssertionError(i);
                case 0: point[0] = -50; point[1] =  40; break;       // Point outside grid on the left.
                case 1: point[0] = 200; point[1] =  60; break;       // Point outside grid on the right.
                case 2: point[0] =  20; point[1] = -50; break;       // Point outside grid on the top.
                case 3: point[0] = -80; point[1] = 230; break;       // Point outside grid two sides.
                case 4: point[0] =  80; point[1] = 185;              // Point outside grid on the bottom.
                        tolerance = 0.3; break;
            }
            verifyDerivative(point);
        }
    }

    /**
     * Performs the tests using the same transformation as <q>France geocentric interpolation</q>
     * transform (approximately), but using shifts in geographic domain instead of in geocentric domain.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException if an error occurred while transforming a coordinate.
     *
     * @see InterpolatedGeocentricTransformTest#testInverseTransform()
     */
    @Test
    public void testRGF93() throws FactoryException, TransformException {
        createRGF93();

        // Forward transform
        isInverseTransformSupported = true;                                 // Set to 'false' for testing one direction at time.
        verifyTransform(FranceGeocentricInterpolationTest.samplePoint(1),
                        FranceGeocentricInterpolationTest.samplePoint(3));

        // Inverse transform
        transform = transform.inverse();
        verifyTransform(FranceGeocentricInterpolationTest.samplePoint(3),
                        FranceGeocentricInterpolationTest.samplePoint(1));

        // Forward derivative
        transform        = transform.inverse();
        derivativeDeltas = new double[] {0.2, 0.2};
        verifyDerivative(FranceGeocentricInterpolationTest.samplePoint(1));

        // Inverse derivative
        transform = transform.inverse();
        verifyDerivative(FranceGeocentricInterpolationTest.samplePoint(3));
    }

    /**
     * Performs the tests using the transformation from NAD27 to NAD93.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException if an error occurred while transforming a coordinate.
     */
    @Test
    public void testNADCON() throws FactoryException, TransformException {
        createNADCON();

        // Forward transform
        isInverseTransformSupported = true;                                 // Set to 'false' for testing one direction at time.
        verifyTransform(NADCONTest.samplePoint(1),
                        NADCONTest.samplePoint(3));

        // Inverse transform
        transform = transform.inverse();
        verifyTransform(NADCONTest.samplePoint(3),
                        NADCONTest.samplePoint(1));
    }

    /**
     * Tests the Well Known Text (version 1) formatting.
     * The result is what we show to users, but may be quite different than what SIS has in memory.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException should never happen.
     */
    @Test
    public void testWKT() throws FactoryException, TransformException {
        createRGF93();
        assertWktEqualsRegex("(?m)\\Q" +
                "PARAM_MT[“NTv2”,\n" +
                "  PARAMETER[“Latitude and longitude difference file”, “\\E.*\\W\\Q" +
                             NTv2Test.TEST_FILE + "”]]\\E");

        transform = transform.inverse();
        assertWktEqualsRegex("(?m)\\Q" +
                "INVERSE_MT[\n" +
                "  PARAM_MT[“NTv2”,\n" +
                "    PARAMETER[“Latitude and longitude difference file”, “\\E.*\\W\\Q" +
                             NTv2Test.TEST_FILE + "”]]]\\E");

        createNADCON();
        assertWktEqualsRegex("(?m)\\Q" +
                "PARAM_MT[“NADCON”,\n" +
                "  PARAMETER[“Latitude difference file”, “\\E.*\\W\\Q"  + NADCONTest.TEST_FILE + ".laa”],\n" +
                "  PARAMETER[“Longitude difference file”, “\\E.*\\W\\Q" + NADCONTest.TEST_FILE + ".loa”]]\\E");

    }
}
