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
package org.apache.sis.xml;

import java.io.IOException;
import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.DataIdentification;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.metadata.xml.TestUsingFile;
import org.apache.sis.metadata.iso.citation.DefaultCitationTest;
import static org.apache.sis.test.Assertions.assertSingleton;


/**
 * Tests {@link ReferenceResolver}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class ReferenceResolverTest extends TestUsingFile {
    /**
     * Creates a new test case.
     */
    public ReferenceResolverTest() {
    }

    /**
     * Tests loading a document with a {@code xlink:href} to an external document.
     *
     * @throws IOException if an error occurred while opening the test file.
     * @throws JAXBException if an error occurred while parsing the test file.
     */
    @Test
    public void testUsingExternalXLink() throws IOException, JAXBException {
        final var data = (DataIdentification) XML.unmarshal(Format.XML2016.getURL("UsingExternalXLink.xml"));
        assertEquals("Test the use of XLink to an external document.", data.getAbstract().toString());
        final Citation citation = data.getCitation();
        DefaultCitationTest.verifyUnmarshalledCitation(citation);
        /*
         * The fragment should reference the exact same object as the one in the citation.
         */
        final var parent  = assertSingleton(citation.getCitedResponsibleParties().iterator().next().getParties());
        final var reusing = assertSingleton(assertSingleton(data.getPointOfContacts()).getParties());
        assertEquals("Little John", reusing.getName().toString());
        assertSame(assertSingleton(parent .getContactInfo()),
                   assertSingleton(reusing.getContactInfo()));

    }
}
