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

import java.time.temporal.TemporalAmount;
import javax.xml.datatype.Duration;
import jakarta.xml.bind.annotation.XmlElement;
import org.opengis.temporal.PeriodDuration;
import org.apache.sis.temporal.DefaultPeriodDuration;
import org.apache.sis.xml.bind.gco.PropertyType;


/**
 * Wraps a {@code gts:TM_PeriodDuration} element.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
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
     * @param  metadata  the metadata value to marshal.
     */
    private TM_PeriodDuration(final PeriodDuration metadata) {
        super(metadata);
    }

    /**
     * Returns the Period Duration value wrapped by a {@code gts:TM_PeriodDuration} element.
     *
     * @param  value  the value to marshal.
     * @return the adapter which wraps the metadata value.
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
    protected final Class<PeriodDuration> getBoundType() {
        return PeriodDuration.class;
    }

    /**
     * Returns the {@link Duration} generated from the metadata value.
     * This method is systematically called at marshalling time by JAXB.
     *
     * @return the time period, or {@code null}.
     */
    @XmlElement(name = "TM_PeriodDuration")
    public final Duration getElement() {
        if (metadata instanceof TemporalAmount) {
            return new TM_Duration((TemporalAmount) metadata).getElement();
        }
        return null;
    }

    /**
     * Sets the value from the {@link Duration}.
     * This method is called at unmarshalling time by JAXB.
     *
     * @param  duration  the adapter to set.
     */
    public final void setElement(final Duration duration) {
        var p = new TM_Duration();
        p.setElement(duration);
        TemporalAmount t = p.get();
        if (t == null || t instanceof PeriodDuration) {
            metadata = (PeriodDuration) t;
        } else {
            metadata = new DefaultPeriodDuration(t);
        }
    }
}
