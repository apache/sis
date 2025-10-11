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
package org.apache.sis.converter;

import java.util.Set;
import java.util.EnumSet;
import java.time.Instant;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.math.FunctionProperty;


/**
 * Handles conversions from {@link Instant} to various objects.
 *
 * <h2>Immutability and thread safety</h2>
 * This base class and all inner classes are immutable, and thus inherently thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param <T>  the base type of converted objects.
 */
abstract class InstantConverter<T> extends SystemConverter<Instant,T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7219681557586687605L;

    /**
     * Creates a converter for the given target type.
     * Subclasses must initialize {@link #inverse}.
     */
    InstantConverter(final Class<T> targetClass) {
        super(Instant.class, targetClass);
    }

    /**
     * Returns the function properties. The function is <em>surjective</em> because any {@code Date} instances
     * can be created from one or many {@code Instant} instances. The same date may be created from many instants
     * because the nanosecond field is dropped.
     */
    @Override
    public Set<FunctionProperty> properties() {
        return EnumSet.of(FunctionProperty.SURJECTIVE, FunctionProperty.ORDER_PRESERVING, FunctionProperty.INVERTIBLE);
    }

    /**
     * From {@code Instant} to {@code Date}.
     */
    public static final class Date extends InstantConverter<java.util.Date> {
        private static final long serialVersionUID = -9192665378798185400L;

        static final Date INSTANCE = new Date();        // Invoked by ServiceLoader when using module-path.
        public static Date provider() {
            return INSTANCE;
        }

        public Date() {                                 // Instantiated by ServiceLoader when using class-path.
            super(java.util.Date.class);
        }

        @Override public ObjectConverter<java.util.Date, Instant> inverse() {
            return DateConverter.Instant.INSTANCE;
        }

        @Override public java.util.Date apply(final Instant source) {
            // TODO: after merge of `util` with `metadata`: return TemporalDate.toDate(source);
            return (source != null) ? java.util.Date.from(source) : null;
        }
    }
}
