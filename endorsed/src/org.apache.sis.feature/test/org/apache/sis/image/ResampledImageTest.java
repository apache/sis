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
import java.awt.Color;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import org.apache.sis.image.privy.RasterFactory;
import org.apache.sis.referencing.privy.AffineTransform2D;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;


/**
 * Tests {@link ResampledImage} with predefined {@link Interpolation} instances.
 *
 * @author  Rémi Marechal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ResampledImageTest extends TestCase {
    /**
     * The source image. This is initialized to arbitrary values in two bands.
     * Location and number of tiles are random.
     */
    private RenderedImage source;

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
     * The interpolation method being tested.
     */
    private Interpolation interpolation;

    /**
     * Creates a new test case.
     */
    public ResampledImageTest() {
    }

    /**
     * Creates a rendered image with arbitrary tiles and some random values.
     *
     * @param  dataType  {@link DataBuffer#TYPE_SHORT} or {@link DataBuffer#TYPE_FLOAT}.
     */
    private static PlanarImage createRandomImage(final int dataType) {
        final Random random  = TestUtilities.createRandomNumberGenerator();
        final int tileWidth  = random.nextInt(8) + 4;
        final int tileHeight = random.nextInt(8) + 4;
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
                random.nextInt(32) - 10,        // minTileY
                random.nextBoolean());          // Banded or interleaved sample model
        image.validate();
        image.initializeAllTiles(0);
        image.setRandomValues(1, random, 1024);
        return image;
    }

    /**
     * Creates an interpolated image in the simple case where the image is scaled by a factor 2.
     * The {@link #source} and {@link #interpolation} fields must be initialized before this method is invoked.
     * This method uses processor instead of instantiating {@link ResampledImage} directly for opportunistic
     * testing of {@link ImageProcessor#resample(RenderedImage, Rectangle, MathTransform)} method.
     *
     * @param  minX  minimal X coordinate to give to the resampled image.
     * @param  minY  minimal Y coordinate to give to the resampled image.
     */
    private void createScaledByTwo(final int minX, final int minY) {
        final var bounds = new Rectangle(minX, minY, source.getWidth() * 2, source.getHeight() * 2);
        final var tr = AffineTransform.getTranslateInstance(source.getMinX(), source.getMinY());
        tr.scale(0.5, 0.5);
        tr.translate(-bounds.x, -bounds.y);
        resample(bounds, tr);
    }

    /**
     * Creates the resampled image. The source image shall be specified in {@link #source} field.
     * The interpolation result will be stored in {@link #target}.
     */
    private void resample(final Rectangle bounds, final AffineTransform tr) {
        final var processor = new ImageProcessor();
        processor.setInterpolation(interpolation);
        target = (ResampledImage) processor.resample(source, bounds, new AffineTransform2D(tr));
        try {
            tr.invert();
        } catch (NoninvertibleTransformException e) {
            throw new AssertionError(e);
        }
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
            p = assertInstanceOf(Point.class, sourceToTarget.transform(p, p));
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
            p = assertInstanceOf(Point.class, sourceToTarget.transform(p, p));
            pt.moveTo(p.x + halfScale, p.y + halfScale);
            tv = pt.getPixel(tv);
            assertArrayEquals(sv, tv, tolerance);
        }
    }

    /**
     * Verifies the valid area of an image which is expected to have a rectangular result.
     */
    private void verifyRectangularResult() {
        assertEquals(target.getBounds(), target.getValidArea().getBounds(), "validArea");
    }

    /**
     * Tests {@link Interpolation#NEAREST} on floating point values.
     */
    @Test
    public void testNearestOnFloats() {
        source = createRandomImage(DataBuffer.TYPE_FLOAT);
        interpolation = Interpolation.NEAREST;
        createScaledByTwo(-30, 12);
        verifyAtIntegerPositions();
        verifyRectangularResult();
    }

    /**
     * Tests {@link Interpolation#NEAREST} on integer values.
     */
    @Test
    public void testNearestOnIntegers() {
        source = createRandomImage(DataBuffer.TYPE_SHORT);
        interpolation = Interpolation.NEAREST;
        createScaledByTwo(18, 20);
        verifyAtIntegerPositions();
        verifyRectangularResult();
    }

    /**
     * Tests {@link Interpolation#BILINEAR} on floating point values.
     */
    @Test
    public void testBilinearOnFloats() {
        source = createRandomImage(DataBuffer.TYPE_FLOAT);
        interpolation = Interpolation.BILINEAR;
        createScaledByTwo(-40, 50);
        verifyAtIntegerPositions();
        verifyAtMiddlePositions(1E-12);
        verifyRectangularResult();
    }

    /**
     * Tests {@link Interpolation#BILINEAR} on integer values.
     */
    @Test
    public void testBilinearOnIntegers() {
        source = createRandomImage(DataBuffer.TYPE_SHORT);
        interpolation = Interpolation.BILINEAR;
        createScaledByTwo(40, -50);
        verifyAtIntegerPositions();
        verifyAtMiddlePositions(0.5);
        verifyRectangularResult();
    }

    /**
     * Resamples a single-tiled and single-banded image with values 1 everywhere except in center.
     * The {@linkplain #source} is a 3×3 or 4×4 image with the following values:
     *
     * <blockquote><pre>
     *   1 1 1
     *   1 2 1
     *   1 1 1
     * </pre></blockquote>
     *
     * The {@linkplain #target} is a 9×9 image computed using the {@linkplain #interpolation} method.
     * It is caller's responsibility to set {@link #interpolation} field before to invoke this method
     * and to verify the result.
     *
     * @param  size  the image width and height.
     * @return the resampled raster values.
     */
    private float[] resampleSimpleImage(final int size) {
        source = RasterFactory.createGrayScaleImage(DataBuffer.TYPE_FLOAT, size, size, 1, 0, 0, 2);
        final WritableRaster raster = assertInstanceOf(BufferedImage.class, source).getRaster();
        final float[] sourceValues = assertInstanceOf(DataBufferFloat.class, raster.getDataBuffer()).getData();
        Arrays.fill(sourceValues, 1);
        final int clo = size/2 - size/4;
        final int cup = size/2 + size/4 + 1;
        for (int i=clo; i<cup; i++) {
            Arrays.fill(sourceValues, i*size + clo, i*size + cup, 2);
        }
        sourceToTarget = AffineTransform.getTranslateInstance(-0.5, -0.5);
        sourceToTarget.scale(3, 3);
        sourceToTarget.translate(0.5, 0.5);
        final AffineTransform2D toSource;
        try {
            toSource = new AffineTransform2D(sourceToTarget.createInverse());
        } catch (NoninvertibleTransformException e) {
            throw new AssertionError(e);
        }
        final var bounds = new Rectangle(9, 9);
        target = new ResampledImage(source,
                ImageLayout.DEFAULT.createCompatibleSampleModel(source, bounds),
                null, bounds, toSource, interpolation, null, null);

        assertEquals(1, target.getNumXTiles());
        assertEquals(1, target.getNumYTiles());
        final DataBufferFloat data = assertInstanceOf(DataBufferFloat.class, target.getTile(0,0).getDataBuffer());
        assertEquals(1, data.getNumBanks());
        return data.getData();
    }

    /**
     * Checks all values of a nearest-neighbor interpolation on a simple image.
     */
    @Test
    public void verifyNearestResults() {
        interpolation = Interpolation.NEAREST;
        assertArrayEquals(new float[] {
            1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,
            1,1,1,2,2,2,1,1,1,
            1,1,1,2,2,2,1,1,1,
            1,1,1,2,2,2,1,1,1,
            1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1
        }, resampleSimpleImage(3));
    }

    /**
     * Checks all values of a bilinear interpolation on a simple image.
     */
    @Test
    public void verifyBilinearResults() {
        interpolation = Interpolation.BILINEAR;
        assertArrayEquals(new float[] {
            1.111111111f, 1f, 0.888888888f, 0.777777777f, 0.666666666f, 0.777777777f, 0.888888888f, 1f, 1.111111111f,
            1.000000000f, 1f, 1.000000000f, 1.000000000f, 1.000000000f, 1.000000000f, 1.000000000f, 1f, 1.000000000f,
            0.888888888f, 1f, 1.111111111f, 1.222222222f, 1.333333333f, 1.222222222f, 1.111111111f, 1f, 0.888888888f,
            0.777777777f, 1f, 1.222222222f, 1.444444444f, 1.666666666f, 1.444444444f, 1.222222222f, 1f, 0.777777777f,
            0.666666666f, 1f, 1.333333333f, 1.666666666f,      2f,      1.666666666f, 1.333333333f, 1f, 0.666666666f,
            0.777777777f, 1f, 1.222222222f, 1.444444444f, 1.666666666f, 1.444444444f, 1.222222222f, 1f, 0.777777777f,
            0.888888888f, 1f, 1.111111111f, 1.222222222f, 1.333333333f, 1.222222222f, 1.111111111f, 1f, 0.888888888f,
            1.000000000f, 1f, 1.000000000f, 1.000000000f, 1.000000000f, 1.000000000f, 1.000000000f, 1f, 1.000000000f,
            1.111111111f, 1f, 0.888888888f, 0.777777777f, 0.666666666f, 0.777777777f, 0.888888888f, 1f, 1.111111111f
        }, resampleSimpleImage(3));
    }

    /**
     * Tests resampling of a 4 banded image. This simple test uses a solid color on the whole image,
     * but with a different value in each band. Interpolations should result in the same values in all bands.
     */
    @Test
    public void testMultiBands() {
        final var image = new BufferedImage(6, 3, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        g.setColor(Color.ORANGE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();

        source = image;
        interpolation = Interpolation.BILINEAR;
        resample(new Rectangle(3, 2), new AffineTransform(0.5, 0, 0, 2/3d, 0, 0));

        final PixelIterator it = PixelIterator.create(target);
        final double[] expected = {255, 200, 0, 255};           // Alpha, Blue, Green, Red.
        double[] actual = null;
        while (it.next()) {
            actual = it.getPixel(actual);
            assertArrayEquals(actual, expected);
        }
    }
}
