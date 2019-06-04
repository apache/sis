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
package org.apache.sis.internal.coverage;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.test.TestCase;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform1D;
import org.junit.Assert;
import org.junit.Test;


/**
 * Tests the {@link BufferedGridCoverage} implementation.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class BufferedGridCoverageTest extends TestCase {
    /**
     * Tests with a two-dimensional coverage.
     */
    @Test
    public void testCoverage2D() {
        /*
         * Create coverage of 2×2 pixels with an identity "grid to CRS" transform.
         * The range of sample values will be [-10 … +10]°C.
         */
        final GridGeometry grid = new GridGeometry(new GridExtent(2, 2),
                PixelInCell.CELL_CENTER, MathTransforms.identity(2), HardCodedCRS.WGS84);

        final MathTransform1D toUnits = (MathTransform1D) MathTransforms.linear(0.5, 100);
        final SampleDimension sd = new SampleDimension.Builder().setName("t")
                .addQuantitative("data", NumberRange.create(-10, true, 10, true), toUnits, Units.CELSIUS)
                .build();
        /*
         * Create the grid coverage, gets its image and set values directly as short integers.
         */
        GridCoverage   coverage = new BufferedGridCoverage(grid, Arrays.asList(sd), DataBuffer.TYPE_SHORT);
        WritableRaster raster = ((BufferedImage) coverage.render(null)).getRaster();
        raster.setSample(0, 0, 0,   0);
        raster.setSample(1, 0, 0,   5);
        raster.setSample(0, 1, 0,  -5);
        raster.setSample(1, 1, 0, -10);
        /*
         * Verify packed values.
         */
        assertSamplesEqual(coverage, new double[][] {
            { 0,   5},
            {-5, -10}
        });
        /*
         * Verify converted values.
         */
        coverage = coverage.forConvertedValues(true);
        assertSamplesEqual(coverage, new double[][] {
            {100.0, 102.5},
            { 97.5,  95.0}
        });
        /*
         * Test writing converted values and verify the result in the packed coverage.
         * For example for the sample value at (0,0), we have (x is the packed value):
         *
         *   70 = x * 0.5 + 100   →   (70-100)/0.5 = x   →   x = -60
         */
        raster = ((BufferedImage) coverage.render(null)).getRaster();
        raster.setSample(0, 0, 0,  70);
        raster.setSample(1, 0, 0,   2.5);
        raster.setSample(0, 1, 0,  -8);
        raster.setSample(1, 1, 0, -90);
        assertSamplesEqual(coverage.forConvertedValues(false), new double[][] {
            { -60, -195},
            {-216, -380}
        });
    }

    /**
     * assert that the sample values in the given coverage are equal to the expected values.
     */
    private static void assertSamplesEqual(final GridCoverage coverage, final double[][] expected) {
        final Raster raster = coverage.render(null).getData();
        for (int y=0; y<expected.length; y++) {
            for (int x=0; x<expected[y].length; x++) {
                double value = raster.getSampleDouble(x, y, 0);
                Assert.assertEquals(expected[y][x], value, STRICT);
            }
        }
    }
}
