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
package org.apache.sis.internal.util;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.AbstractCollection;
import java.util.NoSuchElementException;
import org.apache.sis.io.TableAppender;
import org.apache.sis.util.resources.Errors;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * An alternative to {@link java.util.AbstractMap java.util.AbstractMap} using different implementation strategies.
 * Instead than providing default method implementations on top of {@link #entrySet()}, this base class uses more
 * often the {@link #get(Object)} method with the assumption that the map can not contain null values, or use a
 * special-purpose {@link #entryIterator()} which can reduce the amount of object creations.
 *
 * <p><strong>This base class is for Apache SIS internal purpose only. Do not use!</strong>
 * This class is less robust than the JDK one (e.g. does not accept null values), forces subclasses to implement
 * more methods, uses a non-standard {@link #entryIterator()}, and may change in any future SIS version.</p>
 *
 * <p>This {@code AbstractMap} implementation makes the following assumptions.
 * <strong>Do not use this class if any of those assumptions do not hold!</strong></p>
 * <ul>
 *   <li>The map can not contain {@code null} value.</li>
 *   <li>The map can not contain references to itself, directly or indirectly, in the keys or in the values.</li>
 * </ul>
 *
 * <p>Read-only subclasses need to implement the following methods:</p>
 * <ul>
 *   <li>{@link #size()} (not mandatory but recommended)</li>
 *   <li>{@link #get(Object)}</li>
 *   <li>{@link #entryIterator()} (non-standard method)</li>
 * </ul>
 *
 * <p>Read/write subclasses can implement those additional methods:</p>
 * <ul>
 *   <li>{@link #clear()}</li>
 *   <li>{@link #remove(Object)}</li>
 *   <li>{@link #put(Object,Object)}</li>
 *   <li>{@link #addKey(Object)} (non-standard, optional method)</li>
 *   <li>{@link #addValue(Object)} (non-standard, optional method)</li>
 * </ul>
 *
 * @param <K> The type of keys maintained by the map.
 * @param <V> The type of mapped values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.7
 * @module
 */
public abstract class AbstractMap<K,V> implements Map<K,V> {
    /**
     * An iterator over the entries in the enclosing map. This iterator has two main differences compared
     * to the standard {@code Map.entrySet().iterator()}:
     * <ul>
     *   <li>The {@link #next()} method checks if there is more element and moves to the next one in a single step.
     *       This is exactly the same approach than {@link java.sql.ResultSet#next()}.</li>
     *   <li>Entry elements are returned by the {@link #getKey()} and {@link #getValue()} methods
     *       instead than creating new {@code Map.Element} on each iterator.</li>
     * </ul>
     *
     * @param <K> The type of keys maintained by the map.
     * @param <V> The type of mapped values.
     *
     * @see AbstractMap#entryIterator()
     */
    protected static abstract class EntryIterator<K,V> {
        /**
         * Moves the iterator to the next position, and returns {@code true} if there is at least one remaining element.
         *
         * @return {@code false} if this method reached iteration end.
         */
        protected abstract boolean next();

        /**
         * Returns the key at the current iterator position.
         * This method is invoked only after {@link #next()}.
         *
         * @return The key at the current iterator position.
         */
        protected abstract K getKey();

        /**
         * Returns the value at the current iterator position.
         * This method is invoked only after {@link #next()}.
         *
         * @return The value at the current iterator position.
         */
        protected abstract V getValue();

        /**
         * Returns the entry at the current iterator position.
         * This method is invoked only after {@link #next()}.
         * The default implementation creates an immutable entry with {@link #getKey()} and {@link #getValue()}.
         *
         * @return The entry at the current iterator position.
         */
        protected Entry<K,V> getEntry() {
            return new java.util.AbstractMap.SimpleImmutableEntry<K,V>(getKey(), getValue());
        }

        /**
         * Removes the entry at the current iterator position (optional operation).
         * The default implementation throws {@code UnsupportedOperationException}.
         */
        protected void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException(message(false));
        }
    }

    /**
     * An implementation of {@link EntryIterator} which delegates its work to a standard iterator.
     * Subclasses can modify the {@link #value} or other properties during iteration.
     *
     * <p>This method does not implement the {@link #remove()} method, thus assuming an unmodifiable map
     * (which is consistent with the default implementation of {@link AbstractMap} methods).
     * Modifiable maps should override {@code remove()} themselves.</p>
     *
     * @param <K> The type of keys maintained by the map.
     * @param <V> The type of mapped values.
     *
     * @see AbstractMap#entryIterator()
     */
    protected static class IteratorAdapter<K,V> extends EntryIterator<K,V> {
        /**
         * The standard iterator on which to delegate the work.
         * It is safe to change this value before to invoke {@link #next()}.
         */
        protected Iterator<Entry<K,V>> it;

        /**
         * The entry found by the last call to {@link #next()}.
         */
        protected Entry<K,V> entry;

        /**
         * The value of <code>{@linkplain #entry}.getValue()}</code>.
         * It is safe to change this value after {@link #next()} invocation.
         */
        protected V value;

        /**
         * Creates a new adapter initialized to the entry iterator of the given map.
         *
         * @param map The map from which to return entries.
         */
        public IteratorAdapter(final Map<K,V> map) {
            it = map.entrySet().iterator();
        }

        /**
         * Moves to the next entry having a non-null value. If this method returns {@code true}, then the
         * {@link #entry} and {@link #value} fields are set to the properties of the new current entry.
         * Otherwise (if this method returns {@code false}) the {@link #entry} and {@link #value} fields
         * are undetermined.
         *
         * @return {@inheritDoc}
         */
        @Override
        protected boolean next() {
            do {
                if (!it.hasNext()) {
                    return false;
                }
                entry = it.next();
                value = entry.getValue();
            } while (value == null);
            return true;
        }

        /**
         * Returns <code>{@linkplain #entry}.getKey()}</code>.
         *
         * @return {@inheritDoc}
         */
        @Override
        protected K getKey() {
            return entry.getKey();
        }

        /**
         * Returns {@link #value}, which was itself initialized to <code>{@linkplain #entry}.getValue()}</code>.
         *
         * @return {@inheritDoc}
         */
        @Override
        protected V getValue() {
            return value;
        }
    }

    /**
     * For subclass constructors.
     */
    protected AbstractMap() {
    }

    /**
     * Returns the number of key-value mappings in this map.
     * The default implementation count the number of values returned by {@link #entryIterator()}.
     * Subclasses should implement a more efficient method.
     */
    @Override
    public int size() {
        int count = 0;
        for (final EntryIterator<K,V> it = entryIterator(); it.next();) {
            if (++count == Integer.MAX_VALUE) break;
        }
        return count;
    }

    /**
     * Returns {@code true} if this map contains no element.
     *
     * @return {@code true} if this map contains no element.
     */
    @Override
    public boolean isEmpty() {
        return !entryIterator().next();
    }

    /**
     * Returns {@code true} if this map contains a value for the given name.
     * The default implementation assumes that the map can not contain {@code null} values.
     *
     * @param  key The key for which to test the presence of a value.
     * @return {@code true} if the map contains a non-null value for the given key.
     */
    @Override
    public boolean containsKey(final Object key) {
        return get(key) != null;
    }

    /**
     * Returns {@code true} if this map contains the given value.
     * The default implementation iterates over all values using the {@link #entryIterator()}.
     *
     * @param  value The value for which to test the presence.
     * @return {@code true} if the map contains the given value.
     */
    @Override
    public boolean containsValue(final Object value) {
        final EntryIterator<K,V> it = entryIterator();
        if (it != null) while (it.next()) {
            if (it.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The message to gives to the exception to be thrown in case of unsupported operation.
     *
     * @param add {@code true} if this method is invoked from {@link #addKey(Object)} or {@link #addValue(Object)}.
     */
    static String message(final boolean add) {
        return Errors.format(add ? Errors.Keys.UnsupportedOperation_1 : Errors.Keys.UnmodifiableObject_1,
                             add ? "add" : Map.class);
    }

    /**
     * Removes all entries in this map.
     * The default operation throws {@link UnsupportedOperationException}.
     */
    @Override
    public void clear() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(message(false));
    }

    /**
     * Removes the entry for the given key in this map.
     * The default operation throws {@link UnsupportedOperationException}.
     *
     * @param  key The key of the entry to remove.
     * @return The previous value, or {@code null} if none.
     */
    @Override
    public V remove(Object key) throws UnsupportedOperationException {
        throw new UnsupportedOperationException(message(false));
    }

    /**
     * Adds an entry for the given key in this map.
     * The default operation throws {@link UnsupportedOperationException}.
     *
     * @param  key The key of the entry to remove.
     * @param  value The value to associate to the given key.
     * @return The previous value, or {@code null} if none.
     */
    @Override
    public V put(K key, V value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException(message(false));
    }

    /**
     * Puts all entries of the given map in this map.
     *
     * @param map The other map from which to copy the entries.
     */
    @Override
    public void putAll(final Map<? extends K, ? extends V> map) throws UnsupportedOperationException {
        for (final Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Adds the given key in this map. Implementation of this method shall generate a corresponding value.
     * The default operation throws {@link UnsupportedOperationException}.
     *
     * @param  key The key to add.
     * @return {@code true} if this map changed as a result of this operation.
     */
    protected boolean addKey(final K key) throws UnsupportedOperationException {
        throw new UnsupportedOperationException(message(true));
    }

    /**
     * Adds the given value in this map. Implementation of this method shall generate a corresponding key.
     * The default operation throws {@link UnsupportedOperationException}.
     *
     * @param  value The value to add.
     * @return {@code true} if this map changed as a result of this operation.
     */
    protected boolean addValue(final V value) throws UnsupportedOperationException {
        throw new UnsupportedOperationException(message(true));
    }

    /**
     * Returns a view over the keys in this map. The returned set supports the {@link Set#add(Object)} operation
     * if the enclosing map implements the {@link #addKey(Object)} method.
     *
     * <p>The default implementation does not cache the set on the assumption that it is very quick to create
     * and usually not retained for a long time (we often want only its iterator). Caching the set would require
     * a {@code volatile} field for thread safety, which also has cost.</p>
     *
     * @return A view of the keys in this map.
     */
    @Override
    public Set<K> keySet() {
        return new SetOfUnknownSize<K>() {
            @Override public void        clear()            {       AbstractMap.this.clear();}
            @Override public boolean     isEmpty()          {return AbstractMap.this.isEmpty();}
            @Override public int         size()             {return AbstractMap.this.size();}
            @Override public boolean     contains(Object e) {return AbstractMap.this.containsKey(e);}
            @Override public boolean     remove(Object e)   {return AbstractMap.this.remove(e) != null;}
            @Override public boolean     add(K e)           {return AbstractMap.this.addKey(e);}
            @Override public Iterator<K> iterator() {
                final EntryIterator<K,V> it = entryIterator();
                return (it != null) ? new Keys<K,V>(it) : Collections.<K>emptySet().iterator();
            }

            /** Overridden for the same reason than {@link AbstractMap#equals(Object). */
            @Override public boolean equals(final Object object) {
                if (object == this) {
                    return true;
                }
                if (!(object instanceof Set<?>)) {
                    return false;
                }
                final Set<?> that = (Set<?>) object;
                final EntryIterator<K,V> it = entryIterator();
                if (it == null) {
                    return that.isEmpty();
                }
                int size = 0;
                while (it.next()) {
                    if (!that.contains(it.getKey())) {
                        return false;
                    }
                    size++;
                }
                return size == that.size();
            }
        };
    }

    /**
     * Returns a view over the values in this map. The returned collection supports the {@link Collection#add(Object)}
     * operation if the enclosing map implements the {@link #addValue(Object)} method.
     *
     * <p>The default implementation does not cache the collection on the assumption that it is very quick to create
     * and usually not retained for a long time.</p>
     *
     * @return A view of the values in this map.
     */
    @Override
    public Collection<V> values() {
        return new AbstractCollection<V>() {
            @Override public void        clear()            {       AbstractMap.this.clear();}
            @Override public boolean     isEmpty()          {return AbstractMap.this.isEmpty();}
            @Override public int         size()             {return AbstractMap.this.size();}
            @Override public boolean     contains(Object e) {return AbstractMap.this.containsValue(e);}
            @Override public boolean     add(V e)           {return AbstractMap.this.addValue(e);}
            @Override public Iterator<V> iterator() {
                final EntryIterator<K,V> it = entryIterator();
                return (it != null) ? new Values<K,V>(it) : Collections.<V>emptySet().iterator();
            }
        };
    }

    /**
     * Returns a view over the entries in this map.
     *
     * <p>The default implementation does not cache the set on the assumption that it is very quick to create
     * and usually not retained for a long time.</p>
     *
     * @return A view of the entries in this map.
     */
    @Override
    public Set<Entry<K,V>> entrySet() {
        return new SetOfUnknownSize<Entry<K,V>>() {
            @Override public void    clear()   {       AbstractMap.this.clear();}
            @Override public boolean isEmpty() {return AbstractMap.this.isEmpty();}
            @Override public int     size()    {return AbstractMap.this.size();}

            /** Returns {@code true} if the map contains the given (key, value) pair. */
            @Override public boolean contains(final Object e) {
                if (e instanceof Entry<?,?>) {
                    final Entry<?,?> entry = (Entry<?,?>) e;
                    final Object value = get(entry.getKey());
                    if (value != null) {
                        return value.equals(entry.getValue());
                    }
                }
                return false;
            }

            /** Returns an iterator compliant to the Map contract. */
            @Override public Iterator<Entry<K,V>> iterator() {
                final EntryIterator<K,V> it = entryIterator();
                return (it != null) ? new Entries<K,V>(it) : Collections.<Entry<K,V>>emptySet().iterator();
            }

            /** Overridden for the same reason than {@link AbstractMap#equals(Object). */
            @Override public boolean equals(final Object object) {
                if (object == this) {
                    return true;
                }
                if (!(object instanceof Set<?>)) {
                    return false;
                }
                final Set<?> that = (Set<?>) object;
                final EntryIterator<K,V> it = entryIterator();
                if (it == null) {
                    return that.isEmpty();
                }
                int size = 0;
                while (it.next()) {
                    if (!that.contains(it.getEntry())) {
                        return false;
                    }
                    size++;
                }
                return size == that.size();
            }
        };
    }

    /**
     * Returns an iterator over the entries in this map.
     * It is okay (but not required) to return {@code null} if the map is empty.
     *
     * @return An iterator over the entries in this map, or {@code null}.
     */
    protected abstract EntryIterator<K,V> entryIterator();

    /**
     * Base class of iterators overs keys, values or entries.
     * Those iterators wrap an {@link EntryIterator} instance.
     */
    private static abstract class Iter<K,V> {
        /** The wrapped entry iterator. */
        private final EntryIterator<K,V> iterator;

        /** {@link #TRUE}, {@link #FALSE} or {@link #AFTER_NEXT}, or 0 if not yet determined. */
        private byte hasNext;

        /** Possible values for {@link #hasNext}. */
        private static final byte TRUE=1, FALSE=2, AFTER_NEXT=3;

        /**
         * Creates a new standard iterator wrapping the given entry iterator.
         *
         * @param iterator {@link AbstractMap#entryIterator()}.
         */
        Iter(final EntryIterator<K,V> iterator) {
            this.iterator = iterator;
        }

        /**
         * Returns {@code true} if there is at least one more element to return.
         */
        public final boolean hasNext() {
            switch (hasNext) {
                case TRUE:  return true;
                case FALSE: return false;
                default: {
                    final boolean c = iterator.next();
                    hasNext = c ? TRUE : FALSE;
                    return c;
                }
            }
        }

        /**
         * Ensures that the entry iterator is positioned on a valid entry, and returns it.
         * This method shall be invoked by implementations of {@link Iterator#next()}.
         */
        final EntryIterator<K,V> entry() {
            if (hasNext()) {
                hasNext = AFTER_NEXT;
                return iterator;
            }
            throw new NoSuchElementException();
        }

        /**
         * Removes the current entry.
         */
        public final void remove() {
            if (hasNext == AFTER_NEXT) {
                hasNext = 0;
                iterator.remove();
            } else {
                throw new IllegalStateException();
            }
        }
    }

    /**
     * Iterator over the keys.
     */
    private static final class Keys<K,V> extends Iter<K,V> implements Iterator<K> {
        Keys(final EntryIterator<K,V> it) {super(it);}
        @Override public K next() {return entry().getKey();}
    }

    /**
     * Iterator over the values.
     */
    private static final class Values<K,V> extends Iter<K,V> implements Iterator<V> {
        Values(EntryIterator<K,V> it) {super(it);}
        @Override public V next() {return entry().getValue();}
    }

    /**
     * Iterator over the entries, used only when {@link #entryIterator()} perform recycling.
     * This iterator copies each entry in an {@code SimpleImmutableEntry} instance.
     */
    private static final class Entries<K,V> extends Iter<K,V> implements Iterator<Entry<K,V>> {
        Entries(EntryIterator<K,V> it) {super(it);}
        @Override public Entry<K,V> next() {return entry().getEntry();}
    }

    /**
     * Compares this map with the given object for equality.
     *
     * @param  object The other object to compare with this map.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Map<?,?>)) {
            return false;
        }
        final Map<?,?> that = (Map<?,?>) object;
        final EntryIterator<K,V> it = entryIterator();
        if (it == null) {
            return that.isEmpty();
        }
        /*
         * We do not check if map.size() == size() because in some Apache SIS implementations,
         * the size() method have to scan through all map entries. We presume that if the maps
         * are not equal, we will find a mismatched entry soon anyway.
         */
        int size = 0;
        while (it.next()) {
            if (!it.getValue().equals(that.get(it.getKey()))) {
                return false;
            }
            size++;
        }
        return size == that.size();
    }

    /**
     * Computes a hash code value for this map.
     *
     * @return A hash code value.
     */
    @Override
    public int hashCode() {
        int code = 0;
        final EntryIterator<K,V> it = entryIterator();
        if (it != null) while (it.next()) {
            code += (Objects.hashCode(it.getKey()) ^ Objects.hashCode(it.getValue()));
        }
        return code;
    }

    /**
     * Returns a string representation of this map. The default implementation is different than the
     * {@code java.util.AbstractMap} one, as it uses a tabular format rather than formatting all entries
     * on a single line.
     *
     * @return A string representation of this map.
     */
    @Override
    public String toString() {
        final TableAppender buffer = new TableAppender(" = ");
        buffer.setMultiLinesCells(true);
        final EntryIterator<K,V> it = entryIterator();
        if (it != null) while (it.next()) {
            buffer.append(String.valueOf(it.getKey()));
            buffer.nextColumn();
            buffer.append(AbstractMapEntry.firstLine(it.getValue()));
            buffer.nextLine();
        }
        return buffer.toString();
    }
}
