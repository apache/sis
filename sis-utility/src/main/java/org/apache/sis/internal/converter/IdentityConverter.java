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

import java.util.Set;
import java.util.EnumSet;
import net.jcip.annotations.Immutable;
import org.apache.sis.math.FunctionProperty;


/**
 * An object converter which returns the source unchanged.
 *
 * @param <S> The base type of source objects.
 * @param <T> The base type of converted objects.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-3.01)
 * @version 0.3
 * @module
 *
 * @see org.apache.sis.util.ObjectConverters#identity(Class)
 */
@Immutable
public final class IdentityConverter<S extends T, T> extends SystemConverter<S,T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7203549932226245206L;

    /**
     * Creates a new identity converter.
     *
     * @param sourceClass The {@linkplain #getSourceClass() source class}.
     * @param targetClass The {@linkplain #getTargetClass() target class}.
     */
    public IdentityConverter(final Class<S> sourceClass, final Class<T> targetClass) {
        super(sourceClass, targetClass);
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
        final EnumSet<FunctionProperty> properties = EnumSet.allOf(FunctionProperty.class);
        if (sourceClass != targetClass) {
            // Conservative choice (actually we don't really know).
            properties.remove(FunctionProperty.INVERTIBLE);
        }
        return properties;
    }

    /**
     * Returns the given object unchanged.
     *
     * @param source The value to convert.
     */
    @Override
    public T convert(final S source) {
        return source;
    }
}
