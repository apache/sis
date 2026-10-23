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
import org.apache.sis.util.ArgumentChecks;


/**
 * A range of values in <var>n</var> dimensions, defined by a lower and an upper bound in each dimension.
 * The bounds are stored as two {@link Vector}s, which keep their data type and their sample system.
 *
 * <p>Unlike {@link org.apache.sis.geometry.GeneralEnvelope}, this range does not wrap around: in each dimension,
 * the lower bound is less than or equal to the upper bound. A NaN bound makes the range empty in its dimension.</p>
 *
 * <p>The range can be expanded with {@link #add(Vector)}. Otherwise its bounds are not shared:
 * the bounds given to the constructor are copied, and the bounds returned by the methods are copies.</p>
 *
 * @author Johann Sorel (Geomatys)
 */
public final class NDRange implements Envelope {

    /**
     * Sample system of the bounds, of the same dimension than the bounds.
     */
    private final SampleSystem system;

    /**
     * Lower and upper bounds, of the same dimension.
     * Vectors are immutable, the bounds are replaced by new vectors when the range changes.
     */
    private Vector<?> lower, upper;

    /**
     * Creates a range between the given bounds, in the given reference system.
     *
     * @param  crs    reference system of the bounds, or {@code null} if unspecified.
     * @param  lower  lower bound in each dimension.
     * @param  upper  upper bound in each dimension.
     * @throws MismatchedDimensionException if the two bounds do not have the same dimension.
     * @throws IllegalArgumentException if a lower bound is greater than the upper bound.
     */
    public NDRange(final CoordinateReferenceSystem crs, final Vector<?> lower, final Vector<?> upper) {
        this(system(crs, lower.getDimension()), lower, upper);
    }

    /**
     * Creates a range between the given bounds, in an unspecified reference system.
     *
     * @param  lower  lower bound in each dimension.
     * @param  upper  upper bound in each dimension.
     * @throws MismatchedDimensionException if the two bounds do not have the same dimension.
     * @throws IllegalArgumentException if a lower bound is greater than the upper bound.
     */
    public NDRange(final Vector<?> lower, final Vector<?> upper) {
        this(SampleSystem.ofSize(lower.getDimension()), lower, upper);
    }

    /**
     * Creates a range between the given bounds, in the given sample system.
     *
     * @param  system sample system of the bounds.
     * @param  lower  lower bound in each dimension.
     * @param  upper  upper bound in each dimension.
     * @throws MismatchedDimensionException if the two bounds do not have the same dimension.
     * @throws IllegalArgumentException if a lower bound is greater than the upper bound.
     */
    public NDRange(SampleSystem system, final Vector<?> lower, final Vector<?> upper) {
        ArgumentChecks.ensureNonNull("system", system);
        ArgumentChecks.ensureNonNull("lower", lower);
        ArgumentChecks.ensureNonNull("upper", upper);
        this.system = system;
        final int dimension = lower.getDimension();
        if (upper.getDimension() != dimension) {
            throw new MismatchedDimensionException("The lower bound has " + dimension
                    + " dimensions but the upper bound has " + upper.getDimension() + '.');
        }
        if (system.getSize() != dimension) {
            throw new MismatchedDimensionException("The sample system has " + system.getSize()
                    + " dimensions but the bounds have " + dimension + '.');
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
        this(SampleSystem.ofSize(dimension), Vectors.create(positive(dimension), Objects.requireNonNull(dataType, "dataType")));
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
        this(SampleSystem.of(crs), Vectors.create(crs.getCoordinateSystem().getDimension(), dataType));
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
        this(system, Vectors.create(system.getSize(), dataType));
    }

    /**
     * Returns the sample system of the given reference system, or an undefined one if the CRS is null.
     */
    private static SampleSystem system(final CoordinateReferenceSystem crs, final int dimension) {
        return (crs != null) ? SampleSystem.of(crs) : SampleSystem.ofSize(dimension);
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
    private NDRange(SampleSystem system, final Vector<?> bound) {
        this.system = system;
        lower = bound;
        upper = bound;
    }

    /**
     * Creates a range with the bounds of the given envelope, in double precision.
     * An envelope spanning the anti-meridian gives the whole range of its axis in that dimension.
     *
     * @param  envelope  the envelope to copy.
     */
    public NDRange(final Envelope envelope) {
        final int dimension = envelope.getDimension();
        system = system(envelope.getCoordinateReferenceSystem(), dimension);
        final double[] min = new double[dimension];
        final double[] max = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            min[i] = envelope.getMinimum(i);
            max[i] = envelope.getMaximum(i);
        }
        lower = Vectors.create(min, DataType.DOUBLE);
        upper = Vectors.create(max, DataType.DOUBLE);
    }

    /**
     * Returns the reference system of the bounds, or {@code null} if unspecified.
     */
    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return system.getCoordinateReferenceSystem();
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
        return Vectors.asDirectPostion(lower.copy(), getCoordinateReferenceSystem());
    }

