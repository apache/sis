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
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;
import org.apache.sis.referencing.operation.transform.MathTransforms;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.feature.Assertions.assertValuesEqual;


/**
 * Tests {@link ImageCombiner}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Johann Sorel (Geomatys)
 */
public final class ImageCombinerTest extends ImageTestCase {
    /**
     * The image to add to the {@link ImageCombiner}.
     */
    private PlanarImage toAdd;

    /**
     * Creates a new test case.
     */
    public ImageCombinerTest() {
    }

    /**
     * Creates a rendered image with arbitrary tiles.
     */
    private ImageCombiner initialize() {
        final TiledImageMock destination = new TiledImageMock(
                DataBuffer.TYPE_USHORT, 1,      // dataType, numBands
                 3,  4,                         // minX, minY
                12,  8,                         // width, height
                 4,  4,                         // tileWidth, tileHeight
                -2,  3,                         // minTileX, minTileY
                false);
        /*
         * An image intersecting the destination, with a small part outside.
         * Intentionally use a different data type and different tile layout.
         */
        final TiledImageMock source = new TiledImageMock(
                DataBuffer.TYPE_FLOAT, 1,       // dataType, numBands
                 5,  3,                         // minX, minY
                 9,  6,                         // width, height
                 3,  2,                         // tileWidth, tileHeight
                 5,  9,                         // minTileX, minTileY
                false);

        source.validate();
        source.initializeAllTiles(0);
        destination.validate();
        destination.initializeAllTiles(0);
        toAdd = source;
        return new ImageCombiner(destination.toWritableTiledImage());
    }

    /**
     * Tests {@link ImageCombiner#accept(RenderedImage)}.
     */
    @Test
    public void testAccept() {
        final ImageCombiner combiner = initialize();
        /*
         * Verify initial state, before combine operation.
         * We expect 3×2 tiles, numbered as below:
         *
         *    1xy, 2xy, 3xy,
         *    4xy, 5xy, 6xy
         *
         * where x and y are pixel coordinates in a single tile of size 4×4.
         */
        assertValuesEqual(image = combiner.result(), 0, new double[][] {
            {100, 101, 102, 103, 200, 201, 202, 203, 300, 301, 302, 303},
            {110, 111, 112, 113, 210, 211, 212, 213, 310, 311, 312, 313},
            {120, 121, 122, 123, 220, 221, 222, 223, 320, 321, 322, 323},
            {130, 131, 132, 133, 230, 231, 232, 233, 330, 331, 332, 333},
            {400, 401, 402, 403, 500, 501, 502, 503, 600, 601, 602, 603},
            {410, 411, 412, 413, 510, 511, 512, 513, 610, 611, 612, 613},
            {420, 421, 422, 423, 520, 521, 522, 523, 620, 621, 622, 623},
            {430, 431, 432, 433, 530, 531, 532, 533, 630, 631, 632, 633}
        });
        /*
         * Verify source image, before combine operation.
         * We expect 3×3 tiles, numbered as below:
         *
         *    1xy, 2xy, 3xy,
         *    4xy, 5xy, 6xy,
         *    7xy, 8xy, 9xy
         *
         * where x and y are pixel coordinates in a single tile of size 3×3.A
         * A + sign is added in front of pixel values that we expect to find
         * in the destination image at the end of this test method.
         */
        assertValuesEqual(toAdd, 0, new double[][] {
            { 100,  101,  102,  200,  201,  202,  300,  301,  302},
            {+110, +111, +112, +210, +211, +212, +310, +311, +312},
            {+400, +401, +402, +500, +501, +502, +600, +601, +602},
            {+410, +411, +412, +510, +511, +512, +610, +611, +612},
            {+700, +701, +702, +800, +801, +802, +900, +901, +902},
            {+710, +711, +712, +810, +811, +812, +910, +911, +912}
        });
        /*
         * Write an image on top of destination image and verify. The expected result
         * is same as the first table at the beginning of this test method, with above
         * source image replacing destination values starting from row 0 column 3.
         * A + sign is added in front of those values for making easier to recognize.
         */
        combiner.accept(toAdd);
        assertValuesEqual(image = combiner.result(), 0, new double[][] {
            { 100,  101, +110, +111, +112, +210, +211, +212, +310, +311, +312,  303},
            { 110,  111, +400, +401, +402, +500, +501, +502, +600, +601, +602,  313},
            { 120,  121, +410, +411, +412, +510, +511, +512, +610, +611, +612,  323},
            { 130,  131, +700, +701, +702, +800, +801, +802, +900, +901, +902,  333},
            { 400,  401, +710, +711, +712, +810, +811, +812, +910, +911, +912,  603},
            { 410,  411,  412,  413,  510,  511,  512,  513,  610,  611,  612,  613},
            { 420,  421,  422,  423,  520,  521,  522,  523,  620,  621,  622,  623},
            { 430,  431,  432,  433,  530,  531,  532,  533,  630,  631,  632,  633}
        });
    }

