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

import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.datatype.XMLGregorianCalendar;

// Branch-dependent imports
import org.apache.sis.internal.geoapi.temporal.Instant;


/**
 * The {@linkplain TimePeriod#begin begin} or {@linkplain TimePeriod#end end} position in
 * a {@link TimePeriod}. This information is encoded in different way depending if we are
 * reading or formatting a GML2 or GML3 file.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlTransient
public abstract class TimePeriodBound {
    /**
     * Empty constructor for subclasses only.
     */
    TimePeriodBound() {
    }

    /**
     * Returns the XML calendar, or {@code null} if none. This information is encoded
     * in different fields depending if we are reading/writing a GML2 or a GML3 file.
     */
    abstract XMLGregorianCalendar calendar();

    /**
     * Returns a string representation of this bound for debugging purpose.
     *
     * @return A string representation of the time currently set.
     */
    @Override
    public String toString() {
        return String.valueOf(calendar());
    }

    /**
     * The begin or end position in a {@link TimePeriod}, expressed in the GML 3 way.
     * Example:
     *
     * {@preformat xml
     *   <gml:TimePeriod>
     *     <gml:beginPosition>1992-01-01T01:00:00.000+01:00</gml:beginPosition>
     *     <gml:endPosition>2007-12-31T01:00:00.000+01:00</gml:endPosition>
     *   </gml:TimePeriod>
     * }
     */
    public static final class GML3 extends TimePeriodBound {
        /**
         * A textual indication of the time, usually {@code "before"}, {@code "after"} or {@code "now"}.
         * This attribute and the {@linkplain #value} are mutually exclusive.
         */
        @XmlAttribute
        public String indeterminatePosition;

        /**
         * The actual time position, or {@code null} for {@linkplain #indeterminatePosition indeterminate position}.
         *
         * <p><strong>WARNING: The timezone information may be lost!</strong> This is because this field
         * is derived from a {@link java.util.Date}, in which case we don't know if the time is really 0
         * or just unspecified. This class assumes that a time of zero means "unspecified". This will be
         * revised after we implemented ISO 19108.</p>
         */
        @XmlValue
        public XMLGregorianCalendar value;

        /**
         * Empty constructor used by JAXB.
         */
        public GML3() {
        }

        /**
         * Creates a bound initialized to the given instant.
         *
         * @param instant The instant of the new bound, or {@code null}.
         * @param indeterminate The value to give to {@link #indeterminatePosition} if the date is null.
         */
        GML3(final Instant instant, final String indeterminate) {
            value = TimeInstant.toXML(instant);
            if (value == null) {
                indeterminatePosition = indeterminate;
            }
        }

        /**
         * Returns the XML calendar, or {@code null} if none or undetermined.
         */
        @Override
        XMLGregorianCalendar calendar() {
            return value;
        }
    }

    /**
     * The begin or end position in a {@link TimePeriod}, expressed in the GML 2 way.
     * This object encapsulates a {@link TimeInstant} inside a {@code begin} or {@code end}
     * element inside a GML 2 {@link TimePeriod} in GML 2. This is not used for GML 3.
     * Example:
     *
     * {@preformat xml
     *   <gml:TimePeriod>
     *     <gml:begin>
     *       <gml:TimeInstant gml:id="begin">
     *         <gml:timePosition>1992-01-01T01:00:00.000+01:00</gml:timePosition>
     *       </gml:TimeInstant>
     *     </gml:begin>
     *     <gml:end>
     *       <gml:TimeInstant gml:id="end">
     *         <gml:timePosition>2007-12-31T01:00:00.000+01:00</gml:timePosition>
     *       </gml:TimeInstant>
     *     </gml:end>
     *   </gml:TimePeriod>
     * }
     */
    //@XmlType(name = "TimeInstantPropertyType") // TODO: Omitted for now for allowing external modules to define their own type.
    public static final class GML2 extends TimePeriodBound {
        /**
         * The time.
         */
        @XmlElement(name = "TimeInstant")
        public TimeInstant timeInstant;

        /**
         * Empty constructor used by JAXB.
         */
        public GML2() {
        }

        /**
         * Creates a bound initialized to the given instant.
         *
         * @param instant The instant of the new bound, or {@code null}.
         */
        GML2(final Instant instant) {
            timeInstant = new TimeInstant(instant);
        }

        /**
         * Returns the XML calendar, or {@code null} if none.
         */
        @Override
        XMLGregorianCalendar calendar() {
            final TimeInstant timeInstant = this.timeInstant;
            return (timeInstant != null) ? timeInstant.timePosition : null;
        }
    }
}
