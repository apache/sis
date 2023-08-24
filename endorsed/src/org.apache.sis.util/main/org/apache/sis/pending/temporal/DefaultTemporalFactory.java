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
package org.apache.sis.pending.temporal;

import java.util.Date;
import org.opengis.util.InternationalString;
import org.apache.sis.pending.geoapi.temporal.*;


/**
 * Default implementation of temporal object factory. This is a temporary class;
 * GeoAPI temporal interfaces are expected to change a lot in a future revision.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 * @since   1.2
 */
public final class DefaultTemporalFactory implements TemporalFactory {
    /** The unique instance of this factory. */
    private static final TemporalFactory INSTANCE = new DefaultTemporalFactory();

    /** {@return the unique instance of this factory}. */
    public static TemporalFactory provider() {
        return INSTANCE;
    }

    /** Creates the singleton instance. */
    private DefaultTemporalFactory() {
    }

    /** Creates an {@link Instant} for the given date. */
    @Override public Instant createInstant(Date date) {
        return new DefaultInstant(date);
    }

    /** Creates a period for the two given instants. */
    @Override public Period createPeriod(Instant begin, Instant end) {
        return new DefaultPeriod(begin, end);
    }

    /** Creates a period duration. */
    @Override public PeriodDuration createPeriodDuration(
            InternationalString years, InternationalString months,
            InternationalString week,  InternationalString days,
            InternationalString hours, InternationalString minutes, InternationalString seconds)
    {
        return new DefaultPeriodDuration(years, months, week, days, hours, minutes, seconds);
    }
}
