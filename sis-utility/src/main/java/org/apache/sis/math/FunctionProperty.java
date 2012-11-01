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
package org.apache.sis.math;

import java.util.Set;
import java.util.EnumSet;


/**
 * The manners in which the inputs of a function are mapped to the outputs.
 * The function doesn't need to be mathematical. For example this enumeration
 * can be used with {@link org.apache.sis.util.ObjectConverter}.
 *
 * <p>Any given function can have zero, one or more properties from this enumeration.
 * Some properties not included in this enumeration can be tested by a combination of
 * other properties. For example in order to test if a function is <cite>bijective</cite>
 * (i.e. if there is a one-to-one relationship between all input and output values),
 * one can use:</p>
 *
 * {@preformat java
 *     private static final Set<FunctionProperty> BIJECTIVE = EnumSet.of(INJECTIVE, SURJECTIVE);
 *
 *     public void doSomeStuff(Set<FunctionProperty> properties) {
 *         if (properties.containsAll(BIJECTIVE)) {
 *             // At this point, we have determined that a function
 *             // having the given properties is bijective.
 *         }
 *     }
 * }
 *
 * <p>The Javadoc in this class uses the following terms:</p>
 * <ul>
 *   <li><var>S</var> (as in <cite>source</cite>) is the set of all possible input values (the <cite>domain</cite>).
 *   <li><var>T</var> (as in <cite>target</cite>) is a set containing all possible output values,
 *       and potentially more elements (the <cite>codomain</cite>). For example the set of output
 *       values of the {@link Integer#toString()} function is included in a larger set, which is
 *       the set of all possible {@link String} values. In this Javadoc, <var>T</var> stands for
 *       the later set.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public enum FunctionProperty {
    /**
     * A function is <cite>invertible</cite> if it can provide an other function mapping
     * <var>T</var> values to <var>S</var> values.
     *
     * @see org.apache.sis.util.ObjectConverter#inverse()
     */
    INVERTIBLE,

    /**
     * A function is <cite>injective</cite> if each value of <var>T</var> is either unrelated
     * to <var>S</var>, or is the output of exactly one value of <var>S</var>.
     *
     * <blockquote><font size="-1"><b>Example:</b>
     * A {@code ObjectConverter} doing conversions from {@code Integer} to {@code String} is an
     * injective function, because no pair of integers can produce the same string.
     * </font></blockquote>
     *
     * A function which is both injective and {@linkplain #SURJECTIVE surjective} is a
     * <cite>bijective</cite> function. In such functions, there is a one-to-one relationship
     * between all input and output values.
     *
     * @see #SURJECTIVE
     * @see #isBijective(Set)
     */
    INJECTIVE,

    /**
     * A function is <cite>surjective</cite> if any value of <var>T</var> can be created
     * from one or many values of <var>S</var>.
     *
     * <blockquote><font size="-1"><b>Example:</b>
     * A {@code ObjectConverter} doing conversions from {@link String} to {@link Integer} is a
     * surjective function, since there is always at least one string for each integer value.
     * Note that such function can not be {@linkplain #INJECTIVE injective} since many different
     * strings can represent the same integer value.
     * </font></blockquote>
     *
     * A function which is both {@linkplain #INJECTIVE injective} and surjective is a
     * <cite>bijective</cite> function. In such functions, there is a one-to-one relationship
     * between all input and output values.
     *
     * @see #INJECTIVE
     * @see #isBijective(Set)
     */
    SURJECTIVE,

    /**
     * A function preserves order if any sequence of increasing <var>S</var> values is mapped
     * to a sequence of increasing <var>T</var> values. This enumeration constant can be used
     * only with {@link Comparable} types or primitive types having a comparable wrapper.
     *
     * <p>Strictly ordered input values are not necessarily mapped to strictly ordered output values.
     * Strictness is preserved only if the function is also {@linkplain #INJECTIVE injective}.</p>
     *
     * <p>A function may be both order preserving and {@linkplain #ORDER_REVERSING order reversing}
     * if all input values are mapped to a constant output value.</p>
     *
     * @see #ORDER_REVERSING
     * @see #isMonotonic(Set)
     */
    ORDER_PRESERVING,

    /**
     * A function reverses order if any sequence of increasing <var>S</var> values is mapped
     * to a sequence of decreasing <var>T</var> values. This enumeration constant can be used
     * only with {@link Comparable} types or primitive types having a comparable wrapper.
     *
     * <p>Strictly ordered input values are not necessarily mapped to strictly ordered output values.
     * Strictness is preserved only if the function is also {@linkplain #INJECTIVE injective}.</p>
     *
     * <p>A function may be both {@linkplain #ORDER_PRESERVING order preserving} and order reversing
     * if all input values are mapped to a constant output value.</p>
     *
     * @see #ORDER_PRESERVING
     * @see #isMonotonic(Set)
     */
    ORDER_REVERSING;

    /**
     * Bijective functions shall contains all the value in this set.
     *
     * @see #isBijective(Set)
     */
    private static EnumSet<FunctionProperty> BIJECTIVE = EnumSet.of(INJECTIVE, SURJECTIVE);

    /**
     * Returns {@code true} if a function having the given set of properties is <cite>bijective</cite>.
     * This convenience method tests if the given set contains <em>all</em> following properties:
     *
     * <ul>
     *   <li>{@link #INJECTIVE}</li>
     *   <li>{@link #SURJECTIVE}</li>
     * </u>
     *
     * @param  properties The properties of the function to test for bijectivity.
     * @return {@code true} if a function having the given set of properties is bijective.
     */
    public static boolean isBijective(final Set<FunctionProperty> properties) {
        return properties.containsAll(BIJECTIVE);
    }

    /**
     * Returns {@code true} if a function having the given set of properties is <cite>monotonic</cite>.
     * This convenience method tests if the given set contains <em>at least one</em> following properties:
     *
     * <ul>
     *   <li>{@link #ORDER_PRESERVING}</li>
     *   <li>{@link #ORDER_REVERSING}</li>
     * </u>
     *
     * @param  properties The properties of the function to test for monotonicity.
     * @return {@code true} if a function having the given set of properties is monotonic.
     */
    public static boolean isMonotonic(final Set<FunctionProperty> properties) {
        return properties.contains(ORDER_PRESERVING) || properties.contains(ORDER_REVERSING);
    }
}
