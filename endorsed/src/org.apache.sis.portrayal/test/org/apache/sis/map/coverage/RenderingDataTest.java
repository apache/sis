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
package org.apache.sis.map.coverage;

import java.util.Map;
import java.util.Arrays;
import java.awt.Point;
import java.awt.color.ColorSpace;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverage2D;
import org.apache.sis.coverage.grid.GridCoverageProcessor;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.ImageRenderer;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.image.ErrorHandler;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.image.internal.shared.TiledImage;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;
import org.apache.sis.storage.MemoryGridCoverageResource;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertSingleton;
import org.apache.sis.test.TestCase;


/**
 * Headless reproduction of the {@code CoverageCanvas} display chain for a tiled image.
 * Compares 2D resource against the same resource augmented with a temporal dimension.
 */
public final class RenderingDataTest extends TestCase {
    /**
     * Width and height (in pixels) of the rendered image produced in output.
     */
    private static final int RENDERED_SIZE = 8;

    /**
     * Width and height (in pixels) of the resource used as input.
     */
    private static final int DATA_SIZE = 128;

    /**
     * Tile height in pixels. There is no tile width as this test does not tile the image horizontally.
     * It is sufficient to focus the test on the tiling in only one axis, which is <abbr>y</abbr>.
     */
    private static final int TILE_HEIGHT = 16;

    /**
     * Number of meters per pixel at full resolution.
     */
    private static final double RESOLUTION = 10;

    /**
     * The western-most coordinate of the limit of the resource extent.
     * Unit is meters of the <abbr>UTM</abbr> zone 31 projection.
     */
    private static final double WEST_BOUND = 400000;

    /**
     * The northern-most coordinate of the limit of the resource extent.
     * Unit is meters of the <abbr>UTM</abbr> zone 31 projection.
     */
    private static final double NORTH_BOUND = 5000000;

    /**
     * Creates a new text case.
     */
    public RenderingDataTest() {
    }

    /**
     * Simulates the {@code CoverageCanvas} worker from the <abbr>GUI</abbr> module.
     * The steps include:
     *
     * <ul>
     *   <li>pyramid loader,</li>
     *   <li>{@link RenderingData#ensureCoverageLoaded(LinearTransform, DirectPosition)},</li>
     *   <li>{@link RenderingData#ensureImageLoaded(GridCoverage, GridExtent, boolean)},</li>
     *   <li>{@link RenderingData#resampleAndConvert(RenderedImage, LinearTransform, DirectPosition)}.</li>
     * </ul>
     *
     * @throws Exception if a data store, a factory or a transform exception occurred.
     */
    @Test
    public void testDisplayChain() throws Exception {
        final GridCoverage coverage = createTiledCoverage();
        runDisplayChain(coverage);
        runDisplayChain(addTemporalDimension(coverage));
    }

