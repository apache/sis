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
package org.apache.sis.image.processing.isoline;

import java.util.Map;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.image.DataBuffer;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.image.privy.RasterFactory;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link Isolines}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class IsolinesTest extends TestCase {
    /**
     * Tolerance threshold for rounding errors. Needs to take in account that
     * {@link org.apache.sis.geometry.wrapper.j2d.Polyline} stores coordinate
     * values a single-precision {@code float} numbers.
     */
    private static final double TOLERANCE = 1E-8;

    /**
     * The threshold value. This is used by {@code generateFromXXX(…)} methods.
     */
    private double threshold;

    /**
     * The isoline being tested. This is set by {@code generateFromXXX(…)} methods.
     */
    private Shape isoline;

    /**
     * Creates a new test case.
     */
    public IsolinesTest() {
    }

    /**
     * Tests isolines computed in a contouring grid having only one cell.
     * The cell may have zero, one or two line segments.
     *
     * @throws TransformException if a point cannot be transformed to its final coordinate space.
     */
    @Test
    public void testSingleCell() throws TransformException {
        threshold = -5;
        generateFromCell(0, 0, 0, 0);
        assertNull(isoline);

        threshold = 2;
        generateFromCell(0, 0, 0, 0);
        assertNull(isoline);
        /*
         *   ▘5╌╌╌╌1
         *    ╎    ╎
         *    0╌╌╌╌0
         */
        generateFromCell(5, 1, 0, 0);
        assertSegmentEquals(0, 0.6, 0.75, 0);
        /*
         *    0╌╌╌╌5▝
         *    ╎    ╎
         *   ▖10╌╌20▗
         */
        generateFromCell(0, 5, 10, 20);
        assertSegmentEquals(0, 0.2, 0.4, 0);
        /*
         *    1╌╌╌╌5▝
         *    ╎    ╎
         *    0╌╌╌╌0
         */
        generateFromCell(1, 5, 0, 0);
        assertSegmentEquals(0.25, 0, 1, 0.6);
        /*
         *   ▘5╌╌╌╌0
         *    ╎    ╎
         *   ▖20╌╌10▗
         */
        generateFromCell(5, 0, 20, 10);
        assertSegmentEquals(0.6, 0, 1, 0.2);
        /*
         *    1╌╌╌╌0
         *    ╎    ╎
         *   ▖5╌╌╌10▗
         */
        generateFromCell(1, 0, 5, 10);
        assertSegmentEquals(0, 0.25, 1, 0.2);
        /*
         *   ▘5╌╌╌10▝
         *    ╎    ╎
         *    1╌╌╌╌0
         */
        generateFromCell(5, 10, 1, 0);
        assertSegmentEquals(0, 0.75, 1, 0.8);
        /*
         *    0╌╌╌╌0
         *    ╎    ╎
         *    1╌╌╌╌5▗
         */
        generateFromCell(0, 0, 1, 5);
        assertSegmentEquals(1, 0.4, 0.25, 1);
        /*
         *   ▘20╌╌10▝
         *    ╎    ╎
         *   ▖5╌╌╌╌0
         */
        generateFromCell(20, 10, 5, 0);
        assertSegmentEquals(1, 0.8, 0.6, 1);
        /*
         *    1╌╌╌11▝
         *    ╎    ╎
         *   ▖5╌╌╌╌1
         */
        generateFromCell(1, 11, 5, 1);
        assertSegmentsEqual(0, 0.25, 0.1,  0,
                            1, 0.9,  0.75, 1);
        /*
         *   ▘11╌╌╌1
         *    ╎    ╎
         *    1╌╌╌╌5▗
         */
        generateFromCell(11, 1, 1, 5);
        assertSegmentsEqual(0.9, 0, 1, 0.25,
                            0, 0.9, 0.25, 1);
        /*
         *   ▘5╌╌╌╌1
         *    ╎    ╎
         *   ▖10╌╌╌0
         */
        generateFromCell(5, 1, 10, 0);
        assertSegmentEquals(0.75, 0, 0.8, 1);
        /*
         *    1╌╌╌╌5▝
         *    ╎    ╎
         *    0╌╌╌10▗
         */
        generateFromCell(1, 5, 0, 10);
        assertSegmentEquals(0.25, 0, 0.2, 1);
        /*
         *    0╌╌╌╌0
         *    ╎    ╎
         *   ▖5╌╌╌╌1
         */
        generateFromCell(0, 0, 5, 1);
        assertSegmentEquals(0, 0.4, 0.75, 1);
        /*
         *   ▘10╌╌20▝
         *    ╎    ╎
         *    0╌╌╌╌5▗
         */
        generateFromCell(10, 20, 0, 5);
        assertSegmentEquals(0, 0.8, 0.4, 1);
    }

    /**
     * Tests isolines computed in a contouring grid having 2×2 cells.
     *
     * @throws TransformException if a point cannot be transformed to its final coordinate space.
     */
    @Test
    public void testMultiCells() throws TransformException {
        threshold = 0.5;
        generateFromImage(3,
             0,0,0,
             0,1,0,
             0,0,0);
        verifyIsolineFromMultiCells(isoline);
    }

    /**
     * Verifies the isoline generated for level 0.5 on an image of 3×3 pixels having value 1 in the center
     * and value zero everywhere else. This is the isoline tested by {@link #testMultiCells()}.
     *
     * @param  isoline  the isoline to verify.
     */
    public static void verifyIsolineFromMultiCells(final Shape isoline) {
        /*
         * Expected coordinates:
         *
         *    (1):  1.5  1.0               (2)
         *    (2):  1.0  0.5            (3)   (1)
         *    (3):  0.5  1.0               (4)
         *    (4):  1.0  1.5
         */
        final double[] buffer = new double[2];
        final PathIterator it = isoline.getPathIterator(null);
        assertSegmentEquals(it, buffer, 1.5, 1, 1, 0.5);

        assertFalse(it.isDone());
        assertEquals(PathIterator.SEG_LINETO, it.currentSegment(buffer));
        assertEquals(0.5, buffer[0], TOLERANCE, "x2");
        assertEquals(1,   buffer[1], TOLERANCE, "y2");

        it.next();
        assertFalse(it.isDone());
        assertEquals(PathIterator.SEG_LINETO, it.currentSegment(buffer));
        assertEquals(1,   buffer[0], TOLERANCE, "x3");
        assertEquals(1.5, buffer[1], TOLERANCE, "y3");

        it.next();
        assertFalse(it.isDone());
        assertEquals(PathIterator.SEG_CLOSE, it.currentSegment(buffer));

        it.next();
        assertTrue(it.isDone());
    }

    /**
     * Tests isolines computed in a contouring grid having more than one band.
     * The same values as {@link #testSingleCell()} are used.
     *
     * @throws TransformException if a point cannot be transformed to its final coordinate space.
     */
    @Test
    public void testSingleCellMultiBands() throws TransformException {
        final BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        final WritableRaster raster = image.getRaster();
        threshold = 2;
        /*
         *    0╌╌╌╌5▝       1╌╌╌╌5▝      ▘11╌╌╌1
         *    ╎    ╎        ╎    ╎        ╎    ╎
         *   ▖10╌╌20▗       0╌╌╌╌0        1╌╌╌╌5▗
         */
        raster.setPixel(0, 0, new int[] { 0, 1, 11});
        raster.setPixel(1, 0, new int[] { 5, 5,  1});
        raster.setPixel(0, 1, new int[] {10, 0,  1});
        raster.setPixel(1, 1, new int[] {20, 0,  5});
        final Isolines[] isolines = Isolines.generate(image, new double[][] {{threshold}}, null);
        assertEquals(3, isolines.length, "Number of bands");
        for (int b=0; b<3; b++) {
            final Map<Double, Shape> polylines = isolines[b].polylines();
            assertEquals(1, polylines.size());
            isoline = polylines.get(threshold);
            switch (b) {
                case 0: assertSegmentEquals(0,  0.2, 0.4, 0); break;
                case 1: assertSegmentEquals(0.25, 0, 1, 0.6); break;
                case 2: assertSegmentsEqual(0.9,  0, 1, 0.25,
                                            0, 0.9, 0.25, 1);
            }
        }
    }

    /**
     * Tests isolines computed in a contouring grid having more than one band.
     * The same values as {@link #testMultiCells()} are used, but it tests a different
     * code path because {@link Isolines} contains a special case for one-banded image.
     *
     * @throws TransformException if a point cannot be transformed to its final coordinate space.
     */
    @Test
    public void testMultiCellsMultiBands() throws TransformException {
        final BufferedImage image = new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB);
        final WritableRaster raster = image.getRaster();
        raster.setSample(1, 1, 1, 6);
        threshold = 3;
        final Isolines[] isolines = Isolines.generate(image, new double[][] {{threshold}}, null);
        assertEquals(3, isolines.length, "Number of bands");
        assertTrue(isolines[0].polylines().isEmpty());
        assertTrue(isolines[2].polylines().isEmpty());
        isoline =  isolines[1].polylines().get(threshold);
        verifyIsolineFromMultiCells(isoline);
    }

    /**
     * Tests a cell containing a NaN value.
     *
     * @throws TransformException if a point cannot be transformed to its final coordinate space.
     */
    @Test
    public void testNaN() throws TransformException {
        threshold = 2;
        /*
         *   ▘5╌╌NaN▝
         *    ╎    ╎
         *    0╌╌╌╌0
         */
        generateFromCell(5, Float.NaN, 0, 0);
        assertNull(isoline);
    }

    /**
     * Generates isolines from a 2×2 image having the given values.
     * The result is stored in {@link #isoline}; it may be {@code null}.
     *
     * @throws TransformException if a point cannot be transformed to its final coordinate space.
     */
    private void generateFromCell(float v00, float v10, float v01, float v11) throws TransformException {
        generateFromImage(2, v00, v10, v01, v11);
    }

    /**
     * Generates isolines from a size×size image having the given values.
     * The result is stored in {@link #isoline}; it may be {@code null}.
     *
     * @throws TransformException if a point cannot be transformed to its final coordinate space.
     */
    private void generateFromImage(final int size, final float... values) throws TransformException {
        final BufferedImage image = RasterFactory.createGrayScaleImage(DataBuffer.TYPE_FLOAT, size, size, 1, 0, 0, 10);
        final WritableRaster raster = image.getRaster();
        for (int i=0; i<values.length; i++) {
            raster.setSample(i % size, i / size, 0, values[i]);
        }
        final Isolines[] isolines = Isolines.generate(image, new double[][] {{threshold}}, null);
        assertEquals(1, isolines.length, "Number of bands");
        final Map<Double, Shape> polylines = isolines[0].polylines();
        assertTrue(polylines.size() <= 1);
        isoline = polylines.get(threshold);
    }

    /**
     * Asserts that {@link #isoline} is a segment having the given coordinates.
     */
    private void assertSegmentEquals(final double x0, final double y0, final double x1, final double y1) {
        assertNotNull(isoline);
        final double[] buffer = new double[2];
        final PathIterator it = isoline.getPathIterator(null);
        assertSegmentEquals(it, buffer, x0, y0, x1, y1);
        assertTrue(it.isDone());
    }

    /**
     * Asserts that the iterator contains a {@code SEG_MOVETO} followed by a {@code SEG_LINETO} instruction
     * with the given coordinates. The segment is positioned on next segment after this method call.
     */
    private static void assertSegmentEquals(final PathIterator it, final double[] buffer,
            final double x0, final double y0, final double x1, final double y1)
    {
        assertFalse(it.isDone());
        assertEquals(PathIterator.SEG_MOVETO, it.currentSegment(buffer));
        assertEquals(x0, buffer[0], TOLERANCE);
        assertEquals(y0, buffer[1], TOLERANCE);
        it.next();
        assertFalse(it.isDone());
        assertEquals(PathIterator.SEG_LINETO, it.currentSegment(buffer));
        assertEquals(x1, buffer[0], TOLERANCE);
        assertEquals(y1, buffer[1], TOLERANCE);
        it.next();
    }

    /**
     * Asserts that {@link #isoline} is two segments having the given coordinates.
     */
    private void assertSegmentsEqual(final double x0, final double y0, final double x1, final double y1,
                                     final double x2, final double y2, final double x3, final double y3)
    {
        assertNotNull(isoline);
        final double[] buffer = new double[2];
        final PathIterator it = isoline.getPathIterator(null);
        assertSegmentEquals(it, buffer, x0, y0, x1, y1);
        assertSegmentEquals(it, buffer, x2, y2, x3, y3);
        assertTrue(it.isDone());
    }
}
