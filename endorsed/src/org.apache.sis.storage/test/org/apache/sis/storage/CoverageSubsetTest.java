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
package org.apache.sis.storage;

import java.util.List;
import java.awt.image.DataBufferInt;
import java.awt.image.RenderedImage;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.BufferedGridCoverage;
import org.apache.sis.coverage.grid.DimensionalityReduction;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.geometry.ImmutableEnvelope;
import org.apache.sis.measure.Units;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;
import static org.apache.sis.referencing.Assertions.assertEquivalent;


/**
 * Tests {@link CoverageSubset}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
@SuppressWarnings("exports")
public final class CoverageSubsetTest extends TestCase {
    /**
     * Size (in pixels) of the image to build for testing purpose.
     */
    private static final int WIDTH = 2, HEIGHT = 2;

    /**
     * Creates a new test case.
     */
    public CoverageSubsetTest() {
    }

    /**
     * Creates a four-dimensional resource.
     * Axes are (<var>longitude</var>, <var>latitude</var>, <var>height</var>, <var>time</var>).
     */
    private static GridCoverageResource createResource4D() {
        final var lower  = new double[] {0, 1, 2, 3};   // longitude, latitude, height, time.
        final var upper  = new double[] {4, 5, 6, 7};
        final var extent = new GridExtent(null, null, new long[] {WIDTH, HEIGHT, 1, 1}, false);
        final var region = new ImmutableEnvelope(lower, upper, HardCodedCRS.WGS84_4D);
        final var domain = new GridGeometry(extent, region, GridOrientation.HOMOTHETY);
        final var band   = new SampleDimension.Builder().addQuantitative("101-based row-major order pixel number", 101, 105, 1, 0, Units.UNITY).build();
        final var buffer = new DataBufferInt(values(), WIDTH * HEIGHT);
        return new MemoryGridCoverageResource(null, new BufferedGridCoverage(domain, List.of(band), buffer), null);
    }

    /**
     * Returns the sample values to store in the image.
     * Array length shall be {@code WIDTH * HEIGHT}.
     */
    private static int[] values() {
        return new int[] {101, 102, 103, 104};
    }

    /**
     * Tests axis selection.
     *
     * @throws DataStoreException if an error occurred while creating or reading the reduced resource.
     */
    @Test
    public void testAxisSelection() throws DataStoreException {
        final GridCoverageResource data4D = createResource4D();
        final CoverageQuery query = new CoverageQuery();

        query.setAxisSelection(DimensionalityReduction::reduce);
        verifyAxisSelection(data4D.subset(query), 2, HardCodedCRS.WGS84);

        query.setAxisSelection((gg) -> DimensionalityReduction.select(gg, 0, 1, 3));
        verifyAxisSelection(data4D.subset(query), 3, HardCodedCRS.WGS84_WITH_TIME);

        query.setAxisSelection((gg) -> DimensionalityReduction.remove(gg, 3));
        verifyAxisSelection(data4D.subset(query), 3, HardCodedCRS.WGS84_3D);
    }

    /**
     * Verifies the result of an axis selection.
     *
     * @param  reduced    the result of axis selection.
     * @param  dimension  expected number of dimensions.
     * @param  crs        expected CRS.
     * @throws DataStoreException if an error occurred while fetching information from the reduced coverage.
     */
    private static void verifyAxisSelection(final GridCoverageResource reduced,
            final int dimension, final CoordinateReferenceSystem crs) throws DataStoreException
    {
        final GridGeometry domain = reduced.getGridGeometry();
        assertEquals(dimension, domain.getDimension());
        assertEquivalent(crs, domain.getCoordinateReferenceSystem());

        final GridCoverage loaded = reduced.read(null);
        assertEquals(domain, loaded.getGridGeometry());

        final RenderedImage image = loaded.render(null);
        assertEquals(0,      image.getMinX());
        assertEquals(0,      image.getMinY());
        assertEquals(WIDTH,  image.getWidth());
        assertEquals(HEIGHT, image.getHeight());
        final int[] values = image.getData().getPixels(0, 0, WIDTH, HEIGHT, (int[]) null);
        assertArrayEquals(values(), values);
    }
}
