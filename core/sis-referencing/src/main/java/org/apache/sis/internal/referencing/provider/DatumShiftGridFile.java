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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.lang.reflect.Array;
import java.nio.file.Path;
import javax.measure.Unit;
import javax.measure.Quantity;
import javax.measure.quantity.Angle;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.measure.Units;
import org.apache.sis.measure.Longitude;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.util.collection.Cache;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.DefaultTreeTable;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.datum.DatumShiftGrid;
import org.apache.sis.internal.referencing.j2d.AffineTransform2D;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.InterpolatedTransform;


/**
 * A datum shift grid loaded from a file.
 * The filename is usually a parameter defined in the EPSG database.
 * This class should not be in public API because it requires implementation to expose internal mechanic:
 * Subclasses need to give an access to their internal data (not a copy) through the {@link #getData()}
 * and {@link #setData(Object[])} methods. We use that for managing the cache, reducing memory usage by
 * sharing data and for {@link #equals(Object)} and {@link #hashCode()} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param <C>  dimension of the coordinate unit (usually {@link Angle}).
 * @param <T>  dimension of the translation unit. Usually {@link Angle},
 *             but can also be {@link javax.measure.quantity.Length}.
 *
 * @see org.apache.sis.referencing.operation.transform.InterpolatedTransform
 *
 * @since 0.7
 * @module
 */
abstract class DatumShiftGridFile<C extends Quantity<C>, T extends Quantity<T>> extends DatumShiftGrid<C,T> {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5801692909082130314L;

    /**
     * Cache of grids loaded so far. The keys are typically {@link java.nio.file.Path}s or a tuple of paths.
     * Values are grids stored by hard references until the amount of data exceed 32768 (about 128 kilobytes
     * if the values use the {@code float} type), in which case the oldest grids will be replaced by soft references.
     *
     * <h4>Memory consumption</h4>
     * The use of soft references instead of weak references is on the assumption that users typically use
     * the same few Coordinate Reference Systems for their work. Consequently we presume that users will not
     * load a lot of grids and are likely to reuse the already loaded grids.
     */
    static final Cache<Object, DatumShiftGridFile<?,?>> CACHE = new Cache<Object, DatumShiftGridFile<?,?>>(4, 32*1024, true) {
        @Override protected int cost(final DatumShiftGridFile<?,?> grid) {
            int p = 1;
            for (final Object data : grid.getData()) {
                if (data instanceof DatumShiftGridFile<?,?>) {
                    p += cost((DatumShiftGridFile<?,?>) data);          // When `grid` is a DatumShiftGridGroup.
                } else {
                    p *= Array.getLength(data);                         // short[], float[] or double[].
                }
            }
            return p;
        }
    };

    /**
     * The parameter descriptor of the provider that created this grid.
     */
    private final ParameterDescriptorGroup descriptor;

    /**
     * The files from which the grid has been loaded. This is not used directly by this class
     * (except for {@link #equals(Object)} and {@link #hashCode()}), but can be used by math
     * transform for setting the parameter values. Shall never be null and never empty.
     */
    private final Path[] files;

    /**
     * Number of cells between the start of adjacent rows in the grid. This is usually {@code getGridSize(0)},
     * stored as a field for performance reasons. Value could be greater than {@code getGridSize(0)} if there
     * is some elements to ignore at the end of each row.
     */
    protected final int scanlineStride;

    /**
     * Number of cells that the grid would have if it was spanning 360° of longitude, or 0 if no wraparound
     * should be applied. Current implementation rounds to nearest integer on the assumption that we expect
     * an integer number of cells in 360°. This value is used for longitude values that are on the other side
     * of the ±180° meridian compared to the region where the grid is defined.
     *
     * @see #replaceOutsideGridCoordinates(double[])
     */
    private final double periodX;

    /**
     * The best translation accuracy that we can expect from this file.
     * The unit of measurement depends on {@link #isCellValueRatio()}.
     *
     * <p>This field is initialized to zero. It is loader responsibility to assign
     * a value to this field after {@code DatumShiftGridFile} construction.</p>
     *
     * @see #getCellPrecision()
     */
    double accuracy;

