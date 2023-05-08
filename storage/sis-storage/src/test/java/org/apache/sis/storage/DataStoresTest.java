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
import static org.apache.sis.test.Assertions.assertSetEquals;


/**
 * Tests {@link DataStores}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.1
 * @since   0.4
 */
@DependsOn(StoreTest.class)
public final class DataStoresTest extends TestCase {
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

    /**
     * Fetches the providers in concurrent threads and verifies that all of them created the same set of providers.
     *
     * @throws Exception if a thread has been interrupted or a timeout occurred.
     */
    @Test
    public void datastore_registry_must_be_thread_safe() throws Exception {
        final int maxNbWorkers = 6;
        final ExecutorService exec = Executors.newFixedThreadPool(maxNbWorkers);
        final List<Set<Class<?>>> allResults = new ArrayList<>();
        for (int nbWorkers = 4; nbWorkers <= maxNbWorkers; nbWorkers++) {
            allResults.addAll(collectProvidersConcurrently(nbWorkers, exec));
        }
        exec.shutdown();
        /*
         * All sets shall have the same content. Arbitrarily compare each list element
         * with the previous element, but any other combination should be equivalent.
         */
        for (int i = 1; i < allResults.size(); i++) {
            assertSetEquals(allResults.get(i - 1), allResults.get(i));
        }
        exec.awaitTermination(10, TimeUnit.SECONDS);        // Wait will be much shorter in practice.
        assertTrue(exec.isTerminated());
    }

    /**
     * Starts the specified amount of worker threads where each thread ask for the set of providers.
     *
     * @param  nbWorkers  number of concurrent threads.
     * @param  executor   the executor to use for running the threads.
     * @return the set of of providers found by each thread.
     *         All sets should be equal, but that verification is not done by this method.
     * @throws Exception if a thread has been interrupted or a timeout occurred.
     */
    private static List<Set<Class<?>>> collectProvidersConcurrently(final int nbWorkers, final ExecutorService executor)
            throws Exception
    {
        final DataStoreRegistry dsr = new DataStoreRegistry(DataStoresTest.class.getClassLoader());
        final CyclicBarrier startSignal = new CyclicBarrier(nbWorkers);
        final Callable<Set<Class<?>>> collectProviderClasses = () -> {
            final Set<Class<?>> result = new HashSet<>();
            startSignal.await(10, TimeUnit.SECONDS);                        // Wait until all workers are ready.
            for (DataStoreProvider p : dsr.providers()) {                   // This is the iteration to test.
                result.add(p.getClass());
            }
            return result;
        };
        /*
         * All threads will execute the same task — `collectProviderClasses` — but a separated list is created
         * by each worker. A cyclic barrier is used for making sure that all threads start their iterations in
         * same time.
         */
        final List<Future<Set<Class<?>>>> tasks = new ArrayList<>(nbWorkers);
        for (int i = 0 ; i < nbWorkers ; i++) {
            tasks.add(executor.submit(collectProviderClasses));
        }
        /*
         * Wait for each thread to finish its test and collect the results. Analysis will be done by the caller.
         */
        final List<Set<Class<?>>> results = new ArrayList<>(nbWorkers);
        for (final Future<Set<Class<?>>> workerResult : tasks) {
            results.add(workerResult.get(10, TimeUnit.SECONDS));            // Wait will be much shorter in practice.
        }
        return results;
    }
}
