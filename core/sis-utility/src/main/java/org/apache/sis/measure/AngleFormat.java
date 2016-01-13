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

import java.util.Locale;
import java.text.Format;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.ParseException;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.AttributedCharacterIterator;
import org.apache.sis.util.Debug;
import org.apache.sis.util.Localized;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.util.LocalizedParseException;

import static java.lang.Math.abs;
import static java.lang.Math.rint;
import static java.lang.Double.NaN;
import static java.lang.Double.isNaN;
import static java.lang.Double.isInfinite;
import static org.apache.sis.math.MathFunctions.pow10;
import static org.apache.sis.math.MathFunctions.truncate;
import static org.apache.sis.math.MathFunctions.isNegative;
import static org.apache.sis.math.DecimalFunctions.fractionDigitsForDelta;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;
import org.apache.sis.internal.jdk8.JDK8;


/**
 * Parses and formats angles according a specified pattern. The pattern is a string
 * containing any characters, with a special meaning for the following characters:
 *
 * <table class="sis">
 *   <caption>Reserved characters</caption>
 *   <tr><th>Symbol</th><th>Meaning</th></tr>
 *   <tr><td>{@code D}</td><td>The integer part of degrees</td></tr>
 *   <tr><td>{@code d}</td><td>The fractional part of degrees</td></tr>
 *   <tr><td>{@code M}</td><td>The integer part of minutes</td></tr>
 *   <tr><td>{@code m}</td><td>The fractional part of minutes</td></tr>
 *   <tr><td>{@code S}</td><td>The integer part of seconds</td></tr>
 *   <tr><td>{@code s}</td><td>The fractional part of seconds</td></tr>
 *   <tr><td>{@code #}</td><td>Fraction digits shown only if non-zero</td></tr>
 *   <tr><td>{@code .}</td><td>The decimal separator</td></tr>
 *   <tr><td>{@code ?}</td><td>Omit the preceding field if zero</td></tr>
 * </table>
 *
 * Upper-case letters {@code D}, {@code M} and {@code S} stand for the integer parts of degrees,
 * minutes and seconds respectively. If present, they shall appear in that order.
 *
 * <div class="note"><b>Example:</b>
 * "{@code M′D}" is illegal because "{@code M}" and "{@code S}" are in reverse order.
 * "{@code D°S}" is also illegal because "{@code M}" is missing between "{@code D}" and "{@code S}".</div>
 *
 * Lower-case letters {@code d}, {@code m} and {@code s} stand for fractional parts of degrees, minutes and
 * seconds respectively. Only one of those can appear in a pattern. If present, they must be in the last field.
 *
 * <div class="note"><b>Example:</b>
 * "{@code D.dd°MM′}" is illegal because "{@code d}" is followed by "{@code M}".
 * "{@code D.mm}" is also illegal because "{@code m}" is not the fractional part of "{@code D}".</div>
 *
 * The number of occurrences of {@code D}, {@code M}, {@code S} and their lower-case counterpart is the number
 * of digits to format.
 *
 * <div class="note"><b>Example:</b>
 * "{@code DD.ddd}" will format angles with two digits for the integer part and three digits
 * for the fractional part (e.g. {@code 4.4578} will be formatted as {@code "04.458"}).</div>
 *
 * Separator characters like {@code °}, {@code ′} and {@code ″} are inserted "as-is" in the formatted string,
 * except the decimal separator dot ({@code .}) which is replaced by the local-dependent decimal separator.
 * Separator characters may be completely omitted; {@code AngleFormat} will still differentiate degrees,
 * minutes and seconds fields according the pattern.
 *
 * <div class="note"><b>Example:</b>
 * "{@code 0480439}" with the "{@code DDDMMmm}" pattern will be parsed as 48°04.39′.</div>
 *
 * The {@code ?} modifier specifies that the preceding field can be omitted if its value is zero.
 * Any field can be omitted for {@link Angle} object, but only trailing fields are omitted for
 * {@link Longitude} and {@link Latitude}.
 *
 * <div class="note"><b>Example:</b>
 * "{@code DD°MM′?SS″?}" will format an angle of 12.01° as {@code 12°36″},
 * but a longitude of 12.01°N as {@code 12°00′36″N} (not {@code 12°36″N}).</div>
 *
 * The above special case exists because some kind of angles are expected to be very small (e.g. rotation angles in
 * {@linkplain org.apache.sis.referencing.datum.BursaWolfParameters Bursa-Wolf parameters} are given in arc-seconds),
 * while longitude and latitude values are usually distributed over their full ±180° or ±90° range. Since longitude
 * or latitude values without the degrees field are unusual, omitting that field is likely to increase the
 * risk of confusion in those cases.
 *
 * <div class="note"><b>Examples:</b>
 * <table class="sis">
 *   <caption>Pattern examples</caption>
 *   <tr><th>Pattern               </th>  <th>48.5      </th> <th>-12.53125    </th></tr>
 *   <tr><td>{@code DD°MM′SS.#″}   </td>  <td>48°30′00″ </td> <td>-12°31′52.5″ </td></tr>
 *   <tr><td>{@code DD°MM′}        </td>  <td>48°30′    </td> <td>-12°32′      </td></tr>
 *   <tr><td>{@code DD.ddd}        </td>  <td>48.500    </td> <td>-12.531      </td></tr>
 *   <tr><td>{@code DD.###}        </td>  <td>48.5      </td> <td>-12.531      </td></tr>
 *   <tr><td>{@code DDMM}          </td>  <td>4830      </td> <td>-1232        </td></tr>
 *   <tr><td>{@code DDMMSSs}       </td>  <td>4830000   </td> <td>-1231525     </td></tr>
 *   <tr><td>{@code DD°MM′?SS.s″?} </td>  <td>48°30′    </td> <td>-12°31′52.5″ </td></tr>
 * </table>
 * </div>
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @since   0.3
 * @version 0.4
 * @module
 *
 * @see Angle
 * @see Latitude
 * @see Longitude
 */
public class AngleFormat extends Format implements Localized {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 820524050016391537L;

    /**
     * Hemisphere symbols. Must be upper-case.
     */
    private static final char NORTH='N', SOUTH='S', EAST='E', WEST='W';

    /**
     * A constant for the symbol to appears before the degrees fields. used in {@code switch}
     * statements. Fields PREFIX, DEGREES, MINUTES and SECONDS must have increasing values.
     */
    private static final int PREFIX_FIELD = -1;

    /**
     * Constant for degrees field. When formatting a string, this value may be specified to the
     * {@link FieldPosition} constructor in order to get the bounding index where degrees have
     * been written.
     *
     * @see Field#DEGREES
     */
    static final int DEGREES_FIELD = 0;

    /**
     * Constant for minutes field. When formatting a string, this value may be specified to the
     * {@link FieldPosition} constructor in order to get the bounding index where minutes have
     * been written.
     *
     * @see Field#MINUTES
     */
    static final int MINUTES_FIELD = 1;

    /**
     * Constant for seconds field. When formatting a string, this value may be specified to the
     * {@link FieldPosition} constructor in order to get the bounding index where seconds have
     * been written.
     *
     * @see Field#SECONDS
     */
    static final int SECONDS_FIELD = 2;

    /**
     * Constant for the fractional part of the degrees, minutes or seconds field. When formatting
     * a string, this value may be specified to the {@link FieldPosition} constructor in order to
     * get the bounding index where fraction digits have been written.
     */
    private static final int FRACTION_FIELD = 3; // Not yet implemented.

    /**
     * Constant for hemisphere field. When formatting a string, this value may be specified to the
     * {@link FieldPosition} constructor in order to get the bounding index where the hemisphere
     * symbol has been written.
     *
     * @see Field#HEMISPHERE
     */
    static final int HEMISPHERE_FIELD = 4;

    /**
     * Index for the {@link #SYMBOLS} character which stands for optional field.
     */
    private static final int OPTIONAL_FIELD = 4;

    /**
     * Symbols for degrees (0), minutes (1), seconds (2) and optional fraction digits (3).
     * The index of each symbol shall be equal to the corresponding {@code *_FIELD} constant.
     */
    private static final int[] SYMBOLS = {'D', 'M', 'S', '#', '?'};

    /**
     * Constants that are used as attribute keys in the iterator returned from
     * {@link AngleFormat#formatToCharacterIterator(Object)}.
     *
     * @author  Martin Desruisseaux (Geomatys)
     * @since   0.3
     * @version 0.3
     * @module
     */
    public static final class Field extends FormatField {
        /**
         * For cross-version compatibility.
         */
        private static final long serialVersionUID = -5015489890305908251L;

        /**
         * Creates a new field of the given name. The given name shall
         * be identical to the name of the public static constant.
         */
        private Field(final String name, final int fieldID) {
            super(name, fieldID);
        }

