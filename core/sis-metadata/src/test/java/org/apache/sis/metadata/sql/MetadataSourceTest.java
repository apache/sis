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
import org.opengis.metadata.distribution.Format;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.internal.metadata.sql.TestDatabase;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.distribution.DefaultFormat;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestStep;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link MetadataSource}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
@DependsOn(org.apache.sis.internal.metadata.sql.ScriptRunnerTest.class)
public final strictfp class MetadataSourceTest extends TestCase {
    /**
     * Tests {@link MetadataSource} with an in-memory Derby database.
     * This method delegates its work to all other methods in this class that expect a {@link MetadataSource} argument.
     *
     * @throws Exception if an error occurred while executing the script runner.
     */
    @Test
    public void testOnDerby() throws Exception {
        final DataSource ds = TestDatabase.create("MetadataSource");
        try (MetadataSource source = new MetadataSource(MetadataStandard.ISO_19115, ds, "metadata", null)) {
            source.install();
            verifyFormats(source);
            testSearch(source);
        } finally {
            TestDatabase.drop(ds);
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
        assertEquals("abbreviation", abbreviation, String.valueOf(format.getName()));
        assertEquals("title", title, String.valueOf(format.getSpecification()));
    }

    /**
     * Tests {@link MetadataSource#search(Object)}
     *
     * @param  source  the instance to test.
     * @throws MetadataStoreException if an error occurred while querying the database.
     */
    @TestStep
    public static void testSearch(final MetadataSource source) throws MetadataStoreException {
        final DefaultCitation specification = new DefaultCitation("PNG (Portable Network Graphics) Specification");
        specification.setAlternateTitles(Collections.singleton(new SimpleInternationalString("PNG")));
        final DefaultFormat format = new DefaultFormat();
        format.setFormatSpecificationCitation(specification);

        assertEquals("PNG", source.search(format));
        specification.setTitle(null);
        assertNull(source.search(format));
    }
}
