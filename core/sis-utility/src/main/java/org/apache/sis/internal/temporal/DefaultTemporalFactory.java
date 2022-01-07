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

import java.util.Date;
import java.util.Collection;
import javax.measure.Unit;
import javax.measure.quantity.Time;
import org.opengis.temporal.*;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.extent.Extent;
import org.opengis.util.InternationalString;
import org.apache.sis.util.resources.Errors;


/**
 * Default implementation of temporal object factory. This is a temporary class;
 * GeoAPI temporal interfaces are expected to change a lot in a future revision.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.2
 * @since   1.2
 * @module
 */
public final class DefaultTemporalFactory implements TemporalFactory {
    /** The unique instance of this factory. */
    public static final TemporalFactory INSTANCE = new DefaultTemporalFactory();

    /** Creates the singleton instance. */
    private DefaultTemporalFactory() {
    }

    /** Creates an {@link Instant} for the given date. */
    @Override public Instant createInstant(Date date) {
        return new DefaultInstant(date);
    }

    /** Creates a period for the two given instants. */
    @Override public Period createPeriod(Instant begin, Instant end) {
        return new DefaultPeriod(begin, end);
    }

    /** Creates a period duration. */
    @Override public PeriodDuration createPeriodDuration(
            InternationalString years, InternationalString months,
            InternationalString week,  InternationalString days,
            InternationalString hours, InternationalString minutes, InternationalString seconds)
    {
        return new DefaultPeriodDuration(years, months, week, days, hours, minutes, seconds);
    }

    /** Returns the exception to be thrown by all unsupported methods. */
    static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException(Errors.format(Errors.Keys.MissingRequiredModule_1, "sis-temporal"));
    }

    /** Unsupported. */
    @Override public Calendar createCalendar(Identifier name, Extent domainOfValidity) {
        throw unsupported();
    }

    /** Unsupported. */
    @Override public Calendar createCalendar(Identifier name, Extent domainOfValidity, Collection<CalendarEra> referenceFrame, Clock timeBasis) {
        throw unsupported();
    }

    /** Unsupported. */
    @Override public CalendarDate createCalendarDate(TemporalReferenceSystem frame, IndeterminateValue indeterminatePosition, InternationalString calendarEraName, int[] calendarDate) {
        throw unsupported();
    }

    /** Unsupported. */
    @Override public CalendarEra createCalendarEra(InternationalString name, InternationalString referenceEvent, CalendarDate referenceDate, JulianDate julianReference, Period epochOfUse) {
        throw unsupported();
    }

    /** Unsupported. */
    @Override public Clock createClock(Identifier name, Extent domainOfValidity, InternationalString referenceEvent, ClockTime referenceTime, ClockTime utcReference) {
        throw unsupported();
    }

    /** Unsupported. */
    @Override public ClockTime createClockTime(TemporalReferenceSystem frame, IndeterminateValue indeterminatePosition, Number[] clockTime) {
        throw unsupported();
    }

    /** Unsupported. */
    @Override public DateAndTime createDateAndTime(TemporalReferenceSystem frame, IndeterminateValue indeterminatePosition, InternationalString calendarEraName, int[] calendarDate, Number[] clockTime) {
        throw unsupported();
    }

    /** Unsupported. */
    @Override public IntervalLength createIntervalLenght(Unit unit, int radix, int factor, int value) {
        throw unsupported();
    }

    /** Unsupported. */
    @Override public JulianDate createJulianDate(TemporalReferenceSystem frame, IndeterminateValue indeterminatePosition, Number coordinateValue) {
        throw unsupported();
    }

    /** Unsupported. */
    @Override public OrdinalEra createOrdinalEra(InternationalString name, Date beginning, Date end, Collection<OrdinalEra> member) {
        throw unsupported();
    }

    /** Unsupported. */
    @Override public OrdinalPosition createOrdinalPosition(TemporalReferenceSystem frame, IndeterminateValue indeterminatePosition, OrdinalEra ordinalPosition) {
        throw unsupported();
    }

    /** Unsupported. */
    @Override public OrdinalReferenceSystem createOrdinalReferenceSystem(Identifier name, Extent domainOfValidity, Collection<OrdinalEra> ordinalEraSequence) {
        throw unsupported();
    }

    /** Unsupported. */
    @Override public TemporalCoordinate createTemporalCoordinate(TemporalReferenceSystem frame, IndeterminateValue indeterminatePosition, Number coordinateValue) {
        throw unsupported();
    }

    /** Unsupported. */
    @Override public TemporalCoordinateSystem createTemporalCoordinateSystem(Identifier name, Extent domainOfValidity, Date origin, Unit<Time> interval) {
        throw unsupported();
    }

    /** Unsupported. */
    @Override public TemporalPosition createTemporalPosition(TemporalReferenceSystem frame, IndeterminateValue indeterminatePosition) {
        throw unsupported();
    }

    /** Unsupported. */
    @Override public TemporalReferenceSystem createTemporalReferenceSystem(Identifier name, Extent domainOfValidity) {
        throw unsupported();
    }
}
