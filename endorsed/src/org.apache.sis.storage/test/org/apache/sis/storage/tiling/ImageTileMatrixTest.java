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
package org.apache.sis.storage.tiling;

import java.util.List;
import java.util.Arrays;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.util.iso.Names;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;
import static org.apache.sis.test.Assertions.assertSingleton;


/**
 * Verify behavior of {@link ImageTileMatrix} in a 3D space.
 * This test creates a {@link MockTiledResource coverage mockup} that contains two 2D slices.
 * Each slice contains {@link #NUM_COLS} tile columns and {@link #NUM_ROWS} tile rows.
 * Each tile is a single band image whose diagonal is filled with its tile indices.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class ImageTileMatrixTest extends TestCase {
    /**
     * Number of dimensions of the coverage in this test. The dimension of X and Y are 0 and 1 respectively.
     * These dimensions should use different values for increasing the chances to detect wrong dimension index.
     */
    private static final int DIMENSION = 3;

    /**
     * Size of all tiles in number of pixels.
     * Image should be non-square for increasing the chances to detect bug in use of width and height.
     */
    private static final int TILE_WIDTH  = 4,
                             TILE_HEIGHT = 3;

    /**
     * Number of tiles in each dimension to be tested.
     */
    private static final int NUM_COLS    = 3,
                             NUM_ROWS    = 5,
                             NUM_SLICES  = 2;

    /**
     * Value returned by {@link MockTiledResource#getGridGeometry()}.
     * Shared by each test method since this value is unmodifiable.
     */
    private final GridGeometry gridGeometry;

    /**
     * Value returned by {@link MockTiledResource#getSampleDimensions()}.
     * Shared by each test method since this value is unmodifiable.
     */
    private final List<SampleDimension> sampleDimensions;

    /**
     * Creates a new text case.
     */
    public ImageTileMatrixTest() {
        final var extent = new GridExtent(
                new DimensionNameType[] {
                    DimensionNameType.COLUMN,
                    DimensionNameType.ROW,
                    DimensionNameType.VERTICAL
                },
                null,   // Means all zeros.
                new long[] {
                    NUM_COLS * TILE_WIDTH,
                    NUM_ROWS * TILE_HEIGHT,
                    NUM_SLICES
                },
                false);

        gridGeometry = new GridGeometry(extent, PixelInCell.CELL_CORNER,
                MathTransforms.scale(0.5, 0.5, 100), HardCodedCRS.WGS84_3D);

        sampleDimensions = List.of(new SampleDimension.Builder().setName("data").build());
    }

    /**
     * Tests that a tile from a 3D dataset can return its associated resource.
     *
     * @throws DataStoreException if an error occurred while querying the tiles.
     */
    @Test
    public void testGetIndividualTiles3D() throws DataStoreException {
        testGetIndividualTiles3D(get3DMockupTileMatrix(), false);
    }

    /**
     * Implementation of {@link #testGetIndividualTiles3D()} and {@link #testParallelism()}.
     */
    private void testGetIndividualTiles3D(final TileMatrix matrix, final boolean parallel) throws DataStoreException {
        final var tilingExtent = matrix.getTilingScheme().getExtent();
        assertEquals(DIMENSION, tilingExtent.getDimension());
        tilingExtent.latticePointStream(parallel)
                .map(tileCoord -> loadTile(tileCoord, matrix))
                .forEach(ImageTileMatrixTest::checkTileContent);
    }

    /**
     * Checks another path to obtain and validate tiles.
     * This path gets a batch of (all in fact) tiles using {@link TileMatrix#getTiles(GridExtent, boolean)}.
     *
     * @throws DataStoreException if an error occurred while querying the tiles.
     */
    @Test
    public void testGetTileBatch3D() throws DataStoreException {
        testGetTileBatch3D(get3DMockupTileMatrix(), false);
    }

    /**
     * Implementation of {@link #testGetTileBatch3D()} and {@link #testParallelism()}.
     */
    private void testGetTileBatch3D(final TileMatrix matrix, final boolean parallel) throws DataStoreException {
        matrix.getTiles(null, parallel).forEach(ImageTileMatrixTest::checkTileContent);
    }

    /**
     * Checks tile content by querying a full rendering on each slice in the 3D coverage mockup.
     * The aim is to ensure that coverage rendering properly returns a tiled image,
     * and that each tile in the rendered image gives back its associated Tile content.
     * Said otherwise, it checks that there's no tile mismatch or mixup when using ImageIO accessor.
     *
     * @throws DataStoreException if an error occurred while querying the tiles.
     */
    @Test
    public void testGetTilesViaRender() throws DataStoreException {
        testGetTilesViaRender(false);
    }

    /**
     * Implementation of {@link #testGetTilesViaRender()} and {@link #testParallelism()}.
     */
    private void testGetTilesViaRender(final boolean parallel) throws DataStoreException {
        final var resource = new MockTiledResource();
        final var coverage = resource.read(null, null);
        final var overallExtent = coverage.getGridGeometry().getExtent();
        overallExtent.resize(1, 1).latticePointStream(parallel)
                .forEach(temporalSlice -> {
                    final long[] sliceHigh = temporalSlice.clone();
                    sliceHigh[0] = overallExtent.getHigh(0);
                    sliceHigh[1] = overallExtent.getHigh(1);
                    final var renderExtent = new GridExtent(null, temporalSlice, sliceHigh, true);
                    final RenderedImage image = coverage.render(renderExtent);
                    for (int col = 0; col < NUM_COLS; col++) {
                        for (int row = 0; row < NUM_ROWS; row++) {
                            final Raster tileRaster = image.getTile(col, row);
                            assertEquals(col * TILE_WIDTH,  tileRaster.getMinX());
                            assertEquals(row * TILE_HEIGHT, tileRaster.getMinY());
                            checkTileRaster(tileRaster, new long[] {col, row, temporalSlice[2]});
                        }
                    }
                });
    }

    /**
     * Same tests as {@link #testGetIndividualTiles3D()}, {@link #testGetTileBatch3D()}
     * and {@link #testGetTilesViaRender()} but executed in parallel.
     *
     * @throws DataStoreException if an error occurred while querying the tiles.
     */
    @Test
    public void testParallelism() throws DataStoreException {
        final var matrix = get3DMockupTileMatrix();
        testGetIndividualTiles3D(matrix, true);
        testGetTileBatch3D(matrix, true);
        testGetTilesViaRender(true);
    }

    /**
     * Returns the tile matrix from the three-dimensional resource mock.
     *
     * @return tile matrix from the three-dimensional resource mock.
     * @throws DataStoreException should never be thrown with the mock.
     */
    private TileMatrix get3DMockupTileMatrix() throws DataStoreException {
        final var resource = new MockTiledResource();
        final var tms = assertSingleton(resource.getTileMatrixSets());
        return assertSingleton(tms.getTileMatrices().values());
    }

    /**
     * Loads the tile at the specified tile indices. This method does not perform any verification.
     * Caller will typically invoke {@link #checkTileContent(Tile)} after this method.
     *
     * @param  tileIndices  indices of the tile to load.
     * @param  matrix       tile matrix from which to load the time.
     */
    private static Tile loadTile(final long[] tileIndices, final TileMatrix matrix) {
        try {
            final Tile tile = matrix.getTile(tileIndices).orElseThrow(AssertionError::new);
            assertArrayEquals(tileIndices, tile.getIndices(), "Tile indices differ from request.");
            return tile;
        } catch (DataStoreException e) {
            // Because this method is invoked in lambda function, avoid checked exceptions.
            throw new AssertionError("Cannot read tile (" + Arrays.toString(tileIndices) + ").", e);
        }
    }

    /**
     * Verifies the properties and the sample values stored in the given tile.
     * These sample values should contain the tile coordinates on a diagonal starting from the corner.
     *
     * @param  tile  the tile to verify.
     */
    private static void checkTileContent(final Tile tile) {
        final var tileIndices = tile.getIndices();
        assertEquals(DIMENSION, tileIndices.length);
        assertEquals(TileStatus.EXISTS, tile.getStatus());
        final RenderedImage tileImage;
        try {
            var resource = assertInstanceOf(GridCoverageResource.class, tile.getResource());
            tileImage = resource.read(null, null).render(null);
        } catch (DataStoreException e) {
            // Because this method is invoked in lambda function, avoid checked exceptions.
            throw new AssertionError("Cannot read tile (" + Arrays.toString(tileIndices) + ").", e);
        }
        assertEquals(TILE_WIDTH,  tileImage.getWidth());
        assertEquals(TILE_HEIGHT, tileImage.getHeight());
        assertEquals(1, tileImage.getNumXTiles());
        assertEquals(1, tileImage.getNumYTiles());
        checkTileRaster(tileImage.getTile(tileImage.getMinTileX(), tileImage.getMinTileY()), tileIndices);
    }

    /**
     * Verifies the sample values stored on the diagonal starting in the tile upper-left corner.
     * These sample values should contain the tile coordinates on a diagonal starting from the corner.
     *
     * @param  tile         the tile to verify.
     * @param  tileIndices  expected tile coordinates.
     */
    private static void checkTileRaster(final Raster tile, final long[] tileIndices) {
        assertEquals(DIMENSION, tileIndices.length);
        for (int i=0 ; i<DIMENSION; i++) {
            final var index = i;
            final int x = tile.getMinX() + i;
            final int y = tile.getMinY() + i;
            assertEquals(tileIndices[i], tile.getSample(x, y, 0),
                    () -> String.format("Tile sample at coordinate (%d, %d) should be the tile coordinate at dimension %d.", x, y, index));
        }
    }

    /**
     * A minimal {@link TiledGridCoverageResource} for testing.
     * The size of all rasters is {@value #TILE_WIDTH} × {@value #TILE_HEIGHT} pixels.
     * The tile coordinates are stored as sample values on the diagonal starting from
     * the upper-left corner.
     */
    private final class MockTiledResource extends TiledGridCoverageResource {
        /**
         * Creates a new resource mock.
         */
        MockTiledResource() {
            super(null);
        }

        /**
         * Returns the same grid geometry for all mocks.
         */
        @Override
        public GridGeometry getGridGeometry() {
            return gridGeometry;
        }

        /**
         * Returns the same sample dimensions for all mocks.
         */
        @Override
        @SuppressWarnings("ReturnOfCollectionOrArrayField")
        public List<SampleDimension> getSampleDimensions() {
            return sampleDimensions;
        }

        /**
         * Returns the size of all tiles for all images in this mock.
         * The size is {@value #TILE_WIDTH} × {@value #TILE_HEIGHT} × 1.
         */
        @Override
        protected int[] getTileSize() {
            return new int[] {TILE_WIDTH, TILE_HEIGHT, 1};
        }

        /**
         * Simulates a read operation, actually returning generated tiles.
         */
        @Override
        protected TiledGridCoverage read(Subset subset) {
            return new MockTiledCoverage(subset);
        }
    }

    /**
     * A minimal {@link TiledGridCoverage} that creates almost empty rasters on demand.
     * The size of all rasters is {@value #TILE_WIDTH} × {@value #TILE_HEIGHT} pixels.
     * The tile coordinates are stored as sample values on the diagonal starting from
     * the upper-left corner.
     */
    private static final class MockTiledCoverage extends TiledGridCoverage {
        /**
         * Creates a new mock pretending to be the result of a read operation.
         */
        MockTiledCoverage(TiledGridCoverageResource.Subset subset) {
            super(subset);
        }

        /**
         * Returns a dummy identifier. This is not used by the test.
         */
        @Override
        protected GenericName getIdentifier() {
            return Names.createLocalName(null, null, "test");
        }

        /**
         * Returns all tiles in the given area of interest.
         * For each raster, tile coordinates are written as sample
         * values on a diagonal starting in the upper-left corner.
         *
         * @see #checkTileRaster(Raster, long[])
         */
        @Override
        protected Raster[] readTiles(final TileIterator iterator) {
            final Raster[] tiles = new Raster[iterator.tileCountInQuery];
            do {
                final WritableRaster tile = iterator.createRaster();
                final int xmin = tile.getMinX();
                final int ymin = tile.getMinY();
                final long[] tileIndices = iterator.getTileCoordinatesInResource();
                assertEquals(DIMENSION, tileIndices.length);
                for (int i=0; i<DIMENSION; i++) {
                    tile.setSample(xmin + i, ymin + i, 0, StrictMath.toIntExact(tileIndices[i]));
                }
                tiles[iterator.getTileIndexInResultArray()] = tile;
            } while (iterator.next());
            return tiles;
        }
    }
}
