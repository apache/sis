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
package org.apache.sis.xml.bind.gml;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.internal.shared.XmlUtilities;


/**
 * JAXB adapter wrapping the date value (as milliseconds elapsed since January 1st, 1970) in a
 * {@link XMLGregorianCalendar} for the {@code xs:dateTime} type with the timezone forced to UTC.
 * The milliseconds are omitted if not different than zero.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see DateAdapter
 * @see org.apache.sis.xml.bind.gco.GO_DateTime
 */
public final class UniversalTimeAdapter extends XmlAdapter<XMLGregorianCalendar, Date> {
    /**
     * The timezone of the date to marshal with this adapter.
     */
    private static final TimeZone UTC = TimeZone.getTimeZone(org.apache.sis.util.internal.shared.Constants.UTC);

    /**
     * Empty constructor for JAXB only.
     */
    public UniversalTimeAdapter() {
    }

    /**
     * Converts a date read from a XML stream to the object which will contain
     * the value. JAXB calls automatically this method at unmarshalling time.
     *
     * @param  value  the XML date, or {@code null}.
     * @return the {@code java.util} date, or {@code null}.
     */
    @Override
    public Date unmarshal(final XMLGregorianCalendar value) {
        return (value != null) ? XmlUtilities.toDate(Context.current(), value) : null;
    }

    /**
     * Converts the date to the object to be marshalled in a XML file or stream.
     * JAXB calls automatically this method at marshalling time.
     *
     * @param  value  the {@code java.util} date value, or {@code null}.
     * @return the XML date, or {@code null}.
     */
    @Override
    public XMLGregorianCalendar marshal(final Date value) {
        if (value != null) {
            final GregorianCalendar calendar = new GregorianCalendar(UTC, Locale.ROOT);
            calendar.setTime(value);
            try {
                final XMLGregorianCalendar gc = XmlUtilities.getDatatypeFactory().newXMLGregorianCalendar(calendar);
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
