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
package org.apache.sis.storage.folder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.metadata.identification.Identification;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCaseWithLogs;


/**
 * Tests folder {@link Store}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class StoreTest extends TestCaseWithLogs {
    /**
     * Creates a new test case.
     */
    public StoreTest() {
        super("ucar.nc2.NetcdfFile");
    }

    /**
     * Gets the path to the test directory. If the directory is not accessible through the file system
     * (for example if the test data are read from a JAR file), then skip the tests. This happen if the
     * test are executed from another Maven module than {@code org.apache.sis.storage}.
     */
    private static Path testDirectory() throws URISyntaxException {
        final URL sample = StoreTest.class.getResource("test-data/README.txt");
        assertNotNull(sample, "Test data not found");
        return Path.of(sample.toURI()).getParent();
    }

    /**
     * Verifies that components in a folder are correctly detected.
     *
     * @throws URISyntaxException if the URL to test data cannot be converted to a path of the file system.
     * @throws DataStoreException if an error occurred while reading the resources.
     * @throws IOException if an I/O error occurs.
     */
    @Test
    public void testComponents() throws URISyntaxException, DataStoreException, IOException {
        final Set<String> identifiers = new HashSet<>(List.of("EPSG:4326", "Sample 1", "Sample 2", "Sample 3", "data4"));
        final Path path = testDirectory();
        try (Store store = new Store(null, new StorageConnector(path), path, null)) {
            assertEquals(4, store.components().size(), "Wrong number of data stores.");
            verifyContent(store, identifiers);
        }
        if (!identifiers.isEmpty()) {
            fail("Missing resources: " + identifiers);
        }
        loggings.skipNextLogIfContains("ucar.unidata");
        loggings.skipNextLogIfContains("ucar.unidata");     // Logs emitted by UCAR. There are two files to skip.
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Verifies that specifying a format effectively restricts the number of resources to be found.
     *
     * @throws URISyntaxException if the URL to test data cannot be converted to a path of the file system.
     * @throws DataStoreException if an error occurred while reading the resources.
     * @throws IOException if an I/O error occurs.
     */
    @Test
    public void testSearchProviderParameter() throws URISyntaxException, DataStoreException, IOException {
        final StoreProvider provider = new StoreProvider();
        final Set<String> identifiers = new HashSet<>(List.of("Sample 1", "Sample 2", "Sample 3", "data4"));
        final ParameterValueGroup params = StoreProvider.PARAMETERS.createValue();
        params.parameter("location").setValue(testDirectory());
        params.parameter("format").setValue("XML");
        try (Store store = (Store) provider.open(params)) {
            assertEquals(3, store.components().size(), "Expected one less data store.");
            verifyContent(store, identifiers);
        }
        if (!identifiers.isEmpty()) {
            fail("Missing resources: " + identifiers);
        }
        loggings.assertNoUnexpectedLog();
    }

    /**
     * Verifies that the given metadata contains one of the given identifiers.
     * The identifiers that are found are removed from the given set.
     */
    private static void verifyContent(final Aggregate store, final Set<String> identifiers) throws DataStoreException {
        for (final Resource resource : store.components()) {
            assertNotNull(resource);
            for (Identification info : resource.getMetadata().getIdentificationInfo()) {
                final String id = Citations.getIdentifier(info.getCitation());
                assertTrue(identifiers.remove(id), id);
                if (resource instanceof Aggregate aggregate) {
                    verifyContent(aggregate, identifiers);
                }
            }
        }
    }
}
