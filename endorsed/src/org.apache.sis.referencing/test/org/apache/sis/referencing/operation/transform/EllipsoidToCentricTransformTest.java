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

import java.util.Map;
import java.util.Iterator;
import static java.lang.StrictMath.toRadians;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.referencing.datum.DefaultEllipsoid;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.referencing.datum.HardCodedDatum;
import static org.apache.sis.test.Assertions.assertSerializedEquals;
import org.apache.sis.referencing.operation.provider.GeocentricTranslationTest;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.test.ToleranceModifier;


/**
 * Tests {@link EllipsoidToCentricTransform} from geographic to geocentric coordinates.
 * When a test provides hard-coded expected results, those results are in Cartesian coordinates.
 * See {@link #targetType} for more information.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public class EllipsoidToCentricTransformTest extends MathTransformTestCase {
    /**
     * Whether the {@code EllipsoidToCentricTransform} should target Cartesian or spherical coordinates.
     * The default value is {@code CARTESIAN}. Note that even if this field is set to {@cide SPHERICAL},
     * the {@link #transform} target may still be Cartesian with a calculation done in two steps:
     * geographic to spherical, then {@link SphericalToCartesian}.
     */
    protected EllipsoidToCentricTransform.TargetType targetType;

    /**
     * Whether to add a spherical to Cartesian conversion after the {@linkplain #transform transform} to test.
     * It should be true only if {@link #targetType} is {@code SPHERICAL}.
     */
    protected boolean addSphericalToCartesian;

    /**
     * Creates a new test case.
     */
    public EllipsoidToCentricTransformTest() {
        targetType = EllipsoidToCentricTransform.TargetType.CARTESIAN;
    }

    /**
     * Convenience method for creating an instance from an ellipsoid.
     * The target coordinate system is usually Cartesian. If {@link #targetType} is {@code SPHERICAL},
     * a {@link SphericalToCartesian} step is added as an opaque {@code MathTransform} for preventing
     * Apache <abbr>SIS</abbr> to optimize the concatenation result.
     *
     * @param  ellipsoid   the semi-major and semi-minor axis lengths with their unit of measurement.
     * @param  withHeight  whether source geographic coordinates include an ellipsoidal height.
     * @throws FactoryException if an error occurred while creating a transform.
     */
    final void createGeodeticConversion(final Ellipsoid ellipsoid, final boolean withHeight) throws FactoryException {
        final MathTransformFactory factory = DefaultMathTransformFactory.provider();
        transform = EllipsoidToCentricTransform.createGeodeticConversion(factory, ellipsoid, withHeight, targetType);
        /*
         * If the ellipsoid is a sphere, then `EllipsoidToCentricTransform.createGeodeticConversion(…)` created a
         * `SphericalToCartesian` instance instead of an `EllipsoidToCentricTransform` instance. Create manually
         * the `EllipsoidToCentricTransform` here and wrap the two transforms in a comparator for making sure that
         * the two implementations are consistent.
         */
        if (ellipsoid.isSphere() && targetType == EllipsoidToCentricTransform.TargetType.CARTESIAN) {
            var tr = new EllipsoidToCentricTransform(ellipsoid, withHeight, targetType);
            transform = new TransformResultComparator(transform, tr.context.completeTransform(factory, tr), 1E-2);
        }
        /*
         * If the transform is from geographic to spherical coordinates, add a spherical to Cartesian step.
         * Note that each step works in degrees, not in radians. The use of `MathTransformWrapper` prevent
         * the "radians to degrees to radians" conversions to be optimized, which is intentional for test.
         */
        if (addSphericalToCartesian) {
            var tr = new MathTransformWrapper(SphericalToCartesian.INSTANCE.completeTransform(factory));
            transform = ConcatenatedTransform.create(factory, transform, tr);
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
        createGeodeticConversion(HardCodedDatum.WGS84.getEllipsoid(), true);
        isInverseTransformSupported = false;    // Geocentric to geographic is not the purpose of this test.
        validate();

        final double delta = toRadians(100.0 / 60) / 1852;          // Approximately 100 metres
        derivativeDeltas = new double[] {delta, delta, 100};        // (Δλ, Δφ, Δh)
        tolerance = GeocentricTranslationTest.precision(2);         // Half the precision of target sample point
        verifyTransform(GeocentricTranslationTest.samplePoint(1),   // 53°48'33.820"N, 02°07'46.380"E, 73.00 metres
                        GeocentricTranslationTest.samplePoint(2));  // 3771793.968,  140253.342,  5124304.349 metres
    }

    /**
     * Tests conversion of a single point from geocentric to geographic coordinates.
     * This method uses the same point as {@link #testGeographicToGeocentric()}.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException if conversion of the sample point failed.
     */
    @Test
    public void testGeocentricToGeographic() throws FactoryException, TransformException {
        createGeodeticConversion(HardCodedDatum.WGS84.getEllipsoid(), true);
        transform = transform.inverse();
        isInverseTransformSupported = false;    // Geographic to geocentric is not the purpose of this test.
        validate();

        derivativeDeltas = new double[] {100, 100, 100};            // In metres
        tolerance  = GeocentricTranslationTest.precision(1);        // Required precision for (λ,φ)
        zTolerance = Formulas.LINEAR_TOLERANCE / 2;                 // Required precision for h
        zDimension = new int[] {2};                                 // Dimension of h where to apply zTolerance
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
    public void testRandomPoints() throws FactoryException, TransformException {
        final double delta = toRadians(100.0 / 60) / 1852;          // Approximately 100 metres
        derivativeDeltas  = new double[] {delta, delta, 100};       // (Δλ, Δφ, Δh)
        tolerance         = Formulas.LINEAR_TOLERANCE;
        toleranceModifier = ToleranceModifier.PROJECTION;
        createGeodeticConversion(HardCodedDatum.WGS84.getEllipsoid(), true);
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
        final var factory   = DefaultMathTransformFactory.provider();
        final var ellipsoid = DefaultEllipsoid.createEllipsoid(Map.of("name", "source"), 6000000, 4000000, Units.METRE);
        transform = EllipsoidToCentricTransform.createGeodeticConversion(factory, ellipsoid, true, targetType);

        final double delta = toRadians(100.0 / 60) / 1852;
        derivativeDeltas  = new double[] {delta, delta, 100};
        tolerance         = Formulas.LINEAR_TOLERANCE;
        toleranceModifier = ToleranceModifier.PROJECTION;
        verifyInverse(40, 30, 10000);
    }

    /**
     * Executes the derivative test using the given ellipsoid.
     *
     * @param  ellipsoid   the ellipsoid to use for the test.
     * @param  withHeight  whether geographic coordinates include an ellipsoidal height (i.e. are 3-D).
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException should never happen.
     */
    private void testDerivative(final Ellipsoid ellipsoid, final boolean withHeight) throws FactoryException, TransformException {
        createGeodeticConversion(ellipsoid, withHeight);
        DirectPosition point = withHeight ? new GeneralDirectPosition(-10, 40, 200) : new DirectPosition2D(-10, 40);
        /*
         * Derivative of the direct transform.
         */
        tolerance = 1E-2;
        derivativeDeltas = new double[] {
            toRadians(1.0 / 60) / 1852,             // Approximately one metre.
            toRadians(1.0 / 60) / 1852,
            1
        };
        verifyDerivative(point.getCoordinates());
        /*
         * Derivative of the inverse transform.
         */
        point = transform.transform(point, null);
        transform = transform.inverse();
        tolerance = 1E-8;
        derivativeDeltas = new double[] {1,1,1};    // Approximately one metre.
        verifyDerivative(point.getCoordinates());
    }

    /**
     * Tests the {@link EllipsoidToCentricTransform#derivative(DirectPosition)} method on a sphere.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException should never happen.
     */
    @Test
    public void testDerivativeOnSphere() throws FactoryException, TransformException {
        testDerivative(HardCodedDatum.SPHERE.getEllipsoid(), true);
        testDerivative(HardCodedDatum.SPHERE.getEllipsoid(), false);
    }

    /**
     * Tests the {@link EllipsoidToCentricTransform#derivative(DirectPosition)} method on an ellipsoid.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException should never happen.
     */
    @Test
    public void testDerivative() throws FactoryException, TransformException {
        testDerivative(HardCodedDatum.WGS84.getEllipsoid(), true);
        testDerivative(HardCodedDatum.WGS84.getEllipsoid(), false);
    }

    /**
     * Tests serialization. This method performs the same test as {@link #testGeographicToGeocentric()}
     * and {@link #testGeocentricToGeographic()}, but on the deserialized instance as a way to verify
     * that transient fields have been correctly restored.
     *
     * @throws FactoryException if an error occurred while creating a transform.
     * @throws TransformException if conversion of the sample point failed.
     */
    @Test
    public void testSerialization() throws FactoryException, TransformException {
        createGeodeticConversion(HardCodedDatum.WGS84.getEllipsoid(), true);
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
     * Tests {@link EllipsoidToCentricTransform#tryConcatenate(TransformJoiner)}.
     * The test creates <q>Geographic 3D to 2D conversion</q>, <q>Geographic/Geocentric conversions</q>
     * and <q>Geocentric translation</q> transforms, then concatenate them.
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
        transform = GeocentricTranslationTest.createDatumShiftForGeographic2D(DefaultMathTransformFactory.provider());
        final Iterator<MathTransform> it = MathTransforms.getSteps(transform).iterator();
        MathTransform step;

        // Degrees to radians
        assertInstanceOf(LinearTransform.class, step = it.next());
        assertEquals(2, step.getSourceDimensions());
        assertEquals(2, step.getTargetDimensions());

        // Ellipsoid to geocentric
        assertInstanceOf(EllipsoidToCentricTransform.class, step = it.next());
        assertEquals(2, step.getSourceDimensions());
        assertEquals(3, step.getTargetDimensions());

        // Datum shift
        assertInstanceOf(LinearTransform.class, step = it.next());
        assertEquals(3, step.getSourceDimensions());
        assertEquals(3, step.getTargetDimensions());

        // Geocentric to ellipsoid
        assertInstanceOf(AbstractMathTransform.Inverse.class, step = it.next());
        assertEquals(3, step.getSourceDimensions());
        assertEquals(2, step.getTargetDimensions());

        // Radians to degrees
        assertInstanceOf(LinearTransform.class, step = it.next());
        assertEquals(2, step.getSourceDimensions());
        assertEquals(2, step.getTargetDimensions());
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
        createGeodeticConversion(HardCodedDatum.WGS84.getEllipsoid(), true);
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
        createGeodeticConversion(HardCodedDatum.WGS84.getEllipsoid(), false);
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
        createGeodeticConversion(HardCodedDatum.WGS84.getEllipsoid(), true);
        assertInternalWktEquals(
                "Concat_MT[\n" +
                "  Param_MT[“Affine”,\n" +
                "    Parameter[“num_row”, 4],\n" +
                "    Parameter[“num_col”, 4],\n" +
                "    Parameter[“elt_0_0”, 0.017453292519943295],\n" +
                "    Parameter[“elt_1_1”, 0.017453292519943295],\n" +
                "    Parameter[“elt_2_2”, 1.567855942887398E-7]],\n" +
                "  Param_MT[“Ellipsoid (radians domain) to centric”,\n" +
                "    Parameter[“eccentricity”, 0.0818191908426215],\n" +
                "    Parameter[“csType”, “CARTESIAN”],\n" +
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
                "    Parameter[“eccentricity”, 0.0818191908426215],\n" +
                "    Parameter[“csType”, “CARTESIAN”],\n" +
                "    Parameter[“dim”, 3]],\n" +
                "  Param_MT[“Affine”,\n" +
                "    Parameter[“num_row”, 4],\n" +
                "    Parameter[“num_col”, 4],\n" +
                "    Parameter[“elt_0_0”, 57.29577951308232],\n" +
                "    Parameter[“elt_1_1”, 57.29577951308232],\n" +
                "    Parameter[“elt_2_2”, 6378137.0]]]");
    }
}
