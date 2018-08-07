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
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.test.xml.TestCase;
import org.apache.sis.test.DependsOn;
import org.junit.*;

import static org.apache.sis.test.MetadataAssert.*;


/**
 * Tests {@link DefaultIdentifier}.
 *
 * @author  Martin Desruisseaux
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 * @since   0.4
 * @module
 */
@DependsOn(org.apache.sis.metadata.iso.citation.DefaultCitationTest.class)
public final strictfp class DefaultIdentifierTest extends TestCase {
    /**
     * The expected XML representation for this test.
     */
    private static final String XML =
            "<mcc:MD_Identifier xmlns:mcc=\"" + Namespaces.MCC + "\" " +
                               "xmlns:cit=\"" + Namespaces.CIT + "\" " +
                               "xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
            "  <mcc:authority>\n" +
            "    <cit:CI_Citation>\n" +
            "      <cit:title>\n" +
            "        <gco:CharacterString>EPSG</gco:CharacterString>\n" +
            "      </cit:title>\n" +
            "    </cit:CI_Citation>\n" +
            "  </mcc:authority>\n" +
            "  <mcc:code>\n" +
            "    <gco:CharacterString>4326</gco:CharacterString>\n" +
            "  </mcc:code>\n" +
            "</mcc:MD_Identifier>";

    /**
     * Tests XML marshalling.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testMarshal() throws JAXBException {
        final DefaultIdentifier identifier = new DefaultIdentifier();
        identifier.setAuthority(new DefaultCitation("EPSG"));
        identifier.setCode("4326");
        assertXmlEquals(XML, marshal(identifier), "xmlns:*");
    }

    /**
     * Tests XML unmarshalling.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testUnmarshall() throws JAXBException {
        final DefaultIdentifier identifier = unmarshal(DefaultIdentifier.class, XML);
        assertNull       ("identifier",        identifier.getVersion());
        assertTitleEquals("authority", "EPSG", identifier.getAuthority());
        assertEquals     ("code",      "4326", identifier.getCode());
    }
}
