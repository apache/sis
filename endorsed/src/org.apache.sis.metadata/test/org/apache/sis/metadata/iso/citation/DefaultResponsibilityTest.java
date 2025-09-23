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
package org.apache.sis.metadata.iso.citation;

import java.util.Set;
import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.citation.Role;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.apache.sis.xml.test.TestCase;
import static org.apache.sis.metadata.Assertions.assertXmlEquals;


/**
 * Tests {@link DefaultResponsibility} and its marshalling as a legacy {@link DefaultResponsibleParty}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 */
public final class DefaultResponsibilityTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultResponsibilityTest() {
    }

    /**
     * Tests marshalling with replacement of {@link DefaultResponsibility} by {@link DefaultResponsibleParty}.
     *
     * @throws JAXBException if an error occurred during the marshalling.
     */
    @Test
    public void testLegacyMarshalling() throws JAXBException {
        final DefaultIndividual  party = new DefaultIndividual("An author", null, null);
        final DefaultResponsibleParty r = new DefaultResponsibleParty(Role.AUTHOR);
        final DefaultCitation citation = new DefaultCitation();
        r.setParties(Set.of(party));
        citation.setCitedResponsibleParties(Set.of(r));
        final String xml = marshal(citation, VERSION_2007);
        assertXmlEquals("<gmd:CI_Citation xmlns:gco=\"" + LegacyNamespaces.GCO + '"' +
                                        " xmlns:gmd=\"" + LegacyNamespaces.GMD + "\">\n" +
                "  <gmd:citedResponsibleParty>\n" +
                "    <gmd:CI_ResponsibleParty>\n" +
                "      <gmd:individualName>\n" +
                "        <gco:CharacterString>An author</gco:CharacterString>\n" +
                "      </gmd:individualName>\n" +
                "      <gmd:role>\n" +
                "        <gmd:CI_RoleCode codeList=\"http://www.isotc211.org/2005/resources/Codelist/gmxCodelists.xml#CI_RoleCode\" codeListValue=\"author\" codeSpace=\"eng\">Author</gmd:CI_RoleCode>\n" +
                "      </gmd:role>\n" +
                "    </gmd:CI_ResponsibleParty>\n" +
                "  </gmd:citedResponsibleParty>\n" +
                "</gmd:CI_Citation>\n", xml, "xmlns:*");
    }
}
