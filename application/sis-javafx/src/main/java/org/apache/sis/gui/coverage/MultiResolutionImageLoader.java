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
import org.apache.sis.coverage.grid.GridDerivation;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.internal.map.coverage.MultiResolutionCoverageLoader;


/**
 * A helper class for reading two-dimensional slices of {@link GridCoverage}.
 * The same instance may be shared by {@link GridView} and {@link CoverageCanvas}.
 * {@code GridView} uses only level 0, while {@code CoverageCanvas} use any level.
 *
 * <h2>Multi-threading</h2>
 * Instances of this class are immutable (except for the cache) and safe for use by multiple threads.
 * The same instance may be shared by many {@link CoverageCanvas} or {@link GridView} objects.
 *
 * <h2>Limitations (TODO)</h2>
 * Current implementation reads only the two first dimensions.
 * We will need to define an API for specifying which dimensions to use for the slices.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class MultiResolutionImageLoader extends MultiResolutionCoverageLoader {
    /**
     * The {@value} value, for identifying code that assume two-dimensional objects.
     */
    static final int BIDIMENSIONAL = 2;

    /**
     * The relative position of slice in dimensions other than the 2 visible dimensions,
     * as a ratio between 0 and 1. This may become configurable in a future version.
     *
     * @see GridDerivation#sliceByRatio(double, int[])
     */
    private static final double SLICE_RATIO = 0;

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
    static MultiResolutionImageLoader getInstance(final GridCoverageResource resource,
            MultiResolutionImageLoader cached) throws DataStoreException
    {
        if (cached == null || cached.resource != resource) {
            synchronized (CACHE) {
                cached = CACHE.get(resource);
            }
            if (cached == null) {
                final MultiResolutionImageLoader loader = new MultiResolutionImageLoader(resource);
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

    /**
     * Given a {@code GridGeometry} configured with the resolution to read, returns an amended domain
     * for a two-dimensional slice.
     *
     * @param  subgrid  a grid geometry with the desired resolution.
     * @return the domain to read from the {@linkplain #resource resource}.
     */
    @Override
    protected GridGeometry getReadDomain(final GridGeometry subgrid) {
        return slice(subgrid);
    }

    /**
     * Returns the given grid geometry with grid indices narrowed to a two dimensional slice.
     * If more than two dimensions are eligible, this method selects the 2 first ones.
     *
     * @param  gg  the grid geometry to reduce to two dimensions, or {@code null}.
     * @return the given grid geometry reduced to 2 dimensions, or {@code null} if the geometry was null.
     */
    static GridGeometry slice(GridGeometry gg) {
        if (gg != null && gg.getDimension() > BIDIMENSIONAL && gg.isDefined(GridGeometry.EXTENT)) {
            gg = slice(gg.derive(), gg.getExtent()).build();
        }
        return gg;
    }

    /**
     * Configures the given {@link GridDerivation} for applying a two-dimensional slice.
     * This method selects the two first dimensions having a size greater than 1 cell.
     *
     * @param  subgrid  a grid geometry builder pre-configured with the desired resolution.
     * @param  extent   extent of the coverage to read, in units of the finest level.
     * @return the builder configured for returning the desired two-dimensional slice.
     */
    static GridDerivation slice(final GridDerivation subgrid, final GridExtent extent) {
        final int dimension = extent.getDimension();
        if (dimension <= BIDIMENSIONAL) {
            return subgrid;
        }
        final int[] sliceDimensions = new int[BIDIMENSIONAL];
        int k = 0;
        for (int i=0; i<dimension; i++) {
            if (extent.getLow(i) != extent.getHigh(i)) {
                sliceDimensions[k] = i;
                if (++k >= BIDIMENSIONAL) break;
            }
        }
        return subgrid.sliceByRatio(SLICE_RATIO, sliceDimensions);
    }
}
