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
package org.apache.sis.storage.geotiff.reader;

import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.math.Vector;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests the construction of a localization grid from GeoTIFF tie points.
 * The tie point spacings tested here are the spacings observed in real products:
 * Sentinel 1 images, and ICEYE images in Ground Range Detected, Single Look Complex
 * and ScanSAR flavors.
 *
 * <p>All tests build the tie points from a slightly non-linear model, then verify that the
 * resulting transform maps each tie point to the model coordinates declared in the same record.
 * The model has to be non-linear, otherwise every candidate transform would reproduce the tie
 * points exactly and the tests would only verify the absence of exception.</p>
 *
 * @author  Jonatas Fischer
 */
public final class LocalizationTest extends TestCase {
    /**
     * Tolerance threshold, in degrees, when comparing tie point coordinates.
     * This is about one metre, while the tie points of the grids tested here
     * are kilometres apart.
     */
    private static final double TOLERANCE = 1E-5;

    /**
     * Number of tie points on each axis of most grids tested here.
     * This is the number of tie points in ICEYE products.
     */
    private static final int GRID_SIZE = 10;

    /**
     * Creates a new test case.
     */
    public LocalizationTest() {
    }

    /**
     * Returns the pixel coordinates of {@code count} evenly spaced tie points, the first one
     * on the first pixel and the last one on the given last pixel. The coordinates are exact,
     * i.e. they are not necessarily integers.
     *
     * @param  count  number of tie points.
     * @param  last   pixel coordinate of the last tie point, usually the image size minus 1.
     * @return pixel coordinates of the tie points.
     */
    private static double[] evenSpacing(final int count, final double last) {
        final double[] coordinates = new double[count];
        for (int i=0; i<count; i++) {
            coordinates[i] = i * (last / (count - 1));
        }
        return coordinates;
    }

    /**
     * Returns the pixel coordinates of {@value #GRID_SIZE} evenly spaced tie points,
     * rounded to integers as in the GeoTIFF files written by ICEYE.
     * Consequently the steps may differ from each other by one pixel.
     *
     * @param  last  pixel coordinate of the last tie point, usually the image size minus 1.
     * @return pixel coordinates of the tie points.
     */
    private static double[] roundedSpacing(final int last) {
        final double[] coordinates = evenSpacing(GRID_SIZE, last);
        for (int i=0; i<coordinates.length; i++) {
            coordinates[i] = Math.round(coordinates[i]);
        }
        return coordinates;
    }

    /**
     * Returns the pixel coordinates of {@value #GRID_SIZE} tie points spaced by the given step,
     * except the last point which is closer to its predecessor. This is the spacing of Sentinel 1
     * images, and the reason why {@code Localization} splits irregular grids in four parts.
     *
     * @param  step  step between two consecutive tie points, except the last two.
     * @param  last  step between the two last tie points.
     * @return pixel coordinates of the tie points.
     */
    private static double[] shorterLastStep(final int step, final int last) {
        final double[] coordinates = new double[GRID_SIZE];
        for (int i=1; i<GRID_SIZE; i++) {
            coordinates[i] = coordinates[i-1] + (i < GRID_SIZE-1 ? step : last);
        }
        return coordinates;
    }

    /**
     * Creates tie points on the grid formed by the given pixel coordinates. Model coordinates are
     * computed by a non-linear function of the pixel coordinates, in order to give the localization
     * grid something to interpolate. The magnitude of the non-linear terms is about 10 metres, which
     * is the order of magnitude of the terrain-induced distortion in a radar image.
     *
     * @param  columns  pixel coordinates of the tie points along the <var>x</var> axis.
     * @param  rows     pixel coordinates of the tie points along the <var>y</var> axis.
     * @return the (I,J,K,X,Y,Z) records of the tie points.
     */
    private static Vector tiePoints(final double[] columns, final double[] rows) {
        final double[] records = new double[columns.length * rows.length * Localization.RECORD_LENGTH];
        int p = 0;
        for (final double y : rows) {
            for (final double x : columns) {
                records[p++] = x;
                records[p++] = y;
                records[p++] = 0;
                records[p++] = -66 + x*1E-6 + y*3E-8 + (x*y)*2E-13;
                records[p++] =  45 - y*1E-6 + x*5E-8 - (x*x)*1E-13;
                records[p++] = 0;
            }
        }
        return Vector.create(records, false);
    }

