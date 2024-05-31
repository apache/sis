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
package org.apache.sis.pending.temporal;

import java.util.Date;
import java.time.temporal.Temporal;
import org.opengis.temporal.TemporalPrimitive;
import org.apache.sis.util.privy.TemporalDate;

// Specific to the main branch:
import org.apache.sis.pending.geoapi.temporal.Period;


/**
 * Utilities related to ISO 19108 objects.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 */
public final class TemporalUtilities {
    /**
     * Do not allow instantiation of this class.
     */
    private TemporalUtilities() {
    }

    /**
     * Creates an instant for the given date.
     *
     * @param  time  the date for which to create instant, or {@code null}.
     * @return the instant, or {@code null} if the given time was null.
     */
    public static TemporalPrimitive createInstant(final Date time) {
        return (time == null) ? null : createInstant(time.toInstant());
    }

    /**
     * Creates an instant for the given Java temporal instant.
     *
     * @param  time  the date for which to create instant, or {@code null}.
     * @return the instant, or {@code null} if the given time was null.
     */
    public static TemporalPrimitive createInstant(final Temporal time) {
        return (time == null) ? null : new DefaultPeriod(time, time);
    }

    /**
     * Creates a period for the given begin and end instant.
     *
     * @param  begin  the begin instant (inclusive), or {@code null}.
     * @param  end    the end instant (inclusive), or {@code null}.
     * @return the period, or {@code null} if both arguments are null.
     *
     * @todo Needs to avoid assuming UTC timezone.
     */
    public static TemporalPrimitive createPeriod(final Temporal begin, final Temporal end) {
        return (begin == null && end == null) ? null : new DefaultPeriod(begin, end);
    }

    /**
     * Returns the given value as a period if it is not a single point in time, or {@code null} otherwise.
     * This method is mutually exclusive with {@link #getInstant(TemporalPrimitive)}: if one method returns
     * a non-null value, then the other method shall return a null value.
     *
     * @param  time  the instant or period for which to get a time range, or {@code null}.
     * @return the period, or {@code null} if none.
     */
    public static Period getPeriod(final TemporalPrimitive time) {
        if (time instanceof Period) {
            var p = (Period) time;
            final Temporal begin = p.getBeginning();
            if (begin != null) {
                final Temporal end = p.getEnding();
                if (end != null && !begin.equals(end)) {
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * Returns the given value as an ISO 19108 instant (a temporal in Java API)
     * if the period is a single point in time, or {@code null} otherwise.
     * This method is mutually exclusive with {@link #getPeriod(TemporalPrimitive)}:
     * if one method returns a non-null value, then the other method shall return a null value.
     *
     * @param  time  the instant or period for which to get a date, or {@code null}.
     * @return the ISO 19108 instant, or {@code null} if none.
     */
    public static Temporal getInstant(final TemporalPrimitive time) {
        if (time instanceof Period) {
            var p = (Period) time;
            final Temporal begin = p.getBeginning();
            final Temporal end   = p.getEnding();
            if (end == null) {
                return begin;
            }
            if (begin == null || begin.equals(end)) {
                return end;
            }
        }
        return null;
    }

    /**
     * Infers a value from the extent as a {@link Date} object.
     * This method is used for compatibility with legacy API and may disappear in future SIS version.
     *
     * @param  time  the instant or period for which to get a date, or {@code null}.
     * @return the requested time as a Java date, or {@code null} if none.
     */
    public static Date getAnyDate(final TemporalPrimitive time) {
        if (time instanceof Period) {
            var p = (Period) time;
            Temporal instant;
            if ((instant = p.getEnding()) != null || (instant = p.getBeginning()) != null) {
                return TemporalDate.toDate(instant);
            }
        }
        return null;
    }
}
