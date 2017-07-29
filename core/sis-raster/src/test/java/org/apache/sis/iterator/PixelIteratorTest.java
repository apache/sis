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
package org.apache.sis.iterator;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.util.Arrays;
import org.opengis.coverage.grid.SequenceType;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Base class of {@link PixelIterator} tests.
 *
 * @author Rémi Maréchal (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public strictfp class PixelIteratorTest extends TestCase {
    /**
     * The pixel iterator being tested.
     * This field is initialized by a call to one of the {@code createPixelIterator(…)} methods.
     */
    private PixelIterator iterator;

    /**
     * The raster or image data type as one of the {@link DataBuffer} constants.
     */
    private final int dataType;

    /**
     * <var>x</var> coordinate of upper left corner in raster or image.
     */
    private int xmin;

    /**
     * <var>y</var> coordinate of upper left corner in raster or image.
     */
    private int ymin;

    /**
     * Number of pixels along the <var>x</var> axis in raster or image.
     */
    private int width;

    /**
     * Number of pixels along the <var>y</var> axis in raster or image.
     */
    private int height;

    /**
     * Number of bands in the raster or image.
     */
    private int numBands;

    /**
     * Number of pixels along the <var>x</var> axis of each tile in image tiles.
     * This is zero if the tests are performed on a raster instead than a tiled image.
     */
    private int tileWidth;

    /**
     * Number of pixels along the <var>y</var> axis of each tile in image tiles.
     * This is zero if the tests are performed on a raster instead than a tiled image.
     */
    private int tileHeight;

    /**
     * The expected values as a flat, row-major array. The content of this array is a copy
     * of the sample values stored in the raster or image used as a source of test data.
     */
    private float[] expected;

    /**
     * Creates a new test case for the given data type.
     *
     * @param  dataType  the raster or image data type as one of the {@link DataBuffer} constants.
     */
    PixelIteratorTest(final int dataType) {
        this.dataType = dataType;
    }

    /**
     * Creates a new test case.
     */
    public PixelIteratorTest() {
        this(DataBuffer.TYPE_INT);
    }

    /**
     * Creates a {@code WritableRaster} to use as the source of test data.
     * The raster is filled with arbitrary sample values.
     *
     * <p><b>Pre-conditions:</b>
     * before invocation, the {@link #xmin}, {@link #ymin}, {@link #width}, {@link #height} and {@link #numBands}
     * fields must be initialized.</p>
     *
     * @param  subArea  the raster subarea on which to perform iteration, or {@code null} for the whole area.
     * @return a raster filled with arbitrary sample values.
     */
    private WritableRaster createRaster(final Rectangle subArea) {
        final int xmax = xmin + width;                              // Maximum value is exclusive.
        final int ymax = ymin + height;
        final int subMinX, subMinY, subMaxX, subMaxY;
        if (subArea == null) {
            subMinX = xmin;
            subMinY = ymin;
            subMaxX = xmax;
            subMaxY = ymax;
        } else {
            subMinX = StrictMath.max(xmin, subArea.x);
            subMinY = StrictMath.max(ymin, subArea.y);
            subMaxX = StrictMath.min(xmax, subArea.x + subArea.width);
            subMaxY = StrictMath.min(ymax, subArea.y + subArea.height);
        }
        expected = new float[(subMaxX - subMinX) * (subMaxY - subMinY) * numBands];
        final WritableRaster raster = Raster.createWritableRaster(new PixelInterleavedSampleModel(dataType,
                width, height, numBands, width * numBands, new int[] {0,1,2}), new Point(xmin, ymin));
        /*
         * At this point, all data structures have been created an initialized to zero sample values.
         * Now fill the data structures with arbitrary values.
         */
        int n = 0;
        float value = (dataType == DataBuffer.TYPE_FLOAT) ? -2000.5f : 0f;
        for (int y=ymin; y<ymax; y++) {
            final boolean rowIncluded = (y >= subMinY && y < subMaxY);
            for (int x=xmin; x<xmax; x++) {
                final boolean included = rowIncluded && (x >= subMinX && x < subMaxX);
                for (int b = 0; b < numBands; b++) {
                    raster.setSample(x, y, b, value);
                    if (included) {
                        expected[n++] = value;
                    }
                    value++;
                }
            }
        }
        assertEquals("Number of expected values", expected.length, n);
        return raster;
    }

    /**
     * Creates a {@code RenderedImage} to use as the source of test data.
     * The image is filled with arbitrary sample values.
     *
     * <p><b>Pre-conditions:</b>
     * before invocation, the {@link #xmin}, {@link #ymin}, {@link #width}, {@link #height},
     * {@link #tileWidth}, {@link #tileHeight} and {@link #numBands} fields must be initialized.</p>
     *
     * @param  subArea  the image subarea on which to perform iteration, or {@code null} for the whole area.
     * @return an image filled with arbitrary sample values.
     */
    private WritableRenderedImage createImage(final Rectangle subArea) {
        final int numXTiles = (width  + tileWidth -1) / tileWidth;
        final int numYTiles = (height + tileHeight-1) / tileHeight;
        final int xmax = xmin + width;                                  // Maximum value is exclusive.
        final int ymax = ymin + height;
        final int subMinX, subMinY, subMaxX, subMaxY;
        if (subArea == null) {
            subMinX = xmin;
            subMinY = ymin;
            subMaxX = xmax;
            subMaxY = ymax;
        } else {
            subMinX = StrictMath.max(xmin, subArea.x);
            subMinY = StrictMath.max(ymin, subArea.y);
            subMaxX = StrictMath.min(xmax, subArea.x + subArea.width);
            subMaxY = StrictMath.min(ymax, subArea.y + subArea.height);
        }
        expected = new float[(subMaxX - subMinX) * (subMaxY - subMinY) * numBands];
        final IteratorTestImage image = new IteratorTestImage(xmin, ymin, width, height, tileWidth, tileHeight, xmin+tileWidth, ymin+tileHeight,
                new PixelInterleavedSampleModel(dataType, tileWidth, tileHeight, numBands, tileWidth * numBands, new int[] {0,1,2}));
        /*
         * At this point, all data structures have been created an initialized to zero sample values.
         * Now fill the data structures with arbitrary values. We fill them tile-by-tile.
         */
        int n = 0;
        float value = (dataType == DataBuffer.TYPE_FLOAT) ? -200.5f : 0f;
        for (int j=0; j<numYTiles; j++) {
            for (int i=0; i<numXTiles; i++) {
                final int yNextTile = ymin + (j+1) * tileHeight;
                for (int y = yNextTile - tileHeight; y < yNextTile; y++) {
                    final boolean rowIncluded = (y >= subMinY && y < subMaxY);
                    final int xNextTile = xmin + (i+1) * tileWidth;
                    for (int x = xNextTile - tileWidth; x < xNextTile; x++) {
                        final boolean included = rowIncluded && (x >= subMinX && x < subMaxX);
                        for (int b = 0; b < numBands; b++) {
                            image.setSample(x, y, b, value);
                            if (included) {
                                expected[n++] = value;
                            }
                            value++;
                        }
                    }
                }
            }
        }
        assertEquals("Number of expected values", expected.length, n);
        return image;
    }

    /**
     * Creates a {@code PixelIterator} for the full area of given raster.
     * The iterator shall be assigned to the {@link #iterator} field.
     *
     * <p>The default implementation creates read-only iterators.
     * Tests for read-write iterators need to override.</p>
     *
     * @param  raster  the data on which to perform iteration.
     */
    void createPixelIterator(WritableRaster raster) {
        iterator = PixelIteratorFactory.createReadOnlyIterator(raster);
        assertEquals("getIterationDirection()", SequenceType.LINEAR, iterator.getIterationDirection());
    }

    /**
     * Creates a {@code PixelIterator} for a sub-area of given raster.
     * The iterator shall be assigned to the {@link #iterator} field.
     *
     * <p>The default implementation creates read-only iterators.
     * Tests for read-write iterators need to override.</p>
     *
     * @param  raster   the data on which to perform iteration.
     * @param  subArea  the boundary of the raster sub-area where to perform iteration.
     */
    void createPixelIterator(WritableRaster raster, Rectangle subArea) {
        iterator = PixelIteratorFactory.createReadOnlyIterator(raster, subArea);
        assertEquals("getIterationDirection()", SequenceType.LINEAR, iterator.getIterationDirection());
    }

    /**
     * Creates a {@code PixelIterator} for the full area of given image.
     * The iterator shall be assigned to the {@link #iterator} field.
     *
     * <p>The default implementation creates read-only iterators.
     * Tests for read-write iterators need to override.</p>
     *
     * @param  image  the data on which to perform iteration.
     */
    void createPixelIterator(WritableRenderedImage image) {
        iterator = PixelIteratorFactory.createReadOnlyIterator(image);
    }

    /**
     * Creates a {@code PixelIterator} for the full area of given image.
     * The iterator shall be assigned to the {@link #iterator} field.
     *
     * <p>The default implementation creates read-only iterators.
     * Tests for read-write iterators need to override.</p>
     *
     * @param  image    the data on which to perform iteration.
     * @param  subArea  the boundary of the image sub-area where to perform iteration.
     */
    void createPixelIterator(WritableRenderedImage image, Rectangle subArea) {
        iterator = PixelIteratorFactory.createReadOnlyIterator(image, subArea);
    }

    /**
     * Iterates over all values returned by the current {@link #iterator} and compares with expected values.
     *
     * @param verifyIndices  whether to verify also iterator {@code getX()} and {@code getY()} return values.
     *                       This is usually {@code true} if an only if the iterator cover the full raster area.
     */
    private void verifyIteration(final boolean verifyIndices) {
        int i = 0;
        while (iterator.next()) {
            final float e = expected[i++];
            final float a = iterator.getSampleFloat();
            if (Float.floatToRawIntBits(a) != Float.floatToRawIntBits(e)) {
                fail("Pixel iteration at index " + i + ": expected " + e + " but got " + a);
            }
            if (verifyIndices) {
                final int p = i / numBands;
                assertEquals("x", (p % width) + xmin, iterator.getX());
                assertEquals("y", (p / width) + ymin, iterator.getY());
            }
        }
        assertEquals("Too few elements in iteration.", expected.length, i);
    }

    /**
     * Tests iteration over all pixels in a single raster.
     * This method uses different (<var>x</var>,<var>y</var>) origins.
     * Tests also {@link PixelIterator#rewind()}.
     */
    @Test
    @Ignore
    public void testOnRaster() {
        xmin     =  0;
        ymin     =  0;
        width    = 10;
        height   = 12;
        numBands =  3;
        createPixelIterator(createRaster(null));
        verifyIteration(true);

        xmin = 3;
        ymin = 5;
        createPixelIterator(createRaster(null));
        verifyIteration(true);

        xmin   = -3;
        ymin   =  5;
        height =  9;
        createPixelIterator(createRaster(null));
        verifyIteration(true);

        xmin   =  3;
        ymin   = -5;
        height =  7;
        createPixelIterator(createRaster(null));
        verifyIteration(true);

        xmin  = -3;
        ymin  = -5;
        width =  7;
        createPixelIterator(createRaster(null));
        verifyIteration(true);

        iterator.rewind();
        verifyIteration(true);
    }

    /**
     * Tests iteration over a sub-area in a single raster.
     * This test iterates in the upper-left corner of the raster.
     */
    @Test
    @Ignore
    @DependsOnMethod("testOnRaster")
    public void testOnRasterUpperLeft() {
        xmin     =  5;
        ymin     =  7;
        width    = 12;
        height   = 15;
        numBands =  3;
        final Rectangle subArea = new Rectangle(4, 6, 5, 4);
        createPixelIterator(createRaster(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a single raster.
     * This test iterates in the upper-right corner of the raster.
     */
    @Test
    @Ignore
    @DependsOnMethod("testOnRaster")
    public void testOnRasterUpperRight() {
        width    = 15;
        height   = 14;
        xmin     = 6;
        ymin     = 9;
        numBands = 3;
        final Rectangle subArea = new Rectangle(16, 6, 10, 6);
        createPixelIterator(createRaster(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a single raster.
     * This test iterates in the lower-right corner of the raster.
     */
    @Test
    @Ignore
    @DependsOnMethod("testOnRaster")
    public void testOnRasterLowerRight() {
        xmin     =  6;
        ymin     =  7;
        width    = 15;
        height   = 16;
        numBands =  3;
        final Rectangle subArea = new Rectangle(14, 20, 15, 9);
        createPixelIterator(createRaster(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a single raster.
     * This test iterates in the lower-left corner of the raster.
     */
    @Test
    @Ignore
    @DependsOnMethod("testOnRaster")
    public void testOnRasterLowerLeft() {
        xmin     =  4;
        ymin     =  6;
        width    = 16;
        height   = 15;
        numBands =  3;
        final Rectangle subArea = new Rectangle(2, 12, 10, 6);
        createPixelIterator(createRaster(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a single raster.
     * Tests also {@link PixelIterator#rewind()}.
     */
    @Test
    @Ignore
    @DependsOnMethod("testOnRaster")
    public void testOnRasterSubArea() {
        xmin     =  5;
        ymin     =  7;
        width    = 17;
        height   = 16;
        numBands =  3;
        final Rectangle subArea = new Rectangle(10, 9, 8, 6);
        createPixelIterator(createRaster(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIteration(false);

        iterator.rewind();
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area that actually contains all the raster area.
     * Tests also {@link PixelIterator#rewind()}.
     */
    @Test
    @Ignore
    @DependsOnMethod("testOnRaster")
    public void testOnRasterFullArea() {
        xmin     =  7;
        ymin     =  9;
        width    = 11;
        height   = 13;
        numBands =  3;
        final Rectangle subArea = new Rectangle(2, 3, 25, 17);
        createPixelIterator(createRaster(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIteration(true);
    }

    /**
     * Tests iteration over a sub-area that do not intersect the raster area.
     */
    @Test
    @Ignore
    @DependsOnMethod("testOnRaster")
    public void testOnRasterEmptyArea() {
        width    = 7;
        height   = 10;
        xmin     = 6;
        ymin     = 5;
        numBands = 3;
        final Rectangle subArea = new Rectangle(-17, -20, 5, 15);
        createPixelIterator(createRaster(subArea), subArea);
        assertEquals("Expected an empty set of values.", 0, expected.length);
        verifyIteration(true);
    }

    /**
     * Verifies that invoking {@link PixelIterator#moveTo(int, int, int)} with illegal indices causes
     * an exception to be thrown.
     */
    @Test
    @Ignore
    public void testIllegalMoveOnRaster() {
        xmin     =  4;
        ymin     =  7;
        width    = 13;
        height   = 10;
        numBands =  3;
        createPixelIterator(createRaster(null));
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        try {
            iterator.moveTo(2, 3, 0);
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            iterator.moveTo(9, 10, -1);
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            iterator.moveTo(9, 10, 500);
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    /**
     * Tests iteration over all pixels in a tiled image.
     * This method uses different (<var>x</var>,<var>y</var>) origins.
     * Tests also {@link PixelIterator#rewind()}.
     */
    @Test
    @Ignore
    @DependsOnMethod("testOnRaster")
    public void testOnImage() {
        xmin       =   0;
        ymin       =   0;
        width      = 100;
        height     =  50;
        tileWidth  =  10;
        tileHeight =   5;
        numBands   =   3;
        createPixelIterator(createImage(null));
        assertNull("getIterationDirection()", iterator.getIterationDirection());
        verifyIteration(false);

        xmin =   1;
        ymin = -50;
        createPixelIterator(createImage(null));
        assertNull("getIterationDirection()", iterator.getIterationDirection());
        verifyIteration(false);

        iterator.rewind();
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a tiled image.
     * This test iterates in the upper-left corner of the image.
     * The sub-area is small enough for the iteration to happen in a single tile.
     */
    @Test
    @Ignore
    @DependsOnMethod({"testOnImage", "testOnRasterUpperLeft"})
    public void testOnTileUpperLeft() {
        xmin       = -5;
        ymin       =  5;
        width      = 40;
        height     = 30;
        tileWidth  = 10;
        tileHeight =  5;
        numBands   =  3;
        final Rectangle subArea = new Rectangle(-10, -20, 10, 30);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        assertEquals("getIterationDirection()", SequenceType.LINEAR, iterator.getIterationDirection());
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a tiled image.
     * This test iterates in the upper-right corner of the image.
     * The sub-area is small enough for the iteration to happen in a single tile.
     */
    @Test
    @Ignore
    @DependsOnMethod({"testOnImage", "testOnRasterUpperRight"})
    public void testOnTileUpperRight() {
        xmin       = 35;
        ymin       =  7;
        width      = 60;
        height     = 50;
        tileWidth  = 10;
        tileHeight =  5;
        numBands   =  3;
        final Rectangle subArea = new Rectangle(90, -20, 30, 31);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        assertEquals("getIterationDirection()", SequenceType.LINEAR, iterator.getIterationDirection());
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a tiled image.
     * This test iterates in the lower-right corner of the image.
     * The sub-area is small enough for the iteration to happen in a single tile.
     */
    @Test
    @Ignore
    @DependsOnMethod({"testOnImage", "testOnRasterLowerRight"})
    public void testOnTileLowerRight() {
        xmin       = 55;
        ymin       = -7;
        width      = 40;
        height     = 50;
        tileWidth  = 10;
        tileHeight =  5;
        numBands   =  3;
        final Rectangle subArea = new Rectangle(97, 40, 50, 50);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        assertEquals("getIterationDirection()", SequenceType.LINEAR, iterator.getIterationDirection());
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a tiled image.
     * This test iterates in the lower-left corner of the image.
     * The sub-area is small enough for the iteration to happen in a single tile.
     */
    @Test
    @Ignore
    @DependsOnMethod({"testOnImage", "testOnRasterLowerLeft"})
    public void testOnTileLowerLeft() {
        xmin       =   2;
        ymin       = -15;
        width      =  30;
        height     =  40;
        tileWidth  =  10;
        tileHeight =   5;
        numBands   =   3;
        final Rectangle subArea = new Rectangle(0, 34, 5, 50);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        assertEquals("getIterationDirection()", SequenceType.LINEAR, iterator.getIterationDirection());
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a tiled image.
     * The sub-area is small enough for the iteration to happen in a single tile.
     */
    @Test
    @Ignore
    @DependsOnMethod({"testOnImage", "testOnRasterSubArea"})
    public void testOnTileSubArea() {
        xmin       = -5;
        ymin       =  7;
        width      = 50;
        height     = 40;
        tileWidth  = 10;
        tileHeight =  5;
        numBands   =  3;
        final Rectangle subArea = new Rectangle(16, 18, 8, 3);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        assertEquals("getIterationDirection()", SequenceType.LINEAR, iterator.getIterationDirection());
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a tiled image.
     * This test iterates in the upper-left corner of the image.
     * The sub-area is large enough for covering more than one tile.
     */
    @Test
    @Ignore
    @DependsOnMethod("testOnTileUpperLeft")
    public void testOnImageUpperLeft() {
        xmin       = -5;
        ymin       =  5;
        width      = 90;
        height     = 50;
        tileWidth  = 10;
        tileHeight =  5;
        numBands   =  3;
        final Rectangle subArea = new Rectangle(-10, -20, 40, 30);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        assertNull("getIterationDirection()", iterator.getIterationDirection());
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a tiled image.
     * This test iterates in the upper-right corner of the image.
     * The sub-area is large enough for covering more than one tile.
     */
    @Test
    @Ignore
    @DependsOnMethod("testOnTileUpperRight")
    public void testOnImageUpperRight() {
        xmin       = 20;
        ymin       =  0;
        width      = 80;
        height     = 50;
        tileWidth  = 10;
        tileHeight =  5;
        numBands   =  3;
        final Rectangle subArea = new Rectangle(80, -20, 30, 50);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        assertNull("getIterationDirection()", iterator.getIterationDirection());
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a tiled image.
     * This test iterates in the lower-right corner of the image.
     * The sub-area is large enough for covering more than one tile.
     */
    @Test
    @Ignore
    @DependsOnMethod("testOnTileLowerRight")
    public void testOnImageLowerRight() {
        xmin       = 30;
        ymin       =  0;
        width      = 70;
        height     = 50;
        tileWidth  = 10;
        tileHeight =  5;
        numBands   =  3;
        final Rectangle subArea = new Rectangle(80, 30, 50, 50);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        assertNull("getIterationDirection()", iterator.getIterationDirection());
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a tiled image.
     * This test iterates in the lower-left corner of the image.
     * The sub-area is large enough for covering more than one tile.
     */
    @Test
    @Ignore
    @DependsOnMethod("testOnTileLowerLeft")
    public void testOnImageLowerLeft() {
        xmin       =  0;
        ymin       =  0;
        width      = 70;
        height     = 50;
        tileWidth  = 10;
        tileHeight =  5;
        numBands   =  3;
        final Rectangle subArea = new Rectangle(-20, 30, 50, 50);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        assertNull("getIterationDirection()", iterator.getIterationDirection());
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a tiled image.
     * The sub-area is large enough for covering more than one tile.
     */
    @Test
    @Ignore
    @DependsOnMethod("testOnTileSubArea")
    public void testOnImageSubArea() {
        xmin       =  -5;
        ymin       =   7;
        width      = 100;
        height     =  50;
        tileWidth  =  10;
        tileHeight =   5;
        numBands   =   3;
        final Rectangle subArea = new Rectangle(20, 10, 10, 10);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        assertNull("getIterationDirection()", iterator.getIterationDirection());
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a tiled image.
     * The sub-area is large enough for covering more than one tile.
     */
    @Test
    @Ignore
    @DependsOnMethod({"testOnImage", "testOnRasterFullArea"})
    public void testOnImageFullArea() {
        xmin       =   0;
        ymin       =   0;
        width      = 100;
        height     =  50;
        tileWidth  =  10;
        tileHeight =   5;
        numBands   =   3;
        final Rectangle subArea = new Rectangle(-10, -10, 150, 80);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        assertNull("getIterationDirection()", iterator.getIterationDirection());
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area that do not intersect the image area.
     */
    @Test
    @Ignore
    @DependsOnMethod("testOnRasterEmptyArea")
    public void testOnImageEmptyArea() {
        xmin       = 0;
        ymin       = 0;
        width      = 20;
        height     = 10;
        tileWidth  = 15;
        tileHeight =  5;
        numBands   =  3;
        final Rectangle subArea = new Rectangle(-100, -50, 5, 17);
        createPixelIterator(createImage(subArea), subArea);
        assertEquals("Expected an empty set of values.", 0, expected.length);
        verifyIteration(true);
    }

    /**
     * Verifies that invoking {@link PixelIterator#moveTo(int, int, int)} with illegal indices causes
     * an exception to be thrown.
     */
    @Test
    @Ignore
    @DependsOnMethod("testIllegalMoveOnRaster")
    public void testIllegalMoveOnImage() {
        xmin       =  0;
        ymin       =  0;
        width      = 20;
        height     = 10;
        tileWidth  = 15;
        tileHeight =  5;
        numBands   =  3;
        createPixelIterator(createImage(null));
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        try {
            iterator.moveTo(102, 53, 0);
            fail("Expected IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    /**
     * Verifies that invoking {@link PixelIterator#next()} after iteration end causes an exception to be thrown.
     */
    @Test
    @Ignore
    @DependsOnMethod("testOnImage")
    public void testIllegalNext() {
        xmin       =  0;
        ymin       =  0;
        width      = 20;
        height     = 15;
        tileWidth  = 10;
        tileHeight =  5;
        numBands   =  3;
        final Rectangle subArea = new Rectangle(-10, -10, 150, 80);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIteration(false);
        try {
            iterator.next();
            fail("Expected IllegalStateException.");
        } catch (IllegalStateException e) {
            // ok
        }
    }

    /**
     * Tests {@link PixelIterator#getX()} and {@link PixelIterator#getY()}.
     */
    @Test
    @Ignore
    public void testGetXY() {
        xmin       =  56;
        ymin       =   1;
        width      =  40;
        height     =  32;
        tileWidth  =  10;
        tileHeight =   8;
        numBands   =   3;
        createPixelIterator(createImage(null));
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        int i = 0;
        for (int ty = 0; ty < height/tileHeight; ty++) {
            for (int tx = 0; tx < width/tileWidth; tx++) {
                for (int y = 0; y<tileHeight; y++) {
                    for (int x = 0; x<tileWidth; x++) {
                        assertTrue(iterator.next());
                        assertEquals("x", tx*tileWidth  + x + xmin, iterator.getX());
                        assertEquals("y", ty*tileHeight + y + ymin, iterator.getY());
                        assertEquals(expected[i], iterator.getSampleFloat(), 0f);
                        for (int b=1; b<numBands; b++) {
                            assertTrue(iterator.next());
                            assertEquals(expected[i], iterator.getSampleFloat(), 0f);
                        }
                    }
                }
            }
        }
        assertEquals("Too few elements in iteration.", expected.length, i);
    }

    /**
     * Moves the iterator to the given position and discards the {@link #expected} values prior that position.
     * This method is used for implementation of {@code testMoveXXX()} methods.
     */
    private void moveTo(int x, int y) {
        iterator.moveTo(x, y, 0);
        x -= xmin;
        y -= ymin;
        final int pixelIndex;
        if (tileWidth == 0 && tileHeight == 0) {
            pixelIndex = y*width + x;
        } else {
            final int tx = x / tileWidth;
            final int ty = y / tileHeight;
            final int numTileX = (width + tileWidth - 1) / tileWidth;
            pixelIndex = ((ty * (numTileX - 1) + tx) * tileHeight + y - tx) * tileWidth + x;
        }
        expected = Arrays.copyOfRange(expected, pixelIndex * numBands, expected.length);
    }

    /**
     * Tests iteration after a call to {@link PixelIterator#moveTo(int, int, int)} in a raster.
     */
    @Test
    @Ignore
    public void testMoveIntoRaster() {
        xmin     =  5;
        ymin     =  7;
        width    = 16;
        height   = 13;
        numBands =  3;
        createPixelIterator(createRaster(null));
        moveTo(17, 15);
        verifyIteration(false);
    }

    /**
     * Tests iteration after a call to {@link PixelIterator#moveTo(int, int, int)} in a tiled image.
     */
    @Test
    @Ignore
    @DependsOnMethod({"testOnImage", "testMoveIntoRaster"})
    public void testMoveIntoImage() {
        xmin       =  -1;
        ymin       =   3;
        width      =  60;
        height     =  50;
        tileWidth  =  10;
        tileHeight =   5;
        numBands   =   3;
        createPixelIterator(createImage(null));
        moveTo(17, 15);
        verifyIteration(false);
    }

    /**
     * Tests iteration after a call to {@link PixelIterator#moveTo(int, int, int)} in a tiled image.
     */
    @Test
    @Ignore
    @DependsOnMethod("testMoveIntoImage")
    public void testMoveToUpperLeft() {
        xmin       = -1;
        ymin       =  3;
        width      = 30;
        height     = 25;
        tileWidth  = 10;
        tileHeight =  5;
        numBands   =  3;
        createPixelIterator(createImage(null));
        moveTo(-1, 3);
        verifyIteration(false);
    }

    /**
     * Tests iteration after a call to {@link PixelIterator#moveTo(int, int, int)} in a tiled image.
     */
    @Test
    @Ignore
    @DependsOnMethod("testMoveIntoImage")
    public void testMoveToUpperRight() {
        xmin       = 69;
        ymin       =  3;
        width      = 30;
        height     = 25;
        tileWidth  = 10;
        tileHeight =  5;
        numBands   =  3;
        createPixelIterator(createImage(null));
        moveTo(98, 3);
        verifyIteration(false);
    }

    /**
     * Tests iteration after a call to {@link PixelIterator#moveTo(int, int, int)} in a tiled image.
     */
    @Test
    @Ignore
    @DependsOnMethod("testMoveIntoImage")
    public void testMoveToLowerRight() {
        xmin       = 69;
        ymin       = 28;
        width      = 30;
        height     = 25;
        tileWidth  = 10;
        tileHeight =  5;
        numBands   =  3;
        createPixelIterator(createImage(null));
        moveTo(98, 52);
        verifyIteration(false);
    }

    /**
     * Tests iteration after a call to {@link PixelIterator#moveTo(int, int, int)} in a tiled image.
     */
    @Test
    @Ignore
    @DependsOnMethod("testMoveIntoImage")
    public void testMoveToLowerLeft() {
        xmin       = -1;
        ymin       = 28;
        width      = 30;
        height     = 25;
        tileWidth  = 10;
        tileHeight =  5;
        numBands   =  3;
        createPixelIterator(createImage(null));
        moveTo(-1, 52);
        verifyIteration(false);
    }
}
