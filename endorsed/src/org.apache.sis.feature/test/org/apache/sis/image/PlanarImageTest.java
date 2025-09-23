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

import java.util.Random;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import org.apache.sis.image.internal.shared.ImageUtilities;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import static org.apache.sis.feature.Assertions.assertValuesEqual;


/**
 * Tests {@link PlanarImage}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class PlanarImageTest extends TestCase {
    /**
     * Size of tiles in this test. The width should be different than the height
     * for increasing the chances to detect errors in index calculations.
     */
    private static final int TILE_WIDTH = 3, TILE_HEIGHT = 2;

    /**
     * Creates a rendered image with arbitrary tiles.
     */
    private static PlanarImage createImage() {
        final Random random = TestUtilities.createRandomNumberGenerator();
        final TiledImageMock image = new TiledImageMock(
                DataBuffer.TYPE_USHORT, 1,      // dataType and numBands
                random.nextInt(20) - 10,        // minX
                random.nextInt(20) - 10,        // minY
                TILE_WIDTH  * 4,                // width
                TILE_HEIGHT * 3,                // height
                TILE_WIDTH,
                TILE_HEIGHT,
                random.nextInt(20) - 10,        // minTileX
                random.nextInt(20) - 10,        // minTileY
                random.nextBoolean());          // Banded or interleaved sample model
        image.validate();
        image.initializeAllTiles(0);
        return image;
    }

    /**
     * Creates a new test case.
     */
    public PlanarImageTest() {
    }

    /**
     * Tests {@link PlanarImage#getData()} on a tiled image.
     */
    @Test
    public void testGetData() {
        final PlanarImage image = createImage();
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
     * Tests {@link PlanarImage#getData(Rectangle)} on a tiled image.
     */
    @Test
    public void testGetDataRegion() {
        final PlanarImage image = createImage();
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
