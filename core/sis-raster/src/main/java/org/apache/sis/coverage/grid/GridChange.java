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
import java.util.Locale;
import java.io.Serializable;
import org.opengis.util.FactoryException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Information about the conversion from one grid geometry to another grid geometry.
 * This class holds the {@link MathTransform}s converting cell coordinates from the
 * source grid to cell coordinates in the target grid, together with the grid extent
 * that results from this conversion.
 *
 * <div class="note"><b>Usage:</b>
 * This class can be helpful for implementation of
 * {@link org.apache.sis.storage.GridCoverageResource#read(GridGeometry, int...)}.
 * Example:
 *
 * {@preformat java
 *     class MyDataStorage extends GridCoverageResource {
 *         &#64;Override
 *         public GridCoverage read(GridGeometry domain, int... range) throws DataStoreException {
 *             GridChange change = new GridChange(domain, getGridGeometry());
 *             GridExtent toRead = change.getTargetRange();
 *             int[] subsampling = change.getTargetStrides());
 *             // Do reading here.
 *         }
 *     }
 * }
 * </div>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class GridChange implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7819047186271172885L;

    /**
     * The conversions from source grid to target grid, mapping pixel corners or pixel centers.
     */
    private final MathTransform mapCorners, mapCenters;

    /**
     * Intersection of the two grid geometry extents, in units of the target grid geometry.
     * This is the expected ranges after conversions from source grid coordinates to target
     * grid coordinates, clipped to target extent and ignoring {@linkplain #scales}.
     *
     * @see #getTargetRange()
     */
    private final GridExtent targetRange;

    /**
     * An estimation of the multiplication factors when converting cell coordinates from source
     * grid to target grid. Those factors appear in the order of <em>target</em> grid axes.
     * May be {@code null} if the conversion is identity.
     *
     * @see #getTargetStrides()
     */
    private final double[] scales;

    /**
     * Creates a description of the conversion from given source grid geometry to given target grid geometry.
     * The following information are mandatory:
     * <ul>
     *   <li>Source {@linkplain GridGeometry#getExtent() extent}.</li>
     *   <li>Source "{@linkplain GridGeometry#getGridToCRS(PixelInCell) grid to CRS}" conversion.</li>
     *   <li>Target "grid to CRS" conversion.</li>
     * </ul>
     *
     * The following information are optional but recommended:
     * <ul>
     *   <li>Source coordinate reference system.</li>
     *   <li>Target {@linkplain GridGeometry#getCoordinateReferenceSystem() coordinate reference system}.</li>
     *   <li>Target {@linkplain GridGeometry#getExtent() extent}.</li>
     * </ul>
     *
     * @param  source  the source grid geometry.
     * @param  target  the target grid geometry.
     * @throws IncompleteGridGeometryException if a "grid to CRS" conversion is not defined,
     *         or if the {@code source} does not specify an {@linkplain GridGeometry#getExtent() extent}.
     * @throws TransformException if an error occurred during conversion from source to target.
     */
    public GridChange(final GridGeometry source, final GridGeometry target) throws TransformException {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("target", target);
        if (source.equals(target)) {
            // Optimization for a common case.
            mapCorners = mapCenters = MathTransforms.identity(source.getDimension());
            targetRange = source.getExtent();
        } else {
            final CoordinateOperation crsChange;
            try {
                crsChange = Envelopes.findOperation(source.envelope, target.envelope);
            } catch (FactoryException e) {
                throw new TransformException(Errors.format(Errors.Keys.CanNotTransformEnvelope), e);
            }
            final GridExtent domain = source.getExtent();
            mapCorners  = path(source, crsChange, target, PixelInCell.CELL_CORNER);
            mapCenters  = path(source, crsChange, target, PixelInCell.CELL_CENTER);
            targetRange = new GridExtent(domain.toCRS(mapCorners, mapCenters), target.extent, null);
            /*
             * Get an estimation of the scale factors when converting from source to target.
             * If all scale factors are 1, we will not store the array for consistency with
             * above block for identity case.
             */
            final Matrix matrix = MathTransforms.getMatrix(mapCenters);
            final double[] resolution;
            if (matrix != null) {
                resolution = GridGeometry.resolution(matrix, 1);
            } else {
                resolution = GridGeometry.resolution(mapCenters.derivative(domain.getCentroid()), 0);
            }
            for (int i=resolution.length; --i >= 0;) {
                if (resolution[i] != 1) {
                    scales = resolution;
                    return;
                }
            }
        }
        scales = null;
    }

    /**
     * Returns the concatenation of all transformation steps from the given source to the given target.
     *
     * @param  source     the source grid geometry.
     * @param  crsChange  the change of coordinate reference system, or {@code null} if none.
     * @param  target     the target grid geometry.
     * @param  anchor     whether we want the transform for pixel corner or pixel center.
     */
    private static MathTransform path(final GridGeometry source, final CoordinateOperation crsChange,
            final GridGeometry target, final PixelInCell anchor) throws NoninvertibleTransformException
    {
        MathTransform step1 = source.getGridToCRS(anchor);
        MathTransform step2 = target.getGridToCRS(anchor);
        if (crsChange != null) {
            step1 = MathTransforms.concatenate(step1, crsChange.getMathTransform());
        }
        if (step1.equals(step2)) {                                          // Optimization for a common case.
            return MathTransforms.identity(step1.getSourceDimensions());
        } else {
            return MathTransforms.concatenate(step1, step2.inverse());
        }
    }

    /**
     * Returns an <em>estimation</em> of the scale factor when converting source grid coordinates
     * to target grid coordinates. This is for information purpose only since this method combines
     * potentially different scale factors for all dimensions.
     *
     * @return an <em>estimation</em> of the scale factor for all dimensions.
     */
    public double getGlobalScale() {
        if (scales != null) {
            double sum = 0;
            int count = 0;
            for (final double value : scales) {
                if (Double.isFinite(value)) {
                    sum += value;
                    count++;
                }
            }
            if (count != 0) {
                return sum / count;
            }
        }
        return 1;
    }

    /**
     * Returns the conversion from source grid coordinates to target grid coordinates.
     * The {@code anchor} argument specifies whether the conversion maps cell centers
     * or cell corners of both grids.
     *
     * @param  anchor  the cell part to map (center or corner).
     * @return conversion from source grid coordinates to target grid coordinates.
     *
     * @see GridGeometry#getGridToCRS(PixelInCell)
     */
    public MathTransform getConversion(final PixelInCell anchor) {
        if (PixelInCell.CELL_CENTER.equals(anchor)) {
            return mapCenters;
        } else if (PixelInCell.CELL_CORNER.equals(anchor)) {
            return mapCorners;
        }  else {
            return PixelTranslation.translate(mapCenters, PixelInCell.CELL_CENTER, anchor);
        }
    }

    /**
     * Returns the intersection of the two grid geometry extents, in units of the target grid cells.
     * This is the expected ranges of grid coordinates after conversions from source to target grid,
     * clipped to target grid extent and ignoring {@linkplain #getTargetStrides() strides}.
     *
     * @return intersection of grid geometry extents in units of target cells.
     */
    public GridExtent getTargetRange() {
        return targetRange;
    }

    /**
     * Returns an <em>estimation</em> of the steps for accessing cells along each axis of target range.
     * Given a {@linkplain #getConversion(PixelInCell) conversion} from source grid coordinates
     * (<var>x</var>, <var>y</var>, <var>z</var>) to target grid coordinates
     * (<var>x′</var>, <var>y′</var>, <var>z′</var>) defined as below (generalize to as many dimensions as needed):
     *
     * <ul>
     *   <li><var>x′</var> = s₀⋅<var>x</var></li>
     *   <li><var>y′</var> = s₁⋅<var>y</var></li>
     *   <li><var>z′</var> = s₂⋅<var>z</var></li>
     * </ul>
     *
     * Then this method returns {|s₀|, |s₁|, |s₂|} rounded toward zero and clamped to 1
     * (i.e. all values in the returned array are strictly positive, no zero values).
     * It means that an iteration over source grid coordinates with a step Δ<var>x</var>=1
     * corresponds approximately to an iteration in target grid coordinates with a step of Δ<var>x′</var>=s₀,
     * a step Δ<var>y</var>=1 corresponds approximately to a step Δ<var>y′</var>=s₁, <i>etc.</i>
     * If the conversion changes grid axis order, then the order of elements in the returned array
     * is the order of axes in the {@linkplain #getTargetRange() target range}.
     *
     * <p>In a <em>inverse</em> conversion from target to source grid, the value returned by this
     * method would be the sub-sampling to apply while reading the target grid.</p>
     *
     * @return an <em>estimation</em> of the steps for accessing cells along each axis of target range.
     */
    public int[] getTargetStrides() {
        final int[] subsamplings;
        if (scales == null) {
            subsamplings = new int[targetRange.getDimension()];
            Arrays.fill(subsamplings, 1);
        } else {
            subsamplings = new int[scales.length];
            for (int i=0; i<subsamplings.length; i++) {
                subsamplings[i] = Math.max(1, (int) Math.nextUp(scales[i]));    // Really want rounding toward 0.
            }
        }
        return subsamplings;
    }

    /**
     * Returns a hash code value for this grid change.
     *
     * @return hash code value.
     */
    @Override
    public int hashCode() {
        /*
         * The mapCorners is closely related to mapCenters, so we omit it.
         * The scales array is derived from mapCenters, so we omit it too.
         */
        return mapCorners.hashCode() + 59 * targetRange.hashCode();
    }

    /**
     * Compares this grid change with the given object for equality.
     *
     * @param  other  the other object to compare with this grid change.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other != null && other.getClass() == getClass()) {
            final GridChange that = (GridChange) other;
            return mapCorners.equals(that.mapCorners) &&
                   mapCenters.equals(that.mapCenters) &&
                   targetRange.equals(that.targetRange) &&
                   Arrays.equals(scales, that.scales);
        }
        return false;
    }

    /**
     * Returns a string representation of this grid change for debugging purpose.
     *
     * @return a string representation for debugging purpose.
     */
    @Override
    public String toString() {
        final String lineSeparator = System.lineSeparator();
        final StringBuilder buffer = new StringBuilder(256)
                .append("Grid change").append(lineSeparator)
                .append("└ Scale factor ≈ ").append((float) getGlobalScale()).append(lineSeparator)
                .append("Target range").append(lineSeparator);
        targetRange.appendTo(buffer, Vocabulary.getResources((Locale) null), true);
        return buffer.toString();
    }
}
