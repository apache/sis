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

import java.util.Iterator;
import javax.measure.unit.SI;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.GeneralDirectPosition;

import static java.lang.StrictMath.toRadians;

// Test dependencies
import org.apache.sis.internal.referencing.provider.GeocentricTranslationTest;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests {@link EllipsoidToCentricTransform}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    CoordinateDomainTest.class,
    ContextualParametersTest.class
})
public final strictfp class EllipsoidToCentricTransformTest extends MathTransformTestCase {
    /**
     * Convenience method for creating an instance from an ellipsoid.
     */
    private void createGeodeticConversion(final Ellipsoid ellipsoid, boolean is3D) throws FactoryException {
        final MathTransformFactory factory = DefaultFactories.forBuildin(MathTransformFactory.class);
        transform = EllipsoidToCentricTransform.createGeodeticConversion(factory, ellipsoid, is3D);
        /*
         * If the ellipsoid is a sphere, then EllipsoidToCentricTransform.createGeodeticConversion(…) created a
         * SphericalToCartesian instance instead than an EllipsoidToCentricTransform instance.  Create manually
         * the EllipsoidToCentricTransform here and wrap the two transform in a comparator for making sure that
         * the two implementations are consistent.
         */
        if (ellipsoid.isSphere()) {
            EllipsoidToCentricTransform tr = new EllipsoidToCentricTransform(
                    ellipsoid.getSemiMajorAxis(),
                    ellipsoid.getSemiMinorAxis(),
                    ellipsoid.getAxisUnit(), is3D,
                    EllipsoidToCentricTransform.TargetType.CARTESIAN);
            transform = new TransformResultComparator(transform, tr.context.completeTransform(factory, tr), 1E-2);
        }
    }

    /**
     * Tests conversion of a single point from geographic to geocentric coordinates.
     * This test uses the example given in EPSG guidance note #7.
     * The point in WGS84 is 53°48'33.820"N, 02°07'46.380"E, 73.00 metres.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException if conversion of the sample point failed.
     */
    @Test
    public void testGeographicToGeocentric() throws FactoryException, TransformException {
        createGeodeticConversion(CommonCRS.WGS84.ellipsoid(), true);
        isInverseTransformSupported = false;    // Geocentric to geographic is not the purpose of this test.
        validate();

        final double delta = toRadians(100.0 / 60) / 1852;          // Approximatively 100 metres
        derivativeDeltas = new double[] {delta, delta, 100};        // (Δλ, Δφ, Δh)
        tolerance = GeocentricTranslationTest.precision(2);         // Half the precision of target sample point
        verifyTransform(GeocentricTranslationTest.samplePoint(1),   // 53°48'33.820"N, 02°07'46.380"E, 73.00 metres
                        GeocentricTranslationTest.samplePoint(2));  // 3771793.968,  140253.342,  5124304.349 metres
    }

    /**
     * Tests conversion of a single point from geocentric to geographic coordinates.
     * This method uses the same point than {@link #testGeographicToGeocentric()}.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException if conversion of the sample point failed.
     */
    @Test
    public void testGeocentricToGeographic() throws FactoryException, TransformException {
        createGeodeticConversion(CommonCRS.WGS84.ellipsoid(), true);
        transform = transform.inverse();
        isInverseTransformSupported = false;    // Geographic to geocentric is not the purpose of this test.
        validate();

        derivativeDeltas = new double[] {100, 100, 100};            // In metres
        tolerance  = GeocentricTranslationTest.precision(1);        // Required precision for (λ,φ)
        zTolerance = Formulas.LINEAR_TOLERANCE / 2;                 // Required precision for h
        zDimension = new int[] {2};                                 // Dimension of h where to apply zTolerance
        tolerance  = 1E-4;                                          // Other SIS branches use a stricter threshold.
        verifyTransform(GeocentricTranslationTest.samplePoint(2),   // X = 3771793.968,  Y = 140253.342,  Z = 5124304.349 metres
                        GeocentricTranslationTest.samplePoint(1));  // 53°48'33.820"N, 02°07'46.380"E, 73.00 metres
    }

    /**
     * Tests conversion of random points.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException if a conversion failed.
     */
    @Test
    @DependsOnMethod({
        "testGeographicToGeocentric",
        "testGeocentricToGeographic"
    })
    public void testRandomPoints() throws FactoryException, TransformException {
        final double delta = toRadians(100.0 / 60) / 1852;          // Approximatively 100 metres
        derivativeDeltas  = new double[] {delta, delta, 100};       // (Δλ, Δφ, Δh)
        tolerance         = Formulas.LINEAR_TOLERANCE;
//      toleranceModifier = ToleranceModifier.PROJECTION;
        createGeodeticConversion(CommonCRS.WGS84.ellipsoid(), true);
        verifyInDomain(CoordinateDomain.GEOGRAPHIC, 306954540);
    }

    /**
     * Tests conversion of a point on an imaginary planet with high eccentricity.
     * The {@link EllipsoidToCentricTransform} may need to use an iterative method
     * for reaching the expected precision.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException if conversion of the sample point failed.
     */
    @Test
    public void testHighEccentricity() throws FactoryException, TransformException, FactoryException {
        transform = EllipsoidToCentricTransform.createGeodeticConversion(
                DefaultFactories.forBuildin(MathTransformFactory.class),
                6000000, 4000000, SI.METRE, true, EllipsoidToCentricTransform.TargetType.CARTESIAN);

        final double delta = toRadians(100.0 / 60) / 1852;
        derivativeDeltas  = new double[] {delta, delta, 100};
        tolerance         = Formulas.LINEAR_TOLERANCE;
//      toleranceModifier = ToleranceModifier.PROJECTION;
        verifyInverse(new double[] {40, 30, 10000});
    }

    /**
     * Executes the derivative test using the given ellipsoid.
     *
     * @param  ellipsoid The ellipsoid to use for the test.
     * @param  hasHeight {@code true} if geographic coordinates include an ellipsoidal height (i.e. are 3-D),
     *         or {@code false} if they are only 2-D.
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException should never happen.
     */
    private void testDerivative(final Ellipsoid ellipsoid, final boolean hasHeight) throws FactoryException, TransformException {
        createGeodeticConversion(ellipsoid, hasHeight);
        DirectPosition point = hasHeight ? new GeneralDirectPosition(-10, 40, 200) : new DirectPosition2D(-10, 40);
        /*
         * Derivative of the direct transform.
         */
        tolerance = 1E-2;
        derivativeDeltas = new double[] {toRadians(1.0 / 60) / 1852}; // Approximatively one metre.
        verifyDerivative(point.getCoordinate());
        /*
         * Derivative of the inverse transform.
         */
        point = transform.transform(point, null);
        transform = transform.inverse();
        tolerance = 1E-8;
        derivativeDeltas = new double[] {1}; // Approximatively one metre.
        verifyDerivative(point.getCoordinate());
    }

    /**
     * Tests the {@link EllipsoidToCentricTransform#derivative(DirectPosition)} method on a sphere.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException should never happen.
     */
    @Test
    public void testDerivativeOnSphere() throws FactoryException, TransformException {
        testDerivative(CommonCRS.SPHERE.ellipsoid(), true);
        testDerivative(CommonCRS.SPHERE.ellipsoid(), false);
    }

    /**
     * Tests the {@link EllipsoidToCentricTransform#derivative(DirectPosition)} method on an ellipsoid.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException should never happen.
     */
    @Test
    @DependsOnMethod("testDerivativeOnSphere")
    public void testDerivative() throws FactoryException, TransformException {
        testDerivative(CommonCRS.WGS84.ellipsoid(), true);
        testDerivative(CommonCRS.WGS84.ellipsoid(), false);
    }

    /**
     * Tests serialization. This method performs the same test than {@link #testGeographicToGeocentric()}
     * and {@link #testGeocentricToGeographic()}, but on the deserialized instance. This allow us to verify
     * that transient fields have been correctly restored.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException if conversion of the sample point failed.
     */
    @Test
    @DependsOnMethod("testRandomPoints")
    public void testSerialization() throws FactoryException, TransformException {
        createGeodeticConversion(CommonCRS.WGS84.ellipsoid(), true);
        transform = assertSerializedEquals(transform);
        /*
         * Below is basically a copy-and-paste of testGeographicToGeocentric(), but
         * with isInverseTransformSupported = true for testing inverse conversion.
         */
        final double delta = toRadians(100.0 / 60) / 1852;
        derivativeDeltas = new double[] {delta, delta, 100};
        tolerance = GeocentricTranslationTest.precision(2);
        verifyTransform(GeocentricTranslationTest.samplePoint(1),
                        GeocentricTranslationTest.samplePoint(2));
    }

    /**
     * Tests {@link EllipsoidToCentricTransform#concatenate(MathTransform, boolean, MathTransformFactory)}.
     * The test creates <cite>"Geographic 3D to 2D conversion"</cite>, <cite>"Geographic/Geocentric conversions"</cite>
     * and <cite>"Geocentric translation"</cite> transforms, then concatenate them.
     *
     * <p>Because this test involves a lot of steps, this is more an integration test than a unit test:
     * a failure here may not be easy to debug.</p>
     *
     * @throws FactoryException if an error occurred while creating a transform.
     *
     * @see GeocentricTranslationTest#testWKT2D()
     */
    @Test
    public void testConcatenate() throws FactoryException {
        transform = GeocentricTranslationTest.createDatumShiftForGeographic2D(
                DefaultFactories.forBuildin(MathTransformFactory.class));
        final Iterator<MathTransform> it = MathTransforms.getSteps(transform).iterator();
        MathTransform step;

        assertInstanceOf("Degrees to radians", LinearTransform.class, step = it.next());
        assertEquals("sourceDimensions", 2, step.getSourceDimensions());
        assertEquals("tourceDimensions", 2, step.getTargetDimensions());

        assertInstanceOf("Ellipsoid to geocentric", EllipsoidToCentricTransform.class, step = it.next());
        assertEquals("sourceDimensions", 2, step.getSourceDimensions());
        assertEquals("tourceDimensions", 3, step.getTargetDimensions());

        assertInstanceOf("Datum shift", LinearTransform.class, step = it.next());
        assertEquals("sourceDimensions", 3, step.getSourceDimensions());
        assertEquals("tourceDimensions", 3, step.getTargetDimensions());

        assertInstanceOf("Geocentric to ellipsoid", AbstractMathTransform.Inverse.class, step = it.next());
        assertEquals("sourceDimensions", 3, step.getSourceDimensions());
        assertEquals("tourceDimensions", 2, step.getTargetDimensions());

        assertInstanceOf("Degrees to radians", LinearTransform.class, step = it.next());
        assertEquals("sourceDimensions", 2, step.getSourceDimensions());
        assertEquals("tourceDimensions", 2, step.getTargetDimensions());
    }

    /**
     * Tests the standard Well Known Text (version 1) formatting for three-dimensional transforms.
     * The result is what we show to users, but is quite different than what SIS has in memory.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException should never happen.
     */
    @Test
    public void testWKT3D() throws FactoryException, TransformException {
        createGeodeticConversion(CommonCRS.WGS84.ellipsoid(), true);
        assertWktEquals("PARAM_MT[“Ellipsoid_To_Geocentric”,\n" +
                        "  PARAMETER[“semi_major”, 6378137.0],\n" +
                        "  PARAMETER[“semi_minor”, 6356752.314245179]]");

        transform = transform.inverse();
        assertWktEquals("PARAM_MT[“Geocentric_To_Ellipsoid”,\n" +
                        "  PARAMETER[“semi_major”, 6378137.0],\n" +
                        "  PARAMETER[“semi_minor”, 6356752.314245179]]");
    }

    /**
     * Tests the standard Well Known Text (version 1) formatting for two-dimensional transforms.
     * The result is what we show to users, but is quite different than what SIS has in memory.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException should never happen.
     */
    @Test
    public void testWKT2D() throws FactoryException, TransformException {
        createGeodeticConversion(CommonCRS.WGS84.ellipsoid(), false);
        assertWktEquals("CONCAT_MT[\n" +
                        "  INVERSE_MT[PARAM_MT[“Geographic3D to 2D conversion”]],\n" +
                        "  PARAM_MT[“Ellipsoid_To_Geocentric”,\n" +
                        "    PARAMETER[“semi_major”, 6378137.0],\n" +
                        "    PARAMETER[“semi_minor”, 6356752.314245179]]]");

        transform = transform.inverse();
        assertWktEquals("CONCAT_MT[\n" +
                        "  PARAM_MT[“Geocentric_To_Ellipsoid”,\n" +
                        "    PARAMETER[“semi_major”, 6378137.0],\n" +
                        "    PARAMETER[“semi_minor”, 6356752.314245179]],\n" +
                        "  PARAM_MT[“Geographic3D to 2D conversion”]]");
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
        createGeodeticConversion(CommonCRS.WGS84.ellipsoid(), true);
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
                "    Parameter[“elt_0_0”, 6378137.0],\n" +
                "    Parameter[“elt_1_1”, 6378137.0],\n" +
                "    Parameter[“elt_2_2”, 6378137.0]]]");

        transform = transform.inverse();
        assertInternalWktEquals(
                "Concat_MT[\n" +
                "  Param_MT[“Affine”,\n" +
                "    Parameter[“num_row”, 4],\n" +
                "    Parameter[“num_col”, 4],\n" +
                "    Parameter[“elt_0_0”, 1.567855942887398E-7],\n" +
                "    Parameter[“elt_1_1”, 1.567855942887398E-7],\n" +
                "    Parameter[“elt_2_2”, 1.567855942887398E-7]],\n" +
                "  Param_MT[“Centric to ellipsoid (radians domain)”,\n" +
                "    Parameter[“eccentricity”, 0.08181919084262157],\n" +
                "    Parameter[“target”, “CARTESIAN”],\n" +
                "    Parameter[“dim”, 3]],\n" +
                "  Param_MT[“Affine”,\n" +
                "    Parameter[“num_row”, 4],\n" +
                "    Parameter[“num_col”, 4],\n" +
                "    Parameter[“elt_0_0”, 57.29577951308232],\n" +
                "    Parameter[“elt_1_1”, 57.29577951308232],\n" +
                "    Parameter[“elt_2_2”, 6378137.0]]]");
    }
}
