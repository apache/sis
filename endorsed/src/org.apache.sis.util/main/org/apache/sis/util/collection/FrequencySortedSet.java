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

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.SortedSet;
import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Arrays;
import java.lang.reflect.Array;
import java.io.Serializable;
import org.apache.sis.util.ArgumentChecks;


/**
 * A set with elements ordered by the amount of time they were added.
 * By default, less frequently added elements are first and most frequently added elements are last.
 * If some elements were added the same amount of time, then the iterator will traverse them in their
 * insertion order.
 *
 * <p>An optional Boolean argument in the constructor allows the construction of set in reversed order
 * (most frequently added elements first, less frequently added last). This is similar but not identical
 * to creating a default {@code FrequencySortedSet} and iterating through it in reverse order.
 * The difference is that elements added the same amount of time will still be traversed in their insertion order.</p>
 *
 * <p>This class is <strong>not</strong> thread-safe.
 * Synchronizations (if wanted) are caller responsibility.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 *
 * @param <E>  the type of elements in the set.
 *
 * @since 0.8
 */
public class FrequencySortedSet<E> extends AbstractSet<E> implements SortedSet<E>, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6034102231354388179L;

    /**
     * The frequency of occurrence for each element. We must use a linked hash map instead of an ordinary
     * hash map because we want to preserve insertion order for elements that occur at the same frequency.
     * Values are positives if this set sorts by increasing frequencies, or negatives if this set sorts by
     * decreasing frequencies.
     */
    private final LinkedHashMap<E,Integer> count;

    /**
     * {@code 0} if the element should be sorted in the usual order, or {@code -1}
     * if the elements should be sorted in reverse order (most frequent element first).
     * This value is XORed with the number of times <var>n</var> that an element is added: {@code n ^ order}.
     * The intent is to store negative numbers in the {@link #count} map if this {@code FrequencySortedSet}
     * has been created for reverse order.
     *
     * <h4>Implementation note</h4>
     * We could have used {@code +1} and {@code -1} for the usual and reverse order respectively, and store the
     * multiplication result {@code n * order} in the {@link #count} map. We rather use XOR for two reasons:
     * first, XOR is a simpler operation for the CPU than multiplication. Second, XOR guarantees us that all
     * negative numbers can be made positive in {@link #frequencies()}, by applying again {@code n ^ order}.
     * By contrast, the multiplication approach (or just the {@code -n} negation) would fail to convert
     * {@link Integer#MIN_VALUE}.
     */
    private final int order;

    /**
     * Elements in sorted order, or {@code null} if not yet computed.
     */
    private transient E[] sorted;

    /**
     * The frequency for each {@linkplain #sorted} element.
     * This array is invalid if {@link #sorted} is null.
     */
    private transient int[] frequencies;

    /**
     * Creates an initially empty set with less frequent elements first.
     */
    public FrequencySortedSet() {
        count = new LinkedHashMap<>();      // Default constructor in JDK implementation applies lazy array allocation.
        order = 0;
    }

    /**
     * Creates an initially empty set with the default initial capacity.
     *
     * @param  reversed  {@code true} if the elements should be sorted in reverse order
     *                   (most frequent element first, less frequent element last).
     */
    public FrequencySortedSet(final boolean reversed) {
        count = new LinkedHashMap<>();
        order = reversed ? -1 : 0;
    }

    /**
     * Creates an initially empty set with the specified initial capacity.
     *
     * @param initialCapacity  the initial capacity.
     * @param reversed         {@code true} if the elements should be sorted in reverse order
     *                         (most frequent element first, less frequent element last).
     */
    public FrequencySortedSet(final int initialCapacity, final boolean reversed) {
        count = new LinkedHashMap<>(initialCapacity);
        order = reversed ? -1 : 0;
    }

    /**
     * Returns the number of elements in this set.
     */
    @Override
    public int size() {
        return count.size();
    }

    /**
     * Returns {@code true} if this set is empty.
     */
    @Override
    public boolean isEmpty() {
        return count.isEmpty();
    }

    /**
     * Repetitively adds the specified element to this set. Returns {@code true} if this set changed
     * as a result of this operation. Changes in element order are not notified by the returned value.
     *
     * @param  element     the element to add (may be {@code null}).
     * @param  occurrence  the number of time to add the given element. The default value is 1.
     * @return {@code true} if this set changed as a result of this operation.
     * @throws IllegalArgumentException if {@code occurrence} is negative.
     */
    public boolean add(final E element, int occurrence) throws IllegalArgumentException {
        if (occurrence != 0) {
            ArgumentChecks.ensurePositive("occurrence", occurrence);
            sorted = null;
            occurrence ^= order;
            return count.merge(element, occurrence, (old, n) -> Math.addExact(old, n) - order) == occurrence;
            // Note: the subtraction by `order` cannot overflow.
        }
        return false;
    }

    /**
     * Adds the specified element to this set. Returns {@code true} if this set changed as a result
     * of this operation. Changes in element order are not notified by the returned value.
     *
     * <p>The default implementation delegates to <code>{@linkplain #add(Object, int) add}(element, 1)</code>.</p>
     *
     * @param  element  the element to add (may be {@code null}).
     * @return {@code true} if this set changed as a result of this operation.
     */
    @Override
    public boolean add(final E element) {
        return add(element, 1);
    }

    /**
     * Returns {@code true} if this set contains the specified element.
     *
     * @param  element  the element whose presence in this set is to be tested.
     * @return {@code true} if this set contains the specified element.
     */
    @Override
    @SuppressWarnings("element-type-mismatch")
    public boolean contains(final Object element) {
        return count.containsKey(element);
    }

    /**
     * Removes the specified element from this set, no matter how many time it has been added.
     * Returns {@code true} if this set changed as a result of this operation.
     *
     * @param  element  the element to remove.
     * @return {@code true} if this set changed as a result of this operation.
     */
    @Override
    @SuppressWarnings("element-type-mismatch")
    public boolean remove(final Object element) {
        if (count.remove(element) != null) {
            sorted = null;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Removes all elements from this set.
     */
    @Override
    public void clear() {
        frequencies = null;
        sorted = null;
        count.clear();
    }

    /**
     * Returns an iterator over the elements in this set in frequency order.
     */
    @Override
    public Iterator<E> iterator() {
        ensureSorted();
        return new Iter(sorted, 0, sorted.length);
    }

    /**
     * Iterator over sorted elements.
     */
    private final class Iter implements Iterator<E> {
        /**
         * A copy of {@link FrequencySortedSet#sorted} at the time this iterator has been created.
         * Used because the {@code sorted} array is set to {@code null} when {@link #remove()} is invoked.
         */
        private final E[] elements;

        /**
         * Index of the first element ({@code lower}) and index after the last element ({@code upper})
         * on which to iterate.
         */
        private final int lower, upper;

        /**
         * Index of the next element to return.
         */
        private int index;

        /**
         * Creates an new iterator.
         */
        Iter(final E[] sorted, final int lower, final int upper) {
            elements = sorted;
            this.index = lower;
            this.lower = lower;
            this.upper = upper;
        }

        /**
         * Returns {@code true} if there is more elements to return.
         */
        @Override
        public boolean hasNext() {
            return index < upper;
        }

        /**
         * Return the next element.
         */
        @Override
        public E next() {
            if (index < upper) {
                return elements[index++];
            }
            throw new NoSuchElementException();
        }

        /**
         * Remove the last elements returned by {@link #next}.
         */
        @Override
        public void remove() {
            if (index == lower || !FrequencySortedSet.this.remove(elements[index - 1])) {
                // Could also be ConcurrentModificationException - we do not differentiate.
                throw new IllegalStateException();
            }
        }
    }

    /**
     * A view over a subset of {@link FrequencySortedSet}.
     */
    private final class SubSet extends AbstractSet<E> implements SortedSet<E>, Serializable {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 6843072153603161179L;

        /**
         * Reference to the {@link FrequencySortedSet#sorted} array, used for detecting changes.
         */
        private transient E[] elements;

        /**
         * Low endpoint (inclusive) of the subset. May be {@code null}.
         */
        @SuppressWarnings("serial")         // Not statically typed as Serializable.
        private final E fromElement;

        /**
         * High endpoint (exclusive) of the subset. May be {@code null}.
         */
        @SuppressWarnings("serial")         // Not statically typed as Serializable.
        private final E toElement;

        /**
         * Whether the set should take in account {@link #fromElement} or {@link #toElement}.
         * We have to use those booleans (we cannot use {@code null} sentinel value instead)
         * because {@code null} is a legal value for {@code from/toElement}.
         */
        private final boolean hasFrom, hasTo;

        /**
         * Lower and upper index computed from {@link #fromElement} and {@link #toElement}.
         */
        private transient int lower, upper;

        /**
         * Creates a new subset from the lower element (inclusive) to the upper element (exclusive).
         * Each endpoint can be null.
         */
        SubSet(final boolean hasFrom, final E fromElement, final boolean hasTo, final E toElement) {
            this.fromElement = fromElement;
            this.toElement   = toElement;
            this.hasFrom     = hasFrom;
            this.hasTo       = hasTo;
        }

        /**
         * Returns the comparator, which is the same as {@link FrequencySortedSet#comparator()}.
         */
        @Override
        public Comparator<E> comparator() {
            return FrequencySortedSet.this.comparator();
        }

        /**
         * Ensures that {@link #lower} and {@link #upper} indices are valid.
         */
        private void ensureValidRange() {
            if (elements == null || elements != sorted) {
                ensureSorted();
                elements = sorted;
                if (hasFrom) {
                    lower = Arrays.binarySearch(elements, fromElement, comparator());
                    if (lower < 0) lower = ~lower;
                }
                if (hasTo) {
                    upper = Arrays.binarySearch(elements, toElement, comparator());
                    if (upper < 0)     upper = ~upper;
                    if (upper < lower) upper =  lower;
                } else {
                    upper = elements.length;
                }
            }
        }

        /**
         * Returns an iterator over the elements in this subset.
         */
        @Override
        public Iterator<E> iterator() {
            ensureValidRange();
            return new Iter(elements, lower, upper);
        }

        /**
         * Returns the number of elements in this subset.
         */
        @Override
        public int size() {
            ensureValidRange();
            return upper - lower;
        }

        /**
         * Returns the first element in this subset.
         *
         * @see FrequencySortedSet#first()
         */
        @Override
        public E first() {
            ensureValidRange();
            if (lower != upper) {
                return elements[lower];
            } else {
                throw new NoSuchElementException();
            }
        }

        /**
         * Returns the last element in this subset.
         *
         * @see FrequencySortedSet#last()
         */
        @Override
        public E last() {
            ensureValidRange();
            if (lower != upper) {
                return elements[upper - 1];
            } else {
                throw new NoSuchElementException();
            }
        }

        /**
         * Returns a view of the portion of this subset whose elements occur
         * with a frequency strictly less than {@code toElement} frequency.
         *
         * @see FrequencySortedSet#headSet(Object)
         */
        @Override
        public SortedSet<E> headSet(final E to) {
            return subSet(fromElement, to, hasFrom ? 0 : 2);
        }

        /**
         * Returns a view of the portion of this subset whose elements occur with
         * a frequency equal or greater than {@code fromElement} frequency.
         *
         * @see FrequencySortedSet#tailSet(Object)
         */
        @Override
        public SortedSet<E> tailSet(final E from) {
            return subSet(from, toElement, hasTo ? 0 : 1);
        }

        /**
         * Returns a view of the portion of this subset whose elements occur with a frequency in the
         * range of {@code fromElement} frequency inclusive to {@code toElement} frequency exclusive.
         *
         * @see FrequencySortedSet#subSet(Object, Object)
         */
        @Override
        public SortedSet<E> subSet(final E from, final E to) {
            return subSet(from, to, 0);
        }

        /**
         * Implementation of {@link #headSet(Object)}, {@link #tailSet(Object)} and {@link #subSet(Object, Object)}.
         * The {@code bounds} argument tell which {@link FrequencySortedSet} method to delegate to.
         */
        private SortedSet<E> subSet(E from, E to, final int bounded) {
            if (hasFrom && compare(from, fromElement) < 0) from = fromElement;
            if (hasTo   && compare(to,     toElement) > 0) to   = toElement;
            switch (bounded) {
                default: throw new AssertionError(bounded);
                case 0:  return FrequencySortedSet.this.subSet(from, to);
                case 1:  return FrequencySortedSet.this.tailSet(from);
                case 2:  return FrequencySortedSet.this.headSet(to);
            }
        }
    }

    /**
     * Returns a view of the portion of this set whose elements occur with a frequency strictly less than
     * {@code toElement} frequency.
     *
     * @param  toElement  high endpoint (exclusive) of the returned set. May be {@code null}.
     * @return a view of the portion of this set delimited by the given endpoint.
     */
    @Override
    public SortedSet<E> headSet(final E toElement) {
        return new SubSet(false, null, true, toElement);
    }

    /**
     * Returns a view of the portion of this set whose elements occur with a frequency equal or greater than
     * {@code fromElement} frequency.
     *
     * @param  fromElement  low endpoint (inclusive) of the returned set. May be {@code null}.
     * @return a view of the portion of this set delimited by the given endpoint.
     */
    @Override
    public SortedSet<E> tailSet(final E fromElement) {
        return new SubSet(true, fromElement, false, null);
    }

    /**
     * Returns a view of the portion of this set whose elements occur with a frequency in the range of
     * {@code fromElement} frequency inclusive to {@code toElement} frequency exclusive.
     *
     * @param  fromElement  low endpoint (inclusive) of the returned set. May be {@code null}.
     * @param  toElement    high endpoint (exclusive) of the returned set. May be {@code null}.
     * @return a view of the portion of this set delimited by the given endpoints.
     */
    @Override
    public SortedSet<E> subSet(final E fromElement, final E toElement) {
        return new SubSet(true, fromElement, true, toElement);
    }

    /**
     * Returns the first element in this set.
     *
     * <ul>
     *   <li>For sets created with the default order, this is the less frequently added element.
     *       If more than one element were added with the same frequency, this is the first one
     *       that has been {@linkplain #add added} to this set at this frequency.</li>
     *   <li>For sets created with the reverse order, this is the most frequently added element.
     *       If more than one element were added with the same frequency, this is the first one
     *       that has been {@linkplain #add added} to this set at this frequency.</li>
     * </ul>
     *
     * @throws NoSuchElementException if this set is empty.
     */
    @Override
    public E first() throws NoSuchElementException {
        ensureSorted();
        if (sorted.length != 0) {
            return sorted[0];
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * Returns the last element in this set.
     *
     * <ul>
     *   <li>For sets created with the default order, this is the most frequently added element.
     *       If more than one element were added with the same frequency, this is the last one
     *       that has been {@linkplain #add added} to this set at this frequency.</li>
     *   <li>For sets created with the reverse order, this is the less frequently added element.
     *       If more than one element were added with the same frequency, this is the last one
     *       that has been {@linkplain #add added} to this set at this frequency.</li>
     * </ul>
     *
     * @throws NoSuchElementException if this set is empty.
     */
    @Override
    public E last() throws NoSuchElementException {
        ensureSorted();
        final int length = sorted.length;
        if (length != 0) {
            return sorted[length - 1];
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * Sorts the elements in frequency order, if not already done. The sorted array will contain
     * all elements without duplicated values, with the less frequent element first and the most
     * frequent element last (or the converse if this set has been created for reverse order).
     * If some elements appear at the same frequency, then their ordering will be preserved.
     */
    @SuppressWarnings("unchecked")
    private void ensureSorted() {
        if (sorted == null) {
            final Map.Entry<E,Integer>[] entries = count.entrySet().toArray(Map.Entry[]::new);
            Arrays.sort(entries, COMPARATOR);
            final int length = entries.length;
            sorted = (E[]) new Object[length];
            if (frequencies == null || frequencies.length != length) {
                frequencies = new int[length];
            }
            for (int i=0; i<length; i++) {
                final Map.Entry<E,Integer> entry = entries[i];
                sorted[i] = entry.getKey();
                frequencies[i] = entry.getValue() ^ order;
            }
        }
    }

    /**
     * The comparator used for sorting map entries.
     * Must be consistent with {@link #compare(Object, Object)} implementation.
     */
    private static final Comparator<Map.Entry<?,Integer>> COMPARATOR =
            (Map.Entry<?,Integer> o1, Map.Entry<?,Integer> o2) -> o1.getValue().compareTo(o2.getValue());

    /**
     * Returns the comparator used to order the elements in this set.
     *
     * <p>This method is final because the {@code FrequencySortedSet} implementation makes
     * assumptions on the comparator that would not hold if this method were overridden.</p>
     */
    @Override
    public final Comparator<E> comparator() {
        return this::compare;
    }

    /**
     * Compares the specified elements for {@linkplain #frequency frequency}. For {@code FrequencySortedSet}
     * with default ordering, this method returns a positive number if {@code o1} has been added more frequently
     * to this set than {@code o2}, a negative number if {@code o1} has been added less frequently than {@code o2},
     * and 0 otherwise. For {@code FrequencySortedSet} with reverse ordering, this is the converse.
     *
     * <p>This method is final because the {@code FrequencySortedSet} implementation makes
     * assumptions on the comparator that would not hold if this method were overridden.</p>
     *
     * @param  o1  the first object to compare.
     * @param  o2  the second object to compare.
     * @return ordering of the given objects.
     */
    public final int compare(final E o1, final E o2) {
        return signedFrequency(o1) - signedFrequency(o2);
    }

    /**
     * Returns the frequency of the specified element in this set.
     * Returns a negative number if this set has been created for reversed order.
     */
    private int signedFrequency(final E element) {
        final Integer n = count.get(element);
        return (n != null) ? n : 0;
    }

    /**
     * Returns the frequency of the specified element in this set.
     *
     * @param  element  the element whose frequency is to be obtained.
     * @return the frequency of the given element, or {@code 0} if it does not occur in this set.
     */
    public int frequency(final E element) {
        return signedFrequency(element) ^ order;
    }

    /**
     * Returns the frequency of all elements in this set, in iteration order.
     *
     * @return the frequency of all elements in this set.
     */
    public int[] frequencies() {
        ensureSorted();
        return frequencies.clone();
    }

    /**
     * Returns the content of this set as an array.
     */
    @Override
    public Object[] toArray() {
        ensureSorted();
        return sorted.clone();
    }

    /**
     * Returns the content of this set as an array.
     *
     * @param  <T>    the type of the array elements.
     * @param  array  the array where to copy the elements.
     * @return the elements in the given array, or in a new array
     *         if the given array does not have a sufficient capacity.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] array) {
        ensureSorted();
        if (array.length < sorted.length) {
            array = (T[]) Array.newInstance(array.getClass().getComponentType(), sorted.length);
        }
        System.arraycopy(sorted, 0, array, 0, sorted.length);
        return array;
    }
}
