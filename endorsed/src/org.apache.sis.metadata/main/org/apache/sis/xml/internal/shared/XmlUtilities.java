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
package org.apache.sis.xml.internal.shared;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.YearMonth;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.GregorianCalendar;
import java.util.function.ObjIntConsumer;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.DatatypeConfigurationException;
import static javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED;
import org.apache.sis.system.SystemListener;
import org.apache.sis.system.Modules;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.temporal.TemporalDate;


/**
 * Utilities methods related to XML.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
public final class XmlUtilities extends SystemListener {
    /**
     * The factory for creating {@link javax.xml.datatype} objects.
     */
    private static volatile DatatypeFactory factory;

    /**
     * Resets the {@link #factory} to {@code null} if the module path changed.
     */
    static {
        SystemListener.add(new XmlUtilities());
    }

    /**
     * For internal usage only.
     */
    private XmlUtilities() {
        super(Modules.UTILITIES);
    }

    /**
     * Invoked when the module path changed. This method resets the {@link #factory} to {@code null}
     * in order to force the search for a new instance.
     */
    @Override
    protected void classpathChanged() {
        synchronized (XmlUtilities.class) {
            factory = null;
        }
    }

    /**
     * Returns the factory for creating {@link javax.xml.datatype} objects.
     *
     * @return the factory (never {@code null}).
     * @throws DatatypeConfigurationException if the factory cannot be created.
     */
    public static DatatypeFactory getDatatypeFactory() throws DatatypeConfigurationException {
        DatatypeFactory f = factory;
        if (f == null) {
            synchronized (XmlUtilities.class) {
                f = factory;
                if (f == null) {
                    factory = f = DatatypeFactory.newInstance();
                }
            }
        }
        return f;
    }

    /**
     * Trims the time components of the given calendar if their values are zero, or leaves
     * them unchanged otherwise (except for milliseconds). More specifically:
     *
     * <ul>
     *   <li>If the {@code force} argument is {@code false}, then:
     *     <ul>
     *       <li>If every time components (hour, minute, seconds and milliseconds) are zero, set
     *           them to {@code FIELD_UNDEFINED} in order to prevent them from being formatted
     *           at XML marshalling time.</li>
     *       <li>Otherwise set the milliseconds to {@code FIELD_UNDEFINED} if its value was 0.</li>
     *     </ul></li>
     *   <li>Otherwise (if the {@code force} argument is {@code false}), then the temporal
     *       part is set to {@code FIELD_UNDEFINED} unconditionally.</li>
     * </ul>
     *
     * <strong>WARNING: The timezone information may be lost!</strong> This method is used mostly
     * when the Gregorian Calendar were created from a {@link Date}, in which case we don't know
     * if the time is really 0 or just unspecified. This method should be invoked only when we
     * want to assume that a time of zero means "unspecified".
     *
     * <p>This method will be deprecated after we implemented ISO 19108 in SIS.</p>
     *
     * @param  gc     the date to modify in-place.
     * @param  force  {@code true} for forcing the temporal components to be removed without any check.
     */
    public static void trimTime(final XMLGregorianCalendar gc, final boolean force) {
        if (force || gc.getMillisecond() == 0) {
            gc.setMillisecond(FIELD_UNDEFINED);
            if (force || (gc.getHour() == 0 && gc.getMinute() == 0 && gc.getSecond() == 0)) {
                gc.setHour(FIELD_UNDEFINED);
                gc.setMinute(FIELD_UNDEFINED);
                gc.setSecond(FIELD_UNDEFINED);
                gc.setTimezone(FIELD_UNDEFINED);
            }
        }
    }

    /**
     * Temporal fields that may be copied into {@link XMLGregorianCalendar}.
     */
    private static final ChronoField[] FIELDS = {
        /*[0]*/ ChronoField.YEAR,
        /*[1]*/ ChronoField.MONTH_OF_YEAR,
        /*[2]*/ ChronoField.DAY_OF_MONTH,
        /*[3]*/ ChronoField.HOUR_OF_DAY,
        /*[4]*/ ChronoField.MINUTE_OF_HOUR,
        /*[5]*/ ChronoField.SECOND_OF_MINUTE,
        /*[6]*/ ChronoField.MILLI_OF_SECOND,
        /*[7]*/ ChronoField.OFFSET_SECONDS
    };

    /**
     * Setter methods to invoke for setting the value of a temporal field on a {@link XMLGregorianCalendar}.
     * Indices in this array must correspond to indices in the {@link #FIELDS} array.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})        // Generic array creation.
    private static final ObjIntConsumer<XMLGregorianCalendar>[] SETTERS = new ObjIntConsumer[FIELDS.length];
    static {
        SETTERS[0] = XMLGregorianCalendar::setYear;
        SETTERS[1] = XMLGregorianCalendar::setMonth;
        SETTERS[2] = XMLGregorianCalendar::setDay;
        SETTERS[3] = XMLGregorianCalendar::setHour;
        SETTERS[4] = XMLGregorianCalendar::setMinute;
        SETTERS[5] = XMLGregorianCalendar::setSecond;
        SETTERS[6] = XMLGregorianCalendar::setMillisecond;
        SETTERS[7] = (calendar, seconds) -> calendar.setTimezone(seconds / 60);     // Convert seconds to minutes.
    }

    /**
     * Converts the given temporal object to a XML Gregorian calendar.
     * The returned calendar may have undefined fields (including undefined time zone)
     * if the corresponding information was not provided in the given temporal object.
     *
     * @param  context  the current (un)marshalling context, or {@code null} if none.
     * @param  date     the date to convert to a XML calendar.
     * @return the XML calendar.
     * @throws DatatypeConfigurationException if the factory cannot be created.
     */
    public static XMLGregorianCalendar toXML(final Context context, Temporal date) throws DatatypeConfigurationException {
        if (date instanceof Instant) {
            final TimeZone zone = (context != null) ? context.getTimeZone() : null;
            final ZoneId zid = (zone != null) ? zone.toZoneId() : ZoneId.systemDefault();
            date = ZonedDateTime.ofInstant((Instant) date, zid);
        }
        final XMLGregorianCalendar xml = getDatatypeFactory().newXMLGregorianCalendar();
        for (int i=0; i<FIELDS.length; i++) {
            final ChronoField field = FIELDS[i];
            if (date.isSupported(field)) {
                final int n = date.get(field);
                if (n != 0 || field != ChronoField.MILLI_OF_SECOND) {
                    SETTERS[i].accept(xml, date.get(field));
                }
            }
        }
        return xml;
    }

    /**
     * Converts the given date to a XML Gregorian calendar using the locale and timezone
     * from the current {@linkplain Context marshalling context}.
     * The returned date has millisecond accuracy.
     * Caller may want to clear the millisecond field if it is equal to zero.
     *
     * @param  context  the current (un)marshalling context, or {@code null} if none.
     * @param  date     the date to convert to a XML calendar.
     * @return the XML calendar, or {@code null} if {@code date} was null.
     * @throws DatatypeConfigurationException if the factory cannot be created.
     */
    public static XMLGregorianCalendar toXML(final Context context, final Date date) throws DatatypeConfigurationException {
        if (date instanceof TemporalDate) {
            return toXML(context, ((TemporalDate) date).temporal);
        }
        final GregorianCalendar calendar = createGregorianCalendar(context);
        calendar.setTime(date);
        return getDatatypeFactory().newXMLGregorianCalendar(calendar);
    }

    /**
     * Creates a new Gregorian calendar for the current timezone and locale. If no locale or
     * timezone were explicitly set, then the default ones are used as documented in the
     * {@link org.apache.sis.xml.XML#TIMEZONE} constant.
     *
     * @return a Gregorian calendar initialized with the current timezone and locale.
     */
    private static GregorianCalendar createGregorianCalendar(final Context context) {
        if (context != null) {
            final Locale locale = context.getLocale();
            final TimeZone timezone = context.getTimeZone();
            /*
             * Use the appropriate contructor rather than setting ourself the null values to
             * the default locale or timezone, because the JDK constructors perform a better
             * job of sharing existing timezone instances.
             */
            if (timezone != null) {
                return (locale != null) ? new GregorianCalendar(timezone, locale)
                                        : new GregorianCalendar(timezone);
            } else if (locale != null) {
                return new GregorianCalendar(locale);
            }
        }
        return new GregorianCalendar();
    }

    /**
     * Replaces undefined value by zero. Used for optional time fields.
     */
    private static int zeroIfUndef(final int value) {
        return (value != FIELD_UNDEFINED) ? value : 0;
    }

    /**
     * Converts the given XML Gregorian calendar to a temporal object.
     * The temporal object may be {@link LocalDate}, {@link LocalTime},
     * {@link LocalDateTime}, {@link OffsetDateTime}, {@link Year} or {@link YearMonth}
     * depending on which fields are defined in the given calendar.
     *
     * @param  context  the current (un)marshalling context, or {@code null} if none.
     * @param  xml      the XML calendar to convert to a temporal object, or {@code null}.
     * @return the temporal object, or {@code null} if {@code xml} is null or has too many undefined fields.
     */
    public static Temporal toTemporal(final Context context, final XMLGregorianCalendar xml) {
        if (xml == null) {
            return null;
        }
        final int year  = xml.getYear();
        final int month = xml.getMonth();
        final int day   = xml.getDay();
        final int hour  = xml.getHour();
        final int min   = zeroIfUndef(xml.getMinute());
        final int sec   = zeroIfUndef(xml.getSecond());
        final int nano;
        final boolean hasYear =            (year  != FIELD_UNDEFINED);
        final boolean hasYM   = hasYear && (month != FIELD_UNDEFINED);
        final boolean hasDate = hasYM   && (day   != FIELD_UNDEFINED);
        if (hour == FIELD_UNDEFINED) {
            return hasDate ? LocalDate.of(year, month, day) :
                   hasYM   ? YearMonth.of(year, month) :
                   hasYear ? Year     .of(year) : null;
        } else {
            final BigDecimal f = xml.getFractionalSecond();
            nano = (f != null) ? f.movePointRight(9).intValue() : 0;
        }
        final int offset = xml.getTimezone();
        if (offset == FIELD_UNDEFINED) {
            if (hasDate) {
                return LocalDateTime.of(year, month, day, hour, min, sec, nano);
            } else {
                return LocalTime.of(hour, min, sec, nano);
            }
        }
        final ZoneOffset zone = ZoneOffset.ofTotalSeconds(offset * 60);
        if (hasDate) {
            return OffsetDateTime.of(year, month, day, hour, min, sec, nano, zone);
        } else {
            return OffsetTime.of(hour, min, sec, nano, zone);
        }
    }

    /**
     * Converts the given XML Gregorian calendar to a date.
     *
     * @param  context  the current (un)marshalling context, or {@code null} if none.
     * @param  xml      the XML calendar to convert to a date, or {@code null}.
     * @return the date, or {@code null} if {@code xml} was null.
     */
    public static Date toDate(final Context context, final XMLGregorianCalendar xml) {
        if (xml != null) {
            final GregorianCalendar calendar = xml.toGregorianCalendar();
            if (context != null && xml.getTimezone() == FIELD_UNDEFINED) {
                final TimeZone timezone = context.getTimeZone();
                if (timezone != null) {
                    calendar.setTimeZone(timezone);
                }
            }
            return new TemporalDate(calendar.getTimeInMillis(), toTemporal(context, xml));
        }
        return null;
    }

    /**
     * Closes the reader, input stream or XML stream used by the given source.
     *
     * @param  source  the source to close.
     * @throws Exception if an error occurred while closing the source.
     */
    public static void close(final Source source) throws Exception {
        if (source instanceof StreamSource) {
            var s = (StreamSource) source;
            close(s.getInputStream(), s.getReader());
        } else if (source instanceof SAXSource) {
            var s = ((SAXSource) source).getInputSource();
            if (s != null) {
                close(s.getByteStream(), s.getCharacterStream());
            }
        } else if (source instanceof StAXSource) {
            var s = (StAXSource) source;
            var c = s.getXMLEventReader();  if (c != null) c.close();
            var b = s.getXMLStreamReader(); if (b != null) b.close();
            /*
             * Note: above calls to `close()` do not close the underlying streams,
             * so we are not really done yet. But we cannot do better in this method.
             */
        }
    }

    /**
     * Closes the given byte stream and character stream if non-null.
     * This is a helper method for {@link #close(Source)} only.
     */
    private static void close(final Closeable b, final Closeable c) throws IOException {
        if (c != null) c.close();
        if (b != null) b.close();
    }
}
