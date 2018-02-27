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

import org.apache.sis.test.mock.MetadataMock;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.opengis.metadata.Metadata;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.internal.jaxb.Schemas;
import org.apache.sis.internal.jaxb.LegacyNamespaces;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.test.DependsOnMethod;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;
import static org.apache.sis.internal.util.StandardDateFormat.UTC;


/**
 * Tests the XML marshaling of {@code Locale} when used for a language.
 * The locale is marshalled as a character string. This format was used by ISO 19139:2007
 * but is not used anymore in ISO 19115-3 (the newer version use {@code PT_Locale} instead).
 *
 * <p>This class also test indirectly the {@link org.apache.sis.xml} capability to map the legacy
 * {@code "http://www.isotc211.org/2005/gmd"} namespace to {@code "http://standards.iso.org/…"}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
public final strictfp class LanguageCodeTest extends XMLTestCase {
    /**
     * XML fragment using the {@code <gco:CharacterString>} construct.
     */
    private static final String CHARACTER_STRING = "<gco:CharacterString>jpn</gco:CharacterString>";

    /**
     * XML fragment using the {@code <gmd:LanguageCode>} construct without attributes.
     */
    private static final String LANGUAGE_CODE_WITHOUT_ATTRIBUTE = "<gmd:LanguageCode>jpn</gmd:LanguageCode>";

    /**
     * XML fragment using the {@code <gmd:LanguageCode>} construct with attributes.
     */
    private static final String LANGUAGE_CODE = "<gmd:LanguageCode" +
            " codeList=\"" + Schemas.METADATA_ROOT_LEGACY + Schemas.CODELISTS_PATH_LEGACY + "#LanguageCode\"" +
            " codeListValue=\"jpn\">Japanese</gmd:LanguageCode>";

    /**
     * A poll of configured {@link Marshaller} and {@link Unmarshaller}, created when first needed.
     */
    private static MarshallerPool pool;

    /**
     * Creates the XML (un)marshaller pool to be shared by all test methods.
     * The (un)marshallers locale and timezone will be set to fixed values.
     *
     * <p>This test uses its own pool instead of {@link XMLTestCase#getMarshallerPool()} because it
     * uses {@link MetadataMock} instead of {@link org.apache.sis.metadata.iso.DefaultMetadata}.</p>
     *
     * @throws JAXBException if an error occurred while creating the pool.
     *
     * @see #disposeMarshallerPool()
     */
    @BeforeClass
    public static void createMarshallerPool() throws JAXBException {
        final Map<String,Object> properties = new HashMap<>(4);
        assertNull(properties.put(XML.LOCALE, Locale.UK));
        assertNull(properties.put(XML.TIMEZONE, UTC));
        pool = new MarshallerPool(JAXBContext.newInstance(MetadataMock.class), properties);
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
     * Returns the XML of a metadata element. This method returns a string like below,
     * where the {@code ${languageCode}} string is replaced by the given argument.
     *
     * {@preformat xml
     *   <gmd:MD_Metadata>
     *     <gmd:language>
     *       ${languageCode}
     *     </gmd:language>
     *   </gmd:MD_Metadata>
     * }
     *
     * @param  languageCode  the XML fragment to write inside the {@code <gmd:language>} element.
     */
    private static String getMetadataXML(final String languageCode) {
        return "<gmd:MD_Metadata" +
               " xmlns:gmd=\"" + LegacyNamespaces.GMD + '"' +
               " xmlns:gco=\"" + LegacyNamespaces.GCO + "\">\n" +
               "  <gmd:language>\n" +
               "    " + languageCode + '\n' +
               "  </gmd:language>\n" +
               "</gmd:MD_Metadata>";
    }

    /**
     * Tests marshalling of {@code <gmd:LanguageCode>}.
     * The result shall be as documented in {@link #testMarshalLanguageCode()}.
     *
     * @throws JAXBException if an error occurs while marshalling the language.
     *
     * @see #testMarshalCharacterString()
     */
    @Test
    public void testMarshalLanguageCode() throws JAXBException {
        final MetadataMock metadata = new MetadataMock(Locale.JAPANESE);
        final Marshaller marshaller = pool.acquireMarshaller();
        assertNull(marshaller.getProperty(XML.STRING_SUBSTITUTES));
        marshaller.setProperty(XML.METADATA_VERSION, VERSION_2007);
        assertXmlEquals(getMetadataXML(LANGUAGE_CODE), marshal(marshaller, metadata), "xmlns:*");
        pool.recycle(marshaller);
    }

    /**
     * Tests the unmarshalling using the {@code <gmd:LanguageCode>} construct. XML fragment:
     *
     * {@preformat xml
     *   <gmd:MD_Metadata>
     *     <gmd:language>
     *       <gmd:LanguageCode codeList="(snip)/gmxCodelists.xml#LanguageCode" codeListValue="jpn">Japanese</gmd:LanguageCode>
     *     </gmd:language>
     *   </gmd:MD_Metadata>
     * }
     *
     * @throws JAXBException if an error occurs while unmarshalling the language.
     *
     * @see #testMarshalLanguageCode()
     */
    @Test
    public void testUnmarshalLanguageCode() throws JAXBException {
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        final String xml = getMetadataXML(LANGUAGE_CODE);
        final Metadata metadata = (Metadata) unmarshal(unmarshaller, xml);
        assertEquals(Locale.JAPANESE, getSingleton(metadata.getLanguages()));
    }

    /**
     * Tests the unmarshalling using the {@code <gmd:LanguageCode>} construct without attributes.
     * The adapter is expected to parse the element value. XML fragment:
     *
     * {@preformat xml
     *   <gmd:MD_Metadata>
     *     <gmd:language>
     *       <gmd:LanguageCode>jpn</gmd:LanguageCode>
     *     </gmd:language>
     *   </gmd:MD_Metadata>
     * }
     *
     * @throws JAXBException if an error occurs while unmarshalling the language.
     */
    @Test
    @DependsOnMethod("testMarshalLanguageCode")
    public void testLanguageCodeWithoutAttributes() throws JAXBException {
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        final String xml = getMetadataXML(LANGUAGE_CODE_WITHOUT_ATTRIBUTE);
        final Metadata metadata = (Metadata) unmarshal(unmarshaller, xml);
        assertEquals(Locale.JAPANESE, getSingleton(metadata.getLanguages()));
        pool.recycle(unmarshaller);
    }

    /**
     * Tests marshalling of {@code <gco:CharacterString>}, which require explicit marshaller configuration.
     * The result shall be as documented in {@link #testUnmarshalCharacterString()}.
     *
     * @throws JAXBException if an error occurs while marshalling the language.
     *
     * @see #testMarshalLanguageCode()
     */
    @Test
    public void testMarshalCharacterString() throws JAXBException {
        final MetadataMock metadata = new MetadataMock(Locale.JAPANESE);
        final Marshaller marshaller = pool.acquireMarshaller();
        marshaller.setProperty(XML.METADATA_VERSION, VERSION_2007);
        marshaller.setProperty(XML.STRING_SUBSTITUTES, new String[] {"dummy","language","foo"});
        assertArrayEquals(new String[] {"language"}, (String[]) marshaller.getProperty(XML.STRING_SUBSTITUTES));
        assertXmlEquals(getMetadataXML(CHARACTER_STRING), marshal(marshaller, metadata), "xmlns:*");
        pool.recycle(marshaller);
    }

    /**
     * Tests the unmarshalling of an XML using the {@code gco:CharacterString} construct.
     * XML fragment:
     *
     * {@preformat xml
     *   <gmd:MD_Metadata>
     *     <gmd:language>
     *       <gco:CharacterString>jpn</gco:CharacterString>
     *     </gmd:language>
     *   </gmd:MD_Metadata>
     * }
     *
     * @throws JAXBException if an error occurs while unmarshalling the language.
     */
    @Test
    public void testUnmarshalCharacterString() throws JAXBException {
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        final String xml = getMetadataXML(CHARACTER_STRING);
        final Metadata metadata = (Metadata) unmarshal(unmarshaller, xml);
        assertEquals(Locale.JAPANESE, getSingleton(metadata.getLanguages()));
        pool.recycle(unmarshaller);
    }
}
