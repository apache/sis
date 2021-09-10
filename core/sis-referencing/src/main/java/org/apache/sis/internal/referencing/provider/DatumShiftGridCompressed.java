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
import javax.measure.Quantity;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.internal.util.Numerics;


/**
 * A datum shift grid which store the values in {@code short[]} array.
 * In addition to using half the space of {@code float[]} arrays, it can also (ironically)
 * increase the precision in the common case where the shifts are specified with no more than
 * 5 digits in base 10 in ASCII files.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param <C>  dimension of the coordinate unit (usually {@link javax.measure.quantity.Angle}).
 * @param <T>  dimension of the translation unit (usually {@link javax.measure.quantity.Angle}
 *             or {@link javax.measure.quantity.Length}).
 *
 * @since 0.7
 * @module
 */
final class DatumShiftGridCompressed<C extends Quantity<C>, T extends Quantity<T>> extends DatumShiftGridFile<C,T> {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 1889111858140209014L;

    /**
     * An "average" value for the offset in each dimension.
     *
     * @see #getCellMean(int)
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
        this.averages = averages;
        this.data     = data;
        this.scale    = scale;
    }

    /**
     * Tries to compress the given grid. If this operation succeed, a new grid is returned.
     * Otherwise this method returns the given {@code grid} unchanged.
     *
     * @param  grid      the grid to compress.
     * @param  averages  an "average" value for the offset in each dimension, or {@code null} if unknown.
     * @param  scale     the factor by which to multiply each compressed value before to add to the average value.
     * @return the grid to use (may or may not be compressed).
     */
    static <C extends Quantity<C>, T extends Quantity<T>> DatumShiftGridFile<C,T> compress(
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
                    return grid;                                        // Can not compress.
                }
            }
            data[dim] = compressed;
        }
        return new DatumShiftGridCompressed<>(grid, averages, data, scale);
    }

    /**
     * Returns a new grid with the same geometry than this grid but different data arrays.
     * This method is invoked by {@link #useSharedData()} when it detects that a newly created
     * grid uses the same data than an existing grid. The {@code other} object is the old grid,
     * so we can share existing data.
     */
    @Override
    protected final DatumShiftGridFile<C,T> setData(final Object[] other) {
        return new DatumShiftGridCompressed<>(this, averages, (short[][]) other, scale);
    }

    /**
     * Suggests a precision for the translation values in this grid.
     *
     * @return a precision for the translation values in this grid.
     */
    @Override
    public double getCellPrecision() {
        // 5* is for converting 0.1 × 10⁻ⁿ to 0.5 × 10⁻ⁿ
        // where n is the number of significant digits.
        return Math.min(super.getCellPrecision(), 5*scale);
    }

    /**
     * Returns direct references (not cloned) to the data arrays. This method is for cache management,
     * {@link #equals(Object)} and {@link #hashCode()} implementations only and should not be invoked
     * in other context.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    protected final Object[] getData() {
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
     * @param  dim  the dimension for which to get an average value.
     * @return a value close to the average for the given dimension.
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
        return data[dim][gridX + gridY*scanlineStride] * scale + averages[dim];
    }

    /**
     * Copy of {@link org.apache.sis.referencing.datum.DatumShiftGrid} rewritten in a way that
     * reduce the number of arithmetic operations for efficiency reasons.
     */
    @Override
    public void interpolateInCell(double gridX, double gridY, double[] vector) {
        final int xmax = getGridSize(0) - 2;
        final int ymax = getGridSize(1) - 2;
        int ix = (int) gridX;                               // Really want rounding toward zero (not floor).
        int iy = (int) gridY;
        if (ix < 0 || ix > xmax || iy < 0 || iy > ymax) {
            final double[] gridCoordinates = {gridX, gridY};
            replaceOutsideGridCoordinates(gridCoordinates);
            gridX = gridCoordinates[0];
            gridY = gridCoordinates[1];
            ix = Math.max(0, Math.min(xmax, (int) gridX));
            iy = Math.max(0, Math.min(ymax, (int) gridY));
        }
        gridX -= ix;                                        // If was negative, will continue to be negative.
        gridY -= iy;
        boolean skipX = (gridX < 0); if (skipX) gridX = 0;
        boolean skipY = (gridY < 0); if (skipY) gridY = 0;
        if (gridX > 1)  {gridX = 1; skipX = true;}
        if (gridY > 1)  {gridY = 1; skipY = true;}
        final int p00 = scanlineStride * iy + ix;
        final int p10 = scanlineStride + p00;
        final int n   = data.length;
        boolean derivative = (vector.length >= n + INTERPOLATED_DIMENSIONS * INTERPOLATED_DIMENSIONS);
        for (int dim = 0; dim < n; dim++) {
            double dx, dy;
            final short[] values = data[dim];
            final double r00 = values[p00    ];
            final double r01 = values[p00 + 1];                     // Naming convention: ryx (row index first, like matrix).
            final double r10 = values[p10    ];
            final double r11 = values[p10 + 1];
            final double r0x = r00 + gridX * (dx = r01 - r00);      // TODO: use Math.fma on JDK9.
            final double r1x = r10 + gridX * (dy = r11 - r10);      // Not really "dy" measurement yet, will become dy later.
            vector[dim] = (gridY * (r1x - r0x) + r0x) * scale + averages[dim];
            if (derivative) {
                if (skipX) {
                    dx = 0;
                } else {
                    dx += (dy - dx) * gridX;
                    dx *= scale;
                }
                if (skipY) {
                    dy = 0;
                } else {
                    dy  =  r10 - r00;
                    dy += (r11 - r01 - dy) * gridY;
                    dy *= scale;
                }
                int i = n;
                if (dim == 0) {
                    dx++;
                } else {
                    dy++;
                    i += INTERPOLATED_DIMENSIONS;
                    derivative = false;
                }
                vector[i  ] = dx;
                vector[i+1] = dy;
            }
        }
    }

    /**
     * Returns {@code true} if the given object is a grid containing the same data than this grid.
     *
     * @param  other  the other object to compare with this datum shift grid.
     * @return {@code true} if the given object is non-null, an instance of {@code DatumShiftGridCompressed}
     *         and contains the same data.
     */
    @Override
    public boolean equals(final Object other) {
        if (super.equals(other)) {
            final DatumShiftGridCompressed<?,?> that = (DatumShiftGridCompressed<?,?>) other;
            return Numerics.equals(scale, that.scale) && Arrays.equals(averages, that.averages);
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
