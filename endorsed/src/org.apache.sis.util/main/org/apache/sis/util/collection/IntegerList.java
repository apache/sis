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
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.PrimitiveIterator;
import java.util.NoSuchElementException;
import java.util.ConcurrentModificationException;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.internal.shared.Numerics;


/**
 * A list of unsigned integer values. This class packs the values in the minimal number of bits
 * required for storing unsigned integers of the given {@linkplain #maximalValue() maximal value}.
 *
 * <p>This class is <strong>not</strong> thread-safe. Synchronizations (if wanted) are user's responsibility.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Alexis Manin (Geomatys)
 * @version 1.5
 *
 * @see org.apache.sis.math.Vector
 *
 * @since 0.7
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
     * The following relation must hold: {@code (1 << BASE_SHIFT) == VALUE_SIZE}.
     */
    private static final int BASE_SHIFT = Numerics.LONG_SHIFT;

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
     *
     * @see #size()
     */
    private int size;

    /**
     * Creates an initially empty list with the given initial capacity.
     *
     * @param initialCapacity  the initial capacity.
     * @param maximalValue     the maximal value to be allowed, inclusive.
     */
    public IntegerList(int initialCapacity, int maximalValue) {
        this(initialCapacity, maximalValue, false);
    }

    /**
     * Creates a new list with the given initial size.
     * The value of all elements are initialized to 0.
     *
     * @param initialCapacity  the initial capacity.
     * @param maximalValue     the maximal value to be allowed, inclusive.
     * @param fill if {@code true}, the initial {@linkplain #size() size} is set to the initial capacity
     *        with all values set to 0.
     */
    public IntegerList(final int initialCapacity, final int maximalValue, final boolean fill) {
        ArgumentChecks.ensureStrictlyPositive("initialCapacity", initialCapacity);
        ArgumentChecks.ensureStrictlyPositive("maximalValue",    maximalValue);
        bitCount = Math.max(1, Integer.SIZE - Integer.numberOfLeadingZeros(maximalValue));
        mask     = (1 << bitCount) - 1;
        values   = new long[length(initialCapacity)];
        if (fill) {
            size = initialCapacity;
        }
    }

    /**
     * Returns the array length required for holding a list of the given size.
     *
     * @param  capacity  the desired list size.
     * @return the array length for holding a list of the given size.
     */
    private int length(int capacity) {
        capacity *= bitCount;
        int length = capacity >>> BASE_SHIFT;
        if ((capacity & OFFSET_MASK) != 0) {
            length++;
        }
        return length;
    }

    /**
     * Returns the maximal value that can be stored in this list.
     * May be slightly higher than the value given to the constructor.
     *
     * @return the maximal value, inclusive.
     */
    public int maximalValue() {
        return mask;
    }

    /**
     * Returns the current number of values in this list.
     *
     * @return the number of values.
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
     * @param  size  the new size.
     *
     * @see #trimToSize()
     */
    public void resize(final int size) {
        ArgumentChecks.ensurePositive("size", size);
        modCount++;
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
     * Every existing values are overwritten from index 0 inclusive up to {@link #size()} exclusive.
     *
     * @param  value  the value to set.
     */
    @SuppressWarnings("fallthrough")
    public void fill(int value) {
        ArgumentChecks.ensureBetween("value", 0, mask, value);
        modCount++;
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
            default: {                          // General case (unoptimized)
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
        modCount++;
        size = 0;
    }

    /**
     * Adds the given element to this list.
     *
     * @param  value  the value to add.
     * @return always {@code true}.
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
     * @param  value  the value to add.
     * @throws IllegalArgumentException if the given value is out of bounds.
     *
     * @see #removeLast()
     */
    public void addInt(final int value) throws IllegalArgumentException {
        ArgumentChecks.ensureBetween("value", 0, mask, value);
        modCount++;
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
     * @param  index  the element index.
     * @return the value at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    @Override
    public Integer get(final int index) throws IndexOutOfBoundsException {
        return getInt(index);
    }

    /**
     * Gets the first element.
     *
     * @return the first element.
     * @throws NoSuchElementException if this collection is empty.
     * @since 1.5
     */
    public Integer getFirst() {
        if (size != 0) {
            return getUnchecked(0);
        }
        throw new NoSuchElementException();
    }

    /**
     * Gets the last element.
     *
     * @return the last element.
     * @throws NoSuchElementException if this collection is empty.
     * @since 1.5
     */
    public Integer getLast() {
        if (size != 0) {
            return getUnchecked(size - 1);
        }
        throw new NoSuchElementException();
    }

    /**
     * Returns the element at the given index as the {@code int} primitive type.
     *
     * @param  index  the element index.
     * @return the value at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    public int getInt(final int index) throws IndexOutOfBoundsException {
        return getUnchecked(Objects.checkIndex(index, size));
    }

    /**
     * Returns the element at the given index as the {@code int} primitive type.
     * This argument does not check argument validity, since the verification is
     * assumed already done.
     *
     * @param  index  the element index.
     * @return the value at the given index.
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
     * @param  index  the element index.
     * @param  value  the value at the given index.
     * @return the previous value at the given index.
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
     * @param  index  the element index.
     * @param  value  the value at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     * @throws IllegalArgumentException if the given value is out of bounds.
     */
    public void setInt(int index, int value) throws IndexOutOfBoundsException {
        Objects.checkIndex(index, size);
        ArgumentChecks.ensureBetween("value", 0, mask, value);
        modCount++;
        setUnchecked(index, value);
    }

    /**
     * Sets the element at the given index as the {@code int} primitive type.
     * This argument does not check argument validity, since the verification
     * is assumed already done.
     *
     * @param  index  the element index.
     * @param  value  the value at the given index.
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
     * @param  index  the index of the element to remove.
     * @return the previous value of the element at the given index.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    @Override
    public Integer remove(final int index) throws IndexOutOfBoundsException {
        final Integer old = get(index);
        modCount++;
        removeRange(index, index+1);
        return old;
    }

    /**
     * Retrieves and removes the first element of this list.
     *
     * @return the head of this list.
     * @throws NoSuchElementException if this list is empty.
     *
     * @since 1.5
     */
    public Integer removeFirst() throws NoSuchElementException {
        if (size != 0) {
            modCount++;
            return getUnchecked(0);
        }
        throw new NoSuchElementException();
    }

    /**
     * Retrieves and removes the last element of this list.
     *
     * @return the tail of this list.
     * @throws NoSuchElementException if this list is empty.
     */
    public Integer removeLast() throws NoSuchElementException {
        if (size != 0) {
            modCount++;
            return getUnchecked(--size);
        }
        throw new NoSuchElementException();
    }

    /**
     * Removes all values in the given range of index.
     * Shifts any succeeding elements to the left (reduces their index).
     *
     * @param  lower  index of the first element to remove, inclusive.
     * @param  upper  index after the last element to be removed.
     */
    @Override
    protected void removeRange(int lower, int upper) {
        Objects.checkFromToIndex(lower, upper, size);
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
             * removal don't have the same offset as the original values.
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
     * @param  value  the value to search for.
     * @return the number of time the given value occurs in this list.
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
     * Returns an iterator over the elements in this list in increasing index order.
     * The iterator is <i>fail-fast</i> and supports the remove operation.
     *
     * @return iterator over the integer values in this list.
     *
     * @since 1.0
     */
    @Override
    public PrimitiveIterator.OfInt iterator() {
        return new PrimitiveSpliterator();
    }

    /**
     * Returns an spliterator over the elements in this list in increasing index order.
     * The iterator is <i>fail-fast</i>.
     *
     * @return spliterator over the integer values in this list.
     *
     * @since 1.0
     */
    @Override
    public Spliterator.OfInt spliterator() {
        return new PrimitiveSpliterator();
    }

    /**
     * Returns a stream of integers with this {@code IntegerList} as its source.
     * This method is similar to {@link #stream()}, but does not box the values.
     * The returned stream is <i>fail-fast</i>, meaning that any modification to the list
     * while using the stream will cause a {@link ConcurrentModificationException} to be thrown.
     *
     * <p>The default implementation creates a parallel or sequential stream from {@link #spliterator()}.</p>
     *
     * @param parallel  {@code true} for a parallel stream, or {@code false} for a sequential stream.
     * @return a stream of values in this list as primitive types.
     *
     * @since 1.0
     */
    public IntStream stream(boolean parallel) {
        return StreamSupport.intStream(spliterator(), parallel);
    }

    /**
     * Same as {@link #spliterator()}, but without value boxing.
     * This spliterator provides a fail-fast way to traverse list content, which means
     * that any alteration to the list content causes a failure of the advance operation
     * with a {@link ConcurrentModificationException}.
     *
     * <p>This implementation opportunistically provides an iterator implementation on
     * integer values too, but only one of the {@code Iterator} or {@code Spliterator}
     * API should be used on a given instance.</p>
     */
    private final class PrimitiveSpliterator implements Spliterator.OfInt, PrimitiveIterator.OfInt {
        /**
         * Index after the last element returned by this spliterator. This is initially {@link IntegerList#size},
         * but may be set to a smaller value by call to {@link #trySplit()}.
         */
        private int stopAt;

        /**
         * Index of the next element to be returned.
         */
        private int nextIndex;

        /**
         * The {@link IntegerList#modCount} value as iterator construction time.
         * Used for detecting modification in the backing list during traversal.
         */
        private int expectedModCount;

        /**
         * Index of the last elements removed by a {@link #remove()} operation.
         * This is used for checking that {@code remove()} is not invoked twice
         * before the next advance.
         */
        private int lastRemove;

        /**
         * Creates a new iterator for the whole content of the backing list.
         */
        PrimitiveSpliterator() {
            expectedModCount = modCount;
            stopAt           = size;
        }

        /**
         * Creates the prefix spliterator in a call to {@link #trySplit()}.
         *
         * @param  suffix   the spliterator which will continue iteration after this spliterator.
         * @param  startAt  index of the first element to be returned by this prefix spliterator.
         */
        private PrimitiveSpliterator(final PrimitiveSpliterator suffix, final int startAt) {
            expectedModCount = suffix.expectedModCount;
            stopAt           = suffix.nextIndex;
            nextIndex        = startAt;
        }

        /**
         * Declares that this split iterator does not return null elements, that all elements are
         * traversed in a fixed order (which is increasing index values) and that {@link #size()}
         * represents an exact count of elements.
         */
        @Override
        public int characteristics() {
            return NONNULL | ORDERED | SIZED | SUBSIZED;
        }

        /**
         * Returns the exact number of values to be encountered by a {@code forEachRemaining(…)} traversal.
         */
        @Override
        public long estimateSize() {
            return stopAt - nextIndex;
        }

        /**
         * @todo for now, we keep it simple and forbid parallelism. In the future,
         *       we could use an approach as the one in java standard array lists.
         */
        @Override
        public Spliterator.OfInt trySplit() {
            final int startAt = nextIndex;
            final int halfSize = (stopAt - startAt) >>> 1;
            if (halfSize > 1) {
                nextIndex += halfSize;
                return new PrimitiveSpliterator(this, startAt);
            }
            return null;
        }

        /**
         * Returns {@code true} if there is one more value to return. This method
         * also ensures that no alteration has happened on the backing list since
         * the spliterator creation.
         */
        @Override
        public boolean hasNext() {
            if (modCount == expectedModCount) {
                return nextIndex < stopAt;
            } else {
                throw new ConcurrentModificationException();
            }
        }

        /**
         * Returns the next integer values in iterator order.
         */
        @Override
        public int nextInt() {
            if (hasNext()) {
                return getUnchecked(nextIndex++);
            } else {
                throw new NoSuchElementException();
            }
        }

        /**
         * If a remaining element exists, performs the given action on it and returns {@code true}.
         * Otherwise returns {@code false}.
         */
        @Override
        public boolean tryAdvance(IntConsumer action) {
            final boolean canAdvance = hasNext();
            if (canAdvance) {
                action.accept(getUnchecked(nextIndex++));
            }
            return canAdvance;
        }

        /**
         * Performs the given action on all remaining elements. This implementation
         * is shared by both {@code Iterator} and {@code Spliterator} interfaces.
         */
        @Override
        public void forEachRemaining(final IntConsumer action) {
            while (hasNext()) {
                action.accept(getUnchecked(nextIndex++));
            }
        }

        /**
         * Performs the given action on all remaining elements. This implementation
         * is shared by both {@code Iterator} and {@code Spliterator} interfaces.
         */
        @Override
        public void forEachRemaining(final Consumer<? super Integer> action) {
            if (action instanceof IntConsumer) {
                forEachRemaining((IntConsumer) action);
            } else while (hasNext()) {
                action.accept(getUnchecked(nextIndex++));
            }
        }

        /**
         * Removes the last element returned by {@link #nextInt()}.
         */
        @Override
        public void remove() {
            if (nextIndex < lastRemove || nextIndex > stopAt) {
                throw new IllegalStateException();
            }
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            expectedModCount = ++modCount;
            removeRange(nextIndex - 1, nextIndex);
            lastRemove = --nextIndex;
            stopAt--;
        }
    }

    /**
     * Invokes {@link #trimToSize()} before serialization in order to make the stream more compact.
     *
     * @param  out  the output stream where to serialize this list.
     * @throws IOException if an I/O error occurred while writing.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        trimToSize();
        out.defaultWriteObject();
    }

    /**
     * Trims the capacity of this list to be its current size.
     *
     * @see #size()
     * @see #resize(int)
     */
    public void trimToSize() {
        values = ArraysExt.resize(values, length(size));
    }

    /**
     * Compares the content of this list with the given object. This method overrides the
     * {@link AbstractList#equals(Object) default implementation} for performance reasons.
     *
     * @param  other  the other object to compare with this list.
     * @return {@code true} if both object are equal.
     *
     * @since 1.1
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (other != null && other.getClass() == getClass()) {
            final IntegerList that = (IntegerList) other;
            if (that.size != size) {
                return false;
            }
            if (that.bitCount == bitCount) {
                int n = size * bitCount;
                final int nr = n & OFFSET_MASK;             // Number of remaining values.
                n >>>= BASE_SHIFT;
                if (!Arrays.equals(values, 0, n, that.values, 0, n)) {
                    return false;
                }
                if (nr == 0) return true;
                return ((that.values[n] ^ values[n]) & ((1L << nr) - 1)) == 0;
            }
        }
        return super.equals(other);
    }

    /**
     * Returns a clone of this list.
     *
     * @return a clone of this list.
     */
    @Override
    public IntegerList clone() {
        final IntegerList clone;
        try {
            clone = (IntegerList) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
        clone.values = Arrays.copyOf(values, length(size));
        clone.modCount = 0;
        return clone;
    }
}
