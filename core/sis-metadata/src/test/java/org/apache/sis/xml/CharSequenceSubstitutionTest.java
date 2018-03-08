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
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.internal.jaxb.LegacyNamespaces;
import org.apache.sis.internal.metadata.SensorType;
import org.apache.sis.util.iso.Types;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;

// Branch-dependent imports
import org.opengis.referencing.ReferenceIdentifier;


/**
 * Tests the XML marshalling of {@code Anchor} and {@code CodeList} as substitution of {@code <gco:CharacterSequence>}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 * @since   0.3
 * @module
 */
public final strictfp class CharSequenceSubstitutionTest extends XMLTestCase {
    /**
     * Tests unmarshalling of {@code "RS_Identifier"} element. This element was defined in legacy ISO 19139:2007
     * but has been removed in ISO 19115-3. That element is extensively used for Coordinate Reference Systems in
     * GML 3.2.
     *
     * @throws JAXBException if the unmarshalling failed.
     */
    @Test
    @DependsOnMethod("testAnchorForString")
    public void testLegacy() throws JAXBException {
        final String expected =
                "<gmd:RS_Identifier xmlns:gmd=\""   + LegacyNamespaces.GMD + '"' +
                                  " xmlns:gmx=\""   + LegacyNamespaces.GMX + '"' +
                                  " xmlns:gco=\""   + LegacyNamespaces.GCO + '"' +
                                  " xmlns:xlink=\"" + Namespaces.XLINK + "\">\n" +
                "  <gmd:code>\n" +
                "    <gmx:Anchor xlink:href=\"SDN:L101:2:4326\">EPSG:4326</gmx:Anchor>\n" +
                "  </gmd:code>\n" +
                "  <gmd:codeSpace>\n" +
                "    <gco:CharacterString>L101</gco:CharacterString>\n" +
                "  </gmd:codeSpace>\n" +
                "</gmd:RS_Identifier>";

        final ReferenceIdentifier id = unmarshal(ReferenceIdentifier.class, expected);
        assertEquals("codespace", "L101", id.getCodeSpace());
        assertEquals("code", "EPSG:4326", id.getCode());
    }

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
                "<mdb:MD_Identifier xmlns:mdb=\""   + Namespaces.MDB + '"' +
                                  " xmlns:gcx=\""   + Namespaces.GCX + '"' +
                                  " xmlns:gco=\""   + Namespaces.GCO + '"' +
                                  " xmlns:xlink=\"" + Namespaces.XLINK + "\">\n" +
                "  <mdb:code>\n" +
                "    <gcx:Anchor xlink:href=\"SDN:L101:2:4326\">EPSG:4326</gcx:Anchor>\n" +
                "  </mdb:code>\n" +
                "  <mdb:codeSpace>\n" +
                "    <gco:CharacterString>L101</gco:CharacterString>\n" +
                "  </mdb:codeSpace>\n" +
                "</mdb:MD_Identifier>";

        final DefaultIdentifier id = unmarshal(DefaultIdentifier.class, expected);
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
                "<cit:CI_Address xmlns:cit=\""   + Namespaces.CIT   + '"' +
                               " xmlns:gcx=\""   + Namespaces.GCX   + '"' +
                               " xmlns:gco=\""   + Namespaces.GCO   + '"' +
                               " xmlns:xlink=\"" + Namespaces.XLINK + "\">\n" +
                "  <cit:deliveryPoint>\n" +
                "    <gco:CharacterString>Centre IFREMER de Brest BP 70</gco:CharacterString>\n" +
                "  </cit:deliveryPoint>\n" +
                "  <cit:city>\n" +
                "    <gco:CharacterString>Plouzané</gco:CharacterString>\n" +
                "  </cit:city>\n" +
                "  <cit:postalCode>\n" +
                "    <gco:CharacterString>29280</gco:CharacterString>\n" +
                "  </cit:postalCode>\n" +
                "  <cit:country>\n" +
                "    <gcx:Anchor xlink:href=\"SDN:C320:2:FR\">France</gcx:Anchor>\n" +
                "  </cit:country>\n" +
                "  <cit:electronicMailAddress>\n" +
                "    <gco:CharacterString>(hiden)@ifremer.fr</gco:CharacterString>\n" +
                "  </cit:electronicMailAddress>\n" +
                "</cit:CI_Address>";

        final Address address = unmarshal(Address.class, expected);
        assertEquals("Plouzané", address.getCity().toString());
        assertEquals("France", address.getCountry().toString());
        assertEquals(1, address.getElectronicMailAddresses().size());

        final XLink anchor = (XLink) address.getCountry();
        assertEquals("France", anchor.toString());
        assertEquals("SDN:C320:2:FR", anchor.getHRef().toString());
        assertNull(anchor.getType());

        anchor.setType(XLink.Type.AUTO);
        assertEquals(XLink.Type.LOCATOR, anchor.getType());

        final String actual = marshal(address);
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
                "<mri:MD_DataIdentification xmlns:mri=\"" + Namespaces.MRI + "\">\n" +
                "  <mri:purpose>\n" +
                "    <mri:DS_InitiativeTypeCode\n" +
                "        codeList=\"http://standards.iso.org/iso/19115/resources/Codelist/cat/codelists.xml#DS_InitiativeTypeCode\"\n" +
                "        codeListValue=\"investigation\">Investigation</mri:DS_InitiativeTypeCode>\n" +
                "  </mri:purpose>\n" +
                "</mri:MD_DataIdentification>";

        final DataIdentification id = unmarshal(DataIdentification.class, expected);
        assertEquals("purpose", "Investigation", String.valueOf(id.getPurpose()));
        assertSame("purpose", InitiativeType.INVESTIGATION, Types.forCodeTitle(id.getPurpose()));

        final String actual = marshal(id);
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

        final Instrument instrument = unmarshal(Instrument.class, expected);
        assertEquals("type", "RADIOMETER", String.valueOf(instrument.getType()));
        assertInstanceOf("type", SensorType.class, Types.forCodeTitle(instrument.getType()));
    }
}
