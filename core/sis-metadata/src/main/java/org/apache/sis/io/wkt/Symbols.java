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
import java.io.Serializable;
import java.text.NumberFormat;
import org.apache.sis.util.Localized;
import org.apache.sis.util.Workaround;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.*;

// Related to JDK7
import org.apache.sis.internal.jdk7.JDK7;


/**
 * The set of symbols to use for <cite>Well Known Text</cite> (WKT) parsing and formatting.
 * The two constants defined in this class, namely {@link #SQUARE_BRACKETS} and {@link #CURLY_BRACKETS},
 * define the symbols for ISO 19162 compliant WKT formatting. Their properties are:
 *
 * <blockquote><table class="compact" summary="Standard WKT symbols.">
 *   <tr>
 *     <td>Locale for number format:</td>
 *     <td>{@link Locale#ROOT}</td>
 *     <td></td>
 *   </tr>
 *   <tr>
 *     <td>Bracket symbols:</td>
 *     <td>{@code [}…{@code ]} or {@code (}…{@code )}</td>
 *     <td><span style="font-size: small"><b>Note:</b> the {@code […]} brackets are common in referencing WKT,
 *         while the {@code (…)} brackets are common in geometry WKT.</span></td>
 *   </tr>
 *   <tr>
 *     <td>Quote symbols:</td>
 *     <td>{@code "}…{@code "}</td>
 *     <td><span style="font-size: small"><b>Note:</b> Apache SIS accepts also {@code “…”} quotes
 *         for more readable {@code String} literals in Java code, but this is non-standard.</span></td>
 *   </tr>
 *   <tr>
 *     <td>Sequence symbols:</td>
 *     <td><code>{</code>…<code>}</code></td>
 *     <td></td>
 *   </tr>
 *   <tr>
 *     <td>Separator:</td>
 *     <td>{@code ,}</td>
 *     <td></td>
 *   </tr>
 * </table></blockquote>
 *
 * Users can create their own {@code Symbols} instance for parsing or formatting a WKT with different symbols.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4
 * @version 0.6
 * @module
 *
 * @see WKTFormat#getSymbols()
 * @see WKTFormat#setSymbols(Symbols)
 */
