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

import static org.junit.Assert.assertNotNull;


/**
 * Tests {@link InterpolatedTransform}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    NTv2Test.class,
    NADCONTest.class
})
public final strictfp class InterpolatedTransformTest extends MathTransformTestCase {
    /**
     * Creates the same transformation than <cite>"France geocentric interpolation"</cite> transform
     * (approximatively), but using shifts in geographic domain instead than in geocentric domain.
     *
     * @throws FactoryException if an error occurred while loading the grid.
     */
    private void createRGF93() throws FactoryException {
        final URL file = NTv2Test.class.getResource(NTv2Test.TEST_FILE);
        assertNotNull("Test file \"" + NTv2Test.TEST_FILE + "\" not found.", file);
        final NTv2 provider = new NTv2();
        final ParameterValueGroup values = provider.getParameters().createValue();
        values.parameter("Latitude and longitude difference file").setValue(file);    // Automatic conversion from URL to Path.
        transform = provider.createMathTransform(DefaultFactories.forBuildin(MathTransformFactory.class), values);
        tolerance = Formulas.ANGULAR_TOLERANCE;
        validate();
    }

    /**
     * Creates a transformation from NAD27 to NAD93
     *
     * @throws FactoryException if an error occurred while loading the grid.
     */
    private void createNADCON() throws FactoryException {
        final URL latitudeShifts  = NADCONTest.class.getResource(NADCONTest.TEST_FILE + ".laa");
        final URL longitudeShifts = NADCONTest.class.getResource(NADCONTest.TEST_FILE + ".loa");
        assertNotNull("Test file \"" + NADCONTest.TEST_FILE + ".laa\" not found.", latitudeShifts);
        assertNotNull("Test file \"" + NADCONTest.TEST_FILE + ".loa\" not found.", longitudeShifts);
        final NADCON provider = new NADCON();
        final ParameterValueGroup values = provider.getParameters().createValue();
        values.parameter("Latitude difference file").setValue(latitudeShifts);
        values.parameter("Longitude difference file").setValue(longitudeShifts);
        transform = provider.createMathTransform(DefaultFactories.forBuildin(MathTransformFactory.class), values);
        tolerance = NADCONTest.ANGULAR_TOLERANCE;
        validate();
    }

    /**
     * Tests forward transformation of sample points. Tested transformations are:
     * <ul>
     *   <li>From NTF to RGF93 using a NTv2 grid.</li>
     *   <li>From NAD27 to NAD83 using a NADCON grid.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while loading a grid.
     * @throws TransformException if an error occurred while transforming a coordinate.
     *
     * @see InterpolatedGeocentricTransformTest#testInverseTransform()
     */
    @Test
    public void testForwardTransform() throws FactoryException, TransformException {
        isInverseTransformSupported = false;
        createRGF93();
        verifyTransform(FranceGeocentricInterpolationTest.samplePoint(1),
                        FranceGeocentricInterpolationTest.samplePoint(3));
        createNADCON();
        verifyTransform(NADCONTest.samplePoint(1),
                        NADCONTest.samplePoint(3));
    }

    /**
     * Tests inverse transformation of sample points. Tested transformations are:
     * <ul>
     *   <li>From RGF93 to NTF using a NTv2 grid.</li>
     *   <li>From NAD83 to NAD27 using a NADCON grid.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while loading a grid.
     * @throws TransformException if an error occurred while transforming a coordinate.
     */
    @Test
    @DependsOnMethod("testForwardTransform")
    public void testInverseTransform() throws FactoryException, TransformException {
        isInverseTransformSupported = false;
        createRGF93();
        transform = transform.inverse();
        verifyTransform(FranceGeocentricInterpolationTest.samplePoint(3),
                        FranceGeocentricInterpolationTest.samplePoint(1));
        createNADCON();
        transform = transform.inverse();
        verifyTransform(NADCONTest.samplePoint(3),
                        NADCONTest.samplePoint(1));
    }

    /**
     * Tests the derivatives at the sample point. This method compares the derivatives computed by
     * the transform with an estimation of derivatives computed by the finite differences method.
     *
     * @throws FactoryException if an error occurred while loading the grid.
     * @throws TransformException if an error occurred while transforming the coordinate.
     */
    @Test
    @DependsOnMethod("testForwardTransform")
    public void testForwardDerivative() throws FactoryException, TransformException {
        createRGF93();
        final double delta = 0.2;
        derivativeDeltas = new double[] {delta, delta};
        tolerance = 5E-6;   // Empirical value.
        verifyDerivative(FranceGeocentricInterpolationTest.samplePoint(1));
    }

    /**
     * Tests the derivatives at the sample point. This method compares the derivatives computed by
     * the transform with an estimation of derivatives computed by the finite differences method.
     *
     * @throws FactoryException if an error occurred while loading the grid.
     * @throws TransformException if an error occurred while transforming the coordinate.
     */
    @Test
    @DependsOnMethod("testInverseTransform")
    public void testInverseDerivative() throws FactoryException, TransformException {
        createRGF93();
        transform = transform.inverse();
        final double delta = 0.2;
        derivativeDeltas = new double[] {delta, delta};
        tolerance = 5E-6;   // Empirical value.
        verifyDerivative(FranceGeocentricInterpolationTest.samplePoint(3));
    }

    /**
     * Tests the Well Known Text (version 1) formatting.
     * The result is what we show to users, but may quite different than what SIS has in memory.
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
