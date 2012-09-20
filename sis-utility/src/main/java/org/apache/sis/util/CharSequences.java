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

import org.apache.sis.resources.Errors;

import static java.lang.Character.*;
import static java.util.Arrays.fill;
import static java.util.Arrays.copyOf;
import static org.apache.sis.util.Arrays.resize;


/**
 * Utility methods working on {@link CharSequence} or {@link String} instances. Some methods
 * defined in this class duplicate the functionalities already provided in the {@code String}
 * class, but works on a generic {@code CharSequence} instance instead than {@code String}.
 * Other methods perform their work directly on a provided {@link StringBuilder} instance.
 *
 * {@section Unicode support}
 * Every methods defined in this class work on <cite>code points</cite> instead than characters
 * when appropriate. Consequently those methods should behave correctly with characters outside
 * the <cite>Basic Multilingual Plane</cite> (BMP).
 *
 * {@section Handling of null values}
 * Some methods accept a {@code null} argument, in particular the methods converting the
 * given {@code String} to another {@code String} which may be the same. For example the
 * {@link #camelCaseToAcronym(String)} method returns {@code null} if the string to convert is
 * {@code null}. Some other methods like {@link #count(String, char)} handles {@code null}
 * argument as synonymous to an empty string. The methods that do not accept a {@code null}
 * argument are explicitly documented as throwing a {@link NullPointerException}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 *
 * @see java.util.Arrays#toString(Object[])
 */
public final class CharSequences extends Static {
    /**
     * An array of zero-length. This constant play a role equivalents to
     * {@link java.util.Collections#EMPTY_LIST}.
     */
    public static final String[] EMPTY = new String[0];

    /**
     * An array of strings containing only white spaces. String lengths are equal to their
     * index in the {@code spaces} array. For example, {@code spaces[4]} contains a string
     * of length 4. Strings are constructed only when first needed.
     */
    private static final String[] SPACES = new String[21];
    static {
        final int last = SPACES.length - 1;
        final char[] spaces = new char[last];
        fill(spaces, ' ');
        SPACES[last] = new String(spaces).intern();
    }

    /**
     * Letters in the range 00C0 (192) to 00FF (255) inclusive with their accent removed,
     * when possible.
     */
    private static final String ASCII = "AAAAAAÆCEEEEIIIIDNOOOOO*OUUUUYÞsaaaaaaæceeeeiiiionooooo/ouuuuyþy";
    // Original letters (with accent) = "ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ";

    /**
     * Do not allow instantiation of this class.
     */
    private CharSequences() {
    }

    /**
     * Returns the code point after the given index. This method completes
     * {@link String#codePointBefore(int)} but is rarely used because slightly inefficient
     * (in most cases, the code point at {@code index} and its the {@code charCount(int)}
     * value are already known, so the method calls performed here would be unnecessary).
     */
    private static int codePointAfter(final CharSequence text, final int index) {
        return codePointAt(text, index + charCount(codePointAt(text, index)));
    }

    /**
     * Returns a string of the specified length filled with white spaces.
     * This method tries to return a pre-allocated string if possible.
     *
     * @param  length The string length. Negative values are clamped to 0.
     * @return A string of length {@code length} filled with white spaces.
     */
    public static String spaces(int length) {
        /*
         * No need to synchronize.  In the unlikely event of two threads calling this method
         * at the same time and the two calls creating a new string, the String.intern() call
         * will take care of canonicalizing the strings.
         */
        if (length < 0) {
            length = 0;
        }
        String s;
        if (length < SPACES.length) {
            s = SPACES[length];
            if (s == null) {
                s = SPACES[SPACES.length - 1].substring(0, length).intern();
                SPACES[length] = s;
            }
        } else {
            final char[] spaces = new char[length];
            fill(spaces, ' ');
            s = new String(spaces);
        }
        return s;
    }

    /**
     * Returns the {@linkplain CharSequence#length() length} of the given characters sequence,
     * or 0 if {@code null}.
     *
     * @param  text The character sequence from which to get the length, or {@code null}.
     * @return The length of the character sequence, or 0 if the argument is {@code null}.
     */
    public static int length(final CharSequence text) {
        return (text != null) ? text.length() : 0;
    }

    /**
     * Returns the number of occurrences of the {@code toSearch} string in the given {@code text}.
     * The search is case-sensitive.
     *
     * @param  text String to search in, or {@code null}.
     * @param  toSearch The string to search in the given {@code text}.
     *         Must contain at least one character.
     * @return The number of occurrence of {@code toSearch} in {@code text},
     *         or 0 if {@code text} was null or empty.
     * @throws IllegalArgumentException If the {@code toSearch} array is null or empty.
     */
    public static int count(final String text, final String toSearch) {
        final int length;
        if (toSearch == null || (length = toSearch.length()) == 0) {
            throw new IllegalArgumentException(Errors.format(
                    Errors.Keys.EmptyArgument_1, "toSearch"));
        }
        if (length == 1) {
            return count(text, toSearch.charAt(0));
        }
        int n = 0;
        if (text != null) {
            for (int i=text.indexOf(toSearch); i>=0; i=text.indexOf(toSearch, i+length)) {
                n++;
            }
        }
        return n;
    }

    /**
     * Counts the number of occurrence of the given character in the given string. This
     * method performs the same work than {@link #count(CharSequence, char)}, but is faster.
     *
     * @param  text The text in which to count the number of occurrence.
     * @param  c The character to count, or 0 if {@code text} was null.
     * @return The number of occurrences of the given character.
     */
    public static int count(final String text, final char c) {
        int n = 0;
        if (text != null) {
            for (int i=text.indexOf(c); ++i!=0; i=text.indexOf(c, i)) {
                n++;
            }
        }
        return n;
    }

