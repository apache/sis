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
import java.awt.image.DataBuffer;
import org.opengis.referencing.operation.MathTransform1D;
import org.apache.sis.referencing.operation.transform.MathTransforms;

// Test dependencies
import org.junit.jupiter.api.Test;
import org.apache.sis.test.TestUtilities;
import static org.apache.sis.feature.Assertions.assertValuesEqual;


/**
 * Tests {@link BandedSampleConverter}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class BandedSampleConverterTest extends ImageTestCase {
    /**
     * Size of tiles in this test. The width should be different than the height
     * for increasing the chances to detect errors in index calculations.
     */
    private static final int TILE_WIDTH = 4, TILE_HEIGHT = 3;

    /**
     * Creates a new test case.
     */
    public BandedSampleConverterTest() {
    }

    /**
     * Creates a converted image with arbitrary tiles.
     * The created image is assigned to the {@link #image} field.
     *
     * @param  sourceType  source data type as one of the {@link DataBuffer} constants.
     * @param  targetType  target data type.
     * @param  scale       the scale factor of the conversion to apply.
     */
    private void createImage(final int sourceType, final DataType targetType, final double scale) {
        final Random random = TestUtilities.createRandomNumberGenerator();
        final TiledImageMock source = new TiledImageMock(
                sourceType, 1,
                random.nextInt(20) - 10,        // minX
                random.nextInt(20) - 10,        // minY
                TILE_WIDTH  * 3,                // width
                TILE_HEIGHT * 2,                // height
                TILE_WIDTH,
                TILE_HEIGHT,
                random.nextInt(20) - 10,        // minTileX
                random.nextInt(20) - 10,        // minTileY
                random.nextBoolean());          // Banded or interleaved sample model
        source.validate();
        source.initializeAllTiles(0);
        image = BandedSampleConverter.create(source, ImageLayout.DEFAULT, null,
                new MathTransform1D[] {(MathTransform1D) MathTransforms.linear(scale, 0)},
                targetType, null);
    }

    /**
     * Asserts that {@link #image} values are equal to the source values divided by 10.
     */
    private void assertValuesDivided() {
        assertValuesEqual(image.getData(), 0, new float[][] {
            { 10.0f,  10.1f,  10.2f,  10.3f  ,   20.0f,  20.1f,  20.2f,  20.3f  ,   30.0f,  30.1f,  30.2f,  30.3f},
            { 11.0f,  11.1f,  11.2f,  11.3f  ,   21.0f,  21.1f,  21.2f,  21.3f  ,   31.0f,  31.1f,  31.2f,  31.3f},
            { 12.0f,  12.1f,  12.2f,  12.3f  ,   22.0f,  22.1f,  22.2f,  22.3f  ,   32.0f,  32.1f,  32.2f,  32.3f},
            { 40.0f,  40.1f,  40.2f,  40.3f  ,   50.0f,  50.1f,  50.2f,  50.3f  ,   60.0f,  60.1f,  60.2f,  60.3f},
            { 41.0f,  41.1f,  41.2f,  41.3f  ,   51.0f,  51.1f,  51.2f,  51.3f  ,   61.0f,  61.1f,  61.2f,  61.3f},
            { 42.0f,  42.1f,  42.2f,  42.3f  ,   52.0f,  52.1f,  52.2f,  52.3f  ,   62.0f,  62.1f,  62.2f,  62.3f}
        });
    }

    /**
     * Asserts that {@link #image} values are equal to the source values multiplied by 10.
     */
    private void assertValuesMultiplied() {
        assertValuesEqual(image.getData(), 0, new int[][] {
            { 1000,  1010,  1020,  1030  ,   2000,  2010,  2020,  2030  ,   3000,  3010,  3020,  3030},
            { 1100,  1110,  1120,  1130  ,   2100,  2110,  2120,  2130  ,   3100,  3110,  3120,  3130},
            { 1200,  1210,  1220,  1230  ,   2200,  2210,  2220,  2230  ,   3200,  3210,  3220,  3230},
            { 4000,  4010,  4020,  4030  ,   5000,  5010,  5020,  5030  ,   6000,  6010,  6020,  6030},
            { 4100,  4110,  4120,  4130  ,   5100,  5110,  5120,  5130  ,   6100,  6110,  6120,  6130},
            { 4200,  4210,  4220,  4230  ,   5200,  5210,  5220,  5230  ,   6200,  6210,  6220,  6230}
        });
    }

    /**
     * Tests conversion from unsigned integers to floating point values.
     */
    @Test
    public void testUShortToFloat() {
        createImage(DataBuffer.TYPE_USHORT, DataType.FLOAT, 0.1);
        assertValuesDivided();
    }

    /**
     * Tests conversion from floating point values to unsigned integers.
     */
    @Test
    public void testFloatToUShort() {
        createImage(DataBuffer.TYPE_FLOAT, DataType.USHORT, 10);
        assertValuesMultiplied();
    }

    /**
     * Tests conversion from floating point values to other floating point values.
     */
    @Test
    public void testFloatToFloat() {
        createImage(DataBuffer.TYPE_FLOAT, DataType.FLOAT, 0.1);
        assertValuesDivided();
    }

    /**
     * Tests conversion from unsigned integer values to integers.
     */
    @Test
    public void testUShortToUShort() {
        createImage(DataBuffer.TYPE_USHORT, DataType.USHORT, 10);
        assertValuesMultiplied();
    }

    /**
     * Tests conversion from signed integer values to other integers.
     */
    @Test
    public void testShortToInteger() {
        createImage(DataBuffer.TYPE_SHORT, DataType.INT, 10);
        assertValuesMultiplied();
    }

    /**
     * Tests conversion from floating point values to signed integers.
     */
    @Test
    public void testDoubleToInteger() {
        createImage(DataBuffer.TYPE_DOUBLE, DataType.INT, 10);
        assertValuesMultiplied();
    }
}
