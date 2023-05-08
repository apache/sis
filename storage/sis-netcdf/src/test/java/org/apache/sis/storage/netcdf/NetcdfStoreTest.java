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

import org.opengis.metadata.Metadata;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.test.TestCase;
import org.apache.sis.test.DependsOn;
import org.apache.sis.util.Version;
import org.opengis.test.dataset.TestData;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link NetcdfStore}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.3
 */
@DependsOn({
    MetadataReaderTest.class,
    NetcdfStoreProviderTest.class
})
public final class NetcdfStoreTest extends TestCase {
    /**
     * Returns a new netCDF store to test.
     *
     * @param  dataset the name of the datastore to load.
     * @throws DataStoreException if an error occurred while reading the netCDF file.
     */
    private static NetcdfStore create(final TestData dataset) throws DataStoreException {
        return new NetcdfStore(null, new StorageConnector(dataset.location()));
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
            assertSame("Should be cached.", metadata, store.getMetadata());
        }
        MetadataReaderTest.compareToExpected(metadata).assertMetadataEquals();
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
        assertEquals("major", 1, version.getMajor());
        assertEquals("minor", 4, version.getMinor());
    }
}
