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

import java.util.Arrays;
import java.util.function.Function;
import javax.measure.quantity.Dimensionless;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.referencing.datum.DatumShiftGrid;
import org.apache.sis.referencing.operation.matrix.MatrixSIS;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.referencing.internal.shared.WKTUtilities;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.math.Statistics;
import org.apache.sis.math.Vector;
import org.apache.sis.measure.Units;


/**
 * The residuals after an affine approximation has been created for a set of matching control point pairs.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class ResidualGrid extends DatumShiftGrid<Dimensionless,Dimensionless> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -3668228260650927123L;

    /**
     * Number of source dimensions of the residual grid.
     *
     * @see #INTERPOLATED_DIMENSIONS
     */
    static final int SOURCE_DIMENSION = 2;

    /**
     * The parameter descriptors for the "Localization grid" operation.
     * Current implementation is fixed to {@value #SOURCE_DIMENSION} dimensions.
     *
     * @see #getParameterDescriptors()
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = new ParameterBuilder().setRequired(true);
        final ParameterDescriptor<?>[] grids = new ParameterDescriptor<?>[] {
            builder.addName(Constants.NUM_ROW).createBounded(Integer.class, 2, null, null),
            builder.addName(Constants.NUM_COL).createBounded(Integer.class, 2, null, null),
            builder.addName("grid_x").create(Matrix.class, null),
            builder.addName("grid_y").create(Matrix.class, null)
        };
        PARAMETERS = builder.addName("Localization grid").createGroup(grids);
    }

    /**
     * Sets the parameters of the {@code InterpolatedTransform} which uses that localization grid.
     * The given {@code parameters} must have been created from {@link #PARAMETERS} descriptor.
     * This method sets the matrix parameters using views over the {@link #offsets} array.
     */
    @Override
    public void getParameterValues(final Parameters parameters) {
        final Matrix denormalization = gridToTarget.getMatrix();
        if (parameters instanceof ContextualParameters) {
            /*
             * The denormalization matrix computed by InterpolatedTransform is the inverse of the normalization matrix.
             * This inverse is not suitable for the transform created by LocalizationGridBuilder; we need to replace it
             * by the linear regression. We do not want to define a public API in `DatumShiftGrid` for that purpose yet
             * because it would complexify that class (we would have to define API contract, etc.).
             */
            MatrixSIS m = ((ContextualParameters) parameters).getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
            m.setMatrix(denormalization);
        }
        parameters.parameter(Constants.NUM_ROW).setValue(getGridSize(1));
        parameters.parameter(Constants.NUM_COL).setValue(getGridSize(0));
        parameters.parameter("grid_x").setValue(new Data(0, denormalization));
        parameters.parameter("grid_y").setValue(new Data(1, denormalization));
    }

    /**
     * Number of cells between the start of adjacent rows in the grid. This is usually {@code getGridSize(0)},
     * stored as a field for performance reasons. Value could be greater than {@code getGridSize(0)} if there
     * is some elements to ignore at the end of each row.
     */
    private final int scanlineStride;

    /**
     * The residual data, as translations to apply on the result of affine transform.
     * In this flat array, index of target dimension varies fastest, then column index, then row index.
     * Single precision instead of double is presumed sufficient because this array contains only differences,
     * not absolute positions. Absolute positions will be computed by adding {@code double} values to those offsets.
     */
    private final float[] offsets;

    /**
     * Conversion from translated coordinates (after the datum shift has been applied) to "real world" coordinates.
     * If we were doing NADCON or NTv2 transformations with {@link #isCellValueRatio()} = {@code true} (source and
     * target coordinates in the same coordinate system with axis units in degrees), that conversion would be the
     * inverse of {@link #getCoordinateToGrid()}. But in this {@code ResidualGrid} case, we need to override with
     * the linear regression computed by {@link LocalizationGridBuilder}.
     */
    @SuppressWarnings("serial")             // Most SIS implementations are serializable.
    final LinearTransform gridToTarget;

    /**
     * The best translation accuracy that we can expect from this file.
     *
     * @see #getCellPrecision()
     */
    private final double accuracy;

    /**
     * If grid coordinates in some target dimensions are cyclic, the period in number of cells.
     * For each scalar value in the {@link LocalizationGridBuilder#periods} array (in units of
     * target CRS), the corresponding period in number of cells is a vector. For example, a 360°
     * shift in longitude does not necessarily correspond to an horizontal or vertical offset
     * in grid indices; it may be a combination of both if the grid is inclined.
     *
     * <p>We should have as many vectors as non-zero values in {@link LocalizationGridBuilder#periods}.
     * Each {@code periodVector} (in cell units) should be computed from a {@code periods} vector with
     * exactly one non-zero value (in CRS units) for allowing shifts in different CRS dimensions to be
     * applied independently. Consequently, this field should actually be of type {@code double[][]}.
     * But current version uses only one vector for avoiding the complexity of searching how to combine
     * multiple vectors. It is okay for the usual case where only one CRS axis has wraparound range,
     * but may need to be revisited in the future.</p>
     *
     * <p>This array is {@code null} if no period has been specified, or if a period has been specified
     * but we cannot convert it from CRS units to a constant number of cells.</p>
     *
     * @see LocalizationGridBuilder#periods
     * @see #replaceOutsideGridCoordinates(double[])
     */
    private final double[] periodVector;

    /**
     * Creates a new residual grid.
     *
     * @param sourceToGrid  conversion from the "real world" source coordinates to grid indices including fractional parts.
     * @param gridToTarget  conversion from grid coordinates to the final "real world" coordinates.
     * @param residuals     the residual data, as translations to apply on the result of affine transform.
     * @param precision     desired precision of inverse transformations in unit of grid cells.
     * @param periods       if grid coordinates in some dimensions are cyclic, their periods in units of target CRS.
     * @param linearizer    the linearizer that have been applied, or {@code null} if none.
     */
    ResidualGrid(final LinearTransform sourceToGrid, final LinearTransform gridToTarget,
            final int nx, final int ny, final float[] residuals, final double precision,
            final double[] periods, final ProjectedTransformTry linearizer)
            throws TransformException
    {
        super(Units.UNITY, sourceToGrid, new int[] {nx, ny}, true, Units.UNITY);
        this.gridToTarget   = gridToTarget;
        this.offsets        = residuals;
        this.accuracy       = precision;
        this.scanlineStride = nx;
        if (periods != null && linearizer == null && gridToTarget.isAffine()) {
            /*
             * We require the transform to be affine because it makes the Jacobian independent of
             * coordinate values. It allows us to replace a period in target CRS units by periods
             * in grid units without having to take the coordinate values in account.
             */
            final MatrixSIS m = MatrixSIS.castOrCopy(gridToTarget.inverse().derivative(null));
            periodVector = m.multiply(periods);
            /*
             * Above code is not really right. We should verify that `periods` contains exactly
             * one non-zero element, and if not the case execute above code in a loop with one
             * non-zero element by iteration, creating one independent vector each time.
             * We don't do that for now because `replaceOutsideGridCoordinates(…)` is not yet
             * capable to search the best combination of many vectors.
             *
             * With current implementation, if the `periods` array contains more than 1 non-zero value,
             * then `replaceOutsideGridCoordinates(…)` always shift all wraparound dimensions together.
             * For example if the first dimension has a period of 360° and the second dimension has a
             * period of 12 months, then `replaceOutsideGridCoordinates(…)` will only shift by 360°
             * AND 12 months together, never 360° only or 12 months only.
             */
        } else {
            periodVector = null;
        }
    }

    /**
     * Returns a description of the values in this grid. Grid values may be given as matrices or tensors.
     * Current implementation provides values in the form of {@link Matrix} objects on the assumption
     * that the number of {@linkplain #getGridSize() grid} dimensions is {@value #SOURCE_DIMENSION}.
     *
     * <p>The number of {@linkplain #getGridSize() grid} dimensions determines the parameter type: if that number
     * is greater than {@value #SOURCE_DIMENSION}, then parameters would need to be represented by tensors instead
     * than matrices. By contrast, the {@linkplain #getTranslationDimensions() number of dimensions of translation
     * vectors} only determines how many matrix or tensor parameters appear.</p>
     *
     * @return a description of the values in this grid.
     */
    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return PARAMETERS;
    }

    /**
     * Returns the number of dimensions of the translation vectors interpolated by this shift grid.
     */
    @Override
    public int getTranslationDimensions() {
        return SOURCE_DIMENSION;
    }

    /**
     * Returns the desired precision in iterative calculation performed by inverse transform.
     * The returned value is in unit of grid cell, i.e. a value of 1 is the size of one cell.
     * This unit of measurement is fixed by {@link #isCellValueRatio()} = {@code true}.
     */
    @Override
    public double getCellPrecision() {
        return accuracy;
    }

    /**
     * Returns the cell value at the given dimension and grid index.
     * Those values are components of <em>translation</em> vectors.
     */
    @Override
    public double getCellValue(int dim, int gridX, int gridY) {
        return offsets[(gridX + gridY*scanlineStride) * SOURCE_DIMENSION + dim];
    }

    /**
     * Invoked when a {@code gridX} or {@code gridY} coordinate is outside the range of valid grid coordinates.
     * If the coordinate outside the range is a longitude value and if we handle those values as cyclic, brings
     * that coordinate inside the range.
     */
    @Override
    protected void replaceOutsideGridCoordinates(final double[] gridCoordinates) {
        if (periodVector != null) {
            /*
             * We will try to shift the point by an integral multiple of `periodVector`.
             * Test along each dimension which range of multiples could bring the point
             * inside the grid, and take the range intersection in all dimensions.
             */
            double min = Double.NEGATIVE_INFINITY;
            double max = Double.POSITIVE_INFINITY;
            for (int i=0; i<gridCoordinates.length; i++) {
                final double period = periodVector[i];
                double toLower = gridCoordinates[i];                // If we subtract a shift ≤ toLower, then coordinate ≥ lower bound (which is 0).
                double toUpper = toLower - (getGridSize(i) - 1);    // If we subtract a shift ≥ toUpper, then coordinate ≤ upper bound (which is gridSize−1).
                toLower = Math.floor(toLower / period);             // Convert to integral number of periods, reverse may be ≤ original value.
                toUpper = Math.ceil (toUpper / period);             // Convert to integral number of periods, reverse may be ≥ original value.
                if (toLower < max) max = toLower;                   // Must shift by no more than this value otherwise coordinate < lower bound.
                if (toUpper > min) min = toUpper;                   // Must shift by no less than this value otherwise coordinate > upper bound.
            }
            if (min <= max) {
                /*
                 * If at least one multiple exists, take the multiple closest to zero
                 * (i.e. apply the smallest displacement). Note that the range should
                 * not include zero, otherwise this point would be inside the grid and
                 * this method should not have been invoked.
                 */
                final double n = (min >= 0) ? min : max;
                if (Double.isFinite(n)) {
                    for (int i=0; i<gridCoordinates.length; i++) {
                        gridCoordinates[i] -= periodVector[i] * n;
                    }
                }
            }
        }
    }

    /**
     * View over one target dimension of the localization grid. Used for populating the {@link ParameterDescriptorGroup}
     * that describes the {@code MathTransform}. Those parameters are themselves used for formatting Well Known Text.
     * Current implementation can be used only when the number of grid dimensions is {@value #INTERPOLATED_DIMENSIONS}.
     * If a grid has more dimensions, then tensors would need to be used instead of matrices.
     *
     * <p>This implementation cannot be moved to the {@link DatumShiftGrid} parent class because this class assumes
     * that the translation vectors are added to the source coordinates. This is not always true; for example France
     * Geocentric interpolations add the translation to coordinates converted to geocentric coordinates.</p>
     *
     * @author  Martin Desruisseaux (Geomatys)
     */
    private final class Data extends FormattableObject implements Matrix, Function<int[],Number> {
        /** Coefficients from the denormalization matrix for the row corresponding to this dimension. */
        private final double c0, c1, c2;

        /** Creates a new matrix for the specified dimension. */
        Data(final int dim, final Matrix denormalization) {
            c0 = denormalization.getElement(dim, 0);
            c1 = denormalization.getElement(dim, 1);
            c2 = denormalization.getElement(dim, 2);
        }

        @SuppressWarnings({"CloneInNonCloneableClass", "CloneDoesntCallSuperClone"})
        @Override public Matrix  clone()                            {return this;}
        @Override public boolean isIdentity()                       {return false;}
        @Override public int     getNumCol()                        {return getGridSize(0);}
        @Override public int     getNumRow()                        {return getGridSize(1);}
        @Override public Number  apply     (int[] p)                {return getElement(p[1], p[0]);}
        @Override public void    setElement(int y, int x, double v) {throw new UnsupportedOperationException();}

        /** Computes the matrix element in the given row and column. */
        @Override public double  getElement(final int y, final int x) {
            if ((x | y) < 0 || x >= scanlineStride) {
                throw new IndexOutOfBoundsException();
            }
            return Math.fma(x + getCellValue(0, x, y), c0,
                   Math.fma(y + getCellValue(1, x, y), c1, c2));
        }

        /**
         * Returns a short string representation on one line. This appears as a single row
         * in the table formatted for {@link ParameterDescriptorGroup} string representation.
         */
        @Override public String toString() {
            return new StringBuilder(80).append('[')
                    .append(getElement(0, 0)).append(", …, ")
                    .append(getElement(getGridSize(1) - 1, getGridSize(0) - 1))
                    .append(']').toString();
        }

        /**
         * Returns a multi-lines string representation. This appears in the Well Known Text (WKT)
         * formatting of {@link org.opengis.referencing.operation.MathTransform}.
         */
        @Override protected String formatTo(final Formatter formatter) {
            final Object[] numbers = WKTUtilities.cornersAndCenter(this, getGridSize(), 3);
            final Vector[] rows = new Vector[numbers.length];
            final Statistics stats = new Statistics(null);          // For computing accuracy.
            Vector before = null;
            for (int j=0; j<rows.length; j++) {
                final Vector row = Vector.create(numbers[j], false);
                /*
                 * Estimate an accuracy to use for formatting values. This computation is specific to ResidualGrid
                 * since it assumes that values in each corner are globally increasing or decreasing. Consequently
                 * the differences between consecutive values are assumed a good indication of desired accuracy
                 * (this assumption does not hold for arbitrary matrix).
                 */
                Number right = null;
                for (int i=row.size(); --i >= 0;) {
                    final Number n = row.get(i);
                    if (n != null) {
                        final double value = n.doubleValue();
                        if (right != null) {
                            stats.accept(Math.abs(right.doubleValue() - value));
                        }
                        if (before != null) {
                            final Number up = before.get(i);
                            if (up != null) {
                                stats.accept(Math.abs(up.doubleValue() - value));
                            }
                        }
                    }
                    right = n;
                }
                before  = row;
                rows[j] = row;
            }
            final int accuracy = Numerics.suggestFractionDigits(stats);
            formatter.newLine();
            formatter.append(rows, Math.max(0, accuracy));
            formatter.setInvalidWKT(Matrix.class, null);
            return "Matrix";
        }
    }

    /**
     * Returns {@code true} if the given object is a grid containing the same data as this grid.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {                        // Optimization for a common case.
            return true;
        }
        if (super.equals(other)) {
            final ResidualGrid that = (ResidualGrid) other;
            return Numerics.equals(accuracy, that.accuracy) &&
                    gridToTarget.equals(that.gridToTarget) &&
                    Arrays.equals(offsets, that.offsets);
        }
        return false;
    }

    /**
     * Returns a hash code value for this datum shift grid.
     */
    @Override
    public int hashCode() {
        return super.hashCode() + Arrays.hashCode(offsets) + 37 * gridToTarget.hashCode();
    }
}
