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
package org.apache.sis.storage.netcdf;

import java.net.URL;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.CRS;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.system.Loggers;
import org.apache.sis.util.Version;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.feature.Assertions.assertExtentEquals;
import org.apache.sis.storage.DataStoreTestCase;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.test.dataset.TestData;


/**
 * Tests {@link NetcdfStore}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
@SuppressWarnings("exports")
public final class NetcdfStoreTest extends DataStoreTestCase {
    /**
     * Creates a new test case.
     */
    public NetcdfStoreTest() {
        super(Loggers.CRS_FACTORY);
    }

    /**
     * Returns a new netCDF store opened on a dataset specified by an enumeration value.
     *
     * @param  dataset  the name of the dataset to open.
     * @return netCDF data store opened for the given dataset.
     * @throws DataStoreException if an error occurred while reading the netCDF file.
     */
    private NetcdfStore create(final TestData dataset) throws DataStoreException {
        return create(dataset.name(), dataset.location());
    }

    /**
     * Returns a new netCDF store opened on a dataset specified by a resource name.
     *
     * @param  dataset  the name of a file in the same directory as this test class.
     * @throws DataStoreException if an error occurred while reading the netCDF file.
     */
    private NetcdfStore create(final String dataset) throws DataStoreException {
        return create(dataset, NetcdfStoreTest.class.getResource(dataset));
    }

    /**
     * Returns a new netCDF store to test from the given <abbr>URL</abbr>.
     *
     * @param  name     the name of the dataset to open, for assertion message.
     * @param  dataset  the <abbr>URL</abbr> of the dataset to open.
     * @return netCDF data store opened for the given dataset.
     * @throws DataStoreException if an error occurred while reading the netCDF file.
     */
    private NetcdfStore create(final String name, final URL dataset) throws DataStoreException {
        assertNotNull(dataset, name);
        final var store = new NetcdfStore(null, new StorageConnector(dataset));
        listen(store);
        return store;
    }

    /**
     * Tests {@link NetcdfStore#getMetadata()}.
     *
     * @throws DataStoreException if an error occurred while reading the netCDF file.
     */
    @Test
    public void testGetMetadata() throws DataStoreException {
        final Metadata metadata;
        try (NetcdfStore store = create(TestData.NETCDF_2D_GEOGRAPHIC)) {
            metadata = store.getMetadata();
            assertSame(metadata, store.getMetadata(), "Should be cached.");
        }
        MetadataReaderTest.compareToExpected(metadata, false).assertMetadataEquals();
        loggings.skipNextLogIfContains("EPSG:4019");        // Deprecated EPSG code.
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests {@link NetcdfStore#getConventionVersion()}.
     *
     * @throws DataStoreException if an error occurred while reading the netCDF file.
     */
    @Test
    public void testGetConventionVersion() throws DataStoreException {
        final Version version;
        try (NetcdfStore store = create(TestData.NETCDF_2D_GEOGRAPHIC)) {
            version = store.getConventionVersion();
        }
        assertEquals(1, version.getMajor());
        assertEquals(4, version.getMinor());
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Tests the reading of a file a <abbr>CRS</abbr> declared using <abbr>GDAL</abbr>-specific properties.
     *
     * @throws DataStoreException if an error occurred while reading the netCDF file.
     */
    @Test
    public void testFileWithGDALEncoding() throws DataStoreException {
        try (NetcdfStore store = create("transverse-mercator-wrong-geotransform.nc")) {
            final var r = assertInstanceOf(GridCoverageResource.class, store.findResource("evi"));
            final GridGeometry gg = r.getGridGeometry();
            assertEquals(3, gg.getDimension());

            final GridExtent extent = gg.getExtent();
            assertExtentEquals(new long[3], new long[] {2, 3, 2}, extent);

            final MathTransform gridToCRS = gg.getGridToCRS(PixelInCell.CELL_CORNER);
            assertEquals(3, gridToCRS.getSourceDimensions());
            assertEquals(3, gridToCRS.getTargetDimensions());
            assertFalse(gridToCRS instanceof LinearTransform);

            final CoordinateReferenceSystem crs = gg.getCoordinateReferenceSystem();
            assertInstanceOf(ProjectedCRS.class, CRS.getHorizontalComponent(crs));
            assertNotNull(CRS.getTemporalComponent(crs));
        }
        // The reader should have emitted a warning about mismatched `GeoTransform` values.
        loggings.assertNextLogContains("GeoTransform", "evi", "transverse-mercator-wrong-geotransform.nc");
        loggings.assertNoUnexpectedLog();
    }
}
