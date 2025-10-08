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
package org.apache.sis.metadata.iso.identification;

import java.util.Map;
import java.net.URI;
import java.io.StringReader;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import javax.xml.transform.stream.StreamSource;
import static java.util.logging.Logger.getLogger;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.identification.BrowseGraphic;
import org.apache.sis.util.Version;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.XML;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.xml.test.TestCase;
import static org.apache.sis.metadata.Assertions.assertXmlEquals;


/**
 * Tests {@link DefaultBrowseGraphic}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 */
@SuppressWarnings("exports")
public final class DefaultBrowseGraphicTest extends TestCase {
    /**
     * {@code false} if testing ISO 19115-3 document, or {@code true} if testing ISO 19139:2007 document.
     */
    private boolean legacy;

    /**
     * Creates a new test case.
     */
    public DefaultBrowseGraphicTest() {
    }

    /**
     * Verifies that marshalling the given metadata produces the expected XML document,
     * then verifies that unmarshalling that document gives back the original metadata object.
     * If {@link #legacy} is {@code true}, then this method will use ISO 19139:2007 schema.
     */
    private void roundtrip(final BrowseGraphic browse, String expected) throws JAXBException {
        final String  actual;
        final Version version;
        if (legacy) {
            expected = toLegacyXML(expected);
            version  = VERSION_2007;
        } else {
            version  = VERSION_2014;
        }
        actual = marshal(browse, version);
        assertXmlEquals(expected, actual, "xmlns:*");
        assertEquals(browse, unmarshal(BrowseGraphic.class, actual));
    }

    /**
     * Tests XML marshalling of {@code <gcx:MimeFileType>} inside {@code <mcc:MD_BrowseGraphic>}.
     * This method uses the XML schema defined by ISO 19115-3.
     *
     * @throws JAXBException if an error occurred while (un)marshalling the {@code BrowseGraphic}.
     */
    @Test
    public void testMimeFileType() throws JAXBException {
        final DefaultBrowseGraphic browse = new DefaultBrowseGraphic();
        browse.setFileType("image/tiff");
        roundtrip(browse,
                "<mcc:MD_BrowseGraphic xmlns:mcc=\"" + Namespaces.MCC + '"' +
                                     " xmlns:gcx=\"" + Namespaces.GCX + "\">\n" +
                "  <mcc:fileType>\n" +
                "    <gcx:MimeFileType type=\"image/tiff\">image/tiff</gcx:MimeFileType>\n" +
                "  </mcc:fileType>\n" +
                "</mcc:MD_BrowseGraphic>");
    }

    /**
     * Tests XML marshalling of {@code <gmx:MimeFileType>} inside {@code <gmd:MD_BrowseGraphic>}.
     * This method uses the XML schema defined by ISO 19139:2007.
     *
     * @throws JAXBException if an error occurred while (un)marshalling the {@code BrowseGraphic}.
     */
    @Test
    public void testMimeFileType_Legacy() throws JAXBException {
        legacy = true;
        testMimeFileType();
    }

    /**
     * Tests XML marshalling of {@code <gcx:FileName>} inside {@code <mcc:MD_BrowseGraphic>}.
     * This method uses the XML schema defined by ISO 19115-3.
     *
     * @throws JAXBException if an error occurred while (un)marshalling the {@code BrowseGraphic}.
     */
    @Test
    public void testFileName() throws JAXBException {
        roundtrip(new DefaultBrowseGraphic(URI.create("file:/catalog/image.png")),
                "<mcc:MD_BrowseGraphic xmlns:mcc=\"" + Namespaces.MCC + '"' +
                                     " xmlns:gcx=\"" + Namespaces.GCX + "\">\n" +
                "  <mcc:fileName>\n" +
                "    <gcx:FileName src=\"file:/catalog/image.png\">image.png</gcx:FileName>\n" +
                "  </mcc:fileName>\n" +
                "</mcc:MD_BrowseGraphic>");
    }

    /**
     * Tests XML marshalling of {@code <gmx:FileName>} inside {@code <gmd:MD_BrowseGraphic>}.
     * This method uses the XML schema defined by ISO 19139:2007.
     *
     * @throws JAXBException if an error occurred while (un)marshalling the {@code BrowseGraphic}.
     */
    @Test
    public void testFileName_Legacy() throws JAXBException {
        legacy = true;
        testFileName();
    }

