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
import java.util.Set;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import net.jcip.annotations.ThreadSafe;
import org.apache.sis.util.Decorator;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * A {@linkplain Collections#checkedMap(Map, Class, Class) checked} and
 * {@linkplain Collections#synchronizedMap(Map) synchronized} {@link LinkedHashMap}.
 * The type checks are performed at run-time in addition to the compile-time checks.
 *
 * <p>Using this class is similar to wrapping a {@link LinkedHashMap} using the methods provided
 * in the standard {@link Collections} class, except for the following advantages:</p>
 *
 * <ul>
 *   <li>Avoid the two levels of indirection (for type check and synchronization).</li>
 *   <li>Checks for write permission.</li>
 *   <li>Overrideable methods for controlling the synchronization lock and write permission checks.</li>
 * </ul>
 *
 * The synchronization is provided mostly in order to prevent damages
 * to the map in case of concurrent access. It does <strong>not</strong> prevent
 * {@link java.util.ConcurrentModificationException} to be thrown during iterations,
 * unless the whole iteration is synchronized on this map {@linkplain #getLock() lock}.
 * For real concurrency, see the {@link java.util.concurrent} package instead.
 *
 * {@note The above is the reason why the name of this class emphases the <cite>checked</cite>
 *        aspect rather than the <cite>synchronized</cite> aspect of the map.}
 *
 * @param <K> The type of keys in the map.
 * @param <V> The type of values in the map.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 *
 * @see Collections#checkedMap(Map, Class, Class)
 * @see Collections#synchronizedMap(Map)
 */
@ThreadSafe
public class CheckedHashMap<K,V> extends LinkedHashMap<K,V> implements Cloneable {
    /**
     * Serial version UID for compatibility with different versions.
     */
    private static final long serialVersionUID = -7777695267921872849L;

    /**
     * The class type for keys.
     */
    private final Class<K> keyType;

    /**
     * The class type for values.
     */
    private final Class<V> valueType;

    /**
     * Constructs a map of the specified key and value types.
     *
     * @param keyType   The key type (can not be null).
     * @param valueType The value type (can not be null).
     */
    public CheckedHashMap(final Class<K> keyType, final Class<V> valueType) {
        this.keyType   = keyType;
        this.valueType = valueType;
        ensureNonNull("keyType",   keyType);
        ensureNonNull("valueType", valueType);
    }

    /**
     * Checks the type of the specified object.
     *
     * @param  element the object to check, or {@code null}.
     * @throws IllegalArgumentException if the specified element is not of the expected type.
     */
    private static <E> void ensureValidType(final E element, final Class<E> type)
            throws IllegalArgumentException
    {
        if (element!=null && !type.isInstance(element)) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.IllegalArgumentClass_3, "element", type, element.getClass()));
        }
    }

    /**
     * Checks if changes in this map are allowed. This method is automatically invoked
     * after this map got the {@linkplain #getLock() lock} and before any operation that
     * may change the content. If the write operation is allowed, then this method shall
     * returns normally. Otherwise an {@link UnsupportedOperationException} is thrown.
     *
     * <p>The default implementation does nothing significant (see below), thus allowing this map to
     * be modified. Subclasses can override this method if they want to control write permissions.</p>
     *
     * {@note Actually the current implementation contains an <code>assert</code> statement
     *        ensuring that the thread holds the lock. This is an implementation details that may
     *        change in any future version of the SIS library. Nevertheless methods that override
     *        this one are encouraged to invoke <code>super.checkWritePermission()</code>.}
     *
     * @throws UnsupportedOperationException if this map is unmodifiable.
     */
    protected void checkWritePermission() throws UnsupportedOperationException {
        assert Thread.holdsLock(getLock());
    }

    /**
     * Returns the synchronization lock. The default implementation returns {@code this}.
     *
     * {@section Note for subclass implementors}
     * Subclasses that override this method must be careful to update the lock reference
     * (if needed) when this map is {@linkplain #clone() cloned}.
     *
     * @return The synchronization lock.
     */
    protected Object getLock() {
        return this;
    }

    /**
     * Returns the number of elements in this map.
     */
    @Override
    public int size() {
        synchronized (getLock()) {
            return super.size();
        }
    }

    /**
     * Returns {@code true} if this map contains no elements.
     */
    @Override
    public boolean isEmpty() {
        synchronized (getLock()) {
            return super.isEmpty();
        }
    }

    /**
     * Returns {@code true} if this map contains the specified key.
     */
    @Override
    public boolean containsKey(final Object key) {
        synchronized (getLock()) {
            return super.containsKey(key);
        }
    }

    /**
     * Returns {@code true} if this map contains the specified value.
     */
    @Override
    public boolean containsValue(final Object value) {
        synchronized (getLock()) {
            return super.containsValue(value);
        }
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null} if none.
     */
    @Override
    public V get(Object key) {
        synchronized (getLock()) {
            return super.get(key);
        }
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is replaced.
     *
     * @param  key key with which the specified value is to be associated.
     * @param  value value to be associated with the specified key.
     * @return previous value associated with specified key, or {@code null}.
     * @throws IllegalArgumentException if the key or the value is not of the expected type.
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public V put(final K key, final V value)
            throws IllegalArgumentException, UnsupportedOperationException
    {
        ensureValidType(key,     keyType);
        ensureValidType(value, valueType);
        synchronized (getLock()) {
            checkWritePermission();
            return super.put(key, value);
        }
    }

    /**
     * Copies all of the mappings from the specified map to this map.
     *
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) throws UnsupportedOperationException {
        for (final Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            ensureValidType(entry.getKey(),     keyType);
            ensureValidType(entry.getValue(), valueType);
        }
        synchronized (getLock()) {
            checkWritePermission();
            super.putAll(m);
        }
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     *
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public V remove(Object key) throws UnsupportedOperationException {
        synchronized (getLock()) {
            checkWritePermission();
            return super.remove(key);
        }
    }

    /**
     * Removes all of the elements from this map.
     *
     * @throws UnsupportedOperationException if this collection is unmodifiable.
     */
    @Override
    public void clear() throws UnsupportedOperationException {
        synchronized (getLock()) {
            checkWritePermission();
            super.clear();
        }
    }

    /**
     * Returns a view of the keys in the map.
     * The returned set will support {@linkplain Set#remove(Object) element removal}
     * only if the {@link #checkWritePermission()} method does not throw exception.
     *
     * @return A synchronized view of the keys in the map.
     */
    @Override
    public Set<K> keySet() {
        synchronized (getLock()) {
            return new SyncSet<K>(super.keySet());
        }
    }

    /**
     * Returns a view of the values in the map.
     * The returned collection will support {@linkplain Collection#remove(Object) element removal}
     * only if the {@link #checkWritePermission()} method does not throw exception.
     *
     * @return A synchronized view of the values in the map.
     */
    @Override
    public Collection<V> values() {
        synchronized (getLock()) {
            return new Sync<V>(super.values());
        }
    }

    /**
     * Returns a view of the entries in the map.
     * The returned set will support {@linkplain Set#remove(Object) element removal}
     * only if the {@link #checkWritePermission()} method does not throw exception.
     *
     * @return A synchronized view of the keys in the map.
     */
    @Override
    public Set<Map.Entry<K,V>> entrySet() {
        synchronized (getLock()) {
            return new SyncSet<Map.Entry<K,V>>(super.entrySet());
        }
    }

    /**
     * Returns a string representation of this map.
     */
    @Override
    public String toString() {
        synchronized (getLock()) {
            return super.toString();
        }
    }

    /**
     * Compares the specified object with this map for equality.
     */
    @Override
    public boolean equals(Object o) {
        synchronized (getLock()) {
            return super.equals(o);
        }
    }

    /**
     * Returns the hash code value for this map.
     */
    @Override
    public int hashCode() {
        synchronized (getLock()) {
            return super.hashCode();
        }
    }

    /**
     * Returns a shallow copy of this map.
     *
     * @return A shallow copy of this map.
     */
    @Override
    @SuppressWarnings("unchecked")
    public CheckedHashMap<K,V> clone() {
        synchronized (getLock()) {
            return (CheckedHashMap<K,V>) super.clone();
        }
    }

    /**
     * A synchronized iterator with a check for write permission prior element removal.
     * This class wraps the iterator provided by the {@link LinkedHashMap} views.
     */
    @ThreadSafe
    @Decorator(Iterator.class)
    private final class Iter<E> implements Iterator<E> {
        /** The {@link LinkedHashMap} iterator. */
        private final Iterator<E> iterator;

        /** Creates a new wrapper for the given {@link LinkedHashMap} iterator. */
        Iter(final Iterator<E> iterator) {
            this.iterator = iterator;
        }

        /** Returns {@code true} if there is more elements in the iteration. */
        @Override
        public boolean hasNext() {
            synchronized (getLock()) {
                return iterator.hasNext();
            }
        }

        /** Returns the next element in the iteration. */
        @Override
        public E next() throws NoSuchElementException {
            synchronized (getLock()) {
                return iterator.next();
            }
        }

        /** Removes the previous element if the enclosing {@link CheckedHashMap} allows write operations. */
        @Override
        public void remove() throws UnsupportedOperationException {
            synchronized (getLock()) {
                checkWritePermission();
                iterator.remove();
            }
        }
    }

    /**
     * A collection or a set synchronized on the enclosing map {@linkplain #getLock() lock}.
     * This is used directly for wrapping {@link Map#values()}, or indirectly for wrapping
     * {@link Map#keySet()} or {@link Map#entrySet()} views.
     */
    @ThreadSafe
    @Decorator(Collection.class)
    private class Sync<E> implements Collection<E> {
        /** The {@link Map#keySet()}, {@link Map#values()} or {@link Map#entrySet()} view. */
        private final Collection<E> view;

        /** Create a new synchronized wrapper for the given view. */
        Sync(final Collection<E> view) {
            this.view = view;
        }

        /** Returns a synchronized and checked iterator over the elements in this collection. */
        @Override
        public final Iterator<E> iterator() {
            synchronized (getLock()) {
                return new Iter<E>(view.iterator());
            }
        }

        /** Returns the number of elements in the collection. */
        @Override
        public final int size() {
            synchronized (getLock()) {
                return view.size();
            }
        }

        /** Returns {@code true} if the collection is empty. */
        @Override
        public final boolean isEmpty() {
            synchronized (getLock()) {
                return view.isEmpty();
            }
        }

        /** Returns {@code true} if the collection contains the given element. */
        @Override
        public final boolean contains(final Object element) {
            synchronized (getLock()) {
                return view.contains(element);
            }
        }

        /** Returns {@code true} if the collection contains all elements of the given collection. */
        @Override
        public final boolean containsAll(final Collection<?> collection) {
            synchronized (getLock()) {
                return view.containsAll(collection);
            }
        }

        /** Always unsupported operation in hash map views. */
        @Override
        public final boolean add(final E element) throws UnsupportedOperationException {
            throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnsupportedOperation_1, "add"));
        }

        /** Always unsupported operation in hash map views. */
        @Override
        public final boolean addAll(final Collection<? extends E> collection) throws UnsupportedOperationException {
            throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnsupportedOperation_1, "addAll"));
        }

        /** Remove the given element if the enclosing {@link CheckedHashMap} supports write operations. */
        @Override
        public final boolean remove(final Object element) throws UnsupportedOperationException {
            synchronized (getLock()) {
                checkWritePermission();
                return view.remove(element);
            }
        }

        /** Remove the given elements if the enclosing {@link CheckedHashMap} supports write operations. */
        @Override
        public final boolean removeAll(final Collection<?> collection) throws UnsupportedOperationException {
            synchronized (getLock()) {
                checkWritePermission();
                return view.removeAll(collection);
            }
        }

        /** Retains only the given elements if the enclosing {@link CheckedHashMap} supports write operations. */
        @Override
        public final boolean retainAll(final Collection<?> collection) throws UnsupportedOperationException {
            synchronized (getLock()) {
                checkWritePermission();
                return view.retainAll(collection);
            }
        }

        /** Removes all elements from the collection. */
        @Override
        public final void clear() throws UnsupportedOperationException {
            synchronized (getLock()) {
                checkWritePermission();
                view.clear();
            }
        }

        /** Returns the elements in an array. */
        @Override
        public final Object[] toArray() {
            synchronized (getLock()) {
                return view.toArray();
            }
        }

        /** Returns the elements in an array. */
        @Override
        public final <T> T[] toArray(final T[] array) {
            synchronized (getLock()) {
                return view.toArray(array);
            }
        }

        /** Returns a string representation of the elements. */
        @Override
        public final String toString() {
            synchronized (getLock()) {
                return view.toString();
            }
        }

        /** Compare this collection with the given object for equality. */
        @Override
        public final boolean equals(final Object other) {
            synchronized (getLock()) {
                return view.equals(other);
            }
        }

        /** Returns a hash code value for this collection. */
        @Override
        public final int hashCode() {
            synchronized (getLock()) {
                return view.hashCode();
            }
        }
    }

    /**
     * A set synchronized on the enclosing map {@linkplain #getLock() lock}.
     * This is used for wrapping {@link Map#keySet()} or {@link Map#entrySet()} views.
     */
    @Decorator(Set.class)
    private final class SyncSet<E> extends Sync<E> implements Set<E> {
        /** Create a new synchronized wrapper for the given view. */
        SyncSet(final Set<E> set) {
            super(set);
        }
    }
}
