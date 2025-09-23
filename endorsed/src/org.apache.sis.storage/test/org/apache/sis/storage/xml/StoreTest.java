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
package org.apache.sis.storage.xml;

import java.util.Locale;
import java.io.StringReader;
import java.net.URISyntaxException;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.*;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.TestUtilities.getSingleton;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import java.nio.charset.StandardCharsets;


/**
 * Tests {@link Store}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class StoreTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public StoreTest() {
    }

    /**
     * The metadata to unmarshal.
     */
    public static final String XML =
            "<?xml version=\"1.0\" standalone=\"yes\"?>\n" +
            "<gmd:MD_Metadata\n" +
            "  xmlns:gmd = \"" + LegacyNamespaces.GMD + "\"\n"  +
            "  xmlns:gco = \"" + LegacyNamespaces.GCO + "\"\n"  +
            "  xmlns:xsi = \"" + Namespaces.XSI + "\"\n>" +
            "  <gmd:language>\n" +
            "    <gmd:LanguageCode codeListValue=\"eng\">English</gmd:LanguageCode>\n" +
            "  </gmd:language>\n" +
            "  <gmd:characterSet>\n" +
            "    <gmd:MD_CharacterSetCode codeListValue=\"utf8\">UTF-8</gmd:MD_CharacterSetCode>\n" +
            "  </gmd:characterSet>\n" +
            "  <gmd:contact>\n" +
            "    <gmd:CI_ResponsibleParty>\n" +
            "      <gmd:organisationName>\n" +
            "        <gco:CharacterString>Apache SIS</gco:CharacterString>\n" +
            "      </gmd:organisationName>\n" +
            "      <gmd:contactInfo>\n" +
            "        <gmd:CI_Contact>\n" +
            "          <gmd:onlineResource>\n" +
            "            <gmd:CI_OnlineResource>\n" +
            "              <gmd:linkage>\n" +
            "                <gmd:URL>https://sis.apache.org</gmd:URL>\n" +
            "              </gmd:linkage>\n" +
            "              <gmd:function>\n" +
            "                <gmd:CI_OnLineFunctionCode codeListValue=\"information\" codeSpace=\"fra\">Information</gmd:CI_OnLineFunctionCode>\n" +
            "              </gmd:function>\n" +
            "            </gmd:CI_OnlineResource>\n" +
            "          </gmd:onlineResource>\n" +
            "        </gmd:CI_Contact>\n" +
            "      </gmd:contactInfo>\n" +
            "      <gmd:role>\n" +
            "        <gmd:CI_RoleCode codeListValue=\"principalInvestigator\">Principal investigator</gmd:CI_RoleCode>\n" +
            "      </gmd:role>\n" +
            "    </gmd:CI_ResponsibleParty>\n" +
            "  </gmd:contact>\n" +
            "</gmd:MD_Metadata>\n";

    /**
     * Tests {@link Store#getMetadata()}.
     *
     * @throws URISyntaxException if an error occurred while normalizing the URI.
     * @throws DataStoreException if an error occurred while reading the XML.
     */
    @Test
    public void testMetadata() throws URISyntaxException, DataStoreException {
        final Metadata metadata;
        try (Store store = new Store(null, new StorageConnector(new StringReader(XML)))) {
            metadata = store.getMetadata();
            assertSame(metadata, store.getMetadata(), "Expected cached value.");
        }
        final Responsibility resp     = getSingleton(metadata.getContacts());
        final Party          party    = getSingleton(resp.getParties());
        final Contact        contact  = getSingleton(party.getContactInfo());
        final OnlineResource resource = getSingleton(contact.getOnlineResources());

        assertInstanceOf(Organisation.class, party, "party");
        assertEquals(Locale.ENGLISH,              getSingleton(metadata.getLocalesAndCharsets().keySet()));
        assertEquals(StandardCharsets.UTF_8,      getSingleton(metadata.getLocalesAndCharsets().values()));
        assertEquals(Role.PRINCIPAL_INVESTIGATOR, resp.getRole());
        assertEquals("Apache SIS",                String.valueOf(party.getName()));
        assertEquals("https://sis.apache.org",    String.valueOf(resource.getLinkage()));
        assertEquals(OnLineFunction.INFORMATION,  resource.getFunction());
    }
}
