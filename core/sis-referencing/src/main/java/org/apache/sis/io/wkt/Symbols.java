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
package org.apache.sis.io.wkt;

import java.util.Arrays;
import java.util.Locale;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.NumberFormat;
import org.apache.sis.util.Localized;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.*;


/**
 * The set of symbols to use for <cite>Well Known Text</cite> (WKT) parsing and formatting.
 * Newly created {@code Symbols} instances use the following defaults:
 *
 * <ul>
 *   <li>An English locale for {@linkplain java.text.DecimalFormatSymbols decimal format symbols}.</li>
 *   <li>Square brackets, as in {@code DATUM["WGS84"]}. An alternative allowed by the WKT
 *       specification is curly brackets as in {@code DATUM("WGS84")}.</li>
 *   <li>English quotation mark ({@code '"'}). SIS also accepts {@code '“'} opening quote and {@code '”'}
 *       closing quote for more readable {@link String} constants in Java code, but the later are not legal WKT.</li>
 *   <li>Coma separator followed by a space ({@code ", "}).</li>
 * </ul>
 *
 * {@section Relationship between <code>Symbols</code> locale and <code>WKTFormat</code> locale}
 * The {@link WKTFormat#getLocale()} property specifies the language to use when formatting
 * {@link org.opengis.util.InternationalString} instances. This can be set to any value.
 * On the contrary, the {@code Locale} property of this {@code Symbols} class controls
 * the decimal format symbols and is very rarely set to an other locale than {@link Locale#ROOT}.
 *
 * @author  Martin Desruisseaux (IRD)
 * @since   0.4 (derived from geotk-2.1)
 * @version 0.4
 * @module
 *
 * @see WKTFormat#getSymbols()
 * @see WKTFormat#setSymbols(Symbols)
 */
