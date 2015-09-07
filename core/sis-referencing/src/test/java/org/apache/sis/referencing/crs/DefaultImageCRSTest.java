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
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.AffineCS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.apache.sis.referencing.cs.HardCodedCS;
import org.apache.sis.referencing.datum.DefaultImageDatum;
import org.apache.sis.referencing.cs.DefaultAffineCS;
import org.apache.sis.referencing.cs.HardCodedAxes;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.xml.Namespaces;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests {@link DefaultImageCRS}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.6
 * @version 0.6
 * @module
 */
public final strictfp class DefaultImageCRSTest extends XMLTestCase {
    /**
     * Creates an image CRS using a two-dimensional affine or Cartesian coordinate system.
     *
     * @param cartesian {@code true} for a Cartesian coordinate system, or {@code false} for an affine one.
     */
    private static DefaultImageCRS create(final boolean cartesian) {
        return new DefaultImageCRS(Collections.singletonMap(DefaultImageCRS.NAME_KEY, "An image CRS"),
                new DefaultImageDatum(Collections.singletonMap(DefaultImageDatum.NAME_KEY, "C1"), PixelInCell.CELL_CENTER),
                cartesian ? HardCodedCS.GRID : new DefaultAffineCS(
                        Collections.singletonMap(DefaultAffineCS.NAME_KEY, "Grid"),
                                HardCodedAxes.COLUMN, HardCodedAxes.ROW));
    }

    /**
     * Tests WKT 2 formatting.
     */
    @Test
    public void testWKT2() {
        final DefaultImageCRS crs = create(true);
        assertWktEquals(Convention.WKT2,
                "IMAGECRS[“An image CRS”,\n" +
                "  IDATUM[“C1”],\n" +
                "  CS[Cartesian, 2],\n" +
                "    AXIS[“Column (i)”, columnPositive, ORDER[1]],\n" +
                "    AXIS[“Row (j)”, rowPositive, ORDER[2]],\n" +
                "    SCALEUNIT[“unity”, 1]]",
                crs);

        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "ImageCRS[“An image CRS”,\n" +
                "  ImageDatum[“C1”],\n" +
                "  CS[Cartesian, 2],\n" +
                "    Axis[“Column (i)”, columnPositive],\n" +
                "    Axis[“Row (j)”, rowPositive],\n" +
                "    Unit[“unity”, 1]]",
                crs);
    }

    /**
     * Tests XML (un)marshalling of an image CRS using a Cartesian CS.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testCartesianXML() throws JAXBException {
        testXML(true);
    }

    /**
     * Tests XML (un)marshalling of an image CRS using an affine CS.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testAffineXML() throws JAXBException {
        testXML(false);
    }

    /**
     * Implementation of {@link #testCartesianXML()} and {@link #testAffineXML()}.
     */
    private void testXML(final boolean cartesian) throws JAXBException {
        String expected =
                "<gml:ImageCRS xmlns:gml=\"" + Namespaces.GML + "\">\n" +
                "  <gml:name>An image CRS</gml:name>\n" +
                "  <gml:cartesianCS>\n" +
                "    <gml:CartesianCS gml:id=\"Grid\">\n" +
                "      <gml:name>Grid</gml:name>\n" +
                "      <gml:axis>\n" +
                "        <gml:CoordinateSystemAxis uom=\"urn:ogc:def:uom:EPSG::9201\" gml:id=\"Column\">\n" +
                "          <gml:name>Column</gml:name>\n" +
                "          <gml:axisAbbrev>i</gml:axisAbbrev>\n" +
                "          <gml:axisDirection codeSpace=\"EPSG\">columnPositive</gml:axisDirection>\n" +
                "        </gml:CoordinateSystemAxis>\n" +
                "      </gml:axis>\n" +
                "      <gml:axis>\n" +
                "        <gml:CoordinateSystemAxis uom=\"urn:ogc:def:uom:EPSG::9201\" gml:id=\"Row\">\n" +
                "          <gml:name>Row</gml:name>\n" +
                "          <gml:axisAbbrev>j</gml:axisAbbrev>\n" +
                "          <gml:axisDirection codeSpace=\"EPSG\">rowPositive</gml:axisDirection>\n" +
                "        </gml:CoordinateSystemAxis>\n" +
                "      </gml:axis>\n" +
                "    </gml:CartesianCS>\n" +
                "  </gml:cartesianCS>\n" +
                "  <gml:imageDatum>\n" +
                "    <gml:ImageDatum gml:id=\"C1\">\n" +
                "      <gml:name>C1</gml:name>\n" +
                "      <gml:pixelInCell>cell center</gml:pixelInCell>\n" +
                "    </gml:ImageDatum>\n" +
                "  </gml:imageDatum>\n" +
                "</gml:ImageCRS>";
        if (!cartesian) {
            expected = expected.replace("CartesianCS", "AffineCS").replace("cartesianCS", "affineCS");
        }
        final String xml = marshal(create(cartesian));
        assertXmlEquals(expected, xml, "xmlns:*");

        final DefaultImageCRS crs = unmarshal(DefaultImageCRS.class, xml);
        assertEquals("name", "An image CRS", crs.getName().getCode());
        assertEquals("datum.name", "C1", crs.getDatum().getName().getCode());

        final CoordinateSystem cs = crs.getCoordinateSystem();
        assertInstanceOf("coordinateSystem", cartesian ? CartesianCS.class : AffineCS.class, cs);
        assertEquals("cs.isCartesian", cartesian, cs instanceof CartesianCS);
        assertEquals("cs.name", "Grid", cs.getName().getCode());
        assertEquals("cs.dimension", 2, cs.getDimension());
        assertAxisDirectionsEqual("cartesianCS", cs, AxisDirection.COLUMN_POSITIVE, AxisDirection.ROW_POSITIVE);

        assertEquals("cs.axis[0].name", "Column",    cs.getAxis(0).getName().getCode());
        assertEquals("cs.axis[1].name", "Row",       cs.getAxis(1).getName().getCode());
        assertEquals("cs.axis[0].abbreviation", "i", cs.getAxis(0).getAbbreviation());
        assertEquals("cs.axis[1].abbreviation", "j", cs.getAxis(1).getAbbreviation());
    }
}
