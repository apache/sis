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
package org.apache.sis.internal.storage.csv;

import javax.measure.Unit;
import javax.measure.quantity.Time;
import org.opengis.referencing.datum.TemporalDatum;
import org.apache.sis.internal.converter.SurjectiveConverter;
import org.apache.sis.internal.util.StandardDateFormat;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.measure.Units;

// Branch-dependent imports
import org.apache.sis.internal.jdk8.Instant;


/**
 * Specifies how time is encoded in the CSV file.
 * Time values are formatted as numbers of seconds or minutes since an epoch,
 * except in the special case of {@link #ABSOLUTE} encoding.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 0.8
 * @since   0.7
 * @module
 */
class TimeEncoding extends SurjectiveConverter<String,Instant> {
    /**
     * The temporal coordinate reference system to use for {@link #ABSOLUTE} time encoding.
     */
    static final CommonCRS.Temporal DEFAULT = CommonCRS.Temporal.TRUNCATED_JULIAN;

    /**
     * Times are formatted as ISO dates.
     */
    static final TimeEncoding ABSOLUTE = new TimeEncoding(DEFAULT.datum(), Units.DAY) {
        @Override public Instant apply(final String time) {
            return Instant.parse(time);
        }
    };

    /**
     * Date of value zero on the time axis, in milliseconds since January 1st 1970 at midnight UTC.
     */
    private final long origin;

    /**
     * Number of milliseconds between two consecutive integer values on the time axis.
     */
    private final double interval;

    /**
     * Creates a new time encoding.
     */
    TimeEncoding(final TemporalDatum datum, final Unit<Time> unit) {
        this.origin   = datum.getOrigin().getTime();
        this.interval = unit.getConverterTo(Units.MILLISECOND).convert(1);
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
        final double value = Double.parseDouble(time) * interval;
        final long millis = Math.round(value);
        return Instant.ofEpochMilli(millis + origin)
                      .plusNanos(Math.round((value - millis) * StandardDateFormat.NANOS_PER_MILLISECOND));
        /*
         * Performance note: the call to .plusNano(…) will usually return the same 'Instant' instance
         * (without creating new object) since the time granularity is rarely finer than milliseconds.
         */
    }

    /**
     * Converts the given timestamp to the values used in the temporal coordinate reference system.
     *
     * @param  time  number of milliseconds elapsed since January 1st, 1970 midnight UTC.
     * @return the value to use with the temporal coordinate reference system.
     */
    final double toCRS(final long time) {
        return (time - origin) / interval;
    }
}
