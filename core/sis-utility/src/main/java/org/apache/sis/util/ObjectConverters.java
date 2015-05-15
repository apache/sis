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
import java.util.Set;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.internal.converter.IdentityConverter;
import org.apache.sis.internal.converter.SystemRegistry;


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
 * <div class="note"><b>Example:</b>
 * the following code converts instances in a collection from type {@code S} to type {@code T},
 * where the types are unknown at compile-time. Note that the converter is obtained only once
 * before to be applied to every elements in the loop.
 *
 * {@preformat java
 *     Class<S> sourceType = ...
 *     Class<T> targetType = ...
 *     Collection<S> sources = ...;
 *     Collection<T> targets = ...;
 *     ObjectConverter<S,T> converter = ObjectConverters.find(sourceType, targetType);
 *     for (S source : sources) {
 *         targets.add(converter.apply(source));
 *     }
 * }
 * </div>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see ObjectConverter
 */
public final class ObjectConverters extends Static {
    /**
     * Do not allow instantiation of this class.
     */
    private ObjectConverters() {
    }

    /**
     * Returns an identity converter for objects of the given type.
     *
     * @param  <T>  The object type.
     * @param  type The object type.
     * @return An identity converter for objects of the given type.
     */
    public static <T> ObjectConverter<T,T> identity(final Class<T> type) {
        ArgumentChecks.ensureNonNull("type", type);
        return new IdentityConverter<T,T>(type, type, null).unique();
    }

    /**
     * Returns a converter for the specified source and target classes.
     *
     * @param  <S> The source class.
     * @param  <T> The target class.
     * @param  source The source class.
     * @param  target The target class, or {@code Object.class} for any.
     * @return The converter from the specified source class to the target class.
     * @throws UnconvertibleObjectException if no converter is found.
     */
    public static <S,T> ObjectConverter<? super S, ? extends T> find(final Class<S> source, final Class<T> target)
            throws UnconvertibleObjectException
    {
        ArgumentChecks.ensureNonNull("source", source);
        ArgumentChecks.ensureNonNull("target", target);
        return SystemRegistry.INSTANCE.find(source, target);
    }

    /**
     * Converts the given value to the given type. This convenience method shall be used only for
     * rare conversions. For converting many instances between the same source and target classes,
     * consider invoking {@link #find(Class, Class)} instead in order to reuse the same converter
     * for all values to convert.
     *
     * @param  <T>    The type of the {@code target} class.
     * @param  value  The value to convert, or {@code null}.
     * @param  target The target class.
     * @return The converted value (may be {@code null}).
     * @throws UnconvertibleObjectException if the given value can not be converted.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    public static <T> T convert(Object value, final Class<T> target) throws UnconvertibleObjectException {
        ArgumentChecks.ensureNonNull("target", target);
        if (!target.isInstance(value) && value != null) {
            value = ((ObjectConverter) SystemRegistry.INSTANCE.find(value.getClass(), target)).apply(value);
        }
        return (T) value;
    }

    /**
     * Returns a set whose elements are derived <cite>on-the-fly</cite> from the given set.
     * Conversions from the original elements to the derived elements are performed when needed
     * by invoking the {@link ObjectConverter#apply(Object)} method on the given converter.
     *
     * <p>This convenience method delegates to
     * {@link Containers#derivedSet Containers.derivedSet(…)}.
     * See the javadoc of the above method for more information.
     *
     * @param  <S>       The type of elements in the storage (original) set.
     * @param  <E>       The type of elements in the derived set.
     * @param  storage   The storage set containing the original elements, or {@code null}.
     * @param  converter The converter from the elements in the storage set to the elements
     *                   in the derived set.
     * @return A view over the {@code storage} set containing all elements converted by the given
     *         converter, or {@code null} if {@code storage} was null.
     *
     * @see Containers#derivedSet(Set, ObjectConverter)
     */
    public static <S,E> Set<E> derivedSet(final Set<S> storage, final ObjectConverter<S,E> converter) {
        return Containers.derivedSet(storage, converter);
    }

    /**
     * Returns a map whose keys and values are derived <cite>on-the-fly</cite> from the given map.
     * Conversions from the original entries to the derived entries are performed when needed
     * by invoking the {@link ObjectConverter#apply(Object)} method on the given converters.
     *
     * <p>This convenience method delegates to
     * {@link Containers#derivedMap Containers.derivedMap(…)}.
     * See the javadoc of the above method for more information.
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
     * @see Containers#derivedMap(Map, ObjectConverter, ObjectConverter)
     */
    public static <SK,SV,K,V> Map<K,V> derivedMap(final Map<SK,SV> storage,
                                                  final ObjectConverter<SK,K> keyConverter,
                                                  final ObjectConverter<SV,V> valueConverter)
    {
        return Containers.derivedMap(storage, keyConverter, valueConverter);
    }

    /**
     * Returns a map whose keys are derived <cite>on-the-fly</cite> from the given map.
     * Conversions from the original keys to the derived keys are performed when needed by
     * invoking the {@link ObjectConverter#apply(Object)} method on the given converter.
     *
     * <p>This convenience method delegates to
     * {@link Containers#derivedMap Containers.derivedMap(…)}.
     * See the javadoc of the above method for more information.
     *
     * @param <SK>         The type of keys   in the storage map.
     * @param <K>          The type of keys   in the derived map.
     * @param <V>          The type of values in the storage and derived map.
     * @param storage      The storage map containing the original entries, or {@code null}.
     * @param keyConverter The converter from the keys in the storage map to the keys in the derived map.
     * @param valueType    The type of values in the storage and derived map.
     * @return A view over the {@code storage} map containing all entries with the keys converted
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
     * Returns a map whose values are derived <cite>on-the-fly</cite> from the given map.
     * Conversions from the original values to the derived values are performed when needed by
     * invoking the {@link ObjectConverter#apply(Object)} method on the given converter.
     *
     * <p>This convenience method delegates to
     * {@link Containers#derivedMap Containers.derivedMap(…)}.
     * See the javadoc of the above method for more information.
     *
     * @param <K>          The type of keys in the storage and derived map.
     * @param <SV>         The type of values in the storage map.
     * @param <V>          The type of values in the derived map.
     * @param storage      The storage map containing the original entries, or {@code null}.
     * @param keyType      The type of keys in the storage and derived map.
     * @param valueConverter The converter from the values in the storage map to the values in the derived map.
     * @return A view over the {@code storage} map containing all entries with the values converted
     *         by the given converter, or {@code null} if {@code storage} was null.
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
