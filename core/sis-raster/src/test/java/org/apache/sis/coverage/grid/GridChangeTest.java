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
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.ReferencingAssert.*;
import static org.apache.sis.coverage.grid.GridExtentTest.assertExtentEquals;


/**
 * Tests {@link GridChange}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
@DependsOn({GridExtentTest.class, GridGeometryTest.class})
public final strictfp class GridChangeTest extends TestCase {
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
    public void testLinear2D() throws TransformException {
        GridGeometry source = grid(  10,   -20,  110,  180, 100, -300);     // Envelope x: [1200 … 11300]   y: [-53800 … 6500]
        GridGeometry target = grid(2000, -1000, 9000, 8000,   2,   -1);     // Envelope x: [4200 … 18202]   y: [ -7501 … 1500]
        GridChange   change = new GridChange(source, target);               // Envelope x: [4200 … 11300]   y: [ -7501 … 1500]
        GridExtent   extent = change.getTargetExtent();
        assertExtentEquals(extent, 0,  2000, 5549);                         // Subrange of target extent.
        assertExtentEquals(extent, 1, -1000, 8000);
        assertArrayEquals("subsamplings", new int[] {50, 300}, change.getTargetSubsamplings());  // s = scaleSource / scaleTarget
        /*
         * Scale factors in following matrix shall be the same than above sub-samplings.
         * Translation appears only with PixelInCell different than the one used at construction.
         */
        Matrix3 c = new Matrix3();
        c.m00 =  50;
        c.m11 = 300;
        assertMatrixEquals("CELL_CORNER", c, MathTransforms.getMatrix(change.getConversion(PixelInCell.CELL_CORNER)), STRICT);
        c.m02 =  24.5;
        c.m12 = 149.5;
        assertMatrixEquals("CELL_CENTER", c, MathTransforms.getMatrix(change.getConversion(PixelInCell.CELL_CENTER)), STRICT);
        /*
         * If we do not ask for sub-samplings, the 'gridToCRS' transforms shall be the same than the 'target' geometry.
         * The envelope is the intersection of the envelopes of 'source' and 'target' geometries, documented above.
         */
        GridGeometry tg = change.getTargetGeometry();
        assertSame("extent",      extent, tg.getExtent());
        assertSame("CELL_CORNER", target.getGridToCRS(PixelInCell.CELL_CORNER), tg.getGridToCRS(PixelInCell.CELL_CORNER));
        assertSame("CELL_CENTER", target.getGridToCRS(PixelInCell.CELL_CENTER), tg.getGridToCRS(PixelInCell.CELL_CENTER));
        GeneralEnvelope expected = new GeneralEnvelope(2);
        expected.setRange(0,  4200, 11300);
        expected.setRange(1, -7501,  1500);
        assertEnvelopeEquals(expected, tg.getEnvelope(), STRICT);
        /*
         * If we ask for sub-samplings, then the envelope should be approximately the same or smaller. Note that without
         * the clipping documented in GridExtent(GridExtent, int...) constructor, the envelope could be larger.
         */
        tg = change.getTargetGeometry(50, 300);
        assertEnvelopeEquals(expected, tg.getEnvelope(), STRICT);
        assertMatrixEquals("gridToCRS", new Matrix3(
                100,    0, 200,
                  0, -300, 600,
                  0,    0,   1), MathTransforms.getMatrix(tg.getGridToCRS(PixelInCell.CELL_CORNER)), STRICT);
    }
}
