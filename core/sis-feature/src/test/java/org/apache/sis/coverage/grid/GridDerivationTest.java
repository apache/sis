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

import java.util.Collections;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.referencing.crs.DefaultCompoundCRS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.ReferencingAssert.*;
import static org.apache.sis.coverage.grid.GridGeometryTest.assertExtentEquals;


/**
 * Tests the {@link GridDerivation} implementation.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   1.0
 * @module
 */
@DependsOn(GridGeometryTest.class)
public final strictfp class GridDerivationTest extends TestCase {
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
        final GridGeometry grid = new GridGeometry(PixelInCell.CELL_CORNER, gridToCRS, envelope, GridRoundingMode.NEAREST);
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
        assertExtentEquals(new long[] {370,  40,  4},
                           new long[] {389, 339, 10}, grid.derive().subgrid(envelope).getIntersection());
    }

    /**
     * Creates a grid geometry with the given extent and scale for testing purpose.
     * An arbitrary translation of (2,3) is added to the "grid to CRS" conversion.
     */
    private static GridGeometry grid(int xmin, int ymin, int xmax, int ymax, int xScale, int yScale) throws TransformException {
        GridExtent extent = new GridExtent(null, new long[] {xmin, ymin}, new long[] {xmax, ymax}, true);
        Matrix3 gridToCRS = new Matrix3();
        gridToCRS.m00 = xScale;
        gridToCRS.m11 = yScale;
        gridToCRS.m02 = 200;            // Arbitrary translation.
        gridToCRS.m12 = 500;
        return new GridGeometry(extent, PixelInCell.CELL_CORNER, MathTransforms.linear(gridToCRS), null);
    }

    /**
     * Tests the construction from grid geometries having a linear "grid to CRS" conversion.
     *
     * @throws TransformException if an error occurred while computing the grid geometry.
     */
    @Test
    public void testSubgridFromOtherGrid() throws TransformException {
        GridGeometry   source = grid(  10,   -20,  110,  180, 100, -300);     // Envelope x: [1200 … 11300]   y: [-53800 … 6500]
        GridGeometry   target = grid(2000, -1000, 9000, 8000,   2,   -1);     // Envelope x: [4200 … 18202]   y: [ -7501 … 1500]
        GridDerivation change = target.derive().subgrid(source);              // Envelope x: [4200 … 11300]   y: [ -7501 … 1500]
        GridExtent     extent = change.getIntersection();
        GridExtentTest.assertExtentEquals(extent, 0,  2000, 5549);            // Subrange of target extent.
        GridExtentTest.assertExtentEquals(extent, 1, -1000, 8000);
        assertArrayEquals("subsamplings", new int[] {50, 300}, change.getSubsamplings());       // s = scaleSource / scaleTarget
        /*
         * Above (50, 300) subsampling shall be applied and the `gridToCRS` transform adjusted consequently.
         */
        final GridGeometry tg = change.build();
        extent = tg.getExtent();
        GridExtentTest.assertExtentEquals(extent, 0,  40, 110);
        GridExtentTest.assertExtentEquals(extent, 1,  -3,  27);               // NEAREST grid rounding mode.
        assertMatrixEquals("gridToCRS", new Matrix3(
                100,    0, 200,
                  0, -300, 600,
                  0,    0,   1), MathTransforms.getMatrix(tg.getGridToCRS(PixelInCell.CELL_CORNER)), STRICT);
        /*
         * The envelope is the intersection of the envelopes of `source` and `target` grid geometries, documented above.
         * That intersection should be approximately the same or smaller. Note that without the clipping documented in
         * `GridExtent(GridExtent, int...)` constructor, the envelope would have been larger.
         */
        GeneralEnvelope expected = new GeneralEnvelope(2);
        expected.setRange(0,  4200, 11300);
        expected.setRange(1, -7501,  1500);
        assertEnvelopeEquals(expected, tg.getEnvelope(), STRICT);
    }

    /**
     * Tests {@link GridDerivation#subgrid(Envelope, double...)} with a non-linear "grid to CRS" transform.
     */
    @Test
    @DependsOnMethod("testSubExtent")
    public void testSubExtentNonLinear() {
        final GridExtent extent = new GridExtent(
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
        final GeneralEnvelope envelope = new GeneralEnvelope(HardCodedCRS.WGS84);
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
    @DependsOnMethod("testSubExtent")
    public void testSubgridFromEnvelope() throws TransformException {
        final GeneralEnvelope envelope = new GeneralEnvelope(HardCodedCRS.WGS84_φλ);
        envelope.setRange(0, -70, +80);
        envelope.setRange(1,   5,  15);
        final MathTransform gridToCRS = MathTransforms.linear(new Matrix3(
                0,   0.5, -90,
                0.5, 0,  -180,
                0,   0,     1));
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
        assertMatrixEquals("gridToCRS", new Matrix3(
                0, 1,  -90,
                2, 0, -180,
                0, 0,    1), MathTransforms.getMatrix(grid.getGridToCRS(PixelInCell.CELL_CORNER)), STRICT);
        /*
         * A sub-region again but with a requested resolution which is not a divisor of the actual resolution.
         * It will force GridGeometry to adjust the translation term to compensate. We verify that the adustment
         * is correct by verifying that we still get the same envelope.
         */
        grid = grid.derive().subgrid(envelope, 3, 2).build();
        assertExtentEquals(new long[] {94, 13}, new long[] {95, 39}, grid.getExtent());
        assertEnvelopeEquals(envelope, grid.getEnvelope(), STRICT);
        MathTransform cornerToCRS = grid.getGridToCRS(PixelInCell.CELL_CORNER);
        assertMatrixEquals("gridToCRS", new Matrix3(
                0, 3,  -89,
                2, 0, -180,
                0, 0,    1), MathTransforms.getMatrix(cornerToCRS), STRICT);

        DirectPosition2D src = new DirectPosition2D();
        DirectPosition2D tgt = new DirectPosition2D();
        DirectPosition2D exp = new DirectPosition2D();
        src.x = 94; src.y = 13; exp.x = -50; exp.y =  8; assertEquals("Lower corner", exp, cornerToCRS.transform(src, tgt));
        src.x = 96; src.y = 40; exp.x = +31; exp.y = 12; assertEquals("Upper corner", exp, cornerToCRS.transform(src, tgt));
    }

    /**
     * Tests {@link GridDerivation#slice(DirectPosition)}.
     */
    @Test
    public void testSlice() {
        final GridGeometry grid = new GridGeometry(
                new GridExtent(null, new long[] {336, 20, 4}, new long[] {401, 419, 10}, true),
                PixelInCell.CELL_CORNER, MathTransforms.linear(new Matrix4(
                        0,   0.5, 0,  -90,
                        0.5, 0,   0, -180,
                        0,   0,   2,    3,
                        0,   0,   0,    1)), HardCodedCRS.WGS84_3D);
        /*
         * There is two ways to ask for a slice. The first way is to set some coordinates to NaN.
         */
        GridGeometry slice = grid.derive().slice(new GeneralDirectPosition(Double.NaN, Double.NaN, 15)).build();
        assertNotSame(grid, slice);
        assertSame("gridToCRS", grid.gridToCRS, slice.gridToCRS);
        final long[] expectedLow  = {336,  20, 6};
        final long[] expectedHigh = {401, 419, 6};
        assertExtentEquals(expectedLow, expectedHigh, slice.getExtent());
        /*
         * Same test, but using a one-dimensional slice point instead than NaN values.
         * Opportunistically use different units for testing conversions.
         */
        GeneralDirectPosition p = new GeneralDirectPosition(HardCodedCRS.ELLIPSOIDAL_HEIGHT_cm);
        p.setOrdinate(0, 1500);
        slice = grid.derive().slice(p).build();
        assertNotSame(grid, slice);
        assertSame("gridToCRS", grid.gridToCRS, slice.gridToCRS);
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
                new Envelope2D(HardCodedCRS.WGS84_φλ, 42, 172, 13, 51),
                GridRoundingMode.NEAREST);

        final GridGeometry expectedResult = base.derive()
                .slice(new DirectPosition2D(51, 187))
                .build();

        final GridGeometry fromWrapAround = base.derive()
                .slice(new DirectPosition2D(51, -173))
                .build();

        assertEquals("Slice with wrap-around", expectedResult, fromWrapAround);
        assertBetween("Wrapped Y coordinate",
                      base.envelope.getMinimum(1),
                      base.envelope.getMaximum(1),
                      fromWrapAround.envelope.getMedian(1));
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
        final GeneralEnvelope grid3d = new GeneralEnvelope(3);
        grid3d.setEnvelope(0, 0, 0, 1920, 1080, 4);

        final DefaultCompoundCRS crs3d = new DefaultCompoundCRS(
                Collections.singletonMap("name", "geo3d"),
                HardCodedCRS.WGS84,
                HardCodedCRS.TIME);

        final GeneralEnvelope geo3d = new GeneralEnvelope(crs3d);
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
     * Tests deriving a grid geometry with an envelope crossing the antimeridian.
     */
    @Test
    public void testSubgridCrossingAntiMeridian() {
        final GridGeometry grid = new GridGeometry(
                new GridExtent(200, 180), PixelInCell.CELL_CORNER,
                MathTransforms.linear(new Matrix3(
                        1,  0, 80,
                        0, -1, 90,
                        0,  0,  1)), HardCodedCRS.WGS84);

        final GeneralEnvelope areaOfInterest = new GeneralEnvelope(HardCodedCRS.WGS84);
        areaOfInterest.setRange(0, 140, -179);                 // Cross anti-meridian.
        areaOfInterest.setRange(1, -90,   90);

        final GridGeometry subgrid = grid.derive().subgrid(areaOfInterest).build();
        final Envelope subEnv = subgrid.getEnvelope();
        areaOfInterest.setRange(0, 140, 181);
        assertEnvelopeEquals(areaOfInterest, subEnv);
    }

    /**
     * Tests deriving a grid geometry from an area of interest shifted by 360° before or after the grid valid area.
     */
    @Test
    public void testSubgridFromShiftedAOI() {
        final GridGeometry grid = new GridGeometry(
                new GridExtent(20, 140), PixelInCell.CELL_CORNER,
                MathTransforms.linear(new Matrix3(
                        1,  0, 80,
                        0, -1, 70,
                        0,  0,  1)), HardCodedCRS.WGS84);

        final GeneralEnvelope areaOfInterest = new GeneralEnvelope(HardCodedCRS.WGS84);
        areaOfInterest.setRange(0,  70,  90);
        areaOfInterest.setRange(1, -80,  60);

        final GeneralEnvelope expected = new GeneralEnvelope(HardCodedCRS.WGS84);
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
     * {@preformat text
     *                  ┌────────────────────────────────────────────┐
     *                  │             Domain of validity             │
     *                  └────────────────────────────────────────────┘
     *   ┌────────────────────┐                                ┌─────
     *   │  Area of interest  │                                │  AOI
     *   └────────────────────┘                                └─────
     *    ↖………………………………………………………360° period……………………………………………………↗︎
     * }
     */
    @Test
    public void testAreaOfInterestExpansion() {
        final GridGeometry grid = new GridGeometry(
                new GridExtent(340, 140), PixelInCell.CELL_CORNER,
                MathTransforms.linear(new Matrix3(
                        1,  0, 5,
                        0, -1, 70,
                        0,  0,  1)), HardCodedCRS.WGS84);

        final GeneralEnvelope areaOfInterest = new GeneralEnvelope(HardCodedCRS.WGS84);
        areaOfInterest.setRange(0, -30,  40);
        areaOfInterest.setRange(1, -60,  60);

        final GeneralEnvelope expected = new GeneralEnvelope(HardCodedCRS.WGS84);
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
        final GridGeometry grid = new GridGeometry(
                new GridExtent(10, 20), PixelInCell.CELL_CORNER,
                MathTransforms.linear(new Matrix3(
                        2, 0, 0,
                        0, 2, 0,
                        0, 0, 1)), HardCodedCRS.WGS84);

        final GeneralEnvelope areaOfInterest = new GeneralEnvelope(HardCodedCRS.WGS84);
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
     * Tests {@link GridDerivation#subgrid(GridGeometry)} when the two envelopes contain only an envelope.
     */
    @Test
    public void testWithEnvelopeOnly() {
        final GridGeometry g1 = new GridGeometry(null, new Envelope2D(null, 10, 20, 110, 70));
        final GridGeometry g2 = new GridGeometry(null, new Envelope2D(null, -5, 25, 100, 90));
        final GridGeometry r  = g1.derive().subgrid(g2).build();
        assertTrue(r.isEnvelopeOnly());
        assertEnvelopeEquals(new Envelope2D(null, 10, 25, 85, 65), r.getEnvelope(), STRICT);
    }

    /**
     * Tests {@link GridDerivation#margin(int...)} when no other operation is requested.
     */
    @Test
    public void testWithMarginOnly() {
        final GridGeometry grid = new GridGeometry(
                new GridExtent(10, 20), PixelInCell.CELL_CENTER,
                MathTransforms.linear(new Matrix3(
                        2, 0, 0,
                        0, 3, 0,
                        0, 0, 1)), HardCodedCRS.WGS84);

        final GridGeometry expanded = grid.derive().margin(4,5).build();
        assertSame(grid.gridToCRS, expanded.gridToCRS);
        assertExtentEquals(new long[] {-4, -5},
                           new long[] {13, 24}, expanded.getExtent());

        assertEnvelopeEquals(new Envelope2D(null, -1,  -1.5, 20, 60),     grid.getEnvelope(), STRICT);
        assertEnvelopeEquals(new Envelope2D(null, -9, -16.5, 36, 90), expanded.getEnvelope(), STRICT);
    }
}
