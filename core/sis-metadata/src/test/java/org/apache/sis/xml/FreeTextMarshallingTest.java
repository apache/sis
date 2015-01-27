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

import java.util.Locale;
import javax.xml.bind.JAXBException;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.util.iso.DefaultInternationalString;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the XML marshalling of {@code FreeText}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see <a href="http://jira.geotoolkit.org/browse/GEOTK-107">GEOTK-107</a>
 * @see <a href="http://jira.geotoolkit.org/browse/GEOTK-152">GEOTK-152</a>
 */
public final strictfp class FreeTextMarshallingTest extends XMLTestCase {
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
     * Tests parsing of a free text in an ISO 19139-compliant way.
     * The free text is wrapped inside a citation for marshalling
     * purpose, but only the free text is actually tested.
     *
     * @throws JAXBException If the XML in this test can not be parsed by JAXB.
     */
    @Test
    public void testStandard() throws JAXBException {
        final String expected =
                "<gmd:CI_Citation xmlns:gmd=\"" + Namespaces.GMD + "\" xmlns:gco=\"" + Namespaces.GCO + "\" xmlns:xsi=\"" + Namespaces.XSI + "\">\n" +
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

        final Citation citation = (Citation) XML.unmarshal(expected);
        assertEquals(getExpectedI18N(), citation.getTitle());
        final String actual = XML.marshal(citation);
        assertXmlEquals(expected, actual, "xmlns:*");
    }

    /**
     * Tests parsing of a free text in the legacy (pre-Geotk 3.17) format.
     * We continue to support this format for compatibility reason, but
     * also because it is more compact and closer to what we would expect
     * inside a {@code <textGroup>} node.
     *
     * @throws JAXBException If the XML in this test can not be parsed by JAXB.
     */
    @Test
    public void testLegacy() throws JAXBException {
        final String legacy =
                "<gmd:CI_Citation xmlns:gmd=\"" + Namespaces.GMD + "\" xmlns:gco=\"" + Namespaces.GCO + "\" xmlns:xsi=\"" + Namespaces.XSI + "\">\n" +
                "  <gmd:title xsi:type=\"gmd:PT_FreeText_PropertyType\">\n" +
                "    <gco:CharacterString>OpenSource Project</gco:CharacterString>\n" +
                "    <gmd:PT_FreeText>\n" +
                "      <gmd:textGroup>\n" +
                "        <gmd:LocalisedCharacterString locale=\"#locale-eng\">OpenSource Project</gmd:LocalisedCharacterString>\n" +
                "        <gmd:LocalisedCharacterString locale=\"#locale-ita\">Progetto OpenSource</gmd:LocalisedCharacterString>\n" +
                "        <gmd:LocalisedCharacterString locale=\"#locale-fra\">Projet OpenSource</gmd:LocalisedCharacterString>\n" +
                "      </gmd:textGroup>\n" +
                "    </gmd:PT_FreeText>\n" +
                "  </gmd:title>\n" +
                "</gmd:CI_Citation>\n";

        final Citation citation = (Citation) XML.unmarshal(legacy);
        assertEquals(getExpectedI18N(), citation.getTitle());
    }
}
