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
package org.apache.sis.util;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.converter.IdentityConverter;
import org.apache.sis.converter.SystemRegistry;


/**
 * Static methods for creating {@link ObjectConverter} instances or collection views based on converters.
 * Converters are created by the following methods:
 *
 * <ul>
 *   <li>{@link #identity(Class)}</li>
 *   <li>{@link #find(Class, Class)}</li>
 * </ul>
 *
 * Converters can be used for creating derived collections by the following methods:
 *
 * <ul>
 *   <li>{@link #derivedSet(Set, ObjectConverter)}</li>
 *   <li>{@link #derivedMap(Map, ObjectConverter, ObjectConverter)}</li>
 *   <li>{@link #derivedKeys(Map, ObjectConverter, Class)}</li>
 *   <li>{@link #derivedValues(Map, Class, ObjectConverter)}</li>
 * </ul>
 *
 * <h2>Example</h2>
 * the following code converts instances in a collection from type {@code S} to type {@code T},
 * where the types are unknown at compile-time. Note that the converter is obtained only once
 * before to be applied to every elements in the loop.
 *
 * {@snippet lang="java" :
 *     Class<S> sourceType = ...
 *     Class<T> targetType = ...
 *     Collection<S> sources = ...;
 *     Collection<T> targets = ...;
 *     ObjectConverter<S,T> converter = ObjectConverters.find(sourceType, targetType);
 *     for (S source : sources) {
 *         targets.add(converter.apply(source));
 *     }
 *     }
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.6
 *
 * @see ObjectConverter
 *
 * @since 0.3
 */
public final class ObjectConverters {
    /**
     * Do not allow instantiation of this class.
     */
    private ObjectConverters() {
    }

    /**
     * Returns an identity converter for objects of the given type.
     *
     * @param  <T>   the object type.
     * @param  type  the object type.
     * @return an identity converter for objects of the given type.
     */
    public static <T> ObjectConverter<T,T> identity(final Class<T> type) {
        return new IdentityConverter<>(Objects.requireNonNull(type), type, null).unique();
    }

    /**
     * Returns a converter for the specified source and target classes.
     *
     * @param  <S>     the source class.
     * @param  <T>     the target class.
     * @param  source  the source class.
     * @param  target  the target class, or {@code Object.class} for any.
     * @return the converter from the specified source class to the target class.
     * @throws UnconvertibleObjectException if no converter is found.
     */
    @SuppressWarnings("unchecked")
    public static <S,T> ObjectConverter<? super S, ? extends T> find(
            final Class<? extends S> source,
            final Class<?  super  T> target)
            throws UnconvertibleObjectException
    {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("target", target);
        /*
         * The `(Class<S>)` cast is safe because the generic type of the returned converter is `<? super S>`.
         * Therefore, even if the actual class is a subtype of `S`, the `? super S` declaration stay valid.
         * A similar argument applies also to the `(Class<T>)` cast.
         */
        return SystemRegistry.INSTANCE.find((Class<S>) source, (Class<T>) target);
    }

    /**
     * Converts the given value to the given type. This convenience method shall be used only for
     * rare conversions. For converting many instances between the same source and target classes,
     * consider invoking {@link #find(Class, Class)} instead in order to reuse the same converter
     * for all values to convert.
     *
     * @param  <T>     the type of the {@code target} class.
     * @param  value   the value to convert, or {@code null}.
     * @param  target  the target class.
     * @return the converted value (may be {@code null}).
     * @throws UnconvertibleObjectException if the given value cannot be converted.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public static <T> T convert(Object value, final Class<T> target) throws UnconvertibleObjectException {
        if (!target.isInstance(value) && value != null) {
            value = ((ObjectConverter) SystemRegistry.INSTANCE.find(value.getClass(), target)).apply(value);
        }
        return (T) value;
    }

    /**
     * Returns a set whose elements are derived <i>on-the-fly</i> from the given set.
     * Conversions from the original elements to the derived elements are performed when needed
     * by invoking the {@link ObjectConverter#apply(Object)} method on the given converter.
     *
     * <p>This convenience method delegates to
     * {@link Containers#derivedSet Containers.derivedSet(…)}.
     * See the javadoc of the above method for more information.
     *
     * @param  <S>        the type of elements in the storage (original) set.
     * @param  <E>        the type of elements in the derived set.
     * @param  storage    the storage set containing the original elements, or {@code null}.
     * @param  converter  the converter from the elements in the storage set to the elements in the derived set.
     * @return a view over the {@code storage} set containing all elements converted by the given converter, or
     *         {@code null} if {@code storage} was null.
     *
     * @see Containers#derivedSet(Set, ObjectConverter)
     */
    public static <S,E> Set<E> derivedSet(final Set<S> storage, final ObjectConverter<S,E> converter) {
        return Containers.derivedSet(storage, converter);
    }

