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
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.matrix.Matrix2;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.HardCodedConversions;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.referencing.ExtendedPrecisionMatrix;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.test.Assert.assertMatrixEquals;
import static org.apache.sis.referencing.Assertions.assertEnvelopeEquals;


/**
 * Tests the {@link GridGeometry} implementation.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @version 1.4
 * @since   1.0
 */
@DependsOn(GridExtentTest.class)
public final class GridGeometryTest extends TestCase {
    /**
     * Verifies grid extent coordinates.
     */
    static void assertExtentEquals(final long[] low, final long[] high, final GridExtent extent) {
        assertArrayEquals("extent.low",  low,  extent.getLow() .getCoordinateValues());
        assertArrayEquals("extent.high", high, extent.getHigh().getCoordinateValues());
    }

    /**
     * Verifies the shift between the two {@code gridToCRS} transforms.
     * This method should be invoked when the transforms are linear.
     *
     * @param  grid  the grid geometry to validate.
     */
    private static void verifyGridToCRS(final GridGeometry grid) {
        final Matrix tr1 = MathTransforms.getMatrix(grid.getGridToCRS(PixelInCell.CELL_CENTER));
        final Matrix tr2 = MathTransforms.getMatrix(grid.getGridToCRS(PixelInCell.CELL_CORNER));
        final Matrix m;
        try {
            /*
             * Multiply the matrices instead of concatenating the transforms
             * for making sure that we do not get cached transforms.
             */
            m = MatrixSIS.castOrCopy(tr2).inverse().multiply(tr1);
        } catch (NoninvertibleTransformException e) {
            throw new AssertionError(e);
        }
        /*
         * Example of expected matrix (size may vary):
         * ┌              ┐
         * │ 1  0  0  0.5 │
         * │ 0  1  0  0.5 │
         * │ 0  0  1  0.5 │
         * │ 0  0  0  1   │
         * └              ┘
         */
        for (int j=m.getNumRow(); --j >=0;) {
            double expected = 0.5;                          // Expected translation term in last column.
            for (int i=m.getNumCol(); --i >= 0;) {
                if (i == j) expected = 1;                   // Expected value on the diagonal.
                final double actual = m.getElement(j,i);
                if (actual != expected) {
                    fail("Expected " + expected + " but got " + actual + " in following matrix:\n" + m);
                }
                expected = 0;                               // For all values other than diagonal and translation.
            }
        }
    }

    /**
     * Tests construction with an identity transform mapping pixel corner.
     */
    @Test
    public void testFromPixelCorner() {
        final long[] low   = new long[] {100, 300, 3, 6};
        final long[] high  = new long[] {200, 400, 4, 7};
        final var extent   = new GridExtent(null, low, high, true);
        final var identity = MathTransforms.identity(4);
        final var grid     = new GridGeometry(extent, PixelInCell.CELL_CORNER, identity, null);
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
        verifyGridToCRS(grid);
    }

    /**
     * Tests construction with an identity transform mapping pixel center.
     * This results a 0.5 pixel shifts in the "real world" envelope.
     */
    @Test
    public void testFromPixelCenter() {
        final long[] low   = new long[] { 0,   0, 2};
        final long[] high  = new long[] {99, 199, 4};
        final var extent   = new GridExtent(null, low, high, true);
        final var identity = MathTransforms.identity(3);
        final var grid     = new GridGeometry(extent, PixelInCell.CELL_CENTER, identity, null);
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
        verifyGridToCRS(grid);
    }

    /**
     * Tests the {@link GridGeometry#GridGeometry(GridGeometry, GridExtent, MathTransform)} constructor.
     * The math transform used for this test map to pixel corners.
     *
     * @throws TransformException if an error occurred while using the "grid to CRS" transform.
     */
    @Test
    public void testFromOtherDefinedAtCorner() throws TransformException {
        long[] low    = new long[] {  1,   3, 2};
        long[] high   = new long[] {101, 203, 4};
        var extent    = new GridExtent(null, low, high, false);
        var gridToCRS = MathTransforms.translation(5, 7, 8);
        var grid      = new GridGeometry(extent, PixelInCell.CELL_CORNER, gridToCRS, null);

        low    = new long[] { 11,  35, 20};
        high   = new long[] {120, 250, 39};
        extent = new GridExtent(null, low, high, false);
        grid   = new GridGeometry(grid, extent, MathTransforms.scale(2, 1, 3));
        assertSame(extent, grid.getExtent());
        assertMatrixEquals("gridToCRS", new Matrix4(
                2, 0, 0, 5,
                0, 1, 0, 7,     // Combination of above scales (diagonal) and translation (last column).
                0, 0, 3, 8,
                0, 0, 0, 1), MathTransforms.getMatrix(grid.getGridToCRS(PixelInCell.CELL_CORNER)), STRICT);
        /*
         * Verify other computed properties.
         */
        assertArrayEquals("resolution", new double[] {2, 1, 3}, grid.getResolution(false), STRICT);
        assertTrue("isConversionLinear", grid.isConversionLinear(0, 1, 2));
        verifyGridToCRS(grid);
    }

