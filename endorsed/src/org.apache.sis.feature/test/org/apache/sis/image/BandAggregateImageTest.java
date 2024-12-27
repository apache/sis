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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.stream.IntStream;
import java.util.function.ObjIntConsumer;
import java.awt.Rectangle;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import org.apache.sis.image.privy.ImageUtilities;
import org.apache.sis.image.privy.RasterFactory;
import org.apache.sis.util.ArraysExt;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link BandAggregateImage}.
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class BandAggregateImageTest extends TestCase {
    /**
     * Whether to test write operations.
     */
    private static final boolean WRITABLE = true;

    /**
     * Source images used for building the band aggregate image.
     */
    private RenderedImage[] sourceImages;

    /**
     * Whether to allow the sharing of data arrays.
     * If {@code false}, tests will force copies.
     */
    private boolean allowSharing;

    /**
     * Creates a new test case.
     */
    public BandAggregateImageTest() {
        allowSharing = true;            // This is the default mode of `ImageProcessor`.
    }

    /**
     * Creates the band aggregate instance to test using current value of {@link #sourceImages}.
     */
    private RenderedImage createBandAggregate() {
        return BandAggregateImage.create(sourceImages, null, null, false, allowSharing, false);
    }

    /**
     * Tests the aggregation of two untiled images with forced copy of sample values.
     * This is the simplest case in this test class.
     */
    @Test
    public void testForcedCopy() {
        allowSharing = false;
        testUntiledImages();
    }

    /**
     * Tests the aggregation of two untiled images having the same bounds and only one band.
     * Sample values should not be copied unless forced to.
     */
    @Test
    public void testUntiledImages() {
        final int width  = 3;
        final int height = 4;
        final var im1 = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        final var im2 = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        im1.getRaster().setSamples(0, 0, width, height, 0, IntStream.range(0, width*height).map(s -> s + 1).toArray());
        im2.getRaster().setSamples(0, 0, width, height, 0, IntStream.range(0, width*height).map(s -> s * 2).toArray());
        sourceImages = new RenderedImage[] {im1, im2};

        final RenderedImage result = createBandAggregate();
        assertNotNull(result);
        assertEquals(0, result.getMinTileX());
        assertEquals(0, result.getMinTileY());
        assertEquals(1, result.getNumXTiles());
        assertEquals(1, result.getNumYTiles());

        final Raster tile = result.getTile(0, 0);
        assertEquals(2, tile.getNumBands());
        assertEquals(new Rectangle(0, 0, width, height), tile.getBounds());
        assertArrayEquals(
            new int[] {
                 1,  0,     2,  2,     3,  4,
                 4,  6,     5,  8,     6, 10,
                 7, 12,     8, 14,     9, 16,
                10, 18,    11, 20,    12, 22
            },
            tile.getPixels(0, 0, width, height, (int[]) null)
        );
        verifySharing(result, allowSharing, allowSharing);
        /*
         * Try writing two values, then check again.
         */
        if (WRITABLE) {
            final int tileX = 0;
            final int tileY = 0;
            final var writable = assertInstanceOf(WritableRenderedImage.class, result);
            final WritableRaster target = writable.getWritableTile(tileX, tileY);
            assertSame(tile, target);
            target.setPixel(2, 1, new int[] {100, 80});
            target.setPixel(1, 3, new int[] { 60, 40});
            writable.releaseWritableTile(tileX, tileY);
            assertSame(target, result.getTile(tileX, tileY));
            assertArrayEquals(
                new int[] {
                     1,  0,     2,  2,     3,  4,
                     4,  6,     5,  8,   100, 80,
                     7, 12,     8, 14,     9, 16,
                    10, 18,    60, 40,    12, 22
                },
                tile.getPixels(0, 0, width, height, (int[]) null)
            );
            assertEquals(100, im1.getRaster().getSample(2, 1, 0));
            assertEquals( 80, im2.getRaster().getSample(2, 1, 0));
            assertEquals( 60, im1.getRaster().getSample(1, 3, 0));
            assertEquals( 40, im2.getRaster().getSample(1, 3, 0));
        }
    }

    /**
     * Tests the aggregation of two tiled images having the same tile matrix.
     * The same test is executed many times with different but equivalent classes of sample models.
     * Bands may be copied or referenced, depending on the sample models.
     */
    @Test
    public void testSimilarlyTiledImages() {
        do {
            testSimilarlyTiledImages(true,  true,  false);
            testSimilarlyTiledImages(false, false, false);
            testSimilarlyTiledImages(true,  false, false);
            testSimilarlyTiledImages(false, true,  false);
        } while ((allowSharing = !allowSharing) == false);      // Loop executed exactly twice.
    }

    /**
     * Tests write operations in the aggregation of two tiled images having the same tile matrix.
     */
    @Test
    public void testWriteOperation() {
        testSimilarlyTiledImages(true, true, WRITABLE);
        // Other modes are not supported by `TiledImageMock`.
    }

    /**
     * Implementation of {@link #aggregateSimilarlyTiledImages()} with sample model classes
     * specified by the boolean arguments.
     *
     * @param firstBanded   whether to use {@code BandedSampleModel} for the first image.
     * @param secondBanded  whether to use {@code BandedSampleModel} for the second image.
     * @param testWrite     whether to test write operation.
     */
    private void testSimilarlyTiledImages(final boolean firstBanded, final boolean secondBanded, final boolean testWrite) {
        final int minX   =  7;
        final int minY   = -5;
        final int width  =  6;
        final int height =  9;
        final var im1 = new TiledImageMock(DataBuffer.TYPE_USHORT, 2, minX, minY, width, height, 3, 3, 1, 2, firstBanded);
        final var im2 = new TiledImageMock(DataBuffer.TYPE_USHORT, 2, minX, minY, width, height, 3, 3, 3, 4, secondBanded);
        initializeAllTiles(im1, im2);

        RenderedImage result = createBandAggregate();
        assertNotNull(result);
        assertEquals(minX,   result.getMinX());
        assertEquals(minY,   result.getMinY());
        assertEquals(width,  result.getWidth());
        assertEquals(height, result.getHeight());
        assertEquals(3,      result.getTileWidth());
        assertEquals(3,      result.getTileHeight());
        assertEquals(1,      result.getMinTileX());
        assertEquals(2,      result.getMinTileY());
        assertEquals(2,      result.getNumXTiles());
        assertEquals(3,      result.getNumYTiles());
        assertEquals(4,      result.getSampleModel().getNumBands());
        final int[] expected = {
            // Tile 1                                                                    Tile 2
            1100, 2100, 3100, 4100,  1101, 2101, 3101, 4101,  1102, 2102, 3102, 4102,    1200, 2200, 3200, 4200,  1201, 2201, 3201, 4201,  1202, 2202, 3202, 4202,
            1110, 2110, 3110, 4110,  1111, 2111, 3111, 4111,  1112, 2112, 3112, 4112,    1210, 2210, 3210, 4210,  1211, 2211, 3211, 4211,  1212, 2212, 3212, 4212,
            1120, 2120, 3120, 4120,  1121, 2121, 3121, 4121,  1122, 2122, 3122, 4122,    1220, 2220, 3220, 4220,  1221, 2221, 3221, 4221,  1222, 2222, 3222, 4222,
            // Tile 3                                                                    Tile 4
            1300, 2300, 3300, 4300,  1301, 2301, 3301, 4301,  1302, 2302, 3302, 4302,    1400, 2400, 3400, 4400,  1401, 2401, 3401, 4401,  1402, 2402, 3402, 4402,
            1310, 2310, 3310, 4310,  1311, 2311, 3311, 4311,  1312, 2312, 3312, 4312,    1410, 2410, 3410, 4410,  1411, 2411, 3411, 4411,  1412, 2412, 3412, 4412,
            1320, 2320, 3320, 4320,  1321, 2321, 3321, 4321,  1322, 2322, 3322, 4322,    1420, 2420, 3420, 4420,  1421, 2421, 3421, 4421,  1422, 2422, 3422, 4422,
            // Tile 5                                                                    Tile 6
            1500, 2500, 3500, 4500,  1501, 2501, 3501, 4501,  1502, 2502, 3502, 4502,    1600, 2600, 3600, 4600,  1601, 2601, 3601, 4601,  1602, 2602, 3602, 4602,
            1510, 2510, 3510, 4510,  1511, 2511, 3511, 4511,  1512, 2512, 3512, 4512,    1610, 2610, 3610, 4610,  1611, 2611, 3611, 4611,  1612, 2612, 3612, 4612,
            1520, 2520, 3520, 4520,  1521, 2521, 3521, 4521,  1522, 2522, 3522, 4522,    1620, 2620, 3620, 4620,  1621, 2621, 3621, 4621,  1622, 2622, 3622, 4622
        };
        Raster raster = result.getData();
        assertEquals(4, raster.getNumBands());
        assertEquals(new Rectangle(minX, minY, width, height), raster.getBounds());
        assertArrayEquals(expected, raster.getPixels(minX, minY, width, height, (int[]) null));
        verifySharing(result, allowSharing(4, im1, im2));
        /*
         * Try writing two values, then check again.
         * The modified tile is labeled "Tile 4" above.
         */
        if (testWrite) {
            final int tileX = 2;        // minTileX = 1
            final int tileY = 3;        // minTileY = 2
            final var writable = assertInstanceOf(WritableRenderedImage.class, result);
            final WritableRaster target = writable.getWritableTile(tileX, tileY);
            target.setPixel(10, -2, new int[] {100,  80,  20,  30});        // Upper left corner of tile 4
            target.setPixel(12, -1, new int[] {200, 240, 260, 250});
            writable.releaseWritableTile(tileX, tileY);
            assertEquals(1400, expected[ 84]);              // For verifying that we are at the correct location.
            assertEquals(1412, expected[116]);
            expected[ 84] = 100;
            expected[ 85] =  80;
            expected[ 86] =  20;
            expected[ 87] =  30;
            expected[116] = 200;
            expected[117] = 240;
            expected[118] = 260;
            expected[119] = 250;
            assertSame(target, result.getTile(tileX, tileY));
            assertArrayEquals(expected, result.getData().getPixels(minX, minY, width, height, (int[]) null));
            return;             // Cannot continue the tests because the source images have been modified.
        }
        /*
         * Repeat the test with a custom band selection.
         * One of the source images is used twice, but with a different selection of bands.
         */
        sourceImages = new RenderedImage[] {im1, im2, im1};
        result = BandAggregateImage.create(sourceImages, new int[][] {
            new int[] {1},      // Take second band of image 1.
            null,               // Take all bands of image 2.
            new int[] {0}       // Take first band of image 1.
        }, null, false, allowSharing, false);
        assertNotNull(result);
        assertEquals(minX,   result.getMinX());
        assertEquals(minY,   result.getMinY());
        assertEquals(width,  result.getWidth());
        assertEquals(height, result.getHeight());
        assertEquals(3,      result.getTileWidth());
        assertEquals(3,      result.getTileHeight());
        assertEquals(1,      result.getMinTileX());
        assertEquals(2,      result.getMinTileY());
        assertEquals(2,      result.getNumXTiles());
        assertEquals(3,      result.getNumYTiles());
        assertEquals(4,      result.getSampleModel().getNumBands());

        raster = result.getData();
        assertEquals(4, raster.getNumBands());
        assertEquals(new Rectangle(minX, minY, width, height), raster.getBounds());
        assertArrayEquals(new int[] {
             // Tile 1 ═════════════════╤═════════════════════════╤═══════════════════════     Tile 2 ═════════════════╤═════════════════════════╤════════════════════════
                2100, 3100, 4100, 1100,   2101, 3101, 4101, 1101,   2102, 3102, 4102, 1102,    2200, 3200, 4200, 1200,   2201, 3201, 4201, 1201,   2202, 3202, 4202, 1202,
                2110, 3110, 4110, 1110,   2111, 3111, 4111, 1111,   2112, 3112, 4112, 1112,    2210, 3210, 4210, 1210,   2211, 3211, 4211, 1211,   2212, 3212, 4212, 1212,
                2120, 3120, 4120, 1120,   2121, 3121, 4121, 1121,   2122, 3122, 4122, 1122,    2220, 3220, 4220, 1220,   2221, 3221, 4221, 1221,   2222, 3222, 4222, 1222,
             // Tile 3 ═════════════════╪═════════════════════════╪═══════════════════════     Tile 4 ═════════════════╪═════════════════════════╪════════════════════════
                2300, 3300, 4300, 1300,   2301, 3301, 4301, 1301,   2302, 3302, 4302, 1302,    2400, 3400, 4400, 1400,   2401, 3401, 4401, 1401,   2402, 3402, 4402, 1402,
                2310, 3310, 4310, 1310,   2311, 3311, 4311, 1311,   2312, 3312, 4312, 1312,    2410, 3410, 4410, 1410,   2411, 3411, 4411, 1411,   2412, 3412, 4412, 1412,
                2320, 3320, 4320, 1320,   2321, 3321, 4321, 1321,   2322, 3322, 4322, 1322,    2420, 3420, 4420, 1420,   2421, 3421, 4421, 1421,   2422, 3422, 4422, 1422,
             // Tile 5 ═════════════════╪═════════════════════════╪═══════════════════════     Tile 6 ═════════════════╪═════════════════════════╪════════════════════════
                2500, 3500, 4500, 1500,   2501, 3501, 4501, 1501,   2502, 3502, 4502, 1502,    2600, 3600, 4600, 1600,   2601, 3601, 4601, 1601,   2602, 3602, 4602, 1602,
                2510, 3510, 4510, 1510,   2511, 3511, 4511, 1511,   2512, 3512, 4512, 1512,    2610, 3610, 4610, 1610,   2611, 3611, 4611, 1611,   2612, 3612, 4612, 1612,
                2520, 3520, 4520, 1520,   2521, 3521, 4521, 1521,   2522, 3522, 4522, 1522,    2620, 3620, 4620, 1620,   2621, 3621, 4621, 1621,   2622, 3622, 4622, 1622
            },
            raster.getPixels(minX, minY, width, height, (int[]) null)
        );
        /*
         * Do not invoke `verifySharing(result)` because this test
         * references the same `DataBuffer` more than once.
         */
    }

    /**
     * Tests the aggregation of three tiled images having different tile matrices.
     * A copy of sample values cannot be avoided in this case.
     */
    @Test
    public void testImagesUsingSameExtentButDifferentTileSizes() {
        final int minX   = 3;
        final int minY   = 1;
        final int width  = 8;
        final int height = 4;
        /*
         * Note: we use different tile indices to test robustness.
         * The aggregation algorithm should rely on pixel coordinates
         * for absolute positioning and alignment of image domains.
         */
        final var tiled2x2 = new TiledImageMock(DataBuffer.TYPE_FLOAT, 1, minX, minY, width, height, 2, 2, 1, 2, true);
        final var tiled4x1 = new TiledImageMock(DataBuffer.TYPE_FLOAT, 1, minX, minY, width, height, 4, 1, 3, 4, true);
        final var oneTile  = new TiledImageMock(DataBuffer.TYPE_FLOAT, 1, minX, minY, width, height, 8, 4, 5, 6, true);
        initializeAllTiles(tiled2x2, tiled4x1, oneTile);

        final RenderedImage result = createBandAggregate();
        assertNotNull(result);
        assertEquals(minX,   result.getMinX());
        assertEquals(minY,   result.getMinY());
        assertEquals(width,  result.getWidth());
        assertEquals(height, result.getHeight());
        assertEquals(2,      result.getTileWidth());
        assertEquals(1,      result.getTileHeight());
        assertEquals(1,      result.getMinTileX());
        assertEquals(4,      result.getMinTileY());
        assertEquals(4,      result.getNumXTiles());
        assertEquals(4,      result.getNumYTiles());
        assertEquals(3,      result.getSampleModel().getNumBands());

        final Raster raster = result.getData();
        assertEquals(new Rectangle(minX, minY, width, height), raster.getBounds());
        assertArrayEquals(
            new int[] {
                1100, 2100, 3100,  1101, 2101, 3101,    1200, 2102, 3102,  1201, 2103, 3103,    1300, 2200, 3104,  1301, 2201, 3105,    1400, 2202, 3106,  1401, 2203, 3107,
                1110, 2300, 3110,  1111, 2301, 3111,    1210, 2302, 3112,  1211, 2303, 3113,    1310, 2400, 3114,  1311, 2401, 3115,    1410, 2402, 3116,  1411, 2403, 3117,
                1500, 2500, 3120,  1501, 2501, 3121,    1600, 2502, 3122,  1601, 2503, 3123,    1700, 2600, 3124,  1701, 2601, 3125,    1800, 2602, 3126,  1801, 2603, 3127,
                1510, 2700, 3130,  1511, 2701, 3131,    1610, 2702, 3132,  1611, 2703, 3133,    1710, 2800, 3134,  1711, 2801, 3135,    1810, 2802, 3136,  1811, 2803, 3137,
            },
            raster.getPixels(minX, minY, width, height, (int[]) null)
        );
        verifySharing(result, false, false, false);
    }

    /**
     * Tests the aggregation of three tiled images having different extents and different tile matrices.
     * A copy of sample values cannot be avoided in this case, except on the second image.
     */
    @Test
    public void testImagesUsingDifferentExtentsAndDifferentTiling() {
        testHeterogeneous(false);
    }

    /**
     * Tests {@link BandAggregateImage#prefetch(Rectangle)}.
     */
    @Test
    public void testPrefetch() {
        testHeterogeneous(true);
    }

    /**
     * Implementation of test methods using tiles of different extents and different tile matrices.
     *
     * @param  prefetch  whether to test prefetch operation.
     */
    private void testHeterogeneous(final boolean prefetch) {
        /*
         * Tip: band number match image tile width. i.e:
         *
         *   untiled    →  band 1
         *   tiled 2x2  →  bands 2 and 3        — reference to data arrays can be shared.
         *   tiled 4x4  →  bands 4 and 5
         *   tiled 6x6  →  band 6
         */
        final var untiled  = new TiledImageMock(DataBuffer.TYPE_SHORT, 1, 0, 0, 16, 13, 16, 13, 0, 0, true);
        final var tiled2x2 = new TiledImageMock(DataBuffer.TYPE_SHORT, 2, 4, 2,  8, 10,  2,  2, 0, 0, true);
        final var tiled4x4 = new TiledImageMock(DataBuffer.TYPE_SHORT, 2, 4, 2,  8,  8,  4,  4, 0, 0, true);
        final var tiled6x6 = new TiledImageMock(DataBuffer.TYPE_SHORT, 1, 2, 0, 12,  6,  6,  6, 0, 0, true);
        initializeAllTiles(untiled, tiled2x2, tiled4x4, tiled6x6);

        RenderedImage result = BandAggregateImage.create(sourceImages, null, null, false, allowSharing, prefetch);
        assertNotNull(result);
        assertEquals(4, result.getMinX());
        assertEquals(2, result.getMinY());
        assertEquals(8, result.getWidth());
        assertEquals(4, result.getHeight());
        assertEquals(2, result.getTileWidth());
        assertEquals(2, result.getTileHeight());
        assertEquals(0, result.getMinTileX());
        assertEquals(1, result.getMinTileY());
        assertEquals(4, result.getNumXTiles());
        assertEquals(2, result.getNumYTiles());
        assertEquals(6, result.getSampleModel().getNumBands());

        if (prefetch) {
            result = new ImageProcessor().prefetch(result, new Rectangle(4, 2, 8, 4));
        }
        final Raster raster = result.getData();
        assertEquals(new Rectangle(4, 2, 8, 4), raster.getBounds());
        assertArrayEquals(
            new int[] {
                1124, 2100, 3100, 4100, 5100, 6122,  1125, 2101, 3101, 4101, 5101, 6123,    1126, 2200, 3200, 4102, 5102, 6124,  1127, 2201, 3201, 4103, 5103, 6125,    1128, 2300, 3300, 4200, 5200, 6220,  1129, 2301, 3301, 4201, 5201, 6221,    1130, 2400, 3400, 4202, 5202, 6222,  1131, 2401, 3401, 4203, 5203, 6223,
                1134, 2110, 3110, 4110, 5110, 6132,  1135, 2111, 3111, 4111, 5111, 6133,    1136, 2210, 3210, 4112, 5112, 6134,  1137, 2211, 3211, 4113, 5113, 6135,    1138, 2310, 3310, 4210, 5210, 6230,  1139, 2311, 3311, 4211, 5211, 6231,    1140, 2410, 3410, 4212, 5212, 6232,  1141, 2411, 3411, 4213, 5213, 6233,

                1144, 2500, 3500, 4120, 5120, 6142,  1145, 2501, 3501, 4121, 5121, 6143,    1146, 2600, 3600, 4122, 5122, 6144,  1147, 2601, 3601, 4123, 5123, 6145,    1148, 2700, 3700, 4220, 5220, 6240,  1149, 2701, 3701, 4221, 5221, 6241,    1150, 2800, 3800, 4222, 5222, 6242,  1151, 2801, 3801, 4223, 5223, 6243,
                1154, 2510, 3510, 4130, 5130, 6152,  1155, 2511, 3511, 4131, 5131, 6153,    1156, 2610, 3610, 4132, 5132, 6154,  1157, 2611, 3611, 4133, 5133, 6155,    1158, 2710, 3710, 4230, 5230, 6250,  1159, 2711, 3711, 4231, 5231, 6251,    1160, 2810, 3810, 4232, 5232, 6252,  1161, 2811, 3811, 4233, 5233, 6253,
            },
            raster.getPixels(4, 2, 8, 4, (int[]) null)
        );
        if (!prefetch) {
            verifySharing(result, false, allowSharing, true, false, false, false);
        }
    }

    /**
     * Tests aggregation of aggregated images. The result should be a flattened view.
     * Opportunistically tests a "band select" operation after the aggregation.
     */
    @Test
    public void testNestedAggregation() {
        final int minX   =  7;
        final int minY   = -5;
        final int width  =  6;
        final int height =  4;
        final var im1 = new TiledImageMock(DataBuffer.TYPE_USHORT, 3, minX, minY, width, height, 3, 2, 1, 2, true);
        final var im2 = new TiledImageMock(DataBuffer.TYPE_USHORT, 1, minX, minY, width, height, 3, 2, 3, 4, true);
        final var im3 = new TiledImageMock(DataBuffer.TYPE_USHORT, 2, minX, minY, width, height, 3, 2, 2, 1, true);
        initializeAllTiles(im1, im2, im3);

        RenderedImage result;
        result = BandAggregateImage.create(new RenderedImage[] {im2, im3},    null, null, false, allowSharing, false);
        result = BandAggregateImage.create(new RenderedImage[] {im1, result}, null, null, false, allowSharing, false);
        assertArrayEquals(sourceImages, ((BandAggregateImage) result).getSourceArray());

        assertSame(im1, BandSelectImage.create(result, true, 0, 1, 2));
        assertSame(im2, BandSelectImage.create(result, true, 3));
        assertSame(im3, BandSelectImage.create(result, true, 4, 5));
    }

    /**
     * Initializes all bands of all input images to testing values.
     * The testing values are defined by a "BTYX" pattern where:
     * <ol>
     *   <li><var>B</var> is the band index over all encountered image. It starts at 1 for the first
     *     band of first encountered image, then incremented for each band of each encountered image.</li>
     *   <li><var>T</var> is the tile index starting with 1 for the first tile and increasing in a row-major fashion.</li>
     *   <li><var>Y</var> is the <var>y</var> coordinate (row 0-based index) of the sample value relative to current tile.</li>
     *   <li><var>X</var> is the <var>x</var> coordinate (column 0-based index) of the sample value relative to current tile.</li>
     * </ol>
     */
    @SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
    private void initializeAllTiles(final TiledImageMock... images) {
        sourceImages = images;
        int band = 0;
        for (final TiledImageMock image : images) {
            final int numBands = image.getSampleModel().getNumBands();
            image.initializeAllTiles(ArraysExt.range(0, numBands), band * 1000);
            band += numBands;
        }
    }

    /**
     * Returns {@code true} if the sample model used by the given sources makes possible to share
     * the internal data arrays. This method should be invoked for {@link TiledImageMock} having
     * more than 1 band, because their sample model is selected randomly.
     */
    private boolean[] allowSharing(final int numBands, final RenderedImage... sources) {
        final boolean[] sharingPerBand = new boolean[numBands];
        if (allowSharing) {
            int lower = 0;
            for (final RenderedImage source : sources) {
                final int upper = lower + ImageUtilities.getNumBands(source);
                if (source.getSampleModel() instanceof BandedSampleModel) {
                    Arrays.fill(sharingPerBand, lower, upper, true);
                }
                lower = upper;
            }
            assertEquals(numBands, lower);
        }
        return sharingPerBand;
    }

    /**
     * Verifies if the given image reuses the data arrays of all its source.
     *
     * @param  result   the result of band aggregation.
     * @param  sharingPerBand  whether the caller expects the result to share data arrays. One value per band.
     */
    private static void verifySharing(final RenderedImage result, final boolean... sharingPerBand) {
        assertEquals(ImageUtilities.getNumBands(result), sharingPerBand.length);
        final var arrays = new HashSet<Object>();
        for (final RenderedImage source : result.getSources()) {
            forAllDataArrays(source, (data, band) -> assertTrue(arrays.add(data), "Found two references to the same array."));
        }
        forAllDataArrays(result, (data, band) -> {
            final boolean sharing = sharingPerBand[band];
            assertEquals(sharing, arrays.remove(data),
                    sharing ? "Expected the target image to reference an existing array."
                            : "Expected only copies, no references to existing arrays.");
        });
        boolean sharing = true;
        for (int i=0; i < sharingPerBand.length; i++) {
            sharing &= sharingPerBand[i];
        }
        assertEquals(sharing, arrays.isEmpty());
    }

    /**
     * Performs the given action on data arrays for all bands of all tiles of the given image.
     *
     * @param  source  the image for which to get data arrays.
     * @param  action  the action to execute for each data arrays.
     */
    private static void forAllDataArrays(final RenderedImage source, final ObjIntConsumer<Object> action) {
        for (int x = source.getNumXTiles(); --x >= 0;) {
            final int tileX = source.getMinTileX() + x;
            for (int y = source.getNumYTiles(); --y >= 0;) {
                final int tileY = source.getMinTileY() + y;
                final DataBuffer buffer = source.getTile(tileX, tileY).getDataBuffer();
                for (int band = buffer.getNumBanks(); --band >= 0;) {
                    action.accept(RasterFactory.wrapAsBuffer(buffer, band).array(), band);
                }
            }
        }
    }

    /**
     * Verifies the aggregation of property values.
     */
    @Test
    public void testProperties() {
        final var p1 = new Hashtable<String,Object>();
        final var p2 = new Hashtable<String,Object>();
        assertNull(p1.put(PlanarImage.SAMPLE_RESOLUTIONS_KEY, new double[] {4, 1, 3, 7}));
        assertNull(p2.put(PlanarImage.SAMPLE_RESOLUTIONS_KEY, new double[] {2, 8, 5, 6}));
        final ColorModel cm = ColorModel.getRGBdefault();
        final WritableRaster raster = cm.createCompatibleWritableRaster(1, 1);
        final RenderedImage[] sources = {
            new BufferedImage(cm, raster, false, p1),
            new BufferedImage(cm, raster, false, p2)
        };
        RenderedImage result;
        result = BandAggregateImage.create(sources, null, null, false, allowSharing, false);
        assertArrayEquals(new String[] {PlanarImage.SAMPLE_RESOLUTIONS_KEY}, result.getPropertyNames());
        assertArrayEquals(new double[] {4, 1, 3, 7, 2, 8, 5, 6},
                (double[]) result.getProperty(PlanarImage.SAMPLE_RESOLUTIONS_KEY));
        /*
         * Same tests, but with a subset of the bands.
         * This part of the test depends on `BandSelectImage`.
         */
        sources[0] = BandSelectImage.create(sources[0], false, 0, 2);
        sources[1] = BandSelectImage.create(sources[1], false, 1, 3);
        result = BandAggregateImage.create(sources, null, null, false, allowSharing, false);
        assertArrayEquals(new String[] {PlanarImage.SAMPLE_RESOLUTIONS_KEY}, result.getPropertyNames());
        assertArrayEquals(new double[] {4, 3, 8, 6},
                (double[]) result.getProperty(PlanarImage.SAMPLE_RESOLUTIONS_KEY));
    }
}
