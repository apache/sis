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

import java.util.Date;
import java.time.Instant;

// Branch-dependent imports
import org.opengis.temporal.Period;
import org.opengis.filter.Filter;
import org.opengis.filter.Expression;
import org.opengis.filter.TemporalOperator;
import org.opengis.filter.TemporalOperatorName;


/**
 * Temporal operations between a period and an instant.
 * The nature of the operation depends on the subclass.
 * Subclasses shall override at least one of following methods:
 *
 * <ul>
 *   <li>{@link #evaluate(Instant, Instant)}</li>
 *   <li>{@link #evaluate(Period, Instant)}</li>
 *   <li>{@link #evaluate(Period, Period)}</li>
 * </ul>
 *
 * @author  Johann Sorel (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.1
 *
 * @param  <T>  the type of resources (e.g. {@link org.opengis.feature.Feature}) used as inputs.
 *
 * @since 1.1
 * @module
 */
abstract class TemporalFilter<T> extends BinaryFunction<T,Object,Object>
        implements TemporalOperator<T>, Optimization.OnFilter<T>
{
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = 5392780837658687513L;

    /**
     * Creates a new temporal function.
     *
     * @param  expression1  the first of the two expressions to be used by this function.
     * @param  expression2  the second of the two expressions to be used by this function.
     */
    TemporalFilter(final Expression<? super T, ?> expression1,
                     final Expression<? super T, ?> expression2)
    {
        super(expression1, expression2);
    }

    /**
     * Converts a GeoAPI instant to a Java instant. This is a temporary method
     * to be removed after we revisited {@link org.opengis.temporal} package.
     *
     * @param  instant  the GeoAPI instant, or {@code null}.
     * @return the Java instant, or {@code null}.
     */
    private static Instant toInstant(final org.opengis.temporal.Instant instant) {
        if (instant != null) {
            final Date t = instant.getDate();
            if (t != null) {
                return t.toInstant();
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if {@code self} is non null and before {@code other}.
     * This is an helper function for {@code evaluate(…)} methods implementations.
     */
    private static boolean isBefore(final org.opengis.temporal.Instant self, final Instant other) {
        final Instant t = toInstant(self);
        return (t != null) && t.isBefore(other);
    }

    /**
     * Returns {@code true} if {@code self} is non null and after {@code other}.
     * This is an helper function for {@code evaluate(…)} methods implementations.
     */
    private static boolean isAfter(final org.opengis.temporal.Instant self, final Instant other) {
        final Instant t = toInstant(self);
        return (t != null) && t.isAfter(other);
    }

    /**
     * Returns {@code true} if {@code self} is non null and equal to {@code other}.
     * This is an helper function for {@code evaluate(…)} methods implementations.
     */
    private static boolean isEqual(final org.opengis.temporal.Instant self, final Instant other) {
        final Instant t = toInstant(self);
        return (t != null) && t.equals(other);
    }

    /**
     * Returns {@code true} if {@code self} is non null and before {@code other}.
     * This is an helper function for {@code evaluate(…)} methods implementations.
     */
    private static boolean isBefore(final org.opengis.temporal.Instant self,
                                    final org.opengis.temporal.Instant other)
    {
        final Instant t, o;
        return ((t = toInstant(self)) != null) && ((o = toInstant(other)) != null) && t.isBefore(o);
    }

    /**
     * Returns {@code true} if {@code self} is non null and after {@code other}.
     * This is an helper function for {@code evaluate(…)} methods implementations.
     */
    private static boolean isAfter(final org.opengis.temporal.Instant self,
                                   final org.opengis.temporal.Instant other)
    {
        final Instant t, o;
        return ((t = toInstant(self)) != null) && ((o = toInstant(other)) != null) && t.isAfter(o);
    }

    /**
     * Returns {@code true} if {@code self} is non null and equal to {@code other}.
     * This is an helper function for {@code evaluate(…)} methods implementations.
     */
    private static boolean isEqual(final org.opengis.temporal.Instant self,
                                   final org.opengis.temporal.Instant other)
    {
        final Instant t = toInstant(self);
        return (t != null) && t.equals(toInstant(other));
    }

    /**
     * Determines if the test(s) represented by this filter passes with the given operands.
     * Values of {@link #expression1} and {@link #expression2} shall be two single values.
     */
    @Override
    public final boolean test(final T candidate) {
        final Object left = expression1.apply(candidate);
        if (left instanceof Period) {
            final Object right = expression2.apply(candidate);
            if (right instanceof Period) {
                return evaluate((Period) left, (Period) right);
            }
            final Instant t = ComparisonFilter.toInstant(right);
            if (t != null) {
                return evaluate((Period) left, t);
            }
        } else {
            final Instant t = ComparisonFilter.toInstant(left);
            if (t != null) {
                final Instant t2 = ComparisonFilter.toInstant(expression2.apply(candidate));
                if (t2 != null) {
                    return evaluate(t, t2);
                }
            }
        }
        return false;
    }

    /**
     * Evaluates the filter between two instants.
     * Both arguments given to this method are non-null.
     * The {@code self} and {@code other} argument names are chosen to match ISO 19108 tables.
     */
    protected boolean evaluate(Instant self, Instant other) {
        return false;
    }

    /**
     * Evaluates the filter between a period and an instant.
     * Both arguments given to this method are non-null, but period begin or end instant may be null.
     * The {@code self} and {@code other} argument names are chosen to match ISO 19108 tables.
     */
    protected boolean evaluate(Period self, Instant other) {
        return false;
    }

    /**
     * Evaluates the filter between two periods.
     * Both arguments given to this method are non-null, but period begin or end instant may be null.
     * The {@code self} and {@code other} argument names are chosen to match ISO 19108 tables.
     */
    protected boolean evaluate(Period self, Period other) {
        return false;
    }

    /*
     * No operation on numbers for now. We could revisit this policy in a future version if we
     * allow the temporal function to have a CRS and to operate on temporal coordinate values.
     */


    /**
     * The {@code "TEquals"} (=) filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self = other}</li>
     *   <li>{@literal self.begin = other.begin  AND  self.end = other.end}</li>
     * </ul>
     */
    static final class Equals<T> extends TemporalFilter<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -6060822291802339424L;

        /** Creates a new filter. */
        Equals(Expression<? super T, ?> expression1,
               Expression<? super T, ?> expression2)
        {
            super(expression1, expression2);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public Filter<T> recreate(final Expression<? super T, ?>[] effective) {
            return new Equals<>(effective[0], effective[1]);
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
        @Override protected boolean evaluate(final Instant self, final Instant other) {
            return self.equals(other);
        }

        /** Extension to ISO 19108: handle instant as a tiny period. */
        @Override public boolean evaluate(final Period self, final Instant other) {
            return isEqual(self.getBeginning(), other) &&
                   isEqual(self.getEnding(),    other);
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return isEqual(self.getBeginning(), other.getBeginning()) &&
                   isEqual(self.getEnding(),    other.getEnding());
        }
    }


    /**
     * The {@code "Before"} {@literal (<)} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self     < other}</li>
     *   <li>{@literal self.end < other}</li>
     *   <li>{@literal self.end < other.begin}</li>
     * </ul>
     */
    static final class Before<T> extends TemporalFilter<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -3422629447456003982L;

        /** Creates a new filter. */
        Before(Expression<? super T, ?> expression1,
               Expression<? super T, ?> expression2)
        {
            super(expression1, expression2);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public Filter<T> recreate(final Expression<? super T, ?>[] effective) {
            return new Before<>(effective[0], effective[1]);
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
        @Override protected boolean evaluate(final Instant self, final Instant other) {
            return self.isBefore(other);
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Instant other) {
            return isBefore(self.getEnding(), other);
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return isBefore(self.getEnding(), other.getBeginning());
        }
    }


    /**
     * The {@code "After"} {@literal (>)} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self       > other}</li>
     *   <li>{@literal self.begin > other}</li>
     *   <li>{@literal self.begin > other.end}</li>
     * </ul>
     */
    static final class After<T> extends TemporalFilter<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 5410476260417497682L;

        /** Creates a new filter. */
        After(Expression<? super T, ?> expression1,
              Expression<? super T, ?> expression2)
        {
            super(expression1, expression2);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public Filter<T> recreate(final Expression<? super T, ?>[] effective) {
            return new After<>(effective[0], effective[1]);
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
        @Override protected boolean evaluate(final Instant self, final Instant other) {
            return self.isAfter(other);
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Instant other) {
            return isAfter(self.getBeginning(), other);
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return isAfter(self.getBeginning(), other.getEnding());
        }
    }


    /**
     * The {@code "Begins"} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin = other.begin  AND  self.end < other.end}</li>
     * </ul>
     */
    static final class Begins<T> extends TemporalFilter<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -7880699329127762233L;

        /** Creates a new filter. */
        Begins(Expression<? super T, ?> expression1,
               Expression<? super T, ?> expression2)
        {
            super(expression1, expression2);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public Filter<T> recreate(final Expression<? super T, ?>[] effective) {
            return new Begins<>(effective[0], effective[1]);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.BEGINS;
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return isEqual (self.getBeginning(), other.getBeginning()) &&
                   isBefore(self.getEnding(),    other.getEnding());
        }
    }


    /**
     * The {@code "Ends"} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin > other.begin  AND  self.end = other.end}</li>
     * </ul>
     */
    static final class Ends<T> extends TemporalFilter<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -5508229966320563437L;

        /** Creates a new filter. */
        Ends(Expression<? super T, ?> expression1,
             Expression<? super T, ?> expression2)
        {
            super(expression1, expression2);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public Filter<T> recreate(final Expression<? super T, ?>[] effective) {
            return new Ends<>(effective[0], effective[1]);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.ENDS;
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return isEqual(self.getEnding(),    other.getEnding()) &&
                   isAfter(self.getBeginning(), other.getBeginning());
        }
    }


    /**
     * The {@code "BegunBy"} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin = other}</li>
     *   <li>{@literal self.begin = other.begin  AND  self.end > other.end}</li>
     * </ul>
     */
    static final class BegunBy<T> extends TemporalFilter<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -7212413827394364384L;

        /** Creates a new filter. */
        BegunBy(Expression<? super T, ?> expression1,
                Expression<? super T, ?> expression2)
        {
            super(expression1, expression2);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public Filter<T> recreate(final Expression<? super T, ?>[] effective) {
            return new BegunBy<>(effective[0], effective[1]);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.BEGUN_BY;
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Instant other) {
            return isEqual(self.getBeginning(), other);
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return isEqual(self.getBeginning(), other.getBeginning()) &&
                   isAfter(self.getEnding(),    other.getEnding());
        }
    }


    /**
     * The {@code "EndedBy"} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.end = other}</li>
     *   <li>{@literal self.begin < other.begin  AND  self.end = other.end}</li>
     * </ul>
     */
    static final class EndedBy<T> extends TemporalFilter<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 8586566103462153666L;

        /** Creates a new filter. */
        EndedBy(Expression<? super T, ?> expression1,
                Expression<? super T, ?> expression2)
        {
            super(expression1, expression2);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public Filter<T> recreate(final Expression<? super T, ?>[] effective) {
            return new EndedBy<>(effective[0], effective[1]);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.ENDED_BY;
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Instant other) {
            return isEqual(self.getEnding(), other);
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return isEqual (self.getEnding(),    other.getEnding()) &&
                   isBefore(self.getBeginning(), other.getBeginning());
        }
    }


    /**
     * The {@code "Meets"} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.end = other.begin}</li>
     * </ul>
     */
    static final class Meets<T> extends TemporalFilter<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -3534843269384858443L;

        /** Creates a new filter. */
        Meets(Expression<? super T, ?> expression1,
              Expression<? super T, ?> expression2)
        {
            super(expression1, expression2);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public Filter<T> recreate(final Expression<? super T, ?>[] effective) {
            return new Meets<>(effective[0], effective[1]);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.MEETS;
        }

        /** Extension to ISO 19108: handle instant as a tiny period. */
        @Override public boolean evaluate(final Instant self, final Instant other) {
            return self.equals(other);
        }

        /** Extension to ISO 19108: handle instant as a tiny period. */
        @Override public boolean evaluate(final Period self, final Instant other) {
            return isEqual(self.getEnding(), other);
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return isEqual(self.getEnding(), other.getBeginning());
        }
    }


    /**
     * The {@code "MetBy"} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin = other.end}</li>
     * </ul>
     */
    static final class MetBy<T> extends TemporalFilter<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 5358059498707330482L;

        /** Creates a new filter. */
        MetBy(Expression<? super T, ?> expression1,
              Expression<? super T, ?> expression2)
        {
            super(expression1, expression2);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public Filter<T> recreate(final Expression<? super T, ?>[] effective) {
            return new MetBy<>(effective[0], effective[1]);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.MET_BY;
        }

        /** Extension to ISO 19108: handle instant as a tiny period. */
        @Override public boolean evaluate(final Instant self, final Instant other) {
            return self.equals(other);
        }

        /** Extension to ISO 19108: handle instant as a tiny period. */
        @Override public boolean evaluate(final Period self, final Instant other) {
            return isEqual(self.getBeginning(), other);
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return isEqual(self.getBeginning(), other.getEnding());
        }
    }


    /**
     * The {@code "During"} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin > other.begin  AND  self.end < other.end}</li>
     * </ul>
     */
    static final class During<T> extends TemporalFilter<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -4674319635076886196L;

        /** Creates a new filter. */
        During(Expression<? super T, ?> expression1,
               Expression<? super T, ?> expression2)
        {
            super(expression1, expression2);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public Filter<T> recreate(final Expression<? super T, ?>[] effective) {
            return new During<>(effective[0], effective[1]);
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
            return isAfter (self.getBeginning(), other.getBeginning()) &&
                   isBefore(self.getEnding(),    other.getEnding());
        }
    }


    /**
     * The {@code "TContains"} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin < other AND self.end > other}</li>
     *   <li>{@literal self.begin < other.begin  AND  self.end > other.end}</li>
     * </ul>
     */
    static final class Contains<T> extends TemporalFilter<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 9107531246948034411L;

        /** Creates a new filter. */
        Contains(Expression<? super T, ?> expression1,
                 Expression<? super T, ?> expression2)
        {
            super(expression1, expression2);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public Filter<T> recreate(final Expression<? super T, ?>[] effective) {
            return new Contains<>(effective[0], effective[1]);
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
        @Override public boolean evaluate(final Period self, final Instant other) {
            return isBefore(self.getBeginning(), other) &&
                   isAfter (self.getEnding(),    other);
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return isBefore(self.getBeginning(), other.getBeginning()) &&
                   isAfter (self.getEnding(),    other.getEnding());
        }
    }


    /**
     * The {@code "TOverlaps"} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin < other.begin  AND  self.end > other.begin  AND  self.end < other.end}</li>
     * </ul>
     */
    static final class Overlaps<T> extends TemporalFilter<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 1517443045593389773L;

        /** Creates a new filter. */
        Overlaps(Expression<? super T, ?> expression1,
                 Expression<? super T, ?> expression2)
        {
            super(expression1, expression2);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public Filter<T> recreate(final Expression<? super T, ?>[] effective) {
            return new Overlaps<>(effective[0], effective[1]);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.OVERLAPS;
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            final Instant selfEnd, otherBegin;
            return ((selfEnd    = toInstant(self .getEnding()))    != null) &&
                   ((otherBegin = toInstant(other.getBeginning())) != null) && selfEnd.isAfter(otherBegin) &&
                   isBefore(self.getBeginning(), otherBegin) &&
                   isAfter(other.getEnding(),    selfEnd);
        }
    }


    /**
     * The {@code "OverlappedBy"} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin > other.begin  AND  self.begin < other.end  AND  self.end > other.end}</li>
     * </ul>
     */
    static final class OverlappedBy<T> extends TemporalFilter<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 2228673820507226463L;

        /** Creates a new filter. */
        OverlappedBy(Expression<? super T, ?> expression1,
                     Expression<? super T, ?> expression2)
        {
            super(expression1, expression2);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public Filter<T> recreate(final Expression<? super T, ?>[] effective) {
            return new OverlappedBy<>(effective[0], effective[1]);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.OVERLAPPED_BY;
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            final Instant selfBegin, otherEnd;
            return ((selfBegin = toInstant(self .getBeginning())) != null) &&
                   ((otherEnd  = toInstant(other.getEnding()))    != null) && selfBegin.isBefore(otherEnd) &&
                   isBefore(other.getBeginning(), selfBegin) &&
                   isAfter (self .getEnding(),    otherEnd);
        }
    }


    /**
     * The {@code "AnyInteracts"} filter.
     * This is a shortcut for NOT (Before OR Meets OR MetBy OR After).
     */
    static final class AnyInteracts<T> extends TemporalFilter<T> {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 5972351564286442392L;

        /** Creates a new filter. */
        AnyInteracts(Expression<? super T, ?> expression1,
                     Expression<? super T, ?> expression2)
        {
            super(expression1, expression2);
        }

        /** Creates a new filter of the same type but different parameters. */
        @Override public Filter<T> recreate(final Expression<? super T, ?>[] effective) {
            return new AnyInteracts<>(effective[0], effective[1]);
        }

        /** Identification of this operation. */
        @Override public TemporalOperatorName getOperatorType() {
            return TemporalOperatorName.ANY_INTERACTS;
        }

        /** Condition defined by OGC filter specification. */
        @Override public boolean evaluate(final Period self, final Period other) {
            final Instant selfBegin, selfEnd, otherBegin, otherEnd;
            return ((selfBegin  = toInstant(self .getBeginning())) != null) &&
                   ((otherEnd   = toInstant(other.getEnding()))    != null) && selfBegin.isBefore(otherEnd) &&
                   ((selfEnd    = toInstant(self .getEnding()))    != null) &&
                   ((otherBegin = toInstant(other.getBeginning())) != null) && selfEnd.isAfter(otherBegin);
        }
    }
}
