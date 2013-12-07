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
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.identification.TopicCategory;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.util.CharSequences;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the XML marshaling of {@code CodeList}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3 (derived from geotk-3.17)
 * @version 0.4
 * @module
 *
 * @see <a href="http://jira.geotoolkit.org/browse/GEOTK-121">GEOTK-121</a>
 */
public final strictfp class CodeListMarshallingTest extends XMLTestCase {
    /**
     * Returns a XML string to use for testing purpose.
     *
     * @param baseURL The base URL of XML schemas.
     */
    private static String getResponsiblePartyXML(final String baseURL) {
        return "<gmd:CI_ResponsibleParty xmlns:gmd=\"" + Namespaces.GMD + "\">\n" +
               "  <gmd:role>\n" +
               "    <gmd:CI_RoleCode codeList=\"" + baseURL + "resources/Codelist/gmxCodelists.xml#CI_RoleCode\"" +
                    " codeListValue=\"principalInvestigator\">" + "Principal investigator</gmd:CI_RoleCode>\n" +
               "  </gmd:role>\n" +
               "</gmd:CI_ResponsibleParty>";
    }

    /**
     * Returns a XML string to use for testing purpose.
     *
     * @param baseURL The base URL of XML schemas.
     */
    private static String getCitationXML(final String baseURL, final String language, final String value) {
        return "<gmd:CI_Date xmlns:gmd=\"" + Namespaces.GMD + "\">\n" +
               "  <gmd:dateType>\n" +
               "    <gmd:CI_DateTypeCode codeList=\"" + baseURL + "resources/Codelist/gmxCodelists.xml#CI_DateTypeCode\"" +
                    " codeListValue=\"creation\" codeSpace=\"" + language + "\">" + value + "</gmd:CI_DateTypeCode>\n" +
               "  </gmd:dateType>\n" +
               "</gmd:CI_Date>";
    }

    /**
     * Tests marshaling using the default URL.
     *
     * @throws JAXBException If an error occurred while marshaling the XML.
     */
    @Test
    public void testDefaultURL() throws JAXBException {
        final String expected = getResponsiblePartyXML("http://schemas.opengis.net/iso/19139/20070417/");
        final ResponsibleParty rp = (ResponsibleParty) XML.unmarshal(expected);
        assertEquals(Role.PRINCIPAL_INVESTIGATOR, rp.getRole());
        /*
         * Use the convenience method in order to avoid the effort of creating
         * our own MarshallerPool.
         */
        final String actual = XML.marshal(rp);
        assertXmlEquals(expected, actual, "xmlns:*");
    }

    /**
     * Tests marshaling using the ISO URL.
     *
     * @throws JAXBException If an error occurred while marshaling the XML.
     */
    @Test
    public void testISO_URL() throws JAXBException {
        final String expected = getResponsiblePartyXML("http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/");
        final ResponsibleParty rp = (ResponsibleParty) XML.unmarshal(expected);
        assertEquals(Role.PRINCIPAL_INVESTIGATOR, rp.getRole());

        final MarshallerPool pool = getMarshallerPool();
        final Marshaller marshaller = pool.acquireMarshaller();
        marshaller.setProperty(XML.SCHEMAS, Collections.singletonMap("gmd",
                "http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas")); // Intentionally omit trailing '/'.
        final String actual = marshal(marshaller, rp);
        pool.recycle(marshaller);
        assertXmlEquals(expected, actual, "xmlns:*");
    }

    /**
     * Tests a code list localization.
     *
     * @throws JAXBException If an error occurred while marshaling the XML.
     */
    @Test
    public void testLocalization() throws JAXBException {
        final MarshallerPool pool = getMarshallerPool();
        final Marshaller marshaller = pool.acquireMarshaller();
        /*
         * First, test using the French locale.
         */
        marshaller.setProperty(XML.LOCALE, Locale.FRENCH);
        String expected = getCitationXML("http://schemas.opengis.net/iso/19139/20070417/", "fra", "Création");
        CitationDate ci = (CitationDate) XML.unmarshal(expected);
        assertEquals(DateType.CREATION, ci.getDateType());
        String actual = marshal(marshaller, ci);
        assertXmlEquals(expected, actual, "xmlns:*");
        /*
         * Tests again using the Englisg locale.
         */
        marshaller.setProperty(XML.LOCALE, Locale.ENGLISH);
        expected = getCitationXML("http://schemas.opengis.net/iso/19139/20070417/", "eng", "Creation");
        ci = (CitationDate) XML.unmarshal(expected);
        assertEquals(DateType.CREATION, ci.getDateType());
        actual = marshal(marshaller, ci);
        assertXmlEquals(expected, actual, "xmlns:*");

        pool.recycle(marshaller);
    }

    /**
     * Tests marshaling of a code list which is not in the list of standard codes.
     *
     * @throws JAXBException If an error occurred while marshaling the XML.
     */
    @Test
    public void testExtraCodes() throws JAXBException {
        final DefaultDataIdentification id = new DefaultDataIdentification();
        id.setTopicCategories(Arrays.asList(
                TopicCategory.valueOf("oceans"), // New code
                TopicCategory.valueOf("OCEANS"), // Existing code with UML id="oceans"
                TopicCategory.valueOf("test"))); // New code

        final String xml = marshal(id);

        // "OCEANS" is marshalled as "oceans" because is contains a UML id, which is lower-case.
        assertEquals(2, CharSequences.count(xml, "<gmd:MD_TopicCategoryCode>oceans</gmd:MD_TopicCategoryCode>"));
        assertEquals(0, CharSequences.count(xml, "<gmd:MD_TopicCategoryCode>OCEANS</gmd:MD_TopicCategoryCode>"));
        assertEquals(1, CharSequences.count(xml, "<gmd:MD_TopicCategoryCode>test</gmd:MD_TopicCategoryCode>"));
    }
}
