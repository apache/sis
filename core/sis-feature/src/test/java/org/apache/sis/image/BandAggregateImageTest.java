package org.apache.sis.image;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRenderedImage;
import java.util.Arrays;
import java.util.function.IntBinaryOperator;
import java.util.stream.IntStream;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BandAggregateImageTest extends TestCase {

    @Test
    public void aggregateSingleBandImages() {
        BufferedImage im1 = new BufferedImage(3, 3, BufferedImage.TYPE_BYTE_GRAY);
        im1.getRaster().setSamples(0, 0, 3, 3, 0, IntStream.range(0, 3*3).map(it -> 1).toArray());
        BufferedImage im2 = new BufferedImage(3, 3, BufferedImage.TYPE_BYTE_GRAY);
        im2.getRaster().setSamples(0, 0, 3, 3, 0, IntStream.range(0, 3*3).map(it -> 2).toArray());

        final RenderedImage result = processor().aggregateBands(im1, im2);
        assertNotNull(result);

        final Raster tile = result.getTile(0, 0);
        assertEquals(2, tile.getNumBands());
        assertEquals(new Rectangle(0, 0, 3, 3), tile.getBounds());
        assertArrayEquals(
                new int[] {
                        1, 2,    1, 2,    1, 2,

                        1, 2,    1, 2,    1, 2,

                        1, 2,    1, 2,    1, 2
                },
                tile.getPixels(0, 0, 3, 3, (int[]) null)
        );
    }

    @Test
    public void aggregateSimilarTiledImages() {
        aggregateSimilarTiledImages(true, true);
        aggregateSimilarTiledImages(false, false);
        aggregateSimilarTiledImages(true, false);
        aggregateSimilarTiledImages(false, true);
    }
    
    @Test
    public void aggregateImagesUsingSameExtentButDifferentTileSizes() {
        // Note: we use different tile indices to test robustness. The aggregation algorithm should rely on pixel coordinates for absolute positioning and synchronisation of image domains.
        final TiledImageMock tiled2x2 = new TiledImageMock(DataBuffer.TYPE_FLOAT, 1, 3, 1, 8, 4, 2, 2, 1, 2, true);
        final TiledImageMock tiled4x1 = new TiledImageMock(DataBuffer.TYPE_FLOAT, 1, 3, 1, 8, 4, 4, 1, 3, 4, true);
        final TiledImageMock oneTile  = new TiledImageMock(DataBuffer.TYPE_FLOAT, 1, 3, 1, 8, 4, 8, 4, 5, 6, true);
        
        init(tiled2x2, tiled4x1, oneTile);
        
        final RenderedImage result = processor().aggregateBands(tiled2x2, tiled4x1, oneTile);
        assertNotNull(result);
        assertArrayEquals(new int[] { 3, 1, 8, 4, 2, 1, 1, 4 }, new int[] {
                result.getMinX(), result.getMinY(), result.getWidth(), result.getHeight(), result.getTileWidth(), result.getTileHeight(), result.getMinTileX(), result.getMinTileY()
        });

        final Raster raster = result.getData();
        assertEquals(new Rectangle(3, 1, 8, 4), raster.getBounds());
        assertArrayEquals(
                new int[] {
                    1100, 2100, 3100,  1101, 2101, 3101,    1200, 2102, 3102,  1201, 2103, 3103,    1300, 2200, 3104,  1301, 2201, 3105,    1400, 2202, 3106,  1401, 2203, 3107,
                    1110, 2300, 3110,  1111, 2301, 3111,    1210, 2302, 3112,  1211, 2303, 3113,    1310, 2400, 3114,  1311, 2401, 3115,    1410, 2402, 3116,  1411, 2403, 3117,
                    1500, 2500, 3120,  1501, 2501, 3121,    1600, 2502, 3122,  1601, 2503, 3123,    1700, 2600, 3124,  1701, 2601, 3125,    1800, 2602, 3126,  1801, 2603, 3127,
                    1510, 2700, 3130,  1511, 2701, 3131,    1610, 2702, 3132,  1611, 2703, 3133,    1710, 2800, 3134,  1711, 2801, 3135,    1810, 2802, 3136,  1811, 2803, 3137,
                },
                raster.getPixels(3, 1, 8, 4, (int[]) null)
        );
    }

    @Test
    public void aggregateImagesUsingDifferentExtentsAndDifferentSquaredTiling() {
        // Tip: band number match image tile width. i.e:
        // untiled image -> band 1
        // tiled 2x2 -> bands 2 and 3
        // tiled 4x4 -> bands 4 and 5
        // tiled 6x6 -> band 6
        final TiledImageMock untiled = new TiledImageMock(DataBuffer.TYPE_FLOAT, 1, 0, 0, 16, 13, 16, 13, 0, 0, true);
        final TiledImageMock tiled2x2 = new TiledImageMock(DataBuffer.TYPE_FLOAT, 2, 4, 2, 8, 10, 2, 2, 0, 0, true);
        final TiledImageMock tiled4x4 = new TiledImageMock(DataBuffer.TYPE_FLOAT, 2, 4, 2, 8, 8, 4, 4, 0, 0, true);
        final TiledImageMock tiled6x6 = new TiledImageMock(DataBuffer.TYPE_FLOAT, 1, 2, 0, 12, 6, 6, 6, 0, 0, true);

        init(untiled, tiled2x2, tiled4x4, tiled6x6);

        final RenderedImage result = processor().aggregateBands(untiled, tiled2x2, tiled4x4, tiled6x6);
        assertNotNull(result);
        assertArrayEquals(new int[] { 4, 2, 8, 4, 2, 2, 0, 0 }, new int[] {
                result.getMinX(), result.getMinY(), result.getWidth(), result.getHeight(), result.getTileWidth(), result.getTileHeight(), result.getMinTileX(), result.getMinTileY()
        });

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
    }

    /*

        @Test
        public void validateColorModel() {
            throw new UnsupportedOperationException("TODO");
        }

    */
    private void aggregateSimilarTiledImages(Boolean firstBanded, Boolean secondBanded) {
        final TiledImageMock im1 = new TiledImageMock(DataBuffer.TYPE_INT, 2, 7, 7, 6, 9, 3, 3, 1, 2, firstBanded);
        final TiledImageMock im2 = new TiledImageMock(DataBuffer.TYPE_INT, 2, 7, 7, 6, 9, 3, 3, 3, 4, secondBanded);

        init(im1, im2);

        final ImageProcessor processor = processor();
        RenderedImage result = processor.aggregateBands(im1, im2);
        assertNotNull(result);
        assertArrayEquals(new int[] { 7, 7, 6, 9, 3, 3, 1, 2 }, new int[] { result.getMinX(), result.getMinY(), result.getWidth(), result.getHeight(), result.getTileWidth(), result.getTileHeight(), result.getMinTileX(), result.getMinTileY()});
        Raster raster = result.getData();
        assertEquals(4, raster.getNumBands());
        assertEquals(new Rectangle(7, 7, 6, 9), raster.getBounds());

        assertArrayEquals(
                new int[] {
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
                },
                raster.getPixels(7, 7, 6, 9, (int[]) null)
        );

        // Repeat the test with a custom band selection.
        result = processor.aggregateBands(Arrays.asList(im1, im2, im1), Arrays.asList(null, new int[] { 1 }, new int[] { 0 }));
        assertNotNull(result);
        assertArrayEquals(new int[] { 7, 7, 6, 9, 3, 3, 1, 2 }, new int[] { result.getMinX(), result.getMinY(), result.getWidth(), result.getHeight(), result.getTileWidth(), result.getTileHeight(), result.getMinTileX(), result.getMinTileY()});
        raster = result.getData();
        assertEquals(4, raster.getNumBands());
        assertEquals(new Rectangle(7, 7, 6, 9), raster.getBounds());

        assertArrayEquals(
                new int[] {
                        // Tile 1                                                                    Tile 2
                        1100, 2100, 4100, 1100,  1101, 2101, 4101, 1101,  1102, 2102, 4102, 1102,    1200, 2200, 4200, 1200,  1201, 2201, 4201, 1201,  1202, 2202, 4202, 1202,
                        1110, 2110, 4110, 1110,  1111, 2111, 4111, 1111,  1112, 2112, 4112, 1112,    1210, 2210, 4210, 1210,  1211, 2211, 4211, 1211,  1212, 2212, 4212, 1212,
                        1120, 2120, 4120, 1120,  1121, 2121, 4121, 1121,  1122, 2122, 4122, 1122,    1220, 2220, 4220, 1220,  1221, 2221, 4221, 1221,  1222, 2222, 4222, 1222,
                        // Tile 3                                                                    Tile 4
                        1300, 2300, 4300, 1300,  1301, 2301, 4301, 1301,  1302, 2302, 4302, 1302,    1400, 2400, 4400, 1400,  1401, 2401, 4401, 1401,  1402, 2402, 4402, 1402,
                        1310, 2310, 4310, 1310,  1311, 2311, 4311, 1311,  1312, 2312, 4312, 1312,    1410, 2410, 4410, 1410,  1411, 2411, 4411, 1411,  1412, 2412, 4412, 1412,
                        1320, 2320, 4320, 1320,  1321, 2321, 4321, 1321,  1322, 2322, 4322, 1322,    1420, 2420, 4420, 1420,  1421, 2421, 4421, 1421,  1422, 2422, 4422, 1422,
                        // Tile 5                                                                    Tile 6
                        1500, 2500, 4500, 1500,  1501, 2501, 4501, 1501,  1502, 2502, 4502, 1502,    1600, 2600, 4600, 1600,  1601, 2601, 4601, 1601,  1602, 2602, 4602, 1602,
                        1510, 2510, 4510, 1510,  1511, 2511, 4511, 1511,  1512, 2512, 4512, 1512,    1610, 2610, 4610, 1610,  1611, 2611, 4611, 1611,  1612, 2612, 4612, 1612,
                        1520, 2520, 4520, 1520,  1521, 2521, 4521, 1521,  1522, 2522, 4522, 1522,    1620, 2620, 4620, 1620,  1621, 2621, 4621, 1621,  1622, 2622, 4622, 1622
                },
                raster.getPixels(7, 7, 6, 9, (int[]) null)
        );
    }

    private ImageProcessor processor() { return new ImageProcessor(); }

    /**
     * Initialize all bands of all input images with a "BTYX" pattern where:
     * <ol>
     *     <li>
     *         "B" is the band index over all encountered images.
     *         It means that be start at 1 for the first band of first encountered image,
     *         and then it is incremented for each band of each encountered image
     *     </li>
     *     <li>"TYX" is defined by {@link TiledImageMock#initializeAllTiles(int...)}</li>
     * </ol>
     */
    private void init(TiledImageMock... images) {
        int b = 1;
        for (TiledImageMock image : images) {
            int[] allBands = IntStream.range(0, image.getSampleModel().getNumBands()).toArray();
            image.initializeAllTiles(allBands);
            final int cursor = b;
            updateValues(image, (band, sample) -> (cursor + band) * 1000 + sample);
            b += allBands.length;
        }
    }

    /**
     * Change pixel values of input image with provided binary operator.
     *
     * @param target The image to update (mutated directly)
     * @param valueUpdate Operator updating current sample value.
     *                    It receives as input the current band index (0-based) and the value associated to this band on the current pixel (the current sample value).
     *                    It must return the new value to associate to the pixel.
     */
    private void updateValues(WritableRenderedImage target, IntBinaryOperator valueUpdate) {
        try (WritablePixelIterator it = new PixelIterator.Builder().createWritable(target)) {
            int[] pixel = new int[it.getNumBands()];
            while (it.next()) {
                it.getPixel(pixel);
                for (int i = 0; i < pixel.length; i++) pixel[i] = valueUpdate.applyAsInt(i, pixel[i]);
                it.setPixel(pixel);
            }
        }
    }
}

