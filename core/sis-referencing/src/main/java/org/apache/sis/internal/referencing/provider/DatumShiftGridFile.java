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
import javax.measure.unit.Unit;
import javax.measure.quantity.Quantity;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.Debug;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.datum.DatumShiftGrid;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;

// Branch-specific imports
import java.io.File;
import org.apache.sis.internal.jdk7.Path;
import org.apache.sis.internal.jdk8.JDK8;


/**
 * A datum shift grid loaded from a file.
 * The filename is usually a parameter defined in the EPSG database.
 *
 * <p>This class is in internal package (not public API) because it makes the following assumptions:</p>
 * <ul>
 *   <li>Values <var>x₀</var>, <var>y₀</var>, <var>Δx</var> and <var>Δy</var>
 *       given to the constructor are in degrees and needs to be converted to radians.</li>
 *   <li>Single floating-point precision ({@code float)} is sufficient.</li>
 *   <li>Values were defined in base 10, usually in ASCII files. This assumption has an impact on conversions
 *       from {@code float} to {@code double} performed by the {@link #getCellValue(int, int, int)} method.</li>
 * </ul>
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
public abstract class DatumShiftGridFile<C extends Quantity, T extends Quantity> extends DatumShiftGrid<C,T> {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4471670781277328193L;

    /**
     * Cache of grids loaded so far. Those grids will be stored by soft references until the amount of
     * data exceed 32768 (about 128 kilobytes if the values use the {@code float} type). in which case
     * the oldest grids will be replaced by weak references.
     */
    static final Cache<Object, DatumShiftGridFile<?,?>> CACHE = new Cache<Object, DatumShiftGridFile<?,?>>(4, 32*1024, true) {
        @Override protected int cost(final DatumShiftGridFile<?,?> grid) {
            int p = 1;
            for (final Object array : grid.getData()) {
                p *= Array.getLength(array);
            }
            return p;
        }
    };

    /**
     * The parameter descriptor of the provider that created this grid.
     */
    public final ParameterDescriptorGroup descriptor;

    /**
     * The files from which the grid has been loaded. This is not used directly by this class
     * (except for {@link #equals(Object)} and {@link #hashCode()}), but can be used by math
     * transform for setting the parameter values.
     */
    private final Path[] files;

    /**
     * Number of grid cells along the <var>x</var> axis.
     */
    final int nx;

    /**
     * The best translation accuracy that we can expect from this file.
     *
     * <p>This field is initialized to {@link Double#NaN}. It is loader responsibility
     * to assign a value to this field after {@code DatumShiftGridFile} construction.</p>
     *
     * @see #getCellPrecision()
     */
    double accuracy;

    /**
     * Creates a new datum shift grid for the given grid geometry.
     * The actual offset values need to be provided by subclasses.
     *
     * @param x0  Longitude in degrees of the center of the cell at grid index (0,0).
     * @param y0  Latitude in degrees of the center of the cell at grid index (0,0).
     * @param Δx  Increment in <var>x</var> value between cells at index <var>gridX</var> and <var>gridX</var> + 1.
     * @param Δy  Increment in <var>y</var> value between cells at index <var>gridY</var> and <var>gridY</var> + 1.
     * @param nx  Number of cells along the <var>x</var> axis in the grid.
     * @param ny  Number of cells along the <var>y</var> axis in the grid.
     */
    DatumShiftGridFile(final Unit<C> coordinateUnit,
                       final Unit<T> translationUnit,
                       final boolean isCellValueRatio,
                       final double x0, final double y0,
                       final double Δx, final double Δy,
                       final int    nx, final int    ny,
                       final ParameterDescriptorGroup descriptor,
                       final Path... files) throws NoninvertibleTransformException
    {
        super(coordinateUnit, new AffineTransform2D(Δx, 0, 0, Δy, x0, y0).inverse(),
                new int[] {nx, ny}, isCellValueRatio, translationUnit);
        this.descriptor = descriptor;
        this.files      = files;
        this.nx         = nx;
        this.accuracy   = Double.NaN;
        if (files.length == 0) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Creates a new datum shift grid with the same grid geometry than the given grid.
     *
     * @param other The other datum shift grid from which to copy the grid geometry.
     */
    DatumShiftGridFile(final DatumShiftGridFile<C,T> other) {
        super(other);
        descriptor = other.descriptor;
        files      = other.files;
        nx         = other.nx;
        accuracy   = other.accuracy;
    }

    /**
     * Suggests a precision for the translation values in this grid.
     * The default implementation returns a value smaller than the accuracy.
     *
     * @return A precision for the translation values in this grid.
     */
    @Override
    public double getCellPrecision() {
        return accuracy / 10;   // Division by 10 is arbitrary.
    }

    /**
     * If a grid exists in the cache for the same data, returns a new grid sharing the same data arrays.
     * Otherwise returns {@code this}.
     */
    final DatumShiftGridFile<C,T> useSharedData() {
        final Object[] data = getData();
        for (final DatumShiftGridFile<?,?> grid : CACHE.values()) {
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
    abstract DatumShiftGridFile<C,T> setData(Object[] other);

    /**
     * Returns the data for each shift dimensions.
     */
    abstract Object[] getData();

    /**
     * Sets all parameters for a value of type {@link Path} to the values given to th constructor.
     *
     * @param parameters The parameter group where to set the values.
     */
    public final void setFileParameters(final Parameters parameters) {
        int i = 0;  // The 'files' array should always contains at least one element.
        for (final GeneralParameterDescriptor gd : descriptor.descriptors()) {
            if (gd instanceof ParameterDescriptor<?>) {
                final ParameterDescriptor<?> d = (ParameterDescriptor<?>) gd;
                if (File.class.isAssignableFrom(d.getValueClass())) {
                    parameters.getOrCreate(d).setValue(files[i]);
                    if (++i == files.length) break;
                }
            }
        }
    }

    /**
     * Returns {@code this} casted to the given type, after verification that those types are valid.
     */
    @SuppressWarnings("unchecked")
    final <NC extends Quantity, NT extends Quantity> DatumShiftGridFile<NC,NT> castTo(
            final Class<NC> coordinateType, final Class<NT> translationType)
    {
        super.getCoordinateUnit() .asType(coordinateType);
        super.getTranslationUnit().asType(translationType);
        return (DatumShiftGridFile<NC,NT>) this;
    }

    /**
     * Returns {@code true} if the given object is a grid containing the same data than this grid.
     *
     * @param  other The other object to compare with this datum shift grid.
     * @return {@code true} if the given object is non-null, of the same class than this {@code DatumShiftGrid}
     *         and contains the same data.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {    // Optimization for a common case.
            return true;
        }
        if (super.equals(other)) {
            final DatumShiftGridFile<?,?> that = (DatumShiftGridFile<?,?>) other;
            return Arrays.equals(files, that.files) && Arrays.deepEquals(getData(), that.getData());
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
        return super.hashCode() + Arrays.hashCode(files);
    }

    /**
     * Returns a string representation of this grid.
     *
     * @return A string representation for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        return "DatumShiftGrid[\"" + files[0].getFileName() + "\"]";
    }




    /**
     * An implementation of {@link DatumShiftGridFile} which stores the offset values in {@code float[]} arrays.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.7
     * @version 0.7
     * @module
     */
    static final class Float<C extends Quantity, T extends Quantity> extends DatumShiftGridFile<C,T> {
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
         * All {@code double} values given to this constructor will be converted from degrees to radians.
         */
        Float(final int dim,
              final Unit<C> coordinateUnit,
              final Unit<T> translationUnit,
              final boolean isCellValueRatio,
              final double x0, final double y0,
              final double Δx, final double Δy,
              final int    nx, final int    ny,
              final ParameterDescriptorGroup descriptor,
              final Path... files) throws NoninvertibleTransformException
        {
            super(coordinateUnit, translationUnit, isCellValueRatio, x0, y0, Δx, Δy, nx, ny, descriptor, files);
            offsets = new float[dim][];
            final int size = JDK8.multiplyExact(nx, ny);
            for (int i=0; i<dim; i++) {
                Arrays.fill(offsets[i] = new float[size], java.lang.Float.NaN);
            }
        }

        /**
         * Creates a new grid of the same geometry than the given grid but using a different data array.
         */
        private Float(final DatumShiftGridFile<C,T> grid, final float[][] offsets) {
            super(grid);
            this.offsets = offsets;
        }

        /**
         * Returns a new grid with the same geometry than this grid but different data arrays.
         */
        @Override
        final DatumShiftGridFile<C,T> setData(final Object[] other) {
            return new Float<C,T>(this, (float[][]) other);
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
        public final int getTranslationDimensions() {
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
        public final double getCellValue(final int dim, final int gridX, final int gridY) {
            return DecimalFunctions.floatToDouble(offsets[dim][gridX + gridY*nx]);
        }
    }
}
