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
package org.apache.sis.xml.bind.fra;

import java.util.Set;
import java.util.Collection;
import java.io.InputStream;
import jakarta.xml.bind.JAXBException;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultResponsibleParty;
import org.apache.sis.util.ComparisonMode;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertSingleton;
import org.apache.sis.xml.test.TestCase;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;

// Specific to the main and geoapi-3.1 branches:
import org.opengis.metadata.citation.ResponsibleParty;
import org.apache.sis.xml.bind.metadata.replace.RS_Identifier;


/**
 * Tests (un)marshalling of French profile of reference system.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 */
public final class DirectReferenceSystemTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DirectReferenceSystemTest() {
    }

    /**
     * Opens the stream to the XML file in this package containing a reference system definition.
     *
     * @return stream opened on the XML document to use for testing purpose.
     */
    private static InputStream openTestFile() {
        return DirectReferenceSystemTest.class.getResourceAsStream("DirectReferenceSystem.xml");
    }

    /**
     * Creates the metadata object to be tested.
     *
     * @param  legacy  {@code true} for using the legacy {@code ResponsibleParty} instead of {@code Responsibility}.
     *                 This is sometimes needed for comparison purpose with unmarshalled metadata.
     */
    @SuppressWarnings("deprecation")
    private static DefaultMetadata createMetadata(final boolean legacy) {
        final DefaultMetadata metadata = new DefaultMetadata();
        final DefaultCitation citation = new DefaultCitation("EPSG Geodetic Parameter Dataset");
        Collection<ResponsibleParty> r = HardCodedCitations.EPSG.getCitedResponsibleParties();
        if (legacy) {
            r = Set.of(new DefaultResponsibleParty(assertSingleton(r)));
        }
        citation.setCitedResponsibleParties(r);
        final DirectReferenceSystem refSys = new DirectReferenceSystem(new RS_Identifier(citation, "4326"));
        metadata.setReferenceSystemInfo(Set.of(refSys));
        return metadata;
    }

    /**
     * Ensures that the marshalling process of a {@link DefaultMetadata} produces
     * an XML document which complies with the one expected.
     *
     * @throws JAXBException if the marshalling process fails.
     */
    @Test
    public void marshallingTest() throws JAXBException {
        assertMarshalEqualsFile(openTestFile(), createMetadata(false), VERSION_2007, "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Ensures that the unmarshalling process of a {@link DefaultMetadata} stored in an XML
     * document produces an object containing all the information.
     *
     * @throws JAXBException if the unmarshalling process fails.
     */
    @Test
    public void unmarshallingTest() throws JAXBException {
        final DefaultMetadata expected = createMetadata(true);
        final DefaultMetadata result = unmarshalFile(DefaultMetadata.class, openTestFile());
        assertTrue(expected.equals(result, ComparisonMode.DEBUG));
    }
}
