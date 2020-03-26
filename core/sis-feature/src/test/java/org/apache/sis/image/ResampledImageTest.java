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
import java.util.Random;
import java.nio.FloatBuffer;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.awt.image.BandedSampleModel;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import org.apache.sis.internal.coverage.j2d.TiledImage;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link ResampledImage} with predefined {@link Interpolation} instances.
 *
 * @author  Rémi Marechal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 */
public final strictfp class ResampledImageTest extends TestCase {
    /**
     * The source image. This is initialized to arbitrary values in two bands.
     * Location and number of tiles are random.
     */
    private PlanarImage source;

    /**
     * The result of resampling {@link #source} image.
     */
    private ResampledImage target;

    /**
     * The transform from {@linkplain #source} pixel coordinates to {@linkplain #target} pixel coordinates.
     * Note that this is the inverse of {@link ResampledImage#toSource} transform.
     */
    private AffineTransform sourceToTarget;

    /**
     * Creates a rendered image with arbitrary tiles.
     *
     * @param  dataType  {@link DataBuffer#TYPE_SHORT} or {@link DataBuffer#TYPE_FLOAT}.
     */
    private static PlanarImage createImage(final int dataType) {
        final Random random  = TestUtilities.createRandomNumberGenerator();
        final int tileWidth  = random.nextInt(8) + 3;
        final int tileHeight = random.nextInt(8) + 3;
        final int numXTiles  = random.nextInt(3) + 1;
        final int numYTiles  = random.nextInt(4) + 1;
        final TiledImageMock image = new TiledImageMock(
                dataType, 2,                    // dataType and numBands
                random.nextInt(32) - 10,        // minX
                random.nextInt(32) - 10,        // minY
                tileWidth  * numXTiles,         // width
                tileHeight * numYTiles,         // height
                tileWidth,
                tileHeight,
                random.nextInt(32) - 10,        // minTileX
                random.nextInt(32) - 10);       // minTileY
        image.validate();
        image.initializeAllTiles(0);
        image.setRandomValues(1, random, 1024);
        return image;
    }

    /**
     * Creates an interpolated image in the simple case where the image is scaled by a factor 2.
     * The {@link #source} field must be initialized before this method is invoked.
     *
     * @param  interpolation  the interpolation method to test.
     * @param  minX  minimal X coordinate to give to the resampled image.
     * @param  minY  minimal Y coordinate to give to the resampled image.
     * @throws NoninvertibleTransformException if the test did not setup the transform correctly.
     */
    private void createScaledByTwo(final Interpolation interpolation, final int minX, final int minY)
            throws NoninvertibleTransformException
    {
        final Rectangle bounds = new Rectangle(minX, minY, source.getWidth() * 2, source.getHeight() * 2);
        final AffineTransform tr = AffineTransform.getTranslateInstance(source.getMinX(), source.getMinY());
        tr.scale(0.5, 0.5);
        tr.translate(-bounds.x, -bounds.y);
        target = new ResampledImage(bounds, new AffineTransform2D(tr), source, interpolation, null);

        tr.invert();
        sourceToTarget = tr;
        assertNull(target.verify());        // Fails if we did not setup the `toSource` transform correctly.
    }

    /**
     * Verifies that all pixels that are mapped to an integer position in the source image have the same
     * values than in the source image. This verification is suitable to interpolation methods that are
     * expected to return the exact pixel values when the interpolation point is exactly in a pixel center.
     * This is the case of {@link Interpolation#BILINEAR} for example, but not {@link Interpolation#LANCZOS}.
     */
    private void verifyAtIntegerPositions() {
        final PixelIterator ps = PixelIterator.create(source);
        final PixelIterator pt = PixelIterator.create(target);
        double[] sv = null;
        double[] tv = null;
        while (ps.next()) {
            Point p = ps.getPosition();
            p = (Point) sourceToTarget.transform(p, p);
            pt.moveTo(p.x, p.y);
            sv = ps.getPixel(sv);
            tv = pt.getPixel(tv);
            assertArrayEquals(sv, tv, 1E-12);
        }
    }

    /**
     * Verifies that all pixels that are mapped to a position with 0.5 fraction digits have values equal to
     * the average of values of pixels surrounding that position. This is valid to linear interpolation but
     * is not guaranteed for other interpolation methods.
     *
     * <p>This method assumes that the transform from source to target image is a scale by a factor 2.</p>
     *
     * @param  tolerance  a small value such as 1E-12 if target values are floating points,
     *                    or 0.5 if target values are integers (because of rounding).
     */
    private void verifyAtMiddlePositions(final double tolerance) {
        final int halfScale = 1;              // Half of the scale factor in source to target transform.
        final int ws        = 2;              // Number of pixels to average (window size).

        final PixelIterator pt = PixelIterator.create(target);
        final PixelIterator ps = new PixelIterator.Builder().setWindowSize(new Dimension(ws, ws)).create(source);
        final PixelIterator.Window<FloatBuffer> window = ps.createWindow(TransferType.FLOAT);
        final FloatBuffer values = window.values;
        double[] sv = new double[ps.getNumBands()];
        double[] tv = null;
        while (ps.next()) {
            /*
             * Compute expected values as the average of pixel values in a 2×2 window.
             */
            window.update();
            Arrays.fill(sv, 0);
            while (values.hasRemaining()) {
                for (int i=0; i<sv.length; i++) {
                    sv[i] += values.get();
                }
            }
            for (int i=0; i<sv.length; i++) {
                sv[i] /= (ws * ws);
            }
            /*
             * Compare with actual interpolated values.
             */
            Point p = ps.getPosition();
            p = (Point) sourceToTarget.transform(p, p);
            pt.moveTo(p.x + halfScale, p.y + halfScale);
            tv = pt.getPixel(tv);
            assertArrayEquals(sv, tv, tolerance);
        }
    }

    /**
     * Tests {@link Interpolation#BILINEAR} of floating point values.
     *
     * @throws NoninvertibleTransformException if the test did not setup the transform correctly.
     */
    @Test
    public void testBilinearOnFloats() throws NoninvertibleTransformException {
        source = createImage(DataBuffer.TYPE_FLOAT);
        createScaledByTwo(Interpolation.BILINEAR, -40, 50);
        verifyAtIntegerPositions();
        verifyAtMiddlePositions(1E-12);
    }

    /**
     * Tests {@link Interpolation#BILINEAR} of integer values.
     *
     * @throws NoninvertibleTransformException if the test did not setup the transform correctly.
     */
    @Test
    public void testBilinearOnIntegers() throws NoninvertibleTransformException {
        source = createImage(DataBuffer.TYPE_SHORT);
        createScaledByTwo(Interpolation.BILINEAR, 40, -50);
        verifyAtIntegerPositions();
        verifyAtMiddlePositions(0.5);
    }

    /**
     * Tests interpolation in image corners. We test specifically the corners because there is
     * special cases in {@link ResampledImage} code for image borders.
     *
     * @throws NoninvertibleTransformException if the test did not setup the transform correctly.
     */
    @Test
    public void testCorners() throws NoninvertibleTransformException {
        final WritableRaster raster = WritableRaster.createWritableRaster(
                new BandedSampleModel(DataBuffer.TYPE_FLOAT, 3, 3, 1), new Point(-1, -1));
        float value = 0;
        for (int y = -1; y < 2; y++) {
            for (int x = -1; x < 2; x++) {
                raster.setSample(x, y, 0, value++);
            }
        }
        source = new TiledImage(null, 3, 3, 0, 0, raster);
        assertNull(source.verify());
        createScaledByTwo(Interpolation.BILINEAR, -2, -2);
        final PixelIterator pt = PixelIterator.create(target);
        assertResultEquals(pt, -0.5f, -1.0f,  0.5f);
        assertResultEquals(pt, -1.0f, -0.5f,  1.5f);
        assertResultEquals(pt, -1.0f, -1.0f,  0.0f);            // Upper left corner
        assertResultEquals(pt,  0.5f,  0.0f,  4.5f);
        assertResultEquals(pt,  0.0f, -1.0f,  1.0f);
        assertResultEquals(pt,  1.0f, -1.0f,  2.0f);
        assertResultEquals(pt,  1.5f, -1.0f,  2.5f);            // Upper right corner
        assertResultEquals(pt, -1.0f,  0.5f,  4.5f);
        assertResultEquals(pt,  0.0f,  0.5f,  5.5f);
        assertResultEquals(pt, -1.0f,  1.5f,  7.5f);            // Lower left corner
        assertResultEquals(pt,  0.0f, -0.5f,  2.5f);
        assertResultEquals(pt,  0.5f,  0.0f,  4.5f);
        assertResultEquals(pt,  1.0f,  0.5f,  6.5f);
        assertResultEquals(pt,  0.5f,  1.0f,  7.5f);
        assertResultEquals(pt,  1.5f,  1.5f, 10.0f);            // Lower right corner
    }

    private static void assertResultEquals(final PixelIterator pt, float x, float y, float expected) {
        pt.moveTo((int) (x*2), (int) (y*2));
        assertEquals(expected, pt.getSampleFloat(0), 1E-9f);
    }
}
