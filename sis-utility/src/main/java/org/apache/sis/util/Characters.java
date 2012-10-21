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


/**
 * Static methods working on {@code char} values.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
public final class Characters extends Static {
    /**
     * The <cite>no-break space</cite> (Unicode: <code>\\u00A0</code>, HTML: {@code &nbsp;}).
     * Apache SIS uses Unicode symbols directly in the source code for easier reading,
     * except for no-break spaces since they can not be visually distinguished from the
     * ordinary space (Unicode: <code>\\u0020</code>).
     */
    public static final char NO_BREAK_SPACE = '\u00A0';

    /**
     * The Unicode line separator (Unicode: <code>\\u2028</code>, HTML: {@code <br>}).
     *
     * @see Character#LINE_SEPARATOR
     */
    public static final char LINE_SEPARATOR = '\u2028';

    /**
     * The Unicode paragraph separator (Unicode: <code>\\u2029</code>, HTML: {@code <p>…</p>}).
     *
     * @see Character#PARAGRAPH_SEPARATOR
     */
    public static final char PARAGRAPH_SEPARATOR = '\u2029';

    /**
     * Do not allow instantiation of this class.
     */
    private Characters() {
    }

    /**
     * Returns {@code true} if the given code point is a {@linkplain Character#LINE_SEPARATOR
     * line separator}, a {@linkplain Character#PARAGRAPH_SEPARATOR paragraph separator} or one
     * of the {@code '\r'} or {@code '\n'} control characters.
     *
     * @param  c The code point to test.
     * @return {@code true} if the given code point is a line or paragraph separator.
     *
     * @see #LINE_SEPARATOR
     * @see #PARAGRAPH_SEPARATOR
     */
    public static boolean isLineOrParagraphSeparator(final int c) {
        switch (Character.getType(c)) {
            default: return false;
            case Character.LINE_SEPARATOR:
            case Character.PARAGRAPH_SEPARATOR: return true;
            case Character.CONTROL: return (c == '\r') || (c == '\n');
        }
    }

    /**
     * Determines whether the given character is a superscript. Most (but not all) superscripts
     * have a Unicode value in the [2070 … 207F] range. Superscripts are the following symbols:
     *
     * {@preformat text
     *   ⁰ ¹ ² ³ ⁴ ⁵ ⁶ ⁷ ⁸ ⁹ ⁺ ⁻ ⁼ ⁽ ⁾ ⁿ
     * }
     *
     * @param  c The character to test.
     * @return {@code true} if the given character is a superscript.
     */
    public static boolean isSuperScript(final char c) {
        switch (c) {
            case '¹':      // Legacy values in "Latin-1 supplement" space: 00B9, 00B2 and 00B3.
            case '²':      // Those values are outside the normal [2070 … 207F] range.
            case '³':      return true;
            case '\u2071': // Would be the '¹', '²' and '³' values if they were declared in the
            case '\u2072': // normal range. Since they are not, those values are unassigned.
            case '\u2073': return false;
            default:       return (c>='⁰' && c<='ⁿ');
        }
    }

    /**
     * Determines whether the given character is a subscript. All subscripts have
     * a Unicode value in the [2080 … 208E]. Subscripts are the following symbols:
     *
     * {@preformat text
     *   ₀ ₁ ₂ ₃ ₄ ₅ ₆ ₇ ₈ ₉ ₊ ₋ ₌ ₍ ₎
     * }
     *
     * @param  c The character to test.
     * @return {@code true} if the given character is a subscript.
     */
    public static boolean isSubScript(final char c) {
        return (c>='₀' && c<='₎');
    }

    /**
     * Converts the given character argument to superscript.
     * Only the following characters can be converted (other characters are left unchanged):
     *
     * {@preformat text
     *     0 1 2 3 4 5 6 7 8 9 + - = ( ) n
     * }
     *
     * @param  c The character to convert.
     * @return The given character as a superscript, or {@code c}
     *         if the given character can not be converted.
     */
    public static char toSuperScript(char c) {
        switch (c) {
            case '1': c = '¹'; break;  // 00B9
            case '2': c = '²'; break;  // 00B2
            case '3': c = '³'; break;  // 00B3
            case '+': c = '⁺'; break;  // 207A
            case '-': c = '⁻'; break;  // 207B
            case '=': c = '⁼'; break;  // 207C
            case '(': c = '⁽'; break;  // 207D
            case ')': c = '⁾'; break;  // 207E
            case 'n': c = 'ⁿ'; break;  // 207F
            default: {
                if (c >= '0' && c <= '9') {
                    c += ('⁰' - '0');
                }
                break;
            }
        }
        return c;
    }

    /**
     * Converts the given character argument to subscript.
     * Only the following characters can be converted (other characters are left unchanged):
     *
     * {@preformat text
     *     0 1 2 3 4 5 6 7 8 9 + - = ( )
     * }
     *
     * @param  c The character to convert.
     * @return The given character as a subscript, or {@code c}
     *         if the given character can not be converted.
     */
    public static char toSubScript(char c) {
        switch (c) {
            case '+': c = '₊'; break;  // 208A
            case '-': c = '₋'; break;  // 208B
            case '=': c = '₌'; break;  // 208C
            case '(': c = '₍'; break;  // 208D
            case ')': c = '₎'; break;  // 208E
            default: {
                if (c >= '0' && c <= '9') {
                    c += ('₀' - '0');
                }
                break;
            }
        }
        return c;
    }

    /**
     * Converts the given character argument to normal script.
     *
     * @param  c The character to convert.
     * @return The given character as a normal script, or {@code c} if the
     *         given character was not a superscript or a subscript.
     */
    public static char toNormalScript(char c) {
        switch (c) {
            case '\u2071': // Exceptions to the default case. They would be the ¹²³
            case '\u2072': // cases if they were not defined in the Latin-1 range.
            case '\u2073':               break;
            case '¹':           c = '1'; break;
            case '²':           c = '2'; break;
            case '³':           c = '3'; break;
            case '⁺': case '₊': c = '+'; break;
            case '⁻': case '₋': c = '-'; break;
            case '⁼': case '₌': c = '='; break;
            case '⁽': case '₍': c = '('; break;
            case '⁾': case '₎': c = ')'; break;
            case 'ⁿ':           c = 'n'; break;
            default: {
                if (c >= '⁰' && c <= '₉') {
                    if      (c <= '⁹') c -= ('⁰' - '0');
                    else if (c >= '₀') c -= ('₀' - '0');
                }
                break;
            }
        }
        return c;
    }
}
