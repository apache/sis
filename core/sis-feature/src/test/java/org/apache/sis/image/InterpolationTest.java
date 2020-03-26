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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link Interpolation} predefined instances.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.1
 */
public final strictfp class InterpolationTest extends TestCase {
    /**
     * Size of tiles in this test. The width should be different than the height
     * for increasing the chances to detect errors in index calculations.
     */
    private static final int TILE_WIDTH = 5, TILE_HEIGHT = 4;

    /**
     * Creates a rendered image with arbitrary tiles.
     *
     * @param  dataType  {@link DataBuffer#TYPE_SHORT} or {@link DataBuffer#TYPE_FLOAT}.
     */
    private static PlanarImage createImage(final int dataType) {
        final Random random = TestUtilities.createRandomNumberGenerator();
        final TiledImageMock image = new TiledImageMock(
                DataBuffer.TYPE_SHORT, 2,       // dataType and numBands
                random.nextInt(32) - 10,        // minX
                random.nextInt(32) - 10,        // minY
                TILE_WIDTH  * 3,                // width
                TILE_HEIGHT * 3,                // height
                TILE_WIDTH,
                TILE_HEIGHT,
                random.nextInt(32) - 10,        // minTileX
                random.nextInt(32) - 10);       // minTileY
        image.validate();
        image.initializeAllTiles(0);
        image.setRandomValues(1, random, 1024);
        return image;
    }

    /**
     * Tests interpolation in the simple case where the image is scaled by a factor 2.
     * All sample values at even indices in the result should be equal to sample values
     * at the index divided by 2 in the source image. Sample values at odd indices have
     * values that depend on the interpolation method.
     *
     * @param  interpolation  the interpolation method to test.
     * @param  isInteger      whether to test with integer values or floating point type.
     * @throws NoninvertibleTransformException if the test did not setup the transform correctly.
     */
    private static void scaleByTwo(final Interpolation interpolation, final boolean isInteger) throws NoninvertibleTransformException {
        final PlanarImage     source = createImage(isInteger ? DataBuffer.TYPE_SHORT : DataBuffer.TYPE_FLOAT);
        final Rectangle       bounds = new Rectangle(-40, 50, source.getWidth() * 2, source.getHeight() * 2);
        final AffineTransform tr     = AffineTransform.getTranslateInstance(source.getMinX(), source.getMinY());
        tr.scale(0.5, 0.5);
        tr.translate(-bounds.x, -bounds.y);
        final ResampledImage target = new ResampledImage(bounds, new AffineTransform2D(tr), source, interpolation, null);
        assertNull(target.verify());        // Fails if we did not setup the `toSource` transform correctly.

        tr.invert();
        final PixelIterator ps = PixelIterator.create(source);
        final PixelIterator pt = PixelIterator.create(target);
        double[] sv = null;
        double[] tv = null;
        while (ps.next()) {
            Point p = ps.getPosition();
            p = (Point) tr.transform(p, p);
            pt.moveTo(p.x, p.y);
            sv = ps.getPixel(sv);
            tv = pt.getPixel(tv);
            assertArrayEquals(sv, tv, 1E-12);
        }
    }

    /**
     * Tests {@link Interpolation#BILINEAR}.
     *
     * @throws NoninvertibleTransformException if the test did not setup the transform correctly.
     */
    @Test
    public void testBilinear() throws NoninvertibleTransformException {
        scaleByTwo(Interpolation.BILINEAR, true);
        scaleByTwo(Interpolation.BILINEAR, false);
    }
}
