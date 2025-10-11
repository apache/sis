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
package org.apache.sis.storage.esri;

import java.util.List;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.opengis.metadata.Metadata;
import org.apache.sis.coverage.Category;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.ProbeResult;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMultilinesEquals;
import static org.apache.sis.test.Assertions.assertSingleton;
import static org.apache.sis.test.Assertions.assertSingletonBBox;
import static org.apache.sis.test.Assertions.assertSingletonReferenceSystem;
import org.apache.sis.test.TestCase;

// Specific to the main branch:
import org.opengis.metadata.identification.DataIdentification;


/**
 * Tests {@link AsciiGridStore} and {@link AsciiGridStoreProvider}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class AsciiGridStoreTest extends TestCase {
    /**
     * Filename of the test file.
     */
    private static final String FILENAME = "grid.asc";

    /**
     * Creates a new test case.
     */
    public AsciiGridStoreTest() {
    }

    /**
     * Returns a storage connector with the URL to the test data.
     */
    private static StorageConnector testData() {
        return new StorageConnector(AsciiGridStoreTest.class.getResource(FILENAME));
    }

    /**
     * Tests {@link AsciiGridStoreProvider#probeContent(StorageConnector)} method.
     *
     * @throws DataStoreException if an error occurred while reading the CSV file.
     */
    @Test
    public void testProbeContent() throws DataStoreException {
        final AsciiGridStoreProvider p = new AsciiGridStoreProvider();
        final ProbeResult r = p.probeContent(testData());
        assertTrue(r.isSupported());
        assertEquals("text/plain", r.getMimeType());
    }

    /**
     * Tests the metadata of the {@value #FILENAME} file. This test reads only the header.
     * It should not test sample dimensions or pixel values, because doing so read the full
     * image and is the purpose of {@link #testRead()}.
     *
     * @throws DataStoreException if an error occurred while reading the file.
     */
    @Test
    public void testMetadata() throws DataStoreException {
        try (AsciiGridStore store = new AsciiGridStore(null, testData(), true)) {
            assertEquals("grid", store.getIdentifier().get().toString());
            final Metadata metadata = store.getMetadata();
            /*
             * Format information is hard-coded in "SpatialMetadata" database. Complete string should
             * be "ESRI ArcInfo ASCII Grid format" but it depends on the presence of Derby dependency.
             */
            final DataIdentification id = assertInstanceOf(
                    DataIdentification.class,
                    assertSingleton(metadata.getIdentificationInfo()));
            /*
             * This information should have been read from the PRJ file.
             */
            assertEquals("WGS 84 / World Mercator", assertSingletonReferenceSystem(metadata).getCode());
            final var bbox = assertSingletonBBox(id);
            assertEquals(-84, bbox.getSouthBoundLatitude(), 1);
            assertEquals(+85, bbox.getNorthBoundLatitude(), 1);
            /*
             * Verify that the metadata is cached.
             */
            assertSame(metadata, store.getMetadata());
        }
    }

    /**
     * Tests reading a few values from the {@value #FILENAME} file.
     *
     * @throws DataStoreException if an error occurred while reading the file.
     */
    @Test
    public void testRead() throws DataStoreException {
        try (AsciiGridStore store = new AsciiGridStore(null, testData(), true)) {
            final List<Category> categories = assertSingleton(store.getSampleDimensions()).getCategories();
            assertEquals(2, categories.size());
            assertEquals(   -2, categories.get(0).getSampleRange().getMinDouble(), 1);
            assertEquals(   30, categories.get(0).getSampleRange().getMaxDouble(), 1);
            assertEquals(-9999, categories.get(1).forConvertedValues(false).getSampleRange().getMinDouble());
            /*
             * Check sample values.
             */
            final GridCoverage coverage = store.read(null, null);
            final RenderedImage image = coverage.render(null);
            assertEquals(10, image.getWidth());
            assertEquals(20, image.getHeight());
            final Raster tile = image.getTile(0,0);
            assertEquals(   1.061f, tile.getSampleFloat(0,  0, 0));
            assertEquals(Float.NaN, tile.getSampleFloat(9,  0, 0));
            assertEquals(Float.NaN, tile.getSampleFloat(9, 19, 0));
            assertEquals(  -1.075f, tile.getSampleFloat(0, 19, 0));
            assertEquals(  27.039f, tile.getSampleFloat(4, 10, 0));
            /*
             * Verify that the coverage is cached.
             */
            assertSame(coverage, store.read(null, null));
        }
    }

    /**
     * Tests {@link AsciiGridStore#getFileSet()}. Since {@link AsciiGridStore} inherits
     * the default implementation, this is actually a test of {@code Resource.FileSet}.
     *
     * @throws DataStoreException if an error occurred while fetching the list of files.
     * @throws IOException if an error occurred while copying the file.
     */
    @Test
    public void testFileSet() throws DataStoreException, IOException {
        AsciiGridStore.FileSet fileset;
        try (AsciiGridStore store = new AsciiGridStore(null, testData(), true)) {
            fileset = store.getFileSet().orElseThrow();
        }
        final Path source = fileset.getPaths().iterator().next();
        assertEquals(FILENAME, source.getFileName().toString());
        /*
         * Tests the copy operation in a temporary directory.
         * This is using the default implementation of `FileSet.copy(…)`,
         */
        final Path dir = Files.createTempDirectory("sis-");
        Path target = null;
        try {
            target = fileset.copy(dir);
            assertEquals(dir, target.getParent());
            assertEquals(FILENAME, target.getFileName().toString());
            assertMultilinesEquals(Files.readString(source), Files.readString(target));
            /*
             * In order to test the delete operation, we need to open a new data store on the file
             * that we just copied. Otherwise, `fileset.delete()` would delete the original file.
             */
            try (AsciiGridStore store = new AsciiGridStore(null,  new StorageConnector(target), true)) {
                fileset = store.getFileSet().orElseThrow();
            }
            fileset.delete();
            assertTrue(Files.notExists(target));
            target = null;
        } finally {
            if (target != null) {
                Files.deleteIfExists(target);
            }
            Files.deleteIfExists(dir);
        }
    }
}
