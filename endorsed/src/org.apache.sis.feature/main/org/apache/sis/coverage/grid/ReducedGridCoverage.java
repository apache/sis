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
package org.apache.sis.coverage.grid;

import java.util.Map;
import java.util.TreeMap;
import java.awt.image.RenderedImage;
import org.opengis.geometry.DirectPosition;
import org.apache.sis.util.collection.Containers;


/**
 * A grid coverage on which dimensionality reduction of the domain has been applied.
 * This is a reduction in the number of dimensions of the grid, which usually implies
 * a reduction in the number of dimensions of the CRS but not necessarily in same order.
 * The sample dimensions (coverage range) are unmodified.
 *
 * <p>This coverage is a <em>view</em>: changes in the source coverage are reflected
 * immediately in this {@code ReducedGridCoverage}, and conversely.</p>
 *
 * @author  Alexis Manin (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ReducedGridCoverage extends DerivedGridCoverage {
    /**
     * The dimensionality reduction to apply on the domain of the source coverage.
     */
    private final DimensionalityReduction reduction;

    /**
     * Creates a new reduced grid coverage.
     *
     * @param source     the source coverage.
     * @param reduction  the dimensionality reduction to apply on the source coverage.
     */
    ReducedGridCoverage(final GridCoverage source, final DimensionalityReduction reduction) {
        super(source, reduction.getReducedGridGeometry());
        this.reduction = reduction;
    }

    /**
     * Specifies that this coverage should not be replaced by its source.
     * This is because the number of grid dimensions is not the same.
     */
    @Override
    final boolean isNotRepleacable() {
        return true;
    }

    /**
     * Returns a two-dimensional slice of grid data as a rendered image.
     * This method delegates to the source coverage with a slice extent completed
     * with the dimensions that were hidden by this {@code ReducedGridCoverage}.
     *
     * @param  sliceExtent  a subspace of this grid coverage, or {@code null} for the whole coverage.
     * @return the grid slice as a rendered image. Image location is relative to {@code sliceExtent}.
     */
    @Override
    public RenderedImage render(final GridExtent sliceExtent) {
        return source.render(reduction.reverse(sliceExtent));
    }

    /**
     * Creates a new function for computing or interpolating sample values at given locations.
     * That function accepts {@link DirectPosition} in arbitrary Coordinate Reference System.
     * If a given position contains coordinate values for dimensions that were hidden by this
     * {@code ReducedGridCoverage}, the specified position coordinates will have precedence
     * over the dimensions that were specified in the {@link DimensionalityReduction} object.
     */
    @Override
    public Evaluator evaluator() {
        return new SliceEvaluator(source.evaluator());
    }

    /**
     * Implementation of the evaluator returned by {@link #evaluator()}.
     * This evaluator delegates to the source evaluator with default slices
     * initialized to the values specified by the {@link DimensionalityReduction}.
     */
    private final class SliceEvaluator extends EvaluatorWrapper {
        /**
         * The slice where to perform evaluation, or {@code null} if not yet computed.
         * This is the cached value of {@link #getDefaultSlice()}.
         */
        private Map<Integer, Long> slice;

        /**
         * Creates a new evaluator wrapping the given source coverage evaluator.
         *
         * @param  source  an evaluator newly created from the source coverage.
         */
        SliceEvaluator(final Evaluator source) {
            super(source);
            setDefaultSlice(null);
        }

        /**
         * Returns the coverage from which this evaluator is fetching sample values.
         * This is the coverage on which the {@link ReducedGridCoverage#evaluator()}
         * method has been invoked.
         */
        @Override
        public GridCoverage getCoverage() {
            return ReducedGridCoverage.this;
        }

        /**
         * Returns the default slice where to perform evaluation, or an empty map if unspecified.
         */
        @Override
        @SuppressWarnings("ReturnOfCollectionOrArrayField")     // Because the map is unmodifiable.
        public Map<Integer, Long> getDefaultSlice() {
            if (slice == null) {
                final var copy = new TreeMap<Integer, Long>();
                super.getDefaultSlice().forEach((dim, coord) -> {
                    dim = reduction.toReducedDimension(dim);
                    if (dim >= 0) copy.put(dim, coord);
                });
                slice = Containers.unmodifiable(copy);
            }
            return slice;
        }

        /**
         * Sets the default slice where to perform evaluation when the points do not have enough dimensions.
         *
         * @throws IllegalArgumentException if the map contains an illegal dimension or grid coordinate value.
         */
        @Override
        public void setDefaultSlice(Map<Integer, Long> slice) {
            if (slice == null) {
                GridGeometry origin = ReducedGridCoverage.this.source.getGridGeometry();
                slice = origin.getExtent().getSliceCoordinates();
            } else {
                final var copy = new TreeMap<Integer, Long>();
                slice.forEach((dim, coord) -> copy.put(reduction.toSourceDimension(dim), coord));
                slice = copy;
            }
            slice.putAll(reduction.getSliceCoordinates());
            super.setDefaultSlice(slice);
            this.slice = null;
        }
    }
}
