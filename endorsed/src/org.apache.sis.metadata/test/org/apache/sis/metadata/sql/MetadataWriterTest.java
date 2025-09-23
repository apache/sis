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
package org.apache.sis.metadata.sql;

import java.util.Set;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.Telephone;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.iso.citation.DefaultTelephone;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.citation.Contact;
import org.opengis.metadata.citation.Party;
import org.opengis.metadata.citation.Responsibility;
import org.apache.sis.util.internal.shared.URLs;


/**
 * Creates a metadata database, stores a few elements and read them back.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class MetadataWriterTest extends TestCase {
    /**
     * The data source providing connections to the database.
     */
    private MetadataWriter source;

    /**
     * Creates a new test case.
     */
    public MetadataWriterTest() {
    }

    /**
     * Runs all tests on Derby in the required order.
     *
     * @throws Exception if an error occurred while writing or reading the database.
     */
    @Test
    public void testDerby() throws Exception {
        try (final TestDatabase db = TestDatabase.create("MetadataWriter")) {
            source = new MetadataWriter(MetadataStandard.ISO_19115, db.source, null, null);
            try {
                write();
                search();
                read();
                readWriteDeprecated();
            } finally {
                source.close();
            }
        }
    }

    /**
     * Runs all tests on PostgreSQL in the required order. This test is disabled by default
     * because it requires manual setup of a test database.
     *
     * @throws Exception if an error occurred while writing or reading the database.
     */
    @Test
    @ResourceLock(TestDatabase.POSTGRESQL)
    public void testPostgreSQL() throws Exception {
        try (final TestDatabase db = TestDatabase.createOnPostgreSQL("MetadataWriter", true)) {
            source = new MetadataWriter(MetadataStandard.ISO_19115, db.source, "MetadataWriter", null);
            try {
                write();
                search();
                read();
                readWriteDeprecated();
            } finally {
                source.close();
            }
        }
    }

    /**
     * Creates a new temporary database and write elements in it.
     *
     * @throws MetadataStoreException if an error occurred while writing or reading the database.
     */
    private void write() throws MetadataStoreException {
        assertEquals("ISO 19115", source.add(HardCodedCitations.ISO_19115));
        assertEquals("EPSG",      source.add(HardCodedCitations.EPSG));
        assertEquals("SIS",       source.add(HardCodedCitations.SIS));
    }

    /**
     * Searches known entries in the database.
     *
     * @throws MetadataStoreException if an error occurred while reading the database.
     */
    private void search() throws MetadataStoreException {
        assertNull  (             source.search(HardCodedCitations.ISO_19111));
        assertEquals("ISO 19115", source.search(HardCodedCitations.ISO_19115));
        assertEquals("EPSG",      source.search(HardCodedCitations.EPSG));
        assertEquals("SIS",       source.search(HardCodedCitations.SIS));
        assertNull  (             source.search(HardCodedCitations.ISO_19111));
        assertEquals("{rp}EPSG",  source.search(TestUtilities.getSingleton(
                HardCodedCitations.EPSG.getCitedResponsibleParties())));
    }

    /**
     * Reads known entries in the database.
     * Expected entry is:
     *
     * <pre class="text">
     *   Citation
     *     ├─Title………………………………………………………… EPSG Geodetic Parameter Dataset
     *     ├─Identifier
     *     │   └─Code………………………………………………… EPSG
     *     ├─Cited responsible party
     *     │   ├─Party
     *     │   │   ├─Name……………………………………… International Association of Oil &amp; Gas Producers
     *     │   │   └─Contact info
     *     │   │       └─Online resource
     *     │   │           ├─Linkage………… https://epsg.org/
     *     │   │           └─Function……… Information
     *     │   └─Role………………………………………………… Principal investigator
     *     └─Presentation form………………………… Table digital</pre>
     *
     * @throws MetadataStoreException if an error occurred while reading the database.
     */
    private void read() throws MetadataStoreException {
        final Citation c = source.lookup(Citation.class, "EPSG");
        assertEquals("EPSG Geodetic Parameter Dataset", c.getTitle().toString());
        assertEquals(PresentationForm.TABLE_DIGITAL, TestUtilities.getSingleton(c.getPresentationForms()));
        /*
         * Ask for dependencies that are known to exist.
         */
        final Responsibility responsible = TestUtilities.getSingleton(c.getCitedResponsibleParties());
        assertEquals(Role.PRINCIPAL_INVESTIGATOR, responsible.getRole());

        final Party party = TestUtilities.getSingleton(responsible.getParties());
        assertEquals("International Association of Oil & Gas Producers", party.getName().toString());
        final Contact contact = TestUtilities.getSingleton(party.getContactInfo());
        /*
         * Invoke the deprecated `getOnlineResource()` method (singular form) before the non-deprecated
         * `getOnlineResources()` (plural form) replacement. They shall give the same result no matter
         * which form were stored in the database.
         */
        @SuppressWarnings("deprecation")
        final OnlineResource resource = contact.getOnlineResource();
        assertSame(resource, TestUtilities.getSingleton(contact.getOnlineResources()));
        assertEquals(URLs.EPSG, resource.getLinkage().toString());
        assertEquals(OnLineFunction.INFORMATION, resource.getFunction());
        /*
         * Ask columns that are known to not exist.
         */
        assertNull(c.getEditionDate());
        assertTrue(c.getDates().isEmpty());
        assertEquals(0, c.getAlternateTitles().size());
        /*
         * Test the cache.
         */
        assertSame   (c, source.lookup(Citation.class, "EPSG"));
        assertNotSame(c, source.lookup(Citation.class, "SIS"));
        /*
         * Should return the identifier with no search. Actually the real test is the call to "proxy",
         * since there is no way to ensure that the call to "search" tooks the short path (except by
         * looking at the debugger). But if "proxy" succeed, then "search" should be okay.
         */
        assertEquals("EPSG", source.proxy (c));
        assertEquals("EPSG", source.search(c));
    }

    /**
     * Read and write a metadata object containing deprecated properties.
     * The metadata tested by this method is:
     *
     * <pre class="text">
     *   Telephone
     *     ├─Number………………… 01.02.03.04
     *     └─Number type…… Voice</pre>
     *
     * The metadata should be stored in columns named {@code "number"} and {@code "numberType"} even if we
     * constructed the metadata using the deprecated {@code "voice"} property. Conversely, at reading time
     * the deprecated {@code "voice"} property should be converted in reading of non-deprecated properties.
     */
    @SuppressWarnings("deprecation")
    private void readWriteDeprecated() throws MetadataStoreException {
        final DefaultTelephone tel = new DefaultTelephone();
        tel.setVoices(Set.of("01.02.03.04"));
        assertEquals("01.02.03.04", source.add(tel));

        final Telephone check = source.lookup(Telephone.class, "01.02.03.04");
        assertEquals("01.02.03.04", TestUtilities.getSingleton(check.getVoices()));
    }
}
