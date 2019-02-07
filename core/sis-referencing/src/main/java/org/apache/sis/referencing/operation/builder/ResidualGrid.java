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

import java.util.function.Function;
import javax.measure.quantity.Dimensionless;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.Matrix;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.referencing.operation.matrix.Matrix3;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.ContextualParameters;
import org.apache.sis.internal.referencing.provider.DatumShiftGridFile;
import org.apache.sis.internal.referencing.WKTUtilities;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.math.Statistics;
import org.apache.sis.math.Vector;
import org.apache.sis.measure.Units;


/**
 * The residuals after an affine approximation has been created for a set of matching control point pairs.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.8
 * @module
 */
final class ResidualGrid extends DatumShiftGridFile<Dimensionless,Dimensionless> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1445697681304159019L;

    /**
     * Number of source dimensions of the residual grid.
     */
    static final int SOURCE_DIMENSION = 2;

    /**
     * The parameter descriptors for the "Localization grid" operation.
     * Current implementation is fixed to 2 dimensions. This is not a committed set of parameters and they
     * may change in any future SIS version. We define them mostly for {@code toString()} implementation.
     */
    private static final ParameterDescriptorGroup PARAMETERS;
    static {
        final ParameterBuilder builder = new ParameterBuilder().setRequired(true);
        @SuppressWarnings("rawtypes")
        final ParameterDescriptor<?>[] grids = new ParameterDescriptor[] {
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
    public void setGridParameters(final Parameters parameters) {
        super.setGridParameters(parameters);
        final Matrix denormalization;
        if (parameters instanceof ContextualParameters) {
            denormalization = ((ContextualParameters) parameters).getMatrix(ContextualParameters.MatrixRole.DENORMALIZATION);
        } else {
            denormalization = new Matrix3();            // Identity.
        }
        final int[] size = getGridSize();
        parameters.parameter(Constants.NUM_ROW).setValue(size[1]);
        parameters.parameter(Constants.NUM_COL).setValue(size[0]);
        parameters.parameter("grid_x").setValue(new Data(0, denormalization));
        parameters.parameter("grid_y").setValue(new Data(1, denormalization));
    }

    /**
     * The residual data, as translations to apply on the result of affine transform.
     * In this flat array, index of target dimension varies fastest, then column index, then row index.
     */
    private final double[] offsets;

    /**
     * Conversion from grid coordinates to the final "real world" coordinates.
     *
     * @see #gridToTarget()
     */
    private final LinearTransform gridToTarget;

    /**
     * Creates a new residual grid.
     *
     * @param sourceToGrid  conversion from the "real world" source coordinates to grid indices including fractional parts.
     * @param gridToTarget  conversion from grid coordinates to the final "real world" coordinates.
     * @param residuals     the residual data, as translations to apply on the result of affine transform.
     * @param precision     desired precision of inverse transformations in unit of grid cells.
     */
    ResidualGrid(final LinearTransform sourceToGrid, final LinearTransform gridToTarget,
            final int nx, final int ny, final double[] residuals, final double precision)
    {
        super(Units.UNITY, Units.UNITY, true, sourceToGrid, nx, ny, PARAMETERS);
        this.gridToTarget = gridToTarget;
        this.offsets      = residuals;
        this.accuracy     = precision;
    }

    /**
     * Creates a new datum shift grid with the same grid geometry than the given grid
     * but a reference to a different data array.
     */
    private ResidualGrid(final ResidualGrid other, final double[] data) {
        super(other);
        gridToTarget = other.gridToTarget;
        accuracy     = other.accuracy;
        offsets      = data;
    }

    /**
     * Returns a new grid with the same geometry than this grid but different data array.
     */
    @Override
    protected DatumShiftGridFile<Dimensionless, Dimensionless> setData(final Object[] other) {
        return new ResidualGrid(this, (double[]) other[0]);
    }

    /**
     * Returns reference to the data array. This method is for cache management, {@link #equals(Object)}
     * and {@link #hashCode()} implementations only and should not be invoked in other context.
     */
    @Override
    protected Object[] getData() {
        return new Object[] {offsets};
    }

    /**
     * Returns the transform from grid coordinates to "real world" coordinates after the datum shift has been applied.
     */
    @Override
    public Matrix gridToTarget() {
        return gridToTarget.getMatrix();
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
        return offsets[(gridX + gridY*nx) * SOURCE_DIMENSION + dim];
    }

    /**
     * View over one dimension of the offset vectors. This is used for populating the {@link ParameterDescriptorGroup}
     * that describes the {@code MathTransform}. Those parameters are themselves used for formatting Well Known Text.
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

        @SuppressWarnings("CloneInNonCloneableClass")
        @Override public Matrix  clone()                            {return this;}
        @Override public boolean isIdentity()                       {return false;}
        @Override public int     getNumCol()                        {return nx;}
        @Override public int     getNumRow()                        {return getGridSize()[1];}
        @Override public Number  apply     (int[] p)                {return getElement(p[1], p[0]);}
        @Override public void    setElement(int y, int x, double v) {throw new UnsupportedOperationException();}

        /** Computes the matrix element in the given row and column. */
        @Override public double  getElement(final int y, final int x) {
            return c0 * (x + getCellValue(0, x, y)) +                // TODO: use Math.fma with JDK9.
                   c1 * (y + getCellValue(1, x, y)) +
                   c2;
        }

        /**
         * Returns a short string representation on one line. This appears as a single row
         * in the table formatted for {@link ParameterDescriptorGroup} string representation.
         */
        @Override public String toString() {
            final int[] size = getGridSize();
            return new StringBuilder(80).append('[')
                    .append(getElement(0, 0)).append(", …, ")
                    .append(getElement(size[1] - 1, size[0] - 1))
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
}
