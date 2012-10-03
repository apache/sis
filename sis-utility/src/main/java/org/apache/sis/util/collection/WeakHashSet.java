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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.lang.reflect.Array;
import java.lang.ref.WeakReference;
import net.jcip.annotations.ThreadSafe;

import org.apache.sis.util.Debug;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.Disposable;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.internal.util.ReferenceQueueConsumer;

import static org.apache.sis.util.Arrays.resize;

// Related to JDK7
import org.apache.sis.internal.util.Objects;


/**
 * A set of objects hold by weak references. An entry in a {@code WeakHashSet} will automatically
 * be removed when it is no longer in ordinary use. More precisely, the presence of an entry will
 * not prevent the entry from being discarded by the garbage collector, that is, made finalizable,
 * finalized, and then reclaimed. When an entry has been discarded it is effectively removed from
 * the set, so this class behaves somewhat differently than other {@link java.util.Set} implementations.
 * <p>
 * If the elements stored in this set are arrays like {@code int[]}, {@code float[]} or
 * {@code Object[]}, then the hash code computations and the comparisons are performed using
 * the static {@code hashCode(a)} and {@code equals(a1, a2)} methods defined in the {@link Arrays}
 * class.
 *
 * {@section Optimizing memory use in factory implementations}
 * The {@code WeakHashSet} class has a {@link #get(Object)} method that is not part of the
 * {@link java.util.Set} interface. This {@code get} method retrieves an entry from this set
 * that is equals to the supplied object. The {@link #unique(Object)} method combines a
 * {@code get} followed by a {@code add} operation if the specified object was not in the set.
 * This is similar in spirit to the {@link String#intern()} method. The following example shows
 * a convenient way to use {@code WeakHashSet} as an internal pool of immutable objects:
 *
 * {@preformat java
 *     private final WeakHashSet<Foo> pool = WeakHashSet.newInstance(Foo.class);
 *
 *     public Foo create(String definition) {
 *         Foo created = new Foo(definition);
 *         return pool.unique(created);
 *     }
 * }
 *
 * Thus, {@code WeakHashSet} can be used inside a factory to prevent creating duplicate
 * immutable objects.
 *
 * @param <E> The type of elements in the set.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @since   0.3 (derived from geotk-1.0)
 * @version 0.3
 * @module
 *
 * @see java.util.WeakHashMap
 */
@ThreadSafe
public class WeakHashSet<E> extends AbstractSet<E> implements CheckedContainer<E> {
    /**
     * Minimal capacity for {@link #table}.
     */
    private static final int MIN_CAPACITY = 21;

    /**
     * Load factor. Control the moment where {@link #table} must be rebuild.
     */
    private static final float LOAD_FACTOR = 0.75f;

    /**
     * A weak reference to an element. This is an element in a linked list.
     * When the reference is disposed, it is removed from the enclosing set.
     */
    private final class Entry extends WeakReference<E> implements Disposable {
        /**
         * The next entry, or {@code null} if there is none.
         */
        Entry next;

        /**
         * The hash value of this element.
         */
        final int hash;

        /**
         * Constructs a new weak reference.
         */
        Entry(final E obj, final Entry next, final int hash) {
            super(obj, ReferenceQueueConsumer.DEFAULT.queue);
            assert ReferenceQueueConsumer.DEFAULT.isAlive();
            this.next = next;
            this.hash = hash;
        }

