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

import java.util.Arrays;
import static java.lang.StrictMath.*;
import org.opengis.util.FactoryException;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.provider.FranceGeocentricInterpolation;
import org.apache.sis.referencing.operation.provider.Geographic2Dto3D;
import org.apache.sis.referencing.operation.provider.Geographic3Dto2D;
import org.apache.sis.referencing.operation.provider.Molodensky;
import org.apache.sis.referencing.internal.shared.Formulas;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.referencing.datum.GeodeticDatumMock;
import org.apache.sis.referencing.datum.HardCodedDatum;
import org.apache.sis.referencing.operation.provider.FranceGeocentricInterpolationTest;
import org.apache.sis.referencing.operation.provider.GeocentricTranslationTest;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import java.io.IOException;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.referencing.operation.provider.AbridgedMolodensky;
import org.apache.sis.math.StatisticsFormat;
import org.apache.sis.math.Statistics;
import static org.apache.sis.metadata.internal.shared.ReferencingServices.NAUTICAL_MILE;
import org.opengis.test.CalculationType;
import org.opengis.test.ToleranceModifier;
import org.opengis.test.ToleranceModifiers;
import org.opengis.test.referencing.ParameterizedTransformTest;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link MolodenskyTransform}. The {@link #compareWithGeocentricTranslation()}
 * method uses {@link EllipsoidToCentricTransform} as a reference implementation.
 * The errors compared to geocentric translations should not be greater than
 * approximately 1 centimetre.
 *
 * @author  Tara Athan
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Rémi Maréchal (Geomatys)
 */
public final class MolodenskyTransformTest extends MathTransformTestCase {
    /**
     * Creates a new test case.
     */
    public MolodenskyTransformTest() {
        final double delta = toRadians(100.0 / 60) / 1852;      // Approximately 100 metres
        derivativeDeltas   = new double[] {delta, delta, 100};  // (Δλ, Δφ, Δh)
        λDimension         = new int[] {0};                     // Dimension for which to ignore ±360° differences.
        zDimension         = new int[] {2};                     // Dimension of h where to apply zTolerance
        zTolerance         = Formulas.LINEAR_TOLERANCE;         // Tolerance for ellipsoidal heights (h)
        tolerance          = Formulas.ANGULAR_TOLERANCE;        // Tolerance for longitude and latitude in degrees
    }

    /**
     * Compares the Molodensky (non-abridged) transform with a geocentric translation.
     * Molodensky is an approximation of geocentric translation, so we test here how good this approximation is.
     * If {@link TestCase#VERBOSE} is {@code true}, then this method will print error statistics.
     *
     * @throws FactoryException if an error occurred while creating a transform step.
     * @throws TransformException if a transformation failed.
     * @throws IOException should never happen.
     *
     * @see #compareWithGeocentricTranslation()
     */
    @SuppressWarnings("fallthrough")
    private void compareWithGeocentricTranslation(
            final Ellipsoid source, final Ellipsoid target,
            final double tX,   final double tY,   final double tZ,
            final double xmin, final double ymin, final double zmin,
            final double xmax, final double ymax, final double zmax)
            throws FactoryException, TransformException, IOException
    {
        final MathTransform reference;
        final MathTransformFactory factory = DefaultMathTransformFactory.provider();
        transform = MolodenskyTransform.createGeodeticTransformation(factory, source, true, target, true, tX, tY, tZ, false);
        reference = GeocentricTranslationTest.createDatumShiftForGeographic3D(factory, source, target, tX, tY, tZ);
        final float[] srcPts = verifyInDomain(
                new double[] {xmin, ymin, zmin},
                new double[] {xmax, ymax, zmax},
                new int[]    {  10,   10,   10},
                TestUtilities.createRandomNumberGenerator(103627524044558476L));
        /*
         * Transform the same input coordinates using Molodensky transform (actual) and using the reference
         * implementation (expected). If we were asked to print statistics, compute them before to test the
         * values since the statistics may be a useful information in case of problem.
         */
        final double[] actual   = new double[srcPts.length];
        final double[] expected = new double[srcPts.length];
        transform.transform(srcPts, 0, actual,   0, srcPts.length / 3);
        reference.transform(srcPts, 0, expected, 0, srcPts.length / 3);
        if (TestCase.VERBOSE) {
            final Statistics[] stats = {
                new Statistics("|Δλ| (~cm)"),
                new Statistics("|Δφ| (~cm)"),
                new Statistics("|Δh| (cm)")
            };
            for (int i=0; i<srcPts.length; i++) {
                double Δ = actual[i] - expected[i];
                final int j = i % stats.length;
                switch (j) {
                    case 0: Δ *= cos(toRadians(expected[i+1]));     // Fall through
                    case 1: Δ *= 60 * NAUTICAL_MILE; break;         // Approximate conversion to metres
                }
                Δ *= 100;   // Conversion to centimetres.
                stats[j].accept(abs(Δ));
            }
            StatisticsFormat.getInstance().format(stats, TestCase.out);
        }
        assertCoordinatesEqual(3, expected, 0,
                actual, 0, expected.length / 3,
                CalculationType.DIRECT_TRANSFORM,
                "Comparison of Molodensky and geocentric translation");
    }

    /**
     * Creates a three-dimensional Molodensky transform for a datum shift from WGS84 to ED50.
     * Tolerance thresholds are also initialized.
     *
     * @throws FactoryException if an error occurred while creating a transform step.
     */
    private void create(final boolean abridged) throws FactoryException {
        final Ellipsoid source = HardCodedDatum.WGS84.getEllipsoid();
        final Ellipsoid target = CommonCRS.ED50.ellipsoid();
        transform = MolodenskyTransform.createGeodeticTransformation(
                DefaultMathTransformFactory.provider(),
                source, true, target, true,
                GeocentricTranslationTest.TX,
                GeocentricTranslationTest.TY,
                GeocentricTranslationTest.TZ,
                abridged);

        tolerance  = GeocentricTranslationTest.precision(1);        // Half the precision of target sample point
        zTolerance = GeocentricTranslationTest.precision(3);        // Required precision for h
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
     * Tests using the same EPSG example as the one provided in {@link EllipsoidToCentricTransformTest}.
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
     * Tests the point used in {@link FranceGeocentricInterpolationTest}. We use this test for measuring the
     * errors induced by the use of the Molodensky approximation instead of a real geocentric translation.
     * The error is approximately 1 centimetre, which is about 6 times more than the accuracy of the point
     * given in {@code FranceGeocentricInterpolationTest}.
     *
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws TransformException if transformation of a point failed.
     *
     * @see GeocentricTranslationTest#testFranceGeocentricInterpolationPoint()
     */
    @Test
    public void testFranceGeocentricInterpolationPoint() throws FactoryException, TransformException {
        transform = MolodenskyTransform.createGeodeticTransformation(
                DefaultMathTransformFactory.provider(),
                HardCodedDatum.NTF.getEllipsoid(), true,        // Clarke 1880 (IGN)
                CommonCRS.ETRS89.ellipsoid(), true,             // GRS 1980 ellipsoid
               -FranceGeocentricInterpolation.TX,
               -FranceGeocentricInterpolation.TY,
               -FranceGeocentricInterpolation.TZ,
                false);
        /*
         * Code below is a copy-and-paste of GeocentricTranslationTest.testFranceGeocentricInterpolationPoint(),
         * but with the tolerance threshold increased. We do not let the error goes beyond 1 cm however.
         */
        tolerance = min(Formulas.ANGULAR_TOLERANCE, FranceGeocentricInterpolationTest.ANGULAR_TOLERANCE * 6);
        final double[] source   = Arrays.copyOf(FranceGeocentricInterpolationTest.samplePoint(1), 3);
        final double[] expected = Arrays.copyOf(FranceGeocentricInterpolationTest.samplePoint(2), 3);
        expected[2] = 43.15;  // Anti-regression (this value is not provided in NTG_88 guidance note).
        verifyTransform(source, expected);
        validate();
    }

    /**
     * Compares the Molodensky (non-abridged) transforms with geocentric translations.
     * Molodensky is an approximation of geocentric translation, so we test here how good this
     * approximation is. This test performs the comparison for the following transformations:
     *
     * <ul>
     *   <li>Transformation from NTF to RGF93. Those CRS are the source and target of <q>France geocentric
     *       interpolation</q> (ESPG:9655). This test allows us to verify the accuracy documented in
     *       {@link InterpolatedGeocentricTransform}.</li>
     *   <li>(More areas may be added later).</li>
     * </ul>
     *
     * If {@link TestCase#VERBOSE} is {@code true}, then this method will print error statistics.
     *
     * @throws FactoryException if an error occurred while creating a transform step.
     * @throws TransformException if a transformation failed.
     * @throws IOException should never happen.
     *
     * @see #testFranceGeocentricInterpolationPoint()
     */
    @Test
    public void compareWithGeocentricTranslation() throws FactoryException, TransformException, IOException {
        /*
         * Disable the test for inverse transformations because they are not the purpose of this test.
         * Errors of inverse transformations are added to the error of forward transformations, which
         * would force us to double the tolerance threshold.
         */
        isInverseTransformSupported = false;
        tolerance         = 3*Formulas.LINEAR_TOLERANCE; // To be converted in degrees by ToleranceModifier.GEOGRAPHIC
        zTolerance        = 4*Formulas.LINEAR_TOLERANCE;
        toleranceModifier = ToleranceModifiers.concatenate(ToleranceModifier.GEOGRAPHIC, toleranceModifier);
        compareWithGeocentricTranslation(HardCodedDatum.NTF.getEllipsoid(),   // Clarke 1880 (IGN)
                                         CommonCRS.ETRS89.ellipsoid(),        // GRS 1980 ellipsoid
                                         FranceGeocentricInterpolation.TX,
                                         FranceGeocentricInterpolation.TY,
                                         FranceGeocentricInterpolation.TZ,
                                         -5.5, 41.0, -200,   // Geographic area of GR2DF97A datum shift grid.
                                         10.0, 52.0, +200);
    }

    /**
     * Tests conversion of random points. The test is performed with the Molodensky transform,
     * not the abridged one, because the errors caused by the abridged Molodensky method are
     * too high for this test.
     *
     * @throws FactoryException if an error occurred while creating a transform step.
     * @throws TransformException if a transformation failed.
     */
    @Test
    public void testRandomPoints() throws FactoryException, TransformException {
        create(false);
        tolerance  = Formulas.LINEAR_TOLERANCE * 3;     // To be converted in degrees by ToleranceModifier.GEOGRAPHIC
        zTolerance = Formulas.LINEAR_TOLERANCE * 2;
        toleranceModifier = ToleranceModifiers.concatenate(ToleranceModifier.GEOGRAPHIC, toleranceModifier);
        verifyInDomain(new double[] {-179, -85, -500},
                       new double[] {+179, +85, +500},
                       new int[]    {   8,   8,    8},
                       TestUtilities.createRandomNumberGenerator(208129394));
    }

    /**
     * Tests the creation through the provider.
     *
     * @throws FactoryException if an error occurred while creating a transform step.
     * @throws TransformException if a transformation failed.
     */
    @Test
    public void testProvider() throws FactoryException, TransformException {
        final MathTransformFactory factory = new MathTransformFactoryMock(new Molodensky());
        final var builder = factory.builder("Molodenski");
        final ParameterValueGroup parameters = builder.parameters();
        parameters.parameter("dim").setValue(3);
        parameters.parameter("dx").setValue(-3.0);
        parameters.parameter("dy").setValue(142.0);
        parameters.parameter("dz").setValue(183.0);
        parameters.parameter("src_semi_major").setValue(6378206.4);
        parameters.parameter("src_semi_minor").setValue(6356583.8);
        parameters.parameter("tgt_semi_major").setValue(6378137.0);
        parameters.parameter("tgt_semi_minor").setValue(6356752.31414036);
        transform = builder.create();
        assertEquals(3, transform.getSourceDimensions());
        assertEquals(3, transform.getTargetDimensions());
        tolerance  = Formulas.ANGULAR_TOLERANCE * 5;
        zTolerance = Formulas.LINEAR_TOLERANCE  * 5;
        verifyInDomain(CoordinateDomain.RANGE_10, ORDINATE_COUNT);
    }

    /**
     * Runs the test defined in the GeoAPI-conformance module.
     *
     * @throws FactoryException if an error occurred while creating a transform step.
     * @throws TransformException if a transformation failed.
     */
    @Test
    public void runGeoapiTest() throws FactoryException, TransformException {
        new ParameterizedTransformTest(new MathTransformFactoryMock(new AbridgedMolodensky())).testAbridgedMolodensky();
    }

    /**
     * Verifies that creating a Molodensky operation with same source and target ellipsoid and zero translation
     * results in an identity affine transform.
     *
     * @throws FactoryException if an error occurred while creating a transform step.
     */
    @Test
    public void testIdentity() throws FactoryException {
        final Ellipsoid source = HardCodedDatum.WGS84.getEllipsoid();
        transform = MolodenskyTransform.createGeodeticTransformation(
                DefaultMathTransformFactory.provider(), source, false, source, false, 0, 0, 0, false);
        assertInstanceOf(LinearTransform.class, transform);
        assertTrue(transform.isIdentity());
        validate();
    }

    /**
     * Tests the concatenation of a 3-dimensional transform with a "3D to 2D" transform.
     *
     * @throws FactoryException if an error occurred while creating a transform step.
     * @throws TransformException if an error occurred while requesting the inverse transform.
     */
    @Test
    public void testRedimension3Dto2D() throws FactoryException, TransformException {
        final MathTransformFactory factory = DefaultMathTransformFactory.provider();
        transform = new MolodenskyTransform(
                HardCodedDatum.WGS84.getEllipsoid(), true,
                GeodeticDatumMock.ED50.getEllipsoid(), true,
                GeocentricTranslationTest.TX,
                GeocentricTranslationTest.TY,
                GeocentricTranslationTest.TZ,
                false);
        validate();
        MolodenskyTransform forward, inverse;

        // Verify initial conditions.
        forward = assertInstanceOf(MolodenskyTransform.class, transform);
        inverse = assertInstanceOf(MolodenskyTransform.class, transform.inverse());
        assertSame(forward, inverse.inverse());
        assertEquals(3, forward.getSourceDimensions());
        assertEquals(3, forward.getTargetDimensions());

        // Drop target dimension.
        transform = MathTransforms.concatenate(transform, factory.builder(Geographic3Dto2D.NAME).create());
        forward = assertInstanceOf(MolodenskyTransform.class, transform);
        inverse = assertInstanceOf(MolodenskyTransform.class, transform.inverse());
        assertSame(forward, inverse.inverse());
        assertEquals(3, forward.getSourceDimensions());
        assertEquals(2, forward.getTargetDimensions());

        // Drop source dimension.
        transform = MathTransforms.concatenate(factory.builder(Geographic2Dto3D.NAME).create(), transform);
        forward = assertInstanceOf(MolodenskyTransform.class, transform);
        inverse = assertInstanceOf(MolodenskyTransform.class, transform.inverse());
        assertSame(forward, inverse.inverse());
        assertEquals(2, forward.getSourceDimensions());
        assertEquals(2, forward.getTargetDimensions());
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
                        "  PARAMETER[“src_semi_major”, 6378137.0],\n" +
                        "  PARAMETER[“src_semi_minor”, 6356752.314245179],\n" +
                        "  PARAMETER[“tgt_semi_major”, 6378388.0],\n" +
                        "  PARAMETER[“tgt_semi_minor”, 6356911.9461279465],\n" +
                        "  PARAMETER[“dx”, 84.87],\n" +
                        "  PARAMETER[“dy”, 96.49],\n" +
                        "  PARAMETER[“dz”, 116.95],\n" +
                        "  PARAMETER[“Semi-major axis length difference”, 251.0],\n" +
                        "  PARAMETER[“Flattening difference”, 1.4192702255886284E-5]]");

        transform = transform.inverse();
        assertWktEquals("PARAM_MT[“Abridged_Molodenski”,\n" +
                        "  PARAMETER[“dim”, 3],\n" +
                        "  PARAMETER[“src_semi_major”, 6378388.0],\n" +
                        "  PARAMETER[“src_semi_minor”, 6356911.9461279465],\n" +
                        "  PARAMETER[“tgt_semi_major”, 6378137.0],\n" +
                        "  PARAMETER[“tgt_semi_minor”, 6356752.314245179],\n" +
                        "  PARAMETER[“dx”, -84.87],\n" +
                        "  PARAMETER[“dy”, -96.49],\n" +
                        "  PARAMETER[“dz”, -116.95],\n" +
                        "  PARAMETER[“Semi-major axis length difference”, -251.0],\n" +
                        "  PARAMETER[“Flattening difference”, -1.4192702255886284E-5]]");
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
                "Concat_MT[\n" +
                "  Param_MT[“Affine”,\n" +
                "    Parameter[“num_row”, 4],\n" +
                "    Parameter[“num_col”, 4],\n" +
                "    Parameter[“elt_0_0”, 0.017453292519943295],\n" +       // Degrees to radians conversion
                "    Parameter[“elt_1_1”, 0.017453292519943295]],\n" +
                "  Param_MT[“Molodensky (radians domain)”,\n" +
                "    Parameter[“src_semi_major”, 6378137.0],\n" +
                "    Parameter[“src_semi_minor”, 6356752.314245179],\n" +
                "    Parameter[“Semi-major axis length difference”, 251.0, Id[“EPSG”, 8654]],\n" +
                "    Parameter[“Flattening difference”, 1.4192702255886284E-5, Id[“EPSG”, 8655]],\n" +
                "    Parameter[“X-axis translation”, 84.87, Id[“EPSG”, 8605]],\n" +
                "    Parameter[“Y-axis translation”, 96.49, Id[“EPSG”, 8606]],\n" +
                "    Parameter[“Z-axis translation”, 116.95, Id[“EPSG”, 8607]],\n" +
                "    Parameter[“abridged”, TRUE],\n" +
                "    Parameter[“dim”, 3]],\n" +
                "  Param_MT[“Affine”,\n" +
                "    Parameter[“num_row”, 4],\n" +
                "    Parameter[“num_col”, 4],\n" +
                "    Parameter[“elt_0_0”, 57.29577951308232],\n" +          // Radians to degrees conversion
                "    Parameter[“elt_1_1”, 57.29577951308232]]]");
    }
}