    /**
     * Creates a two-dimensional coverage wrapping a tiled image with an <var>UTM</var> projection.
     * The pixel values do not matter and are all zeros. What matter for this test is the tiling.
     * Therefore, for saving memory and <abbr>CPU</abbr>, all tiles share the same data buffer.
     */
    private static GridCoverage createTiledCoverage() {
        final var gridToCRS  = new AffineTransform2D(RESOLUTION, 0, 0, -RESOLUTION, WEST_BOUND, NORTH_BOUND);
        final var crs        = CommonCRS.WGS84.universal(45, 3);   // UTM zone 31N.
        final var gg         = new GridGeometry(new GridExtent(DATA_SIZE, DATA_SIZE), PixelInCell.CELL_CORNER, gridToCRS, crs);
        final var properties = Map.of(TiledImage.GRID_GEOMETRY_KEY, gg);

        final var colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        final var colors     = new ComponentColorModel(colorSpace, false, true, ComponentColorModel.OPAQUE, DataBuffer.TYPE_BYTE);
        final var layout     = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE, DATA_SIZE, TILE_HEIGHT, 1);
        final var buffer     = new DataBufferByte(DATA_SIZE * TILE_HEIGHT / Byte.SIZE, 1);
        final var tiles      = new Raster[DATA_SIZE / TILE_HEIGHT];
        Arrays.setAll(tiles, (i) -> Raster.createRaster(layout, buffer, new Point(0, i * TILE_HEIGHT)));
        final var image      = new TiledImage(properties, colors, DATA_SIZE, DATA_SIZE, 0, 0, tiles);
        return new GridCoverage2D(gg, null, image);
    }

    /**
     * Adds a temporal dimension to the given coverage.
     */
    private static GridCoverage addTemporalDimension(final GridCoverage coverage) {
        var timeCRS    = CommonCRS.Temporal.UNIX.crs();
        var gridToTime = MathTransforms.linear(86400, 24*60*60);   // 1 cell = 1 day.
        var gridExtent = new GridExtent(null, null, new long[1], true);
        var dimToAdd   = new GridGeometry(gridExtent, PixelInCell.CELL_CORNER, gridToTime, timeCRS);
        var processor  = new GridCoverageProcessor();
        return processor.appendDimensions(coverage, dimToAdd);
    }

    /**
     * Simulates the {@code CoverageCanvas} worker from the <abbr>GUI</abbr> module.
     * The steps include:
     *
     * <ul>
     *   <li>pyramid loader,</li>
     *   <li>{@link RenderingData#ensureCoverageLoaded(LinearTransform, DirectPosition)},</li>
     *   <li>{@link RenderingData#ensureImageLoaded(GridCoverage, GridExtent, boolean)},</li>
     *   <li>{@link RenderingData#resampleAndConvert(RenderedImage, LinearTransform, DirectPosition)}.</li>
     * </ul>
     */
    private static void runDisplayChain(final GridCoverage coverage) throws Exception {
        final var resource = new MemoryGridCoverageResource(coverage);
        /*
         * Prepare a region of interest (ROI) in the middle of the resource domain.
         * For this test, it is important that the ROI does not cover all tiles.
         * Because the ROI is in the middle of the data domain,
         * the envelope center should be invariant.
         */
        GridGeometry domain = resource.getGridGeometry();
        final var zoomArea = new GeneralEnvelope(domain.getEnvelope());
        for (int i = 0; i < RenderingData.BIDIMENSIONAL; i++) {
            final double margin = zoomArea.getSpan (i) / 4;
            zoomArea.setRange(i,  zoomArea.getLower(i) + margin,
                                  zoomArea.getUpper(i) - margin);
        }
        verifyLocationOfCenter(domain);
        domain = domain.derive().subgrid(zoomArea, null).build();
        verifyLocationOfCenter(domain);
        final GridExtent sliceExtent = domain.getExtent();
        /*
         * Simulate the fallback in `RenderingData.ensureImageLoaded(…)` which recompute
         * the grid geometry using `ImageRenderer` when that information is not provided
         * as a property.
         */
        {
            final var r = new ImageRenderer(coverage, sliceExtent);
            verifyLocationOfCenter(r.getImageGeometry(RenderingData.BIDIMENSIONAL));
            assertArrayEquals(new int[] {0, 1}, r.getXYDimensions());
        }
        domain = domain.selectDimensions(0, 1);
        final double scale = ((double) RENDERED_SIZE) / (DATA_SIZE * RESOLUTION);
        final var objectiveToDisplay = new AffineTransform2D(scale, 0, 0, -scale, WEST_BOUND * -scale, NORTH_BOUND * scale);
        final CoordinateReferenceSystem objectiveCRS = domain.getCoordinateReferenceSystem();
        final var poi = new DirectPosition2D(objectiveCRS, WEST_BOUND + DATA_SIZE * RESOLUTION / 2,
                                                          NORTH_BOUND - DATA_SIZE * RESOLUTION / 2);
        /*
         * Render the request which has been prepared above.
         * Get the result, but also the source of the result.
         */
        final var render = new RenderingData(ErrorHandler.THROW);
        render.coverageLoader = new MultiResolutionCoverageLoader(resource, null, null);
        render.setImageSpace(domain, resource.getSampleDimensions(), new int[] {0, 1});
        render.setObjectiveCRS(objectiveCRS);
        render.ensureImageLoaded(render.ensureCoverageLoaded(objectiveToDisplay, poi), sliceExtent, true);
        final RenderedImage shown  = render.resampleAndConvert(render.getSourceImage(), objectiveToDisplay, poi);
        final RenderedImage source = assertSingleton(shown.getSources());
        /*
         * Verify the source of the result before to verify the final result.
         * The source should have the same size as the original data,
         * but translated for having the request at coordinates (0,0).
         */
        assertEquals(-DATA_SIZE / 4,   source.getMinX());   // Difference relative to requested area.
        assertEquals( DATA_SIZE,       source.getWidth());
        assertEquals( 0,               source.getMinY());
        assertEquals( DATA_SIZE / 2,   source.getHeight());
        assertEquals(0,                 shown.getMinX());
        assertEquals(RENDERED_SIZE,     shown.getWidth());
        assertEquals(RENDERED_SIZE,     shown.getTileWidth());
        assertEquals(RENDERED_SIZE / 4, shown.getMinY());
        assertEquals(RENDERED_SIZE / 2, shown.getHeight());
        assertEquals(RENDERED_SIZE / 2, shown.getTileHeight());
        /*
         * Verify georeferencing.
         */
        verifyLocationOfCenter(assertInstanceOf(GridGeometry.class, source.getProperty(PlanarImage.GRID_GEOMETRY_KEY)));
    }

    /**
     * Verifies that the center of the given grid geometry is in the center of the test domain.
     */
    private static void verifyLocationOfCenter(final GridGeometry domain) throws TransformException {
        final Envelope envelope = domain.getEnvelope();
        assertEquals( WEST_BOUND + RESOLUTION * (DATA_SIZE / 2), envelope.getMedian(0));
        assertEquals(NORTH_BOUND - RESOLUTION * (DATA_SIZE / 2), envelope.getMedian(1));

        final GridExtent extent = domain.getExtent();
        final var center = new GeneralDirectPosition(extent.getDimension());
        for (int i = center.getDimension(); --i >= 0;) {
            center.setCoordinate(i, extent.getMedian(i));
        }
        assertSame(center, (domain.getGridToCRS(PixelInCell.CELL_CORNER).transform(center, center)));
        assertEquals( WEST_BOUND + RESOLUTION * (DATA_SIZE / 2), center.getCoordinate(0));
        assertEquals(NORTH_BOUND - RESOLUTION * (DATA_SIZE / 2), center.getCoordinate(1));
    }
}
