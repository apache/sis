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
package org.apache.sis.math;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;
import java.text.Format;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.text.ParseException;

import org.apache.sis.io.TableFormatter;
import org.apache.sis.io.CompoundFormat;
import org.apache.sis.util.resources.Vocabulary;

import static java.lang.Math.*;


/**
 * Formats a {@link Statistics} object.
 * This is a package-private class for now - if we want to make it public, we may need to make it
 * a full-featured {@link java.text.Format} implementation.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @since   0.3 (derived from geotk-1.0)
 * @version 0.3
 * @module
 */
@SuppressWarnings("serial")
final class StatisticsFormat extends CompoundFormat<Statistics> {
    /**
     * Number of additional digits, to be added to the number of digits computed from the
     * range and the number of sample values. This is an arbitrary parameter.
     */
    private static final int ADDITIONAL_DIGITS = 2;

    /**
     * The locale for row labels. This is usually the same than the format locale,
     * but not necessarily.
     */
    private final Locale labelLocale;

    /**
     * {@code true} if the sample values given to {@code Statistics.add(…)} methods were the
     * totality of the population under study, or {@code false} if they were only a sampling.
     *
     * @see Statistics#standardDeviation(boolean)
     */
    private boolean allPopulation;

    /**
     * The column separator, typically as a semicolon or tabulation character.
     * If 0, then the values will be written in a tabular format using {@link TableFormatter}.
     */
    private char columnSeparator;

    /**
     * Returns an instance for the current system default locale.
     *
     * @return A statistics format instance for the current default locale.
     */
    public static StatisticsFormat getInstance() {
        return new StatisticsFormat(
                Locale.getDefault(Locale.Category.FORMAT),
                Locale.getDefault(Locale.Category.DISPLAY), null);
    }

    /**
     * Constructs a new format for the given locales.
     * The timezone is used only if the values added to the {@link Statistics} are dates.
     *
     * @param locale      The locale, or {@code null} for unlocalized format.
     * @param labelLocale The locale for row labels. Usually, but not necessarily, same as {@code locale}.
     * @param timezone    The timezone, or {@code null} for UTC.
     */
    private StatisticsFormat(final Locale locale, final Locale labelLocale, final TimeZone timezone) {
        super(locale, timezone);
        this.labelLocale = labelLocale;
    }

    /**
     * Constructs a new format for the given locale.
     * The timezone is used only if the values added to the {@link Statistics} are dates.
     *
     * @param locale   The locale, or {@code null} for unlocalized format.
     * @param timezone The timezone, or {@code null} for UTC.
     */
    public StatisticsFormat(final Locale locale, final TimeZone timezone) {
        super(locale, timezone);
        labelLocale = locale;
    }

    /**
     * Returns the kind of objects formatted by this class.
     */
    @Override
    public Class<Statistics> getValueType() {
        return Statistics.class;
    }

    /**
     * Not yet implemented.
     */
    @Override
    public Statistics parse(CharSequence text, ParsePosition pos) throws ParseException {
        throw new UnsupportedOperationException();
    }

    /**
     * The resource keys of the rows to formats. Array index must be consistent with
     * the switch statements inside the {@link #format(Statistics)} method.
     */
    private static final int[] KEYS = {
        Vocabulary.Keys.NumberOfValues,
        Vocabulary.Keys.NumberOfNaN,
        Vocabulary.Keys.MinimumValue,
        Vocabulary.Keys.MaximumValue,
        Vocabulary.Keys.MeanValue,
        Vocabulary.Keys.RootMeanSquare,
        Vocabulary.Keys.StandardDeviation
    };

    /**
     * Formats a localized string representation of the given statistics.
     * If statistics on {@linkplain Statistics#differences() differences}
     * are associated to the given object, they will be formatted too.
     */
    @Override
    public void format(Statistics stats, final Appendable toAppendTo) throws IOException {
        final List<Statistics> list = new ArrayList<>(3);
        while (stats != null) {
            list.add(stats);
            stats = stats.differences();
        }
        format(list.toArray(new Statistics[list.size()]), toAppendTo);
    }

