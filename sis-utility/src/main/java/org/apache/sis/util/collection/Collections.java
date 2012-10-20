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

import java.util.*;
import java.io.Serializable;
import java.util.logging.Logger;
import org.apache.sis.util.Static;
import org.apache.sis.util.logging.Logging;

import static java.util.Collections.list;
import static java.util.Collections.emptySet;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Collections.unmodifiableMap;


/**
 * Static methods working on {@link Collection} objects.
 * This is an extension to the Java {@link java.util.Collections} utility class providing:
 *
 * <ul>
 *   <li>Null-safe {@link #clear(Collection) clear}, {@link #isNullOrEmpty(Collection) isNullOrEmpty}
 *       and {@link #addIfNonNull(Collection, Object) addIfNonNull} methods, for the convenience of
 *       classes using the <cite>lazy instantiation</cite> pattern.</li>
 *   <li>{@link #asCollection(Object) asCollection} for wrapping arbitrary objects to list or collection.</li>
 *   <li>List and collection {@linkplain #listComparator() comparators}.</li>
 *   <li>{@link #modifiableCopy(Collection) modifiableCopy} method for taking a snapshot of an arbitrary
 *       implementation into an unsynchronized, modifiable, in-memory object.</li>
 *   <li>{@link #unmodifiableOrCopy(Set) unmodifiableOrCopy} methods, which may be slightly more
 *       compact than the standard {@link java.util.Collections#unmodifiableSet(Set)} equivalent
 *       when the unmodifiable collection is not required to be a view over the original collection.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
public final class Collections extends Static {
    /**
     * The logger where to logs collection events, if logging at the finest level is enabled.
     */
    static final Logger LOGGER = Logging.getLogger(Collections.class);

    /**
     * Do not allow instantiation of this class.
     */
    private Collections() {
    }

    /**
     * Clears the given collection, if non-null.
     * If the given collection is null, then this method does nothing.
     *
     * <p>This is a convenience method for classes implementing the <cite>lazy instantiation</cite>
     * pattern. In such cases, null collections (i.e. collections not yet instantiated) are typically
     * considered as {@linkplain Collection#isEmpty() empty}.</p>
     *
     * @param collection The collection to clear, or {@code null}.
     */
    public static void clear(final Collection<?> collection) {
        if (collection != null) {
            collection.clear();
        }
    }

    /**
     * Clears the given map, if non-null.
     * If the given map is null, then this method does nothing.
     *
     * <p>This is a convenience method for classes implementing the <cite>lazy instantiation</cite>
     * pattern. In such cases, null maps (i.e. maps not yet instantiated) are typically considered
     * as {@linkplain Map#isEmpty() empty}.</p>
     *
     * @param map The map to clear, or {@code null}.
     */
    public static void clear(final Map<?,?> map) {
        if (map != null) {
            map.clear();
        }
    }

    /**
     * Returns {@code true} if the given collection is either null or
     * {@linkplain Collection#isEmpty() empty}. If this method returns {@code false},
     * then the given collection is guaranteed to be non-null and to contain at least
     * one element.
     *
     * @param collection The collection to test, or {@code null}.
     * @return {@code true} if the given collection is null or empty, or {@code false} otherwise.
     */
    public static boolean isNullOrEmpty(final Collection<?> collection) {
        return (collection == null) || collection.isEmpty();
    }

    /**
     * Returns {@code true} if the given map is either null or {@linkplain Map#isEmpty() empty}.
     * If this method returns {@code false}, then the given map is guaranteed to be non-null and
     * to contain at least one element.
     *
     * @param map The map to test, or {@code null}.
     * @return {@code true} if the given map is null or empty, or {@code false} otherwise.
     */
    public static boolean isNullOrEmpty(final Map<?,?> map) {
        return (map == null) || map.isEmpty();
    }

    /**
     * Adds the given element to the given collection only if the element is non-null.
     * If any of the given argument is null, then this method does nothing.
     *
     * @param  <E>        The type of elements in the collection.
     * @param  collection The collection in which to add elements, or {@code null}.
     * @param  element    The element to add in the collection, or {@code null}.
     * @return {@code true} if the given element has been added, or {@code false} otherwise.
     */
    public static <E> boolean addIfNonNull(final Collection<E> collection, final E element) {
        return (collection != null && element != null) && collection.add(element);
    }

    /**
     * Returns a {@linkplain Queue queue} which is always empty and accepts no element.
     *
     * @param <E> The type of elements in the empty collection.
     * @return An empty collection.
     *
     * @see java.util.Collections#emptyList()
     * @see java.util.Collections#emptySet()
     */
    @SuppressWarnings({"unchecked","rawtype"})
    public static <E> Queue<E> emptyQueue() {
        return EmptyQueue.INSTANCE;
    }

    /**
     * Returns a {@linkplain SortedSet sorted set} which is always empty and accepts no element.
     *
     * {@note This method exists only on the JDK6 and JDK7 branches. This method will
     *        be removed from the JDK8 branch, since it has been added to the JDK.}
     *
     * @param <E> The type of elements in the empty collection.
     * @return An empty collection.
     *
     * @see java.util.Collections#emptyList()
     * @see java.util.Collections#emptySet()
     */
    @SuppressWarnings({"unchecked","rawtype"})
    public static <E> SortedSet<E> emptySortedSet() {
        return EmptySortedSet.INSTANCE;
    }

    /**
     * Returns the specified array as an immutable set, or {@code null} if the array is null.
     * If the given array contains duplicated elements, i.e. elements that are equal in the
     * sense of {@link Object#equals(Object)}, then only the last instance of the duplicated
     * values will be included in the returned set.
     *
     * @param  <E> The type of array elements.
     * @param  array The array to copy in a set. May be {@code null}.
     * @return A set containing the array elements, or {@code null} if the given array was null.
     *
     * @see java.util.Collections#unmodifiableSet(Set)
     */
    public static <E> Set<E> immutableSet(final E... array) {
        if (array == null) {
            return null;
        }
        switch (array.length) {
            case 0:  return emptySet();
            case 1:  return singleton(array[0]);
            default: return unmodifiableSet(new LinkedHashSet<E>(Arrays.asList(array)));
        }
    }

    /**
     * Returns a unmodifiable version of the given set.
     * This method is different than the standard {@link java.util.Collections#unmodifiableSet(Set)}
     * in that it tries to returns a more efficient object when there is zero or one element.
     * Such small set occurs frequently in Apache SIS, especially for
     * {@link org.apache.sis.referencing.AbstractIdentifiedObject} names or identifiers.
     *
     * <p><em>The set returned by this method may or may not be a view of the given set</em>.
     * Consequently this method shall be used <strong>only</strong> if the given set will
     * <strong>not</strong> be modified after this method call. In case of doubt, use the
     * standard {@link java.util.Collections#unmodifiableSet(Set)} method instead.</p>
     *
     * @param  <E>  The type of elements in the set.
     * @param  set  The set to make unmodifiable, or {@code null}.
     * @return A unmodifiable version of the given set, or {@code null} if the given set was null.
     */
    public static <E> Set<E> unmodifiableOrCopy(Set<E> set) {
        if (set != null) {
            switch (set.size()) {
                case 0: {
                    set = emptySet();
                    break;
                }
                case 1: {
                    set = singleton(set.iterator().next());
                    break;
                }
                default: {
                    set = unmodifiableSet(set);
                    break;
                }
            }
        }
        return set;
    }

    /**
     * Returns a unmodifiable version of the given map.
     * This method is different than the standard {@link java.util.Collections#unmodifiableMap(Map)}
     * in that it tries to returns a more efficient object when there is zero or one entry.
     * Such small maps occur frequently in Apache SIS.
     *
     * <p><em>The map returned by this method may or may not be a view of the given map</em>.
     * Consequently this method shall be used <strong>only</strong> if the given map will
     * <strong>not</strong> be modified after this method call. In case of doubt, use the
     * standard {@link java.util.Collections#unmodifiableMap(Map)} method instead.</p>
     *
     * @param  <K>  The type of keys in the map.
     * @param  <V>  The type of values in the map.
     * @param  map  The map to make unmodifiable, or {@code null}.
     * @return A unmodifiable version of the given map, or {@code null} if the given map was null.
     */
    public static <K,V> Map<K,V> unmodifiableOrCopy(Map<K,V> map) {
        if (map != null) {
            switch (map.size()) {
                case 0: {
                    map = emptyMap();
                    break;
                }
                case 1: {
                    final Map.Entry<K,V> entry = map.entrySet().iterator().next();
                    map = singletonMap(entry.getKey(), entry.getValue());
                    break;
                }
                default: {
                    map = unmodifiableMap(map);
                    break;
                }
            }
        }
        return map;
    }

    /**
     * Copies the content of the given collection to a new, unsynchronized, modifiable, in-memory
     * collection. The implementation class of the returned collection may be different than the
     * class of the collection given in argument. The following table gives the types mapping
     * applied by this method:
     *
     * <table class="sis">
     * <tr><th>Input type</th>                              <th class="sep">Output type</th></tr>
     * <tr><td>{@link SortedSet}</td>                       <td class="sep">{@link TreeSet}</td></tr>
     * <tr><td>{@link HashSet}</td>                         <td class="sep">{@link HashSet}</td></tr>
     * <tr><td>{@link Set} other than above</td>            <td class="sep">{@link LinkedHashSet}</td></tr>
     * <tr><td>{@link Queue}</td>                           <td class="sep">{@link LinkedList}</td></tr>
     * <tr><td>{@link List} or other {@link Collection}</td><td class="sep">{@link ArrayList}</td></tr>
     * </table>
     *
     * @param  <E> The type of elements in the collection.
     * @param  collection The collection to copy, or {@code null}.
     * @return A copy of the given collection, or {@code null} if the given collection was null.
     */
    @SuppressWarnings("unchecked")
    public static <E> Collection<E> modifiableCopy(final Collection<E> collection) {
        if (collection == null) {
            return null;
        }
        /*
         * We will use the clone() method when possible because they are
         * implemented in a more efficient way than the copy constructors.
         */
        final Class<?> type = collection.getClass();
        if (collection instanceof Set<?>) {
            if (collection instanceof SortedSet<?>) {
                if (type == TreeSet.class) {
                    return (Collection<E>) ((TreeSet<E>) collection).clone();
                }
                return new TreeSet<E>(collection);
            }
            if (type == HashSet.class || type == LinkedHashSet.class) {
                return (Collection<E>) ((HashSet<E>) collection).clone();
            }
            return new LinkedHashSet<E>(collection);
        }
        if (collection instanceof Queue<?>) {
            if (type == LinkedList.class) {
                return (Collection<E>) ((LinkedList<E>) collection).clone();
            }
            return new LinkedList<E>(collection);
        }
        if (type == ArrayList.class) {
            return (Collection<E>) ((ArrayList<E>) collection).clone();
        }
        return new ArrayList<E>(collection);
    }

    /**
     * Copies the content of the given map to a new unsynchronized, modifiable, in-memory map.
     * The implementation class of the returned map may be different than the class of the map
     * given in argument. The following table gives the types mapping applied by this method:
     *
     * <table class="sis">
     * <tr><th>Input type</th>                  <th class="sep">Output type</th></tr>
     * <tr><td>{@link SortedMap}</td>           <td class="sep">{@link TreeMap}</td></tr>
     * <tr><td>{@link HashMap}</td>             <td class="sep">{@link HashMap}</td></tr>
     * <tr><td>{@link Map} other than above</td><td class="sep">{@link LinkedHashMap}</td></tr>
     * </table>
     *
     * @param  <K> The type of keys in the map.
     * @param  <V> The type of values in the map.
     * @param  map The map to copy, or {@code null}.
     * @return A copy of the given map, or {@code null} if the given map was null.
     */
    @SuppressWarnings("unchecked")
    public static <K,V> Map<K,V> modifiableCopy(final Map<K,V> map) {
        if (map == null) {
            return null;
        }
        /*
         * We will use the clone() method when possible because they are
         * implemented in a more efficient way than the copy constructors.
         */
        final Class<?> type = map.getClass();
        if (map instanceof SortedMap<?,?>) {
            if (type == TreeMap.class) {
                return (Map<K,V>) ((TreeMap<K,V>) map).clone();
            }
            return new TreeMap<K,V>(map);
        }
        if (type == HashMap.class || type == LinkedHashMap.class) {
            return (Map<K,V>) ((HashMap<K,V>) map).clone();
        }
        return new LinkedHashMap<K,V>(map);
    }

    /**
     * Returns the given value as a collection. Special cases:
     *
     * <ul>
     *   <li>If the value is null, then this method returns an {@linkplain java.util.Collections#emptyList() empty list}.</li>
     *   <li>If the value is an instance of {@link Collection}, then it is returned unchanged.</li>
     *   <li>If the value is an array of objects, then it is returned {@linkplain Arrays#asList(Object[]) as a list}.</li>
     *   <li>If the value is an instance of {@link Iterable}, {@link Iterator} or {@link Enumeration}, copies the values in a new list.</li>
     *   <li>Otherwise the value is returned as a {@linkplain java.util.Collections#singletonList(Object) singleton list}.</li>
     * </ul>
     *
     * <p>Note that in the {@link Iterator} and {@link Enumeration} cases, the given value object
     * is not valid anymore after this method call since it has been used for the iteration.</p>
     *
     * <p>If the returned object needs to be a list, then this method can be chained
     * with {@link #asList(Collection)} as below:</p>
     *
     * {@preformat java
     *     List<?> list = asList(asCollection(object));
     * }
     *
     * @param  value The value to return as a collection, or {@code null}.
     * @return The value as a collection, or wrapped in a collection (never {@code null}).
     */
    public static Collection<?> asCollection(final Object value) {
        if (value == null) {
            return emptyList();
        }
        if (value instanceof Collection<?>) {
            return (Collection<?>) value;
        }
        if (value instanceof Object[]) {
            return Arrays.asList((Object[]) value);
        }
        if (value instanceof Iterable<?>) {
            final List<Object> list = new ArrayList<Object>();
            for (final Object element : (Iterable<?>) value) {
                list.add(element);
            }
            return list;
        }
        if (value instanceof Iterator<?>) {
            final Iterator<?> it = (Iterator<?>) value;
            final List<Object> list = new ArrayList<Object>();
            while (it.hasNext()) {
                list.add(it.next());
            }
            return list;
        }
        if (value instanceof Enumeration<?>) {
            return list((Enumeration<?>) value);
        }
        return singletonList(value);
    }

    /**
     * Casts or copies the given collection to a list. Special cases:
     *
     * <ul>
     *   <li>If the given collection is {@code null}, then this method returns {@code null}.</li>
     *   <li>If the given collection is already a list, then it is returned unchanged.</li>
     *   <li>Otherwise the elements are copied in a new list, which is returned.</li>
     * </ul>
     *
     * This method can be chained with {@link #asCollection(Object)}
     * for handling a wider range of types:
     *
     * {@preformat java
     *     List<?> list = asList(asCollection(object));
     * }
     *
     * @param  <T> The type of elements in the given collection.
     * @param  collection The collection to cast or copy to a list.
     * @return The given collection as a list, or a copy of the given collection.
     */
    public static <T> List<T> asList(final Collection<T> collection) {
        if (collection instanceof List<?>) {
            return (List<T>) collection;
        }
        return new ArrayList<T>(collection);
    }

    /**
     * The comparator to be returned by {@code #listComparator} and similar methods. Can not be
     * public because of parameterized types: we need a method for casting to the expected type.
     * This is the same trick than {@link Collections#emptySet()} for example.
     */
    @SuppressWarnings("rawtypes")
    private static final class Compare implements Comparator<Collection<Comparable>>, Serializable {
        /**
         * The unique instance.
         */
        static final Comparator<Collection<Comparable>> INSTANCE = new Compare();

        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -8926770873102046405L;

        /**
         * Compares to collections of comparable objects.
         */
        @Override
        @SuppressWarnings("unchecked")
        public int compare(final Collection<Comparable> c1, final Collection<Comparable> c2) {
            final Iterator<Comparable> i1 = c1.iterator();
            final Iterator<Comparable> i2 = c2.iterator();
            int c;
            do {
                final boolean h1 = i1.hasNext();
                final boolean h2 = i2.hasNext();
                if (!h1) return h2 ? -1 : 0;
                if (!h2) return +1;
                final Comparable e1 = i1.next();
                final Comparable e2 = i2.next();
                c = e1.compareTo(e2);
            } while (c == 0);
            return c;
        }
    };

    /**
     * Returns a comparator for lists of comparable elements. The first element of each list
     * are {@linkplain Comparable#compareTo compared}. If one is <cite>greater than</cite> or
     * <cite>less than</cite> the other, the result of that comparison is returned. Otherwise
     * the second element are compared, and so on until either non-equal elements are found,
     * or end-of-list are reached. In the later case, the shortest list is considered
     * <cite>less than</cite> the longest one.
     *
     * <p>If both lists have the same length and equal elements in the sense of
     * {@link Comparable#compareTo}, then the comparator returns 0.</p>
     *
     * @param  <T> The type of elements in both lists.
     * @return The ordering between two lists.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public static <T extends Comparable<T>> Comparator<List<T>> listComparator() {
        return (Comparator) Compare.INSTANCE;
    }

    /**
     * Returns a comparator for sorted sets of comparable elements. The elements are compared in
     * iteration order as for the {@linkplain #listComparator list comparator}.
     *
     * @param <T> The type of elements in both sets.
     * @return The ordering between two sets.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public static <T extends Comparable<T>> Comparator<SortedSet<T>> sortedSetComparator() {
        return (Comparator) Compare.INSTANCE;
    }

    /**
     * Returns a comparator for arbitrary collections of comparable elements. The elements are
     * compared in iteration order as for the {@linkplain #listComparator list comparator}.
     *
     * <p><em>This comparator make sense only for collections having determinist order</em>
     * like {@link java.util.TreeSet}, {@link java.util.LinkedHashSet} or queues.
     * Do <strong>not</strong> use it with {@link java.util.HashSet}.</p>
     *
     * @param <T> The type of elements in both collections.
     * @return The ordering between two collections.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public static <T extends Comparable<T>> Comparator<Collection<T>> collectionComparator() {
        return (Comparator) Compare.INSTANCE;
    }

    /**
     * Returns the capacity to be given to the {@link java.util.HashMap#HashMap(int) HashMap}
     * constructor for holding the given number of elements. This method computes the capacity
     * for the default <cite>load factor</cite>, which is 0.75.
     *
     * <p>The same calculation can be used for {@link java.util.LinkedHashMap} and
     * {@link java.util.HashSet} as well, which are built on top of {@code HashMap}.</p>
     *
     * @param elements The number of elements to be put into the hash map or hash set.
     * @return The optimal initial capacity to be given to the hash map constructor.
     */
    public static int hashMapCapacity(int elements) {
        final int r = elements >>> 2;
        if (elements != (r << 2)) {
            elements++;
        }
        return elements + r;
    }
}
