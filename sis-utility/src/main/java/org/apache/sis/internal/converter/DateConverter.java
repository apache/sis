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
import java.io.Serializable;
import java.io.ObjectStreamException;
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
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
@Immutable
abstract class DateConverter<T> extends SurjectiveConverter<Date,T> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7770401534710581917L;

    /**
     * For inner classes only.
     */
    DateConverter() {
    }

    /**
     * Returns the function properties.
     */
    @Override
    public Set<FunctionProperty> properties() {
        return EnumSet.of(FunctionProperty.SURJECTIVE, FunctionProperty.ORDER_PRESERVING);
    }

    /**
     * Returns the source class, which is always {@link Date}.
     */
    @Override
    public final Class<Date> getSourceClass() {
        return Date.class;
    }

    /**
     * Converter from dates to long integers.
     */
    @Immutable
    static final class Long extends DateConverter<java.lang.Long> {
        /** Cross-version compatibility. */ static final long serialVersionUID = 3163928356094316134L;
        /** The unique, shared instance. */ static final Long INSTANCE = new Long();
        /** For {@link #INSTANCE} only.  */ private Long() {}

        /** Returns the function properties, which is bijective. */
        @Override public Set<FunctionProperty> properties() {
            return EnumSet.of(FunctionProperty.INJECTIVE, FunctionProperty.SURJECTIVE,
                    FunctionProperty.ORDER_PRESERVING, FunctionProperty.INVERTIBLE);
        }

        @Override public Class<java.lang.Long> getTargetClass() {
            return java.lang.Long.class;
        }

        @Override public java.lang.Long convert(final Date source) {
            return (source != null) ? source.getTime() : null;
        }

        @Override public ObjectConverter<java.lang.Long, Date> inverse() {
            return LongConverter.Date.INSTANCE;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from dates to SQL dates.
     */
    @Immutable
    static final class SQL extends DateConverter<java.sql.Date> {
        /** Cross-version compatibility. */ static final long serialVersionUID = -3644605344718636345L;
        /** The unique, shared instance. */ static final SQL INSTANCE = new SQL();
        /** For {@link #INSTANCE} only.  */ private SQL() {}

        @Override public Class<java.sql.Date> getTargetClass() {
            return java.sql.Date.class;
        }

        @Override public java.sql.Date convert(final Date source) {
            return (source != null) ? new java.sql.Date(source.getTime()) : null;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }

    /**
     * Converter from dates to timestamps. This converter is not injective, because
     * the same date could be mapped to many timestamps since timestamps have an
     * additional nanoseconds field.
     */
    @Immutable
    static final class Timestamp extends DateConverter<java.sql.Timestamp> {
        /** Cross-version compatibility. */ static final long serialVersionUID = 3798633184562706892L;
        /** The unique, shared instance. */ static final Timestamp INSTANCE = new Timestamp();
        /** For {@link #INSTANCE} only.  */ private Timestamp() {}

        @Override public Class<java.sql.Timestamp> getTargetClass() {
            return java.sql.Timestamp.class;
        }

        @Override public java.sql.Timestamp convert(final Date source) {
            return (source != null) ? new java.sql.Timestamp(source.getTime()) : null;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }
}
