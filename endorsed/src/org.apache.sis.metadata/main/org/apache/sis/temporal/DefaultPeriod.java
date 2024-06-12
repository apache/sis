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

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;


/**
 * Default implementation of GeoAPI period.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class DefaultPeriod implements Period, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3870895998810224339L;

    /**
     * Bounds making this period.
     */
    @SuppressWarnings("serial")         // Default implementations are serializable.
    private final Instant beginning, ending;

    /**
     * Creates a new period with the given bounds.
     */
    DefaultPeriod(final Instant beginning, final Instant ending) {
        this.beginning = beginning;
        this.ending    = ending;
    }

    /**
     * Returns the beginning instant at which this period starts.
     */
    @Override
    public Instant getBeginning() {
        return beginning;
    }

    /**
     * Returns the ending instant at which this period ends.
     */
    @Override
    public Instant getEnding() {
        return ending;
    }

    /**
     * Returns the duration of this period.
     */
    @Override
    public TemporalAmount length() {
        return GeneralDuration.distance(beginning, ending, false, false);
    }

    /**
     * Returns a string representation in ISO 8601 format.
     * The format is {@code <start>/<end>}.
     */
    @Override
    public String toString() {
        return beginning + "/" + ending;
    }

    /**
     * Hash code value of the time position.
     */
    @Override
    public int hashCode() {
        return beginning.hashCode() + 37 * ending.hashCode();
    }

    /**
     * Compares with given object for equality.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof DefaultPeriod) {
            final var other = (DefaultPeriod) obj;
            return beginning.equals(other.beginning) && ending.equals(other.ending);
        }
        return false;
    }
}
