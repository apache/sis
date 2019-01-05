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

import org.opengis.geometry.Envelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.CRS;
import org.apache.sis.internal.referencing.DirectPositionView;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.raster.Resources;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;

// Branch-dependent imports
import org.opengis.coverage.PointOutsideCoverageException;


/**
 * Helper class for computing the grid extent of a sub-area of a given grid geometry.
 * This class provides the {@link MathTransform} converting source grid coordinates to target grid coordinates,
 * where the source is the given {@link GridGeometry} instance and the target is the subsampled grid.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
final class SubgridCalculator {
    /**
     * The sub-extent computed by the constructor.
     */
    GridExtent extent;

    /**
     * The conversion from the original grid to the subsampled grid, or {@code null} if no subsampling is applied.
     * This is computed by the constructor.
     */
    MathTransform toSubsampled;

    /**
     * List of grid dimensions that are modified by the {@code cornerToCRS} transform, or null for all dimensions.
     * The length of this array is the number of dimensions of the given Area Of Interest (AOI). Each value in this
     * array is between 0 inclusive and {@code extent.getDimension()} exclusive.
     */
    private int[] modifiedDimensions;

    /**
     * Computes the sub-grid for a slice at the given slice point. The given position can be given in any CRS.
     * The position should not define a coordinate for all dimensions, otherwise the sub-grid would degenerate
     * to a single point. Dimensions can be left unspecified either by assigning to the position a CRS without
     * those dimensions, or by assigning the NaN value to some coordinates.
     *
     * @param  grid         the enclosing grid geometry (mandatory).
     * @param  cornerToCRS  the transform from cell corners to grid CRS (mandatory).
     * @param  slicePoint   the coordinates where to get a slice.
     * @throws TransformException if an error occurred while converting the envelope coordinates to grid coordinates.
     * @throws PointOutsideCoverageException if the given point is outside the grid extent.
     */
    SubgridCalculator(final GridGeometry grid, MathTransform cornerToCRS, final DirectPosition slicePoint)
            throws TransformException
    {
        try {
            if (grid.envelope != null) {
                final CoordinateReferenceSystem sourceCRS = grid.envelope.getCoordinateReferenceSystem();
                if (sourceCRS != null) {
                    final CoordinateReferenceSystem targetCRS = slicePoint.getCoordinateReferenceSystem();
                    if (targetCRS != null) {
                        final CoordinateOperation operation = CRS.findOperation(sourceCRS, targetCRS, null);
                        cornerToCRS = MathTransforms.concatenate(cornerToCRS, operation.getMathTransform());
                    }
                }
            }
            final int dimension = cornerToCRS.getTargetDimensions();
            ArgumentChecks.ensureDimensionMatches("slicePoint", dimension, slicePoint);
            cornerToCRS = dropUnusedDimensions(cornerToCRS, dimension);
        } catch (FactoryException e) {
            throw new TransformException(Resources.format(Resources.Keys.CanNotMapToGridDimensions), e);
        }
        extent = grid.extent.slice(cornerToCRS.inverse().transform(slicePoint, null), modifiedDimensions);
    }

    /**
     * Computes the sub-grid over the given area of interest with the given resolution.
     * At least one of {@code areaOfInterest} and {@code resolution} shall be non-null.
     * It is caller's responsibility to ensure that {@link GridGeometry#extent} is non-null.
     *
     * @param  grid            the enclosing grid geometry (mandatory).
     * @param  cornerToCRS     the transform from cell corners to grid CRS (mandatory).
     * @param  areaOfInterest  the desired spatiotemporal region in any CRS, or {@code null} for the whole area.
     * @param  resolution      the desired resolution in the same units and order than the axes of the AOI envelope,
     *                         or {@code null} or an empty array if no subsampling is desired.
     * @throws TransformException if an error occurred while converting the envelope coordinates to grid coordinates.
     */
    SubgridCalculator(final GridGeometry grid, MathTransform cornerToCRS, final Envelope areaOfInterest, double[] resolution)
            throws TransformException, FactoryException
    {
        /*
         * If the envelope CRS is different than the expected CRS, concatenate the envelope transformation
         * to the 'gridToCRS' transform.  We should not transform the envelope here - only concatenate the
         * transforms - because transforming envelopes twice would add errors.
         */
        final CoordinateOperation operation = Envelopes.findOperation(grid.envelope, areaOfInterest);
        if (operation != null) {
            cornerToCRS = MathTransforms.concatenate(cornerToCRS, operation.getMathTransform());
        }
        /*
         * If the envelope dimensions does not encompass all grid dimensions, the envelope is probably non-invertible.
         * We need to reduce the number of grid dimensions in the transform for having a one-to-one relationship.
         */
        int dimension = cornerToCRS.getTargetDimensions();
        ArgumentChecks.ensureDimensionMatches("areaOfInterest", dimension, areaOfInterest);
        cornerToCRS = dropUnusedDimensions(cornerToCRS, dimension);
        /*
         * Compute the sub-extent for the given Area Of Interest (AOI), ignoring for now the subsampling.
         * If no area of interest has been specified, or if the result is identical to the original extent,
         * then we will keep the reference to the original GridExtent (i.e. we share existing instances).
         */
        extent = grid.extent;
        dimension = extent.getDimension();
        GeneralEnvelope indices = null;
        if (areaOfInterest != null) {
            indices = Envelopes.transform(cornerToCRS.inverse(), areaOfInterest);
            setExtent(indices, extent);
        }
        if (indices == null || indices.getDimension() != dimension) {
            indices = new GeneralEnvelope(dimension);
        }
        for (int i=0; i<dimension; i++) {
            indices.setRange(i, extent.getLow(i), extent.getHigh(i) + 1.0);
        }
        /*
         * Convert the target resolutions to grid cell subsamplings and adjust the extent consequently.
         * We perform this conversion by handling the resolution has a small translation vector located
         * at the point of interest, and converting it to a translation vector in grid coordinates. The
         * conversion is done by a multiplication with the "CRS to grid" derivative at that point.
         *
         * The subsampling will be rounded in such a way that the difference in grid size is less than
         * one half of cell. Demonstration:
         *
         *    e = Math.getExponent(span)     →    2^e ≦ span
         *    a = e+1                        →    2^a > span     →    1/2^a < 1/span
         *   Δs = (s - round(s)) / 2^a
         *   (s - round(s)) ≦ 0.5            →    Δs  ≦  0.5/2^a  <  0.5/span
         *   Δs < 0.5/span                   →    Δs⋅span < 0.5 cell.
         */
        if (resolution != null && resolution.length != 0) {
            resolution = ArraysExt.resize(resolution, cornerToCRS.getTargetDimensions());
            final int[] modifiedDimensions = this.modifiedDimensions;                     // Will not change anymore.
            Matrix m = cornerToCRS.derivative(new DirectPositionView.Double(getPointOfInterest(modifiedDimensions)));
            resolution = Matrices.inverse(m).multiply(resolution);
            boolean modified = false;
            for (int k=0; k<resolution.length; k++) {
                double s = Math.abs(resolution[k]);
                if (s > 1) {                                // Also for skipping NaN values.
                    final int i = (modifiedDimensions != null) ? modifiedDimensions[k] : k;
                    final int accuracy = Math.max(0, Math.getExponent(indices.getSpan(i))) + 1;         // Power of 2.
                    s = Math.scalb(Math.rint(Math.scalb(s, accuracy)), -accuracy);
                    indices.setRange(i, indices.getLower(i) / s, indices.getUpper(i) / s);
                    modified = true;
                }
                resolution[k] = s;
            }
            /*
             * If at least one subsampling is effective, build a scale from the old grid coordinates to the new
             * grid coordinates. If we had no rounding, the conversion would be only a scale. But because of rounding,
             * we need a small translation for the difference between the "real" coordinate and the integer coordinate.
             */
            if (modified) {
                final GridExtent unscaled = extent;
                setExtent(indices, null);
                m = Matrices.createIdentity(dimension + 1);
                for (int k=0; k<resolution.length; k++) {
                    final double s = resolution[k];
                    if (s > 1) {                            // Also for skipping NaN values.
                        final int i = (modifiedDimensions != null) ? modifiedDimensions[k] : k;
                        m.setElement(i, i, s);
                        m.setElement(i, dimension, unscaled.getLow(i) - extent.getLow(i) * s);
                    }
                }
                toSubsampled = MathTransforms.linear(m);
            }
        }
    }

    /**
     * Drops the source dimensions that are not needed for producing the target dimensions.
     * The retained source dimensions are stored in {@link #modifiedDimensions}.
     * This method is invoked in an effort to make the transform invertible.
     *
     * @param  cornerToCRS  transform from grid coordinates to AOI coordinates.
     * @param  dimension    value of {@code cornerToCRS.getTargetDimensions()}.
     */
    private MathTransform dropUnusedDimensions(MathTransform cornerToCRS, final int dimension)
            throws FactoryException, TransformException
    {
        if (dimension < cornerToCRS.getSourceDimensions()) {
            final TransformSeparator sep = new TransformSeparator(cornerToCRS);
            sep.setTrimSourceDimensions(true);
            cornerToCRS = sep.separate();
            modifiedDimensions = sep.getSourceDimensions();
            if (modifiedDimensions.length != dimension) {
                throw new TransformException(Resources.format(Resources.Keys.CanNotMapToGridDimensions));
            }
        }
        return cornerToCRS;
    }

    /**
     * Sets {@link #extent} to the given envelope, rounded to nearest integers.
     *
     * @param  indices    the envelope to use for setting the grid extent.
     * @param  enclosing  the enclosing grid extent if a subsampling is not yet applied, {@code null} otherwise.
     */
    private void setExtent(final GeneralEnvelope indices, final GridExtent enclosing) {
        final GridExtent sub = new GridExtent(indices, GridRoundingMode.NEAREST, null, enclosing, modifiedDimensions);
        if (!sub.equals(extent)) {
            extent = sub;
        }
    }

    /**
     * Returns the point of interest of current {@link #extent}.
     */
    private double[] getPointOfInterest(final int[] modifiedDimensions) {
        final double[] pointOfInterest = extent.getPointOfInterest();
        if (modifiedDimensions == null) {
            return pointOfInterest;
        }
        final double[] filtered = new double[modifiedDimensions.length];
        for (int i=0; i<filtered.length; i++) {
            filtered[i] = pointOfInterest[modifiedDimensions[i]];
        }
        return filtered;
    }
}
