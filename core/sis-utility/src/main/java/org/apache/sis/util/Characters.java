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

import org.opengis.metadata.citation.Citation;  // For javadoc.
import org.apache.sis.util.resources.Errors;


/**
 * Static methods working on {@code char} values, and some character constants.
 * Apache SIS uses Unicode symbols directly in the source code for easier reading,
 * except for some symbols that are difficult to differentiate from other similar
 * symbols. For those symbols, constants are declared in this class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.6
 * @module
 */
public final class Characters extends Static {
    /**
     * Hyphen character ('\u2010', Unicode {@code 2010}).
     * This code tells to {@link org.apache.sis.io.LineAppender}
     * that a line break is allowed to be inserted after this character.
     *
     * <p>For non-breaking hyphen, use the Unicode {@code 2011} character.</p>
     */
    public static final char HYPHEN = '\u2010';

    /**
     * Hyphen character to be visible only if there is a line break to insert after it
     * (Unicode {@code 00AD}, HTML {@code &shy;}).
     * Otherwise this character is invisible. When visible, the graphical symbol is similar
     * to the {@link #HYPHEN} character.
     */
    public static final char SOFT_HYPHEN = '\u00AD';

    /**
     * The <cite>no-break space</cite> (Unicode {@code 00A0}, HTML {@code &nbsp;}).
     * Apache SIS uses Unicode symbols directly in the source code for easier reading,
     * except for no-break spaces since they can not be visually distinguished from the
     * ordinary space (Unicode {@code 0020}).
     */
    public static final char NO_BREAK_SPACE = '\u00A0';

    /**
     * The Unicode line separator (Unicode {@code 2028}, HTML {@code <br>}).
     *
     * @see Character#LINE_SEPARATOR
     */
    public static final char LINE_SEPARATOR = '\u2028';