    /**
     * Tests the adjustment done for pixel center in
     * {@link GridGeometry#GridGeometry(GridGeometry, GridExtent, MathTransform)} constructor.
     * We check envelopes as a more intuitive way to verify consistency than inspecting the math transforms.
     *
     * @throws TransformException if an error occurred while using the "grid to CRS" transform.
     */
    @Test
    public void testFromOtherDefinedAtCenter() throws TransformException {
        GridExtent extent = new GridExtent(126, 197);
        GridGeometry grid = new GridGeometry(extent, PixelInCell.CELL_CENTER, MathTransforms.identity(2), HardCodedCRS.WGS84);
        GeneralEnvelope expected = new GeneralEnvelope(new double[] {-0.5, -0.5}, new double[] {125.5, 196.5});
        assertEnvelopeEquals(expected, grid.getEnvelope(), STRICT);
        verifyGridToCRS(grid);
        /*
         * Derive a new grid geometry with 10×10 times more cells. The geographic area should be unchanged.
         */
        extent = extent.resize(1260, 1970);
        grid = new GridGeometry(grid, extent, MathTransforms.scale(0.1, 0.1));
        assertEnvelopeEquals(expected, grid.getEnvelope(), STRICT);
        verifyGridToCRS(grid);
        /*
         * If we create a grid geometry with identical properties, the envelope computed by that grid geometry would
         * be different than the envelope computed above if the "grid to CRS" transform is not correctly adjusted.
         */
        final GridGeometry alternative = new GridGeometry(grid.getExtent(), PixelInCell.CELL_CENTER,
                 grid.getGridToCRS(PixelInCell.CELL_CENTER), grid.getCoordinateReferenceSystem());
        assertEnvelopeEquals(expected, alternative.getEnvelope(), STRICT);
        verifyGridToCRS(grid);
    }

