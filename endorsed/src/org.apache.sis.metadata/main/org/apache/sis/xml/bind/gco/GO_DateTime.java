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
package org.apache.sis.xml.bind.gco;

import java.util.Date;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConstants;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.bind.FilterByVersion;
import org.apache.sis.xml.internal.shared.XmlUtilities;


/**
 * JAXB adapter wrapping the date value in a {@code <gco:Date>} or {@code <gco:DateTime>} element,
 * for ISO 19115-3 compliance. Only one of {@code Date} or {@code DateTime} field shall be non-null.
 * At marshalling time, the choice is performed depending on whatever the given date contains
 * hour, minute or seconds information different than zero.
 *
 * <h2>Difference between ISO 19139:2007 and ISO 19115-3:2016</h2>
 * The ISO {@code baseTypes.xsd} files define two kinds of date property:
 * <ul>
 *   <li>{@code gco:Date_PropertyType} accepts either {@code gco:Date} or {@code gco:DateTime}.</li>
 *   <li>{@code gco:DateTime_PropertyType} accepts only {@code gco:DateTime}.</li>
 * </ul>
 *
 * In the legacy standard (ISO 19139:2007), date properties (in particular in citations) were of type
 * {@code Date_PropertyType}. But in the new standard (ISO 19115-3:2016), most date properties are of
 * type {@code DateTime_PropertyType}, i.e. {@code gco:Date} is not legal anymore. The only exception
 * is {@code versionDate} in {@code cat:AbstractCT_Catalogue}.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @see org.apache.sis.xml.bind.gml.DateAdapter
 * @see org.apache.sis.xml.bind.gml.UniversalTimeAdapter
 *
 *
 * @todo This adapter may be deleted in favor of {@link GO_Temporal} after all usages
 *       of {@link Date} have been replaced by {@link java.time.temporal.Temporal}.
 */
@XmlType(name = "DateTime_PropertyType")
public class GO_DateTime extends XmlAdapter<GO_DateTime, Date> {
    /**
     * The date and time value using the {@code code "DateTime"} name.
     * Only one of {@code date} and {@link #dateTime} shall be non-null.
     */
    @XmlElement(name = "DateTime")
    @XmlSchemaType(name = "dateTime")
    public XMLGregorianCalendar dateTime;

    /**
     * The date value using the {@code "Date"} name,
     * used when there are no hours, minutes or seconds to format.
     */
    @XmlElement(name = "Date")
    @XmlSchemaType(name = "date")
    public XMLGregorianCalendar date;

    /**
     * Empty constructor for JAXB.
     */
    public GO_DateTime() {
    }

    /**
     * Builds a wrapper for the given {@link Date}.
     *
     * @param date  the date to marshal. Cannot be {@code null}.
     */
    private GO_DateTime(final Date date) {
        final Context context = Context.current();
        try {
            final XMLGregorianCalendar gc = XmlUtilities.toXML(context, date);
            if (Context.isFlagSet(context, Context.LEGACY_METADATA)) {
                XmlUtilities.trimTime(gc, false);
                if (gc.getHour() == DatatypeConstants.FIELD_UNDEFINED) {
                    this.date = gc;
                } else {
                    dateTime = gc;
                }
            } else {
                if (gc.getMillisecond() == 0) {
                    gc.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
                }
                dateTime = gc;
            }
        } catch (DatatypeConfigurationException e) {
            Context.warningOccured(context, XmlAdapter.class, "marshal", e, true);
        }
    }

    /**
     * Returns the current date, or {@code null} if none. If both fields are defined,
     * then {@link #dateTime} has precedence since it is assumed more accurate.
     */
    private Date getDate() {
        return XmlUtilities.toDate(Context.current(), dateTime != null ? dateTime : date);
    }

    /**
     * Converts a date read from a XML stream to the object which will contain
     * the value. JAXB calls automatically this method at unmarshalling time.
     *
     * @param  value  the adapter for this metadata value.
     * @return a {@linkplain Date date} which represents the metadata value.
     */
    @Override
    public final Date unmarshal(final GO_DateTime value) {
        return (value != null) ? value.getDate() : null;
    }

    /**
     * Converts the {@linkplain Date date} to the object to be marshalled in a XML
     * file or stream. JAXB calls automatically this method at marshalling time.
     * The use of {@code <gco:Date>} or {@code <gco:DateTime>} is determined automatically.
     *
     * @param  value  the date value.
     * @return the adapter for this date.
     */
    @Override
    public GO_DateTime marshal(final Date value) {
        return (value != null) ? new GO_DateTime(value) : null;
    }

    /**
     * Wraps the value only if marshalling ISO 19115-3 element.
     * Otherwise (i.e. if marshalling a legacy ISO 19139:2007 document), omit the element.
     */
    public static final class Since2014 extends GO_DateTime {
        /** Empty constructor used only by JAXB. */
        public Since2014() {
        }

        /**
         * Wraps the given value in an ISO 19115-3 element, unless we are marshalling an older document.
         *
         * @return a non-null value only if marshalling ISO 19115-3 or newer.
         */
        @Override public GO_DateTime marshal(final Date value) {
            return FilterByVersion.CURRENT_METADATA.accept() ? super.marshal(value) : null;
        }
    }
}
