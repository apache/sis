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
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Iterator;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.CollectionsExt;
import org.apache.sis.util.internal.shared.UnmodifiableArrayList;


/**
 * Static methods working on various types of objects that can be viewed as collections.
 * The types include {@link Collection}, {@link CheckedContainer} or {@code Class<Enum>}.
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
     * Returns an unmodifiable view of the given array. A direct reference to the given array is
     * retained (i.e. the array is <strong>not</strong> cloned). Consequently, the given array
     * shall not be modified after construction if the returned list is intended to be immutable.
     *
     * <p>The returned list implements the {@link CheckedContainer} interface. The value returned by
     * its {@link CheckedContainer#getElementType()} method is inferred from the array component type.
     * Because arrays in the Java language are covariant (at the contrary of collections),
     * the list type have to be {@code <? extends E>} instead of {@code <E>}.</p>
     *
     * @param  <E>    the base type of elements in the list.
     * @param  array  the array to wrap, or {@code null} if none.
     * @return the given array wrapped in an unmodifiable list, or {@code null} if the given array was null.
     *
     * @see java.util.Arrays#asList(Object[])
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <E> List<? extends E> unmodifiableList(final E... array) {
        return UnmodifiableArrayList.wrap(array);
    }

    /**
     * Returns an unmodifiable view of a subregion of the given array. A direct reference to the
     * given array is retained (i.e. the array is <strong>not</strong> cloned). Consequently, the
     * specified sub-region of the given array shall not be modified after construction if the
     * returned list is intended to be immutable.
     *
     * <p>The returned list implements the {@link CheckedContainer} interface. The value returned by
     * its {@link CheckedContainer#getElementType()} method is inferred from the array component type.
     * Because arrays in the Java language are covariant (at the contrary of collections),
     * the list type have to be {@code <? extends E>} instead of {@code <E>}.</p>
     *
     * @param  <E>    the type of elements in the list.
     * @param  array  the array to wrap (cannot be null).
     * @param  lower  low endpoint (inclusive) of the sublist.
     * @param  upper  high endpoint (exclusive) of the sublist.
     * @return the given array wrapped in an unmodifiable list.
     * @throws IndexOutOfBoundsException if the lower or upper value are out of bounds.
     */
    public static <E> List<? extends E> unmodifiableList(final E[] array, final int lower, final int upper) {
        Objects.checkFromToIndex(lower, upper, array.length);
        return UnmodifiableArrayList.wrap(array, lower, upper);
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
     * Returns in an unmodifiable set the names of all enumeration values of the given type.
     * The names are obtained by {@link Enum#name()}, which guarantees that there is no duplicated values.
     * The iteration order is the declaration order of the enumeration values.
     *
     * @param  type  type of the enumeration for which to get the names.
     * @return the names viewed as an unmodifiable set.
     *
     * @since 1.6
     */
    public static Set<String> namesOf(final Class<? extends Enum<?>> type) {
        return CollectionsExt.viewAsSet(derivedList(Arrays.asList(type.getEnumConstants()), Enum::name));
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
