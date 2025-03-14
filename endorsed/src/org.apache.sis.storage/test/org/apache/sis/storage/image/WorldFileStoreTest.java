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
package org.apache.sis.storage.image;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.ResourceAlreadyExistsException;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.util.ArraysExt;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.TestCase;
import static org.apache.sis.test.TestUtilities.getSingleton;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.identification.Identification;


/**
 * Tests {@link WorldFileStore} and {@link WorldFileStoreProvider}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class WorldFileStoreTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public WorldFileStoreTest() {
    }

    /**
     * Returns a storage connector with the URL to the test data.
     */
    private static StorageConnector testData() {
        return new StorageConnector(WorldFileStoreTest.class.getResource("gradient.png"));
    }

    /**
     * Tests {@link WorldFileStoreProvider#probeContent(StorageConnector)} method.
     *
     * @throws DataStoreException if en error occurred while reading the CSV file.
     */
    @Test
    public void testProbeContent() throws DataStoreException {
        final var provider = new WorldFileStoreProvider();
        final ProbeResult result = provider.probeContent(testData());
        assertTrue(result.isSupported());
        assertEquals("image/png", result.getMimeType());
    }

    /**
     * Tests the metadata of the {@code "gradient.png"} file.
     *
     * @throws DataStoreException if an error occurred during Image I/O or data store operations.
     */
    @Test
    public void testMetadata() throws DataStoreException {
        final var provider = new WorldFileStoreProvider();
        try (WorldFileStore store = provider.open(testData())) {
            /*
             * Opportunistic check of store type. Should be read-only,
             * and should have been simplified to the "single image" case.
             */
            assertFalse(store instanceof WritableStore);
            assertTrue(store instanceof SingleImageStore);
            /*
             * Verify format name and MIME type.
             */
            assertTrue(ArraysExt.contains(store.getImageFormat(false), "PNG"));
            assertTrue(ArraysExt.contains(store.getImageFormat(true), "image/png"));
            /*
             * Verify metadata content.
             */
            assertEquals("gradient", store.getIdentifier().get().toString());
            final Metadata metadata = store.getMetadata();
            final Identification id = getSingleton(metadata.getIdentificationInfo());
            final String format = getSingleton(id.getResourceFormats()).getFormatSpecificationCitation().getTitle().toString();
            assertTrue(format.contains("PNG"), format);
            assertEquals("WGS 84", getSingleton(metadata.getReferenceSystemInfo()).getName().getCode());
            final var bbox = (GeographicBoundingBox) getSingleton(getSingleton(id.getExtents()).getGeographicElements());
            assertEquals( -90, bbox.getSouthBoundLatitude());
            assertEquals( +90, bbox.getNorthBoundLatitude());
            assertEquals(-180, bbox.getWestBoundLongitude());
            assertEquals(+180, bbox.getEastBoundLongitude());
            /*
             * Verify that the metadata is cached.
             */
            assertSame(metadata, store.getMetadata());
        }
    }

    /**
     * Tests reading the coverage and writing it in a new file.
     * This test unconditionally open the data store as an aggregate,
     * i.e. it bypasses the simplification of PNG files as {@link SingleImageStore} view.
     *
     * @param  directory  a temporary directory where to write the image.
     * @throws DataStoreException if an error occurred during Image I/O or data store operations.
     * @throws IOException if an error occurred when creating, reading or deleting temporary files.
     */
    @Test
    public void testReadWrite(@TempDir final Path directory) throws DataStoreException, IOException {
        final var provider = new WorldFileStoreProvider(false);
        try (WorldFileStore source = provider.open(testData())) {
            assertFalse(source instanceof WritableStore);
            final GridCoverageResource resource = getSingleton(source.components());
            assertEquals("gradient:1", resource.getIdentifier().get().toString());
            /*
             * Above `resource` is the content of "gradient.png" file.
             * Write the resource in a new file using a different format.
             */
            final Path targetPath = directory.resolve("copy.jpg");
            final var connector = new StorageConnector(targetPath);
            connector.setOption(OptionKey.OPEN_OPTIONS, new StandardOpenOption[] {
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE
            });
            try (WritableStore target = (WritableStore) provider.open(connector)) {
                assertEquals(0, target.isMultiImages());
                final var copy = (WritableResource) target.add(resource);
                assertEquals(1, target.isMultiImages());
                assertNotSame(resource, copy);
                assertEquals (resource.getGridGeometry(),     copy.getGridGeometry());
                assertEquals (resource.getSampleDimensions(), copy.getSampleDimensions());
                /*
                 * Verify that attempt to write again without `REPLACE` mode fails.
                 */
                final GridCoverage coverage = resource.read(null, null);
                var e = assertThrows(ResourceAlreadyExistsException.class, () -> copy.write(coverage),
                                     "Should not have replaced existing resource.");
                assertMessageContains(e, "1");      // "1" is the image identifier.
                /*
                 * Try to write again in `REPLACE` mode.
                 */
                copy.write(coverage, WritableResource.CommonOption.REPLACE);
                assertEquals(1, target.isMultiImages());
            }
            /*
             * Verify that the 3 files have been written. The JGW file content is verified,
             * but the PRJ file content is not fully verified because it may vary.
             */
            assertTrue(Files.size(targetPath) > 0);
            assertTrue(Files.readAllLines(directory.resolve("copy.prj"))
                            .stream().anyMatch((line) -> line.contains("WGS 84")));
            assertArrayEquals(new String[] {
                "2.8125", "0.0", "0.0", "-2.8125", "-178.59375", "88.59375"
            }, Files.readAllLines(directory.resolve("copy.jgw")).toArray());
        }
    }
}
