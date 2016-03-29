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

import java.util.Collections;
import javax.xml.bind.JAXBException;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.SphericalCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.referencing.datum.DefaultEngineeringDatum;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.xml.Namespaces;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests {@link DefaultEngineeringCRS}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final strictfp class DefaultEngineeringCRSTest extends XMLTestCase {
    /**
     * Creates an engineering CRS using a two-dimensional Cartesian coordinate system.
     */
    private static DefaultEngineeringCRS createCartesian() {
        return new DefaultEngineeringCRS(Collections.singletonMap(DefaultEngineeringCRS.NAME_KEY, "A construction site CRS"),
                new DefaultEngineeringDatum(Collections.singletonMap(DefaultEngineeringDatum.NAME_KEY, "P1")),
                HardCodedCS.CARTESIAN_2D);
    }

    /**
     * Creates an engineering CRS using a three-dimensional Spherical coordinate system.
     */
    private static DefaultEngineeringCRS createSpherical() {
        return new DefaultEngineeringCRS(Collections.singletonMap(DefaultEngineeringCRS.NAME_KEY, "A spherical CRS"),
                new DefaultEngineeringDatum(Collections.singletonMap(DefaultEngineeringDatum.NAME_KEY, "Centre")),
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
        assertWktEquals(Convention.WKT2,
                "ENGCRS[“A spherical CRS”,\n" +
                "  EDATUM[“Centre”],\n" +
                "  CS[spherical, 3],\n" +
                "    AXIS[“Spherical latitude (U)”, north, ORDER[1], ANGLEUNIT[“degree”, 0.017453292519943295]],\n" +
                "    AXIS[“Spherical longitude (V)”, east, ORDER[2], ANGLEUNIT[“degree”, 0.017453292519943295]],\n" +
                "    AXIS[“Geocentric radius (R)”, up, ORDER[3], LENGTHUNIT[“metre”, 1]]]",
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
                "    Axis[“Geocentric radius (R)”, up, Unit[“metre”, 1]]]",
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
                "  <gml:cartesianCS gml:id=\"Cartesian2D\">\n" +
                "    <gml:name>Cartesian 2D</gml:name>\n" +
                "    <gml:axis>\n" +
                "      <gml:CoordinateSystemAxis uom=\"urn:ogc:def:uom:EPSG::9001\" gml:id=\"x\">\n" +
                "        <gml:name>x</gml:name>\n" +
                "        <gml:axisAbbrev>x</gml:axisAbbrev>\n" +
                "        <gml:axisDirection codeSpace=\"EPSG\">east</gml:axisDirection>\n" +
                "      </gml:CoordinateSystemAxis>\n" +
                "    </gml:axis>\n" +
                "    <gml:axis>\n" +
                "      <gml:CoordinateSystemAxis uom=\"urn:ogc:def:uom:EPSG::9001\" gml:id=\"y\">\n" +
                "        <gml:name>y</gml:name>\n" +
                "        <gml:axisAbbrev>y</gml:axisAbbrev>\n" +
                "        <gml:axisDirection codeSpace=\"EPSG\">north</gml:axisDirection>\n" +
                "      </gml:CoordinateSystemAxis>\n" +
                "    </gml:axis>\n" +
                "  </gml:cartesianCS>\n" +
                "  <gml:engineeringDatum>\n" +
                "    <gml:EngineeringDatum gml:id=\"P1\">\n" +
                "      <gml:name>P1</gml:name>\n" +
                "    </gml:EngineeringDatum>\n" +
                "  </gml:engineeringDatum>\n" +
                "</gml:EngineeringCRS>",
                xml, "xmlns:*");

        final DefaultEngineeringCRS crs = unmarshal(DefaultEngineeringCRS.class, xml);
        assertEquals("name", "A construction site CRS", crs.getName().getCode());
        assertEquals("datum.name", "P1", crs.getDatum().getName().getCode());

        final CoordinateSystem cs = crs.getCoordinateSystem();
        assertInstanceOf("coordinateSystem", CartesianCS.class, cs);
        assertEquals("cs.name", "Cartesian 2D", cs.getName().getCode());
        assertEquals("cs.dimension", 2, cs.getDimension());
        assertAxisDirectionsEqual("cartesianCS", cs, AxisDirection.EAST, AxisDirection.NORTH);

        assertEquals("cs.axis[0].name", "x", cs.getAxis(0).getName().getCode());
        assertEquals("cs.axis[1].name", "y", cs.getAxis(1).getName().getCode());
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
                "  <gml:sphericalCS gml:id=\"Spherical\">\n" +
                "    <gml:name>Spherical</gml:name>\n" +
                "    <gml:axis>\n" +
                "      <gml:CoordinateSystemAxis uom=\"urn:ogc:def:uom:EPSG::9122\" gml:id=\"SphericalLatitude\">\n" +
                "        <gml:name>Spherical latitude</gml:name>\n" +
                "        <gml:axisAbbrev>φ′</gml:axisAbbrev>\n" +
                "        <gml:axisDirection codeSpace=\"EPSG\">north</gml:axisDirection>\n" +
                "        <gml:minimumValue>-90.0</gml:minimumValue>\n" +
                "        <gml:maximumValue>90.0</gml:maximumValue>\n" +
                "        <gml:rangeMeaning codeSpace=\"EPSG\">exact</gml:rangeMeaning>\n" +
                "      </gml:CoordinateSystemAxis>\n" +
                "    </gml:axis>\n" +
                "    <gml:axis>\n" +
                "      <gml:CoordinateSystemAxis uom=\"urn:ogc:def:uom:EPSG::9122\" gml:id=\"SphericalLongitude\">\n" +
                "        <gml:name>Spherical longitude</gml:name>\n" +
                "        <gml:axisAbbrev>θ</gml:axisAbbrev>\n" +
                "        <gml:axisDirection codeSpace=\"EPSG\">east</gml:axisDirection>\n" +
                "        <gml:minimumValue>-180.0</gml:minimumValue>\n" +
                "        <gml:maximumValue>180.0</gml:maximumValue>\n" +
                "        <gml:rangeMeaning codeSpace=\"EPSG\">wraparound</gml:rangeMeaning>\n" +
                "      </gml:CoordinateSystemAxis>\n" +
                "    </gml:axis>\n" +
                "    <gml:axis>\n" +
                "      <gml:CoordinateSystemAxis uom=\"urn:ogc:def:uom:EPSG::9001\" gml:id=\"GeocentricRadius\">\n" +
                "        <gml:name>Geocentric radius</gml:name>\n" +
                "        <gml:axisAbbrev>R</gml:axisAbbrev>\n" +
                "        <gml:axisDirection codeSpace=\"EPSG\">up</gml:axisDirection>\n" +
                "        <gml:minimumValue>0.0</gml:minimumValue>\n" +
                "        <gml:rangeMeaning codeSpace=\"EPSG\">exact</gml:rangeMeaning>\n" +
                "      </gml:CoordinateSystemAxis>\n" +
                "    </gml:axis>\n" +
                "  </gml:sphericalCS>\n" +
                "  <gml:engineeringDatum>\n" +
                "    <gml:EngineeringDatum gml:id=\"Centre\">\n" +
                "      <gml:name>Centre</gml:name>\n" +
                "    </gml:EngineeringDatum>\n" +
                "  </gml:engineeringDatum>\n" +
                "</gml:EngineeringCRS>",
                xml, "xmlns:*");

        final DefaultEngineeringCRS crs = unmarshal(DefaultEngineeringCRS.class, xml);
        assertEquals("name", "A spherical CRS", crs.getName().getCode());
        assertEquals("datum.name", "Centre", crs.getDatum().getName().getCode());

        final CoordinateSystem cs = crs.getCoordinateSystem();
        assertInstanceOf("coordinateSystem", SphericalCS.class, cs);
        assertEquals("cs.name", "Spherical", cs.getName().getCode());
        assertEquals("cs.dimension", 3, cs.getDimension());
        assertAxisDirectionsEqual("cartesianCS", cs, AxisDirection.NORTH, AxisDirection.EAST, AxisDirection.UP);

        assertEquals("cs.axis[0].name", "Spherical latitude",  cs.getAxis(0).getName().getCode());
        assertEquals("cs.axis[1].name", "Spherical longitude", cs.getAxis(1).getName().getCode());
        assertEquals("cs.axis[2].name", "Geocentric radius",   cs.getAxis(2).getName().getCode());
        assertEquals("cs.axis[0].abbreviation", "φ′",          cs.getAxis(0).getAbbreviation());
        assertEquals("cs.axis[1].abbreviation", "θ",           cs.getAxis(1).getAbbreviation());
        assertEquals("cs.axis[2].abbreviation", "R",           cs.getAxis(2).getAbbreviation());
    }
}
