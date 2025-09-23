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
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.internal.shared.XmlUtilities;


/**
 * JAXB adapter wrapping the date value (as milliseconds elapsed since January 1st, 1970) in a
 * {@link XMLGregorianCalendar} for the {@code xs:date} type. Hours, minutes and seconds are
 * discarded.
 *
 * <p>Using this adapter is equivalent to apply the following annotation on a {@code Date} field:</p>
 *
 * {@snippet lang="java" :
 *     @XmlElement
 *     @XmlSchemaType(name="date")
 *     private Date realizationEpoch;
 * }
 *
 * The main difference is that this adapter will take in account the timezone declared using the
 * {@link org.apache.sis.xml.XML#TIMEZONE} property.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see UniversalTimeAdapter
 * @see org.apache.sis.xml.bind.gco.GO_DateTime
 */
public final class DateAdapter extends XmlAdapter<XMLGregorianCalendar, Date> {
    /**
     * Empty constructor for JAXB only.
     */
    public DateAdapter() {
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
            final Context context = Context.current();
            try {
                final XMLGregorianCalendar gc = XmlUtilities.toXML(context, value);
                XmlUtilities.trimTime(gc, true);        // Type is xs:date without time.
                return gc;
            } catch (DatatypeConfigurationException e) {
                Context.warningOccured(context, XmlAdapter.class, "marshal", e, true);
            }
        }
        return null;
    }
}
