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
package org.apache.sis.internal.converter;

import java.io.Serializable;
import net.jcip.annotations.Immutable;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;


/**
 * Handles conversions from {@link CharSequence} to {@link String}, then forward
 * to an other converter from {@link String} to various objects. Instance of this
 * converter are not registered in {@link ConverterRegistry} like other converters
 * because we avoid registering converter expecting interfaces as their source.
 *
 * <p>The main purpose of this class is to support the conversion of
 * {@link org.opengis.util.InternationalString}.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.02)
 * @version 0.3
 * @module
 */
@Immutable
final class CharSequenceConverter<T> extends SurjectiveConverter<CharSequence,T> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 2591675151163578878L;

    /**
     * A converter from {@link CharSequence} to {@link String}.
     */
    static final CharSequenceConverter<String> STRING =
            new CharSequenceConverter<>(String.class, IdentityConverter.create(String.class));

    /**
     * The target type requested by the user. We retain this type explicitly instead
     * than querying {@code next.getTargetType()} because it may be a super-class of
     * the later.
     */
    private final Class<T> targetType;

    /**
     * The converter to apply after this one.
     */
    private final ObjectConverter<? super String, ? extends T> next;

    /**
     * Creates a new converter from {@link CharSequence} to the given target type.
     *
     * @param targetType The target type requested by the user.
     * @param next The converter to apply after this one.
     */
    private CharSequenceConverter(final Class<T> targetType, final ObjectConverter<? super String, ? extends T> next) {
        this.targetType = targetType;
        this.next = next;
    }

    /**
     * Creates a new converter from {@link CharSequence} to the given target type.
     *
     * @param targetType The target type requested by the user.
     * @param next The converter to apply after this one.
     */
    @SuppressWarnings("unchecked")
    public static <T> ObjectConverter<? super CharSequence, ? extends T> create(
            final Class<T> targetType, final ObjectConverter<? super String, ? extends T> next)
    {
        if (next.getSourceClass().isAssignableFrom(CharSequence.class)) {
            return (ObjectConverter<? super CharSequence, ? extends T>) next;
        }
        return new CharSequenceConverter<>(targetType, next);
    }

    /**
     * Returns the source class, which is always {@link CharSequence}.
     */
    @Override
    public final Class<CharSequence> getSourceClass() {
        return CharSequence.class;
    }

    /**
     * Returns the target class.
     */
    @Override
    public final Class<T> getTargetClass() {
        return targetType;
    }

    /**
     * Converts an object to an object of the target type.
     */
    @Override
    public T convert(final CharSequence source) throws UnconvertibleObjectException {
        if (targetType.isInstance(source)) {
            return targetType.cast(source);
        }
        return next.convert(source != null ? source.toString() : null);
    }
}
