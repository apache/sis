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
import org.apache.sis.util.Classes;
import org.apache.sis.measure.NumberRange;


/**
 * A vector which is the concatenation of two other vectors.
 *
 * @author  Martin Desruisseaux (MPO, Geomatys)
 */
final class ConcatenatedVector extends Vector implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4639375525939012394L;

    /**
     * The vectors to concatenate.
     */
    @SuppressWarnings("serial")         // Most SIS implementations are serializable.
    private final Vector first, second;

    /**
     * The length of the first vector.
     */
    private final int limit;

    /**
     * Creates a concatenated vector.
     *
     * @param first   the vector for the lower indices.
     * @param second  the vector for the higher indices.
     */
    public ConcatenatedVector(final Vector first, final Vector second) {
        this.first  = first;
        this.second = second;
        this.limit  = first.size();
    }

    /**
     * Returns the parent type of the two vectors.
     */
    @Override
    public Class<? extends Number> getElementType() {
        return Classes.findCommonClass(first.getElementType(), second.getElementType()).asSubclass(Number.class);
    }

    /**
     * Returns {@code true} if this vector contains only integer values.
     */
    @Override
    public boolean isInteger() {
        return first.isInteger() && second.isInteger();
    }

    /**
     * Returns {@code true} only if both vectors are unsigned.
     */
    @Override
    public boolean isUnsigned() {
        return first.isUnsigned() && second.isUnsigned();
    }

    /**
     * Returns the length of this vector.
     */
    @Override
    public int size() {
        return limit + second.size();
    }

    /**
     * Returns {@code true} if this vector is empty or contains only {@code NaN} values.
     */
    @Override
    public boolean isEmptyOrNaN() {
        return first.isEmptyOrNaN() && second.isEmptyOrNaN();
    }

    /**
     * Returns {@code true} if the value at the given index is {@code NaN}.
     */
    @Override
    public boolean isNaN(int index) throws IndexOutOfBoundsException {
        final Vector v;
        if (index < limit) {
            v = first;
        } else {
            v = second;
            index -= limit;
        }
        return v.isNaN(index);
    }

    /**
     * Returns the value at the given index.
     */
    @Override
    public double doubleValue(int index) {
        final Vector v;
        if (index < limit) {
            v = first;
        } else {
            v = second;
            index -= limit;
        }
        return v.doubleValue(index);
    }

    /**
     * Returns the value at the given index.
     */
    @Override
    public float floatValue(int index) {
        final Vector v;
        if (index < limit) {
            v = first;
        } else {
            v = second;
            index -= limit;
        }
        return v.floatValue(index);
    }

    /**
     * Returns the value at the given index.
     */
    @Override
    public long longValue(int index) {
        final Vector v;
        if (index < limit) {
            v = first;
        } else {
            v = second;
            index -= limit;
        }
        return v.longValue(index);
    }

    /**
     * Returns the value at the given index.
     */
    @Override
    public int intValue(int index) {
        final Vector v;
        if (index < limit) {
            v = first;
        } else {
            v = second;
            index -= limit;
        }
        return v.intValue(index);
    }

    /**
     * Returns the value at the given index.
     */
    @Override
    public short shortValue(int index) {
        final Vector v;
        if (index < limit) {
            v = first;
        } else {
            v = second;
            index -= limit;
        }
        return v.shortValue(index);
    }

    /**
     * Returns the value at the given index.
     */
    @Override
    public byte byteValue(int index) {
        final Vector v;
        if (index < limit) {
            v = first;
        } else {
            v = second;
            index -= limit;
        }
        return v.byteValue(index);
    }

    /**
     * Returns the string representation at the given index.
     */
    @Override
    public String stringValue(int index) {
        final Vector v;
        if (index < limit) {
            v = first;
        } else {
            v = second;
            index -= limit;
        }
        return v.stringValue(index);
    }

    /**
     * Returns the value at the given index.
     *
     * @throws ArrayIndexOutOfBoundsException if the given index is out of bounds.
     */
    @Override
    public Number get(int index) {
        final Vector v;
        if (index < limit) {
            v = first;
        } else {
            v = second;
            index -= limit;
        }
        return v.get(index);
    }

    /**
     * Sets the value at the given index.
     */
    @Override
    public Number set(int index, final Number value) {
        final Vector v;
        if (index < limit) {
            v = first;
        } else {
            v = second;
            index -= limit;
        }
        final Number old = v.set(index, value);
        modCount++;
        return old;
    }

    /**
     * Sets a range of elements to the given number.
     */
    @Override
    public void fill(final int fromIndex, final int toIndex, final Number value) {
        if (fromIndex < limit) {
            first.fill(fromIndex, Math.min(toIndex, limit), value);
        }
        if ((toIndex > limit)) {
            second.fill(Math.max(0, fromIndex - limit), toIndex - limit, value);
        }
    }

    /**
     * Returns the increment between all consecutive values if this increment is constant, or {@code null} otherwise.
     */
    @Override
    public Number increment(final double tolerance) {
        Number inc = first.increment(tolerance);
        if (inc != null) {
            Number check = second.increment(tolerance);
            if (check != null) {
                NumberType type = NumberType.forNumberClasses(inc.getClass(), check.getClass());
                inc   = type.cast(inc);
                check = type.cast(check);
                if (inc.equals(check)) {
                    return inc;
                }
            }
        }
        return null;
    }

    /**
     * Computes the minimal and maximal values in this vector.
     * This is the union of the range of the two concatenated vectors.
     */
    @Override
    public NumberRange<?> range() {
        return first.range().unionAny(second.range());
    }

    /**
     * Delegates to the backing vectors if possible.
     */
    @Override
    Vector createSubSampling(final int first, final int step, final int length) {
        if (first >= limit) {
            return second.subSampling(first - limit, step, length);
        }
        if (first + step*length <= limit) {
            return this.first.subSampling(first, step, length);
        }
        return super.createSubSampling(first, step, length);
    }

    /**
     * Delegates to the backing vectors since there is a chance that they override
     * their {@code concatenate} method with a more efficient implementation.
     */
    @Override
    Vector createConcatenate(final Vector toAppend) {
        return first.concatenate(second.concatenate(toAppend));
    }

    /**
     * Delegates to the backing vectors since there is a chance that they override
     * their {@code transform} method with a more efficient implementation.
     */
    @Override
    Vector createTransform(final double scale, final double offset) {
        return first.transform(scale, offset).concatenate(second.transform(scale, offset));
    }
}
