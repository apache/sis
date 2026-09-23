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

import java.util.HashMap;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.net.URISyntaxException;
import javax.xml.transform.Source;
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
public final class ReferenceResolverTest extends TestUsingFile implements Filter {
    /**
     * Number of times that an "access denied" warning is expected.
     */
    private int expectAccessDenied;

    /**
     * Creates a new test case.
     */
    public ReferenceResolverTest() {
    }

    /**
     * Invoked when a warning occurred during the XML unmarshalling.
     *
     * @param  record  the warning.
     * @return always {@code false} for keeping the output console quieter.
     */
    @Override
    public boolean isLoggable(final LogRecord record) {
        String file = assertInstanceOf(AccessDeniedException.class, record.getThrown()).getFile();
        assertTrue(file.endsWith("Citation.xml"), file);
        assertNotEquals(0, expectAccessDenied, "Access should not be denied.");
        expectAccessDenied--;
        return false;
    }

    /**
     * Parses the test <abbr>XML</abbr> document.
     *
     * @param  directory     the directory where to look for the {@code "UsingExternalXLink.xml"} file.
     * @param  readExternal  whether to allow the reading of external documents.
     */
    private DataIdentification data(final Format directory, final boolean readExternal)
            throws URISyntaxException, JAXBException
    {
        final Source source = directory.getSource("UsingExternalXLink.xml");
        final var properties = new HashMap<String, Object>(4);
        assertNull(properties.put(XML.WARNING_FILTER, this));
        if (readExternal) {
            assertNull(properties.put(XML.RESOLVER, ReferenceResolver.FOLLOW_EXTERNAL_XLINK));
        }
        final var data = assertInstanceOf(DataIdentification.class, XML.unmarshal(source, properties));
        assertTrue(data.getAbstract().toString().startsWith("Test the use of XLink to an external document"));
        return data;
    }

    /**
     * Tests that the attempt to read a fragment in an external document is denied by default.
     *
     * @throws URISyntaxException if an error occurred while getting the URL to the test file.
     * @throws IOException if an error occurred while opening the test file.
     * @throws JAXBException if an error occurred while parsing the test file.
     */
    @Test
    public void testAccessDenied() throws URISyntaxException, IOException, JAXBException {
        expectAccessDenied = 2;
        final DataIdentification data = data(Format.EXTERN, false);
        final Citation citation = data.getCitation();
        assertNull(citation.getTitle());
        assertEquals(0, expectAccessDenied, "Expected a warning.");
    }

    /**
     * Tests loading a document with a {@code xlink:href} to an external document in a different directory.
     * This is not allowed by default (verified by {@link #testAccessDenied()},
     * but this test grants authorization.
     *
     * @throws URISyntaxException if an error occurred while getting the URL to the test file.
     * @throws IOException if an error occurred while opening the test file.
     * @throws JAXBException if an error occurred while parsing the test file.
     */
    @Test
    public void testAccessGranted() throws URISyntaxException, IOException, JAXBException {
        testUsingExternalXLink(Format.EXTERN, true);
    }

    /**
     * Tests loading a document with a {@code xlink:href} to an external document in the same directory.
     * The access is granted by default.
     *
     * @throws URISyntaxException if an error occurred while getting the URL to the test file.
     * @throws IOException if an error occurred while opening the test file.
     * @throws JAXBException if an error occurred while parsing the test file.
     */
    @Test
    public void testSameDirectory() throws URISyntaxException, IOException, JAXBException {
        testUsingExternalXLink(Format.XML2016, false);
    }

    /**
     * Tests loading a document with a {@code xlink:href} to an external document.
     *
     * @param  directory     the directory where to look for the {@code "UsingExternalXLink.xml"} file.
     * @param  readExternal  whether to allow the reading of external documents.
     * @throws URISyntaxException if an error occurred while getting the URL to the test file.
     * @throws IOException if an error occurred while opening the test file.
     * @throws JAXBException if an error occurred while parsing the test file.
     */
    private void testUsingExternalXLink(final Format directory, final boolean readExternal)
            throws URISyntaxException, IOException, JAXBException
    {
        final DataIdentification data = data(directory, readExternal);
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
