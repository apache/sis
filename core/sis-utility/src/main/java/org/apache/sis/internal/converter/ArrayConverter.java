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
import java.lang.reflect.Array;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.Numbers;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;


/**
 * Handles conversions between arrays. This converter delegates element conversions to an other converter given at
 * construction time. If the source and target types of this converter are {@code <S[]>} and {@code <T[]>}, then
 * the source and target types of the element converter shall be {@code <? super S>} and {@code <? extends T>}
 * respectively.
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
final class ArrayConverter<S,T> extends SystemConverter<S,T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 7108976709306360737L;

    /**
     * The function properties which can be preserved by this converter.
     * We do not preserve ordering because we don't have a universal criterion for array comparisons.
     * For example we don't know how the user wants to handle arrays of different lengths, or null elements.
     */
    private static final EnumSet<FunctionProperty> PROPERTIES = EnumSet.of(FunctionProperty.INVERTIBLE,
            FunctionProperty.INJECTIVE, FunctionProperty.SURJECTIVE);

    /**
     * The converter for array elements. The source and target types shall be compatible with the array component
     * types of {@code <S>} and {@code <T>} (this constraint can not be expressed by JDK 7 parameterized types).
     */
    private final ObjectConverter<?,?> converter;

    /**
     * Creates a new converter for the given source and target classes.
     *
     * @param sourceClass The {@linkplain #getSourceClass() source class}.
     * @param targetClass The {@linkplain #getTargetClass() target class}.
     * @param converter   The converter for array elements. The source and target types shall be
     *                    the array component types of {@code <S>} and {@code <T>}.
     */
    ArrayConverter(final Class<S> sourceClass, final Class<T> targetClass, final ObjectConverter<?,?> converter) {
        super(sourceClass, targetClass);
        assert converter.getSourceClass().isAssignableFrom(Numbers.primitiveToWrapper(sourceClass.getComponentType())) : sourceClass;
        assert Numbers.primitiveToWrapper(targetClass.getComponentType()).isAssignableFrom(converter.getTargetClass()) : targetClass;
        this.converter = converter;
    }

    /**
     * Infers the properties of this converter from the properties of the elements converter.
     */
    @Override
    public Set<FunctionProperty> properties() {
        final EnumSet<FunctionProperty> properties = EnumSet.copyOf(converter.properties());
        properties.retainAll(PROPERTIES);
        return properties;
    }

    /**
     * Converts the given array.
     */
    @Override
    @SuppressWarnings("unchecked")
    public T apply(S source) throws UnconvertibleObjectException {
        if (source == null) {
            return null;
        }
        final int length = Array.getLength(source);
        final T target = (T) Array.newInstance(targetClass.getComponentType(), length);
        for (int i=0; i<length; i++) {
            Array.set(target, i, ((ObjectConverter) converter).apply(Array.get(source, i)));
        }
        return target;
    }
}
