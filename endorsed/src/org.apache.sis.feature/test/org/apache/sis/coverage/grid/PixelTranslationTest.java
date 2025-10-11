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

import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.matrix.Matrix4;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.referencing.Assertions.assertMatrixEquals;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link PixelTranslation}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
@SuppressWarnings("exports")
public final class PixelTranslationTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public PixelTranslationTest() {
    }

    /**
     * Returns a transform from center to corner with the given number of dimensions.
     */
    private static MathTransform centerToCorner(final int dimension) {
        return PixelTranslation.translate(MathTransforms.identity(dimension), PixelInCell.CELL_CENTER, PixelInCell.CELL_CORNER);
    }

    /**
     * Returns a transform from center to corner in the two-dimensional case.
     */
    private static MathTransform centerToCorner2D() {
        return PixelTranslation.translate(MathTransforms.identity(2), PixelOrientation.CENTER, PixelOrientation.UPPER_LEFT, 0, 1);
    }

    /**
     * Tests {@link PixelTranslation#translate(MathTransform, PixelInCell, PixelInCell)} with an identity transform.
     * If grid coordinates (0,0) in "pixel center" convention map to (0,0) in "real world" coordinates,
     * then grid coordinates (0,0) in "pixel corner" convention shall map to (-½, -½) in real world.
     * That way, grid coordinates (½,½) in "pixel corner" convention still map to (0,0) in real world.
     *
     * @throws TransformException if an error occurred while transforming a test point.
     */
    @Test
    public void testTranslatePixelInCell() throws TransformException {
        final MathTransform gridToCRS = centerToCorner(3);
        assertMatrixEquals(new Matrix4(1, 0, 0, -0.5,
                                       0, 1, 0, -0.5,
                                       0, 0, 1, -0.5,
                                       0, 0, 0,  1), gridToCRS, "center → corner");
        /*
         * Just for making clear what we explained in javadoc comment: the real world (0,0,0) coordinates was in the center
         * of cell (0,0,0). After we switched to "cell corner" convention, that center is (½,½,½) in grid coordinates but
         * should still map (0,0,0) in "real world" coordinates.
         */
        final double[] coordinates = new double[] {0.5, 0.5, 0.5};
        gridToCRS.transform(coordinates, 0, coordinates, 0, 1);
        assertArrayEquals(new double[3], coordinates);
    }

    /**
     * Tests {@link PixelTranslation#translate(MathTransform, PixelOrientation, PixelOrientation, int, int)}.
     * See {@link #testTranslatePixelInCell()} for discussion on expected values.
     */
    @Test
    public void testTranslatePixelOrientation() {
        MathTransform gridToCRS = centerToCorner2D();
        assertMatrixEquals(new Matrix3(1, 0, -0.5,
                                       0, 1, -0.5,
                                       0, 0,  1), gridToCRS, "center → corner");

        gridToCRS = PixelTranslation.translate(MathTransforms.identity(3), PixelOrientation.LOWER_LEFT, PixelOrientation.CENTER, 1, 2);
        assertMatrixEquals(new Matrix4(1, 0, 0,  0.0,
                                       0, 1, 0, +0.5,
                                       0, 0, 1, -0.5,
                                       0, 0, 0,  1), gridToCRS, "corner → center");
    }

    /**
     * Verifies consistency of cached transforms when using translations inferred from {@link PixelInCell}.
     */
    @Test
    public void testCache() {
        MathTransform previous = null;
        for (int dimension = 1; dimension <= 5; dimension++) {
            final MathTransform mt = centerToCorner(dimension);
            assertNotSame(previous, mt);                    // Transforms with different number of dimensions.
            assertSame(mt, centerToCorner(dimension));      // Transforms with same number of dimensions.
            previous = mt;
        }
    }

    /**
     * Verifies consistency of cached transforms when using translations inferred from {@link PixelOrientation}.
     */
    @Test
    public void testCache2D() {
        assertSame(centerToCorner(2), centerToCorner2D());
        assertSame(PixelTranslation.translate(MathTransforms.identity(2), PixelInCell.CELL_CORNER, PixelInCell.CELL_CENTER),
                   PixelTranslation.translate(MathTransforms.identity(2), PixelOrientation.UPPER_LEFT, PixelOrientation.CENTER, 0, 1));
    }
}
