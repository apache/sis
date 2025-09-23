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
import java.util.Locale;
import java.util.TimeZone;
import java.io.IOException;
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
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.internal.shared.Numerics;


/**
 * Formats a {@link Statistics} object.
 * By default, newly created {@code StatisticsFormat} instances will format statistical values
 * in a tabular format using spaces as the column separator.
 *
 * <h2>Example</h2>
 * <pre class="text">
 *     Number of values:     8726
 *     Minimum value:       6.853
 *     Maximum value:       8.259
 *     Mean value:          7.421
 *     Root Mean Square:    7.846
 *     Standard deviation:  6.489</pre>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>The current implementation can only format features — parsing is not yet implemented.</li>
 *   <li>{@code StatisticsFormat}, like most {@code java.text.Format} subclasses, is not thread-safe.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (MPO, IRD, Geomatys)
 * @version 1.0
 *
 * @see Statistics#toString()
 *
 * @since 0.3
 */
public class StatisticsFormat extends TabularFormat<Statistics> {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 6914760410359494163L;

    /**
     * The locale for row and column headers.
     * This is usually the same as the format locale, but not necessarily.
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
     * @return a statistics format instance for the current default locale.
     */
    public static StatisticsFormat getInstance() {
        return new StatisticsFormat(
                Locale.getDefault(Locale.Category.FORMAT),
                Locale.getDefault(Locale.Category.DISPLAY), null);
    }

    /**
     * Returns an instance for the given locale.
     *
     * @param  locale  the locale for which to get a {@code StatisticsFormat} instance.
     * @return a statistics format instance for the given locale.
     */
    public static StatisticsFormat getInstance(final Locale locale) {
        return new StatisticsFormat(locale, locale, null);
    }

    /**
     * Constructs a new format for the given numeric and header locales.
     * The timezone is used only if the values added to the {@link Statistics} are dates.
     *
     * @param locale        the locale to use for numbers, dates and angles formatting,
     *                      or {@code null} for the {@linkplain Locale#ROOT root locale}.
     * @param headerLocale  the locale for row and column headers. Usually same as {@code locale}.
     * @param timezone      the timezone, or {@code null} for UTC.
     */
    public StatisticsFormat(final Locale locale, final Locale headerLocale, final TimeZone timezone) {
        super(locale, timezone);
        this.headerLocale = (headerLocale != null) ? headerLocale : Locale.ROOT;
    }

