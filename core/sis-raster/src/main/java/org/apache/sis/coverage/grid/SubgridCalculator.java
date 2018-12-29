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
import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.CoordinateOperation;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.internal.referencing.DirectPositionView;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.internal.raster.Resources;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ArraysExt;


/**
 * Helper class for computing the grid extent of a sub-area of a given grid geometry.
 * This class provides the {@link MathTransform} converting source grid coordinates to target grid coordinates,
 * where the source is the given {@link GridGeometry} instance and the target is the sub-sampled grid.
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
     * The conversion from the original grid to the sub-sampled grid, or {@code null} if no sub-sampling is applied.
     * This is computed by the constructor.
     */
    MathTransform toSubsampled;

    /**
     * Computes the sub-grid over the given area of interest with the given resolution.
     * At least one of {@code areaOfInterest} and {@code resolution} shall be non-null.
     * It is caller's responsibility to ensure that {@link GridGeometry#extent} is non-null.
     *
     * @param  grid            the enclosing grid geometry (mandatory).
     * @param  cornerToCRS     the transform from cell corners to grid CRS (mandatory).
     * @param  areaOfInterest  the desired spatiotemporal region in any CRS, or {@code null} for the whole area.
     * @param  resolution      the desired resolution in the same units and order than the axes of the AOI envelope,
     *                         or {@code null} or an empty array if no sub-sampling is desired.
     * @throws TransformException if an error occurred while converting the envelope coordinates to grid coordinates.
     */
    SubgridCalculator(final GridGeometry grid, MathTransform cornerToCRS, final Envelope areaOfInterest, double[] resolution)
            throws TransformException
    {
        /*
         * List of grid dimensions that are modified by the 'cornerToCRS' transform, or null for all dimensions.
         * The length of this array is the number of dimensions of the given Area Of Interest (AOI). Each value
         * in this array is between 0 inclusive and 'extent.getDimension()' exclusive.
         */
        int[] modifiedDimensions = null;
        try {
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
            final int dimension = cornerToCRS.getTargetDimensions();
            ArgumentChecks.ensureDimensionMatches("areaOfInterest", dimension, areaOfInterest);
            if (dimension < cornerToCRS.getSourceDimensions()) {
                final TransformSeparator sep = new TransformSeparator(cornerToCRS);
                sep.setTrimSourceDimensions(true);
                cornerToCRS = sep.separate();
                modifiedDimensions = sep.getSourceDimensions();
                if (modifiedDimensions.length != dimension) {
                    throw new TransformException(Resources.format(Resources.Keys.CanNotMapToGridDimensions));
                }
            }
        } catch (FactoryException e) {
            throw new TransformException(Resources.format(Resources.Keys.CanNotMapToGridDimensions), e);
        }
        /*
         * Compute the sub-extent for the given Area Of Interest (AOI), ignoring for now the sub-sampling.
         * If no area of interest has been specified, or if the result is identical to the original extent,
         * then we will keep the reference to the original GridExtent (i.e. we share existing instances).
         */
        extent = grid.extent;
        final int dimension = extent.getDimension();
        GeneralEnvelope indices = null;
        if (areaOfInterest != null) {
            indices = Envelopes.transform(cornerToCRS.inverse(), areaOfInterest);
            setExtent(indices, extent, modifiedDimensions);
        }
        if (indices == null || indices.getDimension() != dimension) {
            indices = new GeneralEnvelope(dimension);
        }
        for (int i=0; i<dimension; i++) {
            indices.setRange(i, extent.getLow(i), extent.getHigh(i) + 1.0);
        }
        /*
         * Convert the target resolutions to grid cell sub-samplings and adjust the extent consequently.
         * We perform this conversion by handling the resolution has a small translation vector located
         * at the point of interest, and converting it to a translation vector in grid coordinates. The
         * conversion is done by a multiplication with the "CRS to grid" derivative at that point.
         *
         * The sub-sampling will be rounded in such a way that the difference in grid size is less than
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
             * If at least one sub-sampling is effective, build a scale from the old grid coordinates to the new
             * grid coordinates. If we had no rounding, the conversion would be only a scale. But because of rounding,
             * we need a small translation for the difference between the "real" coordinate and the integer coordinate.
             */
            if (modified) {
                final GridExtent unscaled = extent;
                setExtent(indices, null, null);
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
     * Sets {@link #extent} to the given envelope, rounded to nearest integers.
     *
     * @param  indices             the envelope to use for setting the grid extent.
     * @param  enclosing           the enclosing grid extent if a sub-sampling is not yet applied, {@code null} otherwise.
     * @param  modifiedDimensions  if {@code enclosing} is non-null, the grid dimensions to set from the envelope.
     */
    private void setExtent(final GeneralEnvelope indices, final GridExtent enclosing, final int[] modifiedDimensions) {
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
