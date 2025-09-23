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

import java.util.Map;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.internal.shared.Formulas;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.crs.DefaultCompoundCRS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import static org.apache.sis.coverage.grid.GridGeometryTest.assertExtentEquals;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.operation.HardCodedConversions;
import static org.apache.sis.referencing.Assertions.assertEnvelopeEquals;

// Specific to the main branch:
import static org.apache.sis.test.GeoapiAssert.assertBetween;
import static org.apache.sis.test.GeoapiAssert.assertMatrixEquals;


/**
 * Tests the {@link GridDerivation} implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @author  Johann Sorel (Geomatys)
 */
public final class GridDerivationTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public GridDerivationTest() {
    }

    /**
     * Tests {@link GridDerivation#subgrid(Envelope, double...)} using only the
     * {@link GridExtent} result provided by {@link GridDerivation#getIntersection()}.
     */
    @Test
    public void testSubExtent() {
        GeneralEnvelope envelope = new GeneralEnvelope(HardCodedCRS.WGS84_3D);
        envelope.setRange(0, -80, 120);
        envelope.setRange(1, -12,  21);
        envelope.setRange(2,  10,  25);
        final MathTransform gridToCRS = MathTransforms.linear(new Matrix4(
                0,   0.5, 0,  -90,
                0.5, 0,   0, -180,
                0,   0,   2,    3,
                0,   0,   0,    1));
        final var grid = new GridGeometry(PixelInCell.CELL_CORNER, gridToCRS, envelope, GridRoundingMode.NEAREST);
        assertExtentEquals(
                new long[] {336,  20,  4},
                new long[] {401, 419, 10}, grid.getExtent());
        /*
         * Set the region of interest as a two-dimensional envelope. The vertical dimension is omitted.
         * The result should be that all grid indices in the vertical dimension are kept unchanged.
         */
        envelope = new GeneralEnvelope(HardCodedCRS.WGS84);
        envelope.setRange(0, -70.001, +80.002);
        envelope.setRange(1,   4.997,  15.003);
        GridDerivation change = grid.derive().subgrid(envelope);
        assertFalse(change.hasSubsampling());
        assertTrue(change.getGridTransform(PixelInCell.CELL_CENTER).isIdentity());
        assertExtentEquals(new long[] {370,  40,  4},
                           new long[] {389, 339, 10}, change.getIntersection());
    }

    /**
     * Creates a grid geometry with the given extent and scale for testing purpose.
     * An arbitrary translation of (2,3) is added to the "grid to CRS" conversion.
     */
    private static GridGeometry grid(int xmin, int ymin, int xmax, int ymax, int xScale, int yScale) {
        var extent = new GridExtent(null, new long[] {xmin, ymin}, new long[] {xmax, ymax}, true);
        var gridToCRS = new Matrix3();
        gridToCRS.m00 = xScale;
        gridToCRS.m11 = yScale;
        gridToCRS.m02 = 200;            // Arbitrary translation.
        gridToCRS.m12 = 500;
        return new GridGeometry(extent, PixelInCell.CELL_CORNER, MathTransforms.linear(gridToCRS), null);
    }

    /**
     * Tests the construction from grid geometries having a linear "grid to CRS" conversion.
     */
    @Test
    public void testSubgridFromOtherGrid() {
        testSubgridFromOtherGrid(PixelInCell.CELL_CORNER, 5549);
    }

    /**
     * Same as {@link #testSubgridFromOtherGrid()} but requests tight result.
     */
    @Test
    public void testTightSubgridFromOtherGrid() {
        testSubgridFromOtherGrid(PixelInCell.CELL_CENTER, 5524);
    }

    /**
     * Implementation of {@code test[Tight]SubgridFromOtherGrid()}.
     *
     * @param  include  value of {@code pointsToInclude} property.
     * @param  high     expected high cell value of the result in dimension 0.
     */
    private void testSubgridFromOtherGrid(final PixelInCell include, final int high) {
        GridGeometry   query  = grid(  10,   -20,  110,  180, 100, -300);               // Envelope x: [1200 … 11300]   y: [-53800 … 6500]
        GridGeometry   base   = grid(2000, -1000, 9000, 8000,   2,   -1);               // Envelope x: [4200 … 18202]   y: [ -7501 … 1500]
        GridDerivation change = base.derive().pointsToInclude(include).subgrid(query);  // Envelope x: [4200 … 11300]   y: [ -7501 … 1500]
        GridExtent     extent = change.getIntersection();
        GridExtentTest.assertExtentEquals(extent, 0,  2000, high);                      // Subrange of base extent.
        GridExtentTest.assertExtentEquals(extent, 1, -1000, 8000);
        assertArrayEquals(new long[] {50, 300}, change.getSubsampling());               // s = scaleQuery / scaleBase
        assertArrayEquals(new long[] {0, -100}, change.getSubsamplingOffsets());
        assertFalse(change.getGridTransform(PixelInCell.CELL_CENTER).isIdentity());
        assertTrue(change.hasSubsampling());
        /*
         * Above (50, 300) subsampling shall be applied and the `gridToCRS` transform adjusted consequently.
         */
        final GridGeometry tg = change.build();
        extent = tg.getExtent();
        GridExtentTest.assertExtentEquals(extent, 0,  40, 110);
        GridExtentTest.assertExtentEquals(extent, 1,  -3,  27);               // NEAREST grid rounding mode.
        assertMatrixEquals(new Matrix3(100,    0, 200,
                                         0, -300, 600,
                                         0,    0,   1),
                MathTransforms.getMatrix(tg.getGridToCRS(PixelInCell.CELL_CORNER)), STRICT, "gridToCRS");
        /*
         * The envelope is the intersection of the envelopes of `query` and `base` grid geometries, documented above.
         * That intersection should be approximately the same or smaller. Note that without the clipping documented in
         * `GridExtent(GridExtent, int...)` constructor, the envelope would have been larger.
         */
        var expected = new GeneralEnvelope(2);
        expected.setRange(0,  4200, 11300);
        expected.setRange(1, -7501,  1500);
        assertEnvelopeEquals(expected, tg.getEnvelope(), STRICT);
    }

    /**
     * Tests {@link GridDerivation#subgrid(Envelope, double...)} with a non-linear "grid to CRS" transform.
     */
    @Test
    public void testSubExtentNonLinear() {
        final var extent = new GridExtent(
                new DimensionNameType[] {
                    DimensionNameType.COLUMN,
                    DimensionNameType.ROW,
                    DimensionNameType.VERTICAL
                },
                new long[] {  0,  0, 2},
                new long[] {180, 90, 5}, false);
        final MathTransform linear = MathTransforms.linear(new Matrix4(
                2, 0, 0, -180,
                0, 2, 0,  -90,
                0, 0, 5,   10,
                0, 0, 0,    1));
        final MathTransform latitude  = MathTransforms.interpolate(new double[] {0, 20, 50, 70, 90}, new double[] {-90, -45, 0, 45, 90});
        final MathTransform gridToCRS = MathTransforms.concatenate(linear, MathTransforms.passThrough(1, latitude, 1));
        final GridGeometry  grid      = new GridGeometry(extent, PixelInCell.CELL_CENTER, gridToCRS, HardCodedCRS.WGS84_3D);
        /*
         * Following tests is similar to the one executed in testSubExtent(). Expected values are only
         * anti-regression values, except the vertical range which is expected to cover all cells. The
         * main purpose of this test is to verify that TransformSeparator has been able to extract the
         * two-dimensional transform despite its non-linear component.
         */
        final var envelope = new GeneralEnvelope(HardCodedCRS.WGS84);
        envelope.setRange(0, -70.001, +80.002);
        envelope.setRange(1,  -4.997,  15.003);
        final GridExtent actual = grid.derive().subgrid(envelope).getIntersection();
        assertEquals(extent.getAxisType(0), actual.getAxisType(0));
        assertExtentEquals(new long[] { 56, 69, 2},
                           new long[] {130, 73, 4}, actual);
    }

    /**
     * Tests {@link GridDerivation#subgrid(Envelope, double...)}.
     * Contrarily to {@link #testSubExtent()}, this method checks the full {@link GridGeometry}.
     *
     * @throws TransformException if an error occurred during computation.
     */
    @Test
    public void testSubgridFromEnvelope() throws TransformException {
        final var envelope = new GeneralEnvelope(HardCodedCRS.WGS84_LATITUDE_FIRST);
        envelope.setRange(0, -70, +80);
        envelope.setRange(1,   5,  15);
        final MathTransform gridToCRS = MathTransforms.linear(new Matrix3(
                0,   0.5, -90,
                0.5, 0,  -180,
                0,   0,     1));
        /*
         * Consistency checks before to test the `subgrid(…)` operation.
         */
        GridGeometry grid = new GridGeometry(PixelInCell.CELL_CORNER, gridToCRS, envelope, GridRoundingMode.NEAREST);
        assertExtentEquals(new long[] {370, 40}, new long[] {389, 339}, grid.getExtent());
        assertEnvelopeEquals(envelope, grid.getEnvelope(), STRICT);
        /*
         * Set a sub-region. The grid extent and "grid to CRS" transform shall be adjusted
         * in such a way that envelope computed from the new grid geometry is the same.
         */
        envelope.setRange(0, -50, +30);
        envelope.setRange(1,   8,  12);
        grid = grid.derive().subgrid(envelope, 1, 2).build();
        assertExtentEquals(new long[] {94, 40}, new long[] {95, 119}, grid.getExtent());
        assertEnvelopeEquals(envelope, grid.getEnvelope(), STRICT);
        assertMatrixEquals(new Matrix3(0, 1,  -90,
                                       2, 0, -180,
                                       0, 0,    1),
                MathTransforms.getMatrix(grid.getGridToCRS(PixelInCell.CELL_CORNER)), STRICT, "gridToCRS");
        /*
         * A sub-region again but with a requested resolution which is not a divisor of the actual resolution.
         * It will force GridGeometry to adjust the translation term to compensate. We verify that the adustment
         * is correct by verifying that we still get the same envelope.
         */
        grid = grid.derive().subgrid(envelope, 3, 2).build();
        assertExtentEquals(new long[] {94, 13}, new long[] {95, 39}, grid.getExtent());
        assertEnvelopeEquals(envelope, grid.getEnvelope(), STRICT);
        MathTransform cornerToCRS = grid.getGridToCRS(PixelInCell.CELL_CORNER);
        assertMatrixEquals(new Matrix3(0, 3,  -89,
                                       2, 0, -180,
                                       0, 0,    1),
                MathTransforms.getMatrix(cornerToCRS), STRICT, "gridToCRS");

        DirectPosition2D src = new DirectPosition2D();
        DirectPosition2D tgt = new DirectPosition2D();
        DirectPosition2D exp = new DirectPosition2D();
        src.x = 94; src.y = 13; exp.x = -50; exp.y =  8; assertEquals(exp, cornerToCRS.transform(src, tgt), "Lower corner");
        src.x = 96; src.y = 40; exp.x = +31; exp.y = 12; assertEquals(exp, cornerToCRS.transform(src, tgt), "Upper corner");
    }

    /**
     * Tests {@link GridDerivation#subgrid(Envelope, double...)} using an envelope in a CRS different than the
     * grid geometry CRS. This test constructs the same grid geometry as {@link #testSubgridFromEnvelope()}
     * and tests the same request with only axis order flipped.
     */
    @Test
    public void testSubgridFromEnvelopeDifferentCRS() {
        final var envelope = new GeneralEnvelope(HardCodedCRS.WGS84_LATITUDE_FIRST);
        envelope.setRange(0, -70, +80);
        envelope.setRange(1,   5,  15);
        final MathTransform gridToCRS = MathTransforms.linear(new Matrix3(
                0,   0.5, -90,
                0.5, 0,  -180,
                0,   0,     1));
        /*
         * Same grid geometry as `testSubgridFromEnvelope()` with consistency checks omitted.
         */
        GridGeometry grid = new GridGeometry(PixelInCell.CELL_CORNER, gridToCRS, envelope, GridRoundingMode.NEAREST);
        /*
         * Same request as the one used by `testSubgridFromEnvelope()` but with different axis order.
         * The resulting subgrid should have the same extent as the one in `testSubgridFromEnvelope()`.
         */
        envelope.setCoordinateReferenceSystem(HardCodedCRS.WGS84);
        envelope.setRange(1, -50, +30);
        envelope.setRange(0,   8,  12);
        grid = grid.derive().subgrid(envelope, 2, 1).build();
        assertSame(HardCodedCRS.WGS84_LATITUDE_FIRST, grid.getCoordinateReferenceSystem());
        assertExtentEquals(new long[] {94, 40}, new long[] {95, 119}, grid.getExtent());
        /*
         * Before to check envelope, we need to restore the same axis order as specified in the grid geometry.
         * The envelope below is identical to the one used in `testSubgridFromEnvelope()`.
         */
        envelope.setCoordinateReferenceSystem(HardCodedCRS.WGS84_LATITUDE_FIRST);
        envelope.setRange(0, -50, +30);
        envelope.setRange(1,   8,  12);
        assertEnvelopeEquals(envelope, grid.getEnvelope(), STRICT);
    }

    /**
     * Tests {@link GridDerivation#subgrid(Envelope, double...)} using an envelope with more dimensions
     * than the source grid geometry. The additional dimensions should be ignored.
     */
    @Test
    public void testSubgridFromEnvelopeWithMoreDimensions() {
        GeneralEnvelope envelope = new GeneralEnvelope(HardCodedCRS.WGS84);
        envelope.setRange(0, -70, +80);
        envelope.setRange(1,   5,  15);
        GridGeometry grid = new GridGeometry(new GridExtent(300, 40), envelope, GridOrientation.HOMOTHETY);
        assertExtentEquals(new long[2], new long[] {299, 39}, grid.getExtent());
        /*
         * Above grid has a resolution of 0.5° × 0.25° per pixel. Ask for a resolution of 2° × 1° × 3 meters
         * per pixels. The resolution in meter should be ignored, together with the envelope vertical range.
         */
        envelope = new GeneralEnvelope(HardCodedCRS.WGS84_WITH_TIME);
        envelope.setRange(0, -40, +30);
        envelope.setRange(1,   8,  18);
        envelope.setRange(2,  20,  40);
        final GridDerivation derivation = grid.derive();
        grid = derivation.subgrid(envelope, 2, 1, 3).build();
        /*
         * With the resolution used in this test, the grid size is divided by 4 in the first 2 dimensions.
         * Note that the specified resolution (2,1) appears verbatim on the diagonal of the matrix.
         */
        assertExtentEquals(new long[] {60, 12}, new long[] {199, 39}, derivation.getIntersection());
        assertExtentEquals(new long[] {15,  3}, new long[] { 49,  9}, grid.getExtent());
        assertMatrixEquals(new Matrix3(2, 0, -69, 0, 1, 5.5, 0, 0, 1),
                MathTransforms.getMatrix(grid.getGridToCRS(PixelInCell.CELL_CENTER)),
                STRICT, "gridToCRS");
    }

    /**
     * Tests {@link GridDerivation#subgrid(Envelope, double...)} using an envelope with less dimensions
     * than the source grid geometry.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-514">SIS-514</a>
     */
    @Test
    public void testSubgridFromEnvelopeWithLessDimensions() {
        GeneralEnvelope envelope = new GeneralEnvelope(HardCodedCRS.WGS84_WITH_TIME);
        envelope.setRange(0, -70, +80);
        envelope.setRange(1,   5,  15);
        envelope.setRange(2,  20,  40);
        GridExtent extent = new GridExtent(null, null, new long[] {300, 40, 6}, false);
        GridGeometry grid = new GridGeometry(extent, envelope, GridOrientation.HOMOTHETY);
        assertExtentEquals(new long[3], new long[] {299, 39, 5}, grid.getExtent());
        /*
         * Above grid has a resolution of 0.5° × 0.25° per pixel. Ask for a resolution of 2° × 1° × 3 meters
         * per pixels. The resolution in meter should be ignored.
         */
        envelope = new GeneralEnvelope(HardCodedCRS.WGS84);
        envelope.setRange(0, -40, +30);
        envelope.setRange(1,   8,  18);
        final GridDerivation derivation = grid.derive();
        grid = derivation.subgrid(envelope, 2, 1, 3).build();
        assertExtentEquals(new long[] {15, 3, 0}, new long[] {49, 9, 5}, grid.getExtent());
    }

    /**
     * Tests {@link GridDerivation#subgrid(GridGeometry)} using a grid with less dimensions
     * than the source grid geometry.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-610">SIS-610</a>
     */
    @Test
    public void testSubgridWithLessDimensions() {
        var envelope = new GeneralEnvelope(HardCodedCRS.WGS84_4D);
        envelope.setRange(0, 10, 20);
        envelope.setRange(1, 30, 40);
        envelope.setRange(2, 2, 4);
        envelope.setRange(3, 3, 6);
        var env2D  = new Envelope2D(HardCodedCRS.WGS84, 10, 30, 10, 10);
        var extent = new GridExtent(null, null, new long[] {2, 2, 1, 1}, false);
        var grid   = new GridGeometry(extent, envelope, GridOrientation.DISPLAY);
        var aoi    = new GridGeometry(new GridExtent(2, 2), env2D, GridOrientation.DISPLAY);
        GridGeometry slice = grid.derive().subgrid(aoi).build();
        Matrix gridToCRS = MathTransforms.getMatrix(slice.getGridToCRS(PixelInCell.CELL_CORNER));
        Matrix expected = Matrices.create(5, 5, new double[] {
            5,   0,   0,   0,  10,
            0,  -5,   0,   0,  40,
            0,   0,   2,   0,   2,
            0,   0,   0,   3,   3,
            0,   0,   0,   0,   1});
        assertMatrixEquals(expected, gridToCRS, STRICT, "gridToCRS");
    }

    /**
     * Tests {@link GridDerivation#subgrid(GridGeometry)} using a grid with less dimensions
     * than the source grid geometry when the extra dimensions are not a slice.
     *
     * <h4>Note on cache dependency</h4>
     * Another difference between this test and {@link #testSubgridWithLessDimensions()} is that
     * this test uses a geographic area covering the world. It has subtile implications in the way
     * that {@link org.apache.sis.referencing.operation.CoordinateOperationFinder} uses its cache.
     * See in particular the {@code canStoreInCache} flag of the latter class.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-610">SIS-610</a>
     */
    @Test
    public void testSubgridWithLessDimensionsNoSlice() {
        final var envelope = new GeneralEnvelope(HardCodedCRS.WGS84_4D);
        envelope.setRange(0, -180, 180);
        envelope.setRange(1,  -90,  90);
        envelope.setRange(2,  100, 300);
        envelope.setRange(3,    3,   6);
        final var extent = new GridExtent(null, null, new long[] {2, 2, 3, 3}, false);
        final var grid   = new GridGeometry(extent, envelope, GridOrientation.DISPLAY);
        final var env2D  = new GeneralEnvelope(HardCodedCRS.WGS84);
        env2D.setRange(0, -51, 153);
        env2D.setRange(1,  68,  89);
        final var aoi = new GridGeometry(new GridExtent(100, 100), env2D, GridOrientation.HOMOTHETY);
        /*
         * Test with rounding to nearest grid coordinates. The AOI has a range of latitude values which
         * is fully enclosed in the voxel at index y=1, while the range of longitude values intersects
         * voxels at indexes x=[0…1]. However, because the rounding mode is nearest and the lower bound
         * is closer to x=1, the result is x=[1].
         */
        GridGeometry slice = grid.derive().subgrid(aoi).build();
        assertEquals(grid.getGridToCRS(PixelInCell.CELL_CENTER),
                    slice.getGridToCRS(PixelInCell.CELL_CENTER));
        assertExtentEquals(new long[] {1, 0, 0, 0},
                           new long[] {1, 0, 2, 2},
                           slice.getExtent());
        /*
         * Same as above but with rounding mode to "enclosed".
         * The range in dimension of longitude become x=[0…1].
         */
        slice = grid.derive().rounding(GridRoundingMode.ENCLOSING).subgrid(aoi).build();
        assertEquals(grid.getGridToCRS(PixelInCell.CELL_CENTER),
                    slice.getGridToCRS(PixelInCell.CELL_CENTER));
        assertExtentEquals(new long[] {0, 0, 0, 0},
                           new long[] {1, 0, 2, 2},
                           slice.getExtent());
    }

    /**
     * Tests {@link GridDerivation#subgrid(Envelope, double...)} on a grid using a polar projection.
     * The test also uses a geographic envelope with more dimensions than the source grid geometry.
     * The difficulty is that axis directions do not match directly: the source grid has directions
     * such as "South along 90° meridian".
     */
    @Test
    public void testSubgridOnPolarProjection() {
        GeneralEnvelope envelope = new GeneralEnvelope(CommonCRS.WGS84.universal(90, 0));
        envelope.setRange(0, -1000, 1500);
        envelope.setRange(1, -2000, 1800);
        GridGeometry grid = new GridGeometry(new GridExtent(500, 400), envelope, GridOrientation.HOMOTHETY);

        envelope = new GeneralEnvelope(HardCodedCRS.WGS84_WITH_TIME);
        envelope.setRange(0, -45, -44);
        envelope.setRange(1,  64,  65);
        envelope.setRange(2,  20,  40);
        final GridDerivation derivation = grid.derive();
        grid = derivation.subgrid(envelope, 0.002, 0.001).build();
        /*
         * The main test is to check that we reach this point without an exception being thrown.
         * We add a small arbitrary test below as a matter of principle.
         */
        assertTrue(envelope.subEnvelope(0, 2).intersects(grid.getEnvelope()));
    }

    /**
     * Tests {@link GridDerivation#subgrid(GridExtent, long...)}
     * with an integer number of tiles, operating only on extents.
     */
    @Test
    public void testSubgridWithTilingOnExtent() {
        final GridGeometry base = new GridGeometry(new GridExtent(200, 225), null, null, null);
        final GridExtent domain = new GridExtent(null, new long[] {40, 30}, new long[] {99, 104}, true);
        final GridGeometry test = base.derive().chunkSize(1, 9).subgrid(domain).build();
        final GridExtent result = test.getExtent();
        assertExtentEquals(new long[] {40, 27}, new long[] {99, 107}, result);
    }

    /**
     * Tests {@link GridDerivation#subgrid(Envelope, double...)} with addition of a margin in pixels
     * and an integer number of tiles.
     */
    @Test
    public void testSubgridWithMarginAndTiling() {
        // Same data as above test, but using only 2 dimensions.
        GeneralEnvelope envelope = new GeneralEnvelope(HardCodedCRS.WGS84);
        envelope.setRange(0, -70, +80);
        envelope.setRange(1,   5,  15);
        GridGeometry grid = new GridGeometry(new GridExtent(300, 40), envelope, GridOrientation.HOMOTHETY);
        envelope.setRange(0, -40, +30);
        envelope.setRange(1,   8,  18);
        /*
         * Size of `intersection`:
         *   Before margin and chunk:   {60, 12}, {199, 39}
         *   After margin (∓4, ∓3):     {56,  9}, {203, 39}    (39 is unchanged because of clipping).
         *   After chunk size:          {55,  0}, {204, 39}
         */
        final GridDerivation derivation = grid.derive().margin(4, 3).chunkSize(5, 10);
        grid = derivation.subgrid(envelope, 2, 1).build();
        assertExtentEquals(new long[] {55, 0}, new long[] {204, 39}, derivation.getIntersection());
        assertExtentEquals(new long[] {11, 0}, new long[] { 40,  7}, grid.getExtent());
        assertArrayEquals(new double[] {2.5, 1.25}, grid.getResolution(false));
        /*
         * Without chunk size, the resolution would have been {2,1} which correspond to a subsampling of {4,4}.
         * But because of the chunk size, the subsampling have been rounded to {5,5} which correspond to above
         * resolution. The grid extent, which would have been x:[14 … 50] and y:[0 … 9], is also affected by
         * the subsampling adjustment.
         */
    }

    /**
     * Tests {@link GridDerivation#subgrid(GridGeometry)} with tiling and a subsampling.
     * This is an anti-regression test: values were determined empirically
     * (we have not done manual validation).
     *
     * @see #testSubgridFromOtherGrid()
     */
    @Test
    public void testSubgridWithTilingAndSubsampling() {
        testSubgridWithTilingAndSubsampling(PixelInCell.CELL_CORNER, -910, -28, -3);
    }

    /**
     * Same as {@link #testSubgridWithTilingAndSubsampling()} but requests tight result.
     */
    @Test
    public void testTightSubgridWithTilingAndSubsampling() {
        testSubgridWithTilingAndSubsampling(PixelInCell.CELL_CENTER, -770, -182, -2);
    }

    /**
     * Implementation of {@code test[Tight]SubgridFromOtherGrid()}.
     *
     * @param  include  value of {@code pointsToInclude} property.
     * @param  low      expected low cell value of the result in dimension 1.
     * @param  offset   expected subsampling offset in dimension 1.
     * @param  subLow   expected low cell value of the result in dimension 1 after subsampling.
     */
    private void testSubgridWithTilingAndSubsampling(final PixelInCell include, final int low, final int offset, final int subLow) {
        GridGeometry   query  = grid(  80,   -3,  110,    55, 100, -300);     // Envelope x: [8200 … 11300]   y: [-16300 … 1400]
        GridGeometry   base   = grid(2000, -1000, 9000, 8000,   2,   -1);     // Envelope x: [4200 … 18202]   y: [ -7501 … 1500]
        GridDerivation change = base.derive().chunkSize(390, 70).pointsToInclude(include).subgrid(query);
        GridExtent     extent = change.getIntersection();
        GridExtentTest.assertExtentEquals(extent, 0,  3900, 5849);
        GridExtentTest.assertExtentEquals(extent, 1,   low, 8000);
        assertArrayEquals(new long[] {39, 294},    change.getSubsampling());
        assertArrayEquals(new long[] { 0, offset}, change.getSubsamplingOffsets());
        assertTrue(change.hasSubsampling());

        final GridGeometry tg = change.build();
        extent = tg.getExtent();
        GridExtentTest.assertExtentEquals(extent, 0, 100,   149);
        GridExtentTest.assertExtentEquals(extent, 1, subLow, 27);
        assertMatrixEquals(new Matrix3(78,    0, 200,
                                        0, -294, 500 - offset,
                                        0,    0,   1),
                MathTransforms.getMatrix(tg.getGridToCRS(PixelInCell.CELL_CORNER)),
                STRICT, "gridToCRS");

        var expected = new GeneralEnvelope(2);
        expected.setRange(0,  8000, 11900);
        expected.setRange(1, -7501,  500 - low);
        assertEnvelopeEquals(expected, tg.getEnvelope(), STRICT);
    }

    /**
     * Tests {@link GridDerivation#subgrid(GridGeometry)} with a maximum subsampling value.
     */
    @Test
    public void testSubgridWithMaximumSubsampling() {
        GridGeometry   query  = grid(  80,   -3,  110,    55, 100, -300);     // Same as above test.
        GridGeometry   base   = grid(2000, -1000, 9000, 8000,   2,   -1);
        GridDerivation change = base.derive().chunkSize(390, 70).maximumSubsampling(25, 100).subgrid(query);
        GridGeometry   result = change.build();
        Matrix         toCRS  = MathTransforms.getMatrix(result.getGridToCRS(PixelInCell.CELL_CORNER));
        /*
         * Subsampling values checked below shall be equal or smaller
         * than the values given to `maximumSubsampling(…)`.
         */
        assertTrue(change.hasSubsampling());
        assertFalse(change.getGridTransform(PixelInCell.CELL_CORNER).isIdentity());
        assertArrayEquals(new long[] {15,  84}, change.getSubsampling());
        assertArrayEquals(new long[] { 0, -70}, change.getSubsamplingOffsets());
        assertMatrixEquals(new Matrix3(30,   0, 200,
                                        0, -84, 570,
                                        0,   0,   1),
                toCRS, STRICT, "gridToCRS");
    }

    /**
     * Tests {@link GridDerivation#subgrid(GridGeometry)} with only a "grid to CRS" transform.
     */
    @Test
    public void testSubgridWithTransformOnly() {
        GridGeometry   base   = grid(2000, -1000, 9000, 8000,   2,   -1);
        GridGeometry   query  = new GridGeometry(null, PixelInCell.CELL_CENTER, MathTransforms.scale(10, 40), null);
        GridDerivation change = base.derive().subgrid(query);
        GridGeometry   result = change.build();
        Matrix         toCRS  = MathTransforms.getMatrix(result.getGridToCRS(PixelInCell.CELL_CENTER));
        assertArrayEquals(new double[] {10, 40}, result.getResolution(false));
        assertMatrixEquals(new Matrix3(10,   0, 205,
                                        0, -40, 480,      // Note the negative sign, preserved from base grid geometry.
                                        0,   0,   1),
                toCRS, STRICT, "gridToCRS");
    }

    /**
     * Tests {@link GridDerivation#subgrid(GridExtent, long...)} with a null "grid to CRS" transform.
     */
    @Test
    public void testSubgridWithoutTransform() {
        GridExtent   base   = new GridExtent(null, new long[] {100, 200}, new long[] {300, 350}, true);
        GridExtent   query  = new GridExtent(null, new long[] {120, 180}, new long[] {280, 360}, true);
        GridGeometry result = new GridGeometry(base, null, null, null).derive().subgrid(query).build();
        assertExtentEquals(new long[] {120, 200}, new long[] {280, 350}, result.getExtent());
        assertFalse(result.isDefined(GridGeometry.GRID_TO_CRS));
        assertFalse(result.isDefined(GridGeometry.CRS));
        assertFalse(result.isDefined(GridGeometry.RESOLUTION));
        /*
         * Try again with a subsampling. We can get the subsampling information back as the resolution.
         * Note that there is no way with current API to get the subsampling offsets.
         */
        result = new GridGeometry(base, null, null, null).derive().subgrid(query, 3, 5).build();
        assertExtentEquals(new long[] {40, 40}, new long[] {93, 70}, result.getExtent());
        assertFalse(result.isDefined(GridGeometry.GRID_TO_CRS));
        assertFalse(result.isDefined(GridGeometry.CRS));
        assertTrue (result.isDefined(GridGeometry.RESOLUTION));
        assertArrayEquals(new double[] {3, 5}, result.getResolution(false));
    }

    /**
     * Tests {@link GridDerivation#slice(DirectPosition)}.
     */
    @Test
    public void testSlice() {
        final var grid = new GridGeometry(
                new GridExtent(null, new long[] {336, 20, 4}, new long[] {401, 419, 10}, true),
                PixelInCell.CELL_CORNER, MathTransforms.linear(new Matrix4(
                        0,   0.5, 0,  -90,
                        0.5, 0,   0, -180,
                        0,   0,   2,    3,
                        0,   0,   0,    1)), HardCodedCRS.WGS84_3D);
        /*
         * There are two ways to ask for a slice. The first way is to set some coordinates to NaN.
         */
        GridGeometry slice = grid.derive().slice(new GeneralDirectPosition(Double.NaN, Double.NaN, 15)).build();
        assertNotSame(grid, slice);
        assertSame(grid.gridToCRS, slice.gridToCRS);
        final long[] expectedLow  = {336,  20, 6};
        final long[] expectedHigh = {401, 419, 6};
        assertExtentEquals(expectedLow, expectedHigh, slice.getExtent());
        /*
         * Same test, but using a one-dimensional slice point instead of NaN values.
         * Opportunistically use different units for testing conversions.
         */
        GeneralDirectPosition p = new GeneralDirectPosition(HardCodedCRS.ELLIPSOIDAL_HEIGHT_cm);
        p.setCoordinate(0, 1500);
        slice = grid.derive().slice(p).build();
        assertNotSame(grid, slice);
        assertSame(grid.gridToCRS, slice.gridToCRS);
        assertExtentEquals(expectedLow, expectedHigh, slice.getExtent());
    }

    /**
     * Checks that wraparound is well applied when using {@link GridDerivation#slice(DirectPosition)}.
     */
    @Test
    public void testSliceWithWrapAround() {
        final GridGeometry base = new GridGeometry(
                PixelInCell.CELL_CORNER,
                new AffineTransform2D(-0.02, 0, 0, 0.1, 55, 172),
                new Envelope2D(HardCodedCRS.WGS84_LATITUDE_FIRST, 42, 172, 13, 51),
                GridRoundingMode.NEAREST);

        final GridGeometry expectedResult = base.derive()
                .slice(new DirectPosition2D(51, 187))
                .build();

        final GridGeometry fromWrapAround = base.derive()
                .slice(new DirectPosition2D(51, -173))
                .build();

        assertEquals(expectedResult, fromWrapAround, "Slice with wrap-around");
        assertBetween(base.envelope.getMinimum(1),
                      base.envelope.getMaximum(1),
                      fromWrapAround.envelope.getMedian(1),
                      "Wrapped Y coordinate");
    }

    /**
     * Ensures that slicing on a corner point does not fail, but gives back a grid geometry centered on a pixel corner.
     *
     * @throws TransformException if we cannot build our test point.
     */
    @Test
    public void testSliceOnCorner() throws TransformException {
        GridGeometry base = grid(-132, 327, 986, 597, 2, 3);
        /*
         * First of all, we'll try by focusing on the last pixel.
         */
        final DirectPosition2D gridUpperCorner = new DirectPosition2D(
                base.extent.getHigh(0),
                base.extent.getLow(1));

        final DirectPosition geoUpperCorner = base.getGridToCRS(PixelInCell.CELL_CENTER)
                .transform(gridUpperCorner, null);

        GridGeometry slice = base.derive()
                .slice(geoUpperCorner)
                .build();

        long[] expectedGridPoint = {(long) gridUpperCorner.x, (long) gridUpperCorner.y};
        assertExtentEquals(expectedGridPoint, expectedGridPoint, slice.extent);
        /*
         * We will now try to focus on a point near the envelope edge. Note that slicing ensures to return a valid grid
         * for any point INSIDE the envelope, it's non-determinist about points perfectly aligned on the edge.
         * So, here we will test a point very near to the envelope edge, but still into it.
         */
        final var grid3d = new GeneralEnvelope(3);
        grid3d.setEnvelope(0, 0, 0, 1920, 1080, 4);

        final var crs3d = new DefaultCompoundCRS(
                Map.of("name", "geo3d"),
                HardCodedCRS.WGS84,
                HardCodedCRS.TIME);

        final var geo3d = new GeneralEnvelope(crs3d);
        geo3d.setEnvelope(-180, -90, 1865.128, 180, 90, 1865.256);
        base = new GridGeometry(
                PixelInCell.CELL_CORNER,
                MathTransforms.linear(Matrices.createTransform(grid3d, geo3d)),
                geo3d,
                GridRoundingMode.NEAREST);

        final GeneralDirectPosition geo3dUpperCorner = new GeneralDirectPosition(geo3d.getUpperCorner());
        IntStream.range(0, geo3dUpperCorner.getDimension())
                .forEach(idx -> geo3dUpperCorner.coordinates[idx] -= 1e-7);

        slice = base.derive()
                .slice(geo3dUpperCorner)
                .build();

        // Build expected grid point focused after slicing. We expect it to be upper corner.
        expectedGridPoint = DoubleStream.of(grid3d.getUpperCorner().getCoordinate())
                .mapToLong(value -> (long) value)
                .map(exclusiveValue -> exclusiveValue - 1)        // Exclusive to inclusive.
                .toArray();

        assertExtentEquals(expectedGridPoint, expectedGridPoint, slice.extent);
    }

    /**
     * Tests deriving a grid geometry with a request crossing the antimeridian.
     * The {@link GridGeometry} crossing the anti-meridian is the one given in
     * argument to {@link GridDerivation#subgrid(GridGeometry)}.
     *
     * <a href="https://issues.apache.org/jira/browse/SIS-548">SIS-548</a>
     */
    @Test
    public void testAntiMeridianCrossingInSubgrid() {
        final var    be    = new GridExtent(null, new long[] {0, -64800}, new long[] {168000, 21600}, false);
        final var    qe    = new GridExtent(256, 256);
        final double bs    = 350d / 168000;             // We will use [-175 …  175]° of longitude.
        final double qs    =  10d / 256;                // We will use [-182 … -172]° of longitude.
        final var    bt    = new AffineTransform2D(bs, 0, 0, -bs, -175, -45);
        final var    qt    = new AffineTransform2D(qs, 0, 0, -qs, -182,  90);
        final var    base  = new GridGeometry(be, PixelInCell.CELL_CORNER, bt, HardCodedCRS.WGS84);
        final var    query = new GridGeometry(qe, PixelInCell.CELL_CORNER, qt, HardCodedCRS.WGS84);
        /*
         * The [-182 … -172]° longitude range intersects [-175 …  175]° only in the [-175 … -172]° part.
         * The [-182 … -175]° part becomes [178 … 185]° after wraparound, which still outside the base
         * and should not be included in the intersection result.
         */
        GridGeometry result = base.derive().subgrid(query).build();
        assertExtentEquals(new long[] {0, -3410}, new long[] {75, -3158}, result.getExtent());
        assertEnvelopeEquals(new Envelope2D(null,
                -175,           // Expected minimum value.
                  80,           // Not interesting for this test.
                -172 - -175,    // Expected maximum value minus minimum.
                  90 -   80),
                result.getEnvelope(), 0.02);
    }

    /**
     * Tests deriving a grid geometry with a request crossing the antimeridian.
     * The request uses an envelope instead of a {@link GridGeometry}.
     */
    @Test
    public void testAntiMeridianCrossingInEnvelope() {
        final var grid = new GridGeometry(
                new GridExtent(200, 180), PixelInCell.CELL_CORNER,
                MathTransforms.linear(new Matrix3(
                        1,  0, 80,
                        0, -1, 90,
                        0,  0,  1)), HardCodedCRS.WGS84);

        final var areaOfInterest = new GeneralEnvelope(HardCodedCRS.WGS84);
        areaOfInterest.setRange(0, 140, -179);                 // Cross anti-meridian.
        areaOfInterest.setRange(1, -90,   90);

        final GridGeometry subgrid = grid.derive().subgrid(areaOfInterest).build();
        final Envelope subEnv = subgrid.getEnvelope();
        areaOfInterest.setRange(0, 140, 181);
        assertEnvelopeEquals(areaOfInterest, subEnv);
    }

    /**
     * Tests deriving a grid geometry from a grid crossing the antimeridian.
     * The {@link GridGeometry} crossing the anti-meridian is the one given
     * to {@link GridDerivation} constructor.
     */
    @Test
    public void testAntiMeridianCrossingInBaseGrid() {
        /*
         * Longitudes from 100°E to 240°E (in WGS84 geographic CRS), which is equivalent to 100°E to 120°W.
         * That [100 … 240]°E range is compatible with the [0 … 360]° longitude range declared in the CRS.
         * Latitude range is from 21°S to 60°N, but this is not important for this test.
         */
        final GridGeometry grid = new GridGeometry(
                new GridExtent(null, null, new long[] {8400, 4860}, true), PixelInCell.CELL_CENTER,
                MathTransforms.linear(new Matrix3(
                        0.016664682775860015,  0,  100.00833234138793,   0,
                        0.016663238016868958,      -20.991668380991566,  0, 0, 1)),
                HardCodedCRS.WGS84.forConvention(AxesConvention.POSITIVE_RANGE));
        /*
         * 180°W to 180″E (the world) and 80°S to 80°N in Mercator projection.
         * Only the longitudes are of interest for this test.
         */
        final GridGeometry areaOfInterest = new GridGeometry(
                new GridExtent(null, null, new long[] {256, 256}, false), PixelInCell.CELL_CORNER,
                MathTransforms.linear(new Matrix3(
                         156543.03392804097,  0,  -2.0037508342789244E7,  0,
                        -121066.95890409155,       1.5496570739723722E7,  0, 0, 1)),
                HardCodedConversions.mercator());
        /*
         * Since the area of interest covers the world (in longitude), the intersection should
         * have the same range of longitudes than the source grid, which is [100 … 240]°E.
         * Latitude range is also unchanged: 21°S to 60°N.
         */
        final GridGeometry result = grid.derive().subgrid(areaOfInterest).build();
        assertEnvelopeEquals(new Envelope2D(null, 100, -21, 240 - 100, 60 - -21),
                             result.getEnvelope(), Formulas.ANGULAR_TOLERANCE);
        /*
         * Following is empirical values provided as an anti-regression test.
         * The longitude span is about (240° − 100°) ÷ 360° × 256 px  ≈  99.56 px.
         * The latitude span is more complicated because of adjustement for resolution.
         */
        assertExtentEquals(new long[2], new long[] {100, 73}, result.getExtent());
    }

    /**
     * Tests deriving a grid geometry when all involved grid geometries cross the anti-meridian.
     * Illustration:
     *
     * <pre class="text">
     *   ──────────────┐                    ┌──────────────────
     *        Grid     │                    │       Grid
     *   ──────────────┘                    └──────────────────
     *             120°W                    100°E
     *   ─────────────────┐       ┌────────────────────────────
     *          AOI       │       │      Area Of Interest
     *   ─────────────────┘       └────────────────────────────
     *                102°W       22°W
     *   ↖…………………………………………………………………………………………………360° period…………↗︎</pre>
     */
    @Test
    public void testAntiMeridianCrossingInBothGrids() {
        /*
         * Longitudes from 100°E to 240°E (in WGS84 geographic CRS), which is equivalent to 100°E to 120°W.
         * Latitude range is from 21°S to 60°N, but this is not important for this test.
         */
        final GridGeometry grid = new GridGeometry(
                new GridExtent(null, null, new long[] {8400, 4860}, true), PixelInCell.CELL_CENTER,
                MathTransforms.linear(new Matrix3(
                        0.016664682775860015,  0,  100.00833234138793,   0,
                        0.016663238016868958,      -20.991668380991566,  0, 0, 1)),
                HardCodedCRS.WGS84.forConvention(AxesConvention.POSITIVE_RANGE));
        /*
         * 22°27′34″W to 102°27′35″W in Mercator projection.
         * Latitude range is about 66°S to 80°N, but this is not important for this test.
         */
        final GridGeometry areaOfInterest = new GridGeometry(
                new GridExtent(null, null, new long[] {865, 725}, true), PixelInCell.CELL_CENTER,
                MathTransforms.linear(new Matrix3(
                         35992.44506018084,  0,  -2482193.2646860380,  0,
                        -35992.44506018084,      16123175.875372937,   0, 0, 1)),
                HardCodedConversions.mercator());
        /*
         * Verify that the area of interest contains the full grid.
         */
        final var e1 = new GeneralEnvelope(grid.getGeographicExtent().get());
        final var e2 = new GeneralEnvelope(areaOfInterest.getGeographicExtent().get());
        assertTrue(e2.contains(e1));
        /*
         * Expect the same longitude range as `grid` since it is fully included in `areaOfInterest`.
         */
        final GridGeometry result = grid.derive().subgrid(areaOfInterest).build();
        assertEnvelopeEquals(new Envelope2D(null, 100, -21, 240 - 100, 60 - -21),
                             result.getEnvelope(), Formulas.ANGULAR_TOLERANCE);
        /*
         * Following is empirical values provided as an anti-regression test.
         */
        assertExtentEquals(new long[2], new long[] {442, 285}, result.getExtent());
    }

    /**
     * Tests deriving a grid geometry from an area of interest shifted by 360° before or after the grid valid area.
     */
    @Test
    public void testSubgridFromShiftedAOI() {
        final var grid = new GridGeometry(
                new GridExtent(20, 140), PixelInCell.CELL_CORNER,
                MathTransforms.linear(new Matrix3(
                        1,  0, 80,
                        0, -1, 70,
                        0,  0,  1)), HardCodedCRS.WGS84);

        final var areaOfInterest = new GeneralEnvelope(HardCodedCRS.WGS84);
        areaOfInterest.setRange(0,  70,  90);
        areaOfInterest.setRange(1, -80,  60);

        final var expected = new GeneralEnvelope(HardCodedCRS.WGS84);
        expected.setRange(0,  80,  90);
        expected.setRange(1, -70,  60);
        GridGeometry subgrid;
        /*
         * Area of interest of the left side.
         */
        areaOfInterest.setRange(0, -290, -270);                    // [70 … 90] - 360
        subgrid = grid.derive().subgrid(areaOfInterest).build();
        assertEnvelopeEquals(expected, subgrid.getEnvelope());
        /*
         * Area of interest on the right side.
         */
        areaOfInterest.setRange(0, 430, 450);                      // [70 … 90] + 360
        subgrid = grid.derive().subgrid(areaOfInterest).build();
        assertEnvelopeEquals(expected, subgrid.getEnvelope());
    }

    /**
     * Tests deriving a grid geometry from an area of interest overlapping the grid in such a way
     * that we have to overlap the AOI to the full grid extent. Illustration:
     *
     * <pre class="text">
     *                  ┌────────────────────────────────────────────┐
     *                  │             Domain of validity             │
     *                  └────────────────────────────────────────────┘
     *   ┌────────────────────┐                                ┌─────
     *   │  Area of interest  │                                │  AOI
     *   └────────────────────┘                                └─────
     *    ↖………………………………………………………360° period……………………………………………………↗︎</pre>
     */
    @Test
    public void testAreaOfInterestExpansion() {
        final var grid = new GridGeometry(
                new GridExtent(340, 140), PixelInCell.CELL_CORNER,
                MathTransforms.linear(new Matrix3(
                        1,  0, 5,
                        0, -1, 70,
                        0,  0,  1)), HardCodedCRS.WGS84);

        final var areaOfInterest = new GeneralEnvelope(HardCodedCRS.WGS84);
        areaOfInterest.setRange(0, -30,  40);
        areaOfInterest.setRange(1, -60,  60);

        final var expected = new GeneralEnvelope(HardCodedCRS.WGS84);
        expected.setRange(0,   5, 345);
        expected.setRange(1, -60,  60);

        GridGeometry subgrid = grid.derive().subgrid(areaOfInterest).build();
        assertEnvelopeEquals(expected, subgrid.getEnvelope());
    }

    /**
     * Verifies the exception thrown when we specify an envelope outside the grid extent.
     */
    @Test
    public void testOutsideDomain() {
        final var grid = new GridGeometry(
                new GridExtent(10, 20), PixelInCell.CELL_CORNER,
                MathTransforms.linear(new Matrix3(
                        2, 0, 0,
                        0, 2, 0,
                        0, 0, 1)), HardCodedCRS.WGS84);

        final var areaOfInterest = new GeneralEnvelope(HardCodedCRS.WGS84);
        areaOfInterest.setRange(0, 60, 85);
        areaOfInterest.setRange(1, 15, 30);
        try {
            grid.derive().subgrid(areaOfInterest);
            fail("Should not have accepted the given AOI.");
        } catch (DisjointExtentException e) {
            assertNotNull(e.getMessage());
        }
    }

    /**
     * Tests {@link GridDerivation#subgrid(GridGeometry)} when the two grid geometries contain only an envelope.
     */
    @Test
    public void testWithEnvelopeOnly() {
        Envelope2D   domain   = new Envelope2D(HardCodedCRS.WGS84, 10, 20, 110, 70);
        Envelope2D   request  = new Envelope2D(HardCodedCRS.WGS84, -5, 25, 100, 90);
        Envelope2D   expected = new Envelope2D(HardCodedCRS.WGS84, 10, 25,  85, 65);
        GridGeometry grid1    = new GridGeometry(domain);
        GridGeometry grid2    = new GridGeometry(request);
        GridGeometry subgrid  = grid1.derive().subgrid(grid2).build();
        assertTrue(subgrid.isEnvelopeOnly());
        assertEnvelopeEquals(expected, subgrid.getEnvelope(), STRICT);
        /*
         * Test same envelope but with different axis order. The request uses a different CRS,
         * but the result shall stay in the same CRS as the initial grid geometry.
         */
        request.setCoordinateReferenceSystem(HardCodedCRS.WGS84_LATITUDE_FIRST);
        request.setRect(25, -5, 90, 100);
        grid2   = new GridGeometry(request);
        subgrid = grid1.derive().subgrid(grid2).build();
        assertSame(HardCodedCRS.WGS84, subgrid.getCoordinateReferenceSystem());
        assertTrue(subgrid.isEnvelopeOnly());
        assertEnvelopeEquals(expected, subgrid.getEnvelope(), STRICT);
    }

    /**
     * Tests {@link GridDerivation#margin(int...)} when no other operation is requested.
     */
    @Test
    public void testWithMarginOnly() {
        final var grid = new GridGeometry(
                new GridExtent(10, 20), PixelInCell.CELL_CENTER,
                MathTransforms.linear(new Matrix3(
                        2, 0, 0,
                        0, 3, 0,
                        0, 0, 1)), HardCodedCRS.WGS84);

        final GridGeometry expanded = grid.derive().margin(4,5).clipping(GridClippingMode.BORDER_EXPANSION).build();
        assertSame(grid.gridToCRS, expanded.gridToCRS);
        assertExtentEquals(new long[] {-4, -5},
                           new long[] {13, 24}, expanded.getExtent());

        assertEnvelopeEquals(new Envelope2D(null, -1,  -1.5, 20, 60),     grid.getEnvelope(), STRICT);
        assertEnvelopeEquals(new Envelope2D(null, -9, -16.5, 36, 90), expanded.getEnvelope(), STRICT);
    }
}
