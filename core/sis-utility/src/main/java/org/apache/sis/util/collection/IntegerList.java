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
package org.apache.sis.util.collection;

import java.util.Arrays;
import java.util.AbstractList;
import java.util.RandomAccess;
import java.util.NoSuchElementException;
import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;


/**
 * A list of unsigned integer values. This class packs the values in the minimal amount of bits
 * required for storing unsigned integers of the given {@linkplain #maximalValue() maximal value}.
 *
 * <p>This class is <strong>not</strong> thread-safe. Synchronizations (if wanted) are user's responsibility.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public class IntegerList extends AbstractList<Integer> implements RandomAccess, Serializable, Cloneable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 1241962316404811189L;

    /**
     * The size of the primitive type used for the {@link #values} array.
     */
    private static final int VALUE_SIZE = Long.SIZE;

    /**
     * The shift to apply on {@code index} in order to produce a result equivalent to {@code index} / {@value #VALUE_SIZE}.
     * The following relation must hold: {@code (1 <<< BASE_SHIFT) == VALUE_SIZE}.
     */
    private static final int BASE_SHIFT = 6;

    /**
     * The mask to apply on {@code index} in order to produce a result equivalent to {@code index} % {@value #VALUE_SIZE}.
     */
    private static final int OFFSET_MASK = VALUE_SIZE - 1;

    /**
     * The packed values. We use the {@code long} type instead of {@code int} since 64 bits machines are common now.
     */
    private long[] values;

    /**
     * The bit count for values.
     */
    private final int bitCount;

    /**
     * The mask computed as {@code (1 << bitCount) - 1}.
     */
    private final int mask;

    /**
     * The list size. Initially 0.
     */
    private int size;

    /**
     * Creates an initially empty list with the given initial capacity.
     *
     * @param initialCapacity The initial capacity.
     * @param maximalValue The maximal value to be allowed, inclusive.
     */
    public IntegerList(int initialCapacity, int maximalValue) {
        this(initialCapacity, maximalValue, false);
    }

    /**
     * Creates a new list with the given initial size.
     * The value of all elements are initialized to 0.
     *
     * @param initialCapacity The initial capacity.
     * @param maximalValue The maximal value to be allowed, inclusive.
     * @param fill If {@code true}, the initial {@linkplain #size() size} is set to the initial capacity
     *        with all values set to 0.
     */
    public IntegerList(final int initialCapacity, int maximalValue, final boolean fill) {
        ArgumentChecks.ensureStrictlyPositive("initialCapacity", initialCapacity);
        ArgumentChecks.ensureStrictlyPositive("maximalValue",    maximalValue);
        int bitCount = 0;
        do {
            bitCount++;
            maximalValue >>>= 1;
        } while (maximalValue != 0);
        this.bitCount = bitCount;
        mask = (1 << bitCount) - 1;
        values = new long[length(initialCapacity)];
        if (fill) {
            size = initialCapacity;
        }
    }

    /**
     * Returns the array length required for holding a list of the given size.
     *
     * @param size The list size.
     * @return The array length for holding a list of the given size.
     */
    private int length(int size) {
        size *= bitCount;
        int length = size >>> BASE_SHIFT;
        if ((size & OFFSET_MASK) != 0) {
            length++;
        }
        return length;
    }

    /**
     * Returns the maximal value that can be stored in this list.
     * May be slightly higher than the value given to the constructor.
     *
     * @return The maximal value, inclusive.
     */
    public int maximalValue() {
        return mask;
    }

    /**
     * Returns the current number of values in this list.
     *
     * @return The number of values.
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Sets the list size to the given value. If the new size is lower than previous size,
     * then the elements after the new size are discarded. If the new size is greater than
     * the previous one, then the extra elements are initialized to 0.
     *
     * @param size The new size.
     */
    public void resize(final int size) {
        ArgumentChecks.ensurePositive("size", size);
        if (size > this.size) {
            int base = this.size * bitCount;
            final int offset = base & OFFSET_MASK;
            base >>>= BASE_SHIFT;
            if (offset != 0 && base < values.length) {
                values[base] &= (1L << offset) - 1;
                base++;
            }
            final int length = length(size);
            Arrays.fill(values, base, Math.min(length, values.length), 0L);
            if (length > values.length) {
                values = Arrays.copyOf(values, length);
            }
        }
        this.size = size;
    }

    /**
     * Fills the list with the given value.
     * Every existing values are overwritten from index 0 inclusive up to {@link #size} exclusive.
     *
     * @param value The value to set.
     */
    @SuppressWarnings("fallthrough")
    public void fill(int value) {
        ArgumentChecks.ensureBetween("value", 0, mask, value);
        final long p;
        if (value == 0) {
            p = 0;                              // All bits set to 0.
        } else if (value == mask) {
            p = -1L;                            // All bits set to 1.
        } else switch (bitCount) {
            case  1: value |= (value << 1);     // Fall through
            case  2: value |= (value << 2);     // Fall through
            case  4: value |= (value << 4);     // Fall through
            case  8: value |= (value << 8);     // Fall through
            case 16: value |= (value << 16);    // Fall through
            case 32: p = (value & 0xFFFFFFFFL) | ((long) value << 32); break;
            default: {    // General case (unoptimized)
                for (int i=0; i<size; i++) {
                    setUnchecked(i, value);
                }
                return;
            }
        }
        Arrays.fill(values, 0, length(size), p);
    }

    /**
     * Discards all elements in this list.
     */
    @Override
    public void clear() {
        size = 0;
    }

    /**
     * Adds the given element to this list.
     *
     * @param  value The value to add.
     * @return Always {@code true}.
     * @throws NullPointerException if the given value is null.
     * @throws IllegalArgumentException if the given value is out of bounds.
     */
    @Override
    public boolean add(final Integer value) throws IllegalArgumentException {
        addInt(value);
        return true;
    }

    /**
     * Adds the given element as the {@code int} primitive type.
     *
     * @param  value The value to add.
     * @throws IllegalArgumentException if the given value is out of bounds.
     *
     * @see #removeLast()
     */
    public void addInt(final int value) throws IllegalArgumentException {
        ArgumentChecks.ensureBetween("value", 0, mask, value);
        final int last = size;
        final int length = length(++size);
        if (length > values.length) {
            values = Arrays.copyOf(values, 2*values.length);
        }
        setUnchecked(last, value);
    }

    /**
     * Returns the element at the given index.
     *
     * @param  index The element index.
     * @return The value at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    @Override
    public Integer get(final int index) throws IndexOutOfBoundsException {
        return getInt(index);
    }

    /**
     * Returns the element at the given index as the {@code int} primitive type.
     *
     * @param  index The element index.
     * @return The value at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    public int getInt(final int index) throws IndexOutOfBoundsException {
        ArgumentChecks.ensureValidIndex(size, index);
        return getUnchecked(index);
    }

    /**
     * Returns the element at the given index as the {@code int} primitive type.
     * This argument does not check argument validity, since it is assumed already done.
     *
     * @param  index The element index.
     * @return The value at the given index.
     */
    private int getUnchecked(int index) {
        index *= bitCount;
        int base   = index >>> BASE_SHIFT;
        int offset = index & OFFSET_MASK;
        int value  = (int) (values[base] >>> offset);
        offset = VALUE_SIZE - offset;
        if (offset < bitCount) {
            final int high = (int) values[++base];
            value |= (high << offset);
        }
        value &= mask;
        return value;
    }

    /**
     * Sets the element at the given index.
     *
     * @param  index The element index.
     * @param  value The value at the given index.
     * @return The previous value at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws IllegalArgumentException if the given value is out of bounds.
     * @throws NullPointerException if the given value is null.
     */
    @Override
    public Integer set(final int index, final Integer value) throws IndexOutOfBoundsException {
        final Integer old = get(index);
        setInt(index, value);
        return old;
    }

    /**
     * Sets the element at the given index as the {@code int} primitive type.
     *
     * @param  index The element index.
     * @param  value The value at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws IllegalArgumentException if the given value is out of bounds.
     */
    public void setInt(int index, int value) throws IndexOutOfBoundsException {
        ArgumentChecks.ensureValidIndex(size, index);
        ArgumentChecks.ensureBetween("value", 0, mask, value);
        setUnchecked(index, value);
    }

    /**
     * Sets the element at the given index as the {@code int} primitive type.
     * This argument does not check argument validity, since it is assumed already done.
     *
     * @param index The element index.
     * @param value The value at the given index.
     */
    private void setUnchecked(int index, int value) {
        index *= bitCount;
        int base   = index >>> BASE_SHIFT;
        int offset = index & OFFSET_MASK;
        values[base] &= ~(((long) mask) << offset);
        values[base] |= ((long) value) << offset;
        offset = VALUE_SIZE - offset;
        if (offset < bitCount) {
            value >>>= offset;
            values[++base] &= ~(((long) mask) >>> offset);
            values[base] |= value;
        }
    }

    /**
     * Removes the element at the given index.
     *
     * @param  index The index of the element to remove.
     * @return The previous value of the element at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    @Override
    public Integer remove(final int index) throws IndexOutOfBoundsException {
        final Integer old = get(index);
        removeRange(index, index+1);
        return old;
    }

    /**
     * Retrieves and remove the last element of this list.
     *
     * @return The tail of this list.
     * @throws NoSuchElementException if this list is empty.
     */
    public int removeLast() throws NoSuchElementException {
        if (size != 0) {
            return getUnchecked(--size);
        }
        throw new NoSuchElementException();
    }

    /**
     * Removes all values in the given range of index.
     * Shifts any succeeding elements to the left (reduces their index).
     *
     * @param lower Index of the first element to remove, inclusive.
     * @param upper Index after the last element to be removed.
     */
    @Override
    protected void removeRange(int lower, int upper) {
        ArgumentChecks.ensureValidIndexRange(size, lower, upper);
        int lo = lower * bitCount;
        int hi = upper * bitCount;
        final int offset = (lo & OFFSET_MASK);
        if (offset == (hi & OFFSET_MASK)) {
            /*
             * Optimisation for a special case which can be handled by a call
             * to System.arracopy, which is much faster than our loop.
             */
            lo >>>= BASE_SHIFT;
            hi >>>= BASE_SHIFT;
            final long mask = (1L << offset) - 1;
            final long save = values[lo] & mask;
            System.arraycopy(values, hi, values, lo, length(size) - hi);
            values[lo] = (values[lo] & ~mask) | save;
        } else {
            /*
             * The general case, when the packed values after the range
             * removal don't have the same offset than the original values.
             */
            while (upper < size) {
                setUnchecked(lower++, getUnchecked(upper++));
            }
        }
        this.size -= (upper - lower);
    }

    /**
     * Returns the occurrence of the given value in this list.
     *
     * @param  value The value to search for.
     * @return The number of time the given value occurs in this list.
     */
    public int occurrence(final int value) {
        int count = 0;
        final int size = this.size;
        for (int i=0; i<size; i++) {
            if (getUnchecked(i) == value) {
                count++;
            }
        }
        return count;
    }

    /**
     * Trims the capacity of this list to be its current size.
     */
    public void trimToSize() {
        values = ArraysExt.resize(values, length(size));
    }

    /**
     * Returns a clone of this list.
     *
     * @return A clone of this list.
     */
    @Override
    public IntegerList clone() {
        final IntegerList clone;
        try {
            clone = (IntegerList) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
        clone.values = clone.values.clone();
        return clone;
    }

    /**
     * Invokes {@link #trimToSize()} before serialization in order to make the stream more compact.
     *
     * @param  out The output stream where to serialize this list.
     * @throws IOException If an I/O error occurred while writing.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        trimToSize();
        out.defaultWriteObject();
    }
}
