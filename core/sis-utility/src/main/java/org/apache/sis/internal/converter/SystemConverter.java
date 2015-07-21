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
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.resources.Errors;


/**
 * Base class of all converters defined in the {@code org.apache.sis.internal} package.
 * Those converters are returned by system-wide {@link ConverterRegistry}, and cached for reuse.
 *
 * <div class="section">Immutability and thread safety</div>
 * This base class is immutable, and thus inherently thread-safe. Subclasses should be immutable
 * and thread-safe too if they are intended to be cached in {@link ConverterRegistry}.
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
    private static final long serialVersionUID = -5003169442214901702L;

    /**
     * Creates a new converter for the given source and target classes.
     *
     * @param sourceClass The {@linkplain #getSourceClass() source class}.
     * @param targetClass The {@linkplain #getTargetClass() target class}.
     */
    SystemConverter(final Class<S> sourceClass, final Class<T> targetClass) {
        super(sourceClass, targetClass);
    }

    /**
     * Returns the source class given at construction time.
     *
     * @return The type of objects to convert.
     */
    @Override
    public final Class<S> getSourceClass() {
        return sourceClass;
    }

    /**
     * Returns the target class given at construction time.
     *
     * @return The type of converted objects.
     */
    @Override
    public final Class<T> getTargetClass() {
        return targetClass;
    }

    /**
     * Convenience method for {@link #properties()} implementation of bijective converters
     * between comparable objects. The converter is presumed invertible and to preserve order.
     */
    static Set<FunctionProperty> bijective() {
        return EnumSet.of(FunctionProperty.INJECTIVE, FunctionProperty.SURJECTIVE,
                    FunctionProperty.ORDER_PRESERVING, FunctionProperty.INVERTIBLE);
    }

    /**
     * Default to non-invertible conversion. Must be overridden by subclasses that support inversions.
     *
     * @return A converter for converting instances of <var>T</var> back to instances of <var>S</var>.
     */
    @Override
    public ObjectConverter<T,S> inverse() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(Errors.format(Errors.Keys.NonInvertibleConversion));
    }

    /**
     * Performs the comparisons documented in {@link ClassPair#equals(Object)} with an additional
     * check: if <strong>both</strong> objects to compare are {@code SystemConverter}, then also
     * requires the two objects to be of the same class. We do that in order to differentiate the
     * "ordinary" converters from the {@link FallbackConverter}.
     *
     * <div class="section">Implementation note</div>
     * This is admittedly a little bit convolved. A cleaner approach would have been to not allow
     * the {@code ConverterRegister} hash map to contain anything else than {@code ClassPair} keys,
     * but the current strategy of using the same instance for keys and values reduces a little bit
     * the number of objects to create in the JVM. An other cleaner approach would have been to
     * compare {@code ObjectConverter}s in a separated method, but users invoking {@code equals}
     * on our system converters could be surprised.
     *
     * <p>Our {@code equals(Object)} definition have the following implications regarding
     * the way to use the {@link ConverterRegistry#converters} map:</p>
     * <ul>
     *   <li>When searching for a converter of the same class than the key (as in the
     *       {@link ConverterRegistry#findEquals(SystemConverter)} method), then there
     *       is no restriction on the key that can be given to the {@code Map.get(K)}
     *       method. The {@code Map} is "normal".</li>
     *   <li>When searching for a converter for a pair of source and target classes
     *       (as in {@link ConverterRegistry#find(Class, Class)}), the key shall be
     *       an instance of {@code ClassPair} instance (not a subclass).</li>
     * </ul>
     *
     * @param  other The object to compare with this {@code SystemConverter}.
     * @return {@code true} if the given object is a {@code ClassPair} or a converter of the
     *         same class than {@code this}, and both have the same source and target classes.
     */
    @Override
    public final boolean equals(final Object other) {
        if (super.equals(other)) {
            final Class<?> type = other.getClass();
            return type == ClassPair.class || type == getClass();
        }
        return false;
    }

    /**
     * Returns an unique instance of this converter if one exists. If a converter already
     * exists for the same source an target classes, then this converter is returned.
     * Otherwise this converter is returned <strong>without</strong> being cached.
     *
     * @return The unique instance, or {@code this} if no unique instance can be found.
     */
    public ObjectConverter<S,T> unique() {
        final ObjectConverter<S,T> existing = SystemRegistry.INSTANCE.findEquals(this);
        return (existing != null) ? existing : this;
    }

    /**
     * Returns the singleton instance on deserialization, if any. If no instance already exist
     * in the virtual machine, we do not cache the instance (for now) for security reasons.
     */
    protected final Object readResolve() {
        return unique();
    }

    /**
     * Formats an error message for a value that can not be converted.
     *
     * @param  value The value that can not be converted.
     * @return The error message.
     */
    final String formatErrorMessage(final S value) {
        return Errors.format(Errors.Keys.CanNotConvertValue_2, value, targetClass);
    }
}
