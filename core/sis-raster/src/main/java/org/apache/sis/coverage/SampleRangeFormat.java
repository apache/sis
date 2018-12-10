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

import java.util.List;
import java.util.Locale;
import java.text.NumberFormat;
import java.io.IOException;
import java.text.DecimalFormat;
import org.opengis.util.GenericName;
import org.apache.sis.io.TableAppender;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.RangeFormat;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.measure.MeasurementRange;
import org.apache.sis.util.resources.Vocabulary;


/**
 * Formats the range of a category. This is used for {@link SampleDimension#toString()} implementation.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
@SuppressWarnings({"CloneableClassWithoutClone", "serial"})         // Not intended to be cloned or serialized.
final class SampleRangeFormat extends RangeFormat {
    /**
     * Maximum value for {@link #ndigits}. This is the number of
     * significant digits to allow when formatting real values.
     */
    private static final int MAX_DIGITS = 6;

    /**
     * Number of significant digits used for formatting real values.
     */
    private int ndigits;

    /**
     * {@code true} if the range of sample values is different than the range of real values, or
     * if there is qualitative categories. If {@code false}, then we can omit the "Samples" column.
     */
    private boolean hasPackedValues;

    /**
     * Whether {@link #prepare(List)} found at least one quantitative category.
     * If {@code false}, then we can omit the "Measures" column.
     */
    private boolean hasQuantitative;

    /**
     * The localize resources for table header. Words will be "Values", "Measures" and "Name".
     */
    private final Vocabulary words;

    /**
     * Creates a new format for the given locale.
     *
     * @param locale   the locale for table header, category names and number format.
     */
    SampleRangeFormat(final Locale locale) {
        super(locale);
        words = Vocabulary.getResources(locale);
    }

    /**
     * Computes the smallest number of fraction digits necessary to resolve all quantitative values.
     * This method assumes that real values in the range {@code Category.converse.range} are stored
     * as integer sample values in the range {@code Category.range}.
     */
    private void prepare(final List<Category> categories) {
        ndigits         = 0;
        hasPackedValues = false;
        hasQuantitative = false;
        for (final Category category : categories) {
            final Category converted = category.converted();
            final boolean  isPacked  = (category.minimum != converted.minimum)
                                     | (category.maximum != converted.maximum);
            hasPackedValues |= isPacked;
            /*
             * If the sample values are already real values, pretend that they are packed in bytes.
             * The intent is only to compute an arbitrary number of fraction digits.
             */
            final double range = isPacked ? ( category.maximum -  category.minimum) : 255;
            final double increment =        (converted.maximum - converted.minimum) / range;
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
    private String formatSample(Object value) {
        if (value instanceof Number) {
            final double m = Math.abs(((Number) value).doubleValue());
            final String text;
            if ((m >= 1E+9 || m < 1E-4) && elementFormat instanceof DecimalFormat) {
                final DecimalFormat df = (DecimalFormat) elementFormat;
                final String pattern = df.toPattern();
                df.applyPattern("0.######E00");
                text = df.format(value);
                df.applyPattern(pattern);
            } else {
                text = elementFormat.format(value);
            }
            return text.concat(" ");
        } else if (value instanceof Range<?>) {
            if (value instanceof MeasurementRange<?>) {
                /*
                 * Probably the same range than the one to be formatted in the "Measure" column.
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
     * computed by {@link #prepare(List)} before to call this method.
     *
     * @return the range to write, or {@code null} if the given {@code range} argument was null.
     */
    private String formatMeasure(final Range<?> range) {
        if (range == null) {
            return null;
        }
        final NumberFormat nf = (NumberFormat) elementFormat;
        final int min = nf.getMinimumFractionDigits();
        final int max = nf.getMaximumFractionDigits();
        nf.setMinimumFractionDigits(ndigits);
        nf.setMaximumFractionDigits(ndigits);
        final String text = format(range);
        nf.setMinimumFractionDigits(min);
        nf.setMaximumFractionDigits(max);
        return text;
    }

    /**
     * Returns a string representation of the given list of categories.
     *
     * @param title       caption for the table.
     * @param categories  the list of categories to format.
     */
    final String format(final GenericName title, final CategoryList categories) {
        final StringBuilder buffer = new StringBuilder(800);
        try {
            format(title, categories, buffer);
        } catch (IOException e) {
            throw new AssertionError(e);    // Should never happen since we write to a StringBuilder.
        }
        return buffer.toString();
    }

    /**
     * Formats a string representation of the given list of categories.
     * This method formats a table like below:
     *
     * {@preformat text
     *   ┌────────────┬────────────────┬─────────────┐
     *   │   Values   │    Measures    │    Name     │
     *   ├────────────┼────────────────┼─────────────┤
     *   │         0  │ NaN #0         │ No data     │
     *   │         1  │ NaN #1         │ Clouds      │
     *   │         5  │ NaN #5         │ Lands       │
     *   │ [10 … 200) │ [6.0 … 25.0)°C │ Temperature │
     *   └────────────┴────────────────┴─────────────┘
     * }
     *
     * @param title       caption for the table.
     * @param categories  the list of categories to format.
     * @param out         where to write the category table.
     */
    void format(final GenericName title, final CategoryList categories, final Appendable out) throws IOException {
        prepare(categories);
        final String lineSeparator = System.lineSeparator();
        out.append(title.toInternationalString().toString(getLocale())).append(lineSeparator);
        /*
         * Write table header: │ Values │ Measures │ name │
         */
        final TableAppender table = new TableAppender(out, " │ ");
        table.appendHorizontalSeparator();
        table.setCellAlignment(TableAppender.ALIGN_CENTER);
        if (hasPackedValues) table.append(words.getString(Vocabulary.Keys.Values))  .nextColumn();
        if (hasQuantitative) table.append(words.getString(Vocabulary.Keys.Measures)).nextColumn();
        /* Unconditional  */ table.append(words.getString(Vocabulary.Keys.Name))    .nextLine();
        table.appendHorizontalSeparator();
        for (final Category category : categories) {
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
                String text = formatMeasure(converted.range);               // Example: [6.0 … 25.0)°C
                if (text == null) {
                    text = String.valueOf(converted.getRangeLabel());       // Example: NaN #0
                }
                table.append(text);
                table.nextColumn();
            }
            table.append(category.name.toString(getLocale()));
            table.nextLine();
        }
        table.appendHorizontalSeparator();
        table.flush();
    }
}
