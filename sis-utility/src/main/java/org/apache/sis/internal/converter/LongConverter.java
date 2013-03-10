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

import java.io.Serializable;
import java.io.ObjectStreamException;
import net.jcip.annotations.Immutable;
import org.apache.sis.util.ObjectConverter;


/**
 * Handles conversions from {@link java.lang.Long} to various objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
@Immutable
abstract class LongConverter<T> extends InjectiveConverter<Long,T> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -7313843433890738313L;

    /**
     * For inner classes only.
     */
    LongConverter() {
    }

    /**
     * Returns the source class, which is always {@link Long}.
     */
    @Override
    public final Class<Long> getSourceClass() {
        return Long.class;
    }

    /**
     * Converter from long integers to dates.
     */
    @Immutable
    static final class Date extends LongConverter<java.util.Date> {
        /** Cross-version compatibility. */ static final long serialVersionUID = 3999693055029959455L;
        /** The unique, shared instance. */ static final Date INSTANCE = new Date();
        /** For {@link #INSTANCE} only.  */ private Date() {}

        @Override public Class<java.util.Date> getTargetClass() {
            return java.util.Date.class;
        }

        @Override public java.util.Date convert(final Long target) {
            return (target != null) ? new java.util.Date(target) : null;
        }

        @Override public ObjectConverter<java.util.Date, Long> inverse() {
            return DateConverter.Long.INSTANCE;
        }

        /** Returns the singleton instance on deserialization. */
        Object readResolve() throws ObjectStreamException {
            return INSTANCE;
        }
    }
}
