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
package org.apache.sis.internal.referencing.provider;

import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.transform.CoordinateDomain;
import org.apache.sis.referencing.operation.transform.EllipsoidToCentricTransform;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransformTestCase;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static java.lang.StrictMath.toRadians;
import static org.opengis.test.Assert.*;


/**
 * Tests {@link GeocentricTranslation} and {@link GeocentricTranslation3D}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    AffineTest.class,
    org.apache.sis.referencing.operation.transform.ProjectiveTransformTest.class,
    org.apache.sis.referencing.operation.transform.ConcatenatedTransformTest.class,
    org.apache.sis.referencing.operation.transform.EllipsoidToCentricTransformTest.class,
    org.apache.sis.referencing.datum.BursaWolfParametersTest.class
})
public final strictfp class GeocentricTranslationTest extends MathTransformTestCase {
    /**
     * Geocentric translation parameters for transforming a point in the North Sea from WGS84 to ED50.
     * They are the parameters to use for transformation of the point given by {@link #samplePoint(int)}.
     */
    public static final double TX = 84.87, TY = 96.49, TZ = 116.95;

    /**
     * Returns the sample point for a step in the example given by the EPSG guidance note.
     *
     * <blockquote><b>Source:</b>
     * §2.4.3.5 <cite>Three-parameter geocentric translations</cite> and
     * §2.4.4.2 <cite>Abridged Molodensky transformation</cite> in
     * IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2 – April 2015
     * </blockquote>
     *
     * The example transforms a point in the North Sea from WGS84 to ED50.
     * The transform can be decomposed in 4 steps:
     * <pre>
     *   <b>Step 1:</b> Source point in WGS84:  53°48'33.820"N,  02°07'46.380"E,  73.00 metres.
     *   <b>Step 2:</b> Geocentric (X,Y,Z):      3771793.968,  140253.342,  5124304.349 metres.
     *   <b>Step 3:</b> (X,Y,Z) after shift:         +84.87,      +96.49,      +116.95  metres
     *   <b>Step 4:</b> Target point in ED50:   53°48'36.565"N,  02'07"51.477"E,  28.02 metres.
     * </pre>
     *
     * @param  step The step as a value from 1 to 5 inclusive.
     * @return The sample point at the given step.
     */
    public static double[] samplePoint(final int step) {
        switch (step) {
            case 1: return new double[] {
                         2 + ( 7 + 46.380/60)/60,   // λ: Longitude
                        53 + (48 + 33.820/60)/60,   // φ: Latitude
                        73.00                       // h: Height
                    };
            case 2: return new double[] {
                        3771793.968,                // X: Toward prime meridian
                         140253.342,                // Y: Toward 90° east
                        5124304.349                 // Z: Toward north pole
                    };
            case 3: return new double[] {
                        3771878.84,                 // X: Toward prime meridian
                         140349.83,                 // Y: Toward 90° east
                        5124421.30                  // Z: Toward north pole
                    };
            case 4: return new double[] {           // Result according geocentric translation
                         2 + ( 7 + 51.477/60)/60,   // λ: Longitude
                        53 + (48 + 36.565/60)/60,   // φ: Latitude
                        28.02                       // h: Height
                    };
            case 5: return new double[] {           // Result according abridged Molodensky
                         2 + ( 7 + 51.477/60)/60,   // λ: Longitude
                        53 + (48 + 36.563/60)/60,   // φ: Latitude
                        28.091                      // h: Height
                    };
            default: throw new AssertionError(step);
        }
    }

    /**
     * Returns the sample point precision for the given step.
     *
     * @param  step The step as a value from 1 to 4 inclusive.
     * @return The sample point precision for the given step.
     */
    public static double precision(final int step) {
        switch (step) {
            case 1:
            case 4:
            case 5: return 0.001 / 60 / 60 / 2;     // Half the precision of (λ,φ) values given by EPSG
            case 2: return 0.001 / 2;               // Half the precision for (X,Y,Z) values given by EPSG
            case 3: return 0.01 / 2;                // Half the precision for (X,Y,Z) values given by EPSG
            default: throw new AssertionError(step);
        }
    }

    /**
     * Creates a transformation for the given method.
     *
     * @param method The method to test.
     */
    private void create(final GeocentricAffine method) throws FactoryException {
        final ParameterValueGroup values = method.getParameters().createValue();
        setTranslation(values);
        if (method instanceof GeocentricAffineBetweenGeographic) {
            setEllipsoids(values, CommonCRS.WGS84.ellipsoid(), CommonCRS.ED50.ellipsoid());
        }
        transform = method.createMathTransform(DefaultFactories.forBuildin(MathTransformFactory.class), values);
    }

    /**
     * Creates a datum shift operation without using the provider. This method is equivalent to the
     * <cite>"Geocentric translations (geog3D domain)"</cite> method (EPSG:1035), but the transform
     * steps are created without the use of any {@link ParameterValueGroup}. This way to create the
     * datum shift is provided for better separation of aspects being tested.
     *
     * @param factory  The factory to use for creating the transforms.
     * @param source   The source ellipsoid.
     * @param target   The target ellipsoid.
     * @param tX       Geocentric translation on the X axis.
     * @param tY       Geocentric translation on the Y axis.
     * @param tZ       Geocentric translation on the Z axis.
     * @return Transform performing the datum shift.
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws NoninvertibleTransformException if an error occurred while creating the transform.
     *
     * @see #testGeographicDomain()
     */
    public static MathTransform createDatumShiftForGeographic3D(
            final MathTransformFactory factory,
            final Ellipsoid source, final Ellipsoid target,
            final double tX, final double tY, final double tZ)
            throws FactoryException, NoninvertibleTransformException
    {
        final Matrix4 translation = new Matrix4();
        translation.m03 = tX;
        translation.m13 = tY;
        translation.m23 = tZ;
        final MathTransform step1 = EllipsoidToCentricTransform.createGeodeticConversion(factory, source, true);
        final MathTransform step3 = EllipsoidToCentricTransform.createGeodeticConversion(factory, target, true).inverse();
        final MathTransform step2 = factory.createAffineTransform(translation);
        return factory.createConcatenatedTransform(step1,
               factory.createConcatenatedTransform(step2, step3));
    }

    /**
     * Creates a "Geographic 2D to 3D → Geocentric → Affine → Geographic → Geographic 3D to 2D" chain
     * using EPSG or OGC standard operation methods and parameters. This is used for integration tests.
     *
     * @param  factory The math transform factory to use for creating and concatenating the transform.
     * @return The chain of transforms.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    public static MathTransform createDatumShiftForGeographic2D(final MathTransformFactory factory) throws FactoryException {
        final Parameters values = Parameters.castOrWrap(factory.getDefaultParameters("Geocentric translations (geog2D domain)"));
        setTranslation(values);
        setEllipsoids(values, CommonCRS.WGS84.ellipsoid(), CommonCRS.ED50.ellipsoid());
        final MathTransform gt = new GeocentricTranslation().createMathTransform(factory, values);
        assertFalse("isIdentity", gt.isIdentity());
        assertEquals("sourceDimensions", 3, gt.getSourceDimensions());
        assertEquals("targetDimensions", 3, gt.getTargetDimensions());
        assertInstanceOf("Geocentric translation", LinearTransform.class, gt);
        return Geographic3Dto2DTest.createDatumShiftForGeographic2D(factory, gt, values);
    }

    /**
     * Sets the translation parameters in the given parameter value group.
     */
    private static void setTranslation(final ParameterValueGroup values) {
        values.parameter("X-axis translation").setValue(TX);
        values.parameter("Y-axis translation").setValue(TY);
        values.parameter("Z-axis translation").setValue(TZ);
    }

    /**
     * Sets the source and target ellipsoid axes in the given parameter value group.
     */
    static void setEllipsoids(final ParameterValueGroup values, final Ellipsoid source, final Ellipsoid target) {
        values.parameter("src_semi_major").setValue(source.getSemiMajorAxis());
        values.parameter("src_semi_minor").setValue(source.getSemiMinorAxis());
        values.parameter("tgt_semi_major").setValue(target.getSemiMajorAxis());
        values.parameter("tgt_semi_minor").setValue(target.getSemiMinorAxis());
    }

    /**
     * Tests transformation of the sample point from WGS84 to ED50.
     *
     * @param sourceStep The {@link #samplePoint(int)} to use as the source coordinate.
     * @param targetStep The {@link #samplePoint(int)} expected as a result of the transformation.
     */
    private void datumShift(final int sourceStep, final int targetStep) throws TransformException {
        tolerance = precision(targetStep);
        tolerance = Formulas.LINEAR_TOLERANCE;                 // Other SIS branches use a stricter threshold.
        verifyTransform(samplePoint(sourceStep), samplePoint(targetStep));
        validate();
    }

    /**
     * Tests <cite>"Geocentric translations (geocentric domain)"</cite> (EPSG:1031).
     *
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws TransformException if transformation of a point failed.
     */
    @Test
    public void testGeocentricDomain() throws FactoryException, TransformException {
        create(new GeocentricTranslation());
        assertTrue(transform instanceof LinearTransform);
        derivativeDeltas = new double[] {100, 100, 100};    // In metres
        datumShift(2, 3);
    }

    /**
     * Tests <cite>"Geocentric translations (geog3D domain)"</cite> (EPSG:1035).
     *
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws TransformException if transformation of a point failed.
     */
    @Test
    @DependsOnMethod("testGeocentricDomain")
    public void testGeographicDomain() throws FactoryException, TransformException {
        create(new GeocentricTranslation3D());
        assertFalse(transform instanceof LinearTransform);
        final double delta = toRadians(100.0 / 60) / 1852;      // Approximatively 100 metres
        derivativeDeltas = new double[] {delta, delta, 100};    // (Δλ, Δφ, Δh)
        zTolerance = Formulas.LINEAR_TOLERANCE / 2;             // Half the precision of h value given by EPSG
        zDimension = new int[] {2};                             // Dimension of h where to apply zTolerance
        tolerance  = Formulas.LINEAR_TOLERANCE;                 // Other SIS branches use a stricter threshold.
        datumShift(1, 4);
    }

    /**
     * Tests conversion of random points.
     *
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws TransformException if transformation of a point failed.
     */
    @Test
    @DependsOnMethod("testGeographicDomain")
    public void testRandomPoints() throws FactoryException, TransformException {
        testGeographicDomain();     // For creating the transform.
        tolerance = Formulas.LINEAR_TOLERANCE;
//      toleranceModifier = ToleranceModifier.GEOGRAPHIC;
        verifyInDomain(CoordinateDomain.GEOGRAPHIC, 831342815);
    }

    /**
     * Tests Well Known Text formatting of a two-dimensional transform.
     * The main point of this test is to verify that Geographic 2D/3D conversions have been
     * inserted before and after the transform, and that Geographic/Geocentric conversions
     * have been replaced by Bursa-Wolf parameters for formatting purpose.
     *
     * @throws FactoryException if an error occurred while creating the transform.
     */
    @Test
    @DependsOnMethod("testWKT3D")
    public void testWKT2D() throws FactoryException {
        create(new GeocentricTranslation2D());
        verifyWKT2D();
    }

    /**
     * Verifies the WKT formatting of current transform.
     */
    private void verifyWKT2D() {
        assertWktEquals("CONCAT_MT[\n" +
                        "  INVERSE_MT[PARAM_MT[“Geographic3D to 2D conversion”]],\n" +
                        "  PARAM_MT[“Ellipsoid_To_Geocentric”,\n" +
                        "    PARAMETER[“semi_major”, 6378137.0],\n" +
                        "    PARAMETER[“semi_minor”, 6356752.314245179]],\n" +
                        "  PARAM_MT[“Geocentric translations (geocentric domain)”,\n" +
                        "    PARAMETER[“dx”, 84.87],\n" +
                        "    PARAMETER[“dy”, 96.49],\n" +
                        "    PARAMETER[“dz”, 116.95]],\n" +
                        "  PARAM_MT[“Geocentric_To_Ellipsoid”,\n" +
                        "    PARAMETER[“semi_major”, 6378388.0],\n" +
                        "    PARAMETER[“semi_minor”, 6356911.9461279465]],\n" +
                        "  PARAM_MT[“Geographic3D to 2D conversion”]]");
    }

    /**
     * Tests Well Known Text formatting.
     * The main point of this test is to verify that the affine transform between the two
     * Geographic/Geocentric conversions have been replaced by Bursa-Wolf parameters for
     * formatting purpose.
     *
     * @throws FactoryException if an error occurred while creating the transform.
     */
    @Test
    @DependsOnMethod("testGeographicDomain")
    public void testWKT3D() throws FactoryException {
        create(new GeocentricTranslation3D());
        assertWktEquals("CONCAT_MT[\n" +
                        "  PARAM_MT[“Ellipsoid_To_Geocentric”,\n" +
                        "    PARAMETER[“semi_major”, 6378137.0],\n" +
                        "    PARAMETER[“semi_minor”, 6356752.314245179]],\n" +
                        "  PARAM_MT[“Geocentric translations (geocentric domain)”,\n" +
                        "    PARAMETER[“dx”, 84.87],\n" +
                        "    PARAMETER[“dy”, 96.49],\n" +
                        "    PARAMETER[“dz”, 116.95]],\n" +
                        "  PARAM_MT[“Geocentric_To_Ellipsoid”,\n" +
                        "    PARAMETER[“semi_major”, 6378388.0],\n" +
                        "    PARAMETER[“semi_minor”, 6356911.9461279465]]]");
        /*
         * The above WKT what was we show to users. But what we have in memory is different:
         * affines before, after and between the two Geographic/Geocentric conversions.
         */
        assertInternalWktEquals(
                "Concat_MT[\n" +
                "  Param_MT[“Affine”,\n" +
                "    Parameter[“num_row”, 4],\n" +
                "    Parameter[“num_col”, 4],\n" +
                "    Parameter[“elt_0_0”, 0.017453292519943295],\n" +
                "    Parameter[“elt_1_1”, 0.017453292519943295],\n" +
                "    Parameter[“elt_2_2”, 1.567855942887398E-7]],\n" +
                "  Param_MT[“Ellipsoid (radians domain) to centric”,\n" +
                "    Parameter[“eccentricity”, 0.08181919084262157],\n" +
                "    Parameter[“target”, “CARTESIAN”],\n" +
                "    Parameter[“dim”, 3]],\n" +
                "  Param_MT[“Affine”,\n" +
                "    Parameter[“num_row”, 4],\n" +
                "    Parameter[“num_col”, 4],\n" +
                "    Parameter[“elt_0_0”, 0.9999606483644456],\n" +
                "    Parameter[“elt_0_3”, 1.3305869758942228E-5],\n" +
                "    Parameter[“elt_1_1”, 0.9999606483644456],\n" +
                "    Parameter[“elt_1_3”, 1.512764667185502E-5],\n" +
                "    Parameter[“elt_2_2”, 0.9999606483644456],\n" +
                "    Parameter[“elt_2_3”, 1.8335353697517302E-5]],\n" +
                "  Param_MT[“Centric to ellipsoid (radians domain)”,\n" +
                "    Parameter[“eccentricity”, 0.08199188997902956],\n" +
                "    Parameter[“target”, “CARTESIAN”],\n" +
                "    Parameter[“dim”, 3]],\n" +
                "  Param_MT[“Affine”,\n" +
                "    Parameter[“num_row”, 4],\n" +
                "    Parameter[“num_col”, 4],\n" +
                "    Parameter[“elt_0_0”, 57.29577951308232],\n" +
                "    Parameter[“elt_1_1”, 57.29577951308232],\n" +
                "    Parameter[“elt_2_2”, 6378388.0]]]");
    }

    /**
     * Tests "Geographic 2D to 3D → Geocentric → Affine → Geographic → Geographic 3D to 2D" chain
     * created from a {@link MathTransformFactory}. Because this test involves a lot of steps,
     * this is more an integration test than a unit test: a failure here may not be easy to debug.
     *
     * @throws FactoryException if an error occurred while creating the transform.
     */
    @Test
    @DependsOnMethod("testWKT2D")
    public void testIntegration() throws FactoryException {
        transform = createDatumShiftForGeographic2D(DefaultFactories.forBuildin(MathTransformFactory.class));
        verifyWKT2D();
    }
}
