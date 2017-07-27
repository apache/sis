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

import org.apache.sis.util.Numbers;
import org.apache.sis.util.collection.IntegerList;
import org.apache.sis.util.resources.Errors;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.JDK8;


/**
 * A vector of integer values backed by an {@link IntegerList}.
 * This offers a compressed storage using only the minimal amount of bits per value.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.8
 * @module
 */
final class PackedVector extends ArrayVector<Long> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5097586732924434042L;

    /**
     * Minimal length for creating a packed vector.
     * This is an arbitrary value that may change in any future version.
     */
    static final int MINIMAL_SIZE = 8;

    /**
     * The compressed list of integer values. This list can store values from 0 to {@code delta} inclusive.
     */
    private final IntegerList data;

    /**
     * The value by which to multiply the {@linkplain #data} before to add the {@linkplain #offset}.
     */
    private final long increment;

    /**
     * The offset to add to the {@link #data} in order to get the values to return.
     */
    private final long offset;

    /**
     * Creates a new compressed vector initialized to a copy of the data provided by the given vector.
     *
     * @param  source     the vector to copy.
     * @param  increment  the common divisor of all (sample minus offset) values.
     * @param  offset     the minimal value in the source vector.
     * @param  delta      the maximal value in the source vector minus {@code offset} divided by {@code increment}.
     */
    private PackedVector(final Vector source, final long increment, final long offset, final int delta) {
        this.increment = increment;
        this.offset    = offset;
        final int length = source.size();
        data = new IntegerList(length, delta, true);
        for (int i=0; i<length; i++) {
            data.setInt(i, JDK8.toIntExact((source.longValue(i) - offset) / increment));
        }
    }

    /**
     * Creates a new compressed vector initialized to a copy of the data provided by the given vector.
     * All values in the given vector shall be assignable to the {@code long} type (this is not verified).
     *
     * @param  source  the vector to copy.
     * @param  min     the minimal value in the given vector, inclusive.
     * @param  max     the maximal value in the given vector, inclusive.
     * @return the compressed vector, or {@code null} if the vector can not or should not be compressed.
     */
    static PackedVector compress(final Vector source, final long min, final long max) {
        long delta = max - min;
        if (delta > 0) {                                    // Negative if (max - min) overflow.
            long inc = delta;
            final int length = source.size();
            if (length >= MINIMAL_SIZE) {
                for (int i=0; i<length; i++) {
                    long t = source.longValue(i) - min;
                    if (t < 0) return null;                 // May happen if the given 'min' value is wrong.
                    if ((t % inc) != 0) {
                        do {
                            final long r = (inc % t);       // Search for greatest common divisor with Euclid's algorithm.
                            inc = t;
                            t = r;
                        } while (t != 0);
                        if (inc == 1) {
                            /*
                             * If we reach this point, the increment is of no use for compressing data.
                             * If in addition the minimal number of bits required for storing all values:
                             *
                             *     numBits = Long.SIZE - Long.numberOfLeadingZeros(delta);
                             *
                             * is equal to the number of bits of a primitive type (byte, short, int or long)
                             * and all values are in the range of that primitive type, then there is nothing
                             * to win compared to an array of that primitive type.
                             */
                            final long high = Long.highestOneBit(delta);
                            if ((high & ((1L << (Byte   .SIZE - 1)) |
                                         (1L << (Short  .SIZE - 1)) |
                                         (1L << (Integer.SIZE - 1)) |
                                         (1L << (Long   .SIZE - 1)))) != 0)
                            {
                                long limit = high - 1;                  // Maximal value of signed integers.
                                if (min >= 0) {
                                    limit |= high;                      // Maximal value of unsigned integers.
                                    if (limit < 0) return null;         // Overflow the 'long' primitve type.
                                }
                                if (max <= limit && min >= ~limit) {    // Really tild (~), not minus (-).
                                    return null;                        // All values in range of primitive type.
                                }
                            }
                            break;                                      // No need to check other values.
                        }
                    }
                }
                delta /= inc;
                if (delta > Integer.MAX_VALUE) return null;
                return new PackedVector(source, inc, min, (int) delta);
            }
        }
        return null;
    }

    /**
     * Type of elements fixed to {@code Long} even if the actual storage used by this class is more compact.
     * The reason for the {@code Long} type is that this class can return any value in the {@code Long} range,
     * because of the {@link #offset}.
     */
    @Override
    public Class<Long> getElementType() {
        return Long.class;
    }

    /**
     * Returns the number of elements in this vector.
     */
    @Override
    public int size() {
        return data.size();
    }

    /**
     * Returns the value at the given index as a {@code double} primitive type.
     */
    @Override
    public double doubleValue(final int index) {
        return longValue(index);
    }

    /**
     * Returns the value at the given index as a {@code float} primitive type.
     */
    @Override
    public float floatValue(final int index) {
        return longValue(index);
    }

    /**
     * Returns the value at the given index as a {@code long} primitive type.
     */
    @Override
    public long longValue(final int index) {
        return data.getInt(index) * increment + offset;
    }

    /**
     * Returns the string representation of the value at the given index.
     */
    @Override
    public String stringValue(final int index) {
        return Long.toString(longValue(index));
    }

    /**
     * Returns the value at the given index wrapped in a {@link Long} instance.
     */
    @Override
    public Number get(final int index) {
        return longValue(index);
    }

    /**
     * Sets the value at the given index at returns the previous value.
     */
    @Override
    public Number set(final int index, final Number value) {
        verifyType(value.getClass(), Numbers.LONG);
        long v = value.longValue();
        if (v >= offset) {
            v -= offset;
            if ((v % increment) == 0) {
                v /= increment;
                if (v <= data.maximalValue()) {
                    final Number old = get(index);
                    data.setInt(index, (int) v);
                    modCount++;
                    return old;
                }
            }
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.CanNotStoreInVector_1, value));
    }
}