    /**
     * Formats the given statistics in a tabular format. This method does not check
     * for the statistics on {@linkplain Statistics#differences() differences} - if
     * such statistics are wanted, they must be included in the given array.
     */
    private void format(final Statistics[] stats, Appendable toAppendTo) throws IOException {
        final String lineSeparator = System.lineSeparator();
        /*
         * Verify if we can omit the count of NaN values.
         */
        boolean showNaNCount = false;
        for (final Statistics s : stats) {
            if (s.countNaN() != 0) {
                showNaNCount = true;
                break;
            }
        }
        /*
         * This formatter can optionally use a column separator, typically a semi-colon.
         * If no column separator was specified (which is the usual case), then we will
         * format the values in a tabular format.
         */
        TableFormatter table = null;
        char separator = columnSeparator;
        if (separator == 0) {
            toAppendTo = table = new TableFormatter(toAppendTo, " ");
            separator = '\t'; // Will be handled especially by TableFormatter.
        }
        final Vocabulary resources = Vocabulary.getResources(labelLocale);
        /*
         * Initialize the NumberFormat for formatting integers without scientific notation.
         * This is necessary since the format may have been modified by a previous execution
         * of this method.
         */
        final Format format = getFormat(Double.class);
        if (format instanceof DecimalFormat) {
            ((DecimalFormat) format).applyPattern("#0"); // Also disable scientific notation.
        } else if (format instanceof NumberFormat) {
            setFractionDigits((NumberFormat) format, 0);
        }
        /*
         * Iterates over the rows to format (count, minimum, maximum, mean, RMS, standard deviation),
         * then iterate over columns (statistics on sample values, on the first derivatives, etc.)
         * The NumberFormat configuration may be different for each column, but we can skip many
         * reconfiguration in the common case where there is only one column.
         */
        boolean needsConfigure = false;
        for (int i=0; i<KEYS.length; i++) {
            switch (i) {
                case 1: if (!showNaNCount) continue; else break;
                // Case 0 and 1 use the above configuration for integers.
                // Case 2 unconditionally needs a reconfiguration for floating point values.
                // Case 3 and others need reconfiguration only if there is more than one column.
                case 2: needsConfigure = true; break;
                case 3: needsConfigure = (stats[0].differences() != null); break;
            }
            if (table != null) {
                table.setCellAlignment(TableFormatter.ALIGN_LEFT);
            }
            toAppendTo.append(resources.getString(KEYS[i])).append(':');
            for (final Statistics s : stats) {
                final Number value;
                switch (i) {
                    case 0:  value = s.count();    break;
                    case 1:  value = s.countNaN(); break;
                    case 2:  value = s.minimum();  break;
                    case 3:  value = s.maximum();  break;
                    case 4:  value = s.mean();     break;
                    case 5:  value = s.rms();      break;
                    case 6:  value = s.standardDeviation(allPopulation); break;
                    default: throw new AssertionError(i);
                }
                if (needsConfigure) {
                    configure(format, s);
                }
                toAppendTo.append(separator).append(format.format(value));
                if (table != null) {
                    table.setCellAlignment(TableFormatter.ALIGN_RIGHT);
                }
            }
            toAppendTo.append(lineSeparator);
        }
        /*
         * TableFormatter needs to be explicitly flushed in order to format the values.
         */
        if (table != null) {
            table.flush();
        }
    }

    /**
     * Configures the given formatter for writing a set of data described by the given statistics.
     * This method configures the formatter using heuristic rules based on the range of values and
     * their standard deviation. It can be used for reasonable default formatting when the user
     * didn't specify an explicit one.
     *
     * @param  format The formatter to configure.
     * @param  stats  The statistics for which to configure the formatter.
     */
    private void configure(final Format format, final Statistics stats) {
        final double minimum  = stats.minimum();
        final double maximum  = stats.maximum();
        final double extremum = max(abs(minimum), abs(maximum));
        if ((extremum >= 1E+10 || extremum <= 1E-4) && format instanceof DecimalFormat) {
            /*
             * The above threshold is high so that geocentric and projected coordinates in metres
             * are not formatted with scientific notation (a threshold of 1E+7 is not enough).
             * The number of decimal digits in the pattern is arbitrary.
             */
            ((DecimalFormat) format).applyPattern("0.00000E00");
        } else {
            /*
             * Computes a representative range of values. We take 2 standard deviations away
             * from the mean. Assuming that data have a gaussian distribution, this is 97.7%
             * of data. If the data have a uniform distribution, then this is 100% of data.
             */
            double delta;
            final double mean = stats.mean();
            delta = 2 * stats.standardDeviation(true); // 'true' is for avoiding NaN when count == 1.
            delta = min(maximum, mean+delta) - max(minimum, mean-delta); // Range of 97.7% of values.
            delta = max(delta/stats.count(), ulp(extremum)); // Mean delta for uniform distribution, not finer than 'double' accuracy.
            if (format instanceof NumberFormat) {
                setFractionDigits((NumberFormat) format, max(0, ADDITIONAL_DIGITS
                        + MathFunctions.fractionDigitsForDelta(delta, false)));
            } else {
                // A future version could configure DateFormat here.
            }
        }
    }

    /**
     * Convenience method for setting the minimum and maximum fraction digits of the given format.
     */
    private static void setFractionDigits(final NumberFormat format, final int digits) {
        format.setMinimumFractionDigits(digits);
        format.setMaximumFractionDigits(digits);
    }
}
