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
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.XmlUtilities;

// Branch-dependent imports
import org.apache.sis.internal.geoapi.temporal.Instant;
import org.apache.sis.internal.geoapi.temporal.Position;


/**
 * Encapsulates a {@code gml:TimeInstant}. This element may be used alone, or included in a
 * {@link TimePeriodBound.GML2} object. The later is itself included in {@link TimePeriod}.
 * Note that GML3 does not anymore include {@code TimeInstant} inside {@code TimePeriod}.
 *
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlType(name = "TimeInstantType")
@XmlRootElement(name="TimeInstant")
public final class TimeInstant extends GMLAdapter {
    /**
     * The date, optionally with its time component. The time component is omitted
     * if the hours, minutes, seconds and milliseconds fields are all set to 0.
     *
     * <p><strong>WARNING: The timezone information may be lost!</strong> This is because this field
     * is derived from a {@link java.util.Date}, in which case we don't know if the time is really 0
     * or just unspecified. This class assumes that a time of zero means "unspecified". This will be
     * revised after we implemented ISO 19108.</p>
     */
    @XmlElement
    public XMLGregorianCalendar timePosition;

    /**
     * Empty constructor used by JAXB.
     */
    public TimeInstant() {
    }

    /**
     * Creates a new time instant initialized to the given value.
     *
     * @param instant The initial instant value.
     */
    public TimeInstant(final Instant instant) {
        timePosition = toXML(instant);
    }

    /**
     * Creates a XML Gregorian Calendar from the given instants, if non-null.
     * Otherwise returns {@code null}.
     *
     * <p><strong>WARNING: The timezone information may be lost!</strong> This is because this field
     * is derived from a {@link java.util.Date}, in which case we don't know if the time is really 0
     * or just unspecified. This class assumes that a time of zero means "unspecified". This will be
     * revised after we implemented ISO 19108.</p>
     */
    static XMLGregorianCalendar toXML(final Instant instant) {
        if (instant != null) {
            final Date date = instant.getDate();
            if (date != null) {
                final Context context = Context.current();
                try {
                    final XMLGregorianCalendar gc = XmlUtilities.toXML(context, date);
                    if (gc != null) {
                        XmlUtilities.trimTime(gc, false);
                        return gc;
                    }
                } catch (DatatypeConfigurationException e) {
                    Context.warningOccured(context, TimeInstant.class, "toXML", e, true);
                }
            }
        }
        return null;
    }

    /**
     * Returns a string representation for debugging and formatting error message.
     *
     * @return A string representation of this time instant.
     */
    @Override
    public String toString() {
        return "TimeInstant[" + timePosition + ']';
    }
}
