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
import java.util.AbstractSet;
import java.util.AbstractCollection;
import java.util.Collections;
import org.apache.sis.io.TableAppender;
import org.apache.sis.util.resources.Errors;


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
 *   <li>{@link #size()}</li>
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
 * @version 0.5
 * @module
 */
public abstract class AbstractMap<K,V> implements Map<K,V> {
    /**
     * For subclass constructors.
     */
    protected AbstractMap() {
    }

    /**
     * Returns {@code true} if this map contains no element.
     *
     * @return {@code true} if this map contains no element.
     */
    @Override
    public boolean isEmpty() {
        return size() == 0;
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
        final Iterator<Entry<K,V>> it = entryIterator();
        if (it != null) while (it.hasNext()) {
            if (it.next().getValue().equals(value)) {
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
    private static String message(final boolean add) {
        return Errors.format(add ? Errors.Keys.UnsupportedOperation_1 : Errors.Keys.UnmodifiableObject_1,
                             add ? "add" : Map.class);
    }

    /**
     * Removes all entries in this map.
     * The default operation throws {@link UnsupportedOperationException}.
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException(message(false));
    }

    /**
     * Returns the value for the given key, or {@code defaultValue} if none.
     * The default implementation assumes that the map can not contain {@code null} values.
     *
     * @param  key The key for which to get the value.
     * @param  defaultValue The value to return if this map does not have an entry for the given key.
     * @return The value for the given key, or {@code defaultValue} if none.
     */
    @Override
    public V getOrDefault(final Object key, final V defaultValue) {
        final V value = get(key);
        return (value != null) ? value : defaultValue;
    }

    /**
     * Removes the entry for the given key in this map.
     * The default operation throws {@link UnsupportedOperationException}.
     *
     * @param  key The key of the entry to remove.
     * @return The previous value, or {@code null} if none.
     */
    @Override
    public V remove(Object key) {
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
    public V put(K key, V value) {
        throw new UnsupportedOperationException(message(false));
    }

    /**
     * Puts all entries of the given map in this map.
     *
     * @param map The other map from which to copy the entries.
     */
    @Override
    public void putAll(final Map<? extends K, ? extends V> map) {
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
    protected boolean addKey(final K key) {
        throw new UnsupportedOperationException(message(true));
    }

    /**
     * Adds the given value in this map. Implementation of this method shall generate a corresponding key.
     * The default operation throws {@link UnsupportedOperationException}.
     *
     * @param  value The value to add.
     * @return {@code true} if this map changed as a result of this operation.
     */
    protected boolean addValue(final V value) {
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
        return new AbstractSet<K>() {
            @Override public void        clear()            {       AbstractMap.this.clear();}
            @Override public boolean     isEmpty()          {return AbstractMap.this.isEmpty();}
            @Override public int         size()             {return AbstractMap.this.size();}
            @Override public boolean     contains(Object e) {return AbstractMap.this.containsKey(e);}
            @Override public boolean     remove(Object e)   {return AbstractMap.this.remove(e) != null;}
            @Override public boolean     add(K e)           {return AbstractMap.this.addKey(e);}
            @Override public Iterator<K> iterator() {
                final Iterator<Entry<K,V>> it = entryIterator();
                return (it != null) ? new Keys<>(it) : Collections.emptyIterator();
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
                final Iterator<Entry<K,V>> it = entryIterator();
                return (it != null) ? new Values<>(it) : Collections.emptyIterator();
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
        return new AbstractSet<Entry<K,V>>() {
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
                final Iterator<Entry<K,V>> it = entryIterator();
                if (it == null) {
                    return Collections.emptyIterator();
                } else if (it instanceof Entry<?,?>) {
                    return new Entries<>(it);
                } else {
                    return it;
                }
            }
        };
    }

    /**
     * Returns an iterator over the entries in this map.
     * The returned iterator is not necessarily compliant to the {@code Map} contract:
     * <ul>
     *   <li>It is okay (but not required) to return {@code null} if the map is empty.</li>
     *   <li>The {@code next()} method can return the same {@code Map.Entry} instance on every call, in order to
     *       reduce the amount of objects created during iteration. However if the iterator implements such recycling,
     *       then it shall implement the {@code Entry} interface in order to notify {@code AbstractMap} about this fact.
     *       We use {@code Entry} as a marker interface because the {@code next()} method of an iterator doing such
     *       recycling will typically returns {@code this}.</li>
     * </ul>
     *
     * @return An iterator over the entries in this map, or {@code null}.
     */
    protected abstract Iterator<Entry<K,V>> entryIterator();

    /**
     * Iterator over the keys.
     */
    private static final class Keys<K,V> implements Iterator<K> {
        private final Iterator<Entry<K,V>> it;
        Keys(Iterator<Entry<K,V>> it)      {this.it = it;}
        @Override public boolean hasNext() {return it.hasNext();}
        @Override public K       next()    {return it.next().getKey();}
        @Override public void    remove()  {it.remove();}
    }

    /**
     * Iterator over the values.
     */
    private static final class Values<K,V> implements Iterator<V> {
        private final Iterator<Entry<K,V>> it;
        Values(Iterator<Entry<K,V>> it)    {this.it = it;}
        @Override public boolean hasNext() {return it.hasNext();}
        @Override public V       next()    {return it.next().getValue();}
        @Override public void    remove()  {it.remove();}
    }

    /**
     * Iterator over the entries, used only when {@link #entryIterator()} perform recycling.
     * This iterator copies each entry in an {@code SimpleImmutableEntry} instance.
     */
    private static final class Entries<K,V> implements Iterator<Entry<K,V>> {
        private final Iterator<Entry<K,V>> it;
        Entries(Iterator<Entry<K,V>> it)   {this.it = it;}
        @Override public boolean hasNext() {return it.hasNext();}
        @Override public Entry<K,V> next() {return new java.util.AbstractMap.SimpleImmutableEntry<>(it.next());}
        @Override public void    remove()  {it.remove();}
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
        if (object instanceof Map) {
            final Map<?,?> map = (Map<?,?>) object;
            final Iterator<Entry<K,V>> it = entryIterator();
            if (it == null) {
                return map.isEmpty();
            }
            /*
             * We do not check if map.size() == size() because in some Apache SIS implementations,
             * the size() method have to scan through all map entries. We presume that if the maps
             * are not equal, we will find a mismatched entry soon anyway.
             */
            int size = 0;
            while (it.hasNext()) {
                final Entry<K,V> entry = it.next();
                if (!entry.getValue().equals(map.get(entry.getKey()))) {
                    return false;
                }
                size++;
            }
            return size == map.size();
        }
        return false;
    }

    /**
     * Computes a hash code value for this map.
     *
     * @return A hash code value.
     */
    @Override
    public int hashCode() {
        int code = 0;
        final Iterator<Entry<K,V>> it = entryIterator();
        if (it != null) while (it.hasNext()) {
            code += it.next().hashCode();
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
        final Iterator<Entry<K,V>> it = entryIterator();
        if (it != null) while (it.hasNext()) {
            final Entry<K,V> entry = it.next();
            buffer.append(String.valueOf(entry.getKey()));
            buffer.nextColumn();
            buffer.append(AbstractMapEntry.firstLine(entry.getValue()));
            buffer.nextLine();
        }
        return buffer.toString();
    }
}
