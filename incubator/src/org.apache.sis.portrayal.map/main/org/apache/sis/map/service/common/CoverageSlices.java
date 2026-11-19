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
package org.apache.sis.map.service.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.sis.coverage.grid.DimensionalityReduction;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;


/**
 * The two-dimensional slices of a coverage read for the scene.
 * The two first grid dimensions are the horizontal ones. Other dimensions are either aggregated,
 * or taken at the middle of their extent.
 *
 * <p>A dimension is designated by a name equal, ignoring case, to the type of the grid axis
 * (e.g. {@code time} or {@code vertical}), or to the name or abbreviation of the corresponding axis
 * of the coordinate reference system.</p>
 *
 * @see "OGC CartoSym Part 4 (draft) section 8"
 *
 * @author Johann Sorel (Geomatys)
 */
public final class CoverageSlices {
    /**
     * Number of horizontal dimensions, which are the first ones.
     */
    private static final int HORIZONTAL = 2;

    /**
     * Position of the slice, relative to the extent, for the dimensions which are neither horizontal nor aggregated.
     */
    private static final double MIDDLE = 0.5;

    /**
     * The coverage, which may have more than two dimensions.
     */
    private final GridCoverage coverage;

    /**
     * The grid of the coverage.
     */
    private final GridGeometry grid;

    /**
     * Creates the slices of the given coverage.
     *
     * @param  coverage  the coverage read for the scene.
     */
    public CoverageSlices(final GridCoverage coverage) {
        this.coverage = coverage;
        grid = coverage.getGridGeometry();
    }

    /**
     * Returns the slice at the middle of all non-horizontal dimensions.
     *
     * @return the two-dimensional slice.
     */
    public GridCoverage middle() {
        return slice(middlePoint());
    }

    /**
     * Returns the slices for all positions along the given dimensions, other dimensions being at their middle.
     * If no dimension matches the given names, the only slice is the {@linkplain #middle() middle} one.
     *
     * @param  names  names of the dimensions to iterate over.
     * @return the two-dimensional slices.
     */
    public List<GridCoverage> along(final List<String> names) {
        final GridExtent extent = grid.getExtent();
        final List<Integer> dimensions = new ArrayList<>();
        for (int dim = HORIZONTAL; dim < extent.getDimension(); dim++) {
            for (final String name : names) {
                if (matches(dim, name.trim())) {
                    dimensions.add(dim);
                    break;
                }
            }
        }
        final List<GridCoverage> slices = new ArrayList<>();
        final long[] point = middlePoint();
        for (final int dim : dimensions) {
            point[dim] = extent.getLow(dim);
        }
        do {
            slices.add(slice(point));
        } while (increment(point, dimensions, extent));
        return slices;
    }

    /**
     * Moves the given point to the next position along the given dimensions, as an odometer.
     *
     * @return {@code false} if all positions have been visited.
     */
    private boolean increment(final long[] point, final List<Integer> dimensions, final GridExtent extent) {
        for (final int dim : dimensions) {
            if (point[dim] < extent.getHigh(dim)) {
                point[dim]++;
                return true;
            }
            point[dim] = extent.getLow(dim);
        }
        return false;
    }

    /**
     * Returns the grid coordinates of the middle of the extent.
     */
    private long[] middlePoint() {
        final GridExtent extent = grid.getExtent();
        final long[] point = new long[extent.getDimension()];
        for (int dim=0; dim<point.length; dim++) {
            point[dim] = extent.getRelative(dim, MIDDLE);
        }
        return point;
    }

    /**
     * Returns the two-dimensional slice containing the given point.
     */
    private GridCoverage slice(final long[] point) {
        if (point.length <= HORIZONTAL) {
            return coverage;
        }
        return DimensionalityReduction.select2D(grid).withSlicePoint(point).apply(coverage);
    }

    /**
     * Returns whether the given grid dimension has the given name.
     */
    private boolean matches(final int dimension, final String name) {
        final Optional<DimensionNameType> type = grid.getExtent().getAxisType(dimension);
        if (type.isPresent() && type.get().name().equalsIgnoreCase(name)) {
            return true;
        }
        if (grid.isDefined(GridGeometry.CRS)) {
            final CoordinateReferenceSystem crs = grid.getCoordinateReferenceSystem();
            if (crs.getCoordinateSystem().getDimension() == grid.getDimension()) {
                final CoordinateSystemAxis axis = crs.getCoordinateSystem().getAxis(dimension);
                return name.equalsIgnoreCase(axis.getName().getCode()) || name.equalsIgnoreCase(axis.getAbbreviation());
            }
        }
        return false;
    }
}
