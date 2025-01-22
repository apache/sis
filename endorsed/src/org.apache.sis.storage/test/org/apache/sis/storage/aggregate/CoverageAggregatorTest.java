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
import java.awt.image.Raster;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.RenderedImage;
import org.apache.sis.util.iso.Names;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverage2D;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.referencing.operation.transform.MathTransforms;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the simplest cases of {@link CoverageAggregator}.
 * For more cases, see {@link CoverageMosaicTest}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class CoverageAggregatorTest extends TestCase {
    /**
     * Size of the data cube used in this test. Width and height should be different
     * for increasing the chances to detect confusion between the dimensions.
     */
    private static final int WIDTH = 2, HEIGHT = 3, DEPTH = 4;

    /**
     * Minimum coordinates of the grid extent.
     */
    private static final int MIN_X = 10, MIN_Y = 20, MIN_Z = 30;

    /**
     * Pixel value of the first slice.
     */
    private static final int PIXEL_BASE_VALUE = 100;

    /**
     * The aggregator to test.
     */
    private final CoverageAggregator aggregator;

    /**
     * The sample dimensions for the tests, created when first needed and cached.
     */
    private List<SampleDimension> ranges;

    /**
     * Creates a new test case.
     */
    public CoverageAggregatorTest() {
        aggregator = new CoverageAggregator();
    }

    /**
     * Tests an empty aggregator.
     *
     * @throws DataStoreException if an error occurred.
     */
    @Test
    public void testEmpty() throws DataStoreException {
        final var aggregation = assertInstanceOf(Aggregate.class, aggregator.build(null));
        assertTrue(aggregation.components().isEmpty());
    }

    /**
     * Creates a cube as a stack of two-dimensional resources.
     */
    private ConcatenatedGridResource stack() throws DataStoreException {
        ranges = List.of(new SampleDimension(Names.createLocalName(null, null, "Slice"), null, List.of()));
        final long[] lower = {MIN_X, MIN_Y, MIN_Z};
        final long[] upper = lower.clone();
        upper[0] += WIDTH  - 1;
        upper[1] += HEIGHT - 1;
        final var gridToCRS = MathTransforms.identity(3);
        for (int i=0; i<DEPTH; i++) {
            lower[2] = upper[2] = MIN_Z + i;
            var extent = new GridExtent(null, lower, upper, true);
            var domain = new GridGeometry(extent, PixelInCell.CELL_CORNER, gridToCRS, null);
            var image  = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
            var buffer = assertInstanceOf(DataBufferByte.class, image.getRaster().getDataBuffer());
            Arrays.fill(buffer.getData(), (byte) (PIXEL_BASE_VALUE + i));
            aggregator.add(new OpaqueGridResource(new GridCoverage2D(domain, ranges, image)));
        }
        return assertInstanceOf(ConcatenatedGridResource.class,
                aggregator.build(Names.createLocalName(null, null, "Aggregation")));
    }

    /**
     * Tests a three-dimensional cube made of a stack of two-dimensional slices.
     *
     * @throws DataStoreException if an error occurred.
     */
    @Test
    public void testStack() throws DataStoreException {
        ConcatenatedGridResource resource = stack();
        assertEquals(4, resource.locator.slices.length);
        final GridCoverage full = resource.read(null, null);
        assertEquals(ranges, full.getSampleDimensions());
        final long[] lower = {MIN_X, MIN_Y, MIN_Z};
        for (int i=0; i<DEPTH; i++) {
            lower[2] = MIN_Z + i;
            final var extent = new GridExtent(null, lower, lower, true);
            assertFirstPixelEquals(PIXEL_BASE_VALUE + i, full.render(extent));
            /*
             * Test the same slice as above, but fetched from a read operation.
             * The difference between the two approaches is when data are loaded.
             */
            GridGeometry domain = resource.getGridGeometry().derive().subgrid(extent, (long[]) null).build();
            GridCoverage coverage = resource.read(domain, null);
            assertEquals(ranges, full.getSampleDimensions());
            assertFirstPixelEquals(PIXEL_BASE_VALUE + i, coverage.render(null));
        }
    }

    /**
     * Asserts that the first pixel of the given image is equal to the given value.
     */
    private static void assertFirstPixelEquals(final int expected, final RenderedImage image) {
        final Raster tile = image.getTile(image.getMinTileX(), image.getMinTileY());
        assertEquals(expected, tile.getSample(image.getMinX(), image.getMinY(), 0));
    }
}
