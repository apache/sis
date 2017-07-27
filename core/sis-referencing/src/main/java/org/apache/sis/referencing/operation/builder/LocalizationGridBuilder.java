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
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.referencing.operation.transform.InterpolatedTransform;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.datum.DatumShiftGrid;
import org.apache.sis.internal.referencing.Resources;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.math.MathFunctions;
import org.apache.sis.math.Vector;


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
     * Tolerance threshold for comparing pixel coordinates relative to integer values.
     */
    private static final double EPS = Numerics.COMPARISON_THRESHOLD;

    /**
     * The transform for the linear part.
     */
    private final LinearTransformBuilder linear;

    /**
     * A temporary array for two-dimensional source coordinates.
     * Used for reducing object allocations.
     */
    private final int[] tmp = new int[2];

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
     * Creates a new, initially empty, builder for a localization grid of the given size.
     *
     * @param width   the number of columns in the grid of target positions.
     * @param height  the number of rows in the grid of target positions.
     */
    public LocalizationGridBuilder(final int width, final int height) {
        linear       = new LinearTransformBuilder(width, height);
        sourceToGrid = MathTransforms.identity(2);
    }

    /**
     * Creates a new, initially empty, builder for a localization grid of a size inferred from the given points.
     * This constructor uses the given vectors for computing a grid size and the following initial conversion:
     *
     * <blockquote>({@code sourceX}, {@code sourceY}) → ({@code gridX}, {@code gridY})</blockquote>
     *
     * Above conversion can be obtained by {@link #getSourceToGrid()}.
     *
     * <p>Values in the given vectors should be integers, but this constructor is tolerant to non-integer values
     * if they have a constant offset (typically 0.5) relative to integer values. The two vectors do not need to
     * have the same length (i.e. {@code sourceX[i]} are not necessarily related to {@code sourceY[i]}).</p>
     *
     * @param  sourceX  all possible <var>x</var> inputs before conversion to grid coordinates.
     * @param  sourceY  all possible <var>y</var> inputs before conversion to grid coordinates.
     * @throws ArithmeticException if this constructor can not infer a reasonable grid size from the given vectors.
     */
    public LocalizationGridBuilder(final Vector sourceX, final Vector sourceY) {
        final Matrix fromGrid = Matrices.createDiagonal(3,3);
        linear = new LinearTransformBuilder(infer(sourceX, fromGrid, 0), infer(sourceY, fromGrid, 1));
        try {
            sourceToGrid = MathTransforms.linear(fromGrid).inverse();
        } catch (NoninvertibleTransformException e) {
            // Should not happen because infer(…) verified that the coefficients are okay.
            throw (ArithmeticException) new ArithmeticException(e.getLocalizedMessage()).initCause(e);
        }
    }

    /**
     * Infers a grid size by searching for the greatest common divisor (GCD) for values in the given vector.
     * The vector values should be integers, but this method is tolerant to constant offsets (typically 0.5).
     * The GCD is taken as a "grid to source" scale factor and the minimal value as the translation term.
     * Those two values are stored in the {@code dim} row of the given matrix.
     *
     * @param  source    the vector of values for which to get the GCD and minimum value.
     * @param  fromGrid  matrix where to store the minimum value and the GCD.
     * @param  dim       index of the matrix row to update.
     * @return grid size.
     */
    private static int infer(final Vector source, final Matrix fromGrid, final int dim) {
        final NumberRange<?> range = source.range();
        final double min  = range.getMinDouble(true);
        final double span = range.getMaxDouble(true) - min;
        final Number increment = source.increment(EPS * span);
        double inc;
        if (increment != null) {
            inc = increment.doubleValue();
        } else {
            inc = span;
            final int size = source.size();
            for (int i=0; i<size; i++) {
                double v = source.doubleValue(i) - min;
                if (Math.abs(v % inc) > EPS) {
                    do {
                        final double r = (inc % v);     // Both 'inc' and 'v' are positive, so 'r' will be positive too.
                        inc = v;
                        v = r;
                    } while (Math.abs(v) > EPS);
                }
            }
        }
        fromGrid.setElement(dim, dim, inc);
        fromGrid.setElement(dim,   2, min);
        final double n = span / inc;
        if (n > 0.5 && n < 0.5 + Short.MAX_VALUE) {
            return ((int) Math.round(n)) + 1;
        }
        throw new ArithmeticException(Resources.format(Resources.Keys.CanNotInferGridSizeFromValues_1, range));
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
     * Returns the desired precision of <em>inverse</em> transformations, in units of source coordinates.
     * This is the precision sets by the last call to {@link #setDesiredPrecision(double)}.
     *
     * @return desired precision of the results of inverse transformations.
     */
    public double getDesiredPrecision() {
        return precision;
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
     * Returns the current relationship between "real-world" source coordinates and grid coordinates.
     * This is the value set by the last call to {@link #setSourceToGrid(LinearTransform)}.
     * If that setter method has never been invoked, then this is an automatically computed transform
     * if the grid coordinates {@linkplain #LocalizationGridBuilder(Vector, Vector) have been specified
     * to the constructor}, or the identity transform {@linkplain #LocalizationGridBuilder(int, int) otherwise}.
     *
     * @return the current relationship between "real-world" source coordinates and grid coordinates.
     */
    public LinearTransform getSourceToGrid() {
        return sourceToGrid;
    }

    /**
     * Sets a single matching control point pair. Source position is assumed precise and target position is assumed uncertain.
     * If the given source position was already associated with another target position, then the old target position is discarded.
     *
     * <p>If a {@linkplain #getSourceToGrid() source to grid} conversion exists, it shall have been applied
     * by the caller for computing the ({@code gridX}, {@code gridY}) coordinates given to this method.</p>
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
            return MathTransforms.concatenate(sourceToGrid, gridToCoord);
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
            return MathTransforms.concatenate(sourceToGrid, gridToCoord);
        }
        return InterpolatedTransform.createGeodeticTransformation(nonNull(factory),
                new ResidualGrid(sourceToGrid, gridToCoord, width, height, tgtDim, residual,
                (gridPrecision > 0) ? gridPrecision : DEFAULT_PRECISION));
    }
}