public class Symbols implements Localized, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1730166945430878916L;

    /**
     * A set of symbols with values between square brackets, like {@code DATUM["WGS84"]}.
     * This is the most frequently used WKT format.
     */
    public static final Symbols SQUARE_BRACKETS = new Immutable(
            new int[] {'[', ']', '(', ')'}, new int[] {'"', '"', '“', '”'});

    /**
     * A set of symbols with values between parentheses, like {@code DATUM("WGS84")}.
     * This is a less frequently used but legal WKT format.
     */
    public static final Symbols CURLY_BRACKETS = new Immutable(
            new int[] {'(', ')', '[', ']'}, SQUARE_BRACKETS.quotes);

    /**
     * The default set of symbols, as documented in the class javadoc.
     * This is currently set to {@link #SQUARE_BRACKETS}.
     */
    public static final Symbols DEFAULT = SQUARE_BRACKETS;

    /**
     * The locale of {@linkplain java.text.DecimalFormatSymbols decimal format symbols} or other symbols.
     *
     * @see #getLocale()
     */
    private Locale locale;

    /**
     * List of characters (as Unicode code points) acceptable as opening or closing brackets.
     * The array shall comply to the following restrictions:
     *
     * <ul>
     *   <li>The characters at index 0 and 1 are the preferred opening and closing brackets respectively.</li>
     *   <li>For each even index <var>i</var>, {@code brackets[i+1]} is the closing bracket matching {@code brackets[i]}.</li>
     * </ul>
     *
     * @see #getOpeningBracket(int)
     * @see #getClosingBracket(int)
     */
    private int[] brackets;

    /**
     * List of characters (as Unicode code point) used for opening or closing a quoted text.
     * The array shall comply to the following restrictions:
     *
     * <ul>
     *   <li>The characters at index 0 and 1 are the preferred opening and closing quotes respectively.</li>
     *   <li>For each even index <var>i</var>, {@code quotes[i+1]} is the closing quote matching {@code quotes[i]}.</li>
     * </ul>
     *
     * Both opening and closing quotes are usually {@code '"'}.
     */
    private int[] quotes;

    /**
     * The preferred closing quote character ({@code quotes[1]}) as a string.
     * We use the closing quote because this is the character that the parser
     * will look for determining the text end.
     *
     * @see #getQuote()
     * @see #readObject(ObjectInputStream)
     */
    private transient String quote;

    /**
     * The character (as Unicode code point) used for opening ({@code openSequence})
     * or closing ({@code closeSequence}) an array or enumeration.
     */
    private int openSequence, closeSequence;

    /**
     * The string used as a separator in a list of values. This is usually {@code ", "},
     * but may be different if a non-English locale is used for formatting numbers.
     */
    private String separator;

    /**
     * Creates a new set of WKT symbols initialized to the {@linkplain #DEFAULT default} values.
     */
    public Symbols() {
        this(DEFAULT);
    }

    /**
     * Creates a copy of the given set of WKT symbols.
     *
     * @param symbols The symbols to copy.
     */
    public Symbols(final Symbols symbols) {
        locale        = symbols.locale;
        brackets      = symbols.brackets;
        quotes        = symbols.quotes;
        quote         = symbols.quote;
        openSequence  = symbols.openSequence;
        closeSequence = symbols.closeSequence;
        separator     = symbols.separator;
    }

    /**
     * Constructor reserved to {@link #SQUARE_BRACKETS} and {@link #CURLY_BRACKETS} constants.
     * The given array is stored by reference - it is not cloned.
     */
    private Symbols(final int[] brackets, final int[] quotes) {
        this.locale        = Locale.ROOT;
        this.brackets      = brackets;
        this.quotes        = quotes;
        this.quote         = "\"";
        this.openSequence  = '{';
        this.closeSequence = '}';
        this.separator     = ", ";
    }

    /**
     * An immutable set of symbols.
     */
    private static final class Immutable extends Symbols {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -3252233734797811448L;

        /**
         * Constructor reserved to {@link Symbols#SQUARE_BRACKETS} and {@link Symbols#CURLY_BRACKETS} constants.
         * The given arrays are stored by reference - they are not cloned.
         */
        Immutable(final int[] brackets, final int[] quotes) {
            super(brackets, quotes);
        }

        /**
         * Creates an immutable copy of the given set of symbols.
         */
        Immutable(final Symbols symbols) {
            super(symbols);
        }

        /**
         * Returns {@code this} since this set of symbols is already immutable.
         */
        @Override
        Symbols immutable() {
            return this;
        }

        /**
         * Unconditionally throws an exception since instance of this class are immutable.
         */
        @Override
        void checkWritePermission() throws UnsupportedOperationException {
            throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableObject_1, "Symbols"));
        }

        /**
         * Invoked on deserialization for replacing the deserialized instance by the constant instance.
         */
        Object readResolve() {
            if (equals(SQUARE_BRACKETS)) return SQUARE_BRACKETS;
            if (equals(CURLY_BRACKETS))  return CURLY_BRACKETS;
            return this;
        }
    }

    /**
     * Throws an exception if this set of symbols is immutable.
     * To be overridden by the {@link Immutable} subclass only.
     */
    void checkWritePermission() throws UnsupportedOperationException {
    }

    /**
     * Returns an immutable copy of this set of symbols, or {@code this} if this instance is already immutable.
     */
    Symbols immutable() {
        return new Immutable(this);
    }

    /**
     * Returns the locale of {@linkplain java.text.DecimalFormatSymbols decimal format symbols} or other symbols.
     * The default value is {@link Locale#ROOT}. Note that this is not the same locale than the {@link WKTFormat}
     * one, which is used for choosing the language of international strings.
     *
     * @return The symbols locale.
     */
    @Override
    public final Locale getLocale() {
        return locale;
    }

    /**
     * Sets the locale of decimal format symbols or other symbols.
     * Note that any non-English locale is likely to produce WKT that do not conform to ISO 19162.
     * Such WKT should be used for human reading only, not for data export.
     *
     * @param locale The new symbols locale.
     */
    public void setLocale(final Locale locale) {
        checkWritePermission();
        ensureNonNull("locale", locale);
        this.locale = locale;
    }

    /**
     * If the given character is an opening bracket, returns the matching closing bracket.
     * Otherwise returns -1.
     */
    final int matchingBracket(final int c) {
        for (int i=0; i<brackets.length; i+=2) {
            if (brackets[i] == c) {
                return brackets[i+1];
            }
        }
        return -1;
    }

    /**
     * Returns the number of paired brackets. For example if the WKT parser accepts both the
     * {@code […]} and {@code (…)} bracket pairs, then this method returns 2.
     *
     * @return The number of bracket pairs.
     *
     * @see #getOpeningBracket(int)
     * @see #getClosingBracket(int)
     */
    public final int getNumPairedBrackets() {
        return brackets.length >>> 1;
    }

    /**
     * Returns the opening bracket character at the given index.
     * Index 0 stands for the default bracket used at formatting time.
     * All other index are for optional brackets accepted at parsing time.
     *
     * @param  index Index of the opening bracket to get, from 0 to {@link #getNumPairedBrackets()} exclusive.
     * @return The opening bracket at the given index, as a Unicode code point.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    public final int getOpeningBracket(final int index) {
        return brackets[index << 1];
    }

    /**
     * Returns the closing bracket character at the given index.
     * Index 0 stands for the default bracket used at formatting time.
     * All other index are for optional brackets accepted at parsing time.
     *
     * @param  index Index of the closing bracket to get, from 0 to {@link #getNumPairedBrackets()} exclusive.
     * @return The closing bracket at the given index, as a Unicode code point.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    public final int getClosingBracket(final int index) {
        return brackets[(index << 1) | 1];
    }

    /**
     * Sets the opening and closing brackets to the given pairs.
     * Each string shall contain exactly two code points (usually two characters).
     * The first code point is taken as the opening bracket, and the second code point as the closing bracket.
     *
     * {@example The following code will instruct the WKT formatter to use the <code>(…)</code> pair of brackets
     *           at formatting time, but still accept the more common <code>[…]</code> pair of brackets at parsing
     *           time:
     *
     *           <pre>setPairedBrackets("()", "[]");</pre>}
     *
     * @param preferred The preferred pair of opening and closing quotes, used at formatting time.
     * @param alternatives Alternative pairs of opening and closing quotes accepted at parsing time.
     */
    public void setPairedBrackets(final String preferred, final String... alternatives) {
        checkWritePermission();
        brackets = toCodePoints(preferred, alternatives);
    }

    /**
     * Returns the number of paired quotes. For example if the WKT parser accepts both the
     * {@code "…"} and {@code “…”} quote pairs, then this method returns 2.
     *
     * @return The number of quote pairs.
     *
     * @see #getOpeningQuote(int)
     * @see #getClosingQuote(int)
     */
    public final int getNumPairedQuotes() {
        return quotes.length >>> 1;
    }

    /**
     * Returns the opening quote character at the given index.
     * Index 0 stands for the default quote used at formatting time, which is usually {@code '"'}.
     * All other index are for optional quotes accepted at parsing time.
     *
     * @param  index Index of the opening quote to get, from 0 to {@link #getNumPairedQuotes()} exclusive.
     * @return The opening quote at the given index, as a Unicode code point.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    public final int getOpeningQuote(final int index) {
        return quotes[index << 1];
    }

    /**
     * Returns the closing quote character at the given index.
     * Index 0 stands for the default quote used at formatting time, which is usually {@code '"'}.
     * All other index are for optional quotes accepted at parsing time.
     *
     * @param  index Index of the closing quote to get, from 0 to {@link #getNumPairedQuotes()} exclusive.
     * @return The closing quote at the given index, as a Unicode code point.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    public final int getClosingQuote(final int index) {
        return quotes[(index << 1) | 1];
    }

    /**
     * Returns the preferred closing quote character as a string. This is the quote to double if it
     * appears in a Unicode string to format. We check for the closing quote because this is the one
     * that the parser will look for determining the text end.
     */
    final String getQuote() {
        return quote;
    }

    /**
     * Sets the opening and closing quotes to the given pairs.
     * Each string shall contain exactly two code points (usually two characters).
     * The first code point is taken as the opening quote, and the second code point as the closing quote.
     *
     * {@example The following code will instruct the WKT formatter to use the prettier <code>“…”</code>
     *           quotation marks at formatting time (especially useful for <code>String</code> constants
     *           in Java code), but still accept the standard <code>"…"</code> quotation marks at parsing
     *           time:
     *
     *           <pre>setPairedQuotes("“”", "\"\"");</pre>}
     *
     * @param preferred The preferred pair of opening and closing quotes, used at formatting time.
     * @param alternatives Alternative pairs of opening and closing quotes accepted at parsing time.
     */
    public void setPairedQuotes(final String preferred, final String... alternatives) {
        checkWritePermission();
        quotes = toCodePoints(preferred, alternatives);
        quote = preferred.substring(Character.charCount(quotes[0])).trim();
    }

    /**
     * Packs the given pairs of bracket or quotes in a single array of code points.
     * This method also verifies arguments validity.
     */
    private static int[] toCodePoints(final String preferred, final String[] alternatives) {
        ensureNonEmpty("preferred", preferred);
        final int n = (alternatives != null) ? alternatives.length : 0;
        final int[] array = new int[(n+1) * 2];
        String name = "preferred";
        String pair = preferred;
        int i=0, j=0;
        while (true) {
            if (pair.codePointCount(0, pair.length()) != 2) {
                throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, name, pair));
            }
            final int c = pair.codePointAt(0);
            ensureValidUnicodeCodePoint(name, array[j++] = c);
            ensureValidUnicodeCodePoint(name, array[j++] = pair.codePointAt(Character.charCount(c)));
            if (i >= n) {
                break;
            }
            ensureNonNullElement(name = "alternatives", i, pair = alternatives[i++]);
        }
        return array;
    }

    /**
     * Returns the character used for opening a sequence of values.
     * This is usually <code>'{'</code>.
     *
     * @return The character used for opening a sequence of values, as a Unicode code point.
     */
    public final int getOpenSequence() {
        return openSequence;
    }

    /**
     * Returns the character used for closing a sequence of values.
     * This is usually <code>'}'</code>.
     *
     * @return The character used for closing a sequence of values, as a Unicode code point.
     */
    public final int getCloseSequence() {
        return closeSequence;
    }

    /**
     * Sets the characters used for opening and closing a sequence of values.
     *
     * @param openSequence  The character for opening a sequence of values, as a Unicode code point.
     * @param closeSequence The character for closing a sequence of values, as a Unicode code point.
     */
    public void setSequenceBrackets(final int openSequence, final int closeSequence) {
        checkWritePermission();
        ensureValidUnicodeCodePoint("openSequence",  openSequence);
        ensureValidUnicodeCodePoint("closeSequence", closeSequence);
        this.openSequence  = openSequence;
        this.closeSequence = closeSequence;
    }

    /**
     * Returns the string used as a separator in a list of values. This is usually {@code ", "},
     * but may be different if a non-English locale is used for formatting numbers.
     *
     * @return The string used as a separator in a list of values.
     */
    public final String getSeparator() {
        return separator;
    }

    /**
     * Sets the string to use as a separator in a list of values.
     * The given string will be used "as-is" at formatting time,
     * but leading and trailing spaces will be ignored at parsing time.
     *
     * @param separator The new string to use as a separator in a list of values.
     */
    public void setSeparator(final String separator) {
        checkWritePermission();
        ensureNonEmpty("separator", separator);
        this.separator = separator;
    }

    /**
     * Creates a new number format to use for parsing and formatting. Each {@link WKTFormat} will
     * create its own instance, since {@link NumberFormat}s are not guaranteed to be thread-safe.
     *
     * {@section Scientific notation}
     * The {@link NumberFormat} created here does not use scientific notation. This is okay for many
     * WKT formatting purpose since Earth ellipsoid axis lengths in metres are large enough for trigging
     * scientific notation, while we want to express them as normal numbers with centimetre precision.
     * However this is problematic for small numbers like 1E-5. Callers may need to adjust the precision
     * depending on the kind of numbers (length or angle) to format.
     */
    final NumberFormat createNumberFormat() {
        final NumberFormat format = NumberFormat.getNumberInstance(locale);
        format.setGroupingUsed(false);
        return format;
    }

    /**
     * Returns {@code true} if the given WKT contains at least one instance of the given element.
     * Invoking this method is equivalent to invoking {@link String#contains(CharSequence)} except
     * for the following:
     *
     * <ul>
     *   <li>The search is case-insensitive.</li>
     *   <li>Characters between {@linkplain #getOpeningQuote(int) opening quotes} and
     *       {@linkplain #getClosingQuote(int) closing quotes} are ignored.</li>
     *   <li>The element found in the given WKT can not be preceded by other
     *       {@linkplain Character#isUnicodeIdentifierPart(int) Unicode identifier characters}.</li>
     *   <li>The element found in the given WKT must be followed, ignoring space, by an
     *       {@linkplain #getOpeningBracket(int) opening bracket}.</li>
     * </ul>
     *
     * The purpose of this method is to guess some characteristics about the encoded object without
     * the cost of a full WKT parsing.
     *
     * @param  wkt The WKT to inspect.
     * @param  element The element to search for.
     * @return {@code true} if the given WKT contains at least one instance of the given element.
     */
    public boolean containsElement(final CharSequence wkt, final String element) {
        ensureNonNull("wkt", wkt);
        ensureNonEmpty("element", element);
        if (!CharSequences.isUnicodeIdentifier(element)) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.NotAUnicodeIdentifier_1, element));
        }
        return containsElement(wkt, element, 0);
    }

    /**
     * Returns {@code true} if the given WKT contains at least one instance of the {@code AXIS[…]} element.
     * Invoking this method is equivalent to invoking
     * <code>{@linkplain #containsElement(CharSequence, String) containsElement}(wkt, "AXIS")</code>.
     *
     * {@section Use case}
     * The check for axis elements is of particular interest because the axis order is a frequent cause
     * of confusion when processing geographic data. Some applications just ignore any declared axis order
     * in favor of their own hard-coded (<var>longitude</var>, <var>latitude</var>) axis order.
     * Consequently, the presence of {@code AXIS[…]} elements in a WKT is an indication that the encoded
     * object may not be understood as intended by some external softwares.
     *
     * @param  wkt The WKT to inspect.
     * @return {@code true} if the given WKT contains at least one instance of the {@code AXIS[…]} element.
     */
    public boolean containsAxis(final CharSequence wkt) {
        ensureNonNull("wkt", wkt);
        return containsElement(wkt, "AXIS", 0);
    }

    /**
     * Implementation of {@link #containsElement(CharSequence, String)} without verification of argument validity.
     *
     * @param  wkt     The WKT to inspect.
     * @param  element The element to search. Must contains only uppercase letters.
     * @param  offset  The index to start the search from.
     */
    private boolean containsElement(final CharSequence wkt, final String element, int offset) {
        final int[] quotes = this.quotes;
        final int length = wkt.length();
        boolean isQuoting = false;
        int closeQuote = 0;
        while (offset < length) {
            int c = Character.codePointAt(wkt, offset);
            if (closeQuote != 0) {
                if (c == closeQuote) {
                    isQuoting = false;
                }
            } else for (int i=0; i<quotes.length; i+=2) {
                if (c == quotes[i]) {
                    closeQuote = quotes[i | 1];
                    isQuoting = true;
                    break;
                }
            }
            if (!isQuoting && Character.isUnicodeIdentifierStart(c)) {
                /*
                 * Found the beginning of a Unicode identifier.
                 * Check if this is the identifier we were looking for.
                 */
                if (CharSequences.regionMatches(wkt, offset, element, true)) {
                    offset = CharSequences.skipLeadingWhitespaces(wkt, offset + element.length(), length);
                    if (offset >= length) {
                        break;
                    }
                    c = Character.codePointAt(wkt, offset);
                    if (matchingBracket(c) >= 0) {
                        return true;
                    }
                } else {
                    /*
                     * Not the identifier we were looking for. Skip the whole identifier.
                     */
                    do {
                        offset += Character.charCount(c);
                        if (offset >= length) {
                            return false;
                        }
                        c = Character.codePointAt(wkt, offset);
                    } while (Character.isUnicodeIdentifierPart(c));
                }
            }
            offset += Character.charCount(c);
        }
        return false;
    }

    /**
     * Compares this {@code Symbols} with the given object for equality.
     *
     * @param  other The object to compare with this {@code Symbols}.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof Symbols) {
            final Symbols that = (Symbols) other;
            return Arrays.equals(brackets, that.brackets) &&
                   Arrays.equals(quotes, that.quotes) &&
                   // no need to compare 'quote' because it is computed from 'quotes'.
                   openSequence  == that.openSequence &&
                   closeSequence == that.closeSequence &&
                   separator.equals(that.separator) &&
                   locale.equals(that.locale);
        }
        return false;
    }

    /**
     * Returns a hash code value for this object.
     *
     * @return A hash code value.
     */
    @Override
    public int hashCode() {
        return Arrays.deepHashCode(new Object[] {brackets, quotes, openSequence, closeSequence, separator, locale});
    }

    /**
     * Invoked on deserialization for recomputing the {@link #quote} field.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        quote = String.valueOf(Character.toChars(quotes[1]));
    }
}
