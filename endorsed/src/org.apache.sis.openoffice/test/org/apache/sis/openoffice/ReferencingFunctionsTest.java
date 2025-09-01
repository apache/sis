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
package org.apache.sis.openoffice;

import com.sun.star.lang.IllegalArgumentException;
import org.apache.sis.referencing.privy.Formulas;
import org.apache.sis.referencing.internal.PositionalAccuracyConstant;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.Assertions;


/**
 * Tests {@link ReferencingFunctions}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class ReferencingFunctionsTest extends TestCase {
    /**
     * The instance to use for testing purpose.
     */
    private final ReferencingFunctions instance;

    /**
     * Creates a {@link ReferencingFunctions} instance to use for all tests.
     */
    public ReferencingFunctionsTest() {
        instance = new ReferencingFunctions(null);
        instance.setLocale(new com.sun.star.lang.Locale("en", "US", null));
    }

    /**
     * Verifies the service and implementation names.
     */
    @Test
    public void verifyNames() {
        assertEquals(ReferencingFunctions.class.getName(), ReferencingFunctions.IMPLEMENTATION_NAME);
        assertTrue(ReferencingFunctions.IMPLEMENTATION_NAME.startsWith(ReferencingFunctions.SERVICE_NAME));
    }

    /**
     * Tests {@link ReferencingFunctions#getName(String)}.
     */
    @Test
    public void testGetName() {
        assertEquals("WGS 84", instance.getName("EPSG:4326"));
        assertEquals("WGS 84", instance.getName("urn:ogc:def:crs:epsg::4326"));
        assertEquals("WGS 84", instance.getName("http://www.opengis.net/gml/srs/epsg.xml#4326"));
        assertEquals("WGS 84", instance.getName("EPSG:4326"));
        Assertions.assertLegacyEquals("World Geodetic System 1984", instance.getName("urn:ogc:def:datum:epsg::6326"));
    }

    /**
     * Tests {@link ReferencingFunctions#getScope(String)}.
     * The scope text depends on the availability of an EPSG database,
     * so this method tests only that the function returns a non-null value which may be "unknown".
     */
    @Test
    public void testGetScope() {
        final String scope = instance.getScope("EPSG:4326");
        assertNotNull(scope);
    }

    /**
     * Tests {@link ReferencingFunctions#getAxis(String, int)}.
     */
    @Test
    public void testGetAxis() {
        assertEquals("Latitude (°)",              instance.getAxis("EPSG:4326", 1));
        assertEquals("Longitude (°)",             instance.getAxis("EPSG:4326", 2));
        assertEquals("Index 3 is out of bounds.", instance.getAxis("EPSG:4326", 3));
        Assertions.assertLegacyEquals("Expected “urn:ogc:def:datum:epsg::6326” to reference an instance of ‘CoordinateReferenceSystem’, " +
                "but found an instance of ‘GeodeticDatum’.", instance.getAxis("urn:ogc:def:datum:epsg::6326", 1));
    }

    /**
     * Tests {@link ReferencingFunctions#getDomainOfValidity(String)}.
     */
    @Test
    public void testGetDomainOfValidity() {
        final String domain = instance.getDomainOfValidity("EPSG:4326");
        assertTrue(domain.contains("World"), domain);
    }

    /**
     * Tests {@link ReferencingFunctions#getGeographicArea(String)}.
     */
    @Test
    public void testGetGeographicArea() {
        final double[][] bbox = instance.getGeographicArea("EPSG:32620");     // UTM zone 20
        assumeFalse(bbox.length == 0);            // Empty if EPSG dataset is not installed.
        assertEquals(2, bbox.length);
        assertArrayEquals(new double[] {84, -66}, bbox[0]);
        assertArrayEquals(new double[] { 0, -60}, bbox[1]);
    }

    /**
     * Tests {@link ReferencingFunctions#getAccuracy(String, String, double[][])}.
     * This test asks for a transformation from NAD83 to WGS84.
     *
     * @throws IllegalArgumentException if {@code points} is not a {@code double[][]} value or void.
     */
    @Test
    public void testGetAccuracy() throws IllegalArgumentException {
        final double accuracy = instance.getAccuracy("EPSG:4269", "EPSG:4326", new double[][] {
            {37.783, -122.417},     // San-Francisco
            {40.713,  -74.006},     // New-York
            {34.050, -118.250},     // Los Angeles
            {29.763,  -95.383},     // Houston
            {41.837,  -87.685},     // Chicago
            {25.775,  -80.209},     // Miami
        });
        assertTrue(accuracy > Formulas.LINEAR_TOLERANCE &&
                   accuracy <= PositionalAccuracyConstant.UNKNOWN_ACCURACY);
    }

    /**
     * Tests {@link ReferencingFunctions#transformPoints(String, String, double[][])}.
     */
    @Test
    public void testTransformPoints() {
        final double[][] points = {
            new double[] {30,  20,  4},
            new double[] {34,  17, -3},
            new double[] {27, -12, 12},
            new double[] {32,  23, -1}
        };
        final double[][] result = {
            new double[] {30,  20},
            new double[] {34,  17},
            new double[] {27, -12},
            new double[] {32,  23}
        };
        TransformerTest.assertPointsEqual(result,
                instance.transformPoints("EPSG:4979", "EPSG:4326", points), STRICT);
    }

    /**
     * Tests {@link ReferencingFunctions#transformEnvelope(String, String, double[][])}.
     */
    @Test
    public void testTransformEnvelope() {
        final double[][] points = {
            new double[] {30,  20,  4},
            new double[] {34,  17, -3},
            new double[] {27, -12, 12},
            new double[] {32,  23, -1}
        };
        final double[][] result = {
            new double[] {27, -12},
            new double[] {34,  23}
        };
        TransformerTest.assertPointsEqual(result,
                instance.transformEnvelope("EPSG:4979", "EPSG:4326", points), STRICT);
    }

    /**
     * Tests {@link ReferencingFunctions#parseAngle(String, Object)}.
     *
     * @throws IllegalArgumentException if the pattern used by the test is not a string or void.
     */
    @Test
    public void testParseAngle() throws IllegalArgumentException {
        assertEquals(43.50, singleton(instance.parseAngle(new String[][] {{"43°30'"}}, "D°MM.m'", "en")));
        assertEquals(43.50, singleton(instance.parseAngle(new String[][] {{"4330"}},   "DMM",     "en")));
        assertEquals(-3.25, singleton(instance.parseAngle(new String[][] {{"-3°15'"}}, "D°MM.m'", "en")));
    }

    /**
     * Tests {@link ReferencingFunctions#formatAngle(String, Object)}.
     *
     * @throws IllegalArgumentException if the pattern used by the test is not a string or void.
     */
    @Test
    public void testFormatAngle() throws IllegalArgumentException {
        assertEquals("43°30.0'", singleton(instance.formatAngle(new double[][] {{43.50}}, "D°MM.m'", "en")));
        assertEquals("4330",     singleton(instance.formatAngle(new double[][] {{43.50}}, "DMM",     "en")));
        assertEquals("-3°15.0'", singleton(instance.formatAngle(new double[][] {{-3.25}}, "D°MM.m'", "en")));
    }

    /**
     * Ensures that the given array contains exactly one element and returns that element.
     */
    private static double singleton(final double[][] value) {
        assertEquals(1, value.length);
        assertEquals(1, value[0].length);
        return value[0][0];
    }

    /**
     * Ensures that the given array contains exactly one element and returns that element.
     */
    private static String singleton(final String[][] value) {
        assertEquals(1, value.length);
        assertEquals(1, value[0].length);
        return value[0][0];
    }
}