        /**
         * Identifies the degrees field, including the degrees symbol (if any).
         * When formatting a string, this value may be specified to the {@link FieldPosition}
         * constructor in order to get the bounding index where degrees have been written.
         */
        public static final Field DEGREES = new Field("DEGREES", DEGREES_FIELD);

        /**
         * Identifies the minutes field, including the minutes symbol (if any).
         * When formatting a string, this value may be specified to the {@link FieldPosition}
         * constructor in order to get the bounding index where minutes have been written.
         */
        public static final Field MINUTES = new Field("MINUTES", MINUTES_FIELD);

        /**
         * Identifies the seconds field, including the seconds symbol (if any).
         * When formatting a string, this value may be specified to the {@link FieldPosition}
         * constructor in order to get the bounding index where seconds have been written.
         */
        public static final Field SECONDS = new Field("SECONDS", SECONDS_FIELD);

        /**
         * Identifies the hemisphere symbol (if any).
         * When formatting a string, this value may be specified to the {@link FieldPosition}
         * constructor in order to get the bounding index where hemisphere have been written.
         */
        public static final Field HEMISPHERE = new Field("HEMISPHERE", HEMISPHERE_FIELD);

        /**
         * Returns the field constant for the given numeric identifier.
         */
        static Field forCode(final int field) {
            switch (field) {
                case DEGREES_FIELD:    return DEGREES;
                case MINUTES_FIELD:    return MINUTES;
                case SECONDS_FIELD:    return SECONDS;
                case HEMISPHERE_FIELD: return HEMISPHERE;
                default: throw new AssertionError(field);
            }
        }
    }

    /**
     * The locale specified at construction time (never null).
     */
    private final Locale locale;

    /**
     * Minimal amount of spaces to be used by the degrees, minutes and seconds fields,
     * and by the decimal digits. A value of 0 means that the field is not formatted.
     * {@code fractionFieldWidth} applies to the last non-zero field.
     * {@code maximumTotalWidth} is 0 (the default) if there is no restriction.
     */
    private byte degreesFieldWidth,
                 minutesFieldWidth,
                 secondsFieldWidth,
                 fractionFieldWidth,
                 minimumFractionDigits,
                 maximumTotalWidth;

    /**
     * A bitmask of optional fields. Optional fields are formatted only if their value is different than zero.
     * The bit position is given by a {@code *_FIELD} constant, and the actual bitmask is computed by
     * {@code 1 << *_FIELD}. A value of zero means that no field is optional.
     */
    private byte optionalFields;

    /**
     * Characters to insert before the text to format, and after each field.
     * A {@code null} value means that there is nothing to insert.
     */
    private String prefix,
                   degreesSuffix,
                   minutesSuffix,
                   secondsSuffix;

    /**
     * {@code true} if the {@link #parse(String, ParsePosition)} method is allowed to fallback
     * on the build-in default symbols if the string to parse doesn't match the pattern.
     *
     * <p>This field can not be set by the pattern string,
     * so it needs to be initialized separately.</p>
     *
     * @see #isFallbackAllowed()
     * @see #setFallbackAllowed(boolean)
     */
    private boolean isFallbackAllowed = true;

    /**
     * Specifies whatever the decimal separator shall be inserted between the integer part
     * and the fraction part of the last field. A {@code false} value formats the integer
     * and fractional part without separation, e.g. "34867" for 34.867.
     */
    private boolean useDecimalSeparator;

    /**
     * If {@code true}, {@link #optionalFields} never apply to fields to leading fields.
     * If the minutes field is declared optional but the degrees and seconds are formatted,
     * then minutes will be formatted too un order to reduce the risk of confusion
     *
     * <div class="note"><b>Example:</b>
     * Value 12.01 is formatted as {@code 12°00′36″} if this field is {@code true},
     * and as {@code 12°36″} if this field is {@code false}.</div>
     */
    private transient boolean showLeadingFields;

    /**
     * Format to use for writing numbers (degrees, minutes or seconds) when formatting an angle.
     * The pattern given to this {@code DecimalFormat} shall NOT accept exponential notation,
     * because "E" of "Exponent" would be confused with "E" of "East".
     */
    private transient NumberFormat numberFormat;

    /**
     * Object to give to {@code DecimalFormat.format} methods,
     * cached in order to avoid recreating this object too often.
     *
     * @see #dummyFieldPosition()
     */
    private transient FieldPosition dummyFieldPosition;

    /**
     * A temporary variable which may be set to the character iterator for which the
     * attributes need to be set. IF non-null, then this is actually an instance of
     * {@link FormattedCharacterIterator}. But we use the interface here for avoiding
     * too early class loading.
     *
     * @see #formatToCharacterIterator(Object)
     */
    private transient AttributedCharacterIterator characterIterator;

    /**
     * Returns the number format, created when first needed.
     */
    private NumberFormat numberFormat() {
        if (numberFormat == null) {
            numberFormat = new DecimalFormat("#0", DecimalFormatSymbols.getInstance(locale));
        }
        return numberFormat;
    }

    /**
     * Returns the dummy field position.
     */
    private FieldPosition dummyFieldPosition() {
        if (dummyFieldPosition == null) {
            dummyFieldPosition = new FieldPosition(NumberFormat.INTEGER_FIELD);
        }
        return dummyFieldPosition;
    }

    /**
     * Constructs a new {@code AngleFormat} for the default pattern and the current default locale.
     *
     * @return An angle format for the current default locale.
     */
    public static AngleFormat getInstance() {
        return new AngleFormat();
    }

    /**
     * Constructs a new {@code AngleFormat} for the default pattern and the specified locale.
     *
     * @param  locale The locale to use.
     * @return An angle format for the given locale.
     */
    public static AngleFormat getInstance(final Locale locale) {
        return new AngleFormat(locale);
    }

    /**
     * Constructs a new {@code AngleFormat} for the default pattern and the current default locale.
     */
    public AngleFormat() {
        this(Locale.getDefault());
    }

    /**
     * Constructs a new {@code AngleFormat} for the default pattern and the specified locale.
     *
     * @param  locale The locale to use.
     */
    @SuppressWarnings("PointlessBitwiseExpression")  // We rely on the compiler for simplifying the expression.
    public AngleFormat(final Locale locale) {
        ArgumentChecks.ensureNonNull("locale", locale);
        this.locale = locale;
        degreesFieldWidth   = 1;
        minutesFieldWidth   = 2;
        secondsFieldWidth   = 2;
        fractionFieldWidth  = 16;  // Number of digits for accurate representation of 1″ ULP.
        optionalFields      = (1 << DEGREES_FIELD) | (1 << MINUTES_FIELD) | (1 << SECONDS_FIELD);
        degreesSuffix       = "°";
        minutesSuffix       = "′";
        secondsSuffix       = "″";
        useDecimalSeparator = true;
    }

    /**
     * Constructs a new {@code AngleFormat} for the specified pattern and the current default locale.
     *
     * @param  pattern Pattern to use for parsing and formatting angles.
     *         See class description for an explanation of pattern syntax.
     * @throws IllegalArgumentException If the specified pattern is illegal.
     */
    public AngleFormat(final String pattern) throws IllegalArgumentException {
        this(pattern, Locale.getDefault());
    }

    /**
     * Constructs a new {@code AngleFormat} using the specified pattern and locale.
     *
     * @param  pattern Pattern to use for parsing and formatting angles.
     *         See class description for an explanation of pattern syntax.
     * @param  locale Locale to use.
     * @throws IllegalArgumentException If the specified pattern is illegal.
     */
    public AngleFormat(final String pattern, final Locale locale) throws IllegalArgumentException {
        ArgumentChecks.ensureNonEmpty("pattern", pattern);
        ArgumentChecks.ensureNonNull("locale", locale);
        this.locale = locale;
        applyPattern(pattern, SYMBOLS, '.');
    }

    /**
     * Sets the pattern to use for parsing and formatting angles.
     * See class description for a description of pattern syntax.
     *
     * @param  pattern Pattern to use for parsing and formatting angle.
     * @throws IllegalArgumentException If the specified pattern is not legal.
     *
     * @see #setMinimumFractionDigits(int)
     * @see #setMaximumFractionDigits(int)
     */
    public void applyPattern(final String pattern) throws IllegalArgumentException {
        ArgumentChecks.ensureNonEmpty("pattern", pattern);
        degreesFieldWidth     = 0;
        minutesFieldWidth     = 0;
        secondsFieldWidth     = 0;
        fractionFieldWidth    = 0;
        minimumFractionDigits = 0;
        maximumTotalWidth     = 0;
        optionalFields        = 0;
        prefix                = null;
        degreesSuffix         = null;
        minutesSuffix         = null;
        secondsSuffix         = null;
        useDecimalSeparator   = false;
        applyPattern(pattern, SYMBOLS, '.');
    }

