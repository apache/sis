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

import java.util.Arrays;
import javax.xml.bind.JAXBException;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests (un)marshalling of French profile of reference system.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @version 0.4 (derived from geotk-3.00)
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
     */
    private static DefaultMetadata createMetadata() {
        final DefaultMetadata metadata = new DefaultMetadata();
        final DefaultCitation citation = new DefaultCitation("European Petroleum Survey Group");
        citation.setCitedResponsibleParties(HardCodedCitations.EPSG.getCitedResponsibleParties());
        final DirectReferenceSystem refSys = new DirectReferenceSystem(new ImmutableIdentifier(citation, null, "4326"));
        metadata.setReferenceSystemInfo(Arrays.asList(refSys));
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
        assertMarshalEqualsFile(XML_FILE, createMetadata(), "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Ensures that the unmarshalling process of a {@link DefaultMetadata} stored in an XML
     * document produces an object containing all the information.
     *
     * @throws JAXBException if the unmarshalling process fails.
     */
    @Test
    public void unmarshallingTest() throws JAXBException {
        final DefaultMetadata expected = createMetadata();
        final DefaultMetadata result = unmarshalFile(DefaultMetadata.class, XML_FILE);
        /*
         * Compare in debug mode before to perform the real comparison,
         * for making easier to analyze the stack trace in case of failure.
         */
        assertTrue(expected.equals(result, ComparisonMode.DEBUG));
        assertEquals(expected, result);
    }
}
