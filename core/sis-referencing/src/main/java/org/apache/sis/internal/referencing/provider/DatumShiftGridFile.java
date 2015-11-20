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
import java.lang.reflect.Array;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.referencing.datum.DatumShiftGrid;

// Branch-specific imports
import java.nio.file.Path;


/**
 * A datum shift grid loaded from a file.
 * The filename is usually a parameter defined in the EPSG database.
 *
 * <p>This class is in internal package (not public API) because it makes the following assumptions:</p>
 * <ul>
 *   <li>Single floating-point precision ({@code float)} is sufficient.</li>
 *   <li>Values were defined in base 10, usually in ASCII files. This assumption has an impact on conversions
 *       from {@code float} to {@code double} performed by the {@link #getCellValue(int, int, int)} method.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public abstract class DatumShiftGridFile extends DatumShiftGrid {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4471670781277328193L;

    /**
     * Cache of grids loaded so far. Those grids will be stored by soft references until the amount of
     * data exceed 32768 (about 128 kilobytes if the values use the {@code float} type). in which case
     * the oldest grids will be replaced by weak references.
     */
    static final Cache<Path, DatumShiftGridFile> CACHE = new Cache<Path, DatumShiftGridFile>(4, 32*1024, true) {
        @Override protected int cost(final DatumShiftGridFile grid) {
            int p = 1;
            for (final Object array : grid.getData()) {
                p *= Array.getLength(array);
            }
            return p;
        }
    };

    /**
     * The file from which the grid has been loaded. This is not used directly by this class
     * (except for {@link #equals(Object)} and {@link #hashCode()}), but can be used by math
     * transform for setting the parameter values.
     */
    public final Path file;

    /**
     * Creates a new datum shift grid for the given grid geometry.
     * The actual offset values need to be provided by subclasses.
     *
     * @param x0  First ordinate (often longitude in radians) of the center of the cell at grid index (0,0).
     * @param y0  Second ordinate (often latitude in radians) of the center of the cell at grid index (0,0).
     * @param Δx  Increment in <var>x</var> value between cells at index <var>gridX</var> and <var>gridX</var> + 1.
     * @param Δy  Increment in <var>y</var> value between cells at index <var>gridY</var> and <var>gridY</var> + 1.
     * @param nx  Number of cells along the <var>x</var> axis in the grid.
     * @param ny  Number of cells along the <var>y</var> axis in the grid.
     */
    DatumShiftGridFile(final double x0, final double y0,
                       final double Δx, final double Δy,
                       final int    nx, final int    ny,
                       final Path file)
    {
        super(x0, y0, Δx, Δy, nx, ny);
        this.file = file;
    }

    /**
     * Creates a new datum shift grid with the same grid geometry than the given grid.
     *
     * @param other The other datum shift grid from which to copy the grid geometry.
     */
    DatumShiftGridFile(final DatumShiftGridFile other) {
        super(other);
        this.file = other.file;
    }

    /**
     * If a grid exists in the cache for the same data, returns a new grid sharing the same data arrays.
     * Otherwise returns {@code this}.
     */
    final DatumShiftGridFile useSharedData() {
        final Object[] data = getData();
        for (final DatumShiftGridFile grid : CACHE.values()) {
            final Object[] other = grid.getData();
            if (Arrays.deepEquals(data, other)) {
                return setData(other);
            }
        }
        return this;
    }

    /**
     * Returns a new grid with the same geometry than this grid but different data arrays.
     * This method is invoked by {@link #useSharedData()} when it detected that a newly created grid uses
     * the same data than an existing grid. The typical use case is when a filename is different but still
     * reference the same grid (e.g. symbolic link, lower case versus upper case in a case-insensitive file
     * system).
     */
    abstract DatumShiftGridFile setData(Object[] other);

    /**
     * Returns the data for each shift dimensions.
     */
    abstract Object[] getData();

    /**
     * Returns the value to shown in {@code PARAMETER} WKT elements.
     * Current implementation returns the grid filename.
     *
     * @return The grid filename.
     */
    @Override
    public String toString() {
        return file.getFileName().toString();
    }




    /**
     * An implementation of {@link DatumShiftGridFile} which stores the offset values in {@code float[]} arrays.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.7
     * @version 0.7
     * @module
     */
    static class Float extends DatumShiftGridFile {
        /**
         * Serial number for inter-operability with different versions.
         */
        private static final long serialVersionUID = -9221609983475286496L;

        /**
         * The translation values.
         */
        final float[][] offsets;

        /**
         * Creates a new datum shift grid with the given grid geometry, filename and number of shift dimensions.
         */
        Float(final double x0, final double y0,
              final double Δx, final double Δy,
              final int    nx, final int    ny,
              final Path file, final int dim)
        {
            super(x0, y0, Δx, Δy, nx, ny, file);
            offsets = new float[dim][];
            final int size = Math.multiplyExact(nx, ny);
            for (int i=0; i<dim; i++) {
                Arrays.fill(offsets[i] = new float[size], java.lang.Float.NaN);
            }
        }

        /**
         * Creates a new grid of the same geometry than the given grid but using a different data array.
         */
        private Float(final DatumShiftGridFile grid, final float[][] offsets) {
            super(grid);
            this.offsets = offsets;
        }

        /**
         * Returns a new grid with the same geometry than this grid but different data arrays.
         */
        @Override
        final DatumShiftGridFile setData(final Object[] other) {
            return new Float(this, (float[][]) other);
        }

        /**
         * Returns direct references (not cloned) to the data arrays.
         */
        @Override
        @SuppressWarnings("ReturnOfCollectionOrArrayField")
        final Object[] getData() {
            return offsets;
        }

        /**
         * Returns the number of shift dimension.
         */
        @Override
        public final int getShiftDimensions() {
            return offsets.length;
        }

        /**
         * Returns the cell value at the given dimension and grid index.
         * This method casts the {@code float} values to {@code double} by setting the extra <em>decimal</em> digits
         * (not the <em>binary</em> digits) to 0. This is on the assumption that the {@code float} values were parsed
         * from an ASCII file, or any other medium that format numbers in base 10.
         *
         * @param dim    The dimension for which to get an average value.
         * @param gridX  The grid index along the <var>x</var> axis, from 0 inclusive to {@link #nx} exclusive.
         * @param gridY  The grid index along the <var>y</var> axis, from 0 inclusive to {@link #ny} exclusive.
         * @return The offset at the given dimension in the grid cell at the given index.
         */
        @Override
        protected final double getCellValue(final int dim, final int gridX, final int gridY) {
            return DecimalFunctions.floatToDouble(offsets[dim][gridX + gridY*nx]);
        }
    }
}
