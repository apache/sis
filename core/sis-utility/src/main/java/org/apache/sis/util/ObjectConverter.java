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

// Branch-dependent imports
import org.apache.sis.internal.jdk8.Function;


/**
 * A function which converts instances of <var>source</var> type to instances of <var>target</var> type.
 * The source and target types may be the same, in which case the {@code ObjectConverter} actually converts
 * the values rather than the type.
 *
 * <p>The main method of this interface is {@link #apply(Object)}, which receives an object of type
 * <var>S</var> and returns an object of type <var>T</var>. The set of all <var>S</var> values for which
 * {@code apply(S)} does not throw {@link UnconvertibleObjectException} is called the <cite>domain</cite>
 * of this function, regardless of whether the <var>T</var> result is {@code null} or not.</p>
 *
 * <div class="section">Function properties</div>
 * Some characteristics about the <var>S</var> to <var>T</var> mapping are given by the
 * {@link #properties()} enumeration, together with the {@link #getSourceClass()} and
 * {@link #getTargetClass()} methods. Some possible function properties are:
 *
 * <ul>
 *   <li>{@linkplain FunctionProperty#INJECTIVE Injective} if no pair of <var>S</var> can produce
 *       the same <var>T</var> value (e.g.: conversions from {@link Integer} to {@code String}).</li>
 *   <li>{@linkplain FunctionProperty#SURJECTIVE Surjective} if every values of <var>T</var> can be
 *       created from one or many values of <var>S</var> (e.g.: conversions from {@link String} to
 *       {@link Integer}).</li>
 *   <li>{@linkplain FunctionProperty#isBijective Bijective} if there is a one-to-one
 *       relationship between the <var>S</var> and <var>T</var> values.</li>
 *   <li>{@linkplain FunctionProperty#ORDER_PRESERVING Order preserving} if any sequence of
 *       increasing <var>S</var> values (in the sense of {@link Comparable}) is mapped to a
 *       sequence of increasing <var>T</var> values.</li>
 *   <li>{@linkplain FunctionProperty#ORDER_REVERSING Order reversing} if any sequence of
 *       increasing <var>S</var> values (in the sense of {@link Comparable}) is mapped to
 *       a sequence of decreasing <var>T</var> values.</li>
 * </ul>
 *
 * <div class="note"><b>Example:</b>
 * The function properties regarding order is important when converting {@link org.apache.sis.measure.Range} objects.
 * For example if the converter reverses the value ordering (e.g. reverses the sign of numerical values), then the
 * minimum and maximum values in each {@code Range} instance need to be interchanged. If the ordering is not preserved
 * at all (neither directly or reversed), as for example in the conversion from {@link Number} to {@link String}, then
 * we can not convert ranges at all.</div>
 *
 * Below are some guidelines about the function properties that a converter can declare:
 *
 * <ul>
 *   <li>If {@code apply(S)} returns {@code null} for unconvertible objects, then this {@code ObjectConverter}
 *       can not be declared injective because more than one <var>S</var> value can produce the same
 *       <var>T</var> value (namely {@code null}).</li>
 *   <li>If {@code apply(S)} throws an exception for unconvertible objects, then this {@code ObjectConverter}
 *       can be declared as an injective function if the other values meet the criteria.
 * </ul>
 *
 * @param <S> The type of objects to convert.
 * @param <T> The type of converted objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see ObjectConverters
 */
public interface ObjectConverter<S,T> extends Function<S,T> {
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
     *   <li>{@linkplain FunctionProperty#isBijective Bijective} if there is a one-to-one
     *       relationship between the <var>S</var> and <var>T</var> values.</li>
     *   <li>{@linkplain FunctionProperty#ORDER_PRESERVING Order preserving} if any sequence of
     *       increasing <var>S</var> values (in the sense of {@link Comparable}) is mapped to a
     *       sequence of increasing <var>T</var> values.</li>
     *   <li>{@linkplain FunctionProperty#ORDER_REVERSING Order reversing} if any sequence of
     *       increasing <var>S</var> values (in the sense of {@link Comparable}) is mapped to
     *       a sequence of decreasing <var>T</var> values.</li>
     * </ul>
     *
     * Note that if the {@link #apply(Object)} method returns {@code null} for any non-convertible
     * source value, then this properties set can not contain the {@link FunctionProperty#INJECTIVE}
     * value. See class javadoc for more discussion.
     *
     * @return The manners in which source values are mapped to target values.
     *         May be an empty set, but never null.
     */
    Set<FunctionProperty> properties();

    /**
     * Returns the type of objects to convert.
     *
     * @return The type of objects to convert.
     */
    Class<S> getSourceClass();

    /**
     * Returns the type of converted objects.
     *
     * @return The type of converted objects.
     */
    Class<T> getTargetClass();

    /**
     * Converts the given object from the source type <var>S</var> to the target type <var>T</var>.
     * If the given object can not be converted, then this method may either returns {@code null} or
     * throws an exception, at implementation choice. Note that this choice may affect the set of
     * function {@linkplain #properties() properties} - see the class Javadoc for more discussion.
     *
     * @param  object The object to convert, or {@code null}.
     * @return The converted object, or {@code null}.
     * @throws UnconvertibleObjectException If the given object is not an element of the function domain.
     */
    @Override
    T apply(S object) throws UnconvertibleObjectException;

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
