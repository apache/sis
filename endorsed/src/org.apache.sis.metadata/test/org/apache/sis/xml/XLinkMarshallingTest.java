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

import java.util.Set;
import java.net.URI;
import java.net.URISyntaxException;
import jakarta.xml.bind.JAXBException;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.ComparisonMode;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.xml.test.TestCase;
import static org.apache.sis.metadata.Assertions.assertXmlEquals;
import static org.apache.sis.test.Assertions.assertSingleton;


/**
 * Tests the XML marshalling of object having {@code xlink} attribute.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class XLinkMarshallingTest extends TestCase {
    /**
     * A XML with a {@code xlink:href} without element definition.
     */
    private static final String LINK_ONLY_XML =
            "<mdb:MD_Metadata xmlns:mdb=\""   + Namespaces.MDB + '"' +
                            " xmlns:xlink=\"" + Namespaces.XLINK + "\">\n" +
            "  <mdb:identificationInfo xlink:href=\"" + DUMMY_URL + "\"/>\n" +
            "</mdb:MD_Metadata>";

    /**
     * A XML with a {@code xlink:href} without element definition.
     */
    private static final String LINK_WITH_ELEMENT_XML =
            "<mdb:MD_Metadata xmlns:mdb=\""   + Namespaces.MDB + '"' +
                            " xmlns:mri=\""   + Namespaces.MRI + '"' +
                            " xmlns:gco=\""   + Namespaces.GCO + '"' +
                            " xmlns:xlink=\"" + Namespaces.XLINK + "\">\n" +
            " <mdb:identificationInfo xlink:href=\"" + DUMMY_URL + "\">\n" +
            "    <mri:MD_DataIdentification>\n" +
            "      <mri:abstract>\n" +
            "        <gco:CharacterString>This is a test.</gco:CharacterString>\n" +
            "      </mri:abstract>\n" +
            "    </mri:MD_DataIdentification>\n" +
            "  </mdb:identificationInfo>\n" +
            "</mdb:MD_Metadata>";

    /**
     * Verifies if the given metadata contains the expected {@code xlink:href} attribute value.
     *
     * @param  isNilExpected  {@code true} if the identification info is expected to be a {@link NilObject} instance.
     * @param  metadata       the metadata to verify.
     */
    private static void verify(final boolean isNilExpected, final DefaultMetadata metadata) {
        final IdentifiedObject identification = assertInstanceOf(
                IdentifiedObject.class,
                assertSingleton(metadata.getIdentificationInfo()));
        assertEquals(isNilExpected, identification instanceof NilObject, "NilObject");
        final XLink xlink = identification.getIdentifierMap().getSpecialized(IdentifierSpace.XLINK);
        assertEquals(DUMMY_URL, xlink.getHRef().toString(), "xlink:href");
    }

    /**
     * Creates a new test case.
     */
    public XLinkMarshallingTest() {
    }

    /**
     * Tests (un)marshalling of an object with a {@code xlink:href} attribute without element definition.
     * The XML fragment is:
     *
     * {@snippet lang="xml" :
     *   <mdb:MD_Metadata>
     *     <mdb:identificationInfo xlink:href="http://test.net"/>
     *   </mdb:MD_Metadata>
     *   }
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     * @throws URISyntaxException if the URI used in this test is malformed.
     */
    @Test
    public void testLinkOnly() throws JAXBException, URISyntaxException {
        final XLink xlink = new XLink();
        xlink.setHRef(new URI(DUMMY_URL));
        final DefaultDataIdentification identification = new DefaultDataIdentification();
        identification.getIdentifierMap().putSpecialized(IdentifierSpace.XLINK, xlink);
        final DefaultMetadata metadata = new DefaultMetadata();
        metadata.setIdentificationInfo(Set.of(identification));

        assertXmlEquals(LINK_ONLY_XML, marshal(metadata), "xmlns:*");
        verify(true, unmarshal(DefaultMetadata.class, LINK_ONLY_XML));
    }

    /**
     * Tests (un)marshalling of an object with a {@code xlink:href} attribute with an element definition.
     * The XML fragment is:
     *
     * {@snippet lang="xml" :
     *   <mdb:MD_Metadata>
     *     <mdb:identificationInfo xlink:href="http://test.net">
     *       <mdb:MD_DataIdentification>
     *         <mdb:abstract>
     *           <gco:CharacterString>This is a test.</gco:CharacterString>
     *         </mdb:abstract>
     *       </mdb:MD_DataIdentification>
     *     </mdb:identificationInfo>
     *   </mdb:MD_Metadata>
     *   }
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     * @throws URISyntaxException if the URI used in this test is malformed.
     */
    @Test
    public void testWithElement() throws JAXBException, URISyntaxException {
        final XLink xlink = new XLink();
        xlink.setHRef(new URI(DUMMY_URL));
        final DefaultDataIdentification identification = new DefaultDataIdentification();
        identification.getIdentifierMap().putSpecialized(IdentifierSpace.XLINK, xlink);
        identification.setAbstract(new SimpleInternationalString("This is a test."));
        final DefaultMetadata metadata = new DefaultMetadata();
        metadata.setIdentificationInfo(Set.of(identification));

        assertXmlEquals(LINK_WITH_ELEMENT_XML, marshal(metadata), "xmlns:*");
        final DefaultMetadata unmarshal = unmarshal(DefaultMetadata.class, LINK_WITH_ELEMENT_XML);
        verify(false, unmarshal);
        assertTrue(metadata.equals(unmarshal, ComparisonMode.DEBUG));
    }
}
