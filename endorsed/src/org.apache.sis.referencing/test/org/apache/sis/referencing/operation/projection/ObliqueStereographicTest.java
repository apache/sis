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
package org.apache.sis.referencing.operation.projection;

import static java.lang.StrictMath.*;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.apache.sis.referencing.operation.matrix.Matrix2;


/**
 * Tests the {@link ObliqueStereographic} class.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Emmanuel Giasson (Thales)
 */
public final class ObliqueStereographicTest extends MapProjectionTestCase {
    /**
     * Parameter values provided by the IOGP Report 373-07-02 –
     * <cite>Coordinate conversions and transformation including formulas</cite>
     * for testing {@link ObliqueStereographic} transform conformity.
     * The test uses the parameters for the <cite>Amersfoort / RD New</cite> projection:
     *
     * <ul>
     *   <li>Semi-major axis length:            <var>a</var>  = 6377397.155 metres</li>
     *   <li>Inverse flattening:              1/<var>f</var>  = 299.15281</li>
     *   <li>Latitude of natural origin:        <var>φ₀</var> = 52°09'22.178"N</li>
     *   <li>Longitude of natural origin:       <var>λ₀</var> =  5°23'15.500"E</li>
     *   <li>Scale factor at natural origin:    <var>k₀</var> = 0.9999079</li>
     *   <li>False easting:                     <var>FE</var> = 155000.00 metres</li>
     *   <li>False northing:                    <var>FN</var> = 463000.00 metres</li>
     * </ul>
     *
     * Other parameters (<var>n</var>, <var>R</var>, <var>g</var>, <var>h</var>) are computed from the above.
     * Those parameters fall in three groups:
     *
     * <ul>
     *   <li>Parameters used in linear operations (additions or multiplications) performed <strong>before</strong> the
     *       non-linear part (the "kernel") of the map projection. Those parameters are <var>λ₀</var> and <var>n</var>
     *       and their values are stored in the normalization matrix given by
     *       <code>{@linkplain ContextualParameters#getMatrix ContextualParameters.getMatrix}(NORMALIZATION)</code>.</li>
     *
     *   <li>Parameters used in linear operations (additions or multiplications) performed <strong>after</strong> the
     *       non-linear part (the "kernel") of the map projection. Those parameters are <var>k₀</var>, <var>R</var>,
     *       <var>FE</var> and <var>FN</var> and their values are stored in the denormalization matrix given by
     *       <code>{@linkplain ContextualParameters#getMatrix ContextualParameters.getMatrix}(DENORMALIZATION)</code>.</li>
     *
     *   <li>Other parameters are either used in the non-linear "kernel" of the map projection or used for computing the
     *       above-cited parameters.</li>
     * </ul>
     *
     * <p><b>Note 1:</b> value of <var>R</var> is computed by {@link Initializer#radiusOfConformalSphere(double)}.</p>
     *
     * <p><b>Note 2:</b> we do not follow the Java naming convention here (constants in upper cases) in order to use
     * as much as possible the exact same symbols as in the EPSG guide.</p>
     */
    private static final double φ0  = 0.910296727,      // Latitude of natural origin (rad)
        /* Before kernel */     λ0  = 0.094032038,      // Longitude of natural origin (rad)
        /*  After kernel */     R   = 6382644.571,      // Radius of conformal sphere (m)
                                a   = 6377397.155,      // Semi-major axis length (m)
                                ivf = 299.15281,        // Inverse flattening factor
                                e   = 0.08169683,       // Eccentricity
        /* Before kernel */     n   = 1.000475857,      // Coefficient computed from eccentricity and φ₀.
        /*  After kernel */     k0  = 0.9999079,        // Scale factor
        /*  After kernel */     FE  = 155000.00,        // False Easting (m)
        /*  After kernel */     FN  = 463000.00;        // False Northing (m)

    /**
     * Creates a new test case.
     */
    public ObliqueStereographicTest() {
    }

    /**
     * Compares the <var>n</var> value given in the EPSG guide with the value computed from the formula.
     */
    @Test
    public void testN() {
        assertEquals(n, sqrt(1 + (e*e * pow(cos(φ0), 4)) / (1 - e*e)), 0.5E-9);
    }

