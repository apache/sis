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

import java.io.Serializable;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.Numbers.*;
import static org.apache.sis.util.ArgumentChecks.ensureValidIndex;


/**
 * A vector which is a sequence of numbers.
 *
 * @author  Martin Desruisseaux (MPO, Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class SequenceVector extends Vector implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7980737287789566091L;

    /**
     * The element type, or {@code null} if values are NaN.
     */
    private final Class<? extends Number> type;

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
     * The length of this vector.
     */
    private final int length;

    /**
     * Creates an empty vector of the given type.
     */
    SequenceVector(final Class<? extends Number> type) {
        this.type = type;
        first     = 0;
        increment = 0;
        length    = 0;
    }

    /**
     * Creates a sequence of numbers in a given range of values using the given increment.
     *
     * @param  first      the first value, inclusive.
     * @param  increment  the difference between the values at two adjacent indexes.
     * @param  length     the length of the vector.
     */
    public SequenceVector(final double first, final double increment, final int length) {
        if (length < 0) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalArgumentValue_2, "length", length));
        }
        this.first     = first;
        this.increment = increment;
        this.length    = length;
        if (Double.isNaN(first) || Double.isNaN(increment)) {
            type = null;
        } else {
            Class<? extends Number> t = narrowestClass(first);
            t = widestClass(t, narrowestClass(first + increment));
            t = widestClass(t, narrowestClass(first + increment*(length-1)));
            type = t;
        }
    }

    /**
     * Returns the type of elements.
     */
    @Override
    public Class<? extends Number> getElementType() {
        // Float is the smallest type capable to hold NaN.
        return (type != null) ? type : Float.class;
    }

    /**
     * {@code SequenceVector} values are always interpreted as signed values.
     */
    @Override
    public boolean isUnsigned() {
        return false;
    }

    /**
     * Returns the vector size.
     */
    @Override
    public int size() {
        return length;
    }

    /**
     * Returns {@code true} if this vector returns {@code NaN} values.
     */
    @Override
    public boolean isNaN(final int index) {
        return type == null;
    }

    /**
     * Computes the value at the given index.
     */
    @Override
    public double doubleValue(final int index) throws IndexOutOfBoundsException {
        ensureValidIndex(length, index);
        return first + increment*index;
    }

    /**
     * Computes the value at the given index.
     */
    @Override
    public float floatValue(final int index) throws IndexOutOfBoundsException {
        return (float) doubleValue(index);
    }

    /**
     * Returns the string representation of the value at the given index.
     */
    @Override
    public String stringValue(final int index) throws IndexOutOfBoundsException {
        return String.valueOf(doubleValue(index));
    }

    /**
     * Computes the value at the given index.
     */
    @Override
    public Number get(final int index) throws IndexOutOfBoundsException {
        return doubleValue(index);
    }

    /**
     * Unsupported operation since this vector is not modifiable.
     */
    @Override
    public Number set(final int index, final Number value) {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableObject_1, "Vector"));
    }

    /**
     * Computes the minimal and maximal values in this vector.
     * This is easily computed from the first value and the increment.
     */
    @Override
    public NumberRange<Double> range() {
        double min = first;
        double max = first + increment * (length - 1);
        if (max < min) {
            min = max;
            max = first;
        }
        return NumberRange.create(min, true, max, true);
    }

    /**
     * Creates a new sequence.
     */
    @Override
    Vector createSubSampling(final int first, final int step, final int length) {
        return new SequenceVector(doubleValue(first), increment*step, length);
    }
}
