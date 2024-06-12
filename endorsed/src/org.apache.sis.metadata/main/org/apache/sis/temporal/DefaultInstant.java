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

import java.util.Objects;
import java.util.Optional;
import java.io.Serializable;
import java.time.Duration;
import java.time.DateTimeException;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.resources.Errors;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.temporal.Period;
import org.opengis.temporal.Instant;
import org.opengis.temporal.TemporalPrimitive;
import org.opengis.temporal.IndeterminateValue;
import org.opengis.filter.TemporalOperatorName;


/**
 * Default implementation of an instant as defined by ISO 19108.
 * This is not the same as {@link java.time.Instant}, because the
 * instant can actually be a date, or may be indeterminate.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class DefaultInstant implements Instant, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3898772638524283287L;

    /**
     * The temporal position as a date, time or date/time.
     * May be {@code null} if {@link #indeterminate} is non-null and not "before" or "after".
     */
    @SuppressWarnings("serial")         // Standard implementations are serializable.
    private final Temporal position;

    /**
     * The indeterminate value, or {@code null} if none.
     */
    private final IndeterminateValue indeterminate;

    /**
     * Creates a new instant.
     *
     * @param  position       the temporal position, or {@code null} if unknown or now.
     * @param  indeterminate  the indeterminate value, or {@code null} if none.
     */
    DefaultInstant(final Temporal position, final IndeterminateValue indeterminate) {
        this.position = position;
        this.indeterminate = indeterminate;
    }

    /**
     * Returns the date, time or position on the time-scale represented by this primitive.
     * Should not be null, unless the value is {@linkplain IndeterminateValue#UNKNOWN unknown}.
     *
     * @return the date, time or position on the time-scale represented by this primitive.
     */
    @Override
    public Temporal getPosition() {
        if (indeterminate != IndeterminateValue.NOW) {
            return position;
        }
        return java.time.Instant.now();
    }

    /**
     * Returns the reason why the temporal position is missing or inaccurate.
     *
     * @return the reason why the temporal position is missing or inaccurate.
     */
    @Override
    public Optional<IndeterminateValue> getIndeterminatePosition() {
        return Optional.ofNullable(indeterminate);
    }

    /**
     * Returns the distance from this instant to another instant or period.
     *
     * @param  other the other object from which to measure the distance.
     * @return the distance from this instant to another instant or period.
     * @throws DateTimeException if the duration cannot be computed.
     * @throws ArithmeticException if the calculation exceeds the integer capacity.
     */
    @Override
    public TemporalAmount distance(final TemporalPrimitive other) {
        ArgumentChecks.ensureNonNull("other", other);
        if (other instanceof Instant) {
            return GeneralDuration.distance(this, (Instant) other, false, true);
        } else if (other instanceof Period) {
            final var p = (Period) other;
            TemporalAmount t = GeneralDuration.distance(this, p.getBeginning(), false, false);
            if (t == null) {
                t = GeneralDuration.distance(this, p.getEnding(), true, false);
                if (t == null) {
                    return Duration.ZERO;
                }
            }
            return t;
        } else {
            throw new DateTimeException(Errors.format(Errors.Keys.UnsupportedType_1, other.getClass()));
        }
    }

    /**
     * Determines the position of this primitive relative to another temporal primitive.
     * The relative position is identified by an operator which evaluates to {@code true}
     * when the two operands are {@code this} and {@code other}.
     *
     * @param  other the other primitive for which to determine the relative position.
     * @return a temporal operator which is true when evaluated between this primitive and the other primitive.
     * @throws DateTimeException if the temporal objects cannot be compared.
     */
    @Override
    public TemporalOperatorName findRelativePosition(final TemporalPrimitive other) {
        throw new UnsupportedOperationException();
    }

    /**
     * Compares this instant with the given object for equality.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object instanceof DefaultInstant) {
            final var that = (DefaultInstant) object;
            return Objects.equals(position, that.position) && indeterminate == that.indeterminate;
        }
        return false;
    }

    /**
     * Computes a hash code value for this instant.
     */
    @Override
    public int hashCode() {
        return Objects.hash(position, indeterminate);
    }

    /**
     * Returns a string representation of this instant.
     * This is either the date, the indeterminate position (e.g., "now"),
     * or a combination of both (e.g., "after 2000-01-01").
     */
    @Override
    public String toString() {
        final var s = new StringBuilder();
        if (indeterminate != null) {
            s.append(indeterminate.identifier());
            if (position != null) {
                s.append(' ').append(position);
            }
        } else {
            s.append(position);     // Should never be null.
        }
        return s.toString();
    }
}
