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
import java.nio.file.Path;
import javax.measure.Unit;
import javax.measure.Quantity;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.datum.DatumShiftGrid;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.internal.util.Utilities;


/**
 * A datum shift grid loaded from a file.
 * The filename is usually a parameter defined in the EPSG database.
 * This class should not be in public API because it requires implementation to expose internal mechanic:
 *
 * <ul>
 *   <li>Subclasses need to give an access to their internal data (not a copy) through the {@link #getData()}
 *       and {@link #setData(Object[])} methods. We use that for managing the cache, reducing memory usage by
 *       sharing data and for {@link #equals(Object)} and {@link #hashCode()} implementations.</li>
 *   <li>{@link #descriptor}, {@link #gridToTarget()} and {@link #setFileParameters(Parameters)} are convenience
 *       members for {@link org.apache.sis.referencing.operation.transform.InterpolatedTransform} constructor.
 *       What they do are closely related to how {@code InterpolatedTransform} works, and trying to document that
 *       in a public API would probably be too distracting for the users.</li>
 * </ul>
 *
 * The main concrete subclass is {@link DatumShiftGridFile.Float}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @param <C>  dimension of the coordinate unit (usually {@link javax.measure.quantity.Angle}).
 * @param <T>  dimension of the translation unit (usually {@link javax.measure.quantity.Angle}
 *             or {@link javax.measure.quantity.Length}).
 *
 * @see org.apache.sis.referencing.operation.transform.InterpolatedTransform
 *
 * @since 0.7
 * @module
 */
public abstract class DatumShiftGridFile<C extends Quantity<C>, T extends Quantity<T>> extends DatumShiftGrid<C,T> {
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
    protected final int nx;

    /**
     * The best translation accuracy that we can expect from this file.
     * The unit of measurement depends on {@link #isCellValueRatio()}.
     *
     * <p>This field is initialized to {@link Double#NaN}. It is loader responsibility
     * to assign a value to this field after {@code DatumShiftGridFile} construction.</p>
     *
     * @see #getCellPrecision()
     */
    protected double accuracy;

    /**
     * Creates a new datum shift grid for the given grid geometry.
     * The actual offset values need to be provided by subclasses.
     *
     * @param  coordinateUnit    the unit of measurement of input values, before conversion to grid indices by {@code coordinateToGrid}.
     * @param  translationUnit   the unit of measurement of output values.
     * @param  isCellValueRatio  {@code true} if results of {@link #interpolateInCell interpolateInCell(…)} are divided by grid cell size.
     * @param  coordinateToGrid  conversion from the "real world" coordinates to grid indices including fractional parts.
     * @param  nx                number of cells along the <var>x</var> axis in the grid.
     * @param  ny                number of cells along the <var>y</var> axis in the grid.
     * @param  descriptor        the parameter descriptor of the provider that created this grid.
     * @param  files             the file(s) from which the grid has been loaded.
     *
     * @since 0.8
     */
    protected DatumShiftGridFile(final Unit<C> coordinateUnit,
                                 final Unit<T> translationUnit,
                                 final boolean isCellValueRatio,
                                 final LinearTransform coordinateToGrid,
                                 final int nx, final int ny,
                                 final ParameterDescriptorGroup descriptor,
                                 final Path... files)
    {
        super(coordinateUnit, coordinateToGrid, new int[] {nx, ny}, isCellValueRatio, translationUnit);
        this.descriptor = descriptor;
        this.files      = files;
        this.nx         = nx;
        this.accuracy   = Double.NaN;
    }

    /**
     * Creates a new datum shift grid for the given grid geometry.
     * The actual offset values need to be provided by subclasses.
     *
     * @param x0  longitude in degrees of the center of the cell at grid index (0,0).
     * @param y0  latitude in degrees of the center of the cell at grid index (0,0).
     * @param Δx  increment in <var>x</var> value between cells at index <var>gridX</var> and <var>gridX</var> + 1.
     * @param Δy  increment in <var>y</var> value between cells at index <var>gridY</var> and <var>gridY</var> + 1.
     * @param nx  number of cells along the <var>x</var> axis in the grid.
     * @param ny  number of cells along the <var>y</var> axis in the grid.
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
        this(coordinateUnit, translationUnit, isCellValueRatio,
                new AffineTransform2D(Δx, 0, 0, Δy, x0, y0).inverse(), nx, ny, descriptor, files);
    }

    /**
     * Creates a new datum shift grid with the same grid geometry than the given grid.
     *
     * @param  other  the other datum shift grid from which to copy the grid geometry.
     */
    protected DatumShiftGridFile(final DatumShiftGridFile<C,T> other) {
        super(other);
        descriptor = other.descriptor;
        files      = other.files;
        nx         = other.nx;
        accuracy   = other.accuracy;
    }

    /**
     * Returns {@code this} casted to the given type, after verification that those types are valid.
     * This method is invoked after {@link NADCON}, {@link NTv2} or other providers got an existing
     * {@code DatumShiftGridFile} instance from the {@link #CACHE}.
     */
    @SuppressWarnings("unchecked")
    final <NC extends Quantity<NC>, NT extends Quantity<NT>> DatumShiftGridFile<NC,NT> castTo(
            final Class<NC> coordinateType, final Class<NT> translationType)
    {
        super.getCoordinateUnit() .asType(coordinateType);
        super.getTranslationUnit().asType(translationType);
        return (DatumShiftGridFile<NC,NT>) this;
    }

