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
import java.io.Serializable;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.raster.Resources;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralDirectPosition;

// Branch-dependent imports
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.grid.GridCoordinates;


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
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
public class GridExtent implements GridEnvelope, Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4717353677844056017L;

    /**
     * Minimum and maximum grid ordinates. The first half contains minimum ordinates (inclusive),
     * while the last half contains maximum ordinates (<strong>inclusive</strong>). Note that the
     * later is the opposite of Java2D usage but conform to ISO specification.
     */
    private final int[] ordinates;

    /**
     * Creates a new array of coordinates with the given number of dimensions.
     *
     * @throws IllegalArgumentException if the given number of dimensions is excessive.
     */
    private static int[] allocate(final int dimension) throws IllegalArgumentException {
        if (dimension > Integer.MAX_VALUE / 2) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.ExcessiveNumberOfDimensions_1, dimension));
        }
        return new int[dimension << 1];
    }

    /**
     * Checks if ordinate values in the low part are less than or
     * equal to the corresponding ordinate value in the high part.
     *
     * @throws IllegalArgumentException if a coordinate value in the low part is
     *         greater than the corresponding coordinate value in the high part.
     */
    private static void checkCoherence(final int[] ordinates) throws IllegalArgumentException {
        final int dimension = ordinates.length >>> 1;
        for (int i=0; i<dimension; i++) {
            final int lower = ordinates[i];
            final int upper = ordinates[i + dimension];
            if (lower > upper) {
                throw new IllegalArgumentException(Resources.format(
                        Resources.Keys.IllegalGridEnvelope_3, i, lower, upper));
            }
        }
    }

    /**
     * Creates an initially empty grid envelope with the given number of dimensions.
     * All grid coordinate values are initialized to zero. This constructor is private
     * since {@code GridExtent} coordinate values can not be modified by public API.
     *
     * @param dimension  number of dimensions.
     */
    private GridExtent(final int dimension) {
        ordinates = allocate(dimension);
    }

    /**
     * Constructs a new grid envelope set to the specified coordinates.
     * The given arrays contain a minimum (inclusive) and maximum value for each dimension of the grid coverage.
     * The lowest valid grid coordinates are often zero, but this is not mandatory.
     * As a convenience for this common case, a null {@code low} array means that all low coordinates are zero.
     *
     * @param  low   the valid minimum grid coordinate (always inclusive), or {@code null} for all zeros.
     * @param  high  the valid maximum grid coordinate, inclusive or exclusive depending on the next argument.
     * @param  isHighIncluded  {@code true} if the {@code high} values are inclusive (as in ISO 19123 specification),
     *         or {@code false} if they are exclusive (as in Java2D usage).
     *         This argument does not apply to {@code low} values, which are always inclusive.
     * @throws IllegalArgumentException if a coordinate value in the low part is
     *         greater than the corresponding coordinate value in the high part.
     *
     * @see #getLow()
     * @see #getHigh()
     */
    public GridExtent(final int[] low, final int[] high, final boolean isHighIncluded) {
        ArgumentChecks.ensureNonNull("high", high);
        final int dimension = high.length;
        if (low != null && low.length != dimension) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedDimension_2, low.length, dimension));
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
    @Override
    public final int getDimension() {
        return ordinates.length >>> 1;
    }

    /**
     * Returns the valid minimum grid coordinates, inclusive.
     * The sequence contains a minimum value for each dimension of the grid coverage.
     *
     * @return the valid minimum grid coordinates, inclusive.
     */
    @Override
    public GridCoordinates getLow() {
        return new GridCoordinatesView(ordinates, 0);
    }

    /**
     * Returns the valid maximum grid coordinates, <strong>inclusive</strong>.
     * The sequence contains a maximum value for each dimension of the grid coverage.
     *
     * @return the valid maximum grid coordinates, <strong>inclusive</strong>.
     */
    @Override
    public GridCoordinates getHigh() {
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
    @Override
    public int getLow(final int index) {
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
    @Override
    public int getHigh(final int index) {
        final int dimension = getDimension();
        ArgumentChecks.ensureValidIndex(dimension, index);
        return ordinates[index + dimension];
    }

    /**
     * Returns the number of integer grid coordinates along the specified dimension.
     * This is equal to {@code getHigh(dimension) - getLow(dimension) + 1}.
     *
     * @param  index  the dimension for which to obtain the span.
     * @return the span at the given dimension.
     * @throws IndexOutOfBoundsException if the given index is negative or is equals or greater
     *         than the {@linkplain #getDimension() grid dimension}.
     * @throws ArithmeticException if the span is too large for the {@code int} primitive type.
     *
     * @see #getLow(int)
     * @see #getHigh(int)
     */
    @Override
    public int getSpan(final int index) {
        final int dimension = getDimension();
        ArgumentChecks.ensureValidIndex(dimension, index);
        return Math.toIntExact(ordinates[dimension + index] - (ordinates[index] - 1L));
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
     * @return the sub grid envelope.
     * @throws IndexOutOfBoundsException if an index is out of bounds.
     */
    public GridExtent subExtent(final int lower, final int upper) {
        final int dimension = getDimension();
        if (lower == 0 && upper == dimension) return this;
        ArgumentChecks.ensureValidIndexRange(dimension, lower, upper);
        final int newDim = upper - lower;
        if (newDim == dimension && getClass() == GridExtent.class) {
            return this;
        }
        final GridExtent sub = new GridExtent(newDim);
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
        return Arrays.hashCode(ordinates) ^ (int) serialVersionUID;
    }

    /**
     * Compares the specified object with this grid envelope for equality.
     *
     * @param  object  the object to compare with this grid envelope for equality.
     * @return {@code true} if the given object is equal to this grid envelope.
     */
    @Override
    public boolean equals(final Object object) {
        if (object instanceof GridExtent) {
            return Arrays.equals(ordinates, ((GridExtent) object).ordinates);
        }
        return false;
    }

    /**
     * Returns a string representation of this grid envelope. The returned string
     * is implementation dependent and is provided for debugging purposes only.
     */
    @Override
    public String toString() {
        final int dimension = getDimension();
        final StringBuilder buffer = new StringBuilder("GridEnvelope").append('[');
        for (int i=0; i<dimension; i++) {
            if (i != 0) buffer.append(", ");
            buffer.append(ordinates[i]).append('…').append(ordinates[i + dimension]);
        }
        return buffer.append(']').toString();
    }
}
