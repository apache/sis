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
package org.apache.sis.internal.jaxb.gco;

import java.util.Date;
import javax.xml.bind.annotation.adapters.XmlAdapter;


/**
 * JAXB adapter wrapping the date value (as milliseconds elapsed since January 1st, 1970) in a
 * {@code <gco:Date>} element (<strong>not</strong> {@code <gco:DateTime>}), for ISO-19139 compliance.
 * The {@link Long#MIN_VALUE} is used as a sentinel value meaning "no date".
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
public final class DateAsLongAdapter extends XmlAdapter<GO_DateTime, Long> {
    /**
     * Empty constructor for JAXB only.
     */
    public DateAsLongAdapter() {
    }

    /**
     * Converts a date read from a XML stream to the object which will contains
     * the value. JAXB calls automatically this method at unmarshalling time.
     *
     * @param value The adapter for this metadata value.
     * @return A {@linkplain Long long} which represents the metadata value.
     */
    @Override
    public Long unmarshal(final GO_DateTime value) {
        if (value != null) {
            final Date date = value.getDate();
            if (date != null) {
                final long time = date.getTime();
                if (time != Long.MIN_VALUE) {
                    return time;
                }
            }
        }
        return Long.MIN_VALUE;
    }

    /**
     * Converts the {@linkplain Long long} to the object to be marshalled in a XML
     * file or stream. JAXB calls automatically this method at marshalling time.
     *
     * @param value The date value as a long.
     * @return The adapter for the date.
     */
    @Override
    public GO_DateTime marshal(final Long value) {
        if (value != null) {
            final long time = value;
            if (time != Long.MIN_VALUE) {
                return new GO_DateTime(new Date(time), false);
            }
        }
        return null;
    }
}
