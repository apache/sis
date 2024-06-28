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
import java.time.Duration;
import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import org.apache.sis.util.Classes;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.temporal.Period;
import org.opengis.temporal.Instant;
import org.opengis.temporal.TemporalPrimitive;
import org.opengis.temporal.IndeterminateValue;
import org.opengis.filter.TemporalOperatorName;
import org.opengis.temporal.IndeterminatePositionException;


/**
 * Default implementation of an instant as defined by ISO 19108.
 * This is not the same as {@link java.time.Instant}, because the
 * instant can actually be a date, or may be indeterminate.
 *
 * <h2>Thread-safety</h2>
 * Instances of this class are mostly immutable, except for the list of identifiers.
 * All instances are thread-safe.
 *
 * @author  Martin Desruisseaux (Geomatys)
 */
final class DefaultInstant extends TemporalObject implements Instant {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 3898772638524283287L;

    /**
     * The constant for the "unknown" instant.
     */
    static final DefaultInstant UNKNOWN = new DefaultInstant(null, IndeterminateValue.UNKNOWN);

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
     * Returns the given instant as an instance of this implementation class.
     *
     * @param  other  the other instance to cast or copy, or {@code null}.
     * @return the given instant as an {@code DefaultInstant}, or {@code null} if the given value was null.
     */
    public static DefaultInstant castOrCopy(final Instant other) {
        if (other == null || other instanceof DefaultInstant) {
            return (DefaultInstant) other;
        } else {
            final var indeterminate = other.getIndeterminatePosition().orElse(null);
            if (indeterminate == IndeterminateValue.UNKNOWN) {
                return UNKNOWN;
            }
            final Temporal position = other.getPosition();
            if (indeterminate == null || indeterminate == IndeterminateValue.BEFORE || indeterminate == IndeterminateValue.AFTER) {
                Objects.requireNonNull(position);
            }
            return new DefaultInstant(position, indeterminate);
        }
    }

    /**
     * Returns the date, time or position on the time-scale represented by this primitive.
     * Should not be null, unless the value is {@linkplain IndeterminateValue#UNKNOWN unknown}.
     *
     * @return the date, time or position on the time-scale represented by this primitive.
     */
    @Override
    public final Temporal getPosition() {
        if (indeterminate != IndeterminateValue.NOW) {
            return position;
        }
        return ZonedDateTime.now();
    }

