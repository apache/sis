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

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Party;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.Responsibility;
import org.apache.sis.internal.simple.CitationConstant;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.test.sql.TestDatabase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.internal.util.CollectionsExt.first;


/**
 * Compares the {@link MetadataFallback} hard-coded values with the {@code Citations.sql} content.
 * This test is actually invoked by {@link MetadataSourceTest} in order to opportunistically use
 * the database created by the latter (i.e. for avoiding to recreate the same database many times).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
public final strictfp class MetadataFallbackVerifier {
    /**
     * Identifier for which {@link MetadataFallback} does not provide hard-coded values.
     */
    private static final Set<String> EXCLUDES = new HashSet<>(Arrays.asList("NetCDF", "GeoTIFF", "ArcGIS", "MapInfo"));

    /**
     * Creates a temporary database for comparing {@link MetadataFallback} content with database content.
     * This method is provided for allowing to execute this class individually. In a complete Maven build,
     * of {@code sis-metadata} module, the test will rather be executed by {@link MetadataSourceTest} for
     * opportunistic reasons.
     *
     * @throws Exception if an exception occurred while creating or comparing the database.
     */
    @Test
    public void compare() throws Exception {
        try (TestDatabase db = TestDatabase.create("MetadataSource");
             MetadataSource source = new MetadataSource(MetadataStandard.ISO_19115, db.source, "metadata", null))
        {
            source.install();
            compare(source);
        }
    }

    /**
     * Compares {@link MetadataFallback} content with database content using the given source.
     * This method is invoked by {@link MetadataSourceTest} for opportunistically reusing the
     * available database.
     */
    static void compare(final MetadataSource source) throws MetadataStoreException {
        for (final Citation c : Citations.values()) {
            final String name = ((CitationConstant) c).title;
            final boolean exclude = EXCLUDES.contains(name);
            final Citation fromFB = MetadataFallback.createCitation(name);
            assertEquals(name, exclude, fromFB == null);        // Verify that missing fallbacks are known ones.
            if (!exclude) {
                compare(name, source.lookup(Citation.class, name), fromFB);
            }
        }
        compare("IOGP", source.lookup(Citation.class, "IOGP"), MetadataFallback.createCitation("IOGP"));
    }

    /**
     * Compares a fallback citation from the citation declared in the database.
     *
     * @param  name    identifier used in assertions for identifying which citation failed.
     * @param  fromDB  citation read from the database.
     * @param  fromFB  citation created by {@link MetadataFallback}.
     */
    private static void compare(final String name, final Citation fromDB, final Citation fromFB) {
        /*
         * The database may contain more verbose title than the one declared in MetadataFallback,
         * in which case the shorter title appears as alternate title.
         */
        final InternationalString expectedAltTitle = first(fromDB.getAlternateTitles());
        final InternationalString actualAltTitle   = first(fromFB.getAlternateTitles());
        if (fromFB.getTitle().equals(expectedAltTitle)) {
            assertNull(name, actualAltTitle);
        } else {
            assertEquals(name, fromDB.getTitle(), fromFB.getTitle());
            if (actualAltTitle != null) {
                assertEquals(name, expectedAltTitle, actualAltTitle);
            }
        }
        /*
         * The fallback may not declare all identifiers (but it should not declare more).
         * If it declares an identifier, it should be equal.
         */
        final Identifier expectedID = first(fromDB.getIdentifiers());
        final Identifier actualID   = first(fromFB.getIdentifiers());
        if (expectedID == null) {
            assertNull(name, actualID);
        } else if (actualID != null) {
            assertEquals(name, expectedID.getCode(),      actualID.getCode());
            assertEquals(name, expectedID.getCodeSpace(), actualID.getCodeSpace());
            assertEquals(name, expectedID.getVersion(),   actualID.getVersion());
        }
        /*
         * The fallback may not declare all responsible parties.
         * If it declares a party, the name and role shall be equal.
         */
        final Responsibility expectedResp = first(fromDB.getCitedResponsibleParties());
        final Responsibility actualResp   = first(fromFB.getCitedResponsibleParties());
        if (expectedResp == null) {
            assertNull(name, actualResp);
        } else if (actualResp != null) {
            assertEquals(name, expectedResp.getRole(), actualResp.getRole());
            final Party expectedParty = first(expectedResp.getParties());
            final Party actualParty = first(actualResp.getParties());
            assertEquals(name, expectedParty.getName(), actualParty.getName());
        }
        assertEquals(name, first(fromDB.getPresentationForms()),
                           first(fromFB.getPresentationForms()));
    }
}
