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
import org.opengis.util.InternationalString;
import org.apache.sis.io.TableAppender;
import org.apache.sis.measure.Range;
import org.apache.sis.measure.RangeFormat;
import org.apache.sis.internal.util.Numerics;
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
     * This method assumes that real values in the range {@code Category.converted.range} are stored
     * as integer sample values in the range {@code Category.range}.
     *
     * @return {@code true} if at least one quantitative category has been found.
     */
    private boolean prepare(final List<Category> categories) {
        ndigits = 0;
        boolean hasQuantitative = false;
        for (final Category category : categories) {
            final Category converted = category.converted;
            final double increment = (converted.maximum - converted.minimum)
                                   / ( category.maximum -  category.minimum);
            if (!Double.isNaN(increment)) {
                hasQuantitative = true;
                final int n = 1 - Numerics.toExp10(Math.getExponent(increment));
                if (n > ndigits) {
                    ndigits = n;
                    if (n >= MAX_DIGITS) {
                        ndigits = MAX_DIGITS;
                        break;
                    }
                }
            }
        }
        return hasQuantitative;
    }

    /**
     * Formats a sample value or a range of sample value.
     * The value should have been fetched by {@link Category#getRangeLabel()}.
     */
    private String formatSample(final Object value) {
        if (value instanceof Number) {
            return elementFormat.format(value).concat(" ");
        } else if (value instanceof Range<?>) {
            return format(value);
        } else {
            return String.valueOf(value);
        }
    }

    /**
     * Formats a range of measurements. There is usually only zero or one range of measurement per {@link SampleDimension},
     * but {@code SampleRangeFormat} is not restricted to that limit. The number of fraction digits to use should have been
     * computed by {@link #prepare(List)} before to call this method.
     */
    private String formatMeasure(final Range<?> range) {
        if (range == null) {
            return "";
        }
        final NumberFormat nf = (NumberFormat) elementFormat;
        final int min = nf.getMinimumFractionDigits();
        final int max = nf.getMaximumFractionDigits();
        try {
            nf.setMinimumFractionDigits(ndigits);
            nf.setMaximumFractionDigits(ndigits);
            return format(range);
        } finally {
            nf.setMinimumFractionDigits(min);
            nf.setMaximumFractionDigits(max);
        }
    }

    /**
     * Returns a string representation of the given list of categories.
     *
     * @param title       caption for the table.
     * @param categories  the list of categories to format.
     */
    final String format(final InternationalString title, final CategoryList categories) {
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
     *
     * @param title       caption for the table.
     * @param categories  the list of categories to format.
     * @param out         where to write the category table.
     */
    void format(final InternationalString title, final CategoryList categories, final Appendable out) throws IOException {
        final String lineSeparator = System.lineSeparator();
        out.append(title.toString(getLocale())).append(lineSeparator);
        final TableAppender table  = new TableAppender(out, " │ ");
        final boolean hasQuantitative = prepare(categories);
        table.appendHorizontalSeparator();
        table.setCellAlignment(TableAppender.ALIGN_CENTER);
        table.append(words.getString(Vocabulary.Keys.Values)).nextColumn();
        if (hasQuantitative) {
            table.append(words.getString(Vocabulary.Keys.Measures)).nextColumn();
        }
        table.append(words.getString(Vocabulary.Keys.Name)).nextLine();
        table.appendHorizontalSeparator();
        for (final Category category : categories) {
            table.setCellAlignment(TableAppender.ALIGN_RIGHT);
            table.append(formatSample(category.getRangeLabel()));
            table.nextColumn();
            if (hasQuantitative) {
                table.append(formatMeasure(category.converted.range));
                table.nextColumn();
            }
            table.setCellAlignment(TableAppender.ALIGN_LEFT);
            table.append(category.name.toString(getLocale()));
            table.nextLine();
        }
        table.appendHorizontalSeparator();
        table.flush();
    }
}
