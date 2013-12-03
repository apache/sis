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

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.datatype.XMLGregorianCalendar;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.LegacyNamespaces;

import static org.apache.sis.internal.jaxb.gml.GMLAdapter.GML_3_0;


/**
 * A copy of {@link TimePeriod} using GML 3.1 namespace instead than GML 3.2.
 * This class will be deleted in a future SIS version if we find a better way
 * to support evolution of GML schemas.
 *
 * @author  Guilhem Legal (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
@XmlRootElement(name="TimePeriod", namespace = LegacyNamespaces.GML)
@XmlType(propOrder = {
    "begin",
    "end"
})
public final class TimePeriod31 extends GMLAdapter {
    /**
     * Same as {@link TimePeriod#begin}, but using GML 3.1 namespace.
     */
    @XmlElements({
        @XmlElement(type=TimePeriodBound.GML3.class, name="beginPosition", namespace = LegacyNamespaces.GML),
        @XmlElement(type=Bound.class, name="begin", namespace = LegacyNamespaces.GML)
    })
    TimePeriodBound begin;

    /**
     * Same as {@link TimePeriod#end}, but using GML 3.1 namespace.
     */
    @XmlElements({
        @XmlElement(type=TimePeriodBound.GML3.class, name="endPosition", namespace = LegacyNamespaces.GML),
        @XmlElement(type=Bound.class, name="end", namespace = LegacyNamespaces.GML)
    })
    TimePeriodBound end;

    /**
     * Same as {@link TimePeriod#TimePeriod()} but for GML 3.1 namespace.
     */
    public TimePeriod31() {
    }

    /**
     * Same as {@link TimePeriod#TimePeriod(Period)} but for GML 3.1 namespace.
     *
     * @param period The period to use for initializing this object.
     */
    public TimePeriod31(final Period period) {
        super(period);
        if (period != null) {
            if (Context.isGMLVersion(Context.current(), GML_3_0)) {
                begin = new TimePeriodBound.GML3(period.getBeginning(), "before");
                end   = new TimePeriodBound.GML3(period.getEnding(), "after");
            } else {
                begin = new Bound(period.getBeginning());
                end   = new Bound(period.getEnding());
            }
        }
    }

    /**
     * A copy of {@link TimePeriodBound.GML2} using GML 3.1 namespace instead than GML 3.2.
     * This class will be deleted in a future SIS version if we find a better way to support
     * evolution of GML schemas.
     *
     * @author  Guilhem Legal (Geomatys)
     * @since   0.4
     * @version 0.4
     * @module
     */
    private static final class Bound extends TimePeriodBound {
        /**
         * Same as {@link TimePeriodBound.GML2#timeInstant}, but using GML 3.1 namespace.
         */
        @XmlElement(name = "TimeInstant", namespace = LegacyNamespaces.GML)
        public TimeInstant timeInstant;

        /**
         * Same as {@link TimePeriodBound.GML2#GML2()} but for GML 3.1 namespace.
         */
        public Bound() {
        }

        /**
         * Same as {@link TimePeriodBound.GML2#GML2(Instant)} but for GML 3.1 namespace.
         *
         * @param instant The instant of the new bound, or {@code null}.
         */
        Bound(final Instant instant) {
            timeInstant = new TimeInstant(instant);
        }

        /**
         * Same as {@link TimePeriodBound.GML2#calendar()} but for GML 3.1 namespace.
         */
        @Override
        XMLGregorianCalendar calendar() {
            final TimeInstant timeInstant = this.timeInstant;
            return (timeInstant != null) ? timeInstant.timePosition : null;
        }
    }
}