    /**
     * Actual implementation of {@link #applyPattern(String)}, as a private method for use by the constructor.
     * All fields related to the pattern shall be set to 0 or null before this method call.
     *
     * @param symbols An array of code points containing the reserved symbols as upper-case letters.
     *        This is always the {@link #SYMBOLS} array, unless we apply localized patterns.
     * @param decimalSeparator The code point which represent decimal separator in the pattern.
     */
    @SuppressWarnings("fallthrough")
    private void applyPattern(final String pattern, final int[] symbols, final int decimalSeparator) {
        degreesFieldWidth     = 1;
        useDecimalSeparator   = true;
        int expectedField     = PREFIX_FIELD;
        int endPreviousField  = 0;
        boolean parseFinished = false;
        final int length = pattern.length();
        for (int i=0; i<length;) {
            /*
             * Examine the first characters in the pattern, skipping the non-reserved ones
             * ("D", "M", "S", "d", "m", "s", "#"). Non-reserved characters will be stored
             * as prefix or suffix later.
             */
            int c           = pattern.codePointAt(i);
            int charCount   = Character.charCount(c);
            int upperCaseC  = Character.toUpperCase(c);
            final int field = fieldForSymbol(symbols, upperCaseC);
            if (field < 0) { // If not a reserved character, continue the search.
                i += charCount;
                continue;
            }
            /*
             * A reserved character has been found.  Ensure that it appears in a legal location.
             * For example "MM.mm" is illegal because there is no 'D' before 'M', and "DD.mm" is
             * illegal because the integer part is not 'M'. The legal location is 'expectedField'.
             */
            final boolean isIntegerField = (c == upperCaseC) && (field != FRACTION_FIELD);
            if (isIntegerField) {
                expectedField++;
            }
            if (parseFinished || (field != expectedField && field != FRACTION_FIELD)) {
                throw illegalPattern(pattern);
            }
            if (isIntegerField) {
                /*
                 * If the reserved letter is upper-case, then we found the integer part of a field.
                 * Memorize the characters prior the reserved letter as the suffix of the previous field.
                 * Then count the number of occurrences of that reserved letter. This number will be the
                 * field width.
                 */
                String previousSuffix = null;
                if (endPreviousField < i) {
                    int endPreviousSuffix = i;
                    if (pattern.codePointBefore(endPreviousSuffix) == symbols[OPTIONAL_FIELD]) {
                        // If we find the '?' character, then the previous field is optional.
                        if (--endPreviousSuffix == endPreviousField) {
                            throw illegalPattern(pattern);
                        }
                        optionalFields |= (1 << (field - 1));
                    }
                    previousSuffix = pattern.substring(endPreviousField, endPreviousSuffix);
                }
                int width = 1;
                while ((i += charCount) < length && pattern.codePointAt(i) == c) {
                    width++;
                }
                final byte wb = toByte(width);
                switch (field) {
                    case DEGREES_FIELD: prefix        = previousSuffix; degreesFieldWidth = wb; break;
                    case MINUTES_FIELD: degreesSuffix = previousSuffix; minutesFieldWidth = wb; break;
                    case SECONDS_FIELD: minutesSuffix = previousSuffix; secondsFieldWidth = wb; break;
                    default: throw new AssertionError(field);
                }
            } else {
                /*
                 * If the reserved letter is lower-case or the symbol for optional fraction digit,
                 * then the part before that letter will be the decimal separator rather than the
                 * suffix of previous field. The number of occurrences of the lower-case letter will
                 * be the precision of the fraction part.
                 */
                if (i == endPreviousField) {
                    useDecimalSeparator = false;
                } else {
                    final int b = pattern.codePointAt(endPreviousField);
                    if (b != decimalSeparator || endPreviousField + Character.charCount(b) != i) {
                        throw illegalPattern(pattern);
                    }
                }
                int width = 1;
                while ((i += charCount) < length) {
                    final int fc = pattern.codePointAt(i);
                    if (fc != c) {
                        if (fc != symbols[FRACTION_FIELD]) break;
                        // Switch the search from mandatory to optional digits.
                        minimumFractionDigits = toByte(width);
                        charCount = Character.charCount(c = fc);
                    }
                    width++;
                }
                fractionFieldWidth = toByte(width);
                if (c != symbols[FRACTION_FIELD]) {
                    // The pattern contains only mandatory digits.
                    minimumFractionDigits = fractionFieldWidth;
                } else if (!useDecimalSeparator) {
                    // Variable number of digits not allowed if there is no decimal separator.
                    throw new IllegalArgumentException(Errors.format(Errors.Keys.RequireDecimalSeparator));
                }
                parseFinished = true;
            }
            endPreviousField = i;
        }
        /*
         * At this point, we finished parsing the pattern. We may have some trailing characters which have not
         * been processed by the main loop. Those trailing characters will be the suffix of the last field.
         */
        if (endPreviousField < length) {
            int endPreviousSuffix = length;
            if (pattern.codePointBefore(endPreviousSuffix) == symbols[OPTIONAL_FIELD]) {
                if (--endPreviousSuffix == endPreviousField) {
                    throw illegalPattern(pattern);
                }
                optionalFields |= (1 << expectedField);
            }
            final String suffix = pattern.substring(endPreviousField, endPreviousSuffix);
            switch (expectedField) {
                case DEGREES_FIELD: degreesSuffix = suffix; break;
                case MINUTES_FIELD: minutesSuffix = suffix; break;
                case SECONDS_FIELD: secondsSuffix = suffix; break;
                default: {
                    // Happen if no symbol has been recognized in the pattern.
                    throw illegalPattern(pattern);
                }
            }
        }
    }

    /**
     * Returns the field index for the given upper case character, or -1 if none.
     *
     * @param  symbols An array of code points containing the reserved symbols as upper-case letters.
     * @param  c The symbol to search, as an upper-case character (code point actually).
     * @return The index of the given character, or -1 if not found.
     */
    private static int fieldForSymbol(final int[] symbols, final int c) {
        for (int field=DEGREES_FIELD; field<=FRACTION_FIELD; field++) {
            if (c == symbols[field]) {
                return field;
            }
        }
        return -1;
    }

    /**
     * Returns an exception for an illegal pattern.
     */
    private static IllegalArgumentException illegalPattern(final String pattern) {
        return new IllegalArgumentException(Errors.format(
                Errors.Keys.IllegalFormatPatternForClass_2, Angle.class, pattern));
    }

    /**
     * Returns the pattern used for parsing and formatting angles.
     * See class description for an explanation of how patterns work.
     *
     * @return The formatting pattern.
     *
     * @see #getMinimumFractionDigits()
     * @see #getMaximumFractionDigits()
     */
    public String toPattern() {
        return toPattern(SYMBOLS, '.');
    }

    /**
     * Actual implementation of {@link #toPattern()} and {@code toLocalizedPattern()}
     * (the later method may be provided in a future SIS version).
     *
     * @param symbols An array of code points containing the reserved symbols as upper-case letters.
     *        This is always the {@link #SYMBOLS} array, unless we apply localized patterns.
     * @param decimalSeparator The code point which represent decimal separator in the pattern.
     */
    private String toPattern(final int[] symbols, final int decimalSeparator) {
        int symbol = 0;
        boolean previousWasOptional = false;
        final StringBuilder buffer = new StringBuilder();
        for (int field=DEGREES_FIELD; field<=FRACTION_FIELD; field++) {
            final String previousSuffix;
            int width;
            switch (field) {
                case DEGREES_FIELD: previousSuffix = prefix;        width = degreesFieldWidth; break;
                case MINUTES_FIELD: previousSuffix = degreesSuffix; width = minutesFieldWidth; break;
                case SECONDS_FIELD: previousSuffix = minutesSuffix; width = secondsFieldWidth; break;
                default:            previousSuffix = secondsSuffix; width = 0;
            }
            if (width == 0) {
                /*
                 * We reached the field after the last one. This is not necessarily FRACTIONAL_FIELD
                 * since a previous field can be marked as omitted. Before to stop the loop, write
                 * the pattern for the fractional part of degrees, minutes or seconds, followed by
                 * the suffix. In this case, 'previousSuffix' is actually associated to the integer
                 * part of the current field.
                 */
                width = fractionFieldWidth;
                if (width > 0) {
                    if (useDecimalSeparator) {
                        buffer.appendCodePoint(decimalSeparator);
                    }
                    final int optional = width - minimumFractionDigits;
                    symbol = Character.toLowerCase(symbol);
                    do {
                        if (width == optional) {
                            symbol = symbols[FRACTION_FIELD];
                        }
                        buffer.appendCodePoint(symbol);
                    }
                    while (--width > 0);
                }
                /*
                 * The code for writing the suffix is common to this "if" case (the fraction part of
                 * the pattern) and the "normal" case below. So we write the suffix outside the "if"
                 * block and will exit the main loop immediately after that.
                 */
            }
            if (previousSuffix != null) {
                buffer.append(previousSuffix);
            }
            if (previousWasOptional) {
                buffer.appendCodePoint(symbols[OPTIONAL_FIELD]);
            }
            if (width <= 0) {
                break; // The "if" case above has been executed for writing the fractional part, so we are done.
            }
            /*
             * This is the main part of the loop, before the final fractional part handled in the above "if" case.
             * Write the pattern for the integer part of degrees, minutes or second field.
             */
            symbol = symbols[field];
            do buffer.appendCodePoint(symbol);
            while (--width > 0);
            previousWasOptional = (optionalFields & (1 << field)) != 0;
        }
        return buffer.toString();
    }