        /**
         * Clears the reference.
         */
        @Override
        public void dispose() {
            clear();
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
     * The next size value at which to resize. This value should
     * be <code>{@link #table}.length*{@link #LOAD_FACTOR}</code>.
     */
    private int threshold;

    /**
     * Constructs a {@code WeakHashSet} for elements of the specified type.
     *
     * @param <E>  The type of elements in the set.
     * @param type The type of elements in the set.
     * @return An initially empty set for elements of the given type.
     */
    public static <E> WeakHashSet<E> newInstance(final Class<E> type) {
        return new WeakHashSet<E>(type);
    }

    /**
     * Constructs a {@code WeakHashSet} for elements of the specified type.
     *
     * @param type The type of the element to be included in this set.
     */
    protected WeakHashSet(final Class<E> type) {
        this.elementType = type;
        mayContainArrays = type.isArray() || type.equals(Object.class);
        final Entry[] table = newEntryTable(MIN_CAPACITY);
        threshold = Math.round(table.length * LOAD_FACTOR);
    }

    /**
     * Sets the {@link #table} array to the specified size. The content of the old array is lost.
     * The value is returned for convenience (this is actually a paranoiac safety for making sure
     * that the caller will really use the new array, in case of synchronization bug).
     * <p>
     * This method is a workaround for the "<cite>generic array creation</cite>" compiler error.
     * Otherwise we would use the commented-out line instead.
     */
    @Workaround(library="JDK", version="1.7")
    @SuppressWarnings("unchecked")
    private Entry[] newEntryTable(final int size) {
//      table = new Entry[size];
        return table = (Entry[]) Array.newInstance(Entry.class, size);
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
     * collector. This method will remove the weak reference from {@link #table}.
     */
    private synchronized void removeEntry(final Entry toRemove) {
        assert valid() : count;
        final Entry[] table = this.table;
        final int i = toRemove.hash % table.length;
        Entry prev = null;
        Entry e = table[i];
        while (e != null) {
            if (e == toRemove) {
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    table[i] = e.next;
                }
                count--;
                assert valid();
                /*
                 * If the number of elements has dimunished significatively, rehash the table.
                 * We can't continue the loop pass that point, since 'e' is no longer valid.
                 */
                if (count <= threshold/4) {
                    this.table = rehash("remove");
                }
                return;
            }
            prev = e;
            e = e.next;
        }
        assert valid();
        /*
         * If we reach this point, its mean that reference 'toRemove' has not
         * been found. This situation may occurs if 'toRemove' has already been
         * removed in a previous run of 'rehash'.
         */
    }

    /**
     * Rehash {@link #table}.
     *
     * @param  caller The method invoking this one. User for logging purpose only.
     * @return The new table array. This is actually the value of the {@link #table} field, but is
     *         returned as a paranoiac safety for making sure that the caller uses the table we just
     *         created (in case of synchronization bug).
     */
    private Entry[] rehash(final String caller) {
        assert Thread.holdsLock(this);
        assert valid();
        final Entry[] oldTable = table;
        final int capacity = Math.max(Math.round(count / (LOAD_FACTOR/2)), count + MIN_CAPACITY);
        if (capacity == oldTable.length) {
            return oldTable;
        }
        final Entry[] table = newEntryTable(capacity);
        threshold = Math.round(capacity * LOAD_FACTOR);
        for (int i=0; i<oldTable.length; i++) {
            for (Entry next=oldTable[i]; next!=null;) {
                final Entry e = next;
                next = next.next; // We keep 'next' right now because its value will change.
                final int index = e.hash % table.length;
                e.next = table[index];
                table[index] = e;
            }
        }
        final Logger logger = Logging.getLogger(WeakHashSet.class);
        final Level   level = Level.FINEST;
        if (logger.isLoggable(level)) {
            final LogRecord record = new LogRecord(level,
                    "Rehash from " + oldTable.length + " to " + table.length);
            record.setSourceMethodName(caller);
            record.setSourceClassName(WeakHashSet.class.getName());
            record.setLoggerName(logger.getName());
            logger.log(record);
        }
        assert valid();
        return table;
    }

    /**
     * Checks if this {@code WeakHashSet} is valid. This method counts the number of elements and
     * compares it to {@link #count}. If the check fails, the number of elements is corrected (if
     * we didn't, an {@link AssertionError} would be thrown for every operations after the first
     * error, which make debugging more difficult). The set is otherwise unchanged, which should
     * help to get similar behavior as if assertions hasn't been turned on.
     */
    @Debug
    private boolean valid() {
        int n = 0;
        final Entry[] table = this.table;
        for (int i=0; i<table.length; i++) {
            for (Entry e=table[i]; e!=null; e=e.next) {
                n++;
            }
        }
        if (n != count) {
            count = n;
            return false;
        } else {
            return true;
        }
    }

    /**
     * Returns the count of element in this set.
     *
     * @return Number of elements in this set.
     */
    @Override
    public synchronized int size() {
        assert valid();
        return count;
    }

    /**
     * Adds the specified element to this set if it is not already present.
     * If this set already contains the specified element, the call leaves
     * this set unchanged and returns {@code false}.
     *
     * @param  obj Element to be added to this set.
     * @return {@code true} if this set did not already contain the specified element.
     */
    @Override
    public synchronized boolean add(final E obj) {
        return intern(obj, ADD) == null;
    }

    /**
     * Removes a single instance of the specified element from this set, if it is present
     *
     * @param  obj element to be removed from this set, if present.
     * @return {@code true} if the set contained the specified element.
     */
    @Override
    public synchronized boolean remove(final Object obj) {
        return intern(elementType.cast(obj), REMOVE) != null;
    }

    /**
     * Returns an object equals to the specified object, if present. If
     * this set doesn't contains any object equals to {@code object},
     * then this method returns {@code null}.
     *
     * @param  <T> The type of the element to get.
     * @param  object The element to get.
     * @return An element equals to the given one if already presents in the set,
     *         or {@code null} otherwise.
     *
     * @see #unique(Object)
     */
    public synchronized <T extends E> T get(final T object) {
        return intern(object, GET);
    }

    /**
     * Returns {@code true} if this set contains the specified element.
     *
     * @param  obj Object to be checked for containment in this set.
     * @return {@code true} if this set contains the specified element.
     */
    @Override
    public synchronized boolean contains(final Object obj) {
        return obj != null && intern(elementType.cast(obj), GET) != null;
    }

    /**
     * Returns an object equals to {@code object} if such an object already exist in this
     * {@code WeakHashSet}. Otherwise, adds {@code object} to this {@code WeakHashSet}.
     * This method is equivalents to the following code:
     *
     * {@preformat java
     *     if (object != null) {
     *         Object current = get(object);
     *         if (current != null) {
     *             return current;
     *         } else {
     *             add(object);
     *         }
     *     }
     *     return object;
     * }
     *
     * @param  <T> The type of the element to get.
     * @param  object The element to get or to add in the set if not already presents.
     * @return An element equals to the given one if already presents in the set,
     *         or the given {@code object} otherwise.
     */
    public synchronized <T extends E> T unique(final T object) {
        return intern(object, INTERN);
    }

    /**
     * Iteratively call {@link #unique(Object)} for an array of objects.
     * This method is equivalents to the following code:
     *
     * {@preformat java
     *     for (int i=0; i<objects.length; i++) {
     *         objects[i] = unique(objects[i]);
     *     }
     * }
     *
     * @param objects
     *          On input, the objects to add to this set if not already present. On output,
     *          elements that are {@linkplain Object#equals(Object) equal}, but where every
     *          reference to an instance already presents in this set has been replaced by
     *          a reference to the existing instance.
     */
    public synchronized void uniques(final E[] objects) {
        for (int i=0; i<objects.length; i++) {
            objects[i] = intern(objects[i], INTERN);
        }
    }

    // Arguments for the {@link #intern} method.
    /** The "remove" operation.  */  private static final int REMOVE = -1;
    /** The "get"    operation.  */  private static final int GET    =  0;
    /** The "add"    operation.  */  private static final int ADD    = +1;
    /** The "intern" operation.  */  private static final int INTERN = +2;

    /**
     * Returns an object equals to {@code obj} if such an object already exist in this
     * {@code WeakHashSet}. Otherwise, add {@code obj} to this {@code WeakHashSet}.
     * This method is equivalents to the following code:
     *
     * {@preformat java
     *     if (object!=null) {
     *         final Object current = get(object);
     *         if (current != null) {
     *             return current;
     *         } else {
     *             add(object);
     *         }
     *     }
     *     return object;
     * }
     */
    private <T extends E> T intern(final T obj, final int operation) {
        assert Thread.holdsLock(this);
        assert ReferenceQueueConsumer.DEFAULT.isAlive();
        assert valid() : count;
        if (obj != null) {
            /*
             * Check if the object is already contained in this
             * WeakHashSet. If yes, return the existing element.
             */
            Entry[] table = this.table;
            final int hash = (mayContainArrays ? Utilities.deepHashCode(obj) : obj.hashCode()) & 0x7FFFFFFF;
            int index = hash % table.length;
            for (Entry e=table[index]; e!=null; e=e.next) {
                final E candidate = e.get();
                if (mayContainArrays ? Objects.deepEquals(candidate, obj) : obj.equals(candidate)) {
                    if (operation == REMOVE) {
                        e.dispose();
                    }
                    assert candidate.getClass() == obj.getClass() : candidate;
                    @SuppressWarnings("unchecked")
                    final T result = (T) candidate;
                    return result;
                }
                // Do not remove the null element; lets ReferenceQueue do its job
                // (it was a bug to remove element here as an "optimization")
            }
            if (operation >= ADD) {
                /*
                 * Check if the table needs to be rehashed, and add {@code obj} to the table.
                 */
                if (count >= threshold) {
                    table = rehash("add");
                    index = hash % table.length;
                }
                table[index] = new Entry(obj, table[index], hash);
                count++;
            }
        }
        assert valid();
        return (operation == INTERN) ? obj : null;
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
        assert valid();
        @SuppressWarnings("unchecked")
        final E[] elements = (E[]) Array.newInstance(elementType, count);
        int index = 0;
        final Entry[] table = this.table;
        for (int i=0; i<table.length; i++) {
            for (Entry el=table[i]; el!=null; el=el.next) {
                if ((elements[index] = el.get()) != null) {
                    index++;
                }
            }
        }
        return resize(elements, index);
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
