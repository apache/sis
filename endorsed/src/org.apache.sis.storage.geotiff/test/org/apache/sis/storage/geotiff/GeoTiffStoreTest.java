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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Files;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.util.Utilities;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.image.DataType;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.matrix.Matrix4;

// Test dependencies
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertSingleton;
import static org.apache.sis.feature.Assertions.assertGridToCornerEquals;
import static org.apache.sis.feature.Assertions.assertPixelsEqual;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.operation.HardCodedConversions;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import static org.opengis.test.Assertions.assertAxisDirectionsEqual;


/**
 * Integration tests for {@link GeoTiffStore}.
 * This class tests indirectly (via {@link GeoTiffStore}) the {@link Reader} and {@link Writer} classes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class GeoTiffStoreTest extends TestCase {
    /**
     * Name of a test file for an untiled image with a single band in gray-scale.
     */
    static final String UNTILED = "untiled.tiff";

    /**
     * Name of a test file for an image similar to {@link #UNTILED} but tiled.
     */
    static final String TILED = "tiled.tiff";

    /**
     * Creates a new test case.
     */
    public GeoTiffStoreTest() {
    }

    /**
     * Tests writing an image with a non-linear vertical component in the "grid to CRS" transform.
     * This method merely tests that no exception is thrown during the execution, and that reading
     * the image gives back the three-dimensional <abbr>CRS</abbr>.
     *
     * @throws Exception if an error occurred while preparing or running the test.
     */
    @Test
    public void testNonLinearVerticalTransform() throws Exception {
        final int width     = 5;
        final int height    = 4;
        final var builder   = new GridCoverageBuilder();
        final var extent    = new GridExtent(null, null, new long[] {width, height, 1}, false);
        final var crs       = CRS.compound(HardCodedCRS.WGS84_LATITUDE_FIRST, HardCodedCRS.DEPTH);
        final var gridToCRS = MathTransforms.compound(
                MathTransforms.scale(0.25, -0.25),
                MathTransforms.interpolate(new double[] {0, 1, 2}, new double[] {3, 4, 9}));
        builder.setDomain(new GridGeometry(extent, PixelInCell.CELL_CORNER, gridToCRS, crs));
        builder.setValues(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
        final GridCoverage coverage = builder.build();
        final Path file = Files.createTempFile("sis-test-", ".tiff");
        try {
            try (DataStore store = DataStores.openWritable(file, "GeoTIFF")) {
                assertInstanceOf(GeoTiffStore.class, store).append(coverage, null);
            }
            /*
             * Read the image that we wrote in above block. This block merely tests that no exception is thrown,
             * and that the result has the expected number of dimensions, axis order and scale factors.
             */
            try (DataStore store = DataStores.open(file, "GeoTIFF")) {
                GridCoverageResource r = assertSingleton(assertInstanceOf(GeoTiffStore.class, store).components());
                GridGeometry gg = r.getGridGeometry();
                assertEquals(3, gg.getDimension());
                assertAxisDirectionsEqual(gg.getCoordinateReferenceSystem().getCoordinateSystem(),
                        AxisDirection.EAST, AxisDirection.NORTH, AxisDirection.DOWN);

                assertGridToCornerEquals(
                        new Matrix4(0, -0.25, 0, 0,
                                    0.25,  0, 0, 0,
                                    0,     0, 1, 3,
                                    0,     0, 0, 1), gg);

                RenderedImage image = r.read(null).render(null);
                assertEquals(width,  image.getWidth(),  "width");
                assertEquals(height, image.getHeight(), "height");
            }
        } finally {
            Files.delete(file);
        }
    }

    /**
     * Writes an image and compare with the {@code "untiled.tiff"} file.
     *
     * @throws Exception if a referencing or I/O error occurred.
     */
    @Test
    public void testWriteUntiled() throws Exception {
        testWrite(new Rectangle(32, 16), null);
    }

    /**
     * Writes an image and compare with the {@code "tiled.tiff"} file.
     *
     * @throws Exception if a referencing or I/O error occurred.
     */
    @Test
    public void testWriteTiled() throws Exception {
        final var tileSize = new Dimension(16, 16);     // TIFF tile size must be multiple of 16.
        testWrite(new Rectangle(tileSize.width * 3, tileSize.height * 2), tileSize);
    }

    /**
     * Writes an image and compare with the {@code "tiled.tiff"} file.
     * <p>
     * This test differs from {@link #testWriteTiled()} because it requests a tile size not accepted as is by geotiff.
     * The aim of this test is to ensure that Geotiff writer will adapt tile size according to the Tiff standard.
     * It requests tiles of size 19, and expect the Geotiff writer to adapt request to write tiles of size 16 or 32.
     * </p>
     */
    @Test
    public void testWriteTiledAdapted() throws Exception {
        final var tileSize = new Dimension(7, 7);
        testWrite(new Rectangle(64, 64), tileSize);
    }

    /**
     * Implementation of {@link #testWriteUntiled()} and {@link #testWriteTiled()}.
     *
     * @param  bounds    bounds of the image to create.
     * @param  tileSize  size of the tiles, or {@code null} for the image size.
     */
    private static void testWrite(final Rectangle bounds, final Dimension tileSize)
            throws TransformException, DataStoreException
    {
        /*
         * We need a CRS which has no EPSG code for ensuring that the test write the same GeoTIFF keys
         * with or without the presence of an EPSG database on machine which is building this project.
         */
        var crs = HardCodedConversions.mercator(HardCodedCRS.JUPITER);
        var geographicArea = new GeneralEnvelope(HardCodedCRS.JUPITER);
        geographicArea.setRange(0, 132, 145);   // Range of longitude values.
        geographicArea.setRange(1,  30,  42);   // Range of latitude values.
        final GridCoverage coverage = new GridCoverageBuilder()
                .setDomain(Envelopes.transform(geographicArea, crs))
                .setIntegerValues(DataType.BYTE, bounds, tileSize, (x, y) -> 100 * y + x)
                .flipGridAxis(1)
                .build();

        final var buffer = new ByteArrayOutputStream();
        try (DataStore ds = DataStores.openWritable(buffer, "geotiff")) {
            assertInstanceOf(GeoTiffStore.class, ds).append(coverage, null);
        }

        final byte[] actual = buffer.toByteArray();
        try (var store = new GeoTiffStore(new GeoTiffStoreProvider(), new StorageConnector(ByteBuffer.wrap(actual)))) {
            var coverageToValidate = store.components().get(0).read(null);
            final var expectedGridGeom = coverage.getGridGeometry();
            final var actualGridGeom = coverageToValidate.getGridGeometry();
            assertTrue(
                    Utilities.equalsApproximately(expectedGridGeom, actualGridGeom),
                    () -> String.format(
                            "Written grid geometry differs from original one.%nOriginal:%n%s%nWritten:%n%s%n",
                            expectedGridGeom, actualGridGeom
                    )
            );

            assertTrue(
                    Utilities.equalsApproximately(expectedGridGeom, actualGridGeom),
                    () -> String.format(
                            "Written grid geometry differs from original one.%nOriginal:%n%s%nWritten:%n%s%n",
                            expectedGridGeom, actualGridGeom
                    )
            );

            final var expectedSampleDims = coverage.getSampleDimensions();
            final var actualSampleDims = coverageToValidate.getSampleDimensions();
            assertTrue(
                    Utilities.equalsApproximately(expectedSampleDims, actualSampleDims),
                    () -> String.format(
                            "Written Sample dimensions differ from original one.%nOriginal:%n%s%nWritten:%n%s%n",
                            expectedSampleDims, actualSampleDims
                    )
            );

            final var actualRendering = coverageToValidate.render(null);
            assertPixelsEqual(coverage.render(null), null, actualRendering, null);
            // If user requested a tiled dataset, we must ensure the written Geotiff file has been tiled
            if (tileSize != null && (tileSize.getWidth() < bounds.getWidth() || tileSize.getHeight() < bounds.getHeight())) {
                assertTiling(actualRendering, tileSize, 16);
            }
        }
    }

    /**
     * Represent the side of the tile being evaluated. Either width (X) or height (Y).
     */
    private enum TileAxis { width, height }

    /**
     * Verify that given image tiling respects user tiling request, modulo a given restriction.
     * The restriction maps Tiff standard requirement for tile size to be multiple of a given factor.
     * </br>
     * It means that if user requests a tile size of 3, but the restriction factor is 2,
     * then we expect the image to use a tile size of either 2 or 4,
     * which are the nearest enclosing multiples of 2 for request 3.
     *
     * @param actualRendering The image to control tiling on.
     * @param tileSize The tile size requested by user.
     * @param tileSizeMultiple A factor to use to adapted requested tile size.
     */
    private static void assertTiling(RenderedImage actualRendering, Dimension tileSize, int tileSizeMultiple) {
        assertTileSize(TileAxis.width, actualRendering.getWidth(), actualRendering.getTileWidth(), tileSize.width, tileSizeMultiple);
        assertTileSize(TileAxis.height, actualRendering.getHeight(), actualRendering.getTileHeight(), tileSize.height, tileSizeMultiple);
    }

    /**
     * Test a specific tile side according to requirements expressed by {@link #assertTiling(RenderedImage, Dimension, int)}.
     *
     * @param axis Which side of the tiling is being tested. Used for assertion error message formatting.
     * @param imgSize The image actual size along tested side (its {@link RenderedImage#getWidth() width} or {@link RenderedImage#getHeight() height}).
     * @param imgActualTileSize The image actual tile size along tested side (its {@link RenderedImage#getTileWidth() tile width} or {@link RenderedImage#getTileHeight() tile height}).
     * @param requestedTileSize User request tile size along the side to test.
     * @param tileSizeMultiple The restriction factor: actual tile size must be a multiple of this value, independently of the user request.
     */
    private static void assertTileSize(TileAxis axis, int imgSize, int imgActualTileSize, int requestedTileSize, int tileSizeMultiple) {
        if (imgSize > requestedTileSize) {
            final int modulo = requestedTileSize % tileSizeMultiple;
            if (modulo == 0) {
                assertEquals(requestedTileSize, imgActualTileSize, () -> "Tile " + axis);
            } else if (requestedTileSize < tileSizeMultiple) {
                assertEquals(tileSizeMultiple, imgActualTileSize, () -> "Tile " + axis);
            } else {
                final var minTileSize = requestedTileSize - modulo;
                final var maxTileSize = requestedTileSize + (tileSizeMultiple - modulo);
                assertTrue(imgActualTileSize == minTileSize || imgActualTileSize == maxTileSize,
                        () -> String.format(
                                "Tile %s should be either %d or %d (because it must be a multiple of %d), but it is %d",
                                axis, minTileSize, maxTileSize, tileSizeMultiple, imgActualTileSize
                        )
                );
            }
        }
    }
}
