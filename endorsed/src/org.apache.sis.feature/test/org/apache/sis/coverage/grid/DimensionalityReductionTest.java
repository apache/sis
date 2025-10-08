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

import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.internal.shared.DirectPositionView;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;
import static org.apache.sis.feature.Assertions.assertGridToCornerEquals;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:


/**
 * Tests {@link DimensionalityReduction}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class DimensionalityReductionTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DimensionalityReductionTest() {
    }

    /**
     * Convenience method for building a grid geometry.
     *
     * @param  low        low grid coordinates, inclusive.
     * @param  high       high grid coordinates, inclusive.
     * @param  gridToCRS  map pixel corner to geographic coordinates.
     * @param  crs        target of {@code gridToCRS}.
     * @return grid geometry for testing purposes.
     */
    private static GridGeometry createGridGeometry(long[] low, long[] high, Matrix gridToCRS, CoordinateReferenceSystem crs) {
        return new GridGeometry(new GridExtent(null, low, high, true), PixelInCell.CELL_CORNER,
                                MathTransforms.linear(gridToCRS), crs);
    }

    /**
     * Creates a four-dimensional grid geometry to be used for the tests in this class.
     * The "grid to CRS" transform is a linear one.
     */
    private static GridGeometry linearGrid() {
        return createGridGeometry(new long[] { 20, 136,  4, -2},
                                  new long[] {419, 201, 10,  2},
                                  gridToCRS(), HardCodedCRS.GEOID_4D);
    }

    /**
     * Returns a "grid to CRS" transform for the full grid geometry to be used in tests.
     */
    private static Matrix gridToCRS() {
        return Matrices.create(5, 5, new double[] {
                        0.5, 0,   0,  0,  -180,
                        0,   0.5, 0,  0,   -90,
                        0,   0,   5,  0,    -2,
                        0,   0,   0,  7, 60000,
                        0,   0,   0,  0,     1});
    }

    /**
     * Returns the matrix for the "grid to CRS" transform of the test grid, but without height.
     */
    private static Matrix4 withHeightRemoved() {
        return new Matrix4(0.5, 0,   0,  -180,
                           0,   0.5, 0,   -90,
                           0,   0,   7, 60000,
                           0,   0,   0,     1);
    }

    /**
     * Returns the matrix for the "grid to CRS" transform with only the horizontal part.
     */
    private static Matrix3 withHorizontal() {
        return new Matrix3(0.5, 0,  -180,
                           0,   0.5, -90,
                           0,   0,     1);
    }

    /**
     * Asserts that the "grid to CRS" transform of the given grid geometry is equal to the specified value.
     *
     * @param  test      the grid geometry to verify.
     * @param  expected  the expected "grid to CRS" transform.
     * @return the CRS of the given grid geometry.
     */
    private static CoordinateReferenceSystem verifyGridToCRS(final GridGeometry test, final Matrix expected) {
        assertGridToCornerEquals(expected, test);
        return test.getCoordinateReferenceSystem();
    }

    /**
     * Tests reduction of a direct position.
     *
     * @param reduction  the reduction to apply.
     * @param source     source coordinates.
     * @param target     expected reduced coordinates.
     */
    private static void testPosition(final DimensionalityReduction reduction, double[] source, double[] target) {
        assertArrayEquals(target, reduction.apply(new DirectPositionView.Double(source)).getCoordinates());
    }

    /**
     * Tests the removal of a single dimension in the middle of the "grid to CRS" transform.
     * This test use the same CRS for all steps.
     */
    @Test
    public void testRemoval() {
        var reduction = DimensionalityReduction.remove(linearGrid(), 2);      // Remove height.
        var crs = verifyGridToCRS(reduction.getReducedGridGeometry(), withHeightRemoved());
        assertArrayEquals(new SingleCRS[] {HardCodedCRS.WGS84, HardCodedCRS.TIME},
                          CRS.getSingleComponents(crs).toArray());
        /*
         * Tests the fast path in reduction and reverse operations.
         */
        assertSame(reduction.getReducedGridGeometry(), reduction.apply(linearGrid()));
        assertSame(reduction.getSourceGridGeometry(),  reduction.reverse(reduction.getReducedGridGeometry()));
        /*
         * Test the reverse operation with a slightly different "grid to CRS".
         * It will test the generic path, as opposed to above-cited fast path.
         */
        GridGeometry test = reduction.reverse(createGridGeometry(
                new long[] {100, 180, -5},
                new long[] {200, 195, -3},
                new Matrix4(0.5, 0,   0,  -100,
                            0,   0.5, 0,   -80,
                            0,   0,   7, 60002,
                            0,   0,   0,     1), crs));

        crs = verifyGridToCRS(test, Matrices.create(5, 5, new double[] {
                        0.5, 0,   0,  0,  -100,
                        0,   0.5, 0,  0,   -80,
                        0,   0,   5,  0,    -2,
                        0,   0,   0,  7, 60002,
                        0,   0,   0,  0,     1}));

        assertSame(reduction.getSourceGridGeometry().getCoordinateReferenceSystem(), crs);
        testPosition(reduction, new double[] {100, 101, 102, 103}, new double[] {100, 101, 103});
    }

    /**
     * Tests the selection of two dimensions.
     */
    @Test
    public void testSelect() {
        var reduction = DimensionalityReduction.select2D(linearGrid());
        assertGridToCornerEquals(withHorizontal(), reduction.getReducedGridGeometry());
        assertSame(HardCodedCRS.WGS84, reduction.getReducedGridGeometry().getCoordinateReferenceSystem());

        GridGeometry test = reduction.reverse(createGridGeometry(
                new long[] {380, 100},
                new long[] {400, 200},
                new Matrix3(0,   0.5, -80,
                            0.5, 0,  -100,
                            0,   0,     1), HardCodedCRS.WGS84_LATITUDE_FIRST));

        var crs = verifyGridToCRS(test, Matrices.create(5, 5, new double[] {
                        0,   0.5, 0,  0,   -80,
                        0.5, 0,   0,  0,  -100,
                        0,   0,   5,  0,    -2,
                        0,   0,   0,  7, 60000,
                        0,   0,   0,  0,     1}));
        /*
         * CRS should have different axis order.
         */
        var sourceCRS = reduction.getSourceGridGeometry().getCoordinateReferenceSystem();
        assertFalse(CRS.equivalent(sourceCRS, crs));
        assertTrue(Utilities.deepEquals(test, test, ComparisonMode.ALLOW_VARIANT));     // Ignore axis order.
        testPosition(reduction, new double[] {100, 101, 102, 103}, new double[] {100, 101});
    }
}