    /**
     * Returns the given value as a byte. Values greater
     * than the maximal supported value are clamped.
     */
    private static byte toByte(final int n) {
        return (byte) Math.min(n, Byte.MAX_VALUE);
    }

    /**
     * Returns the minimum number of digits allowed in the fraction portion of the last field.
     * This value can be set by the repetition of {@code 'd'}, {@code 'm'} or {@code 's'} symbol
     * in the pattern.
     *
     * @return The minimum number of digits allowed in the fraction portion.
     *
     * @see DecimalFormat#getMinimumFractionDigits()
     */
    public int getMinimumFractionDigits() {
        return minimumFractionDigits;
    }

    /**
     * Sets the minimum number of digits allowed in the fraction portion of the last field.
     * If the given value is greater than the {@linkplain #getMaximumFractionDigits() maximum
     * number of fraction digits}, then that maximum number will be set to the given value too.
     *
     * @param count The minimum number of digits allowed in the fraction portion.
     *
     * @see DecimalFormat#setMinimumFractionDigits(int)
     */
    public void setMinimumFractionDigits(final int count) {
        ArgumentChecks.ensurePositive("count", count);
        if (!useDecimalSeparator) {
            throw new IllegalStateException(Errors.format(Errors.Keys.RequireDecimalSeparator));
        }
        maximumTotalWidth = 0; // Means "no restriction".
        minimumFractionDigits = toByte(count);
        if (minimumFractionDigits > fractionFieldWidth) {
            fractionFieldWidth = minimumFractionDigits;
        }
    }

    /**
     * Returns the maximum number of digits allowed in the fraction portion of the last field.
     * This value can be set by the repetition of {@code '#'} symbol in the pattern.
     *
     * @return The maximum number of digits allowed in the fraction portion.
     *
     * @see DecimalFormat#getMaximumFractionDigits()
     */
    public int getMaximumFractionDigits() {
        return fractionFieldWidth;
    }

    /**
     * Sets the maximum number of digits allowed in the fraction portion of the last field.
     * If the given value is smaller than the {@linkplain #getMinimumFractionDigits() minimum
     * number of fraction digits}, then that minimum number will be set to the given value too.
     *
     * @param count The maximum number of digits allowed in the fraction portion.
     *
     * @see DecimalFormat#setMaximumFractionDigits(int)
     */
    public void setMaximumFractionDigits(final int count) {
        ArgumentChecks.ensurePositive("count", count);
        if (!useDecimalSeparator) {
            throw new IllegalStateException(Errors.format(Errors.Keys.RequireDecimalSeparator));
        }
        maximumTotalWidth = 0; // Means "no restriction".
        fractionFieldWidth = toByte(count);
        if (fractionFieldWidth < minimumFractionDigits) {
            minimumFractionDigits = fractionFieldWidth;
        }
    }

    /**
     * Modifies, if needed, the pattern in order to fit formatted angles in the given maximum
     * total width. This method applies zero, one or more of the following changes, in that order:
     *
     * <ol>
     *   <li>If needed, reduce the {@linkplain #setMaximumFractionDigits(int) maximum number of
     *       fraction digits}.</li>
     *   <li>If omitting all fraction digits would not be sufficient for fitting a formatted
     *       angle in the given width, remove the seconds field (if any) from the pattern.</li>
     *   <li>If the above changes are not sufficient, remove the minutes field (if any) from
     *       the pattern.</li>
     *   <li>If the above changes are not sufficient, set the minimal width of degrees field to 1.</li>
     * </ol>
     *
     * Note that despite the above changes, formatted angles may still be larger than the given
     * width if that width is small, or if the formatted angles are too large in magnitude.
     *
     * <p>This method does not take into account the space needed for the hemisphere symbol when
     * formatting {@link Latitude} or {@link Longitude} objects.</p>
     *
     * @param width The maximum total width of formatted angle.
     */
    @SuppressWarnings("fallthrough")
    public void setMaximumWidth(int width) {
        ArgumentChecks.ensureStrictlyPositive("width", width);
        if (!useDecimalSeparator) {
            throw new IllegalStateException(Errors.format(Errors.Keys.RequireDecimalSeparator));
        }
        maximumTotalWidth = toByte(width);
        for (int field=PREFIX_FIELD; field<=SECONDS_FIELD; field++) {
            final int previousWidth = width;
            final String suffix;
            switch (field) {
                case PREFIX_FIELD:                              suffix = prefix;        break;
                case DEGREES_FIELD: width -= degreesFieldWidth; suffix = degreesSuffix; break;
                case MINUTES_FIELD: width -= minutesFieldWidth; suffix = minutesSuffix; break;
                case SECONDS_FIELD: width -= secondsFieldWidth; suffix = secondsSuffix; break;
                default: throw new AssertionError(field);
            }
            if (suffix != null) {
                width -= suffix.length();
            }
            /*
             * At this point, we computed the spaces remaining after formatting the angle up to
             * the field identified by the 'field' variable. If there is not enough space, remove
             * that field (if we are allowed to) and all subsequent fields from the pattern, then
             * reset the 'width' variable to its previous value.
             */
            if (width < 0) {
                switch (field) {
                    default:  width += (degreesFieldWidth-1); degreesFieldWidth = 1; // Fall through
                    case MINUTES_FIELD: minutesSuffix = null; minutesFieldWidth = 0; // Fall through
                    case SECONDS_FIELD: secondsSuffix = null; secondsFieldWidth = 0;
                }
                if (field >= MINUTES_FIELD) {
                    width = previousWidth;
                }
                break;
            }
        }
        /*
         * Removes 1 for the space needed by the decimal separator, then
         * set the maximum number of fraction digits to the remaining space.
         */
        if (--width < fractionFieldWidth) {
            fractionFieldWidth = toByte(Math.max(width, 0));
            if (fractionFieldWidth < minimumFractionDigits) {
                minimumFractionDigits = fractionFieldWidth;
            }
        }
    }

    /**
     * Returns the {@code *_FIELD} constant for the given field position, or -1 if none.
     */
    private static int getField(final FieldPosition position) {
        if (position != null) {
            final Format.Field field = position.getFieldAttribute();
            if (field instanceof Field) {
                return ((Field) field).field;
            }
            return position.getField();
        }
        return -1;
    }

    /**
     * Formats an angle. The angle will be formatted according the pattern given to the last call
     * of {@link #applyPattern(String)}.
     *
     * @param  angle Angle to format, in decimal degrees.
     * @return The formatted string.
     */
    public final String format(final double angle) {
        return format(angle, new StringBuffer(), null).toString();
    }

