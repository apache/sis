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

import java.util.Random;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.util.stream.Stream;
import java.awt.image.DataBuffer;
import javax.measure.IncommensurableException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.operation.transform.MathTransforms;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.image.TiledImageMock;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.junit.jupiter.api.TestInstance;

// Specific to the main branch:
import org.apache.sis.coverage.PointOutsideCoverageException;


/**
 * Tests {@link DefaultEvaluator}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class DefaultEvaluatorTest extends TestCase {
    /**
     * The random number generator.
     */
    private final Random random;

    /**
     * Size of the image to test, in pixels.
     * Used for computing expected sample values.
     */
    private final int width, height;

    /**
     * Size of tiles, in pixels.
     * Used for computing expected sample values.
     */
    private final int tileWidth, tileHeight;

    /**
     * Number of tiles on the <var>x</var> axis.
     */
    private final int numXTiles;

    /**
     * The grid geometry of the coverage used as source data.
     */
    private final GridGeometry gridGeometry;

    /**
     * Number of bands.
     */
    private final int numBands;

    /**
     * The evaluator to test.
     */
    private final GridCoverage.Evaluator evaluator;

    /**
     * The expected values in the first band of each coordinate.
     * The length of this array is the number of test points.
     */
    private float[] expectedValues;

    /**
     * Creates a new test case with a small grid coverage.
     * The sample values will be 3 digits numbers of the form "BTYX" where:
     * <ul>
     *   <li><var>B</var> is the band index starting with 1 for the first band.</li>
     *   <li><var>T</var> is the tile index starting with 1 for the first tile and increasing in a row-major fashion.</li>
     *   <li><var>Y</var> is the <var>y</var> coordinate (row 0-based index) of the sample value relative to current tile.</li>
     *   <li><var>X</var> is the <var>x</var> coordinate (column 0-based index) of the sample value relative to current tile.</li>
     * </ul>
     *
     * Image size and tile size are computed in a way that keep each above-cited numbers smaller than 10.
     */
    public DefaultEvaluatorTest() {
        random     = TestUtilities.createRandomNumberGenerator();
        numBands   = random.nextInt(3) + 1;
        tileWidth  = random.nextInt(4) + 6;   // Keep lower than 10.
        tileHeight = random.nextInt(4) + 6;
        width      = tileWidth  * (random.nextInt(3) + 1) - random.nextInt(4);
        height     = tileHeight * (random.nextInt(3) + 1) - random.nextInt(4);
        final var image  = new TiledImageMock(
                DataBuffer.TYPE_USHORT,         // data type
                numBands,                       // number of values per pixel
                random.nextInt(20) - 10,        // minX
                random.nextInt(20) - 10,        // minY
                width,     height,              // image size
                tileWidth, tileHeight,          // tile size
                random.nextInt(20) - 10,        // minTileX
                random.nextInt(20) - 10,        // minTileY
                random.nextBoolean());          // banded or interleaved sample model

        image.initializeAllTiles();
        assertEquals(width,  image.getWidth());
        assertEquals(height, image.getHeight());

        final int dx = random.nextInt(200) - 100;
        final int dy = random.nextInt(200) - 100;
        gridGeometry = new GridGeometry(
                new GridExtent(null, new long[] {dx, dy}, new long[] {dx + width, dy + height}, false),
                new Envelope2D(HardCodedCRS.WGS84, 20, -10, 40, 60), GridOrientation.HOMOTHETY);
        evaluator = new GridCoverage2D(gridGeometry, null, image).evaluator();
        numXTiles = image.getNumXTiles();
    }

    /**
     * Returns the "grid to CRS" transform targeting the given coordinate reference system.
     *
     * @param  crs  the target coordinate reference system.
     * @return the "grid to CRS" to the given CRS.
     * @throws TransformException if the transform cannot be created.
     */
    private MathTransform gridToCRS(final CoordinateReferenceSystem crs) throws TransformException {
        try {
            CoordinateSystem coverageCS = gridGeometry.getCoordinateReferenceSystem().getCoordinateSystem();
            Matrix swap = CoordinateSystems.swapAndScaleAxes(crs.getCoordinateSystem(), coverageCS);
            return MathTransforms.concatenate(gridGeometry.gridToCRS, MathTransforms.linear(swap).inverse());
        } catch (IncommensurableException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Creates test points, together with the expected values.
     * The expected values are set in the {@link #expectedValues} field.
     *
     * @param  allowVariations  whether to allow points outside the coverage domain and change of <abbr>CRS</abbr>.
     * @return the test points.
     * @throws TransformException if a test point cannot be computed.
     */
    private List<DirectPosition> createTestPoints(final boolean allowVariations) throws TransformException {
        final int     numPoints  = random.nextInt(30) + 20;
        final var     points     = new GeneralDirectPosition[numPoints];
        final float   lowerX     = gridGeometry.extent.getLow(0);
        final float   lowerY     = gridGeometry.extent.getLow(1);
        final float[] gridCoords = new float[2];
        MathTransform gridToCRS  = gridGeometry.gridToCRS;
        CoordinateReferenceSystem crs = gridGeometry.getCoordinateReferenceSystem();
        expectedValues = new float[numPoints];
        /*
         * Prepare in advance the indexes of points to put outside the coverage.
         * Some tests need the guarantee that at least one point is outside, and
         * this approach also makes easier to have two consecutive points outside.
         */
        final var indexOfPointsOutside = new HashSet<Integer>();
        if (allowVariations) {
            for (int j=0; j<5; j++) {
                int i = random.nextInt(numPoints);
                indexOfPointsOutside.add(i);
                if (i != 0 && random.nextBoolean()) {
                    indexOfPointsOutside.add(i - 1);
                }
            }
        }
        for (int i=0; i<numPoints; i++) {
            /*
             * Randomly change the CRS if this change is allowed. The test needs at least one CRS
             * with more dimensions than the grid CRS, in order to verify that internal arrays do
             * not overflow.
             */
            if (allowVariations) {
                switch (random.nextInt(10)) {
                    case 0: {
                        crs = HardCodedCRS.WGS84;
                        gridToCRS  = gridGeometry.gridToCRS;
                        break;
                    }
                    case 1: {
                        crs = HardCodedCRS.WGS84_LATITUDE_FIRST;
                        gridToCRS = gridToCRS(crs);
                        break;
                    }
                    case 2: {
                        crs = HardCodedCRS.WGS84_3D;
                        gridToCRS = gridToCRS(crs);
                        break;
                    }
                }
            }
            final float expected;
            if (indexOfPointsOutside.remove(i)) {
                gridCoords[0] = lowerX + (random.nextBoolean() ? -1 : width);
                gridCoords[1] = lowerY + (random.nextBoolean() ? -1 : height);
                expected = Float.NaN;
            } else {
                final int x = random.nextInt(width);
                final int y = random.nextInt(height);
                final int tx = x / tileWidth;
                final int ty = y / tileHeight;
                gridCoords[0] = lowerX + x + 0.875f * (random.nextFloat() - 0.5f);
                gridCoords[1] = lowerY + y + 0.875f * (random.nextFloat() - 0.5f);
                expected = (x - tx * tileWidth)
                         + (y - ty * tileHeight) * 10
                         + 100*(numXTiles*ty + tx + 1);
            }
            expectedValues[i] = expected;
            final var point = new GeneralDirectPosition(crs);
            gridToCRS.transform(gridCoords, 0, point.coordinates, 0, 1);
            points[i] = point;
        }
        assertTrue(indexOfPointsOutside.isEmpty());
        return Arrays.asList(points);
    }

    /**
     * Compares the actual sample values against the expected values.
     * The given stream should compute sample values for the points
     * returned by {@link #createTestPoints()}.
     *
     * @param  stream  the computed values.
     */
    private void runAndCompare(final Stream<double[]> stream) {
        final boolean isNullIfOutside = evaluator.isNullIfOutside();
        final double[][] actual = stream.map((samples) -> {
            if (samples != null) {
                return samples.clone();
            }
            assertTrue(isNullIfOutside, "Unexpected null array of sample values.");
            return null;
        }).toArray(double[][]::new);
        assertEquals(expectedValues.length, actual.length);
        for (int i=0; i<actual.length; i++) {
            double expected = expectedValues[i];
            final double[] samples = actual[i];
            if (samples != null) {
                assertEquals(numBands, samples.length);
            }
            for (int band = 0; band < numBands; band++) {
                assertEquals(Double.isNaN(expected), samples == null);
                assertEquals(expected += 1000, (samples != null) ? samples[band] : Double.NaN);
            }
        }
    }

    /**
     * Tests the {@code apply(DirectPosition)} method.
     * Assuming that the {@link GridCoverage2DTest#testEvaluator()} test passes,
     * this test verifies that the above test infrastructure works.
     *
     * @see GridCoverage2DTest#testEvaluator()
     * @see GridCoverage2DTest#testEvaluatorWithWraparound()
     * @throws TransformException if a test point cannot be computed.
     */
    @Test
    public void testApply() throws TransformException {
        evaluator.setNullIfOutside(false);
        assertFalse(evaluator.isNullIfOutside());
        runAndCompare(createTestPoints(false).stream().map(evaluator::apply));
    }

    /**
     * Same as {@link #testApply()} but with points outside the grid coverage domain.
     * Also tests input points with different <abbr>CRS</abbr>.
     *
     * @throws TransformException if a test point cannot be computed.
     */
    @Test
    public void testApplyWithPointOutside() throws TransformException {
        evaluator.setNullIfOutside(true);
        assertTrue(evaluator.isNullIfOutside());
        runAndCompare(createTestPoints(true).stream().map(evaluator::apply));
    }

    /**
     * Tests with random points inside the coverage computed sequentially.
     *
     * @throws TransformException if a test point cannot be computed.
     */
    @Test
    public void testSequential() throws TransformException {
        evaluator.setNullIfOutside(false);
        assertFalse(evaluator.isNullIfOutside());
        runAndCompare(evaluator.stream(createTestPoints(false), false));
    }

    /**
     * Same as {@link #testSequential()} but with points outside the grid coverage domain.
     * Also tests input points with different <abbr>CRS</abbr>.
     *
     * @throws TransformException if a test point cannot be computed.
     */
    @Test
    public void testSequentialWithPointOutside() throws TransformException {
        evaluator.setNullIfOutside(true);
        assertTrue(evaluator.isNullIfOutside());
        runAndCompare(evaluator.stream(createTestPoints(true), false));
    }

    /**
     * Tests with random points inside the coverage computed sequentially.
     *
     * @throws TransformException if a test point cannot be computed.
     */
    @Test
    public void testParallel() throws TransformException {
        evaluator.setNullIfOutside(false);
        assertFalse(evaluator.isNullIfOutside());
        runAndCompare(evaluator.stream(createTestPoints(false), true));
    }

    /**
     * Same as {@link #testParallel()} but with points outside the grid coverage domain.
     * Also tests input points with different <abbr>CRS</abbr>.
     *
     * @throws TransformException if a test point cannot be computed.
     */
    @Test
    public void testParallelWithPointOutside() throws TransformException {
        evaluator.setNullIfOutside(true);
        assertTrue(evaluator.isNullIfOutside());
        runAndCompare(evaluator.stream(createTestPoints(true), true));
    }

    /**
     * Verifies the exception thrown for point outside the grid domain.
     *
     * @throws TransformException if a test point cannot be computed.
     */
    @Test
    public void testPointOutsideCoverageException() throws TransformException {
        evaluator.setNullIfOutside(false);
        assertFalse(evaluator.isNullIfOutside());
        PointOutsideCoverageException ex = assertThrows(PointOutsideCoverageException.class,
                () -> runAndCompare(evaluator.stream(createTestPoints(true), true)));
        assertNotNull(ex.getMessage());
    }
}
