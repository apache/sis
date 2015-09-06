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
package org.apache.sis.xml;

import java.lang.reflect.Proxy;
import javax.xml.bind.JAXBException;
import org.opengis.metadata.citation.Series;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests the XML marshalling of object having {@code uuid} or {@code uuidref} attributes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 *
 * @see <a href="http://jira.geotoolkit.org/browse/GEOTK-165">GEOTK-165</a>
 */
@DependsOn(NilReasonMarshallingTest.class)
public final strictfp class UUIDMarshallingTest extends XMLTestCase {
    /**
     * A random UUID for the tests in this class.
     */
    private static final String UUID_VALUE = "f8f5fcb1-d57b-4013-b3a4-4eaa40df6dcf";

    /**
     * A XML with a {@code uuid} identifier in the {@code <gmd:CI_Series>} element.
     */
    private static final String IDENTIFIED_XML =
            "<gmd:CI_Citation xmlns:gmd=\""   + Namespaces.GMD + '"' +
                            " xmlns:gco=\""   + Namespaces.GCO + "\">\n" +
            "  <gmd:title>\n" +
            "    <gco:CharacterString>My data</gco:CharacterString>\n" +
            "  </gmd:title>\n" +
            "  <gmd:series>\n" +
            "    <gmd:CI_Series uuid=\"" + UUID_VALUE + "\">\n" +
            "      <gmd:name>\n" +
            "        <gco:CharacterString>My aggregate dataset</gco:CharacterString>\n" +
            "      </gmd:name>\n" +
            "    </gmd:CI_Series>\n" +
            "  </gmd:series>\n" +
            "</gmd:CI_Citation>";

    /**
     * A XML with a {@code uuidref} identifier in the {@code <gmd:series>} element.
     * This XML declares the method body anyway, which is kind of contradictory with usage of reference.
     */
    private static final String REFERENCED_XML_WITH_BODY =
            "<gmd:CI_Citation xmlns:gmd=\""   + Namespaces.GMD + '"' +
                            " xmlns:gco=\""   + Namespaces.GCO + "\">\n" +
            "  <gmd:title>\n" +
            "    <gco:CharacterString>My data</gco:CharacterString>\n" +
            "  </gmd:title>\n" +
            "  <gmd:series uuidref=\"" + UUID_VALUE + "\">\n" +
            "    <gmd:CI_Series>\n" +
            "      <gmd:name>\n" +
            "        <gco:CharacterString>My aggregate dataset</gco:CharacterString>\n" +
            "      </gmd:name>\n" +
            "    </gmd:CI_Series>\n" +
            "  </gmd:series>\n" +
            "</gmd:CI_Citation>";

    /**
     * A XML with a {@code uuidref} identifier in the {@code <gmd:series>} element.
     */
    private static final String REFERENCED_XML =
            "<gmd:CI_Citation xmlns:gmd=\""   + Namespaces.GMD + '"' +
                            " xmlns:gco=\""   + Namespaces.GCO + "\">\n" +
            "  <gmd:title>\n" +
            "    <gco:CharacterString>My data</gco:CharacterString>\n" +
            "  </gmd:title>\n" +
            "  <gmd:series uuidref=\"" + UUID_VALUE + "\"/>\n" +
            "</gmd:CI_Citation>";

    /**
     * Tests (un)marshalling of an object identified by the {@code uuid} attribute.
     * The element of interest for this test is the {@code "uuid"} attribute value
     * in the {@code <gmd:CI_Series>} element of the following XML fragment:
     *
     * {@preformat xml
     *   <gmd:CI_Citation>
     *     <gmd:title>
     *       <gco:CharacterString>My data</gco:CharacterString>
     *     </gmd:title>
     *     <gmd:series>
     *       <gmd:CI_Series uuid="f8f5fcb1-d57b-4013-b3a4-4eaa40df6dcf">
     *         <gmd:name>
     *           <gco:CharacterString>My aggregate dataset</gco:CharacterString>
     *         </gmd:name>
     *       </gmd:CI_Series>
     *     </gmd:series>
     *   </gmd:CI_Citation>
     * }
     *
     * On an implementation note, the {@code uuid} and other attributes of the {@code <gmd:CI_Series>}
     * elements are handled by {@link org.apache.sis.internal.jaxb.gco.PropertyType}.
     *
     * @throws JAXBException Should never happen.
     */
    @Test
    public void testIdentification() throws JAXBException {
        final Citation citation = (Citation) XML.unmarshal(IDENTIFIED_XML);
        assertTitleEquals("Citation", "My data", citation);
        /*
         * Programmatic verification of the Series properties,
         * which is the main object of interest in this test.
         */
        final Series series = citation.getSeries();
        assertFalse("Unexpected proxy", Proxy.isProxyClass(series.getClass()));
        assertInstanceOf("Expected IdentifiedObject", IdentifiedObject.class, series);
        final IdentifierMap map = ((IdentifiedObject) series).getIdentifierMap();
        assertEquals("series", "My aggregate dataset",  series.getName().toString());
        assertNull  ("href", map.get(IdentifierSpace.HREF));
        assertEquals(UUID_VALUE, String.valueOf(map.get(IdentifierSpace.UUID)));
        /*
         * Marshal the object back to XML and compare with the original string
         * supplied to this method.
         */
        final String actual = XML.marshal(citation);
        assertXmlEquals(IDENTIFIED_XML, actual, "xmlns:*");
        assertEquals(citation, XML.unmarshal(actual));
    }

    /**
     * Tests (un)marshalling of an object referenced by a {@code uuidref} attribute.
     * This test does not try to resolve the reference, but only check that the identifier is properly saved.
     *
     * <p>The element of interest for this test is the {@code "uuidref"} part
     * in the {@code <gmd:series>} property of the following XML fragment:</p>
     *
     * {@preformat xml
     *   <gmd:CI_Citation>
     *     <gmd:title>
     *       <gco:CharacterString>My data</gco:CharacterString>
     *     </gmd:title>
     *     <gmd:series uuidref="f8f5fcb1-d57b-4013-b3a4-4eaa40df6dcf">
     *       <gmd:CI_Series>
     *         <gmd:name>
     *           <gco:CharacterString>My aggregate dataset</gco:CharacterString>
     *         </gmd:name>
     *       </gmd:CI_Series>
     *     </gmd:series>
     *   </gmd:CI_Citation>
     * }
     *
     * On an implementation note, the {@code uuidref}, {@code xlink:href} and other attributes of the
     * {@code <gmd:series>} element are handled by {@link org.apache.sis.internal.jaxb.gco.PropertyType}.
     *
     * @throws JAXBException Should never happen.
     */
    @Test
    public void testReference() throws JAXBException {
        final Citation citation = (Citation) XML.unmarshal(REFERENCED_XML_WITH_BODY);
        assertTitleEquals("Citation.title", "My data", citation);
        /*
         * Programmatic verification of the Series properties,
         * which is the main object of interest in this test.
         */
        final Series series = citation.getSeries();
        assertInstanceOf("Citation.series", IdentifiedObject.class, series);
        assertFalse     ("Citation.series.isProxy", Proxy.isProxyClass(series.getClass()));
        assertEquals    ("Citation.series.name", "My aggregate dataset", series.getName().toString());
        final IdentifierMap map = ((IdentifiedObject) series).getIdentifierMap();
        assertNull  ("href",             map.get(IdentifierSpace.HREF));
        assertEquals("uuid", UUID_VALUE, map.get(IdentifierSpace.UUID));
        /*
         * Marshal the object back to XML and compare with the expected result. The result shall be
         * slightly different than the original XML, since the UUID in the <gmd:series> element shall
         * move to the <gmd:CI_Series> element. This is the expected behavior because we have a fully
         * constructed object, not a reference to an object defined elsewhere.
         */
        final String actual = XML.marshal(citation);
        assertXmlEquals(IDENTIFIED_XML, actual, "xmlns:*");
        assertEquals(citation, XML.unmarshal(actual));
    }

    /**
     * The same test than {@link #testReference()}, except that the {@code <gmd:CI_Series>} element is empty.
     * This situation shall force the creation of a new, empty, element for storing the {@code uuidref} information.
     *
     * @throws JAXBException Should never happen.
     */
    @Test
    @DependsOnMethod("testReference")
    public void testReferenceInEmptyObject() throws JAXBException {
        final Citation citation = (Citation) XML.unmarshal(REFERENCED_XML);
        assertTitleEquals("Citation.title", "My data", citation);
        /*
         * Programmatic verification of the Series properties,
         * which is the main object of interest in this test.
         */
        final Series series = citation.getSeries();
        assertInstanceOf("Citation.series", IdentifiedObject.class, series);
        assertNull      ("Citation.series.name", series.getName());
        assertTrue      ("Citation.series.isProxy", Proxy.isProxyClass(series.getClass()));
        assertInstanceOf("Citation.series", NilObject.class, series);
        assertEquals    ("Series[{gco:uuid=“" + UUID_VALUE + "”}]", series.toString());
        final IdentifierMap map = ((IdentifiedObject) series).getIdentifierMap();
        assertNull  ("href",             map.get(IdentifierSpace.HREF));
        assertEquals("uuid", UUID_VALUE, map.get(IdentifierSpace.UUID));
        /*
         * Marshal the object back to XML and compare with the expected result.
         */
        final String actual = XML.marshal(citation);
        assertXmlEquals(REFERENCED_XML, actual, "xmlns:*");
        assertEquals(citation, XML.unmarshal(actual));
    }
}
