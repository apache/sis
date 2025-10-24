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
package org.apache.sis.storage.aggregate;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Random;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import org.apache.sis.util.iso.Names;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverage2D;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.image.internal.shared.ImageUtilities;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.RasterLoadingStrategy;
import org.apache.sis.referencing.operation.transform.MathTransforms;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;


/**
 * Tests {@link CoverageAggregator} when the result is a mosaic.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class CoverageMosaicTest extends TestCase {
    /**
     * Minimum coordinates of the grid extent.
     */
    private static final int MIN_X = 10, MIN_Y = 20;

    /**
     * Size of tiles used in this test. Width and height should be different
     * for increasing the chances to detect confusion between the dimensions.
     */
    private static final int TILE_WIDTH = 2, TILE_HEIGHT = 3;

    /**
     * Number of tiles in the mosaic represented by {@link #resource}.
     * We use the same number on both axes.
     */
    private static final int NUM_TILES = 2;

    /**
     * The resource to test, as a resource made of a mosaic of 4 grid resources.
     * The number of tiles is {@value #NUM_TILES} on both axes.
     */
    private final ConcatenatedGridResource aggregation;

    /**
     * Data buffer of tiles created by the test, in creation order.
     * Used for verifying if the existing tiles have been reused.
     * We cannot test {@link Raster} instances directly because they may have been translated.
     */
    private final List<DataBufferByte> tiles;

    /**
     * The sample dimensions of all coverages in the tests.
     */
    private final List<SampleDimension> ranges;

    /**
     * Random number generator.
     */
    private final Random random;

    /**
     * Creates a new test case.
     *
     * @throws DataStoreException should never happen because this test uses in-memory resources.
     */
    public CoverageMosaicTest() throws DataStoreException {
        random = TestUtilities.createRandomNumberGenerator();
        tiles  = new ArrayList<>();
        ranges = List.of(new SampleDimension(Names.createLocalName(null, null, "Slice"), null, List.of()));
        final var aggregator = new CoverageAggregator();
        final var bounds = new Rectangle(MIN_X, MIN_Y, TILE_WIDTH, TILE_HEIGHT);
        /* Clockwise order. */     aggregator.add(resource(bounds, (byte) 10));
        bounds.x += bounds.width;  aggregator.add(resource(bounds, (byte) 11));
        bounds.y += bounds.height; aggregator.add(resource(bounds, (byte) 12));
        bounds.x -= bounds.width;  aggregator.add(resource(bounds, (byte) 13));
        aggregation = assertInstanceOf(ConcatenatedGridResource.class,
                aggregator.build(Names.createLocalName(null, null, "Aggregation")));
        assertEquals(NUM_TILES*NUM_TILES, aggregation.locator.slices.length);
    }

    /**
     * Creates grid coverage resource filled with the given value.
     *
     * @param  bounds  pixel coordinates.
     * @param  value   value of all pixels.
     */
    private GridCoverageResource resource(final Rectangle bounds, final byte value) {
        final var image  = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_BYTE_GRAY);
        final var buffer = assertInstanceOf(DataBufferByte.class, image.getRaster().getDataBuffer());
        Arrays.fill(buffer.getData(), value);
        assertTrue(tiles.add(buffer));

        final var extent    = new GridExtent(bounds);
        final var gridToCRS = MathTransforms.identity(extent.getDimension());
        final var domain    = new GridGeometry(extent, PixelInCell.CELL_CORNER, gridToCRS, null);
        return new OpaqueGridResource(new GridCoverage2D(domain, ranges, image));
    }

    /**
     * Tests the aggregation of a mosaic with immediate "loading" of pixel values.
     * This method tests the reading of different sub-regions, but always without subsampling.
     *
     * @throws DataStoreException if an error occurred.
     */
    @Test
    public void testReadTilesFully() throws DataStoreException {
        assertTrue(aggregation.setLoadingStrategy(RasterLoadingStrategy.AT_READ_TIME));
        verifyFullTiles();
    }

    /**
     * Tests the aggregation of a mosaic with loading deferred at image rendering time.
     * This is the same test as {@link #testReadTilesFully()} with a slightly different code path.
     *
     * @throws DataStoreException if an error occurred.
     */
    @Test
    public void testDeferred() throws DataStoreException {
        assertTrue(aggregation.setLoadingStrategy(RasterLoadingStrategy.AT_RENDER_TIME));
        verifyFullTiles();
    }

    /**
     * Implementation of {@link #testReadTilesFully()} and {@link #testDeferred()}.
     *
     * @throws DataStoreException if an error occurred.
     */
    private void verifyFullTiles() throws DataStoreException {
        GridCoverage coverage = aggregation.read(null, null);
        assertEquals(ranges, coverage.getSampleDimensions());
        GridExtent extent = coverage.getGridGeometry().getExtent();
        assertEquals(MIN_X, extent.getLow(0));
        assertEquals(MIN_Y, extent.getLow(1));
        assertEquals(MIN_X + NUM_TILES*TILE_WIDTH  - 1, extent.getHigh(0));
        assertEquals(MIN_Y + NUM_TILES*TILE_HEIGHT - 1, extent.getHigh(1));
        /*
         * Test rendering the full image.
         */
        RenderedImage image = coverage.render(null);
        assertEquals(0,                     image.getMinX());
        assertEquals(0,                     image.getMinY());
        assertEquals(NUM_TILES,             image.getNumXTiles());
        assertEquals(NUM_TILES,             image.getNumYTiles());
        assertEquals(TILE_WIDTH,            image.getTileWidth());
        assertEquals(TILE_HEIGHT,           image.getTileHeight());
        assertEquals(NUM_TILES*TILE_WIDTH,  image.getWidth());
        assertEquals(NUM_TILES*TILE_HEIGHT, image.getHeight());
        assertReuseExistingTiles(image);
        assertAllPixelsEqual(image,
                (byte) 10, (byte) 10, (byte) 11, (byte) 11,
                (byte) 10, (byte) 10, (byte) 11, (byte) 11,
                (byte) 10, (byte) 10, (byte) 11, (byte) 11,
                (byte) 13, (byte) 13, (byte) 12, (byte) 12,
                (byte) 13, (byte) 13, (byte) 12, (byte) 12,
                (byte) 13, (byte) 13, (byte) 12, (byte) 12);
        /*
         * Test rendering only two tiles. We take the last column of the tile matrix.
         * The selected tiles are tiles #1 and #2 (because of clockwise ordering).
         * We request a slightly larger region for testing clipping.
         */
        final int offset = random.nextInt(4);   // Offset outside valid area.
        image = coverage.render(new GridExtent(new Rectangle(
                MIN_X + TILE_WIDTH,
                MIN_Y - offset,
                TILE_WIDTH,
                TILE_HEIGHT*NUM_TILES + offset + 2)));
        assertEquals(0,                     image.getMinX());
        assertEquals(offset,                image.getMinY());
        assertEquals(1,                     image.getNumXTiles());
        assertEquals(NUM_TILES,             image.getNumYTiles());
        assertEquals(TILE_WIDTH,            image.getTileWidth());
        assertEquals(TILE_HEIGHT,           image.getTileHeight());
        assertEquals(TILE_WIDTH,            image.getWidth());
        assertEquals(TILE_HEIGHT*NUM_TILES, image.getHeight());
        assertReuseExistingTiles(image);
        assertAllPixelsEqual(image,
                (byte) 11, (byte) 11,
                (byte) 11, (byte) 11,
                (byte) 11, (byte) 11,
                (byte) 12, (byte) 12,
                (byte) 12, (byte) 12,
                (byte) 12, (byte) 12);
        /*
         * Test reading only two tiles. We take the last row of the tile matrix.
         * The selected tiles are tiles #2 and #3 (of a total of 4 tiles).
         */
        coverage = aggregation.read(new GridGeometry(new Envelope2D(null, MIN_X, MIN_Y+TILE_HEIGHT, NUM_TILES*TILE_WIDTH, TILE_HEIGHT)));
        assertEquals(ranges, coverage.getSampleDimensions());
        extent = coverage.getGridGeometry().getExtent();
        assertEquals(MIN_X,                             extent.getLow (0));
        assertEquals(MIN_Y + TILE_HEIGHT,               extent.getLow (1));
        assertEquals(MIN_X + NUM_TILES*TILE_WIDTH  - 1, extent.getHigh(0));
        assertEquals(MIN_Y + NUM_TILES*TILE_HEIGHT - 1, extent.getHigh(1));
        image = coverage.render(null);
        assertEquals(0,                    image.getMinX());
        assertEquals(0,                    image.getMinY());
        assertEquals(NUM_TILES,            image.getNumXTiles());
        assertEquals(1,                    image.getNumYTiles());
        assertEquals(TILE_WIDTH,           image.getTileWidth());
        assertEquals(TILE_HEIGHT,          image.getTileHeight());
        assertEquals(TILE_WIDTH*NUM_TILES, image.getWidth());
        assertEquals(TILE_HEIGHT,          image.getHeight());
        assertReuseExistingTiles(image);
        assertAllPixelsEqual(image,
                (byte) 13, (byte) 13, (byte) 12, (byte) 12,
                (byte) 13, (byte) 13, (byte) 12, (byte) 12,
                (byte) 13, (byte) 13, (byte) 12, (byte) 12);
        /*
         * Test rendering a subset of the read subset. We request a slightly larger region in one
         * axis for verifying that the result has been clipped. The result should be a single tile.
         */
        image = coverage.render(new GridExtent(new Rectangle(
                MIN_X + TILE_WIDTH,
                MIN_Y + TILE_HEIGHT,
                TILE_WIDTH + offset,
                TILE_HEIGHT)));
        assertEquals(0,           image.getMinX());
        assertEquals(0,           image.getMinY());
        assertEquals(1,           image.getNumXTiles());
        assertEquals(1,           image.getNumYTiles());
        assertEquals(TILE_WIDTH,  image.getTileWidth());
        assertEquals(TILE_HEIGHT, image.getTileHeight());
        assertEquals(TILE_WIDTH,  image.getWidth());
        assertEquals(TILE_HEIGHT, image.getHeight());
        assertReuseExistingTiles(image);
        assertAllPixelsEqual(image,
                (byte) 12, (byte) 12,
                (byte) 12, (byte) 12,
                (byte) 12, (byte) 12);
    }

    /**
     * Asserts that the given image reuses data buffer of existing tiles.
     * This is indirectly a verification that the "overlay" operation has
     * been able to perform its optimization.
     */
    @SuppressWarnings("element-type-mismatch")
    private void assertReuseExistingTiles(final RenderedImage image) {
        for (int dy = image.getNumYTiles(); --dy >= 0;) {
            final int ty = dy + image.getMinTileY();
            for (int dx = image.getNumXTiles(); --dx >= 0;) {
                final int tx = dx + image.getMinTileX();
                final Raster tile = image.getTile(tx, ty);
                assertEquals(image.getTileWidth(),  tile.getWidth());
                assertEquals(image.getTileHeight(), tile.getHeight());
                assertEquals(ImageUtilities.tileToPixelX(image, tx), tile.getMinX());
                assertEquals(ImageUtilities.tileToPixelY(image, ty), tile.getMinY());
                assertTrue(tiles.contains(tile.getDataBuffer()),
                        () -> "Data buffer not reused for tile (" + tx + ", " + ty + ").");
            }
        }
    }

    /**
     * Asserts that all pixels in the rendered image have the expected value.
     *
     * @param  coverage  the coverage for which to verify pixel values.
     * @param  expected  the expected pixel values.
     */
    private static void assertAllPixelsEqual(final RenderedImage image, final byte... expected) {
        final var buffer = assertInstanceOf(DataBufferByte.class, image.getData().getDataBuffer());
        assertArrayEquals(expected, buffer.getData());
    }

    /**
     * Test reading and rendering a region that does not cover an integer number of tiles.
     * Only the "read time" loading strategy is tested, as the "render time" one is disabled
     * when a sub-region is specified.
     *
     * @throws DataStoreException if an error occurred.
     */
    @Test
    public void testReadTilesPartially() throws DataStoreException {
        assertTrue(aggregation.setLoadingStrategy(RasterLoadingStrategy.AT_READ_TIME));
        var aoi = new Rectangle(MIN_X + 1,
                                MIN_Y + 2,
                                NUM_TILES*TILE_WIDTH  - 2,
                                NUM_TILES*TILE_HEIGHT - 3);

        GridCoverage coverage = aggregation.read(new GridGeometry(new GridExtent(aoi), null, null));
        assertEquals(ranges, coverage.getSampleDimensions());
        GridExtent extent = coverage.getGridGeometry().getExtent();
        assertEquals(MIN_X + 1,                         extent.getLow (0));
        assertEquals(MIN_Y + 2,                         extent.getLow (1));
        assertEquals(MIN_X + NUM_TILES*TILE_WIDTH  - 2, extent.getHigh(0));
        assertEquals(MIN_Y + NUM_TILES*TILE_HEIGHT - 2, extent.getHigh(1));
        RenderedImage image = coverage.render(null);
        assertEquals(0, image.getMinX());
        assertEquals(0, image.getMinY());
        assertEquals(1, image.getNumXTiles());
        assertEquals(1, image.getNumYTiles());
        assertEquals(2, image.getWidth());
        assertEquals(3, image.getHeight());
        assertAllPixelsEqual(image,
                (byte) 10, (byte) 11,
                (byte) 13, (byte) 12,
                (byte) 13, (byte) 12);
        /*
         * Test rendering a subset of the read subset.
         * Despite requesting less data, the implementation is free to return more.
         */
        final int offset = random.nextInt(4);
        aoi.x -= offset;
        aoi.width += offset + 1;
        aoi.height--;
        image = coverage.render(new GridExtent(aoi));
        assertEquals(offset, image.getMinX());
        assertEquals(0,      image.getMinY());
        assertEquals(1,      image.getNumXTiles());
        assertEquals(1,      image.getNumYTiles());
        assertEquals(2,      image.getWidth());
        assertEquals(3,      image.getHeight());
        assertAllPixelsEqual(image,
                (byte) 10, (byte) 11,
                (byte) 13, (byte) 12,
                (byte) 13, (byte) 12);
    }

    /**
     * Tests reading all tiles, but with subsampling applied.
     * Only the "read time" loading strategy is tested, as the "render time" one
     * is disabled when a sub-subsampling is specified.
     *
     * @throws DataStoreException if an error occurred.
     */
    @Test
    public void testReadWithSubsampling() throws DataStoreException {
        assertTrue(aggregation.setLoadingStrategy(RasterLoadingStrategy.AT_READ_TIME));
        GridGeometry aoi = aggregation.getGridGeometry().derive().subgrid(null, new long[] {2, 3}).build();
        GridCoverage coverage = aggregation.read(aoi);
        assertEquals(ranges, coverage.getSampleDimensions());
        GridExtent extent = coverage.getGridGeometry().getExtent();
        /*
         * At the time of writing this test, the `MemoryGridCoverageResource.read(…)` implementation
         * does not apply subsampling. Therefore, the coverage is as `testReadTilesFully()`.
         * If a test failure happens below in a future version, this is not necessarily a bug.
         * Maybe `MemoryGridCoverageResource` has been improved and this test should be adjusted.
         */
        assertEquals(NUM_TILES*TILE_WIDTH,  extent.getSize(0));
        assertEquals(NUM_TILES*TILE_HEIGHT, extent.getSize(1));
        assertTrue(coverage.getGridGeometry().getGridToCRS(PixelInCell.CELL_CORNER).isIdentity());
    }
}
