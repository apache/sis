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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.sis.internal.storage.xml.StoreTest;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests {@link DataStores}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.7
 * @since   0.4
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


    @Test
    public void datastore_registry_must_be_thread_safe() throws Exception {
        final ExecutorService exec = Executors.newFixedThreadPool(6);
        try {
            List<Set> allResults = new ArrayList<>();
            for (int i = 4; i <= 6; i++) allResults.addAll(collectProvidersConcurrently(i, exec));

            for (int i = 1; i < allResults.size() - 1; i++) {
                assertEquals(
                        "Index " + i,
                        allResults.get(i - 1),
                        allResults.get(i)
                );
            }
            exec.shutdown();
            exec.awaitTermination(1, TimeUnit.SECONDS);
        } finally {
            exec.shutdownNow();
        }
    }

    private List<Set<Class>> collectProvidersConcurrently(int nbWorkers, final ExecutorService executor) throws Exception {
        final DataStoreRegistry dsr = new DataStoreRegistry(DataStoresTest.class.getClassLoader());
        final CyclicBarrier startSignal = new CyclicBarrier(nbWorkers);
        Callable<Set> collectProviderClasses = () -> {
            startSignal.await(1, TimeUnit.SECONDS);
            Set<Class> result = new HashSet<>();
            for (DataStoreProvider p : dsr.providers()) result.add(p.getClass());
            return result;
        };

        final List<Future<Set>> tasks = new ArrayList<>(nbWorkers);
        for (int i = 0 ; i < nbWorkers ; i++) {
            tasks.add(executor.submit(collectProviderClasses));
        }

        final List<Set<Class>> results = new ArrayList<>(nbWorkers);
        for (int i = 0 ; i < nbWorkers ; i++) {
            Set workerResult = tasks.get(i).get(2, TimeUnit.SECONDS);
            results.add(workerResult);
        }

        return results;
    }
}