public class Symbols implements Localized, Cloneable, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -1730166945430878916L;

    /**
     * Set to {@code true} if parsing and formatting of number in scientific notation is allowed.
     * The way to achieve that is currently a hack, because {@link NumberFormat} has no API for
     * managing that as of JDK 1.8.
     *
     * @todo See if a future version of JDK allows us to get ride of this ugly hack.
     */
    @Workaround(library = "JDK", version = "1.8")
    static final boolean SCIENTIFIC_NOTATION = true;

    /**
     * The prefix character for the value of a WKT fragment.
     */
    static final char FRAGMENT_VALUE = '$';

    /**
     * A set of symbols with values between square brackets, like {@code DATUM["WGS84"]}.
     * This instance defines:
     *
     * <ul>
     *   <li>{@link Locale#ROOT} for {@linkplain java.text.DecimalFormatSymbols decimal format symbols}.</li>
     *   <li>Square brackets by default, as in {@code DATUM["WGS84"]}, but accepting also curly brackets as in
     *       {@code DATUM("WGS84")}. Both are legal WKT.</li>
     *   <li>English quotation mark ({@code '"'}) by default, but accepting also “…” quotes
     *       for more readable {@link String} constants in Java code.</li>
     *   <li>Coma separator followed by a space ({@code ", "}).</li>
     * </ul>
     *
     * This is the most frequently used WKT format for referencing objects.
     */
    public static final Symbols SQUARE_BRACKETS = new Symbols(
            new int[] {'[', ']', '(', ')'}, new int[] {'"', '"', '“', '”'});

    /**
     * A set of symbols with values between parentheses, like {@code DATUM("WGS84")}.
     * This instance is identical to {@link #SQUARE_BRACKETS} except that the default
     * brackets are the curly ones instead than the square ones (but both are still
     * accepted at parsing time).
     *
     * <p>This format is rare with referencing objects but common with geometry objects.</p>
     */
    public static final Symbols CURLY_BRACKETS = new Symbols(
            new int[] {'(', ')', '[', ']'}, SQUARE_BRACKETS.quotes);

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
     * @see #readResolve()
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
     * Same value than {@link #separator} but without leading and trailing spaces.
     */
    private transient String trimmedSeparator;

    /**
     * {@code true} if this instance shall be considered as immutable.
     */
    private boolean isImmutable;

    /**
     * Creates a new set of WKT symbols initialized to a copy of the given symbols.
     *
     * @param symbols The symbols to copy.
     */
    public Symbols(final Symbols symbols) {
        ensureNonNull("symbols", symbols);
        locale           = symbols.locale;
        brackets         = symbols.brackets;
        quotes           = symbols.quotes;
        quote            = symbols.quote;
        openSequence     = symbols.openSequence;
        closeSequence    = symbols.closeSequence;
        separator        = symbols.separator;
        trimmedSeparator = symbols.trimmedSeparator;
    }

    /**
     * Constructor reserved to {@link #SQUARE_BRACKETS} and {@link #CURLY_BRACKETS} constants.
     * The given array is stored by reference - it is not cloned.
     */
    private Symbols(final int[] brackets, final int[] quotes) {
        this.locale           = Locale.ROOT;
        this.brackets         = brackets;
        this.quotes           = quotes;
        this.quote            = "\"";
        this.openSequence     = '{';
        this.closeSequence    = '}';
        this.separator        = ", ";
        this.trimmedSeparator = ",";
        this.isImmutable      = true;
    }

    /**
     * Throws an exception if this set of symbols is immutable.
     */
    final void checkWritePermission() throws UnsupportedOperationException {
        if (isImmutable) {
            throw new UnsupportedOperationException(Errors.format(Errors.Keys.UnmodifiableObject_1, "Symbols"));
        }
    }

    /**
     * Returns the default set of symbols.
     * This is currently set to {@link #SQUARE_BRACKETS}.
     *
     * @return The default set of symbols.
     */
    public static Symbols getDefault() {
        return SQUARE_BRACKETS;
    }

    /**
     * Returns the locale for formatting dates and numbers.
     * The default value is {@link Locale#ROOT}.
     *
     * <div class="section">Relationship between {@code Symbols} locale and {@code WKTFormat} locale</div>
     * The {@code WKTFormat.getLocale(Locale.DISPLAY)} property specifies the language to use when
     * formatting {@link org.opengis.util.InternationalString} instances and can be set to any value.
     * On the contrary, the {@code Locale} property of this {@code Symbols} class controls
     * the decimal format symbols and is very rarely set to an other locale than {@code Locale.ROOT}.
     *
     * @return The locale for dates and numbers.
     *
     * @see WKTFormat#getLocale()
     */
    @Override
    public final Locale getLocale() {
        return locale;
    }

    /**
     * Sets the locale of decimal format symbols or other symbols.
     * Note that any non-English locale is likely to produce WKT that do not conform to ISO 19162.
     * Such WKT can be used for human reading, but not for data export.
     *
     * @param locale The new symbols locale.
     */
    public void setLocale(final Locale locale) {
        checkWritePermission();
        ensureNonNull("locale", locale);
        this.locale = locale;
    }

    /**
     * Implementation of {@link #matchingBracket(int)} and {@link #matchingQuote(int)}.
     */
    private static int matching(final int[] chars, final int c) {
        for (int i = 0; i < chars.length; i += 2) {
            if (chars[i] == c) {
                return chars[i + 1];
            }
        }
        return -1;
    }

    /**
     * If the given character is an opening bracket, returns the matching closing bracket.
     * Otherwise returns -1.
     */
    final int matchingBracket(final int c) {
        return matching(brackets, c);
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
     * <div class="note"><b>Example:</b>
     * The following code will instruct the WKT formatter to use the (…) pair of brackets at formatting time,
     * but still accept the more common […] pair of brackets at parsing time:
     *
     * {@preformat java
     *   symbols.setPairedBrackets("()", "[]");
     * }</div>
     *
     * @param preferred The preferred pair of opening and closing quotes, used at formatting time.
     * @param alternatives Alternative pairs of opening and closing quotes accepted at parsing time.
     */
    public void setPairedBrackets(final String preferred, final String... alternatives) {
        checkWritePermission();
        brackets = toCodePoints(preferred, alternatives);
    }

    /**
     * If the given character is an opening quote, returns the matching closing quote.
     * Otherwise returns -1.
     */
    final int matchingQuote(final int c) {
        return matching(quotes, c);
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
     * <div class="note"><b>Example:</b>
     * The following code will instruct the WKT formatter to use the prettier “…” quotation marks at formatting time
     * (especially useful for {@code String} constants in Java code), but still accept the standard "…" quotation marks
     * at parsing time:
     *
     * {@preformat java
     *   symbols.setPairedQuotes("“”", "\"\"");
     * }</div>
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
            ensureValidQuoteOrBracket(name, array[j++] = c);
            ensureValidQuoteOrBracket(name, array[j++] = pair.codePointAt(Character.charCount(c)));
            if (i >= n) {
                break;
            }
            ensureNonNullElement(name = "alternatives", i, pair = alternatives[i++]);
        }
        return array;
    }

    /**
     * Ensures that the given code point is a valid Unicode code point but not a Unicode identifier part.
     */
    private static void ensureValidQuoteOrBracket(final String name, final int code) {
        ensureValidUnicodeCodePoint(name, code);
        if (Character.isUnicodeIdentifierPart(code) || Character.isSpaceChar(code) || code == FRAGMENT_VALUE) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalCharacter_2,
                    name, String.valueOf(Character.toChars(code))));
        }
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
        ensureValidQuoteOrBracket("openSequence",  openSequence);
        ensureValidQuoteOrBracket("closeSequence", closeSequence);
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
        final String s = CharSequences.trimWhitespaces(separator.trim());
        ensureNonEmpty("separator", s);
        this.separator = separator;
        trimmedSeparator = s;
    }

    /**
     * Returns the separator without trailing spaces.
     */
    final String trimmedSeparator() {
        return trimmedSeparator;
    }

    /**
     * Returns the value of {@link #getSeparator()} without trailing spaces,
     * followed by the system line separator.
     */
    final String lineSeparator() {
        final String separator = getSeparator();
        return separator.substring(0, CharSequences.skipTrailingWhitespaces(separator, 0, separator.length()))
                .concat(JDK7.lineSeparator());
    }

    /**
     * Creates a new number format to use for parsing and formatting. Each {@link WKTFormat} will
     * create its own instance, since {@link NumberFormat}s are not guaranteed to be thread-safe.
     *
     * <div class="section">Scientific notation</div>
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
     * <div class="section">Use case</div>
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
     * Returns an immutable copy of this set of symbols, or {@code this} if this instance is already immutable.
     */
    final Symbols immutable() {
        if (isImmutable) {
            return this;
        }
        final Symbols clone = clone();
        clone.isImmutable = true;
        return clone;
    }

    /**
     * Returns a clone of this {@code Symbols}.
     *
     * @return A clone of this {@code Symbols}.
     */
    @Override
    public Symbols clone() {
        final Symbols clone;
        try {
            clone = (Symbols) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
        /*
         * No needs to copy the arrays, because their content are never modified.
         * Instead, the setter methods create new arrays.
         */
        clone.isImmutable = false;
        return clone;
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
     * Invoked on deserialization for replacing the deserialized instance by the constant instance.
     * This method also opportunistically recompute the {@link #quote} field if no replacement is done.
     *
     * @return The object to use after deserialization.
     */
    final Object readResolve() {
        if (isImmutable) {
            if (equals(SQUARE_BRACKETS)) return SQUARE_BRACKETS;
            if (equals(CURLY_BRACKETS))  return CURLY_BRACKETS;
        }
        quote = String.valueOf(Character.toChars(quotes[1]));
        trimmedSeparator = CharSequences.trimWhitespaces(separator.trim());
        return this;
    }
}
