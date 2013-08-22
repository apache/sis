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

import java.util.Locale;
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
 *   <li>Square brackets, as in {@code DATUM["WGS84"]}. An alternative allowed by the WKT
 *       specification is curly brackets as in {@code DATUM("WGS84")}.</li>
 * </ul>
 *
 * {@section Relationship between <code>Symbols</code> locale and <code>WKTFormat</code> locale}
 * The {@link Locale} property of {@link WKTFormat} specifies the language to use when formatting
 * {@link org.opengis.util.InternationalString}. This can be set to any value. On the contrary,
 * the {@code Locale} property of {@code Symbols} specifies the {@linkplain java.text.DecimalFormatSymbols
 * decimal format symbols} and is very rarely set to an other locale than an English one.
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
    public static final Symbols SQUARE_BRACKETS = new Immutable('[', ']', '(', ')');

    /**
     * A set of symbols with values between parentheses, like {@code DATUM("WGS84")}.
     * This is a less frequently used but legal WKT format.
     */
    public static final Symbols CURLY_BRACKETS = new Immutable('(', ')', '[', ']');

    /**
     * The default set of symbols.
     * This is currently set to {@link #SQUARE_BRACKETS}.
     *
     * @see Colors#DEFAULT
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
     * The character (as Unicode code point) used for opening ({@code openSequence})
     * or closing ({@code closeSequence}) an array or enumeration.
     */
    private int openSequence, closeSequence;

    /**
     * The character (as Unicode code point) used for opening ({@code openQuote}) or
     * closing ({@code closeQuote}) a quoted text. This is usually {@code '"'}.
     */
    private int openQuote, closeQuote;

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
        openSequence  = symbols.openSequence;
        closeSequence = symbols.closeSequence;
        openQuote     = symbols.openQuote;
        closeQuote    = symbols.closeQuote;
        separator     = symbols.separator;
    }

    /**
     * Constructor reserved to {@link #SQUARE_BRACKETS} and {@link #CURLY_BRACKETS} constants.
     * The given array is stored by reference - it is not cloned.
     */
    private Symbols(final int[] brackets) {
        this.locale        = Locale.US;
        this.brackets      = brackets;
        this.openSequence  = '{';
        this.closeSequence = '}';
        this.openQuote     = '"';
        this.closeQuote    = '"';
        this.separator     = ", ";
    }

    /**
     * An immutable set of symbols.
     */
    static final class Immutable extends Symbols {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -3252233734797811448L;

        /**
         * Constructor reserved to {@link Symbols#SQUARE_BRACKETS} and {@link Symbols#CURLY_BRACKETS} constants.
         * The given array is stored by reference - it is not cloned.
         */
        Immutable(final int... brackets) {
            super(brackets);
        }

        /**
         * Unconditionally throws an exception since instance of this class are immutable.
         */
        @Override
        void checkWritePermission() throws UnsupportedOperationException {
            throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableObject_1, "Symbols"));
        }
    }

    /**
     * Throws an exception if this set of symbols is immutable.
     * To be overridden by the {@link Immutable} subclass only.
     */
    void checkWritePermission() throws UnsupportedOperationException {
    }

    /**
     * Returns the locale of {@linkplain java.text.DecimalFormatSymbols decimal format symbols}
     * or other symbols. This is usually an English locale. Note that this is not the same locale
     * than the {@link WKTFormat} one, which is used for choosing the language of international strings.
     *
     * @return The symbols locale.
     */
    @Override
    public final Locale getLocale() {
        return locale;
    }

    /**
     * Sets the locale of decimal format symbols or other symbols.
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
     * Returns the opening bracket character at the given index.
     * Index 0 stands for the default bracket used at formatting time.
     * All other index are for optional brackets accepted at parsing time.
     *
     * @param  index Index of the opening bracket to get.
     * @return The opening bracket at the given index, as a Unicode code point.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    public final int getOpeningBracket(final int index) {
        return brackets[index*2];
    }

    /**
     * Returns the closing bracket character at the given index.
     * Index 0 stands for the default bracket used at formatting time.
     * All other index are for optional brackets accepted at parsing time.
     *
     * @param  index Index of the closing bracket to get.
     * @return The closing bracket at the given index, as a Unicode code point.
     * @throws IndexOutOfBoundsException if the given index is out of bounds.
     */
    public final int getClosingBracket(final int index) {
        return brackets[index*2 + 1];
    }

    /**
     * Sets the opening and closing brackets to the given characters.
     * The given arrays shall comply to the following constraints:
     *
     * <ul>
     *   <li>The two arrays shall be non-empty and have the same length.</li>
     *   <li>The characters at index 0 are the preferred opening and closing brackets.</li>
     *   <li>For each index <var>i</var>, {@code closingBrackets[i]} is the closing bracket
     *       matching {@code openingBrackets[i]}.</li>
     * </ul>
     *
     * @param openingBrackets The opening brackets, as a Unicode code point.
     * @param closingBrackets The closing brackets matching the opening ones.
     */
    public void setBrackets(final int[] openingBrackets, final int[] closingBrackets) {
        checkWritePermission();
        ensureNonNull("openingBrackets", openingBrackets);
        ensureNonNull("closingBrackets", closingBrackets);
        final int length = openingBrackets.length;
        if (closingBrackets.length != length) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.MismatchedArrayLengths));
        }
        if (length == 0) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.EmptyArgument_1, "openingBrackets"));
        }
        final int[] brackets = new int[length * 2];
        for (int i=0,j=0; i<length; i++) {
            ensureValidUnicodeCodePoint("openingBrackets", openingBrackets[i]);
            ensureValidUnicodeCodePoint("closingBrackets", closingBrackets[i]);
            brackets[j++] = openingBrackets[i];
            brackets[j++] = closingBrackets[i];
        }
        this.brackets = brackets; // Store only on success.
    }

    /**
     * Returns the character used for opening a sequence of values.
     * This is usually {@code '{'}.
     *
     * @return The character used for opening a sequence of values, as a Unicode code point.
     */
    public final int getOpenSequence() {
        return openSequence;
    }

    /**
     * Returns the character used for closing a sequence of values.
     * This is usually {@code '}'}.
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
     * Returns the character used for opening a quoted text. This is usually {@code '"'}.
     *
     * @return The character used for opening a quoted text, as a Unicode code point.
     */
    public final int getOpenQuote() {
        return openQuote;
    }

    /**
     * Returns the character used for closing a quoted text. This is usually {@code '"'}.
     *
     * @return The character used for closing a quoted text, as a Unicode code point.
     */
    public final int getCloseQuote() {
        return closeQuote;
    }

    /**
     * Sets the characters used for opening and closing a quoted text.
     *
     * @param openQuote  The character for opening a quoted text, as a Unicode code point.
     * @param closeQuote The character for closing a quoted text, as a Unicode code point.
     */
    public void setQuotes(final int openQuote, final int closeQuote) {
        checkWritePermission();
        ensureValidUnicodeCodePoint("openQuote",  openQuote);
        ensureValidUnicodeCodePoint("closeQuote", closeQuote);
        this.openQuote  = openQuote;
        this.closeQuote = closeQuote;
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
        format.setMinimumFractionDigits(1);
        format.setMaximumFractionDigits(8);
        return format;
    }

    /**
     * Returns {@code true} if the given WKT contains at least one instance of the given element.
     * Invoking this method is equivalent to invoking {@link String#contains(CharSequence)} except
     * for the following:
     *
     * <ul>
     *   <li>The search is case-insensitive.</li>
     *   <li>Characters between the {@linkplain #getOpenQuote() open quote} and the
     *       {@link #getCloseQuote() close quote} are ignored.</li>
     *   <li>The element found in the given WKT must not be a substring of a larger Unicode identifier.</li>
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
     * Invoking this method is equivalent to invoking {@link #containsElement(CharSequence, CharSequence)}
     * for the {@code "AXIS"} element.
     *
     * <p>The check for axis elements is of particular interest because the axis order is a frequent cause
     * of confusion when processing geographic data. Some applications just ignore the axis order declared
     * in the WKT in favor of their hard-coded (<var>longitude</var>, <var>latitude</var>) axis order.
     * Consequently, the presence of {@code AXIS[…]} elements in a WKT may be an indication that the encoded
     * object may not be directly usable by some external softwares.</p>
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
        final int length = wkt.length();
        boolean isQuoting = false;
        while (offset < length) {
            final int c = Character.codePointAt(wkt, offset);
            if (c == (isQuoting ? closeQuote : openQuote)) {
                isQuoting = !isQuoting;
            }
            if (!isQuoting && Character.isJavaIdentifierStart(c)) {
                /*
                 * Found the beginning of a Unicode identifier.
                 * Check if this is the identifier we were looking for.
                 */
                if (CharSequences.regionMatches(wkt, offset, element, true)) {
                    offset = CharSequences.skipLeadingWhitespaces(wkt, offset + element.length(), length);
                    if (offset >= length) {
                        break;
                    }
                    if (matchingBracket(Character.codePointAt(wkt, offset)) >= 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
