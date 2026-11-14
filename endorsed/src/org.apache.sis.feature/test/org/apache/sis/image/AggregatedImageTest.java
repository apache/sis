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
package org.apache.sis.image;

import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.image.internal.shared.WritableTiledImage;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;
import org.apache.sis.pending.jdk.JDK18;
import org.apache.sis.util.ArraysExt;
import static java.lang.Float.NaN;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.feature.Assertions.mismatchedSampleValue;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link AggregatedImage}. The source images are small images with predictable values
 * computed by {@link TiledImageMock}. In the "BTYX" pattern of those values, the "B" digit
 * (band) is replaced by a digit identifying the source image. For example, value 2311
 * is the pixel at row 1 and column 1 of the third tile of the second source image.
 *
 * <p>The image sizes, tile sizes, image origins and tile indices are hard-coded to different
 * values in each image for increasing the chances to detect errors in index calculations.
 * The expected values are hard-coded and have been computed independently of the
 * {@link AggregatedImage} implementation.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class AggregatedImageTest extends TestCase {
    /**
     * The image bounds, or {@code null} for automatic.
     * Used at construction and for verifying the properties of the result.
     * If the bounds was null at construction time, then it should be set to
     * the expected value before verifying the result.
     */
    private Rectangle bounds;

    /**
     * Size of the tiles of the aggregated image.
     * Used at construction and for verifying the properties of the result.
     */
    private int tileWidth, tileHeight;

    /**
     * Minimal tile index of the aggregated image.
     * Used at construction and for verifying the properties of the result.
     */
    private int minTileX, minTileY;

    /**
     * Number of bands in the image to create.
     */
    private int numBands;

    /**
     * The aggregated image to verify.
     */
    private AggregatedImage image;

    /**
     * Creates a new test case.
     */
    public AggregatedImageTest() {
    }

    /**
     * Creates a source for the aggregated image with a transform made of a scale followed by a translation.
     * The source image have a single band of floating point values following the "STYX" pattern
     * where <var>S</var> is the given source number, <var>T</var> is the tile number starting at 1,
     * and <var>Y</var>, <var>X</var> are pixel coordinates relative to the tile.
     *
     * @param  sourceNumber  number from 1 to 9 identifying the source image.
     * @return the source image.
     */
    private static AggregatedImage.Source source(
            final int sourceNumber,
            final int numBands,
            final int minX,
            final int minY,
            final int width,
            final int height,
            final int tileWidth,
            final int tileHeight,
            final int minTileX,
            final int minTileY,
            final boolean banded,
            final boolean writable,
            final double scaleX,
            final double scaleY,
            final double translateX,
            final double translateY)
    {
        final var image = new TiledImageMock(DataBuffer.TYPE_FLOAT, numBands,
                minX, minY, width, height, tileWidth, tileHeight, minTileX, minTileY, banded);
        image.initializeAllTiles(ArraysExt.range(0, numBands), (sourceNumber - 1) * 1000);
        image.validate(false);
        return new AggregatedImage.Source(
                writable ? image.toWritableTiledImage() : image,
                new AffineTransform2D(scaleX, 0, 0, scaleY, translateX, translateY));
    }

    /**
     * Creates the aggregated image to test and store the image in the {@link #image} field.
     * All other fields of this class must be initialized before to invoke this method,
     * except {@link #bounds} which can be {@code null} for automatic calculation.
     *
     * @param  sources  the images to aggregate with their transforms from <em>source</em> to <em>this</em>.
     * @throws TransformException if {@link #bounds} is null and an error occurred while computing automatic bounds.
     */
    private void createAggregatedImage(final AggregatedImage.Source... sources) throws TransformException {
        final var tileSize = new Dimension(tileWidth, tileHeight);
        final var minTile  = new Point(minTileX, minTileY);
        image = new AggregatedImage(
                sources,
                new ImageLayout(null, tileSize, false, false, true, minTile),
                bounds,
                Aggregation.RASTER_SPACE_WEIGHTING,
                null);      // Fill values.
    }

    /**
     * Verifies the layout of the given image, then verifies all its sample values.
     * This method reads the tiles one by one, verifying the location of each tile.
     *
     * @param  band      the band to verify.
     * @param  expected  all expected sample values in a row-major fashion.
     */
    private void verify(final int band, final float... expected) {
        assertNotNull(image);   // Shall be set by the caller.
        assertNotNull(bounds);  // Idem.
        assertFalse(bounds.isEmpty());
        final int numXTiles  = JDK18.ceilDiv(bounds.width,  tileWidth);
        final int numYTiles  = JDK18.ceilDiv(bounds.height, tileHeight);
        String inconsistency = image.verify();
        if ("width".equals(inconsistency) || "height".equals(inconsistency)) {
            inconsistency = null;   // Tolerance to width and height because some tiles are partial.
        }
        assertNull(inconsistency);
        assertEquals(bounds,     image.getBounds());
        assertEquals(tileWidth,  image.getTileWidth());
        assertEquals(tileHeight, image.getTileHeight());
        assertEquals(minTileX,   image.getMinTileX());
        assertEquals(minTileY,   image.getMinTileY());
        assertEquals(numXTiles,  image.getNumXTiles());
        assertEquals(numYTiles,  image.getNumYTiles());
        assertEquals(numBands,   image.getSampleModel().getNumBands());
        assertEquals(bounds.width * bounds.height, expected.length, "Invalid number of expected values.");
        for (int ty=0; ty<numYTiles; ty++) {
            for (int tx=0; tx<numXTiles; tx++) {
                final Raster tile = image.getTile(minTileX + tx, minTileY + ty);
                assertNotNull(tile);
                Rectangle area = tile.getBounds();
                assertEquals(tileWidth,  area.width);
                assertEquals(tileHeight, area.height);
                assertEquals(bounds.x + tx * tileWidth,  area.x);
                assertEquals(bounds.y + ty * tileHeight, area.y);
                area = area.intersection(bounds);
                final int xmax = area.x + area.width;
                final int ymax = area.y + area.height;
                for (int y = area.y; y < ymax; y++) {
                    for (int x = area.x; x < xmax; x++) {
                        final int flatIndex = (y - bounds.y) * bounds.width + (x - bounds.x);
                        final float expect = expected[flatIndex];
                        final float actual = tile.getSampleFloat(x, y, band);
                        if (Float.floatToIntBits(expect) != Float.floatToIntBits(actual)) {
                            fail(mismatchedSampleValue(x, y, tx, ty, band, expect, actual));
                        }
                    }
                }
            }
        }
    }

    /**
     * Tests the aggregation of a single source image. The source image is reduced by a factor 2
     * in both directions, which implies that each target pixel is the average of 4 source pixels.
     * The source image has 2×2 tiles of 4×3 pixels, which implies that some averages are computed
     * with pixels from two different tiles. The source image values are:
     *
     * <pre class="text">
     *   1100 1101 1102 1103 │ 1200 1201 1202 1203
     *   1110 1111 1112 1113 │ 1210 1211 1212 1213
     *   1120 1121 1122 1123 │ 1220 1221 1222 1223
     *   ────────────────────┼────────────────────
     *   1300 1301 1302 1303 │ 1400 1401 1402 1403
     *   1310 1311 1312 1313 │ 1410 1411 1412 1413
     *   1320 1321 1322 1323 │ 1420 1421 1422 1423</pre>
     *
     * @throws TransformException if an error occurred while computing the bounds of the aggregated image.
     */
    @Test
    public void testSingleSource() throws TransformException {
        tileWidth  =  3;
        tileHeight =  2;
        minTileX   = -1;
        minTileY   =  7;
        numBands   =  1;
        createAggregatedImage(
                source(1, 1, -4, 6, 8, 6, 4, 3, 5, -2, false, false, 0.5, 0.5, 12, -10));

        bounds = new Rectangle(10, -7, 4, 3);       // Expected result of automatic computation.
        verify(0,   // Number of the band to verify.
                1105.5f, 1107.5f, 1205.5f, 1207.5f,
                1210.5f, 1212.5f, 1310.5f, 1312.5f,
                1315.5f, 1317.5f, 1415.5f, 1417.5f);
    }

    /**
     * Tests the aggregation of three source images. The aggregated image has explicit bounds which
     * are (in pixel coordinates of the aggregated image) from (-2, 3) inclusive to (8, 10) exclusive.
     * The sources are:
     *
     * <ol>
     *   <li>An image of 6×4 pixels with 2×2 tiles of 3×2 pixels,
     *       with upper-left corner translated from (1, -5) to (-3, 4).
     *       The first column is outside the aggregated image bounds.</li>
     *   <li>An image of 4×6 pixels with 2×2 tiles of 2×3 pixels,
     *       with upper-left corner translated from (-6, 10) to (0, 4).</li>
     *   <li>An image of 3×2 pixels with 1×2 tiles of 3×1 pixels, scaled by a factor 2 and
     *       with upper-left corner translated from (3, 2) to (2, 3).
     *       Each source pixel covers 2×2 pixels in the aggregated image.</li>
     * </ol>
     *
     * Pixels where no source contribute have the NaN value.
     * Pixels where many sources contribute have the average value of all contributions.
     * For example, the value at (2, 4) is the average of 1202 (first source),
     * 2200 (second source) and 3100 (third source).
     *
     * @throws TransformException if an error occurred while computing the bounds of the aggregated image.
     */
    @Test
    public void testThreeSources() throws TransformException {
        tileWidth  =  4;
        tileHeight =  3;
        minTileX   =  3;
        minTileY   = -4;
        numBands   =  1;
        bounds = new Rectangle(-2, 3, 10, 7);
        createAggregatedImage(
                source(1, 1,  1, -5, 6, 4, 3, 2, -2, 4, false, false, 1, 1, -4,  9),
                source(2, 1, -6, 10, 4, 6, 2, 3,  0, 7, true,  false, 1, 1,  6, -6),
                source(3, 1,  3,  2, 3, 2, 3, 1,  1, 1, false, false, 2, 2, -4, -1));

        verify(0,   // Number of the band to verify.
                 NaN,  NaN,  NaN,  NaN, 3100,       3100,    3101, 3101, 3102, 3102,
                1101, 1102, 1650, 1651, 2167.3333f, 2650.5f, 3101, 3101, 3102, 3102,
                1111, 1112, 1660, 1661, 2207.3333f, 2705.5f, 3201, 3201, 3202, 3202,
                1301, 1302, 1760, 1761, 2274f,      2710.5f, 3201, 3201, 3202, 3202,
                1311, 1312, 1855, 1856, 1906,       2401,     NaN,  NaN,  NaN,  NaN,
                 NaN,  NaN, 2310, 2311, 2410,       2411,     NaN,  NaN,  NaN,  NaN,
                 NaN,  NaN, 2320, 2321, 2420,       2421,     NaN,  NaN,  NaN,  NaN);
    }

    /**
     * Tests the aggregation of two source images having different numbers of bands.
     * The aggregated image bounds are computed automatically. The sources are:
     *
     * <ol>
     *   <li>An image of 4×4 pixels in 2 bands with 2×2 tiles of 2×2 pixels,
     *       with upper-left corner translated from (2, -3) to (-3, 1).</li>
     *   <li>An image of 6×3 pixels in 3 bands with 3×1 tiles of 2×3 pixels, scaled by a factor ½
     *       on the <var>x</var> axis and with upper-left corner translated from (-1, 0) to (-1, 3).
     *       Each target pixel covers 2 source pixels, each one with a weight of ½.</li>
     * </ol>
     *
     * The aggregated image has 3 bands, as the source having the largest number of bands.
     * Values in the third band are provided only by the second source.
     * Sample values in the first source are 1TYX in the first band and 2TYX in the second band.
     * Sample values in the second source are 3TYX, 4TYX and 5TYX in the three bands.
     * For example, the value at (-1, 3) in the first band is the weighted average
     * of 1400 (weight 1), 3100 (weight ½) and 3101 (weight ½), which is 2250.25.
     *
     * @throws TransformException if an error occurred while computing the bounds of the aggregated image.
     */
    @Test
    public void testDifferentNumberOfBands() throws TransformException {
        tileWidth  =  3;
        tileHeight =  4;
        minTileX   = -5;
        minTileY   =  2;
        numBands   =  3;
        createAggregatedImage(
                source(1, 2,  2, -3, 4, 4, 2, 2,  6, -1, false, false, 1,   1, -5,   4),
                source(3, 3, -1,  0, 6, 3, 2, 3, -4,  9, true,  false, 0.5, 1, -0.5, 3));

        bounds = new Rectangle(-3, 1, 5, 5);        // Expected result of automatic computation.
        verify(0,   // Number of the band to verify.
                1100, 1101, 1200,     1201,     NaN,
                1110, 1111, 1210,     1211,     NaN,
                1300, 1301, 2250.25f, 2300.75f, 3300.5f,
                1310, 1311, 2260.25f, 2310.75f, 3310.5f,
                 NaN,  NaN, 3120.5f,  3220.5f,  3320.5f);

        verify(1,   // Number of the band to verify.
                2100, 2101, 2200,     2201,     NaN,
                2110, 2111, 2210,     2211,     NaN,
                2300, 2301, 3250.25f, 3300.75f, 4300.5f,
                2310, 2311, 3260.25f, 3310.75f, 4310.5f,
                 NaN,  NaN, 4120.5f,  4220.5f,  4320.5f);
        /*
         * The third band exists only in the second source. Pixels covered only by
         * the first source have the fill value, which is NaN in this test.
         */
        verify(2,   // Number of the band to verify.
                NaN, NaN, NaN,     NaN,     NaN,
                NaN, NaN, NaN,     NaN,     NaN,
                NaN, NaN, 5100.5f, 5200.5f, 5300.5f,
                NaN, NaN, 5110.5f, 5210.5f, 5310.5f,
                NaN, NaN, 5120.5f, 5220.5f, 5320.5f);
    }

    /**
     * Tests that the aggregated image is recomputed after a change in a source image.
     * The source image is translated without scale, so the aggregated image values
     * are initially the same as the source image values.
     *
     * @throws TransformException if an error occurred while computing the bounds of the aggregated image.
     */
    @Test
    public void testRecompute() throws TransformException {
        tileWidth  =  5;
        tileHeight =  3;
        minTileX   =  4;
        minTileY   = -1;
        numBands   =  1;
        final AggregatedImage.Source entry = source(1, 1, 7, -3, 6, 4, 2, 2, -3, 2, true, true, 1, 1, -9, 5);
        final WritableTiledImage source = assertInstanceOf(WritableTiledImage.class, entry.source);
        createAggregatedImage(entry);

        bounds = new Rectangle(-2, 2, 6, 4);        // Expected result of automatic computation.
        verify(0,   // Number of the band to verify
                1100, 1101, 1200, 1201, 1300, 1301,
                1110, 1111, 1210, 1211, 1310, 1311,
                1400, 1401, 1500, 1501, 1600, 1601,
                1410, 1411, 1510, 1511, 1610, 1611);
        /*
         * Modify the pixel at (10, -2) in the second tile and the pixel at (11, 0) in the sixth tile.
         * The tile observers are notified when the writable tiles are released.
         */
        WritableRaster tile = source.getWritableTile(-2, 2);
        tile.setSample(10, -2, 0, 5000);
        source.releaseWritableTile(-2, 2);

        tile = source.getWritableTile(-1, 3);
        tile.setSample(11, 0, 0, 6000);
        source.releaseWritableTile(-1, 3);

        verify(0,   // Number of the band to verify
                1100, 1101, 1200, 1201, 1300, 1301,
                1110, 1111, 1210, 5000, 1310, 1311,
                1400, 1401, 1500, 1501, 1600, 1601,
                1410, 1411, 1510, 1511, 6000, 1611);
    }
}
