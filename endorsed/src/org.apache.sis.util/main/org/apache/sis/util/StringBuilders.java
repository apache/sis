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

import java.util.Objects;
import java.text.Normalizer;
import static java.lang.Character.*;
import org.apache.sis.util.resources.Errors;


/**
 * Static methods working on {@link StringBuilder} instances. Some methods defined in this
 * class duplicate the functionalities provided in the {@link CharSequences} class, but
 * modify directly the content of the provided {@code StringBuilder} instead of creating
 * new objects.
 *
 * <h2>Unicode support</h2>
 * Every methods defined in this class work on <i>code points</i> instead of characters
 * when appropriate. Consequently, those methods should behave correctly with characters
 * outside the <i>Basic Multilingual Plane</i> (BMP).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.6
 *
 * @see CharSequences
 *
 * @since 0.3
 */
public final class StringBuilders {
    /**
     * Letters in the range 00C0 (192) to 00FF (255) inclusive with their accent removed, when possible.
     * This string partially duplicates the work done by {@link Normalizer} with additional replacements.
     * We use it for more direct character replacements (compared to using {@code Normalizer} than removing
     * combining marks) for those common and easy cases.
     */
    private static final String ASCII = "AAAAAAÆCEEEEIIIIDNOOOOO*OUUUUYÞsaaaaaaæceeeeiiiionooooo/ouuuuyþy";
    // Original letters (with accent) = "ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ";

    /**
     * Do not allow instantiation of this class.
     */
    private StringBuilders() {
    }

    /**
     * Removes leading and trailing whitespace characters in a subregion of the specified buffer.
     * Space characters are identified by the {@link Character#isWhitespace(int)} method.
     *
     * @param  buffer  the buffer where to remove leading and trailing white spaces.
     * @param  lower   index of the first character of the subregion where to remove leading spaces.
     * @param  upper   index after the last character of the subregion where to remove trailing spaces.
     * @return number of characters removed.
     *
     * @see CharSequences#trimWhitespaces(CharSequence, int, int)
     *
     * @since 1.5
     */
    public static int trimWhitespaces(final StringBuilder buffer, final int lower, final int upper) {
        ArgumentChecks.ensureNonNull("buffer", buffer);
        int i = CharSequences.skipTrailingWhitespaces(buffer, lower, upper);
        final int length = buffer.length();
        buffer.delete(i, upper).delete(lower, CharSequences.skipLeadingWhitespaces(buffer, lower, i));
        return length - buffer.length();
    }

    /**
     * Replaces every occurrences of the given character in the given buffer.
     *
     * @param  buffer     the string in which to perform the replacements.
     * @param  toSearch   the character to replace.
     * @param  replaceBy  the replacement for the searched character.
     * @throws NullPointerException if the {@code buffer} arguments is null.
     *
     * @see String#replace(char, char)
     */
    public static void replace(final StringBuilder buffer, final char toSearch, final char replaceBy) {
        ArgumentChecks.ensureNonNull("buffer", buffer);
        if (toSearch != replaceBy) {
            for (int i=buffer.length(); --i>=0;) {
                if (buffer.charAt(i) == toSearch) {
                    buffer.setCharAt(i, replaceBy);
                }
            }
        }
    }

    /**
     * Replaces every occurrences of the given string in the given buffer.
     * This method invokes {@link StringBuilder#replace(int, int, String)}
     * for each occurrence of {@code search} found in the buffer.
     *
     * @param  buffer     the string in which to perform the replacements.
     * @param  toSearch   the string to replace.
     * @param  replaceBy  the replacement for the searched string.
     * @throws NullPointerException if any of the arguments is null.
     * @throws IllegalArgumentException if the {@code toSearch} argument is empty.
     *
     * @see String#replace(char, char)
     * @see CharSequences#replace(CharSequence, CharSequence, CharSequence)
     * @see StringBuilder#replace(int, int, String)
     */
    public static void replace(final StringBuilder buffer, final String toSearch, final String replaceBy) {
        ArgumentChecks.ensureNonNull ("buffer",    buffer);
        ArgumentChecks.ensureNonEmpty("toSearch",  toSearch);
        ArgumentChecks.ensureNonNull ("replaceBy", replaceBy);
        if (!toSearch.equals(replaceBy)) {
            final int length = toSearch.length();
            int i = buffer.length();
            while ((i = buffer.lastIndexOf(toSearch, i)) >= 0) {
                buffer.replace(i, i+length, replaceBy);
                i -= length;
            }
        }
    }

