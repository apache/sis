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

import java.time.Instant;
import java.time.Duration;
import java.awt.image.BufferedImage;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.matrix.Matrix4;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.ArraysExt;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;
import static org.apache.sis.feature.Assertions.assertGridToCornerEquals;


/**
 * Tests {@link DimensionAppender}. This is partially the converse of {@link DimensionalityReductionTest}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class DimensionAppenderTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DimensionAppenderTest() {
    }

    /**
     * Returns a grid coverage to use as a starting point.
     *
     * @param  width   image width in pixels.
     * @param  height  image height in pixels.
     */
    private static GridCoverage initial(final int width, final int height) {
        var extent = new GridExtent(width, height);
        var gridToCRS = new Matrix3(
                4, 0, 100,
                0, 3, -20,
                0, 0,   1);

        var gg = new GridGeometry(extent, PixelInCell.CELL_CORNER, MathTransforms.linear(gridToCRS), HardCodedCRS.WGS84);
        return new GridCoverage2D(gg, null, new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY));
    }

    /**
     * Asserts that the grid geometry of the given coverage has the expected properties.
     *
     * @param actual        the coverage for which to verify the grid geometry.
     * @param gridToCRS     expected "grid to CRS" transform as a matrix.
     * @param gridIndices   expected lower grid coordinate values.
     */
    private static void assertGridGeometryEquals(final GridCoverage actual, final Matrix gridToCRS, final long... gridIndices) {
        final GridGeometry gg = actual.getGridGeometry();
        assertGridToCornerEquals(gridToCRS, gg);
        assertArrayEquals(gridIndices, gg.getExtent().getLow().getCoordinateValues());
    }

    /**
     * Verifies that the conversion of lower grid coordinates to CRS produces the expected values.
     *
     * @param actual     the coverage for which to verify the coordinate conversion.
     * @param  expected  expected coordinates in units of the CRS.
     * @throws TransformException if the conversion failed.
     */
    private static void verifyTransformLower(final GridCoverage actual, final double... expected) throws TransformException {
        final GridGeometry gg = actual.getGridGeometry();
        final MathTransform gridToCRS = gg.getGridToCRS(PixelInCell.CELL_CORNER);
        final double[] coordinates = ArraysExt.copyAsDoubles(gg.getExtent().getLow().getCoordinateValues());
        gridToCRS.transform(coordinates, 0, coordinates, 0, 1);
        assertArrayEquals(expected, coordinates);
    }

    /**
     * Tests the {@link GridCoverageProcessor} convenience methods.
     *
     * @throws TransformException if the coordinate conversion failed.
     */
    @Test
    public void testUsingProcessor() throws TransformException {
        final var coverage2D = initial(16, 8);
        final var processor  = new GridCoverageProcessor();
        final var coverage3D = processor.appendDimension(coverage2D, 260, 5, HardCodedCRS.GRAVITY_RELATED_HEIGHT);
        assertSame(coverage2D, processor.selectGridDimensions(coverage3D, 0, 1));
        verifyTransformLower(coverage3D, 100, -20, 260);
        assertGridGeometryEquals(coverage3D, new Matrix4(
                4, 0, 0, 100,
                0, 3, 0, -20,
                0, 0, 5,   0,
                0, 0, 0,   1), 0, 0, 52);

        final var coverage4D = processor.appendDimension(coverage3D, Instant.parse("2022-06-18T00:00:00Z"), Duration.parse("P21D"));
        assertSame(coverage2D, processor.selectGridDimensions(coverage3D, 0, 1));
        verifyTransformLower(coverage4D, 100, -20, 260, 19748);
        assertGridGeometryEquals(coverage4D, Matrices.create(5, 5, new double[] {
                4, 0, 0,  0, 100,
                0, 3, 0,  0, -20,
                0, 0, 5,  0,   0,
                0, 0, 0, 21,   8,
                0, 0, 0, 0,    1}), 0, 0, 52, 940);

        // Easy way to check that the correct dimensions were selected.
        verifyTransformLower(processor.selectGridDimensions(coverage4D, 0, 1, 2), 100, -20, 260);
        verifyTransformLower(processor.selectGridDimensions(coverage4D, 0, 1, 3), 100, -20, 19748);
    }
}
