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
package org.apache.sis.storage.aggregate;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.awt.image.RenderedImage;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.SubspaceNotSpecifiedException;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.DisjointExtentException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.privy.Numerics;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.logging.Logging;
import static org.apache.sis.image.privy.ImageUtilities.LOGGER;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coverage.CannotEvaluateException;


/**
 * A grid coverage where a single dimension is the concatenation of many grid coverages.
 * All components must have the same "grid to CRS" transform, except for a translation term.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ConcatenatedGridCoverage extends GridCoverage {
    /**
     * The object for identifying indices in the {@link #slices} array.
     */
    private final GridSliceLocator locator;

    /**
     * Index of the first slice in {@link #locator}.
     */
    private final int startAt;

    /**
     * The class in charge of loading and caching grid coverages.
     * The same loader may be shared by many {@link ConcatenatedGridCoverage} instances.
     * Loaders are immutable (except for the cache) and thread-safe.
     */
    private static final class Loader {
        /**
         * Whether loading of grid coverages should be deferred to rendering time.
         * This is a bit set packed as {@code int} values. A bit value of 1 means
         * that the coverages at the corresponding index should be loaded from the
         * {@linkplain #slices slices} at same index only when first needed.
         *
         * <h4>Invariant</h4>
         * Deferred {@linkplain #slices slices} shall be instances of {@link GridCoverage}
         * and non-deferred slices shall be instances of {@link GridCoverageResource}.
         * So the {@code deferred} flags are redundant with {@code instanceof} checks.
         * However, implementation classes may implement both interfaces.
         * So those flags tell how to use a slice instance.
         *
         * @see ConcatenatedGridResource#deferredLoading
         * @see #isDeferred(int)
         * @see #valid()
         */
        final int[] deferred;

        /**
         * The domain to request when reading a coverage from the resource.
         */
        private final GridGeometry domain;

        /**
         * The sample dimensions to request when loading slices from the {@linkplain #resources}.
         */
        private final int[] ranges;

        /**
         * Cache of {@link GridCoverage} instances. Keys are index in the {@link #slices} array.
         */
        private final Cache<Integer,GridCoverage> coverages;

        /**
         * Creates a new loader.
         *
         * @param deferred  whether loading of grid coverages should be deferred to rendering time.
         * @param domain    grid geometry to request when loading data.
         * @param ranges    bands to request when loading coverages.
         */
        Loader(final int[] deferred, final GridGeometry domain, final int[] ranges) {
            this.deferred = deferred;
            this.domain   = domain;
            this.ranges   = ranges;
            coverages     = new Cache<>(15, 2, true);   // Keep 2 slices by strong reference (for interpolations).
        }

        /**
         * Returns the coverage if available in the cache, or load it immediately otherwise.
         * This method shall be invoked only when {@code isDeferred(key) == true}.
         * This method can be invoked from any thread.
         *
         * @param  key     index of the {@link GridCoverageResource} in the {@link #slices} array.
         * @param  source  value of {@code slices[key]}, used only if data need to be loaded.
         * @return the coverage at the given index.
         * @throws NullPointerException if no {@link GridCoverageResource} are expected to exist.
         * @throws DataStoreException if an error occurred while loading data from the resource.
         */
        final GridCoverage getOrLoad(final Integer key, final GridCoverageResource source) throws DataStoreException {
            GridCoverage coverage = coverages.peek(key);
            if (coverage == null) {
                final Cache.Handler<GridCoverage> handler = coverages.lock(key);
                try {
                    coverage = handler.peek();
                    if (coverage == null) {
                        coverage = source.read(domain, ranges);
                    }
                } finally {
                    handler.putAndUnlock(coverage);
                }
            }
            return coverage;
        }
    }

    /**
     * The object in charge of loading and caching grid coverages, or {@code null} if none.
     * The same loader may be shared by many {@link ConcatenatedGridCoverage} instances.
     */
    private final Loader loader;

    /**
     * The slices of this coverage, in the same order as {@link GridSliceLocator#sliceLows}.
     * Array elements shall be instances of {@link GridCoverage} or {@link GridCoverageResource}.
     * Each slice is not necessarily 1 cell tick; larger slices are accepted.
     * The length of this array shall be at least 2. Shall be read-only.
     */
    private final Object[] slices;

    /**
     * Whether this grid coverage should be considered as converted.
     * This is used only if the {@linkplain #slices} are lazily loaded.
     */
    private final boolean isConverted;

    /**
     * Algorithm to apply when more than one grid coverage can be found at the same grid index.
     * This is {@code null} if no merge should be attempted.
     */
    private final MergeStrategy strategy;

    /**
     * Creates a new aggregated coverage.
     *
     * @param source    the concatenated resource which is creating this coverage.
     * @param domain    domain of the coverage to create.
     * @param slices    each slice as instances of {@link GridCoverage} or {@link GridCoverageResource}.
     * @param startAt   index of the first slice in {@link #locator}.
     * @param deferred  whether loading of grid coverages should be deferred to rendering time, or {@code null} if none.
     * @param request   grid geometry to request when loading data. Used only if {@code slices} are lazily loaded.
     * @param ranges    bands to request when loading coverages. Used only if {@code slices} are lazily loaded.
     */
    ConcatenatedGridCoverage(final ConcatenatedGridResource source, final GridGeometry domain, final Object[] slices,
                             final int startAt, final int[] deferred, final GridGeometry request, final int[] ranges)
    {
        super(domain, source.getSampleDimensions());
        loader = (deferred != null) ? new Loader(deferred, request, ranges) : null;
        this.slices      = slices;
        this.startAt     = startAt;
        this.isConverted = source.isConverted;
        this.locator     = source.locator;
        this.strategy    = source.strategy;
        assert valid();
    }

    /**
     * Creates a new aggregated coverage for the result of a conversion from/to packed values.
     * This constructor assumes that all slices use the same sample dimensions.
     */
    private ConcatenatedGridCoverage(final ConcatenatedGridCoverage source, final Object[] slices,
                                     final List<SampleDimension> sampleDimensions, final boolean converted)
    {
        super(source.getGridGeometry(), sampleDimensions);
        this.slices      = slices;
        this.loader      = source.loader;
        this.startAt     = source.startAt;
        this.locator     = source.locator;
        this.strategy    = source.strategy;
        this.isConverted = converted;
        assert valid();
    }

    /**
     * Verifies that all {@linkplain #slices} are instances of the expected interface.
     * This is used for assertions only.
     */
    private boolean valid() {
        for (int i=0; i < slices.length; i++) {
            final Object actual = slices[i];
            Class<?> expected = isDeferred(i) ? GridCoverageResource.class : GridCoverage.class;
            if (!expected.isInstance(actual)) throw new AssertionError(actual);
        }
        return true;
    }

    /**
     * Returns {@code true} if the loading of the coverage at the given index is deferred.
     * If {@code true},  then {@code slices[i]} shall be an instance of {@link GridCoverageResource}.
     * If {@code false}, then {@code slices[i]} shall be an instance of {@link GridCoverage}.
     */
    private boolean isDeferred(final int i) {
        return (loader != null) && (loader.deferred[i >>> Numerics.INT_SHIFT] & (1 << i)) != 0;
    }

    /**
     * Returns a grid coverage that contains real values or sample values,
     * depending if {@code converted} is {@code true} or {@code false} respectively.
     * This method delegates to all slices in this concatenated coverage.
     *
     * @param  converted  {@code true} for a coverage containing converted values,
     *                    or {@code false} for a coverage containing packed values.
     * @return a coverage containing requested values. May be {@code this} but never {@code null}.
     */
    @Override
    protected GridCoverage createConvertedValues(final boolean converted) {
        boolean changed = false;
        GridCoverage template = null;           // Arbitrary instance to use as a template for sample dimensions.
        final Object[] c = slices.clone();
        for (int i=0; i<c.length; i++) {
            if (!isDeferred(i)) {
                final var source = (GridCoverage) c[i];     // Should never fail.
                changed |= (c[i] = source.forConvertedValues(converted)) != source;
                template = source;
            } else {
                changed |= (converted != isConverted);
            }
        }
        if (!changed) {
            return this;
        }
        final List<SampleDimension> sampleDimensions;
        if (template !=null) {
            sampleDimensions = template.getSampleDimensions();
        } else {
            sampleDimensions = new ArrayList<>(getSampleDimensions());
            sampleDimensions.replaceAll((b) -> b.forConvertedValues(converted));
        }
        return new ConcatenatedGridCoverage(this, c, sampleDimensions, converted);
    }

    /**
     * Returns a two-dimensional slice of grid data as a rendered image.
     * Invoking this method may cause the loading of data from {@link ConcatenatedGridResource}.
     * Most recently used slices are cached for future invocations of this method.
     *
     * @param  extent  a subspace of this grid coverage where all dimensions except two have a size of 1 cell.
     * @return the grid slice as a rendered image. Image location is relative to {@code extent}.
     */
    @Override
    public RenderedImage render(GridExtent extent) {
        int lower = startAt;
        int upper = lower + slices.length;
        if (extent != null) {
            upper = locator.getUpper(extent, lower, upper);
            lower = locator.getLower(extent, lower, upper);
        } else {
            extent = gridGeometry.getExtent();
        }
        int count = upper - lower;
        if (count == 1) {
            return slice(extent, lower);
        }
        /*
         * We have a non-trivial number of source coverages to aggregate.
         * The `failure` exception will be thrown at the end of this method
         * if a merge was attempted but could not find suitable sources.
         */
        DisjointExtentException failure = null;
        if (count > 0) {
            if (strategy == null) {
                throw new SubspaceNotSpecifiedException(Resources.format(
                        Resources.Keys.NoSliceMapped_3, locator.getDimensionName(extent), lower, count));
            }
            /*
             * Prepare a list of slice candidates. Later in this method, a single slice may be selected
             * among those candidates using the user-specified merge strategy. Elements in `candidates`
             * array will be removed if that candidate did not worked and we want to look again among
             * remaining candidates.
             */
            final GridGeometry   request;           // The geographic area and temporal extent requested by user.
            final GridGeometry[] candidates;        // Grid geometry of all slices that intersect the request.
            try {
                request    = new GridGeometry(getGridGeometry(), extent, null);
                candidates = new GridGeometry[count];
                for (int i=0; i<count; i++) {
                    final int j = lower + i;
                    final Object slice = slices[j];
                    candidates[i] = isDeferred(j) ? ((GridCoverageResource) slice).getGridGeometry()
                                                  : ((GridCoverage)         slice).getGridGeometry();
                }
            } catch (DataStoreException | TransformException e) {
                throw new CannotEvaluateException(Resources.format(Resources.Keys.CanNotSelectSlice), e);
            }
            /*
             * The following loop should be executed exactly once. However, it may happen that the "best" slice
             * actually does not intersect the requested extent, for example because the merge strategy looked
             * only for temporal intersection and did not saw that the geographic extent does not intersect.
             */
            final int[] indexes = ArraysExt.range(lower, lower + count);
            final var   sources = new RenderedImage[count];
            do {
                int accepted = 0;
                for (final int i : strategy.filter(request, Arrays.copyOf(candidates, count))) {
                    try {
                        sources[accepted] = slice(extent, indexes[i]);
                        accepted++;     // On a separated line for incrementing only on success.
                    } catch (DisjointExtentException e) {
                        if (failure == null) failure = e;
                        else failure.addSuppressed(e);
                        final int remaining = --count - i;
                        System.arraycopy(candidates, i+1, candidates, i, remaining);
                        System.arraycopy(indexes,    i+1, indexes,    i, remaining);
                    }
                }
                if (accepted > 0) {
                    if (failure != null) {
                        Logging.ignorableException(LOGGER, ConcatenatedGridCoverage.class, "render", failure);
                    }
                    return strategy.aggregate(ArraysExt.resize(sources, accepted));
                }
            } while (count > 0);
        }
        /*
         * No coverage found in the specified area of interest.
         */
        if (failure == null) {
            failure = new DisjointExtentException(gridGeometry.getExtent(), extent, locator.searchDimension);
        }
        throw failure;
    }

    /**
     * Processes to the rendering of a single slice.
     *
     * @param  extent  a subspace of this grid coverage where all dimensions except two have a size of 1 cell.
     * @param  index   index of the slice to render.
     * @return the grid slice as a rendered image. Image location is relative to {@code extent}.
     * @throws CannotEvaluateException if the slice cannot be rendered.
     */
    private RenderedImage slice(final GridExtent extent, final int index) {
        final Object slice = slices[index];
        final GridCoverage coverage;
        if (!isDeferred(index)) {
            coverage = (GridCoverage) slice;        // This cast should never fail.
        } else try {
            coverage = loader.getOrLoad(index, (GridCoverageResource) slice).forConvertedValues(isConverted);
        } catch (DataStoreException e) {
            throw new CannotEvaluateException(Resources.format(Resources.Keys.CanNotReadSlice_1, index + startAt), e);
        }
        /*
         * At this point, coverage of the "best" slice has been fetched from the cache or read from resource.
         * Delegate the rendering to that coverage, after converting the extent from this grid coverage space
         * to the slice coordinate space. If the coverage said that the converted extent does not intersect,
         * try the "next best" slice until we succeed or until we exhausted the candidate list.
         */
        return coverage.render(locator.toSliceExtent(extent, index));
    }
}
