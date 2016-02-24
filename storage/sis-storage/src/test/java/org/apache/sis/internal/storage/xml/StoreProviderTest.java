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
package org.apache.sis.internal.storage.xml;

import java.io.StringReader;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link StoreProvider}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
@DependsOn(org.apache.sis.storage.StorageConnectorTest.class)
public final strictfp class StoreProviderTest extends TestCase {
    /**
     * Tests {@link StoreProvider#probeContent(StorageConnector)} method from a {@link Reader} object.
     *
     * @throws DataStoreException if en error occurred while reading the XML.
     */
    @Test
    public void testProbeContentFromReader() throws DataStoreException {
        final StoreProvider    p = new StoreProvider();
        final StorageConnector c = new StorageConnector(new StringReader(StoreTest.XML));
        final ProbeResult      r = p.probeContent(c);
        c.closeAllExcept(null);
        assertTrue  ("isSupported", r.isSupported());
        assertEquals("mimeType", "application/vnd.iso.19139+xml", r.getMimeType());
    }
}
