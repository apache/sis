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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.opengis.filter.Expression;


/**
 * Removal of leading and/or trailing characters of a string.
 * By default, the leading blank characters are removed.
 * The value is converted to a string before trimming. A null value gives null.
 *
 * <p>The characters to strip are given by a string: all characters of that string are stripped.
 * In the usual case, that string contains a single character.</p>
 *
 * <p>This function is available by the name {@code "Trim"} in Apache SIS filter factories,
 * with the string as parameter optionally followed by the {@code "leading"}, {@code "trailing"}
 * or {@code "both"} keyword, optionally followed by the character to strip.</p>
 *
 * @author  Johann Sorel (Geomatys)
 * @param <R>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 */
public final class Trim<R> extends SymbologyFunction<R,String> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -3009128349542117830L;

    /**
     * The name of this function.
     */
    static final String NAME = "Trim";

    /**
     * Where to strip off characters.
     */
    public enum StripOffPosition {
        /**
         * Strip off characters at the beginning of the string. This is the default.
         */
        LEADING,

        /**
         * Strip off characters at the end of the string.
         */
        TRAILING,

        /**
         * Strip off characters at both the beginning and the end of the string.
         */
        BOTH
    }

    /**
     * The string to trim.
     */
    private final Expression<R,?> stringValue;

    /**
     * Where to strip off characters.
     */
    private final StripOffPosition stripOffPosition;

    /**
     * The characters to strip off, or {@code null} for the blank character.
     */
    private final Expression<R,?> stripOffChar;

    /**
     * Creates a new trim function.
     *
     * @param  stringValue       the string to trim.
     * @param  stripOffPosition  where to strip off characters, or {@code null} for the default ({@code LEADING}).
     * @param  stripOffChar      the characters to strip off, or {@code null} for the blank character.
     */
    public Trim(final Expression<R,?> stringValue, final StripOffPosition stripOffPosition, final Expression<R,?> stripOffChar) {
        super(NAME, String.class);
        this.stringValue      = Objects.requireNonNull(stringValue, "stringValue");
        this.stripOffPosition = (stripOffPosition != null) ? stripOffPosition : StripOffPosition.LEADING;
        this.stripOffChar     = stripOffChar;
    }

    /**
     * Returns the string to trim.
     *
     * @return the string value.
     */
    public Expression<R,?> getStringValue() {
        return stringValue;
    }

    /**
     * Returns where to strip off characters.
     *
     * @return the strip off position.
     */
    public StripOffPosition getStripOffPosition() {
        return stripOffPosition;
    }

    /**
     * Returns the characters to strip off.
     *
     * @return the characters to strip off, or empty for the blank character.
     */
    public Optional<Expression<R,?>> getStripOffChar() {
        return Optional.ofNullable(stripOffChar);
    }

    /**
     * Returns the parameters of this function: the string value, the position keyword,
     * and the characters to strip off if specified.
     *
     * @return the function parameters.
     */
    @Override
    public List<Expression<R,?>> getParameters() {
        final Expression<R,?> keyword = new Keyword<>(keyword(stripOffPosition));
        return (stripOffChar != null) ? List.of(stringValue, keyword, stripOffChar) : List.of(stringValue, keyword);
    }

    /**
     * Trims the string value evaluated on the given resource.
     *
     * @param  input  the resource on which to evaluate the expressions.
     * @return the trimmed string, or {@code null} if the string value is null.
     */
    @Override
    public String apply(final R input) {
        final String text = string(stringValue.apply(input));
        if (text == null) {
            return null;
        }
        String chars = (stripOffChar != null) ? string(stripOffChar.apply(input)) : null;
        if (chars == null || chars.isEmpty()) {
            chars = " ";
        }
        int start = 0, end = text.length();
        if (stripOffPosition != StripOffPosition.TRAILING) {
            while (start < end) {
                final int c = text.codePointAt(start);
                if (chars.indexOf(c) < 0) break;
                start += Character.charCount(c);
            }
        }
        if (stripOffPosition != StripOffPosition.LEADING) {
            while (end > start) {
                final int c = text.codePointBefore(end);
                if (chars.indexOf(c) < 0) break;
                end -= Character.charCount(c);
            }
        }
        return text.substring(start, end);
    }

    @Override
    Object[] properties() {
        return new Object[] {stringValue, stripOffPosition, stripOffChar};
    }
}