    /**
     * Creates a new instance of {@link ObliqueStereographic} for a sphere or an ellipsoid.
     * The new instance is stored in the inherited {@link #transform} field.
     *
     * @param  ellipse  {@code false} for the spherical case, or {@code true} for the ellipsoidal case.
     */
    private void createNormalizedProjection(final boolean ellipse) {
        final OperationMethod op = new org.apache.sis.referencing.operation.provider.ObliqueStereographic();
        final ParameterValueGroup p = op.getParameters().createValue();
        /*
         * Following parameters are not given explicitly by EPSG definitions since they are
         * usually inferred from the datum. However, in the particular case of this test, we
         * need to provide them. The names used below are either OGC names or SIS extensions.
         */
        if (!ellipse) {
            p.parameter("semi_major").setValue(R);
            p.parameter("semi_minor").setValue(R);
        } else {
            p.parameter("semi_major").setValue(a);
            p.parameter("inverse_flattening").setValue(ivf);
        }
        /*
         * Following parameters are reproduced verbatim from EPSG repository and EPSG guide.
         */
        p.parameter("Latitude of natural origin")    .setValue(φ0, Units.RADIAN);
        p.parameter("Longitude of natural origin")   .setValue(λ0, Units.RADIAN);
        p.parameter("Scale factor at natural origin").setValue(k0);
        p.parameter("False easting")                 .setValue(FE, Units.METRE);
        p.parameter("False northing")                .setValue(FN, Units.METRE);

        transform = new ObliqueStereographic(op, (Parameters) p);
    }

    /**
     * Creates the complete transform from the given parameter values.
     * Input and output coordinates will be in degrees, contrarily to the transform
     * created by above {@link #createNormalizedProjection(boolean)} method.
     */
    private void createCompleteTransform(final OperationMethod op, final ParameterValueGroup p) throws FactoryException {
        transform = new ObliqueStereographic(op, (Parameters) p).createMapProjection(context(null, p));
    }

    /**
     * The point given in the EPSG guide for testing the map projection.
     * (φ<sub>t</sub>, λ<sub>t</sub>) is the source geographic coordinate in degrees and
     * (x<sub>t</sub>, y<sub>t</sub>) is the target projected coordinate in metres.
     */
    private static final double φt = 53,                // Latitude in degrees
                                λt = 6,                 // Longitude in degrees
                                Et = 196105.283,        // Easting in metres
                                Nt = 557057.739;        // Northing in metres

    /**
     * Tests {@link ObliqueStereographic#transform(double[], int, double[], int, boolean)}
     * with the values given by the EPSG guide.
     *
     * @throws TransformException if an error occurred while projecting the coordinate.
     */
    @Test
    public void testTransform() throws TransformException {
        final double[] srcPts = new double[] {λt, φt};   // in degrees
        final double[] dstPts = new double[2];

        // Linear operations (normalization) applied before NormalizedTransform.
        srcPts[0] = toRadians(srcPts[0]) - λ0;
        srcPts[1] = toRadians(srcPts[1]);
        srcPts[0] *= n;

        // The non-linear part of map projection (the "kernel").
        createNormalizedProjection(true);
        transform.transform(srcPts, 0, dstPts, 0, 1);

        // Linear operations (denormalization) applied after NormalizedTransform.
        dstPts[0] *= (k0 * 2*R);
        dstPts[1] *= (k0 * 2*R);
        dstPts[0] += FE;
        dstPts[1] += FN;

        assertEquals(Et, dstPts[0], Formulas.LINEAR_TOLERANCE);
        assertEquals(Nt, dstPts[1], Formulas.LINEAR_TOLERANCE);
    }

    /**
     * Tests {@link ObliqueStereographic#inverseTransform(double[], int, double[], int)}
     * with the values given by the EPSG guide.
     *
     * @throws TransformException if an error occurred while projecting the coordinate.
     */
    @Test
    public void testInverseTransform() throws TransformException {
        final double[] srcPts = new double[] {Et, Nt};  // in metres
        final double[] dstPts = new double[2];

        // Linear operations (normalization) applied before NormalizedTransform.
        srcPts[0] -= FE;
        srcPts[1] -= FN;
        srcPts[0] /= (k0 * 2*R);
        srcPts[1] /= (k0 * 2*R);

        // The non-linear part of map projection (the "kernel").
        createNormalizedProjection(true);
        ((NormalizedProjection) transform).inverseTransform(srcPts, 0, dstPts, 0);

        // Linear operations (denormalization) applied after NormalizedTransform.
        dstPts[0] /= n;
        dstPts[0] = toDegrees(dstPts[0] + λ0);
        dstPts[1] = toDegrees(dstPts[1]);

        assertEquals(λt, dstPts[0], Formulas.ANGULAR_TOLERANCE);
        assertEquals(φt, dstPts[1], Formulas.ANGULAR_TOLERANCE);
    }