    /**
     * Formats an angle in the given buffer. The angle will be formatted according
     * the pattern given to the last call of {@link #applyPattern(String)}.
     *
     * @param angle
     *          Angle to format, in decimal degrees.
     * @param toAppendTo
     *          The buffer where to append the formatted angle.
     * @param pos
     *          An optional object where to store the position of the field in the formatted
     *          text, or {@code null} if this information is not wanted. This field position
     *          shall be created with one of the {@link Field} constants.
     *
     * @return The {@code toAppendTo} buffer, returned for method calls chaining.
     */
    @SuppressWarnings("PointlessBitwiseExpression")  // We rely on the compiler for simplifying the expression.
    public StringBuffer format(final double angle, StringBuffer toAppendTo, final FieldPosition pos) {
        final int offset = toAppendTo.length();
        final int fieldPos = getField(pos);
        if (isNaN(angle) || isInfinite(angle)) {
            toAppendTo = numberFormat().format(angle, toAppendTo, dummyFieldPosition());
            if (fieldPos >= DEGREES_FIELD && fieldPos <= SECONDS_FIELD) {
                pos.setBeginIndex(offset);
                pos.setEndIndex(toAppendTo.length());
            }
            return toAppendTo;
        }
        /*
         * Computes the numerical values of minutes and seconds fields.
         * If those fiels are not written, then store NaN.
         */
        double degrees = angle;
        double minutes = NaN;
        double seconds = NaN;
        int maximumFractionDigits = fractionFieldWidth;
        if (minutesFieldWidth != 0 && !isNaN(angle)) {
            minutes = abs(degrees - (degrees = truncate(degrees))) * 60;
            /*
             * Limit the maximal number of fraction digits to the amount of significant digits for a 'double' value.
             * The intend is to avoid non-significant garbage that are pure artifacts from the conversion from base
             * 2 to base 10.
             */
            final int n = fractionDigitsForDelta(Math.ulp(angle) * (secondsFieldWidth == 0 ? 60 : 3600), false);
            maximumFractionDigits = Math.max(minimumFractionDigits,
                                    Math.min(maximumFractionDigits, n - 1));
            final double p = pow10(maximumFractionDigits);
            if (secondsFieldWidth != 0) {
                seconds = (minutes - (minutes = truncate(minutes))) * 60;
                seconds = rint(seconds * p) / p; // Correction for rounding errors.
                if (seconds >= 60) { // We do not expect > 60 (only == 60), but let be safe.
                    seconds = 0;
                    minutes++;
                }
            } else {
                minutes = rint(minutes * p) / p; // Correction for rounding errors.
            }
            if (minutes >= 60) { // We do not expect > 60 (only == 60), but let be safe.
                minutes = 0;
                degrees += Math.signum(angle);
            }
            // Note: a previous version was doing a unconditional addition to the 'degrees' variable,
            // in the form 'degrees += correction'. However -0.0 + 0 == +0.0, while we really need to
            // preserve the sign of negative zero. See [SIS-120].
        }
        /*
         * Avoid formatting values like 12.01°N as 12°36″N because of the risk of confusion.
         * In such cases, force the formatting of minutes field as in 12°00′36″.
         */
        byte effectiveOptionalFields = optionalFields;
        if (showLeadingFields) {
            effectiveOptionalFields &= ~(1 << DEGREES_FIELD);
            if (minutes == 0 && ((effectiveOptionalFields & (1 << SECONDS_FIELD)) == 0 || seconds != 0)) {
                effectiveOptionalFields &= ~(1 << MINUTES_FIELD);
            }
        }
        /*
         * At this point the 'degrees', 'minutes' and 'seconds' variables contain the final values to format.
         * The following loop will format fields from DEGREES_FIELD to SECONDS_FIELD inclusive.
         * The NumberFormat will be reconfigured at each iteration.
         */
        int field = PREFIX_FIELD;
        if (prefix != null) {
            toAppendTo.append(prefix);
        }
        final NumberFormat numberFormat = numberFormat();
        boolean hasMore;
        do {
            int    width;
            double value;
            String suffix;
            switch (++field) {
                case DEGREES_FIELD: value=degrees; width=degreesFieldWidth; suffix=degreesSuffix; hasMore=(minutesFieldWidth != 0); break;
                case MINUTES_FIELD: value=minutes; width=minutesFieldWidth; suffix=minutesSuffix; hasMore=(secondsFieldWidth != 0); break;
                case SECONDS_FIELD: value=seconds; width=secondsFieldWidth; suffix=secondsSuffix; hasMore=false; break;
                default: throw new AssertionError(field);
            }
            /*
             * If the value is zero and the field is optional, propagate the sign to the next field
             * and skip the whole field. Otherwise process to the formatting of current field.
             */
            if (value == 0 && (effectiveOptionalFields & (1 << field)) != 0) {
                switch (field) {
                    case DEGREES_FIELD: minutes = Math.copySign(minutes, degrees); break;
                    case MINUTES_FIELD: seconds = Math.copySign(seconds, minutes); break;
                }
                continue;
            }
            /*
             * Configure the NumberFormat for the number of digits to write, but do not write anything yet.
             */
            if (hasMore) {
                numberFormat.setMinimumIntegerDigits(width);
                numberFormat.setMaximumFractionDigits(0);
            } else if (useDecimalSeparator) {
                numberFormat.setMinimumIntegerDigits(width);
                if (maximumTotalWidth != 0) {
                    /*
                     * If we are required to fit the formatted angle in some maximal total width
                     * (i.e. the user called the setMaximumWidth(int) method), compute the space
                     * available for fraction digits after we removed the space for the integer
                     * digits, the decimal separator (this is the +1 below) and the suffix.
                     */
                    int available = maximumTotalWidth - toAppendTo.codePointCount(offset, toAppendTo.length());
                    available -= (width + 1); // Remove the amount of code points that we plan to write.
                    if (suffix != null) {
                        width -= suffix.length();
                    }
                    for (double scale=pow10(width); value >= scale; scale *= 10) {
                        if (--available <= 0) break;
                    }
                    if (available < maximumFractionDigits) {
                        maximumFractionDigits = Math.max(available, 0);
                    }
                }
                numberFormat.setMinimumFractionDigits(minimumFractionDigits);
                numberFormat.setMaximumFractionDigits(maximumFractionDigits);
            } else {
                value *= pow10(fractionFieldWidth);
                numberFormat.setMaximumFractionDigits(0);
                numberFormat.setMinimumIntegerDigits(width + fractionFieldWidth);
            }
            /*
             * At this point, we known the value to format and the NumberFormat instance has been
             * configured. If the user asked for an attributed character iterator and assuming that
             * we want also the attributes produced by the NumberFormat, then we have to invoke the
             * heavy formatToCharacterIterator(…). Otherwise the usual format(…) method fits well.
             */
            final int startPosition = toAppendTo.length();
            if (characterIterator != null) {
                final FormattedCharacterIterator it = (FormattedCharacterIterator) characterIterator;
                it.append(numberFormat.formatToCharacterIterator(value), toAppendTo);
                if (suffix != null) {
                    toAppendTo.append(suffix);
                }
                final Number userObject;
                if (hasMore) {
                    userObject = JDK8.toIntExact(Math.round(value));
                } else {
                    // Use Float instead of Double because we don't want to give a false impression of accuracy
                    // (when formatting the seconds field, at least the 10 last bits of the 'double' value are
                    // non-significant).
                    userObject = (float) value;
                }
                it.addFieldLimit(Field.forCode(field), userObject, startPosition);
            } else {
                toAppendTo = numberFormat.format(value, toAppendTo, dummyFieldPosition());
                if (suffix != null) {
                    toAppendTo.append(suffix);
                }
            }
            if (field == fieldPos) {
                pos.setBeginIndex(startPosition);
                pos.setEndIndex(toAppendTo.length());
            }
        } while (hasMore);
        return toAppendTo;
    }

