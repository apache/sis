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

import java.awt.image.RenderedImage;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.CorruptedObjectException;
import org.apache.sis.image.PlanarImage;
import org.apache.sis.image.privy.ReshapedImage;


/**
 * A grid coverage for a subset of the data as the source coverage.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ClippedGridCoverage extends DerivedGridCoverage {
    /**
     * Constructs a new grid coverage which will delegate the rendering operation to the given source.
     * This coverage will take the same sample dimensions as the source.
     *
     * @param  source  the source to which to delegate rendering operations.
     * @param  domain  the grid extent, CRS and conversion from cell indices to CRS.
     */
    private ClippedGridCoverage(final GridCoverage source, final GridGeometry domain) {
        super(source, domain);
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
        final GridCoverage cs = source.forConvertedValues(converted);
        return (cs == source) ? this : new ClippedGridCoverage(cs, gridGeometry);
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
                    gridDimensions = clipped.getSubspaceDimensions(GridCoverage2D.BIDIMENSIONAL);
                }
                final var t = new ReshapedImage(image, translation[gridDimensions[0]], translation[gridDimensions[1]]);
                return t.isIdentity() ? t.source : t;
            }
        }
        return image;
    }
}
