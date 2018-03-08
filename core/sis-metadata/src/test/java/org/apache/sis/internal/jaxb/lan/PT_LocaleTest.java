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
package org.apache.sis.internal.jaxb.lan;

import java.util.Arrays;
import java.util.Locale;
import javax.xml.bind.JAXBException;
import org.apache.sis.util.Version;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the XML marshaling of {@link PT_Locale}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 * @since   0.4
 * @module
 */
public final strictfp class PT_LocaleTest extends XMLTestCase {
    /**
     * The locales to use for the tests. For better test coverage we need at least:
     *
     * <ul>
     *   <li>One locale which is a language without specifying the country</li>
     *   <li>At least two different countries for the same language.</li>
     * </ul>
     */
    private static final Locale[] LOCALES = {
            Locale.ENGLISH, Locale.JAPANESE, Locale.CANADA, Locale.FRANCE, Locale.CANADA_FRENCH
    };

    /**
     * Tests marshalling of a few locales using the specified version of metadata schema.
     *
     * @param filename      name of the file containing expected result.
     * @param ignoredNodes  the fully-qualified names of the nodes to ignore.
     */
    private void marshalAndCompare(final String filename, final Version version, final String... ignoredNodes)
            throws JAXBException
    {
        final DefaultMetadata metadata = new DefaultMetadata();
        metadata.setLanguages(Arrays.asList(LOCALES));
        assertMarshalEqualsFile(filename, metadata, version, STRICT, ignoredNodes,
                new String[] {"xmlns:*", "xsi:*"});
    }

    /**
     * Tests marshalling of a few locales.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testMarshalling() throws JAXBException {
        marshalAndCompare("Locales.xml", VERSION_2014,
                          "mdb:contact", "mdb:dateInfo", "mdb:identificationInfo");
    }

    /**
     * Tests marshalling to legacy ISO 19139:2007 schema.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testMarshallingLegacy() throws JAXBException {
        marshalAndCompare("Locales (legacy).xml", VERSION_2007,
                          "gmd:contact", "gmd:dateStamp", "gmd:identificationInfo");
    }

    /**
     * Tests unmarshalling of a few locales.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testUnmarshalling() throws JAXBException {
        final DefaultMetadata metadata = unmarshalFile(DefaultMetadata.class, "Locales.xml");
        assertArrayEquals(LOCALES, metadata.getLanguages().toArray());
    }

    /**
     * Tests unmarshalling from legacy ISO 19139:2007 schema.
     *
     * @throws JAXBException if an error occurred during (un)marshalling.
     */
    @Test
    public void testUnmarshallingLegacy() throws JAXBException {
        final DefaultMetadata metadata = unmarshalFile(DefaultMetadata.class, "Locales (legacy).xml");
        assertArrayEquals(LOCALES, metadata.getLanguages().toArray());
    }
}
