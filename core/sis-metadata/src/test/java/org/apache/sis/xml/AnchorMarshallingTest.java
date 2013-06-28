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

import javax.xml.bind.JAXBException;
import org.opengis.metadata.citation.Address;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the XML marshalling of {@code Anchor}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.18)
 * @version 0.3
 * @module
 */
public final strictfp class AnchorMarshallingTest extends XMLTestCase {
    /**
     * Tests the anchor in an identifier element. Note that the {@code xlink:href}
     * attribute is lost, because the Java type of the {@code gmd:code} attribute
     * is {@link String}.
     *
     * @throws JAXBException Should never happen.
     */
    @Test
    public void testIdentifier() throws JAXBException {
        final String expected =
                "<gmd:RS_Identifier xmlns:gmx=\""   + Namespaces.GMX   + '"' +
                                  " xmlns:gmd=\""   + Namespaces.GMD   + '"' +
                                  " xmlns:gco=\""   + Namespaces.GCO   + '"' +
                                  " xmlns:xlink=\"" + Namespaces.XLINK + "\">\n" +
                "  <gmd:code>\n" +
                "    <gmx:Anchor xlink:href=\"SDN:L101:2:4326\">EPSG:4326</gmx:Anchor>\n" +
                "  </gmd:code>\n" +
                "  <gmd:codeSpace>\n" +
                "    <gco:CharacterString>L101</gco:CharacterString>\n" +
                "  </gmd:codeSpace>\n" +
                "</gmd:RS_Identifier>";
        final ReferenceIdentifier id = (ReferenceIdentifier) XML.unmarshal(expected);
        assertEquals("codespace", "L101", id.getCodeSpace());
        assertEquals("code", "EPSG:4326", id.getCode());
    }

    /**
     * Tests the anchor in an address element.
     *
     * @throws JAXBException Should never happen.
     */
    @Test
    public void testAddress() throws JAXBException {
        final String expected =
                "<gmd:CI_Address xmlns:gmx=\""   + Namespaces.GMX   + '"' +
                               " xmlns:gmd=\""   + Namespaces.GMD   + '"' +
                               " xmlns:gco=\""   + Namespaces.GCO   + '"' +
                               " xmlns:xlink=\"" + Namespaces.XLINK + "\">\n" +
                "  <gmd:deliveryPoint>\n" +
                "    <gco:CharacterString>Centre IFREMER de Brest BP 70</gco:CharacterString>\n" +
                "  </gmd:deliveryPoint>\n" +
                "  <gmd:city>\n" +
                "    <gco:CharacterString>Plouzané</gco:CharacterString>\n" +
                "  </gmd:city>\n" +
                "  <gmd:postalCode>\n" +
                "    <gco:CharacterString>29280</gco:CharacterString>\n" +
                "  </gmd:postalCode>\n" +
                "  <gmd:country>\n" +
                "    <gmx:Anchor xlink:href=\"SDN:C320:2:FR\">France</gmx:Anchor>\n" +
                "  </gmd:country>\n" +
                "  <gmd:electronicMailAddress>\n" +
                "    <gco:CharacterString>(hiden)@ifremer.fr</gco:CharacterString>\n" +
                "  </gmd:electronicMailAddress>\n" +
                "</gmd:CI_Address>";
        final Address address = (Address) XML.unmarshal(expected);
        assertEquals("Plouzané", address.getCity().toString());
        assertEquals("France", address.getCountry().toString());
        assertEquals(1, address.getElectronicMailAddresses().size());

        final XLink anchor = (XLink) address.getCountry();
        assertEquals("France", anchor.toString());
        assertEquals("SDN:C320:2:FR", anchor.getHRef().toString());
        assertNull(anchor.getType());

        anchor.setType(XLink.Type.AUTO);
        assertEquals(XLink.Type.LOCATOR, anchor.getType());

        final String actual = XML.marshal(address);
        assertXmlEquals(expected, actual, "xmlns:*");
    }
}
