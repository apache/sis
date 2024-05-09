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

import java.io.Serializable;
import java.time.temporal.TemporalAmount;
import org.opengis.temporal.PeriodDuration;


/**
 * Default implementation of GeoAPI period duration. This is a temporary class;
 * GeoAPI temporal interfaces are expected to change a lot in a future revision.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
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
