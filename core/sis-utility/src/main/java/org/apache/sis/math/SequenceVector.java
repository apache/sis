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

import java.util.Arrays;
import java.io.Serializable;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.Numerics;


/**
 * A vector which is a sequence of increasing or decreasing values.
 * Values may be {@code long} or {@code double} types.
 *
 * @author  Martin Desruisseaux (MPO, Geomatys)
 * @version 1.1
 * @since   0.8
 * @module
 */
abstract class SequenceVector extends Vector implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2544089499300079707L;

    /**
     * The type of values in the vector.
     */
    final Class<? extends Number> type;

    /**
     * The length of this vector.
     */
    final int length;

    /**
     * Creates a sequence of numbers of the given length.
     */
    SequenceVector(final Class<? extends Number> type, final int length) {
        this.type   = type;
        this.length = length;
        if (length < 0) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalArgumentValue_2, "length", length));
        }
    }

    /**
     * Transforms the sequence. The result is always of {@code Double} type,
     * regardless the type of elements in this vector.
     */
    @Override
    final Vector createTransform(final double scale, final double offset) {
        return new Doubles(Double.class,
                doubleValue(0) * scale + offset,                // TODO: use Math.fma with JDK9.
                increment(0).doubleValue() * scale, length);
    }

    /**
     * Returns the type of elements.
     */
    @Override
    public final Class<? extends Number> getElementType() {
        return type;
    }

    /**
     * Returns the vector size.
     */
    @Override
    public final int size() {
        return length;
    }

    /**
     * Returns {@code true} if this vector is empty or contains only {@code NaN} values.
     */
    @Override
    public final boolean isEmptyOrNaN() {
        return (length == 0) || isNaN(0);
    }

    /**
     * Unsupported operation since this vector is not modifiable.
     */
    @Override
    public final Number set(final int index, final Number value) {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.CanNotStoreInVector_1, value));
    }

    /**
     * Returns {@code this} since Apache SIS can not create a more compact vector than this {@code SequenceVector}.
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public final Vector compress(final double tolerance) {
        return this;
    }

    /**
     * Creates the sequence as a floating point array.
     */
    @Override
    public double[] doubleValues() {
        if (increment(0).doubleValue() == 0) {
            final double[] array = new double[size()];
            Arrays.fill(array, doubleValue(0));
            return array;
        }
        return super.doubleValues();
    }

    /**
     * Creates the sequence as a floating point array.
     */
    @Override
    public float[] floatValues() {
        if (increment(0).doubleValue() == 0) {
            final float[] array = new float[size()];
            Arrays.fill(array, floatValue(0));
            return array;
        }
        return super.floatValues();
    }


    /**
     * A vector which is a sequence of increasing or decreasing {@code double} values.
     */
    static class Doubles extends SequenceVector {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -5222432536264284005L;

        /**
         * The value at index 0.
         */
        private final double first;

        /**
         * The difference between the values at two adjacent indexes.
         * May be positive, negative or zero.
         */
        final double increment;

        /**
         * Creates a sequence of numbers in a given range of values using the given increment.
         *
         * @param  type       the type of elements in the sequence.
         * @param  first      the first value, inclusive.
         * @param  increment  the difference between the values at two adjacent indexes.
         * @param  length     the length of the vector.
         */
        Doubles(final Class<? extends Number> type, final Number first, final Number increment, final int length) {
            super(type, length);
            this.first     = first.doubleValue();
            this.increment = increment.doubleValue();
        }

        /** Creates a new sequence for a subrange of this vector. */
        @Override Vector createSubSampling(final int offset, final int step, final int n) {
            return new Doubles(type, doubleValue(offset), increment*step, n);
        }

        /** Returns {@code true} if this vector contains only integer values. */
        @Override public final boolean isInteger() {
            return Math.floor(first) == first && Math.floor(increment) == increment;
        }

        /**
         * Returns {@code true} if this vector returns {@code NaN} values.
         */
        @Override public final boolean isNaN(final int index) {
            return Double.isNaN(first) || Double.isNaN(increment);
        }

        /** Computes the value at the given index. */
        @Override public final double doubleValue(final int index) {
            ArgumentChecks.ensureValidIndex(length, index);
            return first + increment*index;
            // TODO: use Math.fma with JDK9.
        }

        /** Returns the string representation of the value at the given index. */
        @Override public String stringValue(final int index) {
            return String.valueOf(doubleValue(index));
        }

        /** Computes the value at the given index. */
        @Override public Number get(final int index) {
            return Numbers.wrap(doubleValue(index), type);
        }

        /** Returns the increment between all consecutive values. */
        @Override public final Number increment(final double tolerance) {
            return Numerics.valueOf(increment);         // Always Double even if data type is Float.
        }

        /** Computes the minimal and maximal values in this vector. */
        @SuppressWarnings({"unchecked","rawtypes"})
        @Override public final NumberRange<?> range() {
            Number min = get(0);
            Number max = get(length - 1);
            if (((Comparable) max).compareTo((Comparable) min) < 0) {
                Number tmp = min;
                min = max;
                max = tmp;
            }
            return new NumberRange(type, min, true, max, true);
        }
    }


    /**
     * A vector which is a sequence of increasing or decreasing {@code float} values.
     */
    static final class Floats extends Doubles {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 7972249253456554448L;

        /**
         * Creates a sequence of numbers in a given range of values using the given increment.
         */
        Floats(final Class<? extends Number> type, final Number first, final Number increment, final int length) {
            super(type, first, increment, length);
        }

        /** Creates a new sequence for a subrange of this vector. */
        @Override Vector createSubSampling(final int offset, final int step, final int n) {
            return new Floats(type, doubleValue(offset), increment*step, n);
        }

        /** Computes the value at the given index. */
        @Override public Number get(final int index) {
            return floatValue(index);
        }

        /** Returns the string representation of the value at the given index. */
        @Override public String stringValue(final int index) {
            return String.valueOf(floatValue(index));
        }
    }


    /**
     * A vector which is a sequence of increasing or decreasing {@code long} values.
     */
    static final class Longs extends SequenceVector {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 8959308953555132379L;

        /**
         * The value at index 0.
         */
        private final long first;

        /**
         * The difference between the values at two adjacent indexes.
         * May be positive, negative or zero.
         */
        private final long increment;

        /**
         * Creates a sequence of numbers in a given range of values using the given increment.
         *
         * @param  type       the type of elements in the sequence.
         * @param  first      the first value, inclusive.
         * @param  increment  the difference between the values at two adjacent indexes.
         * @param  length     the length of the vector.
         */
        Longs(final Class<? extends Number> type, final Number first, final Number increment, final int length) {
            super(type, length);
            this.first     = first.longValue();
            this.increment = increment.longValue();
        }

        /** Creates a new sequence for a subrange of this vector. */
        @Override Vector createSubSampling(final int offset, final int step, final int n) {
            return new Longs(type, longValue(offset), increment*step, n);
        }

        /** Returns {@code true} since this vector contains only integer values. */
        @Override public boolean isInteger() {
            return true;
        }

        /** Returns {@code false} since this vector never return {@code NaN} values. */
        @Override public boolean isNaN(final int index) {
            return false;
        }

        /** Computes the value at the given index. */
        @Override public double doubleValue(final int index) {
            return longValue(index);
        }

        /** Computes the value at the given index. */
        @Override public float floatValue(final int index) {
            return longValue(index);
        }

        /** Computes the value at the given index. */
        @Override public long longValue(final int index) {
            ArgumentChecks.ensureValidIndex(length, index);
            return first + increment*index;
        }

        /** Returns the string representation of the value at the given index. */
        @Override public String stringValue(final int index) {
            return String.valueOf(longValue(index));
        }

        /** Computes the value at the given index. */
        @Override public Number get(final int index) {
            return Numbers.wrap(longValue(index), type);
        }

        /** Returns the increment between all consecutive values */
        @Override public Number increment(final double tolerance) {
            return Numbers.wrap(increment, type);
        }

        /** Computes the minimal and maximal values in this vector. */
        @SuppressWarnings({"unchecked","rawtypes"})
        @Override public NumberRange<?> range() {
            long min = first;
            long max = first + increment * (length - 1);
            if (max < min) {
                min = max;
                max = first;
            }
            return new NumberRange(type, Numbers.wrap(min, type), true, Numbers.wrap(max, type), true);
        }
    }
}
