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
import java.util.Iterator;
import java.util.AbstractSet;
import java.lang.reflect.Array;
import org.apache.sis.util.Debug;
import org.apache.sis.util.ArraysExt;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.NullArgumentException;

import static org.apache.sis.util.collection.WeakEntry.*;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * A set of objects hold by weak references. An entry in a {@code WeakHashSet} will automatically
 * be removed when it is no longer in ordinary use. More precisely, the presence of an entry will
 * not prevent the entry from being discarded by the garbage collector, that is, made finalizable,
 * finalized, and then reclaimed. When an entry has been discarded it is effectively removed from
 * the set, so this class behaves somewhat differently than other {@link java.util.Set} implementations.
 *
 * <p>If the elements stored in this set are arrays like {@code int[]}, {@code float[]} or
 * {@code Object[]}, then the hash code computations and the comparisons are performed using
 * the static {@code hashCode(a)} and {@code equals(a1, a2)} methods defined in the {@link Arrays}
 * class.</p>
 *
 * <div class="section">Optimizing memory use in factory implementations</div>
 * The {@code WeakHashSet} class has a {@link #get(Object)} method that is not part of the
 * {@link java.util.Set} interface. This {@code get} method retrieves an entry from this set
 * that is equals to the supplied object. The {@link #unique(Object)} method combines a
 * {@code get} followed by a {@code add} operation if the specified object was not in the set.
 * This is similar in spirit to the {@link String#intern()} method. The following example shows
 * a convenient way to use {@code WeakHashSet} as an internal pool of immutable objects:
 *
 * {@preformat java
 *     private final WeakHashSet<Foo> pool = new WeakHashSet<Foo>(Foo.class);
 *
 *     public Foo create(String definition) {
 *         Foo created = new Foo(definition);
 *         return pool.unique(created);
 *     }
 * }
 *
 * Thus, {@code WeakHashSet} can be used inside a factory to prevent creating duplicate immutable objects.
 *
 * <div class="section">Thread safety</div>
 * The same {@code WeakHashSet} instance can be safely used by many threads without synchronization on the part of
 * the caller. But if a sequence of two or more method calls need to appear atomic from other threads perspective,
 * then the caller can synchronize on {@code this}.
 *
 * @param <E> The type of elements in the set.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see java.util.WeakHashMap
 */
public class WeakHashSet<E> extends AbstractSet<E> implements CheckedContainer<E> {
    /**
     * A weak reference to an element. This is an element in a linked list.
     * When the reference is disposed, it is removed from the enclosing set.
     */
    private final class Entry extends WeakEntry<E> {
        /**
         * Constructs a new weak reference.
         */
        Entry(final E obj, final Entry next, final int hash) {
            super(obj, next, hash);
        }

        /**
         * Invoked by {@link org.apache.sis.internal.system.ReferenceQueueConsumer}
         * for removing the reference from the enclosing collection.
         */
        @Override
        public void dispose() {
            super.clear();
            removeEntry(this);
        }
    }

    /**
     * Table of weak references.
     */
    private Entry[] table;

    /**
     * Number of non-null elements in {@link #table}.
     */
    private int count;

    /**
     * The type of the elements in this set.
     */
    private final Class<E> elementType;

    /**
     * {@code true} if the elements in this set may be arrays. If the elements can not
     * be arrays, then we can avoid the calls to the costly {@link Utilities} methods.
     */
    private final boolean mayContainArrays;

    /**
     * The last time when {@link #table} was not in need for rehash. When the garbage collector
     * collected a lot of elements, we will wait a few seconds before rehashing {@link #table}
     * in case lot of news elements are going to be added. Without this field, we noticed many
     * "reduce", "expand", "reduce", "expand", <i>etc.</i> cycles.
     */
    private transient long lastTimeNormalCapacity;

    /**
     * Creates a {@code WeakHashSet} for elements of the specified type.
     *
     * @param type The type of the element to be included in this set.
     */
    public WeakHashSet(final Class<E> type) {
        elementType            = type;
        mayContainArrays       = type.isArray() || type.equals(Object.class);
        lastTimeNormalCapacity = System.nanoTime();
        /*
         * Workaround for the "generic array creation" compiler error.
         * Otherwise we would use the commented-out line instead.
         */
        @SuppressWarnings("unchecked")
        @Workaround(library="JDK", version="1.7")
        final Entry[] table = (Entry[]) Array.newInstance(Entry.class, MIN_CAPACITY);
//      table = new Entry[size];
        this.table = table;
    }

    /**
     * Returns the type of elements in this set.
     */
    @Override
    public Class<E> getElementType() {
        return elementType;
    }

    /**
     * Invoked by {@link Entry} when an element has been collected by the garbage
     * collector. This method removes the weak reference from the {@link #table}.
     */
    private synchronized void removeEntry(final Entry toRemove) {
        assert isValid();
        final int capacity = table.length;
        if (toRemove.removeFrom(table, toRemove.hash % capacity)) {
            count--;
            assert isValid();
            if (count < lowerCapacityThreshold(capacity)) {
                final long currentTime = System.nanoTime();
                if (currentTime - lastTimeNormalCapacity > REHASH_DELAY) {
                    table = (Entry[]) WeakEntry.rehash(table, count, "remove");
                    lastTimeNormalCapacity = currentTime;
                    assert isValid();
                }
            }
        }
    }

    /**
     * Checks if this {@code WeakHashSet} is valid. This method counts the number of elements and
     * compares it to {@link #count}. This method is invoked in assertions only.
     */
    @Debug
    private boolean isValid() {
        if (!Thread.holdsLock(this)) {
            throw new AssertionError();
        }
        if (count > upperCapacityThreshold(table.length)) {
            throw new AssertionError(count);
        }
        return count(table) == count;
    }

    /**
     * Returns the count of element in this set.
     *
     * @return Number of elements in this set.
     */
    @Override
    public synchronized int size() {
        assert isValid();
        return count;
    }

    /**
     * Adds the specified element to this set if it is not already present.
     * If this set already contains the specified element, the call leaves
     * this set unchanged and returns {@code false}.
     *
     * @param  element Element to be added to this set.
     * @return {@code true} if this set did not already contain the specified element.
     * @throws NullArgumentException If the given object is {@code null}.
     */
    @Override
    public synchronized boolean add(final E element) throws NullArgumentException {
        ArgumentChecks.ensureNonNull("element", element);
        return intern(element, ADD) == null;
    }

    /**
     * Removes a single instance of the specified element from this set, if it is present
     * Null values are considered never present.
     *
     * @param  element element to be removed from this set, if present. Can be {@code null}.
     * @return {@code true} if the set contained the specified element.
     */
    @Override
    public synchronized boolean remove(final Object element) {
        return intern(element, REMOVE) != null;
    }

    /**
     * Returns an object equals to the specified object, if present. If this set doesn't
     * contain any object equals to {@code element}, then this method returns {@code null}.
     * Null values are considered never present.
     *
     * @param  element The element to get.
     * @return An element equals to the given one if already presents in the set,
     *         or {@code null} otherwise.
     *
     * @see #unique(Object)
     */
    public synchronized E get(final Object element) {
        return intern(element, GET);
    }

    /**
     * Returns {@code true} if this set contains the specified element.
     * Null values are considered never present.
     *
     * @param  element Object to be checked for containment in this set. Can be {@code null}.
     * @return {@code true} if this set contains the specified element.
     */
    @Override
    public synchronized boolean contains(final Object element) {
        return intern(element, GET) != null;
    }

    /**
     * Returns an object equals to {@code element} if such an object already exist in this
     * {@code WeakHashSet}. Otherwise, adds {@code element} to this {@code WeakHashSet}.
     * This method is functionally equivalents to the following code:
     *
     * {@preformat java
     *     if (element != null) {
     *         T current = get(element);
     *         if (current != null) {
     *             return current;
     *         } else {
     *             add(element);
     *         }
     *     }
     *     return element;
     * }
     *
     * @param  <T> The type of the element to get. Can be {@code null}.
     * @param  element The element to get or to add in the set if not already presents,
     *         or {@code null} if the given element was null.
     * @return An element equals to the given one if already presents in the set,
     *         or the given {@code object} otherwise.
     */
    public synchronized <T extends E> T unique(final T element) {
        // There is no way to make sure that this operation is really safe.
        // We have to trust the Object.equals(Object) method to be strict
        // about the type of compared objects.
        return (T) intern(element, INTERN);
    }

    // Arguments for the {@link #intern} method.
    /** The "remove" operation.  */  private static final int REMOVE = -1;
    /** The "get"    operation.  */  private static final int GET    =  0;
    /** The "add"    operation.  */  private static final int ADD    = +1;
    /** The "intern" operation.  */  private static final int INTERN = +2;

    /**
     * Implementation of the {@link #add(Object)}, {@link #remove(Object)}, {@link #get(Object)},
     * {@link #contains(Object)} and {@link #unique(Object)} methods.
     */
    private E intern(final Object obj, final int operation) {
        assert isValid();
        if (obj != null) {
            /*
             * Check if the object is already contained in this
             * WeakHashSet. If yes, return the existing element.
             */
            Entry[] table = this.table;
            final int hash = (mayContainArrays ? Utilities.deepHashCode(obj) : obj.hashCode()) & HASH_MASK;
            int index = hash % table.length;
            for (Entry e=table[index]; e!=null; e=(Entry) e.next) {
                final E candidate = e.get();
                if (mayContainArrays ? Objects.deepEquals(candidate, obj) : obj.equals(candidate)) {
                    if (operation == REMOVE) {
                        e.dispose();
                    }
                    return candidate;
                }
                // Do not remove the null element; lets ReferenceQueue do its job
                // (it was a bug to remove element here as an "optimization")
            }
            if (operation >= ADD) {
                /*
                 * Check if the table needs to be rehashed, and add {@code obj} to the table.
                 */
                if (++count >= lowerCapacityThreshold(table.length)) {
                    if (count > upperCapacityThreshold(table.length)) {
                        this.table = table = (Entry[]) rehash(table, count, "add");
                        index = hash % table.length;
                    }
                    lastTimeNormalCapacity = System.nanoTime();
                }
                final E element = elementType.cast(obj);
                table[index] = new Entry(element, table[index], hash);
                assert isValid();
                if (operation == INTERN) {
                    return element;
                }
            }
        }
        return null;
    }

    /**
     * Removes all of the elements from this set.
     */
    @Override
    public synchronized void clear() {
        Arrays.fill(table, null);
        count = 0;
    }

    /**
     * Returns a view of this set as an array. Elements will be in an arbitrary
     * order. Note that this array contains strong references. Consequently, no
     * object reclamation will occur as long as a reference to this array is hold.
     *
     * @return All elements in this set.
     */
    @Override
    public synchronized E[] toArray() {
        assert isValid();
        @SuppressWarnings("unchecked")
        final E[] elements = (E[]) Array.newInstance(elementType, count);
        int index = 0;
        for (Entry el : table) {
            while (el != null) {
                if ((elements[index] = el.get()) != null) {
                    index++;
                }
                el = (Entry) el.next;
            }
        }
        return ArraysExt.resize(elements, index);
    }

    /**
     * Returns an iterator over the elements contained in this collection.
     * No element from this set will be garbage collected as long as a
     * reference to the iterator is hold.
     *
     * @return An iterator over all elements in this set.
     */
    @Override
    public Iterator<E> iterator() {
        return Arrays.asList(toArray()).iterator();
    }
}
