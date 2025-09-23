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

import java.util.Map;
import java.time.DateTimeException;
import java.time.temporal.TemporalAmount;
import org.opengis.temporal.TemporalPrimitive;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.internal.shared.LazyCandidate;

// Specific to the main branch:
import org.apache.sis.pending.geoapi.temporal.Instant;
import org.apache.sis.pending.geoapi.temporal.Period;
import org.apache.sis.pending.geoapi.temporal.TemporalOperatorName;


/**
 * Default implementation of GeoAPI period.
 *
 * <h2>Thread-safety</h2>
 * Instances of this class are mostly immutable, except for the list of identifiers.
 * All instances are thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class DefaultPeriod extends TemporalObject implements Period {
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
     * Determines the position of this period relative to another temporal primitive.
     * The relative position is identified by an operator which evaluates to {@code true}
     * when the two operands are {@code this} and {@code other}.
     *
     * @param  other the other primitive for which to determine the relative position.
     * @return a temporal operator which is true when evaluated between this period and the other primitive.
     * @throws DateTimeException if the temporal objects cannot be compared.
     */
    @Override
    public TemporalOperatorName findRelativePosition(final TemporalPrimitive other) {
        ArgumentChecks.ensureNonNull("other", other);
        if (other instanceof Instant) {
            return DefaultInstant.castOrCopy((Instant) other).relativeToPeriod(this).reversed().orElseThrow();
        }
        if (other instanceof Period) {
            final var period = (Period) other;
            String erroneous;
            TemporalOperatorName relation = DefaultInstant.castOrCopy(beginning).relativeToPeriod(period);
            final var map = POSITIONS.get(relation);
            if (map != null) {
                relation = DefaultInstant.castOrCopy(ending).relativeToPeriod(period);
                final var result = map.get(relation);
                if (result != null) {
                    return result;
                }
                erroneous = "ending";
            } else {
                erroneous = "beginning";
            }
            // Should never happen.
            throw new DateTimeException(Errors.format(Errors.Keys.IllegalMapping_2, erroneous, relation));
        }
        throw new DateTimeException(Errors.format(Errors.Keys.UnsupportedType_1, other.getClass()));
    }

    /**
     * Relative positions for given pairs (beginning, ending) relative positions.
     * Keys of this static map are the relative positions of the beginning of this period relative to the other period.
     * Keys of the enclosed maps are the relative positions of the ending of this period relative to the other period.
     */
    @LazyCandidate
    private static final Map<TemporalOperatorName, Map<TemporalOperatorName, TemporalOperatorName>> POSITIONS = Map.of(
            TemporalOperatorName.BEFORE, Map.of(
                    TemporalOperatorName.BEFORE, TemporalOperatorName.BEFORE,
                    TemporalOperatorName.BEGINS, TemporalOperatorName.MEETS,
                    TemporalOperatorName.DURING, TemporalOperatorName.OVERLAPS,
                    TemporalOperatorName.ENDS,   TemporalOperatorName.ENDED_BY,
                    TemporalOperatorName.AFTER,  TemporalOperatorName.CONTAINS),
            TemporalOperatorName.BEGINS, Map.of(
                    TemporalOperatorName.BEGINS, TemporalOperatorName.MEETS,
                    TemporalOperatorName.DURING, TemporalOperatorName.BEGINS,
                    TemporalOperatorName.ENDS,   TemporalOperatorName.EQUALS,
                    TemporalOperatorName.AFTER,  TemporalOperatorName.BEGUN_BY),
            TemporalOperatorName.DURING, Map.of(
                    TemporalOperatorName.DURING, TemporalOperatorName.DURING,
                    TemporalOperatorName.ENDS,   TemporalOperatorName.ENDS,
                    TemporalOperatorName.AFTER,  TemporalOperatorName.OVERLAPPED_BY),
            TemporalOperatorName.ENDS, Map.of(
                    TemporalOperatorName.ENDS,   TemporalOperatorName.MET_BY,
                    TemporalOperatorName.AFTER,  TemporalOperatorName.MET_BY),
            TemporalOperatorName.AFTER, Map.of(
                    TemporalOperatorName.AFTER,  TemporalOperatorName.AFTER));

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
            return beginning.equals(other.beginning)
                   && ending.equals(other.ending)
                   && equalIdentifiers(other);
        }
        return false;
    }

    /**
     * Compares this period with the given object, optionally ignoring timezone.
     * If the comparison mode ignores metadata, this method compares only the position on the timeline.
     *
     * @param  other  the object to compare to {@code this}.
     * @param  mode   the strictness level of the comparison.
     * @return {@code true} if both objects are equal according the given comparison mode.
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (mode.equals(ComparisonMode.STRICT)) {   // Use `mode.equals(…)` for opportunistic null check.
            return equals(object);
        }
        if (object instanceof Period) {
            final var other = (Period) object;
            return Utilities.deepEquals(beginning, other.getBeginning(), mode) &&
                   Utilities.deepEquals(ending,    other.getEnding(),    mode);
        }
        return false;
    }
}
