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

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Optional;
import java.util.Locale;
import java.io.Serializable;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.internal.raster.Resources;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.TransformSeparator;
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
     * The dimension name types for given coordinate system axis directions.
     * This map contains only the "positive" axis directions.
     *
     * @todo Verify if there is more directions to add as of ISO 19111:2018.
     */
    private static final Map<AxisDirection,DimensionNameType> AXIS_DIRECTIONS;
    static {
        final Map<AxisDirection,DimensionNameType> dir = new HashMap<>(6);
        dir.put(AxisDirection.COLUMN_POSITIVE, DimensionNameType.COLUMN);
        dir.put(AxisDirection.ROW_POSITIVE,    DimensionNameType.ROW);
        dir.put(AxisDirection.UP,              DimensionNameType.VERTICAL);
        dir.put(AxisDirection.FUTURE,          DimensionNameType.TIME);
        AXIS_DIRECTIONS = dir;
    }

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
     * Minimum and maximum grid coordinates. The first half contains minimum coordinates (inclusive),
     * while the last half contains maximum coordinates (<strong>inclusive</strong>). Note that the
     * later is the opposite of Java2D usage but conform to ISO specification.
     */
    private final long[] coordinates;

    /**
     * Creates a new array of coordinates with the given number of dimensions.
     *
     * @throws IllegalArgumentException if the given number of dimensions is excessive.
     */
    private static long[] allocate(final int dimension) throws IllegalArgumentException {
        if (dimension >= Numerics.MAXIMUM_MATRIX_SIZE) {
            // Actually the real limit is Integer.MAX_VALUE / 2, but a value too high is likely to be an error.
            throw new IllegalArgumentException(Errors.format(Errors.Keys.ExcessiveNumberOfDimensions_1, dimension));
        }
        return new long[dimension << 1];
    }

    /**
     * Checks if coordinate values in the low part are less than or
     * equal to the corresponding coordinate value in the high part.
     *
     * @throws IllegalArgumentException if a coordinate value in the low part is
     *         greater than the corresponding coordinate value in the high part.
     */
    private static void checkCoherence(final long[] coordinates) throws IllegalArgumentException {
        final int dimension = coordinates.length >>> 1;
        for (int i=0; i<dimension; i++) {
            final long lower = coordinates[i];
            final long upper = coordinates[i + dimension];
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
        coordinates = allocate(dimension);
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
        coordinates = new long[4];
        coordinates[2] = width  - 1;
        coordinates[3] = height - 1;
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
     * @param  axisTypes       the type of each grid axis, or {@code null} if unspecified.
     * @param  low             the valid minimum grid coordinates (always inclusive), or {@code null} for all zeros.
     * @param  high            the valid maximum grid coordinates, inclusive or exclusive depending on the next argument.
     * @param  isHighIncluded  {@code true} if the {@code high} values are inclusive (as in ISO 19123 specification),
     *                         or {@code false} if they are exclusive (as in Java2D usage).
     *                         This argument does not apply to {@code low} values, which are always inclusive.
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
        coordinates = allocate(dimension);
        if (low != null) {
            System.arraycopy(low, 0, coordinates, 0, dimension);
        }
        System.arraycopy(high, 0, coordinates, dimension, dimension);
        if (!isHighIncluded) {
            for (int i=dimension; i < coordinates.length; i++) {
                coordinates[i] = Math.decrementExact(coordinates[i]);
            }
        }
        checkCoherence(coordinates);
        types = validateAxisTypes(axisTypes);
    }

    /**
     * Creates a new grid extent by rounding the given envelope to (usually) nearest integers.
     * The envelope coordinates shall be cell indices with lower values inclusive and upper values exclusive.
     * Envelopes crossing the anti-meridian shall be {@linkplain GeneralEnvelope#simplify() simplified}.
     * The envelope CRS is ignored, except for identifying dimension names for information purpose.
     * The way floating point values are rounded to integers may be adjusted in any future version.
     *
     * <p><b>NOTE:</b> this constructor is not public because its contract is a bit approximative.</p>
     *
     * @param  envelope  the envelope containing cell indices to store in a {@code GridExtent}.
     *
     * @see #toCRS(MathTransform, MathTransform)
     */
    GridExtent(final AbstractEnvelope envelope) {
        final int dimension = envelope.getDimension();
        coordinates = allocate(dimension);
        for (int i=0; i<dimension; i++) {
            final double min = envelope.getLower(i);
            final double max = envelope.getUpper(i);
            if (min >= Long.MIN_VALUE && max <= Long.MAX_VALUE && min <= max) {
                long lower = Math.round(min);
                long upper = Math.round(max);
                if (lower != upper) upper--;                                // For making the coordinate inclusive.
                /*
                 * The [lower … upper] range may be slightly larger than desired in some rounding error situations.
                 * For example if 'min' was 1.49999 and 'max' was 2.50001, the roundings will create a [1…3] range
                 * while there is actually only 2 pixels. We detect those rounding problems by comparing the spans
                 * before and after rounding. We attempt an adjustment only if the span mistmatch is ±1, otherwise
                 * the difference is assumed to be caused by overflow. On the three values that can be affected by
                 * the adjustment (min, max and span), we change only the number which is farthest from an integer
                 * value.
                 */
                long error = (upper - lower) + 1;                           // Negative number if overflow.
                if (error >= 0) {
                    final double span = envelope.getSpan(i);
                    final long extent = Math.round(span);
                    if (extent != 0 && Math.abs(error -= extent) == 1) {
                        final double dmin = Math.abs(min - Math.rint(min));
                        final double dmax = Math.abs(max - Math.rint(max));
                        final boolean adjustMax = (dmax >= dmin);
                        if (Math.abs(span - extent) < (adjustMax ? dmax : dmin)) {
                            if (adjustMax) upper = Math.subtractExact(upper, error);
                            else lower = Math.addExact(lower, error);
                        }
                    }
                }
                coordinates[i] = lower;
                coordinates[i + dimension] = upper;
            } else {
                throw new IllegalArgumentException(Resources.format(
                        Resources.Keys.IllegalGridEnvelope_3, i, min, max));
            }
        }
        /*
         * At this point we finished to compute coordinate values.
         * Now try to infer dimension types from the CRS axes.
         * This is only for information purpose.
         */
        DimensionNameType[] axisTypes = null;
        final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        if (crs != null) {
            final CoordinateSystem cs = crs.getCoordinateSystem();
            for (int i=0; i<dimension; i++) {
                final DimensionNameType type = AXIS_DIRECTIONS.get(AxisDirections.absolute(cs.getAxis(i).getDirection()));
                if (type != null) {
                    if (axisTypes == null) {
                        axisTypes = new DimensionNameType[dimension];
                    }
                    axisTypes[i] = type;
                }
            }
        }
        types = validateAxisTypes(axisTypes);
    }

    /**
     * Creates a new grid envelope as a copy of the given one.
     *
     * @param  extent  the grid envelope to copy.
     * @throws IllegalArgumentException if a coordinate value in the low part is
     *         greater than the corresponding coordinate value in the high part.
     *
     * @see #castOrCopy(GridEnvelope)
     */
    protected GridExtent(final GridEnvelope extent) {
        ArgumentChecks.ensureNonNull("extent", extent);
        final int dimension = extent.getDimension();
        coordinates = allocate(dimension);
        for (int i=0; i<dimension; i++) {
            coordinates[i] = extent.getLow(i);
            coordinates[i + dimension] = extent.getHigh(i);
        }
        checkCoherence(coordinates);
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
        return coordinates.length >>> 1;
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
        return new GridCoordinatesView(coordinates, 0);
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
        return new GridCoordinatesView(coordinates, getDimension());
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
        return coordinates[index];
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
        return coordinates[index + dimension];
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
        return Math.incrementExact(Math.subtractExact(coordinates[dimension + index], coordinates[index]));
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
            center.setOrdinate(i, ((double) coordinates[i] + (double) coordinates[i + dimension] + 1.0) * 0.5);
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
     * @param  cornerToCRS  a transform from <em>pixel corners</em> to real world coordinates.
     * @param  gridToCRS    the transform specified by the user. May be the same as {@code cornerToCRS}.
     *                      If different, then this is assumed to map pixel centers instead than pixel corners.
     * @return this grid extent in real world coordinates.
     *
     * @see #GridExtent(AbstractEnvelope)
     */
    final GeneralEnvelope toCRS(final MathTransform cornerToCRS, final MathTransform gridToCRS) throws TransformException {
        final int dimension = getDimension();
        GeneralEnvelope envelope = new GeneralEnvelope(dimension);
        for (int i=0; i<dimension; i++) {
            envelope.setRange(i, coordinates[i], coordinates[i + dimension] + 1.0);
        }
        envelope = Envelopes.transform(cornerToCRS, envelope);
        if (envelope.isEmpty()) try {
            /*
             * If the envelope contains some NaN values, try to replace them by constant values inferred from the math transform.
             * We must use the MathTransform specified by the user (gridToCRS), not necessarily the cornerToCRS transform, because
             * because inferring a 'cornerToCRS' by translation a 'centertoCRS' by 0.5 pixels increase the amount of NaN values in
             * the matrix. For giving a chance to TransformSeparator to perform its work, we need the minimal amount of NaN values.
             */
            final boolean isCenter = (gridToCRS != cornerToCRS);
            TransformSeparator separator = null;
            for (int srcDim=0; srcDim < dimension; srcDim++) {
                if (coordinates[srcDim + dimension] == 0 && coordinates[srcDim] == 0) {
                    /*
                     * At this point we found a grid dimension with [0 … 0] range. Only this specific range is processed because
                     * it is assumed associated to NaN scale factors in the 'gridToCRS' matrix, since the resolution is computed
                     * by 0/0.  We require the range to be [0 … 0] instead of [n … n] because if grid indices are not zero, then
                     * we would need to know the scale factors for computing the offset.
                     */
                    if (separator == null) {
                        separator = new TransformSeparator(gridToCRS);
                    }
                    separator.addSourceDimensionRange(srcDim, srcDim + 1);
                    final Matrix component = MathTransforms.getMatrix(separator.separate());
                    if (component != null) {
                        final int[] targets = separator.getTargetDimensions();
                        for (int j=0; j<targets.length; j++) {
                            final int tgtDim = targets[j];
                            double lower = envelope.getLower(tgtDim);
                            double upper = envelope.getUpper(tgtDim);
                            final double value = component.getElement(j, component.getNumCol() - 1);
                            /*
                             * Replace only the envelope NaN values by the translation term (non-NaN values are left unchanged).
                             * If the gridToCRS map pixel corners, then we update only the lower bound since the transform maps
                             * lower-left corner; the upper bound is unknown. If the gridToCRS maps pixel center, then we update
                             * both lower and upper bounds to a value assumed to be in the center; the span is set to zero.
                             */
                            if (isCenter) {
                                double span = upper - value;
                                if (Double.isNaN(span)) {
                                    span = value - lower;
                                    if (Double.isNaN(span)) {
                                        span = 0;
                                    }
                                }
                                if (Double.isNaN(lower)) lower = value - span;
                                if (Double.isNaN(upper)) upper = value + span;
                            } else if (Double.isNaN(lower)) {
                                lower = value;
                            }
                            envelope.setRange(tgtDim, lower, upper);
                        }
                    }
                    separator.clear();
                }
            }
        } catch (FactoryException e) {
            GridGeometry.recoverableException(e);
        }
        return envelope;
    }

    /**
     * Returns a new grid envelope with the specified dimension added after this grid envelope dimensions.
     *
     * @param  axisType        the type of the grid axis to add, or {@code null} if unspecified.
     * @param  low             the valid minimum grid coordinate (always inclusive).
     * @param  high            the valid maximum grid coordinate, inclusive or exclusive depending on the next argument.
     * @param  isHighIncluded  {@code true} if the {@code high} value is inclusive (as in ISO 19123 specification),
     *                         or {@code false} if it is exclusive (as in Java2D usage).
     *                         This argument does not apply to {@code low} value, which is always inclusive.
     * @return a new grid envelope with the specified dimension added.
     * @throws IllegalArgumentException if the low coordinate value is greater than the high coordinate value.
     */
    public GridExtent append(final DimensionNameType axisType, final long low, long high, final boolean isHighIncluded) {
        if (!isHighIncluded) {
            high = Math.decrementExact(high);
        }
        final int dimension = getDimension();
        final int newDim    = dimension + 1;
        DimensionNameType[] axisTypes = null;
        if (types != null || axisType != null) {
            if (types != null) {
                axisTypes = Arrays.copyOf(types, newDim);
            } else {
                axisTypes = new DimensionNameType[newDim];
            }
            axisTypes[dimension] = axisType;
        }
        final GridExtent ex = new GridExtent(newDim, axisTypes);
        System.arraycopy(coordinates, 0,         ex.coordinates, 0,      dimension);
        System.arraycopy(coordinates, dimension, ex.coordinates, newDim, dimension);
        ex.coordinates[dimension]          = low;
        ex.coordinates[dimension + newDim] = high;
        checkCoherence(ex.coordinates);
        return ex;
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
        System.arraycopy(coordinates, lower,           sub.coordinates, 0,      newDim);
        System.arraycopy(coordinates, lower+dimension, sub.coordinates, newDim, newDim);
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
        return Arrays.hashCode(coordinates) + Arrays.hashCode(types) ^ (int) serialVersionUID;
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
            return Arrays.equals(coordinates, other.coordinates) && Arrays.equals(types, other.types);
        }
        return false;
    }

    /**
     * Returns a string representation of this grid envelope. The returned string
     * is implementation dependent and is provided for debugging purposes only.
     */
    @Override
    public String toString() {
        final StringBuilder out = new StringBuilder(256);
        appendTo(out, Vocabulary.getResources((Locale) null), false);
        return out.toString();
    }

    /**
     * Writes a string representation of this grid envelope in the given buffer.
     *
     * @param out         where to write the string representation.
     * @param vocabulary  resources for some words, or {@code null} if not yet fetched.
     * @param tree        whether to format lines of a tree in the margin on the left.
     */
    final void appendTo(final StringBuilder out, final Vocabulary vocabulary, final boolean tree) {
        final TableAppender table = new TableAppender(out, "");
        final int dimension = getDimension();
        for (int i=0; i<dimension; i++) {
            CharSequence name;
            if ((types == null) || (name = Types.getCodeTitle(types[i])) == null) {
                name = vocabulary.getString(Vocabulary.Keys.Dimension_1, i);
            }
            final long lower = coordinates[i];
            final long upper = coordinates[i + dimension];
            table.setCellAlignment(TableAppender.ALIGN_LEFT);
            if (tree) {
                branch(table, i < dimension - 1);
            }
            table.append(name).append(": ").nextColumn();
            table.append('[').nextColumn();
            table.setCellAlignment(TableAppender.ALIGN_RIGHT);
            table.append(Long.toString(lower)).append(" … ").nextColumn();
            table.append(Long.toString(upper)).append("] ") .nextColumn();
            table.append('(').append(vocabulary.getString(Vocabulary.Keys.CellCount_1,
                    Long.toUnsignedString(upper - lower + 1))).append(')').nextLine();
        }
        flush(table);
    }

    /**
     * Formats the symbols on the left side of a node in a tree.
     */
    static void branch(final TableAppender table, final boolean hasMore) {
        table.append(hasMore ? '├' : '└').append("─ ");
    }

    /**
     * Writes the content of given table without throwing {@link IOException}.
     * Shall be invoked only when the destination is known to be {@link StringBuilder}.
     */
    static void flush(final TableAppender table) {
        try {
            table.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
