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
package org.apache.sis.internal.storage.image;

import java.io.IOException;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.identification.Identification;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests {@link Store} and {@link StoreProvider}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public final strictfp class StoreTest extends TestCase {
    /**
     * Returns a storage connector with the URL to the test data.
     */
    private static StorageConnector testData() {
        return new StorageConnector(StoreTest.class.getResource("gradient.png"));
    }

    /**
     * Tests {@link StoreProvider#probeContent(StorageConnector)} method.
     *
     * @throws DataStoreException if en error occurred while reading the CSV file.
     */
    @Test
    public void testProbeContent() throws DataStoreException {
        final StoreProvider p = new StoreProvider();
        final ProbeResult r = p.probeContent(testData());
        assertTrue(r.isSupported());
        assertEquals("image/png", r.getMimeType());
    }

    /**
     * Tests the metadata of the {@code "gradient.png"} file.
     *
     * @throws DataStoreException if an error occurred while reading the file.
     * @throws IOException if an error occurred while creating the image reader instance.
     */
    @Test
    public void testMetadata() throws DataStoreException, IOException {
        try (Store store = new Store(null, testData(), true)) {
            assertEquals("gradient", store.getIdentifier().get().toString());
            final Metadata metadata = store.getMetadata();
            final Identification id = getSingleton(metadata.getIdentificationInfo());
            final String format = getSingleton(id.getResourceFormats()).getFormatSpecificationCitation().getTitle().toString();
            assertTrue(format, format.contains("PNG"));
            assertEquals("WGS 84", getSingleton(metadata.getReferenceSystemInfo()).getName().getCode());
            final GeographicBoundingBox bbox = (GeographicBoundingBox)
                    getSingleton(getSingleton(id.getExtents()).getGeographicElements());
            assertEquals( -90, bbox.getSouthBoundLatitude(), STRICT);
            assertEquals( +90, bbox.getNorthBoundLatitude(), STRICT);
            assertEquals(-180, bbox.getWestBoundLongitude(), STRICT);
            assertEquals(+180, bbox.getEastBoundLongitude(), STRICT);
            /*
             * Verify that the metadata is cached.
             */
            assertSame(metadata, store.getMetadata());
        }
    }
}
