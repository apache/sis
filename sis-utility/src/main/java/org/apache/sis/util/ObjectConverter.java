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
package org.apache.sis.util;

import java.util.Set;
import org.apache.sis.math.FunctionProperty;


/**
 * A function which converts instances of <var>source</var> type to instances of <var>target</var> type.
 * The source and target types may be the same, in which case the {@code ObjectConverter} actually converts
 * the values.
 *
 * <p>The characteristics of the <var>S</var> to <var>T</var> mapping are given by
 * the {@link #properties()} enumeration set.</p>
 *
 * @param <S> The type of objects to convert.
 * @param <T> The type of converted objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public interface ObjectConverter<S,T> {
    /**
     * Returns the manner in which source values (<var>S</var>) are mapped to target values
     * (<var>T</var>). Some possible function properties are:
     *
     * <ul>
     *   <li>{@linkplain FunctionProperty#INJECTIVE Injective} if no pair of <var>S</var> can produce
     *       the same <var>T</var> value (e.g.: conversions from {@link Integer} to {@code String}).</li>
     *   <li>{@linkplain FunctionProperty#SURJECTIVE Surjective} if every values of <var>T</var> can be
     *       created from one or many values of <var>S</var> (e.g.: conversions from {@link String} to
     *       {@link Integer}).</li>
     *   <li>Bijective if there is a one-to-one relationship between the <var>S</var> and <var>T</var>
     *       values.</li>
     *   <li>{@linkplain FunctionProperty#ORDER_PRESERVING Order preserving} if any sequence of
     *       increasing <var>S</var> values (in the sense of {@link Comparable}) is mapped to a
     *       sequence of increasing <var>T</var> values.</li>
     *   <li>{@linkplain FunctionProperty#ORDER_REVERSING Order reversing} if any sequence of
     *       increasing <var>S</var> values (in the sense of {@link Comparable}) is mapped to
     *       a sequence of decreasing <var>T</var> values.</li>
     * </ul>
     *
     * @return The manners in which source values are mapped to target values.
     *         May be an empty set, but never null.
     */
    Set<FunctionProperty> properties();

    /**
     * Returns the type of source values.
     *
     * @return The type of source values.
     */
    Class<? super S> getSourceClass();

    /**
     * Returns the base type of converted values.
     *
     * @return The base type of target values.
     */
    Class<? extends T> getTargetClass();

    /**
     * Converts the given object from the source type <var>S</var> to the target type <var>T</var>.
     * If the given object can not be converted, then this method may either returns {@code null} or
     * throws an exception, depending on the implementation.
     *
     * @param  object The object to convert, or {@code null}.
     * @return The converted object, or {@code null}.
     * @throws UnconvertibleObjectException If the given object can not be converted.
     */
    T convert(S object) throws UnconvertibleObjectException;

    /**
     * Returns a converter capable to convert instances of <var>T</var> back to instances of
     * <var>S</var>. Before to invoke this method, callers can verify if this converter is
     * invertible as below:
     *
     * {@preformat java
     *     if (converter.properties().contains(FunctionProperty.INVERTIBLE)) {
     *         // Call to converter.inverse() is allowed here.
     *     }
     * }
     *
     * @return A converter for converting instances of <var>T</var> back to instances of <var>S</var>.
     * @throws UnsupportedOperationException If this converter is not invertible.
     *
     * @see FunctionProperty#INVERTIBLE
     */
    ObjectConverter<T,S> inverse() throws UnsupportedOperationException;
}
