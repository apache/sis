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

import java.awt.image.BufferedImage;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverage2D;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.internal.storage.MemoryGridResource;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.test.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;

/**
 *
 * @author  Johann Sorel (Geomatys)
 * @version 1.1
 * @since   1.1
 */
public final strictfp class CoverageQueryTest extends TestCase {

    /**
     * Verify source domain expansion parameter is used.
     */
    @Test
    public void sourceExpansionTest() throws DataStoreException {

        final CoordinateReferenceSystem crs = CommonCRS.WGS84.normalizedGeographic();
        final AffineTransform2D gridToCrs = new AffineTransform2D(1, 0, 0, 1, 0, 0);
        final GridCoverageResource resource;
        { //create resource
            final GridGeometry grid = new GridGeometry(new GridExtent(100, 100), PixelInCell.CELL_CENTER, gridToCrs, crs);
            final BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);

            final GridCoverageBuilder gcb = new GridCoverageBuilder();
            gcb.setDomain(grid);
            gcb.setValues(image);
            final GridCoverage coverage = gcb.build();
            resource = new MemoryGridResource(null, (GridCoverage2D) coverage);
        }

        { // test without any parameter
            final CoverageQuery query = new CoverageQuery();
            final GridCoverageResource subres = resource.subset(query);
            Assert.assertEquals(resource.getGridGeometry(), subres.getGridGeometry());
        }

        { // test with sub grid geometry
            final GridGeometry subGrid = new GridGeometry(new GridExtent(
                    new DimensionNameType[]{DimensionNameType.COLUMN, DimensionNameType.ROW},
                    new long[]{20, 20}, new long[]{40, 40}, true),
                    PixelInCell.CELL_CENTER, gridToCrs, crs);

            final CoverageQuery query = new CoverageQuery();
            query.setDomain(subGrid);

            final GridCoverageResource subres = resource.subset(query);
            Assert.assertEquals(subGrid, subres.getGridGeometry());
        }

        { // test with sub grid geometry + expansion
            final GridGeometry subGrid = new GridGeometry(new GridExtent(
                    new DimensionNameType[]{DimensionNameType.COLUMN, DimensionNameType.ROW},
                    new long[]{20, 20}, new long[]{40, 40}, true),
                    PixelInCell.CELL_CENTER, gridToCrs, crs);

            final CoverageQuery query = new CoverageQuery();
            query.setDomain(subGrid);
            query.setSourceDomainExpansion(3);

            final GridCoverageResource subres = resource.subset(query);

            final GridGeometry expGrid = new GridGeometry(new GridExtent(
                    new DimensionNameType[]{DimensionNameType.COLUMN, DimensionNameType.ROW},
                    new long[]{17, 17}, new long[]{43, 43}, true),
                    PixelInCell.CELL_CENTER, gridToCrs, crs);

            Assert.assertEquals(expGrid, subres.getGridGeometry());


            //read operation must add the expected margins
            //this last test is only to avoid regression
            //GridCoverage2D implementation returns a larger area then requested
            final GridGeometry readGrid = new GridGeometry(new GridExtent(
                    new DimensionNameType[]{DimensionNameType.COLUMN, DimensionNameType.ROW},
                    new long[]{30, 30}, new long[]{35, 35}, true),
                    PixelInCell.CELL_CENTER, gridToCrs, crs);
            GridCoverage coverage = subres.read(readGrid);

            final GridGeometry expReadGrid = resource.getGridGeometry();

            Assert.assertEquals(expReadGrid, coverage.getGridGeometry());

        }
    }

}
