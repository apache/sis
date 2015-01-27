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

import java.util.Collections;
import java.net.URI;
import java.net.URISyntaxException;
import javax.xml.bind.JAXBException;
import org.opengis.metadata.identification.Identification;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests the XML marshalling of object having {@code xlink} attribute.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 *
 * @see <a href="http://jira.geotoolkit.org/browse/GEOTK-165">GEOTK-165</a>
 */
@DependsOn(NilReasonMarshallingTest.class)
public final strictfp class XLinkMarshallingTest extends XMLTestCase {
    /**
     * A XML with a {@code xlink:href} without element definition.
     */
    private static final String LINK_ONLY_XML =
            "<gmd:MD_Metadata xmlns:gmd=\""   + Namespaces.GMD + '"' +
                            " xmlns:xlink=\"" + Namespaces.XLINK + "\">\n" +
            "  <gmd:identificationInfo xlink:href=\"http://test.net\"/>\n" +
            "</gmd:MD_Metadata>";

    /**
     * A XML with a {@code xlink:href} without element definition.
     */
    private static final String LINK_WITH_ELEMENT_XML =
            "<gmd:MD_Metadata xmlns:gmd=\""   + Namespaces.GMD + '"' +
                            " xmlns:gco=\""   + Namespaces.GCO + '"' +
                            " xmlns:xlink=\"" + Namespaces.XLINK + "\">\n" +
            "  <gmd:identificationInfo xlink:href=\"http://test.net\">\n" +
            "    <gmd:MD_DataIdentification>\n" +
            "      <gmd:abstract>\n" +
            "        <gco:CharacterString>This is a test.</gco:CharacterString>\n" +
            "      </gmd:abstract>\n" +
            "    </gmd:MD_DataIdentification>\n" +
            "  </gmd:identificationInfo>\n" +
            "</gmd:MD_Metadata>";

    /**
     * Verifies if the given metadata contains the expected {@code xlink:href} attribute value.
     *
     * @param isNilExpected {@code true} if the identification info is expected to be a {@link NilObject} instance.
     * @param metadata The metadata to verify.
     */
    private static void verify(final boolean isNilExpected, final DefaultMetadata metadata) {
        final Identification identification = getSingleton(metadata.getIdentificationInfo());
        assertEquals("NilObject", isNilExpected, identification instanceof NilObject);
        assertInstanceOf("Identification", IdentifiedObject.class, identification);
        final XLink xlink = ((IdentifiedObject) identification).getIdentifierMap().getSpecialized(IdentifierSpace.XLINK);
        assertEquals("xlink:href", "http://test.net", xlink.getHRef().toString());
    }

    /**
     * Tests (un)marshalling of an object with a {@code xlink:href} attribute without element definition.
     * The XML fragment is:
     *
     * {@preformat xml
     *   <gmd:MD_Metadata>
     *     <gmd:identificationInfo xlink:href="http://test.net"/>
     *   </gmd:MD_Metadata>
     * }
     *
     * @throws JAXBException Should never happen.
     * @throws URISyntaxException Should never happen.
     */
    @Test
    public void testLinkOnly() throws JAXBException, URISyntaxException {
        final XLink xlink = new XLink();
        xlink.setHRef(new URI("http://test.net"));
        final DefaultDataIdentification identification = new DefaultDataIdentification();
        identification.getIdentifierMap().putSpecialized(IdentifierSpace.XLINK, xlink);
        final DefaultMetadata metadata = new DefaultMetadata();
        metadata.setIdentificationInfo(Collections.singleton(identification));

        assertXmlEquals(LINK_ONLY_XML, XML.marshal(metadata), "xmlns:*");
        verify(true, unmarshal(DefaultMetadata.class, LINK_ONLY_XML));
    }

    /**
     * Tests (un)marshalling of an object with a {@code xlink:href} attribute with an element definition.
     * The XML fragment is:
     *
     * {@preformat xml
     *   <gmd:MD_Metadata>
     *     <gmd:identificationInfo xlink:href="http://test.net">
     *       <gmd:MD_DataIdentification>
     *         <gmd:abstract>
     *           <gco:CharacterString>This is a test.</gco:CharacterString>
     *         </gmd:abstract>
     *       </gmd:MD_DataIdentification>
     *     </gmd:identificationInfo>
     *   </gmd:MD_Metadata>
     * }
     *
     * @throws JAXBException Should never happen.
     * @throws URISyntaxException Should never happen.
     */
    @Test
    public void testWithElement() throws JAXBException, URISyntaxException {
        final XLink xlink = new XLink();
        xlink.setHRef(new URI("http://test.net"));
        final DefaultDataIdentification identification = new DefaultDataIdentification();
        identification.getIdentifierMap().putSpecialized(IdentifierSpace.XLINK, xlink);
        identification.setAbstract(new SimpleInternationalString("This is a test."));
        final DefaultMetadata metadata = new DefaultMetadata();
        metadata.setIdentificationInfo(Collections.singleton(identification));

        assertXmlEquals(LINK_WITH_ELEMENT_XML, XML.marshal(metadata), "xmlns:*");
        final DefaultMetadata unmarshal = unmarshal(DefaultMetadata.class, LINK_WITH_ELEMENT_XML);
        verify(false, unmarshal);
        assertTrue(metadata.equals(unmarshal, ComparisonMode.DEBUG));
    }
}
