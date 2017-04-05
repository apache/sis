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
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.referencing.operation.transform.InterpolatedTransform;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.datum.DatumShiftGrid;
import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.math.MathFunctions;


/**
 * Creates an "almost linear" transform mapping the given source points to the given target points.
 * The transform is backed by a <cite>grid of localization</cite>, a two-dimensional array of coordinate points.
 * Grid size is {@code width} × {@code height} and input coordinates are (<var>i</var>,<var>j</var>) index in the grid,
 * where <var>i</var> must be in the [0…{@code width}-1] range and <var>j</var> in the [0…{@code height}-1] range inclusive.
 * Output coordinates are the values stored in the grid of localization at the specified index.
 * After a {@code LocalizationGridBuilder} instance has been fully populated (i.e. real world coordinates have been
 * specified for all grid cells), a transformation from grid coordinates to "real world" coordinates can be obtained
 * with the {@link #create(MathTransformFactory)} method. If this transform is close enough to an affine transform,
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
 * @version 0.8
 *
 * @see InterpolatedTransform
 * @see LinearTransform
 * @see DatumShiftGrid
 *
 * @since 0.8
 * @module
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
     * The desired precision of inverse transformations in unit of source coordinates, or 0 in unspecified.
     * If no {@link #sourceToGrid} transform has been specified, than this is in unit of grid cell.
     */
    private double precision;

    /**
     * Arbitrary default {@link #precision} value. May change in any future SIS version.
     */
    static final double DEFAULT_PRECISION = 1E-7;

    /**
     * Creates a new, initially empty, builder.
     *
     * @param width   the number of columns in the grid of target positions.
     * @param height  the number of rows in the grid of target positions.
     */
    public LocalizationGridBuilder(final int width, final int height) {
        linear       = new LinearTransformBuilder(width, height);
        tmp          = new int[2];
        sourceToGrid = MathTransforms.identity(2);
    }

    /**
     * Sets the desired precision of <em>inverse</em> transformations, in units of source coordinates.
     * If a conversion from "real world" to grid coordinates {@linkplain #setSourceToGrid has been specified},
     * then the given precision is in "real world" units. Otherwise the precision is in units of grid cells.
     *
     * <div class="note"><b>Note:</b>
     * there is no method for setting the desired target precision because forward transformations <em>precision</em>
     * (not to be confused with <em>accuracy</em>) are limited only by rounding errors. Of course the accuracy of both
     * forward and inverse transformations still limited by the accuracy of given control points and the grid resolution.
     * </div>
     *
     * @param precision  desired precision of the results of inverse transformations.
     *
     * @see DatumShiftGrid#getCellPrecision()
     */
    public void setDesiredPrecision(final double precision) {
        ArgumentChecks.ensureStrictlyPositive("precision", precision);
        this.precision = precision;
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
     *       axes respectively, in the same unit of measurement than the one documented in the
     *       {@link #setDesiredPrecision(double)} method.</li>
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
     * If a {@linkplain #setDesiredPrecision(double) desired precision} has been specified before this method call,
     * it is caller's responsibility to convert that value to new source units if needed.
     *
     * @param sourceToGrid  conversion from the "real world" source coordinates to grid indices including fractional parts.
     *
     * @see DatumShiftGrid#getCoordinateToGrid()
     */
    public void setSourceToGrid(final LinearTransform sourceToGrid) {
        ArgumentChecks.ensureNonNull("sourceToGrid", sourceToGrid);
        int isTarget = 0;
        int dim = sourceToGrid.getSourceDimensions();
        if (dim >= 2) {
            isTarget = 1;
            dim = sourceToGrid.getTargetDimensions();
            if (dim == 2) {
                this.sourceToGrid = sourceToGrid;
                return;
            }
        }
        throw new MismatchedDimensionException(Resources.format(
                Resources.Keys.MismatchedTransformDimension_3, isTarget, 2, dim));
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
        /*
         * Make a first check about whether the result of above LinearTransformBuilder.create() call
         * can be considered a good fit. If true, then we may return the linear transform directly.
         */
        boolean isExact  = true;
        boolean isLinear = true;
        for (final double c : linear.correlation()) {
            isExact &= (c == 1);
            if (c < 0.9999) {                               // Empirical threshold (may need to be revisited).
                isLinear = false;
                break;
            }
        }
        if (isExact) {
            return gridToCoord;
        }
        final int      width    = linear.gridSize(0);
        final int      height   = linear.gridSize(1);
        final int      tgtDim   = gridToCoord.getTargetDimensions();
        final double[] residual = new double[tgtDim * linear.gridLength];
        final double[] point    = new double[tgtDim + 1];
        double gridPrecision    = precision;
        try {
            /*
             * If the user specified a precision, we need to convert it from source units to grid units.
             * We convert each dimension separately, then retain the largest magnitude of vector results.
             */
            if (gridPrecision > 0 && !sourceToGrid.isIdentity()) {
                final double[] vector = new double[sourceToGrid.getSourceDimensions()];
                final double[] offset = new double[sourceToGrid.getTargetDimensions()];
                double converted = 0;
                for (int i=0; i<vector.length; i++) {
                    vector[i] = precision;
                    sourceToGrid.deltaTransform(vector, 0, offset, 0, 1);
                    final double length = MathFunctions.magnitude(offset);
                    if (length > converted) converted = length;
                    vector[i] = 0;
                }
                gridPrecision = converted;
            }
            /*
             * Compute the residuals, i.e. the differences between the coordinates that we get by a linear
             * transformation and the coordinates that we want to get. If at least one residual is greater
             * than the desired precision,  then the returned MathTransform will need to apply corrections
             * after linear transforms. Those corrections will be done by InterpolatedTransform.
             */
            final MatrixSIS coordToGrid = MatrixSIS.castOrCopy(gridToCoord.inverse().getMatrix());
            final DirectPosition2D src = new DirectPosition2D();
            point[tgtDim] = 1;
            for (int k=0,y=0; y<height; y++) {
                src.y  = y;
                tmp[1] = y;
                for (int x=0; x<width; x++) {
                    src.x  = x;
                    tmp[0] = x;
                    linear.getControlPoint2D(tmp, point);                           // Expected position.
                    double[] grid = coordToGrid.multiply(point);                    // As grid coordinate.
                    isLinear &= (residual[k++] = grid[0] - x) <= gridPrecision;
                    isLinear &= (residual[k++] = grid[1] - y) <= gridPrecision;
                }
            }
        } catch (TransformException e) {
            throw new FactoryException(e);                                          // Should never happen.
        }
        if (isLinear) {
            return gridToCoord;
        }
        return InterpolatedTransform.createGeodeticTransformation(nonNull(factory),
                new ResidualGrid(sourceToGrid, gridToCoord, width, height, tgtDim, residual,
                (gridPrecision > 0) ? gridPrecision : DEFAULT_PRECISION));
    }
}
