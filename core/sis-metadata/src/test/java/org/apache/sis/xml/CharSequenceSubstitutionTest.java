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
import org.opengis.metadata.acquisition.Instrument;
import org.opengis.metadata.identification.DataIdentification;
import org.opengis.metadata.identification.InitiativeType;
import org.apache.sis.internal.metadata.SensorType;
import org.apache.sis.util.iso.Types;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;

// Branch-dependent imports
import org.opengis.referencing.ReferenceIdentifier;


/**
 * Tests the XML marshalling of {@code Anchor} and {@code CodeList} as substitution of {@code <gco:CharacterSequence>}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.7
 * @module
 */
public final strictfp class CharSequenceSubstitutionTest extends XMLTestCase {
    /**
     * Tests unmarshalling of anchor in an identifier element. The {@code xlink:href} attribute
     * is lost because the Java type of the {@code gmd:code} attribute is {@link String}.
     *
     * @throws JAXBException if the unmarshalling failed.
     */
    @Test
    @DependsOnMethod("testAnchor")
    public void testAnchorForString() throws JAXBException {
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
     * Tests the anchor in the country property of an address element.
     * Contrarily to {@link #testAnchorForString()}, this method can test both marshalling and unmarshalling.
     *
     * @throws JAXBException if the (un)marshalling failed.
     */
    @Test
    public void testAnchor() throws JAXBException {
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

    /**
     * Tests substitution by a code list.
     *
     * @throws JAXBException if the (un)marshalling failed.
     *
     * @since 0.7
     */
    @Test
    public void testCodeList() throws JAXBException {
        final String expected =
                "<gmd:MD_DataIdentification xmlns:gmd=\"" + Namespaces.GMD + "\">\n" +
                "  <gmd:purpose>\n" +
                "    <gmd:DS_InitiativeTypeCode\n" +
                "        codeList=\"http://schemas.opengis.net/iso/19139/20070417/resources/Codelist/gmxCodelists.xml#DS_InitiativeTypeCode\"\n" +
                "        codeListValue=\"investigation\">Investigation</gmd:DS_InitiativeTypeCode>\n" +
                "  </gmd:purpose>\n" +
                "</gmd:MD_DataIdentification>";

        final DataIdentification id = (DataIdentification) XML.unmarshal(expected);
        assertEquals("purpose", "Investigation", String.valueOf(id.getPurpose()));
        assertSame("purpose", InitiativeType.INVESTIGATION, Types.forCodeTitle(id.getPurpose()));

        final String actual = XML.marshal(id);
        assertXmlEquals(expected, actual, "xmlns:*");
    }

    /**
     * Tests the code list in a sensor element.
     *
     * @throws JAXBException if the (un)marshalling failed.
     *
     * @since 0.7
     */
    @Test
    @DependsOnMethod("testCodeList")
    public void testSensorCode() throws JAXBException {
        final String expected =
                "<gmi:MI_Instrument xmlns:gmd=\"" + Namespaces.GMD + '"' +
                                  " xmlns:gmi=\"" + Namespaces.GMI + "\">\n" +
                "  <gmi:type>\n" +
                "    <gmi:MI_SensorTypeCode\n" +
                "        codeList=\"http://navigator.eumetsat.int/metadata_schema/eum/resources/Codelist/eum_gmxCodelists.xml#CI_SensorTypeCode\"\n" +
                "        codeListValue=\"RADIOMETER\">RADIOMETER</gmi:MI_SensorTypeCode>\n" +
                "  </gmi:type>\n" +
                "</gmi:MI_Instrument>";

        final Instrument instrument = (Instrument) XML.unmarshal(expected);
        assertEquals("type", "RADIOMETER", String.valueOf(instrument.getType()));
        assertInstanceOf("type", SensorType.class, Types.forCodeTitle(instrument.getType()));
    }
}
