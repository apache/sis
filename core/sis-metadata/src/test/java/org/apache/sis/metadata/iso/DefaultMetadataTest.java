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

import java.util.Date;
import java.util.Locale;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collection;
import java.util.logging.LogRecord;
import java.net.URISyntaxException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.maintenance.ScopeCode;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.util.logging.WarningListener;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.apache.sis.test.MetadataAssert.*;
import static org.apache.sis.test.TestUtilities.date;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests {@link DefaultMetadata}, without Coordinate Reference System (CRS) information.
 *
 * <p><b>Note:</b> a metadata object with CRS information is tested by a different
 * {@code org.apache.sis.test.integration.DefaultMetadataTest} class in the {@code sis-referencing} module.</p>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 */
@DependsOn(org.apache.sis.internal.metadata.OtherLocalesTest.class)
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
        assertArrayEquals("warning", new String[] {"CheckedArrayList<ResponsibleParty>"}, parameters);
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
        assertNull("fileIdentifier", metadata.getFileIdentifier());
        metadata.setFileIdentifier("Apache SIS/Metadata test");
        assertEquals("metadataIdentifier", "Apache SIS/Metadata test", metadata.getMetadataIdentifier().getCode());
        assertEquals("fileIdentifier",     "Apache SIS/Metadata test", metadata.getFileIdentifier());
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
        assertNull("language", metadata.getLanguage());
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
        assertArrayEquals("languages", expected,    metadata.getLanguages().toArray());
        assertEquals     ("language",  expected[0], metadata.getLanguage());
        assertArrayEquals("locales",   Arrays.copyOfRange(expected, 1, expected.length), metadata.getLocales().toArray());
    }

    /**
     * Tests {@link DefaultMetadata#getParentIdentifier()} and {@link DefaultMetadata#setParentIdentifier(String)}
     * methods.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testParentIdentifier() {
        final DefaultMetadata metadata = new DefaultMetadata();
        assertNull("parentIdentifier", metadata.getParentIdentifier());
        metadata.setParentIdentifier("ParentID");
        assertEquals("parentIdentifier", "ParentID", metadata.getParentIdentifier());

        DefaultCitation c = (DefaultCitation) metadata.getParentMetadata();
        assertTitleEquals("parentMetadata", "ParentID", c);
        c.setTitle(new SimpleInternationalString("New parent"));
        assertEquals("parentIdentifier", "New parent", metadata.getParentIdentifier());
    }

    /**
     * Tests {@link DefaultMetadata#getHierarchyLevels()}, {@link DefaultMetadata#getHierarchyLevelNames()},
     * {@link DefaultMetadata#setHierarchyLevel(Collection)} and {@link DefaultMetadata#setHierarchyLevelNames(Collection)}
     * methods.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testHierarchyLevels() {
        final String[]    names  = new String[] {"Bridges", "Golden Gate Bridge"};
        final ScopeCode[] levels = new ScopeCode[] {ScopeCode.FEATURE_TYPE, ScopeCode.FEATURE};
        final DefaultMetadata metadata = new DefaultMetadata();
        assertTrue("hierarchyLevelNames", metadata.getHierarchyLevelNames().isEmpty());
        assertTrue("hierarchyLevels",     metadata.getHierarchyLevels().isEmpty());
        /*
         * Tests the setter and verify immediately with the getter methods.
         */
        metadata.setHierarchyLevelNames(Arrays.asList(names));
        metadata.setHierarchyLevels(Arrays.asList(levels));
        assertArrayEquals("hierarchyLevelNames", names,  metadata.getHierarchyLevelNames().toArray());
        assertArrayEquals("hierarchyLevels",     levels, metadata.getHierarchyLevels().toArray());
        /*
         * The above deprecated methods shall have created MetadataScope object. Verify that.
         */
        final Collection<DefaultMetadataScope> scopes = metadata.getMetadataScopes();
        final Iterator<DefaultMetadataScope> it = scopes.iterator();
        DefaultMetadataScope scope = it.next();
        assertEquals("metadataScopes[0].name", "Bridges", scope.getName().toString());
        assertEquals("metadataScopes[0].resourceScope", ScopeCode.FEATURE_TYPE, scope.getResourceScope());
        scope = it.next();
        assertEquals("metadataScopes[1].name", "Golden Gate Bridge", scope.getName().toString());
        assertEquals("metadataScopes[1].resourceScope", ScopeCode.FEATURE, scope.getResourceScope());
        /*
         * Changes in the MetadataScope object shall be reflected immediately on the scope collection.
         * Verify that.
         */
        it.remove();
        assertFalse(it.hasNext());
        final DefaultMetadataScope c = new DefaultMetadataScope(
                levels[1] = ScopeCode.ATTRIBUTE_TYPE,
                names [1] = "Clearance");
        assertTrue(scopes.add(c));
        assertArrayEquals("hierarchyLevelNames", names,  metadata.getHierarchyLevelNames().toArray());
        assertArrayEquals("hierarchyLevels",     levels, metadata.getHierarchyLevels().toArray());
        /*
         * Test the customized equals(Object) and hashCode() implementations.
         * Note: the 'assertNotSame' check is not a contract requirement. It is just that if
         * 'n1' and 'n2' are the same, then the test become pointless and should be removed.
         */
        Collection<String> n1 = metadata.getHierarchyLevelNames();
        Collection<String> n2 = metadata.getHierarchyLevelNames();
        assertNotSame("Remove this test.", n1, n2); // See above comment.
        assertTrue("equals", n1.equals(n2));
        assertTrue("equals", n2.equals(n1));
        assertEquals("hashCode", n1.hashCode(), n2.hashCode());
    }

    /**
     * Tests {@link DefaultMetadata#getDateStamp()} and {@link DefaultMetadata#setDateStamp(Date)} methods.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testDateStamp() {
        final DefaultMetadata metadata = new DefaultMetadata();
        assertNull("dateStamp", metadata.getDateStamp());
        /*
         * Verifies that the deprecated method get its value from the CitationDate objects.
         */
        Date creation = date("2014-10-07 00:00:00");
        final DefaultCitationDate[] dates = new DefaultCitationDate[] {
                new DefaultCitationDate(date("2014-10-09 00:00:00"), DateType.valueOf("LAST_UPDATE")),
                new DefaultCitationDate(creation, DateType.CREATION)
        };
        metadata.setDateInfo(Arrays.asList(dates));
        assertEquals("dateStamp", creation, metadata.getDateStamp());
        /*
         * Invoking the deprecated setters shall modify the CitationDate object
         * associated to DateType.CREATION.
         */
        creation = date("2014-10-06 00:00:00");
        metadata.setDateStamp(creation);
        assertEquals("citationDates[1].date", creation, dates[1].getDate());
        assertArrayEquals("dates", dates, metadata.getDateInfo().toArray());
    }

    /**
     * Tests {@link DefaultMetadata#getMetadataStandardName()}, {@link DefaultMetadata#getMetadataStandardVersion()},
     * {@link DefaultMetadata#setMetadataStandardName(String)} and {@link DefaultMetadata#setMetadataStandardVersion(String)}
     * methods.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testMetadataStandard() {
        final DefaultMetadata metadata = new DefaultMetadata();
        assertNull("metadataStandardName",    metadata.getMetadataStandardName());
        assertNull("metadataStandardVersion", metadata.getMetadataStandardVersion());

        String name = "ISO 19115-2 Geographic Information - Metadata Part 2 Extensions for imagery and gridded data";
        String version = "ISO 19115-2:2009(E)";
        metadata.setMetadataStandardName(name);
        metadata.setMetadataStandardVersion(version);
        assertEquals("metadataStandardName",    name,    metadata.getMetadataStandardName());
        assertEquals("metadataStandardVersion", version, metadata.getMetadataStandardVersion());
        final Citation standard = getSingleton(metadata.getMetadataStandards());
        assertTitleEquals("standard", name, standard);
        assertEquals(version, standard.getEdition().toString());
    }

    /**
     * Tests {@link DefaultMetadata#getDataSetUri()}.
     *
     * @throws URISyntaxException Should not happen.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testDataSetUri() throws URISyntaxException {
        final DefaultMetadata metadata = new DefaultMetadata();
        metadata.setDataSetUri("file:/tmp/myfile.txt");
        assertEquals("file:/tmp/myfile.txt", metadata.getDataSetUri());
    }
}
