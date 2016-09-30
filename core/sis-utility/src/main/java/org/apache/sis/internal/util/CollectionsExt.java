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

import java.util.*;
import java.lang.reflect.Array;
import org.opengis.util.CodeList;
import org.apache.sis.util.Static;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.collection.CodeListSet;
import org.apache.sis.util.resources.Errors;
import org.opengis.parameter.InvalidParameterCardinalityException;

import static org.apache.sis.util.collection.Containers.hashMapCapacity;


/**
 * Static methods working on {@link Collection} objects.
 * This is an extension to the standard {@link Collections} utility class.
 * Some worthy methods are:
 *
 * <ul>
 *   <li>{@link #toCollection(Object)} for wrapping or copying arbitrary objects to list or collection.</li>
 *   <li>{@link #modifiableCopy(Collection)} method for taking a snapshot of an arbitrary implementation
 *       into an unsynchronized, modifiable, in-memory object.</li>
 *   <li>{@link #unmodifiableOrCopy(Set)} methods, which may be slightly more compact than the standard
 *       {@link Collections#unmodifiableSet(Set)} equivalent when the unmodifiable collection is not required
 *       to be a view over the original collection.</li>
 * </ul>
 *
 * This class is not in public API because some functionality provided there are not really the
 * purpose of a geospatial library and may change at any time. Some method contracts are a little
 * bit tedious to explain, which is an other indication that they should not be in public API.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.8
 * @module
 */
