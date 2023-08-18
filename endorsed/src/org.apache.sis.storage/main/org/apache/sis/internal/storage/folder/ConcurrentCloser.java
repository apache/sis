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

import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.internal.storage.StoreResource;
import org.apache.sis.internal.system.CommonExecutor;


/**
 * Helper class for closing concurrently a collection of data stores.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @param  <R>  type of resource to close.
 *
 * @since 1.4
 */
public abstract class ConcurrentCloser<R> {
    /**
     * A closer for a collection of resources which may be data stores.
     * This closer does not check for {@link StoreResource} instances.
     */
    public static final ConcurrentCloser<Resource> RESOURCES = new ConcurrentCloser<>() {
        @Override protected Callable<?> closer(final Resource r) {
            if (r instanceof DataStore) {
                final DataStore ds = (DataStore) r;
                return () -> {
                    ds.close();
                    return null;
                };
            } else return null;
        }
    };

    /**
     * Creates a new closer.
     */
    protected ConcurrentCloser() {
    }

    /**
     * Creates a task to be invoked in a background thread for closing the given resource.
     * The return value of the callable will be ignored.
     *
     * @param  resource  the resource to close.
     * @return the task for closing the given resource, or {@code null} if none.
     */
    protected abstract Callable<?> closer(R resource);

    /**
     * Closes concurrently all the given resources.
     *
     * @param  resources  the resource to close.
     * @throws DataStoreException if at least one error occurred while closing a resource.
     */
    public final void closeAll(final Collection<? extends R> resources) throws DataStoreException {
        final ExecutorService executor = CommonExecutor.instance();
        final Future<?>[] results = new Future<?>[resources.size()];
        int n = 0;
        for (final R r : resources) {
            final Callable<?> c = closer(r);
            if (c != null) {
                results[n++] = executor.submit(c);
            }
        }
        /*
         * Wait for all tasks to complete and collect
         * the exceptions that are thrown, if any.
         */
        DataStoreException failure = null;
        for (int i=0; i<n; i++) {
            try {
                results[i].get();
            } catch (InterruptedException | ExecutionException ex) {
                Throwable cause = ex.getCause();
                if (cause == null) cause = ex;
                if (failure != null) {
                    failure.addSuppressed(cause);
                } else if (cause instanceof DataStoreException) {
                    failure = (DataStoreException) cause;
                } else {
                    failure = new DataStoreException(cause);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
