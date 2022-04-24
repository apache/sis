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
package org.apache.sis.internal.storage.esri;

import java.net.URL;
import java.io.IOException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.test.storage.CoverageReadConsistency;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import static org.junit.Assert.assertNotNull;


/**
 * Test consistency of read operations in random domains of a BIP (Band Interleaved by Pixel) file.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public final strictfp class BIPConsistencyTest extends CoverageReadConsistency {
    /**
     * The store used for the test, opened only once.
     */
    private static RawRasterStore store;

    /**
     * Opens the test file to be used for all tests.
     *
     * @throws IOException if an error occurred while opening the file.
     * @throws DataStoreException if an error occurred while reading the file.
     */
    @BeforeClass
    public static void openFile() throws IOException, DataStoreException {
        final URL url = BIPConsistencyTest.class.getResource("BIP.raw");
        assertNotNull("Test file not found.", url);
        store = new RawRasterStore(null, new StorageConnector(url));
    }

    /**
     * Closes the test file used by all tests.
     *
     * @throws DataStoreException if an error occurred while closing the file.
     */
    @AfterClass
    public static void closeFile() throws DataStoreException {
        final RawRasterStore s = store;
        if (s != null) {
            store = null;       // Clear first in case of failure.
            s.close();
        }
    }

    /**
     * Creates a new test case.
     *
     * @throws DataStoreException if an error occurred while fetching the first image.
     */
    public BIPConsistencyTest() throws DataStoreException {
        super(store);
    }
}
