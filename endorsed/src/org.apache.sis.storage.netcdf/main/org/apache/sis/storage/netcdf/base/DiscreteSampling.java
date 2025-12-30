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
package org.apache.sis.storage.netcdf.base;

import org.apache.sis.setup.GeometryLibrary;
import org.apache.sis.geometry.wrapper.Geometries;
import org.apache.sis.filter.Optimization;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.AbstractFeatureSet;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureQuery;
import org.apache.sis.storage.base.StoreResource;
import org.apache.sis.storage.event.StoreListeners;
import org.apache.sis.util.resources.Errors;


/**
 * Returns the features encoded in the netCDF files when they are encoded as discrete sampling.
 * The netCDF attributes shall be conform to the "Discrete Sampling Geometries" chapter of
 * <a href="http://cfconventions.org/">CF conventions</a>. Some examples are trajectories
 * and profiles.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public abstract class DiscreteSampling extends AbstractFeatureSet implements StoreResource {
    /**
     * The factory to use for creating geometries.
     */
    protected final Geometries<?> factory;

    /**
     * The object to use for synchronization. For now we use a {@code synchronized} statement,
     * but it may be changed to {@link java.util.concurrent.locks.Lock} in a future version.
     * Current lock is the whole netCDF data store (so this field is opportunistically used
     * by {@link #getOriginator()}), but it may change in future version.
     *
     * @see RasterResource#lock
     * @see #getSynchronizationLock()
     */
    private final DataStore lock;

    /**
     * Creates a new discrete sampling parser.
     *
     * @param  library    the library for geometric objects, or {@code null} for the default.
     * @param  listeners  the set of registered warning listeners for the data store.
     * @param  lock       the lock to use in {@code synchronized(lock)} statements.
     * @throws IllegalArgumentException if the given library is non-null but not available.
     */
    protected DiscreteSampling(final GeometryLibrary library, final StoreListeners listeners, final DataStore lock) {
        super(listeners, false);
        factory = Geometries.factory(library);
        this.lock = lock;
    }

    /**
     * Returns the data store that produced this resource.
     */
    @Override
    public final DataStore getOriginator() {
        /*
         * Could be replaced by `(DataStore) listeners.getParent().get().getSource()`
         * if a future version decides to use a different kind of lock.
         */
        return lock;
    }

    /**
     * Returns the object on which to perform synchronizations for thread-safety.
     *
     * @return the synchronization lock.
     */
    @Override
    protected final Object getSynchronizationLock() {
        return lock;
    }

    /**
     * Configures the optimization of a query with the knowledge that the feature type is final.
     * This configuration asserts that all features will be instances of the type returned by
     * {@link #getType()}, with no sub-type.
     */
    @Override
    protected final void prepareQueryOptimization(FeatureQuery query, Optimization optimizer) throws DataStoreException {
        optimizer.setFinalFeatureType(getType());
    }

    /**
     * Returns the error message for a file that cannot be read.
     *
     * @return default error message to use in exceptions.
     */
    protected final String canNotReadFile() {
        return Errors.forLocale(listeners.getLocale()).getString(Errors.Keys.CanNotRead_1, listeners.getSourceName());
    }
}
