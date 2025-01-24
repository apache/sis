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

import java.util.Arrays;
import java.util.Objects;
import java.nio.CharBuffer;
import static java.lang.Character.*;
import org.opengis.metadata.citation.Citation;      // For javadoc
import org.opengis.referencing.IdentifiedObject;    // For javadoc


/**
 * Static methods working with {@link CharSequence} instances. Some methods defined in this
 * class duplicate the functionalities already provided in the standard {@link String} class,
 * but works on a generic {@code CharSequence} instance instead of {@code String}.
 *
 * <h2>Unicode support</h2>
 * Every methods defined in this class work on <i>code points</i> instead of characters
 * when appropriate. Consequently, those methods should behave correctly with characters outside
 * the <i>Basic Multilingual Plane</i> (BMP).
 *
 * <h2>Policy on space characters</h2>
 * Java defines two methods for testing if a character is a white space:
 * {@link Character#isWhitespace(int)} and {@link Character#isSpaceChar(int)}.
 * Those two methods differ in the way they handle {@linkplain Characters#NO_BREAK_SPACE
 * no-break spaces}, tabulations and line feeds. The general policy in the SIS library is:
 *
 * <ul>
 *   <li>Use {@code isWhitespace(…)} when separating entities (words, numbers, tokens, <i>etc.</i>)
 *       in a list. Using that method, characters separated by a no-break space are considered as
 *       part of the same entity.</li>
 *   <li>Use {@code isSpaceChar(…)} when parsing a single entity, for example a single word.
 *       Using this method, no-break spaces are considered as part of the entity while line
 *       feeds or tabulations are entity boundaries.</li>
 * </ul>
 *
 * For example numbers formatted in the French locale use no-break spaces as group separators.
 * When parsing a list of numbers, ordinary spaces around the numbers may need to be ignored,
 * but no-break spaces shall be considered as part of the numbers.
 * Consequently, {@code isWhitespace(…)} is appropriate for skipping spaces <em>between</em> the numbers.
 * But if there is spaces to skip <em>inside</em> a single number, then {@code isSpaceChar(…)} is a good choice
 * for accepting no-break spaces and for stopping the parse operation at tabulations or line feed character.
 * A tabulation or line feed between two characters is very likely to separate two distinct values.
 *
 * In practice, the {@link java.text.Format} implementations in the SIS library typically use
 * {@code isSpaceChar(…)} while most of the rest of the SIS library, including this
 * {@code CharSequences} class, consistently uses {@code isWhitespace(…)}.
 *
 * <h2>Handling of null values</h2>
 * Most methods in this class accept a {@code null} {@code CharSequence} argument. In such cases
 * the method return value is either a {@code null} {@code CharSequence}, an empty array, or a
 * {@code 0} or {@code false} primitive type calculated as if the input was an empty string.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 *
 * @see StringBuilders
 *
 * @since 0.3
 */
public final class CharSequences extends Static {
    /**
     * An array of zero-length. This constant play a role equivalents to
     * {@link java.util.Collections#EMPTY_LIST}.
     */
    public static final String[] EMPTY_ARRAY = new String[0];

    /**
     * An array of strings containing only white spaces. String lengths are equal to their
     * index in the {@code spaces} array. For example, {@code spaces[4]} contains a string
     * of length 4. Strings are constructed only when first needed.
     */
    private static final String[] SPACES = new String[10];

    /**
     * Do not allow instantiation of this class.
     */
    private CharSequences() {
    }

    /**
     * Returns the code point after the given index. This method completes
     * {@link Character#codePointBefore(CharSequence, int)} but is rarely used because slightly
     * inefficient (in most cases, the code point at {@code index} is known together with the
     * corresponding {@code charCount(int)} value, so the method calls should be unnecessary).
     */
    private static int codePointAfter(final CharSequence text, final int index) {
        return codePointAt(text, index + charCount(codePointAt(text, index)));
    }

    /**
     * Returns a character sequence of the specified length filled with white spaces.
     *
     * <h4>Use case</h4>
     * This method is typically invoked for performing right-alignment of text on the
     * {@linkplain java.io.Console console} or other device using monospaced font.
     * Callers compute a value for the {@code length} argument by (<var>desired width</var> - <var>used width</var>).
     * Since the <var>used width</var> value may be greater than expected, this method handle negative {@code length}
     * values as if the value was zero.
     *
     * @param  length  the string length. Negative values are clamped to 0.
     * @return a string of length {@code length} filled with white spaces.
     */
    public static CharSequence spaces(final int length) {
        /*
         * No need to synchronize.  In the unlikely event of two threads calling this method
         * at the same time and the two calls creating a new string, the String.intern() call
         * will take care of canonicalizing the strings.
         */
        if (length <= 0) {
            return "";
        }
        if (length < SPACES.length) {
            String s = SPACES[length - 1];
            if (s == null) {
                final char[] spaces = new char[length];
                Arrays.fill(spaces, ' ');
                s = new String(spaces).intern();
                SPACES[length - 1] = s;
            }
            return s;
        }
        return new CharSequence() {
            @Override public int length() {
                return length;
            }

            @Override public char charAt(int index) {
                Objects.checkIndex(index, length);
                return ' ';
            }

            @Override public CharSequence subSequence(final int start, final int end) {
                Objects.checkFromToIndex(start, end, length);
                final int n = end - start;
                return (n == length) ? this : spaces(n);
            }

            @Override public String toString() {
                final char[] array = new char[length];
                Arrays.fill(array, ' ');
                return new String(array);
            }
        };
    }

    /**
     * Returns the {@linkplain CharSequence#length() length} of the given characters sequence,
     * or 0 if {@code null}.
     *
     * @param  text  the character sequence from which to get the length, or {@code null}.
     * @return the length of the character sequence, or 0 if the argument is {@code null}.
     */
    public static int length(final CharSequence text) {
        return (text != null) ? text.length() : 0;
    }

    /**
     * Returns the number of Unicode code points in the given characters sequence,
     * or 0 if {@code null}. Unpaired surrogates within the text count as one code
     * point each.
     *
     * @param  text  the character sequence from which to get the count, or {@code null}.
     * @return the number of Unicode code points, or 0 if the argument is {@code null}.
     *
     * @see #codePointCount(CharSequence, int, int)
     */
    public static int codePointCount(final CharSequence text) {
        return (text != null) ? codePointCount(text, 0, text.length()) : 0;
    }

    /**
     * Returns the number of Unicode code points in the given characters sub-sequence,
     * or 0 if {@code null}. Unpaired surrogates within the text count as one code
     * point each.
     *
     * <p>This method performs the same work as the standard
     * {@link Character#codePointCount(CharSequence, int, int)} method, except that it tries
     * to delegate to the optimized methods from the {@link String}, {@link StringBuilder},
     * {@link StringBuffer} or {@link CharBuffer} classes if possible.</p>
     *
     * @param  text       the character sequence from which to get the count, or {@code null}.
     * @param  fromIndex  the index from which to start the computation.
     * @param  toIndex    the index after the last character to take in account.
     * @return the number of Unicode code points, or 0 if the argument is {@code null}.
     *
     * @see Character#codePointCount(CharSequence, int, int)
     * @see String#codePointCount(int, int)
     * @see StringBuilder#codePointCount(int, int)
     */
    public static int codePointCount(final CharSequence text, final int fromIndex, final int toIndex) {
        if (text == null)                  return 0;
        if (text instanceof String)        return ((String)        text).codePointCount(fromIndex, toIndex);
        if (text instanceof StringBuilder) return ((StringBuilder) text).codePointCount(fromIndex, toIndex);
        if (text instanceof StringBuffer)  return ((StringBuffer)  text).codePointCount(fromIndex, toIndex);
        if (text instanceof CharBuffer) {
            final var buffer = (CharBuffer) text;
            if (buffer.hasArray() && !buffer.isReadOnly()) {
                final int position = buffer.position();
                return Character.codePointCount(buffer.array(), position + fromIndex, position + toIndex);
            }
        }
        return Character.codePointCount(text, fromIndex, toIndex);
    }

    /**
     * Returns the number of occurrences of the {@code toSearch} string in the given {@code text}.
     * The search is case-sensitive.
     *
     * @param  text      the character sequence to count occurrences, or {@code null}.
     * @param  toSearch  the string to search in the given {@code text}.
     *                   It shall contain at least one character.
     * @return the number of occurrences of {@code toSearch} in {@code text},
     *         or 0 if {@code text} was null or empty.
     * @throws NullPointerException if the {@code toSearch} argument is null.
     * @throws IllegalArgumentException if the {@code toSearch} argument is empty.
     */
    public static int count(final CharSequence text, final String toSearch) {
        ArgumentChecks.ensureNonEmpty("toSearch", toSearch);
        final int length = toSearch.length();
        if (length == 1) {
            // Implementation working on a single character is faster.
            return count(text, toSearch.charAt(0));
        }
        int n = 0;
        if (text != null) {
            int i = 0;
            while ((i = indexOf(text, toSearch, i, text.length())) >= 0) {
                n++;
                i += length;
            }
        }
        return n;
    }

