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
import org.apache.sis.util.Debug;
import org.apache.sis.util.Classes;
import org.apache.sis.util.resources.Errors;


/**
 * Base class for (usually non-invertible) surjective {@link ObjectConverter}s.
 * Surjective converters are converters for which many different source values can produce
 * the same target value. In many cases, the target value having many possible sources is
 * the {@code null} value. This is the case in particular when the converter is used as a
 * filter.
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
 */
public abstract class SurjectiveConverter<S,T> implements ObjectConverter<S,T> {
    /**
     * Creates a new converter.
     */
    protected SurjectiveConverter() {
    }

    /**
     * Returns {@link FunctionProperty#SURJECTIVE} by default.
     * Subclasses may add more properties (order preserving, <i>etc.</i>).
     */
    @Override
    public Set<FunctionProperty> properties() {
        return EnumSet.of(FunctionProperty.SURJECTIVE);
    }

    /**
     * Unsupported operation, since surjective converters are non-invertible
     * (unless the converter is bijective, which is decided by subclasses).
     */
    @Override
    public ObjectConverter<T,S> inverse() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(Errors.format(
                Errors.Keys.UnsupportedOperation_1, "inverse"));
    }

    /**
     * Returns a string representation of this converter for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        return Classes.getShortClassName(this) + '[' +
                getTargetClass().getSimpleName() + " ← " +
                getSourceClass().getSimpleName() + ']';
    }
}
