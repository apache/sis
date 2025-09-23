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
import java.io.IOException;
import java.io.LineNumberReader;
import static java.lang.Double.NaN;
import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.toRadians;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.referencing.operation.provider.TransverseMercatorSouth;
import org.apache.sis.referencing.operation.transform.CoordinateDomain;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.CharSequences;
import static org.apache.sis.referencing.operation.provider.TransverseMercator.LATITUDE_OF_ORIGIN;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.apache.sis.test.OptionalTestData;
import static org.apache.sis.test.Assertions.assertSerializedEquals;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.test.CalculationType;


/**
 * Tests the {@link TransverseMercator} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class TransverseMercatorTest extends MapProjectionTestCase {
    /**
     * Distance from central meridian, in degrees, at which errors are considered too important.
     * This threshold is determined by comparisons of computed values against values provided by
     * <cite>Karney (2009) Test data for the transverse Mercator projection</cite> data file.
     * On the WGS84 ellipsoid we observed close to equator:
     *
     * <ul>
     *   <li>For ∆λ below 60°, errors below 1 centimetre.</li>
     *   <li>For ∆λ between 60° and 66°, errors up to 0.1 metre.</li>
     *   <li>For ∆λ between 66° and 70°, errors up to 1 metre.</li>
     *   <li>For ∆λ between 70° and 72°, errors up to 2 metres.</li>
     *   <li>For ∆λ between 72° and 74°, errors up to 10 metres.</li>
     *   <li>For ∆λ between 74° and 76°, errors up to 30 metres.</li>
     *   <li>For ∆λ between 76° and 78°, errors up to 1 kilometre.</li>
     *   <li>For ∆λ greater than 85°, errors grow exponentially.</li>
     * </ul>
     *
     * On the WGS84 ellipsoid at latitudes greater than 20°, we found errors less than 1 meter
     * for all ∆λ &lt; (1 − ℯ)⋅90° (82.63627282416406551 in WGS84 case). For larger ∆λ values
     * Karney (2009) uses an “extended” domain of transverse Mercator projection, but Apache SIS
     * does not support such extension. Consequently, ∆λ values between (1 − ℯ)⋅90° and 90° should
     * be considered invalid but are not rejected by Apache SIS. Note that even for those invalid
     * values, the reverse projection continue to gives back the original values.
     */
    private static final double DOMAIN_OF_VALIDITY = 82.63627282416406551;      // (1 − ℯ)⋅90°

    /**
     * The domain of validity at equator. We keep using the limit at all latitudes up to
     * {@value #LATITUDE_OF_REDUCED_DOMAIN} degrees, even if actually the limit could be
     * progressively relaxed until it reaches {@value #DOMAIN_OF_VALIDITY} degrees.
     */
    private static final double DOMAIN_OF_VALIDITY_AT_EQUATOR = 70;

    /**
     * The maximal latitude (exclusive) where to use {@link #DOMAIN_OF_VALIDITY_AT_EQUATOR}
     * instead of {@link #DOMAIN_OF_VALIDITY}.
     */
    private static final double LATITUDE_OF_REDUCED_DOMAIN = 20;

    /**
     * Creates a new test case.
     */
    public TransverseMercatorTest() {
    }

    /**
     * Creates a new instance of {@link TransverseMercator}.
     *
     * @param  ellipsoidal  {@code false} for a sphere, or {@code true} for WGS84 ellipsoid.
     */
    private void createNormalizedProjection(final boolean ellipsoidal, final double latitudeOfOrigin) {
        final org.apache.sis.referencing.operation.provider.TransverseMercator method =
                new org.apache.sis.referencing.operation.provider.TransverseMercator();
        final Parameters parameters = parameters(method, ellipsoidal);
        parameters.getOrCreate(LATITUDE_OF_ORIGIN).setValue(latitudeOfOrigin);
        transform = new TransverseMercator(method, parameters);
        tolerance = NORMALIZED_TOLERANCE;
        validate();
    }

    /**
     * Tests the <cite>Transverse Mercator</cite> case (EPSG:9807).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testTransverseMercator()
     */
    @Test
    public void testTransverseMercator() throws FactoryException, TransformException {
        createGeoApiTest(new org.apache.sis.referencing.operation.provider.TransverseMercator()).testTransverseMercator();
    }

    /**
     * Tests the <cite>Transverse Mercator (South Orientated)</cite> case (EPSG:9808).
     * This test is defined in GeoAPI conformance test suite.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     *
     * @see org.opengis.test.referencing.ParameterizedTransformTest#testTransverseMercatorSouthOrientated()
     */
    @Test
    public void testTransverseMercatorSouthOrientated() throws FactoryException, TransformException {
        createGeoApiTest(new TransverseMercatorSouth()).testTransverseMercatorSouthOrientated();
    }

    /**
     * Verifies the consistency of elliptical formulas with the spherical formulas.
     * This test compares the results of elliptical formulas with the spherical ones
     * for some random points.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void compareEllipticalWithSpherical() throws FactoryException, TransformException {
        createCompleteProjection(new org.apache.sis.referencing.operation.provider.TransverseMercator(),
                6371007,    // Semi-major axis length
                6371007,    // Semi-minor axis length
                0,          // Central meridian
                2.5,        // Latitude of origin
                NaN,        // Standard parallel 1 (none)
                NaN,        // Standard parallel 2 (none)
                0.997,      // Scale factor
                200,        // False easting
                100);       // False northing
        tolerance = Formulas.LINEAR_TOLERANCE;
        compareEllipticalWithSpherical(CoordinateDomain.RANGE_10, 0);
    }

    /**
     * Creates a projection and derivates a few points.
     *
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testSphericalDerivative() throws TransformException {
        createNormalizedProjection(false, 0);
        tolerance = 1E-9;

        final double delta = toRadians(100.0 / 60) / 1852;      // Approximately 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        verifyDerivative(toRadians( 0), toRadians( 0));
        verifyDerivative(toRadians(-3), toRadians(30));
        verifyDerivative(toRadians(+6), toRadians(60));
    }

    /**
     * Creates a projection and derivates a few points.
     *
     * @throws TransformException if an error occurred while projecting a point.
     */
    @Test
    public void testEllipsoidalDerivative() throws TransformException {
        createNormalizedProjection(true, 0);
        tolerance = 1E-9;

        final double delta = toRadians(100.0 / 60) / 1852;      // Approximately 100 metres.
        derivativeDeltas = new double[] {delta, delta};
        verifyDerivative(toRadians( 0), toRadians( 0));
        verifyDerivative(toRadians(-3), toRadians(30));
        verifyDerivative(toRadians(+6), toRadians(60));
    }

    /**
     * Verifies that deserialized projections work as expected. This implies that deserialization
     * recomputed the internal transient fields, especially the series expansion coefficients.
     *
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while projecting a coordinate.
     */
    @Test
    public void testSerialization() throws FactoryException, TransformException {
        createNormalizedProjection(true, 40);
        /*
         * Use a fixed seed for the random number generator in this test, because in case of failure this class will not
         * report which seed it used. This limitation exists because this test class does not extend the SIS TestCase.
         */
        final double[] source = CoordinateDomain.GEOGRAPHIC_RADIANS_HALF_λ.generateRandomInput(new Random(5346144739450824145L), 2, 10);
        final double[] target = new double[source.length];
        for (int i=0; i<source.length; i+=2) {
            // A longitude range of [-90 … +90]° is still too wide for Transverse Mercator. Reduce to [-45 … +45]°.
            source[i] /= 2;
        }
        transform.transform(source, 0, target, 0, 10);
        transform = assertSerializedEquals(transform);
        tolerance = Formulas.LINEAR_TOLERANCE;
        verifyTransform(source, target);
    }

    /**
     * Compares with <cite>Karney (2009) Test data for the transverse Mercator projection</cite>.
     * This is an optional test executed only if the {@code $SIS_DATA/Tests/TMcoords.dat} file is found.
     * The errors observed as of February 2021 are illustrated below. In this image of size 91×91 pixels,
     * pixels coordinates are longitude and latitude coordinates with (0,0) in the upper-left corner.
     * Black pixels are errors less than 0.5 meters. Yellow pixels are errors between 0.5 and 1.5 meters.
     * Red pixels are errors of 254.5 meters or more.
     *
     * <img src="doc-files/TransverseMercatorErrors.png" alt="Errors of Transverse Mercator projection">
     *
     * @throws IOException if an error occurred while reading the test file.
     * @throws FactoryException if an error occurred while creating the map projection.
     * @throws TransformException if an error occurred while transforming coordinates.
     *
     * @see OptionalTestData#TRANSVERSE_MERCATOR
     */
    @Test
    public void compareAgainstDataset() throws IOException, FactoryException, TransformException {
        try (LineNumberReader reader = OptionalTestData.TRANSVERSE_MERCATOR.reader()) {
            createCompleteProjection(new org.apache.sis.referencing.operation.provider.TransverseMercator(),
                    WGS84_A,    // Semi-major axis length
                    WGS84_B,    // Semi-minor axis length
                    0,          // Central meridian
                    0,          // Latitude of origin
                    NaN,        // Standard parallel 1 (none)
                    NaN,        // Standard parallel 2 (none)
                    0.9996,     // Scale factor
                    0,          // False easting
                    0);         // False northing
            final double[] source = new double[2];
            final double[] target = new double[2];
            String line;
            while ((line = reader.readLine()) != null) {
                final CharSequence[] split = CharSequences.split(line, ' ');
                for (int i=4; --i >= 0;) {
                    final double value = Double.parseDouble(split[i].toString());
                    if (i <= 1) source[i ^ 1] = value;                              // Swap axis order.
                    else        target[i - 2] = value;
                }
                // Relax tolerance for longitudes very far from central meridian.
                final double longitude = abs(source[0]);
                final double latitude  = abs(source[1]);
                final double limit     = (latitude < LATITUDE_OF_REDUCED_DOMAIN
                                                   ? DOMAIN_OF_VALIDITY_AT_EQUATOR
                                                   : DOMAIN_OF_VALIDITY);
                if (longitude < limit) {
                    if (latitude >= 89.9)     tolerance = 0.1;
                    else if (longitude <= 60) tolerance = Formulas.LINEAR_TOLERANCE;
                    else if (longitude <= 66) tolerance = 0.1;
                    else                      tolerance = 0.7;
                    transform.transform(source, 0, source, 0, 1);
                    assertCoordinateEquals(target, source, reader.getLineNumber(), CalculationType.DIRECT_TRANSFORM, line);
                }
            }
        }
    }
}
