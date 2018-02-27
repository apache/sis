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
package org.apache.sis.internal.jaxb.code;

import java.util.Arrays;
import java.util.Locale;
import java.util.Collections;
import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBException;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.citation.CitationDate;
import org.opengis.metadata.citation.Responsibility;
import org.opengis.metadata.citation.PresentationForm;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.internal.jaxb.LegacyNamespaces;
import org.apache.sis.internal.jaxb.Schemas;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests the XML marshaling of {@code CodeList}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @version 1.0
 *
 * @see <a href="http://jira.geotoolkit.org/browse/GEOTK-121">GEOTK-121</a>
 *
 * @since 0.3
 * @module
 */
public final strictfp class CodeListMarshallingTest extends XMLTestCase {
    /**
     * Returns a XML string to use for testing purpose.
     * Note that responsible party exists only in legacy ISO 19115:2003 model.
     *
     * @param baseURL  the base URL of XML schemas.
     */
    private static String getResponsiblePartyXML(final String baseURL) {
        return "<gmd:CI_ResponsibleParty xmlns:gmd=\"" + LegacyNamespaces.GMD + "\">\n" +
               "  <gmd:role>\n" +
               "    <gmd:CI_RoleCode codeList=\"" + baseURL + Schemas.CODELISTS_PATH_LEGACY + "#CI_RoleCode\"" +
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
            Schemas.METADATA_ROOT,          // Base URL of code list path
            Schemas.CODELISTS_PATH,         // Relative code list path in base URL
            language, dateType
        };
        if (legacy) {
            args[0] = "gmd";                              // Prefix
            args[1] = LegacyNamespaces.GMD;               // Namespace
            args[2] = Schemas.METADATA_ROOT_LEGACY;       // Base URL of code list path
            args[3] = Schemas.CODELISTS_PATH_LEGACY;      // Relative code list path in base URL
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
     * Tests marshaling using the default URL.
     *
     * @throws JAXBException if an error occurred while marshaling the XML.
     */
    @Test
    public void testDefaultURL() throws JAXBException {
        final String expected = getResponsiblePartyXML(Schemas.METADATA_ROOT_LEGACY);
        final Responsibility rp = (Responsibility) XML.unmarshal(expected);
        assertEquals(Role.PRINCIPAL_INVESTIGATOR, rp.getRole());
        /*
         * Use the convenience method in order to avoid the effort of creating
         * our own MarshallerPool.
         */
        final String actual = marshal(rp, VERSION_2007);
        assertXmlEquals(expected, actual, "xmlns:*");
    }

    /**
     * Tests marshaling using the ISO URL.
     *
     * @throws JAXBException if an error occurred while marshaling the XML.
     */
    @Test
    public void testISO_URL() throws JAXBException {
        final String expected = getResponsiblePartyXML("http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/");
        final Responsibility rp = (Responsibility) XML.unmarshal(expected);
        assertEquals(Role.PRINCIPAL_INVESTIGATOR, rp.getRole());

        final MarshallerPool pool = getMarshallerPool();
        final Marshaller marshaller = pool.acquireMarshaller();
        marshaller.setProperty(XML.METADATA_VERSION, VERSION_2007);
        marshaller.setProperty(XML.SCHEMAS, Collections.singletonMap("gmd",
                "http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas"));     // Intentionally omit trailing '/'.
        final String actual = marshal(marshaller, rp);
        pool.recycle(marshaller);
        assertXmlEquals(expected, actual, "xmlns:*");
    }

    /**
     * Tests a code list localization.
     *
     * @throws JAXBException if an error occurred while marshaling the XML.
     */
    @Test
    public void testLocalization() throws JAXBException {
        testLocalization(false);
    }

    /**
     * Tests a code list localization in ISO 19139:2007.
     *
     * @throws JAXBException if an error occurred while marshaling the XML.
     *
     * @since 1.0
     */
    @Test
    @DependsOnMethod("testLocalization")
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
        CitationDate ci = (CitationDate) XML.unmarshal(expected);
        assertEquals(DateType.CREATION, ci.getDateType());
        String actual = marshal(marshaller, ci);
        assertXmlEquals(expected, actual, "xmlns:*");
        /*
         * Tests again using the English locale.
         */
        marshaller.setProperty(XML.LOCALE, Locale.ENGLISH);
        expected = getCitationXML("eng", "Creation", legacy);
        ci = (CitationDate) XML.unmarshal(expected);
        assertEquals(DateType.CREATION, ci.getDateType());
        actual = marshal(marshaller, ci);
        assertXmlEquals(expected, actual, "xmlns:*");

        pool.recycle(marshaller);
    }

    /**
     * Tests marshaling of a code list which is not in the list of standard codes.
     *
     * @throws JAXBException if an error occurred while marshaling the XML.
     */
    @Test
    public void testExtraCodes() throws JAXBException {
        final DefaultCitation id = new DefaultCitation();
        id.setPresentationForms(Arrays.asList(
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
