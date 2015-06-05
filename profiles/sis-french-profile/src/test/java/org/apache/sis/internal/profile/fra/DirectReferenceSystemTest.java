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
package org.apache.sis.internal.profile.fra;

import java.util.Collection;
import java.util.Collections;
import javax.xml.bind.JAXBException;
import org.opengis.metadata.citation.ResponsibleParty;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultResponsibleParty;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static java.util.Collections.singleton;
import static org.apache.sis.test.Assert.*;


/**
 * Tests (un)marshalling of French profile of reference system.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @version 0.4
 * @since   0.5
 * @module
 */
public final strictfp class DirectReferenceSystemTest extends XMLTestCase {
    /**
     * An XML file in this package containing a reference system definition.
     */
    private static final String XML_FILE = "DirectReferenceSystem.xml";

    /**
     * Creates the metadata object to be tested.
     *
     * @param legacy {@code true} for using the legacy {@code ResponsibleParty} instead of {@code Responsibility}.
     *        This is sometime needed for comparison purpose with unmarshalled metadata.
     */
    @SuppressWarnings("deprecation")
    private static DefaultMetadata createMetadata(final boolean legacy) {
        final DefaultMetadata metadata = new DefaultMetadata();
        final DefaultCitation citation = new DefaultCitation("EPSG Geodetic Parameter Dataset");
        Collection<ResponsibleParty> r = HardCodedCitations.EPSG.getCitedResponsibleParties();
        if (legacy) {
            r = Collections.<ResponsibleParty>singleton(new DefaultResponsibleParty(TestUtilities.getSingleton(r)));
        }
        citation.setCitedResponsibleParties(r);
        final DirectReferenceSystem refSys = new DirectReferenceSystem(new ImmutableIdentifier(citation, null, "4326"));
        metadata.setReferenceSystemInfo(singleton(refSys));
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
        assertMarshalEqualsFile(XML_FILE, createMetadata(false), "xmlns:*", "xsi:schemaLocation");
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
        final DefaultMetadata result = unmarshalFile(DefaultMetadata.class, XML_FILE);
        /*
         * Compare in debug mode before to perform the real comparison,
         * for making easier to analyze the stack trace in case of failure.
         */
        assertTrue(expected.equals(result, ComparisonMode.DEBUG));
        assertEquals(expected, result);
    }
}
