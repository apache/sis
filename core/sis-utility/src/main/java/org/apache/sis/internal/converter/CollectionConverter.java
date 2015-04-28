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
import org.apache.sis.math.FunctionProperty;


/**
 * Handles conversions from {@link Collection} to various objects.
 * The source class is fixed to {@code Collection}. The target class is determined
 * by the inner class which extends this {@code CollectionConverter} class.
 *
 * <div class="section">Immutability and thread safety</div>
 * This base class is immutable, and thus inherently thread-safe. Subclasses should be immutable
 * and thread-safe too if they are intended to be cached in {@link ConverterRegistry}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
abstract class CollectionConverter<T> extends SystemConverter<Collection<?>,T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -9214936334129327955L;

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
    public static final class List extends CollectionConverter<java.util.List<?>> {
        private static final long serialVersionUID = -8680976097058177832L;

        @SuppressWarnings("unchecked")
        public List() { // Instantiated by ServiceLoader.
            super((Class) java.util.List.class);
        }

        @Override
        @SuppressWarnings({"unchecked","rawtypes"})
        public java.util.List<?> apply(final Collection<?> source) {
            if (source == null) {
                return null;
            }
            if (source instanceof java.util.List<?>) {
                return (java.util.List<?>) source;
            }
            return new ArrayList(source); // Checked in JDK7 branch.
        }
    }


    /**
     * Converter from {@link Collection} to {@link java.util.Set}.
     */
    public static final class Set extends CollectionConverter<java.util.Set<?>> {
        private static final long serialVersionUID = -1065360595793529078L;

        @SuppressWarnings("unchecked")
        public Set() { // Instantiated by ServiceLoader.
            super((Class) java.util.Set.class);
        }

        @Override
        @SuppressWarnings({"unchecked","rawtypes"})
        public java.util.Set<?> apply(final Collection<?> source) {
            if (source == null) {
                return null;
            }
            if (source instanceof java.util.Set<?>) {
                return (java.util.Set<?>) source;
            }
            return new LinkedHashSet(source); // Checked in JDK7 branch.
        }
    }
}
