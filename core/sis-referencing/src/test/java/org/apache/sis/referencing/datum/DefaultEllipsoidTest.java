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

import javax.xml.bind.JAXBException;
import org.apache.sis.measure.Units;
import org.apache.sis.test.xml.TestCase;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;
import static org.apache.sis.referencing.Assertions.assertWktEquals;


/**
 * Tests the {@link DefaultEllipsoid} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.4
 * @since   0.4
 */
@DependsOn({
    org.apache.sis.internal.referencing.FormulasTest.class,
    org.apache.sis.internal.jaxb.referencing.SecondDefiningParameterTest.class
})
public final class DefaultEllipsoidTest extends TestCase {
    /**
     * An XML file in this package containing an ellipsoid definition.
     */
    private static final String ELLIPSOID_FILE = "Ellipsoid.xml";

    /**
     * An XML file in this package containing a sphere definition.
     */
    private static final String SPHERE_FILE = "Sphere.xml";

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
     * Tests {@link DefaultEllipsoid#getRadius(double)}.
     */
    @Test
    public void testRadius() {
        final DefaultEllipsoid e = DefaultEllipsoid.castOrCopy(GeodeticDatumMock.WGS84.getEllipsoid());
        assertEquals(6378137, e.getRadius( 0),  0.5);
        assertEquals(6372824, e.getRadius( 30), 0.5);
        assertEquals(6356752, e.getRadius(+90), 0.5);
        assertEquals(6356752, e.getRadius(-90), 0.5);
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
     * Tests unmarshalling and marshalling of an ellipsoid.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testEllipsoidXML() throws JAXBException {
        final DefaultEllipsoid ellipsoid = unmarshalFile(DefaultEllipsoid.class, ELLIPSOID_FILE);
        assertEquals("name", "Clarke 1880 (international foot)", ellipsoid.getName().getCode());
        assertEquals("remarks", "Definition in feet assumed to be international foot.", ellipsoid.getRemarks().toString());
        assertFalse ("isSphere",                              ellipsoid.isSphere());
        assertFalse ("isIvfDefinitive",                       ellipsoid.isIvfDefinitive());
        assertEquals("semiMajorAxis",     20926202,           ellipsoid.getSemiMajorAxis(), STRICT);
        assertEquals("semiMinorAxis",     20854895,           ellipsoid.getSemiMinorAxis(), STRICT);
        assertEquals("inverseFlattening", 293.46630765562986, ellipsoid.getInverseFlattening(), 1E-12);
        assertEquals("axisUnit",          Units.FOOT,         ellipsoid.getAxisUnit());
        /*
         * Marshal and compare to the original file.
         */
        assertMarshalEqualsFile(ELLIPSOID_FILE, ellipsoid, "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Tests unmarshalling and marshalling of a sphere.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-333">SIS-333</a>
     *
     * @since 0.8
     */
    @Test
    public void testSphereXML() throws JAXBException {
        final DefaultEllipsoid ellipsoid = unmarshalFile(DefaultEllipsoid.class, SPHERE_FILE);
        assertEquals("name", "GRS 1980 Authalic Sphere", ellipsoid.getName().getCode());
        assertEquals("remarks", "Authalic sphere derived from GRS 1980 ellipsoid (code 7019).", ellipsoid.getRemarks().toString());
        assertTrue  ("isSphere",                                    ellipsoid.isSphere());
        assertFalse ("isIvfDefinitive",                             ellipsoid.isIvfDefinitive());
        assertEquals("semiMajorAxis",     6371007,                  ellipsoid.getSemiMajorAxis(), STRICT);
        assertEquals("semiMinorAxis",     6371007,                  ellipsoid.getSemiMinorAxis(), STRICT);
        assertEquals("inverseFlattening", Double.POSITIVE_INFINITY, ellipsoid.getInverseFlattening(), STRICT);
        assertEquals("axisUnit",          Units.METRE,              ellipsoid.getAxisUnit());
        /*
         * Marshal and compare to the original file.
         */
        assertMarshalEqualsFile(SPHERE_FILE, ellipsoid, "xmlns:*", "xsi:schemaLocation");
    }
}
