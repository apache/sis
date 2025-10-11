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
package org.apache.sis.storage.wkt;

import java.io.StringReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertSingleton;
import org.apache.sis.test.TestCase;

// Specific to the main branch:
import static org.apache.sis.test.GeoapiAssert.assertAxisDirectionsEqual;


/**
 * Tests the WKT {@link Store}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class StoreTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public StoreTest() {
    }

    /**
     * The WKT to parse. This WKT uses US-ASCII characters only.
     */
    public static final String WKT =
            "GEOGCS[\"NTF (Paris)\",\n" +
            "  DATUM[\"Nouvelle Triangulation Francaise (Paris)\",\n" +
            "    SPHEROID[\"Clarke 1880 (IGN)\", 6378249.2, 293.4660212936269]],\n" +
            "    PRIMEM[\"Paris\", 2.5969213, AUTHORITY[\"EPSG\", \"8903\"]],\n" +
            "  UNIT[\"grad\", 0.015707963267948967],\n" +
            "  AXIS[\"Latitude\", NORTH],\n" +
            "  AXIS[\"Longitude\", EAST]]";

    /**
     * Validates the parsed CRS.
     */
    private static void validate(final GeographicCRS crs) {
        assertEquals("NTF (Paris)", crs.getName().getCode());
        assertEquals("Nouvelle Triangulation Francaise (Paris)", crs.getDatum().getName().getCode());
        assertAxisDirectionsEqual(crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);
    }

    /**
     * Tests {@link Store#getMetadata()} reading from a {@link java.io.Reader}.
     *
     * @throws DataStoreException if an error occurred while reading the metadata.
     */
    @Test
    public void testFromReader() throws DataStoreException {
        final Metadata metadata;
        try (Store store = new Store(null, new StorageConnector(new StringReader(WKT)))) {
            metadata = store.getMetadata();
            assertSame(metadata, store.getMetadata(), "Expected cached value.");
        }
        validate(assertInstanceOf(GeographicCRS.class, assertSingleton(metadata.getReferenceSystemInfo())));
    }

    /**
     * Tests {@link StoreProvider#probeContent(StorageConnector)} followed by {@link Store#getMetadata()}
     * reading from an {@link java.io.InputStream}. This method tests indirectly {@link StorageConnector}
     * capability to reset the {@code InputStream} to its original position after {@code probeContent(…)}.
     *
     * @throws DataStoreException if an error occurred while reading the WKT.
     */
    @Test
    public void testFromInputStream() throws DataStoreException {
        final Metadata metadata;
        final var p = new StoreProvider();
        final var c = new StorageConnector(new ByteArrayInputStream(StoreTest.WKT.getBytes(StandardCharsets.US_ASCII)));
        assertTrue(p.probeContent(c).isSupported());
        try (Store store = new Store(null, c)) {
            metadata = store.getMetadata();
            assertSame(metadata, store.getMetadata(), "Expected cached value.");
        }
        validate(assertInstanceOf(GeographicCRS.class, assertSingleton(metadata.getReferenceSystemInfo())));
    }
}
