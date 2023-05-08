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
import org.apache.sis.test.xml.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.metadata.Assertions.assertTitleEquals;
import static org.apache.sis.metadata.Assertions.assertXmlEquals;
import static org.opengis.test.Assert.assertInstanceOf;


/**
 * Tests the XML marshalling of object having {@code uuid} or {@code uuidref} attributes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 */
@DependsOn(NilReasonMarshallingTest.class)
public final class UUIDMarshallingTest extends TestCase {
    /**
     * A random UUID for the tests in this class.
     */
    private static final String UUID_VALUE = "f8f5fcb1-d57b-4013-b3a4-4eaa40df6dcf";

    /**
     * A XML with a {@code uuid} identifier in the {@code <cit:CI_Series>} element.
     */
    private static final String IDENTIFIED_XML =
            "<cit:CI_Citation xmlns:cit=\"" + Namespaces.CIT + '"' +
                            " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
            "  <cit:title>\n" +
            "    <gco:CharacterString>My data</gco:CharacterString>\n" +
            "  </cit:title>\n" +
            "  <cit:series>\n" +
            "    <cit:CI_Series uuid=\"" + UUID_VALUE + "\">\n" +
            "      <cit:name>\n" +
            "        <gco:CharacterString>My aggregate dataset</gco:CharacterString>\n" +
            "      </cit:name>\n" +
            "    </cit:CI_Series>\n" +
            "  </cit:series>\n" +
            "</cit:CI_Citation>";

    /**
     * A XML with a {@code uuidref} identifier in the {@code <cit:series>} element.
     * This XML declares the method body anyway, which is kind of contradictory with usage of reference.
     */
    private static final String REFERENCED_XML_WITH_BODY =
            "<cit:CI_Citation xmlns:cit=\"" + Namespaces.CIT + '"' +
                            " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
            "  <cit:title>\n" +
            "    <gco:CharacterString>My data</gco:CharacterString>\n" +
            "  </cit:title>\n" +
            "  <cit:series uuidref=\"" + UUID_VALUE + "\">\n" +
            "    <cit:CI_Series>\n" +
            "      <cit:name>\n" +
            "        <gco:CharacterString>My aggregate dataset</gco:CharacterString>\n" +
            "      </cit:name>\n" +
            "    </cit:CI_Series>\n" +
            "  </cit:series>\n" +
            "</cit:CI_Citation>";

    /**
     * A XML with a {@code uuidref} identifier in the {@code <cit:series>} element.
     */
    private static final String REFERENCED_XML =
            "<cit:CI_Citation xmlns:cit=\"" + Namespaces.CIT + '"' +
                            " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
            "  <cit:title>\n" +
            "    <gco:CharacterString>My data</gco:CharacterString>\n" +
            "  </cit:title>\n" +
            "  <cit:series uuidref=\"" + UUID_VALUE + "\"/>\n" +
            "</cit:CI_Citation>";

    /**
     * Tests (un)marshalling of an object identified by the {@code uuid} attribute.
     * The element of interest for this test is the {@code "uuid"} attribute value
     * in the {@code <cit:CI_Series>} element of the following XML fragment:
     *
     * {@snippet lang="xml" :
     *   <cit:CI_Citation>
     *     <cit:title>
     *       <gco:CharacterString>My data</gco:CharacterString>
     *     </cit:title>
     *     <cit:series>
     *       <cit:CI_Series uuid="f8f5fcb1-d57b-4013-b3a4-4eaa40df6dcf">
     *         <cit:name>
     *           <gco:CharacterString>My aggregate dataset</gco:CharacterString>
     *         </cit:name>
     *       </cit:CI_Series>
     *     </cit:series>
     *   </cit:CI_Citation>
     *   }
     *
     * On an implementation note, the {@code uuid} and other attributes of the {@code <cit:CI_Series>}
     * elements are handled by {@link org.apache.sis.internal.jaxb.gco.PropertyType}.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
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
     * in the {@code <cit:series>} property of the following XML fragment:</p>
     *
     * {@snippet lang="xml" :
     *   <cit:CI_Citation>
     *     <cit:title>
     *       <gco:CharacterString>My data</gco:CharacterString>
     *     </cit:title>
     *     <cit:series uuidref="f8f5fcb1-d57b-4013-b3a4-4eaa40df6dcf">
     *       <cit:CI_Series>
     *         <cit:name>
     *           <gco:CharacterString>My aggregate dataset</gco:CharacterString>
     *         </cit:name>
     *       </cit:CI_Series>
     *     </cit:series>
     *   </cit:CI_Citation>
     *   }
     *
     * On an implementation note, the {@code uuidref}, {@code xlink:href} and other attributes of the
     * {@code <cit:series>} element are handled by {@link org.apache.sis.internal.jaxb.gco.PropertyType}.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
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
         * slightly different than the original XML, since the UUID in the <cit:series> element shall
         * move to the <cit:CI_Series> element. This is the expected behavior because we have a fully
         * constructed object, not a reference to an object defined elsewhere.
         */
        final String actual = XML.marshal(citation);
        assertXmlEquals(IDENTIFIED_XML, actual, "xmlns:*");
        assertEquals(citation, XML.unmarshal(actual));
    }

    /**
     * The same test than {@link #testReference()}, except that the {@code <cit:CI_Series>} element is empty.
     * This situation shall force the creation of a new, empty, element for storing the {@code uuidref} information.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
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
