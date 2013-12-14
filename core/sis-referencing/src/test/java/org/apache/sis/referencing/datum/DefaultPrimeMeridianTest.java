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

import javax.measure.unit.NonSI;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.util.CharSequences;
import org.apache.sis.internal.jaxb.LegacyNamespaces;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.apache.sis.referencing.Assert.*;
import static org.apache.sis.test.mock.PrimeMeridianMock.GREENWICH;


/**
 * Tests the {@link DefaultPrimeMeridian} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn(org.apache.sis.referencing.AbstractIdentifiedObjectTest.class)
public final strictfp class DefaultPrimeMeridianTest extends DatumTestCase {
    /**
     * Tests {@link DefaultPrimeMeridian#toWKT()}.
     */
    @Test
    public void testToWKT() {
        final DefaultPrimeMeridian pm = new DefaultPrimeMeridian(GREENWICH);
        assertIsGreenwichMeridian(pm);
        assertWktEquals(pm, "PRIMEM[“Greenwich”, 0.0]");
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
     * @throws JAXBException If an error occurred during marshalling.
     */
    @Test
    public void testMarshall() throws JAXBException {
        final DefaultPrimeMeridian pm = new DefaultPrimeMeridian(GREENWICH);
        assertXmlEquals(getGreenwichXml(Namespaces.GML), marshal(pm), "xmlns:*");
    }

    /**
     * Tests marshalling in the GML 3.1 namespace.
     *
     * @throws JAXBException If an error occurred during marshalling.
     */
    @Test
    @DependsOnMethod("testMarshall")
    public void testMarshallGML31() throws JAXBException {
        final DefaultPrimeMeridian pm = new DefaultPrimeMeridian(GREENWICH);
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
     * @throws JAXBException If an error occurred during unmarshalling.
     *
     * @see <a href="http://epsg-registry.org/export.htm?gml=urn:ogc:def:meridian:EPSG::8901">GML export of EPSG:8901</a>
     */
    @Test
    public void testUnmarshall() throws JAXBException {
        final DefaultPrimeMeridian pm = unmarshall(DefaultPrimeMeridian.class, "Greenwich.xml");
        assertIsGreenwichMeridian(pm);
    }

    /**
     * Tests marshalling in the GML 3.1 namespace.
     *
     * @throws JAXBException If an error occurred during unmarshalling.
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
        assertIsGreenwichMeridian(pm);
    }

    /**
     * Tests unmarshalling of Paris prime meridian.
     *
     * @throws JAXBException If an error occurred during unmarshalling.
     */
    @Test
    @DependsOnMethod({"testUnmarshall", "testMarshall"})
    public void testParisMeridian() throws JAXBException {
        final DefaultPrimeMeridian pm = unmarshall(DefaultPrimeMeridian.class, "Paris.xml");
        assertIsParisMeridian(pm);
        assertEquals("greenwichLongitude", 2.33722917, pm.getGreenwichLongitude(NonSI.DEGREE_ANGLE), 1E-12);
        assertEquals("Equivalent to 2°20′14.025″.", pm.getRemarks().toString());
        assertNull("name.codeSpace", pm.getName().getCodeSpace());
        assertWktEquals(pm, "PRIMEM[“Paris”, 2.33722917, AUTHORITY[“EPSG”, “8903”]]");
        assertXmlEquals(
                "<gml:PrimeMeridian xmlns:gml=\"" + Namespaces.GML + "\">\n" +
                "  <gml:identifier codeSpace=\"EPSG\">8903</gml:identifier>" +
                "  <gml:name>Paris</gml:name>\n" +
                "  <gml:remarks>Equivalent to 2°20′14.025″.</gml:remarks>\n" +
                "  <gml:greenwichLongitude uom=\"urn:ogc:def:uom:EPSG::9105\">2.5969213</gml:greenwichLongitude>\n" +
                "</gml:PrimeMeridian>\n",
                marshal(pm), "xmlns:*");
    }
}
