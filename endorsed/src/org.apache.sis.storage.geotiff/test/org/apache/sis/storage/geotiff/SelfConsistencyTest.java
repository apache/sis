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
package org.apache.sis.storage.geotiff;

import java.util.List;
import org.opengis.util.GenericName;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.StorageConnector;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertMessageContains;
import org.apache.sis.test.OptionalTestData;
import org.apache.sis.storage.test.CoverageReadConsistency;


/**
 * Test consistency of read operations in random domains.
 * Assuming that the code reading the full extent is correct, this class can detect some bugs
 * in the code reading sub-regions or applying sub-sampling. This assumption is reasonable if
 * we consider that the code reading the full extent is usually simpler than the code reading
 * a subset of data.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 */
@SuppressWarnings("exports")
public final class SelfConsistencyTest extends CoverageReadConsistency<GeoTiffStore> {
    /**
     * Opens the test file to be used for all tests.
     *
     * @throws DataStoreException if an error occurred while opening the file.
     */
    public SelfConsistencyTest() throws DataStoreException {
        super(new GeoTiffStore(null, new StorageConnector(OptionalTestData.GEOTIFF.path())));
    }

    /**
     * Work around for RFE #4093999 in Sun's bug database
     * ("Relax constraint on placement of this()/super() call in constructors").
     */
    @Override
    protected GridCoverageResource resource() throws DataStoreException {
        return store.components().iterator().next();
    }

    /**
     * Verifies that {@link GeoTiffStore#findResource(String)} returns the resource when using
     * either the full name or only its tip.
     *
     * @throws DataStoreException if an error occurred while reading the file.
     */
    @Test
    public void findResourceByName() throws DataStoreException {
        final List<GridCoverageResource> datasets = store.components();
        assertFalse(datasets.isEmpty());
        for (GridCoverageResource dataset : datasets) {
            final GenericName name = dataset.getIdentifier()
                    .orElseThrow(() -> new AssertionError("A component of the GeoTIFF datastore is unnamed"));
            GridCoverageResource foundResource = store.findResource(name.toString());
            assertSame(dataset, foundResource);
            foundResource = store.findResource(name.tip().toString());
            assertSame(dataset, foundResource);
        }
        var e = assertThrows(IllegalNameException.class, () -> store.findResource("a_wrong_namespace:1"),
                "No dataset should be returned when user specifies the wrong namespace.");
        assertMessageContains(e, "a_wrong_namespace:1");
    }
}
