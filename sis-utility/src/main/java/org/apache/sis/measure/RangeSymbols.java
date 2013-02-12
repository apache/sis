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
package org.apache.sis.measure;


/**
 * Symbols used by {@link RangeFormat} when parsing and formatting a range.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.06)
 * @version 0.3
 * @module
 *
 * @see RangeFormat
 */
public class RangeSymbols implements Cloneable {
    /**
     * The character opening a range in which the minimal value is inclusive.
     * The default value is {@code '['}.
     */
    public char openInclusive = '[';

    /**
     * The character opening a range in which the minimal value is exclusive.
     * The default value is {@code '('}. Note that the {@code ']'} character
     * is also sometime used.
     */
    public char openExclusive = '(';

    /**
     * An alternative character opening a range in which the minimal value is exclusive.
     * This character is not used for formatting (only {@link #openExclusive} is used),
     * but is accepted during parsing. The default value is {@code ']'}.
     */
    public char openExclusiveAlt = ']';

    /**
     * The character closing a range in which the maximal value is inclusive.
     * The default value is {@code ']'}.
     */
    public char closeInclusive = ']';

    /**
     * The character closing a range in which the maximal value is exclusive.
     * The default value is {@code ')'}. Note that the {@code '['} character
     * is also sometime used.
     */
    public char closeExclusive = ')';

    /**
     * An alternative character closing a range in which the maximal value is exclusive.
     * This character is not used for formatting (only {@link #closeExclusive} is used),
     * but is accepted during parsing. The default value is {@code '['}.
     */
    public char closeExclusiveAlt = '[';

    /**
     * The string to use as a separator between minimal and maximal value, not including
     * whitespaces. The default value is {@code "…"} (Unicode 2026).
     */
    public String separator = "…";

    /**
     * Creates a new set of range symbols initialized to their default values.
     */
    public RangeSymbols() {
    }

    /**
     * Returns {@code true} if the given character is any of the opening bracket characters.
     */
    final boolean isOpen(final char c) {
        return (c == openInclusive) || (c == openExclusive) || (c == openExclusiveAlt);
    }

    /**
     * Returns {@code true} if the given character is any of the closing bracket characters.
     */
    final boolean isClose(final char c) {
        return (c == closeInclusive) || (c == closeExclusive) || (c == closeExclusiveAlt);
    }

    /**
     * Returns a clone of this set of symbols.
     */
    @Override
    public RangeSymbols clone() {
        try {
            return (RangeSymbols) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen since we are cloneable.
            throw new AssertionError(e);
        }
    }
}
