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

import java.util.Map;
import java.net.URI;
import java.net.URL;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.awt.image.Raster;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.MathTransform;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.PixelInCell;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.util.Version;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.storage.DataStoreTestCase;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.test.dataset.TestData;
import static org.apache.sis.feature.Assertions.assertExtentEquals;
import static org.apache.sis.test.Assertions.assertSingleton;


/**
 * Tests {@link NetcdfStore}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @author  Quentin Bialota (Geomatys)
 */
@SuppressWarnings("exports")
public final class NetcdfStoreTest extends DataStoreTestCase {
    /**
     * Creates a new test case.
     */
    public NetcdfStoreTest() {
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
        listenToWarnings(store);
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
     * Tests the reading of a file a <abbr>CRS</abbr> declared using <abbr>GDAL</abbr>-specific attributes.
     * In this test, the "GeoTransform" attribute is inconsistent with the transform implied by netCDF dimensions
     * (the coordinate values do not match). Therefore, a warning is expected.
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

    /**
     * Tests the reading of a netCDF 4 file on a file system other than the default one.
     * The <abbr>UCAR</abbr> library cannot open such file from its path, so the bytes
     * shall be read through a Java <abbr>NIO</abbr> channel. This test uses a ZIP file
     * system as a substitute for a cloud storage such as S3.
     *
     * @throws IOException if an error occurred while creating the ZIP file.
     * @throws DataStoreException if an error occurred while reading the netCDF file.
     */
    @Test
    public void testNetcdf4FromOtherFileSystem() throws IOException, DataStoreException {
        final String filename = "netcdf4-geographic.nc";
        final Path zip = Files.createTempFile("sis-test-", ".zip");
        try {
            Files.delete(zip);          // The ZIP file system creates the file only if it does not exist.
            try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + zip.toUri()), Map.of("create", "true"))) {
                final Path file = fs.getPath(filename);
                try (InputStream in = NetcdfStoreTest.class.getResourceAsStream(filename)) {
                    assertNotNull(in, filename);
                    Files.copy(in, file);
                }
                final var provider = new NetcdfStoreProvider();
                final ProbeResult probe = provider.probeContent(new StorageConnector(file));
                assertTrue  (probe.isSupported());
                assertEquals(NetcdfStoreProvider.MIME_TYPE, probe.getMimeType());
                try (NetcdfStore store = new NetcdfStore(provider, new StorageConnector(file))) {
                    listenToWarnings(store);
                    final var r = assertInstanceOf(GridCoverageResource.class, assertSingleton(store.components()));
                    assertExtentEquals(new long[2], new long[] {3, 2}, r.getGridGeometry().getExtent());
                    /*
                     * Verify the sum of all values instead of individual values,
                     * because the row order depends on the direction of latitude axis.
                     */
                    final Raster raster = r.read(null).render(null).getData();
                    double sum = 0;
                    for (int y=0; y<raster.getHeight(); y++) {
                        for (int x=0; x<raster.getWidth(); x++) {
                            sum += raster.getSampleDouble(x, y, 0);
                        }
                    }
                    assertEquals(78, sum);
                }
            }
        } finally {
            Files.deleteIfExists(zip);
        }
        /*
         * The UCAR library emits diagnostic messages about the detection of the feature type
         * (e.g. "CoverageFactory for …"). Those messages are not the purpose of this test.
         */
        loggings.clear();
    }
}
