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

import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.IllegalArgumentException;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.referencing.PositionalAccuracyConstant;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;


/**
 * Tests {@link Referencing}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
@DependsOn(TransformerTest.class)
public final strictfp class ReferencingTest extends TestCase {
    /**
     * A dummy {@code XPropertySet} instance for testing purpose.
     * The current implementation sets the value to {@code null} since {@link Referencing} ignores the properties.
     * But that value could be changed if the properties are not ignored anymore in a future SIS version.
     */
    private static final XPropertySet properties = null;

    /**
     * The instance to use for testing purpose.
     */
    private static Referencing instance;

    /**
     * Creates a {@link Referencing} instance to use for all tests.
     */
    @BeforeClass
    public static void createReferencingInstance() {
        instance = new Referencing();
        instance.setLocale(new com.sun.star.lang.Locale("en", "US", null));
    }

    /**
     * Disposes the {@link Referencing} instance after all tests completed.
     */
    @AfterClass
    public static void disposeReferencingInstance() {
        instance = null;
    }

    /**
     * Tests {@link Referencing#getName(XPropertySet, String)}.
     */
    @Test
    public void testGetName() {
        assertEquals("Using a simple code.", "WGS 84", instance.getName(properties, "EPSG:4326"));
        assertEquals("Using a URN.",         "WGS 84", instance.getName(properties, "urn:ogc:def:crs:epsg::4326"));
        assertEquals("Using a HTTP code.",   "WGS 84", instance.getName(properties, "http://www.opengis.net/gml/srs/epsg.xml#4326"));
        assertEquals("Cached value.",        "WGS 84", instance.getName(properties, "EPSG:4326"));
        assertEquals("URN for a datum.", "World Geodetic System 1984",
                instance.getName(properties, "urn:ogc:def:datum:epsg::6326"));
    }

    /**
     * Tests {@link Referencing#getAxis(XPropertySet, String, int)}.
     */
    @Test
    public void testGetAxis() {
        assertEquals("Latitude (°)",              instance.getAxis(properties, "EPSG:4326", 1));
        assertEquals("Longitude (°)",             instance.getAxis(properties, "EPSG:4326", 2));
        assertEquals("Index 3 is out of bounds.", instance.getAxis(properties, "EPSG:4326", 3));
        assertEquals("Expected “urn:ogc:def:datum:epsg::6326” to reference an instance of ‘CoordinateReferenceSystem’, " +
                "but found an instance of ‘GeodeticDatum’.", instance.getAxis(properties, "urn:ogc:def:datum:epsg::6326", 1));
    }

    /**
     * Tests {@link Referencing#getGeographicArea(XPropertySet, String)}.
     */
    @Test
    public void testGetGeographicArea() {
        final double[][] bbox = instance.getGeographicArea(properties, "EPSG:32620");     // UTM zone 20
        assertEquals(2, bbox.length);
        assumeFalse(Double.isNaN(bbox[0][0]));    // NaN if EPSG dataset is not installed.
        assertArrayEquals("Upper left corner",  new double[] {84, -66}, bbox[0], STRICT);
        assertArrayEquals("Lower right corner", new double[] { 0, -60}, bbox[1], STRICT);
    }

    /**
     * Tests {@link Referencing#getAccuracy(XPropertySet, String, String, double[][])}.
     * This test asks for a transformation from NAD83 to WGS84.
     */
    @Test
    public void testGetAccuracy() {
        final double accuracy = instance.getAccuracy(properties, "EPSG:4269", "EPSG:4326", new double[][] {
            {37.783, -122.417},     // San-Francisco
            {40.713,  -74.006},     // New-York
            {34.050, -118.250},     // Los Angeles
            {29.763,  -95.383},     // Houston
            {41.837,  -87.685},     // Chicago
            {25.775,  -80.209},     // Miami
        });
        assertTrue("Accuracy = " + accuracy,
                accuracy > Formulas.LINEAR_TOLERANCE &&
                accuracy <= PositionalAccuracyConstant.UNKNOWN_ACCURACY);
    }

    /**
     * Tests {@link Referencing#transformCoordinates(XPropertySet, String, String, double[][])}.
     */
    @Test
    public void testTransformCoordinates() {
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
                instance.transformCoordinates(properties, "EPSG:4979", "EPSG:4326", points), STRICT);
    }

    /**
     * Tests {@link Referencing#parseAngle(XPropertySet, String, Object)}.
     *
     * @throws IllegalArgumentException if the pattern used by the test is not a string or void.
     */
    @Test
    public void testParseAngle() throws IllegalArgumentException {
        assertEquals(43.50, instance.parseAngle(properties, "43°30'", "D°MM.m'"), STRICT);
        assertEquals(43.50, instance.parseAngle(properties, "4330",   "DMM"),     STRICT);
        assertEquals(-3.25, instance.parseAngle(properties, "-3°15'", "D°MM.m'"), STRICT);
    }

    /**
     * Tests {@link Referencing#formatAngle(XPropertySet, String, Object)}.
     *
     * @throws IllegalArgumentException if the pattern used by the test is not a string or void.
     */
    @Test
    public void testFormatAngle() throws IllegalArgumentException {
        assertEquals("43°30.0'", instance.formatAngle(properties, 43.50, "D°MM.m'"));
        assertEquals("4330",     instance.formatAngle(properties, 43.50, "DMM"));
        assertEquals("-3°15.0'", instance.formatAngle(properties, -3.25, "D°MM.m'"));
    }
}