    /**
     * Tests construction from a <cite>grid to CRS</cite> having a 0.5 pixel translation.
     * This translation happens in transform mapping <cite>pixel center</cite> when the
     * corresponding <cite>pixel corner</cite> transformation is identity.
     */
    @Test
    public void testShifted() {
        final long[] low    = new long[] {100, 300};
        final long[] high   = new long[] {200, 400};
        final var extent    = new GridExtent(null, low, high, true);
        final var gridToCRS = MathTransforms.linear(new Matrix3(
                1, 0, 0.5,
                0, 1, 0.5,
                0, 0, 1));
        final GridGeometry grid = new GridGeometry(extent, PixelInCell.CELL_CENTER, gridToCRS, null);
        assertTrue("gridToCRS.isIdentity", grid.getGridToCRS(PixelInCell.CELL_CORNER).isIdentity());
        verifyGridToCRS(grid);
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
     * The "grid to CRS" transform is explicitly given.
     */
    @Test
    public void testFromGeospatialEnvelope() {
        final GeneralEnvelope envelope = new GeneralEnvelope(HardCodedCRS.WGS84_LATITUDE_FIRST);
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
        /*
         * Verify other computed properties.
         */
        assertArrayEquals("resolution", new double[] {0.5, 0.5}, grid.getResolution(false), STRICT);
        assertTrue("isConversionLinear", grid.isConversionLinear(0, 1));
        verifyGridToCRS(grid);
    }

    /**
     * Tests the construction from a geospatial envelope and an extent.
     * The "grid to CRS" transform is inferred.
     *
     * @see GridExtentTest#testCornerToCRS()
     */
    @Test
    public void testFromExtentAndEnvelope() {
        final GeneralEnvelope aoi = new GeneralEnvelope(HardCodedCRS.WGS84);
        aoi.setRange(0,  40, 55);
        aoi.setRange(1, -10, 70);
        final GridExtent extent = new GridExtent(null,
                new long[] {-20, -25},
                new long[] { 10,  15}, false);
        /*
         * Simplest case: no axis flip.
         * Verification:  y  =  2 × −25 + 40  =  −10  (the minimum value declared in envelope).
         */
        GridGeometry grid = new GridGeometry(extent, aoi, GridOrientation.HOMOTHETY);
        Matrix matrix = MathTransforms.getMatrix(grid.getGridToCRS(PixelInCell.CELL_CORNER));
        assertMatrixEquals("cornerToCRS", new Matrix3(
                0.5,  0,   50,
                0,    2,   40,
                0,    0,    1), matrix, STRICT);

        // Verify other computed properties.
        assertArrayEquals("resolution", new double[] {0.5, 2}, grid.getResolution(false), STRICT);
        assertTrue("isConversionLinear", grid.isConversionLinear(0, 1));
        assertSame("extent", extent, grid.getExtent());
        verifyGridToCRS(grid);
        /*
         * Same envelope and extent, but flip Y axis.
         * Verification:  y  =  −2 × −25 + 20  =  70  (the maximum value declared in envelope).
         */
        grid = new GridGeometry(extent, aoi, GridOrientation.REFLECTION_Y);
        matrix = MathTransforms.getMatrix(grid.getGridToCRS(PixelInCell.CELL_CORNER));
        assertMatrixEquals("cornerToCRS", new Matrix3(
                0.5,  0,   50,
                0,   -2,   20,
                0,    0,    1), matrix, STRICT);

        // Verify other computed properties.
        assertArrayEquals("resolution", new double[] {0.5, 2}, grid.getResolution(false), STRICT);
        assertTrue("isConversionLinear", grid.isConversionLinear(0, 1));
        assertSame("extent", extent, grid.getExtent());
        verifyGridToCRS(grid);
        /*
         * The use of `DISPLAY` mode in this particular case should be equivalent ro `REFLECTION_Y`.
         */
        assertEquals(grid, new GridGeometry(extent, aoi, GridOrientation.DISPLAY));
        /*
         * Test when only an envelope is specified.
         * The grid orientation should have no effect.
         */
        assertEnvelopeEquals(aoi, new GridGeometry(null, aoi, GridOrientation.DISPLAY).getEnvelope());
    }

    /**
     * Tests the construction from a geospatial envelope and an extent with reordering of axes
     * for matching display convention.
     *
     * @see GridExtentTest#testCornerToCRS()
     */
    @Test
    public void testFromExtentAndDisplayEnvelope() {
        final GeneralEnvelope aoi = new GeneralEnvelope(HardCodedCRS.WGS84_LATITUDE_FIRST);
        aoi.setRange(1,  40, 55);
        aoi.setRange(0, -10, 70);
        final GridExtent extent = new GridExtent(null,
                new long[] {-25, -20},
                new long[] { 15,  10}, false);
        /*
         * Same case than the one tested by `testFromExtentAndEnvelope()`,
         * but with axis order swapped.
         */
        GridGeometry grid = new GridGeometry(extent, aoi, GridOrientation.DISPLAY);
        Matrix matrix = MathTransforms.getMatrix(grid.getGridToCRS(PixelInCell.CELL_CORNER));
        assertMatrixEquals("cornerToCRS", new Matrix3(
                0,  0.5,   50,
               -2,    0,   20,
                0,    0,    1), matrix, STRICT);

        // Verify other computed properties.
        assertArrayEquals("resolution", new double[] {0.5, 2}, grid.getResolution(false), STRICT);
        assertTrue("isConversionLinear", grid.isConversionLinear(0, 1));
        assertSame("extent", extent, grid.getExtent());
        verifyGridToCRS(grid);
        /*
         * Same extent and envelope, but reordering extend dimensions
         * instead of `gridToCRS` columns.
         */
        grid = new GridGeometry(extent, aoi, GridOrientation.DISPLAY.canReorderGridAxis(true));
        matrix = MathTransforms.getMatrix(grid.getGridToCRS(PixelInCell.CELL_CORNER));
        assertMatrixEquals("cornerToCRS", new Matrix3(
                0.5,  0,   50,
                0,   -2,   20,
                0,    0,    1), matrix, STRICT);

        assertExtentEquals(
                new long[] {-20, -25},
                new long[] {  9,  14}, grid.getExtent());

        // Verify other computed properties.
        assertArrayEquals("resolution", new double[] {0.5, 2}, grid.getResolution(false), STRICT);
        assertTrue("isConversionLinear", grid.isConversionLinear(0, 1));
        assertNotSame("extent", extent, grid.getExtent());
        verifyGridToCRS(grid);
    }

    /**
     * Verifies "grid to CRS" coefficients of a simple grid geometry.
     */
    @Test
    public void testGetGridToCRS() {
        final var extent = new GridExtent(null, new long[3], new long[3], true);
        final var bbox   = new GeneralEnvelope(new double[] {-180, -90, -1000}, new double[] {180, 90, 2000});
        final var grid   = new GridGeometry(extent, bbox, GridOrientation.HOMOTHETY);
        assertEquals(extent, grid.getExtent());
        assertEnvelopeEquals(bbox, grid.getEnvelope());
        /*
         * Verify internal consistency of the "grid to CRS" matrix.
         * This consistency is violated if `ProjectiveTransform` constructor takes an `Number[]`
         * array which is not protected against changes in the `GeneralMatrix` that own that array.
         */
        final Matrix   gridToCRS = MathTransforms.getMatrix(grid.getGridToCRS(PixelInCell.CELL_CORNER));
        final Number[] numbers   = ((ExtendedPrecisionMatrix) gridToCRS).getElementAsNumbers(false);
        final double[] elements  = MatrixSIS.castOrCopy(gridToCRS).getElements();
        assertArrayEquals(new double[] {360, 0, 0, -180, 0, 180, 0, -90, 0, 0, 3000, -1000, 0, 0, 0, 1}, elements, STRICT);
        assertEquals(elements.length, numbers.length);
        for (int i=0; i<elements.length; i++) {
            final double expected = elements[i];
            final Number actual   = numbers [i];
            if (expected == 0) {
                assertNull(actual);
            } else {
                assertEquals(expected, actual.doubleValue(), STRICT);
            }
        }
    }

    /**
     * Tests {@link GridGeometry#getEnvelope(CoordinateReferenceSystem)}.
     *
     * @throws TransformException if coordinates cannot be transformed.
     */
    @Test
    public void testGetEnvelope() throws TransformException {
        GridGeometry grid = new GridGeometry(
                new GridExtent(12, 18),
                PixelInCell.CELL_CORNER,
                MathTransforms.linear(new Matrix3(
                    0.25, 0,    -2,
                    0,   -0.25, -3,
                    0,    0,     1)),
                HardCodedCRS.WGS84);

        Envelope envelope = grid.getEnvelope(HardCodedCRS.WGS84);
        assertSame(envelope, grid.getEnvelope());
        assertEnvelopeEquals(new GeneralEnvelope(
                new double[] {-2, -7.5},
                new double[] { 1, -3.0}), envelope, STRICT);

        envelope = grid.getEnvelope(HardCodedCRS.WGS84_LATITUDE_FIRST);
        assertEnvelopeEquals(new GeneralEnvelope(
                new double[] {-7.5, -2},
                new double[] {-3.0,  1}), envelope, STRICT);

        envelope = grid.getEnvelope(HardCodedConversions.mercator());
        assertEnvelopeEquals(new GeneralEnvelope(
                new double[] {-222638.98, -831717.36},
                new double[] { 111319.49, -331876.53}), envelope, 0.01);
    }

    /**
     * Tests {@link GridGeometry#upsample(int...)}.
     */
    @Test
    public void testUpsample() {
        final GridGeometry grid;
        {   // Source grid
            final GridExtent extent = new GridExtent(null, new long[] {10,-8}, new long[] {100, 50}, false);
            final Matrix3 mat = new Matrix3(
                    1,  0, 10,
                    0, -2, 50,
                    0,  0,  1);
            final MathTransform gridToCRS = MathTransforms.linear(mat);
            grid = new GridGeometry(extent, PixelInCell.CELL_CENTER, gridToCRS, HardCodedCRS.CARTESIAN_2D);
        }
        final GridGeometry upsampled = grid.upsample(4, 4);
        final GridGeometry expected;
        {   // Expected grid
            GridExtent extent = new GridExtent(null, new long[] {40,-32}, new long[] {400, 200}, false);
            final Matrix3 mat = new Matrix3(
                    0.25,  0,  9.625,
                    0,  -0.5, 50.750,
                    0,     0,      1);
            final MathTransform gridToCRS = MathTransforms.linear(mat);
            expected = new GridGeometry(extent, PixelInCell.CELL_CENTER, gridToCRS, HardCodedCRS.CARTESIAN_2D);
        }
        assertSame("envelope", grid.getEnvelope(), upsampled.getEnvelope());
        assertEquals("GridGeometry", expected, upsampled);
        assertArrayEquals("resolution", new double[] {0.25, 0.5}, expected.getResolution(false), STRICT);
    }

    /**
     * Tests {@link GridGeometry#shiftGrid(long[])}.
     */
    @Test
    public void testShiftGrid() {
        GridGeometry grid = new GridGeometry(
                new GridExtent(17, 10),
                PixelInCell.CELL_CENTER,
                MathTransforms.linear(new Matrix3(
                    1,   0,  -7,
                    0,  -1,  50,
                    0,   0,   1)),
                HardCodedCRS.WGS84);
        /*
         * The "real world" envelope should be unchanged by grid translation.
         */
        final Envelope envelope = grid.getEnvelope();
        grid = grid.shiftGrid(12, 15);
        assertExtentEquals(new long[] {12, 15}, new long[] {12 + 16, 15 + 9}, grid.getExtent());
        assertEquals(envelope, grid.getEnvelope());
    }

    /**
     * Tests {@link GridGeometry#relocate(GridExtent)}.
     *
     * @throws TransformException if the relocated envelope cannot be computed.
     */
    @Test
    public void testRelocate() throws TransformException {
        final GridGeometry grid = new GridGeometry(
                new GridExtent(10, 10),
                PixelInCell.CELL_CORNER,
                MathTransforms.linear(new Matrix3(
                    2,  0,  10,
                    0,  3,  20,
                    0,  0,   1)),
                HardCodedCRS.WGS84);

        assertSame(grid, grid.relocate(new GridExtent(10, 10)));
        final GridGeometry relocated = grid.relocate(new GridExtent(20, 20));
        assertSame(grid.gridToCRS,   relocated.gridToCRS);
        assertSame(grid.cornerToCRS, relocated.cornerToCRS);
        assertSame(grid.resolution,  relocated.resolution);
        assertEnvelopeEquals(new GeneralEnvelope(
                new double[] {10, 20},
                new double[] {30, 50}), grid.envelope, STRICT);
        assertEnvelopeEquals(new GeneralEnvelope(
                new double[] {10, 20},
                new double[] {50, 80}), relocated.envelope, STRICT);
    }

    /**
     * Tests {@link GridGeometry#selectDimensions(int[])}.
     */
    @Test
    public void testSelectDimensions() {
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
        GridGeometry reduced = grid.selectDimensions(0, 1);
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
        reduced = grid.selectDimensions(2);
        assertNotSame(grid, reduced);
        assertExtentEquals(new long[] {4}, new long[] {10}, reduced.getExtent());
        assertSame("CRS", HardCodedCRS.GRAVITY_RELATED_HEIGHT, reduced.getCoordinateReferenceSystem());
        assertArrayEquals("resolution", new double[] {2}, reduced.getResolution(false), STRICT);
        assertMatrixEquals("gridToCRS", new Matrix2(
                  2, 3,
                  0, 1), MathTransforms.getMatrix(reduced.getGridToCRS(PixelInCell.CELL_CORNER)), STRICT);
        /*
         * Verify other computed properties.
         */
        assertArrayEquals("resolution", new double[] {0.5, 0.5, 2}, grid.getResolution(false), STRICT);
        assertTrue("isConversionLinear", grid.isConversionLinear(0, 1, 2));
        verifyGridToCRS(grid);
    }

    /**
     * Tests {@link GridGeometry#selectDimensions(int[])} with a {@code gridToCRS} transform having a constant value
     * in one dimension. This method tests indirectly {@link SliceGeometry#findTargetDimensions(MathTransform,
     * GridExtent, double[], int[], int)}.
     */
    @Test
    public void testRemoveScalelessDimension() {
        final GridGeometry grid = new GridGeometry(
                new GridExtent(null, new long[] {336, 20, 4}, new long[] {401, 419, 10}, true),
                PixelInCell.CELL_CORNER, MathTransforms.linear(new Matrix4(
                        0,   0.5, 0,  -90,
                        0.5, 0,   0, -180,
                        0,   0,   0,    3,   // All scale coefficients set to 0.
                        0,   0,   0,    1)), HardCodedCRS.GEOID_3D);

        GridGeometry reduced = grid.selectDimensions(0, 1);
        MathTransform tr = reduced.getGridToCRS(PixelInCell.CELL_CORNER);
        /*
         * If the boolean argument given to the `GridGeometry(GridGeometry, int[], boolean)` constructor was false,
         * we would have a 4×3 matrix identical to the matrix below but with a [0 0 3] row inserted at the commented
         * line. The role of the `GridGeometry.findTargetDimensions(int[])` is to filter that line.
         */
        assertMatrixEquals("gridToCRS", new Matrix3(
                        0,   0.5, -90,
                        0.5, 0,  -180,
        //              0,   0,     3,  // All scale coefficients set to 0.
                        0,   0,     1), MathTransforms.getMatrix(tr), STRICT);
        /*
         * Verify other computed properties.
         */
        assertArrayEquals("resolution", new double[] {0.5, 0.5}, reduced.getResolution(false), STRICT);
        assertTrue("isConversionLinear", reduced.isConversionLinear(0, 1));
        verifyGridToCRS(reduced);
        /*
         * Test again by keeping the dimension without scale instead of discarding it.
         * We have to skip `verifyGridToCRS(reduced)` because matrix is non-invertible.
         */
        reduced = grid.selectDimensions(2);
        tr = reduced.getGridToCRS(PixelInCell.CELL_CORNER);
        assertMatrixEquals("gridToCRS", new Matrix2(0, 3, 0, 1), MathTransforms.getMatrix(tr), STRICT);
    }

    /**
     * Tests {@link GridGeometry#createImageCRS(String, PixelInCell)}.
     */
    @Test
    public void testCreateImageCRS() {
        final GridGeometry gg = new GridGeometry(
                new GridExtent(null, null, new long[] {17, 10, 4}, true),
                PixelInCell.CELL_CENTER,
                MathTransforms.linear(new Matrix4(
                    1,   0,  0, -7,
                    0,  -1,  0, 50,
                    0,   0,  8, 20,
                    0,   0,  0,  1)),
                HardCodedCRS.WGS84_WITH_TIME);

        final DerivedCRS crs = gg.createImageCRS("Horizontal part", PixelInCell.CELL_CENTER);
        assertEquals("Horizontal part", crs.getName().getCode());
        final Matrix mt = MathTransforms.getMatrix(crs.getConversionFromBase().getMathTransform());
        assertSame(HardCodedCRS.WGS84, crs.getBaseCRS());
        assertMatrixEquals("CRS to grid",
                new Matrix3(1,  0,  7,      // Opposite sign because this is the inverse transform.
                            0, -1, 50,      // Opposite sign cancelled by -1 scale factor.
                            0,  0,  1), mt, STRICT);
    }

    /**
     * Tests {@link GridGeometry#createTransformTo(GridGeometry, PixelInCell)}.
     *
     * @throws TransformException if the transform cannot be computed.
     */
    @Test
    public void testCreateTransformTo() throws TransformException {
        final GridGeometry source = new GridGeometry(
                new GridExtent(17, 10),
                PixelInCell.CELL_CENTER,
                MathTransforms.linear(new Matrix3(
                    1,   0,  -7.0,
                    0,  -1,  50.0,
                    0,   0,   1)),
                HardCodedCRS.WGS84);

        final GridGeometry target = new GridGeometry(
                new GridExtent(200, 300),
                PixelInCell.CELL_CENTER,
                MathTransforms.linear(new Matrix3(
                   -0.05,  0,    53.0,
                    0,     0.1,  -8.0,
                    0,     0,     1)),
                HardCodedCRS.WGS84_LATITUDE_FIRST);

        final MathTransform tr = source.createTransformTo(target, PixelInCell.CELL_CENTER);
        assertMatrixEquals("createTransformTo", new Matrix3(
                    0,  20,  60,
                   10,   0,  10,
                    0,   0,   1), MathTransforms.getMatrix(tr), STRICT);
    }
}
