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
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.feature.internal.Resources;
import org.apache.sis.util.StringBuilders;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.resources.Errors;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.coordinate.MismatchedDimensionException;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.coverage.grid.GridCoordinates;


/**
 * Grid coordinates which may have fraction digits after the integer part.
 * Grid coordinates specify the location of a cell within a {@link GridCoverage}.
 * They are normally integer numbers, but fractional parts may exist for example
 * after converting a geospatial {@link DirectPosition} to grid coordinates.
 * Preserving that fractional part is needed for interpolations.
 * This class can store such fractional part and can also compute a {@link GridExtent}
 * containing the coordinates, which can be used for requesting data for interpolations.
 *
 * <p>Current implementation stores coordinate values as {@code double} precision floating-point numbers
 * and {@linkplain Math#round(double) rounds} them to 64-bits integers on the fly. If a {@code double}
 * cannot be {@linkplain #getCoordinateValue(int) returned} as a {@code long}, or if a {@code long}
 * cannot be {@linkplain #setCoordinateValue(int, long) stored} as a {@code double}, then an
 * {@link ArithmeticException} is thrown.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 *
 * @see GridCoverage.Evaluator#toGridCoordinates(DirectPosition)
 *
 * @since 1.1
 */
public class FractionalGridCoordinates implements GridCoordinates, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5652265407347129550L;

    /**
     * The grid coordinates as floating-point numbers.
     */
    private final double[] coordinates;

    /**
     * Creates a new grid coordinates with the given number of dimensions.
     *
     * <h4>Usage note</h4>
     * {@code FractionalGridCoordinates} are usually not created directly, but are instead obtained
     * indirectly for example from the {@linkplain GridCoverage.Evaluator#toGridCoordinates(DirectPosition)
     * conversion of a geospatial position}.
     *
     * @param  dimension  the number of dimensions.
     */
    public FractionalGridCoordinates(final int dimension) {
        coordinates = new double[dimension];
    }

    /**
     * Creates a new grid coordinates with the given coordinates.
     * The array length is the number of dimensions.
     *
     * @param  coordinates  the grid coordinates.
     */
    FractionalGridCoordinates(final double[] coordinates) {
        this.coordinates = coordinates;
    }

    /**
     * Creates a new grid coordinates initialized to a copy of the given coordinates.
     *
     * @param  other  the coordinates to copy.
     */
    public FractionalGridCoordinates(final FractionalGridCoordinates other) {
        coordinates = other.coordinates.clone();
    }

    /**
     * Returns the number of dimension of this grid coordinates.
     *
     * @return  the number of dimensions.
     */
    @Override
    public int getDimension() {
        return coordinates.length;
    }

    /**
     * Returns one integer value for each dimension of the grid.
     * The default implementation invokes {@link #getCoordinateValue(int)}
     * for each element in the returned array.
     *
     * @return a copy of the coordinates. Changes in the returned array will
     *         not be reflected back in this {@code GridCoordinates} object.
     * @throws ArithmeticException if a coordinate value is outside the range
     *         of values representable as a 64-bits integer value.
     */
    @Override
    public long[] getCoordinateValues() {
        final long[] indices = new long[coordinates.length];
        for (int i=0; i<indices.length; i++) {
            indices[i] = getCoordinateValue(i);
        }
        return indices;
    }

    /**
     * Returns the grid coordinate value at the specified dimension.
     * Floating-point values are rounded to the nearest 64-bits integer values.
     * If the coordinate value is NaN or outside the range of {@code long} values,
     * then an {@link ArithmeticException} is thrown.
     *
     * @param  dimension  the dimension for which to obtain the coordinate value.
     * @return the coordinate value at the given dimension,
     *         {@linkplain Math#round(double) rounded} to nearest integer.
     * @throws IndexOutOfBoundsException if the given index is negative or is
     *         equal or greater than the {@linkplain #getDimension grid dimension}.
     * @throws ArithmeticException if the coordinate value is outside the range
     *         of values representable as a 64-bits integer value.
     */
    @Override
    public long getCoordinateValue(final int dimension) {
        final double value = coordinates[dimension];
        if (value >= ValuesAtPointIterator.DOMAIN_MINIMUM && value <= ValuesAtPointIterator.DOMAIN_MAXIMUM) {
            return Math.round(value);
        }
        throw new ArithmeticException(Resources.format(Resources.Keys.UnconvertibleGridCoordinate_2, "long", value));
    }

    /**
     * Returns a grid coordinate value together with its fractional part, if any.
     *
     * @param  dimension  the dimension for which to obtain the coordinate value.
     * @return the coordinate value at the given dimension.
     * @throws IndexOutOfBoundsException if the given index is negative or is
     *         equal or greater than the {@linkplain #getDimension grid dimension}.
     */
    public double getCoordinateFractional(final int dimension) {
        return coordinates[dimension];
    }

    /**
     * Sets the coordinate value at the specified dimension.
     * The given value shall be convertible to {@code double} without precision lost.
     *
     * @param  dimension  the dimension for which to set the coordinate value.
     * @param  value      the new value.
     * @throws IndexOutOfBoundsException if the given index is negative or is
     *         equal or greater than the {@linkplain #getDimension grid dimension}.
     * @throws ArithmeticException if this method cannot store the given grid coordinate
     *         without precision lost.
     */
    @Override
    public void setCoordinateValue(final int dimension, final long value) {
        if ((coordinates[dimension] = value) != value) {
            throw new ArithmeticException(Resources.format(Resources.Keys.UnconvertibleGridCoordinate_2, "double", value));
        }
    }

    /**
     * Creates a new grid extent around this grid coordinates. The returned extent will have the same number
     * of dimensions than this grid coordinates. For each dimension <var>i</var> the following relationships
     * will hold (where {@code extent} is the return value):
     *
     * <ol>
     *   <li>If <code>extent.{@linkplain GridExtent#getSize(int) getSize}(i)</code> ≥ 2 and no shift (see below) then:<ul>
     *      <li><code>extent.{@linkplain GridExtent#getLow(int)  getLow}(i)</code> ≤
     *          <code>{@linkplain #getCoordinateFractional(int)  getCoordinateFractional}(i)</code></li>
     *      <li><code>extent.{@linkplain GridExtent#getHigh(int) getHigh}(i)</code> ≥
     *          <code>{@linkplain #getCoordinateFractional(int)  getCoordinateFractional}(i)</code></li>
     *   </ul></li>
     *   <li>If {@code bounds.getSize(i)} ≥ {@code size[i]} and {@code size[i]} ≠ 0 then:<ul>
     *      <li><code>extent.{@linkplain GridExtent#getSize(int) getSize}(i)</code> = {@code size[i]}</li>
     *   </ul></li>
     * </ol>
     *
     * <p>The {@code size} argument is optional and can be incomplete (i.e. the number of {@code size} values can be
     * less than the number of dimensions). For each dimension <var>i</var>, if a {@code size[i]} value is provided
     * and is not zero, then this method tries to expand the extent in that dimension to the specified {@code size[i]}
     * value as shown in constraint #2 above. Otherwise the default size is the smallest possible extent that met
     * constraint #1 above, clipped to the {@code bounds}. This implies a size of 1 if the grid coordinate in that
     * dimension is an integer, or a size of 2 (before clipping to the bounds) if the grid coordinate has a fractional
     * part.</p>
     *
     * <p>The {@code bounds} argument is also optional.
     * If non-null, then this method enforces the following additional rules:</p>
     *
     * <ul>
     *   <li>Coordinates rounded to nearest integers must be inside the given bounds,
     *       otherwise a {@link PointOutsideCoverageException} is thrown.</li>
     *   <li>If the computed extent overlaps an area outside the bounds, then the extent will be shifted (if an explicit
     *       size was given) or clipped (if automatic size is used) in order to be be fully contained inside the bounds.</li>
     *   <li>If a given size is larger than the corresponding bounds {@linkplain GridExtent#getSize(int) size},
     *       then the returned extent will be clipped to the bounds.</li>
     * </ul>
     *
     * <p>In all cases, this method tries to keep the grid coordinates close to the center of the returned extent.
     * A shift may exist if necessary for keeping the extent inside the {@code bounds} argument, but will never
     * move the grid coordinates outside the [<var>low</var> … <var>high</var>+1) range of returned extent.</p>
     *
     * @param  bounds  if the coordinates shall be contained inside a grid, that grid extent. Otherwise {@code null}.
     * @param  size    the desired extent sizes as strictly positive numbers, or 0 sentinel values for automatic
     *                 sizes (1 or 2 depending on bounds and coordinate values). This array may have any length;
     *                 if shorter than the number of dimensions, missing values default to 0.
     *                 If longer than the number of dimensions, extra values are ignored.
     * @throws IllegalArgumentException if a {@code size} value is negative.
     * @throws ArithmeticException if a coordinate value is outside the range of {@code long} values.
     * @throws MismatchedDimensionException if {@code bounds} dimension is not equal to grid coordinates dimension.
     * @throws PointOutsideCoverageException if the grid coordinates (rounded to nearest integers) are outside the
     *         given bounds.
     * @return a grid extent of the given size (if possible) containing those grid coordinates.
     *
     * @deprecated Not used anymore because this method leads to a multiplication of very small read operations.
     */
    @Deprecated(since = "1.6", forRemoval = true)
    public GridExtent toExtent(final GridExtent bounds, final long... size) {
        final int dimension = coordinates.length;
        if (bounds != null) {
            final int bd = bounds.getDimension();
            if (bd != dimension) {
                throw new org.opengis.geometry.MismatchedDimensionException(Errors.format(
                        Errors.Keys.MismatchedDimension_3, "bounds", dimension, bd));
            }
        }
        final long[] extent = GridExtent.allocate(dimension);
        for (int i=0; i<dimension; i++) {
            final double value = coordinates[i];
            if (!(value >= Long.MIN_VALUE && value <= Long.MAX_VALUE)) {        // Use ! for catching NaN values.
                throw new ArithmeticException(Resources.format(
                        Resources.Keys.UnconvertibleGridCoordinate_2, "long", value));
            }
            long margin = 0;
            if (i < size.length) {
                margin = size[i];
                if (margin < 0) {
                    throw new IllegalArgumentException(Errors.format(
                            Errors.Keys.NegativeArgument_2, Strings.toIndexed("size", i), margin));
                }
            }
            /*
             * The lower/upper values are given by Math.floor/ceil respectively (may be equal).
             * However, we do an exception to this rule if user asked explicitly for a size of 1.
             * In such case we can no longer enforce the `lower ≤ value ≤ upper` rule. The best
             * we can do is to take the nearest neighbor.
             */
            final long nearest = Math.round(value);
            long lower, upper;
            if (margin == 1) {
                lower = upper = nearest;
            } else {
                lower = (long) Math.floor(value);       // Inclusive.
                upper = (long) Math.ceil (value);       // Inclusive too (lower == upper if value is an integer).
                if (margin != 0) {
                    margin -= (upper - lower + 1);      // Total number of cells to add.
                    assert margin >= 0 : margin;        // Because (upper - lower + 1) ≤ 2
                    if ((margin & 1) != 0) {
                        if (nearest >= upper) {
                            upper = Math.incrementExact(upper);
                        } else {
                            lower = Math.decrementExact(lower);
                        }
                    }
                    margin >>= 1;     // Number of cells to add on each side.
                    lower  = Math.subtractExact(lower, margin);
                    upper  = Math.addExact(upper, margin);
                    margin = 2;       // Any value different than 0 for remembering that it was explicitly specified.
                }
            }
            /*
             * At this point the grid range has been computed (lower to upper).
             * Shift it if needed for keeping it inside the enclosing extent.
             */
            if (bounds != null) {
                final long validMin = bounds.getLow(i);
                final long validMax = bounds.getHigh(i);
                if (nearest > validMax || nearest < validMin) {
                    final var b = new StringBuilder();
                    writeCoordinates(b);
                    throw new PointOutsideCoverageException(
                            Resources.format(Resources.Keys.GridCoordinateOutsideCoverage_4,
                            bounds.getAxisIdentification(i,i), validMin, validMax, b.toString()));
                }
                if (upper > validMax) {
                    if (margin != 0) {      // In automatic mode (margin = 0) just clip, don't shift.
                        /*
                         * Because (upper - validMax) is always positive, then (t > lower) would mean
                         * that we have an overflow. In such cases we do not need the result since we
                         * know that we are outside the enclosing extent anyway.
                         */
                        final long t = lower - Math.subtractExact(upper, validMax);
                        lower = (t >= validMin && t <= lower) ? t : validMin;
                    }
                    upper = validMax;
                }
                if (lower < validMin) {
                    if (margin != 0) {
                        final long t = upper + Math.subtractExact(validMin, lower);
                        upper = (t <= validMax && t >= upper) ? t : validMax;           // Same rational as above.
                    }
                    lower = validMin;
                }
            }
            extent[i] = lower;
            extent[i+dimension] = upper;
        }
        return new GridExtent(bounds, extent);
    }

    /**
     * Returns the grid coordinates converted to a geospatial position using the given transform.
     * This is the reverse of {@link GridCoverage.Evaluator#toGridCoordinates(DirectPosition)}.
     * The {@code gridToCRS} argument is typically {@link GridGeometry#getGridToCRS(PixelInCell)}
     * with {@link PixelInCell#CELL_CENTER}.
     *
     * @param  gridToCRS  the transform to apply on grid coordinates.
     * @return the grid coordinates converted using the given transform.
     * @throws TransformException if the grid coordinates cannot be converted by {@code gridToCRS}.
     *
     * @see GridCoverage.Evaluator#toGridCoordinates(DirectPosition)
     */
    public DirectPosition toPosition(final MathTransform gridToCRS) throws TransformException {
        final var position = new GeneralDirectPosition(gridToCRS.getTargetDimensions());
        gridToCRS.transform(coordinates, 0, position.coordinates, 0, 1);
        return position;
    }

    /**
     * Returns a string representation of this grid coordinates for debugging purposes.
     *
     * @return a string representation for debugging purposes.
     */
    @Override
    public String toString() {
        final var buffer = new StringBuilder("GridCoordinates[");
        writeCoordinates(buffer);
        return buffer.append(']').toString();
    }

    /**
     * Writes coordinates in the given buffer.
     */
    private void writeCoordinates(final StringBuilder buffer) {
        for (int i=0; i<coordinates.length; i++) {
            if (i != 0) buffer.append(' ');
            StringBuilders.trimFractionalPart(buffer.append(coordinates[i]));
        }
    }

    /**
     * Returns a hash code value for this grid coordinates.
     *
     * @return a hash code value.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(coordinates) ^ (int) serialVersionUID;
    }

    /**
     * Compares this grid coordinates with the specified object for equality.
     *
     * @param  object  the object to compares with this grid coordinates.
     * @return {@code true} if the given object is equal to this grid coordinates.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {                           // Slight optimization.
            return true;
        }
        if (object != null && object.getClass() == getClass()) {
            return Arrays.equals(((FractionalGridCoordinates) object).coordinates, coordinates);
        }
        return false;
    }
}
