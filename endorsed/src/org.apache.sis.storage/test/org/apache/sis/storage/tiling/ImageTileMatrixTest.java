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

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.List;
import org.apache.sis.storage.GridCoverageResource;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.util.GenericName;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.iso.Names;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;


/**
 * Verify behavior of {@link ImageTileMatrix} in a 3D space.
 * </br>
 * This test creates a {@link MockTiledResource coverage mockup} that contains two 2D slices.
 * Each slice contains {@link #NUM_COLS} tile columns and {@link #NUM_ROWS} tile rows.
 * Each tile is a single band image whose diagonal is filled with its tile indices.
 *
 * @author  Alexis Manin (Geomatys)
 */
@SuppressWarnings("exports")
public final class ImageTileMatrixTest extends TestCase {

    private static final BufferedImage MODEL = new BufferedImage(4, 4, BufferedImage.TYPE_BYTE_GRAY);
    private static final int TILE_WIDTH  = 4;
    private static final int TILE_HEIGHT = 4;
    private static final int NUM_COLS    = 3;
    private static final int NUM_ROWS    = 3;
    private static final int NUM_SLICES  = 2;

    /**
     * Test that a tile from a 3D dataset can return its associated resource.
     * This is an anti-regression test because of a bug encountered while fetching tile resource.
     */
    @Test
    public void testGetIndividualTiles3D() throws DataStoreException {
        final var matrix = get3DMockupTileMatrix();
        final var tilingExtent = matrix.getTilingScheme().getExtent();
        assertEquals(3, tilingExtent.getDimension());
        tilingExtent.latticePointStream(true)
                .map(tileCoord -> loadTile(tileCoord, matrix))
                .forEach(ImageTileMatrixTest::checkTileContent);
    }

    /**
     * Check another path to obtain and validate tiles: get a batch of (all in fact) tiles using {@link TileMatrix#getTiles(GridExtent, boolean)}
     */
    @Test
    public void testGetTileBatch3D() throws DataStoreException {
        final var matrix = get3DMockupTileMatrix();
        matrix.getTiles(null, true)
              .forEach(ImageTileMatrixTest::checkTileContent);
    }

    /**
     * Check tile content by querying a full rendering on each slice in the 3D coverage mockup.
     * The aim is to ensure that coverage rendering properly returns a tiled image,
     * and that each tile in the rendered image gives back its associated Tile content.
     * Said otherwise, it checks that there's no tile mismatch or mixup when using ImageIO accessor.
     */
    @Test
    public void testGetTilesViaRender() throws DataStoreException {
        final var resource = new MockTiledResource();
        final var coverage = resource.read((GridGeometry) null);
        final var overallExtent = coverage.getGridGeometry().getExtent();
        overallExtent.resize(1, 1).latticePointStream(true)
                .forEach(temporalSlice -> {
                    var sliceHigh = temporalSlice.clone();
                    sliceHigh[0] = overallExtent.getHigh(0);
                    sliceHigh[1] = overallExtent.getHigh(1);
                    final var renderExtent = new GridExtent(null, temporalSlice, sliceHigh, true);

                    final var image = coverage.render(renderExtent);
                    for (int col = 0; col < NUM_COLS; col++) {
                        for (int row = 0; row < NUM_ROWS; row++) {
                            final var tileIndices = temporalSlice.clone();
                            tileIndices[0] = col;
                            tileIndices[1] = row;
                            final var tileRaster = image.getTile(col, row);
                            checkTileOrigin(tileRaster, tileIndices);
                            checkTileRaster(tileRaster, tileIndices);
                        }
                    }
                });
    }

    private void checkTileOrigin(Raster tileRaster, long[] tileIndices) {
        final var expectedTileOrigin = new int[] {
                Math.toIntExact(Math.multiplyExact(tileIndices[0], TILE_WIDTH)),
                Math.toIntExact(Math.multiplyExact(tileIndices[1], TILE_HEIGHT))
        };
        final var actualTileOrigin = new int[] { tileRaster.getMinX(), tileRaster.getMinY() };
        assertArrayEquals(expectedTileOrigin, actualTileOrigin,
                () -> String.format(
                        "Tile (%s): raster origin does not match rendering location. Expected: (%s) but was (%s)",
                        Arrays.toString(tileIndices), Arrays.toString(expectedTileOrigin), Arrays.toString(actualTileOrigin)
                )
        );
    }

    private TileMatrix get3DMockupTileMatrix() throws DataStoreException {
        final var resource = new MockTiledResource();
        final var tileMatrixSets = resource.getTileMatrixSets();
        assertFalse(tileMatrixSets.isEmpty());
        final var tms = tileMatrixSets.iterator().next();
        final var tileMatrixIterator = tms.getTileMatrices().values().iterator();
        assertTrue(tileMatrixIterator.hasNext());
        final var matrix = tileMatrixIterator.next();
        assertFalse(tileMatrixIterator.hasNext());
        return matrix;
    }

