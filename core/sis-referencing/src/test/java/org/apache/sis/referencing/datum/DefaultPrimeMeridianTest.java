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

import java.util.Map;
import javax.measure.quantity.Angle;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.util.CharSequences;
import org.apache.sis.measure.Units;
import org.apache.sis.internal.xml.LegacyNamespaces;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.test.xml.TestCase;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.metadata.Assertions.assertXmlEquals;
import static org.apache.sis.referencing.Assertions.assertWktEquals;
import static org.apache.sis.referencing.GeodeticObjectVerifier.*;


/**
 * Tests the {@link DefaultPrimeMeridian} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.4
 */
@DependsOn(org.apache.sis.referencing.AbstractIdentifiedObjectTest.class)
public final class DefaultPrimeMeridianTest extends TestCase {
    /**
     * An XML file in this package containing a prime meridian definition.
     */
    private static final String XML_FILE = "Greenwich.xml";

    /**
     * Tests {@link DefaultPrimeMeridian#toWKT()}.
     */
    @Test
    public void testToWKT() {
        final DefaultPrimeMeridian pm = new DefaultPrimeMeridian(PrimeMeridianMock.GREENWICH);
        assertIsGreenwich(pm);
        assertWktEquals(Convention.WKT2, "PRIMEM[“Greenwich”, 0.0, ANGLEUNIT[“degree”, 0.017453292519943295]]", pm);
        assertWktEquals(Convention.WKT2_SIMPLIFIED, "PrimeMeridian[“Greenwich”, 0.0]", pm);
    }

    /**
     * Tests WKT formatting of a prime meridian in grad units.
     *
     * @since 0.6
     */
    @Test
    @DependsOnMethod("testToWKT")
    public void testWKT_inGrads() {
        final DefaultPrimeMeridian pm = HardCodedDatum.PARIS;
        assertWktEquals(Convention.WKT1, "PRIMEM[“Paris”, 2.33722917, AUTHORITY[“EPSG”, “8903”]]", pm);
        assertWktEquals(Convention.WKT2_SIMPLIFIED, "PrimeMeridian[“Paris”, 2.5969213, Unit[“grad”, 0.015707963267948967],"
                + " Id[“EPSG”, 8903, URI[“urn:ogc:def:meridian:EPSG::8903”]]]", pm);
    }

    /**
     * Tests WKT formatting of a prime meridian with sexagesimal units.
     * Since those units cannot be formatted in a {@code UNIT["name", scale]} element,
     * the formatter should convert them to a formattable unit like degrees.
     *
     * @since 0.6
     */
    @Test
    @DependsOnMethod("testWKT_inGrads")
    public void testWKT_withUnformattableUnit() {
        final DefaultPrimeMeridian pm = new DefaultPrimeMeridian(
                Map.of(DefaultPrimeMeridian.NAME_KEY, "Test"),
                10.3, Units.valueOfEPSG(9111).asType(Angle.class));
        /*
         * In WKT 1 format, if there is no contextual unit (which is the case of this test),
         * the formatter default to decimal degrees. In WKT 2 format it depends on the PM unit.
         */
        assertWktEquals(Convention.WKT1, "PRIMEM[“Test”, 10.5]", pm);  // 10.3 DM  ==  10.5°
        assertWktEquals(Convention.WKT2, "PRIMEM[“Test”, 10.5, ANGLEUNIT[“degree”, 0.017453292519943295]]", pm);
        assertWktEquals(Convention.WKT2_SIMPLIFIED, "PrimeMeridian[“Test”, 10.5]", pm);
        assertWktEquals(Convention.INTERNAL, "PrimeMeridian[“Test”, 10.3, Unit[“D.M”, 0.017453292519943295, Id[“EPSG”, 9111]]]", pm);
    }

    /**
     * Returns a XML representation of Greenwich prime meridian using the given GML namespace URI.
     *
     * @param  namespace The GML namespace.
     * @return XML representation of Greenwich prime meridian.
     */
    private static String getGreenwichXml(final String namespace) {
        return CharSequences.replace(
                "<gml:PrimeMeridian xmlns:gml=\"" + Namespaces.GML + "\">\n" +
                "  <gml:name codeSpace=\"test\">Greenwich</gml:name>\n" +
                "  <gml:greenwichLongitude uom=\"urn:ogc:def:uom:EPSG::9102\">0.0</gml:greenwichLongitude>\n" +
                "</gml:PrimeMeridian>\n",
                Namespaces.GML, namespace).toString();
    }

