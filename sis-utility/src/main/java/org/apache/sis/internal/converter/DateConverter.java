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
import net.jcip.annotations.Immutable;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.math.FunctionProperty;


/**
 * Handles conversions from {@link Date} to various objects.
 *
 * {@section String representation}
 * There is currently no converter between {@link String} and {@link java.util.Date} because the
 * date format is not yet defined (we are considering the ISO format for a future SIS version).
 *
 * {@section Special cases}
 * The converter from dates to timestamps is not injective, because the same date could be mapped
 * to many timestamps since timestamps have an additional nanoseconds field.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
@Immutable
abstract class DateConverter<T> extends SystemConverter<Date,T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7770401534710581917L;

    /**
     * The inverse converter specified at construction time.
     */
    private final SystemConverter<T, Date> inverse;

    /**
     * Creates a converter with an inverse which is the identity converter.
     */
    @SuppressWarnings("unchecked")
    DateConverter(final Class<T> targetClass) {
        super(Date.class, targetClass);
        assert Date.class.isAssignableFrom(targetClass);
        inverse = new IdentityConverter(targetClass, Date.class, this);
    }

    /**
     * Creates a converter with the given inverse converter.
     */
    DateConverter(final Class<T> targetClass, final SystemConverter<T, Date> inverse) {
        super(Date.class, targetClass);
        this.inverse = inverse;
    }

    /**
     * Returns a predefined instance for the given target class, or {@code null} if none.
     * This method does not create any new instance.
     *
     * @param  <T> The target class.
     * @param  targetClass The target class.
     * @return An instance for the given target class, or {@code null} if none.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    static <T> DateConverter<T> getInstance(final Class<T> targetClass) {
        if (targetClass == java.lang.Long    .class) return (DateConverter<T>) Long     .INSTANCE;
        if (targetClass == java.sql.Date     .class) return (DateConverter<T>) SQL      .INSTANCE;
        if (targetClass == java.sql.Timestamp.class) return (DateConverter<T>) Timestamp.INSTANCE;
        return null;
    }

    /**
     * Returns the singleton instance on deserialization, if any.
     */
    @Override
    public final ObjectConverter<Date, T> unique() {
        assert sourceClass == Date.class : sourceClass;
        final DateConverter<T> instance = getInstance(targetClass);
        return (instance != null) ? instance : this;
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
     * Returns the inverse given at construction time.
     */
    @Override
    public final ObjectConverter<T, Date> inverse() {
        return inverse;
    }

    /**
     * Converter from {@code Long} to {@code Date}.
     * This is the inverse of {@link org.apache.sis.internal.converter.DateConverter.Long}.
     */
    private static final class Inverse extends SystemConverter<java.lang.Long, java.util.Date> {
        private static final long serialVersionUID = 3999693055029959455L;

        private Inverse() {
            super(java.lang.Long.class, java.util.Date.class);
        }

        @Override public ObjectConverter<java.util.Date, java.lang.Long> inverse() {
            return DateConverter.Long.INSTANCE;
        }

        @Override public Set<FunctionProperty> properties() {
            return DateConverter.Long.INSTANCE.properties();
        }

        @Override public java.util.Date convert(final java.lang.Long target) {
            return (target != null) ? new java.util.Date(target) : null;
        }
    }

    /**
     * Converter from {@code Date} to {@code Long}.
     * This is the inverse of {@link org.apache.sis.internal.converter.DateConverter.Inverse}.
     */
    private static final class Long extends DateConverter<java.lang.Long> {
        private static final long serialVersionUID = 3163928356094316134L;
        static final Long INSTANCE = new Long();

        private Long() {
            super(java.lang.Long.class, new Inverse());
        }

        @Override public Set<FunctionProperty> properties() {
            return EnumSet.of(FunctionProperty.INJECTIVE, FunctionProperty.SURJECTIVE,
                    FunctionProperty.ORDER_PRESERVING, FunctionProperty.INVERTIBLE);
        }

        @Override public java.lang.Long convert(final Date source) {
            return (source != null) ? source.getTime() : null;
        }
    }

    /**
     * From {@code Date} to SQL {@code Date}.
     * The inverse of this converter is the identity conversion.
     */
    private static final class SQL extends DateConverter<java.sql.Date> {
        private static final long serialVersionUID = -3644605344718636345L;
        static final SQL INSTANCE = new SQL();

        private SQL() {
            super(java.sql.Date.class);
        }

        @Override public java.sql.Date convert(final Date source) {
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
    private static final class Timestamp extends DateConverter<java.sql.Timestamp> {
        private static final long serialVersionUID = 3798633184562706892L;
        static final Timestamp INSTANCE = new Timestamp();

        private Timestamp() {
            super(java.sql.Timestamp.class);
        }

        @Override public java.sql.Timestamp convert(final Date source) {
            if (source == null || source instanceof java.sql.Timestamp) {
                return (java.sql.Timestamp) source;
            }
            return new java.sql.Timestamp(source.getTime());
        }
    }
}
