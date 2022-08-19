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

import java.awt.image.RenderedImage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.SubspaceNotSpecifiedException;


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
     */
    private final GridCoverage[] slices;

    /**
     * The object for identifying indices in the {@link #slices} array.
     */
    private final GridSliceLocator locator;

    /**
     * Index of the first slice in {@link #locator}.
     */
    private final int startAt;

    /**
     * View over this grid coverage after conversion of sample values, or {@code null} if not yet created.
     * May be {@code this} if we determined that there is no conversion or the conversion is identity.
     *
     * @see #forConvertedValues(boolean)
     */
    private transient ConcatenatedGridCoverage convertedView;

    /**
     * Creates a new aggregated coverage.
     */
    ConcatenatedGridCoverage(final ConcatenatedGridResource source, final GridGeometry domain,
                             final GridCoverage[] slices, final int startAt)
    {
        super(domain, source.getSampleDimensions());
        this.slices  = slices;
        this.startAt = startAt;
        this.locator = source.locator;
    }

    /**
     * Creates a new aggregated coverage for the result of a conversion from/to package values.
     * This constructor assumes that all slices use the same sample dimensions.
     */
    private ConcatenatedGridCoverage(final ConcatenatedGridCoverage source, final GridCoverage[] slices) {
        super(source.getGridGeometry(), slices[0].getSampleDimensions());
        this.slices   = slices;
        this.startAt  = source.startAt;
        this.locator  = source.locator;
        convertedView = source;
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
    public synchronized GridCoverage forConvertedValues(final boolean converted) {
        if (convertedView == null) {
            boolean changed = false;
            final GridCoverage[] c = new GridCoverage[slices.length];
            for (int i=0; i<c.length; i++) {
                final GridCoverage source = slices[i];
                changed |= (c[i] = source.forConvertedValues(converted)) != source;
            }
            convertedView = changed ? new ConcatenatedGridCoverage(this, c) : this;
        }
        return convertedView;
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
        return slices[lower].render(locator.toSliceExtent(extent, lower));
    }
}
