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
package org.apache.sis.xml.bind.lan;

import java.util.Map;
import java.util.Locale;
import java.io.InputStream;
import java.nio.charset.Charset;
import jakarta.xml.bind.JAXBException;
import org.apache.sis.metadata.iso.DefaultMetadata;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.metadata.xml.TestUsingFile;


/**
 * Tests the XML marshalling of {@link PT_Locale}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 */
public final class PT_LocaleTest extends TestUsingFile {
    /**
     * Creates a new test case.
     */
    public PT_LocaleTest() {
    }

    /**
     * Opens the stream to the XML file containing localized strings.
     *
     * @param  format  whether to use the 2007 or 2016 version of ISO 19115.
     * @return stream opened on the XML document to use for testing purpose.
     */
    private static InputStream openTestFile(final Format format) {
        return format.openTestFile("Locales.xml");
    }

    /**
     * The locales to use for the tests. For better test coverage we need at least:
     *
     * <ul>
     *   <li>One locale which is a language without specifying the country.</li>
     *   <li>At least two different countries for the same language.</li>
     * </ul>
     */
    private final Locale[] locales = {
        Locale.ENGLISH, Locale.JAPANESE, Locale.CANADA, Locale.FRANCE, Locale.CANADA_FRENCH
    };

    /**
     * Tests marshalling of a few locales using the specified version of metadata schema.
     *
     * @param format        whether to use the 2007 or 2016 version of ISO 19115.
     * @param ignoredNodes  the fully-qualified names of the nodes to ignore.
     */
    private void marshalAndCompare(final Format format, final String... ignoredNodes)
            throws JAXBException
    {
        final var metadata = new DefaultMetadata();
        final Map<Locale,Charset> lc = metadata.getLocalesAndCharsets();
        for (final Locale locale : locales) {
            lc.put(locale, null);
        }
        final String[] ignoredAttributes = {"xmlns:*", "xsi:*"};
        assertMarshalEqualsFile(openTestFile(format), metadata, format.schemaVersion, 0, ignoredNodes, ignoredAttributes);
    }

    /**
     * Tests marshalling of a few locales.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testMarshalling() throws JAXBException {
        marshalAndCompare(Format.XML2016, "mdb:contact", "mdb:dateInfo", "mdb:identificationInfo");
    }

    /**
     * Tests marshalling to legacy ISO 19139:2007 schema.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testMarshallingLegacy() throws JAXBException {
        marshalAndCompare(Format.XML2007, "gmd:contact", "gmd:dateStamp", "gmd:identificationInfo");
    }

    /**
     * Tests unmarshalling of a few locales.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testUnmarshalling() throws JAXBException {
        final DefaultMetadata metadata = unmarshalFile(DefaultMetadata.class, openTestFile(Format.XML2016));
        assertArrayEquals(locales, metadata.getLocalesAndCharsets().keySet().toArray());
    }

    /**
     * Tests unmarshalling from legacy ISO 19139:2007 schema.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testUnmarshallingLegacy() throws JAXBException {
        final DefaultMetadata metadata = unmarshalFile(DefaultMetadata.class, openTestFile(Format.XML2007));
        assertArrayEquals(locales, metadata.getLocalesAndCharsets().keySet().toArray());
    }
}
