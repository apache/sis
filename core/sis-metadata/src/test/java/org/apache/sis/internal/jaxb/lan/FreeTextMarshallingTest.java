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
package org.apache.sis.internal.jaxb.lan;

import java.util.Locale;
import javax.xml.bind.JAXBException;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.util.iso.DefaultInternationalString;
import org.apache.sis.internal.xml.LegacyNamespaces;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.test.xml.TestCase;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests the XML marshalling of {@code FreeText}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
public final strictfp class FreeTextMarshallingTest extends TestCase {
    /**
     * Returns the expected string.
     */
    private static DefaultInternationalString getExpectedI18N() {
        final DefaultInternationalString i18n = new DefaultInternationalString();
        i18n.add(Locale.ENGLISH, "OpenSource Project");
        i18n.add(Locale.FRENCH,  "Projet OpenSource");
        i18n.add(Locale.ITALIAN, "Progetto OpenSource");
        return i18n;
    }

    /**
     * Tests parsing of a free text in an ISO 19139 compliant way.
     * The free text is wrapped inside a citation for marshalling
     * purpose, but only the free text is actually tested.
     *
     * @throws JAXBException if the XML in this test can not be parsed by JAXB.
     */
    @Test
    public void testLegacy() throws JAXBException {
        final String expected =
                "<gmd:CI_Citation xmlns:gmd=\"" + LegacyNamespaces.GMD + '"'
                              + " xmlns:gco=\"" + LegacyNamespaces.GCO + '"'
                              + " xmlns:xsi=\"" + Namespaces.XSI + "\">\n" +
                "  <gmd:title xsi:type=\"gmd:PT_FreeText_PropertyType\">\n" +
                "    <gco:CharacterString>OpenSource Project</gco:CharacterString>\n" +
                "    <gmd:PT_FreeText>\n" +
                "      <gmd:textGroup>\n" +
                "        <gmd:LocalisedCharacterString locale=\"#locale-eng\">OpenSource Project</gmd:LocalisedCharacterString>\n" +
                "      </gmd:textGroup>\n" +
                "      <gmd:textGroup>\n" +
                "        <gmd:LocalisedCharacterString locale=\"#locale-ita\">Progetto OpenSource</gmd:LocalisedCharacterString>\n" +
                "      </gmd:textGroup>\n" +
                "      <gmd:textGroup>\n" +
                "        <gmd:LocalisedCharacterString locale=\"#locale-fra\">Projet OpenSource</gmd:LocalisedCharacterString>\n" +
                "      </gmd:textGroup>\n" +
                "    </gmd:PT_FreeText>\n" +
                "  </gmd:title>\n" +
                "</gmd:CI_Citation>\n";

        final Citation citation = unmarshal(Citation.class, expected);
        assertEquals(getExpectedI18N(), citation.getTitle());
        final String actual = marshal(citation, VERSION_2007);
        assertXmlEquals(expected, actual, "xmlns:*");
    }

    /**
     * Tests parsing of a free text in an ISO 19115-3 compliant way.
     * The free text is wrapped inside a citation for marshalling
     * purpose, but only the free text is actually tested.
     *
     * @throws JAXBException if the XML in this test can not be parsed by JAXB.
     */
    @Test
    public void testStandard() throws JAXBException {
        final String expected =
                "<cit:CI_Citation xmlns:lan=\"" + Namespaces.LAN + '"'
                              + " xmlns:cit=\"" + Namespaces.CIT + '"'
                              + " xmlns:gco=\"" + Namespaces.GCO + '"'
                              + " xmlns:xsi=\"" + Namespaces.XSI + "\">\n" +
                "  <cit:title xsi:type=\"lan:PT_FreeText_PropertyType\">\n" +
                "    <gco:CharacterString>OpenSource Project</gco:CharacterString>\n" +
                "    <lan:PT_FreeText>\n" +
                "      <lan:textGroup>\n" +
                "        <lan:LocalisedCharacterString locale=\"#locale-eng\">OpenSource Project</lan:LocalisedCharacterString>\n" +
                "      </lan:textGroup>\n" +
                "      <lan:textGroup>\n" +
                "        <lan:LocalisedCharacterString locale=\"#locale-ita\">Progetto OpenSource</lan:LocalisedCharacterString>\n" +
                "      </lan:textGroup>\n" +
                "      <lan:textGroup>\n" +
                "        <lan:LocalisedCharacterString locale=\"#locale-fra\">Projet OpenSource</lan:LocalisedCharacterString>\n" +
                "      </lan:textGroup>\n" +
                "    </lan:PT_FreeText>\n" +
                "  </cit:title>\n" +
                "</cit:CI_Citation>\n";

        final Citation citation = unmarshal(Citation.class, expected);
        assertEquals(getExpectedI18N(), citation.getTitle());
        final String actual = marshal(citation);
        assertXmlEquals(expected, actual, "xmlns:*");
    }

    /**
     * Tests parsing of a free text in a non-standard variant.
     * We continue to support this format for compatibility reason, but
     * also because it is more compact and closer to what we would expect
     * inside a {@code <textGroup>} node.
     *
     * @throws JAXBException if the XML in this test can not be parsed by JAXB.
     */
    @Test
    public void testNonStandard() throws JAXBException {
        final String legacy =
                "<cit:CI_Citation xmlns:lan=\"" + Namespaces.LAN + '"'
                              + " xmlns:cit=\"" + Namespaces.CIT + '"'
                              + " xmlns:gco=\"" + Namespaces.GCO + '"'
                              + " xmlns:xsi=\"" + Namespaces.XSI + "\">\n" +
                "  <cit:title xsi:type=\"lan:PT_FreeText_PropertyType\">\n" +
                "    <gco:CharacterString>OpenSource Project</gco:CharacterString>\n" +
                "    <lan:PT_FreeText>\n" +
                "      <lan:textGroup>\n" +
                "        <lan:LocalisedCharacterString locale=\"#locale-eng\">OpenSource Project</lan:LocalisedCharacterString>\n" +
                "        <lan:LocalisedCharacterString locale=\"#locale-ita\">Progetto OpenSource</lan:LocalisedCharacterString>\n" +
                "        <lan:LocalisedCharacterString locale=\"#locale-fra\">Projet OpenSource</lan:LocalisedCharacterString>\n" +
                "      </lan:textGroup>\n" +
                "    </lan:PT_FreeText>\n" +
                "  </cit:title>\n" +
                "</cit:CI_Citation>\n";

        final Citation citation = unmarshal(Citation.class, legacy);
        assertEquals(getExpectedI18N(), citation.getTitle());
    }
}
