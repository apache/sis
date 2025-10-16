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
package org.apache.sis.storage.csv;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.measure.Unit;
import javax.measure.quantity.Time;
import org.opengis.referencing.datum.TemporalDatum;
import org.apache.sis.converter.SurjectiveConverter;
import org.apache.sis.temporal.LenientDateFormat;
import org.apache.sis.temporal.TemporalDate;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.measure.Units;


/**
 * Specifies how time is encoded in the CSV file.
 * Time values are formatted as numbers of seconds or minutes since an epoch,
 * except in the special case of {@link #ABSOLUTE} encoding.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
class TimeEncoding extends SurjectiveConverter<String,Instant> {
    /**
     * Times are formatted as ISO dates.
     */
    static final TimeEncoding ABSOLUTE = new TimeEncoding(CommonCRS.defaultTemporal().getDatum(), Units.DAY) {
        @Override public Instant apply(final String time) {
            return LenientDateFormat.parseInstantUTC(time);
        }
    };

    /**
     * Date of value zero on the time axis.
     */
    private final Instant origin;

    /**
     * Number of seconds between two consecutive integer values on the time axis.
     */
    private final double interval;

    /**
     * Creates a new time encoding.
     */
    TimeEncoding(final TemporalDatum datum, final Unit<Time> unit) {
        this.origin   = TemporalDate.toInstant(datum.getOrigin(), null);
        this.interval = unit.getConverterTo(Units.SECOND).convert(1);
    }

    /**
     * Returns the type of values to convert.
     */
    @Override
    public final Class<String> getSourceClass() {
        return String.class;
    }

    /**
     * Returns the type of converted values.
     */
    @Override
    public final Class<Instant> getTargetClass() {
        return Instant.class;
    }

    /**
     * Returns the instant for the given string, which is usually a time elapsed since the CRS temporal origin.
     *
     * @param  time  the string representation of the time to parse, often as a number since the CRS temporal origin.
     * @return the instant parsed from the given string.
     */
    @Override
    public Instant apply(final String time) {
        return TemporalDate.addSeconds(origin, Double.parseDouble(time) * interval);
    }

    /**
     * Converts the given timestamp to the values used in the temporal coordinate reference system.
     *
     * @param  time  instant to convert.
     * @return the value to use with the temporal coordinate reference system.
     */
    final double toCRS(final Instant time) {
        return origin.until(time, ChronoUnit.SECONDS) / interval;
    }
}
