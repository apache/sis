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
package org.apache.sis.map.service.common;

import java.awt.Dimension;
import java.awt.image.DataBufferDouble;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.image.Interpolation;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.aggregate.CoverageAggregator;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.referencing.crs.CoordinateReferenceSystem;

// Test dependencies
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 * Tests {@link CoverageSampler}, {@link SampleTable} and {@link CoverageSlices}.
 *
 * @author Johann Sorel (Geomatys)
 */
public final class CoverageSamplerTest {
    /**
     * The CRS of the coverages and of the target grids.
     */
    private static final CoordinateReferenceSystem CRS84 = CommonCRS.WGS84.normalizedGeographic();

    /**
     * Creates a new test case.
     */
    public CoverageSamplerTest() {
    }

    /**
     * Creates a coverage of double values over the given square, with one band per array of values.
     * Values are in row-major order, the first row being at the top.
     */
    private static GridCoverage coverage(final double min, final double span, final int size, final double[]... bands) {
        final GridGeometry domain = grid(min, span, size);
        final SampleDimension[] ranges = new SampleDimension[bands.length];
        for (int i=0; i<ranges.length; i++) {
            ranges[i] = new SampleDimension.Builder().setName("b" + i).build();
        }
        return new GridCoverageBuilder().setDomain(domain).setRanges(ranges)
                .setValues(new DataBufferDouble(bands, size * size), new Dimension(size, size)).build();
    }

    /**
     * Creates a grid of the given number of cells over the given square, with <var>y</var> down.
     */
    private static GridGeometry grid(final double min, final double span, final int size) {
        return new GridGeometry(new GridExtent(size, size), new Envelope2D(CRS84, min, min, span, span), GridOrientation.REFLECTION_Y);
    }

    /**
     * Tests the sampling of a 2×2 coverage on a 4×4 grid of the same resolution, centered on the coverage.
     */
    @Test
    public void testSample() throws Exception {
        final GridCoverage coverage = coverage(0, 2, 2, new double[] {1, 2, 3, 4}, new double[] {10, 20, 30, 40});
        final CoverageSampler sampler = new CoverageSampler(grid(-1, 4, 4));
        assertEquals(16, sampler.size());
        final SampleTable table = sampler.sample(coverage, Interpolation.NEAREST);
        assertEquals(4, table.getWidth());
        assertEquals(4, table.getHeight());
        assertEquals(2, table.getNumBands());
        assertEquals(1,  table.value(0, 5));
        assertEquals(4,  table.value(0, 10));
        assertEquals(20, table.value(1, 6));
        assertTrue (table.isInside(5));
        assertFalse(table.isEmpty(5));
        assertFalse(table.isInside(0), "Cell outside the coverage.");
        assertTrue (table.isEmpty(0));
        assertTrue (Double.isNaN(table.value(0, 15)));
        assertFalse(table.isByte(0));

        final SampleTable selected = sampler.sample(coverage, Interpolation.NEAREST, new int[] {1});
        assertEquals(1, selected.getNumBands());
        final double[] band = selected.band(0);
        assertEquals(30, band[9]);
        band[9] = 0;
        assertEquals(30, selected.value(0, 9), "The band shall be a copy.");
    }

    /**
     * Tests the slices of a coverage with a time dimension of 3 slices with the values 10, 0 and 5.
     */
    @Test
    public void testSlices() throws Exception {
        final CoverageAggregator aggregator = new CoverageAggregator();
        final double[] values = {10, 0, 5};
        final Instant start = Instant.parse("2020-01-01T00:00:00Z");
        for (int i=0; i<values.length; i++) {
            aggregator.add(coverage(-50, 100, 1, new double[] {values[i]}), start.plus(Duration.ofDays(i)), Duration.ofDays(1));
        }
        final GridCoverage series = ((GridCoverageResource) aggregator.build(null)).read(null);
        final CoverageSlices slices = new CoverageSlices(series);
        final CoverageSampler sampler = new CoverageSampler(grid(-50, 100, 2));
        assertEquals(0, sampler.sample(slices.middle(), Interpolation.NEAREST).value(0, 3), "Middle slice.");

        final List<GridCoverage> all = slices.along(List.of("TIME"));
        assertEquals(3, all.size());
        for (int i=0; i<values.length; i++) {
            assertEquals(values[i], sampler.sample(all.get(i), Interpolation.NEAREST).value(0, 0));
        }
        assertEquals(1, slices.along(List.of("depth")).size(), "Unknown dimension.");
    }
}
