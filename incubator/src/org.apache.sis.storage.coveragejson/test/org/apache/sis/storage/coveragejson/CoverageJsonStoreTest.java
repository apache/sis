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
package org.apache.sis.storage.coveragejson;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.opengis.util.FactoryException;
import org.opengis.metadata.spatial.DimensionNameType;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridCoverageBuilder;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.MemoryGridCoverageResource;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.WritableAggregate;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class CoverageJsonStoreTest {
    public CoverageJsonStoreTest() {
    }

    /**
     * Test coverage example from https://covjson.org/playground/.
     */
    @Test
    public void testReadCoverageXYZT() throws DataStoreException, FactoryException {
        try (final DataStore store = new CoverageJsonStoreProvider().open(new StorageConnector(CoverageJsonStoreTest.class.getResource("coverage_xyzt.json")))) {

            //test grid coverage resource exist
            assertTrue(store instanceof Aggregate);
            final Aggregate aggregate = (Aggregate) store;
            assertEquals(1, aggregate.components().size());
            final Resource candidate = aggregate.components().iterator().next();
            assertTrue(candidate instanceof GridCoverageResource);
            final GridCoverageResource gcr = (GridCoverageResource) candidate;

            { //test grid geometry
                final GridGeometry result = gcr.getGridGeometry();

                assertEquals(4, result.getDimension());

                final GridExtent expectedExtent = new GridExtent(new DimensionNameType[]{
                    DimensionNameType.valueOf("x"),
                    DimensionNameType.valueOf("y"),
                    DimensionNameType.valueOf("z"),
                    DimensionNameType.valueOf("t")},
                        new long[]{0,0,0,0}, new long[]{2,1,0,0}, true);
                assertEquals(expectedExtent, result.getExtent());
                assertEquals(CRS.compound(CommonCRS.WGS84.geographic3D(), CommonCRS.Temporal.JAVA.crs()), result.getCoordinateReferenceSystem());
                //TODO test transform
            }

            {   //test data
                GridCoverage coverage = gcr.read(null);
                Raster data = coverage.render(null).getData();
                assertEquals(0.5, data.getSampleDouble(0, 0, 0));
                assertEquals(0.6, data.getSampleDouble(1, 0, 0));
                assertEquals(0.4, data.getSampleDouble(2, 0, 0));
                assertEquals(0.6, data.getSampleDouble(0, 1, 0));
                assertEquals(0.2, data.getSampleDouble(1, 1, 0));
                assertEquals(Double.NaN, data.getSampleDouble(2, 1, 0));
            }
        }
    }

    /**
     * Test writing most simple 2D Grid coverage.
     */
    @Test
    public void testWriteCoverageXY() throws IOException, DataStoreException {

        final Path tempPath = Files.createTempFile("test", ".covjson");
        Files.delete(tempPath);

        try (final DataStore store = new CoverageJsonStoreProvider().open(new StorageConnector(tempPath))) {

            //test grid coverage resource exist
            assertTrue(store instanceof WritableAggregate);
            final WritableAggregate aggregate = (WritableAggregate) store;
            assertEquals(0, aggregate.components().size());

            //write a grid coverage
            final GridGeometry grid = new GridGeometry(new GridExtent(4,2), CRS.getDomainOfValidity(CommonCRS.WGS84.normalizedGeographic()), GridOrientation.REFLECTION_Y);
            final BufferedImage image = new BufferedImage(4, 2, BufferedImage.TYPE_BYTE_GRAY);
            final WritableRaster raster = image.getRaster();
            raster.setSample(0, 0, 0, 1);
            raster.setSample(1, 0, 0, 2);
            raster.setSample(2, 0, 0, 3);
            raster.setSample(3, 0, 0, 4);
            raster.setSample(0, 1, 0, 5);
            raster.setSample(1, 1, 0, 6);
            raster.setSample(2, 1, 0, 7);
            raster.setSample(3, 1, 0, 8);

            final GridCoverageBuilder gcb = new GridCoverageBuilder();
            gcb.setDomain(grid);
            gcb.setValues(image);
            final GridCoverage coverage = gcb.build();

            final GridCoverageResource gcr = new MemoryGridCoverageResource(null, coverage, null);

            aggregate.add(gcr);

            String json = Files.readString(tempPath, StandardCharsets.UTF_8);
        } finally {
            Files.deleteIfExists(tempPath);
        }
    }
}
