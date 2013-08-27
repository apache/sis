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
import java.io.IOException;
import java.io.InputStream;
import javax.xml.bind.JAXBException;
import org.apache.sis.xml.XML;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests (un)marshalling of French profile of reference system.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 * @version 0.4 (derived from geotk-3.00)
 * @since   0.4
 * @module
 */
public final strictfp class DirectReferenceSystemTest extends TestCase {
    /**
     * An XML file representing a reference system tree.
     */
    private static final String RESOURCE_FILE = "DirectReferenceSystem.xml";

    /**
     * Creates the metadata object to be tested.
     */
    private static DefaultMetadata createMetadata() {
        final DefaultMetadata metadata = new DefaultMetadata();
        final DirectReferenceSystem refSys = new DirectReferenceSystem(new ImmutableIdentifier(
                new DefaultCitation(getSingleton(HardCodedCitations.EPSG.getCitedResponsibleParties())), null, "4326"));
        metadata.setReferenceSystemInfo(Arrays.asList(refSys));
        return metadata;
    }

    /**
     * Ensures that the marshalling process of a {@link DefaultMetadata} produces
     * an XML document which complies with the one expected.
     *
     * @throws IOException if an error occurred while reading the resource file.
     * @throws JAXBException if the marshalling process fails.
     */
    @Test
    public void marshallingTest() throws IOException, JAXBException {
        final String actual = XML.marshal(createMetadata());
        assertXmlEquals(DirectReferenceSystemTest.class.getResource(RESOURCE_FILE), actual, "xmlns:*", "xsi:schemaLocation");
    }

    /**
     * Ensures that the unmarshalling process of a {@link DefaultMetadata} stored in an XML
     * document produces an object containing all the information.
     *
     * @throws JAXBException if the unmarshalling process fails.
     * @throws IOException if an error occurred while reading the resource file.
     */
    @Test
    public void unmarshallingTest() throws JAXBException, IOException {
        final DefaultMetadata result;
        final InputStream in = DirectReferenceSystemTest.class.getResourceAsStream(RESOURCE_FILE);
        try {
            result = (DefaultMetadata) XML.unmarshal(in);
        } finally {
            in.close();
        }
        final DefaultMetadata expected = createMetadata();
        /*
         * Compare in debug mode before to perform the real comparison,
         * for making easier to analyze the stack trace in case of failure.
         */
        assertTrue(expected.equals(result, ComparisonMode.DEBUG));
        assertEquals(expected, result);
    }
}
