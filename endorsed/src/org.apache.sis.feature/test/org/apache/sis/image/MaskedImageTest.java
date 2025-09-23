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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import org.apache.sis.image.internal.shared.TiledImage;
import org.apache.sis.util.Debug;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.feature.Assertions.assertPixelsEqual;
import org.apache.sis.test.TestCase;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import static org.opengis.test.Assertions.assertSampleValuesEqual;


/**
 * Tests {@link MaskedImage}.
 *
 * @author  Alexis Manin (Geomatys)
 */
public final class MaskedImageTest extends TestCase {
    /**
     * The image width and height in pixels.
     */
    private static final int WIDTH = 8, HEIGHT = 8;

    /**
     * The tile width and height in pixels. Note that the number of tiles
     * is hard-coded to 2 in methods such as {@link #multiTile()}.
     */
    private static final int TILE_WIDTH = WIDTH/2, TILE_HEIGHT = HEIGHT/2;

    /**
     * Creates a new test case.
     */
    public MaskedImageTest() {
    }

    /**
     * Returns a rectangular shape to use as a mask.
     */
    private static Rectangle rectangularMask() {
        return new Rectangle(3, 3, 5, 3);
    }

    /**
     * Returns a non-rectangular polygon to use as a mask.
     */
    private static Polygon concavePolygon() {
        return new Polygon(
            new int[] { 1, 1, 7, 7, 5, 5, 2, 2 },
            new int[] { 1, 6, 6, 3, 3, 4, 4, 1 },
            8);
    }

    /**
     * Tests an empty mask. The result should be identical to source image.
     * Actually, {@link MaskedImage} is expected to optimize by reusing the same raster.
     */
    @Test
    public void noErrorOnEmptyMasks() {
        final BufferedImage source = monoTile();
        final RenderedImage masked = new MaskedImage(source, new Polygon(), true, new Number[] {127});
        assertSampleValuesEqual(source, masked, STRICT, null);
        assertSame(source.getRaster(), masked.getTile(0,0));    // Optimization applied by MaskedImage.
    }

    /**
     * Tests masking pixels inside a rectangle on a image having a single tile.
     */
    @Test
    public void fill_MONO_tile_INside_conVEX_polygon() {
        maskInsideRectangle(monoTile());
    }

    /**
     * Tests masking pixels outside a rectangle on a image having a single tile.
     */
    @Test
    public void fill_MONO_tile_OUTside_conVEX_polygon() {
        maskOutsideConvexPolygon(monoTile());
    }

    /**
     * Tests masking pixels inside a rectangle on a image having many tiles.
     */
    @Test
    public void fill_MULTI_tile_INside_conVEX_polygon() {
        maskInsideRectangle(multiTile());
    }

    /**
     * Tests masking pixels outside a rectangle on a image having many tiles.
     */
    @Test
    public void fill_MULTI_tile_OUTside_conVEX_polygon() {
        maskOutsideConvexPolygon(multiTile());
    }

    /**
     * Tests masking pixels inside a non-rectangular polygon on a image having a single tile.
     */
    @Test
    public void fill_MONO_tile_INside_conCAVE_polygon() {
        fillInsideConcavePolygon(monoTile());
    }

    /**
     * Tests masking pixels outside a non-rectangular polygon on a image having a single tile.
     */
    @Test
    public void fill_MONO_tile_OUTside_conCAVE_polygon() {
        fillOutsideConcavePolygon(monoTile());
    }

    /**
     * Tests masking pixels inside a non-rectangular polygon on a image having many tiles.
     */
    @Test
    public void fill_MULTI_tile_INside_conCAVE_polygon() {
        fillInsideConcavePolygon(multiTile());
    }

    /**
     * Tests masking pixels outside a non-rectangular polygon on a image having many tiles.
     */
    @Test
    public void fill_MULTI_tile_OUTside_conCAVE_polygon() {
        fillOutsideConcavePolygon(multiTile());
    }

