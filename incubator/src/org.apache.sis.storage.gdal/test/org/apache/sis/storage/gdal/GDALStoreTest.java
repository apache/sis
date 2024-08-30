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
package org.apache.sis.storage.gdal;

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import org.opengis.util.GenericName;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.util.Utilities;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.privy.AffineTransform2D;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.StorageConnector;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests {@link GDALStore}.
 *
 * @author Johann Sorel (Geomatys)
 * @author Quentin BIALOTA (Geomatys)
 */
public final class GDALStoreTest {
    /**
     * Creates a new test case.
     */
    public GDALStoreTest() {
    }

    /**
     * Verifies that the provider is registered.
     */
    @Test
    public void testProviderRegistration() {
        for (DataStoreProvider provider : DataStores.providers()) {
            if (provider instanceof GDALStoreProvider) {
                return;
            }
        }
        fail("Provider not found.");
    }

    /**
     * Tests reading an indexed image. The test uses a small image with 1 band and indexed color palette.
     *
     * @throws Exception if any error occurred.
     */
    @Test
    public void readIndexedImage() throws Exception {
        boolean foundGrid = false;
        boolean foundBand = false;
        final var input = new StorageConnector(GDALStoreTest.class.getResource("test.tiff"));
        try (GDALStore store = new GDALStore(new GDALStoreProvider(), input)) {
            for (final Resource r : store.components()) {
                assertFalse(foundGrid);
                foundGrid = true;

                final GenericName name = r.getIdentifier().orElseThrow();
                assertEquals("test.tiff:Image #1", name.toString());
                assertSame(r, store.findResource(name.toString()));

                // Test reading fully the coverage.
                final var gr = assertInstanceOf(GridCoverageResource.class, r);
                final GridCoverage coverage = gr.read(null);

                // Check the CRS.
                CoordinateReferenceSystem expectedCRS;
                try {
                    expectedCRS = CRS.forCode("EPSG:32617");
                } catch (NoSuchAuthorityCodeException e) {
                    // Happen if the EPSG factory is not available.
                    expectedCRS = null;
                }
                final CoordinateReferenceSystem actual = coverage.getCoordinateReferenceSystem();
                assertNotNull(actual);
                if (expectedCRS != null) {
                    assertTrue(Utilities.equalsIgnoreMetadata(expectedCRS, actual));
                }

                // Check the grid geometry.
                final GridGeometry gridGeometry = coverage.getGridGeometry();
                final GridExtent extent = gridGeometry.getExtent();
                assertEquals(  2, extent.getDimension());
                assertEquals(120, extent.getSize(0));
                assertEquals( 75, extent.getSize(1));
                final var gridToCrs = assertInstanceOf(AffineTransform2D.class, gridGeometry.getGridToCRS(PixelInCell.CELL_CORNER));
                assertEquals( 664769.191709,    gridToCrs.getTranslateX(), 1E-9);
                assertEquals(4600950.488848333, gridToCrs.getTranslateY(), 1E-9);
                assertEquals(    839.977999999, gridToCrs.getScaleX(),     1E-9);
                assertEquals(   -846.395733333, gridToCrs.getScaleY(),     1E-9);
                assertEquals(      0.0,         gridToCrs.getShearX());
                assertEquals(      0.0,         gridToCrs.getShearY());

                // Check the sample dimension.
                for (final SampleDimension dimension : coverage.getSampleDimensions()) {
                    assertFalse(foundBand);
                    foundBand = true;

                    assertTrue(dimension.getTransferFunction().isEmpty());
                    assertTrue(dimension.getUnits().isEmpty());

                    // Check image.
                    final RenderedImage img = coverage.render(null);
                    assertEquals(120, img.getWidth());
                    assertEquals( 75, img.getHeight());
                    assertTrue(img.getSampleModel().getNumBands() == 1);

                    final DataBuffer data = img.getData().getDataBuffer();
                    assertEquals(DataBuffer.TYPE_BYTE, data.getDataType());
                    assertEquals(9000, data.getSize());
                    assertEquals(5, data.getElem(2000));    // Check a random value.
                }
            }
        }
        assertTrue(foundGrid);
        assertTrue(foundBand);
    }
}
