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
package org.apache.sis.internal.temporal;

import java.util.Objects;
import org.opengis.temporal.PeriodDuration;
import org.opengis.util.InternationalString;


/**
 * Default implementation of GeoAPI period duration. This is a temporary class;
 * GeoAPI temporal interfaces are expected to change a lot in a future revision.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
final class DefaultPeriodDuration implements PeriodDuration {
    /** Components of this period duration. */
    private final InternationalString years, months, week, days, hours, minutes, seconds;

    /**
     * Creates a new duration.
     */
    DefaultPeriodDuration(
            final InternationalString years, final InternationalString months,
            final InternationalString week,  final InternationalString days,
            final InternationalString hours, final InternationalString minutes, final InternationalString seconds)
    {
        this.years   = years;
        this.months  = months;
        this.week    = week;
        this.days    = days;
        this.hours   = hours;
        this.minutes = minutes;
        this.seconds = seconds;
    }


    @Override public InternationalString getDesignator()    {return null;}
    @Override public InternationalString getYears()         {return years;}
    @Override public InternationalString getMonths()        {return months;}
    @Override public InternationalString getDays()          {return days;}
    @Override public InternationalString getTimeIndicator() {return null;}
    @Override public InternationalString getHours()         {return hours;}
    @Override public InternationalString getMinutes()       {return minutes;}
    @Override public InternationalString getSeconds()       {return seconds;}

    /** String representation. */
    @Override public String toString() {
        return "PeriodDuration[" + years + '-' + months + '-' + days + ' ' + hours + ':' + minutes + ':' + seconds + ']';
    }

    /** Hash code value of the time position. */
    @Override public int hashCode() {
        return Objects.hash(years, months, week, days, hours, minutes, seconds);
    }

    /** Compares with given object for equality. */
    @Override public boolean equals(final Object obj) {
        if (obj instanceof DefaultPeriodDuration) {
            DefaultPeriodDuration other = (DefaultPeriodDuration) obj;
            return Objects.equals(other.years,   years) &&
                   Objects.equals(other.months,  months) &&
                   Objects.equals(other.week,    week) &&
                   Objects.equals(other.days,    days) &&
                   Objects.equals(other.hours,   hours) &&
                   Objects.equals(other.minutes, minutes) &&
                   Objects.equals(other.seconds, seconds);
        }
        return false;
    }
}
