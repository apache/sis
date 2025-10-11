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
package org.apache.sis.referencing.crs;

import java.util.Map;
import jakarta.xml.bind.JAXBException;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.referencing.datum.DefaultEngineeringDatum;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.xml.Namespaces;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.xml.test.TestCase;
import org.apache.sis.referencing.cs.HardCodedCS;
import static org.apache.sis.referencing.Assertions.assertWktEquals;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import static org.opengis.test.Assertions.assertAxisDirectionsEqual;


/**
 * Tests {@link DefaultEngineeringCRS}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class DefaultEngineeringCRSTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultEngineeringCRSTest() {
    }

    /**
     * Creates an engineering CRS using a two-dimensional Cartesian coordinate system.
     */
    private static DefaultEngineeringCRS createCartesian() {
        return new DefaultEngineeringCRS(Map.of(DefaultEngineeringCRS.NAME_KEY, "A construction site CRS"),
                new DefaultEngineeringDatum(Map.of(DefaultEngineeringDatum.NAME_KEY, "P1")), null,
                HardCodedCS.CARTESIAN_2D);
    }

    /**
     * Creates an engineering CRS using a three-dimensional Spherical coordinate system.
     */
    private static DefaultEngineeringCRS createSpherical() {
        return new DefaultEngineeringCRS(Map.of(DefaultEngineeringCRS.NAME_KEY, "A spherical CRS"),
                new DefaultEngineeringDatum(Map.of(DefaultEngineeringDatum.NAME_KEY, "Centre")), null,
                HardCodedCS.SPHERICAL);
    }

    /**
     * Tests WKT 1 formatting.
     */
    @Test
    public void testWKT1() {
        final DefaultEngineeringCRS crs = createCartesian();
        assertWktEquals(Convention.WKT1,
                "LOCAL_CS[“A construction site CRS”,\n" +
                "  LOCAL_DATUM[“P1”, 0],\n" +
                "  UNIT[“metre”, 1],\n" +
                "  AXIS[“x”, EAST],\n" +
                "  AXIS[“y”, NORTH]]",
                crs);
    }

    /**
     * Tests WKT 2 formatting.
     */
    @Test
    public void testWKT2() {
        final DefaultEngineeringCRS crs = createSpherical();
        assertWktEquals(Convention.WKT2_2015,
                "ENGCRS[“A spherical CRS”,\n" +
                "  EDATUM[“Centre”],\n" +
                "  CS[spherical, 3],\n" +
                "    AXIS[“Spherical latitude (U)”, north, ORDER[1], ANGLEUNIT[“degree”, 0.017453292519943295]],\n" +
                "    AXIS[“Spherical longitude (V)”, east, ORDER[2], ANGLEUNIT[“degree”, 0.017453292519943295]],\n" +
                "    AXIS[“Geocentric radius (r)”, up, ORDER[3], LENGTHUNIT[“metre”, 1]]]",
                crs);
    }

    /**
     * Tests WKT 2 "simplified" formatting.
     */
    @Test
    public void testWKT2_Simplified() {
        final DefaultEngineeringCRS crs = createSpherical();
        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "EngineeringCRS[“A spherical CRS”,\n" +
                "  EngineeringDatum[“Centre”],\n" +
                "  CS[spherical, 3],\n" +
                "    Axis[“Spherical latitude (U)”, north, Unit[“degree”, 0.017453292519943295]],\n" +
                "    Axis[“Spherical longitude (V)”, east, Unit[“degree”, 0.017453292519943295]],\n" +
                "    Axis[“Geocentric radius (r)”, up, Unit[“metre”, 1]]]",
                crs);
    }

    /**
     * Tests XML (un)marshalling of an engineering CRS using a Cartesian CS.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testCartesianXML() throws JAXBException {
        final String xml = marshal(createCartesian());
        assertXmlEquals(
                "<gml:EngineeringCRS xmlns:gml=\"" + Namespaces.GML + "\">\n" +
                "  <gml:name>A construction site CRS</gml:name>\n" +
                "  <gml:cartesianCS>\n" +
                "    <gml:CartesianCS gml:id=\"Cartesian2D\">\n" +
                "      <gml:name>Cartesian 2D</gml:name>\n" +
                "      <gml:axis>\n" +
                "        <gml:CoordinateSystemAxis uom=\"urn:ogc:def:uom:EPSG::9001\" gml:id=\"x\">\n" +
                "          <gml:name>x</gml:name>\n" +
                "          <gml:axisAbbrev>x</gml:axisAbbrev>\n" +
                "          <gml:axisDirection codeSpace=\"EPSG\">east</gml:axisDirection>\n" +
                "        </gml:CoordinateSystemAxis>\n" +
                "      </gml:axis>\n" +
                "      <gml:axis>\n" +
                "        <gml:CoordinateSystemAxis uom=\"urn:ogc:def:uom:EPSG::9001\" gml:id=\"y\">\n" +
                "          <gml:name>y</gml:name>\n" +
                "          <gml:axisAbbrev>y</gml:axisAbbrev>\n" +
                "          <gml:axisDirection codeSpace=\"EPSG\">north</gml:axisDirection>\n" +
                "        </gml:CoordinateSystemAxis>\n" +
                "      </gml:axis>\n" +
                "    </gml:CartesianCS>\n" +
                "  </gml:cartesianCS>\n" +
                "  <gml:engineeringDatum>\n" +
                "    <gml:EngineeringDatum gml:id=\"P1\">\n" +
                "      <gml:name>P1</gml:name>\n" +
                "    </gml:EngineeringDatum>\n" +
                "  </gml:engineeringDatum>\n" +
                "</gml:EngineeringCRS>",
                xml, "xmlns:*");

        final DefaultEngineeringCRS crs = unmarshal(DefaultEngineeringCRS.class, xml);
        assertEquals("A construction site CRS", crs.getName().getCode());
        assertEquals("P1", crs.getDatum().getName().getCode());

        final CoordinateSystem cs = crs.getCoordinateSystem();
        assertInstanceOf(CartesianCS.class, cs);
        assertEquals("Cartesian 2D", cs.getName().getCode());
        assertEquals(2, cs.getDimension());
        assertAxisDirectionsEqual(cs, AxisDirection.EAST, AxisDirection.NORTH);

        assertEquals("x", cs.getAxis(0).getName().getCode());
        assertEquals("y", cs.getAxis(1).getName().getCode());
    }

    /**
     * Tests XML (un)marshalling of an engineering CRS using a Spherical CS.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testSphericalXML() throws JAXBException {
        final String xml = marshal(createSpherical());
        assertXmlEquals(
                "<gml:EngineeringCRS xmlns:gml=\"" + Namespaces.GML + "\">\n" +
                "  <gml:name>A spherical CRS</gml:name>\n" +
                "  <gml:sphericalCS>\n" +
                "    <gml:SphericalCS gml:id=\"Spherical\">\n" +
                "      <gml:name>Spherical</gml:name>\n" +
                "      <gml:axis>\n" +
                "        <gml:CoordinateSystemAxis uom=\"urn:ogc:def:uom:EPSG::9122\" gml:id=\"SphericalLatitude\">\n" +
                "          <gml:name>Spherical latitude</gml:name>\n" +
                "          <gml:axisAbbrev>Ω</gml:axisAbbrev>\n" +
                "          <gml:axisDirection codeSpace=\"EPSG\">north</gml:axisDirection>\n" +
                "          <gml:minimumValue>-90.0</gml:minimumValue>\n" +
                "          <gml:maximumValue>90.0</gml:maximumValue>\n" +
                "          <gml:rangeMeaning codeSpace=\"EPSG\">exact</gml:rangeMeaning>\n" +
                "        </gml:CoordinateSystemAxis>\n" +
                "      </gml:axis>\n" +
                "      <gml:axis>\n" +
                "        <gml:CoordinateSystemAxis uom=\"urn:ogc:def:uom:EPSG::9122\" gml:id=\"SphericalLongitude\">\n" +
                "          <gml:name>Spherical longitude</gml:name>\n" +
                "          <gml:axisAbbrev>θ</gml:axisAbbrev>\n" +
                "          <gml:axisDirection codeSpace=\"EPSG\">east</gml:axisDirection>\n" +
                "          <gml:minimumValue>-180.0</gml:minimumValue>\n" +
                "          <gml:maximumValue>180.0</gml:maximumValue>\n" +
                "          <gml:rangeMeaning codeSpace=\"EPSG\">wraparound</gml:rangeMeaning>\n" +
                "        </gml:CoordinateSystemAxis>\n" +
                "      </gml:axis>\n" +
                "      <gml:axis>\n" +
                "        <gml:CoordinateSystemAxis uom=\"urn:ogc:def:uom:EPSG::9001\" gml:id=\"GeocentricRadius\">\n" +
                "          <gml:name>Geocentric radius</gml:name>\n" +
                "          <gml:axisAbbrev>r</gml:axisAbbrev>\n" +
                "          <gml:axisDirection codeSpace=\"EPSG\">up</gml:axisDirection>\n" +
                "          <gml:minimumValue>0.0</gml:minimumValue>\n" +
                "          <gml:rangeMeaning codeSpace=\"EPSG\">exact</gml:rangeMeaning>\n" +
                "        </gml:CoordinateSystemAxis>\n" +
                "      </gml:axis>\n" +
                "    </gml:SphericalCS>\n" +
                "  </gml:sphericalCS>\n" +
                "  <gml:engineeringDatum>\n" +
                "    <gml:EngineeringDatum gml:id=\"Centre\">\n" +
                "      <gml:name>Centre</gml:name>\n" +
                "    </gml:EngineeringDatum>\n" +
                "  </gml:engineeringDatum>\n" +
                "</gml:EngineeringCRS>",
                xml, "xmlns:*");

        final DefaultEngineeringCRS crs = unmarshal(DefaultEngineeringCRS.class, xml);
        assertEquals("A spherical CRS", crs.getName().getCode());
        assertEquals("Centre", crs.getDatum().getName().getCode());

        final CoordinateSystem cs = crs.getCoordinateSystem();
        assertInstanceOf(SphericalCS.class, cs);
        assertEquals("Spherical", cs.getName().getCode());
        assertEquals(3, cs.getDimension());
        assertAxisDirectionsEqual(cs, AxisDirection.NORTH, AxisDirection.EAST, AxisDirection.UP);

        assertEquals("Spherical latitude",  cs.getAxis(0).getName().getCode());
        assertEquals("Spherical longitude", cs.getAxis(1).getName().getCode());
        assertEquals("Geocentric radius",   cs.getAxis(2).getName().getCode());
        assertEquals("Ω", cs.getAxis(0).getAbbreviation());
        assertEquals("θ", cs.getAxis(1).getAbbreviation());
        assertEquals("r", cs.getAxis(2).getAbbreviation());
    }
}
