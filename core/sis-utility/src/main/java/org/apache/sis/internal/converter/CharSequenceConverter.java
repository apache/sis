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

import java.util.EnumSet;
import java.util.Set;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.math.FunctionProperty;
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
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable, and thus inherently thread-safe,
 * if the converter given to the constructor is also immutable.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
final class CharSequenceConverter<T> extends SystemConverter<CharSequence,T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -2853169224777674260L;

    /**
     * The converter to apply after this one.
     */
    private final ObjectConverter<? super String, ? extends T> next;

    /**
     * Creates a new converter from {@link CharSequence} to the given target type.
     *
     * @param targetClass The target class requested by the user.
     * @param next The converter to apply after this one.
     */
    CharSequenceConverter(final Class<T> targetClass, final ObjectConverter<? super String, ? extends T> next) {
        super(CharSequence.class, targetClass);
        this.next = next;
    }

    /**
     * Converts an object to an object of the target type.
     */
    @Override
    public T apply(final CharSequence source) throws UnconvertibleObjectException {
        if (targetClass.isInstance(source)) {
            return targetClass.cast(source);
        }
        return next.apply(source != null ? source.toString() : null);
    }

    /**
     * Returns the properties of the converter given at construction time minus
     * {@link FunctionProperty#INJECTIVE}, because we don't know how many source
     * {@code CharSequence}s can produce the same {@code String}.
     */
    @Override
    public Set<FunctionProperty> properties() {
        final EnumSet<FunctionProperty> properties = EnumSet.copyOf(next.properties());
        properties.remove(FunctionProperty.INJECTIVE);
        return properties;
    }
}
