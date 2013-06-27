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
package org.apache.sis.internal.jaxb.gmd;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.opengis.metadata.identification.DataIdentification;
import org.apache.sis.test.mock.DataIdentificationMock;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.test.XMLTestCase;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests the XML marshaling of {@code Locale} when used for a language.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.18)
 * @version 0.3
 * @module
 */
public final strictfp class LanguageMarshallingTest extends XMLTestCase {
    /**
     * A poll of configured {@link Marshaller} and {@link Unmarshaller}, created when first needed.
     */
    private static MarshallerPool pool;

    /**
     * Creates the XML (un)marshaller pool to be shared by all test methods.
     * The (un)marshallers locale and timezone will be set to fixed values.
     *
     * @throws JAXBException If an error occurred while creating the pool.
     *
     * @see #disposeMarshallerPool()
     */
    @BeforeClass
    public static void createMarshallerPool() throws JAXBException {
        final Map<String,Object> properties = new HashMap<>(4);
        assertNull(properties.put(XML.LOCALE, Locale.UK));
        assertNull(properties.put(XML.TIMEZONE, "UTC"));
        pool = new MarshallerPool(JAXBContext.newInstance(DataIdentificationMock.class), properties);
    }

    /**
     * Invoked by JUnit after the execution of every tests in order to dispose
     * the {@link MarshallerPool} instance used internally by this class.
     */
    @AfterClass
    public static void disposeMarshallerPool() {
        pool = null;
    }

    /**
     * Returns the XML of a data identification element. This method returns the following string,
     * where the {@code <gco:CharacterString>} block is replaced by the more complex
     * {@code <gmd:LanguageCode>} if the {@code languageCode} argument is {@code true}.
     *
     * {@preformat xml
     *   <gmd:MD_DataIdentification>
     *     <gmd:language>
     *       <gco:CharacterString>fra</gco:CharacterString>
     *     </gmd:language>
     *   </gmd:MD_DataIdentification>
     * }
     *
     * @param languageCode {@code true} for using the {@code gmd:LanguageCode} construct,
     *        or false for using the {@code gco:CharacterString} construct.
     */
    private static String getDataIdentificationXML(final boolean languageCode) {
        final StringBuilder buffer = new StringBuilder(
                "<gmd:MD_DataIdentification" +
                " xmlns:gmd=\"" + Namespaces.GMD + '"' +
                " xmlns:gco=\"" + Namespaces.GCO + "\">\n" +
                "  <gmd:language>\n");
        if (languageCode) {
            buffer.append("    <gmd:LanguageCode"
                    + " codeList=\"http://schemas.opengis.net/iso/19139/20070417/resources/Codelist/ML_gmxCodelists.xml#LanguageCode\""
                    + " codeListValue=\"fra\">French</gmd:LanguageCode>\n");
        } else {
            buffer.append("    <gco:CharacterString>fra</gco:CharacterString>\n");
        }
        buffer.append(
                "  </gmd:language>\n" +
                "</gmd:MD_DataIdentification>");
        return buffer.toString();
    }

    /**
     * Tests the parsing of an XML using the {@code gmd:LanguageCode} construct.
     *
     * @throws JAXBException Should never happen.
     */
    @Test
    public void testLanguageCode() throws JAXBException {
        final Marshaller   marshaller   = pool.acquireMarshaller();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();

        final String xml = getDataIdentificationXML(true);
        final DataIdentification id = (DataIdentification) unmarshal(unmarshaller, xml);
        assertEquals(Locale.FRENCH, getSingleton(id.getLanguages()));
        /*
         * Reformat and test against the original XML.
         */
        assertXmlEquals(xml, marshal(marshaller, id), "xmlns:*");
        pool.recycle(unmarshaller);
        pool.recycle(marshaller);
    }

    /**
     * Tests the parsing of an XML using the {@code gco:CharacterString} construct.
     *
     * @throws JAXBException Should never happen.
     */
    @Test
    public void testCharacterString() throws JAXBException {
        final Marshaller   marshaller   = pool.acquireMarshaller();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();

        final String xml = getDataIdentificationXML(false);
        final DataIdentification id = (DataIdentification) unmarshal(unmarshaller, xml);
        assertEquals(Locale.FRENCH, getSingleton(id.getLanguages()));
        /*
         * Reformat and test against the expected XML.
         */
        assertXmlEquals(getDataIdentificationXML(true), marshal(marshaller, id), "xmlns:*");
        pool.recycle(unmarshaller);
        pool.recycle(marshaller);
    }

    /**
     * Tests the formatting of {@code <gco:CharacterString>}, which require explicit configuration.
     *
     * @throws JAXBException Should never happen.
     */
    @Test
    public void testCharacterStringFormat() throws JAXBException {
        final String inspire = getDataIdentificationXML(true);
        final String simpler = getDataIdentificationXML(false);
        final DataIdentificationMock id = new DataIdentificationMock(Locale.FRENCH);

        final Marshaller marshaller = pool.acquireMarshaller();
        assertNull(marshaller.getProperty(XML.STRING_SUBSTITUTES));
        assertXmlEquals(inspire, marshal(marshaller, id), "xmlns:*");

        marshaller.setProperty(XML.STRING_SUBSTITUTES, new String[] {"dummy","language","foo"});
        assertArrayEquals(new String[] {"language"}, (String[]) marshaller.getProperty(XML.STRING_SUBSTITUTES));
        assertXmlEquals(simpler, marshal(marshaller, id), "xmlns:*");
        pool.recycle(marshaller);
    }
}
