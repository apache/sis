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
package org.apache.sis.filter;

import java.util.Optional;
import java.io.Serializable;
import java.time.DateTimeException;
import java.time.temporal.Temporal;
import org.apache.sis.util.internal.shared.Strings;
import org.apache.sis.util.collection.WeakHashSet;
import org.apache.sis.temporal.TimeMethods;
import static org.apache.sis.temporal.TimeMethods.BEFORE;
import static org.apache.sis.temporal.TimeMethods.AFTER;
import static org.apache.sis.temporal.TimeMethods.EQUAL;

// Specific to the geoapi-3.1 and geoapi-4.0 branches:
import org.opengis.temporal.Period;
import org.opengis.temporal.Instant;
import org.opengis.temporal.IndeterminateValue;
import org.opengis.filter.TemporalOperatorName;


/**
 * Temporal operations between periods and/or instants.
 * The nature of the operation depends on the subclass.
 * Subclasses shall override at least one of the following methods:
 *
 * <ul>
 *   <li>{@link #evaluate(T, T)}</li>
 *   <li>{@link #evaluate(T, Period)}</li>
 *   <li>{@link #evaluate(Period, T)}</li>
 *   <li>{@link #evaluate(Period, Period)}</li>
 * </ul>
 *
 * Instances of this classes are immutable and thread-safe.
 * The same instances are shared by many filters.
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 *
 * @param  <T>  the base type of temporal objects, or {@code Object.class} for any type.
 */
