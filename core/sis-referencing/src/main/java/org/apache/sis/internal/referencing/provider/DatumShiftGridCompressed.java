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


/**
 * A datum shift grid which store the values in {@code short[]} array.
 * In addition to using half the space of {@code float[]} arrays, it can also (ironically)
 * increase the precision in the common case where the shifts are specified with no more than
 * 5 digits in base 10 in ASCII files.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class DatumShiftGridCompressed extends DatumShiftGridFile {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4847888093457104917L;

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
    private DatumShiftGridCompressed(final DatumShiftGridFile grid, final double[] averages,
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
     * @param  grid      The grid to compress.
     * @param  averages  An "average" value for the offset in each dimension, or {@code null} if unknown.
     * @param  scale     The factor by which to multiply each compressed value before to add to the average value.
     * @return The grid to use (may or may not be compressed).
     */
    static DatumShiftGridFile compress(final DatumShiftGridFile.Float grid, double[] averages, final double scale) {
        final short[][] data = new short[grid.offsets.length][];
        final boolean computeAverages = (averages == null);
        if (computeAverages) {
            averages = new double[data.length];
        }
        for (int dim = 0; dim < data.length; dim++) {
            if (computeAverages) {
                averages[dim] = grid.getAverageOffset(dim);
            }
            final double average = averages[dim];
            final float[] offsets = grid.offsets[dim];
            final short[] compressed = new short[offsets.length];
            for (int i=0; i<offsets.length; i++) {
                final double c = (offsets[i] - average) / scale;
                final double e = (compressed[i] = (short) c) - c;
                if (!(Math.abs(e) < Math.ulp((float) c))) {         // Use '!' for catching NaN values.
                    return grid;    // Can not compress.
                }
            }
            data[dim] = compressed;
        }
        return new DatumShiftGridCompressed(grid, averages, data, scale);
    }

    /**
     * Returns a new grid with the same geometry than this grid but different data arrays.
     */
    @Override
    final DatumShiftGridFile setData(final Object[] other) {
        return new DatumShiftGridCompressed(this, averages, (short[][]) other, scale);
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
    public final int getShiftDimensions() {
        return data.length;
    }

    /**
     * Returns the average translation parameters from source to target.
     *
     * @param dim The dimension for which to get an average value.
     * @return A value close to the average for the given dimension.
     */
    @Override
    public double getAverageOffset(final int dim) {
        return averages[dim];
    }

    /**
     * Returns the cell value at the given grid index.
     */
    @Override
    protected double getCellValue(final int dim, final int gridX, final int gridY) {
        return data[dim][gridX + gridY*nx] * scale + averages[dim];
    }
}
