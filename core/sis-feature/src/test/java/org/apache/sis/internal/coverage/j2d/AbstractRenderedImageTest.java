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
package org.apache.sis.internal.coverage.j2d;

import java.util.Random;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.FeatureAssert.assertValuesEqual;


/**
 * Tests {@link AbstractRenderedImage}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
public final strictfp class AbstractRenderedImageTest extends TestCase {
    /**
     * Size of tiles in this test. The width should be different than the height
     * for increasing the chances to detect errors in index calculations.
     */
    private static final int TILE_WIDTH = 3, TILE_HEIGHT = 2;

    /**
     * Image size. Shall be multiple of tile width and height.
     */
    private static final int WIDTH  = TILE_WIDTH  * 4,
                             HEIGHT = TILE_HEIGHT * 3;

    /**
     * Random number generator for this test.
     */
    private final Random random;

    /**
     * Creates a new test.
     */
    public AbstractRenderedImageTest() {
        random = TestUtilities.createRandomNumberGenerator();
    }

    /**
     * A rendered image which can contain an arbitrary number of tiles. Tiles are stored in memory.
     * We use this class for testing purpose only because tiled images in production need a more
     * sophisticated implementation capable to store some tiles on disk (for memory consumption reasons).
     */
    private static final class TiledImage extends AbstractRenderedImage {
        /**
         * Index of the first tile in the image. Should be a non-trivial value
         * for increasing the chances to detect error in index calculation.
         */
        private final int minTileX, minTileY;

        /**
         * Location of the upper-left pixel of the image. Should be a non-trivial
         * value for increasing the chances to detect error in index calculation.
         */
        private final int minX, minY;

        /**
         * The tiles.
         */
        private final Raster[] tiles;

        /**
         * Creates a new tiled image.
         */
        TiledImage(final Random random) {
            minTileX = random.nextInt(20) - 10;
            minTileY = random.nextInt(20) - 10;
            minX     = random.nextInt(20) - 10;
            minY     = random.nextInt(20) - 10;
            final int numXTiles = getNumXTiles();
            final int numYTiles = getNumYTiles();
            tiles = new WritableRaster[numXTiles * numYTiles];
            int i = 0;
            for (int ty = 0; ty < numYTiles; ty++) {
                for (int tx = 0; tx < numXTiles; tx++) {
                    tiles[i] = createTile(tx * TILE_WIDTH  + minX,
                                          ty * TILE_HEIGHT + minY,
                                          ++i * 100);
                }
            }
            assertEquals(tiles.length, i);
        }

        /**
         * Creates a tile at the given location and with values starting at the given value.
         *
         * @param  x      column index of the upper-left pixel.
         * @param  y      row index of the upper-left pixel.
         * @param  value  value of the upper-left pixel.
         */
        private static WritableRaster createTile(final int x, final int y, final int value) {
            final WritableRaster raster = Raster.createBandedRaster(DataBuffer.TYPE_USHORT, TILE_WIDTH, TILE_HEIGHT, 1, new Point(x,y));
            for (int j=0; j<TILE_HEIGHT; j++) {
                for (int i=0; i<TILE_WIDTH; i++) {
                    raster.setSample(x+i, y+j, 0, value + 10*j + i);
                }
            }
            return raster;
        }

        /*
         * Size and tiling information.
         */
        @Override public int         getMinX()        {return minX;}
        @Override public int         getMinY()        {return minY;}
        @Override public int         getWidth()       {return WIDTH;}
        @Override public int         getHeight()      {return HEIGHT;}
        @Override public int         getTileWidth()   {return TILE_WIDTH;}
        @Override public int         getTileHeight()  {return TILE_HEIGHT;}
        @Override public int         getMinTileX()    {return minTileX;}
        @Override public int         getMinTileY()    {return minTileY;}
        @Override public ColorModel  getColorModel()  {return null;}
        @Override public SampleModel getSampleModel() {return tiles[0].getSampleModel();}

        /**
         * Returns the tile at the given location in tile coordinates.
         */
        @Override
        public Raster getTile(int tileX, int tileY) {
            final int numXTiles = getNumXTiles();
            final int numYTiles = getNumYTiles();
            assertTrue((tileX -= minTileX) >= 0 && tileX < numXTiles);
            assertTrue((tileY -= minTileY) >= 0 && tileY < numYTiles);
            return tiles[tileY * numXTiles + tileX];
        }
    }

    /**
     * Tests {@link AbstractRenderedImage#getData()} on a tiled image.
     */
    @Test
    public void testGetData() {
        final AbstractRenderedImage image = new TiledImage(random);
        assertValuesEqual(image.getData(), 0, new int[][] {
            { 100,  101,  102  ,   200,  201,  202  ,   300,  301,  302  ,   400,  401,  402},
            { 110,  111,  112  ,   210,  211,  212  ,   310,  311,  312  ,   410,  411,  412},
            { 500,  501,  502  ,   600,  601,  602  ,   700,  701,  702  ,   800,  801,  802},
            { 510,  511,  512  ,   610,  611,  612  ,   710,  711,  712  ,   810,  811,  812},
            { 900,  901,  902  ,  1000, 1001, 1002  ,  1100, 1101, 1102  ,  1200, 1201, 1202},
            { 910,  911,  912  ,  1010, 1011, 1012  ,  1110, 1111, 1112  ,  1210, 1211, 1212}
        });
    }

    /**
     * Tests {@link AbstractRenderedImage#getData(Rectangle)} on a tiled image.
     */
    @Test
    public void testGetDataRegion() {
        final AbstractRenderedImage image = new TiledImage(random);
        final Rectangle region = ImageUtilities.getBounds(image);
        region.x      += 4;     // Exclude 4 columns on left side.
        region.width  -= 6;     // Exclude 2 columns on right side.
        region.y      += 1;     // Exclude 1 row on top.
        region.height -= 3;     // Exclude 2 rows on bottom.
        assertValuesEqual(image.getData(region), 0, new int[][] {
            { 211,  212  ,   310,  311,  312  ,   410},
            { 601,  602  ,   700,  701,  702  ,   800},
            { 611,  612  ,   710,  711,  712  ,   810}
        });
    }
}
