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
import java.util.function.Function;


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
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.3
 *
 * @param <S>  the type of objects to convert.
 * @param <T>  the type of converted objects.
 *
 * @see ObjectConverters
 *
 * @since 0.3
 * @module
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
     * Note that if the {@link #apply(Object)} method returns {@code null} for unconvertible source values,
     * then this properties set can not contain {@link FunctionProperty#INJECTIVE} because more than one
     * source value could be converted to the same {@code null} target value.
     *
     * @return the manners in which source values are mapped to target values.
     *         May be an empty set, but never null.
     */
    Set<FunctionProperty> properties();

    /**
     * Returns the type of objects to convert.
     *
     * @return the type of objects to convert.
     */
    Class<S> getSourceClass();

    /**
     * Returns the type of converted objects.
     *
     * @return the type of converted objects.
     */
    Class<T> getTargetClass();

    /**
     * Converts the given object from the source type <var>S</var> to the target type <var>T</var>.
     * If the given object can not be converted, then this method may either returns {@code null} or throws an exception,
     * at implementation choice (except for {@linkplain FunctionProperty#INJECTIVE injective} functions, which must throw
     * an exception - see the class Javadoc for more discussion about function {@linkplain #properties() properties}).
     *
     * <div class="note"><b>Example:</b>
     * in Apache SIS implementation, converters from {@link String} to {@link Number} distinguish two kinds of
     * unconvertible objects:
     *
     * <ul>
     *   <li>Null or empty source string result in a {@code null} value to be returned.</li>
     *   <li>All other kind of unparsable strings results in an exception to be thrown.</li>
     * </ul>
     *
     * In other words, the {@code ""} value is unconvertible but nevertheless considered as part of the converter
     * domain, and is mapped to <cite>"no number"</cite>. All other unparsable strings are considered outside the
     * converter domain.</div>
     *
     * @param  object  the object to convert, or {@code null}.
     * @return the converted object, or {@code null}.
     * @throws UnconvertibleObjectException if the given object is not an element of the function domain.
     */
    @Override
    T apply(S object) throws UnconvertibleObjectException;

    /**
     * Returns a converter capable to convert instances of <var>T</var> back to instances of <var>S</var>.
     * Before to invoke this method, callers can verify if this converter is invertible as below:
     *
     * {@preformat java
     *     if (converter.properties().contains(FunctionProperty.INVERTIBLE)) {
     *         // Call to converter.inverse() is allowed here.
     *     }
     * }
     *
     * @return a converter for converting instances of <var>T</var> back to instances of <var>S</var>.
     * @throws UnsupportedOperationException if this converter is not invertible.
     *
     * @see FunctionProperty#INVERTIBLE
     */
    ObjectConverter<T,S> inverse() throws UnsupportedOperationException;
}
