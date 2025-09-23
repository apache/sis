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

import java.time.temporal.Temporal;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.xml.internal.shared.XmlUtilities;


/**
 * Encapsulates a {@code gml:TimeInstant}. This element may be used alone, or included in a
 * {@link TimePeriodBound.GML2} object. The latter is itself included in {@link TimePeriod}.
 * Note that GML3 does not anymore include {@code TimeInstant} inside {@code TimePeriod}.
 *
 * @author  Guilhem Legal (Geomatys)
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
     * @param instant  the initial instant value.
     */
    public TimeInstant(final Temporal instant) {
        timePosition = toXML(instant);
    }

    /**
     * Creates a XML Gregorian Calendar from the given instant, if non-null.
     * Otherwise returns {@code null}.
     */
    static XMLGregorianCalendar toXML(final Temporal instant) {
        if (instant != null) {
            final Context context = Context.current();
            try {
                return XmlUtilities.toXML(context, instant);
            } catch (DatatypeConfigurationException | IllegalArgumentException e) {
                Context.warningOccured(context, TimeInstant.class, "toXML", e, true);
            }
        }
        return null;
    }

    /**
     * Returns a string representation for debugging and formatting error message.
     *
     * @return a string representation of this time instant.
     */
    @Override
    public String toString() {
        return Strings.bracket(getClass(), timePosition);
    }
}
