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
package org.apache.sis.coverage.grid;

import java.util.Arrays;
import java.util.Optional;
import java.io.Serializable;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.internal.raster.Resources;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.io.TableAppender;
import org.apache.sis.util.iso.Types;

// Branch-dependent imports
import org.opengis.coverage.grid.GridEnvelope;


/**
 * A range of grid coverage coordinates, also known as "grid envelope".
 * {@code GridExtent} are defined by {@linkplain #getLow() low} coordinates (often all zeros)
 * and {@linkplain #getHigh() high} coordinates, <strong>inclusive</strong>.
 * For example a grid with a width of 512 cells can have a low coordinate of 0 and high coordinate of 511.
 *
 * <div class="note"><b>Note:</b>
 * The inclusiveness of {@linkplain #getHigh() high} coordinates come from ISO 19123.
 * We follow this specification for all getters methods, but developers should keep in mind
 * that this is the opposite of Java2D usage where {@link java.awt.Rectangle} maximal values are exclusive.</div>
 *
 * {@code GridExtent} instances are unmodifiable, so they can be shared between different {@link GridGeometry} instances.
 *
 * <div class="note"><b>Upcoming API generalization:</b>
 * this class may implement the {@code GridEnvelope} interface in a future Apache SIS version.
 * This is pending <a href="https://github.com/opengeospatial/geoapi/issues/36">GeoAPI update</a>.</div>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class GridExtent implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4717353677844056017L;

    /**
     * Default axis types for the two-dimensional cases.
     */
    private static final DimensionNameType[] DEFAULT_TYPES = new DimensionNameType[] {
        DimensionNameType.COLUMN,
        DimensionNameType.ROW
    };

    /**
     * A pool of shared {@link DimensionNameType} arrays. We use a pool
     * because a small amount of arrays is shared by most grid extent.
     */
    private static final WeakValueHashMap<DimensionNameType[],DimensionNameType[]> POOL = new WeakValueHashMap<>(DimensionNameType[].class);

    /**
     * Type of each axis (vertical, temporal, …) or {@code null} if unspecified.
     * If non-null, the array length shall be equal to {@link #getDimension()}.
     * Any array element may be null if unspecified for that particular axis.
     * The same array may be shared by many {@code GridExtent} instances.
     *
     * @see #getAxisType(int)
     */
    private final DimensionNameType[] types;

    /**
     * Minimum and maximum grid ordinates. The first half contains minimum ordinates (inclusive),
     * while the last half contains maximum ordinates (<strong>inclusive</strong>). Note that the
     * later is the opposite of Java2D usage but conform to ISO specification.
     */
    private final long[] ordinates;

    /**
     * Creates a new array of coordinates with the given number of dimensions.
     *
     * @throws IllegalArgumentException if the given number of dimensions is excessive.
     */
    private static long[] allocate(final int dimension) throws IllegalArgumentException {
        if (dimension > Integer.MAX_VALUE / 2) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.ExcessiveNumberOfDimensions_1, dimension));
        }
        return new long[dimension << 1];
    }

    /**
     * Checks if ordinate values in the low part are less than or
     * equal to the corresponding ordinate value in the high part.
     *
     * @throws IllegalArgumentException if a coordinate value in the low part is
     *         greater than the corresponding coordinate value in the high part.
     */
    private static void checkCoherence(final long[] ordinates) throws IllegalArgumentException {
        final int dimension = ordinates.length >>> 1;
        for (int i=0; i<dimension; i++) {
            final long lower = ordinates[i];
            final long upper = ordinates[i + dimension];
            if (lower > upper) {
                throw new IllegalArgumentException(Resources.format(
                        Resources.Keys.IllegalGridEnvelope_3, i, lower, upper));
            }
        }
    }

    /**
     * Verifies that the given array (if non-null) contains no duplicated values, then returns a copy of that array.
     * The returned copy may be shared by many {@code GridExtent} instances. Consequently it shall not be modified.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    private static DimensionNameType[] validateAxisTypes(DimensionNameType[] types) {
        if (types == null) {
            return null;
        }
        if (Arrays.equals(DEFAULT_TYPES, types)) {          // Common case verified before POOL synchronized lock.
            return DEFAULT_TYPES;
        }
        DimensionNameType[] shared = POOL.get(types);
        if (shared == null) {
            /*
             * Verify the array only if it was not found in the pool. Arrays in the pool were already validated,
             * so do not need to be verified again. The check performed here is inefficient (nested loop), but it
             * should be okay since the arrays are usually small (less than 5 elements) and the checks should not
             * be done often (because of the pool).
             */
            types = types.clone();
            for (int i=1; i<types.length; i++) {
                final DimensionNameType t = types[i];
                if (t != null) {
                    for (int j=i; --j >= 0;) {
                        if (t.equals(types[j])) {
                            throw new IllegalArgumentException(Errors.format(Errors.Keys.DuplicatedElement_1, t));
                        }
                    }
                }
            }
            shared = POOL.putIfAbsent(types, types);
            if (shared == null) {
                return types;
            }
        }
        return shared;
    }

    /**
     * Creates an initially empty grid envelope with the given number of dimensions.
     * All grid coordinate values are initialized to zero. This constructor is private
     * since {@code GridExtent} coordinate values can not be modified by public API.
     *
     * @param dimension  number of dimensions.
     * @param axisTypes  the axis types, or {@code null} if unspecified.
     */
    private GridExtent(final int dimension, final DimensionNameType[] axisTypes) {
        ordinates = allocate(dimension);
        types = validateAxisTypes(axisTypes);
    }

    /**
     * Creates a new grid extent for an image or matrix of the given size.
     * The {@linkplain #getLow() low} grid coordinates are zeros and the axis types are
     * {@link DimensionNameType#COLUMN} and {@link DimensionNameType#ROW ROW} in that order.
     *
     * @param  width   number of pixels in each row.
     * @param  height  number of pixels in each column.
     */
    public GridExtent(final long width, final long height) {
        ArgumentChecks.ensureStrictlyPositive("width",  width);
        ArgumentChecks.ensureStrictlyPositive("height", height);
        ordinates = new long[4];
        ordinates[2] = width  - 1;
        ordinates[3] = height - 1;
        types = DEFAULT_TYPES;
    }

    /**
     * Constructs a new grid envelope set to the specified coordinates.
     * The given arrays contain a minimum (inclusive) and maximum value for each dimension of the grid coverage.
     * The lowest valid grid coordinates are often zero, but this is not mandatory.
     * As a convenience for this common case, a null {@code low} array means that all low coordinates are zero.
     *
     * <p>An optional (nullable) {@code axisTypes} argument can be used for attaching a label to each grid axis.
     * For example if this {@code GridExtent} is four-dimensional, then the axis types may be
     * {{@linkplain DimensionNameType#COLUMN   column}  (<var>x</var>),
     *  {@linkplain DimensionNameType#ROW      row}     (<var>y</var>),
     *  {@linkplain DimensionNameType#VERTICAL vertical (<var>z</var>),
     *  {@linkplain DimensionNameType#TIME     time}    (<var>t</var>)},
     * which means that the last axis is for the temporal dimension, the third axis is for the vertical dimension, <i>etc.</i>
     * This information is related to the "real world" coordinate reference system axes, but not necessarily in the same order;
     * it is caller responsibility to ensure that the grid axes are consistent with the CRS axes.
     * The {@code axisTypes} array shall not contain duplicated elements,
     * but may contain {@code null} elements if the type of some axes are unknown.</p>
     *
     * @param  axisTypes  the type of each grid axis, or {@code null} if unspecified.
     * @param  low    the valid minimum grid coordinate (always inclusive), or {@code null} for all zeros.
     * @param  high   the valid maximum grid coordinate, inclusive or exclusive depending on the next argument.
     * @param  isHighIncluded  {@code true} if the {@code high} values are inclusive (as in ISO 19123 specification),
     *         or {@code false} if they are exclusive (as in Java2D usage).
     *         This argument does not apply to {@code low} values, which are always inclusive.
     * @throws IllegalArgumentException if a coordinate value in the low part is
     *         greater than the corresponding coordinate value in the high part.
     *
     * @see #getLow()
     * @see #getHigh()
     */
    public GridExtent(final DimensionNameType[] axisTypes, final long[] low, final long[] high, final boolean isHighIncluded) {
        ArgumentChecks.ensureNonNull("high", high);
        final int dimension = high.length;
        if (low != null && low.length != dimension) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedDimension_2, low.length, dimension));
        }
        if (axisTypes != null && axisTypes.length != dimension) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedArrayLengths));
        }
        ordinates = allocate(dimension);
        if (low != null) {
            System.arraycopy(low, 0, ordinates, 0, dimension);
        }
        System.arraycopy(high, 0, ordinates, dimension, dimension);
        if (!isHighIncluded) {
            for (int i=dimension; i < ordinates.length; i++) {
                ordinates[i] = Math.decrementExact(ordinates[i]);
            }
        }
        checkCoherence(ordinates);
        types = validateAxisTypes(axisTypes);
    }

    /**
     * Creates a new grid envelope as a copy of the given one.
     *
     * @param  extent  the grid envelope to copy.
     * @throws IllegalArgumentException if a coordinate value in the low part is
     *         greater than the corresponding coordinate value in the high part.
     */
    protected GridExtent(final GridEnvelope extent) {
        ArgumentChecks.ensureNonNull("extent", extent);
        final int dimension = extent.getDimension();
        ordinates = allocate(dimension);
        for (int i=0; i<dimension; i++) {
            ordinates[i] = extent.getLow(i);
            ordinates[i + dimension] = extent.getHigh(i);
        }
        checkCoherence(ordinates);
        types = (extent instanceof GridExtent) ? ((GridExtent) extent).types : null;
    }

    /**
     * Returns the given grid envelope as a {@code GridExtent} implementation.
     * If the given extent is already a {@code GridExtent} instance or is null, then it is returned as-is.
     * Otherwise a new extent is created using the {@linkplain #GridExtent(GridEnvelope) copy constructor}.
     *
     * @param  extent  the grid envelope to cast or copy, or {@code null}.
     * @return the grid envelope as a {@code GridExtent}, or {@code null} if the given extent was null.
     */
    public static GridExtent castOrCopy(final GridEnvelope extent) {
        if (extent == null || extent instanceof GridExtent) {
            return (GridExtent) extent;
        } else {
            return new GridExtent(extent);
        }
    }

    /**
     * Returns the number of dimensions.
     *
     * @return the number of dimensions.
     */
    public final int getDimension() {
        return ordinates.length >>> 1;
    }

    /**
     * Returns the valid minimum grid coordinates, inclusive.
     * The sequence contains a minimum value for each dimension of the grid coverage.
     *
     * @return the valid minimum grid coordinates, inclusive.
     *
     * @todo Pending resolution of <a href="https://github.com/opengeospatial/geoapi/issues/36">GeoAPI update</a>
     *       before to become public API, in order to use the interface in return type.
     */
    GridCoordinatesView getLow() {
        return new GridCoordinatesView(ordinates, 0);
    }

    /**
     * Returns the valid maximum grid coordinates, <strong>inclusive</strong>.
     * The sequence contains a maximum value for each dimension of the grid coverage.
     *
     * @return the valid maximum grid coordinates, <strong>inclusive</strong>.
     *
     * @todo Pending resolution of <a href="https://github.com/opengeospatial/geoapi/issues/36">GeoAPI update</a>
     *       before to become public API, in order to use the interface in return type.
     */
    GridCoordinatesView getHigh() {
        return new GridCoordinatesView(ordinates, getDimension());
    }

    /**
     * Returns the valid minimum inclusive grid coordinate along the specified dimension.
     *
     * @param  index  the dimension for which to obtain the coordinate value.
     * @return the low coordinate value at the given dimension, inclusive.
     * @throws IndexOutOfBoundsException if the given index is negative or is equals or greater
     *         than the {@linkplain #getDimension() grid dimension}.
     *
     * @see #getLow()
     * @see #getHigh(int)
     */
    public long getLow(final int index) {
        ArgumentChecks.ensureValidIndex(getDimension(), index);
        return ordinates[index];
    }

    /**
     * Returns the valid maximum <strong>inclusive</strong> grid coordinate along the specified dimension.
     *
     * @param  index  the dimension for which to obtain the coordinate value.
     * @return the high coordinate value at the given dimension, <strong>inclusive</strong>.
     * @throws IndexOutOfBoundsException if the given index is negative or is equals or greater
     *         than the {@linkplain #getDimension() grid dimension}.
     *
     * @see #getHigh()
     * @see #getLow(int)
     */
    public long getHigh(final int index) {
        final int dimension = getDimension();
        ArgumentChecks.ensureValidIndex(dimension, index);
        return ordinates[index + dimension];
    }

    /**
     * Returns the number of integer grid coordinates along the specified dimension.
     * This is equal to {@code getHigh(dimension) - getLow(dimension) + 1}.
     *
     * @param  index  the dimension for which to obtain the size.
     * @return the number of integer grid coordinates along the given dimension.
     * @throws IndexOutOfBoundsException if the given index is negative or is equals or greater
     *         than the {@linkplain #getDimension() grid dimension}.
     * @throws ArithmeticException if the size is too large for the {@code long} primitive type.
     *
     * @see #getLow(int)
     * @see #getHigh(int)
     */
    public long getSize(final int index) {
        final int dimension = getDimension();
        ArgumentChecks.ensureValidIndex(dimension, index);
        return Math.incrementExact(Math.subtractExact(ordinates[dimension + index], ordinates[index]));
    }

    /**
     * Returns the grid coordinates of a representative point.
     * This point may be used for estimating a {@linkplain GridGeometry#getResolution(boolean) grid resolution}.
     * The default implementation returns the median (or center) coordinates of this grid extent,
     * but subclasses can override this method if another point is considered more representative.
     *
     * @return the grid coordinates of a representative point.
     */
    public DirectPosition getCentroid() {
        final int dimension = getDimension();
        final GeneralDirectPosition center = new GeneralDirectPosition(dimension);
        for (int i=0; i<dimension; i++) {
            center.setOrdinate(i, ((double) ordinates[i] + (double) ordinates[i + dimension] + 1.0) * 0.5);
        }
        return center;
    }

    /**
     * Returns the type (vertical, temporal, …) of grid axis at given dimension.
     * This information is provided because the grid axis type can not always be inferred from the context.
     * Some examples are:
     *
     * <ul>
     *   <li>{@code getAxisType(0)} may return {@link DimensionNameType#COLUMN},
     *       {@link DimensionNameType#TRACK TRACK} or {@link DimensionNameType#LINE LINE}.</li>
     *   <li>{@code getAxisType(1)} may return {@link DimensionNameType#ROW},
     *       {@link DimensionNameType#CROSS_TRACK CROSS_TRACK} or {@link DimensionNameType#SAMPLE SAMPLE}.</li>
     *   <li>{@code getAxisType(2)} may return {@link DimensionNameType#VERTICAL}.</li>
     *   <li>{@code getAxisType(3)} may return {@link DimensionNameType#TIME}.</li>
     * </ul>
     *
     * Above are only examples; there is no constraint on axis order. In particular grid axes do not need to be in the same
     * order than the corresponding {@linkplain GridGeometry#getCoordinateReferenceSystem() coordinate reference system} axes.
     *
     * @param  index  the dimension for which to obtain the axis type.
     * @return the axis type at the given dimension. May be absent if the type is unknown.
     * @throws IndexOutOfBoundsException if the given index is negative or is equals or greater
     *         than the {@linkplain #getDimension() grid dimension}.
     */
    public Optional<DimensionNameType> getAxisType(final int index) {
        ArgumentChecks.ensureValidIndex(getDimension(), index);
        return Optional.ofNullable((types != null) ? types[index] : null);
    }

    /**
     * Transforms this grid extent to a "real world" envelope using the given transform.
     * The transform shall map <em>pixel corner</em> to real world coordinates.
     *
     * @param  gridToCRS  a transform from <em>pixel corner</em> to real world coordinates
     * @return this grid extent in real world coordinates.
     */
    final GeneralEnvelope toCRS(final MathTransform gridToCRS) throws TransformException {
        final int dimension = getDimension();
        final GeneralEnvelope envope = new GeneralEnvelope(dimension);
        for (int i=0; i<dimension; i++) {
            envope.setRange(i, ordinates[i], ordinates[i + dimension] + 1.0);
        }
        return Envelopes.transform(gridToCRS, envope);
    }

    /**
     * Returns a new grid envelope that encompass only some dimensions of this grid envelope.
     * This method copies this grid envelope into a new grid envelope, beginning at dimension
     * {@code lower} and extending to dimension {@code upper-1} inclusive. Thus the dimension
     * of the sub grid envelope is {@code upper - lower}.
     *
     * @param  lower  the first dimension to copy, inclusive.
     * @param  upper  the last  dimension to copy, exclusive.
     * @return the sub-envelope, or {@code this} if [{@code lower} … {@code upper}] is [0 … {@link #getDimension() dimension}].
     * @throws IndexOutOfBoundsException if an index is out of bounds.
     */
    public GridExtent subExtent(final int lower, final int upper) {
        final int dimension = getDimension();
        ArgumentChecks.ensureValidIndexRange(dimension, lower, upper);
        final int newDim = upper - lower;
        if (newDim == dimension) {
            return this;
        }
        DimensionNameType[] axisTypes = types;
        if (axisTypes != null) {
            axisTypes = Arrays.copyOfRange(axisTypes, lower, upper);
        }
        final GridExtent sub = new GridExtent(newDim, axisTypes);
        System.arraycopy(ordinates, lower,           sub.ordinates, 0,      newDim);
        System.arraycopy(ordinates, lower+dimension, sub.ordinates, newDim, newDim);
        return sub;
    }

    /**
     * Returns a hash value for this grid envelope. This value need not remain
     * consistent between different implementations of the same class.
     *
     * @return a hash value for this grid envelope.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(ordinates) + Arrays.hashCode(types) ^ (int) serialVersionUID;
    }

    /**
     * Compares the specified object with this grid envelope for equality.
     *
     * @param  object  the object to compare with this grid envelope for equality.
     * @return {@code true} if the given object is equal to this grid envelope.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == GridExtent.class) {
            final GridExtent other = (GridExtent) object;
            return Arrays.equals(ordinates, other.ordinates) && Arrays.equals(types, other.types);
        }
        return false;
    }

    /**
     * Returns a string representation of this grid envelope. The returned string
     * is implementation dependent and is provided for debugging purposes only.
     */
    @Override
    public String toString() {
        final TableAppender table = new TableAppender(" ");
        final int dimension = getDimension();
        for (int i=0; i<dimension; i++) {
            String name;
            if ((types == null) || (name = Types.getCodeLabel(types[i])) == null) {
                name = Integer.toString(i);
            }
            table.append(name).append(':').nextColumn();
            table.setCellAlignment(TableAppender.ALIGN_RIGHT);
            table.append(Long.toString(ordinates[i])).nextColumn();
            table.append("to").nextColumn();
            table.append(Long.toString(ordinates[i + dimension])).nextLine();
            table.setCellAlignment(TableAppender.ALIGN_LEFT);
        }
        return table.toString();
    }
}
