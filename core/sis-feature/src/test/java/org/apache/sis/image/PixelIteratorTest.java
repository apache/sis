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
import java.util.Optional;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.nio.DoubleBuffer;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.BandedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import org.opengis.coverage.grid.SequenceType;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.TestCase;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Base class of {@link PixelIterator} tests. This base class tests the default read-write iterator
 * on signed short integer values. Subclasses will test variants, for example optimized iterators
 * for some specific sample models.
 *
 * @author  Rémi Maréchal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
public strictfp class PixelIteratorTest extends TestCase {
    /**
     * The pixel iterator being tested.
     * This field is initialized by a call to one of the {@code createPixelIterator(…)} methods.
     */
    WritablePixelIterator iterator;

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
     * This is zero if the tests are performed on a raster instead of a tiled image.
     */
    private int tileWidth;

    /**
     * Number of pixels along the <var>y</var> axis of each tile in image tiles.
     * This is zero if the tests are performed on a raster instead of a tiled image.
     */
    private int tileHeight;

    /**
     * The minimum tile index.
     */
    private int minTileX, minTileY;

    /**
     * The expected values as a flat, row-major array. The content of this array is a copy
     * of the sample values stored in the raster or image used as a source of test data.
     */
    private float[] expected;

    /**
     * Iteration order to request at construction time. This is not necessarily the same
     * than the actual iteration order chosen by the iterator because the iterator may
     * replace default order by a more specific one.
     */
    private final SequenceType requestedOrder;

    /**
     * {@code true} for testing write operations in addition of read operations.
     */
    boolean isWritable;

    /**
     * {@code true} for using {@link BandedSampleModel} instead of {@link PixelInterleavedSampleModel}.
     */
    boolean useBandedSampleModel;

    /**
     * Creates a new test case for the given data type.
     *
     * @param  dataType  the raster or image data type as one of the {@link DataBuffer} constants.
     */
    PixelIteratorTest(final int dataType, final SequenceType requestedOrder) {
        this.dataType = dataType;
        this.requestedOrder = requestedOrder;
    }

    /**
     * Creates a new test case.
     */
    public PixelIteratorTest() {
        dataType = DataBuffer.TYPE_SHORT;
        requestedOrder = null;
    }

    /**
     * Creates a {@code WritableRaster} to use as the source of test data.
     * The raster is filled with arbitrary sample values.
     *
     * <h4>Pre-conditions</h4>
     * Before invocation, the {@link #xmin}, {@link #ymin}, {@link #width}, {@link #height} and {@link #numBands}
     * fields must be initialized.
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
        expected = new float[StrictMath.max(subMaxX - subMinX, 0) * StrictMath.max(subMaxY - subMinY, 0) * numBands];
        final SampleModel sm;
        if (useBandedSampleModel) {
            sm = new BandedSampleModel(dataType, width, height, numBands);
        } else {
            sm = new PixelInterleavedSampleModel(dataType, width, height, numBands,
                        width * numBands, ArraysExt.range(0, numBands));
        }
        final WritableRaster raster = Raster.createWritableRaster(sm, new Point(xmin, ymin));
        /*
         * At this point, all data structures have been created an initialized to zero sample values.
         * Now fill the data structures with arbitrary values.
         */
        int n = 0;
        float value = (dataType == DataBuffer.TYPE_FLOAT) ? -100.5f : 100f;
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
            value += 10;                // Arbitrary offset.
        }
        assertEquals("Number of expected values", expected.length, n);
        return raster;
    }

    /**
     * Creates a {@code RenderedImage} to use as the source of test data.
     * The image is filled with arbitrary sample values.
     *
     * <h4>Pre-conditions</h4>
     * Before invocation, the {@link #xmin}, {@link #ymin}, {@link #width}, {@link #height},
     * {@link #tileWidth}, {@link #tileHeight} and {@link #numBands} fields must be initialized.
     *
     * @param  subArea  the image subarea on which to perform iteration, or {@code null} for the whole area.
     * @return an image filled with arbitrary sample values.
     */
    private WritableRenderedImage createImage(final Rectangle subArea) {
        assertEquals(0, width  % tileWidth);
        assertEquals(0, height % tileHeight);
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
        expected = new float[StrictMath.max(subMaxX - subMinX, 0) * StrictMath.max(subMaxY - subMinY, 0) * numBands];
        final TiledImageMock image = new TiledImageMock(dataType, numBands, xmin, ymin, width, height,
                                        tileWidth, tileHeight, minTileX, minTileY, useBandedSampleModel);
        image.validate();
        /*
         * At this point, all data structures have been created an initialized to zero sample values.
         * Now fill the data structures with arbitrary values. We fill them tile-by-tile by default,
         * unless an iteration order is specified.
         */
        int n = 0;
        float value = (dataType == DataBuffer.TYPE_FLOAT) ? -100.5f : 100f;
        final int[] coordinates = getCoordinatesInExpectedOrder(new Rectangle(xmin, ymin, width, height));
        for (int i=0; i<coordinates.length;) {
            final int x = coordinates[i++];
            final int y = coordinates[i++];
            final boolean included = (y >= subMinY && y < subMaxY) && (x >= subMinX && x < subMaxX);
            for (int b = 0; b < numBands; b++) {
                image.setSample(x, y, b, value);
                if (included) {
                    expected[n++] = value;
                }
                value++;
            }
            if (x == y) value += 10;        // Arbitrary offset.
        }
        assertEquals("Number of expected values", expected.length, n);
        return image;
    }

    /**
     * Returns the sequence of (x,y) coordinates expected for the iterator being tested.
     * This method must be overridden for each kind of iterator to test.
     *
     * @param  subArea  the ranges of pixel coordinates in which to iterate.
     * @return sequence of (x,y) tuples inside the given ranges, in the order to be traversed by the iterator.
     *
     * @see #getExpectedWindowValues(Rectangle, float[])
     */
    int[] getCoordinatesInExpectedOrder(final Rectangle subArea) {
        final int[] coordinates = new int[subArea.width * subArea.height * 2];
        final int numXTiles = (width  + tileWidth -1) / tileWidth;
        final int numYTiles = (height + tileHeight-1) / tileHeight;
        int i = 0;
        for (int ty=0; ty<numYTiles; ty++) {
            for (int tx=0; tx<numXTiles; tx++) {
                final int yNextTile = ymin + (ty+1) * tileHeight;
                for (int y = yNextTile - tileHeight; y < yNextTile; y++) {
                    final int xNextTile = xmin + (tx+1) * tileWidth;
                    for (int x = xNextTile - tileWidth; x < xNextTile; x++) {
                        coordinates[i++] = x;
                        coordinates[i++] = y;
                    }
                }
            }
        }
        assertEquals(coordinates.length, i);
        return coordinates;
    }

    /**
     * Returns the index in iteration of the given coordinates. For example a return value of 2 means
     * that the given (x,y) should be the third point in iteration (iteration starts at index zero).
     * This method must be overridden for each kind of iterator to test.
     *
     * @param  x  <var>x</var> coordinate for which the iterator position is desired.
     * @param  y  <var>y</var> coordinate for which the iterator position is desired.
     * @return point index in iterator order for the given (x,y) coordinates.
     */
    int getIndexOf(int x, int y) {
        x -= xmin;
        y -= ymin;
        if (tileWidth == 0 && tileHeight == 0) {
            return y * width + x;
        }
        final int tx = x / tileWidth;
        final int ty = y / tileHeight;
        final int numTileX = (width + tileWidth - 1) / tileWidth;
        return ((ty * (numTileX - 1) + tx) * tileHeight + y - tx) * tileWidth + x;
    }

    /**
     * Returns the bounds of the image or raster to be tested.
     * This method is provided for subclasses information purposes.
     */
    final Rectangle getImageBounds() {
        return new Rectangle(xmin, ymin, width, height);
    }

    /**
     * Returns the expected sequence type. Subclasses may need to override.
     *
     * @param  singleTile  {@code true} if iteration occurs in a single tile, or {@code false} for the whole image.
     * @return the iteration order (may be {@code null}).
     */
    SequenceType getIterationOrder(boolean singleTile) {
        return singleTile ? SequenceType.LINEAR : null;
    }

    /**
     * Verifies that actual iteration order is equal to the expected one.
     *
     * @param  singleTile  {@code true} if iteration occurs in a single tile, or {@code false} for the whole image.
     */
    private void verifyIterationOrder(final boolean singleTile) {
        assertEquals("getIterationOrder()", Optional.ofNullable(getIterationOrder(singleTile)), iterator.getIterationOrder());
    }

    /**
     * Creates a {@code PixelIterator} for a sub-area of given raster.
     * The iterator shall be assigned to the {@link #iterator} field.
     *
     * <p>The default implementation creates {@link PixelIterator} instances.
     * Tests for other kinds of iterator need to override.</p>
     *
     * @param  raster   the data on which to perform iteration.
     * @param  subArea  the boundary of the raster sub-area where to perform iteration.
     */
    void createPixelIterator(WritableRaster raster, Rectangle subArea) {
        iterator = new WritablePixelIterator(raster, isWritable ? raster : null, subArea, null, requestedOrder);
        assertEquals("getIterationOrder()", SequenceType.LINEAR, iterator.getIterationOrder().get());
        assertEquals("isWritable", isWritable, iterator.isWritable());
    }

    /**
     * Creates a {@code PixelIterator} for a sub-area of given image.
     * The iterator shall be assigned to the {@link #iterator} field.
     *
     * <p>The default implementation creates {@link PixelIterator} instances.
     * Tests for other kinds of iterator need to override.</p>
     *
     * @param  image    the data on which to perform iteration.
     * @param  subArea  the boundary of the image sub-area where to perform iteration.
     */
    void createPixelIterator(WritableRenderedImage image, Rectangle subArea) {
        iterator = new WritablePixelIterator(image, isWritable ? image : null, subArea, null, requestedOrder);
        assertEquals("isWritable", isWritable, iterator.isWritable());
    }

    /**
     * Creates a {@code PixelIterator} for a window in the given image.
     * The iterator shall be assigned to the {@link #iterator} field.
     *
     * <p>The default implementation creates {@link PixelIterator} instances.
     * Tests for other kinds of iterator need to override.</p>
     *
     * @param  image    the data on which to perform iteration.
     * @param  window   size of the window to use in {@link PixelIterator#createWindow(TransferType)} method.
     */
    void createWindowIterator(WritableRenderedImage image, Dimension window) {
        iterator = new WritablePixelIterator(image, isWritable ? image : null, null, window, requestedOrder);
        assertEquals("isWritable", isWritable, iterator.isWritable());
    }

    /**
     * Invoked after every tests for releasing resources.
     */
    @After
    public void dispose() {
        iterator.close();
    }

    /**
     * Tests {@link PixelIterator#getSampleRanges()}.
     */
    @Test
    public void testGetSampleRanges() {
        for (int type = DataBuffer.TYPE_BYTE; type <= DataBuffer.TYPE_DOUBLE; type++) {
            createPixelIterator(WritableRaster.createWritableRaster(new BandedSampleModel(type, 1, 1, 3), null), null);
            final NumberRange<?>[] ranges = iterator.getSampleRanges();
            final Number min, max;
            final Class<?> c;
            switch (type) {
                case DataBuffer.TYPE_BYTE:    c = Short.class;   min = (short) 0;         max = (short) 0xFF;      break;
                case DataBuffer.TYPE_USHORT:  c = Integer.class; min = 0;                 max = 0xFFFF;            break;
                case DataBuffer.TYPE_SHORT:   c = Short.class;   min = Short.MIN_VALUE;   max = Short.MAX_VALUE;   break;
                case DataBuffer.TYPE_INT:     c = Integer.class; min = Integer.MIN_VALUE; max = Integer.MAX_VALUE; break;
                case DataBuffer.TYPE_FLOAT:   c = Float.class;   min = null;              max = null;              break;
                case DataBuffer.TYPE_DOUBLE:  c = Double.class;  min = null;              max = null;              break;
                default: throw new AssertionError(type);
            }
            assertEquals(ranges.length, 3);
            for (final NumberRange<?> r : ranges) {
                assertEquals(c,   r.getElementType());
                assertEquals(min, r.getMinValue());
                assertEquals(max, r.getMaxValue());
            }
            iterator.close();
        }
    }

    /**
     * Verifies the sample value at current iterator position.
     * If the iterator is writable, tests also setting a value.
     *
     * @param  i  index in {@link #expected} array.
     * @param  b  band number at current iterator position.
     */
    private void verifySample(final int i, final int b) {
        final float e = expected[i];
        final float a = iterator.getSampleFloat(b);
        if (Float.floatToRawIntBits(a) != Float.floatToRawIntBits(e)) {
            fail("Pixel iteration at index " + i + ": expected " + e + " but got " + a);
        }
        if (isWritable) {
            final float newValue = a + 20;
            iterator.setSample(b, newValue);
            assertEquals("Verification after write", newValue, iterator.getSampleFloat(b), 0f);
            expected[i] = newValue;
        }
    }

    /**
     * Iterates over all values returned by the current {@link #iterator} and compares with expected values.
     *
     * @param  verifyIndices  whether to verify also iterator {@code getPosition()} return values.
     *                        This is usually {@code true} if and only if the iterator covers the full raster area.
     *
     * @see #verifyIterationAfterMove(int, int)
     * @see #verifyWindow(Dimension)
     */
    private void verifyIteration(final boolean verifyIndices) {
        int i = 0;
        while (iterator.next()) {
            for (int b=0; b<numBands; b++) {
                verifySample(i, b);
                if (verifyIndices) {
                    final int p = i / numBands;
                    final Point position = iterator.getPosition();
                    assertEquals("x", (p % width) + xmin, position.x);
                    assertEquals("y", (p / width) + ymin, position.y);
                }
                i++;
            }
        }
        assertEquals("Too few elements in iteration.", expected.length, i);
    }

    /**
     * Tests iteration over all pixels in a single raster.
     * Raster location and number of bands are different than other tests (each test uses arbitrary values).
     */
    @Test
    public void testOnRaster() {
        width    =  7;
        height   = 10;
        numBands =  3;
        createPixelIterator(createRaster(null), null);
        verifyIteration(true);
    }

    /**
     * Tests {@link PixelIterator#rewind()}.
     * Raster location and number of bands are different than other tests (each test uses arbitrary values).
     */
    @Test
    @DependsOnMethod("testOnRaster")
    public void testRasterRewind() {
        xmin     = -3;
        ymin     = -5;
        width    =  8;
        height   =  7;
        numBands =  2;
        createPixelIterator(createRaster(null), null);
        verifyIteration(true);

        iterator.rewind();
        verifyIteration(true);
    }

    /**
     * Tests iteration over a sub-area in a single raster.
     * This test iterates in the upper-left corner of the raster.
     * Raster location and number of bands are different than other tests (each test uses arbitrary values).
     */
    @Test
    @DependsOnMethod("testOnRaster")
    public void testOnRasterUpperLeft() {
        xmin     =  5;
        ymin     =  7;
        width    =  9;
        height   =  8;
        numBands =  3;
        final Rectangle subArea = new Rectangle(4, 6, 5, 4);
        createPixelIterator(createRaster(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a single raster.
     * This test iterates in the upper-right corner of the raster.
     * Raster location and number of bands are different than other tests (each test uses arbitrary values).
     */
    @Test
    @DependsOnMethod("testOnRaster")
    public void testOnRasterUpperRight() {
        xmin     = 11;
        ymin     = 12;
        width    = 10;
        height   = 11;
        numBands =  1;
        final Rectangle subArea = new Rectangle(16, 9, 10, 6);
        createPixelIterator(createRaster(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a single raster.
     * This test iterates in the lower-right corner of the raster.
     * Raster location and number of bands are different than other tests (each test uses arbitrary values).
     */
    @Test
    @DependsOnMethod("testOnRaster")
    public void testOnRasterLowerRight() {
        xmin     = -4;
        ymin     = -6;
        width    =  8;
        height   =  8;
        numBands =  4;
        final Rectangle subArea = new Rectangle(2, -2, 10, 12);
        createPixelIterator(createRaster(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a single raster.
     * This test iterates in the lower-left corner of the raster.
     * Raster location and number of bands are different than other tests (each test uses arbitrary values).
     */
    @Test
    @DependsOnMethod("testOnRaster")
    public void testOnRasterLowerLeft() {
        xmin     =  6;
        ymin     =  7;
        width    =  5;
        height   =  9;
        numBands =  3;
        final Rectangle subArea = new Rectangle(3, 10, 4, 9);
        createPixelIterator(createRaster(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a single raster.
     * Raster location and number of bands are different than other tests (each test uses arbitrary values).
     * This method tests also {@link PixelIterator#rewind()}.
     */
    @Test
    @DependsOnMethod({"testOnRaster", "testRasterRewind"})
    public void testOnRasterSubArea() {
        xmin     =  5;
        ymin     =  7;
        width    = 17;
        height   = 16;
        numBands =  2;
        final Rectangle subArea = new Rectangle(10, 9, 8, 6);
        createPixelIterator(createRaster(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIteration(false);

        iterator.rewind();
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area that actually contains all the raster area.
     * Raster location and number of bands are different than other tests (each test uses arbitrary values).
     * This method tests also {@link PixelIterator#rewind()}.
     */
    @Test
    @DependsOnMethod({"testOnRaster", "testRasterRewind"})
    public void testOnRasterFullArea() {
        xmin     =  7;
        ymin     =  9;
        width    = 11;
        height   = 17;
        numBands =  3;
        final Rectangle subArea = new Rectangle(2, 3, 25, 17);
        createPixelIterator(createRaster(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIteration(true);

        iterator.rewind();
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area that do not intersect the raster area.
     * Raster location and number of bands are different than other tests (each test uses arbitrary values).
     * This method tests also {@link PixelIterator#rewind()}.
     */
    @Test
    @DependsOnMethod({"testOnRaster", "testRasterRewind"})
    public void testOnRasterEmptyArea() {
        xmin     = 6;
        ymin     = 5;
        width    = 3;
        height   = 2;
        numBands = 3;
        final Rectangle subArea = new Rectangle(-17, -20, 5, 15);
        createPixelIterator(createRaster(subArea), subArea);
        assertEquals("Expected an empty set of values.", 0, expected.length);
        verifyIteration(true);

        iterator.rewind();
        verifyIteration(false);
    }

    /**
     * Verifies that invoking {@link PixelIterator#moveTo(int, int)} with illegal indices causes
     * an exception to be thrown.
     */
    @Test
    public void testIllegalMoveOnRaster() {
        xmin     =  4;
        ymin     =  7;
        width    =  5;
        height   =  4;
        numBands =  1;
        createPixelIterator(createRaster(null), null);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        try {
            iterator.moveTo(2, 3);
            fail("Expected IndexOutOfBoundsException.");
        } catch (IndexOutOfBoundsException e) {
            assertNotNull(e.getMessage());
        }
        try {
            iterator.moveTo(9, 3);
            fail("Expected IndexOutOfBoundsException.");
        } catch (IndexOutOfBoundsException e) {
            assertNotNull(e.getMessage());
        }
        try {
            iterator.moveTo(2, 10);
            fail("Expected IndexOutOfBoundsException.");
        } catch (IndexOutOfBoundsException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Tests iteration over all pixels in a tiled image.
     * Image location and number of bands are different than other tests (each test uses arbitrary values).
     * This method tests also {@link PixelIterator#rewind()}.
     */
    @Test
    @DependsOnMethod("testOnRaster")
    public void testOnImage() {
        width      =  24;
        height     =  15;
        tileWidth  =   8;
        tileHeight =   5;
        numBands   =   3;
        createPixelIterator(createImage(null), null);
        verifyIterationOrder(false);
        verifyIteration(false);
    }

    /**
     * Tests iteration over all pixels in a tiled image.
     * Image location and number of bands are different than other tests (each test uses arbitrary values).
     * This method tests also {@link PixelIterator#rewind()}.
     */
    @Test
    @DependsOnMethod("testOnImage")
    public void testImageRewind() {
        xmin       =   1;
        ymin       =  -4;
        width      =  24;
        height     =  15;
        tileWidth  =   8;
        tileHeight =   5;
        numBands   =   2;
        createPixelIterator(createImage(null), null);
        verifyIterationOrder(false);
        verifyIteration(false);

        iterator.rewind();
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a tiled image.
     * This test iterates in the upper-left corner of the image.
     * The sub-area is small enough for the iteration to happen in a single tile.
     * Image location and number of bands are different than other tests (each test uses arbitrary values).
     */
    @Test
    @DependsOnMethod({"testOnImage", "testOnRasterUpperLeft"})
    public void testOnTileUpperLeft() {
        xmin       = -5;
        ymin       =  5;
        width      = 20;
        height     = 10;
        tileWidth  =  4;
        tileHeight =  5;
        numBands   =  1;
        minTileX   = -1;
        final Rectangle subArea = new Rectangle(-10, -20, 8, 28);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIterationOrder(true);
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a tiled image.
     * This test iterates in the upper-right corner of the image.
     * The sub-area is small enough for the iteration to happen in a single tile.
     * Image location and number of bands are different than other tests (each test uses arbitrary values).
     */
    @Test
    @DependsOnMethod({"testOnImage", "testOnRasterUpperRight"})
    public void testOnTileUpperRight() {
        xmin       = 35;
        ymin       =  7;
        width      = 15;
        height     = 12;
        tileWidth  =  5;
        tileHeight =  3;
        numBands   =  3;
        minTileY   = -2;
        final Rectangle subArea = new Rectangle(45, -20, 30, 29);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIterationOrder(true);
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a tiled image.
     * This test iterates in the lower-right corner of the image.
     * The sub-area is small enough for the iteration to happen in a single tile.
     * Image location and number of bands are different than other tests (each test uses arbitrary values).
     */
    @Test
    @DependsOnMethod({"testOnImage", "testOnRasterLowerRight"})
    public void testOnTileLowerRight() {
        xmin       = 55;
        ymin       = -7;
        width      = 18;
        height     = 16;
        tileWidth  =  6;
        tileHeight =  4;
        numBands   =  2;
        minTileX   = 12;
        minTileY   = 20;
        final Rectangle subArea = new Rectangle(68, 5, 4, 4);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIterationOrder(true);
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a tiled image.
     * This test iterates in the lower-left corner of the image.
     * The sub-area is small enough for the iteration to happen in a single tile.
     * Image location and number of bands are different than other tests (each test uses arbitrary values).
     */
    @Test
    @DependsOnMethod({"testOnImage", "testOnRasterLowerLeft"})
    public void testOnTileLowerLeft() {
        xmin       =   2;
        ymin       = -15;
        width      =  21;
        height     =  20;
        tileWidth  =   7;
        tileHeight =   5;
        numBands   =   3;
        minTileX   = -12;
        minTileY   =  20;
        final Rectangle subArea = new Rectangle(0, 0, 9, 50);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIterationOrder(true);
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a tiled image.
     * The sub-area is small enough for the iteration to happen in a single tile.
     */
    @Test
    @DependsOnMethod({"testOnImage", "testOnRasterSubArea"})
    public void testOnTileSubArea() {
        xmin       = -5;
        ymin       =  7;
        width      = 15;
        height     = 24;
        tileWidth  =  5;
        tileHeight =  6;
        numBands   =  2;
        minTileX   =  2;
        minTileY   =  3;
        final Rectangle subArea = new Rectangle(6, 20, 4, 5);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIterationOrder(true);
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a tiled image.
     * This test iterates in the upper-left corner of the image.
     * The sub-area is large enough for covering more than one tile.
     */
    @Test
    @DependsOnMethod("testOnTileUpperLeft")
    public void testOnImageUpperLeft() {
        xmin       = -5;
        ymin       =  5;
        width      = 27;
        height     = 20;
        tileWidth  =  9;
        tileHeight =  5;
        numBands   =  3;
        minTileX   =  8;
        minTileY   =  8;
        final Rectangle subArea = new Rectangle(-10, -5, 25, 22);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIterationOrder(false);
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a tiled image.
     * This test iterates in the upper-right corner of the image.
     * The sub-area is large enough for covering more than one tile.
     */
    @Test
    @DependsOnMethod("testOnTileUpperRight")
    public void testOnImageUpperRight() {
        xmin       = 20;
        ymin       =  0;
        width      = 25;
        height     = 24;
        tileWidth  =  5;
        tileHeight =  6;
        numBands   =  2;
        minTileX   = -2;
        minTileY   = 20;
        final Rectangle subArea = new Rectangle(27, -20, 30, 37);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIterationOrder(false);
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a tiled image.
     * This test iterates in the lower-right corner of the image.
     * The sub-area is large enough for covering more than one tile.
     */
    @Test
    @DependsOnMethod("testOnTileLowerRight")
    public void testOnImageLowerRight() {
        xmin       = 30;
        ymin       =  1;
        width      = 15;
        height     = 12;
        tileWidth  =  3;
        tileHeight =  4;
        numBands   =  4;
        minTileX   =  1;
        minTileY   =  2;
        final Rectangle subArea = new Rectangle(36, 8, 12, 20);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIterationOrder(false);
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a tiled image.
     * This test iterates in the lower-left corner of the image.
     * The sub-area is large enough for covering more than one tile.
     */
    @Test
    @DependsOnMethod("testOnTileLowerLeft")
    public void testOnImageLowerLeft() {
        xmin       = -2;
        ymin       = -7;
        width      = 15;
        height     = 16;
        tileWidth  =  5;
        tileHeight =  4;
        numBands   =  1;
        minTileX   = -9;
        minTileY   = -9;
        final Rectangle subArea = new Rectangle(-20, -1, 30, 20);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIterationOrder(false);
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area in a tiled image.
     * The sub-area is large enough for covering more than one tile.
     */
    @Test
    @DependsOnMethod("testOnTileSubArea")
    public void testOnImageSubArea() {
        xmin       =  -5;
        ymin       =   7;
        width      =  70;
        height     =  48;
        tileWidth  =   7;
        tileHeight =   4;
        numBands   =   2;
        minTileX   = -12;
        minTileY   = -20;
        final Rectangle subArea = new Rectangle(20, 10, 30, 25);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIterationOrder(false);
        verifyIteration(false);
    }

    /**
     * Tests iteration over a all the region of a tiled image.
     */
    @Test
    @DependsOnMethod({"testOnImage", "testOnRasterFullArea"})
    public void testOnImageFullArea() {
        xmin       =  -5;
        ymin       =  -3;
        width      =  60;
        height     =  50;
        tileWidth  =   6;
        tileHeight =   5;
        numBands   =   1;
        minTileX   = 999;
        minTileY   = -99;
        final Rectangle subArea = new Rectangle(-10, -10, 150, 80);
        createPixelIterator(createImage(subArea), subArea);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIterationOrder(false);
        verifyIteration(false);
    }

    /**
     * Tests iteration over a sub-area that do not intersect the image area.
     */
    @Test
    @DependsOnMethod("testOnRasterEmptyArea")
    public void testOnImageEmptyArea() {
        xmin       = 5;
        ymin       = 6;
        width      = 8;
        height     = 9;
        tileWidth  = 2;
        tileHeight = 3;
        numBands   = 2;
        final Rectangle subArea = new Rectangle(-100, -50, 5, 17);
        createPixelIterator(createImage(subArea), subArea);
        assertEquals("Expected an empty set of values.", 0, expected.length);
        verifyIteration(true);
    }

    /**
     * Verifies that invoking {@link PixelIterator#moveTo(int, int)} with illegal indices causes
     * an exception to be thrown.
     */
    @Test
    @DependsOnMethod("testIllegalMoveOnRaster")
    public void testIllegalMoveOnImage() {
        xmin       =  0;
        ymin       =  0;
        width      =  8;
        height     =  6;
        tileWidth  =  4;
        tileHeight =  3;
        numBands   =  1;
        createPixelIterator(createImage(null), null);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        try {
            iterator.moveTo(102, 53);
            fail("Expected IndexOutOfBoundsException.");
        } catch (IndexOutOfBoundsException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Verifies that invoking {@link PixelIterator#next()} after iteration end causes an exception to be thrown.
     */
    @Test
    @DependsOnMethod("testOnImage")
    public void testIllegalNext() {
        xmin       = -1;
        ymin       =  3;
        width      =  8;
        height     =  6;
        tileWidth  =  4;
        tileHeight =  3;
        numBands   =  1;
        createPixelIterator(createImage(null), null);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIteration(false);
        try {
            iterator.next();
            fail("Expected IllegalStateException.");
        } catch (IllegalStateException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Tests {@link PixelIterator#getPosition()}.
     */
    @Test
    public void testGetPosition() {
        xmin       =  56;
        ymin       =   1;
        width      =  20;
        height     =  24;
        tileWidth  =   5;
        tileHeight =   8;
        numBands   =   2;
        minTileX   =  10;
        minTileY   = 100;
        createPixelIterator(createImage(null), null);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        final int[] coordinates = getCoordinatesInExpectedOrder(new Rectangle(xmin, ymin, width, height));
        int i = 0;
        for (int j=0; j<coordinates.length;) {
            final int x = coordinates[j++];
            final int y = coordinates[j++];
            assertTrue(iterator.next());
            final Point position = iterator.getPosition();
            assertEquals("x", x, position.x);
            assertEquals("y", y, position.y);
            for (int b=0; b<numBands; b++) {
                assertEquals(expected[i++], iterator.getSampleFloat(b), 0f);
            }
        }
        assertEquals("Too few elements in iteration.", expected.length, i);
    }

    /**
     * Moves the iterator to the given position and verifies the iteration.
     * This method is used for implementation of {@code testMoveXXX()} methods.
     *
     * @see #verifyIteration(boolean)
     * @see #verifyWindow(Dimension)
     */
    private void verifyIterationAfterMove(final int x, final int y) {
        /*
         * Move the iterator and verify location after the move.
         */
        iterator.moveTo(x, y);
        final Point p = iterator.getPosition();
        assertEquals("x", x, p.x);
        assertEquals("y", y, p.y);
        /*
         * Compute index of the (x,y) position in the array of expected values.
         * Iteration verification will need to begin at that value.
         */
        int i = getIndexOf(x, y) * numBands;
        /*
         * Iteration verification happens here. Note that contrarily to 'verifyIteration(boolean)' method,
         * we use a do … while loop instead of a while loop because the call to 'moveTo(x, y)' should be
         * understood as an implicit 'next()' method call.
         */
        do {
            for (int b=0; b<numBands; b++) {
                verifySample(i++, b);
            }
        } while (iterator.next());
        assertEquals("Too few elements in iteration.", expected.length, i);
    }

    /**
     * Tests iteration after a call to {@link PixelIterator#moveTo(int, int)} in a raster.
     */
    @Test
    @DependsOnMethod("testOnRaster")
    public void testMoveIntoRaster() {
        xmin     =  5;
        ymin     =  7;
        width    =  8;
        height   =  9;
        numBands =  2;
        createPixelIterator(createRaster(null), null);
        verifyIterationAfterMove(8, 10);
    }

    /**
     * Tests iteration after a call to {@link PixelIterator#moveTo(int, int)} in a tiled image.
     */
    @Test
    @DependsOnMethod({"testOnImage", "testMoveIntoRaster"})
    public void testMoveIntoImage() {
        xmin       =  -1;
        ymin       =   3;
        width      =  12;
        height     =  15;
        tileWidth  =   4;
        tileHeight =   5;
        numBands   =   1;
        minTileX   = 120;
        minTileY   = 200;
        createPixelIterator(createImage(null), null);
        verifyIterationAfterMove(7, 5);
    }

    /**
     * Verifies {@link PixelIterator#createWindow(TransferType)}.
     * This method assumes that the iterator traverses the full image (no sub-area).
     * All 3 window types are tested, but values are not necessarily fetched at each iteration step.
     * Fetching values or not is determined randomly for testing {@code Window} capability to reuse
     * values from previous iteration step.
     *
     * @see #verifyIteration(boolean)
     * @see #verifyIterationAfterMove(int, int)
     */
    private void verifyWindow(final Dimension window) {
        final Random random = TestUtilities.createRandomNumberGenerator();
        final WindowVerifier verifier = new WindowVerifier(window);
        while (iterator.next()) {
            final Point pos = iterator.getPosition();
            pos.translate(-xmin, -ymin);
            verifier.test(pos, random.nextBoolean(), random.nextBoolean(), random.nextBoolean());
        }
        /*
         * Test again, but moving the window at random positions.
         */
        iterator.rewind();
        for (int i=0; i<50; i++) {
            final int x = random.nextInt(width  - window.width);
            final int y = random.nextInt(height - window.height);
            iterator.moveTo(x + xmin, y + ymin);
            verifier.test(new Point(x,y), true, true, true);
        }
    }

    /**
     * Helper class for testing {@link org.apache.sis.image.PixelIterator.Window} values
     * at current iterator position. This class tests all 3 window types.
     */
    private final class WindowVerifier {
        private final Dimension window;
        private final float[] windowValues;
        private final PixelIterator.Window<IntBuffer> wi;
        private final PixelIterator.Window<FloatBuffer> wf;
        private final PixelIterator.Window<DoubleBuffer> wd;

        /**
         * Creates a new verifier for windows of the given size.
         */
        WindowVerifier(final Dimension window) {
            this.window = window;
            wi = iterator.createWindow(TransferType.INT);
            wf = iterator.createWindow(TransferType.FLOAT);
            wd = iterator.createWindow(TransferType.DOUBLE);
            windowValues = new float[window.width * window.height * numBands];
        }

        /**
         * Tests window values at current iterator position.
         *
         * @param  pos  (0,0)-based position of expected values.
         * @param  ti   whether to test {@code int} values.
         * @param  tf   whether to test {@code float} values.
         * @param  td   whether to test {@code double} values.
         */
        public void test(final Point pos, final boolean ti, final boolean tf, final boolean td) {
            getExpectedWindowValues(new Rectangle(pos, window), windowValues);
            if (ti) wi.update();
            if (tf) wf.update();
            if (td) wd.update();
            int indexOfExpected = 0;
            for (int y=0; y<window.height; y++) {
                for (int x=0; x<window.width; x++) {
                    for (int b=0; b<numBands; b++) {
                        final float expected = windowValues[indexOfExpected++];
                        TransferType<?> type = null;
                        float actual = Float.NaN;
                        boolean success = true;
                        if (tf) {
                            type    = TransferType.FLOAT;
                            actual  = wf.values.get();
                            success = Float.floatToRawIntBits(actual) == Float.floatToRawIntBits(expected);
                        }
                        if (success) {
                            if (td) {
                                type    = TransferType.DOUBLE;
                                actual  = (float) wd.values.get();
                                success = Float.floatToRawIntBits(actual) == Float.floatToRawIntBits(expected);
                            }
                            if (success) {
                                if (ti) {
                                    type    = TransferType.INT;
                                    actual  = wi.values.get();
                                    success = !(StrictMath.abs(actual - expected) >= 1);  // Use `!` for accepting NaN.
                                }
                                if (success) {
                                    continue;
                                }
                            }
                        }
                        fail("Type " + type + " index (" + x + ", " + y + ") in window starting at index (" +
                             pos.x + ", " + pos.y + "), band " + b + ": expected " + expected + " but got " + actual);
                    }
                }
            }
            if (ti) assertEquals("buffer.remaining()", 0, wi.values.remaining());
            if (tf) assertEquals("buffer.remaining()", 0, wf.values.remaining());
            if (td) assertEquals("buffer.remaining()", 0, wd.values.remaining());
        }
    }

    /**
     * Returns the values of the given sub-region, organized in a {@link SequenceType#LINEAR} fashion.
     * This method is invoked for {@link #verifyWindow(Dimension)} purpose. This method is responsible
     * for reordering the {@link #expected} values in a linear order.
     *
     * @param  window  the sub-region for which to get values in a linear fashion.
     * @param  values  where to store the expected window values in linear order.
     */
    void getExpectedWindowValues(final Rectangle window, final float[] values) {
        final int tileSize   = tileWidth * tileHeight;
        final int tileStride = tileSize * (width / tileWidth);
        int index = 0;
        for (int y=0; y<window.height; y++) {
            int p,t;
            p  = window.y + y;
            t  = p / tileHeight;
            p %=     tileHeight;
            final int start = t * tileStride + p * tileWidth;
            for (int x=0; x<window.width; x++) {
                p  = window.x + x;
                t  = p / tileWidth;
                p %=     tileWidth;
                final int offset = start + t * tileSize + p;
                copyExpectedPixels(offset, values, index++, 1);
            }
        }
        assertEquals(values.length, index * numBands);
    }

    /**
     * Copies the expected values of all bands of pixels starting at the given index.
     * The index arguments are indices of the points (not the indices of sample values);
     * the number of bands will be multiplied to all given arguments.
     *
     * <p>This is a helper method for {@link #getExpectedWindowValues(Rectangle, float[])}
     * implementation by subclasses.</p>
     *
     * @param  srcPts       index of the first pixel to copy.
     * @param  destination  where to copy pixel values.
     * @param  dstPts       index of the first pixel to write in the destination array.
     * @param  numPixels    number of pixels to write.
     */
    final void copyExpectedPixels(final int srcPts, final float[] destination, final int dstPts, final int numPixels) {
        System.arraycopy(expected, srcPts * numBands, destination, dstPts * numBands, numPixels * numBands);
    }

    /**
     * Tests {@link PixelIterator#createWindow(TransferType)} on a single tile.
     */
    @Test
    @DependsOnMethod("testMoveIntoImage")
    public void testWindowOnTile() {
        xmin       =   1;
        ymin       =  -2;
        width      =   8;
        height     =  10;
        numBands   =   2;
        tileWidth  = width;
        tileHeight = height;
        final Dimension window = new Dimension(3, 4);
        createWindowIterator(createImage(null), window);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIterationOrder(true);
        verifyWindow(window);
    }

    /**
     * Tests {@link PixelIterator#createWindow(TransferType)} on a tiled image.
     */
    @Test
    @DependsOnMethod("testWindowOnTile")
    public void testWindowOnImage() {
        xmin       =   1;
        ymin       =  -2;
        width      =   9;
        height     =  12;
        tileWidth  =   3;
        tileHeight =   4;
        numBands   =   2;
        minTileX   = 100;
        minTileY   = 200;
        final Dimension window = new Dimension(2, 3);
        createWindowIterator(createImage(null), window);
        assertTrue("Expected a non-empty set of values.", expected.length != 0);
        verifyIterationOrder(false);
        verifyWindow(window);
    }

    /**
     * Tests write operations in a single raster.
     * The destination image is the same than the source image.
     */
    @Test
    @DependsOnMethod("testOnRasterSubArea")
    public void testOnWritableRaster() {
        isWritable = true;
        testOnRasterSubArea();
    }

    /**
     * Tests write operations in a single tile of an image.
     * The destination image is the same than the source image.
     */
    @Test
    @DependsOnMethod({"testOnWritableRaster", "testOnTileSubArea"})
    public void testOnWritableTile() {
        isWritable = true;
        testOnTileSubArea();
    }

    /**
     * Tests write operations in a tiled image.
     * The destination image is the same than the source image.
     */
    @Test
    @DependsOnMethod({"testOnWritableTile", "testOnImageSubArea"})
    public void testOnWritableImage() {
        isWritable = true;
        testOnImageSubArea();
    }

    /**
     * Tests iterator on an area.
     */
    @Test
    public void testEmpty() {
        tileWidth  = width  = 3;
        tileHeight = height = 2;
        numBands            = 1;
        final Rectangle subArea = new Rectangle(5, 1, 3, 2);    // No intersection with image bounds.
        createPixelIterator(createImage(subArea), subArea);
        assertEquals(0, expected.length);
        verifyIteration(true);
    }
}
