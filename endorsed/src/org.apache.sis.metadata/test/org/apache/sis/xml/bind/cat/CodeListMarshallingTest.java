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
package org.apache.sis.xml.bind.cat;

import java.util.Map;
import java.util.List;
import java.util.Locale;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.PresentationForm;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.xml.test.TestCase;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.citation.Responsibility;


/**
 * Tests the XML marshalling of {@code CodeList}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 */
public final class CodeListMarshallingTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public CodeListMarshallingTest() {
    }

    /**
     * Returns a XML string to use for testing purpose.
     * Note that responsible party exists only in legacy ISO 19115:2003 model.
     *
     * @param baseURL  the base URL of XML schemas.
     */
    private static String getResponsiblePartyXML(final String baseURL) {
        return "<gmd:CI_ResponsibleParty xmlns:gmd=\"" + LegacyNamespaces.GMD + "\">\n" +
               "  <gmd:role>\n" +
               "    <gmd:CI_RoleCode codeList=\"" + baseURL + CodeListUID.CODELISTS_PATH_LEGACY + "#CI_RoleCode\"" +
                    " codeListValue=\"principalInvestigator\">" + "Principal investigator</gmd:CI_RoleCode>\n" +
               "  </gmd:role>\n" +
               "</gmd:CI_ResponsibleParty>";
    }

    /**
     * Returns a XML string to use for testing purpose.
     *
     * @param  language  three-letter ISO code.
     * @param  dateType  date type code list in the language identified by {@code language}.
     * @param  legacy    {@code true} for ISO 19139:2007 format, {@code false} for ISO 19115-3 format.
     */
    private static String getCitationXML(final String language, final String dateType, final boolean legacy) {
        final Object[] args = new Object[] {
            "cit",                          // Prefix
            Namespaces.CIT,                 // Namespace
            CodeListUID.METADATA_ROOT,      // Base URL of code list path
            CodeListUID.CODELISTS_PATH,     // Relative code list path in base URL
            language, dateType
        };
        if (legacy) {
            args[0] = "gmd";                                // Prefix
            args[1] = LegacyNamespaces.GMD;                 // Namespace
            args[2] = CodeListUID.METADATA_ROOT_LEGACY;     // Base URL of code list path
            args[3] = CodeListUID.CODELISTS_PATH_LEGACY;    // Relative code list path in base URL
        }
        return String.format(
                "<%1$s:CI_Date xmlns:%1$s=\"%2$s\">\n" +
                "  <%1$s:dateType>\n" +
                "    <%1$s:CI_DateTypeCode codeList=\"%3$s%4$s#CI_DateTypeCode\"" +
                     " codeListValue=\"creation\" codeSpace=\"%5$s\">%6$s</%1$s:CI_DateTypeCode>\n" +
                "  </%1$s:dateType>\n" +
                "</%1$s:CI_Date>", args);
    }

    /**
     * Tests marshalling using the default URL.
     *
     * @throws JAXBException if an error occurred while marshalling the XML.
     */
    @Test
    public void testDefaultURL() throws JAXBException {
        final String expected = getResponsiblePartyXML(CodeListUID.METADATA_ROOT_LEGACY);
        final Responsibility rp = unmarshal(Responsibility.class, expected);
        assertEquals(Role.PRINCIPAL_INVESTIGATOR, rp.getRole());
        /*
         * Use the convenience method in order to avoid the effort of creating
         * our own MarshallerPool.
         */
        final String actual = marshal(rp, VERSION_2007);
        assertXmlEquals(expected, actual, "xmlns:*");
    }

    /**
     * Tests marshalling using legacy ISO URLs.
     *
     * @throws JAXBException if an error occurred while marshalling the XML.
     *
     * @see <a href="https://schemas.isotc211.org/19115/">ISO schemas for metadata</a>
     */
    @Test
    public void testLegacyISO_URL() throws JAXBException {
        final String expected = getResponsiblePartyXML("http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/");
        final Responsibility rp = unmarshal(Responsibility.class, expected);
        assertEquals(Role.PRINCIPAL_INVESTIGATOR, rp.getRole());

        final MarshallerPool pool = getMarshallerPool();
        final Marshaller marshaller = pool.acquireMarshaller();
        marshaller.setProperty(XML.METADATA_VERSION, VERSION_2007);
        marshaller.setProperty(XML.SCHEMAS, Map.of("gmd",
                "http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas"));     // Intentionally omit trailing '/'.
        final String actual = marshal(marshaller, rp);
        pool.recycle(marshaller);
        assertXmlEquals(expected, actual, "xmlns:*");
    }

    /**
     * Tests a code list localization.
     *
     * @throws JAXBException if an error occurred while marshalling the XML.
     */
    @Test
    public void testLocalization() throws JAXBException {
        testLocalization(false);
    }

    /**
     * Tests a code list localization in ISO 19139:2007.
     *
     * @throws JAXBException if an error occurred while marshalling the XML.
     */
    @Test
    public void testLocalizationLegacyXML() throws JAXBException {
        testLocalization(true);
    }

    /**
     * Implementation of {@link #testLocalization()} and {@link #testLocalizationLegacyXML()}.
     */
    private void testLocalization(final boolean legacy) throws JAXBException {
        final MarshallerPool pool = getMarshallerPool();
        final Marshaller marshaller = pool.acquireMarshaller();
        if (legacy) {
            marshaller.setProperty(XML.METADATA_VERSION, VERSION_2007);
        }
        /*
         * First, test using the French locale.
         */
        marshaller.setProperty(XML.LOCALE, Locale.FRENCH);
        String expected = getCitationXML("fra", "Création", legacy);
        CitationDate ci = unmarshal(CitationDate.class, expected);
        assertEquals(DateType.CREATION, ci.getDateType());
        String actual = marshal(marshaller, ci);
        assertXmlEquals(expected, actual, "xmlns:*");
        /*
         * Tests again using the English locale.
         */
        marshaller.setProperty(XML.LOCALE, Locale.ENGLISH);
        expected = getCitationXML("eng", "Creation", legacy);
        ci = unmarshal(CitationDate.class, expected);
        assertEquals(DateType.CREATION, ci.getDateType());
        actual = marshal(marshaller, ci);
        assertXmlEquals(expected, actual, "xmlns:*");

        pool.recycle(marshaller);
    }

    /**
     * Tests marshalling of a code list which is not in the list of standard codes.
     *
     * @throws JAXBException if an error occurred while marshalling the XML.
     */
    @Test
    public void testExtraCodes() throws JAXBException {
        final DefaultCitation id = new DefaultCitation();
        id.setPresentationForms(List.of(
                PresentationForm.valueOf("IMAGE_DIGITAL"),      // Existing code with UML id="imageDigital"
                PresentationForm.valueOf("test")));             // New code

        final String xml = marshal(id);
        /*
         * "IMAGE_DIGITAL" is marshalled as "imageDigital" because is contains a UML id, which is lower-case.
         */
        assertXmlEquals(
                "<cit:CI_Citation xmlns:cit=\"" + Namespaces.CIT + "\">\n" +
                "  <cit:presentationForm>\n" +
                "    <cit:CI_PresentationFormCode codeListValue=\"imageDigital\">Image digital</cit:CI_PresentationFormCode>\n" +
                "  </cit:presentationForm>\n" +
                "  <cit:presentationForm>\n" +
                "    <cit:CI_PresentationFormCode codeListValue=\"test\">Test</cit:CI_PresentationFormCode>\n" +
                "  </cit:presentationForm>\n" +
                "</cit:CI_Citation>\n",
                xml, "xmlns:*", "codeList", "codeSpace");
    }
}
