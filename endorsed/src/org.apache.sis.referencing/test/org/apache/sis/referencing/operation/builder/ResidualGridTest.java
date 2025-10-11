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
package org.apache.sis.referencing.operation.builder;

import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.transform.MathTransforms;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;

// Specific to the main branch:
import static org.apache.sis.test.GeoapiAssert.assertMatrixEquals;


/**
 * Tests {@link ResidualGrid}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class ResidualGridTest extends TestCase {
    /**
     * The grid to test.
     */
    private final ResidualGrid grid;

    /**
     * Creates a new test case with a 3×4 grid with 2 values in each cells.
     * Those two values are typically the horizontal components of translation vectors.
     * The grid has no "source to grid" or "grid to CRS" transformations.
     *
     * @throws TransformException if an error occurred while handling a wraparound axis.
     */
    public ResidualGridTest() throws TransformException {
        grid = new ResidualGrid(MathTransforms.identity(2), MathTransforms.identity(2), 3, 4, new float[] {
                0,2  ,  1,2  ,  2,1,
                1,3  ,  2,2  ,  1,1,
                0,4  ,  2,3  ,  3,2,
                1,4  ,  3,3  ,  3,2}, 0.1, null, null);
    }

    /**
     * Verifies some global properties.
     */
    @Test
    public void verifyGlobalProperties() {
        assertEquals(2, grid.getTranslationDimensions());
        assertTrue(grid.getCoordinateToGrid().isIdentity());
        assertTrue(grid.gridToTarget.isIdentity());
    }

    /**
     * Tests {@link ResidualGrid#getCellValue(int, int, int)}.
     * That method fetches values directly, without interpolations.
     */
    @Test
    public void testGetCellValue() {
        assertEquals(0, grid.getCellValue(0, 0, 0));
        assertEquals(2, grid.getCellValue(1, 0, 0));
        assertEquals(1, grid.getCellValue(0, 1, 0));
        assertEquals(2, grid.getCellValue(1, 1, 0));
        assertEquals(2, grid.getCellValue(0, 2, 0));
        assertEquals(1, grid.getCellValue(1, 2, 0));
        assertEquals(1, grid.getCellValue(0, 0, 3));
        assertEquals(4, grid.getCellValue(1, 0, 3));
        assertEquals(3, grid.getCellValue(0, 2, 3));
        assertEquals(2, grid.getCellValue(1, 2, 3));
    }

    /**
     * Tests {@link ResidualGrid#interpolateAt(double...)} without interpolation.
     * This test checks the same values as {@link #testGetCellValue()}.
     *
     * @throws TransformException if an error occurred while transforming a coordinate.
     */
    @Test
    public void testInterpolateAtExactLocation() throws TransformException {
        assertArrayEquals(new double[] {0, 2}, grid.interpolateAt(0, 0));
        assertArrayEquals(new double[] {1, 2}, grid.interpolateAt(1, 0));
        assertArrayEquals(new double[] {2, 1}, grid.interpolateAt(2, 0));
        assertArrayEquals(new double[] {1, 4}, grid.interpolateAt(0, 3));
        assertArrayEquals(new double[] {3, 2}, grid.interpolateAt(2, 3));
    }

    /**
     * Tests {@link ResidualGrid#interpolateAt(double...)} at the median point between cells.
     * The result in this special case is equivalent to the average of all 4 surrounding cells.
     *
     * @throws TransformException if an error occurred while transforming a coordinate.
     */
    @Test
    public void testInterpolateAtIntersection() throws TransformException {
        assertArrayEquals(new double[] {1,    2.25}, grid.interpolateAt(0.5, 0.5));
        assertArrayEquals(new double[] {1.5,  1.5 }, grid.interpolateAt(1.5, 0.5));
        assertArrayEquals(new double[] {1.25, 3   }, grid.interpolateAt(0.5, 1.5));
        assertArrayEquals(new double[] {2,    2   }, grid.interpolateAt(1.5, 1.5));
        assertArrayEquals(new double[] {1.5,  3.5 }, grid.interpolateAt(0.5, 2.5));
        assertArrayEquals(new double[] {2.75, 2.5 }, grid.interpolateAt(1.5, 2.5));
    }

    /**
     * Tests {@link ResidualGrid#interpolateAt(double...)} at other locations.
     *
     * @throws TransformException if an error occurred while transforming a coordinate.
     */
    @Test
    public void testInterpolateAt() throws TransformException {
        assertArrayEquals(new double[] {0.25,  2   }, grid.interpolateAt(0.25, 0   ));
        assertArrayEquals(new double[] {1.75,  1.25}, grid.interpolateAt(1.75, 0   ));
        assertArrayEquals(new double[] {1.25,  2   }, grid.interpolateAt(1,    0.25));
        assertArrayEquals(new double[] {1.625, 1.25}, grid.interpolateAt(1.75, 0.25));
    }

    /**
     * Tests {@link ResidualGrid#derivativeInCell(double, double)}.
     * The Jacobian is computed with those values:
     *
     * <pre class="text">
     *   (0,4)   (2,3)
     *   (1,4)   (3,3)</pre>
     *
     * So this mean for example that when moving from 1 cell to the right, the residual change from (0,4) to (2,3).
     * Consequently, the <var>x</var> position is increased from (2-0) = 2 cells in addition to the move to the right
     * (so the total increase is 3), and the <var>y</var> position is increased from (3-4) = -1.
     */
    @Test
    public void testDerivativeInCell() {
        final Matrix expected = new Matrix2(3, 1, -1, 1);
        final Matrix actual = grid.derivativeInCell(0.5, 2.5);
        assertMatrixEquals(expected, actual, "derivativeInCell");
    }
}
