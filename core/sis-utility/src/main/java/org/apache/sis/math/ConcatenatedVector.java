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
import org.apache.sis.util.Numbers;


/**
 * A vector which is the concatenation of two other vectors.
 *
 * @author  Martin Desruisseaux (MPO, Geomatys)
 * @since   0.8
 * @version 0.8
 * @module
 */
final class ConcatenatedVector extends Vector implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 4639375525939012394L;

    /**
     * The vectors to concatenate.
     */
    private final Vector first, second;

    /**
     * The length of the first vector.
     */
    private final int limit;

    /**
     * Creates a concatenated vector.
     *
     * @param first  the vector for the lower indices.
     * @param second the vector for the higher indices.
     */
    public ConcatenatedVector(final Vector first, final Vector second) {
        this.first  = first;
        this.second = second;
        this.limit  = first.size();
    }

    /**
     * Returns widest type of the two vectors.
     */
    @Override
    public Class<? extends Number> getElementType() {
        return Numbers.widestClass(first.getElementType(), second.getElementType());
    }

    /**
     * Returns the length of this vector.
     */
    @Override
    public int size() {
        return limit + second.size();
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
        return v.set(index, value);
    }

    /**
     * Delegates to the backing vectors if possible.
     */
    @Override
    Vector createSubList(final int first, final int step, final int length) {
        if (first >= limit) {
            return second.subList(first - limit, step, length);
        }
        if (first + step*length <= limit) {
            return this.first.subList(first, step, length);
        }
        return super.createSubList(first, step, length);
    }

    /**
     * Delegates to the backing vectors since there is a chance that they overloaded
     * their {@code concatenate} method with a more effective implementation.
     */
    @Override
    Vector createConcatenate(final Vector toAppend) {
        return first.concatenate(second.concatenate(toAppend));
    }
}
