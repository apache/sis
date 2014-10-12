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
package org.apache.sis.metadata.iso;

import java.util.Locale;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.LogRecord;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.util.logging.WarningListener;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests XML (un)marshalling of various metadata objects.
 * For every metadata objects tested by this class, the expected XML representation
 * is provided by {@code *.xml} files in the following directory:
 *
 * <ul>
 *   <li>{@code "core/sis-metadata/src/test/resources/org/apache/sis/metadata/iso"}</li>
 * </ul>
 *
 * Metadata tested by this class do not include Coordinate Reference System (CRS) information. A metadata
 * object with CRS information is tested by {@code org.apache.sis.test.integration.DefaultMetadataTest}
 * in the {@code sis-referencing} module.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.5
 * @module
 */
public final strictfp class DefaultMetadataTest extends XMLTestCase implements WarningListener<Object> {
    /**
     * The resource key for the message of the warning that occurred while unmarshalling a XML fragment,
     * or {@code null} if none.
     */
    private Object resourceKey;

    /**
     * The parameter of the warning that occurred while unmarshalling a XML fragment, or {@code null} if none.
     */
    private Object[] parameters;

    /**
     * For internal {@code DefaultMetadata} usage.
     *
     * @return {@code Object.class}.
     */
    @Override
    public Class<Object> getSourceClass() {
        return Object.class;
    }

    /**
     * Invoked when a warning occurred while unmarshalling a test XML fragment. This method ensures that no other
     * warning occurred before this method call (i.e. each test is allowed to cause at most one warning), then
     * remember the warning parameters for verification by the test method.
     *
     * @param source  Ignored.
     * @param warning The warning.
     */
    @Override
    public void warningOccured(final Object source, final LogRecord warning) {
        assertNull(resourceKey);
        assertNull(parameters);
        assertNotNull(resourceKey = warning.getMessage());
        assertNotNull(parameters  = warning.getParameters());
    }

    /**
     * Unmarshalls the given XML fragment.
     */
    private DefaultMetadata unmarshal(final String xml) throws JAXBException {
        final MarshallerPool pool = getMarshallerPool();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        unmarshaller.setProperty(XML.WARNING_LISTENER, this);
        final Object c = unmarshal(unmarshaller, xml);
        pool.recycle(unmarshaller);
        return (DefaultMetadata) c;
    }

    /**
     * Tests unmarshalling of a metadata having a collection that contains no element.
     * This was used to cause a {@code NullPointerException} prior SIS-139 fix.
     *
     * @throws JAXBException If an error occurred during the during unmarshalling processes.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-139">SIS-139</a>
     */
    @Test
    public void testEmptyCollection() throws JAXBException {
        final DefaultMetadata metadata = unmarshal(
                "<gmd:MD_Metadata xmlns:gmd=\"" + Namespaces.GMD + "\">\n" +
                "  <gmd:contact/>\n" +
                "</gmd:MD_Metadata>");
        /*
         * Verify metadata property.
         */
        assertTrue(metadata.getContacts().isEmpty());
        /*
         * Verify warning message emitted during unmarshalling.
         */
        assertEquals("warning", "NullCollectionElement_1", resourceKey);
        assertArrayEquals("warning", new String[] {"CheckedArrayList<Responsibility>"}, parameters);
    }

    /**
     * Tests {@link DefaultMetadata#getFileIdentifier()} and {@link DefaultMetadata#setFileIdentifier(String)}
     * legacy methods. Those methods should delegate to newer methods.
     *
     * @since 0.5
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testFileIdentifier() {
        final DefaultMetadata metadata = new DefaultMetadata();
        assertNull("getFileIdentifier", metadata.getFileIdentifier());
        metadata.setFileIdentifier("Apache SIS/Metadata test");
        assertEquals("getMetadataIdentifier", "Apache SIS/Metadata test", metadata.getMetadataIdentifier().getCode());
        assertEquals("getFileIdentifier",     "Apache SIS/Metadata test", metadata.getFileIdentifier());
    }

    /**
     * Tests {@link DefaultMetadata#getLanguage()}, {@link DefaultMetadata#setLanguage(Locale)},
     * {@link DefaultMetadata#getLocales()} and {@link DefaultMetadata#setLocales(Collection)}
     * legacy methods. Those methods should delegate to newer methods.
     *
     * @since 0.5
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testLocales() {
        final DefaultMetadata metadata = new DefaultMetadata();
        assertNull("getLanguage", metadata.getLanguage());
        /*
         * Set the default language, which shall be the first entry in the collection.
         * The "other locales" property shall be unmodified by the "language" one.
         */
        metadata.setLanguage(Locale.JAPANESE);
        assertLanguagesEquals(metadata, Locale.JAPANESE);
        /*
         * Add other languages. They should appear as additional entries after the first one.
         * The "language" property shall be unmodified by changes in the "other locales" one.
         */
        metadata.setLocales(Arrays.asList(Locale.FRENCH, Locale.ENGLISH));
        assertLanguagesEquals(metadata, Locale.JAPANESE, Locale.FRENCH, Locale.ENGLISH);
        /*
         * Ensure that the "locales" list is modifiable, since JAXB writes directly in it.
         */
        metadata.getLocales().clear();
        assertLanguagesEquals(metadata, Locale.JAPANESE);
        final Collection<Locale> locales = metadata.getLocales();
        assertTrue(locales.add(Locale.KOREAN));
        assertTrue(locales.add(Locale.ENGLISH));
        assertLanguagesEquals(metadata, Locale.JAPANESE, Locale.KOREAN, Locale.ENGLISH);
        /*
         * Changing again the default language shall not change the other locales.
         */
        metadata.setLanguage(Locale.GERMAN);
        assertLanguagesEquals(metadata, Locale.GERMAN, Locale.KOREAN, Locale.ENGLISH);
    }

    /**
     * Compares the {@link DefaultMetadata#getLanguages()}, {@link DefaultMetadata#getLanguage()} and
     * {@link DefaultMetadata#getLocales()} values against the expected array.
     */
    @SuppressWarnings("deprecation")
    private static void assertLanguagesEquals(final DefaultMetadata metadata, final Locale... expected) {
        assertArrayEquals("getLanguages", expected,    metadata.getLanguages().toArray());
        assertEquals     ("getLanguage",  expected[0], metadata.getLanguage());
        assertArrayEquals("getLocales",   Arrays.copyOfRange(expected, 1, expected.length), metadata.getLocales().toArray());
    }
}
