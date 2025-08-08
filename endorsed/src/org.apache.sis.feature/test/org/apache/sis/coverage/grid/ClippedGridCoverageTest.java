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
package org.apache.sis.coverage.grid;

import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.RenderedImage;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.image.privy.RasterFactory;
import org.apache.sis.geometry.DirectPosition2D;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;


/**
 * Tests {@link ClippedGridCoverage}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class ClippedGridCoverageTest extends TestCase {
    /**
     * Size of the test image, in pixels.
     */
    private static final int WIDTH = 7, HEIGHT = 9;

    /**
     * Origin of the grid extent used in the test.
     */
    private static final int OX = 2, OY = 5;

    /**
     * Creates a new test case.
     */
    public ClippedGridCoverageTest() {
    }

    /**
     * Creates a test coverage.
     *
     * @param  processor  non-null for hiding the {@link BufferedImage} implementation class.
     */
    private static GridCoverage createCoverage(final GridCoverageProcessor processor) {
        final GridExtent     extent = new GridExtent(WIDTH, HEIGHT).translate(OX, OY);
        final GridGeometry   domain = new GridGeometry(extent, PixelInCell.CELL_CORNER, MathTransforms.scale(4, 2), HardCodedCRS.WGS84);
        final BufferedImage  data   = RasterFactory.createGrayScaleImage(DataBuffer.TYPE_BYTE, WIDTH, HEIGHT, 1, 0, 0, 100);
        final WritableRaster raster = data.getRaster();
        for (int y=0; y<HEIGHT; y++) {
            for (int x=0; x<WIDTH; x++) {
                raster.setSample(x, y, 0, 10*y+x);
            }
        }
        RenderedImage image = data;
        if (processor != null) {
            // We are not really interrested in statistics, we just want to get a different implementation class.
            image = processor.imageProcessor.statistics(image, null, null);
        }
        return new GridCoverageBuilder().setDomain(domain).setValues(image).build();
    }

    /**
     * Verifies that the given two-dimensional extent has the given low coordinates and size.
     */
    private static void assertExtentStarts(final GridExtent extent, final long low0, final long low1, final long size0, final long size1) {
        assertEquals(2, extent.getDimension());
        assertEquals(low0,  extent.getLow (0));
        assertEquals(low1,  extent.getLow (1));
        assertEquals(size0, extent.getSize(0));
        assertEquals(size1, extent.getSize(1));
    }

    /**
     * Asserts that the value of the first pixel of the given image is equal to the given value.
     */
    private static void assertFirstPixelEquals(final int expected, final RenderedImage image) {
        final Raster tile = image.getTile(0, 0);
        assertEquals(expected, tile.getSample(0, 0, 0));
    }

    /**
     * Tests the clipping of a coverage backed by a {@link BufferedImage}.
     */
    @Test
    public void testWithBufferedImage() {
        test(false);
    }

    /**
     * Tests the clipping of a coverage backed by something else than {@link BufferedImage}.
     */
    @Test
    public void testWithRenderedImage() {
        test(true);
    }

    /**
     * Shared implementation of {@link #testWithBufferedImage()} and {@link #testWithRenderedImage()}.
     *
     * @param  hide  whether to hide the {@link BufferedImage} implementation.
     */
    private static void test(final boolean hide) {
        final var clip = new GridExtent(WIDTH - 3, HEIGHT - 2).translate(OX - 1, OY + 1);
        final var processor = new GridCoverageProcessor();
        final GridCoverage source = createCoverage(hide ? processor : null);
        final GridCoverage target = processor.clip(source, clip);
        assertExtentStarts(source.getGridGeometry().getExtent(), OX, OY,   WIDTH,   HEIGHT);
        assertExtentStarts(target.getGridGeometry().getExtent(), OX, OY+1, WIDTH-4, HEIGHT-2);
        /*
         * Verifications on the source as a matter of principle.
         * This is for making sure that the verifications on the target are okay.
         */
        RenderedImage image = source.render(null);
        assertEquals(WIDTH,  image.getWidth());
        assertEquals(HEIGHT, image.getHeight());
        assertEquals(0,      image.getMinX());      // The returned image match exactly the request.
        assertEquals(0,      image.getMinY());
        assertFirstPixelEquals(0, image);
        /*
         * Verification on the target. In the general case, the image is translated instead of clipped
         * for having at the (0,0) coordinates the pixel that we would have if the image was clipped.
         * This is because `GridCoverage2D` does not know how to clip an arbitrary rendered image.
         * Only in the particular case of `BufferedImage`, a real clip is expected.
         */
        image = target.render(null);
        if (hide) {
            assertEquals(WIDTH,  image.getWidth());
            assertEquals(HEIGHT, image.getHeight());
            assertEquals( 0,     image.getMinX());
            assertEquals(-1,     image.getMinY());
        } else {
            assertEquals(WIDTH  - 4, image.getWidth());
            assertEquals(HEIGHT - 2, image.getHeight());
            assertEquals(0,          image.getMinX());
            assertEquals(0,          image.getMinY());
        }
        assertFirstPixelEquals(10, image);
        /*
         * The result for identical "real world" coordinates should be the same for both coverages.
         */
        final var p = new DirectPosition2D(HardCodedCRS.WGS84, 15, 20);
        assertArrayEquals(source.evaluator().apply(p),
                          target.evaluator().apply(p));
        /*
         * Test again with a sub-region having its origin inside the clipped area. Because of that,
         * `ClippedGridCoverage` does not apply any additional translation on the rendered image.
         */
        image = target.render(new GridExtent(WIDTH, HEIGHT).translate(OX+1, OY+2));
        if (hide) {
            assertEquals(WIDTH,  image.getWidth());
            assertEquals(HEIGHT, image.getHeight());
            assertEquals(-1,     image.getMinX());
            assertEquals(-2,     image.getMinY());
        } else {
            assertEquals(WIDTH  - 5, image.getWidth());
            assertEquals(HEIGHT - 3, image.getHeight());
            assertEquals(0,          image.getMinX());
            assertEquals(0,          image.getMinY());
        }
        assertFirstPixelEquals(21, image);
        assertArrayEquals(source.evaluator().apply(p),
                          target.evaluator().apply(p));
        /*
         * Test again with a sub-region having its origin outside the clipped area.
         * `ClippedGridCoverage` must add a translation for the difference between
         * the request and what we got.
         */
        image = target.render(new GridExtent(WIDTH, HEIGHT).translate(OX+1, OY-1));
        if (hide) {
            assertEquals(WIDTH,  image.getWidth());
            assertEquals(HEIGHT, image.getHeight());
            assertEquals(-1,     image.getMinX());
            assertEquals(+1,     image.getMinY());
        } else {
            assertEquals(WIDTH  - 5, image.getWidth());
            assertEquals(HEIGHT - 2, image.getHeight());
            assertEquals(0,          image.getMinX());
            assertEquals(2,          image.getMinY());
        }
        // Do not test pixel at (0,0) because it does not exist.
        assertArrayEquals(source.evaluator().apply(p),
                          target.evaluator().apply(p));
    }
}