    /**
     * Returns the reason why the temporal position is missing or inaccurate.
     *
     * @return the reason why the temporal position is missing or inaccurate.
     */
    @Override
    public final Optional<IndeterminateValue> getIndeterminatePosition() {
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
        }
        if (other instanceof Period) {
            final var p = (Period) other;
            TemporalAmount t = GeneralDuration.distance(this, p.getBeginning(), false, false);
            if (t == null) {
                t = GeneralDuration.distance(this, p.getEnding(), true, false);
                if (t == null) {
                    return Duration.ZERO;
                }
            }
            return t;
        }
        throw new DateTimeException(Errors.format(Errors.Keys.UnsupportedType_1, other.getClass()));
    }

    /**
     * Determines the position of this instant relative to another temporal primitive.
     * The relative position is identified by an operator which evaluates to {@code true}
     * when the two operands are {@code this} and {@code other}.
     *
     * @param  other the other primitive for which to determine the relative position.
     * @return a temporal operator which is true when evaluated between this instant and the other primitive.
     * @throws DateTimeException if the temporal objects cannot be compared.
     */
    @Override
    public TemporalOperatorName findRelativePosition(final TemporalPrimitive other) {
        ArgumentChecks.ensureNonNull("other", other);
        if (other instanceof Instant) {
            return relativeToInstant((Instant) other);
        }
        if (other instanceof Period) {
            return relativeToPeriod((Period) other);
        }
        throw new DateTimeException(Errors.format(Errors.Keys.UnsupportedType_1, other.getClass()));
    }

    /**
     * Determines the position of this instant relative to a period.
     *
     * @param  other the period for which to determine the relative position.
     * @return a temporal operator which is true when evaluated between this primitive and the period.
     * @throws DateTimeException if the temporal objects cannot be compared.
     */
    final TemporalOperatorName relativeToPeriod(final Period other) {
        TemporalOperatorName relation = relativeToInstant(other.getBeginning());
        String erroneous;
        if (relation == TemporalOperatorName.BEFORE) return relation;
        if (relation == TemporalOperatorName.EQUALS) return TemporalOperatorName.BEGINS;
        if (relation == TemporalOperatorName.AFTER) {
            relation = relativeToInstant(other.getEnding());
            if (relation == TemporalOperatorName.AFTER)  return relation;
            if (relation == TemporalOperatorName.EQUALS) return TemporalOperatorName.ENDS;
            if (relation == TemporalOperatorName.BEFORE) return TemporalOperatorName.DURING;
            erroneous = "ending";
        } else {
            erroneous = "beginning";
        }
        // Should never happen.
        throw new DateTimeException(Errors.format(Errors.Keys.IllegalMapping_2, erroneous, relation));
    }

    /**
     * Determines the position of this instant relative to another instant.
     *
     * @param  other the other instant for which to determine the relative position.
     * @return a temporal operator which is true when evaluated between this primitive and the other primitive.
     * @throws DateTimeException if the temporal objects cannot be compared.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})    // See end of method.
    private TemporalOperatorName relativeToInstant(final Instant other) {
        boolean canTestBefore = true;
        boolean canTestAfter  = true;
        boolean canTestEqual  = true;
        if (indeterminate != null && indeterminate != IndeterminateValue.NOW) {
            canTestBefore = (indeterminate == IndeterminateValue.BEFORE);
            canTestAfter  = (indeterminate == IndeterminateValue.AFTER);
            canTestEqual  = false;
        }
        final IndeterminateValue oip = other.getIndeterminatePosition().orElse(null);
        if (oip != null) {
            if (oip != IndeterminateValue.NOW) {
                canTestBefore &= (oip == IndeterminateValue.AFTER);
                canTestAfter  &= (oip == IndeterminateValue.BEFORE);
                canTestEqual   = false;
            } else if (indeterminate == IndeterminateValue.NOW) {
                return TemporalOperatorName.EQUALS;
            }
        }
cmp:    if (canTestBefore | canTestAfter | canTestEqual) {
            final Temporal t1;                  // Same as `this.position` except if "now".
            final Temporal t2;                  // Position of the other instant.
            final TimeMethods<?> comparators;   // The "is before", "is after" and "is equal" methods to invoke.
            /*
             * First, resolve the case when the indeterminate value is "now". Do not invoke `getPosition()`
             * because the results could differ by a few nanoseconds when two "now" instants are compared,
             * and also for getting a temporal object of the same type than the other instant.
             */
            if (oip == IndeterminateValue.NOW) {
                t1 = position;
                if (t1 == null) break cmp;
                comparators = TimeMethods.find(t1.getClass());
                t2 = comparators.now();
            } else {
                t2 = other.getPosition();
                if (t2 == null) break cmp;
                if (indeterminate == IndeterminateValue.NOW) {
                    comparators = TimeMethods.find(t2.getClass());
                    t1 = comparators.now();
                } else {
                    t1 = position;
                    if (t1 == null) break cmp;
                    comparators = TimeMethods.find(Classes.findCommonClass(t1.getClass(), t2.getClass()));
                }
            }
            // This is where the @SuppressWarnings(…) apply.
            if (canTestBefore && ((TimeMethods) comparators).isBefore.test(t1, t2)) return TemporalOperatorName.BEFORE;
            if (canTestAfter  && ((TimeMethods) comparators).isAfter .test(t1, t2)) return TemporalOperatorName.AFTER;
            if (canTestEqual  && ((TimeMethods) comparators).isEqual .test(t1, t2)) return TemporalOperatorName.EQUALS;
        }
        throw new IndeterminatePositionException(Errors.format(Errors.Keys.IndeterminatePosition));
    }

    /**
     * Compares this instant with the given object, optionally ignoring timezone.
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
        if (object instanceof Instant) {
            final var that = (Instant) object;
            if (indeterminate == that.getIndeterminatePosition().orElse(null)) {
                if (indeterminate == IndeterminateValue.NOW || indeterminate == IndeterminateValue.UNKNOWN) {
                    return true;
                }
                final Temporal other = that.getPosition();
                return Objects.equals(position, other) ||       // Needed in all cases for testing null values.
                        (mode.isIgnoringMetadata() && TimeMethods.compareAny(TimeMethods.EQUAL, position, other));
            }
        }
        return false;
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
            return indeterminate == that.indeterminate
                    && Objects.equals(position, that.position)
                    && equalIdentifiers(that);
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
            String id = indeterminate.identifier();
            if (id == null) id = indeterminate.name();
            s.append(id);
            if (position != null) {
                s.append(' ').append(position);
            }
        } else {
            s.append(position);     // Should never be null.
        }
        return s.toString();
    }
}
