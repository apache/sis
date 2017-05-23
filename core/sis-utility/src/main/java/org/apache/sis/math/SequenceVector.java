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
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;


/**
 * A vector which is a sequence of increasing or decreasing values.
 * Values may be {@code long} or {@code double} types.
 *
 * @author  Martin Desruisseaux (MPO, Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
abstract class SequenceVector extends Vector implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7980737287789566091L;

    /**
     * The length of this vector.
     */
    final int length;

    /**
     * Creates a sequence of numbers of the given length.
     */
    SequenceVector(final int length) {
        this.length = length;
        if (length < 0) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalArgumentValue_2, "length", length));
        }
    }

    /**
     * {@code SequenceVector} values are always interpreted as signed values.
     */
    @Override
    public final boolean isUnsigned() {
        return false;
    }

    /**
     * Returns the vector size.
     */
    @Override
    public final int size() {
        return length;
    }

    /**
     * Unsupported operation since this vector is not modifiable.
     */
    @Override
    public final Number set(final int index, final Number value) {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableObject_1, "Vector"));
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
    static final class Doubles extends SequenceVector {
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
        private final double increment;

        /**
         * Creates a sequence of numbers in a given range of values using the given increment.
         *
         * @param  first      the first value, inclusive.
         * @param  increment  the difference between the values at two adjacent indexes.
         * @param  length     the length of the vector.
         */
        Doubles(final Number first, final Number increment, final int length) {
            super(length);
            this.first     = first.doubleValue();
            this.increment = increment.doubleValue();
        }

        /** Creates a new sequence for a subrange of this vector. */
        @Override Vector createSubSampling(final int offset, final int step, final int n) {
            return new Doubles(doubleValue(offset), increment*step, n);
        }

        /** Returns the type of elements. */
        @Override public Class<Double> getElementType() {
            return Double.class;
        }

        /** Returns {@code true} if this vector contains only integer values. */
        @Override public boolean isInteger() {
            return Math.floor(first) == first && Math.floor(increment) == increment;
        }

        /**
         * Returns {@code true} if this vector returns {@code NaN} values.
         */
        @Override public boolean isNaN(final int index) {
            return Double.isNaN(first) || Double.isNaN(increment);
        }

        /** Computes the value at the given index. */
        @Override public double doubleValue(final int index) {
            ArgumentChecks.ensureValidIndex(length, index);
            return first + increment*index;
        }

        /** Computes the value at the given index. */
        @Override public float floatValue(final int index) {
            return (float) doubleValue(index);
        }

        /** Returns the string representation of the value at the given index. */
        @Override public String stringValue(final int index) {
            return String.valueOf(doubleValue(index));
        }

        /** Computes the value at the given index. */
        @Override public Number get(final int index) {
            return doubleValue(index);
        }

        /** Returns the increment between all consecutive values */
        @Override public Number increment(final double tolerance) {
            return increment;
        }

        /** Computes the minimal and maximal values in this vector. */
        @Override public NumberRange<Double> range() {
            double min = first;
            double max = first + increment * (length - 1);
            if (max < min) {
                min = max;
                max = first;
            }
            return NumberRange.create(min, true, max, true);
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
         * @param  first      the first value, inclusive.
         * @param  increment  the difference between the values at two adjacent indexes.
         * @param  length     the length of the vector.
         */
        Longs(final Number first, final Number increment, final int length) {
            super(length);
            this.first     = first.longValue();
            this.increment = increment.longValue();
        }

        /** Creates a new sequence for a subrange of this vector. */
        @Override Vector createSubSampling(final int offset, final int step, final int n) {
            return new Longs(longValue(offset), increment*step, n);
        }

        /** Returns the type of elements. */
        @Override public Class<Long> getElementType() {
            return Long.class;
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
            return longValue(index);
        }

        /** Returns the increment between all consecutive values */
        @Override public Number increment(final double tolerance) {
            return increment;
        }

        /** Computes the minimal and maximal values in this vector. */
        @Override public NumberRange<Long> range() {
            long min = first;
            long max = first + increment * (length - 1);
            if (max < min) {
                min = max;
                max = first;
            }
            return NumberRange.create(min, true, max, true);
        }
    }
}
