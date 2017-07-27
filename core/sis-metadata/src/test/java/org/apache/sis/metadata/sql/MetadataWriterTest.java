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

import java.util.Collections;
import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.PresentationForm;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.citation.Role;
import org.opengis.metadata.citation.Telephone;
import org.apache.sis.internal.metadata.sql.TestDatabase;
import org.apache.sis.metadata.iso.citation.HardCodedCitations;
import org.apache.sis.metadata.iso.citation.DefaultTelephone;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

// Branch-dependent imports
import org.opengis.metadata.citation.ResponsibleParty;


/**
 * Creates a metadata database, stores a few elements and read them back.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
@DependsOn({
    MetadataSourceTest.class,
    IdentifierGeneratorTest.class
})
public final strictfp class MetadataWriterTest extends TestCase {
    /**
     * The data source providing connections to the database.
     */
    private MetadataWriter source;

    /**
     * Runs all tests on JavaDB in the required order.
     *
     * @throws Exception if an error occurred while writing or reading the database.
     */
    @Test
    public void testDerby() throws Exception {
        final DataSource ds = TestDatabase.create("MetadataWriter");
        source = new MetadataWriter(MetadataStandard.ISO_19115, ds, null, null);
        try {
            write();
            search();
            read();
            readWriteDeprecated();
            source.close();
        } finally {
            TestDatabase.drop(ds);
        }
    }

    /**
     * Runs all tests on PostgreSQL in the required order. This test is disabled by default
     * because it requires manual setup of a test database.
     *
     * @throws Exception if an error occurred while writing or reading the database.
     */
    @Test
    @Ignore("This test need to be run manually on a machine having a local PostgreSQL database.")
    public void testPostgreSQL() throws Exception {
        final PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setServerName("localhost");
        ds.setDatabaseName("SpatialMetadataTest");
        source = new MetadataWriter(MetadataStandard.ISO_19115, ds, "metadata", null);
        try {
            write();
            search();
            read();
            readWriteDeprecated();
        } finally {
            source.close();
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
        assertNull  ("ISO 19111", source.search(HardCodedCitations.ISO_19111));
        assertEquals("ISO 19115", source.search(HardCodedCitations.ISO_19115));
        assertEquals("EPSG",      source.search(HardCodedCitations.EPSG));
        assertEquals("SIS",       source.search(HardCodedCitations.SIS));
        assertNull  ("ISO 19111", source.search(HardCodedCitations.ISO_19111));
        assertEquals("EPSG",      source.search(TestUtilities.getSingleton(
                HardCodedCitations.EPSG.getCitedResponsibleParties())));
    }

    /**
     * Reads known entries in the database.
     * Expected entry is:
     *
     * {@preformat text
     *   Citation
     *     ├─Title………………………………………………………… EPSG Geodetic Parameter Dataset
     *     ├─Identifier
     *     │   └─Code………………………………………………… EPSG
     *     ├─Cited responsible party
     *     │   ├─Party
     *     │   │   ├─Name……………………………………… International Association of Oil & Gas Producers
     *     │   │   └─Contact info
     *     │   │       └─Online resource
     *     │   │           ├─Linkage………… http://www.epsg.org
     *     │   │           └─Function……… Information
     *     │   └─Role………………………………………………… Principal investigator
     *     └─Presentation form………………………… Table digital
     * }
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
        final ResponsibleParty responsible = TestUtilities.getSingleton(c.getCitedResponsibleParties());
        assertEquals(Role.PRINCIPAL_INVESTIGATOR, responsible.getRole());

        assertEquals("International Association of Oil & Gas Producers", responsible.getOrganisationName().toString());

        OnlineResource resource = responsible.getContactInfo().getOnlineResource();
        assertEquals("http://www.epsg.org", resource.getLinkage().toString());
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
     * {@preformat text
     *   Telephone
     *     ├─Number………………… 01.02.03.04
     *     └─Number type…… Voice
     * }
     *
     * The metadata should be stored in columns named {@code "number"} and {@code "numberType"} even if we
     * constructed the metadata using the deprecated {@code "voice"} property. Conversely, at reading time
     * the deprecated {@code "voice"} property should be converted in reading of non-deprecated properties.
     */
    @SuppressWarnings("deprecation")
    private void readWriteDeprecated() throws MetadataStoreException {
        final DefaultTelephone tel = new DefaultTelephone();
        tel.setVoices(Collections.singleton("01.02.03.04"));
        assertEquals("01.02.03.04", source.add(tel));

        final Telephone check = source.lookup(Telephone.class, "01.02.03.04");
        assertEquals("01.02.03.04", TestUtilities.getSingleton(check.getVoices()));
    }
}