    /**
     * If a grid exists in the cache for the same data, returns a new grid sharing the same data arrays.
     * Otherwise returns {@code this}.
     *
     * @return a grid using the same data than this grid, or {@code this}.
     *
     * @see #getData()
     * @see #setData(Object[])
     */
    protected final DatumShiftGridFile<C,T> useSharedData() {
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
     *
     * @param  other  data from another {@code DatumShiftGridFile} that we can share.
     * @return a new {@code DatumShiftGridFile} using the given data reference.
     */
    protected abstract DatumShiftGridFile<C,T> setData(Object[] other);

    /**
     * Returns the data for each shift dimensions. This method is for cache management, {@link #equals(Object)}
     * and {@link #hashCode()} implementations only and should not be invoked in other context.
     *
     * @return a direct (not cloned) reference to the internal data array.
     */
    protected abstract Object[] getData();

    /**
     * Suggests a precision for the translation values in this grid.
     * This information is used for deciding when to stop iterations in inverse transformations.
     * The default implementation returns the {@linkplain #accuracy} divided by an arbitrary value.
     *
     * @return a precision for the translation values in this grid.
     */
    @Override
    public double getCellPrecision() {
        return accuracy / 10;               // Division by 10 is arbitrary.
    }

    /**
     * Returns the transform from grid coordinates to "real world" coordinates after the datum shift has been applied,
     * or {@code null} for the default. This is usually the inverse of the transform from "real world" coordinates to
     * grid coordinates before datum shift, since NADCON and NTv2 transformations have source and target coordinates
     * in the same coordinate system (with axis units in degrees). But this method may be overridden by subclasses that
     * use {@code DatumShiftGridFile} for other kind of transformations.
     *
     * @return the transformation from grid coordinates to "real world" coordinates after datum shift,
     *         or {@code null} for the default (namely the inverse of the "source to grid" transformation).
     */
    public Matrix gridToTarget() {
        return null;
    }

    /**
     * Sets all parameters for a value of type {@link Path} to the values given to th constructor.
     *
     * @param  parameters  the parameter group where to set the values.
     */
    public final void setFileParameters(final Parameters parameters) {
        if (files.length != 0) {
            int i = 0;
            for (final GeneralParameterDescriptor gd : descriptor.descriptors()) {
                if (gd instanceof ParameterDescriptor<?>) {
                    final ParameterDescriptor<?> d = (ParameterDescriptor<?>) gd;
                    if (Path.class.isAssignableFrom(d.getValueClass())) {
                        parameters.getOrCreate(d).setValue(files[i]);
                        if (++i == files.length) break;
                    }
                }
            }
        }
    }

    /**
     * Returns {@code true} if the given object is a grid containing the same data than this grid.
     *
     * @param  other  the other object to compare with this datum shift grid.
     * @return {@code true} if the given object is non-null, of the same class than this {@code DatumShiftGrid}
     *         and contains the same data.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {                        // Optimization for a common case.
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
     * @return a string representation for debugging purpose.
     */
    @Override
    public String toString() {
        return Utilities.toString(getClass(), "file", (files.length != 0) ? files[0] : null);
    }




    /**
     * An implementation of {@link DatumShiftGridFile} which stores the offset values in {@code float[]} arrays.
     * This class is in internal package (not public API) because it makes the following assumptions:
     * <ul>
     *   <li>Values <var>x₀</var>, <var>y₀</var>, <var>Δx</var> and <var>Δy</var>
     *       given to the constructor are in degrees and needs to be converted to radians.</li>
     *   <li>Single floating-point precision ({@code float)} is sufficient.</li>
     *   <li>Values were defined in base 10, usually in ASCII files. This assumption has an impact on conversions
     *       from {@code float} to {@code double} performed by the {@link #getCellValue(int, int, int)} method.</li>
     * </ul>
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 0.7
     * @since   0.7
     * @module
     */
    static final class Float<C extends Quantity<C>, T extends Quantity<T>> extends DatumShiftGridFile<C,T> {
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
            final int size = Math.multiplyExact(nx, ny);
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
        protected final DatumShiftGridFile<C,T> setData(final Object[] other) {
            return new Float<>(this, (float[][]) other);
        }

        /**
         * Returns direct references (not cloned) to the data arrays. This method is for cache management,
         * {@link #equals(Object)} and {@link #hashCode()} implementations only and should not be invoked
         * in other context.
         */
        @Override
        @SuppressWarnings("ReturnOfCollectionOrArrayField")
        protected final Object[] getData() {
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
         * @param  dim    the dimension for which to get an average value.
         * @param  gridX  the grid index along the <var>x</var> axis, from 0 inclusive to {@link #nx} exclusive.
         * @param  gridY  the grid index along the <var>y</var> axis, from 0 inclusive to {@code  ny} exclusive.
         * @return the offset at the given dimension in the grid cell at the given index.
         */
        @Override
        public final double getCellValue(final int dim, final int gridX, final int gridY) {
            return DecimalFunctions.floatToDouble(offsets[dim][gridX + gridY*nx]);
        }
    }
}