    /**
     * Load a specific tile from the given tile matrix.
     * This method expects that tile content will respect this test
     */
    private Tile loadTile(long[] requestedTileIndices, TileMatrix matrix) {
        try {
            final var optTile = matrix.getTile(requestedTileIndices);
            assertTrue(optTile.isPresent());
            final var tile = optTile.get();
            final var tIndices = tile.getIndices();
            assertArrayEquals(requestedTileIndices, tIndices, "Tile indices differ from request");
            return tile;
        } catch (DataStoreException e) {
            throw new AssertionError("Extraction of tile ("+ Arrays.toString(requestedTileIndices)+") failed", e);
        }
    }

    private static void checkTileContent(Tile tile) {
        final var tileIndices = tile.getIndices();
        try {
            assertEquals(TileStatus.EXISTS, tile.getStatus());
            final var tResource = tile.getResource();
            assertNotNull(tResource);
            if (!(tResource instanceof GridCoverageResource tileGridResource)) {
                throw new AssertionError("Tile resource is not a grid resource");
            }
            final var tileImage = tileGridResource.read(null).render(null);
            assertEquals(TILE_WIDTH, tileImage.getWidth());
            assertEquals(TILE_HEIGHT, tileImage.getHeight());

            assertEquals(1, tileImage.getNumXTiles() * tileImage.getNumYTiles(),
                    "Tile image should contain only a single raster tile.");
            final var tileRaster = tileImage.getTile(tileImage.getMinTileX(), tileImage.getMinTileY());
            checkTileRaster(tileRaster, tileIndices);
        } catch (DataStoreException e) {
            fail("Validation of tile ("+ Arrays.toString(tileIndices)+") failed", e);
        }
    }

    private static void checkTileRaster(Raster tileRaster, long[] tileIndices) {
        for (int i=0 ; i < tileIndices.length ; i++) {
            final var index = i;
            assertEquals(tileIndices[i], tileRaster.getSample(tileRaster.getMinX() + i, tileRaster.getMinY() + i, 0),
                    () -> String.format("Tile sample at coordinate (%1$d, %1$d) should be the tile coordinate at dimension %1$d", index));
        }
    }

    /**
     * A minimal {@link TiledGridCoverageResource} for testing.
     */
    private static final class MockTiledResource extends TiledGridCoverageResource {

        private final GridGeometry gridGeometry;
        private final List<SampleDimension> sampleDimensions;

        MockTiledResource() {
            super(null);
            final var extent = new GridExtent(
                    new DimensionNameType[] {
                        DimensionNameType.COLUMN,
                        DimensionNameType.ROW,
                        DimensionNameType.VERTICAL
                    },
                    new long[3],
                    new long[] {
                        NUM_COLS * TILE_WIDTH  - 1,
                        NUM_ROWS * TILE_HEIGHT - 1,
                        NUM_SLICES - 1
                    },
                    true);

            final int dimension = 3;
            final MatrixSIS gridToCRS = Matrices.createIdentity(dimension + 1);
            gridToCRS.setNumber(0, 0, 0.5);
            gridToCRS.setNumber(1, 1, 0.5);
            gridToCRS.setNumber(2, 2, 100);
            gridGeometry = new GridGeometry(extent, PixelInCell.CELL_CORNER,
                    MathTransforms.linear(gridToCRS), HardCodedCRS.WGS84_3D);

            sampleDimensions = List.of(new SampleDimension.Builder().setName("data").build());
        }

        @Override
        public GridGeometry getGridGeometry() {
            return gridGeometry;
        }

        @Override
        public List<SampleDimension> getSampleDimensions() {
            return sampleDimensions;
        }

        @Override
        protected int[] getTileSize() {
            return new int[] {TILE_WIDTH, TILE_HEIGHT, 1};
        }

        @Override
        protected ColorModel getColorModel(int[] bands) {
            return MODEL.getColorModel();
        }

        @Override
        protected SampleModel getSampleModel(int[] bands) {
            return MODEL.getSampleModel();
        }

        @Override
        protected TiledGridCoverage read(Subset subset) {
            return new MockTiledCoverage(subset);
        }
    }

    /**
     * A minimal {@link TiledGridCoverage} that creates empty rasters on demand.
     */
    private static final class MockTiledCoverage extends TiledGridCoverage {

        MockTiledCoverage(TiledGridCoverageResource.Subset subset) {
            super(subset);
        }

        @Override
        protected GenericName getIdentifier() {
            return Names.createLocalName(null, null, "test");
        }

        @Override
        protected Raster[] readTiles(TileIterator iterator) {
            final Raster[] tiles = new Raster[iterator.tileCountInQuery];
            do {
                final WritableRaster raster = iterator.createRaster();
                final var tileCoords = iterator.getTileCoordinatesInResource();
                for (int i = 0; i < tileCoords.length; i++) {
                    raster.setSample(raster.getMinX() + i, raster.getMinY() + i, 0, tileCoords[i]);
                }
                tiles[iterator.getTileIndexInResultArray()] = raster;
            } while (iterator.next());
            return tiles;
        }
    }
}