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

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;
import java.text.Format;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.ParseException;
import org.opengis.util.InternationalString;
import org.apache.sis.io.TableAppender;
import org.apache.sis.io.TabularFormat;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.collection.BackingStoreException;

import static java.lang.Math.*;


/**
 * Formats a {@link Statistics} object.
 * By default, newly created {@code StatisticsFormat} instances will format statistical values
 * in a tabular format using spaces as the column separator. This default configuration matches
 * the {@link Statistics#toString()} format.
 *
 * <div class="section">Limitations</div>
 * The current implementation can only format statistics - parsing is not yet implemented.
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public class StatisticsFormat extends TabularFormat<Statistics> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6914760410359494163L;

    /**
     * Number of additional digits, to be added to the number of digits computed from the
     * range and the number of sample values. This is an arbitrary parameter.
     */
    private static final int ADDITIONAL_DIGITS = 2;

    /**
     * The locale for row and column headers.
     * This is usually the same than the format locale, but not necessarily.
     */
    private final Locale headerLocale;

    /**
     * The "width" of the border to drawn around the table, in number of lines.
     *
     * @see #getBorderWidth()
     * @see #setBorderWidth(int)
     */
    private byte borderWidth;

    /**
     * {@code true} if the sample values given to {@code Statistics.accept(…)} methods were the
     * totality of the population under study, or {@code false} if they were only a sampling.
     *
     * @see #isForAllPopulation()
     * @see #setForAllPopulation(boolean)
     * @see Statistics#standardDeviation(boolean)
     */
    private boolean allPopulation;

    /**
     * Returns an instance for the current system default locale.
     *
     * @return A statistics format instance for the current default locale.
     */
    public static StatisticsFormat getInstance() {
        return getInstance(Locale.getDefault());
    }

    /**
     * Returns an instance for the given locale.
     *
     * @param  locale The locale for which to get a {@code StatisticsFormat} instance.
     * @return A statistics format instance for the given locale.
     */
    public static StatisticsFormat getInstance(final Locale locale) {
        return new StatisticsFormat(locale, locale, null);
    }

    /**
     * Constructs a new format for the given numeric and header locales.
     * The timezone is used only if the values added to the {@link Statistics} are dates.
     *
     * @param locale       The locale to use for numbers, dates and angles formatting,
     *                     or {@code null} for the {@linkplain Locale#ROOT root locale}.
     * @param headerLocale The locale for row and column headers. Usually same as {@code locale}.
     * @param timezone     The timezone, or {@code null} for UTC.
     */
    public StatisticsFormat(final Locale locale, final Locale headerLocale, final TimeZone timezone) {
        super(locale, timezone);
        this.headerLocale = (headerLocale != null) ? headerLocale : Locale.ROOT;
    }

    /**
     * Returns the type of objects formatted by this class.
     *
     * @return {@code Statistics.class}
     */
    @Override
    public final Class<Statistics> getValueType() {
        return Statistics.class;
    }

    /**
     * Returns {@code true} if this formatter shall consider that the statistics where computed
     * using the totality of the populations under study. This information impacts the standard
     * deviation values to be formatted.
     *
     * @return {@code true} if the statistics to format where computed using the totality of
     *         the populations under study.
     *
     * @see Statistics#standardDeviation(boolean)
     */
    public boolean isForAllPopulation() {
        return allPopulation;
    }

    /**
     * Sets whether this formatter shall consider that the statistics where computed using
     * the totality of the populations under study. The default value is {@code false}.
     *
     * @param allPopulation {@code true} if the statistics to format where computed
     *        using the totality of the populations under study.
     *
     * @see Statistics#standardDeviation(boolean)
     */
    public void setForAllPopulation(final boolean allPopulation) {
        this.allPopulation = allPopulation;
    }

    /**
     * Returns the "width" of the border to drawn around the table, in number of lines.
     * The default width is 0, which stands for no border.
     *
     * @return The border "width" in number of lines.
     */
    public int getBorderWidth() {
        return borderWidth;
    }

    /**
     * Sets the "width" of the border to drawn around the table, in number of lines.
     * The value can be any of the following:
     *
     * <ul>
     *  <li>0 (the default) for no border</li>
     *  <li>1 for single line ({@code │},{@code ─})</li>
     *  <li>2 for double lines ({@code ║},{@code ═})</li>
     * </ul>
     *
     * @param borderWidth The border width, in number of lines.
     */
    public void setBorderWidth(final int borderWidth) {
        ArgumentChecks.ensureBetween("borderWidth", 0, 2, borderWidth);
        this.borderWidth = (byte) borderWidth;
    }

    /**
     * Not yet implemented.
     *
     * @return Currently never return.
     * @throws ParseException Currently never thrown.
     */
    @Override
    public Statistics parse(CharSequence text, ParsePosition pos) throws ParseException {
        throw new UnsupportedOperationException();
    }

    /**
     * Formats the given statistics. This method will delegates to one of the following methods,
     * depending on the type of the given object:
     *
     * <ul>
     *   <li>{@link #format(Statistics, Appendable)}</li>
     *   <li>{@link #format(Statistics[], Appendable)}</li>
     * </ul>
     *
     * @param  object      The object to format.
     * @param  toAppendTo  Where to format the object.
     * @param  pos         Ignored in current implementation.
     * @return             The given buffer, returned for convenience.
     */
    @Override
    public StringBuffer format(final Object object, final StringBuffer toAppendTo, final FieldPosition pos) {
        if (object instanceof Statistics[]) try {
            format((Statistics[]) object, toAppendTo);
            return toAppendTo;
        } catch (IOException e) {
            // Same exception handling than in the super-class.
            throw new BackingStoreException(e);
        } else {
            return super.format(object, toAppendTo, pos);
        }
    }

    /**
     * Formats a localized string representation of the given statistics.
     * If statistics on {@linkplain Statistics#differences() differences}
     * are associated to the given object, they will be formatted too.
     *
     * @param  stats       The statistics to format.
     * @param  toAppendTo  Where to format the statistics.
     * @throws IOException If an error occurred while writing to the given appendable.
     */
    @Override
    public void format(Statistics stats, final Appendable toAppendTo) throws IOException {
        final List<Statistics> list = new ArrayList<Statistics>(3);
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
     *
     * @param  stats       The statistics to format.
     * @param  toAppendTo  Where to format the statistics.
     * @throws IOException If an error occurred while writing to the given appendable.
     */
    public void format(final Statistics[] stats, final Appendable toAppendTo) throws IOException {
        /*
         * Inspect the given statistics in order to determine if we shall omit the headers,
         * and if we shall omit the count of NaN values.
         */
        final String[] headers = new String[stats.length];
        boolean showHeaders  = false;
        boolean showNaNCount = false;
        for (int i=0; i<stats.length; i++) {
            final Statistics s = stats[i];
            showNaNCount |= (s.countNaN() != 0);
            final InternationalString header = s.name();
            if (header != null) {
                headers[i] = header.toString(headerLocale);
                showHeaders |= (headers[i] != null);
            }
        }
        char horizontalLine = 0;
        String separator = columnSeparator;
        switch (borderWidth) {
            case 1: horizontalLine = '─'; separator += "│ "; break;
            case 2: horizontalLine = '═'; separator += "║ "; break;
        }
        final TableAppender table = new TableAppender(toAppendTo, separator);
        final Vocabulary resources = Vocabulary.getResources(headerLocale);
        /*
         * If there is a header for at least one statistics, write the full headers row.
         */
        if (horizontalLine != 0) {
            table.nextLine(horizontalLine);
        }
        if (showHeaders) {
            table.nextColumn();
            for (final String header : headers) {
                if (header != null) {
                    table.append(header);
                    table.setCellAlignment(TableAppender.ALIGN_CENTER);
                }
                table.nextColumn();
            }
            table.append(lineSeparator);
            if (horizontalLine != 0) {
                table.nextLine(horizontalLine);
            }
        }
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
            table.setCellAlignment(TableAppender.ALIGN_LEFT);
            table.append(resources.getString(KEYS[i])).append(':');
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
                table.append(beforeFill);
                table.nextColumn(fillCharacter);
                table.append(format.format(value));
                table.setCellAlignment(TableAppender.ALIGN_RIGHT);
            }
            table.append(lineSeparator);
        }
        if (horizontalLine != 0) {
            table.nextLine(horizontalLine);
        }
        /*
         * TableAppender needs to be explicitly flushed in order to format the values.
         */
        table.flush();
    }

    /**
     * The resource keys of the rows to formats. Array index must be consistent with the
     * switch statements inside the {@link #format(Statistics[], Appendable)} method
     * (we define this static field close to the format methods for this purpose).
     */
    private static final short[] KEYS = {
        Vocabulary.Keys.NumberOfValues,
        Vocabulary.Keys.NumberOfNaN,
        Vocabulary.Keys.MinimumValue,
        Vocabulary.Keys.MaximumValue,
        Vocabulary.Keys.MeanValue,
        Vocabulary.Keys.RootMeanSquare,
        Vocabulary.Keys.StandardDeviation
    };

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
                        + DecimalFunctions.fractionDigitsForDelta(delta, false)));
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
