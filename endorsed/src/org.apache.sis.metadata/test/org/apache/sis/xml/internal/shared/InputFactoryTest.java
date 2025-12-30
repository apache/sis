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
package org.apache.sis.xml.internal.shared;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Files;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.TestCase;


/**
 * Tests related to {@link InputFactory}.
 *
 * <h2>Security</h2>
 * This class checks the fix for the "XML External Entity Prevention" vulnerability.
 * Users of Apache <abbr>SIS</abbr> versions without this fix can avoid the vulnerability
 * by setting the {@code javax.xml.accessExternalDTD} property to a value such as {@code ""}
 * (for blocking all accesses) or {@code "https"} (for allowing <abbr>HTTPS</abbr> accesses)
 * at <abbr>JVM</abbr> launch time.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see <a href="https://openjdk.org/jeps/185">JEP 185: Restrict Fetching of External XML Resources</a>
 * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html">XML External Entity Prevention</a>
 */
@SuppressWarnings("exports")
public final class InputFactoryTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public InputFactoryTest() {
    }

    /**
     * Verifies the capability to access an external <abbr>XML</abbr> entity.
     * This is used as a reference for testing the effect of different options
     * in other test cases.
     *
     * @throws IOException if an error occurred while writing the test file.
     * @throws XMLStreamException if an error occurred while parsing the <abbr>XML</abbr>.
     */
    @Test
    @Disabled("JAXP09020006")   // NullPointerException: argument 'catalog' can not be NULL. TODO: test with Maven4.
    public void verifyExternalEntityAccess() throws IOException, XMLStreamException {
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        assumeTrue("all".equals(factory.getProperty(XMLConstants.ACCESS_EXTERNAL_DTD)));
        assumeTrue(Boolean.TRUE.equals(factory.getProperty(XMLInputFactory.SUPPORT_DTD)));
        assumeTrue(Boolean.TRUE.equals(factory.getProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES)));
        readExternalEntity(factory, false);
    }

    /**
     * Verifies that the {@code IS_SUPPORTING_EXTERNAL_ENTITIES} property blocks the access to the local file.
     * Instead, the value is {@code null}.
     *
     * @throws IOException if an error occurred while writing the test file.
     * @throws XMLStreamException if an error occurred while parsing the <abbr>XML</abbr>.
     */
    @Test
    public void testDisableExternalEntities() throws IOException, XMLStreamException {
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        readExternalEntity(factory, true);
    }

    /**
     * Verifies that the {@code SUPPORT_DTD} property blocks the access to the local file.
     * Instead, an {@link XMLStreamException} is thrown.
     *
     * @throws IOException if an error occurred while writing the test file.
     */
    @Test
    public void testDisableDTD() throws IOException {
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        var ex = assertThrows(XMLStreamException.class, () -> readExternalEntity(factory, false));
        assertMessageContains(ex, "xxe");
    }

    /**
     * Verifies that the {@code SUPPORT_DTD} property blocks the access to the local file.
     * Instead, an {@link XMLStreamException} is thrown.
     *
     * @throws IOException if an error occurred while writing the test file.
     */
    @Test
    @Disabled("JAXP09020006")   // NullPointerException: argument 'catalog' can not be NULL. TODO: test with Maven4.
    public void testDisableAccessExternalDTD() throws IOException {
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "http,https");
        var ex = assertThrows(XMLStreamException.class, () -> readExternalEntity(factory, false));
        assertMessageContains(ex, "sis-");          // Prefix of the temporary file.
    }

    /**
     * Tries to get the content of a temporary file on the local file by parsing a <abbr>XML</abbr> document.
     *
     * @param  factory  the factory to use for attempting an attack.
     * @param  blocked  whether we expect the access to be blocked.
     * @throws IOException if an error occurred while writing the test file.
     * @throws XMLStreamException if an error occurred while parsing the <abbr>XML</abbr>.
     */
    private void readExternalEntity(final XMLInputFactory factory, final boolean blocked) throws IOException, XMLStreamException {
        String content = null;
        final String text = "Some text which should be inaccessible.";
        final Path path = Files.createTempFile("sis-", ".tmp");
        try {
            Files.writeString(path, text);
            final String xml = String.format(
                    "<!DOCTYPE foo [%n" +
                    "   <!ELEMENT foo ANY >%n" +
                    "   <!ENTITY xxe SYSTEM \"%s\" >%n" +
                    "]>%n" +
                    "<foo>&xxe;</foo>%n", path.toUri());

            final XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(xml));
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamReader.CHARACTERS) {
                    String found = reader.getText();
                    if (found != null && !(found = found.trim()).isEmpty()) {
                        assertNull(content);    // We expect only one occurrence.
                        content = found;
                    }
                }
            }
            reader.close();
        } finally {
            Files.delete(path);
        }
        if (blocked) {
            assertNull(content);
        } else {
            assertEquals(text, content);
        }
    }
}
