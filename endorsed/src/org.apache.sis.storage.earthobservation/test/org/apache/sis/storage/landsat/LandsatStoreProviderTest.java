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
package org.apache.sis.storage.landsat;

import java.nio.charset.StandardCharsets;
import org.apache.sis.setup.OptionKey;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.sis.test.TestCase;


/**
 * Tests {@link LandsatStoreProvider}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class LandsatStoreProviderTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public LandsatStoreProviderTest() {
    }

    /**
     * Tests {@link LandsatStoreProvider#probeContent(StorageConnector)} method.
     *
     * @throws DataStoreException if an error occurred while reading the test file.
     */
    @Test
    public void testProbeContentFromReader() throws DataStoreException {
        final StorageConnector connector = new StorageConnector(MetadataReaderTest.class.getResourceAsStream("LandsatTest.txt"));
        connector.setOption(OptionKey.ENCODING, StandardCharsets.UTF_8);
        final LandsatStoreProvider provider = new LandsatStoreProvider();
        assertEquals(ProbeResult.SUPPORTED, provider.probeContent(connector));
    }
}
