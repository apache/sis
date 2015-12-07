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
import org.apache.sis.internal.referencing.provider.NTv2;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.system.DefaultFactories;

// Test dependencies
import org.apache.sis.internal.referencing.provider.FranceGeocentricInterpolationTest;
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
    NTv2Test.class
})
public final strictfp class InterpolatedTransformTest extends MathTransformTestCase {
    /**
     * Creates the same transformation than <cite>"France geocentric interpolation"</cite> transform
     * (approximatively), but using shifts in geographic domain instead than in geocentric domain.
     *
     * @throws FactoryException if an error occurred while loading the grid.
     */
    private void createGeodeticTransformation() throws FactoryException {
        final URL file = NTv2Test.class.getResource(NTv2Test.TEST_FILE);
        assertNotNull("Test file \"" + NTv2Test.TEST_FILE + "\" not found.", file);
        final NTv2 provider = new NTv2();
        final ParameterValueGroup values = provider.getParameters().createValue();
        values.parameter("Latitude and longitude difference file").setValue(file);    // Automatic conversion from URL to Path.
        transform = provider.createMathTransform(DefaultFactories.forBuildin(MathTransformFactory.class), values);
        tolerance = Formulas.ANGULAR_TOLERANCE;
    }

    /**
     * Tests transformation of sample point from NTF to RGF93.
     *
     * @throws FactoryException if an error occurred while loading the grid.
     * @throws TransformException if an error occurred while transforming the coordinate.
     *
     * @see InterpolatedGeocentricTransformTest#testInverseTransform()
     */
    @Test
    public void testForwardTransform() throws FactoryException, TransformException {
        createGeodeticTransformation();
        isInverseTransformSupported = false;
        verifyTransform(FranceGeocentricInterpolationTest.samplePoint(1),
                        FranceGeocentricInterpolationTest.samplePoint(3));
        validate();
    }

    /**
     * Tests transformation of sample point from RGF93 to NTF.
     *
     * @throws FactoryException if an error occurred while loading the grid.
     * @throws TransformException if an error occurred while transforming the coordinate.
     *
     * @see InterpolatedGeocentricTransformTest#testForwardTransform()
     */
    @Test
    @DependsOnMethod("testForwardTransform")
    public void testInverseTransform() throws FactoryException, TransformException {
        createGeodeticTransformation();
        transform = transform.inverse();
        isInverseTransformSupported = false;
        verifyTransform(FranceGeocentricInterpolationTest.samplePoint(3),
                        FranceGeocentricInterpolationTest.samplePoint(1));
        validate();
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
    public void testDerivative() throws FactoryException, TransformException {
        createGeodeticTransformation();
        final double delta = 0.2;
        derivativeDeltas = new double[] {delta, delta};
        tolerance = 5E-6;   // Empirical value.
        verifyDerivative(FranceGeocentricInterpolationTest.samplePoint(1));
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
        createGeodeticTransformation();
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
    }
}
