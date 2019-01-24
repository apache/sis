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
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.ReferencingAssert.*;
import org.junit.Ignore;
import org.opengis.geometry.Envelope;


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
    static void assertExtentEquals(final long[] low, final long[] high, final GridExtent extent) {
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
     * Test derivate grid geometry with an envelope crossing the antimeridian.
     */
    @Test
    @Ignore
    public void testSubGridCrossingAntiMeridian() {

        final GridExtent extent = new GridExtent(200, 180);
        final AffineTransform2D gridToCrs = new AffineTransform2D(1, 0, 0, -1, 80, 90);
        final GridGeometry grid = new GridGeometry(extent, PixelInCell.CELL_CORNER, gridToCrs, CommonCRS.WGS84.normalizedGeographic());

        final GeneralEnvelope aoi = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
        aoi.setRange(0, 140, -179);
        aoi.setRange(1, -90, 90);

        final GridGeometry subgrid = grid.derive().subgrid(aoi).build();
        Envelope subEnv = subgrid.getEnvelope();
        assertEquals(aoi, subEnv);
    }
}
