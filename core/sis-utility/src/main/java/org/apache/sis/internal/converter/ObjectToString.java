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
 * Handles conversions from arbitrary objects to {@link String}. This converter is
 * suitable to any object for which the {@link #toString()} method is sufficient.
 *
 * <p>Some pre-defined unique instances of {@code ObjectToString} are available
 * by the following pattern:</p>
 *
 * {@preformat java
 *     Class<S> sourceClass = ...;
 *     ObjectConverter<S,String> c = StringConverter.getInstance(sourceClass).inverse();
 * }
 *
 * <div class="section">Immutability and thread safety</div>
 * This base class and all inner classes are immutable, and thus inherently thread-safe.
 *
 * @param <S> The source type.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
class ObjectToString<S> extends SystemConverter<S,String> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 502567744195102675L;

    /**
     * The inverse converter specified at construction time.
     */
    private final SystemConverter<String, S> inverse;

    /**
     * Creates a new converter from the given type of objects to {@code String} instances.
     */
    ObjectToString(final Class<S> sourceClass, final SystemConverter<String, S> inverse) {
        super(sourceClass, String.class);
        this.inverse = inverse;
    }

    /**
     * Declares this converter as injective on the assumption that all instances
     * of the source class produce distinct string representations.
     */
    @Override
    public Set<FunctionProperty> properties() {
        return EnumSet.of(FunctionProperty.INJECTIVE, FunctionProperty.INVERTIBLE);
    }

    /**
     * Converts the given number to a string.
     */
    @Override
    public String apply(final S source) {
        return (source != null) ? source.toString() : null;
    }

    /**
     * Returns the inverse given at construction time.
     */
    @Override
    public final ObjectConverter<String, S> inverse() {
        return (inverse != null) ? inverse : super.inverse();
    }

    /**
     * Returns the singleton instance on deserialization, if any.
     */
    @Override
    public final ObjectConverter<S, String> unique() {
        if (inverse != null) {
            return inverse.unique().inverse(); // Will typically delegate to StringConverter.
        }
        return this;
    }


    /**
     * Specialized instance for {@link org.opengis.util.CodeList}.
     * This class invokes {@link org.opengis.util.CodeList#name()} instead than {@code toString()}.
     *
     * @see org.apache.sis.internal.converter.StringConverter.CodeList
     */
    static final class CodeList<S extends org.opengis.util.CodeList<S>> extends ObjectToString<S> {
        private static final long serialVersionUID = 1454105232343463228L;

        /** Creates a new converter from the given type of code list to strings. */
        CodeList(final Class<S> sourceClass, final SystemConverter<String, S> inverse) {
            super(sourceClass, inverse);
        }

        /** Function is bijective, because no duplicated code list name shall exist. */
        @Override public Set<FunctionProperty> properties() {
            return EnumSet.of(FunctionProperty.INJECTIVE, FunctionProperty.SURJECTIVE,
                    FunctionProperty.INVERTIBLE);
        }

        /** Returns the name of the given code list element. */
        @Override public String apply(final S source) {
            return (source != null) ? source.name() : null;
        }
    }


    /**
     * Specialized instance for {@link java.lang.Enum}.
     * This class invokes {@link java.lang.Enum#name()} instead than {@code toString()}.
     *
     * @see org.apache.sis.internal.converter.StringConverter.Enum
     */
    static final class Enum<S extends java.lang.Enum<S>> extends ObjectToString<S> {
        private static final long serialVersionUID = 5391817175838307542L;

        /** Creates a new converter from the given type of enum to strings. */
        Enum(final Class<S> sourceClass, final SystemConverter<String, S> inverse) {
            super(sourceClass, inverse);
        }

        /** Function is bijective, because no duplicated enum name shall exist. */
        @Override public Set<FunctionProperty> properties() {
            return EnumSet.of(FunctionProperty.INJECTIVE, FunctionProperty.SURJECTIVE,
                    FunctionProperty.INVERTIBLE);
        }

        /** Returns the name of the given code list element. */
        @Override public String apply(final S source) {
            return (source != null) ? source.name() : null;
        }
    }
}
