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
package org.apache.sis.internal.jaxb.gml;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.XmlUtilities;

import static org.apache.sis.internal.jaxb.XmlUtilities.getDatatypeFactory;


/**
 * JAXB adapter wrapping the date value (as milliseconds elapsed since January 1st, 1970) in a
 * {@link XMLGregorianCalendar} for the {@code xsd:dateTime} type with the timezone forced to UTC.
 * The milliseconds are omitted if not different than zero.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 *
 * @see DateAdapter
 */
public final class UniversalTimeAdapter extends XmlAdapter<XMLGregorianCalendar, Date> {
    /**
     * The timezone of the date to marshal with this adapter.
     */
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /**
     * Empty constructor for JAXB only.
     */
    public UniversalTimeAdapter() {
    }

    /**
     * Converts a date read from a XML stream to the object which will contains
     * the value. JAXB calls automatically this method at unmarshalling time.
     *
     * @param  value The XML date, or {@code null}.
     * @return The {@code java.util} date, or {@code null}.
     */
    @Override
    public Date unmarshal(final XMLGregorianCalendar value) {
        return (value != null) ? XmlUtilities.toDate(Context.current(), value) : null;
    }

    /**
     * Converts the date to the object to be marshalled in a XML file or stream.
     * JAXB calls automatically this method at marshalling time.
     *
     * @param  value The {@code java.util} date value, or {@code null}.
     * @return The XML date, or {@code null}.
     */
    @Override
    public XMLGregorianCalendar marshal(final Date value) {
        if (value != null) {
            final GregorianCalendar calendar = new GregorianCalendar(UTC, Locale.ROOT);
            calendar.setTime(value);
            try {
                final XMLGregorianCalendar gc = getDatatypeFactory().newXMLGregorianCalendar(calendar);
                if (gc.getMillisecond() == 0) {
                    gc.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
                }
                return gc;
            } catch (DatatypeConfigurationException e) {
                Context.warningOccured(Context.current(), XmlAdapter.class, "marshal", e, true);
            }
        }
        return null;
    }
}