    /**
     * Replaces the characters in a substring of the buffer with characters in the specified array.
     * The substring to be replaced begins at the specified {@code start} and extends to the
     * character at index {@code end - 1}.
     *
     * @param  buffer  the buffer in which to perform the replacement.
     * @param  start   the beginning index in the {@code buffer}, inclusive.
     * @param  end     the ending index in the {@code buffer}, exclusive.
     * @param  chars   the array that will replace previous contents.
     * @throws NullPointerException if the {@code buffer} or {@code chars} argument is null.
     *
     * @see StringBuilder#replace(int, int, String)
     */
    public static void replace(final StringBuilder buffer, int start, final int end, final char[] chars) {
        ArgumentChecks.ensureNonNull("buffer", buffer);
        ArgumentChecks.ensureNonNull("chars",  chars);
        int length = end - start;
        if (start < 0 || length < 0) {
            throw new StringIndexOutOfBoundsException(Errors.format(Errors.Keys.IllegalRange_2, start, end));
        }
        final int remaining = chars.length - length;
        if (remaining < 0) {
            buffer.delete(end + remaining, end);
            length = chars.length;
        }
        for (int i=0; i<length; i++) {
            buffer.setCharAt(start++, chars[i]);
        }
        if (remaining > 0) {
            buffer.insert(start, chars, length, remaining);
        }
    }

    /**
     * Removes every occurrences of the given string in the given buffer. This method invokes
     * {@link StringBuilder#delete(int, int)} for each occurrence of {@code search} found in
     * the buffer.
     *
     * @param  buffer    the string in which to perform the removals.
     * @param  toSearch  the string to remove.
     * @throws NullPointerException if any of the arguments is null.
     * @throws IllegalArgumentException if the {@code toSearch} argument is empty.
     *
     * @see StringBuilder#delete(int, int)
     */
    public static void remove(final StringBuilder buffer, final String toSearch) {
        ArgumentChecks.ensureNonNull ("buffer",   buffer);
        ArgumentChecks.ensureNonEmpty("toSearch", toSearch);
        final int length = toSearch.length();
        for (int i=buffer.lastIndexOf(toSearch); i>=0; i=buffer.lastIndexOf(toSearch, i)) {
            buffer.delete(i, i + length);
        }
    }

    /**
     * Inserts the given character <var>n</var> times at the given position.
     * This method does nothing if the given {@code count} is zero.
     *
     * @param  buffer  the buffer where to insert the character.
     * @param  offset  position where to insert the characters.
     * @param  c       the character to repeat.
     * @param  count   number of times to repeat the given character.
     * @throws NullPointerException if the given buffer is null.
     * @throws IndexOutOfBoundsException if the given index is invalid.
     * @throws IllegalArgumentException if the given count is negative.
     *
     * @since 0.8
     */
    public static void repeat(final StringBuilder buffer, final int offset, final char c, final int count) {
        switch (count) {
            case 0:  break;
            case 1:  buffer.insert(offset, c); break;
            default: {
                ArgumentChecks.ensurePositive("count", count);
                final CharSequence r;
                switch (c) {
                    case ' ': r = CharSequences.spaces(count); break;
                    case '0': r = Repeat.ZERO; break;
                    default:  r = new Repeat(c, count); break;
                }
                buffer.insert(offset, r, 0, count);
                break;
            }
        }
    }

    /**
     * A sequence of a constant character. This implementation does not perform any argument
     * check since it is for {@link StringBuilder#append(CharSequence, int, int)} usage only.
     * The intent is to allow {@code StringBuilder} to append the characters in one operation
     * instead of looping on {@link StringBuilder#insert(int, char)} (which would require
     * memory move on each call).
     */
    private static final class Repeat implements CharSequence {
        /** An infinite sequence of {@code '0'} character. */
        static final Repeat ZERO = new Repeat('0', Integer.MAX_VALUE);

        /** The character to repeat. */
        private final char c;

        /** Number of times the character is repeated. */
        private final int n;

        /** Creates a new sequence of constant character. */
        Repeat(final char c, final int n) {
            this.c = c;
            this.n = n;
        }

        /** Returns the number of times the character is repeated. */
        @Override public int length() {return n;}

        /** Returns the constant character, regardless the index. */
        @Override public char charAt(int i) {return c;}

        /** Returns a sequence of the same constant character but different length. */
        @Override public CharSequence subSequence(int start, int end) {return new Repeat(c, end - start);}
    }

