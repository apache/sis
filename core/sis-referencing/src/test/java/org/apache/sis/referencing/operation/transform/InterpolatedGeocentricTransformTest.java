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
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.referencing.provider.FranceGeocentricInterpolation;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import org.apache.sis.internal.referencing.provider.FranceGeocentricInterpolationTest;
import org.apache.sis.internal.referencing.provider.GeocentricTranslationTest;
import org.apache.sis.referencing.datum.HardCodedDatum;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.junit.Test;

import static org.opengis.test.Assert.*;


/**
 * Tests {@link InterpolatedGeocentricTransform}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    GeocentricTranslationTest.class,
    FranceGeocentricInterpolationTest.class
})
public strictfp class InterpolatedGeocentricTransformTest extends MathTransformTestCase {
    /**
     * Creates the <cite>"France geocentric interpolation"</cite> transform,
     * including the normalization and denormalization parts.
     *
     * @throws FactoryException if an error occurred while loading the grid.
     */
    void createGeodeticTransformation() throws FactoryException {
        createGeodeticTransformation(new FranceGeocentricInterpolation());
    }

    /**
     * Creates the transform using the given provider.
     */
    final void createGeodeticTransformation(final FranceGeocentricInterpolation provider) throws FactoryException {
        final URL file = FranceGeocentricInterpolationTest.class.getResource(FranceGeocentricInterpolationTest.TEST_FILE);
        assertNotNull("Test file \"" + FranceGeocentricInterpolationTest.TEST_FILE + "\" not found.", file);
        final Ellipsoid source = HardCodedDatum.NTF.getEllipsoid();     // Clarke 1880 (IGN)
        final Ellipsoid target = CommonCRS.ETRS89.ellipsoid();          // GRS 1980 ellipsoid
        final ParameterValueGroup values = provider.getParameters().createValue();
        values.parameter("src_semi_major").setValue(source.getSemiMajorAxis());
        values.parameter("src_semi_minor").setValue(source.getSemiMinorAxis());
        values.parameter("tgt_semi_major").setValue(target.getSemiMajorAxis());
        values.parameter("tgt_semi_minor").setValue(target.getSemiMinorAxis());
        values.parameter("Geocentric translation file").setValue(file);    // Automatic conversion from URL to Path.
        transform = provider.createMathTransform(DefaultFactories.forBuildin(MathTransformFactory.class), values);
        tolerance = FranceGeocentricInterpolationTest.ANGULAR_TOLERANCE;
    }

    /**
     * Tests transformation of sample point from RGF93 to NTF.
     * We call this transformation "forward" because it uses the grid values directly,
     * without doing first an approximation followed by an iteration.
     *
     * @throws FactoryException if an error occurred while loading the grid.
     * @throws TransformException if an error occurred while transforming the coordinate.
     */
    @Test
    public void testForwardTransform() throws FactoryException, TransformException {
        createGeodeticTransformation();   // Create the inverse of the transform we are interrested in.
        transform = transform.inverse();
        isInverseTransformSupported = false;
        verifyTransform(FranceGeocentricInterpolationTest.samplePoint(3),
                        FranceGeocentricInterpolationTest.samplePoint(1));
        validate();
        /*
         * Input:     2.424971108333333    48.84444583888889
         * Expected:  2.425671861111111    48.84451225
         * Actual:    2.425671863799633    48.844512255374376   (interpolated geocentric transform)
         * Actual:    2.4256718922236735   48.84451219111167    (interpolated Molodensky transform)
         */
    }

    /**
     * Tests transformation of sample point from NTF to RGF93.
     *
     * @throws FactoryException if an error occurred while loading the grid.
     * @throws TransformException if an error occurred while transforming the coordinate.
     */
    @Test
    @DependsOnMethod("testForwardTransform")
    public void testInverseTransform() throws FactoryException, TransformException {
        createGeodeticTransformation();
        isInverseTransformSupported = false;
        verifyTransform(FranceGeocentricInterpolationTest.samplePoint(1),
                        FranceGeocentricInterpolationTest.samplePoint(3));
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
    public void testForwardDerivative() throws FactoryException, TransformException {
        createGeodeticTransformation();
        transform = transform.inverse();
        final double delta = (100.0 / 60) / 1852;      // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        tolerance = 1E-5;   // Empirical value.
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
        createGeodeticTransformation();
        final double delta = (100.0 / 60) / 1852;      // Approximatively 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        tolerance = 1E-5;   // Empirical value.
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
        createGeodeticTransformation();
        transform = transform.inverse();
        assertWktEqualsRegex("(?m)\\Q" +
                "PARAM_MT[“Geocentric interpolation”,\n" +
                "  PARAMETER[“dim”, 2],\n" +
                "  PARAMETER[“src_semi_major”, 6378137.0],\n" +
                "  PARAMETER[“src_semi_minor”, 6356752.314140356],\n" +
                "  PARAMETER[“tgt_semi_major”, 6378249.2],\n" +
                "  PARAMETER[“tgt_semi_minor”, 6356515.0],\n" +
                "  PARAMETER[“Geocentric translation file”, “\\E.*\\W\\Q" +
                             FranceGeocentricInterpolationTest.TEST_FILE + "”]]\\E");

        transform = transform.inverse();
        assertWktEqualsRegex("(?m)\\Q" +
                "PARAM_MT[“Geocentric inverse interpolation”,\n" +
                "  PARAMETER[“dim”, 2],\n" +
                "  PARAMETER[“src_semi_major”, 6378249.2],\n" +
                "  PARAMETER[“src_semi_minor”, 6356515.0],\n" +
                "  PARAMETER[“tgt_semi_major”, 6378137.0],\n" +
                "  PARAMETER[“tgt_semi_minor”, 6356752.314140356],\n" +
                "  PARAMETER[“Geocentric translation file”, “\\E.*\\W\\Q" +
                             FranceGeocentricInterpolationTest.TEST_FILE + "”]]\\E");
    }

    /**
     * Tests the internal Well Known Text formatting.
     * This WKT shows what SIS has in memory for debugging purpose.
     * This is normally not what we show to users.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException should never happen.
     */
    @Test
    public void testInternalWKT() throws FactoryException, TransformException {
        createGeodeticTransformation();
        assertInternalWktEqualsRegex("(?m)\\Q" +
                "Concat_MT[\n" +
                "  Param_MT[“Affine parametric transformation”,\n" +
                "    Parameter[“A0”, 0.017453292519943295, Id[“EPSG”, 8623]],\n" +   // Degrees to radians conversion
                "    Parameter[“B1”, 0.017453292519943295, Id[“EPSG”, 8640]]],\n" +
                "  Param_MT[“Geocentric inverse interpolation”,\n" +
                "    Parameter[“dim”, 2],\n" +
                "    Parameter[“src_semi_major”, 6378249.2],\n" +
                "    Parameter[“src_semi_minor”, 6356515.0],\n" +
                "    Parameter[“tgt_semi_major”, 6378137.0],\n" +
                "    Parameter[“tgt_semi_minor”, 6356752.314140356],\n" +
                "    ParameterFile[“Geocentric translation file”, “\\E.*\\W\\Q" +
                                   FranceGeocentricInterpolationTest.TEST_FILE + "”, Id[“EPSG”, 8727],\n" +
                "      Remark[“\\E.*\\Q”]]],\n" +
                "  Param_MT[“Affine parametric transformation”,\n" +
                "    Parameter[“A0”, 57.29577951308232, Id[“EPSG”, 8623]],\n" +      // Radians to degrees conversion
                "    Parameter[“B1”, 57.29577951308232, Id[“EPSG”, 8640]]]]\\E");
    }
}
