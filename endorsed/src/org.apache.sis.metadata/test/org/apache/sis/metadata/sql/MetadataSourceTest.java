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
import java.util.Collection;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.distribution.Format;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.distribution.DefaultFormat;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestStep;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import java.util.Collections;
import static org.apache.sis.test.Assertions.assertSingleton;


/**
 * Tests {@link MetadataSource}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class MetadataSourceTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public MetadataSourceTest() {
    }

    /**
     * Tests {@link MetadataSource} with an in-memory Derby database.
     * This method delegates its work to all other methods in this class that expect a {@link MetadataSource} argument.
     *
     * @throws Exception if an error occurred while executing the script runner.
     */
    @Test
    public void testOnDerby() throws Exception {
        try (TestDatabase db = TestDatabase.create("MetadataSource")) {
            testAll(db);
        }
    }

    /**
     * Tests {@link MetadataSource} with a PostgreSQL database if available.
     *
     * @throws Exception if an error occurred while executing the script runner.
     */
    @Test
    @ResourceLock(TestDatabase.POSTGRESQL)
    public void testOnPostgreSQL() throws Exception {
        try (TestDatabase db = TestDatabase.createOnPostgreSQL("metadata", false)) {
            testAll(db);
        }
    }

    /**
     * Runs all public tests declared in this class.
     * Also opportunistically run tests declared in {@link MetadataFallbackVerifier}.
     */
    private static void testAll(final TestDatabase db) throws Exception {
        try (MetadataSource source = new MetadataSource(MetadataStandard.ISO_19115, db.source, "metadata", null)) {
            source.install();
            verifyFormats(source);
            testSearch(source);
            ensureReadOnly(source);

            // Opportunistic verification using the database we have at hand.
            MetadataFallbackVerifier.compare(source);
        }
    }

    /**
     * Tests {@link MetadataSource#lookup(Class, String)} be fetching some {@link Format} instances from
     * the given source. The first call to the {@code lookup(…)} method will trig the database installation.
     *
     * @param  source  the instance to test.
     * @throws MetadataStoreException if an error occurred while querying the database.
     */
    @TestStep
    public static void verifyFormats(final MetadataSource source) throws MetadataStoreException {
        verify(source.lookup(Format.class, "PNG"),     "PNG",     "PNG (Portable Network Graphics) Specification");
        verify(source.lookup(Format.class, "NetCDF"),  "NetCDF",  "NetCDF Classic and 64-bit Offset Format");
        verify(source.lookup(Format.class, "GeoTIFF"), "GeoTIFF", "GeoTIFF Coverage Encoding Profile");
        verify(source.lookup(Format.class, "CSV"),     "CSV",     "Common Format and MIME Type for Comma-Separated Values (CSV) Files");
        verify(source.lookup(Format.class, "CSV-MF"),  "CSV",     "OGC Moving Features Encoding Extension: Simple Comma-Separated Values (CSV)");
    }

    /**
     * Verifies properties of the given format.
     *
     * @param format        the instance to verify.
     * @param abbreviation  the expected format alternate title.
     * @param title         the expected format title.
     */
    private static void verify(final Format format, final String abbreviation, final String title) {
        final Citation spec = format.getFormatSpecificationCitation();
        assertNotNull(spec, "formatSpecificationCitation");
        assertEquals(abbreviation, String.valueOf(assertSingleton(spec.getAlternateTitles())), "abbreviation");
        assertEquals(title, String.valueOf(spec.getTitle()), "title");
    }

    /**
     * Tests {@link MetadataSource#search(Object)}
     *
     * @param  source  the instance to test.
     * @throws MetadataStoreException if an error occurred while querying the database.
     */
    @TestStep
    public static void testSearch(final MetadataSource source) throws MetadataStoreException {
        final var specification = new DefaultCitation("PNG (Portable Network Graphics) Specification");
        specification.setAlternateTitles(Set.of(new SimpleInternationalString("PNG")));
        final var format = new DefaultFormat();
        format.setFormatSpecificationCitation(specification);

        assertEquals("PNG", source.search(format));
        specification.setTitle(null);
        assertNull(source.search(format));
    }

    /**
     * Verifies that instances created by {@link MetadataSource} are read-only.
     * In particular, it should not be possible to add elements in the collection.
     *
     * @param  source  the instance to test.
     * @throws MetadataStoreException if an error occurred while querying the database.
     */
    @TestStep
    public static void ensureReadOnly(final MetadataSource source) throws MetadataStoreException {
        final Citation c = source.lookup(Citation.class, "SIS");
        @SuppressWarnings("unchecked")                                  // Cheat or the purpose of this test.
        var titles = (Collection<InternationalString>) c.getAlternateTitles();
        var more = new SimpleInternationalString("An open source project.");
        var e = assertThrows(UnsupportedOperationException.class, () -> titles.add(more),
                             "Predefined metadata should be unmodifiable.");
        assertNotNull(e);
    }
}
