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
package org.apache.sis.referencing.operation.builder;

import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.InterpolatedTransform;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.datum.DatumShiftGrid;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.util.ArgumentChecks;


/**
 * Creates an "almost linear" transform mapping the given source points to the given target points.
 * The transform is backed by a <cite>grid of localization</cite>, a two-dimensional array of coordinate points.
 * Grid size is {@code width} × {@code height} and input coordinates are (<var>i</var>,<var>j</var>) index in the grid,
 * where <var>i</var> must be in the [0…{@code width}-1] range and <var>j</var> in the [0…{@code height}-1] range inclusive.
 * Output coordinates are the values stored in the grid of localization at the specified index.
 * After a {@code LocalizationGridBuilder} instance has been fully populated (i.e. real world coordinates have been
 * specified for all grid cells), a transformation from grid coordinates to "real world" coordinates can be obtained
 * with the {@link #create()} method. If this transform is close enough to an affine transform,
 * then an instance of {@link LinearTransform} is returned.
 * Otherwise, a transform backed by the localization grid is returned.
 *
 * <p>This builder performs two steps:</p>
 * <ol>
 *   <li>Compute a linear approximation of the transformation using {@link LinearTransformBuilder}.</li>
 *   <li>Compute {@link DatumShiftGrid} with the residuals.</li>
 *   <li>Create a {@link InterpolatedTransform} with the above shift grid.</li>
 * </ol>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 *
 * @see InterpolatedTransform
 * @see LinearTransform
 * @see DatumShiftGrid
 */
public class LocalizationGridBuilder extends TransformBuilder {
    /**
     * The transform for the linear part.
     */
    private final LinearTransformBuilder linear;

    /**
     * A temporary array for two-dimensional source coordinates.
     * Used for reducing object allocations.
     */
    private final int[] tmp;

    /**
     * Conversions from source real-world coordinates to grid indices before interpolation.
     */
    private LinearTransform sourceToGrid;

    /**
     * Creates a new, initially empty, builder.
     *
     * @param width   the number of columns in the grid of target positions.
     * @param height  the number of rows in the grid of target positions.
     */
    public LocalizationGridBuilder(final int width, final int height) {
        linear = new LinearTransformBuilder(width, height);
        tmp    = new int[2];
        sourceToGrid = MathTransforms.identity(2);
    }

    /**
     * Defines relationship between "real-world" source coordinates and grid coordinates.
     * The given transform is usually two-dimensional, in which case conversions from (<var>x</var>,<var>y</var>)
     * source coordinates to ({@code gridX}, {@code gridY}) indices can be done with the following formulas:
     * <ul>
     *   <li><var>gridX</var> = (<var>x</var> - <var>x₀</var>) / <var>Δx</var></li>
     *   <li><var>gridY</var> = (<var>y</var> - <var>y₀</var>) / <var>Δy</var></li>
     * </ul>
     *
     * where:
     * <ul>
     *   <li>(<var>x₀</var>, <var>y₀</var>) is the coordinate of the center of the cell at grid index (0,0).</li>
     *   <li><var>Δx</var> and <var>Δy</var> are the distances between two cells on the <var>x</var> and <var>y</var>
     *       axes respectively, in the unit of measurement given by {@link #getCoordinateUnit()}.</li>
     * </ul>
     *
     * The {@code coordinateToGrid} transform for the above formulas can be represented by the following matrix:
     *
     * {@preformat math
     *   ┌                      ┐
     *   │ 1/Δx      0   -x₀/Δx │
     *   │    0   1/Δy   -y₀/Δy │
     *   │    0      0        1 │
     *   └                      ┘
     * }
     *
     * If this method is never invoked, then the default conversion is identity.
     *
     * @param sourceToGrid  conversion from the "real world" source coordinates to grid indices including fractional parts.
     *
     * @see DatumShiftGrid#getCoordinateToGrid()
     */
    public void setSourceToGrid(final LinearTransform sourceToGrid) {
        ArgumentChecks.ensureNonNull("sourceToGrid", sourceToGrid);
        this.sourceToGrid = sourceToGrid;
    }

