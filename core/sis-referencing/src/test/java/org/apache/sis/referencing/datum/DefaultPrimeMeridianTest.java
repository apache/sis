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
import javax.xml.bind.JAXBException;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.util.CharSequences;
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
public final strictfp class DefaultPrimeMeridianTest extends DatumTestCase {
    /**
     * Tests {@link DefaultPrimeMeridian#toWKT()}.
     */
    @Test
    public void testToWKT() {
        final DefaultPrimeMeridian pm = new DefaultPrimeMeridian(GREENWICH);
        assertWktEquals(pm, "PRIMEM[“Greenwich”, 0.0]");
    }

    /**
     * Compares the given XML representation of Greenwich prime meridian against the expected XML.
     * The XML is expected to use the given GML namespace URI.
     *
     * @param namespace The expected GML namespace.
     * @param actual XML representation of Greenwich prime meridian to test.
     */
    private static void assertGreenwichXmlEquals(final String namespace, final String actual) {
        assertXmlEquals(CharSequences.replace(
                "<gml:PrimeMeridian xmlns:gml=\"" + Namespaces.GML + "\">\n" +
                "  <gml:name codeSpace=\"test\">Greenwich</gml:name>\n" +
                "  <gml:greenwichLongitude uom=\"urn:ogc:def:uom:EPSG::9102\">0.0</gml:greenwichLongitude>\n" +
                "</gml:PrimeMeridian>\n",
                Namespaces.GML, namespace).toString(), actual, "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Tests marshalling in the default namespace.
     *
     * @throws JAXBException If an error occurred during marshalling.
     */
    @Test
    public void testMarshall() throws JAXBException {
        final DefaultPrimeMeridian pm = new DefaultPrimeMeridian(GREENWICH);
        assertGreenwichXmlEquals(Namespaces.GML, marshal(pm));
    }

    /**
     * Tests marshalling in the GML 3.1 namespace.
     *
     * @throws JAXBException If an error occurred during marshalling.
     */
    @Test
    @org.junit.Ignore
    public void testMarshallGML31() throws JAXBException {
        final DefaultPrimeMeridian pm = new DefaultPrimeMeridian(GREENWICH);
        final MarshallerPool pool = getMarshallerPool();
        final Marshaller marshaller = pool.acquireMarshaller();
        final String xml = marshal(marshaller, pm);
        pool.recycle(marshaller);
        assertGreenwichXmlEquals("http://www.opengis.net/gml", marshal(pm));
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
        DefaultPrimeMeridian pm = unmarshall(DefaultPrimeMeridian.class, "Greenwich.xml");
        assertEquals("greenwichLongitude", pm.getGreenwichLongitude(), 0, 0);
        assertEquals("angularUnit", NonSI.DEGREE_ANGLE, pm.getAngularUnit());
    }
}
