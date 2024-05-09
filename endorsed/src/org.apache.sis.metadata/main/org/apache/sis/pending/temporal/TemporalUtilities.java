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
import java.time.Instant;
import org.opengis.temporal.TemporalPrimitive;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.temporal.Period;


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
     * @throws UnsupportedOperationException if the temporal factory is not available on the module path.
     */
    public static TemporalPrimitive createInstant(final Date time) throws UnsupportedOperationException {
        if (time == null) return null;
        final Instant t = time.toInstant();
        return new DefaultPeriod(t, t);
    }

    /**
     * Creates a period for the given begin and end dates.
     *
     * @param  begin  the begin date (inclusive), or {@code null}.
     * @param  end    the end date (inclusive), or {@code null}.
     * @return the period, or {@code null} if both arguments are null.
     * @throws UnsupportedOperationException if the temporal factory is not available on the module path.
     */
    public static TemporalPrimitive createPeriod(final Date begin, final Date end) throws UnsupportedOperationException {
        if (begin == null && end == null) return null;
        return new DefaultPeriod(
                (begin != null) ? begin.toInstant() : null,
                  (end != null) ?   end.toInstant() : null);
    }

    /**
     * Returns the given value as an instant if the period is a single point in time, or {@code null} otherwis.
     *
     * @param  time  the instant or period for which to get a date, or {@code null}.
     * @return the instant, or {@code null} if none.
     */
    public static Instant getInstant(final TemporalPrimitive time) {
        if (time instanceof Period) {
            var p = (Period) time;
            final Instant begin = p.getBeginning();
            final Instant end = p.getEnding();
            if (begin == null) return end;
            if (end == null) return begin;
            if (begin.equals(end)) return end;
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
            Instant instant;
            if ((instant = p.getEnding()) != null || (instant = p.getBeginning()) != null) {
                return Date.from(instant);
            }
        }
        return null;
    }
}
