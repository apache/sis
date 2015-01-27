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
package org.apache.sis.internal.jaxb;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.GregorianCalendar;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.DatatypeConfigurationException;
import org.apache.sis.internal.system.SystemListener;
import org.apache.sis.internal.system.Modules;

import static javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED;


/**
 * Utilities methods related to XML.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final class XmlUtilities extends SystemListener {
    /**
     * The factory for creating {@link javax.xml.datatype} objects.
     */
    private static volatile DatatypeFactory factory;

    /**
     * Resets the {@link #factory} to {@code null} if the classpath changed.
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
     * Invoked when the classpath changed. This method resets the {@link #factory} to {@code null}
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
     * @return The factory (never {@code null}).
     * @throws DatatypeConfigurationException If the factory can not be created.
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
     *           at XML marshalling time. Then returns {@code true}.</li>
     *       <li>Otherwise returns {@code false}. But before doing so, still set the milliseconds
     *           to {@code FIELD_UNDEFINED} if its value was 0.</li>
     *     </ul></li>
     *   <li>Otherwise (if the {@code force} argument is {@code false}), then the temporal
     *       part is set to {@code FIELD_UNDEFINED} unconditionally and this method returns
     *       {@code true}.</li>
     * </ul>
     *
     * <strong>WARNING: The timezone information may be lost!</strong> This method is used mostly
     * when the Gregorian Calendar were created from a {@link Date}, in which case we don't know
     * if the time is really 0 or just unspecified. This method should be invoked only when we
     * want to assume that a time of zero means "unspecified".
     *
     * <p>This method will be deprecated after we implemented ISO 19108 in SIS.</p>
     *
     * @param  gc The date to modify in-place.
     * @param  force {@code true} for forcing the temporal components to be removed without any check.
     * @return {@code true} if the time part has been completely removed, {@code false} otherwise.
     */
    public static boolean trimTime(final XMLGregorianCalendar gc, final boolean force) {
        if (force || gc.getMillisecond() == 0) {
            gc.setMillisecond(FIELD_UNDEFINED);
            if (force || (gc.getHour() == 0 && gc.getMinute() == 0 && gc.getSecond() == 0)) {
                gc.setHour(FIELD_UNDEFINED);
                gc.setMinute(FIELD_UNDEFINED);
                gc.setSecond(FIELD_UNDEFINED);
                gc.setTimezone(FIELD_UNDEFINED);
                return true;
            }
        }
        return false;
    }

    /**
     * Converts the given date to a XML Gregorian calendar using the locale and timezone
     * from the current {@linkplain Context marshalling context}.
     *
     * @param  context The current (un)marshalling context, or {@code null} if none.
     * @param  date The date to convert to a XML calendar, or {@code null}.
     * @return The XML calendar, or {@code null} if {@code date} was null.
     * @throws DatatypeConfigurationException If the factory can not be created.
     */
    public static XMLGregorianCalendar toXML(final Context context, final Date date) throws DatatypeConfigurationException {
        if (date != null) {
            final GregorianCalendar calendar = createGregorianCalendar(context);
            calendar.setTime(date);
            return getDatatypeFactory().newXMLGregorianCalendar(calendar);
        }
        return null;
    }

    /**
     * Creates a new Gregorian calendar for the current timezone and locale. If no locale or
     * timezone were explicitely set, then the default ones are used as documented in the
     * {@link org.apache.sis.xml.XML#TIMEZONE} constant.
     *
     * @return A Gregorian calendar initialized with the current timezone and locale.
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
     * Converts the given XML Gregorian calendar to a date.
     *
     * @param  context The current (un)marshalling context, or {@code null} if none.
     * @param  xml The XML calendar to convert to a date, or {@code null}.
     * @return The date, or {@code null} if {@code xml} was null.
     */
    public static Date toDate(final Context context, final XMLGregorianCalendar xml) {
        if (xml != null) {
            final GregorianCalendar calendar =  xml.toGregorianCalendar();
            if (context != null && xml.getTimezone() == FIELD_UNDEFINED) {
                final TimeZone timezone = context.getTimeZone();
                if (timezone != null) {
                    calendar.setTimeZone(timezone);
                }
            }
            return calendar.getTime();
        }
        return null;
    }
}
