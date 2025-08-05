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
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoField;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.math.DecimalFunctions;
import org.apache.sis.referencing.privy.WKTKeywords;
import org.apache.sis.util.privy.Constants;


/**
 * Epoch of a coordinate set or of a dynamic reference frame.
 * This is a temporary object used for Well-Known Text formatting.
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
     * Converts the given epoch to a fractional year.
     *
     * @param  epoch  the epoch to express as a fractional year.
     */
    public Epoch(Temporal epoch) {
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
     * Formats this epoch as a <i>Well Known Text</i> {@code CoordinateMetadata[…]} element.
     *
     * @param  formatter  the formatter where to format the inner content of this WKT element.
     * @return {@code "Epoch"}.
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        formatter.append(value, precision);
        return WKTKeywords.Epoch;
    }
}
