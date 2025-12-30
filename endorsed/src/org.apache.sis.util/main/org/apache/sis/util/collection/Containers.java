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

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import org.opengis.util.CodeList;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.resources.Errors;


/**
 * Static methods working on various types of objects that can be viewed as collections.
 * The types include {@link Collection} and {@link CheckedContainer}.
 * Some methods are extensions to the standard {@link Collections} utility class.
 *
 * <h2>Null values</h2>
 * Most methods in this class accepts null argument values, and collections or arrays containing null elements.
 * By contrast, the standard static methods of {@link List}, {@link Set} and {@link Map} interfaces reject null values.
 * These standard methods should be preferred when null values would be error.
 *
 * <h2>Element order</h2>
 * All methods in this class preserve element order, including the methods returning a {@link Set}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.6
 * @since   0.3
 */
public final class Containers {
    /**
     * Do not allow instantiation of this class.
     */
    private Containers() {
    }

    /**
     * Returns a queue which is always empty and accepts no element.
     *
     * @param  <E>  the type of elements in the empty queue.
     * @return an empty queue.
     *
     * @see Collections#emptyList()
     * @see Collections#emptySet()
     * @since 1.6
     */
    @SuppressWarnings("unchecked")
    public static <E> Queue<E> emptyQueue() {
        return EmptyQueue.INSTANCE;
    }

    /**
     * Returns {@code true} if the given collection is unmodifiable (not necessarily immutable).
     * In case of doubt, this method returns {@code false}.
     * A null collection is considered as unmodifiable.
     *
     * @param  collection  the collection, or {@code null}.
     * @return {@code true} if the given collection is null or unmodifiable.
     *
     * @see CheckedContainer.Mutability#UNMODIFIABLE
     */
    private static boolean isUnmodifiable(final Collection<?> collection) {
        if (collection == null) {
            return true;
        }
        if (collection instanceof CheckedContainer<?>) {
            return ((CheckedContainer<?>) collection).getMutability().isUnmodifiable();
        }
        return false;
    }

    /**
     * Returns {@code true} if the given collection is either null or
     * {@linkplain Collection#isEmpty() empty}. If this method returns {@code false},
     * then the given collection is guaranteed to be non-null and to contain at least
     * one element.
     *
     * <p>This is a convenience method for classes implementing the <i>lazy instantiation</i>
     * pattern. In such cases, null collections (i.e. collections not yet instantiated) are typically
     * considered as {@linkplain Collection#isEmpty() empty}.</p>
     *
     * @param  collection  the collection to test, or {@code null}.
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
     * <p>This is a convenience method for classes implementing the <i>lazy instantiation</i>
     * pattern. In such cases, null maps (i.e. maps not yet instantiated) are typically considered
     * as {@linkplain Map#isEmpty() empty}.</p>
     *
     * @param  map  the map to test, or {@code null}.
     * @return {@code true} if the given map is null or empty, or {@code false} otherwise.
     */
    public static boolean isNullOrEmpty(final Map<?,?> map) {
        return (map == null) || map.isEmpty();
    }

    /**
     * Returns the given collection, or {@link Collections#EMPTY_SET} if the given collection is null.
     * In the latter case, the returned collection is tolerant to calls to {@code contains(null)} and
     * {@code clear()}.
     *
     * @param  <E>         the type of elements in the collection.
     * @param  collection  the collection, or {@code null}.
     * @return the given collection, or an empty collection if the given collection was null.
     *
     * @since 1.6
     */
    public static <E> Collection<E> nonNull(final Collection<E> collection) {
        return (collection != null) ? collection : Collections.emptySet();
        // We do not use `Set.of()` in order to be more tolerant.
    }

    /**
     * Returns the given set, or {@link Collections#EMPTY_SET} if the given set is null.
     * In the latter case, the returned set is tolerant to calls to {@code contains(null)}
     * and {@code clear()}.
     *
     * @param  <E>  the type of elements in the collection.
     * @param  set  the collection, or {@code null}.
     * @return the given collection, or an empty set if the given collection was null.
     *
     * @since 1.6
     */
    public static <E> Set<E> nonNull(final Set<E> set) {
        return (set != null) ? set : Collections.emptySet();
        // We do not use `Set.of()` in order to be more tolerant.
    }