    /**
     * The sub-grids, or {@code null} if none. The domain of validity of each sub-grid should be contained
     * in the domain of validity of this grid. Children do not change the way this {@code DatumShiftGrid}
     * performs its calculation; this list is used only at the time of building {@link MathTransform} tree.
     *
     * <div class="note"><b>Design note:</b>
     * we do not provide sub-grids functionality in the {@link DatumShiftGrid} parent class because
     * the {@link MathTransform} tree will depend on assumptions about {@link #getCoordinateToGrid()},
     * in particular that it contains only translations and scales (no rotation, no shear).
     * Those assumptions are enforced by the {@link DatumShiftGridFile} constructor.</div>
     *
     * This field has protected access for usage by {@link DatumShiftGridGroup} subclass only.
     * No access to this field should be done except by subclasses.
     *
     * @see #setSubGrids(Collection)
     */
    protected DatumShiftGridFile<C,T>[] subgrids;

    /**
     * Creates a new datum shift grid for the given grid geometry.
     * The actual offset values need to be provided by subclasses.
     *
     * @param coordinateUnit    the unit of measurement of input values, before conversion to grid indices by {@code coordinateToGrid}.
     * @param translationUnit   the unit of measurement of output values.
     * @param isCellValueRatio  {@code true} if results of {@link #interpolateInCell interpolateInCell(…)} are divided by grid cell size.
     * @param x0                longitude in degrees of the center of the cell at grid index (0,0), positive east.
     * @param y0                latitude in degrees of the center of the cell at grid index (0,0), positive north.
     * @param Δx                increment in <var>x</var> value between cells at index <var>gridX</var> and <var>gridX</var> + 1.
     * @param Δy                increment in <var>y</var> value between cells at index <var>gridY</var> and <var>gridY</var> + 1.
     * @param nx                number of cells along the <var>x</var> axis in the grid.
     * @param ny                number of cells along the <var>y</var> axis in the grid.
     * @param descriptor        the parameter descriptor of the provider that created this grid.
     * @param files             the file(s) from which the grid has been loaded. This array is not cloned.
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
        this.descriptor     = descriptor;
        this.files          = files;
        this.scanlineStride = nx;
        if (Units.isAngular(coordinateUnit)) {
            periodX = Math.rint((Longitude.MAX_VALUE - Longitude.MIN_VALUE) / Math.abs(Δx));
        } else {
            periodX = 0;
            /*
             * Note: non-angular source coordinates are currently never used in this package.
             * If it continue to be like that in the future, we should remove the check for
             * Units.isAngular(…) and replace the C parameterized type by Angle directly.
             */
        }
    }

    /**
     * Creates a new datum shift grid with the same grid geometry than the given grid.
     * This is used by {@link DatumShiftGridCompressed} for replacing a grid by another one.
     *
     * @param  other  the other datum shift grid from which to copy the grid geometry.
     */
    protected DatumShiftGridFile(final DatumShiftGridFile<C,T> other) {
        super(other);
        descriptor     = other.descriptor;
        files          = other.files;
        scanlineStride = other.scanlineStride;
        accuracy       = other.accuracy;
        subgrids       = other.subgrids;
        periodX        = other.periodX;
    }

    /**
     * Creates a new datum shift grid with the same configuration than the given grid,
     * except the size and transform which are set to the given values.
     * This is used for creating a {@link DatumShiftGridGroup} containing many grids,
     * using one grid as a template for setting parameter values.
     * The {@link #accuracy} is initialized to zero and should be updated by the caller.
     *
     * @param  other      the other datum shift grid from which to copy parameters.
     * @param  gridToCRS  conversion from grid indices to "real world" coordinates.
     * @param  nx         number of cells along the <var>x</var> axis in the grid.
     * @param  ny         number of cells along the <var>y</var> axis in the grid.
     */
    DatumShiftGridFile(final DatumShiftGridFile<C,T> other, final AffineTransform2D gridToCRS, final int nx, final int ny)
            throws NoninvertibleTransformException
    {
        super(other.getCoordinateUnit(), gridToCRS.inverse(), new int[] {nx, ny},
              other.isCellValueRatio(), other.getTranslationUnit());
        scanlineStride = nx;
        descriptor     = other.descriptor;
        files          = other.files;
        periodX        = (other.periodX == 0) ? 0 : Math.rint((Longitude.MAX_VALUE - Longitude.MIN_VALUE)
                                                  / AffineTransforms2D.getScaleX0(gridToCRS));
        // Accuracy to be set by caller. Initial value needs to be zero.
    }

    /**
     * Sets the sub-grids that are direct children of this grid.
     * This method can be invoked only once.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    final void setSubGrids(final Collection<DatumShiftGridFile<C,T>> children) {
        if (subgrids != null) throw new IllegalStateException();
        subgrids = children.toArray(new DatumShiftGridFile[children.size()]);
    }

    /**
     * Returns the number of grids, including this grid and all sub-grids counted recursively.
     * This is used for information purpose only.
     *
     * @see #toTree(TreeTable.Node)
     */
    private int getGridCount() {
        int n = 1;
        if (subgrids != null) {
            for (final DatumShiftGridFile<C,T> subgrid : subgrids) {
                n += subgrid.getGridCount();
            }
        }
        return n;
    }

    /**
     * Returns a string representation of this grid for debugging purpose.
     * If this grid has children, then it will be formatted as a tree.
     */
    @Override
    public final String toString() {
        if (subgrids == null) {
            return super.toString();
        }
        final TreeTable tree = new DefaultTreeTable(TableColumn.NAME);
        toTree(tree.getRoot());
        return tree.toString();
    }

    /**
     * Formats this grid as a tree with its children.
     */
    private void toTree(final TreeTable.Node branch) {
        String label = super.toString();
        if (subgrids != null) {
            label = label + " (" + getGridCount() + " grids)";
            for (final DatumShiftGridFile<C,T> subgrid : subgrids) {
                subgrid.toTree(branch.newChild());
            }
        }
        branch.setValue(TableColumn.NAME, label);
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
     * Returns {@code true} if the given object is a grid containing the same data than this grid.
     * This method compares the data provided by {@link #getData()}.
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
     * Returns a hash code value for this datum shift grid. The hash code is based on metadata
     * such as filename, but not on {@link #getData()} for performance reason.
     *
     * @return a hash code based on metadata.
     */
    @Override
    public int hashCode() {
        return super.hashCode() + Arrays.hashCode(files);
    }

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
     * Invoked when a {@code gridX} or {@code gridY} coordinate is outside the range of valid grid coordinates.
     * If the coordinate outside the range is a longitude value and if we handle those values as cyclic, brings
     * that coordinate inside the range.
     */
    @Override
    protected void replaceOutsideGridCoordinates(final double[] gridCoordinates) {
        if (periodX != 0) {
            gridCoordinates[0] = Math.IEEEremainder(gridCoordinates[0], periodX);
        }
    }

    /**
     * Returns the descriptor specified at construction time.
     *
     * @return a description of the values in this grid.
     */
    @Override
    public final ParameterDescriptorGroup getParameterDescriptors() {
        return descriptor;
    }

    /**
     * Sets all parameters for a value of type {@link Path} to the values given to the constructor.
     * Subclasses may override for defining other kinds of parameters too.
     *
     * @param  parameters  the parameter group where to set the values.
     */
    @Override
    public final void getParameterValues(final Parameters parameters) {
        int i = 0;
        for (final GeneralParameterDescriptor gd : descriptor.descriptors()) {
            if (gd instanceof ParameterDescriptor<?>) {
                final ParameterDescriptor<?> d = (ParameterDescriptor<?>) gd;
                if (Path.class.isAssignableFrom(d.getValueClass())) {
                    if (i >= files.length) break;                               // Safety in case of invalid parameters.
                    parameters.getOrCreate(d).setValue(files[i++]);
                }
            }
        }
    }

    /**
     * Creates a transformation between two geodetic CRS, including the sub-grid transforms.
     * If the given grid has no sub-grid, then this method is equivalent to a direct call to
     * {@link InterpolatedTransform#createGeodeticTransformation(MathTransformFactory, DatumShiftGrid)}.
     *
     * @param  provider  the provider which is creating a transform.
     * @param  factory   the factory to use for creating the transform.
     * @param  grid      the grid of datum shifts from source to target datum.
     * @return the transformation between geodetic coordinates.
     * @throws FactoryException if an error occurred while creating a transform.
     *
     * @see InterpolatedTransform#createGeodeticTransformation(MathTransformFactory, DatumShiftGrid)
     */
    public static MathTransform createGeodeticTransformation(final Class<? extends AbstractProvider> provider,
            final MathTransformFactory factory, final DatumShiftGridFile<Angle,Angle> grid) throws FactoryException
    {
        MathTransform global = InterpolatedTransform.createGeodeticTransformation(factory, grid);
        final DatumShiftGridFile<Angle,Angle>[] subgrids = grid.subgrids;
        if (subgrids == null) {
            return global;
        }
        final Map<Envelope,MathTransform> specializations = new LinkedHashMap<>(Containers.hashMapCapacity(subgrids.length));
        for (final DatumShiftGridFile<Angle,Angle> sg : subgrids) try {
            final Envelope domain = sg.getDomainOfValidity(Units.DEGREE);
            final MathTransform st = createGeodeticTransformation(provider, factory, sg);
            if (specializations.putIfAbsent(domain, st) != null) {
                DatumShiftGridLoader.log(provider, Errors.getResources((Locale) null)
                        .getLogRecord(Level.FINE, Errors.Keys.DuplicatedElement_1, domain));
            }
        } catch (TransformException e) {
            throw new FactoryException(e);
        }
        return MathTransforms.specialize(global, specializations);
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
         * The translation values. {@code offsets.length} is the number of dimensions, and {@code offsets[dim].length}
         * shall be the same for all {@code dim} value. Component {@code dim} of the translation vector at coordinate
         * {@code gridX}, {@code gridY} is {@code offsets[dim][gridX + gridY*scanlineStride]}.
         */
        final float[][] offsets;

        /**
         * Creates a new datum shift grid with the given grid geometry, filename and number of shift dimensions.
         * All {@code double} values given to this constructor will be converted from degrees to radians.
         *
         * @param  dim  number of dimensions of translation vectors.
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
            offsets = new float[dim][Math.multiplyExact(nx, ny)];
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
         * This method is invoked by {@link #useSharedData()} when it detects that a newly created
         * grid uses the same data than an existing grid. The {@code other} object is the old grid,
         * so we can share existing data.
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
         * Returns the number of shift dimensions.
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
         * @param  gridX  the grid index along the <var>x</var> axis, from 0 inclusive to {@code nx} exclusive.
         * @param  gridY  the grid index along the <var>y</var> axis, from 0 inclusive to {@code ny} exclusive.
         * @return the offset at the given dimension in the grid cell at the given index.
         */
        @Override
        public final double getCellValue(final int dim, final int gridX, final int gridY) {
            return DecimalFunctions.floatToDouble(offsets[dim][gridX + gridY*scanlineStride]);
        }

        /**
         * Returns the average translation parameters from source to target.
         * There is no need to use double-double arithmetic here since all data have only single precision.
         *
         * @param  dim  the dimension for which to get an average value.
         * @return a value close to the average for the given dimension.
         */
        @Override
        public double getCellMean(final int dim) {
            final float[] data = offsets[dim];
            double sum = 0;
            for (final float value : data) {
                sum += value;
            }
            return sum / data.length;
        }
    }




    /**
     * An implementation of {@link DatumShiftGridFile} which stores the offset values in {@code double[]} arrays.
     * See {@link DatumShiftGridFile.Float} for more information (most comments apply to this class as well).
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @version 1.1
     * @since   1.1
     * @module
     */
    static final class Double<C extends Quantity<C>, T extends Quantity<T>> extends DatumShiftGridFile<C,T> {
        /**
         * Serial number for inter-operability with different versions.
         */
        private static final long serialVersionUID = 3999271636016362364L;

        /**
         * The translation values. See {@link DatumShiftGridFile.Float#offsets} for more documentation.
         */
        final double[][] offsets;

        /**
         * Creates a new datum shift grid with the given grid geometry, filename and number of shift dimensions.
         * All {@code double} values given to this constructor will be converted from degrees to radians.
         */
        Double(final int dim,
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
            offsets = new double[dim][Math.multiplyExact(nx, ny)];
        }

        /**
         * Creates a new grid of the same geometry than the given grid but using a different data array.
         */
        private Double(final DatumShiftGridFile<C,T> grid, final double[][] offsets) {
            super(grid);
            this.offsets = offsets;
        }

        /**
         * Returns a new grid with the same geometry than this grid but different data arrays.
         * See {@link DatumShiftGridFile.Float#setData(Object[])} for more documentation.
         */
        @Override
        protected final DatumShiftGridFile<C,T> setData(final Object[] other) {
            return new Double<>(this, (double[][]) other);
        }

        /**
         * Returns direct references (not cloned) to the data arrays.
         * See {@link DatumShiftGridFile.Float#getData()} for more documentation.
         */
        @Override
        @SuppressWarnings("ReturnOfCollectionOrArrayField")
        protected final Object[] getData() {
            return offsets;
        }

        /**
         * Returns the number of shift dimensions.
         */
        @Override
        public final int getTranslationDimensions() {
            return offsets.length;
        }

        /**
         * Returns the cell value at the given dimension and grid index.
         * See {@link DatumShiftGridFile.Float#getCellValue(int, int, int)} for more documentation.
         */
        @Override
        public final double getCellValue(final int dim, final int gridX, final int gridY) {
            return offsets[dim][gridX + gridY*scanlineStride];
        }
    }
}
