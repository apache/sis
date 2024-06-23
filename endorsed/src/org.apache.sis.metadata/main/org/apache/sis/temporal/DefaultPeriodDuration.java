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
package org.apache.sis.temporal;

import java.io.Serializable;
import java.time.temporal.TemporalAmount;
import org.opengis.temporal.PeriodDuration;

// Specific to the geoapi-3.1 branch:
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.List;


/**
 * Default implementation of GeoAPI period duration.
 *
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @deprecated This is a temporary class for compatibility with GeoAPI 3.x only.
 * It should disappear with GeoAPI 4.0.
 */
@Deprecated
@SuppressWarnings("serial")
public final class DefaultPeriodDuration implements PeriodDuration, Serializable {
    /**
     * The temporal object providing the duration value.
     */
    public final TemporalAmount duration;

    /**
     * Creates a new duration.
     */
    public DefaultPeriodDuration(final TemporalAmount duration) {
        this.duration = duration;
    }

    @Override public List<TemporalUnit>   getUnits()          {return duration.getUnits();}
    @Override public long     get         (TemporalUnit unit) {return duration.get(unit);}
    @Override public Temporal addTo       (Temporal temporal) {return duration.addTo(temporal);}
    @Override public Temporal subtractFrom(Temporal temporal) {return duration.subtractFrom(temporal);}

    /** String representation. */
    @Override public String toString() {
        return duration.toString();
    }

    /** Hash code value of the time position. */
    @Override public int hashCode() {
        return duration.hashCode() ^ 879337943;
    }

    /** Compares with given object for equality. */
    @Override public boolean equals(final Object obj) {
        if (obj instanceof DefaultPeriodDuration) {
            duration.equals(((DefaultPeriodDuration) obj).duration);
        }
        return false;
    }
}
