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

import java.util.Set;
import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;
import java.io.ObjectStreamException;
import java.io.Serializable;
import net.jcip.annotations.Immutable;
import org.apache.sis.math.FunctionProperty;


/**
 * An object converter which returns the source unchanged.
 *
 * @param <T> The base type of source and converted objects.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-3.01)
 * @version 0.3
 * @module
 */
@Immutable
final class IdentityConverter<T> implements ObjectConverter<T,T>, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7203549932226245206L;

    /**
     * Identity converters created in the JVM, for sharing unique instances.
     * Use weak keys in order to allow class unloading.
     */
    private static final Map<Class<?>, IdentityConverter<?>> CACHE = new WeakHashMap<>();

    /**
     * Returns an identity converter for the given type.
     */
    public static synchronized <T> IdentityConverter<T> create(final Class<T> type) {
        @SuppressWarnings("unchecked")
        IdentityConverter<T> converter = (IdentityConverter<T>) CACHE.get(type);
        if (converter == null) {
            converter = new IdentityConverter<>(type);
            CACHE.put(type, converter);
        }
        return converter;
    }

    /**
     * The type of source and converted objects.
     */
    private final Class<T> type;

    /**
     * Creates a new identity converter.
     *
     * @param type The type of source and converted objects.
     */
    private IdentityConverter(final Class<T> type) {
        this.type = type;
    }

    /**
     * Returns the properties of this converter.
     * This method returns a new {@link EnumSet} instead than returning a constant, because
     * creating {@code EnumSet} is cheap and the standard JDK implementation has optimizations
     * for bulk operations between {@code EnumSet} instances. Those optimizations are lost (at
     * least on JDK6) is we wrap the {@code EnumSet} in a {@code Collections.unmodifiableSet}
     * view.
     */
    @Override
    public Set<FunctionProperty> properties() {
        return EnumSet.allOf(FunctionProperty.class);
    }

    /**
     * Returns the type for source objects.
     */
    @Override
    public Class<T> getSourceClass() {
        return type;
    }

    /**
     * Returns the type of converted objects.
     */
    @Override
    public Class<T> getTargetClass() {
        return type;
    }

    /**
     * Returns the given object unchanged.
     */
    @Override
    public T convert(final T source) {
        return source;
    }

    /**
     * Returns {@code this}, since this converter is its own inverse.
     */
    @Override
    public ObjectConverter<T,T> inverse() {
        return this;
    }

    /**
     * Invoked on deserialization for resolving to a unique instance.
     */
    private Object readResolve() throws ObjectStreamException {
        synchronized (IdentityConverter.class) {
            @SuppressWarnings("unchecked")
            final IdentityConverter<T> converter = (IdentityConverter<T>) CACHE.get(type);
            if (converter != null) {
                return converter;
            }
            CACHE.put(type, this);
        }
        return this;
    }
}
