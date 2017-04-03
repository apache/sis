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
 * A set with elements ordered by the amount of time they were {@linkplain #add added}.
 * Less frequently added elements are first, and most frequently added ones are last.
 * If some elements were added the same amount of time, then the iterator will traverse
 * them in their insertion order.
 *
 * <p>An optional boolean argument in the constructor allows the construction of set in reversed order
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
 * @param <E> the type of elements in the set.
 *
 * @since 0.8
 * @module
 */
public class FrequencySortedSet<E> extends AbstractSet<E> implements SortedSet<E>, Comparator<E>, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6034102231354388179L;

    /**
     * The frequency of occurrence for each element. We must use a linked hash map instead of an ordinary
     * hash map because we want to preserve insertion order for elements that occur at the same frequency.
     */
    private final Map<E,Integer> count;

    /**
     * {@code +1} if the element should be sorted in the usual order, or {@code -1}
     * if the elements should be sorted in reverse order (most frequent element first).
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
        this(false);
    }

    /**
     * Creates an initially empty set with the default initial capacity.
     *
     * @param  reversed  {@code true} if the elements should be sorted in reverse order
     *                   (most frequent element first, less frequent last).
     */
    public FrequencySortedSet(final boolean reversed) {
        this(16, reversed);
    }

    /**
     * Creates an initially empty set with the specified initial capacity.
     *
     * @param initialCapacity  the initial capacity.
     * @param reversed         {@code true} if the elements should be sorted in reverse order
     *                         (most frequent element first, less frequent last).
     */
    public FrequencySortedSet(final int initialCapacity, final boolean reversed) {
        count = new LinkedHashMap<>(initialCapacity);
        order = reversed ? -1 : +1;
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
     * Adds the specified element to this set. Returns {@code true} if this set changed as a result
     * of this operation. Changes in element order are not notified by the returned value.
     *
     * @param  element     the element to add.
     * @param  occurrence  the number of time to add the given elements. The default value is 1.
     * @return {@code true} if this set changed as a result of this operation.
     * @throws IllegalArgumentException if {@code occurrence} is negative.
     */
    public boolean add(final E element, int occurrence) throws IllegalArgumentException {
        if (occurrence != 0) {
            ArgumentChecks.ensurePositive("occurrence", occurrence);
            sorted = null;
            occurrence *= order;
            final Integer n = count.put(element, occurrence);
            if (n == null) {
                return true;
            }
            count.put(element, n + occurrence);
        }
        return false;
    }

    /**
     * Adds the specified element to this set. Returns {@code true} if this set changed as a result
     * of this operation. Changes in element order are not notified by the returned value.
     *
     * @param  element  the element to add.
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
        return new Iter();
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
         * Index of the next element to return.
         */
        private int index;

        /**
         * Creates an new iterator.
         */
        Iter() {
            elements = sorted;
        }

        /**
         * Returns {@code true} if there is more elements to return.
         */
        @Override
        public boolean hasNext() {
            return index < elements.length;
        }

        /**
         * Return the next element.
         */
        @Override
        public E next() {
            if (index >= elements.length) {
                throw new NoSuchElementException();
            }
            return elements[index++];
        }

        /**
         * Remove the last elements returned by {@link #next}.
         */
        @Override
        public void remove() {
            if (index == 0) {
                throw new IllegalStateException();
            }
            if (!FrequencySortedSet.this.remove(elements[index - 1])) {
                // Could also be ConcurrentModificationException - we do not differentiate.
                throw new IllegalStateException();
            }
        }
    }

    /**
     * Not yet implemented.
     */
    @Override
    public SortedSet<E> headSet(E toElement) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Not yet implemented.
     */
    @Override
    public SortedSet<E> tailSet(E fromElement) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Not yet implemented.
     */
    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        throw new UnsupportedOperationException("Not supported yet.");
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
     * Sorts the elements in frequency order, if not already done. The sorted array will contains
     * all elements without duplicated values, with the less frequent element first and the most
     * frequent last (or the converse if this set has been created for reverse order).
     * If some elements appear at the same frequency, then their ordering will be preserved.
     */
    @SuppressWarnings("unchecked")
    private void ensureSorted() {
        if (sorted != null) {
            return;
        }
        @SuppressWarnings("rawtypes")                                   // Generic array creation.
        final Map.Entry<E,Integer>[] entries = count.entrySet().toArray(new Map.Entry[count.size()]);
        Arrays.sort(entries, COMPARATOR);
        final int length = entries.length;
        sorted = (E[]) new Object[length];
        if (frequencies == null || frequencies.length != length) {
            frequencies = new int[length];
        }
        for (int i=0; i<length; i++) {
            final Map.Entry<E,Integer> entry = entries[i];
            sorted[i] = entry.getKey();
            frequencies[i] = Math.abs(entry.getValue());
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
     * For a {@code FrequencySortedSet}, the comparator is always {@code this}.
     *
     * <p>This method is final because the {@code FrequencySortedSet} implementation makes
     * assumptions on the comparator that would not hold if this method were overridden.</p>
     */
    @Override
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public final Comparator<E> comparator() {
        return this;
    }

    /**
     * Compares the specified elements for {@linkplain #frequency frequency}. For {@code FrequencySortedSet}
     * with default ordering, this method returns a positive number if {@code o1} has been added more frequently
     * to this set than {@code o2}, a negative number if {@code o1} has been added less frequently than {@code o2},
     * and 0 otherwise. For {@code FrequencySortedSet} with reverse ordering, this is the converse.
     *
     * <p>This method is final because the {@code FrequencySortedSet} implementation makes
     * assumptions on the comparator that would not hold if this method were overridden.</p>
     */
    @Override
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
     * @return the frequency of the given element, or {@code 0} if it doesn't occur in this set.
     */
    public int frequency(final E element) {
        return Math.abs(signedFrequency(element));
    }

    /**
     * Returns the frequency of each element in this set, in iteration order.
     *
     * @return the frequency of each element in this set.
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