    /**
     * The Unicode paragraph separator (Unicode {@code 2029}, HTML {@code <p>…</p>}).
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
     * Returns {@code true} if the given code point is a valid character for <cite>Well Known Text</cite> (WKT).
     * This method returns {@code true} for the following characters:
     *
     * <blockquote><pre>{@literal A-Z a-z 0-9 _ [ ] ( ) { } < = > . , : ; + - (space) % & ' " * ^ / \ ? | °}</pre></blockquote>
     *
     * They are ASCII codes 32 to 125 inclusive except ! (33), # (35), $ (36), @ (64) and ` (96),
     * plus the addition of ° (176) despite being formally outside the ASCII character set.
     *
     * @param  c The code point to test.
     * @return {@code true} if the given code point is a valid WKT character.
     *
     * @see org.apache.sis.io.wkt.Transliterator
     *
     * @since 0.6
     */
    public static boolean isValidWKT(final int c) {
        switch (c) {
            case '!':
            case '#':
            case '$':
            case '@':
            case '`': return false;
            case '°': return true;
            default : return (c >= ' ') && (c <= '}');
        }
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
     * Returns {@code true} if the given character is an hexadecimal digit.
     * This method returns {@code true} if {@code c} is between {@code '0'} and {@code '9'} inclusive,
     * or between {@code 'A'} and {@code 'F'} inclusive, or between {@code 'a'} and {@code 'f'} inclusive.
     *
     * @param  c The character to test.
     * @return {@code true} if the given character is an hexadecimal digit.
     *
     * @since 0.5
     */
    public static boolean isHexadecimal(int c) {
        /*
         * The &= ~32 is a cheap conversion of lower-case letters to upper-case letters.
         * It is not a rigorous conversion since it does not check if 'c' is a letter,
         * but for the purpose of this method it is okay.
         */
        return (c >= '0' && c <= '9') || ((c &= ~32) >= 'A' && c <= 'F');
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
    public static boolean isSuperScript(final int c) {
        switch (c) {
            case '¹':      // Legacy values in "Latin-1 supplement" space: 00B9, 00B2 and 00B3.
            case '²':      // Those values are outside the normal [2070 … 207F] range.
            case '³':      return true;
            case '\u2071': // Would be the '¹', '²' and '³' values if they were declared in the
            case '\u2072': // normal range. Since they are not, those values are unassigned.
            case '\u2073': return false;
            default:       return (c >= '⁰' && c <= 'ⁿ');
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
    public static boolean isSubScript(final int c) {
        return (c >= '₀' && c <= '₎');
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




    /**
     * Subsets of Unicode characters identified by their general category.
     * The categories are identified by constants defined in the {@link Character} class, like
     * {@link Character#LOWERCASE_LETTER     LOWERCASE_LETTER},
     * {@link Character#UPPERCASE_LETTER     UPPERCASE_LETTER},
     * {@link Character#DECIMAL_DIGIT_NUMBER DECIMAL_DIGIT_NUMBER} and
     * {@link Character#SPACE_SEPARATOR      SPACE_SEPARATOR}.
     *
     * <p>An instance of this class can be obtained from an enumeration of character types
     * using the {@link #forTypes(byte[])} method, or using one of the constants predefined
     * in this class. Then, Unicode characters can be tested for inclusion in the subset by
     * calling the {@link #contains(int)} method.</p>
     *
     * <div class="section">Relationship with international standards</div>
     * ISO 19162:2015 §B.5.2 recommends to ignore spaces, case and the following characters when comparing two
     * {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#getName() identified object names}:
     * “{@code _}” (underscore), “{@code -}” (minus sign), “{@code /}” (solidus),
     * “{@code (}” (left parenthesis) and “{@code )}” (right parenthesis).
     * The same specification also limits the set of valid characters in a name to the following (§6.3.1):
     *
     * <blockquote>{@code A-Z a-z 0-9 _ [ ] ( ) { } < = > . , : ; + - (space) % & ' " * ^ / \ ? | °}</blockquote>
     * <div class="note"><b>Note:</b> SIS does not enforce this restriction in its programmatic API,
     * but may perform some character substitutions at <cite>Well Known Text</cite> (WKT) formatting time.</div>
     *
     * If we take only the characters in the above list which are valid in a {@linkplain #UNICODE_IDENTIFIER
     * Unicode identifier} and remove the characters that ISO 19162 recommends to ignore, the only characters
     * left are {@linkplain #LETTERS_AND_DIGITS letters and digits}.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     *
     * @see java.lang.Character.Subset
     * @see Character#getType(int)
     * @see <a href="http://docs.opengeospatial.org/is/12-063r5/12-063r5.html#139">WKT 2 specification §B.5</a>
     */
    public static class Filter extends Character.Subset {
        /*
         * This class can not easily be Serializable, because the parent class is not Serializable
         * and does not define a no-argument constructor.  We could workaround with a writeReplace
         * method - waiting to see if there is a real need for that.
         */

        /**
         * The subset of all characters for which {@link Character#isLetterOrDigit(int)}
         * returns {@code true}. This subset includes the following general categories:
         *
         * <blockquote>
         * {@link Character#LOWERCASE_LETTER},
         * {@link Character#UPPERCASE_LETTER     UPPERCASE_LETTER},
         * {@link Character#TITLECASE_LETTER     TITLECASE_LETTER},
         * {@link Character#MODIFIER_LETTER      MODIFIER_LETTER},
         * {@link Character#OTHER_LETTER         OTHER_LETTER} and
         * {@link Character#DECIMAL_DIGIT_NUMBER DECIMAL_DIGIT_NUMBER}.
         * </blockquote>
         *
         * SIS uses this filter when comparing two
         * {@linkplain org.apache.sis.referencing.AbstractIdentifiedObject#getName() identified object names}.
         * See the <cite>Relationship with international standards</cite> section in this class javadoc
         * for more information.
         *
         * @see org.apache.sis.referencing.AbstractIdentifiedObject#isHeuristicMatchForName(String)
         * @see org.apache.sis.metadata.iso.citation.Citations#identifierMatches(Citation, String)
         */
        public static final Filter LETTERS_AND_DIGITS = new LettersAndDigits();

        /**
         * The subset of all characters for which {@link Character#isUnicodeIdentifierPart(int)}
         * returns {@code true}, excluding {@linkplain Character#isIdentifierIgnorable(int) ignorable} characters.
         * This subset includes all the {@link #LETTERS_AND_DIGITS} categories with the addition of the following
         * ones:
         *
         * <blockquote>
         * {@link Character#LETTER_NUMBER},
         * {@link Character#CONNECTOR_PUNCTUATION CONNECTOR_PUNCTUATION},
         * {@link Character#NON_SPACING_MARK NON_SPACING_MARK} and
         * {@link Character#COMBINING_SPACING_MARK COMBINING_SPACING_MARK}.
         * </blockquote>
         */
        public static final Filter UNICODE_IDENTIFIER = new UnicodeIdentifier();

        /**
         * A bitmask of character types in this subset.
         */
        private final long types;

        /**
         * Creates a new subset of the given name.
         *
         * @param name  The subset name.
         * @param types A bitmask of character types.
         */
        Filter(final String name, final long types) {
            super(name);
            this.types = types;
        }

        /**
         * Returns {@code true} if this subset contains the given Unicode character.
         *
         * @param  codePoint The Unicode character, as a code point value.
         * @return {@code true} if this subset contains the given character.
         */
        public boolean contains(final int codePoint) {
            return containsType(Character.getType(codePoint));
        }

        /**
         * Returns {@code true} if this subset contains the characters of the given type.
         * The given type shall be one of the {@link Character} constants like
         * {@link Character#LOWERCASE_LETTER     LOWERCASE_LETTER},
         * {@link Character#UPPERCASE_LETTER     UPPERCASE_LETTER},
         * {@link Character#DECIMAL_DIGIT_NUMBER DECIMAL_DIGIT_NUMBER} or
         * {@link Character#SPACE_SEPARATOR      SPACE_SEPARATOR}.
         *
         * @param  type One of the {@link Character} constants.
         * @return {@code true} if this subset contains the characters of the given type.
         *
         * @see Character#getType(int)
         */
        public final boolean containsType(final int type) {
            return (type >= 0) && (type < Long.SIZE) && (types & (1L << type)) != 0;
        }

        /**
         * Returns a subset representing the union of all Unicode characters of the given types.
         *
         * @param  types The character types, as {@link Character} constants.
         * @return The subset of Unicode characters of the given type.
         *
         * @see Character#LOWERCASE_LETTER
         * @see Character#UPPERCASE_LETTER
         * @see Character#DECIMAL_DIGIT_NUMBER
         * @see Character#SPACE_SEPARATOR
         */
        public static Filter forTypes(final byte... types) {
            long mask = 0;
            for (int i=0; i<types.length; i++) {
                final int type = types[i];
                if (type < 0 || type >= Long.SIZE) {
                    throw new IllegalArgumentException(Errors.format(
                            Errors.Keys.IllegalArgumentValue_2, "types[" + i + ']', type));
                }
                mask |= (1L << type);
            }
predefined: for (int i=0; ; i++) {
                final Filter candidate;
                switch (i) {
                    case 0:  candidate = LETTERS_AND_DIGITS; break;
                    case 1:  candidate = UNICODE_IDENTIFIER; break;
                    default: break predefined;
                }
                if (mask == candidate.types) {
                    return candidate;
                }
            }
            return new Filter("Filter", mask);
        }
    }

    /**
     * Implementation of the {@link Filter#LETTERS_AND_DIGITS} constant.
     */
    private static final class LettersAndDigits extends Filter {
        /**
         * Creates the {@link Filter#LETTERS_AND_DIGITS} singleton instance.
         */
        LettersAndDigits() {
            super("LETTERS_AND_DIGITS",
                      (1L << Character.LOWERCASE_LETTER)
                    | (1L << Character.UPPERCASE_LETTER)
                    | (1L << Character.TITLECASE_LETTER)
                    | (1L << Character.MODIFIER_LETTER)
                    | (1L << Character.OTHER_LETTER)
                    | (1L << Character.DECIMAL_DIGIT_NUMBER));
        }

        /**
         * Returns {@code true} if this subset contains the given Unicode character.
         */
        @Override
        public boolean contains(final int codePoint) {
            return Character.isLetterOrDigit(codePoint);
        }
    }

    /**
     * Implementation of the {@link Filter#UNICODE_IDENTIFIER} constant.
     */
    private static final class UnicodeIdentifier extends Filter {
        /**
         * Creates the {@link Filter#LETTERS_AND_DIGITS} singleton instance.
         */
        UnicodeIdentifier() {
            super("UNICODE_IDENTIFIER",
                      (1L << Character.LOWERCASE_LETTER)
                    | (1L << Character.UPPERCASE_LETTER)
                    | (1L << Character.TITLECASE_LETTER)
                    | (1L << Character.MODIFIER_LETTER)
                    | (1L << Character.OTHER_LETTER)
                    | (1L << Character.DECIMAL_DIGIT_NUMBER)
                    | (1L << Character.LETTER_NUMBER)
                    | (1L << Character.CONNECTOR_PUNCTUATION)
                    | (1L << Character.NON_SPACING_MARK)
                    | (1L << Character.COMBINING_SPACING_MARK));
        }

        /**
         * Returns {@code true} if this subset contains the given Unicode character.
         */
        @Override
        public boolean contains(final int codePoint) {
            return Character.isUnicodeIdentifierPart(codePoint) &&
                  !Character.isIdentifierIgnorable(codePoint);
        }
    }
}