    /**
     * Builds the localization grid for the given tie points, then verifies that the resulting
     * transform maps the pixel coordinates of each tie point to the model coordinates declared
     * in the same record.
     *
     * @param  columns  pixel coordinates of the tie points along the <var>x</var> axis.
     * @param  rows     pixel coordinates of the tie points along the <var>y</var> axis.
     * @throws Exception if the transform cannot be created or used.
     */
    private static void verify(final double[] columns, final double[] rows) throws Exception {
        final Vector tiePoints = tiePoints(columns, rows);
        final MathTransform gridToCRS = Localization.nonLinear(tiePoints);
        assertNotNull(gridToCRS);
        final double[] source = new double[2];
        final double[] target = new double[2];
        for (int i=0; i<tiePoints.size(); i += Localization.RECORD_LENGTH) {
            source[0] = tiePoints.doubleValue(i);
            source[1] = tiePoints.doubleValue(i+1);
            gridToCRS.transform(source, 0, target, 0, 1);
            assertEquals(tiePoints.doubleValue(i+3), target[0], TOLERANCE, "Longitude of tie point");
            assertEquals(tiePoints.doubleValue(i+4), target[1], TOLERANCE, "Latitude of tie point");
        }
    }

    /**
     * Tests a grid where all points are evenly spaced by an integer amount of pixels. This is the
     * case handled directly by {@link org.apache.sis.referencing.operation.builder.LocalizationGridBuilder},
     * without any of the fallbacks tested by the other methods.
     *
     * @throws Exception if the transform cannot be created or used.
     */
    @Test
    public void testRegularGrid() throws Exception {
        final double[] coordinates = new double[GRID_SIZE];
        for (int i=1; i<GRID_SIZE; i++) {
            coordinates[i] = coordinates[i-1] + 2222;
        }
        verify(coordinates, coordinates);
    }

    /**
     * Tests a grid where all points are evenly spaced, but by a fractional amount of pixels.
     * The greatest common divisor of those coordinates is much smaller than the actual step,
     * so the grid size cannot be inferred from it.
     *
     * <p>This is the case of ICEYE ScanSAR images of 19250 × 19510 pixels, which have 39 × 40
     * tie points spaced by 506.55 and 500.23 pixels respectively.</p>
     *
     * @throws Exception if the transform cannot be created or used.
     */
    @Test
    public void testGridWithFractionalSpacing() throws Exception {
        verify(evenSpacing(39, 19249), evenSpacing(40, 19509));
    }

    /**
     * Tests a grid where the steps differ by one pixel on a single axis. Only two of the four parts
     * in which {@code Localization} would split such a grid receive points; the empty parts shall
     * not cause an {@link IndexOutOfBoundsException}.
     *
     * <p>This is the case of ICEYE Single Look Complex images of 114644 × 16714 pixels:
     * the tie points are spaced by 12738 pixels along <var>x</var> except one step of 12739 pixels,
     * and evenly spaced by 1857 pixels along <var>y</var>.</p>
     *
     * @throws Exception if the transform cannot be created or used.
     */
    @Test
    public void testGridWithIrregularStepOnOneAxis() throws Exception {
        verify(roundedSpacing(114643), roundedSpacing(16713));
    }

    /**
     * Tests a grid where the steps differ by one pixel on both axes.
     * This is the case of ICEYE Ground Range Detected images of 20000 × 20000 pixels:
     * the tie points are spaced by 2222 pixels except one step of 2223 pixels on each axis.
     *
     * @throws Exception if the transform cannot be created or used.
     */
    @Test
    public void testGridWithIrregularStepOnBothAxes() throws Exception {
        verify(roundedSpacing(19999), roundedSpacing(19999));
    }

    /**
     * Tests a grid where the steps alternate between two values differing by one pixel.
     * This is the case of ICEYE Single Look Complex images of 34484 × 15342 pixels:
     * the tie points are spaced by 3831 and 3832 pixels alternately along <var>x</var>,
     * and by 1705 and 1704 pixels alternately along <var>y</var>. Splitting such a grid
     * gives parts that are still irregular, so the split has to recurse.
     *
     * @throws Exception if the transform cannot be created or used.
     */
    @Test
    public void testGridWithAlternatingSteps() throws Exception {
        verify(roundedSpacing(34483), roundedSpacing(15341));
    }

    /**
     * Tests a grid where the last step is genuinely shorter than the other steps,
     * as in Sentinel 1 images where the tie points are spaced by 1320 pixels except
     * the last two which are 1302 pixels apart. Contrarily to the ICEYE grids, the
     * spacing of this grid is not uniform up to a rounding to integers, so it has to
     * be handled by splitting the grid in parts.
     *
     * @throws Exception if the transform cannot be created or used.
     */
    @Test
    public void testGridWithShorterLastStep() throws Exception {
        final double[] coordinates = shorterLastStep(1320, 1302);
        verify(coordinates, coordinates);
    }

    /**
     * Tests a grid where the last step is genuinely shorter on a single axis.
     * This combines the Sentinel 1 spacing with the empty parts of
     * {@link #testGridWithIrregularStepOnOneAxis()}.
     *
     * @throws Exception if the transform cannot be created or used.
     */
    @Test
    public void testGridWithShorterLastStepOnOneAxis() throws Exception {
        final double[] rows = new double[GRID_SIZE];
        for (int i=1; i<GRID_SIZE; i++) {
            rows[i] = rows[i-1] + 1320;
        }
        verify(shorterLastStep(1320, 1302), rows);
    }
}
