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
import java.awt.image.RenderedImage;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.CorruptedObjectException;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.image.internal.shared.ReshapedImage;


/**
 * A grid coverage for a subset of the data as the source coverage.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ClippedGridCoverage extends DerivedGridCoverage {
    /**
     * The coordinates of the slice, or {@code null} if same as the source coverage.
     *
     * @see #evaluator()
     */
    private final Map<Integer, Long> defaultSlice;

    /**
     * Constructs a new grid coverage which will delegate the rendering operation to the given source.
     * This coverage will take the same sample dimensions as the source.
     *
     * @param  source  the source to which to delegate rendering operations.
     * @param  domain  the grid extent, CRS and conversion from cell indices to CRS.
     */
    private ClippedGridCoverage(final GridCoverage source, final GridGeometry domain) {
        super(source, domain);
        final Map<Integer, Long> c = domain.getExtent().getSliceCoordinates();
        if (c.equals(source.getGridGeometry().getExtent().getSliceCoordinates())) {
            defaultSlice = null;
        } else {
            defaultSlice = Map.copyOf(c);
        }
    }

    /**
     * Returns a grid coverage clipped to the given extent.
     *
     * @param  source  the source to which to delegate rendering operations.
     * @param  clip    the clip to apply in units of source grid coordinates.
     * @return the clipped coverage. May be the {@code source} returned as-is.
     * @throws IncompleteGridGeometryException if the given coverage has no grid extent.
     * @throws DisjointExtentException if the given extent does not intersect the given coverage.
     * @throws IllegalArgumentException if axes of the given extent are inconsistent with the axes of the grid of the given coverage.
     */
    static GridCoverage create(GridCoverage source, final GridExtent clip, final boolean allowSourceReplacement) {
        GridGeometry gridGeometry = source.getGridGeometry();
        GridExtent extent = gridGeometry.getExtent();
        if (extent == (extent = extent.intersect(clip))) {
            return source;
        }
        if (allowSourceReplacement) {
            while (source instanceof ClippedGridCoverage) {
                source = ((ClippedGridCoverage) source).source;
                if (extent.equals(source.gridGeometry.extent)) {
                    return source;
                }
            }
        }
        try {
            gridGeometry = new GridGeometry(gridGeometry, extent, null);
        } catch (TransformException e) {
            // Unable to transform an envelope which was successfully transformed before.
            // If it happens, assume that something wrong happened with the objects.
            throw new CorruptedObjectException(e);
        }
        return new ClippedGridCoverage(source, gridGeometry);
    }

    /**
     * Returns a grid coverage that contains real values or sample values, depending if {@code converted}
     * is {@code true} or {@code false} respectively. This method delegates to the source and wraps the
     * result in a {@link ClippedGridCoverage} with the same extent.
     */
    @Override
    protected final GridCoverage createConvertedValues(final boolean converted) {
        final GridCoverage c = source.forConvertedValues(converted);
        return (c == source) ? this : new ClippedGridCoverage(c, gridGeometry);
    }

    /**
     * Returns a two-dimensional slice of grid data as a rendered image.
     */
    @Override
    public RenderedImage render(GridExtent sliceExtent) {
        final GridExtent clipped;
        if (sliceExtent == null) {
            clipped = sliceExtent = gridGeometry.extent;
        } else {
            clipped = sliceExtent.intersect(gridGeometry.extent);
        }
        final RenderedImage image = source.render(clipped);
        /*
         * After `render(…)` execution, the (minX, minY) image coordinates are the differences
         * between the extent that we requested and what we got. If the clipped extent that we
         * specified in above method call has an origin different than the user-supplied extent,
         * we need to adjust.
         */
        if (clipped != sliceExtent) {       // Slight optimization for a common case.
            long any = 0;
            final long[] translation = new long[clipped.getDimension()];
            for (int i=0; i<translation.length; i++) {
                any |= (translation[i] = Math.subtractExact(clipped.getLow(i), sliceExtent.getLow(i)));
            }
            if (any != 0) {
                final Object property = image.getProperty(PlanarImage.XY_DIMENSIONS_KEY);
                final int[] gridDimensions;
                if (property instanceof int[]) {
                    gridDimensions = (int[]) property;
                } else {
                    gridDimensions = clipped.getSubspaceDimensions(BIDIMENSIONAL);
                }
                final var t = new ReshapedImage(image, translation[gridDimensions[0]], translation[gridDimensions[1]]);
                return t.isIdentity() ? t.source : t;
            }
        }
        return image;
    }

    /**
     * Creates a new function for computing or interpolating sample values at given locations.
     * That function accepts {@link DirectPosition} in arbitrary Coordinate Reference System.
     * If a given position contains coordinate values for dimensions that were sliced by this
     * {@code ClippedGridCoverage}, the coordinates of the specified position will have precedence
     * over the grid extent that were specified in the {@link ClippedGridCoverage} constructor.
     */
    @Override
    public Evaluator evaluator() {
        Evaluator evaluator = source.evaluator();
        if (defaultSlice != null) {
            evaluator = new SliceEvaluator(evaluator);
        }
        return evaluator;
    }

    /**
     * Implementation of the evaluator returned by {@link #evaluator()}.
     * This evaluator delegates to the source evaluator with default slices
     * initialized to the values specified by {@link ClippedGridCoverage}.
     */
    private final class SliceEvaluator extends EvaluatorWrapper {
        /**
         * Creates a new evaluator wrapping the given source coverage evaluator.
         *
         * @param  source  an evaluator newly created from the source coverage.
         */
        SliceEvaluator(final GridCoverage.Evaluator source) {
            super(source);
            super.setDefaultSlice(defaultSlice);
        }

        /**
         * Returns the coverage from which this evaluator is fetching sample values.
         * This is the coverage on which the {@link ClippedGridCoverage#evaluator()}
         * method has been invoked.
         */
        @Override
        public GridCoverage getCoverage() {
            return ClippedGridCoverage.this;
        }

        /**
         * Sets the default slice where to perform evaluation when the points do not have enough dimensions.
         *
         * @throws IllegalArgumentException if the map contains an illegal dimension or grid coordinate value.
         */
        @Override
        public void setDefaultSlice(final Map<Integer, Long> slice) {
            super.setDefaultSlice(slice != null ? slice : defaultSlice);
        }
    }
}
