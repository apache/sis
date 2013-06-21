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
package org.apache.sis.internal.jaxb.gco;

import java.util.Date;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.XmlUtilities;


/**
 * JAXB adapter wrapping the date value in a {@code <gco:Date>} or {@code <gco:DateTime>} element,
 * for ISO-19139 compliance. Only one of {@code Date} or {@code DateTime} field shall be non-null.
 * At marshalling time, the choice is performed depending on whatever the given date contains
 * hour, minute or seconds information different than zero.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.3
 * @module
 */
public final class GO_DateTime extends XmlAdapter<GO_DateTime, Date> {
    /**
     * The date and time value using the {@code code "DateTime"} name.
     * Only one of {@code date} and {@link #dateTime} shall be non-null.
     */
    @XmlElement(name = "DateTime")
    private XMLGregorianCalendar dateTime;

    /**
     * The date value using the {@code "Date"} name, used when there is no
     * hour, minutes or seconds to format.
     */
    @XmlElement(name = "Date")
    private XMLGregorianCalendar date;

    /**
     * Empty constructor for JAXB, and also for empty wrapper for formating only an empty element.
     */
    public GO_DateTime() {
    }

    /**
     * Builds a wrapper for the given {@link Date}.
     *
     * @param source The source for reporting errors.
     * @param date The date to marshal. Can not be {@code null}.
     * @param allowTime {@code true} for allowing the usage of {@code "DateTime"} field if
     *        applicable, or {@code false} for using the {@code "Date"} field in every cases.
     */
    GO_DateTime(final XmlAdapter<GO_DateTime,?> source, final Date date, final boolean allowTime) {
        final Context context = Context.current();
        try {
            final XMLGregorianCalendar gc = XmlUtilities.toXML(context, date);
            if (XmlUtilities.trimTime(gc, !allowTime)) {
                this.date = gc;
            } else {
                dateTime = gc;
            }
        } catch (DatatypeConfigurationException e) {
            Context.warningOccured(context, source, XmlAdapter.class, "marshal", e, true);
        }
    }

    /**
     * Returns the current date, or {@code null} if none. IF both fields are defined,
     * then {@link #dateTime} has precedence since it is assumed more accurate.
     */
    final Date getDate() {
        return XmlUtilities.toDate(dateTime != null ? dateTime : date);
    }

    /**
     * Converts a date read from a XML stream to the object which will contains
     * the value. JAXB calls automatically this method at unmarshalling time.
     *
     * @param value The adapter for this metadata value.
     * @return A {@linkplain Date date} which represents the metadata value.
     */
    @Override
    public Date unmarshal(final GO_DateTime value) {
        return (value != null) ? value.getDate() : null;
    }

    /**
     * Converts the {@linkplain Date date} to the object to be marshalled in a XML
     * file or stream. JAXB calls automatically this method at marshalling time.
     * The use of {@code <gco:Date>} or {@code <gco:DateTime>} is determined automatically.
     *
     * @param value The date value.
     * @return The adapter for this date.
     */
    @Override
    public GO_DateTime marshal(final Date value) {
        return (value != null) ? new GO_DateTime(this, value, true) : null;
    }
}
