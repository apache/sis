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
package org.apache.sis.image.internal.shared;

import java.util.Random;
import java.awt.image.DataBuffer;
import java.awt.image.BufferedImage;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.image.TiledImageMock;
import static org.apache.sis.feature.Assertions.assertValuesEqual;


/**
 * Tests the {@link ReshapedImage} implementation.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ReshapedImageTest extends TestCase {
    /**
     * Size of tiles used in this test.
     */
    private static final int TILE_WIDTH = 3, TILE_HEIGHT = 2;

    /**
     * Expected coordinates of image upper-left corner. Default value is (0,0).
     */
    private int minX, minY;

    /**
     * Expected index of first tile. Default value is (0,0).
     */
    private int minTileX, minTileY;

    /**
     * Expected number of tiles. Shall be initialized by test method
     * before to invoke {@link #verifyLayout(ReshapedImage)}.
     */
    private int numXTiles, numYTiles;

    /**
     * Expected values of image size. Shall be initialized by test
     * method before to invoke {@link #verifyLayout(ReshapedImage)}.
     */
    private int width, height;

    /**
     * Expected values of tile grid offset. Shall be initialized by test
     * method before to invoke {@link #verifyLayout(ReshapedImage)}.
     */
    private int tileXOffset, tileYOffset;

    /**
     * Creates a new test case.
     */
    public ReshapedImageTest() {
    }

    /**
     * Tests wrapping a {@link BufferedImage}. This single case has only one tile
     * with pixel coordinates starting at (0,0).
     */
    @Test
    public void testSingleTile() {
        numXTiles = 1;
        numYTiles = 1;
        width     = TILE_WIDTH;
        height    = TILE_HEIGHT;
        final BufferedImage data = new BufferedImage(TILE_WIDTH, TILE_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
        data.getRaster().setSamples(0, 0, TILE_WIDTH, TILE_HEIGHT, 0, new int[] {
            1, 2, 3,
            4, 7, 6
        });
        /*
         * Tests with a request starting on the left and on top of data.
         * The requested size is larger than image size; constructor shall clamp.
         */
        tileXOffset = minX = 1;
        tileYOffset = minY = 2;
        verifySingleTile(new ReshapedImage(data, -1, -2, 4, 4));
        /*
         * Tests with a request inside the image. Constructor should expand to
         * an integer number of tiles, which is the whole image for this test.
         */
        tileXOffset = minX = -2;
        tileYOffset = minY = -1;
        verifySingleTile(new ReshapedImage(data, 2, 1, 1, 1));
    }

    /**
     * Verify an image created by {@link #testSingleTile()}.
     *
     * @param  image   the reshaped image.
     */
    private void verifySingleTile(final ReshapedImage image) {
        verifyLayout(image);
        assertValuesEqual(image.getData(), 0, new int[][] {
            {1, 2, 3},
            {4, 7, 6}
        });
    }

    /**
     * Verifies the image properties (size, number of tiles).
     *
     * @param  image  the image to verify.
     */
    private void verifyLayout(final ReshapedImage image) {
        assertNull(               image.verify());
        assertEquals(minX,        image.getMinX());
        assertEquals(minY,        image.getMinY());
        assertEquals(width,       image.getWidth());
        assertEquals(height,      image.getHeight());
        assertEquals(TILE_WIDTH,  image.getTileWidth());
        assertEquals(TILE_HEIGHT, image.getTileHeight());
        assertEquals(minTileX,    image.getMinTileX());
        assertEquals(minTileY,    image.getMinTileY());
        assertEquals(numXTiles,   image.getNumXTiles());
        assertEquals(numYTiles,   image.getNumYTiles());
        assertEquals(tileXOffset, image.getTileGridXOffset());
        assertEquals(tileYOffset, image.getTileGridYOffset());
    }

    /**
     * Tests wrapping a {@link TiledImageMock}.
     */
    @Test
    public void testMultiTiles() {
        final Random random = TestUtilities.createRandomNumberGenerator();
        final int dataMinX, dataMinY;
        dataMinX  = random.nextInt(20) - 10;
        dataMinY  = random.nextInt(20) - 10;
        minTileX  = random.nextInt(20) - 10;
        minTileY  = random.nextInt(20) - 10;
        numXTiles = 5;
        numYTiles = 4;
        width     = numXTiles * TILE_WIDTH;
        height    = numYTiles * TILE_HEIGHT;
        final var data = new TiledImageMock(DataBuffer.TYPE_USHORT, 1, dataMinX, dataMinY,
                width, height, TILE_WIDTH, TILE_HEIGHT, minTileX, minTileY,
                random.nextBoolean());  // Banded or interleaved sample model
        data.validate();
        data.initializeAllTiles(0);
        /*
         * Apply only a translation, keep all tiles.
         */
        tileXOffset = (minX =  7) - minTileX * TILE_WIDTH;
        tileYOffset = (minY = 13) - minTileY * TILE_HEIGHT;
        var image = new ReshapedImage(data, dataMinX - 7, dataMinY - 13, 100, 100);
        verifyLayout(image);
        assertValuesEqual(image.getData(), 0, new int[][] {
            { 100,  101,  102  ,   200,  201,  202  ,   300,  301,  302  ,   400,  401,  402  ,   500,  501,  502},
            { 110,  111,  112  ,   210,  211,  212  ,   310,  311,  312  ,   410,  411,  412  ,   510,  511,  512},
            { 600,  601,  602  ,   700,  701,  702  ,   800,  801,  802  ,   900,  901,  902  ,  1000, 1001, 1002},
            { 610,  611,  612  ,   710,  711,  712  ,   810,  811,  812  ,   910,  911,  912  ,  1010, 1011, 1012},
            {1100, 1101, 1102  ,  1200, 1201, 1202  ,  1300, 1301, 1302  ,  1400, 1401, 1402  ,  1500, 1501, 1502},
            {1110, 1111, 1112  ,  1210, 1211, 1212  ,  1310, 1311, 1312  ,  1410, 1411, 1412  ,  1510, 1511, 1512},
            {1600, 1601, 1602  ,  1700, 1701, 1702  ,  1800, 1801, 1802  ,  1900, 1901, 1902  ,  2000, 2001, 2002},
            {1610, 1611, 1612  ,  1710, 1711, 1712  ,  1810, 1811, 1812  ,  1910, 1911, 1912  ,  2010, 2011, 2012}
        });
        /*
         * Ask for a subregion of the image. The subregion starts at (5,3) and ends at (9,5) inclusive.
         * ReshapedImageTest shall expand to an integer number of tiles, which result in (3,2) - (11,5).
         * This is 3×2 tiles.
         */
        minTileX++;         // Skip one tile on the left.
        minTileY++;         // Skip one tile on the bottom.
        width       = (numXTiles = 3) * TILE_WIDTH;
        height      = (numYTiles = 2) * TILE_HEIGHT;
        tileXOffset = (minX = -2) - minTileX * TILE_WIDTH;
        tileYOffset = (minY = -1) - minTileY * TILE_HEIGHT;
        image = new ReshapedImage(data, dataMinX + 5, dataMinY + 3, dataMinX + 9, dataMinY + 5);
        verifyLayout(image);
        assertValuesEqual(image.getData(), 0, new int[][] {
            { 700,  701,  702  ,   800,  801,  802  ,   900,  901,  902},
            { 710,  711,  712  ,   810,  811,  812  ,   910,  911,  912},
            {1200, 1201, 1202  ,  1300, 1301, 1302  ,  1400, 1401, 1402},
            {1210, 1211, 1212  ,  1310, 1311, 1312  ,  1410, 1411, 1412}
        });
    }
}
