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
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ImagingOpException;
import java.util.function.Consumer;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertInstanceOf;
import static org.apache.sis.feature.Assertions.assertValuesEqual;


/**
 * Tests {@link ComputedImage}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 */
@DependsOn(PlanarImageTest.class)
public final class ComputedImageTest extends TestCase {
    /**
     * Size of tiles in this test. The width should be different than the height
     * for increasing the chances to detect errors in index calculations.
     */
    private static final int TILE_WIDTH = 3, TILE_HEIGHT = 2;

    /**
     * Tile indices used in some methods performing their tests on only one tile.
     */
    private static final int TILE_X = 1, TILE_Y = 2;

    /**
     * Additional code to invoke during {@link ComputedImage#computeTile(int, int, WritableRaster)} execution,
     * or {@code null} if none.
     */
    private Consumer<ComputedImage> onComputeTile;

    /**
     * Creates an image to test. The {@link ComputedImage} tiles are simply sub-regions of a {@link BufferedImage}.
     * If {@link #onComputeTile} is non-null, it will be invoked every time that a tile is computed.
     */
    private ComputedImage createImage() {
        final BufferedImage source = new BufferedImage(TILE_WIDTH * 2, TILE_HEIGHT * 4, BufferedImage.TYPE_USHORT_GRAY);
        final WritableRaster raster = source.getRaster();
        for (int y=raster.getHeight(); --y >= 0;) {
            for (int x=raster.getWidth(); --x >= 0;) {
                raster.setSample(x, y, 0, 10*y + x);
            }
        }
        return new ComputedImage(source.getSampleModel().createCompatibleSampleModel(TILE_WIDTH, TILE_HEIGHT), source) {
            @Override public ColorModel getColorModel() {return getSource(0).getColorModel();}
            @Override public int        getWidth()      {return getSource(0).getWidth();}
            @Override public int        getHeight()     {return getSource(0).getHeight();}
            @Override protected Raster  computeTile(final int tileX, final int tileY, WritableRaster previous) {
                final Consumer<ComputedImage> f = onComputeTile;
                if (f != null) f.accept(this);
                final int tw = getTileWidth();
                final int th = getTileHeight();
                return getSource(0).getData(new Rectangle(tileX * tw, tileY * th, tw, th));
            }
        };
    }

    /**
     * Verifies that tiles returned by {@link ComputedImage#getTile(int, int)} are cached.
     */
    @Test
    public void testTileCaching() {
        final ComputedImage image = createImage();
        assertNull(image.verify());
        Raster tile10 = image.getTile(1, 0);
        Raster tile02 = image.getTile(0, 2);
        assertNotSame(tile10, tile02);
        assertValuesEqual(tile10, 0, new int[][] {
            { 3,  4,  5},
            {13, 14, 15}
        });
        assertValuesEqual(tile02, 0, new int[][] {
            {40, 41, 42},
            {50, 51, 52}
        });
        /*
         * Verify that computed tiles are cached.
         */
        assertSame(tile10, image.getTile(1, 0));
        assertSame(tile02, image.getTile(0, 2));
        /*
         * Verify that call to ComputedImage.Cleaner.dispose() remove tiles from the cache.
         */
        image.dispose();
        assertNotSame(tile10, tile10 = image.getTile(1, 0));
        assertNotSame(tile02, tile02 = image.getTile(0, 2));
        assertSame   (tile10,          image.getTile(1, 0));    // New tiles are now cached.
        assertSame   (tile02,          image.getTile(0, 2));
        /*
         * Should be last because this test will trig the computation of all tiles.
         */
        assertValuesEqual(image.getData(), 0, new int[][] {
            { 0,  1,  2,  3,  4,  5},
            {10, 11, 12, 13, 14, 15},
            {20, 21, 22, 23, 24, 25},
            {30, 31, 32, 33, 34, 35},
            {40, 41, 42, 43, 44, 45},
            {50, 51, 52, 53, 54, 55},
            {60, 61, 62, 63, 64, 65},
            {70, 71, 72, 73, 74, 75}
        });
    }

    /**
     * Tests {@link ComputedImage#hasTileWriters()}, {@link ComputedImage#isTileWritable(int, int)}
     * and {@link ComputedImage#getWritableTileIndices()}.
     */
    @Test
    public void testHasTileWriters() {
        onComputeTile = ComputedImageTest::verifyNoWrite;
        final ComputedImage image = createImage();
        verifyWritableTiles(image, (Point[]) null);
        /*
         * During execution of ComputedImage.computeTile(1, 2),
         * the writable tile indices should be set to (1, 2).
         */
        onComputeTile = ComputedImageTest::verifyWriting;
        final Raster tile = image.getTile(TILE_X, TILE_Y);
        /*
         * After tile computation we should be back to no writable tile.
         */
        verifyWritableTiles(image, (Point[]) null);
        onComputeTile = ComputedImageTest::verifyNoWrite;
        assertSame(tile, image.getTile(TILE_X, TILE_Y));
    }

    /**
     * Callback method invoked during {@code ComputedImage.computeTile(…)} execution.
     */
    private static void verifyWriting(final ComputedImage image) {verifyWritableTiles(image, new Point(TILE_X, TILE_Y));}
    private static void verifyNoWrite(final ComputedImage image) {verifyWritableTiles(image, (Point[]) null);}

    /**
     * Asserts that the writable tiles indices are the expected ones.
     * If non-null, the given indices shall contain (1,2) and shall not contain (2,1).
     */
    private static void verifyWritableTiles(final ComputedImage image, final Point... expected) {
        assertFalse      ("isTileWritable",                   image.isTileWritable(2, 1));
        assertEquals     ("isTileWritable", null != expected, image.isTileWritable(TILE_X, TILE_Y));
        assertEquals     ("hasTileWriters", null != expected, image.hasTileWriters());
        assertArrayEquals("getWritableTileIndices", expected, image.getWritableTileIndices());
    }

    /**
     * Verifies that a tile that failed to compute will not be computed again, unless we mark it as dirty.
     */
    @Test
    public void testErrorFlag() {
        onComputeTile = ComputedImageTest::makeError;
        final ComputedImage image = createImage();
        try {
            image.getTile(TILE_X, TILE_Y);
            fail("Computation should have failed.");
        } catch (ImagingOpException e) {
            assertInstanceOf("cause", IllegalStateException.class, e.getCause());
        }
        /*
         * Ask again for the same tile. ComputedTile should have set a flag for remembering
         * that the computation of this tile failed, and should not try to compute it again.
         */
        onComputeTile = ComputedImageTest::notInvoked;
        try {
            image.getTile(TILE_X, TILE_Y);
            fail("Computation should have failed.");
        } catch (ImagingOpException e) {
            assertNull("cause", e.getCause());
        }
        /*
         * Clearing the error flag should allow to compute the tile again.
         */
        onComputeTile = null;
        assertTrue(image.clearErrorFlags(new Rectangle(TILE_X, TILE_Y, 1, 1)));
        assertNotNull(image.getTile(TILE_X, TILE_Y));
    }

    /**
     * Callback method invoked during {@code ComputedImage.computeTile(…)} execution.
     */
    private static void makeError (final ComputedImage image) {throw new IllegalStateException("Testing an error");}
    private static void notInvoked(final ComputedImage image) {fail("Should not be invoked.");}
}
