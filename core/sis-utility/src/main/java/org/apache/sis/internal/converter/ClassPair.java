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
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.Debug;


/**
 * Holds explicit {@link #sourceClass} and {@link #targetClass} values. Used as key in a hash
 * map of converters. Also used as the base class for converters defined in this package.
 *
 * <p>The only direct subtype allowed is {@link SystemConverter}.
 * <strong>No other direct subtype shall exist</strong>.
 * See {@link #equals(Object)} for an explanation.</p>
 *
 * <div class="section">Immutability and thread safety</div>
 * This base class is immutable and thus inherently thread-safe. {@code ClassPair} immutability is necessary
 * for {@link ConverterRegistry}. Subclasses should also be immutable, but this requirement is not as strong
 * as for {@code ClassPair} (because subclasses are not used as keys in hash map).
 *
 * @param <S> The base type of source objects.
 * @param <T> The base type of converted objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
class ClassPair<S,T> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5214470401299470687L;

    /**
     * The source class.
     */
    final Class<S> sourceClass;

    /**
     * The target class.
     */
    final Class<T> targetClass;

    /**
     * Creates an entry for the given source and target classes.
     *
     * @param sourceClass The {@linkplain ObjectConverter#getSourceClass() source class}.
     * @param targetClass The {@linkplain ObjectConverter#getTargetClass() target class}.
     */
    ClassPair(final Class<S> sourceClass, final Class<T> targetClass) {
        this.sourceClass = sourceClass;
        this.targetClass = targetClass;
    }

    /**
     * Returns a key for the parent source, or {@code null} if none.
     * This method applies the following rules:
     *
     * <ul>
     *   <li>If {@link #sourceClass} is a class and have a parent class, then returns a new
     *       {@code ClassPair} having that parent class as the source.</li>
     *   <li>Otherwise if {@link #sourceClass} is an interface extending at least one interface,
     *       then returns a new {@code ClassPair} having the first parent interface as the source.
     *       we select the first interface on the assumption that it is the mean one.</li>
     *   <li>Otherwise (i.e. if there is no parent class or interface), returns {@code null}.</li>
     * </ul>
     *
     * The target class is left unchanged.
     *
     * @return A key for the parent source, or {@code null}.
     */
    @SuppressWarnings({"unchecked","rawtypes"})
    final ClassPair<? super S, T> parentSource() {
        final Class<? super S> source;
        if (sourceClass.isInterface()) {
            final Class<? super S>[] interfaces = (Class[]) sourceClass.getInterfaces();
            if (interfaces.length == 0) {
                return null;
            }
            source = interfaces[0]; // Take only the first interface declaration; ignore others.
        } else {
            source = sourceClass.getSuperclass();
            if (source == null) {
                return null;
            }
        }
        return new ClassPair(source, targetClass); // Checked in JDK7 branch.
    }

    /**
     * Casts the given converter to the source and target classes of this {@code ClassPair}.
     * This method is not public because the checks are performed using assertions only.
     * If this method was to goes public, the assertions would need to be replaced by
     * unconditional checks.
     *
     * <p>This method is used by {@link ConverterRegistry} after fetching a value from a hash
     * map using this {@code ClassPair} as a key. In this context, the cast should never fail
     * (assuming that the converters do not change their source and target classes).</p>
     */
    @SuppressWarnings("unchecked")
    final ObjectConverter<? super S, ? extends T> cast(final ObjectConverter<?,?> converter) {
        if (converter != null) {
            assert converter.getSourceClass().isAssignableFrom(sourceClass) : sourceClass;
            assert targetClass.isAssignableFrom(converter.getTargetClass()) : targetClass;
        }
        return (ObjectConverter<? super S, ? extends T>) converter;
    }

    /**
     * Compares the given object with this {@code ClassPair} for equality. Two {@code ClassPair}
     * instances are considered equal if they have the same source and target classes, ignoring
     * all other properties eventually defined in subclasses.
     *
     * <p>This method is designed for use by {@link ConverterRegistry} as {@link java.util.HashMap}
     * keys. Its primary purpose is <strong>not</strong> to determine if two objects are converters
     * doing the same conversions. However the {@link SystemConverter} subclass overrides this
     * method with an additional safety check.</p>
     *
     * @param  other The object to compare with this {@code ClassPair}.
     * @return {@code true} if the given object is a {@code ClassPair}
     *         having the same source and target classes.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof ClassPair<?,?>) {
            final ClassPair<?,?> that = (ClassPair<?,?>) other;
            return sourceClass == that.sourceClass &&
                   targetClass == that.targetClass;
        }
        return false;
    }

    /**
     * Returns a hash code value for this {@code ClassPair}.
     * See {@link #equals(Object)} javadoc for information on the scope of this method.
     */
    @Override
    public final int hashCode() {
        return sourceClass.hashCode() + 31*targetClass.hashCode();
    }

    /**
     * Returns a string representation for this entry.
     * Used for formatting error messages.
     */
    @Debug
    @Override
    public String toString() {
        return targetClass.getSimpleName() + " ← " + sourceClass.getSimpleName();
    }
}
