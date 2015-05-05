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
package org.apache.sis.internal.jaxb.gts;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.xml.datatype.Duration;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.datatype.DatatypeConfigurationException;
import org.opengis.temporal.PeriodDuration;
import org.apache.sis.internal.geoapi.temporal.TemporalFactory;
import org.opengis.util.InternationalString;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.XmlUtilities;
import org.apache.sis.internal.jaxb.gco.PropertyType;
import org.apache.sis.internal.util.TemporalUtilities;
import org.apache.sis.util.iso.SimpleInternationalString;


/**
 * Wraps a {@code gts:TM_PeriodDuration} element.
 *
 * @todo The work done in the {@link #getElement()} and {@link #setElement(Duration)} methods should move
 *       to {@link org.apache.sis.xml.ValueConverter}. However they rely on the {@link org.opengis.temporal}
 *       API in geoapi-pending, which is not very clear... We prefer to hide this for now.
 *
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 */
public final class TM_PeriodDuration extends PropertyType<TM_PeriodDuration, PeriodDuration> {
    /**
     * Empty constructor for JAXB.
     */
    public TM_PeriodDuration() {
    }

    /**
     * Wraps a Temporal Period Duration value at marshalling-time.
     *
     * @param metadata The metadata value to marshal.
     */
    private TM_PeriodDuration(final PeriodDuration metadata) {
        super(metadata);
    }

    /**
     * Returns the Period Duration value wrapped by a {@code gts:TM_PeriodDuration} element.
     *
     * @param value The value to marshal.
     * @return The adapter which wraps the metadata value.
     */
    @Override
    protected TM_PeriodDuration wrap(final PeriodDuration value) {
        return new TM_PeriodDuration(value);
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     *
     * @return {@code PeriodDuration.class}
     */
    @Override
    protected Class<PeriodDuration> getBoundType() {
        return PeriodDuration.class;
    }

    /**
     * Returns the {@link Duration} generated from the metadata value.
     * This method is systematically called at marshalling time by JAXB.
     *
     * @return The time period, or {@code null}.
     */
    @XmlElement(name = "TM_PeriodDuration")
    public Duration getElement() {
        if (metadata instanceof org.apache.sis.internal.geoapi.temporal.PeriodDuration) try {
            /*
             * Get the DatatypeFactory first because if not available, then we don't need to parse
             * the calendar fields. This has the side effect of not validating the calendar fields
             * syntax (which should be integer values), but maybe this is what the user wants.
             */
            final DatatypeFactory factory = XmlUtilities.getDatatypeFactory();
            final org.apache.sis.internal.geoapi.temporal.PeriodDuration metadata =
                    (org.apache.sis.internal.geoapi.temporal.PeriodDuration) this.metadata;
            InternationalString value;
            BigInteger years = null;
            if ((value = metadata.getYears()) != null) {
                years = new BigInteger(value.toString());
            }
            BigInteger months = null;
            if ((value = metadata.getMonths()) != null) {
                months = new BigInteger(value.toString());
            }
            BigInteger days = null;
            if ((value = metadata.getDays()) != null) {
                days = new BigInteger(value.toString());
            }
            BigInteger hours = null;
            if ((value = metadata.getHours()) != null) {
                hours = new BigInteger(value.toString());
            }
            BigInteger minutes = null;
            if ((value = metadata.getMinutes()) != null) {
                minutes = new BigInteger(value.toString());
            }
            BigDecimal seconds = null;
            if ((value = metadata.getSeconds()) != null) {
                seconds = new BigDecimal(value.toString());
            }
            return factory.newDuration(true, years, months, days, hours, minutes, seconds);
        } catch (DatatypeConfigurationException e) {
            warningOccured("getElement", e);
        }
        return null;
    }

    /**
     * Sets the value from the {@link Duration}.
     * This method is called at unmarshalling time by JAXB.
     *
     * @param duration The adapter to set.
     */
    public void setElement(final Duration duration) {
        metadata = null; // Cleaned first in case of failure.
        if (duration != null) try {
            final TemporalFactory factory = TemporalUtilities.getTemporalFactory();
            InternationalString years = null;
            int value;
            if ((value = duration.getYears()) != 0) {
                years = new SimpleInternationalString(Integer.toString(value));
            }
            InternationalString months = null;
            if ((value = duration.getMonths()) != 0) {
                months = new SimpleInternationalString(Integer.toString(value));
            }
            InternationalString weeks = null; // no weeks in javax.xml.datatype.Duration
            InternationalString days = null;
            if ((value = duration.getDays()) != 0) {
                days = new SimpleInternationalString(Integer.toString(value));
            }
            InternationalString hours = null;
            if ((value = duration.getHours()) != 0) {
                hours = new SimpleInternationalString(Integer.toString(value));
            }
            InternationalString minutes = null;
            if ((value = duration.getMinutes()) != 0) {
                minutes = new SimpleInternationalString(Integer.toString(value));
            }
            InternationalString seconds = null;
            if ((value = duration.getSeconds()) != 0) {
                seconds = new SimpleInternationalString(Integer.toString(value));
            }
            metadata = factory.createPeriodDuration(years, months, weeks, days, hours, minutes, seconds);
        } catch (UnsupportedOperationException e) {
            warningOccured("setElement", e);
        }
    }

    /**
     * Reports a failure to execute the operation because of missing {@code sis-temporal} module.
     *
     * @param methodName The method name.
     * @param e The exception.
     */
    private static void warningOccured(final String methodName, final Exception e) {
        if (TemporalUtilities.REPORT_MISSING_MODULE || !e.getMessage().contains("sis-temporal")) {
            Context.warningOccured(Context.current(), TM_PeriodDuration.class, methodName, e, true);
        }
    }
}
