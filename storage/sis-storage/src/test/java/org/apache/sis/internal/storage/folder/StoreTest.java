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
package org.apache.sis.internal.storage.folder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.metadata.identification.Identification;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;


/**
 * Tests folder {@link Store}.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 * @since   0.8
 * @module
 */
public final strictfp class StoreTest extends TestCase {
    /**
     * Gets the path to the test directory. If the directory is not accessible through the file system
     * (for example if the test data are read from a JAR file), then skip the tests. This happen if the
     * test are executed from another Maven module than {@code sis-storage}.
     */
    private static Path testDirectory() throws URISyntaxException {
        final URL sample = StoreTest.class.getResource("test-data/README.txt");
        assertNotNull("Test data not found", sample);
        assumeTrue(sample.getProtocol().equals("file"));
        return Paths.get(sample.toURI()).getParent();
    }

    /**
     * Verifies that components in a folder are correctly detected.
     *
     * @throws URISyntaxException if the URL to test data can not be converted to a path of the file system.
     * @throws DataStoreException if an error occurred while reading the resources.
     * @throws IOException if an I/O error occurs.
     */
    @Test
    public void testComponents() throws URISyntaxException, DataStoreException, IOException {
        final Set<String> identifiers = new HashSet<>(Arrays.asList("EPSG:4326", "Sample 1", "Sample 2", "Sample 3", "data4"));
        final Path path = testDirectory();
        try (Store store = new Store(null, new StorageConnector(path), path, null)) {
            assertEquals("Wrong number of data stores.", 4, store.components().size());
            verifyContent(store, identifiers);
        }
        if (!identifiers.isEmpty()) {
            fail("Missing resources: " + identifiers);
        }
    }

    /**
     * Verifies that specifying a format effectively restricts the number of resources to be found.
     *
     * @throws URISyntaxException if the URL to test data can not be converted to a path of the file system.
     * @throws DataStoreException if an error occurred while reading the resources.
     * @throws IOException if an I/O error occurs.
     */
    @Test
    public void testSearchProviderParameter() throws URISyntaxException, DataStoreException, IOException {
        final StoreProvider provider = new StoreProvider();
        final Set<String> identifiers = new HashSet<>(Arrays.asList("Sample 1", "Sample 2", "Sample 3", "data4"));
        final ParameterValueGroup params = StoreProvider.PARAMETERS.createValue();
        params.parameter("location").setValue(testDirectory());
        params.parameter("format").setValue("XML");
        try (Store store = (Store) provider.open(params)) {
            assertEquals("Expected one less data store.", 3, store.components().size());
            verifyContent(store, identifiers);
        }
        if (!identifiers.isEmpty()) {
            fail("Missing resources: " + identifiers);
        }
    }

    /**
     * Verifies that the given metadata contains one of the given identifiers.
     * The identifiers that are found are removed from the given set.
     */
    private static void verifyContent(final Aggregate store, final Set<String> identifiers) throws DataStoreException {
        for (final Resource resource : store.components()) {
            assertNotNull("resource", resource);
            for (Identification info : resource.getMetadata().getIdentificationInfo()) {
                final String id = Citations.getIdentifier(info.getCitation());
                assertTrue(id, identifiers.remove(id));
                if (resource instanceof Aggregate) {
                    verifyContent((Aggregate) resource, identifiers);
                }
            }
        }
    }
}