    /**
     * Ensure that performing a mask on a {@linkplain BufferedImage#getSubimage(int, int, int, int)
     * subset of a buffered image} will return a tile correctly sized.
     */
    @Test
    public void maskSubRegion() {
        final BufferedImage source = monoTile();
        final BufferedImage sourceSubset = source.getSubimage(0, 0, 4, 4);
        final ImageProcessor processor = new ImageProcessor();
        processor.setFillValues(4);
        final RenderedImage mask = processor.mask(sourceSubset, new Rectangle(0, 0, 2, 2), true);

        final Raster tile = mask.getTile(0, 0);
        assertEquals(mask.getTileWidth(),  tile.getWidth());
        assertEquals(mask.getTileHeight(), tile.getHeight());

        // Note: put 5 on pixels that should not be tested, so the test will fail if we do not test the right area
        final RenderedImage expected = monoTile(new int[] {
                4, 4, 0, 0, 5, 5, 5, 5,
                4, 4, 0, 0, 5, 5, 5, 5,
                0, 0, 0, 0, 5, 5, 5, 5,
                0, 0, 0, 0, 5, 5, 5, 5,
                5, 5, 5, 5, 5, 5, 5, 5,
                5, 5, 5, 5, 5, 5, 5, 5,
                5, 5, 5, 5, 5, 5, 5, 5,
                5, 5, 5, 5, 5, 5, 5, 5
        });
        assertPixelsEqual(expected, new Rectangle(4, 4), mask, null);
    }

    /**
     * Tests masking pixels inside a rectangle on the given image.
     * This method is invoked twice, for untiled image and for tiled image.
     */
    private static void maskInsideRectangle(final RenderedImage source) {
        final RenderedImage masked = new MaskedImage(source, rectangularMask(), true, new Number[]{ 4 });
        final RenderedImage expected = monoTile(new int[] {
                0, 0, 0, 0, 1, 1, 1, 1,
                0, 0, 0, 0, 1, 1, 1, 1,
                0, 0, 0, 0, 1, 1, 1, 1,
                0, 0, 0, 4, 4, 4, 4, 4,
                3, 3, 3, 4, 4, 4, 4, 4,
                3, 3, 3, 4, 4, 4, 4, 4,
                3, 3, 3, 3, 2, 2, 2, 2,
                3, 3, 3, 3, 2, 2, 2, 2
        });
        assertSampleValuesEqual(expected, masked, STRICT, null);
    }

    /**
     * Tests masking pixels outside a rectangle on the given image.
     * This method is invoked twice, for untiled image and for tiled image.
     */
    private static void maskOutsideConvexPolygon(final RenderedImage source) {
        final RenderedImage masked = new MaskedImage(source, rectangularMask(), false, new Number[]{ 4 });
        final RenderedImage expected = monoTile(new int[] {
                4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 0, 1, 1, 1, 1,
                4, 4, 4, 3, 2, 2, 2, 2,
                4, 4, 4, 3, 2, 2, 2, 2,
                4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4
        });
        assertSampleValuesEqual(expected, masked, STRICT, null);
    }

    /**
     * Tests masking pixels inside a non-rectangular polygon on the given image.
     * This method is invoked twice, for untiled image and for tiled image.
     */
    private static void fillInsideConcavePolygon(final RenderedImage source) {
        final RenderedImage masked = new MaskedImage(source, concavePolygon(), true, new Number[]{ 4 });
        final RenderedImage expected = monoTile(new int[] {
                0, 0, 0, 0, 1, 1, 1, 1,
                0, 4, 0, 0, 1, 1, 1, 1,
                0, 4, 0, 0, 1, 1, 1, 1,
                0, 4, 0, 0, 1, 4, 4, 1,
                3, 4, 4, 4, 4, 4, 4, 2,
                3, 4, 4, 4, 4, 4, 4, 2,
                3, 3, 3, 3, 2, 2, 2, 2,
                3, 3, 3, 3, 2, 2, 2, 2
        });
        assertSampleValuesEqual(expected, masked, STRICT, null);
    }

    /**
     * Tests masking pixels outside a non-rectangular polygon on the given image.
     * This method is invoked twice, for untiled image and for tiled image.
     */
    private static void fillOutsideConcavePolygon(final RenderedImage source) {
        final RenderedImage masked = new MaskedImage(source, concavePolygon(), false, new Number[]{ 4 });
        final RenderedImage expected = monoTile(new int[] {
                4, 4, 4, 4, 4, 4, 4, 4,
                4, 0, 4, 4, 4, 4, 4, 4,
                4, 0, 4, 4, 4, 4, 4, 4,
                4, 0, 4, 4, 4, 1, 1, 4,
                4, 3, 3, 3, 2, 2, 2, 4,
                4, 3, 3, 3, 2, 2, 2, 4,
                4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4
        });
        assertSampleValuesEqual(expected, masked, STRICT, null);
    }

