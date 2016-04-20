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
import java.util.List;
import java.util.Collection;
import org.apache.sis.util.Static;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.UnmodifiableArrayList;


/**
 * Static methods working on {@link Collection} or {@link CheckedContainer} objects.
 * Unless otherwise noted in the javadoc, every collections returned by the methods
 * in this class implement the {@code CheckedContainer} interface.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 */
public final class Containers extends Static {
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
     * Returns an unmodifiable view of the given array. A direct reference to the given array is
     * retained (i.e. the array is <strong>not</strong> cloned). Consequently the given array
     * shall not be modified after construction if the returned list is intended to be immutable.
     *
     * <p>The returned list implements the {@link CheckedContainer} interface. The value returned by
     * its {@link CheckedContainer#getElementType()} method is inferred from the array component type.
     * Because arrays in the Java language are covariant (at the contrary of collections),
     * the list type have to be {@code <? extends E>} instead than {@code <E>}.</p>
     *
     * @param  <E> The base type of elements in the list.
     * @param  array The array to wrap, or {@code null} if none.
     * @return The given array wrapped in an unmodifiable list, or {@code null} if the given array was null.
     *
     * @see java.util.Arrays#asList(Object[])
     */
    public static <E> List<? extends E> unmodifiableList(final E... array) {
        return UnmodifiableArrayList.wrap(array);
    }

    /**
     * Returns an unmodifiable view of a subregion of the given array. A direct reference to the
     * given array is retained (i.e. the array is <strong>not</strong> cloned). Consequently the
     * specified sub-region of the given array shall not be modified after construction if the
     * returned list is intended to be immutable.
     *
     * <p>The returned list implements the {@link CheckedContainer} interface. The value returned by
     * its {@link CheckedContainer#getElementType()} method is inferred from the array component type.
     * Because arrays in the Java language are covariant (at the contrary of collections),
     * the list type have to be {@code <? extends E>} instead than {@code <E>}.</p>
     *
     * @param  <E>   The type of elements in the list.
     * @param  array The array to wrap (can not be null).
     * @param  lower Low endpoint (inclusive) of the sublist.
     * @param  upper High endpoint (exclusive) of the sublist.
     * @return The given array wrapped in an unmodifiable list.
     * @throws IndexOutOfBoundsException If the lower or upper value are out of bounds.
     */
    public static <E> List<? extends E> unmodifiableList(final E[] array, final int lower, final int upper) {
        ArgumentChecks.ensureNonNull("array", array);
        ArgumentChecks.ensureValidIndexRange(array.length, lower, upper);
        return UnmodifiableArrayList.wrap(array, lower, upper);
    }

    /**
     * Returns a set whose elements are derived <cite>on-the-fly</cite> from the given set.
     * Conversions from the original elements to the derived elements are performed when needed
     * by invoking the {@link ObjectConverter#apply(Object)} method on the given converter.
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
     * {@code converter.apply(S)} returns {@code null}. As a consequence of this sentinel
     * value usage, the derived set can not contain {@code null} elements.</p>
     *
     * <p>The returned set can be serialized if the given set and converter are serializable.
     * The returned set is not synchronized by itself, but is nevertheless thread-safe if the
     * given set (including its iterator) and converter are thread-safe.</p>
     *
     * @param  <S>       The type of elements in the storage (original) set.
     * @param  <E>       The type of elements in the derived set.
     * @param  storage   The storage set containing the original elements, or {@code null}.
     * @param  converter The converter from the elements in the storage set to the elements in the derived set.
     * @return A view over the {@code storage} set containing all elements converted by the given converter,
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
     * Returns a map whose keys and values are derived <cite>on-the-fly</cite> from the given map.
     * Conversions from the original entries to the derived entries are performed when needed
     * by invoking the {@link ObjectConverter#apply(Object)} method on the given converters.
     * Those conversions are repeated every time a {@code Map} method is invoked; there is no cache.
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
     * As a consequence of this sentinel key usage, the derived map can not contain {@code null} keys.
     * It may contain {@code null} values however.</p>
     *
     * <p>The returned map can be serialized if the given map and converters are serializable.
     * The returned map is <strong>not</strong> thread-safe.</p>
     *
     * <p>The returned map does not implement the {@link CheckedContainer} interface since {@code Map}
     * is not a {@code Collection} sub-type, but the derived map {@linkplain Map#keySet() key set} and
     * {@linkplain Map#entrySet() entry set} do.</p>
     *
     * @param <SK>         The type of keys   in the storage map.
     * @param <SV>         The type of values in the storage map.
     * @param <K>          The type of keys   in the derived map.
     * @param <V>          The type of values in the derived map.
     * @param storage      The storage map containing the original entries, or {@code null}.
     * @param keyConverter The converter from the keys in the storage map to the keys in the derived map.
     * @param valueConverter The converter from the values in the storage map to the values in the derived map.
     * @return A view over the {@code storage} map containing all entries converted by the given converters,
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
     * Returns the value mapped to the given key casted to the given type,
     * or {@code null} if the map is null or does not contain a value for the key.
     * If the mapped value is non-null but can not be casted to the given type, then this
     * method throws an {@link IllegalArgumentException} with a message of the form
     * <cite>"Property ‘{@code key}’ does not accept instances of ‘{@code value.class}’."</cite>.
     *
     * <p>This is a helper method for processing a {@code Map} argument containing property values of various
     * kinds, as in the {@link org.apache.sis.referencing.AbstractIdentifiedObject#AbstractIdentifiedObject(Map)
     * AbstractIdentifiedObject} constructor.</p>
     *
     * @param  <T>        The compile-time value of the {@code type} argument.
     * @param  properties The map of properties from which to get a value, or {@code null} if none.
     * @param  key        The key of the property value to return. Can be {@code null} if the map supports null key.
     * @param  type       The expected type of the property value. Can not be null.
     * @return The property value for the given key casted to the given type, or {@code null} if none.
     * @throws IllegalArgumentException If a non-null property value exists for the given key but can
     *         not be casted to the given type.
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
            throw new IllegalArgumentException(Errors.getResources(properties)
                    .getString(Errors.Keys.IllegalPropertyValueClass_3, key, type, value.getClass()));
        }
        return (T) value;
    }

    /**
     * Returns the capacity to be given to the {@link java.util.HashMap#HashMap(int) HashMap}
     * constructor for holding the given number of elements. This method computes the capacity
     * for the default <cite>load factor</cite>, which is 0.75.
     *
     * <p>The same calculation can be used for {@link java.util.LinkedHashMap} and
     * {@link java.util.HashSet} as well, which are built on top of {@code HashMap}.
     * However it is not needed for {@link java.util.IdentityHashMap}.</p>
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
