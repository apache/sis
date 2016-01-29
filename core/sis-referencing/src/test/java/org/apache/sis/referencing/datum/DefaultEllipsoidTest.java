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
package org.apache.sis.referencing.datum;

import java.util.Random;
import javax.xml.bind.JAXBException;
import javax.measure.unit.NonSI;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.DependsOnMethod;
import org.junit.Test;

import static java.lang.StrictMath.*;
import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests the {@link DefaultEllipsoid} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
@DependsOn({
    org.apache.sis.internal.referencing.FormulasTest.class,
    org.apache.sis.internal.jaxb.referencing.SecondDefiningParameterTest.class
})
public final strictfp class DefaultEllipsoidTest extends XMLTestCase {
    /**
     * An XML file in this package containing an ellipsoid definition.
     */
    private static final String XML_FILE = "Ellipsoid.xml";

    /**
     * Half of a minute of angle, in degrees.
     */
    private static final double HM = 0.5 / 60;

    /**
     * Tolerances in metres for the tests using on spheres.
     * Those tests are usually more accurate than the tests on ellipsoid.
     */
    private static final double SPHERICAL_TOLERANCE = 0.001;

    /**
     * Returns a random longitude (in degrees) using the given random number generator.
     */
    private static double nextLongitude(final Random random) {
        return (Longitude.MAX_VALUE - Longitude.MIN_VALUE) * random.nextDouble() + Longitude.MIN_VALUE;
    }

    /**
     * Tests {@link DefaultEllipsoid#getEccentricity()}.
     *
     * @since 0.7
     */
    @Test
    public void testGetEccentricity() {
        final DefaultEllipsoid e = new DefaultEllipsoid(GeodeticDatumMock.WGS84.getEllipsoid());
        assertEquals("semiMajorAxis",       6378137.0,            e.getSemiMajorAxis(),       STRICT);   // By definition
        assertEquals("inverseFlattening",   298.257223563,        e.getInverseFlattening(),   STRICT);   // By definition
        assertEquals("eccentricitySquared", 0.006694379990141317, e.getEccentricitySquared(), STRICT);
        assertEquals("eccentricity",        0.0818191908426215,   e.getEccentricity(),        STRICT);
    }

    /**
     * Tests {@link DefaultEllipsoid#semiMajorAxisDifference(Ellipsoid)}. This test uses the data provided
     * in §2.4.4.2 of IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2 – April 2015.
     *
     * @since 0.7
     */
    @Test
    public void testSemiMajorAxisDifference() {
        final DefaultEllipsoid e = new DefaultEllipsoid(GeodeticDatumMock.WGS84.getEllipsoid());
        assertEquals("semiMajorAxisDifference",   0, e.semiMajorAxisDifference(GeodeticDatumMock.WGS84.getEllipsoid()), STRICT);
        assertEquals("semiMajorAxisDifference", 251, e.semiMajorAxisDifference(GeodeticDatumMock.ED50 .getEllipsoid()), STRICT);
    }

    /**
     * Tests {@link DefaultEllipsoid#flatteningDifference(Ellipsoid)}. This test uses the data provided
     * in §2.4.4.2 of IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2 – April 2015.
     *
     * @since 0.7
     */
    @Test
    public void testFlatteningDifference() {
        final DefaultEllipsoid e = new DefaultEllipsoid(GeodeticDatumMock.WGS84.getEllipsoid());
        assertEquals("flatteningDifference", 0.0,         e.flatteningDifference(GeodeticDatumMock.WGS84.getEllipsoid()), STRICT);
        assertEquals("flatteningDifference", 1.41927E-05, e.flatteningDifference(GeodeticDatumMock.ED50 .getEllipsoid()), 1E-10);
    }

    /**
     * Tests the orthodromic distances computed by {@link DefaultEllipsoid}. There is actually two algorithms:
     * one for the ellipsoidal model, and a simpler one for spherical model. This method tests the ellipsoidal
     * model using known values of nautical mile at different latitude.
     *
     * <p>This method performs the test on the Clark 1866 ellipsoid, which was the basis for the Imperial and
     * U.S. definitions prior the First International Extraordinary Hydrographic Conference in Monaco (1929).</p>
     */
    @Test
    public void testOrthodromicDistance() {
        final DefaultEllipsoid e = new DefaultEllipsoid(GeodeticDatumMock.NAD27.getEllipsoid()); // Clark 1866
        assertEquals("Nautical mile at equator",    1842.78, e.orthodromicDistance(0,    -HM,   0,    +HM), 0.01);
        assertEquals("Nautical mile at North pole", 1861.67, e.orthodromicDistance(0,  90-HM*2, 0,  90   ), 0.02);
        assertEquals("Nautical mile at South pole", 1861.67, e.orthodromicDistance(0, -90+HM*2, 0, -90   ), 0.02);
        assertEquals("International nautical mile", 1852.00, e.orthodromicDistance(0,  45-HM,   0,  45+HM), 0.20);
        /*
         * Test parallel segments of increasing length at random positions on the equator.
         */
        final Random random = TestUtilities.createRandomNumberGenerator();
        final double semiMajor = e.getSemiMajorAxis();
        for (double length = 0; length <= Longitude.MAX_VALUE; length += 0.5) {
            final double λ = nextLongitude(random);
            assertEquals(semiMajor * toRadians(length), e.orthodromicDistance(λ, 0, λ+length, 0), 0.2);
        }
    }

    /**
     * Tests the orthodromic distances computed by {@link DefaultEllipsoid} on a sphere,
     * and compares them with the distances computed by {@link Sphere}.
     */
    @Test
    @DependsOnMethod("testOrthodromicDistance")
    public void testOrthodromicDistanceOnSphere() {
        /*
         * Creates instance of DefaultEllipsoid and Sphere with the same properties.
         * Those instances will use different formulas for orthodromic distances, which we will compare.
         */
        final DefaultEllipsoid e = new DefaultEllipsoid(GeodeticDatumMock.SPHERE.getEllipsoid());
        final double radius = e.getSemiMajorAxis();
        final Sphere s = new Sphere(IdentifiedObjects.getProperties(e), radius, false, e.getAxisUnit());
        assertTrue(e.isSphere());
        assertTrue(s.isSphere());
        /*
         * Test parallel segments of increasing length at random positions on the equator.
         */
        final Random random = TestUtilities.createRandomNumberGenerator();
        for (double length = 0; length <= Longitude.MAX_VALUE; length += 0.5) {
            final double λ = nextLongitude(random);
            final double distance = radius * toRadians(length);
            assertEquals(distance, s.orthodromicDistance(λ, 0, λ+length, 0), SPHERICAL_TOLERANCE);
            assertEquals(distance, e.orthodromicDistance(λ, 0, λ+length, 0), SPHERICAL_TOLERANCE);
        }
        /*
         * Test meridian segments from equator to increasing latitudes.
         */
        for (double φ = Latitude.MIN_VALUE; φ <= Latitude.MAX_VALUE; φ += 0.5) {
            final double λ = nextLongitude(random);
            final double distance = radius * toRadians(abs(φ));
            assertEquals(distance, s.orthodromicDistance(λ, 0, λ, φ), SPHERICAL_TOLERANCE);
            assertEquals(distance, e.orthodromicDistance(λ, 0, λ, φ), SPHERICAL_TOLERANCE);
        }
        /*
         * Tests random segments.
         */
        final double circumference = (radius * (1 + 1E-8)) * (2*PI);
        for (int i=0; i<100; i++) {
            final double φ1 =  -90 + 180*random.nextDouble();
            final double φ2 =  -90 + 180*random.nextDouble();
            final double λ1 = -180 + 360*random.nextDouble();
            final double λ2 = -180 + 360*random.nextDouble();
            final double distance = s.orthodromicDistance(λ1, φ1, λ2, φ2);
            assertTrue(distance >= 0 && distance <= circumference);
            assertEquals(distance, e.orthodromicDistance(λ1, φ1, λ2, φ2), SPHERICAL_TOLERANCE);
        }
    }

    /**
     * Tests the {@link DefaultEllipsoid#getAuthalicRadius()} method on the GRS 1980 ellipsoid (EPSG:7019).
     * The expected result is the radius of the sphere defined by EPSG:7048.
     */
    @Test
    public void testAuthalicRadius() {
        final DefaultEllipsoid sphere = DefaultEllipsoid.castOrCopy(GeodeticDatumMock.SPHERE.getEllipsoid());
        final DefaultEllipsoid GRS80  = DefaultEllipsoid.castOrCopy(GeodeticDatumMock.NAD83 .getEllipsoid());
        assertInstanceOf("SPHERE", Sphere.class, sphere);
        assertTrue  ("SPHERE", sphere.isSphere());
        assertFalse ("GRS80",  GRS80 .isSphere());
        assertEquals("SPHERE", 6371007, sphere.getAuthalicRadius(), 0.0);
        assertEquals("GRS80",  6371007, GRS80 .getAuthalicRadius(), 0.2);
    }

    /**
     * Tests {@link DefaultEllipsoid#toWKT()}.
     */
    @Test
    public void testToWKT() {
        final DefaultEllipsoid e = new DefaultEllipsoid(GeodeticDatumMock.WGS84.getEllipsoid());
        assertWktEquals("ELLIPSOID[“WGS84”, 6378137.0, 298.257223563, LENGTHUNIT[“metre”, 1]]", e);
    }

    /**
     * Tests unmarshalling and marshalling.
     *
     * @throws JAXBException If an error occurred during (un)marshalling.
     */
    @Test
    public void testXML() throws JAXBException {
        final DefaultEllipsoid ellipsoid = unmarshalFile(DefaultEllipsoid.class, XML_FILE);
        assertEquals("name", "Clarke 1880 (international foot)", ellipsoid.getName().getCode());
        assertEquals("remarks", "Definition in feet assumed to be international foot.", ellipsoid.getRemarks().toString());
        assertFalse ("isIvfDefinitive",                       ellipsoid.isIvfDefinitive());
        assertEquals("semiMajorAxis",     20926202,           ellipsoid.getSemiMajorAxis(), 0);
        assertEquals("semiMinorAxis",     20854895,           ellipsoid.getSemiMinorAxis(), 0);
        assertEquals("inverseFlattening", 293.46630765562986, ellipsoid.getInverseFlattening(), 1E-12);
        assertEquals("axisUnit",          NonSI.FOOT,         ellipsoid.getAxisUnit());
        /*
         * Marshall and compare to the original file.
         */
        assertMarshalEqualsFile(XML_FILE, ellipsoid, "xlmns:*", "xsi:schemaLocation");
    }
}
