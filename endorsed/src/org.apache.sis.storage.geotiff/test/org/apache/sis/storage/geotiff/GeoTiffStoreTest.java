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

import java.util.Random;
import java.util.Iterator;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.Utilities;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.image.DataType;
import org.apache.sis.storage.OptionKey;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import static org.apache.sis.storage.geotiff.writer.ReformattedImage.TILE_DIVISOR;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertSingleton;
import static org.apache.sis.feature.Assertions.assertPixelsEqual;
import static org.apache.sis.feature.Assertions.assertGridToCornerEquals;
import org.apache.sis.image.OverviewImageTest;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.operation.HardCodedConversions;

// Specific to the main branch:
import static org.apache.sis.test.GeoapiAssert.assertAxisDirectionsEqual;


/**
 * Integration tests for {@link GeoTiffStore}.
 * This class tests indirectly (via {@link GeoTiffStore}) the {@link Reader} and {@link Writer} classes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Estelle Idée (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
@SuppressWarnings("exports")
public final class GeoTiffStoreTest extends TestCase {
    /**
     * Name of a test file for an untiled image with a single band in gray-scale.
     * The image is uncompressed for avoiding <abbr>JVM</abbr>-dependent variations.
     */
    static final String UNTILED_WITHOUT_COMPRESSION = "untiled_without_compression.tiff";

    /**
     * Name of a test file for an image similar to {@link #UNTILED_WITHOUT_COMPRESSION} but tiled.
     * The image is uncompressed for avoiding <abbr>JVM</abbr>-dependent variations.
     */
    static final String TILED_WITHOUT_COMPRESSION = "tiled_without_compression.tiff";

    /**
     * Name of a test file for an untiled image with a single band in gray-scale.
     * This image has been encoded with the default compression and predictor.
     * Note that the stream of bytes encoded with {@link Compression#DEFLATE}
     * may varies depending on the <abbr>JVM</abbr>.
     */
    static final String UNTILED = "untiled.tiff";

    /**
     * Name of a test file for an image similar to {@link #UNTILED} but tiled.
     * This image has been encoded with the default compression and predictor.
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
     * Writes an image and compares with the {@code "untiled_without_compression.tiff"} file.
     * Then, reads back the image and performs some validations.
     *
     * @throws Exception if a referencing or I/O error occurred.
     */
    @Test
    public void testWriteUntiled() throws Exception {
        testWriteAndRead(UNTILED_WITHOUT_COMPRESSION, new Rectangle(32, 16), null, 1240);
    }

    /**
     * Writes an image and compares with the {@code "tiled_without_compression.tiff"} file.
     * Then, reads back the image and performs some validations.
     *
     * @throws Exception if a referencing or I/O error occurred.
     */
    @Test
    public void testWriteTiled() throws Exception {
        final var tileSize = new Dimension(16, 16);     // TIFF tile size must be multiple of 16.
        testWriteAndRead(TILED_WITHOUT_COMPRESSION, new Rectangle(tileSize.width * 3, tileSize.height * 2), tileSize, 2324);
    }

    /**
     * Writes an image and validates the result.
     * This test differs from {@link #testWriteTiled()} because it requests a tile size not accepted as-is by GeoTIFF.
     * The aim of this test is to ensure that GeoTIFF writer will adapt tile size according to the TIFF standard.
     * It requests tiles of size 7, and expects the GeoTIFF writer to adapt request for writing tiles of size 16 or 32.
     *
     * @throws Exception if a referencing or I/O error occurred.
     */
    @Test
    public void testWriteResizedTiles() throws Exception {
        testWriteAndRead(null, new Rectangle(64, 64), new Dimension(7, 7), 4964);
    }

    /**
     * Implementation of {@link #testWriteUntiled()} and {@link #testWriteTiled()}.
     * The image is written with no compression for avoiding variations in compression algorithms.
     *
     * @param  filename  name of the file which contain the expected image, or {@code null} if none.
     * @param  bounds    bounds of the image to create.
     * @param  tileSize  size of the tiles, or {@code null} for the image size.
     * @param  length    expected length in bytes.
     */
    private static void testWriteAndRead(final String filename, final Rectangle bounds, final Dimension tileSize, final int length)
            throws TransformException, DataStoreException, IOException
    {
        /*
         * We need a CRS which has no EPSG code for ensuring that the test write the same GeoTIFF keys
         * with or without the presence of an EPSG database on machine which is building this project.
         */
        final var crs =  HardCodedConversions.mercator(HardCodedCRS.JUPITER);
        final var geographicArea = new GeneralEnvelope(HardCodedCRS.JUPITER);
        geographicArea.setRange(0, 132, 145);   // Range of longitude values.
        geographicArea.setRange(1,  30,  42);   // Range of latitude values.
        final GridCoverage coverage = new GridCoverageBuilder()
                .setDomain(Envelopes.transform(geographicArea, crs))
                .setIntegerValues(DataType.BYTE, bounds, tileSize, (x, y) -> 100 * y + x)
                .flipGridAxis(1)
                .build();

        final var buffer = new ByteArrayOutputStream(length);
        final var source = new StorageConnector(buffer);
        source.setOption(Compression.OPTION_KEY, Compression.NONE);
        try (DataStore ds = DataStores.openWritable(source, "geotiff")) {
            assertInstanceOf(GeoTiffStore.class, ds).append(coverage, null);
        }
        final byte[] actual = buffer.toByteArray();
        assertEquals(length, actual.length);
        if (filename != null) {
            final byte[] expected;
            try (InputStream in = GeoTiffStoreTest.class.getResourceAsStream(filename)) {
                assertNotNull(in, filename);
                expected = in.readAllBytes();
            }
            assertArrayEquals(expected, actual);
        }
        /*
         * At this point, the test of the writer is considered as completed since we compared the writer output
         * against the expected stream of bytes in a file. The remaining of this method is test of the reader,
         * unless we had no file to compare with.
         */
        try (var store = new GeoTiffStore(null, new StorageConnector(ByteBuffer.wrap(actual)))) {
            final var coverageToValidate = assertSingleton(store.components()).read(null);
            assertEqualsApproximately(
                    coverage.getGridGeometry(),
                    coverageToValidate.getGridGeometry(),
                    "Written grid geometry differs from original one.");

            assertEqualsApproximately(
                    coverage.getSampleDimensions(),
                    coverageToValidate.getSampleDimensions(),
                    "Written Sample dimensions differ from original one.");

            final var actualRendering = coverageToValidate.render(null);
            assertPixelsEqual(coverage.render(null), null, actualRendering, null);
            if (tileSize != null) {
                // If user requested a tiled dataset, we must ensure the written GeoTIFF file has been tiled.
                validateTileSize("width",  tileSize.width,  actualRendering.getTileWidth());
                validateTileSize("height", tileSize.height, actualRendering.getTileHeight());
            }
        }
    }

    /**
     * Asserts that the given object are approximately equal.
     *
     * @param expected  the expected object.
     * @param actual    the actual object.
     * @param message   message to show if the objects are not approximately equal.
     */
    private static void assertEqualsApproximately(final Object expected, final Object actual, final String message) {
        assertTrue(Utilities.equalsApproximately(expected, actual),
                () -> String.format("%s%nOriginal:%n%s%nWritten:%n%s%n", message, expected, actual));
    }

    /**
     * Verifies that given tile width or height is compliant with <abbr>TIFF</abbr> standard.
     * The {@code sourceSize} and {@code writtenSize} arguments shall be the rendered image's
     * {@linkplain RenderedImage#getTileWidth() tile width} or {@linkplain RenderedImage#getTileHeight() tile height}.
     *
     * <p>If the source image had a tile size which was already compliant with <abbr>TIFF</abbr> requirement,
     * then the image should have been written. Otherwise, there is no guarantee about the size of the tiles.
     * It will not necessarily be the multiple of 16 immediately before or after the original tile size.</p>
     *
     * @param axis        which side of the tiling is being tested. Used for assertion error message formatting.
     * @param sourceSize  the tile size along tested side in the image to write.
     * @param writtenSize the tile size along tested side in the image which has been written.
     */
    private static void validateTileSize(final String axis, final int sourceSize, final int writtenSize) {
        assertEquals(0, writtenSize % TILE_DIVISOR, () -> axis + " tile size is not a multiple of " + TILE_DIVISOR);
        if ((sourceSize % TILE_DIVISOR) == 0) {
            assertEquals(sourceSize, writtenSize, () -> axis + " should have keep the original tile size");
        }
    }

    /**
     * Tests writing a pyramided image.
     *
     * @throws Exception if an error occurred while preparing or running the test.
     */
    @Test
    public void testPyramided() throws Exception {
        final Random r   = TestUtilities.createRandomNumberGenerator();
        final int width  = r.nextInt(2 * Writer.OVERVIEW_SIZE) + 3 * Writer.OVERVIEW_SIZE;
        final int height = r.nextInt(2 * Writer.OVERVIEW_SIZE) + 3 * Writer.OVERVIEW_SIZE;
        final var area   = new GeneralEnvelope(HardCodedCRS.WGS84);
        area.setRange(0, 132, 145);   // Range of longitude values.
        area.setRange(1,  30,  42);   // Range of latitude values.
        final GridCoverage coverage = new GridCoverageBuilder()
                .setDomain(Envelopes.transform(area, HardCodedCRS.WGS84))
                .setValues(DataType.BYTE, new Rectangle(width, height), null, (x, y) -> 100 * y + x)
                .flipGridAxis(1)
                .build();

        final Path path = Files.createTempFile("pyramided", ".tiff");
        try {
            final var connector = new StorageConnector(path);
            connector.setOption(FormatModifier.OPTION_KEY, new FormatModifier[] {
                FormatModifier.PYRAMIDED
            });
            connector.setOption(OptionKey.OPEN_OPTIONS, new StandardOpenOption[] {
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
            });
            try (var store = new GeoTiffStore(null, connector)) {
                assertNotNull(store.append(coverage, null));
            }
            /*
             * Try to read the image using the standard TIFF reader, which is used as a reference implementation.
             * We expect at least one implementation. If there is more implementations, test will all of them.
             */
            final Iterator<ImageReader> imageReaders = ImageIO.getImageReadersByFormatName("TIFF");
            assertTrue(imageReaders.hasNext());
            do {
                final ImageReader reader = imageReaders.next();
                try (ImageInputStream input = ImageIO.createImageInputStream(path.toFile())) {
                    reader.setInput(input);
                    RenderedImage image = reader.read(0);
                    assertEquals(width,  image.getWidth());
                    assertEquals(height, image.getHeight());
                    int imageIndex = 1;
                    while (image.getWidth() > Writer.OVERVIEW_SIZE || image.getHeight() > Writer.OVERVIEW_SIZE) {
                        final RenderedImage overview = reader.read(imageIndex);
                        OverviewImageTest.verify(image, overview, true);
                        image = overview;
                        imageIndex++;
                    }
                    try {
                        reader.read(imageIndex);
                        fail("Expected no more images.");
                    } catch (IndexOutOfBoundsException e) {
                        // Ignore expected exception.
                    }
                    assertTrue(imageIndex > 1);     // Expect at least one overview.
                }
                reader.dispose();
            } while (imageReaders.hasNext());
        } finally {
            Files.delete(path);
        }
    }
}
