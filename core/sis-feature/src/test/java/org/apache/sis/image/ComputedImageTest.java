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

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.FeatureAssert.assertValuesEqual;


/**
 * Tests {@link ComputedImage}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 * @module
 */
@DependsOn(PlanarImageTest.class)
public final strictfp class ComputedImageTest extends TestCase {
    /**
     * Size of tiles in this test. The width should be different than the height
     * for increasing the chances to detect errors in index calculations.
     */
    private static final int TILE_WIDTH = 3, TILE_HEIGHT = 2;

    /**
     * Creates an image to test. The {@link ComputedImage} tiles are simply sub-regions of a {@link BufferedImage}.
     */
    private static ComputedImage createImage() {
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
}
