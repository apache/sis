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
package org.apache.sis.referencing.operation.gridded;

import java.util.Arrays;
import java.util.Collection;
import java.util.AbstractMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.concurrent.Callable;
import java.lang.reflect.Array;
import java.net.URI;
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
import org.apache.sis.util.resources.Errors;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.datum.DatumShiftGrid;
import org.apache.sis.referencing.operation.provider.AbstractProvider;
import org.apache.sis.referencing.operation.matrix.AffineTransforms2D;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.InterpolatedTransform;
import org.apache.sis.referencing.privy.AffineTransform2D;
import org.apache.sis.pending.jdk.JDK19;


/**
 * A datum shift grid fully loaded in memory from a file.
 * The filename is usually a parameter defined in the EPSG database.
 * This class should not be in public API because it requires implementation to expose internal mechanic:
 * Subclasses need to give an access to their internal data (not a copy) through the {@link #getData()}
 * and {@link #setData(Object[])} methods. We use that for managing the cache, reducing memory usage by
 * sharing data and for {@link #equals(Object)} and {@link #hashCode()} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param <C>  dimension of the coordinate unit (usually {@link Angle}).
 * @param <T>  dimension of the translation unit. Usually {@link Angle},
 *             but can also be {@link javax.measure.quantity.Length}.
 *
 * @see org.apache.sis.referencing.operation.transform.InterpolatedTransform
 */
public abstract class LoadedGrid<C extends Quantity<C>, T extends Quantity<T>> extends DatumShiftGrid<C,T> {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1690433946781367085L;

    /**
     * Cache of grids loaded so far. The keys are typically {@link URI}s or a tuple of URIs.
     * Values are grids stored by hard references until the amount of data exceed 32768 (about 128 kilobytes
     * if the values use the {@code float} type), in which case the oldest grids will be replaced by soft references.
     *
     * <h4>Memory consumption</h4>
     * The use of soft references instead of weak references is on the assumption that users typically use
     * the same few Coordinate Reference Systems for their work. Consequently, we presume that users will not
     * load a lot of grids and are likely to reuse the already loaded grids.
     *
     * @see #getOrLoad(URI, URI, Callable)
     */
    private static final Cache<Object, LoadedGrid<?,?>> CACHE = new Cache<Object, LoadedGrid<?,?>>(4, 32*1024, true) {
        @Override protected int cost(final LoadedGrid<?,?> grid) {
            int p = 1;
            for (final Object data : grid.getData()) {
                if (data instanceof LoadedGrid<?,?>) {
                    p += cost((LoadedGrid<?,?>) data);      // When `grid` is a GridGroup.
                } else {
                    p *= Array.getLength(data);             // short[], float[] or double[].
                }
            }
            return p;
        }
    };

    /**
     * The parameter descriptor of the provider that created this grid.
     */
    @SuppressWarnings("serial")                     // Most SIS implementations are serializable.
    private final ParameterDescriptorGroup descriptor;

    /**
     * The files from which the grid has been loaded. This is not used directly by this class
     * (except for {@link #equals(Object)} and {@link #hashCode()}), but can be used by math
     * transform for setting the parameter values. Shall never be null and never empty.
     *
     * @see <a href="https://issues.apache.org/jira/browse/SIS-569">SIS-569</a>
     */
    private final URI[] files;

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
     * a value to this field after {@code LoadedGrid} construction.</p>
     *
     * @see #getCellPrecision()
     */
    public double accuracy;

