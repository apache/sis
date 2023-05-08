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

import java.util.List;
import java.util.Arrays;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import org.opengis.referencing.datum.PixelInCell;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.util.iso.Names;
import org.junit.Test;

import static org.apache.sis.feature.Assertions.assertValuesEqual;


/**
 * Tests the {@link BufferedGridCoverage} implementation.
 * This method inherits the tests defined in {@link GridCoverage2DTest},
 * changing only the implementation class to test.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   1.0
 */
public final class BufferedGridCoverageTest extends GridCoverage2DTest {
    /**
     * Creates a {@link GridCoverage} instance to test with fixed sample values.
     * The coverage returned by this method shall contain the following values:
     *
     * <pre class="text">
     *    2    5
     *   -5  -10</pre>
     *
     * @param  grid  the grid geometry of the coverage to create.
     * @param  sd    the sample dimensions of the coverage to create.
     * @return the coverage instance to test, with above-cited values.
     */
    @Override
    GridCoverage createTestCoverage(final GridGeometry grid, final List<SampleDimension> sd) {
        /*
         * Create the grid coverage, gets its image and set values directly as short integers.
         */
        GridCoverage   coverage = new BufferedGridCoverage(grid, sd, DataBuffer.TYPE_SHORT);
        WritableRaster raster = ((BufferedImage) coverage.render(null)).getRaster();
        raster.setSample(0, 0, 0,   2);
        raster.setSample(1, 0, 0,   5);
        raster.setSample(0, 1, 0,  -5);
        raster.setSample(1, 1, 0, -10);
        return coverage;
    }

    /**
     * Tests the creation of a three-dimensional coverage.
     */
    @Test
    public void testMultidimensional() {
        final int width  = 4;
        final int height = 3;
        final int nbTime = 3;
        final GridExtent extent = new GridExtent(null, null, new long[] {width, height, nbTime}, false);
        final GridGeometry domain = new GridGeometry(extent, PixelInCell.CELL_CENTER, MathTransforms.scale(2, 3, 5), null);
        final SampleDimension band = new SampleDimension(Names.createLocalName(null, null, "Data"), null, List.of());
        /*
         * Fill slices with all values set to 10, 11 and 12 at time t=0, 1 and 2 respectively.
         * All values are stored in a single bank.
         */
        final int sliceSize = width*height;
        final int size = sliceSize*nbTime;
        final int[] buffer = new int[size];
        for (int t=0, i=0; t<nbTime; t++) {
            Arrays.fill(buffer, i, i += sliceSize, t + 10);
        }
        final DataBufferInt data = new DataBufferInt(buffer, size);
        final GridCoverage coverage = new BufferedGridCoverage(domain, List.of(band), data);
        /*
         * Verify a value in each temporal slice.
         */
        final int[] row10 = new int[width]; Arrays.fill(row10, 10);
        final int[] row11 = new int[width]; Arrays.fill(row11, 11);
        final int[] row12 = new int[width]; Arrays.fill(row12, 12);
        assertRenderEqual(coverage, null,               new long[] {width, height, 0}, new int[][] {row10, row10, row10});
        assertRenderEqual(coverage, new long[] {0,0,1}, new long[] {width, height, 1}, new int[][] {row11, row11, row11});
        assertRenderEqual(coverage, new long[] {0,0,2}, new long[] {width, height, 2}, new int[][] {row12, row12, row12});
        assertRenderEqual(coverage, null,               new long[] {width, 0, nbTime}, new int[][] {row10, row11, row12});
        assertRenderEqual(coverage, null, new long[] {0, height, nbTime}, new int[][] {
            {10, 10, 10},
            {11, 11, 11},
            {12, 12, 12}
        });
    }

    /**
     * Performs a {@link GridCoverage#render(GridExtent)} operation for the given region and
     * verifies that pixels taken from the given slice have values equal to the expected values.
     *
     * @param coverage   the coverage on which to perform the render operation.
     * @param low        lower grid coordinates, inclusive, or {@code null} for zeros.
     * @param high       high grid coordinates, inclusives.
     * @param expected   expected sample values.
     */
    private static void assertRenderEqual(final GridCoverage coverage, final long[] low, final long[] high, final int[][] expected) {
        final RenderedImage slice = coverage.render(new GridExtent(null, low, high, true));
        assertValuesEqual(slice.getTile(0,0), 0, expected);
    }
}
