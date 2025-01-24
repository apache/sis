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

import java.util.Arrays;
import org.opengis.util.InternationalString;
import org.opengis.metadata.spatial.DimensionNameType;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.iso.Types;


/**
 * Coordinates of slices together with search methods.
 * An instance of {@code GridSliceLocator} is retained for the lifetime of {@link ConcatenatedGridResource}.
 *
 * @todo Bilinear search needs to be replaced by an R-Tree.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class GridSliceLocator {
    /**
     * The slices contained in the aggregated resource.
     * This array shall be considered as read-only.
     */
    final GridSlice[] slices;

    /**
     * The grid geometry of the aggregated resource which is using this locator.
     * The {@link #sliceLows}, {@link #sliceHighs} and {@link GridSlice#offset}
     * arrays are expressed in units of cells of this grid geometry.
     *
     * @see ConcatenatedGridResource#getGridGeometry()
     */
    final GridGeometry gridGeometry;

    /**
     * The dimension on which the searches are done.
     *
     * @todo If we replace bilinear search by an R-Tree, then it would be an array of all dimensions returned
     *       by {@code findConcatenatedDimensions()}. Only those dimensions need to be managed by an R-Tree.
     *
     * @see GroupByTransform#findConcatenatedDimensions()
     */
    final int searchDimension;

    /**
     * Low grid coordinates of each slice (inclusive) in the search dimension.
     * Values must be sorted in increasing order. Duplicated values may exist.
     *
     * @todo Replace by an R-Tree.
     *
     * @see GroupByTransform#compare(GridSlice, GridSlice)
     */
    private final long[] sliceLows;

    /**
     * High grid coordinates of each slice (inclusive) in the search dimension.
     * Values are for slices in the same order as {@link #sliceLows}.
     *
     * @todo Replace by an R-Tree.
     */
    private final long[] sliceHighs;

    /**
     * Creates a new locator for the given slices.
     * The {@link #gridGeometry} will be the union of all grid extents in the concatenated resource.
     *
     * <h4>Prerequisites</h4>
     * All resources shall have the same "grid to CRS" transform, ignoring the translation terms.
     * This is not verified by this constructor.
     *
     * @param domain           base geometry to expand to the union of the grid geometry of all resources.
     * @param slices           descriptions of the grid resources to use as slices in a multi-dimensional cube.
     * @param searchDimension  the dimension on which the searches for grid slices are done.
     */
    GridSliceLocator(GridGeometry domain, final GridSlice[] selected, final int searchDimension) {
        this.searchDimension = searchDimension;
        this.slices          = selected;
        this.sliceLows       = new long[selected.length];
        this.sliceHighs      = new long[selected.length];
        GridExtent aoi = domain.getExtent();
        final int dimension = aoi.getDimension();
        final var axes = new DimensionNameType[dimension];
        final var low  = new long[dimension];
        final var high = new long[dimension];
        for (int i=0; i<dimension; i++) {
            axes[i] = aoi.getAxisType(i).orElse(null);
            low [i] = Long.MAX_VALUE;
            high[i] = Long.MIN_VALUE;
        }
        for (int i=0; i<slices.length; i++) {
            final GridSlice  slice  = slices[i];
            final GridExtent extent = slice.extentInGroup;
            sliceLows [i] = extent.getLow (searchDimension);
            sliceHighs[i] = extent.getHigh(searchDimension);
            for (int j=0; j<dimension; j++) {
                low [j] = Math.min(low [j], extent.getLow (j));
                high[j] = Math.max(high[j], extent.getHigh(j));
            }
        }
        if (!aoi.equals(aoi = new GridExtent(axes, low, high, true))) {
            var crs = domain.isDefined(GridGeometry.CRS) ? domain.getCoordinateReferenceSystem() : null;
            domain = new GridGeometry(aoi, GridSlice.CELL_ANCHOR, domain.getGridToCRS(GridSlice.CELL_ANCHOR), crs);
        }
        gridGeometry = domain;
    }

    /**
     * Returns the indexes of the slices that intersect the given extent.
     *
     * @param  request  the extent to search.
     * @return indexes of slices that intersect the given extent.
     */
    final int[] find(final GridExtent request) {
        /*
         * Find by elimination: slices cannot intersect if:
         *
         *   • slice.low  > request.high   — `upper` is the index of the first such slice.
         *   • slice.high < request.low    — `lower` is the index after the last such slice.
         */
        int lower;
        final long high = request.getHigh(searchDimension);
        int upper = Arrays.binarySearch(sliceLows, high);
        if (upper < 0) {
            lower = upper = ~upper;     // Tild, not minus.
        } else {
            lower = upper;
            while (++upper < sliceLows.length) {
                if (sliceLows[upper] > high) break;
            }
        }
        final long low = request.getLow(searchDimension);
        lower = Arrays.binarySearch(sliceHighs, 0, lower, low);
        if (lower < 0) {
            lower = ~lower;
        } else {
            while (--lower >= 0) {
                if (sliceHighs[lower] < low) break;
            }
            lower++;
        }
        int count = 0;
        final var selection = new int[upper - lower];
        for (int i=lower; i<upper; i++) {
            final GridSlice slice = slices[i];
            if (request.intersects(slice.extentInGroup)) {
                selection[count++] = i;
            }
        }
        return ArraysExt.resize(selection, count);
    }

    /**
     * Returns the name of the extent axis in the search dimension.
     * Used for formatting error messages in exceptions.
     *
     * @param  extent  the extent from which to get an axis label.
     * @return label for the search axis.
     */
    final String getDimensionName(final GridExtent extent) {
        return extent.getAxisType(searchDimension)
                .map(Types::getCodeTitle).map(InternationalString::toString)
                .orElseGet(() -> String.valueOf(searchDimension));
    }
}
