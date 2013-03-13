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

import java.io.ObjectStreamException;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.resources.Errors;


/**
 * Base class of all converters defined in the {@code org.apache.sis.internal} package.
 * Those converters are returned by system-wide {@link ConverterRegitry}, and cached for
 * reuse.
 *
 * @param <S> The base type of source objects.
 * @param <T> The base type of converted objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
abstract class SystemConverter<S,T> extends ClassPair<S,T> implements ObjectConverter<S,T> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 885663610056067478L;

    /**
     * Creates a new converter for the given source and target classes.
     *
     * @param sourceClass The {@linkplain ObjectConverter#getSourceClass() source class}.
     * @param targetClass The {@linkplain ObjectConverter#getTargetClass() target class}.
     */
    SystemConverter(final Class<S> sourceClass, final Class<T> targetClass) {
        super(sourceClass, targetClass);
    }

    /**
     * Returns the source class given at construction time.
     */
    @Override
    public final Class<S> getSourceClass() {
        return sourceClass;
    }

    /**
     * Returns the target class given at construction time.
     */
    @Override
    public final Class<T> getTargetClass() {
        return targetClass;
    }

    /**
     * Unsupported by default. To be overridden by subclasses that support this operation.
     */
    @Override
    public ObjectConverter<T, S> inverse() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.NonInvertibleConversion));
    }

    /**
     * Returns an unique instance of this converter. If a converter already exists for the same
     * source an target classes, then this converter is returned. Otherwise this converter is
     * cached and returned.
     */
    final ObjectConverter<S,T> unique() {
        return ConverterRegistry.SYSTEM.unique(this, true);
    }

    /**
     * Returns the singleton instance on deserialization, if any. If no instance already exist
     * in the virtual machine, we do not cache the instance (for now) for security reasons.
     */
    protected final Object readResolve() throws ObjectStreamException {
        return ConverterRegistry.SYSTEM.unique(this, false);
    }

    /**
     * Formats an error message for a value that can not be converted.
     *
     * @param  value The value that can not be converted.
     * @return The error message.
     */
    final String formatErrorMessage(final S value) {
        return Errors.format(Errors.Keys.CanNotConvertValue_2, value, getTargetClass());
    }
}
