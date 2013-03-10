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
import java.util.LinkedHashSet;
import java.io.Serializable;
import java.io.ObjectStreamException;
import net.jcip.annotations.Immutable;


/**
 * Handles conversions from {@link Collection} to various objects.
 * The source class is fixed to {@code Collection}. The target class is determined
 * by the inner class which extends this {@code CollectionConverter} class.
 *
 * <p>All subclasses will have a unique instance. For this reason, it is not necessary to
 * override the {@code hashCode()} and {@code equals(Object)} methods, since identity
 * comparisons will work just well.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.02)
 * @version 0.3
 * @module
 */
@Immutable
abstract class CollectionConverter<T> extends SurjectiveConverter<Collection<?>,T> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4515250904953131514L;

    /**
     * Returns the source class, which is always {@link Collection}.
     */
    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    public final Class<Collection<?>> getSourceClass() {
        return (Class) Collection.class;
    }


    /**
     * Converter from {@link Collection} to {@link java.util.List}.
     */
    @Immutable
    static final class List extends CollectionConverter<java.util.List<?>> {
        /** Cross-version compatibility. */ static final long serialVersionUID = 5492247760609833586L;
        /** The unique, shared instance. */ static final List INSTANCE = new List();
        /** For {@link #INSTANCE} only.  */ private List() {}

        @Override
        @SuppressWarnings({"unchecked","rawtypes"})
        public Class<java.util.List<?>> getTargetClass() {
            return (Class) java.util.List.class;
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

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }


    /**
     * Converter from {@link Collection} to {@link java.util.Set}.
     */
    @Immutable
    static final class Set extends CollectionConverter<java.util.Set<?>> {
        /** Cross-version compatibility. */ static final long serialVersionUID = -4200659837453206164L;
        /** The unique, shared instance. */ static final Set INSTANCE = new Set();
        /** For {@link #INSTANCE} only.  */ private Set() {}

        @Override
        @SuppressWarnings({"unchecked","rawtypes"})
        public Class<java.util.Set<?>> getTargetClass() {
            return (Class) java.util.Set.class;
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

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }
}