    /**
     * The sub-grids, or {@code null} if none. The domain of validity of each sub-grid should be contained
     * in the domain of validity of this grid. Children do not change the way this {@code DatumShiftGrid}
     * performs its calculation; this list is used only at the time of building {@link MathTransform} tree.
     *
     * <h4>Design note</h4>
     * we do not provide sub-grids functionality in the {@link DatumShiftGrid} parent class because
     * the {@link MathTransform} tree will depend on assumptions about {@link #getCoordinateToGrid()},
     * in particular that it contains only translations and scales (no rotation, no shear).
     * Those assumptions are enforced by the {@link LoadedGrid} constructor.
     *
     * <p>This field has package access for usage by {@link GridGroup} subclass only.
     * No access to this field should be done except by subclasses.</p>
     *
     * @see #setSubGrids(Collection)
     */
    LoadedGrid<C,T>[] subgrids;

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
     * @param files             the file(s) from which the grid has been loaded.
     */
    LoadedGrid(final Unit<C> coordinateUnit,
               final Unit<T> translationUnit,
               final boolean isCellValueRatio,
               final double x0, final double y0,
               final double Δx, final double Δy,
               final int    nx, final int    ny,
               final ParameterDescriptorGroup descriptor,
               final GridFile... sources) throws NoninvertibleTransformException
    {
        super(coordinateUnit, new AffineTransform2D(Δx, 0, 0, Δy, x0, y0).inverse(),
              new int[] {nx, ny}, isCellValueRatio, translationUnit);
        files = new URI[sources.length];
        for (int i=0; i<sources.length; i++) {
            files[i] = sources[i].resolved();
        }
        this.descriptor = descriptor;
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
     * Creates a new datum shift grid with the same grid geometry as the given grid.
     * This is used by {@link CompressedGrid} for replacing a grid by another one.
     *
     * @param  other  the other datum shift grid from which to copy the grid geometry.
     */
    LoadedGrid(final LoadedGrid<C,T> other) {
        super(other);
        descriptor     = other.descriptor;
        files          = other.files;
        scanlineStride = other.scanlineStride;
        accuracy       = other.accuracy;
        subgrids       = other.subgrids;
        periodX        = other.periodX;
    }

    /**
     * Creates a new datum shift grid with the same configuration as the given grid,
     * except the size and transform which are set to the given values.
     * This is used for creating a {@link GridGroup} containing many grids,
     * using one grid as a template for setting parameter values.
     * The {@link #accuracy} is initialized to zero and should be updated by the caller.
     *
     * @param  other      the other datum shift grid from which to copy parameters.
     * @param  gridToCRS  conversion from grid indices to "real world" coordinates.
     * @param  nx         number of cells along the <var>x</var> axis in the grid.
     * @param  ny         number of cells along the <var>y</var> axis in the grid.
     */
    LoadedGrid(final LoadedGrid<C,T> other, final AffineTransform2D gridToCRS, final int nx, final int ny)
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
     * Gets the grid from the cache if available, or loads it.
     *
     * @param  f1      the main file to load.
     * @param  f2      a second file to load, or {@code null} if none.
     * @param  loader  the loader to execute if the grid is not in the cache.
     * @return the cached or loaded grid.
     * @throws Exception if an error occurred while loading the grid.
     *         Caller should handle the exception with {@code canNotLoad(…)}.
     *
     * @see GridLoader#canNotLoad(Class, String, URI, Exception)
     */
    public static LoadedGrid<?,?> getOrLoad(final GridFile f1, final GridFile f2, final Callable<LoadedGrid<?,?>> loader)
            throws Exception
    {
        Object key = f1.resolved();
        if (f2 != null) {
            key = new AbstractMap.SimpleImmutableEntry<>(key, f2.resolved());
        }
        return CACHE.getOrCreate(key, loader);
    }

    /**
     * Sets the sub-grids that are direct children of this grid.
     * This method can be invoked only once.
     *
     * @param  children  the sub-grids that are direct children of this grid.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public final void setSubGrids(final Collection<LoadedGrid<C,T>> children) {
        if (subgrids != null) throw new IllegalStateException();
        subgrids = children.toArray(LoadedGrid[]::new);
    }

    /**
     * Returns the number of grids, including this grid and all sub-grids counted recursively.
     * This is used for information purpose only.
     *
     * @return number of grids, including children.
     *
     * @see #toTree(TreeTable.Node)
     */
    private int getGridCount() {
        int n = 1;
        if (subgrids != null) {
            for (final LoadedGrid<C,T> subgrid : subgrids) {
                n += subgrid.getGridCount();
            }
        }
        return n;
    }

    /**
     * {@return a string representation of this grid for debugging purpose}.
     * If this grid has children, then they will be formatted as a tree.
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
     * Used for building a tree representation of children nodes.
     *
     * @param  branch  destination where to format the tree.
     */
    private void toTree(final TreeTable.Node branch) {
        String label = super.toString();
        if (subgrids != null) {
            label = label + " (" + getGridCount() + " grids)";
            for (final LoadedGrid<C,T> subgrid : subgrids) {
                subgrid.toTree(branch.newChild());
            }
        }
        branch.setValue(TableColumn.NAME, label);
    }

    /**
     * Returns {@code this} casted to the given type, after verification that those types are valid.
     * This method is invoked after NADCON, NTv2 or other providers got an existing {@code LoadedGrid}
     * instance from the {@link #CACHE}.
     *
     * @param  coordinateType   desired type (angle or length) of coordinate values.
     * @param  translationType  desired type (angle or length) of translation vectors.
     */
    @SuppressWarnings("unchecked")
    public final <NC extends Quantity<NC>, NT extends Quantity<NT>> LoadedGrid<NC,NT> castTo(
            final Class<NC> coordinateType, final Class<NT> translationType)
    {
        super.getCoordinateUnit() .asType(coordinateType);
        super.getTranslationUnit().asType(translationType);
        return (LoadedGrid<NC,NT>) this;
    }

    /**
     * If a grid exists in the cache for the same data, returns a new grid sharing the same data arrays.
     * Otherwise returns {@code this}.
     *
     * @return a grid using the same data as this grid, or {@code this}.
     *
     * @see #getData()
     * @see #setData(Object[])
     */
    public final LoadedGrid<C,T> useSharedData() {
        final Object[] data = getData();
        for (final LoadedGrid<?,?> grid : CACHE.values()) {
            final Object[] other = grid.getData();
            if (Arrays.deepEquals(data, other)) {
                return setData(other);
            }
        }
        return this;
    }

    /**
     * Returns a new grid with the same geometry as this grid but different data arrays.
     * This method is invoked by {@link #useSharedData()} when it detected that a newly created grid uses
     * the same data as an existing grid. The typical use case is when a filename is different but still
     * reference the same grid (e.g. symbolic link, lower case versus upper case in a case-insensitive file
     * system).
     *
     * @param  other  data from another {@code LoadedGrid} that we can share.
     * @return a new {@code LoadedGrid} using the given data reference.
     */
    protected abstract LoadedGrid<C,T> setData(Object[] other);

    /**
     * Returns the data for each shift dimensions. This method is for cache management, {@link #equals(Object)}
     * and {@link #hashCode()} implementations only and should not be invoked in other context.
     *
     * @return a direct (not cloned) reference to the internal data array.
     */
    protected abstract Object[] getData();

    /**
     * Returns {@code true} if the given object is a grid containing the same data as this grid.
     * This method compares the data provided by {@link #getData()}.
     *
     * @param  other  the other object to compare with this datum shift grid.
     * @return {@code true} if the given object is non-null, of the same class as this {@code DatumShiftGrid}
     *         and contains the same data.
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {                        // Optimization for a common case.
            return true;
        }
        if (super.equals(other)) {
            final LoadedGrid<?,?> that = (LoadedGrid<?,?>) other;
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
     * Sets all parameters for a value of type {@link URI} to the values given to the constructor.
     *
     * @param  parameters  the parameter group where to set the values.
     */
    @Override
    public final void getParameterValues(final Parameters parameters) {
        int i = 0;
        for (final GeneralParameterDescriptor gd : descriptor.descriptors()) {
            if (gd instanceof ParameterDescriptor<?>) {
                final ParameterDescriptor<?> d = (ParameterDescriptor<?>) gd;
                if (URI.class.isAssignableFrom(d.getValueClass())) {
                    if (i >= files.length) break;                       // Safety in case of invalid parameters.
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
            final MathTransformFactory factory, final LoadedGrid<Angle,Angle> grid) throws FactoryException
    {
        MathTransform global = InterpolatedTransform.createGeodeticTransformation(factory, grid);
        final LoadedGrid<Angle,Angle>[] subgrids = grid.subgrids;
        if (subgrids == null) {
            return global;
        }
        final Map<Envelope,MathTransform> specializations = JDK19.newLinkedHashMap(subgrids.length);
        for (final LoadedGrid<Angle,Angle> sg : subgrids) try {
            final Envelope domain = sg.getDomainOfValidity(Units.DEGREE);
            final MathTransform st = createGeodeticTransformation(provider, factory, sg);
            if (specializations.putIfAbsent(domain, st) != null) {
                GridLoader.log(provider, Errors.forLocale(null)
                        .createLogRecord(Level.FINE, Errors.Keys.DuplicatedElement_1, domain));
            }
        } catch (TransformException e) {
            throw new FactoryException(e);
        }
        return MathTransforms.specialize(global, specializations);
    }




    /**
     * An implementation of {@link LoadedGrid} which stores the offset values in {@code float[]} arrays.
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
     *
     * @param <C>  dimension of the coordinate unit (usually angular).
     * @param <T>  dimension of the translation unit (usually angular or linear).
     */
    public static final class Float<C extends Quantity<C>, T extends Quantity<T>> extends LoadedGrid<C,T> {
        /**
         * Serial number for inter-operability with different versions.
         */
        private static final long serialVersionUID = -9221609983475286496L;

        /**
         * The translation values. {@code offsets.length} is the number of dimensions, and {@code offsets[dim].length}
         * shall be the same for all {@code dim} value. Component {@code dim} of the translation vector at coordinate
         * {@code gridX}, {@code gridY} is {@code offsets[dim][gridX + gridY*scanlineStride]}.
         */
        public final float[][] offsets;

        /**
         * Creates a new datum shift grid with the given grid geometry, filename and number of shift dimensions.
         * All {@code double} values given to this constructor will be converted from degrees to radians.
         *
         * @param  dim  number of dimensions of translation vectors.
         */
        public Float(final int dim,
                     final Unit<C> coordinateUnit,
                     final Unit<T> translationUnit,
                     final boolean isCellValueRatio,
                     final double x0, final double y0,
                     final double Δx, final double Δy,
                     final int    nx, final int    ny,
                     final ParameterDescriptorGroup descriptor,
                     final GridFile... files) throws NoninvertibleTransformException
        {
            super(coordinateUnit, translationUnit, isCellValueRatio, x0, y0, Δx, Δy, nx, ny, descriptor, files);
            offsets = new float[dim][Math.multiplyExact(nx, ny)];
        }

        /**
         * Creates a new grid of the same geometry as the given grid but using a different data array.
         */
        private Float(final LoadedGrid<C,T> grid, final float[][] offsets) {
            super(grid);
            this.offsets = offsets;
        }

        /**
         * Returns a new grid with the same geometry as this grid but different data arrays.
         * This method is invoked by {@link #useSharedData()} when it detects that a newly created
         * grid uses the same data as an existing grid. The {@code other} object is the old grid,
         * so we can share existing data.
         */
        @Override
        protected final LoadedGrid<C,T> setData(final Object[] other) {
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
     * An implementation of {@link LoadedGrid} which stores the offset values in {@code double[]} arrays.
     * See {@link LoadedGrid.Float} for more information (most comments apply to this class as well).
     *
     * @author  Martin Desruisseaux (Geomatys)
     *
     * @param <C>  dimension of the coordinate unit (usually angular).
     * @param <T>  dimension of the translation unit (usually angular or linear).
     */
    public static final class Double<C extends Quantity<C>, T extends Quantity<T>> extends LoadedGrid<C,T> {
        /**
         * Serial number for inter-operability with different versions.
         */
        private static final long serialVersionUID = 3999271636016362364L;

        /**
         * The translation values. See {@link LoadedGrid.Float#offsets} for more documentation.
         */
        public final double[][] offsets;

        /**
         * Creates a new datum shift grid with the given grid geometry, filename and number of shift dimensions.
         * All {@code double} values given to this constructor will be converted from degrees to radians.
         */
        public Double(final int dim,
                      final Unit<C> coordinateUnit,
                      final Unit<T> translationUnit,
                      final boolean isCellValueRatio,
                      final double x0, final double y0,
                      final double Δx, final double Δy,
                      final int    nx, final int    ny,
                      final ParameterDescriptorGroup descriptor,
                      final GridFile... files) throws NoninvertibleTransformException
        {
            super(coordinateUnit, translationUnit, isCellValueRatio, x0, y0, Δx, Δy, nx, ny, descriptor, files);
            offsets = new double[dim][Math.multiplyExact(nx, ny)];
        }

        /**
         * Creates a new grid of the same geometry as the given grid but using a different data array.
         */
        private Double(final LoadedGrid<C,T> grid, final double[][] offsets) {
            super(grid);
            this.offsets = offsets;
        }

        /**
         * Returns a new grid with the same geometry as this grid but different data arrays.
         * See {@link LoadedGrid.Float#setData(Object[])} for more documentation.
         */
        @Override
        protected final LoadedGrid<C,T> setData(final Object[] other) {
            return new Double<>(this, (double[][]) other);
        }

        /**
         * Returns direct references (not cloned) to the data arrays.
         * See {@link LoadedGrid.Float#getData()} for more documentation.
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
         * See {@link LoadedGrid.Float#getCellValue(int, int, int)} for more documentation.
         */
        @Override
        public final double getCellValue(final int dim, final int gridX, final int gridY) {
            return offsets[dim][gridX + gridY*scanlineStride];
        }
    }
}
