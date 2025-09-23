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

import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.referencing.internal.shared.AffineTransform2D;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.TestCase;
import org.apache.sis.referencing.crs.HardCodedCRS;


/**
 * Tests {@link CoverageQuery} and (indirectly) {@link CoverageSubset}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public final class CoverageQueryTest extends TestCase {
    /**
     * The coordinate reference system used by the tests.
     */
    private final CoordinateReferenceSystem crs;

    /**
     * The conversion from pixel coordinates to geographic coordinates for the coverage in this test.
     */
    private final AffineTransform2D gridToCRS;

    /**
     * The resource to be tested by each test method.
     */
    private final GridCoverageResource resource;

    /**
     * Creates a new test case.
     */
    public CoverageQueryTest() {
        crs = HardCodedCRS.WGS84;
        gridToCRS = new AffineTransform2D(2, 0, 0, 3, 0, 0);
        final int width  = 32;
        final int height = 37;
        final GridGeometry grid = new GridGeometry(new GridExtent(width, height), PixelInCell.CELL_CENTER, gridToCRS, crs);
        resource = new GridResourceMock(grid);
    }

    /**
     * Tests without any parameter.
     *
     * @throws DataStoreException if query execution failed.
     */
    @Test
    public void testNoParameters() throws DataStoreException {
        final CoverageQuery query = new CoverageQuery();
        final GridCoverageResource subset = resource.subset(query);
        assertSame(resource.getGridGeometry(), subset.getGridGeometry());
        verifyRead(subset, 0);
    }

    /**
     * Creates an arbitrary grid geometry included inside the {@linkplain #resource} extent.
     */
    private GridGeometry createSubGrid(final int expansion) {
        final GridExtent extent = new GridExtent(
                new DimensionNameType[] {DimensionNameType.COLUMN, DimensionNameType.ROW},
                new long[] { 7 - expansion, 19 - expansion},
                new long[] {21 + expansion, 27 + expansion}, true);

        return new GridGeometry(extent, PixelInCell.CELL_CENTER, gridToCRS, crs);
    }

    /**
     * Tests with a sub-grid geometry.
     *
     * @throws DataStoreException if query execution failed.
     */
    @Test
    public void testWithSubgrid() throws DataStoreException {
        final GridGeometry subGrid = createSubGrid(0);
        final CoverageQuery query = new CoverageQuery();
        query.setSelection(subGrid);

        final GridCoverageResource subset = resource.subset(query);
        assertEquals(subGrid, subset.getGridGeometry());
        verifyRead(subset, 0);
    }

    /**
     * Tests with a sub-grid geometry and a source domain expansion.
     *
     * @throws DataStoreException if query execution failed.
     */
    @Test
    public void testWithExpansion() throws DataStoreException {
        final int expansion = 3;
        final GridGeometry subGrid = createSubGrid(0);
        final CoverageQuery query = new CoverageQuery();
        query.setSelection(subGrid);
        query.setSourceDomainExpansion(expansion);

        final GridCoverageResource subset = resource.subset(query);
        assertEquals(createSubGrid(expansion), subset.getGridGeometry());
        verifyRead(subset, expansion);
    }

    /**
     * Tests using only the methods defined in the {@link Query} base class.
     *
     * @throws DataStoreException if query execution failed.
     */
    @Test
    public void testQueryMethods() throws DataStoreException {
        final GridGeometry subGrid = createSubGrid(0);
        final Query query = new CoverageQuery();
        query.setSelection(subGrid.getEnvelope());
        query.setProjection("0");

        final GridCoverageResource subset = resource.subset(query);
        assertEquals(subGrid, subset.getGridGeometry());
        verifyRead(subset, 0);
    }

    /**
     * Tests using an invalid sample dimension name.
     *
     * @throws DataStoreException if query execution failed.
     */
    @Test
    public void testInvalidName() throws DataStoreException {
        final Query query = new CoverageQuery();
        query.setProjection("Apple");
        var e = assertThrows(UnsupportedQueryException.class, () -> resource.subset(query));
        assertMessageContains(e, "Apple");
    }

    /**
     * Verifies that the read operation adds the expected margins.
     */
    private void verifyRead(final GridCoverageResource subset, final int expansion) throws DataStoreException {
        /*
         * Test reading the whole image. The grid geometry of returned coverage
         * must be the same as the grid geometry of the GridCoverageResource,
         * which has been verified by the caller to contain the expansion.
         */
        assertEquals(subset.getGridGeometry(), subset.read(null).getGridGeometry());
        /*
         * Request for a smaller area and verify that the request has the expected size,
         * including expansion.
         */
        GridGeometry request = createSubGrid(-4);
        final GridCoverage coverage = subset.read(request);
        if (expansion != 0) {
            request = createSubGrid(-4 + expansion);
        }
        assertEquals(request, coverage.getGridGeometry());
    }
}