    /**
     * Returns the first element of the given iterable, or {@code null} if none.
     * If the iterable contains more than one element, the remaining elements are ignored.
     * Consequently, this method should be used only when multi-occurrence is not ambiguous.
     *
     * <p>This method is null-safe. Note however that the first element may be null.
     * This method does not distinguish between an empty collection and a collection
     * containing null elements.</p>
     *
     * @param  <T>         the type of elements contained in the iterable.
     * @param  collection  the iterable from which to get the first element, or {@code null}.
     * @return the first element, or {@code null} if the given iterable is null or empty.
     *
     * @see Deque#peekFirst()
     * @since 1.6
     */
    public static <T> T peekFirst(final Iterable<T> collection) {
        if (collection != null) {
            final Iterator<T> it = collection.iterator();
            if (it != null && it.hasNext()) {
                return it.next();
            }
        }
        return null;
    }

    /**
     * If the given iterable contains exactly one non-null element, returns that element.
     * Otherwise returns {@code null}. This is a variant of {@link #peekFirst(Iterable)}
     * where remaining elements are not ignored.
     *
     * @param  <T>         the type of elements contained in the iterable.
     * @param  collection  the iterable from which to get the singleton element, or {@code null}.
     * @return the singleton element, or {@code null} if the given iterable is null or does not
     *         contain exactly one non-null element.
     *
     * @since 1.6
     */
    public static <T> T peekIfSingleton(final Iterable<T> collection) {
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
     *
     * @see Collections#emptySet()
     * @see Collections#singleton(Object)
     * @since 1.6
     */
    public static <E> Set<E> singletonOrEmpty(final E element) {
        return (element != null) ? Collections.singleton(element) : Collections.emptySet();
    }

    /**
     * Copies the non-null elements of the specified array in an immutable set, or returns {@code null} if the array is null.
     * This method does the same work as {@link #copyToImmutableSet(Object...)} except that null elements are silently ignored.
     *
     * @param  <E>       the type of array elements.
     * @param  elements  the array to copy in a set. May be {@code null} or contain null elements.
     * @return an immutable set containing the non-null array elements, or {@code null} if the given array was null.
     *
     * @see Set#of(Object...)
     * @since 1.6
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <E> Set<E> copyToImmutableSetIgnoreNull(final E... elements) {
        return copyToSet(elements, true);
    }

    /**
     * Copies the content of the specified array in an immutable set, or returns {@code null} if the array is null.
     * If the given array contains duplicated elements in the sense of {@link Object#equals(Object)},
     * then the returned set will contain an arbitrary instance among the duplicated values.
     * If the array contains some {@code null} elements, then the returned set contains one null element.
     *
     * <h4>Differences with standard {@code Set.of(…)} method</h4>
     * This method differs from {@link Set#of(Object...)} in that it preserves element order,
     * accepts null argument, accepts null elements and does not throw {@link IllegalArgumentException}
     * if the array contains duplicated elements.
     *
     * @param  <E>       the type of array elements.
     * @param  elements  the array to copy in a set. May be {@code null} or contain null elements.
     * @return an immutable set containing the array elements, or {@code null} if the given array was null.
     *
     * @see Set#of(Object...)
     * @since 1.6
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <E> Set<E> copyToImmutableSet(final E... elements) {
        return copyToSet(elements, false);
    }

    /**
     * Implementation of {@code copyToImmutableSet(…)} and {@code copyToImmutableSetIgnoreNull(…)}.
     *
     * @param  <E>          the type of array elements.
     * @param  elements     the array to copy in a set. May be {@code null} or contain null elements.
     * @param  excludeNull  {@code true} for excluding the {@code null} element from the returned set.
     * @return an immutable set containing the array elements, or {@code null} if the given array was null.
     *
     * @see Collections#unmodifiableSet(Set)
     */
    @SuppressWarnings("fallthrough")
    private static <E> Set<E> copyToSet(final E[] elements, final boolean excludeNull) {
        if (elements == null) {
            return null;
        }
        switch (elements.length) {
            case 1: {
                final E element = elements[0];
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
                final var set = new LinkedHashSet<>(Arrays.asList(elements));
                if (excludeNull) {
                    set.remove(null);
                }
                return unmodifiable(set);
            }
        }
    }

    /**
     * Copies the content of the specified collection in an immutable list, or returns {@code null} if
     * the collection is null. The returned list implements the {@link CheckedContainer} interface.
     *
     * <h4>Differences with standard {@code List.of(…)} method</h4>
     * This method differs from {@link List#of(Object...)} in that it accepts null argument,
     * accepts null array elements, and returns a list implementing {@link CheckedContainer}.
     *
     * @param  <E>          the type of collection elements.
     * @param  collection   the collection to copy in a list. May be {@code null} or contain null elements.
     * @param  elementType  type of elements. This value will be returned by {@link CheckedContainer#getElementType()}.
     * @return an immutable list containing the collection elements, or {@code null} if the given collection was null.
     *
     * @see List#of(Object...)
     * @since 1.6
     */
    @SuppressWarnings("unchecked")
    public static <E> List<E> copyToImmutableList(final Collection<? extends E> collection, final Class<E> elementType) {
        if (collection == null) {
            return null;
        }
        return viewAsUnmodifiableList(collection.toArray((size) -> (E[]) Array.newInstance(elementType, size)));
    }

    /**
     * Returns an unmodifiable view of the given array. A direct reference to the given array is
     * retained (i.e. the array is <strong>not</strong> cloned). Consequently, the given array
     * shall not be modified after construction if the returned list is intended to be immutable.
     *
     * <p>The returned list implements the {@link CheckedContainer} interface. The value returned by its
     * {@link CheckedContainer#getElementType()} method is inferred from the array component type.</p>
     *
     * @param  <E>    the base type of elements in the list.
     * @param  array  the array to wrap, or {@code null} if none.
     * @return the given array wrapped in an unmodifiable list, or {@code null} if the given array was null.
     *
     * @see Arrays#asList(Object[])
     * @see Collections#unmodifiableList(List)
     * @since 1.6
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <E> List<E> viewAsUnmodifiableList(final E... array) {
        return (array == null) ? null : new UnmodifiableArrayList<>(array);
    }

    /**
     * Returns an unmodifiable view of a subregion of the given array. A direct reference to the
     * given array is retained (i.e. the array is <strong>not</strong> cloned). Consequently, the
     * specified sub-region of the given array shall not be modified after construction if the
     * returned list is intended to be immutable.
     *
     * <p>The returned list implements the {@link CheckedContainer} interface. The value returned by its
     * {@link CheckedContainer#getElementType()} method is inferred from the array component type.</p>
     *
     * @param  <E>    the type of elements in the list.
     * @param  array  the array to wrap (cannot be null).
     * @param  lower  low endpoint (inclusive) of the sublist.
     * @param  upper  high endpoint (exclusive) of the sublist.
     * @return the given array wrapped in an unmodifiable list.
     * @throws IndexOutOfBoundsException if the lower or upper value are out of bounds.
     *
     * @since 1.6
     */
    public static <E> List<E> viewAsUnmodifiableList(final E[] array, final int lower, final int upper) {
        Objects.checkFromToIndex(lower, upper, array.length);
        if (lower == 0 && upper == array.length) {
            return new UnmodifiableArrayList<>(array);
        }
        return new UnmodifiableArrayList.SubList<>(array, lower, upper - lower);
    }

    /**
     * Returns an unmodifiable view of the given array. A direct reference to the given array is
     * retained (i.e. the array is <strong>not</strong> cloned).
     *
     * @param  <E>    the base type of elements in the list.
     * @param  array  the array to wrap, or {@code null} if none.
     * @return the given array wrapped in an unmodifiable list, or {@code null} if the given array was null.
     *
     * @deprecated Renamed as {@link #viewAsUnmodifiableList(Object...)} for clarity.
     * The new name emphases the contrast with {@link #copyToImmutableList(Collection, Class)}.
     * The parameterized return type is also different.
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    @Deprecated(since = "1.6", forRemoval = true)
    public static <E> List<? extends E> unmodifiableList(final E... array) {
        return viewAsUnmodifiableList(array);
    }

    /**
     * Returns an unmodifiable view of a subregion of the given array. A direct reference to the
     * given array is retained (i.e. the array is <strong>not</strong> cloned).
     *
     * @param  <E>    the type of elements in the list.
     * @param  array  the array to wrap (cannot be null).
     * @param  lower  low endpoint (inclusive) of the sublist.
     * @param  upper  high endpoint (exclusive) of the sublist.
     * @return the given array wrapped in an unmodifiable list.
     * @throws IndexOutOfBoundsException if the lower or upper value are out of bounds.
     *
     * @deprecated Renamed as {@link #viewAsUnmodifiableList(Object[], int, int)} for clarity.
     * The new name emphases the contrast with {@link #copyToImmutableList(Collection, Class)}.
     * The parameterized return type is also different.
     */
    @Deprecated(since = "1.6", forRemoval = true)
    public static <E> List<? extends E> unmodifiableList(final E[] array, final int lower, final int upper) {
        return viewAsUnmodifiableList(array, lower, upper);
    }

    /**
     * Returns an unmodifiable view or copy of the given list.
     * This method is similar to the standard {@link Collections#unmodifiableList(List)} except for the following:
     *
     * <ul>
     *   <li>Accepts {@code null} argument, in which case this method returns {@code null}.</li>
     *   <li>Does not guarantee that the returned list is a view of the given list. It may be a copy.</li>
     *   <li>As a result of above relaxation, returns more efficient implementations for lists of zero or one element.
     *       Such small set occurs frequently in Apache <abbr>SIS</abbr>.</li>
     * </ul>
     *
     * This method should be used only if the given list will not be modified after this method call.
     * In case of doubt, use the standard {@link Collections#unmodifiableList(List)} method if a view
     * is desired, or {@link List#copyOf(Collection)} otherwise.
     *
     * <h4>Differences with standard {@code copyOf(…)} method</h4>
     * This method differs from {@link List#copyOf(Collection)} in that it may avoid copy,
     * and accepts null elements.
     *
     * @param  <E>   the type of elements in the list.
     * @param  list  the list to make unmodifiable, or {@code null}.
     * @return an unmodifiable view or copy of the given list, or {@code null} if the given list was null.
     *
     * @since 1.6
     */
    public static <E> List<E> unmodifiable(List<E> list) {
        if (list != null) {
            final int length = list.size();
            switch (length) {
                case 0: list = Collections.emptyList(); break;
                case 1: list = Collections.singletonList(list.get(0)); break;
                default: {
                    if (list instanceof CheckedContainer<?>) {
                        final var c = (CheckedContainer<?>) list;
                        if (!c.getMutability().isUnmodifiable()) {
                            @SuppressWarnings("unchecked") // Okay if `c` is compliant with CheckedContainer contract.
                            final E[] array = (E[]) Array.newInstance(c.getElementType(), length);
                            list = new UnmodifiableArrayList<>(list.toArray(array));
                        }
                    } else {
                        list = Collections.unmodifiableList(list);
                    }
                }
            }
        }
        return list;
    }

    /**
     * Returns an unmodifiable view or copy of the given set.
     * This method is similar to the standard {@link Collections#unmodifiableSet(Set)} except for the following:
     *
     * <ul>
     *   <li>Accepts {@code null} argument, in which case this method returns {@code null}.</li>
     *   <li>Does not guarantee that the returned set is a view of the given set. It may be a copy.</li>
     *   <li>As a result of above relaxation, returns more efficient implementations for sets of zero or one element.
     *       Such small set occurs frequently in Apache <abbr>SIS</abbr>, especially for names or identifiers.</li>
     * </ul>
     *
     * This method should be used only if the given set will not be modified after this method call.
     * In case of doubt, use the standard {@link Collections#unmodifiableSet(Set)} method instead.
     *
     * <h4>Differences with standard {@code copyOf(…)} method</h4>
     * This method differs from {@link Set#copyOf(Collection)} in that it may avoid copy,
     * preserves element order and accepts null elements.
     *
     * @param  <E>  the type of elements in the set.
     * @param  set  the set to make unmodifiable, or {@code null}.
     * @return an unmodifiable view or copy of the given set, or {@code null} if the given set was null.
     *
     * @since 1.6
     */
    public static <E> Set<E> unmodifiable(final Set<E> set) {
        if (isUnmodifiable(set)) {
            return set;
        }
        switch (set.size()) {
            case 0:  return Collections.emptySet();
            case 1:  return Collections.singleton(set.iterator().next());
            default: return Collections.unmodifiableSet(set);
        }
    }

    /**
     * Returns an unmodifiable view or copy of the given map.
     * This method is similar to the standard {@link Collections#unmodifiableMap(Map)} except for the following:
     *
     * <ul>
     *   <li>Accepts {@code null} argument, in which case this method returns {@code null}.</li>
     *   <li>Does not guarantee that the returned map is a view of the given map. It may be a copy.</li>
     *   <li>As a result of above relaxation, returns more efficient implementations for maps of zero or one entry.
     *       Such small map occurs frequently in Apache <abbr>SIS</abbr>.</li>
     * </ul>
     *
     * This method should be used only if the given map will not be modified after this method call.
     * In case of doubt, use the standard {@link Collections#unmodifiableMap(Map)} method instead.
     *
     * <h4>Differences with standard {@code copyOf(…)} method</h4>
     * This method differs from {@link Map#copyOf(Map)} in that it may avoid copy,
     * preserves entry order and accepts null keys and values.
     *
     * @param  <K>  the type of keys in the map.
     * @param  <V>  the type of values in the map.
     * @param  map  the map to make unmodifiable, or {@code null}.
     * @return an unmodifiable view or copy of the given map, or {@code null} if the given map was null.
     *
     * @since 1.6
     */
    public static <K,V> Map<K,V> unmodifiable(final Map<K,V> map) {
        if (map == null) {
            return null;
        }
        switch (map.size()) {
            case 0:  return Collections.emptyMap();
            case 1:  final Map.Entry<K,V> entry = map.entrySet().iterator().next();
                     return Collections.singletonMap(entry.getKey(), entry.getValue());
            default: return Collections.unmodifiableMap(map);
        }
    }

    /**
     * Creates a dynamically type-safe and null-safe set.
     * Any attempt to insert an element of the wrong type will result in an immediate {@link ClassCastException},
     * and any attempt to insert a null element will result in an immediate {@link NullPointerException}.
     *
     * <h4>Differences with standard {@code Collections.checkedSet(…)} method</h4>
     * This method differs from {@link Collections#checkedSet(Set, Class)} in that it copies the given collection
     * instead of wrapping it, verifies the elements of the given collection, does not accept null elements,
     * and returns an instance of {@link EnumSet}, {@link CodeListSet} or {@link CheckedContainer}.
     *
     * @param  <E>          the type of collection elements.
     * @param  collection   the collection to copy in the set, or {@code null} for creating an initially empty set.
     * @param  elementType  type of elements. This value will be returned by {@link CheckedContainer#getElementType()}.
     * @return a modifiable set initialized to the given collection elements.
     * @throws NullPointerException if {@code elementType} is null or {@code collection} is non-null and contains null elements.
     * @throws ClassCastException if the given collection is non-null and contains elements not assignable to {@code elementType}.
     *
     * @see Collections#checkedSet(Set, Class)
     * @since 1.6
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public static <E> Set<E> newCheckedSet(final Collection<? extends E> collection, final Class<E> elementType) {
        final Set<E> set;
        if (elementType.isEnum()) {
            set = EnumSet.noneOf((Class) elementType);
        } else if (CodeList.class.isAssignableFrom(elementType) && Modifier.isFinal(elementType.getModifiers())) {
            set = new CodeListSet(elementType);
        } else {
            return (collection != null) ? new CheckedHashSet<>(collection, elementType)
                                        : new CheckedHashSet<>(elementType);
        }
        if (collection != null) {
            set.addAll(collection);
        }
        return set;
    }

    /**
     * Creates a dynamically type-safe and null-safe list.
     * Any attempt to insert an element of the wrong type will result in an immediate {@link ClassCastException},
     * and any attempt to insert a null element will result in an immediate {@link NullPointerException}.
     *
     * <h4>Differences with standard {@code Collections.checkedList(…)} method</h4>
     * This method differs from {@link Collections#checkedList(List, Class)} in that it copies the given collection
     * instead of wrapping it, verifies the elements of the given collection, does not accept null elements,
     * and returns an instance of {@link CheckedContainer}.
     *
     * @param  <E>          the type of collection elements.
     * @param  collection   the collection to copy in the list, or {@code null} for creating an initially empty list.
     * @param  elementType  type of elements. This value will be returned by {@link CheckedContainer#getElementType()}.
     * @return a modifiable list initialized to the given collection elements.
     * @throws NullPointerException if {@code elementType} is null or {@code collection} is non-null and contains null elements.
     * @throws ClassCastException if the given collection is non-null and contains elements not assignable to {@code elementType}.
     *
     * @see Collections#checkedList(List, Class)
     * @since 1.6
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public static <E> List<E> newCheckedList(final Collection<? extends E> collection, final Class<E> elementType) {
        return (collection != null) ? new CheckedArrayList<>(collection, elementType)
                                    : new CheckedArrayList<>(elementType);
    }

    /**
     * Returns a list whose elements are derived <i>on-the-fly</i> from the given list.
     * Conversions from the original elements to the derived elements are performed when needed
     * by invoking the {@link Function#apply(Object)} method on the given converter.
     * Those conversions are repeated every time that a {@code List} method needs to access values.
     * Consequently, any change in the original list is immediately visible in the derived list.
     *
     * <p>The returned list can be serialized if the given list and converter are serializable.
     * The returned list is not synchronized by itself, but is nevertheless thread-safe if the
     * given list (including its iterator) and converter are thread-safe.</p>
     *
     * <p>The returned list does <em>not</em> implement {@link CheckedContainer}.</p>
     *
     * @param  <S>        the type of elements in the storage (original) list.
     * @param  <E>        the type of elements in the derived list.
     * @param  storage    the storage list containing the original elements, or {@code null}.
     * @param  converter  the converter from the elements in the storage list to the elements in the derived list.
     * @return a view over the {@code storage} list containing all elements converted by the given converter,
     *         or {@code null} if {@code storage} was null.
     *
     * @since 1.6
     */
    public static <S,E> List<E> derivedList(final List<S> storage, final Function<S,E> converter) {
        ArgumentChecks.ensureNonNull("converter", converter);
        if (storage == null) {
            return null;
        }
        return new DerivedList<>(storage, converter);
    }

    /**
     * Returns a set whose elements are derived <i>on-the-fly</i> from the given set.
     * Conversions from the original elements to the derived elements are performed when needed
     * by invoking the {@link ObjectConverter#apply(Object)} method on the given converter.
     * Those conversions are repeated every time that a {@code Set} method needs to access values.
     * Consequently, any change in the original set is immediately visible in the derived set,
     * and conversely.
     *
     * <p>The {@link Set#add(Object) Set.add(E)} method is supported only if the given converter
     * is {@linkplain org.apache.sis.math.FunctionProperty#INVERTIBLE invertible}.
     * An invertible converter is not mandatory for other {@code Set} operations.
     * However, {@link Set#contains(Object) contains} and {@link Set#remove(Object) remove}
     * operations are likely to be faster if the inverse converter is available.</p>
     *
     * <p>The derived set may contain fewer elements than the original set if some elements
     * are not convertible. Non-convertible elements are <var>S</var> values for which
     * {@code converter.apply(S)} returns {@code null}. As a consequence of this sentinel
     * value usage, the derived set cannot contain {@code null} elements.</p>
     *
     * <p>The returned set can be serialized if the given set and converter are serializable.
     * The returned set is not synchronized by itself, but is nevertheless thread-safe if the
     * given set (including its iterator) and converter are thread-safe.</p>
     *
     * @param  <S>        the type of elements in the storage (original) set.
     * @param  <E>        the type of elements in the derived set.
     * @param  storage    the storage set containing the original elements, or {@code null}.
     * @param  converter  the converter from the elements in the storage set to the elements in the derived set.
     * @return a view over the {@code storage} set containing all elements converted by the given converter,
     *         or {@code null} if {@code storage} was null.
     *
     * @see org.apache.sis.util.ObjectConverters#derivedSet(Set, ObjectConverter)
     */
    public static <S,E> Set<E> derivedSet(final Set<S> storage, final ObjectConverter<S,E> converter) {
        ArgumentChecks.ensureNonNull("converter", converter);
        if (storage == null) {
            return null;
        }
        return DerivedSet.create(storage, converter);
    }

    /**
     * Returns a map whose keys and values are derived <i>on-the-fly</i> from the given map.
     * Conversions from the original entries to the derived entries are performed when needed
     * by invoking the {@link ObjectConverter#apply(Object)} method on the given converters.
     * Those conversions are repeated every time that a {@code Map} method needs to access values.
     * Consequently, any change in the original map is immediately visible in the derived map,
     * and conversely.
     *
     * <p>The {@link Map#put(Object,Object) Map.put(K,V)} method is supported only if the given
     * converters are {@linkplain org.apache.sis.math.FunctionProperty#INVERTIBLE invertible}.
     * An invertible converter is not mandatory for other {@code Map} operations like {@link Map#get(Object)},
     * but some of them may be faster if the inverse converters are available.</p>
     *
     * <p>The derived map may contain fewer entries than the original map if some keys are not convertible.
     * A key <var>K</var> is non-convertible if {@code keyConverter.apply(K)} returns {@code null}.
     * As a consequence of this sentinel key usage, the derived map cannot contain {@code null} keys.
     * It may contain {@code null} values however.</p>
     *
     * <p>The returned map can be serialized if the given map and converters are serializable.
     * The returned map is <strong>not</strong> thread-safe.</p>
     *
     * <p>The returned map does not implement the {@link CheckedContainer} interface since {@code Map}
     * is not a {@code Collection} sub-type, but the derived map {@linkplain Map#keySet() key set} and
     * {@linkplain Map#entrySet() entry set} do.</p>
     *
     * @param  <SK>            the type of keys   in the storage map.
     * @param  <SV>            the type of values in the storage map.
     * @param  <K>             the type of keys   in the derived map.
     * @param  <V>             the type of values in the derived map.
     * @param  storage         the storage map containing the original entries, or {@code null}.
     * @param  keyConverter    the converter from the keys in the storage map to the keys in the derived map.
     * @param  valueConverter  the converter from the values in the storage map to the values in the derived map.
     * @return a view over the {@code storage} map containing all entries converted by the given converters,
     *         or {@code null} if {@code storage} was null.
     *
     * @see org.apache.sis.util.ObjectConverters#derivedMap(Map, ObjectConverter, ObjectConverter)
     * @see org.apache.sis.util.ObjectConverters#derivedKeys(Map, ObjectConverter, Class)
     * @see org.apache.sis.util.ObjectConverters#derivedValues(Map, Class, ObjectConverter)
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
     * Returns the given object as a collection.
     * This method recognizes the following types, in that order:
     *
     * <ul>
     *   <li>If the object is null, then this method returns an {@linkplain Collections#emptySet() empty set}.</li>
     *   <li>If the object is an instance of {@link Collection}, then it is returned unchanged.</li>
     *   <li>If the object is an array of objects, then it is returned {@linkplain Arrays#asList(Object[]) as a list}.</li>
     *   <li>If the object is an array of primitive type, then it is returned as a list of their wrapper class.</li>
     *   <li>Otherwise, the object is returned as a {@linkplain Collections#singleton(Object) singleton}.</li>
     * </ul>
     *
     * @param  object  the object to return as a collection, or {@code null}.
     * @return the object cast as a collection or wrapped in a collection (never {@code null}).
     *
     * @since 1.6
     */
    public static Collection<?> toCollection(final Object object) {
        if (object == null) {
            return Collections.emptySet();
        }
        if (object instanceof Collection<?>) {
            return (Collection<?>) object;
        }
        if (object.getClass().isArray()) {
            if (object instanceof Object[]) {
                return Arrays.asList((Object[]) object);
            }
            return new AbstractList<Object>() {
                /** Returns the number of elements in the backing array. */
                @Override public int size() {
                    return Array.getLength(object);
                }

                /** Returns the element at the given index. Primitive numbers as returned as instance of their wrapper class. */
                @Override public Object get(final int index) {
                    return Array.get(object, index);
                }

                /** Sets the element at the given index. Primitive numbers shall be given as instance of their wrapper class. */
                @Override public Object set(final int index, final Object value) {
                    final Object old = Array.get(value, index);
                    Array.set(value, index, value);
                    return old;
                }
            };
        }
        return Collections.singleton(object);
    }

    /**
     * Returns the value mapped to the given key cast to the given type,
     * or {@code null} if the map is null or does not contain a value for the key.
     * If the mapped value is non-null but cannot be cast to the given type, then this
     * method throws an {@link IllegalArgumentException} with a message of the form
     * <q>Property ‘{@code key}’ does not accept instances of ‘{@code value.class}’.</q>.
     *
     * <p>This is a helper method for processing a {@code Map} argument containing property values of various
     * kinds, as in the {@link org.apache.sis.referencing.AbstractIdentifiedObject#AbstractIdentifiedObject(Map)
     * AbstractIdentifiedObject} constructor.</p>
     *
     * @param  <T>         the compile-time value of the {@code type} argument.
     * @param  properties  the map of properties from which to get a value, or {@code null} if none.
     * @param  key         the key of the property value to return. Can be {@code null} if the map supports null key.
     * @param  type        the expected type of the property value. Cannot be null.
     * @return the property value for the given key cast to the given type, or {@code null} if none.
     * @throws IllegalArgumentException if a non-null property value exists for the given key but can
     *         not be cast to the given type.
     *
     * @see ArgumentChecks#ensureCanCast(String, Class, Object)
     */
    @SuppressWarnings("unchecked")
    public static <T> T property(final Map<?,?> properties, final Object key, final Class<T> type)
            throws IllegalArgumentException
    {
        if (properties == null) {
            return null;
        }
        final Object value = properties.get(key);
        if (value != null && !type.isInstance(value)) {
            throw new IllegalArgumentException(Errors.forProperties(properties)
                    .getString(Errors.Keys.IllegalPropertyValueClass_3, key, type, value.getClass()));
        }
        return (T) value;
    }

    /**
     * Returns the capacity to give to the {@code HashMap} and {@code HashSet} constructors for holding the
     * given number of elements. This method computes the capacity for the default load factor, which is 0.75.
     * This capacity is applicable to the following classes:
     * {@link java.util.HashSet},
     * {@link java.util.HashMap},
     * {@link java.util.LinkedHashSet} and
     * {@link java.util.LinkedHashMap}.
     * This capacity is <strong>not</strong> applicable to {@link java.util.IdentityHashMap}.
     *
     * <p>Since Java 19, the static factory methods in above-cited classes should be used instead of this method.
     * However, this {@code hashMapCapacity} method is still useful in a few cases where the standard factory methods
     * cannot be invoked, for example because the collection to construct is a subclasses for the standard classes.</p>
     *
     * @param  count  the number of elements to be put into the hash map or hash set.
     * @return the minimal initial capacity to be given to the hash map constructor.
     */
    public static int hashMapCapacity(final int count) {
        // Dividing `count` by 0.75 is equivalent to multiplying by 1.333333… rounded to next integer.
        return (count * 4 + 2) / 3;
    }

    /**
     * Compares element-by-element the values provided by two iterators, in iteration order. Let {@code o1} be an
     * element from the first iterator and {@code o2} the element at the same position from the second iterator.
     * This method returns the result of the first {@code o1.compareTo(o2)} call which returned a value different
     * than zero. If all {@code o1.compareTo(o2)} calls returned zero, then this method returns -1 if {@code it1}
     * iteration finished before {@code it2}, +1 if {@code it2} iteration finished before {@code it1}, or 0 if both
     * iterators finished at the same time.
     *
     * <p>Iterators may return null elements. Null elements are considered "after" any non-null element.</p>
     *
     * @param  <E>  the type of elements returned by the iterators.
     * @param  it1  the first iterator (cannot be null).
     * @param  it2  the second iterator (cannot be null).
     * @return -1 if the content given by the first iterator is considered "before" the content given by the second
     *         iterator, +1 if considered "after", or 0 if considered equal.
     *
     * @since 1.0
     */
    public static <E extends Comparable<E>> int compare(final Iterator<E> it1, final Iterator<? extends E> it2) {
        while (it1.hasNext()) {
            if (!it2.hasNext()) return +1;          // it1 longer than it2.
            final E o1 = it1.next();
            final E o2 = it2.next();
            if (o1 != o2) {
                if (o1 == null) return +1;
                if (o2 == null) return -1;
                final int c = o1.compareTo(o2);
                if (c != 0) return c;
            }
        }
        return it2.hasNext() ? -1 : 0;
    }
}