    /**
     * Creates the base test image with a single tile.
     * Sample values are between 0 and 3 inclusive and
     * the image uses an {@linkplain #colorPalette() indexed color model}.
     */
    private static BufferedImage monoTile() {
        return monoTile(new int[] {
                0, 0, 0, 0, 1, 1, 1, 1,
                0, 0, 0, 0, 1, 1, 1, 1,
                0, 0, 0, 0, 1, 1, 1, 1,
                0, 0, 0, 0, 1, 1, 1, 1,
                3, 3, 3, 3, 2, 2, 2, 2,
                3, 3, 3, 3, 2, 2, 2, 2,
                3, 3, 3, 3, 2, 2, 2, 2,
                3, 3, 3, 3, 2, 2, 2, 2
        });
    }

    /**
     * Creates an image with a single tile and the given number of values.
     * Image size is {@value #WIDTH}×{@value #HEIGHT} pixels.
     */
    private static BufferedImage monoTile(final int[] pixels) {
        assertEquals(WIDTH*HEIGHT, pixels.length, "Input raster must be " + WIDTH + "×" + HEIGHT);
        final BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_BYTE_INDEXED, colorPalette());
        final WritableRaster raster = image.getRaster();
        raster.setPixels(0, 0, WIDTH, HEIGHT, pixels);
        return image;
    }

    /**
     * Creates an image with a 4 tiles and the given number of values.
     * Image size is {@value #WIDTH}×{@value #HEIGHT} pixels.
     */
    private static RenderedImage multiTile() {
        final TiledImage image = new TiledImage(null, colorPalette(), WIDTH, HEIGHT, 0, 0,
                tile(         0,           0, 0),
                tile(TILE_WIDTH,           0, 1),
                tile(         0, TILE_HEIGHT, 3),
                tile(TILE_WIDTH, TILE_HEIGHT, 2));
        assertNull(image.verify());
        return image;
    }

    /**
     * Creates a tile filled with the given value.
     *
     * @param  x  <var>x</var> coordinate of upper-left corner.
     * @param  y  <var>y</var> coordinate of upper-left corner.
     * @param  fillValue  the value to use for filling the raster.
     * @return a tile filled with the given value.
     */
    private static WritableRaster tile(final int x, final int y, final int fillValue) {
        final WritableRaster tile = Raster.createInterleavedRaster(
                DataBuffer.TYPE_BYTE, TILE_WIDTH, TILE_HEIGHT, 1, new Point(x, y));
        final int[] pixels = new int[TILE_WIDTH * TILE_HEIGHT];
        Arrays.fill(pixels, fillValue);
        tile.setPixels(x, y, TILE_WIDTH, TILE_HEIGHT, pixels);
        return tile;
    }

    /**
     * Base colors are shades of blue. The last (fifth) color used for masks is green.
     */
    private static IndexColorModel colorPalette() {
        byte[] reds   = { 0,  0,  0,   0,   0 };
        byte[] greens = { 0,  0,  0,   0, 127 };
        byte[] blues  = { 0, 50, 95, 127,   0 };
        return new IndexColorModel(Byte.SIZE, 5, reds, greens, blues);
    }

    /**
     * Draws the given polygon into the given image, in order to display expected result in debugger.
     * If the given image is {@code null}, the {@linkplain #monoTile() untiled source image} is used.
     *
     * <p>Tip for IntelliJ users:</p> this method allows to display input image in debugger
     * by adding a new watch calling this method on the wanted image.
     *
     * @param  source    the image to display, or {@code null} for {@link #monoTile()}.
     * @param  geometry  the geometry to show on top of the image, or {@code null} if none.
     * @return the image directly displayable through debugger.
     */
    @Debug
    private static BufferedImage debugGeometryImage(RenderedImage source, final Shape geometry) {
        if (source == null) {
            source = monoTile();
        }
        final BufferedImage debugImg = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        final Graphics2D painter = debugImg.createGraphics();
        painter.drawRenderedImage(source, new AffineTransform());
        if (geometry != null) {
            final Rectangle bounds = geometry.getBounds();
            final Rectangle enlargedBounds = new Rectangle(bounds.x - 1, bounds.y - 1, bounds.width + 2, bounds.height + 2);
            final Rectangle intersection = enlargedBounds.intersection(debugImg.getRaster().getBounds());
            painter.setColor(Color.RED);
            painter.fill(intersection);
            painter.setColor(Color.GREEN);
            painter.fill(geometry);
        }
        painter.dispose();
        return debugImg;
    }
}
