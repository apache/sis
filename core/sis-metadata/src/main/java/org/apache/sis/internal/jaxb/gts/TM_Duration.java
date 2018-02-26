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

import javax.xml.bind.annotation.XmlElement;
import org.opengis.temporal.Duration;
import org.opengis.temporal.PeriodDuration;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.gco.PropertyType;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.Classes;


/**
 * Wraps a {@code gts:TM_Duration} element.
 *
 * @todo Current implementation supports only ISO 19108 {@code Duration} that are instance of {@code DurationPeriod}.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Cullen Rombach (Image Matters)
 * @version 1.0
 * @since   1.0
 * @module
 */
public final class TM_Duration extends PropertyType<TM_Duration, Duration> {
    /**
     * Empty constructor for JAXB.
     */
    TM_Duration() {
    }

    /**
     * Wraps a Temporal Duration value at marshalling-time.
     *
     * @param  metadata  the metadata value to marshal.
     */
    private TM_Duration(final Duration metadata) {
        super(metadata);
    }

    /**
     * Returns the Duration value wrapped by a {@code gts:TM_Duration} element.
     *
     * @param  value  the value to marshal.
     * @return the adapter which wraps the metadata value.
     */
    @Override
    protected TM_Duration wrap(final Duration value) {
        return new TM_Duration(value);
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     *
     * @return {@code Duration.class}
     */
    @Override
    protected Class<Duration> getBoundType() {
        return Duration.class;
    }

    /**
     * Returns the {@link Duration} generated from the metadata value.
     * This method is systematically called at marshalling time by JAXB.
     *
     * @return the time period, or {@code null}.
     */
    @XmlElement(name = "TM_Duration")
    public javax.xml.datatype.Duration getElement() {
        if (metadata != null) {
            if (metadata instanceof PeriodDuration) {
                return TM_PeriodDuration.toXML((PeriodDuration) metadata);
            }
            Context.warningOccured(Context.current(), TM_Duration.class, "getElement", Errors.class,
                    Errors.Keys.UnsupportedType_1, Classes.getClass(metadata));
        }
        return null;
    }

    /**
     * Sets the value from the {@link Duration}.
     * This method is called at unmarshalling time by JAXB.
     *
     * @param  duration  the value to set.
     */
    public void setElement(final javax.xml.datatype.Duration duration) {
        metadata = TM_PeriodDuration.toISO(duration);
    }
}
