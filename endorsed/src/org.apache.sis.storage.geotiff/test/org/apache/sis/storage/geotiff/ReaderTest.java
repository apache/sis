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
package org.apache.sis.storage.geotiff;

import java.util.SortedMap;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import org.opengis.util.GenericName;
import org.opengis.referencing.crs.ProjectedCRS;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.NoSuchDataException;
import org.apache.sis.storage.tiling.Tile;
import org.apache.sis.storage.tiling.TileMatrix;
import org.apache.sis.storage.tiling.TileMatrixSet;
import org.apache.sis.storage.tiling.TileStatus;
import org.apache.sis.storage.tiling.TiledResource;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertSingleton;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.TestCase;


/**
 * Tests a few read operations.
 * Despite the name of this test class, the {@link Reader} class is not tested directly
 * but indirectly via {@link GeoTiffStore}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @todo We should rewrite {@code "untiled.tiff"} as a tiled image.
 */
@SuppressWarnings("exports")
public class ReaderTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public ReaderTest() {
    }

    /**
     * Creates a new data store for the test file.
     *
     * @return the data store to test.
     * @throws DataStoreException if an error occurred while creating the data store.
     */
    private static GeoTiffStore createStore() throws DataStoreException {
        return new GeoTiffStore(null, new StorageConnector(ReaderTest.class.getResource(GeoTiffStoreTest.UNTILED)));
    }

    /**
     * Returns the single raster of the rendered image of the given tile.
     */
    private static Raster raster(final Tile tile) throws DataStoreException {
        RenderedImage image = assertInstanceOf(GridCoverageResource.class, tile.getResource()).read(null, null).render(null);
        assertEquals(1, image.getNumXTiles());
        assertEquals(1, image.getNumYTiles());
        return image.getTile(image.getMinTileX(), image.getMinTileY());
    }

    /**
     * Tests the tile matrix set.
     *
     * @todo Need to be updated if we rewrite {@code "untiled.tiff"} as a more interesting image.
     *
     * @throws DataStoreException if an error occurred while creating the data store.
     */
    @Test
    public void testTileMatrixSet() throws DataStoreException {
        try (GeoTiffStore ds = createStore()) {
            final GridCoverageResource resource = assertInstanceOf(GridCoverageResource.class, assertSingleton(ds.components()));
            assertInstanceOf(ProjectedCRS.class, resource.getGridGeometry().getCoordinateReferenceSystem());

            final TileMatrixSet pyramid = assertSingleton(assertInstanceOf(TiledResource.class, resource).getTileMatrixSets());
            assertSame(resource.getGridGeometry().getCoordinateReferenceSystem(), pyramid.getCoordinateReferenceSystem());
            assertEquals("untiled:1:TMS", pyramid.getIdentifier().toString());
            assertFalse(pyramid.getEnvelope().isEmpty());

            final SortedMap<GenericName, ? extends TileMatrix> matrices = pyramid.getTileMatrices();
            assertFalse(matrices.isEmpty());
            assertEquals(1, matrices.size());
            assertFalse(matrices.isEmpty());    // Test again because code path changed.
            assertTrue(matrices.subMap(matrices.firstKey(), matrices.lastKey()).isEmpty());

            final TileMatrix matrix = assertSingleton(matrices.values());
            assertEquals("untiled:1:TMS:L0", matrix.getIdentifier().toFullyQualifiedName().toString());
            assertEquals("TMS:L0", matrix.getIdentifier().toString());
            assertEquals(assertSingleton(matrices.keySet()), matrix.getIdentifier());
            assertArrayEquals(resource.getGridGeometry().getResolution(false), matrix.getResolution());
            assertSame(TileStatus.OUTSIDE_EXTENT, matrix.getTileStatus(1, 0));
            assertSame(TileStatus.OUTSIDE_EXTENT, matrix.getTileStatus(0, 1));
            assertSame(TileStatus.UNKNOWN,        matrix.getTileStatus(0, 0));  // Because the tile has not yet been loaded.

            assertMessageContains(assertThrows(NoSuchDataException.class, () -> matrix.getTile(1, 0)));
            final Tile tile = matrix.getTile(0, 0).orElseThrow();
            assertArrayEquals(new long[] {0, 0}, tile.getIndices());
            assertEquals(TileStatus.EXISTS, tile.getStatus());
            assertTrue(tile.getContentPath().isEmpty());

            final Raster raster = raster(tile);
            assertEquals(TileStatus.EXISTS, tile.getStatus());
            assertArrayEquals(tile.getIndices(), assertSingleton(matrix.getTiles(null, false).toList()).getIndices());
            assertSame(raster, raster(assertSingleton(matrix.getTiles(null, false).toList())));
        }
    }
}
