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

import java.util.Arrays;
import java.awt.image.Raster;
import java.awt.image.DataBufferInt;
import org.opengis.referencing.datum.PixelInCell;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.referencing.operation.transform.MathTransforms;

// Test dependencies
import org.junit.Test;
import static org.junit.Assert.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;


/**
 * Tests {@link BandAggregateGridCoverage}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class BandAggregateGridCoverageTest extends TestCase {
    /**
     * Width and height of images created for tests.
     */
    private static final int WIDTH = 3, HEIGHT = 2;

    /**
     * The processor to use for creating the aggregated grid coverage.
     */
    private final GridCoverageProcessor processor;

    /**
     * Creates a new test case.
     */
    public BandAggregateGridCoverageTest() {
        processor = new GridCoverageProcessor();
    }

    /**
     * Tests aggregation with two coverages having the same grid geometry.
     */
    @Test
    public void testSameGridGeometry() {
        final GridCoverage c1 = createCoverage(-2, 4, 3, -1, 100, 200);
        final GridCoverage c2 = createCoverage(-2, 4, 3, -1, 300);
        final GridCoverage cr = processor.aggregateRanges(c1, c2);
        assertEquals(c1.getGridGeometry(), cr.getGridGeometry());
        assertEquals(c2.getGridGeometry(), cr.getGridGeometry());
        assertEquals(3, cr.getSampleDimensions().size());
        assertPixelsEqual(cr, 100, 200, 300, 101, 201, 301, 102, 202, 302,
                              103, 203, 303, 104, 204, 304, 105, 205, 305);
    }

    /**
     * Tests aggregation with two coverages having a translation in their grid extents.
     * Their "grid to CRS" transforms are the same, which implies that the "real world"
     * coordinates are different. The intersection is not equal to any source extent.
     */
    @Test
    public void testDifferentExtent() {
        final GridCoverage c1 = createCoverage(-2, 4, 3, -1, 100, 200);
        final GridCoverage c2 = createCoverage(-1, 3, 3, -1, 300);
        final GridCoverage cr = processor.aggregateRanges(c1, c2);
        final GridExtent extent = cr.getGridGeometry().getExtent();
        assertNotEquals(c1.getGridGeometry().getExtent(), extent);
        assertNotEquals(c2.getGridGeometry().getExtent(), extent);
        assertEquals(extent(-1, 4, 1, 5), extent);
        assertEquals(3, cr.getSampleDimensions().size());
        assertPixelsEqual(cr, 101, 201, 303, 102, 202, 304);
    }

    /**
     * Tests aggregation with two coverages having equivalent extent but different "grid to CRS".
     * This test indirectly verifies that the {@link BandAggregateGridCoverage#gridTranslations}
     * array is correctly computed and used.
     */
    @Test
    public void testDifferentGridToCRS() {
        final GridCoverage c1 = createCoverage(-2, 4, 3, -1, 100, 200);
        final GridCoverage c2 = createCoverage(-1, 2, 2, +1, 300);
        final GridCoverage cr = processor.aggregateRanges(c1, c2);
        assertEquals   (c1.getGridGeometry(), cr.getGridGeometry());
        assertNotEquals(c2.getGridGeometry(), cr.getGridGeometry());
        assertEquals   (3, cr.getSampleDimensions().size());
        assertPixelsEqual(cr, 100, 200, 300, 101, 201, 301, 102, 202, 302,
                              103, 203, 303, 104, 204, 304, 105, 205, 305);
    }

    /**
     * Tests aggregation with two coverages having a translation in both grid extents and "grid to CRS" transforms.
     */
    @Test
    public void testDifferentExtentAndGridToCRS() {
        final GridCoverage c1 = createCoverage(-2, 4, 3, -1, 100, 200);
        final GridCoverage c2 = createCoverage( 0, 2, 2, +1, 300);
        final GridCoverage cr = processor.aggregateRanges(c1, c2);
        assertNotEquals(c1.getGridGeometry(), cr.getGridGeometry());
        assertNotEquals(c2.getGridGeometry(), cr.getGridGeometry());
        assertEquals   (3, cr.getSampleDimensions().size());
        assertPixelsEqual(cr, 101, 201, 300, 102, 202, 301,
                              104, 204, 303, 105, 205, 304);
    }

    /**
     * Tests aggregation of two coverages where one of them is itself another aggregation.
     */
    @Test
    public void testNestedAggregation() {
        final GridCoverage c1 = createCoverage(-2, 4, 3, -1, 100, 200);
        final GridCoverage c2 = createCoverage( 0, 2, 2, +1, 300);
        final GridCoverage c3 = createCoverage(-2, 4, 3, -1, 400);
        final GridCoverage cr = processor.aggregateRanges(
                                processor.aggregateRanges(c1, c2), c3);
        assertPixelsEqual(cr, 101, 201, 300, 401, 102, 202, 301, 402,
                              104, 204, 303, 404, 105, 205, 304, 405);
    }

    /**
     * Returns a two-dimensional grid extents with the given bounding box.
     * The maximal coordinates are exclusive.
     */
    private static GridExtent extent(final int minX, final int minY, final int maxX, final int maxY) {
        return new GridExtent(null, new long[] {minX, minY}, new long[] {maxX, maxY}, false);
    }

    /**
     * Creates a new grid coverage with bands starting with the given sample values.
     * The length of the {@code bandValues} array is the number of bands to create.
     * In a given band <var>b</var>, all pixels have the {@code bandValues[b]}.
     *
     * @param  minX        minimal <var>x</var> coordinate value of the grid extent.
     * @param  minY        minimal <var>y</var> coordinate value of the grid extent.
     * @param  translateX  <var>x</var> component of the "grid to CRS" translation.
     * @param  translateY  <var>y</var> component of the "grid to CRS" translation.
     * @param  bandValues  sample values for the first pixel.
     * @return a coverage with an image where all pixels have the specified sample values.
     */
    private static GridCoverage createCoverage(final int minX, final int minY,
            final int translateX, final int translateY, final int... bandValues)
    {
        final int numBands = bandValues.length;
        final var samples  = new SampleDimension[numBands];
        final var builder  = new SampleDimension.Builder();
        for (int i=0; i<numBands; i++) {
            samples[i] = builder.setName("Band with value " + bandValues[i]).build();
        }
        final int[] data = new int[WIDTH * HEIGHT * numBands];
        for (int i=0; i<data.length; i++) {
            data[i] = bandValues[i % numBands] + i / numBands;
        }
        final var values = new DataBufferInt(data, data.length);
        final var domain = new GridGeometry(extent(minX, minY, minX+WIDTH, minY+HEIGHT),
                PixelInCell.CELL_CORNER, MathTransforms.translation(translateX, translateY), HardCodedCRS.WGS84);
        return new BufferedGridCoverage(domain, Arrays.asList(samples), values);
    }

    /**
     * Asserts that all pixels from the given coverage have the expected values.
     */
    private static void assertPixelsEqual(final GridCoverage cr, final int... expected) {
        final Raster r = cr.render(null).getData();
        final int[] data = r.getPixels(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight(), (int[]) null);
        assertArrayEquals(expected, data);
    }
}
