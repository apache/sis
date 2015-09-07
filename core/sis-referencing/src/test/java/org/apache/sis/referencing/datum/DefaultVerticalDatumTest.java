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

import java.lang.reflect.Field;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import org.opengis.referencing.datum.VerticalDatumType;
import org.apache.sis.internal.metadata.VerticalDatumTypes;
import org.apache.sis.internal.jaxb.LegacyNamespaces;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.util.Version;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static java.util.Collections.singletonMap;
import static org.apache.sis.test.MetadataAssert.*;
import static org.apache.sis.referencing.GeodeticObjectVerifier.*;


/**
 * Tests the {@link DefaultVerticalDatum} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class DefaultVerticalDatumTest extends XMLTestCase {
    /**
     * An XML file in this package containing a vertical datum definition.
     */
    private static final String XML_FILE = "VerticalDatum.xml";

    /**
     * An XML file with the same content than {@link #XML_FILE}, but written in an older GML format.
     */
    private static final String GML31_FILE = "VerticalDatum (GML 3.1).xml";

    /**
     * Tests the {@link DefaultVerticalDatum#getVerticalDatumType()} method in a state
     * simulating unmarshalling of GML 3.2 document.
     *
     * @throws NoSuchFieldException   Should never happen.
     * @throws IllegalAccessException Should never happen.
     */
    @Test
    public void testAfterUnmarshal() throws NoSuchFieldException, IllegalAccessException {
        final Field typeField = DefaultVerticalDatum.class.getDeclaredField("type");
        typeField.setAccessible(true);
        assertEquals(VerticalDatumType .GEOIDAL,       typeForName(typeField, "Geoidal height"));
        assertEquals(VerticalDatumType .DEPTH,         typeForName(typeField, "Some depth measurement"));
        assertEquals(VerticalDatumTypes.ELLIPSOIDAL,   typeForName(typeField, "Ellipsoidal height"));
        assertEquals(VerticalDatumType .OTHER_SURFACE, typeForName(typeField, "NotADepth"));
    }

    /**
     * Returns the vertical datum type inferred by {@link DefaultVerticalDatum} for the given name.
     */
    private static VerticalDatumType typeForName(final Field typeField, final String name) throws IllegalAccessException {
        final DefaultVerticalDatum datum = new DefaultVerticalDatum(
                singletonMap(DefaultVerticalDatum.NAME_KEY, name), VerticalDatumType.OTHER_SURFACE);
        typeField.set(datum, null);
        return datum.getVerticalDatumType();
    }

    /**
     * Tests {@link DefaultVerticalDatum#toWKT()}.
     */
    @Test
    public void testToWKT() {
        DefaultVerticalDatum datum;
        datum = new DefaultVerticalDatum(singletonMap(DefaultVerticalDatum.NAME_KEY, "Geoidal"), VerticalDatumType.GEOIDAL);
        assertWktEquals(Convention.WKT1, "VERT_DATUM[“Geoidal”, 2005]", datum);
        assertWktEquals(Convention.WKT2, "VDATUM[“Geoidal”]", datum);
        assertWktEquals(Convention.WKT2_SIMPLIFIED, "VerticalDatum[“Geoidal”]", datum);

        datum = new DefaultVerticalDatum(singletonMap(DefaultVerticalDatum.NAME_KEY, "Ellipsoidal"), VerticalDatumTypes.ELLIPSOIDAL);
        assertWktEquals(Convention.WKT1, "VERT_DATUM[“Ellipsoidal”, 2002]", datum);
        assertWktEquals(Convention.WKT2, "VDATUM[“Ellipsoidal”]", datum);
        assertWktEquals(Convention.WKT2_SIMPLIFIED, "VerticalDatum[“Ellipsoidal”]", datum);
    }

    /**
     * Tests XML (un)marshalling.
     *
     * @throws JAXBException If an error occurred during (un)marshalling.
     */
    @Test
    public void testXML() throws JAXBException {
        final DefaultVerticalDatum datum = unmarshalFile(DefaultVerticalDatum.class, XML_FILE);
        assertIsMeanSeaLevel(datum, true);
        /*
         * Following attribute does not exist in GML 3.2, so it has been inferred.
         * Our datum name is "Mean Sea Level", which is mapped to the geoidal type.
         */
        assertEquals("vertDatumType", VerticalDatumType.GEOIDAL, datum.getVerticalDatumType());
        /*
         * Values in the following tests are specific to our XML file.
         * The actual texts in the EPSG database are more descriptive.
         */
        assertEquals("remarks",          "Approximates geoid.",             datum.getRemarks().toString());
        assertEquals("scope",            "Hydrography.",                    datum.getScope().toString());
        assertEquals("anchorDefinition", "Averaged over a 19-year period.", datum.getAnchorPoint().toString());
        /*
         * Test marshalling and compare with the original file.
         */
        assertMarshalEqualsFile(XML_FILE, datum, "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Tests (un)marshalling of an older version, GML 3.1.
     *
     * @throws JAXBException If an error occurred during unmarshalling.
     *
     * @see <a href="http://issues.apache.org/jira/browse/SIS-160">SIS-160: Need XSLT between GML 3.1 and 3.2</a>
     */
    @Test
    public void testGML31() throws JAXBException {
        final Version version = new Version("3.1");
        final MarshallerPool pool = getMarshallerPool();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        unmarshaller.setProperty(XML.GML_VERSION, version);
        final DefaultVerticalDatum datum =
                (DefaultVerticalDatum) unmarshaller.unmarshal(getClass().getResource(GML31_FILE));
        pool.recycle(unmarshaller);
        /*
         * Following attribute exists in GML 3.1 only.
         */
        assertEquals("vertDatumType", VerticalDatumType.GEOIDAL, datum.getVerticalDatumType());
        /*
         * The name, anchor definition and domain of validity are lost because
         * those property does not have the same XML element name (SIS-160).
         * Below is all we have.
         */
        assertEquals("remarks", "Approximates geoid.", datum.getRemarks().toString());
        assertEquals("scope",   "Hydrography.",        datum.getScope().toString());
        /*
         * Test marshaling. We can not yet compare with the original XML file
         * because of all the information lost. This may be fixed in a future
         * SIS version (SIS-160).
         */
        final Marshaller marshaller = pool.acquireMarshaller();
        marshaller.setProperty(XML.GML_VERSION, version);
        final String xml = marshal(marshaller, datum);
        pool.recycle(marshaller);
        assertXmlEquals(
                "<gml:VerticalDatum xmlns:gml=\"" + LegacyNamespaces.GML + "\">\n" +
                "  <gml:remarks>Approximates geoid.</gml:remarks>\n" +
                "  <gml:scope>Hydrography.</gml:scope>\n" +
                "  <gml:verticalDatumType>geoidal</gml:verticalDatumType>\n" +
                "</gml:VerticalDatum>", xml, "xmlns:*");
    }
}
