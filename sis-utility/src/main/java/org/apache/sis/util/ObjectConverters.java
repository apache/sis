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
import org.apache.sis.util.collection.CollectionsExt;


/**
 * Creates {@link ObjectConverter} instances, or uses them for creating collection views.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
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
        return IdentityConverter.create(type);
    }

    /**
     * Returns a set whose elements are derived <cite>on-the-fly</cite> from the given set.
     * Conversions from the original elements to the derived elements are performed when needed
     * by invoking the {@link ObjectConverter#convert(Object)} method on the given converter.
     *
     * <p>This convenience method delegates to
     * {@link CollectionsExt#derivedSet CollectionsExt.derivedSet(…)}.
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
     * @see CollectionsExt#derivedSet(Set, ObjectConverter)
     */
    public static <S,E> Set<E> derivedSet(final Set<S> storage, final ObjectConverter<S,E> converter) {
        return CollectionsExt.derivedSet(storage, converter);
    }

    /**
     * Returns a map whose whose keys and values are derived <cite>on-the-fly</cite> from the given map.
     * Conversions from the original entries to the derived entries are performed when needed
     * by invoking the {@link ObjectConverter#convert(Object)} method on the given converters.
     *
     * <p>This convenience method delegates to
     * {@link CollectionsExt#derivedMap CollectionsExt.derivedMap(…)}.
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
     * @see CollectionsExt#derivedMap(Map, ObjectConverter, ObjectConverter)
     */
    public static <SK,SV,K,V> Map<K,V> derivedMap(final Map<SK,SV> storage,
                                                  final ObjectConverter<SK,K> keyConverter,
                                                  final ObjectConverter<SV,V> valueConverter)
    {
        return CollectionsExt.derivedMap(storage, keyConverter, valueConverter);
    }

    /**
     * Returns a map whose whose keys are derived <cite>on-the-fly</cite> from the given map.
     * Conversions from the original keys to the derived keys are performed when needed by
     * invoking the {@link ObjectConverter#convert(Object)} method on the given converter.
     *
     * <p>This convenience method delegates to
     * {@link CollectionsExt#derivedMap CollectionsExt.derivedMap(…)}.
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
     * @see CollectionsExt#derivedMap(Map, ObjectConverter, ObjectConverter)
     */
    public static <SK,K,V> Map<K,V> derivedKeys(final Map<SK,V> storage,
                                                final ObjectConverter<SK,K> keyConverter,
                                                final Class<V> valueType)
    {
        ArgumentChecks.ensureNonNull("valueType", valueType);
        return CollectionsExt.derivedMap(storage, keyConverter, IdentityConverter.create(valueType));
    }

    /**
     * Returns a map whose whose values are derived <cite>on-the-fly</cite> from the given map.
     * Conversions from the original values to the derived values are performed when needed by
     * invoking the {@link ObjectConverter#convert(Object)} method on the given converter.
     *
     * <p>This convenience method delegates to
     * {@link CollectionsExt#derivedMap CollectionsExt.derivedMap(…)}.
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
     * @see CollectionsExt#derivedMap(Map, ObjectConverter, ObjectConverter)
     */
    public static <K,SV,V> Map<K,V> derivedValues(final Map<K,SV> storage,
                                                  final Class<K> keyType,
                                                  final ObjectConverter<SV,V> valueConverter)
    {
        ArgumentChecks.ensureNonNull("keyType", keyType);
        return CollectionsExt.derivedMap(storage, IdentityConverter.create(keyType), valueConverter);
    }
}
