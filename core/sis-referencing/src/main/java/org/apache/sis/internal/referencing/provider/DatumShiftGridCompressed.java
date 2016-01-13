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
package org.apache.sis.internal.referencing.provider;

import java.util.Arrays;
import javax.measure.quantity.Quantity;
import org.apache.sis.math.DecimalFunctions;


/**
 * A datum shift grid which store the values in {@code short[]} array.
 * In addition to using half the space of {@code float[]} arrays, it can also (ironically)
 * increase the precision in the common case where the shifts are specified with no more than
 * 5 digits in base 10 in ASCII files.
 *
 * @param <C> Dimension of the coordinate unit (usually {@link javax.measure.quantity.Angle}).
 * @param <T> Dimension of the translation unit (usually {@link javax.measure.quantity.Angle}
 *            or {@link javax.measure.quantity.Length}).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class DatumShiftGridCompressed<C extends Quantity, T extends Quantity> extends DatumShiftGridFile<C,T> {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4847888093457104917L;

    /**
     * Maximal grid index along the <var>y</var> axis.
     * This is the number of grid cells minus 2.
     */
    private final int ymax;

    /**
     * An "average" value for the offset in each dimension.
     */
    private final double[] averages;

    /**
     * Differences between {@link #averages} values and the actual value.
     * The differences need to be multiplied by {@link #scale}.
     */
    private final short[][] data;

    /**
     * The factor by which to multiply each {@link #data} value before to add to the {@link #averages}.
     */
    private final double scale;

    /**
     * Creates a new datum shift grid for the same geometry than the given grid but different data.
     */
    private DatumShiftGridCompressed(final DatumShiftGridFile<C,T> grid, final double[] averages,
            final short[][] data, final double scale)
    {
        super(grid);
        this.ymax     = getGridSize()[1] - 2;
        this.averages = averages;
        this.data     = data;
        this.scale    = scale;
    }

    /**
     * Tries to compress the given grid. If this operation succeed, a new grid is returned.
     * Otherwise this method returns the given {@code grid} unchanged.
     *
     * @param  grid      The grid to compress.
     * @param  averages  An "average" value for the offset in each dimension, or {@code null} if unknown.
     * @param  scale     The factor by which to multiply each compressed value before to add to the average value.
     * @return The grid to use (may or may not be compressed).
     */
    static <C extends Quantity, T extends Quantity> DatumShiftGridFile<C,T> compress(
            final DatumShiftGridFile.Float<C,T> grid, double[] averages, final double scale)
    {
        final short[][] data = new short[grid.offsets.length][];
        final boolean computeAverages = (averages == null);
        if (computeAverages) {
            averages = new double[data.length];
        }
        for (int dim = 0; dim < data.length; dim++) {
            final double average;
            if (computeAverages) {
                average = Math.rint(grid.getCellMean(dim) / scale);
                averages[dim] = average * scale;
            } else {
                average = averages[dim] / scale;
            }
            final float[] offsets = grid.offsets[dim];
            final short[] compressed = new short[offsets.length];
            for (int i=0; i<offsets.length; i++) {
                double c = DecimalFunctions.floatToDouble(offsets[i]);  // Presume that values were defined in base 10 (usually in an ASCII file).
                c /= scale;                                             // The scale is usually a power of 10 (so the above conversion helps).
                final float tolerance = Math.ulp((float) c);            // Maximum difference for considering that we do not lost any digit.
                c -= average;
                c -= (compressed[i] = (short) Math.round(c));
                if (!(Math.abs(c) < tolerance)) {                       // Use '!' for catching NaN values.
                    return grid;    // Can not compress.
                }
            }
            data[dim] = compressed;
        }
        return new DatumShiftGridCompressed<C,T>(grid, averages, data, scale);
    }

    /**
     * Returns a new grid with the same geometry than this grid but different data arrays.
     */
    @Override
    final DatumShiftGridFile<C,T> setData(final Object[] other) {
        return new DatumShiftGridCompressed<C,T>(this, averages, (short[][]) other, scale);
    }

    /**
     * Suggests a precision for the translation values in this grid.
     *
     * @return A precision for the translation values in this grid.
     */
    @Override
    public double getCellPrecision() {
        // 5* is for converting 0.1 × 10⁻ⁿ to 0.5 × 10⁻ⁿ
        // where n is the number of significant digits.
        return Math.min(super.getCellPrecision(), 5*scale);
    }

    /**
     * Returns direct references (not cloned) to the data arrays.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    final Object[] getData() {
        return data;
    }

    /**
     * Returns the number of shift dimensions.
     */
    @Override
    public final int getTranslationDimensions() {
        return data.length;
    }

    /**
     * Returns the average translation parameters from source to target.
     *
     * @param dim The dimension for which to get an average value.
     * @return A value close to the average for the given dimension.
     */
    @Override
    public double getCellMean(final int dim) {
        return averages[dim];
    }

    /**
     * Returns the cell value at the given grid index.
     */
    @Override
    public double getCellValue(final int dim, final int gridX, final int gridY) {
        return data[dim][gridX + gridY*nx] * scale + averages[dim];
    }

    /**
     * Copy of {@link org.apache.sis.referencing.datum.DatumShiftGrid} rewritten in a way that
     * reduce the number of arithmetic operations for efficiency reasons.
     */
    @Override
    public void interpolateInCell(double gridX, double gridY, double[] vector) {
        int ix = (int) gridX;  gridX -= ix;
        int iy = (int) gridY;  gridY -= iy;
        if (ix < 0) {
            ix = 0;
            gridX = -1;
        } else if (ix >= nx - 1) {
            ix = nx - 2;
            gridX = +1;
        }
        if (iy < 0) {
            iy = 0;
            gridY = -1;
        } else if (iy > ymax) {   // Subtraction of 2 already done by the constructor.
            iy = ymax;
            gridY = +1;
        }
        final int p0 = nx*iy + ix;
        final int p1 = nx + p0;
        for (int dim = 0; dim < data.length; dim++) {
            final short[] values = data[dim];
            double r0 = values[p0];
            double r1 = values[p1];
            r0 +=  gridX * (values[p0+1] - r0);
            r1 +=  gridX * (values[p1+1] - r1);
            vector[dim] = (gridY * (r1 - r0) + r0) * scale + averages[dim];
        }
    }

    /**
     * Returns {@code true} if the given object is a grid containing the same data than this grid.
     *
     * @param  other The other object to compare with this datum shift grid.
     * @return {@code true} if the given object is non-null, an instance of {@code DatumShiftGridCompressed}
     *         and contains the same data.
     */
    @Override
    public boolean equals(final Object other) {
        if (super.equals(other)) {
            final DatumShiftGridCompressed<?,?> that = (DatumShiftGridCompressed<?,?>) other;
            return Double.doubleToLongBits(scale) == Double.doubleToLongBits(that.scale)
                   && Arrays.equals(averages, that.averages);
        }
        return false;
    }

    /**
     * Returns a hash code value for this datum shift grid.
     *
     * @return {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return super.hashCode() + Arrays.hashCode(averages);
    }
}