    /**
     * Tests the <cite>Oblique Stereographic</cite> case (EPSG:9809).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testObliqueStereographic()
     */
    @Test
    public void testObliqueStereographic() throws FactoryException, TransformException {
        createGeoApiTest(new org.apache.sis.referencing.operation.provider.ObliqueStereographic()).testObliqueStereographic();
    }

    /**
     * Tests consistency between forward and reverse projection using a point that was known to fail.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-537">SIS-537</a>
     */
    @Test
    public void testObliqueStereographicConsistency() throws FactoryException, TransformException {
        final OperationMethod op = new org.apache.sis.referencing.operation.provider.ObliqueStereographic();
        final ParameterValueGroup p = op.getParameters().createValue();
        p.parameter("semi_major").setValue(WGS84_A);
        p.parameter("semi_minor").setValue(WGS84_B);
        p.parameter("Latitude of natural origin") .setValue( 45, Units.DEGREE);
        p.parameter("Longitude of natural origin").setValue(-70, Units.DEGREE);
        createCompleteTransform(op, p);
        tolerance = Formulas.ANGULAR_TOLERANCE;
        verifyInverse(30, 45);
    }

    /**
     * Verifies the consistency of spherical formulas with the elliptical formulas.
     * This test transforms the point given in the EPSG guide and takes the result
     * of the elliptical implementation as a reference.
     *
     * @throws TransformException if an error occurred while projecting the coordinate.
     */
    @Test
    public void testSphericalTransform() throws TransformException {
        final double[] srcPts = new double[] {λt, φt};  // in degrees
        final double[] dstPts = new double[2];
        final double[] refPts = new double[2];

        // Linear operations (normalization) applied before NormalizedTransform.
        srcPts[0] = toRadians(srcPts[0]) - λ0;
        srcPts[1] = toRadians(srcPts[1]);
        srcPts[0] *= n;

        // The non-linear part of map projection (the "kernel").
        createNormalizedProjection(false);
        transform.transform(srcPts, 0, refPts, 0, 1);

        // Linear operations (denormalization) applied after NormalizedTransform.
        refPts[0] *= (k0 * 2*R);
        refPts[1] *= (k0 * 2*R);
        refPts[0] += FE;
        refPts[1] += FN;

        // Transform the same point, now using the spherical implementation.
        ObliqueStereographic spherical = (ObliqueStereographic) transform;
        spherical = new ObliqueStereographic.Spherical(spherical);
        spherical.transform(srcPts, 0, dstPts, 0, 1);

        // Linear operations (denormalization) applied after NormalizedTransform.
        dstPts[0] *= (k0 * 2*R);
        dstPts[1] *= (k0 * 2*R);
        dstPts[0] += FE;
        dstPts[1] += FN;

        // Use a smaller tolerance because spherical and elliptical formulas should be equivalent in this case.
        assertArrayEquals(refPts, dstPts, Formulas.LINEAR_TOLERANCE / 1E4);
    }

