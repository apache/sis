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
import java.util.HashMap;
import java.util.function.Function;
import org.opengis.util.InternationalString;
import org.opengis.metadata.spatial.DimensionNameType;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.util.iso.Types;


/**
 * Coordinates of slices together with search methods.
 *
 * @todo Bilinear search needs to be replaced by an R-Tree.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class GridSliceLocator {
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
     * Lows grid coordinates of each slice (inclusive) in the search dimension.
     * Values must be sorted in increasing order. Duplicated values may exist.
     *
     * @todo Replace by an R-Tree.
     */
    private final long[] sliceLows;

    /**
     * Highs grid coordinates of each slice (inclusive) in the search dimension.
     * Values are for slices in the same order as {@link #sliceLows}.
     * This is not <strong>not</strong> guaranteed to be sorted in increasing order.
     *
     * @todo Replace by an R-Tree.
     */
    private final long[] sliceHighs;

    /**
     * Translation from source coordinates of {@link GroupByTransform#gridToCRS} to grid coordinates
     * of {@link Gridslice#geometry}. Values are for slices in the same order as {@link #sliceLows}.
     */
    private final long[][] offsets;

    /**
     * Creates a new locator for slices at given coordinates.
     *
     * @param slices           descriptions of the grid resources to use as slices in a multi-dimensional cube.
     * @param searchDimension  the dimension on which the searches for grid slices are done.
     * @param resources        an array of initially null elements where to store the resources.
     */
    GridSliceLocator(final List<GridSlice> slices, final int searchDimension, final GridCoverageResource[] resources) {
        this.searchDimension = searchDimension;

        // TODO: use `parallelSort(…)` if https://bugs.openjdk.org/browse/JDK-8059093 is fixed.
        slices.sort((o1, o2) -> Long.compare(o1.getGridLow(searchDimension), o2.getGridLow(searchDimension)));

        sliceLows  = new long[resources.length];
        sliceHighs = new long[resources.length];
        offsets    = new long[resources.length][];
        final var shared = new HashMap<GridSlice,long[]>();
        for (int i=0; i<resources.length; i++) {
            final GridSlice  slice  = slices.get(i);
            final GridExtent extent = slice.getGridExtent();
            final long[]     offset = slice.getOffset(shared);
            final long       dimOff = offset[searchDimension];
            sliceLows [i] = Math.subtractExact(extent.getLow (searchDimension), dimOff);
            sliceHighs[i] = Math.subtractExact(extent.getHigh(searchDimension), dimOff);
            resources [i] = slice.resource;
            offsets   [i] = offset;
        }
    }

    /**
     * Creates a new grid geometry which is the union of all grid extent in the concatenated resource.
     *
     * @param  <E>     type of slice objects.
     * @param  base    base geometry to expand.
     * @param  slices  objects providing the grid extents.
     * @param  getter  getter method for getting the grid extents from slices.
     * @return expanded grid geometry.
     */
    final <E> GridGeometry union(final GridGeometry base, final List<E> slices, final Function<E,GridExtent> getter) {
        GridExtent extent = base.getExtent();
        final int dimension = extent.getDimension();
        final var axes = new DimensionNameType[dimension];
        final long[] low  = new long[dimension];
        final long[] high = new long[dimension];
        for (int i=0; i<dimension; i++) {
            axes[i] = extent.getAxisType(i).orElse(null);
            low [i] = extent.getLow (i);
            high[i] = extent.getHigh(i);
        }
        boolean changed = false;
        final int count = slices.size();
        for (int i=0; i<count; i++) {
            final GridExtent slice = getter.apply(slices.get(i));
            for (int j=0; j<dimension; j++) {
                final long offset = offsets[i][j];
                long v;
                if ((v = Math.subtractExact(slice.getLow (j), offset)) < low [j]) {low [j] = v; changed = true;}
                if ((v = Math.subtractExact(slice.getHigh(j), offset)) > high[j]) {high[j] = v; changed = true;}
            }
        }
        if (!changed) {
            return base;
        }
        extent = new GridExtent(axes, low, high, true);
        return new GridGeometry(extent, GridSlice.CELL_ANCHOR, base.getGridToCRS(GridSlice.CELL_ANCHOR),
                                base.isDefined(GridGeometry.CRS) ? base.getCoordinateReferenceSystem() : null);
    }

    /**
     * Returns the extent to use for querying a coverage from the slice at the given index.
     *
     * @param  extent  extent in units of aggregated grid coverage cells.
     * @param  slice   index of the slice to which to delegate an operation.
     * @return extent in units of the slice grid coverage.
     */
    final GridExtent toSliceExtent(final GridExtent extent, final int slice) {
        return extent.translate(offsets[slice]);
    }

    /**
     * Returns the index after the last slice which may intersect the given extent.
     *
     * @param sliceExtent  the extent to search.
     * @param fromIndex    index of the first slice to include in the search.
     * @param toIndex      index after the last slice to include in the search.
     */
    final int getUpper(final GridExtent sliceExtent, final int fromIndex, final int toIndex) {
        final long high = sliceExtent.getHigh(searchDimension);
        int upper = Arrays.binarySearch(sliceLows, fromIndex, toIndex, high);
        if (upper < 0) {
            upper = ~upper;         // Index of first slice that cannot intersect, because slice.low > high.
        } else {
            do upper++;
            while (upper < toIndex && sliceLows[upper] <= high);
        }
        return upper;
    }

    /**
     * Returns the index of the first slice which intersect the given extent.
     * This method performs a linear search. For better performance, it should be invoked
     * with {@code toIndex} parameter set to {@link #getUpper(GridExtent, int, int)} value.
     *
     * <h4>Limitations</h4>
     * Current implementation assumes that {@link #sliceHighs} are sorted in increasing order,
     * which is not guaranteed. For a robust search, we would need an R-Tree.
     *
     * @param sliceExtent  the extent to search.
     * @param fromIndex    index of the first slice to include in the search.
     * @param toIndex      index after the last slice to include in the search.
     */
    final int getLower(final GridExtent sliceExtent, final int fromIndex, int toIndex) {
        final long low = sliceExtent.getLow(searchDimension);
        while (toIndex > fromIndex) {
            if (sliceHighs[--toIndex] < low) {
                return toIndex + 1;
            }
        }
        return toIndex;
    }

    /**
     * Returns {@code true} if the grid extent in the search dimension is a slice of size 1.
     *
     * @param  sliceExtent  the extent to search.
     * @return whether the extent is a slice in the search dimension.
     */
    final boolean isSlice(final GridExtent sliceExtent) {
        return sliceExtent.getLow(searchDimension) == sliceExtent.getHigh(searchDimension);
    }

    /**
     * Returns the name of the extent axis in the search dimension.
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
