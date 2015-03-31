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

import java.util.Date;
import java.util.Set;
import java.util.EnumSet;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.math.FunctionProperty;


/**
 * Handles conversions from {@link Date} to various objects.
 *
 * <div class="section">String representation</div>
 * There is currently no converter between {@link String} and {@link java.util.Date} because the
 * date format is not yet defined (we are considering the ISO format for a future SIS version).
 *
 * <div class="section">Special cases</div>
 * The converter from dates to timestamps is not injective, because the same date could be mapped
 * to many timestamps since timestamps have an additional nanoseconds field.
 *
 * <div class="section">Immutability and thread safety</div>
 * This base class and all inner classes are immutable, and thus inherently thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
abstract class DateConverter<T> extends SystemConverter<Date,T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 945435736679371963L;

    /**
     * The inverse converter. Must be initialized by subclass constructors.
     */
    SystemConverter<T, Date> inverse;

    /**
     * Creates a converter for the given target type.
     * Subclasses must initialize {@link #inverse}.
     */
    DateConverter(final Class<T> targetClass) {
        super(Date.class, targetClass);
    }

    /**
     * Returns the function properties.
     */
    @Override
    public Set<FunctionProperty> properties() {
        return EnumSet.of(FunctionProperty.SURJECTIVE, FunctionProperty.ORDER_PRESERVING,
                FunctionProperty.INVERTIBLE);
    }

    /**
     * Returns the inverse converter.
     */
    @Override
    public final ObjectConverter<T, Date> inverse() {
        return inverse;
    }

    /**
     * Converter from {@code Long} to {@code Date}.
     * This converter is <strong>not</strong> registered by {@link ConverterRegistry#initialize()}.
     * Instead, {@link SystemRegistry} will fetch it when first needed by looking at the inverse
     * of {@link DateConverter.Long}. The same is true for all inverse converters defined in the
     * {@code DateConverter} enclosing class.
     */
    private static final class Inverse extends SystemConverter<java.lang.Long, java.util.Date> {
        private static final long serialVersionUID = 5022624034871426299L;

        private final SystemConverter<java.util.Date, java.lang.Long> inverse;

        Inverse(final SystemConverter<java.util.Date, java.lang.Long> inverse) {
            super(java.lang.Long.class, java.util.Date.class);
            this.inverse = inverse;
        }

        @Override public ObjectConverter<java.util.Date, java.lang.Long> inverse() {
            return inverse;
        }

        @Override public ObjectConverter<java.lang.Long, Date> unique() {
            return inverse.inverse();
        }

        @Override public Set<FunctionProperty> properties() {
            return inverse.properties();
        }

        @Override public java.util.Date apply(final java.lang.Long target) {
            return (target != null) ? new java.util.Date(target) : null;
        }
    }

    /**
     * Converter from {@code Date} to {@code Long}.
     */
    public static final class Long extends DateConverter<java.lang.Long> {
        private static final long serialVersionUID = 5145114630594761657L;

        public Long() { // Instantiated by ServiceLoader.
            super(java.lang.Long.class);
            inverse = new Inverse(this);
        }

        @Override public Set<FunctionProperty> properties() {
            return bijective();
        }

        @Override public java.lang.Long apply(final Date source) {
            return (source != null) ? source.getTime() : null;
        }
    }

    /**
     * From {@code Date} to SQL {@code Date}.
     * The inverse of this converter is the identity conversion.
     */
    public static final class SQL extends DateConverter<java.sql.Date> {
        private static final long serialVersionUID = -7444502675467008640L;

        public SQL() { // Instantiated by ServiceLoader.
            super(java.sql.Date.class);
            inverse = new IdentityConverter<Date, java.sql.Date>(targetClass, Date.class, this); // <T,S> in reverse order.
        }

        @Override public java.sql.Date apply(final Date source) {
            if (source == null || source instanceof java.sql.Date) {
                return (java.sql.Date) source;
            }
            return new java.sql.Date(source.getTime());
        }
    }

    /**
     * From {@code Date} to SQL {@code Timestamp}.
     * The inverse of this converter is the identity conversion.
     */
    public static final class Timestamp extends DateConverter<java.sql.Timestamp> {
        private static final long serialVersionUID = 7629460512978844462L;

        public Timestamp() { // Instantiated by ServiceLoader.
            super(java.sql.Timestamp.class);
            inverse = new IdentityConverter<Date, java.sql.Timestamp>(targetClass, Date.class, this); // <T,S> in reverse order.
        }

        @Override public java.sql.Timestamp apply(final Date source) {
            if (source == null || source instanceof java.sql.Timestamp) {
                return (java.sql.Timestamp) source;
            }
            return new java.sql.Timestamp(source.getTime());
        }
    }
}
