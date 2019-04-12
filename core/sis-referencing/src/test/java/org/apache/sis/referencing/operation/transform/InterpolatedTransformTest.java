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
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.provider.NADCON;
import org.apache.sis.internal.referencing.provider.NTv2;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.system.DefaultFactories;

// Test dependencies
import org.apache.sis.internal.referencing.provider.FranceGeocentricInterpolationTest;
import org.apache.sis.internal.referencing.provider.NADCONTest;
import org.apache.sis.internal.referencing.provider.NTv2Test;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.junit.Test;


/**
 * Tests {@link InterpolatedTransform}.
 * Tested transformations are:
 *
 * <ul>
 *   <li>Simple case based on linear calculations (easier to debug).</li>
 *   <li>From NTF to RGF93 using a NTv2 grid.</li>
 *   <li>From NAD27 to NAD83 using a NADCON grid.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.7
 * @module
 */
@DependsOn({
    NTv2Test.class,
    NADCONTest.class
})
public final strictfp class InterpolatedTransformTest extends MathTransformTestCase {
    /**
     * Creates an {@link InterpolatedTransform} derived from a quadratic formula.
     * We do not really need {@code InterpolatedTransform} for quadratic formulas,
     * but we use them for testing purpose since they are easier to debug.
     *
     * @param  rotation  rotation angle, in degrees. Use 0 for debugging a simple case.
     * @return suggested points to use for testing purposes as an array of length 2,
     *         with source coordinates in the first array and target coordinates in the second array.
     */
    private double[][] createQuadratic(final double rotation) throws TransformException {
        final QuadraticShiftGrid grid = new QuadraticShiftGrid(rotation);
        transform = new InterpolatedTransform(grid);
        return grid.samplePoints();
    }

    /**
     * Creates the same transformation than <cite>"France geocentric interpolation"</cite> transform
     * (approximately), but using shifts in geographic domain instead than in geocentric domain.
     *
     * @throws FactoryException if an error occurred while loading the grid.
     */
    private void createRGF93() throws FactoryException {
        final URL file = NTv2Test.getResourceAsConvertibleURL(NTv2Test.TEST_FILE);
        final NTv2 provider = new NTv2();
        final ParameterValueGroup values = provider.getParameters().createValue();
        values.parameter("Latitude and longitude difference file").setValue(file);    // Automatic conversion from URL to Path.
        transform = provider.createMathTransform(DefaultFactories.forBuildin(MathTransformFactory.class), values);
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
        transform = provider.createMathTransform(DefaultFactories.forBuildin(MathTransformFactory.class), values);
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
        final double[][] samplePoints = createQuadratic(-15);
        tolerance = 1E-10;
        verifyTransform(Arrays.copyOf(samplePoints[0], QuadraticShiftGrid.FIRST_FRACTIONAL_COORDINATE),
                        Arrays.copyOf(samplePoints[1], QuadraticShiftGrid.FIRST_FRACTIONAL_COORDINATE));

        tolerance = 0.003;                                          // Because of interpolations in fractional coordinates.
        verifyTransform(samplePoints[0], samplePoints[1]);
    }

    /**
     * Tests inverse transformation of sample points.
     * Inverse transform requires derivative.
     *
     * @throws TransformException if an error occurred while transforming a coordinate.
     */
    @Test
    @org.junit.Ignore("Debugging still in progress")
    @DependsOnMethod({"testForwardTransform", "testDerivative"})
    public void testInverseTransform() throws TransformException {
        isInverseTransformSupported = false;                                            // For focusing on a single aspect.
        final double[][] samplePoints = createQuadratic(-20);
        transform = transform.inverse();
        tolerance = QuadraticShiftGrid.PRECISION;
        verifyTransform(samplePoints[1], samplePoints[0]);
    }

    /**
     * Tests the derivatives at the sample point. This method compares the derivatives computed by
     * the transform with an estimation of derivatives computed by the finite differences method.
     *
     * @throws TransformException if an error occurred while transforming the coordinate.
     */
    @Test
    @DependsOnMethod("testForwardTransform")
    public void testDerivative() throws TransformException {
        final double[][] samplePoints = createQuadratic(-40);
        final double[] point = new double[QuadraticShiftGrid.DIMENSION];                    // A single point from 'samplePoints'
        derivativeDeltas = new double[] {0.002, 0.002};
        isInverseTransformSupported = false;                                                // For focusing on a single aspect.
        for (int i=0; i < samplePoints[0].length; i += QuadraticShiftGrid.DIMENSION) {
            System.arraycopy(samplePoints[0], i, point, 0, QuadraticShiftGrid.DIMENSION);
            if (i < QuadraticShiftGrid.FIRST_FRACTIONAL_COORDINATE) {
                tolerance = 1E-10;                                                          // Empirical value.
            } else {
                tolerance = 0.003;                       // Because current implementation does not yet interpolate derivatives.
            }
            verifyDerivative(point);
            /*
             * Verify derivative at the same point but using inverse transform,
             * done in same loop for easier to comparisons during debugging.
             */
            if (isInverseTransformSupported) {
                transform = transform.inverse();
                System.arraycopy(samplePoints[1], i, point, 0, QuadraticShiftGrid.DIMENSION);
                verifyDerivative(point);
                transform = transform.inverse();            // Back to forward transform.
            }
        }
    }

    /**
     * Performs the tests using the same transformation than <cite>"France geocentric interpolation"</cite>
     * transform (approximately), but using shifts in geographic domain instead than in geocentric domain.
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
        tolerance        = 5E-6;                        // Empirical value.
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
