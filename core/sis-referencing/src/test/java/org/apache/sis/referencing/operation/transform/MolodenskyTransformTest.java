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

import org.opengis.util.FactoryException;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.measure.Longitude;

import static java.lang.StrictMath.toRadians;

// Test dependencies
import org.apache.sis.internal.referencing.provider.GeocentricTranslationTest;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestUtilities;
import org.opengis.test.ToleranceModifier;
import org.opengis.test.ToleranceModifiers;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link MolodenskyTransform}.
 *
 * @author  Tara Athan
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    CoordinateDomainTest.class,
    ContextualParametersTest.class
})
public final strictfp class MolodenskyTransformTest extends MathTransformTestCase {
    /**
     * Creates a Molodensky transform for a datum shift from WGS84 to ED50.
     * Tolerance thresholds are also initialized.
     *
     * @throws FactoryException if an error occurred while creating a transform step.
     */
    private void create(final boolean abridged) throws FactoryException {
        final Ellipsoid source = CommonCRS.WGS84.ellipsoid();
        final Ellipsoid target = CommonCRS.ED50.ellipsoid();
        transform = MolodenskyTransform.createGeodeticTransformation(
                DefaultFactories.forBuildin(MathTransformFactory.class),
                source, true, target, true, 84.87, 96.49, 116.95, abridged);

        final double delta = toRadians(100.0 / 60) / 1852;          // Approximatively 100 metres
        derivativeDeltas = new double[] {delta, delta, 100};        // (Δλ, Δφ, Δh)
        tolerance  = GeocentricTranslationTest.precision(1);        // Half the precision of target sample point
        zTolerance = GeocentricTranslationTest.precision(3);        // Required precision for h
        zDimension = new int[] {2};                                 // Dimension of h where to apply zTolerance
        assertFalse(transform.isIdentity());
        validate();
    }

    /**
     * Tests the derivative of the Abridged Molodensky transformation.
     *
     * @throws FactoryException if an error occurred while creating a transform step.
     * @throws TransformException if the transformation failed.
     */
    @Test
    public void testAbridgedMolodenskyDerivative() throws FactoryException, TransformException {
        create(true);
        verifyDerivative( 0,  0,  0);
        verifyDerivative(-3, 30,  7);
        verifyDerivative(+6, 60, 20);
    }

    /**
     * Tests the derivative of the Molodensky transformation.
     *
     * @throws FactoryException if an error occurred while creating a transform step.
     * @throws TransformException if the transformation failed.
     */
    @Test
    @DependsOnMethod("testAbridgedMolodenskyDerivative")
    public void testMolodenskyDerivative() throws FactoryException, TransformException {
        create(false);
        verifyDerivative( 0,  0,  0);
        verifyDerivative(-3, 30,  7);
        verifyDerivative(+6, 60, 20);
    }

    /**
     * Tests using the sample point given by the EPSG guide.
     *
     * <ul>
     *   <li>Source point in WGS84: 53°48'33.820"N, 02°07'46.380"E, 73.00 metres.</li>
     *   <li>Target point in ED50:  53°48'36.565"N, 02'07"51.477"E, 28.02 metres.</li>
     *   <li>Datum shift: dX = +84.87m, dY = +96.49m, dZ = +116.95m.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating a transform step.
     * @throws TransformException if the transformation failed.
     */
    @Test
    @DependsOnMethod("testAbridgedMolodenskyDerivative")
    public void testAbridgedMolodensky() throws FactoryException, TransformException {
        create(true);
        final double[] sample   = GeocentricTranslationTest.samplePoint(1);
        final double[] expected = GeocentricTranslationTest.samplePoint(5);
        isInverseTransformSupported = false;
        verifyTransform(sample, expected);
        /*
         * When testing the inverse transformation, we need to relax slightly
         * the tolerance for the 'h' value.
         */
        zTolerance = Formulas.LINEAR_TOLERANCE;
        isInverseTransformSupported = true;
        verifyTransform(sample, expected);
    }

    /**
     * Tests using the same EPSG example than the one provided in {@link EllipsoidalToCartesianTransformTest}.
     *
     * <ul>
     *   <li>Source point in WGS84: 53°48'33.820"N, 02°07'46.380"E, 73.00 metres.</li>
     *   <li>Target point in ED50:  53°48'36.565"N, 02'07"51.477"E, 28.02 metres.</li>
     *   <li>Datum shift: dX = +84.87m, dY = +96.49m, dZ = +116.95m.</li>
     * </ul>
     *
     * @throws FactoryException if an error occurred while creating a transform step.
     * @throws TransformException if the transformation failed.
     */
    @Test
    @DependsOnMethod({"testAbridgedMolodensky", "testMolodenskyDerivative"})
    public void testMolodensky() throws FactoryException, TransformException {
        create(false);
        final double[] sample   = GeocentricTranslationTest.samplePoint(1);
        final double[] expected = GeocentricTranslationTest.samplePoint(4);
        isInverseTransformSupported = false;
        verifyTransform(sample, expected);
        /*
         * When testing the inverse transformation, we need to relax slightly
         * the tolerance for the 'h' value.
         */
        zTolerance = Formulas.LINEAR_TOLERANCE;
        isInverseTransformSupported = true;
        verifyTransform(sample, expected);
    }

    /**
     * Tests conversion of random points. The test is performed with the Molodensky transform,
     * not the abridged one, because the errors caused by the abridged Molondeky method is too
     * high for this test.
     *
     * @throws FactoryException if an error occurred while creating a transform step.
     * @throws TransformException if a conversion failed.
     */
    @Test
    @DependsOnMethod("testMolodensky")
    public void testRandomPoints() throws FactoryException, TransformException {
        create(false);
        tolerance  = Formulas.LINEAR_TOLERANCE * 3;     // To be converted in degrees by ToleranceModifier.GEOGRAPHIC
        zTolerance = Formulas.LINEAR_TOLERANCE * 2;
        toleranceModifier = ToleranceModifiers.concatenate(ToleranceModifier.GEOGRAPHIC, toleranceModifier);
        verifyInDomain(new double[] {Longitude.MIN_VALUE, -85, -500},
                       new double[] {Longitude.MIN_VALUE, +85, +500},
                       new int[] {8, 8, 8},
                       TestUtilities.createRandomNumberGenerator(208129394));
    }

    /**
     * Tests the standard Well Known Text (version 1) formatting.
     * The result is what we show to users, but may quite different than what SIS has in memory.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException should never happen.
     */
    @Test
    public void testWKT() throws FactoryException, TransformException {
        create(true);
        assertWktEquals("PARAM_MT[“Abridged_Molodenski”,\n" +
                        "  PARAMETER[“dim”, 3],\n" +
                        "  PARAMETER[“dx”, 84.87],\n" +
                        "  PARAMETER[“dy”, 96.49],\n" +
                        "  PARAMETER[“dz”, 116.95],\n" +
                        "  PARAMETER[“Semi-major axis length difference”, 251.0],\n" +
                        "  PARAMETER[“Flattening difference”, 1.4192702255886284E-5],\n" +
                        "  PARAMETER[“src_semi_major”, 6378137.0],\n" +
                        "  PARAMETER[“src_semi_minor”, 6356752.314245179],\n" +
                        "  PARAMETER[“tgt_semi_major”, 6378388.0],\n" +
                        "  PARAMETER[“tgt_semi_minor”, 6356911.9461279465]]");

        transform = transform.inverse();
        assertWktEquals("PARAM_MT[“Abridged_Molodenski”,\n" +
                        "  PARAMETER[“dim”, 3],\n" +
                        "  PARAMETER[“dx”, -84.87],\n" +
                        "  PARAMETER[“dy”, -96.49],\n" +
                        "  PARAMETER[“dz”, -116.95],\n" +
                        "  PARAMETER[“Semi-major axis length difference”, -251.0],\n" +
                        "  PARAMETER[“Flattening difference”, -1.4192702255886284E-5],\n" +
                        "  PARAMETER[“src_semi_major”, 6378388.0],\n" +
                        "  PARAMETER[“src_semi_minor”, 6356911.9461279465],\n" +
                        "  PARAMETER[“tgt_semi_major”, 6378137.0],\n" +
                        "  PARAMETER[“tgt_semi_minor”, 6356752.314245179]]");
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
        create(true);
        assertInternalWktEquals(
                "Concat_MT[Param_MT[“Affine”,\n" +
                "    Parameter[“num_row”, 4],\n" +
                "    Parameter[“num_col”, 4],\n" +
                "    Parameter[“elt_0_0”, 0.017453292519943295],\n" +       // Degrees to radians conversion
                "    Parameter[“elt_1_1”, 0.017453292519943295]],\n" +
                "  Param_MT[“Molodensky”,\n" +
                "    Parameter[“X-axis translation”, 84.87, Unit[“metre”, 1, Id[“EPSG”, 9001]]],\n" +
                "    Parameter[“Y-axis translation”, 96.49, Unit[“metre”, 1, Id[“EPSG”, 9001]]],\n" +
                "    Parameter[“Z-axis translation”, 116.95, Unit[“metre”, 1, Id[“EPSG”, 9001]]],\n" +
                "    Parameter[“Semi-major axis length difference”, 251.0, Unit[“metre”, 1, Id[“EPSG”, 9001]]],\n" +
                "    Parameter[“Flattening difference”, 1.4192702255886284E-5, Unit[“unity”, 1, Id[“EPSG”, 9201]]],\n" +
                "    Parameter[“src_semi_major”, 6378137.0, Unit[“metre”, 1, Id[“EPSG”, 9001]]],\n" +
                "    Parameter[“excentricity”, 0.0818191908426215],\n" +
                "    Parameter[“abridged”, TRUE],\n" +
                "    Parameter[“dim”, 3]],\n" +
                "  Param_MT[“Affine”,\n" +
                "    Parameter[“num_row”, 4],\n" +
                "    Parameter[“num_col”, 4],\n" +
                "    Parameter[“elt_0_0”, 57.29577951308232],\n" +          // Radians to degrees conversion
                "    Parameter[“elt_1_1”, 57.29577951308232]]]");
    }
}
