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

import org.opengis.geometry.Envelope;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
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
 * @version 1.0
 * @since   1.0
 * @module
 */
@DependsOn(GridGeometryTest.class)
public final strictfp class GridDerivationTest extends TestCase {
    /**
     * Tests {@link GridDerivation#subgrid(Envelope, double...)}.
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
         * If we do not ask for subsamplings, the 'gridToCRS' transforms shall be the same than the 'target' geometry.
         * The envelope is the intersection of the envelopes of 'source' and 'target' geometries, documented above.
         */
        GridGeometry tg = change.build();
        assertSame("extent",      extent, tg.getExtent());
        assertSame("CELL_CORNER", target.getGridToCRS(PixelInCell.CELL_CORNER), tg.getGridToCRS(PixelInCell.CELL_CORNER));
        assertSame("CELL_CENTER", target.getGridToCRS(PixelInCell.CELL_CENTER), tg.getGridToCRS(PixelInCell.CELL_CENTER));
        GeneralEnvelope expected = new GeneralEnvelope(2);
        expected.setRange(0,  4200, 11300);
        expected.setRange(1, -7501,  1500);
        assertEnvelopeEquals(expected, tg.getEnvelope(), STRICT);
        /*
         * If we ask for subsamplings, then the envelope should be approximately the same or smaller. Note that without
         * the clipping documented in GridExtent(GridExtent, int...) constructor, the envelope could be larger.
         */
        tg = change.subsample(50, 300).build();
        assertEnvelopeEquals(expected, tg.getEnvelope(), STRICT);
        assertMatrixEquals("gridToCRS", new Matrix3(
                100,    0, 200,
                  0, -300, 600,
                  0,    0,   1), MathTransforms.getMatrix(tg.getGridToCRS(PixelInCell.CELL_CORNER)), STRICT);
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

        final GeneralEnvelope aoi = new GeneralEnvelope(HardCodedCRS.WGS84);
        aoi.setRange(0, 140, -179);                 // Cross anti-meridian.
        aoi.setRange(1, -90,   90);

        final GridGeometry subgrid = grid.derive().subgrid(aoi).build();
        final Envelope subEnv = subgrid.getEnvelope();
        aoi.setRange(0, 140, 181);
        assertEnvelopeEquals(aoi, subEnv);
    }
}
