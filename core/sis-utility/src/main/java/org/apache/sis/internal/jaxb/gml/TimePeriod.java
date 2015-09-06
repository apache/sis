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
import org.apache.sis.internal.jaxb.Context;

import static org.apache.sis.internal.jaxb.LegacyNamespaces.VERSION_3_0;

// Branch-dependent imports
import org.apache.sis.internal.geoapi.temporal.Period;


/**
 * The adapter for {@code "TimePeriod"}. This is an attribute of {@link TM_Primitive}.
 *
 * @todo A time period can also be expressed as a begin position and a period or duration.
 *       This is not yet supported in the current implementation.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
@XmlRootElement(name="TimePeriod")
@XmlType(/*name = "TimePeriodType",*/ propOrder = { // TODO: Omitted for now for allowing external modules to define their own type.
    "begin",
    "end"
})
public final class TimePeriod extends GMLAdapter {
    /**
     * The start time, which may be marshalled in a GML3 way or GML2 way.
     * The GML2 way is more verbose.
     */
    @XmlElements({
        @XmlElement(type = TimePeriodBound.GML3.class, name = "beginPosition"),
        @XmlElement(type = TimePeriodBound.GML2.class, name = "begin")
    })
    TimePeriodBound begin;

    /**
     * The end time, which may be marshalled in a GML3 way or GML2 way.
     * The GML2 way is more verbose.
     */
    @XmlElements({
        @XmlElement(type = TimePeriodBound.GML3.class, name = "endPosition"),
        @XmlElement(type = TimePeriodBound.GML2.class, name = "end")
    })
    TimePeriodBound end;

    /**
     * Empty constructor used by JAXB.
     */
    public TimePeriod() {
    }

    /**
     * Creates a new Time Period bounded by the begin and end time specified in the given object.
     *
     * @param period The period to use for initializing this object.
     */
    public TimePeriod(final Period period) {
        super(period);
        if (period != null) {
            if (Context.isGMLVersion(Context.current(), VERSION_3_0)) {
                begin = new TimePeriodBound.GML3(period.getBeginning(), "before");
                end   = new TimePeriodBound.GML3(period.getEnding(), "after");
            } else {
                begin = new TimePeriodBound.GML2(period.getBeginning());
                end   = new TimePeriodBound.GML2(period.getEnding());
            }
        }
    }

    /**
     * Returns a string representation for debugging and formatting error message.
     *
     * @return A string representation of this time period.
     */
    @Override
    public String toString() {
        return "TimePeriod[" + begin + " … " + end + ']';
    }
}
