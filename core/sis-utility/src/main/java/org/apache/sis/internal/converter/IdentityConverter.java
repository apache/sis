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
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.math.FunctionProperty;


/**
 * An object converter which returns the source unchanged.
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus inherently thread-safe.
 *
 * @param <S> The base type of source objects.
 * @param <T> The base type of converted objects.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see org.apache.sis.util.ObjectConverters#identity(Class)
 */
public final class IdentityConverter<T, S extends T> extends SystemConverter<S,T> {
    // JDK6 NOTE: Order of above <T> and <S> parameters is reversed compared to the
    // JDK7 branch, because the JDK6 compiler does not supports forward reference.

    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4410848323263094741L;

    /**
     * The inverse converter specified at construction time, or {@code null} if none.
     */
    private final ObjectConverter<T, S> inverse;

    /**
     * Creates a new identity converter.
     *
     * @param sourceClass The {@linkplain #getSourceClass() source class}.
     * @param targetClass The {@linkplain #getTargetClass() target class}.
     * @param inverse     The inverse converter, or {@code null} if none.
     */
    @SuppressWarnings("unchecked")
    public IdentityConverter(final Class<S> sourceClass, final Class<T> targetClass,
            ObjectConverter<T, S> inverse)
    {
        super(sourceClass, targetClass);
        if (inverse == null && sourceClass == targetClass) {
            inverse = (ObjectConverter<T,S>) this;
        }
        this.inverse = inverse;
    }

    /**
     * Returns the properties of this converter.
     * This method returns a new {@link EnumSet} instead than returning a constant, because
     * creating {@code EnumSet} is cheap and the standard JDK implementation has optimizations
     * for bulk operations between {@code EnumSet} instances. Those optimizations are lost (at
     * least on JDK6) is we wrap the {@code EnumSet} in a {@code Collections.unmodifiableSet} view.
     *
     * @return The manners in which source values are mapped to target values.
     */
    @Override
    public Set<FunctionProperty> properties() {
        final EnumSet<FunctionProperty> properties = EnumSet.allOf(FunctionProperty.class);
        if (inverse == null) {
            properties.remove(FunctionProperty.INVERTIBLE);
        }
        return properties;
    }

    /**
     * Returns the inverse converter, if any.
     *
     * @return A converter for converting instances of <var>T</var> back to instances of <var>S</var>.
     */
    @Override
    public ObjectConverter<T,S> inverse() throws UnsupportedOperationException {
        return (inverse != null) ? inverse : super.inverse();
    }

    /**
     * Returns the given object unchanged.
     *
     * @param source The value to convert.
     * @return The given value unchanged.
     */
    @Override
    public T apply(final S source) {
        return source;
    }
}
