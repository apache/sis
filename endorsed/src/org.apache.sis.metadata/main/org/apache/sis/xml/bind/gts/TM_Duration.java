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
package org.apache.sis.xml.bind.gts;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import javax.xml.datatype.Duration;
import javax.xml.datatype.DatatypeConfigurationException;
import jakarta.xml.bind.annotation.XmlElement;
import org.apache.sis.xml.bind.Context;
import org.apache.sis.xml.internal.shared.XmlUtilities;
import org.apache.sis.xml.bind.gco.PropertyType;
import org.apache.sis.util.resources.Errors;


/**
 * Wraps a {@code gts:TM_Duration} element.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 */
public class TM_Duration extends PropertyType<TM_Duration, TemporalAmount> {
    /**
     * Empty constructor for JAXB.
     */
    public TM_Duration() {
    }

    /**
     * Wraps a duration value at marshalling-time.
     *
     * @param  metadata  the metadata value to marshal.
     */
    TM_Duration(final TemporalAmount metadata) {
        super(metadata);
    }

    /**
     * Returns the duration value wrapped by a {@code gts:TM_Duration} element.
     *
     * @param  value  the value to marshal.
     * @return the adapter which wraps the metadata value.
     */
    @Override
    protected TM_Duration wrap(final TemporalAmount value) {
        return new TM_Duration(value);
    }

    /**
     * Returns the interface which is bound by this adapter.
     *
     * @return {@code TemporalAmount.class}
     */
    @Override
    protected final Class<TemporalAmount> getBoundType() {
        return TemporalAmount.class;
    }

    /**
     * Returns the wrapped metadata value.
     */
    final TemporalAmount get() {
        return metadata;
    }

    /**
     * Returns the {@code Duration} generated from the metadata value.
     * This method is systematically called at marshalling time by JAXB.
     *
     * @return the time period, or {@code null}.
     */
    @XmlElement(name = "TM_Duration")
    public final Duration getElement() {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final TemporalAmount metadata = this.metadata;
        if (metadata != null) try {
            BigInteger years   = null;
            BigInteger months  = null;
            BigInteger days    = null;
            BigInteger hours   = null;
            BigInteger minutes = null;
            BigDecimal seconds = null;
            for (TemporalUnit unit : metadata.getUnits()) {
                if (unit instanceof ChronoUnit) {
                    final BigInteger value = BigInteger.valueOf(metadata.get(unit));
                    switch ((ChronoUnit) unit) {
                        case YEARS:   years   = value; continue;
                        case MONTHS:  months  = value; continue;
                        case DAYS:    days    = value; continue;
                        case HOURS:   hours   = value; continue;
                        case MINUTES: minutes = value; continue;
                        case SECONDS: seconds = new BigDecimal(value); continue;
                        case MILLIS:  seconds = addSeconds(seconds, value, 1000); continue;
                        case MICROS:  seconds = addSeconds(seconds, value, 1_000_000); continue;
                        case NANOS:   seconds = addSeconds(seconds, value, 1_000_000_000); continue;
                    }
                }
                Context.warningOccured(Context.current(), TM_Duration.class, "getElement",
                                       Errors.class, Errors.Keys.UnsupportedType_1, unit);
                return null;
            }
            return XmlUtilities.getDatatypeFactory().newDuration(true, years, months, days, hours, minutes, seconds);
        } catch (DatatypeConfigurationException e) {
            Context.warningOccured(Context.current(), TM_Duration.class, "getElement", e, true);
        }
        return null;
    }

    /**
     * Returns <var>current</var> + (<var>numerator</var> / <var>denominator</var>) in seconds.
     * If {@code current} is {@code null}, then it is considered zero.
     *
     * @param  current      the value to add to in number of seconds, or {@code null} if not yet defined.
     * @param  numerator    numerator of the number of seconds to add.
     * @param  denominator  denominator of the number of seconds to add.
     * @return return of the addition, or {@code null} if none.
     */
    private static BigDecimal addSeconds(final BigDecimal current, final BigInteger numerator, final int denominator) {
        if (BigInteger.ZERO.equals(numerator)) {
            return current;
        }
        BigDecimal toAdd = new BigDecimal(numerator).divide(BigDecimal.valueOf(denominator));
        return (current != null) ? current.add(toAdd) : toAdd;
    }

    /**
     * Sets the value from the {@code Duration}.
     * This method is called at unmarshalling time by JAXB.
     *
     * @param  duration  the value to set.
     */
    public void setElement(final Duration duration) {
        if (duration != null) {
            int years  = duration.getYears();       // 0 if not present.
            int months = duration.getMonths();
            int days   = duration.getDays();
            if ((years | months | days) != 0) {
                metadata = Period.of(years, months, days);
            } else {
                int hours   = duration.getHours();
                int minutes = duration.getMinutes();
                int seconds = duration.getSeconds();
                if ((hours | minutes | seconds) != 0) {
                    metadata = java.time.Duration.ofSeconds(((hours * 24L) + minutes)*60 + seconds);
                }
            }
        }
    }

    /**
     * Wraps the value only if marshalling an element from the ISO 19115:2014 metadata model.
     * Otherwise (i.e. if marshalling according legacy ISO 19115:2003 model), omits the element.
     */
    public static final class Since2014 extends TM_Duration {
        /** Empty constructor used only by JAXB. */
        public Since2014() {
        }

        /**
         * Wraps the given value in an ISO 19115-3 element, unless we are marshalling an older document.
         *
         * @return a non-null value only if marshalling ISO 19115-3 or newer.
         */
        @Override protected TM_Duration wrap(final TemporalAmount value) {
            return accept2014() ? super.wrap(value) : null;
        }
    }
}