    /**
     * Tests {@link ImageCombiner#resample(RenderedImage, Rectangle, MathTransform)}.
     * The transform used in this test is a simple translation. The expected result is
     * similar to the {@link #testAccept()} one, with the new pixel values (identified
     * by a + sign in source code) shifted by 2 rows and 2 columns.
     *
     * <p>In this test, the X coordinate of the first pixel to write is at the beginning of a tile.
     * This alignment creates a situation where each row in {@link #toAdd} is either copied in full
     * or not copied at all. This characteristics help to isolate the problem if a test fails.</p>
     */
    @Test
    public void testResampleAligned() {
        final ImageCombiner combiner = initialize();
        final Rectangle bounds = toAdd.getBounds();
        bounds.translate(2, 2);
        final MathTransform toSource = MathTransforms.translation(-2, -2);
        combiner.resample(toAdd, bounds, toSource);
        assertValuesEqual(image = combiner.result(), 0, new double[][] {
            { 100,  101,  102,  103,  200,  201,  202,  203,  300,  301,  302,  303},
            { 110,  111,  112,  113, +100, +101, +102, +200, +201, +202, +300, +301},
            { 120,  121,  122,  123, +110, +111, +112, +210, +211, +212, +310, +311},
            { 130,  131,  132,  133, +400, +401, +402, +500, +501, +502, +600, +601},
            { 400,  401,  402,  403, +410, +411, +412, +510, +511, +512, +610, +611},
            { 410,  411,  412,  413, +700, +701, +702, +800, +801, +802, +900, +901},
            { 420,  421,  422,  423, +710, +711, +712, +810, +811, +812, +910, +911},
            { 430,  431,  432,  433,  530,  531,  532,  533,  630,  631,  632,  633}
        });
    }

    /**
     * Same as {@link #testResampleAligned()}, but with a "more difficult" translation.
     * In this test, {@link #toAdd} rows are only partially copied.
     *
     * <p><b>Tip:</b> if this test fails, it is easier to first make sure that
     * {@link #testResampleAligned()} pass before to debug this test.</p>
     */
    @Test
    public void testResample() {
        final ImageCombiner combiner = initialize();
        final Rectangle bounds = toAdd.getBounds();
        bounds.translate(-1, 2);
        final MathTransform toSource = MathTransforms.translation(1, -2);
        combiner.resample(toAdd, bounds, toSource);
        assertValuesEqual(image = combiner.result(), 0, new double[][] {
            { 100,  101,  102,  103,  200,  201,  202,  203,  300,  301,  302,  303},
            { 110, +100, +101, +102, +200, +201, +202, +300, +301, +302,  312,  313},
            { 120, +110, +111, +112, +210, +211, +212, +310, +311, +312,  322,  323},
            { 130, +400, +401, +402, +500, +501, +502, +600, +601, +602,  332,  333},
            { 400, +410, +411, +412, +510, +511, +512, +610, +611, +612,  602,  603},
            { 410, +700, +701, +702, +800, +801, +802, +900, +901, +902,  612,  613},
            { 420, +710, +711, +712, +810, +811, +812, +910, +911, +912,  622,  623},
            { 430,  431,  432,  433,  530,  531,  532,  533,  630,  631,  632,  633}
        });
    }

    /**
     * Tests a resampling which requires a correct {@link ResampledImage#interpolationLimit(double, int)} computation.
     * The source image has only one row while the target image has two rows. But the {@code toSource} transform has
     * translation terms of -0.25 pixel, which causes the two destination rows to map to the single source row.
     */
    @Test
    public void testResampleOneToTwo() {
        final double[] inputs   = {3,    5,    1   };
        final double[] expected = {3, 3, 5, 5, 1, 1};
        final BufferedImage source = new BufferedImage(inputs.length,   1, BufferedImage.TYPE_BYTE_GRAY);
        final BufferedImage target = new BufferedImage(expected.length, 2, BufferedImage.TYPE_BYTE_GRAY);
        source.getRaster().setPixels(0, 0, inputs.length, 1, inputs);
        final ImageCombiner combiner = new ImageCombiner(target);
        combiner.setInterpolation(Interpolation.NEAREST);
        combiner.resample(source, null, new AffineTransform2D(0.5, 0, 0, 0.5, -0.25, -0.25));
        assertSame(target, combiner.result());
        assertValuesEqual(source, 0, new double[][] {inputs});
        assertValuesEqual(target, 0, new double[][] {expected, expected});
    }
}
