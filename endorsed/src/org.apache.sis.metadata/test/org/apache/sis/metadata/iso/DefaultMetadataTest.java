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

import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.net.URISyntaxException;
import jakarta.xml.bind.JAXBException;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.DateType;
import org.opengis.metadata.maintenance.ScopeCode;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.citation.DefaultCitationDate;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.xml.test.TestCase;
import static org.apache.sis.test.Assertions.assertSingleton;
import static org.apache.sis.test.Assertions.assertSingletonCitation;
import static org.apache.sis.metadata.Assertions.assertTitleEquals;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.MetadataScope;


/**
 * Tests {@link DefaultMetadata}, without Coordinate Reference System (CRS) information.
 *
 * <p><b>Note:</b> a metadata object with CRS information is tested by a different
 * {@code org.apache.sis.test.integration.DefaultMetadataTest} class in the
 * {@code org.apache.sis.referencing} module.</p>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 */
@SuppressWarnings({"exports", "deprecation"})
public final class DefaultMetadataTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultMetadataTest() {
    }

    /**
     * Tests unmarshalling of a metadata having a collection that contains no element.
     * This was used to cause a {@code NullPointerException} prior SIS-139 fix.
     *
     * @throws JAXBException if an error occurred during the during unmarshalling processes.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-139">SIS-139</a>
     */
    @Test
    public void testEmptyCollection() throws JAXBException {
        final DefaultMetadata metadata = unmarshal(DefaultMetadata.class,
                "<mdb:MD_Metadata xmlns:mdb=\"" + Namespaces.MDB + "\">\n" +
                "  <mdb:contact/>\n" +
                "</mdb:MD_Metadata>");
        /*
         * Verify metadata property.
         */
        assertTrue(metadata.getContacts().isEmpty());
    }

    /**
     * Tests legacy methods related to file identifiers.
     * The following methods should delegate to newer methods:
     *
     * <ul>
     *   <li>{@link DefaultMetadata#getFileIdentifier()}</li>
     *   <li>{@link DefaultMetadata#setFileIdentifier(String)}</li>
     * </ul>
     */
    @Test
    public void testFileIdentifier() {
        final var metadata = new DefaultMetadata();
        assertNull(metadata.getFileIdentifier());
        metadata.setFileIdentifier("Apache SIS/Metadata test");
        assertEquals("Apache SIS/Metadata test", metadata.getMetadataIdentifier().getCode());
        assertEquals("Apache SIS/Metadata test", metadata.getFileIdentifier());
    }

    /**
     * Tests legacy methods related to locales.
     * The following methods should delegate to newer methods:
     *
     * <ul>
     *   <li>{@link DefaultMetadata#getLanguage()}</li>
     *   <li>{@link DefaultMetadata#setLanguage(Locale)}</li>
     *   <li>{@link DefaultMetadata#getLocales()}</li>
     * </ul>
     */
    @Test
    public void testLocales() {
        final var metadata = new DefaultMetadata();
        assertNull(metadata.getLanguage());
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
        Collections.addAll(metadata.getLocales(), Locale.FRENCH, Locale.ENGLISH);
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
     * Tests methods related to languages.
     * The following methods should delegate to newer methods:
     *
     * <ul>
     *   <li>{@link DefaultMetadata#getLocalesAndCharsets()}</li>
     *   <li>{@link DefaultMetadata#getLanguage()}</li>
     *   <li>{@link DefaultMetadata#getLocales()}</li>
     * </ul>
     */
    private static void assertLanguagesEquals(final DefaultMetadata metadata, final Locale... expected) {
        assertArrayEquals(expected,    metadata.getLocalesAndCharsets().keySet().toArray());
        assertEquals     (expected[0], metadata.getLanguage());
        assertArrayEquals(Arrays.copyOfRange(expected, 1, expected.length), metadata.getLocales().toArray());
    }

    /**
     * Tests legacy methods related to metadata identifiers.
     * The following methods should delegate to newer methods:
     *
     * <ul>
     *   <li>{@link DefaultMetadata#getParentIdentifier()}</li>
     *   <li>{@link DefaultMetadata#setParentIdentifier(String)}</li>
     * </ul>
     */
    @Test
    public void testParentIdentifier() {
        final var metadata = new DefaultMetadata();
        assertNull(metadata.getParentIdentifier());
        metadata.setParentIdentifier("ParentID");
        assertEquals("ParentID", metadata.getParentIdentifier());

        var c = assertInstanceOf(DefaultCitation.class, metadata.getParentMetadata());
        assertTitleEquals("ParentID", c, "parentMetadata");
        c.setTitle(new SimpleInternationalString("New parent"));
        assertEquals("New parent", metadata.getParentIdentifier());
    }

    /**
     * Tests legacy methods related to metadata hierarchy levels.
     * The following methods should delegate to newer methods:
     *
     * <ul>
     *   <li>{@link DefaultMetadata#getHierarchyLevels()}</li>
     *   <li>{@link DefaultMetadata#getHierarchyLevelNames()}</li>
     *   <li>{@link DefaultMetadata#setHierarchyLevels(Collection)}</li>
     *   <li>{@link DefaultMetadata#setHierarchyLevelNames(Collection)}</li>
     * </ul>
     */
    @Test
    public void testHierarchyLevels() {
        final var names    = new String[] {"Bridges", "Golden Gate Bridge"};
        final var levels   = new ScopeCode[] {ScopeCode.FEATURE_TYPE, ScopeCode.FEATURE};
        final var metadata = new DefaultMetadata();
        assertTrue(metadata.getHierarchyLevelNames().isEmpty());
        assertTrue(metadata.getHierarchyLevels().isEmpty());
        /*
         * Tests the setter and verify immediately with the getter methods.
         */
        metadata.setHierarchyLevelNames(Arrays.asList(names));
        metadata.setHierarchyLevels(Arrays.asList(levels));
        assertArrayEquals(names,  metadata.getHierarchyLevelNames().toArray());
        assertArrayEquals(levels, metadata.getHierarchyLevels().toArray());
        /*
         * The above deprecated methods shall have created MetadataScope object. Verify that.
         */
        final Collection<MetadataScope> scopes = metadata.getMetadataScopes();
        final Iterator<MetadataScope> it = scopes.iterator();
        MetadataScope scope = it.next();
        assertEquals("Bridges", scope.getName().toString());
        assertEquals(ScopeCode.FEATURE_TYPE, scope.getResourceScope());
        scope = it.next();
        assertEquals("Golden Gate Bridge", scope.getName().toString());
        assertEquals(ScopeCode.FEATURE, scope.getResourceScope());
        /*
         * Changes in the MetadataScope object shall be reflected immediately on the scope collection.
         * Verify that.
         */
        it.remove();
        assertFalse(it.hasNext());
        final var c = new DefaultMetadataScope(
                levels[1] = ScopeCode.ATTRIBUTE_TYPE,
                names [1] = "Clearance");
        assertTrue(scopes.add(c));
        assertArrayEquals(names,  metadata.getHierarchyLevelNames().toArray());
        assertArrayEquals(levels, metadata.getHierarchyLevels().toArray());
        /*
         * Test the customized equals(Object) and hashCode() implementations.
         * Note: the `assertNotSame` check is not a contract requirement. It is just that if
         * `n1` and `n2` are the same, then the test become pointless and should be removed.
         */
        Collection<String> n1 = metadata.getHierarchyLevelNames();
        Collection<String> n2 = metadata.getHierarchyLevelNames();
        assertNotSame(n1, n2, "Remove this test.");                 // See above comment.
        assertTrue(n1.equals(n2));
        assertTrue(n2.equals(n1));
        assertEquals(n1.hashCode(), n2.hashCode());
    }

    /**
     * Tests legacy methods related to date stamps.
     * The following methods should delegate to newer methods:
     *
     * <ul>
     *   <li>{@link DefaultMetadata#getDateStamp()}</li>
     *   <li>{@link DefaultMetadata#setDateStamp(Date)}</li>
     * </ul>
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testDateStamp() {
        final var metadata = new DefaultMetadata();
        assertNull(metadata.getDateStamp());
        /*
         * Verifies that the deprecated method get its value from the CitationDate objects.
         */
        Instant creation = Instant.parse("2014-10-07T00:00:00Z");
        final var dates = new DefaultCitationDate[] {
                new DefaultCitationDate(Instant.parse("2014-10-09T00:00:00Z"), DateType.LAST_UPDATE),
                new DefaultCitationDate(creation, DateType.CREATION)
        };
        metadata.setDateInfo(Arrays.asList(dates));
        assertEquals(creation, metadata.getDateStamp().toInstant());
        /*
         * Invoking the deprecated setters shall modify the CitationDate object
         * associated to DateType.CREATION.
         */
        creation = Instant.parse("2014-10-06T00:00:00Z");
        metadata.setDateStamp(Date.from(creation));
        assertEquals(creation, dates[1].getDate().toInstant());
        assertArrayEquals(dates, metadata.getDateInfo().toArray());
    }

    /**
     * Tests legacy methods related to metadata version.
     * The following methods should delegate to newer methods:
     *
     * <ul>
     *   <li>{@link DefaultMetadata#getMetadataStandardName()}</li>
     *   <li>{@link DefaultMetadata#getMetadataStandardVersion()}</li>
     *   <li>{@link DefaultMetadata#setMetadataStandardName(String)}</li>
     *   <li>{@link DefaultMetadata#setMetadataStandardVersion(String)}</li>
     * </ul>
     */
    @Test
    public void testMetadataStandard() {
        final var metadata = new DefaultMetadata();
        assertNull(metadata.getMetadataStandardName());
        assertNull(metadata.getMetadataStandardVersion());

        String name = "ISO 19115-2 Geographic Information - Metadata Part 2 Extensions for imagery and gridded data";
        String version = "ISO 19115-2:2019";
        metadata.setMetadataStandardName(name);
        metadata.setMetadataStandardVersion(version);
        assertEquals(name,    metadata.getMetadataStandardName());
        assertEquals(version, metadata.getMetadataStandardVersion());
        final Citation standard = assertSingleton(metadata.getMetadataStandards());
        assertTitleEquals(name, standard, "standard");
        assertEquals(version, standard.getEdition().toString());
    }

    /**
     * Tests legacy methods related to <abbr>URI</abbr>.
     * The following methods should delegate to newer methods:
     *
     * <ul>
     *   <li>{@link DefaultMetadata#getDataSetUri()}</li>
     * </ul>
     *
     * @throws URISyntaxException if the URI used in this test is malformed.
     */
    @Test
    public void testDataSetUri() throws URISyntaxException {
        final var metadata = new DefaultMetadata();
        metadata.setDataSetUri("file:/tmp/myfile.txt");
        assertEquals("file:/tmp/myfile.txt", metadata.getDataSetUri());
        assertEquals("file:/tmp/myfile.txt",
                assertSingleton(assertSingletonCitation(metadata).getOnlineResources()).getLinkage().toString());
    }
}
