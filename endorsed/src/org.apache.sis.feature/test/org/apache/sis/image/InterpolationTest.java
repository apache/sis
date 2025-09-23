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

import java.nio.DoubleBuffer;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.awt.image.BandedSampleModel;
import org.apache.sis.image.internal.shared.TiledImage;
import org.apache.sis.util.internal.shared.Numerics;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link Interpolation} instances.
 *
 * @author  Rémi Marechal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class InterpolationTest extends TestCase {
    /**
     * Minimal pixel X and Y coordinates used in this test, inclusive.
     */
    private static final int XMIN = -1, YMIN = -1;

    /**
     * Maximal pixel X and Y coordinates used in this test, exclusive.
     */
    private static final int XUP = 2, YUP = 2;

    /**
     * Iterator over an arbitrary source image.
     */
    private PixelIterator iterator;

    /**
     * The interpolation method being tested.
     */
    private Interpolation interpolation;

    /**
     * Values of pixels to use for performing interpolation.
     * The size of this window depends on the interpolation support size.
     */
    private PixelIterator.Window<DoubleBuffer> window;

    /**
     * Creates a new test case.
     */
    public InterpolationTest() {
    }

    /**
     * Creates an iterator over a simple 3×3 image. Image values are:
     *
     * <pre class="text">
     *   0 1 2
     *   3 4 5
     *   6 7 8</pre>
     *
     * @param  support  number of pixels needed for interpolations:
     *         1 for nearest, 2 for bilinear, 4 for bicubic.
     */
    private void createImage(final int support) {
        final WritableRaster raster = WritableRaster.createWritableRaster(
                new BandedSampleModel(DataBuffer.TYPE_FLOAT, XUP-XMIN, YUP-YMIN, 1),
                new Point(XMIN, YMIN));

        float value = 0;
        for (int y=YMIN; y<YUP; y++) {
            for (int x=XMIN; x<XUP; x++) {
                raster.setSample(x, y, 0, value++);
            }
        }
        final TiledImage source = new TiledImage(null, null, XUP-XMIN, YUP-YMIN, 0, 0, raster);
        assertNull(source.verify());
        iterator = new PixelIterator.Builder().setWindowSize(new Dimension(support, support)).create(source);
        window   = iterator.createWindow(TransferType.DOUBLE);
    }

    /**
     * Tests nearest-neighbor interpolation.
     */
    @Test
    public void testNearest() {
        createImage(1);
        interpolation = Interpolation.NEAREST;
        assertNearestResultEquals(-1.0, -1.0,  0);
        assertNearestResultEquals( 0.4, -0.6,  1);
        assertNearestResultEquals(-0.7,  0.4,  3);
        assertNearestResultEquals( 0.4,  0.2,  4);
        assertNearestResultEquals( 0.2, -1.0,  1);
        assertNearestResultEquals( 0.9, -1.0,  2);
        assertNearestResultEquals( 1.2, -0.1,  5);
        assertNearestResultEquals(-0.1, -0.3,  4);
        assertNearestResultEquals(-0.7,  1.0,  6);
        assertNearestResultEquals(-0.1,  1.0,  7);
        assertNearestResultEquals(-1.0,  0.1,  3);
        assertNearestResultEquals(-0.4, -0.3,  4);
        assertNearestResultEquals( 1.0,  0.1,  5);
        assertNearestResultEquals( 0.9,  1.1,  8);
        assertNearestResultEquals( 0.2,  0.9,  7);
        assertNearestResultEquals(-0.1, -0.3,  4);
    }

    /**
     * Tests bilinear interpolation.
     */
    @Test
    public void testBilinear() {
        createImage(2);
        interpolation = Interpolation.BILINEAR;
        assertResultEquals( 0.0,  0.0,  4.0);
        assertResultEquals(-0.5, -1.0,  0.5);
        assertResultEquals(-1.0, -0.5,  1.5);
        assertResultEquals(-1.0, -1.0,  0.0);            // Upper left corner
        assertResultEquals( 0.5,  0.0,  4.5);
        assertResultEquals( 0.0, -1.0,  1.0);
        assertResultEquals( 1.0, -1.0,  2.0);
        assertResultEquals( 1.5, -1.0,  2.5);            // Upper right corner
        assertResultEquals(-1.0,  0.5,  4.5);
        assertResultEquals( 0.0,  0.5,  5.5);
        assertResultEquals(-1.0,  1.5,  7.5);            // Lower left corner
        assertResultEquals( 0.0, -0.5,  2.5);
        assertResultEquals( 0.5,  0.0,  4.5);
        assertResultEquals( 1.0,  0.5,  6.5);
        assertResultEquals( 0.5,  1.0,  7.5);
        assertResultEquals( 1.5,  1.5, 10.0);            // Lower right corner
    }

    /**
     * Tests Lanczos interpolation. The Lanczos kernel has value 1 at the interpolated position
     * and 0 at distances that are an integer number of pixels from the interpolated position.
     * Consequently, when interpolating exactly at pixel center, we expect the exact pixel value.
     */
    @Test
    public void testLanczos() {
        createImage(2);
        interpolation = new LanczosInterpolation(1);
        assertResultEquals(-1.0, -1.0,  0.0);
        assertResultEquals( 0.0,  0.0,  4.0);
        assertResultEquals( 1.0,  1.0,  8.0);
        assertResultEquals( 0.0,  1.0,  7.0);
        assertResultEquals(-1.0,  1.0,  6.0);
    }

    /**
     * Simulate the behavior of the special case done by {@link ResampledImage}
     * for nearest-neighbor interpolation.
     */
    private void assertNearestResultEquals(final double x, final double y, final double expected) {
        assertResultEquals(Math.rint(x), Math.rint(y), expected);
    }

    /**
     * Verifies that a pixel value interpolated in the source image is equal to the expected value.
     *
     * @param x         <var>x</var> coordinate in the source image, from {@value #XMIN} to {@value #XUP} (exclusive).
     * @param y         <var>y</var> coordinate in the source image, from {@value #YMIN} to {@value #YUP} (exclusive).
     * @param expected  the expected value.
     */
    private void assertResultEquals(double x, double y, final double expected) {
        final Dimension size = window.getSize();
        x += ResampledImage.interpolationSupportOffset(size.width);
        y += ResampledImage.interpolationSupportOffset(size.height);
        double px = StrictMath.floor(x);
        double py = StrictMath.floor(y);
        if (px > XUP - size.width)  px--;
        if (py > YUP - size.height) py--;
        if (px < XMIN) px++;
        if (py < YMIN) py++;
        iterator.moveTo((int) px, (int) py);
        window.update();
        final double[] result = new double[1];
        interpolation.interpolate(window.values, 1, x - px, y - py, result, 0);
        assertEquals(expected, result[0], Numerics.COMPARISON_THRESHOLD);
    }
}
