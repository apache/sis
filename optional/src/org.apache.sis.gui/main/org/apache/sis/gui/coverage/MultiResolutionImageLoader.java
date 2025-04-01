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
package org.apache.sis.gui.coverage;

import java.util.WeakHashMap;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.map.coverage.MultiResolutionCoverageLoader;


/**
 * A helper class for reading {@link GridCoverage} for rendering purposes.
 * The same instance may be shared by {@link GridView} and {@link CoverageCanvas}.
 * {@code GridView} uses only level 0, while {@code CoverageCanvas} use any level.
 *
 * <h2>Multi-threading</h2>
 * Instances of this class are immutable (except for the cache) and safe for use by multiple threads.
 * The same instance may be shared by many {@link CoverageCanvas} or {@link GridView} objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class MultiResolutionImageLoader extends MultiResolutionCoverageLoader {
    /**
     * The loaders created for grid coverage resources.
     */
    private static final WeakHashMap<GridCoverageResource, MultiResolutionImageLoader> CACHE = new WeakHashMap<>();

    /**
     * Creates a new loader of grid coverages from the given resource. The loader assumes a pyramid with
     * the declared resolutions of given resource if present, or computes default resolutions otherwise.
     *
     * @param  resource  the resource from which to read grid coverages.
     * @throws DataStoreException if an error occurred while querying the resource for resolutions.
     */
    private MultiResolutionImageLoader(final GridCoverageResource resource) throws DataStoreException {
        super(resource, null, null);
    }

    /**
     * Gets or creates a new loader of grid coverages from the given resource. The loader assumes a pyramid
     * with the declared resolutions of given resource if present, or computes default resolutions otherwise.
     * This method returns cached instances if available.
     *
     * @param  resource  the resource from which to read grid coverages.
     * @param  cached    a cached instance that may be reused, or {@code null} if none.
     * @return loader for the specified resource (never {@code null}).
     * @throws DataStoreException if an error occurred while querying the resource for resolutions.
     */
    static MultiResolutionCoverageLoader getInstance(final GridCoverageResource resource,
            MultiResolutionCoverageLoader cached) throws DataStoreException
    {
        if (cached == null || cached.resource != resource) {
            synchronized (CACHE) {
                cached = CACHE.get(resource);
            }
            if (cached == null) {
                final var loader = new MultiResolutionImageLoader(resource);
                synchronized (CACHE) {
                    cached = CACHE.putIfAbsent(resource, loader);
                }
                if (cached == null) {
                    cached = loader;
                }
            }
        }
        return cached;
    }
}
