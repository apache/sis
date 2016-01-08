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

import java.net.URI;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.LogRecord;
import javax.xml.bind.JAXBException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.sis.util.logging.WarningListener;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.XML;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static java.util.Collections.singletonMap;


/**
 * Tests {@link DefaultBrowseGraphic}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class DefaultBrowseGraphicTest extends TestCase {
    /**
     * Tests XML marshalling of {@code <gmx:MimeFileType>} inside {@code <gmd:MD_BrowseGraphic>}.
     *
     * @throws JAXBException if an error occurred while (un)marshalling the {@code BrowseGraphic}.
     */
    @Test
    public void testMimeFileType() throws JAXBException {
        final DefaultBrowseGraphic browse = new DefaultBrowseGraphic();
        browse.setFileType("image/tiff");
        final String xml = XML.marshal(browse);
        assertXmlEquals(
                "<gmd:MD_BrowseGraphic xmlns:gmd=\"" + Namespaces.GMD + '"' +
                                     " xmlns:gmx=\"" + Namespaces.GMX + "\">\n" +
                "  <gmd:fileType>\n" +
                "    <gmx:MimeFileType type=\"image/tiff\">image/tiff</gmx:MimeFileType>\n" +
                "  </gmd:fileType>\n" +
                "</gmd:MD_BrowseGraphic>", xml, "xmlns:*");
        /*
         * Unmarshal the element back to a Java object and compare to the original.
         */
        assertEquals(browse, XML.unmarshal(xml));
    }

    /**
     * Tests XML marshalling of {@code <gmx:FileName>} inside {@code <gmd:MD_BrowseGraphic>}.
     *
     * @throws JAXBException if an error occurred while (un)marshalling the {@code BrowseGraphic}.
     */
    @Test
    public void testFileName() throws JAXBException {
        final URI uri = URI.create("file:/catalog/image.png");
        final DefaultBrowseGraphic browse = new DefaultBrowseGraphic(uri);
        final String xml = XML.marshal(browse);
        assertXmlEquals(
                "<gmd:MD_BrowseGraphic xmlns:gmd=\"" + Namespaces.GMD + '"' +
                                     " xmlns:gmx=\"" + Namespaces.GMX + "\">\n" +
                "  <gmd:fileName>\n" +
                "    <gmx:FileName src=\"file:/catalog/image.png\">image.png</gmx:FileName>\n" +
                "  </gmd:fileName>\n" +
                "</gmd:MD_BrowseGraphic>", xml, "xmlns:*");
        /*
         * Unmarshal the element back to a Java object and compare to the original.
         */
        assertEquals(browse, XML.unmarshal(xml));
    }

    /**
     * Tests unmarshalling of {@code <gmx:FileName>} without {@code src} attribute.
     *
     * @throws JAXBException if an error occurred while (un)marshalling the {@code BrowseGraphic}.
     */
    @Test
    @DependsOnMethod("testFileName")
    public void testFileNameWithoutSrc() throws JAXBException {
        final DefaultBrowseGraphic browse = (DefaultBrowseGraphic) XML.unmarshal(
                "<gmd:MD_BrowseGraphic xmlns:gmd=\"" + Namespaces.GMD + '"' +
                                     " xmlns:gmx=\"" + Namespaces.GMX + "\">\n" +
                "  <gmd:fileName>\n" +
                "    <gmx:FileName>file:/catalog/image.png</gmx:FileName>\n" +
                "  </gmd:fileName>\n" +
                "</gmd:MD_BrowseGraphic>");

        assertEquals(URI.create("file:/catalog/image.png"), browse.getFileName());
    }

    /**
     * Tests XML marshalling of {@code <gmx:FileName>} and {@code <gmx:MimeFileType>} together.
     *
     * @throws JAXBException if an error occurred while (un)marshalling the {@code BrowseGraphic}.
     */
    @Test
    @DependsOnMethod({"testFileName", "testMimeFileType"})
    public void testFileNameAndType() throws JAXBException {
        final URI uri = URI.create("file:/catalog/image.png");
        final DefaultBrowseGraphic browse = new DefaultBrowseGraphic(uri);
        browse.setFileType("image/tiff");
        final String xml = XML.marshal(browse);
        assertXmlEquals(
                "<gmd:MD_BrowseGraphic xmlns:gmd=\"" + Namespaces.GMD + '"' +
                                     " xmlns:gmx=\"" + Namespaces.GMX + "\">\n" +
                "  <gmd:fileName>\n" +
                "    <gmx:FileName src=\"file:/catalog/image.png\">image.png</gmx:FileName>\n" +
                "  </gmd:fileName>\n" +
                "  <gmd:fileType>\n" +
                "    <gmx:MimeFileType type=\"image/tiff\">image/tiff</gmx:MimeFileType>\n" +
                "  </gmd:fileType>\n" +
                "</gmd:MD_BrowseGraphic>", xml, "xmlns:*");
        /*
         * Unmarshal the element back to a Java object and compare to the original.
         */
        assertEquals(browse, XML.unmarshal(xml));
    }

    /**
     * Tests XML marshalling of filename substituted by {@code <gco:CharacterString>}
     * inside {@code <gmd:MD_BrowseGraphic>}.
     *
     * @throws JAXBException if an error occurred while (un)marshalling the {@code BrowseGraphic}.
     */
    @Test
    @DependsOnMethod("testFileNameAndType")
    public void testStringSubstitution() throws JAXBException {
        final URI uri = URI.create("file:/catalog/image.png");
        final DefaultBrowseGraphic browse = new DefaultBrowseGraphic(uri);
        browse.setFileType("image/tiff");
        final StringWriter buffer = new StringWriter();
        XML.marshal(browse, new StreamResult(buffer),
                singletonMap(XML.STRING_SUBSTITUTES, new String[] {"filename", "mimetype"}));
        final String xml = buffer.toString();
        assertXmlEquals(
                "<gmd:MD_BrowseGraphic xmlns:gmd=\"" + Namespaces.GMD + '"' +
                                     " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <gmd:fileName>\n" +
                "    <gco:CharacterString>file:/catalog/image.png</gco:CharacterString>\n" +
                "  </gmd:fileName>\n" +
                "  <gmd:fileType>\n" +
                "    <gco:CharacterString>image/tiff</gco:CharacterString>\n" +
                "  </gmd:fileType>\n" +
                "</gmd:MD_BrowseGraphic>", xml, "xmlns:*");
        /*
         * Unmarshal the element back to a Java object and compare to the original.
         */
        assertEquals(browse, XML.unmarshal(xml));
    }

    /**
     * Tests the unmarshaller with the same URI in both {@code <gco:CharacterString>} and {@code <gmx:FileName>}.
     * Since the URI is the same, the unmarshaller should not produce any warning since there is no ambiguity.
     *
     * @throws JAXBException if an error occurred while (un)marshalling the {@code BrowseGraphic}.
     */
    @Test
    @DependsOnMethod("testStringSubstitution")
    public void testDuplicatedValues() throws JAXBException {
        final Warning listener = new Warning();
        final DefaultBrowseGraphic browse = listener.unmarshal(
                "<gmd:MD_BrowseGraphic xmlns:gmd=\"" + Namespaces.GMD + '"' +
                                     " xmlns:gmx=\"" + Namespaces.GMX + '"' +
                                     " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <gmd:fileName>\n" +
                "    <gmx:FileName src=\"file:/catalog/image.png\">image.png</gmx:FileName>\n" +
                "    <gco:CharacterString>file:/catalog/image.png</gco:CharacterString>\n" +
                "  </gmd:fileName>\n" +
                "</gmd:MD_BrowseGraphic>");

        assertEquals(URI.create("file:/catalog/image.png"), browse.getFileName());
        assertFalse("Expected no warning.", listener.receivedWarning);
    }

    /**
     * Ensures that the unmarshaller produces a warning when {@code <gco:CharacterString>} and
     * {@code <gmx:FileName>} both exist inside the same {@code <gmd:MD_BrowseGraphic>}.
     *
     * @throws JAXBException if an error occurred while (un)marshalling the {@code BrowseGraphic}.
     */
    @Test
    @DependsOnMethod("testStringSubstitution")
    public void testWarnings() throws JAXBException {
        testWarnings("<gmx:FileName src=\"file:/catalog/image.png\">image.png</gmx:FileName>",
                     "<gco:CharacterString>file:/catalog/image2.png</gco:CharacterString>");
        /*
         * Test again with the same element value, but in reverse order.
         * We do that for ensuring that FileName still has precedence.
         */
        testWarnings("<gco:CharacterString>file:/catalog/image2.png</gco:CharacterString>",
                     "<gmx:FileName src=\"file:/catalog/image.png\">image.png</gmx:FileName>");
    }

    /**
     * Implementation of {@link #testWarnings()} using the given {@code <gmd:fileName>} values.
     */
    private void testWarnings(final String first, final String second) throws JAXBException {
        final Warning listener = new Warning();
        final DefaultBrowseGraphic browse = listener.unmarshal(
                "<gmd:MD_BrowseGraphic xmlns:gmd=\"" + Namespaces.GMD + '"' +
                                     " xmlns:gmx=\"" + Namespaces.GMX + '"' +
                                     " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <gmd:fileName>\n" +
                "    " + first + "\n" +
                "    " + second + "\n" +
                "  </gmd:fileName>\n" +
                "</gmd:MD_BrowseGraphic>");

        assertEquals(URI.create("file:/catalog/image.png"), browse.getFileName());
        assertTrue("Expected a warning.", listener.receivedWarning);
    }

    /**
     * A warning listener to be registered by {@link #testWarnings()}.
     */
    private static final class Warning implements WarningListener<Object> {
        /**
         * {@code true} if a warning has been sent by the XML unmarshaller.
         */
        boolean receivedWarning;

        /**
         * Fixed to {@code Object.class} as required by {@link XML#WARNING_LISTENER} contract.
         */
        @Override
        public Class<Object> getSourceClass() {
            return Object.class;
        }

        /**
         * Invoked when a warning occurred. Ensures that no warning were previously sent,
         * then ensure that the warning content the expected message.
         */
        @Override
        public void warningOccured(final Object source, final LogRecord warning) {
            assertFalse("No other warning were expected.", receivedWarning);
            if (VERBOSE) {
                // In verbose mode, log the warning for allowing the developer to
                // check the message. In normal mode, the test will be silent.
                Logging.getLogger(warning.getLoggerName()).log(warning);
            }
            assertArrayEquals("FileName shall have precedence over CharacterString.",
                    new Object[] {"CharacterString", "FileName"}, warning.getParameters());
            receivedWarning = true;
        }

        /**
         * Unmarshals the given object while listening to warnings.
         */
        public DefaultBrowseGraphic unmarshal(final String xml) throws JAXBException {
            return (DefaultBrowseGraphic) XML.unmarshal(new StreamSource(new StringReader(xml)),
                    singletonMap(XML.WARNING_LISTENER, this));
        }
    }
}
