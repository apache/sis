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
package org.apache.sis.internal.storage.wkt;

import java.io.StringReader;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests the WKT {@link StoreProvider}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn(org.apache.sis.storage.StorageConnectorTest.class)
public final strictfp class StoreProviderTest extends TestCase {
    /**
     * Verifies validity of {@code StoreProvider.MIN_LENGTH} and {@code StoreProvider.MAX_LENGTH} constants
     * by comparing them with the content of {@code StoreProvider.KEYWORDS} map.
     */
    @Test
    public void testKeywordsMap() {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (final String keyword : StoreProvider.keywords()) {
            final int length = keyword.length();
            if (length < min) min = length;
            if (length > max) max = length;
        }
        assertEquals("MIN_LENGTH", min, StoreProvider.MIN_LENGTH);
        assertEquals("MAX_LENGTH", max, StoreProvider.MAX_LENGTH);
    }

    /**
     * Tests {@link StoreProvider#probeContent(StorageConnector)} method from a {@link Reader} object.
     *
     * @throws DataStoreException if en error occurred while reading the WKT.
     */
    @Test
    public void testProbeContentFromReader() throws DataStoreException {
        final StoreProvider p = new StoreProvider();
        testProbeContentFromReader(true,  1, p, StoreTest.WKT);
        testProbeContentFromReader(true,  2, p, "GeodeticCRS[…]");
        testProbeContentFromReader(true,  2, p, "GeodeticCRS(…)");
        testProbeContentFromReader(true,  1, p, "Vert_CS[…]");
        testProbeContentFromReader(false, 1, p, "DummyCS[…]");
        testProbeContentFromReader(true,  2, p, "   GeodeticCRS  […]");
        testProbeContentFromReader(false, 1, p, "   DummyCS  […]");
        testProbeContentFromReader(false, 0, p, "Geodetic");
    }

    /**
     * Implementation of {@link #testProbeContentFromReader()}.
     */
    private static void testProbeContentFromReader(final boolean isSupported, final int version,
            final StoreProvider p, final String wkt) throws DataStoreException
    {
        final StorageConnector c = new StorageConnector(new StringReader(wkt));
        final ProbeResult r = p.probeContent(c);
        c.closeAllExcept(null);
        assertEquals("isSupported", isSupported, r.isSupported());
        if (isSupported) {
            assertEquals("mimeType", "application/wkt", r.getMimeType());
            assertEquals("version", version, r.getVersion().getMajor());
        }
    }
}