    /**
     * Returns a copy of the upper bound as a direct position.
     */
    @Override
    public DirectPosition getUpperCorner() {
        return Vectors.asDirectPostion(upper.copy(), getCoordinateReferenceSystem());
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
        final double[] lowers = lower.toArrayDouble();
        final double[] uppers = upper.toArrayDouble();
        lowers[dimension] = widen(lower.getDataType(), min, false);
        uppers[dimension] = widen(upper.getDataType(), max, true);
        lower = Vectors.create(lowers, lower.getDataType());
        upper = Vectors.create(uppers, upper.getDataType());
    }

    /**
     * Sets all bounds to NaN, which makes this range empty in all dimensions.
     * NaN is representable only by the {@code FLOAT} and {@code DOUBLE} data types:
     * integer types store 0 instead.
     */
    public void setToNaN() {
        final double[] nan = new double[lower.getDimension()];
        java.util.Arrays.fill(nan, Double.NaN);
        lower = Vectors.create(nan, lower.getDataType());
        upper = Vectors.create(nan, upper.getDataType());
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
    public void add(final Vector<?> position) throws MismatchedDimensionException {
        ensureDimensionMatches(position.getDimension());
        add(position.toArrayDouble());
    }

    /**
     * Expands this range for containing the given position.
     *
     * @param  position  the position to add.
     * @throws MismatchedDimensionException if the position does not have the dimension of this range.
     *
     * @see #add(Vector)
     */
    public void add(final DirectPosition position) throws MismatchedDimensionException {
        ensureDimensionMatches(position.getDimension());
        add(position.getCoordinates());
    }

    /**
     * Expands this range for containing the given position coordinates.
     * The bounds are replaced only if they change.
     */
    private void add(final double[] position) {
        double[] lowers = null, uppers = null;
        for (int i = 0; i < position.length; i++) {
            final double value = position[i];
            if (value < lower.get(i)) {
                if (lowers == null) lowers = lower.toArrayDouble();
                lowers[i] = widen(lower.getDataType(), value, false);
            }
            if (value > upper.get(i)) {
                if (uppers == null) uppers = upper.toArrayDouble();
                uppers[i] = widen(upper.getDataType(), value, true);
            }
        }
        if (lowers != null) lower = Vectors.create(lowers, lower.getDataType());
        if (uppers != null) upper = Vectors.create(uppers, upper.getDataType());
    }

    /**
     * Rounds a bound value away from the inside of the range if the data type of the bound
     * cannot represent the value exactly.
     *
     * @param  dataType   data type of the bound.
     * @param  value      the new bound value.
     * @param  up         whether to round toward positive infinity (upper bound) or negative infinity (lower bound).
     * @return the bound value to store.
     */
    private static double widen(final DataType dataType, final double value, final boolean up) {
        switch (dataType) {
            case DOUBLE: {
                return value;
            }
            case FLOAT: {
                float f = (float) value;
                if (up ? f < value : f > value) {
                    f = up ? Math.nextUp(f) : Math.nextDown(f);
                }
                return f;
            }
            default: {
                return up ? Math.ceil(value) : Math.floor(value);
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
    public boolean contains(final Vector<?> position) throws MismatchedDimensionException {
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
     * @see #contains(Vector)
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
     * Returns whether the given envelope intersects this range, within the given tolerance.
     * Envelopes separated by no more than the tolerance in every dimension are considered as intersecting,
     * so that envelopes touching on their border intersect even with a tolerance of zero.
     * A NaN bound in this range or in the envelope makes them disjoint, the range being empty in that dimension.
     * The envelope is assumed in the reference system of this range, without verification.
     *
     * @param  envelope   the envelope to test.
     * @param  tolerance  the largest distance between the envelope and this range for considering them as intersecting.
     * @return whether the envelope intersects this range or is separated from it by no more than the tolerance.
     * @throws MismatchedDimensionException if the envelope does not have the dimension of this range.
     * @throws IllegalArgumentException if the tolerance is negative or NaN.
     */
    public boolean intersects(final Envelope envelope, final double tolerance) throws MismatchedDimensionException {
        ensureDimensionMatches("envelope", envelope.getDimension());
        ArgumentChecks.ensurePositive("tolerance", tolerance);
        for (int i = 0; i < lower.getDimension(); i++) {
            // Written for being false if a bound is NaN.
            if (!(envelope.getMinimum(i) <= upper.get(i) + tolerance && lower.get(i) <= envelope.getMaximum(i) + tolerance)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Verifies that a position has the dimension of this range.
     */
    private void ensureDimensionMatches(final int dimension) {
        ensureDimensionMatches("position", dimension);
    }

    /**
     * Verifies that an object of the given name, for example a position or an envelope, has the dimension of this range.
     */
    private void ensureDimensionMatches(final String name, final int dimension) {
        if (dimension != lower.getDimension()) {
            throw new MismatchedDimensionException("The " + name + " has " + dimension
                    + " dimensions but the range has " + lower.getDimension() + '.');
        }
    }

    /**
     * Returns whether the given object is a range with the same bounds and sample system.
     */
    @Override
    public boolean equals(final Object obj) {
        return (obj instanceof NDRange other) && lower.equals(other.lower) && upper.equals(other.upper)
                && system.equals(other.system);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lower, upper, system);
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
