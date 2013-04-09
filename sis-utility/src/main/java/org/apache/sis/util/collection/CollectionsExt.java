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
import org.apache.sis.util.Static;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverter;


/**
 * Static methods working on {@link Collection} objects.
 * This is an extension to the standard {@link Collections} utility class.
 * Some worthy methods are:
 *
 * <ul>
 *   <li>Null-safe {@link #isNullOrEmpty(Collection) isNullOrEmpty} method,
 *       for the convenience of classes using the <cite>lazy instantiation</cite> pattern.</li>
 *   <li>{@link #toCollection(Object) toCollection} for wrapping or copying arbitrary objects to
 *       list or collection.</li>
 *   <li>List and sorted set {@linkplain #listComparator() comparators}.</li>
 *   <li>{@link #modifiableCopy(Collection) modifiableCopy} method for taking a snapshot of an arbitrary
 *       implementation into an unsynchronized, modifiable, in-memory object.</li>
 *   <li>{@link #unmodifiableOrCopy(Set) unmodifiableOrCopy} methods, which may be slightly more
 *       compact than the standard {@link Collections#unmodifiableSet(Set)} equivalent
 *       when the unmodifiable collection is not required to be a view over the original collection.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
public final class CollectionsExt extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private CollectionsExt() {
    }

    /**
     * Returns {@code true} if the given collection is either null or
     * {@linkplain Collection#isEmpty() empty}. If this method returns {@code false},
     * then the given collection is guaranteed to be non-null and to contain at least
     * one element.
     *
     * <p>This is a convenience method for classes implementing the <cite>lazy instantiation</cite>
     * pattern. In such cases, null collections (i.e. collections not yet instantiated) are typically
     * considered as {@linkplain Collection#isEmpty() empty}.</p>
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
     * <p>This is a convenience method for classes implementing the <cite>lazy instantiation</cite>
     * pattern. In such cases, null maps (i.e. maps not yet instantiated) are typically considered
     * as {@linkplain Map#isEmpty() empty}.</p>
     *
     * @param map The map to test, or {@code null}.
     * @return {@code true} if the given map is null or empty, or {@code false} otherwise.
     */
    public static boolean isNullOrEmpty(final Map<?,?> map) {
        return (map == null) || map.isEmpty();
    }

    /**
     * Returns a {@linkplain Queue queue} which is always empty and accepts no element.
     *
     * @param <E> The type of elements in the empty collection.
     * @return An empty collection.
     *
     * @see Collections#emptyList()
     * @see Collections#emptySet()
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
     * @see Collections#emptyList()
     * @see Collections#emptySet()
     */
    @SuppressWarnings({"unchecked","rawtype"})
    public static <E> SortedSet<E> emptySortedSet() {
        return EmptySortedSet.INSTANCE;
    }

    /**
     * Returns a set whose elements are derived <cite>on-the-fly</cite> from the given set.
     * Conversions from the original elements to the derived elements are performed when needed
     * by invoking the {@link ObjectConverter#convert(Object)} method on the given converter.
     * Those conversions are repeated every time a {@code Set} method is invoked; there is no cache.
     * Consequently, any change in the original set is immediately visible in the derived set,
     * and conversely.
     *
     * <p>The {@link Set#add(Object) Set.add(E)} method is supported only if the given converter
     * is {@linkplain org.apache.sis.math.FunctionProperty#INVERTIBLE invertible}.
     * An invertible converter is not mandatory for other {@code Set} operations.
     * However {@link Set#contains(Object) contains} and {@link Set#remove(Object) remove}
     * operations are likely to be faster if the inverse converter is available.</p>
     *
     * <p>The derived set may contain fewer elements than the original set if some elements
     * are not convertible. Non-convertible elements are <var>S</var> values for which
     * {@code converter.convert(S)} returns {@code null}. As a consequence of this sentinel
     * value usage, the derived set can not contain {@code null} elements.</p>
     *
     * <p>The returned set can be serialized if the given set and converter are serializable.
     * The returned set is not synchronized by itself, but is nevertheless thread-safe if the
     * given set (including its iterator) and converter are thread-safe.</p>
     *
     * @param  <S>       The type of elements in the storage (original) set.
     * @param  <E>       The type of elements in the derived set.
     * @param  storage   The storage set containing the original elements, or {@code null}.
     * @param  converter The converter from the elements in the storage set to the elements
     *                   in the derived set.
     * @return A view over the {@code storage} set containing all elements converted by the given
     *         converter, or {@code null} if {@code storage} was null.
     *
     * @see org.apache.sis.util.ObjectConverters#derivedSet(Set, ObjectConverter)
     *
     * @category converter
     */
    public static <S,E> Set<E> derivedSet(final Set<S> storage, final ObjectConverter<S,E> converter) {
        ArgumentChecks.ensureNonNull("converter", converter);
        if (storage == null) {
            return null;
        }
        return DerivedSet.create(storage, converter);
    }

    /**
     * Returns a map whose whose keys and values are derived <cite>on-the-fly</cite> from the given map.
     * Conversions from the original entries to the derived entries are performed when needed
     * by invoking the {@link ObjectConverter#convert(Object)} method on the given converters.
     * Those conversions are repeated every time a {@code Map} method is invoked; there is no cache.
     * Consequently, any change in the original map is immediately visible in the derived map,
     * and conversely.
     *
     * <p>The {@link Map#put(Object,Object) Map.put(K,V)} method is supported only if the given
     * converters are {@linkplain org.apache.sis.math.FunctionProperty#INVERTIBLE invertible}.
     * An invertible converter is not mandatory for other {@code Map} operations.
     * However some of them are likely to be faster if the inverse converters are available.</p>
     *
     * <p>The derived map may contain fewer entries than the original map if some keys
     * are not convertible. Non-convertible keys are <var>K</var> values for which
     * {@code keyConverter.convert(K)} returns {@code null}. As a consequence of this sentinel
     * value usage, the derived map can not contain {@code null} keys.
     * It may contain {@code null} values however.</p>
     *
     * <p>The returned map can be serialized if the given map and converters are serializable.
     * The returned map is <strong>not</strong> thread-safe.</p>
     *
     * @param <SK>         The type of keys   in the storage map.
     * @param <SV>         The type of values in the storage map.
     * @param <K>          The type of keys   in the derived map.
     * @param <V>          The type of values in the derived map.
     * @param storage      The storage map containing the original entries, or {@code null}.
     * @param keyConverter The converter from the keys in the storage map to the keys in the derived map.
     * @param valueConverter The converter from the values in the storage map to the values in the derived map.
     * @return A view over the {@code storage} map containing all entries converted by the given
     *         converters, or {@code null} if {@code storage} was null.
     *
     * @see org.apache.sis.util.ObjectConverters#derivedMap(Map, ObjectConverter, ObjectConverter)
     * @see org.apache.sis.util.ObjectConverters#derivedKeys(Map, ObjectConverter, Class)
     * @see org.apache.sis.util.ObjectConverters#derivedValues(Map, Class, ObjectConverter)
     *
     * @category converter
     */
    public static <SK,SV,K,V> Map<K,V> derivedMap(final Map<SK,SV> storage,
                                                  final ObjectConverter<SK,K> keyConverter,
                                                  final ObjectConverter<SV,V> valueConverter)
    {
        ArgumentChecks.ensureNonNull("keyConverter",   keyConverter);
        ArgumentChecks.ensureNonNull("valueConverter", valueConverter);
        if (storage == null) {
            return null;
        }
        return DerivedMap.create(storage, keyConverter, valueConverter);
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
     * @see Collections#unmodifiableSet(Set)
     *
     * @category converter
     */
    @SafeVarargs
    public static <E> Set<E> immutableSet(final E... array) {
        if (array == null) {
            return null;
        }
        switch (array.length) {
            case 0:  return Collections.emptySet();
            case 1:  return Collections.singleton(array[0]);
            default: return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(array)));
        }
    }

    /**
     * Returns a unmodifiable version of the given set.
     * This method is different than the standard {@link Collections#unmodifiableSet(Set)}
     * in that it tries to returns a more efficient object when there is zero or one element.
     * Such small set occurs frequently in Apache SIS, especially for
     * {@link org.apache.sis.referencing.AbstractIdentifiedObject} names or identifiers.
     *
     * <p><em>The set returned by this method may or may not be a view of the given set</em>.
     * Consequently this method shall be used <strong>only</strong> if the given set will
     * <strong>not</strong> be modified after this method call. In case of doubt, use the
     * standard {@link Collections#unmodifiableSet(Set)} method instead.</p>
     *
     * @param  <E>  The type of elements in the set.
     * @param  set  The set to make unmodifiable, or {@code null}.
     * @return A unmodifiable version of the given set, or {@code null} if the given set was null.
     *
     * @category converter
     */
    public static <E> Set<E> unmodifiableOrCopy(Set<E> set) {
        if (set != null) {
            switch (set.size()) {
                case 0: {
                    set = Collections.emptySet();
                    break;
                }
                case 1: {
                    set = Collections.singleton(set.iterator().next());
                    break;
                }
                default: {
                    set = Collections.unmodifiableSet(set);
                    break;
                }
            }
        }
        return set;
    }

    /**
     * Returns a unmodifiable version of the given map.
     * This method is different than the standard {@link Collections#unmodifiableMap(Map)}
     * in that it tries to returns a more efficient object when there is zero or one entry.
     * Such small maps occur frequently in Apache SIS.
     *
     * <p><em>The map returned by this method may or may not be a view of the given map</em>.
     * Consequently this method shall be used <strong>only</strong> if the given map will
     * <strong>not</strong> be modified after this method call. In case of doubt, use the
     * standard {@link Collections#unmodifiableMap(Map)} method instead.</p>
     *
     * @param  <K>  The type of keys in the map.
     * @param  <V>  The type of values in the map.
     * @param  map  The map to make unmodifiable, or {@code null}.
     * @return A unmodifiable version of the given map, or {@code null} if the given map was null.
     *
     * @category converter
     */
    public static <K,V> Map<K,V> unmodifiableOrCopy(Map<K,V> map) {
        if (map != null) {
            switch (map.size()) {
                case 0: {
                    map = Collections.emptyMap();
                    break;
                }
                case 1: {
                    final Map.Entry<K,V> entry = map.entrySet().iterator().next();
                    map = Collections.singletonMap(entry.getKey(), entry.getValue());
                    break;
                }
                default: {
                    map = Collections.unmodifiableMap(map);
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
     *
     * @category converter
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
                return new TreeSet<>(collection);
            }
            if (type == HashSet.class || type == LinkedHashSet.class) {
                return (Collection<E>) ((HashSet<E>) collection).clone();
            }
            return new LinkedHashSet<>(collection);
        }
        if (collection instanceof Queue<?>) {
            if (type == LinkedList.class) {
                return (Collection<E>) ((LinkedList<E>) collection).clone();
            }
            return new LinkedList<>(collection);
        }
        if (type == ArrayList.class) {
            return (Collection<E>) ((ArrayList<E>) collection).clone();
        }
        return new ArrayList<>(collection);
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
     *
     * @category converter
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
            return new TreeMap<>(map);
        }
        if (type == HashMap.class || type == LinkedHashMap.class) {
            return (Map<K,V>) ((HashMap<K,V>) map).clone();
        }
        return new LinkedHashMap<>(map);
    }

    /**
     * Returns the given value as a collection. Special cases:
     *
     * <ul>
     *   <li>If the value is null, then this method returns an {@linkplain Collections#emptyList() empty list}.</li>
     *   <li>If the value is an instance of {@link Collection}, then it is returned unchanged.</li>
     *   <li>If the value is an array of objects, then it is returned {@linkplain Arrays#asList(Object[]) as a list}.</li>
     *   <li>If the value is an instance of {@link Iterable}, {@link Iterator} or {@link Enumeration}, copies the values in a new list.</li>
     *   <li>Otherwise the value is returned as a {@linkplain Collections#singletonList(Object) singleton list}.</li>
     * </ul>
     *
     * <p>Note that in the {@link Iterator} and {@link Enumeration} cases, the given value object
     * is not valid anymore after this method call since it has been used for the iteration.</p>
     *
     * <p>If the returned object needs to be a list, then this method can be chained
     * with {@link #toList(Collection)} as below:</p>
     *
     * {@preformat java
     *     List<?> list = toList(toCollection(object));
     * }
     *
     * @param  value The value to return as a collection, or {@code null}.
     * @return The value as a collection, or wrapped in a collection (never {@code null}).
     *
     * @category converter
     */
    public static Collection<?> toCollection(final Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof Collection<?>) {
            return (Collection<?>) value;
        }
        if (value instanceof Object[]) {
            return Arrays.asList((Object[]) value);
        }
        if (value instanceof Iterable<?>) {
            final List<Object> list = new ArrayList<>();
            for (final Object element : (Iterable<?>) value) {
                list.add(element);
            }
            return list;
        }
        if (value instanceof Iterator<?>) {
            final Iterator<?> it = (Iterator<?>) value;
            final List<Object> list = new ArrayList<>();
            while (it.hasNext()) {
                list.add(it.next());
            }
            return list;
        }
        if (value instanceof Enumeration<?>) {
            return Collections.list((Enumeration<?>) value);
        }
        return Collections.singletonList(value);
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
     * This method can be chained with {@link #toCollection(Object)}
     * for handling a wider range of types:
     *
     * {@preformat java
     *     List<?> list = toList(toCollection(object));
     * }
     *
     * @param  <T> The type of elements in the given collection.
     * @param  collection The collection to cast or copy to a list.
     * @return The given collection as a list, or a copy of the given collection.
     *
     * @category converter
     */
    public static <T> List<T> toList(final Collection<T> collection) {
        if (collection instanceof List<?>) {
            return (List<T>) collection;
        }
        return new ArrayList<>(collection);
    }

    /**
     * The comparator to be returned by {@link Collections#listComparator()} and similar methods.
     */
    private static final class Compare<T extends Comparable<T>>
            implements Comparator<Collection<T>>, Serializable
    {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 7050753365408754641L;

        /**
         * The unique instance. Can not be public because of parameterized types: we need a method
         * for casting to the expected type. This is the same trick than the one used by the JDK
         * in the {@link Collections#emptySet()} method for instance.
         */
        @SuppressWarnings("rawtypes")
        static final Comparator INSTANCE = new Compare();

        /**
         * Do not allow instantiation other than the unique {@link #INSTANCE}.
         */
        private Compare() {
        }

        /**
         * Compares two collections of comparable objects.
         */
        @Override
        public int compare(final Collection<T> c1, final Collection<T> c2) {
            final Iterator<T> i1 = c1.iterator();
            final Iterator<T> i2 = c2.iterator();
            int c;
            do {
                final boolean h1 = i1.hasNext();
                final boolean h2 = i2.hasNext();
                if (!h1) return h2 ? -1 : 0;
                if (!h2) return +1;
                final T e1 = i1.next();
                final T e2 = i2.next();
                c = e1.compareTo(e2);
            } while (c == 0);
            return c;
        }
    };

    /**
     * Returns a comparator for lists of comparable elements. The first element of each list are
     * {@linkplain Comparable#compareTo(Object) compared}. If one is <cite>greater than</cite> or
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
     *
     * @category comparator
     */
    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> Comparator<List<T>> listComparator() {
        return Compare.INSTANCE;
    }

    /**
     * Returns a comparator for sorted sets of comparable elements. The first element of each set
     * are {@linkplain Comparable#compareTo(Object) compared}. If one is <cite>greater than</cite>
     * or <cite>less than</cite> the other, the result of that comparison is returned. Otherwise
     * the second element are compared, and so on until either non-equal elements are found,
     * or end-of-set are reached. In the later case, the smallest set is considered
     * <cite>less than</cite> the largest one.
     *
     * {@note There is no method accepting an arbitrary <code>Set</code> or <code>Collection</code>
     *        argument because this comparator makes sense only for collections having determinist
     *        iteration order.}
     *
     * @param <T> The type of elements in both sets.
     * @return The ordering between two sets.
     *
     * @category comparator
     */
    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> Comparator<SortedSet<T>> sortedSetComparator() {
        return Compare.INSTANCE;
    }

    /**
     * The comparator to be returned by {@link Collections#valueComparator()}.
     */
    private static final class ValueComparator<K,V extends Comparable<V>>
            implements Comparator<Map.Entry<K,V>>, Serializable
    {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = 807166038568740444L;

        /**
         * The unique instance. Can not be public because of parameterized types: we need a method
         * for casting to the expected type. This is the same trick than the one used by the JDK
         * in the {@link Collections#emptySet()} method for instance.
         */
        @SuppressWarnings("rawtypes")
        static final ValueComparator INSTANCE = new ValueComparator();

        /**
         * Do not allow instantiation other than the unique {@link #INSTANCE}.
         */
        private ValueComparator() {
        }

        /**
         * Compares the values of two entries.
         */
        @Override
        public int compare(final Map.Entry<K,V> e1, final Map.Entry<K,V> e2) {
            return e1.getValue().compareTo(e2.getValue());
        }
    }

    /**
     * Returns a comparator for map entries having comparable {@linkplain java.util.Map.Entry#getValue() values}.
     * For any pair of entries {@code e1} and {@code e2}, this method performs the comparison as below:
     *
     * {@preformat java
     *     return e1.getValue().compareTo(e2.getValue());
     * }
     *
     * This comparator can be used as a complement to {@link SortedSet}. While {@code SortedSet}
     * maintains keys ordering at all time, {@code valueComparator()} is typically used only at
     * the end of a process in which the values are the numerical calculation results.
     *
     * @param <K> The type of keys in the map entries.
     * @param <V> The type of values in the map entries.
     * @return A comparator for the values of the given type.
     *
     * @category comparator
     */
    @SuppressWarnings("unchecked")
    public static <K,V extends Comparable<V>> Comparator<Map.Entry<K,V>> valueComparator() {
        return ValueComparator.INSTANCE;
    }

    /**
     * Returns the capacity to be given to the {@link HashMap#HashMap(int) HashMap}
     * constructor for holding the given number of elements. This method computes the capacity
     * for the default <cite>load factor</cite>, which is 0.75.
     *
     * <p>The same calculation can be used for {@link LinkedHashMap} and
     * {@link HashSet} as well, which are built on top of {@code HashMap}.</p>
     *
     * @param count The number of elements to be put into the hash map or hash set.
     * @return The minimal initial capacity to be given to the hash map constructor.
     */
    public static int hashMapCapacity(final int count) {
        int r = count >>> 2;
        if ((count & 0x3) != 0) {
            r++;
        }
        return count + r;
    }
}
