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

import java.util.List;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.MathTransform1D;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.image.internal.shared.RasterFactory;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.transform.MathTransforms;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;
import static org.apache.sis.test.Assertions.assertMessageContains;
import static org.apache.sis.feature.Assertions.assertPixelsEqual;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coordinate.MismatchedDimensionException;
import org.opengis.coverage.PointOutsideCoverageException;
import static org.opengis.test.Assertions.assertSampleValuesEqual;


/**
 * Tests the {@link GridCoverage2D} implementation.
 * Also used as a base class for testing other implementations.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
@SuppressWarnings("exports")
public class GridCoverage2DTest extends TestCase {
    /**
     * Width and height of the grid tested in this class.
     */
    protected static final int GRID_SIZE = 2;

    /**
     * Creates a new test case.
     */
    public GridCoverage2DTest() {
    }

    /**
     * Creates a {@link GridCoverage} instance with arbitrary sample values.
     * The image size is 2×2 pixels, the "grid to CRS" transform is identity,
     * the range of sample values is [-97.5 … 105] metres and the packed values are:
     *
     * <pre class="text">
     *    2    5
     *   -5  -10</pre>
     */
    private GridCoverage createTestCoverage() {
        return createTestCoverage(MathTransforms.identity(2));
    }

    /**
     * Sames as {@link #createTestCoverage()} except that the "grid to CRS" transform can be specified.
     * The domain of source grid indices is the [0 … 1] range in all dimensions.
     */
    private GridCoverage createTestCoverage(final MathTransform gridToCRS) {
        final var grid = new GridGeometry(new GridExtent(GRID_SIZE, GRID_SIZE),
                            PixelInCell.CELL_CENTER, gridToCRS, HardCodedCRS.WGS84);

        final var toUnits = (MathTransform1D) MathTransforms.linear(0.5, 100);
        final SampleDimension sd = new SampleDimension.Builder().setName("Some kind of height")
                .addQuantitative("data", NumberRange.create(-10, true, 10, true), toUnits, Units.METRE)
                .build();
        return createTestCoverage(grid, List.of(sd));
    }

    /**
     * Creates a {@link GridCoverage} instance to test with fixed sample values.
     * The coverage returned by this method shall contain the following values:
     *
     * <pre class="text">
     *    2    5
     *   -5  -10</pre>
     *
     * @param  grid  the grid geometry of the coverage to create.
     * @param  sd    the sample dimensions of the coverage to create.
     * @return the coverage instance to test, with above-cited values.
     */
    GridCoverage createTestCoverage(final GridGeometry grid, final List<SampleDimension> sd) {
        /*
         * Create an image and set values directly as integers. We do not use one of the
         * BufferedImage.TYPE_* constant because this test uses some negative values.
         */
        final BufferedImage  image  = RasterFactory.createGrayScaleImage(DataBuffer.TYPE_INT, GRID_SIZE, GRID_SIZE, 1, 0, -10, 10);
        final WritableRaster raster = image.getRaster();
        raster.setSample(0, 0, 0,   2);
        raster.setSample(1, 0, 0,   5);
        raster.setSample(0, 1, 0,  -5);
        raster.setSample(1, 1, 0, -10);
        return new GridCoverage2D(grid, sd, image);
    }

    /**
     * Asserts that the sample values in the given coverage are equal to the expected values.
     *
     * @param  coverage  the coverage containing the sample values to check.
     * @param  expected  the expected sample values.
     */
    private static void assertSamplesEqual(final GridCoverage coverage, final double[][] expected) {
        final Raster raster = coverage.render(null).getData();
        assertEquals(expected.length, raster.getHeight(), "height");
        for (int y=0; y<expected.length; y++) {
            assertEquals(expected[y].length, raster.getWidth(), "width");
            for (int x=0; x<expected[y].length; x++) {
                double value = raster.getSampleDouble(x, y, 0);
                assertEquals(expected[y][x], value);
            }
        }
    }

    /**
     * Tests reading the values provided by {@link GridCoverage2D#forConvertedValues(boolean)}.
     */
    @Test
    public void testReadConvertedValues() {
        GridCoverage coverage = createTestCoverage();
        /*
         * Verify packed values.
         */
        assertSamplesEqual(coverage, new double[][] {
            { 2,   5},
            {-5, -10}
        });
        /*
         * Verify converted values.
         */
        coverage = coverage.forConvertedValues(true);
        assertSamplesEqual(coverage, new double[][] {
            {101.0, 102.5},
            { 97.5,  95.0}
        });
    }

    /**
     * Tests writing values in {@link GridCoverage2D#forConvertedValues(boolean)}.
     */
    @Test
    public void testWriteConvertedValues() {
        GridCoverage coverage = createTestCoverage();
        coverage = coverage.forConvertedValues(true);
        assertSamplesEqual(coverage, new double[][] {
            {101.0, 102.5},
            { 97.5,  95.0}
        });
        /*
         * Test writing converted values and verify the result in the packed coverage.
         * For example, for the sample value at (0,0), we have (p is the packed value):
         *
         *   70 = p * 0.5 + 100   →   (70-100)/0.5 = p   →   p = -60
         */
        final var image = (WritableRenderedImage) coverage.render(null);
        final WritableRaster raster = image.getWritableTile(0, 0);
        raster.setSample(0, 0, 0,  70);
        raster.setSample(1, 0, 0,   2.5);
        raster.setSample(0, 1, 0,  -8);
        raster.setSample(1, 1, 0, -90);
        image.releaseWritableTile(0, 0);
        assertSamplesEqual(coverage.forConvertedValues(false), new double[][] {
            { -60, -195},
            {-216, -380}
        });
    }

    /**
     * Tests {@link GridCoverage.Evaluator#apply(DirectPosition)}.
     */
    @Test
    public void testEvaluator() {
        final GridCoverage.Evaluator evaluator = createTestCoverage().evaluator();
        /*
         * Test evaluation at indeger indices. No interpolation should be applied.
         */
        assertArrayEquals(new double[] {  2}, evaluator.apply(new DirectPosition2D(0, 0)));
        assertArrayEquals(new double[] {  5}, evaluator.apply(new DirectPosition2D(1, 0)));
        assertArrayEquals(new double[] { -5}, evaluator.apply(new DirectPosition2D(0, 1)));
        assertArrayEquals(new double[] {-10}, evaluator.apply(new DirectPosition2D(1, 1)));
        /*
         * Test evaluation at fractional indices. Current interpolation is nearest neighor rounding,
         * but future version may do a bilinear interpolation.
         */
        assertArrayEquals(new double[] {2}, evaluator.apply(new DirectPosition2D(-0.499, -0.499)));
        assertArrayEquals(new double[] {2}, evaluator.apply(new DirectPosition2D( 0.499,  0.499)));
        /*
         * Test some points that are outside the coverage extent.
         */
        var e = assertThrows(PointOutsideCoverageException.class,
                () -> evaluator.apply(new DirectPosition2D(-0.51, 0)));
        assertNotNull(e.getMessage());

        e = assertThrows(PointOutsideCoverageException.class,
                () -> evaluator.apply(new DirectPosition2D(1.51, 0)));
        assertNotNull(e.getMessage());
    }

    /**
     * Tests {@link GridCoverage.Evaluator#apply(DirectPosition)} with a wraparound on the longitude axis.
     * This method tests a coordinate that would be outside the grid if wraparound was not applied.
     */
    @Test
    public void testEvaluatorWithWraparound() {
        final var gridToCRS = new Matrix3();
        gridToCRS.m00 = 100;        // Scale
        gridToCRS.m02 = 100;        // Offset
        final GridCoverage.Evaluator evaluator = createTestCoverage(MathTransforms.linear(gridToCRS)).evaluator();
        evaluator.setWraparoundEnabled(true);
        assertArrayEquals(new double[] {2}, evaluator.apply(new DirectPosition2D(100, 0)));
        assertArrayEquals(new double[] {5}, evaluator.apply(new DirectPosition2D(200, 0)));
        /*
         * Following tests fail if wraparound is not applied by `GridCoverage.Evaluator`.
         */
        assertArrayEquals(new double[] {5}, evaluator.apply(new DirectPosition2D(200 - 360, 0)));
        assertArrayEquals(new double[] {2}, evaluator.apply(new DirectPosition2D(100 - 360, 0)));
    }

    /**
     * Verifies that calling {@link GridCoverage#render(GridExtent)} with a sub-extent (crop operation)
     * returns precisely the requested area, not a smaller or bigger one.
     */
    @Test
    public void testRenderOfSubextent() {
        final GridCoverage coverage = createTestCoverage();
        RenderedImage result;
        /*
         * Row extraction:
         *   - Expected size (2,1) is verified by `assertPixelsEqual(…)`.
         *   - Bounds of expected values is Rectangle(translation, size).
         *   - Pixel source(0, 1) → output(0, 0)
         *   - Pixel source(1, 1) → output(1, 0)
         */
        final var singleRow = new GridExtent(GRID_SIZE, 1).translate(0, 1);
        result = coverage.render(singleRow);
        assertInstanceOf(BufferedImage.class, result);
        assertPixelsEqual(coverage.render(null), new Rectangle(0, 1, GRID_SIZE, 1), result, null);
        /*
         * Column extraction:
         *   - Expected size (1,2) is verified by `assertPixelsEqual(…)`.
         *   - Bounds of expected values is Rectangle(translation, size).
         *   - Pixel source(1, 0) → output(0, 0)
         *   - Pixel source(1, 1) → output(0, 1)
         */
        final var singleCol = new GridExtent(1, GRID_SIZE).translate(1, 0);
        result = coverage.render(singleCol);
        assertInstanceOf(BufferedImage.class, result);
        assertPixelsEqual(coverage.render(null), new Rectangle(1, 0, 1, GRID_SIZE), result, null);
    }

    /**
     * Verifies that calling {@link GridCoverage#render(GridExtent)} with a larger extent
     * returns an image with the appropriate offset.
     */
    @Test
    public void testRenderOfLargerExtent() {
        final GridCoverage coverage = createTestCoverage();
        final GridExtent sliceExtent = new GridExtent(null,
                new long[] {-5, -2},
                new long[] {GRID_SIZE + 3, GRID_SIZE + 5}, true);
        final RenderedImage result = coverage.render(sliceExtent);
        assertEquals(5,         result.getMinX());
        assertEquals(2,         result.getMinY());
        assertEquals(GRID_SIZE, result.getWidth());
        assertEquals(GRID_SIZE, result.getHeight());
        assertSampleValuesEqual(coverage.render(null), result, 0, null);
    }

    /**
     * Verifies that calling {@link GridCoverage#render(GridExtent)} with an extent
     * having the wrong number of exception causes an exception to be thrown.
     */
    @Test
    public void testInvalidDimension() {
        final GridCoverage coverage = createTestCoverage();
        var sliceExtent = new GridExtent(null, null, new long[] {GRID_SIZE, GRID_SIZE, 0}, true);
        var e = assertThrows(MismatchedDimensionException.class, () -> coverage.render(sliceExtent),
                "Should not have accepted an extent with wrong number of dimensions.");
        assertMessageContains(e, "sliceExtent");
    }
}