abstract class TemporalOperation<T> implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -5304814915639460679L;

    /**
     * Temporal operations created in the current JVM.
     */
    @SuppressWarnings("unchecked")
    private static final WeakHashSet<TemporalOperation<?>> POOL = new WeakHashSet<>((Class) TemporalOperation.class);

    /**
     * The set of methods to invoke for performing "is before", "is after" or "is equal" comparisons.
     */
    protected final TimeMethods<T> comparators;

    /**
     * Creates a new temporal operation.
     *
     * @param  comparators  the set of methods to invoke for performing comparisons.
     */
    protected TemporalOperation(final TimeMethods<T> comparators) {
        this.comparators = comparators;
    }

    /**
     * Returns a unique, shared instance of this operation.
     *
     * @return a unique instance equal to {@code this}.
     */
    public final TemporalOperation<T> unique() {
        return POOL.unique(this);
    }

    /**
     * Returns a hash code value for this operation.
     * Used for {@link #unique()} implementation.
     */
    @Override
    public final int hashCode() {
        return getClass().hashCode() + 31 * comparators.type.hashCode();
    }

    /**
     * Compares the given object with this operation for equality.
     * Used for {@link #unique()} implementation.
     */
    @Override
    public final boolean equals(final Object other) {
        return (other != null) && (other.getClass() == getClass()) &&
                ((TemporalOperation) other).comparators.type == comparators.type;
    }

    /**
     * Returns a string representation for debugging purposes.
     */
    @Override
    public final String toString() {
        return Strings.toString(getClass(),
                "operator", getOperatorType().identifier(),
                "type", comparators.type.getSimpleName());
    }

    /**
     * Returns an identification of this operation.
     */
    public abstract TemporalOperatorName getOperatorType();

    /**
     * Returns the mathematical symbol for this temporal operation.
     *
     * @return the mathematical symbol, or 0 if none.
     */
    protected char symbol() {
        return (char) 0;
    }

    /**
     * Evaluates the filter between two temporal objects.
     * Both arguments given to this method shall be non-null.
     */
    protected boolean evaluate(T self, T other) {
        return false;
    }

    /**
     * Evaluates the filter between a temporal object and a period.
     * Both arguments given to this method shall be non-null, but period begin or end instant may be null.
     *
     * <p><b>Note:</b> this relationship is not defined by ISO 19108. This method should be overridden
     * only when an ISO 19108 extension can be easily defined, for example for the "equal" operation.</p>
     *
     * @throws DateTimeException if two temporal objects cannot be compared.
     * @throws ArithmeticException if the comparison exceeds integer capacity.
     */
    protected boolean evaluate(T self, Period other) {
        return false;
    }

    /**
     * Evaluates the filter between a period and a temporal object.
     * Both arguments given to this method shall be non-null, but period begin or end instant may be null.
     * Note: the {@code self} and {@code other} argument names are chosen to match ISO 19108 tables.
     *
     * @throws DateTimeException if two temporal objects cannot be compared.
     * @throws ArithmeticException if the comparison exceeds integer capacity.
     */
    protected boolean evaluate(Period self, T other) {
        return false;
    }

    /**
     * Evaluates the filter between two periods.
     * Both arguments given to this method shall be non-null, but period begin or end instant may be null.
     * Note: the {@code self} and {@code other} argument names are chosen to match ISO 19108 tables.
     *
     * @throws DateTimeException if two temporal objects cannot be compared.
     * @throws ArithmeticException if the comparison exceeds integer capacity.
     */
    protected abstract boolean evaluate(Period self, Period other);

    /**
     * Returns {@code true} if {@code other} is non-null and the specified comparison evaluates to {@code true}.
     * This is a helper function for {@code evaluate(…)} methods implementations.
     *
     * @param  test   enumeration value such as {@link TimeMethods#BEFORE} or {@link TimeMethods#AFTER}.
     * @param  self   the object on which to invoke the method identified by {@code test}.
     * @param  other  the argument to give to the test method call, or {@code null} if none.
     * @return the result of performing the comparison identified by {@code test}.
     * @throws DateTimeException if the two objects cannot be compared.
     */
    protected final boolean compare(final int test, final T self, final Instant other) {
        if (other != null) {
            final Temporal position;
            final Optional<IndeterminateValue> p = other.getIndeterminatePosition();
            if (p.isPresent()) {
                if (p.get() == IndeterminateValue.NOW) {
                    position = comparators.now();
                } else {
                    switch (test) {
                        case BEFORE: if (p.get() != IndeterminateValue.AFTER)  return false; else break;
                        case AFTER:  if (p.get() != IndeterminateValue.BEFORE) return false; else break;
                        default: return false;
                    }
                    position = other.getPosition();
                }
            } else {
                position = other.getPosition();
            }
            if (position != null) {
                return comparators.compare(test, self, position);
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if both arguments are non-null and the specified comparison evaluates to {@code true}.
     * This is a helper function for {@code evaluate(…)} methods implementations.
     *
     * @param  test   enumeration value such as {@link TimeMethods#BEFORE} or {@link TimeMethods#AFTER}.
     * @param  self   the object on which to invoke the method identified by {@code test}, or {@code null} if none.
     * @param  other  the argument to give to the test method call, or {@code null} if none.
     * @return the result of performing the comparison identified by {@code test}.
     * @throws DateTimeException if the two objects cannot be compared.
     */
    @SuppressWarnings("unchecked")
    protected static boolean compare(final int test, final Instant self, final Instant other) {
        if (self == null || other == null) {
            return false;
        }
        final IndeterminateValue p1 = self.getIndeterminatePosition().orElse(null);
        final IndeterminateValue p2 = other.getIndeterminatePosition().orElse(null);
        if (p1 != null || p2 != null) {
            if (p1 == p2) {
                return (test == EQUAL) && (p1 == IndeterminateValue.NOW);
            }
            switch (test) {
                case BEFORE: if (isAmbiguous(p1, IndeterminateValue.BEFORE) || isAmbiguous(p2, IndeterminateValue.AFTER))  return false; else break;
                case AFTER:  if (isAmbiguous(p1, IndeterminateValue.AFTER)  || isAmbiguous(p2, IndeterminateValue.BEFORE)) return false; else break;
                default: return false;
            }
        }
        return TimeMethods.compareAny(test, self.getPosition(), other.getPosition());
    }

    /**
     * Returns {@code true} if using the {@code p} value would be ambiguous.
     *
     * @param  p         the indeterminate value to test.
     * @param  required  the required value for a non-ambiguous comparison.
     * @return whether using the given value would be ambiguous.
     */
    private static boolean isAmbiguous(final IndeterminateValue p, final IndeterminateValue required) {
        return (p != null) && (p != IndeterminateValue.NOW) && (p != required);
    }


    /**
     * Reference to a sub-class constructor.
     *
     * @param <T> type of temporal object.
     */
    @FunctionalInterface
    static interface Factory {
        /**
         * Creates a new temporal operation.
         *
         * @param  <T>          type of temporal objects that the operation will accept.
         * @param  comparators  the set of methods to invoke for performing comparisons.
         * @return the temporal operation using the given "is before", "is after" and "is equal" methods.
         */
        <T> TemporalOperation<T> create(TimeMethods<T> comparators);
    }


    /**
     * The {@code "TEquals"} (=) operation. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self = other}</li>
     *   <li>{@literal self.begin = other.begin  AND  self.end = other.end}</li>
     * </ul>
     *
     * @param  <T>  the base type of temporal objects.
     */
    static final class Equals<T> extends TemporalOperation<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -6060822291802339424L;

        /** Creates a new operation. */
        Equals(TimeMethods<T> comparators) {
            super(comparators);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.EQUALS;
        }

        /** Symbol of this operation. */
        @Override protected char symbol() {
            return '=';
        }

        /** Condition defined by ISO 19108:2002 §5.2.3.5. */
        @Override protected boolean evaluate(T self, T other) {
            return comparators.isEqual.test(self, other);
        }

        /** Extension to ISO 19108: handle instant as a tiny period. */
        @Override public boolean evaluate(T self, Period other) {
            return compare(EQUAL, self, other.getBeginning()) &&
                   compare(EQUAL, self, other.getEnding());
        }

        /** Extension to ISO 19108: handle instant as a tiny period. */
        @Override public boolean evaluate(Period self, T other) {
            return compare(EQUAL, other, self.getBeginning()) &&
                   compare(EQUAL, other, self.getEnding());
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(Period self, Period other) {
            return compare(EQUAL, self.getBeginning(), other.getBeginning()) &&
                   compare(EQUAL, self.getEnding(),    other.getEnding());
        }
    }


    /**
     * The {@code "Before"} {@literal (<)} operation. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self     < other}</li>
     *   <li>{@literal self.end < other}</li>
     *   <li>{@literal self.end < other.begin}</li>
     * </ul>
     *
     * @param  <T>  the base type of temporal objects.
     */
    static final class Before<T> extends TemporalOperation<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -3422629447456003982L;

        /** Creates a new operation. */
        Before(TimeMethods<T> comparators) {
            super(comparators);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.BEFORE;
        }

        /** Symbol of this operation. */
        @Override protected char symbol() {
            return '<';
        }

        /** Condition defined by ISO 19108:2002 §5.2.3.5. */
        @Override protected boolean evaluate(T self, T other) {
            return comparators.isBefore.test(self, other);
        }

        /** Relationship not defined by ISO 19108:2006. */
        @Override public boolean evaluate(T self, Period other) {
            return compare(BEFORE, self, other.getBeginning());
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(Period self, T other) {
            return compare(AFTER, other, self.getEnding());
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(Period self, Period other) {
            return compare(BEFORE, self.getEnding(), other.getBeginning());
        }
    }


    /**
     * The {@code "After"} {@literal (>)} operation. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self       > other}</li>
     *   <li>{@literal self.begin > other}</li>
     *   <li>{@literal self.begin > other.end}</li>
     * </ul>
     *
     * @param  <T>  the base type of temporal objects.
     */
    static final class After<T> extends TemporalOperation<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 5410476260417497682L;

        /** Creates a new operation. */
        After(TimeMethods<T> comparators) {
            super(comparators);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.AFTER;
        }

        /** Symbol of this operation. */
        @Override protected char symbol() {
            return '>';
        }

        /** Condition defined by ISO 19108:2002 §5.2.3.5. */
        @Override protected boolean evaluate(T self, T other) {
            return comparators.isAfter.test(self, other);
        }

        /** Relationship not defined by ISO 19108:2006. */
        @Override public boolean evaluate(T self, Period other) {
            return compare(AFTER, self, other.getEnding());
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(Period self, T other) {
            return compare(BEFORE, other, self.getBeginning());
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(Period self, Period other) {
            return compare(AFTER, self.getBeginning(), other.getEnding());
        }
    }


    /**
     * The {@code "Begins"} operation. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin = other.begin  AND  self.end < other.end}</li>
     * </ul>
     *
     * @param  <T>  the base type of temporal objects.
     */
    static final class Begins<T> extends TemporalOperation<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -7880699329127762233L;

        /** Creates a new operation. */
        Begins(TimeMethods<T> comparators) {
            super(comparators);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.BEGINS;
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return compare(EQUAL,  self.getBeginning(), other.getBeginning()) &&
                   compare(BEFORE, self.getEnding(),    other.getEnding());
        }
    }


    /**
     * The {@code "Ends"} operation. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin > other.begin  AND  self.end = other.end}</li>
     * </ul>
     *
     * @param  <T>  the base type of temporal objects.
     */
    static final class Ends<T> extends TemporalOperation<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -5508229966320563437L;

        /** Creates a new operation. */
        Ends(TimeMethods<T> comparators) {
            super(comparators);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.ENDS;
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return compare(EQUAL, self.getEnding(),    other.getEnding()) &&
                   compare(AFTER, self.getBeginning(), other.getBeginning());
        }
    }


    /**
     * The {@code "BegunBy"} operation. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin = other}</li>
     *   <li>{@literal self.begin = other.begin  AND  self.end > other.end}</li>
     * </ul>
     *
     * @param  <T>  the base type of temporal objects.
     */
    static final class BegunBy<T> extends TemporalOperation<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -7212413827394364384L;

        /** Creates a new operation. */
        BegunBy(TimeMethods<T> comparators) {
            super(comparators);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.BEGUN_BY;
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(Period self, T other) {
            return compare(EQUAL, other, self.getBeginning());
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return compare(EQUAL, self.getBeginning(), other.getBeginning()) &&
                   compare(AFTER, self.getEnding(),    other.getEnding());
        }
    }


    /**
     * The {@code "EndedBy"} operation. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.end = other}</li>
     *   <li>{@literal self.begin < other.begin  AND  self.end = other.end}</li>
     * </ul>
     *
     * @param  <T>  the base type of temporal objects.
     */
    static final class EndedBy<T> extends TemporalOperation<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 8586566103462153666L;

        /** Creates a new operation. */
        EndedBy(TimeMethods<T> comparators) {
            super(comparators);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.ENDED_BY;
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final T other) {
            return compare(EQUAL, other, self.getEnding());
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return compare(EQUAL,  self.getEnding(),    other.getEnding()) &&
                   compare(BEFORE, self.getBeginning(), other.getBeginning());
        }
    }


    /**
     * The {@code "Meets"} operation. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.end = other.begin}</li>
     * </ul>
     *
     * @param  <T>  the base type of temporal objects.
     */
    static final class Meets<T> extends TemporalOperation<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -3534843269384858443L;

        /** Creates a new operation. */
        Meets(TimeMethods<T> comparators) {
            super(comparators);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.MEETS;
        }

        /** Extension to ISO 19108: handle instant as a tiny period. */
        @Override public boolean evaluate(final T self, final T other) {
            return comparators.isEqual.test(self, other);
        }

        /** Extension to ISO 19108: handle instant as a tiny period. */
        @Override public boolean evaluate(final T self, final Period other) {
            return compare(EQUAL, self, other.getBeginning());
        }

        /** Extension to ISO 19108: handle instant as a tiny period. */
        @Override public boolean evaluate(final Period self, final T other) {
            return compare(EQUAL, other, self.getEnding());
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return compare(EQUAL, self.getEnding(), other.getBeginning());
        }
    }


    /**
     * The {@code "MetBy"} operation. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin = other.end}</li>
     * </ul>
     *
     * @param  <T>  the base type of temporal objects.
     */
    static final class MetBy<T> extends TemporalOperation<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 5358059498707330482L;

        /** Creates a new operation. */
        MetBy(TimeMethods<T> comparators) {
            super(comparators);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.MET_BY;
        }

        /** Extension to ISO 19108: handle instant as a tiny period. */
        @Override public boolean evaluate(final T self, final T other) {
            return comparators.isEqual.test(self, other);
        }

        /** Extension to ISO 19108: handle instant as a tiny period. */
        @Override public boolean evaluate(final T self, final Period other) {
            return compare(EQUAL, self, other.getEnding());
        }

        /** Extension to ISO 19108: handle instant as a tiny period. */
        @Override public boolean evaluate(final Period self, final T other) {
            return compare(EQUAL, other, self.getBeginning());
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return compare(EQUAL, self.getBeginning(), other.getEnding());
        }
    }


    /**
     * The {@code "During"} operation. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin > other.begin  AND  self.end < other.end}</li>
     * </ul>
     *
     * @param  <T>  the base type of temporal objects.
     */
    static final class During<T> extends TemporalOperation<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -4674319635076886196L;

        /** Creates a new operation. */
        During(TimeMethods<T> comparators) {
            super(comparators);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.DURING;
        }

        /** Symbol of this operation. */
        @Override protected char symbol() {
            return '⊊';         // `self` is a proper (or strict) subset of `other`.
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return compare(AFTER,  self.getBeginning(), other.getBeginning()) &&
                   compare(BEFORE, self.getEnding(),    other.getEnding());
        }
    }


    /**
     * The {@code "TContains"} operation. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin < other AND self.end > other}</li>
     *   <li>{@literal self.begin < other.begin  AND  self.end > other.end}</li>
     * </ul>
     *
     * @param  <T>  the base type of temporal objects.
     */
    static final class Contains<T> extends TemporalOperation<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 9107531246948034411L;

        /** Creates a new operation. */
        Contains(TimeMethods<T> comparators) {
            super(comparators);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.CONTAINS;
        }

        /** Symbol of this operation. */
        @Override protected char symbol() {
            return '⊋';         // `self` is a proper (or strict) superset of `other`.
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final T other) {
            return compare(AFTER,  other, self.getBeginning()) &&
                   compare(BEFORE, other, self.getEnding());
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return compare(BEFORE, self.getBeginning(), other.getBeginning()) &&
                   compare(AFTER,  self.getEnding(),    other.getEnding());
        }
    }


    /**
     * The {@code "TOverlaps"} operation. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin < other.begin  AND  self.end > other.begin  AND  self.end < other.end}</li>
     * </ul>
     *
     * @param  <T>  the base type of temporal objects.
     */
    static final class Overlaps<T> extends TemporalOperation<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 1517443045593389773L;

        /** Creates a new operation. */
        Overlaps(TimeMethods<T> comparators) {
            super(comparators);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.OVERLAPS;
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            final Instant selfBegin, selfEnd, otherBegin, otherEnd;
            return ((otherBegin = other.getBeginning()) != null) &&
                   ((selfBegin  = self.getBeginning())  != null) && compare(BEFORE, selfBegin, otherBegin) &&
                   ((selfEnd    = self.   getEnding())  != null) && compare(AFTER,  selfEnd,   otherBegin) &&
                   ((otherEnd   = other.  getEnding())  != null) && compare(BEFORE, selfEnd,   otherEnd);
        }
    }


    /**
     * The {@code "OverlappedBy"} operation. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin > other.begin  AND  self.begin < other.end  AND  self.end > other.end}</li>
     * </ul>
     *
     * @param  <T>  the base type of temporal objects.
     */
    static final class OverlappedBy<T> extends TemporalOperation<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 2228673820507226463L;

        /** Creates a new operation. */
        OverlappedBy(TimeMethods<T> comparators) {
            super(comparators);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.OVERLAPPED_BY;
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            final Instant selfBegin, selfEnd, otherBegin, otherEnd;
            return ((selfBegin  = self.getBeginning())  != null) &&
                   ((otherBegin = other.getBeginning()) != null) && compare(AFTER,  selfBegin,  otherBegin) &&
                   ((otherEnd   = other.  getEnding())  != null) && compare(BEFORE, selfBegin,  otherEnd)   &&
                   ((selfEnd    = self.   getEnding())  != null) && compare(AFTER,  selfEnd,    otherEnd);
        }
    }


    /**
     * The {@code "AnyInteracts"} filter.
     * This is a shortcut for NOT (Before OR Meets OR MetBy OR After).
     *
     * @param  <T>  the base type of temporal objects.
     */
    static final class AnyInteracts<T> extends TemporalOperation<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 5972351564286442392L;

        /** Creates a new operation. */
        AnyInteracts(TimeMethods<T> comparators) {
            super(comparators);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.ANY_INTERACTS;
        }

        /** Condition defined by OGC filter specification. */
        @Override public boolean evaluate(final Period self, final Period other) {
            final Instant selfBegin, selfEnd, otherBegin, otherEnd;
            return ((selfBegin  = self.getBeginning())  != null) &&
                   ((otherEnd   = other.  getEnding())  != null) && compare(BEFORE, selfBegin, otherEnd) &&
                   ((selfEnd    = self.   getEnding())  != null) &&
                   ((otherBegin = other.getBeginning()) != null) && compare(AFTER, selfEnd, otherBegin);
        }
    }
}
