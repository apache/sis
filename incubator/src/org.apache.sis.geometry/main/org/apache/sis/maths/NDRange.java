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
package org.apache.sis.maths;

import java.util.Objects;
import org.opengis.coordinate.MismatchedDimensionException;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.ArgumentChecks;


/**
 * A range of values in <var>n</var> dimensions, defined by a lower and an upper bound in each dimension.
 * The bounds are stored as two {@link Vector}s, which keep their data type and their sample system.
 *
 * <p>Unlike {@link org.apache.sis.geometry.GeneralEnvelope}, this range does not wrap around: in each dimension,
 * the lower bound is less than or equal to the upper bound. A NaN bound makes the range empty in its dimension.</p>
 *
 * <p>The range can be expanded with {@link #add(ReadOnly.Vector)}. Otherwise its bounds are not shared:
 * the bounds given to the constructor are copied, and the bounds returned by the methods are copies.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class NDRange implements Envelope {
    /**
     * Lower and upper bounds, of the same dimension and sample system.
     */
    private final Vector<?> lower, upper;

    /**
     * Creates a range between the given bounds.
     *
     * @param  lower  lower bound in each dimension.
     * @param  upper  upper bound in each dimension.
     * @throws MismatchedDimensionException if the two bounds do not have the same dimension.
     * @throws IllegalArgumentException if the two bounds are not in the same reference system,
     *         or if a lower bound is greater than the upper bound.
     */
    public NDRange(final ReadOnly.Vector<?> lower, final ReadOnly.Vector<?> upper) {
        ArgumentChecks.ensureNonNull("lower", lower);
        ArgumentChecks.ensureNonNull("upper", upper);
        final int dimension = lower.getDimension();
        if (upper.getDimension() != dimension) {
            throw new MismatchedDimensionException("The lower bound has " + dimension
                    + " dimensions but the upper bound has " + upper.getDimension() + '.');
        }
        if (!CRS.equivalent(lower.getCoordinateReferenceSystem(), upper.getCoordinateReferenceSystem())) {
            throw new IllegalArgumentException("The lower and upper bounds are in different reference systems.");
        }
        for (int i = 0; i < dimension; i++) {
            if (lower.get(i) > upper.get(i)) {
                throw new IllegalArgumentException("The lower bound " + lower.get(i) + " is greater than the upper bound "
                        + upper.get(i) + " in dimension " + i + '.');
            }
        }
        this.lower = lower.copy();
        this.upper = upper.copy();
    }

    /**
     * Creates a range of the given number of dimensions, with bounds of type {@code DOUBLE} all set to 0.
     *
     * @param  dimension  number of dimensions, greater than zero.
     */
    public NDRange(final int dimension) {
        this(dimension, DataType.DOUBLE);
    }

    /**
     * Creates a range of the given number of dimensions, with bounds of the given type all set to 0.
     *
     * @param  dimension  number of dimensions, greater than zero.
     * @param  dataType   data type of the bounds.
     */
    public NDRange(final int dimension, final DataType dataType) {
        this(Vectors.create(positive(dimension), Objects.requireNonNull(dataType, "dataType")));
    }

    /**
     * Creates a range in the given reference system, with bounds of type {@code DOUBLE} all set to 0.
     *
     * @param  crs  reference system of the bounds.
     */
    public NDRange(final CoordinateReferenceSystem crs) {
        this(crs, DataType.DOUBLE);
    }

    /**
     * Creates a range in the given reference system, with bounds of the given type all set to 0.
     *
     * @param  crs       reference system of the bounds.
     * @param  dataType  data type of the bounds.
     */
    public NDRange(final CoordinateReferenceSystem crs, final DataType dataType) {
        this(Vectors.create(Objects.requireNonNull(crs, "crs"), Objects.requireNonNull(dataType, "dataType")));
    }

    /**
     * Creates a range in the given sample system, with bounds of type {@code DOUBLE} all set to 0.
     *
     * @param  system  sample system of the bounds.
     */
    public NDRange(final SampleSystem system) {
        this(system, DataType.DOUBLE);
    }

    /**
     * Creates a range in the given sample system, with bounds of the given type all set to 0.
     *
     * @param  system    sample system of the bounds.
     * @param  dataType  data type of the bounds.
     */
    public NDRange(final SampleSystem system, final DataType dataType) {
        this(Vectors.create(Objects.requireNonNull(system, "system"), Objects.requireNonNull(dataType, "dataType")));
    }

    /**
     * Returns the given number of dimensions after verifying that it is positive.
     */
    private static int positive(final int dimension) {
        ArgumentChecks.ensureStrictlyPositive("dimension", dimension);
        return dimension;
    }

    /**
     * Creates a range whose two bounds are the given vector, which is used directly for the lower bound.
     */
    private NDRange(final Vector<?> bound) {
        lower = bound;
        upper = bound.copy();
    }

    /**
     * Creates a range with the bounds of the given envelope, in double precision.
     * An envelope spanning the anti-meridian gives the whole range of its axis in that dimension.
     *
     * @param  envelope  the envelope to copy.
     */
    public NDRange(final Envelope envelope) {
        final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        final int dimension = envelope.getDimension();
        lower = (crs != null) ? Vectors.create(crs, DataType.DOUBLE) : Vectors.createDouble(dimension);
        upper = lower.copy();
        for (int i = 0; i < dimension; i++) {
            lower.set(i, envelope.getMinimum(i));
            upper.set(i, envelope.getMaximum(i));
        }
    }

    /**
     * Returns the reference system of the bounds, or {@code null} if unspecified.
     */
    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return lower.getCoordinateReferenceSystem();
    }

    /**
     * Returns the number of dimensions of this range.
     */
    @Override
    public int getDimension() {
        return lower.getDimension();
    }

    /**
     * Returns a copy of the lower bound.
     *
     * @return the lower bound in each dimension.
     */
    public Vector<?> getLower() {
        return lower.copy();
    }

    /**
     * Returns a copy of the upper bound.
     *
     * @return the upper bound in each dimension.
     */
    public Vector<?> getUpper() {
        return upper.copy();
    }

    /**
     * Returns a copy of the lower bound as a direct position.
     */
    @Override
    public DirectPosition getLowerCorner() {
        return Vectors.asDirectPostion(lower.copy());
    }

    /**
     * Returns a copy of the upper bound as a direct position.
     */
    @Override
    public DirectPosition getUpperCorner() {
        return Vectors.asDirectPostion(upper.copy());
    }

    @Override
    public double getMinimum(final int dimension) {
        return lower.get(dimension);
    }

    @Override
    public double getMaximum(final int dimension) {
        return upper.get(dimension);
    }

    @Override
    public double getMedian(final int dimension) {
        return (lower.get(dimension) + upper.get(dimension)) / 2;
    }

    @Override
    public double getSpan(final int dimension) {
        return upper.get(dimension) - lower.get(dimension);
    }

    /**
     * Sets the range in the given dimension. The bounds keep their data type: a value which cannot be
     * represented exactly is rounded outward, toward negative infinity for the lower bound and positive
     * infinity for the upper bound, so that the range contains the given values.
     *
     * @param  dimension  the dimension to set.
     * @param  min        the lower bound, less than or equal to the upper bound, or NaN for an empty range.
     * @param  max        the upper bound, greater than or equal to the lower bound, or NaN for an empty range.
     * @throws IndexOutOfBoundsException if the dimension is not valid.
     * @throws IllegalArgumentException if the lower bound is greater than the upper bound.
     */
    public void setRange(final int dimension, final double min, final double max) {
        Objects.checkIndex(dimension, lower.getDimension());
        if (min > max) {
            throw new IllegalArgumentException("The lower bound " + min + " is greater than the upper bound " + max + '.');
        }
        widen(lower, dimension, min, false);
        widen(upper, dimension, max, true);
    }

    /**
     * Sets all bounds to NaN, which makes this range empty in all dimensions.
     * NaN is representable only by the {@code FLOAT} and {@code DOUBLE} data types:
     * integer types store 0 instead.
     */
    public void setToNaN() {
        for (int i = 0; i < lower.getDimension(); i++) {
            lower.set(i, Double.NaN);
            upper.set(i, Double.NaN);
        }
    }

    /**
     * Expands this range for containing the given position.
     * A NaN coordinate is ignored, and a NaN bound stays NaN, as in {@link org.apache.sis.geometry.GeneralEnvelope}.
     * The bounds keep their data type: they are widened to the nearest integer, or to the nearest {@code float},
     * when the data type cannot represent the coordinate exactly, so that the position is contained.
     *
     * @param  position  the position to add.
     * @throws MismatchedDimensionException if the position does not have the dimension of this range.
     *
     * @see #add(DirectPosition)
     */
    public void add(final ReadOnly.Vector<?> position) throws MismatchedDimensionException {
        ensureDimensionMatches(position.getDimension());
        for (int i = 0; i < lower.getDimension(); i++) {
            add(i, position.get(i));
        }
    }

    /**
     * Expands this range for containing the given position.
     *
     * @param  position  the position to add.
     * @throws MismatchedDimensionException if the position does not have the dimension of this range.
     *
     * @see #add(ReadOnly.Vector)
     */
    public void add(final DirectPosition position) throws MismatchedDimensionException {
        ensureDimensionMatches(position.getDimension());
        for (int i = 0; i < lower.getDimension(); i++) {
            add(i, position.getCoordinate(i));
        }
    }

    /**
     * Expands this range in the given dimension for containing the given value.
     */
    private void add(final int dimension, final double value) {
        if (value < lower.get(dimension)) widen(lower, dimension, value, false);
        if (value > upper.get(dimension)) widen(upper, dimension, value, true);
    }

    /**
     * Sets a bound to the given value, rounded away from the inside of the range if the data type of the bound
     * cannot represent the value exactly.
     *
     * @param  bound      the lower or the upper bound.
     * @param  dimension  the dimension of the value.
     * @param  value      the new bound value.
     * @param  up         whether to round toward positive infinity (upper bound) or negative infinity (lower bound).
     */
    private static void widen(final Vector<?> bound, final int dimension, final double value, final boolean up) {
        switch (bound.getDataType()) {
            case DOUBLE: {
                bound.set(dimension, value);
                break;
            }
            case FLOAT: {
                float f = (float) value;
                if (up ? f < value : f > value) {
                    f = up ? Math.nextUp(f) : Math.nextDown(f);
                }
                bound.set(dimension, f);
                break;
            }
            default: {
                bound.set(dimension, up ? Math.ceil(value) : Math.floor(value));
                break;
            }
        }
    }

    /**
     * Returns whether the given position is inside this range or on its border.
     * A position with a NaN coordinate is not contained.
     *
     * @param  position  the position to test.
     * @return whether the position is inside this range or on its border.
     * @throws MismatchedDimensionException if the position does not have the dimension of this range.
     *
     * @see #contains(DirectPosition)
     */
    public boolean contains(final ReadOnly.Vector<?> position) throws MismatchedDimensionException {
        ensureDimensionMatches(position.getDimension());
        for (int i = 0; i < lower.getDimension(); i++) {
            if (!contains(i, position.get(i))) return false;
        }
        return true;
    }

    /**
     * Returns whether the given position is inside this range or on its border.
     *
     * @param  position  the position to test.
     * @return whether the position is inside this range or on its border.
     * @throws MismatchedDimensionException if the position does not have the dimension of this range.
     *
     * @see #contains(ReadOnly.Vector)
     */
    public boolean contains(final DirectPosition position) throws MismatchedDimensionException {
        ensureDimensionMatches(position.getDimension());
        for (int i = 0; i < lower.getDimension(); i++) {
            if (!contains(i, position.getCoordinate(i))) return false;
        }
        return true;
    }

    /**
     * Returns whether the given value is inside this range in the given dimension. False if a value is NaN.
     */
    private boolean contains(final int dimension, final double value) {
        return value >= lower.get(dimension) && value <= upper.get(dimension);
    }

    /**
     * Verifies that a position has the dimension of this range.
     */
    private void ensureDimensionMatches(final int dimension) {
        if (dimension != lower.getDimension()) {
            throw new MismatchedDimensionException("The position has " + dimension
                    + " dimensions but the range has " + lower.getDimension() + '.');
        }
    }

    /**
     * Returns whether the given object is a range with the same bounds.
     */
    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof NDRange other) && lower.equals(other.lower) && upper.equals(other.upper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lower, upper);
    }

    /**
     * Returns the lower and upper bounds, for example {@code NDRange[(1.0, 2.0) … (4.0, 6.0)]}.
     */
    @Override
    public String toString() {
        final var sb = new StringBuilder("NDRange[");
        for (final Vector<?> bound : new Vector<?>[] {lower, upper}) {
            if (bound == upper) sb.append(" … ");
            sb.append('(');
            for (int i = 0; i < bound.getDimension(); i++) {
                if (i != 0) sb.append(", ");
                sb.append(bound.get(i));
            }
            sb.append(')');
        }
        return sb.append(']').toString();
    }
}