    /**
     * Verifies the consistency of spherical formulas with the elliptical formulas.
     * This test computes the inverse transform of the point given in the EPSG guide
     * and takes the result of the elliptical implementation as a reference.
     *
     * @throws TransformException if an error occurred while projecting the coordinate.
     */
    @Test
    public void testSphericalInverseTransform() throws TransformException {
        final double[] srcPts = new double[] {Et, Nt};  // in metres
        final double[] dstPts = new double[2];
        final double[] refPts = new double[2];

        // Linear operations (normalization) applied before NormalizedTransform.
        srcPts[0] -= FE;
        srcPts[1] -= FN;
        srcPts[0] /= (k0 * 2*R);
        srcPts[1] /= (k0 * 2*R);

        // The non-linear part of map projection (the "kernel").
        createNormalizedProjection(false);
        ((NormalizedProjection) transform).inverseTransform(srcPts, 0, refPts, 0);

        // Linear operations (denormalization) applied after NormalizedTransform.
        refPts[0] /= n;
        refPts[0] = toDegrees(refPts[0] + λ0);
        refPts[1] = toDegrees(refPts[1]);

        // Transform the same point, now using the spherical implementation.
        ObliqueStereographic spherical = (ObliqueStereographic) transform;
        spherical = new ObliqueStereographic.Spherical(spherical);
        spherical.inverseTransform(srcPts, 0, dstPts, 0);

        // Linear operations (denormalization) applied after NormalizedTransform.
        dstPts[0] /= n;
        dstPts[0] = toDegrees(dstPts[0] + λ0);
        dstPts[1] = toDegrees(dstPts[1]);

        // Use a smaller tolerance because spherical and elliptical formulas should be equivalent in this case.
        assertArrayEquals(refPts, dstPts, Formulas.ANGULAR_TOLERANCE / 1E6);
    }

    /**
     * Verifies the consistency of spherical formulas with the elliptical formulas.
     * This test computes the derivative at a point and takes the result of the elliptical
     * implementation as a reference.
     *
     * @throws TransformException if an error occurred while computing the derivative.
     */
    @Test
    public void testSphericalDerivative() throws TransformException {
        final double[] srcPts = new double[] {λt, φt};  // in degrees
        srcPts[0] = toRadians(srcPts[0]) - λ0;
        srcPts[1] = toRadians(srcPts[1]);
        srcPts[0] *= n;

        // Using elliptical implementation.
        createNormalizedProjection(false);
        final Matrix reference = ((NormalizedProjection) transform).transform(srcPts, 0, null, 0, true);

        // Using spherical implementation.
        ObliqueStereographic spherical = (ObliqueStereographic) transform;
        spherical = new ObliqueStereographic.Spherical(spherical);
        final Matrix derivative = spherical.transform(srcPts, 0, null, 0, true);

        tolerance = 1E-12;
        var delta = new Matrix2(tolerance, tolerance, tolerance, tolerance);
        assertMatrixEquals(reference, derivative, delta, "Spherical derivative");
    }

    /**
     * Creates a projection and derivates a few points.
     *
     * @throws TransformException if an error occurred while computing the derivative.
     */
    @Test
    public void testDerivative() throws TransformException {
        createNormalizedProjection(true);
        tolerance = 1E-9;

        final double delta = toRadians(100.0 / 60) / 1852;      // Approximately 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        verifyDerivative(toRadians( 0), toRadians( 0));
        verifyDerivative(toRadians(-3), toRadians(30));
        verifyDerivative(toRadians(+6), toRadians(60));
    }

    /**
     * Tests the delegation to {@link PolarStereographic} implementation when the latitude of origin is ±90°.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testPolarStereographic() throws FactoryException, TransformException {
        final OperationMethod op = new org.apache.sis.referencing.operation.provider.ObliqueStereographic();
        final ParameterValueGroup p = op.getParameters().createValue();
        p.parameter("semi_major")                    .setValue(WGS84_A);
        p.parameter("inverse_flattening")            .setValue(298.2572236);
        p.parameter("Latitude of natural origin")    .setValue(90);
        p.parameter("Scale factor at natural origin").setValue(0.994);
        p.parameter("False easting")                 .setValue(2000000);
        p.parameter("False northing")                .setValue(2000000);
        createCompleteTransform(op, p);
        tolerance = 0.01;
        verifyTransform(new double[] {44, 73}, new double[] {3320416.75, 632668.43});
    }

    /**
     * Tests {@link ObliqueStereographic.Spherical#inverseTransform(double[], int, double[], int)} with input
     * coordinates close to zero. The tested method implementation has an indetermination at ρ = 0, so we test
     * its behavior close to that indetermination point. We test both coordinates, but the main coordinate of
     * interest is <var>y</var> because it is used in a formula containing y/ρ where y and ρ both tend to zero.
     *
     * @throws TransformException if an error occurred while projecting the coordinate.
     */
    @Test
    public void testValuesNearZero() throws TransformException {
        createNormalizedProjection(false);
        transform = transform.inverse();
        tolerance = 1E-15;
        verifyValuesNearZero(0, φ0);
    }
}
