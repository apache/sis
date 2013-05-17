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
package org.apache.sis.internal.util;

import java.util.Date;
import java.util.Formatter;
import java.util.FormattableFlags;
import javax.xml.bind.DatatypeConverter;
import org.apache.sis.util.Static;
import org.apache.sis.util.CharSequences;

import static java.lang.Math.abs;
import static java.lang.Math.max;


/**
 * Miscellaneous utilities which should not be put in public API.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
public final class Utilities extends Static {
    /**
     * Relative difference tolerated when comparing floating point numbers using
     * {@link org.apache.sis.util.ComparisonMode#APPROXIMATIVE}.
     *
     * <p>Historically, this was the relative tolerance threshold for considering two
     * matrixes as equal. This value has been determined empirically in order to allow
     * {@link org.apache.sis.referencing.operation.transform.ConcatenatedTransform} to
     * detect the cases where two {@link org.apache.sis.referencing.operation.transform.LinearTransform}
     * are equal for practical purpose. This threshold can be used as below:</p>
     *
     * {@preformat java
     *     Matrix m1 = ...;
     *     Matrix m2 = ...;
     *     if (Matrices.epsilonEqual(m1, m2, EQUIVALENT_THRESHOLD, true)) {
     *         // Consider that matrixes are equal.
     *     }
     * }
     *
     * By extension, the same threshold value is used for comparing other floating point values.
     *
     * @see org.apache.sis.internal.referencing.Utilities#LINEAR_TOLERANCE
     * @see org.apache.sis.internal.referencing.Utilities#ANGULAR_TOLERANCE
     */
    public static final double COMPARISON_THRESHOLD = 1E-14;

    /**
     * Bit mask to isolate the sign bit of non-{@linkplain Double#isNaN(double) NaN} values in a
     * {@code double}. For any real value, the following code evaluate to 0 if the given value is
     * positive:
     *
     * {@preformat java
     *     Double.doubleToRawLongBits(value) & SIGN_BIT_MASK;
     * }
     *
     * Note that this idiom differentiates positive zero from negative zero.
     * It should be used only when such difference matter.
     *
     * @see org.apache.sis.math.MathFunctions#isPositive(double)
     * @see org.apache.sis.math.MathFunctions#isNegative(double)
     * @see org.apache.sis.math.MathFunctions#isSameSign(double, double)
     * @see org.apache.sis.math.MathFunctions#xorSign(double, double)
     */
    public static final long SIGN_BIT_MASK = Long.MIN_VALUE;

    /**
     * Do not allow instantiation of this class.
     */
    private Utilities() {
    }

    /**
     * Returns {@code true} if the given values are approximatively equal,
     * up to the {@linkplain #COMPARISON_THRESHOLD comparison threshold}.
     *
     * @param  v1 The first value to compare.
     * @param  v2 The second value to compare.
     * @return {@code true} If both values are approximatively equal.
     */
    public static boolean epsilonEqual(final double v1, final double v2) {
        final double threshold = COMPARISON_THRESHOLD * max(abs(v1), abs(v2));
        if (threshold == Double.POSITIVE_INFINITY || Double.isNaN(threshold)) {
            return Double.doubleToLongBits(v1) == Double.doubleToLongBits(v2);
        }
        return abs(v1 - v2) <= threshold;
    }

    /**
     * Returns {@code true} if the following objects are floating point numbers ({@link Float} or
     * {@link Double} types) and approximatively equal. If the given object are not floating point
     * numbers, then this method returns {@code false} unconditionally on the assumption that
     * strict equality has already been checked before this method call.
     *
     * @param  v1 The first value to compare.
     * @param  v2 The second value to compare.
     * @return {@code true} If both values are real number and approximatively equal.
     */
    public static boolean floatEpsilonEqual(final Object v1, final Object v2) {
        return (v1 instanceof Float || v1 instanceof Double) &&
               (v2 instanceof Float || v2 instanceof Double) &&
               epsilonEqual(((Number) v1).doubleValue(), ((Number) v2).doubleValue());
    }

    /**
     * Parses a date from a string in ISO 8601 format. More specifically, this method expects the
     * format defined by <cite>XML Schema Part 2: Datatypes for {@code xsd:dateTime}</cite>, with
     * some additional flexibility (e.g. missing minutes or seconds fields are automatically added).
     *
     * @param  date The date to parse, or {@code null}.
     * @param  defaultToUTC {@code true} if the default timezone shall be UTL, or {@code false} for locale default.
     * @return The parsed date, or {@code null} if the given string was {@code null}.
     * @throws IllegalArgumentException if the given date can not be parsed.
     *
     * @see DatatypeConverter#parseDateTime(String)
     */
    public static Date parseDateTime(String date, final boolean defaultToUTC) throws IllegalArgumentException {
        if (date != null) {
            date = CharSequences.trimWhitespaces(date);
            if (!date.isEmpty()) {
                /*
                 * Check for missing time fields and time zone. For example if the given date is
                 * "2005-09-22T00:00", then this block will complete it as "2005-09-22T00:00:00".
                 * In addition, a 'Z' suffix will be appended if 'defaultToUTC' is true.
                 */
                int timeFieldStart  = date.lastIndexOf('T') + 1; // 0 if there is no time field.
                int timeFieldEnd    = date.length();             // To be updated if there is a time field.
                int missingFields   = 0;                         // Number of missing time fields.
                boolean hasTimeZone = date.charAt(timeFieldEnd - 1) == 'Z';
                if (timeFieldStart != 0) {
                    if (hasTimeZone) {
                        timeFieldEnd--;
                    } else {
                        final int s = Math.max(date.indexOf('+', timeFieldStart),
                                               date.indexOf('-', timeFieldStart));
                        hasTimeZone = (s >= 0);
                        if (hasTimeZone) {
                            timeFieldEnd = s;
                        }
                    }
                    missingFields = 2;
                    for (int i=timeFieldStart; i<timeFieldEnd; i++) {
                        if (date.charAt(i) == ':') {
                            if (--missingFields == 0) break;
                        }
                    }
                }
                /*
                 * At this point, we have determined if there is some missing fields.
                 * The timezone will be considered missing only if 'defaultToUTC' is true.
                 */
                CharSequence modified = date;
                hasTimeZone |= !defaultToUTC;
                if (missingFields != 0 || !hasTimeZone) {
                    final StringBuilder buffer = new StringBuilder(date);
                    while (--missingFields >= 0) {
                        buffer.append(":00");
                    }
                    if (!hasTimeZone) {
                        buffer.append('Z');
                    }
                    modified = buffer;
                }
                /*
                 * Now ensure that all numbers have at least two digits.
                 */
                int indexOfLastDigit = 0;
                for (int i=modified.length(); --i >= 0;) {
                    char c = modified.charAt(i);
                    final boolean isDigit = (c >= '0' && c <= '9'); // Do not use Character.isDigit(char).
                    if (indexOfLastDigit == 0) {
                        // We were not scaning a number. Check if we are now starting doing so.
                        if (isDigit) {
                            indexOfLastDigit = i;
                        }
                    } else {
                        // We were scaning a number. Check if we found the begining.
                        if (!isDigit) {
                            if (indexOfLastDigit - i == 1) {
                                // Reuse the buffer if it exists, or create a new one otherwise.
                                final StringBuilder buffer;
                                if (modified == date) {
                                    modified = buffer = new StringBuilder(date);
                                } else {
                                    buffer = (StringBuilder) modified;
                                }
                                buffer.insert(i+1, '0');
                            }
                            indexOfLastDigit = 0;
                        }
                    }
                }
                /*
                 * Now delegate to the utility method provided in the JAXB packages.
                 */
                return DatatypeConverter.parseDateTime(modified.toString()).getTime();
            }
        }
        return null;
    }

    /**
     * Formats the given character sequence to the given formatter. This method takes in account
     * the {@link FormattableFlags#UPPERCASE} and {@link FormattableFlags#LEFT_JUSTIFY} flags.
     *
     * @param formatter The formatter in which to format the value.
     * @param flags     The formatting flags.
     * @param width     Minimal number of characters to write, padding with {@code ' '} if necessary.
     * @param precision Number of characters to keep before truncation, or -1 if no limit.
     * @param value     The text to format.
     */
    public static void formatTo(final Formatter formatter, final int flags,
            int width, int precision, String value)
    {
        final String format;
        final Object[] args;
        boolean isUpperCase = (flags & FormattableFlags.UPPERCASE) != 0;
        if (isUpperCase && width > 0) {
            // May change the string length in some locales.
            value = value.toUpperCase(formatter.locale());
            isUpperCase = false; // Because conversion has already been done.
        }
        int length = value.length();
        if (precision >= 0) {
            for (int i=0,n=0; i<length; i += n) {
                if (--precision < 0) {
                    // Found the amount of characters to keep. The 'n' variable can be
                    // zero only if precision == 0, in which case the string is empty.
                    if (n == 0) {
                        value = "";
                    } else {
                        length = (i -= n) + 1;
                        final StringBuilder buffer = new StringBuilder(length);
                        value = buffer.append(value, 0, i).append('…').toString();
                    }
                    break;
                }
                n = Character.charCount(value.codePointAt(i));
            }
        }
        // Double check since length() is faster than codePointCount(...).
        if (width > length && (width -= value.codePointCount(0, length)) > 0) {
            format = "%s%s";
            args = new Object[] {value, value};
            args[(flags & FormattableFlags.LEFT_JUSTIFY) != 0 ? 1 : 0] = CharSequences.spaces(width);
        } else {
            format = isUpperCase ? "%S" : "%s";
            args = new Object[] {value};
        }
        formatter.format(format, args);
    }
}