    /**
     * Returns a map whose keys and values are derived <i>on-the-fly</i> from the given map.
     * Conversions from the original entries to the derived entries are performed when needed
     * by invoking the {@link ObjectConverter#apply(Object)} method on the given converters.
     *
     * <p>This convenience method delegates to
     * {@link Containers#derivedMap Containers.derivedMap(…)}.
     * See the javadoc of the above method for more information.
     *
     * @param  <SK>            the type of keys   in the storage map.
     * @param  <SV>            the type of values in the storage map.
     * @param  <K>             the type of keys   in the derived map.
     * @param  <V>             the type of values in the derived map.
     * @param  storage         the storage map containing the original entries, or {@code null}.
     * @param  keyConverter    the converter from the keys in the storage map to the keys in the derived map.
     * @param  valueConverter  the converter from the values in the storage map to the values in the derived map.
     * @return a view over the {@code storage} map containing all entries converted by the given converters, or
     *         {@code null} if {@code storage} was null.
     *
     * @see Containers#derivedMap(Map, ObjectConverter, ObjectConverter)
     */
    public static <SK,SV,K,V> Map<K,V> derivedMap(final Map<SK,SV> storage,
                                                  final ObjectConverter<SK,K> keyConverter,
                                                  final ObjectConverter<SV,V> valueConverter)
    {
        return Containers.derivedMap(storage, keyConverter, valueConverter);
    }

    /**
     * Returns a map whose keys are derived <i>on-the-fly</i> from the given map.
     * Conversions from the original keys to the derived keys are performed when needed by
     * invoking the {@link ObjectConverter#apply(Object)} method on the given converter.
     *
     * <p>This convenience method delegates to
     * {@link Containers#derivedMap Containers.derivedMap(…)}.
     * See the javadoc of the above method for more information.
     *
     * @param  <SK>          the type of keys   in the storage map.
     * @param  <K>           the type of keys   in the derived map.
     * @param  <V>           the type of values in the storage and derived map.
     * @param  storage       the storage map containing the original entries, or {@code null}.
     * @param  keyConverter  the converter from the keys in the storage map to the keys in the derived map.
     * @param  valueType     the type of values in the storage and derived map.
     * @return a view over the {@code storage} map containing all entries with the keys converted
     *         by the given converter, or {@code null} if {@code storage} was null.
     *
     * @see Containers#derivedMap(Map, ObjectConverter, ObjectConverter)
     */
    public static <SK,K,V> Map<K,V> derivedKeys(final Map<SK,V> storage,
                                                final ObjectConverter<SK,K> keyConverter,
                                                final Class<V> valueType)
    {
        ArgumentChecks.ensureNonNull("valueType", valueType);
        return Containers.derivedMap(storage, keyConverter, identity(valueType));
    }

    /**
     * Returns a map whose values are derived <i>on-the-fly</i> from the given map.
     * Conversions from the original values to the derived values are performed when needed by
     * invoking the {@link ObjectConverter#apply(Object)} method on the given converter.
     *
     * <p>This convenience method delegates to
     * {@link Containers#derivedMap Containers.derivedMap(…)}.
     * See the javadoc of the above method for more information.
     *
     * @param  <K>             the type of keys in the storage and derived map.
     * @param  <SV>            the type of values in the storage map.
     * @param  <V>             the type of values in the derived map.
     * @param  storage         the storage map containing the original entries, or {@code null}.
     * @param  keyType         the type of keys in the storage and derived map.
     * @param  valueConverter  the converter from the values in the storage map to the values in the derived map.
     * @return a view over the {@code storage} map containing all entries with the values converted by the given
     *         converter, or {@code null} if {@code storage} was null.
     *
     * @see Containers#derivedMap(Map, ObjectConverter, ObjectConverter)
     */
    public static <K,SV,V> Map<K,V> derivedValues(final Map<K,SV> storage,
                                                  final Class<K> keyType,
                                                  final ObjectConverter<SV,V> valueConverter)
    {
        ArgumentChecks.ensureNonNull("keyType", keyType);
        return Containers.derivedMap(storage, identity(keyType), valueConverter);
    }
}
