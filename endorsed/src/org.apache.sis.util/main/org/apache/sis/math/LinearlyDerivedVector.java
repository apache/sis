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
package org.apache.sis.math;

import java.util.Objects;
import java.io.Serializable;
import java.util.function.IntSupplier;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.measure.NumberRange;


/**
 * A vector derived from another vector by application of a scale factor and an offset.
 * This is used for example when data in a netCDF file are packed as integer values.
 *
 * <p>Several methods in this implementation requires a bijective relationship between {@code this} and {@link #base}.
 * This is mostly the case if coefficients are finite and {@link #scale} is non-zero, as asserted at construction time.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see Vector#transform(double, double)
 */
final class LinearlyDerivedVector extends Vector implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -2172866644024476121L;

    /**
     * The vector on which this vector is derived from.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private final Vector base;

    /**
     * The scale factor to apply.
     */
    private final double scale;

    /**
     * The offset to apply after the scale factor.
     */
    private final double offset;

    /**
     * Creates a new vector of derived data.
     */
    LinearlyDerivedVector(final Vector base, final double scale, final double offset) {
        this.base   = base;
        this.scale  = scale;
        this.offset = offset;
        assert Double.isFinite(scale) && scale != 0 : scale;
        assert Double.isFinite(offset) : offset;
    }

    /**
     * Returns the type of values resulting from the linear conversion applied by this vector.
     * Note that it does not means that this vector can store all values of that type.
     *
     * @see #get(int)
     */
    @Override
    public Class<Double> getElementType() {
        return Double.class;
    }

    /**
     * Double-precision values are not guaranteed to be convertible to single-precision floating point type.
     */
    @Override
    public boolean isSinglePrecision() {
        return false;
    }

    /**
     * Returns {@code true} if this vector contains only integer values. This implementation delegates
     * to the {@linkplain #base} vector if coefficients are integers, or scans all values otherwise.
     */
    @Override
    public boolean isInteger() {
        if (Numerics.isInteger(scale) && Numerics.isInteger(offset) && base.isInteger()) {
            return true;
        }
        return super.isInteger();
    }

    /**
     * Returns the number of elements in this vector. This is taken directly from the {@linkplain #base} vector.
     */
    @Override
    public int size() {
        return base.size();
    }

    /**
     * Returns {@code true} if this vector is empty or contains only {@code NaN} values.
     * The implementation delegates to the {@linkplain #base} vector since linear relationship
     * does not change whether values are NaN.
     */
    @Override
    public boolean isEmptyOrNaN() {
        return base.isEmptyOrNaN();
    }

    /**
     * Returns {@code true} if the value at the given index is {@code null} or {@code NaN}.
     * The implementation delegates to the {@linkplain #base} vector since linear relationship
     * does not change whether a value is NaN.
     */
    @Override
    public boolean isNaN(final int index) {
        return base.isNaN(index);
    }

    /**
     * Returns the value at the given index as a {@code double}.
     * This method is invoked by all others {@code fooValue()}.
     */
    @Override
    public double doubleValue(final int index) {
        return Math.fma(base.doubleValue(index), scale, offset);
    }

    /**
     * Returns a string representation of the value at the given index.
     * This implementation uses the {@code double} representation.
     */
    @Override
    public String stringValue(final int index) {
        return Double.toString(doubleValue(index));
    }

    /**
     * Returns the number at the given index as a {@link Double}.
     *
     * @see #getElementType()
     */
    @Override
    public Number get(final int index) {
        return doubleValue(index);
    }

    /**
     * Sets the number at the given index. This action is likely to loose precision
     * if the {@linkplain #base} vector stores values using integer primitive type.
     */
    @Override
    public Number set(final int index, final Number value) {
        return convert(base.set(index, inverse(value)));
    }

    /**
     * Sets a range of elements to the given number. This action is likely to loose precision
     * if the {@linkplain #base} vector stores values using integer primitive type.
     */
    @Override
    public void fill(final int fromIndex, final int toIndex, final Number value) {
        base.fill(fromIndex, toIndex, inverse(value));
    }

    /**
     * Returns the index of a search based one values at given indices (see javadoc in parent class for details).
     * This implementation forwards to backing vector on the assumption that the {@code this} ↔︎ {@link #base}
     * relationship is bijective.
     */
    @Override
    final int indexOf(final int toSearch, int index, final boolean equality) {
        return base.indexOf(toSearch, index, equality);
    }

    /**
     * Detects repetition patterns in the values contained in this vector.
     * This implementation forwards to backing vector on the assumption that
     * the {@code this} ↔︎ {@link #base} relationship is bijective.
     */
    @Override
    public int[] repetitions(final int... candidates) {
        return base.repetitions(candidates);
    }

    /**
     * Returns the increment between all consecutive values if this increment is constant, or {@code null} otherwise.
     * This implementation converts the value computed by the backing vector on the assumption that the {@code this}
     * ↔︎ {@link #base} relationship is a linear.
     */
    @Override
    public Number increment(final double tolerance) {
        Number inc = base.increment(tolerance / scale);
        if (inc != null && scale != 1) {
            inc = inc.doubleValue() * scale;
        }
        return inc;
    }

    /**
     * Applies the linear relationship on the given value.
     *
     * @param  value  the number to convert, or {@code null}.
     * @return the converted number (may be {@code null}).
     */
    private Number convert(Number value) {
        if (value != null) {
            value = Math.fma(value.doubleValue(), scale, offset);
        }
        return value;
    }

    /**
     * Applies the inverse linear function on the given value.
     *
     * @param  value  the number to inverse convert, or {@code null}.
     * @return the inverse converted number (may be {@code null}).
     */
    private Number inverse(Number value) {
        if (value != null) {
            value = (value.doubleValue() - offset) / scale;
        }
        return value;
    }

    /**
     * Converts the given range. This is used for delegating range calculation to the {@linkplain #base}
     * vector on the assumption that is will be more efficient than iterating ourselves on all values.
     *
     * @param  range  the range to convert, or {@code null}.
     * @return the converted range, or {@code null} if the given range was null.
     */
    private NumberRange<?> convert(NumberRange<?> range) {
        if (range != null) {
            Number min = range.getMinValue();
            Number max = range.getMaxValue();
            if (!Objects.equals(min, min = convert(min)) |          // Really | operator, not ||.
                !Objects.equals(max, max = convert(max)))
            {
                boolean isMinIncluded = range.isMinIncluded();
                boolean isMaxIncluded = range.isMaxIncluded();
                if (scale < 0) {
                    Number  tv = min; min = max; max = tv;
                    boolean ti = isMinIncluded; isMinIncluded = isMaxIncluded; isMaxIncluded = ti;
                }
                range = new NumberRange<>(Double.class,
                        (min != null) ? min.doubleValue() : null, isMinIncluded,
                        (max != null) ? max.doubleValue() : null, isMaxIncluded);
            }
        }
        return range;
    }

    /**
     * Returns the minimal and maximal values found in this vector. This implementation delegates to
     * the {@linkplain #base} vector (which may provide fast implementation) then convert the result.
     */
    @Override
    public NumberRange<?> range() {
        return convert(base.range());
    }

    /**
     * Computes the range of values at the indices provided by the given supplier. This implementation delegates
     * to the {@linkplain #base} vector (which may provide fast implementation) then convert the result.
     */
    @Override
    final NumberRange<?> range(final IntSupplier indices, int n) {
        return convert(base.range(indices, n));
    }

    /**
     * Returns a view which contain the values of this vector in a given index range.
     * This implementation delegates to {@linkplain #base} vector and wraps again.
     */
    @Override
    public Vector subSampling(final int first, final int step, final int length) {
        return new LinearlyDerivedVector(base.subSampling(first, step, length), scale, offset);
    }

    /**
     * Returns a view which contains the values of this vector at the given indexes.
     * This implementation delegates to {@linkplain #base} vector and wraps again.
     */
    @Override
    public Vector pick(final int... indices) {
        return new LinearlyDerivedVector(base.pick(indices), scale, offset);
    }

    /**
     * Returns the concatenation of this vector with the given one.
     * This implementation potentially delegate to {@linkplain #base} vector.
     */
    @Override
    final Vector createConcatenate(final Vector toAppend) {
        if (toAppend instanceof LinearlyDerivedVector) {
            final LinearlyDerivedVector d = (LinearlyDerivedVector) toAppend;
            if (d.scale == scale && d.offset == offset) {
                return new LinearlyDerivedVector(base.concatenate(d.base), scale, offset);
            }
        }
        return super.createConcatenate(toAppend);
    }

    /**
     * Concatenates the given transformation with the current transform.
     */
    @Override
    final Vector createTransform(final double s, final double t) {
        return base.transform(scale * s, Math.fma(offset, s, t));
    }

    /**
     * Copies all values in an array of double precision floating point numbers.
     * We override this method for efficiency. We do not override the {@code float}
     * versions for accuracy reasons.
     */
    @Override
    public double[] doubleValues() {
        final double[] array = base.doubleValues();
        for (int i=0; i<array.length; i++) {
            array[i] = Math.fma(array[i], scale, offset);
        }
        return array;
    }

    /**
     * Optimization of {@code equals} method for the case where the other object
     * is another {@code LinearlyDerivedVector} using the same linear relationship.
     */
    @Override
    public boolean equals(final Object object) {
        if (object instanceof LinearlyDerivedVector) {
            final LinearlyDerivedVector d = (LinearlyDerivedVector) object;
            if (d.scale == scale && d.offset == offset) {
                return base.equals(d.base);
            }
        }
        return super.equals(object);
    }

    /**
     * Optimization of {@code equals} method for the case where the other object
     * is another {@code LinearlyDerivedVector} using the same linear relationship.
     */
    @Override
    boolean equals(int lower, final int upper, final Vector other, int otherOffset) {
        if (other instanceof LinearlyDerivedVector) {
            final LinearlyDerivedVector d = (LinearlyDerivedVector) other;
            if (d.scale == scale && d.offset == offset) {
                return base.equals(lower, upper, d.base, otherOffset);
            }
        }
        return super.equals(lower, upper, other, otherOffset);
    }
}
