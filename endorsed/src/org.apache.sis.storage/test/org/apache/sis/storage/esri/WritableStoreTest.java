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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.ResourceAlreadyExistsException;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.util.CharSequences;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;


/**
 * Tests {@link WritableStore}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class WritableStoreTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public WritableStoreTest() {
    }

    /**
     * Creates a test grid coverage filled with arbitrary data.
     *
     * @param  crs  the CRS to assign to the coverage. May be {@code null}.
     */
    private static GridCoverage createTestCoverage(final CoordinateReferenceSystem crs) {
        final BufferedImage image = new BufferedImage(4, 3, BufferedImage.TYPE_BYTE_GRAY);
        final byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        for (int i=0; i<data.length; i++) {
            data[i] = (byte) ((i+2) * 3);
        }
        return new GridCoverageBuilder().setValues(image)
                .setDomain(new Envelope2D(crs, 20, 10, 8, 9))
                .flipGridAxis(1).build();
    }

    /**
     * Returns the expected ASCII Grid lines for the coverage created by {@link #createTestCoverage()}.
     */
    private static String[] getExpectedLines() {
        return new String[] {
            "NCOLS           4",
            "NROWS           3",
            "XLLCORNER    20.0",
            "YLLCORNER    10.0",
            "XCELLSIZE     2.0",
            "YCELLSIZE     3.0",
            "NODATA_VALUE  NaN",
            "6 9 12 15",
            "18 21 24 27",
            "30 33 36 39",
        };
    }

    /**
     * Verifies that the content of the given file is equal to the expected values
     * for a coverage created by {@link #createTestCoverage()}.
     */
    private static void verifyContent(final Path file) throws IOException {
        assertArrayEquals(getExpectedLines(), Files.readAllLines(file).toArray());
        assertArrayEquals(new String[] {
            "GEODCRS[\"WGS 84\",",
            "  DATUM[\"World Geodetic System 1984\",",
            "    ELLIPSOID[\"WGS84\", 6378137.0, 298.257223563, LENGTHUNIT[\"metre\", 1]]],",
            "    PRIMEM[\"Greenwich\", 0.0, ANGLEUNIT[\"degree\", 0.017453292519943295]],",
            "  CS[ellipsoidal, 2],",
            "    AXIS[\"Longitude (L)\", east, ORDER[1]],",
            "    AXIS[\"Latitude (B)\", north, ORDER[2]],",
            "    ANGLEUNIT[\"degree\", 0.017453292519943295],",
            "  AREA[\"World\"],",
            "  BBOX[-90.00, -180.00, 90.00, 180.00]]",
        }, Files.readAllLines(toPRJ(file)).toArray());
    }

    /**
     * Given the path to an ASCII Grid file, returns the path to its PRJ auxiliary file.
     */
    private static Path toPRJ(final Path file) {
        String filename = file.getFileName().toString();
        filename = filename.substring(0, filename.lastIndexOf('.')) + ".prj";
        return file.resolveSibling(filename);
    }

    /**
     * Tests writing an ASCII Grid in a temporary file.
     *
     * @throws IOException if the temporary file cannot be created.
     * @throws DataStoreException if an error occurred while writing the file.
     */
    @Test
    public void testWriteInFile() throws IOException, DataStoreException {
        final GridCoverage coverage = createTestCoverage(HardCodedCRS.WGS84);
        final Path file = Files.createTempFile(null, ".asc");
        try {
            final Path filePRJ = toPRJ(file);
            final var sc = new StorageConnector(file);
            sc.setOption(OptionKey.OPEN_OPTIONS, new StandardOpenOption[] {StandardOpenOption.WRITE});
            boolean deleted;
            try (WritableStore store = new WritableStore(null, sc)) {
                store.write(coverage);
                verifyContent(file);
                /*
                 * Verify that a second attempt to write the coverage fails,
                 * because now a coverage already exists.
                 */
                try {
                    store.write(coverage);
                    fail("Should not be allowed to overwrite an existing coverage.");
                } catch (ResourceAlreadyExistsException e) {
                    assertNotNull(e.getMessage());
                }
                verifyContent(file);
                /*
                 * Write again, this time allowing the writer to replace the existing image.
                 */
                store.write(coverage, WritableStore.CommonOption.REPLACE);
                verifyContent(file);
                store.write(coverage, WritableStore.CommonOption.UPDATE);
                verifyContent(file);
            } finally {
                deleted = Files.deleteIfExists(filePRJ);
            }
            assertTrue(deleted, "Missing PRJ file.");
        } finally {
            Files.delete(file);
        }
    }

    /**
     * Tests writing an ASCII Grid in an in-memory buffer. The PRJ files cannot be created in this test,
     * which force us to use a null CRS for avoiding {@link java.net.UnknownServiceException} to be thrown.
     *
     * @throws DataStoreException if an error occurred while writing the file.
     */
    @Test
    public void testWriteInMemory() throws DataStoreException {
        final GridCoverage coverage = createTestCoverage(null);
        final var output = new ByteArrayOutputStream();
        try (WritableStore store = new WritableStore(null, new StorageConnector(output))) {
            store.write(coverage);
        }
        final String text = new String(output.toByteArray(), StandardCharsets.US_ASCII).trim();
        assertArrayEquals(getExpectedLines(), CharSequences.splitOnEOL(text));
    }
}
