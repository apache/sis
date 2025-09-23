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
import java.util.TreeMap;
import java.awt.image.RenderedImage;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.SubspaceNotSpecifiedException;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.DisjointExtentException;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.internal.Resources;
import org.apache.sis.storage.base.MemoryGridResource;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.internal.shared.Numerics;
import static org.apache.sis.image.internal.shared.ImageUtilities.LOGGER;

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
     * All slices in this coverage, together with methods for locating the slices to use.
     */
    private final GridSliceLocator locator;

    /**
     * The sample dimensions to request when loading slices from the resource, or {@code null} for all.
     */
    private final int[] ranges;

    /**
     * Whether this grid coverage should be considered as converted.
     */
    private final boolean isConverted;

    /**
     * The (un)converted grid coverage for the opposite of the {@link #isConverted} flag.
     */
    private ConcatenatedGridCoverage opposite;

    /**
     * Cache of {@link GridCoverage} instances for each slice.
     * This cache is shared by converted and non-converted views.
     */
    private final Cache<GridSlice,GridCoverage> coverages;

    /**
     * The resolution that we expect from loaded coverages.
     * This is used for verifying the consistency.
     */
    private final double[] expectedResolution;

    /**
     * Algorithm to apply when more than one grid coverage can be found at the same grid index.
     * This is {@code null} if no merge should be attempted.
     */
    private final MergeStrategy strategy;

    /**
     * Creates a new aggregated coverage.
     *
     * @param source   the concatenated resource which is creating this coverage.
     * @param locator  all slices in this coverage, together with methods for locating the slices to use.
     * @param ranges   bands to request when loading coverages. Used only if {@code slices} are lazily loaded.
     */
    ConcatenatedGridCoverage(final ConcatenatedGridResource source, final GridSliceLocator locator, final int[] ranges) {
        super(locator.gridGeometry, source.getSampleDimensions());
        this.ranges        = ranges;
        this.locator       = locator;
        this.isConverted   = source.isConverted;
        this.strategy      = source.mergeStrategy;
        this.coverages     = new Cache<>(15, 2, true);   // Keep 2 slices by strong reference (for interpolations).
        expectedResolution = gridGeometry.getResolution(true);
    }

    /**
     * Creates a new aggregated coverage for the result of a conversion from/to packed values.
     * This constructor assumes that all slices use the same sample dimensions.
     */
    private ConcatenatedGridCoverage(final ConcatenatedGridCoverage source, final GridSliceLocator locator,
                                     final List<SampleDimension> sampleDimensions, final boolean converted)
    {
        super(source.getGridGeometry(), sampleDimensions);
        this.isConverted   = converted;
        this.locator       = locator;
        this.ranges        = source.ranges;
        this.strategy      = source.strategy;
        this.coverages     = source.coverages;
        expectedResolution = source.expectedResolution;
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
    protected synchronized GridCoverage createConvertedValues(final boolean converted) {
        if (converted == isConverted) {
            return this;
        }
        if (opposite == null) {
            List<SampleDimension> sampleDimensions = null;
            for (final GridSlice slice : locator.slices) {
                if (slice.resource instanceof MemoryGridResource) {
                    GridCoverage coverage = ((MemoryGridResource) slice.resource).coverage;
                    coverage = coverage.forConvertedValues(converted);
                    sampleDimensions = coverage.getSampleDimensions();
                    break;
                }
            }
            if (sampleDimensions == null) {
                sampleDimensions = new ArrayList<>(getSampleDimensions());
                sampleDimensions.replaceAll((b) -> b.forConvertedValues(converted));
            }
            opposite = new ConcatenatedGridCoverage(this, locator, sampleDimensions, converted);
        }
        return opposite;
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
        if (extent == null) {
            extent = gridGeometry.getExtent();
        }
        final int[] selection = locator.find(extent);
        int count = selection.length;
        if (count == 1) {
            return render(locator.slices[selection[0]], extent);
        }
        /*
         * We have a non-trivial number of source coverages to aggregate.
         * The `failure` exception will be thrown at the end of this method
         * if a merge was attempted but could not find suitable sources.
         */
        DisjointExtentException failure = null;
        if (count > 0) {
            if (strategy == null) {
                throw new SubspaceNotSpecifiedException(Resources.format(Resources.Keys.NoSliceMapped_3,
                        locator.getDimensionName(extent), extent.getMedian(locator.searchDimension), count));
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
                    final int j = selection[i];
                    final GridSlice slice = locator.slices[j];
                    candidates[i] = slice.resource.getGridGeometry();
                }
            } catch (DataStoreException | TransformException e) {
                throw new CannotEvaluateException(Resources.format(Resources.Keys.CanNotSelectSlice), e);
            }
            /*
             * The following loop should be executed exactly once. However, it may happen that the "best" slice
             * actually does not intersect the requested extent, for example because the merge strategy looked
             * only for temporal intersection and did not saw that the geographic extent does not intersect.
             */
            final var sources = new TreeMap<GridSlice,RenderedImage>();
            do {
                for (final int i : strategy.filter(request, Arrays.copyOf(candidates, count))) {
                    try {
                        final GridSlice slice = locator.slices[selection[i]];
                        if (!sources.containsKey(slice)) {
                            sources.put(slice, render(slice, extent));
                        }
                    } catch (DisjointExtentException e) {
                        if (failure == null) failure = e;
                        else failure.addSuppressed(e);
                        final int remaining = --count - i;
                        System.arraycopy(candidates, i+1, candidates, i, remaining);
                        System.arraycopy(selection,  i+1, selection,  i, remaining);
                    }
                }
                if (!sources.isEmpty()) {
                    if (failure != null) {
                        Logging.ignorableException(LOGGER, ConcatenatedGridCoverage.class, "render", failure);
                    }
                    return strategy.aggregate(sources.values().toArray(RenderedImage[]::new));
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
     * Processes to the rendering of a single slice. The given extent shall be relative to
     * the {@link #gridGeometry} origin, which is the union of the domains of all sources.
     *
     * @param  slice   the slice to render.
     * @param  extent  the desired subspace in units of {@link #gridGeometry} grid cells.
     * @return the grid slice as a rendered image. Image location is relative to {@code extent}.
     * @throws CannotEvaluateException if the slice cannot be rendered.
     */
    private RenderedImage render(final GridSlice slice, final GridExtent extent) {
        final int[] subdim = extent.getSubspaceDimensions(GridSlice.BIDIMENSIONAL);
        GridCoverage coverage = coverages.peek(slice);
        if (coverage == null) try {
            final Cache.Handler<GridCoverage> handler = coverages.lock(slice);
            try {
                coverage = handler.peek();
                if (coverage == null) {
                    coverage = slice.resource.read(gridGeometry, ranges);
                }
            } finally {
                handler.putAndUnlock(coverage);
            }
            coverage = coverage.forConvertedValues(isConverted);
        } catch (DataStoreException e) {
            throw new CannotEvaluateException(Resources.format(Resources.Keys.CanNotReadSlice_1, slice.getIdentifier()), e);
        }
        /*
         * At this point, coverage of the "best" slice has been fetched from the cache or read from resource.
         * Delegate the rendering to that coverage, after converting the extent from this grid coverage space
         * to the slice coordinate space. If the coverage said that the converted extent does not intersect,
         * the caller will try the "next best" slice until we succeed or until we exhausted the candidate list.
         */
        final double[] resolution = coverage.getGridGeometry().getResolution(true);
        for (int i : subdim) {
            if (!Numerics.equals(resolution[i], expectedResolution[i])) {
                throw new CannotEvaluateException(Resources.format(Resources.Keys.UnexpectedSliceResolution_3,
                            resolution(expectedResolution, subdim), resolution(resolution, subdim), slice.getIdentifier()));
            }
        }
        return slice.render(coverage, extent, subdim);
    }

    /**
     * Returns the resolution in the dimensions used for image rendering.
     * This is used for formatting an error message only.
     */
    private static Double resolution(final double[] resolution, final int[] subdim) {
        return Math.hypot(resolution[subdim[0]], resolution[subdim[1]]);
    }
}
