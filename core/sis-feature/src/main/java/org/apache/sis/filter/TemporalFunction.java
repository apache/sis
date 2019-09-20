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
import java.math.BigDecimal;
import java.math.BigInteger;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.temporal.BinaryTemporalOperator;
import org.opengis.temporal.Period;
import org.apache.sis.math.Fraction;



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
 * @since   1.1
 * @module
 */
abstract class TemporalFunction extends BinaryFunction implements BinaryTemporalOperator {
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
    TemporalFunction(final Expression expression1, final Expression expression2) {
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
    public final boolean evaluate(final Object candidate) {
        final Object left = expression1.evaluate(candidate);
        if (left instanceof Period) {
            final Object right = expression2.evaluate(candidate);
            if (right instanceof Period) {
                return evaluate((Period) left, (Period) right);
            }
            final Instant t = ComparisonFunction.toInstant(right);
            if (t != null) {
                return evaluate((Period) left, t);
            }
        } else {
            final Instant t = ComparisonFunction.toInstant(left);
            if (t != null) {
                final Instant t2 = ComparisonFunction.toInstant(expression2.evaluate(candidate));
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

    /**
     * No operation on numbers for now. We could revisit this policy in a future version if we
     * allow the temporal function to have a CRS and to operate on temporal coordinate values.
     */
    @Override protected Number applyAsLong    (long       left, long       right) {return null;}
    @Override protected Number applyAsDouble  (double     left, double     right) {return null;}
    @Override protected Number applyAsFraction(Fraction   left, Fraction   right) {return null;}
    @Override protected Number applyAsInteger (BigInteger left, BigInteger right) {return null;}
    @Override protected Number applyAsDecimal (BigDecimal left, BigDecimal right) {return null;}


    /**
     * The {@value #NAME} (=) filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self = other}</li>
     *   <li>{@literal self.begin = other.begin  AND  self.end = other.end}</li>
     * </ul>
     */
    static final class Equals extends TemporalFunction implements org.opengis.filter.temporal.TEquals {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -6060822291802339424L;

        /** Creates a new filter for the {@value #NAME} operation. */
        Equals(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        /** Identification of this operation. */
        @Override protected String getName() {return NAME;}
        @Override protected char   symbol()  {return '=';}

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

        /** Implementation of the visitor pattern (not used by Apache SIS). */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }


    /**
     * The {@value #NAME} {@literal (<)} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self     < other}</li>
     *   <li>{@literal self.end < other}</li>
     *   <li>{@literal self.end < other.begin}</li>
     * </ul>
     */
    static final class Before extends TemporalFunction implements org.opengis.filter.temporal.Before {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -3422629447456003982L;

        /** Creates a new filter for the {@value #NAME} operation. */
        Before(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        /** Identification of this operation. */
        @Override protected String getName() {return NAME;}
        @Override protected char   symbol()  {return '<';}

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

        /** Implementation of the visitor pattern (not used by Apache SIS). */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }


    /**
     * The {@value #NAME} {@literal (>)} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self       > other}</li>
     *   <li>{@literal self.begin > other}</li>
     *   <li>{@literal self.begin > other.end}</li>
     * </ul>
     */
    static final class After extends TemporalFunction implements org.opengis.filter.temporal.After {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 5410476260417497682L;

        /** Creates a new filter for the {@value #NAME} operation. */
        After(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        /** Identification of this operation. */
        @Override protected String getName() {return NAME;}
        @Override protected char   symbol()  {return '>';}

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

        /** Implementation of the visitor pattern (not used by Apache SIS). */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }


    /**
     * The {@value #NAME} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin = other.begin  AND  self.end < other.end}</li>
     * </ul>
     */
    static final class Begins extends TemporalFunction implements org.opengis.filter.temporal.Begins {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -7880699329127762233L;

        /** Creates a new filter for the {@value #NAME} operation. */
        Begins(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        /** Identification of this operation. */
        @Override protected String getName() {return NAME;}

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return isEqual (self.getBeginning(), other.getBeginning()) &&
                   isBefore(self.getEnding(),    other.getEnding());
        }

        /** Implementation of the visitor pattern (not used by Apache SIS). */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }


    /**
     * The {@value #NAME} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin > other.begin  AND  self.end = other.end}</li>
     * </ul>
     */
    static final class Ends extends TemporalFunction implements org.opengis.filter.temporal.Ends {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -5508229966320563437L;

        /** Creates a new filter for the {@value #NAME} operation. */
        Ends(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        /** Identification of this operation. */
        @Override protected String getName() {return NAME;}

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return isEqual(self.getEnding(),    other.getEnding()) &&
                   isAfter(self.getBeginning(), other.getBeginning());
        }

        /** Implementation of the visitor pattern (not used by Apache SIS). */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }


    /**
     * The {@value #NAME} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin = other}</li>
     *   <li>{@literal self.begin = other.begin  AND  self.end > other.end}</li>
     * </ul>
     */
    static final class BegunBy extends TemporalFunction implements org.opengis.filter.temporal.BegunBy {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -7212413827394364384L;

        /** Creates a new filter for the {@value #NAME} operation. */
        BegunBy(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        /** Identification of this operation. */
        @Override protected String getName() {return NAME;}

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Instant other) {
            return isEqual(self.getBeginning(), other);
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return isEqual(self.getBeginning(), other.getBeginning()) &&
                   isAfter(self.getEnding(),    other.getEnding());
        }

        /** Implementation of the visitor pattern (not used by Apache SIS). */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }


    /**
     * The {@value #NAME} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.end = other}</li>
     *   <li>{@literal self.begin < other.begin  AND  self.end = other.end}</li>
     * </ul>
     */
    static final class EndedBy extends TemporalFunction implements org.opengis.filter.temporal.EndedBy {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 8586566103462153666L;

        /** Creates a new filter for the {@value #NAME} operation. */
        EndedBy(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        /** Identification of this operation. */
        @Override protected String getName() {return NAME;}

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Instant other) {
            return isEqual(self.getEnding(), other);
        }

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return isEqual (self.getEnding(),    other.getEnding()) &&
                   isBefore(self.getBeginning(), other.getBeginning());
        }

        /** Implementation of the visitor pattern (not used by Apache SIS). */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }


    /**
     * The {@value #NAME} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.end = other.begin}</li>
     * </ul>
     */
    static final class Meets extends TemporalFunction implements org.opengis.filter.temporal.Meets {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -3534843269384858443L;

        /** Creates a new filter for the {@value #NAME} operation. */
        Meets(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        /** Identification of this operation. */
        @Override protected String getName() {return NAME;}

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

        /** Implementation of the visitor pattern (not used by Apache SIS). */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }


    /**
     * The {@value #NAME} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin = other.end}</li>
     * </ul>
     */
    static final class MetBy extends TemporalFunction implements org.opengis.filter.temporal.MetBy {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 5358059498707330482L;

        /** Creates a new filter for the {@value #NAME} operation. */
        MetBy(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        /** Identification of this operation. */
        @Override protected String getName() {return NAME;}

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

        /** Implementation of the visitor pattern (not used by Apache SIS). */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }


    /**
     * The {@value #NAME} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin > other.begin  AND  self.end < other.end}</li>
     * </ul>
     */
    static final class During extends TemporalFunction implements org.opengis.filter.temporal.During {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = -4674319635076886196L;

        /** Creates a new filter for the {@value #NAME} operation. */
        During(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        /** Identification of this operation. */
        @Override protected String getName() {return NAME;}
        @Override protected char   symbol()  {return '⊊';}      // `self` is a proper (or strict) subset of `other`.

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            return isAfter (self.getBeginning(), other.getBeginning()) &&
                   isBefore(self.getEnding(),    other.getEnding());
        }

        /** Implementation of the visitor pattern (not used by Apache SIS). */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }


    /**
     * The {@value #NAME} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin < other AND self.end > other}</li>
     *   <li>{@literal self.begin < other.begin  AND  self.end > other.end}</li>
     * </ul>
     */
    static final class Contains extends TemporalFunction implements org.opengis.filter.temporal.TContains {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 9107531246948034411L;

        /** Creates a new filter for the {@value #NAME} operation. */
        Contains(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        /** Identification of this operation. */
        @Override protected String getName() {return NAME;}
        @Override protected char   symbol()  {return '⊋';}      // `self` is a proper (or strict) superset of `other`.

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

        /** Implementation of the visitor pattern (not used by Apache SIS). */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }


    /**
     * The {@value #NAME} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin < other.begin  AND  self.end > other.begin  AND  self.end < other.end}</li>
     * </ul>
     */
    static final class Overlaps extends TemporalFunction implements org.opengis.filter.temporal.TOverlaps {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 1517443045593389773L;

        /** Creates a new filter for the {@value #NAME} operation. */
        Overlaps(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        /** Identification of this operation. */
        @Override protected String getName() {return NAME;}

        /** Condition defined by ISO 19108:2006 (corrigendum) §5.2.3.5. */
        @Override public boolean evaluate(final Period self, final Period other) {
            final Instant selfEnd, otherBegin;
            return ((selfEnd    = toInstant(self .getEnding()))    != null) &&
                   ((otherBegin = toInstant(other.getBeginning())) != null) && selfEnd.isAfter(otherBegin) &&
                   isBefore(self.getBeginning(), otherBegin) &&
                   isAfter(other.getEnding(),    selfEnd);
        }

        /** Implementation of the visitor pattern (not used by Apache SIS). */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }


    /**
     * The {@value #NAME} filter. Defined by ISO 19108 as:
     * <ul>
     *   <li>{@literal self.begin > other.begin  AND  self.begin < other.end  AND  self.end > other.end}</li>
     * </ul>
     */
    static final class OverlappedBy extends TemporalFunction implements org.opengis.filter.temporal.OverlappedBy {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 2228673820507226463L;

        /** Creates a new filter for the {@value #NAME} operation. */
        OverlappedBy(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        /** Identification of this operation. */
        @Override protected String getName() {return NAME;}

        /** Implementation of the visitor pattern (not used by Apache SIS). */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
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
     * The {@value #NAME} filter.
     * This is a shortcut for NOT (Before OR Meets OR MetBy OR After).
     */
    static final class AnyInteracts extends TemporalFunction implements org.opengis.filter.temporal.AnyInteracts {
        /** For cross-version compatibility during (de)serialization. */
        private static final long serialVersionUID = 5972351564286442392L;

        /** Creates a new filter for the {@value #NAME} operation. */
        AnyInteracts(Expression expression1, Expression expression2) {
            super(expression1, expression2);
        }

        /** Identification of this operation. */
        @Override protected String getName() {return NAME;}

        /** Condition defined by OGC filter specification. */
        @Override public boolean evaluate(final Period self, final Period other) {
            final Instant selfBegin, selfEnd, otherBegin, otherEnd;
            return ((selfBegin  = toInstant(self .getBeginning())) != null) &&
                   ((otherEnd   = toInstant(other.getEnding()))    != null) && selfBegin.isBefore(otherEnd) &&
                   ((selfEnd    = toInstant(self .getEnding()))    != null) &&
                   ((otherBegin = toInstant(other.getBeginning())) != null) && selfEnd.isAfter(otherBegin);
        }

        /** Implementation of the visitor pattern (not used by Apache SIS). */
        @Override public Object accept(FilterVisitor visitor, Object extraData) {
            return visitor.visit(this, extraData);
        }
    }
}