    /**
     * Tests unmarshalling of {@code <gcx:FileName>} without {@code src} attribute.
     *
     * @throws JAXBException if an error occurred while (un)marshalling the {@code BrowseGraphic}.
     */
    @Test
    public void testFileNameWithoutSrc() throws JAXBException {
        final DefaultBrowseGraphic browse = unmarshal(DefaultBrowseGraphic.class,
                "<mcc:MD_BrowseGraphic xmlns:mcc=\"" + Namespaces.MCC + '"' +
                                     " xmlns:gcx=\"" + Namespaces.GCX + "\">\n" +
                "  <mcc:fileName>\n" +
                "    <gcx:FileName>file:/catalog/image.png</gcx:FileName>\n" +
                "  </mcc:fileName>\n" +
                "</mcc:MD_BrowseGraphic>");

        assertEquals(URI.create("file:/catalog/image.png"), browse.getFileName());
    }

    /**
     * Tests XML marshalling of {@code <gcx:FileName>} and {@code <gcx:MimeFileType>} together.
     * This method uses the XML schema defined by ISO 19115-3.
     *
     * @throws JAXBException if an error occurred while (un)marshalling the {@code BrowseGraphic}.
     */
    @Test
    public void testFileNameAndType() throws JAXBException {
        final DefaultBrowseGraphic browse = new DefaultBrowseGraphic(URI.create("file:/catalog/image.png"));
        browse.setFileType("image/tiff");
        roundtrip(browse,
                "<mcc:MD_BrowseGraphic xmlns:mcc=\"" + Namespaces.MCC + '"' +
                                     " xmlns:gcx=\"" + Namespaces.GCX + "\">\n" +
                "  <mcc:fileName>\n" +
                "    <gcx:FileName src=\"file:/catalog/image.png\">image.png</gcx:FileName>\n" +
                "  </mcc:fileName>\n" +
                "  <mcc:fileType>\n" +
                "    <gcx:MimeFileType type=\"image/tiff\">image/tiff</gcx:MimeFileType>\n" +
                "  </mcc:fileType>\n" +
                "</mcc:MD_BrowseGraphic>");
    }

    /**
     * Tests XML marshalling of {@code <gmx:FileName>} and {@code <gmx:MimeFileType>} together.
     * This method uses the XML schema defined by ISO 19139:2007.
     *
     * @throws JAXBException if an error occurred while (un)marshalling the {@code BrowseGraphic}.
     */
    @Test
    public void testFileNameAndType_Legacy() throws JAXBException {
        legacy = true;
        testFileNameAndType();
    }

    /**
     * Tests XML marshalling of filename substituted by {@code <gco:CharacterString>}
     * inside {@code <mcc:MD_BrowseGraphic>}.
     *
     * @throws JAXBException if an error occurred while (un)marshalling the {@code BrowseGraphic}.
     */
    @Test
    public void testStringSubstitution() throws JAXBException {
        final DefaultBrowseGraphic browse = new DefaultBrowseGraphic(URI.create("file:/catalog/image.png"));
        browse.setFileType("image/tiff");

        final MarshallerPool pool = getMarshallerPool();
        final Marshaller marshaller = pool.acquireMarshaller();
        marshaller.setProperty(XML.STRING_SUBSTITUTES, new String[] {"filename", "mimetype"});
        final String xml = marshal(marshaller, browse);
        pool.recycle(marshaller);

        assertXmlEquals(
                "<mcc:MD_BrowseGraphic xmlns:mcc=\"" + Namespaces.MCC + '"' +
                                     " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <mcc:fileName>\n" +
                "    <gco:CharacterString>file:/catalog/image.png</gco:CharacterString>\n" +
                "  </mcc:fileName>\n" +
                "  <mcc:fileType>\n" +
                "    <gco:CharacterString>image/tiff</gco:CharacterString>\n" +
                "  </mcc:fileType>\n" +
                "</mcc:MD_BrowseGraphic>", xml, "xmlns:*");
        /*
         * Unmarshal the element back to a Java object and compare to the original.
         */
        assertEquals(browse, unmarshal(BrowseGraphic.class, xml));
    }

