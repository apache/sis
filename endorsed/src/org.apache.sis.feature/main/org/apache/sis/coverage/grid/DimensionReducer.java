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

import java.util.Arrays;
import org.opengis.util.FactoryException;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.internal.shared.AxisDirections;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.ImmutableEnvelope;


/**
 * An helper class for reducing the number of dimensions in a grid geometry, an envelope or a position.
 * This is used when the Area Of Interest has more dimensions than the grid geometry, in which case the
 * transform from grid to <abbr>AOI</abbr> will fail if we do not discard the extra dimensions.
 *
 * <p>This class works on the <abbr>CRS</abbr> dimensions.
 * This is different than {@link DimensionalityReduction}, which works on grid dimensions.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class DimensionReducer {
    /**
     * The <abbr>CRS</abbr> dimensions to keep, or {@code null} for keeping them all.
     */
    private int[] dimensions;

    /**
     * The <abbr>CRS</abbr> with only the {@linkplain #dimensions} to keep, or {@code null} if no reduction.
     */
    private CoordinateReferenceSystem reducedCRS;

    /**
     * Requests to retain only the axes in the specified <abbr>CRS</abbr> dimensions.
     *
     * @param  dimensions  the <abbr>CRS</abbr> dimensions to keep, or {@code null} for keeping them all.
     */
    DimensionReducer(final int... dimensions) {
        this.dimensions = dimensions;
    }

    /**
     * Requests to retain only the {@code targetCRS} dimensions that are found in the base grid.
     * This will be used by caller for creating a valid {@code sourceCRS} to {@code targetCRS} transform.
     *
     * @param  base       the grid geometry which will be derived. Cannot be null.
     * @param  targetCRS  <abbr>CRS</abbr> of the area or point of interest. Cannot be null.
     */
    DimensionReducer(final GridGeometry base, final CoordinateReferenceSystem targetCRS) throws FactoryException {
        if (base != null && base.envelope != null) {
            final CoordinateReferenceSystem sourceCRS = base.envelope.getCoordinateReferenceSystem();
            if (sourceCRS != null) {
                final CoordinateSystem sourceCS = sourceCRS.getCoordinateSystem();
                final CoordinateSystem targetCS = targetCRS.getCoordinateSystem();
                if (sourceCS.getDimension() < targetCS.getDimension()) {
                    dimensions = AxisDirections.indicesOfLenientMapping(targetCS, sourceCS);
                    if (dimensions != null) {
                        Arrays.sort(dimensions);
                        reducedCRS = CRS.selectDimensions(targetCRS, dimensions);
                    }
                }
            }
        }
    }

    /**
     * Applies dimension reduction on the given array of resolutions.
     * If the resolution cannot be reduced, then it is returned as-is.
     */
    private double[] apply(final double[] target) {
        if (target == null || dimensions == null) {
            return target;
        }
        final var resolution = new double[dimensions.length];
        for (int i=0; i < resolution.length; i++) {
            resolution[i] = target[dimensions[i]];
        }
        return resolution;
    }

    /**
     * Applies dimension reduction on the given position.
     * If the position cannot be reduced, then it is returned as-is.
     */
    final DirectPosition apply(final DirectPosition target) {
        if (target == null || dimensions == null) {
            return target;
        }
        final var position = new GeneralDirectPosition(reducedCRS);
        for (int i=0; i < dimensions.length; i++) {
            position.coordinates[i] = target.getCoordinate(dimensions[i]);
        }
        return position;
    }

    /**
     * Applies dimension reduction on the given envelope.
     * If the envelope cannot be reduced, then it is returned as-is.
     */
    final Envelope apply(final Envelope target) {
        if (target == null || dimensions == null) {
            return target;
        }
        final DirectPosition lowerCorner = target.getLowerCorner();
        final DirectPosition upperCorner = target.getUpperCorner();
        final var envelope = new GeneralEnvelope(reducedCRS);
        for (int i=0; i < dimensions.length; i++) {
            final int s = dimensions[i];
            envelope.setRange(i, lowerCorner.getCoordinate(s), upperCorner.getCoordinate(s));
        }
        return envelope;
    }

    /**
     * Applies dimension reduction on the given grid geometry.
     * It may cause a reduction in the number of dimensions of the grid.
     * If the grid cannot be reduced, then it is returned as-is.
     */
    final GridGeometry apply(final GridGeometry target) throws FactoryException {
        if (target == null || dimensions == null) {
            return target;
        }
        /*
         * Select the dimension to keep in the "grid to CRS" transform. We repeat this operation for the
         * "corner to CRS" transform rather than deriving the latter from the former because we don't
         * know in advance which one of the two transforms is more accurate (has less NaN values).
         * This separation determines the dimensions to keep in the grid.
         */
        int[] gridIndices = null;
        MathTransform gridToCRS = target.gridToCRS;
        if (gridToCRS != null) {
            var ts = new TransformSeparator(gridToCRS);
            ts.addTargetDimensions(dimensions);
            gridToCRS = ts.separate();
            gridIndices = ts.getSourceDimensions();
        }
        MathTransform cornerToCRS = target.cornerToCRS;
        if (cornerToCRS != null) {
            var ts = new TransformSeparator(cornerToCRS);
            ts.addTargetDimensions(dimensions);
            if (gridIndices != null) {
                ts.addSourceDimensions(gridIndices);
                cornerToCRS = ts.separate();
            } else {
                cornerToCRS = ts.separate();
                gridIndices = ts.getSourceDimensions();
            }
        }
        /*
         * Reduces the number of dimensions of the extent, envelope and resolution.
         */
        GridExtent extent = target.extent;
        if (extent != null && gridIndices != null) {
            extent = extent.selectDimensions(gridIndices);
        }
        reducedCRS = CRS.selectDimensions(target.getCoordinateReferenceSystem(), dimensions);
        final Envelope envelope = apply(target.envelope);
        final double[] resolution = apply(target.resolution);
        long nonLinears = 0;
        for (int i=0; i < dimensions.length; i++) {
            if ((target.nonLinears & (1L << dimensions[i])) != 0) {
                nonLinears |= (1L << i);
            }
        }
        return new GridGeometry(extent, gridToCRS, cornerToCRS, ImmutableEnvelope.castOrCopy(envelope), resolution, nonLinears);
    }
}
