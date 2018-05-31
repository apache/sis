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

import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.ReferencingAssert.*;

// Branch-dependent imports
import org.opengis.coverage.grid.GridEnvelope;


/**
 * Tests the {@link GridGeometry} implementation.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final strictfp class GridGeometryTest extends TestCase {
    /**
     * Verifies grid extent coordinates.
     */
    private static void assertExtentEquals(final GridEnvelope extent, final int[] low, final int[] high) {
        assertArrayEquals("extent.low",  low,  extent.getLow() .getCoordinateValues());
        assertArrayEquals("extent.high", high, extent.getHigh().getCoordinateValues());
    }

    /**
     * Tests construction with an identity transform mapping pixel corner.
     *
     * @throws TransformException if an error occurred while using the "grid to CRS" transform.
     */
    @Test
    public void testSimple() throws TransformException {
        final int[]         low      = new int[] {100, 300, 3, 6};
        final int[]         high     = new int[] {200, 400, 4, 7};
        final GridExtent    extent   = new GridExtent(low, high, true);
        final MathTransform identity = MathTransforms.identity(4);
        final GridGeometry  grid     = new GridGeometry(extent, PixelInCell.CELL_CORNER, identity, null);
        /*
         * Verify properties that should be stored "as-is".
         */
        final MathTransform trCorner = grid.getGridToCRS(PixelInCell.CELL_CORNER);
        assertSame("gridToCRS", identity, trCorner);
        assertExtentEquals(grid.getExtent(), low, high);
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
        assertArrayEquals("resolution", new double[] {1, 1, 1, 1}, grid.resolution(false), STRICT);
        assertTrue("isConversionLinear", grid.isConversionLinear(0, 1, 2, 3));
    }

    /**
     * Tests construction with an identity transform mapping pixel center.
     * This results a 0.5 pixel shifts in the "real world" envelope.
     *
     * @throws TransformException if an error occurred while using the "grid to CRS" transform.
     */
    @Test
    public void testShifted() throws TransformException {
        final int[]         low      = new int[] { 0,   0, 2};
        final int[]         high     = new int[] {99, 199, 4};
        final GridExtent    extent   = new GridExtent(low, high, true);
        final MathTransform identity = MathTransforms.identity(3);
        final GridGeometry  grid     = new GridGeometry(extent, PixelInCell.CELL_CENTER, identity, null);
        /*
         * Verify properties that should be stored "as-is".
         */
        final MathTransform trCenter = grid.getGridToCRS(PixelInCell.CELL_CENTER);
        assertSame("gridToCRS", identity, trCenter);
        assertExtentEquals(grid.getExtent(), low, high);
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
        assertArrayEquals("resolution", new double[] {1, 1, 1}, grid.resolution(false), STRICT);
        assertTrue("isConversionLinear", grid.isConversionLinear(0, 1, 2));
    }

    /**
     * Tests construction with a non-linear component in the transform.
     *
     * @throws TransformException if an error occurred while using the "grid to CRS" transform.
     */
    @Test
    public void testNonLinear() throws TransformException {
        final GridExtent extent = new GridExtent(
                new int[] {0,     0, 2, 6},
                new int[] {100, 200, 3, 9}, false);
        final MathTransform horizontal = MathTransforms.linear(Matrices.create(3, 3, new double[] {
                0.5, 0,    12,
                0,   0.25, -2,
                0,   0,     1}));
        final MathTransform vertical  = MathTransforms.interpolate(null, new double[] {1, 2, 4, 10});
        final MathTransform temporal  = MathTransforms.linear(3600, 60);
        final MathTransform gridToCRS = MathTransforms.compound(horizontal, vertical, temporal);
        final GridGeometry  grid      = new GridGeometry(extent, PixelInCell.CELL_CENTER, gridToCRS, null);
        assertArrayEquals("resolution", new double[] {0.5, 0.25,        6.0, 3600}, grid.resolution(true),  STRICT);
        assertArrayEquals("resolution", new double[] {0.5, 0.25, Double.NaN, 3600}, grid.resolution(false), STRICT);
        assertFalse("isConversionLinear", grid.isConversionLinear(0, 1, 2, 3));
        assertTrue ("isConversionLinear", grid.isConversionLinear(0, 1,    3));
    }
}
