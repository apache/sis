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

import java.util.List;
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
     * @param  filename  name of the file to load.
     * @return the data store to test.
     * @throws DataStoreException if an error occurred while creating the data store.
     */
    private static GeoTiffStore createStore(final String filename) throws DataStoreException {
        return new GeoTiffStore(null, new StorageConnector(ReaderTest.class.getResource(filename)));
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
     * Tests the tile matrix set or an untiled image.
     *
     * @throws DataStoreException if an error occurred while creating the data store.
     */
    @Test
    public void testUntiledWithoutCompression() throws DataStoreException {
        readAndVerify(false, GeoTiffStoreTest.UNTILED_WITHOUT_COMPRESSION);
    }

    /**
     * Tests the tile matrix set or an untiled image.
     *
     * @throws DataStoreException if an error occurred while creating the data store.
     */
    @Test
    public void testUntiled() throws DataStoreException {
        readAndVerify(false, GeoTiffStoreTest.UNTILED);
    }

    /**
     * Tests the tile matrix set.
     *
     * @throws DataStoreException if an error occurred while creating the data store.
     */
    @Test
    public void testTileMatrixSetWithoutCompression() throws DataStoreException {
        readAndVerify(true, GeoTiffStoreTest.TILED_WITHOUT_COMPRESSION);
    }

    /**
     * Tests the tile matrix set.
     *
     * @throws DataStoreException if an error occurred while creating the data store.
     */
    @Test
    public void testTileMatrixSet() throws DataStoreException {
        readAndVerify(true, GeoTiffStoreTest.TILED);
    }

    /**
     * Implementation of {@link #testUntiled()} and {@link #testTiled()}.
     *
     * @param  tiled     whether the file to read is tiled.
     * @param  filename  the file to read.
     * @throws DataStoreException if an error occurred while creating the data store.
     */
    private void readAndVerify(final boolean tiled, final String filename) throws DataStoreException {
        try (GeoTiffStore ds = createStore(filename)) {
            final GridCoverageResource resource = assertInstanceOf(GridCoverageResource.class, assertSingleton(ds.components()));
            assertInstanceOf(ProjectedCRS.class, resource.getGridGeometry().getCoordinateReferenceSystem());
            if (!tiled) {
                assertTrue(assertInstanceOf(TiledResource.class, resource).getTileMatrixSets().isEmpty());
                return;
            }
            final String name = filename.substring(0, filename.lastIndexOf('.'));
            final TileMatrixSet pyramid = assertSingleton(assertInstanceOf(TiledResource.class, resource).getTileMatrixSets());
            assertSame(resource.getGridGeometry().getCoordinateReferenceSystem(), pyramid.getCoordinateReferenceSystem());
            assertEquals(name + ":1:TMS", pyramid.getIdentifier().toString());
            assertFalse(pyramid.getEnvelope().isEmpty());

            final SortedMap<GenericName, ? extends TileMatrix> matrices = pyramid.getTileMatrices();
            assertFalse(matrices.isEmpty());
            assertEquals(1, matrices.size());
            assertFalse(matrices.isEmpty());    // Test again because code path changed.
            assertTrue(matrices.subMap(matrices.firstKey(), matrices.lastKey()).isEmpty());

            final TileMatrix matrix = assertSingleton(matrices.values());
            assertEquals(name + ":1:TMS:L0", matrix.getIdentifier().toFullyQualifiedName().toString());
            assertEquals("TMS:L0", matrix.getIdentifier().toString());
            assertEquals(assertSingleton(matrices.keySet()), matrix.getIdentifier());
            assertArrayEquals(resource.getGridGeometry().getResolution(false), matrix.getResolution());
            assertSame(TileStatus.OUTSIDE_EXTENT, matrix.getTileStatus(3, 0));
            assertSame(TileStatus.OUTSIDE_EXTENT, matrix.getTileStatus(0, 2));
            assertSame(TileStatus.UNKNOWN,        matrix.getTileStatus(0, 0));  // Because the tile has not yet been loaded.
            assertSame(TileStatus.UNKNOWN,        matrix.getTileStatus(2, 1));

            assertMessageContains(assertThrows(NoSuchDataException.class, () -> matrix.getTile(3, 0)));
            final Tile tile = matrix.getTile(0, 0).orElseThrow();
            assertArrayEquals(new long[] {0, 0}, tile.getIndices());
            assertEquals(TileStatus.EXISTS, tile.getStatus());
            assertTrue(tile.getContentPath().isEmpty());

            final Raster raster = raster(tile);
            assertEquals(TileStatus.EXISTS, tile.getStatus());
            final List<Tile> tiles = matrix.getTiles(null, false).toList();
            assertEquals(3 * 2, tiles.size());
            assertArrayEquals(tile.getIndices(), tiles.get(0).getIndices());
            assertEquals(raster.getBounds(), raster(tiles.get(0)).getBounds());
        }
    }
}
