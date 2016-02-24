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
package org.apache.sis.storage;

import java.io.StringReader;
import org.apache.sis.internal.storage.xml.StoreTest;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link DataStores}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.7
 * @module
 */
@DependsOn(StoreTest.class)
public final strictfp class DataStoresTest extends TestCase {
    /**
     * Tests {@link DataStores#probeContentType(Object)}.
     *
     * @throws DataStoreException Should never happen.
     */
    @Test
    public void testProbeContentType() throws DataStoreException {
        final String type = DataStores.probeContentType(new StringReader(StoreTest.XML));
        assertEquals("application/vnd.iso.19139+xml", type);
    }

    /**
     * Tests {@link DataStores#open(Object)}.
     *
     * @throws DataStoreException Should never happen.
     */
    @Test
    public void testOpen() throws DataStoreException {
        final DataStore store = DataStores.open(new StringReader(StoreTest.XML));
        assertFalse(store.getMetadata().getContacts().isEmpty());
    }
}