    /**
     * Tests marshalling in the default namespace.
     *
     * @throws JAXBException if an error occurred during marshalling.
     */
    @Test
    public void testMarshall() throws JAXBException {
        final DefaultPrimeMeridian pm = new DefaultPrimeMeridian(PrimeMeridianMock.GREENWICH);
        assertXmlEquals(getGreenwichXml(Namespaces.GML), marshal(pm), "xmlns:*");
    }

    /**
     * Tests marshalling in the GML 3.1 namespace.
     *
     * @throws JAXBException if an error occurred during marshalling.
     */
    @Test
    @DependsOnMethod("testMarshall")
    public void testMarshallGML31() throws JAXBException {
        final DefaultPrimeMeridian pm = new DefaultPrimeMeridian(PrimeMeridianMock.GREENWICH);
        final MarshallerPool pool = getMarshallerPool();
        final Marshaller marshaller = pool.acquireMarshaller();
        marshaller.setProperty(XML.GML_VERSION, LegacyNamespaces.VERSION_3_0);
        final String xml = marshal(marshaller, pm);
        pool.recycle(marshaller);
        assertXmlEquals(getGreenwichXml(LegacyNamespaces.GML), xml, "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Tests unmarshalling.
     *
     * @throws JAXBException if an error occurred during unmarshalling.
     */
    @Test
    public void testUnmarshall() throws JAXBException {
        final DefaultPrimeMeridian pm = unmarshalFile(DefaultPrimeMeridian.class, XML_FILE);
        assertIsGreenwich(pm);
    }

    /**
     * Tests marshalling in the GML 3.1 namespace.
     *
     * @throws JAXBException if an error occurred during unmarshalling.
     */
    @Test
    @DependsOnMethod("testUnmarshall")
    public void testUnarshallGML31() throws JAXBException {
        final MarshallerPool pool = getMarshallerPool();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        unmarshaller.setProperty(XML.GML_VERSION, LegacyNamespaces.VERSION_3_0);
        final DefaultPrimeMeridian pm = (DefaultPrimeMeridian)
                unmarshal(unmarshaller, getGreenwichXml(LegacyNamespaces.GML));
        pool.recycle(unmarshaller);
        assertIsGreenwich(pm);
    }

    /**
     * Tests unmarshalling of Paris prime meridian.
     *
     * @throws JAXBException if an error occurred during unmarshalling.
     */
    @Test
    @DependsOnMethod({"testUnmarshall", "testMarshall", "testWKT_inGrads"})
    public void testParisMeridian() throws JAXBException {
        final DefaultPrimeMeridian pm = unmarshalFile(DefaultPrimeMeridian.class, "PrimeMeridian.xml");
        assertIsParis(pm);
        assertEquals("greenwichLongitude", 2.33722917, pm.getGreenwichLongitude(Units.DEGREE), 1E-12);
        assertEquals("Equivalent to 2°20′14.025″.", pm.getRemarks().toString());
        assertNull("name.codeSpace", pm.getName().getCodeSpace());
        assertWktEquals(Convention.WKT1,
                "PRIMEM[“Paris”, 2.33722917, AUTHORITY[“EPSG”, “8903”]]", pm);
        assertWktEquals(Convention.WKT2,
                "PRIMEM[“Paris”, 2.5969213, ANGLEUNIT[“grad”, 0.015707963267948967], ID[“EPSG”, 8903, URI[“urn:ogc:def:meridian:EPSG::8903”]]]", pm);
        assertWktEquals(Convention.WKT2_SIMPLIFIED,
                "PrimeMeridian[“Paris”, 2.5969213, Unit[“grad”, 0.015707963267948967], Id[“EPSG”, 8903, URI[“urn:ogc:def:meridian:EPSG::8903”]]]", pm);
        assertWktEquals(Convention.INTERNAL,
                "PrimeMeridian[“Paris”, 2.5969213, Unit[“grad”, 0.015707963267948967, Id[“EPSG”, 9105]], Id[“EPSG”, 8903],\n" +
                "  Remark[“Equivalent to 2°20′14.025″.”]]", pm);
        assertXmlEquals(
                "<gml:PrimeMeridian xmlns:gml=\"" + Namespaces.GML + "\">\n" +
                "  <gml:identifier codeSpace=\"IOGP\">urn:ogc:def:meridian:EPSG::8903</gml:identifier>\n" +
                "  <gml:name>Paris</gml:name>\n" +
                "  <gml:remarks>Equivalent to 2°20′14.025″.</gml:remarks>\n" +
                "  <gml:greenwichLongitude uom=\"urn:ogc:def:uom:EPSG::9105\">2.5969213</gml:greenwichLongitude>\n" +
                "</gml:PrimeMeridian>\n",
                marshal(pm), "xmlns:*");
    }
}
