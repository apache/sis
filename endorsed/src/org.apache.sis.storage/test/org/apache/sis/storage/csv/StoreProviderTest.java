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
package org.apache.sis.storage.csv;

import org.apache.sis.setup.OptionKey;
import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.ProbeResult;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link StoreProvider}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class StoreProviderTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public StoreProviderTest() {
    }

    /**
     * Tests {@link StoreProvider#probeContent(StorageConnector)} method.
     *
     * @throws DataStoreException if en error occurred while reading the CSV file.
     */
    @Test
    public void testProbeContent() throws DataStoreException {
        final var p = new StoreProvider();
        final var c = new StorageConnector(StoreTest.testData());
        c.setOption(OptionKey.GEOMETRY_LIBRARY, GeometryLibrary.ESRI);
        assertEquals(ProbeResult.SUPPORTED, p.probeContent(c));
    }
}
