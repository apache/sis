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
package org.apache.sis.util.privy;

import java.util.*;
import java.lang.reflect.Array;
import org.opengis.util.CodeList;
import org.opengis.parameter.InvalidParameterCardinalityException;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.collection.CodeListSet;
import org.apache.sis.util.collection.CheckedContainer;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.pending.jdk.JDK19;


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
 * bit tedious to explain, which is another indication that they should not be in public API.
 *
 * <h2>Null values</h2>
 * All methods in this class accepts null values. The collections created by this class also accept null.
 * This class prefers {@link Collections} methods instead of the new static {@code of(…)} method in interfaces
 * because the former accept null values while the latter throws {@link NullPointerException}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
public final class CollectionsExt {
    /**
     * Do not allow instantiation of this class.
     */
    private CollectionsExt() {
    }

    /**
     * Returns a queue which is always empty and accepts no element.
     * This method will be removed if a future JDK version provides such method in {@link Collections}.
     *
     * @param  <E>  the type of elements in the empty collection.
     * @return an empty collection.
     *
     * @see Collections#emptyList()
     * @see Collections#emptySet()
     */
    @SuppressWarnings("unchecked")
    public static <E> Queue<E> emptyQueue() {
        return EmptyQueue.INSTANCE;
    }

    /**
     * Returns an empty collection of the given type, or {@code null} if the given type is unknown to this method.
     *
     * @param  type  the desired collection type.
     * @return an empty collection of the given type, or {@code null} if the type is unknown.
     */
    public static Collection<?> empty(final Class<?> type) {
        if (type.isAssignableFrom(List.class)) {                    // Most common case first.
            return Collections.EMPTY_LIST;
        } else if (type.isAssignableFrom(Set.class)) {
            return Collections.EMPTY_SET;
        } else if (type.isAssignableFrom(NavigableSet.class)) {     // Rarely used case (at least in SIS).
            if (type.isAssignableFrom(SortedSet.class)) {
                return Collections.emptySortedSet();
            } else {
                return Collections.emptyNavigableSet();
            }
        } else if (type.isAssignableFrom(Queue.class)) {
            return emptyQueue();
        } else {
            return null;
        }
    }