    /**
     * Tests the unmarshaller with the same URI in both {@code <gco:CharacterString>} and {@code <gcx:FileName>}.
     * Since the URI is the same, the unmarshaller should not produce any warning since there is no ambiguity.
     *
     * @throws JAXBException if an error occurred while (un)marshalling the {@code BrowseGraphic}.
     */
    @Test
    public void testDuplicatedValues() throws JAXBException {
        final Warning listener = new Warning();
        final DefaultBrowseGraphic browse = listener.unmarshal(
                "<mcc:MD_BrowseGraphic xmlns:mcc=\"" + Namespaces.MCC + '"' +
                                     " xmlns:gcx=\"" + Namespaces.GCX + '"' +
                                     " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <mcc:fileName>\n" +
                "    <gcx:FileName src=\"file:/catalog/image.png\">image.png</gcx:FileName>\n" +
                "    <gco:CharacterString>file:/catalog/image.png</gco:CharacterString>\n" +
                "  </mcc:fileName>\n" +
                "</mcc:MD_BrowseGraphic>");

        assertEquals(URI.create("file:/catalog/image.png"), browse.getFileName());
        assertFalse(listener.receivedWarning, "Expected no warning.");
    }

    /**
     * Ensures that the unmarshaller produces a warning when {@code <gco:CharacterString>} and
     * {@code <gcx:FileName>} both exist inside the same {@code <mcc:MD_BrowseGraphic>}.
     *
     * @throws JAXBException if an error occurred while (un)marshalling the {@code BrowseGraphic}.
     */
    @Test
    public void testWarnings() throws JAXBException {
        testWarnings("<gcx:FileName src=\"file:/catalog/image.png\">image.png</gcx:FileName>",
                     "<gco:CharacterString>file:/catalog/image2.png</gco:CharacterString>");
        /*
         * Test again with the same element value, but in reverse order.
         * We do that for ensuring that FileName still has precedence.
         */
        testWarnings("<gco:CharacterString>file:/catalog/image2.png</gco:CharacterString>",
                     "<gcx:FileName src=\"file:/catalog/image.png\">image.png</gcx:FileName>");
    }

    /**
     * Implementation of {@link #testWarnings()} using the given {@code <mcc:fileName>} values.
     */
    private void testWarnings(final String first, final String second) throws JAXBException {
        final Warning listener = new Warning();
        final DefaultBrowseGraphic browse = listener.unmarshal(
                "<mcc:MD_BrowseGraphic xmlns:mcc=\"" + Namespaces.MCC + '"' +
                                     " xmlns:gcx=\"" + Namespaces.GCX + '"' +
                                     " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <mcc:fileName>\n" +
                "    " + first + "\n" +
                "    " + second + "\n" +
                "  </mcc:fileName>\n" +
                "</mcc:MD_BrowseGraphic>");

        assertEquals(URI.create("file:/catalog/image.png"), browse.getFileName());
        assertTrue(listener.receivedWarning, "Expected a warning.");
    }

    /**
     * A warning listener to be registered by {@link #testWarnings()}.
     */
    private static final class Warning implements Filter {
        /**
         * {@code true} if a warning has been sent by the XML unmarshaller.
         */
        boolean receivedWarning;

        /**
         * Invoked when a warning occurred. Ensures that no warning were previously sent,
         * then ensure that the warning content the expected message.
         */
        @Override
        public boolean isLoggable(final LogRecord warning) {
            assertFalse(receivedWarning, "No other warning were expected.");
            if (VERBOSE) {
                /*
                 * In verbose mode, log the warning for allowing the developer to
                 * check the message. In normal mode, the test will be silent.
                 */
                getLogger(warning.getLoggerName()).log(warning);
            }
            assertArrayEquals(new Object[] {"CharacterString", "FileName"}, warning.getParameters(),
                              "FileName shall have precedence over CharacterString.");
            receivedWarning = true;
            return false;
        }

        /**
         * Unmarshals the given object while listening to warnings.
         */
        public DefaultBrowseGraphic unmarshal(final String xml) throws JAXBException {
            return (DefaultBrowseGraphic) XML.unmarshal(
                    new StreamSource(new StringReader(xml)),
                    Map.of(XML.WARNING_FILTER, this));
        }
    }
}
