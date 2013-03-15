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
import org.apache.sis.util.Classes;
import org.apache.sis.util.resources.Errors;


/**
 * Base class for (usually invertible) injective {@link ObjectConverter}s.
 * Injective converters are converters for which each source value can produce
 * only one target value.
 *
 * <p>This base class is stateless. Consequently sub-classes that choose to implement
 * {@link java.io.Serializable} do not need to care about this base class.</p>
 *
 * @param <S> The type of objects to convert.
 * @param <T> The type of converted objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see SurjectiveConverter
 */
public abstract class InjectiveConverter<S,T> implements ObjectConverter<S,T> {
    /**
     * Creates a new converter.
     */
    protected InjectiveConverter() {
    }

    /**
     * Returns {@link FunctionProperty#INJECTIVE} and {@link FunctionProperty#INVERTIBLE} by default.
     * Subclasses may add more properties (order preserving, <i>etc.</i>).
     */
    @Override
    public Set<FunctionProperty> properties() {
        return EnumSet.of(FunctionProperty.INJECTIVE, FunctionProperty.INVERTIBLE);
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

    /**
     * Returns a string representation of this converter for debugging purpose.
     */
    @Override
    public String toString() {
        return Classes.getShortClassName(this) + '[' +
                getTargetClass().getSimpleName() + " ← " +
                getSourceClass().getSimpleName() + ']';
    }
}