    /**
     * Returns the locale for the given category. This method implements the following mapping:
     *
     * <ul>
     *   <li>{@link java.util.Locale.Category#DISPLAY} — the {@code headerLocale} given at construction time.</li>
     *   <li>{@link java.util.Locale.Category#FORMAT} — the {@code locale} given at construction time,
     *       used for all values below the header row.</li>
     * </ul>
     *
     * @param  category  the category for which a locale is desired.
     * @return the locale for the given category (never {@code null}).
     *
     * @since 0.4
     */
    @Override
    public Locale getLocale(final Locale.Category category) {
        if (category == Locale.Category.DISPLAY) {
            return headerLocale;
        }
        return super.getLocale(category);
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
     * @param  allPopulation  {@code true} if the statistics to format where computed using
     *                        the totality of the populations under study.
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
     * @return the border "width" in number of lines.
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
     * @param  borderWidth  the border width, in number of lines.
     */
    public void setBorderWidth(final int borderWidth) {
        ArgumentChecks.ensureBetween("borderWidth", 0, 2, borderWidth);
        this.borderWidth = (byte) borderWidth;
    }

    /**
     * Not yet supported.
     *
     * @return currently never return.
     * @throws ParseException currently always thrown.
     */
    @Override
    public Statistics parse(CharSequence text, ParsePosition pos) throws ParseException {
        throw new ParseException(Errors.format(Errors.Keys.UnsupportedOperation_1, "parse"), pos.getIndex());
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
     * @param  object      the object to format.
     * @param  toAppendTo  where to format the object.
     * @param  pos         ignored in current implementation.
     * @return the given buffer, returned for convenience.
     */
    @Override
    public StringBuffer format(final Object object, final StringBuffer toAppendTo, final FieldPosition pos) {
        if (object instanceof Statistics[]) try {
            format((Statistics[]) object, toAppendTo);
            return toAppendTo;
        } catch (IOException e) {
            // Same exception handling as in the super-class.
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
     * @param  stats       the statistics to format.
     * @param  toAppendTo  where to format the statistics.
     * @throws IOException if an error occurred while writing to the given appendable.
     */
    @Override
    public void format(Statistics stats, final Appendable toAppendTo) throws IOException {
        final var list = new ArrayList<Statistics>(3);
        while (stats != null) {
            list.add(stats);
            stats = stats.differences();
        }
        format(list.toArray(Statistics[]::new), toAppendTo);
    }

    /**
     * Formats the given statistics in a tabular format. This method does not check
     * for the statistics on {@linkplain Statistics#differences() differences} - if
     * such statistics are wanted, they must be included in the given array.
     *
     * @param  stats       the statistics to format.
     * @param  toAppendTo  where to format the statistics.
     * @throws IOException if an error occurred while writing to the given appendable.
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
        final var table = new TableAppender(toAppendTo, separator);
        final Vocabulary resources = Vocabulary.forLocale(headerLocale);
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
         * Iterates over the rows to format (count, minimum, maximum, mean, RMS, standard deviation),
         * then iterate over columns (statistics on first set of sample values, on second set, etc.)
         * The NumberFormat configuration may be different for each column.
         */
        final Format countFormat = getFormat(Integer.class);
        final Format valueFormat = getFormat(Double.class);
        final Format[] formats = new Format[stats.length];
        for (int i=0; i<formats.length; i++) {
            formats[i] = configure(valueFormat, stats[i], i != 0);
        }
        for (int line=0; line < KEYS.length; line++) {
            if (line == 1 & !showNaNCount) {
                continue;
            }
            table.setCellAlignment(TableAppender.ALIGN_LEFT);
            table.append(resources.getString(KEYS[line])).append(':');
            for (int i=0; i<stats.length; i++) {
                final Statistics s = stats[i];
                final Number value;
                switch (line) {
                    case 0:  value = s.count();    break;
                    case 1:  value = s.countNaN(); break;
                    case 2:  value = s.minimum();  break;
                    case 3:  value = s.maximum();  break;
                    case 4:  value = s.mean();     break;
                    case 5:  value = s.rms();      break;
                    case 6:  value = s.standardDeviation(allPopulation); break;
                    default: throw new AssertionError(line);
                }
                table.append(beforeFill);
                table.nextColumn(fillCharacter);
                table.append((line >= 2 ? formats[i] : countFormat).format(value));
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
     * @param  format  the formatter to configure.
     * @param  stats   the statistics for which to configure the formatter.
     * @param  clone   whether to clone the given format before to modify it.
     * @return the formatter to use. May be a clone of the given formatter.
     */
    private static Format configure(final Format format, final Statistics stats, final boolean clone) {
        int multiplier = 1;
        if (format instanceof DecimalFormat) {
            var df = (DecimalFormat) format;
            multiplier = df.getMultiplier();
            /*
             * Check for scientific notation: the threshold below is high so that geocentric and projected
             * coordinates in metres are not formatted with scientific notation (a 1E+7 threshold is not
             * enough). If the numbers seem to require scientific notation, switch to that notation only
             * if the user has not already set a different number pattern.
             */
            final double extremum = Math.max(Math.abs(stats.minimum()), Math.abs(stats.maximum()));
            if (multiplier == 1 && (extremum >= 1E+10 || extremum <= 1E-4)) {
                final String pattern = df.toPattern();
                for (int i = pattern.length(); --i >= 0;) {
                    switch (pattern.charAt(i)) {
                        case '\'':                // Quote character: if present, user probably personalized the pattern.
                        case '¤':                 // Currency sign: not asked by super.createFormat(…), so assumed user format.
                        case 'E': return format;  // Scientific notation: not asked by super.createFormat(…), so assumed user format.
                    }
                }
                /*
                 * Apply the scientific notation on a clone in order to avoid misleading
                 * this 'configure' method next time we will format a Statistics object.
                 * The number of decimal digits in the pattern is arbitrary.
                 */
                df = (DecimalFormat) df.clone();
                df.applyPattern("0.00000E00");
                return df;
            }
        }
        /*
         * Numerics.suggestFractionDigits(stats) computes a representative range of values
         * based on 2 standard deviations away from the mean. For a gaussian distribution,
         * this covers 97.7% of data. If the data have a uniform distribution, then this is
         * 100% of data.
         */
        if (format instanceof NumberFormat) {
            int digits = Numerics.suggestFractionDigits(stats);
            digits -= DecimalFunctions.floorLog10(multiplier);
            digits = Math.max(0, digits);
            var nf = (NumberFormat) format;
            if (digits != nf.getMinimumFractionDigits() ||
                digits != nf.getMaximumFractionDigits())
            {
                if (clone) nf = (NumberFormat) nf.clone();
                nf.setMinimumFractionDigits(digits);
                nf.setMaximumFractionDigits(digits);
            }
            return nf;
        } else {
            // A future version could configure DateFormat here.
        }
        return format;
    }

    /**
     * Returns a clone of this format.
     *
     * @return a clone of this format.
     */
    @Override
    public StatisticsFormat clone() {
        return (StatisticsFormat) super.clone();
    }
}