    /**
     * Counts the number of occurrence of the given character in the given character sequence.
     *
     * @param  text      the character sequence to count occurrences, or {@code null}.
     * @param  toSearch  the character to count.
     * @return the number of occurrences of the given character, or 0 if the {@code text} is null.
     */
    public static int count(final CharSequence text, final char toSearch) {
        int n = 0;
        if (text != null) {
            if (text instanceof String) {
                final var s = (String) text;
                for (int i=s.indexOf(toSearch); ++i != 0; i=s.indexOf(toSearch, i)) {
                    n++;
                }
            } else {
                // No need to use the code point API here, since we are looking for exact matches.
                for (int i=text.length(); --i>=0;) {
                    if (text.charAt(i) == toSearch) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    /**
     * Returns the index within the given strings of the first occurrence of the specified part,
     * starting at the specified index. This method is equivalent to the following method call,
     * except that this method works on arbitrary {@link CharSequence} objects instead of
     * {@link String}s only, and that the upper limit can be specified:
     *
     * {@snippet lang="java" :
     *     return text.indexOf(part, fromIndex);
     *     }
     *
     * There is no restriction on the value of {@code fromIndex}. If negative or greater
     * than {@code toIndex}, then the behavior of this method is as if the search started
     * from 0 or {@code toIndex} respectively. This is consistent with the
     * {@link String#indexOf(String, int)} behavior.
     *
     * @param  text       the string in which to perform the search.
     * @param  toSearch   the substring for which to search.
     * @param  fromIndex  the index from which to start the search.
     * @param  toIndex    the index after the last character where to perform the search.
     * @return the index within the text of the first occurrence of the specified part, starting at the specified index,
     *         or -1 if no occurrence has been found or if the {@code text} argument is null.
     * @throws NullPointerException if the {@code toSearch} argument is null.
     * @throws IllegalArgumentException if the {@code toSearch} argument is empty.
     *
     * @see String#indexOf(String, int)
     * @see StringBuilder#indexOf(String, int)
     * @see StringBuffer#indexOf(String, int)
     */
    public static int indexOf(final CharSequence text, final CharSequence toSearch, int fromIndex, int toIndex) {
        ArgumentChecks.ensureNonEmpty("toSearch", toSearch);
        if (text != null) {
            int length = text.length();
            if (toIndex > length) {
                toIndex = length;
            }
            if (toSearch instanceof String && toIndex == length) {
                if (text instanceof String) {
                    return ((String) text).indexOf((String) toSearch, fromIndex);
                }
                if (text instanceof StringBuilder) {
                    return ((StringBuilder) text).indexOf((String) toSearch, fromIndex);
                }
                if (text instanceof StringBuffer) {
                    return ((StringBuffer) text).indexOf((String) toSearch, fromIndex);
                }
            }
            if (fromIndex < 0) {
                fromIndex = 0;
            }
            length = toSearch.length();
            toIndex -= length;
search:     for (; fromIndex <= toIndex; fromIndex++) {
                for (int i=0; i<length; i++) {
                    // No need to use the codePointAt API here, since we are looking for exact matches.
                    if (text.charAt(fromIndex + i) != toSearch.charAt(i)) {
                        continue search;
                    }
                }
                return fromIndex;
            }
        }
        return -1;
    }

    /**
     * Returns the index within the given character sequence of the first occurrence of the
     * specified character, starting the search at the specified index. If the character is
     * not found, then this method returns -1.
     *
     * <p>There is no restriction on the value of {@code fromIndex}. If negative or greater
     * than {@code toIndex}, then the behavior of this method is as if the search started
     * from 0 or {@code toIndex} respectively. This is consistent with the behavior documented
     * in {@link String#indexOf(int, int)}.</p>
     *
     * @param  text       the character sequence in which to perform the search, or {@code null}.
     * @param  toSearch   the Unicode code point of the character to search.
     * @param  fromIndex  the index to start the search from.
     * @param  toIndex    the index after the last character where to perform the search.
     * @return the index of the first occurrence of the given character in the specified sub-sequence,
     *         or -1 if no occurrence has been found or if the {@code text} argument is null.
     *
     * @see String#indexOf(int, int)
     */
    public static int indexOf(final CharSequence text, final int toSearch, int fromIndex, int toIndex) {
        if (text != null) {
            final int length = text.length();
            if (toIndex >= length) {
                if (text instanceof String) {
                    // String provides a faster implementation.
                    return ((String) text).indexOf(toSearch, fromIndex);
                }
                toIndex = length;
            }
            if (fromIndex < 0) {
                fromIndex = 0;
            }
            char head = (char) toSearch;
            char tail = (char) 0;
            if (head != toSearch) {                     // Outside BMP plane?
                head = highSurrogate(toSearch);
                tail = lowSurrogate (toSearch);
                toIndex--;
            }
            while (fromIndex < toIndex) {
                if (text.charAt(fromIndex) == head) {
                    if (tail == 0 || text.charAt(fromIndex+1) == tail) {
                        return fromIndex;
                    }
                }
                fromIndex++;
            }
        }
        return -1;
    }

    /**
     * Returns the index within the given character sequence of the last occurrence of the
     * specified character, searching backward in the given index range.
     * If the character is not found, then this method returns -1.
     *
     * <p>There is no restriction on the value of {@code toIndex}. If greater than the text length
     * or less than {@code fromIndex}, then the behavior of this method is as if the search started
     * from {@code length} or {@code fromIndex} respectively. This is consistent with the behavior
     * documented in {@link String#lastIndexOf(int, int)}.</p>
     *
     * @param  text       the character sequence in which to perform the search, or {@code null}.
     * @param  toSearch   the Unicode code point of the character to search.
     * @param  fromIndex  the index of the first character in the range where to perform the search.
     * @param  toIndex    the index after the last character in the range where to perform the search.
     * @return the index of the last occurrence of the given character in the specified sub-sequence,
     *         or -1 if no occurrence has been found or if the {@code text} argument is null.
     *
     * @see String#lastIndexOf(int, int)
     */
    public static int lastIndexOf(final CharSequence text, final int toSearch, int fromIndex, int toIndex) {
        if (text != null) {
            if (fromIndex <= 0) {
                if (text instanceof String) {
                    // String provides a faster implementation.
                    return ((String) text).lastIndexOf(toSearch, toIndex - 1);
                }
                fromIndex = 0;
            }
            final int length = text.length();
            if (toIndex > length) {
                toIndex = length;
            }
            char tail = (char) toSearch;
            char head = (char) 0;
            if (tail != toSearch) { // Outside BMP plane?
                tail = lowSurrogate (toSearch);
                head = highSurrogate(toSearch);
                fromIndex++;
            }
            while (toIndex > fromIndex) {
                if (text.charAt(--toIndex) == tail) {
                    if (head == 0 || text.charAt(--toIndex) == head) {
                        return toIndex;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Returns the index of the first character after the given number of lines.
     * This method counts the number of occurrence of {@code '\n'}, {@code '\r'}
     * or {@code "\r\n"} starting from the given position. When {@code numLines}
     * occurrences have been found, the index of the first character after the last
     * occurrence is returned.
     *
     * <p>If the {@code numLines} argument is positive, this method searches forward.
     * If negative, this method searches backward. If 0, this method returns the
     * beginning of the current line.</p>
     *
     * <p>If this method reaches the end of {@code text} while searching forward, then
     * {@code text.length()} is returned. If this method reaches the beginning of
     * {@code text} while searching backward, then 0 is returned.</p>
     *
     * @param  text       the string in which to skip a determined number of lines.
     * @param  numLines   the number of lines to skip. Can be positive, zero or negative.
     * @param  fromIndex  index at which to start the search, from 0 to {@code text.length()} inclusive.
     * @return index of the first character after the last skipped line.
     * @throws NullPointerException if the {@code text} argument is null.
     * @throws IndexOutOfBoundsException if {@code fromIndex} is out of bounds.
     */
    public static int indexOfLineStart(final CharSequence text, int numLines, int fromIndex) {
        final int length = text.length();
        /*
         * Go backward if the number of lines is negative.
         * No need to use the codePoint API because we are
         * looking only for characters in the BMP plane.
         */
        if (numLines <= 0) {
            do {
                char c;
                do {
                    if (fromIndex == 0) {
                        return fromIndex;
                    }
                    c = text.charAt(--fromIndex);
                    if (c == '\n') {
                        if (fromIndex != 0 && text.charAt(fromIndex - 1) == '\r') {
                            --fromIndex;
                        }
                        break;
                    }
                } while (c != '\r');
            } while (++numLines != 1);
            // Execute the forward code below for skipping the "end of line" characters.
        }
        /*
         * Skips forward the given number of lines.
         */
        while (--numLines >= 0) {
            char c;
            do {
                if (fromIndex == length) {
                    return fromIndex;
                }
                c = text.charAt(fromIndex++);
                if (c == '\r') {
                    if (fromIndex != length && text.charAt(fromIndex) == '\n') {
                        fromIndex++;
                    }
                    break;
                }
            } while (c != '\n');
        }
        return fromIndex;
    }

    /**
     * Returns the index of the first non-white character in the given range.
     * If the given range contains only space characters, then this method returns the index of the
     * first character after the given range, which is always equals or greater than {@code toIndex}.
     * Note that this character may not exist if {@code toIndex} is equal to the text length.
     *
     * <p>Special cases:</p>
     * <ul>
     *   <li>If {@code fromIndex} is greater than {@code toIndex},
     *       then this method unconditionally returns {@code fromIndex}.</li>
     *   <li>If the given range contains only space characters and the character at {@code toIndex-1}
     *       is the high surrogate of a valid supplementary code point, then this method returns
     *       {@code toIndex+1}, which is the index of the next code point.</li>
     *   <li>If {@code fromIndex} is negative or {@code toIndex} is greater than the text length,
     *       then the behavior of this method is undefined.</li>
     * </ul>
     *
     * Space characters are identified by the {@link Character#isWhitespace(int)} method.
     *
     * @param  text       the string in which to perform the search (cannot be null).
     * @param  fromIndex  the index from which to start the search (cannot be negative).
     * @param  toIndex    the index after the last character where to perform the search.
     * @return the index within the text of the first occurrence of a non-space character, starting
     *         at the specified index, or a value equals or greater than {@code toIndex} if none.
     * @throws NullPointerException if the {@code text} argument is null.
     *
     * @see #skipTrailingWhitespaces(CharSequence, int, int)
     * @see #trimWhitespaces(CharSequence)
     * @see String#stripLeading()
     */
    public static int skipLeadingWhitespaces(final CharSequence text, int fromIndex, final int toIndex) {
        while (fromIndex < toIndex) {
            final int c = codePointAt(text, fromIndex);
            if (!isWhitespace(c)) break;
            fromIndex += charCount(c);
        }
        return fromIndex;
    }

    /**
     * Returns the index <em>after</em> the last non-white character in the given range.
     * If the given range contains only space characters, then this method returns the index of the
     * first character in the given range, which is always equals or lower than {@code fromIndex}.
     *
     * <p>Special cases:</p>
     * <ul>
     *   <li>If {@code fromIndex} is lower than {@code toIndex},
     *       then this method unconditionally returns {@code toIndex}.</li>
     *   <li>If the given range contains only space characters and the character at {@code fromIndex}
     *       is the low surrogate of a valid supplementary code point, then this method returns
     *       {@code fromIndex-1}, which is the index of the code point.</li>
     *   <li>If {@code fromIndex} is negative or {@code toIndex} is greater than the text length,
     *       then the behavior of this method is undefined.</li>
     * </ul>
     *
     * Space characters are identified by the {@link Character#isWhitespace(int)} method.
     *
     * @param  text       the string in which to perform the search (cannot be null).
     * @param  fromIndex  the index from which to start the search (cannot be negative).
     * @param  toIndex    the index after the last character where to perform the search.
     * @return the index within the text of the last occurrence of a non-space character, starting
     *         at the specified index, or a value equals or lower than {@code fromIndex} if none.
     * @throws NullPointerException if the {@code text} argument is null.
     *
     * @see #skipLeadingWhitespaces(CharSequence, int, int)
     * @see #trimWhitespaces(CharSequence)
     * @see String#stripTrailing()
     */
    public static int skipTrailingWhitespaces(final CharSequence text, final int fromIndex, int toIndex) {
        while (toIndex > fromIndex) {
            final int c = codePointBefore(text, toIndex);
            if (!isWhitespace(c)) break;
            toIndex -= charCount(c);
        }
        return toIndex;
    }

    /**
     * Allocates the array to be returned by the {@code split(…)} methods. If the given {@code text} argument is
     * an instance of {@link String}, {@link StringBuilder} or {@link StringBuffer},  then this method returns a
     * {@code String[]} array instead of {@code CharSequence[]}. This is possible because the specification of
     * their {@link CharSequence#subSequence(int, int)} method guarantees to return {@code String} instances.
     * Some Apache SIS code will cast the {@code split(…)} return value based on this knowledge.
     *
     * <p>Note that this is a undocumented SIS features. There is currently no commitment that this implementation
     * details will not change in future version.</p>
     *
     * @param  text  the text to be split.
     * @return an array where to store the result of splitting the given {@code text}.
     */
    private static CharSequence[] createSplitArray(final CharSequence text) {
        return (text instanceof String ||
                text instanceof StringBuilder ||
                text instanceof StringBuffer) ? new String[8] : new CharSequence[8];
    }

    /**
     * Splits a text around the given character. The array returned by this method contains all
     * subsequences of the given text that is terminated by the given character or is terminated
     * by the end of the text. The subsequences in the array are in the order in which they occur
     * in the given text. If the character is not found in the input, then the resulting array has
     * just one element, which is the whole given text.
     *
     * <p>This method is similar to the standard {@link String#split(String)} method except for the
     * following:</p>
     *
     * <ul>
     *   <li>It accepts generic character sequences.</li>
     *   <li>It accepts {@code null} argument, in which case an empty array is returned.</li>
     *   <li>The separator is a simple character instead of a regular expression.</li>
     *   <li>If the {@code separator} argument is {@code '\n'} or {@code '\r'}, then this method
     *       splits around any of {@code "\r"}, {@code "\n"} or {@code "\r\n"} characters sequences.
     *   <li>The leading and trailing spaces of each subsequences are trimmed.</li>
     * </ul>
     *
     * @param  text       the text to split, or {@code null}.
     * @param  separator  the delimiting character (typically the coma).
     * @return the array of subsequences computed by splitting the given text around the given
     *         character, or an empty array if {@code text} was null.
     *
     * @see String#split(String)
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public static CharSequence[] split(final CharSequence text, final char separator) {
        if (text == null) {
            return EMPTY_ARRAY;
        }
        if (separator == '\n' || separator == '\r') {
            final CharSequence[] split = splitOnEOL(text);
            for (int i=0; i < split.length; i++) {
                // For consistency with the rest of this method.
                split[i] = trimWhitespaces(split[i]);
            }
            return split;
        }
        // 'excludeEmpty' must use the same criterion as trimWhitespaces(…).
        final boolean excludeEmpty = isWhitespace(separator);
        CharSequence[] split = createSplitArray(text);
        final int length = text.length();
        int count = 0, last  = 0, i = 0;
        while ((i = indexOf(text, separator, i, length)) >= 0) {
            final CharSequence item = trimWhitespaces(text, last, i);
            if (!excludeEmpty || item.length() != 0) {
                if (count == split.length) {
                    split = Arrays.copyOf(split, count << 1);
                }
                split[count++] = item;
            }
            last = ++i;
        }
        // Add the last element.
        final CharSequence item = trimWhitespaces(text, last, length);
        if (!excludeEmpty || item.length() != 0) {
            if (count == split.length) {
                split = Arrays.copyOf(split, count + 1);
            }
            split[count++] = item;
        }
        return ArraysExt.resize(split, count);
    }

    /**
     * Splits a text around the <i>End Of Line</i> (EOL) characters.
     * EOL characters can be any of {@code "\r"}, {@code "\n"} or {@code "\r\n"} sequences.
     * Each element in the returned array will be a single line. If the given text is already
     * a single line, then this method returns a singleton containing only the given text.
     *
     * <p>Notes:</p>
     * <ul>
     *   <li>At the difference of <code>{@linkplain #split split}(toSplit, '\n’)</code>,
     *       this method does not remove whitespaces.</li>
     *   <li>This method does not check for Unicode
     *       {@linkplain Characters#LINE_SEPARATOR line separator} and
     *       {@linkplain Characters#PARAGRAPH_SEPARATOR paragraph separator}.</li>
     * </ul>
     *
     * <h4>Performance note</h4>
     * Prior Java 8 this method was usually cheap because all string instances created by
     * {@link String#substring(int,int)} shared the same {@code char[]} internal array.
     * However, since Java 8, the new {@code String} implementation copies the data in new arrays.
     * Consequently, it is better to use index rather than this method for splitting large {@code String}s.
     * However, this method still useful for other {@link CharSequence} implementations providing an efficient
     * {@code subSequence(int,int)} method.
     *
     * @param  text  the multi-line text from which to get the individual lines, or {@code null}.
     * @return the lines in the text, or an empty array if the given text was null.
     *
     * @see #indexOfLineStart(CharSequence, int, int)
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public static CharSequence[] splitOnEOL(final CharSequence text) {
        if (text == null) {
            return EMPTY_ARRAY;
        }
        /*
         * This method is implemented on top of String.indexOf(int,int),
         * assuming that it will be faster for String and StringBuilder.
         */
        final int length = text.length();
        int lf = indexOf(text, '\n', 0, length);
        int cr = indexOf(text, '\r', 0, length);
        if (lf < 0 && cr < 0) {
            return new CharSequence[] {
                text
            };
        }
        int count = 0;
        CharSequence[] split = createSplitArray(text);
        int last = 0;
        boolean hasMore;
        do {
            int skip = 1;
            final int splitAt;
            if (cr < 0) {
                // There is no "\r" character in the whole text, only "\n".
                splitAt = lf;
                hasMore = (lf = indexOf(text, '\n', lf+1, length)) >= 0;
            } else if (lf < 0) {
                // There is no "\n" character in the whole text, only "\r".
                splitAt = cr;
                hasMore = (cr = indexOf(text, '\r', cr+1, length)) >= 0;
            } else if (lf < cr) {
                // There is both "\n" and "\r" characters with "\n" first.
                splitAt = lf;
                hasMore = true;
                lf = indexOf(text, '\n', lf+1, length);
            } else {
                // There is both "\r" and "\n" characters with "\r" first.
                // We need special care for the "\r\n" sequence.
                splitAt = cr;
                if (lf == ++cr) {
                    cr = indexOf(text, '\r', cr+1, length);
                    lf = indexOf(text, '\n', lf+1, length);
                    hasMore = (cr >= 0 || lf >= 0);
                    skip = 2;
                } else {
                    cr = indexOf(text, '\r', cr+1, length);
                    hasMore = true; // Because there is lf.
                }
            }
            if (count >= split.length) {
                split = Arrays.copyOf(split, count*2);
            }
            split[count++] = text.subSequence(last, splitAt);
            last = splitAt + skip;
        } while (hasMore);
        /*
         * Add the remaining string and we are done.
         */
        if (count >= split.length) {
            split = Arrays.copyOf(split, count+1);
        }
        split[count++] = text.subSequence(last, text.length());
        return ArraysExt.resize(split, count);
    }

    /**
     * Returns {@code true} if {@link #split(CharSequence, char)} parsed an empty string.
     */
    private static boolean isEmpty(final CharSequence[] tokens) {
        switch (tokens.length) {
            case 0:  return true;
            case 1:  return tokens[0].length() == 0;
            default: return false;
        }
    }

    /**
     * {@linkplain #split(CharSequence, char) Splits} the given text around the given character,
     * then {@linkplain Double#parseDouble(String) parses} each item as a {@code double}.
     * Empty sub-sequences are parsed as {@link Double#NaN}.
     *
     * @param  values     the text containing the values to parse, or {@code null}.
     * @param  separator  the delimiting character (typically the coma).
     * @return the array of numbers parsed from the given text,
     *         or an empty array if {@code values} was null.
     * @throws NumberFormatException if at least one number cannot be parsed.
     */
    public static double[] parseDoubles(final CharSequence values, final char separator)
            throws NumberFormatException
    {
        final CharSequence[] tokens = split(values, separator);
        if (isEmpty(tokens)) return ArraysExt.EMPTY_DOUBLE;
        final double[] parsed = new double[tokens.length];
        for (int i=0; i<tokens.length; i++) {
            final String token = trimWhitespaces(tokens[i]).toString();
            parsed[i] = token.isEmpty() ? Double.NaN : Double.parseDouble(token);
        }
        return parsed;
    }

    /**
     * {@linkplain #split(CharSequence, char) Splits} the given text around the given character,
     * then {@linkplain Float#parseFloat(String) parses} each item as a {@code float}.
     * Empty sub-sequences are parsed as {@link Float#NaN}.
     *
     * @param  values     the text containing the values to parse, or {@code null}.
     * @param  separator  the delimiting character (typically the coma).
     * @return the array of numbers parsed from the given text,
     *         or an empty array if {@code values} was null.
     * @throws NumberFormatException if at least one number cannot be parsed.
     */
    public static float[] parseFloats(final CharSequence values, final char separator)
            throws NumberFormatException
    {
        final CharSequence[] tokens = split(values, separator);
        if (isEmpty(tokens)) return ArraysExt.EMPTY_FLOAT;
        final float[] parsed = new float[tokens.length];
        for (int i=0; i<tokens.length; i++) {
            final String token = trimWhitespaces(tokens[i]).toString();
            parsed[i] = token.isEmpty() ? Float.NaN : Float.parseFloat(token);
        }
        return parsed;
    }

    /**
     * {@linkplain #split(CharSequence, char) Splits} the given text around the given character,
     * then {@linkplain Long#parseLong(String) parses} each item as a {@code long}.
     *
     * @param  values     the text containing the values to parse, or {@code null}.
     * @param  separator  the delimiting character (typically the coma).
     * @param  radix      the radix to be used for parsing. This is usually 10.
     * @return the array of numbers parsed from the given text,
     *         or an empty array if {@code values} was null.
     * @throws NumberFormatException if at least one number cannot be parsed.
     */
    public static long[] parseLongs(final CharSequence values, final char separator, final int radix)
            throws NumberFormatException
    {
        final CharSequence[] tokens = split(values, separator);
        if (isEmpty(tokens)) return ArraysExt.EMPTY_LONG;
        final long[] parsed = new long[tokens.length];
        for (int i=0; i<tokens.length; i++) {
            parsed[i] = Long.parseLong(trimWhitespaces(tokens[i]).toString(), radix);
        }
        return parsed;
    }

    /**
     * {@linkplain #split(CharSequence, char) Splits} the given text around the given character,
     * then {@linkplain Integer#parseInt(String) parses} each item as an {@code int}.
     *
     * @param  values     the text containing the values to parse, or {@code null}.
     * @param  separator  the delimiting character (typically the coma).
     * @param  radix      the radix to be used for parsing. This is usually 10.
     * @return the array of numbers parsed from the given text,
     *         or an empty array if {@code values} was null.
     * @throws NumberFormatException if at least one number cannot be parsed.
     */
    public static int[] parseInts(final CharSequence values, final char separator, final int radix)
            throws NumberFormatException
    {
        final CharSequence[] tokens = split(values, separator);
        if (isEmpty(tokens)) return ArraysExt.EMPTY_INT;
        final int[] parsed = new int[tokens.length];
        for (int i=0; i<tokens.length; i++) {
            parsed[i] = Integer.parseInt(trimWhitespaces(tokens[i]).toString(), radix);
        }
        return parsed;
    }

    /**
     * {@linkplain #split(CharSequence, char) Splits} the given text around the given character,
     * then {@linkplain Short#parseShort(String) parses} each item as a {@code short}.
     *
     * @param  values     the text containing the values to parse, or {@code null}.
     * @param  separator  the delimiting character (typically the coma).
     * @param  radix      the radix to be used for parsing. This is usually 10.
     * @return the array of numbers parsed from the given text,
     *         or an empty array if {@code values} was null.
     * @throws NumberFormatException if at least one number cannot be parsed.
     */
    public static short[] parseShorts(final CharSequence values, final char separator, final int radix)
            throws NumberFormatException
    {
        final CharSequence[] tokens = split(values, separator);
        if (isEmpty(tokens)) return ArraysExt.EMPTY_SHORT;
        final short[] parsed = new short[tokens.length];
        for (int i=0; i<tokens.length; i++) {
            parsed[i] = Short.parseShort(trimWhitespaces(tokens[i]).toString(), radix);
        }
        return parsed;
    }

    /**
     * {@linkplain #split(CharSequence, char) Splits} the given text around the given character,
     * then {@linkplain Byte#parseByte(String) parses} each item as a {@code byte}.
     *
     * @param  values     the text containing the values to parse, or {@code null}.
     * @param  separator  the delimiting character (typically the coma).
     * @param  radix      the radix to be used for parsing. This is usually 10.
     * @return the array of numbers parsed from the given text,
     *         or an empty array if {@code values} was null.
     * @throws NumberFormatException if at least one number cannot be parsed.
     */
    public static byte[] parseBytes(final CharSequence values, final char separator, final int radix)
            throws NumberFormatException
    {
        final CharSequence[] tokens = split(values, separator);
        if (isEmpty(tokens)) return ArraysExt.EMPTY_BYTE;
        final byte[] parsed = new byte[tokens.length];
        for (int i=0; i<tokens.length; i++) {
            parsed[i] = Byte.parseByte(trimWhitespaces(tokens[i]).toString(), radix);
        }
        return parsed;
    }

    /**
     * Replaces some Unicode characters by ASCII characters on a "best effort basis".
     * For example, the “ é ” character is replaced by  “ e ” (without accent),
     * the  “ ″ ” symbol for minutes of angle is replaced by straight double quotes “ " ”,
     * and combined characters like ㎏, ㎎, ㎝, ㎞, ㎢, ㎦, ㎖, ㎧, ㎩, ㎐, <i>etc.</i> are replaced
     * by the corresponding sequences of characters.
     *
     * <div class="note"><b>Note:</b>
     * the replacement of Greek letters is a more complex task than what this method can do,
     * since it depends on the context. For example if the Greek letters are abbreviations
     * for coordinate system axes like φ and λ, then the replacements depend on the enclosing
     * coordinate system. See {@link org.apache.sis.io.wkt.Transliterator} for more information.</div>
     *
     * @param  text  the text to scan for Unicode characters to replace by ASCII characters, or {@code null}.
     * @return the given text with substitutions applied, or {@code text} if no replacement
     *         has been applied, or {@code null} if the given text was null.
     *
     * @see StringBuilders#toASCII(StringBuilder)
     * @see org.apache.sis.io.wkt.Transliterator#filter(String)
     * @see java.text.Normalizer
     */
    public static CharSequence toASCII(final CharSequence text) {
        return StringBuilders.toASCII(text, null);
    }

    /**
     * Returns a text with leading and trailing whitespace characters omitted.
     * Space characters are identified by the {@link Character#isWhitespace(int)} method.
     *
     * <p>This method is the generalized version of {@link String#strip()}.</p>
     *
     * @param  text  the text from which to remove leading and trailing whitespaces, or {@code null}.
     * @return a characters sequence with leading and trailing whitespaces removed,
     *         or {@code null} is the given text was null.
     *
     * @see #skipLeadingWhitespaces(CharSequence, int, int)
     * @see #skipTrailingWhitespaces(CharSequence, int, int)
     * @see String#strip()
     */
    public static CharSequence trimWhitespaces(CharSequence text) {
        if (text != null) {
            text = trimWhitespaces(text, 0, text.length());
        }
        return text;
    }

    /**
     * Returns a sub-sequence with leading and trailing whitespace characters omitted.
     * Space characters are identified by the {@link Character#isWhitespace(int)} method.
     *
     * <p>Invoking this method is functionally equivalent to the following code snippet,
     * except that the {@link CharSequence#subSequence(int, int) subSequence} method is
     * invoked only once instead of two times:</p>
     *
     * {@snippet lang="java" :
     *     text = trimWhitespaces(text.subSequence(lower, upper));
     *     }
     *
     * @param  text   the text from which to remove leading and trailing white spaces.
     * @param  lower  index of the first character to consider for inclusion in the sub-sequence.
     * @param  upper  index after the last character to consider for inclusion in the sub-sequence.
     * @return a characters sequence with leading and trailing white spaces removed, or {@code null}
     *         if the {@code text} argument is null.
     * @throws IndexOutOfBoundsException if {@code lower} or {@code upper} is out of bounds.
     */
    public static CharSequence trimWhitespaces(CharSequence text, int lower, int upper) {
        final int length = length(text);
        Objects.checkFromToIndex(lower, upper, length);
        if (text != null) {
            lower = skipLeadingWhitespaces (text, lower, upper);
            upper = skipTrailingWhitespaces(text, lower, upper);
            if (lower != 0 || upper != length) {                  // Safety in case subSequence doesn't make the check.
                text = text.subSequence(lower, upper);
            }
        }
        return text;
    }

    /**
     * Returns a text with ignorable characters in Unicode identifier removed. While valid in identifiers,
     * those {@linkplain Character#isIdentifierIgnorable(int) ignorable characters} are often non-displayed.
     * An example of ignorable character is the zero-width space.
     *
     * <h4>Relationship with XML</h4>
     * Unlike Unicode identifiers, ignorable characters are invalid in XML identifiers.
     * This restriction avoids, for example, homograph attacks in domain name.
     * So this method can be used for converting an Unicode identifier to an XML identifier,
     * except for the characters listed below. Those characters are non-ignorable
     * (so not removed by this method), but nevertheless invalid in XML identifiers.
     * <ul>
     *   <li>{@code µ} (U+00B5) — micro</li>
     *   <li>{@code ª} (U+00AA) — feminine ordinal indicator</li>
     *   <li>{@code º} (U+00BA) — masculine ordinal indicator</li>
     *   <li>{@code ⁔} (U+2054) — inverted undertie</li>
     * </ul>
     *
     * @param  text  the text from which to remove ignorable characters, or {@code null}.
     * @return text with ignorable characters removed, or {@code null} if the given text was null.
     *
     * @see Character#isIdentifierIgnorable(int)
     *
     * @since 1.5
     */
    public static CharSequence trimIgnorables(final CharSequence text) {
        if (text != null) {
            /*
             * First perform a quick check to see if there is any ignorable characters.
             * We make this check because there is usually no such characters,
             * so we will avoid the StringBuilder creation in the vast majority of times.
             *
             * Note that 'µ' and its friends are not ignorable, so we do not remove them.
             * This method is aimed for `getUnicodeIdentifier`, not `getXmlIdentifier`.
             */
            final int length = text.length();
            for (int i=0; i<length;) {
                int c = codePointAt(text, i);
                int n = Character.charCount(c);
                if (Character.isIdentifierIgnorable(c)) {
                    /*
                     * Found an ignorable character. Create the buffer and copy non-ignorable characters.
                     * Following algorithm is inefficient, since we fill the buffer character-by-character
                     * (a more efficient approach would be to perform bulk appends). However, we presume
                     * that this block will be rarely executed, so it is not worth to optimize it.
                     */
                    final var buffer = new StringBuilder(length - n).append(text, 0, i);
                    while ((i += n) < length) {
                        c = codePointAt(text, i);
                        n = Character.charCount(c);
                        if (!Character.isIdentifierIgnorable(c)) {
                            buffer.appendCodePoint(c);
                        }
                    }
                    /*
                     * No need to verify if the buffer is empty, because ignorable
                     * characters are not legal Unicode identifier start.
                     */
                    return buffer.toString();
                }
                i += n;
            }
        }
        return text;
    }

    /**
     * Trims the fractional part of the given formatted number, provided that it doesn't change
     * the value. This method assumes that the number is formatted in the US locale, typically
     * by the {@link Double#toString(double)} method.
     *
     * <p>More specifically if the given value ends with a {@code '.'} character followed by a
     * sequence of {@code '0'} characters, then those characters are omitted. Otherwise this
     * method returns the text unchanged. This is a <q>all or nothing</q> method:
     * either the fractional part is completely removed, or either it is left unchanged.</p>
     *
     * <h4>Examples</h4>
     * This method returns {@code "4"} if the given value is {@code "4."}, {@code "4.0"} or
     * {@code "4.00"}, but returns {@code "4.10"} unchanged (including the trailing {@code '0'}
     * character) if the input is {@code "4.10"}.
     *
     * <h4>Use case</h4>
     * This method is useful before to {@linkplain Integer#parseInt(String) parse a number}
     * if that number should preferably be parsed as an integer before attempting to parse
     * it as a floating point number.
     *
     * @param  value  the value to trim if possible, or {@code null}.
     * @return the value without the trailing {@code ".0"} part (if any),
     *         or {@code null} if the given text was null.
     *
     * @see StringBuilders#trimFractionalPart(StringBuilder)
     */
    public static CharSequence trimFractionalPart(final CharSequence value) {
        if (value != null) {
            for (int i=value.length(); i>0;) {
                final int c = codePointBefore(value, i);
                i -= charCount(c);
                switch (c) {
                    case '0': continue;
                    case '.': return value.subSequence(0, i);
                    default : return value;
                }
            }
        }
        return value;
    }

    /**
     * Makes sure that the {@code text} string is not longer than {@code maxLength} characters.
     * If {@code text} is not longer, then it is returned unchanged. Otherwise this method returns
     * a copy of {@code text} with some characters substituted by the {@code "(…)"} string.
     *
     * <p>If the text needs to be shortened, then this method tries to apply the above-cited
     * substitution between two words. For example, the following text:</p>
     *
     * <blockquote>
     *   "This sentence given as an example is way too long to be included in a short name."
     * </blockquote>
     *
     * May be shortened to something like this:
     *
     * <blockquote>
     *   "This sentence given (…) in a short name."
     * </blockquote>
     *
     * @param  text       the sentence to reduce if it is too long, or {@code null}.
     * @param  maxLength  the maximum length allowed for {@code text}.
     * @return a sentence not longer than {@code maxLength}, or {@code null} if the given text was null.
     */
    public static CharSequence shortSentence(CharSequence text, final int maxLength) {
        ArgumentChecks.ensureStrictlyPositive("maxLength", maxLength);
        if (text != null) {
            final int length = text.length();
            int toRemove = length - maxLength;
            if (toRemove > 0) {
                toRemove += 5;          // Space needed for the " (…) " string.
                /*
                 * We will remove characters from 'lower' to 'upper' both exclusive. We try to
                 * adjust 'lower' and 'upper' in such a way that the first and last characters
                 * to be removed will be spaces or punctuation characters.
                 */
                int lower = length >>> 1;
                if (lower != 0 && isLowSurrogate(text.charAt(lower))) {
                    lower--;
                }
                int upper = lower;
                boolean forward = false;
                do { // To be run as long as we need to remove more characters.
                    int nc=0, type=UNASSIGNED;
                    forward = !forward;
searchWordBreak:    while (true) {
                        final int c;
                        if (forward) {
                            if ((upper += nc) == length) break;
                            c = codePointAt(text, upper);
                        } else {
                            if ((lower -= nc) == 0) break;
                            c = codePointBefore(text, lower);
                        }
                        nc = charCount(c);
                        if (isWhitespace(c)) {
                            if (type != UNASSIGNED) {
                                type = SPACE_SEPARATOR;
                            }
                        } else switch (type) {
                            // After we skipped white, then non-white, then white characters, stop.
                            case SPACE_SEPARATOR: {
                                break searchWordBreak;
                            }
                            // For the first non-white character, just remember its type.
                            // Arbitrarily use UPPERCASE_LETTER for any kind of identifier
                            // part (which include UPPERCASE_LETTER anyway).
                            case UNASSIGNED: {
                                type = isUnicodeIdentifierPart(c) ? UPPERCASE_LETTER : getType(c);
                                break;
                            }
                            // If we expected an identifier, stop at the first other char.
                            case UPPERCASE_LETTER: {
                                if (!isUnicodeIdentifierPart(c)) {
                                    break searchWordBreak;
                                }
                                break;
                            }
                            // For all other kind of character, break when the type change.
                            default: {
                                if (getType(c) != type) {
                                    break searchWordBreak;
                                }
                                break;
                            }
                        }
                        toRemove -= nc;
                    }
                } while (toRemove > 0);
                text = new StringBuilder(lower + (length-upper) + 5) // 5 is the length of " (…) "
                        .append(text, 0, lower).append(" (…) ").append(text, upper, length);
            }
        }
        return text;
    }

    /**
     * Given a string in upper cases (typically a Java constant), returns a string formatted
     * like an English sentence. This heuristic method performs the following steps:
     *
     * <ol>
     *   <li>Replace all occurrences of {@code '_'} by spaces.</li>
     *   <li>Converts all letters except the first one to lower case letters using
     *       {@link Character#toLowerCase(int)}. Note that this method does not use
     *       the {@link String#toLowerCase()} method. Consequently, the system locale
     *       is ignored. This method behaves as if the conversion were done in the
     *       {@linkplain java.util.Locale#ROOT root} locale.</li>
     * </ol>
     *
     * <p>Note that those heuristic rules may be modified in future SIS versions,
     * depending on the practical experience gained.</p>
     *
     * @param  identifier  the name of a Java constant, or {@code null}.
     * @return the identifier like an English sentence, or {@code null}
     *         if the given {@code identifier} argument was null.
     */
    public static CharSequence upperCaseToSentence(final CharSequence identifier) {
        if (identifier == null) {
            return null;
        }
        final var buffer = new StringBuilder(identifier.length());
        final int length = identifier.length();
        for (int i=0; i<length;) {
            int c = codePointAt(identifier, i);
            if (i != 0) {
                if (c == '_') {
                    c = ' ';
                } else {
                    c = toLowerCase(c);
                }
            }
            buffer.appendCodePoint(c);
            i += charCount(c);
        }
        return buffer;
    }

    /**
     * Given a string in camel cases (typically an identifier), returns a string formatted
     * like an English sentence. This heuristic method performs the following steps:
     *
     * <ol>
     *   <li>Invoke {@link #camelCaseToWords(CharSequence, boolean)}, which separate the words
     *     on the basis of character case. For example, {@code "transferFunctionType"} become
     *     <q>transfer function type</q>. This works fine for ISO 19115 identifiers.</li>
     *
     *   <li>Next replace all occurrence of {@code '_'} by spaces in order to take in account
     *     another common naming convention, which uses {@code '_'} as a word separator. This
     *     convention is used by netCDF attributes like {@code "project_name"}.</li>
     *
     *   <li>Finally ensure that the first character is upper-case.</li>
     * </ol>
     *
     * <h4>Exception to the above rules</h4>
     * If the given identifier contains only upper-case letters, digits and the {@code '_'} character,
     * then the identifier is returned "as is" except for the {@code '_'} characters which are replaced by {@code '-'}.
     * This work well for identifiers like {@code "UTF-8"} or {@code "ISO-LATIN-1"} for instance.
     *
     * <p>Note that those heuristic rules may be modified in future SIS versions,
     * depending on the practical experience gained.</p>
     *
     * @param  identifier  an identifier with no space, words begin with an upper-case character, or {@code null}.
     * @return the identifier with spaces inserted after what looks like words, or {@code null}
     *         if the given {@code identifier} argument was null.
     */
    public static CharSequence camelCaseToSentence(final CharSequence identifier) {
        if (identifier == null) {
            return null;
        }
        final StringBuilder buffer;
        if (isCode(identifier)) {
            if (identifier instanceof String) {
                return ((String) identifier).replace('_', '-');
            }
            buffer = new StringBuilder(identifier);
            StringBuilders.replace(buffer, '_', '-');
        } else {
            buffer = (StringBuilder) camelCaseToWords(identifier, true);
            final int length = buffer.length();
            if (length != 0) {
                StringBuilders.replace(buffer, '_', ' ');
                final int c = buffer.codePointAt(0);
                final int up = toUpperCase(c);
                if (c != up) {
                    StringBuilders.replace(buffer, 0, charCount(c), toChars(up));
                }
            }
        }
        return buffer;
    }

    /**
     * Given a string in camel cases, returns a string with the same words separated by spaces.
     * A word begins with a upper-case character following a lower-case character. For example
     * if the given string is {@code "PixelInterleavedSampleModel"}, then this method returns
     * <q>Pixel Interleaved Sample Model</q> or <q>Pixel interleaved sample model</q>
     * depending on the value of the {@code toLowerCase} argument.
     *
     * <p>If {@code toLowerCase} is {@code false}, then this method inserts spaces but does not change
     * the case of characters. If {@code toLowerCase} is {@code true}, then this method changes
     * {@linkplain Character#toLowerCase(int) to lower case} the first character after each spaces
     * inserted by this method (note that this intentionally exclude the very first character in
     * the given string), except if the second character {@linkplain Character#isUpperCase(int)
     * is upper case}, in which case the word is assumed an acronym.</p>
     *
     * <p>The given string is usually a programmatic identifier like a class name or a method name.</p>
     *
     * @param  identifier   an identifier with no space, words begin with an upper-case character.
     * @param  toLowerCase  {@code true} for changing the first character of words to lower case,
     *         except for the first word and acronyms.
     * @return the identifier with spaces inserted after what looks like words, or {@code null}
     *         if the given {@code identifier} argument was null.
     */
    public static CharSequence camelCaseToWords(final CharSequence identifier, final boolean toLowerCase) {
        if (identifier == null) {
            return null;
        }
        /*
         * Implementation note: the 'camelCaseToSentence' method needs
         * this method to unconditionally returns a new StringBuilder.
         */
        final int length = identifier.length();
        final var buffer = new StringBuilder(length + 8);
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
                        StringBuilders.replace(buffer, pos, pos + charCount(c), toChars(low));
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
            if (isWhitespace(cp)) {
                buffer.setLength(lg - charCount(cp));
            }
        }
        return buffer;
    }

    /**
     * Creates an acronym from the given text. This method returns a string containing the first character of each word,
     * where the words are separated by the camel case convention, the {@code '_'} character, or any character which is
     * not a {@linkplain Character#isUnicodeIdentifierPart(int) Unicode identifier part} (including spaces).
     *
     * <p>An exception to the above rule happens if the given text is a Unicode identifier without the {@code '_'}
     * character, and every characters are upper case. In such case the text is returned unchanged on the assumption
     * that it is already an acronym.</p>
     *
     * <p><b>Examples:</b> given {@code "northEast"}, this method returns {@code "NE"}.
     * Given {@code "Open Geospatial Consortium"}, this method returns {@code "OGC"}.</p>
     *
     * @param  text  the text for which to create an acronym, or {@code null}.
     * @return the acronym, or {@code null} if the given text was null.
     */
    public static CharSequence camelCaseToAcronym(CharSequence text) {
        text = trimWhitespaces(text);
        if (text != null && !isAcronym(text)) {
            final int length = text.length();
            final var buffer = new StringBuilder(8);        // Acronyms are usually short.
            boolean wantChar = true;
            for (int i=0; i<length;) {
                final int c = codePointAt(text, i);
                if (wantChar) {
                    if (isUnicodeIdentifierStart(c)) {
                        buffer.appendCodePoint(c);
                        wantChar = false;
                    }
                } else if (!isUnicodeIdentifierPart(c) || c == '_') {
                    wantChar = true;
                } else if (Character.isUpperCase(c)) {
                    // Test for mixed-case (e.g. "northEast").
                    // Note that i is guaranteed to be greater than 0 here.
                    if (!Character.isUpperCase(codePointBefore(text, i))) {
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
                if (isUpperCase(buffer, 1, acrlg, true)) {
                    final int c = buffer.codePointAt(0);
                    final int up = toUpperCase(c);
                    if (c != up) {
                        StringBuilders.replace(buffer, 0, charCount(c), toChars(up));
                    }
                }
                if (!equals(text, buffer)) {
                    text = buffer;
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
     * If any of the given arguments is {@code null}, this method returns {@code false}.
     *
     * <h4>Example</h4>
     * Given the {@code "Open Geospatial Consortium"} words, the following strings are recognized as acronyms:
     * {@code "OGC"}, {@code "ogc"}, {@code "O.G.C."}, {@code "OpGeoCon"}.
     *
     * @param  acronym  a possible acronym of the sequence of words, or {@code null}.
     * @param  words    the sequence of words, or {@code null}.
     * @return {@code true} if the first string is an acronym of the second one.
     */
    public static boolean isAcronymForWords(final CharSequence acronym, final CharSequence words) {
        final int lga = length(acronym);
        int ia=0, ca;
        do {
            if (ia >= lga) return false;
            ca = codePointAt(acronym, ia);
            ia += charCount(ca);
        } while (!isLetterOrDigit(ca));
        final int lgc = length(words);
        int ic=0, cc;
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
         * Now that we have processed all acronym letters, the complete name cannot have
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
     *
     * <p>This method is used for identifying character strings that are likely to be code
     * like {@code "UTF-8"} or {@code "ISO-LATIN-1"}.</p>
     *
     * @see #isUnicodeIdentifier(CharSequence)
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
     * Returns {@code true} if the given text is presumed to be an acronym. Acronyms are presumed
     * to be valid Unicode identifiers in all upper-case letters and without the {@code '_'} character.
     *
     * @see #camelCaseToAcronym(CharSequence)
     */
    private static boolean isAcronym(final CharSequence text) {
        return isUpperCase(text) && indexOf(text, '_', 0, text.length()) < 0 && isUnicodeIdentifier(text);
    }

    /**
     * Returns {@code true} if the given identifier is a legal Unicode identifier.
     * This method returns {@code true} if the identifier length is greater than zero,
     * the first character is a {@linkplain Character#isUnicodeIdentifierStart(int)
     * Unicode identifier start} and all remaining characters (if any) are
     * {@linkplain Character#isUnicodeIdentifierPart(int) Unicode identifier parts}.
     *
     * <h4>Relationship with legal XML identifiers</h4>
     * Most legal Unicode identifiers are also legal XML identifiers, but the converse is not true.
     * The most noticeable differences are the ‘{@code :}’, ‘{@code -}’ and ‘{@code .}’ characters,
     * which are legal in XML identifiers but not in Unicode.
     *
     * <table class="sis">
     *   <caption>Characters legal in one set but not in the other</caption>
     *   <tr><th colspan="2">Not legal in Unicode</th>    <th class="sep" colspan="2">Not legal in XML</th></tr>
     *   <tr><td>{@code :}</td><td>(colon)</td>           <td class="sep">{@code µ}</td><td>(micro sign)</td></tr>
     *   <tr><td>{@code -}</td><td>(hyphen or minus)</td> <td class="sep">{@code ª}</td><td>(feminine ordinal indicator)</td></tr>
     *   <tr><td>{@code .}</td><td>(dot)</td>             <td class="sep">{@code º}</td><td>(masculine ordinal indicator)</td></tr>
     *   <tr><td>{@code ·}</td><td>(middle dot)</td>      <td class="sep">{@code ⁔}</td><td>(inverted undertie)</td></tr>
     *   <tr>
     *     <td colspan="2">Many punctuation, symbols, <i>etc</i>.</td>
     *     <td colspan="2" class="sep">{@linkplain Character#isIdentifierIgnorable(int) Identifier ignorable} characters.</td>
     *   </tr>
     * </table>
     *
     * Note that the ‘{@code _}’ (underscore) character is legal according both Unicode and XML, while spaces,
     * ‘{@code !}’, ‘{@code #}’, ‘{@code *}’, ‘{@code /}’, ‘{@code ?}’ and most other punctuation characters are not.
     *
     * <h4>Usage in Apache SIS</h4>
     * In its handling of {@linkplain org.apache.sis.referencing.ImmutableIdentifier identifiers}, Apache SIS favors
     * Unicode identifiers without {@linkplain Character#isIdentifierIgnorable(int) ignorable} characters since those
     * identifiers are legal XML identifiers except for the above-cited rarely used characters. As a side effect,
     * this policy excludes ‘{@code :}’, ‘{@code -}’ and ‘{@code .}’ which would normally be legal XML identifiers.
     * But since those characters could easily be confused with
     * {@linkplain org.apache.sis.util.iso.DefaultNameSpace#DEFAULT_SEPARATOR namespace separators},
     * this exclusion is considered desirable.
     *
     * @param  identifier  the character sequence to test, or {@code null}.
     * @return {@code true} if the given character sequence is a legal Unicode identifier.
     *
     * @see org.apache.sis.referencing.ImmutableIdentifier
     * @see org.apache.sis.metadata.iso.citation.Citations#toCodeSpace(Citation)
     * @see org.apache.sis.referencing.IdentifiedObjects#getSimpleNameOrIdentifier(IdentifiedObject)
     */
    public static boolean isUnicodeIdentifier(final CharSequence identifier) {
        final int length = length(identifier);
        if (length == 0) {
            return false;
        }
        int c = codePointAt(identifier, 0);
        if (!isUnicodeIdentifierStart(c)) {
            return false;
        }
        for (int i=0; (i += charCount(c)) < length;) {
            c = codePointAt(identifier, i);
            if (!isUnicodeIdentifierPart(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if the given text is non-null, contains at least one upper-case character and
     * no lower-case character. Space and punctuation are ignored.
     *
     * @param  text  the character sequence to test (may be {@code null}).
     * @return {@code true} if non-null, contains at least one upper-case character and no lower-case character.
     *
     * @see String#toUpperCase()
     *
     * @since 0.7
     */
    public static boolean isUpperCase(final CharSequence text) {
        return isUpperCase(text, 0, length(text), false);
    }

    /**
     * Returns {@code true} if the given sub-sequence is non-null, contains at least one upper-case character and
     * no lower-case character. Space and punctuation are ignored.
     *
     * @param  text          the character sequence to test.
     * @param  lower         index of the first character to check, inclusive.
     * @param  upper         index of the last character to check, exclusive.
     * @param  hasUpperCase  {@code true} if this method should behave as if the given text already had
     *         at least one upper-case character (not necessarily in the portion given by the indices).
     * @return {@code true} if contains at least one upper-case character and no lower-case character.
     */
    private static boolean isUpperCase(final CharSequence text, int lower, final int upper, boolean hasUpperCase) {
        while (lower < upper) {
            final int c = codePointAt(text, lower);
            if (Character.isLowerCase(c)) {
                return false;
            }
            if (!hasUpperCase) {
                hasUpperCase = Character.isUpperCase(c);
            }
            lower += charCount(c);
        }
        return hasUpperCase;
    }

    /**
     * Returns {@code true} if the given texts are equal, optionally ignoring case and filtered-out characters.
     * This method is sometimes used for comparing identifiers in a lenient way.
     *
     * <p><b>Example:</b> the following call compares the two strings ignoring case and any
     * characters which are not {@linkplain Character#isLetterOrDigit(int) letter or digit}.
     * In particular, spaces and punctuation characters like {@code '_'} and {@code '-'} are
     * ignored:</p>
     *
     * {@snippet lang="java" :
     *     assert equalsFiltered("WGS84", "WGS_84", Characters.Filter.LETTERS_AND_DIGITS, true) == true;
     *     }
     *
     * @param  s1          the first characters sequence to compare, or {@code null}.
     * @param  s2          the second characters sequence to compare, or {@code null}.
     * @param  filter      the subset of characters to compare, or {@code null} for comparing all characters.
     * @param  ignoreCase  {@code true} for ignoring cases, or {@code false} for requiring exact match.
     * @return {@code true} if both arguments are {@code null} or if the two given texts are equal,
     *         optionally ignoring case and filtered-out characters.
     */
    public static boolean equalsFiltered(final CharSequence s1, final CharSequence s2,
            final Characters.Filter filter, final boolean ignoreCase)
    {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }
        if (filter == null) {
            return ignoreCase ? equalsIgnoreCase(s1, s2) : equals(s1, s2);
        }
        final int lg1 = s1.length();
        final int lg2 = s2.length();
        int i1 = 0, i2 = 0;
        while (i1 < lg1) {
            int c1 = codePointAt(s1, i1);
            final int n = charCount(c1);
            if (filter.contains(c1)) {
                int c2;                     // Fetch the next significant character from the second string.
                do {
                    if (i2 >= lg2) {
                        return false;       // The first string has more significant characters than expected.
                    }
                    c2 = codePointAt(s2, i2);
                    i2 += charCount(c2);
                } while (!filter.contains(c2));

                // Compare the characters in the same way as String.equalsIgnoreCase(String).
                if (c1 != c2 && !(ignoreCase && equalsIgnoreCase(c1, c2))) {
                    return false;
                }
            }
            i1 += n;
        }
        while (i2 < lg2) {
            final int s = codePointAt(s2, i2);
            if (filter.contains(s)) {
                return false;               // The first string has less significant characters than expected.
            }
            i2 += charCount(s);
        }
        return true;
    }

    /**
     * Returns {@code true} if the given code points are equal, ignoring case.
     * This method implements the same comparison algorithm as String#equalsIgnoreCase(String).
     *
     * <p>This method does not verify if {@code c1 == c2}. This check should have been done
     * by the caller, since the caller code is a more optimal place for this check.</p>
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
     * Returns {@code true} if the two given texts are equal, ignoring case.
     * This method is similar to {@link String#equalsIgnoreCase(String)}, except
     * it works on arbitrary character sequences and compares <i>code points</i>
     * instead of characters.
     *
     * @param  s1  the first string to compare, or {@code null}.
     * @param  s2  the second string to compare, or {@code null}.
     * @return {@code true} if the two given texts are equal, ignoring case,
     *         or if both arguments are {@code null}.
     *
     * @see String#equalsIgnoreCase(String)
     */
    public static boolean equalsIgnoreCase(final CharSequence s1, final CharSequence s2) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }
        // Do not check for String cases. We do not want to delegate to String.equalsIgnoreCase
        // because we compare code points while String.equalsIgnoreCase compares characters.
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
     * Returns {@code true} if the two given texts are equal. This method delegates to
     * {@link String#contentEquals(CharSequence)} if possible. This method never invoke
     * {@link CharSequence#toString()} in order to avoid a potentially large copy of data.
     *
     * @param  s1  the first string to compare, or {@code null}.
     * @param  s2  the second string to compare, or {@code null}.
     * @return {@code true} if the two given texts are equal, or if both arguments are {@code null}.
     *
     * @see String#contentEquals(CharSequence)
     */
    public static boolean equals(final CharSequence s1, final CharSequence s2) {
        if (s1 == s2) {
            return true;
        }
        if (s1 != null && s2 != null) {
            if (s1 instanceof String) return ((String) s1).contentEquals(s2);
            if (s2 instanceof String) return ((String) s2).contentEquals(s1);
            final int length = s1.length();
            if (s2.length() == length) {
                for (int i=0; i<length; i++) {
                    if (s1.charAt(i) != s2.charAt(i)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the given text at the given offset contains the given part,
     * in a case-sensitive comparison. This method is equivalent to the following code,
     * except that this method works on arbitrary {@link CharSequence} objects instead of
     * {@link String}s only:
     *
     * {@snippet lang="java" :
     *     return text.regionMatches(offset, part, 0, part.length());
     *     }
     *
     * This method does not thrown {@code IndexOutOfBoundsException}. Instead, if
     * {@code fromIndex < 0} or {@code fromIndex + part.length() > text.length()},
     * then this method returns {@code false}.
     *
     * @param  text       the character sequence for which to tests for the presence of {@code part}.
     * @param  fromIndex  the offset in {@code text} where to test for the presence of {@code part}.
     * @param  part       the part which may be present in {@code text}.
     * @return {@code true} if {@code text} contains {@code part} at the given {@code offset}.
     * @throws NullPointerException if any of the arguments is null.
     *
     * @see String#regionMatches(int, String, int, int)
     */
    public static boolean regionMatches(final CharSequence text, final int fromIndex, final CharSequence part) {
        if (text instanceof String && part instanceof String) {
            // It is okay to delegate to String implementation since we do not ignore cases.
            return ((String) text).startsWith((String) part, fromIndex);
        }
        final int length;
        if (fromIndex < 0 || fromIndex + (length = part.length()) > text.length()) {
            return false;
        }
        for (int i=0; i<length; i++) {
            // No need to use the code point API here, since we are looking for exact matches.
            if (text.charAt(fromIndex + i) != part.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if the given text at the given offset contains the given part,
     * optionally in a case-insensitive way. This method is equivalent to the following code,
     * except that this method works on arbitrary {@link CharSequence} objects instead of
     * {@link String}s only:
     *
     * {@snippet lang="java" :
     *     return text.regionMatches(ignoreCase, offset, part, 0, part.length());
     *     }
     *
     * This method does not thrown {@code IndexOutOfBoundsException}. Instead, if
     * {@code fromIndex < 0} or {@code fromIndex + part.length() > text.length()},
     * then this method returns {@code false}.
     *
     * @param  text        the character sequence for which to tests for the presence of {@code part}.
     * @param  fromIndex   the offset in {@code text} where to test for the presence of {@code part}.
     * @param  part        the part which may be present in {@code text}.
     * @param  ignoreCase  {@code true} if the case should be ignored.
     * @return {@code true} if {@code text} contains {@code part} at the given {@code offset}.
     * @throws NullPointerException if any of the arguments is null.
     *
     * @see String#regionMatches(boolean, int, String, int, int)
     *
     * @since 0.4
     */
    public static boolean regionMatches(final CharSequence text, int fromIndex, final CharSequence part, final boolean ignoreCase) {
        if (!ignoreCase) {
            return regionMatches(text, fromIndex, part);
        }
        // Do not check for String cases. We do not want to delegate to String.regionMatches
        // because we compare code points while String.regionMatches(…) compares characters.
        final int limit  = text.length();
        final int length = part.length();
        if (fromIndex < 0) {                // Not checked before because we want NullPointerException if an argument is null.
            return false;
        }
        for (int i=0; i<length;) {
            if (fromIndex >= limit) {
                return false;
            }
            final int c1 = codePointAt(part, i);
            final int c2 = codePointAt(text, fromIndex);
            if (c1 != c2 && !equalsIgnoreCase(c1, c2)) {
                return false;
            }
            fromIndex += charCount(c2);
            i += charCount(c1);
        }
        return true;
    }

    /**
     * Returns {@code true} if the given character sequence starts with the given prefix.
     *
     * @param  text         the characters sequence to test.
     * @param  prefix       the expected prefix.
     * @param  ignoreCase   {@code true} if the case should be ignored.
     * @return {@code true} if the given sequence starts with the given prefix.
     * @throws NullPointerException if any of the arguments is null.
     */
    public static boolean startsWith(final CharSequence text, final CharSequence prefix, final boolean ignoreCase) {
        return regionMatches(text, 0, prefix, ignoreCase);
    }

    /**
     * Returns {@code true} if the given character sequence ends with the given suffix.
     *
     * @param  text         the characters sequence to test.
     * @param  suffix       the expected suffix.
     * @param  ignoreCase   {@code true} if the case should be ignored.
     * @return {@code true} if the given sequence ends with the given suffix.
     * @throws NullPointerException if any of the arguments is null.
     */
    public static boolean endsWith(final CharSequence text, final CharSequence suffix, final boolean ignoreCase) {
        int is = text.length();
        int ip = suffix.length();
        while (ip > 0) {
            if (is <= 0) {
                return false;
            }
            final int cs = codePointBefore(text, is);
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
     * Returns the longest sequence of characters which is found at the beginning of the two given texts.
     * If one of those texts is {@code null}, then the other text is returned.
     * If there is no common prefix, then this method returns an empty string.
     *
     * @param  s1  the first text,  or {@code null}.
     * @param  s2  the second text, or {@code null}.
     * @return the common prefix of both texts (may be empty), or {@code null} if both texts are null.
     */
    public static CharSequence commonPrefix(final CharSequence s1, final CharSequence s2) {
        if (s1 == null) return s2;
        if (s2 == null) return s1;
        final CharSequence shortest;
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
        return shortest.subSequence(0, i);
    }

    /**
     * Returns the longest sequence of characters which is found at the end of the two given texts.
     * If one of those texts is {@code null}, then the other text is returned.
     * If there is no common suffix, then this method returns an empty string.
     *
     * @param  s1  the first text,  or {@code null}.
     * @param  s2  the second text, or {@code null}.
     * @return the common suffix of both texts (may be empty), or {@code null} if both texts are null.
     */
    public static CharSequence commonSuffix(final CharSequence s1, final CharSequence s2) {
        if (s1 == null) return s2;
        if (s2 == null) return s1;
        final CharSequence shortest;
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
        return shortest.subSequence(length - i, shortest.length());
    }

    /**
     * Returns the words found at the beginning and end of both texts.
     * The returned string is the concatenation of the {@linkplain #commonPrefix common prefix}
     * with the {@linkplain #commonSuffix common suffix}, with prefix and suffix eventually made
     * shorter for avoiding to cut in the middle of a word.
     *
     * <p>The purpose of this method is to create a global identifier from a list of component identifiers.
     * The latter are often eastward and northward components of a vector, in which case this method provides
     * an identifier for the vector as a whole.</p>
     *
     * <p>If one of the given texts is {@code null}, then the other text is returned.
     * If there are no common words, then this method returns an empty string.</p>
     *
     * <h4>Example</h4>
     * Given the following inputs:
     * <ul>
     *   <li>{@code "baroclinic_eastward_velocity"}</li>
     *   <li>{@code "baroclinic_northward_velocity"}</li>
     * </ul>
     * This method returns {@code "baroclinic_velocity"}. Note that the {@code "ward"} characters
     * are a common suffix of both texts but nevertheless omitted because they cut a word.
     *
     * <h4>Possible future evolution</h4>
     * Current implementation searches only for a common prefix and a common suffix, ignoring any common words
     * that may appear in the middle of the strings. A character is considered the beginning of a word if it is
     * {@linkplain Character#isLetterOrDigit(int) a letter or digit} which is not preceded by another letter or
     * digit (as leading "s" and "c" in "snake_case"), or if it is an {@linkplain Character#isUpperCase(int)
     * upper case} letter preceded by a {@linkplain Character#isLowerCase(int) lower case} letter or no letter
     * (as both "C" in "CamelCase").
     *
     * @param  s1  the first text,  or {@code null}.
     * @param  s2  the second text, or {@code null}.
     * @return the common suffix of both texts (may be empty), or {@code null} if both texts are null.
     *
     * @since 1.1
     */
    public static CharSequence commonWords(final CharSequence s1, final CharSequence s2) {
        final int lg1 = length(s1);
        final int lg2 = length(s2);
        final int shortestLength  = Math.min(lg1, lg2);   // 0 if s1 or s2 is null, in which case prefix and suffix will have the other value.
        final CharSequence prefix = commonPrefix(s1, s2); int prefixLength = length(prefix); if (prefixLength >= shortestLength) return prefix;
        final CharSequence suffix = commonSuffix(s1, s2); int suffixLength = length(suffix); if (suffixLength >= shortestLength) return suffix;
        final int length = prefixLength + suffixLength;
        if (length >= lg1) return s1;                     // Check if one of the strings is already equal to prefix + suffix.
        if (length >= lg2) return s2;
        /*
         * At this point `s1` and `s2` contain at least one character between the prefix and the suffix.
         * If the prefix or the suffix seems to stop in the middle of a word, skip the remaining of that word.
         * For example if `s1` and `s2` are "eastward_velocity" and "northward_velocity", the common suffix is
         * "ward_velocity" but we want to retain only "velocity".
         *
         * The first condition below (before the loop) checks the character after the common prefix (for example "e"
         * in "baroclinic_eastward_velocity" if the prefix is "baroclinic_"). The intent is to handle the case where
         * the word separator is not the same (e.g. "baroclinic_eastward_velocity" and "baroclinic northward velocity",
         * in which case the '_' or ' ' character would not appear in the prefix).
         */
        if (!isWordBoundary(s1, prefixLength, codePointAt(s1, prefixLength)) &&
            !isWordBoundary(s2, prefixLength, codePointAt(s2, prefixLength)))
        {
            while (prefixLength > 0) {
                final int c = codePointBefore(prefix, prefixLength);
                final int n = charCount(c);
                prefixLength -= n;
                if (isWordBoundary(prefix, prefixLength, c)) {
                    if (!isLetterOrDigit(c)) prefixLength += n;                     // Keep separator character.
                    break;
                }
            }
        }
        /*
         * Same process as for the prefix above. The condition before the loop checks the character before suffix
         * for the same reason as above, but using only `isLetterOrDigit` ignoring camel-case. The reason is that
         * if the character before was a word separator according camel-case convention (i.e. an upper-case letter),
         * we would need to include it in the common suffix.
         */
        int suffixStart  = 0;
        if (isLetterOrDigit(codePointBefore(s1, lg1 - suffixLength)) &&
            isLetterOrDigit(codePointBefore(s2, lg2 - suffixLength)))
        {
            while (suffixStart < suffixLength) {
                final int c = codePointAt(suffix, suffixStart);
                if (isWordBoundary(suffix, suffixStart, c)) break;
                suffixStart += charCount(c);
            }
        }
        /*
         * At this point we got the final prefix and suffix to use. If the prefix or suffix is empty,
         * trim whitespaces or '_' character. For example if the suffix is "_velocity" and no prefix,
         * return "velocity" without leading "_" character.
         */
        if (prefixLength == 0) {
            while (suffixStart < suffixLength) {
                final int c = codePointAt(suffix, suffixStart);
                if (isLetterOrDigit(c)) {
                    return suffix.subSequence(suffixStart, suffixLength);   // Skip leading ignorable characters in suffix.
                }
                suffixStart += charCount(c);
            }
            return "";
        }
        if (suffixStart >= suffixLength) {
            while (prefixLength > 0) {
                final int c = codePointBefore(prefix, prefixLength);
                if (isLetterOrDigit(c)) {
                    return prefix.subSequence(0, prefixLength);             // Skip trailing ignorable characters in prefix.
                }
                prefixLength -= charCount(c);
            }
            return "";
        }
        /*
         * All special cases have been examined. Return the concatenation of (possibly shortened)
         * common prefix and suffix.
         */
        final var buffer = new StringBuilder(prefixLength + suffixLength).append(prefix);
        final int c1 = codePointBefore(prefix, prefixLength);
        final int c2 = codePointAt(suffix, suffixStart);
        if (isLetterOrDigit(c1) && isLetterOrDigit(c2)) {
            if (!Character.isUpperCase(c2) || !isLowerCase(c1)) {
                buffer.append(' ');             // Keep a separator between two words (except if CamelCase is used).
            }
        } else if (c1 == c2) {
            suffixStart += charCount(c2);       // Avoid repeating '_' in e.g. "baroclinic_<removed>_velocity".
        }
        return buffer.append(suffix, suffixStart, suffixLength).toString();
    }

    /**
     * Returns {@code true} if the character {@code c} is the beginning of a word or a non-word character.
     * For example, this method returns {@code true} if {@code c} is {@code '_'} in {@code "snake_case"} or
     * {@code "C"} in {@code "CamelCase"}.
     *
     * @param  s  the character sequence from which the {@code c} character has been obtained.
     * @param  i  the index in {@code s} where the {@code c} character has been obtained.
     * @param  c  the code point in {@code s} as index {@code i}.
     * @return whether the given character is the beginning of a word or a non-word character.
     */
    private static boolean isWordBoundary(final CharSequence s, final int i, final int c) {
        if (!isLetterOrDigit(c)) return true;
        if (!Character.isUpperCase(c)) return false;
        return (i <= 0 || isLowerCase(codePointBefore(s, i)));
    }

    /**
     * Returns the token starting at the given offset in the given text. For the purpose of this
     * method, a "token" is any sequence of consecutive characters of the same type, as defined
     * below.
     *
     * <p>Let define <var>c</var> as the first non-blank character located at an index equals or
     * greater than the given offset. Then the characters that are considered of the same type
     * are:</p>
     *
     * <ul>
     *   <li>If <var>c</var> is a
     *       {@linkplain Character#isUnicodeIdentifierStart(int) Unicode identifier start},
     *       then any following characters that are
     *       {@linkplain Character#isUnicodeIdentifierPart(int) Unicode identifier part}.</li>
     *   <li>If <var>c</var> is a dash punctuation of a connector punctuation, then all following punctuation
     *       characters of the same type followed by all characters that are Unicode identifier part.</li>
     *   <li>Otherwise any character for which {@link Character#getType(int)} returns
     *       the same value as for <var>c</var>.</li>
     * </ul>
     *
     * @param  text       the text for which to get the token.
     * @param  fromIndex  index of the first character to consider in the given text.
     * @return a sub-sequence of {@code text} starting at the given offset, or an empty string
     *         if there are no non-blank character at or after the given offset.
     * @throws NullPointerException if the {@code text} argument is null.
     */
    public static CharSequence token(final CharSequence text, int fromIndex) {
        final int length = text.length();
        int upper = fromIndex;
        /*
         * Skip whitespaces. At the end of this loop,
         * 'c' will be the first non-blank character.
         */
        int c;
        do {
            if (upper >= length) return "";
            c = codePointAt(text, upper);
            fromIndex = upper;
            upper += charCount(c);
        }
        while (isWhitespace(c));
        /*
         * Advance over all characters "of the same type".
         * If the character are dash or connector punctuation, also include the next characters.
         */
        if (isUnicodeIdentifierStart(c)) {
            while (upper<length && isUnicodeIdentifierPart(c = codePointAt(text, upper))) {
                upper += charCount(c);
            }
        } else {
            final int type = getType(codePointAt(text, fromIndex));
            while (upper<length && getType(c = codePointAt(text, upper)) == type) {
                upper += charCount(c);
            }
            if (type == Character.DASH_PUNCTUATION || type == Character.CONNECTOR_PUNCTUATION) {
                while (upper<length && isUnicodeIdentifierPart(c = codePointAt(text, upper))) {
                    upper += charCount(c);
                }
            }
        }
        return text.subSequence(fromIndex, upper);
    }

    /**
     * Replaces all occurrences of a given string in the given character sequence. If no occurrence of
     * {@code toSearch} is found in the given text or if {@code toSearch} is equal to {@code replaceBy},
     * then this method returns the {@code text} unchanged.
     * Otherwise this method returns a new character sequence with all occurrences replaced by {@code replaceBy}.
     *
     * <p>This method is similar to {@link String#replace(CharSequence, CharSequence)} except that is accepts
     * arbitrary {@code CharSequence} objects. As of Java 10, another difference is that this method does not
     * create a new {@code String} if {@code toSearch} is equal to {@code replaceBy}.</p>
     *
     * @param  text       the character sequence in which to perform the replacements, or {@code null}.
     * @param  toSearch   the string to replace.
     * @param  replaceBy  the replacement for the searched string.
     * @return the given text with replacements applied, or {@code text} if no replacement has been applied,
     *         or {@code null} if the given text was null
     *
     * @see String#replace(char, char)
     * @see StringBuilders#replace(StringBuilder, String, String)
     * @see String#replace(CharSequence, CharSequence)
     *
     * @since 0.4
     */
    public static CharSequence replace(final CharSequence text, final CharSequence toSearch, final CharSequence replaceBy) {
        ArgumentChecks.ensureNonEmpty("toSearch",  toSearch);
        ArgumentChecks.ensureNonNull ("replaceBy", replaceBy);
        if (text != null && !toSearch.equals(replaceBy)) {
            if (text instanceof String) {
                return ((String) text).replace(toSearch, replaceBy);
            }
            final int length = text.length();
            int i = indexOf(text, toSearch, 0, length);
            if (i >= 0) {
                int p = 0;
                final int sl = toSearch.length();
                final var buffer = new StringBuilder(length + (replaceBy.length() - sl));
                do {
                    buffer.append(text, p, i).append(replaceBy);
                    i = indexOf(text, toSearch, p = i + sl, length);
                } while (i >= 0);
                return buffer.append(text, p, length);
            }
        }
        return text;
    }

    /**
     * Copies a sequence of characters in the given {@code char[]} array.
     *
     * @param src        the characters sequence from which to copy characters.
     * @param srcOffset  index of the first character from {@code src} to copy.
     * @param dst        the array where to copy the characters.
     * @param dstOffset  index where to write the first character in {@code dst}.
     * @param length     number of characters to copy.
     *
     * @see String#getChars(int, int, char[], int)
     * @see StringBuilder#getChars(int, int, char[], int)
     * @see StringBuffer#getChars(int, int, char[], int)
     * @see CharBuffer#get(char[], int, int)
     * @see javax.swing.text.Segment#array
     */
    public static void copyChars(final CharSequence src, int srcOffset,
                                 final char[] dst, int dstOffset, int length)
    {
        ArgumentChecks.ensurePositive("length", length);
        if (src instanceof String) {
            ((String) src).getChars(srcOffset, srcOffset + length, dst, dstOffset);
        } else if (src instanceof StringBuilder) {
            ((StringBuilder) src).getChars(srcOffset, srcOffset + length, dst, dstOffset);
        } else if (src instanceof StringBuffer) {
            ((StringBuffer) src).getChars(srcOffset, srcOffset + length, dst, dstOffset);
        } else if (src instanceof CharBuffer) {
            ((CharBuffer) src).subSequence(srcOffset, srcOffset + length).get(dst, dstOffset, length);
        } else {
            /*
             * Another candidate could be `javax.swing.text.Segment`, but it
             * is probably not worth to introduce a Swing dependency for it.
             */
            while (length != 0) {
                dst[dstOffset++] = src.charAt(srcOffset++);
                length--;
            }
        }
    }
}
