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
package org.apache.sis.internal.storage.aggregate;

import java.util.List;
import java.util.ArrayList;
import java.awt.image.RenderedImage;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.SubspaceNotSpecifiedException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.internal.storage.Resources;

// Branch-dependent imports
import org.opengis.coverage.CannotEvaluateException;


/**
 * A grid coverage where a single dimension is the concatenation of many grid coverages.
 * All components must have the same "grid to CRS" transform, except for a translation term.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.3
 * @since   1.3
 * @module
 */
final class ConcatenatedGridCoverage extends GridCoverage {
    /**
     * The slices of this coverage, in the same order than {@link #coordinatesOfSlices}.
     * Each slice is not necessarily 1 cell tick; larger slices are accepted.
     * The length of this array shall be at least 2.
     *
     * <p>Some elements in the array may be {@code null} if the coverage are lazily loaded.</p>
     */
    private final GridCoverage[] slices;

    /**
     * The resource from which load the coverages in the {@link #slices} array, or {@code null} if none.
     * This is non-null only if the {@linkplain #slices} are lazily loaded.
     */
    private final GridCoverageResource[] resources;

    /**
     * The domain to request when reading a coverage from the resource.
     * This is non-null only if the {@linkplain #slices} are lazily loaded.
     */
    private final GridGeometry request;

    /**
     * The sample dimensions to request when loading slices from the {@linkplain #resources}.
     * This is non-null only if the {@linkplain #slices} are lazily loaded.
     */
    private final int[] ranges;

    /**
     * Whether this grid coverage should be considered as converted.
     * This is used only if the {@linkplain #slices} are lazily loaded.
     */
    private final boolean isConverted;

    /**
     * The object for identifying indices in the {@link #slices} array.
     */
    private final GridSliceLocator locator;

    /**
     * Index of the first slice in {@link #locator}.
     */
    private final int startAt;

    /**
     * Creates a new aggregated coverage.
     *
     * @param source     the concatenated resource which is creating this coverage.
     * @param domain     domain of the coverage to create.
     * @param request    grid geometry to request when loading data. Used only if {@code resources} is non-null.
     * @param slices     grid coverages for each slice. May contain {@code null} elements is lazy loading is applied.
     * @param resources  resources from which to load grid coverages, or {@code null} if none.
     * @param startAt    index of the first slice in {@link #locator}.
     * @param ranges     bands to request when loading coverages. Used only if {@code resources} is non-null.
     */
    ConcatenatedGridCoverage(final ConcatenatedGridResource source, final GridGeometry domain, final GridGeometry request,
                             final GridCoverage[] slices, final GridCoverageResource[] resources, final int startAt,
                             final int[] ranges)
    {
        super(domain, source.getSampleDimensions());
        this.slices      = slices;
        this.resources   = resources;
        this.startAt     = startAt;
        this.request     = request;
        this.ranges      = ranges;
        this.isConverted = source.isConverted;
        this.locator     = source.locator;
    }

    /**
     * Creates a new aggregated coverage for the result of a conversion from/to packed values.
     * This constructor assumes that all slices use the same sample dimensions.
     */
    private ConcatenatedGridCoverage(final ConcatenatedGridCoverage source, final GridCoverage[] slices,
            final List<SampleDimension> sampleDimensions, final boolean converted)
    {
        super(source.getGridGeometry(), sampleDimensions);
        this.slices      = slices;
        this.resources   = source.resources;
        this.startAt     = source.startAt;
        this.request     = source.request;
        this.ranges      = source.ranges;
        this.locator     = source.locator;
        this.isConverted = converted;
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
        int template = -1;              // Index of a grid coverage to use as a template.
        final GridCoverage[] c = new GridCoverage[slices.length];
        for (int i=0; i<c.length; i++) {
            final GridCoverage source = slices[i];
            if (source != null) {
                changed |= (c[i] = source.forConvertedValues(converted)) != source;
                template = i;
            } else {
                changed |= (converted != isConverted);
            }
        }
        if (!changed) {
            return this;
        }
        final List<SampleDimension> sampleDimensions;
        if (template >= 0) {
            sampleDimensions = c[template].getSampleDimensions();
        } else {
            sampleDimensions = new ArrayList<>(getSampleDimensions());
            sampleDimensions.replaceAll((b) -> b.forConvertedValues(converted));
        }
        return new ConcatenatedGridCoverage(this, c, sampleDimensions, converted);
    }

    /**
     * Returns a two-dimensional slice of grid data as a rendered image.
     *
     * @param  extent  a subspace of this grid coverage extent where all dimensions except two have a size of 1 cell.
     * @return the grid slice as a rendered image. Image location is relative to {@code sliceExtent}.
     */
    @Override
    public RenderedImage render(GridExtent extent) {
        int lower = startAt, upper = lower + slices.length;
        if (extent != null) {
            upper = locator.getUpper(extent, lower, upper);
            lower = locator.getLower(extent, lower, upper);
        } else {
            extent = gridGeometry.getExtent();
        }
        if (upper - lower != 1) {
            throw new SubspaceNotSpecifiedException();
        }
        GridCoverage slice = slices[lower];
        if (slice == null) try {
            slice = resources[lower].read(request, ranges).forConvertedValues(isConverted);
            slices[lower] = slice;
        } catch (DataStoreException e) {
            throw new CannotEvaluateException(Resources.format(Resources.Keys.CanNotReadSlice_1, lower + startAt), e);
        }
        return slice.render(locator.toSliceExtent(extent, lower));
    }
}