    /**
     * Sets a single matching control point pair. Source position is assumed precise and target position is assumed uncertain.
     * If the given source position was already associated with another target position, then the old target position is discarded.
     *
     * @param  gridX   the column index in the grid where to store the given target position.
     * @param  gridY   the row index in the grid where to store the given target position.
     * @param  target  the target coordinates, assumed uncertain.
     * @throws IllegalArgumentException if the {@code x} or {@code y} ordinate value is out of grid range.
     * @throws MismatchedDimensionException if the target position does not have the expected number of dimensions.
     */
    public void setControlPoint(final int gridX, final int gridY, final double... target) {
        tmp[0] = gridX;
        tmp[1] = gridY;
        linear.setControlPoint(tmp, target);
    }

    /**
     * Returns a single target coordinate for the given source coordinate, or {@code null} if none.
     *
     * @param  gridX  the column index in the grid where to read the target position.
     * @param  gridY  the row index in the grid where to read the target position.
     * @return the target coordinates associated to the given source, or {@code null} if none.
     * @throws IllegalArgumentException if the {@code x} or {@code y} ordinate value is out of grid range.
     */
    public double[] getControlPoint(final int gridX, final int gridY) {
        tmp[0] = gridX;
        tmp[1] = gridY;
        return linear.getControlPoint(tmp);
    }

    /**
     * Returns whether the result of last call to {@link LinearTransformBuilder#create()} can be considered
     * a good fit. If {@code true}, then {@link #create()} will return the linear transform directly.
     */
    private boolean isLinear() {
        for (final double c : linear.correlation()) {
            if (c < 0.99) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a transform from the source points to the target points.
     * This method assumes that source points are precise and all uncertainty is in the target points.
     * If this transform is close enough to an affine transform, then an instance of {@link LinearTransform} is returned.
     *
     * @param  factory  the factory to use for creating the transform, or {@code null} for the default factory.
     *                  The {@link MathTransformFactory#createAffineTransform(Matrix)} method of that factory
     *                  shall return {@link LinearTransform} instances.
     * @return the transform from source to target points.
     * @throws FactoryException if the transform can not be created,
     *         for example because the target points have not be specified.
     */
    @Override
    public MathTransform create(final MathTransformFactory factory) throws FactoryException {
        final LinearTransform gridToCoord = linear.create(factory);
        if (isLinear()) {
            return gridToCoord;
        }
        final int      width  = linear.gridSize(0);
        final int      height = linear.gridSize(1);
        final int      tgtDim = gridToCoord.getTargetDimensions();
        final double[] data   = new double[tgtDim * linear.gridLength];
        final double[] point  = new double[tgtDim];
        try {
            final DirectPosition2D src = new DirectPosition2D();
            DirectPosition tgt = null;
            for (int k=0,y=0; y<height; y++) {
                src.y  = y;
                tmp[1] = y;
                for (int x=0; x<width; x++) {
                    src.x  = x;
                    tmp[0] = x;
                    linear.getControlPoint2D(tmp, point);               // Expected position.
                    tgt = gridToCoord.transform(src, tgt);              // Interpolated position.
                    for (int i=0; i<tgtDim; i++) {
                        data[k++] = point[i] - tgt.getOrdinate(i);      // Residual.
                    }
                }
            }
            /*
             * At this point, we computed the residual of all coordinate values.
             * Now we need to express those residuals in grid units instead than
             * "real world" unit, because InterpolatedTransform works that way.
             */
            final LinearTransform coordToGrid = gridToCoord.inverse();
            if (tgtDim == 2) {
                coordToGrid.deltaTransform(data, 0, data, 0, linear.gridLength);
            } else {
                throw new UnsupportedOperationException();          // TODO: use a fallback.
            }
        } catch (TransformException e) {
            throw new FactoryException(e);                          // Should never happen.
        }
        return InterpolatedTransform.createGeodeticTransformation(nonNull(factory),
                new ResidualGrid(sourceToGrid, gridToCoord, width, height, tgtDim, data));
    }
}
