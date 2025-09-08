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

import java.io.InputStream;
import jakarta.xml.bind.JAXBException;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.xml.test.TestCase;
import static org.apache.sis.referencing.Assertions.assertWktEquals;
import static org.apache.sis.referencing.Assertions.assertRemarksEquals;


/**
 * Tests the {@link DefaultEllipsoid} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public final class DefaultEllipsoidTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultEllipsoidTest() {
    }

    /**
     * Opens the stream to the XML file in this package containing an ellipsoid or sphere definition.
     *
     * @param  sphere  {@code true} for a sphere or {@code false} for an ellipsoid.
     * @return stream opened on the XML document to use for testing purpose.
     */
    private static InputStream openTestFile(final boolean sphere) {
        // Call to `getResourceAsStream(…)` is caller sensitive: it must be in the same module.
        return DefaultEllipsoidTest.class.getResourceAsStream(sphere ? "Sphere.xml" : "Ellipsoid.xml");
    }

    /**
     * Tests {@link DefaultEllipsoid#getEccentricity()}.
     */
    @Test
    public void testGetEccentricity() {
        final DefaultEllipsoid e = new DefaultEllipsoid(GeodeticDatumMock.WGS84.getEllipsoid());
        assertEquals(6378137.0,            e.getSemiMajorAxis());           // By definition
        assertEquals(298.257223563,        e.getInverseFlattening());       // By definition
        assertEquals(0.006694379990141317, e.getEccentricitySquared());
        assertEquals(0.0818191908426215,   e.getEccentricity());
    }

    /**
     * Tests {@link DefaultEllipsoid#semiMajorAxisDifference(Ellipsoid)}. This test uses the data provided
     * in §2.4.4.2 of IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2 – April 2015.
     */
    @Test
    public void testSemiMajorAxisDifference() {
        final DefaultEllipsoid e = new DefaultEllipsoid(GeodeticDatumMock.WGS84.getEllipsoid());
        assertEquals(  0, e.semiMajorAxisDifference(GeodeticDatumMock.WGS84.getEllipsoid()));
        assertEquals(251, e.semiMajorAxisDifference(GeodeticDatumMock.ED50 .getEllipsoid()));
    }

    /**
     * Tests {@link DefaultEllipsoid#flatteningDifference(Ellipsoid)}. This test uses the data provided
     * in §2.4.4.2 of IOGP Publication 373-7-2 – Geomatics Guidance Note number 7, part 2 – April 2015.
     */
    @Test
    public void testFlatteningDifference() {
        final DefaultEllipsoid e = new DefaultEllipsoid(GeodeticDatumMock.WGS84.getEllipsoid());
        assertEquals(0.0,         e.flatteningDifference(GeodeticDatumMock.WGS84.getEllipsoid()));
        assertEquals(1.41927E-05, e.flatteningDifference(GeodeticDatumMock.ED50 .getEllipsoid()), 1E-10);
    }

    /**
     * Tests the {@link DefaultEllipsoid#getAuthalicRadius()} method on the GRS 1980 ellipsoid (EPSG:7019).
     * The expected result is the radius of the sphere defined by EPSG:7048.
     */
    @Test
    public void testAuthalicRadius() {
        final DefaultEllipsoid sphere = DefaultEllipsoid.castOrCopy(GeodeticDatumMock.SPHERE.getEllipsoid());
        final DefaultEllipsoid GRS80  = DefaultEllipsoid.castOrCopy(GeodeticDatumMock.NAD83 .getEllipsoid());
        assertInstanceOf(Sphere.class, sphere);
        assertTrue  (sphere.isSphere());
        assertFalse (GRS80 .isSphere());
        assertEquals(6371007, sphere.getAuthalicRadius());
        assertEquals(6371007, GRS80 .getAuthalicRadius(), 0.2);
    }

    /**
     * Tests {@link DefaultEllipsoid#getGeocentricRadius(double)}.
     */
    @Test
    public void testGeocentricRadius() {
        final DefaultEllipsoid e = DefaultEllipsoid.castOrCopy(GeodeticDatumMock.WGS84.getEllipsoid());
        assertEquals(6378137, e.getGeocentricRadius( 0),  0.5);
        assertEquals(6372824, e.getGeocentricRadius( 30), 0.5);
        assertEquals(6356752, e.getGeocentricRadius(+90), 0.5);
        assertEquals(6356752, e.getGeocentricRadius(-90), 0.5);
    }

    /**
     * Tests {@link DefaultEllipsoid#toWKT()}.
     */
    @Test
    public void testToWKT() {
        final var e = new DefaultEllipsoid(GeodeticDatumMock.WGS84.getEllipsoid());
        assertWktEquals(Convention.WKT2, "ELLIPSOID[“WGS84”, 6378137.0, 298.257223563, LENGTHUNIT[“metre”, 1]]", e);
    }

    /**
     * Tests unmarshalling and marshalling of an ellipsoid.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testEllipsoidXML() throws JAXBException {
        final DefaultEllipsoid ellipsoid = unmarshalFile(DefaultEllipsoid.class, openTestFile(false));
        assertEquals("Clarke 1880 (international foot)", ellipsoid.getName().getCode());
        assertRemarksEquals("Definition in feet assumed to be international foot.", ellipsoid, null);
        assertFalse (                    ellipsoid.isSphere());
        assertFalse (                    ellipsoid.isIvfDefinitive());
        assertEquals(20926202,           ellipsoid.getSemiMajorAxis());
        assertEquals(20854895,           ellipsoid.getSemiMinorAxis());
        assertEquals(293.46630765562986, ellipsoid.getInverseFlattening(), 1E-12);
        assertEquals(Units.FOOT,         ellipsoid.getAxisUnit());
        /*
         * Marshal and compare to the original file.
         */
        assertMarshalEqualsFile(openTestFile(false), ellipsoid, "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Tests unmarshalling and marshalling of a sphere.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-333">SIS-333</a>
     */
    @Test
    public void testSphereXML() throws JAXBException {
        final DefaultEllipsoid ellipsoid = unmarshalFile(DefaultEllipsoid.class, openTestFile(true));
        assertEquals("GRS 1980 Authalic Sphere", ellipsoid.getName().getCode());
        assertRemarksEquals("Authalic sphere derived from GRS 1980 ellipsoid (code 7019).", ellipsoid, null);
        assertTrue  (                          ellipsoid.isSphere());
        assertFalse (                          ellipsoid.isIvfDefinitive());
        assertEquals(6371007,                  ellipsoid.getSemiMajorAxis());
        assertEquals(6371007,                  ellipsoid.getSemiMinorAxis());
        assertEquals(Double.POSITIVE_INFINITY, ellipsoid.getInverseFlattening());
        assertEquals(Units.METRE,              ellipsoid.getAxisUnit());
        /*
         * Marshal and compare to the original file.
         */
        assertMarshalEqualsFile(openTestFile(true), ellipsoid, "xmlns:*", "xsi:schemaLocation");
    }
}