public final class CollectionsExt extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private CollectionsExt() {
    }

    /**
     * Returns the first element of the given iterable, or {@code null} if none.
     * This method does not emit warning if more than one element is found.
     * Consequently, this method should be used only when multi-occurrence is not ambiguous.
     *
     * <p>This method is null-safe. Note however that the first element may be null.</p>
     *
     * @param  <T>         the type of elements contained in the iterable.
     * @param  collection  the iterable from which to get the first element, or {@code null}.
     * @return the first element, or {@code null} if the given iterable is null or empty.
     */
    public static <T> T first(final Iterable<T> collection) {
        if (collection != null) {
            final Iterator<T> it = collection.iterator();
            if (it != null && it.hasNext()) {                       // This check for null is paranoiac.
                return it.next();
            }
        }
        return null;
    }

    /**
     * If the given iterable contains exactly one non-null element, returns that element.
     * Otherwise returns {@code null}.
     *
     * @param  <T>         the type of elements contained in the iterable.
     * @param  collection  the iterable from which to get the singleton element, or {@code null}.
     * @return the singleton element, or {@code null} if the given iterable is null or does not
     *         contain exactly one non-null element.
     *
     * @since 0.8
     */
    public static <T> T singletonOrNull(final Iterable<T> collection) {
        if (collection != null) {
            final Iterator<T> it = collection.iterator();
            if (it != null) {                                       // This check for null is paranoiac.
                T element = null;
                while (it.hasNext()) {
                    final T next = it.next();
                    if (next != null) {
                        if (element != null) {
                            return null;
                        }
                        element = next;
                    }
                }
                return element;
            }
        }
        return null;
    }

    /**
     * Returns a {@linkplain Queue queue} which is always empty and accepts no element.
     *
     * @param  <E>  the type of elements in the empty collection.
     * @return an empty collection.
     *
     * @see Collections#emptyList()
     * @see Collections#emptySet()
     */
    @SuppressWarnings({"unchecked","rawtype"})
    public static <E> Queue<E> emptyQueue() {
        return EmptyQueue.INSTANCE;
    }

    /**
     * Returns the given value as a singleton if non-null, or returns an empty set otherwise.
     *
     * @param  <E>      the element type.
     * @param  element  the element to returns in a collection if non-null.
     * @return a collection containing the given element if non-null, or an empty collection otherwise.
     */
    public static <E> Set<E> singletonOrEmpty(final E element) {
        return (element != null) ? Collections.singleton(element) : Collections.emptySet();
    }

    /**
     * Returns a copy of the given array as a non-empty immutable set.
     * If the given array is empty, then this method returns {@code null}.
     *
     * @param  <T>       the type of elements.
     * @param  elements  the elements to copy in a set.
     * @return an unmodifiable set which contains all the given elements, or {@code null}.
     *
     * @since 0.6
     */
    @SafeVarargs
    public static <T> Set<T> nonEmptySet(final T... elements) {
        final Set<T> asSet = immutableSet(true, elements);
        return (asSet != null && asSet.isEmpty()) ? null : asSet;
    }

    /**
     * Returns the given array if non-empty, or {@code null} if the given array is null or empty.
     * This method is generally not recommended, since public API should prefer empty array instead of null.
     * However this method is occasionally useful for managing private fields.
     *
     * @param  <E>    the type of elements in the array.
     * @param  array  the array, or {@code null}.
     * @return the given array, or {@code null} if the given array was empty.
     */
    public static <E> E[] nonEmpty(final E[] array) {
        return (array != null && array.length == 0) ? null : array;
    }

    /**
     * Returns the given collection if non-empty, or {@code null} if the given collection is null or empty.
     * This method is generally not recommended, since public API should prefer empty collection instead of null.
     * However this method is occasionally useful for managing private fields, especially for inter-operability
     * with frameworks that may expect or return null (e.g. if we want to exclude completely an empty collection
     * from marshalling with JAXB).
     *
     * @param  <T>  the type of the collection.
     * @param  <E>  the type of elements in the collection.
     * @param  c    the collection, or {@code null}.
     * @return the given collection, or {@code null} if the given collection was empty.
     */
    public static <T extends Collection<E>, E> T nonEmpty(final T c) {
        return (c != null && c.isEmpty()) ? null : c;
    }

    /**
     * Returns the given collection, or {@link Collections#EMPTY_SET} if the given collection is null.
     *
     * @param  <E>  the type of elements in the collection.
     * @param  c    the collection, or {@code null}.
     * @return the given collection, or an empty set if the given collection was null.
     */
    public static <E> Collection<E> nonNull(final Collection<E> c) {
        return (c != null) ? c : Collections.<E>emptySet();
    }

    /**
     * Returns the given set, or {@link Collections#EMPTY_SET} if the given set is null.
     *
     * @param  <E>  the type of elements in the collection.
     * @param  c    the collection, or {@code null}.
     * @return the given collection, or an empty set if the given collection was null.
     */
    public static <E> Set<E> nonNull(final Set<E> c) {
        return (c != null) ? c : Collections.<E>emptySet();
    }

    /**
     * Given a value which is either {@code null}, an instance of {@code <E>} or an array of {@code <E>},
     * returns a non-null array containing all elements without null and without duplicated values.
     * More specifically:
     *
     * <ul>
     *   <li>If the given value is {@code null}, then this method returns {@code emptyArray}.</li>
     *   <li>If the given value is an instance of {@code <E>}, then this method returns an array of length 1
     *       which contain only {@code value}.</li>
     *   <li>If the given value is an array of {@code <E>}, then this method returns copy of that array,
     *       omitting {@code null} elements and duplicated elements.</li>
     *   <li>Otherwise this method throws {@link IllegalArgumentException}.</li>
     * </ul>
     *
     * <div class="note"><b>Note:</b>
     * It would be very easy to add support for {@code value} argument of type {@code Object[]} or collections.
     * But we do not provide such support for now because this method is used mostly as a helper method for
     * constructors of {@code AbstractIdentifiedObject} subclasses receiving a map of properties,
     * and the contract of our constructors do not allow those other types for now.</div>
     *
     * @param  <E>         the type of elements in the array to be returned.
     * @param  name        the parameter name, used only for formatting an error message in case of failure.
     * @param  value       the value to return as an array, or {@code null}.
     * @param  emptyArray  an instance of {@code new E[0]}. This argument can not be null.
     * @return the given value as an array of {@code <E>}. Never null.
     * throws  IllegalArgumentException if the given value is not null, an instance of {@code <E>}
     *         or an array of {@code <E>}.
     *
     * @since 0.4
     */
    @SuppressWarnings("unchecked")
    public static <E> E[] nonNullArraySet(final String name, final Object value, final E[] emptyArray)
            throws IllegalArgumentException
    {
        assert emptyArray.length == 0;
        if (value == null) {
            return emptyArray;
        }
        Class<?> type = emptyArray.getClass();
        final Class<?> valueType = value.getClass();
        if (valueType.isArray()) {
            if (type.isAssignableFrom(valueType)) {
                final Set<E> set = new LinkedHashSet<>(Arrays.asList((E[]) value));
                set.remove(null);
                return set.toArray(emptyArray);
            }
        } else {
            type = type.getComponentType();
            if (type.isAssignableFrom(valueType)) {
                final E[] array = (E[]) Array.newInstance(type, 1);
                array[0] = (E) value;
                return array;
            }
        }
        throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalPropertyValueClass_2, name, valueType));
    }

    /**
     * Creates an initially empty set for elements of the given type.
     * This method will creates specialized set for code lists and enumerations.
     *
     * @param  <E>    the type of elements in the set.
     * @param  type   the type of elements in the set.
     * @param  count  the expected number of elements to put in the set.
     * @return a new set for elements of the given type.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public static <E> Set<E> createSetForType(final Class<E> type, final int count) {
        if (CodeList.class.isAssignableFrom(type)) {
            return new CodeListSet((Class) type);
        }
        if (Enum.class.isAssignableFrom(type)) {
            return EnumSet.noneOf((Class) type);
        }
        return new LinkedHashSet<>(hashMapCapacity(count));
    }

    /**
     * Returns the specified array as an immutable set, or {@code null} if the array is null.
     * If the given array contains duplicated elements, i.e. elements that are equal in the
     * sense of {@link Object#equals(Object)}, then only the last instance of the duplicated
     * values will be included in the returned set.
     *
     * @param  <E>          the type of array elements.
     * @param  excludeNull  {@code true} for excluding the {@code null} element from the returned set.
     * @param  array        the array to copy in a set. May be {@code null} or contain null elements.
     * @return a set containing the array elements, or {@code null} if the given array was null.
     *
     * @see Collections#unmodifiableSet(Set)
     */
    @SafeVarargs
    @SuppressWarnings("fallthrough")
    public static <E> Set<E> immutableSet(final boolean excludeNull, final E... array) {
        if (array == null) {
            return null;
        }
        switch (array.length) {
            case 1: {
                final E element = array[0];
                if (element != null || !excludeNull) {
                    return Collections.singleton(element);
                }
                // Fallthrough for an empty set.
            }
            case 0: {
                return Collections.emptySet();
            }
            default: {
                final Set<E> set = new LinkedHashSet<>(Arrays.asList(array));
                if (excludeNull) {
                    set.remove(null);
                }
                return unmodifiableOrCopy(set);
            }
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
     * @param  <E>  the type of elements in the set.
     * @param  set  the set to make unmodifiable, or {@code null}.
     * @return a unmodifiable version of the given set, or {@code null} if the given set was null.
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
     * @param  <K>  the type of keys in the map.
     * @param  <V>  the type of values in the map.
     * @param  map  the map to make unmodifiable, or {@code null}.
     * @return a unmodifiable version of the given map, or {@code null} if the given map was null.
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
     * <caption>Implementations for types</caption>
     * <tr><th>Input type</th>                              <th class="sep">Output type</th></tr>
     * <tr><td>{@link SortedSet}</td>                       <td class="sep">{@link TreeSet}</td></tr>
     * <tr><td>{@link HashSet}</td>                         <td class="sep">{@link HashSet}</td></tr>
     * <tr><td>{@link Set} other than above</td>            <td class="sep">{@link LinkedHashSet}</td></tr>
     * <tr><td>{@link Queue}</td>                           <td class="sep">{@link LinkedList}</td></tr>
     * <tr><td>{@link List} or other {@link Collection}</td><td class="sep">{@link ArrayList}</td></tr>
     * </table>
     *
     * This method may not preserve the {@link org.apache.sis.util.collection.CheckedContainer} interface.
     *
     * @param  <E>         the type of elements in the collection.
     * @param  collection  the collection to copy, or {@code null}.
     * @return a copy of the given collection, or {@code null} if the given collection was null.
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
            if (collection instanceof EnumSet<?>) {
                return ((EnumSet) collection).clone();
            }
            if (collection instanceof CodeListSet<?>) {
                return ((CodeListSet) collection).clone();
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
     * <caption>Implementations for types</caption>
     * <tr><th>Input type</th>                  <th class="sep">Output type</th></tr>
     * <tr><td>{@link SortedMap}</td>           <td class="sep">{@link TreeMap}</td></tr>
     * <tr><td>{@link HashMap}</td>             <td class="sep">{@link HashMap}</td></tr>
     * <tr><td>{@link Map} other than above</td><td class="sep">{@link LinkedHashMap}</td></tr>
     * </table>
     *
     * @param  <K>  the type of keys in the map.
     * @param  <V>  the type of values in the map.
     * @param  map  the map to copy, or {@code null}.
     * @return a copy of the given map, or {@code null} if the given map was null.
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
     * Returns a more compact representation of the given map. This method is similar to
     * {@link #unmodifiableOrCopy(Map)} except that it does not wrap the map in an unmodifiable
     * view. The intend is to avoid one level of indirection for performance and memory reasons.
     * This is okay only if the map is kept in a private field and never escape outside this class.
     *
     * @param  <K>  the type of keys in the map.
     * @param  <V>  the type of values in the map.
     * @param  map  the map to compact, or {@code null}.
     * @return a potentially compacted map, or {@code null} if the given map was null.
     */
    public static <K,V> Map<K,V> compact(final Map<K,V> map) {
        if (map != null) {
            switch (map.size()) {
                case 0:  return Collections.emptyMap();
                case 1:  final Map.Entry<K,V> entry = map.entrySet().iterator().next();
                         return Collections.singletonMap(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    /**
     * Returns a snapshot of the given list. The returned list will not be affected by changes
     * in the given list after this method call. This method makes no guaranteed about whether
     * the returned list is modifiable or not.
     *
     * @param  <E>   the type of elements in the list.
     * @param  list  the list for which to take a snapshot, or {@code null} if none.
     * @return a snapshot of the given list, or {@code list} itself if null or unmodifiable.
     */
    @SuppressWarnings("unchecked")
    public static <E> List<E> snapshot(final List<E> list) {
        if (list != null && !(list instanceof UnmodifiableArrayList<?>)) {
            switch (list.size()) {
                case 0:  return Collections.emptyList();
                case 1:  return Collections.singletonList(list.get(0));
                default: return (List<E>) Arrays.asList(list.toArray());
            }
        }
        return list;
    }

    /**
     * Returns the given value as a collection. Special cases:
     *
     * <ul>
     *   <li>If the value is null, then this method returns an {@linkplain Collections#emptyList() empty list}.</li>
     *   <li>If the value is an instance of {@link Collection}, then it is returned unchanged.</li>
     *   <li>If the value is an array of objects, then it is returned {@linkplain Arrays#asList(Object[]) as a list}.</li>
     *   <li>If the value is an array of primitive type, then it is returned as a list of their wrapper class.</li>
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
     * @param  value  the value to return as a collection, or {@code null}.
     * @return the value as a collection, or wrapped in a collection (never {@code null}).
     */
    public static Collection<?> toCollection(final Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof Collection<?>) {
            return (Collection<?>) value;
        }
        if (value.getClass().isArray()) {
            if (value instanceof Object[]) {
                return Arrays.asList((Object[]) value);
            } else {
                return new AbstractList<Object>() {
                    /** Returns the number of elements in the backing array. */
                    @Override public int size() {
                        return Array.getLength(value);
                    }

                    /** Returns the element at the given index. Primitive numbers as returned as instance of their wrapper class. */
                    @Override public Object get(final int index) {
                        return Array.get(value, index);
                    }

                    /** Sets the element at the given index. Primitive numbers shall be given as instance of their wrapper class. */
                    @Override public Object set(final int index, final Object value) {
                        final Object old = Array.get(value, index);
                        Array.set(value, index, value);
                        return old;
                    }
                };
            }
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
     * @param  <T>         the type of elements in the given collection.
     * @param  collection  the collection to cast or copy to a list.
     * @return the given collection as a list, or a copy of the given collection.
     */
    public static <T> List<T> toList(final Collection<T> collection) {
        if (collection instanceof List<?>) {
            return (List<T>) collection;
        }
        return new ArrayList<>(collection);
    }

    /**
     * Returns the elements of the given collection as an array. This method can be used when the {@code valueClass}
     * argument is not known at compile-time. If the {@code valueClass} is known at compile-time, then callers should
     * use {@link Collection#toArray(T[])} instead.
     *
     * @param  <T>         the compile-time value of {@code valueClass}.
     * @param  collection  the collection from which to get the elements.
     * @param  valueClass  the runtime type of collection elements.
     * @return the collection elements as an array, or {@code null} if {@code collection} is null.
     *
     * @since 0.6
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(final Collection<T> collection, final Class<T> valueClass) {
        assert Numbers.primitiveToWrapper(valueClass) == valueClass : valueClass;
        if (collection != null) {
            return collection.toArray((T[]) Array.newInstance(valueClass, collection.size()));
        }
        return null;
    }

    /**
     * Adds a value in a pseudo multi-values map. The multi-values map is simulated by a map of lists.
     * The map can be initially empty - lists will be created as needed.
     *
     * @param  <K>    the type of key elements in the map.
     * @param  <V>    the type of value elements in the lists.
     * @param  map    the multi-values map where to add an element.
     * @param  key    the key of the element to add. Can be null if the given map supports null keys.
     * @param  value  the value of the element to add. Can be null.
     * @return the list where the given value has been added. May be unmodifiable.
     */
    public static <K,V> List<V> addToMultiValuesMap(final Map<K,List<V>> map, final K key, final V value) {
        final List<V> singleton = Collections.singletonList(value);
        List<V> values = map.put(key, singleton);
        if (values == null) {
            return singleton;               // This is the most common case.
        }
        if (values.size() <= 1) {
            values = new ArrayList<>(values);
        }
        if (map.put(key, values) != singleton) {
            throw new ConcurrentModificationException();
        }
        values.add(value);
        return values;
    }

    /**
     * Removes a value in a pseudo multi-values map. The multi-values map is simulated by a map of lists.
     * If more than one occurrence of the given value is found in the list, only the first occurrence is
     * removed. If the list become empty after this method call, that list is removed from the map.
     *
     * @param  <K>    the type of key elements in the map.
     * @param  <V>    the type of value elements in the lists.
     * @param  map    the multi-values map where to remove an element.
     * @param  key    the key of the element to remove. Can be null if the given map supports null keys.
     * @param  value  the value of the element to remove. Can be null.
     * @return list of remaining elements after the removal, or {@code null} if no list is mapped to the given key.
     */
    public static <K,V> List<V> removeFromMultiValuesMap(final Map<K,List<V>> map, final K key, final V value) {
        List<V> values = map.get(key);
        if (values != null) {
            final boolean isEmpty;
            switch (values.size()) {
                case 0:  isEmpty = true; break;
                case 1:  isEmpty = Objects.equals(value, values.get(0)); break;
                default: isEmpty = values.remove(value) && values.isEmpty(); break;
            }
            if (isEmpty) {
                if (map.remove(key) != values) {
                    throw new ConcurrentModificationException();
                }
                values = Collections.emptyList();
            }
        }
        return values;
    }

    /**
     * Creates a (<cite>name</cite>, <cite>element</cite>) mapping for the given collection of elements.
     * If the name of an element is not all lower cases, then this method also adds an entry for the
     * lower cases version of that name in order to allow case-insensitive searches.
     *
     * <p>Code searching in the returned map shall ask for the original (non lower-case) name
     * <strong>before</strong> to ask for the lower-cases version of that name.</p>
     *
     * @param  <E>           the type of elements.
     * @param  entries       the entries to store in the map, or {@code null} if none.
     * @param  namesLocale   the locale to use for creating the "all lower cases" names.
     * @return a (<cite>name</cite>, <cite>element</cite>) mapping with lower cases entries where possible.
     * @throws InvalidParameterCardinalityException if the same name is used for more than one element.
     */
    public static <E> Map<String,E> toCaseInsensitiveNameMap(
            final Collection<Map.Entry<String,E>> entries, final Locale namesLocale)
    {
        if (entries == null) {
            return Collections.emptyMap();
        }
        final Map<String,E> map = new HashMap<>(hashMapCapacity(entries.size()));
        final Set<String> generated = new HashSet<>();
        for (final Map.Entry<String, ? extends E> entry : entries) {
            final String name = entry.getKey();
            final E value = entry.getValue();
            E old = map.put(name, value);
            if (old != null && !generated.remove(name)) {
                /*
                 * If two elements use exactly the same name, this is considered an error. Otherwise the previous
                 * mapping was using a lower case name version of its original name, so we can discard that lower
                 * case version (the original name is still present in the map).
                 */
                throw new InvalidParameterCardinalityException(Errors.format(Errors.Keys.ValueAlreadyDefined_1, name), name);
            }
            /*
             * Add lower-cases versions of the above element names, only if that name is not already used.
             * If a name was already used, then the original mapping will have precedence.
             */
            final String lower = name.toLowerCase(namesLocale);
            if (!name.equals(lower)) {
                if (generated.add(lower)) {
                    map.putIfAbsent(lower, value);
                } else {
                    /*
                     * Two entries having non-lower case names got the same name after conversion to
                     * lower cases. Retains none of them, since doing so would introduce an ambiguity.
                     * Remember that we can not use that lower cases name for any other entries.
                     */
                    map.remove(lower);
                }
            }
        }
        return map;
    }

    /**
     * Returns {@code true} if the next elements returned by the given iterators are the same.
     * This method compares using the identity operation ({@code ==}), not {@code equals(Object)}.
     *
     * @param  it1  the first iterator.
     * @param  it2  the second iterator.
     * @return if both iterators return the same objects.
     */
    public static boolean identityEquals(final Iterator<?> it1, final Iterator<?> it2) {
        while (it1.hasNext()) {
            if (!it2.hasNext() || it1.next() != it2.next()) {
                return false;
            }
        }
        return !it2.hasNext();
    }
}
