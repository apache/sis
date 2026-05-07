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
import java.util.List;
import java.util.Optional;
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
 * Minimal test case for image tile matrices
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
     *
     * @throws DataStoreException if a data store error occurred.
     */
    @Test
    public void testGetResourceFromTile3D() throws DataStoreException {
        final var resource = new MockTiledResource();
        final var tileMatrixSets = resource.getTileMatrixSets();
        assertFalse(tileMatrixSets.isEmpty());
        final TileMatrixSet tms = tileMatrixSets.iterator().next();
        assertFalse(tms.getTileMatrices().isEmpty());
        final TileMatrix matrix = tms.getTileMatrices().values().iterator().next();

        final GridExtent tilingExtent = matrix.getTilingScheme().getExtent();
        assertEquals(3, tilingExtent.getDimension());
        final long[] indices = new long[] {
            tilingExtent.getLow(0),
            tilingExtent.getLow(1),
            tilingExtent.getLow(2)
        };

        final Optional<Tile> optTile = matrix.getTile(indices);
        assertTrue(optTile.isPresent());
        final var tile = optTile.get();
        assertEquals(TileStatus.EXISTS, tile.getStatus());
        final var tResource = tile.getResource();
        assertNotNull(tResource);
        if (!(tResource instanceof GridCoverageResource tileGridResource)) {
            throw new AssertionError("Tile resource is not a grid resource");
        }
        var tileImage = tileGridResource.read(null).render(null);
        assertEquals(TILE_WIDTH, tileImage.getWidth());
        assertEquals(TILE_HEIGHT, tileImage.getHeight());
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
                tiles[iterator.getTileIndexInResultArray()] = raster;
            } while (iterator.next());
            return tiles;
        }
    }
}