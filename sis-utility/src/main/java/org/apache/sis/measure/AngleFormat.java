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
import net.jcip.annotations.NotThreadSafe;

import org.apache.sis.util.Debug;
import org.apache.sis.util.Localized;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;

import static java.lang.Math.abs;
import static java.lang.Math.rint;
import static java.lang.Double.NaN;
import static java.lang.Double.isNaN;
import static java.lang.Double.isInfinite;
import static org.apache.sis.math.MathFunctions.pow10;
import static org.apache.sis.math.MathFunctions.truncate;
import static org.apache.sis.math.MathFunctions.isNegative;

// Related to JDK7
import java.util.Objects;


/**
 * Parses and formats angles according a specified pattern. The pattern is a string
 * containing any characters, with a special meaning for the following characters:
 *
 * <blockquote><table class="sis">
 *   <tr><th>Symbol</th><th>Meaning</th></tr>
 *   <tr><td>{@code D}</td><td>The integer part of degrees</td></tr>
 *   <tr><td>{@code d}</td><td>The fractional part of degrees</td></tr>
 *   <tr><td>{@code M}</td><td>The integer part of minutes</td></tr>
 *   <tr><td>{@code m}</td><td>The fractional part of minutes</td></tr>
 *   <tr><td>{@code S}</td><td>The integer part of seconds</td></tr>
 *   <tr><td>{@code s}</td><td>The fractional part of seconds</td></tr>
 *   <tr><td>{@code #}</td><td>Fraction digits shown only if non-zero</td></tr>
 *   <tr><td>{@code .}</td><td>The decimal separator</td></tr>
 * </table></blockquote>
 *
 * Upper-case letters {@code D}, {@code M} and {@code S} stand for the integer parts of degrees,
 * minutes and seconds respectively. They shall appear in this order. For example {@code M'D} is
 * illegal because "M" and "S" are in reverse order; {@code D°S} is illegal too because "M" is
 * missing between "D" and "S".
 *
 * <p>Lower-case letters {@code d}, {@code m} and {@code s} stand for fractional parts of degrees,
 * minutes and seconds respectively. Only one of those may appears in a pattern, and it must be
 * the last special symbol. For example {@code D.dd°MM'} is illegal because "d" is followed by
 * "M"; {@code D.mm} is illegal because "m" is not the fractional part of "D".</p>
 *
 * <p>The number of occurrence of {@code D}, {@code M}, {@code S} and their lower-case counterpart
 * is the number of digits to format. For example, {@code DD.ddd} will format angles with two digits
 * for the integer part and three digits for the fractional part (e.g. 4.4578 will be formatted as
 * "04.458").</p>
 *
 * <p>Separator characters like {@code °}, {@code ′} and {@code ″} are inserted "as-is" in the
 * formatted string, except the decimal separator dot ({@code .}) which is replaced by the
 * local-dependent decimal separator. Separator characters may be completely omitted;
 * {@code AngleFormat} will still differentiate degrees, minutes and seconds fields according
 * the pattern. For example, "{@code 0480439}" with the pattern {@code DDDMMmm} will be parsed
 * as 48°04.39'.</p>
 *
 * <p>The following table gives some pattern examples:</p>
 *
 * <blockquote><table class="sis">
 *   <tr><th>Pattern           </th>  <th>Example   </th></tr>
 *   <tr><td>{@code DD°MM′SS″ }</td>  <td>48°30′00″ </td></tr>
 *   <tr><td>{@code DD°MM′    }</td>  <td>48°30′    </td></tr>
 *   <tr><td>{@code DD.ddd    }</td>  <td>48.500    </td></tr>
 *   <tr><td>{@code DD.###    }</td>  <td>48.5      </td></tr>
 *   <tr><td>{@code DDMM      }</td>  <td>4830      </td></tr>
 *   <tr><td>{@code DDMMSS    }</td>  <td>483000    </td></tr>
 * </table></blockquote>
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @since   0.3 (derived from geotk-1.0)
 * @version 0.3
 * @module
 *
 * @see Angle
 * @see Latitude
 * @see Longitude
 */
@NotThreadSafe
public class AngleFormat extends Format implements Localized {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 4320403817210439764L;

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
     */
    public static final int DEGREES_FIELD = 0;

    /**
     * Constant for minutes field. When formatting a string, this value may be specified to the
     * {@link FieldPosition} constructor in order to get the bounding index where minutes have
     * been written.
     */
    public static final int MINUTES_FIELD = 1;

