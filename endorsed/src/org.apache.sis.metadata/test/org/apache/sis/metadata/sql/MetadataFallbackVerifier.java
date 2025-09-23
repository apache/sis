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
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.internal.CitationConstant;
import org.apache.sis.metadata.iso.citation.Citations;
import static org.apache.sis.util.internal.shared.CollectionsExt.first;
import org.apache.sis.util.internal.shared.Constants;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.citation.Party;
import org.opengis.metadata.citation.Responsibility;


/**
 * Compares the {@link MetadataFallback} hard-coded values with the {@code Citations.sql} content.
 * This test is actually invoked by {@link MetadataSourceTest} in order to opportunistically use
 * the database created by the latter (i.e. for avoiding to recreate the same database many times).
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class MetadataFallbackVerifier {
    /**
     * Identifier for which {@link MetadataFallback} does not provide hard-coded values.
     */
    private static final Set<String> EXCLUDES = Set.of(Constants.NETCDF, Constants.GEOTIFF, "ArcGIS", "MapInfo");

    /**
     * Creates a new test case.
     */
    public MetadataFallbackVerifier() {
    }

    /**
     * Creates a temporary database for comparing {@link MetadataFallback} content with database content.
     * This method is provided for allowing to execute this class individually. In a complete Maven build,
     * of {@code org.apache.sis.metadata} module, the test will rather be executed by {@link MetadataSourceTest}
     * for opportunistic reasons.
     *
     * @throws Exception if an exception occurred while creating or comparing the database.
     */
    @Test
    public void compare() throws Exception {
        try (TestDatabase db = TestDatabase.create("MetadataFallback");
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
            assertEquals(exclude, fromFB == null, name);        // Verify that missing fallbacks are known ones.
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
            assertNull(actualAltTitle, name);
        } else {
            assertEquals(fromDB.getTitle(), fromFB.getTitle(), name);
            if (actualAltTitle != null) {
                assertEquals(expectedAltTitle, actualAltTitle, name);
            }
        }
        /*
         * The fallback may not declare all identifiers (but it should not declare more).
         * If it declares an identifier, it should be equal.
         */
        final Identifier expectedID = first(fromDB.getIdentifiers());
        final Identifier actualID   = first(fromFB.getIdentifiers());
        if (expectedID == null) {
            assertNull(actualID, name);
        } else if (actualID != null) {
            assertEquals(expectedID.getCode(),      actualID.getCode(),      name);
            assertEquals(expectedID.getCodeSpace(), actualID.getCodeSpace(), name);
            assertEquals(expectedID.getVersion(),   actualID.getVersion(),   name);
        }
        /*
         * The fallback may not declare all responsible parties.
         * If it declares a party, the name and role shall be equal.
         */
        final Responsibility expectedResp = first(fromDB.getCitedResponsibleParties());
        final Responsibility actualResp   = first(fromFB.getCitedResponsibleParties());
        if (expectedResp == null) {
            assertNull(actualResp, name);
        } else if (actualResp != null) {
            assertEquals(expectedResp.getRole(), actualResp.getRole(), name);
            final Party expectedParty = first(expectedResp.getParties());
            final Party actualParty = first(actualResp.getParties());
            assertEquals(expectedParty.getName(), actualParty.getName(), name);
        }
        assertEquals(first(fromDB.getPresentationForms()),
                     first(fromFB.getPresentationForms()), name);
    }
}
