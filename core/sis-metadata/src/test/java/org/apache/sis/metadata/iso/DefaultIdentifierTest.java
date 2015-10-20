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
package org.apache.sis.metadata.iso;

import javax.xml.bind.JAXBException;
import org.opengis.metadata.Identifier;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.util.CharSequences;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.test.DependsOn;
import org.junit.*;

import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests {@link DefaultIdentifier}.
 *
 * @author  Martin Desruisseaux
 * @since   0.4
 * @version 0.4
 * @module
 */
@DependsOn(org.apache.sis.metadata.iso.citation.DefaultCitationTest.class)
public final strictfp class DefaultIdentifierTest extends XMLTestCase {
    /**
     * The expected XML representation for this test.
     */
    private static final String XML =
            "<gmd:MD_Identifier xmlns:gmd=\"" + Namespaces.GMD + "\" " +
                               "xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
            "  <gmd:authority>\n" +
            "    <gmd:CI_Citation>\n" +
            "      <gmd:title>\n" +
            "        <gco:CharacterString>EPSG</gco:CharacterString>\n" +
            "      </gmd:title>\n" +
            "    </gmd:CI_Citation>\n" +
            "  </gmd:authority>\n" +
            "  <gmd:code>\n" +
            "    <gco:CharacterString>4326</gco:CharacterString>\n" +
            "  </gmd:code>\n" +
            "</gmd:MD_Identifier>";

    /**
     * Asserts that XML marshalling of the given object produce the {@link #XML} string.
     */
    void testMarshal(final String type, final Identifier identifier) throws JAXBException {
        assertXmlEquals(CharSequences.replace(XML, "MD_Identifier", type).toString(), marshal(identifier), "xmlns:*");
    }

    /**
     * Test XML marshalling.
     *
     * @throws JAXBException Should never happen.
     */
    @Test
    public void testMarshal() throws JAXBException {
        final DefaultIdentifier identifier = new DefaultIdentifier();
        identifier.setAuthority(new DefaultCitation("EPSG"));
        identifier.setCode("4326");
        testMarshal("MD_Identifier", identifier);
    }

    /**
     * Test XML unmarshalling.
     *
     * @throws JAXBException Should never happen.
     */
    @Test
    public void testUnmarshall() throws JAXBException {
        final DefaultIdentifier identifier = unmarshal(DefaultIdentifier.class, XML);
        assertNull       ("identifier",        identifier.getVersion());
        assertTitleEquals("authority", "EPSG", identifier.getAuthority());
        assertEquals     ("code",      "4326", identifier.getCode());
    }
}
