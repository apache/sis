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
package org.apache.sis.coverage;

import java.util.Locale;
import java.text.NumberFormat;
import java.text.Format;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.opengis.util.GenericName;
import org.apache.sis.io.TableAppender;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.RangeFormat;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.util.internal.shared.Numerics;
import org.apache.sis.system.Configuration;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Formats the range of a category. This is used for {@link SampleDimension#toString()} implementation.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 */
@SuppressWarnings({"serial", "CloneableImplementsClone"})           // Not intended to serialized.
final class SampleRangeFormat extends RangeFormat {
    /**
     * Maximum value for {@link #numFractionDigits}. This is the number
     * of significant digits to allow when formatting real values.
     */
    @Configuration
    private static final int MAX_DIGITS = 6;

    /**
     * Number of significant digits used for formatting real values.
     */
    private int[] numFractionDigits;

    /**
     * {@code true} if the range of sample values is different than the range of real values, or
     * if there is qualitative categories with non NaN values. If {@code false}, then we can omit
     * the "Samples" column.
     */
    private boolean hasPackedValues;

    /**
     * Whether {@link #prepare(SampleDimension[])} found at least one quantitative category.
     * If {@code false}, then we can omit the "Measures" column.
     */
    private boolean hasQuantitative;

    /**
     * Whether at least one category of any type has been found in at least one sample dimension.
     */
    private boolean hasCategory;

    /**
     * The localize resources for table header. Words will be "Values", "Measures" and "Name".
     */
    private final Vocabulary words;

    /**
     * Index of the current sample dimension being formatted.
     */
    private int currentIndex;

    /**
     * Creates a new format for the given locale.
     *
     * @param locale   the locale for table header, category names and number format.
     */
    SampleRangeFormat(final Locale locale) {
        super(locale);
        words = Vocabulary.forLocale(locale);
    }

    /**
     * Computes the smallest number of fraction digits necessary to resolve all quantitative values.
     * This method assumes that real values in the range {@code Category.converse.range} are stored
     * as integer sample values in the range {@code Category.range}.
     */
    private void prepare(final SampleDimension[] dimensions) {
        final int count   = dimensions.length;
        numFractionDigits = new int[count];
        hasPackedValues   = false;
        hasQuantitative   = false;
        hasCategory       = false;
        for (int i=0; i<count; i++) {
            int ndigits = 0;
            for (final Category category : dimensions[i].getCategories()) {
                final NumberRange<?> sr = category.getSampleRange();
                final NumberRange<?> cr = category.converted().range;
                final double  smin = sr.getMinDouble(true);
                final double  smax = sr.getMaxDouble(false);
                final double  cmin = cr.getMinDouble(true);
                final double  cmax = cr.getMaxDouble(false);
                final boolean isPacked = (Double.doubleToRawLongBits(smin) != Double.doubleToRawLongBits(cmin))
                                       | (Double.doubleToRawLongBits(smax) != Double.doubleToRawLongBits(cmax));
                hasPackedValues |= isPacked;
                hasCategory = true;
                /*
                 * If the sample values are already real values, pretend that they are packed in bytes.
                 * The intent is only to compute an arbitrary number of fraction digits.
                 */
                final double range = isPacked ? (smax - smin) : 256;
                final double increment =        (cmax - cmin) / range;
                if (!Double.isNaN(increment)) {
                    hasQuantitative = true;
                    final int n = -Numerics.toExp10(Math.getExponent(increment));
                    if (n > ndigits) {
                        ndigits = n;
                    }
                }
            }
            if (ndigits >= MAX_DIGITS) {
                ndigits = MAX_DIGITS;
            }
            numFractionDigits[i] = ndigits;
        }
    }

    /**
     * Formats a sample value or a range of sample value.
     * The value should have been fetched by {@link Category#getRangeLabel()}.
     * This method applies the following rules:
     *
     * <ul>
     *   <li>If the value is a number, check if we should use scientific notation.</li>
     *   <li>If the value is a range, discard the unit of measurement if any.
     *       We do that because the range may be repeated in the "Measure" column with units.</li>
     * </ul>
     */
    private String formatSample(final Object value) {
        if (value instanceof Number) {
            return Numerics.useScientificNotationIfNeeded(elementFormat, value, Format::format).concat(" ");
        } else if (value instanceof Range<?>) {
            if (value instanceof MeasurementRange<?>) {
                /*
                 * Probably the same range as the one to be formatted in the "Measure" column.
                 * Format it in the same way (same number of fraction digits) but without units.
                 */
                return formatMeasure(new NumberRange<>((MeasurementRange<?>) value));
            } else {
                return format(value);
            }
        } else {
            return String.valueOf(value);
        }
    }

    /**
     * Formats a range of measurements. There is usually only zero or one range of measurement per {@link SampleDimension},
     * but {@code SampleRangeFormat} is not restricted to that limit. The number of fraction digits to use should have been
     * computed by {@link #prepare(SampleDimension[])} before to call this method.
     *
     * @return the range to write, or {@code null} if the given {@code range} argument was null.
     */
    private String formatMeasure(final Range<?> range) {
        final var nf  = (NumberFormat) elementFormat;
        final int min = nf.getMinimumFractionDigits();
        final int max = nf.getMaximumFractionDigits();
        final int ndigits = numFractionDigits[currentIndex];
        nf.setMinimumFractionDigits(ndigits);
        nf.setMaximumFractionDigits(ndigits);
        final String text = format(range);
        nf.setMinimumFractionDigits(min);
        nf.setMaximumFractionDigits(max);
        return text;
    }

    /**
     * Formats a string representation of the given list of categories.
     * This method formats a table like below:
     *
     * <pre class="text">
     *   ┌────────────┬────────────────┬─────────────┐
     *   │   Values   │    Measures    │    Name     │
     *   ╞════════════╧════════════════╧═════════════╡
     *   │Band 1                                     │
     *   ├────────────┬────────────────┬─────────────┤
     *   │         0  │ NaN #0         │ No data     │
     *   │         1  │ NaN #1         │ Clouds      │
     *   │         5  │ NaN #5         │ Lands       │
     *   │ [10 … 200) │ [6.0 … 25.0)°C │ Temperature │
     *   └────────────┴────────────────┴─────────────┘</pre>
     *
     * @param dimensions  the list of sample dimensions to format.
     */
    String write(final SampleDimension[] dimensions) {
        prepare(dimensions);
        /*
         * Write table header: │ Values │ Measures │ name │
         */
        final var buffer = new StringBuilder(800);
        final var table = new TableAppender(buffer, " │ ");
        table.setMultiLinesCells(true);
        table.appendHorizontalSeparator();
        table.setCellAlignment(TableAppender.ALIGN_CENTER);
        if (hasPackedValues) table.append(words.getString(Vocabulary.Keys.Values))  .nextColumn();
        if (hasQuantitative) table.append(words.getString(Vocabulary.Keys.Measures)).nextColumn();
        /* Unconditional  */ table.append(words.getString(Vocabulary.Keys.Name))    .nextLine();
        if (!hasCategory) {
            table.nextLine('═');
            table.setCellAlignment(TableAppender.ALIGN_LEFT);
            for (final SampleDimension dim : dimensions) {
                table.append(getName(dim));
                table.nextLine();
            }
        } else {
            for (final SampleDimension dim : dimensions) {
                table.nextLine('═');
                table.append('#');                      // Dummy character to be replaced by band name later.
                table.appendHorizontalSeparator();
                for (final Category category : dim.getCategories()) {
                    /*
                     * "Sample values" column. Omitted if all values are already real values.
                     */
                    if (hasPackedValues) {
                        table.setCellAlignment(TableAppender.ALIGN_RIGHT);
                        table.append(formatSample(category.getRangeLabel()));
                        table.nextColumn();
                    }
                    table.setCellAlignment(TableAppender.ALIGN_LEFT);
                    /*
                     * "Real values" column. Omitted if no category has a transfer function.
                     */
                    if (hasQuantitative) {
                        final Category converted = category.converted();
                        final String text;
                        if (converted.isConvertedQualitative()) {
                            text = String.valueOf(converted.getRangeLabel());       // Example: NaN #0
                        } else {
                            text = formatMeasure(converted.getSampleRange());       // Example: [6.0 … 25.0)°C
                        }
                        table.append(text);
                        table.nextColumn();
                    }
                    table.append(category.getName().toString(getLocale()));
                    table.nextLine();
                }
            }
        }
        table.appendHorizontalSeparator();
        try {
            table.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);      // Should never happen since we write to a StringBuilder.
        }
        /*
         * After we formatted the table, insert the sample dimension names before each category lists.
         * We do that after formatting table because TableAppender currently has no API for spanning
         * a value on many cells. The following code changes some characters but do not change buffer
         * length.
         */
        if (hasCategory) {
            int lastDimensionEnd = 0;
            final String lineSeparator = table.getLineSeparator();
            final String toSearch = lineSeparator + '╞';
            for (final SampleDimension dim : dimensions) {
                int lineStart = buffer.indexOf(toSearch, lastDimensionEnd);
                if (lineStart < 0) break;                                           // Should not happen.
                lineStart += toSearch.length();
                int i = replace(buffer, lineStart, '╪', '╧', '╡');
                int limit = (i-2) - lineStart;                                      // Space available in a row.
                i += lineSeparator.length() + 2;                                    // Beginning of next line.
                /*
                 * At this point, 'i' is at the beginning of the row where to format the band name.
                 * The line above that row has been modified for removing vertical lines. Now fill
                 * the space in current row with band name and pad with white spaces.
                 */
                String name = getName(dim);
                if (name.length() > limit) {
                    name = name.substring(0, limit);
                }
                limit += i;                                         // Now an absolute index instead of a length.
                buffer.replace(i, i += name.length(), name);
                while (i < limit) buffer.setCharAt(i++, ' ');
                /*
                 * At this point the sample dimension name has been written.
                 * Update the next line and move to the next sample dimension.
                 */
                lastDimensionEnd = replace(buffer, i + lineSeparator.length() + 2, '┼', '┬', '┤');
            }
        }
        return buffer.toString();
    }

    /**
     * Returns the localized name for the given dimension, or "unnamed" if none.
     */
    private String getName(final SampleDimension dim) {
        final GenericName name = dim.getName();
        if (name != null) {
            return name.toInternationalString().toString(getLocale());
        } else {
            return words.getString(Vocabulary.Keys.Unnamed);
        }
    }

    /**
     * Replaces characters in the given buffer until a sentinel value, which must exist.
     *
     * @param  buffer   the buffer where to perform the replacements.
     * @param  i        index of the first character to check.
     * @param  search   character to search for replacement.
     * @param  replace  character to use as a replacement.
     * @param  stop     sentinel value for stopping the search.
     * @return index after the sentinel value.
     */
    private static int replace(final StringBuilder buffer, int i, final char search, final char replace, final char stop) {
        char c;
        do {
            c = buffer.charAt(i);
            if (c == search) {
                buffer.setCharAt(i, replace);
            }
            i++;
        } while (c != stop);
        return i;
    }
}
