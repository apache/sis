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

import org.apache.sis.util.resources.Errors;

import static java.lang.Character.*;


/**
 * Static methods working on {@link StringBuilder} instances. Some methods defined in this
 * class duplicate the functionalities provided in the {@link CharSequences} class, but
 * modify directly the content of the provided {@code StringBuilder} instead than creating
 * new objects.
 *
 * <div class="section">Unicode support</div>
 * Every methods defined in this class work on <cite>code points</cite> instead than characters
 * when appropriate. Consequently those methods should behave correctly with characters outside
 * the <cite>Basic Multilingual Plane</cite> (BMP).
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see CharSequences
 */
public final class StringBuilders extends Static {
    /**
     * Letters in the range 00C0 (192) to 00FF (255) inclusive with their accent removed, when possible.
     */
    private static final String ASCII = "AAAAAAÆCEEEEIIIIDNOOOOO*OUUUUYÞsaaaaaaæceeeeiiiionooooo/ouuuuyþy";
    // Original letters (with accent) = "ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ";

    /**
     * Do not allow instantiation of this class.
     */
    private StringBuilders() {
    }

    /**
     * Replaces every occurrences of the given character in the given buffer.
     *
     * @param  buffer    The string in which to perform the replacements.
     * @param  toSearch  The character to replace.
     * @param  replaceBy The replacement for the searched character.
     * @throws NullArgumentException If the {@code buffer} arguments is null.
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
     * @param  buffer    The string in which to perform the replacements.
     * @param  toSearch  The string to replace.
     * @param  replaceBy The replacement for the searched string.
     * @throws NullArgumentException If any of the arguments is null.
     * @throws IllegalArgumentException If the {@code toSearch} argument is empty.
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
     * @param  buffer The buffer in which to perform the replacement.
     * @param  start  The beginning index in the {@code buffer}, inclusive.
     * @param  end    The ending index in the {@code buffer}, exclusive.
     * @param  chars  The array that will replace previous contents.
     * @throws NullArgumentException if the {@code buffer} or {@code chars} argument is null.
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
     * @param  buffer   The string in which to perform the removals.
     * @param  toSearch The string to remove.
     * @throws NullPointerException If any of the arguments is null.
     * @throws IllegalArgumentException If the {@code toSearch} argument is empty.
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
     * Trims the fractional part of the given formatted number, provided that it doesn't change
     * the value. This method assumes that the number is formatted in the US locale, typically
     * by the {@link Double#toString(double)} method.
     *
     * <p>More specifically if the given buffer ends with a {@code '.'} character followed by a
     * sequence of {@code '0'} characters, then those characters are removed. Otherwise this
     * method does nothing. This is a <cite>"all or nothing"</cite> method: either the fractional
     * part is completely removed, or either it is left unchanged.</p>
     *
     * <div class="section">Use case</div>
     * This method is useful after a {@linkplain StringBuilder#append(double) double value has
     * been appended to the buffer}, in order to make it appears like an integer when possible.
     *
     * @param buffer The buffer to trim if possible.
     * @throws NullArgumentException If the given {@code buffer} is null.
     *
     * @see CharSequences#trimFractionalPart(CharSequence)
     */
    @SuppressWarnings("fallthrough")
    public static void trimFractionalPart(final StringBuilder buffer) {
        ArgumentChecks.ensureNonNull ("buffer", buffer);
        for (int i=buffer.length(); i > 0;) {
            final int c = buffer.codePointBefore(i);
            i -= charCount(c);
            switch (c) {
                case '0': continue;
                case '.': buffer.setLength(i); // Fall through
                default : return;
            }
        }
    }

    /**
     * Replaces some Unicode characters by ASCII characters on a "best effort basis".
     * For example the {@code 'é'} character is replaced by {@code 'e'} (without accent).
     *
     * <p>The current implementation replaces the characters in the range {@code 00C0}
     * to {@code 00FF} (inclusive) and some space and punctuation characters.</p>
     *
     * @param  buffer The text to scan for Unicode characters to replace by ASCII characters.
     * @throws NullArgumentException If the given {@code buffer} is null.
     *
     * @see CharSequences#toASCII(CharSequence)
     */
    public static void toASCII(final StringBuilder buffer) {
        ArgumentChecks.ensureNonNull("buffer", buffer);
        toASCII(buffer, buffer);
    }

    /**
     * Implementation of the public {@code toASCII} methods.
     */
    static CharSequence toASCII(CharSequence text, StringBuilder buffer) {
        if (text != null) {
            /*
             * Scan the buffer in reverse order because we may suppress some characters.
             */
            int i = text.length();
            while (i > 0) {
                final int c = codePointBefore(text, i);
                final int n = charCount(c);
                final int r = c - 0xC0;
                i -= n; // After this line, 'i' is the index of character 'c'.
                if (r >= 0) {
                    final char cr; // The character replacement.
                    if (r < ASCII.length()) {
                        cr = ASCII.charAt(r);
                    } else {
                        switch (getType(c)) {
                            case FORMAT:
                            case CONTROL: buffer.delete(i, i + n); continue;  // Character.isIdentifierIgnorable
                            case PARAGRAPH_SEPARATOR:       // Fall through
                            case LINE_SEPARATOR:            cr = '\n'; break;
                            case SPACE_SEPARATOR:           cr = ' '; break;
                            case INITIAL_QUOTE_PUNCTUATION: cr = (c == '‘') ? '\'' : '"'; break;
                            case FINAL_QUOTE_PUNCTUATION:   cr = (c == '’') ? '\'' : '"'; break;
                            case OTHER_PUNCTUATION: {
                                switch (c) {
                                    case '′': cr = '\''; break;
                                    case '″': cr = '"';  break;
                                    default:  continue;
                                }
                                break;
                            }
                            default: continue;
                        }
                    }
                    if (buffer == null) {
                        buffer = new StringBuilder(text.length()).append(text);
                        text = buffer;
                    }
                    if (n == 2) {
                        buffer.deleteCharAt(i + 1); // Remove the low surrogate of a surrogate pair.
                    }
                    // Nothing special to do about codepoint here, since 'c' is in
                    // the basic plane (verified by the r < ASCII.length() check).
                    buffer.setCharAt(i, cr);
                }
            }
        }
        return text;
    }
}