    /**
     * Trims the fractional part of the given formatted number, provided that it doesn't change
     * the value. This method assumes that the number is formatted in the US locale, typically
     * by the {@link Double#toString(double)} method.
     *
     * <p>More specifically if the given buffer ends with a {@code '.'} character followed by a
     * sequence of {@code '0'} characters, then those characters are removed. Otherwise this
     * method does nothing. This is a <q>all or nothing</q> method: either the fractional
     * part is completely removed, or either it is left unchanged.</p>
     *
     * <h4>Use case</h4>
     * This method is useful after a {@linkplain StringBuilder#append(double) double value has
     * been appended to the buffer}, in order to make it appears like an integer when possible.
     *
     * @param  buffer  the buffer to trim if possible.
     * @throws NullPointerException if the given {@code buffer} is null.
     *
     * @see CharSequences#trimFractionalPart(CharSequence)
     */
    @SuppressWarnings("fallthrough")
    public static void trimFractionalPart(final StringBuilder buffer) {
        for (int i=buffer.length(); i > 0;) {
            switch (buffer.charAt(--i)) {               // No need to use Unicode code points here.
                case '0': continue;
                case '.': buffer.setLength(i);          // Fall through
                default : return;
            }
        }
    }

    /**
     * Replaces some Unicode characters by ASCII characters on a "best effort basis".
     * For example, the “ é ” character is replaced by  “ e ” (without accent),
     * the  “ ″ ” symbol for minutes of angle is replaced by straight double quotes “ " ”,
     * and combined characters like ㎏, ㎎, ㎝, ㎞, ㎢, ㎦, ㎖, ㎧, ㎩, ㎐, <i>etc.</i> are replaced
     * by the corresponding sequences of characters.
     *
     * @param  buffer  the text to scan for Unicode characters to replace by ASCII characters.
     * @throws NullPointerException if the given {@code buffer} is null.
     *
     * @see CharSequences#toASCII(CharSequence)
     * @see Normalizer#normalize(CharSequence, Normalizer.Form)
     */
    public static void toASCII(final StringBuilder buffer) {
        toASCII(Objects.requireNonNull(buffer), buffer);
    }

    /**
     * Implementation of the public {@code toASCII} methods.
     */
    static CharSequence toASCII(CharSequence text, StringBuilder buffer) {
        if (text != null) {
            boolean doneNFKD = false;
            /*
             * Scan the buffer in reverse order because we may suppress some characters.
             */
            int i = text.length();
            while (i > 0) {
                final int c = codePointBefore(text, i);
                final int n = charCount(c);
                final int r = c - 0xC0;
                i -= n;                                     // After this line, 'i' is the index of character 'c'.
                if (r >= 0) {
                    final char cr;                          // The character replacement.
                    if (r < ASCII.length()) {
                        cr = ASCII.charAt(r);
                    } else {
                        switch (getType(c)) {
                            case FORMAT:
                            case CONTROL:                   // Character.isIdentifierIgnorable
                            case NON_SPACING_MARK:          cr = 0; break;
                            case PARAGRAPH_SEPARATOR:       // Fall through
                            case LINE_SEPARATOR:            cr = '\n'; break;
                            case SPACE_SEPARATOR:           cr = ' '; break;
                            case INITIAL_QUOTE_PUNCTUATION: cr = (c == '‘') ? '\'' : '"'; break;
                            case FINAL_QUOTE_PUNCTUATION:   cr = (c == '’') ? '\'' : '"'; break;
                            case OTHER_PUNCTUATION:
                            case MATH_SYMBOL: {
                                switch (c) {
                                    case '⋅': cr = '*';  break;
                                    case '∕': cr = '/';  break;
                                    case '′': cr = '\''; break;
                                    case '″': cr = '"';  break;
                                    default:  continue;
                                }
                                break;
                            }
                            default: {
                                /*
                                 * For any unknown character, try to decompose the string in a sequence of simpler
                                 * letters with their modifiers and restart the whole process from the beginning.
                                 * If the character is still unknown after decomposition, leave it unchanged.
                                 */
                                if (!doneNFKD) {
                                    doneNFKD = true;
                                    final String decomposed = Normalizer.normalize(text, Normalizer.Form.NFKD);
                                    if (!decomposed.contentEquals(text)) {
                                        if (buffer == null) {
                                            text = buffer = new StringBuilder(decomposed.length());
                                        } else {
                                            buffer.setLength(0);
                                        }
                                        i = buffer.append(decomposed).length();
                                    }
                                }
                                continue;
                            }
                        }
                    }
                    if (buffer == null) {
                        buffer = new StringBuilder(text.length()).append(text);
                        text = buffer;
                    }
                    if (cr == 0) {
                        buffer.delete(i, i + n);
                    } else {
                        if (n == 2) {
                            buffer.deleteCharAt(i + 1);         // Remove the low surrogate of a surrogate pair.
                        }
                        /*
                         * Nothing special to do about codepoint here, since 'c' is in
                         * the basic plane (verified by the r < ASCII.length() check).
                         */
                        buffer.setCharAt(i, cr);
                    }
                }
            }
        }
        return text;
    }
}
