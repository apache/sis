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

import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.matrix.Matrices;
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


/**
 * Tests the {@link GridGeometry} implementation.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
@DependsOn(GridExtentTest.class)
public final strictfp class GridGeometryTest extends TestCase {
    /**
     * Verifies grid extent coordinates.
     */
    private static void assertExtentEquals(final long[] low, final long[] high, final GridExtent extent) {
        assertArrayEquals("extent.low",  low,  extent.getLow() .getCoordinateValues());
        assertArrayEquals("extent.high", high, extent.getHigh().getCoordinateValues());
    }

    /**
     * Tests construction with an identity transform mapping pixel corner.
     */
    @Test
    public void testFromPixelCorner() {
        final long[]         low     = new long[] {100, 300, 3, 6};
        final long[]         high    = new long[] {200, 400, 4, 7};
        final GridExtent    extent   = new GridExtent(null, low, high, true);
        final MathTransform identity = MathTransforms.identity(4);
        final GridGeometry  grid     = new GridGeometry(extent, PixelInCell.CELL_CORNER, identity, null);
        /*
         * Verify properties that should be stored "as-is".
         */
        final MathTransform trCorner = grid.getGridToCRS(PixelInCell.CELL_CORNER);
        assertSame("gridToCRS", identity, trCorner);
        assertExtentEquals(low, high, grid.getExtent());
        /*
         * Verify computed math transform.
         */
        final MathTransform trCenter = grid.getGridToCRS(PixelInCell.CELL_CENTER);
        assertNotSame(trCenter, trCorner);
        assertFalse ("gridToCRS.isIdentity",          trCenter.isIdentity());
        assertEquals("gridToCRS.sourceDimensions", 4, trCenter.getSourceDimensions());
        assertEquals("gridToCRS.targetDimensions", 4, trCenter.getTargetDimensions());
        assertMatrixEquals("gridToCRS", Matrices.create(5, 5, new double[] {
                1, 0, 0, 0, 0.5,
                0, 1, 0, 0, 0.5,
                0, 0, 1, 0, 0.5,
                0, 0, 0, 1, 0.5,
                0, 0, 0, 0, 1}), MathTransforms.getMatrix(trCenter), STRICT);
        /*
         * Verify the envelope, which should have been computed using the given math transform as-is.
         */
        assertEnvelopeEquals(new GeneralEnvelope(
                new double[] {100, 300, 3, 6},
                new double[] {201, 401, 5, 8}), grid.getEnvelope(), STRICT);
        /*
         * Verify other computed properties.
         */
        assertArrayEquals("resolution", new double[] {1, 1, 1, 1}, grid.getResolution(false), STRICT);
        assertTrue("isConversionLinear", grid.isConversionLinear(0, 1, 2, 3));
    }

    /**
     * Tests construction with an identity transform mapping pixel center.
     * This results a 0.5 pixel shifts in the "real world" envelope.
     */
    @Test
    public void testFromPixelCenter() {
        final long[]        low      = new long[] { 0,   0, 2};
        final long[]        high     = new long[] {99, 199, 4};
        final GridExtent    extent   = new GridExtent(null, low, high, true);
        final MathTransform identity = MathTransforms.identity(3);
        final GridGeometry  grid     = new GridGeometry(extent, PixelInCell.CELL_CENTER, identity, null);
        /*
         * Verify properties that should be stored "as-is".
         */
        final MathTransform trCenter = grid.getGridToCRS(PixelInCell.CELL_CENTER);
        assertSame("gridToCRS", identity, trCenter);
        assertExtentEquals(low, high, grid.getExtent());
        /*
         * Verify computed math transform.
         */
        final MathTransform trCorner = grid.getGridToCRS(PixelInCell.CELL_CORNER);
        assertNotSame(trCenter, trCorner);
        assertFalse ("gridToCRS.isIdentity",          trCorner.isIdentity());
        assertEquals("gridToCRS.sourceDimensions", 3, trCorner.getSourceDimensions());
        assertEquals("gridToCRS.targetDimensions", 3, trCorner.getTargetDimensions());
        assertMatrixEquals("gridToCRS", new Matrix4(
                1, 0, 0, -0.5,
                0, 1, 0, -0.5,
                0, 0, 1, -0.5,
                0, 0, 0,  1), MathTransforms.getMatrix(trCorner), STRICT);
        /*
         * Verify the envelope, which should have been computed using the math transform shifted by 0.5.
         */
        assertEnvelopeEquals(new GeneralEnvelope(
                new double[] {-0.5,  -0.5, 1.5},
                new double[] {99.5, 199.5, 4.5}), grid.getEnvelope(), STRICT);
        /*
         * Verify other computed properties.
         */
        assertArrayEquals("resolution", new double[] {1, 1, 1}, grid.getResolution(false), STRICT);
        assertTrue("isConversionLinear", grid.isConversionLinear(0, 1, 2));
    }

    /**
     * Tests the {@link GridGeometry#GridGeometry(GridGeometry, GridExtent, MathTransform)} constructor.
     *
     * @throws TransformException if an error occurred while using the "grid to CRS" transform.
     */
    @Test
    public void testFromOther() throws TransformException {
        long[]        low       = new long[] {  1,   3, 2};
        long[]        high      = new long[] {101, 203, 4};
        GridExtent    extent    = new GridExtent(null, low, high, false);
        MathTransform gridToCRS = MathTransforms.translation(5, 7, 8);
        GridGeometry  grid      = new GridGeometry(extent, PixelInCell.CELL_CENTER, gridToCRS, null);

        low    = new long[] { 11,  35, 20};
        high   = new long[] {120, 250, 39};
        extent = new GridExtent(null, low, high, false);
        grid   = new GridGeometry(grid, extent, MathTransforms.scale(2, 1, 3));
        assertSame(extent, grid.getExtent());
        assertMatrixEquals("gridToCRS", new Matrix4(
                2, 0, 0, 5,
                0, 1, 0, 7,     // Combination of above scales (diagonal) and translation (last column).
                0, 0, 3, 8,
                0, 0, 0, 1), MathTransforms.getMatrix(grid.getGridToCRS(PixelInCell.CELL_CENTER)), STRICT);
    }

    /**
     * Tests construction from a <cite>grid to CRS</cite> having a 0.5 pixel translation.
     * This translation happens in transform mapping <cite>pixel center</cite> when the
     * corresponding <cite>pixel corner</cite> transformation is identity.
     */
    @Test
    public void testShifted() {
        final long[]        low      = new long[] {100, 300};
        final long[]        high     = new long[] {200, 400};
        final GridExtent    extent   = new GridExtent(null, low, high, true);
        final MathTransform identity = MathTransforms.linear(new Matrix3(
                1, 0, 0.5,
                0, 1, 0.5,
                0, 0, 1));
        final GridGeometry grid = new GridGeometry(extent, PixelInCell.CELL_CENTER, identity, null);
        assertTrue("gridToCRS.isIdentity", grid.getGridToCRS(PixelInCell.CELL_CORNER).isIdentity());
    }

    /**
     * Tests construction with a non-linear component in the transform.
     */
    @Test
    public void testNonLinear() {
        final GridExtent extent = new GridExtent(
                new DimensionNameType[] {
                    DimensionNameType.COLUMN,
                    DimensionNameType.ROW,
                    DimensionNameType.VERTICAL,
                    DimensionNameType.TIME
                },
                new long[] {  0,   0, 2, 6},
                new long[] {100, 200, 3, 9}, false);
        final MathTransform horizontal = MathTransforms.linear(new Matrix3(
                0.5, 0,    12,
                0,   0.25, -2,
                0,   0,     1));
        final MathTransform vertical  = MathTransforms.interpolate(null, new double[] {1, 2, 4, 10});
        final MathTransform temporal  = MathTransforms.linear(3600, 60);
        final MathTransform gridToCRS = MathTransforms.compound(horizontal, vertical, temporal);
        final GridGeometry  grid      = new GridGeometry(extent, PixelInCell.CELL_CENTER, gridToCRS, null);
        assertArrayEquals("resolution", new double[] {0.5, 0.25,        6.0, 3600}, grid.getResolution(true),  STRICT);
        assertArrayEquals("resolution", new double[] {0.5, 0.25, Double.NaN, 3600}, grid.getResolution(false), STRICT);
        assertFalse("isConversionLinear", grid.isConversionLinear(0, 1, 2, 3));
        assertTrue ("isConversionLinear", grid.isConversionLinear(0, 1,    3));
    }

    /**
     * Tests the construction from a geospatial envelope.
     */
    @Test
    public void testFromGeospatialEnvelope() {
        final GeneralEnvelope envelope = new GeneralEnvelope(HardCodedCRS.WGS84_φλ);
        envelope.setRange(0, -70.001, +80.002);
        envelope.setRange(1,   4.997,  15.003);
        final MathTransform gridToCRS = MathTransforms.linear(new Matrix3(
            0,   0.5, -90,
            0.5, 0,  -180,
            0,   0,     1));
        final GridGeometry grid = new GridGeometry(PixelInCell.CELL_CORNER, gridToCRS, envelope, GridRoundingMode.NEAREST);
        assertExtentEquals(
                new long[] {370, 40},
                new long[] {389, 339}, grid.getExtent());
        assertEnvelopeEquals(new GeneralEnvelope(
                new double[] {-70,  5},
                new double[] {+80, 15}), grid.getEnvelope(), STRICT);
        assertArrayEquals("resolution", new double[] {0.5, 0.5}, grid.getResolution(false), STRICT);
        assertMatrixEquals("gridToCRS", new Matrix3(
                0,   0.5, -89.75,
                0.5, 0,  -179.75,
                0,   0,     1), MathTransforms.getMatrix(grid.getGridToCRS(PixelInCell.CELL_CENTER)), STRICT);
    }

    /**
     * Tests {@link GridGeometry#subExtent(Envelope, GridRoundingMode)}.
     */
    @Test
    @DependsOnMethod("testFromGeospatialEnvelope")
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
                           new long[] {389, 339, 10}, grid.subExtent(envelope, GridRoundingMode.NEAREST));
    }

    /**
     * Tests {@link GridGeometry#subExtent(Envelope, GridRoundingMode)} with a non-linear "grid to CRS" transform.
     */
    @Test
    @DependsOnMethod({"testNonLinear", "testSubExtent"})
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
        final GridExtent actual = grid.subExtent(envelope, GridRoundingMode.NEAREST);
        assertEquals(extent.getAxisType(0), actual.getAxisType(0));
        assertExtentEquals(new long[] { 56, 69, 2},
                           new long[] {130, 73, 4}, actual);
    }

    /**
     * Tests {@link GridGeometry#subgrid(Envelope, GridRoundingMode, double...)}.
     *
     * @throws TransformException if an error occurred during computation.
     */
    @Test
    @DependsOnMethod({"testFromGeospatialEnvelope", "testSubExtent"})
    public void testSubgrid() throws TransformException {
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
        grid = grid.subgrid(envelope, GridRoundingMode.NEAREST, 1, 2);
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
        grid = grid.subgrid(envelope, GridRoundingMode.NEAREST, 3, 2);
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
     * Tests {@link GridGeometry#slice(DirectPosition)}.
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
        GridGeometry slice = grid.slice(new GeneralDirectPosition(Double.NaN, Double.NaN, 15));
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
        slice = grid.slice(p);
        assertNotSame(grid, slice);
        assertSame("gridToCRS", grid.gridToCRS, slice.gridToCRS);
        assertExtentEquals(expectedLow, expectedHigh, slice.getExtent());
    }

    /**
     * Tests {@link GridGeometry#reduce(int...)}.
     */
    @Test
    public void testReduce() {
        final GridGeometry grid = new GridGeometry(
                new GridExtent(null, new long[] {336, 20, 4}, new long[] {401, 419, 10}, true),
                PixelInCell.CELL_CORNER, MathTransforms.linear(new Matrix4(
                        0,   0.5, 0,  -90,
                        0.5, 0,   0, -180,
                        0,   0,   2,    3,
                        0,   0,   0,    1)), HardCodedCRS.GEOID_3D);
        /*
         * Tests on the two first dimensions.
         */
        GridGeometry reduced = grid.reduce(0, 1);
        assertNotSame(grid, reduced);
        assertExtentEquals(new long[] {336, 20}, new long[] {401, 419}, reduced.getExtent());
        assertSame("CRS", HardCodedCRS.WGS84, reduced.getCoordinateReferenceSystem());
        assertArrayEquals("resolution", new double[] {0.5, 0.5}, reduced.getResolution(false), STRICT);
        assertMatrixEquals("gridToCRS", new Matrix3(
                  0, 0.5,  -90,
                  0.5, 0, -180,
                  0,   0,    1), MathTransforms.getMatrix(reduced.getGridToCRS(PixelInCell.CELL_CORNER)), STRICT);
        /*
         * Tests on the last dimension.
         */
        reduced = grid.reduce(2);
        assertNotSame(grid, reduced);
        assertExtentEquals(new long[] {4}, new long[] {10}, reduced.getExtent());
        assertSame("CRS", HardCodedCRS.GRAVITY_RELATED_HEIGHT, reduced.getCoordinateReferenceSystem());
        assertArrayEquals("resolution", new double[] {2}, reduced.getResolution(false), STRICT);
        assertMatrixEquals("gridToCRS", new Matrix2(
                  2, 3,
                  0, 1), MathTransforms.getMatrix(reduced.getGridToCRS(PixelInCell.CELL_CORNER)), STRICT);
    }
}
