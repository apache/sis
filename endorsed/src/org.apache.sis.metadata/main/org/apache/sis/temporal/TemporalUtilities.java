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
package org.apache.sis.temporal;

import java.util.Date;
import java.time.temporal.Temporal;
import org.opengis.temporal.TemporalPrimitive;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.temporal.IndeterminateValue;
import org.opengis.temporal.Instant;
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
     * Creates an instant for the given Java temporal instant.
     *
     * @param  time  the date for which to create instant, or {@code null}.
     * @return the instant, or an unknown instant if the given time was null.
     */
    public static Instant createInstant(final Temporal time) {
        return (time == null) ? DefaultInstant.UNKNOWN : new DefaultInstant(time, null);
    }

    /**
     * Creates an instant for the given Java temporal instant associated to the indeterminate value.
     * This is used for creating "before" or "after" instant.
     *
     * @param  time   the date for which to create instant.
     * @param  value  the indeterminate value.
     * @return the instant.
     */
    public static Instant createInstant(final Temporal time, final IndeterminateValue value) {
        ArgumentChecks.ensureNonNull("value", value);
        if (value == IndeterminateValue.UNKNOWN) {
            return DefaultInstant.UNKNOWN;
        }
        if (value == IndeterminateValue.BEFORE || value == IndeterminateValue.AFTER) {
            ArgumentChecks.ensureNonNull("time", time);
        }
        return new DefaultInstant(time, value);
    }

    /**
     * Creates an instant for the given indeterminate value.
     * The given value cannot be "before" or "after".
     *
     * @param  value  the indeterminate value.
     * @return the instant for the given indeterminate value.
     * @throws IllegalArgumentException if the given value is "before" or "after".
     */
    public static Instant createInstant(final IndeterminateValue value) {
        ArgumentChecks.ensureNonNull("value", value);
        if (value == IndeterminateValue.BEFORE || value == IndeterminateValue.AFTER) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "value", value));
        }
        return (value == IndeterminateValue.UNKNOWN) ? DefaultInstant.UNKNOWN : new DefaultInstant(null, value);
    }

    /**
     * Creates a period for the given begin and end instant.
     *
     * @param  beginning  the begin instant (inclusive), or {@code null}.
     * @param  ending     the end instant (exclusive), or {@code null}.
     * @return the period, or {@code null} if both arguments are null.
     */
    public static Period createPeriod(final Temporal beginning, final Temporal ending) {
        if (beginning == null && ending == null) {
            return null;
        }
        return new DefaultPeriod(createInstant(beginning), createInstant(ending));
    }

    /**
     * Creates a period for the given begin and end instant.
     *
     * @param  beginning  the begin instant (inclusive), or {@code null}.
     * @param  ending     the end instant (exclusive), or {@code null}.
     * @return the period, or {@code null} if both arguments are null.
     */
    public static Period createPeriod(Instant beginning, Instant ending) {
        if (beginning == null && ending == null) {
            return null;
        }
        if (beginning == null) beginning = DefaultInstant.UNKNOWN;
        if    (ending == null)    ending = DefaultInstant.UNKNOWN;
        return new DefaultPeriod(beginning, ending);
    }

    /**
     * Returns the given value as a temporal position, or {@code null} if not available.
     *
     * @param  time  the instant or period for which to get a date, or {@code null}.
     * @return the temporal position, or {@code null} if indeterminate.
     */
    public static Temporal getInstant(final TemporalPrimitive time) {
        if (time instanceof Instant) {
            return ((Instant) time).getPosition();
        }
        return null;
    }

    /**
     * Infers a value from the instant or extent as a {@link Date} object.
     * This method is used for compatibility with legacy API and may disappear in future SIS version.
     *
     * @param  time  the instant or period for which to get a date, or {@code null}.
     * @return the requested time as a Java date, or {@code null} if none.
     */
    public static Date getAnyDate(final TemporalPrimitive time) {
        Temporal t = null;
        if (time instanceof Instant) {
            t = ((Instant) time).getPosition();
        } else if (time instanceof Period) {
            final var p = (Period) time;
            Instant i = p.getEnding();      // Should never be null, but we are paranoiac.
            if (i == null || (t = i.getPosition()) == null) {
                i = p.getBeginning();
                if (i != null) {
                    t = i.getPosition();
                }
            }
        }
        return TemporalDate.toDate(t);
    }
}
