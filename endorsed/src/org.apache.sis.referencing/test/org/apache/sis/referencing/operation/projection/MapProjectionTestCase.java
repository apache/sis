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

import java.util.Random;
import static java.lang.Double.isNaN;
import static java.lang.StrictMath.*;
import org.opengis.util.FactoryException;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.parameter.ParameterValueGroup;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.referencing.operation.provider.MapProjection;
import org.apache.sis.referencing.operation.provider.AbstractProvider;
import org.apache.sis.referencing.operation.transform.CoordinateDomain;
import org.apache.sis.referencing.operation.transform.MathTransformProvider;
import org.apache.sis.referencing.operation.transform.MathTransforms;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.referencing.operation.transform.MathTransformFactoryMock;
import org.apache.sis.referencing.operation.transform.MathTransformTestCase;
import org.apache.sis.referencing.datum.GeodeticDatumMock;
import org.apache.sis.test.TestUtilities;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.test.referencing.ParameterizedTransformTest;


/**
 * Base class of map projection tests.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
abstract class MapProjectionTestCase extends MathTransformTestCase {
    /**
     * Axis length of WGS84 ellipsoid.
     */
    static final double WGS84_A = 6378137,
                        WGS84_B = 6356752.314245179;

    /**
     * Axis length of Clarke 1866 ellipsoid. This ellipsoid is used by Snyder examples.
     */
    static final double CLARKE_A = 6378206.4,
                        CLARKE_B = 6356583.8;

    /**
     * A radius for tests of spherical formulas.
     */
    static final double RADIUS = 6400000;

    /**
     * Tolerance level for comparing formulas on the unitary sphere or ellipsoid.
     */
    static final double NORMALIZED_TOLERANCE = 1E-12;

    /**
     * Creates a new test case.
     */
    MapProjectionTestCase() {
    }

    /**
     * Instantiates the object to use for running GeoAPI test.
     *
     * @param  provider  the provider of the projection to test.
     * @return the GeoAPI test class using the given provider.
     */
    static ParameterizedTransformTest createGeoApiTest(final MapProjection provider) {
        return new ParameterizedTransformTest(new MathTransformFactoryMock(provider));
    }

    /**
     * Instantiates the object to use for running GeoAPI test without derivative tests.
     *
     * @param  provider  the provider of the projection to test.
     * @return the GeoAPI test class using the given provider.
     */
    static ParameterizedTransformTest createGeoApiTestNoDerivatives(final MapProjection provider) {
        final class Tester extends ParameterizedTransformTest {
            Tester(final MapProjection provider) {
                super(new MathTransformFactoryMock(provider));
                isDerivativeSupported = false;
            }
        }
        return new Tester(provider);
    }

    /**
     * Creates a context with the given factory and parameters, used for map projection construction.
     * The given parameters should not be {@code null}, but this method nevertheless accepts null when
     * the caller knows that the tested code will not use parameters.
     *
     * @param  factory     the factory to use, or {@code null} for the default one.
     * @param  parameters  the parameters. Should not be null, but null is nevertheless accepted for testing purposes.
     * @return the context to use in map projection construction.
     */
    static MathTransformProvider.Context context(final MathTransformFactory factory, final ParameterValueGroup parameters) {
        return new MathTransformProvider.Context() {
            /** Returns the specified factory, or the default one if none. */
            @Override public MathTransformFactory getFactory() {
                return (factory != null) ? factory : MathTransformProvider.Context.super.getFactory();
            }

            /** Returns the specified parameters (possible null). */
            @Override public ParameterValueGroup getCompletedParameters() {
                return parameters;
            }
        };
    }

    /**
     * Returns the parameters to use for instantiating the projection to test.
     * The parameters are initialized with the ellipse semi-axis lengths.
     *
     * @param  provider     the provider of the projection to test.
     * @param  ellipsoidal  {@code false} for a sphere, or {@code true} for WGS84 ellipsoid.
     * @return the parameters to use for instantiating the projection.
     */
    static Parameters parameters(final AbstractProvider provider, final boolean ellipsoidal) {
        final Parameters parameters = Parameters.castOrWrap(provider.getParameters().createValue());
        final Ellipsoid ellipsoid = (ellipsoidal ? GeodeticDatumMock.WGS84 : GeodeticDatumMock.SPHERE).getEllipsoid();
        parameters.parameter(Constants.SEMI_MAJOR).setValue(ellipsoid.getSemiMajorAxis());
        parameters.parameter(Constants.SEMI_MINOR).setValue(ellipsoid.getSemiMinorAxis());
        if (ellipsoidal) {
            parameters.parameter(Constants.INVERSE_FLATTENING).setValue(ellipsoid.getInverseFlattening());
        }
        return parameters;
    }

    /**
     * Initializes a complete projection (including conversion from degrees to radians) for the given provider.
     * Base CRS axis order is (longitude, latitude).
     */
    final void createCompleteProjection(final AbstractProvider provider,
            final double semiMajor,
            final double semiMinor,
            final double centralMeridian,
            final double latitudeOfOrigin,
            final double standardParallel1,
            final double standardParallel2,
            final double scaleFactor,
            final double falseEasting,
            final double falseNorthing) throws FactoryException
    {
        final Parameters values = Parameters.castOrWrap(provider.getParameters().createValue());
        values.parameter(Constants.SEMI_MAJOR).setValue(semiMajor);
        values.parameter(Constants.SEMI_MINOR).setValue(semiMinor);
        if (semiMajor == WGS84_A && semiMinor == WGS84_B) {
            values.parameter(Constants.INVERSE_FLATTENING).setValue(298.257223563);
        }
        if (!isNaN(centralMeridian))   values.parameter(Constants.CENTRAL_MERIDIAN)   .setValue(centralMeridian);
        if (!isNaN(latitudeOfOrigin))  values.parameter(Constants.LATITUDE_OF_ORIGIN) .setValue(latitudeOfOrigin);
        if (!isNaN(standardParallel1)) values.parameter(Constants.STANDARD_PARALLEL_1).setValue(standardParallel1);
        if (!isNaN(standardParallel2)) values.parameter(Constants.STANDARD_PARALLEL_2).setValue(standardParallel2);
        if (!isNaN(scaleFactor))       values.parameter(Constants.SCALE_FACTOR)       .setValue(scaleFactor);
        if (!isNaN(falseEasting))      values.parameter(Constants.FALSE_EASTING)      .setValue(falseEasting);
        if (!isNaN(falseNorthing))     values.parameter(Constants.FALSE_NORTHING)     .setValue(falseNorthing);
        transform = provider.createMathTransform(new MathTransformFactoryMock(provider), values);
        validate();
    }

    /**
     * Returns the {@code NormalizedProjection} component of the current transform.
     */
    final NormalizedProjection getKernel() {
        NormalizedProjection kernel = null;
        for (final MathTransform component : MathTransforms.getSteps(transform)) {
            if (component instanceof NormalizedProjection proj) {
                assertNull(kernel, "Found more than one kernel.");
                kernel = proj;
            }
        }
        assertNotNull(kernel);
        return kernel;
    }

    /**
     * Projects the given latitude value. The longitude is fixed to zero.
     * This method is useful for testing the behavior close to poles in a simple case.
     *
     * @param  φ  the latitude.
     * @return the northing.
     * @throws ProjectionException if the projection failed.
     */
    final double transform(final double φ) throws ProjectionException {
        final double[] coordinate = new double[2];
        coordinate[1] = φ;
        ((NormalizedProjection) transform).transform(coordinate, 0, coordinate, 0, false);
        final double y = coordinate[1];
        if (Double.isFinite(y)) {
            assertEquals(0, coordinate[0], tolerance);
        }
        return y;
    }

    /**
     * Inverse projects the given northing value. The easting is fixed to zero.
     * This method is useful for testing the behavior close to poles in a simple case.
     *
     * @param  y  the northing.
     * @return the latitude.
     * @throws ProjectionException if the projection failed.
     */
    final double inverseTransform(final double y) throws ProjectionException {
        final double[] coordinate = new double[2];
        coordinate[1] = y;
        ((NormalizedProjection) transform).inverseTransform(coordinate, 0, coordinate, 0);
        final double φ = coordinate[1];
        if (!isNaN(φ)) {
            /*
             * Opportunistically verify that the longitude is still zero. However, the longitude value is meaningless
             * at poles. We cannot always use coordinate[0] for testing if we are at a pole because its calculation
             * is not finished (the denormalization matrix has not yet been applied).  In the particular case of SIS
             * implementation, we observe sometimes a ±180° rotation, which we ignore below. Such empirical hack is
             * not rigorous, but it is not the purpose of this test to check the longitude value - we are doing only
             * an opportunist check, other test methods will test longitude more accurately.
             */
            double λ = coordinate[0];
            λ -= rint(λ / PI) * PI;
            assertEquals(0, λ, tolerance);
        }
        return φ;
    }

    /**
     * Compares the elliptical formulas with the spherical formulas for random points in the given domain.
     * The spherical formulas are arbitrarily selected as the reference implementation because they are simpler,
     * so less bug-prone, than the elliptical formulas.
     *
     * @param  domain      the domain of the numbers to be generated.
     * @param  randomSeed  the seed for the random number generator, or 0 for choosing a random seed.
     * @throws TransformException if a conversion, transformation or derivative failed.
     */
    final void compareEllipticalWithSpherical(final CoordinateDomain domain, final long randomSeed)
            throws TransformException
    {
        transform = ProjectionResultComparator.sphericalAndEllipsoidal(transform);
        if (derivativeDeltas == null) {
            final double delta = toRadians(100.0 / 60) / 1852;          // Approximately 100 metres.
            derivativeDeltas = new double[] {delta, delta};
        }
        verifyInDomain(domain, randomSeed);
    }

    /**
     * Tests coordinates close to zero. Callers must set the transform and tolerance threshold before to invoke
     * this method. This method tests (among others) the 1.4914711209038602E-154 value, which is the threshold
     * documented in {@link org.apache.sis.referencing.internal.shared.Formulas#fastHypot(double, double)}.
     *
     * @throws TransformException if an error occurred while projecting the coordinate.
     */
    final void verifyValuesNearZero(final double... expected) throws TransformException {
        final double[] source = new double[2];
        verifyTransform(source, expected);        // Test (0,0).
        for (int i=0; i<source.length; i++) {
            source[i] = 1E-20;                    verifyTransform(source, expected);
            source[i] = 1E-100;                   verifyTransform(source, expected);
            source[i] = 1.4914711209038602E-154;  verifyTransform(source, expected);
            source[i] = 2.2227587494850775E-162;  verifyTransform(source, expected);
            source[i] = Double.MIN_NORMAL;        verifyTransform(source, expected);
            source[i] = Double.MIN_VALUE;         verifyTransform(source, expected);
            source[i] = 0;
        }
        final Random r = TestUtilities.createRandomNumberGenerator();
        for (int i=0; i<25; i++) {
            source[i & 1] = Math.scalb(r.nextDouble() - 0.5, r.nextInt(800) + Double.MIN_EXPONENT);
        }
    }
}
