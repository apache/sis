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
package org.apache.sis.xml.bind.referencing;

import java.util.Map;
import jakarta.xml.bind.JAXBException;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.datum.DefaultEllipsoid;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.xml.test.TestCase;


/**
 * Tests {@link SecondDefiningParameter}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class SecondDefiningParameterTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public SecondDefiningParameterTest() {
    }

    /**
     * XML of an ellipsoid defined by semi-major and semi-minor axes.
     * The numerical values used for this test is the ones of Clarke 1866 (EPSG:7008).
     */
    private static final String ELLIPSOID =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<gml:SecondDefiningParameter xmlns:gml=\"http://www.opengis.net/gml/3.2\">\n" +
            "  <gml:semiMinorAxis uom=\"urn:ogc:def:uom:EPSG::9001\">6356583.8</gml:semiMinorAxis>\n" +
            "</gml:SecondDefiningParameter>";

    /**
     * XML of a sphere.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-333">SIS-333</a>
     */
    private static final String SPHERE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<gml:SecondDefiningParameter xmlns:gml=\"http://www.opengis.net/gml/3.2\">\n" +
            "  <gml:isSphere>true</gml:isSphere>\n" +
            "</gml:SecondDefiningParameter>";

    /**
     * Tests marshalling of an ellipsoid.
     *
     * @throws JAXBException if an error occurred during the marshalling process.
     */
    @Test
    public void testMarshalling() throws JAXBException {
        final var ellipsoid = DefaultEllipsoid.createEllipsoid(
                Map.of(DefaultEllipsoid.NAME_KEY, "Clarke 1866"),
                6378206.4, 6356583.8, Units.METRE);
        final var sdp = new SecondDefiningParameter(ellipsoid, false);
        assertXmlEquals(ELLIPSOID, marshal(sdp), "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Tests unmarshalling of an ellipsoid.
     *
     * @throws JAXBException if an error occurred during the unmarshalling process.
     */
    @Test
    public void testUnmarshalling() throws JAXBException {
        final SecondDefiningParameter sdp = unmarshal(SecondDefiningParameter.class, ELLIPSOID);
        assertNull(sdp.isSphere);
        assertEquals(6356583.8, sdp.measure.value);
        assertEquals(Units.METRE, sdp.measure.unit);
    }

    /**
     * Tests marshalling of a sphere.
     *
     * @throws JAXBException if an error occurred during the marshalling process.
     */
    @Test
    public void testMarshallingSphere() throws JAXBException {
        final var ellipsoid = DefaultEllipsoid.createEllipsoid(
                Map.of(DefaultEllipsoid.NAME_KEY, "Sphere"),
                6371000, 6371000, Units.METRE);
        final var sdp = new SecondDefiningParameter(ellipsoid, false);
        assertXmlEquals(SPHERE, marshal(sdp), "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Tests unmarshalling of a sphere.
     *
     * @throws JAXBException if an error occurred during the unmarshalling process.
     */
    @Test
    public void testUnmarshallingSphere() throws JAXBException {
        final SecondDefiningParameter sdp = unmarshal(SecondDefiningParameter.class, SPHERE);
        assertEquals(Boolean.TRUE, sdp.isSphere);
        assertNull(sdp.measure);
    }
}
