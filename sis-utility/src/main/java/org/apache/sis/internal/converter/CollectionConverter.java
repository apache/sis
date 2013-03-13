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

import java.util.Collection;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import net.jcip.annotations.Immutable;
import org.apache.sis.math.FunctionProperty;


/**
 * Handles conversions from {@link Collection} to various objects.
 * The source class is fixed to {@code Collection}. The target class is determined
 * by the inner class which extends this {@code CollectionConverter} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.02)
 * @version 0.3
 * @module
 */
@Immutable
abstract class CollectionConverter<T> extends SystemConverter<Collection<?>,T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4515250904953131514L;

    /**
     * For inner classes only.
     */
    @SuppressWarnings("unchecked")
    CollectionConverter(final Class<T> targetClass) {
        super((Class) Collection.class, targetClass);
    }

    /**
     * Returns {@link FunctionProperty#SURJECTIVE} by default.
     */
    @Override
    public java.util.Set<FunctionProperty> properties() {
        return EnumSet.of(FunctionProperty.SURJECTIVE);
    }

    /**
     * Converter from {@link Collection} to {@link java.util.List}.
     */
    @Immutable
    static final class List extends CollectionConverter<java.util.List<?>> {
        private static final long serialVersionUID = 5492247760609833586L;

        @SuppressWarnings("unchecked")
        List() {
            super((Class) java.util.List.class);
        }

        @Override
        public java.util.List<?> convert(final Collection<?> source) {
            if (source == null) {
                return null;
            }
            if (source instanceof java.util.List<?>) {
                return (java.util.List<?>) source;
            }
            return new ArrayList<>(source);
        }
    }


    /**
     * Converter from {@link Collection} to {@link java.util.Set}.
     */
    @Immutable
    static final class Set extends CollectionConverter<java.util.Set<?>> {
        private static final long serialVersionUID = -4200659837453206164L;

        @SuppressWarnings("unchecked")
        Set() {
            super((Class) java.util.Set.class);
        }

        @Override
        public java.util.Set<?> convert(final Collection<?> source) {
            if (source == null) {
                return null;
            }
            if (source instanceof java.util.Set<?>) {
                return (java.util.Set<?>) source;
            }
            return new LinkedHashSet<>(source);
        }
    }
}
