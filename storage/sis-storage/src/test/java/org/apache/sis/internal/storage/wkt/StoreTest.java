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
package org.apache.sis.internal.storage.wkt;

import java.io.StringReader;
import java.io.ByteArrayInputStream;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.StandardCharsets;


/**
 * Tests the WKT {@link Store}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn(StoreProviderTest.class)
public final strictfp class StoreTest extends TestCase {
    /**
     * The WKT to parse. This WKT uses US-ASCII characters only.
     */
    public static final String WKT =
            "GEOGCS[\"NTF (Paris)\",\n" +
            "  DATUM[\"Nouvelle Triangulation Francaise (Paris)\",\n" +
            "    SPHEROID[\"Clarke 1880 (IGN)\", 6378249.2, 293.4660212936269]],\n" +
            "    PRIMEM[\"Paris\", 2.5969213, AUTHORITY[\"EPSG\", \"8903\"]],\n" +
            "  UNIT[\"grade\", 0.015707963267948967],\n" +
            "  AXIS[\"Latitude\", NORTH],\n" +
            "  AXIS[\"Longitude\", EAST]]";

    /**
     * Validates the parsed CRS.
     */
    private static void validate(final GeographicCRS crs) {
        assertEquals("NTF (Paris)", crs.getName().getCode());
        assertEquals("Nouvelle Triangulation Francaise (Paris)", crs.getDatum().getName().getCode());
        assertAxisDirectionsEqual("EllipsoidalCS", crs.getCoordinateSystem(), AxisDirection.NORTH, AxisDirection.EAST);
    }

    /**
     * Tests {@link Store#getMetadata()} reading from a {@link Reader}.
     *
     * @throws DataStoreException If an error occurred while reading the metadata.
     */
    @Test
    public void testFromReader() throws DataStoreException {
        final Metadata metadata;
        final Store store = new Store(new StorageConnector(new StringReader(WKT)));
        try {
            metadata = store.getMetadata();
            assertSame("Expected cached value.", metadata, store.getMetadata());
        } finally {
            store.close();
        }
        validate((GeographicCRS) TestUtilities.getSingleton(metadata.getReferenceSystemInfo()));
    }

    /**
     * Tests {@link StoreProvider#probeContent(StorageConnector)} followed by {@link Store#getMetadata()}
     * reading from an {@link InputStream}. This method tests indirectly {@link StorageConnector} capability
     * to reset the {@code InputStream} to its original position after {@code probeContent(…)}.
     *
     * @throws DataStoreException if en error occurred while reading the WKT.
     */
    @Test
    public void testFromInputStream() throws DataStoreException {
        final Metadata metadata;
        final StoreProvider p = new StoreProvider();
        final StorageConnector c = new StorageConnector(new ByteArrayInputStream(StoreTest.WKT.getBytes(StandardCharsets.US_ASCII)));
        assertTrue("isSupported", p.probeContent(c).isSupported());
        final Store store = new Store(c);
        try {
            metadata = store.getMetadata();
            assertSame("Expected cached value.", metadata, store.getMetadata());
        } finally {
            store.close();
        }
        validate((GeographicCRS) TestUtilities.getSingleton(metadata.getReferenceSystemInfo()));
    }
}
