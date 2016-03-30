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

import javax.measure.unit.Unit;
import javax.measure.unit.NonSI;
import javax.measure.quantity.Duration;
import org.opengis.referencing.datum.TemporalDatum;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.measure.Units;


/**
 * Specifies how time is encoded in the CSV file.
 * Time values are formatted as numbers of seconds or minutes since an epoch,
 * except in the special case of {@link #ABSOLUTE} encoding.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class TimeEncoding {
    /**
     * The temporal coordinate reference system to use for {@link #ABSOLUTE} time encoding.
     */
    static final CommonCRS.Temporal DEFAULT = CommonCRS.Temporal.TRUNCATED_JULIAN;

    /**
     * Times are formatted as ISO dates.
     */
    static final TimeEncoding ABSOLUTE = new TimeEncoding(DEFAULT.datum(), NonSI.DAY);

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
    TimeEncoding(final TemporalDatum datum, final Unit<Duration> unit) {
        this.origin   = datum.getOrigin().getTime();
        this.interval = unit.getConverterTo(Units.MILLISECOND).convert(1);
    }

    /**
     * Converts the given timestamp to the values used in the temporal coordinate reference system.
     *
     * @param time Number of milliseconds elapsed since January 1st, 1970 midnight UTC.
     * @return The value to use with the temporal coordinate reference system.
     */
    final double toCRS(final long time) {
        return (time - origin) / interval;
    }

    /**
     * Reverse of {@link #toCRS(long)}.
     *
     * @param time The value used with the temporal coordinate reference system.
     * @return Number of milliseconds elapsed since January 1st, 1970 midnight UTC.
     */
    final long toMillis(final double time) {
        return Math.round(time * interval) + origin;
    }
}