    /**
     * Formats an angle, latitude or longitude value in the given buffer.
     * The angular values will be formatted according the pattern given to the
     * last call of {@link #applyPattern(String)}, with some variations that
     * depend on the {@code value} class:
     *
     * <ul>
     *   <li>If {@code value} is a {@link Latitude} instance, then the value is formatted as a
     *       positive angle followed by the "N" (positive value) or "S" (negative value) symbol.</li>
     *   <li>If {@code value} is a {@link Longitude} instance, then the value is formatted as a
     *       positive angle followed by the "E" (positive value) or "W" (negative value) symbol.</li>
     *   <li>If {@code value} is any {@link Angle} other than a {@code Latitude} or {@code Longitude},
     *       then it is formatted as by the {@link #format(double, StringBuffer, FieldPosition)}
     *       method.</li>
     * </ul>
     *
     * @param value
     *          {@link Angle} object to format.
     * @param toAppendTo
     *          The buffer where to append the formatted angle.
     * @param pos
     *          An optional object where to store the position of the field in the formatted
     *          text, or {@code null} if this information is not wanted. This field position
     *          shall be created with one of the {@link Field} constants.
     *
     * @return The {@code toAppendTo} buffer, returned for method calls chaining.
     * @throws IllegalArgumentException if {@code value} if not an instance of {@link Angle}.
     */
    @Override
    public StringBuffer format(final Object value, StringBuffer toAppendTo, final FieldPosition pos)
            throws IllegalArgumentException
    {
        if (value instanceof Latitude) {
            return format(((Latitude) value).degrees(), toAppendTo, pos, NORTH, SOUTH);
        }
        if (value instanceof Longitude) {
            return format(((Longitude) value).degrees(), toAppendTo, pos, EAST, WEST);
        }
        if (value instanceof Angle) {
            return format(((Angle) value).degrees(), toAppendTo, pos);
        }
        ArgumentChecks.ensureNonNull("value", value);
        throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentClass_3,
                "value", Angle.class, value.getClass()));
    }

    /**
     * Formats a latitude or longitude value in the given buffer. The magnitude of the
     * angular value will be formatted according the pattern given to the last call of
     * {@link #applyPattern(String)}, and one of the given suffix will be appended
     * according the angle sign.
     */
    private StringBuffer format(final double angle, StringBuffer toAppendTo,
            final FieldPosition pos, final char positiveSuffix, final char negativeSuffix)
    {
        try {
            showLeadingFields = true;
            toAppendTo = format(abs(angle), toAppendTo, pos);
        } finally {
            showLeadingFields = false;
        }
        final int startPosition = toAppendTo.length();
        final char suffix = isNegative(angle) ? negativeSuffix : positiveSuffix;
        toAppendTo.append(suffix);
        if (getField(pos) == HEMISPHERE_FIELD) {
            pos.setBeginIndex(startPosition);
            pos.setEndIndex(toAppendTo.length());
        }
        if (characterIterator != null) {
            ((FormattedCharacterIterator) characterIterator).addFieldLimit(
                    Field.HEMISPHERE, suffix, startPosition);
        }
        return toAppendTo;
    }

    /**
     * Formats an angle, latitude or longitude value as an attributed character iterator.
     * Callers can iterate and queries the attribute values as in the following example:
     *
     * {@preformat java
     *     AttributedCharacterIterator it = angleFormat.formatToCharacterIterator(myAngle);
     *     for (char c=it.first(); c!=AttributedCharacterIterator.DONE; c=c.next()) {
     *         // 'c' is a character from the formatted string.
     *         if (it.getAttribute(AngleFormat.Field.MINUTES) != null) {
     *             // If we enter this block, then the character 'c' is part of the minutes field,
     *             // This field extends from it.getRunStart(MINUTES) to it.getRunLimit(MINUTES).
     *         }
     *     }
     * }
     *
     * Alternatively, if the current {@linkplain AttributedCharacterIterator#getIndex() iterator
     * index} is before the start of the minutes field, then the starting position of that field
     * can be obtained directly by {@code it.getRunLimit(MINUTES)}. If the current iterator index
     * is inside the minutes field, then the above method call will rather returns the end of that
     * field. The same strategy works for other all fields too.
     *
     * <p>The returned character iterator contains all {@link java.text.NumberFormat.Field}
     * attributes in addition to the {@link Field} ones. Consequently the same character may
     * have more than one attribute. For example when formatting 45°30′15.0″N, then:</p>
     *
     * <ul>
     *   <li>The {@code 45°}   part has the {@link Field#DEGREES} attribute.</li>
     *   <li>The {@code 30′}   part has the {@link Field#MINUTES} attribute.</li>
     *   <li>The {@code 15.0″} part has the {@link Field#SECONDS} attribute.</li>
     *   <li>The {@code N}     part has the {@link Field#HEMISPHERE} attribute.</li>
     *   <li>The {@code 45}, {@code 30} and {@code 15} parts have the
     *       {@link java.text.NumberFormat.Field#INTEGER} attribute.</li>
     *   <li>The {@code .} part has the {@link java.text.NumberFormat.Field#DECIMAL_SEPARATOR} attribute.</li>
     *   <li>The last {@code 0} part has the {@link java.text.NumberFormat.Field#FRACTION} attribute.</li>
     * </ul>
     *
     * In Apache SIS implementation, the returned character iterator also implements the
     * {@link CharSequence} interface for convenience.
     *
     * @param  value {@link Angle} object to format.
     * @return A character iterator together with the attributes describing the formatted value.
     * @throws IllegalArgumentException if {@code value} if not an instance of {@link Angle}.
     */
    @Override
    public AttributedCharacterIterator formatToCharacterIterator(final Object value) {
        final StringBuffer buffer = new StringBuffer();
        final FormattedCharacterIterator it = new FormattedCharacterIterator(buffer);
        try {
            characterIterator = it;
            format(value, buffer, null);
        } finally {
            characterIterator = null;
        }
        return it;
    }

    /**
     * Ignores a field suffix, then returns the identifier of the suffix just skipped.
     * This method is invoked by {@link #parse(String, ParsePosition)} for determining
     * what was the field it just parsed. For example if we just parsed "48°12'", then
     * this method will skip the "°" part and returns {@link #DEGREES_FIELD}.
     *
     * <p>This method skips whitespaces before the suffix, then compares the characters
     * with the suffix specified to {@link #applyPattern(String)}. If the suffix has not
     * been recognized, then this method will compares against the standard ', ° and "
     * ASCII symbols.</p>
     *
     * @param source
     *          The string being parsed.
     * @param pos
     *          On input, index of the first {@code source} character to read.
     *          On output, index after the last suffix character.
     * @param expectedField
     *          First field to verify. For example a value of {@link #MINUTES_FIELD} means that
     *          the suffix for minute and seconds shall be verified before degrees.
     * @return The {@code *_FIELD} constant for the suffix which has been found, or a value
     *         outside those constants if no suffix matched.
     */
    private int skipSuffix(final String source, final ParsePosition pos, final int expectedField) {
        int field = expectedField;
        int start = pos.getIndex();
        final int length = source.length();
        assert field >= PREFIX_FIELD && field <= SECONDS_FIELD : field;
        do {
            int index = start;
            final String toSkip;
            switch (field) {
                case  PREFIX_FIELD: toSkip = prefix;        break;
                case DEGREES_FIELD: toSkip = degreesSuffix; break;
                case MINUTES_FIELD: toSkip = minutesSuffix; break;
                case SECONDS_FIELD: toSkip = secondsSuffix; break;
                default: throw new AssertionError(field);
            }
            if (toSkip != null) {
                int c;
                do {
                    if (source.startsWith(toSkip, index)) {
                        pos.setIndex(index + toSkip.length());
                        return field;
                    }
                    if (index >= length) break;
                    c = source.codePointAt(index);
                    index += Character.charCount(c);
                }
                while (Character.isSpaceChar(c)); // Method shall be consistent with skipSpaces(…)
            }
            if (++field > SECONDS_FIELD) {
                field = PREFIX_FIELD;
            }
        } while (field != expectedField);
        /*
         * No suffix from the pattern has been found in the supplied text.
         * Check for the usual symbols, if we were allowe to.
         */
        if (isFallbackAllowed) {
            int c;
            do {
                if (start >= length) {
                    return Integer.MIN_VALUE;
                }
                c = source.codePointAt(start);
                start += Character.charCount(c);
            }
            while (Character.isSpaceChar(c)); // Method shall be consistent with skipSpaces(…)
            switch (c) {
                case '°' :            pos.setIndex(start); return DEGREES_FIELD;
                case '′' : case '\'': pos.setIndex(start); return MINUTES_FIELD;
                case '″' : case '"' : pos.setIndex(start); return SECONDS_FIELD;
            }
        }
        return Integer.MIN_VALUE; // Unknown field.
    }

    /**
     * Returns the index of the first non-space character in the given string.
     * This method performs the same work than {@code CharSequences.skipLeadingWhitespaces},
     * except that it tests for spaces using the {@link Character#isSpaceChar(int)} method
     * instead than {@link Character#isWhitespace(int)}. The reason is that we really want
     * to skip no-break spaces, since they are often used inside a single entity (e.g. the
     * group separator in numbers formatted using the French locale).  Furthermore we do
     * not want to skip tabulations or line feeds, since they are unlikely to be part of
     * the angle to parse.
     *
     * @param  source The string being parsed.
     * @param  index  Index of the first {@code source} character to read.
     * @param  length The length of {@code source}.
     * @return Index of the first non-space character, or the end of string if none.
     */
    private static int skipSpaces(final String source, int index, final int length) {
        while (index < length) {
            final int c = source.codePointAt(index);
            if (!Character.isSpaceChar(c)) break;
            index += Character.charCount(c);
        }
        return index;
    }

    /**
     * Parses the given string as an angle. This method can parse the string even if it is not
     * strictly compliant to the expected pattern. For example if {@link #isFallbackAllowed()}
     * is {@code true}, then this method will parse "{@code 48°12.34'}" correctly even if the
     * expected pattern was "{@code DDMM.mm}" (i.e. the string should have been "{@code 4812.34}").
     *
     * <p>If the given string ends with a "N" or "S" hemisphere symbol, then this method returns
     * an instance of {@link Latitude}. Otherwise if the string ends with a "E" or "W" symbol,
     * then this method returns an instance of {@link Longitude}. Otherwise this method returns
     * an instance of {@link Angle}.</p>
     *
     * <p>This method is stricter than the {@link #parse(String)} method regarding whitespaces
     * between the degrees, minutes and seconds fields. This is because whitespaces could be
     * used as a separator for other kinds of values. If the string is known to contain only
     * an angle value, use {@code parse(String)} instead.</p>
     *
     * @param  source The string to parse.
     * @param  pos    On input, index of the first {@code source} character to read.
     *                On output, index after the last parsed character.
     * @return The parsed string as an {@link Angle}, {@link Latitude} or {@link Longitude} object.
     *
     * @see #isFallbackAllowed()
     */
    public Angle parse(final String source, final ParsePosition pos) {
        return parse(source, pos, false);
    }

    /**
     * Parses the given string as an angle. The {@code spaceAsSeparator} additional argument
     * specifies if spaces can be accepted as a field separator. For example if {@code true},
     * then "45 30" will be parsed as "45°30".
     */
    @SuppressWarnings({"fallthrough", "UnnecessaryLabelOnBreakStatement"})
    private Angle parse(final String source, final ParsePosition pos, final boolean spaceAsSeparator) {
        double degrees;
        double minutes   = NaN;
        double seconds   = NaN;
        final int length = source.length();
        final NumberFormat numberFormat = numberFormat();
        ///////////////////////////////////////////////////////////////////////////////
        // BLOCK A: Assign values to 'degrees', 'minutes' and 'seconds' variables.   //
        //          This block does not take the hemisphere field in account, and    //
        //          values will need adjustment if decimal separator is missing.     //
        //          The { } block is for restricting the scope of local variables.   //
        ///////////////////////////////////////////////////////////////////////////////
        {
            /*
             * Extract the prefix, if any. If we find a degrees, minutes or seconds suffix
             * before to have meet any number, we will consider that as a parsing failure.
             */
            final int indexStart = pos.getIndex();
            int index = skipSuffix(source, pos, PREFIX_FIELD);
            if (index >= DEGREES_FIELD && index <= SECONDS_FIELD) {
                pos.setErrorIndex(indexStart);
                pos.setIndex(indexStart);
                return null;
            }
            index = skipSpaces(source, pos.getIndex(), length);
            pos.setIndex(index);
            /*
             * Parse the degrees field. If there is no separator between degrees, minutes
             * and seconds, then the parsed number may actually include many fields (e.g.
             * "DDDMMmmm"). The separation will be done later.
             */
            Number fieldObject = numberFormat.parse(source, pos);
            if (fieldObject == null) {
                pos.setIndex(indexStart);
                if (pos.getErrorIndex() < indexStart) {
                    pos.setErrorIndex(index);
                }
                return null;
            }
            degrees = fieldObject.doubleValue();
            int indexEndField = pos.getIndex();
            boolean missingDegrees = true;
BigBoss:    switch (skipSuffix(source, pos, DEGREES_FIELD)) {
                /* ------------------------------------------
                 * STRING ANALYSIS FOLLOWING PRESUMED DEGREES
                 * ------------------------------------------
                 * The degrees value is followed by the prefix for angles.
                 * Stop parsing, since the remaining characters are for an other angle.
                 */
                case PREFIX_FIELD: {
                    pos.setIndex(indexEndField);
                    break BigBoss;
                }
                /* ------------------------------------------
                 * STRING ANALYSIS FOLLOWING PRESUMED DEGREES
                 * ------------------------------------------
                 * Found the seconds suffix instead then the degrees suffix. Move 'degrees'
                 * value to 'seconds' and stop parsing, since seconds are the last field.
                 */
                case SECONDS_FIELD: {
                    seconds = degrees;
                    degrees = NaN;
                    break BigBoss;
                }
                /* ------------------------------------------
                 * STRING ANALYSIS FOLLOWING PRESUMED DEGREES
                 * ------------------------------------------
                 * No recognized suffix after degrees. If "spaces as separator" are allowed and
                 * a minutes field is expected after the degrees field, we will pretent that we
                 * found the minutes suffix. Otherwise stop parsing.
                 */
                default: {
                    if (!spaceAsSeparator || !isFallbackAllowed || minutesFieldWidth == 0) {
                        break BigBoss;
                    }
                    // Fall through for parsing minutes.
                }
                /* ------------------------------------------
                 * STRING ANALYSIS FOLLOWING PRESUMED DEGREES
                 * ------------------------------------------
                 * After the degrees field, check if there is a minute field.
                 * We proceed as for degrees (parse a number, skip the suffix).
                 */
                case DEGREES_FIELD: {
                    final int indexStartField = pos.getIndex();
                    index = skipSpaces(source, indexStartField, length);
                    if (!spaceAsSeparator && index != indexStartField) {
                        break BigBoss;
                    }
                    pos.setIndex(index);
                    fieldObject = numberFormat.parse(source, pos);
                    if (fieldObject == null) {
                        pos.setIndex(indexStartField);
                        break BigBoss;
                    }
                    indexEndField = pos.getIndex();
                    minutes = fieldObject.doubleValue();
                    switch (skipSuffix(source, pos, (minutesFieldWidth != 0) ? MINUTES_FIELD : PREFIX_FIELD)) {
                        /* ------------------------------------------
                         * STRING ANALYSIS FOLLOWING PRESUMED MINUTES
                         * ------------------------------------------
                         * Found the expected suffix, nothing special to do.
                         * Continue the outer switch for parsing seconds.
                         */
                        case MINUTES_FIELD: {
                            break; // Continue outer switch for parsing seconds.
                        }
                        /* ------------------------------------------
                         * STRING ANALYSIS FOLLOWING PRESUMED MINUTES
                         * ------------------------------------------
                         * Found the seconds suffix instead then the minutes suffix. Move 'minutes'
                         * value to 'seconds' and stop parsing, since seconds are the last field.
                         */
                        case SECONDS_FIELD: {
                            seconds = minutes;
                            minutes = NaN;
                            break BigBoss;
                        }
                        /* ------------------------------------------
                         * STRING ANALYSIS FOLLOWING PRESUMED MINUTES
                         * ------------------------------------------
                         * No suffix has been found. This is normal if the pattern doesn't specify
                         * a minutes field, in which case we reject the number that we just parsed.
                         * However if minutes were expected and space separators are allowed, then
                         * check for seconds.
                         */
                        default: {
                            if (spaceAsSeparator && isFallbackAllowed && minutesFieldWidth != 0) {
                                break; // Continue outer switch for parsing seconds.
                            }
                            // Fall through for rejecting the minutes.
                        }
                        /* ------------------------------------------
                         * STRING ANALYSIS FOLLOWING PRESUMED MINUTES
                         * ------------------------------------------
                         * Found the degrees suffix instead than the minutes suffix.
                         * This means that the number we have just read belong to an
                         * other angle. Stop the parsing before that number.
                         */
                        case DEGREES_FIELD: {
                            pos.setIndex(indexStartField);
                            minutes = NaN;
                            break BigBoss;
                        }
                        /* ------------------------------------------
                         * STRING ANALYSIS FOLLOWING PRESUMED MINUTES
                         * ------------------------------------------
                         * Found the prefix of an other angle. Accept the number that
                         * we have just parsed despite the missing minutes suffix, and
                         * stop parsing before the prefix.
                         */
                        case PREFIX_FIELD: {
                            pos.setIndex(indexEndField);
                            break BigBoss;
                        }
                    }
                    missingDegrees = false;
                    // Fall through for parsing the seconds.
                }
                /* -----------------------------------------------------
                 * STRING ANALYSIS FOLLOWING PRESUMED DEGREES OR MINUTES
                 * -----------------------------------------------------
                 * If a minutes field was found without degrees, move the 'degrees'
                 * value to 'minutes'. Then try to parse the next number as seconds.
                 */
                case MINUTES_FIELD: {
                    if (missingDegrees) {
                        minutes = degrees;
                        degrees = NaN;
                    }
                    final int indexStartField = pos.getIndex();
                    index = skipSpaces(source, indexStartField, length);
                    if (!spaceAsSeparator && index != indexStartField) {
                        break BigBoss;
                    }
                    pos.setIndex(index);
                    fieldObject = numberFormat.parse(source, pos);
                    if (fieldObject == null) {
                        pos.setIndex(indexStartField);
                        break;
                    }
                    indexEndField = pos.getIndex();
                    seconds = fieldObject.doubleValue();
                    switch (skipSuffix(source, pos, (secondsFieldWidth != 0) ? MINUTES_FIELD : PREFIX_FIELD)) {
                        /* ------------------------------------------
                         * STRING ANALYSIS FOLLOWING PRESUMED SECONDS
                         * ------------------------------------------
                         * Found the expected second suffix. We are done.
                         */
                        case SECONDS_FIELD: {
                            break;
                        }
                        /* ------------------------------------------
                         * STRING ANALYSIS FOLLOWING PRESUMED SECONDS
                         * ------------------------------------------
                         * No suffix has been found. This is normal if the pattern doesn't specify
                         * a seconds field, in which case we reject the number that we just parsed.
                         * However if seconds were expected and space separators are allowed, then
                         * accept the value.
                         */
                        default: {
                            if (isFallbackAllowed && secondsFieldWidth != 0) {
                                break;
                            }
                            // Fall through for rejecting the seconds.
                        }
                        /* ------------------------------------------
                         * STRING ANALYSIS FOLLOWING PRESUMED SECONDS
                         * ------------------------------------------
                         * Found the degrees or minutes suffix instead than the seconds suffix.
                         * This means that the number we have just read belong to an other angle.
                         * Stop the parsing before that number.
                         */
                        case MINUTES_FIELD:
                        case DEGREES_FIELD: {
                            pos.setIndex(indexStartField);
                            seconds = NaN;
                            break;
                        }
                        /* ------------------------------------------
                         * STRING ANALYSIS FOLLOWING PRESUMED SECONDS
                         * ------------------------------------------
                         * Found the prefix of an other angle. Accept the number that
                         * we have just parsed despite the missing seconds suffix, and
                         * stop parsing before the prefix.
                         */
                        case PREFIX_FIELD: {
                            pos.setIndex(indexEndField);
                            break BigBoss;
                        }
                    }
                }
            }
        }
        ////////////////////////////////////////////////////////////////////
        // BLOCK B: Handle the case when there is no decimal separator.   //
        //          Then combine the fields into a decimal degrees value. //
        ////////////////////////////////////////////////////////////////////
        if (isNegative(minutes)) {
            seconds = -seconds;
        }
        if (isNegative(degrees)) {
            minutes = -minutes;
            seconds = -seconds;
        }
        if (!useDecimalSeparator) {
            final double facteur = pow10(fractionFieldWidth);
            if (secondsFieldWidth != 0) {
                if (minutesSuffix == null && isNaN(seconds)) {
                    if (degreesSuffix == null && isNaN(minutes)) {
                        degrees /= facteur;
                    } else {
                        minutes /= facteur;
                    }
                } else {
                    seconds /= facteur;
                }
            } else if (isNaN(seconds)) {
                if (minutesFieldWidth != 0) {
                    if (degreesSuffix == null && isNaN(minutes)) {
                        degrees /= facteur;
                    } else {
                        minutes /= facteur;
                    }
                } else if (isNaN(minutes)) {
                    degrees /= facteur;
                }
            }
        }
        /*
         * If there is no separation between degrees and minutes fields (e.g. if the pattern
         * is "DDDMMmmm"), then the 'degrees' variable contains degrees, minutes and seconds
         * in sexagesimal units. We need to convert to decimal units.
         */
        if (minutesSuffix == null && secondsFieldWidth != 0 && isNaN(seconds)) {
            double facteur = pow10(secondsFieldWidth);
            if (degreesSuffix == null && minutesFieldWidth != 0 && isNaN(minutes)) {
                ///////////////////
                //// DDDMMSS.s ////
                ///////////////////
                seconds  = degrees;
                minutes  = truncate(degrees / facteur);
                seconds -= minutes * facteur;
                facteur  = pow10(minutesFieldWidth);
                degrees  = truncate(minutes / facteur);
                minutes  -= degrees * facteur;
            } else {
                ////////////////////
                //// DDD°MMSS.s ////
                ////////////////////
                seconds  = minutes;
                minutes  = truncate(minutes / facteur);
                seconds -= minutes*facteur;
            }
        } else if (degreesSuffix == null && minutesFieldWidth != 0 && isNaN(minutes)) {
            /////////////////
            //// DDDMM.m ////
            /////////////////
            final double facteur = pow10(minutesFieldWidth);
            minutes  = degrees;
            degrees  = truncate(degrees / facteur);
            minutes -= degrees * facteur;
        }
        pos.setErrorIndex(-1);
        if ( isNaN(degrees)) degrees  = 0;
        if (!isNaN(minutes)) degrees += minutes /   60;
        if (!isNaN(seconds)) degrees += seconds / 3600;
        /////////////////////////////////////////////////////////
        // BLOCK C: Check for hemisphere suffix (N, S, E or W) //
        //          after the angle string representation.     //
        /////////////////////////////////////////////////////////
        for (int index = pos.getIndex(); index < length;) {
            final int c = source.codePointAt(index);
            index += Character.charCount(c);
            switch (Character.toUpperCase(c)) {
                case NORTH: pos.setIndex(index); return new Latitude ( degrees);
                case SOUTH: pos.setIndex(index); return new Latitude (-degrees);
                case EAST : pos.setIndex(index); return new Longitude( degrees);
                case WEST : pos.setIndex(index); return new Longitude(-degrees);
            }
            if (!Character.isSpaceChar(c)) { // Method shall be consistent with skipSpaces(…)
                break;
            }
        }
        return new Angle(degrees);
    }

    /**
     * Parses the given string as an angle. This full string is expected to represents an
     * angle value. This assumption allows {@code parse(String)} to be more tolerant than
     * {@link #parse(String, ParsePosition)} regarding white spaces between degrees, minutes
     * and seconds fields.
     *
     * @param  source The string to parse.
     * @return The parsed string as an {@link Angle}, {@link Latitude} or {@link Longitude} object.
     * @throws ParseException If the string can not be fully parsed.
     *
     * @see #isFallbackAllowed()
     */
    public Angle parse(final String source) throws ParseException {
        final ParsePosition pos = new ParsePosition(0);
        final Angle angle = parse(source, pos, true);
        final int offset = pos.getIndex();
        final int length = source.length();
        if (skipSpaces(source, offset, length) < length) {
            throw new LocalizedParseException(locale, Angle.class, source, pos);
        }
        return angle;
    }

    /**
     * Parses a substring as an object.
     * The default implementation delegates to {@link #parse(String, ParsePosition)}.
     *
     * @param  source The string to parse.
     * @param  pos The position where to start parsing.
     * @return The parsed string as an {@link Angle}, {@link Latitude} or {@link Longitude} object.
     */
    @Override
    public Object parseObject(final String source, final ParsePosition pos) {
        return parse(source, pos);
    }

    /**
     * Parses the given string as an object.
     * The default implementation delegates to {@link #parse(String)}.
     *
     * @param  source The string to parse.
     * @return The parsed string as an {@link Angle}, {@link Latitude} or {@link Longitude} object.
     * @throws ParseException If the string can not been fully parsed.
     */
    @Override
    public Object parseObject(final String source) throws ParseException {
        return parse(source);
    }

    /**
     * Returns {@code true} if the {@link #parse(String, ParsePosition) parse} methods are allowed
     * to fallback on the build-in default symbols if the string to parse doesn't match the
     * {@linkplain #applyPattern(String) applied pattern}.
     *
     * @return {@code true} if the ASCII quote characters are allowed at parsing time.
     */
    public boolean isFallbackAllowed() {
        return isFallbackAllowed;
    }

    /**
     * Sets whether the {@link #parse(String, ParsePosition) parse} methods are allowed to
     * fallback on the build-in default symbols if the string to parse doesn't match the
     * {@linkplain #applyPattern(String) applied pattern}. The build-in fallback is:
     *
     * <ul>
     *   <li>{@code °} (an extended-ASCII character) or space (in {@link #parse(String)} method only) for degrees.</li>
     *   <li>{@code '} (an ASCII character) or {@code ′} (the default Unicode character) for minutes.</li>
     *   <li>{@code "} (an ASCII character) or {@code ″} (the default Unicode character) for seconds.</li>
     * </ul>
     *
     * The default value is {@code true}, because many end-users will not enter the Unicode
     * {@code ′} and {@code ″} symbols. However developers may need to set this flag to
     * {@code false} if those ASCII symbols are used in a wider context (for example the
     * {@code "} character for quoting strings).
     *
     * @param allowed {@code true} if the ASCII quote characters are allowed at parsing time.
     */
    public void setFallbackAllowed(final boolean allowed) {
        isFallbackAllowed = allowed;
    }

    /**
     * Returns this formatter locale. This is the locale specified at construction time if any,
     * or the {@linkplain Locale#getDefault() default locale} at construction time otherwise.
     *
     * @return This formatter locale (never {@code null}).
     */
    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns a clone of this {@code AngleFormat}.
     *
     * @return A clone of this format.
     */
    @Override
    public AngleFormat clone() {
        final AngleFormat clone = (AngleFormat) super.clone();
        clone.numberFormat = null;
        clone.dummyFieldPosition = null;
        return clone;
    }

    /**
     * Returns a "hash value" for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(degreesFieldWidth, minutesFieldWidth, secondsFieldWidth, fractionFieldWidth,
                minimumFractionDigits, useDecimalSeparator, isFallbackAllowed, optionalFields, locale,
                prefix, degreesSuffix, minutesSuffix, secondsSuffix) ^ (int) serialVersionUID;
    }

    /**
     * Compares this format with the specified object for equality.
     *
     * @param object The object to compare with this angle format for equality.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object != null && getClass() == object.getClass()) {
            final  AngleFormat cast = (AngleFormat) object;
            return degreesFieldWidth     == cast.degreesFieldWidth     &&
                   minutesFieldWidth     == cast.minutesFieldWidth     &&
                   secondsFieldWidth     == cast.secondsFieldWidth     &&
                   fractionFieldWidth    == cast.fractionFieldWidth    &&
                   minimumFractionDigits == cast.minimumFractionDigits &&
                   useDecimalSeparator   == cast.useDecimalSeparator   &&
                   isFallbackAllowed     == cast.isFallbackAllowed     &&
                   optionalFields        == cast.optionalFields        &&
                   Objects.equals(locale,        cast.locale)          &&
                   Objects.equals(prefix,        cast.prefix)          &&
                   Objects.equals(degreesSuffix, cast.degreesSuffix)   &&
                   Objects.equals(minutesSuffix, cast.minutesSuffix)   &&
                   Objects.equals(secondsSuffix, cast.secondsSuffix);
        }
        return false;
    }

    /**
     * Returns a string representation of this object for debugging purpose.
     */
    @Debug
    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + toPattern() + ']';
    }
}
