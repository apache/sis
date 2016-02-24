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
package org.apache.sis.internal.storage.xml;

import java.util.Locale;
import java.io.StringReader;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.citation.*;
import org.opengis.metadata.identification.CharacterSet;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.opengis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests {@link Store}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
@DependsOn(org.apache.sis.storage.StorageConnectorTest.class)
public final strictfp class StoreTest extends TestCase {
    /**
     * The metadata to unmarshal.
     */
    public static final String XML =
            "<?xml version=\"1.0\" standalone=\"yes\"?>\n" +
            "<gmd:MD_Metadata\n" +
            "  xmlns:gmd = \"" + Namespaces.GMD + "\"\n"  +
            "  xmlns:gco = \"" + Namespaces.GCO + "\"\n"  +
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
            "                <gmd:URL>http://sis.apache.org</gmd:URL>\n" +
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
     * @throws DataStoreException if en error occurred while reading the XML.
     */
    @Test
    public void testMetadata() throws DataStoreException {
        final Metadata metadata;
        final Store store = new Store(new StorageConnector(new StringReader(XML)));
        try {
            metadata = store.getMetadata();
            assertSame("Expected cached value.", metadata, store.getMetadata());
        } finally {
            store.close();
        }
        final ResponsibleParty resp     = getSingleton(metadata.getContacts());
        final Contact          contact  = resp.getContactInfo();
        final OnlineResource   resource = contact.getOnlineResource();

        assertEquals(Locale.ENGLISH,              metadata.getLanguage());
        assertEquals(CharacterSet.UTF_8,          metadata.getCharacterSet());
        assertEquals(Role.PRINCIPAL_INVESTIGATOR, resp.getRole());
        assertEquals("Apache SIS",                String.valueOf(resp.getOrganisationName()));
        assertEquals("http://sis.apache.org",     String.valueOf(resource.getLinkage()));
        assertEquals(OnLineFunction.INFORMATION,  resource.getFunction());
    }
}