    /**
     * Constant for seconds field. When formatting a string, this value may be specified to the
     * {@link FieldPosition} constructor in order to get the bounding index where seconds have
     * been written.
     */
    public static final int SECONDS_FIELD = 2;

    /**
     * Constant for the fractional part of the degrees, minutes or seconds field. When formatting
     * a string, this value may be specified to the {@link FieldPosition} constructor in order to
     * get the bounding index where seconds have been written.
     */
    public static final int FRACTION_FIELD = 3;

    /**
     * Constant for hemisphere field. When formatting a string, this value may be specified to the
     * {@link FieldPosition} constructor in order to get the bounding index where the hemisphere
     * symbol has been written.
     */
    public static final int HEMISPHERE_FIELD = 4;

    /**
     * Symbols for degrees (0), minutes (1), seconds (2) and optional fraction digits (3).
     * The index of each symbol shall be equals to the corresponding {@code *_FIELD} constant.
     */
    private static final char[] SYMBOLS = {'D', 'M', 'S', '#'};

    /**
     * The locale specified at construction time.
     */
    private final Locale locale;

    /**
     * Minimal amount of spaces to be used by the degrees, minutes and seconds fields,
     * and by the decimal digits. A value of 0 means that the field is not formatted.
     * {@code fractionFieldWidth} applies to the last non-zero field.
     */
    private byte degreesFieldWidth,
                 minutesFieldWidth,
                 secondsFieldWidth,
                 fractionFieldWidth,
                 minimumFractionDigits;

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
    private transient FieldPosition dummy;

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
        if (dummy == null) {
            dummy = new FieldPosition(0);
        }
        return dummy;
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
        this(Locale.getDefault(Locale.Category.FORMAT));
    }

    /**
     * Constructs a new {@code AngleFormat} for the default pattern and the specified locale.
     *
     * @param  locale The locale to use.
     */
    public AngleFormat(final Locale locale) {
        this.locale = locale;
        degreesFieldWidth     = 1;
        minutesFieldWidth     = 2;
        secondsFieldWidth     = 2;
        minimumFractionDigits = 6;
        degreesSuffix         = "°";
        minutesSuffix         = "′";
        secondsSuffix         = "″";
        useDecimalSeparator   = true;
    }

    /**
     * Constructs a new {@code AngleFormat} for the specified pattern and the current default locale.
     *
     * @param  pattern Pattern to use for parsing and formatting angles.
     *         See class description for an explanation of pattern syntax.
     * @throws IllegalArgumentException If the specified pattern is illegal.
     */
    public AngleFormat(final String pattern) throws IllegalArgumentException {
        this(pattern, Locale.getDefault(Locale.Category.FORMAT));
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
     * @see #setFallbackAllowed(boolean)
     */
    public void applyPattern(final String pattern) throws IllegalArgumentException {
        applyPattern(pattern, SYMBOLS, '.');
    }

    /**
     * Actual implementation of {@link #applyPattern(String)},
     * as a private method for use by the constructor.
     *
     * @param symbols An array of 3 characters containing the reserved symbols as upper-case letters.
     *        This is always the {@link #SYMBOLS} array, unless we apply localized patterns.
     * @param decimalSeparator The code point which represent decimal separator in the pattern.
     */
    @SuppressWarnings("fallthrough")
    private void applyPattern(final String pattern, final char[] symbols, final int decimalSeparator) {
        ArgumentChecks.ensureNonEmpty("pattern", pattern);
        degreesFieldWidth     = 1;
        minutesFieldWidth     = 0;
        secondsFieldWidth     = 0;
        fractionFieldWidth    = 0;
        minimumFractionDigits = 0;
        prefix                = null;
        degreesSuffix         = null;
        minutesSuffix         = null;
        secondsSuffix         = null;
        useDecimalSeparator   = true;
        int expectedField     = PREFIX_FIELD;
        int endPreviousField  = 0;
        boolean parseFinished = false;
        final int length = pattern.length();
scan:   for (int i=0; i<length;) {
            /*
             * Examine the first characters in the pattern, skipping the non-reserved ones
             * ("D", "M", "S", "d", "m", "s", "#"). Non-reserved characters will be stored
             * as suffix later.
             */
            int c          = pattern.codePointAt(i);
            int charCount  = Character.charCount(c);
            int upperCaseC = Character.toUpperCase(c);
            for (int field=DEGREES_FIELD; field<=FRACTION_FIELD; field++) {
                if (upperCaseC != symbols[field]) {
                    continue;
                }
                /*
                 * A reserved character has been found.  Ensure that it appears in a legal
                 * location. For example "MM.mm" is illegal because there is no 'D' before
                 * 'M', and "DD.mm" is illegal because the integer part is not 'M'.
                 */
                final boolean isIntegerField = (c == upperCaseC) && (field != FRACTION_FIELD);
                if (isIntegerField) {
                    expectedField++;
                }
                if (parseFinished || (field != expectedField && field != FRACTION_FIELD)) {
                    throw new IllegalArgumentException(Errors.format(
                            Errors.Keys.IllegalFormatPatternForClass_2, pattern, Angle.class));
                }
                if (isIntegerField) {
                    /*
                     * Memorize the characters prior the reserved letter as the suffix of
                     * the previous field. Then count the number of occurrences of that
                     * reserved letter. This number will be the field width.
                     */
                    final String previousSuffix = (i > endPreviousField) ? pattern.substring(endPreviousField, i) : null;
                    int width = 1;
                    while ((i += charCount) < length && pattern.codePointAt(i) == c) {
                        width++;
                    }
                    final byte wb = (byte) Math.min(width, Byte.MAX_VALUE);
                    switch (field) {
                        case DEGREES_FIELD: prefix        = previousSuffix; degreesFieldWidth = wb; break;
                        case MINUTES_FIELD: degreesSuffix = previousSuffix; minutesFieldWidth = wb; break;
                        case SECONDS_FIELD: minutesSuffix = previousSuffix; secondsFieldWidth = wb; break;
                        default: throw new AssertionError(field);
                    }
                } else {
                    /*
                     * If the reserved letter is lower-case or the symbol for optional fraction
                     * digit, the part before that letter will be the decimal separator rather
                     * than the suffix of previous field. The count the number of occurrences of
                     * the lower-case letter; this will be the precision of the fraction part.
                     */
                    if (i == endPreviousField) {
                        useDecimalSeparator = false;
                    } else {
                        final int b = pattern.codePointAt(endPreviousField);
                        if (b != decimalSeparator || endPreviousField + Character.charCount(b) != i) {
                            throw new IllegalArgumentException(Errors.format(
                                    Errors.Keys.IllegalFormatPatternForClass_2, pattern, Angle.class));
                        }
                    }
                    int width = 1;
                    while ((i += charCount) < length) {
                        final int fc = pattern.codePointAt(i);
                        if (fc != c) {
                            if (fc != symbols[FRACTION_FIELD]) break;
                            // Switch the search from mandatory to optional digits.
                            minimumFractionDigits = (byte) Math.min(width, Byte.MAX_VALUE);
                            charCount = Character.charCount(c = fc);
                        }
                        width++;
                    }
                    fractionFieldWidth = (byte) Math.min(width, Byte.MAX_VALUE);
                    if (c != symbols[FRACTION_FIELD]) {
                        // The pattern contains only mandatory digits.
                        minimumFractionDigits = fractionFieldWidth;
                    }
                    parseFinished = true;
                }
                endPreviousField = i;
                continue scan;
            }
            i += charCount;
        }
        if (endPreviousField < length) {
            final String suffix = pattern.substring(endPreviousField);
            switch (expectedField) {
                case DEGREES_FIELD: degreesSuffix = suffix; break;
                case MINUTES_FIELD: minutesSuffix = suffix; break;
                case SECONDS_FIELD: secondsSuffix = suffix; break;
                default: {
                    // Happen if no symbol has been recognized in the pattern.
                    throw new IllegalArgumentException(Errors.format(
                            Errors.Keys.IllegalFormatPatternForClass_2, pattern, Angle.class));
                }
            }
        }
    }

    /**
     * Returns the pattern used for parsing and formatting angles.
     * See class description for an explanation of how patterns work.
     *
     * @return The formatting pattern.
     */
    public String toPattern() {
        return toPattern(SYMBOLS, '.');
    }

    /**
     * Actual implementation of {@link #toPattern()}.
     *
     * @param symbols An array of 3 characters containing the reserved symbols as upper-case letters.
     *        This is always the {@link #SYMBOLS} array, unless we apply localized patterns.
     * @param decimalSeparator The code point which represent decimal separator in the pattern.
     *
     * @see #isFallbackAllowed()
     */
    private String toPattern(final char[] symbols, final int decimalSeparator) {
        char symbol = 0;
        final StringBuilder buffer = new StringBuilder(12);
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
                        buffer.append(symbol);
                    }
                    while (--width > 0);
                }
                if (previousSuffix != null) {
                    buffer.append(previousSuffix);
                }
                break; // We are done.
            }
            /*
             * This is the normal part of the loop, before the final fractional part handled
             * in the above block. Write the suffix of the previous field, then the pattern
             * for the integer part of degrees, minutes or second field.
             */
            if (previousSuffix != null) {
                buffer.append(previousSuffix);
            }
            symbol = symbols[field];
            do buffer.append(symbol);
            while (--width > 0);
        }
        return buffer.toString();
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
        minimumFractionDigits = (byte) Math.min(count, Byte.MAX_VALUE);
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
        fractionFieldWidth = (byte) Math.min(count, Byte.MAX_VALUE);
        if (fractionFieldWidth < minimumFractionDigits) {
            minimumFractionDigits = fractionFieldWidth;
        }
    }

    /**
     * Formats an angle. The angle will be formatted according the pattern given to the last call
     * of {@link #applyPattern(String)}.
     *
     * @param  angle Angle to format, in decimal degrees.
     * @return The formatted string.
     */
    public final String format(final double angle) {
        return format(angle, new StringBuffer(20), null).toString();
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
     *          shall be created with one of the following constants: {@link #DEGREES_FIELD},
     *          {@link #MINUTES_FIELD}, {@link #SECONDS_FIELD} or {@link #HEMISPHERE_FIELD}.
     *
     * @return The {@code toAppendTo} buffer, returned for method calls chaining.
     */
    public StringBuffer format(final double angle, StringBuffer toAppendTo, final FieldPosition pos) {
        if (isNaN(angle) || isInfinite(angle)) {
            return numberFormat().format(angle, toAppendTo,
                    (pos != null) ? pos : new FieldPosition(DecimalFormat.INTEGER_FIELD));
        }
        double degrees = angle;
        /*
         * Computes the numerical values of minutes and seconds fields.
         * If those fiels are not written, then store NaN.
         */
        double minutes = NaN;
        double seconds = NaN;
        if (minutesFieldWidth != 0 && !isNaN(angle)) {
            minutes = abs(degrees - (degrees = truncate(degrees))) * 60;
            if (secondsFieldWidth != 0) {
                seconds = (minutes - (minutes = truncate(minutes))) * 60;
                /*
                 * Correction for rounding errors.
                 */
                final double puissance = pow10(fractionFieldWidth);
                seconds = rint(seconds * puissance) / puissance;
                final double correction = truncate(seconds / 60);
                seconds -= correction * 60;
                minutes += correction;
            } else {
                final double puissance = pow10(fractionFieldWidth);
                minutes = rint(minutes * puissance) / puissance;
            }
            final double correction = truncate(minutes / 60);
            minutes -= correction * 60;
            degrees += correction;
        }
        /*
         * At this point, 'degrees', 'minutes' and 'seconds'
         * contain the final values to format.
         */
        if (prefix != null) {
            toAppendTo.append(prefix);
        }
        final int field;
        if (pos != null) {
            field = pos.getField();
            pos.setBeginIndex(0);
            pos.setEndIndex(0);
        } else {
            field = PREFIX_FIELD;
        }
        toAppendTo = formatField(degrees, toAppendTo, (field == DEGREES_FIELD) ? pos : null,
                degreesFieldWidth, (minutesFieldWidth == 0), degreesSuffix);
        if (!isNaN(minutes)) {
            toAppendTo = formatField(minutes, toAppendTo, (field == MINUTES_FIELD) ? pos : null,
                    minutesFieldWidth, (secondsFieldWidth == 0), minutesSuffix);
        }
        if (!isNaN(seconds)) {
            toAppendTo = formatField(seconds, toAppendTo, (field == SECONDS_FIELD) ? pos : null,
                    secondsFieldWidth, true, secondsSuffix);
        }
        return toAppendTo;
    }

    /**
     * Formats a single field value.
     *
     * @param value      The field value.
     * @param toAppendTo The buffer where to append the formatted angle.
     * @param pos        An optional object where to store the position of a field in the formatted text.
     * @param width      The field width.
     * @param decimal    {@code true} for formatting the decimal digits (last field only).
     * @param suffix     Suffix to append, or {@code null} if none.
     */
    private StringBuffer formatField(double value, StringBuffer toAppendTo, final FieldPosition pos,
                                     final int width, final boolean decimal, final String suffix)
    {
        final NumberFormat numberFormat = numberFormat();
        final int startPosition = toAppendTo.length();
        if (!decimal) {
            numberFormat.setMinimumIntegerDigits(width);
            numberFormat.setMaximumFractionDigits(0);
            toAppendTo = numberFormat.format(value, toAppendTo, dummyFieldPosition());
        } else if (useDecimalSeparator) {
            numberFormat.setMinimumIntegerDigits(width);
            numberFormat.setMinimumFractionDigits(minimumFractionDigits);
            numberFormat.setMaximumFractionDigits(fractionFieldWidth);
            toAppendTo = numberFormat.format(value, toAppendTo, dummyFieldPosition());
        } else {
            value *= pow10(fractionFieldWidth);
            numberFormat.setMaximumFractionDigits(0);
            numberFormat.setMinimumIntegerDigits(width + fractionFieldWidth);
            toAppendTo = numberFormat.format(value, toAppendTo, dummyFieldPosition());
        }
        if (suffix != null) {
            toAppendTo.append(suffix);
        }
        if (pos != null) {
            pos.setBeginIndex(startPosition);
            pos.setEndIndex(toAppendTo.length() - 1);
        }
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
     *          shall be created with one of the following constants: {@link #DEGREES_FIELD},
     *          {@link #MINUTES_FIELD}, {@link #SECONDS_FIELD} or {@link #HEMISPHERE_FIELD}.
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
                "value", value.getClass(), Angle.class));
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
        toAppendTo = format(abs(angle), toAppendTo, pos);
        final int start = toAppendTo.length();
        toAppendTo.append(isNegative(angle) ? negativeSuffix : positiveSuffix);
        if (pos != null && pos.getField() == HEMISPHERE_FIELD) {
            pos.setBeginIndex(start);
            pos.setEndIndex(toAppendTo.length()-1);
        }
        return toAppendTo;
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
                final int toSkipLength = toSkip.length();
                int c;
                do {
                    if (source.regionMatches(index, toSkip, 0, toSkipLength)) {
                        pos.setIndex(index + toSkipLength);
                        return field;
                    }
                    if (index >= length) break;
                    c = source.codePointAt(index);
                    index += Character.charCount(c);
                }
                while (Character.isSpaceChar(c));
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
            while (Character.isSpaceChar(c));
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
     *
     * @param  source The string being parsed.
     * @param  index  Index of the first {@code source} character to read.
     * @return Index of the first non-space character, or the end of string if none.
     */
    private static int skipSpaces(final String source, int index) {
        final int length = source.length();
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
     */
    public Angle parse(final String source, final ParsePosition pos) {
        return parse(source, pos, false);
    }

    /**
     * Parses the given string as an angle. The {@code spaceAsSeparator} additional argument
     * specifies if spaces can be accepted as a field separator. For example if {@code true},
     * then "45 30" will be parsed as "45°30".
     */
    @SuppressWarnings("fallthrough")
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
            index = skipSpaces(source, pos.getIndex());
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
                    index = skipSpaces(source, indexStartField);
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
                    index = skipSpaces(source, indexStartField);
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
            if (!Character.isSpaceChar(c)) {
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
     */
    public Angle parse(final String source) throws ParseException {
        final ParsePosition pos = new ParsePosition(0);
        final Angle angle = parse(source, pos, true);
        if (skipSpaces(source, pos.getIndex()) != source.length()) {
            throw Exceptions.createParseException(locale, Angle.class, source, pos);
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
     * or the default locale at construction time otherwise.
     *
     * @return This formatter locale.
     */
    @Override
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns a clone of this {@code AngleFormat}.
     */
    @Override
    public AngleFormat clone() {
        final AngleFormat clone = (AngleFormat) super.clone();
        clone.numberFormat = null;
        clone.dummy = null;
        return clone;
    }

    /**
     * Returns a "hash value" for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(degreesFieldWidth, minutesFieldWidth, secondsFieldWidth,
                fractionFieldWidth, minimumFractionDigits, useDecimalSeparator, isFallbackAllowed,
                locale, prefix, degreesSuffix, minutesSuffix, secondsSuffix) ^ (int) serialVersionUID;
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
                   Objects.equals(locale,        cast.locale)          &&
                   Objects.equals(prefix,        cast.prefix)          &&
                   Objects.equals(degreesSuffix, cast.degreesSuffix)   &&
                   Objects.equals(minutesSuffix, cast.minutesSuffix)   &&
                   Objects.equals(secondsSuffix, cast.secondsSuffix);
        } else {
            return false;
        }
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
