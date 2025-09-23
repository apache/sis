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
package org.apache.sis.referencing.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoField;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.util.internal.shared.Constants;


/**
 * Epoch of a coordinate set or of a dynamic reference frame.
 * This is a temporary object used for Well-Known Text formatting.
 * Contains also utility methods for conversion from/to temporal objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class Epoch extends FormattableObject {
    /**
     * Value of the epoch as a fractional year.
     */
    public final double value;

    /**
     * Number of fraction digits to use for formatting the fractional year.
     */
    public final int precision;

    /**
     * The keyword of the <abbr>WKT</abbr> element which will contain the epoch.
     * This is {@code "Epoch"}, {@code "FrameEpoch"} or {@code "AnchorEpoch"}.
     */
    private final String keyword;

    /**
     * Converts the given epoch to a fractional year.
     *
     * @param  epoch    the epoch to express as a fractional year.
     * @param  keyword  the keyword of the <abbr>WKT</abbr> element which will contain the epoch.
     */
    public Epoch(Temporal epoch, final String keyword) {
        this.keyword = keyword;
        if (epoch instanceof Instant) {
            epoch = OffsetDateTime.ofInstant((Instant) epoch, ZoneOffset.UTC);
        }
        final int year = epoch.get(ChronoField.YEAR);
        if (epoch instanceof Year) {
            value = year;
            precision = 0;
        } else {
            double day = 0;
            int fractionDigits = 3;             // Default value for a precision of 1 day.
            if (epoch instanceof YearMonth) {
                epoch = ((YearMonth) epoch).atDay(1);
                fractionDigits = 2;
            } else if (epoch.isSupported(ChronoField.NANO_OF_DAY)) {
                final long nano = epoch.getLong(ChronoField.NANO_OF_DAY);
                day = nano / (double) Constants.NANOSECONDS_PER_DAY;
                fractionDigits = ((nano % Constants.NANOS_PER_SECOND) != 0) ? 16 : 8;
            }
            day += epoch.get(ChronoField.DAY_OF_YEAR) - 1;
            value = year + day / Year.of(year).length();
            precision = Math.min(DecimalFunctions.fractionDigitsForValue(value) - 1, fractionDigits);
        }
    }

    /**
     * Returns the temporal object from the given year.
     * The type of the returned object depends on the {@code precision} argument:
     *
     * <li>
     *   <ul>= 0: returns {@link Year}, unless there is a fraction part in which case returns {@link YearMonth}.</ul>
     *   <ul>≤ 2: returns {@link YearMonth}.</ul>
     *   <ul>≤ 3: returns {@link LocalDate}.</ul>
     *   <ul>Other cases not yet implemented, but may be in the future.</ul>
     * </li>
     *
     * @param  epoch      the epoch as a fractional year.
     * @param  precision  number of valid digits in the given epoch.
     * @return the given epoch as a temporal object, or {@code null} if the given value is NaN.
     */
    public static Temporal fromYear(final double epoch, final int precision) {
        if (Double.isNaN(epoch)) {
            return null;
        }
        Year   year = Year.of((int) epoch);
        double time = epoch - year.getValue();
        long   day  = Math.round(time * year.length());
        if (day == 0 && precision <= 0) return year;
        final LocalDate date = year.atDay(Math.toIntExact(day + 1));
        if (precision <= 2) return year.atMonth(date.getMonth());
        return date;
    }

    /**
     * Returns the temporal object from a year given as a string.
     * The precision is determined by the number of digits that are explicitly written, even if zero.
     *
     * @param  epoch  the epoch as a fractional year.
     * @return the given epoch as a temporal object, or {@code null} if the given value is NaN.
     * @throws NumberFormatException if the given string cannot be parsed as number.
     */
    public static Temporal fromYear(final String epoch) {
        int precision = 0;
        int i = epoch.indexOf('.');
        if (i >= 0) {
            final int length = epoch.length();
            while (++i < length) {
                final char c = epoch.charAt(i);
                if (c < '0' || c > '9') break;
                precision++;
            }
        }
        return fromYear(Double.parseDouble(epoch), precision);
    }

    /**
     * Formats this epoch as a <i>Well Known Text</i> {@code CoordinateMetadata[…]} element.
     *
     * @param  formatter  the formatter where to format the inner content of this WKT element.
     * @return {@code "Epoch"}, {@code "FrameEpoch"} or {@code "AnchorEpoch"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        formatter.append(value, precision);
        return keyword;
    }
}
