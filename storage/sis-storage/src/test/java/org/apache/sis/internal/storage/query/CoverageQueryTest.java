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
package org.apache.sis.internal.storage.query;

import java.awt.image.RenderedImage;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverage2D;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.referencing.crs.HardCodedCRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link CoverageQuery} and (indirectly) {@link CoverageSubset}.
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 */
public final strictfp class CoverageQueryTest extends TestCase {
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
        resource = new MockGridResource(grid);
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
        verifyRead(subset);
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
        query.setDomain(subGrid);

        final GridCoverageResource subset = resource.subset(query);
        assertEquals(subGrid, subset.getGridGeometry());
        verifyRead(subset);
    }

    /**
     * Tests with a sub-grid geometry and a source domain expansion.
     *
     * @throws DataStoreException if query execution failed.
     */
    @Test
    public void testWithExpansion() throws DataStoreException {
        final GridGeometry subGrid = createSubGrid(0);
        final CoverageQuery query = new CoverageQuery();
        query.setDomain(subGrid);
        query.setSourceDomainExpansion(3);

        final GridCoverageResource subset = resource.subset(query);
        assertEquals(createSubGrid(3), subset.getGridGeometry());
        verifyRead(subset);

        /*
        Current implementation returns the full image
        but still, the expansion should be visible in the image offset
        */
        GridCoverage coverage = subset.read(null);
        RenderedImage image = coverage.render(null);
        System.out.println(image);
    }

    /**
     * Verifies that the read operation adds the expected margins.
     * This is an anti-regression test; in current implementation,
     * {@link GridCoverage2D} returns a larger area then requested.
     */
    private void verifyRead(final GridCoverageResource subset) throws DataStoreException {

        { //must be the same as subset grid geometry
            final GridCoverage coverage = subset.read(null);
            assertEquals(subset.getGridGeometry(), coverage.getGridGeometry());
        }

        {
            final GridGeometry request  = createSubGrid(-4);
            final GridCoverage coverage = subset.read(request);
            assertEquals(request, coverage.getGridGeometry());
        }
    }
}
