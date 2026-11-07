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
package org.apache.sis.style.se1.function;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.opengis.filter.Expression;


/**
 * Extraction of a part of a string.
 * Returns the substring starting at the given position (counting from 1) with the given length.
 * The string value is converted to a string before applying the operation.
 * If the position is not specified, it is assumed to be 1.
 * If the length is not specified, the substring extends to the end of the string.
 *
 * <p>Invalid positions and lengths are handled friendly: positions and lengths lower than or equal to 0
 * yield the empty string, and if the string is shorter than the requested substring, the existing part is
 * returned. Positions and lengths are counted in Unicode code points. A null string value gives null.</p>
 *
 * <p>This function is available by the name {@code "Substring"} in Apache SIS filter factories,
 * with the string, position and length as parameters (the last two being optional).</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @param <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 */
public final class Substring<R> extends SymbologyFunction<R,String> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5230983462210916436L;

    /**
     * The name of this function.
     */
    static final String NAME = "Substring";

    /**
     * The string from which to extract a substring.
     */
    private final Expression<R,?> stringValue;

    /**
     * The position of the first character (counting from 1), or {@code null} for 1.
     */
    private final Expression<R,?> position;

    /**
     * The number of characters, or {@code null} for the remaining characters.
     */
    private final Expression<R,?> length;

    /**
     * Creates a new substring function.
     *
     * @param  stringValue  the string from which to extract a substring.
     * @param  position     the position of the first character (counting from 1), or {@code null} for 1.
     * @param  length       the number of characters, or {@code null} for the remaining characters.
     */
    public Substring(final Expression<R,?> stringValue,
                     final Expression<R,?> position,
                     final Expression<R,?> length)
    {
        super(NAME, String.class);
        this.stringValue = Objects.requireNonNull(stringValue, "stringValue");
        this.position    = position;
        this.length      = length;
    }

    /**
     * Returns the string from which to extract a substring.
     *
     * @return the string value.
     */
    public Expression<R,?> getStringValue() {
        return stringValue;
    }

    /**
     * Returns the position of the first character, counting from 1.
     *
     * @return the position, or empty for 1.
     */
    public Optional<Expression<R,?>> getPosition() {
        return Optional.ofNullable(position);
    }

    /**
     * Returns the number of characters.
     *
     * @return the length, or empty for the remaining characters.
     */
    public Optional<Expression<R,?>> getLength() {
        return Optional.ofNullable(length);
    }

    /**
     * Returns the parameters of this function: the string value, then the position and length if specified.
     * If only the length is specified, the position 1 is added before it.
     *
     * @return the function parameters.
     */
    @Override
    public List<Expression<R,?>> getParameters() {
        final List<Expression<R,?>> parameters = new ArrayList<>(3);
        parameters.add(stringValue);
        if (position != null || length != null) {
            parameters.add((position != null) ? position : new Keyword<>("1"));
        }
        if (length != null) {
            parameters.add(length);
        }
        return List.copyOf(parameters);
    }

    /**
     * Extracts the substring from the string value evaluated on the given resource.
     *
     * @param  input  the resource on which to evaluate the expressions.
     * @return the substring, or {@code null} if the string value is null.
     */
    @Override
    public String apply(final R input) {
        final String text = string(stringValue.apply(input));
        if (text == null) {
            return null;
        }
        final double start = (position != null) ? number(position.apply(input)) : 1;
        if (!(start > 0)) {
            return "";
        }
        final int count = text.codePointCount(0, text.length());
        final long first = (long) start - 1;
        if (first >= count) {
            return "";
        }
        long end = count;
        if (length != null) {
            final double n = number(length.apply(input));
            if (!(n > 0)) {
                return "";
            }
            end = Math.min(count, first + (long) Math.min(n, Integer.MAX_VALUE));
        }
        return text.substring(text.offsetByCodePoints(0, (int) first), text.offsetByCodePoints(0, (int) end));
    }

    @Override
    Object[] properties() {
        return new Object[] {stringValue, position, length};
    }
}
