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
package org.apache.sis.internal.storage.ascii;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.identification.Identification;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.getSingleton;


/**
 * Tests {@link Store}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public final strictfp class StoreTest extends TestCase {
    /**
     * Opens an ASCII Grid store on the test data.
     */
    private static Store open() throws DataStoreException {
        return new Store(null, new StorageConnector(StoreTest.class.getResource("grid.asc")));
    }

    /**
     * Tests the metadata of the {@code "grid.asc"} file.
     *
     * @throws DataStoreException if an error occurred while reading the file.
     */
    @Test
    public void testMetadata() throws DataStoreException {
        try (Store store = open()) {
            assertEquals("grid", store.getIdentifier().get().toString());
            final Metadata metadata = store.getMetadata();
            /*
             * Format information is hard-coded in "SpatialMetadata" database. Complete string should
             * be "ESRI ArcInfo ASCII Grid format" but it depends on the presence of Derby dependency.
             */
            final Identification id = getSingleton(metadata.getIdentificationInfo());
            final String format = getSingleton(id.getResourceFormats()).getFormatSpecificationCitation().getTitle().toString();
            assertTrue(format, format.contains("ASCII Grid"));
            /*
             * This information should have been read from the PRJ file.
             */
            assertEquals("WGS 84 / World Mercator",
                    getSingleton(metadata.getReferenceSystemInfo()).getName().getCode());
            final GeographicBoundingBox bbox = (GeographicBoundingBox)
                    getSingleton(getSingleton(id.getExtents()).getGeographicElements());
            assertEquals(-84, bbox.getSouthBoundLatitude(), 1);
            assertEquals(+85, bbox.getNorthBoundLatitude(), 1);
        }
    }

    /**
     * Tests reading a few values from the {@code "grid.asc"} file.
     *
     * @throws DataStoreException if an error occurred while reading the file.
     */
    @Test
    public void testRead() throws DataStoreException {
        try (Store store = open()) {
            final GridCoverage coverage = store.read(null, null);
            final RenderedImage image = coverage.render(null);
            assertEquals(10, image.getWidth());
            assertEquals(20, image.getHeight());
            final Raster tile = image.getTile(0,0);
            assertEquals(   1.061f, tile.getSampleFloat(0,  0, 0), 0f);
            assertEquals(Float.NaN, tile.getSampleFloat(9,  0, 0), 0f);
            assertEquals(Float.NaN, tile.getSampleFloat(9, 19, 0), 0f);
            assertEquals(  -1.075f, tile.getSampleFloat(0, 19, 0), 0f);
            assertEquals(  27.039f, tile.getSampleFloat(4, 10, 0), 0f);
        }
    }
}