    /**
     * Returns the number of elements if the given object is a collection or a map.
     * Otherwise returns 0 if the given object if null or 1 otherwise.
     *
     * @param  c  the collection or map for which to get the size, or {@code null}.
     * @return the size or pseudo-size of the given object.
     */
    public static int size(final Object c) {
        if (c == null) {
            return 0;
        } else if (c instanceof Collection<?>) {
            return ((Collection<?>) c).size();
        } else if (c instanceof Map<?,?>) {
            return ((Map<?,?>) c).size();
        } else {
            return 1;
        }
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
     * Returns the last non-null element of the given list.
     *
     * @param  <T>   the type of elements contained in the list.
     * @param  list  the list from which to get the last non-null element, or {@code null}.
     * @return the last non-null element, or {@code null} if the given list is null or empty.
     */
    public static <T> T lastNonNull(final List<T> collection) {
        if (collection != null) {
            int i = collection.size();
            while (--i >= 0) {
                T e = collection.get(i);
                if (e != null) return e;
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
     * Returns the given value as a singleton if non-null, or returns an empty set otherwise.
     *
     * @param  <E>      the element type.
     * @param  element  the element to return in a collection if non-null.
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
     */
    @SafeVarargs
    public static <T> Set<T> nonEmptySet(final T... elements) {
        @SuppressWarnings("varargs")
        final Set<T> asSet = immutableSet(true, elements);
        return (asSet != null && asSet.isEmpty()) ? null : asSet;
    }

    /**
     * Returns the given array if non-empty, or {@code null} if the given array is null or empty.
     * This method is generally not recommended, since public API should prefer empty array instead of null.
     * However, this method is occasionally useful for managing private fields.
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
     * However, this method is occasionally useful for managing private fields, especially for inter-operability
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
        return (c != null) ? c : Collections.emptySet();
    }

    /**
     * Returns the given set, or {@link Collections#EMPTY_SET} if the given set is null.
     *
     * @param  <E>  the type of elements in the collection.
     * @param  c    the collection, or {@code null}.
     * @return the given collection, or an empty set if the given collection was null.
     */
    public static <E> Set<E> nonNull(final Set<E> c) {
        return (c != null) ? c : Collections.emptySet();
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
     * <h4>Design note</h4>
     * It would be very easy to add support for {@code value} argument of type {@code Object[]} or collections.
     * But we do not provide such support for now because this method is used mostly as a helper method for
     * constructors of {@code AbstractIdentifiedObject} subclasses receiving a map of properties,
     * and the contract of our constructors do not allow those other types for now.
     *
     * @param  <E>         the type of elements in the array to be returned.
     * @param  name        the parameter name, used only for formatting an error message in case of failure.
     * @param  value       the value to return as an array, or {@code null}.
     * @param  emptyArray  an instance of {@code new E[0]}. This argument cannot be null.
     * @return the given value as an array of {@code <E>}. Never null.
     * throws  IllegalArgumentException if the given value is not null, an instance of {@code <E>}
     *         or an array of {@code <E>}.
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
                final var set = new LinkedHashSet<E>(Arrays.asList((E[]) value));
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
        return JDK19.newLinkedHashSet(count);
    }

    /**
     * Returns the specified array as an immutable set, or {@code null} if the array is null.
     * If the given array contains duplicated elements, i.e. elements that are equal in the
     * sense of {@link Object#equals(Object)}, then only the last instance of the duplicated
     * values will be included in the returned set.
     *
     * <p>This method differs from {@link Set#of(Object...)} in that it preserves element order
     * and accepts null elements.</p>
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
                @SuppressWarnings("varargs")
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
     * in that it tries to return a more efficient object when there is zero or one element.
     * Such small set occurs frequently in Apache SIS, especially for
     * {@link org.apache.sis.referencing.AbstractIdentifiedObject} names or identifiers.
     *
     * <p><em>The set returned by this method may or may not be a view of the given set</em>.
     * Consequently, this method shall be used <strong>only</strong> if the given set will
     * <strong>not</strong> be modified after this method call. In case of doubt, use the
     * standard {@link Collections#unmodifiableSet(Set)} method instead.</p>
     *
     * <p>This method differs from {@link Set#copyOf(Collection)} in that it may avoid copy,
     * preserves element order and accepts null elements.</p>
     *
     * @param  <E>  the type of elements in the set.
     * @param  set  the set to make unmodifiable, or {@code null}.
     * @return a unmodifiable version of the given set, or {@code null} if the given set was null.
     */
    public static <E> Set<E> unmodifiableOrCopy(final Set<E> set) {
        if (set == null) {
            return null;
        }
        switch (set.size()) {
            case 0:  return Collections.emptySet();
            case 1:  return Collections.singleton(set.iterator().next());
            default: return Collections.unmodifiableSet(set);
        }
    }

    /**
     * Returns a unmodifiable version of the given map.
     * This method is different than the standard {@link Collections#unmodifiableMap(Map)}
     * in that it tries to return a more efficient object when there is zero or one entry.
     * Such small maps occur frequently in Apache SIS.
     *
     * <p><em>The map returned by this method may or may not be a view of the given map</em>.
     * Consequently, this method shall be used <strong>only</strong> if the given map will
     * <strong>not</strong> be modified after this method call. In case of doubt, use the
     * standard {@link Collections#unmodifiableMap(Map)} method instead.</p>
     *
     * <p>This method differs from {@link Map#copyOf(Map)} in that it may avoid copy,
     * preserves element order and accepts null elements.</p>
     *
     * @param  <K>  the type of keys in the map.
     * @param  <V>  the type of values in the map.
     * @param  map  the map to make unmodifiable, or {@code null}.
     * @return a unmodifiable version of the given map, or {@code null} if the given map was null.
     *
     * @see #compact(Map)
     */
    public static <K,V> Map<K,V> unmodifiableOrCopy(final Map<K,V> map) {
        if (map == null) {
            return null;
        }
        switch (map.size()) {
            case 0: return Collections.emptyMap();
            case 1: {
                final Map.Entry<K,V> entry = map.entrySet().iterator().next();
                return Collections.singletonMap(entry.getKey(), entry.getValue());
            }
            default: return Collections.unmodifiableMap(map);
        }
    }

    /**
     * Returns a unmodifiable version of the given list.
     *
     * <p><em>The collection returned by this method may or may not be a view of the given list</em>.
     * Consequently, this method shall be used <strong>only</strong> if the given list will
     * <strong>not</strong> be modified after this method call. In case of doubt, use the
     * standard {@link Collections#unmodifiableList(List)} method instead.</p>
     *
     * @param  <E>   the type of elements in the list.
     * @param  list  the list to make unmodifiable, or {@code null}.
     * @return a unmodifiable version of the given list, or {@code null} if the given list was null.
     */
    public static <E> List<E> unmodifiableOrCopy(List<E> list) {
        if (list != null) {
            final int length = list.size();
            switch (length) {
                case 0: {
                    list = Collections.emptyList();
                    break;
                }
                case 1: {
                    list = Collections.singletonList(list.get(0));
                    break;
                }
                default: {
                    if (list instanceof UnmodifiableArrayList<?>) {
                        break;                                              // List is already unmodifiable.
                    }
                    if (list instanceof CheckedContainer<?>) {
                        /*
                         * We use UnmodifiableArrayList for avoiding one level of indirection. The fact that it
                         * implements CheckedContainer is not a goal here, and is actually unsafe since we have
                         * no guarantee (except Javadoc contract) that the <E> in CheckedContainer<E> is really
                         * the same as in Collection<E>.  We tolerate this hole for now because we documented
                         * the restriction in CheckedContainer javadoc.
                         */
                        @SuppressWarnings("unchecked")       // Okay if collection is compliant with CheckedContainer contract.
                        final E[] array = (E[]) Array.newInstance(((CheckedContainer<E>) list).getElementType(), length);
                        list = UnmodifiableArrayList.wrap(list.toArray(array));
                    } else if (list instanceof List<?>) {
                        list = Collections.unmodifiableList(list);
                    }
                    break;
                }
            }
        }
        return list;
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
     * view. The intent is to avoid one level of indirection for performance and memory reasons.
     * This is okay only if the map is kept in a private field and never escape outside that class.
     *
     * @param  <K>  the type of keys in the map.
     * @param  <V>  the type of values in the map.
     * @param  map  the map to compact, or {@code null}.
     * @return a potentially compacted map, or {@code null} if the given map was null.
     *
     * @see #unmodifiableOrCopy(Map)
     */
    public static <K,V> Map<K,V> compact(final Map<K,V> map) {
        if (map != null) {
            switch (map.size()) {
                case 0: return Collections.emptyMap();
                case 1: final Map.Entry<K,V> entry = map.entrySet().iterator().next();
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
     * <p>This method differs from {@link List#copyOf(Collection)} in that it accepts null elements.</p>
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
     * Returns a unmodifiable copy of the given set, preserving order.
     *
     * @param  <E>   the type of elements.
     * @param  set   the set to copy, or {@code null}.
     * @return a copy of the given set, or {@code null} if the given set was null.
     */
    public static <E> Set<E> copyPreserveOrder(final Set<E> set) {
        if (set == null) {
            return null;
        }
        switch (set.size()) {
            case 0:  return Collections.emptySet();
            case 1:  return Collections.singleton(set.iterator().next());
            default: return Collections.unmodifiableSet(new LinkedHashSet<>(set));
        }
    }

    /**
     * Returns a clone of the given set. This method is only intended to avoid the "unchecked cast" warning.
     *
     * @param  <E>  type of elements in the set.
     * @param  set  the set to clone.
     * @return a clone of the given set.
     */
    @SuppressWarnings("unchecked")
    public static <E> HashSet<E> clone(final HashSet<E> set) {
        return (HashSet<E>) set.clone();
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
            final var list = new ArrayList<Object>();
            for (final Object element : (Iterable<?>) value) {
                list.add(element);
            }
            return list;
        }
        if (value instanceof Iterator<?>) {
            final var it = (Iterator<?>) value;
            final var list = new ArrayList<Object>();
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
     * Returns the elements of the given collection as an array.
     * This method can be used when the {@code valueClass} argument is not known at compile-time.
     *
     * @param  <T>         the compile-time value of {@code valueClass}.
     * @param  collection  the collection from which to get the elements.
     * @param  valueClass  the runtime type of collection elements.
     * @return the collection elements as an array, or {@code null} if {@code collection} is null.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(final Collection<? extends T> collection, final Class<T> valueClass) {
        assert Numbers.primitiveToWrapper(valueClass) == valueClass : valueClass;
        if (collection != null) {
            return collection.toArray((T[]) Array.newInstance(valueClass, collection.size()));
        }
        return null;
    }

    /**
     * Adds a value in a pseudo multi-values map. The multi-values map is simulated by a map of sets.
     * The map can be initially empty: sets will be created as needed, with an optimization for the
     * common case where the majority of keys are associated to exactly one value.
     * Null values are accepted.
     *
     * @param  <K>    the type of key elements in the map.
     * @param  <V>    the type of value elements in the sets.
     * @param  map    the multi-values map where to add an element.
     * @param  key    the key of the element to add. Can be null if the given map supports null keys.
     * @param  value  the value of the element to add. Can be null.
     * @return the set where the given value has been added. May be unmodifiable.
     */
    public static <K,V> Set<V> addToMultiValuesMap(final Map<K,Set<V>> map, final K key, final V value) {
        return map.merge(key, Collections.singleton(value), (values, singleton) -> {
            final Set<V> dest = (values.size() > 1) ? values : new LinkedHashSet<>(values);
            return dest.addAll(singleton) ? dest : values;
        });
    }

    /**
     * Removes a value in a pseudo multi-values map. The multi-values map is simulated by a map of sets.
     * If the set become empty after this method call, that set is removed from the map and this method
     * returns the empty set.
     *
     * @param  <K>    the type of key elements in the map.
     * @param  <V>    the type of value elements in the lists.
     * @param  map    the multi-values map where to remove an element.
     * @param  key    the key of the element to remove. Can be null if the given map supports null keys.
     * @param  value  the value of the element to remove. Can be null.
     * @return set of remaining elements after the removal, or {@code null} if no set is mapped to the given key.
     */
    public static <K,V> Set<V> removeFromMultiValuesMap(final Map<K,Set<V>> map, final K key, final V value) {
        final Set<V> remaining = map.compute(key, (k, values) -> {
            if (values != null) {
                final boolean isEmpty;
                switch (values.size()) {
                    case 0:  isEmpty = true; break;
                    case 1:  isEmpty = values.contains(value); break;
                    default: isEmpty = values.remove(value) && values.isEmpty(); break;
                }
                if (isEmpty) {
                    return Collections.emptySet();
                }
            }
            return values;
        });
        if (remaining != null && remaining.isEmpty()) {
            if (map.remove(key) != remaining) {
                throw new ConcurrentModificationException();
            }
        }
        return remaining;
    }

    /**
     * Creates a (<var>name</var>, <var>element</var>) mapping for the given collection of elements.
     * If the name of an element is not all lower cases, then this method also adds an entry for the
     * lower cases version of that name in order to allow case-insensitive searches.
     *
     * <p>Code searching in the returned map shall ask for the original (non lower-case) name
     * <strong>before</strong> to ask for the lower-cases version of that name.</p>
     *
     * <p>Iteration order in map entries is the same as iteration order in the given collection.
     * If lower-case names have been generated, they appear immediately after the original names.</p>
     *
     * @param  <E>           the type of elements.
     * @param  entries       the entries to store in the map, or {@code null} if none.
     * @param  namesLocale   the locale to use for creating the "all lower cases" names.
     * @return a (<var>name</var>, <var>element</var>) mapping with lower cases entries where possible.
     * @throws InvalidParameterCardinalityException if the same name is used for more than one element.
     */
    public static <E> Map<String,E> toCaseInsensitiveNameMap(
            final Collection<Map.Entry<String,E>> entries, final Locale namesLocale)
    {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String,E> map = JDK19.newLinkedHashMap(entries.size());
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
                     * Remember that we cannot use that lower cases name for any other entries.
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
