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
import java.time.temporal.ChronoField;
import jakarta.xml.bind.annotation.XmlElement;
import org.opengis.temporal.TemporalPrimitive;
import org.apache.sis.xml.internal.shared.XmlUtilities;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.bind.gco.PropertyType;
import org.apache.sis.temporal.TemporalObjects;
import org.apache.sis.util.resources.Errors;

// Specific to the main branch:
import org.apache.sis.pending.geoapi.temporal.Period;


/**
 * JAXB adapter for {@link TemporalPrimitive}, in order to integrate the value in an element complying
 * with OGC/ISO standard. Note that the CRS is formatted using the GML schema, not the ISO 19139:2007 one.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 */
public class TM_Primitive extends PropertyType<TM_Primitive, TemporalPrimitive> {
    /**
     * Empty constructor for JAXB.
     */
    public TM_Primitive() {
    }

    /**
     * Wraps a Temporal Primitive value at marshalling-time.
     *
     * @param metadata  the metadata value to marshal.
     */
    private TM_Primitive(final TemporalPrimitive metadata) {
        super(metadata);
    }

    /**
     * Returns the Vertical CRS value wrapped by a temporal primitive element.
     *
     * @param  value  the value to marshal.
     * @return the adapter which wraps the metadata value.
     */
    @Override
    protected TM_Primitive wrap(final TemporalPrimitive value) {
        return new TM_Primitive(value);
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     *
     * @return {@code TemporalPrimitive.class}
     */
    @Override
    protected final Class<TemporalPrimitive> getBoundType() {
        return TemporalPrimitive.class;
    }

    /**
     * Returns the {@code TimePeriod} generated from the metadata value.
     * This method is systematically called at marshalling-time by JAXB.
     *
     * @return the time period, or {@code null}.
     */
    @XmlElement(name = "TimePeriod")
    public final TimePeriod getTimePeriod() {
        if (metadata instanceof Period) {
            return new TimePeriod((Period) metadata);
        }
        return null;
    }

    /**
     * Returns the {@link TimeInstant} generated from the metadata value.
     * This method is systematically called at marshalling-time by JAXB.
     *
     * @return the time instant, or {@code null}.
     */
    @XmlElement(name = "TimeInstant")
    public final TimeInstant getTimeInstant() {
        Temporal time = TemporalObjects.getInstant(metadata);
        return (time != null) ? new TimeInstant(time) : null;
    }

    /**
     * Sets the value from the {@link TimePeriod}.
     * This method is called at unmarshalling-time by JAXB.
     *
     * @param  period  the wrapper to set.
     */
    public final void setTimePeriod(final TimePeriod period) {
        metadata = null;                                        // Cleaned first in case of failure.
        if (period != null) {
            final Context context = Context.current();
            final Temporal begin = toInstant(context, period.begin);
            final Temporal end   = toInstant(context, period.end);
            if (begin != null && end != null
                    && end.isSupported(ChronoField.INSTANT_SECONDS)
                    && begin.isSupported(ChronoField.INSTANT_SECONDS)
                    && begin.getLong(ChronoField.INSTANT_SECONDS) > end.getLong(ChronoField.INSTANT_SECONDS))
            {
                /*
                 * We log with `TemporalPrimitive` as the source class,
                 * because it is the closest we can get to a public API.
                 */
                Context.warningOccured(context, TemporalPrimitive.class,
                        "setTimePeriod", Errors.class, Errors.Keys.IllegalRange_2, begin, end);
            } else {
                metadata = TemporalObjects.createPeriod(begin, end);
                period.copyIdTo(metadata);
            }
        }
    }

    /**
     * Sets the value from the {@link TimeInstant}.
     * This method is called at unmarshalling-time by JAXB.
     *
     * @param  instant  the wrapper to set.
     */
    public final void setTimeInstant(final TimeInstant instant) {
        metadata = null;                                        // Cleaned first in case of failure.
        if (instant != null) {
            final Temporal position = XmlUtilities.toTemporal(Context.current(), instant.timePosition);
            if (position != null) {
                metadata = TemporalObjects.createInstant(position);
                instant.copyIdTo(metadata);
            }
        }
    }

    /**
     * Returns the instant of the given bounds, or {@code null} if none.
     */
    private static Temporal toInstant(final Context context, final TimePeriodBound bound) {
        return (bound != null) ? XmlUtilities.toTemporal(context, bound.calendar()) : null;
    }

    /**
     * Wraps the value only if marshalling ISO 19115-3 element.
     * Otherwise (i.e. if marshalling a legacy ISO 19139:2007 document), omit the element.
     */
    public static final class Since2014 extends TM_Primitive {
        /** Empty constructor used only by JAXB. */
        public Since2014() {
        }

        /**
         * Wraps the given value in an ISO 19115-3 element, unless we are marshalling an older document.
         *
         * @return a non-null value only if marshalling ISO 19115-3 or newer.
         */
        @Override protected TM_Primitive wrap(final TemporalPrimitive value) {
            return accept2014() ? super.wrap(value) : null;
        }
    }
}