    /**
     * Counts the number of occurrence of the given character in the given character sequence.
     * This method performs the same work than {@link #count(String, char)}, but on a more
     * generic interface.
     *
     * @param  text The text in which to count the number of occurrence.
     * @param  c The character to count, or 0 if {@code text} was null.
     * @return The number of occurrences of the given character.
     */
    public static int count(final CharSequence text, final char c) {
        if (text instanceof String) {
            return count((String) text, c);
        }
        int n = 0;
        if (text != null) {
            // No need to use the code point API here, since we are looking for exact matches.
            for (int i=text.length(); --i>=0;) {
                if (text.charAt(i) == c) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Splits a string around the given character. The array returned by this method contains each
     * substring of the given string that is terminated by the given character or is terminated by
     * the end of the string. The substrings in the array are in the order in which they occur in
     * the given string. If the character is not found in the input, then the resulting array has
     * just one element, namely the given string.
     * <p>
     * This method is similar to the standard {@link String#split(String)} method except for the
     * following:
     * <p>
     * <ul>
     *   <li>It accepts a {@code null} input string, in which case an empty array is returned.</li>
     *   <li>The separator is a simple character instead than a regular expression.</li>
     *   <li>The leading and trailing spaces of each substring are {@linkplain String#trim trimmed}.</li>
     * </ul>
     *
     * @param  toSplit   The string to split, or {@code null}.
     * @param  separator The delimiting character (typically the coma).
     * @return The array of strings computed by splitting the given string around the given
     *         character, or an empty array if {@code toSplit} was null.
     *
     * @see String#split(String)
     */
    public static String[] split(final String toSplit, final char separator) {
        final boolean excludeEmpty = (separator <= ' '); // Use the same criterion than String.trim().
        String[] strings = new String[4];
        int count = 0;
        if (toSplit != null) {
            int last = 0;
            for (int i=toSplit.indexOf(separator); i>=0; i=toSplit.indexOf(separator, i)) {
                // Note: parseDoubles(...) needs the call to trim().
                final String item = toSplit.substring(last, i).trim();
                if (!excludeEmpty || !item.isEmpty()) {
                    if (count == strings.length) {
                        strings = copyOf(strings, count << 1);
                    }
                    strings[count++] = item;
                }
                last = ++i;
            }
            final String item = toSplit.substring(last).trim();
            if (!excludeEmpty || !item.isEmpty()) {
                if (count == strings.length) {
                    strings = copyOf(strings, count + 1);
                }
                strings[count++] = item;
            }
        }
        return resize(strings, count);
    }

    /**
     * {@linkplain #split(String, char) Splits} the given string around the given character,
     * then {@linkplain Double#parseDouble(String) parses} each item as a {@code double}.
     *
     * @param  values The strings containing the values to parse, or {@code null}.
     * @param  separator The delimiting character (typically the coma).
     * @return The array of numbers parsed from the given string,
     *         or an empty array if {@code values} was null.
     * @throws NumberFormatException If at least one number can not be parsed.
     *
     * @since 3.19
     */
    public static double[] parseDoubles(final String values, final char separator) throws NumberFormatException {
        final String[] tokens = split(values, separator);
        final double[] parsed = new double[tokens.length];
        for (int i=0; i<tokens.length; i++) {
            final String token = tokens[i];
            parsed[i] = token.isEmpty() ? Double.NaN : Double.parseDouble(token);
        }
        return parsed;
    }

    /**
     * {@linkplain #split(String, char) Splits} the given string around the given character,
     * then {@linkplain Float#parseFloat(String) parses} each item as a {@code float}.
     *
     * @param  values The strings containing the values to parse, or {@code null}.
     * @param  separator The delimiting character (typically the coma).
     * @return The array of numbers parsed from the given string,
     *         or an empty array if {@code values} was null.
     * @throws NumberFormatException If at least one number can not be parsed.
     *
     * @since 3.19
     */
    public static float[] parseFloats(final String values, final char separator) throws NumberFormatException {
        final String[] tokens = split(values, separator);
        final float[] parsed = new float[tokens.length];
        for (int i=0; i<tokens.length; i++) {
            final String token = tokens[i];
            parsed[i] = token.isEmpty() ? Float.NaN : Float.parseFloat(token);
        }
        return parsed;
    }

    /**
     * {@linkplain #split(String, char) Splits} the given string around the given character,
     * then {@linkplain Long#parseLong(String) parses} each item as a {@code long}.
     *
     * @param  values The strings containing the values to parse, or {@code null}.
     * @param  separator The delimiting character (typically the coma).
     * @param  radix the radix to be used for parsing. This is usually 10.
     * @return The array of numbers parsed from the given string,
     *         or an empty array if {@code values} was null.
     * @throws NumberFormatException If at least one number can not be parsed.
     *
     * @since 3.19
     */
    public static long[] parseLongs(final String values, final char separator, final int radix) throws NumberFormatException {
        final String[] tokens = split(values, separator);
        final long[] parsed = new long[tokens.length];
        for (int i=0; i<tokens.length; i++) {
            parsed[i] = Long.parseLong(tokens[i], radix);
        }
        return parsed;
    }

    /**
     * {@linkplain #split(String, char) Splits} the given string around the given character,
     * then {@linkplain Integer#parseInt(String) parses} each item as an {@code int}.
     *
     * @param  values The strings containing the values to parse, or {@code null}.
     * @param  separator The delimiting character (typically the coma).
     * @param  radix the radix to be used for parsing. This is usually 10.
     * @return The array of numbers parsed from the given string,
     *         or an empty array if {@code values} was null.
     * @throws NumberFormatException If at least one number can not be parsed.
     *
     * @since 3.19
     */
    public static int[] parseInts(final String values, final char separator, final int radix) throws NumberFormatException {
        final String[] tokens = split(values, separator);
        final int[] parsed = new int[tokens.length];
        for (int i=0; i<tokens.length; i++) {
            parsed[i] = Integer.parseInt(tokens[i], radix);
        }
        return parsed;
    }

    /**
     * {@linkplain #split(String, char) Splits} the given string around the given character,
     * then {@linkplain Short#parseShort(String) parses} each item as a {@code short}.
     *
     * @param  values The strings containing the values to parse, or {@code null}.
     * @param  separator The delimiting character (typically the coma).
     * @param  radix the radix to be used for parsing. This is usually 10.
     * @return The array of numbers parsed from the given string,
     *         or an empty array if {@code values} was null.
     * @throws NumberFormatException If at least one number can not be parsed.
     *
     * @since 3.19
     */
    public static short[] parseShorts(final String values, final char separator, final int radix) throws NumberFormatException {
        final String[] tokens = split(values, separator);
        final short[] parsed = new short[tokens.length];
        for (int i=0; i<tokens.length; i++) {
            parsed[i] = Short.parseShort(tokens[i], radix);
        }
        return parsed;
    }

    /**
     * {@linkplain #split(String, char) Splits} the given string around the given character,
     * then {@linkplain Byte#parseByte(String) parses} each item as a {@code byte}.
     *
     * @param  values The strings containing the values to parse, or {@code null}.
     * @param  separator The delimiting character (typically the coma).
     * @param  radix the radix to be used for parsing. This is usually 10.
     * @return The array of numbers parsed from the given string,
     *         or an empty array if {@code values} was null.
     * @throws NumberFormatException If at least one number can not be parsed.
     *
     * @since 3.19
     */
    public static byte[] parseBytes(final String values, final char separator, final int radix) throws NumberFormatException {
        final String[] tokens = split(values, separator);
        final byte[] parsed = new byte[tokens.length];
        for (int i=0; i<tokens.length; i++) {
            parsed[i] = Byte.parseByte(tokens[i], radix);
        }
        return parsed;
    }

    /**
     * Formats the given elements as a (typically) comma-separated list. This method is similar to
     * {@link java.util.AbstractCollection#toString()} or {@link java.util.Arrays#toString(Object[])}
     * except for the following:
     * <p>
     * <ul>
     *   <li>There is no leading {@code '['} and trailing {@code ']'} characters.</li>
     *   <li>Null elements are ignored instead than formatted as {@code "null"}.</li>
     *   <li>If the {@code collection} argument is null or contains only null elements,
     *       then this method returns {@code null}.</li>
     *   <li>In the common case where the collection contains a single {@link String} element,
     *       that string is returned directly (no object duplication).</li>
     * </ul>
     * <p>
     * This method is the converse of {@link #getLinesFromMultilines(String)}
     * when the separator is {@link System#lineSeparator()}.
     *
     * @param  collection The elements to format in a (typically) comma-separated list, or {@code null}.
     * @param  separator  The element separator, which is usually {@code ", "}.
     * @return The (typically) comma-separated list, or {@code null} if the given {@code collection}
     *         was null or contains only null elements.
     *
     * @since 3.20
     */
    public static String formatList(final Iterable<?> collection, final String separator) {
        ArgumentChecks.ensureNonNull("separator", separator);
        String list = null;
        if (collection != null) {
            StringBuilder buffer = null;
            for (final Object element : collection) {
                if (element != null) {
                    if (list == null) {
                        list = element.toString();
                    } else {
                        if (buffer == null) {
                            buffer = new StringBuilder(list);
                        }
                        buffer.append(separator).append(element);
                    }
                }
            }
            if (buffer != null) {
                list = buffer.toString();
            }
        }
        return list;
    }

    /**
     * Replaces every occurrences of the given string in the given buffer.
     * This method invokes {@link StringBuilder#replace(int, int, String)}
     * for each occurrence of {@code search} found in the buffer.
     *
     * @param buffer The string in which to perform the replacements.
     * @param search The string to replace.
     * @param replacement The replacement for the target string.
     * @throws NullPointerException if any of the arguments is null.
     *
     * @see String#replace(char, char)
     * @see String#replace(CharSequence, CharSequence)
     * @see StringBuilder#replace(int, int, String)
     */
    public static void replace(final StringBuilder buffer, final String search, final String replacement) {
        if (!search.equals(replacement)) {
            final int length = search.length();
            int i = buffer.length();
            while ((i = buffer.lastIndexOf(search, i)) >= 0) {
                buffer.replace(i, i+length, replacement);
                i -= length;
            }
        }
    }

    /**
     * Replaces the characters in a substring of the buffer with characters in the specified array.
     * The substring to be replaced begins at the specified {@code start} and extends to the
     * character at index {@code end - 1}.
     *
     * @param buffer The buffer in which to perform the replacement.
     * @param start  The beginning index in the {@code buffer}, inclusive.
     * @param end    The ending index in the {@code buffer}, exclusive.
     * @param chars  The array that will replace previous contents.
     * @throws NullPointerException if the {@code buffer} or {@code chars} argument is null.
     *
     * @see StringBuilder#replace(int, int, String)
     *
     * @since 3.20
     */
    public static void replace(final StringBuilder buffer, int start, final int end, final char[] chars) {
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
     * @param buffer The string in which to perform the removals.
     * @param search The string to remove.
     * @throws NullPointerException if any of the arguments is null.
     *
     * @see StringBuilder#delete(int, int)
     */
    public static void remove(final StringBuilder buffer, final String search) {
        final int length = search.length();
        for (int i=buffer.lastIndexOf(search); i>=0; i=buffer.lastIndexOf(search, i)) {
            buffer.delete(i, i + length);
        }
    }

    /**
     * Returns a string with leading and trailing white spaces omitted. White spaces are identified
     * by the {@link Character#isWhitespace(int)} method.
     * <p>
     * This method is similar in purpose to {@link String#trim()}, except that the later considers
     * every ASCII control codes below 32 to be a whitespace. This have the effect of removing
     * {@linkplain org.apache.sis.io.X364 X3.64} escape sequences as well. Users should invoke
     * this {@code CharSequences.trim} method instead if they need to preserve X3.64 escape sequences.
     *
     * @param text The string from which to remove leading and trailing white spaces, or {@code null}.
     * @return A string with leading and trailing white spaces removed, or {@code null} is the given
     *         string was null.
     *
     * @see String#trim()
     */
    public static String trim(String text) {
        if (text != null) {
            int upper = text.length();
            while (upper != 0) {
                final int c = text.codePointBefore(upper);
                if (!isWhitespace(c)) break;
                upper -= charCount(c);
            }
            int lower = 0;
            while (lower < upper) {
                final int c = text.codePointAt(lower);
                if (!isWhitespace(c)) break;
                lower += charCount(c);
            }
            text = text.substring(lower, upper);
        }
        return text;
    }

    /**
     * Trims the fractional part of the given formatted number, provided that it doesn't change
     * the value. This method assumes that the number is formatted in the US locale, typically
     * by the {@link Double#toString(double)} method.
     * <p>
     * More specifically if the given string ends with a {@code '.'} character followed by a
     * sequence of {@code '0'} characters, then those characters are omitted. Otherwise this
     * method returns the string unchanged. This is a "<cite>all or nothing</cite>" method:
     * either the fractional part is completely removed, or either it is left unchanged.
     *
     * {@section Examples}
     * This method returns {@code "4"} if the given value is {@code "4."}, {@code "4.0"} or
     * {@code "4.00"}, but returns {@code "4.10"} unchanged (including the trailing {@code '0'}
     * character) if the input is {@code "4.10"}.
     *
     * {@section Use case}
     * This method is useful before to {@linkplain Integer#parseInt(String) parse a number}
     * if that number should preferably be parsed as an integer before attempting to parse
     * it as a floating point number.
     *
     * @param  value The value to trim if possible, or {@code null}.
     * @return The value without the trailing {@code ".0"} part (if any),
     *         or {@code null} if the given string was null.
     */
    public static String trimFractionalPart(final String value) {
        if (value != null) {
            for (int i=value.length(); i>0;) {
                final int c = value.codePointBefore(i);
                i -= charCount(c);
                switch (c) {
                    case '0': continue;
                    case '.': return value.substring(0, i);
                    default : return value;
                }
            }
        }
        return value;
    }

    /**
     * Trims the fractional part of the given formatted number, provided that it doesn't change
     * the value. This method performs the same work than {@link #trimFractionalPart(String)}
     * except that it modifies the given buffer in-place.
     *
     * {@section Use case}
     * This method is useful after a {@linkplain StringBuilder#append(double) double value has
     * been appended to the buffer}, in order to make it appears like an integer when possible.
     *
     * @param buffer The buffer to trim if possible.
     * @throws NullPointerException if the argument is null.
     */
    @SuppressWarnings("fallthrough")
    public static void trimFractionalPart(final StringBuilder buffer) {
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
     * <p>
     * The current implementation replaces only the characters in the range {@code 00C0}
     * to {@code 00FF}, inclusive. Other characters are left unchanged.
     * <p>
     * Note that if the given character sequence is an instance of {@link StringBuilder},
     * then the replacement will be performed in-place.
     *
     * @param  text The text to scan for Unicode characters to replace by ASCII characters,
     *         or {@code null}.
     * @return The given text with substitution applied, or {@code text} if no replacement
     *         has been applied.
     *
     * @since 3.18
     */
    public static CharSequence toASCII(CharSequence text) {
        if (text != null) {
            StringBuilder buffer = null;
            final int length = text.length();
            for (int i=0; i<length;) {
                final int c = codePointAt(text, i);
                final int r = c - 0xC0;
                if (r >= 0 && r<ASCII.length()) {
                    final char ac = ASCII.charAt(r);
                    if (buffer == null) {
                        if (text instanceof StringBuilder) {
                            buffer = (StringBuilder) text;
                        } else {
                            buffer = new StringBuilder(text);
                            text = buffer;
                        }
                    }
                    // Nothing special do to about codepoint here, since 'c' is
                    // in the basic plane (verified by the r<ASCII.length() check).
                    buffer.setCharAt(i, ac);
                }
                i += charCount(c);
            }
        }
        return text;
    }

    /**
     * Given a string in camel cases (typically a Java identifier), returns a string formatted
     * like an English sentence. This heuristic method performs the following steps:
     *
     * <ol>
     *   <li><p>Invoke {@link #camelCaseToWords(CharSequence, boolean)}, which separate the words
     *     on the basis of character case. For example {@code "transferFunctionType"} become
     *     "<cite>transfer function type</cite>". This works fine for ISO 19115 identifiers.</p></li>
     *
     *   <li><p>Next replace all occurrence of {@code '_'} by spaces in order to take in account
     *     an other common naming convention, which uses {@code '_'} as a word separator. This
     *     convention is used by NetCDF attributes like {@code "project_name"}.</p></li>
     *
     *   <li><p>Finally ensure that the first character is upper-case.</p></li>
     * </ol>
     *
     * {@section Exception to the above rules}
     * If the given identifier contains only upper-case letters, digits and the {@code '_'}
     * character, then the identifier is returned "as is" except for the {@code '_'} characters
     * which are replaced by {@code '-'}. This work well for identifiers like {@code "UTF-8"} or
     * {@code "ISO-LATIN-1"} for example.
     * <p>
     * Note that those heuristic rules may be modified in future SIS versions,
     * depending on the practical experience gained.
     *
     * @param  identifier An identifier with no space, words begin with an upper-case character,
     *         or {@code null}.
     * @return The identifier with spaces inserted after what looks like words, or {@code null}
     *         if the given argument was null.
     *
     * @since 3.18 (derived from 3.09)
     */
    public static String camelCaseToSentence(final CharSequence identifier) {
        if (identifier == null) {
            return null;
        }
        if (isCode(identifier)) {
            return identifier.toString().replace('_', '-');
        }
        final StringBuilder buffer = camelCaseToWords(identifier, true);
        final int length = buffer.length();
        for (int i=0; i<length; i++) {
            // No need to use the code point API here, since we are looking for exact matches.
            if (buffer.charAt(i) == '_') {
                buffer.setCharAt(i, ' ');
            }
        }
        if (length != 0) {
            final int c = buffer.codePointAt(0);
            final int up = toUpperCase(c);
            if (c != up) {
                replace(buffer, 0, charCount(c), Character.toChars(up));
            }
        }
        return buffer.toString().trim();
    }

    /**
     * Given a string in camel cases, returns a string with the same words separated by spaces.
     * A word begins with a upper-case character following a lower-case character. For example
     * if the given string is {@code "PixelInterleavedSampleModel"}, then this method returns
     * "<cite>Pixel Interleaved Sample Model</cite>" or "<cite>Pixel interleaved sample model</cite>"
     * depending on the value of the {@code toLowerCase} argument.
     * <p>
     * If {@code toLowerCase} is {@code false}, then this method inserts spaces but does not change
     * the case of characters. If {@code toLowerCase} is {@code true}, then this method changes
     * {@linkplain Character#toLowerCase(int) to lower case} the first character after each spaces
     * inserted by this method (note that this intentionally exclude the very first character in
     * the given string), except if the second character {@linkplain Character#isUpperCase(int)
     * is upper case}, in which case the words is assumed an acronym.
     * <p>
     * The given string is usually a programmatic identifier like a class name or a method name.
     *
     * @param  identifier An identifier with no space, words begin with an upper-case character.
     * @param  toLowerCase {@code true} for changing the first character of words to lower case,
     *         except for the first word and acronyms.
     * @return The identifier with spaces inserted after what looks like words, returned
     *         as a {@link StringBuilder} in order to allow modifications by the caller.
     * @throws NullPointerException if the {@code identifier} argument is null.
     */
    public static StringBuilder camelCaseToWords(final CharSequence identifier, final boolean toLowerCase) {
        final int length = identifier.length();
        final StringBuilder buffer = new StringBuilder(length + 8);
        final int lastIndex = (length != 0) ? length - charCount(codePointBefore(identifier, length)) : 0;
        int last = 0;
        for (int i=1; i<=length;) {
            final int cp;
            final boolean doAppend;
            if (i == length) {
                cp = 0;
                doAppend = true;
            } else {
                cp = codePointAt(identifier, i);
                doAppend = Character.isUpperCase(cp) && isLowerCase(codePointBefore(identifier, i));
            }
            if (doAppend) {
                final int pos = buffer.length();
                buffer.append(identifier, last, i).append(' ');
                if (toLowerCase && pos!=0 && last<lastIndex && isLowerCase(codePointAfter(identifier, last))) {
                    final int c = buffer.codePointAt(pos);
                    final int low = toLowerCase(c);
                    if (c != low) {
                        replace(buffer, pos, pos + charCount(c), Character.toChars(low));
                    }
                }
                last = i;
            }
            i += charCount(cp);
        }
        /*
         * Removes the trailing space, if any.
         */
        final int lg = buffer.length();
        if (lg != 0) {
            final int cp = buffer.codePointBefore(lg);
            if (isSpaceChar(cp)) {
                buffer.setLength(lg - charCount(cp));
            }
        }
        return buffer;
    }

    /**
     * Creates an acronym from the given text. If every characters in the given text are upper
     * case, then the text is returned unchanged on the assumption that it is already an acronym.
     * Otherwise this method returns a string containing the first character of each word, where
     * the words are separated by the camel case convention, the {@code '_'} character, or any
     * character which is not a {@linkplain Character#isJavaIdentifierPart(int) java identifier
     * part} (including spaces).
     * <p>
     * <b>Examples:</b> given {@code "northEast"}, this method returns {@code "NE"}.
     * Given {@code "Open Geospatial Consortium"}, this method returns {@code "OGC"}.
     *
     * @param  text The text for which to create an acronym, or {@code null}.
     * @return The acronym, or {@code null} if the given text was null.
     */
    public static String camelCaseToAcronym(String text) {
        if (text != null && !isUpperCase(text = text.trim())) {
            final int length = text.length();
            final StringBuilder buffer = new StringBuilder(8); // Acronyms are usually short.
            boolean wantChar = true;
            for (int i=0; i<length;) {
                final int c = text.codePointAt(i);
                if (wantChar) {
                    if (isJavaIdentifierStart(c)) {
                        buffer.appendCodePoint(c);
                        wantChar = false;
                    }
                } else if (!isJavaIdentifierPart(c) || c == '_') {
                    wantChar = true;
                } else if (Character.isUpperCase(c)) {
                    // Test for mixed-case (e.g. "northEast").
                    // Note that the buffer is guaranteed to contain at least 1 character.
                    if (isLowerCase(buffer.codePointBefore(buffer.length()))) {
                        buffer.appendCodePoint(c);
                    }
                }
                i += charCount(c);
            }
            final int acrlg = buffer.length();
            if (acrlg != 0) {
                /*
                 * If every characters except the first one are upper-case, ensure that the
                 * first one is upper-case as well. This is for handling the identifiers which
                 * are compliant to Java-Beans convention (e.g. "northEast").
                 */
                if (isUpperCase(buffer, 1, acrlg)) {
                    final int c = buffer.codePointAt(0);
                    final int up = toUpperCase(c);
                    if (c != up) {
                        replace(buffer, 0, charCount(c), Character.toChars(up));
                    }
                }
                final String acronym = buffer.toString();
                if (!text.equals(acronym)) {
                    text = acronym;
                }
            }
        }
        return text;
    }

    /**
     * Returns {@code true} if the first string is likely to be an acronym of the second string.
     * An acronym is a sequence of {@linkplain Character#isLetterOrDigit(int) letters or digits}
     * built from at least one character of each word in the {@code words} string. More than
     * one character from the same word may appear in the acronym, but they must always
     * be the first consecutive characters. The comparison is case-insensitive.
     * <p>
     * <b>Example:</b> given the string {@code "Open Geospatial Consortium"}, the following
     * strings are recognized as acronyms: {@code "OGC"}, {@code "ogc"}, {@code "O.G.C."},
     * {@code "OpGeoCon"}.
     *
     * @param  acronym A possible acronym of the sequence of words.
     * @param  words The sequence of words.
     * @return {@code true} if the first string is an acronym of the second one.
     * @throws NullPointerException if any of the arguments is null.
     */
    public static boolean isAcronymForWords(final CharSequence acronym, final CharSequence words) {
        final int lgc = words.length();
        final int lga = acronym.length();
        int ic=0, ia=0;
        int ca, cc;
        do {
            if (ia >= lga) return false;
            ca = codePointAt(acronym, ia);
            ia += charCount(ca);
        } while (!isLetterOrDigit(ca));
        do {
            if (ic >= lgc) return false;
            cc = codePointAt(words, ic);
            ic += charCount(cc);
        }
        while (!isLetterOrDigit(cc));
        if (toUpperCase(ca) != toUpperCase(cc)) {
            // The first letter must match.
            return false;
        }
cmp:    while (ia < lga) {
            if (ic >= lgc) {
                // There is more letters in the acronym than in the complete name.
                return false;
            }
            ca = codePointAt(acronym, ia); ia += charCount(ca);
            cc = codePointAt(words,   ic); ic += charCount(cc);
            if (isLetterOrDigit(ca)) {
                if (toUpperCase(ca) == toUpperCase(cc)) {
                    // Acronym letter matches the letter from the complete name.
                    // Continue the comparison with next letter of both strings.
                    continue;
                }
                // Will search for the next word after the 'else' block.
            } else do {
                if (ia >= lga) break cmp;
                ca = codePointAt(acronym, ia);
                ia += charCount(ca);
            } while (!isLetterOrDigit(ca));
            /*
             * At this point, 'ca' is the next acronym letter to compare and we
             * need to search for the next word in the complete name. We first
             * skip remaining letters, then we skip non-letter characters.
             */
            boolean skipLetters = true;
            do while (isLetterOrDigit(cc) == skipLetters) {
                if (ic >= lgc) {
                    return false;
                }
                cc = codePointAt(words, ic);
                ic += charCount(cc);
            } while ((skipLetters = !skipLetters) == false);
            // Now that we are aligned on a new word, the first letter must match.
            if (toUpperCase(ca) != toUpperCase(cc)) {
                return false;
            }
        }
        /*
         * Now that we have processed all acronym letters, the complete name can not have
         * any additional word. We can only finish the current word and skip trailing non-
         * letter characters.
         */
        boolean skipLetters = true;
        do {
            do {
                if (ic >= lgc) return true;
                cc = codePointAt(words, ic);
                ic += charCount(cc);
            } while (isLetterOrDigit(cc) == skipLetters);
        } while ((skipLetters = !skipLetters) == false);
        return false;
    }

    /**
     * Returns {@code true} if the given string contains only upper case letters or digits.
     * A few punctuation characters like {@code '_'} and {@code '.'} are also accepted.
     * <p>
     * This method is used for identifying character strings that are likely to be code
     * like {@code "UTF-8"} or {@code "ISO-LATIN-1"}.
     *
     * @see #isJavaIdentifier(CharSequence)
     *
     * @since 3.18 (derived from 3.17)
     */
    private static boolean isCode(final CharSequence identifier) {
        for (int i=identifier.length(); --i>=0;) {
            final char c = identifier.charAt(i);
            // No need to use the code point API here, since the conditions
            // below are requiring the characters to be in the basic plane.
            if (!((c >= 'A' && c <= 'Z') || (c >= '-' && c <= ':') || c == '_')) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if the given identifier is a legal Java identifier.
     * This method returns {@code true} if the identifier length is greater than zero,
     * the first character is a {@linkplain Character#isJavaIdentifierStart(int) Java
     * identifier start} and all remaining characters (if any) are
     * {@linkplain Character#isJavaIdentifierPart(int) Java identifier parts}.
     *
     * @param identifier The character sequence to test.
     * @return {@code true} if the given character sequence is a legal Java identifier.
     * @throws NullPointerException if the argument is null.
     */
    public static boolean isJavaIdentifier(final CharSequence identifier) {
        final int length = identifier.length();
        if (length == 0) {
            return false;
        }
        int c = codePointAt(identifier, 0);
        if (!isJavaIdentifierStart(c)) {
            return false;
        }
        for (int i=0; (i += charCount(c)) < length;) {
            c = codePointAt(identifier, i);
            if (!isJavaIdentifierPart(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if every characters in the given character sequence are
     * {@linkplain Character#isUpperCase(int) upper-case}.
     *
     * @param  text The character sequence to test.
     * @return {@code true} if every character are upper-case.
     * @throws NullPointerException if the argument is null.
     *
     * @see String#toUpperCase()
     */
    public static boolean isUpperCase(final CharSequence text) {
        return isUpperCase(text, 0, text.length());
    }

    /**
     * Same as {@link #isUpperCase(CharSequence)}, but on a sub-sequence.
     */
    private static boolean isUpperCase(final CharSequence text, int lower, final int upper) {
        while (lower < upper) {
            final int c = codePointAt(text, lower);
            if (!Character.isUpperCase(c)) {
                return false;
            }
            lower += charCount(c);
        }
        return true;
    }

    /**
     * Returns {@code true} if the given code points are equal, ignoring case.
     * This method implements the same comparison algorithm than String#equalsIgnoreCase(String).
     * <p>
     * This method does not verify if {@code c1 == c2}. This check should have been done
     * by the caller, since the caller code is a more optimal place for this check.
     */
    private static boolean equalsIgnoreCase(int c1, int c2) {
        c1 = toUpperCase(c1);
        c2 = toUpperCase(c2);
        if (c1 == c2) {
            return true;
        }
        // Need this check for Georgian alphabet.
        return toLowerCase(c1) == toLowerCase(c2);
    }

    /**
     * Returns {@code true} if the two given strings are equal, ignoring case.
     * This method is similar to {@link String#equalsIgnoreCase(String)}, except
     * it works on arbitrary character sequences and compares <cite>code points</cite>
     * instead than characters.
     *
     * @param  s1 The first string to compare.
     * @param  s2 The second string to compare.
     * @return {@code true} if the two given strings are equal, ignoring case.
     * @throws NullPointerException if any of the arguments is null.
     *
     * @see String#equalsIgnoreCase(String)
     */
    public static boolean equalsIgnoreCase(final CharSequence s1, final CharSequence s2) {
        final int lg1 = s1.length();
        final int lg2 = s2.length();
        int i1 = 0, i2 = 0;
        while (i1<lg1 && i2<lg2) {
            final int c1 = codePointAt(s1, i1);
            final int c2 = codePointAt(s2, i2);
            if (c1 != c2 && !equalsIgnoreCase(c1, c2)) {
                return false;
            }
            i1 += charCount(c1);
            i2 += charCount(c2);
        }
        return i1 == i2;
    }

    /**
     * Returns {@code true} if the given string at the given offset contains the given part,
     * in a case-sensitive comparison. This method is equivalent to the following code:
     *
     * {@preformat java
     *     return string.regionMatches(offset, part, 0, part.length());
     * }
     *
     * Except that this method works on arbitrary {@link CharSequence} objects instead than
     * {@link String}s only.
     *
     * @param string The string for which to tests for the presence of {@code part}.
     * @param offset The offset in {@code string} where to test for the presence of {@code part}.
     * @param part   The part which may be present in {@code string}.
     * @return {@code true} if {@code string} contains {@code part} at the given {@code offset}.
     * @throws NullPointerException if any of the arguments is null.
     *
     * @see String#regionMatches(int, String, int, int)
     */
    public static boolean regionMatches(final CharSequence string, final int offset, final CharSequence part) {
        if (string instanceof String && part instanceof String) {
            return ((String) string).regionMatches(offset, (String) part, 0, part.length());
        }
        final int length = part.length();
        if (offset + length > string.length()) {
            return false;
        }
        for (int i=0; i<length; i++) {
            // No need to use the code point API here, since we are looking for exact matches.
            if (string.charAt(offset + i) != part.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the index within the given strings of the first occurrence of the specified part,
     * starting at the specified index. This method is equivalent to the following code:
     *
     * {@preformat java
     *     return string.indexOf(part, fromIndex);
     * }
     *
     * Except that this method works on arbitrary {@link CharSequence} objects instead than
     * {@link String}s only.
     *
     * @param  string    The string in which to perform the search.
     * @param  part      The substring for which to search.
     * @param  fromIndex The index from which to start the search.
     * @return The index within the string of the first occurrence of the specified part,
     *         starting at the specified index, or -1 if none.
     * @throws NullPointerException if any of the arguments is null.
     *
     * @see String#indexOf(String, int)
     * @see StringBuilder#indexOf(String, int)
     * @see StringBuffer#indexOf(String, int)
     */
    public static int indexOf(final CharSequence string, final CharSequence part, int fromIndex) {
        if (part instanceof String) {
            if (string instanceof String) {
                return ((String) string).indexOf((String) part, fromIndex);
            }
            if (string instanceof StringBuilder) {
                return ((StringBuilder) string).indexOf((String) part, fromIndex);
            }
            if (string instanceof StringBuffer) {
                return ((StringBuffer) string).indexOf((String) part, fromIndex);
            }
        }
        final int length = part.length();
        final int stopAt = string.length() - length;
search: for (; fromIndex <= stopAt; fromIndex++) {
            for (int i=0; i<length; i++) {
                // No need to use the codePointAt API here, since we are looking for exact matches.
                if (string.charAt(fromIndex + i) != part.charAt(i)) {
                    continue search;
                }
            }
            return fromIndex;
        }
        return -1;
    }

    /**
     * Returns the token starting at the given offset in the given text. For the purpose of this
     * method, a "token" is any sequence of consecutive characters of the same type, as defined
     * below.
     * <p>
     * Let define <var>c</var> as the first non-blank character located at an index equals or
     * greater than the given offset. Then the characters that are considered of the same type
     * are:
     * <p>
     * <ul>
     *   <li>If <var>c</var> is a
     *       {@linkplain Character#isJavaIdentifierStart(int) Java identifier start},
     *       then any following character that are
     *       {@linkplain Character#isJavaIdentifierPart(int) Java identifier part}.</li>
     *   <li>Otherwise any character for which {@link Character#getType(int)} returns
     *       the same value than for <var>c</var>.</li>
     * </ul>
     *
     * @param  text The text for which to get the token.
     * @param  offset Index of the fist character to consider in the given text.
     * @return A sub-sequence of {@code text} starting at the given offset, or an empty string
     *         if there is no non-blank character at or after the given offset.
     * @throws NullPointerException if the {@code text} argument is null.
     */
    public static CharSequence token(final CharSequence text, int offset) {
        final int length = text.length();
        int upper = offset;
        /*
         * Skip whitespaces. At the end of this loop,
         * 'c' will be the first non-blank character.
         */
        int c;
        do {
            if (upper >= length) return "";
            c = codePointAt(text, upper);
            offset = upper;
            upper += charCount(c);
        }
        while (isWhitespace(c));
        /*
         * Advance over all characters "of the same type".
         */
        if (isJavaIdentifierStart(c)) {
            while (upper<length && isJavaIdentifierPart(c = codePointAt(text, upper))) {
                upper += charCount(c);
            }
        } else {
            final int type = getType(codePointAt(text, offset));
            while (upper<length && getType(c = codePointAt(text, upper)) == type) {
                upper += charCount(c);
            }
        }
        return text.subSequence(offset, upper);
    }

    /**
     * Returns the longest sequence of characters which is found at the beginning of the
     * two given strings. If one of those string is {@code null}, then the other string is
     * returned.
     *
     * @param s1 The first string, or {@code null}.
     * @param s2 The second string, or {@code null}.
     * @return The common prefix of both strings, or {@code null} if both strings are null.
     */
    public static String commonPrefix(final String s1, final String s2) {
        if (s1 == null) return s2;
        if (s2 == null) return s1;
        final String shortest;
        final int lg1 = s1.length();
        final int lg2 = s2.length();
        final int length;
        if (lg1 <= lg2) {
            shortest = s1;
            length = lg1;
        } else {
            shortest = s2;
            length = lg2;
        }
        int i = 0;
        while (i < length) {
            // No need to use the codePointAt API here, since we are looking for exact matches.
            if (s1.charAt(i) != s2.charAt(i)) {
                break;
            }
            i++;
        }
        return shortest.substring(0, i);
    }

    /**
     * Returns the longest sequence of characters which is found at the end of the two given
     * strings. If one of those string is {@code null}, then the other string is returned.
     *
     * @param s1 The first string, or {@code null}.
     * @param s2 The second string, or {@code null}.
     * @return The common suffix of both strings, or {@code null} if both strings are null.
     */
    public static String commonSuffix(final String s1, final String s2) {
        if (s1 == null) return s2;
        if (s2 == null) return s1;
        final String shortest;
        final int lg1 = s1.length();
        final int lg2 = s2.length();
        final int length;
        if (lg1 <= lg2) {
            shortest = s1;
            length = lg1;
        } else {
            shortest = s2;
            length = lg2;
        }
        int i = 0;
        while (++i <= length) {
            // No need to use the codePointAt API here, since we are looking for exact matches.
            if (s1.charAt(lg1 - i) != s2.charAt(lg2 - i)) {
                break;
            }
        }
        i--;
        return shortest.substring(length - i);
    }

    /**
     * Returns {@code true} if the given character sequence starts with the given prefix.
     *
     * @param sequence    The sequence to test.
     * @param prefix      The expected prefix.
     * @param ignoreCase  {@code true} if the case should be ignored.
     * @return {@code true} if the given sequence starts with the given prefix.
     * @throws NullPointerException if any of the arguments is null.
     */
    public static boolean startsWith(final CharSequence sequence, final CharSequence prefix, final boolean ignoreCase) {
        final int lgs = sequence.length();
        final int lgp = prefix  .length();
        int is = 0;
        int ip = 0;
        while (ip < lgp) {
            if (is >= lgs) {
                return false;
            }
            final int cs = codePointAt(sequence, is);
            final int cp = codePointAt(prefix,   ip);
            if (cs != cp && (!ignoreCase || !equalsIgnoreCase(cs, cp))) {
                return false;
            }
            is += charCount(cs);
            ip += charCount(cp);
        }
        return true;
    }

    /**
     * Returns {@code true} if the given character sequence ends with the given suffix.
     *
     * @param sequence    The sequence to test.
     * @param suffix      The expected suffix.
     * @param ignoreCase  {@code true} if the case should be ignored.
     * @return {@code true} if the given sequence ends with the given suffix.
     * @throws NullPointerException if any of the arguments is null.
     */
    public static boolean endsWith(final CharSequence sequence, final CharSequence suffix, final boolean ignoreCase) {
        int is = sequence.length();
        int ip = suffix  .length();
        while (ip > 0) {
            if (is <= 0) {
                return false;
            }
            final int cs = codePointBefore(sequence, is);
            final int cp = codePointBefore(suffix,   ip);
            if (cs != cp && (!ignoreCase || !equalsIgnoreCase(cs, cp))) {
                return false;
            }
            is -= charCount(cs);
            ip -= charCount(cp);
        }
        return true;
    }

    /**
     * Returns the index of the first character after the given number of lines.
     * This method counts the number of occurrence of {@code '\n'}, {@code '\r'}
     * or {@code "\r\n"} starting from the given position. When {@code numToSkip}
     * occurrences have been found, the index of the first character after the last
     * occurrence is returned.
     *
     * @param string    The string in which to skip a determined amount of lines.
     * @param numToSkip The number of lines to skip. Can be positive, zero or negative.
     * @param startAt   Index at which to start the search.
     * @return Index of the first character after the last skipped line.
     * @throws NullPointerException if the {@code string} argument is null.
     */
    public static int skipLines(final CharSequence string, int numToSkip, int startAt) {
        final int length = string.length();
        /*
         * Go backward if the number of lines is negative.
         * No need to use the codePoint API because we are
         * looking only for '\r' and '\n' characters.
         */
        if (numToSkip < 0) {
            do {
                char c;
                do {
                    if (startAt == 0) {
                        return startAt;
                    }
                    c = string.charAt(--startAt);
                    if (c == '\n') {
                        if (startAt != 0 && string.charAt(startAt - 1) == '\r') {
                            --startAt;
                        }
                        break;
                    }
                } while (c != '\r');
            } while (++numToSkip != 0);
            numToSkip = 1; // For skipping the "end of line" characters.
        }
        /*
         * Skips forward the given amount of lines.
         */
        while (--numToSkip >= 0) {
            char c;
            do {
                if (startAt >= length) {
                    return startAt;
                }
                c = string.charAt(startAt++);
                if (c == '\r') {
                    if (startAt != length && string.charAt(startAt) == '\n') {
                        startAt++;
                    }
                    break;
                }
            } while (c != '\n');
        }
        return startAt;
    }

    /**
     * Returns a {@link String} instance for each line found in a multi-lines string. Each element
     * in the returned array will be a single line. If the given text is already a single line,
     * then this method returns a singleton containing only the given text.
     * <p>
     * The converse of this method is {@link #formatList(Iterable, String)}.
     *
     * {@note This method has been designed in a time when <code>String.substring(int,int)</code>
     * was cheap, because it shared the same internal <code>char[]</code> array than the original
     * array. However as of JDK8, the <code>String</code> implementation changed and now copies
     * the data. The pertinence of this method may need to be re-evaluated.}
     *
     * @param  text The multi-line text from which to get the individual lines.
     * @return The lines in the text, or {@code null} if the given text was null.
     */
    public static String[] getLinesFromMultilines(final String text) {
        if (text == null) {
            return null;
        }
        /*
         * This method is implemented on top of String.indexOf(int,int), which is the
         * fatest method available while taking care of the complexity of code points.
         */
        int lf = text.indexOf('\n');
        int cr = text.indexOf('\r');
        if (lf < 0 && cr < 0) {
            return new String[] {
                text
            };
        }
        int count = 0;
        String[] splitted = new String[8];
        int last = 0;
        boolean hasMore;
        do {
            int skip = 1;
            final int splitAt;
            if (cr < 0) {
                // There is no "\r" character in the whole text, only "\n".
                splitAt = lf;
                hasMore = (lf = text.indexOf('\n', lf+1)) >= 0;
            } else if (lf < 0) {
                // There is no "\n" character in the whole text, only "\r".
                splitAt = cr;
                hasMore = (cr = text.indexOf('\r', cr+1)) >= 0;
            } else if (lf < cr) {
                // There is both "\n" and "\r" characters with "\n" first.
                splitAt = lf;
                hasMore = true;
                lf = text.indexOf('\n', lf+1);
            } else {
                // There is both "\r" and "\n" characters with "\r" first.
                // We need special care for the "\r\n" sequence.
                splitAt = cr;
                if (lf == ++cr) {
                    cr = text.indexOf('\r', cr+1);
                    lf = text.indexOf('\n', lf+1);
                    hasMore = (cr >= 0 || lf >= 0);
                    skip = 2;
                } else {
                    cr = text.indexOf('\r', cr+1);
                    hasMore = true; // Because there is lf.
                }
            }
            if (count >= splitted.length) {
                splitted = copyOf(splitted, count*2);
            }
            splitted[count++] = text.substring(last, splitAt);
            last = splitAt + skip;
        } while (hasMore);
        /*
         * Add the remaining string and we are done.
         */
        if (count >= splitted.length) {
            splitted = copyOf(splitted, count+1);
        }
        splitted[count++] = text.substring(last);
        return resize(splitted, count);
    }
}
